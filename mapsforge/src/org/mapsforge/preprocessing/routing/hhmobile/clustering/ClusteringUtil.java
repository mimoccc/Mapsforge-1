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

import org.mapsforge.preprocessing.routing.hhmobile.LevelGraph;
import org.mapsforge.preprocessing.routing.hhmobile.LevelGraph.Level;
import org.mapsforge.preprocessing.routing.hhmobile.util.graph.IEdge;
import org.mapsforge.preprocessing.routing.hhmobile.util.graph.IGraph;
import org.mapsforge.preprocessing.routing.highwayHierarchies.HHComputation;

public class ClusteringUtil {

	public static int getNumClusters(IClustering[] clustering) {
		int sum = 0;
		for (int i = 0; i < clustering.length; i++) {
			sum += clustering[i].size();
		}
		return sum;
	}

	public static int getClusterDegree(LevelGraph levelGraph, IClustering[] clustering,
			ICluster cluster, int clusterLevel) {
		int numAdj = getAdjacentClusters(clustering[clusterLevel], cluster, levelGraph
				.getLevel(clusterLevel)).length;
		int numSubj = getSubjacentClusters(clustering, cluster, clusterLevel).length;
		int numOverly = getOverlyingClusters(clustering, cluster, clusterLevel).length;

		return numAdj + numSubj + numOverly;
	}

	public static int getMaxClusterDegree(LevelGraph levelGraph, IClustering[] clustering,
			int lvl) {
		int max = 0;
		for (ICluster cluster : clustering[lvl].getClusters()) {
			max = Math.max(max, getClusterDegree(levelGraph, clustering, cluster, lvl));
		}
		return max;
	}

	public static int getMaxClusterDegree(LevelGraph levelGraph, IClustering[] clustering) {
		int max = 0;
		for (int lvl = 0; lvl < clustering.length; lvl++) {
			max = Math.max(max, getMaxClusterDegree(levelGraph, clustering, lvl));
		}
		return max;
	}

	public static int[] getMaxClusterDegrees(LevelGraph levelGraph, IClustering[] clustering) {
		int[] maxDegree = new int[levelGraph.numLevels()];
		for (int lvl = 0; lvl < clustering.length; lvl++) {
			maxDegree[lvl] = getMaxClusterDegree(levelGraph, clustering, lvl);
		}
		return maxDegree;
	}

	public static int getMaxVertexDegree(ICluster cluster, IGraph graph) {
		int max = 0;
		for (int v : cluster.getVertices()) {
			max = Math.max(max, graph.getVertex(v).getOutboundEdges().length);
		}
		return max;
	}

	public static int getMaxVertexDegree(IClustering clustering, IGraph graph) {
		int max = 0;
		for (ICluster cluster : clustering.getClusters()) {
			max = Math.max(max, getMaxVertexDegree(cluster, graph));
		}
		return max;
	}

	public static int getMaxVertexDegree(LevelGraph levelGraph, IClustering[] clustering) {
		int max = 0;
		for (int lvl = 0; lvl < clustering.length; lvl++) {
			max = Math.max(max, getMaxVertexDegree(clustering[lvl], levelGraph.getLevel(lvl)));
		}
		return max;
	}

	public static int[] getMaxVertexDegrees(LevelGraph levelGraph, IClustering[] clustering) {
		int[] maxVertexDegrees = new int[levelGraph.numLevels()];
		for (int lvl = 0; lvl < clustering.length; lvl++) {
			maxVertexDegrees[lvl] = getMaxVertexDegree(clustering[lvl], levelGraph
					.getLevel(lvl));
		}
		return maxVertexDegrees;
	}

	public static int getMinLongitude(ICluster cluster, int[] vertexLon) {
		int min = Integer.MAX_VALUE;
		for (int v : cluster.getVertices()) {
			min = Math.min(min, vertexLon[v]);
		}
		return min;
	}

	public static int getMinLatitude(ICluster cluster, int[] vertexLat) {
		int min = Integer.MAX_VALUE;
		for (int v : cluster.getVertices()) {
			min = Math.min(min, vertexLat[v]);
		}
		return min;
	}

	public static int getMaxLongitude(ICluster cluster, int[] vertexLon) {
		int max = Integer.MIN_VALUE;
		for (int v : cluster.getVertices()) {
			max = Math.max(max, vertexLon[v]);
		}
		return max;
	}

	public static int getMaxLatitude(ICluster cluster, int[] vertexLat) {
		int max = Integer.MIN_VALUE;
		for (int v : cluster.getVertices()) {
			max = Math.max(max, vertexLat[v]);
		}
		return max;
	}

	public static int getMaxNumVerticesPerCluster(IClustering clustering) {
		int max = 0;
		for (ICluster cluster : clustering.getClusters()) {
			max = Math.max(max, cluster.size());
		}
		return max;
	}

	public static int getMaxNumVerticesPerCluster(IClustering[] clustering) {
		int max = 0;
		for (int lvl = 0; lvl < clustering.length; lvl++) {
			max = Math.max(max, getMaxNumVerticesPerCluster(clustering[lvl]));
		}
		return max;
	}

	public static int getMinEdgeWeight(ICluster cluster, IGraph graph) {
		int min = Integer.MAX_VALUE;
		for (int v : cluster.getVertices()) {
			for (IEdge e : graph.getVertex(v).getOutboundEdges()) {
				min = Math.min(min, e.getWeight());
			}
		}
		return min;
	}

