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
package org.mapsforge.preprocessing.map;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.mapsforge.preprocessing.util.DBConnection;

/**
 * 
 * Extracts already filtered data from the database. Writes the data grouped by map tiles and
 * ordered by zoom levels.
 * 
 * @author sinikka.schroeter
 * 
 */

public class BinaryFileWriter {

	private static final Logger logger = Logger.getLogger(BinaryFileWriter.class.getName());

	// sql queries
	private final String SQL_GET_METADATA = "SELECT * FROM metadata";

	private final String SQL_GET_POIS_SIZE_FOR_TILE = "SELECT sum(size) FROM pois_to_tiles WHERE tile_x = ? AND tile_y = ?";
	private final String SQL_GET_WAYS_SIZE_FOR_TILE = "SELECT sum(size) FROM ways_to_tiles WHERE tile_x = ? AND tile_y = ?";

	private final String SQL_GET_ZOOM_TABLE_FOR_POIS = "SELECT z.zoom_level,count(ptt.poi_id) FROM zoom_level z LEFT OUTER JOIN (select pt.poi_id,pt.zoom_level from pois_to_tiles pt WHERE pt.tile_x = ? AND pt.tile_y = ?) as ptt ON (z.zoom_level = ptt.zoom_level) group by z.zoom_level ORDER BY z.zoom_level";
	private final String SQL_GET_ZOOM_TABLE_FOR_WAYS = "SELECT z.zoom_level, count(wtt.way_id) FROM zoom_level z LEFT OUTER JOIN (select wt.way_id, wt.zoom_level from ways_to_tiles wt where wt.tile_x = ? and wt.tile_y = ?) as wtt ON (z.zoom_level = wtt.zoom_level) GROUP BY z.zoom_level ORDER BY z.zoom_level";

	private final String SQL_GET_POIS_FOR_TILE = "SELECT pois.*,ptt.zoom_level FROM pois_to_tiles ptt JOIN pois pois "
			+ "ON (ptt.poi_id = pois.id) WHERE tile_x = ? AND tile_y = ? ORDER BY zoom_level";
	private final String SQL_GET_WAYS_FOR_TILE = "SELECT ways.*, wtt.tile_bitmask,wtt.zoom_level,wtt.size FROM ways_to_tiles wtt "
			+ "JOIN ways ways ON (wtt.way_id = ways.id) WHERE tile_x = ? AND tile_y = ? ORDER BY zoom_level";
	private final String SQL_GET_WAYNODES = "SELECT latitude,longitude FROM waynodes WHERE way_id = ? ORDER BY waynode_sequence";
	private final String SQL_GET_INNER_WAY_NODES = "SELECT latitude,longitude FROM multipolygons WHERE outer_way_id = ? AND inner_way_sequence = ? order by waynode_sequence";

	// prepared statements
	private PreparedStatement pstmtPoisSizeForTiles;
	private PreparedStatement pstmtWaysSizeForTiles;

	private PreparedStatement pstmtPoisZoomTable;
	private PreparedStatement pstmtWaysZoomTable;

	private PreparedStatement pstmtPoisForTile;
	private PreparedStatement pstmtWaysForTile;

	private PreparedStatement pstmtWaynodes;
	private PreparedStatement pstmtMultipolygons;

	// result sets
	private ResultSet rsBBoxCorners;

	private ResultSet rsPoisSize;
	private ResultSet rsWaysSize;

	private ResultSet rsPoisZoomTable;
	private ResultSet rsWaysZoomTable;

	private ResultSet rsPoisForTile;
	private ResultSet rsWaysForTile;

	private ResultSet rsWaynodes;
	private ResultSet rsMultipolygons;

	private Tile upperLeft;
	private Tile bottomRight;

	private static String propertiesFile;
	private static String targetFile;

	private static String mapFileComment = "";

	private Connection conn;

	private static RandomAccessFile raf;

	private static Map<String, Byte> whiteList = WhiteList.getWayTagWhitelist();

