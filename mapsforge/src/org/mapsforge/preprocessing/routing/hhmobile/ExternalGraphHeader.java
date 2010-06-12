///*
// * Copyright 2010 mapsforge.org
// *
// * This program is free software: you can redistribute it and/or modify
// * it under the terms of the GNU General Public License as published by
// * the Free Software Foundation, either version 3 of the License, or
// * (at your option) any later version.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU General Public License for more details.
// *
// * You should have received a copy of the GNU General Public License
// * along with this program.  If not, see <http://www.gnu.org/licenses/>.
// */
//package org.mapsforge.preprocessing.routing.hhmobile;
//
//import java.io.FileInputStream;
//import java.io.FileOutputStream;
//import java.io.IOException;
//import java.io.NotSerializableException;
//import java.sql.Connection;
//import java.sql.SQLException;
//
//import org.mapsforge.preprocessing.routing.hhmobile.clustering.ClusteringUtil;
//import org.mapsforge.preprocessing.routing.hhmobile.clustering.IClustering;
//import org.mapsforge.preprocessing.routing.hhmobile.clustering.KCenterClusteringAlgorithm;
//import org.mapsforge.preprocessing.routing.hhmobile.graph.LevelGraph;
//import org.mapsforge.preprocessing.routing.hhmobile.util.BitArrayInputStream;
//import org.mapsforge.preprocessing.routing.hhmobile.util.BitArrayOutputStream;
//import org.mapsforge.preprocessing.util.DBConnection;
//
//public class ExternalGraphHeader {
//
//	public static final int HEADER_LENGTH = 4096;
//	public static final short HEADER_MAGIC = 0x001;
//
//	// global bounds : used for compression in all levels
//	public final int numClusters;
//	public final short numLevels;
//	public final int maxVerticesPerCluster;
//
//	// level bounds : used for cluster header compression
//	public final int[] lvlMaxClusterDegree;
//	public final int[] lvlMaxVertexDegree;
//	public final int[] lvlMinEdgeWeight;
//	public final int[] lvlMaxEdgeWeight;
//	public final int[] lvlMinNeighborhood;
//	public final int[] lvlMaxNeighborhood;
//
//	public ExternalGraphHeader(LevelGraph levelGraph, IClustering[] clustering) {
//		numClusters = ClusteringUtil.getNumClusters(clustering);
//		numLevels = (short) levelGraph.numLevels();
//		maxVerticesPerCluster = ClusteringUtil.getMaxNumVerticesPerCluster(clustering);
//
//		lvlMaxClusterDegree = ClusteringUtil.getMaxClusterDegrees(levelGraph, clustering);
//		lvlMaxVertexDegree = ClusteringUtil.getMaxVertexDegrees(levelGraph, clustering);
//		lvlMinEdgeWeight = ClusteringUtil.getMinEdgeWeights(clustering, levelGraph);
//		lvlMaxEdgeWeight = ClusteringUtil.getMaxEdgeWeights(clustering, levelGraph);
//		lvlMinNeighborhood = ClusteringUtil.getMinNeighborhoods(clustering, levelGraph);
//		lvlMaxNeighborhood = ClusteringUtil.getMaxNeighborhoods(clustering, levelGraph);
//	}
//
//	public ExternalGraphHeader(byte[] buff) throws IOException {
//		try {
//			BitArrayInputStream iStream = new BitArrayInputStream(buff);
//			short headerMagic = iStream.readShort();
//			if (headerMagic != HEADER_MAGIC) {
//				throw new IOException("Verification of header failed (" + headerMagic + " != "
//						+ headerMagic + ".");
//			}
//			numClusters = iStream.readInt();
//			numLevels = iStream.readShort();
//			maxVerticesPerCluster = iStream.readInt();
//
//			lvlMaxClusterDegree = new int[numLevels];
//			lvlMaxVertexDegree = new int[numLevels];
//			lvlMinEdgeWeight = new int[numLevels];
//			lvlMaxEdgeWeight = new int[numLevels];
//			lvlMinNeighborhood = new int[numLevels];
//			lvlMaxNeighborhood = new int[numLevels];
//
//			for (int lvl = 0; lvl < numLevels; lvl++) {
//				lvlMaxClusterDegree[lvl] = iStream.readInt();
//				lvlMaxVertexDegree[lvl] = iStream.readInt();
//				lvlMinEdgeWeight[lvl] = iStream.readInt();
//				lvlMaxEdgeWeight[lvl] = iStream.readInt();
//				lvlMinNeighborhood[lvl] = iStream.readInt();
//				lvlMaxNeighborhood[lvl] = iStream.readInt();
//			}
//		} catch (IOException e) {
//			throw new IOException("Error reading header from array.");
//		}
//	}
//
//	public byte[] getBytes() throws NotSerializableException {
//		try {
//			byte[] buff = new byte[HEADER_LENGTH];
//			BitArrayOutputStream oStream = new BitArrayOutputStream(buff);
//
//			oStream.writeShort(HEADER_MAGIC);
//
//			oStream.writeInt(numClusters);
//			oStream.writeShort(numLevels);
//			oStream.writeInt(maxVerticesPerCluster);
//
//			for (int lvl = 0; lvl < numLevels; lvl++) {
//				oStream.writeInt(lvlMaxClusterDegree[lvl]);
//				oStream.writeInt(lvlMaxVertexDegree[lvl]);
//				oStream.writeInt(lvlMinEdgeWeight[lvl]);
//				oStream.writeInt(lvlMaxEdgeWeight[lvl]);
//				oStream.writeInt(lvlMinNeighborhood[lvl]);
//				oStream.writeInt(lvlMaxNeighborhood[lvl]);
//			}
//			oStream.alignPointer(HEADER_LENGTH);
//
//			return buff;
//		} catch (IOException e) {
//			e.printStackTrace();
//			throw new NotSerializableException(
//					"Could not serialize header, data exceeds header length (" + HEADER_LENGTH
//							+ ").");
//		}
//	}
//
//	@Override
//	public String toString() {
//		StringBuilder sb = new StringBuilder();
//		sb.append("numClusters = " + numClusters + "\n");
//		sb.append("numLevels = " + numLevels + "\n");
//		sb.append("maxVerticesPerCluster = " + maxVerticesPerCluster + "\n\n");
//
//		sb.append("lvlMaxClusterDegree = ");
//		append(lvlMaxClusterDegree, sb);
//		sb.append("\n");
//		sb.append("lvlMaxVertexDegree = ");
//		append(lvlMaxVertexDegree, sb);
//		sb.append("\n");
//		sb.append("lvlMinEdgeWeight = ");
//		append(lvlMinEdgeWeight, sb);
//		sb.append("\n");
//		sb.append("lvlMaxEdgeWeight = ");
//		append(lvlMaxEdgeWeight, sb);
//		sb.append("\n");
//		sb.append("lvlMinNeighborhood = ");
//		append(lvlMinNeighborhood, sb);
//		sb.append("\n");
//		sb.append("lvlMaxNeighborhood = ");
//		append(lvlMaxNeighborhood, sb);
//		sb.append("\n");
//
//		return sb.toString();
//	}
//
//	private void append(int[] arr, StringBuilder sb) {
//		for (int i : arr) {
//			sb.append(i + ", ");
//		}
//	}
//
//	public static void main(String[] args) throws SQLException, NotSerializableException,
//			IOException {
//		Connection conn = DBConnection.getJdbcConnectionPg("localhost", 5432, "berlin",
//				"postgres", "admin");
//		LevelGraph levelGraph = new LevelGraph(conn);
//		IClustering[] clustering = KCenterClusteringAlgorithm.computeClustering(levelGraph
//				.getLevels(), 1000, KCenterClusteringAlgorithm.HEURISTIC_MIN_SIZE);
//		ExternalGraphHeader header = new ExternalGraphHeader(levelGraph, clustering);
//
//		String file = "test.txt";
//
//		FileOutputStream oStream = new FileOutputStream(file);
//		oStream.write(header.getBytes());
//		oStream.flush();
//		oStream.close();
//
//		FileInputStream iStream = new FileInputStream(file);
//		byte[] b = new byte[HEADER_LENGTH];
//		iStream.read(b);
//
//		ExternalGraphHeader header_ = new ExternalGraphHeader(b);
//		System.out.println("HEADER : \n\n" + header);
//		System.out.println("HEADER_ : \n\n" + header_);
//	}
// }
