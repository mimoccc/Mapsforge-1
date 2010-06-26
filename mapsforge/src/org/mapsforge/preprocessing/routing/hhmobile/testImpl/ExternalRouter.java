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
package org.mapsforge.preprocessing.routing.hhmobile.testImpl;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import org.mapsforge.preprocessing.routing.hhmobile.binaryFile.BinaryFileHeader;
import org.mapsforge.preprocessing.routing.hhmobile.binaryFile.graph.BlockPointerIndex;
import org.mapsforge.preprocessing.routing.hhmobile.binaryFile.graph.BlockedGraphHeader;
import org.mapsforge.preprocessing.routing.hhmobile.testImpl.routingGraph.Block;
import org.mapsforge.preprocessing.routing.hhmobile.testImpl.routingGraph.BlockReader;
import org.mapsforge.preprocessing.routing.hhmobile.testImpl.routingGraph.Edge;
import org.mapsforge.preprocessing.routing.hhmobile.testImpl.routingGraph.Vertex;
import org.mapsforge.preprocessing.routing.highwayHierarchies.util.renderer.RendererV2;
import org.mapsforge.preprocessing.util.GeoCoordinate;
import org.mapsforge.server.routing.IRouter;
import org.mapsforge.server.routing.RouterFactory;

public class ExternalRouter {

	private final RandomAccessFile raf;
	private final BinaryFileHeader header;
	private final BlockedGraphHeader graphHeader;
	private final BlockPointerIndex blockIdx;
	private final BlockReader blockReader;

	public ExternalRouter(File file) throws IOException {
		this.raf = new RandomAccessFile(file, "r");

		this.header = new BinaryFileHeader(read(0, BinaryFileHeader.HEADER_LENGTH));
		if (!header.isHeaderValid()) {
			throw new IOException("Header not valid!");
		}

		this.graphHeader = new BlockedGraphHeader(read(header.startAddrGraphHeader,
				header.endAddrGraphHeader));
		this.blockIdx = new BlockPointerIndex(read(header.startAddrBlockPointerIdx,
				header.endAddrBlockPointerIdx));
		this.blockReader = new BlockReader(file, header.startAddrClusterBlocks, graphHeader,
				blockIdx);
		System.out.println(header);
		System.out.println(graphHeader);
		System.out.println(blockIdx);

		IRouter router = RouterFactory.getRouter();
		RendererV2 renderer = new RendererV2(1024, 768, router, Color.WHITE, Color.BLACK);

		long time = System.currentTimeMillis();
		for (int j = 0; j < blockIdx.size(); j++) {
			Block block = this.blockReader.readBlock(j);
			// System.out.println(block);
			for (int i = 0; i < block.getNumVertices(); i++) {
				Vertex v = block.getVertex(i);
				// System.out.println(v);
				renderer.addCircle(new GeoCoordinate(v.getLat(), v.getLon()), Color.GREEN);
				for (Edge e : v.getOutboundEdges()) {
					System.out.println(e);
				}

			}
		}
		System.out.println("time " + (System.currentTimeMillis() - time) + "ms.");
	}

	private byte[] read(long startAddr, long endAddr) throws IOException {
		byte[] b = new byte[(int) (endAddr - startAddr)];
		raf.seek(startAddr);
		raf.readFully(b);
		return b;
	}

	public static void main(String[] args) throws IOException {
		String map = "berlin";

		ExternalRouter router = new ExternalRouter(new File(map + ".mobile_hh"));

	}
}
