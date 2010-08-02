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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.hadoop.util.IndexedSortable;
import org.apache.hadoop.util.QuickSort;
import org.mapsforge.preprocessing.routing.hhmobile.clustering.Cluster;
import org.mapsforge.preprocessing.routing.hhmobile.clustering.Clustering;
import org.mapsforge.preprocessing.routing.hhmobile.clustering.ClusteringUtil;
import org.mapsforge.preprocessing.routing.hhmobile.graph.LevelGraph;
import org.mapsforge.preprocessing.routing.hhmobile.graph.LevelGraph.Level;
import org.mapsforge.preprocessing.routing.hhmobile.util.BitArrayOutputStream;
import org.mapsforge.preprocessing.routing.hhmobile.util.Utils;

public class BlockedGraphSerializer {

	public static boolean DEBUG = false;

	private final static int BUFFER_SIZE = 25000000;
	private final static byte[] BUFFER = new byte[BUFFER_SIZE];

	public static int[] writeBlockedGraph(File fHeader, File fClusterBlocks,
			LevelGraph levelGraph, Clustering[] clustering) throws IOException {

		ClusteringUtil cUtil = new ClusteringUtil(clustering, levelGraph);
		BlockedGraphHeader header = computeHeader(cUtil);

		// --- WRITE HEADER FILE ---

		BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(fHeader));
		out.write(header.serialize());
		out.close();

		// --- SORT DATA ---

		sortClusterVertices(levelGraph, clustering);
		ClusterBlockMapping mapping = new ClusterBlockMapping(clustering);
		// need new instance since we swapped clusters and vertices
		cUtil = new ClusteringUtil(clustering, levelGraph);
		int[] blockSizes = getBlockByteSizes(mapping, cUtil, header);
		sortClusters(blockSizes, mapping);

		// --- WRITE CLUSTER BLOCKS FILE ---

		out = new BufferedOutputStream(new FileOutputStream(fClusterBlocks));

		int[] blockSize = new int[mapping.size()];
		for (int i = 0; i < mapping.size(); i++) {
			blockSize[i] = serializeBlock(BUFFER, mapping.getCluster(i), mapping, cUtil, header);
			out.write(BUFFER, 0, blockSize[i]);
			Utils.setZero(BUFFER, 0, blockSize[i]);
		}

		out.close();
		return blockSize;
	}

	private static BlockedGraphHeader computeHeader(ClusteringUtil cUtil) {
		byte bitsPerClusterId = Utils.numBitsToEncode(0, cUtil.getGlobalNumClusters() - 1);
		byte bitsPerVertexOffset = Utils.numBitsToEncode(0, cUtil
				.getGlobalMaxVerticesPerCluster());
		byte bitsPerEdgeCount = Utils.numBitsToEncode(0, cUtil.getGlobalMaxEdgesPerCluster());
		byte bitsPerNeighborhood = Utils.numBitsToEncode(0, cUtil.getGlobalMaxNeighborhood());
		byte numGraphLevels = (byte) cUtil.getGlobalNumLevels();
		return new BlockedGraphHeader(bitsPerClusterId, bitsPerVertexOffset, bitsPerEdgeCount,
				bitsPerNeighborhood, numGraphLevels);
	}

	private static int[] getBlockByteSizes(ClusterBlockMapping mapping, ClusteringUtil cUtil,
			BlockedGraphHeader header) throws IOException {
		int[] byteSize = new int[mapping.size()];
		for (int i = 0; i < byteSize.length; i++) {
			byteSize[i] = serializeBlock(BUFFER, mapping.getCluster(i), mapping, cUtil, header);
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
			ClusterBlockMapping mapping, ClusteringUtil cUtil, BlockedGraphHeader enc)
			throws IOException {

		Block b = new Block(cluster, mapping, cUtil);
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

}
