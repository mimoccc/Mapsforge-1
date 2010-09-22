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

import static org.junit.Assert.assertEquals;

import java.util.Random;
import java.util.Stack;

import org.junit.Test;

public class BitSerializerTest {

	private final static int N = 10000;

	@Test
	public void testByteRW() {
		long seed = System.currentTimeMillis();
		byte[] buff = new byte[N];
		int bitOffset = 1;
		int[] byteOffsets = getRandomOffsets(N - 1, 1);

		Random rnd = new Random(seed);
		for (int i = 0; i < byteOffsets.length; i++) {
			BitSerializer.writeByte((byte) rnd.nextInt(), buff, byteOffsets[i], bitOffset);
		}

		rnd = new Random(seed);
		for (int i = 0; i < byteOffsets.length; i++) {
			byte val = (byte) rnd.nextInt();
			byte val_ = BitSerializer.readByte(buff, byteOffsets[i], bitOffset);
			assertEquals(val, val_);
		}
	}

	@Test
	public void testShortRW() {
		long seed = System.currentTimeMillis();
		int alignment = 2;
		byte[] buff = new byte[N * alignment];
		int[] byteOffsets = getRandomOffsets(N - 1, alignment);

		for (int bitOffset = 0; bitOffset < 8; bitOffset++) {
			Random rnd = new Random(seed);
			for (int i = 0; i < byteOffsets.length; i++) {
				BitSerializer
						.writeShort((short) rnd.nextInt(), buff, byteOffsets[i], bitOffset);
			}

			rnd = new Random(seed);
			for (int i = 0; i < byteOffsets.length; i++) {
				short val = (short) rnd.nextInt();
				short val_ = BitSerializer.readShort(buff, byteOffsets[i], bitOffset);
				assertEquals(val, val_);
			}
		}
	}

	@Test
	public void testIntRW() {
		long seed = System.currentTimeMillis();
		int alignment = 4;
		byte[] buff = new byte[N * alignment];
		int[] byteOffsets = getRandomOffsets(N - 1, alignment);

		for (int bitOffset = 0; bitOffset < 8; bitOffset++) {
			Random rnd = new Random(seed);
			for (int i = 0; i < byteOffsets.length; i++) {
				BitSerializer.writeInt(rnd.nextInt(), buff, byteOffsets[i], bitOffset);
			}

			rnd = new Random(seed);
			for (int i = 0; i < byteOffsets.length; i++) {
				int val = rnd.nextInt();
				int val_ = BitSerializer.readInt(buff, byteOffsets[i], bitOffset);
				assertEquals(val, val_);
			}
		}
	}

	@Test
	public void testLongRW() {
		long seed = System.currentTimeMillis();
		int alignment = 8;
		byte[] buff = new byte[N * alignment];
		int[] byteOffsets = getRandomOffsets(N - 1, alignment);

		for (int bitOffset = 0; bitOffset < 8; bitOffset++) {
			Random rnd = new Random(seed);
			for (int i = 0; i < byteOffsets.length; i++) {
				BitSerializer.writeLong(rnd.nextLong(), buff, byteOffsets[i], bitOffset);
			}

			rnd = new Random(seed);
			for (int i = 0; i < byteOffsets.length; i++) {
				long val = rnd.nextLong();
				long val_ = BitSerializer.readLong(buff, byteOffsets[i], bitOffset);
				assertEquals(val, val_);
			}
		}
	}

	@Test
	public void testUintRW() {
		int[] vals = getRandomInts(N);
		byte[] buff = new byte[N * 64];
		int[] byteOffset = new int[N];
		int[] bitOffset = new int[N];
		byteOffset[0] = 0;
		bitOffset[0] = 0;
		for (int i = 1; i < vals.length; i++) {
			int nBits = (int) Math.floor(Math.log(vals[i - 1]) / Math.log(2)) + 1;
			byteOffset[i] = byteOffset[i - 1] + ((bitOffset[i - 1] + nBits) / 8);
			bitOffset[i] = (bitOffset[i - 1] + nBits) % 8;
		}

		int[] order = getRandomOffsets(N, 1);
		for (int i = 1; i < order.length; i++) {
			int nBits = (int) Math.floor(Math.log(vals[order[i]]) / Math.log(2)) + 1;
			BitSerializer.writeUInt(vals[order[i]], nBits, buff, byteOffset[order[i]],
					bitOffset[order[i]]);
			int val = (int) BitSerializer.readUInt(buff, nBits, byteOffset[order[i]],
					bitOffset[order[i]]);
			assertEquals(vals[order[i]], val);
		}
		for (int i = 1; i < order.length; i++) {
			int nBits = (int) Math.floor(Math.log(vals[order[i]]) / Math.log(2)) + 1;
			int val = (int) BitSerializer.readUInt(buff, nBits, byteOffset[order[i]],
					bitOffset[order[i]]);
			assertEquals(vals[order[i]], val);
		}
	}

	@Test
	public void testBitRW() {
		byte[] buff = new byte[(N / 8) + 1];
		boolean[] bits = getRandomBooleans(N);
		int byteOffset = 0;
		int bitOffset = 0;
		for (int i = 0; i < bits.length; i++) {
			BitSerializer.writeBit(bits[i], buff, byteOffset, bitOffset);
			bitOffset++;
			if (bitOffset == 8) {
				byteOffset++;
				bitOffset = 0;
			}
		}
		byteOffset = 0;
		bitOffset = 0;
		for (int i = 0; i < bits.length; i++) {
			boolean b = BitSerializer.readBit(buff, byteOffset, bitOffset);
			assertEquals(bits[i], b);
			bitOffset++;
			if (bitOffset == 8) {
				byteOffset++;
				bitOffset = 0;
			}
		}
	}

	private boolean[] getRandomBooleans(int n) {
		boolean[] b = new boolean[n];
		Random rnd = new Random();
		for (int i = 0; i < b.length; i++) {
			b[i] = rnd.nextBoolean();
		}
		return b;
	}

	private int[] getRandomOffsets(int n, int alignment) {
		Random rnd = new Random();
		Stack<Integer> stack = new Stack<Integer>();
		for (int i = 0; i < n; i++) {
			stack.push(i * alignment);
		}
		int j = 0;
		int[] randomOffsets = new int[n];
		while (!stack.isEmpty()) {
			int x = rnd.nextInt(stack.size());
			randomOffsets[j++] = stack.get(x);
			stack.remove(x);
		}
		return randomOffsets;
	}

	private int[] getRandomInts(int n) {
		Random rnd = new Random();
		int[] vals = new int[n];
		double j = 1;
		for (int i = 0; i < n; i++) {
			vals[i] = rnd.nextInt((int) Math.pow(2, j++));
			if (j == 30) {
				j = 3;
			}
			if (vals[i] <= 2) {
				i--;
			}
		}
		return vals;
	}

}
