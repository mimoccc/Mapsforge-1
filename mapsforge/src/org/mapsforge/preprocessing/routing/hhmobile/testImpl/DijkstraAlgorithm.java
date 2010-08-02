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

import org.mapsforge.core.GeoCoordinate;
import org.mapsforge.preprocessing.routing.hhmobile.testImpl.routingGraph.DummyCache;
import org.mapsforge.preprocessing.routing.hhmobile.testImpl.routingGraph.Edge;
import org.mapsforge.preprocessing.routing.hhmobile.testImpl.routingGraph.RoutingGraph;
import org.mapsforge.preprocessing.routing.hhmobile.testImpl.routingGraph.Vertex;
import org.mapsforge.preprocessing.routing.highwayHierarchies.util.prioQueue.BinaryMinHeap;
import org.mapsforge.preprocessing.routing.highwayHierarchies.util.prioQueue.IBinaryHeapItem;
import org.mapsforge.preprocessing.routing.highwayHierarchies.util.renderer.RendererV2;
import org.mapsforge.server.routing.RouterFactory;

public class DijkstraAlgorithm {

	private final BinaryMinHeap<HeapItem, Integer> queue;
	private final RoutingGraph graph;
	private final TIntObjectHashMap<HeapItem> discovered;

	public DijkstraAlgorithm(RoutingGraph graph) {
		this.queue = new BinaryMinHeap<HeapItem, Integer>(10000);
		this.graph = graph;
		this.discovered = new TIntObjectHashMap<HeapItem>();
	}

	public int getShortestPath(int sourceId, int targetId, LinkedList<Vertex> shortestPathBuff)
			throws IOException {
		this.queue.clear();
		this.discovered.clear();

		HeapItem s = new HeapItem(sourceId, 0, null);
		queue.insert(s);
		discovered.put(s.vertexId, s);
		while (!queue.isEmpty()) {
			HeapItem _u = queue.extractMin();
			Vertex u = graph.getVertex(_u.vertexId);
			if (u.getId() == targetId) {
				break;
			}
			for (Edge e : u.getOutboundEdges()) {
				if (!e.isForward()) {
					continue;
				}
				HeapItem _v = discovered.get(e.getTargetId());
				if (_v == null) {
					_v = new HeapItem(e.getTargetId(), _u.distance + e.getWeight(), u);
					queue.insert(_v);
					discovered.put(_v.vertexId, _v);
				} else if (_v.distance > _u.distance + e.getWeight()) {
					queue.decreaseKey(_v, _u.distance + e.getWeight());
					_v.parent = u;
				}
			}
		}
		HeapItem _t = discovered.get(targetId);
		if (_t == null) {
			return Integer.MAX_VALUE;
		}
		int distance = _t.distance;
		shortestPathBuff.add(graph.getVertex(targetId));
		while (_t.parent != null) {
			shortestPathBuff.add(_t.parent);
			_t = discovered.get(_t.parent.getId());
		}
		return distance;
	}

	private class HeapItem implements IBinaryHeapItem<Integer> {

		private int heapIdx;
		private int distance;
		private Vertex parent;
		private int vertexId;

		public HeapItem(int vertexId, int distance, Vertex parent) {
			this.vertexId = vertexId;
			this.distance = distance;
			this.parent = parent;
			this.heapIdx = -1;
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

	public static void main(String[] args) throws IOException {
		String map = "berlin";
		int n = 10;

		RoutingGraph graph = new RoutingGraph(new File(map + ".mobile_hh"), new DummyCache());

		DijkstraAlgorithm d = new DijkstraAlgorithm(graph);
		RendererV2 renderer = new RendererV2(1024, 768, RouterFactory.getRouter(), Color.WHITE,
				Color.BLACK);
		LinkedList<Vertex> sp = new LinkedList<Vertex>();

		long time = System.currentTimeMillis();
		for (int i = 0; i < n; i++) {
			Vertex s = graph.getRandomVertex(0);
			Vertex t = graph.getRandomVertex(0);
			int distance = d.getShortestPath(s.getId(), t.getId(), sp);
			for (Vertex v : sp) {
				Vertex v_ = graph.getVertex(v.getIdLvlZero());
				renderer.addCircle(new GeoCoordinate(v_.getLat(), v_.getLon()), Color.BLUE);

				Edge e = v.getOutboundEdges()[0];
				Vertex et = graph.getVertex(e.getTargetId());
				if (et.getIdLvlZero() != e.getTargetIdLvlZero()) {
					System.out.println("error");
				}
			}
			// renderer.addCircle(new GeoCoordinate(s.getLat(), s.getLon()), Color.GREEN);
			// renderer.addCircle(new GeoCoordinate(t.getLat(), t.getLon()), Color.GREEN);
			sp.clear();
		}
		System.out.println("num routes : " + n);
		System.out.println("exec time : " + (System.currentTimeMillis() - time) + "ms.");
	}
}
