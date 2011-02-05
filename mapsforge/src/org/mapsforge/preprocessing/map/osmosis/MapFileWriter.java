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
package org.mapsforge.preprocessing.map.osmosis;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import org.mapsforge.core.GeoCoordinate;
import org.mapsforge.core.MercatorProjection;
import org.mapsforge.preprocessing.map.osmosis.TileData.TDNode;
import org.mapsforge.preprocessing.map.osmosis.TileData.TDWay;

class MapFileWriter {

	private static final String DEBUG_INDEX_START_STRING = "+++IndexStart+++";

	private static final int SIZE_ZOOMINTERVAL_CONFIGURATION = 13;

	private static final int PIXEL_COMPRESSION_MAX_DELTA = 5;

	private static final int BYTE_AMOUNT_SUBFILE_INDEX_PER_TILE = 5;

	private static final String MAGIC_BYTE = "mapsforge binary OSM";

	// DEBUG STRINGS
	private static final String DEBUG_STRING_POI_HEAD = "***POIStart";
	private static final String DEBUG_STRING_POI_TAIL = "***";
	private static final String DEBUG_STRING_TILE_HEAD = "###TileStart";
	private static final String DEBUG_STRING_TILE_TAIL = "###";
	private static final String DEBUG_STRING_WAY_HEAD = "---WayStart";
	private static final String DEBUG_STRING_WAY_TAIL = "---";

	// bitmap flags for pois and ways
	private static final short BITMAP_NAME = 128;

	// bitmap flags for pois
	private static final short BITMAP_ELEVATION = 64;
	private static final short BITMAP_HOUSENUMBER = 32;

	// bitmap flags for ways
	private static final short BITMAP_REF = 64;
	private static final short BITMAP_LABEL = 32;
	private static final short BITMAP_MULTIPOLYGON = 16;
	private static final short BITMAP_HIGHWAY = 128;
	private static final short BITMAP_RAILWAY = 64;
	private static final short BITMAP_BUILDING = 32;
	private static final short BITMAP_LANDUSE = 16;
	private static final short BITMAP_LEISURE = 8;
	private static final short BITMAP_AMENITY = 4;
	private static final short BITMAP_NATURAL = 2;
	private static final short BITMAP_WATERWAY = 1;

	// bitmap flags for file features
	private static final short BITMAP_DEBUG = 128;
	private static final short BITMAP_MAP_START_POSITION = 64;

	private static final int BITMAP_INDEX_ENTRY_WATER = 0x80;

	private static final Logger logger = Logger.getLogger(MapFileWriter.class
			.getName());

	private static final String PROJECTION = "Mercator";

	private static final byte MAX_ZOOMLEVEL_PIXEL_FILTER = 11;

	private static final byte MIN_ZOOMLEVEL_POLYGON_CLIPPING = 8;

	private static final Charset UTF8_CHARSET = Charset.forName("utf8");

	// data
	private TileBasedDataStore dataStore;

	private static final TileInfo tileInfo = TileInfo.getInstance();
	// private static final CoastlineHandler COASTLINE_HANDLER = new CoastlineHandler();

	// IO
	private static final int HEADER_BUFFER_SIZE = 0x100000; // 1MB
	private static final int MIN_TILE_BUFFER_SIZE = 0xF00000; // 15MB
	private static final int TILE_BUFFER_SIZE = 0x3200000; // 50MB
	private static final int WAY_BUFFER_SIZE = 0x100000; // 1MB
	private static final int POI_BUFFER_SIZE = 0x100000; // 1MB
	private final RandomAccessFile randomAccessFile;
	private ByteBuffer bufferZoomIntervalConfig;

	// concurrent computation of subtile bitmask
	private final ExecutorService executorService;

	// accounting
	private long tilesProcessed = 0;
	private long fivePercentOfTilesToProcess;
	private long emptyTiles = 0;
	private long maxTileSize = 0;
	private long cumulatedTileSizeOfNonEmptyTiles = 0;
	private int maxWaysPerTile = 0;
	private int cumulatedNumberOfWaysInTiles = 0;

	private int posZoomIntervalConfig;
	final int bboxEnlargement;

	MapFileWriter(TileBasedDataStore dataStore, RandomAccessFile file,
			int threadpoolSize, int bboxEnlargement) {
		super();
		this.dataStore = dataStore;
		this.randomAccessFile = file;
		fivePercentOfTilesToProcess = dataStore.cumulatedNumberOfTiles() / 20;
		if (fivePercentOfTilesToProcess == 0)
			fivePercentOfTilesToProcess = 1;
		executorService = Executors.newFixedThreadPool(threadpoolSize);
		this.bboxEnlargement = bboxEnlargement;
	}

