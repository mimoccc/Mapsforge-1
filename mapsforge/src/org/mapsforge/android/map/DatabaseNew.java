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

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;

/**
 * A database class for reading binary OSM files. Byte order is big-endian.
 */
public class DatabaseNew {
	/**
	 * The magic byte at the beginning of a valid binary map file.
	 */
	private static final String BINARY_OSM_MAGIC_BYTE = "mapsforge binary OSM";

	/**
	 * The version of the binary map file that this implementation supports.
	 */
	private static final int BINARY_OSM_VERSION = 1;

	/**
	 * The flag to indicate, if the binary map file contains debug signatures.
	 */
	private static final boolean DEBUG_FILE = !false;

	/**
	 * The size of the fixed header in a binary map file in bytes.
	 */
	private static final int FIXED_HEADER_SIZE = BINARY_OSM_MAGIC_BYTE.length() + 38;

	/**
	 * The size of the index cache for each contained map file in bytes.
	 */
	private static final int INDEX_CACHE_SIZE = 32;

	/**
	 * The initial length of the way nodes array.
	 */
	private static final int INITIAL_WAY_NODES_CAPACITY = 2048;

	/**
	 * The size of the header data for each contained map file in bytes.
	 */
	private static final int MAP_FILE_HEADER_SIZE = 13;

	/**
	 * The length of the debug signature at the beginning of each block.
	 */
	private static final byte SIGNATURE_LENGTH_BLOCK = 32;

	/**
	 * The length of the debug signature at the beginning of the index.
	 */
	private static final byte SIGNATURE_LENGTH_INDEX = 16;

	/**
	 * The length of the debug signature at the beginning of each node.
	 */
	private static final byte SIGNATURE_LENGTH_NODE = 32;

	/**
	 * The length of the debug signature at the beginning of each way.
	 */
	private static final byte SIGNATURE_LENGTH_WAY = 32;

	private byte baseZoomLevel;
	private long blockNumber;
	private int boundaryBottom;
	private int boundaryLeft;
	private int boundaryRight;
	private int boundaryTop;
	private int bufferPosition;
	private String commentText;
	private long currentBlockPointer;
	private int currentBlockSize;
	private long currentColumn;
	private long currentRow;
	private DatabaseIndexCacheNew databaseIndexCache;
	private boolean[] defaultNodeTagIds;
	private boolean[] defaultWayTagIds;
	private short elementCounter;
	private File file;
	private long fileSize;
	private int firstWayOffset;
	private long fromBaseTileX;
	private long fromBaseTileY;
	private long fromBlockX;
	private long fromBlockY;
	private byte globalMaximumZoomLevel;
	private byte globalMinimumZoomLevel;
	private long indexStartAddress;
	private int[] innerWay;
	private short innerWayNodesSequenceLength;
	private short innerWayNumberOfWayNodes;
	private RandomAccessFile inputFile;
	private Rect mapBoundary;
	private long mapDate;
	private MapFile mapFile;
	private long mapFileSize;
	private MapFile[] mapFilesList;
	private MapFile[] mapFilesLookupTable;
	private long maximumBlockSize;
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
	private byte numberOfMapFiles;
	private long parentTileX;
	private long parentTileY;
	private boolean queryReadWayNames;
	private int queryTileBitmask;
	private int queryZoomLevel;
	private byte[] readBuffer;
	private boolean stopCurrentQuery;
	private short stringLength;
	private long subtileX;
	private long subtileY;
	private byte tempByte;
	private int tempInt;
	private short tempShort;
	private String tempString;
	private int tileEntriesTableOffset;
	private short tilePixelSize;
	private long toBaseTileX;
	private long toBaseTileY;
	private long toBlockX;
	private long toBlockY;
	private boolean useTileBitmask;
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
	private byte wayNumberOfRelevantTags;
	private byte wayNumberOfTags;
	private short wayNumberOfWayNodes;
	private int waySize;
	private short waysOnZoomLevel;
	private byte waySpecialByte1;
	private byte waySpecialByte2;
	private byte wayTagBitmap;
	private byte wayTagId;
	private boolean[] wayTagIds;
	private short wayTileBitmask;
	private int zoomLevelDifference;
	private byte zoomLevelMax;
	private byte zoomLevelMin;

	/**
	 * Empty default constructor with limited visibility.
	 */
	DatabaseNew() {
		// do nothing
	}

	/**
	 * Returns the comment text of the binary map file.
	 * 
	 * @return the comment text of the binary map file.
	 */
	public String getCommentText() {
		return this.commentText;
	}

	/**
	 * Returns the center coordinates of the current map file.
	 * 
	 * @return the area coordinates in microdegrees.
	 */
	public GeoPoint getMapCenter() {
		return this.mapBoundary.getCenter();
	}

