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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.mapsforge.preprocessing.util.DBConnection;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

/**
 * Extracts information relevant for binary map file generation from the OSM-data and stores it
 * in a database. The schema for the database is defined in map_import.sql contained in the sql
 * folder. Currently the class expects the database to be PostgreSQL.
 * 
 * <b>Attention:</b> Truncates tables of target database before inserting new data.
 * 
 * @author bross
 * 
 *         adjusted by sschroet
 * 
 */

public class XML2PostgreSQLMap extends DefaultHandler {

	private static final Logger logger = Logger.getLogger(XML2PostgreSQLMap.class.getName());

	private static final String DEFAULT_BATCH_SIZE = "50000";

	private static final byte zoom = 14;

	private int batchSize;

	private static int batchSizeMP = 600;

	private static long DATE;

	private static final int VERSION = 1;

	private int nodes;
	private short node_tags;

	private int ways;
	private short way_tags;

	private int multipolygons;

	private MapElementNode currentNode;
	private Vector<String> currentTags;

	private MapElementWay currentWay;
	private Vector<Integer> currentWayNodes;

	private Vector<Integer> currentInnerWays;
	private Vector<Integer> currentOuterWays;

	private long startTime;

	// sql queries
	private final String SQL_INSERT_POIS_TMP = "INSERT INTO pois_tmp (id, latitude, longitude, name_length, name, tags_amount, tags, layer, elevation, housenumber) VALUES (?,?,?,?,?,?,?,?,?,?)";
	private final String SQL_INSERT_POIS = "INSERT INTO pois (id, latitude, longitude, name_length, name, tags_amount, tags, layer, elevation, housenumber) VALUES (?,?,?,?,?,?,?,?,?,?)";
	private final String SQL_INSERT_POI_TAG = "INSERT INTO pois_tags(poi_id, tag) VALUES (?,?)";
	private final String SQL_INSERT_WAYS = "INSERT INTO ways (id, name_length, name, tags_amount, tags, layer, waynodes_amount, way_type, convexness, label_pos_lat, label_pos_lon, inner_way_amount) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
	private final String SQL_INSERT_WAY_TAG = "INSERT INTO ways_tags(way_id, tag) VALUES (?,?)";
	private final String SQL_INSERT_MULTIPOLYGONS = "INSERT INTO multipolygons (outer_way_id, inner_way_sequence, waynode_sequence, latitude, longitude) VALUES (?,?,?,?,?)";
	private final String SQL_SELECT_WAYNODES = "SELECT latitude, longitude FROM pois_tmp WHERE id = ?";
	private final String SQL_INSERT_WAYNODES = "INSERT INTO waynodes (way_id, waynode_sequence, latitude, longitude) VALUES (?,?,?,?)";
	private final String SQL_INSERT_WAYNODES_TMP = "INSERT INTO waynodes_tmp (way_id, waynode_sequence, latitude, longitude) VALUES (?,?,?,?)";
	private final String SQL_SELECT_INNERWAYNODES = "SELECT latitude,longitude FROM waynodes_tmp WHERE way_id = ?";
	private final String SQL_INSERT_METADATA = "INSERT INTO metadata (maxlat,minlon,minlat,maxlon,date,import_version,zoom,tile_size,min_zoom_level,max_zoom_level) VALUES (?,?,?,?,?,?,?,?,?,?)";
	private final String SQL_SELECT_BBOX = "SELECT max(latitude) as maxlat,min(longitude) as minlon,min(latitude) as minlat,max(longitude) as maxlon from pois_tmp";
	private final String SQL_UPDATE_WAYTYPE_FLAG = "UPDATE ways SET way_type = 3 WHERE id = ?";
	private final String SQL_UPDATE_OPEN_WAYTYPE_FLAG = "UPDATE ways SET way_type = 0 WHERE id = ?";
	private final String SQL_INSERT_POI_TILE = "INSERT INTO pois_to_tiles (poi_id,tile_x,tile_y,zoom_level) VALUES (?,?,?,?)";
	private final String SQL_INSERT_WAY_TILE = "INSERT INTO ways_to_tiles (way_id,tile_x,tile_y,tile_bitmask,zoom_level) VALUES (?,?,?,?,?)";
	private final String SQL_DISTINCT_WAY_TILES = "SELECT DISTINCT tile_x,tile_y FROM ways_to_tiles";
	private final String SQL_SELECT_WAY_IDS = "SELECT w.id,w.waynodes_amount,w.way_type FROM ways_to_tiles wtt JOIN ways w ON (wtt.way_id = w.id) WHERE wtt.tile_x = ? AND wtt.tile_y = ?";
	private final String SQL_SELECT_WAYS_FOR_TILE = "SELECT latitude,longitude FROM waynodes_tmp WHERE way_id = ? order by waynode_sequence";
	private final String SQL_UPDATE_WAY_TILE = "UPDATE ways_to_tiles SET tile_bitmask = ? WHERE way_id = ? and tile_x = ? and tile_y = ?";
	private final String SQL_SELECT_WAY_WITH_TAGS = "SELECT * FROM ways WHERE id = ?";
	private final String SQL_WAY_NODE_DIFF = "INSERT INTO waynodes_diff (way_id,waynode_sequence,diff_lat,diff_lon) VALUES (?,?,?,?)";
	private final String SQL_UPDATE_INNER_WAY_AMOUNT = "UPDATE ways SET inner_way_amount = ? WHERE id = ?";

