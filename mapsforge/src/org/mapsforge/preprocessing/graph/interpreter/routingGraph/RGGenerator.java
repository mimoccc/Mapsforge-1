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

import gnu.trove.map.hash.TLongIntHashMap;
import gnu.trove.set.hash.TLongHashSet;

import java.sql.Connection;
import java.util.Iterator;
import java.util.LinkedList;

import org.mapsforge.preprocessing.graph.model.gui.Transport;
import org.mapsforge.preprocessing.graph.model.osmxml.OsmNode;
import org.mapsforge.preprocessing.graph.model.osmxml.OsmWay_withNodes;
import org.mapsforge.preprocessing.graph.osm2rg.routingGraph.RgEdge;
import org.mapsforge.preprocessing.graph.osm2rg.routingGraph.RgVertex;

public class RGGenerator {

	private RGService service;
	private static int doubleSeenCount;

	public RGGenerator(Connection conn) {

		this.service = new RGService(conn);
		setDoubleSeenCount(0);
	}

	public void generate(Transport transport) {

		LinkedList<OsmWay_withNodes> ways = service.getWaysForTransport(transport);

		/*
		 * untersuche jeden wegknoten von allen wegen. die wegknoten die start oder endknoten
		 * eines wegese sind, sowie all die knoten die ein wegknoten von mehr als einem weg sind
		 * werden zu knoten im graphen und erhalten einen graphknotenid
		 */
		Iterator<OsmWay_withNodes> it = ways.iterator();
		OsmWay_withNodes way;
		LinkedList<OsmNode> way_nodes;
		int vertex_id = 0;
		TLongIntHashMap graph_nodes = new TLongIntHashMap(); // enthält die id aller knoten die
		// bereits knoten im graphen sind
		TLongHashSet seen_nodes = new TLongHashSet(); // enthält alle wegknoten die
		// bereits besucht wurden
		LinkedList<RgVertex> vertices = new LinkedList<RgVertex>(); // liste aller graphknoten
		long way_node_id;

		System.out.println("suche und erstelle graphknoten");
		System.out.println("ways" + ways.size());
		while (it.hasNext()) {

			way = it.next();
			way_nodes = way.getNodes();

			System.out.println("way " + way.getId() + " has " + way_nodes.size()
					+ " way nods. The first one is " + way_nodes.getFirst().getId()
					+ " and the last one is " + way_nodes.getLast().getId());
			for (OsmNode way_node : way_nodes) {

				way_node_id = way_node.getId();
				// System.out.print(way_node_id + ";");

				// prüfe ob der knoten schon einmal besucht wurde
				if (seen_nodes.contains(way_node_id)) {
					/*
					 * knoten wurde schon besucht, ist aber noch kein knoten im graphen, das
					 * wird jetzt geändert
					 */
					if (!(graph_nodes.contains(way_node_id))) {
						graph_nodes.put(way_node_id, vertex_id);
						vertices.add(new RgVertex(vertex_id, way_node.getLongitude(), way_node
								.getLatitude(), way_node_id));
						vertex_id++;
					} else {
						doubleSeenCount++;
					}
				} else if (way_node_id == way_nodes.getFirst().getId()
						|| way_node_id == way_nodes.getLast().getId()) {
					/*
					 * es handelt sich um einen knoten der noch nicht besucht wurde, jedoch ist
					 * es der erste oder letzte knoten des weges und muss daher zum knoten im
					 * graphen gemacht werden
					 */
					graph_nodes.put(way_node_id, vertex_id);
					vertices.add(new RgVertex(vertex_id, way_node.getLongitude(), way_node
							.getLatitude(), way_node_id));
					vertex_id++;
					seen_nodes.add(way_node_id);
				} else {
					seen_nodes.add(way_node_id);
				}
				// egal ob neuer graphknoten oder nicht, der knoten wurde definitv besucht

			}
		}

		System.out.println("insert vertices");
		service.insertVerticesIntoDB(vertices);

		vertices = null;
		way_nodes = null;
		seen_nodes = null;
		/*
		 * dann müssen kanten zwischen den kreuzungen erzeugt werden, dabei muss beachtet
		 * werden, dass es kreuzungen gibt wo es mehrer kanten gibt, die die selben beiden
		 * punkte miteinander verbinden.
		 * 
		 * für kanten spielt auch eine entfernung eine rolle. diese muss berechnte werden
		 * 
		 * noch einmal jeden weg abgehen. für jeden wegknoten die länge zum vorgängerknoten
		 * berechnen. handelt sich bei dem aktuellem wegnoten um eine kreuzung so muss eine
		 * kante zischen der letzten bekannten kreuzung und dieser hinzugefügt werden. die
		 * aktuelle kreuzung ist nun die letzt bekannte
		 */

		double test[] = {};
		it = ways.iterator();
		float edge_length;
		int edge_id = 0;
		OsmNode previous_junction_node, previous_node = null;
		long current_node_id;
		// long previous_junction_node_id;
		OsmWay_withNodes current_way = null;
		LinkedList<RgEdge> edges = new LinkedList<RgEdge>();
		// TLongHashSet end_nodes_set;
		// TLongObjectHashMap<TLongHashSet> existing_edges = new
		// TLongObjectHashMap<TLongHashSet>();

		System.out.println("erstelle kanten");
		while (it.hasNext()) {

			current_way = it.next();
			previous_node = previous_junction_node = null;
			edge_length = 0;
			for (OsmNode current_node : current_way.getNodes()) {

				// lesen ersten oder letzten wegknoten, dieser ist immer eine knoten im graph
				if (previous_node == null || previous_junction_node == null) {
					// absicherung
					previous_junction_node = current_node;

				} else {
					// current_node hat einen vorgänger
					// previous_junction_node_id = previous_junction_node.getId();
					current_node_id = current_node.getId();
					// berechne die länge vom vorgänger zur current_node
					edge_length += (float) previous_node.distance(current_node);
					// current_node ist ein knoten im graphen
					if (graph_nodes.contains(current_node_id)) {
						// prüfen ob schon eine kante von diesem Knoten ausgeht
						// if (existing_edges.contains(previous_junction_node_id)) {
						// von diesem knoten gehen schon kanten aus, prüfen nun ob es auch
						// schon eine kante zu dem aktuellen ziel gibt
						// if (existing_edges.get(previous_junction_node_id).contains(
						// current_node_id)) {
						/*
						 * es gibt schon eine kante zwischen previous_junction_node und
						 * current_node TODO wir fügen nun einen zwischenknoten ein, damit der
						 * graph keine mehrfachkanten enthält
						 */
						// }
						/*
						 * da es noch keine kante von der previous_junction_node zur
						 * current_node gibt, können wir diese nun anlegen. wir speichern nun
						 * den knoten als end_node in der list und legen weiter unten die
						 * passende kante dazu an.
						 */
						// existing_edges.get(previous_junction_node_id).add(current_node_id);

						// } else {
						// es gibt noch keine kante von diesem knoten aus
						// end_nodes_set = new TLongHashSet();
						// end_nodes_set.add(current_node_id);
						// existing_edges.put(previous_junction_node_id, end_nodes_set);
						// }
						// kante erzeugen

						// TODO konstruktor anpassen, die wegknoten müssen unterwegs mit
						// eingefügt werden
						edges.add(new RgEdge(edge_id++, graph_nodes.get(previous_junction_node
								.getId()), graph_nodes.get(current_node.getId()), test, test,
								current_way, edge_length));
						// knoten ist nun letzter kreuzungspunkt auf diesem weg
						previous_junction_node = current_node;
					}

					// nur ein einfacher wegknoten ohne bedeutung für den graphen

				}
				// knoten ist abgearbeitet
				previous_node = current_node;
			}
		}

		/*
		 * nun müssen die graphnotes und die edges noch in der datenbank gespeichert werden
		 */

		// service.insertHighwayLevels(transport.getUseableWays());
		System.out.println("insert edges");
		service.insertEdgesIntoDB(edges);
	}

	public void setDoubleSeenCount(int doubleSeenCount) {
		RGGenerator.doubleSeenCount = doubleSeenCount;
	}

	public static int getDoubleSeenCount() {
		return doubleSeenCount;
	}
}
