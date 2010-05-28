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
package org.mapsforge.preprocessing.routing.hhmobile.clustering;

import org.apache.hadoop.util.IndexedSortable;
import org.apache.hadoop.util.QuickSort;
import org.mapsforge.preprocessing.routing.hhmobile.clustering.QuadTreeClustering.QuadTreeCluster;
import org.mapsforge.preprocessing.util.GeoCoordinate;

public class QuadTreeClusteringAlgorithm {

	public static final int HEURISTIC_CENTER = 0;
	public static final int HEURISTIC_MEDIAN = 1;
	public static final int HEURISTIC_AVERAGE = 2;

	private static final int HEURISTIC_DEFAULT = HEURISTIC_MEDIAN;

	private final QuickSort quicksort;

	public QuadTreeClusteringAlgorithm() {
		this.quicksort = new QuickSort();
	}

	public QuadTreeClustering computeClustering(DirectedWeightedStaticArrayGraph graph,
			int[] lon, int[] lat, int heuristik, int threshold) throws IllegalArgumentException {
		if (lon.length != lat.length || lon.length != graph.numVertices()) {
			throw new IllegalArgumentException(
					"Must pass exactly one coordinate for each vertex");
		}

		// due to reordering we also store and reorder vertexIds to keep the mapping
		int[] vertexId = new int[lon.length];
		for (int i = 0; i < vertexId.length; i++) {
			vertexId[i] = i;
		}

		// subdivide
		QuadTreeClustering clustering = new QuadTreeClustering(graph.numVertices());
		subdivide(vertexId, lon, lat, 0, vertexId.length, heuristik, clustering, threshold);

		// restore initial ordering
		SortableVertices s = new SortableVertices(vertexId, lon, lat);
		s.setSortByVertexId();
		quicksort.sort(s, 0, vertexId.length);

		return clustering;
	}

	private void subdivide(int vertexId[], int lon[], int lat[], int l, int r, int heuristic,
			QuadTreeClustering clustering, int threshold) {
		if ((r - l) > threshold) {
			// subdivide
			GeoCoordinate splitCoord = getSplitCoordinate(vertexId, lon, lat, l, r, heuristic);
			int splitLon = splitCoord.getLongitudeInt();
			int splitLat = splitCoord.getLatitudeInt();
			SortableVertices s = new SortableVertices(vertexId, lon, lat);
			System.out.println("split @ " + splitCoord);

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

	private GeoCoordinate getSplitCoordinate(int vertexId[], int lon[], int lat[], int l,
			int r, int heuristic) {
		switch (heuristic) {
			case HEURISTIC_CENTER:
				return getCenterCoordinate(lon, lat, l, r);
			case HEURISTIC_MEDIAN:
				return getAverageCoordinate(lon, lat, l, r);
			case HEURISTIC_AVERAGE:
				return getMedianCoordinate(vertexId, lon, lat, l, r);
			default:
				return getSplitCoordinate(vertexId, lon, lat, l, r, HEURISTIC_DEFAULT);
		}
	}

	private GeoCoordinate getCenterCoordinate(int lon[], int lat[], int l, int r) {
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

	private GeoCoordinate getAverageCoordinate(int lon[], int lat[], int l, int r) {
		double sumLon = 0d;
		double sumLat = 0d;

		for (int i = l; i < r; i++) {
			sumLon += GeoCoordinate.itod(lon[i]);
			sumLat += GeoCoordinate.itod(lat[i]);
		}

		double longitude = sumLon / (r - l);
		double latitude = sumLat / (r - l);

		return new GeoCoordinate(latitude, longitude);
	}

	private GeoCoordinate getMedianCoordinate(int[] vertexId, int lon[], int lat[], int l, int r) {
		SortableVertices s = new SortableVertices(vertexId, lon, lat);
		int medianIdx = l + ((l - r) / 2);

		s.setSortByLon();
		quicksort.sort(s, l, r);
		int longitude = lon[medianIdx];

		s.setSortByLat();
		quicksort.sort(s, l, r);
		int latitude = lat[medianIdx];

		return new GeoCoordinate(latitude, longitude);
	}

	private class SortableVertices implements IndexedSortable {

		private final int[][] data;
		private int sortDim;

		public SortableVertices(int[] vertexId, int[] lon, int[] lat) {
			this.data = new int[][] { vertexId, lon, lat };
			sortDim = 0;
		}

		public void setSortByVertexId() {
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
