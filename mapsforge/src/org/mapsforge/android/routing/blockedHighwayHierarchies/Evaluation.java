package org.mapsforge.android.routing.blockedHighwayHierarchies;

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

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedList;

import org.mapsforge.core.DBConnection;
import org.mapsforge.core.GeoCoordinate;
import org.mapsforge.preprocessing.routing.highwayHierarchies.HHDbReader;
import org.mapsforge.preprocessing.routing.highwayHierarchies.HHGraphProperties;

class Evaluation {

	private static class Result {
		double numGraphBlockReads;
		double numFileSystemBlockReads;
		double numSettledVertices;
		double numCacheHits;
		double maxHeapSize;

		Result() {
			this.numGraphBlockReads = 0;
			this.numFileSystemBlockReads = 0;
			this.numSettledVertices = 0;
			this.numCacheHits = 0;
			this.maxHeapSize = 0;
		}

		public Result(double numGraphBlockReads, double numFileSystemBlockReads,
				double numSettledVertices, double numCacheHits, double maxHeapSize) {
			this.numGraphBlockReads = numGraphBlockReads;
			this.numFileSystemBlockReads = numFileSystemBlockReads;
			this.numSettledVertices = numSettledVertices;
			this.numCacheHits = numCacheHits;
			this.maxHeapSize = maxHeapSize;
		}

		@Override
		public String toString() {
			return "Result(" + "\n"
					+ "  numGraphBlockReads = " + numGraphBlockReads + "\n"
					+ "  numFileSystemBlockReads = " + numFileSystemBlockReads + "\n"
					+ "  numSettledVertices = " + numSettledVertices + "\n"
					+ "  numCacheHits = " + numCacheHits + "\n"
					+ "  maxHeapSize = " + maxHeapSize + "\n"
					+ ")";
		}

		void addValues(Result other) {
			numGraphBlockReads += other.numGraphBlockReads;
			numFileSystemBlockReads += other.numFileSystemBlockReads;
			numSettledVertices += other.numSettledVertices;
			numCacheHits += other.numCacheHits;
			maxHeapSize += other.maxHeapSize;
		}

		void divValues(double scalar) {
			numGraphBlockReads /= scalar;
			numFileSystemBlockReads /= scalar;
			numSettledVertices /= scalar;
			numCacheHits /= scalar;
			maxHeapSize /= scalar;
		}

		void minValues(Result other) {
			numGraphBlockReads = Math.min(numGraphBlockReads, other.numGraphBlockReads);
			numFileSystemBlockReads = Math.min(numFileSystemBlockReads,
					other.numFileSystemBlockReads);
			numSettledVertices = Math.min(numSettledVertices, other.numSettledVertices);
			numCacheHits = Math.min(numCacheHits, other.numCacheHits);
			maxHeapSize = Math.min(maxHeapSize, other.maxHeapSize);
		}

		void maxValues(Result other) {
			numGraphBlockReads = Math.max(numGraphBlockReads, other.numGraphBlockReads);
			numFileSystemBlockReads = Math.max(numFileSystemBlockReads,
					other.numFileSystemBlockReads);
			numSettledVertices = Math.max(numSettledVertices, other.numSettledVertices);
			numCacheHits = Math.max(numCacheHits, other.numCacheHits);
			maxHeapSize = Math.max(maxHeapSize, other.maxHeapSize);
		}

		Result getCopy() {
			return new Result(numGraphBlockReads, numFileSystemBlockReads, numSettledVertices,
					numCacheHits, maxHeapSize);
		}
	}

	private static class TestRoute {
		final HHVertex source;
		final HHVertex target;

		public TestRoute(HHVertex source, HHVertex target) {
			this.source = source;
			this.target = target;
		}
	}

	public static final int PHASE_A = 0;
	public static final int PHASE_B = 1;

	private static int currentPhase = PHASE_A;
	private static Result[] currentResult = getEmptyResult();

	// database
	private static final String DB_HOST = "localhost";
	private static final int DB_PORT = 5432;
	private static final String DB_USER = "osm";
	private static final String DB_PASS = "osm";

	// general configuration
	private static final int FILE_SYSTEM_BLOCK_SIZE = 4096;
	private static final int CACHE_SIZE = 400 * 1000 * 1024; // 400MB big enough to cache it all

	static void setPhase(int phase) {
		if (phase == PHASE_A || phase == PHASE_B) {
			currentPhase = phase;
		}
	}

	static void notifyBlockRead(long startAddr, long endAddr) {
		// one graph block was read
		currentResult[currentPhase].numGraphBlockReads++;

		// number of file system blocks read
		while (startAddr < endAddr) {
			startAddr = startAddr + FILE_SYSTEM_BLOCK_SIZE
					- (startAddr % FILE_SYSTEM_BLOCK_SIZE);
			currentResult[currentPhase].numFileSystemBlockReads++;
		}
	}

	static void notifyCacheHit() {
		currentResult[currentPhase].numCacheHits++;
	}

	static void notifyVertexSettled() {
		currentResult[currentPhase].numSettledVertices++;
	}

	static void notifyHeapSizeChanged(int currentHeapSize) {
		currentResult[currentPhase].maxHeapSize = Math.max(
				currentResult[currentPhase].maxHeapSize, currentHeapSize);
	}

	static Result[] getEmptyResult() {
		return new Result[] { new Result(), new Result() };
	}

	static Connection getConnection(String dbName) throws SQLException {
		return DBConnection.getJdbcConnectionPg(DB_HOST, DB_PORT, dbName, DB_USER, DB_PASS);
	}

