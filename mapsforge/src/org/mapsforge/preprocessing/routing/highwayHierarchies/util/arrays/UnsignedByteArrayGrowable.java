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
package org.mapsforge.preprocessing.routing.highwayHierarchies.util.arrays;

import java.io.Serializable;

/**
 * @author Frank Viernau
 */
public class UnsignedByteArrayGrowable implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5352302212205236878L;
	private final IntArrayGrowable data;
	private int size;
	private int fourByteOffset, byteOffset;

	public UnsignedByteArrayGrowable(int chunkSize) {
		data = new IntArrayGrowable(chunkSize / 4);
		data.add(0);
		fourByteOffset = byteOffset = size = 0;
	}

	public void add(int val) {
		val &= 0x000000ff;
		if (byteOffset == 4) {
			byteOffset = 0;
			fourByteOffset++;
			data.add(0);
		}
		data.set(fourByteOffset,
				(data.get(fourByteOffset) & (~(0x000000ff << (byteOffset * 8))))
						| (val << (byteOffset * 8)));
		byteOffset++;
		size++;
	}

	public void set(int idx, int val) {
		val &= 0x000000ff;
		int offsetA = idx / 4;
		int offsetB = idx % 4;
		data.set(offsetA, (data.get(offsetA) & (~(0x000000ff << (offsetB * 8))))
				| (val << (offsetB * 8)));
	}

	public int get(int idx) {
		int offsetA = idx / 4;
		int offsetB = idx % 4;
		return (data.get(offsetA) >>> (offsetB * 8)) & 0x000000ff;
	}

	public int size() {
		return size;
	}

	public static void main(String[] args) {
		UnsignedByteArrayGrowable a = new UnsignedByteArrayGrowable(100);
		for (int i = 0; i < 10000; i++) {
			a.add(i);
		}
		a.set(3, 244);
		a.set(4, 255);
		a.set(5, 246);
		a.set(6, 247);
		for (int i = 0; i < 10000; i++) {
			System.out.println(a.get(i));
		}

	}
}
