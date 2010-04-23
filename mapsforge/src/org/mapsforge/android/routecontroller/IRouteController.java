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
package org.mapsforge.android.routecontroller;

import java.util.List;

/**
 * Implementations of this interface allows communication between server and client via a
 * webservice.
 * 
 * @author kuehnf & bogus
 * 
 */
public interface IRouteController extends Runnable {

	/**
	 * Retrieve the coordinates for the given parameters.
	 * 
	 * @param query
	 *            parameters for the starting point
	 * @param wanted
	 *            length(answer) == wanted (if possible)
	 * @param max
	 *            length(answer) <= max
	 */
	public void geoCode(QueryParameters query, short wanted, short max);

	/**
	 * Retrieve the coordinates from the entry point in the graph near the given point.
	 * 
	 * @param point
	 *            point for lookup in the graph
	 * @param wanted
	 *            length(answer) == wanted (if possible)
	 * @param max
	 *            length(answer) <= max
	 */
	public void getPoints(Point point, short wanted, short max);

	/**
	 * Retrieve the server provided features
	 */
	public void getServerFeatures();

	/**
	 * Retrieve the calculated route from start to end.
	 * 
	 * @param wayPoints
	 *            first wayPoints is used as start last wayPoints is used as end // TODO alle
	 *            anderen der Reihe nach.
	 */
	public void getRoute(List<Point> wayPoints);

	/**
	 * Sets the RouteHandler of the used client application.
	 * 
	 * @param rh
	 *            RouteHandler of the client application
	 */
	public void setCallback(IRouteHandler rh);

	/**
	 * Attempts to cancel the actual executed request.
	 */
	public void cancel();

	/**
	 * This method determine the actual status of this Thread.
	 * 
	 * @return true if the RouteControllerThread wait for new jobs else false.
	 */
	public boolean isReady();
}
