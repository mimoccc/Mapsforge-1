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

import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

import android.graphics.Bitmap;

/**
 * A cache for bitmap images with a fixed size and LRU policy.
 */
class ImageBitmapCache {
	/**
	 * The load factor of the internal HashMap.
	 */
	private static final float LOAD_FACTOR = 0.6f;
	private final ByteBuffer bitmapBuffer;
	private final int capacity;
	private final LinkedHashMap<Tile, Bitmap> map;
	private Bitmap tempBitmap;
	final LinkedList<Bitmap> bitmapPool;

	/**
	 * Constructs an image bitmap cache with a fixes size and LRU policy.
	 * 
	 * @param capacity
	 *            the maximum number of entries in the cache.
	 * @throws IllegalArgumentException
	 *             if the capacity is negative.
	 */
	ImageBitmapCache(int capacity) {
		if (capacity < 0) {
			throw new IllegalArgumentException();
		}
		this.capacity = capacity;
		this.map = createMap(this.capacity);
		this.bitmapPool = new LinkedList<Bitmap>();
		// one more bitmap than the cache capacity is needed for put operations
		for (int i = 0; i <= this.capacity; ++i) {
			this.bitmapPool.add(Bitmap.createBitmap(Tile.TILE_SIZE, Tile.TILE_SIZE,
					Bitmap.Config.RGB_565));
		}
		this.bitmapBuffer = ByteBuffer.allocate(Tile.TILE_SIZE_IN_BYTES);
	}

	private LinkedHashMap<Tile, Bitmap> createMap(final int initialCapacity) {
		return new LinkedHashMap<Tile, Bitmap>((int) (initialCapacity / LOAD_FACTOR) + 2,
				LOAD_FACTOR, true) {
			private static final long serialVersionUID = 1L;

			@Override
			protected boolean removeEldestEntry(Map.Entry<Tile, Bitmap> eldest) {
				if (size() > initialCapacity) {
					this.remove(eldest.getKey());
					ImageBitmapCache.this.bitmapPool.add(eldest.getValue());
				}
				return false;
			}
		};
	}

	synchronized boolean containsKey(Tile tile) {
		return this.map.containsKey(tile);
	}

	/**
	 * Destroy the cache at the end of its lifetime.
	 */
	synchronized void destroy() {
		for (Bitmap bitmap : this.map.values()) {
			bitmap.recycle();
		}
		for (Bitmap bitmap : this.bitmapPool) {
			bitmap.recycle();
		}
	}

	synchronized Bitmap get(Tile tile) {
		return this.map.get(tile);
	}

	synchronized void put(Tile tile, Bitmap bitmap) {
		if (this.capacity > 0) {
			if (this.map.containsKey(tile)) {
				// query the element to update its LRU status
				this.map.get(tile);
				return;
			}
			bitmap.copyPixelsToBuffer(this.bitmapBuffer);
			this.bitmapBuffer.rewind();
			this.tempBitmap = this.bitmapPool.remove();
			this.tempBitmap.copyPixelsFromBuffer(this.bitmapBuffer);
			this.map.put(tile, this.tempBitmap);
		}
	}
}