	public static int getMinEdgeWeight(IClustering clustering, IGraph graph) {
		int min = Integer.MAX_VALUE;
		for (ICluster cluster : clustering.getClusters()) {
			min = Math.min(min, getMinEdgeWeight(cluster, graph));
		}
		return min;
	}

	public static int[] getMinEdgeWeights(IClustering[] clustering, LevelGraph levelGraph) {
		int[] minWeights = new int[levelGraph.numLevels()];
		for (int lvl = 0; lvl < levelGraph.numLevels(); lvl++) {
			minWeights[lvl] = getMinEdgeWeight(clustering[lvl], levelGraph.getLevel(0));
		}
		return minWeights;
	}

	public static int getMaxEdgeWeight(ICluster cluster, IGraph graph) {
		int max = 0;
		for (int v : cluster.getVertices()) {
			for (IEdge e : graph.getVertex(v).getOutboundEdges()) {
				max = Math.max(max, e.getWeight());
			}
		}
		return max;
	}

	public static int getMaxEdgeWeight(IClustering clustering, IGraph graph) {
		int max = 0;
		for (ICluster cluster : clustering.getClusters()) {
			max = Math.max(max, getMaxEdgeWeight(cluster, graph));
		}
		return max;
	}

	public static int[] getMaxEdgeWeights(IClustering[] clustering, LevelGraph levelGraph) {
		int[] maxWeights = new int[levelGraph.numLevels()];
		for (int lvl = 0; lvl < levelGraph.numLevels(); lvl++) {
			maxWeights[lvl] = getMaxEdgeWeight(clustering[lvl], levelGraph.getLevel(lvl));
		}
		return maxWeights;
	}

	public static int getMinNeighborhood(ICluster cluster, Level graph) {
		int min = Integer.MAX_VALUE;
		for (int v : cluster.getVertices()) {
			int nh = graph.getVertex(v).getNeighborhood();
			min = Math.min(nh, min);
		}
		return min;
	}

	public static int getMinNeighborhood(IClustering clustering, Level graph) {
		int min = Integer.MAX_VALUE;
		for (ICluster cluster : clustering.getClusters()) {
			min = Math.min(min, getMinNeighborhood(cluster, graph));
		}
		if (min == Integer.MAX_VALUE) {
			min = 0;
		}
		return min;
	}

	public static int[] getMinNeighborhoods(IClustering[] clustering, LevelGraph levelGraph) {
		int[] min = new int[levelGraph.numLevels()];
		for (int lvl = 0; lvl < levelGraph.numLevels(); lvl++) {
			min[lvl] = getMinNeighborhood(clustering[lvl], levelGraph.getLevel(lvl));
		}
		return min;
	}

	public static int getMaxNeighborhood(ICluster cluster, Level graph) {
		int max = 0;
		for (int v : cluster.getVertices()) {
			int nh = graph.getVertex(v).getNeighborhood();
			if (nh != HHComputation.INFINITY_1 && nh != HHComputation.INFINITY_2) {
				max = Math.max(nh, max);
			}
		}
		return max;
	}

	public static int getMaxNeighborhood(IClustering clustering, Level graph) {
		int max = 0;
		for (ICluster cluster : clustering.getClusters()) {
			max = Math.max(max, getMaxNeighborhood(cluster, graph));
		}
		return max;
	}

	public static int[] getMaxNeighborhoods(IClustering[] clustering, LevelGraph levelGraph) {
		int[] max = new int[levelGraph.numLevels()];
		for (int lvl = 0; lvl < levelGraph.numLevels(); lvl++) {
			max[lvl] = getMaxNeighborhood(clustering[lvl], levelGraph.getLevel(lvl));
		}
		return max;
	}

	public static ICluster[] getAdjacentClusters(IClustering clustering, ICluster cluster,
			IGraph graph) {
		THashSet<ICluster> set = new THashSet<ICluster>();
		for (int v : cluster.getVertices()) {
			for (IEdge e : graph.getVertex(v).getOutboundEdges()) {
				ICluster c = clustering.getCluster(e.getTarget().getId());
				if (c != null && !c.equals(cluster)) {
					set.add(c);
				}
			}
		}
		ICluster[] adjClusters = new ICluster[set.size()];
		set.toArray(adjClusters);
		return adjClusters;
	}

	public static ICluster[] getSubjacentClusters(IClustering[] clustering, ICluster cluster,
			int clusterLevel) {
		if (clusterLevel == 0) {
			return new ICluster[0];
		}

		THashSet<ICluster> set = new THashSet<ICluster>();
		for (int v : cluster.getVertices()) {
			ICluster c = clustering[clusterLevel - 1].getCluster(v);
			if (c != null) {
				set.add(c);
			}
		}
		ICluster[] subjClusters = new ICluster[set.size()];
		set.toArray(subjClusters);
		return subjClusters;
	}

	public static ICluster[] getOverlyingClusters(IClustering[] clustering, ICluster cluster,
			int clusterLevel) {
		if (clusterLevel == clustering.length - 1) {
			return new ICluster[0];
		}

		THashSet<ICluster> set = new THashSet<ICluster>();
		for (int v : cluster.getVertices()) {
			ICluster c = clustering[clusterLevel + 1].getCluster(v);
			if (c != null) {
				set.add(c);
			}
		}
		ICluster[] overlClusters = new ICluster[set.size()];
		set.toArray(overlClusters);
		return overlClusters;
	}

}
