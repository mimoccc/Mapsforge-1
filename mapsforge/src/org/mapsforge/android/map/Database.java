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
import java.util.HashSet;

/**
 * A database class for reading binary osm files.
 */
class Database {
	private static final byte DATABASE_ZOOM_MAX = 18;
	private int blockNumber;
	private int blockSize;
	private int bufferPosition;
	private long currentBlockPointer;
	private int currentBlockSize;
	private long currentColumn;
	private short currentInnerWayNodes;
	private String currentName;
	private int currentNodeLatitude;
	private byte currentNodeLayer;
	private int currentNodeLongitude;
	private byte currentNodeNameLength;
	private byte currentNodeNumberOfTags;
	private boolean[] currentNodeTagIds;
	private long currentRow;
	private byte currentTagId;
	private int currentWayId;
	private int currentWayInnerBlockSize;
	private boolean currentWayIsMultipolygon;
	private byte currentWayLayer;
	private byte currentWayNameLength;
	private short currentWayNodes;
	private int[] currentWayNodesSequence;
	private byte currentWayNumberOfRealTags;
	private byte currentWayNumberOfTags;
	private byte currentWayTagBitmap;
	private boolean[] currentWayTagIds;
	private DatabaseIndexCache databaseIndexCache;
	private boolean[] defaultNodeTagIds;
	private boolean[] defaultWayTagIds;
	private int elementCounter;
	private String fileName;
	private int firstBlockPointer;
	private long fromColumn;
	private long fromRow;
	private Rect geoRectangle;
	private int[] innerWay;
	private int[][] innerWays;
	private RandomAccessFile inputFile;
	private Rect mapBoundary;
	private DatabaseMapGenerator mapGenerator;
	private int matrixBlocks;
	private int matrixHeight;
	private int matrixWidth;
	private byte[] nameBuffer = new byte[128];
	private long nextBlockPointer;
	private int nodeListSizeInBytes;
	private short nodesOnZoomLevel;
	private byte numberOfInnerWays;
	private byte[] readBuffer;
	private boolean readWayNames;
	private boolean stopCurrentQuery;
	private long toColumn;
	private long toRow;
	private boolean wayInRange;
	private HashSet<Integer> ways;
	private short waysOnZoomLevel;
	private byte zoomLevel;

	/**
	 * Construct a new database object to read binary OpenStreetMap files.
	 */
	Database() {
		this.ways = new HashSet<Integer>((int) (5000 / 0.5f) + 2, 0.5f);
	}

	/**
	 * Create the DatabaseIndexCache.
	 */
	private void handleIndexBlockPointers() {
		this.databaseIndexCache = new DatabaseIndexCache(this.inputFile, this.matrixBlocks,
				this.firstBlockPointer, 16);
		// 12 bytes per block (block id, block pointer and block size)
		int blockPointerSize = this.matrixBlocks * 12;
		this.firstBlockPointer += blockPointerSize;

		// FIXME: we don't know the size of the largest block!
		this.readBuffer = new byte[1000000];
	}

