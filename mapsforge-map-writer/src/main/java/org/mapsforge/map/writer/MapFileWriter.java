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
package org.mapsforge.map.writer;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.mapsforge.map.writer.model.Encoding;
import org.mapsforge.map.writer.model.GeoCoordinate;
import org.mapsforge.map.writer.model.MapWriterConfiguration;
import org.mapsforge.map.writer.model.MercatorProjection;
import org.mapsforge.map.writer.model.OSMTag;
import org.mapsforge.map.writer.model.TDNode;
import org.mapsforge.map.writer.model.TDWay;
import org.mapsforge.map.writer.model.TileBasedDataProcessor;
import org.mapsforge.map.writer.model.TileCoordinate;
import org.mapsforge.map.writer.model.TileData;
import org.mapsforge.map.writer.model.TileInfo;
import org.mapsforge.map.writer.model.WayDataBlock;
import org.mapsforge.map.writer.model.ZoomIntervalConfiguration;
import org.mapsforge.map.writer.util.Constants;
import org.mapsforge.map.writer.util.GeoUtils;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Writes the binary file format for mapsforge maps.
 * 
 * @author bross
 */
public class MapFileWriter {

	private static final long DUMMY_LONG = 0xf0f0f0f0f0f0f0f0L;

	private static final int DUMMY_INT = 0xf0f0f0f0;

	private static final int BYTES_SHORT = 2;
	private static final int BYTES_INT = 4;

	private static final int DEBUG_BLOCK_SIZE = 32;

	private static final String DEBUG_INDEX_START_STRING = "+++IndexStart+++";

	private static final int SIZE_ZOOMINTERVAL_CONFIGURATION = 19;

	// private static final int PIXEL_COMPRESSION_MAX_DELTA = 5;

	private static final int BYTE_AMOUNT_SUBFILE_INDEX_PER_TILE = 5;

	private static final String MAGIC_BYTE = "mapsforge binary OSM";

	private static final int OFFSET_FILE_SIZE = 28;

	// private static final CoastlineHandler COASTLINE_HANDLER = new
	// CoastlineHandler();

	// DEBUG STRINGS
	private static final String DEBUG_STRING_POI_HEAD = "***POIStart";
	private static final String DEBUG_STRING_POI_TAIL = "***";
	private static final String DEBUG_STRING_TILE_HEAD = "###TileStart";
	private static final String DEBUG_STRING_TILE_TAIL = "###";
	private static final String DEBUG_STRING_WAY_HEAD = "---WayStart";
	private static final String DEBUG_STRING_WAY_TAIL = "---";

	// bitmap flags for pois and ways
	private static final short BITMAP_NAME = 128; // NOPMD by bross on 25.12.11 13:52

	// bitmap flags for pois
	private static final short BITMAP_ELEVATION = 64; // NOPMD by bross on 25.12.11 13:52
	private static final short BITMAP_HOUSENUMBER = 32; // NOPMD by bross on 25.12.11 13:52

	// bitmap flags for ways
	private static final short BITMAP_REF = 64; // NOPMD by bross on 25.12.11 13:52
	private static final short BITMAP_LABEL = 32; // NOPMD by bross on 25.12.11 13:52
	private static final short BITMAP_MULTIPLE_WAY_BLOCKS = 16;
	// TODO write encoding bit
	private static final short BITMAP_ENCODING = 8;

	// bitmap flags for file features
	private static final short BITMAP_DEBUG = 128; // NOPMD by bross on 25.12.11 13:53
	private static final short BITMAP_MAP_START_POSITION = 64; // NOPMD by bross on 25.12.11 13:53
	private static final short BITMAP_MAP_START_ZOOM = 32; // NOPMD by bross on 25.12.11 13:53
	private static final short BITMAP_PREFERRED_LANGUAGE = 16; // NOPMD by bross on 25.12.11 13:53
	private static final short BITMAP_COMMENT = 8; // NOPMD by bross on 25.12.11 13:53

	private static final int BITMAP_INDEX_ENTRY_WATER = 0x80;

	private static final Logger LOGGER = Logger.getLogger(MapFileWriter.class.getName());

	private static final String PROJECTION = "Mercator";

	// private static final byte MAX_ZOOMLEVEL_PIXEL_FILTER = 11;

	// private static final byte MIN_ZOOMLEVEL_POLYGON_CLIPPING = 8;

	private static final Charset UTF8_CHARSET = Charset.forName("utf8");

	private static final float PROGRESS_PERCENT_STEP = 10f;

