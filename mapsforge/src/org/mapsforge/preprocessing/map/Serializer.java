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
package org.mapsforge.preprocessing.map;

class Serializer {
	static final byte[] getBytes(int value) {
		return new byte[] { (byte) (value >> 24), (byte) (value >> 16), (byte) (value >> 8),
				(byte) value };
	}

	static byte[] getBytes(long value) {
		return new byte[] { (byte) (value >> 56), (byte) (value >> 48), (byte) (value >> 40),
				(byte) (value >> 32), (byte) (value >> 24), (byte) (value >> 16),
				(byte) (value >> 8), (byte) (value) };
	}

	static byte[] getBytes(short value) {
		return new byte[] { (byte) (value >> 8), (byte) value };
	}
}