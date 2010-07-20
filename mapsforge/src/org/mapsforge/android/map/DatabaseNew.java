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
import java.io.UnsupportedEncodingException;

/**
 * A database class for reading binary OSM files.
 */
class DatabaseNew {
	private static final int BINARY_OSM_VERSION = 1;
	private static final int INDEX_CACHE_SIZE = 32;
	private static final String MAGIC_BYTE = "mapsforge binary OSM";

	private byte baseZoomLevel;
	private int boundaryTileBottom;
	private int boundaryTileLeft;
	private int boundaryTileRight;
	private int boundaryTileTop;
	private int bufferPosition;
	private String commentText;
	private long currentBlockPointer;
	private int currentBlockSize;
	private short currentNodeElevation;
	private byte currentNodeFeatureByte;
	private boolean currentNodeFeatureElevation;
	private boolean currentNodeFeatureHouseNumber;
	private boolean currentNodeFeatureName;
	private String currentNodeHouseNumber;
	private int currentNodeLatitude;
	private byte currentNodeLayer;
	private int currentNodeLongitude;
	private String currentNodeName;
	private byte currentNodeNumberOfTags;
	private byte currentNodeSpecialByte;
	private boolean[] currentNodeTagIds;
	private byte currentTagId;
	private byte currentWayNumberOfRelevantTags;
	private byte currentWayNumberOfTags;
	private int currentWaySize;
	private byte currentWaySpecialByte1;
	private byte currentWaySpecialByte2;
	private short currentWayTileBitmap;
	private DatabaseIndexCacheNew databaseIndexCache;
	private boolean[] defaultNodeTagIds;
	private short elementCounter;
	private long firstWayOffset;
	private long indexStartAddress;
	private RandomAccessFile inputFile;
	private long mapDataDate;
	private int mapFileBlocks;
	private int mapFileBlocksHeight;
	private int mapFileBlocksWidth;
	private int maximumTileSize; // FIXME: int or long?
	private long nextBlockPointer;
	private short nodesOnZoomLevel;
	private DatabaseMapGenerator queryMapGenerator;
	private boolean queryReadWayNames;
	private byte queryZoomLevel;
	private byte[] readBuffer;
	private short stringLength;
	private int tempByte;
	private int tempInt;
	private String tempString;
	private byte tileZoomLevelMax;
	private byte tileZoomLevelMin;
	private short waysOnZoomLevel;

	/**
	 * Closes the map file.
	 */
	void closeFile() {
		if (this.databaseIndexCache != null) {
			this.databaseIndexCache.destroy();
			this.databaseIndexCache = null;
		}
		try {
			if (this.inputFile != null) {
				this.inputFile.close();
				this.inputFile = null;
			}
		} catch (IOException e) {
			Logger.e(e);
		}
	}

	/**
	 * Start a database query with the given parameters.
	 * 
	 * @param tile
	 *            the tile to read
	 * @param readWayNames
	 *            if way names should be read
	 * @param mapGenerator
	 *            the MapGenerator object for rendering all map elements
	 */
	void executeQuery(Tile tile, boolean readWayNames, DatabaseMapGenerator mapGenerator) {
		try {
			if (tile.zoomLevel > this.tileZoomLevelMax) {
				this.queryZoomLevel = this.tileZoomLevelMax;
			} else {
				this.queryZoomLevel = tile.zoomLevel;
			}
			this.queryReadWayNames = readWayNames;
			this.queryMapGenerator = mapGenerator;
			if (tile.zoomLevel >= this.baseZoomLevel) {
				// calculate the appropriate tile in the base zoom level
				double baseTileLongitude = MercatorProjection.tileXToLongitude(tile.x,
						tile.zoomLevel);
				double baseTileLatitude = MercatorProjection.tileYToLatitude(tile.y,
						tile.zoomLevel);

				long baseTileX = MercatorProjection.longitudeToTileX(baseTileLongitude,
						this.baseZoomLevel);
				long baseTileY = MercatorProjection.latitudeToTileY(baseTileLatitude,
						this.baseZoomLevel);

				// calculate the position of the needed block in the file
				long neededBlockX = baseTileX - this.boundaryTileLeft;
				long neededBlockY = baseTileY - this.boundaryTileTop;

				// calculate the block number of the needed block in the file
				long blockNumber = neededBlockY * this.mapFileBlocksWidth + neededBlockX;

				// get the pointers to the current and the next block
				this.currentBlockPointer = this.databaseIndexCache.getAddress(blockNumber);
				this.nextBlockPointer = this.databaseIndexCache.getAddress(blockNumber + 1);

				// check if the next block has a valid pointer
				if (this.nextBlockPointer == -1) {
					// the current block is the last one in the file
					this.nextBlockPointer = this.inputFile.length();
				}

				// calculate the size of the current block
				// FIXME: what if pointer == 0 for empty tiles?
				this.currentBlockSize = (int) (this.nextBlockPointer - this.currentBlockPointer);

				// if the current block has no map data continue with the next one
				if (this.currentBlockSize == 0) {
					return;
				}

				// go to the current block and read its data to the buffer
				this.inputFile.seek(this.currentBlockPointer);
				if (this.inputFile.read(this.readBuffer, 0, this.currentBlockSize) != this.currentBlockSize) {
					// if reading the current block has failed, skip it
					return;
				}

				// handle the current block data
				processBlock();
			}
		} catch (IOException e) {
			Logger.e(e);
		}
	}

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

