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
package org.mapsforge.preprocessing.routing.hhmobile.extmem.graph;

import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.LinkedList;

import org.mapsforge.preprocessing.routing.hhmobile.clustering.ClusteringUtil;
import org.mapsforge.preprocessing.routing.hhmobile.clustering.ICluster;
import org.mapsforge.preprocessing.routing.hhmobile.clustering.KCenterClustering;
import org.mapsforge.preprocessing.routing.hhmobile.clustering.ClusteringUtil.BoundingBox;
import org.mapsforge.preprocessing.routing.hhmobile.graph.LevelGraph;
import org.mapsforge.preprocessing.routing.hhmobile.graph.LevelGraph.Level.LevelEdge;
import org.mapsforge.preprocessing.routing.hhmobile.graph.LevelGraph.Level.LevelVertex;
import org.mapsforge.preprocessing.routing.hhmobile.util.BitArrayOutputStream;
import org.mapsforge.preprocessing.routing.hhmobile.util.BitSerializer;
import org.mapsforge.preprocessing.routing.hhmobile.util.Utils;
import org.mapsforge.preprocessing.routing.highwayHierarchies.HHComputation;
import org.mapsforge.preprocessing.routing.highwayHierarchies.util.Serializer;

final class Block {

	public final static int POINTER_IDX_SUBJACENT = 0;
	public final static int POINTER_IDX_OVERLYING = 1;
	public final static int POINTER_IDX_LEVEL_ZERO = 2;

	// --- HEADER ---

	public final int level; // 1 Byte
	public final int numVerticesHavingNh;
	public final int numVerticesHavingNoNh;
	public final int numEdges;
	public final int bitsPerEdgeWeight;
	public final int bitsPerLon;
	public final int bitsPerLat;
	public final int minLon;
	public final int minLat;

	// --- DATA ---

	// block-identifiers of referenced blocks :
	public final int[] adjacentBlocks; // each block pointed to by edge target, includes this
	// block!
	public final int[] subjacentBlocks;
	public final int[] overlyingBlocks;
	public final int[] levelZeroBlocks;

	// vertices :
	public final VertexEntry[] vertices;

	// edges :
	public final EdgeEntry[] edges;

