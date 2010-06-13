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

import org.mapsforge.preprocessing.routing.hhmobile.graph.LevelGraph;
import org.mapsforge.preprocessing.routing.hhmobile.graph.LevelGraph.Level;
import org.mapsforge.preprocessing.routing.hhmobile.graph.LevelGraph.Level.LevelEdge;
import org.mapsforge.preprocessing.routing.hhmobile.graph.LevelGraph.Level.LevelVertex;
import org.mapsforge.preprocessing.routing.hhmobile.util.BoundingBox;
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

	public int getClusterLevel(ICluster c) {
		return clusterLevels.get(c);
	}

	public int getClusterVertexOffset(int vertexId, int lvl) {
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

	public LinkedList<LevelVertex> getClusterExternalReferencedVertices(ICluster c) {
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

	public static int getGlobalNumClusters(IClustering[] clustering) {
		int sum = 0;
		for (int i = 0; i < clustering.length; i++) {
			sum += clustering[i].size();
		}
		return sum;
	}

}