	final void writeFileWithDebugInfos(long date, int version, short tilePixel)
			throws IOException {
		writeFile(date, version, tilePixel, null, true, true, true, true, null);
	}

	final void writeFile(long date, int version, short tilePixel) throws IOException {
		writeFile(date, version, tilePixel, null, false, true, true, true, null);
	}

	final void writeFile() throws IOException {
		writeFile(System.currentTimeMillis(), 1, (short) 256, null, false, true, true, true,
				null);
	}

	final void writeFile(GeoCoordinate mapStartPosition) throws IOException {
		writeFile(System.currentTimeMillis(), 1, (short) 256, null, false, true, true, true,
				mapStartPosition);
	}

	final void writeFile(long date, int version, short tilePixel, String comment,
			boolean debugStrings, boolean waynodeCompression, boolean polygonClipping,
			boolean pixelCompression, GeoCoordinate mapStartPosition)
			throws IOException {

		// CONTAINER HEADER
		long totalHeaderSize = writeContainerHeader(date, version, tilePixel,
				comment, debugStrings, mapStartPosition);

		int n_zoom_intervals = dataStore.getZoomIntervalConfiguration()
				.getNumberOfZoomIntervals();

		// SUB FILES
		// for each zoom interval write a sub file
		long currentFileSize = totalHeaderSize;
		for (int i = 0; i < n_zoom_intervals; i++) {
			// SUB FILE INDEX AND DATA
			long subfileSize = writeSubfile(currentFileSize, i, debugStrings,
					waynodeCompression, polygonClipping, pixelCompression);
			// SUB FILE META DATA IN CONTAINER HEADER
			writeSubfileMetaDataToContainerHeader(i, currentFileSize, subfileSize);
			currentFileSize += subfileSize;
		}

		randomAccessFile.seek(posZoomIntervalConfig);
		byte[] containerB = bufferZoomIntervalConfig.array();
		randomAccessFile.write(containerB);
		randomAccessFile.close();

		logger.fine("number of empty tiles: " + emptyTiles);
		logger.fine("percentage of empty tiles: " + (float) emptyTiles
				/ dataStore.cumulatedNumberOfTiles());
		logger.fine("cumulated size of non-empty tiles: " + cumulatedTileSizeOfNonEmptyTiles);
		logger.fine("average tile size of non-empty tile: "
				+ (float) cumulatedTileSizeOfNonEmptyTiles
				/ (dataStore.cumulatedNumberOfTiles() - emptyTiles));
		logger.fine("maximum size of a tile: " + maxTileSize);
		logger.fine("cumulated number of ways in all non-empty tiles: "
				+ cumulatedNumberOfWaysInTiles);
		logger.fine("maximum number of ways in a tile: " + maxWaysPerTile);
		logger.fine("average number of ways in non-empty tiles: "
				+ (float) cumulatedNumberOfWaysInTiles
				/ (dataStore.cumulatedNumberOfTiles() - emptyTiles));

	}

	// private void writeByteArray(int pos, byte[] array, ByteBuffer buffer) {
	// int currentPos = buffer.position();
	// buffer.position(pos);
	// buffer.put(array);
	// buffer.position(currentPos);
	// }

	private void writeUTF8(String string, ByteBuffer buffer) {
		buffer.put(Serializer.getVariableByteUnsigned(string.getBytes(UTF8_CHARSET).length));
		buffer.put(string.getBytes(UTF8_CHARSET));
	}

