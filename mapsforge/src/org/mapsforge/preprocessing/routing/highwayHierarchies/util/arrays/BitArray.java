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
public class BitArray implements Serializable {

	private static final long serialVersionUID = 5879036119351268170L;

	protected static final int BITS = 32;
	protected final int[] data;
	protected final int size;

	public BitArray(int size) {
		this.size = size;
		int len = size / 32;
		if (size % 32 != 0) {
			len++;
		}
		data = new int[len];
	}

	public void clear(int i) {
		data[i / BITS] &= ~(1 << (i % BITS));
	}

	public boolean get(int i) {
		return (data[i / BITS] & (1 << (i % BITS))) != 0;
	}

	public void set(int i, boolean b) {
		if (b) {
			set(i);
		} else {
			clear(i);
		}
	}

	public void set(int i) {
		data[i / BITS] |= (1 << (i % BITS));
	}

	public int size() {
		return size;
	}
}
