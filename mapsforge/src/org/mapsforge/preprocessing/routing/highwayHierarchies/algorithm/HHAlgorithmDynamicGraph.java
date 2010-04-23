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
package org.mapsforge.preprocessing.routing.highwayHierarchies.algorithm;

import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.LinkedList;

import org.mapsforge.preprocessing.routing.highwayHierarchies.datastructures.HHDynamicGraph.HHDynamicEdge;
import org.mapsforge.preprocessing.routing.highwayHierarchies.datastructures.HHDynamicGraph.HHDynamicVertex;
import org.mapsforge.preprocessing.routing.highwayHierarchies.util.prioQueue.BinaryMinHeap;
import org.mapsforge.preprocessing.routing.highwayHierarchies.util.prioQueue.IBinaryHeapItem;

/**
 * @author Frank Viernau viernau[at]mi.fu-berlin.de
 * 
 */
public class HHAlgorithmDynamicGraph {

	private static final int INITIAL_QUEUE_SIZE = 300;
	private static final int INITIAL_MAP_SIZE = 5000;
	private static final int HEAP_IDX_SETTLED = -1234567;

	private static final int FWD = 0;
	private static final int BWD = 1;

	private BinaryMinHeap<DiscoveredVertex, HeapKey>[] queue;
	private TIntObjectHashMap<DiscoveredVertex>[] discoveredVertices;

	public HHAlgorithmDynamicGraph() {
		queue = new BinaryMinHeap[] {
				new BinaryMinHeap<DiscoveredVertex, HeapKey>(INITIAL_QUEUE_SIZE),
				new BinaryMinHeap<DiscoveredVertex, HeapKey>(INITIAL_QUEUE_SIZE) };
		discoveredVertices = new TIntObjectHashMap[] {
				new TIntObjectHashMap<DiscoveredVertex>(INITIAL_MAP_SIZE),
				new TIntObjectHashMap<DiscoveredVertex>(INITIAL_MAP_SIZE) };
	}

	public int shortestDistance(HHDynamicVertex source, HHDynamicVertex target) {
		int numSettled = 0;
		LinkedList<HHDynamicEdge> list = new LinkedList<HHDynamicEdge>();

		// clear queue
		queue[FWD].clear();
		queue[BWD].clear();
		discoveredVertices[FWD].clear();
		discoveredVertices[BWD].clear();

		// enqueue source and target
		DiscoveredVertex s = new DiscoveredVertex(source, null, null, new HeapKey(0, 0, source
				.getNeighborhood(0)));
		DiscoveredVertex t = new DiscoveredVertex(target, null, null, new HeapKey(0, 0, target
				.getNeighborhood(0)));
		queue[FWD].insert(s);
		queue[BWD].insert(t);
		discoveredVertices[FWD].put(source.getId(), s);
		discoveredVertices[BWD].put(target.getId(), t);

		int direction = FWD;
		int d = Integer.MAX_VALUE;

		while (!queue[FWD].isEmpty() || !queue[BWD].isEmpty()) {
			if (queue[direction].isEmpty()) {
				direction = (direction + 1) % 2;
			}
			DiscoveredVertex u = queue[direction].extractMin();
			u.heapIdx = HEAP_IDX_SETTLED;
			if (u.edgeToParent != null) {
				list.add(u.edgeToParent);
			}
			numSettled++;
			// System.out.println("[" + numSettled + "] " + u.key + " " + direction);
			if (u.key.distance > d) {
				queue[direction].clear();
				continue;
			}

			DiscoveredVertex u_ = discoveredVertices[(direction + 1) % 2].get(u.vertex.getId());
			if (u_ != null && u_.heapIdx == HEAP_IDX_SETTLED) {
				d = Math.min(d, u.key.distance + u_.key.distance);
				// System.out.println(u.key + " " + u_.key);
			}

			if (!relaxAdjacentEdges(u, direction, u.key.level, u.key.gap)) {
				int maxLvl = u.vertex.getMaxLevel();
				for (int lvl = u.key.level + 1; lvl <= maxLvl; lvl++) {
					if (relaxAdjacentEdges(u, direction, lvl, u.vertex.getNeighborhood(lvl))) {
						break;
					}
				}
			}
			direction = (direction + 1) % 2;
		}
		return d;
	}

