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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import org.mapsforge.server.core.geoinfo.BoundingBox;
import org.mapsforge.server.core.geoinfo.IPoint;
import org.mapsforge.server.core.geoinfo.IWay;
import org.mapsforge.server.core.geoinfo.Point;
import org.mapsforge.server.core.geoinfo.Node;
import org.mapsforge.server.geoCoding.GeoCoderGoogle;
import org.mapsforge.server.geoCoding.GeoCoderNode;
import org.mapsforge.server.routing.core.Route;
import org.mapsforge.server.routing.core.Router;
import org.mapsforge.server.routing.core.Route.Section;

/**
 * This class represents the WebService and the methods which are available.
 * Use the Axis2 Service Archiver to generate the appropriate .aar File
 * 
 * Configure Axis to provide json output as follows (taken from http://www.marcusschiesser.de/?p=130):
 * 
 * For json support install dynamicresponse into webapps\axis2\WEB-INF\modules from
 * http://dist.wso2.org/maven2/org/wso2/dynamicresponse/wso2dynamic-response/1.5/wso2dynamic-response-1.5.mar
 * 
 * also you need to enable it by adding this line to webapps\axis\WEB-INF\conf\axis2.conf
 * <module ref="DynamicResponseHandler"/>
 * 
 * finally replace webapps\axis2\WEB-INF\lib\jettisonXXX.jar with
 * http://www.marcusschiesser.de/wp-content/uploads/2009/01/jettison-11-snapshot.jar
 * 
 * HTTP requests can the be appended by the query param response=application/json
 * 
 * @author kuehnf, eikesend
 * 
 */
public class OSMService implements IWebService {

	static {
		try {
			/*
			 * Initialization of the routing graph
			 */
			Properties props = new Properties();
			props.load(new FileInputStream("C:/uni/workspace/mapsforge/src/org/mapsforge/server/routing/core/defaults.properties"));
			
			router = Router.newInstance(props);
		} catch (Exception exc) {
			router = null;
			// exc.printStackTrace();
			handleException(exc);
		}
	}

	private static Router router;

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
	public GeoCoderNode[] getGeoLocation(String searchString, short wanted, short max) {
		GeoCoderNode[] p = null;
		if (wanted > 0) {
			// TODO if wanted is set
			List<GeoCoderNode> lp = new GeoCoderGoogle().search(searchString, max);
			p = new GeoCoderNode[lp.size()];
			lp.toArray(p);
		} else {
			List<GeoCoderNode> lp = new GeoCoderGoogle().search(searchString, max);
			p = new GeoCoderNode[lp.size()];
			lp.toArray(p);
		}
		return p;
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
		// Hand this list to the router by requesting a route
		Route route = router.route(parseInputString(points));
		// prepare the result for output
		ArrayList<Node> nodes = new ArrayList<Node>();
		nodes.add(route.source());
		nodes.addAll(route.intermediateNodes());
		nodes.add(route.destination());
		Node[] result = new Node[nodes.size()];
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
	public Node[] getSubRoutes(String points) {
		// Hand this list to the router by requesting a route
		Route route = router.route(parseInputString(points));
		// prepare the result for output
		
		List<Section> subRoutes = route.sections();
		ArrayList<Node> resultList= new ArrayList();
		for (Section subRoute : subRoutes) {
			Node n = subRoute.source();
			n.getAttributes().put("source", "true");
			resultList.add(n);
			resultList.addAll(subRoute.intermediateNodes());
			n = subRoute.destination();
			n.getAttributes().put("destination", "true");
			resultList.add(n);
		}
		Node[] results = new Node[resultList.size()];
		for (int i = 0; i < results.length; i++) 
			results[i] = resultList.get(i);
		return results;
	}
	
	/**
	 * Turn a String of coordinates into a List of Points
	 * 
	 * @param points is a serialized String of Coordinates, see getRoute 
	 * @return ArrayList of Points representing these Coordinates
	 */
	private ArrayList<Point> parseInputString(String points) {
		String[] alternatingCoordinates = points.split("[;,]");
		ArrayList<Point> pp = new ArrayList<Point>();
		for (int i = 0; i < alternatingCoordinates.length; i += 2) {
			Point point = Point.newInstance(
					Integer.valueOf(alternatingCoordinates[i+1]),
					Integer.valueOf(alternatingCoordinates[i])
				);
			pp.add(point);
		}
		return pp;
	}
}
