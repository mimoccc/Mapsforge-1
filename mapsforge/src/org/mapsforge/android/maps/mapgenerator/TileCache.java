/*
 * Copyright 2010, 2011 mapsforge.org
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.mapsforge.android.maps.mapgenerator;

import android.graphics.Bitmap;

/**
 * Abstract base class for tile image caches with a fixed size and LRU policy.
 */
public abstract class TileCache {
	/**
	 * Load factor of the internal HashMap.
	 */
	static final float LOAD_FACTOR = 0.6f;

	final int cacheCapacity;

	/**
	 * Constructs a new cache with a fixed size and LRU policy.
	 * 
	 * @param cacheCapacity
	 *            the maximum number of tile images in this cache.
	 * @throws IllegalArgumentException
	 *             if the cache capacity is negative.
	 */
	TileCache(int cacheCapacity) {
		if (cacheCapacity < 0) {
			throw new IllegalArgumentException("capacity must not be negative: " + cacheCapacity);
		}

		this.cacheCapacity = cacheCapacity;
	}

	/**
	 * @param mapGeneratorJob
	 *            the key of the image.
	 * @return true if this cache contains a tile image for the given key, false otherwise.
	 */
	public abstract boolean containsKey(MapGeneratorJob mapGeneratorJob);

	/**
	 * Destroys this cache.
	 */
	public abstract void destroy();

	/**
	 * @param mapGeneratorJob
	 *            the key of the tile image.
	 * @return the tile image for the given key or null, if this cache contains no tile image for the key.
	 */
	public abstract Bitmap get(MapGeneratorJob mapGeneratorJob);

	/**
	 * @return true if this cache is persistent, false otherwise.
	 */
	public abstract boolean isPersistent();

	/**
	 * Adds another tile image to this cache.
	 * 
	 * @param mapGeneratorJob
	 *            the key of the tile image.
	 * @param bitmap
	 *            the tile image.
	 */
	public abstract void put(MapGeneratorJob mapGeneratorJob, Bitmap bitmap);

	/**
	 * Sets the persistence of this cache.
	 * 
	 * @param persistent
	 *            the new persistence of this cache.
	 * @throws UnsupportedOperationException
	 *             if this cache does not support persistence.
	 */
	public abstract void setPersistent(boolean persistent);
}