	private long writeContainerHeader(long date, int version, short tilePixel, String comment,
			boolean debugStrings, GeoCoordinate mapStartPosition)
			throws IOException {

		// get metadata for the map file
		int numberOfZoomIntervals = dataStore.getZoomIntervalConfiguration()
				.getNumberOfZoomIntervals();

		logger.fine("writing header");

		ByteBuffer containerHeaderBuffer = ByteBuffer.allocate(HEADER_BUFFER_SIZE);

		// write file header
		// magic byte
		byte[] magicBytes = MAGIC_BYTE.getBytes();
		containerHeaderBuffer.put(magicBytes);

		// write container header size
		int headerSizePosition = containerHeaderBuffer.position();
		containerHeaderBuffer.position(headerSizePosition + 4);

		// version number of the binary file format
		containerHeaderBuffer.putInt(version);

		// meta info byte
		containerHeaderBuffer.put(infoByteOptmizationParams(debugStrings,
				mapStartPosition != null));

		// amount of map files inside this file
		containerHeaderBuffer.put((byte) numberOfZoomIntervals);

		// projection type
		writeUTF8(PROJECTION, containerHeaderBuffer);

		// width and height of a tile in pixel
		containerHeaderBuffer.putShort(tilePixel);

		logger.fine("Bounding box for file: " +
				dataStore.getBoundingBox().maxLatitudeE6 + ", " +
				dataStore.getBoundingBox().minLongitudeE6 + ", " +
				dataStore.getBoundingBox().minLatitudeE6 + ", " +
				dataStore.getBoundingBox().maxLongitudeE6);
		// upper left corner of the bounding box
		containerHeaderBuffer.putInt(dataStore.getBoundingBox().maxLatitudeE6);
		containerHeaderBuffer.putInt(dataStore.getBoundingBox().minLongitudeE6);
		containerHeaderBuffer.putInt(dataStore.getBoundingBox().minLatitudeE6);
		containerHeaderBuffer.putInt(dataStore.getBoundingBox().maxLongitudeE6);

		if (mapStartPosition != null) {
			containerHeaderBuffer.putInt(mapStartPosition.getLatitudeE6());
			containerHeaderBuffer.putInt(mapStartPosition.getLongitudeE6());
		}

		// date of the map data
		containerHeaderBuffer.putLong(date);

		// store the mapping of tags to tag ids
		containerHeaderBuffer.putShort((short) PoiEnum.values().length);
		for (PoiEnum poiEnum : PoiEnum.values()) {
			writeUTF8(poiEnum.toString(), containerHeaderBuffer);
			containerHeaderBuffer.putShort((short) poiEnum.ordinal());
		}
		containerHeaderBuffer.putShort((short) WayEnum.values().length);
		for (WayEnum wayEnum : WayEnum.values()) {
			writeUTF8(wayEnum.toString(), containerHeaderBuffer);
			containerHeaderBuffer.putShort((short) wayEnum.ordinal());
		}

		// comment
		if (comment != null && !comment.equals("")) {
			writeUTF8(comment, containerHeaderBuffer);
		} else {
			writeUTF8("", containerHeaderBuffer);
		}

		// initialize buffer for writing zoom interval configurations
		this.posZoomIntervalConfig = containerHeaderBuffer.position();
		bufferZoomIntervalConfig = ByteBuffer.allocate(SIZE_ZOOMINTERVAL_CONFIGURATION
				* numberOfZoomIntervals);

		containerHeaderBuffer.position(containerHeaderBuffer.position()
				+ SIZE_ZOOMINTERVAL_CONFIGURATION
				* numberOfZoomIntervals);

		// -4 bytes of header size variable itself
		int headerSize = containerHeaderBuffer.position() - headerSizePosition - 4;
		containerHeaderBuffer.putInt(headerSizePosition, headerSize);

		if (!containerHeaderBuffer.hasArray()) {
			randomAccessFile.close();
			throw new RuntimeException(
					"unsupported operating system, byte buffer not backed by array");
		}
		randomAccessFile.write(containerHeaderBuffer.array(), 0,
				containerHeaderBuffer.position());

		return containerHeaderBuffer.position();
	}

	private void writeSubfileMetaDataToContainerHeader(int i, long startIndexOfSubfile,
			long subfileSize) {

		// HEADER META DATA FOR SUB FILE
		// write zoom interval configuration to header
		byte minZoomCurrentInterval = dataStore.getZoomIntervalConfiguration().getMinZoom(i);
		byte maxZoomCurrentInterval = dataStore.getZoomIntervalConfiguration().getMaxZoom(i);
		byte baseZoomCurrentInterval = dataStore.getZoomIntervalConfiguration().getBaseZoom(i);

		bufferZoomIntervalConfig.put(baseZoomCurrentInterval);
		bufferZoomIntervalConfig.put(minZoomCurrentInterval);
		bufferZoomIntervalConfig.put(maxZoomCurrentInterval);
		bufferZoomIntervalConfig.put(Serializer.getFiveBytes(startIndexOfSubfile));
		bufferZoomIntervalConfig.put(Serializer.getFiveBytes(subfileSize));
	}

