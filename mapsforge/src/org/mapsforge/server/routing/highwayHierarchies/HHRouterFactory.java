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
package org.mapsforge.server.routing.highwayHierarchies;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import org.mapsforge.preprocessing.util.DBConnection;

/**
 * @author Frank Viernau
 * 
 *         Gives a Router object. These objects are all based on the same graph, wich can be
 *         adjusted in res/conf/hhRouter.properties. Data is obtained from files or from
 *         database. If no file is available, files are written after importing from db.
 */
public class HHRouterFactory {

	private static final String propertiesFile = "res/conf/hhRouter.properties";
	private static HHCompleteRoutingGraph routingGraph = null;
	private static Properties props = null;

	public static synchronized HHRouter getHHRouterInstance() throws FileNotFoundException,
			IOException {

		// get properties
		if (props == null) {
			props = new Properties();
			props.load(new FileInputStream(propertiesFile));
		}

		// try get from file
		if (routingGraph == null) {
			try {
				System.out.println("load file");
				File inDir = new File(props.getProperty("hh.routing.graph.dir"));
				routingGraph = HHCompleteRoutingGraph.importGraphFromFile(inDir);
				System.out.println("loaded file");

			} catch (ClassNotFoundException e) {
			} catch (IOException e) {
			}
		}

		// try get from db
		if (routingGraph == null) {
			try {
				String host = props.getProperty("input.db.host");
				int port = Integer.parseInt(props.getProperty("input.db.port"));
				String dbName = props.getProperty("input.db.name");
				String user = props.getProperty("input.db.user");
				String pass = props.getProperty("input.db.pass");
				Connection conn = new DBConnection(host, dbName, user, pass, port)
						.getConnection();

				System.out.println("HHRouterFactory : importing routing graph from db...");
				routingGraph = HHCompleteRoutingGraph.importGraphFromDb(conn);
				System.out.println("HHRouterFactory : importing ready!");

				File outDir = new File(props.getProperty("hh.routing.graph.dir"));
				System.out.println("HHRouterFactory : exporting routing graph to file system");
				HHCompleteRoutingGraph.exportGraphToDirectory(outDir, routingGraph);
				System.out.println("HHRouterFactory : exporting ready!");

			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return new HHRouter(routingGraph);
	}

	public static void main(String[] args) throws FileNotFoundException, IOException {
		getHHRouterInstance();
	}
}
