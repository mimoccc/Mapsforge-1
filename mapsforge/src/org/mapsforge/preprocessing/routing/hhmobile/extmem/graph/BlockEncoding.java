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


public final class BlockEncoding {

	public final byte bitsPerClusterId, bitsPerVertexOffset, bitsPerEdgeCount,
			bitsPerNeighborhood, numGraphLevels;

	public BlockEncoding(byte bitsPerClusterId, byte bitsPerVertexOffset,
			byte bitsPerEdgeCount, byte bitsPerNeighborhood, byte numGraphLevels) {
		this.bitsPerClusterId = bitsPerClusterId;
		this.bitsPerVertexOffset = bitsPerVertexOffset;
		this.bitsPerEdgeCount = bitsPerEdgeCount;
		this.bitsPerNeighborhood = bitsPerNeighborhood;
		this.numGraphLevels = numGraphLevels;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(BlockEncoding.class.getName() + " (\n");
		sb.append("  bitsPerClusterId = " + bitsPerClusterId + "\n");
		sb.append("  bitsPerVertexOffset = " + bitsPerVertexOffset + "\n");
		sb.append("  bitsPerEdgeCount = " + bitsPerEdgeCount + "\n");
		sb.append("  bitsPerNeighborhood = " + bitsPerNeighborhood + "\n");
		sb.append("  numGraphLevels = " + numGraphLevels + "\n");
		sb.append(")");
		return sb.toString();
	}
}