	private long writeSubfile(long startPositionSubfile, int zoomIntervalIndex,
			boolean debugStrings, boolean waynodeCompression, boolean polygonClipping,
			boolean pixelCompression)
			throws IOException {

		logger.fine("writing data for zoom interval " + zoomIntervalIndex
				+ ", number of tiles: " +
				dataStore.getTileGridLayout(zoomIntervalIndex).getAmountTilesHorizontal()
				* dataStore.getTileGridLayout(zoomIntervalIndex).getAmountTilesVertical());

		TileCoordinate upperLeft = dataStore.getTileGridLayout(zoomIntervalIndex)
				.getUpperLeft();
		int lengthX = dataStore.getTileGridLayout(zoomIntervalIndex).getAmountTilesHorizontal();
		int lengthY = dataStore.getTileGridLayout(zoomIntervalIndex).getAmountTilesVertical();

		byte minZoomCurrentInterval = dataStore.getZoomIntervalConfiguration().getMinZoom(
				zoomIntervalIndex);
		byte maxZoomCurrentInterval = dataStore.getZoomIntervalConfiguration().getMaxZoom(
				zoomIntervalIndex);
		byte baseZoomCurrentInterval = dataStore.getZoomIntervalConfiguration().getBaseZoom(
				zoomIntervalIndex);
		// byte maxMaxZoomlevel = dataStore.getZoomIntervalConfiguration().getMaxMaxZoom();

		int tileAmountInBytes = lengthX * lengthY * BYTE_AMOUNT_SUBFILE_INDEX_PER_TILE;
		int indexBufferSize = tileAmountInBytes
				+ (debugStrings ? DEBUG_INDEX_START_STRING.getBytes().length : 0);
		ByteBuffer indexBuffer = ByteBuffer.allocate(indexBufferSize);
		ByteBuffer tileBuffer = ByteBuffer.allocate(TILE_BUFFER_SIZE);
		ByteBuffer wayBuffer = ByteBuffer.allocate(WAY_BUFFER_SIZE);
		ByteBuffer poiBuffer = ByteBuffer.allocate(POI_BUFFER_SIZE);

		// write debug strings for tile index segment if necessary
		if (debugStrings)
			indexBuffer.put(DEBUG_INDEX_START_STRING.getBytes());

		long currentSubfileOffset = indexBufferSize;
		randomAccessFile.seek(startPositionSubfile + indexBufferSize);

		// loop over tiles (row-wise)
		for (int tileY = upperLeft.getY(); tileY < upperLeft.getY() + lengthY; tileY++) {
			for (int tileX = upperLeft.getX(); tileX < upperLeft.getX() + lengthX; tileX++) {
				// logger.info("writing data for tile (" + tileX + ", " + tileY + ")");

				// ***************** TILE INDEX SEGMENT ********************
				long currentTileOffsetInBuffer = tileBuffer.position();
				TileCoordinate currentTileCoordinate = new TileCoordinate(tileX, tileY,
						baseZoomCurrentInterval);
				int currentTileLat = GeoCoordinate.doubleToInt(MercatorProjection
						.tileYToLatitude(currentTileCoordinate.getY(),
								currentTileCoordinate.getZoomlevel()));
				int currentTileLon = GeoCoordinate.doubleToInt(MercatorProjection
						.tileXToLongitude(currentTileCoordinate.getX(),
								currentTileCoordinate.getZoomlevel()));

				byte[] indexBytes = Serializer.getFiveBytes(currentSubfileOffset);
				if (tileInfo.isWaterTile(currentTileCoordinate)) {
					indexBytes[0] |= BITMAP_INDEX_ENTRY_WATER;
				}
				// else {
				// // the TileInfo class may produce false negatives for tiles on zoom level
				// // greater than TileInfo.TILE_INFO_ZOOMLEVEL
				// // we need to run the coastline algorithm to detect whether the tile is
				// // completely covered by water or not
				// if (currentTileCoordinate.getZoomlevel() > TileInfo.TILE_INFO_ZOOMLEVEL) {
				// if (COASTLINE_HANDLER.isWaterTile(currentTileCoordinate,
				// dataStore.getCoastLines(currentTileCoordinate))) {
				// indexBytes[0] |= BITMAP_INDEX_ENTRY_WATER;
				// }
				// }
				// }

				// seek to index frame of this tile and write relative offset of this
				// tile as five bytes to the index
				indexBuffer.put(indexBytes);

				// ***************** TILE DATA SEGMENT ********************
				// get statistics for tile
				TileData currentTile = dataStore.getTile(zoomIntervalIndex, tileX, tileY);

				// TODO we need to rethink the semantic of zoom levels
				// ***************** POIs ********************
				// write amount of POIs and ways for each zoom level
				Map<Byte, List<TDNode>> poisByZoomlevel = currentTile
						.poisByZoomlevel(minZoomCurrentInterval, maxZoomCurrentInterval);
				Map<Byte, List<TDWay>> waysByZoomlevel = currentTile
						.waysByZoomlevel(minZoomCurrentInterval, maxZoomCurrentInterval);

				if (poisByZoomlevel.size() > 0 || waysByZoomlevel.size() > 0) {
					if (debugStrings) {
						// write tile header
						StringBuilder sb = new StringBuilder();
						sb.append(DEBUG_STRING_TILE_HEAD).append(tileX).append(",")
								.append(tileY)
								.append(DEBUG_STRING_TILE_TAIL);
						tileBuffer.put(sb.toString().getBytes());
						// append withespaces so that block has 32 bytes
						appendWhitespace(32 - sb.toString().getBytes().length, tileBuffer);
					}

					short cumulatedPOIs = 0;
					short cumulatedWays = 0;
					for (byte zoomlevel = minZoomCurrentInterval; zoomlevel <= maxZoomCurrentInterval; zoomlevel++) {
						if (poisByZoomlevel.get(zoomlevel) != null)
							cumulatedPOIs += poisByZoomlevel.get(zoomlevel).size();
						if (waysByZoomlevel.get(zoomlevel) != null)
							cumulatedWays += waysByZoomlevel.get(zoomlevel).size();
						tileBuffer.putShort(cumulatedPOIs);
						tileBuffer.putShort(cumulatedWays);
					}

					if (maxWaysPerTile < cumulatedWays)
						maxWaysPerTile = cumulatedWays;
					cumulatedNumberOfWaysInTiles += cumulatedWays;

					// clear poi buffer
					poiBuffer.clear();

					// write POIs for each zoom level beginning with lowest zoom level
					for (byte zoomlevel = minZoomCurrentInterval; zoomlevel <= maxZoomCurrentInterval; zoomlevel++) {
						List<TDNode> pois = poisByZoomlevel.get(zoomlevel);
						if (pois == null)
							continue;
						for (TDNode poi : pois) {
							if (debugStrings) {
								StringBuilder sb = new StringBuilder();
								sb.append(DEBUG_STRING_POI_HEAD).append(poi.getId())
										.append(DEBUG_STRING_POI_TAIL);
								poiBuffer.put(sb.toString().getBytes());
								// append withespaces so that block has 32 bytes
								appendWhitespace(32 - sb.toString().getBytes().length,
										poiBuffer);
							}

							// write poi features to the file
							poiBuffer.put(Serializer.getVariableByteSigned(poi.getLatitude()
									- currentTileLat));
							poiBuffer.put(Serializer.getVariableByteSigned(poi.getLongitude()
									- currentTileLon));

							// write byte with layer and tag amount info
							poiBuffer.put(infoByteLayerAndTagAmount(poi.getLayer(),
									poi.getTags() == null ? 0 : (short) poi.getTags().size()));

							// write tag ids to the file
							if (poi.getTags() != null) {
								for (PoiEnum poiEnum : poi.getTags()) {
									poiBuffer.put(Serializer.getVariableByteUnsigned(poiEnum
											.ordinal()));
								}
							}

							// write byte with bits set to 1 if the poi has a name, an elevation
							// or a housenumber
							poiBuffer.put(infoBytePOI(poi.getName(),
									poi.getElevation(),
									poi.getHouseNumber()));

							if (poi.getName() != null && poi.getName().length() > 0) {
								writeUTF8(poi.getName(), poiBuffer);

							}
							if (poi.getElevation() != 0) {
								poiBuffer.put(Serializer.getVariableByteSigned(poi
										.getElevation()));
							}
							if (poi.getHouseNumber() != null
									&& poi.getHouseNumber().length() > 0) {
								writeUTF8(poi.getHouseNumber(), poiBuffer);
							}
						}
					}// end for loop over POIs

					// write offset to first way in the tile header
					tileBuffer.put(Serializer.getVariableByteUnsigned(poiBuffer.position()));
					tileBuffer.put(poiBuffer.array(), 0, poiBuffer.position());

					// ***************** WAYS ********************
					// loop over all relevant zoom levels
					for (byte zoomlevel = minZoomCurrentInterval; zoomlevel <= maxZoomCurrentInterval; zoomlevel++) {
						List<TDWay> ways = waysByZoomlevel.get(zoomlevel);
						if (ways == null)
							continue;

						// use executor service to parallelize computation of subtile bitmasks
						// for all
						// ways in the current tile
						short[] bitmaskComputationResults = computeSubtileBitmasks(ways,
								currentTileCoordinate);
						assert bitmaskComputationResults.length == ways.size();
						// needed to access bitmask computation results in the foreach loop
						int i = 0;
						for (TDWay way : ways) {
							wayBuffer.clear();

							WayNodePreprocessingResult wayNodePreprocessingResult = preprocessWayNodes(
									way, waynodeCompression, pixelCompression, polygonClipping,
									maxZoomCurrentInterval, minZoomCurrentInterval,
									currentTileCoordinate);

							if (wayNodePreprocessingResult == null) {
								continue;
							}
							if (debugStrings) {
								StringBuilder sb = new StringBuilder();
								sb.append(DEBUG_STRING_WAY_HEAD).append(way.getId())
										.append(DEBUG_STRING_WAY_TAIL);
								tileBuffer.put(sb.toString().getBytes());
								// append withespaces so that block has 32 bytes
								appendWhitespace(32 - sb.toString().getBytes().length,
										tileBuffer);
							}

							// write way features
							wayBuffer.putShort(bitmaskComputationResults[i++]);

							// write byte with layer and tag amount
							wayBuffer.put(infoByteLayerAndTagAmount(way.getLayer(),
									way.getTags() == null ? 0 : (short) way.getTags().size()));

							// write byte with amount of tags which are rendered
							wayBuffer.put(infoByteWayAmountRenderedTags(
									way.getTags()));

							// write tag bitmap
							wayBuffer.put(infoByteTagBitmask(way.getTags()));

							// write tag ids
							if (way.getTags() != null) {
								for (WayEnum wayEnum : way.getTags()) {
									wayBuffer.put(Serializer.getVariableByteUnsigned(wayEnum
											.ordinal()));
								}
							}
							// write the amount of way nodes to the file
							wayBuffer
									.put(Serializer
											.getVariableByteUnsigned(wayNodePreprocessingResult
													.getCoordinates()
													.size() / 2));

							// write the way nodes:
							// the first node is always stored with four bytes
							// the remaining way node differences are stored according to the
							// compression type
							writeWayNodes(wayNodePreprocessingResult.getCoordinates(),
									currentTileLat,
									currentTileLon, wayBuffer);

							// write a byte with name, label and way type information
							wayBuffer.put(infoByteWay(way.getName(), way.getRef(),
										wayNodePreprocessingResult.getLabelPosition() != null,
									// false,
									way.getWaytype()
									));

							// // if the way has a name, write it to the file
							if (way.getName() != null && way.getName().length() > 0) {
								writeUTF8(way.getName(), wayBuffer);
							}

							// if the way has a ref, write it to the file
							if (way.getRef() != null && way.getRef().length() > 0) {
								writeUTF8(way.getRef(), wayBuffer);
							}

							if (wayNodePreprocessingResult.getLabelPosition() != null) {
								// if (way.getId() == 36056459)
								// System.out.println("rewe");
								// logger.info("writing label position: "
								// + wayNodePreprocessingResult.getLabelPosition());
								wayBuffer.put(Serializer
										.getVariableByteSigned(wayNodePreprocessingResult
												.getLabelPosition().getLatitudeE6()
												- wayNodePreprocessingResult.getCoordinates()
														.get(0)));
								wayBuffer.put(Serializer
										.getVariableByteSigned(wayNodePreprocessingResult
												.getLabelPosition().getLongitudeE6()
												- wayNodePreprocessingResult.getCoordinates()
														.get(1)));
							}

							// ***************** MULTIPOLYGONS WITH INNER WAYS ***************
							if (way.getWaytype() == 3) {
								List<TDWay> innerways = dataStore
										.getInnerWaysOfMultipolygon(way.getId());
								if (innerways != null && innerways.size() > 0) {

									wayBuffer.put(Serializer.getVariableByteUnsigned(innerways
											.size()));
									for (TDWay innerway : innerways) {
										WayNodePreprocessingResult innerWayAsList =
													preprocessWayNodes(innerway,
															waynodeCompression,
															pixelCompression,
															false,
															maxZoomCurrentInterval,
															minZoomCurrentInterval,
															currentTileCoordinate);
										// write the amount of way nodes to the file
										wayBuffer
												.put(Serializer
														.getVariableByteUnsigned(innerWayAsList
																.getCoordinates()
																.size() / 2));
										writeInnerWayNodes(wayNodePreprocessingResult
												.getCoordinates().get(0),
												wayNodePreprocessingResult.getCoordinates()
														.get(1),
												innerWayAsList.getCoordinates(), wayBuffer);
									}
								}
							}
							tileBuffer.put(Serializer.getVariableByteUnsigned(wayBuffer
									.position()));
							tileBuffer.put(wayBuffer.array(), 0, wayBuffer.position());
						}
					}// end for loop over ways
				}// end if clause checking if tile is empty or not
				else {
					emptyTiles++;
				}
				long tileSize = tileBuffer.position() - currentTileOffsetInBuffer;
				currentSubfileOffset += tileSize;

				// accounting
				if (maxTileSize < tileSize)
					maxTileSize = tileSize;
				if (tileSize > 0)
					cumulatedTileSizeOfNonEmptyTiles += tileSize;

				// if necessary, allocate new buffer
				if (tileBuffer.remaining() < MIN_TILE_BUFFER_SIZE) {
					randomAccessFile.write(tileBuffer.array(), 0, tileBuffer.position());
					tileBuffer.clear();
				}

				tilesProcessed++;
				if (tilesProcessed % fivePercentOfTilesToProcess == 0) {
					logger.info("written " + (tilesProcessed / fivePercentOfTilesToProcess)
							* 5
							+ "% of file");
				}
			}// end for loop over tile columns
		}// /end for loop over tile rows

		// write remaining tiles
		if (tileBuffer.position() > 0) {
			// byte buffer was not previously cleared
			randomAccessFile.write(tileBuffer.array(), 0, tileBuffer.position());
		}

		// write index
		randomAccessFile.seek(startPositionSubfile);
		randomAccessFile.write(indexBuffer.array());
		randomAccessFile.seek(currentSubfileOffset);

		// return size of sub file in bytes
		return currentSubfileOffset;
	}// end writeSubfile()

