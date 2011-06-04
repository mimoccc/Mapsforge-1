/*
 * Copyright 2010, 2011 mapsforge.org
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.mapsforge.pc.maps;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;

import android.graphics.Rect;

/**
 * A database class for reading binary map files. Byte order is big-endian.
 */
public class MapDatabase {
	/**
	 * Magic byte at the beginning of a valid binary map file.
	 */
	private static final String BINARY_OSM_MAGIC_BYTE = "mapsforge binary OSM";

	/**
	 * Maximum supported version of the file format.
	 */
	private static final int BINARY_OSM_VERSION_MAX = 2;

	/**
	 * Minimal supported version of the file format.
	 */
	private static final int BINARY_OSM_VERSION_MIN = 2;

	/**
	 * Bitmask to extract the block offset from an index entry.
	 */
	private static final long BITMASK_INDEX_OFFSET = 0x7FFFFFFFFFL;

	/**
	 * Bitmask to extract the water information from an index entry.
	 */
	private static final long BITMASK_INDEX_WATER = 0x8000000000L;

	/**
	 * Bitmask for the debug flag in the file header.
	 */
	private static final int HEADER_BITMASK_DEBUG = 0x80;

	/**
	 * Bitmask for the start position in the file header.
	 */
	private static final int HEADER_BITMASK_START_POSITION = 0x40;

	/**
	 * Amount of cache blocks that the index cache should store.
	 */
	private static final int INDEX_CACHE_SIZE = 64;

	/**
	 * Initial length of the way nodes array.
	 */
	private static final int INITIAL_WAY_NODES_CAPACITY = 2048;

	/**
	 * Load factor of the internal HashMap.
	 */
	private static final float LOAD_FACTOR = 0.6f;

	/**
	 * Maximum tag ID which is considered as valid.
	 */
	private static final int MAXIMUM_ALLOWED_TAG_ID = 8192;

	/**
	 * Maximum size of a single block in bytes that is supported by this implementation.
	 */
	private static final int MAXIMUM_BLOCK_SIZE = 2500000;

	/**
	 * Maximum number of inner ways which is considered as valid.
	 */
	private static final int MAXIMUM_NUMBER_OF_INNER_WAYS = 1024;

	/**
	 * Maximum way nodes sequence length which is considered as valid.
	 */
	private static final int MAXIMUM_WAY_NODES_SEQUENCE_LENGTH = 8192;

	/**
	 * Bitmask for the optional node feature "elevation".
	 */
	private static final int NODE_FEATURE_BITMASK_ELEVATION = 0x40;

	/**
	 * Bitmask for the optional node feature "house number".
	 */
	private static final int NODE_FEATURE_BITMASK_HOUSE_NUMBER = 0x20;

	/**
	 * Bitmask for the optional node feature "name".
	 */
	private static final int NODE_FEATURE_BITMASK_NAME = 0x80;

	/**
	 * Bitmask for the node layer.
	 */
	private static final int NODE_LAYER_BITMASK = 0xf0;

	/**
	 * Bit shift for calculating the node layer.
	 */
	private static final int NODE_LAYER_SHIFT = 4;

	/**
	 * Bitmask for the number of node tags.
	 */
	private static final int NODE_NUMBER_OF_TAGS_BITMASK = 0x0f;

	/**
	 * Maximum size of the remaining file header in bytes.
	 */
	private static final int REMAINING_HEADER_SIZE_MAX = 1000000;

	/**
	 * Minimum size of the remaining file header in bytes.
	 */
	private static final int REMAINING_HEADER_SIZE_MIN = 50;

	/**
	 * Length of the debug signature at the beginning of each block.
	 */
	private static final byte SIGNATURE_LENGTH_BLOCK = 32;

	/**
	 * Length of the debug signature at the beginning of the index.
	 */
	private static final byte SIGNATURE_LENGTH_INDEX = 16;

	/**
	 * Length of the debug signature at the beginning of each node.
	 */
	private static final byte SIGNATURE_LENGTH_NODE = 32;

	/**
	 * Length of the debug signature at the beginning of each way.
	 */
	private static final byte SIGNATURE_LENGTH_WAY = 32;

	/**
	 * Bitmask for the optional way feature "label position".
	 */
	private static final int WAY_FEATURE_BITMASK_LABEL_POSITION = 0x20;

	/**
	 * Bitmask for the optional way feature "multipolygon".
	 */
	private static final int WAY_FEATURE_BITMASK_MULTIPOLYGON = 0x10;

	/**
	 * Bitmask for the optional way feature "name".
	 */
	private static final int WAY_FEATURE_BITMASK_NAME = 0x80;

	/**
	 * Bitmask for the optional way feature "reference".
	 */
	private static final int WAY_FEATURE_BITMASK_REF = 0x40;

	/**
	 * Bitmask for the way layer.
	 */
	private static final int WAY_LAYER_BITMASK = 0xf0;

	/**
	 * Bit shift for calculating the way layer.
	 */
	private static final int WAY_LAYER_SHIFT = 4;

	/**
	 * Bitmask for the number of way tags.
	 */
	private static final int WAY_NUMBER_OF_TAGS_BITMASK = 0x0f;

	/**
	 * Bitmask for the number of relevant way tags.
	 */
	private static final int WAY_RELEVANT_TAGS_BITMASK = 0xe0;

	/**
	 * Bit shift for calculating the number of relevant way tags.
	 */
	private static final int WAY_RELEVANT_TAGS_SHIFT = 5;

