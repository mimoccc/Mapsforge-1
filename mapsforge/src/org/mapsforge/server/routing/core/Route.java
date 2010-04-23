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
package org.mapsforge.server.routing.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.mapsforge.server.core.geoinfo.IPoint;
import org.mapsforge.server.core.geoinfo.IWay;
import org.mapsforge.server.core.geoinfo.Node;
import org.mapsforge.server.core.geoinfo.Point;
import org.mapsforge.server.routing.core.IGeoMap.VertexDistance;

public final class Route extends MetaWay {

	public final class Section extends MetaWay {

		public final class Way implements IWay {

			private final Node destination;

			private final List<Node> imNodes;
			private final Node source;

			private Way(Node source, List<Node> imNodes, Node destination) {
				this.source = source;
				this.imNodes = imNodes;
				this.destination = destination;
			}

			@Override
			public Node destination() {
				return this.destination;
			}

			@Override
			public Map<Attribute, String> getAttributes() {
				// TODO read attributes from GeoMap / RoutingGraph
				return Collections.emptyMap();
			}

			@Override
			public List<Node> intermediateNodes() {
				return this.imNodes;
			}

			public Section section() {
				return Section.this;
			}

			@Override
			public Node source() {
				return this.source;
			}

		}

		private final Point destinationWp;
		private final Point sourceWp;
		private final int[] vertices;

		private volatile List<Way> ways;

		private Section(IPoint src, int[] vertices, IPoint dst) {
			this.sourceWp = Point.getInstance(src);
			this.destinationWp = Point.getInstance(dst);

			/** make a defensive copy of the vertices array */
			this.vertices = Arrays.copyOf(vertices, vertices.length);
			LOGGER.fine("new Section (" + src + "," + Arrays.toString(vertices) + "," + dst
					+ ")");
		}

		public Route route() {
			return Route.this;
		}

		/**
		 * Determines all Ways (complete and incomplete ways between vertices).
		 * 
		 * @return a List of all Edges.
		 */
		public List<Way> ways() {
			List<Way> res = this.ways;
			if (res == null) {
				if (this.vertices.length == 0) {
					res = Collections.singletonList(newWay(this.sourceWp, this.destinationWp));
				} else {
					res = new ArrayList<Way>();

					/** find the preceding/first vertex and add incipient Way */
					VertexDistance[] vdsPre = Route.this.geoMap
							.getAdjacentVertices(this.sourceWp);

					int firstVtxId = this.vertices[0];

					if (vdsPre.length > 1) {
						/**
						 * use first the two vertices (should not be more provided, but who
						 * knows..)
						 */
						if (vdsPre[0].vtxId != firstVtxId) {
							if (vdsPre[1].distance != 0)
								/**
								 * if the second vertex is not the point itself (otherwise skip
								 * edge)
								 */
								res.add(newWay(this.sourceWp, vdsPre[0].vtxId, firstVtxId));
						} else if (vdsPre[0].distance != 0)
							/**
							 * if the first vertex is not the point itself (otherwise skip edge)
							 */
							res.add(newWay(this.sourceWp, vdsPre[1].vtxId, firstVtxId));
					} else if (vdsPre.length == 1) {
						if (vdsPre[0].distance != 0 || vdsPre[0].vtxId != firstVtxId)
							/** add incipient Way */
							res.add(newWay(this.sourceWp, vdsPre[0].vtxId, firstVtxId));
					}
					/** else (if no vertices provided), omit Way */

					/** add intermediate Edges */
					for (int i = 0; i < this.vertices.length - 1; i++)
						res.add(newWay(this.vertices[i], this.vertices[i + 1]));

					/** find the subsequent/final vertex and add terminal Way */
					VertexDistance[] vdsPost = Route.this.geoMap
							.getAdjacentVertices(this.destinationWp);

					int lastVtxId = this.vertices[this.vertices.length - 1];

					if (vdsPost.length > 1) {
						/**
						 * use first the two vertices (should not be more provided, but who
						 * knows..)
						 */
						if (vdsPost[0].vtxId != lastVtxId) {
							if (vdsPost[1].distance != 0)
								/**
								 * if the second vertex is not the point itself (otherwise skip
								 * edge)
								 */
								res
										.add(newWay(lastVtxId, vdsPost[0].vtxId,
												this.destinationWp));
						} else if (vdsPost[0].distance != 0)
							/**
							 * if the first vertex is not the point itself (otherwise skip edge)
							 */
							res.add(newWay(lastVtxId, vdsPost[1].vtxId, this.destinationWp));
					} else if (vdsPost.length == 1) {
						if (vdsPost[0].distance != 0 || vdsPost[0].vtxId != lastVtxId)
							/** add incipient Way */
							res.add(newWay(this.sourceWp, vdsPost[0].vtxId, lastVtxId));
					}
					/** else (if no vertices provided), omit Way */

					res = Collections.unmodifiableList(res);
				}
				this.ways = res;
			}
			return res;
		}

