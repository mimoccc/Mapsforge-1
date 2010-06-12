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

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.Collection;

public class QuadTreeClustering implements IClustering {

	private static final long serialVersionUID = 1L;

	private final TIntObjectHashMap<QuadTreeCluster> clusters;
	private final int[] clusterIds;

	private int nextClusterId;

	public QuadTreeClustering(int maxVertexId) {
		this.clusters = new TIntObjectHashMap<QuadTreeCluster>();
		this.clusterIds = new int[maxVertexId + 1];
		this.nextClusterId = 0;

		for (int i = 0; i < clusterIds.length; i++) {
			clusterIds[i] = -1;
		}
	}

	public QuadTreeCluster addCluster() {
		int clusterId = nextClusterId++;
		QuadTreeCluster c = new QuadTreeCluster(clusterId);
		clusters.put(clusterId, c);
		return c;
	}

	@Override
	public QuadTreeCluster getCluster(int vertexId) {
		return clusters.get(clusterIds[vertexId]);
	}

	@Override
	public Collection<QuadTreeCluster> getClusters() {
		return clusters.valueCollection();
	}

	@Override
	public int size() {
		return clusters.size();
	}

	public class QuadTreeCluster implements ICluster {

		private static final long serialVersionUID = 1L;

		private TIntArrayList vertices;
		private int clusterId;

		private QuadTreeCluster(int clusterId) {
			this.vertices = new TIntArrayList();
			this.clusterId = clusterId;
		}

		public boolean addVertex(int vertexId) {
			if (clusterIds[vertexId] == -1) {
				vertices.add(vertexId);
				clusterIds[vertexId] = clusterId;
				return true;
			}
			System.out.println("Warning: could not add vertex " + vertexId + " to cluster "
					+ clusterId);
			return false;
		}

		@Override
		public boolean containsVertex(int vertexId) {
			return clusterIds[vertexId] == clusterId;
		}

		@Override
		public int[] getVertices() {
			return vertices.toArray();
		}

		@Override
		public int size() {
			return vertices.size();
		}
	}
}
