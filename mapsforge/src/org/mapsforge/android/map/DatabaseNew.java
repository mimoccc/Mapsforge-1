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
	private static final String BINARY_OSM_MAGIC_BYTE = "mapsforge binary OSM";
	private static final int BINARY_OSM_VERSION = 1;
	private static final int INDEX_CACHE_SIZE = 32;

	private byte baseZoomLevel;
	private long blockNumber;
	private int boundaryBottom;
	private long boundaryBottomTile;
	private int boundaryLeft;
	private long boundaryLeftTile;
	private int boundaryRight;
	private long boundaryRightTile;
	private int boundaryTop;
	private long boundaryTopTile;
	private int bufferPosition;
	private String commentText;
	private long currentBlockPointer;
	private int currentBlockSize;
	private DatabaseIndexCacheNew databaseIndexCache;
	private boolean[] defaultNodeTagIds;
	private boolean[] defaultWayTagIds;
	private short elementCounter;
	private long firstWayOffset;
	private long indexStartAddress;
	private int[] innerWay;
	private short innerWayNodesSequenceLength;
	private short innerWayNumberOfWayNodes;
	private RandomAccessFile inputFile;
	private long inputFileSize;
	private long mapDataDate;
	private long mapFileBlocks;
	private long mapFileBlocksHeight;
	private long mapFileBlocksWidth;
	private long maximumTileSize;
	private long nextBlockPointer;
	private short nodeElevation;
	private byte nodeFeatureByte;
	private boolean nodeFeatureElevation;
	private boolean nodeFeatureHouseNumber;
	private boolean nodeFeatureName;
	private String nodeHouseNumber;
	private int nodeLatitude;
	private byte nodeLayer;
	private int nodeLongitude;
	private String nodeName;
	private byte nodeNumberOfTags;
	private short nodesOnZoomLevel;
	private byte nodeSpecialByte;
	private byte nodeTagId;
	private boolean[] nodeTagIds;
	private DatabaseMapGenerator queryMapGenerator;
	private boolean queryReadWayNames;
	private byte queryZoomLevel;
	private byte[] readBuffer;
	private boolean stopCurrentQuery;
	private short stringLength;
	private byte tempByte;
	private int tempInt;
	private short tempShort;
	private String tempString;
	private int tileEntriesTableSize;
	private short tilePixelSize;
	private byte tileZoomLevelMax;
	private byte tileZoomLevelMin;
	private boolean wayFeatureArea;
	private byte wayFeatureByte;
	private boolean wayFeatureLabelPosition;
	private boolean wayFeatureMultipolygon;
	private boolean wayFeatureName;
	private int[][] wayInnerWays;
	private int wayLabelPositionLatitude;
	private int wayLabelPositionLongitude;
	private byte wayLayer;
	private String wayName;
	private int wayNodeLatitude;
	private int wayNodeLongitude;
	private int[] wayNodesSequence;
	private short wayNodesSequenceLength;
	private byte wayNumberOfInnerWays;
	private short wayNumberOfWayNodes;
	private byte wayNumberOfRelevantTags;
	private byte wayNumberOfTags;
	private int waySize;
	private short waysOnZoomLevel;
	private byte waySpecialByte1;
	private byte waySpecialByte2;
	private byte wayTagBitmap;
	private byte wayTagId;
	private boolean[] wayTagIds;
	private short wayTileBitmap;

	/**
	 * Read a single block and call the render functions on all map elements.
	 * 
	 * @throws IndexOutOfBoundsException
	 *             if the block contains invalid data.
	 * @throws UnsupportedEncodingException
	 *             if the string decoding fails.
	 */
	private void processBlock() throws IndexOutOfBoundsException, UnsupportedEncodingException {
		Logger.d("  processing block ...");
		// calculate the offset in the tile entries table and move the pointer there
		this.bufferPosition = (this.queryZoomLevel - this.tileZoomLevelMin) * 4;

		// read the amount of way and nodes on the current zoomLevel level
		this.nodesOnZoomLevel = Deserializer.toShort(this.readBuffer, this.bufferPosition);
		this.bufferPosition += 2;
		this.waysOnZoomLevel = Deserializer.toShort(this.readBuffer, this.bufferPosition);
		Logger.d("    nodesOnZoomLevel: " + nodesOnZoomLevel);
		Logger.d("    waysOnZoomLevel: " + waysOnZoomLevel);

		// move the pointer to the end of the tile entries table
		this.bufferPosition = this.tileEntriesTableSize;

		// read the offset to the first stored way in the block (8 bytes)
		this.firstWayOffset = Deserializer.toLong(this.readBuffer, this.bufferPosition);
		this.bufferPosition += 8;

		// read nodes
		for (this.elementCounter = this.nodesOnZoomLevel; this.elementCounter != 0; --this.elementCounter) {
			Logger.d("    reading node");
			// read node latitude (4 bytes)
			this.nodeLatitude = Deserializer.toInt(this.readBuffer, this.bufferPosition);
			this.bufferPosition += 4;
			Logger.d("      nodeLatitude: " + nodeLatitude);

			// read node longitude (4 bytes)
			this.nodeLongitude = Deserializer.toInt(this.readBuffer, this.bufferPosition);
			this.bufferPosition += 4;
			Logger.d("      nodeLongitude: " + nodeLongitude);

			// read the special byte that encodes multiple fields (1 byte)
			this.nodeSpecialByte = this.readBuffer[this.bufferPosition];
			this.bufferPosition += 1;
			Logger.d("      nodeSpecialByte: " + nodeSpecialByte);

			// bit 1-4 of the special byte represent the node layer
			this.nodeLayer = (byte) ((this.nodeSpecialByte & 0xf0) >> 4);
			Logger.d("      nodeLayer: " + nodeLayer);
			// bit 5-7 of the special byte represent the number of tag IDs
			this.nodeNumberOfTags = (byte) ((this.nodeSpecialByte & 0x0e) >> 1);
			Logger.d("      nodeNumberOfTags: " + nodeNumberOfTags);

			// reset the node tag array
			System.arraycopy(this.defaultNodeTagIds, 0, this.nodeTagIds, 0,
					this.defaultNodeTagIds.length);
			// read node tag IDs (1 byte per tag ID)
			for (this.tempByte = this.nodeNumberOfTags; this.tempByte != 0; --this.tempByte) {
				this.nodeTagId = this.readBuffer[this.bufferPosition];
				this.bufferPosition += 1;
				if (this.nodeTagId < 0 || this.nodeTagId >= this.nodeTagIds.length) {
					Logger.d("        invalid nodeTagId: " + this.nodeTagId);
					continue;
				}
				this.nodeTagIds[this.nodeTagId] = true;
				Logger.d("        nodeTagId: " + this.nodeTagId);
			}

			// read the feature byte that activates optional node features (1 byte)
			this.nodeFeatureByte = this.readBuffer[this.bufferPosition];
			this.bufferPosition += 1;
			Logger.d("      nodeFeatureByte: " + this.nodeFeatureByte);

			// check if the node has a name
			this.nodeFeatureName = (this.nodeFeatureByte & 0x80) != 0;
			if (this.nodeFeatureName) {
				Logger.d("      nodeFeatureName");
				// get the length of the node name (2 bytes)
				this.stringLength = Deserializer.toShort(this.readBuffer, this.bufferPosition);
				this.bufferPosition += 2;
				Logger.d("        stringLength: " + this.stringLength);
				if (this.stringLength > 0) {
					// read the node name
					this.nodeName = new String(this.readBuffer, this.bufferPosition,
							this.stringLength, "UTF-8");
					this.bufferPosition += this.stringLength;
					Logger.d("        nodeName: " + this.nodeName);
				} else {
					Logger.d("  invalid stringLength: " + stringLength);
					this.nodeName = null;
				}
			} else {
				this.nodeName = null;
			}

			// check if the node has an elevation
			this.nodeFeatureElevation = (this.nodeFeatureByte & 0x40) != 0;
			if (this.nodeFeatureElevation) {
				Logger.d("      nodeFeatureElevation");
				// get the node elevation (2 bytes)
				this.nodeElevation = Deserializer.toShort(this.readBuffer, this.bufferPosition);
				this.bufferPosition += 2;
				Logger.d("        nodeElevation: " + this.nodeElevation);
			} else {
				this.nodeElevation = -1;
			}

			// check if the node has a house number
			this.nodeFeatureHouseNumber = (this.nodeFeatureByte & 0x20) != 0;
			if (this.nodeFeatureHouseNumber) {
				Logger.d("      nodeFeatureHouseNumber");
				// get the length of the node house number (2 bytes)
				this.stringLength = Deserializer.toShort(this.readBuffer, this.bufferPosition);
				this.bufferPosition += 2;
				Logger.d("        stringLength: " + this.stringLength);
				if (this.stringLength > 0) {
					// read the node house number
					this.nodeHouseNumber = new String(this.readBuffer, this.bufferPosition,
							this.stringLength, "UTF-8");
					this.bufferPosition += this.stringLength;
					Logger.d("        nodeHouseNumber: " + this.nodeHouseNumber);
				} else {
					Logger.d("  invalid stringLength: " + stringLength);
					this.nodeHouseNumber = null;
				}
			} else {
				this.nodeHouseNumber = null;
			}

			// render the node
			// TODO: send optional node fields to the MapGenerator
			this.queryMapGenerator.renderPointOfInterest(this.nodeLayer, this.nodeLatitude,
					this.nodeLongitude, this.nodeName, this.nodeTagIds);
		}

		// finished reading nodes, now move the pointer to the first way
		this.bufferPosition = (int) this.firstWayOffset;
		Logger.d("    moving to firstWayOffset: " + firstWayOffset);

		// read ways
		for (this.elementCounter = this.waysOnZoomLevel; this.elementCounter != 0; --this.elementCounter) {
			Logger.d("    reading way");
			// read the size of the way (4 bytes)
			this.waySize = Deserializer.toInt(this.readBuffer, this.bufferPosition);
			this.bufferPosition += 4;

			// read the way tile bitmap (2 bytes)
			this.wayTileBitmap = Deserializer.toShort(this.readBuffer, this.bufferPosition);
			this.bufferPosition += 2;
			// TODO: do something useful with this bitmap

			// read the first special byte that encodes multiple fields (1 byte)
			this.waySpecialByte1 = this.readBuffer[this.bufferPosition];
			this.bufferPosition += 1;

			// bit 1-4 of the first special byte represent the way layer
			this.wayLayer = (byte) ((this.waySpecialByte1 & 0xf0) >> 4);
			// bit 5-7 of the first special byte represent the number of tag IDs
			this.wayNumberOfTags = (byte) ((this.waySpecialByte1 & 0x0e) >> 1);

			// read the second special byte that encodes multiple fields (1 byte)
			this.waySpecialByte2 = this.readBuffer[this.bufferPosition];
			this.bufferPosition += 1;

			// bit 1-3 of the second special byte represent the number of relevant tags
			this.wayNumberOfRelevantTags = (byte) ((this.waySpecialByte2 & 0xe0) >> 5);

			// read the way tag bitmap (1 byte)
			this.wayTagBitmap = this.readBuffer[this.bufferPosition];
			this.bufferPosition += 1;

			// reset the way tag array
			System.arraycopy(this.defaultWayTagIds, 0, this.wayTagIds, 0,
					this.defaultWayTagIds.length);
			// read way tag IDs (1 byte per tag ID)
			for (this.tempByte = this.wayNumberOfTags; this.tempByte != 0; --this.tempByte) {
				this.wayTagId = this.readBuffer[this.bufferPosition];
				this.bufferPosition += 1;
				if (this.wayTagId < 0 || this.wayTagId >= this.wayTagIds.length) {
					Logger.d("        invalid wayTagId: " + this.wayTagId);
					continue;
				}
				this.wayTagIds[this.wayTagId] = true;
				Logger.d("        wayTagId: " + this.wayTagId);
			}

			// read the number of way nodes (2 bytes)
			this.wayNumberOfWayNodes = Deserializer.toShort(this.readBuffer,
					this.bufferPosition);
			this.bufferPosition += 2;

			// each way node consists of latitude and longitude fields
			this.wayNodesSequenceLength = (short) (this.wayNumberOfWayNodes * 2);

			// read the way nodes
			this.wayNodesSequence = new int[this.wayNumberOfWayNodes];
			for (this.tempShort = 0; this.tempShort < this.wayNodesSequenceLength; this.tempShort += 2) {
				// read way node latitude (4 bytes)
				this.wayNodeLatitude = Deserializer.toInt(this.readBuffer, this.bufferPosition);
				this.bufferPosition += 4;
				// read way node longitude (4 bytes)
				this.wayNodeLongitude = Deserializer
						.toInt(this.readBuffer, this.bufferPosition);
				this.bufferPosition += 4;
				this.wayNodesSequence[this.tempShort] = this.wayNodeLongitude;
				this.wayNodesSequence[this.tempShort + 1] = this.wayNodeLatitude;
			}

			// read the feature byte that activates optional way features (1 byte)
			this.wayFeatureByte = this.readBuffer[this.bufferPosition];
			this.bufferPosition += 1;

			// check if the way has a name
			this.wayFeatureName = (this.wayFeatureByte & 0x80) != 0;
			if (this.wayFeatureName) {
				// get the length of the way name (2 bytes)
				this.stringLength = Deserializer.toShort(this.readBuffer, this.bufferPosition);
				this.bufferPosition += 2;
				if (this.stringLength > 0) {
					if (this.queryReadWayNames) {
						// read the way name
						this.wayName = new String(this.readBuffer, this.bufferPosition,
								this.stringLength, "UTF-8");
					} else {
						this.wayName = null;
					}
					this.bufferPosition += this.stringLength;
					Logger.d("    wayName: " + this.wayName);
				} else {
					Logger.d("  invalid stringLength: " + stringLength);
					this.wayName = null;
				}
			} else {
				this.wayName = null;
			}

			// check if the way has a label position
			this.wayFeatureLabelPosition = (this.wayFeatureByte & 0x40) != 0;
			if (this.wayFeatureLabelPosition) {
				// read the label position latitude (4 bytes)
				this.wayLabelPositionLatitude = Deserializer.toInt(this.readBuffer,
						this.bufferPosition);
				this.bufferPosition += 4;
				// read the label position longitude (4 bytes)
				this.wayLabelPositionLongitude = Deserializer.toInt(this.readBuffer,
						this.bufferPosition);
				this.bufferPosition += 4;
			} // TODO: if no label position exists, mark the old values as invalid

			// check if the way represents a closed area
			this.wayFeatureArea = (this.wayFeatureByte & 0x20) != 0;

			// check if the way represents a multipolygon
			this.wayFeatureMultipolygon = (this.wayFeatureByte & 0x10) != 0;
			if (this.wayFeatureMultipolygon) {
				// read the amount of inner ways (1 byte)
				this.wayNumberOfInnerWays = this.readBuffer[this.bufferPosition];
				this.bufferPosition += 1;

				if (this.wayNumberOfInnerWays > 0) {
					// create a two-dimensional array for the inner way coordinates
					this.wayInnerWays = new int[this.wayNumberOfInnerWays][];

					// for each inner way
					for (this.tempByte = (byte) (this.wayNumberOfInnerWays - 1); this.tempByte >= 0; --this.tempByte) {
						// read the number of way nodes (2 bytes)
						this.innerWayNumberOfWayNodes = Deserializer.toShort(this.readBuffer,
								this.bufferPosition);
						this.bufferPosition += 2;

						// each way node consists of latitude and longitude fields
						this.innerWayNodesSequenceLength = (short) (this.innerWayNumberOfWayNodes * 2);

						this.innerWay = new int[this.innerWayNodesSequenceLength];
						for (this.tempShort = 0; this.tempShort < this.innerWayNodesSequenceLength; this.tempShort += 2) {
							// read inner way node latitude (4 bytes)
							this.wayNodeLatitude = Deserializer.toInt(this.readBuffer,
									this.bufferPosition);
							this.bufferPosition += 4;
							// read inner way node longitude (4 bytes)
							this.wayNodeLongitude = Deserializer.toInt(this.readBuffer,
									this.bufferPosition);
							this.bufferPosition += 4;
							this.innerWay[this.tempShort] = this.wayNodeLongitude;
							this.innerWay[this.tempShort + 1] = this.wayNodeLatitude;
						}
						this.wayInnerWays[this.tempByte] = this.innerWay;
					}
				} else {
					this.wayInnerWays = null;
				}
			} else {
				this.wayInnerWays = null;
			}

			// render the way
			// TODO: send optional way fields to the MapGenerator
			this.queryMapGenerator.renderWay(this.wayLayer, this.wayNumberOfRelevantTags,
					this.wayName, this.wayTagIds, this.wayTagBitmap,
					this.wayNodesSequenceLength, this.wayNodesSequence, this.wayInnerWays);
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
		final int FIXED_HEADER_SIZE = BINARY_OSM_MAGIC_BYTE.length() + 40;
		this.readBuffer = new byte[FIXED_HEADER_SIZE];

		// read the fixed size part of the header in the buffer to avoid multiple reads
		if (this.inputFile.read(this.readBuffer, 0, FIXED_HEADER_SIZE) != FIXED_HEADER_SIZE) {
			return false;
		}
		this.bufferPosition = 0;

		// check the magic byte
		this.tempString = new String(this.readBuffer, this.bufferPosition,
				BINARY_OSM_MAGIC_BYTE.length(), "UTF-8");
		this.bufferPosition += BINARY_OSM_MAGIC_BYTE.length();
		Logger.d("  magic byte: " + this.tempString);
		if (!this.tempString.equals(BINARY_OSM_MAGIC_BYTE)) {
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

		// get and check the tile pixel size (2 bytes)
		this.tilePixelSize = Deserializer.toShort(this.readBuffer, this.bufferPosition);
		this.bufferPosition += 2;
		Logger.d("  tilePixelSize: " + this.tilePixelSize);
		if (this.tilePixelSize < 0) {
			Logger.d("invalid tilePixelSize: " + this.tilePixelSize);
			return false;
		}

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

		// get and check the the top boundary (4 bytes)
		this.boundaryTop = Deserializer.toInt(this.readBuffer, this.bufferPosition);
		this.bufferPosition += 4;
		Logger.d("  boundaryTop: " + this.boundaryTop);
		if (this.boundaryTop > 90000000) {
			Logger.d("invalid top boundary: " + this.boundaryTop);
			return false;
		}

		// get and check the left boundary (4 bytes)
		this.boundaryLeft = Deserializer.toInt(this.readBuffer, this.bufferPosition);
		this.bufferPosition += 4;
		Logger.d("  boundaryLeft: " + this.boundaryLeft);
		if (this.boundaryLeft < -180000000) {
			Logger.d("invalid left boundary: " + this.boundaryLeft);
			return false;
		}

		// get and check the bottom boundary (4 bytes)
		this.boundaryBottom = Deserializer.toInt(this.readBuffer, this.bufferPosition);
		this.bufferPosition += 4;
		Logger.d("  boundaryBottom: " + this.boundaryBottom);
		if (this.boundaryBottom < -90000000) {
			Logger.d("invalid bottom boundary: " + this.boundaryBottom);
			return false;
		}

		// get and check the right boundary (4 bytes)
		this.boundaryRight = Deserializer.toInt(this.readBuffer, this.bufferPosition);
		this.bufferPosition += 4;
		Logger.d("  boundaryRight: " + this.boundaryRight);
		if (this.boundaryRight > 180000000) {
			Logger.d("invalid right boundary: " + this.boundaryRight);
			return false;
		}

		// calculate the XY numbers of the boundary tiles
		this.boundaryLeftTile = MercatorProjection.longitudeToTileX(
				this.boundaryLeft / 1000000d, this.baseZoomLevel);
		this.boundaryTopTile = MercatorProjection.latitudeToTileY(this.boundaryTop / 1000000d,
				this.baseZoomLevel);
		this.boundaryRightTile = MercatorProjection.longitudeToTileX(
				this.boundaryRight / 1000000d, this.baseZoomLevel);
		this.boundaryBottomTile = MercatorProjection.latitudeToTileY(
				this.boundaryBottom / 1000000d, this.baseZoomLevel);
		Logger.d("    boundaryLeftTile: " + this.boundaryLeftTile);
		Logger.d("    boundaryTopTile: " + this.boundaryTopTile);
		Logger.d("    boundaryRightTile: " + this.boundaryRightTile);
		Logger.d("    boundaryBottomTile: " + this.boundaryBottomTile);

		// calculate the horizontal and vertical amount of blocks in the file
		this.mapFileBlocksWidth = this.boundaryRightTile - this.boundaryLeftTile + 1;
		Logger.d("  mapFileBlocksWidth: " + this.mapFileBlocksWidth);
		this.mapFileBlocksHeight = this.boundaryBottomTile - this.boundaryTopTile + 1;
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

		// get and check the maximum tile size (5 bytes)
		this.maximumTileSize = Deserializer.fiveBytesToLong(this.readBuffer,
				this.bufferPosition);
		this.bufferPosition += 5;
		Logger.d("  maximumTileSize: " + this.maximumTileSize);
		if (this.maximumTileSize < 0) {
			Logger.d("invalid maximum tile size: " + this.maximumTileSize);
			return false;
		}

		// get the length of the comment text (2 bytes)
		this.stringLength = Deserializer.toShort(this.readBuffer, this.bufferPosition);
		this.bufferPosition += 2;
		Logger.d("  commentText length: " + this.stringLength);
		if (this.stringLength > 0) {
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
		} else {
			Logger.d("  invalid stringLength: " + stringLength);
			this.commentText = null;
		}

		// save the end address of the header where the index starts
		this.indexStartAddress = FIXED_HEADER_SIZE + this.stringLength;

		Logger.d("finished file header");
		return true;
	}

	/**
	 * Closes the map file.
	 */
	void closeFile() {
		try {
			if (this.databaseIndexCache != null) {
				this.databaseIndexCache.destroy();
				this.databaseIndexCache = null;
			}

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
			Logger.d("executing query ...");
			Logger.d("  tile: " + tile.x + ", " + tile.y + ", " + tile.zoomLevel);
			this.stopCurrentQuery = false;
			if (tile.zoomLevel > this.tileZoomLevelMax) {
				this.queryZoomLevel = this.tileZoomLevelMax;
			} else {
				this.queryZoomLevel = tile.zoomLevel;
			}
			this.queryReadWayNames = readWayNames;
			this.queryMapGenerator = mapGenerator;

			// TODO: for-loop needed for all base tiles within the tile
			if (tile.zoomLevel >= this.baseZoomLevel) {
				// check if the current query was interrupted
				if (this.stopCurrentQuery) {
					return;
				}

				// calculate the appropriate tile in the base zoom level
				double baseTileLongitude = MercatorProjection.tileXToLongitude(tile.x,
						tile.zoomLevel);
				double baseTileLatitude = MercatorProjection.tileYToLatitude(tile.y,
						tile.zoomLevel);

				long baseTileX = MercatorProjection.longitudeToTileX(baseTileLongitude,
						this.baseZoomLevel);
				long baseTileY = MercatorProjection.latitudeToTileY(baseTileLatitude,
						this.baseZoomLevel);
				Logger.d("  baseTileX: " + baseTileX);
				Logger.d("  baseTileY: " + baseTileY);

				// calculate the position of the needed block in the file
				long neededBlockX = baseTileX - this.boundaryLeftTile;
				long neededBlockY = baseTileY - this.boundaryTopTile;
				Logger.d("  neededBlockX: " + neededBlockX);
				Logger.d("  neededBlockY: " + neededBlockY);

				// calculate the block number of the needed block in the file
				this.blockNumber = neededBlockY * this.mapFileBlocksWidth + neededBlockX;
				Logger.d("  blockNumber: " + this.blockNumber);

				// get and check the pointer to the current block
				this.currentBlockPointer = this.databaseIndexCache.getAddress(this.blockNumber);
				if (this.currentBlockPointer < 0
						|| this.currentBlockPointer > this.inputFileSize) {
					Logger.d("invalid currentBlockPointer: " + this.currentBlockPointer);
					return;
				}
				Logger.d("  currentBlockPointer: " + this.currentBlockPointer);

				// get and check the pointer to the next block
				this.nextBlockPointer = this.databaseIndexCache
						.getAddress(this.blockNumber + 1);
				if (this.nextBlockPointer < 0 || this.nextBlockPointer > this.inputFileSize) {
					Logger.d("invalid nextBlockPointer: " + this.nextBlockPointer);
					return;
				}
				Logger.d("  nextBlockPointer: " + this.nextBlockPointer);

				// check if the next block has a valid pointer
				if (this.nextBlockPointer == -1) {
					// the current block is the last one in the file
					this.nextBlockPointer = this.inputFile.length();
				}

				// calculate the size of the current block
				this.currentBlockSize = (int) (this.nextBlockPointer - this.currentBlockPointer);
				Logger.d("  currentBlockSize: " + currentBlockSize);

				// if the current block has no map data continue with the next one
				if (this.currentBlockSize == 0) {
					Logger.d("  current block is empty");
					return;
				}

				// go to the current block and read its data to the buffer
				this.inputFile.seek(this.currentBlockPointer);
				Logger.d("  reading current block");
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
		Logger.d("execution finished");
	}

	/**
	 * Returns the area coordinates of the current map file.
	 * 
	 * @return the area coordinates in microdegrees.
	 */
	Rect getMapBoundary() {
		return new Rect(boundaryLeft, boundaryTop, boundaryRight, boundaryBottom);
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
			// make sure to close any previous file first
			closeFile();

			this.inputFile = new RandomAccessFile(fileName, "r");
			if (!readFileHeader()) {
				return false;
			}

			// get the size of the file
			this.inputFileSize = this.inputFile.length();

			// create the DatabaseIndexCache
			this.databaseIndexCache = new DatabaseIndexCacheNew(this.inputFile,
					this.mapFileBlocks, this.indexStartAddress, INDEX_CACHE_SIZE);

			// create a read buffer that is big enough even for the largest tile
			this.readBuffer = new byte[(int) this.maximumTileSize];

			// calculate the size of the tile entries table
			this.tileEntriesTableSize = 2 * (this.tileZoomLevelMax - this.tileZoomLevelMin + 1) * 2;

			// create the tag arrays
			this.defaultNodeTagIds = new boolean[Byte.MAX_VALUE];
			this.nodeTagIds = new boolean[Byte.MAX_VALUE];
			this.defaultWayTagIds = new boolean[Byte.MAX_VALUE];
			this.wayTagIds = new boolean[Byte.MAX_VALUE];

			return true;
		} catch (IOException e) {
			Logger.e(e);
			// make sure that the file is closed
			closeFile();
			return false;
		}
	}

	/**
	 * This method exists only for compatibility reasons with the old database class.
	 * 
	 * @param fileName
	 *            the path to the map file.
	 * @return true if the file could be opened and is a valid map file, false otherwise.
	 */
	boolean setFile(String fileName) {
		return openFile(fileName);
	}

	void stopCurrentQuery() {
		this.stopCurrentQuery = true;
	}
}