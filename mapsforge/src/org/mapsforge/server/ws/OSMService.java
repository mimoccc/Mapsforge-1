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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.mapsforge.server.core.geoinfo.BoundingBox;
import org.mapsforge.server.core.geoinfo.IPoint;
import org.mapsforge.server.core.geoinfo.Point;
import org.mapsforge.server.geoCoding.GeoCoderGoogle;
import org.mapsforge.server.geoCoding.Node;
import org.mapsforge.server.routing.core.Route;
import org.mapsforge.server.routing.core.Router;

/**
 * This class represents the WebService and the methods which are available.
 * 
 * @author kuehnf
 * 
 */
public class OSMService implements IWebService {

	static {
		try {
			/*
			 * Initialization of the routing graph
			 */
			Properties props = new Properties();
			//props.load(new FileInputStream(getPropFile()));
			//props.load(new FileInputStream("C:/uni/workspace/mapsforge/res/conf/routingGraph.properties"));
			props.load(new FileInputStream("C:/uni/workspace/mapsforge/src/org/mapsforge/server/routing/core/defaults.properties"));
			
			router = Router.newInstance(props);
		} catch (Exception exc) {
			router = null;
			// exc.printStackTrace();
			handleException(exc);
		}
	}

	private static Router router;
	private boolean devmode = false; // Set to true to create mock services

	private static void handleException(Throwable t) {
		do {
			System.err.println(t.getMessage());
			t.printStackTrace(System.err);
			t = t.getCause();
		} while (t != null);
	}

	/**
	 * Returns the features of the used routing graph.
	 * 
	 * @return a {@link Features Features} object representing the features
	 */
	public Features getFeatures() {
		Features f = new Features();
		f.setDescription("Berechnung einer Route in Berlin"); // description
		f.setTransportType(router.getVehicle().name()); // transport
		// type
		f.setAlgorithm(router.getAlgorithmName()); // routing algorithm
		return f;
	}

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
	public Node[] getGeoLocation(String searchString, short wanted, short max) {
		Node[] p = null;
		if (wanted > 0) {
			// TODO if wanted is set
			List<Node> lp = new GeoCoderGoogle().search(searchString, max);
			p = new Node[lp.size()];
			lp.toArray(p);
		} else {
			List<Node> lp = new GeoCoderGoogle().search(searchString, max);
			p = new Node[lp.size()];
			lp.toArray(p);
		}
		return p;
	}

	/**
	 * Finds the next points to a given coordinate.
	 * 
	 * @param points
	 *            points separated by semicolon and coordinates separated by comma (first
	 *            longitude, second latitude); e.g.
	 *            <code>points=lon1,lat1;lon2,lat2;lon3,lat3</code>. Longitude and Latitude are
	 *            given as Integer with a conversion factor of 10e6.
	 * @param wanted
	 *            the approximate number of returned points
	 * @param max
	 *            the maximal number of returned points
	 * @return a array of points
	 */
	public Node[] getNextPoints(String points, short wanted, short max) {
		String[] input = points.split(";");
		int[][] coordinates = new int[input.length][2];
		for (int i = 0; i < input.length; i++) {
			// coords[0] = longitude
			// coords[1] = latitude
			String[] s = input[i].split(",");
			coordinates[i][0] = Integer.valueOf(s[0]);
			coordinates[i][1] = Integer.valueOf(s[1]);
		}

		/*
		 * coordinates[0][0] longitude of first point coordinates[0][1] latitude of first point
		 */
		if (wanted > 0) {
			// TODO if wanted is set
		} else {

		}
		Node[] result = null;

		Iterable<Point> points1 = router.getGeoMap().getWayPoints(
				Point.newInstance(coordinates[0][1], coordinates[0][0]), max,
				BoundingBox.WHOLE_WORLD);
		ArrayList<Node> nodes = new ArrayList<Node>();
		for (IPoint p : points1) {
			nodes.add(new Node(p.getLon(), p.getLat()));
		}
		result = new Node[nodes.size()];
		for (int i = 0; i < nodes.size(); i++) {
			result[i] = nodes.get(i);
		}
		return result;
	}

	/**
	 * Calculates a route for the given point coordinates.
	 * 
	 * @param points
	 *            points separated by semicolon and coordinates separated by comma (first
	 *            longitude, second latitude); e.g.
	 *            <code>points=lon1,lat1;lon2,lat2;lon3,lat3</code>. Longitude and Latitude are
	 *            given as Integer with a conversion factor of 10e6.
	 * @return a array of points representing a route
	 */
	public Node[] getRoute(String points) {

		String[] input = points.split(";");
		/*
		 * coordinates[0][0] longitude of start point coordinates[0][1] latitude of start point
		 * 
		 * coordinates[X][0] longitude of intermediate stop coordinates[X][1] latitude of
		 * intermediate stop
		 * 
		 * coordinates[coordinates.length-1][0] longitude of end point
		 * coordinates[coordinates.length-1][1] latitude of end point
		 */
		int[][] coordinates = new int[input.length][2];
		for (int i = 0; i < input.length; i++) {
			// coords[0] = longitude
			// coords[1] = latitude
			String[] s = input[i].split(",");
			coordinates[i][0] = Integer.valueOf(s[0]);
			coordinates[i][1] = Integer.valueOf(s[1]);
		}
		Node[] route = null;
		ArrayList<Point> pp = new ArrayList<Point>();
		for (int i = 0; i < (coordinates.length); i++) {
			Point point = Point.newInstance(
					Integer.valueOf(coordinates[i][1]), 
					Integer.valueOf(coordinates[i][0])
				);
			pp.add(point);
		}
		Route iroute = router.route(pp);
		ArrayList<Node> p = new ArrayList<Node>();
		p.add(new Node(iroute.source().getLon(), iroute.source().getLat(), iroute.source().getAttributes()));
		List<org.mapsforge.server.core.geoinfo.Node> nodes = iroute.intermediateNodes();
		for (org.mapsforge.server.core.geoinfo.Node n : nodes) {
			p.add(new Node(n.getLon(), n.getLat(), n.getAttributes()));
		}
		p.add(new Node(iroute.destination().getLon(), iroute.destination().getLat(), iroute.destination().getAttributes()));
		route = new Node[p.size()];
		for (int i = 0; i < p.size(); i++) {
			route[i] = p.get(i);
		}
		return route;
	}
}
