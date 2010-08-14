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
package org.mapsforge.android.routing.hh;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import org.mapsforge.preprocessing.routing.hhmobile.util.BitArrayInputStream;

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
final class AddressLookupTable {

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

	private final int byteSize;

	/**
	 * Constructs and index based on the serialization.
	 * 
	 * @param buff
	 *            serialized index.
	 * @throws IOException
	 *             thrown if something is wrong with the byte array.
	 */
	public AddressLookupTable(long startAddr, long endAddr, File f) throws IOException {
		// read into ram
		this.byteSize = (int) (endAddr - startAddr);
		RandomAccessFile raf = new RandomAccessFile(f, "r");
		byte[] buff = new byte[byteSize];
		raf.seek(startAddr);
		raf.readFully(buff);
		raf.close();

		// deserialize
		DataInputStream in = new DataInputStream(new ByteArrayInputStream(buff));

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
		in.close();
	}

	/**
	 * Reads the pointer with the given id.
	 * 
	 * @param blockId
	 *            id of the pointer.
	 * @return pointer identified by the given id or bull if id is out of range.
	 */
	public Pointer getPointer(int blockId) {
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
			return new Pointer(blockStartAddr, blockSize);
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
		return byteSize;
	}
}
