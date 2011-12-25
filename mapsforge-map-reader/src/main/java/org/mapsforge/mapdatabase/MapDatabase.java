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
package org.mapsforge.mapdatabase;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mapsforge.core.MercatorProjection;
import org.mapsforge.core.Tag;
import org.mapsforge.core.Tile;

/**
 * A class for reading binary map files.
 * <p>
 * This class is not thread-safe. Each thread should use its own instance.
 * 
 * @see <a href="http://code.google.com/p/mapsforge/wiki/SpecificationBinaryMapFile">Specification</a>
 */
public class MapDatabase {
	/**
	 * Bitmask to extract the block offset from an index entry.
	 */
	private static final long BITMASK_INDEX_OFFSET = 0x7FFFFFFFFFL;

	/**
	 * Bitmask to extract the water information from an index entry.
	 */
	private static final long BITMASK_INDEX_WATER = 0x8000000000L;

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
	 * Amount of cache blocks that the index cache should store.
	 */
	private static final int INDEX_CACHE_SIZE = 64;

	/**
	 * Error message for an invalid first way offset.
	 */
	private static final String INVALID_FIRST_WAY_OFFSET = "invalid first way offset: ";

	private static final Logger LOG = Logger.getLogger(MapDatabase.class.getName());

	/**
	 * Maximum way nodes sequence length which is considered as valid.
	 */
	private static final int MAXIMUM_WAY_NODES_SEQUENCE_LENGTH = 8192;

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

	private IndexCache databaseIndexCache;
	private boolean debugFile;
	private long fileSize;
	private RandomAccessFile inputFile;
	private MapFileHeader mapFileHeader;
	private ReadBuffer readBuffer;
	private String signatureBlock;
	private String signaturePoi;
	private String signatureWay;
	private int tileLatitude;
	private int tileLongitude;

	/**
	 * Closes the map file and destroys all internal caches. This method has no effect if no map file is
	 * currently opened.
	 */
	public void closeFile() {
		try {
			this.mapFileHeader = null;

			if (this.databaseIndexCache != null) {
				this.databaseIndexCache.destroy();
			}

			if (this.inputFile != null) {
				this.inputFile.close();
				this.inputFile = null;
			}

			this.readBuffer = null;
		} catch (IOException e) {
			LOG.log(Level.SEVERE, null, e);
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
			QueryParameters queryParameters = new QueryParameters();
			queryParameters.queryZoomLevel = this.mapFileHeader.getQueryZoomLevel(tile.zoomLevel);

			// get and check the sub-file for the query zoom level
			SubFileParameter subFileParameter = this.mapFileHeader
					.getSubFileParameter(queryParameters.queryZoomLevel);
			if (subFileParameter == null) {
				LOG.warning("no sub-file for zoom level: " + queryParameters.queryZoomLevel);
				return;
			}

			QueryCalculations.calculateBaseTiles(queryParameters, tile, subFileParameter);
			QueryCalculations.calculateBlocks(queryParameters, subFileParameter);

			processBlocks(mapDatabaseCallback, queryParameters, subFileParameter);
		} catch (IOException e) {
			LOG.log(Level.SEVERE, null, e);
		}
	}

	/**
	 * @return the metadata for the current map file.
	 * @throws IllegalStateException
	 *             if no map is currently opened.
	 */
	public MapFileInfo getMapFileInfo() {
		if (this.mapFileHeader == null) {
			throw new IllegalStateException("no map file is currently opened");
		}
		return this.mapFileHeader.getMapFileInfo();
	}

	/**
	 * @return true if a map file is currently opened, false otherwise.
	 */
	public boolean hasOpenFile() {
		return this.inputFile != null;
	}

	/**
	 * Opens the given map file, reads its header data and validates them.
	 * 
	 * @param fileName
	 *            the path to the map file.
	 * @return a FileOpenResult containing an error message in case of a failure.
	 * @throws IllegalArgumentException
	 *             if the given file name is null.
	 */
	public FileOpenResult openFile(String fileName) {
		try {
			if (fileName == null) {
				throw new IllegalArgumentException("fileName must not be null");
			}

			// make sure to close any previously opened file first
			closeFile();

			// check if the file exists and is readable
			File file = new File(fileName);
			if (!file.exists()) {
				return new FileOpenResult("file does not exist: " + fileName);
			} else if (!file.isFile()) {
				return new FileOpenResult("not a file: " + fileName);
			} else if (!file.canRead()) {
				return new FileOpenResult("cannot read file: " + fileName);
			}

			// open the file in read only mode
			this.inputFile = new RandomAccessFile(file, "r");
			this.fileSize = this.inputFile.length();

			this.readBuffer = new ReadBuffer(this.inputFile);
			this.mapFileHeader = new MapFileHeader();
			FileOpenResult fileOpenResult = this.mapFileHeader.readHeader(this.readBuffer, this.fileSize);
			if (!fileOpenResult.isSuccess()) {
				closeFile();
				return fileOpenResult;
			}

			return FileOpenResult.SUCCESS;
		} catch (IOException e) {
			LOG.log(Level.SEVERE, null, e);
			// make sure that the file is closed
			closeFile();
			return new FileOpenResult(e.getMessage());
		}
	}

