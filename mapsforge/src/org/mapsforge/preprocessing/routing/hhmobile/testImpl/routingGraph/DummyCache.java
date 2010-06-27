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

import gnu.trove.map.hash.TIntObjectHashMap;

public class DummyCache implements ICache {

	private TIntObjectHashMap<Block> map;

	public DummyCache() {
		this.map = new TIntObjectHashMap<Block>();
	}

	@Override
	public Block getBlock(int blockId) {
		return map.get(blockId);
	}

	@Override
	public void putBlock(Block block) {
		map.put(block.getBlockId(), block);
	}
}