	private static final TileInfo TILE_INFO = TileInfo.getInstance();
	// private static final CoastlineHandler COASTLINE_HANDLER = new
	// CoastlineHandler();

	// IO
	private static final int HEADER_BUFFER_SIZE = 0x100000; // 1MB
	private static final int MIN_TILE_BUFFER_SIZE = 0xF00000; // 15MB
	private static final int COMPRESSED_TILES_BUFFER_SIZE = 0x3200000; // 50MB
	private static final int TILE_BUFFER_SIZE = 0xA00000; // 10MB
	private static final int WAY_BUFFER_SIZE = 0x100000; // 1MB
	private static final int POI_BUFFER_SIZE = 0x100000; // 1MB

	public static void writeFile(MapWriterConfiguration configuration, TileBasedDataProcessor dataStore)
			throws IOException {

		RandomAccessFile randomAccessFile = new RandomAccessFile(configuration.getOutputFile(), "rw");

		int amountOfZoomIntervals = dataStore.getZoomIntervalConfiguration().getNumberOfZoomIntervals();
		ByteBuffer containerHeaderBuffer = ByteBuffer.allocate(HEADER_BUFFER_SIZE);
		// CONTAINER HEADER
		int totalHeaderSize = writeContainerHeaderBuffer(configuration, dataStore, containerHeaderBuffer);

		// set to mark where zoomIntervalConfig starts
		containerHeaderBuffer.reset();

		// SUB FILES
		// for each zoom interval write a sub file
		long currentFileSize = totalHeaderSize;
		for (int i = 0; i < amountOfZoomIntervals; i++) {
			// SUB FILE INDEX AND DATA
			long subfileSize = writeSubfile(currentFileSize, i, dataStore, randomAccessFile, configuration);
			// SUB FILE META DATA IN CONTAINER HEADER
			writeSubfileMetaDataToContainerHeader(dataStore.getZoomIntervalConfiguration(), i, currentFileSize,
					subfileSize, containerHeaderBuffer);
			currentFileSize += subfileSize;
		}

		randomAccessFile.seek(0);
		randomAccessFile.write(containerHeaderBuffer.array(), 0, totalHeaderSize);

		// WRITE FILE SIZE TO HEADER
		long fileSize = randomAccessFile.length();
		randomAccessFile.seek(OFFSET_FILE_SIZE);
		randomAccessFile.writeLong(fileSize);

		randomAccessFile.close();

		LOGGER.info("Finished writing file.");
	}

	private static void writeUTF8(String string, ByteBuffer buffer) {
		buffer.put(Serializer.getVariableByteUnsigned(string.getBytes(UTF8_CHARSET).length));
		buffer.put(string.getBytes(UTF8_CHARSET));
	}

