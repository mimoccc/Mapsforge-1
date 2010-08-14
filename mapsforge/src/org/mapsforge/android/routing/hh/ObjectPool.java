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

import java.util.ArrayList;

final class ObjectPool<T> {

	public static interface PoolableFactory<T> {

		public T makeObject();

	}

	private final ArrayList<T> objects;
	private final PoolableFactory<T> factory;
	private int numBorrowed;

	public ObjectPool(PoolableFactory<T> factory, int initialSize) {
		this.objects = new ArrayList<T>(initialSize);
		this.factory = factory;
		this.numBorrowed = 0;
		for (int i = 0; i < initialSize; i++) {
			objects.add(factory.makeObject());
		}
	}

	public T borrow() {
		T obj;
		if (objects.size() == 0) {
			obj = factory.makeObject();
		} else {
			obj = objects.remove(objects.size() - 1);
		}
		numBorrowed++;
		return obj;
	}

	public void release(T obj) {
		if (obj != null) {
			objects.add(obj);
			numBorrowed--;
		}
	}

	public void clear() {
		this.objects.clear();
		this.numBorrowed = 0;
	}

	public int numBorrowed() {
		return numBorrowed;
	}

	public int numReleased() {
		return objects.size();
	}
}
