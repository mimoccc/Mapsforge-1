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
package org.mapsforge.preprocessing.routing;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.mapsforge.preprocessing.model.ITransportConfigurationPreprocessing;
import org.mapsforge.preprocessing.util.DBConnection;

public class ReachDAO {

	public static final int BATCH_SIZE = 1000;

	private Connection conn;

	public ReachDAO(String propertiesFile) throws Exception {
		this.conn = new DBConnection(propertiesFile).getConnection();
	}

	public void deleteReaches(ITransportConfigurationPreprocessing conf) {
		try {
			PreparedStatement pstmt = conn
					.prepareStatement("DELETE FROM reaches WHERE configuration = ?");
			pstmt.setString(1, conf.name());
			pstmt.executeUpdate();
			conn.commit();
		} catch (SQLException e) {
			e.printStackTrace();
			if (e.getNextException() != null)
				e.getNextException().printStackTrace();
		}
	}

	public void writeReaches(double[][] reaches, ArrayBasedRoutingGraph graph,
			ITransportConfigurationPreprocessing conf) {
		String sqlReaches = "INSERT INTO reaches (node_id, configuration, reach) VALUES (?,?,?)";

		String sqlReachesForNodesOfNoReturn = "INSERT INTO reaches (node_id, reach, configuration) "
				+ "SELECT tmp.id, 0, '"
				+ conf.name()
				+ "' "
				+ "FROM (SELECT osm_id AS id FROM id_mapping EXCEPT SELECT node_id FROM reaches) tmp";

		try {
			PreparedStatement pstmtReach = conn.prepareStatement(sqlReaches);
			double currentMax;
			for (int i = 0; i < graph.size(); i++) {
				currentMax = 0;
				for (int j = 0; j < reaches.length; j++) {
					currentMax = Math.max(currentMax, reaches[j][i]);
				}

				pstmtReach.setLong(1, graph.osmID(i));
				pstmtReach.setString(2, conf.name());
				pstmtReach.setInt(3, (int) currentMax);
				pstmtReach.addBatch();

				if ((i) % BATCH_SIZE == 0)
					pstmtReach.executeBatch();
			}

			pstmtReach.executeBatch();

			int affectedRows = conn.createStatement().executeUpdate(
					sqlReachesForNodesOfNoReturn);
			System.out.println("added " + affectedRows
					+ " rows in reach table for 'nodes with no return'");

			conn.commit();

		} catch (SQLException e) {
			e.printStackTrace();
			if (e.getNextException() != null)
				e.getNextException().printStackTrace();
		}
	}

	@Override
	protected void finalize() throws Throwable {
		if (conn != null && !conn.isClosed())
			conn.close();
	}

}
