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
package org.mapsforge.preprocessing.graph.interpreter;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;

import org.mapsforge.preprocessing.graph.interpreter.routingGraph.RGGenerator;
import org.mapsforge.preprocessing.graph.interpreter.util.FileLoader;
import org.mapsforge.preprocessing.graph.model.gui.Profil;
import org.mapsforge.preprocessing.util.DBConnection;

public class SimpleRoutingInterpreter implements IInterpreter {

	// interpretierung starten
	public void startPreprocessing(File xmlConfigFile) {

		// first we must parse the xml profile file
		SimpleRoutingConfigurationParser parser = new SimpleRoutingConfigurationParser(
				xmlConfigFile);
		Profil profil = parser.getProfil();

		// now we create a database connection because that would needed later
		Connection conn = null;
		try {
			conn = new DBConnection(profil.getDbProberties()).getConnection();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out
					.println("Can't connect to the Database. Maybe the Proberties are wrong.");
			System.exit(-1);
		}

		// TODO get the right osm file to parse it
		File osm = getOsmFile(profil.getUrl());
		//
		// try {
		// // parse osm file an insert into db
		// new SimpleOSM2DBParser(conn, osm).parseFile();
		// } catch (SAXException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// } catch (IOException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// } catch (SQLException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }

		// create new graph generator which generate the routing graph
		RGGenerator generator = new RGGenerator(conn);
		generator.generate(profil.getTransport());
	}

	private File getOsmFile(String url) {
		// TODO Auto-generated method stub
		File osmFile = new FileLoader().getOsmFile(url);
		return osmFile;
	}

	private static void usage() {
		System.out.println("Usage: SimpleRoutingInterpreter <profile file>");
	}

	public static void main(String[] args) {
		// eingabe prüfen
		if (args.length != 1) {
			usage();
		}
		// TODO file überprüfen
		File xmlConfigFile = new File("U:\\berlin.osm\\testprofil.profil");
		/*
		 * if (!xmlConfigFile.isFile() || !xmlConfigFile.getName().endsWith(".xml")) {
		 * System.out.println("Path is no xml file."); usage(); System.exit(1); }
		 */

		// object erzeugen und file übergeben
		SimpleRoutingInterpreter sri = new SimpleRoutingInterpreter();
		sri.startPreprocessing(xmlConfigFile);
	}

}
