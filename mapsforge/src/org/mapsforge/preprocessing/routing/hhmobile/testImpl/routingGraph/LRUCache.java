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
package org.mapsforge.preprocessing.routing.hhmobile.testImpl.routingGraph;

import gnu.trove.list.TLinkable;
import gnu.trove.list.linked.TLinkedList;
import gnu.trove.map.hash.TIntObjectHashMap;

public class LRUCache implements ICache {

	private final TLinkedList<CacheItem> list;
	private final TIntObjectHashMap<CacheItem> map;
	private int sizeBytesThreshold, sizeBytes;

	public LRUCache(int sizeBytesThreshold) {
		this.list = new TLinkedList<CacheItem>();
		this.map = new TIntObjectHashMap<CacheItem>();
		this.sizeBytesThreshold = sizeBytesThreshold;
		this.sizeBytes = 0;
	}

	@Override
	public void clear() {
		this.list.clear();
		this.map.clear();
		this.sizeBytes = 0;
	}

	@Override
	public Block getBlock(int blockId) {
		CacheItem item = map.get(blockId);
		if (item != null) {
			list.remove(item);
			list.addFirst(item);
			return item.block;
		}
		return null;
	}

	@Override
	public void putBlock(Block block) {
		CacheItem item = new CacheItem(block);
		list.addFirst(item);
		map.put(block.getBlockId(), item);
		sizeBytes += block.getSizeBytes();
		while (sizeBytes > sizeBytesThreshold) {
			CacheItem last = list.removeLast();
			map.remove(last.block.getBlockId());
			sizeBytes -= last.block.getSizeBytes();
		}
		// System.out.println(sizeBytes);
	}

	private static class CacheItem implements TLinkable<CacheItem> {
		private static final long serialVersionUID = 1L;
		final Block block;
		CacheItem next;
		CacheItem prev;

		public CacheItem(Block block) {
			this.block = block;
			this.next = null;
			this.prev = null;
		}

		@Override
		public CacheItem getNext() {
			return this.next;
		}

		@Override
		public CacheItem getPrevious() {
			return this.prev;
		}

		@Override
		public void setNext(CacheItem next) {
			this.next = next;
		}

		@Override
		public void setPrevious(CacheItem prev) {
			this.prev = prev;
		}
	}
}
