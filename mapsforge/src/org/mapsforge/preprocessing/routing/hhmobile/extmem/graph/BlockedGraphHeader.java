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
package org.mapsforge.preprocessing.routing.hhmobile.extmem.graph;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

final class BlockedGraphHeader {

	public final int bitsPerClusterId, bitsPerVertexOffset, bitsPerEdgeCount,
			bitsPerNeighborhood, numGraphLevels;

	public BlockedGraphHeader(int bitsPerClusterId, int bitsPerVertexOffset,
			int bitsPerEdgeCount, int bitsPerNeighborhood, int numGraphLevels) {
		this.bitsPerClusterId = bitsPerClusterId;
		this.bitsPerVertexOffset = bitsPerVertexOffset;
		this.bitsPerEdgeCount = bitsPerEdgeCount;
		this.bitsPerNeighborhood = bitsPerNeighborhood;
		this.numGraphLevels = numGraphLevels;
	}

	public static BlockedGraphHeader deserialize(byte[] b) throws IOException {
		DataInputStream in = new DataInputStream(new ByteArrayInputStream(b));
		int bitsPerClusterId = in.readInt();
		int bitsPerVertexOffset = in.readInt();
		int bitsPerEdgeCount = in.readInt();
		int bitsPerNeighborhood = in.readInt();
		int numGraphLevels = in.readInt();

		return new BlockedGraphHeader(bitsPerClusterId, bitsPerVertexOffset, bitsPerEdgeCount,
				bitsPerNeighborhood, numGraphLevels);
	}

	public void serialize(OutputStream oStream) throws IOException {
		DataOutputStream out = new DataOutputStream(oStream);
		out.writeInt(bitsPerClusterId);
		out.writeInt(bitsPerVertexOffset);
		out.writeInt(bitsPerEdgeCount);
		out.writeInt(bitsPerNeighborhood);
		out.writeInt(numGraphLevels);
		out.flush();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(BlockedGraphHeader.class.getName() + " (\n");
		sb.append("  bitsPerClusterId = " + bitsPerClusterId + "\n");
		sb.append("  bitsPerVertexOffset = " + bitsPerVertexOffset + "\n");
		sb.append("  bitsPerEdgeCount = " + bitsPerEdgeCount + "\n");
		sb.append("  bitsPerNeighborhood = " + bitsPerNeighborhood + "\n");
		sb.append("  numGraphLevels = " + numGraphLevels + "\n");
		sb.append(")");
		return sb.toString();
	}
}
