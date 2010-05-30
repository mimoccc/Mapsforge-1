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
package org.mapsforge.server.routing;

import org.mapsforge.preprocessing.util.GeoCoordinate;

public interface IRouter {

	public IEdge[] getShortestPath(int sourceId, int targetId);

	/**
	 * Indexed search on vertice's coordinates.
	 * 
	 * @param coord
	 * @return
	 */
	public IVertex getNearestVertex(GeoCoordinate coord);

	/**
	 * Indexed search only for edges having intermediate waypoints.
	 * 
	 * @param coord
	 * @return
	 */
	public IEdge[] getNearestEdges(GeoCoordinate coord);

	public String getAlgorithmName();

}