		@Override
		protected List<? extends IWay> subTraces() {
			return ways();
		}

		/**
		 * create Way over the whole way between vertex with srcId and dstId.
		 * 
		 * @param srcId
		 * @param dstId
		 */
		private Way newWay(int srcId, int dstId) {
			if (srcId == dstId)
				throw new IllegalArgumentException(Issue.ASSERTION_ERR.msg());
			return new Way(Route.this.geoMap.vertexNode(srcId), Route.this.geoMap
					.intermediateNodes(srcId, dstId), Route.this.geoMap.vertexNode(dstId));
		}

		/**
		 * Creates final Way from the way's beginning to stop point on the way between vertex
		 * with srcId and dstId. <br/>
		 * Intermediate stop, set stop point as source and trim intermediate nodes list
		 * 
		 * @param srcId
		 * @param dstId
		 * @param stopP
		 */
		private Way newWay(int srcId, int dstId, IPoint stopP) {
			if (srcId == dstId)
				throw new IllegalArgumentException(Issue.ASSERTION_ERR.msg());

			/** retrieve intermediate Nodes from the GeoMap component */
			List<Node> nList = Route.this.geoMap.intermediateNodes(srcId, dstId);

			/** if the edge is restricted to points */
			int stop = -1;
			for (int n = 0; n < nList.size(); n++) {
				if (nList.get(n).matches(stopP))
					stop = n;
			}

			Node source = Route.this.geoMap.vertexNode(srcId);

			if (stop == -1) {
				// TODO: srcId/dstId
				return new Way(source, Collections.<Node> emptyList(), Route.this.geoMap
						.vertexNode(dstId));
			}
			return new Way(source,
					Collections.unmodifiableList(nList.subList(0, stop - 1 + 1)), nList
							.get(stop));
		}

		/**
		 * Creates initial Way from start point to end on the way between vertex with srcId and
		 * dstId. <br/>
		 * Intermediate start, set start point as source and trim intermediate nodes list
		 * 
		 * @param startP
		 * @param srcId
		 * @param dstId
		 */
		private Way newWay(IPoint startP, int srcId, int dstId) {
			if (srcId == dstId)
				throw new IllegalArgumentException(Issue.ASSERTION_ERR.msg());

			/** retrieve intermediate Nodes from the GeoMap component */
			List<Node> nList = Route.this.geoMap.intermediateNodes(srcId, dstId);

			/** if the edge is restricted to points */
			int start = -1;
			for (int n = 0; n < nList.size(); n++) {
				if (nList.get(n).matches(startP))
					start = n;
			}

			Node destination = Route.this.geoMap.vertexNode(dstId);

			if (start == -1) {
				// TODO: srcId/dstId
				return new Way(Route.this.geoMap.vertexNode(srcId), Collections
						.<Node> emptyList(), destination);
			}
			return new Way(nList.get(start), Collections.unmodifiableList(nList.subList(start,
					nList.size())), destination);
		}