	private byte baseZoomLevel;
	private int blockEntriesTableOffset;
	private long blockNumber;
	private String blockSignature;
	private int boundaryBottom;
	private int boundaryLeft;
	private int boundaryRight;
	private int boundaryTop;
	private int bufferPosition;
	private String commentText;
	private long currentBlockIndexEntry;
	private boolean currentBlockIsWater;
	private long currentBlockPointer;
	private int currentBlockSize;
	private long currentColumn;
	private long currentRow;
	private MapDatabaseIndexCache databaseIndexCache;
	private boolean debugFile;
	private boolean[] defaultTagIds;
	private short elementCounter;
	private File file;
	private long fileSize;
	private int fileVersionNumber;
	private int firstWayOffset;
	private long fromBaseTileX;
	private long fromBaseTileY;
	private long fromBlockX;
	private long fromBlockY;
	private byte globalMaximumZoomLevel;
	private byte globalMinimumZoomLevel;
	private boolean headerStartPosition;
	private long indexStartAddress;
	private int[] innerWay;
	private int innerWayNodesSequenceLength;
	private int innerWayNumber;
	private int innerWayNumberOfWayNodes;
	private RandomAccessFile inputFile;
	private String magicByte;
	private Rect mapBoundary;
	private long mapDate;
	private MapFileParameters mapFileParameters;
	private long mapFileSize;
	private MapFileParameters[] mapFilesList;
	private MapFileParameters[] mapFilesLookupTable;
	private int maximumNodeTagId;
	private int maximumWayTagId;
	private byte metaFlags;
	private long nextBlockPointer;
	private String nodeElevation;
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
	private String nodeSignature;
	private short nodesOnZoomLevel;
	private byte nodeSpecialByte;
	private String nodeTag;
	private int nodeTagId;
	private boolean[] nodeTagIds;
	private HashMap<String, Integer> nodeTags;
	private byte numberOfMapFiles;
	private short numberOfNodeTags;
	private short numberOfWayTags;
	private long parentTileX;
	private long parentTileY;
	private String projectionName;
	private boolean queryIsWater;
	private boolean queryReadWaterInfo;
	private boolean queryReadWayNames;
	private int queryTileBitmask;
	private int queryZoomLevel;
	private byte[] readBuffer;
	private int remainingHeaderSize;
	private long startAddress;
	private int startPositionLatitude;
	private int startPositionLongitude;
	private boolean stopCurrentQuery;
	private int stringLength;
	private long subtileX;
	private long subtileY;
	private byte tempByte;
	private int tempInt;
	private int tileLatitude;
	private int tileLongitude;
	private short tilePixelSize;
	private long toBaseTileX;
	private long toBaseTileY;
	private long toBlockX;
	private long toBlockY;
	private boolean useTileBitmask;
	private int variableByteDecode;
	private byte variableByteShift;
	private byte wayFeatureByte;
	private boolean wayFeatureLabelPosition;
	private boolean wayFeatureMultipolygon;
	private boolean wayFeatureName;
	private boolean wayFeatureRef;
	private int[][] wayInnerWays;
	private int[] wayLabelPosition;
	private byte wayLayer;
	private String wayName;
	private int wayNodeLatitude;
	private int wayNodeLongitude;
	private int[] wayNodesSequence;
	private int wayNodesSequenceLength;
	private int wayNumberOfInnerWays;
	private byte wayNumberOfRelevantTags;
	private byte wayNumberOfTags;
	private int wayNumberOfWayNodes;
	private String wayRef;
	private String waySignature;
	private int waySize;
	private short waysOnZoomLevel;
	private byte waySpecialByte1;
	private byte waySpecialByte2;
	private String wayTag;
	private byte wayTagBitmap;
	private int wayTagId;
	private boolean[] wayTagIds;
	private HashMap<String, Integer> wayTags;
	private short wayTileBitmask;
	private int zoomLevelDifference;
	private byte zoomLevelMax;
	private byte zoomLevelMin;

	/**
	 * Empty default constructor with limited visibility.
	 */
	MapDatabase() {
		// do nothing
	}

	/**
	 * Returns the comment text of the current map file.
	 * 
	 * @return the comment text of the current map file.
	 */
	public String getCommentText() {
		return this.commentText;
	}

	/**
	 * Returns the area coordinates of the current map file in microdegrees.
	 * 
	 * @return the area coordinates of the current map file in microdegrees.
	 */
	public Rect getMapBoundary() {
		return this.mapBoundary;
	}

	/**
	 * Returns the center of the current map file (may be null).
	 * 
	 * @return the center of the current map file (may be null).
	 */
	public GeoPoint getMapCenter() {
		if (this.mapBoundary != null) {
			return new GeoPoint(this.mapBoundary.centerY(), this.mapBoundary.centerX());
		}
		return null;
	}

	/**
	 * Returns the date of the map data in the current map file.
	 * 
	 * @return the date of the map data in the current map file.
	 */
	public long getMapDate() {
		return this.mapDate;
	}

	/**
	 * Returns the start position from the map file header (may be null).
	 * 
	 * @return the start position from the map file header (may be null).
	 */
	public GeoPoint getStartPosition() {
		if (this.headerStartPosition) {
			return new GeoPoint(this.startPositionLatitude, this.startPositionLongitude);
		}
		return null;
	}

	/**
	 * Informs about the existence of debug information in the current map file.
	 * 
	 * @return true if the current map file includes debug information, false otherwise.
	 */
	public boolean isDebugFile() {
		return this.debugFile;
	}