	private static short minZoom;
	private static short maxZoom;

	// bitmap flags for pois and ways
	private static final short BITMAP_NAME = 128;

	// bitmap flags for pois
	private static final short BITMAP_ELEVATION = 64;
	private static final short BITMAP_HOUSENUMBER = 32;

	// bitmap flags for ways
	private static final short BITMAP_LABEL = 64;
	private static final short BITMAP_AREA = 32;
	private static final short BITMAP_MULTIPOLYGON = 16;
	private static final short BITMAP_HIGHWAY = 128;
	private static final short BITMAP_RAILWAY = 64;
	private static final short BITMAP_BUILDING = 32;
	private static final short BITMAP_LANDUSE = 16;
	private static final short BITMAP_LEISURE = 8;
	private static final short BITMAP_AMENITY = 4;
	private static final short BITMAP_NATURAL = 2;
	private static final short BITMAP_WATERWAY = 1;

	private static long startTime;

	public BinaryFileWriter(String propertiesFile) {
		try {
			startTime = System.currentTimeMillis();

			// setup database connection
			Properties props = new Properties();
			props.load(new FileInputStream(propertiesFile));
			DBConnection dbConnection = new DBConnection(propertiesFile);
			conn = dbConnection.getConnection();

			conn.setAutoCommit(false);

			// conn.createStatement().execute("TRUNCATE TABLE filtered_pois CASCADE");
			// conn.createStatement().execute("TRUNCATE TABLE filtered_ways CASCADE");

			// conn.commit();

			pstmtPoisSizeForTiles = conn.prepareStatement(SQL_GET_POIS_SIZE_FOR_TILE);
			pstmtWaysSizeForTiles = conn.prepareStatement(SQL_GET_WAYS_SIZE_FOR_TILE);

			pstmtPoisZoomTable = conn.prepareStatement(SQL_GET_ZOOM_TABLE_FOR_POIS);
			pstmtWaysZoomTable = conn.prepareStatement(SQL_GET_ZOOM_TABLE_FOR_WAYS);

			pstmtPoisForTile = conn.prepareStatement(SQL_GET_POIS_FOR_TILE);
			pstmtWaysForTile = conn.prepareStatement(SQL_GET_WAYS_FOR_TILE);

			pstmtWaynodes = conn.prepareStatement(SQL_GET_WAYNODES);
			pstmtMultipolygons = conn.prepareStatement(SQL_GET_INNER_WAY_NODES);

			logger.info("database connection setup done");

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void finalize() throws Throwable {
		if (!conn.isClosed())
			conn.close();
	}

	private void writeHeaderAndIndex() {
		try {
			int maxlat = -1;
			int minlon = -1;
			int minlat = -1;
			int maxlon = -1;
			long date = -1;
			int version = -1;
			short zoom = -1;
			short tilePixel = -1;

			String comment = mapFileComment;

			String magicByte = "mapsforge binary OSM";

			long biggestTileSize = 0;

			byte[] tileSizeBytes;

			long biggestTileSizePosition;

			// get metadata for the map file
			rsBBoxCorners = conn.createStatement().executeQuery(SQL_GET_METADATA);
			while (rsBBoxCorners.next()) {
				maxlat = rsBBoxCorners.getInt("maxlat");
				minlon = rsBBoxCorners.getInt("minlon");
				minlat = rsBBoxCorners.getInt("minlat");
				maxlon = rsBBoxCorners.getInt("maxlon");
				date = rsBBoxCorners.getLong("date");
				version = rsBBoxCorners.getInt("import_version");
				zoom = rsBBoxCorners.getShort("zoom");
				tilePixel = rsBBoxCorners.getShort("tile_size");
				minZoom = rsBBoxCorners.getShort("min_zoom_level");
				maxZoom = rsBBoxCorners.getShort("max_zoom_level");

				// calculate corner tiles of the bounding box of the given area
				upperLeft = new Tile(MercatorProjection.longitudeToTileX(
						(double) minlon / 1000000, (byte) zoom), MercatorProjection
						.latitudeToTileY((double) maxlat / 1000000, (byte) zoom), (byte) zoom);
				bottomRight = new Tile(MercatorProjection.longitudeToTileX(
						(double) maxlon / 1000000, (byte) zoom), MercatorProjection
						.latitudeToTileY((double) minlat / 1000000, (byte) zoom), (byte) zoom);
			}

			logger.info("writing header");

			// write file header
			// magic byte
			byte[] magicBytes = magicByte.getBytes();
			for (byte b : magicBytes) {
				raf.writeByte(b);
			}
			// version number of the binary file format
			raf.writeInt(version);

			// basic zoom level
			raf.writeByte(zoom);

			// width and height of a tile in pixel
			raf.writeShort(tilePixel);

			// minimal zoom level
			raf.writeByte((byte) minZoom);

			// maximal zoom level
			raf.writeByte((byte) maxZoom);

			// upper left corner of the bounding box
			raf.writeInt(maxlat);
			raf.writeInt(minlon);

			// bottom right corner of the bounding box
			raf.writeInt(minlat);
			raf.writeInt(maxlon);

			// date of the map data
			raf.writeLong(date);

			// place holder for the size of the biggest tile
			biggestTileSizePosition = raf.getFilePointer();
			raf.seek(biggestTileSizePosition + 5);

			// comment
			if (!comment.equals("")) {
				raf.writeUTF(comment);
			} else {
				raf.writeUTF("no comment");
			}

			// zoom_level = (short) ((maxZoom - minZoom) + 1);

			// create temporary zoom table
			conn.createStatement().execute(
					"CREATE TEMPORARY TABLE zoom_level(zoom_level smallint)");
			for (short s = minZoom; s <= maxZoom; s++) {
				conn.createStatement().execute(
						"INSERT INTO zoom_level (zoom_level) VALUES (" + s + ")");
			}

			// TODO: needs refactoring: zoom level of elements should not be changed!
			// set zoom level smaller than minimal zoom to the value of the minimal zoom
			conn.createStatement().execute(
					"UPDATE pois_to_tiles SET zoom_level = " + minZoom + " WHERE zoom_level < "
							+ minZoom);
			conn.createStatement().execute(
					"UPDATE ways_to_tiles SET zoom_level = " + minZoom + " WHERE zoom_level < "
							+ minZoom);

			// write Index
			// for debug:

			/** only for debug **/
			// write tile header
			String tileStart = "+++IndexStart+++";
			logger.info(tileStart);
			byte[] stringBytes = tileStart.getBytes();
			for (byte b : stringBytes) {
				raf.writeByte(b);
			}
			/** only for debug **/
			logger.info("+++IndexStart+++");

			long tileSize;
			long cumulatedTileSize = raf.getFilePointer();
			for (long tileX = upperLeft.x; tileX <= bottomRight.x; tileX++) {
				for (long tileY = upperLeft.y; tileY <= bottomRight.y; tileY++) {
					tileSize = 0;

					// get size of all pois for a tile
					pstmtPoisSizeForTiles.setLong(1, tileX);
					pstmtPoisSizeForTiles.setLong(2, tileY);
					rsPoisSize = pstmtPoisSizeForTiles.executeQuery();
					while (rsPoisSize.next()) {
						tileSize += rsPoisSize.getInt(1);
					}

					// get size of all ways for a tile
					pstmtWaysSizeForTiles.setLong(1, tileX);
					pstmtWaysSizeForTiles.setLong(2, tileY);
					rsWaysSize = pstmtWaysSizeForTiles.executeQuery();
					while (rsWaysSize.next()) {
						tileSize += rsWaysSize.getInt(1);
					}
					// set biggest tile size
					if (tileSize > biggestTileSize)
						biggestTileSize = tileSize;

					cumulatedTileSize += tileSize;

					// write tile size
					tileSizeBytes = Serializer.getFiveBytes(cumulatedTileSize);
					for (byte b : tileSizeBytes) {
						raf.writeByte(b);
					}
				}
			}

			// write biggest tile size into the header
			long currentPosition = raf.getFilePointer();
			raf.seek(biggestTileSizePosition);
			tileSizeBytes = Serializer.getFiveBytes(biggestTileSize);
			for (byte b : tileSizeBytes) {
				raf.writeByte(b);
			}
			raf.seek(currentPosition);

		} catch (SQLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void writeTiles() {
		try {
			String tileHead = "###TileStart";
			String tileTail = "###";
			String poisHead = "***POIStart";
			String poisTail = "***";
			String waysHead = "---WayStart";
			String waysTail = "---";
			String tileStart;
			String poiStart;
			String wayStart;

			byte[] stringBytes;
			long firstWayInTile;
			int byteArrayLength;

			Map<Short, Short> poiTableForTile;
			Map<Short, Short> wayTableForTile;
			// short cumulatedCounterPois;
			// short cumulatedCounterWays;

			long id;
			// int latitude;
			// int longitude;
			Byte layer;
			short tagAmount;
			String tags;
			String name;
			short nameLength;
			int elevation;
			String housenumber;
			// short zoom;
			int waySize;
			int waynodesAmount;
			short wayType;
			// short convexness;
			int labelPositionLatitude;
			int labelPositionLongitude;
			short innerWayAmount;
			short tileBitmask;

			long innerwayNodeCounterPos;
			short innerwayNodes = 0;

			long currentPosition;

			String[] tagStrings;

			Map<String, Byte> tagIdsPOIs = TagIdsPOIs.getMap();
			Map<String, Byte> tagIdsWays = TagIdsWays.getMap();

			int tileCounter = 0;

			for (long tileY = upperLeft.y; tileY <= bottomRight.y; tileY++) {
				for (long tileX = upperLeft.x; tileX <= bottomRight.x; tileX++) {
					tileCounter++;
					/** only for debug **/
					// write tile header
					tileStart = tileHead + tileX + "," + tileY + tileTail;
					logger.info(tileStart);

					stringBytes = tileStart.getBytes();
					byteArrayLength = stringBytes.length;
					for (byte b : stringBytes) {
						raf.writeByte(b);
					}

					if (byteArrayLength < 32) {
						while (byteArrayLength < 32) {
							raf.writeByte(32);
							byteArrayLength++;
						}
						/** only for debug **/

						logger.info("write element zoom table");
						poiTableForTile = new TreeMap<Short, Short>();
						wayTableForTile = new TreeMap<Short, Short>();

						// get amount of pois and ways grouped by zoom level for this certain
						// tile
						pstmtPoisZoomTable.setLong(1, tileX);
						pstmtPoisZoomTable.setLong(2, tileY);
						rsPoisZoomTable = pstmtPoisZoomTable.executeQuery();

						// join temporary zoom level table with filtered pois table
						while (rsPoisZoomTable.next()) {
							poiTableForTile.put(rsPoisZoomTable.getShort(1), rsPoisZoomTable
									.getShort(2));
						}

						pstmtWaysZoomTable.setLong(1, tileX);
						pstmtWaysZoomTable.setLong(2, tileY);
						rsWaysZoomTable = pstmtWaysZoomTable.executeQuery();

						while (rsWaysZoomTable.next()) {
							wayTableForTile.put(rsWaysZoomTable.getShort(1), rsWaysZoomTable
									.getShort(2));
						}

						// cumulate the amount of pois and ways
						// cumulatedCounterPois = 0;
						// cumulatedCounterWays = 0;
						for (short min = minZoom; min <= maxZoom; min++) {
							// cumulatedCounterPois += poiTableForTile.get(min);
							// cumulatedCounterWays += wayTableForTile.get(min);

							// write amount of pois and ways which should be read for a certain
							// zoom level
							raf.writeShort(poiTableForTile.get(min));
							raf.writeShort(wayTableForTile.get(min));
						}

						// get pointer to first way in this tile
						firstWayInTile = raf.getFilePointer();
						raf.seek(firstWayInTile + 8);

						pstmtPoisForTile.setLong(1, tileX);
						pstmtPoisForTile.setLong(2, tileY);

						// get all pois for this tile ordered by zoom level
						rsPoisForTile = pstmtPoisForTile.executeQuery();
						while (rsPoisForTile.next()) {
							/** only for debug **/
							// write debug string
							poiStart = poisHead + rsPoisForTile.getLong("id") + poisTail;
							logger.info(poiStart);
							stringBytes = poiStart.getBytes();
							byteArrayLength = stringBytes.length;
							for (byte b : stringBytes) {
								raf.writeByte(b);
							}
							// if string is not 32 byte long append whitespaces, byte value = 32
							if (byteArrayLength < 32) {
								while (byteArrayLength < 32) {
									raf.writeByte(32);
									byteArrayLength++;
								}
							}
							/** only for debug **/

							// write poi features to the file
							raf.writeInt(rsPoisForTile.getInt("latitude"));
							raf.writeInt(rsPoisForTile.getInt("longitude"));
							nameLength = rsPoisForTile.getShort("name_length");
							name = rsPoisForTile.getString("name");
							tagAmount = rsPoisForTile.getShort("tags_amount");
							layer = (byte) rsPoisForTile.getShort("layer");
							elevation = rsPoisForTile.getInt("elevation");
							housenumber = rsPoisForTile.getString("housenumber");
							tags = rsPoisForTile.getString("tags");
							// zoom = rsPoisForTile.getShort("zoom_level");

							/** debug **/
							logger.info("hausnumber: " + housenumber + " length: "
									+ housenumber.getBytes().length);
							logger.info("name: " + name + " length: " + nameLength);
							logger.info("elevation: " + elevation);
							/** debug **/

							// write byte with layer and tag amount info
							raf.writeByte(buildLayerTagAmountByte(layer, tagAmount));

							// write tag ids to the file
							tagStrings = tags.split("\n");
							for (String tag : tagStrings) {
								raf.writeByte(tagIdsPOIs.get(tag));
							}

							// write byte with bits set to 1 if the poi has a name, an elevation
							// or
							// a housenumber
							raf.writeByte(buildInfoByteForPOI(nameLength, elevation,
									housenumber.getBytes().length));

							if (nameLength != 0) {
								/** debug **/
								logger.info("poi has name");
								/** debug **/
								raf.writeUTF(name);
							}

							if (elevation != 0) {
								/** debug **/
								logger.info("poi has elevation");
								/** debug **/
								raf.writeShort(elevation);
							}

							if (housenumber.getBytes().length != 0) {
								/** debug **/
								logger.info("poi has housenumber");
								/** debug **/
								raf.writeUTF(housenumber);
							}
						}

						// write position of the first way in the tile
						currentPosition = raf.getFilePointer();
						raf.seek(firstWayInTile);
						raf.writeLong(currentPosition);
						raf.seek(currentPosition);

						// get ways
						pstmtWaysForTile.setLong(1, tileX);
						pstmtWaysForTile.setLong(2, tileY);

						// get all ways for this tile ordered by zoom level
						rsWaysForTile = pstmtWaysForTile.executeQuery();
						while (rsWaysForTile.next()) {
							id = rsWaysForTile.getLong("id");
							/** only for debug **/
							// write debug string
							wayStart = waysHead + id + waysTail;
							logger.info(wayStart);
							stringBytes = wayStart.getBytes();
							byteArrayLength = stringBytes.length;
							for (byte b : stringBytes) {
								raf.writeByte(b);
							}
							// if string is not 32 byte long append whitespaces, byte value = 32
							if (byteArrayLength < 32) {
								while (byteArrayLength < 32) {
									raf.writeByte(32);
									byteArrayLength++;
								}
							}
							/** only for debug **/

							nameLength = rsWaysForTile.getShort("name_length");
							name = rsWaysForTile.getString("name");
							tagAmount = rsWaysForTile.getShort("tags_amount");
							layer = (byte) rsWaysForTile.getShort("layer");
							waynodesAmount = rsWaysForTile.getInt("waynodes_amount");
							wayType = rsWaysForTile.getShort("way_type");
							tags = rsWaysForTile.getString("tags");
							labelPositionLatitude = rsWaysForTile.getInt("label_pos_lat");
							labelPositionLongitude = rsWaysForTile.getInt("label_pos_lon");
							innerWayAmount = rsWaysForTile.getShort("inner_way_amount");
							tileBitmask = rsWaysForTile.getShort("tile_bitmask");
							waySize = rsWaysForTile.getInt("size");

							tagStrings = tags.split("\n");

							// write way features
							raf.writeInt(waySize);
							raf.writeShort(tileBitmask);

							// write byte with layer and tag amount
							raf.writeByte(buildLayerTagAmountByte(layer, tagAmount));

							// write byte with amount of tags which are rendered
							raf.writeByte(buildRenderTagByte(tagStrings));

							// write tag bitmap
							raf.writeByte(buildTagBitmapByte(tagStrings));

							// write tag ids
							for (String tag : tagStrings) {
								raf.writeByte(tagIdsWays.get(tag));
							}

							// write way nodes
							raf.writeShort(waynodesAmount);

							pstmtWaynodes.setLong(1, id);
							rsWaynodes = pstmtWaynodes.executeQuery();
							while (rsWaynodes.next()) {
								raf.writeInt(rsWaynodes.getInt("latitude"));
								raf.writeInt(rsWaynodes.getInt("longitude"));
							}

							// write byte with name, label and way type information
							raf.writeByte(buildInfoByteForWay(nameLength,
									labelPositionLatitude, labelPositionLongitude, wayType));

							if (nameLength != 0) {
								raf.writeUTF(name);
							}
							if (labelPositionLatitude != 0 && labelPositionLongitude != 0) {
								raf.writeInt(labelPositionLatitude);
								raf.writeInt(labelPositionLongitude);
							}

							// if way is an outer way of a multipolygon write all inner ways and
							// the
							// corresponding way nodes
							if (wayType == 3) { // || wayType == 0) {
								/** debug **/
								logger.info("amount of innerways: " + innerWayAmount);
								/** debug **/
								raf.writeByte((byte) innerWayAmount);
								for (int k = 1; k <= innerWayAmount; k++) {
									innerwayNodeCounterPos = raf.getFilePointer();
									raf.seek(innerwayNodeCounterPos + 2);
									innerwayNodes = 0;
									pstmtMultipolygons.setLong(1, id);
									pstmtMultipolygons.setShort(2, (short) k);
									rsMultipolygons = pstmtMultipolygons.executeQuery();
									while (rsMultipolygons.next()) {
										innerwayNodes++;
										raf.writeInt(rsMultipolygons.getInt("latitude"));
										raf.writeInt(rsMultipolygons.getInt("longitude"));
									}
									currentPosition = raf.getFilePointer();
									raf.seek(innerwayNodeCounterPos);
									logger.info("innerwayNodes: " + innerwayNodes);
									raf.writeShort(innerwayNodes);
									raf.seek(currentPosition);
								}
							}
						}
					}
				}
			}
			logger.info("total number of " + tileCounter + " tiles");
			logger.info("processing took " + (System.currentTimeMillis() - startTime) / 1000
					+ "s.");
			logger.info("binary map data file created");
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Returns a byte where the single bits indicate if the current poi has certain features.
	 * 
	 * @param nameLength
	 *            length of the UTF-8 encoded name of the poi in bytes
	 * @param elevation
	 *            value of the elevation feature in meter
	 * @param housenumberLength
	 *            length of the UTF-8 encoded housenumber in bytes
	 * @return a byte where certain bits are set to 1 if the current poi has the feature
	 */
	private byte buildInfoByteForPOI(short nameLength, int elevation, int housenumberLength) {
		byte infoByte = 0;

		if (nameLength != 0) {
			infoByte |= BITMAP_NAME;
		}
		if (elevation != 0) {
			infoByte |= BITMAP_ELEVATION;
		}
		if (housenumberLength != 0) {
			infoByte |= BITMAP_HOUSENUMBER;
		}

		logger.info("infobyte: " + infoByte);

		return infoByte;
	}

	/**
	 * Returns a byte where the single bits indicate if the current way has certain features.
	 * 
	 * @param nameLength
	 *            length of the name of the way in bytes
	 * @param labelPosLat
	 *            latitude value of the label position coordinate
	 * @param labelPosLon
	 *            longitude value of the label position coordinate
	 * @param wayType
	 *            type of the way (way, area, multipolygon)
	 * @return a byte where certain bits are set to 1 if the current way has the feature
	 */
	private byte buildInfoByteForWay(short nameLength, int labelPosLat, int labelPosLon,
			short wayType) {
		byte infoByte = 0;

		if (nameLength != 0) {
			infoByte |= BITMAP_NAME;
		}
		if (labelPosLat != 0 && labelPosLon != 0) {
			infoByte |= BITMAP_LABEL;
		}
		if (wayType == 2) {
			infoByte |= BITMAP_AREA;
		}
		if (wayType == 3 || wayType == 0) {
			infoByte |= BITMAP_MULTIPOLYGON;
		}

		return infoByte;
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
		byte infoByte = 0;

		infoByte = (byte) (layer << 4 | tagAmount);

		return infoByte;
	}

	/**
	 * Returns a byte that specifies the amount of tags which will be rendered.
	 * 
	 * @param tags
	 *            the tags of the current way
	 * @return a byte
	 */
	private byte buildRenderTagByte(String[] tags) {
		byte infoByte = 0;
		short counter = 0;

		for (String tag : tags) {
			if (!whiteList.get(tag).equals(Byte.MAX_VALUE))
				counter++;
		}

		infoByte = (byte) (counter << 5);
		return infoByte;
	}

	/**
	 * Returns a byte that specifies which kind of tags a way has. A certain bit of the returned
	 * byte is set to 1 if the way has such a tag, else 0.
	 * 
	 * @param tags
	 *            the tags of the current way
	 * @return a byte where the bits are set to 1 if the current way has certain tags
	 */
	private byte buildTagBitmapByte(String[] tags) {
		byte infoByte = 0;
		String key;

		for (String tag : tags) {
			logger.info("tag for bitmap: " + tag);
			key = tag.substring(0, tag.indexOf("="));
			if (key.equals("highway")) {
				infoByte |= BITMAP_HIGHWAY;
			} else if (key.equals("railway")) {
				infoByte |= BITMAP_RAILWAY;
			} else if (key.equals("building")) {
				infoByte |= BITMAP_BUILDING;
			} else if (key.equals("landuse")) {
				infoByte |= BITMAP_LANDUSE;
			} else if (key.equals("leisure")) {
				infoByte |= BITMAP_LEISURE;
			} else if (key.equals("amenity")) {
				infoByte |= BITMAP_AMENITY;
			} else if (key.equals("natural")) {
				infoByte |= BITMAP_NATURAL;
			} else if (key.equals("waterway")) {
				infoByte |= BITMAP_WATERWAY;
			}
		}

		return infoByte;
	}

	public static void main(String[] args) {
		if (args.length < 2 || args.length > 3) {
			System.err
					.println("usage: BinaryFileWriter <properties-file> <target-file> (optional: <comment>)");
			System.exit(1);
		}

		propertiesFile = args[0];
		targetFile = args[1];

		if (args.length == 3) {
			mapFileComment = args[2];
		}

		try {
			raf = new RandomAccessFile(targetFile, "rw");

			BinaryFileWriter bfw = new BinaryFileWriter(propertiesFile);
			bfw.writeHeaderAndIndex();
			bfw.writeTiles();

			logger.info("processing took " + (System.currentTimeMillis() - startTime) / 1000
					+ "s.");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
}
