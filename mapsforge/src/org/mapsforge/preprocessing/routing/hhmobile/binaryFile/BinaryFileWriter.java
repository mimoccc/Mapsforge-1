/*
 * Copyright 2010 mapsforge.org
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.mapsforge.preprocessing.routing.hhmobile.binaryFile;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import org.mapsforge.preprocessing.routing.hhmobile.clustering.Clustering;
import org.mapsforge.preprocessing.routing.hhmobile.clustering.KCenterClusteringAlgorithm;
import org.mapsforge.preprocessing.routing.hhmobile.clustering.QuadTreeClusteringAlgorithm;
import org.mapsforge.preprocessing.routing.hhmobile.graph.LevelGraph;
import org.mapsforge.preprocessing.util.DBConnection;

class BinaryFileWriter {

	private final static byte[] HEADER_MAGIC = HHGlobalConstants.BINARY_FILE_HEADER_MAGIC;
	private final static int HEADER_LENGTH = HHGlobalConstants.BINARY_FILE_HEADER_LENGTH;

	private final static int BUFFER_SIZE = 16384 * 1000;

	public static void writeBinaryFile(Properties props) {
		try {
			// get parameters from properties
			String dbHost = props.getProperty("hhmobile.input.db.host");
			int dbPort = Integer.parseInt(props.getProperty("hhmobile.input.db.port"));
			String dbName = props.getProperty("hhmobile.input.db.name");
			String dbUser = props.getProperty("hhmobile.input.db.user");
			String dbPassword = props.getProperty("hhmobile.input.db.password");

			File outputFile = new File(props.getProperty("hhmobile.output.file"));

			String clusteringAlgorithm = props.getProperty("hhmobile.clusteringAlgorithm");
			int quadtreeAlgorithmThreshold = Integer.parseInt(props
					.getProperty("hhmobile.quadTreeAlgorithm.threshold"));
			int kcenterAlgorithmVerticesPerCluster = Integer.parseInt(props
					.getProperty("hhmobile.kcenterAlgorithm.verticesPerCluster"));
			String clusterBlockEncoding = props.getProperty("hhmobile.clusterblockEncoding");
			int blockPointerIdxGroupSizeThreshold = Integer.parseInt(props
					.getProperty("hhmobile.blockPointerIndex.groupSizeThreshold"));
			System.out.println("create hh binary file '" + outputFile.getAbsolutePath() + "'");

			// load graph from database
			System.out.println("load graph from db 'jdbc://" + dbHost + ":" + dbPort + "/"
					+ dbName + "'");
			Connection conn = DBConnection.getJdbcConnectionPg(dbHost, dbPort, dbName, dbUser,
					dbPassword);
			LevelGraph graph = new LevelGraph(conn);

			// compute clustering
			System.out.println("compute clustering");
			Clustering[] clustering;
			if (clusteringAlgorithm.equals(QuadTreeClusteringAlgorithm.ALGORITHM_NAME)) {
				clustering = QuadTreeClusteringAlgorithm.computeClustering(graph.getLevels(),
						graph.getVertexLongitudes(), graph.getVertexLatitudes(),
						QuadTreeClusteringAlgorithm.HEURISTIC_CENTER,
						quadtreeAlgorithmThreshold);

			} else if (clusteringAlgorithm.equals(KCenterClusteringAlgorithm.ALGORITHM_NAME)) {
				clustering = KCenterClusteringAlgorithm.computeClustering(graph.getLevels(),
						kcenterAlgorithmVerticesPerCluster,
						KCenterClusteringAlgorithm.HEURISTIC_MIN_SIZE);
			} else {
				System.out.println("invalid clustering algorithm specified in properties.");
				return;
			}
			// create the binary file
			System.out.println("write file '" + outputFile.getAbsolutePath() + "'");
			writeBinaryFile(graph, clustering, outputFile, blockPointerIdxGroupSizeThreshold);
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void writeBinaryFile(LevelGraph levelGraph, Clustering[] clustering,
			File targetFile, int indexGroupSizeThreshold) throws IOException {

		// ---------------- WRITE TEMPORARY FILES --------------------------

		File fBlocks = new File(targetFile.getAbsolutePath() + ".blocks");
		File fBlockIndex = new File(targetFile.getAbsolutePath() + ".blockIdx");
		File fRTree = new File(targetFile.getAbsolutePath() + ".rtree");

		// write the graphs cluster blocks
		int[] blockSize = RleBlockWriter.writeClusterBlocks(fBlocks, levelGraph, clustering);

		// write block index
		BlockIndex blockIdx = BlockIndex.getSpaceOptimalIndex(blockSize,
				indexGroupSizeThreshold);

		DataOutputStream out = new DataOutputStream(new BufferedOutputStream(
				new FileOutputStream(fBlockIndex)));
		blockIdx.serialize(out);
		out.close();

		// ---------------- WRITE THE BINARY FILE --------------------------

		out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(targetFile)));

		// write header of the binary
		long startAddrGraph = HEADER_LENGTH;
		long endAddrGraph = startAddrGraph + fBlocks.length();

		long startAddrBlockIdx = endAddrGraph;
		long endAddrBlockIdx = startAddrBlockIdx + fBlockIndex.length();

		out.write(HEADER_MAGIC);
		out.writeLong(startAddrGraph);
		out.writeLong(endAddrGraph);
		out.writeLong(startAddrBlockIdx);
		out.writeLong(endAddrBlockIdx);
		if (out.size() <= HEADER_LENGTH) {
			out.write(new byte[HEADER_LENGTH - out.size()]);
		} else {
			throw new RuntimeException("need to increase header length.");
		}

		// write cluster blocks
		writeFile(fBlocks, out);

		// write cluster blocks
		writeFile(fBlockIndex, out);
		out.flush();
		out.close();

		// ---------------- CLEAN UP TEMPORARY FILES --------------------------
		// fGraph.delete();
		// fBlockIndex.delete();
	}

	/**
	 * Writes the content of the given file to the output stream.
	 * 
	 * @param f
	 *            the file to be written.
	 * @param oStream
	 *            the stream to write to.
	 * @return number of bytes written.
	 * @throws IOException
	 *             on write errors.
	 */
	private static long writeFile(File f, OutputStream oStream) throws IOException {
		byte[] buff = new byte[BUFFER_SIZE];
		BufferedInputStream in = new BufferedInputStream(new FileInputStream(f));

		int len;
		long offset = 0;
		while ((len = in.read(buff)) > 0) {
			oStream.write(buff, 0, len);
			offset += len;
		}
		in.close();
		return offset;
	}

	public static void main(String[] args) throws IOException {
		Properties props = new Properties();
		props.load(new FileInputStream("res/conf/hhMobile.properties"));
		writeBinaryFile(props);
	}
}
