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

class Deserializer {
	/**
	 * Converts four bytes from an byte array to an int number.
	 * 
	 * @param buffer
	 *            the byte array
	 * @param offset
	 *            the offset in the array
	 * @return the int value
	 */
	static int toInt(byte[] buffer, int offset) {
		return buffer[offset] << 24 | (buffer[offset + 1] & 0xff) << 16
				| (buffer[offset + 2] & 0xff) << 8 | (buffer[offset + 3] & 0xff);
	}

	/**
	 * Converts eight bytes from an byte array to a long number.
	 * 
	 * @param buffer
	 *            the byte array
	 * @param offset
	 *            the offset in the array
	 * @return the long value
	 */
	static long toLong(byte[] buffer, int offset) {
		return (long) buffer[offset] << 56 | (buffer[offset + 1] & 0xffL) << 48
				| (buffer[offset + 2] & 0xffL) << 40 | (buffer[offset + 3] & 0xffL) << 32
				| (buffer[offset + 4] & 0xffL) << 24 | (buffer[offset + 5] & 0xffL) << 16
				| (buffer[offset + 6] & 0xffL) << 8 | (buffer[offset + 7] & 0xffL);
	}

	/**
	 * Converts two bytes from an byte array to a short number.
	 * 
	 * @param buffer
	 *            the byte array
	 * @param offset
	 *            the offset in the array
	 * @return the short value
	 */
	static short toShort(byte[] buffer, int offset) {
		return (short) (buffer[offset] << 8 | (buffer[offset + 1] & 0xff));
	}
}