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
package org.mapsforge.preprocessing.routing.hhmobile.binaryFile.graph;

import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.Collection;

import org.mapsforge.preprocessing.routing.hhmobile.clustering.ClusteringUtil;
import org.mapsforge.preprocessing.routing.hhmobile.clustering.ICluster;
import org.mapsforge.preprocessing.routing.hhmobile.clustering.IClustering;
import org.mapsforge.preprocessing.routing.hhmobile.util.Utils;

final class ClusterBlockMapping {

	private final TObjectIntHashMap<ICluster> blockIds;
	private final ICluster[] clusters;

	public ClusterBlockMapping(IClustering[] clustering) {
		this.clusters = new ICluster[ClusteringUtil.getGlobalNumClusters(clustering)];
		this.blockIds = new TObjectIntHashMap<ICluster>();
		int blockId = 0;
		for (int lvl = 0; lvl < clustering.length; lvl++) {
			for (ICluster c : clustering[lvl].getClusters()) {
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

	public int[] getBlockIds(Collection<ICluster> col) {
		int[] arr = new int[col.size()];
		int i = 0;
		for (ICluster c : col) {
			arr[i++] = getBlockId(c);
		}
		return arr;
	}

	public int getBlockId(ICluster cluster) {
		return blockIds.get(cluster);
	}

	public ICluster getCluster(int blockId) {
		return clusters[blockId];
	}

	public int size() {
		return clusters.length;
	}

}
