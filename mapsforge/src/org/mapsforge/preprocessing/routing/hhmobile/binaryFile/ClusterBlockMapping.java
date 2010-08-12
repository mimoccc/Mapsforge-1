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
package org.mapsforge.preprocessing.routing.hhmobile.binaryFile;

import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.Collection;

import org.mapsforge.preprocessing.routing.hhmobile.clustering.Cluster;
import org.mapsforge.preprocessing.routing.hhmobile.clustering.Clustering;
import org.mapsforge.preprocessing.routing.hhmobile.clustering.ClusteringUtil;
import org.mapsforge.preprocessing.routing.hhmobile.util.Utils;

final class ClusterBlockMapping {

	private final TObjectIntHashMap<Cluster> blockIds;
	private final Cluster[] clusters;

	public ClusterBlockMapping(Clustering[] clustering) {
		this.clusters = new Cluster[ClusteringUtil.getGlobalNumClusters(clustering)];
		this.blockIds = new TObjectIntHashMap<Cluster>();
		int blockId = 0;
		for (int lvl = 0; lvl < clustering.length; lvl++) {
			for (Cluster c : clustering[lvl].getClusters()) {
				clusters[blockId] = c;
				blockIds.put(c, blockId);
				blockId++;
			}
		}
	}

	public void swapBlockIds(int i, int j) {
		Utils.swap(clusters, i, j);
		blockIds.put(clusters[i], i);
		blockIds.put(clusters[j], j);
	}

	public int[] getBlockIds(Collection<Cluster> col) {
		int[] arr = new int[col.size()];
		int i = 0;
		for (Cluster c : col) {
			arr[i++] = getBlockId(c);
		}
		return arr;
	}

	public int getBlockId(Cluster cluster) {
		return blockIds.get(cluster);
	}

	public Cluster getCluster(int blockId) {
		return clusters[blockId];
	}

	public int size() {
		return clusters.length;
	}

}