	/**
	 * Reads a single block and calls the render functions on all map elements.
	 * 
	 * @param databaseMapGenerator
	 *            the DatabaseMapGenerator callback which handles the extracted map elements.
	 * @throws UnsupportedEncodingException
	 *             if string decoding fails.
	 */
	private void processBlock(DatabaseMapGenerator databaseMapGenerator)
			throws UnsupportedEncodingException {
		if (this.debugFile) {
			// get and check the block signature
			this.blockSignature = new String(this.readBuffer, this.bufferPosition,
					SIGNATURE_LENGTH_BLOCK, "UTF-8");
			this.bufferPosition += SIGNATURE_LENGTH_BLOCK;
			if (!this.blockSignature.startsWith("###TileStart")) {
				Logger.d("invalid block signature: " + this.blockSignature);
				return;
			}
		}

		// calculate the offset in the block entries table and move the pointer
		this.blockEntriesTableOffset = (this.queryZoomLevel - this.mapFileParameters.zoomLevelMin) * 4;
		this.bufferPosition += this.blockEntriesTableOffset;

		// get the amount of way and nodes on the current zoomLevel level
		this.nodesOnZoomLevel = readShort();
		this.waysOnZoomLevel = readShort();

		// move the pointer to the end of the block entries table
		this.bufferPosition += this.mapFileParameters.blockEntriesTableSize
				- this.blockEntriesTableOffset - 4;

		// get the offset to the first stored way in the block
		this.firstWayOffset = readVariableByteEncodedUnsignedInt() + this.bufferPosition;
		if (this.firstWayOffset > this.readBuffer.length) {
			Logger.d("invalid first way offset: " + this.firstWayOffset);
			if (this.debugFile) {
				Logger.d("block signature: " + this.blockSignature);
			}
			return;
		}

		// get the nodes
		for (this.elementCounter = this.nodesOnZoomLevel; this.elementCounter != 0; --this.elementCounter) {
			if (this.debugFile) {
				// get and check the node signature
				this.nodeSignature = new String(this.readBuffer, this.bufferPosition,
						SIGNATURE_LENGTH_NODE, "UTF-8");
				this.bufferPosition += SIGNATURE_LENGTH_NODE;
				if (!this.nodeSignature.startsWith("***POIStart")) {
					Logger.d("invalid node signature: " + this.nodeSignature);
					Logger.d("block signature: " + this.blockSignature);
					return;
				}
			}

			// get the node latitude offset (VBE-S)
			this.nodeLatitude = this.tileLatitude + readVariableByteEncodedSignedInt();

			// get the node longitude offset (VBE-S)
			this.nodeLongitude = this.tileLongitude + readVariableByteEncodedSignedInt();

			// get the special byte that encodes multiple fields
			this.nodeSpecialByte = readByte();

			// bit 1-4 of the special byte represent the node layer
			this.nodeLayer = (byte) ((this.nodeSpecialByte & NODE_LAYER_BITMASK) >>> NODE_LAYER_SHIFT);
			// bit 5-8 of the special byte represent the number of tag IDs
			this.nodeNumberOfTags = (byte) (this.nodeSpecialByte & NODE_NUMBER_OF_TAGS_BITMASK);

			// reset the node tag array
			System.arraycopy(this.defaultTagIds, 0, this.nodeTagIds, 0, this.nodeTagIds.length);
			// get the node tag IDs (VBE-U)
			for (this.tempByte = this.nodeNumberOfTags; this.tempByte != 0; --this.tempByte) {
				this.nodeTagId = readVariableByteEncodedUnsignedInt();
				if (this.nodeTagId < 0 || this.nodeTagId >= this.nodeTagIds.length) {
					Logger.d("invalid node tag ID: " + this.nodeTagId);
					if (this.debugFile) {
						Logger.d("node signature: " + this.nodeSignature);
						Logger.d("block signature: " + this.blockSignature);
					}
					return;
				}
				this.nodeTagIds[this.nodeTagId] = true;
			}

			// get the feature byte
			this.nodeFeatureByte = readByte();

			// bit 1-3 of the node feature byte enable optional features
			this.nodeFeatureName = (this.nodeFeatureByte & NODE_FEATURE_BITMASK_NAME) != 0;
			this.nodeFeatureElevation = (this.nodeFeatureByte & NODE_FEATURE_BITMASK_ELEVATION) != 0;
			this.nodeFeatureHouseNumber = (this.nodeFeatureByte & NODE_FEATURE_BITMASK_HOUSE_NUMBER) != 0;

			// check if the node has a name
			if (this.nodeFeatureName) {
				this.nodeName = readUTF8EncodedString(true);
			} else {
				// no node name
				this.nodeName = null;
			}

			// check if the node has an elevation
			if (this.nodeFeatureElevation) {
				// get the node elevation (VBE-S)
				this.nodeElevation = Integer.toString(readVariableByteEncodedSignedInt());
			} else {
				// no elevation
				this.nodeElevation = null;
			}

			// check if the node has a house number
			if (this.nodeFeatureHouseNumber) {
				this.nodeHouseNumber = readUTF8EncodedString(true);
			} else {
				// no house number
				this.nodeHouseNumber = null;
			}

			// render the node
			databaseMapGenerator.renderPointOfInterest(this.nodeLayer, this.nodeLatitude,
					this.nodeLongitude, this.nodeName, this.nodeHouseNumber,
					this.nodeElevation, this.nodeTagIds);
		}

		// finished reading nodes, check if the current buffer position is valid
		if (this.bufferPosition > this.firstWayOffset) {
			Logger.d("invalid buffer position: " + this.bufferPosition + " - "
					+ this.firstWayOffset);
			if (this.debugFile) {
				Logger.d("block signature: " + this.blockSignature);
			}
			return;
		}

		// move the pointer to the first way
		this.bufferPosition = this.firstWayOffset;

		// get the ways
		for (this.elementCounter = this.waysOnZoomLevel; this.elementCounter != 0; --this.elementCounter) {
			if (this.debugFile) {
				// get and check the way signature
				this.waySignature = new String(this.readBuffer, this.bufferPosition,
						SIGNATURE_LENGTH_WAY, "UTF-8");
				this.bufferPosition += SIGNATURE_LENGTH_WAY;
				if (!this.waySignature.startsWith("---WayStart")) {
					Logger.d("invalid way signature: " + this.waySignature);
					Logger.d("block signature: " + this.blockSignature);
					return;
				}
			}

			// get the size of the way (VBE-U)
			this.waySize = readVariableByteEncodedUnsignedInt();

			if (this.useTileBitmask) {
				// get the way tile bitmask (2 bytes)
				this.wayTileBitmask = readShort();
				// check if the way is inside the requested tile
				if ((this.queryTileBitmask & this.wayTileBitmask) == 0) {
					// skip the rest of the way and continue with the next way
					this.bufferPosition += this.waySize - 2;
					continue;
				}
			} else {
				// ignore the way tile bitmask (2 bytes)
				this.bufferPosition += 2;
			}

			// get the first special byte that encodes multiple fields
			this.waySpecialByte1 = readByte();

			// bit 1-4 of the first special byte represent the way layer
			this.wayLayer = (byte) ((this.waySpecialByte1 & WAY_LAYER_BITMASK) >>> WAY_LAYER_SHIFT);
			// bit 5-8 of the first special byte represent the number of tag IDs
			this.wayNumberOfTags = (byte) (this.waySpecialByte1 & WAY_NUMBER_OF_TAGS_BITMASK);

			// get the second special byte that encodes multiple fields
			this.waySpecialByte2 = readByte();

			// bit 1-3 of the second special byte represent the number of relevant tags
			this.wayNumberOfRelevantTags = (byte) ((this.waySpecialByte2 & WAY_RELEVANT_TAGS_BITMASK) >>> WAY_RELEVANT_TAGS_SHIFT);

			// get the way tag bitmap
			this.wayTagBitmap = readByte();

			// reset the way tag array
			System.arraycopy(this.defaultTagIds, 0, this.wayTagIds, 0, this.wayTagIds.length);
			// get the way tag IDs (VBE-U)
			for (this.tempByte = this.wayNumberOfTags; this.tempByte != 0; --this.tempByte) {
				this.wayTagId = readVariableByteEncodedUnsignedInt();
				if (this.wayTagId < 0 || this.wayTagId >= this.wayTagIds.length) {
					Logger.d("invalid way tag ID: " + this.wayTagId);
					if (this.debugFile) {
						Logger.d("way signature: " + this.waySignature);
						Logger.d("block signature: " + this.blockSignature);
					}
					return;
				}
				this.wayTagIds[this.wayTagId] = true;
			}

			// get and check the number of way nodes (VBE-U)
			this.wayNumberOfWayNodes = readVariableByteEncodedUnsignedInt();
			if (this.wayNumberOfWayNodes < 1
					|| this.wayNumberOfWayNodes > MAXIMUM_WAY_NODES_SEQUENCE_LENGTH) {
				Logger.d("invalid number of way nodes: " + this.wayNumberOfWayNodes);
				if (this.debugFile) {
					Logger.d("way signature: " + this.waySignature);
					Logger.d("block signature: " + this.blockSignature);
				}
				return;
			}

			// each way node consists of latitude and longitude fields
			this.wayNodesSequenceLength = this.wayNumberOfWayNodes * 2;

			// make sure that the array for the way nodes is large enough
			if (this.wayNodesSequenceLength > this.wayNodesSequence.length) {
				this.wayNodesSequence = new int[this.wayNodesSequenceLength];
			}

			// get the first way node latitude offset (VBE-S)
			this.wayNodeLatitude = this.tileLatitude + readVariableByteEncodedSignedInt();
			// get the first way node longitude offset (VBE-S)
			this.wayNodeLongitude = this.tileLongitude + readVariableByteEncodedSignedInt();

			// store the first way node
			this.wayNodesSequence[1] = this.wayNodeLatitude;
			this.wayNodesSequence[0] = this.wayNodeLongitude;

			// get the remaining way nodes offsets
			for (this.tempInt = 2; this.tempInt < this.wayNodesSequenceLength; this.tempInt += 2) {
				// get the way node latitude offset (VBE-S)
				this.wayNodeLatitude = readVariableByteEncodedSignedInt();
				// get the way node longitude offset (VBE-S)
				this.wayNodeLongitude = readVariableByteEncodedSignedInt();

				// calculate the way node coordinates
				this.wayNodesSequence[this.tempInt] = this.wayNodesSequence[this.tempInt - 2]
						+ this.wayNodeLongitude;
				this.wayNodesSequence[this.tempInt + 1] = this.wayNodesSequence[this.tempInt - 1]
						+ this.wayNodeLatitude;
			}

			// get the feature byte
			this.wayFeatureByte = readByte();

			// bit 1-4 of the way feature byte enable optional features
			this.wayFeatureName = (this.wayFeatureByte & WAY_FEATURE_BITMASK_NAME) != 0;
			this.wayFeatureRef = (this.wayFeatureByte & WAY_FEATURE_BITMASK_REF) != 0;
			this.wayFeatureLabelPosition = (this.wayFeatureByte & WAY_FEATURE_BITMASK_LABEL_POSITION) != 0;
			this.wayFeatureMultipolygon = (this.wayFeatureByte & WAY_FEATURE_BITMASK_MULTIPOLYGON) != 0;

			// check if the way has a name
			if (this.wayFeatureName) {
				this.wayName = readUTF8EncodedString(this.queryReadWayNames);
			} else {
				// no way name
				this.wayName = null;
			}

			// check if the way has a reference
			if (this.wayFeatureRef) {
				this.wayRef = readUTF8EncodedString(this.queryReadWayNames);
			} else {
				// no reference
				this.wayRef = null;
			}

			// check if the way has a label position
			if (this.wayFeatureLabelPosition) {
				if (this.queryReadWayNames) {
					this.wayLabelPosition = new int[2];
					// get the label position latitude offset (VBE-S)
					this.wayLabelPosition[1] = this.wayNodesSequence[1]
							+ readVariableByteEncodedSignedInt();
					// get the label position longitude offset (VBE-S)
					this.wayLabelPosition[0] = this.wayNodesSequence[0]
							+ readVariableByteEncodedSignedInt();
				} else {
					// skip the label position latitude and longitude offsets (VBE-S)
					readVariableByteEncodedSignedInt();
					readVariableByteEncodedSignedInt();
					this.wayLabelPosition = null;
				}
			} else {
				// no label position
				this.wayLabelPosition = null;
			}

			// check if the way represents a multipolygon
			if (this.wayFeatureMultipolygon) {
				// get the amount of inner ways (VBE-U)
				this.wayNumberOfInnerWays = readVariableByteEncodedUnsignedInt();

				if (this.wayNumberOfInnerWays > 0
						&& this.wayNumberOfInnerWays < MAXIMUM_NUMBER_OF_INNER_WAYS) {
					// create a two-dimensional array for the coordinates of the inner ways
					this.wayInnerWays = new int[this.wayNumberOfInnerWays][];

					// for each inner way
					for (this.innerWayNumber = this.wayNumberOfInnerWays - 1; this.innerWayNumber >= 0; --this.innerWayNumber) {
						// get and check the number of inner way nodes (VBE-U)
						this.innerWayNumberOfWayNodes = readVariableByteEncodedUnsignedInt();
						if (this.innerWayNumberOfWayNodes < 1
								|| this.innerWayNumberOfWayNodes > MAXIMUM_WAY_NODES_SEQUENCE_LENGTH) {
							Logger.d("invalid inner way number of way nodes: "
									+ this.innerWayNumberOfWayNodes);
							if (this.debugFile) {
								Logger.d("way signature: " + this.waySignature);
								Logger.d("block signature: " + this.blockSignature);
							}
							return;
						}

						// each inner way node consists of a latitude and a longitude field
						this.innerWayNodesSequenceLength = this.innerWayNumberOfWayNodes * 2;

						// create an array for the inner way coordinates
						this.innerWay = new int[this.innerWayNodesSequenceLength];

						// get the first inner way node latitude offset (VBE-S)
						this.wayNodeLatitude = this.wayNodesSequence[1]
								+ readVariableByteEncodedSignedInt();
						// get the first inner way node longitude offset (VBE-S)
						this.wayNodeLongitude = this.wayNodesSequence[0]
								+ readVariableByteEncodedSignedInt();

						// store the first inner way node
						this.innerWay[1] = this.wayNodeLatitude;
						this.innerWay[0] = this.wayNodeLongitude;

						// get and store the remaining inner way nodes offsets
						for (this.tempInt = 2; this.tempInt < this.innerWayNodesSequenceLength; this.tempInt += 2) {
							// get the inner way node latitude offset (VBE-S)
							this.wayNodeLatitude = readVariableByteEncodedSignedInt();
							// get the inner way node longitude offset (VBE-S)
							this.wayNodeLongitude = readVariableByteEncodedSignedInt();

							// calculate the inner way node coordinates
							this.innerWay[this.tempInt] = this.innerWay[this.tempInt - 2]
									+ this.wayNodeLongitude;
							this.innerWay[this.tempInt + 1] = this.innerWay[this.tempInt - 1]
									+ this.wayNodeLatitude;
						}

						// store the inner way
						this.wayInnerWays[this.innerWayNumber] = this.innerWay;
					}
				} else {
					Logger.d("invalid way number of inner ways: " + this.wayNumberOfInnerWays);
					if (this.debugFile) {
						Logger.d("way signature: " + this.waySignature);
						Logger.d("block signature: " + this.blockSignature);
					}
					return;
				}
			} else {
				// no multipolygon
				this.wayInnerWays = null;
			}

			// render the way
			databaseMapGenerator.renderWay(this.wayLayer, this.wayNumberOfRelevantTags,
					this.wayName, this.wayRef, this.wayLabelPosition, this.wayTagIds,
					this.wayTagBitmap, this.wayNodesSequenceLength, this.wayNodesSequence,
					this.wayInnerWays);
		}
	}

