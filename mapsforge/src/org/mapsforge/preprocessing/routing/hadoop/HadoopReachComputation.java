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

import java.net.URI;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.filecache.DistributedCache;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.mapsforge.preprocessing.model.impl.TransportConfigurationPreprocessing;
import org.mapsforge.preprocessing.routing.ArrayBasedRoutingGraph;
import org.mapsforge.preprocessing.routing.IGraph;

public class HadoopReachComputation extends Configured implements Tool {

	@Override
	public int run(String[] args) throws Exception {

		IGraph graph = new ArrayBasedRoutingGraph(args[0],
				TransportConfigurationPreprocessing.STANDARD_CAR__SIMPLE_HEURISTIC);

		Job job = new Job(this.getConf());

		job.setMapperClass(ReachMapper.class);
		job.setReducerClass(ReachReducer.class);
		job.setInputFormatClass(ReachInputFormat.class);
		job.setOutputKeyClass(IntWritable.class);
		job.setOutputValueClass(DoubleWritable.class);
		job.setNumReduceTasks(1);

		ReachInputFormat.setGraphSize(job, graph.size());
		ReachInputFormat.setNumMapTasks(job, Integer.parseInt(args[2]));

		FileOutputFormat.setOutputPath(job, new Path(args[1]));

		job.setJarByClass(ReachMapper.class);
		job.setJobName("HadoopReachComputation");

		FileSystem fs = FileSystem.get(getConf());

		String filename = "GRAPH.bin";// + "#" + "GRAPH.bin";
		// RoutingConfiguration.STANDARD_CAR.transport().getName() + "." +
		// RoutingConfiguration.STANDARD_CAR.heuristic().name() + ".bin";
		Path graphPath = new Path("/tmp", filename);
		FSDataOutputStream out = fs.create(graphPath, true);
		graph.write(out);
		out.close();

		System.out.println(graphPath.toString());
		job.getConfiguration().set(ReachMapper.PARAM_GRAPH_FILE_PATH,
				graphPath.toUri().toString());
		DistributedCache.addCacheFile(new URI("/tmp/GRAPH.bin#GRAPH.bin"), job
				.getConfiguration());

		job.submit();

		return 0;
	}

	public static void main(String[] args) throws Exception {
		int res = ToolRunner.run(new Configuration(), new HadoopReachComputation(), args);
		System.exit(res);
	}

}
