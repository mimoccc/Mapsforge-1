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
package org.mapsforge.android.maps.mapdatabase;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.mapsforge.android.maps.GeoPoint;
import org.mapsforge.android.maps.Logger;
import org.mapsforge.android.maps.MercatorProjection;
import org.mapsforge.android.maps.Tile;
import org.mapsforge.android.maps.rendertheme.Tag;

import android.graphics.Rect;

/**
 * A class for reading binary map files.
 * <p>
 * This class is not thread-safe. Each thread should use its own instance.
 * 
 * @see <a href="http://code.google.com/p/mapsforge/wiki/SpecificationBinaryMapFile">Specification</a>
 */
public class MapDatabase {
	/**
	 * Maximum supported version of the map file format.
	 */
	public static final int BINARY_OSM_VERSION_MAX = 3;

	/**
	 * Minimal supported version of the map file format.
	 */
	public static final int BINARY_OSM_VERSION_MIN = 3;

	/**
	 * Magic byte at the beginning of a valid binary map file.
	 */
	private static final String BINARY_OSM_MAGIC_BYTE = "mapsforge binary OSM";

	/**
	 * Bitmask to extract the block offset from an index entry.
	 */
	private static final long BITMASK_INDEX_OFFSET = 0x7FFFFFFFFFL;

	/**
	 * Bitmask to extract the water information from an index entry.
	 */
	private static final long BITMASK_INDEX_WATER = 0x8000000000L;

	/**
	 * Name of the UTF-8 character set, used to decode strings.
	 */
	private static final String CHARSET_UTF8 = "UTF-8";

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
	 * The maximum latitude values in microdegrees.
	 */
	private static final int LATITUDE_MAX = 90000000;

	/**
	 * The minimum latitude values in microdegrees.
	 */
	private static final int LATITUDE_MIN = -90000000;

	/**
	 * The maximum longitude values in microdegrees.
	 */
	private static final int LONGITUDE_MAX = 180000000;

	/**
	 * The minimum longitude values in microdegrees.
	 */
	private static final int LONGITUDE_MIN = -180000000;

	/**
	 * Maximum size of a single block in bytes that is supported by this implementation.
	 */
	private static final int MAXIMUM_BLOCK_SIZE = 2500000;

	/**
	 * Maximum number of inner ways which is considered as valid.
	 */
	private static final int MAXIMUM_NUMBER_OF_INNER_WAYS = 256;

	/**
	 * Maximum way nodes sequence length which is considered as valid.
	 */
	private static final int MAXIMUM_WAY_NODES_SEQUENCE_LENGTH = 8192;

	/**
	 * The name of the Mercator projection as stored in the file header.
	 */
	private static final String MERCATOR = "Mercator";

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
	private static final int REMAINING_HEADER_SIZE_MIN = 75;

	private static final String SIGNATURE_BLOCK = "block signature: ";

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

	private static final String SIGNATURE_NODE = "node signature: ";

	private static final String SIGNATURE_WAY = "way signature: ";

	/**
	 * The key of the elevation OpenStreetMap tag.
	 */
	private static final String TAG_KEY_ELE = "ele";

	/**
	 * The key of the house number OpenStreetMap tag.
	 */
	private static final String TAG_KEY_HOUSE_NUMBER = "addr:housenumber";

	/**
	 * The key of the name OpenStreetMap tag.
	 */
	private static final String TAG_KEY_NAME = "name";

	/**
	 * The key of the reference OpenStreetMap tag.
	 */
	private static final String TAG_KEY_REF = "ref";

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
	 * Convenience method to check whether the given file is a valid map file.
	 * 
	 * @param file
	 *            the path to the map file that should be tested.
	 * @return true if the file is a valid map file, false otherwise.
	 */
	public static boolean isValidMapFile(String file) {
		MapDatabase testDatabase = new MapDatabase();
		boolean isValid = testDatabase.openFile(file);
		testDatabase.closeFile();
		return isValid;
	}

	private String blockSignature;
	private int bufferPosition;
	private String commentText;
	private IndexCache databaseIndexCache;
	private boolean debugFile;
	private long fileSize;
	private int fileVersion;
	private long fromBaseTileX;
	private long fromBaseTileY;
	private byte globalMaximumZoomLevel;
	private byte globalMinimumZoomLevel;
	private int innerWayNodesSequenceLength;
	private int innerWayNumber;
	private int innerWayNumberOfWayNodes;
	private RandomAccessFile inputFile;
	private String languagePreference;
	private Rect mapBoundary;
	private long mapDate;
	private MapFileParameters[] mapFilesLookupTable;
	private boolean mapStartPosition;
	private long nextBlockPointer;
	private String nodeSignature;
	private Tag[] nodeTags;
	private long parentTileX;
	private long parentTileY;
	private String projectionName;
	private int queryTileBitmask;
	private int queryZoomLevel;
	private byte[] readBuffer;
	private int startPositionLatitude;
	private int startPositionLongitude;
	private long subtileX;
	private long subtileY;
	private List<Tag> tagList;
	private int tileLatitude;
	private int tileLongitude;
	private long toBaseTileX;
	private long toBaseTileY;
	private boolean useTileBitmask;
	private int variableByteDecode;
	private byte variableByteShift;
	private float[] wayLabelPosition;
	private float[][] wayNodes;
	private int wayNumberOfInnerWays;
	private String waySignature;
	private Tag[] wayTags;
	private int wayTileBitmask;
	private int zoomLevelDifference;

