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
package org.mapsforge.preprocessing.routing.test;

import java.util.Arrays;

import org.mapsforge.preprocessing.model.impl.TransportConfigurationPreprocessing;
import org.mapsforge.preprocessing.routing.ArrayBasedRoutingGraph;
import org.mapsforge.preprocessing.routing.Node;
import org.mapsforge.preprocessing.routing.ReachComputationThread;
import org.mapsforge.preprocessing.routing.ReachDAO;
import org.mapsforge.preprocessing.routing.ReachDijkstra;
import org.mapsforge.preprocessing.routing.ThreadedReachComputation;

import junit.framework.TestCase;

public class ReachComputation extends TestCase {

	private ArrayBasedRoutingGraph graph;

	@Override
	protected void setUp() throws Exception {
		graph = new ArrayBasedRoutingGraph();

		graph.adjacentNodesIDs = new int[10][];
		graph.adjacentNodesWeights = new double[10][];
		graph.nodeMapping = new long[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };

		graph.adjacentNodesIDs[0] = new int[] { 1, 2 };
		graph.adjacentNodesWeights[0] = new double[] { 1, 3 };

		graph.adjacentNodesIDs[1] = new int[] { 0, 2, 3, 5 };
		graph.adjacentNodesWeights[1] = new double[] { 1, 1, 2, 4 };

		graph.adjacentNodesIDs[2] = new int[] { 0, 1, 4 };
		graph.adjacentNodesWeights[2] = new double[] { 3, 1, 2 };

		graph.adjacentNodesIDs[3] = new int[] { 1, 5 };
		graph.adjacentNodesWeights[3] = new double[] { 2, 1.5 };

		graph.adjacentNodesIDs[4] = new int[] { 2 };
		graph.adjacentNodesWeights[4] = new double[] { 2 };

		graph.adjacentNodesIDs[5] = new int[] { 6 };
		graph.adjacentNodesWeights[5] = new double[] { 6 };

		graph.adjacentNodesIDs[6] = new int[] { 5, 7, 8 };
		graph.adjacentNodesWeights[6] = new double[] { 6, 4, 5 };

		graph.adjacentNodesIDs[7] = new int[] { 6 };
		graph.adjacentNodesWeights[7] = new double[] { 4 };

		graph.adjacentNodesIDs[8] = new int[] { 6 };
		graph.adjacentNodesWeights[8] = new double[] { 5 };

		graph.adjacentNodesIDs[9] = new int[] {};
		graph.adjacentNodesWeights[9] = new double[] {};

		long[] osmIDs = new long[10];
		osmIDs[0] = 172539;
		osmIDs[1] = 172545;
		osmIDs[2] = 172546;
		osmIDs[3] = 172549;
		osmIDs[4] = 172547;
		osmIDs[5] = 172558;
		osmIDs[6] = 172559;
		osmIDs[7] = 172562;
		osmIDs[8] = 172564;
		osmIDs[9] = 172566;

		graph.nodeMapping = osmIDs;
	}

	public void testSingleSourceReach() {
		double[] reaches = new double[graph.size()];
		Arrays.fill(reaches, 0);

		ReachDijkstra.computeReaches(graph, new Node(0), reaches);

		assertEquals(0d, reaches[0]);
		assertEquals(1d, reaches[1]);
		assertEquals(2d, reaches[2]);
		assertEquals(3d, reaches[3]);
		assertEquals(0d, reaches[4]);
		assertEquals(4.5d, reaches[5]);
		assertEquals(5d, reaches[6]);
		assertEquals(0d, reaches[7]);
		assertEquals(0d, reaches[8]);
		assertEquals(0d, reaches[9]);
	}

	public void testAllPairsThreadedReach() {
		double[] reaches = new double[graph.size()];
		Arrays.fill(reaches, 0);
		ReachComputationThread rtc1 = new ReachComputationThread(graph, 0, 5, reaches);
		ReachComputationThread rtc2 = new ReachComputationThread(graph, 5, 5, reaches);

		rtc1.start();
		rtc2.start();

		try {
			rtc1.join();
			rtc2.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		assertEquals(0d, reaches[0]);
		assertEquals(3d, reaches[1]);
		assertEquals(2d, reaches[2]);
		assertEquals(5d, reaches[3]);
		assertEquals(0d, reaches[4]);
		assertEquals(6.5d, reaches[5]);
		assertEquals(5d, reaches[6]);
		assertEquals(0d, reaches[7]);
		assertEquals(0d, reaches[8]);
		assertEquals(0d, reaches[9]);
	}

	public void testThreadedReachComputation() {
		try {
			double[][] reaches = ThreadedReachComputation.computeReaches(2, graph);
			assertEquals(0d, Math.max(reaches[0][0], reaches[1][0]));
			assertEquals(3d, Math.max(reaches[0][1], reaches[1][1]));
			assertEquals(2d, Math.max(reaches[0][2], reaches[1][2]));
			assertEquals(5d, Math.max(reaches[0][3], reaches[1][3]));
			assertEquals(0d, Math.max(reaches[0][4], reaches[1][4]));
			assertEquals(6.5d, Math.max(reaches[0][5], reaches[1][5]));
			assertEquals(5d, Math.max(reaches[0][6], reaches[1][6]));
			assertEquals(0d, Math.max(reaches[0][7], reaches[1][7]));
			assertEquals(0d, Math.max(reaches[0][8], reaches[1][8]));
			assertEquals(0d, Math.max(reaches[0][9], reaches[1][9]));

			ReachDAO dao = new ReachDAO("Preprocessing/Graph/conf/preprocessing.properties");
			dao
					.deleteReaches(TransportConfigurationPreprocessing.STANDARD_CAR__SIMPLE_HEURISTIC);
			dao.writeReaches(reaches, graph,
					TransportConfigurationPreprocessing.STANDARD_CAR__SIMPLE_HEURISTIC);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

}
