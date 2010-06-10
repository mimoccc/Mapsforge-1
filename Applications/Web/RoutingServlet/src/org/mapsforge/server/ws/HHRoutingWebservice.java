package org.mapsforge.server.ws;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.*;
import org.mapsforge.preprocessing.util.GeoCoordinate;
import org.mapsforge.server.routing.IEdge;
import org.mapsforge.server.routing.IRouter;
import org.mapsforge.server.routing.RouterFactory;

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
			IEdge[] routeEdges = router.getShortestPath(pointIds.get(0), pointIds.get(1));
			//IEdge[] routeEdges = router.getShortestPath(2, 6921);
			JSONObject json = getGeoJson(routeEdges);
			String format = request.getParameter("format");
			if (format.equalsIgnoreCase("json")) {
				response.setHeader("Content-Type", "application/json; charset=utf-8"); // iso-8859-1  utf-8
				json.write(out);
			} else if (format.equalsIgnoreCase("xml")) {
				response.setHeader("Content-Type", "text/xml;");
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
	private JSONObject getGeoJson(IEdge[] routeEdges) {
		try {
			JSONObject json = new JSONObject();
			JSONArray jsonfeatures = new JSONArray();
			json.put("type", "FeatureCollection");
			json.put("features", jsonfeatures);
			for (IEdge routeEdge : routeEdges) {
				ArrayList<GeoCoordinate> streetCoordinates = new ArrayList<GeoCoordinate>();
				streetCoordinates.add(routeEdge.getSource().getCoordinate());
				streetCoordinates.addAll(Arrays.asList(routeEdge.getWaypoints()));
				streetCoordinates.add(routeEdge.getTarget().getCoordinate());
				JSONArray streetCoordinatesAsJson = new JSONArray();
				for (GeoCoordinate sc : streetCoordinates) {
					streetCoordinatesAsJson.put(new JSONArray()
						.put(sc.getLongitude().getDegree())
						.put(sc.getLatitude().getDegree())
					);
				}
				jsonfeatures.put(new JSONObject()
					.put("type", "Feature")
					.put("geometry", new JSONObject()
						.put("type", "LineString")
						.put("coordinates", streetCoordinatesAsJson)
					)
					.put("properties", new JSONObject()
						.put("Name", routeEdge.getName())
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
			int id = router.getNearestVertex(new GeoCoordinate(
					Double.valueOf(alternatingCoordinates[i+1]), 
					Double.valueOf(alternatingCoordinates[i])
				)).getId();
			System.out.println(id);
			pp.add(id);
		}
		return pp;
	}	
}
