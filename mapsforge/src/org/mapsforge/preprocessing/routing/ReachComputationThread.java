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
package org.mapsforge.preprocessing.routing;

public class ReachComputationThread extends Thread {

	private int partitionOffset;
	private int partitionLength;
	private double[] reaches;

	private IGraph graph;

	private int i;

	public ReachComputationThread(IGraph graph, int partitionOffset, int partitionLength,
			double[] reaches) {
		super();
		this.graph = graph;
		this.partitionOffset = partitionOffset;
		this.partitionLength = partitionLength;
		this.reaches = reaches;
	}

	@Override
	public void run() {
		for (i = 0; i < partitionLength; i++) {
			ReachDijkstra.computeReaches(graph, new Node(i + partitionOffset), reaches);
			// if(i % 25 == 0)
			// System.out.println(this.getName() + ": " + i);
		}
	}

	public int progress() {
		return i;
	}
}