	public boolean relaxAdjacentEdges(DiscoveredVertex u, int direction, int lvl, int gap) {
		// System.out.println("relasx " + lvl + " " + direction);
		boolean result = true;

		boolean forward = (direction == FWD);
		for (HHDynamicEdge e : u.vertex.getOutboundEdges(lvl)) {
			if (forward && !e.isForward()) {
				continue;
			}
			if (!forward && !e.isBackward()) {
				continue;
			}
			HHDynamicVertex _v = e.getTarget();

			int gap_;
			if (gap == Integer.MAX_VALUE) {
				gap_ = _v.getNeighborhood(lvl);
			} else {
				if (u.vertex.getNeighborhood(lvl) != Integer.MAX_VALUE
						&& _v.getNeighborhood(lvl) == Integer.MAX_VALUE) {
					result = false;
					continue;
				}
				gap_ = gap - e.getWeight();
			}

			if (gap_ < 0) {
				result = false;
				continue;
			}

			HeapKey key = new HeapKey(u.key.distance + e.getWeight(), lvl, gap_);
			DiscoveredVertex v = discoveredVertices[direction].get(_v.getId());
			if (v == null) {
				v = new DiscoveredVertex(_v, e, u, key);
				discoveredVertices[direction].put(v.vertex.getId(), v);
				queue[direction].insert(v);
			} else if (key.compareTo(v.key) < 0) {
				queue[direction].decreaseKey(v, key);
				v.parent = u;
				v.edgeToParent = e;
			}
		}
		return result;
	}

	private class DiscoveredVertex implements IBinaryHeapItem<HeapKey> {
		private HHDynamicVertex vertex;
		private HHDynamicEdge edgeToParent;
		// could be used later on if finding ways instead of sp distances is desired
		public DiscoveredVertex parent;
		private HeapKey key;
		private int heapIdx;

		public DiscoveredVertex(HHDynamicVertex vertex, HHDynamicEdge edgeToParent,
				DiscoveredVertex parent, HeapKey key) {
			this.vertex = vertex;
			this.edgeToParent = edgeToParent;
			this.parent = parent;
			this.key = key;
		}

		@Override
		public int getHeapIndex() {
			return heapIdx;
		}

		@Override
		public HeapKey getHeapKey() {
			return key;
		}

		@Override
		public void setHeapIndex(int idx) {
			heapIdx = idx;

		}

		@Override
		public void setHeapKey(HeapKey key) {
			this.key = key;
		}
	}

	private class HeapKey implements Comparable<HeapKey> {
		private int distance, level, gap;

		public HeapKey(int distance, int level, int gap) {
			this.distance = distance;
			this.level = level;
			this.gap = gap;
		}

		@Override
		public String toString() {
			return "key : distance=" + distance + " lvl=" + level + " gap=" + gap;
		}

		@Override
		public int compareTo(HeapKey other) {
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
	}

