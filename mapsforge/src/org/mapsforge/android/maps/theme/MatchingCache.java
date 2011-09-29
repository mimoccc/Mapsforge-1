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
package org.mapsforge.android.maps.theme;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A cache for matching tags with a fixed size and LRU policy.
 * <p>
 * This class is not thread-safe. Each thread should use its own instance.
 */
class MatchingCache {
	/**
	 * Load factor of the internal HashMap.
	 */
	private static final float LOAD_FACTOR = 0.6f;

	private final Map<List<Tag>, List<RenderInstruction>> map;

	MatchingCache(int capacity) {
		if (capacity < 0) {
			throw new IllegalArgumentException("capacity must not be negative: " + capacity);
		}
		this.map = createMap(capacity);
	}

	private Map<List<Tag>, List<RenderInstruction>> createMap(final int initialCapacity) {
		return new LinkedHashMap<List<Tag>, List<RenderInstruction>>(
				(int) (initialCapacity / LOAD_FACTOR) + 2, LOAD_FACTOR, true) {
			private static final long serialVersionUID = 1L;

			@Override
			protected boolean removeEldestEntry(Map.Entry<List<Tag>, List<RenderInstruction>> eldest) {
				return size() > initialCapacity;
			}
		};
	}

	void destroy() {
		this.map.clear();
	}

	List<RenderInstruction> get(List<Tag> tags) {
		return this.map.get(tags);
	}

	void put(List<Tag> tags, List<RenderInstruction> matchingList) {
		this.map.put(tags, matchingList);
	}
}