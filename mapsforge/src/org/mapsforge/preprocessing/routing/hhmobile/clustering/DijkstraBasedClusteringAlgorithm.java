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
package org.mapsforge.preprocessing.routing.hhmobile.clustering;

import java.util.HashMap;
import java.util.Random;

import org.mapsforge.preprocessing.routing.hhmobile.clustering.DirectedWeightedStaticArrayGraph.Edge;
import org.mapsforge.preprocessing.routing.hhmobile.clustering.QuadTreeClustering.QuadTreeCluster;
import org.mapsforge.preprocessing.routing.highwayHierarchies.util.arrays.BitArray;
import org.mapsforge.preprocessing.routing.highwayHierarchies.util.prioQueue.BinaryMinHeap;
import org.mapsforge.preprocessing.routing.highwayHierarchies.util.prioQueue.IBinaryHeapItem;

public class DijkstraBasedClusteringAlgorithm {

	public DijkstraBasedClusteringAlgorithm() {

	}

	public QuadTreeClustering computeClustering(DirectedWeightedStaticArrayGraph graph,
			int verticesPerCluster) {
		// map vertex id to heap item of enqueued vertex
		HashMap<Integer, HeapItem> enqueuedVertices = new HashMap<Integer, HeapItem>();

		// initialize heap
		BinaryMinHeap<HeapItem, Integer> queue = new BinaryMinHeap<HeapItem, Integer>(10000);

		// select Random start node and enqueue
		Random rnd = new Random();
		int[] potentianlVertices = graph.getConnectedVertices();
		int s = potentianlVertices[rnd.nextInt(potentianlVertices.length)];
		HeapItem item = new HeapItem(s, 0, s);
		queue.insert(item);
		enqueuedVertices.put(s, item);

		QuadTreeClustering clustering = new QuadTreeClustering(graph.numVertices() - 1);
		BitArray visited = new BitArray(graph.numVertices());

		// dijkstra loop
		while (!queue.isEmpty()) {
			// dequeue vertex u
			HeapItem uItem = queue.extractMin();
			int u = uItem.vertexId;
			enqueuedVertices.remove(u);
			visited.set(u);

			// create new cluster
			if (clustering.getCluster(u) == null) {
				createNewCluster(graph, clustering, u, verticesPerCluster);
			}

			// relax adjacent edges
			Edge[] adjEdges = graph.getOutboundEdges(graph.getVertex(u));
			for (int i = 0; i < adjEdges.length; i++) {
				int weight = adjEdges[i].getWeight();
				int v = adjEdges[i].getTargetId();

				// relax edge if v was not already visited
				if (!visited.get(v)) {
					HeapItem vItem = enqueuedVertices.get(v);
					if (vItem == null) {
						vItem = new HeapItem(v, uItem.distance + weight, u);
						queue.insert(vItem);
						enqueuedVertices.put(v, vItem);
					} else {
						if (uItem.distance + weight < vItem.distance) {
							queue.decreaseKey(vItem, uItem.distance + weight);
							vItem.parent = u;
						}
					}
				}
			}
		}
		return clustering;
	}

	private void createNewCluster(DirectedWeightedStaticArrayGraph graph,
			QuadTreeClustering clustering, int s, int clusterSize) {
		QuadTreeCluster cluster = clustering.addCluster();

		// map vertex id to heap item of enqueued vertex
		HashMap<Integer, HeapItem> enqueuedVertices = new HashMap<Integer, HeapItem>();

		// initialize heap
		BinaryMinHeap<HeapItem, Integer> queue = new BinaryMinHeap<HeapItem, Integer>(10000);

		// enqueue start node s
		HeapItem item = new HeapItem(s, 0, s);
		queue.insert(item);
		enqueuedVertices.put(s, item);

		BitArray visited = new BitArray(graph.numVertices());

		// dijkstra loop
		while (!queue.isEmpty() && cluster.size() < clusterSize) {
			// dequeue vertex u
			HeapItem uItem = queue.extractMin();
			int u = uItem.vertexId;
			enqueuedVertices.remove(u);
			visited.set(u);

			// create new cluster
			if (clustering.getCluster(u) == null) {
				cluster.addVertex(u);
			}

			// relax adjacent edges
			Edge[] adjEdges = graph.getOutboundEdges(graph.getVertex(u));
			for (int i = 0; i < adjEdges.length; i++) {
				int weight = adjEdges[i].getWeight();
				int v = adjEdges[i].getTargetId();

				// relax edge if v was not already visited
				if (!visited.get(v)) {
					HeapItem vItem = enqueuedVertices.get(v);
					if (vItem == null) {
						vItem = new HeapItem(v, uItem.distance + weight, u);
						queue.insert(vItem);
						enqueuedVertices.put(v, vItem);
					} else {
						if (uItem.distance + weight < vItem.distance) {
							queue.decreaseKey(vItem, uItem.distance + weight);
							vItem.parent = u;
						}
					}
				}
			}
		}

	}

	private class HeapItem implements IBinaryHeapItem<Integer> {

		private final int vertexId;
		private int heapIndex, distance, parent;

		public HeapItem(int vertexId, int distance, int parent) {
			this.heapIndex = -1;
			this.distance = distance;
			this.vertexId = vertexId;
			this.parent = parent;
		}

		@Override
		public int getHeapIndex() {
			return heapIndex;
		}

		@Override
		public Integer getHeapKey() {
			return distance;
		}

		@Override
		public void setHeapIndex(int idx) {
			this.heapIndex = idx;

		}

		@Override
		public void setHeapKey(Integer key) {
			this.distance = key;
		}
	}
}
