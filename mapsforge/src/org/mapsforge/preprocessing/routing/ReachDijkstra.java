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

import java.util.Arrays;

public final class ReachDijkstra {

	public static void computeReaches(IGraph graph, INode origin, double[] reach) {

		UpdatablePriorityQueue<Node> queue = new UpdatablePriorityQueue<Node>(graph.size());

		int[] parent = new int[graph.size()];
		Arrays.fill(parent, -1);
		double[] dist = new double[graph.size()];
		Arrays.fill(dist, Double.POSITIVE_INFINITY);
		dist[origin.id()] = 0;
		queue.add(new Node(origin.id(), 0));

		Node currentNode = null;
		Iterable<IEdge> currentAdjacentEdges;
		double updatedDist;
		Node currentWeight;

		double distanceSourceTarget;
		int next;

		while (!queue.isEmpty()) {
			// remove next node from queue
			currentNode = queue.poll();
			// first node in the queue is now settled, i.e. the shortest path
			// has been found
			dist[currentNode.id()] = currentNode.distance;

			// all following nodes are unreachable
			if (currentNode.distance == Double.POSITIVE_INFINITY)
				break;

			// get all adjacent nodes and the weight of the edges leading to
			// them
			currentAdjacentEdges = graph.getAdjacentEdges(currentNode);

			if (currentAdjacentEdges == null)
				continue;

			// calculate the distance from the source through the currently
			// settled node
			// to all the adjacent nodes
			// update distances if we found a shorter path and adjust the parent
			// node
			for (IEdge currentEdge : currentAdjacentEdges) {

				// node already settled?
				if (dist[currentEdge.target().id()] < Double.POSITIVE_INFINITY)
					continue;

				updatedDist = currentEdge.weight() + dist[currentNode.id()];
				currentWeight = queue.get(currentEdge.target().id());

				// node already seen?
				if (currentWeight == null) {
					// first time we reach this node
					queue.add(new Node(currentEdge.target().id(), updatedDist));
					parent[currentEdge.target().id()] = currentNode.id();
				} else if (currentWeight.distance > updatedDist) {
					// we have already seen this node and we found a shorter
					// path
					currentWeight.distance = updatedDist;
					queue.update(currentWeight);
					parent[currentEdge.target().id()] = currentNode.id();
				}
			}

			// update reach values for nodes on the currently encountered
			// shortest path
			// traverse the currently found path from "target" to "source"
			distanceSourceTarget = dist[currentNode.id()];
			next = parent[currentNode.id()];
			while (next > 0) {

				// we need to synchronize here because of
				// concurrent access on the reach array
				// synchronized (reach) {
				reach[next] = Math.max(reach[next], Math.min(distanceSourceTarget - dist[next],
						dist[next]));
				// }

				next = parent[next];
			}
		}
	}

	public static class Node implements Comparable<Node>, INode {
		public int id;
		public double distance;

		public Node(int id, double weight) {
			super();
			this.id = id;
			this.distance = weight;
		}

		@Override
		public int compareTo(Node other) {
			return Double.compare(distance, other.distance);
		}

		@Override
		public boolean equals(Object o) {
			if (o == null)
				return false;

			if (!(o instanceof Node))
				return false;

			Node other = (Node) o;
			return this.id == other.id;
		}

		@Override
		public int hashCode() {
			return id;
		}

		@Override
		public int id() {
			return id;
		}

		@Override
		public String toString() {
			return "Point [id=" + id + ", weight=" + distance + "]";
		}

	}

}
