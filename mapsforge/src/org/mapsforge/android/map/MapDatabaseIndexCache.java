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

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A cache for database index blocks with a fixed size and LRU policy.
 */
class MapDatabaseIndexCache {
	/**
	 * An immutable container class which is the key for the map of the cache.
	 */
	private class CacheEntryKey {
		private final int hashCode;
		private CacheEntryKey other;
		final long blockNumber;
		final MapFile mapFile;

		CacheEntryKey(MapFile mapFile, long blockNumber) {
			this.mapFile = mapFile;
			this.blockNumber = blockNumber;
			this.hashCode = calculateHashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			} else if (!(obj instanceof CacheEntryKey)) {
				return false;
			} else {
				this.other = (CacheEntryKey) obj;
				if (this.mapFile == null && other.mapFile != null) {
					return false;
				} else if (!this.mapFile.equals(other.mapFile)) {
					return false;
				} else if (this.blockNumber != other.blockNumber) {
					return false;
				}
				return true;
			}
		}

		@Override
		public int hashCode() {
			return this.hashCode;
		}

		/**
		 * Calculates the hash value of this object.
		 * 
		 * @return the hash value of this object.
		 */
		private int calculateHashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((this.mapFile == null) ? 0 : this.mapFile.hashCode());
			result = prime * result + (int) (this.blockNumber ^ (this.blockNumber >>> 32));
			return result;
		}
	}

	/**
	 * Number of bytes a single index entry consists of.
	 */
	private static final byte BYTES_PER_INDEX_ENTRY = 5;

	/**
	 * Number of index entries that one index block consists of.
	 */
	private static final int INDEX_ENTRIES_PER_CACHE_BLOCK = 256;

	/**
	 * The load factor of the internal HashMap.
	 */
	private static final float LOAD_FACTOR = 0.6f;

	/**
	 * The real size in bytes of one index block.
	 */
	private static final int SIZE_OF_CACHE_BLOCK = INDEX_ENTRIES_PER_CACHE_BLOCK
			* BYTES_PER_INDEX_ENTRY;
	private int addressInCacheBlock;
	private byte[] cacheBlock;
	private long cacheBlockNumber;
	private final int cacheCapacity;
	private CacheEntryKey cacheEntryKey;
	private RandomAccessFile inputFile;
	private LinkedHashMap<CacheEntryKey, byte[]> map;

	/**
	 * Constructs an database index cache with a fixes size and LRU policy.
	 * 
	 * @param inputFile
	 *            the map file from which the index should be read and cached.
	 * @param capacity
	 *            the maximum number of entries in the cache.
	 * @throws IllegalArgumentException
	 *             if the capacity is negative.
	 */
	MapDatabaseIndexCache(RandomAccessFile inputFile, int capacity) {
		if (capacity < 0) {
			throw new IllegalArgumentException();
		}
		this.inputFile = inputFile;
		this.cacheCapacity = capacity;
		this.map = createMap(this.cacheCapacity);
	}

	private LinkedHashMap<CacheEntryKey, byte[]> createMap(final int initialCapacity) {
		return new LinkedHashMap<CacheEntryKey, byte[]>(
				(int) (initialCapacity / LOAD_FACTOR) + 2, LOAD_FACTOR, true) {
			private static final long serialVersionUID = 1L;

			@Override
			protected boolean removeEldestEntry(Map.Entry<CacheEntryKey, byte[]> eldest) {
				return size() > initialCapacity;
			}
		};
	}

	/**
	 * Destroy the cache at the end of its lifetime.
	 */
	void destroy() {
		this.inputFile = null;
		if (this.map != null) {
			this.map.clear();
			this.map = null;
		}
	}

	/**
	 * Returns the real address of a block in the given map file. If the required block address
	 * is not cached, it will be read from the correct map file index and put in the cache.
	 * 
	 * @param mapFile
	 *            the map file for which the address is needed.
	 * @param blockNumber
	 *            the number of the block in the map file.
	 * @return the block address or -1 if the block number is invalid.
	 */
	long getAddress(MapFile mapFile, long blockNumber) {
		try {
			// check if the block number is out of bounds
			if (blockNumber >= mapFile.numberOfBlocks) {
				return -1;
			}

			// create the cache entry key for this request
			this.cacheEntryKey = new CacheEntryKey(mapFile, blockNumber);

			// calculate the index block number
			this.cacheBlockNumber = blockNumber / INDEX_ENTRIES_PER_CACHE_BLOCK;

			// check for cached index block
			this.cacheBlock = this.map.get(this.cacheEntryKey);
			if (this.cacheBlock == null) {
				// cache miss, create a new index block
				this.cacheBlock = new byte[SIZE_OF_CACHE_BLOCK];
				// seek to the correct index block in the file and read it
				this.inputFile.seek(mapFile.indexStartAddress + this.cacheBlockNumber
						* SIZE_OF_CACHE_BLOCK);
				if (this.inputFile.read(this.cacheBlock, 0, SIZE_OF_CACHE_BLOCK) != SIZE_OF_CACHE_BLOCK) {
					return -1;
				}
				// put the index block in the map
				this.map.put(this.cacheEntryKey, this.cacheBlock);
			}

			// calculate the address of the index entry inside the index block
			this.addressInCacheBlock = (int) ((blockNumber % INDEX_ENTRIES_PER_CACHE_BLOCK) * BYTES_PER_INDEX_ENTRY);

			// return the real address
			return Deserializer.fiveBytesToLong(this.cacheBlock, this.addressInCacheBlock);
		} catch (IOException e) {
			Logger.e(e);
			return -1;
		}
	}
}