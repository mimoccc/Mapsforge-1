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

import gnu.trove.list.TLinkable;
import gnu.trove.list.linked.TLinkedList;
import gnu.trove.map.hash.TIntObjectHashMap;

final class LRUCache<I extends CacheItem> implements Cache<I> {

	private final TLinkedList<ListNode<I>> list;
	private final TIntObjectHashMap<ListNode<I>> map;
	private int cacheSizeBytes, currentSizeBytes;
	private int numCacheMisses;
	private int numBytesRead;

	public LRUCache(int cacheSizeBytes) {
		this.list = new TLinkedList<ListNode<I>>();
		this.map = new TIntObjectHashMap<ListNode<I>>();
		this.cacheSizeBytes = cacheSizeBytes;
		this.currentSizeBytes = 0;
		this.numCacheMisses = 0;
		this.numBytesRead = 0;
	}

	@Override
	public void clear() {
		this.list.clear();
		this.map.clear();
		this.currentSizeBytes = 0;
		this.numCacheMisses = 0;
		this.numBytesRead = 0;
	}

	public int getNumCacheMisses() {
		return numCacheMisses;
	}

	public int getNumBytesRead() {
		return numBytesRead;
	}

	@Override
	public I getItem(int id) {
		ListNode<I> ci = map.get(id);
		if (ci != null) {
			list.remove(ci);
			list.addFirst(ci);
			return ci.item;
		}
		numCacheMisses++;
		return null;
	}

	@Override
	public void putItem(I item) {
		ListNode<I> ci = new ListNode<I>(item);
		list.addFirst(ci);
		map.put(item.getId(), ci);
		currentSizeBytes += item.getSizeBytes();
		while (currentSizeBytes > cacheSizeBytes) {
			ListNode<I> last = list.removeLast();
			map.remove(last.item.getId());
			currentSizeBytes -= last.item.getSizeBytes();
		}
		this.numBytesRead += item.getSizeBytes();
	}

	private static class ListNode<I> implements TLinkable<ListNode<I>> {
		private static final long serialVersionUID = 1L;
		final I item;
		ListNode<I> next;
		ListNode<I> prev;

		public ListNode(I item) {
			this.item = item;
			this.next = null;
			this.prev = null;
		}

		@Override
		public ListNode<I> getNext() {
			return this.next;
		}

		@Override
		public ListNode<I> getPrevious() {
			return this.prev;
		}

		@Override
		public void setNext(ListNode<I> next) {
			this.next = next;
		}

		@Override
		public void setPrevious(ListNode<I> prev) {
			this.prev = prev;
		}
	}
}
