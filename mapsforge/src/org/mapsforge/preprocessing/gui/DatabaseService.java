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
package org.mapsforge.preprocessing.gui;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class DatabaseService implements IDatabaseService {

	private static Connection con;

	public DatabaseService(Connection con) {
		DatabaseService.setCon(con);
	}

	// Getter

	public Connection getCon() {
		return con;
	}

	// Setter

	public static void setCon(Connection con) {
		DatabaseService.con = con;
	}

	@Override
	public void addTransport(Transport transport) throws Exception {

		String sql;
		sql = "INSERT INTO transports (tid, transportname, maxspeed, usableways) VALUES (?, ?, ?, ?);";
		PreparedStatement pstmt = con.prepareStatement(sql);

		// pstmt.setInt(1,0);
		pstmt.setString(2, transport.getName());
		pstmt.setInt(3, transport.getMaxSpeed());
		pstmt.setString(4, transport.getUseableWaysSerialized());
		pstmt.addBatch();
		pstmt.executeBatch();
		pstmt.close();
	}

	@Override
	public Transport getTransport(int transportID) throws Exception {
		String sql;
		sql = "SELECT * FROM transports WHERE tid=" + transportID + ";";
		PreparedStatement stmt = con.prepareStatement(sql);
		ResultSet rs = stmt.executeQuery(sql);
		if (!rs.next())
			throw new Exception("There is no transport object in the database with the id "
					+ transportID);
		int tid = rs.getInt("tid");
		String transportname = rs.getString("transportname");
		int speed = rs.getInt("maxspeed");
		String ways = rs.getString("useableways");

		if (rs.next())
			throw new Exception(
					"There are more then one transport objects in the database with the id "
							+ transportID);

		return new Transport(tid, transportname, speed, deserialized(ways));
	}

	@Override
	public ArrayList<Transport> getAllTransports() throws Exception {
		String sql;
		sql = "SELECT * FROM(tid, transportname, maxspeed, usableways) VALUES (?, ?, ?, ?);";
		PreparedStatement stmt = con.prepareStatement(sql);
		ResultSet rs = stmt.executeQuery(sql);
		ArrayList<Transport> transports = new ArrayList<Transport>();
		int id;
		String name;
		int speed;
		String ways;
		while (rs.next()) {
			id = rs.getInt("tid");
			name = rs.getString("transportname");
			speed = rs.getInt("maxspeed");
			ways = rs.getString("useableways");

			transports.add(new Transport(id, name, speed, deserialized(ways)));
		}

		return transports;
	}

	private List<Tag> deserialized(String ways) {
		ArrayList<Tag> tags = new ArrayList<Tag>();

		String[] pairs = ways.split(";");
		for (String pair : pairs) {
			tags.add(new Tag(pair.split("=")[0], pair.split("=")[1]));
		}

		return tags;
	}

	@Override
	public void addProfil(Profil profil) throws Exception {
		String sql;
		sql = "INSERT INTO profil (pid, profilname, url, tid, heuristic) VALUES (?, ?, ?, ?, ?);";
		PreparedStatement stmt = con.prepareStatement(sql);

		// stmt.setInt(1, 0);
		stmt.setString(2, profil.getName());
		stmt.setString(3, profil.getUrl());
		stmt.setInt(4, profil.getTransport().getId());
		stmt.setString(5, profil.getHeuristic());
		stmt.addBatch();
		stmt.executeBatch();
		stmt.close();
	}

	@Override
	public ArrayList<Profil> getAllProfilsOfTransport(Transport transport) throws Exception {
		String sql;
		sql = "SELECT * FROM profil WHERE tid=" + transport.getId() + ";";
		PreparedStatement stmt = con.prepareStatement(sql);
		ResultSet rs = stmt.executeQuery(sql);
		ArrayList<Profil> profiles = new ArrayList<Profil>();
		int pid, tid;
		String profilname, url, heuristic;
		Transport trans;
		while (rs.next()) {
			pid = rs.getInt("pid");
			profilname = rs.getString("profilname");
			url = rs.getString("url");
			tid = rs.getInt("tid");
			heuristic = rs.getString("heuristic");

			trans = getTransport(tid);

			profiles.add(new Profil(pid, profilname, url, trans, heuristic));
		}

		// test

		// TODO Auto-generated method stub
		return null;
	}

	private void listTables() throws Exception {
		DatabaseMetaData md = con.getMetaData();
		ResultSet rs = md.getTables(null, null, "%", null);
		while (rs.next()) {
			System.out.println(rs.getString(3));
		}
	}

	private void getSchema() throws Exception {
		DatabaseMetaData dbmd = con.getMetaData();

		String[] types = { "TABLE" };
		ResultSet resultSet = dbmd.getTables(null, null, "%", types);

		while (resultSet.next()) {
			String tableName = resultSet.getString(3);

			String tableCatalog = resultSet.getString(1);
			String tableSchema = resultSet.getString(2);

			System.out.println("tableCatalog:" + tableCatalog + "; tableSchema:" + tableSchema
					+ "; tableName:" + tableName);
		}
	}

	private void createTables() {
		String trans, profil;
		trans = "CREATE TABLE Transports (TID INTEGER PRIMARY KEY, Transportname VARCHAR(30), Maxspeed INTEGER, Useableways STRING);";
		profil = "CREATE TABLE Profil (PID INTEGER PRIMARY KEY, Profilename VARCHAR(30), Url VARCHAR(55), TID INTEGER, Heuristic VARCHAR(55));";
		try {
			Statement stmt = con.createStatement();
			stmt.executeUpdate(trans);
			stmt.executeUpdate(profil);
			stmt.close();
			// con.commit();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void dropTables() {
		String trans, profil;
		trans = "DROP TABLE Transports;";
		profil = "DROP TABLE Profil;";
		try {
			Statement stmt = con.createStatement();
			stmt.executeUpdate(trans);
			stmt.executeUpdate(profil);
			stmt.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static void main(String[] args) {

		DatabaseService dbs = new DatabaseService(new JDBCConnection().getConnection());
		try {
			dbs.dropTables();
			dbs.createTables();
			dbs.listTables();
			dbs.getSchema();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				dbs.getCon().close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		// System.out.println("No bean in use!");
	}

}
