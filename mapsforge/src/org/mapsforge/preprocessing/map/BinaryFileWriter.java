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

import gnu.trove.map.hash.THashMap;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import org.mapsforge.core.MercatorProjection;
import org.mapsforge.preprocessing.util.DBConnection;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * 
 * Extracts already filtered data from the database. Writes the data grouped by map tiles and
 * ordered by zoom levels.
 * 
 */

public class BinaryFileWriter {

	private static final String MAGIC_BYTE = "mapsforge binary OSM";

	private static final Logger logger = Logger.getLogger(BinaryFileWriter.class.getName());

	// sql queries
	private final String SQL_GET_METADATA = "SELECT * FROM metadata";

	private final String SQL_GET_MIN_ZOOM_ELEMENT_COUNT_POIS_LOW_ZOOM = "SELECT count(pt.poi_id) FROM pois_to_tiles_less_data pt WHERE pt.tile_x = ? AND pt.tile_y = ? AND pt.zoom_level <= ?";
	private final String SQL_GET_MIN_ZOOM_ELEMENT_COUNT_WAYS_LOW_ZOOM = "SELECT count(wt.way_id) FROM ways_to_tiles_less_data wt WHERE wt.tile_x = ? AND wt.tile_y = ? AND wt.zoom_level <= ?";

	private final String SQL_GET_ZOOM_TABLE_FOR_POIS_LOW_ZOOM = "SELECT z.zoom_level,count(ptt.poi_id) FROM zoom_level_low z LEFT OUTER JOIN "
			+ "(SELECT pt.poi_id,pt.zoom_level FROM pois_to_tiles_less_data pt WHERE pt.tile_x = ? AND pt.tile_y = ?) as ptt ON (z.zoom_level = ptt.zoom_level) group by z.zoom_level ORDER BY z.zoom_level";
	private final String SQL_GET_ZOOM_TABLE_FOR_WAYS_LOW_ZOOM = "SELECT z.zoom_level, count(wtt.way_id) FROM zoom_level_low z LEFT OUTER JOIN "
			+ "(SELECT wt.way_id, wt.zoom_level FROM ways_to_tiles_less_data wt WHERE wt.tile_x = ? and wt.tile_y = ?) as wtt ON (z.zoom_level = wtt.zoom_level) GROUP BY z.zoom_level ORDER BY z.zoom_level";

	private final String SQL_GET_MAX_ZOOM_ELEMENT_COUNT_POIS_LOW_ZOOM = "SELECT count(pt.poi_id) FROM pois_to_tiles_less_data pt WHERE pt.tile_x = ? AND pt.tile_y = ? AND pt.zoom_level >= ?";
	private final String SQL_GET_MAX_ZOOM_ELEMENT_COUNT_WAYS_LOW_ZOOM = "SELECT count(wt.way_id) FROM ways_to_tiles_less_data wt WHERE wt.tile_x = ? AND wt.tile_y = ? AND wt.zoom_level >= ?";

	private final String SQL_GET_POIS_FOR_TILE_LOW_ZOOM = "SELECT pois.*,ptt.zoom_level FROM  pois_to_tiles_less_data ptt JOIN pois pois "
			+ "ON (ptt.poi_id = pois.id) WHERE tile_x = ? AND tile_y = ? ORDER BY zoom_level";
	private final String SQL_GET_WAYS_FOR_TILE_LOW_ZOOM = "SELECT ways.*, wtt.tile_bitmask,wtt.zoom_level,wtt.size FROM ways_to_tiles_less_data wtt JOIN ways ways ON (wtt.way_id = ways.id) WHERE tile_x = ? AND tile_y = ? ORDER BY zoom_level";

	private final String SQL_GET_MIN_ZOOM_ELEMENT_COUNT_POIS = "SELECT count(pt.poi_id) FROM pois_to_tiles pt WHERE pt.tile_x = ? AND pt.tile_y = ? AND pt.zoom_level <= ?";
	private final String SQL_GET_MIN_ZOOM_ELEMENT_COUNT_WAYS = "SELECT count(wt.way_id) FROM ways_to_tiles wt WHERE wt.tile_x = ? AND wt.tile_y = ? AND wt.zoom_level <= ?";

	private final String SQL_GET_ZOOM_TABLE_FOR_POIS = "SELECT z.zoom_level,count(ptt.poi_id) FROM zoom_level_high z LEFT OUTER JOIN "
			+ "(SELECT pt.poi_id,pt.zoom_level FROM pois_to_tiles pt WHERE pt.tile_x = ? AND pt.tile_y = ?) as ptt ON (z.zoom_level = ptt.zoom_level) group by z.zoom_level ORDER BY z.zoom_level";
	private final String SQL_GET_ZOOM_TABLE_FOR_WAYS = "SELECT z.zoom_level, count(wtt.way_id) FROM zoom_level_high z LEFT OUTER JOIN "
			+ "(SELECT wt.way_id, wt.zoom_level FROM ways_to_tiles wt WHERE wt.tile_x = ? and wt.tile_y = ?) as wtt ON (z.zoom_level = wtt.zoom_level) GROUP BY z.zoom_level ORDER BY z.zoom_level";

	private final String SQL_GET_MAX_ZOOM_ELEMENT_COUNT_POIS = "SELECT count(pt.poi_id) FROM pois_to_tiles pt WHERE pt.tile_x = ? AND pt.tile_y = ? AND pt.zoom_level >= ?";
	private final String SQL_GET_MAX_ZOOM_ELEMENT_COUNT_WAYS = "SELECT count(wt.way_id) FROM ways_to_tiles wt WHERE wt.tile_x = ? AND wt.tile_y = ? AND wt.zoom_level >= ?";