	private boolean poisBatched = false;
	private boolean waysBatched = false;

	// prepared statements
	private PreparedStatement pstmtPoisTmp;
	private PreparedStatement pstmtSelectWayNodes;
	private PreparedStatement pstmtInsertWayNodes;
	private PreparedStatement pstmtInsertWayNodesTmp;
	private PreparedStatement pstmtWays;
	private PreparedStatement pstmtWayTag;
	private PreparedStatement pstmtMultipolygons;
	private PreparedStatement pstmtInnerWays;
	private PreparedStatement pstmtMetadata;
	private PreparedStatement pstmtBoundingBox;
	private PreparedStatement pstmtUpdateWayType;
	private PreparedStatement pstmtUpdateOpenWayType;
	private PreparedStatement pstmtPoisTiles;
	private PreparedStatement pstmtWaysTiles;
	private PreparedStatement pstmtPoisTags;
	private PreparedStatement pstmtPoisTag;
	private PreparedStatement pstmtWaysWithTags;
	private PreparedStatement pstmtWayNodeDiff;
	private PreparedStatement pstmtUpdateWayInnerWayAmount;

	// result sets
	private ResultSet rsWayNodes;
	private ResultSet rsInnerWays;
	private ResultSet rsBoundingBox;
	private ResultSet rsWaysWithTags;

	private Connection conn;

	private String tmpTags;

	private Map<String, Byte> poiTagWhiteList;
	private Map<String, Byte> wayTagWhiteList;

	private int maxlat;
	private int minlon;
	private int minlat;
	private int maxlon;

	Geometry geoWay;
	private Set<Tile> wayTiles;
	private Map<Tile, Short> waySubTiles;
	private Set<Entry<Tile, Short>> wayTilesEntries;