	public Block(ICluster cluster, ClusterBlockMapping mapping, ClusteringUtil cUtil) {
		LinkedList<LevelVertex> vertexList = cUtil.getClusterVertices(cluster);

		LinkedList<LevelEdge> edgeList = cUtil.getClusterEdges(cluster);
		BoundingBox bbox = cUtil.getClusterBoundingBox(cluster);

		LinkedList<ICluster> adjacentClusters = cUtil.getAdjacentClusters(cluster);
		adjacentClusters.add(cluster);
		LinkedList<ICluster> subjacentClusters = cUtil.getSubjacentClusters(cluster);
		LinkedList<ICluster> overLyingClusters = cUtil.getOverlyingClusters(cluster);
		LinkedList<ICluster> levelZeroClusters = cUtil.getLevelZeroClusters(cluster);

		// --- HEADER ---
		this.level = cUtil.getClusterLevel(cluster);
		this.numVerticesHavingNh = countVerticesHavingNeighborhood(vertexList);
		this.numVerticesHavingNoNh = vertexList.size() - numVerticesHavingNh;
		this.numEdges = edgeList.size();
		this.bitsPerEdgeWeight = BitSerializer.numBitsToEncodeUInt(0, cUtil
				.getClusterMaxEdgeWeight(cluster));
		this.bitsPerLon = BitSerializer.numBitsToEncodeUInt(bbox.minLon, bbox.maxLon);
		this.bitsPerLat = BitSerializer.numBitsToEncodeUInt(bbox.minLat, bbox.maxLat);
		this.minLon = bbox.minLon;
		this.minLat = bbox.minLat;

		// --- DATA ---

		// block-identifiers of referenced blocks
		this.adjacentBlocks = mapping.getBlockIds(adjacentClusters);
		this.subjacentBlocks = mapping.getBlockIds(subjacentClusters);
		this.overlyingBlocks = mapping.getBlockIds(overLyingClusters);
		this.levelZeroBlocks = mapping.getBlockIds(levelZeroClusters);

		// vertices :
		this.vertices = new VertexEntry[vertexList.size()];

		TIntObjectHashMap<IndirectVertexPointer> subjacentPointers = new TIntObjectHashMap<IndirectVertexPointer>();
		if (level > 0) {
			subjacentPointers = getVertexPointers(subjacentClusters, vertexList, cUtil,
					level - 1);

		}

		TIntObjectHashMap<IndirectVertexPointer> overlyingPointers = new TIntObjectHashMap<IndirectVertexPointer>();
		if (level < cUtil.numLevels() - 1) {
			overlyingPointers = getVertexPointers(overLyingClusters, vertexList, cUtil,
					level + 1);
		}

		TIntObjectHashMap<IndirectVertexPointer> levelZeroPointers = getVertexPointers(
				levelZeroClusters, vertexList, cUtil, 0);

		int[] edgeOffset = getEdgeOffsets(cluster, cUtil);
		int i = 0;
		for (LevelVertex v : vertexList) {
			IndirectVertexPointer[] interLevelPointers = new IndirectVertexPointer[] {
					subjacentPointers.get(v.getId()), overlyingPointers.get(v.getId()),
					levelZeroPointers.get(v.getId()) };

			this.vertices[i] = new VertexEntry(interLevelPointers, v.getNeighborhood(),
					edgeOffset[i], v.getCoordinate().getLongitudeInt(), v.getCoordinate()
							.getLatitudeInt());
			i++;
		}

		// edges
		LinkedList<LevelVertex> edgeTargets = cUtil.getExternalReferencedVertices(cluster);
		edgeTargets.addAll(vertexList);
		TIntObjectHashMap<IndirectVertexPointer> adjacentPointers = getVertexPointers(
				adjacentClusters, edgeTargets, cUtil, level);

		this.edges = new EdgeEntry[edgeList.size()];
		i = 0;
		for (LevelEdge e : edgeList) {
			this.edges[i++] = new EdgeEntry(e.getWeight(), true, adjacentPointers.get(e
					.getTarget().getId()));
		}
	}

