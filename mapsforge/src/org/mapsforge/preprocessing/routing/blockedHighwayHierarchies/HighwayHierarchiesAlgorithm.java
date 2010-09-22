///*
// * Copyright 2010 mapsforge.org
// *
// * This program is free software: you can redistribute it and/or modify
// * it under the terms of the GNU General Public License as published by
// * the Free Software Foundation, either version 3 of the License, or
// * (at your option) any later version.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU General Public License for more details.
// *
// * You should have received a copy of the GNU General Public License
// * along with this program.  If not, see <http://www.gnu.org/licenses/>.
// */
//package org.mapsforge.preprocessing.routing.blockedHighwayHierarchies;
//
//import gnu.trove.map.hash.TIntObjectHashMap;
//
//import java.io.File;
//import java.io.IOException;
//import java.util.LinkedList;
//import java.util.Random;
//
//import org.mapsforge.preprocessing.routing.blockedHighwayHierarchies.LevelGraph.Level.LevelEdge;
//import org.mapsforge.preprocessing.routing.blockedHighwayHierarchies.LevelGraph.Level.LevelVertex;
//import org.mapsforge.preprocessing.routing.highwayHierarchies.HHAlgorithmDynamicGraph;
//import org.mapsforge.preprocessing.routing.highwayHierarchies.HHDynamicGraph;
//import org.mapsforge.preprocessing.routing.highwayHierarchies.util.Serializer;
//import org.mapsforge.preprocessing.routing.highwayHierarchies.util.prioQueue.BinaryMinHeap;
//import org.mapsforge.preprocessing.routing.highwayHierarchies.util.prioQueue.IBinaryHeapItem;
//
//class HighwayHierarchiesAlgorithm {
//
//	private static final int INITIAL_QUEUE_SIZE = 300;
//	private static final int INITIAL_MAP_SIZE = 2000;
//	private static final int FWD = 0;
//	private static final int BWD = 1;
//	private static final int HEAP_IDX_SETTLED = -123456789;
//
//	private final LevelGraph graph;
//	private final Queue[] queue;
//	private final DiscoveredMap[] discovered;
//	private int[][] numSettled;
//
//	public HighwayHierarchiesAlgorithm(LevelGraph graph) {
//		this.graph = graph;
//		this.queue = new Queue[] { new Queue(INITIAL_QUEUE_SIZE), new Queue(INITIAL_QUEUE_SIZE) };
//		this.discovered = new DiscoveredMap[] { new DiscoveredMap(INITIAL_MAP_SIZE),
//				new DiscoveredMap(INITIAL_MAP_SIZE) };
//	}
//
//	public int getShortestPath(int sourceId, int targetId,
//			LinkedList<LevelVertex> shortestPathBuff) {
//		queue[FWD].clear();
//		queue[BWD].clear();
//		discovered[FWD].clear();
//		discovered[BWD].clear();
//		numSettled = new int[][] { new int[graph.numLevels()], new int[graph.numLevels()] };
//
//		LevelVertex s = graph.getLevel(0).getVertex(sourceId);
//		HeapItem _s = new HeapItem(0, 0, s.getNeighborhood(), s, -1);
//		queue[FWD].insert(_s);
//		discovered[FWD].put(s.getId(), _s);
//
//		LevelVertex t = graph.getLevel(0).getVertex(targetId);
//		HeapItem _t = new HeapItem(0, 0, t.getNeighborhood(), t, -1);
//		queue[BWD].insert(_t);
//		discovered[BWD].put(t.getId(), _t);
//
//		int direction = FWD;
//		int distance = Integer.MAX_VALUE;
//
//		while (!queue[FWD].isEmpty() || !queue[BWD].isEmpty()) {
//			if (queue[direction].isEmpty()) {
//				direction = (direction + 1) % 2;
//			}
//			HeapItem u = queue[direction].extractMin();
//			u.heapIdx = HEAP_IDX_SETTLED;
//			shortestPathBuff.add(u.vertex);
//			numSettled[direction][u.level]++;
//			u.vertex = graph.getLevel(u.level).getVertex(u.vertex.getId());
//
//			if (u.distance > distance) {
//				queue[direction].clear();
//				continue;
//			}
//
//			HeapItem u_ = discovered[(direction + 1) % 2].get(u.vertex.getId());
//			if (u_ != null && u_.heapIdx == HEAP_IDX_SETTLED) {
//				distance = Math.min(distance, u.distance + u_.distance);
//			}
//
//			int lvl = u.level;
//			int gap = u.gap;
//			while (!relaxAdjacentEdges(u, direction, lvl, gap)) {
//				if (lvl < u.vertex.getMaxLevel()) {
//					lvl++;
//					u.vertex = graph.getLevel(lvl).getVertex(u.vertex.getId());
//					gap = u.vertex.getNeighborhood();
//				} else {
//					break;
//				}
//
//			}
//			direction = (direction + 1) % 2;
//		}
//		return distance;
//	}
//
//	private boolean relaxAdjacentEdges(HeapItem u, int direction, int lvl, int gap) {
//		boolean result = true;
//		boolean forward = (direction == FWD);
//
//		LevelEdge[] adjEdges = u.vertex.getOutboundEdges();
//		for (int i = 0; i < adjEdges.length; i++) {
//			LevelEdge e = adjEdges[i];
//			if (forward && !e.isForward()) {
//				continue;
//			}
//			if (!forward && !e.isBackward()) {
//				continue;
//			}
//
//			LevelVertex _v = e.getTarget();
//
//			int gap_;
//			if (gap == Integer.MAX_VALUE) {
//				gap_ = _v.getNeighborhood();
//			} else {
//				if (u.vertex.getNeighborhood() != Integer.MAX_VALUE
//						&& _v.getNeighborhood() == Integer.MAX_VALUE) {
//					// don't leave the core
//					result = false;
//					continue;
//				}
//				gap_ = gap - e.getWeight();
//			}
//
//			if (gap_ < 0) {
//				// edge crosses neighborhood of entry point, don't relax it
//				result = false;
//				continue;
//			}
//
//			HeapItem v = discovered[direction].get(_v.getId());
//			if (v == null) {
//				v = new HeapItem(u.distance + e.getWeight(), lvl, gap_, _v, u.vertex.getId());
//				discovered[direction].put(v.vertex.getId(), v);
//				queue[direction].insert(v);
//			} else if (v.compareTo(u.distance + e.getWeight(), lvl, gap_) > 0) {
//				v.distance = u.distance + e.getWeight();
//				v.level = lvl;
//				v.gap = gap_;
//				v.parentId = u.vertex.getId();
//				queue[direction].decreaseKey(v, v);
//			}
//		}
//
//		return result;
//	}
//
//	private static class DiscoveredMap extends TIntObjectHashMap<HeapItem> {
//		// need class without parameter to allow array creation without warning
//		public DiscoveredMap(int initialCapacity) {
//			super(initialCapacity);
//		}
//
//	}
//
//	private static class Queue extends BinaryMinHeap<HeapItem, HeapItem> {
//		// need class without parameter to allow array creation without warning
//		public Queue(int initialSize) {
//			super(initialSize);
//		}
//	}
//
//	public static class HeapItem implements IBinaryHeapItem<HeapItem>, Comparable<HeapItem> {
//
//		int heapIdx;
//		// the key
//		public int distance;
//		public int level;
//		public int gap;
//		// 
//		public LevelVertex vertex;
//		public int parentId;
//
//		public HeapItem(int distance, int level, int gap, LevelVertex v, int parentId) {
//			this.heapIdx = -1;
//			this.distance = distance;
//			this.level = level;
//			this.gap = gap;
//			this.vertex = v;
//			this.parentId = parentId;
//		}
//
//		@Override
//		public int getHeapIndex() {
//			return heapIdx;
//		}
//
//		@Override
//		public void setHeapIndex(int idx) {
//			this.heapIdx = idx;
//		}
//
//		@Override
//		public void setHeapKey(HeapItem key) {
//			this.distance = key.distance;
//			this.level = key.level;
//			this.gap = key.gap;
//		}
//
//		@Override
//		public int compareTo(HeapItem other) {
//			if (distance < other.distance) {
//				return -3;
//			} else if (distance > other.distance) {
//				return 3;
//			} else if (level < other.level) {
//				return -2;
//			} else if (level > other.level) {
//				return 2;
//			} else if (gap < other.gap) {
//				return -1;
//			} else if (gap > other.gap) {
//				return 1;
//			} else {
//				return 0;
//			}
//		}
//
//		public int compareTo(int _distance, int _level, int _gap) {
//			if (distance < _distance) {
//				return -3;
//			} else if (distance > _distance) {
//				return 3;
//			} else if (level < _level) {
//				return -2;
//			} else if (level > _level) {
//				return 2;
//			} else if (gap < _gap) {
//				return -1;
//			} else if (gap > _gap) {
//				return 1;
//			} else {
//				return 0;
//			}
//		}
//
//		@Override
//		public HeapItem getHeapKey() {
//			return this;
//		}
//	}
//
//	public static void main(String[] args) throws IOException, ClassNotFoundException {
//		String map = "berlin";
//		int n = 500;
//
//		LevelGraph graph = Serializer.deserialize(new File(map + ".levelGraph"));
//		HighwayHierarchiesAlgorithm hh = new HighwayHierarchiesAlgorithm(graph);
//		DijkstraAlgorithm dijkstra = new DijkstraAlgorithm(graph);
//		HHDynamicGraph graph2 = Serializer.deserialize(new File("berlin.hhDynamicGraph"));
//		HHAlgorithmDynamicGraph hh2 = new HHAlgorithmDynamicGraph();
//
//		// for (Iterator<LevelVertex> iter = graph.getLevel(0).getVertices(); iter.hasNext();) {
//		// LevelVertex v = iter.next();
//		// System.out.println("vertex[" + v.getId() + "]");
//		// for (int i = 0; i <= v.getMaxLevel(); i++) {
//		// LevelVertex v_ = graph.getLevel(i).getVertex(v.getId());
//		// for (LevelEdge e : v_.getOutboundEdges()) {
//		// System.out.println(v.getId() + " -> " + e.getTarget().getId());
//		// }
//		// }
//		//
//		// }
//		// if (true) {
//		// return;
//		// }
//
//		// RendererV2 renderer = new RendererV2(1024, 768, RouterFactory.getRouter(),
//		// Color.BLACK,
//		// Color.WHITE);
//		LinkedList<LevelVertex> sp1 = new LinkedList<LevelVertex>();
//		LinkedList<LevelVertex> sp2 = new LinkedList<LevelVertex>();
//		Random rnd = new Random();
//		long time = System.currentTimeMillis();
//		for (int i = 0; i < n; i++) {
//			int s = rnd.nextInt(graph.getLevel(0).numVertices());
//			int t = rnd.nextInt(graph.getLevel(0).numVertices());
//			int d1 = hh.getShortestPath(s, t, sp1);
//			int d2 = dijkstra.getShortestPath(s, t, 0, sp2, new LinkedList<Integer>(), true,
//					false);
//			int d3 = hh2.shortestDistance(graph2.getVertex(s), graph2.getVertex(t));
//			if (d1 != d2) {
//				System.out.println(d1 + " " + d2);
//			}
//			if (d1 != d3) {
//				System.out.println(d1 + " " + d3);
//			}
//			// for (Vertex v : sp1) {
//			// Vertex vLz = graph.getVertex(v.getIdLvlZero());
//			// // renderer.addCircle(new GeoCoordinate(vLz.getLat(), vLz.getLon()), Color.RED);
//			// }
//			// for (Vertex v : sp2) {
//			// Vertex vLz = graph.getVertex(v.getIdLvlZero());
//			// renderer.addCircle(new GeoCoordinate(vLz.getLat(), vLz.getLon()), Color.BLUE);
//			// }
//			//
//			// renderer.addCircle(new GeoCoordinate(s.getLat(), s.getLon()), Color.GREEN);
//			// renderer.addCircle(new GeoCoordinate(t.getLat(), t.getLon()), Color.GREEN);
//			// sp1.clear();
//		}
//		System.out.println("num routes : " + n);
//		System.out.println("exec time : " + (System.currentTimeMillis() - time) + "ms.");
//	}
//
//	public static int sumWeight(org.mapsforge.server.routing.IEdge[] e) {
//		int sum = 0;
//		for (org.mapsforge.server.routing.IEdge x : e) {
//			sum += x.getWeight();
//		}
//		return sum;
//	}
//
// }