	/**
	 * Closes the map file. Has no effect if no map file is currently opened.
	 */
	public void closeFile() {
		try {
			if (this.databaseIndexCache != null) {
				this.databaseIndexCache.destroy();
				this.databaseIndexCache = null;
			}

			if (this.inputFile != null) {
				this.inputFile.close();
				this.inputFile = null;
			}

			this.readBuffer = null;
		} catch (IOException e) {
			Logger.exception(e);
		}
	}

	/**
	 * Starts a database query with the given parameters.
	 * 
	 * @param tile
	 *            the tile to read.
	 * @param mapDatabaseCallback
	 *            the callback which handles the extracted map elements.
	 */
	public void executeQuery(Tile tile, MapDatabaseCallback mapDatabaseCallback) {
		try {
			prepareExecution();

			// limit the zoom level of the requested tile for this query
			if (tile.zoomLevel > this.globalMaximumZoomLevel) {
				this.queryZoomLevel = this.globalMaximumZoomLevel;
			} else if (tile.zoomLevel < this.globalMinimumZoomLevel) {
				this.queryZoomLevel = this.globalMinimumZoomLevel;
			} else {
				this.queryZoomLevel = tile.zoomLevel;
			}

			// get and check the map file for the query zoom level
			MapFileParameters mapFileParameters = this.mapFilesLookupTable[this.queryZoomLevel];
			if (mapFileParameters == null) {
				Logger.debug("no map file for zoom level: " + tile.zoomLevel);
				return;
			}

			// calculate the blocks that cover the area of the requested tile
			if (tile.zoomLevel < mapFileParameters.baseZoomLevel) {
				// calculate the XY numbers of the upper left and lower right subtiles
				this.zoomLevelDifference = mapFileParameters.baseZoomLevel - tile.zoomLevel;
				this.fromBaseTileX = tile.x << this.zoomLevelDifference;
				this.fromBaseTileY = tile.y << this.zoomLevelDifference;
				this.toBaseTileX = this.fromBaseTileX + (1 << this.zoomLevelDifference) - 1;
				this.toBaseTileY = this.fromBaseTileY + (1 << this.zoomLevelDifference) - 1;
				this.useTileBitmask = false;
			} else if (tile.zoomLevel > mapFileParameters.baseZoomLevel) {
				// calculate the XY numbers of the parent base tile
				this.zoomLevelDifference = tile.zoomLevel - mapFileParameters.baseZoomLevel;
				this.fromBaseTileX = tile.x >>> this.zoomLevelDifference;
				this.fromBaseTileY = tile.y >>> this.zoomLevelDifference;
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
			}

			// calculate the blocks in the file which need to be read
			long fromBlockX = Math.max(this.fromBaseTileX - mapFileParameters.boundaryLeftTile, 0);
			long fromBlockY = Math.max(this.fromBaseTileY - mapFileParameters.boundaryTopTile, 0);
			long toBlockX = Math.min(this.toBaseTileX - mapFileParameters.boundaryLeftTile,
					mapFileParameters.blocksWidth - 1);
			long toBlockY = Math.min(this.toBaseTileY - mapFileParameters.boundaryTopTile,
					mapFileParameters.blocksHeight - 1);

			boolean queryIsWater = true;
			boolean queryReadWaterInfo = false;

			// read and process all necessary blocks from top to bottom and from left to right
			for (long currentRow = fromBlockY; currentRow <= toBlockY; ++currentRow) {
				for (long currentColumn = fromBlockX; currentColumn <= toBlockX; ++currentColumn) {
					// calculate the actual block number of the needed block in the file
					long blockNumber = currentRow * mapFileParameters.blocksWidth + currentColumn;

					// get the current index entry
					long currentBlockIndexEntry = this.databaseIndexCache.getIndexEntry(
							mapFileParameters, blockNumber);

					// check if the current query would still return a water tile
					if (queryIsWater) {
						// check the water flag of the current block
						boolean currentBlockIsWater = (currentBlockIndexEntry & BITMASK_INDEX_WATER) != 0;
						queryIsWater = queryIsWater && currentBlockIsWater;
						queryReadWaterInfo = true;
					}

					// get and check the current block pointer
					long currentBlockPointer = currentBlockIndexEntry & BITMASK_INDEX_OFFSET;
					if (currentBlockPointer < 1 || currentBlockPointer > mapFileParameters.mapFileSize) {
						Logger.debug("invalid current block pointer: " + currentBlockPointer);
						Logger.debug("mapFileSize: " + mapFileParameters.mapFileSize);
						return;
					}

					// check if the current block is the last block in the file
					if (blockNumber + 1 == mapFileParameters.numberOfBlocks) {
						// set the next block pointer to the end of the file
						this.nextBlockPointer = mapFileParameters.mapFileSize;
					} else {
						// get and check the next block pointer
						this.nextBlockPointer = this.databaseIndexCache.getIndexEntry(
								mapFileParameters, blockNumber + 1) & BITMASK_INDEX_OFFSET;
						if (this.nextBlockPointer < 1
								|| this.nextBlockPointer > mapFileParameters.mapFileSize) {
							Logger.debug("invalid next block pointer: " + this.nextBlockPointer);
							Logger.debug("mapFileSize: " + mapFileParameters.mapFileSize);
							return;
						}
					}

					// calculate the size of the current block
					int currentBlockSize = (int) (this.nextBlockPointer - currentBlockPointer);
					if (currentBlockSize < 0) {
						Logger.debug("invalid current block size: " + currentBlockSize);
						return;
					} else if (currentBlockSize == 0) {
						// the current block is empty, continue with the next block
						continue;
					} else if (currentBlockSize > MAXIMUM_BLOCK_SIZE) {
						// the current block is too large, continue with the next block
						Logger.debug("current block size too large: " + currentBlockSize);
						continue;
					} else if (currentBlockPointer + currentBlockSize > this.fileSize) {
						Logger.debug("invalid current block size: " + currentBlockSize);
						return;
					}

					// go to the current block in the map file and read the data into the buffer
					this.inputFile.seek(mapFileParameters.startAddress + currentBlockPointer);

					// read the current block into the buffer
					if (!readFromMapFile(currentBlockSize)) {
						// skip the current block
						Logger.debug("reading current block has failed: " + currentBlockSize);
						return;
					}

					// calculate the top-left coordinates of the underlying tile
					this.tileLatitude = (int) (MercatorProjection.tileYToLatitude(
							mapFileParameters.boundaryTopTile + currentRow,
							mapFileParameters.baseZoomLevel) * 1000000);
					this.tileLongitude = (int) (MercatorProjection.tileXToLongitude(
							mapFileParameters.boundaryLeftTile + currentColumn,
							mapFileParameters.baseZoomLevel) * 1000000);

					// handle the current block data
					processBlock(mapFileParameters, mapDatabaseCallback);
				}
			}

			// the query is finished, was the water flag set for all blocks?
			if (queryIsWater && queryReadWaterInfo) {
				// render the water background
				mapDatabaseCallback.renderWaterBackground();
			}
		} catch (IOException e) {
			Logger.exception(e);
		}
	}

