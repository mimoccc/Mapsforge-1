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

final class EdgeEntry {

	public final int weight;
	public final boolean isCore;
	public final boolean isForward;
	public final boolean isBackward;
	public final IndirectVertexPointer target;

	public EdgeEntry(int weight, boolean isCore, boolean isForward, boolean isBackward,
			IndirectVertexPointer target) {
		this.weight = weight;
		this.isCore = isCore;
		this.isForward = isForward;
		this.isBackward = isBackward;
		this.target = target;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(EdgeEntry.class.getName() + " (\n");
		sb.append("  weight = " + weight + "\n");
		sb.append("  isCore = " + isCore + "\n");
		sb.append("  isForward = " + isCore + "\n");
		sb.append("  isBackward = " + isCore + "\n");
		sb.append("  target = " + target + "\n");
		sb.append(")");
		return sb.toString();
	}
}
