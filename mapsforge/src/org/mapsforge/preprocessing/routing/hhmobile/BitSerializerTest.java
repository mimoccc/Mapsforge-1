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
package org.mapsforge.preprocessing.routing.hhmobile;

import static org.junit.Assert.assertEquals;

import java.util.Random;
import java.util.Stack;

import org.junit.Test;

public class BitSerializerTest {

	@Test
	public void testByteRW() {
		long seed = System.currentTimeMillis();
		int n = 10000;
		byte[] buff = new byte[n];
		int bitOffset = 1;
		int[] byteOffsets = getRandomOffsets(n - 1, 1);

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
		int n = 10000;
		int alignment = 2;
		byte[] buff = new byte[n * alignment];
		int[] byteOffsets = getRandomOffsets(n - 1, alignment);

		for (int bitOffset = 0; bitOffset < 8; bitOffset++) {
			Random rnd = new Random(seed);
			for (int i = 0; i < byteOffsets.length; i++) {
				BitSerializer.writeShort((short) rnd.nextInt(), buff, byteOffsets[i], bitOffset);
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
		int n = 10000;
		int alignment = 4;
		byte[] buff = new byte[n * alignment];
		int[] byteOffsets = getRandomOffsets(n - 1, alignment);

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
		int n = 10000;
		int alignment = 8;
		byte[] buff = new byte[n * alignment];
		int[] byteOffsets = getRandomOffsets(n - 1, alignment);

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
		int n = 10000;
		int[] vals = getRandomInts(n);
		byte[] buff = new byte[n * 64];
		int[] byteOffset = new int[n];
		int[] bitOffset = new int[n];
		byteOffset[0] = 0;
		bitOffset[0] = 0;
		for (int i = 1; i < vals.length; i++) {
			int nBits = (int) Math.floor(Math.log(vals[i - 1]) / Math.log(2)) + 1;
			byteOffset[i] = byteOffset[i - 1] + ((bitOffset[i - 1] + nBits) / 8);
			bitOffset[i] = (bitOffset[i - 1] + nBits) % 8;
		}

		int[] order = getRandomOffsets(n, 1);
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
