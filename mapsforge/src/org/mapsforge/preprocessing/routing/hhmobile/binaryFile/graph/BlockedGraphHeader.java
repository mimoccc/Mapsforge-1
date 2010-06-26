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
package org.mapsforge.preprocessing.routing.hhmobile.binaryFile.graph;

import java.io.IOException;

import org.mapsforge.preprocessing.routing.hhmobile.util.BitArrayInputStream;
import org.mapsforge.preprocessing.routing.hhmobile.util.BitArrayOutputStream;

public final class BlockedGraphHeader {

	public final static int HEADER_SIZE = 4096; // bytes

	public final byte bpClusterId, bpVertexCount, bpEdgeCount, bpNeighborhood, numLevels;

	public BlockedGraphHeader(byte bpClusterId, byte bpVertexCount, byte bpEdgeCount,
			byte bpNeighborhood, byte numLevels) {
		this.bpClusterId = bpClusterId;
		this.bpVertexCount = bpVertexCount;
		this.bpEdgeCount = bpEdgeCount;
		this.bpNeighborhood = bpNeighborhood;
		this.numLevels = numLevels;
	}

	public BlockedGraphHeader(byte[] buff) throws IOException {
		BitArrayInputStream stream = new BitArrayInputStream(buff);
		this.bpClusterId = stream.readByte();
		this.bpVertexCount = stream.readByte();
		this.bpEdgeCount = stream.readByte();
		this.bpNeighborhood = stream.readByte();
		this.numLevels = stream.readByte();
	}

	public byte[] serialize() {
		try {
			byte[] buff = new byte[HEADER_SIZE];
			BitArrayOutputStream stream = new BitArrayOutputStream(buff);
			stream.writeByte(bpClusterId);
			stream.writeByte(bpVertexCount);
			stream.writeByte(bpEdgeCount);
			stream.writeByte(bpNeighborhood);
			stream.writeByte(numLevels);
			return buff;
		} catch (IOException e) {
			throw new RuntimeException("need to increase BlockedGraphHeader size");
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(BlockedGraphHeader.class.getName() + " (\n");
		sb.append("  bpClusterId = " + bpClusterId + "\n");
		sb.append("  bpVertexCount = " + bpVertexCount + "\n");
		sb.append("  bpEdgeCount = " + bpEdgeCount + "\n");
		sb.append("  bpNeighborhood = " + bpNeighborhood + "\n");
		sb.append("  numLevels = " + numLevels + "\n");
		sb.append(")");
		return sb.toString();
	}
}
