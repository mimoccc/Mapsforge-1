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
	public static final int FILE_VERSION_MAX = 3;

	/**
	 * Minimal supported version of the map file format.
	 */
	public static final int FILE_VERSION_MIN = 3;

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
	 * Debug message prefix for the block signature.
	 */
	private static final String DEBUG_SIGNATURE_BLOCK = "block signature: ";

	/**
	 * Debug message prefix for the POI signature.
	 */
	private static final String DEBUG_SIGNATURE_POI = "POI signature: ";

	/**
	 * Debug message prefix for the way signature.
	 */
	private static final String DEBUG_SIGNATURE_WAY = "way signature: ";

	/**
	 * Bitmask for the debug flag in the file header.
	 */
	private static final int HEADER_BITMASK_DEBUG = 0x80;

	/**
	 * Bitmask for the start position in the file header.
	 */
	private static final int HEADER_BITMASK_START_POSITION = 0x40;

	/**
	 * Maximum size of the file header in bytes.
	 */
	private static final int HEADER_SIZE_MAX = 1000000;

	/**
	 * Minimum size of the file header in bytes.
	 */
	private static final int HEADER_SIZE_MIN = 70;

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
	private static final int MAXIMUM_BLOCK_SIZE = 2000000;

	/**
	 * Maximum way nodes sequence length which is considered as valid.
	 */
	private static final int MAXIMUM_WAY_NODES_SEQUENCE_LENGTH = 8192;

	/**
	 * The name of the Mercator projection as stored in the file header.
	 */
	private static final String MERCATOR = "Mercator";

	/**
	 * Bitmask for the optional POI feature "elevation".
	 */
	private static final int POI_FEATURE_BITMASK_ELEVATION = 0x40;

	/**
	 * Bitmask for the optional POI feature "house number".
	 */
	private static final int POI_FEATURE_BITMASK_HOUSE_NUMBER = 0x20;

	/**
	 * Bitmask for the optional POI feature "name".
	 */
	private static final int POI_FEATURE_BITMASK_NAME = 0x80;

	/**
	 * Bitmask for the POI layer.
	 */
	private static final int POI_LAYER_BITMASK = 0xf0;

	/**
	 * Bit shift for calculating the POI layer.
	 */
	private static final int POI_LAYER_SHIFT = 4;

	/**
	 * Bitmask for the number of POI tags.
	 */
	private static final int POI_NUMBER_OF_TAGS_BITMASK = 0x0f;

	/**
	 * Length of the debug signature at the beginning of each block.
	 */
	private static final byte SIGNATURE_LENGTH_BLOCK = 32;

	/**
	 * Length of the debug signature at the beginning of the index.
	 */
	private static final byte SIGNATURE_LENGTH_INDEX = 16;

	/**
	 * Length of the debug signature at the beginning of each POI.
	 */
	private static final byte SIGNATURE_LENGTH_POI = 32;

	/**
	 * Length of the debug signature at the beginning of each way.
	 */
	private static final byte SIGNATURE_LENGTH_WAY = 32;

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
	private RandomAccessFile inputFile;
	private String languagePreference;
	private Rect mapBoundary;
	private long mapDate;
	private MapFileParameter[] mapFilesLookupTable;
	private int mapStartLatitude;
	private int mapStartLongitude;
	private boolean mapStartPosition;
	private long nextBlockPointer;
	private long parentTileX;
	private long parentTileY;
	private Tag[] poiTags;
	private String projectionName;
	private int queryTileBitmask;
	private int queryZoomLevel;
	private byte[] readBuffer;
	private String signatureBlock;
	private String signaturePoi;
	private String signatureWay;
	private long subtileX;
	private long subtileY;
	private int tileLatitude;
	private int tileLongitude;
	private long toBaseTileX;
	private long toBaseTileY;
	private boolean useTileBitmask;
	private Tag[] wayTags;
	private int zoomLevelDifference;

	/**
	 * Closes the map file and destroys all internal caches. This method has no effect if no map file is
	 * currently opened.
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
			MapFileParameter mapFileParameter = this.mapFilesLookupTable[this.queryZoomLevel];
			if (mapFileParameter == null) {
				Logger.debug("no map file for zoom level: " + tile.zoomLevel);
				return;
			}

			// calculate the blocks that cover the area of the requested tile
			if (tile.zoomLevel < mapFileParameter.baseZoomLevel) {
				// calculate the XY numbers of the upper left and lower right subtiles
				this.zoomLevelDifference = mapFileParameter.baseZoomLevel - tile.zoomLevel;
				this.fromBaseTileX = tile.x << this.zoomLevelDifference;
				this.fromBaseTileY = tile.y << this.zoomLevelDifference;
				this.toBaseTileX = this.fromBaseTileX + (1 << this.zoomLevelDifference) - 1;
				this.toBaseTileY = this.fromBaseTileY + (1 << this.zoomLevelDifference) - 1;
				this.useTileBitmask = false;
			} else if (tile.zoomLevel > mapFileParameter.baseZoomLevel) {
				// calculate the XY numbers of the parent base tile
				this.zoomLevelDifference = tile.zoomLevel - mapFileParameter.baseZoomLevel;
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
			long fromBlockX = Math.max(this.fromBaseTileX - mapFileParameter.boundaryLeftTile, 0);
			long fromBlockY = Math.max(this.fromBaseTileY - mapFileParameter.boundaryTopTile, 0);
			long toBlockX = Math.min(this.toBaseTileX - mapFileParameter.boundaryLeftTile,
					mapFileParameter.blocksWidth - 1);
			long toBlockY = Math.min(this.toBaseTileY - mapFileParameter.boundaryTopTile,
					mapFileParameter.blocksHeight - 1);

			boolean queryIsWater = true;
			boolean queryReadWaterInfo = false;

			// read and process all necessary blocks from top to bottom and from left to right
			for (long currentRow = fromBlockY; currentRow <= toBlockY; ++currentRow) {
				for (long currentColumn = fromBlockX; currentColumn <= toBlockX; ++currentColumn) {
					// calculate the actual block number of the needed block in the file
					long blockNumber = currentRow * mapFileParameter.blocksWidth + currentColumn;

					// get the current index entry
					long currentBlockIndexEntry = this.databaseIndexCache.getIndexEntry(
							mapFileParameter, blockNumber);

					// check if the current query would still return a water tile
					if (queryIsWater) {
						// check the water flag of the current block
						boolean currentBlockIsWater = (currentBlockIndexEntry & BITMASK_INDEX_WATER) != 0;
						queryIsWater = queryIsWater && currentBlockIsWater;
						queryReadWaterInfo = true;
					}

					// get and check the current block pointer
					long currentBlockPointer = currentBlockIndexEntry & BITMASK_INDEX_OFFSET;
					if (currentBlockPointer < 1 || currentBlockPointer > mapFileParameter.mapFileSize) {
						Logger.debug("invalid current block pointer: " + currentBlockPointer);
						Logger.debug("mapFileSize: " + mapFileParameter.mapFileSize);
						return;
					}

					// check if the current block is the last block in the file
					if (blockNumber + 1 == mapFileParameter.numberOfBlocks) {
						// set the next block pointer to the end of the file
						this.nextBlockPointer = mapFileParameter.mapFileSize;
					} else {
						// get and check the next block pointer
						this.nextBlockPointer = this.databaseIndexCache.getIndexEntry(
								mapFileParameter, blockNumber + 1) & BITMASK_INDEX_OFFSET;
						if (this.nextBlockPointer < 1
								|| this.nextBlockPointer > mapFileParameter.mapFileSize) {
							Logger.debug("invalid next block pointer: " + this.nextBlockPointer);
							Logger.debug("mapFileSize: " + mapFileParameter.mapFileSize);
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
					this.inputFile.seek(mapFileParameter.startAddress + currentBlockPointer);

					// read the current block into the buffer
					if (!readFromMapFile(currentBlockSize)) {
						// skip the current block
						Logger.debug("reading current block has failed: " + currentBlockSize);
						return;
					}

					// calculate the top-left coordinates of the underlying tile
					this.tileLatitude = (int) (MercatorProjection.tileYToLatitude(
							mapFileParameter.boundaryTopTile + currentRow,
							mapFileParameter.baseZoomLevel) * 1000000);
					this.tileLongitude = (int) (MercatorProjection.tileXToLongitude(
							mapFileParameter.boundaryLeftTile + currentColumn,
							mapFileParameter.baseZoomLevel) * 1000000);

					try {
						processBlock(mapFileParameter, mapDatabaseCallback);
					} catch (ArrayIndexOutOfBoundsException e) {
						Logger.exception(e);
					}
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
	 * Returns the map start position from the file header (may be null).
	 * 
	 * @return the map start position from the file header (may be null).
	 */
	public GeoPoint getStartPosition() {
		if (this.mapStartPosition) {
			return new GeoPoint(this.mapStartLatitude, this.mapStartLongitude);
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
	 * Logs the debug signatures of the current way and block.
	 */
	private void logDebugSignatures() {
		if (this.debugFile) {
			Logger.debug(DEBUG_SIGNATURE_WAY + this.signatureWay);
			Logger.debug(DEBUG_SIGNATURE_BLOCK + this.signatureBlock);
		}
	}

	/**
	 * Prepares and sets up the internal data structures and caches.
	 */
	private void prepareExecution() {
		if (this.databaseIndexCache == null) {
			this.databaseIndexCache = new IndexCache(this.inputFile, INDEX_CACHE_SIZE);
		}
	}

	/**
	 * Processes a single block and executes the callback functions on all map elements.
	 * 
	 * @param mapFileParameter
	 *            the parameters of the current map file.
	 * @param mapDatabaseCallback
	 *            the callback which handles the extracted map elements.
	 * @throws UnsupportedEncodingException
	 *             if string decoding fails.
	 */
	private void processBlock(MapFileParameter mapFileParameter,
			MapDatabaseCallback mapDatabaseCallback) throws UnsupportedEncodingException {
		if (!processBlockSignature()) {
			return;
		}

		// calculate the offset in the block entries table and move the pointer
		int blockEntriesTableOffset = (this.queryZoomLevel - mapFileParameter.zoomLevelMin) * 4;
		this.bufferPosition += blockEntriesTableOffset;

		// get the amount of POIs and ways on the current zoomLevel level
		int poisOnZoomLevel = readShort();
		int waysOnZoomLevel = readShort();

		// move the pointer to the end of the block entries table
		this.bufferPosition += mapFileParameter.blockEntriesTableSize - blockEntriesTableOffset - 4;

		// get the relative offset to the first stored way in the block
		int firstWayOffset = readUnsignedInt();
		if (firstWayOffset < 0) {
			Logger.debug("invalid first way offset: " + firstWayOffset);
			if (this.debugFile) {
				Logger.debug(DEBUG_SIGNATURE_BLOCK + this.signatureBlock);
			}
			return;
		}

		// add the current buffer position to the relative first way offset
		firstWayOffset += this.bufferPosition;
		if (firstWayOffset > this.readBuffer.length) {
			Logger.debug("invalid first way offset: " + firstWayOffset);
			if (this.debugFile) {
				Logger.debug(DEBUG_SIGNATURE_BLOCK + this.signatureBlock);
			}
			return;
		}

		if (!processPOIs(mapDatabaseCallback, poisOnZoomLevel)) {
			return;
		}

		// finished reading POIs, check if the current buffer position is valid
		if (this.bufferPosition > firstWayOffset) {
			Logger.debug("invalid buffer position: " + this.bufferPosition + " - " + firstWayOffset);
			if (this.debugFile) {
				Logger.debug(DEBUG_SIGNATURE_BLOCK + this.signatureBlock);
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
			this.signatureBlock = new String(this.readBuffer, this.bufferPosition,
					SIGNATURE_LENGTH_BLOCK, CHARSET_UTF8);
			this.bufferPosition += SIGNATURE_LENGTH_BLOCK;
			if (!this.signatureBlock.startsWith("###TileStart")) {
				Logger.debug("invalid block signature: " + this.signatureBlock);
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
		int magicByteLength = BINARY_OSM_MAGIC_BYTE.length();
		if (!readFromMapFile(magicByteLength + 4)) {
			Logger.debug("reading magic byte has failed");
			return false;
		}

		// get and check the magic byte
		String magicByte = new String(this.readBuffer, this.bufferPosition, magicByteLength,
				CHARSET_UTF8);
		this.bufferPosition += magicByteLength;
		if (!BINARY_OSM_MAGIC_BYTE.equals(magicByte)) {
			Logger.debug("invalid magic byte: " + magicByte);
			return false;
		}

		// get and check the size of the remaining file header (4 bytes)
		int remainingHeaderSize = readInt();
		if (remainingHeaderSize < HEADER_SIZE_MIN || remainingHeaderSize > HEADER_SIZE_MAX) {
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
		if (this.fileVersion < FILE_VERSION_MIN || this.fileVersion > FILE_VERSION_MAX) {
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
		// is the map date before 2010-01-10 ?
		if (this.mapDate < 1200000000000L) {
			Logger.debug("invalid map date: " + this.mapDate);
			return false;
		}

		// get and check the minimum latitude (4 bytes)
		int latitudeMin = readInt();
		if (latitudeMin < LATITUDE_MIN || latitudeMin > LATITUDE_MAX) {
			Logger.debug("invalid minimum latitude: " + latitudeMin);
			return false;
		}

		// get and check the minimum longitude (4 bytes)
		int longitudeMin = readInt();
		if (longitudeMin < LONGITUDE_MIN || longitudeMin > LONGITUDE_MAX) {
			Logger.debug("invalid minimum longitude: " + longitudeMin);
			return false;
		}

		// get and check the maximum latitude (4 bytes)
		int latitudeMax = readInt();
		if (latitudeMax < LATITUDE_MIN || latitudeMax > LATITUDE_MAX) {
			Logger.debug("invalid maximum latitude: " + latitudeMax);
			return false;
		}

		// get and check the maximum longitude (4 bytes)
		int longitudeMax = readInt();
		if (longitudeMax < LONGITUDE_MIN || longitudeMax > LONGITUDE_MAX) {
			Logger.debug("invalid maximum longitude: " + longitudeMax);
			return false;
		}

		// check latitude and longitude range
		if (latitudeMin > latitudeMax) {
			Logger.debug("invalid latitude range: " + latitudeMin + " > " + latitudeMax);
			return false;
		} else if (longitudeMin > longitudeMax) {
			Logger.debug("invalid longitude range: " + longitudeMin + " > " + longitudeMax);
			return false;
		}

		// create the map boundary rectangle
		this.mapBoundary = new Rect(longitudeMin, latitudeMax, longitudeMax, latitudeMin);

		// get and check the tile pixel size (2 bytes)
		int tilePixelSize = readShort();
		if (tilePixelSize != Tile.TILE_SIZE) {
			Logger.debug("unsupported tile pixel size: " + tilePixelSize);
			return false;
		}

		// get and check the projection name
		this.projectionName = readUTF8EncodedString();
		if (!MERCATOR.equals(this.projectionName)) {
			Logger.debug("unsupported projection: " + this.projectionName);
			return false;
		}

		// get the language preference
		this.languagePreference = readUTF8EncodedString();

		// get the meta-information byte which encodes multiple flags
		byte metaFlags = readByte();

		// extract the important flags from the meta-information byte
		this.debugFile = (metaFlags & HEADER_BITMASK_DEBUG) != 0;
		this.mapStartPosition = (metaFlags & HEADER_BITMASK_START_POSITION) != 0;

		// check if the header contains a map start position
		if (this.mapStartPosition) {
			// get and check the start position latitude (4 byte)
			this.mapStartLatitude = readInt();
			if (this.mapStartLatitude < LATITUDE_MIN || this.mapStartLatitude > LATITUDE_MAX) {
				Logger.debug("invalid map start latitude: " + this.mapStartLatitude);
				return false;
			}

			// get and check the start position longitude (4 byte)
			this.mapStartLongitude = readInt();
			if (this.mapStartLongitude < LONGITUDE_MIN || this.mapStartLongitude > LONGITUDE_MAX) {
				Logger.debug("invalid map start longitude: " + this.mapStartLongitude);
				return false;
			}
		}

		// get and check the number of POI tags (2 bytes)
		int numberOfPoiTags = readShort();
		if (numberOfPoiTags < 0) {
			Logger.debug("invalid number of POI tags: " + numberOfPoiTags);
			return false;
		}

		this.poiTags = new Tag[numberOfPoiTags];

		for (int currentTagId = 0; currentTagId < numberOfPoiTags; ++currentTagId) {
			// get and check the POI tag
			String tag = readUTF8EncodedString();
			if (tag == null) {
				Logger.debug("POI tag must not be null: " + currentTagId);
				return false;
			}

			this.poiTags[currentTagId] = new Tag(tag);
		}

		// get and check the number of way tags (2 bytes)
		int numberOfWayTags = readShort();
		if (numberOfWayTags < 0) {
			Logger.debug("invalid number of way tags: " + numberOfWayTags);
			return false;
		}

		this.wayTags = new Tag[numberOfWayTags];

		for (int currentTagId = 0; currentTagId < numberOfWayTags; ++currentTagId) {
			// get and check the way tag
			String tag = readUTF8EncodedString();
			if (tag == null) {
				Logger.debug("way tag must not be null: " + currentTagId);
				return false;
			}

			this.wayTags[currentTagId] = new Tag(tag);
		}

		// get and check the number of contained map files (1 byte)
		byte numberOfMapFiles = readByte();
		if (numberOfMapFiles < 1) {
			Logger.debug("invalid number of contained map files: " + numberOfMapFiles);
			return false;
		}

		MapFileParameter[] mapFileParameters = new MapFileParameter[numberOfMapFiles];
		this.globalMinimumZoomLevel = Byte.MAX_VALUE;
		this.globalMaximumZoomLevel = Byte.MIN_VALUE;

		// get and check the information for each contained map file
		for (byte currentMapFile = 0; currentMapFile < numberOfMapFiles; ++currentMapFile) {
			// get and check the base zoom level (1 byte)
			byte baseZoomLevel = readByte();
			if (baseZoomLevel < 0 || baseZoomLevel > 20) {
				Logger.debug("invalid base zooom level: " + baseZoomLevel);
				return false;
			}

			// get and check the minimum zoom level (1 byte)
			byte zoomLevelMin = readByte();
			if (zoomLevelMin < 0 || zoomLevelMin > 22) {
				Logger.debug("invalid minimum zoom level: " + zoomLevelMin);
				return false;
			}

			// get and check the maximum zoom level (1 byte)
			byte zoomLevelMax = readByte();
			if (zoomLevelMax < 0 || zoomLevelMax > 22) {
				Logger.debug("invalid maximum zoom level: " + zoomLevelMax);
				return false;
			}

			// check for valid zoom level range
			if (zoomLevelMin > zoomLevelMax) {
				Logger.debug("invalid zoom level range: " + zoomLevelMin + " > " + zoomLevelMax);
				return false;
			}

			// get and check the start address of the map file (8 bytes)
			long startAddress = readLong();
			if (startAddress < HEADER_SIZE_MIN || startAddress >= this.fileSize) {
				Logger.debug("invalid start address: " + startAddress);
				return false;
			}

			long indexStartAddress = startAddress;
			if (this.debugFile) {
				// the map file has an index signature before the index
				indexStartAddress += SIGNATURE_LENGTH_INDEX;
			}

			// get and check the size of the map file (8 bytes)
			long mapFileSize = readLong();
			if (mapFileSize < 1) {
				Logger.debug("invalid map file size: " + mapFileSize);
				return false;
			}

			// add the current map file to the map files list
			mapFileParameters[currentMapFile] = new MapFileParameter(startAddress, indexStartAddress,
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
		this.mapFilesLookupTable = new MapFileParameter[this.globalMaximumZoomLevel + 1];
		for (int currentMapFile = 0; currentMapFile < numberOfMapFiles; ++currentMapFile) {
			MapFileParameter mapFileParameter = mapFileParameters[currentMapFile];
			for (byte zoomLevel = mapFileParameter.zoomLevelMin; zoomLevel <= mapFileParameter.zoomLevelMax; ++zoomLevel) {
				this.mapFilesLookupTable[zoomLevel] = mapFileParameter;
			}
		}

		// get the comment text
		this.commentText = readUTF8EncodedString();

		return true;
	}

	/**
	 * Processes the given number of POIs.
	 * 
	 * @param mapDatabaseCallback
	 *            the callback which handles the extracted POIs.
	 * @param numberOfPois
	 *            how many POIs should be processed.
	 * @return true if the POIs could be processed successfully, false otherwise.
	 * @throws UnsupportedEncodingException
	 *             if string decoding fails.
	 */
	private boolean processPOIs(MapDatabaseCallback mapDatabaseCallback, int numberOfPois)
			throws UnsupportedEncodingException {
		List<Tag> tags = new ArrayList<Tag>();

		for (int elementCounter = numberOfPois; elementCounter != 0; --elementCounter) {
			if (this.debugFile) {
				// get and check the POI signature
				this.signaturePoi = new String(this.readBuffer, this.bufferPosition,
						SIGNATURE_LENGTH_POI, CHARSET_UTF8);
				this.bufferPosition += SIGNATURE_LENGTH_POI;
				if (!this.signaturePoi.startsWith("***POIStart")) {
					Logger.debug("invalid POI signature: " + this.signaturePoi);
					Logger.debug(DEBUG_SIGNATURE_BLOCK + this.signatureBlock);
					return false;
				}
			}

			// get the POI latitude offset (VBE-S)
			int latitude = this.tileLatitude + readSignedInt();

			// get the POI longitude offset (VBE-S)
			int longitude = this.tileLongitude + readSignedInt();

			// get the special byte which encodes multiple flags
			byte specialByte = readByte();

			// bit 1-4 represent the layer
			byte layer = (byte) ((specialByte & POI_LAYER_BITMASK) >>> POI_LAYER_SHIFT);
			// bit 5-8 represent the number of tag IDs
			byte numberOfTags = (byte) (specialByte & POI_NUMBER_OF_TAGS_BITMASK);

			tags.clear();

			// get the tag IDs (VBE-U)
			for (byte tagIndex = numberOfTags; tagIndex != 0; --tagIndex) {
				int tagId = readUnsignedInt();
				if (tagId < 0 || tagId >= this.poiTags.length) {
					Logger.debug("invalid POI tag ID: " + tagId);
					if (this.debugFile) {
						Logger.debug(DEBUG_SIGNATURE_POI + this.signaturePoi);
						Logger.debug(DEBUG_SIGNATURE_BLOCK + this.signatureBlock);
					}
					return false;
				}
				tags.add(this.poiTags[tagId]);
			}

			// get the feature bitmask (1 byte)
			byte featureByte = readByte();

			// bit 1-3 enable optional features
			boolean featureName = (featureByte & POI_FEATURE_BITMASK_NAME) != 0;
			boolean featureElevation = (featureByte & POI_FEATURE_BITMASK_ELEVATION) != 0;
			boolean featureHouseNumber = (featureByte & POI_FEATURE_BITMASK_HOUSE_NUMBER) != 0;

			// check if the POI has a name
			if (featureName) {
				tags.add(new Tag(TAG_KEY_NAME, readUTF8EncodedString()));
			}

			// check if the POI has an elevation
			if (featureElevation) {
				tags.add(new Tag(TAG_KEY_ELE, Integer.toString(readSignedInt())));
			}

			// check if the POI has a house number
			if (featureHouseNumber) {
				tags.add(new Tag(TAG_KEY_HOUSE_NUMBER, readUTF8EncodedString()));
			}

			mapDatabaseCallback.renderPointOfInterest(layer, latitude, longitude, tags);
		}

		return true;
	}

	private float[][] processWayDataBlock() {
		// get and check the number of coordinate blocks (1 byte)
		byte numberOfCoordinateBlocks = readByte();
		if (numberOfCoordinateBlocks < 1) {
			Logger.debug("invalid number of coordinate blocks: " + numberOfCoordinateBlocks);
			logDebugSignatures();
			return null;
		}

		// create the array which will store the different way coordinate blocks
		float[][] wayCoordinates = new float[numberOfCoordinateBlocks][];

		// read the way coordinate blocks
		for (byte coordinateBlock = 0; coordinateBlock < numberOfCoordinateBlocks; ++coordinateBlock) {
			// get and check the number of way nodes (VBE-U)
			int numberOfWayNodes = readUnsignedInt();
			if (numberOfWayNodes < 2 || numberOfWayNodes > MAXIMUM_WAY_NODES_SEQUENCE_LENGTH) {
				Logger.debug("invalid number of way nodes: " + numberOfWayNodes);
				logDebugSignatures();
				return null;
			}

			// each way node consists of latitude and longitude
			int wayNodesSequenceLength = numberOfWayNodes * 2;

			// create the array which will store the current way segment
			float[] waySegment = new float[wayNodesSequenceLength];

			// get the first way node latitude offset (VBE-S)
			int wayNodeLatitude = this.tileLatitude + readSignedInt();

			// get the first way node longitude offset (VBE-S)
			int wayNodeLongitude = this.tileLongitude + readSignedInt();

			// store the first way node
			waySegment[1] = wayNodeLatitude;
			waySegment[0] = wayNodeLongitude;

			// get the remaining way nodes offsets
			for (int wayNodesIndex = 2; wayNodesIndex < wayNodesSequenceLength; wayNodesIndex += 2) {
				// get the way node latitude offset (VBE-S)
				wayNodeLatitude = wayNodeLatitude + readSignedInt();

				// get the way node longitude offset (VBE-S)
				wayNodeLongitude = wayNodeLongitude + readSignedInt();

				waySegment[wayNodesIndex + 1] = wayNodeLatitude;
				waySegment[wayNodesIndex] = wayNodeLongitude;
			}

			wayCoordinates[coordinateBlock] = waySegment;
		}

		return wayCoordinates;
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
		List<Tag> tags = new ArrayList<Tag>();

		for (int elementCounter = numberOfWays; elementCounter != 0; --elementCounter) {
			if (this.debugFile) {
				// get and check the way signature
				this.signatureWay = new String(this.readBuffer, this.bufferPosition,
						SIGNATURE_LENGTH_WAY, CHARSET_UTF8);
				this.bufferPosition += SIGNATURE_LENGTH_WAY;
				if (!this.signatureWay.startsWith("---WayStart")) {
					Logger.debug("invalid way signature: " + this.signatureWay);
					Logger.debug(DEBUG_SIGNATURE_BLOCK + this.signatureBlock);
					return false;
				}
			}

			// get the size of the way (VBE-U)
			int wayDataSize = readUnsignedInt();
			if (wayDataSize < 0) {
				Logger.debug("invalid way data size: " + wayDataSize);
				if (this.debugFile) {
					Logger.debug(DEBUG_SIGNATURE_BLOCK + this.signatureBlock);
				}
				return false;
			}

			if (this.useTileBitmask) {
				// get the way tile bitmask (2 bytes)
				int tileBitmask = readShort();
				// check if the way is inside the requested tile
				if ((this.queryTileBitmask & tileBitmask) == 0) {
					// skip the rest of the way and continue with the next way
					this.bufferPosition += wayDataSize - 2;
					continue;
				}
			} else {
				// ignore the way tile bitmask (2 bytes)
				this.bufferPosition += 2;
			}

			// get the special byte which encodes multiple flags
			byte specialByte = readByte();

			// bit 1-4 represent the layer
			byte layer = (byte) ((specialByte & WAY_LAYER_BITMASK) >>> WAY_LAYER_SHIFT);
			// bit 5-8 represent the number of tag IDs
			byte numberOfTags = (byte) (specialByte & WAY_NUMBER_OF_TAGS_BITMASK);

			tags.clear();

			// get the tag IDs (VBE-U)
			for (byte tagIndex = numberOfTags; tagIndex != 0; --tagIndex) {
				int tagId = readUnsignedInt();
				if (tagId < 0 || tagId >= this.wayTags.length) {
					Logger.debug("invalid way tag ID: " + tagId);
					logDebugSignatures();
					return false;
				}
				tags.add(this.wayTags[tagId]);
			}

			// get the feature bitmask (1 byte)
			byte featureByte = readByte();

			// bit 1-3 enable optional features
			boolean featureName = (featureByte & WAY_FEATURE_BITMASK_NAME) != 0;
			boolean featureRef = (featureByte & WAY_FEATURE_BITMASK_REF) != 0;
			boolean featureLabelPosition = (featureByte & WAY_FEATURE_BITMASK_LABEL_POSITION) != 0;

			// check if the way has a name
			if (featureName) {
				tags.add(new Tag(TAG_KEY_NAME, readUTF8EncodedString()));
			}

			// check if the way has a reference
			if (featureRef) {
				tags.add(new Tag(TAG_KEY_REF, readUTF8EncodedString()));
			}

			// check if the way has a label position
			float[] labelPosition;
			if (featureLabelPosition) {
				labelPosition = new float[2];

				// get the label position latitude offset (VBE-S)
				labelPosition[1] = this.tileLatitude + readSignedInt();

				// get the label position longitude offset (VBE-S)
				labelPosition[0] = this.tileLongitude + readSignedInt();
			} else {
				// no label position
				labelPosition = null;
			}

			// get and check the number of way data blocks (1 byte)
			byte wayDataBlocks = readByte();
			if (wayDataBlocks < 1) {
				Logger.debug("invalid number of way data blocks: " + wayDataBlocks);
				logDebugSignatures();
				return false;
			}

			for (byte wayDataBlock = 0; wayDataBlock < wayDataBlocks; ++wayDataBlock) {
				float[][] wayNodes = processWayDataBlock();
				if (wayNodes == null) {
					return false;
				}
				mapDatabaseCallback.renderWay(layer, labelPosition, tags, wayNodes);
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
				Logger.debug("invalid read length: " + length);
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
		int variableByteDecode = 0;
		byte variableByteShift = 0;

		// check if the continuation bit is set
		while ((this.readBuffer[this.bufferPosition] & 0x80) != 0) {
			variableByteDecode |= (this.readBuffer[this.bufferPosition++] & 0x7f) << variableByteShift;
			variableByteShift += 7;
		}

		// read the six data bits from the last byte
		if ((this.readBuffer[this.bufferPosition] & 0x40) != 0) {
			// negative
			return -(variableByteDecode | ((this.readBuffer[this.bufferPosition++] & 0x3f) << variableByteShift));
		}
		// positive
		return variableByteDecode
				| ((this.readBuffer[this.bufferPosition++] & 0x3f) << variableByteShift);
	}

	/**
	 * Converts a variable amount of bytes from the read buffer to an unsigned int.
	 * <p>
	 * The first bit is for continuation info, the other seven bits are for data.
	 * 
	 * @return the int value or -1 in case of an error.
	 */
	private int readUnsignedInt() {
		int variableByteDecode = 0;
		byte variableByteShift = 0;

		// check if the continuation bit is set
		while ((this.readBuffer[this.bufferPosition] & 0x80) != 0) {
			variableByteDecode |= (this.readBuffer[this.bufferPosition++] & 0x7f) << variableByteShift;
			variableByteShift += 7;
		}

		// read the seven data bits from the last byte
		return variableByteDecode | (this.readBuffer[this.bufferPosition++] << variableByteShift);
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