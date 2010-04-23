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
package org.mapsforge.preprocessing.routing;

import gnu.trove.TDoubleArrayList;
import gnu.trove.TIntArrayList;
import gnu.trove.TLongIntHashMap;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.HashSet;

import org.mapsforge.preprocessing.model.ITransportConfigurationPreprocessing;
import org.mapsforge.preprocessing.model.impl.TransportConfigurationPreprocessing;
import org.mapsforge.preprocessing.util.DBConnection;

public class ArrayBasedRoutingGraph implements IGraph {

	private static final int FETCH_SIZE = 1000;
	public int[][] adjacentNodesIDs;
	public double[][] adjacentNodesWeights;
	public long[] nodeMapping;

	private ITransportConfigurationPreprocessing transportConfiguration;

	private Connection conn;

	public ArrayBasedRoutingGraph(String propertiesFile,
			ITransportConfigurationPreprocessing conf) throws Exception {
		DBConnection dbConnection = new DBConnection(propertiesFile);
		conn = dbConnection.getConnection();

		this.transportConfiguration = conf;

		readFromDB();
	}

	public ArrayBasedRoutingGraph() {
		super();
	}

	@Override
	protected void finalize() throws Throwable {
		if (conn != null && !conn.isClosed())
			conn.close();
	}

	@Override
	public Iterable<IEdge> getAdjacentEdges(INode node) {
		IEdge[] edges = null;

		if (adjacentNodesIDs[node.id()] != null && adjacentNodesIDs[node.id()].length > 0) {
			edges = new IEdge[adjacentNodesIDs[node.id()].length];
		} else {
			return null;
		}
		for (int i = 0; i < adjacentNodesIDs[node.id()].length; i++) {
			edges[i] = new Edge(node, new Node(adjacentNodesIDs[node.id()][i]),
					adjacentNodesWeights[node.id()][i]);
		}

		return Arrays.asList(edges);
	}

	private void readFromDB() throws Exception {

		int n_nodes = 0;

		String SQL_COUNT_NODES = "SELECT COUNT(DISTINCT u.id) FROM "
				+ "(SELECT source_id AS id FROM adjacency_list WHERE configuration = ? "
				+ "UNION SELECT dest_id AS id FROM adjacency_list WHERE configuration = ?) u";
		String SQL_ADJACENCY_LIST = "SELECT source_id, dest_id, weight FROM adjacency_list WHERE configuration = ?";

		PreparedStatement pstmtCount = conn.prepareStatement(SQL_COUNT_NODES);
		pstmtCount.setString(1, transportConfiguration.name());
		pstmtCount.setString(2, transportConfiguration.name());

		ResultSet rs = pstmtCount.executeQuery();
		if (rs.next()) {
			n_nodes = rs.getInt(1);
		}
		if (n_nodes == 0) {
			throw new Exception("empty list of nodes for this configuration");
		}

		System.out.println(n_nodes);
		rs.close();
		pstmtCount.close();

		this.adjacentNodesIDs = new int[n_nodes][];
		this.adjacentNodesWeights = new double[n_nodes][];
		this.nodeMapping = new long[n_nodes];

		PreparedStatement pstmtAdjacencyList = conn.prepareStatement(SQL_ADJACENCY_LIST);
		pstmtAdjacencyList.setString(1, transportConfiguration.name());

		pstmtAdjacencyList.setFetchSize(FETCH_SIZE);

		rs = pstmtAdjacencyList.executeQuery();
		long firstNodeID = -1;
		long currentID = -1;
		long previousID = -1;
		long currentAdjacent;
		double currentWeight;
		int currentIndex = 0;
		int nodeNo = 0;
		TIntArrayList currentAdjacentNodes = new TIntArrayList();
		TDoubleArrayList currentAdjacentWeights = new TDoubleArrayList();
		int currentMappingAdjacent = 0;
		TLongIntHashMap mapping = new TLongIntHashMap(n_nodes);
		while (rs.next()) {

			currentID = rs.getLong(1);
			currentAdjacent = rs.getLong(2);
			currentWeight = rs.getInt(3);

			// encountered a new node
			// write data for the previous one
			if (currentID != previousID) {
				// initial node
				if (previousID < 0) {
					firstNodeID = currentID;
					currentIndex = nodeNo++;
					mapping.put(currentID, currentIndex);
					this.nodeMapping[currentIndex] = currentID;
				}
				// next node
				else {
					this.adjacentNodesIDs[currentIndex] = currentAdjacentNodes.toNativeArray();
					this.adjacentNodesWeights[currentIndex] = currentAdjacentWeights
							.toNativeArray();

					currentAdjacentNodes.clear();
					currentAdjacentWeights.clear();

					currentIndex = mapping.get(currentID);

					if (currentIndex == 0) {
						currentIndex = nodeNo++;
						mapping.put(currentID, currentIndex);
						this.nodeMapping[currentIndex] = currentID;
					}
				}
			}

			currentMappingAdjacent = mapping.get(currentAdjacent);
			// adjacent node has not been seen so far, we do not know its
			// index position in the array
			if (currentMappingAdjacent == 0 && currentAdjacent != firstNodeID) {
				currentMappingAdjacent = nodeNo++;
				mapping.put(currentAdjacent, currentMappingAdjacent);
				this.nodeMapping[currentMappingAdjacent] = currentAdjacent;
			}

			currentAdjacentNodes.add(currentMappingAdjacent);
			currentAdjacentWeights.add(currentWeight);

			previousID = currentID;
		}

		this.adjacentNodesIDs[currentIndex] = currentAdjacentNodes.toNativeArray();
		this.adjacentNodesWeights[currentIndex] = currentAdjacentWeights.toNativeArray();
		this.nodeMapping[currentIndex] = previousID;

	}