	private static int writeContainerHeaderBuffer(final MapWriterConfiguration configuration,
			final TileBasedDataProcessor dataStore, final ByteBuffer containerHeaderBuffer) {

		LOGGER.fine("writing header");
		LOGGER.fine("Bounding box for file: " + dataStore.getBoundingBox().maxLatitudeE6 + ", "
				+ dataStore.getBoundingBox().minLongitudeE6 + ", " + dataStore.getBoundingBox().minLatitudeE6 + ", "
				+ dataStore.getBoundingBox().maxLongitudeE6);

		// write file header
		// MAGIC BYTE
		byte[] magicBytes = MAGIC_BYTE.getBytes();
		containerHeaderBuffer.put(magicBytes);

		// HEADER SIZE: Write dummy pattern as header size. It will be replaced
		// later in time
		int headerSizePosition = containerHeaderBuffer.position();
		containerHeaderBuffer.putInt(DUMMY_INT);

		// FILE VERSION
		containerHeaderBuffer.putInt(configuration.getVersion());

		// FILE SIZE: Write dummy pattern as file size. It will be replaced
		// later in time
		containerHeaderBuffer.putLong(DUMMY_LONG);
		// DATE OF CREATION
		containerHeaderBuffer.putLong(System.currentTimeMillis());

		// BOUNDING BOX
		containerHeaderBuffer.putInt(dataStore.getBoundingBox().minLatitudeE6);
		containerHeaderBuffer.putInt(dataStore.getBoundingBox().minLongitudeE6);
		containerHeaderBuffer.putInt(dataStore.getBoundingBox().maxLatitudeE6);
		containerHeaderBuffer.putInt(dataStore.getBoundingBox().maxLongitudeE6);

		// TILE SIZE
		containerHeaderBuffer.putShort((short) Constants.DEFAULT_TILE_SIZE);

		// PROJECTION
		writeUTF8(PROJECTION, containerHeaderBuffer);

		// check whether zoom start is a valid zoom level

		// FLAGS
		containerHeaderBuffer.put(infoByteOptmizationParams(configuration.isDebugStrings(),
				configuration.getMapStartPosition() != null, configuration.hasMapStartZoomLevel(),
				configuration.getPreferredLanguage() != null, configuration.getComment() != null));

		// MAP START POSITION
		if (configuration.getMapStartPosition() != null) {
			containerHeaderBuffer.putInt(configuration.getMapStartPosition().getLatitudeE6());
			containerHeaderBuffer.putInt(configuration.getMapStartPosition().getLongitudeE6());
		}

		// MAP START ZOOM
		if (configuration.hasMapStartZoomLevel()) {
			containerHeaderBuffer.put((byte) configuration.getMapStartZoomLevel());
		}

		// PREFERRED LANGUAGE
		if (configuration.getPreferredLanguage() != null) {
			writeUTF8(configuration.getPreferredLanguage(), containerHeaderBuffer);
		}

		// COMMENT
		if (configuration.getComment() != null) {
			writeUTF8(configuration.getComment(), containerHeaderBuffer);
		}

		// AMOUNT POI TAGS
		containerHeaderBuffer.putShort((short) configuration.getTagMapping().getOptimizedPoiIds().size());
		// POI TAGS
		// retrieves tag ids in order of frequency, most frequent come first
		for (short tagId : configuration.getTagMapping().getOptimizedPoiIds().keySet()) {
			OSMTag tag = configuration.getTagMapping().getPoiTag(tagId);
			writeUTF8(tag.tagKey(), containerHeaderBuffer);
		}

		// AMOUNT OF WAY TAGS
		containerHeaderBuffer.putShort((short) configuration.getTagMapping().getOptimizedWayIds().size());

		// WAY TAGS
		for (short tagId : configuration.getTagMapping().getOptimizedWayIds().keySet()) {
			OSMTag tag = configuration.getTagMapping().getWayTag(tagId);
			writeUTF8(tag.tagKey(), containerHeaderBuffer);
		}

		// AMOUNT OF ZOOM INTERVALS
		int numberOfZoomIntervals = dataStore.getZoomIntervalConfiguration().getNumberOfZoomIntervals();
		containerHeaderBuffer.put((byte) numberOfZoomIntervals);

		// ZOOM INTERVAL CONFIGURATION: SKIP COMPUTED AMOUNT OF BYTES
		containerHeaderBuffer.mark();
		containerHeaderBuffer.position(containerHeaderBuffer.position() + SIZE_ZOOMINTERVAL_CONFIGURATION
				* numberOfZoomIntervals);

		// now write header size
		// -4 bytes of header size variable itself
		int headerSize = containerHeaderBuffer.position() - headerSizePosition - BYTES_INT;
		containerHeaderBuffer.putInt(headerSizePosition, headerSize);

		return containerHeaderBuffer.position();
	}

	private static void writeSubfileMetaDataToContainerHeader(ZoomIntervalConfiguration zoomIntervalConfiguration,
			int i, long startIndexOfSubfile, long subfileSize, ByteBuffer buffer) {

		// HEADER META DATA FOR SUB FILE
		// write zoom interval configuration to header
		byte minZoomCurrentInterval = zoomIntervalConfiguration.getMinZoom(i);
		byte maxZoomCurrentInterval = zoomIntervalConfiguration.getMaxZoom(i);
		byte baseZoomCurrentInterval = zoomIntervalConfiguration.getBaseZoom(i);

		buffer.put(baseZoomCurrentInterval);
		buffer.put(minZoomCurrentInterval);
		buffer.put(maxZoomCurrentInterval);
		buffer.putLong(startIndexOfSubfile);
		buffer.putLong(subfileSize);
	}

