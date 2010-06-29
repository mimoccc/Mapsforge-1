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
package org.mapsforge.preprocessing.routing.hhmobile.graph;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Iterator;

import org.mapsforge.preprocessing.routing.highwayHierarchies.HHDbReader;
import org.mapsforge.preprocessing.routing.highwayHierarchies.HHDbReader.HHEdgeLvl;
import org.mapsforge.preprocessing.routing.highwayHierarchies.HHDbReader.HHVertex;
import org.mapsforge.preprocessing.routing.highwayHierarchies.HHDbReader.HHVertexLvl;
import org.mapsforge.preprocessing.routing.highwayHierarchies.util.arrays.BitArray;
import org.mapsforge.preprocessing.util.GeoCoordinate;

public class LevelGraph implements Serializable {

	private static final long serialVersionUID = 1L;

	private static final int FWD = 0;
	private static final int BWD = 1;

	private final int[] vFirstLvlVertex, vLvlVNh, vLvlFirstEdge, vLon, vLat, eSource, eTarget,
			eWeight;
	private final BitArray[] eDirection;
	private final BitArray eIsShortcut;
	private final int numLevels, numVertices, numLvlVertices, numEdges;

	private final Level[] levels;

	public LevelGraph(Connection conn) throws SQLException {
		HHDbReader reader = new HHDbReader(conn);

		// initialize counts
		this.numLevels = reader.numLevels();
		this.numVertices = reader.numVertices();
		this.numLvlVertices = reader.numLevelVertices();
		int sum = 0;
		for (int i = 0; i < numLevels; i++) {
			sum += reader.numEdges(i);
		}
		this.numEdges = sum;

		// initialize arrays
		vFirstLvlVertex = new int[numVertices + 1];
		vFirstLvlVertex[numVertices] = numLvlVertices;
		vLon = new int[numVertices];
		vLat = new int[numVertices];
		vLvlVNh = new int[numLvlVertices];
		vLvlFirstEdge = new int[numLvlVertices + 1];
		vLvlFirstEdge[numLvlVertices] = numEdges;
		eSource = new int[numEdges];
		eTarget = new int[numEdges];
		eWeight = new int[numEdges];
		eDirection = new BitArray[] { new BitArray(numEdges), new BitArray(numEdges) };
		this.eIsShortcut = new BitArray(numEdges);
		levels = new Level[numLevels];

		// copy data to arrays

		// vLon + vLat
		int offset = 0;
		for (Iterator<HHVertex> iter = reader.getVertices(); iter.hasNext();) {
			HHVertex v = iter.next();
			vLon[offset] = GeoCoordinate.dtoi(v.longitude);
			vLat[offset] = GeoCoordinate.dtoi(v.latitude);
			offset++;
		}

		// vLvlVNh + vFirstLvlVertex
		offset = 0;
		for (Iterator<HHVertexLvl> iter = reader.getVertexLvls(); iter.hasNext();) {
			HHVertexLvl v = iter.next();
			vLvlVNh[offset] = v.neighborhood;
			if (v.lvl == 0) {
				vFirstLvlVertex[v.id] = offset;
			}
			offset++;
		}
		// vLvlFirstEdge + eSource + eTarget + eWeight + eDirection
		for (int i = 0; i < numLvlVertices; i++) {
			vLvlFirstEdge[i] = -1;
		}

		offset = 0;
		for (Iterator<HHEdgeLvl> iter = reader.getEdgesLvl(); iter.hasNext();) {
			HHEdgeLvl e = iter.next();
			eSource[offset] = e.sourceId;
			eTarget[offset] = e.targetId;
			eWeight[offset] = e.weight;
			eDirection[FWD].set(offset, e.fwd);
			eDirection[BWD].set(offset, e.bwd);
			eIsShortcut.set(offset, e.minLvl > 0);
			if (vLvlFirstEdge[vFirstLvlVertex[e.sourceId] + e.lvl] == -1) {
				vLvlFirstEdge[vFirstLvlVertex[e.sourceId] + e.lvl] = offset;
			}
			offset++;
		}

		// initialize Levels
		for (int i = 0; i < levels.length; i++) {
			levels[i] = new Level(i, reader.getGraphProperties().levelStats[i].numVertices,
					reader.getGraphProperties().levelStats[i].numEdges);
		}
	}

