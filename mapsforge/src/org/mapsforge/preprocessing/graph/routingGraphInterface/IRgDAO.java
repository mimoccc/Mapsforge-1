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
package org.mapsforge.preprocessing.graph.routingGraphInterface;

/**
 * @author Frank Viernau
 * @param <V>
 * @param <E>
 *            This interface can be used as input for routing graph preprocessing. For a graph
 *            consisting of n vertices, ids should range from 0 to n-1.
 */
public interface IRgDAO<V extends IRgVertex, E extends IRgEdge> {

	public int getNumVertices();

	public int getNumEdges();

	public Iterable<V> getVertices();

	public Iterable<E> getEdges();

}