	/**
	 * Reads and processes the header block from the file.
	 * 
	 * @return true if the header was processed successfully, false otherwise.
	 * @throws IOException
	 *             if an error occurs while reading the file.
	 */
	private boolean processFileHeader() throws IOException {
		// read the the magic byte and the file header size into the buffer
		if (!readFromMapFile(BINARY_OSM_MAGIC_BYTE.length() + 4)) {
			Logger.d("reading magic byte has failed");
			return false;
		}

		// get and check the magic byte
		this.magicByte = new String(this.readBuffer, this.bufferPosition, BINARY_OSM_MAGIC_BYTE
				.length(), "UTF-8");
		this.bufferPosition += BINARY_OSM_MAGIC_BYTE.length();
		if (!this.magicByte.equals(BINARY_OSM_MAGIC_BYTE)) {
			Logger.d("invalid magic byte: " + this.magicByte);
			return false;
		}

		// get and check the size of the remaining file header (4 bytes)
		this.remainingHeaderSize = readInt();
		if (this.remainingHeaderSize < REMAINING_HEADER_SIZE_MIN
				|| this.remainingHeaderSize > REMAINING_HEADER_SIZE_MAX) {
			Logger.d("invalid remaining header size: " + this.remainingHeaderSize);
			return false;
		}

		// read the header data into the buffer
		if (!readFromMapFile(this.remainingHeaderSize)) {
			Logger.d("reading header data has failed: " + this.remainingHeaderSize);
			return false;
		}

		// get and check the file version number (4 bytes)
		this.fileVersionNumber = readInt();
		if (this.fileVersionNumber < BINARY_OSM_VERSION_MIN
				|| this.fileVersionNumber > BINARY_OSM_VERSION_MAX) {
			Logger.d("unsupported file format version: " + this.fileVersionNumber);
			return false;
		}

		// get the meta-information byte that encodes multiple flags
		this.metaFlags = readByte();

		// extract the important flags from the meta-information byte
		this.debugFile = (this.metaFlags & HEADER_BITMASK_DEBUG) != 0;
		this.headerStartPosition = (this.metaFlags & HEADER_BITMASK_START_POSITION) != 0;

		// get and check the number of contained map files
		this.numberOfMapFiles = readByte();
		if (this.numberOfMapFiles < 1) {
			Logger.d("invalid number of contained map files: " + this.numberOfMapFiles);
			return false;
		}

		// get and check the projection name (VBE-U)
		this.projectionName = readUTF8EncodedString(true);

		// get and check the tile pixel size (2 bytes)
		this.tilePixelSize = readShort();
		if (this.tilePixelSize < 1) {
			Logger.d("invalid tile pixel size: " + this.tilePixelSize);
			return false;
		}

		// get and check the the top boundary (4 bytes)
		this.boundaryTop = readInt();
		if (this.boundaryTop > 90000000) {
			Logger.d("invalid top boundary: " + this.boundaryTop);
			return false;
		}

		// get and check the left boundary (4 bytes)
		this.boundaryLeft = readInt();
		if (this.boundaryLeft < -180000000) {
			Logger.d("invalid left boundary: " + this.boundaryLeft);
			return false;
		}

		// get and check the bottom boundary (4 bytes)
		this.boundaryBottom = readInt();
		if (this.boundaryBottom < -90000000) {
			Logger.d("invalid bottom boundary: " + this.boundaryBottom);
			return false;
		}

		// get and check the right boundary (4 bytes)
		this.boundaryRight = readInt();
		if (this.boundaryRight > 180000000) {
			Logger.d("invalid right boundary: " + this.boundaryRight);
			return false;
		}

		// create the map boundary rectangle
		this.mapBoundary = new Rect(this.boundaryLeft, this.boundaryBottom, this.boundaryRight,
				this.boundaryTop);

		// check if the header contains a start position
		if (this.headerStartPosition) {
			// get and check the start position latitude (4 byte)
			this.startPositionLatitude = readInt();
			if (this.startPositionLatitude < -90000000 || this.startPositionLatitude > 90000000) {
				Logger.d("invalid start position latitude: " + this.startPositionLatitude);
				return false;
			}

			// get and check the start position longitude (4 byte)
			this.startPositionLongitude = readInt();
			if (this.startPositionLongitude < -180000000
					|| this.startPositionLongitude > 180000000) {
				Logger.d("invalid start position longitude: " + this.startPositionLongitude);
				return false;
			}
		}

		// get and check the the map date (8 bytes)
		this.mapDate = readLong();
		if (this.mapDate < 0) {
			Logger.d("invalid map date: " + this.mapDate);
			return false;
		}

		// get and check the number of node tags (2 bytes)
		this.numberOfNodeTags = readShort();
		if (this.numberOfNodeTags < 0) {
			Logger.d("invalid number of node tags: " + this.numberOfNodeTags);
			return false;
		} else if (this.numberOfNodeTags > Short.MAX_VALUE) {
			Logger.d("invalid number of node tags: " + this.numberOfNodeTags);
			return false;
		}

		// create the hash map for the mapping of node tag IDs
		this.nodeTags = new HashMap<String, Integer>(
				(int) (this.numberOfNodeTags / LOAD_FACTOR) + 2, LOAD_FACTOR);

		// get the node tag mapping and store the maximum node tag ID
		this.maximumNodeTagId = 0;
		for (this.tempInt = 0; this.tempInt < this.numberOfNodeTags; ++this.tempInt) {
			// get and check the node tag
			this.nodeTag = readUTF8EncodedString(true);
			if (this.nodeTag == null) {
				return false;
			}

			// get and check the node tag ID (2 bytes)
			this.nodeTagId = readShort();
			if (this.nodeTagId < 0 || this.nodeTagId > MAXIMUM_ALLOWED_TAG_ID) {
				Logger.d("invalid node tag ID: " + this.nodeTagId);
				return false;
			}

			// check for an existing mapping of this node tag
			if (this.nodeTags.containsKey(this.nodeTag)) {
				Logger.d("duplicate node tag mapping: " + this.nodeTag);
				Logger.d("IDs: " + this.nodeTags.get(this.nodeTag) + " " + this.nodeTagId);
				return false;
			}

			// store the mapping in the hash map
			this.nodeTags.put(this.nodeTag, Integer.valueOf(this.nodeTagId));

			// update the maximum node tag ID information
			if (this.nodeTagId > this.maximumNodeTagId) {
				this.maximumNodeTagId = this.nodeTagId;
			}
		}

		// get and check the number of way tags (2 bytes)
		this.numberOfWayTags = readShort();
		if (this.numberOfWayTags < 0) {
			Logger.d("invalid number of way tags: " + this.numberOfWayTags);
			return false;
		}

		// create the hash map for the mapping of way tag IDs
		this.wayTags = new HashMap<String, Integer>(
				(int) (this.numberOfWayTags / LOAD_FACTOR) + 2, LOAD_FACTOR);

		// get the way tag mapping and store the maximum way tag ID
		this.maximumWayTagId = 0;
		for (this.tempInt = 0; this.tempInt < this.numberOfWayTags; ++this.tempInt) {
			// get and check the way tag
			this.wayTag = readUTF8EncodedString(true);
			if (this.wayTag == null) {
				return false;
			}

			// get and check the way tag ID (2 bytes)
			this.wayTagId = readShort();
			if (this.wayTagId < 0 || this.wayTagId > MAXIMUM_ALLOWED_TAG_ID) {
				Logger.d("invalid way tag ID: " + this.wayTagId);
				return false;
			}

			// check for an existing mapping of this way tag
			if (this.wayTags.containsKey(this.wayTag)) {
				Logger.d("duplicate way tag mapping: " + this.wayTag);
				Logger.d("IDs: " + this.wayTags.get(this.wayTag) + " " + this.wayTagId);
				return false;
			}

			// store the mapping in the hash map
			this.wayTags.put(this.wayTag, Integer.valueOf(this.wayTagId));

			// update the maximum way tag ID information
			if (this.wayTagId > this.maximumWayTagId) {
				this.maximumWayTagId = this.wayTagId;
			}
		}

		// get and check the comment text
		this.commentText = readUTF8EncodedString(true);

		// create the list of all contained map files
		this.mapFilesList = new MapFileParameters[this.numberOfMapFiles];
		this.globalMinimumZoomLevel = Byte.MAX_VALUE;
		this.globalMaximumZoomLevel = Byte.MIN_VALUE;

		// get and check the information for each contained map file
		for (this.tempByte = 0; this.tempByte < this.numberOfMapFiles; ++this.tempByte) {
			// get and check the base zoom level
			this.baseZoomLevel = readByte();
			if (this.baseZoomLevel < 0 || this.baseZoomLevel > 21) {
				Logger.d("invalid base zooom level: " + this.baseZoomLevel);
				return false;
			}

			// get and check the minimum zoom level
			this.zoomLevelMin = readByte();
			if (this.zoomLevelMin < 0 || this.zoomLevelMin > 21) {
				Logger.d("invalid minimum zoom level: " + this.zoomLevelMin);
				return false;
			}

			// get and check the maximum zoom level
			this.zoomLevelMax = readByte();
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

			// get and check the start address of the map file (5 bytes)
			this.startAddress = readFiveBytesLong();
			if (this.startAddress < 1 || this.startAddress >= this.fileSize) {
				Logger.d("invalid start address: " + this.startAddress);
				return false;
			}

			if (this.debugFile) {
				// the map file has an index signature before the index
				this.indexStartAddress = this.startAddress + SIGNATURE_LENGTH_INDEX;
			} else {
				// the map file begins directly with the index
				this.indexStartAddress = this.startAddress;
			}

			// get and check the size of the map file (5 bytes)
			this.mapFileSize = readFiveBytesLong();
			if (this.mapFileSize < 1) {
				Logger.d("invalid map file size: " + this.mapFileSize);
				return false;
			}

			// add the current map file to the map files list
			this.mapFilesList[this.tempByte] = new MapFileParameters(this.startAddress,
					this.indexStartAddress, this.mapFileSize, this.baseZoomLevel,
					this.zoomLevelMin, this.zoomLevelMax, this.mapBoundary);

			// update the global minimum and maximum zoom level information
			if (this.zoomLevelMin < this.globalMinimumZoomLevel) {
				this.globalMinimumZoomLevel = this.zoomLevelMin;
			}
			if (this.zoomLevelMax > this.globalMaximumZoomLevel) {
				this.globalMaximumZoomLevel = this.zoomLevelMax;
			}
		}

		// create and fill the lookup table for the map files
		this.mapFilesLookupTable = new MapFileParameters[this.globalMaximumZoomLevel + 1];
		for (this.tempInt = 0; this.tempInt < this.numberOfMapFiles; ++this.tempInt) {
			this.mapFileParameters = this.mapFilesList[this.tempInt];
			for (this.tempByte = this.mapFileParameters.zoomLevelMin; this.tempByte <= this.mapFileParameters.zoomLevelMax; ++this.tempByte) {
				this.mapFilesLookupTable[this.tempByte] = this.mapFileParameters;
			}
		}

		return true;
	}

