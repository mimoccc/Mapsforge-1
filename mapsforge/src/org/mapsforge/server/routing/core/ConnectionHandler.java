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
package org.mapsforge.server.routing.core;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class ConnectionHandler {

	public interface ICallback<T> {
		T callback(Connection con, Object... objects) throws SQLException;
	}

	private final String conPwd;
	private final String conStr;
	private final String conUsr;

	/** package private */
	ConnectionHandler(String conStr, String conUsr, String conPwd) {
		assert conStr != null;
		assert conUsr != null;
		assert conPwd != null;
		this.conStr = conStr;
		this.conUsr = conUsr;
		this.conPwd = conPwd;
	}

	public <T> T handle(ICallback<T> cb, Object... objects)
			throws ComponentInitializationException {
		Connection con = null;
		try {
			con = DriverManager.getConnection(this.conStr, this.conUsr, this.conPwd);
			return cb.callback(con, objects);
		} catch (SQLException exc) {
			throw new ComponentInitializationException(Issue.DB_CON_SETUP_EXC.msg(), exc);
		} finally {
			if (con != null)
				try {
					con.close();
				} catch (Exception exc) {
					throw new ComponentInitializationException(Issue.DB_CON_SHUTDOWN_EXC.msg(),
							exc);
				}
		}
	}
}
