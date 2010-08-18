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

/*
 * This WAR requires:
 * - json.jar for JSON writing
 * - xstream-1.3.1.jar for XML writing
 * - mapsforge-routing-X.X.X.jar as created by the ant task
 * - trove-3.0.0.a3.jar or later
 * 
 * *.hh file for which the location is set in th routerFactory.properties
 */
package org.mapsforge.server.ws;

import java.io.IOException;

import java.io.PrintWriter;
import java.util.ArrayList;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mapsforge.core.GeoCoordinate;
import org.mapsforge.server.routing.IEdge;
import org.mapsforge.server.routing.IRouter;
import org.mapsforge.server.routing.RouterFactory;

import org.mapsforge.directions.TurnByTurnDescription;

/**
 * Servlet implementation class HHRoutingWebservice
 */
public class HHRoutingWebservice extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	private static IRouter router;
	private String propertiesURI;
	
	public void init() {
		try {
			propertiesURI = getServletContext().getRealPath("/WEB-INF/routerFactory.properties");
			System.out.println("Loading from: " + propertiesURI);
			long t = System.currentTimeMillis();
			router = RouterFactory.getRouter(propertiesURI);
			t = System.currentTimeMillis() - t;
			System.out.println("Loaded in " + t + " milliseconds");
		} catch (Exception e) {
			System.err.print(e.toString());
		}
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.setCharacterEncoding("UTF-8");
		PrintWriter out = response.getWriter();
		try {
			ArrayList<Integer> pointIds = parseInputString(request.getParameter("points"));
			// ToDo: handle any number of stops along the way
			// for now its just source and destination

			IEdge[] routeEdges = router.getShortestPath(pointIds.get(0), pointIds.get(1));
			if (routeEdges == null || routeEdges.length == 0) {
				response.setStatus(500);
				out.print("<error>It seems like I was not able to find a route. Sorry about that!</error>");
				return;
			}
			TurnByTurnDescription turnByTurn = new TurnByTurnDescription(routeEdges);
			String format = request.getParameter("format");
			if (format.equalsIgnoreCase("json")) {
				response.setContentType("application/json; charset=UTF-8");
				out.write(turnByTurn.toJSONString());
			} else if (format.equalsIgnoreCase("xml")) {
				response.setContentType("text/xml; charset=UTF-8");
				out.write(turnByTurn.toXMLString());
			} else if (format.equalsIgnoreCase("kml")) {
				response.setContentType("application/vnd.google-earth.kml+xml");
				out.write(turnByTurn.toXMLString());
			}
		} catch (Exception e) {
			e.printStackTrace();
			out.print("<error>"+e.toString()+"</error>");
		}
	}
	
	/**
	 * Turn a String of coordinates into a List of Points
	 * 
	 * @param points is a serialized String of Coordinates
	 * @return ArrayList of ints representing the IDs 
	 *         of the next vertex in the routing graph  
	 */
	private ArrayList<Integer> parseInputString(String points) {
		String[] alternatingCoordinates = points.split("[;,]");
		ArrayList<Integer> pp = new ArrayList<Integer>();
		for (int i = 0; i < alternatingCoordinates.length - (alternatingCoordinates.length%2); i += 2) {
			double lon = Double.valueOf(alternatingCoordinates[i]);
			double lat = Double.valueOf(alternatingCoordinates[i+1]);
			int id = router.getNearestVertex(new GeoCoordinate(lat, lon)).getId();
			pp.add(id);
		}
		return pp;
	}	
	
}