	/**
	 * Returns one signed byte from the read buffer.
	 * 
	 * @return the byte value.
	 */
	private byte readByte() {
		return this.readBuffer[this.bufferPosition++];
	}

	/**
	 * Converts five bytes from the read buffer to an unsigned long.
	 * <p>
	 * The byte order is big-endian.
	 * 
	 * @return the long value.
	 */
	private long readFiveBytesLong() {
		this.bufferPosition += 5;
		return Deserializer.getFiveBytesLong(this.readBuffer, this.bufferPosition - 5);
	}

	/**
	 * Reads the given amount of bytes from the map file into the read buffer and resets the internal
	 * buffer position. If the capacity of the read buffer is too small, a larger read buffer is created
	 * automatically.
	 * 
	 * @param length
	 *            the amount of bytes to read from the map file.
	 * @return true if the whole data was read successfully, false otherwise.
	 * @throws IOException
	 *             if an error occurs while reading the file.
	 */
	private boolean readFromMapFile(int length) throws IOException {
		// ensure that the read buffer is large enough
		if (this.readBuffer == null || this.readBuffer.length < length) {
			// ensure that the read buffer is not too large
			if (length > MAXIMUM_BLOCK_SIZE) {
				return false;
			}
			this.readBuffer = new byte[length];
		}

		// reset the buffer position and read the data into the buffer
		this.bufferPosition = 0;
		return this.inputFile.read(this.readBuffer, 0, length) == length;
	}

