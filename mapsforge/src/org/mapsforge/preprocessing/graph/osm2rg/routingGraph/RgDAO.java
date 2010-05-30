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
package org.mapsforge.preprocessing.graph.osm2rg.routingGraph;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Iterator;

import org.mapsforge.preprocessing.graph.routingGraphInterface.IRgDAO;
import org.mapsforge.preprocessing.model.EHighwayLevel;

/**
 * @author Frank Viernau Implementation of the Routing graph interface.
 */
public class RgDAO implements IRgDAO<RgVertex, RgEdge> {

	private static final int FETCH_SIZE = 1000;

	private static final String SQL_COUNT_VERTICES = "SELECT COUNT(*) AS count FROM rg_vertex;";
	private static final String SQL_COUNT_EDGES = "SELECT COUNT(*) AS count FROM rg_edge;";
	private static final String SQL_COUNT_WAYPOINTS = "SELECT sum(array_length(longitudes, 1) - 2) AS count FROM rg_edge;";
	private static final String SQL_SELECT_HIGHWAY_LEVELS = "SELECT id, name FROM rg_hwy_lvl;";
	private static final String SQL_SELECT_VERTICES = "SELECT id, osm_node_id, lon, lat FROM rg_vertex ORDER BY id;";
	private static final String SQL_SELECT_EDGES = "SELECT id, osm_way_id, source_id, target_id, length_meters, longitudes, latitudes, name, hwy_lvl, undirected, urban FROM rg_edge ORDER BY id;";

	private final Connection conn;
	private final HashMap<Integer, EHighwayLevel> hwyLvlInt2E;
	private final int numVertices, numEdges, numWaypoints;

	public RgDAO(Connection conn) throws SQLException {
		this.conn = conn;
		this.hwyLvlInt2E = new HashMap<Integer, EHighwayLevel>();
		conn.setAutoCommit(false);
		ResultSet rs;

		rs = conn.createStatement().executeQuery(SQL_COUNT_VERTICES);
		rs.next();
		numVertices = rs.getInt("count");
		rs.close();

		rs = conn.createStatement().executeQuery(SQL_COUNT_EDGES);
		rs.next();
		numEdges = rs.getInt("count");
		rs.close();

		rs = conn.createStatement().executeQuery(SQL_COUNT_WAYPOINTS);
		rs.next();
		numWaypoints = rs.getInt("count");
		rs.close();

		rs = conn.createStatement().executeQuery(SQL_SELECT_HIGHWAY_LEVELS);
		while (rs.next()) {
			int id = rs.getInt("id");
			String name = rs.getString("name");
			hwyLvlInt2E.put(id, EHighwayLevel.valueOf(name));
		}
	}

	@Override
	public int getNumEdges() {
		return numEdges;
	}

	@Override
	public int getNumVertices() {
		return numVertices;
	}

	public int getNumWaypoints() {
		return numWaypoints;
	}

	@Override
	public Iterable<RgVertex> getVertices() {
		return new Iterable<RgVertex>() {

			@Override
			public Iterator<RgVertex> iterator() {
				try {
					return getVertexIterator();
				} catch (SQLException e) {
					e.printStackTrace();
					return null;
				}
			}
		};
	}

	@Override
	public Iterable<RgEdge> getEdges() {
		return new Iterable<RgEdge>() {
			@Override
			public Iterator<RgEdge> iterator() {
				try {
					return getEdgeIterator();
				} catch (SQLException e) {
					e.printStackTrace();
					return null;
				}
			}

		};
	}

	public Iterable<EHighwayLevel> getHighwayLevels() {
		return new Iterable<EHighwayLevel>() {

			@Override
			public Iterator<EHighwayLevel> iterator() {
				return hwyLvlInt2E.values().iterator();
			}
		};
	}

	private Iterator<RgVertex> getVertexIterator() throws SQLException {
		return new Iterator<RgVertex>() {

			private ResultSet rs = getStreamedResult(SQL_SELECT_VERTICES);

			@Override
			public boolean hasNext() {
				try {
					return !(rs.isLast() || rs.isAfterLast());
				} catch (SQLException e) {
					e.printStackTrace();
					while (e.getNextException() != null) {
						e = e.getNextException();
						e.printStackTrace();
					}
					return false;
				}
			}

			@Override
			public RgVertex next() {
				try {
					if (rs.next()) {
						return new RgVertex(rs.getInt("id"), rs.getDouble("lon"), rs
								.getDouble("lat"), rs.getLong("osm_node_id"));
					}
					return null;

				} catch (SQLException e) {
					e.printStackTrace();
					while (e.getNextException() != null) {
						e = e.getNextException();
						e.printStackTrace();
					}
					return null;
				}
			}

			@Override
			public void remove() {

			}
		};
	}

	private Iterator<RgEdge> getEdgeIterator() throws SQLException {
		return new Iterator<RgEdge>() {

			private ResultSet rs = getStreamedResult(SQL_SELECT_EDGES);

			@Override
			public boolean hasNext() {
				try {
					boolean ret = !(rs.isLast() || rs.isAfterLast());
					if (!ret) {
						rs.close();
					}
					return ret;
				} catch (SQLException e) {
					e.printStackTrace();
					while (e.getNextException() != null) {
						e = e.getNextException();
						e.printStackTrace();
					}
					return false;
				}
			}

			@Override
			public RgEdge next() {
				try {
					if (rs.next()) {
						Double[] lon = (Double[]) rs.getArray("longitudes").getArray();
						Double[] lat = (Double[]) rs.getArray("latitudes").getArray();
						double[] lon_ = new double[lon.length];
						double[] lat_ = new double[lat.length];
						for (int i = 0; i < lon.length; i++) {
							lon_[i] = lon[i];
							lat_[i] = lat[i];
						}
						return new RgEdge(rs.getInt("id"), rs.getInt("source_id"), rs
								.getInt("target_id"), lon_, lat_, rs.getBoolean("undirected"),
								rs.getBoolean("urban"), rs.getLong("osm_way_id"), rs
										.getString("name"), rs.getDouble("length_meters"),
								hwyLvlInt2E.get(rs.getInt("hwy_lvl")));
					}
					return null;

				} catch (SQLException e) {
					e.printStackTrace();
					while (e.getNextException() != null) {
						e = e.getNextException();
						e.printStackTrace();
					}
					return null;
				}
			}

			@Override
			public void remove() {

			}
		};
	}

	private ResultSet getStreamedResult(String sql) throws SQLException {
		Statement stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY,
				ResultSet.CONCUR_READ_ONLY);
		stmt.setFetchSize(FETCH_SIZE);
		return stmt.executeQuery(sql);
	}

	// public static void main(String[] args) throws SQLException {
	// RgDAO dao = new RgDAO(DbConnection.getBerlinDbConn());
	// Iterator<RgVertex> iter = dao.getVertices().iterator();
	// while(iter.hasNext()) {
	// RgVertex v = iter.next();
	// System.out.println(v.getLongitude());
	// System.out.println(v.getLatitude());
	// }
	// Iterator<RgEdge> iter2 = dao.getEdges().iterator();
	// while(iter2.hasNext()) {
	// System.out.println(iter2.next());
	// }
	// }
}
