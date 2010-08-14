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

import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.hash.THashSet;

import java.util.LinkedList;

import org.mapsforge.core.Rect;
import org.mapsforge.preprocessing.routing.hhmobile.LevelGraph.Level;
import org.mapsforge.preprocessing.routing.hhmobile.LevelGraph.Level.LevelEdge;
import org.mapsforge.preprocessing.routing.hhmobile.LevelGraph.Level.LevelVertex;
import org.mapsforge.preprocessing.routing.hhmobile.util.Utils;
import org.mapsforge.preprocessing.routing.highwayHierarchies.HHComputation;

public class ClusteringUtil {

	private final Clustering[] clustering;
	private final Level[] graphLevel;
	private final TObjectIntHashMap<Cluster> clusterLevels;
	private final TIntIntHashMap[] vertexIdToClusterOffset;
	private final int[] lvlMaxVerticesPerCluster, lvlMaxEdgesPerCluster, lvlMaxNeighborhood;

	public ClusteringUtil(Clustering[] clustering, LevelGraph levelGraph) {
		this.clustering = clustering;
		this.graphLevel = levelGraph.getLevels();
		this.clusterLevels = new TObjectIntHashMap<Cluster>();
		this.vertexIdToClusterOffset = new TIntIntHashMap[clustering.length];

		for (int lvl = 0; lvl < clustering.length; lvl++) {
			vertexIdToClusterOffset[lvl] = new TIntIntHashMap(graphLevel[lvl].numVertices());
			for (Cluster c : clustering[lvl].getClusters()) {
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
			for (Cluster c : clustering[lvl].getClusters()) {
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

	public int getGlobalMaxVerticesPerCluster() {
		return Utils.max(lvlMaxVerticesPerCluster);
	}

	public int getGlobalMaxEdgesPerCluster() {
		return Utils.max(lvlMaxEdgesPerCluster);
	}

	public int getGlobalMaxNeighborhood() {
		return Utils.max(lvlMaxNeighborhood);
	}

	public int getGlobalNumClusters() {
		return clusterLevels.size();
	}

	public int getGlobalNumLevels() {
		return clustering.length;
	}

	public int getLevelMaxVerticesPerCluster(int lvl) {
		return lvlMaxVerticesPerCluster[lvl];
	}

	public int getLevelMaxEdgesPerCluster(int lvl) {
		return lvlMaxEdgesPerCluster[lvl];
	}

	public int getLevelMaxNeighborhood(int lvl) {
		return lvlMaxNeighborhood[lvl];
	}

	public int getLevelNumClusters(int lvl) {
		return clustering[lvl].size();
	}

	public int getClusterLevel(Cluster c) {
		return clusterLevels.get(c);
	}

	public int getClusterVertexOffset(int vertexId, int lvl) {
		return vertexIdToClusterOffset[lvl].get(vertexId);
	}

	public Cluster getCluster(int vertexId, int lvl) {
		if (lvl < clustering.length) {
			return clustering[lvl].getCluster(vertexId);
		}
		return null;
	}

	public LinkedList<LevelVertex> getClusterVertices(Cluster c) {
		LinkedList<LevelVertex> list = new LinkedList<LevelVertex>();
		int lvl = clusterLevels.get(c);
		for (int v : c.getVertices()) {
			list.add(graphLevel[lvl].getVertex(v));
		}
		return list;
	}

	public LinkedList<LevelEdge> getClusterEdges(Cluster c) {
		LinkedList<LevelEdge> list = new LinkedList<LevelEdge>();
		int lvl = clusterLevels.get(c);
		for (int v : c.getVertices()) {
			for (LevelEdge e : graphLevel[lvl].getVertex(v).getOutboundEdges()) {
				list.add(e);
			}
		}
		return list;
	}

	public LinkedList<LevelEdge> getClusterInternalEdges(Cluster c) {
		LinkedList<LevelEdge> list = new LinkedList<LevelEdge>();
		int lvl = clusterLevels.get(c);
		for (int v : c.getVertices()) {
			for (LevelEdge e : graphLevel[lvl].getVertex(v).getOutboundEdges()) {
				Cluster c_ = clustering[lvl].getCluster(e.getTarget().getId());
				if (c_.equals(c)) {
					list.add(e);
				}
			}
		}
		return list;
	}

	public LinkedList<LevelEdge> getClusterExternalEdges(Cluster c) {
		LinkedList<LevelEdge> list = new LinkedList<LevelEdge>();
		int lvl = clusterLevels.get(c);
		for (int v : c.getVertices()) {
			for (LevelEdge e : graphLevel[lvl].getVertex(v).getOutboundEdges()) {
				Cluster c_ = clustering[lvl].getCluster(e.getTarget().getId());
				if (!c_.equals(c)) {
					list.add(e);
				}
			}
		}
		return list;
	}

	public int getClusterMaxEdgeWeight(Cluster c) {
		int max = 0;
		for (LevelEdge e : getClusterEdges(c)) {
			max = Math.max(max, e.getWeight());
		}
		return max;
	}

	public Rect getClusterBoundingBox(Cluster c) {
		int minLon = Integer.MAX_VALUE;
		int minLat = Integer.MAX_VALUE;
		int maxLon = Integer.MIN_VALUE;
		int maxLat = Integer.MIN_VALUE;

		for (LevelVertex v : getClusterVertices(c)) {
			int lon = v.getCoordinate().getLongitudeE6();
			int lat = v.getCoordinate().getLatitudeE6();
			minLon = Math.min(minLon, lon);
			minLat = Math.min(minLat, lat);
			maxLon = Math.max(maxLon, lon);
			maxLat = Math.max(maxLat, lat);
		}
		return new Rect(minLon, minLat, maxLon, maxLat);
	}

	public LinkedList<Cluster> getAdjacentClusters(Cluster c) {
		int lvl = clusterLevels.get(c);
		THashSet<Cluster> set = new THashSet<Cluster>();
		for (LevelEdge e : getClusterEdges(c)) {
			int tgt = e.getTarget().getId();
			Cluster c_ = clustering[lvl].getCluster(tgt);
			if (c_ != null && !c_.equals(c)) {
				set.add(c_);
			}
		}
		LinkedList<Cluster> list = new LinkedList<Cluster>();
		list.addAll(set);
		return list;
	}

	public LinkedList<Cluster> getSubjacentClusters(Cluster c) {
		int lvl = clusterLevels.get(c);
		if (lvl == 0) {
			return new LinkedList<Cluster>();
		}
		THashSet<Cluster> set = new THashSet<Cluster>();
		for (int v : c.getVertices()) {
			Cluster c_ = clustering[lvl - 1].getCluster(v);
			if (c_ != null && !c_.equals(c)) {
				set.add(c_);
			}
		}
		LinkedList<Cluster> list = new LinkedList<Cluster>();
		list.addAll(set);
		return list;
	}

	public LinkedList<Cluster> getOverlyingClusters(Cluster c) {
		int lvl = clusterLevels.get(c);
		if (lvl == graphLevel.length - 1) {
			return new LinkedList<Cluster>();
		}
		THashSet<Cluster> set = new THashSet<Cluster>();
		for (int v : c.getVertices()) {
			Cluster c_ = clustering[lvl + 1].getCluster(v);
			if (c_ != null && !c_.equals(c)) {
				set.add(c_);
			}
		}
		LinkedList<Cluster> list = new LinkedList<Cluster>();
		list.addAll(set);
		return list;
	}

	public LinkedList<Cluster> getLevelZeroClustersForVertices(Cluster c) {
		int lvl = clusterLevels.get(c);
		LinkedList<Cluster> list = new LinkedList<Cluster>();
		if (lvl == 0) {
			list.add(c);
			return list;
		}
		THashSet<Cluster> set = new THashSet<Cluster>();
		for (int v : c.getVertices()) {
			Cluster c_ = clustering[0].getCluster(v);
			if (c_ != null && !c_.equals(c)) {
				set.add(c_);
			}
		}
		list.addAll(set);
		return list;
	}

	public LinkedList<Cluster> getLevelZeroClustersForExternalEdgeTargets(Cluster c) {
		int lvl = clusterLevels.get(c);
		LinkedList<Cluster> list = new LinkedList<Cluster>();
		if (lvl == 0) {
			list.add(c);
			return list;
		}
		THashSet<Cluster> set = new THashSet<Cluster>();
		for (int v : c.getVertices()) {
			for (LevelEdge e : graphLevel[lvl].getVertex(v).getOutboundEdges()) {
				Cluster c_ = clustering[0].getCluster(e.getTarget().getId());
				if (c_ != null && !c_.equals(c)) {
					set.add(c_);
				}
			}
		}
		list.addAll(set);
		return list;
	}

	public LinkedList<LevelVertex> getClusterExternalReferencedVertices(Cluster c) {
		int lvl = clusterLevels.get(c);
		LinkedList<LevelVertex> list = new LinkedList<LevelVertex>();
		for (LevelEdge e : getClusterEdges(c)) {
			LevelVertex target = e.getTarget();
			Cluster c_ = clustering[lvl].getCluster(target.getId());
			if (c_ != null && !c.equals(c_)) {
				list.add(target);
			}
		}
		return list;
	}

	public static int getGlobalNumClusters(Clustering[] clustering) {
		int sum = 0;
		for (int i = 0; i < clustering.length; i++) {
			sum += clustering[i].size();
		}
		return sum;
	}

}
