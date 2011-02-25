/*
 * Copyright 2010, 2011 mapsforge.org
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
package org.mapsforge.preprocessing.highwayHierarchies.preprocessing.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * lookup connections to databases.
 */
public class DBConnection {

	/**
	 * Open connection to a database.
	 * 
	 * @param hostName
	 *            host.
	 * @param port
	 *            port.
	 * @param dbName
	 *            name of the database.
	 * @param username
	 *            name of the user.
	 * @param password
	 *            password.
	 * @return null on error, else the connection.
	 * @throws SQLException
	 *             on sql error.
	 */
	public static Connection getJdbcConnectionPg(String hostName, int port, String dbName,
			String username, String password) throws SQLException {
		String url = "jdbc:postgresql://" + hostName + "/" + dbName;
		return DriverManager.getConnection(url, username, password);
	}
}