	/**
	 * Read a single block and call the render functions on all map elements.
	 * 
	 * @throws IndexOutOfBoundsException
	 * @throws UnsupportedEncodingException
	 */
	private void processBlock() throws IndexOutOfBoundsException, UnsupportedEncodingException {
		// read the amount of way and nodes on the current zoomLevel level
		this.bufferPosition = this.zoomLevel * 4;
		this.nodesOnZoomLevel = Deserializer.toShort(this.readBuffer, this.bufferPosition);
		this.waysOnZoomLevel = Deserializer.toShort(this.readBuffer, this.bufferPosition + 2);

		// read the total size of the node block
		this.nodeListSizeInBytes = Deserializer.toInt(this.readBuffer, 76);
		this.bufferPosition = 80;

		// read nodes
		for (this.elementCounter = this.nodesOnZoomLevel; this.elementCounter != 0; --this.elementCounter) {
			// read node longitude
			this.currentNodeLongitude = Deserializer
					.toInt(this.readBuffer, this.bufferPosition);

			// read node latitude
			this.currentNodeLatitude = Deserializer.toInt(this.readBuffer,
					this.bufferPosition + 4);

			// read node name length
			this.currentNodeNameLength = this.readBuffer[this.bufferPosition + 8];

			// read number of tags
			this.currentNodeNumberOfTags = this.readBuffer[this.bufferPosition + 9];
			this.bufferPosition += 10;

			// process node name
			if (this.currentNodeNameLength > 0) {
				System.arraycopy(this.readBuffer, this.bufferPosition, this.nameBuffer, 0,
						this.currentNodeNameLength);
				this.currentName = new String(this.nameBuffer, 0, this.currentNodeNameLength,
						"UTF-8");
				this.bufferPosition += this.currentNodeNameLength;
			} else {
				this.currentName = null;
			}

			// TODO: read the node layer
			this.currentNodeLayer = (byte) 5;

			// reset node tag array
			System.arraycopy(this.defaultNodeTagIds, 0, this.currentNodeTagIds, 0,
					this.defaultNodeTagIds.length);
			// read node tags
			for (byte i = this.currentNodeNumberOfTags; i != 0; --i) {
				this.currentTagId = this.readBuffer[this.bufferPosition];
				this.bufferPosition += 1;
				this.currentNodeTagIds[this.currentTagId] = true;
			}

			// render the current node
			this.mapGenerator.renderPointOfInterest(this.currentNodeLayer,
					this.currentNodeLatitude, this.currentNodeLongitude, this.currentName,
					this.currentNodeTagIds);
		}

		// move the pointer to the first way
		this.bufferPosition = 80 + this.nodeListSizeInBytes;

		// read ways
		for (this.elementCounter = this.waysOnZoomLevel; this.elementCounter != 0; --this.elementCounter) {
			// read way name length
			this.currentWayNameLength = this.readBuffer[this.bufferPosition];

			// read the multipolygon flag and the number of tags
			this.currentWayNumberOfTags = (byte) (this.readBuffer[this.bufferPosition + 1] & 0x7f);
			if ((this.readBuffer[this.bufferPosition + 1] >> 7) != 0) {
				this.currentWayIsMultipolygon = true;
			} else {
				this.currentWayIsMultipolygon = false;
			}

			// read number of nodes
			this.currentWayNodes = (short) (Deserializer.toShort(this.readBuffer,
					this.bufferPosition + 2) * 2);

			if (this.currentWayIsMultipolygon) {
				// read the inner way block size
				this.currentWayInnerBlockSize = Deserializer.toInt(this.readBuffer,
						this.bufferPosition + 4);
				this.bufferPosition += 8;
			} else {
				this.currentWayInnerBlockSize = 0;
				this.bufferPosition += 4;
			}

			// read way id
			this.currentWayId = Deserializer.toInt(this.readBuffer, this.bufferPosition);
			if (this.ways.contains(Integer.valueOf(this.currentWayId))) {
				this.bufferPosition += 4 + 1 + this.currentWayNameLength + 1
						* this.currentWayNumberOfTags + 1 + this.currentWayNodes * 4
						+ this.currentWayInnerBlockSize;
				continue;
			}
			this.bufferPosition += 4;

			// read the number of real tags and the way layer
			this.currentWayLayer = (byte) (this.readBuffer[this.bufferPosition] & 0x0f);
			this.currentWayNumberOfRealTags = (byte) (this.readBuffer[this.bufferPosition] >> 4);
			this.bufferPosition += 1;

			// process way name
			if (this.currentWayNameLength > 0) {
				// check if way name should be read
				if (this.readWayNames) {
					System.arraycopy(this.readBuffer, this.bufferPosition, this.nameBuffer, 0,
							this.currentWayNameLength);
					this.currentName = new String(this.nameBuffer, 0,
							this.currentWayNameLength, "UTF-8");
				}
				this.bufferPosition += this.currentWayNameLength;
			} else {
				this.currentName = null;
			}

			// reset way tag array
			System.arraycopy(this.defaultWayTagIds, 0, this.currentWayTagIds, 0,
					this.defaultWayTagIds.length);
			// read way tags
			for (byte i = this.currentWayNumberOfTags; i != 0; --i) {
				this.currentTagId = this.readBuffer[this.bufferPosition];
				this.bufferPosition += 1;
				this.currentWayTagIds[this.currentTagId] = true;
			}

			// read way tag bitmap
			this.currentWayTagBitmap = this.readBuffer[this.bufferPosition];
			this.bufferPosition += 1;

			// read way nodes
			this.currentWayNodesSequence = new int[this.currentWayNodes];
			this.wayInRange = false;
			for (short i = 0; i < this.currentWayNodes; i += 2) {
				this.currentNodeLongitude = Deserializer.toInt(this.readBuffer,
						this.bufferPosition);
				this.currentNodeLatitude = Deserializer.toInt(this.readBuffer,
						this.bufferPosition + 4);
				this.bufferPosition += 8;
				this.currentWayNodesSequence[i] = this.currentNodeLongitude;
				this.currentWayNodesSequence[i + 1] = this.currentNodeLatitude;

				// check if current way node is in the requested area
				if (!this.wayInRange && this.currentNodeLatitude <= this.geoRectangle.top
						&& this.currentNodeLatitude >= this.geoRectangle.bottom
						&& this.currentNodeLongitude >= this.geoRectangle.left
						&& this.currentNodeLongitude <= this.geoRectangle.right) {
					this.wayInRange = true;
				}
			}

			// return the way if at least one of its nodes is in the requested
			// area
			if (!this.wayInRange) {
				for (short i = 0; i < this.currentWayNodes - 2; i += 2) {
					this.currentNodeLongitude = this.currentWayNodesSequence[i];
					this.currentNodeLatitude = this.currentWayNodesSequence[i + 1];
					if (CohenSutherlandClipping.isLineInRectangle(this.currentNodeLongitude,
							this.currentNodeLatitude, this.currentWayNodesSequence[i + 2],
							this.currentWayNodesSequence[i + 3], this.geoRectangle.left,
							this.geoRectangle.bottom, this.geoRectangle.right,
							this.geoRectangle.top)) {
						this.wayInRange = true;
						break;
					}
				}
			}

			if (!this.wayInRange) {
				// skip rendering of this way and continue with the next one
				this.bufferPosition += this.currentWayInnerBlockSize;
				continue;
			}

			this.innerWays = null;
			if (this.currentWayIsMultipolygon) {
				// read the amount of inner ways
				this.numberOfInnerWays = this.readBuffer[this.bufferPosition];
				this.innerWays = new int[this.numberOfInnerWays][];
				this.bufferPosition += 1;

				for (byte i = (byte) (this.numberOfInnerWays - 1); i >= 0; --i) {
					// read the amount of nodes
					this.currentInnerWayNodes = (short) (Deserializer.toShort(this.readBuffer,
							this.bufferPosition) * 2);
					this.bufferPosition += 2;

					this.innerWay = new int[this.currentInnerWayNodes];
					for (short j = 0; j < this.currentInnerWayNodes; j += 2) {
						// read the current inner node
						this.currentNodeLongitude = Deserializer.toInt(this.readBuffer,
								this.bufferPosition);
						this.currentNodeLatitude = Deserializer.toInt(this.readBuffer,
								this.bufferPosition + 4);
						this.bufferPosition += 8;

						this.innerWay[j] = this.currentNodeLongitude;
						this.innerWay[j + 1] = this.currentNodeLatitude;
					}
					this.innerWays[i] = this.innerWay;
				}
			}

			// render the current way
			this.mapGenerator.renderWay(this.currentWayLayer, this.currentWayNumberOfRealTags,
					this.currentName, this.currentWayTagIds, this.currentWayTagBitmap,
					this.currentWayNodes, this.currentWayNodesSequence, this.innerWays);
			this.ways.add(Integer.valueOf(this.currentWayId));
		}
	}

