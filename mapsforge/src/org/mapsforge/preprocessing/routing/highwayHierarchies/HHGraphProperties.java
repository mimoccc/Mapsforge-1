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
package org.mapsforge.preprocessing.routing.highwayHierarchies;

import java.io.Serializable;
import java.util.Date;

/**
 * @author Frank Viernau
 */
public class HHGraphProperties implements Serializable {

	private static final long serialVersionUID = 1L;

	public final Date creationDate;
	public final String transport;
	public final int h, vertexThreshold, hopLimit, numThreads;
	public final double c, compTimeMinutes;
	public final boolean downgradedEdges;
	public final HHLevelStats[] levelStats;

	public HHGraphProperties(Date creationDate, String transport, int h, int vertexThreshold,
			int hopLimit, int numThreads, double c, double compTimeMinutes,
			boolean downgradedEdges, HHLevelStats[] levelStats) {
		this.creationDate = creationDate;
		this.transport = transport;
		this.h = h;
		this.vertexThreshold = vertexThreshold;
		this.hopLimit = hopLimit;
		this.numThreads = numThreads;
		this.c = c;
		this.compTimeMinutes = compTimeMinutes;
		this.downgradedEdges = downgradedEdges;
		this.levelStats = levelStats;
	}

	@Override
	public String toString() {
		String str = "createdOn = " + creationDate.toString() + "\n" + "transport = '"
				+ transport + "'\n" + "h = " + h + "\n" + "c = " + c + "\n" + "hopLimit = "
				+ hopLimit + "\n" + "downgradedEdges = " + downgradedEdges + "\n"
				+ "vertexThreshold = " + vertexThreshold + "\n" + "numThreads = " + numThreads
				+ "\n" + "compTime = " + compTimeMinutes + " minutes \n";
		for (HHLevelStats ls : levelStats) {
			str += ls.toString() + "\n";
		}
		return str;
	}

	public static class HHLevelStats implements Serializable {

		private static final long serialVersionUID = -6115864997731495133L;

		public final int lvl, numEdges, numVertices, numCoreEdges, numCoreVertices;

		public HHLevelStats(int lvl, int numEdges, int numVertices, int numCoreEdges,
				int numCoreVertices) {
			this.lvl = lvl;
			this.numEdges = numEdges;
			this.numVertices = numVertices;
			this.numCoreEdges = numCoreEdges;
			this.numCoreVertices = numCoreVertices;
		}

		@Override
		public String toString() {
			return "G" + lvl + "  : |V|=" + numVertices + " |E|=" + numEdges + "\n" + "G" + lvl
					+ "' : |V|=" + numCoreVertices + " |E|=" + numCoreEdges;
		}
	}
}
