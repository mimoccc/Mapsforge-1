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

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.mapsforge.preprocessing.routing.hhmobile.util.BitArrayInputStream;
import org.mapsforge.preprocessing.routing.hhmobile.util.BitArrayOutputStream;
import org.mapsforge.preprocessing.routing.hhmobile.util.Utils;

/**
 * This class implements a run length encoded index of pointers, pointing to variable length
 * blocks. Pointers have to be sorted by ascending block sizes. This reduces the problem to
 * compress a sorted array of integer values. The compression rate is just a constant factor but
 * since the ram is very sparse on smart phones, this saves relatively much of ram. It is traded
 * off with runtime, on the one hand caused by the required shift operations, on the other hand
 * a constant amount of pointers has to be read, to access a single pointer entry. The overhead
 * is constant an can be limited by group size. Additionally a method is provided which
 * determines the optimal group size with regard to a given threshold.
 */
public final class BlockPointerIndex {

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

	/**
	 * Constructs an index for given group size and block sizes. The block sizes must be sorted
	 * ascending. It is assumed that the first pointer points to address 0, while addressing
	 * blockSize[0] bytes. The i st pointer points to the end address of the i-1 st pointer. The
	 * Index is separated in groups of constant size where each group is individually encoded.
	 * This especially addresses the characteristics of block sizes computed by the k-center
	 * algorithm. The block sizes are normal distributed so many of the blocks are encoded
	 * efficiently. The parameter gSize determines the size of the groups of pointers and thus
	 * also limit the runtime overhead.
	 * 
	 * @param blockSize
	 *            sorted ascending, the block size at array index i belongs to block with id i.
	 * @param gSize
	 *            pointer are grouped in groups of size gSize.
	 */
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

	/**
	 * Constructs and index based on the serialization.
	 * 
	 * @param buff
	 *            serialized index.
	 * @throws IOException
	 *             thrown if something is wrong with the byte array.
	 */
	public BlockPointerIndex(byte[] buff) throws IOException {
		ByteArrayInputStream iStream = new ByteArrayInputStream(buff);
		DataInputStream in = new DataInputStream(iStream);

		this.gSize = in.readInt();
		this.numBlocks = in.readInt();

		int numGroups = in.readInt();
		this.gStartAddr = new long[numGroups];
		this.gBlockEncOffs = new int[numGroups];
		this.gFirstBlockSize = new int[numGroups];
		this.gEncBits = new byte[numGroups];
		for (int groupIdx = 0; groupIdx < numGroups; groupIdx++) {
			gStartAddr[groupIdx] = in.readLong();
			gBlockEncOffs[groupIdx] = in.readInt();
			gFirstBlockSize[groupIdx] = in.readInt();
			gEncBits[groupIdx] = in.readByte();
		}

		int encBlockSizeLength = in.readInt();
		this.encBlockSize = new byte[encBlockSizeLength];
		in.read(encBlockSize);
	}

	/**
	 * Determines the optimal group size limited by the given parameter maxGSize, by building
	 * the index for each possible group size and choosing the less space consuming one.
	 * 
	 * @param blockSize
	 *            size of the blocks to be indexed, sorted ascending.
	 * @param maxGSize
	 *            limit on size of the pointer groups.
	 * @return the space optimal index concerning the given parameters.
	 */
	public static BlockPointerIndex getSpaceOptimalIndex(int[] blockSize, int maxGSize) {
		maxGSize = Math.min(blockSize.length, maxGSize);
		int[] indexSize = new int[maxGSize - MIN_G_SIZE + 1];

		for (int gSize = MIN_G_SIZE; gSize <= maxGSize; gSize++) {
			BlockPointerIndex index = new BlockPointerIndex(blockSize, gSize);
			indexSize[gSize - MIN_G_SIZE] = index.byteSize();
		}
		int optGSize = MIN_G_SIZE + Utils.firstIndexOfMin(indexSize);

		return new BlockPointerIndex(blockSize, optGSize);
	}

	/**
	 * Writes this index to a stream.
	 * 
	 * @param oStream
	 *            stream to write to.
	 * @throws IOException
	 *             on write error.
	 */
	public void serialize(OutputStream oStream) throws IOException {
		DataOutputStream out = new DataOutputStream(oStream);

		out.writeInt(gSize);
		out.writeInt(numBlocks);

		out.writeInt(gStartAddr.length);
		for (int i = 0; i < gStartAddr.length; i++) {
			out.writeLong(gStartAddr[i]);
			out.writeInt(gBlockEncOffs[i]);
			out.writeInt(gFirstBlockSize[i]);
			out.writeByte(gEncBits[i]);
		}

		out.writeInt(encBlockSize.length);
		out.write(encBlockSize);

		out.flush();
	}

