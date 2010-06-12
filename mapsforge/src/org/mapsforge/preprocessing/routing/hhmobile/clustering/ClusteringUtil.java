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

import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.hash.THashSet;

import java.util.LinkedList;

import org.mapsforge.preprocessing.routing.hhmobile.graph.IEdge;
import org.mapsforge.preprocessing.routing.hhmobile.graph.IGraph;
import org.mapsforge.preprocessing.routing.hhmobile.graph.LevelGraph;
import org.mapsforge.preprocessing.routing.hhmobile.graph.LevelGraph.Level;
import org.mapsforge.preprocessing.routing.hhmobile.graph.LevelGraph.Level.LevelEdge;
import org.mapsforge.preprocessing.routing.hhmobile.graph.LevelGraph.Level.LevelVertex;
import org.mapsforge.preprocessing.routing.hhmobile.util.Utils;
import org.mapsforge.preprocessing.routing.highwayHierarchies.HHComputation;

public class ClusteringUtil {

	private final IClustering[] clustering;
	private final Level[] graphLevel;
	private final TObjectIntHashMap<ICluster> clusterLevels;
	private final TIntIntHashMap[] vertexIdToClusterOffset;
	private final int[] lvlMaxVerticesPerCluster, lvlMaxEdgesPerCluster, lvlMaxNeighborhood;

	public ClusteringUtil(IClustering[] clustering, LevelGraph levelGraph) {
		this.clustering = clustering;
		this.graphLevel = levelGraph.getLevels();
		this.clusterLevels = new TObjectIntHashMap<ICluster>();
		this.vertexIdToClusterOffset = new TIntIntHashMap[clustering.length];

		for (int lvl = 0; lvl < clustering.length; lvl++) {
			vertexIdToClusterOffset[lvl] = new TIntIntHashMap(graphLevel[lvl].numVertices());
			for (ICluster c : clustering[lvl].getClusters()) {
				clusterLevels.put(c, lvl);
				int[] clusterVertices = c.getVertices();
				for (int offset = 0; offset < clusterVertices.length; offset++) {
					int v = clusterVertices[offset];
					vertexIdToClusterOffset[lvl].put(v, offset);
				}
			}
		}

		this.lvlMaxVerticesPerCluster = new int[clustering.length];
		this.lvlMaxEdgesPerCluster = new int[clustering.length];
		this.lvlMaxNeighborhood = new int[clustering.length];

		for (int lvl = 0; lvl < clustering.length; lvl++) {
			lvlMaxVerticesPerCluster[lvl] = 0;
			lvlMaxEdgesPerCluster[lvl] = 0;
			lvlMaxNeighborhood[lvl] = 0;
			for (ICluster c : clustering[lvl].getClusters()) {
				lvlMaxVerticesPerCluster[lvl] = Math.max(lvlMaxVerticesPerCluster[lvl], c
						.size());
				int edgesInCluster = 0;
				for (int v : c.getVertices()) {
					edgesInCluster += graphLevel[lvl].getVertex(v).getOutboundEdges().length;
					LevelVertex v_ = graphLevel[lvl].getVertex(v);
					int nh = v_.getNeighborhood();
					if (nh != HHComputation.INFINITY_1 && nh != HHComputation.INFINITY_2) {
						lvlMaxNeighborhood[lvl] = Math.max(lvlMaxNeighborhood[lvl], v_
								.getNeighborhood());
					}
				}
				lvlMaxEdgesPerCluster[lvl] = Math.max(lvlMaxEdgesPerCluster[lvl],
						edgesInCluster);
			}
		}
	}

	public int maxVerticesPerCluster() {
		return Utils.max(lvlMaxVerticesPerCluster);
	}

	public int maxEdgesPerCluster() {
		return Utils.max(lvlMaxEdgesPerCluster);
	}

	public int maxNeighborhood() {
		return Utils.max(lvlMaxNeighborhood);
	}

	public static int getNumClusters(IClustering[] clustering) {
		int sum = 0;
		for (int i = 0; i < clustering.length; i++) {
			sum += clustering[i].size();
		}
		return sum;
	}

	public int numClusters() {
		return clusterLevels.size();
	}

	public int numLevels() {
		return clustering.length;
	}

	public int getClusterLevel(ICluster c) {
		return clusterLevels.get(c);
	}

	public int getClusterOffset(int vertexId, int lvl) {
		return vertexIdToClusterOffset[lvl].get(vertexId);
	}

	public ICluster getCluster(int vertexId, int lvl) {
		return clustering[lvl].getCluster(vertexId);
	}

	public LinkedList<LevelVertex> getClusterVertices(ICluster c) {
		LinkedList<LevelVertex> list = new LinkedList<LevelVertex>();
		int lvl = clusterLevels.get(c);
		for (int v : c.getVertices()) {
			list.add(graphLevel[lvl].getVertex(v));
		}
		return list;
	}

