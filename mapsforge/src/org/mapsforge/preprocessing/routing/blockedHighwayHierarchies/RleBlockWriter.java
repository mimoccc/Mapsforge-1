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
package org.mapsforge.preprocessing.routing.blockedHighwayHierarchies;

import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.LinkedList;

import org.apache.hadoop.util.IndexedSortable;
import org.apache.hadoop.util.QuickSort;
import org.mapsforge.core.Rect;
import org.mapsforge.preprocessing.routing.blockedHighwayHierarchies.LevelGraph.Level;
import org.mapsforge.preprocessing.routing.blockedHighwayHierarchies.LevelGraph.Level.LevelEdge;
import org.mapsforge.preprocessing.routing.blockedHighwayHierarchies.LevelGraph.Level.LevelVertex;
import org.mapsforge.preprocessing.routing.blockedHighwayHierarchies.util.BitArrayOutputStream;
import org.mapsforge.preprocessing.routing.blockedHighwayHierarchies.util.HHGlobals;
import org.mapsforge.preprocessing.routing.blockedHighwayHierarchies.util.Utils;
import org.mapsforge.preprocessing.routing.highwayHierarchies.HHComputation;

class RleBlockWriter {

	private static final byte[] HEADER_MAGIC = HHGlobals.RLE_CLUSTER_BLOCKS_HEADER_MAGIC;
	private static final int HEADER_LENGTH = HHGlobals.RLE_CLUSTER_BLOCKS_HEADER_LENGTH;

	private final static int BUFFER_SIZE = 25000000;
	private final static byte[] BUFFER = new byte[BUFFER_SIZE];

	public static int[] writeClusterBlocks(File targetFile, LevelGraph levelGraph,
			Clustering[] clustering, ClusterBlockMapping mapping, boolean includeHopIndices)
			throws IOException {

		BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(targetFile));

		ClusteringUtil cUtil = new ClusteringUtil(clustering, levelGraph);
		EncodingParams enc = computeHeader(cUtil, includeHopIndices);
		TIntIntHashMap edgeToAdjacencyListIndex = computeEdgeAdjListIndex(levelGraph,
				clustering);

		// --- WRITE HEADER FILE ---
		writeHeader(out, enc);

		// --- REARRANGE VERTICES AND BLOCK IDS ---

		sortClusterVertices(levelGraph, clustering);
		// need new instance since we swapped clusters and vertices
		cUtil = new ClusteringUtil(clustering, levelGraph);
		int[] blockSizes = getBlockByteSizes(mapping, cUtil, enc);
		sortClusters(blockSizes, mapping);

		// --- WRITE CLUSTER BLOCKS FILE ---

		int[] blockSize = new int[mapping.size()];
		for (int i = 0; i < mapping.size(); i++) {
			blockSize[i] = serializeBlock(BUFFER, mapping.getCluster(i), mapping, cUtil, enc);
			out.write(BUFFER, 0, blockSize[i]);
			Utils.setZero(BUFFER, 0, blockSize[i]);
		}

