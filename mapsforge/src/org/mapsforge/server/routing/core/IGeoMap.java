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
import java.util.Date;
import java.util.List;

import org.mapsforge.server.core.geoinfo.BoundingBox;
import org.mapsforge.server.core.geoinfo.Node;
import org.mapsforge.server.core.geoinfo.Point;

/**
 * 
 */
public interface IGeoMap {

	public final class VertexDistance {
		public final int distance;
		public final int vtxId;

		public VertexDistance(int vtxId, int distance) {
			this.vtxId = vtxId;
			this.distance = distance;
		}

		@Override
		public String toString() {
			return "{ID=" + this.vtxId + ",dist=" + this.distance + "}"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
	}

	public static final VertexDistance[] EMPTY_VERTEXDISTANCE_ARRAY = new VertexDistance[0];

	/**
	 * TODO determines the routable vertices for any way point.
	 * 
	 * @param wayPoint
	 *            this <b>must</b> be a point on a way.
	 * @return the IDs of the <b>exactly two</b> directly reachable vertices at the ends of this
	 *         way. Is the point given identically to a vertex location, the result contains
	 *         <b>only the vertex' ID</b>, this means: This method is not thought to be an
	 *         equivalent to the IRoutingGraph method for retrieving adjacent vertices!
	 * @throws IllegalArgumentException
	 *             if the point given is <b>not</b> on any way.
	 */
	VertexDistance[] getAdjacentVertices(Point wayPoint) throws IllegalArgumentException;

	/**
	 * retrieves a map feature: the box building the bounds for <b>all</b> points of this map.
	 * 
	 * @return a surrounding box of this map.
	 */
	BoundingBox getBoundingBox();

	/**
	 * retrieves a map feature: the name of the streetmap (usually a city or country name).
	 * 
	 * @return the name of the streetmap.
	 */
	String getName();

	/**
	 * retrieves a map feature: the date of the data release of the map data all preprocessing,
	 * etc. is based on.
	 * 
	 * @return the release date of the map data.
	 */
	Date getReleaseDate();

	/**
	 * determines the geographical coordinates of a vertex.
	 * 
	 * @param vtxId
	 *            the ID of the vertex to be located.
	 * @return a point representing the location of this vertex.
	 */
	Point getVertexPoint(int vtxId);

	/**
	 * determines the position of the next point lying on <b>any way</b> of the map.
	 * 
	 * @param arbitraryPosition
	 *            should be within the BoundingBox of this map, but that's not a must.
	 * @return a point representing the geographical coordinate of the nearest point
	 */
	Point getWayPoint(Point arbitraryPosition);

	/**
	 * as {@link #getWayPoint(Point)} does, this method determines the position of the next
	 * point lying on <b>any way</b> of the map. Special to this method is the fact that it
	 * returns a larger amount of points, <b>each belonging to a different way</b>. The number
	 * of points is limited by the number in the second argument and the area to search in the
	 * third one.
	 * 
	 * @param arbitraryPosition
	 *            should be within the BoundingBox of this map, but that's not a must.
	 * @param nPointsRequested
	 * @param centralSearchArea
	 * @return all points representing the geographical coordinates of the nearest points of the
	 *         nearest ways.
	 * @see #getWayPoint(Point)
	 */
	Iterable<Point> getWayPoints(Point arbitraryPosition, int nPointsRequested,
			BoundingBox centralSearchArea);

	/**
	 * retrieves <b>all points of the way</b> between (exclusive) the vertices given by their
	 * IDs. The order of the way points is determined by the order of the vertices for
	 * bidirectional ways. So for unidirectional ways there is one direction undefined, in this
	 * case the result returned simply is an empty List, as is for any not existing edge.
	 * 
	 * @param srcId
	 *            the ID of the vertex starting from.
	 * @param dstId
	 *            the ID of the vertex traveling to (on the edge).
	 * @return all points desired.
	 */
	List<Node> getNoneVertexNodes(int srcId, int dstId);

	/**
	 * Locates a vertex of the routing graph with geographical coordinates.
	 * 
	 * @param vtxId
	 *            the ID of the vertex to be located on the map
	 * @return Node representing the vertex
	 */
	Node vertexNode(int vtxId);

	/**
	 * (optional operation) simply writes out this GeoMap into the DataOutput for subsequent
	 * reading of that data at the next startup. This process speeds up the whole startup.
	 * <p/>
	 * Writing a GeoMap once suffices, since it is an immutable object.
	 * <p/>
	 * This method is an alternative to usage of the java {@link java.io.Serializable} pattern,
	 * which might produce some bad overhead sometimes.
	 * <p/>
	 * TODO : tests write() / Serializable
	 * 
	 * @param out
	 *            where to write the GeoMap data to
	 * @throws IOException
	 *             occurs on a writing error.
	 * @throws UnsupportedOperationException
	 *             if operation is not supported
	 */
	void write(DataOutput out) throws IOException, UnsupportedOperationException;

}
