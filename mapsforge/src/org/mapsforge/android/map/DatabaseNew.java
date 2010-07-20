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

import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * A database class for reading binary OSM files.
 */
class DatabaseNew {
	private static final int BINARY_OSM_VERSION = 1;
	private static final String MAGIC_BYTE = "mapsforge binary OSM";

	private RandomAccessFile inputFile;
	private final byte[] readBuffer = new byte[65536];
	private String tempString;
	private String commentText;
	private int tempInt;
	private short stringLength;
	private byte baseZoomLevel;
	private int bufferPosition;
	private byte tileZoomLevelMin;
	private byte tileZoomLevelMax;
	private int boundaryTileLeft;
	private int boundaryTileRight;
	private int boundaryTileTop;
	private int boundaryTileBottom;
	private long mapDataDate;
	private long maximumTileSize; // FIXME: int or long?

	/**
	 * Opens a map file and checks for valid header data.
	 * 
	 * @param fileName
	 *            the path to the map file.
	 * @return true if the file could be opened and is a valid map file, false otherwise.
	 */
	boolean openFile(String fileName) {
		try {
			this.inputFile = new RandomAccessFile(fileName, "r");
			if (!readFileHeader()) {
				return false;
			}
			return true;
		} catch (IOException e) {
			Logger.e(e);
			return false;
		}
	}

	/**
	 * Reads and processes the header block from the file.
	 * 
	 * @return true, if the header was processed successfully, false otherwise.
	 * @throws IOException
	 *             if an error occurs while reading the file.
	 */
	private boolean readFileHeader() throws IOException {
		final int HEADER_SIZE = MAGIC_BYTE.length() + 40;
		// read the fixed size part of the header in the buffer to avoid multiple reads
		if (this.inputFile.read(this.readBuffer, 0, HEADER_SIZE) != HEADER_SIZE) {
			return false;
		}
		this.bufferPosition = 0;

		// check the magic byte
		this.tempString = new String(this.readBuffer, this.bufferPosition, MAGIC_BYTE.length(),
				"US-ASCII");
		this.bufferPosition += MAGIC_BYTE.length();
		if (!this.tempString.equals(MAGIC_BYTE)) {
			Logger.d("invalid magic byte: " + this.tempString);
			return false;
		}

		// check the version number (4 bytes)
		this.tempInt = Deserializer.toInt(this.readBuffer, this.bufferPosition);
		this.bufferPosition += 4;
		if (this.tempInt != BINARY_OSM_VERSION) {
			Logger.d("unsupported version number: " + this.tempInt);
			return false;
		}

		// get and check the base zoom level (1 byte)
		this.baseZoomLevel = this.readBuffer[this.bufferPosition];
		this.bufferPosition += 1;
		if (this.baseZoomLevel < 0 || this.baseZoomLevel > 20) {
			Logger.d("invalid base zooom level: " + this.baseZoomLevel);
			return false;
		}

		// FIXME: Kachelgröße, nach der die Umrechnungen erfolgten (2 Byte).
		this.bufferPosition += 2;

		// get and check the minimum tile zoom level (1 byte)
		this.tileZoomLevelMin = this.readBuffer[this.bufferPosition];
		this.bufferPosition += 1;
		if (this.tileZoomLevelMin < 0 || this.baseZoomLevel > 20) {
			Logger.d("invalid minimum tile zoom level: " + this.tileZoomLevelMin);
			return false;
		}

		// get and check the maximum tile zoom level (1 byte)
		this.tileZoomLevelMax = this.readBuffer[this.bufferPosition];
		this.bufferPosition += 1;
		if (this.tileZoomLevelMax < 0 || this.tileZoomLevelMax > 20) {
			Logger.d("invalid maximum tile zoom level: " + this.tileZoomLevelMax);
			return false;
		}

		// check for valid minimum and maximum tile zoom levels
		if (this.tileZoomLevelMin > this.tileZoomLevelMax) {
			Logger.d("invalid minimum and maximum zoom levels: " + this.tileZoomLevelMin
					+ " - " + this.tileZoomLevelMax);
			return false;
		}

		// get and check the number of the left boundary tile (4 bytes)
		this.boundaryTileLeft = Deserializer.toInt(this.readBuffer, this.bufferPosition);
		this.bufferPosition += 4;
		if (this.boundaryTileLeft < 0) {
			Logger.d("invalid left boundary tile number: " + this.boundaryTileLeft);
			return false;
		}

		// get and check the number of the top boundary tile (4 bytes)
		this.boundaryTileTop = Deserializer.toInt(this.readBuffer, this.bufferPosition);
		this.bufferPosition += 4;
		if (this.boundaryTileTop < 0) {
			Logger.d("invalid top boundary tile number: " + this.boundaryTileTop);
			return false;
		}

		// get and check the number of the right boundary tile (4 bytes)
		this.boundaryTileRight = Deserializer.toInt(this.readBuffer, this.bufferPosition);
		this.bufferPosition += 4;
		if (this.boundaryTileRight < 0) {
			Logger.d("invalid right boundary tile number: " + this.boundaryTileRight);
			return false;
		}

		// get and check the number of the bottom boundary tile (4 bytes)
		this.boundaryTileBottom = Deserializer.toInt(this.readBuffer, this.bufferPosition);
		this.bufferPosition += 4;
		if (this.boundaryTileBottom < 0) {
			Logger.d("invalid bottom boundary tile number: " + this.boundaryTileBottom);
			return false;
		}

		// get and check the date of the map data (8 bytes)
		this.mapDataDate = Deserializer.toLong(this.readBuffer, this.bufferPosition);
		this.bufferPosition += 8;
		if (this.mapDataDate < 0) {
			Logger.d("invalid map data date: " + this.mapDataDate);
			return false;
		}

		// get and check the maximum tile size (5 bytes) FIXME: int or long?
		this.maximumTileSize = Deserializer.toLong(this.readBuffer, this.bufferPosition);
		this.bufferPosition += 5;
		if (this.maximumTileSize < 0) {
			Logger.d("invalid maximum tile size: " + this.maximumTileSize);
			return false;
		}

		// get the length of the comment text (2 bytes)
		this.stringLength = Deserializer.toShort(this.readBuffer, this.bufferPosition);
		this.bufferPosition += 2;
		if (stringLength > 0) {
			// read the comment text
			if (this.inputFile.read(this.readBuffer, 0, this.stringLength) != this.stringLength) {
				return false;
			}
			this.bufferPosition = 0;
			this.commentText = new String(this.readBuffer, this.bufferPosition,
					this.stringLength, "UTF-8");
			this.bufferPosition += this.stringLength;
		}

		return true;
	}
}