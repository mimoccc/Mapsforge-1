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
package org.mapsforge.preprocessing.routing;

import java.util.Comparator;
import java.util.HashMap;
import java.util.PriorityQueue;

public class UpdatablePriorityQueue<T extends Identifieable> extends PriorityQueue<T> {

	private static final long serialVersionUID = 8824225913687045425L;
	private HashMap<Integer, T> map;

	public UpdatablePriorityQueue() {
		super();
		map = new HashMap<Integer, T>();
	}

	public UpdatablePriorityQueue(int initialCapacity, Comparator<? super T> comparator) {
		super(initialCapacity, comparator);
		map = new HashMap<Integer, T>(initialCapacity);
	}

	public UpdatablePriorityQueue(int initialCapacity) {
		super(initialCapacity);
		map = new HashMap<Integer, T>(initialCapacity);
	}

	@Override
	public boolean add(T e) {
		map.put(e.id(), e);
		return super.add(e);
	}

	@Override
	public void clear() {
		super.clear();
		map.clear();
	}

	@Override
	public boolean offer(T e) {
		map.put(e.id(), e);
		return super.offer(e);
	}

	@Override
	public T peek() {
		return super.peek();
	}

	@Override
	public T poll() {
		T t = super.poll();
		map.remove(t.id());
		return t;
	}

	@Override
	public boolean remove(Object o) {
		if (o instanceof Identifieable) {
			Identifieable i = (Identifieable) o;
			map.remove(i).id();
		}
		return super.remove(o);
	}

	public boolean update(T t) {
		super.remove(map.get(t.id()));
		return this.add(t);
	}

	public T get(int id) {
		return map.get(id);
	}

}
