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
package org.mapsforge.preprocessing.routing.hhmobile;

import java.util.Iterator;

import org.apache.hadoop.util.IndexedSortable;
import org.apache.hadoop.util.QuickSort;
import org.mapsforge.core.GeoCoordinate;
import org.mapsforge.preprocessing.routing.hhmobile.QuadTreeClustering.QuadTreeCluster;

class QuadTreeClusteringAlgorithm {

	public static final String ALGORITHM_NAME = "quad_tree";

	public static final int HEURISTIC_CENTER = 0;
	public static final int HEURISTIC_MEDIAN = 1;
	public static final int HEURISTIC_AVERAGE = 2;

	private static final int HEURISTIC_DEFAULT = HEURISTIC_MEDIAN;
	private static final String[] HEURISTIC_NAMES = new String[] { "center", "median",
			"average" };
	private static final QuickSort quicksort = new QuickSort();

	public static QuadTreeClustering[] computeClustering(Graph[] graph, int[] lon, int[] lat,
			int heuristik, int threshold) throws IllegalArgumentException {
		QuadTreeClustering[] clustering = new QuadTreeClustering[graph.length];
		for (int i = 0; i < graph.length; i++) {
			clustering[i] = computeClustering(graph[i], lon, lat, heuristik, threshold,
					graph[0].numVertices());
		}
		return clustering;
	}

	public static QuadTreeClustering computeClustering(Graph graph, int[] lon, int[] lat,
			int heuristik, int threshold, int numVerticesLvlZero)
			throws IllegalArgumentException {
		System.out.println("computing quad-clustering (|V|=" + graph.numVertices()
				+ ", threshold=" + threshold + ", heuristic=" + HEURISTIC_NAMES[heuristik]
				+ ")");
		int[] vertexId = new int[graph.numVertices()];
		int[] lon_ = new int[graph.numVertices()];
		int[] lat_ = new int[graph.numVertices()];
		int i = 0;
		for (Iterator<? extends Vertex> iter = graph.getVertices(); iter.hasNext();) {
			Vertex v = iter.next();
			vertexId[i] = v.getId();
			lon_[i] = lon[v.getId()];
			lat_[i] = lat[v.getId()];
			i++;
		}

		// subdivide
		QuadTreeClustering clustering = new QuadTreeClustering(numVerticesLvlZero);
		subdivide(vertexId, lon_, lat_, 0, vertexId.length, heuristik, clustering, threshold);

		return clustering;
	}

	private static void subdivide(int vertexId[], int lon[], int lat[], int l, int r,
			int heuristic, QuadTreeClustering clustering, int threshold) {
		if ((r - l) > threshold) {
			// subdivide
			GeoCoordinate splitCoord = getSplitCoordinate(vertexId, lon, lat, l, r, heuristic);
			int splitLon = splitCoord.getLongitudeE6();
			int splitLat = splitCoord.getLatitudeE6();
			SortableVertices s = new SortableVertices(vertexId, lon, lat);

			// 1st quadrant
			int j = l - 1;
			for (int i = j + 1; i < r; i++) {
				if (lat[i] >= splitLat && lon[i] <= splitLon) {
					s.swap(i, ++j);
				}
			}
			int l_ = l;
			int r_ = j + 1;
			subdivide(vertexId, lon, lat, l_, r_, heuristic, clustering, threshold);

			// 2nd quadrant
			for (int i = j + 1; i < r; i++) {
				if (lat[i] >= splitLat && lon[i] > splitLon) {
					s.swap(i, ++j);
				}
			}
			l_ = r_;
			r_ = j + 1;
			subdivide(vertexId, lon, lat, l_, r_, heuristic, clustering, threshold);

			// 3rd quadrant
			for (int i = j + 1; i < r; i++) {
				if (lat[i] < splitLat && lon[i] <= splitLon) {
					s.swap(i, ++j);
				}
			}
			l_ = r_;
			r_ = j + 1;
			subdivide(vertexId, lon, lat, l_, r_, heuristic, clustering, threshold);

			// 4rd quadrant
			for (int i = j + 1; i < r; i++) {
				if (lat[i] < splitLat && lon[i] > splitLon) {
					s.swap(i, ++j);
				}
			}
			l_ = r_;
			r_ = j + 1;
			subdivide(vertexId, lon, lat, l_, r_, heuristic, clustering, threshold);

		} else {
			// recursive anchor - no subdivision - create new cluster if cluster is not empty
			if (r > l) {
				QuadTreeCluster cluster = clustering.addCluster();
				for (int i = l; i < r; i++) {
					cluster.addVertex(vertexId[i]);
				}
			}
		}
	}