	/**
	 * Logs the debug signatures of the current way and block.
	 */
	private void logDebugSignatures() {
		if (this.debugFile) {
			LOG.warning(DEBUG_SIGNATURE_WAY + this.signatureWay);
			LOG.warning(DEBUG_SIGNATURE_BLOCK + this.signatureBlock);
		}
	}

	private void prepareExecution() {
		if (this.databaseIndexCache == null) {
			this.databaseIndexCache = new IndexCache(this.inputFile, INDEX_CACHE_SIZE);
		}
	}

	/**
	 * Processes a single block and executes the callback functions on all map elements.
	 * 
	 * @param queryParameters
	 *            the parameters of the current query.
	 * @param subFileParameter
	 *            the parameters of the current map file.
	 * @param mapDatabaseCallback
	 *            the callback which handles the extracted map elements.
	 */
	private void processBlock(QueryParameters queryParameters, SubFileParameter subFileParameter,
			MapDatabaseCallback mapDatabaseCallback) {
		if (!processBlockSignature()) {
			return;
		}

		// calculate the offset in the block entries table and move the pointer
		int blockEntriesTableOffset = (queryParameters.queryZoomLevel - subFileParameter.zoomLevelMin) * 4;
		this.readBuffer.skipBytes(blockEntriesTableOffset);

		// get the amount of POIs and ways on the current zoomLevel level
		int poisOnZoomLevel = this.readBuffer.readShort();
		int waysOnZoomLevel = this.readBuffer.readShort();

		// move the pointer to the end of the block entries table
		this.readBuffer.skipBytes(subFileParameter.blockEntriesTableSize - blockEntriesTableOffset - 4);

		// get the relative offset to the first stored way in the block
		int firstWayOffset = this.readBuffer.readUnsignedInt();
		if (firstWayOffset < 0) {
			LOG.warning(INVALID_FIRST_WAY_OFFSET + firstWayOffset);
			if (this.debugFile) {
				LOG.warning(DEBUG_SIGNATURE_BLOCK + this.signatureBlock);
			}
			return;
		}

		// add the current buffer position to the relative first way offset
		firstWayOffset += this.readBuffer.getBufferPosition();
		if (firstWayOffset > this.readBuffer.getBufferSize()) {
			LOG.warning(INVALID_FIRST_WAY_OFFSET + firstWayOffset);
			if (this.debugFile) {
				LOG.warning(DEBUG_SIGNATURE_BLOCK + this.signatureBlock);
			}
			return;
		}

		if (!processPOIs(mapDatabaseCallback, poisOnZoomLevel)) {
			return;
		}

		// finished reading POIs, check if the current buffer position is valid
		if (this.readBuffer.getBufferPosition() > firstWayOffset) {
			LOG.warning("invalid buffer position: " + this.readBuffer.getBufferPosition());
			if (this.debugFile) {
				LOG.warning(DEBUG_SIGNATURE_BLOCK + this.signatureBlock);
			}
			return;
		}

		// move the pointer to the first way
		this.readBuffer.setBufferPosition(firstWayOffset);

		if (!processWays(queryParameters, mapDatabaseCallback, waysOnZoomLevel)) {
			return;
		}
	}

