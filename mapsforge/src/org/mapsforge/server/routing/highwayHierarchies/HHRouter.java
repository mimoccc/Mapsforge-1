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
package org.mapsforge.server.routing.highwayHierarchies;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Random;

import org.mapsforge.core.conf.IVehicle;
import org.mapsforge.preprocessing.routing.highwayHierarchies.datastructures.HHStaticGraph.HHStaticEdge;
import org.mapsforge.preprocessing.routing.highwayHierarchies.util.geo.PolarCoordinate;
import org.mapsforge.preprocessing.util.GeoCoordinate;
import org.mapsforge.server.core.geoinfo.Point;
import org.mapsforge.server.routing.core.IGeoMap;
import org.mapsforge.server.routing.core.IRoutingGraph;
import org.mapsforge.server.routing.core.PropertyConfiguration;
import org.mapsforge.server.routing.core.Router;

/**
 * @author Frank Viernau This will implements the OsmNavigation projects Interfaces for routing.
 */
public class HHRouter extends Router {

	public final HHCompleteRoutingGraph routingGraph;
	private final HHAlgorithm hhAlgorithm;

	HHRouter(HHCompleteRoutingGraph routingGraph) {
		super(routingGraph, null);
		this.routingGraph = routingGraph;
		this.hhAlgorithm = new HHAlgorithm();
	}

	public HHRouter(IVehicle vehicle, IRoutingGraph routingGraph, IGeoMap geoMap,
			PropertyConfiguration<?> propConf) {
		super((HHCompleteRoutingGraph) routingGraph, vehicle);
		this.routingGraph = (HHCompleteRoutingGraph) routingGraph;
		this.hhAlgorithm = new HHAlgorithm();
	}

	/**
	 * @param lon
	 * @param lat
	 * @return entry point in routing graph.
	 */
	public int getNearestVertexId(double lon, double lat) {
		return routingGraph.vertexIndex.getNearestNeighborId(GeoCoordinate.dtoi(lon),
				GeoCoordinate.dtoi(lat));
	}

	/**
	 * @param vertexId
	 * @return coordinate of vertex.
	 */
	public PolarCoordinate getCoordinate(int vertexId) {
		GeoCoordinate c = routingGraph.vertexIndex.getCoordinate(vertexId);
		return new PolarCoordinate(c.getLongitudeInt(), c.getLatitudeInt());
	}

	/**
	 * @param sourceId
	 * @param targetId
	 * @return shortest path between vertices, null if none is found.
	 */
	public LinkedList<HHStaticEdge> getShortestPath(int sourceId, int targetId) {
		LinkedList<HHStaticEdge> searchSpace = new LinkedList<HHStaticEdge>();
		LinkedList<HHStaticEdge> fwd = new LinkedList<HHStaticEdge>();
		LinkedList<HHStaticEdge> bwd = new LinkedList<HHStaticEdge>();
		LinkedList<HHStaticEdge> expandedBwd = new LinkedList<HHStaticEdge>();
		int distance = hhAlgorithm.shortestPath(routingGraph.graph, sourceId, targetId,
				routingGraph.distanceTable, fwd, bwd, searchSpace);
		if (distance == Integer.MAX_VALUE) {
			return null;
		}
		LinkedList<HHStaticEdge> sp = new LinkedList<HHStaticEdge>();
		routingGraph.edgeExpander.expandShortestPath(fwd, sp);
		routingGraph.edgeExpander.expandShortestPath(bwd, expandedBwd);
		routingGraph.edgeReverser.reverseEdges(expandedBwd, sp);
		return sp;
	}

	@Override
	public String getAlgorithmName() {
		return "highway hierarchies";
	}

	@Override
	public IRoutingGraph getRoutingGraph() {
		return routingGraph;
	}

	@Override
	protected synchronized int[] route(Point source, Point destination) {
		int srcID = routingGraph.vertexIndex.getNearestNeighborId(source.getLon(), source
				.getLat());

		int dstID = routingGraph.vertexIndex.getNearestNeighborId(destination.getLon(),
				destination.getLat());

		// TODO: route berechnen

		return new int[] { srcID, dstID };
	}

	public static void main(String[] args) throws FileNotFoundException, IOException {
		HHRouter router = HHRouterFactory.getHHRouterInstance();
		Random rnd = new Random(123456);
		long start = System.currentTimeMillis();
		for (int i = 0; i < 10000; i++) {
			int s = rnd.nextInt(router.routingGraph.graph.numVertices());
			int t = rnd.nextInt(router.routingGraph.graph.numVertices());

			router.getShortestPath(s, t);
		}
		long end = System.currentTimeMillis();
		System.out.println((end - start) + "ms");
	}
}
