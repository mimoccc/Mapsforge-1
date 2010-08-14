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
package org.mapsforge.android.routing.hh;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.LinkedList;
import java.util.Random;

import org.mapsforge.core.GeoCoordinate;
import org.mapsforge.core.WGS84;
import org.mapsforge.preprocessing.routing.hhmobile.util.HHGlobals;
import org.mapsforge.preprocessing.routing.highwayHierarchies.util.renderer.RendererV2;
import org.mapsforge.server.routing.RouterFactory;

class RoutingGraph {

	public final static byte[] HEADER_MAGIC = HHGlobals.BINARY_FILE_HEADER_MAGIC;
	public final static int HEADER_LENGTH = HHGlobals.BINARY_FILE_HEADER_LENGTH;

	private final RleBlockReader blockReader;
	private final Cache<RleBlock> cache;
	private final Random rnd = new Random(1333);
	private final StaticRTree rtree;
	public int[] numBlockReads;
	public long ioTime;
	public long shiftTime;

	public RoutingGraph(File hhBinaryFile, Cache<RleBlock> cache) throws IOException {
		RandomAccessFile raf = new RandomAccessFile(hhBinaryFile, "r");

		// ------------- READ BINARY FILE HEADER --------------

		// read the header from secondary storage
		byte[] header = new byte[HEADER_LENGTH];
		raf.seek(0);
		raf.readFully(header);

		// extract data from header and verify the header magic
		DataInputStream iStream = new DataInputStream(new ByteArrayInputStream(header));
		byte[] headerMagic = new byte[HEADER_MAGIC.length];
		iStream.read(headerMagic);

		for (int i = 0; i < headerMagic.length; i++) {
			if (headerMagic[i] != HEADER_MAGIC[i]) {
				throw new IOException("invalid header.");
			}
		}

		long startAddrGraph = iStream.readLong();
		/* long endAddrGraph = */iStream.readLong();
		long startAddrBlockIndex = iStream.readLong();
		long endAddrBlockIndex = iStream.readLong();
		long startAddrRTree = iStream.readLong();
		/* long endAddrRTree = */iStream.readLong();

		AddressLookupTable blockIdx = new AddressLookupTable(startAddrBlockIndex,
				endAddrBlockIndex, hhBinaryFile);
		for (int i = 0; i < blockIdx.size(); i++) {
			blockIdx.getPointer(i);
		}
		raf.close();

		this.blockReader = new RleBlockReader(hhBinaryFile, startAddrGraph, blockIdx);
		this.rtree = new StaticRTree(hhBinaryFile, startAddrRTree);
		this.cache = cache;
		this.numBlockReads = new int[blockReader.numLevels];
		this.ioTime = 0;
		this.shiftTime = 0;
	}

	public int numLevels() {
		return blockReader.numLevels;
	}

	public boolean getNearestVertex(GeoCoordinate c, double maxDistanceMeters, Vertex buff)
			throws IOException {
		double alphaLon = (maxDistanceMeters / WGS84.EQUATORIALRADIUS) * 180;
		double alphaLat = (maxDistanceMeters / WGS84.EQUATORIALRADIUS) * 180; // TODO:
		int minLon = GeoCoordinate.doubleToInt(c.getLongitude() - alphaLon);
		int maxLon = GeoCoordinate.doubleToInt(c.getLongitude() + alphaLon);
		int minLat = GeoCoordinate.doubleToInt(c.getLatitude() - alphaLat);
		int maxLat = GeoCoordinate.doubleToInt(c.getLatitude() + alphaLat);
		LinkedList<Integer> blockIds = rtree.overlaps(minLon, maxLon, minLat, maxLat);
		Vertex v = new Vertex();
		double dBest = Double.MAX_VALUE;
		for (int blockId : blockIds) {
			RleBlock block = getBlock(blockId);
			int n = block.getNumVertices();
			for (int i = 0; i < n; i++) {
				block.getVertex(i, v);
				double distance = GeoCoordinate.sphericalDistance(c.getLatitudeE6(), c
						.getLongitudeE6(), v.getLat(), v.getLon());
				if (dBest > distance) {
					dBest = distance;
					block.getVertex(i, buff);
				}
			}
		}
		return dBest != Double.MAX_VALUE;
	}

	public void getRandomVertex(int lvl, Vertex buff) throws IOException {
		int blockId = rnd.nextInt(blockReader.getNumBlocks());
		getVertex(blockReader.getVertexId(blockId, 0), buff);
		while (buff.getLvl() != lvl) {
			blockId = rnd.nextInt(blockReader.getNumBlocks());
			getVertex(blockReader.getVertexId(blockId, 0), buff);
		}
	}

	public void getVertex(int vertexId, Vertex buff) throws IOException {
		int blockId = blockReader.getBlockId(vertexId);
		RleBlock block = getBlock(blockId);
		int vertexOffset = blockReader.getVertexOffset(vertexId);
		long time = System.nanoTime();
		block.getVertex(vertexOffset, buff);
		shiftTime += System.nanoTime() - time;
	}

	public void getOutboundEdge(Vertex v, int i, Edge buff) throws IOException {
		int blockId = blockReader.getBlockId(v.getId());
		RleBlock block = getBlock(blockId);
		long time = System.nanoTime();
		block.getOutboundEdge(v, i, buff);
		shiftTime += System.nanoTime() - time;
	}

	private RleBlock getBlock(int blockId) throws IOException {
		RleBlock block = cache.getItem(blockId);
		if (block == null) {
			long time = System.nanoTime();
			block = blockReader.readBlock(blockId);
			ioTime += System.nanoTime() - time;

			numBlockReads[block.getLevel()]++;
			cache.putItem(block);
		}
		return block;
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

		RoutingGraph router = new RoutingGraph(new File(map + ".hhmobile"),
				new DummyCache<RleBlock>());
		Vertex s = new Vertex();
		router.getRandomVertex(0, s);
		Vertex t = new Vertex();
		router.getRandomVertex(0, t);
		System.out.println(s);
		System.out.println(t);

		RendererV2 renderer = new RendererV2(1024, 768, RouterFactory.getRouter(), Color.WHITE,
				Color.BLACK);
		renderer.addCircle(new GeoCoordinate(s.getLat(), s.getLon()), Color.RED);
		renderer.addCircle(new GeoCoordinate(t.getLat(), t.getLon()), Color.RED);
	}
}
