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
package org.mapsforge.preprocessing.routing.hhmobile.util;

import java.io.IOException;

public class BitArrayInputStream {

	private int byteOffset, bitOffset;
	private byte[] buff;
	private long bitsAvailable;

	public BitArrayInputStream(byte[] buff) {
		this.bitOffset = 0;
		this.byteOffset = 0;
		this.buff = buff;
		this.bitsAvailable = buff.length * 8L;
	}

	public int getByteOffset() {
		return byteOffset;
	}

	public int getBitOffset() {
		return bitOffset;
	}

	public long getBitsAvailable() {
		return bitsAvailable;
	}

	public void read(byte[] b) throws IOException {
		for (int i = 0; i < b.length; i++) {
			b[i] = readByte();
		}
	}

	public boolean readBit() throws IOException {
		if (bitsAvailable >= 1) {
			boolean val = BitSerializer.readBit(buff, byteOffset, bitOffset);
			bitsRead(1);
			return val;
		}
		throw new IOException();
	}

	public byte readByte() throws IOException {
		if (bitsAvailable >= BitSerializer.BITS_PER_BYTE) {
			byte val = BitSerializer.readByte(buff, byteOffset, bitOffset);
			bitsRead(BitSerializer.BITS_PER_BYTE);
			return val;
		}
		throw new IOException();
	}

	public short readShort() throws IOException {
		if (bitsAvailable >= BitSerializer.BITS_PER_SHORT) {
			short val = BitSerializer.readShort(buff, byteOffset, bitOffset);
			bitsRead(BitSerializer.BITS_PER_SHORT);
			return val;
		}
		throw new IOException();
	}

	public int readInt() throws IOException {
		if (bitsAvailable >= BitSerializer.BITS_PER_INT) {
			int val = BitSerializer.readInt(buff, byteOffset, bitOffset);
			bitsRead(BitSerializer.BITS_PER_INT);
			return val;
		}
		throw new IOException();
	}

	public long readLong() throws IOException {
		if (bitsAvailable >= BitSerializer.BITS_PER_LONG) {
			long val = BitSerializer.readLong(buff, byteOffset, bitOffset);
			bitsRead(BitSerializer.BITS_PER_LONG);
			return val;
		}
		throw new IOException();
	}

	public long readUInt(int nBits) throws IOException {
		if (bitsAvailable >= nBits) {
			long val = BitSerializer.readUInt(buff, nBits, byteOffset, bitOffset);
			bitsRead(nBits);
			return val;
		}
		throw new IOException();
	}

	public void alignPointer(int byteAlignment) throws IOException {
		int _byteOffset = byteOffset;
		if (bitOffset != 0) {
			_byteOffset++;
		}
		if (_byteOffset % byteAlignment != 0) {
			_byteOffset += byteAlignment - (_byteOffset % byteAlignment);
		}
		if (_byteOffset <= buff.length) {
			byteOffset = _byteOffset;
			bitOffset = 0;
			bitsAvailable = (buff.length - byteOffset) * 8L;
		} else {
			throw new IOException();
		}
	}

	public void setPointer(int _byteOffset, int _bitOffset) throws IOException {
		if (_byteOffset < buff.length || (_byteOffset == buff.length && _bitOffset == 0)) {
			byteOffset = _byteOffset;
			bitOffset = _bitOffset;
			bitsAvailable = ((buff.length - byteOffset) * 8L) - bitOffset;
		} else {
			throw new IOException();
		}
	}

	public void skipBits(int nBits) throws IOException {
		int _byteOffset = byteOffset + ((bitOffset + nBits) / 8);
		int _bitOffset = (bitOffset + nBits) % 8;
		if (_byteOffset < buff.length || (_byteOffset == buff.length && _bitOffset == 0)) {
			byteOffset = _byteOffset;
			bitOffset = _bitOffset;
			bitsAvailable = ((buff.length - byteOffset) * 8L) - bitOffset;
		} else {
			throw new IOException();
		}
	}

	private void bitsRead(int nBits) {
		byteOffset += (bitOffset + nBits) / 8;
		bitOffset = (bitOffset + nBits) % 8;
		bitsAvailable -= nBits;
	}

}