	private void appendWhitespace(int amount, ByteBuffer buffer) {
		for (int i = 0; i < amount; i++) {
			buffer.put((byte) ' ');
		}
	}

	private List<Integer> waynodesAsList(List<GeoCoordinate> waynodeCoordinates) {
		ArrayList<Integer> result = new ArrayList<Integer>();
		for (GeoCoordinate geoCoordinate : waynodeCoordinates) {
			result.add(geoCoordinate.getLatitudeE6());
			result.add(geoCoordinate.getLongitudeE6());
		}

		return result;
	}

	private WayNodePreprocessingResult preprocessWayNodes(TDWay way,
			boolean waynodeCompression,
			boolean pixelCompression, boolean polygonClipping, byte maxZoomCurrentInterval,
			byte minZoomCurrentInterval,
			TileCoordinate tile) {
		List<GeoCoordinate> waynodeCoordinates = way.wayNodesAsCoordinateList();
		GeoCoordinate polygonCentroid = null;

		// if the sub file for lower zoom levels is written, remove all way
		// nodes from the list which are projected on the same pixel
		if (pixelCompression && maxZoomCurrentInterval <= MAX_ZOOMLEVEL_PIXEL_FILTER) {
			waynodeCoordinates = GeoUtils.filterWaynodesOnSamePixel(
					waynodeCoordinates,
					maxZoomCurrentInterval, PIXEL_COMPRESSION_MAX_DELTA);
		}

		// if the way is a multipolygon without a name, clip the way to the
		// tile
		if (polygonClipping && minZoomCurrentInterval >= MIN_ZOOMLEVEL_POLYGON_CLIPPING) {
			// if (false && way.getWaytype() == 1 && waynodeCoordinates.size() >= 2) {
			// List<GeoCoordinate> clipped = GeoUtils.clipLineToTile(waynodeCoordinates, tile,
			// bboxEnlargement);
			// if (clipped != null && !clipped.isEmpty())
			// waynodeCoordinates = clipped;
			// else
			// return null;
			// }
			if (way.getWaytype() >= 2 && waynodeCoordinates.size() >= 4
					// TODO activate polygon clipping of named polygons
					&& (way.getName() == null || way.getName().equals(""))) {
				List<GeoCoordinate> clipped = GeoUtils.clipPolygonToTile(
						waynodeCoordinates, tile, bboxEnlargement);
				if (clipped != null && !clipped.isEmpty()) {
					waynodeCoordinates = clipped;
					// TODO activate polygon clipping of named polygons
					// if (way.getName() != null && way.getName().length() > 0) {
					// polygonCentroid = GeoUtils.computePolygonCentroid(waynodeCoordinates);
					// if (polygonCentroid != null) {
					// logger.info("centroid for way '" + way.getName() + "' ("
					// + way.getId() + ") is: " + polygonCentroid);
					// }
					// }
					// if (polygonCentroid != null
					// && !GeoUtils.pointInTile(polygonCentroid,
					// tile))
					// polygonCentroid = null;
				} else
					return null;
			}
		}

		// if the wayNodeCompression flag is set, compress the way nodes
		// with a minimal amount of bytes
		List<Integer> waynodesAsList = null;
		if (waynodeCompression) {
			waynodesAsList = GeoUtils.waynodeAbsoluteCoordinatesToOffsets(waynodeCoordinates);
		} else {
			waynodesAsList = waynodesAsList(waynodeCoordinates);
		}

		return new WayNodePreprocessingResult(waynodesAsList, polygonCentroid);
	}

