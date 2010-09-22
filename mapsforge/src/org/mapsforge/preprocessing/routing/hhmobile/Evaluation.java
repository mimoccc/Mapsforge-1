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
package org.mapsforge.preprocessing.routing.hhmobile;

import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.mapsforge.core.DBConnection;
import org.mapsforge.preprocessing.routing.highwayHierarchies.util.Serializer;

public class Evaluation {

	private static final String PG_RESTORE_EXE = "C:\\Program Files (x86)\\PostgreSQL\\8.4\\bin\\pg_restore.exe";
	private static String[] CREATE_DB_NAMES = { "osm", "germany", "routing_osm", "osm_routing" };

	private static void importDumps(File dumpDirectory, String dbNamePrefix, Connection conn)
			throws IOException, SQLException, InterruptedException {
		File[] files = dumpDirectory.listFiles(new FileFilter() {

			@Override
			public boolean accept(File pathname) {
				return pathname.isFile();
			}
		});
		Statement stmt = conn.createStatement();

		for (int i = 0; i < files.length; i++) {
			File dump = files[i];
			String targetDbName = dbNamePrefix + i;

			System.out.println("[" + i + "] import the dump");
			String cmd = PG_RESTORE_EXE + " -d test2 -C -v -U osm "
					+ dump.getAbsolutePath();
			Process p = Runtime.getRuntime().exec(cmd);
			readFromStream(p.getInputStream());
			readFromStream(p.getErrorStream());
			p.waitFor();

			for (String dbName : CREATE_DB_NAMES) {
				try {
					System.out.println("[" + i + "] rename database " + dbName
							+ " -> "
							+ targetDbName);
					stmt.executeUpdate("ALTER DATABASE " + dbName + " RENAME TO "
							+ targetDbName
							+ ";");
				} catch (SQLException e) {
					//
				}
				try {
					stmt.executeUpdate("DROP DATABASE " + dbName + ";");
					System.out
							.println("[" + i + "] drop database '" + dbName + "'");
				} catch (SQLException e) {
					//
				}
			}
		}
	}

	private static void readFromStream(final InputStream in) {
		Runnable r = new Runnable() {

			@Override
			public void run() {
				try {
					final LineNumberReader lnr = new LineNumberReader(new InputStreamReader(in));
					String line;
					while ((line = lnr.readLine()) != null) {
						System.out.println(line);
					}
				} catch (IOException e) {
					//
				}
			}
		};
		Thread t = new Thread(r);
		t.start();
	}

	private static void serializeLevelGraph(File dbNames) throws IOException, SQLException {
		LineNumberReader reader = new LineNumberReader(new FileReader(dbNames));
		String line;
		while ((line = reader.readLine()) != null) {
			Connection conn = DBConnection.getJdbcConnectionPg("localhost", 5432, line, "osm",
					"osm");
			LevelGraph graph = new LevelGraph(conn);
			Serializer.serialize(new File(line + ".levelGraph"), graph);
			conn.close();
		}
	}

	public static void main(String[] args) throws IOException, SQLException,
			InterruptedException {
		File dumpDirectory = new File(
				"C:/Users/braindamage/Desktop/diplomarbeit/2010-08-04_evaluation/hh_dumps");
		String dbNamePrefix = "ger_";
		Connection conn = DBConnection.getJdbcConnectionPg("localhost", 5432, "test2", "osm",
				"osm");
		File dbNames = new File("dbNames.txt");
		importDumps(dumpDirectory, dbNamePrefix, conn);
		serializeLevelGraph(dbNames);
		conn.close();
		//
		// try {
		// Runtime rt = Runtime.getRuntime();
		// Process proc = rt.exec("javac");
		// InputStream stderr = proc.getErrorStream();
		// InputStreamReader isr = new InputStreamReader(stderr);
		// BufferedReader br = new BufferedReader(isr);
		// String line = null;
		// System.out.println("<ERROR>");
		// while ((line = br.readLine()) != null)
		// System.out.println(line);
		// System.out.println("</ERROR>");
		// int exitVal = proc.waitFor();
		// System.out.println("Process exitValue: " + exitVal);
		// } catch (Throwable t) {
		// t.printStackTrace();
		// }

	}
}