			// create the DatabaseIndexCache
			this.databaseIndexCache = new DatabaseIndexCacheNew(this.inputFile,
					this.mapFileBlocks, this.indexStartAddress, INDEX_CACHE_SIZE);

			// create a read buffer that is big enough to store even the largest tile
			this.readBuffer = new byte[this.maximumTileSize];

			// create the tag arrays
			this.defaultNodeTagIds = new boolean[Byte.MAX_VALUE];
			this.currentNodeTagIds = new boolean[Byte.MAX_VALUE];

			return true;
		} catch (IOException e) {
			Logger.e(e);
			return false;
		}
	}

	/**
	 * Read a single block and call the render functions on all map elements.
	 * 
	 * @throws IndexOutOfBoundsException
	 *             if the block contains invalid data.
	 * @throws UnsupportedEncodingException
	 *             if the string decoding fails.
	 */
	private void processBlock() throws IndexOutOfBoundsException, UnsupportedEncodingException {
		// the index table stores the number of ways and POIs on each zoomlevel
		int blockEntryTableSize = 2 * (this.tileZoomLevelMax - this.tileZoomLevelMin + 1) * 2;

		// each table row needs 4 bytes per zoomlevel entry
		int tableRowOffset = (this.queryZoomLevel - this.tileZoomLevelMin) * 4;
		this.bufferPosition = tableRowOffset;

		// read the amount of way and nodes on the current zoomLevel level
		this.nodesOnZoomLevel = Deserializer.toShort(this.readBuffer, this.bufferPosition);
		this.waysOnZoomLevel = Deserializer.toShort(this.readBuffer, this.bufferPosition + 2);
		this.bufferPosition = blockEntryTableSize;

		// read the offset to the first stored way in the block (8 bytes)
		// TODO: change to int with 4 bytes?
		this.firstWayOffset = Deserializer.toLong(this.readBuffer, this.bufferPosition);
		this.bufferPosition += 8;

		// read nodes
		for (this.elementCounter = this.nodesOnZoomLevel; this.elementCounter != 0; --this.elementCounter) {
			// read node latitude
			this.currentNodeLatitude = Deserializer.toInt(this.readBuffer, this.bufferPosition);
			bufferPosition += 4;

			// read node longitude
			this.currentNodeLongitude = Deserializer
					.toInt(this.readBuffer, this.bufferPosition);
			bufferPosition += 4;

			// read the special byte that encodes multiple fields
			this.currentNodeSpecialByte = this.readBuffer[this.bufferPosition];
			bufferPosition += 1;

			// bit 1-4 of the special byte represent the node layer
			this.currentNodeLayer = (byte) ((this.currentNodeSpecialByte & 0xf0) >> 4);
			// bit 5-7 of the special byte represent the number of tags
			this.currentNodeNumberOfTags = (byte) ((this.currentNodeSpecialByte & 0x0e) >> 1);

			// reset the node tag array
			System.arraycopy(this.defaultNodeTagIds, 0, this.currentNodeTagIds, 0,
					this.defaultNodeTagIds.length);
			// read node tags
			for (tempByte = this.currentNodeNumberOfTags; tempByte != 0; --tempByte) {
				this.currentTagId = this.readBuffer[this.bufferPosition];
				this.bufferPosition += 1;
				this.currentNodeTagIds[this.currentTagId] = true;
			}

			// read the feature byte that activates optional node features
			this.currentNodeFeatureByte = this.readBuffer[this.bufferPosition];
			bufferPosition += 1;

			// check if the current node has a name
			this.currentNodeFeatureName = (this.currentNodeFeatureByte & 80) != 0;
			if (this.currentNodeFeatureName) {
				// get the length of the node name (2 bytes)
				this.stringLength = Deserializer.toShort(this.readBuffer, this.bufferPosition);
				this.bufferPosition += 2;
				if (stringLength > 0) {
					// read the node name
					this.currentNodeName = new String(this.readBuffer, this.bufferPosition,
							this.stringLength, "UTF-8");
					this.bufferPosition += this.stringLength;
				}
			}

			// check if the current node has an elevation
			this.currentNodeFeatureElevation = (this.currentNodeFeatureByte & 40) != 0;
			if (this.currentNodeFeatureElevation) {
				// get the node elevation (2 bytes)
				this.currentNodeElevation = Deserializer.toShort(this.readBuffer,
						this.bufferPosition);
				this.bufferPosition += 2;
			}

			// check if the current node has a house number
			this.currentNodeFeatureHouseNumber = (this.currentNodeFeatureByte & 20) != 0;
			if (this.currentNodeFeatureHouseNumber) {
				// get the length of the node house number (2 bytes)
				this.stringLength = Deserializer.toShort(this.readBuffer, this.bufferPosition);
				this.bufferPosition += 2;
				if (stringLength > 0) {
					// read the node house number
					this.currentNodeHouseNumber = new String(this.readBuffer,
							this.bufferPosition, this.stringLength, "UTF-8");
					this.bufferPosition += this.stringLength;
				}
			}

			// render the current node
			// TODO: send optional node fields to the MapGenerator
			this.queryMapGenerator.renderPointOfInterest(this.currentNodeLayer,
					this.currentNodeLatitude, this.currentNodeLongitude, this.currentNodeName,
					this.currentNodeTagIds);
		}

		// move the pointer to the first way
		this.bufferPosition = (int) this.firstWayOffset;
		// TODO: remove cast after changed to int

		// read ways
		for (this.elementCounter = this.waysOnZoomLevel; this.elementCounter != 0; --this.elementCounter) {
			// FIXME: change from int to short?
			// read the size of the current way
			this.currentWaySize = Deserializer.toInt(this.readBuffer, this.bufferPosition);
			this.bufferPosition += 4;

			// read the current way tile bitmap
			this.currentWayTileBitmap = Deserializer.toShort(this.readBuffer,
					this.bufferPosition);
			this.bufferPosition += 2;
			// TODO: do something with this bitmap

			// read the first special byte that encodes multiple fields
			this.currentWaySpecialByte1 = this.readBuffer[this.bufferPosition];
			bufferPosition += 1;

			// bit 1-4 of the first special byte represent the way layer
			this.currentWayNumberOfRelevantTags = (byte) ((this.currentWaySpecialByte1 & 0xf0) >> 4);
			// bit 5-7 of the first special byte represent the number of tags
			this.currentWayNumberOfTags = (byte) ((this.currentWaySpecialByte1 & 0x0e) >> 1);

			// read the second special byte that encodes multiple fields
			this.currentWaySpecialByte2 = this.readBuffer[this.bufferPosition];
			bufferPosition += 1;

			// bit 1-3 of the second special byte represent the number of relevant tags
			this.currentWayNumberOfRelevantTags = (byte) ((this.currentWaySpecialByte2 & 0xe0) >> 5);

			// TODO: handle all the remaining data of the way
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
		Logger.d("processing file header ...");
		final int FIXED_HEADER_SIZE = MAGIC_BYTE.length() + 39; // FIXME: really 39?
		this.readBuffer = new byte[FIXED_HEADER_SIZE];

		// read the fixed size part of the header in the buffer to avoid multiple reads
		if (this.inputFile.read(this.readBuffer, 0, FIXED_HEADER_SIZE) != FIXED_HEADER_SIZE) {
			return false;
		}
		this.bufferPosition = 0;

		// check the magic byte
		this.tempString = new String(this.readBuffer, this.bufferPosition, MAGIC_BYTE.length(),
				"US-ASCII");
		this.bufferPosition += MAGIC_BYTE.length();
		Logger.d("  magic byte: " + this.tempString);
		if (!this.tempString.equals(MAGIC_BYTE)) {
			Logger.d("invalid magic byte: " + this.tempString);
			return false;
		}

		// check the version number (4 bytes)
		this.tempInt = Deserializer.toInt(this.readBuffer, this.bufferPosition);
		this.bufferPosition += 4;
		Logger.d("  version number: " + this.tempInt);
		if (this.tempInt != BINARY_OSM_VERSION) {
			Logger.d("unsupported version number: " + this.tempInt);
			return false;
		}

		// get and check the base zoom level (1 byte)
		this.baseZoomLevel = this.readBuffer[this.bufferPosition];
		this.bufferPosition += 1;
		Logger.d("  baseZoomLevel: " + this.baseZoomLevel);
		if (this.baseZoomLevel < 0 || this.baseZoomLevel > 20) {
			Logger.d("invalid base zooom level: " + this.baseZoomLevel);
			return false;
		}

		// FIXME: Kachelgröße, nach der die Umrechnungen erfolgten (2 Byte).
		this.bufferPosition += 2;

		// get and check the minimum tile zoom level (1 byte)
		this.tileZoomLevelMin = this.readBuffer[this.bufferPosition];
		this.bufferPosition += 1;
		Logger.d("  tileZoomLevelMin: " + this.tileZoomLevelMin);
		if (this.tileZoomLevelMin < 0 || this.baseZoomLevel > 20) {
			Logger.d("invalid minimum tile zoom level: " + this.tileZoomLevelMin);
			return false;
		}

		// get and check the maximum tile zoom level (1 byte)
		this.tileZoomLevelMax = this.readBuffer[this.bufferPosition];
		this.bufferPosition += 1;
		Logger.d("  tileZoomLevelMax: " + this.tileZoomLevelMax);
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
		Logger.d("  boundaryTileLeft: " + this.boundaryTileLeft);
		if (this.boundaryTileLeft < 0) {
			Logger.d("invalid left boundary tile number: " + this.boundaryTileLeft);
			return false;
		}

		// get and check the number of the top boundary tile (4 bytes)
		this.boundaryTileTop = Deserializer.toInt(this.readBuffer, this.bufferPosition);
		this.bufferPosition += 4;
		Logger.d("  boundaryTileTop: " + this.boundaryTileTop);
		if (this.boundaryTileTop < 0) {
			Logger.d("invalid top boundary tile number: " + this.boundaryTileTop);
			return false;
		}

		// get and check the number of the right boundary tile (4 bytes)
		this.boundaryTileRight = Deserializer.toInt(this.readBuffer, this.bufferPosition);
		this.bufferPosition += 4;
		Logger.d("  boundaryTileRight: " + this.boundaryTileRight);
		if (this.boundaryTileRight < 0) {
			Logger.d("invalid right boundary tile number: " + this.boundaryTileRight);
			return false;
		}

		// get and check the number of the bottom boundary tile (4 bytes)
		this.boundaryTileBottom = Deserializer.toInt(this.readBuffer, this.bufferPosition);
		this.bufferPosition += 4;
		Logger.d("  boundaryTileBottom: " + this.boundaryTileBottom);
		if (this.boundaryTileBottom < 0) {
			Logger.d("invalid bottom boundary tile number: " + this.boundaryTileBottom);
			return false;
		}

		// calculate the horizontal and vertical amount of blocks in the file
		this.mapFileBlocksWidth = this.boundaryTileRight - this.boundaryTileLeft + 1;
		Logger.d("  mapFileBlocksWidth: " + this.mapFileBlocksWidth);
		this.mapFileBlocksHeight = this.boundaryTileBottom - this.boundaryTileTop + 1;
		Logger.d("  mapFileBlocksHeight: " + this.mapFileBlocksHeight);
		// calculate the total amount of blocks in the file
		this.mapFileBlocks = this.mapFileBlocksWidth * this.mapFileBlocksHeight;
		Logger.d("  mapFileBlocks: " + this.mapFileBlocks);

		// get and check the date of the map data (8 bytes)
		this.mapDataDate = Deserializer.toLong(this.readBuffer, this.bufferPosition);
		this.bufferPosition += 8;
		Logger.d("  mapDataDate: " + this.mapDataDate);
		if (this.mapDataDate < 0) {
			Logger.d("invalid map data date: " + this.mapDataDate);
			return false;
		}

		// get and check the maximum tile size (4 bytes) FIXME: int or long?
		this.maximumTileSize = Deserializer.toInt(this.readBuffer, this.bufferPosition);
		this.bufferPosition += 4;
		Logger.d("  maximumTileSize: " + this.maximumTileSize);
		if (this.maximumTileSize < 0) {
			Logger.d("invalid maximum tile size: " + this.maximumTileSize);
			return false;
		}

		// get the length of the comment text (2 bytes)
		this.stringLength = Deserializer.toShort(this.readBuffer, this.bufferPosition);
		this.bufferPosition += 2;
		Logger.d("  commentText length: " + this.stringLength);
		if (stringLength > 0) {
			// read the comment text
			this.readBuffer = new byte[this.stringLength];
			if (this.inputFile.read(this.readBuffer, 0, this.stringLength) != this.stringLength) {
				return false;
			}
			this.bufferPosition = 0;
			this.commentText = new String(this.readBuffer, this.bufferPosition,
					this.stringLength, "UTF-8");
			this.bufferPosition += this.stringLength;
			Logger.d("  commentText: " + this.commentText);
		}

		// save the end address of the header where the index starts
		this.indexStartAddress = FIXED_HEADER_SIZE + stringLength;

		Logger.d("finished file header");
		return true;
	}
}