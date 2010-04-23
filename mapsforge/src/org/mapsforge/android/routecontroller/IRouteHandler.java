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
import java.util.Map;

/**
 * callback methods to notify the client application
 * 
 * @author kuehnf
 * 
 */
public interface IRouteHandler {

	public void onGeoCode(List<Point> points);

	/**
	 * passes the calculated points of interests to the client application
	 * 
	 * @param points
	 *            nearest points of interests
	 */
	public void onPoints(List<Point> points);

	/**
	 * passes the calculated route to the client application
	 * 
	 * @param route
	 *            waypoints used by route
	 */
	public void onRoute(List<Point> route);

	/**
	 * passes thrown exceptions to the client application
	 * 
	 * @param error
	 *            thrown by controller
	 * @param desc
	 *            textual description of the error
	 */
	public void onError(ControllerError error, String desc);

	/**
	 * passes the server provided features to the client application
	 * 
	 * @param features
	 *            Map of features provided by server
	 */
	public void onServerFeatures(Map<String, String> features);
}
