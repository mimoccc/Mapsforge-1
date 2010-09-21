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

import gnu.trove.map.hash.TIntObjectHashMap;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.Random;

import org.mapsforge.preprocessing.routing.hhmobile.LevelGraph.Level.LevelEdge;
import org.mapsforge.preprocessing.routing.hhmobile.LevelGraph.Level.LevelVertex;
import org.mapsforge.preprocessing.routing.highwayHierarchies.util.prioQueue.BinaryMinHeap;
import org.mapsforge.preprocessing.routing.highwayHierarchies.util.prioQueue.IBinaryHeapItem;

class DijkstraAlgorithm {

	private final BinaryMinHeap<HeapItem, Integer> queue;
	private final LevelGraph graph;
	private final TIntObjectHashMap<HeapItem> discovered;

	public DijkstraAlgorithm(LevelGraph graph) {
		this.queue = new BinaryMinHeap<HeapItem, Integer>(10000);
		this.graph = graph;
		this.discovered = new TIntObjectHashMap<HeapItem>();
	}

	public int getShortestPath(int sourceId, int targetId, int level,
			LinkedList<LevelVertex> shortestPathBuff, LinkedList<Integer> hopIndicesBuff,
			boolean forward, boolean backward) {
		this.queue.clear();
		this.discovered.clear();

		HeapItem s = new HeapItem(sourceId, level, null);
		queue.insert(s);
		discovered.put(s.vertexId, s);
		while (!queue.isEmpty()) {
			HeapItem _u = queue.extractMin();
			LevelVertex u = graph.getLevel(level).getVertex(_u.vertexId);
			if (u.getId() == targetId) {
				break;
			}
			LevelEdge[] adjEdges = u.getOutboundEdges();
			for (int i = 0; i < adjEdges.length; i++) {
				LevelEdge e = adjEdges[i];
				if ((forward && !e.isForward()) || (backward && !e.isBackward())) {
					continue;
				}
				HeapItem _v = discovered.get(e.getTarget().getId());
				if (_v == null) {
					_v = new HeapItem(e.getTarget().getId(), _u.distance + e.getWeight(), u);
					queue.insert(_v);
					discovered.put(_v.vertexId, _v);
					_v.hopIdx = i;
				} else if (_v.distance > _u.distance + e.getWeight()) {
					queue.decreaseKey(_v, _u.distance + e.getWeight());
					_v.hopIdx = i;
					_v.parent = u;
				}
			}
		}
		HeapItem _t = discovered.get(targetId);
		if (_t == null) {
			return Integer.MAX_VALUE;
		}
		int distance = _t.distance;
		shortestPathBuff.addFirst(graph.getLevel(level).getVertex(targetId));
		while (_t.parent != null) {
			shortestPathBuff.addFirst(_t.parent);
			hopIndicesBuff.addFirst(_t.hopIdx);
			_t = discovered.get(_t.parent.getId());
		}
		return distance;
	}

	private class HeapItem implements IBinaryHeapItem<Integer> {

		private int heapIdx;
		int distance;
		LevelVertex parent;
		int vertexId;
		int hopIdx; // the index of the outbound edge lying on shortest path tree

		public HeapItem(int vertexId, int distance, LevelVertex parent) {
			this.vertexId = vertexId;
			this.distance = distance;
			this.parent = parent;
			this.heapIdx = -1;
			this.hopIdx = -1;
		}

		@Override
		public int getHeapIndex() {
			return heapIdx;
		}

		@Override
		public Integer getHeapKey() {
			return distance;
		}

		@Override
		public void setHeapIndex(int idx) {
			this.heapIdx = idx;

		}

		@Override
		public void setHeapKey(Integer key) {
			distance = key;
		}

	}

	public static void main(String[] args) throws IOException, SQLException,
			ClassNotFoundException {
		String map = "berlin";
		int n = 10;

		// LevelGraph graph = new LevelGraph(DBConnection.getJdbcConnectionPg("localhost", 5432,
		// "berlin", "osm", "osm"));
		// org.mapsforge.preprocessing.routing.highwayHierarchies.util.Serializer.serialize(
		// new File(map), graph);
		LevelGraph graph = org.mapsforge.preprocessing.routing.highwayHierarchies.util.Serializer
				.deserialize(new File(map));

		DijkstraAlgorithm d = new DijkstraAlgorithm(graph);
		LinkedList<LevelVertex> sp = new LinkedList<LevelVertex>();
		LinkedList<Integer> hopIndices = new LinkedList<Integer>();

		Random rnd = new Random();
		long time = System.currentTimeMillis();
		for (int i = 0; i < n; i++) {
			int s = rnd.nextInt(graph.getLevel(0).numVertices());
			int t = rnd.nextInt(graph.getLevel(0).numVertices());
			int distance = d.getShortestPath(s, t, 0, sp, hopIndices, true, false);
			System.out.print(s + " -> " + t + "  :  ");
			for (LevelVertex v : sp) {
				System.out.print(v.getId() + ",");
			}
			System.out.println();

			System.out.print(s + " -> " + t + "  :  ");

			LevelVertex v = graph.getLevel(0).getVertex(s);
			System.out.print(v.getId());
			for (int idx : hopIndices) {
				v = v.getOutboundEdges()[idx].getTarget();
				System.out.print("," + v.getId());
			}
			System.out.println();
			sp.clear();
			hopIndices.clear();
		}
	}
}
