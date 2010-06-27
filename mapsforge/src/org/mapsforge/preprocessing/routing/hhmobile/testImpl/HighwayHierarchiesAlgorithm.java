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
package org.mapsforge.preprocessing.routing.hhmobile.testImpl;

import gnu.trove.map.hash.TIntObjectHashMap;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;

import org.mapsforge.preprocessing.routing.hhmobile.testImpl.routingGraph.DummyCache;
import org.mapsforge.preprocessing.routing.hhmobile.testImpl.routingGraph.Edge;
import org.mapsforge.preprocessing.routing.hhmobile.testImpl.routingGraph.RoutingGraph;
import org.mapsforge.preprocessing.routing.hhmobile.testImpl.routingGraph.Vertex;
import org.mapsforge.preprocessing.routing.highwayHierarchies.util.prioQueue.BinaryMinHeap;
import org.mapsforge.preprocessing.routing.highwayHierarchies.util.prioQueue.IBinaryHeapItem;
import org.mapsforge.preprocessing.routing.highwayHierarchies.util.renderer.RendererV2;
import org.mapsforge.server.routing.RouterFactory;

public class HighwayHierarchiesAlgorithm {

	private static final int INITIAL_QUEUE_SIZE = 300;
	private static final int INITIAL_MAP_SIZE = 2000;
	private static final int FWD = 0;
	private static final int BWD = 1;
	private static final int HEAP_IDX_SETTLED = -123456789;

	private final RoutingGraph graph;
	private final Queue[] queue;
	private final DiscoveredMap[] discovered;

	public HighwayHierarchiesAlgorithm(RoutingGraph graph) {
		this.graph = graph;
		this.queue = new Queue[] { new Queue(INITIAL_QUEUE_SIZE), new Queue(INITIAL_QUEUE_SIZE) };
		this.discovered = new DiscoveredMap[] { new DiscoveredMap(INITIAL_MAP_SIZE),
				new DiscoveredMap(INITIAL_MAP_SIZE) };
	}

	public int getShortestPath(int sourceId, int targetId, LinkedList<Vertex> shortestPathBuff)
			throws IOException {
		queue[FWD].clear();
		queue[BWD].clear();
		discovered[FWD].clear();
		discovered[BWD].clear();

		Vertex s = graph.getVertex(sourceId);
		HeapItem _s = new HeapItem(0, 0, 0, s, -1);
		queue[FWD].insert(_s);
		discovered[FWD].put(s.getIdLvlZero(), _s);

		Vertex t = graph.getVertex(targetId);
		HeapItem _t = new HeapItem(0, 0, 0, t, -1);
		queue[BWD].insert(_t);
		discovered[BWD].put(t.getIdLvlZero(), _t);

		int direction = FWD;
		int distance = Integer.MAX_VALUE;

		while (!queue[FWD].isEmpty() || !queue[BWD].isEmpty()) {
			if (queue[direction].isEmpty()) {
				direction = (direction + 1) % 2;
			}
			HeapItem u = queue[direction].extractMin();
			u.heapIdx = HEAP_IDX_SETTLED;
			shortestPathBuff.add(u.vertex);

			if (u.distance > distance) {
				queue[direction].clear();
				continue;
			}

			HeapItem u_ = discovered[(direction + 1) % 2].get(u.vertex.getIdLvlZero());
			if (u_ != null && u_.heapIdx == HEAP_IDX_SETTLED) {
				distance = Math.min(distance, u.distance + u_.distance);
			}

			int lvl = u.level;
			int gap = u.gap;
			while (!relaxAdjacentEdges(u, direction, lvl, gap) && u.vertex.getIdOverly() != -1) {
				// switch to next level
				u.vertex = graph.getVertex(u.vertex.getIdOverly());
				lvl++;
				gap = u.vertex.getNeighborhood();
			}
			direction = (direction + 1) % 2;
		}

		return distance;
	}

	private boolean relaxAdjacentEdges(HeapItem u, int direction, int lvl, int gap)
			throws IOException {
		boolean result = true;
		boolean forward = (direction == FWD);

		Edge[] adjEdges = u.vertex.getOutboundEdges();
		for (int i = 0; i < adjEdges.length; i++) {
			Edge e = adjEdges[i];
			if (forward && !e.isForward()) {
				continue;
			}
			if (!forward && !e.isBackward()) {
				continue;
			}

			Vertex _v = graph.getVertex(e.getTargetId());

			int gap_;
			if (gap == Integer.MAX_VALUE) {
				gap_ = _v.getNeighborhood();
			} else {
				if (u.vertex.getNeighborhood() != Integer.MAX_VALUE
						&& _v.getNeighborhood() == Integer.MAX_VALUE) {
					// don't leave the core
					continue;
				}
				gap_ = gap - e.getWeight();
			}

			if (gap_ < 0) {
				// edge crosses neighborhood of entry point, don't relax it
				result = false;
				continue;
			}

			HeapItem v = discovered[direction].get(_v.getIdLvlZero());
			if (v == null) {
				v = new HeapItem(u.distance + e.getWeight(), lvl, gap_, _v, u.vertex.getId());
				discovered[direction].put(v.vertex.getIdLvlZero(), v);
				queue[direction].insert(v);
			} else if (v.compareTo(u.distance + e.getWeight(), lvl, gap_) > 0) {
				v.distance = u.distance + e.getWeight();
				v.level = lvl;
				v.gap = gap_;
				v.parentId = u.vertex.getId();
				queue[direction].decreaseKey(v, v);
			}
		}

		return result;
	}

