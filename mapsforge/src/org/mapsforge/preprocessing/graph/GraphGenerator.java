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
/**
 * 
 */
package org.mapsforge.preprocessing.graph;

import gnu.trove.map.hash.TLongIntHashMap;
import gnu.trove.procedure.TLongIntProcedure;

import java.io.File;
import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Properties;
import java.util.logging.Logger;

import org.mapsforge.core.conf.Vehicle;
import org.mapsforge.preprocessing.model.Node;
import org.mapsforge.preprocessing.model.Way;
import org.mapsforge.preprocessing.model.WayNode;
import org.mapsforge.preprocessing.util.DBConnection;

/**
 * Extracts a routing graph from OSM-data. The class expects the OSM-data to be present in a
 * database (currently written for PostgreSQL) according to the OSM-API-V6 schema. This means
 * the extractor relies on the database tables 'nodes', 'ways', 'node_tags', 'way_tags',
 * 'way_nodes'. Use the {@link XML2PostgreSQL} class to extract from OSM-XML format into this
 * scheme. A script for creating the schema (rg_base.sql) for the database can be found in the
 * sql folder.
 * 
 * The extractor works as follows:
 * <ol>
 * <li>Extract all junctions using an SQL-query. Store each extracted node in a HashMap using
 * the node_id as key.</li>
 * <li>Fetch all ways, with all way_points and all way_tags ordered by way_id,
 * way_point.sequence_id from the database in streaming mode.</li>
 * <li>Check for each way_point of a way if it is a junction (HashMap) or the beginning or end
 * of a way. If so, generate a vertex in the graph representation and add an edge between this
 * vertex and the previous one extracted from this way.</li>
 * <li>For each way-segment between vertices in the routing graph calculate the distance and
 * gather extra information such as speed limits or number of traffic signs.</li>
 * <li>Write the extracted routing graph to the target database</li>
 * </ol>
 * 
 * The database is configured in the graph_extractor.properties file.
 * 
 * <b>Attention:</b> Truncates edge and edge_nodes tables before inserting new data.
 * 
 * @author bross
 * 
 */
public class GraphGenerator {

	private static final Logger logger = Logger.getLogger(GraphGenerator.class.getName());

	private static final int DEFAULT_EXPECTED_VERTICES = 500000;
	private static final int DEFAULT_FETCH_SIZE = 500;
	private static final int DEFAULT_BATCH_SIZE = 50000;

	private static final String SQL_EXTRACT_JUNCTIONS = "SELECT junctions.id AS node_id "
			+ "FROM " + "(SELECT n.id FROM way_tags wt JOIN way_nodes wn ON(wt.id = wn.id) "
			+ "JOIN nodes n ON(wn.node_id = n.id) WHERE wt.k = 'highway' "
			+ "AND wt.v IN ('living_street','residential','primary',"
			+ "'secondary','tertiary','motorway_link','motorway',"
			+ "'primary_link','road', 'trunk','trunk_link','service')"
			+ "GROUP BY 1 HAVING COUNT(n.id) >= 2) AS junctions";

	private static final String SQL_EXTRACT_WAYNODES = "SELECT n.id AS node_id, wn.id AS waynode_id, wn.sequence_id AS sid, wt.k AS tagname, wt.v AS tagvalue, nt.v AS nodetags, n.int_longitude, n.int_latitude "
			+ "FROM "
			+ "(SELECT w.id FROM ways w JOIN way_tags wt1 ON (w.id = wt1.id) WHERE wt1.k = 'highway' "
			+ "AND wt1.v IN ('living_street','residential','primary',"
			+ "'secondary','tertiary','motorway_link','motorway',"
			+ "'primary_link','road', 'trunk','trunk_link','service')) highways "
			+ "JOIN way_nodes wn ON (highways.id = wn.id) "
			+ "JOIN way_tags wt ON (wt.id = highways.id) "
			+ "JOIN nodes n ON (n.id = wn.node_id) "
			+ "LEFT OUTER JOIN node_tags nt ON (n.id=nt.id AND nt.k='highway') "
			+ "WHERE wt.k IN ('highway','oneway','maxspeed','name') "
			+ "ORDER BY 2 ASC, 3 ASC ";

