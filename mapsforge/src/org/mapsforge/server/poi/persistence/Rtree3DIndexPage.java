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

import org.garret.perst.Storage;
import org.garret.perst.impl.Page;

class Rtree3DIndexPage<T> extends AbstractHilbertRtreePage<T, RtreeBox> {

	static final int capacity = (Page.pageSize - 8 - 4 * 4) / (6 * 4 + 4);

	RtreeBox[] boxes;

	Rtree3DIndexPage(Storage storage, Object[] objs, RtreeBox[] boxes) {
		super(storage, objs, boxes);
	}

	Rtree3DIndexPage(Storage storage, Object obj, RtreeBox box) {
		super(storage, obj, box);
	}

	Rtree3DIndexPage(Storage storage, AbstractHilbertRtreePage<T, RtreeBox> root) {
		super(storage, root);
	}

	Rtree3DIndexPage(Storage storage) {
		super(storage);
	}

	Rtree3DIndexPage() {
		super();
	}

	@Override
	int capacity() {
		return capacity;
	}

	@Override
	RtreeBox getShape(int index) {
		return boxes[index];
	}

	@Override
	RtreeBox[] getShapes() {
		return boxes;
	}

	@Override
	void initialize(Storage storage) {
		branch = storage.createLink(capacity());
		branch.setSize(capacity());
		boxes = new RtreeBox[capacity()];
		for (int i = 0; i < capacity(); i++) {
			boxes[i] = new RtreeBox();
		}
		n = 0;
	}

	@Override
	protected void instantiateShape(int index) {
		boxes[index] = new RtreeBox();
	}

	@Override
	AbstractHilbertRtreePage<T, RtreeBox> newRoot(Storage storage,
			AbstractHilbertRtreePage<T, RtreeBox> oldRoot) {
		return new Rtree3DIndexPage<T>(storage, oldRoot);
	}

	@Override
	AbstractHilbertRtreePage<T, RtreeBox> newNode(Storage storage,
			AbstractHilbertRtreePage<T, RtreeBox> parentNode) {
		Rtree3DIndexPage<T> node = new Rtree3DIndexPage<T>(storage);
		node.parent = parentNode;
		return new Rtree3DIndexPage<T>(storage);
	}

	@Override
	void setShape(int index, RtreeBox shape) {
		boxes[index] = shape;
	}

	@Override
	public RtreeBox getMinimalBoundingShape() {
		RtreeBox box = new RtreeBox(boxes[0]);
		for (int i = 1; i < n; i++) {
			box.join(boxes[i]);
		}
		return box;
	}

}