		/**
		 * create Way from start point to stop point.
		 * 
		 * @param startP
		 * @param stopP
		 */
		private Way newWay(Point startP, Point stopP) {
			// TODO: some bugs ?? -> rewrite !

			VertexDistance[] vdStart = Route.this.geoMap.getAdjacentVertices(startP);
			VertexDistance[] vdStop = Route.this.geoMap.getAdjacentVertices(stopP);

			/** catch empty/less than 2 length-array */
			if (vdStart.length == 0 || vdStop.length == 0)
				/** no way point */
				throw new IllegalArgumentException(Issue.ASSERTION_ERR.msg());

			int srcId;
			int dstId;

			/** crosswise check the start and destination vertices of this edge */
			if (vdStart.length == 2) {
				if (vdStart[0].vtxId != vdStop[0].vtxId) {
					srcId = vdStart[0].vtxId;
					dstId = vdStop[0].vtxId;
				} else {
					srcId = vdStart[1].vtxId;
					dstId = vdStop[0].vtxId;
				}
			} else if (vdStart.length == 1 && vdStart[0].vtxId != vdStop[0].vtxId) {
				srcId = vdStart[0].vtxId;
				dstId = vdStop[0].vtxId;
			} else if (vdStart.length == 1 && vdStop.length == 2) {
				srcId = vdStart[0].vtxId;
				dstId = vdStop[1].vtxId;
			} else
				throw new IllegalStateException();

			/** retrieve intermediate Nodes from the GeoMap component */
			List<Node> nList = new LinkedList<Node>(Route.this.geoMap.intermediateNodes(srcId,
					dstId));
			nList.add(0, Route.this.geoMap.vertexNode(srcId));
			nList.add(Route.this.geoMap.vertexNode(dstId));

			/** test for the restricting points */
			int start = -1, stop = -1;
			for (int n = 0; n < nList.size(); n++) {
				if (nList.get(n).matches(startP))
					start = n;
				if (nList.get(n).matches(stopP))
					stop = n;
			}

			if (start <= stop) {
				Node source = nList.get(start < 0 ? 0 : start);
				Node destination = nList.get(stop < 0 ? nList.size() - 1 : stop);
				return new Way(source, nList.subList(start + 1, stop), destination);
			}
			Node source = nList.get(stop < 0 ? 0 : stop);
			Node destination = nList.get(start < 0 ? nList.size() - 1 : start);
			return new Way(destination, nList.subList(stop + 1, start), source);
		}
	}

	private static final Logger LOGGER = Logger.getLogger(Route.class.getCanonicalName());

	private final IGeoMap geoMap;

	private final IRoutingGraph routingGraph;

	private final List<Section> sections = new LinkedList<Section>();

	/**
	 * package private method
	 * 
	 * @param geoMap
	 * @param routingGraph
	 */
	Route(IGeoMap geoMap, IRoutingGraph routingGraph) {
		this.geoMap = geoMap;
		this.routingGraph = routingGraph;
	}

	public void addSection(IPoint start, int[] vertices, IPoint end) {
		/** perform a feasibility check before appending this section */
		if (!this.sections.isEmpty()
				&& !this.sections.get(this.sections.size() - 1).destinationWp.equals(start))
			throw new IllegalArgumentException(Issue.ASSERTION_ERR.msg());

		/** append a newly created Section now */
		this.sections.add(new Section(start, vertices, end));
	}

	public List<Section> sections() {
		/**
		 * since a Route object is modifiable, the internal data from sections is not allowed to
		 * be changed from external code
		 */
		return Collections.unmodifiableList(this.sections);
	}

	@Override
	protected List<? extends IWay> subTraces() {
		return sections();
	}
}

abstract class MetaWay implements IWay {

	private volatile EnumMap<Attribute, String> attributes;

	@Override
	public final Node destination() {
		List<? extends IWay> subTraces = subTraces();

		if (subTraces.isEmpty())
			return null;

		return subTraces.get(subTraces.size() - 1).destination();
	}

	@Override
	public final EnumMap<Attribute, String> getAttributes() {
		EnumMap<Attribute, String> res = this.attributes;
		if (res == null) {
			res = new EnumMap<Attribute, String>(Attribute.class);
			for (IWay w : subTraces()) {
				Map<Attribute, String> wAtts = w.getAttributes();
				for (Attribute key : wAtts.keySet()) {
					String val = res.get(key);
					if (val != null)
						res.put(key, Integer.parseInt(val) + wAtts.get(key));
					else
						res.put(key, wAtts.get(key));
				}
			}
			this.attributes = res;
		}
		return res;
	}

	@Override
	public final List<Node> intermediateNodes() {
		List<? extends IWay> subTraces = subTraces();

		if (subTraces.isEmpty())
			return Collections.emptyList();

		List<Node> res = new ArrayList<Node>();
		for (IWay w : subTraces) {
			res.addAll(w.intermediateNodes());
			res.add(w.destination());
		}
		/**
		 * cut off last element: this is the destination an no intermediate node
		 */
		return res.subList(0, res.size() - 1);
	}

	@Override
	public final Node source() {
		List<? extends IWay> subTraces = subTraces();

		if (subTraces.isEmpty())
			return null;

		return subTraces().get(0).source();
	}

	protected abstract List<? extends IWay> subTraces();
}
