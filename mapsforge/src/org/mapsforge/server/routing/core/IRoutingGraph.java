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
package org.mapsforge.server.routing.core;

import java.io.DataOutput;
import java.io.IOException;

public interface IRoutingGraph {

	/**
	 * determines the number of edges in the whole graph.
	 * 
	 * @return the number of edges.
	 */
	long getNEdges();

	/**
	 * determines the number of vertices in the whole graph.
	 * 
	 * @return the number of vertices.
	 */
	int getNVertices();

	/**
	 * <i>Important:</i> Bidirectional edges <b>cannot</b> have different weights in different
	 * directions!
	 * 
	 * @param sourceId
	 *            ID of the source vertex.
	 * @param destinationId
	 *            ID of the destination vertex.
	 * @return {@code true} if the edge identified by the source's and destination's ID can be
	 *         used in both directions, {@code false} otherwise.
	 */
	boolean isBidirectional(int sourceId, int destinationId);

	/**
	 * @param out
	 *            where to write the component data to
	 * @throws IOException
	 *             occurs on a writing error.
	 */
	void write(DataOutput out) throws IOException;

}
