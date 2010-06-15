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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.hadoop.util.IndexedSortable;
import org.apache.hadoop.util.QuickSort;
import org.mapsforge.preprocessing.routing.hhmobile.clustering.ClusteringUtil;
import org.mapsforge.preprocessing.routing.hhmobile.clustering.ICluster;
import org.mapsforge.preprocessing.routing.hhmobile.clustering.IClustering;
import org.mapsforge.preprocessing.routing.hhmobile.graph.LevelGraph;
import org.mapsforge.preprocessing.routing.hhmobile.graph.LevelGraph.Level;
import org.mapsforge.preprocessing.routing.hhmobile.util.Utils;

public class BlockedGraphSerializer {

	private final static int BUFFER_SIZE = 25000000;
	private final static byte[] BUFFER = new byte[BUFFER_SIZE];

	public static BlockEncoding writeBlockedGraph(File clusterBlocksFile,
			File blockPointerIndexFile, LevelGraph levelGraph, IClustering[] clustering,
			int indexGroupSizeThreshold) throws IOException {

		// write cluster blocks
		BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(
				clusterBlocksFile));
		int numBlocks = ClusteringUtil.getGlobalNumClusters(clustering);
		int[] blockSize = new int[numBlocks];
		BlockEncoding blockEnc = writeBlocks(out, levelGraph, clustering, blockSize);
		out.close();

		// write block index
		BlockPointerIndex blockIndex = BlockPointerIndex.getSpaceOptimalIndex(blockSize,
				indexGroupSizeThreshold);
		out = new BufferedOutputStream(new FileOutputStream(blockPointerIndexFile));
		blockIndex.serialize(out);
		out.flush();
		out.close();

		return blockEnc;
	}

	private static BlockEncoding writeBlocks(OutputStream oStream, LevelGraph levelGraph,
			IClustering[] clustering, int[] blockSizeBuff) throws IOException {
		ClusterBlockMapping mapping = new ClusterBlockMapping(clustering);
		ClusteringUtil cUtil = new ClusteringUtil(clustering, levelGraph);

		int[] byteSize = getBlockByteSizes(mapping, cUtil, getBlockEncoding(cUtil));
		System.out.println(getBlockEncoding(cUtil));

		reassignBlockIdsByAscByteSize(byteSize, mapping);
		reassignClusterVertexOffsetByAscNh(levelGraph, clustering);

		// important to create new instance after block'ids and vertex order has changed!
		// since some values are pre-processed
		cUtil = new ClusteringUtil(clustering, levelGraph);

		// write the blocks
		BlockEncoding graphHeader = getBlockEncoding(cUtil);
		Utils.setZero(BUFFER, 0, BUFFER.length);

		for (int blockId = 0; blockId < mapping.size(); blockId++) {
			Block block = new Block(mapping.getCluster(blockId), mapping, cUtil);
			blockSizeBuff[blockId] = block.serialize(BUFFER, graphHeader);

			oStream.write(BUFFER, 0, blockSizeBuff[blockId]);
			Utils.setZero(BUFFER, 0, blockSizeBuff[blockId]);
		}
		return graphHeader;
	}

	private static BlockEncoding getBlockEncoding(ClusteringUtil cUtil) {
		byte bitsPerClusterId = Utils.numBitsToEncode(0, cUtil.getGlobalNumClusters() - 1);
		byte bitsPerVertexOffset = Utils.numBitsToEncode(0, cUtil
				.getGlobalMaxVerticesPerCluster());
		byte bitsPerEdgeCount = Utils.numBitsToEncode(0, cUtil.getGlobalMaxEdgesPerCluster());
		byte bitsPerNeighborhood = Utils.numBitsToEncode(0, cUtil.getGlobalMaxNeighborhood());
		byte numGraphLevels = (byte) cUtil.getGlobalNumLevels();
		return new BlockEncoding(bitsPerClusterId, bitsPerVertexOffset, bitsPerEdgeCount,
				bitsPerNeighborhood, numGraphLevels);
	}

	static void printEncodingParams(ClusteringUtil cUtil) {
		for (int lvl = 0; lvl < cUtil.getGlobalNumLevels(); lvl++) {
			byte bitsPerClusterId = Utils
					.numBitsToEncode(0, cUtil.getLevelNumClusters(lvl) - 1);
			byte bitsPerVertexOffset = Utils.numBitsToEncode(0, cUtil
					.getLevelMaxVerticesPerCluster(lvl));
			byte bitsPerEdgeCount = Utils.numBitsToEncode(0, cUtil
					.getLevelMaxEdgesPerCluster(lvl));
			byte bitsPerNeighborhood = Utils.numBitsToEncode(0, cUtil
					.getLevelMaxNeighborhood(lvl));
			byte numGraphLevels = (byte) cUtil.getGlobalNumLevels();
			BlockEncoding enc = new BlockEncoding(bitsPerClusterId, bitsPerVertexOffset,
					bitsPerEdgeCount, bitsPerNeighborhood, numGraphLevels);
			System.out.println(enc);
		}
	}

	private static int[] getBlockByteSizes(ClusterBlockMapping mapping, ClusteringUtil cUtil,
			BlockEncoding enc) throws IOException {
		int[] byteSize = new int[mapping.size()];

		for (int i = 0; i < byteSize.length; i++) {
			ICluster c = mapping.getCluster(i);
			Block b = new Block(c, mapping, cUtil);
			byteSize[i] = b.serialize(BUFFER, enc);
		}
		return byteSize;
	}

	private static void reassignBlockIdsByAscByteSize(final int[] byteSize,
			final ClusterBlockMapping mapping) {
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

	private static void reassignClusterVertexOffsetByAscNh(LevelGraph levelGraph,
			IClustering[] clustering) {
		QuickSort quicksort = new QuickSort();
		for (int lvl = 0; lvl < clustering.length; lvl++) {
			Level graph = levelGraph.getLevel(lvl);
			for (ICluster c : clustering[lvl].getClusters()) {
				final int[] vertexIds = c.getVertices();
				final int[] nh = new int[vertexIds.length];
				for (int i = 0; i < vertexIds.length; i++) {
					nh[i] = graph.getVertex(vertexIds[i]).getNeighborhood();
				}
				quicksort.sort(new IndexedSortable() {

					@Override
					public void swap(int i, int j) {
						Utils.swap(nh, i, j);
						Utils.swap(vertexIds, i, j);
					}

					@Override
					public int compare(int i, int j) {
						return nh[i] - nh[j];
					}
				}, 0, c.size());
			}
		}
	}
}
