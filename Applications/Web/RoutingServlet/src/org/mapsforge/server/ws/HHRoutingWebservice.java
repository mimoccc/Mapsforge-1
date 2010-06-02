package org.mapsforge.server.ws;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedList;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.*;
import org.mapsforge.preprocessing.routing.highwayHierarchies.datastructures.HHStaticGraph.HHStaticEdge;
import org.mapsforge.preprocessing.routing.highwayHierarchies.util.geo.PolarCoordinate;
import org.mapsforge.server.routing.highwayHierarchies.*;

/**
 * Servlet implementation class HHRoutingWebservice
 */
public class HHRoutingWebservice extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	private static HHRouter router;
	
	public void init() {
		try {
			router = HHRouterFactory.getHHRouterInstance();
		} catch (Exception e) {
			System.err.print(e.toString());
		}
	}
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public HHRoutingWebservice() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		PrintWriter out = response.getWriter();
		try {
			
			ArrayList<Integer> pointIds = parseInputString(request.getParameter("points"));
			// ToDo: handle any number of stops along the way
			// for now its just source and destination
			LinkedList<HHStaticEdge> routeEdges = router.getShortestPath(pointIds.get(0), pointIds.get(1));
			JSONObject json = getGeoJson(routeEdges);
			String format = request.getParameter("format");
			System.out.println(format);
			if (format.equalsIgnoreCase("json")) {
				response.setHeader("Content-Type", "application/json");
				json.write(out);
			} else if (format.equalsIgnoreCase("xml")) {
				response.setHeader("Content-Type", "text/xml");
				// Put it into a root object
				out.write(org.json.XML.toString(json, "route"));
			}
		} catch (Exception e) {
			System.err.print(e.toString());
			out.println("Error");
		}
	}
	
	/**
	 * @param c the coordinate which is to be added
	 * @param a the JSONArray to which the coordinate is added
	 */
	private JSONObject getGeoJson(LinkedList<HHStaticEdge> routeEdges) {
		try {
			JSONObject json = new JSONObject();
			JSONArray jsonfeatures = new JSONArray();
			json.put("type", "FeatureCollection");
			json.put("features", jsonfeatures);
			for (HHStaticEdge routeEdge : routeEdges) {
				ArrayList<PolarCoordinate> streetCoordinates = new ArrayList<PolarCoordinate>();
				streetCoordinates.add(router.getCoordinate(routeEdge.getSource().getId()));
				streetCoordinates.add(router.getCoordinate(routeEdge.getTarget().getId()));
				JSONArray streetCoordinatesAsJson = new JSONArray();
				for (PolarCoordinate sc : streetCoordinates) {
					streetCoordinatesAsJson.put(new JSONArray()
						.put(sc.getLongitudeDouble())
						.put(sc.getLatitudeDouble())
					);
				}
				jsonfeatures.put(new JSONObject()
					.put("type", "Feature")
					.put("geometry", new JSONObject()
						.put("type", "LineString")
						.put("coordinates", streetCoordinatesAsJson)
					)
					.put("properties", new JSONObject()
						.put("Name", "Value")
					)
				);	
			}
			return json;
		} catch (Exception e) {
			System.err.println("Error when creating json");
			return new JSONObject();
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
		for (int i = 0; i < alternatingCoordinates.length; i += 2) {
			pp.add(router.getNearestVertexId(
					Double.valueOf(alternatingCoordinates[i]), 
					Double.valueOf(alternatingCoordinates[i+1])
				));
		}
		return pp;
	}	
}