	private static long writeSubfile(final long startPositionSubfile, final int zoomIntervalIndex,
			final TileBasedDataProcessor dataStore, final RandomAccessFile randomAccessFile,
			final MapWriterConfiguration configuration) throws IOException {

		LOGGER.fine("writing data for zoom interval " + zoomIntervalIndex + ", number of tiles: "
				+ dataStore.getTileGridLayout(zoomIntervalIndex).getAmountTilesHorizontal()
				* dataStore.getTileGridLayout(zoomIntervalIndex).getAmountTilesVertical());

		TileCoordinate upperLeft = dataStore.getTileGridLayout(zoomIntervalIndex).getUpperLeft();
		int lengthX = dataStore.getTileGridLayout(zoomIntervalIndex).getAmountTilesHorizontal();
		int lengthY = dataStore.getTileGridLayout(zoomIntervalIndex).getAmountTilesVertical();

		final byte minZoomCurrentInterval = dataStore.getZoomIntervalConfiguration().getMinZoom(zoomIntervalIndex);
		final byte maxZoomCurrentInterval = dataStore.getZoomIntervalConfiguration().getMaxZoom(zoomIntervalIndex);
		final byte baseZoomCurrentInterval = dataStore.getZoomIntervalConfiguration().getBaseZoom(zoomIntervalIndex);

		int tileAmountInBytes = lengthX * lengthY * BYTE_AMOUNT_SUBFILE_INDEX_PER_TILE;
		int indexBufferSize = tileAmountInBytes
				+ (configuration.isDebugStrings() ? DEBUG_INDEX_START_STRING.getBytes().length : 0);
		ByteBuffer indexBuffer = ByteBuffer.allocate(indexBufferSize);
		ByteBuffer compressedTilesBuffer = ByteBuffer.allocate(COMPRESSED_TILES_BUFFER_SIZE);
		ByteBuffer wayBuffer = ByteBuffer.allocate(WAY_BUFFER_SIZE);
		ByteBuffer poiBuffer = ByteBuffer.allocate(POI_BUFFER_SIZE);
		ByteBuffer tileBuffer = ByteBuffer.allocate(TILE_BUFFER_SIZE);

		// write debug strings for tile index segment if necessary
		if (configuration.isDebugStrings()) {
			indexBuffer.put(DEBUG_INDEX_START_STRING.getBytes());
		}

		long currentSubfileOffset = indexBufferSize;
		randomAccessFile.seek(startPositionSubfile + indexBufferSize);

		// loop over tiles (row-wise)
		for (int tileY = upperLeft.getY(); tileY < upperLeft.getY() + lengthY; tileY++) {
			for (int tileX = upperLeft.getX(); tileX < upperLeft.getX() + lengthX; tileX++) {
				// logger.info("writing data for tile (" + tileX + ", " + tileY
				// + ")");

				// ***************** TILE INDEX SEGMENT ********************

				TileCoordinate currentTileCoordinate = new TileCoordinate(tileX, tileY, baseZoomCurrentInterval);
				int currentTileLat = GeoCoordinate.doubleToInt(MercatorProjection.tileYToLatitude(
						currentTileCoordinate.getY(), currentTileCoordinate.getZoomlevel()));
				int currentTileLon = GeoCoordinate.doubleToInt(MercatorProjection.tileXToLongitude(
						currentTileCoordinate.getX(), currentTileCoordinate.getZoomlevel()));

				byte[] indexBytes = Serializer.getFiveBytes(currentSubfileOffset);
				if (TILE_INFO.isWaterTile(currentTileCoordinate)) {
					indexBytes[0] |= BITMAP_INDEX_ENTRY_WATER;
				}
				// else {
				// the TileInfo class may produce false negatives for tiles on
				// zoom level
				// greater than TileInfo.TILE_INFO_ZOOMLEVEL
				// we need to run the coastline algorithm to detect whether the
				// tile is
				// completely covered by water or not
				// if (currentTileCoordinate.getZoomlevel() >
				// TileInfo.TILE_INFO_ZOOMLEVEL) {
				// if (COASTLINE_HANDLER.isWaterTile(currentTileCoordinate,
				// dataStore.getCoastLines(currentTileCoordinate))) {
				// indexBytes[0] |= BITMAP_INDEX_ENTRY_WATER;
				// }
				// }
				// }

				// append relative offset of this tile as five bytes to the
				// index
				indexBuffer.put(indexBytes);

				// ***************** TILE DATA SEGMENT ********************

				// clear tile buffer
				tileBuffer.clear();

				// get data for tile
				TileData currentTile = dataStore.getTile(zoomIntervalIndex, tileX, tileY);

				// ***************** POIs ********************
				// write amount of POIs and ways for each zoom level
				Map<Byte, List<TDNode>> poisByZoomlevel = currentTile.poisByZoomlevel(minZoomCurrentInterval,
						maxZoomCurrentInterval);
				Map<Byte, List<TDWay>> waysByZoomlevel = currentTile.waysByZoomlevel(minZoomCurrentInterval,
						maxZoomCurrentInterval);

				if (!poisByZoomlevel.isEmpty() || !waysByZoomlevel.isEmpty()) {
					if (configuration.isDebugStrings()) {
						// write tile header
						StringBuilder sb = new StringBuilder();
						sb.append(DEBUG_STRING_TILE_HEAD).append(tileX).append(",").append(tileY)
								.append(DEBUG_STRING_TILE_TAIL);
						tileBuffer.put(sb.toString().getBytes());
						// append withespaces so that block has 32 bytes
						appendWhitespace(DEBUG_BLOCK_SIZE - sb.toString().getBytes().length, tileBuffer);
					}

					int entitiesPerZoomLevelTablePosition = tileBuffer.position();
					short[][] entitiesPerZoomLevel = new short[maxZoomCurrentInterval - minZoomCurrentInterval + 1][2];
					// skip some bytes that will later be filled with the number
					// of POIs and ways on
					// each zoom level number of zoom levels times 2 (for POIs
					// and ways) times
					// BYTES_SHORT for the amount of bytes
					tileBuffer.position(tileBuffer.position() + entitiesPerZoomLevel.length * 2 * BYTES_SHORT);

					// clear poi buffer
					poiBuffer.clear();

					// write POIs for each zoom level beginning with lowest zoom
					// level
					for (byte zoomlevel = minZoomCurrentInterval; zoomlevel <= maxZoomCurrentInterval; zoomlevel++) {
						int indexEntitiesPerZoomLevelTable = zoomlevel - minZoomCurrentInterval;
						List<TDNode> pois = poisByZoomlevel.get(Byte.valueOf(zoomlevel));
						if (pois == null) {
							continue;
						}
						for (TDNode poi : pois) {
							if (configuration.isDebugStrings()) {
								StringBuilder sb = new StringBuilder();
								sb.append(DEBUG_STRING_POI_HEAD).append(poi.getId()).append(DEBUG_STRING_POI_TAIL);
								poiBuffer.put(sb.toString().getBytes());
								// append withespaces so that block has 32 bytes
								appendWhitespace(DEBUG_BLOCK_SIZE - sb.toString().getBytes().length, poiBuffer);
							}

							// write poi features to the file
							poiBuffer.put(Serializer.getVariableByteSigned(poi.getLatitude() - currentTileLat));
							poiBuffer.put(Serializer.getVariableByteSigned(poi.getLongitude() - currentTileLon));

							// write byte with layer and tag amount info
							poiBuffer.put(infoBytePoiLayerAndTagAmount(poi));

							// write tag ids to the file
							if (poi.getTags() != null) {
								for (short tagID : poi.getTags()) {
									poiBuffer.put(Serializer.getVariableByteUnsigned(OSMTagMapping.getInstance()
											.getOptimizedPoiIds().get(Short.valueOf(tagID)).intValue()));
								}
							}

							// write byte with bits set to 1 if the poi has a
							// name, an elevation
							// or a housenumber
							poiBuffer.put(infoBytePOI(poi.getName(), poi.getElevation(), poi.getHouseNumber()));

							if (poi.getName() != null && poi.getName().length() > 0) {
								writeUTF8(poi.getName(), poiBuffer);

							}
							if (poi.getElevation() != 0) {
								poiBuffer.put(Serializer.getVariableByteSigned(poi.getElevation()));
							}
							if (poi.getHouseNumber() != null && poi.getHouseNumber().length() > 0) {
								writeUTF8(poi.getHouseNumber(), poiBuffer);
							}

							// increment count of POIs on this zoom level
							entitiesPerZoomLevel[indexEntitiesPerZoomLevelTable][0]++;
						}
					} // end for loop over POIs

					// write offset to first way in the tile header
					tileBuffer.put(Serializer.getVariableByteUnsigned(poiBuffer.position()));
					// write POIs to buffer
					tileBuffer.put(poiBuffer.array(), 0, poiBuffer.position());

					// ***************** WAYS ********************
					// loop over all relevant zoom levels
					for (byte zoomlevel = minZoomCurrentInterval; zoomlevel <= maxZoomCurrentInterval; zoomlevel++) {
						int indexEntitiesPerZoomLevelTable = zoomlevel - minZoomCurrentInterval;
						List<TDWay> ways = waysByZoomlevel.get(Byte.valueOf(zoomlevel));
						if (ways == null) {
							continue;
						}

						for (TDWay way : ways) {
							wayBuffer.clear();

							WayPreprocessingResult wpr = preprocessWay(way, currentTileCoordinate, dataStore,
									configuration);

							if (wpr == null) {
								// exclude this way
								continue;
							}

							if (configuration.isDebugStrings()) {
								StringBuilder sb = new StringBuilder();
								sb.append(DEBUG_STRING_WAY_HEAD).append(way.getId()).append(DEBUG_STRING_WAY_TAIL);
								tileBuffer.put(sb.toString().getBytes());
								// append withespaces so that block has 32 bytes
								appendWhitespace(DEBUG_BLOCK_SIZE - sb.toString().getBytes().length, tileBuffer);
							}

							// write subtile bitmask of way
							wayBuffer.putShort(wpr.getSubtileMask());

							// write byte with layer and tag amount
							wayBuffer.put(infoByteWayLayerAndTagAmount(way));

							// write tag ids
							if (way.getTags() != null) {
								for (short tagID : way.getTags()) {
									wayBuffer.put(Serializer.getVariableByteUnsigned(mappedWayTagID(tagID)));
								}
							}

							// write a byte with flags for existence of name,
							// ref, label position, and multiple blocks
							wayBuffer.put(infoByteWayFeatures(way, wpr));

							// // if the way has a name, write it to the file
							if (way.getName() != null && !way.getName().isEmpty()) {
								writeUTF8(way.getName(), wayBuffer);
							}

							// if the way has a ref, write it to the file
							if (way.getRef() != null && !way.getRef().isEmpty()) {
								writeUTF8(way.getRef(), wayBuffer);
							}

							if (wpr.getLabelPosition() != null) {
								int firstWayStartLat = wpr.getWayDataBlocks().get(0).getOuterWay().get(0).intValue();
								int firstWayStartLon = wpr.getWayDataBlocks().get(0).getOuterWay().get(1).intValue();

								wayBuffer.put(Serializer.getVariableByteSigned(wpr.getLabelPosition().getLatitudeE6()
										- firstWayStartLat));
								wayBuffer.put(Serializer.getVariableByteSigned(wpr.getLabelPosition().getLongitudeE6()
										- firstWayStartLon));
							}

							if (wpr.getWayDataBlocks().size() > 1) {
								// write the amount of way data blocks
								wayBuffer.put(Serializer.getVariableByteUnsigned(wpr.getWayDataBlocks().size()));
							}

							// write the way data blocks

							// case 1: simple way or simple polygon --> the way
							// block consists of
							// exactly one way
							// case 2: multi polygon --> the way consists of
							// exactly one outer way and
							// one or more inner ways
							for (WayDataBlock wayDataBlock : wpr.getWayDataBlocks()) {

								// write the amount of coordinate blocks
								// we have at least one block (potentially
								// interpreted as outer way) and
								// possible blocks for inner ways
								if (wayDataBlock.getInnerWays() != null && !wayDataBlock.getInnerWays().isEmpty()) {
									// multi polygon: outer way + number of
									// inner ways
									wayBuffer.put((byte) (1 + wayDataBlock.getInnerWays().size()));
								} else {
									// simply a single way (not a multi polygon)
									wayBuffer.put((byte) 1);
								}

								// write block for (outer/simple) way
								writeWay(wayDataBlock.getOuterWay(), currentTileLat, currentTileLon, wayBuffer);

								// write blocks for inner ways
								if (wayDataBlock.getInnerWays() != null && !wayDataBlock.getInnerWays().isEmpty()) {
									for (List<Integer> innerWayCoordinates : wayDataBlock.getInnerWays()) {
										writeWay(innerWayCoordinates, currentTileLat, currentTileLon, wayBuffer);
									}
								}

							}

							// write size of way to tile buffer
							tileBuffer.put(Serializer.getVariableByteUnsigned(wayBuffer.position()));
							// write way data to tile buffer
							tileBuffer.put(wayBuffer.array(), 0, wayBuffer.position());

							// increment count of ways on this zoom level
							entitiesPerZoomLevel[indexEntitiesPerZoomLevelTable][1]++;
						}
					} // end for loop over ways

					int tileSize = tileBuffer.position();

					// update the zoom level table
					// write cumulated number of POIs and ways for this tile on
					// each zoom level
					tileBuffer.position(entitiesPerZoomLevelTablePosition);
					short[] cumulatedCounts = new short[2];
					for (short[] entityCount : entitiesPerZoomLevel) {
						cumulatedCounts[0] += entityCount[0];
						cumulatedCounts[1] += entityCount[1];
						tileBuffer.putShort(cumulatedCounts[0]);
						tileBuffer.putShort(cumulatedCounts[1]);
					}

					tileBuffer.position(tileSize);

					currentSubfileOffset += tileBuffer.position();

					// add tile to tiles buffer
					compressedTilesBuffer.put(tileBuffer.array(), 0, tileSize);

					// if necessary, allocate new buffer
					if (compressedTilesBuffer.remaining() < MIN_TILE_BUFFER_SIZE) {
						randomAccessFile.write(compressedTilesBuffer.array(), 0, compressedTilesBuffer.position());
						compressedTilesBuffer.clear();
					}
				} // end if clause checking if tile is empty or not

				// TODO accounting for progress information
			} // end for loop over tile columns
		} // /end for loop over tile rows

		// write remaining tiles
		if (compressedTilesBuffer.position() > 0) {
			// byte buffer was not previously cleared
			randomAccessFile.write(compressedTilesBuffer.array(), 0, compressedTilesBuffer.position());
		}

		// write index
		randomAccessFile.seek(startPositionSubfile);
		randomAccessFile.write(indexBuffer.array());
		randomAccessFile.seek(currentSubfileOffset);

		// return size of sub file in bytes
		return currentSubfileOffset;
	} // end writeSubfile()

