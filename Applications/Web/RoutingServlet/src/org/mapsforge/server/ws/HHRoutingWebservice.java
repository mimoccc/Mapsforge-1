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
		response.setCharacterEncoding("UTF-8");
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
				//response.setHeader("Content-Type", "application/json;charset=utf-8"); // iso-8859-1  utf-8´
				response.setContentType("application/json; charset=UTF-8");
				json.write(out);
			} else if (format.equalsIgnoreCase("xml")) {
				//response.setHeader("Content-Type", "text/xml; charset=utf-8");//charset=utf-8");
				response.setContentType("text/xml; charset=UTF-8");
				// Put it into a root object
				// "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" + 
				out.write(org.json.XML.toString(json, "xml"));
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
			if (routeEdges == null) {
				return new JSONObject().put("type", "Error").put("message", "Freaking null pointer exception");
			}
			JSONObject json = new JSONObject();
			JSONArray jsonfeatures = new JSONArray();
			json.put("type", "FeatureCollection");
			json.put("features", jsonfeatures);
			int distance;
			for (int i = 0; i < routeEdges.length; i++) {
				IEdge routeEdge = routeEdges[i];
				String StreetName = routeEdge.getName();
				if (StreetName == null) StreetName = "";
				ArrayList<GeoCoordinate> streetCoordinates = new ArrayList<GeoCoordinate>();
				streetCoordinates.add(routeEdge.getSource().getCoordinate());
				streetCoordinates.addAll(Arrays.asList(routeEdge.getWaypoints()));
				streetCoordinates.add(routeEdge.getTarget().getCoordinate());
				JSONArray streetCoordinatesAsJson = new JSONArray();
				distance = 0;
				for (int j = 0; j < streetCoordinates.size(); j++) {
					GeoCoordinate sc = streetCoordinates.get(j);
					streetCoordinatesAsJson.put(new JSONArray()
						.put(sc.getLongitude().getDegree())
						.put(sc.getLatitude().getDegree())
					);
					if (j > 0) {
						distance += streetCoordinates.get(j-1).distance(sc);
					}
				}
				JSONObject last = null;
				if (jsonfeatures.length() > 0) {
					last = jsonfeatures.getJSONObject(jsonfeatures.length()-1);
				}
				// In this if clause streets are merged into a single GeoJSON feature if they have the same name
				if (last != null && 
						last.getJSONObject("properties").getString("Name").equals(StreetName)) {
					for (int m = 1; m < streetCoordinates.size(); m++) {
						GeoCoordinate sc = streetCoordinates.get(m);
						last.getJSONObject("geometry").getJSONArray("coordinates").put(
							new JSONArray()
								.put(sc.getLongitude().getDegree())
								.put(sc.getLatitude().getDegree())
						);
					}
					distance += last.getJSONObject("properties").getInt("Length");
					last.getJSONObject("properties").put("Length", distance);
				} else {
					jsonfeatures.put(new JSONObject()
						.put("type", "Feature")
						.put("geometry", new JSONObject()
							.put("type", "LineString")
							.put("coordinates", streetCoordinatesAsJson)
						)
						.put("properties", new JSONObject()
							.put("Name", StreetName)
							.put("Length", distance)
						)
					);	
				}
			}
			return json;
		} catch (Exception e) {
			System.err.println("Error when creating json");
			e.printStackTrace();
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
	
	public static void main(String[] args) {
		HHRoutingWebservice hhrs = new HHRoutingWebservice();
		//hhrs.init();
		router = RouterFactory.getRouter("C:/uni/apache-tomcat-6.0.26/webapps/HHRoutingWebservice/WEB-INF/routerFactory.properties");
		ArrayList<Integer> pointIds = hhrs.parseInputString("8.7909019470218,53.087836143123;8.8166511535637,53.094743161751");
		// ToDo: handle any number of stops along the way
		// for now its just source and destination
		IEdge[] routeEdges = router.getShortestPath(pointIds.get(0), pointIds.get(1));
		//IEdge[] routeEdges = router.getShortestPath(2, 6921);
		JSONObject json = hhrs.getGeoJson(routeEdges);
		try {
			System.out.println(json.toString(2));
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}
