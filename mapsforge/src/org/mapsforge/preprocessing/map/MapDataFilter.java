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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.mapsforge.preprocessing.util.DBConnection;

public class MapDataFilter {

	private static final Logger logger = Logger.getLogger(MapDataFilter.class.getName());

	private static final String DEFAULT_BATCH_SIZE = "50000";

	private int batchSize;

	private Connection conn;

	private Map<String, Byte> nodeFilterMap;
	private Map<String, Byte> wayFilterMap;

	// SQL statements
	private final String SQL_SELECT_POIS = "SELECT poi_id FROM pois_tags WHERE tag = ?";
	private final String SQL_SELECT_WAYS = "SELECT way_id FROM ways_tags WHERE tag = ?";

	private final String SQL_SELECT_POIS_TO_TILES = "SELECT poi_id,tile_x,tile_y,zoom_level FROM pois_to_tiles WHERE poi_id = ?";
	private final String SQL_SELECT_WAYS_TO_TILES = "SELECT * FROM ways_to_tiles WHERE way_id = ?";

	private final String SQL_INSERT_FILTER_POI = "INSERT INTO filtered_pois (poi_id,tile_x,tile_y,zoom_level) VALUES (?,?,?,?)";
	private final String SQL_INSERT_FILTER_WAY = "INSERT INTO filtered_ways (way_id,tile_x,tile_y,tile_bitmask,zoom_level) VALUES (?,?,?,?,?)";

	// prepared statements
	private final PreparedStatement pstmtSelectPois;
	private final PreparedStatement pstmtSelectWays;

	private final PreparedStatement pstmtSelectPoisToTiles;
	private final PreparedStatement pstmtSelectWaysToTiles;

	private final PreparedStatement pstmtInsertFilteredPois;
	private final PreparedStatement pstmtInsertFilteredWays;

	// result sets
	private ResultSet rsPois;
	private ResultSet rsWays;

	private ResultSet rsPoisTiles;
	private ResultSet rsWaysTiles;

	public MapDataFilter(String propertiesFile) throws Exception {

		// setup database connection

		Properties props = new Properties();
		props.load(new FileInputStream(propertiesFile));

		DBConnection dbConnection = new DBConnection(propertiesFile);

		batchSize = Integer.parseInt(props.getProperty("xml2postgresql.batchSize",
				DEFAULT_BATCH_SIZE));

		conn = dbConnection.getConnection();

		conn.setAutoCommit(false);

		conn.createStatement().execute("SET CONSTRAINTS ALL DEFERRED");

		logger.info("database connection setup done");

		nodeFilterMap = Filter.getNodeFilter();
		wayFilterMap = Filter.getWayFilter();

		pstmtSelectPois = conn.prepareStatement(SQL_SELECT_POIS);
		pstmtSelectWays = conn.prepareStatement(SQL_SELECT_WAYS);

		pstmtSelectPoisToTiles = conn.prepareStatement(SQL_SELECT_POIS_TO_TILES);
		pstmtSelectWaysToTiles = conn.prepareStatement(SQL_SELECT_WAYS_TO_TILES);

		pstmtInsertFilteredPois = conn.prepareStatement(SQL_INSERT_FILTER_POI);
		pstmtInsertFilteredWays = conn.prepareStatement(SQL_INSERT_FILTER_WAY);
	}

	public void filter() {
		String tag;
		Set<Long> pois = new TreeSet<Long>();
		Set<Long> ways = new TreeSet<Long>();
		int poisC = 0;
		int waysC = 0;
		try {
			// get all pois which have one of the tags specified by the filter
			logger.info("get all relevant pois");
			for (Entry<String, Byte> tagEntry : nodeFilterMap.entrySet()) {
				tag = tagEntry.getKey();
				pstmtSelectPois.setString(1, tag);
				rsPois = pstmtSelectPois.executeQuery();
				while (rsPois.next()) {
					pois.add(rsPois.getLong(1));
				}
			}

			// all relevant records of the pois_to_tiles table are copied into the filtered_pois
			// table
			logger.info("copy relevant poi records into filter table");
			for (Long id : pois) {
				poisC++;
				pstmtSelectPoisToTiles.setLong(1, id);
				rsPoisTiles = pstmtSelectPoisToTiles.executeQuery();
				while (rsPoisTiles.next()) {
					pstmtInsertFilteredPois.setLong(1, rsPoisTiles.getLong("poiid"));
					pstmtInsertFilteredPois.setInt(2, rsPoisTiles.getInt("tilex"));
					pstmtInsertFilteredPois.setInt(3, rsPoisTiles.getInt("tiley"));
					pstmtInsertFilteredPois.setInt(4, rsPoisTiles.getInt("zoomlevel"));
					pstmtInsertFilteredPois.addBatch();
				}
				if (poisC % batchSize == 0) {
					pstmtInsertFilteredPois.executeBatch();
					logger.info("executed batch for pois insert " + (poisC - batchSize) + "-"
							+ poisC);
				}
			}

			// get all ways which have one of the tags specified by the filter
			logger.info("get all relevant ways");
			for (Entry<String, Byte> tagEntry : wayFilterMap.entrySet()) {
				tag = tagEntry.getKey();
				pstmtSelectWays.setString(1, tag);
				rsWays = pstmtSelectWays.executeQuery();
				while (rsWays.next()) {
					ways.add(rsWays.getLong(1));
				}
			}

			// all relevant records of the ways_to_tiles table are copied into the filtered_ways
			// table
			logger.info("copy relevant way records into filter table");
			for (Long id : ways) {
				waysC++;
				pstmtSelectWaysToTiles.setLong(1, id);
				rsWaysTiles = pstmtSelectWaysToTiles.executeQuery();
				while (rsWaysTiles.next()) {
					pstmtInsertFilteredWays.setLong(1, rsWaysTiles.getLong("wayid"));
					pstmtInsertFilteredWays.setInt(2, rsWaysTiles.getInt("tilex"));
					pstmtInsertFilteredWays.setInt(3, rsWaysTiles.getInt("tiley"));
					pstmtInsertFilteredWays.setInt(4, rsWaysTiles.getInt("tilebitmask"));
					pstmtInsertFilteredWays.setInt(5, rsWaysTiles.getInt("zoomlevel"));
					pstmtInsertFilteredWays.addBatch();
				}
				if (waysC % batchSize == 0) {
					pstmtInsertFilteredPois.executeBatch();
					logger.info("executed batch for pois insert " + (waysC - batchSize) + "-"
							+ waysC);
				}
			}

			logger.info("execute last batches");
			pstmtInsertFilteredPois.executeBatch();
			pstmtInsertFilteredWays.executeBatch();

			logger.info("create indices on filter tables");
			conn.createStatement().execute(
					"CREATE INDEX filtered_pois_idx ON filtered_pois (tilex,tiley)");
			conn.createStatement().execute(
					"CREATE INDEX filtered_ways_idx ON filtered_ways (tilex,tiley)");

			conn.commit();

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void finalize() throws Throwable {
		if (!conn.isClosed())
			conn.close();
	}

	private static void usage() {
		System.out.println("Usage: MapDataFilter <properties-file>");
	}

	public static void main(String[] args) {
		if (args.length != 1) {
			usage();
			System.exit(0);
		}

		File file = new File(args[0]);
		if (!file.isFile()) {
			System.out.println("Path is no file.");
			usage();
			System.exit(1);
		}

		try {
			MapDataFilter mapFilter = new MapDataFilter(args[0]);

			mapFilter.filter();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}
