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
package org.mapsforge.server.ws;

import org.mapsforge.server.geoCoding.Node;

/**
 * Implementations of this interface provides the core methods of the WebService to calculate
 * points and/or a route between them.
 * 
 * @author kuehnf
 * 
 */
public interface IWebService {

	/**
	 * Returns the features of the used routing graph.
	 * 
	 * @return a {@link Features Features} object representing the features
	 */
	public abstract Features getFeatures();

	/**
	 * Calculates points to a given search criteria.
	 * 
	 * @param searchString
	 *            search criteria (e.g. an address)
	 * @param wanted
	 *            the approximate number of returned points
	 * @param max
	 *            the maximal number of returned points
	 * @return a array of points
	 */
	public abstract Node[] getGeoLocation(String searchString, short wanted, short max);

	/**
	 * Finds the next points to a given coordinate.
	 * 
	 * @param points
	 *            the point coordinates encoded as a string
	 * @param wanted
	 *            the approximate number of returned points
	 * @param max
	 *            the maximal number of returned points
	 * @return a array of points
	 */
	public abstract Node[] getNextPoints(String points, short wanted, short max);

	/**
	 * Calculates a route for the given point coordinates.
	 * 
	 * @param points
	 *            the point coordinates encoded as a string
	 * @return a array of points representing a route
	 */
	public abstract Node[] getRoute(String points);

}