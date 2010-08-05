package org.mapsforge.server.ws;

import java.io.IOException;

import java.io.PrintWriter;
import java.util.ArrayList;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.*;
import org.mapsforge.core.GeoCoordinate;
import org.mapsforge.core.MercatorProjection;
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
	 * This function turns the provided Streets (IEdge) into a JSONObject
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
			double lastAngle = -360d;
			for (int i = 0; i < routeEdges.length; i++) {
				IEdge currentStreetEdge = routeEdges[i];
				// This Object keeps the meta-info of the current street
				JSONObject streetProperties = new JSONObject();
				streetProperties.put("Name", currentStreetEdge.getName());
				IEdge lastStreetEdge = null; // This is the last street as an IEdge
				JSONObject lastStreetJSON = null; // This is the last street as an JSONObject. This is the representation that gets modified
				if (i > 0) {
					lastStreetEdge = routeEdges[i-1];	
					lastStreetJSON = jsonfeatures.getJSONObject(jsonfeatures.length()-1);
				}
				
				// The angle is calculated here
				// We need to do this even if the street name remains the same, because we need to identify U-turns
				
				double delta = getAngleOfStreets(lastStreetEdge, currentStreetEdge);
				
				// This bool marks if the streetname remains the same as before
				boolean sameStreetAsBefore = lastStreetEdge != null && 
						((lastStreetEdge.getName() != null && currentStreetEdge.getName() != null && lastStreetEdge.getName().equals(currentStreetEdge.getName())) || 
						(lastStreetEdge.getName() == null && currentStreetEdge.getName() == null));

				boolean isUturn = false; 
				if (lastStreetJSON != null) {
//					int lastAngle = lastStreetJSON.getJSONObject("properties").optInt("Angle"); 
					if (lastAngle != -360) {
						double anglesum = (lastAngle + delta) % 360;
						if (i > 1 && (170 <= anglesum && anglesum <= 190)) {
							IEdge secondLastStreetEdge = routeEdges[i-2];
							if (currentStreetEdge.getName() != null &&
									secondLastStreetEdge.getName() != null &&
									secondLastStreetEdge.getName()
									.equals(currentStreetEdge.getName())) {
								isUturn = true;
								delta = 180.0;
							}
						}
					}
				}
				lastAngle = delta;

				GeoCoordinate[] streetCoordinates = currentStreetEdge.getAllWaypoints();
				// This is the JSON array which holds the geometry coordinates of an individual street
				JSONArray streetCoordinatesAsJson = new JSONArray();	
				
				/**/
				// If the street is already in the features array, its coordinates will be used
				if (!isUturn && sameStreetAsBefore) {
					streetCoordinatesAsJson = lastStreetJSON.getJSONObject("geometry").getJSONArray("coordinates");
					streetProperties = lastStreetJSON.getJSONObject("properties");
				}
				
				// This if clause checks for 2-lane streets where the uTurn is done 
				if (isUturn && !sameStreetAsBefore) {
					streetCoordinatesAsJson = lastStreetJSON.getJSONObject("geometry").getJSONArray("coordinates");
					streetProperties = lastStreetJSON.getJSONObject("properties");
					// We ignore the name of the supershort street
					streetProperties.put("Name", currentStreetEdge.getName());
					streetProperties.put("Angle", 180);
				}
				/* TODO: 
				 * Check if this street is very short, it may be part of a large 2 lane intersection
				 * if so, it should be displayed as part of the street which is a continuation of  
				 * */
								
				// This loop adds the geocoordinates of the current edge to the geojson array
				int streetLength = 0;
				for (int j = 0; j < streetCoordinates.length; j++) {
					GeoCoordinate sc = streetCoordinates[j];
					streetCoordinatesAsJson.put(new JSONArray()
						.put(sc.getLongitude())
						.put(sc.getLatitude())
					);
					if (j > 0) {
						streetLength += java.lang.Math.round(streetCoordinates[j-1].sphericalDistance(sc)); 
					}
				}
				streetProperties.put("Length", streetLength + streetProperties.optInt("Length"));

				// This section creates a new feature i.e. a new street
				// also if a U-Turn is done on the exact same street a new street is created
				if ((!isUturn && !sameStreetAsBefore) || (isUturn && sameStreetAsBefore)) {
					streetProperties.put("Angle", delta);
					jsonfeatures.put(new JSONObject()
						.put("type", "Feature")
						.put("geometry", new JSONObject()
							.put("type", "LineString")
							.put("coordinates", streetCoordinatesAsJson)
						)
						.put("properties", streetProperties)
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
	 * @param lastStreetEdge the IEdge of the street before the crossing
	 * @param currentStreetEdge the IEdge of the street after the crossing
	 * @return the angle between the given streets
	 */
	private double getAngleOfStreets(IEdge lastStreetEdge, IEdge currentStreetEdge) {
		double delta = -360.0;
		if (lastStreetEdge != null ) {
			// Let's see if i can get the angle between the last street and this
			// This is the crossing
			GeoCoordinate crossingCoordinate = currentStreetEdge.getAllWaypoints()[0]; 
			// The following is the last coordinate before the crossing
			GeoCoordinate lastCoordinate = lastStreetEdge.getAllWaypoints()
					[lastStreetEdge.getAllWaypoints().length-2];
			// Take a coordinate further away from the crossing if it's too close
			if (lastCoordinate.sphericalDistance(crossingCoordinate) < 10 && lastStreetEdge.getAllWaypoints().length > 2) {
				lastCoordinate = lastStreetEdge.getAllWaypoints()
					[lastStreetEdge.getAllWaypoints().length-3];
			}
			// Here comes the first coordinate after the crossing
			GeoCoordinate firstCoordinate = currentStreetEdge.getAllWaypoints()[1];
			if (firstCoordinate.sphericalDistance(crossingCoordinate) < 10 && currentStreetEdge.getAllWaypoints().length > 2) {
				firstCoordinate = currentStreetEdge.getAllWaypoints()[2];
			}			
			// calculate angles of the incoming street
			double deltaY = MercatorProjection.latitudeToMetersY(crossingCoordinate.getLatitude()) 
					- MercatorProjection.latitudeToMetersY(lastCoordinate.getLatitude());
			double deltaX = MercatorProjection.longitudeToMetersX(crossingCoordinate.getLongitude()) 
					- MercatorProjection.longitudeToMetersX(lastCoordinate.getLongitude());
			double alpha = java.lang.Math.toDegrees(java.lang.Math.atan(deltaX / deltaY));
			if (deltaY < 0) alpha += 180; // this compensates for the atan result being between -90 and +90 deg
			// calculate angles of the outgoing street
			deltaY = MercatorProjection.latitudeToMetersY(firstCoordinate.getLatitude()) 
					- MercatorProjection.latitudeToMetersY(crossingCoordinate.getLatitude());
			deltaX = MercatorProjection.longitudeToMetersX(firstCoordinate.getLongitude()) 
					- MercatorProjection.longitudeToMetersX(crossingCoordinate.getLongitude());
			double beta = java.lang.Math.toDegrees(java.lang.Math.atan(deltaX / deltaY));
			if (deltaY < 0) beta += 180; // this compensates for the atan result being between -90 and +90 deg
			// the angle difference is angle of the turn, 
			delta = alpha - beta;
			// For some reason the angle is conterclockwise, so it's turned around
			delta = 360 - delta;
			// make sure there are no values above 360 or below 0
			delta = java.lang.Math.round((delta + 360) % 360);
		} 
		return delta;
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
	
	/**
	 * This method is used for debugging only
	 * @param args
	 */
	public static void main(String[] args) {		
		HHRoutingWebservice hhrs = new HHRoutingWebservice();
		//router = RouterFactory.getRouter("C:/uni/apache-tomcat-6.0.26/webapps/HHRoutingWebservice/WEB-INF/routerFactory.properties");
		router = RouterFactory.getRouter("WebContent/WEB-INF/routerFactory.properties");
		ArrayList<Integer> pointIds = hhrs.parseInputString("8.7826388545226,53.066696341606;8.7753169767347,53.062582199353");
		//ArrayList<Integer> pointIds = hhrs.parseInputString("8.7873039113617,53.099872678854;8.7889815950041,53.097440456826");
		
		// ToDo: handle any number of stops along the way
		// for now its just source and destination
		IEdge[] routeEdges = router.getShortestPath(pointIds.get(0), pointIds.get(1));
		JSONObject json = hhrs.getGeoJson(routeEdges);
		try {
			System.out.println(json.toString(2));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
