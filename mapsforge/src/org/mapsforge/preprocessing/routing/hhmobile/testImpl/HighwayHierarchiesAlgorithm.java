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

import org.mapsforge.preprocessing.routing.hhmobile.testImpl.routingGraph.Edge;
import org.mapsforge.preprocessing.routing.hhmobile.testImpl.routingGraph.LRUCache;
import org.mapsforge.preprocessing.routing.hhmobile.testImpl.routingGraph.RoutingGraph;
import org.mapsforge.preprocessing.routing.hhmobile.testImpl.routingGraph.Vertex;
import org.mapsforge.preprocessing.routing.hhmobile.util.Utils;
import org.mapsforge.preprocessing.routing.highwayHierarchies.util.prioQueue.BinaryMinHeap;
import org.mapsforge.preprocessing.routing.highwayHierarchies.util.prioQueue.IBinaryHeapItem;
import org.mapsforge.preprocessing.routing.highwayHierarchies.util.renderer.RendererV2;
import org.mapsforge.preprocessing.util.GeoCoordinate;
import org.mapsforge.server.routing.RouterFactory;

//check for duplicate ids
public class HighwayHierarchiesAlgorithm {

	private static final int INITIAL_HH_QUEUE_SIZE = 300;
	private static final int INITIAL_HH_MAP_SIZE = 2000;
	private static final int INITIAL_DIJKSTRA_QUEUE_SIZE = 50;
	private static final int INITIAL_DIJKSTRA_MAP_SIZE = 100;
	private static final int FWD = 0;
	private static final int BWD = 1;
	private static final int HEAP_IDX_SETTLED = -123456789;

	private final RoutingGraph graph;
	private final HHQueue[] queue;
	private final HHMap[] discovered;
	private final BinaryMinHeap<DijkstraHeapItem, DijkstraHeapItem> queueDijkstra;
	private final TIntObjectHashMap<DijkstraHeapItem> discoveredDijkstra;
	private int[][] numSettled;

	public HighwayHierarchiesAlgorithm(RoutingGraph graph) {
		this.graph = graph;
		this.queue = new HHQueue[] { new HHQueue(INITIAL_HH_QUEUE_SIZE),
				new HHQueue(INITIAL_HH_QUEUE_SIZE) };
		this.discovered = new HHMap[] { new HHMap(INITIAL_HH_MAP_SIZE),
				new HHMap(INITIAL_HH_MAP_SIZE) };
		this.queueDijkstra = new BinaryMinHeap<DijkstraHeapItem, DijkstraHeapItem>(
				INITIAL_DIJKSTRA_QUEUE_SIZE);
		this.discoveredDijkstra = new TIntObjectHashMap<DijkstraHeapItem>(
				INITIAL_DIJKSTRA_MAP_SIZE);
	}

	public int getShortestPath(int sourceId, int targetId, LinkedList<Vertex> shortestPathBuff)
			throws IOException {
		queue[FWD].clear();
		queue[BWD].clear();
		discovered[FWD].clear();
		discovered[BWD].clear();
		numSettled = new int[][] { new int[graph.numLevels()], new int[graph.numLevels()] };

		Vertex s = graph.getVertex(sourceId);
		HHHeapItem _s = new HHHeapItem(0, 0, s.getNeighborhood(), sourceId, sourceId, -1, -1,
				-1);
		queue[FWD].insert(_s);
		discovered[FWD].put(s.getIdLvlZero(), _s);

		Vertex t = graph.getVertex(targetId);
		HHHeapItem _t = new HHHeapItem(0, 0, t.getNeighborhood(), targetId, targetId, -1, -1,
				-1);
		queue[BWD].insert(_t);
		discovered[BWD].put(t.getIdLvlZero(), _t);

		int direction = FWD;
		int distance = Integer.MAX_VALUE;
		int searchScopeHitId = -1;

		while (!queue[FWD].isEmpty() || !queue[BWD].isEmpty()) {
			if (queue[direction].isEmpty()) {
				direction = (direction + 1) % 2;
			}
			HHHeapItem uItem = queue[direction].extractMin();
			uItem.heapIdx = HEAP_IDX_SETTLED;
			numSettled[direction][uItem.level]++;

			if (uItem.distance > distance) {
				queue[direction].clear();
				continue;
			}

			HHHeapItem uItem_ = discovered[(direction + 1) % 2].get(uItem.idLvlZero);
			if (uItem_ != null && uItem_.heapIdx == HEAP_IDX_SETTLED) {
				if (distance > uItem.distance + uItem_.distance) {
					distance = uItem.distance + uItem_.distance;
					searchScopeHitId = uItem.idLvlZero;
				}
			}

			Vertex u = graph.getVertex(uItem.id);
			if (uItem.gap == Integer.MAX_VALUE) {
				uItem.gap = u.getNeighborhood();
			}
			int lvl = uItem.level;
			int gap = uItem.gap;
			while (!relaxAdjacentEdges(uItem, u, direction, lvl, gap) && u.getIdOverly() != -1) {
				// switch to next level
				lvl++;
				u = graph.getVertex(u.getIdOverly());
				uItem.id = u.getId();
				gap = u.getNeighborhood();
			}
			direction = (direction + 1) % 2;
		}
		if (searchScopeHitId != -1) {
			expandEdges(discovered[FWD].get(searchScopeHitId), discovered[BWD]
					.get(searchScopeHitId), shortestPathBuff);

		}

		System.out.println("settled : " + Utils.arrToString(numSettled[0]) + " | "
				+ Utils.arrToString(numSettled[1]));
		return distance;
	}