	static HHGraphProperties getHHPropertiesFromDb(Connection conn) throws SQLException {
		HHDbReader reader = new HHDbReader(conn);
		return reader.getGraphProperties();
	}

	static String getDbName(File hhBinaryFile) {
		return hhBinaryFile.getName().substring(0, 6);
	}

	static String getClusteringAlgorithmName(File hhBinaryFile) {
		String s = hhBinaryFile.getName().substring(6);
		if (s.startsWith("quad_tree")) {
			return "quad_tree";
		}
		return "k_center";
	}

	static LinkedList<TestRoute> getTestRoutes(File testRoutesFile, HHRoutingGraph routingGraph)
			throws IOException {
		final int maxDistance = 300;
		LinkedList<TestRoute> testRoutes = new LinkedList<TestRoute>();
		LineNumberReader lnr = new LineNumberReader(new FileReader(testRoutesFile));
		String line;
		while ((line = lnr.readLine()) != null) {
			String[] coords = line.split(";");
			String[] s = coords[0].split(",");
			String[] t = coords[1].split(",");
			HHVertex source = routingGraph.getNearestVertex(new GeoCoordinate(Double
					.parseDouble(s[0]), Double.parseDouble(s[1])), maxDistance);
			HHVertex target = routingGraph.getNearestVertex(new GeoCoordinate(Double
					.parseDouble(t[0]), Double.parseDouble(t[1])), maxDistance);
			testRoutes.add(new TestRoute(source, target));
		}
		return testRoutes;
	}

	static LinkedList<Result[]> evaluateTestRoutes(File hhBinaryFile, File testRoutesFile)
			throws IOException {
		HHRoutingGraph routingGraph = new HHRoutingGraph(hhBinaryFile, CACHE_SIZE);
		HHAlgorithm algo = new HHAlgorithm(routingGraph);
		LinkedList<TestRoute> testRoutes = getTestRoutes(testRoutesFile, routingGraph);
		LinkedList<Result[]> results = new LinkedList<Result[]>();

		int count = 0;
		for (TestRoute testRoute : testRoutes) {
			currentResult = getEmptyResult();
			algo.getShortestPath(testRoute.source.vertexIds[0], testRoute.target.vertexIds[0],
					new LinkedList<HHEdge>(), true);
			results.add(currentResult);

			if (count++ == 10) {
				break;
			}
		}
		return results;
	}

	// static LinkedList<Result[]> evaluateAllFiles(File testRoutesFile)
	// throws IOException, SQLException {
	// int rank = Integer.parseInt(testRoutesFile.getName().split(".")[0]);
	//
	// File outputFile = new File("binaryEvaluation_" + rank + ".txt");
	// PrintWriter writer = new PrintWriter(new FileWriter(outputFile));
	//
	// File[] hhBinaryFiles = new File("evaluation/binaries").listFiles();
	// for (File hhBinaryFile : hhBinaryFiles) {
	// String clusteringAlgorithmName = getClusteringAlgorithmName(hhBinaryFile);
	// HHGraphProperties props = getHHPropertiesFromDb(getConnection(getDbName(hhBinaryFile)));
	// LinkedList<Result[]> results = evaluateTestRoutes(hhBinaryFile, testRoutesFile);
	// Result[] minResult = getMinResult(results);
	// Result[] maxResult = getMaxResult(results);
	// Result[] avgResult = getAverageResult(results);
	// writer
	// .println("c;h;hopLimit;clustering;clusteringAnchor;hasHopIndices;"
	// + "blockReadsPhaseA;4kReadsPhaseA;SettledPhaseA;cacheHitsPhaseA;heapSizePhaseA;"
	// + "blockReadsPhaseB;4kReadsPhaseB;SettledPhaseB;cacheHitsPhaseB;heapSizePhaseB;");
	// writer.print(props.c + ";");
	// writer.print(props.h + ";");
	// writer.print(props.hopLimit + ";");
	// writer.print(clusteringAlgorithmName + ";");
	//
	// }
	//
	// }

	static Result[] getAverageResult(LinkedList<Result[]> results) {
		Result[] avg = getEmptyResult();
		for (Result[] r : results) {
			avg[0].addValues(r[0]);
			avg[1].addValues(r[1]);
		}
		avg[0].divValues(results.size());
		avg[1].divValues(results.size());
		return avg;
	}

	static Result[] getMinResult(LinkedList<Result[]> results) {
		Result[] min = { results.getFirst()[0].getCopy(), results.getFirst()[1].getCopy() };
		for (Result[] r : results) {
			min[0].minValues(r[0]);
			min[1].minValues(r[1]);
		}
		return min;
	}

	static Result[] getMaxResult(LinkedList<Result[]> results) {
		Result[] max = { results.getFirst()[0].getCopy(), results.getFirst()[1].getCopy() };
		for (Result[] r : results) {
			max[0].maxValues(r[0]);
			max[1].maxValues(r[1]);
		}
		return max;
	}

	public static void main(String[] args) throws IOException {
		File hhBinaryFile = new File("evaluation/binaries/ger_02_k_center_400_true.blockedHH");
		File testRoutesFile = new File("evaluation/routes/1048576.txt");
		LinkedList<Result[]> result = evaluateTestRoutes(hhBinaryFile, testRoutesFile);

		for (Result[] r : result) {
			System.out.println(r[0] + "\n" + r[1] + "\n-");
		}
	}
}