	private static void writeWay(List<Integer> wayNodes, int currentTileLat, int currentTileLon, ByteBuffer buffer) {
		// write the amount of way nodes to the file
		// wayBuffer
		buffer.put(Serializer.getVariableByteUnsigned(wayNodes.size() / 2));

		// write the way nodes:
		// the first node is always stored with four bytes
		// the remaining way node differences are stored according to the
		// compression type
		writeWayNodes(wayNodes, currentTileLat, currentTileLon, buffer);
	}

	private static void writeWayNodes(List<Integer> waynodes, int currentTileLat, int currentTileLon, ByteBuffer buffer) {
		if (!waynodes.isEmpty() && waynodes.size() % 2 == 0) {
			Iterator<Integer> waynodeIterator = waynodes.iterator();
			buffer.put(Serializer.getVariableByteSigned(waynodeIterator.next().intValue() - currentTileLat));
			buffer.put(Serializer.getVariableByteSigned(waynodeIterator.next().intValue() - currentTileLon));

			while (waynodeIterator.hasNext()) {
				buffer.put(Serializer.getVariableByteSigned(waynodeIterator.next().intValue()));
			}
		}
	}

	private static void appendWhitespace(int amount, ByteBuffer buffer) {
		for (int i = 0; i < amount; i++) {
			buffer.put((byte) ' ');
		}
	}