	public int write(byte[] buff, BlockEncodingParams enc) throws IOException {
		BitArrayOutputStream stream = new BitArrayOutputStream(buff);

		// --- HEADER ---

		stream.writeByte((byte) (level & 0xff));
		stream.writeUInt(numVerticesHavingNh, enc.bitsPerVertexOffset);
		stream.writeUInt(numVerticesHavingNoNh, enc.bitsPerVertexOffset);
		stream.writeUInt(numEdges, enc.bitsPerEdgeCount);
		stream.writeUInt(bitsPerEdgeWeight, 5);
		stream.writeUInt(bitsPerLon, 5);
		stream.writeUInt(bitsPerLat, 5);
		stream.writeInt(minLon);
		stream.writeInt(minLat);
		// TODO array length
		stream.alignPointer(1);

		// --- DATA ---

		// block-identifiers of referenced blocks :
		for (int i = 0; i < adjacentBlocks.length; i++) {
			stream.writeUInt(adjacentBlocks[i], enc.bitsPerClusterId);
		}
		stream.alignPointer(1);

		for (int i = 0; i < subjacentBlocks.length; i++) {
			stream.writeUInt(subjacentBlocks[i], enc.bitsPerClusterId);
		}
		stream.alignPointer(1);

		for (int i = 0; i < overlyingBlocks.length; i++) {
			stream.writeUInt(overlyingBlocks[i], enc.bitsPerClusterId);
		}
		stream.alignPointer(1);

		for (int i = 0; i < levelZeroBlocks.length; i++) {
			stream.writeUInt(levelZeroBlocks[i], enc.bitsPerClusterId);
		}
		stream.alignPointer(1);

		// vertices :
		int bitsPerSubjacentOffset = BitSerializer.numBitsToEncodeUInt(0,
				subjacentBlocks.length - 1);
		int bitsPerOverlyingZeroOffset = BitSerializer.numBitsToEncodeUInt(0,
				overlyingBlocks.length - 1);
		int bitsPerLevelZeroOffset = BitSerializer.numBitsToEncodeUInt(0,
				levelZeroBlocks.length - 1);

		for (VertexEntry v : vertices) {
			if (level > 1) {
				stream.writeUInt(v.interLevelPointers[POINTER_IDX_SUBJACENT].blockIdOffset,
						bitsPerSubjacentOffset);
				stream.writeUInt(v.interLevelPointers[POINTER_IDX_SUBJACENT].vertexOffset,
						enc.bitsPerVertexOffset);
			}
			if (level < (enc.numGraphLevels - 1)) {
				if (v.interLevelPointers[POINTER_IDX_OVERLYING] != null) {
					stream.writeUInt(v.interLevelPointers[POINTER_IDX_OVERLYING].blockIdOffset,
							bitsPerOverlyingZeroOffset);
					stream.writeUInt(v.interLevelPointers[POINTER_IDX_OVERLYING].vertexOffset,
							enc.bitsPerVertexOffset);
				} else {
					stream.writeUInt(0, bitsPerOverlyingZeroOffset + enc.bitsPerVertexOffset);
				}
			}
			if (level > 0) {
				stream.writeUInt(v.interLevelPointers[POINTER_IDX_LEVEL_ZERO].blockIdOffset,
						bitsPerLevelZeroOffset);
				stream.writeUInt(v.interLevelPointers[POINTER_IDX_LEVEL_ZERO].vertexOffset,
						enc.bitsPerVertexOffset);
			}
			stream.writeUInt(v.neighborhood, enc.bitsPerNeighborhood);
			if (level == 0) {
				stream.writeUInt(v.longitude - minLon, bitsPerLon);
				stream.writeUInt(v.longitude - minLon, bitsPerLat);
			}
		}
		stream.alignPointer(1);

		// edges
		int bitsPerAdjacentZeroOffset = BitSerializer.numBitsToEncodeUInt(0,
				adjacentBlocks.length - 1);
		for (EdgeEntry e : edges) {
			stream.writeUInt(e.weight, bitsPerEdgeWeight);
			stream.writeBit(e.isCore);
			stream.writeUInt(e.target.blockIdOffset, bitsPerAdjacentZeroOffset);
			stream.writeUInt(e.target.vertexOffset, enc.bitsPerVertexOffset);
		}
		stream.alignPointer(1);

		return stream.getByteOffset();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("--- HEADER ---\n\n");
		sb.append("level = " + level + "\n");
		sb.append("numVerticesHavingNh = " + numVerticesHavingNh + "\n");
		sb.append("numVerticesHavingNoNh = " + numVerticesHavingNoNh + "\n");
		sb.append("numEdges = " + numEdges + "\n");
		sb.append("bitsPerEdgeWeight = " + bitsPerEdgeWeight + "\n");
		sb.append("bitsPerLon = " + bitsPerLon + "\n");
		sb.append("bitsPerLat = " + bitsPerLat + "\n");
		sb.append("minLon = " + minLon + "\n");
		sb.append("minLat = " + minLat + "\n");
		sb.append("\n");
		sb.append("adjacentBlocks = " + adjacentBlocks.length + "\n");
		sb.append("subjacentBlocks = " + subjacentBlocks.length + "\n");
		sb.append("overlyingBlock = " + overlyingBlocks.length + "\n");
		sb.append("levelZeroBlocks = " + levelZeroBlocks.length + "\n");
		sb.append("\n");

		sb.append("--- REFERENCED BLOCKS ---\n");
		sb.append("\n");
		sb.append("adjacentBlocks = {" + Utils.arrToString(adjacentBlocks) + "}\n");
		sb.append("subjacentBlocks = {" + Utils.arrToString(subjacentBlocks) + "}\n");
		sb.append("overlyingBlocks = {" + Utils.arrToString(overlyingBlocks) + "}\n");
		sb.append("levelZeroBlocks = {" + Utils.arrToString(levelZeroBlocks) + "}\n");

		sb.append("\n");
		sb.append("--- VERTICES ---\n");
		sb.append("\n");
		for (int i = 0; i < vertices.length; i++) {
			sb.append(vertices[i] + "\n");
		}
		sb.append("\n");
		sb.append("--- EDGES ---\n");
		sb.append("\n");
		for (int i = 0; i < edges.length; i++) {
			sb.append(edges[i] + "\n");
		}
		return sb.toString();
	}

