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

	private static String filterName;

	private Connection conn;

	private Map<String, Byte> nodeFilterMap;
	private Map<String, Byte> wayFilterMap;

	// SQL statements
	private final String SQL_SELECT_POIS = "SELECT poi_id FROM pois_tags WHERE tag = ?";
	private final String SQL_SELECT_WAYS = "SELECT way_id FROM ways_tags WHERE tag = ?";

	private final String SQL_INSERT_FILTER_POI = "INSERT INTO filtered_pois (poi_id) VALUES (?)";
	private final String SQL_INSERT_FILTER_WAY = "INSERT INTO filtered_ways (way_id) VALUES (?)";

	// prepared statements
	private final PreparedStatement pstmtSelectPois;
	private final PreparedStatement pstmtSelectWays;

	private final PreparedStatement pstmtInsertFilteredPois;
	private final PreparedStatement pstmtInsertFilteredWays;

	// result sets
	private ResultSet rsPois;
	private ResultSet rsWays;

	private long startTime;

	public MapDataFilter(String propertiesFile) throws Exception {

		startTime = System.currentTimeMillis();

		// setup database connection

		Properties props = new Properties();
		props.load(new FileInputStream(propertiesFile));

		DBConnection dbConnection = new DBConnection(propertiesFile);

		batchSize = Integer.parseInt(props.getProperty("xml2postgresql.batchSize",
				DEFAULT_BATCH_SIZE));

		// batchSize = Integer.parseInt(DEFAULT_BATCH_SIZE);

		conn = dbConnection.getConnection();

		conn.setAutoCommit(false);

		// conn.createStatement().execute("CREATE TABLE filtered_pois_ids (poi_id bigint)");
		// conn.createStatement().execute("CREATE TABLE filtered_ways_ids (way_id bigint)");

		conn.createStatement().execute("TRUNCATE TABLE filtered_pois CASCADE");
		conn.createStatement().execute("TRUNCATE TABLE filtered_ways CASCADE");

		conn.createStatement().execute("DROP INDEX IF EXISTS filtered_pois_idx");
		conn.createStatement().execute("DROP INDEX IF EXISTS filtered_ways_idx");

		conn.createStatement().execute("ALTER TABLE filtered_pois DROP CONSTRAINT poi_id_fk");
		conn.createStatement().execute("ALTER TABLE filtered_ways DROP CONSTRAINT way_id_fk");

		conn.commit();

		conn.createStatement().execute("SET CONSTRAINTS ALL DEFERRED");

		logger.info("database connection setup done");

		// conn.createStatement().execute(
		// "CREATE TABLE " + filterName + "_pois(poi_id bigint not null)");
		// conn.createStatement().execute(
		// "CREATE TABLE " + filterName + "_ways(way_id bigint not null)");

		nodeFilterMap = Filter.getNodeFilter();
		wayFilterMap = Filter.getWayFilter();

		pstmtSelectPois = conn.prepareStatement(SQL_SELECT_POIS);
		pstmtSelectWays = conn.prepareStatement(SQL_SELECT_WAYS);

		pstmtInsertFilteredPois = conn.prepareStatement(SQL_INSERT_FILTER_POI);
		pstmtInsertFilteredWays = conn.prepareStatement(SQL_INSERT_FILTER_WAY);

		// pstmtInsertFilteredPois = conn.prepareStatement("INSERT INTO " + filterName
		// + "_pois (poi_id) VALUES (?)");
		// pstmtInsertFilteredWays = conn.prepareStatement("INSERT INTO " + filterName
		// + "_ways (way_id) VALUES (?)");

	}

	public void filter() {
		String tag;
		Set<Long> pois = new TreeSet<Long>();
		Set<Long> ways = new TreeSet<Long>();
		int poisC = 0;
		int waysC = 0;
		try {
			// get all pois which have one of the tags specified by the filter
			logger.info("get all relevant poi ids");
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
			logger.info("copy relevant " + pois.size() + " poi records into filter table");
			// logger.info("update relevant " + pois.size() + " poi records into filter table");
			for (Long id : pois) {
				poisC++;
				pstmtInsertFilteredPois.setLong(1, id);
				pstmtInsertFilteredPois.addBatch();

				if (poisC % batchSize == 0) {
					pstmtInsertFilteredPois.executeBatch();
					logger.info("executed batch for pois insert " + (poisC - batchSize) + "-"
							+ poisC);
				}
			}

			// get all ways which have one of the tags specified by the filter
			logger.info("get all relevant way ids");
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
			logger.info("copy relevant " + ways.size() + " way records into filter table");
			for (Long id : ways) {
				waysC++;
				pstmtInsertFilteredWays.setLong(1, id);
				pstmtInsertFilteredWays.addBatch();

				if (waysC % batchSize == 0) {
					pstmtInsertFilteredPois.executeBatch();
					logger.info("executed batch for ways insert " + (waysC - batchSize) + "-"
							+ waysC);
				}
			}

			logger.info("execute last batches");
			pstmtInsertFilteredPois.executeBatch();
			pstmtInsertFilteredWays.executeBatch();

			logger.info("create indices on filter tables");
			conn.createStatement().execute(
					"CREATE INDEX filtered_pois_idx ON filtered_pois(poi_id)");
			conn.createStatement().execute(
					"CREATE INDEX filtered_ways_idx ON filtered_ways(way_id)");
			conn
					.createStatement()
					.execute(
							"ALTER TABLE filtered_pois ADD CONSTRAINT poi_id_fk FOREIGN KEY (poi_id) REFERENCES pois(id)");
			conn
					.createStatement()
					.execute(
							"ALTER TABLE filtered_ways ADD CONSTRAINT way_id_fk FOREIGN KEY (way_id) REFERENCES ways(id)");
			conn.commit();

		} catch (SQLException e) {
			e.printStackTrace();
		}
		logger
				.info("processing took " + (System.currentTimeMillis() - startTime) / 1000
						+ "s.");
	}

	@Override
	protected void finalize() throws Throwable {
		if (!conn.isClosed())
			conn.close();
	}

	private static void usage() {
		System.out.println("Usage: MapDataFilter <properties-file> <filter_name>");
	}

	public static void main(String[] args) {
		if (args.length < 2 || args.length > 2) {
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

			filterName = args[1];

			mapFilter.filter();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}
