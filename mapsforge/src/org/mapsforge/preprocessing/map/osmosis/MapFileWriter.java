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
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.mapsforge.core.GeoCoordinate;
import org.mapsforge.preprocessing.map.osmosis.TileData.TDNode;
import org.mapsforge.preprocessing.map.osmosis.TileData.TDWay;

class MapFileWriter {

	private static final int SIZE_ZOOMINTERVAL_CONFIGURATION = 13;

	private static final int PIXEL_COMPRESSION_MAX_DELTA = 3;

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
	// private static final short BITMAP_LABEL = 32;
	private static final short BITMAP_MULTIPOLYGON = 16;
	private static final short BITMAP_WAYNODECOMPRESSION_4_BYTE = 0;
	private static final short BITMAP_WAYNODECOMPRESSION_3_BYTE = 1;
	private static final short BITMAP_WAYNODECOMPRESSION_2_BYTE = 2;
	private static final short BITMAP_WAYNODECOMPRESSION_1_BYTE = 3;
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
	private static final short BITMAP_WAYNODE_FILTERING = 32;
	private static final short BITMAP_POLYGON_CLIPPING = 16;
	private static final short BITMAP_WAYNODE_COMPRESSION = 8;

	private static final Logger logger = Logger.getLogger(MapFileWriter.class.getName());

	private static final String PROJECTION = "Mercator";

	private static final byte MAX_ZOOMLEVEL_PIXEL_FILTER = 11;

	private static final byte MIN_ZOOMLEVEL_POLYGON_CLIPPING = 12;

	private TileBasedDataStore dataStore;

	private RandomAccessFile file;

	private long tilesProcessed = 0;
	private long fivePercentOfTilesToProcess;

	MapFileWriter(TileBasedDataStore dataStore, RandomAccessFile file) {
		super();
		this.dataStore = dataStore;
		this.file = file;
		fivePercentOfTilesToProcess = dataStore.cumulatedNumberOfTiles() / 20;
		if (fivePercentOfTilesToProcess == 0)
			fivePercentOfTilesToProcess = 1;
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
		long startIndexOfZoomIntervalConf = writeContainerHeader(date, version, tilePixel,
				comment, debugStrings, waynodeCompression, polygonClipping, pixelCompression,
				mapStartPosition);

		int n_zoom_intervals = dataStore.getZoomIntervalConfiguration()
				.getNumberOfZoomIntervals();

		// SUB FILES
		// for each zoom interval write a sub file
		for (int i = 0; i < n_zoom_intervals; i++) {
			long startIndexOfSubfile = file.getFilePointer();
			// SUB FILE INDEX AND DATA
			long subfileSize = writeSubfile(i, debugStrings,
					waynodeCompression, polygonClipping, pixelCompression);

			// SUB FILE META DATA IN CONTAINER HEADER
			writeSubfileMetaDataToContainerHeader(i, startIndexOfZoomIntervalConf,
					startIndexOfSubfile, subfileSize);
		}

		file.close();
	}

