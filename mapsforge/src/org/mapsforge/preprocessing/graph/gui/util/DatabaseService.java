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
package org.mapsforge.preprocessing.graph.gui.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.regex.PatternSyntaxException;

import org.mapsforge.preprocessing.graph.model.gui.DatabaseProperties;
import org.mapsforge.preprocessing.graph.model.gui.Profil;
import org.mapsforge.preprocessing.graph.model.gui.Transport;
import org.mapsforge.preprocessing.model.EHighwayLevel;
import org.mapsforge.preprocessing.util.HighwayLevelExtractor;

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
	public void addTransport(Transport transport) {

		String sql = "INSERT INTO transports (transportname, maxspeed, useableways) VALUES (?, ?, ?);";
		try {
			PreparedStatement pstmt = con.prepareStatement(sql);
			pstmt.setString(1, transport.getName());
			pstmt.setInt(2, transport.getMaxSpeed());
			pstmt.setString(3, transport.getUseableWaysSerialized());
			pstmt.executeUpdate();
			pstmt.close();
		} catch (SQLException e) {
			throw new IllegalArgumentException(
					"Can't create transport confiuration. There already exits one with this name.");
		}

	}

	@Override
	public void updateTransport(Transport transport) {

		String sql = "UPDATE Transports SET transportname = ?, maxspeed = ?, useableways = ? WHERE transportname = ? ;";
		int update = 0;
		try {
			PreparedStatement pstmt = con.prepareStatement(sql);
			pstmt.setString(1, transport.getName());
			pstmt.setInt(2, transport.getMaxSpeed());
			pstmt.setString(3, transport.getUseableWaysSerialized());
			pstmt.setString(4, transport.getName());
			update = pstmt.executeUpdate();
			pstmt.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		// no update, because this transport, doesn't exists
		if (update == 0)
			throw new NoSuchElementException(
					"There did not existing any transport configuration with this name: "
							+ transport.getName());

	}

	@Override
	public void deleteTransport(String name) {
		String sql = "DELETE FROM transports WHERE transportname = '" + name + "';";
		Statement stmt;
		int delete = 0;
		try {
			stmt = con.createStatement();
			delete = stmt.executeUpdate(sql);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		if (delete == 0) {
			throw new NoSuchElementException(
					"There did not existing any transport configuration with this name: "
							+ name + " enthalten.");
		}
	}

	@Override
	public Transport getTransport(String transportName) {
		String sql = "SELECT * FROM transports WHERE transportname = ?;";

		ResultSet rs = null;
		String transportname = null;
		int speed = 0;
		String ways = null;
		try {

			PreparedStatement pstmt = con.prepareStatement(sql);
			pstmt.setString(1, transportName);
			rs = pstmt.executeQuery();

			if (!rs.next()) {
				throw new NoSuchElementException("There is no transport "
						+ "object in the database with the name " + transportName);
			}

			transportname = rs.getString("transportname");
			speed = rs.getInt("maxspeed");
			ways = rs.getString("useableways");
			if (rs.next()) {
				throw new NoSuchElementException(
						"There are more then one transport objects in the database with the name "
								+ transportName);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return new Transport(transportname, speed, deserialized(ways));
	}

	@Override
	public ArrayList<Transport> getAllTransports() {

		String sql = "SELECT * FROM transports;";
		Statement stmt;
		ResultSet rs = null;
		ArrayList<Transport> transports = new ArrayList<Transport>();
		try {
			stmt = con.createStatement();
			rs = stmt.executeQuery(sql);

			String name;
			int speed;
			String ways;
			while (rs.next()) {
				name = rs.getString("transportname");
				speed = rs.getInt("maxspeed");
				ways = rs.getString("useableways");

				transports.add(new Transport(name, speed, deserialized(ways)));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return transports;
	}

	/*
	 * public DatabaseProperties getDefaultDbConfig() {
	 * 
	 * DatabaseProperties dbProps = null; String sql =
	 * "SELECT * FROM DbConfigurations WHERE id=" + 0 + ";"; Statement stmt; ResultSet rs =
	 * null; try { stmt = con.createStatement(); rs = stmt.executeQuery(sql);
	 * 
	 * String host, dbname, username, password; int port; while (rs.next()) { host =
	 * rs.getString("host"); dbname = rs.getString("dbname"); username =
	 * rs.getString("username"); password = rs.getString("password"); port = rs.getInt("port");
	 * 
	 * dbProps = new DatabaseProperties(host, port, dbname, username, password); } } catch
	 * (SQLException e) { e.printStackTrace(); }
	 * 
	 * return dbProps; }
	 */

	public DatabaseProperties getDbConfig() {

		DatabaseProperties dbProps = null;
		String sql = "SELECT * FROM DbConfigurations WHERE id=" + 0 + ";";
		Statement stmt;
		ResultSet rs = null;
		try {
			stmt = con.createStatement();
			rs = stmt.executeQuery(sql);

			String host, dbname, username, password;
			int port;
			while (rs.next()) {
				host = rs.getString("host");
				dbname = rs.getString("dbname");
				username = rs.getString("username");
				password = rs.getString("password");
				port = rs.getInt("port");

				dbProps = new DatabaseProperties(host, port, dbname, username, password);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return dbProps;
	}

	@Override
	public void addProfil(Profil profil) {
		String sql = "INSERT INTO profil (profilname, url, transport, heuristic) VALUES ( ?, ?, ?, ?);";
		PreparedStatement pstmt;
		try {
			pstmt = con.prepareStatement(sql);
			pstmt.setString(1, profil.getName());
			pstmt.setString(2, profil.getUrl());
			pstmt.setString(3, profil.getTransport().getName());
			pstmt.setString(4, profil.getHeuristic());
			pstmt.execute();
			pstmt.close();
		} catch (SQLException e) {
			throw new IllegalArgumentException(
					"Es existiert bereits ein Profil mit dem angegeben Namen.");
		}
	}

	// eine methode zum initialisieren
	public void setDefaultDatabaseConfig(DatabaseProperties dbProps) {

		String sql = "INSERT INTO DbConfigurations (id,host,dbname,username,password,port) VALUES (?,?,?,?,?,?);";
		int update = 0;
		try {
			PreparedStatement pstmt = con.prepareStatement(sql);
			pstmt.setInt(1, 0);
			pstmt.setString(2, dbProps.getHost());
			pstmt.setString(3, dbProps.getDbName());
			pstmt.setString(4, dbProps.getUsername());
			pstmt.setString(5, dbProps.getPassword());
			pstmt.setInt(6, dbProps.getPort());
			update = pstmt.executeUpdate();
			pstmt.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void addDatabaseConfig(DatabaseProperties dbProps) throws Exception {

		String sql = "UPDATE DbConfigurations SET id = ?, host = ?, dbname = ?, username = ?, password = ?, port = ? WHERE id ="
				+ 0 + " ;";
		int update = 0;
		try {
			PreparedStatement pstmt = con.prepareStatement(sql);
			pstmt.setInt(1, 0);
			pstmt.setString(2, dbProps.getHost());
			pstmt.setString(3, dbProps.getDbName());
			pstmt.setString(4, dbProps.getUsername());
			pstmt.setString(5, dbProps.getPassword());
			pstmt.setInt(6, dbProps.getPort());
			update = pstmt.executeUpdate();
			pstmt.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		if (update == 0) {
			throw new Exception(
					"An unexpected error occurs while inserting the configuration into the database.");
		}
	}

	public void updateProfil(Profil p) {
		String sql = "UPDATE Profil SET profilname = ?, url = ?, transport = ?, heuristic = ? WHERE profilname = ? ;";
		int update = 0;
		try {
			PreparedStatement pstmt = con.prepareStatement(sql);
			pstmt.setString(1, p.getName());
			pstmt.setString(2, p.getUrl());
			pstmt.setString(3, p.getTransport().getName());
			pstmt.setString(4, p.getHeuristic());
			pstmt.setString(5, p.getName());
			update = pstmt.executeUpdate();
			pstmt.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		// no update, because this transport, doesn't exists
		if (update == 0)
			throw new NoSuchElementException("There existing no such profil  " + p.getName());
	}

	public void deleteProfil(String name) {
		String sql = "DELETE FROM Profil WHERE profilname = '" + name + "';";
		Statement stmt;
		int delete = 0;
		try {
			stmt = con.createStatement();
			delete = stmt.executeUpdate(sql);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		if (delete == 0) {
			throw new NoSuchElementException("There existing no such profil  " + name);
		}

	}

	@Override
	public ArrayList<Profil> getProfilesOfTransport(Transport transport) {
		String sql = "SELECT * FROM profil WHERE transport = ? ;";
		PreparedStatement pstmt;
		ResultSet rs = null;
		ArrayList<Profil> profiles = new ArrayList<Profil>();
		try {
			pstmt = con.prepareStatement(sql);
			pstmt.setString(1, transport.getName());
			rs = pstmt.executeQuery();
			String transportname, profilname, url, heuristic;
			Transport trans;
			while (rs.next()) {
				profilname = rs.getString("profilname");
				url = rs.getString("url");
				transportname = rs.getString("transport");
				heuristic = rs.getString("heuristic");

				trans = getTransport(transportname);

				profiles.add(new Profil(profilname, url, trans, heuristic));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return profiles;
	}

	@Override
	public ArrayList<Profil> getAllProfiles() {
		String sql = "SELECT * FROM profil;";
		Statement stmt;
		ResultSet rs = null;
		ArrayList<Profil> profiles = new ArrayList<Profil>();
		try {
			stmt = con.createStatement();
			rs = stmt.executeQuery(sql);
			String transportname, profilname, url, heuristic;
			Transport trans;
			while (rs.next()) {
				profilname = rs.getString("profilname");
				url = rs.getString("url");
				transportname = rs.getString("transport");
				heuristic = rs.getString("heuristic");

				trans = getTransport(transportname);

				profiles.add(new Profil(profilname, url, trans, heuristic));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return profiles;
	}

	private HashSet<EHighwayLevel> deserialized(String ways) {
		HashSet<EHighwayLevel> hwyLvls = new HashSet<EHighwayLevel>();

		if (ways.length() != 0) {
			String[] pairs = null;
			try {
				pairs = ways.split(";");
			} catch (PatternSyntaxException e) {
				e.printStackTrace();
			}
			if (pairs != null) {
				EHighwayLevel hwyLvl = null;
				for (String pair : pairs) {

					hwyLvl = HighwayLevelExtractor.getLevel(pair);
					if (hwyLvl != null && !hwyLvls.contains(hwyLvl))
						hwyLvls.add(hwyLvl);
				}
			}
		}

		return hwyLvls;
	}

	private void createTables() {
		String trans, profil, dbConfig;
		trans = "CREATE TABLE Transports (Transportname VARCHAR(30) PRIMARY KEY, Maxspeed INTEGER, Useableways STRING);";
		profil = "CREATE TABLE Profil (Profilname VARCHAR(30) PRIMARY KEY, Url VARCHAR(55), Transport VARCHAR(30), Heuristic VARCHAR(30));";
		dbConfig = "CREATE TABLE DbConfigurations(id INTEGER PRIMARY KEY, host VARCHAR(30), dbname VARCHAR(30), username VARCHAR(30), password VARCHAR(30), port INTEGER)";
		try {
			Statement stmt = con.createStatement();
			stmt.executeUpdate(trans);
			stmt.executeUpdate(profil);
			stmt.executeUpdate(dbConfig);
			stmt.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private void dropTables() {
		String trans, profil, dbConfig;
		trans = "DROP TABLE Transports;";
		profil = "DROP TABLE Profil;";
		dbConfig = "DROP TABLE DbConfigurations;";
		try {
			Statement stmt = con.createStatement();
			stmt.executeUpdate(trans);
			stmt.executeUpdate(profil);
			stmt.executeUpdate(dbConfig);
			stmt.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}

	public static void main(String[] args) {

		DatabaseService dbs = new DatabaseService(new JDBCConnection().getConnection());
		try {
			dbs.dropTables();
			dbs.createTables();
			// dbs.listTables();
			// dbs.getSchema();
			EHighwayLevel hwyLvl1 = HighwayLevelExtractor.getLevel("motorway");
			EHighwayLevel hwyLvl2 = HighwayLevelExtractor.getLevel("track");
			EHighwayLevel hwyLvl3 = HighwayLevelExtractor.getLevel("primary");
			HashSet<EHighwayLevel> set1 = new HashSet<EHighwayLevel>();
			set1.add(hwyLvl1);
			set1.add(hwyLvl3);

			HashSet<EHighwayLevel> set2 = new HashSet<EHighwayLevel>();
			set2.add(hwyLvl2);
			set2.add(hwyLvl3);

			HashSet<EHighwayLevel> set3 = new HashSet<EHighwayLevel>();
			set3.add(hwyLvl2);

			Transport auto = new Transport("Auto", 20, set1);
			dbs.addTransport(auto);
			Transport fahrrad = new Transport("Fahrrad1", 10, set3);
			dbs.addTransport(new Transport("Fahrrad", 10, set2));
			dbs.addTransport(fahrrad);

			dbs.addProfil(new Profil("Testprofil", "keineUrl", auto, "keineHeuristic"));
			dbs.addProfil(new Profil("Testprofil2", "keineUrl", auto, "keineHeuristic"));
			dbs.addProfil(new Profil("Testprofil3", "keineUrl", fahrrad, "keineHeuristic"));

			ArrayList<Profil> profiles = dbs.getAllProfiles();
			for (Profil p : profiles) {
				System.out.println(p.getName());
			}
			DatabaseProperties dbProps = new DatabaseProperties("localhost", 5432, "osm_base",
					"postgres", "bachelor");
			dbs.setDefaultDatabaseConfig(dbProps);
			// dbs.deleteTransport("Motorrad");
			// ArrayList<Transport> testlist = dbs.getAllTransports();
			// for (Transport t : testlist) {
			// System.out.println(t.getId()+": "+t.getName());
			// System.out.println(t.getUseableWaysSerialized());
			// }

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				dbs.getCon().close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
}