	/**
	 * Converts four bytes from the read buffer to a signed int.
	 * <p>
	 * The byte order is big-endian.
	 * 
	 * @return the int value.
	 */
	private int readInt() {
		this.bufferPosition += 4;
		return Deserializer.getInt(this.readBuffer, this.bufferPosition - 4);
	}

	/**
	 * Converts eight bytes from the read buffer to a signed long.
	 * <p>
	 * The byte order is big-endian.
	 * 
	 * @return the long value.
	 */
	private long readLong() {
		this.bufferPosition += 8;
		return Deserializer.getLong(this.readBuffer, this.bufferPosition - 8);
	}

	/**
	 * Converts two bytes from the read buffer to a signed short.
	 * <p>
	 * The byte order is big-endian.
	 * 
	 * @return the short value.
	 */
	private short readShort() {
		this.bufferPosition += 2;
		return Deserializer.getShort(this.readBuffer, this.bufferPosition - 2);
	}

	/**
	 * Decodes a variable amount of bytes from the read buffer to a string.
	 * 
	 * @param readString
	 *            true if the string should be decoded and returned, false otherwise.
	 * @return the UTF-8 decoded string (may be null).
	 * @throws UnsupportedEncodingException
	 *             if string decoding fails.
	 */
	private String readUTF8EncodedString(boolean readString)
			throws UnsupportedEncodingException {
		// get and check the length of string (VBE-U)
		this.stringLength = readVariableByteEncodedUnsignedInt();
		if (this.stringLength >= 0
				&& this.bufferPosition + this.stringLength <= this.readBuffer.length) {
			this.bufferPosition += this.stringLength;
			if (readString) {
				// get the string
				return new String(this.readBuffer, this.bufferPosition - this.stringLength,
						this.stringLength, "UTF-8");
			}
			return null;
		}
		Logger.d("invalid string length: " + this.stringLength);
		return null;
	}