	private long writeContainerHeader(long date, int version, short tilePixel, String comment,
			boolean debugStrings, boolean waynodeCompression, boolean polygonClipping,
			boolean pixelCompression, GeoCoordinate mapStartPosition)
			throws IOException {

		// get metadata for the map file
		int numberOfZoomIntervals = dataStore.getZoomIntervalConfiguration()
				.getNumberOfZoomIntervals();

		logger.info("writing header");

		// write file header
		// magic byte
		byte[] magicBytes = MAGIC_BYTE.getBytes();
		file.write(magicBytes);

		// write container header size
		long headerSizePosition = file.getFilePointer();
		file.seek(headerSizePosition + 4);

		// version number of the binary file format
		file.writeInt(version);

		// meta info byte
		file.writeByte(buildMetaInfoByte(debugStrings, mapStartPosition != null,
				pixelCompression,
				polygonClipping, waynodeCompression));

		// amount of map files inside this file
		file.writeByte(numberOfZoomIntervals);

		// projection type
		file.writeUTF(PROJECTION);

		// width and height of a tile in pixel
		file.writeShort(tilePixel);

		logger.info("Bounding box for file: " +
				dataStore.getBoundingBox().maxLatitudeE6 + ", " +
				dataStore.getBoundingBox().minLongitudeE6 + ", " +
				dataStore.getBoundingBox().minLatitudeE6 + ", " +
				dataStore.getBoundingBox().maxLongitudeE6);
		// upper left corner of the bounding box
		file.writeInt(dataStore.getBoundingBox().maxLatitudeE6);
		file.writeInt(dataStore.getBoundingBox().minLongitudeE6);

		// bottom right corner of the bounding box
		file.writeInt(dataStore.getBoundingBox().minLatitudeE6);
		file.writeInt(dataStore.getBoundingBox().maxLongitudeE6);

		if (mapStartPosition != null) {
			file.writeInt(mapStartPosition.getLatitudeE6());
			file.writeInt(mapStartPosition.getLongitudeE6());
		}

		// date of the map data
		file.writeLong(date);

		// store the mapping of tags to tag ids
		file.writeShort(PoiEnum.values().length);
		for (PoiEnum poiEnum : PoiEnum.values()) {
			file.writeUTF(poiEnum.toString());
			file.writeShort(poiEnum.ordinal());
		}
		file.writeShort(WayEnum.values().length);
		for (WayEnum wayEnum : WayEnum.values()) {
			file.writeUTF(wayEnum.toString());
			file.writeShort(wayEnum.ordinal());
		}

		// comment
		if (comment != null && !comment.equals("")) {
			file.writeUTF(comment);
		} else {
			file.writeUTF("");
		}

		// reserve place for zoom interval configuration, 13 bytes for each zoom interval
		long startIndexOfZoomIntervalConf = file.getFilePointer();
		file.seek(startIndexOfZoomIntervalConf + numberOfZoomIntervals
				* SIZE_ZOOMINTERVAL_CONFIGURATION);

		long currentPosition = file.getFilePointer();
		int headerSize = (int) (currentPosition - headerSizePosition - 4);
		file.seek(headerSizePosition);
		file.writeInt(headerSize);
		file.seek(currentPosition);

		return startIndexOfZoomIntervalConf;
	}

	private void writeSubfileMetaDataToContainerHeader(int i,
			long startIndexOfZoomIntervalConf, long startIndexOfSubfile, long subfileSize)
			throws IOException {

		// HEADER META DATA FOR SUB FILE
		// write zoom interval configuration to header
		byte minZoomCurrentInterval = dataStore.getZoomIntervalConfiguration().getMinZoom(i);
		byte maxZoomCurrentInterval = dataStore.getZoomIntervalConfiguration().getMaxZoom(i);
		byte baseZoomCurrentInterval = dataStore.getZoomIntervalConfiguration().getBaseZoom(i);

		long startPosition = file.getFilePointer();
		file.seek(startIndexOfZoomIntervalConf + i * SIZE_ZOOMINTERVAL_CONFIGURATION);
		file.writeByte(baseZoomCurrentInterval);
		file.writeByte(minZoomCurrentInterval);
		file.writeByte(maxZoomCurrentInterval);
		file.write(Serializer.getFiveBytes(startIndexOfSubfile));
		file.write(Serializer.getFiveBytes(subfileSize));
		file.seek(startPosition);

	}

