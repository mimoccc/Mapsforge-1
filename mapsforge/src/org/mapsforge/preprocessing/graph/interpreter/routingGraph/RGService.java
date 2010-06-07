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
package org.mapsforge.preprocessing.graph.interpreter.routingGraph;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.LinkedList;

import org.mapsforge.preprocessing.graph.interpreter.util.OsmBaseService;
import org.mapsforge.preprocessing.graph.model.gui.Transport;
import org.mapsforge.preprocessing.graph.model.osmxml.OsmWay_withNodes;
import org.mapsforge.preprocessing.graph.osm2rg.routingGraph.RgEdge;
import org.mapsforge.preprocessing.graph.osm2rg.routingGraph.RgVertex;
import org.mapsforge.preprocessing.model.EHighwayLevel;
import org.mapsforge.preprocessing.util.DBConnection;
import org.mapsforge.preprocessing.util.HighwayLevelExtractor;

public class RGService {

	private OsmBaseService dbService;

	public RGService(Connection conn) {
		dbService = new OsmBaseService(conn);
	}

	/*
	 * diese Klasse wird von dem Graphgenerierer angesporchen und liefert diesem die benötigten
	 * Knoten und Kanten zur Grapherstellung. Das abrufen der Daten aus der Datenbank übernimmt
	 * eine DBService, welcher von dieser Klasse angesprochen wird. Diese Klasse übernimmt das
	 * Filtern des gesamten Wege anhand des Transportmittels.
	 */

	public LinkedList<OsmWay_withNodes> getWaysForTransport(Transport transport) {

		LinkedList<OsmWay_withNodes> allWays = null;

		try {
			allWays = dbService.getAllWaysFromDB();
		} catch (SQLException e) {
			System.err
					.println("Error: Can't get all ways from the database. Extracting would be canceled.");
			e.printStackTrace();
			System.exit(-1);
		}

		LinkedList<OsmWay_withNodes> useableWays = new LinkedList<OsmWay_withNodes>();

		if (allWays == null) {
			System.err.println("Error: Get no ways from the database.");
			System.exit(-1);
		} else {

			OsmWay_withNodes currentWay;
			// Tag currentHighwayTag = new Tag("highway", "");
			EHighwayLevel hwyLvl;
			Iterator<OsmWay_withNodes> it = allWays.iterator();

			while (it.hasNext()) {
				currentWay = it.next();
				hwyLvl = currentWay.getHighwayLevel();
				if (hwyLvl != null) {
					if (transport.getUseableWays().contains(hwyLvl)) {
						useableWays.add(currentWay);
					}
				}

			}
		}

		allWays = null;
		return useableWays;
	}

	public void insertVerticesIntoDB(LinkedList<RgVertex> vertices) {

		try {
			dbService.insertVertices(vertices);
		} catch (SQLException e) {
			System.err
					.println("Error: An error occurred while inserting the vertices. Extracting would be canceled.");
			e.printStackTrace();
			System.out.println("wie offt besuche ich knoten doppelt?"
					+ RGGenerator.getDoubleSeenCount());
			System.exit(-1);

		}
	}

	public void insertEdgesIntoDB(LinkedList<RgEdge> edges) {

		try {
			dbService.insertEdges(edges);
		} catch (SQLException e) {
			System.err
					.println("Error: An error occurred while inserting the edges. Extracting would be canceled.");
			e.printStackTrace();
			e.getNextException().printStackTrace();
			System.exit(-1);

		}

	}

	public static void main(String[] args) {
		System.out.println("Teste getAllWay methode:");

		RGService s = null;
		DBConnection connection;
		try {
			connection = new DBConnection("U:\\berlin.osm\\preprocessing.properties");
			s = new RGService(connection.getConnection());
		} catch (Exception e) {
			System.err.println("Can't create connection to the datbase.");
			System.exit(-1);
		}
		Transport trans = new Transport("Auto", 100);
		EHighwayLevel hwyLvl = HighwayLevelExtractor.getLevel("secondary");
		if (!trans.addHighwayLevelToUsableWays(hwyLvl)) {
			System.out.println("Fehler");
			System.exit(-1);
		}

		if (s != null) {
			LinkedList<OsmWay_withNodes> test = s.getWaysForTransport(trans);
			System.out.println(test.size());
		}

	}

	// public void insertHighwayLevels(HashSet<Tag> useableWays) {
	// try {
	// dbService.insertHighwayLevels(useableWays);
	// } catch (SQLException e) {
	// System.err
	// .println("Error: An error occurred while inserting the edges. Extracting would be canceled.");
	// e.printStackTrace();
	// e.getNextException().printStackTrace();
	// System.exit(-1);
	//
	// }
	//
	// }
}