	public XML2PostgreSQLMap(String propertiesFile) throws Exception {

		Properties props = new Properties();
		props.load(new FileInputStream(propertiesFile));

		DBConnection dbConnection = new DBConnection(propertiesFile);

		currentTags = new Vector<String>();
		currentWayNodes = new Vector<Integer>();
		currentOuterWays = new Vector<Integer>();
		currentInnerWays = new Vector<Integer>();

		wayTiles = new HashSet<Tile>();

		// batchSize = Integer.parseInt(props.getProperty("xml2postgresql.batchSize",
		// DEFAULT_BATCH_SIZE));

		batchSize = 50000;

		conn = dbConnection.getConnection();

		conn.setAutoCommit(false);

		logger.info("truncating tables");

		conn.createStatement().execute("TRUNCATE TABLE waynodes_diff CASCADE");
		conn.createStatement().execute("TRUNCATE TABLE multipolygons CASCADE");
		conn.createStatement().execute("TRUNCATE TABLE waynodes CASCADE");
		conn.createStatement().execute("TRUNCATE TABLE ways CASCADE");
		conn.createStatement().execute("TRUNCATE TABLE pois CASCADE");
		conn.createStatement().execute("TRUNCATE TABLE metadata CASCADE");
		conn.createStatement().execute("TRUNCATE TABLE pois_to_tiles CASCADE");
		conn.createStatement().execute("TRUNCATE TABLE ways_to_tiles CASCADE");
		conn.createStatement().execute("TRUNCATE TABLE pois_tags CASCADE");
		conn.createStatement().execute("TRUNCATE TABLE ways_tags CASCADE");

		conn.createStatement().execute("DROP INDEX IF EXISTS pois_tags_idx");
		conn.createStatement().execute("DROP INDEX IF EXISTS ways_tags_idx");
		conn.createStatement().execute("DROP INDEX IF EXISTS pois_to_tiles_idx");
		conn.createStatement().execute("DROP INDEX IF EXISTS ways_to_tiles_idx");
		conn.createStatement().execute("DROP INDEX IF EXISTS pois_to_tiles_id_idx");
		conn.createStatement().execute("DROP INDEX IF EXISTS ways_to_tiles_id_idx");
		conn.createStatement().execute("DROP INDEX IF EXISTS ways_to_tiles_id_tile_idx");
		conn.createStatement().execute("DROP INDEX IF EXISTS waynodes_id_idx");
		conn.createStatement().execute("DROP INDEX IF EXISTS waynodes_id_sequence_idx");
		conn.createStatement().execute("DROP INDEX IF EXISTS multipolygons_idx");

		conn.commit();

		conn.createStatement().execute("SET CONSTRAINTS ALL DEFERRED");

		// create temporary tables
		conn.createStatement().execute(
				"CREATE TEMPORARY TABLE pois_tmp (id bigint NOT NULL,"
						+ "latitude bigint,longitude bigint,name_length smallint,"
						+ "name text,tags_amount smallint,tags text,layer smallint,"
						+ "elevation integer,housenumber text)");

		conn.createStatement().execute("ALTER TABLE pois_tmp OWNER TO sschroet");

		conn.createStatement().execute(
				"ALTER TABLE ONLY pois_tmp ADD CONSTRAINT pois_tmp_pkey PRIMARY KEY (id)");

		conn.createStatement().execute(
				"CREATE TEMPORARY TABLE waynodes_tmp (way_id bigint,"
						+ "waynode_sequence smallint,latitude bigint,longitude bigint)");

		conn.createStatement().execute("ALTER TABLE waynodes_tmp OWNER TO sschroet");

		conn
				.createStatement()
				.execute(
						"ALTER TABLE ONLY waynodes_tmp ADD CONSTRAINT waynodes_tmp_pkey PRIMARY KEY (way_id,waynode_sequence)");

		pstmtPoisTmp = conn.prepareStatement(SQL_INSERT_POIS_TMP);

		pstmtSelectWayNodes = conn.prepareStatement(SQL_SELECT_WAYNODES);

		pstmtInsertWayNodes = conn.prepareStatement(SQL_INSERT_WAYNODES);

		pstmtInsertWayNodesTmp = conn.prepareStatement(SQL_INSERT_WAYNODES_TMP);

		pstmtWays = conn.prepareStatement(SQL_INSERT_WAYS);

		pstmtWayTag = conn.prepareStatement(SQL_INSERT_WAY_TAG);

		pstmtMultipolygons = conn.prepareStatement(SQL_INSERT_MULTIPOLYGONS);

		pstmtInnerWays = conn.prepareStatement(SQL_SELECT_INNERWAYNODES);

		pstmtMetadata = conn.prepareStatement(SQL_INSERT_METADATA);

		pstmtBoundingBox = conn.prepareStatement(SQL_SELECT_BBOX);

		pstmtUpdateWayType = conn.prepareStatement(SQL_UPDATE_WAYTYPE_FLAG);

		pstmtUpdateOpenWayType = conn.prepareStatement(SQL_UPDATE_OPEN_WAYTYPE_FLAG);

		pstmtPoisTiles = conn.prepareStatement(SQL_INSERT_POI_TILE);

		pstmtWaysTiles = conn.prepareStatement(SQL_INSERT_WAY_TILE);

		pstmtPoisTags = conn.prepareStatement(SQL_INSERT_POIS);

		pstmtPoisTag = conn.prepareStatement(SQL_INSERT_POI_TAG);

		pstmtWaysWithTags = conn.prepareStatement(SQL_SELECT_WAY_WITH_TAGS);

		pstmtWayNodeDiff = conn.prepareStatement(SQL_WAY_NODE_DIFF);

		pstmtUpdateWayInnerWayAmount = conn.prepareStatement(SQL_UPDATE_INNER_WAY_AMOUNT);

		logger.info("database connection setup done...");

	}

	@Override
	protected void finalize() throws Throwable {
		if (!conn.isClosed())
			conn.close();
	}

	@Override
	public void startDocument() throws SAXException {
		super.startDocument();

		poiTagWhiteList = WhiteList.getNodeTagWhitelist();
		wayTagWhiteList = WhiteList.getWayTagWhitelist();

		logger.info("start reading file");
		startTime = System.currentTimeMillis();
	}

