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
package org.mapsforge.preprocessing.routing.highwayHierarchies.sql;

import gnu.trove.map.hash.TIntIntHashMap;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.Iterator;

import org.mapsforge.preprocessing.routing.highwayHierarchies.HHGraphProperties;
import org.mapsforge.preprocessing.routing.highwayHierarchies.HHGraphProperties.HHLevelStats;
import org.mapsforge.preprocessing.routing.highwayHierarchies.datastructures.DistanceTable;

/**
 * @author Frank Viernau
 */
public class HHDbReader {

	public static final int FETCH_SIZE = 1000;

	private static final String SQL_COUNT_VERTICES = "SELECT COUNT(*) AS count FROM hh_vertex_lvl WHERE lvl = 0;";
	private static final String SQL_COUNT_LVL_VERTICES = "SELECT COUNT(*) AS count FROM hh_vertex_lvl;";
	private static final String SQL_COUNT_EDGES = "SELECT COUNT(*) AS count FROM hh_edge;";
	private static final String SQL_COUNT_LEVELS = "SELECT MAX(lvl) + 1 AS count FROM hh_vertex_lvl;";
	public static final String SQL_SELECT_VERTICES = "SELECT id, longitude AS lon, latitude AS lat FROM hh_vertex ORDER BY id;";
	private static final String SQL_SELECT_VERTEX_LVLS = "SELECT * FROM hh_vertex_lvl ORDER BY id, lvl;";
	private static final String SQL_SELECT_EDGES = "SELECT * FROM hh_edge ORDER BY source_id, max_lvl, min_lvl, weight;";
	private static final String SQL_SELECT_LEVEL_STATS = "SELECT * FROM hh_lvl_stats ORDER BY lvl;";
	private static final String SQL_SELECT_GRAPH_PROPERTIES = "SELECT * FROM hh_graph_properties;";

	private final Connection conn;
	private final int numVertices, numLevelVertices, numEdges, numLevels;

	public HHDbReader(Connection conn) throws SQLException {
		this.conn = conn;
		this.conn.setAutoCommit(false);
		ResultSet rs;

		rs = conn.createStatement().executeQuery(SQL_COUNT_VERTICES);
		rs.next();
		numVertices = rs.getInt("count");

		rs = conn.createStatement().executeQuery(SQL_COUNT_LVL_VERTICES);
		rs.next();
		numLevelVertices = rs.getInt("count");

		rs = conn.createStatement().executeQuery(SQL_COUNT_EDGES);
		rs.next();
		numEdges = rs.getInt("count");

		rs = conn.createStatement().executeQuery(SQL_COUNT_LEVELS);
		rs.next();
		numLevels = rs.getInt("count");
	}

	public int numVertices() {
		return numVertices;
	}

	public int numLevelVertices() {
		return numLevelVertices;
	}

	public int numEdges() {
		return numEdges;
	}

	public int numLevels() {
		return numLevels;
	}

	public DistanceTable getDistanceTable() throws SQLException {
		String selectRowCount = "SELECT count(*) AS c FROM hh_distance_table_row;";
		ResultSet rs = conn.createStatement().executeQuery(selectRowCount);
		if (rs.next()) {
			int rowCount = rs.getInt("c");
			int[][] distances = new int[rowCount][rowCount];
			TIntIntHashMap map = new TIntIntHashMap();

			String selectRows = "SELECT row_idx, vertex_id, distances FROM hh_distance_table_row;";
			rs = getStreamedResult(selectRows);
			while (rs.next()) {
				int rowIdx = rs.getInt("row_idx");
				int vertexId = rs.getInt("vertex_id");
				map.put(vertexId, rowIdx);

				Integer[] tmp = (Integer[]) rs.getArray("distances").getArray();
				for (int i = 0; i < tmp.length; i++) {
					distances[rowIdx][i] = tmp[i];
				}
			}
			return new DistanceTable(distances, map);
		}
		return null;

	}

