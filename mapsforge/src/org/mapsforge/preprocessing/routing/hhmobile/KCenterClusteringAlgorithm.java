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
package org.mapsforge.preprocessing.routing.hhmobile;

import gnu.trove.set.hash.THashSet;
import gnu.trove.set.hash.TIntHashSet;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

import org.mapsforge.preprocessing.routing.hhmobile.KCenterClustering.KCenterCluster;
import org.mapsforge.preprocessing.routing.highwayHierarchies.util.Serializer;
import org.mapsforge.preprocessing.routing.highwayHierarchies.util.prioQueue.BinaryMinHeap;
import org.mapsforge.preprocessing.routing.highwayHierarchies.util.prioQueue.IBinaryHeapItem;
import org.mapsforge.preprocessing.util.DBConnection;

class KCenterClusteringAlgorithm {

	public static final String ALGORITHM_NAME = "k_center";

	private static final int MSG_INT_BUILD_CLUSTERS = 100000;
	private static final int MSG_INT_SAMPLE_DOWN = 1000;

	public static final int HEURISTIC_MIN_SIZE = 0;
	public static final int HEURISTIC_MIN_RADIUS = 1;

	private static final int HEURISTIC_DEFAULT = HEURISTIC_MIN_SIZE;

	public static KCenterClustering[] computeClustering(Graph[] graph,
			int avgVerticesPerCluster, int heuristic) {
		KCenterClustering[] clustering = new KCenterClustering[graph.length];
		for (int i = 0; i < graph.length; i++) {
			clustering[i] = computeClustering(graph[i], graph[i].numVertices()
					/ avgVerticesPerCluster, heuristic);
		}
		return clustering;
	}

	public static KCenterClustering computeClustering(Graph graph, int k, int heuristic) {
		k = Math.max(k, 1);
		int k_ = (int) Math.rint(k * (Math.log(k) / Math.log(8)));
		k_ = Math.max(k, k_);
		System.out.println("computing k-center clustering (k = " + k + ", k' = " + k_ + ")");

		System.out.println("randomly choosing k' centers");
		KCenterClustering clustering = chooseRandomCenters(graph, k_);

		System.out.println("building clusters from centers");
		expandClusters(graph, clustering);

		System.out.println("sampling down to k = " + Math.min(k, k_) + " clusters");
		sampleDown(graph, clustering, k, k_, heuristic);
		return clustering;
	}

	private static KCenterClustering chooseRandomCenters(Graph graph, int k_) {

		Random rnd = new Random(1);
		int maxVertexId = 0;
		int[] ids = new int[graph.numVertices()];
		int offset = 0;
		for (Iterator<? extends Vertex> iter = graph.getVertices(); iter.hasNext();) {
			Vertex v = iter.next();
			ids[offset] = v.getId();
			maxVertexId = Math.max(ids[offset], maxVertexId);
			offset++;
		}
		KCenterClustering clustering = new KCenterClustering(maxVertexId);

		for (int i = 0; i < k_; i++) {
			int centerVertex = ids[rnd.nextInt(ids.length)];
			while (clustering.getCluster(centerVertex) != null) {
				centerVertex = ids[rnd.nextInt(ids.length)];
			}
			clustering.addCluster(centerVertex);
		}
		return clustering;
	}

