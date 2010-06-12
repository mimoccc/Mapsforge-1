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

import org.mapsforge.preprocessing.routing.hhmobile.util.Utils;

final class VertexEntry {

	public final IndirectVertexPointer[] interLevelPointers;
	public final int neighborhood;
	public final int edgeOffset;
	public final int longitude;
	public final int latitude;

	public VertexEntry(IndirectVertexPointer[] interLevelPointers, int neighborhood,
			int edgeOffset, int longitude, int latitude) {
		this.interLevelPointers = interLevelPointers;
		this.neighborhood = neighborhood;
		this.edgeOffset = edgeOffset;
		this.longitude = longitude;
		this.latitude = latitude;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(VertexEntry.class.getName() + " (\n");
		sb.append("  interLevelPointers = {" + Utils.arrToString(interLevelPointers) + "}\n");
		sb.append("  neighborhood = " + neighborhood + "\n");
		sb.append("  edgeOffset = " + edgeOffset + "\n");
		sb.append("  longitude = " + longitude + "\n");
		sb.append("  latitude = " + latitude + "\n");
		sb.append(")");
		return sb.toString();
	}
}