	private boolean relaxAdjacentEdges(HHHeapItem uItem, Vertex u, int direction, int lvl,
			int gap) {
		boolean result = true;
		boolean forward = (direction == FWD);

		Edge[] adjEdges = u.getOutboundEdges();
		for (int i = 0; i < adjEdges.length; i++) {
			Edge e = adjEdges[i];
			if (forward && !e.isForward()) {
				continue;
			}
			if (!forward && !e.isBackward()) {
				continue;
			}

			int gap_ = gap;
			if (gap != Integer.MAX_VALUE) {
				gap_ = gap - e.getWeight();
				if (!e.isCore()) {
					// don't leave the core
					continue;
				}
				if (gap_ < 0) {
					// edge crosses neighborhood of entry point, don't relax it
					result = false;
					continue;
				}
			}

			HHHeapItem vItem = discovered[direction].get(e.getTargetIdLvlZero());
			if (vItem == null) {
				vItem = new HHHeapItem(uItem.distance + e.getWeight(), lvl, gap_, e
						.getTargetId(), e.getTargetIdLvlZero(), u.getIdLvlZero(), u.getId(), e
						.getTargetId());
				discovered[direction].put(e.getTargetIdLvlZero(), vItem);
				queue[direction].insert(vItem);
			} else if (vItem.compareTo(uItem.distance + e.getWeight(), lvl, gap_) > 0) {
				vItem.distance = uItem.distance + e.getWeight();
				vItem.level = lvl;
				vItem.id = e.getTargetId();
				vItem.gap = gap_;
				vItem.parentIdLvlZero = u.getIdLvlZero();
				vItem.eSrcId = u.getId();
				vItem.eTgtId = e.getTargetId();
				queue[direction].decreaseKey(vItem, vItem);
			}
		}

		return result;
	}

	private void expandEdges(HHHeapItem fwd, HHHeapItem bwd, LinkedList<Vertex> buff)
			throws IOException {
		while (fwd.eSrcId != -1) {
			expandEdgeRec(fwd.eSrcId, fwd.eTgtId, buff, true);
			fwd = discovered[FWD].get(fwd.parentIdLvlZero);
		}
		while (bwd.eSrcId != -1) {
			expandEdgeRec(bwd.eSrcId, bwd.eTgtId, buff, false);
			bwd = discovered[BWD].get(bwd.parentIdLvlZero);
		}
	}

	private void expandEdgeRec(int src, int tgt, LinkedList<Vertex> buff, boolean fwd)
			throws IOException {
		Vertex s = graph.getVertex(src);
		Vertex t = graph.getVertex(tgt);
		// System.out.println("expand edge " + s.getId() + " " + t.getId() + " " + s.getLvl()
		// + " " + t.getLvl());
		Edge e = extractEdge(s, t);
		if (s.getIdSubj() == -1) {
			// edge level == 0
			if (fwd) {
				buff.addFirst(t);
				buff.addFirst(s);
			} else {
				buff.addLast(t);
				buff.addLast(s);
			}
		} else if (!e.isShortcut()) {
			// jump directly to level 0
			expandEdgeRec(s.getIdLvlZero(), t.getIdLvlZero(), buff, fwd);
		} else {
			// use dijkstra within the core of subjacent level
			discoveredDijkstra.clear();
			queueDijkstra.clear();
			DijkstraHeapItem sItem = new DijkstraHeapItem(0, s.getIdSubj(), null);
			discoveredDijkstra.put(s.getIdSubj(), sItem);
			queueDijkstra.insert(sItem);

			while (!queueDijkstra.isEmpty()) {
				DijkstraHeapItem uItem = queueDijkstra.extractMin();
				if (uItem.id == t.getIdSubj()) {
					// found target
					break;
				}
				Vertex u = graph.getVertex(uItem.id);

				// relax edges
				Edge[] adjEdges = u.getOutboundEdges();
				for (int i = 0; i < adjEdges.length; i++) {
					if (!adjEdges[i].isCore() || (fwd && !adjEdges[i].isForward())
							|| (!fwd && !adjEdges[i].isBackward())) {
						// -skip edge if it is not applicable for current search direction
						// -skip non core edges
						continue;
					}
					DijkstraHeapItem vItem = discoveredDijkstra.get(adjEdges[i].getTargetId());
					if (vItem == null) {
						vItem = new DijkstraHeapItem(uItem.distance + adjEdges[i].getWeight(),
								adjEdges[i].getTargetId(), uItem);
						discoveredDijkstra.put(adjEdges[i].getTargetId(), vItem);
						queueDijkstra.insert(vItem);
					} else if (vItem.distance > uItem.distance + adjEdges[i].getWeight()) {
						vItem.distance = uItem.distance + adjEdges[i].getWeight();
						vItem.parent = uItem;
					}
				}
			}
			DijkstraHeapItem i = discoveredDijkstra.get(t.getIdSubj());
			while (i.parent != null) {
				int s_ = i.parent.id;
				int t_ = i.id;
				expandEdgeRec(s_, t_, buff, fwd);
				i = i.parent;
			}
		}
	}