	private static final String SQL_INSERT_ID_MAPPING = "INSERT INTO id_mapping (osm_id, internal_id, configuration) VALUES (?,?,?)";
	// "SELECT n.id AS node_id, wn.id AS waynode_id, wn.sequence_id AS sid, wt.k AS tagname, wt.v AS tagvalue, nt.v AS nodetags, n.longitude, n.latitude "
	// +
	// "FROM way_nodes wn JOIN way_tags wt ON (wt.id = wn.id) " +
	// "JOIN nodes n ON (n.id = wn.node_id) LEFT OUTER JOIN node_tags nt ON (n.id=nt.id AND nt.k='highway') "
	// +
	// "WHERE wt.k IN ('highway','oneway','maxspeed','name') ORDER BY 2 ASC, 3 ASC;";

	protected Connection conn;

	protected PreparedStatement pstmtInsertEdge = null;
	protected PreparedStatement pstmtInsertEdgeNodes = null;
	protected PreparedStatement pstmtInsertIDMapping = null;

	protected TLongIntHashMap junctions;

	private int counter = 0;

	protected int fetchSize;
	protected int batchSize;

	private int n_edges = 0;

	public GraphGenerator(String propertiesFile) throws Exception {
		Properties props = new Properties();
		props.load(new FileInputStream(propertiesFile));

		DBConnection dbConnection = new DBConnection(propertiesFile);

		int expectedNodeCount = DEFAULT_EXPECTED_VERTICES;
		try {
			expectedNodeCount = Integer.parseInt(props.getProperty("graph.expectedVertices"));
		} catch (NumberFormatException e1) {
			logger.info("property 'expectedVertices' not found. Choosing default: "
					+ DEFAULT_EXPECTED_VERTICES);
		}

		this.fetchSize = DEFAULT_FETCH_SIZE;
		try {
			this.fetchSize = Integer.parseInt(props.getProperty("graph.fetchsize"));
		} catch (NumberFormatException e1) {
			logger.info("property 'fetchsize' not found. Choosing default: "
					+ DEFAULT_FETCH_SIZE);
		}

		this.batchSize = DEFAULT_BATCH_SIZE;
		try {
			this.fetchSize = Integer.parseInt(props.getProperty("graph.batchsize"));
		} catch (NumberFormatException e1) {
			logger.info("property 'batchsize' not found. Choosing default: "
					+ DEFAULT_BATCH_SIZE);
		}

		conn = dbConnection.getConnection();
		conn.setAutoCommit(false);

		// truncate tables
		logger.info("truncating old data");

		this.conn.createStatement().executeUpdate("TRUNCATE table id_mapping CASCADE");
		this.conn.createStatement().executeUpdate("TRUNCATE table weights CASCADE");
		this.conn.createStatement().executeUpdate("TRUNCATE table edge_nodes CASCADE");
		this.conn.createStatement().executeUpdate("TRUNCATE table edges CASCADE");

		conn.commit();

		pstmtInsertIDMapping = conn.prepareStatement(SQL_INSERT_ID_MAPPING);

		this.junctions = new TLongIntHashMap(expectedNodeCount);
	}

	@Override
	protected void finalize() throws Throwable {
		if (!conn.isClosed())
			conn.close();
	}

