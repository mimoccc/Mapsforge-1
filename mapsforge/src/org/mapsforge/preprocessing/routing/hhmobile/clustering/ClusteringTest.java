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

import java.awt.Color;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.mapsforge.preprocessing.routing.hhmobile.clustering.DirectedWeightedStaticArrayGraph.Edge;
import org.mapsforge.preprocessing.routing.highwayHierarchies.renderer.HHRenderer;
import org.mapsforge.preprocessing.routing.highwayHierarchies.sql.HHDbReader;
import org.mapsforge.preprocessing.routing.highwayHierarchies.sql.HHDbReader.HHVertex;
import org.mapsforge.preprocessing.util.DBConnection;
import org.mapsforge.preprocessing.util.GeoCoordinate;
import org.mapsforge.server.routing.highwayHierarchies.HHRouter;
import org.mapsforge.server.routing.highwayHierarchies.HHRouterFactory;

public class ClusteringTest {

	public static void main(String[] args) throws SQLException, FileNotFoundException,
			IOException {

		// get data from db

		Connection conn = DBConnection.getJdbcConnectionPg("localhost", 5432, "osm_base",
				"osm", "osm");
		DirectedWeightedStaticArrayGraph graph = DirectedWeightedStaticArrayGraph.buildHHGraph(
				conn, 0);

		HHDbReader reader = new HHDbReader(conn);
		int n = reader.numVertices();
		int[] lon = new int[n];
		int[] lat = new int[n];

		for (Iterator<HHVertex> iter = reader.getVertices(); iter.hasNext();) {
			HHVertex v = iter.next();
			lon[v.id] = GeoCoordinate.dtoi(v.longitude);
			lat[v.id] = GeoCoordinate.dtoi(v.latitude);
		}

		HHRouter router = HHRouterFactory.getHHRouterInstance();

		// k-center
		int avgVerticesPerCluster = 100;
		KCenterClusteringAlgorithm kCenterAlgorithm = new KCenterClusteringAlgorithm();
		int k = (int) Math.rint(graph.numConnectedVertices() / avgVerticesPerCluster);
		KCenterClustering kCenterClustering = kCenterAlgorithm.computeClustering(graph, k,
				KCenterClusteringAlgorithm.HEURISTIC_MIN_SIZE);

		HHRenderer renderer1 = new HHRenderer(1920, 1200, router.routingGraph.graph,
				router.routingGraph.vertexIndex, 26);
		drawClustering(renderer1, getClusterColors(graph, kCenterClustering));
		renderer1.update();

		// quad
		QuadTreeClusteringAlgorithm quadAlgorithm = new QuadTreeClusteringAlgorithm();
		QuadTreeClustering quadClustering = quadAlgorithm.computeClustering(graph, lon, lat,
				QuadTreeClusteringAlgorithm.HEURISTIC_CENTER, 400);

		HHRenderer renderer2 = new HHRenderer(1920, 1200, router.routingGraph.graph,
				router.routingGraph.vertexIndex, 26);
		drawClustering(renderer2, getClusterColors(graph, quadClustering));
		renderer2.update();
	}

	public static void drawClustering(HHRenderer renderer, HashMap<ICluster, Color> colors) {
		for (ICluster c : colors.keySet()) {
			for (int v : c.getVertices()) {
				renderer.drawVertex(v, colors.get(c));
			}
		}
	}

	public static HashMap<ICluster, Color> getClusterColors(
			DirectedWeightedStaticArrayGraph graph, IClustering clustering) {
		HashMap<ICluster, Color> colors = new HashMap<ICluster, Color>();

		for (ICluster cluster : clustering.getClusters()) {
			HashSet<Color> adjColors = new HashSet<Color>();
			for (ICluster adj : getAdjClusters(graph, clustering, cluster)) {
				Color cAdj = colors.get(adj);
				if (cAdj != null) {
					adjColors.add(cAdj);
				}
			}
			for (int i = 0; i < colors_avail.length; i++) {
				if (!adjColors.contains(colors_avail[i])) {
					colors.put(cluster, colors_avail[i]);
					break;
				}
			}
		}
		return colors;
	}

	private static Color[] colors_avail = { Color.GREEN, Color.BLUE, Color.RED, Color.YELLOW,
			Color.MAGENTA, Color.CYAN, Color.black, Color.ORANGE, Color.PINK, Color.MAGENTA };

	public static ICluster[] getAdjClusters(DirectedWeightedStaticArrayGraph graph,
			IClustering clustering, ICluster cluster) {
		THashSet<ICluster> set = new THashSet<ICluster>();
		for (int v : cluster.getVertices()) {
			for (Edge e : graph.getOutboundEdges(graph.getVertex(v))) {
				ICluster c = clustering.getCluster(e.getTargetId());
				if (c != null && !c.equals(cluster)) {
					set.add(c);
				}
			}
		}
		ICluster[] adjClusters = new ICluster[set.size()];
		set.toArray(adjClusters);
		return adjClusters;
	}

}
