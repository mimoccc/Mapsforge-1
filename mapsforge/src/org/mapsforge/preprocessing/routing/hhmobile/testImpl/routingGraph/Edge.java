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

public class Edge {
	private final int targetId, targetIdLvlZero;
	private final int weight;
	private final boolean isShortcut, isForward, isBackward, isCore;

	public Edge(int targetId, int targetIdLvlZero, int weight, boolean isShortcut,
			boolean isForward, boolean isBackward, boolean isCore) {
		this.targetId = targetId;
		this.targetIdLvlZero = targetIdLvlZero;
		this.weight = weight;
		this.isShortcut = isShortcut;
		this.isForward = isForward;
		this.isBackward = isBackward;
		this.isCore = isCore;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(Edge.class.getName() + " (\n");
		sb.append("  targetId = " + targetId + "\n");
		sb.append("  targetIdLvlZero = " + targetIdLvlZero + "\n");
		sb.append("  weight = " + weight + "\n");
		sb.append("  isShortcut = " + isShortcut + "\n");
		sb.append("  isForward = " + isForward + "\n");
		sb.append("  isBackward = " + isBackward + "\n");
		sb.append("  isCore = " + isCore + "\n");
		sb.append("(");
		return sb.toString();
	}

	public int getTargetId() {
		return targetId;
	}

	public int getTargetIdLvlZero() {
		return targetIdLvlZero;
	}

	public int getWeight() {
		return weight;
	}

	public boolean isShortcut() {
		return isShortcut;
	}

	public boolean isForward() {
		return isForward;
	}

	public boolean isBackward() {
		return isBackward;
	}

	public boolean isCore() {
		return isCore;
	}
}
