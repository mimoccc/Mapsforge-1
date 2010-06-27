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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;

import org.mapsforge.preprocessing.routing.hhmobile.binaryFile.graph.BlockPointerIndex;
import org.mapsforge.preprocessing.routing.hhmobile.binaryFile.graph.BlockedGraphSerializer;
import org.mapsforge.preprocessing.routing.hhmobile.clustering.IClustering;
import org.mapsforge.preprocessing.routing.hhmobile.clustering.QuadTreeClusteringAlgorithm;
import org.mapsforge.preprocessing.routing.hhmobile.graph.LevelGraph;
import org.mapsforge.preprocessing.routing.highwayHierarchies.util.Serializer;
import org.mapsforge.preprocessing.util.DBConnection;

public class BinaryFileWriter {

	private final static int BUFFER_SIZE = 16384 * 1000;

	public static void writeBinaryFile(LevelGraph levelGraph, IClustering[] clustering,
			File targetFile, String comment, int indexGroupSizeThreshold) throws IOException {

		// temporary files

		File fGraphHeader = new File(targetFile.getAbsolutePath() + ".blocksHeader");
		File fClusterBlocks = new File(targetFile.getAbsolutePath() + ".blocks");
		File fBlockPointerIdx = new File(targetFile.getAbsolutePath() + ".blockIdx");

		// write graphHeader and clusterBlocks

		int[] blockSize = BlockedGraphSerializer.writeBlockedGraph(fGraphHeader,
				fClusterBlocks, levelGraph, clustering);

		// write blockPointer Index

		BlockPointerIndex blockIdx = BlockPointerIndex.getSpaceOptimalIndex(blockSize,
				indexGroupSizeThreshold);
		BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(
				fBlockPointerIdx));
		blockIdx.serialize(out);
		out.close();

		// create binary file header

		long startAddrGraphHeader = BinaryFileHeader.HEADER_LENGTH;
		long endAddrGraphHeader = startAddrGraphHeader + fGraphHeader.length();

		long startAddrClusterBlocks = endAddrGraphHeader;
		long endAddrClusterBlocks = startAddrClusterBlocks + fClusterBlocks.length();

		long startAddrBlockPointerIdx = endAddrClusterBlocks;
		long endAddrBlockPointerIdx = startAddrBlockPointerIdx + fBlockPointerIdx.length();

		BinaryFileHeader header = new BinaryFileHeader(startAddrGraphHeader,
				endAddrGraphHeader, startAddrClusterBlocks, endAddrClusterBlocks,
				startAddrBlockPointerIdx, endAddrBlockPointerIdx, comment);

		// ---------------- WRITE THE BINARY FILE --------------------------

		out = new BufferedOutputStream(new FileOutputStream(targetFile));

		// write header
		out.write(header.serialize());

		// write graph header
		writeFile(fGraphHeader, out);

		// write cluster blocks
		writeFile(fClusterBlocks, out);

		// write cluster blocks
		writeFile(fBlockPointerIdx, out);

		out.flush();
		out.close();

		// clean up
		fGraphHeader.delete();
		fClusterBlocks.delete();
		fBlockPointerIdx.delete();
	}

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

	public static void main(String[] args) throws IOException, ClassNotFoundException,
			SQLException {
		LevelGraph levelGraph;
		IClustering[] clustering;
		String map = "berlin";

		if (true) {
			levelGraph = new LevelGraph(DBConnection.getJdbcConnectionPg("localhost", 5432,
					map, "postgres", "admin"));
			Serializer.serialize(new File(map + ".levelGraph"), levelGraph);
			clustering = QuadTreeClusteringAlgorithm.computeClustering(levelGraph.getLevels(),
					levelGraph.getVertexLongitudes(), levelGraph.getVertexLatitudes(),
					QuadTreeClusteringAlgorithm.HEURISTIC_CENTER, 100);
			// clustering = KCenterClusteringAlgorithm.computeClustering(levelGraph.getLevels(),
			// 100, KCenterClusteringAlgorithm.HEURISTIC_MIN_SIZE);

			Serializer.serialize(new File(map + ".clustering"), clustering);
		}
		for (int i = 0; i < levelGraph.numLevels(); i++) {
			System.out.print(levelGraph.getLevel(i).numVertices() + " ");
		}
		System.out.println();
		for (int i = 0; i < levelGraph.numLevels(); i++) {
			System.out.print(levelGraph.getLevel(i).numEdges() + " ");
		}
		System.out.println();

		System.out.print("reading input data (" + map + ") ... ");
		levelGraph = Serializer.deserialize(new File(map + ".levelGraph"));
		clustering = Serializer.deserialize(new File(map + ".clustering"));
		System.out.println("ready!");

		File file = new File(map + ".mobile_hh");
		int indexGroupSizeThreshold = 100;

		writeBinaryFile(levelGraph, clustering, file, "test", indexGroupSizeThreshold);
	}
}