	public Level getLevel(int lvl) {
		return levels[lvl];
	}

	public Level[] getLevels() {
		return levels;
	}

	public int numLevels() {
		return numLevels;
	}

	private int getVertexLvl(int id) {
		return (vFirstLvlVertex[id + 1] - vFirstLvlVertex[id]) - 1;
	}

	public int[] getVertexLongitudes() {
		return vLon;
	}

	public int[] getVertexLatitudes() {
		return vLat;
	}

	public class Level implements IGraph, Serializable {

		private static final long serialVersionUID = 1L;

		public final int lvl;
		private final int lvlNumVertices, lvlNumEdges;

		private Level(int lvl, int lvlNumVertices, int lvlNumEdges) {
			this.lvl = lvl;
			this.lvlNumVertices = lvlNumVertices;
			this.lvlNumEdges = lvlNumEdges;
		}

		@Override
		public LevelVertex getVertex(int id) {
			if (getVertexLvl(id) >= lvl) {
				return new LevelVertex(id);
			}
			return null;
		}

		@Override
		public Iterator<LevelVertex> getVertices() {
			return new Iterator<LevelVertex>() {

				private int nextVertex = getNextVertex(0);

				@Override
				public boolean hasNext() {
					return nextVertex < numVertices;
				}

				@Override
				public LevelVertex next() {
					if (nextVertex < numVertices) {
						LevelVertex v = new LevelVertex(nextVertex);
						nextVertex = getNextVertex(nextVertex + 1);
						return v;
					}
					return null;
				}

				@Override
				public void remove() {

				}

				private int getNextVertex(int startId) {
					while (startId < numVertices) {
						if (getVertexLvl(startId) >= lvl) {
							break;
						}
						startId++;
					}
					return startId;
				}
			};
		}

		@Override
		public int numEdges() {
			return lvlNumEdges;
		}

		@Override
		public int numVertices() {
			return lvlNumVertices;
		}

		public class LevelVertex implements IVertex, Serializable {

			private static final long serialVersionUID = 1L;

			private final int id;

			private LevelVertex(int id) {
				if (getVertexLvl(id) < lvl) {
					System.out.println("dasdsadasgdasdasgjk");
				}
				this.id = id;
			}

			@Override
			public int getId() {
				return id;
			}

			public int getNeighborhood() {
				return vLvlVNh[vFirstLvlVertex[id] + lvl];
			}

			@Override
			public LevelEdge[] getOutboundEdges() {
				int start = vLvlFirstEdge[vFirstLvlVertex[id] + lvl];
				int end = vLvlFirstEdge[vFirstLvlVertex[id] + lvl + 1];

				LevelEdge[] edges = new LevelEdge[end - start];
				for (int i = start; i < end; i++) {
					edges[i - start] = new LevelEdge(i);
				}
				return edges;
			}

			public GeoCoordinate getCoordinate() {
				return new GeoCoordinate(vLat[id], vLon[id]);
			}

			public int getLevel() {
				return lvl;
			}

			public int getMaxLevel() {
				return vFirstLvlVertex[id + 1] - vFirstLvlVertex[id] - 1;
			}
		}

		public class LevelEdge implements IEdge, Serializable {

			private static final long serialVersionUID = 1L;

			private final int id;

			private LevelEdge(int id) {
				this.id = id;
			}

			@Override
			public LevelVertex getSource() {
				return new LevelVertex(eSource[id]);
			}

			@Override
			public LevelVertex getTarget() {
				return new LevelVertex(eTarget[id]);
			}

			@Override
			public int getWeight() {
				return eWeight[id];
			}

			public boolean isForward() {
				return eDirection[FWD].get(id);
			}

			public boolean isBackward() {
				return eDirection[BWD].get(id);
			}

			public boolean isShortcut() {
				return eIsShortcut.get(id);
			}
		}
	}
}