	/**
	 * Converts a variable amount of bytes from the read buffer to a signed int.
	 * <p>
	 * The first bit is for continuation info, the other six (last byte) or seven (all other bytes) bits
	 * for data. The second bit in the last byte indicates the sign of the number.
	 * 
	 * @return the int value.
	 */
	private int readVariableByteEncodedSignedInt() {
		this.variableByteDecode = 0;
		this.variableByteShift = 0;

		// check if the continuation bit is set
		while ((this.readBuffer[this.bufferPosition] & 0x80) != 0) {
			this.variableByteDecode |= (this.readBuffer[this.bufferPosition++] & 0x7f) << this.variableByteShift;
			this.variableByteShift += 7;
		}

		// read the six data bits from the last byte
		if ((this.readBuffer[this.bufferPosition] & 0x40) != 0) {
			// negative
			return -(this.variableByteDecode | ((this.readBuffer[this.bufferPosition++] & 0x3f) << this.variableByteShift));
		}
		// positive
		return this.variableByteDecode
				| ((this.readBuffer[this.bufferPosition++] & 0x3f) << this.variableByteShift);
	}

	/**
	 * Converts a variable amount of bytes from the read buffer to an unsigned int.
	 * <p>
	 * The first bit is for continuation info, the other seven bits for data.
	 * 
	 * @return the int value.
	 */
	private int readVariableByteEncodedUnsignedInt() {
		this.variableByteDecode = 0;
		this.variableByteShift = 0;

		// check if the continuation bit is set
		while ((this.readBuffer[this.bufferPosition] & 0x80) != 0) {
			this.variableByteDecode |= (this.readBuffer[this.bufferPosition++] & 0x7f) << this.variableByteShift;
			this.variableByteShift += 7;
		}

		// read the seven data bits from the last byte
		return this.variableByteDecode
				| (this.readBuffer[this.bufferPosition++] << this.variableByteShift);
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
	 * @param databaseMapGenerator
	 *            the DatabaseMapGenerator callback which handles the extracted map elements.
	 */
	void executeQuery(Tile tile, boolean readWayNames, DatabaseMapGenerator databaseMapGenerator) {
		try {
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
			this.mapFileParameters = this.mapFilesLookupTable[this.queryZoomLevel];
			if (this.mapFileParameters == null) {
				Logger.d("no map file for zoom level: " + tile.zoomLevel);
				return;
			}

			this.queryReadWayNames = readWayNames;

			// calculate the blocks that cover the area of the requested tile
			if (tile.zoomLevel < this.mapFileParameters.baseZoomLevel) {
				// calculate the XY numbers of the upper left and lower right subtiles
				this.zoomLevelDifference = this.mapFileParameters.baseZoomLevel
						- tile.zoomLevel;
				this.fromBaseTileX = tile.x << this.zoomLevelDifference;
				this.fromBaseTileY = tile.y << this.zoomLevelDifference;
				this.toBaseTileX = this.fromBaseTileX + (1 << this.zoomLevelDifference) - 1;
				this.toBaseTileY = this.fromBaseTileY + (1 << this.zoomLevelDifference) - 1;
				this.useTileBitmask = false;
				databaseMapGenerator.renderCoastlineTile(tile);
			} else if (tile.zoomLevel > this.mapFileParameters.baseZoomLevel) {
				// calculate the XY numbers of the parent base tile
				this.zoomLevelDifference = tile.zoomLevel
						- this.mapFileParameters.baseZoomLevel;
				this.fromBaseTileX = tile.x >>> this.zoomLevelDifference;
				this.fromBaseTileY = tile.y >>> this.zoomLevelDifference;
				this.toBaseTileX = this.fromBaseTileX;
				this.toBaseTileY = this.fromBaseTileY;
				databaseMapGenerator.renderCoastlineTile(new Tile(this.fromBaseTileX,
						this.fromBaseTileY, this.mapFileParameters.baseZoomLevel));

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
					this.subtileX = tile.x >>> (this.zoomLevelDifference - 2);
					this.subtileY = tile.y >>> (this.zoomLevelDifference - 2);

					// calculate the XY numbers of the parent tile
					this.parentTileX = this.subtileX >>> 1;
					this.parentTileY = this.subtileY >>> 1;

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
				databaseMapGenerator.renderCoastlineTile(tile);
			}

			// calculate the blocks in the file which need to be read
			this.fromBlockX = Math.max(this.fromBaseTileX
					- this.mapFileParameters.boundaryLeftTile, 0);
			this.fromBlockY = Math.max(this.fromBaseTileY
					- this.mapFileParameters.boundaryTopTile, 0);
			this.toBlockX = Math.min(
					this.toBaseTileX - this.mapFileParameters.boundaryLeftTile,
					this.mapFileParameters.blocksWidth - 1);
			this.toBlockY = Math.min(this.toBaseTileY - this.mapFileParameters.boundaryTopTile,
					this.mapFileParameters.blocksHeight - 1);

			this.queryIsWater = true;
			this.queryReadWaterInfo = false;

			// read and process all necessary blocks from top to bottom and from left to right
			for (this.currentRow = this.fromBlockY; this.currentRow <= this.toBlockY; ++this.currentRow) {
				for (this.currentColumn = this.fromBlockX; this.currentColumn <= this.toBlockX; ++this.currentColumn) {
					// check if the query was interrupted
					if (this.stopCurrentQuery) {
						return;
					}

					// calculate the actual block number of the needed block in the file
					this.blockNumber = this.currentRow * this.mapFileParameters.blocksWidth
							+ this.currentColumn;

					// get the current index entry
					this.currentBlockIndexEntry = this.databaseIndexCache.getIndexEntry(
							this.mapFileParameters, this.blockNumber);

					// check if the current query would still return a water tile
					if (this.queryIsWater) {
						// check the water flag of the current block
						this.currentBlockIsWater = (this.currentBlockIndexEntry & BITMASK_INDEX_WATER) != 0;
						this.queryIsWater = this.queryIsWater && this.currentBlockIsWater;
						this.queryReadWaterInfo = true;
					}

					// get and check the current block pointer
					this.currentBlockPointer = this.currentBlockIndexEntry
							& BITMASK_INDEX_OFFSET;
					if (this.currentBlockPointer < 1
							|| this.currentBlockPointer > this.mapFileParameters.mapFileSize) {
						Logger.d("invalid current block pointer: " + this.currentBlockPointer);
						Logger.d("mapFileSize: " + this.mapFileParameters.mapFileSize);
						return;
					}

					// check if the current block is the last block in the file
					if (this.blockNumber + 1 == this.mapFileParameters.numberOfBlocks) {
						// set the next block pointer to the end of the file
						this.nextBlockPointer = this.mapFileParameters.mapFileSize;
					} else {
						// get and check the next block pointer
						this.nextBlockPointer = this.databaseIndexCache.getIndexEntry(
								this.mapFileParameters, this.blockNumber + 1)
								& BITMASK_INDEX_OFFSET;
						if (this.nextBlockPointer < 1
								|| this.nextBlockPointer > this.mapFileParameters.mapFileSize) {
							Logger.d("invalid next block pointer: " + this.nextBlockPointer);
							Logger.d("mapFileSize: " + this.mapFileParameters.mapFileSize);
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
						continue;
					} else if (this.currentBlockSize > MAXIMUM_BLOCK_SIZE) {
						// the current block is too large, continue with the next block
						Logger.d("current block size too large: " + this.currentBlockSize);
						continue;
					} else if (this.currentBlockPointer + this.currentBlockSize > this.fileSize) {
						Logger.d("invalid current block size: " + this.currentBlockSize);
						return;
					}

					// go to the current block in the map file and read the data into the buffer
					this.inputFile.seek(this.mapFileParameters.startAddress
							+ this.currentBlockPointer);

					// read the current block into the buffer
					if (!readFromMapFile(this.currentBlockSize)) {
						// skip the current block
						Logger.d("reading current block has failed: " + this.currentBlockSize);
						return;
					}

					// calculate the top-left coordinates of the underlying tile
					this.tileLatitude = (int) (MercatorProjection.tileYToLatitude(
							this.mapFileParameters.boundaryTopTile + this.currentRow,
							this.mapFileParameters.baseZoomLevel) * 1000000);
					this.tileLongitude = (int) (MercatorProjection.tileXToLongitude(
							this.mapFileParameters.boundaryLeftTile + this.currentColumn,
							this.mapFileParameters.baseZoomLevel) * 1000000);

					// handle the current block data
					processBlock(databaseMapGenerator);
				}
			}

			// the query is finished, was the water flag set for all blocks?
			if (this.queryIsWater && this.queryReadWaterInfo) {
				// render the water background
				databaseMapGenerator.renderWaterBackground();
			}
		} catch (IOException e) {
			Logger.e(e);
		}
	}

	/**
	 * Returns the mapping of node tags to IDs in the current map file.
	 * 
	 * @return a map containing the tags and their corresponding IDs.
	 */
	HashMap<String, Integer> getNodeTags() {
		return this.nodeTags;
	}

	/**
	 * Returns the name of the projection as it is encoded in the map file.
	 * 
	 * @return the projection name of the map file.
	 */
	String getProjection() {
		return this.projectionName;
	}

	/**
	 * Returns the mapping of way tags to IDs in the current map file.
	 * 
	 * @return a map containing the tags and their corresponding IDs.
	 */
	HashMap<String, Integer> getWayTags() {
		return this.wayTags;
	}

	/**
	 * Returns the current state of the database.
	 * 
	 * @return true if the database has an open map file, false otherwise.
	 */
	boolean hasOpenFile() {
		return this.inputFile != null;
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
			if (!processFileHeader()) {
				return false;
			}

			// create the DatabaseIndexCache
			this.databaseIndexCache = new MapDatabaseIndexCache(this.inputFile,
					INDEX_CACHE_SIZE);

			// create an array for the way nodes coordinates
			this.wayNodesSequence = new int[INITIAL_WAY_NODES_CAPACITY];

			// create the tag arrays
			this.defaultTagIds = new boolean[Math.max(this.maximumNodeTagId,
					this.maximumWayTagId) + 1];
			this.nodeTagIds = new boolean[this.maximumNodeTagId + 1];
			this.wayTagIds = new boolean[this.maximumWayTagId + 1];

			return true;
		} catch (IOException e) {
			Logger.e(e);
			// make sure that the file is closed
			closeFile();
			return false;
		}
	}

	/**
	 * Notifies the database reader to stop the currently executed query.
	 */
	void stopCurrentQuery() {
		this.stopCurrentQuery = true;
	}
}