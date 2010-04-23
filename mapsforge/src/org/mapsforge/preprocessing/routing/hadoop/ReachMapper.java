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
package org.mapsforge.preprocessing.routing.hadoop;

import java.io.IOException;

import org.apache.hadoop.filecache.DistributedCache;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.mapreduce.Mapper;
import org.mapsforge.preprocessing.routing.ArrayBasedRoutingGraph;
import org.mapsforge.preprocessing.routing.IGraph;
import org.mapsforge.preprocessing.routing.Node;
import org.mapsforge.preprocessing.routing.ReachDijkstra;

public class ReachMapper extends Mapper<IntWritable, IntWritable, IntWritable, DoubleWritable> {

	public static final String PARAM_GRAPH_FILE_PATH = "hadoop.reach.graphfile";

	private IGraph graph;
	private double[] localReaches;

	@Override
	protected void setup(Context context) throws IOException, InterruptedException {
		super.setup(context);

		Path[] cacheFilesP = DistributedCache.getLocalCacheFiles(context.getConfiguration());
		if (cacheFilesP != null) {
			FileSystem fs = FileSystem.getLocal(context.getConfiguration());
			FSDataInputStream in = fs.open(cacheFilesP[0]);
			graph = new ArrayBasedRoutingGraph();
			graph.readFields(in);
			System.out.println("successfully read graph");
			System.out.println(graph.size());
		} else {
			throw new IOException("graph data not found");
		}

		// TODO load graph from HDFS and deserialize
		localReaches = new double[graph.size()];
		localReaches = new double[graph.size()];
	}

	@Override
	protected void map(IntWritable key, IntWritable value, Context context) throws IOException,
			InterruptedException {
		System.out.println("computing reach for node: " + key.get());
		ReachDijkstra.computeReaches(graph, new Node(key.get()), localReaches);
	}

	@Override
	public void run(Context context) throws IOException, InterruptedException {
		setup(context);
		while (context.nextKeyValue()) {
			map(context.getCurrentKey(), context.getCurrentValue(), context);
		}

		for (int i = 0; i < localReaches.length; i++) {
			context.write(new IntWritable(i), new DoubleWritable(localReaches[i]));
		}

		cleanup(context);

	}

}
