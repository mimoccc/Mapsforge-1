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
package org.mapsforge.preprocessing.routing.hhmobile.binaryFile.graph;

import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.LinkedList;

import org.mapsforge.preprocessing.routing.hhmobile.clustering.Cluster;
import org.mapsforge.preprocessing.routing.hhmobile.clustering.ClusteringUtil;
import org.mapsforge.preprocessing.routing.hhmobile.graph.LevelGraph.Level.LevelEdge;
import org.mapsforge.preprocessing.routing.hhmobile.graph.LevelGraph.Level.LevelVertex;
import org.mapsforge.preprocessing.routing.hhmobile.util.BoundingBox;
import org.mapsforge.preprocessing.routing.hhmobile.util.Utils;
import org.mapsforge.preprocessing.routing.highwayHierarchies.HHComputation;

class Block {

	// -------- HEADER --------

	public final int lvl;

	public final int numVerticesWithNeighborhood;
	public final int numVerticesHavingHigherLevel;

	public final int numVertices;
	public final int numEdgesInt;
	public final int numEdgesExt;

	public final int numBlocksAdj;
	public final int numBlocksSubj;
	public final int numBlocksOverly;
	public final int numBlocksLvlZero;

	public final int minLon, minLat;
	public final byte bpLon, bpLat, bpOffsEdgeInt, bpOffsEdgeExt, bpOffsBlockAdj,
			bpOffsBlockSubj, bpOffsBlockOverly, bpOffsBlockLvlZero, bpEdgeWeight;

	// -------- BLOCK ID'S OF ALL REFERENCED BLOCKS --------

	public final int[] blockAdj;
	public final int[] blockSubj; // used only if level > 1
	public final int[] blockOverly; // used only if level < maxLevel
	public final int[] blockLvlZero; // used only if level > 0

	// -------- VERTICES --------

	// vertices must be sorted by (level descending, neighborhood ascending)!!!

	// reference same vertex in different level e.g.:
	// (blockId, vertexOffset) = (blockSubj[vOffsBlockSubj[v]], vOffsVertexSubj[v])
	public final int[] vOffsBlockSubj; // used only if level > 1
	public final int[] vOffsVertexSubj; // used only if level > 1

	// length = numVerticesHavingHigherLevel
	public final int[] vOffsBlockOverly; // used only if level < maxLevel
	public final int[] vOffsVertexOverly; // used only if level < maxLevel

	public final int[] vOffsBlockLvlZero; // used only if level > 0
	public final int[] vOffsVertexLvlZero; // used only if level > 0

	// length = number of vertices having neighborhood
	public final int[] vNeighborhood;

	public final int[] vLongitude; // used only if level == 0
	public final int[] vLatitude; // used only if level == 0

	// points to first outgoing edge
	public final int[] vOffsIntEdge;
	public final int[] vOffsExtEdge;

	// -------- INTERNAL EDGES --------

	// target id
	public final int[] eIntTargetOffsVertex;

	// edge attributes
	public final int[] eIntWeight;
	public final boolean[] eIntIsShortcut;
	public final boolean[] eIntIsForward;
	public final boolean[] eIntIsBackward;
	public final boolean[] eIntIsCore;

	// -------- EXTERNAL EDGES --------

	// target id's
	public final int[] eExtTargetOffsBlockAdj;
	public final int[] eExtTargetOffsVertexAdj;
	public final int[] eExtTargetOffsBlockLvlZero; // only used if level > 0
	public final int[] eExtTargetOffsVertexLvlZero; // only used if level > 0

	// edge attributes
	public final int[] eExtWeight;
	public final boolean[] eExtIsShortcut;
	public final boolean[] eExtIsForward;
	public final boolean[] eExtIsBackward;
	public final boolean[] eExtIsCore;