	private void processBlocks(MapDatabaseCallback mapDatabaseCallback, QueryParameters queryParameters,
			SubFileParameter subFileParameter) throws IOException {
		boolean queryIsWater = true;
		boolean queryReadWaterInfo = false;

		// read and process all blocks from top to bottom and from left to right
		for (long row = queryParameters.fromBlockY; row <= queryParameters.toBlockY; ++row) {
			for (long column = queryParameters.fromBlockX; column <= queryParameters.toBlockX; ++column) {

				// calculate the actual block number of the needed block in the file
				long blockNumber = row * subFileParameter.blocksWidth + column;

				// get the current index entry
				long currentBlockIndexEntry = this.databaseIndexCache.getIndexEntry(subFileParameter,
						blockNumber);

				// check if the current query would still return a water tile
				if (queryIsWater) {
					// check the water flag of the current block in its index entry
					queryIsWater &= (currentBlockIndexEntry & BITMASK_INDEX_WATER) != 0;
					queryReadWaterInfo = true;
				}

				// get and check the current block pointer
				long currentBlockPointer = currentBlockIndexEntry & BITMASK_INDEX_OFFSET;
				if (currentBlockPointer < 1 || currentBlockPointer > subFileParameter.subFileSize) {
					LOG.warning("invalid current block pointer: " + currentBlockPointer);
					LOG.warning("subFileSize: " + subFileParameter.subFileSize);
					return;
				}

				long nextBlockPointer;
				// check if the current block is the last block in the file
				if (blockNumber + 1 == subFileParameter.numberOfBlocks) {
					// set the next block pointer to the end of the file
					nextBlockPointer = subFileParameter.subFileSize;
				} else {
					// get and check the next block pointer
					nextBlockPointer = this.databaseIndexCache.getIndexEntry(subFileParameter, blockNumber + 1)
							& BITMASK_INDEX_OFFSET;
					if (nextBlockPointer < 1 || nextBlockPointer > subFileParameter.subFileSize) {
						LOG.warning("invalid next block pointer: " + nextBlockPointer);
						LOG.warning("sub-file size: " + subFileParameter.subFileSize);
						return;
					}
				}

				// calculate the size of the current block
				int currentBlockSize = (int) (nextBlockPointer - currentBlockPointer);
				if (currentBlockSize < 0) {
					LOG.warning("current block size must not be negative: " + currentBlockSize);
					return;
				} else if (currentBlockSize == 0) {
					// the current block is empty, continue with the next block
					continue;
				} else if (currentBlockSize > ReadBuffer.MAXIMUM_BUFFER_SIZE) {
					// the current block is too large, continue with the next block
					LOG.warning("current block size too large: " + currentBlockSize);
					continue;
				} else if (currentBlockPointer + currentBlockSize > this.fileSize) {
					LOG.warning("current block largher than file size: " + currentBlockSize);
					return;
				}

				// seek to the current block in the map file
				this.inputFile.seek(subFileParameter.startAddress + currentBlockPointer);

				// read the current block into the buffer
				if (!this.readBuffer.readFromFile(currentBlockSize)) {
					// skip the current block
					LOG.warning("reading current block has failed: " + currentBlockSize);
					return;
				}

				// calculate the top-left coordinates of the underlying tile
				this.tileLatitude = (int) (MercatorProjection.tileYToLatitude(subFileParameter.boundaryTileTop
						+ row, subFileParameter.baseZoomLevel) * 1000000);
				this.tileLongitude = (int) (MercatorProjection.tileXToLongitude(
						subFileParameter.boundaryTileLeft + column, subFileParameter.baseZoomLevel) * 1000000);

				try {
					processBlock(queryParameters, subFileParameter, mapDatabaseCallback);
				} catch (ArrayIndexOutOfBoundsException e) {
					LOG.log(Level.SEVERE, null, e);
				}
			}
		}

		// the query is finished, was the water flag set for all blocks?
		if (queryIsWater && queryReadWaterInfo) {
			mapDatabaseCallback.renderWaterBackground();
		}
	}