	public void generate() throws SQLException {

		logger.info("loading junctions");
		loadJunctions();
		logger.info(junctions.size() + " junctions loaded");

		String insertEdgeNodes = "INSERT INTO edge_nodes (edge_id, node_id, sequence_id) "
				+ "VALUES (?,?,?)";

		String insertEdge = "INSERT into edges (id, length, level, unidirectional, source_id, dest_id, "
				+ "traffic_light, urban, maxspeed,streetname) "
				+ "VALUES (?,?,?,?,?,?,?,?,?,?)";

		pstmtInsertEdgeNodes = conn.prepareStatement(insertEdgeNodes);
		pstmtInsertEdge = conn.prepareStatement(insertEdge);

		/**
		 * Load all waynodes, associate the given way_tags and order by waynode_id, then by
		 * sequence_id in way. Use STRAIGHT_JOINS to force the join order. Join the node_id to
		 * all way_nodes.
		 */
		Statement stmtWays = conn.createStatement();

		/**
		 * use streaming to lower memory consumption see http://dev.mysql.com/doc
		 * /refman/5.1/de/connector-j-reference-implementation-notes.html
		 */
		// stmtWays.setFetchSize(Integer.MIN_VALUE);
		stmtWays.setFetchSize(fetchSize);
		logger.info("Fetching ways...");
		ResultSet rsWays = stmtWays.executeQuery(SQL_EXTRACT_WAYNODES);

		// variables needed to detect ways
		// -->the resultset is ordered by ways and then sequence
		long previous_way_id = -1, current_way_id;

		// variable needed to detect different tags of one waynode
		// -->the resultset contains one row for each waynode and each different
		// tag
		long previous_node_id = -1, current_node_id;

		// use this data structure to hold all waynodes
		// of a single way (ordered by sequence number)
		Way currentWay = null;

		logger.info("Processing ways...");
		int n_ways = 0;
		int n_nodes = 0;
		int n_highways = 0;

		while (rsWays.next()) {
			current_node_id = rsWays.getInt("node_id");
			current_way_id = rsWays.getInt("waynode_id");

			if (current_way_id != previous_way_id && currentWay != null) {

				// process loaded way
				analyzeWay(currentWay);
				n_ways++;

				// clear data structure
				currentWay = null;
				previous_node_id = -1;
			}

			// check if current node id is NOT the same (multiple way_tags)
			if (current_node_id != previous_node_id) {
				// first node?
				if (currentWay == null)
					currentWay = new Way();

				// new node on this way encountered --> add it to data structure
				currentWay.addWayNode(new WayNode(new Node(current_node_id, rsWays
						.getInt("int_latitude"), rsWays.getInt("int_longitude"), rsWays
						.getString("nodetags")), current_way_id, rsWays.getInt("sid")));
				n_nodes++;
			}
			// independent of new node or not, add the known tags to the way
			// attributes
			currentWay.setKnownTag(rsWays.getString("tagname"), rsWays.getString("tagvalue"));

			if (rsWays.getString("tagname").equalsIgnoreCase("highway"))
				n_highways++;

			previous_node_id = current_node_id;
			previous_way_id = current_way_id;
		}

		// analyze last way
		analyzeWay(currentWay);
		n_ways++;

		logger.info("Processed " + n_ways + " ways...");
		logger.info("Nodes: " + n_nodes);
		logger.info("Highways: " + n_highways);
		logger.info("Edges: " + n_edges);
		logger.info("Vertices: " + junctions.size());
		logger.info("Adding inserts for vertices to batch...");

		logger.info("Executing last edges batch...");
		pstmtInsertEdge.executeBatch();
		pstmtInsertEdgeNodes.executeBatch();

		// for (Entry<Long, Integer> mapping : junctions.) {
		//			
		// }
		junctions.forEachEntry(new TLongIntProcedure() {

			@Override
			public boolean execute(long osm_id, int internal_id) {

				try {
					pstmtInsertIDMapping.setLong(1, osm_id);
					pstmtInsertIDMapping.setInt(2, internal_id);
					pstmtInsertIDMapping.setString(3, configuration.name());

					pstmtInsertIDMapping.addBatch();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return true;
			}
		});
		pstmtInsertIDMapping.executeBatch();

		logger.info("Committing transaction...");
		this.conn.commit();

	}

	private final Vehicle configuration = Vehicle.STANDARD_CAR__SIMPLE_HEURISTIC;

	/**
	 * Analyze a given way and compute the length, the average maxspeed and number of traffic
	 * lights contained.
	 * 
	 * @param waynodes
	 * @throws SQLException
	 */
	private void analyzeWay(Way way) throws SQLException {
		WayNode previousJunctionWaynode = null;
		WayNode previousWaynode = null;
		float lengthSoFar = 0f;
		float segmentLength;
		int trafficLightsSoFar = 0;
		int i = 0;
		int previousIndex = 0;
		for (WayNode currentWaynode : way.wayNodes) {

			// first waynode of a way --> always a junction
			// initialize processing of a way
			if (i == 0) {
				if (!junctions.contains(currentWaynode.node.id)) {
					junctions.put(currentWaynode.node.id, counter++);
				}

				if (currentWaynode.node.hasTrafficLight())
					trafficLightsSoFar++;

				previousJunctionWaynode = currentWaynode;
				previousWaynode = currentWaynode;
			}
			// intermediate node of the way, maybe a junction
			else {
				// first update the info for the current edge

				// update length
				segmentLength = (float) currentWaynode.node.distance(previousWaynode.node);
				lengthSoFar += segmentLength;

				if (currentWaynode.node.hasTrafficLight())
					trafficLightsSoFar++;

				// update variables
				previousWaynode = currentWaynode;

				// the last waynode in a way has been reached or a junction has
				// been found
				// --> write the current segment into the graph
				if (i == (way.wayNodes.size() - 1)
						|| junctions.contains(currentWaynode.node.id)) {

					if (!junctions.contains(currentWaynode.node.id)) {
						junctions.put(currentWaynode.node.id, counter++);
					}

					// add an edge between the current junction and the previous
					// junction
					// using the calculated information
					pstmtInsertEdge.setInt(1, n_edges);
					pstmtInsertEdge.setInt(2, Math.round(lengthSoFar));
					pstmtInsertEdge.setString(3, way.level.toString());
					pstmtInsertEdge.setBoolean(4, way.oneway != 0);

					// switch source and destination if oneway is defined as -1
					if (way.oneway < 0) {
						pstmtInsertEdge.setLong(6, previousJunctionWaynode.node.id);
						pstmtInsertEdge.setLong(5, currentWaynode.node.id);
						logger.fine("OneWay < 0: " + way.streetname);
					} else {
						pstmtInsertEdge.setLong(5, previousJunctionWaynode.node.id);
						pstmtInsertEdge.setLong(6, currentWaynode.node.id);
					}
					pstmtInsertEdge.setInt(7, trafficLightsSoFar);
					pstmtInsertEdge.setBoolean(8, true);
					if (way.maxspeed != null)
						pstmtInsertEdge.setString(9, way.maxspeed);
					else
						pstmtInsertEdge.setNull(9, Types.VARCHAR);
					if (way.streetname != null)
						pstmtInsertEdge.setString(10, way.streetname);
					else
						pstmtInsertEdge.setNull(10, Types.VARCHAR);

					pstmtInsertEdge.addBatch();

					// add edge_nodes

					int seqNo = 0;
					int subListSize = i + 1 - previousIndex;
					for (WayNode wn : way.wayNodes.subList(previousIndex, i + 1)) {
						pstmtInsertEdgeNodes.setInt(1, n_edges);
						pstmtInsertEdgeNodes.setLong(2, wn.node.id);
						// adjust order of edge nodes if oneway = -1
						if (way.oneway < 0)
							pstmtInsertEdgeNodes.setInt(3, subListSize - seqNo - 1);
						else
							pstmtInsertEdgeNodes.setInt(3, seqNo);
						pstmtInsertEdgeNodes.addBatch();
						seqNo++;
					}

					n_edges++;

					if (n_edges % batchSize == 0) {
						pstmtInsertEdge.executeBatch();
						pstmtInsertEdgeNodes.executeBatch();
						logger.info("executed batch for edges " + (n_edges - batchSize) + " - "
								+ n_edges);
					}

					previousJunctionWaynode = currentWaynode;
					trafficLightsSoFar = 0;
					lengthSoFar = 0;
					previousIndex = i;
				}

			}

			i++;
		}
	}

	/**
	 * Extracts all junctions from the OSM database. A junction is defined by a node which is
	 * associated with at least two different ways. -->join node table with way_node table and
	 * group by node_id, node_ids that occur more than once in the joined table (i.e. have more
	 * than one join partner) are junctions
	 * 
	 * @throws SQLException
	 */
	private void loadJunctions() throws SQLException {
		Statement stmt = conn.createStatement(ResultSet.FETCH_FORWARD,
				ResultSet.CONCUR_READ_ONLY);
		ResultSet rs = stmt.executeQuery(SQL_EXTRACT_JUNCTIONS);

		// put all junctions into hashmap (key: node_id)
		while (rs.next()) {
			this.junctions.put(rs.getLong(1), counter++);
		}
		rs.close();
		stmt.close();
	}

	private static void usage() {
		System.out.println("Usage: GraphGenerator <properties-file>");
	}

	public static void main(String[] args) {

		if (args.length != 1) {
			usage();
			System.exit(0);
		}

		File properties = new File(args[0]);
		if (!properties.exists()) {
			System.out.println("No such file: " + properties.toString() + ".");
			usage();
			System.exit(1);
		}

		try {
			logger.info("Starting generation process...");
			long start = System.currentTimeMillis();
			GraphGenerator jbgg = new GraphGenerator(properties.toString());
			jbgg.generate();
			logger.info("Generation process finished in "
					+ (System.currentTimeMillis() - start) / 1000 + "s");
		} catch (SQLException e) {
			e.printStackTrace();
			if (e.getNextException() != null)
				e.getNextException().printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}