	/**
	 * Reads the pointer with the given id.
	 * 
	 * @param blockId
	 *            id of the pointer.
	 * @return pointer identified by the given id or bull if id is out of range.
	 */
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
			return null;
		}

	}

	/**
	 * @return number of pointers in index.
	 */
	public int size() {
		return numBlocks;
	}

	/**
	 * @return number of bytes consumed in ram.
	 */
	public int byteSize() {
		return 8 // gSize, numBlocks
				+ 4 // g*[].length
				+ (17 * gStartAddr.length) // g*[] entries
				+ 4 // encBlocksize[].length
				+ encBlockSize.length; // encBlockSize[] entries
	}

	@Override
	public String toString() {
		return "BlockPointerIndex(size=" + size() + ")";
	}

	/**
	 * 
	 * @param gEncBits
	 *            number of bits used for encoding a single entry. gEncBits[i] determines the
	 *            number of bits per entry used for the i th group.
	 * @param blockSize
	 *            sizes sorted ascending.
	 * @param gSize
	 *            number of pointers per group.
	 * @param gBlockEncOffsBuff
	 *            the start addresses of each group are written here.
	 * @param gFirstBlockSizeBuff
	 *            the sizes of the first pointer of each group are put here.
	 * @return run length encoded block sizes.
	 */
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

	/**
	 * Computes the number of bits required for encoding an entry of the respective group.
	 * 
	 * @param gMaxDiff
	 *            gMaxDiff[i] contains the maximum difference of two succeeding entries of the i
	 *            th group.
	 * @return number of bits to encode for each group.
	 */
	private static byte[] getGroupEncBits(int[] gMaxDiff) {
		byte[] gEncBits = new byte[gMaxDiff.length];
		for (int i = 0; i < gMaxDiff.length; i++) {
			gEncBits[i] = Utils.numBitsToEncode(0, gMaxDiff[i]);
		}
		return gEncBits;
	}

	/**
	 * Computes start addresses of each group.
	 * 
	 * @param gByteSize
	 *            gByteSize[i] determines the number of bytes to encode the i th group.
	 * @return start addresses of each group.
	 */
	private static long[] getGroupStartAddr(long[] gByteSize) {
		long[] gStartAddr = new long[gByteSize.length];
		gStartAddr[0] = 0;
		for (int i = 0; i < gByteSize.length - 1; i++) {
			gStartAddr[i + 1] = gStartAddr[i] + gByteSize[i];
		}
		return gStartAddr;
	}

	/**
	 * Number of bytes required for encoding each group.
	 * 
	 * @param blockSize
	 *            block sizes sorted ascending.
	 * @param gSize
	 *            number of entries per group.
	 * @return number of bytes required for each group.
	 */
	private static long[] getGroupByteSize(int[] blockSize, int gSize) {
		int numG = getNumGroups(blockSize.length, gSize);
		long[] gByteSize = new long[numG];
		for (int i = 0; i < numG; i++) {
			gByteSize[i] = Utils.sum(blockSize, i * gSize, Math.min((i + 1) * gSize,
					blockSize.length));
		}
		return gByteSize;
	}

	/**
	 * Computes the maximum difference of succeeding entries for each group.
	 * 
	 * @param blockSize
	 *            block sizes sorted ascending.
	 * @param gSize
	 *            number of entries per group.
	 * @return maximum difference within each group.
	 */
	private static int[] getGroupMaxSucceedingBlockSizeDiff(int[] blockSize, int gSize) {
		int numG = getNumGroups(blockSize.length, gSize);
		int[] maxDiffs = new int[numG];
		for (int i = 0; i < numG; i++) {
			maxDiffs[i] = maxSucceedingDiff(blockSize, i * gSize, Math.min((i + 1) * gSize,
					blockSize.length));
		}
		return maxDiffs;
	}

	/**
	 * Helper function for getGroupMaxSucceedingBlockSizeDiff.
	 * 
	 * @param arr
	 *            the values.
	 * @param start
	 *            start index.
	 * @param end
	 *            end index.
	 * @return maximum difference of succeeding array entries within the given bounds.
	 */
	private static int maxSucceedingDiff(int[] arr, int start, int end) {
		int maxDiff = 0;
		for (int i = start + 1; i < end; i++) {
			maxDiff = Math.max(maxDiff, arr[i] - arr[i - 1]);
		}
		return maxDiff;
	}

	/**
	 * Computes the number of groups.
	 * 
	 * @param numBlocks
	 *            number of entries to index.
	 * @param gSize
	 *            entries per group.
	 * @return number of groups to index.
	 */
	private static int getNumGroups(int numBlocks, int gSize) {
		int numG = (int) Math.ceil(((double) numBlocks) / ((double) gSize));
		return numG;
	}
}
