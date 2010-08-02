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

import gnu.trove.map.hash.TObjectIntHashMap;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Iterator;

import org.mapsforge.core.DBConnection;
import org.mapsforge.preprocessing.graph.osm2rg.routingGraph.RgDAO;
import org.mapsforge.preprocessing.graph.osm2rg.routingGraph.RgEdge;
import org.mapsforge.preprocessing.routing.highwayHierarchies.util.Serializer;

public class RgEdgeNames implements Serializable {

	private static final long serialVersionUID = 2122661604323386224L;

	private final String[] names;
	private final int[] namesIndex;

	private RgEdgeNames(String[] names, int[] namesIndex) {
		this.names = names;
		this.namesIndex = namesIndex;
	}

	public String getName(int rgEdgeId) {
		if (rgEdgeId < 0 || rgEdgeId >= namesIndex.length || namesIndex[rgEdgeId] == -1) {
			return "";
		}
		return names[namesIndex[rgEdgeId]];
	}

	public int size() {
		return namesIndex.length;
	}

	public void serialize(OutputStream oStream) throws IOException {
		Serializer.serialize(oStream, this);
	}

	public static RgEdgeNames deserialize(InputStream iStream) throws IOException,
			ClassNotFoundException {
		return Serializer.deserialize(iStream);
	}

	public static RgEdgeNames importFromDb(Connection conn) throws SQLException {
		RgDAO rg = new RgDAO(conn);

		int[] index = new int[rg.getNumEdges()];

		int counter = 0;
		// put all names on a map
		TObjectIntHashMap<String> namesMap = new TObjectIntHashMap<String>();
		for (Iterator<RgEdge> iter = rg.getEdges().iterator(); iter.hasNext();) {
			RgEdge e = iter.next();
			String name = e.getName();
			if (name != null && !name.isEmpty()) {
				if (!namesMap.containsKey(name)) {
					namesMap.put(name, counter++);
				}
				int offset = namesMap.get(name);
				index[e.getId()] = offset;
			} else {
				index[e.getId()] = -1;
			}
		}

		String[] names = new String[counter];
		for (Object s : namesMap.keys()) {
			String s1 = (String) s;
			names[namesMap.get(s)] = s1;
		}
		return new RgEdgeNames(names, index);
	}

	public static void main(String[] args) throws SQLException {
		Connection conn = DBConnection.getJdbcConnectionPg("localhost", 5432, "osm_base",
				"osm", "osm");
		RgEdgeNames edgeNames = importFromDb(conn);
		for (int i = 0; i < edgeNames.size(); i++) {
			System.out.println(edgeNames.getName(i));
		}
	}
}