	private long writeSubfile(int zoomIntervalIndex,
			boolean debugStrings, boolean waynodeCompression, boolean polygonClipping,
			boolean pixelCompression)
			throws IOException {

		TileCoordinate upperLeft = dataStore.getUpperLeft(zoomIntervalIndex);
		int lengthX = dataStore.numberOfHorizontalTiles(zoomIntervalIndex);
		int lengthY = dataStore.numberOfVerticalTiles(zoomIntervalIndex);

		byte minZoomCurrentInterval = dataStore.getZoomIntervalConfiguration().getMinZoom(
				zoomIntervalIndex);
		byte maxZoomCurrentInterval = dataStore.getZoomIntervalConfiguration().getMaxZoom(
				zoomIntervalIndex);
		byte baseZoomCurrentInterval = dataStore.getZoomIntervalConfiguration().getBaseZoom(
				zoomIntervalIndex);
		byte maxMaxZoomlevel = dataStore.getZoomIntervalConfiguration().getMaxMaxZoom();

		// TILE INDEX FOR SUB FILE
		// remember the start of the sub file index
		long startIndexSubfile = file.getFilePointer();
		if (debugStrings) {
			file.write("+++IndexStart+++".getBytes());
		}

		long startIndexSubfileIndex = file.getFilePointer();
		long indexPointer = startIndexSubfileIndex;
		// compute the size of the index for the current sub file
		long tileAmountInBytes = dataStore.numberOfHorizontalTiles(zoomIntervalIndex)
				* dataStore.numberOfVerticalTiles(zoomIntervalIndex)
				* BYTE_AMOUNT_SUBFILE_INDEX_PER_TILE;
		logger.info("writing data for zoom interval " + zoomIntervalIndex
				+ ", number of tiles: " +
				dataStore.numberOfHorizontalTiles(zoomIntervalIndex)
				* dataStore.numberOfVerticalTiles(zoomIntervalIndex));

		long startIndexSubfileData = startIndexSubfileIndex + tileAmountInBytes;
		// jump to start of sub file data
		file.seek(startIndexSubfileData);

		for (int tileY = upperLeft.getY(); tileY < upperLeft.getY() + lengthY; tileY++) {
			for (int tileX = upperLeft.getX(); tileX < upperLeft.getX() + lengthX; tileX++) {
				// logger.info("writing data for tile (" + tileX + ", " + tileY + ")");

				TileCoordinate currentTileCoordinate = new TileCoordinate(tileX, tileY,
						baseZoomCurrentInterval);

				// seek to index frame of this tile and write relative offset of this
				// tile as five bytes to the index
				long startPositionCurrentTile = file.getFilePointer();
				file.seek(indexPointer);
				file.write(Serializer.getFiveBytes(startPositionCurrentTile
						- startIndexSubfile));
				indexPointer = file.getFilePointer();

				// TODO delete
				// // tile size bigger than current maximum tile size?
				// if (startPositionCurrentTile - startPositionPreviousTile > maxTileSize) {
				// maxTileSize = startPositionCurrentTile - startPositionPreviousTile;
				// biggestTileSizePosition = startPositionPreviousTile;
				// }

				// seek to start position of current tile
				file.seek(startPositionCurrentTile);

				// get statistics for tile
				TileData currentTile = dataStore.getTile(zoomIntervalIndex, tileX, tileY);

				// ************* POI ************
				// write amount of POIs and ways for each zoom level
				// TODO is this computation correct? Ways that have an associated zoom level of
				// e.g. 9
				// are lifted to zoom level 12 for an interval 12,14,17
				Map<Byte, List<TDNode>> poisByZoomlevel = currentTile
						.poisByZoomlevel(minZoomCurrentInterval, maxMaxZoomlevel);
				Map<Byte, List<TDWay>> waysByZoomlevel = currentTile
						.waysByZoomlevel(minZoomCurrentInterval, maxMaxZoomlevel);

				// TODO this verification in implementation dependent --> write explicit method
				if (poisByZoomlevel.size() > 0 || waysByZoomlevel.size() > 0) {
					long tileContainerStart = file.getFilePointer();
					if (debugStrings) {
						// write tile header
						StringBuilder sb = new StringBuilder();
						sb.append(DEBUG_STRING_TILE_HEAD).append(tileX).append(",")
								.append(tileY)
								.append(DEBUG_STRING_TILE_TAIL);
						file.write(sb.toString().getBytes());
						// append withespaces so that block has 32 bytes
						appendWhitespace(32 - sb.toString().getBytes().length);
					}

					short cumulatedPOIs = 0;
					short cumulatedWays = 0;
					for (byte zoomlevel = minZoomCurrentInterval; zoomlevel <= maxZoomCurrentInterval; zoomlevel++) {
						if (poisByZoomlevel.get(zoomlevel) != null)
							cumulatedPOIs += poisByZoomlevel.get(zoomlevel).size();
						if (waysByZoomlevel.get(zoomlevel) != null)
							cumulatedWays += waysByZoomlevel.get(zoomlevel).size();
						file.writeShort(cumulatedPOIs);
						file.writeShort(cumulatedWays);
					}

					// skip 4 bytes, later these 4 bytes will contain the start
					// position of the ways in this tile
					long fileIndexStartWayContainer = file.getFilePointer();
					file.seek(fileIndexStartWayContainer + 4);

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
								file.write(sb.toString().getBytes());
								// append withespaces so that block has 32 bytes
								appendWhitespace(32 - sb.toString().getBytes().length);
							}

							// write poi features to the file
							file.writeInt(poi.getLatitude());
							file.writeInt(poi.getLongitude());

							// write byte with layer and tag amount info
							file.writeByte(buildLayerTagAmountByte(poi.getLayer(),
									poi.getTags() == null ? 0 : (short) poi.getTags().size()));

							// write tag ids to the file
							if (poi.getTags() != null) {
								for (PoiEnum poiEnum : poi.getTags()) {
									file.writeShort(poiEnum.ordinal());
								}
							}

							// write byte with bits set to 1 if the poi has a name, an elevation
							// or a housenumber
							file.writeByte(buildInfoByteForPOI(poi.getName(),
									poi.getElevation(),
									poi.getHouseNumber()));

							if (poi.getName() != null && poi.getName().length() > 0) {
								file.writeUTF(poi.getName());
							}
							if (poi.getElevation() != 0) {
								file.writeShort(poi.getElevation());
							}
							if (poi.getHouseNumber() != null
									&& poi.getHouseNumber().length() > 0) {
								file.writeUTF(poi.getHouseNumber());
							}
						}
					}// end for loop over POIs

					// write offset to first way in the tile header
					long fileIndexCurrentPosition = file.getFilePointer();
					file.seek(fileIndexStartWayContainer);
					file.writeInt((int) (fileIndexCurrentPosition - tileContainerStart));
					file.seek(fileIndexCurrentPosition);

					// ************* WAYS ************
					// write ways
					for (byte zoomlevel = minZoomCurrentInterval; zoomlevel <= maxZoomCurrentInterval; zoomlevel++) {
						List<TDWay> ways = waysByZoomlevel.get(zoomlevel);
						if (ways == null)
							continue;

						for (TDWay way : ways) {
							// // INNER WAY
							// // inner ways will be written as part of the outer way
							// if (way.isInnerWay())
							// continue;
							long startIndexWay = file.getFilePointer();

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
								file.write(sb.toString().getBytes());
								// append withespaces so that block has 32 bytes
								appendWhitespace(32 - sb.toString().getBytes().length);
							}

							// skip 4 bytes to reserve space for way size
							long startIndexWaySize = file.getFilePointer();
							file.seek(startIndexWaySize + 4);

							// write way features
							short bitmask = GeoUtils.computeBitmask(way,
									currentTileCoordinate);
							// bitmask = (short) 0xffff;
							file.writeShort(bitmask);

							// write byte with layer and tag amount
							file.writeByte(buildLayerTagAmountByte(way.getLayer(),
									way.getTags() == null ? 0 : (short) way.getTags().size()));

							// set type of the way node compression
							int compressionType = wayNodePreprocessingResult
									.getCompressionType();

							// write byte with amount of tags which are rendered
							file.writeByte(buildRenderTagWayNodeCompressionByte(way.getTags(),
									compressionType));

							// write tag bitmap
							file.writeByte(buildTagBitmapByte(way.getTags()));
							// file.writeByte((byte) 0xff);

							// write tag ids
							if (way.getTags() != null) {
								for (WayEnum wayEnum : way.getTags()) {
									file.writeShort(wayEnum.ordinal());
								}
							}
							// write the amount of way nodes to the file
							// TODO improve
							file.writeShort(wayNodePreprocessingResult.getWaynodesAsList()
									.size() / 2);

							// write the way nodes:
							// the first node is always stored with four bytes
							// the remaining way node differences are stored according to the
							// compression type
							writeWayNodes(wayNodePreprocessingResult.getWaynodesAsList(),
									wayNodePreprocessingResult.getCompressionType());

							// write a byte with name, label and way type information
							file.writeByte(buildInfoByteForWay(way.getName(),
									way.getWaytype(),
									way.getRef()));

							// // if the way has a name, write it to the file
							if (way.getName() != null && way.getName().length() > 0) {
								file.writeUTF(way.getName());
							}

							// if the way has a ref, write it to the file
							if (way.getRef() != null && way.getRef().length() > 0) {
								file.writeUTF(way.getRef());
							}
							//
							// // // if the way has a label position write it to the file
							// // if (labelPositionLatitude != 0 && labelPositionLongitude != 0)
							// {
							// // raf.writeInt(labelPositionLatitude);
							// // raf.writeInt(labelPositionLongitude);
							// // }
							//
							// *********MULTIPOLYGON PROCESSING***********
							// TODO multipolygons must have inner ways?
							// TODO multipolygons which are closed ways are added twice to tile
							// data
							if (way.getWaytype() == 3 && dataStore
									.getInnerWaysOfMultipolygon(way.getId()) != null) {
								List<TDWay> innerways = dataStore
										.getInnerWaysOfMultipolygon(way.getId());

								if (innerways == null) {
									file.writeByte(0);
								} else {
									file.writeByte(innerways.size());
									for (TDWay innerway : innerways) {
										WayNodePreprocessingResult innerWayNodePreprocessingResult =
												preprocessWayNodes(innerway,
														waynodeCompression, pixelCompression,
														false,
														maxZoomCurrentInterval,
														minZoomCurrentInterval,
														currentTileCoordinate);
										// write the amount of way nodes to the file
										file.writeShort(innerWayNodePreprocessingResult
												.getWaynodesAsList()
												.size() / 2);
										writeWayNodes(
												innerWayNodePreprocessingResult
														.getWaynodesAsList(),
												wayNodePreprocessingResult.getCompressionType());
									}
								}
							}
							// write the size of the way to the file
							fileIndexCurrentPosition = file.getFilePointer();
							file.seek(startIndexWaySize);
							file.writeInt((int) (fileIndexCurrentPosition - startIndexWay));
							file.seek(fileIndexCurrentPosition);
						}
					}// end for loop over ways
				}// end if clause checking if tile is empty or not
				tilesProcessed++;
				if (tilesProcessed % fivePercentOfTilesToProcess == 0) {
					logger.info("written " + (tilesProcessed / fivePercentOfTilesToProcess)
							* 5
							+ "% of file");
				}
			}// end for loop over tile columns
		}// /end for loop over tile rows

		// return size of sub file in bytes
		return file.getFilePointer() - startIndexSubfile;
	}

	private List<Integer> waynodesAsList(List<GeoCoordinate> waynodeCoordinates) {
		ArrayList<Integer> result = new ArrayList<Integer>();
		for (GeoCoordinate geoCoordinate : waynodeCoordinates) {
			result.add(geoCoordinate.getLatitudeE6());
			result.add(geoCoordinate.getLongitudeE6());
		}

		return result;
	}

	private void appendWhitespace(int amount) throws IOException {
		for (int i = 0; i < amount; i++) {
			file.writeByte(32);
		}
	}

	private WayNodePreprocessingResult preprocessWayNodes(TDWay way,
			boolean waynodeCompression,
			boolean pixelCompression, boolean polygonClipping, byte maxZoomCurrentInterval,
			byte minZoomCurrentInterval,
			TileCoordinate tile) {
		List<GeoCoordinate> waynodeCoordinates = way.wayNodesAsCoordinateList();

		// if the sub file for lower zoom levels is written, remove all way
		// nodes from the list which are projected on the same pixel
		if (pixelCompression && maxZoomCurrentInterval <= MAX_ZOOMLEVEL_PIXEL_FILTER) {
			waynodeCoordinates = GeoUtils.filterWaynodesOnSamePixel(
					waynodeCoordinates,
					maxZoomCurrentInterval, PIXEL_COMPRESSION_MAX_DELTA);
		}

		// if the way is a multipolygon without a name, clip the way to the
		// tile
		if (polygonClipping && way.getWaytype() >= 2 && waynodeCoordinates.size() >= 4 &&
				(way.getName() == null || way.getName().length() == 0)
					&& minZoomCurrentInterval >= MIN_ZOOMLEVEL_POLYGON_CLIPPING) {
			List<GeoCoordinate> clipped = GeoUtils.clipPolygonToTile(
					waynodeCoordinates, tile);
			if (clipped != null)
				waynodeCoordinates = clipped;
		}

		// if the wayNodeCompression flag is set, compress the way nodes
		// with a minimal amount of bytes
		List<Integer> waynodesAsList = null;
		int maxDiff = Integer.MAX_VALUE;
		if (waynodeCompression) {
			waynodesAsList = GeoUtils.waynodeAbsoluteCoordinatesToOffsets(waynodeCoordinates);
			maxDiff = GeoUtils.maxDiffBetweenCompressedWayNodes(waynodesAsList);
		} else {
			waynodesAsList = waynodesAsList(waynodeCoordinates);
		}

		WayNodePreprocessingResult result = new WayNodePreprocessingResult(waynodesAsList,
				computeCompressionType(maxDiff));

		return result;
	}

	/**
	 * Returns a byte that specifies the layer on which the map element should be rendered and
	 * the amount of tags that a map element has.
	 * 
	 * @param layer
	 *            the layer on which a map element should be rendered
	 * @param tagAmount
	 *            the amount of all tags a map element has
	 * @return a byte that holds the specified information
	 */
	private byte buildLayerTagAmountByte(byte layer, short tagAmount) {
		return (byte) (layer << 4 | tagAmount);
	}

	private byte buildMetaInfoByte(boolean debug, boolean mapStartPosition, boolean filtering,
			boolean clipping,
			boolean compression) {
		byte infoByte = 0;

		if (debug)
			infoByte |= BITMAP_DEBUG;
		if (mapStartPosition)
			infoByte |= BITMAP_MAP_START_POSITION;
		if (filtering)
			infoByte |= BITMAP_WAYNODE_FILTERING;
		if (clipping)
			infoByte |= BITMAP_POLYGON_CLIPPING;
		if (compression)
			infoByte |= BITMAP_WAYNODE_COMPRESSION;

		return infoByte;
	}

	private byte buildInfoByteForPOI(String name, int elevation, String housenumber) {
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

	private int computeCompressionType(int maxDiff) {
		int compressionType = 0;

		if (maxDiff <= Byte.MAX_VALUE)
			compressionType = 3;
		else if (maxDiff <= Short.MAX_VALUE)
			compressionType = 2;
		else if (maxDiff <= Serializer.MAX_VALUE_THREE_BYTES)
			compressionType = 1;

		return compressionType;
	}

	private byte buildRenderTagWayNodeCompressionByte(EnumSet<WayEnum> tags,
			int wayNodeCompressionType) {

		byte infoByte = 0;
		short counter = 0;
		if (tags != null) {
			for (WayEnum wayEnum : tags) {
				if (wayEnum.associatedWithValidZoomlevel())
					counter++;
			}
			infoByte = (byte) (counter << 5);
		}

		if (wayNodeCompressionType == 0) {
			infoByte |= BITMAP_WAYNODECOMPRESSION_4_BYTE;
		}
		if (wayNodeCompressionType == 1) {
			infoByte |= BITMAP_WAYNODECOMPRESSION_3_BYTE;
		}
		if (wayNodeCompressionType == 2) {
			infoByte |= BITMAP_WAYNODECOMPRESSION_2_BYTE;
		}
		if (wayNodeCompressionType == 3) {
			infoByte |= BITMAP_WAYNODECOMPRESSION_1_BYTE;
		}

		return infoByte;
	}

	private byte buildTagBitmapByte(EnumSet<WayEnum> tags) {
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

	private byte buildInfoByteForWay(String name, int wayType, String ref) {
		byte infoByte = 0;

		if (name != null && name.length() > 0) {
			infoByte |= BITMAP_NAME;
		}
		if (ref != null && ref.length() > 0) {
			infoByte |= BITMAP_REF;
		}
		// TODO we do not yet support label positions for ways
		// if (labelPosLat != 0 && labelPosLon != 0) {
		// infoByte |= BITMAP_LABEL;
		// }
		if (wayType == 3) {
			infoByte |= BITMAP_MULTIPOLYGON;
		}

		return infoByte;
	}

	private void writeWayNodes(List<Integer> waynodes, int compressionType)
			throws IOException {
		if (!waynodes.isEmpty()
				&& waynodes.size() % 2 == 0) {
			Iterator<Integer> waynodeIterator = waynodes.iterator();
			file.writeInt(waynodeIterator.next());
			file.writeInt(waynodeIterator.next());

			while (waynodeIterator.hasNext()) {
				switch (compressionType) {
					case 0:
						file.writeInt(waynodeIterator.next());
						file.writeInt(waynodeIterator.next());
						break;
					case 1:
						file.write(Serializer
								.getSignedThreeBytes(waynodeIterator.next()));
						file.write(Serializer
								.getSignedThreeBytes(waynodeIterator.next()));
						break;
					case 2:
						file.writeShort(waynodeIterator.next());
						file.writeShort(waynodeIterator.next());
						break;
					case 3:
						file.writeByte(waynodeIterator.next());
						file.writeByte(waynodeIterator.next());
						break;
				}
			}
		}
	}

	private class WayNodePreprocessingResult {
		private final List<Integer> waynodesAsList;
		private final int compressionType;

		WayNodePreprocessingResult(List<Integer> waynodesAsList, int compressionType) {
			super();
			this.waynodesAsList = waynodesAsList;
			this.compressionType = compressionType;
		}

		List<Integer> getWaynodesAsList() {
			return waynodesAsList;
		}

		int getCompressionType() {
			return compressionType;
		}
	}
}