/*
 * Copyright 2010, 2011 mapsforge.org
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.mapsforge.navigation;

import org.mapsforge.core.Edge;
import org.mapsforge.core.GeoCoordinate;
import org.mapsforge.core.Vertex;

/**
 * Defines a map matching service. The implementing class must be capable to serve
 * {@link MapMatchingListener} objects and provide basic map-matching functionality, i.e. return a
 * matched edge or vertex to a given coordinate.
 * 
 * @author thilo ratnaweera
 * 
 */
public interface MapMatchingService {
	/**
	 * Registers a {@link MapMatchingListener}.
	 * 
	 * @param mapMatchingListener
	 *            The listener that should be added.
	 */
	public void addMapMatchingListener(MapMatchingListener mapMatchingListener);

	/**
	 * Removes a {@link MapMatchingListener}.
	 * 
	 * @param mapMatchingListener
	 *            The listener that should be removed.
	 */
	public void removeMapMatchingListener(MapMatchingListener mapMatchingListener);

	/**
	 * @param coordinate
	 *            The coordinate that should be matched to a vertex in the routing graph.
	 * @return The matched vertex.
	 */
	public Vertex getMatchedVertex(GeoCoordinate coordinate);

	/**
	 * @param coordinate
	 *            The coordinate that should be matched to an edge in the routing graph.
	 * @return The matched edge.
	 */
	public Edge getMatchedEdge(GeoCoordinate coordinate);
}
