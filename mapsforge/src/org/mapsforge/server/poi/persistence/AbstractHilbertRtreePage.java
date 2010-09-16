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
package org.mapsforge.server.poi.persistence;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Stack;

import org.garret.perst.Assert;
import org.garret.perst.Link;
import org.garret.perst.Persistent;
import org.garret.perst.Storage;

abstract class AbstractHilbertRtreePage<T, S extends SpatialShape<S>> extends Persistent
		implements RtreeIndexPage<T, S> {

	static final int cooperatingSiblings = 2;

	class HilbertIterator implements Iterator<T> {

		private Stack<Iterator<Entry<S>>> iteratorStack;
		private int height = 0;

		public HilbertIterator(AbstractHilbertRtreePage<T, S> root, int height) {
			super();
			this.iteratorStack = new Stack<Iterator<Entry<S>>>();
			this.height = height;
			iteratorStack.push(root.getEntryList().iterator());
			downToLeaf();
		}

		@Override
		public boolean hasNext() {
			while (!iteratorStack.empty() && !iteratorStack.peek().hasNext()) {
				iteratorStack.pop();
			}
			if (iteratorStack.empty()) {
				return false;
			}
			return iteratorStack.peek().hasNext();
		}

		@Override
		public T next() {
			while (!iteratorStack.empty() && !iteratorStack.peek().hasNext()) {
				iteratorStack.pop();
			}
			if (iteratorStack.empty()) {
				throw new NoSuchElementException();
			}

			downToLeaf();

			return (T) iteratorStack.peek().next().item;
		}

		private void downToLeaf() {
			while (currentLevel() != 0) {
				AbstractHilbertRtreePage<T, S> page = (AbstractHilbertRtreePage<T, S>) iteratorStack
						.peek().next().item;
				iteratorStack.push(page.getEntryList().iterator());
			}
		}

		private int currentLevel() {
			return height - iteratorStack.size();
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}

	}

	private static class Entry<S extends SpatialShape<S>> implements Comparable<Entry<S>> {
		final Object item;
		final S shape;
		final long value;

		Entry(Object item, S shape) {
			super();
			this.item = item;
			this.shape = shape;
			value = shape.linearOderValue();
		}

		@Override
		public int compareTo(Entry<S> other) {
			long result = value - other.value;
			if (result < 0)
				return -1;
			if (result > 0)
				return 1;
			return 0;
		}
	}

	private static <E, R extends SpatialShape<R>> boolean isOverflowing(
			ArrayList<AbstractHilbertRtreePage<E, R>> pages) {
		boolean overflowing = true;
		AbstractHilbertRtreePage<E, R> page = null;
		for (int i = 0; i < pages.size() && overflowing; i++) {
			page = pages.get(i);
			if (page.n < page.capacity()) {
				overflowing = false;
			}
		}
		return overflowing;
	}

	private static <E, R extends SpatialShape<R>> boolean isUnderflowing(
			ArrayList<AbstractHilbertRtreePage<E, R>> pages) {
		int numberOfEntrys = 0;
		for (AbstractHilbertRtreePage<E, R> page : pages) {
			numberOfEntrys += page.n;
		}

		return (numberOfEntrys < pages.get(0).minFill() * (pages.size()));
	}

	private static <E, R extends SpatialShape<R>> void distributeElements(
			ArrayList<AbstractHilbertRtreePage<E, R>> pages, ArrayList<Entry<R>> additionalItems) {
		ArrayList<Entry<R>> children = collectChildren(pages);

		if (additionalItems != null) {
			children.addAll(additionalItems);
			Collections.sort(children);
		}

		Assert.that(children.size() <= (pages.size() * pages.get(0).capacity()));

		int rest = children.size() % pages.size();
		int offset = 0;
		int step = children.size() / pages.size();

		for (int i = 0; i < rest; i++) {
			pages.get(i).replaceChildren(children.subList(offset, offset + step + 1));
			offset += (step + 1);
		}
		for (int i = rest; i < pages.size(); i++) {
			pages.get(i).replaceChildren(children.subList(offset, offset + step));
			offset += (step);
		}
	}

	private static <E, R extends SpatialShape<R>> ArrayList<Entry<R>> collectChildren(
			ArrayList<AbstractHilbertRtreePage<E, R>> pages) {
		ArrayList<Entry<R>> result = new ArrayList<Entry<R>>();
		for (AbstractHilbertRtreePage<E, R> page : pages) {
			result.addAll(page.getEntryList());
		}
		Collections.sort(result);
		return result;
	}

	int n = 0;
	Link<Object> branch;
	AbstractHilbertRtreePage<T, S> parent;
	long largestHilbertValue = 0;

	AbstractHilbertRtreePage() {
		// required by perst
	}

	AbstractHilbertRtreePage(Storage storage, Object[] objs, S[] shapes) {
		Assert.that(objs.length <= capacity());
		Assert.that(objs.length == shapes.length);
		initialize(storage);

		for (int i = 0; i < objs.length; i++) {
			Assert.that(objs[i] != null);
			Assert.that(shapes[i] != null);
			setBranch(i, shapes[i], objs[i]);
		}

		n = objs.length;
	}

	AbstractHilbertRtreePage(Storage storage, Object obj, S shape) {
		initialize(storage);
		setBranch(0, shape, obj);
		n = 1;
		largestHilbertValue = shape.linearOderValue();
	}

	AbstractHilbertRtreePage(Storage storage) {
		initialize(storage);
	}

	AbstractHilbertRtreePage(Storage storage, AbstractHilbertRtreePage<T, S> root) {
		initialize(storage);
		n = 1;
		setBranch(0, root.getMinimalBoundingShape(), root);
		root.parent = this;
		largestHilbertValue = getShape(0).linearOderValue();
	}

	abstract void initialize(Storage storage);

	abstract AbstractHilbertRtreePage<T, S> newRoot(Storage storage,
			AbstractHilbertRtreePage<T, S> oldRoot);

	abstract AbstractHilbertRtreePage<T, S> newNode(Storage storage,
			AbstractHilbertRtreePage<T, S> parentNode);

	abstract void setShape(int index, S shape);

	abstract S getShape(int index);

	abstract S[] getShapes();

	abstract int capacity();

	private int minFill() {
		return capacity() / 2;
	}

	@Override
	public void find(S shape, ArrayList<T> result, int level) {
		PerstQueryTracer.getInstance().incrementNodes();

		if (--level != 0) { /* this is an internal node in the tree */
			for (int i = 0; i < n; i++) {
				if (shape.intersects(getShape(i))) {
					this.<AbstractHilbertRtreePage<T, S>> getBranch(i).find(shape, result,
							level);
				}
			}
		} else { /* this is a leaf node */
			for (int i = 0; i < n; i++) {
				if (shape.intersects(getShape(i))) {
					result.add(this.<T> getBranch(i));
				}
			}
		}
	}

	@Override
	public RtreeIndexPage<T, S> insert(Storage storage, S shape, T item, int level) {
		modify();
		AbstractHilbertRtreePage<T, S> leaf = chooseLeaf(level, shape.linearOderValue());
		return leaf.put(storage, item, shape);
	}

	private AbstractHilbertRtreePage<T, S> chooseLeaf(int level, long hilbert) {
		if (--level != 0) { /* this is an internal node in the tree */
			for (int i = 0; i < n; i++) {
				if (this.<AbstractHilbertRtreePage<T, S>> getBranch(i).largestHilbertValue > hilbert) {
					return this.<AbstractHilbertRtreePage<T, S>> getBranch(i).chooseLeaf(level,
							hilbert);
				}
			}
			return this.<AbstractHilbertRtreePage<T, S>> getBranch(n - 1).chooseLeaf(level,
					hilbert);
		}
		return this;
	}

	@Override
	public void purge(int level) {
		if (--level != 0) { /* this is an internal node in the tree */
			for (int i = 0; i < n; i++) {
				this.<AbstractHilbertRtreePage<T, S>> getBranch(i).purge(level);
			}
		}
		deallocate();
	}

	@Override
	public RtreeIndexPage<T, S> remove(S shape, T item, int level) {
		AbstractHilbertRtreePage<T, S> leaf = findContainingLeaf(shape, level);
		if (leaf != null) {
			return leaf.removeBranch(leaf.branch.indexOfObject(item));

		}
		return null;
	}

	abstract protected void instantiateShape(int index);

	private <E> E getBranch(final int index) {
		Object result = branch.get(index);
		return (E) result;
	}

	private AbstractHilbertRtreePage<T, S> put(Storage storage, Object item, S shape) {
		if (n < capacity()) {
			ArrayList<Entry<S>> children = getEntryList();
			children.add(new Entry<S>(item, shape));
			Collections.sort(children);
			replaceChildren(children);
			if (parent != null) {
				updateParent();
			}
			return null;
		}
		return distributeOnPut(storage, new Entry<S>(item, shape));
	}

	private AbstractHilbertRtreePage<T, S> distributeOnPut(Storage storage, Entry<S> newEntry) {
		AbstractHilbertRtreePage<T, S> newRoot = null;
		if (parent == null) {
			newRoot = newRoot(storage, this);
			this.parent = newRoot;
		}

		ArrayList<AbstractHilbertRtreePage<T, S>> siblings = parent.getSiblings(parent.branch
				.indexOfObject(this));
		ArrayList<AbstractHilbertRtreePage<T, S>> pages = new ArrayList<AbstractHilbertRtreePage<T, S>>(
				siblings);

		ArrayList<Entry<S>> list = new ArrayList<Entry<S>>(1);
		list.add(newEntry);

		AbstractHilbertRtreePage<T, S> newSibling = null;
		if (isOverflowing(pages)) {
			// create new sibling and add to list of pages to distribute children
			newSibling = newNode(storage, parent);
			pages.add(newSibling);
		}

		distributeElements(pages, list);
		updateParent(storage, siblings, newSibling);

		return newRoot;
	}

	ArrayList<Entry<S>> getEntryList() {
		ArrayList<Entry<S>> result = new ArrayList<Entry<S>>(n);
		for (int i = 0; i < n; i++) {
			result.add(new Entry<S>(branch.get(i), getShape(i)));
		}
		return result;
	}

	/**
	 * @param storage
	 *            the storage this page is stored in.
	 * @param pages
	 *            changed on this level including this.
	 * @param newNode
	 *            the newly created page if split occured.
	 */
	private void updateParent(Storage storage, ArrayList<AbstractHilbertRtreePage<T, S>> pages,
			AbstractHilbertRtreePage<T, S> newNode) {
		if (parent != null) {
			int index = 0;
			for (AbstractHilbertRtreePage<T, S> page : pages) {
				index = parent.branch.indexOfObject(page);
				parent.setShape(index, page.getMinimalBoundingShape());
			}
			parent.replaceChildren(parent.getEntryList());
			if (newNode != null) {
				parent.put(storage, newNode, newNode.getMinimalBoundingShape());
			} else {
				parent.updateParent();
			}
		}
	}

	private void updateParent() {
		if (parent != null) {
			int index = parent.branch.indexOfObject(this);
			parent.setShape(index, this.getMinimalBoundingShape());
			parent.updateParent();
		}
	}

	private void replaceChildren(List<Entry<S>> children) {
		Assert.that(children.size() <= capacity());
		Entry<S> entry = null;
		branch.clear();
		branch.setSize(capacity());
		Class<?> childType = children.get(0).item.getClass();
		for (int i = 0; i < children.size(); i++) {
			entry = children.get(i);
			Assert.that(entry.item.getClass().equals(childType));
			setShape(i, entry.shape);
			branch.set(i, entry.item);
		}

		for (int i = children.size(); i < n; i++) {
			instantiateShape(i);
		}
		n = children.size();
		largestHilbertValue = getShape(n - 1).linearOderValue();

		modify();
	}

	private AbstractHilbertRtreePage<T, S> distributeOnRemove() {
		if (parent != null) { /* inner node */
			int index = parent.branch.indexOfObject(this);
			ArrayList<AbstractHilbertRtreePage<T, S>> pages = parent.getSiblings(index);
			if (isUnderflowing(pages)) {
				pages.remove(this);
				distributeElements(pages, this.getEntryList());
				parent.removeBranch(index);
				this.deallocate();
			} else {
				distributeElements(pages, null);
			}
		} else { /* root node */
			if (n == 1) {
				Object newRoot = getBranch(0);
				if (newRoot instanceof AbstractHilbertRtreePage<?, ?>) {
					this.deallocate();
					return (AbstractHilbertRtreePage<T, S>) newRoot;
				}
			}
		}

		return null;
	}

	private ArrayList<AbstractHilbertRtreePage<T, S>> getSiblings(int index) {
		Assert.that(index < n);
		// find child range for distribution
		int offset = (index - cooperatingSiblings / 2);
		if (offset + cooperatingSiblings > n)
			offset = n - 1 - cooperatingSiblings;
		if (offset < 0)
			offset = 0;
		int max = Math.min(n, offset + cooperatingSiblings + 1);

		Assert.that(offset <= index && index <= max);

		ArrayList<AbstractHilbertRtreePage<T, S>> pages = new ArrayList<AbstractHilbertRtreePage<T, S>>(
				cooperatingSiblings + 1);

		for (int i = offset; i < max; i++) {
			pages.add(this.<AbstractHilbertRtreePage<T, S>> getBranch(i));
		}

		Assert.that(pages.size() > 0);

		return pages;
	}

	private AbstractHilbertRtreePage<T, S> removeBranch(int i) {
		n -= 1;
		S[] shapes = getShapes();
		System.arraycopy(shapes, i + 1, shapes, i, n - i);
		branch.remove(i);
		branch.setSize(capacity());

		AbstractHilbertRtreePage<T, S> newRoot = null;
		if (n < minFill()) {
			newRoot = distributeOnRemove();
		}

		modify();
		return newRoot;
	}

	private void setBranch(int i, S shape, Object obj) {
		setShape(i, shape);
		branch.setObject(i, obj);
	}

	private AbstractHilbertRtreePage<T, S> findContainingLeaf(S shape, int level) {
		if (--level != 0) { /* this is an internal node in the tree */
			for (int i = 0; i < n; i++) {
				if (shape.intersects(getShape(i))) {
					return this.<AbstractHilbertRtreePage<T, S>> getBranch(i)
							.findContainingLeaf(shape, level);
				}
			}
			return null;
		}
		return this;
	}

	@Override
	public Iterator<T> iterator(int level) {
		return new HilbertIterator(this, level);
	}
}
