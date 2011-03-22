/*
 * Copyright 2010, 2011 mapsforge.org
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
package org.mapsforge.preprocessing.highwayHierarchies.mobile;

/**
 * Package private vertex to allow reusing the clustering code on different graph
 * implementations.
 */
interface Vertex {

	/**
	 * @return Returns the identifier of this vertex.
	 */
	public int getId();

	/**
	 * @return Returns the outgoing adjacency list of this vertex.
	 */
	public Edge[] getOutboundEdges();

}