	/**
	 * Processes the block signature, if present.
	 * 
	 * @return true if the block signature could be processed successfully, false otherwise.
	 */
	private boolean processBlockSignature() {
		if (this.debugFile) {
			// get and check the block signature
			this.signatureBlock = this.readBuffer.readUTF8EncodedString(SIGNATURE_LENGTH_BLOCK);
			if (!this.signatureBlock.startsWith("###TileStart")) {
				LOG.warning("invalid block signature: " + this.signatureBlock);
				return false;
			}
		}
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
	 */
	private boolean processPOIs(MapDatabaseCallback mapDatabaseCallback, int numberOfPois) {
		List<Tag> tags = new ArrayList<Tag>();
		Tag[] poiTags = this.mapFileHeader.getMapFileInfo().poiTags;

		for (int elementCounter = numberOfPois; elementCounter != 0; --elementCounter) {
			if (this.debugFile) {
				// get and check the POI signature
				this.signaturePoi = this.readBuffer.readUTF8EncodedString(SIGNATURE_LENGTH_POI);
				if (!this.signaturePoi.startsWith("***POIStart")) {
					LOG.warning("invalid POI signature: " + this.signaturePoi);
					LOG.warning(DEBUG_SIGNATURE_BLOCK + this.signatureBlock);
					return false;
				}
			}

			// get the POI latitude offset (VBE-S)
			int latitude = this.tileLatitude + this.readBuffer.readSignedInt();

			// get the POI longitude offset (VBE-S)
			int longitude = this.tileLongitude + this.readBuffer.readSignedInt();

			// get the special byte which encodes multiple flags
			byte specialByte = this.readBuffer.readByte();

			// bit 1-4 represent the layer
			byte layer = (byte) ((specialByte & POI_LAYER_BITMASK) >>> POI_LAYER_SHIFT);
			// bit 5-8 represent the number of tag IDs
			byte numberOfTags = (byte) (specialByte & POI_NUMBER_OF_TAGS_BITMASK);

			tags.clear();

			// get the tag IDs (VBE-U)
			for (byte tagIndex = numberOfTags; tagIndex != 0; --tagIndex) {
				int tagId = this.readBuffer.readUnsignedInt();
				if (tagId < 0 || tagId >= poiTags.length) {
					LOG.warning("invalid POI tag ID: " + tagId);
					if (this.debugFile) {
						LOG.warning(DEBUG_SIGNATURE_POI + this.signaturePoi);
						LOG.warning(DEBUG_SIGNATURE_BLOCK + this.signatureBlock);
					}
					return false;
				}
				tags.add(poiTags[tagId]);
			}

			// get the feature bitmask (1 byte)
			byte featureByte = this.readBuffer.readByte();

			// bit 1-3 enable optional features
			boolean featureName = (featureByte & POI_FEATURE_BITMASK_NAME) != 0;
			boolean featureElevation = (featureByte & POI_FEATURE_BITMASK_ELEVATION) != 0;
			boolean featureHouseNumber = (featureByte & POI_FEATURE_BITMASK_HOUSE_NUMBER) != 0;

			// check if the POI has a name
			if (featureName) {
				tags.add(new Tag(TAG_KEY_NAME, this.readBuffer.readUTF8EncodedString()));
			}

			// check if the POI has an elevation
			if (featureElevation) {
				tags.add(new Tag(TAG_KEY_ELE, Integer.toString(this.readBuffer.readSignedInt())));
			}

			// check if the POI has a house number
			if (featureHouseNumber) {
				tags.add(new Tag(TAG_KEY_HOUSE_NUMBER, this.readBuffer.readUTF8EncodedString()));
			}

			mapDatabaseCallback.renderPointOfInterest(layer, latitude, longitude, tags);
		}

		return true;
	}

	private float[][] processWayDataBlock() {
		// get and check the number of coordinate blocks (1 byte)
		byte numberOfCoordinateBlocks = this.readBuffer.readByte();
		if (numberOfCoordinateBlocks < 1) {
			LOG.warning("invalid number of coordinate blocks: " + numberOfCoordinateBlocks);
			logDebugSignatures();
			return null;
		}

		// create the array which will store the different way coordinate blocks
		float[][] wayCoordinates = new float[numberOfCoordinateBlocks][];

		// read the way coordinate blocks
		for (byte coordinateBlock = 0; coordinateBlock < numberOfCoordinateBlocks; ++coordinateBlock) {
			// get and check the number of way nodes (VBE-U)
			int numberOfWayNodes = this.readBuffer.readUnsignedInt();
			if (numberOfWayNodes < 2 || numberOfWayNodes > MAXIMUM_WAY_NODES_SEQUENCE_LENGTH) {
				LOG.warning("invalid number of way nodes: " + numberOfWayNodes);
				logDebugSignatures();
				return null;
			}

			// each way node consists of latitude and longitude
			int wayNodesSequenceLength = numberOfWayNodes * 2;

			// create the array which will store the current way segment
			float[] waySegment = new float[wayNodesSequenceLength];

			// get the first way node latitude offset (VBE-S)
			int wayNodeLatitude = this.tileLatitude + this.readBuffer.readSignedInt();

			// get the first way node longitude offset (VBE-S)
			int wayNodeLongitude = this.tileLongitude + this.readBuffer.readSignedInt();

			// store the first way node
			waySegment[1] = wayNodeLatitude;
			waySegment[0] = wayNodeLongitude;

			// get the remaining way nodes offsets
			for (int wayNodesIndex = 2; wayNodesIndex < wayNodesSequenceLength; wayNodesIndex += 2) {
				// get the way node latitude offset (VBE-S)
				wayNodeLatitude = wayNodeLatitude + this.readBuffer.readSignedInt();

				// get the way node longitude offset (VBE-S)
				wayNodeLongitude = wayNodeLongitude + this.readBuffer.readSignedInt();

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
	 * @param queryParameters
	 *            the parameters of the current query.
	 * @param mapDatabaseCallback
	 *            the callback which handles the extracted ways.
	 * @param numberOfWays
	 *            how many ways should be processed.
	 * @return true if the ways could be processed successfully, false otherwise.
	 */
	private boolean processWays(QueryParameters queryParameters, MapDatabaseCallback mapDatabaseCallback,
			int numberOfWays) {
		List<Tag> tags = new ArrayList<Tag>();
		Tag[] wayTags = this.mapFileHeader.getMapFileInfo().wayTags;

		for (int elementCounter = numberOfWays; elementCounter != 0; --elementCounter) {
			if (this.debugFile) {
				// get and check the way signature
				this.signatureWay = this.readBuffer.readUTF8EncodedString(SIGNATURE_LENGTH_WAY);
				if (!this.signatureWay.startsWith("---WayStart")) {
					LOG.warning("invalid way signature: " + this.signatureWay);
					LOG.warning(DEBUG_SIGNATURE_BLOCK + this.signatureBlock);
					return false;
				}
			}

			// get the size of the way (VBE-U)
			int wayDataSize = this.readBuffer.readUnsignedInt();
			if (wayDataSize < 0) {
				LOG.warning("invalid way data size: " + wayDataSize);
				if (this.debugFile) {
					LOG.warning(DEBUG_SIGNATURE_BLOCK + this.signatureBlock);
				}
				return false;
			}

			if (queryParameters.useTileBitmask) {
				// get the way tile bitmask (2 bytes)
				int tileBitmask = this.readBuffer.readShort();
				// check if the way is inside the requested tile
				if ((queryParameters.queryTileBitmask & tileBitmask) == 0) {
					// skip the rest of the way and continue with the next way
					this.readBuffer.skipBytes(wayDataSize - 2);
					continue;
				}
			} else {
				// ignore the way tile bitmask (2 bytes)
				this.readBuffer.skipBytes(2);
			}

			// get the special byte which encodes multiple flags
			byte specialByte = this.readBuffer.readByte();

			// bit 1-4 represent the layer
			byte layer = (byte) ((specialByte & WAY_LAYER_BITMASK) >>> WAY_LAYER_SHIFT);
			// bit 5-8 represent the number of tag IDs
			byte numberOfTags = (byte) (specialByte & WAY_NUMBER_OF_TAGS_BITMASK);

			tags.clear();

			for (byte tagIndex = numberOfTags; tagIndex != 0; --tagIndex) {
				int tagId = this.readBuffer.readUnsignedInt();
				if (tagId < 0 || tagId >= wayTags.length) {
					LOG.warning("invalid tag ID: " + tagId);
					logDebugSignatures();
					return false;
				}
				tags.add(wayTags[tagId]);
			}

			// get the feature bitmask (1 byte)
			byte featureByte = this.readBuffer.readByte();

			// bit 1-3 enable optional features
			boolean featureName = (featureByte & WAY_FEATURE_BITMASK_NAME) != 0;
			boolean featureRef = (featureByte & WAY_FEATURE_BITMASK_REF) != 0;
			boolean featureLabelPosition = (featureByte & WAY_FEATURE_BITMASK_LABEL_POSITION) != 0;

			// check if the way has a name
			if (featureName) {
				tags.add(new Tag(TAG_KEY_NAME, this.readBuffer.readUTF8EncodedString()));
			}

			// check if the way has a reference
			if (featureRef) {
				tags.add(new Tag(TAG_KEY_REF, this.readBuffer.readUTF8EncodedString()));
			}

			float[] labelPosition = readOptionalLabelPosition(featureLabelPosition);

			// get and check the number of way data blocks (1 byte)
			byte wayDataBlocks = this.readBuffer.readByte();
			if (wayDataBlocks < 1) {
				LOG.warning("invalid number of way data blocks: " + wayDataBlocks);
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

	private float[] readOptionalLabelPosition(boolean featureLabelPosition) {
		float[] labelPosition = null;
		if (featureLabelPosition) {
			labelPosition = new float[2];

			// get the label position latitude offset (VBE-S)
			labelPosition[1] = this.tileLatitude + this.readBuffer.readSignedInt();

			// get the label position longitude offset (VBE-S)
			labelPosition[0] = this.tileLongitude + this.readBuffer.readSignedInt();
		}
		return labelPosition;
	}
}
