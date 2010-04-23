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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
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
import com.vividsolutions.jts.geom.GeometryFactory;

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

	private static final byte zoom = 15;

	private int batchSize;

	private static int batchSizeMP = 600;

	private static long DATE;

	private static final int VERSION = 1;

	private int nodes;
	private int node_tags;

	private int ways;
	private int way_tags;

	private int multipolygons;

	private MapElementNode currentNode;
	private Vector<String> currentTags;

	private MapElementWay currentWay;
	private Vector<Integer> currentWayNodes;

	private Vector<Integer> currentInnerWays;
	private Vector<Integer> currentOuterWays;

	private long startTime;

	private final String SQL_INSERT_POIS = "INSERT INTO pois (id, latitude, longitude, namelength, name, tagsamount, tags, layer, elevation, housenumber) VALUES (?,?,?,?,?,?,?,?,?,?)";

	private final String SQL_INSERT_WAYS = "INSERT INTO ways (id, namelength, name, tagsamount, tags, layer, waynodesamount, waytype, convexness) VALUES (?,?,?,?,?,?,?,?,?)";

	private final String SQL_INSERT_MULTIPOLYGONS = "INSERT INTO multipolygons (outerwayid, innerwaysequence, waynodesequence, latitude, longitude) VALUES (?,?,?,?,?)";

	private final String SQL_SELECT_WAYNODES = "SELECT latitude, longitude FROM pois WHERE id = ?";

	private final String SQL_INSERT_WAYNODES = "INSERT INTO waynodes (wayid, waynodesequence, latitude, longitude) VALUES (?,?,?,?)";

	private final String SQL_SELECT_INNERWAYNODES = "SELECT latitude,longitude FROM waynodes WHERE wayid = ?";

	private final String SQL_INSERT_METADATA = "INSERT INTO metadata (maxlat,minlon,minlat,maxlon,date,importversion,zoom,tilesize) VALUES (?,?,?,?,?,?,?,?)";

	private final String SQL_SELECT_BBOX = "SELECT max(latitude) as maxlat,min(longitude) as minlon,min(latitude) as minlat,max(longitude) as maxlon from pois";

	private final String SQL_UPDATE_WAYTYPE_FLAG = "UPDATE ways SET waytype = 3 WHERE id = ?";

	private final String SQL_INSERT_POI_TILE = "INSERT INTO poistotiles (poiid,tilex,tiley,copy) VALUES (?,?,?,?)";

	private final String SQL_INSERT_WAY_TILE = "INSERT INTO waystotiles (wayid,tilex,tiley,tilebitmask) VALUES (?,?,?,?)";

	private final String SQL_DISTINCT_WAY_TILES = "SELECT DISTINCT tilex,tiley FROM waystotiles";

	private final String SQL_SELECT_WAY_IDS = "SELECT w.id,w.waynodesamount,w.waytype FROM waystotiles wtt JOIN ways w ON (wtt.wayid = w.id) WHERE wtt.tilex = ? AND wtt.tiley = ?";

	private final String SQL_SELECT_WAYS_FOR_TILE = "SELECT latitude,longitude FROM waynodes WHERE wayid = ? order by waynodesequence";

	private final String SQL_UPDATE_WAY_TILE = "UPDATE waystotiles SET tilebitmask = ? WHERE wayid = ? and tilex = ? and tiley = ?";

	private final String SQL_DELETE_POIS = "DELETE FROM pois WHERE tags = ''";

	private final String SQL_DELETE_WAYS = "DELETE FROM ways WHERE tags = ''";

	private PreparedStatement pstmtPois;

	private boolean poisBatched = false;

	private PreparedStatement pstmtSelectWayNodes;

	private PreparedStatement pstmtInsertWayNodes;

	private PreparedStatement pstmtWays;

	private boolean waysBatched = false;

	private PreparedStatement pstmtMultipolygons;

	private PreparedStatement pstmtInnerWays;

	private PreparedStatement pstmtMetadata;

	private PreparedStatement pstmtBoundingBox;

	private PreparedStatement pstmtUpdateWayType;

	private PreparedStatement pstmtPoisTiles;

	private PreparedStatement pstmtWaysTiles;

	private PreparedStatement pstmtDistinctTiles;

	private PreparedStatement pstmtSelectWayIds;

	private PreparedStatement pstmtSelectWaysToTiles;

	private PreparedStatement pstmtUpdateWaysTiles;

	private PreparedStatement pstmtDelPois;

	private PreparedStatement pstmtDelWays;

	private ResultSet rsWayNodes;

	private ResultSet rsInnerWays;

	private ResultSet rsBoundingBox;

	private ResultSet rsDistinctTiles;

	private ResultSet rsWayIds;

	private ResultSet rsWaysForTiles;

	private Connection conn;

	private String tmpTags;

	private Map<String, Byte> nodeTagWhiteList;
	private Map<String, Byte> wayTagWhiteList;

	private GeometryFactory geoFac;

	private int maxlat;
	private int minlon;
	private int minlat;
	private int maxlon;

	private Set<Tile> wayTiles;

	public XML2PostgreSQLMap(String propertiesFile) throws Exception {

		Properties props = new Properties();
		props.load(new FileInputStream(propertiesFile));

		DBConnection dbConnection = new DBConnection(propertiesFile);

		currentTags = new Vector<String>();
		currentWayNodes = new Vector<Integer>();
		currentOuterWays = new Vector<Integer>();
		currentInnerWays = new Vector<Integer>();

		wayTiles = new HashSet<Tile>();

		geoFac = new GeometryFactory();

		batchSize = Integer.parseInt(props.getProperty("xml2postgresql.batchSize",
				DEFAULT_BATCH_SIZE));

		conn = dbConnection.getConnection();

		conn.setAutoCommit(false);

		logger.info("truncating tables");

		conn.createStatement().execute("TRUNCATE TABLE multipolygons CASCADE");

		conn.createStatement().execute("TRUNCATE TABLE waynodes CASCADE");

		conn.createStatement().execute("TRUNCATE TABLE ways CASCADE");

		conn.createStatement().execute("TRUNCATE TABLE pois CASCADE");

		conn.createStatement().execute("TRUNCATE TABLE metadata CASCADE");

		conn.createStatement().execute("TRUNCATE TABLE poistotiles CASCADE");

		conn.createStatement().execute("TRUNCATE TABLE waystotiles CASCADE");

		conn.commit();

		conn.createStatement().execute("SET CONSTRAINTS ALL DEFERRED");

		pstmtPois = conn.prepareStatement(SQL_INSERT_POIS);

		pstmtSelectWayNodes = conn.prepareStatement(SQL_SELECT_WAYNODES);

		pstmtInsertWayNodes = conn.prepareStatement(SQL_INSERT_WAYNODES);

		pstmtWays = conn.prepareStatement(SQL_INSERT_WAYS);

		pstmtMultipolygons = conn.prepareStatement(SQL_INSERT_MULTIPOLYGONS);

		pstmtInnerWays = conn.prepareStatement(SQL_SELECT_INNERWAYNODES);

		pstmtMetadata = conn.prepareStatement(SQL_INSERT_METADATA);

		pstmtBoundingBox = conn.prepareStatement(SQL_SELECT_BBOX);

		pstmtUpdateWayType = conn.prepareStatement(SQL_UPDATE_WAYTYPE_FLAG);

		pstmtPoisTiles = conn.prepareStatement(SQL_INSERT_POI_TILE);

		pstmtWaysTiles = conn.prepareStatement(SQL_INSERT_WAY_TILE);

		pstmtDistinctTiles = conn.prepareStatement(SQL_DISTINCT_WAY_TILES);

		pstmtSelectWayIds = conn.prepareStatement(SQL_SELECT_WAY_IDS);

		pstmtSelectWaysToTiles = conn.prepareStatement(SQL_SELECT_WAYS_FOR_TILE);

		pstmtUpdateWaysTiles = conn.prepareStatement(SQL_UPDATE_WAY_TILE);

		pstmtDelPois = conn.prepareStatement(SQL_DELETE_POIS);

		pstmtDelWays = conn.prepareStatement(SQL_DELETE_WAYS);

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

		nodeTagWhiteList = WhiteList.getNodeTagWhitelist();
		wayTagWhiteList = WhiteList.getWayTagWhitelist();

		logger.info("start reading file");
		startTime = System.currentTimeMillis();
	}

	@Override
	public void endDocument() throws SAXException {
		super.endDocument();

		logger.info("executing last batches...");

		try {

			rsBoundingBox = pstmtBoundingBox.executeQuery();
			while (rsBoundingBox.next()) {
				maxlat = rsBoundingBox.getInt("maxlat");
				minlon = rsBoundingBox.getInt("minlon");
				minlat = rsBoundingBox.getInt("minlat");
				maxlon = rsBoundingBox.getInt("maxlon");
			}

			// System.out.println(maxlat+" "+minlon+" "+minlat+" "+maxlon);

			pstmtMetadata.setInt(1, maxlat);
			pstmtMetadata.setInt(2, minlon);
			pstmtMetadata.setInt(3, minlat);
			pstmtMetadata.setInt(4, maxlon);
			pstmtMetadata.setLong(5, DATE);
			pstmtMetadata.setInt(6, VERSION);
			pstmtMetadata.setInt(7, zoom);
			pstmtMetadata.setInt(8, Tile.TILE_SIZE);
			pstmtMetadata.execute();

			logger.info("metadata inserted");

			// pstmtPois.executeBatch();
			//		
			// pstmtWays.executeBatch();

			logger.info("execute last multipolygon batches");

			pstmtMultipolygons.executeBatch();

			pstmtUpdateWayType.executeBatch();

			logger.info("delete pois without tags");

			pstmtDelPois.execute();

			logger.info("deleted pois without tags");

			logger.info("delete ways without tags");

			pstmtDelWays.execute();

			logger.info("deleted ways without tags");

			logger.info("calculate way bitmasks");

			calculateTileBitmask();

			logger.info("bitmasks calculated");

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

	void calculateTileBitmask() throws SQLException {

		Tile tile;
		MapElementWay way;
		Geometry geoWay;
		int id;
		int waynodes;
		int wayType;
		Map<MapElementWay, Geometry> waysMap = new HashMap<MapElementWay, Geometry>();

		rsDistinctTiles = pstmtDistinctTiles.executeQuery();

		while (rsDistinctTiles.next()) {

			tile = new Tile(rsDistinctTiles.getInt("tilex"), rsDistinctTiles.getInt("tiley"),
					zoom);
			waysMap.clear();
			pstmtSelectWayIds.setLong(1, tile.x);
			pstmtSelectWayIds.setLong(2, tile.y);
			rsWayIds = pstmtSelectWayIds.executeQuery();

			while (rsWayIds.next()) {
				id = rsWayIds.getInt("id");
				waynodes = rsWayIds.getInt("waynodesamount");
				wayType = rsWayIds.getInt("waytype");

				way = new MapElementWay(id);
				way.wayType = wayType;

				pstmtSelectWaysToTiles.setInt(1, id);
				rsWaysForTiles = pstmtSelectWaysToTiles.executeQuery();
				Coordinate[] coords = new Coordinate[waynodes];

				int k = 0;

				while (rsWaysForTiles.next()) {
					int lat = rsWaysForTiles.getInt("latitude");
					int lon = rsWaysForTiles.getInt("longitude");

					coords[k] = new Coordinate((double) lat / 1000000, (double) lon / 1000000);
					k++;
				}
				if (wayType == 1) {
					geoWay = geoFac.createLineString(coords);
				} else {
					geoWay = geoFac.createPolygon(geoFac.createLinearRing(coords), null);
				}

				waysMap.put(way, geoWay);
			}
			Map<MapElementWay, Short> results = Utils.getTileBitmasksForWays(tile, waysMap);
			Set<Entry<MapElementWay, Short>> entries = results.entrySet();
			for (Entry<MapElementWay, Short> entry : entries) {
				pstmtUpdateWaysTiles.setShort(1, entry.getValue());
				pstmtUpdateWaysTiles.setLong(2, entry.getKey().id);
				pstmtUpdateWaysTiles.setLong(3, tile.x);
				pstmtUpdateWaysTiles.setLong(4, tile.y);
				pstmtUpdateWaysTiles.execute();
			}
		}

	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes)
			throws SAXException {

		if (qName.equals("bounds")) {
			maxlat = (int) (Double.parseDouble(attributes.getValue("maxlat")) * 1000000);
			minlon = (int) (Double.parseDouble(attributes.getValue("minlon")) * 1000000);
			minlat = (int) (Double.parseDouble(attributes.getValue("minlat")) * 1000000);
			maxlon = (int) (Double.parseDouble(attributes.getValue("maxlon")) * 1000000);

			// System.out.println(maxlat+" "+minlon+" "+minlat+" "+maxlon);
		}

		if (qName.equals("node")) {
			currentTags.clear();

			long id = Long.parseLong(attributes.getValue("id"));
			int latitude = (int) (Double.parseDouble(attributes.getValue("lat")) * 1000000);
			int longitude = (int) (Double.parseDouble(attributes.getValue("lon")) * 1000000);
			currentNode = new MapElementNode(id, latitude, longitude);
		} else if (qName.equals("way")) {
			if (!poisBatched) {
				try {
					pstmtPois.executeBatch();
					pstmtPoisTiles.executeBatch();
				} catch (SQLException e) {
					System.err.println(pstmtPois);
					e.printStackTrace();

				}

				poisBatched = true;
			}

			currentTags.clear();
			currentWayNodes.clear();

			currentWay = new MapElementWay(Integer.parseInt(attributes.getValue("id")));
		} else if (qName.equals("relation")) {
			if (!waysBatched) {
				try {
					pstmtWays.executeBatch();
					pstmtInsertWayNodes.executeBatch();
					pstmtWaysTiles.executeBatch();
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

			if (qName.equals("node")) {

				nodes++;
				pstmtPois.setLong(1, currentNode.id);
				pstmtPois.setInt(2, currentNode.latitude);
				pstmtPois.setInt(3, currentNode.longitude);

				String tag;
				Iterator<String> it = currentTags.iterator();
				int size = currentTags.size();
				String[] splits = new String[2];

				Vector<String> t = new Vector<String>();
				node_tags = 0;
				while (it.hasNext()) {
					tag = it.next();
					splits = tag.split("=");
					if (splits[0].equals("ele")) {
						try {
							currentNode.elevation = (int) Double.parseDouble(splits[1]);
						} catch (NumberFormatException e) {
							currentNode.elevation = 0;
						}
					} else if (splits[0].equals("addr:housenumber")) {
						currentNode.housenumber = splits[1];
					} else if (splits[0].equals("name")) {
						currentNode.name = splits[1];
					} else if (splits[0].equals("layer")) {
						currentNode.layer = Integer.parseInt(splits[1]);

					} else if (nodeTagWhiteList.containsKey(tag)) {
						t.add(tag);

						node_tags++;
					}
					size--;
				}

				String tags = "";
				for (int k = 0; k < node_tags; k++) {
					tags += t.get(k);
					tags += "\n";
				}

				if (currentNode.name != null) {
					pstmtPois.setInt(4, currentNode.name.getBytes().length);
				} else {
					pstmtPois.setInt(4, 0);
				}

				pstmtPois.setString(5, currentNode.name);
				pstmtPois.setInt(6, node_tags);
				pstmtPois.setString(7, tags);
				pstmtPois.setInt(8, currentNode.layer);
				pstmtPois.setInt(9, currentNode.elevation);
				pstmtPois.setString(10, currentNode.housenumber);
				pstmtPois.addBatch();

				Tile mainTileForPOI = new Tile(MercatorProjection.longitudeToTileX(
						(double) currentNode.longitude / 1000000, zoom), MercatorProjection
						.latitudeToTileY((double) currentNode.latitude / 1000000, zoom), zoom);

				// System.out.println(mainTileForPOI.toString());

				List<Tile> otherTilesForPOI = POIToTiles.getPOITiles(currentNode, zoom);

				for (Tile tile : otherTilesForPOI) {
					pstmtPoisTiles.setLong(1, currentNode.id);
					pstmtPoisTiles.setLong(2, tile.x);
					pstmtPoisTiles.setLong(3, tile.y);
					if (tile.equals(mainTileForPOI)) {
						pstmtPoisTiles.setBoolean(4, false);
					} else {
						pstmtPoisTiles.setBoolean(4, true);
					}
					// System.out.println(tile.toString());
					pstmtPoisTiles.addBatch();
				}

				poisBatched = false;

				if (nodes % batchSize == 0) {
					pstmtPois.executeBatch();
					pstmtPoisTiles.executeBatch();
					logger
							.info("executed batch for nodes " + (nodes - batchSize) + "-"
									+ nodes);
				}

			} else if (qName.equals("way")) {

				ways++;

				String tag;
				Iterator<String> itWayTags = currentTags.iterator();
				int size = currentTags.size();
				Vector<String> t = new Vector<String>();
				way_tags = 0;
				String[] splits = new String[2];

				while (itWayTags.hasNext()) {
					tag = itWayTags.next();
					splits = tag.split("=");
					if (splits[0].equals("name")) {
						currentWay.name = splits[1];
					} else if (splits[0].equals("layer")) {
						currentWay.layer = (byte) Integer.parseInt(splits[1]);
					} else if (wayTagWhiteList.containsKey(tag)) {
						t.add(tag);

						way_tags++;
					}
					size--;
				}

				String tags = "";
				for (int k = 0; k < way_tags; k++) {
					tags += t.get(k);
					tags += "\n";
				}

				int wnSize = currentWayNodes.size();
				int sequence = 1;

				if (currentWayNodes.get(0).equals(currentWayNodes.get(wnSize - 1))) {
					currentWay.wayType = 2;
				}

				int lat = 0;
				int lon = 0;
				Coordinate[] wayNodes = new Coordinate[wnSize];

				for (int i = 0; i < wnSize; i++) {
					pstmtSelectWayNodes.setInt(1, currentWayNodes.get(i));
					rsWayNodes = pstmtSelectWayNodes.executeQuery();

					while (rsWayNodes.next()) {
						lat = rsWayNodes.getInt("latitude");
						lon = rsWayNodes.getInt("longitude");
						wayNodes[i] = (new Coordinate((double) lat / 1000000,
								(double) lon / 1000000));
						pstmtInsertWayNodes.setLong(1, currentWay.id);
						pstmtInsertWayNodes.setInt(2, sequence);
						pstmtInsertWayNodes.setInt(3, lat);
						pstmtInsertWayNodes.setInt(4, lon);
					}

					pstmtInsertWayNodes.addBatch();
					sequence++;
				}
				wayTiles = Utils.wayToTiles(currentWay, wayNodes, currentWay.wayType, zoom);
				for (Tile wayTile : wayTiles) {
					pstmtWaysTiles.setLong(1, currentWay.id);
					pstmtWaysTiles.setLong(2, wayTile.x);
					pstmtWaysTiles.setLong(3, wayTile.y);
					pstmtWaysTiles.setInt(4, 0);
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
				pstmtWays.setString(5, tags);
				pstmtWays.setInt(6, currentWay.layer);
				pstmtWays.setInt(7, wnSize);
				pstmtWays.setInt(8, currentWay.wayType);
				pstmtWays.setInt(9, currentWay.convexness);
				pstmtWays.addBatch();
				waysBatched = false;

				if (ways % batchSize == 0) {
					pstmtWays.executeBatch();
					pstmtInsertWayNodes.executeBatch();
					pstmtWaysTiles.executeBatch();
					logger.info("executed batch for ways " + (ways - batchSize) + "-" + ways);
				}
			} else if (qName.equals("relation")) {

				int size = currentOuterWays.size();

				int innersize;
				if (size != 0) {
					for (int i = 0; i < size; i++) {
						int outer = currentOuterWays.get(i);

						pstmtUpdateWayType.setInt(1, outer);
						pstmtUpdateWayType.addBatch();

						innersize = currentInnerWays.size();

						for (int j = 0; j < innersize; j++) {
							pstmtInnerWays.setInt(1, currentInnerWays.get(j));
							rsInnerWays = pstmtInnerWays.executeQuery();
							int nodeSequence = 1;
							while (rsInnerWays.next()) {
								try {
									pstmtMultipolygons.setInt(1, outer);
									pstmtMultipolygons.setInt(2, j + 1);
									pstmtMultipolygons.setInt(3, nodeSequence);
									pstmtMultipolygons
											.setInt(4, rsInnerWays.getInt("latitude"));
									pstmtMultipolygons.setInt(5, rsInnerWays
											.getInt("longitude"));
									pstmtMultipolygons.addBatch();
									nodeSequence++;
								} catch (SQLException e) {

									System.err.println(pstmtMultipolygons);
									throw e;
								}
							}
						}
						multipolygons++;
					}
				}
				if (multipolygons % batchSizeMP == 0) {
					pstmtMultipolygons.executeBatch();
					pstmtUpdateWayType.executeBatch();
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