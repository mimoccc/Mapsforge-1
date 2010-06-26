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

public class BitArrayOutputStream {

	private int byteOffset, bitOffset;
	private byte[] buff;
	private long bitsRemain;

	public BitArrayOutputStream(byte[] buff) {
		this.byteOffset = 0;
		this.bitOffset = 0;
		this.buff = buff;
		this.bitsRemain = buff.length * 8L;
	}

	public int getByteOffset() {
		return byteOffset;
	}

	public int getBitOffset() {
		return bitOffset;
	}

	public long getBitsRemain() {
		return bitsRemain;
	}

	public int numBytesWritten() {
		if (bitOffset == 0) {
			return byteOffset;
		}
		return byteOffset + 1;
	}

	public void writeBit(boolean val) throws IOException {
		if (bitsRemain >= 1) {
			BitSerializer.writeBit(val, buff, byteOffset, bitOffset);
			bitsWritten(1);
		} else {
			throw new IOException();
		}
	}

	public void writeByte(byte val) throws IOException {
		if (bitsRemain >= BitSerializer.BITS_PER_BYTE) {
			BitSerializer.writeByte(val, buff, byteOffset, bitOffset);
			bitsWritten(BitSerializer.BITS_PER_BYTE);
		} else {
			throw new IOException();
		}
	}

	public void writeShort(short val) throws IOException {
		if (bitsRemain >= BitSerializer.BITS_PER_SHORT) {
			BitSerializer.writeShort(val, buff, byteOffset, bitOffset);
			bitsWritten(BitSerializer.BITS_PER_SHORT);
		} else {
			throw new IOException();
		}
	}

	public void writeInt(int val) throws IOException {
		if (bitsRemain >= BitSerializer.BITS_PER_INT) {
			BitSerializer.writeInt(val, buff, byteOffset, bitOffset);
			bitsWritten(BitSerializer.BITS_PER_INT);
		} else {
			throw new IOException();
		}
	}

	public void writeLong(long val) throws IOException {
		if (bitsRemain >= BitSerializer.BITS_PER_LONG) {
			BitSerializer.writeLong(val, buff, byteOffset, bitOffset);
			bitsWritten(BitSerializer.BITS_PER_LONG);
		} else {
			throw new IOException();
		}
	}

	public void writeUInt(long val, int nBits) throws IOException {
		if (bitsRemain >= nBits) {
			BitSerializer.writeUInt(val, nBits, buff, byteOffset, bitOffset);
			bitsWritten(nBits);
		} else {
			throw new IOException();
		}
	}

	public void write(byte[] b) throws IOException {
		for (int i = 0; i < b.length; i++) {
			writeByte(b[i]);
		}
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
		} else {
			throw new IOException();
		}
	}

	private void bitsWritten(int nBits) {
		byteOffset += (bitOffset + nBits) / 8;
		bitOffset = (bitOffset + nBits) % 8;
		bitsRemain -= 8;
	}
}