	public int[] getAdjacent(int nodeID) {
		return adjacentNodesIDs[nodeID];
	}

	public double[] getAdjacentWeights(int nodeID) {
		return adjacentNodesWeights[nodeID];
	}

	public long osmID(int internalNodeID) {
		return nodeMapping[internalNodeID];
	}

	public int size() {
		return adjacentNodesIDs.length;
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		int size = in.readInt();
		this.adjacentNodesIDs = new int[size][];
		this.adjacentNodesWeights = new double[size][];
		this.nodeMapping = new long[size];

		for (int i = 0; i < size; i++) {
			nodeMapping[i] = in.readLong();
		}

		int l;
		for (int i = 0; i < size; i++) {
			l = in.readInt();
			if (l == 0)
				continue;
			adjacentNodesIDs[i] = new int[l];
			adjacentNodesWeights[i] = new double[l];
			for (int j = 0; j < l; j++) {
				adjacentNodesIDs[i][j] = in.readInt();
				adjacentNodesWeights[i][j] = in.readDouble();
			}
		}

	}

	@Override
	public void write(DataOutput out) throws IOException {
		out.writeInt(size());
		for (int i = 0; i < size(); i++) {
			out.writeLong(nodeMapping[i]);
		}

		for (int i = 0; i < size(); i++) {
			if (adjacentNodesIDs[i] == null)
				out.writeInt(0);
			else {
				out.writeInt(adjacentNodesIDs[i].length);
				for (int j = 0; j < adjacentNodesIDs[i].length; j++) {
					out.writeInt(adjacentNodesIDs[i][j]);
					out.writeDouble(adjacentNodesWeights[i][j]);
				}
			}
		}

	}

	public static void main(String[] args) throws Exception {
		ArrayBasedRoutingGraph brg = new ArrayBasedRoutingGraph(
				"Preprocessing/Graph/conf/preprocessing.properties",
				TransportConfigurationPreprocessing.STANDARD_CAR__SIMPLE_HEURISTIC);
		//		
		// System.gc();
		// System.out.println("mem: " + (Runtime.getRuntime().totalMemory() -
		// Runtime.getRuntime().freeMemory())/1000000d + " MB");
		//		
		// DataOutputStream dos = new DataOutputStream(new
		// FileOutputStream("Preprocessing/graph.bin"));
		// brg.write(dos);

		// ArrayBasedRoutingGraph brg = new ArrayBasedRoutingGraph();
		// DataInputStream dis = new DataInputStream(new
		// FileInputStream("Preprocessing/graph.bin"));
		// brg.readFields(dis);

		HashSet<Long> osmIds = new HashSet<Long>();

		long[] ids = brg.nodeMapping;
		for (int i = 0; i < ids.length; i++) {
			long l = ids[i];
			if (l == 0)
				System.out.println(i);
			if (osmIds.contains(l)) {
				System.out.println("duplicate: " + l);
			} else
				osmIds.add(l);
		}

	}

}
