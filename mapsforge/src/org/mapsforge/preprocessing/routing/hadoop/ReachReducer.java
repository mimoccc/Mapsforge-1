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

import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.mapreduce.Reducer;

public class ReachReducer extends
		Reducer<IntWritable, DoubleWritable, IntWritable, DoubleWritable> {

	@Override
	protected void reduce(IntWritable key, Iterable<DoubleWritable> values, Context context)
			throws IOException, InterruptedException {

		double max = Double.NEGATIVE_INFINITY;
		for (DoubleWritable reach : values) {
			max = Math.max(max, reach.get());
		}

		context.write(key, new DoubleWritable(max));
	}

}
