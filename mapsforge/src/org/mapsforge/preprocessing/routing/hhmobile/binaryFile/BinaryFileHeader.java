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

import java.io.IOException;

import org.mapsforge.preprocessing.routing.hhmobile.util.BitArrayInputStream;
import org.mapsforge.preprocessing.routing.hhmobile.util.BitArrayOutputStream;

public class BinaryFileHeader {

	public final static byte[] HEADER_MAGIC = "# mapsforge hh #".getBytes();
	public final static int HEADER_LENGTH = 4096;

	public final byte[] headerMagic;

	public final long startAddrGraphHeader;
	public final long endAddrGraphHeader;

	public final long startAddrClusterBlocks;
	public final long endAddrClusterBlocks;

	public final long startAddrBlockPointerIdx;
	public final long endAddrBlockPointerIdx;

	public final String comment;

	public BinaryFileHeader(long startAddrGraphHeader, long endAddrGraphHeader,
			long startAddrClusterBlocks, long endAddrClusterBlocks,
			long startAddrBlockPointerIdx, long endAddrBlockPointerIdx, String comment) {
		this.headerMagic = HEADER_MAGIC;
		this.startAddrGraphHeader = startAddrGraphHeader;
		this.endAddrGraphHeader = endAddrGraphHeader;
		this.startAddrClusterBlocks = startAddrClusterBlocks;
		this.endAddrClusterBlocks = endAddrClusterBlocks;
		this.startAddrBlockPointerIdx = startAddrBlockPointerIdx;
		this.endAddrBlockPointerIdx = endAddrBlockPointerIdx;
		this.comment = comment;
	}

	public BinaryFileHeader(byte[] buff) throws IOException {
		BitArrayInputStream stream = new BitArrayInputStream(buff);
		this.headerMagic = new byte[HEADER_MAGIC.length];
		stream.read(headerMagic);
		this.startAddrGraphHeader = stream.readLong();
		this.endAddrGraphHeader = stream.readLong();

		this.startAddrClusterBlocks = stream.readLong();
		this.endAddrClusterBlocks = stream.readLong();

		this.startAddrBlockPointerIdx = stream.readLong();
		this.endAddrBlockPointerIdx = stream.readLong();

		short commentLength = stream.readShort();
		byte[] comment_ = new byte[commentLength];
		stream.read(comment_);
		this.comment = new String(comment_, "utf-8");
	}

	public boolean isHeaderValid() {
		for (int i = 0; i < headerMagic.length; i++) {
			if (headerMagic[i] != HEADER_MAGIC[i]) {
				return false;
			}
		}
		return true;
	}

	public byte[] serialize() {
		try {
			byte[] buff = new byte[HEADER_LENGTH];
			BitArrayOutputStream stream = new BitArrayOutputStream(buff);

			stream.write(headerMagic);

			stream.writeLong(startAddrGraphHeader);
			stream.writeLong(endAddrGraphHeader);

			stream.writeLong(startAddrClusterBlocks);
			stream.writeLong(endAddrClusterBlocks);

			stream.writeLong(startAddrBlockPointerIdx);
			stream.writeLong(endAddrBlockPointerIdx);

			stream.writeShort((short) comment.getBytes("utf-8").length);
			stream.write(comment.getBytes("utf-8"));
			return buff;
		} catch (IOException e) {
			throw new RuntimeException("Header size exceeded");
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(BinaryFileHeader.class.getName() + " (\n");
		sb.append("  headerMagic = '" + new String(headerMagic) + "'\n");
		sb.append("  startAddrGraphHeader = " + startAddrGraphHeader + "\n");
		sb.append("  endAddrGraphHeader = " + endAddrGraphHeader + "\n");
		sb.append("  startAddrClusterBlocks = " + startAddrClusterBlocks + "\n");
		sb.append("  endAddrClusterBlocks = " + endAddrClusterBlocks + "\n");
		sb.append("  startAddrBlockPointerIdx = " + startAddrBlockPointerIdx + "\n");
		sb.append("  endAddrBlockPointerIdx = " + endAddrBlockPointerIdx + "\n");
		sb.append("  comment = '" + comment + "'\n");
		sb.append(")");

		return sb.toString();
	}
}