	private static void expandClusters(Graph graph, KCenterClustering clustering) {
		// map vertex id to heap item of enqueued vertex
		HashMap<Integer, HeapItem> enqueuedVertices = new HashMap<Integer, HeapItem>();

		// initialize heap and enqueue centers
		BinaryMinHeap<HeapItem, Integer> queue = new BinaryMinHeap<HeapItem, Integer>(10000);
		for (KCenterCluster c : clustering.getClusters()) {
			HeapItem item = new HeapItem(c.getCenterVertex(), 0, c.getCenterVertex());
			queue.insert(item);
			enqueuedVertices.put(c.getCenterVertex(), item);
		}

		// remember which vertices were visited
		TIntHashSet visited = new TIntHashSet();

		// dijkstra loop
		int count = 0;
		while (!queue.isEmpty()) {
			// dequeue vertex u
			HeapItem uItem = queue.extractMin();
			int u = uItem.vertexId;
			enqueuedVertices.remove(u);
			visited.add(u);

			// add u to the cluster his it parent belongs to
			if (uItem.parent != u) {
				clustering.getCluster(uItem.parent).addVertex(u, uItem.distance);
			}
			// relax adjacent edges
			Edge[] adjEdges = graph.getVertex(u).getOutboundEdges();
			for (int i = 0; i < adjEdges.length; i++) {
				int weight = adjEdges[i].getWeight();
				int v = adjEdges[i].getTarget().getId();

				// relax edge if v was not already visited
				if (!visited.contains(v)) {
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
			if ((++count % MSG_INT_BUILD_CLUSTERS == 0)) {
				System.out.println("[build clusters] vertices : "
						+ (count - MSG_INT_BUILD_CLUSTERS) + " - " + count);
			}
		}
		System.out.println("[build clusters] vertices : "
				+ ((count / MSG_INT_BUILD_CLUSTERS) * MSG_INT_BUILD_CLUSTERS) + " - " + count);
	}

	private static void sampleDown(Graph graph, KCenterClustering clustering, int k, int k_,
			int heuristik) {
		int count = 0;
		while (k_ - count > k) {
			KCenterCluster cluster = chooseClusterForRemoval(graph, clustering, heuristik);
			removeClusterAndRearrange(graph, clustering, cluster);
			count++;
			if (count % MSG_INT_SAMPLE_DOWN == 0) {
				System.out.println("[sample down] clusters : " + (count - MSG_INT_SAMPLE_DOWN)
						+ " - " + count);
			}
		}
		System.out.println("[sample down] clusters : "
				+ ((count / MSG_INT_SAMPLE_DOWN) * MSG_INT_SAMPLE_DOWN) + " - " + count);

	}

	private static void removeClusterAndRearrange(Graph graph, KCenterClustering clustering,
			KCenterCluster cluster) {
		// remove the cluster
		KCenterCluster[] adjClusters = getAdjacentClusters(graph, clustering, cluster);
		int clusterSize = cluster.size();
		clustering.removeCluster(cluster);

		// disseminate vertices to neighbor clusters using dijkstra:

		// map vertex id to heap item of enqueued vertex
		HashMap<Integer, HeapItem> enqueuedVertices = new HashMap<Integer, HeapItem>();

		// initialize heap and enqueue centers
		BinaryMinHeap<HeapItem, Integer> queue = new BinaryMinHeap<HeapItem, Integer>(10000);
		for (KCenterCluster c : adjClusters) {
			HeapItem item = new HeapItem(c.getCenterVertex(), 0, c.getCenterVertex());
			queue.insert(item);
			enqueuedVertices.put(c.getCenterVertex(), item);
		}

		// remember which vertices were visited
		TIntHashSet visited = new TIntHashSet();

		int disseminatedVertices = 0;
		// dijkstra loop (skip if all vertices of cluster are disseminated)
		while (!queue.isEmpty() && disseminatedVertices < clusterSize) {
			// dequeue vertex u
			HeapItem uItem = queue.extractMin();
			int u = uItem.vertexId;
			enqueuedVertices.remove(u);
			visited.add(u);

			// add u to the cluster his it parent belongs to, if u does not belong to any
			// cluster
			if (uItem.parent != u && clustering.getCluster(u) == null) {
				clustering.getCluster(uItem.parent).addVertex(u, uItem.distance);
				disseminatedVertices++;
			}
			// relax adjacent edges
			Edge[] adjEdges = graph.getVertex(u).getOutboundEdges();
			for (int i = 0; i < adjEdges.length; i++) {
				int weight = adjEdges[i].getWeight();
				int v = adjEdges[i].getTarget().getId();

				// relax edge if v was not already visited
				if (!visited.contains(v)) {
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

	private static KCenterCluster[] getAdjacentClusters(Graph graph,
			KCenterClustering clustering, KCenterCluster cluster) {
		THashSet<KCenterCluster> set = new THashSet<KCenterCluster>();
		for (int v : cluster.getVertices()) {
			for (Edge e : graph.getVertex(v).getOutboundEdges()) {
				KCenterCluster c = clustering.getCluster(e.getTarget().getId());
				if (c != null && !c.equals(cluster)) {
					set.add(c);
				}
			}
		}
		KCenterCluster[] adjClusters = new KCenterCluster[set.size()];
		set.toArray(adjClusters);
		return adjClusters;
	}

	private static KCenterCluster chooseClusterForRemoval(Graph graph,
			KCenterClustering clustering, int heuristik) {
		switch (heuristik) {
			case HEURISTIC_MIN_RADIUS:
				return getMinCluster(clustering, new Comparator<KCenterCluster>() {

					@Override
					public int compare(KCenterCluster c1, KCenterCluster c2) {
						return c1.getRadius() - c2.getRadius();
					}
				});
			case HEURISTIC_MIN_SIZE:
				return getMinCluster(clustering, new Comparator<KCenterCluster>() {

					@Override
					public int compare(KCenterCluster c1, KCenterCluster c2) {
						return c1.size() - c2.size();
					}
				});
			default:
				return chooseClusterForRemoval(graph, clustering, HEURISTIC_DEFAULT);
		}
	}

	private static KCenterCluster getMinCluster(KCenterClustering clustering,
			Comparator<KCenterCluster> comp) {
		KCenterCluster min = clustering.getClusters().iterator().next();
		for (KCenterCluster c : clustering.getClusters()) {
			if (comp.compare(c, min) < 0) {
				min = c;
			}
		}
		return min;
	}

	private static class HeapItem implements IBinaryHeapItem<Integer> {

		final int vertexId;
		private int heapIndex;
		int distance;
		int parent;

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

	public static void main(String[] args) throws SQLException, IOException {
		Connection conn = DBConnection.getJdbcConnectionPg("localhost", 5432, "germany",
				"postgres", "admin");
		LevelGraph levelGraph = new LevelGraph(conn);
		int avgVerticesPerCluster = 1000;
		KCenterClustering[] clustering = computeClustering(levelGraph.getLevels(),
				avgVerticesPerCluster, HEURISTIC_MIN_SIZE);
		Serializer.serialize(new File("clustering_ger"), clustering);
		Serializer.serialize(new File("graph_ger"), levelGraph);
	}
}