	private final String SQL_GET_POIS_FOR_TILE = "SELECT pois.*,ptt.zoom_level FROM pois_to_tiles ptt JOIN pois pois "
			+ "ON (ptt.poi_id = pois.id) WHERE tile_x = ? AND tile_y = ? ORDER BY zoom_level";
	private final String SQL_GET_WAYS_FOR_TILE = "SELECT ways.*, wtt.tile_bitmask,wtt.zoom_level,wtt.size FROM ways_to_tiles wtt "
			+ "JOIN ways ways ON (wtt.way_id = ways.id) WHERE tile_x = ? AND tile_y = ? ORDER BY zoom_level";

	private final String SQL_GET_WAYNODES = "SELECT latitude,longitude FROM waynodes WHERE way_id = ? ORDER BY waynode_sequence";
	private final String SQL_GET_INNER_WAY_NODES = "SELECT latitude,longitude FROM multipolygons WHERE outer_way_id = ? AND inner_way_sequence = ? order by waynode_sequence";

	// prepared statements
	private PreparedStatement pstmtPoisCountMinZoomLow;
	private PreparedStatement pstmtWaysCountMinZoomLow;

	private PreparedStatement pstmtPoisCountMaxZoomLow;
	private PreparedStatement pstmtWaysCountMaxZoomLow;

	private PreparedStatement pstmtPoisZoomTableLow;
	private PreparedStatement pstmtWaysZoomTableLow;

	private PreparedStatement pstmtPoisForTileLow;
	private PreparedStatement pstmtWaysForTileLow;

	private PreparedStatement pstmtPoisCountMinZoom;
	private PreparedStatement pstmtWaysCountMinZoom;

	private PreparedStatement pstmtPoisCountMaxZoom;
	private PreparedStatement pstmtWaysCountMaxZoom;

	private PreparedStatement pstmtPoisZoomTable;
	private PreparedStatement pstmtWaysZoomTable;

	private PreparedStatement pstmtPoisForTile;
	private PreparedStatement pstmtWaysForTile;

	private PreparedStatement pstmtWaynodes;
	private PreparedStatement pstmtMultipolygons;

	// result sets
	private ResultSet rsBBoxCorners;

	private ResultSet rsPoisMinZoom;
	private ResultSet rsWaysMinZoom;

	private ResultSet rsPoisMaxZoom;
	private ResultSet rsWaysMaxZoom;

	private ResultSet rsPoisZoomTable;
	private ResultSet rsWaysZoomTable;

	private ResultSet rsPoisForTile;
	private ResultSet rsWaysForTile;

	private ResultSet rsWaynodes;
	private ResultSet rsMultipolygons;

	private Tile upperLeftHigh;
	private Tile bottomRightHigh;

	private Tile upperLeftLow;
	private Tile bottomRightLow;

	private static String propertiesFile;
	private static String targetFile;

	private static String featurePropertiesFile;

	private static String mapFileComment = "";

	// TODO: make sub file amount flexible
	private static short fileAmount;

	private static boolean debugStrings;
	private static boolean polygonClipping;
	private static boolean wayNodePixelFilter;
	private static boolean wayNodeCompression;

	private short zoom_level_low;
	private short zoom_level_high;

	private Connection conn;

	private static RandomAccessFile raf;

	private static Map<String, Byte> whiteList = WhiteList.getWayTagWhitelist();

	private static short minZoom;
	private static short maxZoom;
	private static short minZoomLow;
	private static short maxZoomLow;

	// bitmap flags for pois and ways
	private static final short BITMAP_NAME = 128;

	// bitmap flags for pois
	private static final short BITMAP_ELEVATION = 64;
	private static final short BITMAP_HOUSENUMBER = 32;

	// bitmap flags for ways
	private static final short BITMAP_REF = 64;
	private static final short BITMAP_LABEL = 32;
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

	private static final int MAX_VALUE_THREE_BYTES = 8388607;

	private static long startTime;

	private long nextIndexValue;
	private long biggestTileSizePosition;

	long previousTilePosition = 0;

	private long startLowerZoom;
	private long startHigherZoom;

	private long subFileSizeLowZoom;
	private long subFileSizeHighZoom;

	private long endLowerZoom;
	private long endHigherZoom;

	private long startPosition;
	private byte[] startPositionInFiveBytes;

	private int biggestTileSize = 0;