	public Block(Cluster cluster, ClusterBlockMapping mapping, ClusteringUtil cUtil) {

		LinkedList<LevelVertex> vertices = cUtil.getClusterVertices(cluster);

		this.lvl = cUtil.getClusterLevel(cluster);

		this.numVerticesWithNeighborhood = countVerticesHavingNeighborhood(vertices);
		this.numVerticesHavingHigherLevel = countVerticesHavingHigherLevel(vertices, this.lvl);

		// -------- BLOCK ID'S OF ALL REFERENCED BLOCKS --------

		TObjectIntHashMap<Cluster> clusterToBlockOffsAdj = new TObjectIntHashMap<Cluster>();
		TObjectIntHashMap<Cluster> clusterToBlockOffsSubj = new TObjectIntHashMap<Cluster>();
		TObjectIntHashMap<Cluster> clusterToBlockOffsOverly = new TObjectIntHashMap<Cluster>();
		TObjectIntHashMap<Cluster> clusterToBlockOffsLvlZero = new TObjectIntHashMap<Cluster>();

		{
			// adjacent
			LinkedList<Cluster> clusterAdj = cUtil.getAdjacentClusters(cluster);
			this.blockAdj = mapping.getBlockIds(clusterAdj);
			clusterToBlockOffsAdj = mapClustersToListOffset(clusterAdj);

			// subjacent
			if (this.lvl > 1) {
				LinkedList<Cluster> clusterSubj = cUtil.getSubjacentClusters(cluster);
				this.blockSubj = mapping.getBlockIds(clusterSubj);
				clusterToBlockOffsSubj = mapClustersToListOffset(clusterSubj);
			} else {
				this.blockSubj = new int[0];
			}

			// overly
			if (this.lvl < cUtil.getGlobalNumLevels() - 1) {
				LinkedList<Cluster> clusterOverly = cUtil.getOverlyingClusters(cluster);
				this.blockOverly = mapping.getBlockIds(clusterOverly);
				clusterToBlockOffsOverly = mapClustersToListOffset(clusterOverly);
			} else {
				this.blockOverly = new int[0];
			}

			// level zero
			if (this.lvl > 0) {
				LinkedList<Cluster> clusterLvlZero = cUtil
						.getLevelZeroClustersForExternalEdgeTargets(cluster);
				clusterLvlZero.addAll(cUtil.getLevelZeroClustersForVertices(cluster));
				Utils.removeDuplicates(clusterLvlZero);
				this.blockLvlZero = mapping.getBlockIds(clusterLvlZero);
				clusterToBlockOffsLvlZero = mapClustersToListOffset(clusterLvlZero);
			} else {
				this.blockLvlZero = new int[0];
			}
		}

		// -------- VERTICES --------

		{
			if (this.lvl > 1) {
				this.vOffsBlockSubj = new int[vertices.size()];
				this.vOffsVertexSubj = new int[vertices.size()];
			} else {
				this.vOffsBlockSubj = new int[0];
				this.vOffsVertexSubj = new int[0];
			}

			this.vOffsBlockOverly = new int[numVerticesHavingHigherLevel];
			this.vOffsVertexOverly = new int[numVerticesHavingHigherLevel];

			if (this.lvl > 0) {
				this.vOffsBlockLvlZero = new int[vertices.size()];
				this.vOffsVertexLvlZero = new int[vertices.size()];
			} else {
				this.vOffsBlockLvlZero = new int[0];
				this.vOffsVertexLvlZero = new int[0];
			}

			this.vNeighborhood = new int[this.numVerticesWithNeighborhood];

			if (this.lvl == 0) {
				this.vLongitude = new int[vertices.size()];
				this.vLatitude = new int[vertices.size()];
			} else {
				this.vLongitude = new int[0];
				this.vLatitude = new int[0];
			}

			int i = 0;
			for (LevelVertex v : cUtil.getClusterVertices(cluster)) {
				if (lvl > 1) {
					vOffsBlockSubj[i] = clusterToBlockOffsSubj.get(cUtil.getCluster(v.getId(),
							lvl - 1));
					vOffsVertexSubj[i] = cUtil.getClusterVertexOffset(v.getId(), lvl - 1);
				}
				if (i < numVerticesHavingHigherLevel) {
					vOffsBlockOverly[i] = clusterToBlockOffsOverly.get(cUtil.getCluster(
							v.getId(), lvl + 1));
					vOffsVertexOverly[i] = cUtil.getClusterVertexOffset(v.getId(), lvl + 1);
				}
				if (this.lvl > 0) {
					vOffsBlockLvlZero[i] = clusterToBlockOffsLvlZero.get(cUtil.getCluster(
							v.getId(), 0));
					vOffsVertexLvlZero[i] = cUtil.getClusterVertexOffset(v.getId(), 0);
				}

				if (i < numVerticesWithNeighborhood) {
					vNeighborhood[i] = v.getNeighborhood();
				}
				if (lvl == 0) {
					vLongitude[i] = v.getCoordinate().getLongitudeInt();
					vLatitude[i] = v.getCoordinate().getLatitudeInt();
				}

				i++;
			}
			this.vOffsIntEdge = getInternalEdgeOffsets(cluster, cUtil);
			this.vOffsExtEdge = getExternalEdgeOffsets(cluster, cUtil);
		}

		// -------- INTERNAL EDGES --------

		{
			int numIntEdges = vOffsIntEdge[vOffsIntEdge.length - 1];
			this.eIntTargetOffsVertex = new int[numIntEdges];
			this.eIntWeight = new int[numIntEdges];
			this.eIntIsShortcut = new boolean[numIntEdges];
			this.eIntIsForward = new boolean[numIntEdges];
			this.eIntIsBackward = new boolean[numIntEdges];
			this.eIntIsCore = new boolean[numIntEdges];

			int i = 0;
			// get edges ordered by source vertices
			for (LevelEdge e : cUtil.getClusterInternalEdges(cluster)) {
				eIntTargetOffsVertex[i] = cUtil.getClusterVertexOffset(e.getTarget().getId(),
						this.lvl);
				eIntWeight[i] = e.getWeight();
				eIntIsShortcut[i] = e.isShortcut();
				eIntIsForward[i] = e.isForward();
				eIntIsBackward[i] = e.isBackward();
				eIntIsCore[i] = e.getSource().getNeighborhood() != HHComputation.INFINITY_1
						&& e.getSource().getNeighborhood() != HHComputation.INFINITY_2
						&& e.getTarget().getNeighborhood() != HHComputation.INFINITY_1
						&& e.getTarget().getNeighborhood() != HHComputation.INFINITY_2;
				i++;
			}
		}

		// -------- EXTERNAL EDGES --------

		{
			int numExtEdges = vOffsExtEdge[vOffsExtEdge.length - 1];
			this.eExtTargetOffsBlockAdj = new int[numExtEdges];
			this.eExtTargetOffsVertexAdj = new int[numExtEdges];
			if (this.lvl > 0) {
				this.eExtTargetOffsBlockLvlZero = new int[numExtEdges];
				this.eExtTargetOffsVertexLvlZero = new int[numExtEdges];
			} else {
				this.eExtTargetOffsBlockLvlZero = new int[0];
				this.eExtTargetOffsVertexLvlZero = new int[0];
			}
			this.eExtWeight = new int[numExtEdges];
			this.eExtIsShortcut = new boolean[numExtEdges];
			this.eExtIsForward = new boolean[numExtEdges];
			this.eExtIsBackward = new boolean[numExtEdges];
			this.eExtIsCore = new boolean[numExtEdges];

			int i = 0;
			// get edges ordered by source vertices
			for (LevelEdge e : cUtil.getClusterExternalEdges(cluster)) {
				eExtTargetOffsBlockAdj[i] = clusterToBlockOffsAdj.get(cUtil.getCluster(e
						.getTarget().getId(), this.lvl));
				eExtTargetOffsVertexAdj[i] = cUtil.getClusterVertexOffset(
						e.getTarget().getId(), this.lvl);
				if (this.lvl > 0) {
					eExtTargetOffsBlockLvlZero[i] = clusterToBlockOffsLvlZero.get(cUtil
							.getCluster(e.getTarget().getId(), 0));
					eExtTargetOffsVertexLvlZero[i] = cUtil.getClusterVertexOffset(e.getTarget()
							.getId(), 0);
				}
				eExtWeight[i] = e.getWeight();
				eExtIsShortcut[i] = e.isShortcut();
				eExtIsForward[i] = e.isForward();
				eExtIsBackward[i] = e.isBackward();
				eExtIsCore[i] = e.getSource().getNeighborhood() != HHComputation.INFINITY_1
						&& e.getSource().getNeighborhood() != HHComputation.INFINITY_2
						&& e.getTarget().getNeighborhood() != HHComputation.INFINITY_1
						&& e.getTarget().getNeighborhood() != HHComputation.INFINITY_2;
				i++;
			}
		}

		// ------- MISSING PARTS OF THE HEADER --------

		{
			BoundingBox bbox = cUtil.getClusterBoundingBox(cluster);

			this.numVertices = vertices.size();
			this.numEdgesInt = eIntWeight.length;
			this.numEdgesExt = eExtWeight.length;

			this.numBlocksAdj = blockAdj.length;
			this.numBlocksSubj = blockSubj.length;
			this.numBlocksOverly = blockOverly.length;
			this.numBlocksLvlZero = blockLvlZero.length;

			this.minLon = bbox.minLon;
			this.minLat = bbox.minLat;

			this.bpLon = Utils.numBitsToEncode(minLon, bbox.maxLon);
			this.bpLat = Utils.numBitsToEncode(minLat, bbox.maxLat);
			this.bpOffsEdgeInt = Utils.numBitsToEncode(0, eIntWeight.length);
			this.bpOffsEdgeExt = Utils.numBitsToEncode(0, eExtWeight.length);
			this.bpOffsBlockAdj = Utils.numBitsToEncode(0, blockAdj.length);
			this.bpOffsBlockSubj = Utils.numBitsToEncode(0, blockSubj.length);
			this.bpOffsBlockOverly = Utils.numBitsToEncode(0, blockOverly.length);
			this.bpOffsBlockLvlZero = Utils.numBitsToEncode(0, blockLvlZero.length);
			this.bpEdgeWeight = Utils
					.numBitsToEncode(0, cUtil.getClusterMaxEdgeWeight(cluster));
		}

	}

