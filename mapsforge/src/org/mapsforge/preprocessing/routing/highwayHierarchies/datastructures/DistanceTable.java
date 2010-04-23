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
package org.mapsforge.preprocessing.routing.highwayHierarchies.datastructures;

import gnu.trove.map.hash.TIntIntHashMap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import org.mapsforge.preprocessing.routing.highwayHierarchies.sql.HHDbReader;

/**
 * Maps vertex id|s to table rows / cols and hold distances in an array.
 * 
 * @author Frank Viernau
 */
public class DistanceTable implements Serializable {

	private static final long serialVersionUID = 7621456445875680368L;

	private final TIntIntHashMap map;
	private int[][] distances;

	public DistanceTable(List<Integer> vertexIds) {
		this.map = new TIntIntHashMap();
		int idx = 0;
		for (int id : vertexIds) {
			map.put(id, idx++);
		}
		distances = new int[map.size()][map.size()];
		for (int i = 0; i < distances.length; i++) {
			for (int j = 0; j < distances[i].length; j++) {
				distances[i][j] = Integer.MAX_VALUE;
			}
		}
	}

	public void serialize(File f) throws IOException {
		FileOutputStream fos = new FileOutputStream(f);
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		oos.writeObject(this);
		oos.close();
		fos.close();
	}

	public static DistanceTable getFromSerialization(File f) throws IOException,
			ClassNotFoundException {
		FileInputStream fis = new FileInputStream(f);
		ObjectInputStream ois = new ObjectInputStream(fis);
		DistanceTable dt = (DistanceTable) ois.readObject();
		ois.close();
		fis.close();
		return dt;
	}

	public static DistanceTable getFromHHDb(Connection conn) throws SQLException {
		HHDbReader reader = new HHDbReader(conn);
		return reader.getDistanceTable();
	}

	public DistanceTable(int[][] distances, TIntIntHashMap mapping) {
		this.distances = distances;
		this.map = mapping;
	}

	public int[] getVertexIds() {
		return map.keys();
	}

	public int getRowColIndex(int vertexId) {
		if (map.contains(vertexId)) {
			return map.get(vertexId);
		}
		return -1;

	}

	public void set(int vId1, int vId2, int distance) {
		distances[map.get(vId1)][map.get(vId2)] = distance;
	}

	public int get(int vId1, int vId2) {
		return distances[map.get(vId1)][map.get(vId2)];
	}

	public int[][] getDistances() {
		return distances;
	}

	public int size() {
		return distances.length;
	}

	@Override
	public String toString() {
		return distances.length + "x" + distances.length;
	}
}