	BinaryFileWriter(String propertiesFile) {
		try {
			startTime = System.currentTimeMillis();

			// setup database connection
			Properties props = new Properties();
			props.load(new FileInputStream(propertiesFile));
			DBConnection dbConnection = new DBConnection(propertiesFile);
			conn = dbConnection.getConnection();

			pstmtPoisCountMinZoomLow = conn
					.prepareStatement(SQL_GET_MIN_ZOOM_ELEMENT_COUNT_POIS_LOW_ZOOM);
			pstmtWaysCountMinZoomLow = conn
					.prepareStatement(SQL_GET_MIN_ZOOM_ELEMENT_COUNT_WAYS_LOW_ZOOM);

			pstmtPoisCountMaxZoomLow = conn
					.prepareStatement(SQL_GET_MAX_ZOOM_ELEMENT_COUNT_POIS_LOW_ZOOM);
			pstmtWaysCountMaxZoomLow = conn
					.prepareStatement(SQL_GET_MAX_ZOOM_ELEMENT_COUNT_WAYS_LOW_ZOOM);

			pstmtPoisZoomTableLow = conn.prepareStatement(SQL_GET_ZOOM_TABLE_FOR_POIS_LOW_ZOOM);
			pstmtWaysZoomTableLow = conn.prepareStatement(SQL_GET_ZOOM_TABLE_FOR_WAYS_LOW_ZOOM);

			pstmtPoisForTileLow = conn.prepareStatement(SQL_GET_POIS_FOR_TILE_LOW_ZOOM);
			pstmtWaysForTileLow = conn.prepareStatement(SQL_GET_WAYS_FOR_TILE_LOW_ZOOM);

			pstmtPoisCountMinZoom = conn.prepareStatement(SQL_GET_MIN_ZOOM_ELEMENT_COUNT_POIS);
			pstmtWaysCountMinZoom = conn.prepareStatement(SQL_GET_MIN_ZOOM_ELEMENT_COUNT_WAYS);

			pstmtPoisCountMaxZoom = conn.prepareStatement(SQL_GET_MAX_ZOOM_ELEMENT_COUNT_POIS);
			pstmtWaysCountMaxZoom = conn.prepareStatement(SQL_GET_MAX_ZOOM_ELEMENT_COUNT_WAYS);

			pstmtPoisZoomTable = conn.prepareStatement(SQL_GET_ZOOM_TABLE_FOR_POIS);
			pstmtWaysZoomTable = conn.prepareStatement(SQL_GET_ZOOM_TABLE_FOR_WAYS);

			pstmtPoisForTile = conn.prepareStatement(SQL_GET_POIS_FOR_TILE);
			pstmtWaysForTile = conn.prepareStatement(SQL_GET_WAYS_FOR_TILE);

			pstmtWaynodes = conn.prepareStatement(SQL_GET_WAYNODES);
			pstmtMultipolygons = conn.prepareStatement(SQL_GET_INNER_WAY_NODES);

			logger.info("database connection setup done");

			// set feature options
			props = new Properties();
			props.load(new FileInputStream(featurePropertiesFile));

			debugStrings = Boolean.parseBoolean(props.getProperty("debug.strings"));
			polygonClipping = Boolean.parseBoolean(props.getProperty("polygon.clipping"));
			wayNodePixelFilter = Boolean
					.parseBoolean(props.getProperty("waynode.pixel.filter"));
			wayNodeCompression = Boolean.parseBoolean(props.getProperty("waynode.compression"));

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

	private void writeBinaryFile() {
		try {
			writeHeaderForWholeFile();

			// begin of the part for lower zoom level
			startPosition = raf.getFilePointer();
			startPositionInFiveBytes = Serializer.getFiveBytes(startPosition);
			raf.seek(startLowerZoom);
			raf.write(startPositionInFiveBytes);
			raf.seek(startPosition);

			minZoom = (short) (upperLeftLow.zoomLevel - 2);
			maxZoom = (short) (upperLeftLow.zoomLevel + 3);
			writeHeaderAndIndex(upperLeftLow, bottomRightLow);
			writeTiles(upperLeftLow, bottomRightLow, true);

			endLowerZoom = raf.getFilePointer();
			raf.seek(subFileSizeLowZoom);
			long difference = endLowerZoom - startPosition;
			byte[] differenceInFiveBytes = Serializer.getFiveBytes(difference);
			raf.write(differenceInFiveBytes);
			raf.seek(endLowerZoom);

			// begin of the part for higher zoom level
			startPosition = raf.getFilePointer();
			startPositionInFiveBytes = Serializer.getFiveBytes(startPosition);
			raf.seek(startHigherZoom);
			raf.write(startPositionInFiveBytes);
			raf.seek(startPosition);

			minZoom = (short) (upperLeftHigh.zoomLevel - 2);
			maxZoom = (short) (upperLeftHigh.zoomLevel + 3);
			writeHeaderAndIndex(upperLeftHigh, bottomRightHigh);
			writeTiles(upperLeftHigh, bottomRightHigh, false);

			endHigherZoom = raf.getFilePointer();
			raf.seek(subFileSizeHighZoom);
			difference = endHigherZoom - startPosition;
			differenceInFiveBytes = Serializer.getFiveBytes(difference);
			raf.write(differenceInFiveBytes);
			raf.seek(endHigherZoom);

			long currentPosition = raf.getFilePointer();
			raf.seek(biggestTileSizePosition);
			// logger.info("biggest tile is written to: " + raf.getFilePointer());
			// logger.info("biggest tile size: " + biggestTileSize);
			raf.writeInt(biggestTileSize);
			// logger.info("biggest tile is written to file " + raf.getFilePointer());
			raf.seek(currentPosition);
			logger.info("end of file: " + raf.getFilePointer());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void writeHeaderForWholeFile() {
		try {
			int maxlat = -1;
			int minlon = -1;
			int minlat = -1;
			int maxlon = -1;
			long date = -1;
			int version = -1;
			short tilePixel = -1;

			String comment = mapFileComment;

			// get metadata for the map file
			rsBBoxCorners = conn.createStatement().executeQuery(SQL_GET_METADATA);
			while (rsBBoxCorners.next()) {
				maxlat = rsBBoxCorners.getInt("maxlat");
				minlon = rsBBoxCorners.getInt("minlon");
				minlat = rsBBoxCorners.getInt("minlat");
				maxlon = rsBBoxCorners.getInt("maxlon");
				date = rsBBoxCorners.getLong("date");
				version = rsBBoxCorners.getInt("import_version");
				zoom_level_high = rsBBoxCorners.getShort("zoom");
				zoom_level_low = rsBBoxCorners.getShort("zoom_low");
				tilePixel = rsBBoxCorners.getShort("tile_size");
				minZoom = rsBBoxCorners.getShort("min_zoom_level");
				maxZoom = rsBBoxCorners.getShort("max_zoom_level");
				minZoomLow = rsBBoxCorners.getShort("min_zoom_level_low");
				maxZoomLow = rsBBoxCorners.getShort("max_zoom_level_low");

				// calculate corner tiles of the bounding box of the given area
				upperLeftHigh = new Tile(MercatorProjection.longitudeToTileX(
						(double) minlon / 1000000, (byte) zoom_level_high), MercatorProjection
						.latitudeToTileY((double) maxlat / 1000000, (byte) zoom_level_high),
						(byte) zoom_level_high);
				bottomRightHigh = new Tile(MercatorProjection.longitudeToTileX(
						(double) maxlon / 1000000, (byte) zoom_level_high), MercatorProjection
						.latitudeToTileY((double) minlat / 1000000, (byte) zoom_level_high),
						(byte) zoom_level_high);

				upperLeftLow = new Tile(MercatorProjection.longitudeToTileX(
						(double) minlon / 1000000, (byte) zoom_level_low), MercatorProjection
						.latitudeToTileY((double) maxlat / 1000000, (byte) (zoom_level_low)),
						(byte) (zoom_level_low));
				bottomRightLow = new Tile(MercatorProjection.longitudeToTileX(
						(double) maxlon / 1000000, (byte) zoom_level_low), MercatorProjection
						.latitudeToTileY((double) minlat / 1000000, (byte) (zoom_level_low)),
						(byte) (zoom_level_low));

			}

			logger.info("writing header");

			// write file header
			// magic byte
			byte[] magicBytes = MAGIC_BYTE.getBytes();
			raf.write(magicBytes);

			// version number of the binary file format
			raf.writeInt(version);

			// amount of map files inside this file
			raf.writeByte(fileAmount);

			// width and height of a tile in pixel
			raf.writeShort(tilePixel);

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
			// logger.info("position of the value for the biggest tile: "
			// + biggestTileSizePosition);
			raf.seek(biggestTileSizePosition + 4);

			// comment
			if (!comment.equals("")) {
				raf.writeUTF(comment);
			} else {
				raf.writeUTF("no comment");
			}

			// basic zoom level (low)
			raf.writeByte(zoom_level_low);
			// minimal zoom level
			raf.writeByte((byte) minZoomLow);
			// maximal zoom level
			raf.writeByte((byte) maxZoomLow);
			// position of the begin of the index for the lower zoom level data is stored here
			startLowerZoom = raf.getFilePointer();
			raf.seek(raf.getFilePointer() + 5);
			subFileSizeLowZoom = raf.getFilePointer();
			raf.seek(raf.getFilePointer() + 5);

			// basic zoom level (high)
			raf.writeByte(zoom_level_high);
			// minimal zoom level
			raf.writeByte((byte) minZoom);
			// maximal zoom level
			raf.writeByte((byte) maxZoom);
			// position of the begin of the index for the higher zoom level data is stored here
			startHigherZoom = raf.getFilePointer();
			raf.seek(raf.getFilePointer() + 5);
			subFileSizeHighZoom = raf.getFilePointer();
			raf.seek(raf.getFilePointer() + 5);

			// create temporary zoom table
			conn.createStatement().execute(
					"CREATE TEMPORARY TABLE zoom_level_high(zoom_level smallint)");
			for (short s = (short) (zoom_level_high - 1); s < maxZoom; s++) {
				conn.createStatement().execute(
						"INSERT INTO zoom_level_high(zoom_level) VALUES (" + s + ")");
			}

			conn.createStatement().execute(
					"CREATE TEMPORARY TABLE zoom_level_low(zoom_level smallint)");
			for (short s = (short) (zoom_level_low - 1); s < maxZoomLow; s++) {
				conn.createStatement().execute(
						"INSERT INTO zoom_level_low(zoom_level) VALUES (" + s + ")");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private void writeHeaderAndIndex(Tile upperLeft, Tile bottomRight) {
		try {

			if (debugStrings) {
				String indexStart = "+++IndexStart+++";
				byte[] stringBytes = indexStart.getBytes();
				raf.write(stringBytes);
			}

			// write Index
			logger.info("+++IndexStart+++");
			// logger.info("filepointer " + raf.getFilePointer());

			long diffX = (bottomRight.x - upperLeft.x) + 1;
			long diffY = (bottomRight.y - upperLeft.y) + 1;
			long tileAmount = diffX * diffY;

			long bytesForIndex = tileAmount * 5;
			// logger.info("bytes needed for index: " + bytesForIndex);

			nextIndexValue = raf.getFilePointer();
			// logger.info("first index value at: " + nextIndexValue);

			raf.seek(raf.getFilePointer() + bytesForIndex);
			// logger.info("current filepointer " + raf.getFilePointer());

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void writeTiles(Tile upperLeft, Tile bottomRight, boolean writeLowerZoomLevel) {
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

			long firstWayInTile;

			THashMap<Short, Short> poiTableForTile;
			THashMap<Short, Short> wayTableForTile;
			short cumulatedCounterPois;
			short cumulatedCounterWays;

			long id;
			Byte layer;
			short tagAmount;
			String tags;
			String name;
			short nameLength;
			int elevation;
			String housenumber;
			int waynodesAmount;
			short wayType;
			short compressionType = 0;
			int labelPositionLatitude;
			int labelPositionLongitude;
			short innerWayAmount;
			short tileBitmask;

			long waySizePosition;

			long innerwayNodeCounterPos;
			short innerwayNodes = 0;
			byte innerWays;
			long innerWaysAmountPosition;

			long currentPosition;
			byte[] currentPositionInFiveBytes;
			long tileDiff;

			boolean hadResult = false;

			String[] tagStrings;

			byte[] stringBytes;
			int byteArrayLength;

			Map<String, Short> tagIdsPOIs = TagIdsPOIs.getMap();
			Map<String, Short> tagIdsWays = TagIdsWays.getMap();

			int tileCounter = 0;

			long tileStartAdress;
			int skipTillFirstWay;

			long startOfWay;

			Coordinate[] tileVertices;
			ArrayList<Coordinate> wayNodes = new ArrayList<Coordinate>();
			ArrayList<Integer> wayNodesArray = new ArrayList<Integer>();
			// int wayNodesArrayIndex;
			int maxDiffLat = 0;
			int maxDiffLon = 0;

			byte currentZoom;

			for (long tileY = upperLeft.y; tileY <= bottomRight.y; tileY++) {
				for (long tileX = upperLeft.x; tileX <= bottomRight.x; tileX++) {

					currentZoom = (byte) (upperLeft.zoomLevel + 3);

					tileVertices = Utils.getBoundingBox(tileX, tileY, bottomRight.zoomLevel,
							true).getEnvelope().getCoordinates();

					tileCounter++;

					// logger.info("before tile:" + raf.getFilePointer());
					currentPosition = raf.getFilePointer();
					currentPositionInFiveBytes = Serializer.getFiveBytes(currentPosition
							- startPosition);
					raf.seek(nextIndexValue);
					// logger.info("nextindexvalue " + nextIndexValue);
					raf.write(currentPositionInFiveBytes);
					nextIndexValue = raf.getFilePointer();
					// logger.info("new nextindexvalue " + nextIndexValue);
					raf.seek(currentPosition);
					// logger.info("new position " + raf.getFilePointer());
					if (previousTilePosition != 0) {
						// logger.info("previousTilePosition: " + previousTilePosition);
						tileDiff = raf.getFilePointer() - previousTilePosition;
						// logger.info("size of the previous tile " + tileDiff);
						if (tileDiff > biggestTileSize) {
							biggestTileSize = (int) tileDiff;
						}
					}
					previousTilePosition = currentPosition;
					// logger.info("previous tilePosition " + previousTilePosition);

					tileStartAdress = raf.getFilePointer();
					// logger.info("tilestart: " + tileStartAdress);

					tileStart = tileHead + tileX + "," + tileY + tileTail;
					logger.info(tileStart);

					// logger.info("write element zoom table");
					poiTableForTile = new THashMap<Short, Short>();
					wayTableForTile = new THashMap<Short, Short>();

					if (writeLowerZoomLevel) {
						// get amount of pois and ways grouped by zoom level for this certain
						// tile
						hadResult = false;
						pstmtPoisCountMinZoomLow.setLong(1, tileX);
						pstmtPoisCountMinZoomLow.setLong(2, tileY);
						pstmtPoisCountMinZoomLow.setShort(3, minZoom);
						rsPoisMinZoom = pstmtPoisCountMinZoomLow.executeQuery();

						while (rsPoisMinZoom.next()) {
							hadResult = true;
							poiTableForTile.put(minZoom, rsPoisMinZoom.getShort(1));

						}
						if (!hadResult) {
							poiTableForTile.put(minZoom, (short) 0);
						}

						hadResult = false;
						pstmtWaysCountMinZoomLow.setLong(1, tileX);
						pstmtWaysCountMinZoomLow.setLong(2, tileY);
						pstmtWaysCountMinZoomLow.setShort(3, minZoom);
						rsWaysMinZoom = pstmtWaysCountMinZoomLow.executeQuery();

						while (rsWaysMinZoom.next()) {
							hadResult = true;
							wayTableForTile.put(minZoom, rsWaysMinZoom.getShort(1));
						}

						if (!hadResult) {
							wayTableForTile.put(minZoom, (short) 0);
						}

						pstmtPoisZoomTableLow.setLong(1, tileX);
						pstmtPoisZoomTableLow.setLong(2, tileY);
						rsPoisZoomTable = pstmtPoisZoomTableLow.executeQuery();

						// join temporary zoom level table with filtered pois table
						while (rsPoisZoomTable.next()) {
							poiTableForTile.put(rsPoisZoomTable.getShort(1), rsPoisZoomTable
									.getShort(2));
						}

						pstmtWaysZoomTableLow.setLong(1, tileX);
						pstmtWaysZoomTableLow.setLong(2, tileY);
						rsWaysZoomTable = pstmtWaysZoomTableLow.executeQuery();
						while (rsWaysZoomTable.next()) {
							wayTableForTile.put(rsWaysZoomTable.getShort(1), rsWaysZoomTable
									.getShort(2));
						}

						hadResult = false;
						pstmtPoisCountMaxZoomLow.setLong(1, tileX);
						pstmtPoisCountMaxZoomLow.setLong(2, tileY);
						pstmtPoisCountMaxZoomLow.setShort(3, maxZoom);
						rsPoisMaxZoom = pstmtPoisCountMaxZoomLow.executeQuery();
						while (rsPoisMaxZoom.next()) {
							hadResult = true;
							poiTableForTile.put(maxZoom, rsPoisMaxZoom.getShort(1));
						}

						if (!hadResult) {
							poiTableForTile.put(maxZoom, (short) 0);
						}

						hadResult = false;
						pstmtWaysCountMaxZoomLow.setLong(1, tileX);
						pstmtWaysCountMaxZoomLow.setLong(2, tileY);
						pstmtWaysCountMaxZoomLow.setShort(3, maxZoom);
						rsWaysMaxZoom = pstmtWaysCountMaxZoomLow.executeQuery();

						while (rsWaysMaxZoom.next()) {
							hadResult = true;
							wayTableForTile.put(maxZoom, rsWaysMaxZoom.getShort(1));
						}

						if (!hadResult) {
							wayTableForTile.put(maxZoom, (short) 0);
						}
					} else {
						// get amount of pois and ways grouped by zoom level for this certain
						// tile
						hadResult = false;
						pstmtPoisCountMinZoom.setLong(1, tileX);
						pstmtPoisCountMinZoom.setLong(2, tileY);
						pstmtPoisCountMinZoom.setShort(3, minZoom);
						rsPoisMinZoom = pstmtPoisCountMinZoom.executeQuery();

						while (rsPoisMinZoom.next()) {
							hadResult = true;
							poiTableForTile.put(minZoom, rsPoisMinZoom.getShort(1));

						}
						if (!hadResult) {
							poiTableForTile.put(minZoom, (short) 0);
						}

						hadResult = false;
						pstmtWaysCountMinZoom.setLong(1, tileX);
						pstmtWaysCountMinZoom.setLong(2, tileY);
						pstmtWaysCountMinZoom.setShort(3, minZoom);
						rsWaysMinZoom = pstmtWaysCountMinZoom.executeQuery();

						while (rsWaysMinZoom.next()) {
							hadResult = true;
							wayTableForTile.put(minZoom, rsWaysMinZoom.getShort(1));
						}

						if (!hadResult) {
							wayTableForTile.put(minZoom, (short) 0);
						}

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

						hadResult = false;
						pstmtPoisCountMaxZoom.setLong(1, tileX);
						pstmtPoisCountMaxZoom.setLong(2, tileY);
						pstmtPoisCountMaxZoom.setShort(3, maxZoom);
						rsPoisMaxZoom = pstmtPoisCountMaxZoom.executeQuery();
						while (rsPoisMaxZoom.next()) {
							hadResult = true;
							poiTableForTile.put(maxZoom, rsPoisMaxZoom.getShort(1));
						}

						if (!hadResult) {
							poiTableForTile.put(maxZoom, (short) 0);
						}

						hadResult = false;
						pstmtWaysCountMaxZoom.setLong(1, tileX);
						pstmtWaysCountMaxZoom.setLong(2, tileY);
						pstmtWaysCountMaxZoom.setShort(3, maxZoom);
						rsWaysMaxZoom = pstmtWaysCountMaxZoom.executeQuery();

						while (rsWaysMaxZoom.next()) {
							hadResult = true;
							wayTableForTile.put(maxZoom, rsWaysMaxZoom.getShort(1));
						}

						if (!hadResult) {
							wayTableForTile.put(maxZoom, (short) 0);
						}
					}
					// cumulate the amount of pois and ways
					cumulatedCounterPois = 0;
					cumulatedCounterWays = 0;

					for (short min = minZoom; min <= maxZoom; min++) {
						cumulatedCounterPois += poiTableForTile.get(min);
						cumulatedCounterWays += wayTableForTile.get(min);
					}

					if (cumulatedCounterPois != 0 || cumulatedCounterWays != 0) {
						if (debugStrings) {
							// write tile header
							stringBytes = tileStart.getBytes();
							byteArrayLength = stringBytes.length;
							raf.write(stringBytes);

							if (byteArrayLength < 32) {
								while (byteArrayLength < 32) {
									raf.writeByte(32);
									byteArrayLength++;
								}
							}
						}
						cumulatedCounterPois = 0;
						cumulatedCounterWays = 0;
						for (short min = minZoom; min <= maxZoom; min++) {
							cumulatedCounterPois += poiTableForTile.get(min);
							cumulatedCounterWays += wayTableForTile.get(min);

							// logger.info("table:  " + min + " " + cumulatedCounterPois + " "
							// + cumulatedCounterWays);

							// write amount of pois and ways which should be read for a certain
							// zoom level
							raf.writeShort(cumulatedCounterPois);
							raf.writeShort(cumulatedCounterWays);
						}

						// get pointer to first way in this tile
						// logger.info("firstWayInTile: " + raf.getFilePointer());
						firstWayInTile = raf.getFilePointer();
						raf.seek(firstWayInTile + 4);

						if (writeLowerZoomLevel) {
							pstmtPoisForTileLow.setLong(1, tileX);
							pstmtPoisForTileLow.setLong(2, tileY);

							// get all pois for this tile ordered by zoom level
							rsPoisForTile = pstmtPoisForTileLow.executeQuery();

						} else {
							pstmtPoisForTile.setLong(1, tileX);
							pstmtPoisForTile.setLong(2, tileY);

							// get all pois for this tile ordered by zoom level
							rsPoisForTile = pstmtPoisForTile.executeQuery();
						}
						while (rsPoisForTile.next()) {

							if (debugStrings) {
								poiStart = poisHead + rsPoisForTile.getLong("id") + poisTail;
								// logger.info(poiStart);
								stringBytes = poiStart.getBytes();
								byteArrayLength = stringBytes.length;
								raf.write(stringBytes);

								// if string is not 32 byte long append whitespaces, byte value
								// = 32
								if (byteArrayLength < 32) {
									while (byteArrayLength < 32) {
										raf.writeByte(32);
										byteArrayLength++;
									}
								}
							}

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

							// write byte with layer and tag amount info
							raf.writeByte(buildLayerTagAmountByte(layer, tagAmount));

							// write tag ids to the file
							if (!tags.equals("")) {
								tagStrings = tags.split("\n");

								for (String tag : tagStrings) {
									raf.writeShort(tagIdsPOIs.get(tag));
								}
							}

							// write byte with bits set to 1 if the poi has a name, an elevation
							// or
							// a housenumber
							raf.writeByte(buildInfoByteForPOI(nameLength, elevation,
									housenumber.getBytes().length));

							if (nameLength != 0) {
								/** debug **/
								// logger.info("poi has name");
								/** debug **/
								raf.writeUTF(name);
							}

							if (elevation != 0) {
								/** debug **/
								// logger.info("poi has elevation");
								/** debug **/
								raf.writeShort(elevation);
							}

							if (housenumber.getBytes().length != 0) {
								/** debug **/
								// logger.info("poi has housenumber");
								/** debug **/
								raf.writeUTF(housenumber);
							}
						}

						// write position of the first way in the tile
						currentPosition = raf.getFilePointer();
						raf.seek(firstWayInTile);
						// logger.info("current: " + currentPosition + " start: "
						// + tileStartAdress);
						skipTillFirstWay = (int) (currentPosition - tileStartAdress);
						// logger.info("skipTillFirstWay: " + skipTillFirstWay);
						raf.writeInt(skipTillFirstWay);
						raf.seek(currentPosition);

						if (writeLowerZoomLevel) {
							// get ways
							pstmtWaysForTileLow.setLong(1, tileX);
							pstmtWaysForTileLow.setLong(2, tileY);

							// get all ways for this tile ordered by zoom level
							rsWaysForTile = pstmtWaysForTileLow.executeQuery();
						} else {
							// get ways
							pstmtWaysForTile.setLong(1, tileX);
							pstmtWaysForTile.setLong(2, tileY);

							// get all ways for this tile ordered by zoom level
							rsWaysForTile = pstmtWaysForTile.executeQuery();
						}
						while (rsWaysForTile.next()) {
							id = rsWaysForTile.getLong("id");
							wayStart = waysHead + id + waysTail;
							startOfWay = raf.getFilePointer();

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

							tagStrings = tags.split("\n");

							wayNodes.clear();
							pstmtWaynodes.setLong(1, id);
							rsWaynodes = pstmtWaynodes.executeQuery();
							while (rsWaynodes.next()) {
								int lat = rsWaynodes.getInt("latitude");
								int lon = rsWaynodes.getInt("longitude");
								double la = (double) lat / 1000000;
								double lo = (double) lon / 1000000;
								wayNodes.add(new Coordinate(la, lo));
							}

							if (writeLowerZoomLevel && wayNodePixelFilter) {
								wayNodes = Utils.compressWay(wayNodes, currentZoom);
							}

							waynodesAmount = wayNodes.size();

							if (wayType >= 2 && waynodesAmount >= 4 && name.equals("")
									&& polygonClipping) {
								for (int t = 0; t < tileVertices.length - 1; t++) {
									wayNodes = Utils.clipPolygonToTile(tileVertices[t],
											tileVertices[t + 1], wayNodes);
								}
								waynodesAmount = wayNodes.size();
							}

							if (wayNodeCompression) {
								wayNodesArray.clear();
								for (Coordinate c : wayNodes) {
									wayNodesArray.add((int) (c.x * 1000000));
									wayNodesArray.add((int) (c.y * 1000000));
								}

								wayNodesArray = Utils.compressWayDiffs(wayNodesArray, true);

								maxDiffLat = wayNodesArray.remove(wayNodesArray.size() - 2);
								maxDiffLon = wayNodesArray.remove(wayNodesArray.size() - 1);
							}

							if (debugStrings) {
								// write debug string
								wayStart = waysHead + id + waysTail;
								// logger.info(wayStart);
								stringBytes = wayStart.getBytes();
								byteArrayLength = stringBytes.length;
								raf.write(stringBytes);

								// if string is not 32 byte long append whitespaces, byte value
								// = 32
								if (byteArrayLength < 32) {
									while (byteArrayLength < 32) {
										raf.writeByte(32);
										byteArrayLength++;
									}
								}
							}

							// write way features
							waySizePosition = raf.getFilePointer();
							// logger.info("waySizePosition: " + waySizePosition);
							raf.seek(waySizePosition + 4);
							// logger.info("position after waysize: " + raf.getFilePointer());
							raf.writeShort(tileBitmask);

							// write byte with layer and tag amount
							raf.writeByte(buildLayerTagAmountByte(layer, tagAmount));

							compressionType = 0;
							if (wayNodeCompression) {
								if (maxDiffLat <= Byte.MAX_VALUE
										&& maxDiffLon <= Byte.MAX_VALUE)
									compressionType = 3;
								else if (maxDiffLat <= Short.MAX_VALUE
										&& maxDiffLon <= Short.MAX_VALUE)
									compressionType = 2;
								else if (maxDiffLat <= MAX_VALUE_THREE_BYTES
										&& maxDiffLon <= MAX_VALUE_THREE_BYTES)
									compressionType = 1;
							}

							// write byte with amount of tags which are rendered
							raf.writeByte(buildRenderTagWayNodeCompressionByte(tagStrings,
									compressionType));

							// write tag bitmap
							raf.writeByte(buildTagBitmapByte(tagStrings));

							// write tag ids
							for (String tag : tagStrings) {
								Short s = tagIdsWays.get(tag);
								if (s != null) {
									raf.writeShort(s);
								}
							}

							raf.writeShort(waynodesAmount);

							if (wayNodeCompression) {
								if (compressionType == 3) {
									raf.writeInt(wayNodesArray.get(0));
									raf.writeInt(wayNodesArray.get(1));
									for (int i = 2; i < wayNodesArray.size(); i += 2) {
										raf.writeByte(wayNodesArray.get(i));
										raf.writeByte(wayNodesArray.get(i + 1));
									}

								} else if (compressionType == 2) {
									raf.writeInt(wayNodesArray.get(0));
									raf.writeInt(wayNodesArray.get(1));
									for (int i = 2; i < wayNodesArray.size(); i += 2) {
										raf.writeShort(wayNodesArray.get(i));
										raf.writeShort(wayNodesArray.get(i + 1));
									}
								} else if (compressionType == 1) {
									raf.writeInt(wayNodesArray.get(0));
									raf.writeInt(wayNodesArray.get(1));
									for (int i = 2; i <= wayNodesArray.size() - 2; i += 2) {
										raf.write(Serializer.getSignedThreeBytes(wayNodesArray
												.get(i)));
										raf.write(Serializer.getSignedThreeBytes(wayNodesArray
												.get(i + 1)));
									}
								} else {
									// compressionType = 0;
									for (Coordinate c : wayNodes) {
										raf.writeInt((int) (c.x * 1000000));
										raf.writeInt((int) (c.y * 1000000));
									}
								}
							} else {
								for (Coordinate c : wayNodes) {
									raf.writeInt((int) (c.x * 1000000));
									raf.writeInt((int) (c.y * 1000000));
								}
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

								innerWaysAmountPosition = raf.getFilePointer();
								raf.seek(innerWaysAmountPosition + 1);
								innerWays = 0;
								for (int k = 1; k <= innerWayAmount; k++) {
									wayNodesArray.clear();
									innerwayNodeCounterPos = raf.getFilePointer();
									innerwayNodes = 0;
									pstmtMultipolygons.setLong(1, id);
									pstmtMultipolygons.setShort(2, (short) k);
									rsMultipolygons = pstmtMultipolygons.executeQuery();
									while (rsMultipolygons.next()) {
										wayNodesArray.add(rsMultipolygons.getInt("latitude"));
										wayNodesArray.add(rsMultipolygons.getInt("longitude"));
									}
									// System.out.println("innerwayNodes  " + innerwayNodes
									// + " id:" + id + " sequence " + k);

									if (!wayNodesArray.isEmpty()) {
										raf.seek(innerwayNodeCounterPos + 2);
										innerWays++;

										if (wayNodeCompression) {
											innerwayNodes = (short) wayNodes.size();
											wayNodesArray = Utils.compressWayDiffs(
													wayNodesArray, false);

											if (compressionType == 3) {
												raf.writeInt(wayNodesArray.get(0));
												raf.writeInt(wayNodesArray.get(1));
												for (int i = 2; i < wayNodesArray.size(); i += 2) {

													raf.writeByte(wayNodesArray.get(i));
													raf.writeByte(wayNodesArray.get(i + 1));
												}
											} else if (compressionType == 2) {
												raf.writeInt(wayNodesArray.get(0));
												raf.writeInt(wayNodesArray.get(1));
												for (int i = 2; i < wayNodesArray.size(); i += 2) {

													raf.writeShort(wayNodesArray.get(i));
													raf.writeShort(wayNodesArray.get(i + 1));
												}
											} else if (compressionType == 1) {
												raf.writeInt(wayNodesArray.get(0));
												raf.writeInt(wayNodesArray.get(1));
												for (int i = 2; i < wayNodesArray.size(); i += 2) {
													raf.write(Serializer
															.getSignedThreeBytes(wayNodesArray
																	.get(i)));
													raf.write(Serializer
															.getSignedThreeBytes(wayNodesArray
																	.get(i + 1)));
												}
											} else {
												// compressionType = 0;
												for (int i = 0; i < wayNodesArray.size(); i += 2) {
													raf.writeInt(wayNodesArray.get(i));
													raf.writeInt(wayNodesArray.get(i + 1));
												}
											}

										} else {
											innerwayNodes = (short) wayNodesArray.size();
											for (int i = 0; i < wayNodesArray.size(); i += 2) {
												raf.writeInt(wayNodesArray.get(i));
												raf.writeInt(wayNodesArray.get(i + 1));
											}
										}

										if (innerwayNodes != 0) {
											currentPosition = raf.getFilePointer();
											raf.seek(innerwayNodeCounterPos);
											// logger.info("innerwayNodes: " + innerwayNodes +
											// " "
											// + id);
											raf.writeShort(innerwayNodes);
											raf.seek(currentPosition);
										}
									}
								}
								if (innerWays != 0) {
									currentPosition = raf.getFilePointer();
									raf.seek(innerWaysAmountPosition);
									// logger.info("innerWays: " + innerWays + " " + id);
									raf.writeByte(innerWays);
									raf.seek(currentPosition);
								}
							}

							currentPosition = raf.getFilePointer();
							// logger.info("currentPosition after complete way "
							// + raf.getFilePointer());
							raf.seek(waySizePosition);
							// logger.info("waysizeposition " + raf.getFilePointer());
							raf.writeInt((int) (currentPosition - startOfWay));
							raf.seek(currentPosition);
							// logger.info("end of way: " + raf.getFilePointer());
						}
					}
				}
			}

			currentPosition = raf.getFilePointer();
			logger.info("file ends at: " + currentPosition);
			// compare size of last tile in file with the size of the biggest tile
			tileDiff = raf.getFilePointer() - previousTilePosition;
			if (tileDiff > biggestTileSize) {
				biggestTileSize = (int) tileDiff;
			}

			logger.info("total number of " + tileCounter + " tiles");
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
		short refLength = 0;

		if (nameLength != 0) {
			infoByte |= BITMAP_NAME;
		}
		if (refLength != 0) {
			infoByte |= BITMAP_REF;
		}
		if (labelPosLat != 0 && labelPosLon != 0) {
			infoByte |= BITMAP_LABEL;
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
	private byte buildRenderTagWayNodeCompressionByte(String[] tags, short wayNodeCompression) {
		byte infoByte = 0;
		short counter = 0;

		if (tags != null) {
			for (String tag : tags) {
				Byte b = whiteList.get(tag);
				if (b != null && b.equals(Byte.MAX_VALUE))
					counter++;
			}

			infoByte = (byte) (counter << 5);
		}

		if (wayNodeCompression == 0) {
			infoByte |= BITMAP_WAYNODECOMPRESSION_4_BYTE;
		}
		if (wayNodeCompression == 1) {
			infoByte |= BITMAP_WAYNODECOMPRESSION_3_BYTE;
		}
		if (wayNodeCompression == 2) {
			infoByte |= BITMAP_WAYNODECOMPRESSION_2_BYTE;
		}
		if (wayNodeCompression == 3) {
			infoByte |= BITMAP_WAYNODECOMPRESSION_1_BYTE;
		}

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
			int i = tag.indexOf("=");
			if (i == -1) {
				continue;
			}
			key = tag.substring(0, i);
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

	/**
	 * The main method to start the writing of a map data file.
	 * 
	 * @param args
	 *            command line arguments
	 * 
	 */
	public static void main(String[] args) {
		if (args.length < 4 || args.length > 5) {
			System.err
					.println("usage: BinaryFileWriter <properties-file> <target-file> <amount-of-sub-files> <feature-properties-file> (optional: <comment>)");
			System.exit(1);
		}

		propertiesFile = args[0];
		targetFile = args[1];
		fileAmount = Short.parseShort(args[2]);

		if (fileAmount > 2) {
			System.err.println("currently maximal two subfiles are supported");
			System.exit(1);
		}

		featurePropertiesFile = args[3];

		if (args.length == 5) {
			mapFileComment = args[4];
		}

		try {

			raf = new RandomAccessFile(targetFile, "rw");

			BinaryFileWriter bfw = new BinaryFileWriter(propertiesFile);
			bfw.writeBinaryFile();

			logger.info("processing took " + (System.currentTimeMillis() - startTime) / 1000
					+ "s.");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
}