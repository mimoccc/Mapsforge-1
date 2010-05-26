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
package org.mapsforge.preprocessing.util;

import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DBConnection {

	private Connection conn;

	public DBConnection(String propertiesFile) throws Exception {
		Properties props = new Properties();
		props.load(new FileInputStream(propertiesFile));

		conn = DriverManager.getConnection("jdbc:postgresql://" + props.getProperty("db.host")
				+ "/" + props.getProperty("db.database"), props.getProperty("db.user"), props
				.getProperty("db.password"));
		conn.setAutoCommit(false);
	}

	public DBConnection(String hostName, String dbName, String username, String password,
			int port) throws SQLException {
		this.conn = getJdbcConnectionPg(hostName, port, dbName, username, password);
		conn.setAutoCommit(false);
	}

	public Connection getConnection() {
		return conn;
	}

	public static Connection getJdbcConnectionPg(String hostName, int port, String dbName,
			String username, String password) throws SQLException {
		String url = "jdbc:postgresql://" + hostName + "/" + dbName;
		return DriverManager.getConnection(url, username, password);
	}

	/* temp stuff */
	public static Connection getBerlinDbConn() throws SQLException {
		return new DBConnection("localhost", "berlin", "postgres", "admin", 5432)
				.getConnection();
	}

	public static Connection getGermanyDbConn() throws SQLException {
		return new DBConnection("localhost", "germany", "postgres", "admin", 5432)
				.getConnection();
	}

}
