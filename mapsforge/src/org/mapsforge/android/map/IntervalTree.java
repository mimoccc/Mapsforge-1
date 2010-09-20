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

package org.mapsforge.android.map;

import java.util.LinkedList;

class IntervalTree {

	private Node root; // root of the BST

	// BST helper node data type
	private class Node {
		Interval<?> interval; // key

		Node left, right; // left and right subtrees
		int N; // size of subtree rooted at this node
		int max; // max endpoint in subtree rooted at this node

		Node(Interval<?> interval) {
			this.interval = interval;

			this.N = 1;
			this.max = interval.high;
		}
	}

	/*
	 * BST search
	 */

	public boolean contains(Interval<?> interval) {
		return (get(interval) != null);
	}

	// return value associated with the given key
	// if no such value, return null
	public Interval<?> get(Interval<?> interval) {
		return get(root, interval);
	}

	private Interval<?> get(Node x, Interval<?> interval) {
		if (x == null)
			return null;
		if (eq(interval, x.interval))
			return x.interval;
		if (less(interval, x.interval))
			return get(x.left, interval);

		return get(x.right, interval);
	}

	/*
	 * randomized insertion
	 */
	public void put(Interval<?> interval) {
		if (contains(interval)) {
			System.out.println("duplicate");
			remove(interval);
		}
		root = randomizedInsert(root, interval);
	}

	// make new node the root with uniform probability
	private Node randomizedInsert(Node x, Interval<?> interval) {
		if (x == null)
			return new Node(interval);
		if (Math.random() * size(x) < 1.0)
			return rootInsert(x, interval);
		if (less(interval, x.interval))
			x.left = randomizedInsert(x.left, interval);
		else
			x.right = randomizedInsert(x.right, interval);
		fix(x);
		return x;
	}

	private Node rootInsert(Node x, Interval<?> interval) {
		if (x == null)
			return new Node(interval);
		if (less(interval, x.interval)) {
			x.left = rootInsert(x.left, interval);
			x = rotR(x);
		} else {
			x.right = rootInsert(x.right, interval);
			x = rotL(x);
		}
		return x;
	}

	/*
	 * deletion
	 */
	private Node joinLR(Node a, Node b) {
		if (a == null)
			return b;
		if (b == null)
			return a;

		if (Math.random() * (size(a) + size(b)) < size(a)) {
			a.right = joinLR(a.right, b);
			fix(a);
			return a;
		}

		b.left = joinLR(a, b.left);
		fix(b);
		return b;
	}

	// remove and return value associated with given interval;
	// if no such interval exists return null
	public Interval<?> remove(Interval<?> interval) {
		Interval<?> value = get(interval);
		root = remove(root, interval);
		return value;
	}

	private Node remove(Node h, Interval<?> interval) {
		if (h == null)
			return null;
		if (less(interval, h.interval))
			h.left = remove(h.left, interval);
		if (less(h.interval, interval))
			h.right = remove(h.right, interval);
		if (eq(interval, h.interval))
			h = joinLR(h.left, h.right);
		fix(h);
		return h;
	}

	/*
	 * Interval searching
	 */

	// return an interval in data structure that intersects the given inteval;
	// return null if no such interval exists
	public Interval<?> search(Interval<?> interval) {
		return search(root, interval);
	}

	// look in subtree rooted at x
	public Interval<?> search(Node x, Interval<?> interval) {
		while (x != null) {
			if (interval.intersects(x.interval))
				return x.interval;
			else if (x.left == null)
				x = x.right;
			else if (x.left.max < interval.low)
				x = x.right;
			else
				x = x.left;
		}
		return null;
	}

	// return *all* intervals in data structure that intersect the given interval
	public Iterable<Interval<?>> searchAll(Interval<?> interval) {
		LinkedList<Interval<?>> list = new LinkedList<Interval<?>>();
		searchAll(root, interval, list);
		return list;
	}

	// look in subtree rooted at x
	public boolean searchAll(Node x, Interval<?> interval, LinkedList<Interval<?>> list) {
		boolean found1 = false;
		boolean found2 = false;
		boolean found3 = false;
		if (x == null)
			return false;
		if (interval.intersects(x.interval)) {
			list.add(x.interval);
			found1 = true;
		}
		if (x.left != null && x.left.max >= interval.low)
			found2 = searchAll(x.left, interval, list);
		if (found2 || x.left == null || x.left.max < interval.low)
			found3 = searchAll(x.right, interval, list);
		return found1 || found2 || found3;
	}

	// return number of nodes in subtree rooted at x
	public int size() {
		return size(root);
	}

	private int size(Node x) {
		if (x == null)
			return 0;

		return x.N;
	}

	// height of tree (empty tree height = 0)
	public int height() {
		return height(root);
	}

	private int height(Node x) {
		if (x == null)
			return 0;
		return 1 + Math.max(height(x.left), height(x.right));
	}

	// fix auxilliar information (subtree count and max fields)
	private void fix(Node x) {
		if (x == null)
			return;
		x.N = 1 + size(x.left) + size(x.right);
		x.max = max3(x.interval.high, max(x.left), max(x.right));
	}

	private int max(Node x) {
		if (x == null)
			return Integer.MIN_VALUE;
		return x.max;
	}

	// precondition: a is not null
	private int max3(int a, int b, int c) {
		return Math.max(a, Math.max(b, c));
	}

	// right rotate
	private Node rotR(Node h) {
		Node x = h.left;
		h.left = x.right;
		x.right = h;
		fix(h);
		fix(x);
		return x;
	}

	// left rotate
	private Node rotL(Node h) {
		Node x = h.right;
		h.right = x.left;
		x.left = h;
		fix(h);
		fix(x);
		return x;
	}

	// check integrity of subtree count fields
	public boolean check() {
		return checkCount() && checkMax();
	}

	// check integrity of count fields
	private boolean checkCount() {
		return checkCount(root);
	}

	private boolean checkCount(Node x) {
		if (x == null)
			return true;
		return checkCount(x.left) && checkCount(x.right)
				&& (x.N == 1 + size(x.left) + size(x.right));
	}

	private boolean checkMax() {
		return checkMax(root);
	}

	private boolean checkMax(Node x) {
		if (x == null)
			return true;
		return x.max == max3(x.interval.high, max(x.left), max(x.right));
	}

	// is left endpoint of a less than left endpoint of a?
	// break ties with right endpoint
	private boolean less(Interval<?> a, Interval<?> b) {
		if (a.low < b.low)
			return true;
		else if (a.low > b.low)
			return false;
		else if (a.high < b.high)
			return true;
		else
			return false;
	}

	private boolean eq(Interval<?> a, Interval<?> b) {
		return (a.low == b.low) && (a.high == b.high);
	}

}