	public HHGraphProperties getGraphProperties() throws SQLException {
		HHLevelStats[] stats = new HHLevelStats[numLevels];
		ResultSet rs = conn.createStatement().executeQuery(SQL_SELECT_LEVEL_STATS);
		while (rs.next()) {
			int lvl = rs.getInt("lvl");
			stats[lvl] = new HHLevelStats(lvl, rs.getInt("num_edges"), rs
					.getInt("num_vertices"), rs.getInt("num_core_edges"), rs
					.getInt("num_core_vertices"));
		}
		rs.close();

		rs = conn.createStatement().executeQuery(SQL_SELECT_GRAPH_PROPERTIES);
		if (rs.next()) {
			HHGraphProperties props = new HHGraphProperties(new Date(rs.getTimestamp(
					"creation_date").getTime()), rs.getString("transport"), rs.getInt("h"), rs
					.getInt("vertex_threshold"), rs.getInt("hoplimit"), rs
					.getInt("num_threads"), rs.getDouble("c"), rs.getDouble("comp_time_mins"),
					rs.getBoolean("downgraded_edges"), stats);
			return props;
		}
		return null;

	}

	public Iterator<HHVertex> getVertices() {
		try {
			return new Iterator<HHVertex>() {
				private final ResultSet rs = getStreamedResult(SQL_SELECT_VERTICES);

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
				public HHVertex next() {
					try {
						if (rs.next()) {
							return new HHVertex(rs.getInt("id"), rs.getDouble("lon"), rs
									.getDouble("lat"));
						}
					} catch (SQLException e) {
						e.printStackTrace();
					}
					return null;
				}

				@Override
				public void remove() {

				}
			};
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	}

	public Iterator<HHVertexLvl> getVertexLvls() {

		try {
			return new Iterator<HHVertexLvl>() {
				private final ResultSet rs = getStreamedResult(SQL_SELECT_VERTEX_LVLS);

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
				public HHVertexLvl next() {
					try {
						if (rs.next()) {
							return new HHVertexLvl(rs.getInt("id"), rs.getInt("neighborhood"),
									rs.getInt("lvl"));
						}
					} catch (SQLException e) {
						e.printStackTrace();
					}
					return null;
				}

				@Override
				public void remove() {

				}
			};
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	}

	public Iterator<HHEdge> getEdges() {
		try {
			return new Iterator<HHEdge>() {
				private final ResultSet rs = getStreamedResult(SQL_SELECT_EDGES);

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
				public HHEdge next() {
					try {
						if (rs.next()) {
							return new HHEdge(rs.getInt("source_id"), rs.getInt("target_id"),
									rs.getInt("weight"), rs.getInt("min_lvl"), rs
											.getInt("max_lvl"), rs.getBoolean("fwd"), rs
											.getBoolean("bwd"), rs.getBoolean("shortcut"));
						}
					} catch (SQLException e) {
						e.printStackTrace();
					}
					return null;
				}

				@Override
				public void remove() {

				}
			};
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	}

	private ResultSet getStreamedResult(String sql) throws SQLException {
		Statement stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY,
				ResultSet.CONCUR_READ_ONLY);
		stmt.setFetchSize(FETCH_SIZE);
		return stmt.executeQuery(sql);
	}

	public class HHEdge {

		public final int sourceId, targetId, weight, minLvl, maxLvl;
		public final boolean fwd, bwd, shortcut;

		public HHEdge(int sourceId, int targetId, int weight, int minLvl, int maxLvl,
				boolean fwd, boolean bwd, boolean shortcut) {
			this.sourceId = sourceId;
			this.targetId = targetId;
			this.weight = weight;
			this.minLvl = minLvl;
			this.maxLvl = maxLvl;
			this.fwd = fwd;
			this.bwd = bwd;
			this.shortcut = shortcut;
		}
	}

	public class HHVertexLvl {

		public final int id, neighborhood, lvl;

		public HHVertexLvl(int id, int neighborhood, int lvl) {
			this.id = id;
			this.neighborhood = neighborhood;
			this.lvl = lvl;
		}
	}

	public class HHVertex {
		public final int id;
		public final double longitude, latitude;

		public HHVertex(int id, double longitude, double latitude) {
			this.id = id;
			this.longitude = longitude;
			this.latitude = latitude;
		}
	}
}
