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

import java.io.IOException;

import org.mapsforge.preprocessing.routing.hhmobile.util.BitArrayInputStream;
import org.mapsforge.preprocessing.routing.hhmobile.util.BitArrayOutputStream;
import org.mapsforge.preprocessing.routing.hhmobile.util.Utils;

public class BlockPointerIndex {

	private final static int MIN_G_SIZE = 5;

	// 8 bytes overhead per index
	private final int gSize;
	private final int numBlocks;

	// 17 bytes overhead per group
	private final long[] gStartAddr;
	private final int[] gBlockEncOffs;
	private final int[] gFirstBlockSize;
	private final byte[] gEncBits;

	// encoded succeeding block size differences (first of a group is zero)
	private final byte[] encBlockSize;

	public BlockPointerIndex(int[] blockSize, int gSize) {
		this.gSize = gSize;
		this.numBlocks = blockSize.length;

		long[] gByteSize = getGroupByteSize(blockSize, gSize);
		this.gStartAddr = getGroupStartAddr(gByteSize);

		int[] gMaxDiff = getGroupMaxSucceedingBlockSizeDiff(blockSize, gSize);
		this.gEncBits = getGroupEncBits(gMaxDiff);

		int numG = getNumGroups(blockSize.length, gSize);
		this.gBlockEncOffs = new int[numG];
		this.gFirstBlockSize = new int[numBlocks];
		this.encBlockSize = encodeBlockSizes(gEncBits, blockSize, gSize, gBlockEncOffs,
				gFirstBlockSize);
	}

	public static BlockPointerIndex getOptimalIndex(int[] blockSize, int maxGSize) {
		maxGSize = Math.min(blockSize.length, maxGSize);
		int[] indexSize = new int[maxGSize - MIN_G_SIZE + 1];

		for (int gSize = MIN_G_SIZE; gSize <= maxGSize; gSize++) {
			BlockPointerIndex index = new BlockPointerIndex(blockSize, gSize);
			indexSize[gSize - MIN_G_SIZE] = index.byteSize();
		}
		int optGSize = MIN_G_SIZE + Utils.firstIndexOfMin(indexSize);

		return new BlockPointerIndex(blockSize, optGSize);
	}

	public BlockPointer getPointer(int blockId) {
		try {
			final int groupIdx = blockId / gSize;
			final int blockOffset = blockId % gSize;

			long blockStartAddr = gStartAddr[groupIdx];
			int blockSize = gFirstBlockSize[groupIdx];

			BitArrayInputStream stream = new BitArrayInputStream(encBlockSize);
			stream.setPointer(gBlockEncOffs[groupIdx], 0);
			for (int i = 0; i < blockOffset; i++) {
				blockStartAddr += blockSize;
				blockSize += stream.readUInt(gEncBits[groupIdx]);
			}
			return new BlockPointer(blockStartAddr, blockSize);
		} catch (IOException e) {
			throw new RuntimeException("Got impossible error!");
		}

	}

	public int size() {
		return numBlocks;
	}

	public int byteSize() {
		return 8 + (17 * gStartAddr.length) + encBlockSize.length;
	}

	private static byte[] encodeBlockSizes(byte[] gEncBits, int[] blockSize, int gSize,
			int[] gBlockEncOffsBuff, int[] gFirstBlockSizeBuff) {
		try {
			byte[] buff = new byte[blockSize.length * 32];
			BitArrayOutputStream stream = new BitArrayOutputStream(buff);

			for (int i = 0; i < blockSize.length; i++) {

				if (i % gSize == 0) {
					// the first element has always difference = 0
					stream.alignPointer(1);

					gBlockEncOffsBuff[i / gSize] = stream.getByteOffset();
					gFirstBlockSizeBuff[i / gSize] = blockSize[i];

					// no entry for first element of each group!! would always be zero
				} else {
					stream.writeUInt(blockSize[i] - blockSize[i - 1], gEncBits[i / gSize]);
				}
			}
			stream.alignPointer(1);

			byte[] result = new byte[stream.getByteOffset()];
			for (int i = 0; i < result.length; i++) {
				result[i] = buff[i];
			}
			return result;
		} catch (IOException e) {
			throw new RuntimeException("Got impossible error!");
		}
	}

	private static byte[] getGroupEncBits(int[] gMaxDiff) {
		byte[] gEncBits = new byte[gMaxDiff.length];
		for (int i = 0; i < gMaxDiff.length; i++) {
			gEncBits[i] = Utils.numBitsToEncode(0, gMaxDiff[i]);
		}
		return gEncBits;
	}

	private static long[] getGroupStartAddr(long[] gByteSize) {
		long[] gStartAddr = new long[gByteSize.length];
		gStartAddr[0] = 0;
		for (int i = 0; i < gByteSize.length - 1; i++) {
			gStartAddr[i + 1] = gStartAddr[i] + gByteSize[i];
		}
		return gStartAddr;
	}

	private static long[] getGroupByteSize(int[] blockSize, int gSize) {
		int numG = getNumGroups(blockSize.length, gSize);
		long[] gByteSize = new long[numG];
		for (int i = 0; i < numG; i++) {
			gByteSize[i] = Utils.sum(blockSize, i * gSize, Math.min((i + 1) * gSize,
					blockSize.length));
		}
		return gByteSize;
	}

	private static int[] getGroupMaxSucceedingBlockSizeDiff(int[] blockSize, int gSize) {
		int numG = getNumGroups(blockSize.length, gSize);
		int[] maxDiffs = new int[numG];
		for (int i = 0; i < numG; i++) {
			maxDiffs[i] = maxSucceedingDiff(blockSize, i * gSize, Math.min((i + 1) * gSize,
					blockSize.length));
		}
		return maxDiffs;
	}

	private static int maxSucceedingDiff(int[] arr, int start, int end) {
		int maxDiff = 0;
		for (int i = start + 1; i < end; i++) {
			maxDiff = Math.max(maxDiff, arr[i] - arr[i - 1]);
		}
		return maxDiff;
	}

	private static int getNumGroups(int numBlocks, int gSize) {
		int numG = (int) Math.ceil(((double) numBlocks) / ((double) gSize));
		return numG;
	}
}
