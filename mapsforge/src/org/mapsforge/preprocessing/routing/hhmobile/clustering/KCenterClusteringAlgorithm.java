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

import gnu.trove.set.hash.THashSet;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Random;

import org.mapsforge.preprocessing.routing.hhmobile.clustering.DirectedWeightedStaticArrayGraph.Edge;
import org.mapsforge.preprocessing.routing.hhmobile.clustering.KCenterClustering.Cluster;
import org.mapsforge.preprocessing.routing.highwayHierarchies.util.arrays.BitArray;
import org.mapsforge.preprocessing.routing.highwayHierarchies.util.prioQueue.BinaryMinHeap;
import org.mapsforge.preprocessing.routing.highwayHierarchies.util.prioQueue.IBinaryHeapItem;

public class KCenterClusteringAlgorithm {

	public static final int HEURISTIC_MIN_SIZE = 0;
	public static final int HEURISTIC_MIN_RADIUS = 1;

	private static final int HEURISTIC_DEFAULT = HEURISTIC_MIN_SIZE;

	public KCenterClustering computeClustering(DirectedWeightedStaticArrayGraph graph, int k,
			int heuristic) {
		int k_ = (int) Math.rint(k * (Math.log(k) / Math.log(2)));
		System.out.println("computing k-center clustering (k = " + k + ", k' = " + k_ + ")");

		System.out.println("randomly choosing k' centers");
		KCenterClustering clustering = chooseRandomCenters(graph, k_);

		System.out.println("expanding the clusters from their centers");
		expandClusters(graph, clustering);

		System.out.println("sampling down to k = " + Math.min(k, k_) + " clusters");
		sampleDown(graph, clustering, k, k_, heuristic);
		return clustering;
	}

	private KCenterClustering chooseRandomCenters(DirectedWeightedStaticArrayGraph graph, int k_) {
		KCenterClustering clustering = new KCenterClustering(graph.numVertices());

		Random rnd = new Random(1);
		int[] potentialIds = graph.getConnectedVertices();

		for (int i = 0; i < k_; i++) {
			int centerVertex = potentialIds[rnd.nextInt(potentialIds.length)];
			while (clustering.getCluster(centerVertex) != null) {
				centerVertex = potentialIds[rnd.nextInt(potentialIds.length)];
			}
			clustering.addCluster(centerVertex);
		}
		return clustering;
	}

	private void expandClusters(DirectedWeightedStaticArrayGraph graph,
			KCenterClustering clustering) {
		// map vertex id to heap item of enqueued vertex
		HashMap<Integer, HeapItem> enqueuedVertices = new HashMap<Integer, HeapItem>();

		// initialize heap and enqueue centers
		BinaryMinHeap<HeapItem, Integer> queue = new BinaryMinHeap<HeapItem, Integer>(10000);
		for (Cluster c : clustering.getClusters()) {
			HeapItem item = new HeapItem(c.getCenterVertex(), 0, c.getCenterVertex());
			queue.insert(item);
			enqueuedVertices.put(c.getCenterVertex(), item);
		}

		// keep one bit for every visited vertex
		BitArray visited = new BitArray(graph.numVertices());

		// dijkstra loop
		while (!queue.isEmpty()) {
			// dequeue vertex u
			HeapItem uItem = queue.extractMin();
			int u = uItem.vertexId;
			enqueuedVertices.remove(u);
			visited.set(u);

			// add u to the cluster his it parent belongs to
			if (uItem.parent != u) {
				clustering.getCluster(uItem.parent).addVertex(u, uItem.distance);
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

	private void sampleDown(DirectedWeightedStaticArrayGraph graph,
			KCenterClustering clustering, int k, int k_, int heuristik) {
		while (k_ > k) {
			System.out.println(k_);
			Cluster cluster = chooseClusterForRemoval(graph, clustering, heuristik);
			removeClusterAndRearrange(graph, clustering, cluster);
			k_--;
		}
	}

	private void removeClusterAndRearrange(DirectedWeightedStaticArrayGraph graph,
			KCenterClustering clustering, Cluster cluster) {
		// remove the cluster
		Cluster[] adjClusters = getAdjacentClusters(graph, clustering, cluster);
		int clusterSize = cluster.size();
		clustering.removeCluster(cluster);

		// disseminate vertices to neighbor clusters using dijkstra:

		// map vertex id to heap item of enqueued vertex
		HashMap<Integer, HeapItem> enqueuedVertices = new HashMap<Integer, HeapItem>();

		// initialize heap and enqueue centers
		BinaryMinHeap<HeapItem, Integer> queue = new BinaryMinHeap<HeapItem, Integer>(10000);
		for (Cluster c : adjClusters) {
			HeapItem item = new HeapItem(c.getCenterVertex(), 0, c.getCenterVertex());
			queue.insert(item);
			enqueuedVertices.put(c.getCenterVertex(), item);
		}

		// keep one bit for every visited vertex
		BitArray visited = new BitArray(graph.numVertices());

		int disseminatedVertices = 0;
		// dijkstra loop (skip if all vertices of cluster are disseminated)
		while (!queue.isEmpty() && disseminatedVertices < clusterSize) {
			// dequeue vertex u
			HeapItem uItem = queue.extractMin();
			int u = uItem.vertexId;
			enqueuedVertices.remove(u);
			visited.set(u);

			// add u to the cluster his it parent belongs to, if u does not belong to any
			// cluster
			if (uItem.parent != u && clustering.getCluster(u) == null) {
				clustering.getCluster(uItem.parent).addVertex(u, uItem.distance);
				disseminatedVertices++;
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

	private Cluster[] getAdjacentClusters(DirectedWeightedStaticArrayGraph graph,
			KCenterClustering clustering, Cluster cluster) {
		THashSet<Cluster> set = new THashSet<Cluster>();
		for (int v : cluster.getVertices()) {
			for (Edge e : graph.getOutboundEdges(graph.getVertex(v))) {
				Cluster c = clustering.getCluster(e.getTargetId());
				if (c != null && !c.equals(cluster)) {
					set.add(c);
				}
			}
		}
		Cluster[] adjClusters = new Cluster[set.size()];
		set.toArray(adjClusters);
		return adjClusters;
	}

	private Cluster chooseClusterForRemoval(DirectedWeightedStaticArrayGraph graph,
			KCenterClustering clustering, int heuristik) {
		switch (heuristik) {
			case HEURISTIC_MIN_RADIUS:
				return getMinCluster(clustering, new Comparator<Cluster>() {

					@Override
					public int compare(Cluster c1, Cluster c2) {
						return c1.getRadius() - c2.getRadius();
					}
				});
			case HEURISTIC_MIN_SIZE:
				return getMinCluster(clustering, new Comparator<Cluster>() {

					@Override
					public int compare(Cluster c1, Cluster c2) {
						return c1.size() - c2.size();
					}
				});
			default:
				return chooseClusterForRemoval(graph, clustering, HEURISTIC_DEFAULT);
		}
	}

	private Cluster getMinCluster(KCenterClustering clustering, Comparator<Cluster> comp) {
		Cluster min = clustering.getClusters().iterator().next();
		for (Cluster c : clustering.getClusters()) {
			if (comp.compare(c, min) < 0) {
				min = c;
			}
		}
		return min;
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