	private static WayPreprocessingResult preprocessWay(TDWay way, TileCoordinate tile,
			TileBasedDataProcessor dataStore, MapWriterConfiguration configuration) {

		// TODO more sophisticated clipping of polygons needed
		// we have a problem when clipping polygons which border needs to be
		// rendered
		// the problem does not occur with polygons that do not have a border
		// imagine an administrive border, such a polygon is not filled, but its
		// border is rendered
		// in case the polygon spans multiple base zoom tiles, clipping
		// introduces connections between
		// nodes that haven't existed before (exactly at the borders of a base
		// tile)
		// in case of filled polygons we do not care about these connections
		// polygons that represent a border must be clipped as simple ways and
		// not as polygons

		List<TDWay> innerways = dataStore.getInnerWaysOfMultipolygon(way.getId());
		// wayDataBlockList = GeoUtils.preprocessWay(way, null, clipPolygons,
		// simplify, clipWays, tile,
		// bboxEnlargement);
		Geometry geometry = GeoUtils.preprocessWay(way, innerways, configuration.isPolygonClipping(),
				configuration.isWayClipping(), configuration.getSimplification(), tile,
				configuration.getBboxEnlargement());
		List<WayDataBlock> blocks = GeoUtils.toWayDataBlockList(geometry);
		if (blocks == null) {
			return null;
		}
		if (blocks.isEmpty()) {
			LOGGER.finer("empty list of way data blocks after preprocessing way: " + way.getId());
			return null;
		}
		short subtileMask = GeoUtils.computeBitmask(geometry, tile, configuration.getBboxEnlargement());

		// check if the polygon is completely contained in the current tile
		// in that case clipped polygon equals the original polygon
		// as a consequence we do not try to compute a label position
		// this is left to the renderer for more flexibility
		GeoCoordinate centroid = null;
		if (GeoUtils.coveredByTile(geometry, tile, configuration.getBboxEnlargement())) {
			centroid = GeoUtils.computeCentroid(geometry);
		}

		switch (configuration.getEncodingChoice()) {
			case SINGLE:
				blocks = DeltaEncoder.encode(blocks, Encoding.DELTA);
				break;
			case DOUBLE:
				blocks = DeltaEncoder.encode(blocks, Encoding.DOUBLE_DELTA);
				break;
			case AUTO:
				List<WayDataBlock> blocksDelta = DeltaEncoder.encode(blocks, Encoding.DELTA);
				List<WayDataBlock> blocksDoubleDelta = DeltaEncoder.encode(blocks, Encoding.DOUBLE_DELTA);
				int simDelta = DeltaEncoder.simulateSerialization(blocksDelta);
				int simDoubleDelta = DeltaEncoder.simulateSerialization(blocksDoubleDelta);
				if (simDelta <= simDoubleDelta) {
					blocks = blocksDelta;
				} else {
					blocks = blocksDoubleDelta;
				}
				break;
		}

		return new WayPreprocessingResult(blocks, centroid, subtileMask);
	}