	private byte infoByteLayerAndTagAmount(byte layer, short tagAmount) {
		// make sure layer is in [0,10]
		return (byte) ((layer < 0 ? 0 : layer > 10 ? 10 : layer) << 4 | tagAmount);
	}

	private byte infoByteOptmizationParams(boolean debug, boolean mapStartPosition) {
		byte infoByte = 0;

		if (debug)
			infoByte |= BITMAP_DEBUG;
		if (mapStartPosition)
			infoByte |= BITMAP_MAP_START_POSITION;

		return infoByte;
	}

	private byte infoBytePOI(String name, int elevation, String housenumber) {
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

	private byte infoByteWay(String name, String ref, boolean labelPosition, int wayType) {
		byte infoByte = 0;

		if (name != null && name.length() > 0) {
			infoByte |= BITMAP_NAME;
		}
		if (ref != null && ref.length() > 0) {
			infoByte |= BITMAP_REF;
		}
		if (labelPosition) {
			infoByte |= BITMAP_LABEL;
		}
		if (wayType == 3) {
			infoByte |= BITMAP_MULTIPOLYGON;
		}

		return infoByte;
	}

	private byte infoByteWayAmountRenderedTags(EnumSet<WayEnum> tags) {

		byte infoByte = 0;
		short counter = 0;
		if (tags != null) {
			for (WayEnum wayEnum : tags) {
				if (wayEnum.associatedWithValidZoomlevel())
					counter++;
			}
			infoByte = (byte) (counter << 5);
		}

		return infoByte;
	}

	private byte infoByteTagBitmask(EnumSet<WayEnum> tags) {
		if (tags == null)
			return 0;
		byte infoByte = 0;

		for (WayEnum wayEnum : tags) {
			switch (wayEnum.waytype()) {
				case HIGHWAY:
					infoByte |= BITMAP_HIGHWAY;
					break;
				case RAILWAY:
					infoByte |= BITMAP_RAILWAY;
					break;
				case BUILDING:
					infoByte |= BITMAP_BUILDING;
					break;
				case LANDUSE:
					infoByte |= BITMAP_LANDUSE;
					break;
				case LEISURE:
					infoByte |= BITMAP_LEISURE;
					break;
				case AMENITY:
					infoByte |= BITMAP_AMENITY;
					break;
				case NATURAL:
					infoByte |= BITMAP_NATURAL;
					break;
				case WATERWAY:
					infoByte |= BITMAP_WATERWAY;
					break;
				case UNCLASSIFIED:
					break;
			}
		}
		return infoByte;
	}

	private void writeWayNodes(List<Integer> waynodes, int currentTileLat, int currentTileLon,
			ByteBuffer buffer) {
		if (!waynodes.isEmpty()
				&& waynodes.size() % 2 == 0) {
			Iterator<Integer> waynodeIterator = waynodes.iterator();
			buffer.put(Serializer.getVariableByteSigned(waynodeIterator.next() - currentTileLat));
			buffer.put(Serializer.getVariableByteSigned(waynodeIterator.next() - currentTileLon));

			while (waynodeIterator.hasNext()) {
				buffer.put(Serializer.getVariableByteSigned(waynodeIterator.next()));
			}
		}
	}

	private void writeInnerWayNodes(int latRef, int lonRef, List<Integer> innerWay,
			ByteBuffer buffer) {

		if (!innerWay.isEmpty()
				&& innerWay.size() % 2 == 0) {
			Iterator<Integer> waynodeIterator = innerWay.iterator();
			buffer.put(Serializer.getVariableByteSigned(waynodeIterator.next() - latRef));
			buffer.put(Serializer.getVariableByteSigned(waynodeIterator.next() - lonRef));

			while (waynodeIterator.hasNext()) {
				buffer.put(Serializer.getVariableByteSigned(waynodeIterator.next()));
			}
		}
	}

	private short[] computeSubtileBitmasks(List<TDWay> ways,
			TileCoordinate currentTileCoordinate) {
		short[] bitmaskComputationResults = new short[ways.size()];
		Collection<TileBitmaskComputationTask> tasks = new ArrayList<MapFileWriter.TileBitmaskComputationTask>();
		for (TDWay tdWay : ways) {
			tasks.add(new TileBitmaskComputationTask(currentTileCoordinate,
					tdWay));
		}

		try {
			List<Future<Short>> results = executorService.invokeAll(tasks);
			int i = 0;
			for (Future<Short> future : results) {
				bitmaskComputationResults[i++] = future.get().shortValue();
			}
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} catch (ExecutionException e) {
			throw new RuntimeException(e);
		}

		return bitmaskComputationResults;
	}

	private class WayNodePreprocessingResult {

		final List<Integer> coordinates;
		final GeoCoordinate labelPosition;

		public WayNodePreprocessingResult(List<Integer> coordinates, GeoCoordinate labelPosition) {
			super();
			this.coordinates = coordinates;
			this.labelPosition = labelPosition;
		}

		public List<Integer> getCoordinates() {
			return coordinates;
		}

		public GeoCoordinate getLabelPosition() {
			return labelPosition;
		}

	}

	private class TileBitmaskComputationTask implements Callable<Short> {

		private static final short COASTLINE_BITMASK = (short) 0xFFFF;
		private final TileCoordinate baseTile;
		private final TDWay way;

		TileBitmaskComputationTask(TileCoordinate baseTile, TDWay way) {
			super();
			this.baseTile = baseTile;
			this.way = way;
		}

		@Override
		public Short call() {
			if (way.getTags() != null && way.getTags().contains(WayEnum.NATURAL$COASTLINE)) {
				return COASTLINE_BITMASK;
			}
			return GeoUtils.computeBitmask(way, baseTile, bboxEnlargement);
		}

	}
}