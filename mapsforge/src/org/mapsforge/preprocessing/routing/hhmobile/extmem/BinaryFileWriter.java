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
package org.mapsforge.preprocessing.routing.hhmobile.extmem;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.mapsforge.preprocessing.routing.hhmobile.clustering.IClustering;
import org.mapsforge.preprocessing.routing.hhmobile.clustering.KCenterClustering;
import org.mapsforge.preprocessing.routing.hhmobile.extmem.graph.BlockEncoding;
import org.mapsforge.preprocessing.routing.hhmobile.extmem.graph.BlockedGraphSerializer;
import org.mapsforge.preprocessing.routing.hhmobile.graph.LevelGraph;
import org.mapsforge.preprocessing.routing.hhmobile.util.BitArrayOutputStream;
import org.mapsforge.preprocessing.routing.highwayHierarchies.util.Serializer;

public class BinaryFileWriter {

	public final static String HEADER_MAGIC = "# mapsforge hh #";
	public final static int HEADER_LENGTH = 4096;

	private final static int BUFFER_SIZE = 16384 * 1000;

	public static void writeBinaryFile(LevelGraph levelGraph, IClustering[] clustering,
			File targetFile, String comment, int indexGroupSizeThreshold) throws IOException {

		// temporary files

		File fClusterBlocks = new File(targetFile.getAbsolutePath() + ".blocks");
		File fBlockPointerIdx = new File(targetFile.getAbsolutePath() + ".blockIdx");
		File fRTree = new File(targetFile.getAbsolutePath() + ".blockIdx");

		// write data to temporary files

		BlockEncoding blockEnc = BlockedGraphSerializer.writeBlockedGraph(fClusterBlocks,
				fBlockPointerIdx, levelGraph, clustering, indexGroupSizeThreshold);

		// components location within file

		long startAddrClusterBlocks = HEADER_LENGTH;
		long endAddrClusterBlocks = startAddrClusterBlocks + fClusterBlocks.length();

		long startAddrBlockPointerIdx = endAddrClusterBlocks;
		long endAddrBlockPointerIdx = startAddrBlockPointerIdx + fBlockPointerIdx.length();

		long startAddrRTree = endAddrBlockPointerIdx;
		long endAddrRTree = startAddrRTree + fBlockPointerIdx.length();

		// ---------------- SERIALIZE HEADER -----------------

		byte[] header = new byte[HEADER_LENGTH];
		try {
			BitArrayOutputStream bitStream = new BitArrayOutputStream(header);

			bitStream.write(HEADER_MAGIC.getBytes());

			// cluster blocks
			bitStream.writeLong(startAddrClusterBlocks);
			bitStream.writeByte(blockEnc.bitsPerClusterId);
			bitStream.writeByte(blockEnc.bitsPerVertexOffset);
			bitStream.writeByte(blockEnc.bitsPerEdgeCount);
			bitStream.writeByte(blockEnc.bitsPerNeighborhood);
			bitStream.writeByte(blockEnc.numGraphLevels);
			bitStream.writeLong(endAddrClusterBlocks);

			// block pointer index
			bitStream.writeLong(startAddrBlockPointerIdx);
			bitStream.writeLong(endAddrBlockPointerIdx);

			// r-tree
			bitStream.writeLong(startAddrRTree);
			bitStream.writeLong(endAddrRTree);

			bitStream.writeInt(comment.getBytes("utf-8").length);
			bitStream.write(comment.getBytes("utf-8"));
		} catch (IOException e) {
			throw new RuntimeException(
					"header size exceeded, increase header size! cannot build file.");
		}

		// ---------------- WRITE THE BINARY FILE --------------------------

		DataOutputStream out = new DataOutputStream(new BufferedOutputStream(
				new FileOutputStream(targetFile)));

		// write header
		out.write(header);

		// write cluster blocks
		writeFile(fClusterBlocks, out);

		// write cluster blocks
		writeFile(fBlockPointerIdx, out);

		// write cluster blocks
		writeFile(fRTree, out);

		out.flush();
		out.close();

		// clean up
		fClusterBlocks.delete();
		fBlockPointerIdx.delete();
		fRTree.delete();
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

	public static void main(String[] args) throws IOException, ClassNotFoundException {
		String map = "Ger";

		System.out.print("reading input data (" + map + ") ... ");
		LevelGraph levelGraph = Serializer.deserialize(new File("g" + map));
		KCenterClustering[] clustering = Serializer.deserialize(new File("c" + map));
		System.out.println("ready!");

		File file = new File(map + ".mobile_hh");
		int indexGroupSizeThreshold = 100;

		writeBinaryFile(levelGraph, clustering, file, "test", indexGroupSizeThreshold);
	}
}