	/**
	 * Close the map file.
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
	 * @param queryReadWayNames
	 *            if way names should be read
	 * @param queryMapGenerator
	 *            the MapGenerator object for rendering all map elements
	 */
	void executeQuery(Tile tile, boolean queryReadWayNames,
			DatabaseMapGenerator queryMapGenerator) {
		try {
			this.geoRectangle = tile.getBoundingBox();
			if (tile.zoomLevel > DATABASE_ZOOM_MAX) {
				this.zoomLevel = DATABASE_ZOOM_MAX;
			} else {
				this.zoomLevel = tile.zoomLevel;
			}
			this.readWayNames = queryReadWayNames;
			this.mapGenerator = queryMapGenerator;
			this.ways.clear();
			this.stopCurrentQuery = false;

			// calculate the blocks which need to be read during this query
			this.fromRow = (this.mapBoundary.top - this.geoRectangle.top) / this.blockSize;
			this.toRow = (this.mapBoundary.top - this.geoRectangle.bottom) / this.blockSize;
			this.fromColumn = (this.geoRectangle.left - this.mapBoundary.left) / this.blockSize;
			this.toColumn = (this.geoRectangle.right - this.mapBoundary.left) / this.blockSize;

			// check that all values are within the block matrix
			if (this.fromRow < 0) {
				this.fromRow = 0;
			}
			if (this.toRow >= this.matrixHeight) {
				this.toRow = this.matrixHeight - 1;
			}
			if (this.fromColumn < 0) {
				this.fromColumn = 0;
			}
			if (this.toColumn >= this.matrixWidth || this.toColumn < 0) {
				this.toColumn = this.matrixWidth - 1;
			}

			for (this.currentRow = this.fromRow; this.currentRow <= this.toRow; ++this.currentRow) {
				for (this.currentColumn = this.fromColumn; this.currentColumn <= this.toColumn; ++this.currentColumn) {
					// check if the current query was interrupted
					if (this.stopCurrentQuery) {
						return;
					}

					// calculate the block number of the current block
					this.blockNumber = (int) (this.currentRow * this.matrixWidth + this.currentColumn);

					// get the pointers to the current and the next block
					this.currentBlockPointer = this.databaseIndexCache
							.getAddress(this.blockNumber);
					this.nextBlockPointer = this.databaseIndexCache
							.getAddress(this.blockNumber + 1);
					// check if the next block has a valid pointer
					if (this.nextBlockPointer == -1) {
						// the current block is the last one in the file
						this.nextBlockPointer = this.inputFile.length();
					}

					// calculate the size of the current block
					this.currentBlockSize = (int) (this.nextBlockPointer - this.currentBlockPointer);

					// if the current block has no map data continue with the next one
					if (currentBlockSize == 80) {
						continue;
					}

					// go to the current block and read its data to the buffer
					this.inputFile.seek(this.firstBlockPointer + this.currentBlockPointer);
					if (this.inputFile.read(this.readBuffer, 0, this.currentBlockSize) != this.currentBlockSize) {
						// if reading the current block has failed, skip it
						continue;
					}

					// handle the current block data
					processBlock();
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
	 * Set the map file from which the database should read.
	 * 
	 * @param fileName
	 *            the path to the map file
	 * @return true if the initial map file block was read successfully, false otherwise
	 */
	boolean setFile(String fileName) {
		try {
			this.fileName = fileName;
			this.inputFile = new RandomAccessFile(this.fileName, "r");
			this.readBuffer = new byte[28];
			// read the initial file block
			if (this.inputFile.read(this.readBuffer, 0, 28) != 28) {
				closeFile();
				return false;
			}
			this.firstBlockPointer = 28;

			// read the map boundaries and store them
			this.mapBoundary = new Rect(Deserializer.toInt(this.readBuffer, 0), Deserializer
					.toInt(this.readBuffer, 4), Deserializer.toInt(this.readBuffer, 8),
					Deserializer.toInt(this.readBuffer, 12));

			// read the matrix width and height
			this.matrixWidth = Deserializer.toInt(this.readBuffer, 16);
			this.matrixHeight = Deserializer.toInt(this.readBuffer, 20);

			// calculate the amount of blocks and read the block size
			this.matrixBlocks = this.matrixWidth * this.matrixHeight;
			this.blockSize = Deserializer.toInt(this.readBuffer, 24);

			// create the tag array
			this.defaultNodeTagIds = new boolean[Byte.MAX_VALUE];
			this.currentNodeTagIds = new boolean[Byte.MAX_VALUE];
			this.defaultWayTagIds = new boolean[Byte.MAX_VALUE];
			this.currentWayTagIds = new boolean[Byte.MAX_VALUE];

			// read the block information
			handleIndexBlockPointers();
			return true;
		} catch (ArrayIndexOutOfBoundsException e) {
			Logger.e(e);
			return false;
		} catch (IOException e) {
			Logger.e(e);
			return false;
		}
	}

	void stopCurrentQuery() {
		this.stopCurrentQuery = true;
	}
}