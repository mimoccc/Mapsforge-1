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

/**
 * A structure for holding edge data of the highway hierarchies routing graph.
 */
public class HHEdge {

	int sourceId, targetId, targetIdZeroLvl;
	int weight;
	boolean isShortcut, isForward, isBackward, isCore;

	HHEdge() {
		// no initialization due to using object pooling.
	}

	/**
	 * @return returns the unique identifier of the source vertex.
	 */
	public int getSourceId() {
		return sourceId;
	}

	/**
	 * 
	 * @return returns the unique identifier of the target vertex.
	 */
	public int getTargetId() {
		return targetId;
	}

	/**
	 * @return returns the edge weight of unkown metric.
	 */
	public int getWeight() {
		return weight;
	}

	boolean isShortcut() {
		return isShortcut;
	}

	boolean isForward() {
		return isForward;
	}

	boolean isBackward() {
		return isBackward;
	}

	boolean isCore() {
		return isCore;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(HHEdge.class.getName() + " (\n");
		sb.append("  targetId = " + targetId + "\n");
		sb.append("  targetIdLvlZero = " + targetIdZeroLvl + "\n");
		sb.append("  weight = " + weight + "\n");
		sb.append("  isShortcut = " + isShortcut + "\n");
		sb.append("  isForward = " + isForward + "\n");
		sb.append("  isBackward = " + isBackward + "\n");
		sb.append("  isCore = " + isCore + "\n");
		sb.append("(");
		return sb.toString();
	}
}
