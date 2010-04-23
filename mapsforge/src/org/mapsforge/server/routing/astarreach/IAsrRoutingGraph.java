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
package org.mapsforge.server.routing.astarreach;

import org.mapsforge.server.routing.core.IRoutingGraph;

public interface IAsrRoutingGraph extends IRoutingGraph {

	public static final int INFINITE_COST = Integer.MAX_VALUE;

	/**
	 * Computes the cost of using the edge defined by the source and destination.
	 * 
	 * @param srcId
	 *            source ID of the edge
	 * @param dstId
	 *            destination ID of the edge
	 * @return cost of the edge
	 */
	int getEdgeCost(int srcId, int dstId);

	/**
	 * Retrieves neighbor IDs for a given vertex ID.
	 * 
	 * @param vertexId
	 *            id of the vertex ending at
	 * @return the IDs of all source vertices for edges ending at this vertex
	 */
	int[] getInNeighbors(int vertexId);

	int[] getOutNeighbors(int vertexId);

	int getReachValue(int vertexId);

	int getRuleCost(int pdcId, int srcId, int dstId);
}
