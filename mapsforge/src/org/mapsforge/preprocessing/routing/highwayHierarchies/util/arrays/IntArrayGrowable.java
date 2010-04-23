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
public class IntArrayGrowable implements Serializable {

	private static final long serialVersionUID = -8273618619192656814L;
	// the chunks
	private int[][] data;
	// position to put the next element
	private int offsetI, offsetJ;
	// number of elements stored in data array
	private int numElements;
	// number of integers per chunk
	private final int chunkSize;
	static Runtime r = Runtime.getRuntime();

	public IntArrayGrowable(int chunkSize) {
		this.chunkSize = chunkSize;
		data = new int[1][chunkSize];
		offsetI = 0;
		offsetJ = 0;
		numElements = 0;
	}

	public void add(int val) {
		if (offsetJ == chunkSize) {
			addChunk();
			offsetI++;
			offsetJ = 0;
		}
		data[offsetI][offsetJ++] = val;
		numElements++;
	}

	public int get(int idx) {
		int i = idx / chunkSize;
		int j = idx % chunkSize;
		return data[i][j];
	}

	public void set(int idx, int val) {
		int i = idx / chunkSize;
		int j = idx % chunkSize;
		data[i][j] = val;
	}

	public int size() {
		return numElements;
	}

	private void addChunk() {
		// System.out.println("addChunk "+data.length + " " + numElements);
		// System.out.println("addChunk " + data.length + " " + numElements);
		// System.out.println(r.maxMemory() + " - " + r.freeMemory() + " - " + r.totalMemory()
		// + " used: " + (r.totalMemory() - r.freeMemory()));

		int[][] tmp = new int[data.length + 1][];
		for (int i = 0; i < data.length; i++) {
			tmp[i] = data[i];
		}
		tmp[tmp.length - 1] = new int[chunkSize];
		data = tmp;
	}

}