		out.close();
		return blockSize;
	}

	private static TIntIntHashMap computeEdgeAdjListIndex(LevelGraph levelGraph,
			Clustering[] clustering) {
		TIntIntHashMap map = new TIntIntHashMap();
		// the order between interal edges is kept
		// the order between external edges is kept
		// the internal edges are store first thus the order is changed
		// this must be taken into account when mapping a edge id of the input
		// graph to a hopIdx of the output graph
		for (int lvl = 0; lvl < clustering.length; lvl++) {
			Level graph = levelGraph.getLevel(lvl);
			for (Iterator<LevelVertex> iter = graph.getVertices(); iter.hasNext();) {
				LevelVertex v = iter.next();
				int hopIdx = 0;
				for (LevelEdge e : v.getOutboundEdges()) {
					if (clustering[lvl].getCluster(e.getSource().getId()).equals(
							clustering[lvl].getCluster(e.getTarget().getId()))) {
						// internal edge
						map.put(e.getId(), hopIdx++);

					}
				}
				for (LevelEdge e : v.getOutboundEdges()) {
					if (!clustering[lvl].getCluster(e.getSource().getId()).equals(
							clustering[lvl].getCluster(e.getTarget().getId()))) {
						// external edge
						map.put(e.getId(), hopIdx++);
					}
				}
			}

		}
		return map;
	}

	private static void writeHeader(OutputStream out, EncodingParams enc) throws IOException {
		ByteArrayOutputStream arrayOut = new ByteArrayOutputStream();
		DataOutputStream dataOut = new DataOutputStream(arrayOut);
		dataOut.write(HEADER_MAGIC);
		dataOut.write(enc.bpClusterId);
		dataOut.write(enc.bpVertexCount);
		dataOut.write(enc.bpEdgeCount);
		dataOut.write(enc.bpNeighborhood);
		dataOut.write(enc.numLevels);
		dataOut.writeBoolean(enc.includeHopIndices);
		byte[] header = arrayOut.toByteArray();
		dataOut.close();

		if (header.length <= HEADER_LENGTH) {
			out.write(header);
			out.write(new byte[HEADER_LENGTH - header.length]);
		} else {
			throw new RuntimeException("Need to increase header size!");
		}
	}

	private static EncodingParams computeHeader(ClusteringUtil cUtil, boolean writeHopIndices) {
		byte bitsPerClusterId = Utils.numBitsToEncode(0, cUtil.getGlobalNumClusters() - 1);
		byte bitsPerVertexOffset = Utils.numBitsToEncode(0, cUtil
				.getGlobalMaxVerticesPerCluster());
		byte bitsPerEdgeCount = Utils.numBitsToEncode(0, cUtil.getGlobalMaxEdgesPerCluster());
		byte bitsPerNeighborhood = Utils.numBitsToEncode(0, cUtil.getGlobalMaxNeighborhood());
		byte numGraphLevels = (byte) cUtil.getGlobalNumLevels();
		return new EncodingParams(bitsPerClusterId, bitsPerVertexOffset, bitsPerEdgeCount,
				bitsPerNeighborhood, numGraphLevels, writeHopIndices);
	}

	private static int[] getBlockByteSizes(ClusterBlockMapping mapping, ClusteringUtil cUtil,
			EncodingParams enc) throws IOException {
		int[] byteSize = new int[mapping.size()];
		for (int i = 0; i < byteSize.length; i++) {
			byteSize[i] = serializeBlock(BUFFER, mapping.getCluster(i), mapping, cUtil, enc);
		}
		return byteSize;
	}

	private static void sortClusters(final int[] byteSize, final ClusterBlockMapping mapping) {
		QuickSort quicksort = new QuickSort();
		quicksort.sort(new IndexedSortable() {

			@Override
			public void swap(int i, int j) {
				mapping.swapBlockIds(i, j);
				Utils.swap(byteSize, i, j);
			}

			@Override
			public int compare(int i, int j) {
				return byteSize[i] - byteSize[j];
			}
		}, 0, mapping.size());
	}

	private static void sortClusterVertices(LevelGraph levelGraph, Clustering[] clustering) {
		QuickSort quicksort = new QuickSort();
		for (int lvl = 0; lvl < clustering.length; lvl++) {
			Level graph = levelGraph.getLevel(lvl);
			for (final Cluster c : clustering[lvl].getClusters()) {
				final int[] vertexIds = c.getVertices();
				final int[] nh = new int[vertexIds.length];
				final int[] level = new int[vertexIds.length];
				for (int i = 0; i < vertexIds.length; i++) {
					nh[i] = graph.getVertex(vertexIds[i]).getNeighborhood();
					level[i] = graph.getVertex(vertexIds[i]).getMaxLevel();
				}
				quicksort.sort(new IndexedSortable() {

					@Override
					public void swap(int i, int j) {
						Utils.swap(level, i, j);
						Utils.swap(nh, i, j);
						Utils.swap(vertexIds, i, j);
						c.swapVertices(i, j);
					}

					@Override
					public int compare(int i, int j) {
						if (level[i] != level[j]) {
							return level[j] - level[i];
						}
						return nh[i] - nh[j];
					}
				}, 0, c.size());
			}
		}
	}

	private static int serializeBlock(byte[] buff, Cluster cluster,
			ClusterBlockMapping mapping, ClusteringUtil cUtil, EncodingParams enc)
			throws IOException {

		Block b = new Block(cluster, mapping, cUtil, enc.includeHopIndices);
		BitArrayOutputStream stream = new BitArrayOutputStream(buff);

		// --------HEADER --------

		stream.writeByte((byte) b.lvl);

		stream.writeUInt(b.numVerticesWithNeighborhood, enc.bpVertexCount);
		stream.writeUInt(b.numVerticesHavingHigherLevel, enc.bpVertexCount);

		stream.writeUInt(b.numVertices, enc.bpVertexCount);
		stream.writeUInt(b.numEdgesInt, enc.bpEdgeCount);
		stream.writeUInt(b.numEdgesExt, enc.bpEdgeCount);

		stream.writeUInt(b.numBlocksAdj, enc.bpClusterId);
		stream.writeUInt(b.numBlocksSubj, enc.bpClusterId);
		stream.writeUInt(b.numBlocksOverly, enc.bpClusterId);
		stream.writeUInt(b.numBlocksLvlZero, enc.bpClusterId);

		stream.writeInt(b.minLon);
		stream.writeInt(b.minLat);

		stream.writeUInt(b.bpLon, 5);
		stream.writeUInt(b.bpLat, 5);
		stream.writeUInt(b.bpOffsEdgeInt, 5);
		stream.writeUInt(b.bpOffsEdgeExt, 5);
		stream.writeUInt(b.bpOffsBlockAdj, 5);
		stream.writeUInt(b.bpOffsBlockSubj, 5);
		stream.writeUInt(b.bpOffsBlockOverly, 5);
		stream.writeUInt(b.bpOffsBlockLvlZero, 5);
		stream.writeUInt(b.bpEdgeWeight, 5);

		stream.alignPointer(1);

		// -------- BLOCK ID'S OF ALL REFERENCED BLOCKS --------
		for (int val : b.blockAdj) {
			stream.writeUInt(val, enc.bpClusterId);
		}
		stream.alignPointer(1);
		for (int val : b.blockSubj) {
			stream.writeUInt(val, enc.bpClusterId);
		}
		stream.alignPointer(1);
		for (int val : b.blockOverly) {
			stream.writeUInt(val, enc.bpClusterId);
		}
		stream.alignPointer(1);
		for (int val : b.blockLvlZero) {
			stream.writeUInt(val, enc.bpClusterId);
		}
		stream.alignPointer(1);

		// -------- VERTICES --------

		// reference to subjacent vertex
		for (int val : b.vOffsBlockSubj) {
			stream.writeUInt(val, b.bpOffsBlockSubj);
		}
		stream.alignPointer(1);
		for (int val : b.vOffsVertexSubj) {
			stream.writeUInt(val, enc.bpVertexCount);
		}
		stream.alignPointer(1);
		// reference to overlying vertex
		for (int val : b.vOffsBlockOverly) {
			stream.writeUInt(val, b.bpOffsBlockOverly);
		}
		stream.alignPointer(1);
		for (int val : b.vOffsVertexOverly) {
			stream.writeUInt(val, enc.bpVertexCount);
		}
		stream.alignPointer(1);
		// reference to level zero vertex
		for (int val : b.vOffsBlockLvlZero) {
			stream.writeUInt(val, b.bpOffsBlockLvlZero);
		}
		stream.alignPointer(1);
		for (int val : b.vOffsVertexLvlZero) {
			stream.writeUInt(val, enc.bpVertexCount);
		}
		stream.alignPointer(1);
		// neighborhood
		for (int val : b.vNeighborhood) {
			stream.writeUInt(val, enc.bpNeighborhood);
		}
		stream.alignPointer(1);
		// coordinate
		for (int val : b.vLongitude) {
			stream.writeUInt(val - b.minLon, b.bpLon);
		}
		stream.alignPointer(1);
		for (int val : b.vLatitude) {
			stream.writeUInt(val - b.minLat, b.bpLat);
		}
		stream.alignPointer(1);
		for (int val : b.vOffsIntEdge) {
			stream.writeUInt(val, b.bpOffsEdgeInt);
		}
		stream.alignPointer(1);
		// first outgoing edge offset
		for (int val : b.vOffsExtEdge) {
			stream.writeUInt(val, b.bpOffsEdgeExt);
		}
		stream.alignPointer(1);

		// -------- INTERNAL EDGES --------

		// target vertex offset
		for (int val : b.eIntTargetOffsVertex) {
			stream.writeUInt(val, enc.bpVertexCount);
		}
		stream.alignPointer(1);
		// weight
		for (int val : b.eIntWeight) {
			stream.writeUInt(val, b.bpEdgeWeight);
		}
		stream.alignPointer(1);
		// downward link
		for (int i = 0; i < b.eIntSourceDownwardLinkBlockId.length; i++) {
			stream.writeUInt((b.eIntSourceDownwardLinkBlockId[i] << enc.bpVertexCount)
					| b.eIntSourceDownwardLinkVertexOffset[i], 32);
		}
		stream.alignPointer(1);
		// is shortcut
		for (boolean val : b.eIntIsShortcut) {
			stream.writeBit(val);
		}
		stream.alignPointer(1);
		// is forward
		for (boolean val : b.eIntIsForward) {
			stream.writeBit(val);
		}
		stream.alignPointer(1);
		// is backward
		for (boolean val : b.eIntIsBackward) {
			stream.writeBit(val);
		}
		stream.alignPointer(1);
		// is core
		for (boolean val : b.eIntIsCore) {
			stream.writeBit(val);
		}
		stream.alignPointer(1);

		// -------- EXTERNAL EDGES --------

		for (int val : b.eExtTargetOffsBlockAdj) {
			stream.writeUInt(val, b.bpOffsBlockAdj);
		}
		stream.alignPointer(1);

		for (int val : b.eExtTargetOffsVertexAdj) {
			stream.writeUInt(val, enc.bpVertexCount);
		}
		stream.alignPointer(1);

		for (int val : b.eExtTargetOffsBlockLvlZero) {
			stream.writeUInt(val, b.bpOffsBlockLvlZero);
		}
		stream.alignPointer(1);

		for (int val : b.eExtTargetOffsVertexLvlZero) {
			stream.writeUInt(val, enc.bpVertexCount);
		}
		stream.alignPointer(1);

		// weight
		for (int val : b.eExtWeight) {
			stream.writeUInt(val, b.bpEdgeWeight);
		}
		stream.alignPointer(1);
		// downward link
		for (int i = 0; i < b.eExtSourceDownwardLinkBlockId.length; i++) {
			stream.writeUInt((b.eExtSourceDownwardLinkBlockId[i] << enc.bpVertexCount)
					| b.eExtSourceDownwardLinkVertexOffset[i], 32);
		}
		stream.alignPointer(1);
		// is shortcut
		for (boolean val : b.eExtIsShortcut) {
			stream.writeBit(val);
		}
		stream.alignPointer(1);
		// is forward
		for (boolean val : b.eExtIsForward) {
			stream.writeBit(val);
		}
		stream.alignPointer(1);
		// is backward
		for (boolean val : b.eExtIsBackward) {
			stream.writeBit(val);
		}
		stream.alignPointer(1);
		// is core
		for (boolean val : b.eExtIsCore) {
			stream.writeBit(val);
		}
		stream.alignPointer(1);

		return stream.getByteOffset();
	}

	private static class Block {

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
		public final int[] eIntSourceDownwardLinkBlockId; // only if hop indices are written
		public final int[] eIntSourceDownwardLinkVertexOffset; // only if hop indices are
		// written

		// -------- EXTERNAL EDGES --------

		// target id's
		public final int[] eExtTargetOffsBlockAdj;
		public final int[] eExtTargetOffsVertexAdj;
		public final int[] eExtTargetOffsBlockLvlZero; // only used if level > 0
		public final int[] eExtTargetOffsVertexLvlZero; // only used if level > 0
		public final int[] eExtSourceDownwardLinkBlockId; // only if hop indices are written
		public final int[] eExtSourceDownwardLinkVertexOffset; // only if hop indices are
		// written

		// edge attributes
		public final int[] eExtWeight;
		public final boolean[] eExtIsShortcut;
		public final boolean[] eExtIsForward;
		public final boolean[] eExtIsBackward;
		public final boolean[] eExtIsCore;

		public Block(Cluster cluster, ClusterBlockMapping mapping, ClusteringUtil cUtil,
				boolean writeHopIndices) {

			LinkedList<LevelVertex> vertices = cUtil.getClusterVertices(cluster);

			this.lvl = cUtil.getClusterLevel(cluster);

			this.numVerticesWithNeighborhood = countVerticesHavingNeighborhood(vertices);
			this.numVerticesHavingHigherLevel = countVerticesHavingHigherLevel(vertices,
					this.lvl);

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
						vOffsBlockSubj[i] = clusterToBlockOffsSubj.get(cUtil.getCluster(v
								.getId(), lvl - 1));
						vOffsVertexSubj[i] = cUtil.getClusterVertexOffset(v.getId(), lvl - 1);
					}
					if (i < numVerticesHavingHigherLevel) {
						vOffsBlockOverly[i] = clusterToBlockOffsOverly.get(cUtil.getCluster(v
								.getId(), lvl + 1));
						vOffsVertexOverly[i] = cUtil.getClusterVertexOffset(v.getId(), lvl + 1);
					}
					if (this.lvl > 0) {
						vOffsBlockLvlZero[i] = clusterToBlockOffsLvlZero.get(cUtil.getCluster(v
								.getId(), 0));
						vOffsVertexLvlZero[i] = cUtil.getClusterVertexOffset(v.getId(), 0);
					}

					if (i < numVerticesWithNeighborhood) {
						vNeighborhood[i] = v.getNeighborhood();
					}
					if (lvl == 0) {
						vLongitude[i] = v.getCoordinate().getLongitudeE6();
						vLatitude[i] = v.getCoordinate().getLatitudeE6();
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
				if (writeHopIndices) {
					this.eIntSourceDownwardLinkBlockId = new int[numIntEdges];
					this.eIntSourceDownwardLinkVertexOffset = new int[numIntEdges];
				} else {
					this.eIntSourceDownwardLinkBlockId = new int[0];
					this.eIntSourceDownwardLinkVertexOffset = new int[0];
				}
				this.eIntIsShortcut = new boolean[numIntEdges];
				this.eIntIsForward = new boolean[numIntEdges];
				this.eIntIsBackward = new boolean[numIntEdges];
				this.eIntIsCore = new boolean[numIntEdges];

				int i = 0;
				// get edges ordered by source vertices
				for (LevelEdge e : cUtil.getClusterInternalEdges(cluster)) {
					eIntTargetOffsVertex[i] = cUtil.getClusterVertexOffset(e.getTarget()
							.getId(), this.lvl);
					eIntWeight[i] = e.getWeight();
					if (writeHopIndices) {
						if (e.isShortcut()) {
							int lvl = e.getMinLevel() - 1;
							Cluster c = cUtil.getCluster(e.getSource().getId(), lvl);
							eIntSourceDownwardLinkBlockId[i] = mapping.getBlockId(c);
							eIntSourceDownwardLinkVertexOffset[i] = cUtil
									.getClusterVertexOffset(e.getSource().getId(), lvl);
						} else {
							eIntSourceDownwardLinkBlockId[i] = -1;
							eIntSourceDownwardLinkVertexOffset[i] = -1;
						}
					}
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
				if (writeHopIndices) {
					this.eExtSourceDownwardLinkBlockId = new int[numExtEdges];
					this.eExtSourceDownwardLinkVertexOffset = new int[numExtEdges];
				} else {
					this.eExtSourceDownwardLinkBlockId = new int[0];
					this.eExtSourceDownwardLinkVertexOffset = new int[0];
				}
				this.eExtIsShortcut = new boolean[numExtEdges];
				this.eExtIsForward = new boolean[numExtEdges];
				this.eExtIsBackward = new boolean[numExtEdges];
				this.eExtIsCore = new boolean[numExtEdges];

				int i = 0;
				// get edges ordered by source vertices
				for (LevelEdge e : cUtil.getClusterExternalEdges(cluster)) {
					eExtTargetOffsBlockAdj[i] = clusterToBlockOffsAdj.get(cUtil.getCluster(e
							.getTarget().getId(), this.lvl));
					eExtTargetOffsVertexAdj[i] = cUtil.getClusterVertexOffset(e.getTarget()
							.getId(), this.lvl);
					if (this.lvl > 0) {
						eExtTargetOffsBlockLvlZero[i] = clusterToBlockOffsLvlZero.get(cUtil
								.getCluster(e.getTarget().getId(), 0));
						eExtTargetOffsVertexLvlZero[i] = cUtil.getClusterVertexOffset(e
								.getTarget().getId(), 0);
					}
					if (writeHopIndices) {
						if (e.isShortcut()) {
							int lvl = e.getMinLevel() - 1;
							Cluster c = cUtil.getCluster(e.getSource().getId(), lvl);
							eExtSourceDownwardLinkBlockId[i] = mapping.getBlockId(c);
							eExtSourceDownwardLinkVertexOffset[i] = cUtil
									.getClusterVertexOffset(e.getSource().getId(), lvl);
						} else {
							eExtSourceDownwardLinkBlockId[i] = -1;
							eExtSourceDownwardLinkVertexOffset[i] = -1;
						}
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
				Rect bbox = cUtil.getClusterBoundingBox(cluster);

				this.numVertices = vertices.size();
				this.numEdgesInt = eIntWeight.length;
				this.numEdgesExt = eExtWeight.length;

				this.numBlocksAdj = blockAdj.length;
				this.numBlocksSubj = blockSubj.length;
				this.numBlocksOverly = blockOverly.length;
				this.numBlocksLvlZero = blockLvlZero.length;

				this.minLon = bbox.minLongitudeE6;
				this.minLat = bbox.minLatitudeE6;

				this.bpLon = Utils.numBitsToEncode(minLon, bbox.maxLongitudeE6);
				this.bpLat = Utils.numBitsToEncode(minLat, bbox.maxLatitudeE6);
				this.bpOffsEdgeInt = Utils.numBitsToEncode(0, eIntWeight.length);
				this.bpOffsEdgeExt = Utils.numBitsToEncode(0, eExtWeight.length);
				this.bpOffsBlockAdj = Utils.numBitsToEncode(0, blockAdj.length);
				this.bpOffsBlockSubj = Utils.numBitsToEncode(0, blockSubj.length);
				this.bpOffsBlockOverly = Utils.numBitsToEncode(0, blockOverly.length);
				this.bpOffsBlockLvlZero = Utils.numBitsToEncode(0, blockLvlZero.length);
				this.bpEdgeWeight = Utils.numBitsToEncode(0, cUtil
						.getClusterMaxEdgeWeight(cluster));
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

		public static int countVerticesHavingHigherLevel(LinkedList<LevelVertex> vertices,
				int lvl) {
			int count = 0;
			for (LevelVertex v : vertices) {
				if (v.getMaxLevel() > lvl) {
					count++;
				}
			}
			return count;
		}
	}

	private static class EncodingParams {

		public final byte bpClusterId, bpVertexCount, bpEdgeCount, bpNeighborhood, numLevels;
		final boolean includeHopIndices;

		public EncodingParams(byte bpClusterId, byte bpVertexCount, byte bpEdgeCount,
				byte bpNeighborhood, byte numLevels, boolean writeHopIndices) {
			this.bpClusterId = bpClusterId;
			this.bpVertexCount = bpVertexCount;
			this.bpEdgeCount = bpEdgeCount;
			this.bpNeighborhood = bpNeighborhood;
			this.numLevels = numLevels;
			this.includeHopIndices = writeHopIndices;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(EncodingParams.class.getName() + " (\n");
			sb.append("  bpClusterId = " + bpClusterId + "\n");
			sb.append("  bpVertexCount = " + bpVertexCount + "\n");
			sb.append("  bpEdgeCount = " + bpEdgeCount + "\n");
			sb.append("  bpNeighborhood = " + bpNeighborhood + "\n");
			sb.append("  numLevels = " + numLevels + "\n");
			sb.append("  includeHopIndices = " + includeHopIndices + "\n");
			sb.append(")");
			return sb.toString();
		}
	}
}