	public LinkedList<LevelEdge> getClusterEdges(ICluster c) {
		LinkedList<LevelEdge> list = new LinkedList<LevelEdge>();
		int lvl = clusterLevels.get(c);
		for (int v : c.getVertices()) {
			for (LevelEdge e : graphLevel[lvl].getVertex(v).getOutboundEdges()) {
				list.add(e);
			}
		}
		return list;
	}

	public int getClusterMaxEdgeWeight(ICluster c) {
		int max = 0;
		for (LevelEdge e : getClusterEdges(c)) {
			max = Math.max(max, e.getWeight());
		}
		return max;
	}

	public BoundingBox getClusterBoundingBox(ICluster c) {
		int minLon = Integer.MAX_VALUE;
		int minLat = Integer.MAX_VALUE;
		int maxLon = Integer.MIN_VALUE;
		int maxLat = Integer.MIN_VALUE;

		for (LevelVertex v : getClusterVertices(c)) {
			int lon = v.getCoordinate().getLongitudeInt();
			int lat = v.getCoordinate().getLatitudeInt();
			minLon = Math.min(minLon, lon);
			minLat = Math.min(minLat, lat);
			maxLon = Math.max(maxLon, lon);
			maxLat = Math.max(maxLat, lat);
		}
		return new BoundingBox(minLon, minLat, maxLon, maxLat);
	}

	public LinkedList<ICluster> getAdjacentClusters(ICluster c) {
		int lvl = clusterLevels.get(c);
		THashSet<ICluster> set = new THashSet<ICluster>();
		for (LevelEdge e : getClusterEdges(c)) {
			int tgt = e.getTarget().getId();
			ICluster c_ = clustering[lvl].getCluster(tgt);
			if (c_ != null && !c_.equals(c)) {
				set.add(c_);
			}
		}
		LinkedList<ICluster> list = new LinkedList<ICluster>();
		list.addAll(set);
		return list;
	}

	public LinkedList<ICluster> getSubjacentClusters(ICluster c) {
		int lvl = clusterLevels.get(c);
		if (lvl == 0) {
			return new LinkedList<ICluster>();
		}
		THashSet<ICluster> set = new THashSet<ICluster>();
		for (int v : c.getVertices()) {
			ICluster c_ = clustering[lvl - 1].getCluster(v);
			if (c_ != null && !c_.equals(c)) {
				set.add(c_);
			}
		}
		LinkedList<ICluster> list = new LinkedList<ICluster>();
		list.addAll(set);
		return list;
	}

	public LinkedList<ICluster> getOverlyingClusters(ICluster c) {
		int lvl = clusterLevels.get(c);
		if (lvl == graphLevel.length - 1) {
			return new LinkedList<ICluster>();
		}
		THashSet<ICluster> set = new THashSet<ICluster>();
		for (int v : c.getVertices()) {
			ICluster c_ = clustering[lvl + 1].getCluster(v);
			if (c_ != null && !c_.equals(c)) {
				set.add(c_);
			}
		}
		LinkedList<ICluster> list = new LinkedList<ICluster>();
		list.addAll(set);
		return list;
	}

	public LinkedList<ICluster> getLevelZeroClusters(ICluster c) {
		int lvl = clusterLevels.get(c);
		LinkedList<ICluster> list = new LinkedList<ICluster>();
		if (lvl == 0) {
			list.add(c);
			return list;
		}
		THashSet<ICluster> set = new THashSet<ICluster>();
		for (int v : c.getVertices()) {
			ICluster c_ = clustering[0].getCluster(v);
			if (c_ != null && !c_.equals(c)) {
				set.add(c_);
			}
		}
		list.addAll(set);
		return list;
	}

	public LinkedList<LevelVertex> getExternalReferencedVertices(ICluster c) {
		int lvl = clusterLevels.get(c);
		LinkedList<LevelVertex> list = new LinkedList<LevelVertex>();
		for (LevelEdge e : getClusterEdges(c)) {
			LevelVertex target = e.getTarget();
			ICluster c_ = clustering[lvl].getCluster(target.getId());
			if (c_ != null && !c.equals(c_)) {
				list.add(target);
			}
		}
		return list;
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

	public class BoundingBox {

		public final int minLon, minLat, maxLon, maxLat;

		public BoundingBox(int minLon, int minLat, int maxLon, int maxLat) {
			this.minLon = minLon;
			this.minLat = minLat;
			this.maxLon = maxLon;
			this.maxLat = maxLat;
		}

		public String toString() {
			return "(" + minLon + ", " + minLat + ") (" + maxLon + " " + maxLat + ")";
		}
	}

}
