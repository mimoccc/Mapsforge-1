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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapreduce.InputFormat;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.JobContext;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;

public class ReachInputFormat extends InputFormat<IntWritable, IntWritable> {

	public static final String PARAM_GRAPH_SIZE = "hadoop.reach.graphsize";
	public static final String PARAM_NUM_MAPS = "hadoop.reach.maps";

	@Override
	public RecordReader<IntWritable, IntWritable> createRecordReader(InputSplit split,
			TaskAttemptContext context) throws IOException, InterruptedException {

		return new ReachRecordReader();
	}

	@Override
	public List<InputSplit> getSplits(JobContext context) throws IOException,
			InterruptedException {

		int graphSize = context.getConfiguration().getInt(PARAM_GRAPH_SIZE, 0);
		int numMapTasks = context.getConfiguration().getInt(PARAM_NUM_MAPS, 1);

		if (graphSize <= 0)
			throw new IOException("invalid graph size: " + graphSize);
		if (numMapTasks < 1)
			throw new IOException("invalid number of map tasks: " + numMapTasks);

		int partitionSize = graphSize / numMapTasks;
		if (graphSize % numMapTasks != 0)
			partitionSize++;

		List<InputSplit> splits = new ArrayList<InputSplit>(numMapTasks);
		for (int i = 0; i < numMapTasks; i++) {
			splits.add(new ReachInputSplit(i * partitionSize,
					(i + 1) * partitionSize < graphSize ? partitionSize : graphSize - i
							* partitionSize));
		}
		return splits;
	}

	public static void setGraphSize(Job job, int size) {
		job.getConfiguration().setInt(PARAM_GRAPH_SIZE, size);
	}

	public static void setNumMapTasks(Job job, int numMapTasks) {
		job.getConfiguration().setInt(PARAM_NUM_MAPS, numMapTasks);
	}

	public static class ReachRecordReader extends RecordReader<IntWritable, IntWritable> {

		private int offset;
		private int length;
		private int counter;

		private IntWritable key;
		private IntWritable value = new IntWritable(1);

		@Override
		public void initialize(InputSplit split, TaskAttemptContext context)
				throws IOException, InterruptedException {
			ReachInputSplit rSplit = (ReachInputSplit) split;
			this.offset = rSplit.getOffset();
			this.length = (int) rSplit.getLength();
			counter = 0;
		}

		@Override
		public void close() throws IOException {
			// nothing to do

		}

		@Override
		public IntWritable getCurrentKey() throws IOException, InterruptedException {
			return key;
		}

		@Override
		public IntWritable getCurrentValue() throws IOException, InterruptedException {
			return value;
		}

		@Override
		public float getProgress() throws IOException, InterruptedException {
			return counter / (float) (length);
		}

		@Override
		public boolean nextKeyValue() throws IOException, InterruptedException {
			key = new IntWritable(offset + counter);
			counter++;
			return counter <= length;
		}

	}

	public static class ReachInputSplit extends InputSplit implements Writable {

		private int offset;
		private int length;

		public ReachInputSplit(int offset, int length) {
			super();
			this.offset = offset;
			this.length = length;
		}

		public ReachInputSplit() {
			super();
		}

		@Override
		public long getLength() throws IOException, InterruptedException {
			return length;
		}

		@Override
		public String[] getLocations() throws IOException, InterruptedException {
			return new String[] {};
		}

		public int getOffset() {
			return offset;
		}

		@Override
		public void readFields(DataInput in) throws IOException {
			this.offset = in.readInt();
			this.length = in.readInt();
		}

		@Override
		public void write(DataOutput out) throws IOException {
			out.writeInt(offset);
			out.writeInt(length);

		}

	}
}
