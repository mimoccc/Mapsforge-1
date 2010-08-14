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
package org.mapsforge.android.routing.hh;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.LinkedList;

import org.mapsforge.core.Rect;
import org.mapsforge.preprocessing.routing.hhmobile.util.HHGlobals;

final class StaticRTree {

	private final RandomAccessFile raf;
	private final int blockSizeBytes;
	private final long startAddr;
	private final RtreeNode root;
	private final byte[] readBuff;

	/**
	 * Instantiate an RTree stored in a file.
	 * 
	 * @param file
	 *            File where the RTree is stored.
	 * @param startAddr
	 *            byte index of the 1st byte of the r-tree within the given file.
	 * @throws IOException
	 *             if there was an error reading file, or the given location does not contain a
	 *             valid r-tree.
	 */
	public StaticRTree(File file, long startAddr) throws IOException {
		this.startAddr = startAddr;
		this.raf = new RandomAccessFile(file, "r");

		// read header
		raf.seek(startAddr);
		byte[] headerMagic = new byte[HHGlobals.STATIC_RTREE_HEADER_MAGIC.length];
		raf.readFully(headerMagic);
		for (int i = 0; i < headerMagic.length; i++) {
			if (headerMagic[i] != HHGlobals.STATIC_RTREE_HEADER_MAGIC[i]) {
				throw new IOException("Could not access RTree, invalid header.");
			}
		}
		this.blockSizeBytes = raf.readInt();
		this.readBuff = new byte[blockSizeBytes];
		this.root = readNode(1);
	}

	/**
	 * Get integers values associated with the indexed rectangles overlapping the given
	 * rectangle.
	 * 
	 * @param r
	 *            the rectangle to be queried against.
	 * @return integers associated with all matching rectangles.
	 * @throws IOException
	 *             on error accessing file.
	 */
	public LinkedList<Integer> overlaps(Rect r) throws IOException {
		return overlaps(r.minLon, r.maxLon, r.minLat, r.maxLat);
	}

	/**
	 * @param minLon
	 *            longitude bound of the rectangle.
	 * @param maxLon
	 *            longitude bound of the rectangle.
	 * @param minLat
	 *            latitude bound of the rectangle.
	 * @param maxLat
	 *            latitude bound of the rectangle.
	 * @return integers associated with all matching rectangles.
	 * @throws IOException
	 *             on error accessing file.
	 */
	public LinkedList<Integer> overlaps(int minLon, int maxLon, int minLat, int maxLat)
			throws IOException {
		LinkedList<Integer> buff = new LinkedList<Integer>();
		overlaps(minLon, maxLon, minLat, maxLat, root, buff);
		return buff;
	}

	/**
	 * Recursive overlaps query implementation.
	 * 
	 * @param minLon
	 *            longitude bound of the rectangle.
	 * @param maxLon
	 *            longitude bound of the rectangle.
	 * @param minLat
	 *            latitude bound of the rectangle.
	 * @param maxLat
	 *            latitude bound of the rectangle.
	 * @param node
	 *            the current node to be processed.
	 * @param buff
	 *            the result is added to this list.
	 * @throws IOException
	 *             on error accessing file.
	 */
	private void overlaps(int minLon, int maxLon, int minLat, int maxLat, RtreeNode node,
			LinkedList<Integer> buff) throws IOException {
		for (int i = 0; i < node.minLon.length; i++) {
			boolean overlaps = Rect.overlaps(node.minLon[i], node.maxLon[i], node.minLat[i],
					node.maxLat[i], minLon, maxLon, minLat, maxLat);
			if (overlaps) {
				if (node.isLeaf) {
					buff.add(node.pointer[i]);
				} else {
					RtreeNode child = readNode(node.pointer[i]);
					overlaps(minLon, maxLon, minLat, maxLat, child, buff);
				}
			}
		}
	}

	/**
	 * Reads a node from secondary storage.
	 * 
	 * @param id
	 *            The id of the given node (root node has id 1).
	 * @return the desired node representation.
	 * @throws IOException
	 *             on error accessing file.
	 */
	private RtreeNode readNode(int id) throws IOException {
		raf.seek(startAddr + (blockSizeBytes * id));
		raf.readFully(readBuff);
		return new RtreeNode(readBuff);
	}

	/**
	 * A node representation used only during runtime, not during r-tree construction.
	 */
	private static class RtreeNode {
		final boolean isLeaf;
		final int[] minLon, maxLon, minLat, maxLat, pointer;

		/**
		 * Constructs a Tree Node based on the given data representing the tree node.
		 * 
		 * @param b
		 *            data where the tree node is stored.
		 * @throws IOException
		 *             on error accessing file.
		 */
		public RtreeNode(byte[] b) throws IOException {
			DataInputStream stream = new DataInputStream(new ByteArrayInputStream(b));

			this.isLeaf = stream.readBoolean();
			short numEntries = stream.readShort();
			this.minLon = new int[numEntries];
			this.maxLon = new int[numEntries];
			this.minLat = new int[numEntries];
			this.maxLat = new int[numEntries];
			this.pointer = new int[numEntries];

			for (int i = 0; i < numEntries; i++) {
				this.minLon[i] = stream.readInt();
				this.maxLon[i] = stream.readInt();
				this.minLat[i] = stream.readInt();
				this.maxLat[i] = stream.readInt();
				this.pointer[i] = stream.readInt();
			}
		}
	}

}
