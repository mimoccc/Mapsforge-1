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
package org.mapsforge.preprocessing.routing.hhmobile.clustering;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Iterator;

import org.mapsforge.preprocessing.routing.highwayHierarchies.sql.HHDbReader;
import org.mapsforge.preprocessing.routing.highwayHierarchies.sql.HHDbReader.HHEdge;
import org.mapsforge.preprocessing.util.DBConnection;

public class DirectedWeightedStaticArrayGraph {

	private final int[] vFirstOutboundEdge;
	private final int[] eWeight;
	private final int[] eTarget;
	private final int numConnectedVertices;

	private DirectedWeightedStaticArrayGraph(int[] vFirstOutboundEdge, int[] eWeight, int[] eTarget) {
		this.vFirstOutboundEdge = vFirstOutboundEdge;
		this.eWeight = eWeight;
		this.eTarget = eTarget;

		// count connected vertices
		int count = 0;
		for (int i = 0; i < vFirstOutboundEdge.length - 1; i++) {
			if (getOutboundDegree(i) > 0) {
				count++;
			}
		}
		numConnectedVertices = count;
	}

	public static DirectedWeightedStaticArrayGraph buildHHGraph(Connection conn, int lvl) throws SQLException {
		HHDbReader reader = new HHDbReader(conn);
		int numVertices = reader.numVertices();
		int numEdges = reader.numEdges(lvl);

		int[] vFirstOutboundEdge = new int[numVertices + 1];
		int[] eWeight = new int[numEdges];
		int[] eTarget = new int[numEdges];

		// initialize all edge offsets by -1
		for (int i = 0; i < vFirstOutboundEdge.length; i++) {
			vFirstOutboundEdge[i] = -1;
		}

		// fetch edges
		int offset = 0;
		for (Iterator<HHEdge> iter = reader.getEdges(lvl); iter.hasNext();) {
			HHEdge e = iter.next();
			if (vFirstOutboundEdge[e.sourceId] == -1) {
				vFirstOutboundEdge[e.sourceId] = offset;
			}
			eWeight[offset] = e.weight;
			eTarget[offset] = e.targetId;
			offset++;
		}
		// some vertices might have no edges -> we need to initialize the offset nervertheless
		vFirstOutboundEdge[numVertices] = numEdges;
		for (int j = vFirstOutboundEdge.length - 2; j >= 0; j--) {
			if (vFirstOutboundEdge[j] == -1) {
				vFirstOutboundEdge[j] = vFirstOutboundEdge[j + 1];
			}
		}
		return new DirectedWeightedStaticArrayGraph(vFirstOutboundEdge, eWeight, eTarget);
	}

	public int[] getConnectedVertices() {
		int[] connectedVertices = new int[numConnectedVertices];
		int offset = 0;
		for (int i = 0; i < vFirstOutboundEdge.length - 1; i++) {
			if (getOutboundDegree(i) > 0) {
				connectedVertices[offset++] = i;
			}
		}
		return connectedVertices;
	}

	public int numConnectedVertices() {
		return numConnectedVertices;
	}

	public int numVertices() {
		return vFirstOutboundEdge.length - 1;
	}

	public int numEdges() {
		return eTarget.length;
	}

	public Vertex getVertex(int id) {
		if (id >= 0 && id < vFirstOutboundEdge.length - 1) {
			return new Vertex(id);
		}
		return null;
	}

	public Edge[] getOutboundEdges(Vertex v) {
		Edge[] e = new Edge[getOutboundDegree(v.id)];
		int firstEdge = vFirstOutboundEdge[v.id];
		for (int i = 0; i < e.length; i++) {
			e[i] = new Edge(firstEdge + i, v.id);
		}
		return e;
	}

	private int getOutboundDegree(int vertexId) {
		return vFirstOutboundEdge[vertexId + 1] - vFirstOutboundEdge[vertexId];
	}

	public class Vertex {

		private int id;

		public Vertex(int id) {
			this.id = id;
		}

		public int getId() {
			return id;
		}
	}

	public class Edge {

		private int id;
		private int sourceId;

		public Edge(int id, int sourceId) {
			this.id = id;
			this.sourceId = sourceId;
		}

		public int getId() {
			return id;
		}

		public int getSourceId() {
			return sourceId;
		}

		public int getTargetId() {
			return eTarget[id];
		}

		public int getWeight() {
			return eWeight[id];
		}
	}

	public static void main(String[] args) throws SQLException {
		Connection conn = DBConnection.getJdbcConnectionPg("localhost", 5432, "osm_base",
				"osm", "osm");

		DirectedWeightedStaticArrayGraph g = buildHHGraph(conn, 1);
		System.out.println(g.numVertices());
		System.out.println(g.numEdges());
		int count = 0;
		int maxDegree = 0;
		for (int i = 0; i < g.numVertices(); i++) {
			System.out.println(g.getOutboundEdges(g.getVertex(i)).length);
			if (g.getOutboundEdges(g.getVertex(i)).length > 0)
				count++;

			maxDegree = Math.max(maxDegree, g.getOutboundEdges(g.getVertex(i)).length);
		}
		System.out.println("numVertices : " + count);
		System.out.println("maxDegree : " + maxDegree);

	}

}