	private static int mappedWayTagID(short original) {
		return OSMTagMapping.getInstance().getOptimizedWayIds().get(Short.valueOf(original)).intValue();
	}

	private static byte infoBytePoiLayerAndTagAmount(TDNode node) {
		byte layer = node.getLayer();
		// make sure layer is in [0,10]
		layer = layer < 0 ? 0 : layer > 10 ? 10 : layer;
		short tagAmount = node.getTags() == null ? 0 : (short) node.getTags().length;

		return (byte) (layer << BYTES_INT | tagAmount);
	}

	private static byte infoByteWayLayerAndTagAmount(TDWay way) {
		byte layer = way.getLayer();
		// make sure layer is in [0,10]
		layer = layer < 0 ? 0 : layer > 10 ? 10 : layer;
		short tagAmount = way.getTags() == null ? 0 : (short) way.getTags().length;

		return (byte) (layer << BYTES_INT | tagAmount);
	}

	private static byte infoByteOptmizationParams(boolean debug, boolean mapStartPosition, boolean mapStartZoom,
			boolean preferredLanguage, boolean comment) {
		byte infoByte = 0;

		if (debug) {
			infoByte |= BITMAP_DEBUG;
		}
		if (mapStartPosition) {
			infoByte |= BITMAP_MAP_START_POSITION;
		}
		if (mapStartZoom) {
			infoByte |= BITMAP_MAP_START_ZOOM;
		}
		if (preferredLanguage) {
			infoByte |= BITMAP_PREFERRED_LANGUAGE;
		}
		if (comment) {
			infoByte |= BITMAP_COMMENT;
		}

		return infoByte;
	}

