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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import org.mapsforge.preprocessing.routing.hhmobile.binaryFile.graph.BlockPointer;
import org.mapsforge.preprocessing.routing.hhmobile.binaryFile.graph.BlockPointerIndex;
import org.mapsforge.preprocessing.routing.hhmobile.binaryFile.graph.BlockedGraphHeader;

class BlockReader {

	private final RandomAccessFile raf;
	private final BlockedGraphHeader graphHeader;
	private final BlockPointerIndex blockIndex;
	private final long startAddrClusterBlocks;

	private int bytesRead = 0;

	public BlockReader(File f, long startAddrClusterBlocks, BlockedGraphHeader graphHeader,
			BlockPointerIndex blockIndex) throws FileNotFoundException {
		this.raf = new RandomAccessFile(f, "r");
		this.graphHeader = graphHeader;
		this.blockIndex = blockIndex;
		this.startAddrClusterBlocks = startAddrClusterBlocks;
	}

	public Block readBlock(int blockId) throws IOException {
		BlockPointer pointer = blockIndex.getPointer(blockId);
		raf.seek(startAddrClusterBlocks + pointer.startAddr);
		byte[] buff = new byte[pointer.lengthBytes];
		bytesRead += buff.length;
		raf.readFully(buff);
		return new Block(buff, graphHeader, blockId);
	}

	public int getNumBlocks() {
		return blockIndex.size();
	}

}
