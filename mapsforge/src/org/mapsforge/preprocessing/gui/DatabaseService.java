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
//import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.regex.PatternSyntaxException;

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
			throw new IllegalArgumentException("Es existiert bereits ein Transportmittel mit dem angegeben Namen.");
		}
		
		
	}
	
	@Override
	public void updateTransport(Transport transport){
		
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
		if (update == 0) throw new NoSuchElementException("Kein Transportmittel " +
				"mit dem Namen: "+transport.getName()+" enthalten.");
		
	}

	@Override
	public void deleteTransport(String name) {
		String sql = "DELETE FROM transports WHERE transportname = '"+name+"';";
		Statement stmt;
		int delete = 0;
		try {
			stmt = con.createStatement();
			delete = stmt.executeUpdate(sql);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		if(delete == 0)
		{
			throw new NoSuchElementException("Kein Transportmittel " +
					"mit dem Namen: "+name+" enthalten.");
		}
	}

	@Override
	public Transport getTransport(String transportName) {
		String sql = "SELECT * FROM transports WHERE transportname="
			+ transportName + ";";
		
		ResultSet rs = null;
		String transportname = null;
		int speed = 0;
		String ways = null;
		try {
			PreparedStatement pstmt = con.prepareStatement(sql);
			rs = pstmt.executeQuery(sql);
			if (!rs.next()) 
			{
				throw new NoSuchElementException("There is no transport " +
						"object in the database with the name "+ transportName);
			}
			
			transportname = rs.getString("transportname");
			speed= rs.getInt("maxspeed");
			ways = rs.getString("useableways");
			if (rs.next()) 
			{
				throw new NoSuchElementException("There are more then one transport objects in the database with the name "
						+ transportName);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return new Transport(transportname, speed, deserialized(ways));
	}

	@Override
	public ArrayList<Transport> getAllTransports(){
		
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

	private List<Tag> deserialized(String ways) {
		ArrayList<Tag> tags = new ArrayList<Tag>();

		if(ways.length() != 0)
		{
			String[] pairs = null;
			try {
				pairs = ways.split(";");
			} catch (PatternSyntaxException e)
			{
					e.printStackTrace();
			}
			if (pairs != null)
			{
				for (String pair : pairs) {
					tags.add(new Tag(pair.split("=")[0], pair.split("=")[1]));
				}
			}
		}
		
		return tags;
	}

	@Override
	public void addProfil(Profil profil) {
		String sql = "INSERT INTO profil (profilname, url, tid, heuristic) VALUES ( ?, ?, ?, ?);";
		PreparedStatement pstmt;
		try {
			pstmt = con.prepareStatement(sql);
			pstmt.setString(1, profil.getName());
			pstmt.setString(2, profil.getUrl());
			pstmt.setString(3, profil.getTransport().getName());
			pstmt.setString(4, profil.getHeuristic());
			pstmt.executeBatch();
			pstmt.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@Override
	public ArrayList<Profil> getAllProfilsOfTransport(Transport transport){
		String sql = "SELECT * FROM profil WHERE transportname = ?;";
		PreparedStatement pstmt;
		ResultSet rs = null;
		ArrayList<Profil> profiles = new ArrayList<Profil>();
		try {
			pstmt = con.prepareStatement(sql);
			pstmt.setString(1, transport.getName());
			rs = pstmt.executeQuery(sql);
			String transportname, profilname, url, heuristic;
			Transport trans;
			while (rs.next()) {
				profilname = rs.getString("profilname");
				url = rs.getString("url");
				transportname = rs.getString("transportname");
				heuristic = rs.getString("heuristic");

				trans = getTransport(transportname);

				profiles.add(new Profil(profilname, url, trans, heuristic));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		// TODO die methode stimmt nicht, überleg nochmal was hier gemacht werden sollte!!!!!!!!!!!!!!!!!!!!!!!!
		return null;
	}

//	private void listTables(){
//		DatabaseMetaData md;
//		ResultSet rs = null;
//		try {
//			md = con.getMetaData();
//			rs = md.getTables(null, null, "%", null);
//			while (rs.next()) {
//					System.out.println(rs.getString(3));
//			}
//		} catch (SQLException e) {
//			e.printStackTrace();
//		}
//	}

//	private void getSchema(){
//		DatabaseMetaData dbmd;
//		try {
//			dbmd = con.getMetaData();
//			String[] types = { "TABLE" };
//			ResultSet resultSet = dbmd.getTables(null, null, "%", types);
//
//			while (resultSet.next()) {
//				String tableName = resultSet.getString(3);
//
//				String tableCatalog = resultSet.getString(1);
//				String tableSchema = resultSet.getString(2);
//
//				System.out.println("tableCatalog:" + tableCatalog + "; tableSchema:" + tableSchema
//						+ "; tableName:" + tableName);
//			}
//			
//		} catch (SQLException e) {
//			e.printStackTrace();
//		}
//	}

	private void createTables() {
		String trans, profil;
		trans = "CREATE TABLE Transports (Transportname VARCHAR(30) PRIMARY KEY, Maxspeed INTEGER, Useableways STRING);";
		profil = "CREATE TABLE Profil (Profilename VARCHAR(30) PRIMARY KEY, Url VARCHAR(55), TName VARCHAR(30), Heuristic VARCHAR(30));";
		try {
			Statement stmt = con.createStatement();
			stmt.executeUpdate(trans);
			stmt.executeUpdate(profil);
			stmt.close();
		} catch (SQLException e) {
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
			e.printStackTrace();
		}

	}

	public static void main(String[] args) {

		DatabaseService dbs = new DatabaseService(new JDBCConnection().getConnection());
		try {
			dbs.dropTables();
			dbs.createTables();
			//dbs.listTables();
			//dbs.getSchema();
			Tag tag1 = new Tag("highway","motorway");
			Tag tag2 = new Tag("highway","track");
			Tag tag3 = new Tag("highway","primary");
			List<Tag> list1 = new ArrayList<Tag>();
			list1.add(tag1);
			list1.add(tag3);
			
			List<Tag> list2 = new ArrayList<Tag>();
			list2.add(tag2);
			list2.add(tag3);
			
			List<Tag> list3 = new ArrayList<Tag>();
			list3.add(tag2);
			
			dbs.addTransport(new Transport("Auto", 20, list1));
			dbs.addTransport(new Transport("Fahrrad", 10, list2));
			dbs.addTransport(new Transport("Fahrrad1", 10, list3));
			//dbs.deleteTransport("Motorrad");
//			ArrayList<Transport> testlist =  dbs.getAllTransports();
//			for (Transport t : testlist) {
//				 System.out.println(t.getId()+": "+t.getName());
//				 System.out.println(t.getUseableWaysSerialized());
//			}
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				dbs.getCon().close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		// System.out.println("No bean in use!");
	}

}
