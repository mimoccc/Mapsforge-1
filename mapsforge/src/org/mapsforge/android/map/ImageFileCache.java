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
	/**
	 * The load factor of the internal HashMap.
	 */
	private static final float LOAD_FACTOR = 0.6f;
	private final ByteBuffer bitmapBuffer;
	private int capacity;
	private FileInputStream fileInputStream;
	private FileOutputStream fileOutputStream;
	private File imageFile;
	private LinkedHashMap<Tile, File> map;
	private final File tempDir;

	/**
	 * Constructs an image file cache with a fixes size and LRU policy.
	 * 
	 * @param tempDir
	 *            the temporary directory to use for cached files.
	 * @param capacity
	 *            the maximum number of entries in the cache.
	 * @throws IllegalArgumentException
	 *             if the capacity is negative.
	 */
	ImageFileCache(String tempDir, int capacity) {
		if (capacity < 0) {
			throw new IllegalArgumentException();
		}
		this.tempDir = new File(tempDir);
		// check if the cache directory exists
		if (!this.tempDir.exists()) {
			// check if the cache directory could be created
			if (this.tempDir.mkdirs()) {
				this.capacity = capacity;
			} else {
				this.capacity = 0;
			}
		} else if (!this.tempDir.isDirectory() || !this.tempDir.canRead()
				|| !this.tempDir.canWrite()) {
			this.capacity = 0;
		} else {
			this.capacity = capacity;
		}

		this.map = createMap(this.capacity);
		this.bitmapBuffer = ByteBuffer.allocate(Tile.TILE_SIZE_IN_BYTES);
	}

	private LinkedHashMap<Tile, File> createMap(final int initialCapacity) {
		return new LinkedHashMap<Tile, File>((int) (initialCapacity / LOAD_FACTOR) + 2,
				LOAD_FACTOR, true) {
			private static final long serialVersionUID = 1L;

			@Override
			protected boolean removeEldestEntry(Map.Entry<Tile, File> eldest) {
				if (size() > initialCapacity) {
					// remove the entry from the cache and delete the cached file
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
		if (this.map != null) {
			for (File file : this.map.values()) {
				if (!file.delete()) {
					file.deleteOnExit();
				}
			}
			this.map.clear();
			this.map = null;
		}
		// delete the cache directory
		if (this.tempDir != null) {
			if (!this.tempDir.delete()) {
				this.tempDir.deleteOnExit();
			}
		}
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
				this.imageFile = new File(this.tempDir, tile.zoomLevel + "_" + tile.x + "_"
						+ tile.y + ".tile");
				this.fileOutputStream = new FileOutputStream(this.imageFile, false);
				this.fileOutputStream.write(this.bitmapBuffer.array(), 0, this.bitmapBuffer
						.array().length);
				this.fileOutputStream.close();
				this.map.put(tile, this.imageFile);
			} catch (IOException e) {
				Logger.e(e);
			}
		}
	}

	/**
	 * Adjusts the capacity of the cache.
	 * 
	 * @param capacity
	 *            the new capacity of the cache.
	 */
	synchronized void setCapacity(int capacity) {
		this.capacity = capacity;
		// create a new map with the new capacity
		LinkedHashMap<Tile, File> newMap = createMap(this.capacity);

		// put all entries from the old map in the new one.
		for (Map.Entry<Tile, File> entry : this.map.entrySet()) {
			newMap.put(entry.getKey(), entry.getValue());
		}
		this.map = newMap;
	}
}