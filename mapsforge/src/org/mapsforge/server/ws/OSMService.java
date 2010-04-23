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
import java.util.List;
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
			props.load(new FileInputStream(getPropFile()));

			router = Router.newInstance(props);
		} catch (Exception exc) {
			router = null;
			// exc.printStackTrace();
			handleException(exc);
		}
	}

	private static Router router;
	private boolean devmode = false; // Set to true to create mock services

	private static String getPropFile() throws FileNotFoundException, IOException {
		// /opt/tomcat6/conf/osm/global.properties auf wasser.mi.fu-...

		Properties props = new Properties();
		String propFileName = null;

		props.load(new FileInputStream("/opt/tomcat6/conf/osm/global.properties"));
		propFileName = props.getProperty("routingGraph.properties");

		return propFileName;
	}

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
		if (devmode) {
			f.setDescription("Berechnung einer Route in Berlin"); // description
			f.setTransportType("Auto"); // transport type
			f.setAlgorithm("A*"); // routing algorithm
		} else {
			f.setDescription("Berechnung einer Route in Berlin"); // description
			f.setTransportType(router.getVehicle().name()); // transport
			// type
			f.setAlgorithm(router.getAlgorithmName()); // routing algorithm
		}
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
		if (devmode) {
			Node[] p = new Node[10];
			p[0] = new Node(13.2969681, 52.4568067);
			p[1] = new Node(13.2944749, 52.4557233);
			p[2] = new Node(13.2906384, 52.4580225);
			p[3] = new Node(13.3044602, 52.4587301);
			p[4] = new Node(13.2974333, 52.4577959);
			p[5] = new Node(13.2985155, 52.4535404);
			p[6] = new Node(13.2958723, 52.4510186);
			p[7] = new Node(13.2867763, 52.4525322);
			p[8] = new Node(13.2979435, 52.4559984);
			p[9] = new Node(13.2926711, 52.4621264);

			result = new Node[max];
			for (int i = 0; i < max; i++)
				result[i] = p[i];
		} else {
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
		// System.out.println("lon: " + coordinates[0][0] + " lat: " +
		// coordinates[0][1] + " -> lon: " +
		// coordinates[coordinates.length-1][0] + " lat: " +
		// coordinates[coordinates.length-1][1]);
		Node[] route = null;
		if (devmode) {
			route = new Node[11];
			route[0] = new Node(13.2987555, 52.4577899);
			route[1] = new Node(13.2986312, 52.457564);
			route[2] = new Node(13.2983602, 52.4574172);
			route[3] = new Node(13.2982862, 52.4573871);
			route[4] = new Node(13.297754, 52.4571561);
			route[5] = new Node(13.2975143, 52.4570509);
			route[6] = new Node(13.2969681, 52.4568067);
			route[7] = new Node(13.2958627, 52.4563265);
			route[8] = new Node(13.2956988, 52.4562528);
			route[9] = new Node(13.2944749, 52.4557233);
			route[10] = new Node(13.2918805, 52.4546849);
		} else {

			ArrayList<Point> pp = new ArrayList<Point>();

			for (int i = 0; i < (coordinates.length); i++) {
				Point point = Point.newInstance(Integer.valueOf(coordinates[i][1]), Integer
						.valueOf(coordinates[i][0]));
				pp.add(point);
			}
			Route iroute = router.route(pp);
			List<org.mapsforge.server.core.geoinfo.Node> nodes = iroute.intermediateNodes();
			ArrayList<Node> p = new ArrayList<Node>();
			for (IPoint n : nodes)
				p.add(new Node(n.getLon(), n.getLat()));

			route = new Node[p.size()];
			for (int i = 0; i < p.size(); i++) {
				route[i] = p.get(i);
			}
		}
		return route;
	}
}
