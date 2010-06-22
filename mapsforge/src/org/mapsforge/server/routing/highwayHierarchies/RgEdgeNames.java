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

import org.mapsforge.preprocessing.graph.osm2rg.routingGraph.RgDAO;
import org.mapsforge.preprocessing.graph.osm2rg.routingGraph.RgEdge;
import org.mapsforge.preprocessing.routing.highwayHierarchies.util.Serializer;
import org.mapsforge.preprocessing.util.DBConnection;

class RgEdgeNames implements Serializable {

	private static final long serialVersionUID = 2122661604323386224L;

	// names[nameOffset[rgEdgeId]] := <edge name>
	private final byte[] names;
	private final int[] nameOffsets;

	private RgEdgeNames(byte[] names, int[] nameOffsets) {
		this.names = names;
		this.nameOffsets = nameOffsets;
	}

	public String getName(int rgEdgeId) {
		if (rgEdgeId < 0 || rgEdgeId >= nameOffsets.length || nameOffsets[rgEdgeId] == -1) {
			return null;
		}
		int startIdx = nameOffsets[rgEdgeId];
		int length = 0;
		while (names[startIdx + length] != 0x00) {
			length++;
		}
		return new String(names, startIdx, length);
	}

	public int size() {
		return nameOffsets.length;
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

		int[] nameOffsets = new int[rg.getNumEdges()];
		StringBuilder sb = new StringBuilder();

		// put all names on a map
		TObjectIntHashMap<String> names = new TObjectIntHashMap<String>();
		for (Iterator<RgEdge> iter = rg.getEdges().iterator(); iter.hasNext();) {
			RgEdge e = iter.next();
			String name = e.getName();
			if (name != null) {
				if (!names.containsKey(name)) {
					int offset = sb.length();
					sb.append(name);
					sb.append((char) 0x00);
					names.put(name, offset);
				}
				int offset = names.get(name);
				nameOffsets[e.getId()] = offset;
			} else {
				nameOffsets[e.getId()] = -1;
			}
		}
		return new RgEdgeNames(sb.toString().getBytes(), nameOffsets);
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