	/**
	 * Returns the date of the map data in the binary map file.
	 * 
	 * @return the date of the map data.
	 */
	public long getMapDate() {
		return this.mapDate;
	}

	/**
	 * Reads a single block and calls the render functions on all map elements.
	 * 
	 * @param mapGenerator
	 *            the MapGenerator callback which handles the extracted map elements.
	 * @throws IndexOutOfBoundsException
	 *             if the block contains invalid data.
	 * @throws UnsupportedEncodingException
	 *             if the string decoding fails.
	 */
	private void processBlock(DatabaseMapGenerator mapGenerator)
			throws IndexOutOfBoundsException, UnsupportedEncodingException {
		if (DEBUG_FILE) {
			// check read and the block signature
			this.tempString = new String(this.readBuffer, this.bufferPosition,
					SIGNATURE_LENGTH_BLOCK, "UTF-8");
			this.bufferPosition += SIGNATURE_LENGTH_BLOCK;
			if (!this.tempString.startsWith("###TileStart")) {
				Logger.d("invalid block signature: " + this.tempString);
				return;
			}
			// Logger.d("valid block signature: " + this.tempString);
		}

		// calculate the offset in the tile entries table and move the pointer
		this.tileEntriesTableOffset = (this.queryZoomLevel - this.mapFile.zoomLevelMin) * 4;
		this.bufferPosition += this.tileEntriesTableOffset;

		// read the amount of way and nodes on the current zoomLevel level
		this.nodesOnZoomLevel = Deserializer.toShort(this.readBuffer, this.bufferPosition);
		this.bufferPosition += 2;
		// Logger.d("  nodesOnZoomLevel: " + this.nodesOnZoomLevel);
		this.waysOnZoomLevel = Deserializer.toShort(this.readBuffer, this.bufferPosition);
		this.bufferPosition += 2;
		// Logger.d("  waysOnZoomLevel: " + this.waysOnZoomLevel);

		// move the pointer to the end of the tile entries table
		this.bufferPosition += this.mapFile.tileEntriesTableSize - this.tileEntriesTableOffset
				- 4;

		// read the offset to the first stored way in the block (4 bytes)
		this.firstWayOffset = Deserializer.toInt(this.readBuffer, this.bufferPosition);
		this.bufferPosition += 4;
		if (this.firstWayOffset > this.readBuffer.length) {
			Logger.d("invalid first way offset: " + this.firstWayOffset);
			return;
		}
		// Logger.d("  firstWayOffset: " + this.firstWayOffset);

		// read nodes
		for (this.elementCounter = this.nodesOnZoomLevel; this.elementCounter != 0; --this.elementCounter) {
			if (DEBUG_FILE) {
				// read and check the node signature
				this.tempString = new String(this.readBuffer, this.bufferPosition,
						SIGNATURE_LENGTH_NODE, "UTF-8");
				this.bufferPosition += SIGNATURE_LENGTH_NODE;
				if (!this.tempString.startsWith("***POIStart")) {
					Logger.d("invalid node signature: " + this.tempString);
					return;
				}
				// Logger.d("valid node signature: " + this.tempString);
			}

			// read node latitude (4 bytes)
			this.nodeLatitude = Deserializer.toInt(this.readBuffer, this.bufferPosition);
			this.bufferPosition += 4;

			// read node longitude (4 bytes)
			this.nodeLongitude = Deserializer.toInt(this.readBuffer, this.bufferPosition);
			this.bufferPosition += 4;

			// read the special byte that encodes multiple fields (1 byte)
			this.nodeSpecialByte = this.readBuffer[this.bufferPosition];
			this.bufferPosition += 1;

			// bit 1-4 of the special byte represent the node layer
			this.nodeLayer = (byte) ((this.nodeSpecialByte & 0xf0) >> 4);
			// bit 5-8 of the special byte represent the number of tag IDs
			this.nodeNumberOfTags = (byte) (this.nodeSpecialByte & 0x0f);

			// reset the node tag array
			System.arraycopy(this.defaultNodeTagIds, 0, this.nodeTagIds, 0,
					this.nodeTagIds.length);
			// read node tag IDs (1 byte per tag ID)
			for (this.tempByte = this.nodeNumberOfTags; this.tempByte != 0; --this.tempByte) {
				this.nodeTagId = this.readBuffer[this.bufferPosition];
				this.bufferPosition += 1;
				if (this.nodeTagId < 0 || this.nodeTagId >= this.nodeTagIds.length) {
					Logger.d("invalid node tag ID: " + this.nodeTagId);
					continue;
				}
				this.nodeTagIds[this.nodeTagId] = true;
			}

			// read the feature byte that activates optional node features (1 byte)
			this.nodeFeatureByte = this.readBuffer[this.bufferPosition];
			this.bufferPosition += 1;

			// check if the node has a name
			this.nodeFeatureName = (this.nodeFeatureByte & 0x80) != 0;
			if (this.nodeFeatureName) {
				// get the length of the node name (2 bytes)
				this.stringLength = Deserializer.toShort(this.readBuffer, this.bufferPosition);
				this.bufferPosition += 2;
				if (this.stringLength > 0) {
					// read the node name
					this.nodeName = new String(this.readBuffer, this.bufferPosition,
							this.stringLength, "UTF-8");
					this.bufferPosition += this.stringLength;
				} else {
					Logger.d("invalid string length: " + this.stringLength);
					this.nodeName = null;
				}
			} else {
				this.nodeName = null;
			}

			// check if the node has an elevation
			this.nodeFeatureElevation = (this.nodeFeatureByte & 0x40) != 0;
			if (this.nodeFeatureElevation) {
				// get the node elevation (2 bytes)
				this.nodeElevation = Deserializer.toShort(this.readBuffer, this.bufferPosition);
				this.bufferPosition += 2;
			} else {
				this.nodeElevation = -1;
			}

			// check if the node has a house number
			this.nodeFeatureHouseNumber = (this.nodeFeatureByte & 0x20) != 0;
			if (this.nodeFeatureHouseNumber) {
				// get the length of the node house number (2 bytes)
				this.stringLength = Deserializer.toShort(this.readBuffer, this.bufferPosition);
				this.bufferPosition += 2;
				if (this.stringLength > 0) {
					// read the node house number
					this.nodeHouseNumber = new String(this.readBuffer, this.bufferPosition,
							this.stringLength, "UTF-8");
					this.bufferPosition += this.stringLength;
				} else {
					Logger.d("invalid string length: " + this.stringLength);
					this.nodeHouseNumber = null;
				}
			} else {
				this.nodeHouseNumber = null;
			}

			// render the node
			// TODO: send optional node fields to the MapGenerator
			mapGenerator.renderPointOfInterest(this.nodeLayer, this.nodeLatitude,
					this.nodeLongitude, this.nodeName, this.nodeTagIds);
		}

		// FIXME: remove both 32 byte offsets!!!
		// finished reading nodes, check if the buffer position is valid
		if (this.bufferPosition > this.firstWayOffset + 32) {
			Logger.d("invalid buffer position:" + this.bufferPosition + " - "
					+ this.firstWayOffset);
			return;
		}
		// move the pointer to the first way
		this.bufferPosition = this.firstWayOffset + 32;

		// read ways
		for (this.elementCounter = this.waysOnZoomLevel; this.elementCounter != 0; --this.elementCounter) {
			if (DEBUG_FILE) {
				// read and check the way signature
				this.tempString = new String(this.readBuffer, this.bufferPosition,
						SIGNATURE_LENGTH_WAY, "UTF-8");
				this.bufferPosition += SIGNATURE_LENGTH_WAY;
				if (!this.tempString.startsWith("---WayStart")) {
					Logger.d("invalid way signature: " + this.tempString);
					return;
				}
				// Logger.d("valid way signature: " + this.tempString);
			}

			// read the size of the way (4 bytes)
			this.waySize = Deserializer.toInt(this.readBuffer, this.bufferPosition);
			this.bufferPosition += 4;

			if (this.useTileBitmask) {
				// read the way tile bitmask (2 bytes)
				this.wayTileBitmask = Deserializer
						.toShort(this.readBuffer, this.bufferPosition);
				this.bufferPosition += 2;
				// check if the way is inside the requested tile
				if ((this.queryTileBitmask & this.wayTileBitmask) == 0) {
					// skip the way and continue with the next way
					this.bufferPosition += this.waySize - 2;
					continue;
				}
			} else {
				// ignore the way tile bitmask (2 bytes)
				this.bufferPosition += 2;
			}

			// read the first special byte that encodes multiple fields (1 byte)
			this.waySpecialByte1 = this.readBuffer[this.bufferPosition];
			this.bufferPosition += 1;

			// bit 1-4 of the first special byte represent the way layer
			this.wayLayer = (byte) ((this.waySpecialByte1 & 0xf0) >> 4);
			// bit 5-8 of the first special byte represent the number of tag IDs
			this.wayNumberOfTags = (byte) (this.waySpecialByte1 & 0x0f);

			// read the second special byte that encodes multiple fields (1 byte)
			this.waySpecialByte2 = this.readBuffer[this.bufferPosition];
			this.bufferPosition += 1;

			// bit 1-3 of the second special byte represent the number of relevant tags
			this.wayNumberOfRelevantTags = (byte) ((this.waySpecialByte2 & 0xe0) >> 5);

			// read the way tag bitmap (1 byte)
			this.wayTagBitmap = this.readBuffer[this.bufferPosition];
			this.bufferPosition += 1;

			// reset the way tag array
			System
					.arraycopy(this.defaultWayTagIds, 0, this.wayTagIds, 0,
							this.wayTagIds.length);
			// read way tag IDs (1 byte per tag ID)
			for (this.tempByte = this.wayNumberOfTags; this.tempByte != 0; --this.tempByte) {
				this.wayTagId = this.readBuffer[this.bufferPosition];
				this.bufferPosition += 1;
				if (this.wayTagId < 0 || this.wayTagId >= this.wayTagIds.length) {
					Logger.d("invalid way tag ID: " + this.wayTagId);
					continue;
				}
				this.wayTagIds[this.wayTagId] = true;
			}

			// read the number of way nodes (2 bytes)
			this.wayNumberOfWayNodes = Deserializer.toShort(this.readBuffer,
					this.bufferPosition);
			this.bufferPosition += 2;

			// each way node consists of latitude and longitude fields
			this.wayNodesSequenceLength = (short) (this.wayNumberOfWayNodes * 2);

			// make sure that the array for the way nodes is large enough
			if (this.wayNodesSequenceLength > this.wayNodesSequence.length) {
				this.wayNodesSequence = new int[this.wayNodesSequenceLength];
			}

			// read the way nodes
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
				} else {
					Logger.d("invalid string length: " + this.stringLength);
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
					// create a two-dimensional array for the coordinates of the inner ways
					this.wayInnerWays = new int[this.wayNumberOfInnerWays][];

					// for each inner way
					for (this.tempByte = (byte) (this.wayNumberOfInnerWays - 1); this.tempByte >= 0; --this.tempByte) {
						// read the number of inner way nodes (2 bytes)
						this.innerWayNumberOfWayNodes = Deserializer.toShort(this.readBuffer,
								this.bufferPosition);
						this.bufferPosition += 2;

						// each inner way node consists of latitude and longitude fields
						this.innerWayNodesSequenceLength = (short) (this.innerWayNumberOfWayNodes * 2);

						// create an array for the inner way coordinates
						this.innerWay = new int[this.innerWayNodesSequenceLength];

						// read the inner way nodes
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
					Logger.d("invalid way number of inner ways: " + this.wayNumberOfInnerWays);
					this.wayInnerWays = null;
				}
			} else {
				this.wayInnerWays = null;
			}

			// render the way
			// TODO: send optional way fields to the MapGenerator
			mapGenerator.renderWay(this.wayLayer, this.wayNumberOfRelevantTags, this.wayName,
					this.wayTagIds, this.wayTagBitmap, this.wayNodesSequenceLength,
					this.wayNodesSequence, this.wayInnerWays);
		}
	}

	/**
	 * Reads and processes the header block from the file.
	 * 
	 * @return true if the header was processed successfully, false otherwise.
	 * @throws IOException
	 *             if an error occurs while reading the file.
	 */
	private boolean readFileHeader() throws IOException {
		// read the fixed size part of the header in the buffer to avoid multiple reads
		this.readBuffer = new byte[FIXED_HEADER_SIZE];
		if (this.inputFile.read(this.readBuffer, 0, this.readBuffer.length) != this.readBuffer.length) {
			Logger.d("reading header data has failed");
			return false;
		}
		this.bufferPosition = 0;

		// check the magic byte
		this.tempString = new String(this.readBuffer, this.bufferPosition,
				BINARY_OSM_MAGIC_BYTE.length(), "UTF-8");
		this.bufferPosition += BINARY_OSM_MAGIC_BYTE.length();
		if (!this.tempString.equals(BINARY_OSM_MAGIC_BYTE)) {
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

		// get and check the number of contained map files (1 byte)
		this.numberOfMapFiles = this.readBuffer[this.bufferPosition];
		this.bufferPosition += 1;
		Logger.d("numberOfMapFiles: " + this.numberOfMapFiles);
		if (this.numberOfMapFiles < 1) {
			Logger.d("invalid number of contained map files: " + this.numberOfMapFiles);
			return false;
		}

		// get and check the tile pixel size (2 bytes)
		this.tilePixelSize = Deserializer.toShort(this.readBuffer, this.bufferPosition);
		this.bufferPosition += 2;
		if (this.tilePixelSize < 1) {
			Logger.d("invalid tile pixel size: " + this.tilePixelSize);
			return false;
		}

		// get and check the the top boundary (4 bytes)
		this.boundaryTop = Deserializer.toInt(this.readBuffer, this.bufferPosition);
		this.bufferPosition += 4;
		if (this.boundaryTop > 90000000) {
			Logger.d("invalid top boundary: " + this.boundaryTop);
			return false;
		}

		// get and check the left boundary (4 bytes)
		this.boundaryLeft = Deserializer.toInt(this.readBuffer, this.bufferPosition);
		this.bufferPosition += 4;
		if (this.boundaryLeft < -180000000) {
			Logger.d("invalid left boundary: " + this.boundaryLeft);
			return false;
		}

		// get and check the bottom boundary (4 bytes)
		this.boundaryBottom = Deserializer.toInt(this.readBuffer, this.bufferPosition);
		this.bufferPosition += 4;
		if (this.boundaryBottom < -90000000) {
			Logger.d("invalid bottom boundary: " + this.boundaryBottom);
			return false;
		}

		// get and check the right boundary (4 bytes)
		this.boundaryRight = Deserializer.toInt(this.readBuffer, this.bufferPosition);
		this.bufferPosition += 4;
		if (this.boundaryRight > 180000000) {
			Logger.d("invalid right boundary: " + this.boundaryRight);
			return false;
		}

		// create the map boundary rectangle
		this.mapBoundary = new Rect(this.boundaryLeft, this.boundaryTop, this.boundaryRight,
				this.boundaryBottom);

		// get and check the the map date (8 bytes)
		this.mapDate = Deserializer.toLong(this.readBuffer, this.bufferPosition);
		this.bufferPosition += 8;
		if (this.mapDate < 0) {
			Logger.d("invalid map date: " + this.mapDate);
			return false;
		}

		// get and check the maximum block size (5 bytes)
		this.maximumBlockSize = Deserializer.fiveBytesToLong(this.readBuffer,
				this.bufferPosition);
		this.bufferPosition += 5;
		if (this.maximumBlockSize < 1) {
			Logger.d("invalid maximum block size: " + this.maximumBlockSize);
			return false;
		}

		// get the length of the comment text (2 bytes)
		this.stringLength = Deserializer.toShort(this.readBuffer, this.bufferPosition);
		this.bufferPosition += 2;
		if (this.stringLength > 0) {
			// read the comment text
			this.readBuffer = new byte[this.stringLength];
			if (this.inputFile.read(this.readBuffer, 0, this.readBuffer.length) != this.readBuffer.length) {
				Logger.d("reading the comment text has failed");
				return false;
			}
			this.bufferPosition = 0;
			this.commentText = new String(this.readBuffer, this.bufferPosition,
					this.stringLength, "UTF-8");
			this.bufferPosition += this.stringLength;
		} else if (this.stringLength == 0) {
			this.commentText = null;
		} else {
			Logger.d("invalid string length: " + this.stringLength);
			return false;
		}

		// read the details for all contained map files
		this.readBuffer = new byte[this.numberOfMapFiles * MAP_FILE_HEADER_SIZE];
		if (this.inputFile.read(this.readBuffer, 0, this.readBuffer.length) != this.readBuffer.length) {
			Logger.d("reading map files data has failed");
			return false;
		}
		this.bufferPosition = 0;

		// create the list of all contained map files
		this.mapFilesList = new MapFile[this.numberOfMapFiles];
		this.globalMinimumZoomLevel = Byte.MAX_VALUE;
		this.globalMaximumZoomLevel = Byte.MIN_VALUE;

		// get and check the information for each contained map file
		for (this.tempByte = 0; this.tempByte < this.numberOfMapFiles; ++this.tempByte) {
			// get and check the base zoom level (1 byte)
			this.baseZoomLevel = this.readBuffer[this.bufferPosition];
			this.bufferPosition += 1;
			Logger.d("base zooom level: " + this.baseZoomLevel);
			if (this.baseZoomLevel < 0 || this.baseZoomLevel > 21) {
				Logger.d("invalid base zooom level: " + this.baseZoomLevel);
				return false;
			}

			// get and check the minimum zoom level (1 byte)
			this.zoomLevelMin = this.readBuffer[this.bufferPosition];
			this.bufferPosition += 1;
			Logger.d("minimum zoom level: " + this.zoomLevelMin);
			if (this.zoomLevelMin < 0 || this.zoomLevelMin > 21) {
				Logger.d("invalid minimum zoom level: " + this.zoomLevelMin);
				return false;
			}

			// get and check the maximum zoom level (1 byte)
			this.zoomLevelMax = this.readBuffer[this.bufferPosition];
			this.bufferPosition += 1;
			Logger.d("maximum zoom level: " + this.zoomLevelMax);
			if (this.zoomLevelMax < 0 || this.zoomLevelMax > 21) {
				Logger.d("invalid maximum zoom level: " + this.zoomLevelMax);
				return false;
			}

			// check for valid zoom level range
			if (this.zoomLevelMin > this.zoomLevelMax) {
				Logger.d("invalid zoom level range: " + this.zoomLevelMin + " - "
						+ this.zoomLevelMax);
				return false;
			}

			// get and check the index start address of the map file (5 bytes)
			this.indexStartAddress = Deserializer.fiveBytesToLong(this.readBuffer,
					this.bufferPosition);
			this.bufferPosition += 5;
			Logger.d("index start address: " + this.indexStartAddress);
			if (this.indexStartAddress < 1 || this.indexStartAddress >= this.fileSize) {
				Logger.d("invalid index start address: " + this.indexStartAddress);
				return false;
			}

			if (DEBUG_FILE) {
				// the beginning of the index is marked with a signature
				this.indexStartAddress += SIGNATURE_LENGTH_INDEX;
			}

			// get and check the size of the map file (5 bytes)
			this.mapFileSize = Deserializer.fiveBytesToLong(this.readBuffer,
					this.bufferPosition);
			this.bufferPosition += 5;
			Logger.d("map file size: " + this.mapFileSize);
			if (this.mapFileSize < 1) {
				Logger.d("invalid map file size: " + this.mapFileSize);
				return false;
			}

			// add the current map file to the map files list
			this.mapFilesList[this.tempByte] = new MapFile(this.indexStartAddress,
					this.mapFileSize, this.baseZoomLevel, this.zoomLevelMin, this.zoomLevelMax,
					this.mapBoundary);

			// update the global minimum and maximum zoom level information
			if (this.zoomLevelMin < this.globalMinimumZoomLevel) {
				this.globalMinimumZoomLevel = this.zoomLevelMin;
			}
			if (this.zoomLevelMax > this.globalMaximumZoomLevel) {
				this.globalMaximumZoomLevel = this.zoomLevelMax;
			}
		}

		// create and fill the lookup table for the map files
		this.mapFilesLookupTable = new MapFile[this.globalMaximumZoomLevel + 1];
		for (this.tempInt = 0; this.tempInt < this.numberOfMapFiles; ++this.tempInt) {
			this.mapFile = this.mapFilesList[this.tempInt];
			for (this.tempByte = this.mapFile.zoomLevelMin; this.tempByte <= this.mapFile.zoomLevelMax; ++this.tempByte) {
				this.mapFilesLookupTable[this.tempByte] = this.mapFile;
			}
		}

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
	 * Starts a database query with the given parameters.
	 * 
	 * @param tile
	 *            the tile to read.
	 * @param readWayNames
	 *            if way names should be read.
	 * @param mapGenerator
	 *            the MapGenerator callback which handles the extracted map elements.
	 */
	void executeQuery(Tile tile, boolean readWayNames, DatabaseMapGenerator mapGenerator) {
		try {
			Logger.d("executing query: " + tile);
			// reset the stop execution flag
			this.stopCurrentQuery = false;

			// limit the zoom level of the requested tile for this query
			if (tile.zoomLevel > this.globalMaximumZoomLevel) {
				this.queryZoomLevel = this.globalMaximumZoomLevel;
			} else if (tile.zoomLevel < this.globalMinimumZoomLevel) {
				this.queryZoomLevel = this.globalMinimumZoomLevel;
			} else {
				this.queryZoomLevel = tile.zoomLevel;
			}

			// get and check the map file for the query zoom level
			this.mapFile = this.mapFilesLookupTable[this.queryZoomLevel];
			if (this.mapFile == null) {
				Logger.d("no map file for zoom level: " + tile.zoomLevel);
				return;
			}

			this.queryReadWayNames = readWayNames;

			// calculate the base tiles that cover the area of the requested tile
			if (tile.zoomLevel < this.mapFile.baseZoomLevel) {
				// calculate the XY numbers of the upper left and lower right subtiles
				this.zoomLevelDifference = this.mapFile.baseZoomLevel - tile.zoomLevel;
				this.fromBaseTileX = tile.x << this.zoomLevelDifference;
				this.fromBaseTileY = tile.y << this.zoomLevelDifference;
				this.toBaseTileX = this.fromBaseTileX + (1 << this.zoomLevelDifference) - 1;
				this.toBaseTileY = this.fromBaseTileY + (1 << this.zoomLevelDifference) - 1;
				this.useTileBitmask = false;
			} else if (tile.zoomLevel > this.mapFile.baseZoomLevel) {
				// calculate the XY numbers of the parent base tile
				this.zoomLevelDifference = tile.zoomLevel - this.mapFile.baseZoomLevel;
				this.fromBaseTileX = tile.x >> this.zoomLevelDifference;
				this.fromBaseTileY = tile.y >> this.zoomLevelDifference;
				this.toBaseTileX = this.fromBaseTileX;
				this.toBaseTileY = this.fromBaseTileY;

				if (this.zoomLevelDifference == 1) {
					// determine the correct bitmask for all quadrants
					if (tile.x % 2 == 0 && tile.y % 2 == 0) {
						// upper left quadrant
						this.queryTileBitmask = 0xcc00;
					} else if (tile.x % 2 == 1 && tile.y % 2 == 0) {
						// upper right quadrant
						this.queryTileBitmask = 0x3300;
					} else if (tile.x % 2 == 0 && tile.y % 2 == 1) {
						// lower left quadrant
						this.queryTileBitmask = 0xcc;
					} else {
						// lower right quadrant
						this.queryTileBitmask = 0x33;
					}
				} else {
					// calculate the XY numbers of the second level subtile
					this.subtileX = tile.x >> (this.zoomLevelDifference - 2);
					this.subtileY = tile.y >> (this.zoomLevelDifference - 2);

					// calculate the XY numbers of the parent tile
					this.parentTileX = this.subtileX >> 1;
					this.parentTileY = this.subtileY >> 1;

					// determine the correct bitmask for all 16 subtiles
					if (this.parentTileX % 2 == 0 && this.parentTileY % 2 == 0) {
						// upper left quadrant
						if (this.subtileX % 2 == 0 && this.subtileY % 2 == 0) {
							// upper left subtile
							this.queryTileBitmask = 0x8000;
						} else if (this.subtileX % 2 == 1 && this.subtileY % 2 == 0) {
							// upper right subtile
							this.queryTileBitmask = 0x4000;
						} else if (this.subtileX % 2 == 0 && this.subtileY % 2 == 1) {
							// lower left subtile
							this.queryTileBitmask = 0x800;
						} else {
							// lower right subtile
							this.queryTileBitmask = 0x400;
						}
					} else if (this.parentTileX % 2 == 1 && this.parentTileY % 2 == 0) {
						// upper right quadrant
						if (this.subtileX % 2 == 0 && this.subtileY % 2 == 0) {
							// upper left subtile
							this.queryTileBitmask = 0x2000;
						} else if (this.subtileX % 2 == 1 && this.subtileY % 2 == 0) {
							// upper right subtile
							this.queryTileBitmask = 0x1000;
						} else if (this.subtileX % 2 == 0 && this.subtileY % 2 == 1) {
							// lower left subtile
							this.queryTileBitmask = 0x200;
						} else {
							// lower right subtile
							this.queryTileBitmask = 0x100;
						}
					} else if (this.parentTileX % 2 == 0 && this.parentTileY % 2 == 1) {
						// lower left quadrant
						if (this.subtileX % 2 == 0 && this.subtileY % 2 == 0) {
							// upper left subtile
							this.queryTileBitmask = 0x80;
						} else if (this.subtileX % 2 == 1 && this.subtileY % 2 == 0) {
							// upper right subtile
							this.queryTileBitmask = 0x40;
						} else if (this.subtileX % 2 == 0 && this.subtileY % 2 == 1) {
							// lower left subtile
							this.queryTileBitmask = 0x8;
						} else {
							// lower right subtile
							this.queryTileBitmask = 0x4;
						}
					} else {
						// lower right quadrant
						if (this.subtileX % 2 == 0 && this.subtileY % 2 == 0) {
							// upper left subtile
							this.queryTileBitmask = 0x20;
						} else if (this.subtileX % 2 == 1 && this.subtileY % 2 == 0) {
							// upper right subtile
							this.queryTileBitmask = 0x10;
						} else if (this.subtileX % 2 == 0 && this.subtileY % 2 == 1) {
							// lower left subtile
							this.queryTileBitmask = 0x2;
						} else {
							// lower right subtile
							this.queryTileBitmask = 0x1;
						}
					}
				}
				this.useTileBitmask = true;
			} else {
				// use the tile XY numbers of the requested tile
				this.fromBaseTileX = tile.x;
				this.fromBaseTileY = tile.y;
				this.toBaseTileX = this.fromBaseTileX;
				this.toBaseTileY = this.fromBaseTileY;
				this.useTileBitmask = false;
			}

			// calculate the blocks in the file which need to be read
			this.fromBlockX = Math.max(this.fromBaseTileX - this.mapFile.boundaryLeftTile, 0);
			this.fromBlockY = Math.max(this.fromBaseTileY - this.mapFile.boundaryTopTile, 0);
			this.toBlockX = Math.min(this.toBaseTileX - this.mapFile.boundaryLeftTile,
					this.mapFile.blocksWidth - 1);
			this.toBlockY = Math.min(this.toBaseTileY - this.mapFile.boundaryTopTile,
					this.mapFile.blocksHeight - 1);

			// read all necessary blocks from top to bottom and from left to right
			for (this.currentRow = this.fromBlockY; this.currentRow <= this.toBlockY; ++this.currentRow) {
				for (this.currentColumn = this.fromBlockX; this.currentColumn <= this.toBlockX; ++this.currentColumn) {
					// check if the query was interrupted
					if (this.stopCurrentQuery) {
						return;
					}

					// calculate the actual block number of the needed block in the file
					this.blockNumber = this.currentRow * this.mapFile.blocksWidth
							+ this.currentColumn;
					// Logger.d("  blockNumber: " + this.blockNumber);

					// get and check the current block pointer
					this.currentBlockPointer = this.databaseIndexCache.getAddress(this.mapFile,
							this.blockNumber);
					if (this.currentBlockPointer < 1
							|| this.currentBlockPointer > this.mapFile.mapFileSize) {
						Logger.d("invalid current block pointer: " + this.currentBlockPointer);
						Logger.d("mapFileSize: " + this.mapFile.mapFileSize);
						return;
					}
					// Logger.d("  currentBlockPointer: " + this.currentBlockPointer);

					// check if the current block is the last block in the file
					if (this.blockNumber + 1 == this.mapFile.numberOfBlocks) {
						// set the next block pointer to the end of the file
						this.nextBlockPointer = this.mapFile.mapFileSize;
					} else {
						// get and check the next block pointer
						this.nextBlockPointer = this.databaseIndexCache.getAddress(
								this.mapFile, this.blockNumber + 1);
						if (this.nextBlockPointer < 1
								|| this.nextBlockPointer > this.mapFile.mapFileSize) {
							Logger.d("invalid next block pointer: " + this.nextBlockPointer);
							Logger.d("mapFileSize: " + this.mapFile.mapFileSize);
							return;
						}
					}

					// calculate the size of the current block
					this.currentBlockSize = (int) (this.nextBlockPointer - this.currentBlockPointer);
					if (this.currentBlockSize < 0) {
						Logger.d("invalid current block size: " + this.currentBlockSize);
						return;
					} else if (this.currentBlockSize == 0) {
						// the current block is empty, continue with the next block
						return;
					} else if (this.currentBlockPointer + this.currentBlockSize > this.fileSize) {
						Logger.d("invalid current block size: " + this.currentBlockSize);
						return;
					}
					// Logger.d("  currentBlockSize: " + this.currentBlockSize);

					// check that the read buffer is large enough
					if (this.currentBlockSize > this.readBuffer.length) {
						Logger.d("invalid buffer size:" + this.readBuffer.length);
						return;
					}

					// go to the current block and read the data into the buffer
					this.inputFile.seek(this.currentBlockPointer);
					if (this.inputFile.read(this.readBuffer, 0, this.currentBlockSize) != this.currentBlockSize) {
						// if reading the current block has failed, skip it
						Logger.d("reading the current block has failed");
						return;
					}
					this.bufferPosition = 0;

					// handle the current block data
					processBlock(mapGenerator);
				}
			}
		} catch (IOException e) {
			Logger.e(e);
		}
	}

	/**
	 * Returns the area coordinates of the current map file.
	 * 
	 * @return the area coordinates in microdegrees.
	 */
	Rect getMapBoundary() {
		return this.mapBoundary;
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

			// check for null parameter
			if (fileName == null) {
				return false;
			}

			// check if the file exists and is readable
			this.file = new File(fileName);
			if (!this.file.exists()) {
				Logger.d("file does not exist: " + fileName);
				return false;
			} else if (!this.file.isFile()) {
				Logger.d("not a file: " + fileName);
				return false;
			} else if (!this.file.canRead()) {
				Logger.d("cannot read file: " + fileName);
				return false;
			}

			// open the binary map file in read only mode
			this.inputFile = new RandomAccessFile(this.file, "r");
			this.fileSize = this.inputFile.length();

			// read the header data from the file
			if (!readFileHeader()) {
				return false;
			}

			// create the DatabaseIndexCache
			this.databaseIndexCache = new DatabaseIndexCacheNew(inputFile, INDEX_CACHE_SIZE);

			// create a read buffer that is big enough even for the largest block
			this.readBuffer = new byte[(int) this.maximumBlockSize];

			// create an array for the way nodes coordinates
			this.wayNodesSequence = new int[INITIAL_WAY_NODES_CAPACITY];

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
	 * @deprecated use {@link #openFile} instead.
	 */
	@Deprecated
	boolean setFile(String fileName) {
		return openFile(fileName);
	}

	/**
	 * Notifies the database reader to stop the currently executed query.
	 */
	void stopCurrentQuery() {
		this.stopCurrentQuery = true;
	}
}