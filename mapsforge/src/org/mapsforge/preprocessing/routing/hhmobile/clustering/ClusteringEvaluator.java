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

import java.awt.Color;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Iterator;

import org.mapsforge.preprocessing.routing.hhmobile.clustering.DirectedWeightedStaticArrayGraph.Edge;
import org.mapsforge.preprocessing.routing.highwayHierarchies.HHDbReader;
import org.mapsforge.preprocessing.routing.highwayHierarchies.HHDbReader.HHVertex;
import org.mapsforge.preprocessing.routing.highwayHierarchies.util.renderer.RendererV2;
import org.mapsforge.preprocessing.util.DBConnection;
import org.mapsforge.preprocessing.util.GeoCoordinate;
import org.mapsforge.server.routing.IRouter;
import org.mapsforge.server.routing.RouterFactory;

public class ClusteringEvaluator {

	public static void main(String[] args) throws SQLException {

		// get data from db

		Connection conn = DBConnection.getJdbcConnectionPg("localhost", 5432, "germany",
				"postgres", "admin");
		DirectedWeightedStaticArrayGraph graph = DirectedWeightedStaticArrayGraph.buildHHGraph(
				conn, 0);
		IRouter router = RouterFactory.getRouter();

		HHDbReader reader = new HHDbReader(conn);
		int n = reader.numVertices();
		int[] lon = new int[n];
		int[] lat = new int[n];

		for (Iterator<HHVertex> iter = reader.getVertices(); iter.hasNext();) {
			HHVertex v = iter.next();
			lon[v.id] = GeoCoordinate.dtoi(v.longitude);
			lat[v.id] = GeoCoordinate.dtoi(v.latitude);
		}

		// k-center
		int avgVerticesPerCluster = 500;
		KCenterClusteringAlgorithm kCenterAlgorithm = new KCenterClusteringAlgorithm();
		int k = (int) Math.rint(graph.numConnectedVertices() / avgVerticesPerCluster);
		KCenterClustering kCenterClustering = kCenterAlgorithm.computeClustering(graph, k,
				KCenterClusteringAlgorithm.HEURISTIC_MIN_SIZE);

		// quad
		// QuadTreeClusteringAlgorithm quadAlgorithm = new QuadTreeClusteringAlgorithm();
		// QuadTreeClustering quadClustering = quadAlgorithm.computeClustering(graph, lon, lat,
		// QuadTreeClusteringAlgorithm.HEURISTIC_CENTER, avgVerticesPerCluster * 2);
		//
		// // dijkstra based
		// DijkstraBasedClusteringAlgorithm dbAlgorithm = new
		// DijkstraBasedClusteringAlgorithm();
		// QuadTreeClustering dbClustering = dbAlgorithm.computeClustering(graph,
		// avgVerticesPerCluster);

		// render
		// renderClustering(router, kCenterClustering);
		// renderClustering(router, quadClustering);
		// renderClustering(router, dbClustering);

		evaluateClustering(kCenterClustering, graph);
	}

	private static void renderClustering(IRouter router, IClustering clustering) {
		RendererV2 renderer1 = new RendererV2(1024, 768, router, Color.white, Color.black);
		renderer1.setClustering(clustering);

	}

	private static void evaluateClustering(IClustering clustering,
			DirectedWeightedStaticArrayGraph graph) {
		int[] countV, countInternalE, countExternalE, countVE, percentInternalE;
		countV = new int[clustering.size()];
		countInternalE = new int[clustering.size()];
		countExternalE = new int[clustering.size()];
		countVE = new int[clustering.size()];
		percentInternalE = new int[clustering.size()];
		// compute counts
		int i = 0;
		for (ICluster c : clustering.getClusters()) {
			countV[i] = c.size();
			countVE[i] = c.size();
			countInternalE[i] = 0;
			countExternalE[i] = 0;
			for (int v : c.getVertices()) {
				for (Edge e : graph.getOutboundEdges(graph.getVertex(v))) {
					if (clustering.getCluster(e.getTargetId()) == c) {
						countInternalE[i]++;
					} else {
						countExternalE[i]++;
					}
					countVE[i]++;
				}
			}
			percentInternalE[i] = (int) Math
					.rint((((double) countInternalE[i]) / ((double) (countInternalE[i] + countExternalE[i]))) * 100);
			i++;
		}

		int minV = min(countV);
		int maxV = max(countV);
		int minInternalE = min(countInternalE);
		int maxInternalE = max(countInternalE);
		int minExternalE = min(countExternalE);
		int maxExternalE = max(countExternalE);
		int minVE = min(countVE);
		int maxVE = max(countVE);
		int minPercentInternalE = min(percentInternalE);
		int maxPercentInternalE = max(percentInternalE);

		System.out.println("|V| = [" + minV + ", " + maxV + "]");
		System.out.println("|E|(internal) = [" + minInternalE + ", " + maxInternalE + "]");
		System.out.println("|V|(external) = [" + minExternalE + ", " + maxExternalE + "]");
		System.out.println("|V|+|E| = [" + minVE + ", " + maxVE + "]");
		System.out.println("|E|(internal) = [" + minPercentInternalE + "%, "
				+ maxPercentInternalE + "%]");

		int numIntervals = 20;
		int[] distV = intervalCount(countV, numIntervals);
		int[] distInternalE = intervalCount(countInternalE, numIntervals);
		int[] distExternalE = intervalCount(countExternalE, numIntervals);
		int[] distVE = intervalCount(countVE, numIntervals);
		int[] distPercentInternalE = intervalCount(percentInternalE, numIntervals);

		System.out.println("distV : " + arrToS(distV));
		System.out.println("distE(internal) : " + arrToS(distInternalE));
		System.out.println("distE(external) : " + arrToS(distExternalE));
		System.out.println("distVE : " + arrToS(distVE));
		System.out.println("distE(internal%) : " + arrToS(distPercentInternalE));
	}

	private static int[] intervalCount(int[] values, int n) {
		int[] counts = new int[n];
		double min = min(values);
		double max = max(values) + 0.000000001;
		double intervalSize = (max - min) / n;
		for (double v : values) {
			counts[(int) Math.floor((v - min) / intervalSize)]++;
		}
		return counts;
	}

	private static String arrToS(int[] arr) {
		StringBuilder sb = new StringBuilder();
		for (int i : arr) {
			sb.append(i + ", ");
		}
		return sb.toString();
	}

	private static int min(int[] arr) {
		int min = Integer.MAX_VALUE;
		for (int i = 0; i < arr.length; i++) {
			min = Math.min(arr[i], min);
		}
		return min;
	}

	private static int max(int[] arr) {
		int max = Integer.MIN_VALUE;
		for (int i = 0; i < arr.length; i++) {
			max = Math.max(arr[i], max);
		}
		return max;
	}

	private static double min(double[] arr) {
		double min = Integer.MAX_VALUE;
		for (int i = 0; i < arr.length; i++) {
			min = Math.min(arr[i], min);
		}
		return min;
	}

	private static double max(double[] arr) {
		double max = Integer.MIN_VALUE;
		for (int i = 0; i < arr.length; i++) {
			max = Math.max(arr[i], max);
		}
		return max;
	}
}