	@Override
	public void endDocument() throws SAXException {
		super.endDocument();

		logger.info("executing last batches...");

		try {
			// write metadata into the database

			rsBoundingBox = pstmtBoundingBox.executeQuery();

			// get bounding box coordinates
			while (rsBoundingBox.next()) {
				maxlat = rsBoundingBox.getInt("maxlat");
				minlon = rsBoundingBox.getInt("minlon");
				minlat = rsBoundingBox.getInt("minlat");
				maxlon = rsBoundingBox.getInt("maxlon");
			}

			pstmtMetadata.setInt(1, maxlat);
			pstmtMetadata.setInt(2, minlon);
			pstmtMetadata.setInt(3, minlat);
			pstmtMetadata.setInt(4, maxlon);
			pstmtMetadata.setLong(5, DATE);
			pstmtMetadata.setInt(6, VERSION);
			pstmtMetadata.setInt(7, zoom);
			pstmtMetadata.setInt(8, Tile.TILE_SIZE);
			pstmtMetadata.setShort(9, (short) 12);
			pstmtMetadata.setShort(10, (short) 17);
			pstmtMetadata.execute();

			logger.info("metadata inserted");

			logger.info("execute last multipolygon batches");

			// execute very last batches
			pstmtMultipolygons.executeBatch();
			pstmtUpdateWayType.executeBatch();
			pstmtUpdateWayInnerWayAmount.executeBatch();

			logger.info("create indices on tag tables");

			// create indices

			// pois/ways_tags
			conn.createStatement().execute("CREATE INDEX pois_tags_idx ON pois_tags (tag)");
			conn.createStatement().execute("CREATE INDEX ways_tags_idx ON ways_tags(tag)");

			// pois/ways_to_tiles
			conn.createStatement().execute(
					"CREATE INDEX pois_to_tiles_idx ON pois_to_tiles(tile_x,tile_y)");
			conn.createStatement().execute(
					"CREATE INDEX ways_to_tiles_idx ON ways_to_tiles(tile_x,tile_y)");

			conn.createStatement().execute(
					"CREATE INDEX pois_to_tiles_id_idx ON pois_to_tiles(poi_id)");
			conn.createStatement().execute(
					"CREATE INDEX ways_to_tiles_id_idx ON ways_to_tiles(way_id)");

			conn
					.createStatement()
					.execute(
							"CREATE INDEX ways_to_tiles_id_tile_idx ON ways_to_tiles(way_id, tile_x, tile_y)");

			// waynodes
			conn.createStatement().execute("CREATE INDEX waynodes_id_idx ON waynodes(way_id)");
			conn
					.createStatement()
					.execute(
							"CREATE INDEX waynodes_id_sequence_idx ON waynodes(way_id,waynode_sequence)");

			// multipolygons
			conn.createStatement().execute(
					"CREATE INDEX multipolygons_idx ON multipolygons(outer_way_id)");

			logger.info("created indices on tag tables");

			// truncate temporary tables
			conn.createStatement().execute("TRUNCATE TABLE waynodes_tmp CASCADE");
			conn.createStatement().execute("TRUNCATE TABLE pois_tmp CASCADE");

			logger.info("committing transaction...");

			conn.commit();

		} catch (SQLException e) {
			logger.info("SQLException: " + e.getMessage());
			while ((e = e.getNextException()) != null) {
				logger.info(e.getMessage());
			}
		}

		logger
				.info("processing took " + (System.currentTimeMillis() - startTime) / 1000
						+ "s.");

		logger.info("inserted " + nodes + " nodes");
		logger.info("inserted " + ways + " ways");
		logger.info("inserted " + multipolygons + " multipolygons");
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes)
			throws SAXException {

		// get bounding box coordinates
		if (qName.equals("bounds")) {
			maxlat = (int) (Double.parseDouble(attributes.getValue("maxlat")) * 1000000);
			minlon = (int) (Double.parseDouble(attributes.getValue("minlon")) * 1000000);
			minlat = (int) (Double.parseDouble(attributes.getValue("minlat")) * 1000000);
			maxlon = (int) (Double.parseDouble(attributes.getValue("maxlon")) * 1000000);
		}

		if (qName.equals("node")) {
			currentTags.clear();

			// get id and coordinates
			long id = Long.parseLong(attributes.getValue("id"));
			int latitude = (int) (Double.parseDouble(attributes.getValue("lat")) * 1000000);
			int longitude = (int) (Double.parseDouble(attributes.getValue("lon")) * 1000000);
			currentNode = new MapElementNode(id, latitude, longitude);

		} else if (qName.equals("way")) {
			// execute last poi batches
			if (!poisBatched) {
				try {
					pstmtPoisTmp.executeBatch();
					pstmtPoisTags.executeBatch();
					pstmtPoisTiles.executeBatch();
					pstmtPoisTag.executeBatch();
				} catch (SQLException e) {
					System.err.println(pstmtPoisTmp);
					e.printStackTrace();
				}
				poisBatched = true;
			}

			currentTags.clear();
			currentWayNodes.clear();

			// get id
			currentWay = new MapElementWay(Integer.parseInt(attributes.getValue("id")));

		} else if (qName.equals("relation")) {
			// execute last way batches
			if (!waysBatched) {
				try {
					pstmtWays.executeBatch();
					pstmtInsertWayNodes.executeBatch();
					pstmtInsertWayNodesTmp.executeBatch();
					pstmtWaysTiles.executeBatch();
					pstmtWayNodeDiff.executeBatch();
					pstmtWayTag.executeBatch();
					logger.info("last ways batched");
				} catch (SQLException e) {
					System.err.println(pstmtWays);
					System.err.println(pstmtInsertWayNodes);
					e.printStackTrace();
					if (e.getNextException() != null) {
						System.out.println(e.getNextException().getMessage());
					}
				}
				waysBatched = true;
			}
			currentOuterWays.clear();
			currentInnerWays.clear();
		} else if (qName.equals("tag")) {
			if (!"created_by".equals(attributes.getValue("k"))) {
				tmpTags = attributes.getValue("k");
				tmpTags += "=";
				tmpTags += attributes.getValue("v");
				currentTags.add(tmpTags);
			}
		} else if (qName.equals("nd")) {
			currentWayNodes.add(Integer.parseInt(attributes.getValue("ref")));
		} else if (qName.equals("member")) {
			// only multipolygons with outer and inner ways are considered
			if (attributes.getValue("role").equals("outer")) {
				currentOuterWays.add(Integer.parseInt(attributes.getValue("ref")));
			} else if (attributes.getValue("role").equals("inner")) {
				currentInnerWays.add(Integer.parseInt(attributes.getValue("ref")));
			}
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		try {
			String tag;
			Iterator<String> tagIterator;
			int tagSize;
			String[] splittedTags;
			Vector<String> tempTags;
			String storedTags = "";
			short nameLength;
			int wnSize;
			int sequence = 1;
			int lat = 0;
			int lon = 0;

			int firstLat = 0;
			int firstLon = 0;
			int size;
			int innersize;
			long outer;
			int[] innerNodes;
			int waytype;

			double latDouble;
			double lonDouble;

			if (qName.equals("node")) {

				nodes++;
				pstmtPoisTmp.setLong(1, currentNode.id);
				pstmtPoisTmp.setInt(2, currentNode.latitude);
				pstmtPoisTmp.setInt(3, currentNode.longitude);

				tagIterator = currentTags.iterator();
				tagSize = currentTags.size();
				splittedTags = new String[2];

				tempTags = new Vector<String>();
				node_tags = 0;
				while (tagIterator.hasNext()) {
					tag = tagIterator.next();
					splittedTags = tag.split("=");

					// special tags like elevation, housenumber, name and layer are stored in
					// separate fields in the database
					if (splittedTags[0].equals("ele")) {
						try {
							currentNode.elevation = (int) Double.parseDouble(splittedTags[1]);
							if (currentNode.elevation < 32000) {
								currentNode.elevation = 0;
							}
						} catch (NumberFormatException e) {
							currentNode.elevation = 0;
						}
					} else if (splittedTags[0].equals("addr:housenumber")) {
						currentNode.housenumber = splittedTags[1];
					} else if (splittedTags[0].equals("name")) {
						currentNode.name = splittedTags[1];
					} else if (splittedTags[0].equals("layer")) {
						try {
							currentNode.layer = Integer.parseInt(splittedTags[1]) + 5;
						} catch (NumberFormatException e) {
							currentNode.layer = 5;
						}
					} else if (poiTagWhiteList.containsKey(tag)) {
						// if current tag is in the white list, add it to the temporary tag list
						currentNode.zoomLevel = poiTagWhiteList.get(tag);
						if (currentNode.zoomLevel == 127) {
							currentNode.zoomLevel = zoom;
						}
						tempTags.add(tag);

						node_tags++;
					}
					tagSize--;
				}

				// create one string of tags. tags are separated with a line break
				for (int k = 0; k < node_tags; k++) {
					storedTags += tempTags.get(k);
					storedTags += "\n";
				}

				if (currentNode.name != null) {
					nameLength = (short) currentNode.name.getBytes().length;
				} else {
					nameLength = 0;
				}

				// all nodes are stored in the temporary poi table
				pstmtPoisTmp.setInt(4, nameLength);
				pstmtPoisTmp.setString(5, currentNode.name);
				pstmtPoisTmp.setShort(6, node_tags);
				pstmtPoisTmp.setString(7, storedTags);
				pstmtPoisTmp.setInt(8, currentNode.layer);
				pstmtPoisTmp.setInt(9, currentNode.elevation);
				pstmtPoisTmp.setString(10, currentNode.housenumber);
				pstmtPoisTmp.addBatch();

				// if the node has tags it is a poi and it is stored in the persistent poi table
				if (!storedTags.equals("")) {
					pstmtPoisTags.setLong(1, currentNode.id);
					pstmtPoisTags.setInt(2, currentNode.latitude);
					pstmtPoisTags.setInt(3, currentNode.longitude);
					pstmtPoisTags.setShort(4, nameLength);
					pstmtPoisTags.setString(5, currentNode.name);
					pstmtPoisTags.setShort(6, node_tags);
					pstmtPoisTags.setString(7, storedTags);
					pstmtPoisTags.setInt(8, currentNode.layer);
					pstmtPoisTags.setInt(9, currentNode.elevation);
					pstmtPoisTags.setString(10, currentNode.housenumber);
					pstmtPoisTags.addBatch();

					// to execute fast filtering of elements every tag is stored together with
					// the element id in a tag table
					String[] oneTag = storedTags.split("\n");
					for (String s : oneTag) {
						pstmtPoisTag.setLong(1, currentNode.id);
						pstmtPoisTag.setString(2, s);
						pstmtPoisTag.addBatch();
					}

					// calculate the tile in which the poi is located
					Tile mainTileForPOI = new Tile(MercatorProjection.longitudeToTileX(
							(double) currentNode.longitude / 1000000, zoom), MercatorProjection
							.latitudeToTileY((double) currentNode.latitude / 1000000, zoom),
							zoom);

					pstmtPoisTiles.setLong(1, currentNode.id);
					pstmtPoisTiles.setLong(2, mainTileForPOI.x);
					pstmtPoisTiles.setLong(3, mainTileForPOI.y);
					pstmtPoisTiles.setShort(4, currentNode.zoomLevel);
					pstmtPoisTiles.addBatch();
				}

				poisBatched = false;

				if (nodes % batchSize == 0) {
					pstmtPoisTmp.executeBatch();
					pstmtPoisTags.executeBatch();
					pstmtPoisTiles.executeBatch();
					pstmtPoisTag.executeBatch();
					logger
							.info("executed batch for nodes " + (nodes - batchSize) + "-"
									+ nodes);
				}

			} else if (qName.equals("way")) {

				ways++;

				tagIterator = currentTags.iterator();
				tagSize = currentTags.size();
				tempTags = new Vector<String>();
				way_tags = 0;
				splittedTags = new String[2];

				while (tagIterator.hasNext()) {
					tag = tagIterator.next();
					splittedTags = tag.split("=");

					// special tags like elevation, housenumber, name and layer are stored in
					// separate fields in the database
					if (splittedTags[0].equals("name")) {
						currentWay.name = splittedTags[1];
					} else if (splittedTags[0].equals("layer")) {
						try {
							currentWay.layer = (byte) (Integer.parseInt(splittedTags[1]) + 5);
						} catch (NumberFormatException e) {
							currentWay.layer = 5;
						}
					} else if (wayTagWhiteList.containsKey(tag)) {
						// if current tag is in the white list, add it to the temporary tag list
						currentWay.zoomLevel = wayTagWhiteList.get(tag);
						if (currentWay.zoomLevel == 127) {
							currentWay.zoomLevel = zoom;
						}
						tempTags.add(tag);

						way_tags++;
					}
					tagSize--;
				}

				// create one string of tags. tags are separated with a line break
				for (int k = 0; k < way_tags; k++) {
					storedTags += tempTags.get(k);
					storedTags += "\n";
				}

				wnSize = currentWayNodes.size();

				if (wnSize > 1) {
					// mark the way as area if the first and the last way node are the same and
					// if the way has more than two way nodes
					if (currentWayNodes.get(0).equals(currentWayNodes.get(wnSize - 1))
							&& !(currentWayNodes.size() == 2)) {
						currentWay.wayType = 2;
					}

					Coordinate[] wayNodes = new Coordinate[wnSize];

					// get the way nodes from the temporary poi table
					for (int i = 0; i < wnSize; i++) {
						pstmtSelectWayNodes.setInt(1, currentWayNodes.get(i));
						rsWayNodes = pstmtSelectWayNodes.executeQuery();

						while (rsWayNodes.next()) {
							lat = rsWayNodes.getInt("latitude");
							lon = rsWayNodes.getInt("longitude");

							if (i == 0) {
								firstLat = lat;
								firstLon = lon;
							}

							latDouble = (double) lat / 1000000;
							lonDouble = (double) lon / 1000000;

							wayNodes[i] = (new Coordinate(latDouble, lonDouble));

							// store all way nodes in a temporary way node table
							pstmtInsertWayNodesTmp.setLong(1, currentWay.id);
							pstmtInsertWayNodesTmp.setInt(2, sequence);
							pstmtInsertWayNodesTmp.setInt(3, lat);
							pstmtInsertWayNodesTmp.setInt(4, lon);
							pstmtInsertWayNodesTmp.addBatch();

							if (storedTags != "") {
								// if the way has tags store the way nodes in a persistent way
								// node table
								pstmtInsertWayNodes.setLong(1, currentWay.id);
								pstmtInsertWayNodes.setInt(2, sequence);
								pstmtInsertWayNodes.setInt(3, lat);
								pstmtInsertWayNodes.setInt(4, lon);
								pstmtInsertWayNodes.addBatch();

								// for development: calculate the differences between the first
								// and all following way nodes
								pstmtWayNodeDiff.setLong(1, currentWay.id);
								pstmtWayNodeDiff.setInt(2, sequence);
								// System.out.println((Math.abs(firstLat - lat)));
								// System.out.println((Math.abs(firstLon - lon)));
								pstmtWayNodeDiff.setInt(3, (Math.abs(firstLat - lat)));
								pstmtWayNodeDiff.setInt(4, (Math.abs(firstLon - lon)));
								pstmtWayNodeDiff.addBatch();
							}
						}
						sequence++;
					}

					// store only ways with tags in the persistent table
					if (storedTags != "") {
						// to execute fast filtering of elements every tag is stored together
						// with the element id in a tag table
						String[] oneTag = storedTags.split("\n");
						for (String s : oneTag) {
							pstmtWayTag.setLong(1, currentWay.id);
							pstmtWayTag.setString(2, s);
							pstmtWayTag.addBatch();
						}

						// create a geometry object for the way
						geoWay = Utils.createWay(currentWay, wayNodes);

						// calculate all tiles which are related to a way
						wayTiles = Utils.wayToTilesWay(geoWay, currentWay.wayType, zoom);

						// calculate all subtiles of the tiles which are related to the way and
						// create the tile bitmask
						waySubTiles = Utils
								.getTileBitMask(geoWay, wayTiles, currentWay.wayType);

						wayTilesEntries = waySubTiles.entrySet();
						for (Entry<Tile, Short> entry : wayTilesEntries) {
							pstmtWaysTiles.setLong(1, currentWay.id);
							pstmtWaysTiles.setLong(2, entry.getKey().x);
							pstmtWaysTiles.setLong(3, entry.getKey().y);
							pstmtWaysTiles.setInt(4, entry.getValue());
							pstmtWaysTiles.addBatch();
						}

						pstmtWays.setLong(1, currentWay.id);
						if (currentWay.name != null) {
							pstmtWays.setInt(2, currentWay.name.getBytes().length);
						} else {
							pstmtWays.setInt(2, 0);
						}
						pstmtWays.setString(3, currentWay.name);
						pstmtWays.setInt(4, way_tags);
						pstmtWays.setString(5, storedTags);
						pstmtWays.setInt(6, currentWay.layer);
						pstmtWays.setInt(7, wnSize);
						pstmtWays.setInt(8, currentWay.wayType);
						pstmtWays.setInt(9, currentWay.convexness);
						// TODO calculate label position
						pstmtWays.setInt(10, 0);
						pstmtWays.setInt(11, 0);
						// inner way amount if way is outer way of a multipolygon
						pstmtWays.setInt(12, 0);
						pstmtWays.addBatch();
						waysBatched = false;
					}
				}

				if (ways % batchSize == 0) {
					pstmtWays.executeBatch();
					pstmtInsertWayNodes.executeBatch();
					pstmtInsertWayNodesTmp.executeBatch();
					pstmtWayNodeDiff.executeBatch();
					pstmtWaysTiles.executeBatch();
					pstmtWayTag.executeBatch();
					logger.info("executed batch for ways " + (ways - batchSize) + "-" + ways);
				}
			} else if (qName.equals("relation")) {

				size = currentOuterWays.size();

				if (size != 0) {
					for (int i = 0; i < size; i++) {
						// a multipolygon could have multiple outer ways
						outer = currentOuterWays.get(i);

						pstmtWaysWithTags.setLong(1, outer);
						rsWaysWithTags = pstmtWaysWithTags.executeQuery();

						if (rsWaysWithTags.next()) {
							waytype = rsWaysWithTags.getInt("way_type");

							// if (waytype == 2) {
							// // outer way is a closed way
							// pstmtUpdateWayType.setLong(1, outer);
							// pstmtUpdateWayType.addBatch();
							// } else if (waytype == 1) {
							// // outer way is a "normal" way
							// pstmtUpdateOpenWayType.setLong(1, outer);
							// pstmtUpdateOpenWayType.addBatch();
							// }

							// get inner way nodes
							innersize = currentInnerWays.size();
							innerNodes = new int[innersize];
							for (int j = 0; j < innersize; j++) {
								pstmtInnerWays.setInt(1, currentInnerWays.get(j));
								rsInnerWays = pstmtInnerWays.executeQuery();
								sequence = 1;
								while (rsInnerWays.next()) {
									try {
										pstmtMultipolygons.setLong(1, outer);
										pstmtMultipolygons.setInt(2, j + 1);
										pstmtMultipolygons.setInt(3, sequence);
										pstmtMultipolygons.setLong(4, rsInnerWays
												.getInt("latitude"));
										pstmtMultipolygons.setLong(5, rsInnerWays
												.getInt("longitude"));
										pstmtMultipolygons.addBatch();
										sequence++;
									} catch (SQLException e) {

										System.err.println(pstmtMultipolygons);
										throw e;
									}
								}
								innerNodes[j] = sequence - 1;
							}

							boolean noInnerNodes = true;
							for (int k = 0; k < innerNodes.length; k++) {
								// if inner ways have no inner waynodes don't mark the outer way
								// as multipolygon
								if (innerNodes[k] == 0) {
									noInnerNodes = noInnerNodes && true;
								} else {
									noInnerNodes = noInnerNodes && false;
								}
							}

							// set inner way amount for every outer way
							if (!noInnerNodes) {
								pstmtUpdateWayInnerWayAmount.setInt(1, innersize);
								pstmtUpdateWayInnerWayAmount.setLong(2, outer);
								pstmtUpdateWayInnerWayAmount.addBatch();
								if (waytype == 2) {
									// outer way is a closed way
									pstmtUpdateWayType.setLong(1, outer);
									pstmtUpdateWayType.addBatch();
								} else if (waytype == 1) {
									// outer way is a "normal" way
									pstmtUpdateOpenWayType.setLong(1, outer);
									pstmtUpdateOpenWayType.addBatch();
								}
							}
						}
						multipolygons++;
					}
				}
				if (multipolygons % batchSizeMP == 0) {
					pstmtMultipolygons.executeBatch();
					pstmtUpdateWayType.executeBatch();
					pstmtUpdateWayInnerWayAmount.executeBatch();
					logger.info("executed batch for multipolygons "
							+ (multipolygons - batchSizeMP) + "-" + multipolygons);
				}

			}

		} catch (SQLException e) {
			e.printStackTrace();
			if (e.getNextException() != null) {
				System.out.println(e.getNextException().getMessage());
			}
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
	}

	private static void usage() {
		System.out.println("Usage: XML2PostgreSQL <osm-file> <properties-file>");
	}

	public static void main(String[] args) throws Exception {

		if (args.length != 2) {
			usage();
			System.exit(0);
		}

		File file = new File(args[0]);
		if (!file.isFile()) {
			System.out.println("Path is no file.");
			usage();
			System.exit(1);
		}

		DATE = file.lastModified();

		DefaultHandler saxParser = new XML2PostgreSQLMap(args[1]);

		// get a factory
		SAXParserFactory spf = SAXParserFactory.newInstance();
		try {

			// get a new instance of parser
			SAXParser sp = spf.newSAXParser();

			// parse the file and also register this class for call backs
			sp.parse(args[0], saxParser);

		} catch (SAXException se) {
			se.printStackTrace();
		} catch (ParserConfigurationException pce) {
			pce.printStackTrace();
		} catch (IOException ie) {
			ie.printStackTrace();
		}

	}
}