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
class DatabaseIndexCacheNew {
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
	private final long indexStart;
	private RandomAccessFile inputFile;
	private final long inputFileBlocks;
	private LinkedHashMap<Long, byte[]> map;

	/**
	 * Constructs an database index cache with a fixes size and LRU policy.
	 * 
	 * @param inputFile
	 *            the map file from which the index should be read and cached.
	 * @param inputFileBlocks
	 *            the total number of blocks in the map file index.
	 * @param indexStart
	 *            the offset in the map file at which the index starts.
	 * @param capacity
	 *            the maximum number of entries in the cache.
	 * @throws IllegalArgumentException
	 *             if the capacity is negative.
	 */
	DatabaseIndexCacheNew(RandomAccessFile inputFile, long inputFileBlocks, long indexStart,
			int capacity) {
		if (capacity < 0) {
			throw new IllegalArgumentException();
		}
		this.inputFile = inputFile;
		this.inputFileBlocks = inputFileBlocks;
		this.indexStart = indexStart;
		this.cacheCapacity = capacity;
		this.map = createMap(this.cacheCapacity);
	}

	private LinkedHashMap<Long, byte[]> createMap(final int initialCapacity) {
		return new LinkedHashMap<Long, byte[]>((int) (initialCapacity / LOAD_FACTOR) + 2,
				LOAD_FACTOR, true) {
			private static final long serialVersionUID = 1L;

			@Override
			protected boolean removeEldestEntry(Map.Entry<Long, byte[]> eldest) {
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
	 * Returns the real address of a block in the map file. If the required block address is not
	 * cached, it will be read from the map file index and put in the cache.
	 * 
	 * @param blockNumber
	 *            the number of the block.
	 * @return the block address or -1 if the block number is invalid.
	 * @throws IOException
	 *             if an error occurs while reading the map file.
	 */
	long getAddress(long blockNumber) throws IOException {
		try {
			if (blockNumber >= this.inputFileBlocks) {
				return -1;
			}
			// calculate the index block number
			this.cacheBlockNumber = blockNumber / INDEX_ENTRIES_PER_CACHE_BLOCK;

			// check for cached index block
			if (this.map.containsKey(Long.valueOf(this.cacheBlockNumber))) {
				// cache hit, read the index block from the map
				this.cacheBlock = this.map.get(Long.valueOf(this.cacheBlockNumber));
			} else {
				// cache miss, create a new index block
				this.cacheBlock = new byte[SIZE_OF_CACHE_BLOCK];
				// seek to the correct index block in the file and read it
				this.inputFile.seek(this.indexStart + this.cacheBlockNumber
						* SIZE_OF_CACHE_BLOCK);
				if (this.inputFile.read(this.cacheBlock, 0, SIZE_OF_CACHE_BLOCK) != SIZE_OF_CACHE_BLOCK) {
					throw new IOException();
				}
				// put the index block in the map
				this.map.put(Long.valueOf(this.cacheBlockNumber), this.cacheBlock);
			}

			// calculate the address of the index entry inside the index block
			this.addressInCacheBlock = (int) ((blockNumber % INDEX_ENTRIES_PER_CACHE_BLOCK) * BYTES_PER_INDEX_ENTRY);

			// return the real address
			return Deserializer.fiveBytesToLong(this.cacheBlock, this.addressInCacheBlock);
		} catch (IOException e) {
			e.printStackTrace();
			throw e;
		}
	}
}