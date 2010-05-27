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

import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.Collection;

public class Clustering {

	private final TIntObjectHashMap<Cluster> clusters;
	private final int[] clusterIds;

	private int nextClusterId;

	public Clustering(int maxVertexId) {
		this.clusters = new TIntObjectHashMap<Cluster>();
		this.clusterIds = new int[maxVertexId + 1];
		this.nextClusterId = 0;

		for (int i = 0; i < clusterIds.length; i++) {
			clusterIds[i] = -1;
		}
	}

	public Cluster addCluster(int centerVertex) {
		int clusterId = nextClusterId++;
		Cluster c = new Cluster(centerVertex, clusterId);
		clusters.put(clusterId, c);
		return c;
	}

	public Cluster getCluster(int vertexId) {
		return clusters.get(clusterIds[vertexId]);
	}

	public Collection<Cluster> getClusters() {
		return clusters.valueCollection();
	}

	public void removeCluster(Cluster c) {
		for (TIntIterator iter = c.vertices.iterator(); iter.hasNext();) {
			int v = iter.next();
			clusterIds[v] = -1;
		}
		clusters.remove(c.clusterId);

		c.vertices.clear();
		c.centerVertex = 0;
		c.radius = 0;
	}

	public int size() {
		return clusters.size();
	}

	public class Cluster {

		private TIntArrayList vertices;
		private int centerVertex;
		private int radius;
		private int clusterId;

		private Cluster(int centerVertex, int id) {
			this.vertices = new TIntArrayList();
			this.centerVertex = centerVertex;
			this.clusterId = id;

			addVertex(centerVertex, 0);
		}

		public boolean addVertex(int vertexId, int newRadius) {
			if (newRadius >= radius && clusterIds[vertexId] == -1) {
				vertices.add(vertexId);
				clusterIds[vertexId] = clusterId;
				return true;
			}
			System.out.println("Warning: could not add vertex " + vertexId + " to cluster "
					+ clusterId);
			return false;
		}

		public boolean containsVertex(int vertexId) {
			return clusterIds[vertexId] == clusterId;
		}

		public int getCenterVertex() {
			return centerVertex;
		}

		public int getRadius() {
			return radius;
		}

		public int[] getVertices() {
			return vertices.toArray();
		}

		public int size() {
			return vertices.size();
		}
	}
}
