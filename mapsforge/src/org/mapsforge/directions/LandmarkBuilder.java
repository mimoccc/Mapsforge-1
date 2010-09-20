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
package org.mapsforge.directions;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.mapsforge.core.DBConnection;
import org.mapsforge.core.GeoCoordinate;

/**
 * Builder for Landmark instances.
 * 
 * @author Eike Send
 */
public class LandmarkBuilder {
	private Connection conn;
	private PreparedStatement nearestLandmarkByID;
	private PreparedStatement nearestLandmarkByCoordinates;

	/**
	 * @param junction
	 *            describes the location of the decision point where to look for a Landmark
	 * @return nearest Landmark
	 * @throws SQLException
	 *             if sth goes wrong
	 */
	public Landmark getNearestLandMark(GeoCoordinate junction) {
		try {
			nearestLandmarkByCoordinates.setString(1, "POINT(" + junction.getLongitude() + " "
					+ junction.getLatitude() + ")");
			ResultSet rs = nearestLandmarkByCoordinates.executeQuery();
			if (rs.next()) {
				GeoCoordinate p = new GeoCoordinate(rs.getString("wkt"));
				return new Landmark(p, rs.getString("name"), rs.getString("key"), rs
						.getString("value"));
			}
			return null;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * @param osmId
	 *            OpenStreetMap ID of the junction node where to look for a Landmark
	 * @return nearest Landmark
	 * @throws SQLException
	 *             if sth goes wrong
	 */
	public Landmark getNearestLandMark(int osmId) {
		try {
			nearestLandmarkByID.setInt(1, osmId);
			ResultSet rs = nearestLandmarkByID.executeQuery();
			if (rs.next()) {
				GeoCoordinate p = new GeoCoordinate(rs.getString("wkt"));
				return new Landmark(p, rs.getString("name"), rs.getString("key"), rs
						.getString("value"));
			}
			return null;
		} catch (SQLException e) {
			return null;
		}
	}

	/**
	 * Constructor using default database settings
	 * 
	 * @throws SQLException
	 *             if things go wrong
	 */
	public LandmarkBuilder() {
		DBConnection dbconn;
		try {
			dbconn = new DBConnection("localhost", "osm", "osm", "osm", 3128);
			conn = dbconn.getConnection();
			init();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Constructor providing database settings
	 * 
	 * Same parameters as in DBConnection
	 * 
	 * @param db_host
	 *            host
	 * @param db_name
	 *            Database name
	 * @param db_user
	 *            Database user
	 * @param db_pw
	 *            Database password
	 * @param db_port
	 *            Database port
	 * @throws SQLException
	 *             if things go wrong
	 */
	public LandmarkBuilder(String db_host,
			String db_name,
			String db_user,
			String db_pw,
			int db_port) throws SQLException {
		DBConnection dbconn = new DBConnection(db_host, db_name, db_user, db_pw, db_port);
		conn = dbconn.getConnection();
		init();
	}

	/**
	 * Constructor to be used if a database Connection is already available
	 * 
	 * @param conn
	 *            the Connection to the DB
	 * @throws SQLException
	 *             if things go wrong
	 */
	public LandmarkBuilder(Connection conn) throws SQLException {
		this.conn = conn;
		init();
	}

	@Override
	protected void finalize() {
		try {
			conn.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void init() throws SQLException {
		String queryTagString =
				"AND ( " +
						"(tags.k = 'railway' AND tags.v = 'station') OR " +
						"(tags.k = 'railway' AND tags.v = 'halt') OR " +
						"(tags.k = 'railway' AND tags.v = 'tram_stop') OR " +
						"(tags.k = 'amenity' AND tags.v = 'bank') OR " +
						"(tags.k = 'amenity' AND tags.v = 'pharmacy') OR " +
						"(tags.k = 'amenity' AND tags.v = 'hospital') OR " +
						"(tags.k = 'amenity' AND tags.v = 'cinema') OR " +
						"(tags.k = 'amenity' AND tags.v = 'fuel') OR " +
						"(tags.k = 'amenity' AND tags.v = 'theatre') OR " +
						"(tags.k = 'amenity' AND tags.v = 'police') OR " +
						"(tags.k = 'amenity' AND tags.v = 'post_office') OR " +
						"(tags.k = 'shop' AND tags.v = 'supermarket') OR " +
						"(tags.k = 'tourism' AND tags.v = 'hostel') OR " +
						"(tags.k = 'tourism' AND tags.v = 'hotel') OR " +
						"(tags.k = 'tourism' AND tags.v = 'motel') OR " +
						"(tags.k = 'tourism' AND tags.v = 'zoo') OR " +
						// "(tags.k = 'highway' AND tags.v = 'traffic_signals') OR " +
						"(tags.k = 'highway' AND tags.v = 'crossing') OR " +
						"(tags.k = 'highway' AND tags.v = 'services') " +
						" ) " +
						"ORDER BY dist " +
						"LIMIT 1";
		nearestLandmarkByID = conn
				.prepareStatement(
				"SELECT names.v AS name, tags.k AS key, tags.v AS value, ST_Distance(junction.geom, landmark.geom, true) AS dist, ST_AsText(landmark.geom) AS wkt "
						+
						"FROM nodes AS junction, nodes AS landmark "
						+
						"JOIN node_tags AS tags ON tags.node_id = landmark.id  "
						+
						"LEFT OUTER JOIN node_tags AS names ON names.node_id = landmark.id AND names.k = 'name'"
						+
						"WHERE ST_DWithin(junction.geom, landmark.geom, 150, true) " +
						"AND junction.id = ? " +
						queryTagString);
		nearestLandmarkByCoordinates = conn
				.prepareStatement(
				"SELECT names.v AS name, tags.k AS key, tags.v AS value, ST_Distance(junction, landmark.geom, true) AS dist, ST_AsText(landmark.geom) AS wkt "
						+
						"FROM "
						+
						"(SELECT GeometryFromText(?, 4326) AS junction) AS foo, "
						+
						"nodes AS landmark "
						+
						"JOIN node_tags AS tags ON tags.node_id = landmark.id "
						+
						"LEFT OUTER JOIN node_tags AS names ON names.node_id = landmark.id AND names.k = 'name' "
						+
						"WHERE ST_DWithin(junction, landmark.geom, 150, true) " +
						queryTagString);

	}

}
