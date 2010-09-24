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
package org.mapsforge.preprocessing.routing.blockedHighwayHierarchies;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;
import java.util.Random;

import org.mapsforge.core.DBConnection;
import org.mapsforge.preprocessing.routing.blockedHighwayHierarchies.LevelGraph.Level;
import org.mapsforge.preprocessing.routing.blockedHighwayHierarchies.LevelGraph.Level.LevelVertex;
import org.mapsforge.preprocessing.routing.highwayHierarchies.util.Serializer;

class Evaluation {

	private static final Random rnd = new Random();

	// database
	private static final String DB_HOST = "localhost";
	private static final int DB_PORT = 5432;
	private static final String DB_USER = "osm";
	private static final String DB_PASS = "osm";

	// graphs
	private static final int GRAPHS_COUNT = 27;
	private static final String GRAPHS_NAME_PREFIX = "ger_";

	// test routes
	private static final int ROUTES_COUNT = 1000;
	private static final int[] ROUTES_RANKS = new int[] { 1000, 10000 };

	static void generateRoutes(LevelGraph levelGraph) throws IOException {
		for (int rank : ROUTES_RANKS) {
			generateRoutes(levelGraph, ROUTES_COUNT, rank, new File("evaluation/routes/" + rank
					+ ".txt"));
		}
	}

	static void generateRoutes(LevelGraph levelGraph, int n, int rank, File targetFile)
			throws IOException {
		System.out.println("generateRoutes");
		System.out.println("  n = " + n);
		System.out.println("  rank = " + rank);
		System.out.println("  targetFile = " + targetFile.getAbsolutePath());
		DijkstraAlgorithm dijkstra = new DijkstraAlgorithm(levelGraph);
		FileWriter writer = new FileWriter(targetFile);
		Level graph = levelGraph.getLevel(0);
		for (int i = 0; i < n; i++) {
			int sourceId = rnd.nextInt(levelGraph.numVertices);
			int taretId = dijkstra.getVertexByDijkstraRank(sourceId, rank);
			if (taretId == -1) {
				i--;
				continue;
			}

			LevelVertex s = graph.getVertex(sourceId);
			LevelVertex t = graph.getVertex(taretId);
			writer.write(s.getCoordinate().getLatitude() + ","
					+ s.getCoordinate().getLongitude());
			writer.write(";");
			writer.write(t.getCoordinate().getLatitude() + ","
					+ t.getCoordinate().getLongitude());
			writer.write("\n");
			if ((i + 1) % 10 == 0)
				System.out.println((i + 1) + "/" + n);
		}
		writer.flush();
		writer.close();
		System.out.println("ready!");
	}

	static void serializeLevelGraphs()
			throws SQLException, IOException {
		for (int i = 27; i <= GRAPHS_COUNT; i++) {

			String graphName = GRAPHS_NAME_PREFIX;
			if (i < 10) {
				graphName += "0";
			}
			graphName += i;
			Connection conn = getConnection(graphName);
			Date date = new Date(System.currentTimeMillis());
			System.out.println("serializing graph : '" + graphName + "'" + " "
					+ date.getHours() + ":" + date.getMinutes() + "h");
			LevelGraph levelGraph = new LevelGraph(conn);
			Serializer.serialize(new File("evaluation/graphs/" + graphName + ".levelGraph"),
					levelGraph);
			levelGraph = null;
			conn.close();
			System.gc();
		}
	}

	static Connection getConnection(String dbName) throws SQLException {
		return DBConnection.getJdbcConnectionPg(DB_HOST, DB_PORT, dbName,
				DB_USER,
				DB_PASS);
	}

	public static void main(String[] args) throws SQLException, IOException {
		// generate routes
		// LevelGraph levelGraph = new LevelGraph(getConnection("berlin"));
		// generateRoutes(levelGraph);
		// levelGraph = null;

		// serialize graphs
		serializeLevelGraphs();

	}
}