	private static GeoCoordinate getSplitCoordinate(int vertexId[], int lon[], int lat[],
			int l, int r, int heuristic) {
		switch (heuristic) {
			case HEURISTIC_CENTER:
				return getCenterCoordinate(lon, lat, l, r);
			case HEURISTIC_MEDIAN:
				return getMedianCoordinate(vertexId, lon, lat, l, r);
			case HEURISTIC_AVERAGE:
				return getAverageCoordinate(lon, lat, l, r);
			default:
				return getSplitCoordinate(vertexId, lon, lat, l, r, HEURISTIC_DEFAULT);
		}
	}

	private static GeoCoordinate getCenterCoordinate(int lon[], int lat[], int l, int r) {
		long minLon = Integer.MAX_VALUE;
		long minLat = Integer.MAX_VALUE;
		long maxLon = Integer.MIN_VALUE;
		long maxLat = Integer.MIN_VALUE;

		for (int i = l; i < r; i++) {
			minLon = Math.min(lon[i], minLon);
			minLat = Math.min(lat[i], minLat);
			maxLon = Math.max(lon[i], maxLon);
			maxLat = Math.max(lat[i], maxLat);
		}

		int longitude = (int) ((minLon + maxLon) / 2);
		int latitude = (int) ((minLat + maxLat) / 2);

		return new GeoCoordinate(latitude, longitude);
	}

	private static GeoCoordinate getAverageCoordinate(int lon[], int lat[], int l, int r) {
		double sumLon = 0d;
		double sumLat = 0d;

		for (int i = l; i < r; i++) {
			sumLon += GeoCoordinate.intToDouble(lon[i]);
			sumLat += GeoCoordinate.intToDouble(lat[i]);
		}

		double longitude = sumLon / (r - l);
		double latitude = sumLat / (r - l);

		return new GeoCoordinate(latitude, longitude);
	}

	private static GeoCoordinate getMedianCoordinate(int[] vertexId, int lon[], int lat[],
			int l, int r) {
		SortableVertices s = new SortableVertices(vertexId, lon, lat);
		int medianIdx = l + ((r - l) / 2);

		s.setSortByLon();
		quicksort.sort(s, l, r);
		int longitude = lon[medianIdx];

		s.setSortByLat();
		quicksort.sort(s, l, r);
		int latitude = lat[medianIdx];

		return new GeoCoordinate(latitude, longitude);
	}

	private static class SortableVertices implements IndexedSortable {

		private final int[][] data;
		private int sortDim;

		public SortableVertices(int[] vertexId, int[] lon, int[] lat) {
			this.data = new int[][] { vertexId, lon, lat };
			sortDim = 0;
		}

		public void setSortByLon() {
			sortDim = 1;
		}

		public void setSortByLat() {
			sortDim = 2;
		}

		@Override
		public int compare(int i, int j) {
			return data[sortDim][i] - data[sortDim][j];
		}

		@Override
		public void swap(int i, int j) {
			for (int d = 0; d < data.length; d++) {
				int tmp = data[d][i];
				data[d][i] = data[d][j];
				data[d][j] = tmp;
			}
		}
	}
}