	public int shortestDistanceDijkstra(HHDynamicVertex source, HHDynamicVertex target) {
		int numSettled = 0;

		// clear queue
		queue[FWD].clear();
		queue[BWD].clear();
		discoveredVertices[FWD].clear();
		discoveredVertices[BWD].clear();

		// enqueue source and target
		DiscoveredVertex s = new DiscoveredVertex(source, null, null, new HeapKey(0, 0, source
				.getNeighborhood(0)));
		DiscoveredVertex t = new DiscoveredVertex(target, null, null, new HeapKey(0, 0, target
				.getNeighborhood(0)));
		queue[FWD].insert(s);
		queue[BWD].insert(t);
		discoveredVertices[FWD].put(source.getId(), s);
		discoveredVertices[BWD].put(target.getId(), t);

		int direction = FWD;
		int d = Integer.MAX_VALUE;

		while (!queue[FWD].isEmpty() || !queue[BWD].isEmpty()) {
			if (queue[direction].isEmpty()) {
				direction = (direction + 1) % 2;
			}
			DiscoveredVertex u = queue[direction].extractMin();
			u.heapIdx = -456;
			numSettled++;

			DiscoveredVertex u_ = discoveredVertices[(direction + 1) % 2].get(u.vertex.getId());
			if (u_ != null && u_.heapIdx == -456) {
				return u.key.distance + u_.key.distance;
			}
			if (direction == FWD) {
				for (HHDynamicEdge e : u.vertex.getOutboundEdges(0)) {
					DiscoveredVertex v = discoveredVertices[direction].get(e.getTarget()
							.getId());
					HeapKey key = new HeapKey(u.key.distance + e.getWeight(), 0, 0);
					if (v == null) {
						v = new DiscoveredVertex(e.getTarget(), e, u, key);
						queue[direction].insert(v);
						discoveredVertices[direction].put(v.vertex.getId(), v);
					} else if (key.compareTo(v.key) < 0) {
						queue[direction].decreaseKey(v, key);
						v.parent = u;
						v.edgeToParent = e;
					}
				}
			} else {
				for (HHDynamicEdge e : u.vertex.getInboundEdges(0)) {
					DiscoveredVertex v = discoveredVertices[direction].get(e.getSource()
							.getId());
					HeapKey key = new HeapKey(u.key.distance + e.getWeight(), 0, 0);
					if (v == null) {
						v = new DiscoveredVertex(e.getSource(), e, u, key);
						queue[direction].insert(v);
						discoveredVertices[direction].put(v.vertex.getId(), v);
					} else if (key.compareTo(v.key) < 0) {
						queue[direction].decreaseKey(v, key);
						v.parent = u;
						v.edgeToParent = e;
					}
				}
			}
			direction = (direction + 1) % 2;
		}
		return d;
	}

	public int dijkstra(HHDynamicVertex source, HHDynamicVertex target, int lvl) {
		int numSettled = 0;

		// clear queue
		queue[FWD].clear();
		discoveredVertices[FWD].clear();

		// enqueue source and target
		DiscoveredVertex s = new DiscoveredVertex(source, null, null, new HeapKey(0, 0, source
				.getNeighborhood(0)));
		queue[FWD].insert(s);
		discoveredVertices[FWD].put(source.getId(), s);

		while (!queue[FWD].isEmpty()) {
			DiscoveredVertex u = queue[FWD].extractMin();
			numSettled++;

			if (u.vertex.getId() == target.getId()) {
				return u.key.distance;
			}

			for (HHDynamicEdge e : u.vertex.getOutboundEdges(lvl)) {
				if (!e.isForward())
					continue;
				DiscoveredVertex v = discoveredVertices[FWD].get(e.getTarget().getId());
				HeapKey key = new HeapKey(u.key.distance + e.getWeight(), 0, 0);
				if (v == null) {
					v = new DiscoveredVertex(e.getTarget(), e, u, key);
					queue[FWD].insert(v);
					discoveredVertices[FWD].put(v.vertex.getId(), v);
				} else if (key.compareTo(v.key) < 0) {
					queue[FWD].decreaseKey(v, key);
					v.parent = u;
					v.edgeToParent = e;
				}
			}
		}
		return Integer.MAX_VALUE;
	}

	public int dijkstrab(HHDynamicVertex source, HHDynamicVertex target) {
		int numSettled = 0;

		// clear queue
		queue[FWD].clear();
		discoveredVertices[FWD].clear();

		// enqueue source and target
		DiscoveredVertex t = new DiscoveredVertex(target, null, null, new HeapKey(0, 0, 0));
		queue[FWD].insert(t);
		discoveredVertices[FWD].put(target.getId(), t);

		while (!queue[FWD].isEmpty()) {
			DiscoveredVertex u = queue[FWD].extractMin();
			numSettled++;

			if (u.vertex.getId() == source.getId()) {
				return u.key.distance;
			}

			for (HHDynamicEdge e : u.vertex.getInboundEdges(0)) {
				DiscoveredVertex v = discoveredVertices[FWD].get(e.getSource().getId());
				HeapKey key = new HeapKey(u.key.distance + e.getWeight(), 0, 0);
				if (v == null) {
					v = new DiscoveredVertex(e.getSource(), e, u, key);
					queue[FWD].insert(v);
					discoveredVertices[FWD].put(v.vertex.getId(), v);
				} else if (key.compareTo(v.key) < 0) {
					queue[FWD].decreaseKey(v, key);
					v.parent = u;
					v.edgeToParent = e;
				}
			}
		}
		return Integer.MAX_VALUE;
	}
}