	private static class DiscoveredMap extends TIntObjectHashMap<HeapItem> {
		// need class without parameter to allow array creation without warning
		public DiscoveredMap(int initialCapacity) {
			super(initialCapacity);
		}

	}

	private static class Queue extends BinaryMinHeap<HeapItem, HeapItem> {
		// need class without parameter to allow array creation without warning
		public Queue(int initialSize) {
			super(initialSize);
		}
	}

	public static class HeapItem implements IBinaryHeapItem<HeapItem>, Comparable<HeapItem> {

		private int heapIdx;
		// the key
		public int distance;
		public int level;
		public int gap;
		// 
		public Vertex vertex;
		public int parentId;

		public HeapItem(int distance, int level, int gap, Vertex v, int parentId) {
			this.heapIdx = -1;
			this.distance = distance;
			this.level = level;
			this.gap = gap;
			this.vertex = v;
			this.parentId = parentId;
		}

		@Override
		public int getHeapIndex() {
			return heapIdx;
		}

		@Override
		public void setHeapIndex(int idx) {
			this.heapIdx = idx;
		}

		@Override
		public void setHeapKey(HeapItem key) {
			this.distance = key.distance;
			this.level = key.level;
			this.gap = key.gap;
		}

		@Override
		public int compareTo(HeapItem other) {
			if (distance < other.distance) {
				return -3;
			} else if (distance > other.distance) {
				return 3;
			} else if (level < other.level) {
				return -2;
			} else if (level > other.level) {
				return 2;
			} else if (gap < other.gap) {
				return -1;
			} else if (gap > other.gap) {
				return 1;
			} else {
				return 0;
			}
		}

		public int compareTo(int _distance, int _level, int _gap) {
			if (distance < _distance) {
				return -3;
			} else if (distance > _distance) {
				return 3;
			} else if (level < _level) {
				return -2;
			} else if (level > _level) {
				return 2;
			} else if (gap < _gap) {
				return -1;
			} else if (gap > _gap) {
				return 1;
			} else {
				return 0;
			}
		}

		@Override
		public HeapItem getHeapKey() {
			return this;
		}
	}

	public static void main(String[] args) throws IOException {
		String map = "berlin";
		int n = 1;

		RoutingGraph graph = new RoutingGraph(new File(map + ".mobile_hh"), new DummyCache());

		HighwayHierarchiesAlgorithm hh = new HighwayHierarchiesAlgorithm(graph);
		DijkstraAlgorithm dijkstra = new DijkstraAlgorithm(graph);
		RendererV2 renderer = new RendererV2(1024, 768, RouterFactory.getRouter(), Color.BLACK,
				Color.WHITE);
		LinkedList<Vertex> sp1 = new LinkedList<Vertex>();
		LinkedList<Vertex> sp2 = new LinkedList<Vertex>();

		long time = System.currentTimeMillis();
		for (int i = 0; i < n; i++) {
			Vertex s = graph.getRandomVertex(0);
			Vertex t = graph.getRandomVertex(0);
			graph.clearCache();
			int d1 = hh.getShortestPath(s.getId(), t.getId(), sp1);
			// int d2 = dijkstra.getShortestPath(s.getId(), t.getId(), sp2);
			// System.out.println(d1 + " ?= " + d2);
			// for (Vertex v : sp1) {
			// Vertex vLz = graph.getVertex(v.getIdLvlZero());
			// renderer.addCircle(new GeoCoordinate(vLz.getLat(), vLz.getLon()), Color.RED);
			// }
			// for (Vertex v : sp2) {
			// Vertex vLz = graph.getVertex(v.getIdLvlZero());
			// renderer.addCircle(new GeoCoordinate(vLz.getLat(), vLz.getLon()), Color.BLUE);
			// }
			//
			// renderer.addCircle(new GeoCoordinate(s.getLat(), s.getLon()), Color.GREEN);
			// renderer.addCircle(new GeoCoordinate(t.getLat(), t.getLon()), Color.GREEN);
			// sp1.clear();
		}
		System.out.println("cache misses : " + graph.numCacheMisses);
		System.out.println("num routes : " + n);
		System.out.println("exec time : " + (System.currentTimeMillis() - time) + "ms.");
	}
}