	private Edge extractEdge(Vertex s, Vertex t) {
		Edge[] adjEdges = s.getOutboundEdges();
		int minWeight = Integer.MAX_VALUE;
		int idx = -1;
		for (int i = 0; i < adjEdges.length; i++) {
			if (adjEdges[i].getTargetId() == t.getId() && adjEdges[i].getWeight() < minWeight) {
				minWeight = adjEdges[i].getWeight();
				idx = i;
			}
		}
		return adjEdges[idx];
	}

	private static class HHHeapItem implements IBinaryHeapItem<HHHeapItem>,
			Comparable<HHHeapItem> {

		private int heapIdx;
		// the key
		public int distance;
		public int level;
		public int gap;
		// 
		public int id;
		public int idLvlZero;

		public int parentIdLvlZero;
		public int eSrcId;
		public int eTgtId;

		public HHHeapItem(int distance, int level, int gap, int id, int idLvlZero,
				int parentIdLvlZero, int eSrcId, int eTgtId) {
			this.heapIdx = -1;
			this.distance = distance;
			this.level = level;
			this.gap = gap;

			this.id = id;
			this.idLvlZero = idLvlZero;

			this.parentIdLvlZero = parentIdLvlZero;
			this.eSrcId = eSrcId;
			this.eTgtId = eTgtId;
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
		public void setHeapKey(HHHeapItem key) {
			this.distance = key.distance;
			this.level = key.level;
			this.gap = key.gap;
		}

		@Override
		public int compareTo(HHHeapItem other) {
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
		public HHHeapItem getHeapKey() {
			return this;
		}
	}

	private static class DijkstraHeapItem implements IBinaryHeapItem<DijkstraHeapItem>,
			Comparable<DijkstraHeapItem> {

		public int heapIdx;
		public int distance;
		public int id;
		public DijkstraHeapItem parent;

		public DijkstraHeapItem(int distance, int id, DijkstraHeapItem parent) {
			this.distance = distance;
			this.id = id;
			this.parent = parent;
		}

		@Override
		public int getHeapIndex() {
			return heapIdx;
		}

		@Override
		public DijkstraHeapItem getHeapKey() {
			return this;
		}

		@Override
		public void setHeapIndex(int idx) {
			this.heapIdx = idx;

		}

		@Override
		public void setHeapKey(DijkstraHeapItem key) {
			this.distance = key.distance;

		}

		@Override
		public int compareTo(DijkstraHeapItem other) {
			return distance - other.distance;
		}

	}

	private static class HHMap extends TIntObjectHashMap<HHHeapItem> {
		// need class without parameter to allow array creation without warning
		public HHMap(int initialCapacity) {
			super(initialCapacity);
		}

	}

	private static class HHQueue extends BinaryMinHeap<HHHeapItem, HHHeapItem> {
		// need class without parameter to allow array creation without warning
		public HHQueue(int initialSize) {
			super(initialSize);
		}
	}

	public static void main(String[] args) throws IOException, ClassNotFoundException {
		String map = "germany";
		int n = 1;

		RoutingGraph graph = new RoutingGraph(new File(map + ".mobile_hh"), new LRUCache(
				1024 * 1000));

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
			int d1 = hh.getShortestPath(s.getId(), t.getId(), sp1);
			// graph.clearCache();
			// int d2 = dijkstra.getShortestPath(s.getId(), t.getId(), sp2);
			// if (d1 != d2) {
			// System.out.println(d1 + " != " + d2);
			// }

			for (Vertex v : sp1) {
				renderer.addCircle(new GeoCoordinate(v.getLat(), v.getLon()), Color.RED);
			}

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