	private int[] getEdgeOffsets(ICluster c, ClusteringUtil cUtil) {
		LinkedList<LevelVertex> list = cUtil.getClusterVertices(c);
		int[] offsets = new int[list.size()];
		int i = 0;
		for (LevelVertex v : list) {
			if (i == 0) {
				offsets[i] = 0;
			} else {
				offsets[i] = offsets[i - 1] + v.getOutboundEdges().length;
			}
			i++;
		}
		return offsets;
	}

	private TIntObjectHashMap<IndirectVertexPointer> getVertexPointers(
			LinkedList<ICluster> referencedClusters, LinkedList<LevelVertex> vertexList,
			ClusteringUtil cUtil, int lvl) {

		// determine the offset where the cluster's block id will be stored.
		TObjectIntHashMap<ICluster> clusterToOffset = new TObjectIntHashMap<ICluster>();
		int offset = 0;
		for (ICluster c : referencedClusters) {
			clusterToOffset.put(c, offset++);
		}

		// calculate vertex references
		TIntObjectHashMap<IndirectVertexPointer> pointers = new TIntObjectHashMap<IndirectVertexPointer>();
		for (LevelVertex v : vertexList) {
			int vertexOffset = cUtil.getClusterOffset(v.getId(), lvl);
			ICluster c = cUtil.getCluster(v.getId(), lvl);
			if (clusterToOffset.containsKey(c)) {
				IndirectVertexPointer pointer = new IndirectVertexPointer(clusterToOffset
						.get(c), vertexOffset);
				pointers.put(v.getId(), pointer);
			}
		}
		return pointers;
	}

	private static int countVerticesHavingNeighborhood(LinkedList<LevelVertex> vertices) {
		int count = 0;
		for (LevelVertex v : vertices) {
			int nh = v.getNeighborhood();
			if (nh != HHComputation.INFINITY_1 && nh != HHComputation.INFINITY_2) {
				count++;
			}
		}
		return count;
	}

	public static void main(String[] args) throws IOException, ClassNotFoundException {
		System.out.print("reading files... ");
		LevelGraph levelGraph = Serializer.deserialize(new File("graph_ger"));
		KCenterClustering[] clustering = Serializer.deserialize(new File("clustering_ger"));
		System.out.println("ready!");

		ClusteringUtil cUtil = new ClusteringUtil(clustering, levelGraph);
		ClusterBlockMapping mapping = new ClusterBlockMapping(clustering);
		int count = 0;
		int bytes = 0;
		int V = 0;
		int E = 0;
		for (int lvl = 0; lvl < clustering.length; lvl++) {
			for (ICluster c : clustering[lvl].getClusters()) {
				Block b = new Block(c, mapping, cUtil);
				byte[] buff = new byte[1000000];
				bytes += b.write(buff, new BlockEncodingParams(24, 16, 24, 24,
						clustering.length));
				if ((++count) % 100 == 0) {
					System.out.println("[writing blocks] " + count + " / "
							+ cUtil.numClusters());
				}
				V += b.vertices.length;
				E += b.edges.length;
			}
		}
		DecimalFormat df = new DecimalFormat("###,###,###");
		System.out.println(df.format(bytes) + " Bytes |V| = " + df.format(V) + ", |E| = "
				+ df.format(E));
	}
}