	private static byte infoBytePOI(String name, int elevation, String housenumber) {
		byte infoByte = 0;

		if (name != null && name.length() > 0) {
			infoByte |= BITMAP_NAME;
		}
		if (elevation != 0) {
			infoByte |= BITMAP_ELEVATION;
		}
		if (housenumber != null && housenumber.length() > 0) {
			infoByte |= BITMAP_HOUSENUMBER;
		}
		return infoByte;
	}

	private static byte infoByteWayFeatures(TDWay way, WayPreprocessingResult wpr) {
		byte infoByte = 0;

		if (way.getName() != null && !way.getName().isEmpty()) {
			infoByte |= BITMAP_NAME;
		}
		if (way.getRef() != null && !way.getRef().isEmpty()) {
			infoByte |= BITMAP_REF;
		}
		if (wpr.getLabelPosition() != null) {
			infoByte |= BITMAP_LABEL;
		}
		if (wpr.getWayDataBlocks().size() > 1) {
			infoByte |= BITMAP_MULTIPLE_WAY_BLOCKS;
		}

		if (!wpr.getWayDataBlocks().isEmpty()) {
			WayDataBlock wayDataBlock = wpr.getWayDataBlocks().get(0);
			if (wayDataBlock.getEncoding() == Encoding.DOUBLE_DELTA) {
				infoByte |= BITMAP_ENCODING;
			}
		}

		return infoByte;
	}

	private static class WayPreprocessingResult {

		final List<WayDataBlock> wayDataBlocks;
		final GeoCoordinate labelPosition;
		final short subtileMask;

		WayPreprocessingResult(List<WayDataBlock> wayDataBlocks, GeoCoordinate labelPosition, short subtileMask) {
			super();
			this.wayDataBlocks = wayDataBlocks;
			this.labelPosition = labelPosition;
			this.subtileMask = subtileMask;
		}

		List<WayDataBlock> getWayDataBlocks() {
			return this.wayDataBlocks;
		}

		GeoCoordinate getLabelPosition() {
			return this.labelPosition;
		}

		short getSubtileMask() {
			return this.subtileMask;
		}

	}

}
