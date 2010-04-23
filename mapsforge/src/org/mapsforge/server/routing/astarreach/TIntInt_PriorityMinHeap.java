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
package org.mapsforge.server.routing.astarreach;

import gnu.trove.TIntHashSet;

/**
 * rudimentally inspired by binary heap implementation of Lars Kuhnt in "BinaryHeapArray". <br/>
 * a similar implementation (including nice comments) can be found at"http://www.java-tips.org/java-se-tips/java.lang/priority-queue-binary-heap-implementation-in-3.html"
 * <br/>
 * additionally a look at the (english) wikipedia.org is recommendable ->
 * "http://en.wikipedia.org/wiki/Binary_heap"
 * 
 * @author Till Stolzenhain
 * 
 */
public class TIntInt_PriorityMinHeap {
	private final TIntHashSet elements;
	private int[] heap;
	private int maxIdx = 0;

	// a float array is used to get the priorities for all elements.
	// (I don't like the mutability, but there's no faster implementation as far
	// as I know the java language)
	private int[] prio;

	public TIntInt_PriorityMinHeap(int initialCapacity, int[] priorities) {
		this.prio = priorities;
		this.heap = new int[initialCapacity];
		this.elements = new TIntHashSet(initialCapacity);
	}

	public boolean add(int newElem) {
		// usedSize++;
		if (++this.maxIdx == this.heap.length) {
			// make new array with doubled length
			int[] newHeap = new int[2 * this.heap.length];
			for (int i = 0; i < this.heap.length; i++)
				newHeap[i] = this.heap[i];
			this.heap = newHeap;
		}
		this.heap[this.maxIdx] = newElem;
		percolateUp(this.maxIdx);
		// insertion of element finished...
		this.elements.add(newElem);
		return true;
	}

	public void clear() {
		this.elements.clear();
		this.maxIdx = 0;
	}

	public boolean contains(int elem) {
		return this.elements.contains(elem);
	}

	public boolean isEmpty() {
		return this.maxIdx == 0;
	}

	public int poll() {
		int min = this.heap[1];
		this.heap[1] = this.heap[this.maxIdx--];
		percolateDown(1);
		this.elements.remove(min);
		return min;
	}

	public boolean remove(int obsoleteElem) {
		// find element first
		for (int pos = 0; pos < this.maxIdx; pos++) {
			if (this.heap[pos] == obsoleteElem) {
				// if it's found: replace it by the last element and percolate
				// that one down ...
				this.heap[pos] = this.heap[this.maxIdx--];
				percolateDown(pos);
				this.elements.remove(obsoleteElem);
				return true;
			}
		}
		return false;
	}

	/**
	 * let the value at position "pos" percolate down in the heap
	 * 
	 * @param pos
	 *            the position of the element to be percolated downwards
	 */
	private void percolateDown(int pos) {
		// the position 0 always holds the last element percolated, this makes
		// the loop implementation much easier
		this.heap[0] = this.heap[pos];
		int childPos;
		while ((childPos = 2 * pos) <= this.maxIdx) {
			if (childPos != this.maxIdx
					&& this.prio[this.heap[childPos + 1]] < this.prio[this.heap[childPos]])
				childPos++;
			if (this.prio[this.heap[childPos]] < this.prio[this.heap[0]])
				this.heap[pos] = this.heap[childPos];
			else
				break;
			pos = childPos;
		}
		this.heap[pos] = this.heap[0];
	}

	/**
	 * let the value at position "pos" percolate down in the heap
	 * 
	 * @param pos
	 *            the position of the element to be percolated downwards
	 */
	private void percolateUp(int pos) {
		// the position 0 always holds the last element percolated, this makes
		// the loop implementation much easier
		this.heap[0] = this.heap[pos];
		int parent;
		while (this.prio[this.heap[0]] < this.prio[this.heap[parent = pos / 2]]) {
			this.heap[pos] = this.heap[parent];
			pos = parent;
		}
		this.heap[pos] = this.heap[0];
	}
}