	public static int getNumExternalEdges(LevelVertex v, ClusteringUtil cUtil) {
		int count = 0;
		int src = v.getId();
		int lvl = v.getLevel();
		for (LevelEdge e : v.getOutboundEdges()) {
			int dst = e.getTarget().getId();
			Cluster c = cUtil.getCluster(src, lvl);
			Cluster c_ = cUtil.getCluster(dst, lvl);
			if (!c.equals(c_)) {
				count++;
			}
		}
		return count;
	}

	public static int[] getInternalEdgeOffsets(Cluster c, ClusteringUtil cUtil) {
		LinkedList<LevelVertex> list = cUtil.getClusterVertices(c);
		int[] offsets = new int[list.size() + 1];
		offsets[0] = 0;
		int i = 1;
		for (LevelVertex v : list) {
			offsets[i] = offsets[i - 1]
					+ (v.getOutboundEdges().length - getNumExternalEdges(v, cUtil));

			i++;
		}
		return offsets;
	}

	public static int[] getExternalEdgeOffsets(Cluster c, ClusteringUtil cUtil) {
		LinkedList<LevelVertex> list = cUtil.getClusterVertices(c);
		int[] offsets = new int[list.size() + 1];
		offsets[0] = 0;
		int i = 1;
		for (LevelVertex v : list) {
			offsets[i] = offsets[i - 1] + getNumExternalEdges(v, cUtil);
			i++;
		}
		return offsets;
	}

	public static TObjectIntHashMap<Cluster> mapClustersToListOffset(
			LinkedList<Cluster> clusters) {
		int i = 0;
		TObjectIntHashMap<Cluster> map = new TObjectIntHashMap<Cluster>();
		for (Cluster c : clusters) {
			map.put(c, i++);
		}
		return map;
	}

	public static int countVerticesHavingNeighborhood(LinkedList<LevelVertex> vertices) {
		int count = 0;
		for (LevelVertex v : vertices) {
			int nh = v.getNeighborhood();
			if (nh != HHComputation.INFINITY_1 && nh != HHComputation.INFINITY_2) {
				count++;
			}
		}
		return count;
	}

	public static int countVerticesHavingHigherLevel(LinkedList<LevelVertex> vertices, int lvl) {
		int count = 0;
		for (LevelVertex v : vertices) {
			if (v.getMaxLevel() > lvl) {
				count++;
			}
		}
		return count;
	}
}
