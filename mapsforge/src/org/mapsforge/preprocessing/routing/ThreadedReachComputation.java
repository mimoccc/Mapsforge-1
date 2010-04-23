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

import java.io.File;
import java.text.DecimalFormat;

import org.mapsforge.preprocessing.model.impl.TransportConfigurationPreprocessing;

public class ThreadedReachComputation {

	public static int NO_THREADS = 2;

	public static double[][] computeReaches(int noThreads, ArrayBasedRoutingGraph graph)
			throws Exception {
		double[][] reaches = new double[NO_THREADS][];

		int partitionSize = graph.size() / NO_THREADS;
		if (graph.size() % NO_THREADS != 0)
			partitionSize++;

		long start = System.currentTimeMillis();

		ReachComputationThread[] threads = new ReachComputationThread[NO_THREADS];
		for (int i = 0; i < threads.length; i++) {
			reaches[i] = new double[graph.size()];
			// Arrays.fill(reaches[i], 0);
			threads[i] = new ReachComputationThread(graph, i * partitionSize, (i + 1)
					* partitionSize <= graph.size() ? partitionSize : graph.size() - (i + 1)
					* partitionSize, reaches[i]);
			// threads[i] = new ReachComputationThread(graph, i, 1, reaches[i]);
			threads[i].setName("T" + i);
		}
		for (int i = 0; i < threads.length; i++) {
			threads[i].start();
		}

		boolean running = true;
		int progress = 0;
		double estimate = 0;
		DecimalFormat df = new DecimalFormat("##.##%");
		while (running) {
			progress = 0;
			running = false;
			for (int i = 0; i < threads.length; i++) {
				threads[i].join(10000 / NO_THREADS);
				progress += threads[i].progress();

				running |= threads[i].isAlive();
			}

			estimate = (System.currentTimeMillis() - start)
					/ ((double) progress / (double) graph.size());
			System.out.println("progress: "
					+ df.format((double) progress / (double) graph.size())
					+ ", est. running time: " + timeMillisToNiceString(estimate)
					+ ", est. time left: "
					+ timeMillisToNiceString(estimate - System.currentTimeMillis() + start));
		}

		System.out.println("running time for computation: "
				+ (System.currentTimeMillis() - start) + "ms");

		return reaches;
	}

	private static DecimalFormat df2 = new DecimalFormat("######.#");

	private static String timeMillisToNiceString(double timeMillis) {
		if (timeMillis < 1000)
			return df2.format(timeMillis) + "ms";
		if (timeMillis < 1000 * 60)
			return df2.format(timeMillis / 1000) + "s";
		if (timeMillis < 1000 * 60 * 60)
			return df2.format(timeMillis / (1000 * 60)) + "min";
		if (timeMillis < 1000 * 60 * 60 * 24)
			return df2.format(timeMillis / (1000 * 60 * 60)) + "h";
		if (timeMillis < 1000 * 60 * 60 * 24 * 30)
			return df2.format(timeMillis / (1000 * 60 * 60 * 24)) + "d";
		if (timeMillis < 1000 * 60 * 60 * 24 * 30 * 12)
			return df2.format(timeMillis / (1000 * 60 * 60 * 24 * 30)) + "mths";
		return df2.format(timeMillis / (1000 * 60 * 60 * 24 * 30 * 12)) + "yrs";
	}

	private static void usage() {
		System.out.println("Usage: ThreadedReachComputation <properties-file>");
	}

	public static void main(String[] args) throws Exception {

		if (args.length != 1) {
			usage();
			System.exit(0);
		}

		File properties = new File(args[0]);
		if (!properties.exists()) {
			System.out.println("No such file: " + properties.toString() + ".");
			usage();
			System.exit(1);
		}

		ArrayBasedRoutingGraph graph = new ArrayBasedRoutingGraph(properties.toString(),
		// "Preprocessing/Graph/conf/preprocessing.properties",
				TransportConfigurationPreprocessing.STANDARD_CAR__SIMPLE_HEURISTIC);
		// new ArrayBasedRoutingGraph();
		// DataInputStream dis = new DataInputStream(new
		// FileInputStream("Preprocessing/graph.bin"));
		// graph.readFields(dis);
		double[][] reaches = computeReaches(NO_THREADS, graph);

		System.out.println("writing reaches to DB");

		ReachDAO reachDAO = new ReachDAO(properties.toString());
		// "Preprocessing/Graph/conf/preprocessing.properties");
		reachDAO
				.deleteReaches(TransportConfigurationPreprocessing.STANDARD_CAR__SIMPLE_HEURISTIC);
		reachDAO.writeReaches(reaches, graph,
				TransportConfigurationPreprocessing.STANDARD_CAR__SIMPLE_HEURISTIC);
	}

}
