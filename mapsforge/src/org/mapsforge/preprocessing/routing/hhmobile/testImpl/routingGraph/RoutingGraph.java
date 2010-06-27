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
package org.mapsforge.preprocessing.routing.hhmobile.testImpl.routingGraph;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Random;

import org.mapsforge.preprocessing.routing.hhmobile.binaryFile.BinaryFileHeader;
import org.mapsforge.preprocessing.routing.hhmobile.binaryFile.graph.BlockPointerIndex;
import org.mapsforge.preprocessing.routing.hhmobile.binaryFile.graph.BlockedGraphHeader;
import org.mapsforge.preprocessing.routing.highwayHierarchies.util.renderer.RendererV2;
import org.mapsforge.preprocessing.util.GeoCoordinate;
import org.mapsforge.server.routing.RouterFactory;

public class RoutingGraph {

	private final int numLevels;
	private final BlockReader blockReader;
	private final ICache cache;
	private final int shiftClusterId;
	private final int bitMask;

	public RoutingGraph(File file, ICache cache) throws IOException {
		RandomAccessFile raf = new RandomAccessFile(file, "r");

		BinaryFileHeader fileHeader = new BinaryFileHeader(read(raf, 0,
				BinaryFileHeader.HEADER_LENGTH));
		if (!fileHeader.isHeaderValid()) {
			throw new IOException("Header not valid!");
		}

		BlockedGraphHeader graphHeader = new BlockedGraphHeader(read(raf,
				fileHeader.startAddrGraphHeader, fileHeader.endAddrGraphHeader));
		BlockPointerIndex blockIdx = new BlockPointerIndex(read(raf,
				fileHeader.startAddrBlockPointerIdx, fileHeader.endAddrBlockPointerIdx));
		raf.close();

		this.numLevels = graphHeader.numLevels;
		this.blockReader = new BlockReader(file, fileHeader.startAddrClusterBlocks,
				graphHeader, blockIdx);
		this.cache = cache;
		this.shiftClusterId = graphHeader.bpVertexCount;
		this.bitMask = getBitmask(shiftClusterId);
	}

	public int numLevels() {
		return numLevels;
	}

	public Vertex getRandomVertex() throws IOException {
		Random rnd = new Random();
		int blockId = rnd.nextInt(blockReader.getNumBlocks());
		Vertex v = getVertex(blockId << shiftClusterId);
		while (v.getLvl() != 0) {
			blockId = rnd.nextInt(blockReader.getNumBlocks());
			v = getVertex(blockId << shiftClusterId);
		}
		return v;
	}

	public Vertex getVertex(int vertexId) throws IOException {
		int blockId = getBlockId(vertexId);
		Block block = cache.getBlock(blockId);
		if (block == null) {
			block = blockReader.readBlock(blockId);
			cache.putBlock(block);
		}
		int vertexOffset = getVertexOffset(vertexId);
		return block.getVertex(vertexOffset);
	}

	public int getBlockId(int vertexId) {
		return vertexId >>> shiftClusterId;
	}

	public int getVertexOffset(int vertexId) {
		return vertexId & bitMask;
	}

	private static int getBitmask(int shiftClusterId) {
		int bMask = 0;
		for (int i = 0; i < shiftClusterId; i++) {
			bMask = (bMask << 1) | 1;
		}
		return bMask;
	}

	private static byte[] read(RandomAccessFile raf, long startAddr, long endAddr)
			throws IOException {
		byte[] b = new byte[(int) (endAddr - startAddr)];
		raf.seek(startAddr);
		raf.readFully(b);
		return b;
	}

	public static void main(String[] args) throws IOException {
		String map = "berlin";

		RoutingGraph router = new RoutingGraph(new File(map + ".mobile_hh"), new DummyCache());
		Vertex s = router.getRandomVertex();
		Vertex t = router.getRandomVertex();
		System.out.println(s);
		System.out.println(t);

		RendererV2 renderer = new RendererV2(1024, 768, RouterFactory.getRouter(), Color.WHITE,
				Color.BLACK);
		renderer.addCircle(new GeoCoordinate(s.getLat(), s.getLon()), Color.RED);
		renderer.addCircle(new GeoCoordinate(t.getLat(), t.getLon()), Color.RED);
	}
}
