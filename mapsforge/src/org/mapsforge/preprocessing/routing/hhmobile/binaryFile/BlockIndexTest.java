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
package org.mapsforge.preprocessing.routing.hhmobile.binaryFile;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;
import org.mapsforge.preprocessing.routing.hhmobile.util.Utils;

public class BlockIndexTest {

	private static final int INDEX_SIZE = 50000;
	private static final int BLOCK_SIZE_MIN = 800;
	private static final int BLOCK_SIZE_MAX = 60000;
	private static final int GROUP_SIZE = 100;

	@Test
	public void testIndex() {
		int[] blockSize = Utils.getRandomInts(INDEX_SIZE, BLOCK_SIZE_MIN, BLOCK_SIZE_MAX);
		Arrays.sort(blockSize);

		BlockIndex index = new BlockIndex(blockSize, GROUP_SIZE);
		assertEquals(INDEX_SIZE, index.size());

		int startAddr = 0;
		for (int i = 0; i < index.size(); i++) {
			BlockPointer p = index.getPointer(i);

			assertEquals(p.startAddr, startAddr);
			assertEquals(p.lengthBytes, blockSize[i]);

			startAddr += blockSize[i];
		}
	}

}
