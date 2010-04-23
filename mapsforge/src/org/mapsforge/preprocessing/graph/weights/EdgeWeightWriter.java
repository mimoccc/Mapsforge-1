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
package org.mapsforge.preprocessing.graph.weights;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;

import org.mapsforge.core.conf.IVehicle;
import org.mapsforge.core.conf.Vehicle;
import org.mapsforge.preprocessing.model.EHighwayLevel;
import org.mapsforge.preprocessing.model.Edge;
import org.mapsforge.preprocessing.util.DBConnection;

public class EdgeWeightWriter {

	public static final int CONST_SECOND_TO_MILLISECOND = 1000;

	private static final Logger logger = Logger.getLogger(EdgeWeightWriter.class.getName());

	private static final int FETCH_SIZE = 1000;
	private static final int BATCH_SIZE = 10000;
	private Connection conn;

	public EdgeWeightWriter(String propertiesFile) throws Exception {
		DBConnection dbConnection = new DBConnection(propertiesFile);
		conn = dbConnection.getConnection();
		conn.setAutoCommit(false);
	}

	@Override
	protected void finalize() throws Throwable {
		if (!conn.isClosed())
			conn.close();
	}

	public void generateWeights(IVehicle transport) throws SQLException {

		logger.info("deleting old weights for this transport and heuristic");
		PreparedStatement pstmtDelOld = conn
				.prepareStatement("DELETE FROM weights WHERE configuration = ?");
		pstmtDelOld.setString(1, transport.name());
		int deleted = pstmtDelOld.executeUpdate();
		logger.info("deleted " + deleted + " rows");

		String SQL_EDGES = "SELECT id, length, level, maxspeed, traffic_light, urban FROM edges ";

		String SQL_WEIGHT = "INSERT INTO weights (edge_id, configuration, weight) VALUES (?,?,?)";

		PreparedStatement pstmtEdges = conn.prepareStatement(SQL_EDGES);
		PreparedStatement pstmtWeight = conn.prepareStatement(SQL_WEIGHT);

		pstmtEdges.setFetchSize(FETCH_SIZE);
		ResultSet rs = pstmtEdges.executeQuery();
		Edge edge = new Edge();
		int edgeID;
		int weight;
		int batchCount = 0;
		while (rs.next()) {

			if (!transport.isValidHighwayLevel(rs.getString(3)))
				continue;

			edgeID = rs.getInt(1);
			edge.length = rs.getInt(2);
			edge.level = EHighwayLevel.valueOf(rs.getString(3));
			edge.maxspeed = rs.getString(4);
			edge.trafficLights = rs.getInt(5);
			edge.urban = rs.getBoolean(6);

			weight = (int) (rs.getInt(2) / (transport.getAvgSpeed() / 3600f));

			pstmtWeight.setInt(1, edgeID);
			pstmtWeight.setString(2, transport.name());
			pstmtWeight.setInt(3, weight);
			pstmtWeight.addBatch();

			if (++batchCount % BATCH_SIZE == 0) {
				logger.info("executing batch for batch " + (batchCount - BATCH_SIZE) + " - "
						+ batchCount);
				pstmtWeight.executeBatch();
			}
		}

		pstmtWeight.executeBatch();
		logger.info("committing transaction");
		conn.commit();

		rs.close();
		pstmtWeight.close();
		pstmtEdges.close();
	}

	private static void usage() {
		System.out.println("Usage: EdgeWeightWriter <properties-file>");
	}

	public static void main(String[] args) throws Exception {

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

		EdgeWeightWriter writer = new EdgeWeightWriter(properties.toString());
		writer.generateWeights(Vehicle.STANDARD_CAR__SIMPLE_HEURISTIC);
	}

}