	/**
	 * Returns the comment text of the current map file (may be null).
	 * 
	 * @return the comment text of the current map file (may be null).
	 */
	public String getCommentText() {
		return this.commentText;
	}

	/**
	 * Returns the size of the current map file, measured in bytes.
	 * 
	 * @return the size of the current map file, measured in bytes.
	 */
	public long getFileSize() {
		return this.fileSize;
	}

	/**
	 * Returns the file version number of the current map file.
	 * 
	 * @return the file version number of the current map file.
	 */
	public int getFileVersion() {
		return this.fileVersion;
	}

	/**
	 * Returns the preferred language for names as defined in ISO 3166-1 (may be null).
	 * 
	 * @return the preferred language for names as defined in ISO 3166-1 (may be null).
	 */
	public String getLanguagePreference() {
		return this.languagePreference;
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
	 * Returns the name of the projection of the map file (may be null).
	 * 
	 * @return the name of the projection of the map file (may be null).
	 */
	public String getProjection() {
		return this.projectionName;
	}

	/**
	 * Returns the start position from the map file header (may be null).
	 * 
	 * @return the start position from the map file header (may be null).
	 */
	public GeoPoint getStartPosition() {
		if (this.mapStartPosition) {
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
	 * Opens the given map file, reads its header data and validates them.
	 * 
	 * @param fileName
	 *            the path to the map file.
	 * @return true if the file could be opened and is a valid map file, false otherwise.
	 * @throws IllegalArgumentException
	 *             if the given fileName is null.
	 */
	public boolean openFile(String fileName) {
		try {
			// make sure to close any previous file first
			closeFile();

			// check for null parameter
			if (fileName == null) {
				throw new IllegalArgumentException("fileName must not be null");
			}

			// check if the file exists and is readable
			File file = new File(fileName);
			if (!file.exists()) {
				Logger.debug("file does not exist: " + fileName);
				return false;
			} else if (!file.isFile()) {
				Logger.debug("not a file: " + fileName);
				return false;
			} else if (!file.canRead()) {
				Logger.debug("cannot read file: " + fileName);
				return false;
			}

			// open the binary map file in read only mode
			this.inputFile = new RandomAccessFile(file, "r");
			this.fileSize = this.inputFile.length();

			// read the header data from the file
			if (!processFileHeader()) {
				closeFile();
				return false;
			}

			return true;
		} catch (IOException e) {
			Logger.exception(e);
			// make sure that the file is closed
			closeFile();
			return false;
		}
	}

	/**
	 * Logs the signature of the current way and block.
	 */
	private void logSignatures() {
		if (this.debugFile) {
			Logger.debug(SIGNATURE_WAY + this.waySignature);
			Logger.debug(SIGNATURE_BLOCK + this.blockSignature);
		}
	}

	/**
	 * Prepares and sets up the internal data structures and caches.
	 */
	private void prepareExecution() {
		if (this.databaseIndexCache == null) {
			this.databaseIndexCache = new IndexCache(this.inputFile, INDEX_CACHE_SIZE);
		}

		if (this.tagList == null) {
			this.tagList = new ArrayList<Tag>();
		}
	}

	/**
	 * Processes a single block and executes the callback functions on all map elements.
	 * 
	 * @param mapFileParameters
	 *            the parameters of the current map file.
	 * @param mapDatabaseCallback
	 *            the callback which handles the extracted map elements.
	 * @throws UnsupportedEncodingException
	 *             if string decoding fails.
	 */
	private void processBlock(MapFileParameters mapFileParameters,
			MapDatabaseCallback mapDatabaseCallback) throws UnsupportedEncodingException {
		if (!processBlockSignature()) {
			return;
		}

		// calculate the offset in the block entries table and move the pointer
		int blockEntriesTableOffset = (this.queryZoomLevel - mapFileParameters.zoomLevelMin) * 4;
		this.bufferPosition += blockEntriesTableOffset;

		// get the amount of nodes and ways on the current zoomLevel level
		int nodesOnZoomLevel = readShort();
		int waysOnZoomLevel = readShort();

		// move the pointer to the end of the block entries table
		this.bufferPosition += mapFileParameters.blockEntriesTableSize - blockEntriesTableOffset
				- 4;

		// get the relative offset to the first stored way in the block
		int firstWayOffset = readUnsignedInt();
		if (firstWayOffset < 0) {
			Logger.debug("invalid first way offset: " + firstWayOffset);
			if (this.debugFile) {
				Logger.debug(SIGNATURE_BLOCK + this.blockSignature);
			}
			return;
		}

		// add the current buffer position to the relative first way offset
		firstWayOffset += this.bufferPosition;
		if (firstWayOffset > this.readBuffer.length) {
			Logger.debug("invalid first way offset: " + firstWayOffset);
			if (this.debugFile) {
				Logger.debug(SIGNATURE_BLOCK + this.blockSignature);
			}
			return;
		}

		if (!processNodes(mapDatabaseCallback, nodesOnZoomLevel)) {
			return;
		}

		// finished reading nodes, check if the current buffer position is valid
		if (this.bufferPosition > firstWayOffset) {
			Logger.debug("invalid buffer position: " + this.bufferPosition + " - "
					+ firstWayOffset);
			if (this.debugFile) {
				Logger.debug(SIGNATURE_BLOCK + this.blockSignature);
			}
			return;
		}

		// move the pointer to the first way
		this.bufferPosition = firstWayOffset;

		if (!processWays(mapDatabaseCallback, waysOnZoomLevel)) {
			return;
		}
	}

	/**
	 * Processes the block signature, if present.
	 * 
	 * @return true if the block signature could be processed successfully, false otherwise.
	 * @throws UnsupportedEncodingException
	 *             if string decoding fails.
	 */
	private boolean processBlockSignature() throws UnsupportedEncodingException {
		if (this.debugFile) {
			// get and check the block signature
			this.blockSignature = new String(this.readBuffer, this.bufferPosition,
					SIGNATURE_LENGTH_BLOCK, CHARSET_UTF8);
			this.bufferPosition += SIGNATURE_LENGTH_BLOCK;
			if (!this.blockSignature.startsWith("###TileStart")) {
				Logger.debug("invalid block signature: " + this.blockSignature);
				return false;
			}
		}

		return true;
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
			Logger.debug("reading magic byte has failed");
			return false;
		}

		// get and check the magic byte
		String magicByte = new String(this.readBuffer, this.bufferPosition, BINARY_OSM_MAGIC_BYTE
				.length(), CHARSET_UTF8);
		this.bufferPosition += BINARY_OSM_MAGIC_BYTE.length();
		if (!magicByte.equals(BINARY_OSM_MAGIC_BYTE)) {
			Logger.debug("invalid magic byte: " + magicByte);
			return false;
		}

		// get and check the size of the remaining file header (4 bytes)
		int remainingHeaderSize = readInt();
		if (remainingHeaderSize < REMAINING_HEADER_SIZE_MIN
				|| remainingHeaderSize > REMAINING_HEADER_SIZE_MAX) {
			Logger.debug("invalid remaining header size: " + remainingHeaderSize);
			return false;
		}

		// read the header data into the buffer
		if (!readFromMapFile(remainingHeaderSize)) {
			Logger.debug("reading header data has failed: " + remainingHeaderSize);
			return false;
		}

		// get and check the file version (4 bytes)
		this.fileVersion = readInt();
		if (this.fileVersion < BINARY_OSM_VERSION_MIN || this.fileVersion > BINARY_OSM_VERSION_MAX) {
			Logger.debug("unsupported file version: " + this.fileVersion);
			return false;
		}

		// get and check the file size (8 bytes)
		long headerFileSize = readLong();
		if (headerFileSize != this.fileSize) {
			Logger.debug("invalid file size: " + headerFileSize);
			return false;
		}

		// get and check the the map date (8 bytes)
		this.mapDate = readLong();
		if (this.mapDate < 0) {
			Logger.debug("invalid map date: " + this.mapDate);
			return false;
		}

		// get and check the the top boundary (4 bytes)
		int boundaryTop = readInt();
		if (boundaryTop > LATITUDE_MAX) {
			Logger.debug("invalid top boundary: " + boundaryTop);
			return false;
		}

		// get and check the left boundary (4 bytes)
		int boundaryLeft = readInt();
		if (boundaryLeft < LONGITUDE_MIN) {
			Logger.debug("invalid left boundary: " + boundaryLeft);
			return false;
		}

		// get and check the bottom boundary (4 bytes)
		int boundaryBottom = readInt();
		if (boundaryBottom < LATITUDE_MIN) {
			Logger.debug("invalid bottom boundary: " + boundaryBottom);
			return false;
		}

		// get and check the right boundary (4 bytes)
		int boundaryRight = readInt();
		if (boundaryRight > LONGITUDE_MAX) {
			Logger.debug("invalid right boundary: " + boundaryRight);
			return false;
		}

		// create the map boundary rectangle
		this.mapBoundary = new Rect(boundaryLeft, boundaryBottom, boundaryRight, boundaryTop);

		// get and check the tile pixel size (2 bytes)
		int tilePixelSize = readShort();
		if (tilePixelSize < 1) {
			Logger.debug("invalid tile pixel size: " + tilePixelSize);
			return false;
		} else if (tilePixelSize != Tile.TILE_SIZE) {
			Logger.debug("unsupported tile pixel size: " + tilePixelSize);
			return false;
		}

		// get and check the projection name (VBE-U)
		this.projectionName = readUTF8EncodedString();
		if (!MERCATOR.equals(this.projectionName)) {
			Logger.debug("unsupported projection: " + this.projectionName);
			return false;
		}

		// get the language preference (VBE-U)
		this.languagePreference = readUTF8EncodedString();

		// get the meta-information byte that encodes multiple flags
		byte metaFlags = readByte();

		// extract the important flags from the meta-information byte
		this.debugFile = (metaFlags & HEADER_BITMASK_DEBUG) != 0;
		this.mapStartPosition = (metaFlags & HEADER_BITMASK_START_POSITION) != 0;

		// check if the header contains a map start position
		if (this.mapStartPosition) {
			// get and check the start position latitude (4 byte)
			this.startPositionLatitude = readInt();
			if (this.startPositionLatitude < LATITUDE_MIN || this.startPositionLatitude > LATITUDE_MAX) {
				Logger.debug("invalid map start position latitude: " + this.startPositionLatitude);
				return false;
			}

			// get and check the start position longitude (4 byte)
			this.startPositionLongitude = readInt();
			if (this.startPositionLongitude < LONGITUDE_MIN
					|| this.startPositionLongitude > LONGITUDE_MAX) {
				Logger.debug("invalid map start position longitude: " + this.startPositionLongitude);
				return false;
			}
		}

		// get and check the number of node tags (2 bytes)
		int numberOfNodeTags = readShort();
		if (numberOfNodeTags < 0) {
			Logger.debug("invalid number of node tags: " + numberOfNodeTags);
			return false;
		}

		this.nodeTags = new Tag[numberOfNodeTags];

		for (int tempInt = 0; tempInt < numberOfNodeTags; ++tempInt) {
			// get and check the node tag
			String tag = readUTF8EncodedString();
			if (tag == null) {
				Logger.debug("node tag must not be null: " + tempInt);
				return false;
			}

			this.nodeTags[tempInt] = new Tag(tag);
		}

		// get and check the number of way tags (2 bytes)
		int numberOfWayTags = readShort();
		if (numberOfWayTags < 0) {
			Logger.debug("invalid number of way tags: " + numberOfWayTags);
			return false;
		}

		this.wayTags = new Tag[numberOfWayTags];

		for (int tempInt = 0; tempInt < numberOfWayTags; ++tempInt) {
			// get and check the way tag
			String tag = readUTF8EncodedString();
			if (tag == null) {
				Logger.debug("way tag must not be null: " + tempInt);
				return false;
			}

			this.wayTags[tempInt] = new Tag(tag);
		}

		// get and check the number of contained map files
		byte numberOfMapFiles = readByte();
		if (numberOfMapFiles < 1) {
			Logger.debug("invalid number of contained map files: " + numberOfMapFiles);
			return false;
		}

		// create the list of all contained map files
		MapFileParameters[] mapFilesList = new MapFileParameters[numberOfMapFiles];
		this.globalMinimumZoomLevel = Byte.MAX_VALUE;
		this.globalMaximumZoomLevel = Byte.MIN_VALUE;

		// get and check the information for each contained map file
		for (byte tempByte = 0; tempByte < numberOfMapFiles; ++tempByte) {
			// get and check the base zoom level
			byte baseZoomLevel = readByte();
			if (baseZoomLevel < 0 || baseZoomLevel > 21) {
				Logger.debug("invalid base zooom level: " + baseZoomLevel);
				return false;
			}

			// get and check the minimum zoom level
			byte zoomLevelMin = readByte();
			if (zoomLevelMin < 0 || zoomLevelMin > 21) {
				Logger.debug("invalid minimum zoom level: " + zoomLevelMin);
				return false;
			}

			// get and check the maximum zoom level
			byte zoomLevelMax = readByte();
			if (zoomLevelMax < 0 || zoomLevelMax > 21) {
				Logger.debug("invalid maximum zoom level: " + zoomLevelMax);
				return false;
			}

			// check for valid zoom level range
			if (zoomLevelMin > zoomLevelMax) {
				Logger.debug("invalid zoom level range: " + zoomLevelMin + " - " + zoomLevelMax);
				return false;
			}

			// get and check the start address of the map file (5 bytes)
			long startAddress = readFiveBytesLong();
			if (startAddress < 1 || startAddress >= this.fileSize) {
				Logger.debug("invalid start address: " + startAddress);
				return false;
			}

			long indexStartAddress = startAddress;
			if (this.debugFile) {
				// the map file has an index signature before the index
				indexStartAddress += SIGNATURE_LENGTH_INDEX;
			}

			// get and check the size of the map file (5 bytes)
			long mapFileSize = readFiveBytesLong();
			if (mapFileSize < 1) {
				Logger.debug("invalid map file size: " + mapFileSize);
				return false;
			}

			// add the current map file to the map files list
			mapFilesList[tempByte] = new MapFileParameters(startAddress, indexStartAddress,
					mapFileSize, baseZoomLevel, zoomLevelMin, zoomLevelMax, this.mapBoundary);

			// update the global minimum and maximum zoom level information
			if (zoomLevelMin < this.globalMinimumZoomLevel) {
				this.globalMinimumZoomLevel = zoomLevelMin;
			}
			if (zoomLevelMax > this.globalMaximumZoomLevel) {
				this.globalMaximumZoomLevel = zoomLevelMax;
			}
		}

		// create and fill the lookup table for the map files
		this.mapFilesLookupTable = new MapFileParameters[this.globalMaximumZoomLevel + 1];
		for (int tempInt = 0; tempInt < numberOfMapFiles; ++tempInt) {
			MapFileParameters mapFileParameters = mapFilesList[tempInt];
			for (byte tempByte = mapFileParameters.zoomLevelMin; tempByte <= mapFileParameters.zoomLevelMax; ++tempByte) {
				this.mapFilesLookupTable[tempByte] = mapFileParameters;
			}
		}

		// get the comment text
		this.commentText = readUTF8EncodedString();

		return true;
	}

	/**
	 * Processes the given number of nodes.
	 * 
	 * @param mapDatabaseCallback
	 *            the callback which handles the extracted nodes.
	 * @param numberOfNodes
	 *            how many nodes should be processed.
	 * @return true if the nodes could be processed successfully, false otherwise.
	 * @throws UnsupportedEncodingException
	 *             if string decoding fails.
	 */
	private boolean processNodes(MapDatabaseCallback mapDatabaseCallback, int numberOfNodes)
			throws UnsupportedEncodingException {
		for (int elementCounter = numberOfNodes; elementCounter != 0; --elementCounter) {
			if (this.debugFile) {
				// get and check the node signature
				this.nodeSignature = new String(this.readBuffer, this.bufferPosition,
						SIGNATURE_LENGTH_NODE, CHARSET_UTF8);
				this.bufferPosition += SIGNATURE_LENGTH_NODE;
				if (!this.nodeSignature.startsWith("***POIStart")) {
					Logger.debug("invalid node signature: " + this.nodeSignature);
					Logger.debug(SIGNATURE_BLOCK + this.blockSignature);
					return false;
				}
			}

			// get the node latitude offset (VBE-S)
			int nodeLatitude = this.tileLatitude + readSignedInt();

			// get the node longitude offset (VBE-S)
			int nodeLongitude = this.tileLongitude + readSignedInt();

			// get the special byte that encodes multiple fields
			byte nodeSpecialByte = readByte();

			// bit 1-4 of the special byte represent the node layer
			byte nodeLayer = (byte) ((nodeSpecialByte & NODE_LAYER_BITMASK) >>> NODE_LAYER_SHIFT);
			// bit 5-8 of the special byte represent the number of tag IDs
			byte nodeNumberOfTags = (byte) (nodeSpecialByte & NODE_NUMBER_OF_TAGS_BITMASK);

			this.tagList.clear();

			// get the node tag IDs (VBE-U)
			for (byte tempByte = nodeNumberOfTags; tempByte != 0; --tempByte) {
				int tagId = readUnsignedInt();
				if (tagId < 0 || tagId >= this.nodeTags.length) {
					Logger.debug("invalid node tag ID: " + tagId);
					if (this.debugFile) {
						Logger.debug(SIGNATURE_NODE + this.nodeSignature);
						Logger.debug(SIGNATURE_BLOCK + this.blockSignature);
					}
					return false;
				}
				this.tagList.add(this.nodeTags[tagId]);
			}

			// get the feature byte
			byte nodeFeatureByte = readByte();

			// bit 1-3 of the node feature byte enable optional features
			boolean nodeFeatureName = (nodeFeatureByte & NODE_FEATURE_BITMASK_NAME) != 0;
			boolean nodeFeatureElevation = (nodeFeatureByte & NODE_FEATURE_BITMASK_ELEVATION) != 0;
			boolean nodeFeatureHouseNumber = (nodeFeatureByte & NODE_FEATURE_BITMASK_HOUSE_NUMBER) != 0;

			// check if the node has a name
			if (nodeFeatureName) {
				this.tagList.add(new Tag(TAG_KEY_NAME, readUTF8EncodedString()));
			}

			// check if the node has an elevation
			if (nodeFeatureElevation) {
				// get the node elevation (VBE-S)
				this.tagList.add(new Tag(TAG_KEY_ELE, Integer.toString(readSignedInt())));
			}

			// check if the node has a house number
			if (nodeFeatureHouseNumber) {
				this.tagList.add(new Tag(TAG_KEY_HOUSE_NUMBER, readUTF8EncodedString()));
			}

			mapDatabaseCallback.renderPointOfInterest(nodeLayer, nodeLatitude,
					nodeLongitude, this.tagList);
		}

		return true;
	}

	/**
	 * Processes the given number of ways.
	 * 
	 * @param mapDatabaseCallback
	 *            the callback which handles the extracted ways.
	 * @param numberOfWays
	 *            how many ways should be processed.
	 * @return true if the ways could be processed successfully, false otherwise.
	 * @throws UnsupportedEncodingException
	 *             if string decoding fails.
	 */
	private boolean processWays(MapDatabaseCallback mapDatabaseCallback, int numberOfWays)
			throws UnsupportedEncodingException {
		for (int elementCounter = numberOfWays; elementCounter != 0; --elementCounter) {
			if (this.debugFile) {
				// get and check the way signature
				this.waySignature = new String(this.readBuffer, this.bufferPosition,
						SIGNATURE_LENGTH_WAY, CHARSET_UTF8);
				this.bufferPosition += SIGNATURE_LENGTH_WAY;
				if (!this.waySignature.startsWith("---WayStart")) {
					Logger.debug("invalid way signature: " + this.waySignature);
					Logger.debug(SIGNATURE_BLOCK + this.blockSignature);
					return false;
				}
			}

			// get the size of the way (VBE-U)
			int waySize = readUnsignedInt();
			if (waySize < 0) {
				Logger.debug("invalid way size: " + waySize);
				if (this.debugFile) {
					Logger.debug(SIGNATURE_BLOCK + this.blockSignature);
				}
				return false;
			}

			if (this.useTileBitmask) {
				// get the way tile bitmask (2 bytes)
				this.wayTileBitmask = readShort();
				// check if the way is inside the requested tile
				if ((this.queryTileBitmask & this.wayTileBitmask) == 0) {
					// skip the rest of the way and continue with the next way
					this.bufferPosition += waySize - 2;
					continue;
				}
			} else {
				// ignore the way tile bitmask (2 bytes)
				this.bufferPosition += 2;
			}

			// get the first special byte that encodes multiple fields
			byte waySpecialByte1 = readByte();

			// bit 1-4 of the first special byte represent the way layer
			byte wayLayer = (byte) ((waySpecialByte1 & WAY_LAYER_BITMASK) >>> WAY_LAYER_SHIFT);
			// bit 5-8 of the first special byte represent the number of tag IDs
			byte wayNumberOfTags = (byte) (waySpecialByte1 & WAY_NUMBER_OF_TAGS_BITMASK);

			// skip the second special byte
			readByte();

			// skip the way tag bitmap
			readByte();

			this.tagList.clear();

			// get the way tag IDs (VBE-U)
			for (byte tempByte = wayNumberOfTags; tempByte != 0; --tempByte) {
				int tagId = readUnsignedInt();
				if (tagId < 0 || tagId >= this.wayTags.length) {
					Logger.debug("invalid way tag ID: " + tagId);
					logSignatures();
					return false;
				}
				this.tagList.add(this.wayTags[tagId]);
			}

			// get and check the number of way nodes (VBE-U)
			int wayNumberOfWayNodes = readUnsignedInt();
			if (wayNumberOfWayNodes < 1 || wayNumberOfWayNodes > MAXIMUM_WAY_NODES_SEQUENCE_LENGTH) {
				Logger.debug("invalid number of way nodes: " + wayNumberOfWayNodes);
				logSignatures();
				return false;
			}

			// each way node consists of latitude and longitude fields
			int wayNodesSequenceLength = wayNumberOfWayNodes * 2;

			float[] way = new float[wayNodesSequenceLength];

			// get the first way node latitude offset (VBE-S)
			int wayNodeLatitude = this.tileLatitude + readSignedInt();

			// get the first way node longitude offset (VBE-S)
			int wayNodeLongitude = this.tileLongitude + readSignedInt();

			// store the first way node
			way[1] = wayNodeLatitude;
			way[0] = wayNodeLongitude;

			final int firstWayNodeLatitude = wayNodeLatitude;
			final int firstWayNodeLongitude = wayNodeLongitude;

			// get the remaining way nodes offsets
			for (int tempInt = 2; tempInt < wayNodesSequenceLength; tempInt += 2) {
				// get the way node latitude offset (VBE-S)
				wayNodeLatitude = wayNodeLatitude + readSignedInt();

				// get the way node longitude offset (VBE-S)
				wayNodeLongitude = wayNodeLongitude + readSignedInt();

				way[tempInt] = wayNodeLongitude;
				way[tempInt + 1] = wayNodeLatitude;
			}

			// get the feature byte
			byte wayFeatureByte = readByte();

			// bit 1-4 of the way feature byte enable optional features
			boolean wayFeatureName = (wayFeatureByte & WAY_FEATURE_BITMASK_NAME) != 0;
			boolean wayFeatureRef = (wayFeatureByte & WAY_FEATURE_BITMASK_REF) != 0;
			boolean wayFeatureLabelPosition = (wayFeatureByte & WAY_FEATURE_BITMASK_LABEL_POSITION) != 0;
			boolean wayFeatureMultipolygon = (wayFeatureByte & WAY_FEATURE_BITMASK_MULTIPOLYGON) != 0;

			// check if the way has a name
			if (wayFeatureName) {
				this.tagList.add(new Tag(TAG_KEY_NAME, readUTF8EncodedString()));
			}

			// check if the way has a reference
			if (wayFeatureRef) {
				this.tagList.add(new Tag(TAG_KEY_REF, readUTF8EncodedString()));
			}

			// check if the way has a label position
			if (wayFeatureLabelPosition) {
				this.wayLabelPosition = new float[2];

				// get the label position latitude offset (VBE-S)
				this.wayLabelPosition[1] = firstWayNodeLatitude + readSignedInt();

				// get the label position longitude offset (VBE-S)
				this.wayLabelPosition[0] = firstWayNodeLongitude + readSignedInt();

			} else {
				// no label position
				this.wayLabelPosition = null;
			}

			// check if the way represents a multipolygon
			if (wayFeatureMultipolygon) {
				// get the amount of inner ways (VBE-U)
				this.wayNumberOfInnerWays = readUnsignedInt();

				if (this.wayNumberOfInnerWays > 0
						&& this.wayNumberOfInnerWays < MAXIMUM_NUMBER_OF_INNER_WAYS) {
					this.wayNodes = new float[1 + this.wayNumberOfInnerWays][];
					this.wayNodes[0] = way;

					// for each inner way
					for (this.innerWayNumber = 1; this.innerWayNumber <= this.wayNumberOfInnerWays; ++this.innerWayNumber) {
						// get and check the number of inner way nodes (VBE-U)
						this.innerWayNumberOfWayNodes = readUnsignedInt();
						if (this.innerWayNumberOfWayNodes < 1
								|| this.innerWayNumberOfWayNodes > MAXIMUM_WAY_NODES_SEQUENCE_LENGTH) {
							Logger.debug("invalid number of inner way nodes: "
									+ this.innerWayNumberOfWayNodes);
							logSignatures();
							return false;
						}

						// each inner way node consists of a latitude and a longitude field
						this.innerWayNodesSequenceLength = this.innerWayNumberOfWayNodes * 2;

						this.wayNodes[this.innerWayNumber] = new float[this.innerWayNodesSequenceLength];

						// get the first inner way node latitude (VBE-S)
						wayNodeLatitude = firstWayNodeLatitude + readSignedInt();

						// get the first inner way node longitude (VBE-S)
						wayNodeLongitude = firstWayNodeLongitude + readSignedInt();

						this.wayNodes[this.innerWayNumber][1] = wayNodeLatitude;
						this.wayNodes[this.innerWayNumber][0] = wayNodeLongitude;

						// get and store the remaining inner way nodes offsets
						for (int tempInt = 2; tempInt < this.innerWayNodesSequenceLength; tempInt += 2) {
							// get the inner way node latitude offset (VBE-S)
							wayNodeLatitude = wayNodeLatitude + readSignedInt();

							// get the inner way node longitude offset (VBE-S)
							wayNodeLongitude = wayNodeLongitude + readSignedInt();

							this.wayNodes[this.innerWayNumber][tempInt] = wayNodeLongitude;
							this.wayNodes[this.innerWayNumber][tempInt + 1] = wayNodeLatitude;
						}
					}
				} else {
					Logger.debug("invalid number of inner ways: " + this.wayNumberOfInnerWays);
					logSignatures();
					return false;
				}
			} else {
				// no multipolygon
				this.wayNodes = new float[][] { way };
			}

			mapDatabaseCallback.renderWay(wayLayer, this.wayLabelPosition, this.tagList,
					this.wayNodes);
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
	 * Converts two bytes from the read buffer to a signed int.
	 * <p>
	 * The byte order is big-endian.
	 * 
	 * @return the int value.
	 */
	private int readShort() {
		this.bufferPosition += 2;
		return Deserializer.getShort(this.readBuffer, this.bufferPosition - 2);
	}

	/**
	 * Converts a variable amount of bytes from the read buffer to a signed int.
	 * <p>
	 * The first bit is for continuation info, the other six (last byte) or seven (all other bytes) bits
	 * are for data. The second bit in the last byte indicates the sign of the number.
	 * 
	 * @return the int value.
	 */
	private int readSignedInt() {
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
	 * The first bit is for continuation info, the other seven bits are for data.
	 * 
	 * @return the int value or -1 in case of an error.
	 */
	private int readUnsignedInt() {
		try {
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
		} catch (ArrayIndexOutOfBoundsException e) {
			Logger.exception(e);
			return -1;
		}
	}

	/**
	 * Decodes a variable amount of bytes from the read buffer to a string.
	 * 
	 * @return the UTF-8 decoded string (may be null).
	 * @throws UnsupportedEncodingException
	 *             if string decoding fails.
	 */
	private String readUTF8EncodedString() throws UnsupportedEncodingException {
		// get and check the length of string (VBE-U)
		int stringLength = readUnsignedInt();
		if (stringLength >= 0 && this.bufferPosition + stringLength <= this.readBuffer.length) {
			this.bufferPosition += stringLength;

			// get the string
			return new String(this.readBuffer, this.bufferPosition - stringLength, stringLength,
					CHARSET_UTF8);
		}
		Logger.debug("invalid string length: " + stringLength);
		return null;
	}
}