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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;

import android.graphics.Bitmap;

/**
 * A cache for image files with a fixed size and LRU policy.
 */
class ImageFileCache {
	private static final float LOAD_FACTOR = 0.6f;
	private final ByteBuffer bitmapBuffer;
	private final int capacity;
	private FileInputStream fileInputStream;
	private FileOutputStream fileOutputStream;
	private File imageFile;
	private final LinkedHashMap<Tile, File> map;
	private final String tempDir;

	/**
	 * Constructs a cache with a fixes size and LRU policy.
	 * 
	 * @param tempDir
	 *            the temporary directory to use for cached images.
	 * @param capacity
	 *            the maximum number of entries in the cache.
	 */
	ImageFileCache(String tempDir, int capacity) {
		this.tempDir = tempDir;
		this.capacity = capacity;
		this.map = createMap(this.capacity);
		this.bitmapBuffer = ByteBuffer.allocate(Tile.TILE_SIZE * Tile.TILE_SIZE
				* Tile.TILE_BYTES_PER_PIXEL);
	}

	/**
	 * Constructs a new cache from an old cache with a fixes size and LRU policy. The entries
	 * from the old cache are copied into the new cache and the old cache is cleared.
	 * 
	 * @param tempDir
	 *            the temporary directory to use for cached images.
	 * @param capacity
	 *            the maximum number of entries in the cache.
	 * @param oldImageFileCache
	 *            the old image cache.
	 */
	ImageFileCache(String tempDir, int capacity, ImageFileCache oldImageFileCache) {
		this.tempDir = tempDir;
		this.capacity = capacity;
		this.map = createMap(this.capacity);
		this.bitmapBuffer = ByteBuffer.allocate(Tile.TILE_SIZE * Tile.TILE_SIZE
				* Tile.TILE_BYTES_PER_PIXEL);
		if (this.capacity >= oldImageFileCache.capacity) {
			// put all entries from the old cache in the new one
			this.map.putAll(oldImageFileCache.map);
		} else {
			// put all entries from the old cache in the new one. The
			// replacement policy will only keep the last files.
			for (Map.Entry<Tile, File> entry : oldImageFileCache.map.entrySet()) {
				this.map.put(entry.getKey(), entry.getValue());
			}
		}
		oldImageFileCache.map.clear();
	}

	private LinkedHashMap<Tile, File> createMap(final int initialCapacity) {
		return new LinkedHashMap<Tile, File>((int) (initialCapacity / LOAD_FACTOR) + 2,
				LOAD_FACTOR, true) {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean removeEldestEntry(Map.Entry<Tile, File> eldest) {
				if (size() > initialCapacity) {
					// remove the entry from the cache and delete the cached
					// file
					this.remove(eldest.getKey());
					if (!eldest.getValue().delete()) {
						eldest.getValue().deleteOnExit();
					}
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
		// delete all cached files
		for (File file : this.map.values()) {
			if (!file.delete()) {
				file.deleteOnExit();
			}
		}
		this.map.clear();
		this.fileInputStream = null;
		this.fileOutputStream = null;
		this.imageFile = null;
	}

	synchronized void get(Tile tile, ByteBuffer buffer) {
		try {
			this.fileInputStream = new FileInputStream(this.map.get(tile));
			if (this.fileInputStream.read(buffer.array()) == buffer.array().length) {
				// the complete bitmap has been read successfully
				buffer.rewind();
			}
			this.fileInputStream.close();
		} catch (IOException e) {
			Logger.e(e);
		}
	}

	synchronized void put(Tile tile, Bitmap bitmap) {
		if (this.capacity > 0) {
			// write the image to a temporary file
			try {
				bitmap.copyPixelsToBuffer(this.bitmapBuffer);
				this.bitmapBuffer.rewind();
				this.imageFile = new File(this.tempDir, tile.x + "_" + tile.y + "."
						+ tile.zoomLevel);
				this.fileOutputStream = new FileOutputStream(this.imageFile);
				this.fileOutputStream.write(this.bitmapBuffer.array(), 0, this.bitmapBuffer
						.array().length);
				this.fileOutputStream.close();
				this.map.put(tile, this.imageFile);
			} catch (IOException e) {
				Logger.e(e);
			}
		}
	}
}