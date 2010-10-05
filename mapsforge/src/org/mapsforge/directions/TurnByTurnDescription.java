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
package org.mapsforge.directions;

import java.io.FileInputStream;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Vector;

import javax.naming.directory.InvalidAttributeValueException;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mapsforge.core.GeoCoordinate;
import org.mapsforge.core.MercatorProjection;
import org.mapsforge.preprocessing.graph.osm2rg.osmxml.TagHighway;
import org.mapsforge.server.poi.PointOfInterest;
import org.mapsforge.server.routing.IEdge;
import org.mapsforge.server.routing.IRouter;
import org.mapsforge.server.routing.IVertex;
import org.mapsforge.server.routing.highwayHierarchies.HHRouterServerside;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Turn by turn directions contain a way which was found by a routing algorithm. Streets which
 * are of them same name are concatenated, except in case of a U-turn. Street lengths and angles
 * between streets are calculated and saved.
 * 
 * Several methods to get a string representation like GeoJSON, KML or plain text are provided.
 * 
 * @author Eike Send
 */
public class TurnByTurnDescription {
	private static final int NO_MODE = -1;
	private static final int MOTORWAY_MODE = 0;
	private static final int CITY_MODE = 1;
	private static final int REGIONAL_MODE = 2;
	private static final double VERY_SHORT_STREET_LENGTH = 30;
	Vector<TurnByTurnStreet> streets = new Vector<TurnByTurnStreet>();
	LandmarksFromPerst landmarkService;

	/**
	 * Constructs a TurnByTurnDirectionsObject from an array of IEdges as the are provided by
	 * the IRouter which includes Landmarks
	 * 
	 * @param routeEdges
	 *            is the IEdges array to convert to directions
	 * @param landmarkService
	 *            is the instance to get Landmarks from
	 * @throws InvalidAttributeValueException
	 *             if any parameter is null
	 */
	public TurnByTurnDescription(IEdge[] routeEdges, LandmarksFromPerst landmarkService)
			throws InvalidAttributeValueException {
		if (landmarkService == null)
			throw new InvalidAttributeValueException();
		this.landmarkService = landmarkService;
		generateDirectionsFromPath(routeEdges);
	}

	/**
	 * Constructs a TurnByTurnDirectionsObject from an array of IEdges as the are provided by
	 * the IRouter
	 * 
	 * @param routeEdges
	 *            is the IEdges array to convert to directions
	 * @throws InvalidAttributeValueException
	 *             if the parameter is null
	 */
	public TurnByTurnDescription(IEdge[] routeEdges) throws InvalidAttributeValueException {
		generateDirectionsFromPath(routeEdges);
	}

	void generateDirectionsFromPath(IEdge[] edges) {
		if (edges.length == 0)
			return;
		// These are the edges which are used to make decisions based on local information
		IEdge lastEdge;
		IEdge edgeBeforePoint;
		IEdge edgeAfterPoint;
		IEdge nextEdge;
		// These are both the current decision point, which is at the end of the current edge
		IVertex decisionPointVertex;
		GeoCoordinate decisionPointCoord;
		// These don't change in the process, they are the beginning and end of the route
		GeoCoordinate startPoint = edges[0].getSource().getCoordinate();
		GeoCoordinate endPoint = edges[edges.length - 1].getTarget().getCoordinate();
		// TODO: get start and finishing city with radius
		City startCity = getCityFromCoords(startPoint);
		City endCity = getCityFromCoords(endPoint);
		// this contains concatenated IEdges and represents the current street / road
		TurnByTurnStreet currentStreet = new TurnByTurnStreet(edges[0]);
		// What navigational mode is the current and what was the last one
		int routingMode = NO_MODE;
		int lastRoutingMode = NO_MODE;
		// The whole point of this method boils down to the question if at a given potential
		// decision point a new instruction is to be generated. This boolean represents that.
		boolean startANewStreet;
		for (int i = 0; i < edges.length; i++) {
			// First setup the "environment" variables, ie the edges and points around the
			// potential decision point
			edgeBeforePoint = edges[i];
			decisionPointVertex = edgeBeforePoint.getTarget();
			decisionPointCoord = decisionPointVertex.getCoordinate();
			if (i > 0) {
				lastEdge = edges[i - 1];
			} else {
				lastEdge = null;
			}
			if (i < edges.length - 1) {
				edgeAfterPoint = edges[i + 1];
			} else {
				edgeAfterPoint = null;
			}
			if (i < edges.length - 2) {
				nextEdge = edges[i + 2];
			} else {
				nextEdge = null;
			}
			// Now the variables are set up.
			// First determine which kind of navigational level we're on
			lastRoutingMode = routingMode;
			// if we're on a motorway
			if (isMotorway(edgeBeforePoint)) {
				routingMode = MOTORWAY_MODE;
			}
			// if we're in the start or destination city, we'll do local navigation
			else if (isInStartOrDestinationCity(startCity, endCity, decisionPointCoord)) {
				routingMode = CITY_MODE;
			}
			// if we're not in the start- or end city but on a primary again its motorway
			// routing
			else if (isPrimary(edgeBeforePoint)) {
				routingMode = MOTORWAY_MODE;
			} else {
				// if we're not in the start- or end city and not on a motorway, trunk or
				// primary we must be in regional mode
				routingMode = REGIONAL_MODE;
			}
			// Now that the mode of travel has been determined we need to figure out if a new
			// street is to be started
			startANewStreet = false;
			switch (routingMode) {
				case CITY_MODE:
					startANewStreet = startNewStreetCityMode(lastEdge, edgeBeforePoint,
							edgeAfterPoint, nextEdge, currentStreet);
					break;
				case REGIONAL_MODE:

					startANewStreet = startNewStreetRegionalMode(lastEdge, edgeBeforePoint,
							edgeAfterPoint, nextEdge, currentStreet);
					break;
				case MOTORWAY_MODE:
					startANewStreet = startNewStreetMotorwayMode(lastEdge, edgeBeforePoint,
							edgeAfterPoint, nextEdge, currentStreet);
					break;
			}
			if (lastRoutingMode == NO_MODE) {
				lastRoutingMode = routingMode;
			}
			if (lastRoutingMode != routingMode) {
				startANewStreet = true;
			}
			if (startANewStreet) {
				if (currentStreet.angleFromStreetLastStreet == -360) {
					double delta = getAngleOfStreets(lastEdge, edgeBeforePoint);
					currentStreet.angleFromStreetLastStreet = delta;
				}
				streets.add(currentStreet);
				if (edgeAfterPoint != null)
					currentStreet = new TurnByTurnStreet(edgeAfterPoint);
			} else {
				currentStreet.appendCoordinatesFromEdge(edgeAfterPoint);
			}
		}
		// streets.add(currentStreet);
	}

	private boolean isInStartOrDestinationCity(City start, City end,
			GeoCoordinate decisionPointCoord) {
		if (start == null || end == null)
			return true;
		return (start.contains(decisionPointCoord) || end.contains(decisionPointCoord));
	}

	private City getCityFromCoords(GeoCoordinate point) {
		// TODO Auto-generated method stub
		return null;
	}

	private boolean startNewStreetCityMode(IEdge lastEdge, IEdge edgeBeforePoint,
			IEdge edgeAfterPoint,
			IEdge nextEdge, TurnByTurnStreet currentStreet) {
		// Only one instruction per U-turn is necessary
		// also U-Turns are really the sum of two right angle turns
		if (isUTurn(lastEdge, edgeBeforePoint, edgeAfterPoint)) {
			currentStreet.angleFromStreetLastStreet = 180;
			return false;
		}
		if (haveSameName(edgeBeforePoint, edgeAfterPoint)) {
			// If a U-Turn is performed an instruction is needed
			if (isUTurn(edgeBeforePoint, edgeAfterPoint, nextEdge)) {
				return true;
			}
			return false;
		}
		if (isInTwoLaneJunction(lastEdge, edgeBeforePoint, edgeAfterPoint, nextEdge,
				currentStreet)) {
			return false;
		}
		return true;
	}

	private boolean isInTwoLaneJunction(IEdge lastEdge, IEdge edgeBeforePoint,
			IEdge edgeAfterPoint, IEdge nextEdge, TurnByTurnStreet currentStreet) {
		// If the edge after the decision point is very short and followed by a right angle,
		// the edgeAfterPoint is part of a two lane junction where the name is the one of
		// the street coming from the other direction
		if (isRightAngle(getAngleOfStreets(edgeAfterPoint, nextEdge))
				&& isVeryShortEdge(edgeAfterPoint)) {
			return true;
		}
		// If there was a right angle turn between the last edge and the edge before the
		// decision point and this edge is very short, the edgeBeforePoint is part of a two lane
		// junction and no instruction is needed, only the name should be that of the actual
		// street
		if (isRightAngle(getAngleOfStreets(lastEdge, edgeBeforePoint))
				&& isVeryShortEdge(edgeBeforePoint) && edgeAfterPoint != null) {
			currentStreet.name = edgeAfterPoint.getName();
			return true;
		}
		return false;
	}

	private boolean startNewStreetRegionalMode(IEdge lastEdge, IEdge currentEdge,
			IEdge nextEdge, IEdge secondNextEdge, TurnByTurnStreet currentStreet) {
		return true;
	}

	private boolean startNewStreetMotorwayMode(IEdge lastEdge, IEdge currentEdge,
			IEdge nextEdge, IEdge secondNextEdge, TurnByTurnStreet currentStreet) {
		return true;
	}

	private boolean isVeryShortEdge(IEdge edge) {
		GeoCoordinate source = edge.getSource().getCoordinate();
		GeoCoordinate destination = edge.getTarget().getCoordinate();
		return source.sphericalDistance(destination) < VERY_SHORT_STREET_LENGTH;
	}

	private boolean isRightAngle(double angle) {
		return (90d - 45d < angle && angle < 90d + 45d)
				|| (270d - 45d < angle && angle < 270d + 45d);
	}

	private boolean haveSameName(IEdge edge1, IEdge edge2) {
		if (edge2 == null)
			return false;
		if (edge1 == null)
			return false;
		if (edge1.getName() == null && edge2.getName() == null)
			return true;
		if (edge1.getName() == null || edge2.getName() == null)
			return false;
		return edge1.getName().equalsIgnoreCase(edge2.getName());
	}

	private boolean isMotorway(IEdge curEdge) {
		return curEdge.getType() == TagHighway.MOTORWAY ||
				curEdge.getType() == TagHighway.MOTORWAY_LINK ||
				curEdge.getType() == TagHighway.TRUNK ||
				curEdge.getType() == TagHighway.TRUNK_LINK;
	}

	private boolean isPrimary(IEdge curEdge) {
		return curEdge.getType() == TagHighway.PRIMARY ||
				curEdge.getType() == TagHighway.PRIMARY_LINK;
	}

	/**
	 * Check 3 edges to see if they form a U-turn.
	 * 
	 * @param edge1
	 *            second last Edge before the current edge
	 * @param edge2
	 *            last Edge before the current edge
	 * @param edge3
	 *            current Edge
	 * @return true if the edges form a u-turn around the 2nd edge
	 */
	boolean isUTurn(IEdge edge1, IEdge edge2, IEdge edge3) {
		if (edge1 == null || edge2 == null || edge3 == null)
			return false;
		double angleSum = (getAngleOfStreets(edge1, edge2) + getAngleOfStreets(
				edge2, edge3)) % 360;
		if (haveSameName(edge1, edge3)
				&& (170 < angleSum && angleSum < 190)
				&& isVeryShortEdge(edge2)) {
			return true;
		}
		return false;
	}

	/**
	 * Calculate the angle between two IEdge objects / streets
	 * 
	 * @param edge1
	 *            the IEdge of the street before the crossing
	 * @param edge2
	 *            the IEdge of the street after the crossing
	 * @return the angle between the given streets
	 */
	static double getAngleOfStreets(IEdge edge1, IEdge edge2) {
		double delta = -360.0;
		if (edge1 != null && edge2 != null) {
			// Let's see if i can get the angle between the last street and this
			// This is the crossing
			GeoCoordinate crossingCoordinate = edge2.getAllWaypoints()[0];
			// The following is the last coordinate before the crossing
			GeoCoordinate lastCoordinate = edge1.getAllWaypoints()[edge1
					.getAllWaypoints().length - 2];
			// Take a coordinate further away from the crossing if it's too close
			if (lastCoordinate.sphericalDistance(crossingCoordinate) < 10
					&& edge1.getAllWaypoints().length > 2) {
				lastCoordinate = edge1.getAllWaypoints()[edge1
						.getAllWaypoints().length - 3];
			}
			// Here comes the first coordinate after the crossing
			GeoCoordinate firstCoordinate = edge2.getAllWaypoints()[1];
			if (firstCoordinate.sphericalDistance(crossingCoordinate) < 10
					&& edge2.getAllWaypoints().length > 2) {
				firstCoordinate = edge2.getAllWaypoints()[2];
			}
			// calculate angles of the incoming street
			double deltaY = MercatorProjection.latitudeToMetersY(crossingCoordinate
					.getLatitude())
					- MercatorProjection.latitudeToMetersY(lastCoordinate.getLatitude());
			double deltaX = MercatorProjection.longitudeToMetersX(crossingCoordinate
					.getLongitude())
					- MercatorProjection.longitudeToMetersX(lastCoordinate.getLongitude());
			double alpha = java.lang.Math.toDegrees(java.lang.Math.atan(deltaX / deltaY));
			if (deltaY < 0)
				alpha += 180; // this compensates for the atan result being between -90 and +90
			// deg
			// calculate angles of the outgoing street
			deltaY = MercatorProjection.latitudeToMetersY(firstCoordinate.getLatitude())
					- MercatorProjection.latitudeToMetersY(crossingCoordinate.getLatitude());
			deltaX = MercatorProjection.longitudeToMetersX(firstCoordinate.getLongitude())
					- MercatorProjection.longitudeToMetersX(crossingCoordinate.getLongitude());
			double beta = java.lang.Math.toDegrees(java.lang.Math.atan(deltaX / deltaY));
			if (deltaY < 0)
				beta += 180; // this compensates for the atan result being between -90 and +90
			// deg
			// the angle difference is angle of the turn,
			delta = alpha - beta;
			// For some reason the angle is conterclockwise, so it's turned around
			delta = 360 - delta;
			// make sure there are no values above 360 or below 0
			delta = java.lang.Math.round((delta + 360) % 360);
		}
		return delta;
	}

	@Override
	public String toString() {
		String result = "";
		for (TurnByTurnStreet street : streets) {
			result += street;
		}
		return result;
	}

	/**
	 * Generates a GeoJSON String which represents the route
	 * 
	 * @return a string containing a GeoJSON representation of the route
	 * @throws JSONException
	 *             if the construction of the JSON fails
	 */
	public String toJSONString() throws JSONException {
		JSONObject json = new JSONObject();
		JSONArray jsonfeatures = new JSONArray();
		json.put("type", "FeatureCollection");
		json.put("features", jsonfeatures);
		for (TurnByTurnStreet street : streets) {
			JSONObject jsonstreet = new JSONObject();
			jsonstreet.put("type", "Feature");
			JSONArray streetCoordinatesAsJson = new JSONArray();
			for (int j = 0; j < street.points.size(); j++) {
				GeoCoordinate sc = street.points.elementAt(j);
				streetCoordinatesAsJson.put(new JSONArray()
						.put(sc.getLongitude())
						.put(sc.getLatitude()));
			}
			jsonstreet.put("geometry", new JSONObject()
					.put("type", "LineString")
					.put("coordinates", streetCoordinatesAsJson));
			jsonstreet.put("properties", new JSONObject()
					.put("Name", street.name)
					.put("Ref", street.ref)
					.put("Length", street.length)
					.put("Angle", street.angleFromStreetLastStreet)
					// .put("Landmark_Type", street.nearestLandmark.value)
					// .put("Landmark_Name", street.nearestLandmark.name)
					.put("Roundabout", street.isRoundabout)
					.put("Motorway_Link", street.isMotorwayLink));
			jsonfeatures.put(jsonstreet);
		}
		return json.toString(2);
	}

	/**
	 * Creates a KML (Keyhole markup language) version of the directions.
	 * 
	 * @return a KML string
	 * @throws ParserConfigurationException
	 *             if the DOM can't be built
	 * @throws TransformerConfigurationException
	 *             if turning the DOM into a string fails
	 * @throws TransformerException
	 *             if turning the DOM into a string fails
	 * @throws TransformerFactoryConfigurationError
	 *             if turning the DOM into a string fails
	 */
	public String toKML() throws ParserConfigurationException,
			TransformerConfigurationException, TransformerException,
			TransformerFactoryConfigurationError {
		// This creates a new DOM
		Document dom = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		// And let's get this started
		dom.setXmlVersion("1.0");
		dom.setXmlStandalone(true);
		Element kml = dom.createElement("kml");
		dom.appendChild(kml);
		kml.setAttribute("xmlns", "http://www.opengis.net/kml/2.2");
		Element document = dom.createElement("Document");
		kml.appendChild(document);
		Element name = dom.createElement("name");
		name.setTextContent("MapsForge directions from " + streets.firstElement().name + " to "
				+ streets.lastElement().name);
		document.appendChild(name);
		Element style = dom.createElement("Style");
		style.setAttribute("id", "MapsForgeStyle");
		document.appendChild(style);
		Element lineStyle = dom.createElement("LineStyle");
		style.appendChild(lineStyle);
		Element color = dom.createElement("color");
		color.setTextContent("ff0000ff");
		lineStyle.appendChild(color);
		Element width = dom.createElement("width");
		width.setTextContent("3");
		lineStyle.appendChild(width);
		for (TurnByTurnStreet street : streets) {
			Element placemark = dom.createElement("Placemark");
			document.appendChild(placemark);
			Element placemarkName = dom.createElement("name");
			placemarkName.setTextContent(street.name);
			placemark.appendChild(placemarkName);
			Element lineString = dom.createElement("LineString");
			placemark.appendChild(lineString);
			Element coordinates = dom.createElement("coordinates");
			lineString.appendChild(coordinates);
			String coordinatesContent = "";
			for (GeoCoordinate c : street.points) {
				coordinatesContent += c.getLongitude() + "," + c.getLatitude() + " ";
			}
			coordinatesContent = coordinatesContent.substring(0,
					coordinatesContent.length() - 1); // remove last space
			coordinates.setTextContent(coordinatesContent);
			Element extendedData = dom.createElement("ExtendedData");
			placemark.appendChild(extendedData);
			Element length = dom.createElement("Length");
			extendedData.appendChild(length);
			length.setTextContent(Double.toString(street.length));
			Element angle = dom.createElement("AngleToPreviousStreet");
			extendedData.appendChild(angle);
			angle.setTextContent(Double.toString(street.angleFromStreetLastStreet));
			Element styleUrl = dom.createElement("styleUrl");
			placemark.appendChild(styleUrl);
			styleUrl.setTextContent("#MapsForgeStyle");

		}
		// This is for turning the DOM object into a proper StringWriter
		StringWriter stringWriter = new StringWriter();
		TransformerFactory.newInstance().newTransformer().transform(new DOMSource(dom),
				new StreamResult(stringWriter));
		return stringWriter.getBuffer().toString();
	}

	/**
	 * Creates a GPX (GPS Exchange Format) version of the directions.
	 * 
	 * @return a KML string
	 * @throws ParserConfigurationException
	 *             if the DOM can't be built
	 * @throws TransformerConfigurationException
	 *             if turning the DOM into a string fails
	 * @throws TransformerException
	 *             if turning the DOM into a string fails
	 * @throws TransformerFactoryConfigurationError
	 *             if turning the DOM into a string fails
	 */
	public String toGPX() throws ParserConfigurationException,
			TransformerConfigurationException, TransformerException,
			TransformerFactoryConfigurationError {
		// This creates a new DOM
		Document dom = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		// And let's get this started
		dom.setXmlVersion("1.0");
		dom.setXmlStandalone(true);
		Element gpx = dom.createElement("gpx");
		dom.appendChild(gpx);
		gpx.setAttribute("version", "1.1");
		gpx.setAttribute("xmlns", "http://www.topografix.com/GPX/1/1");
		gpx.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
		gpx.setAttribute("xmlns:mf", "http://tom.mapsforge.de");
		gpx.setAttribute("xsi:schemaLocation",
				"http://www.topografix.com/GPX/1/1 http://www.topografix.com/gpx/1/1/gpx.xsd");
		gpx.setAttribute("creator", "tom.mapsforge.de");
		Element metadata = dom.createElement("metadata");
		gpx.appendChild(metadata);
		Element name = dom.createElement("name");
		name.setTextContent("MapsForge directions from " + streets.firstElement().name + " to "
				+ streets.lastElement().name);
		metadata.appendChild(name);
		for (TurnByTurnStreet street : streets) {
			Element trk = dom.createElement("trk");
			gpx.appendChild(trk);
			Element trkName = dom.createElement("name");
			trkName.setTextContent(street.name);
			trk.appendChild(trkName);
			Element trkseg = dom.createElement("trkseg");
			trk.appendChild(trkseg);
			for (GeoCoordinate c : street.points) {
				Element trkpt = dom.createElement("trkpt");
				trkseg.appendChild(trkpt);
				trkpt.setAttribute("lat", Double.toString(c.getLatitude()));
				trkpt.setAttribute("lon", Double.toString(c.getLongitude()));
			}
			Element extensions = dom.createElement("extensions");
			trkseg.appendChild(extensions);
			Element length = dom.createElement("mf:Length");
			extensions.appendChild(length);
			length.setTextContent(Double.toString(street.length));
			Element angle = dom.createElement("mf:AngleToPreviousStreet");
			extensions.appendChild(angle);
			angle.setTextContent(Double.toString(street.angleFromStreetLastStreet));
		}
		// This is for turning the DOM object into a proper StringWriter
		StringWriter stringWriter = new StringWriter();
		TransformerFactory.newInstance().newTransformer().transform(new DOMSource(dom),
				new StreamResult(stringWriter));
		return stringWriter.getBuffer().toString();
	}

	/**
	 * Represents one or many routing graph edges which belong to the same street
	 * 
	 * @author Eike
	 */
	class TurnByTurnStreet {
		double length = 0;
		double angleFromStreetLastStreet = -360;
		boolean isRoundabout, isMotorwayLink = false;
		Vector<GeoCoordinate> points = new Vector<GeoCoordinate>();
		String name = "";
		String ref = "";
		String type = "";
		PointOfInterest nearestLandmark;
		int exitCount = 0;

		/**
		 * Constructor for using a single IEdge
		 * 
		 * @param edge
		 *            turn this IEdge into a new TurnByTurnStreet
		 */
		TurnByTurnStreet(IEdge edge) {
			name = edge.getName();
			ref = edge.getRef();
			isRoundabout = edge.isRoundabout();
			isMotorwayLink = edge.getType() == TagHighway.MOTORWAY_LINK;
			appendCoordinatesFromEdge(edge);

			if (landmarkService != null) {
				nearestLandmark = landmarkService.getPOINearStreet(edge);
			}
		}

		/**
		 * Append the GeoCoordinates of the given edge to the internal data structure
		 * 
		 * @param edge
		 *            The edge to take the GeoCoordinates from
		 */
		void appendCoordinatesFromEdge(IEdge edge) {
			GeoCoordinate[] newWaypoints = edge.getAllWaypoints();
			if (points.size() > 0 && newWaypoints[0].equals(points.lastElement())) {
				points.removeElementAt(points.size() - 1);
			}
			points.addAll(Arrays.asList(edge.getAllWaypoints()));
			for (int i = 0; i < newWaypoints.length - 1; i++) {
				length += newWaypoints[i].sphericalDistance(newWaypoints[i + 1]);
			}
		}

		@Override
		public String toString() {
			int delta = (int) java.lang.Math.round(angleFromStreetLastStreet / 45);
			String turnInstruction = "";
			if (nearestLandmark != null) {
				double targetLandmarkDistance = points.lastElement().sphericalDistance(
						nearestLandmark.getGeoCoordinate());
				String landmarkName = nearestLandmark.getCategory().getTitle();
				if (nearestLandmark.getName() != null)
					landmarkName += " " + nearestLandmark.getName();
				landmarkName += "\n";
				if (targetLandmarkDistance <= 50) {
					turnInstruction += "Close to " + landmarkName;
				} else if (targetLandmarkDistance <= 100) {
					turnInstruction += java.lang.Math.round(targetLandmarkDistance / 10) * 10
							+ " m after " + landmarkName;
				}
			}
			switch (delta) {
				case 0:
				case 8:
					turnInstruction += "Go straight on ";
					break;
				case 1:
					turnInstruction += "Make a slight right turn onto ";
					break;
				case 2:
					turnInstruction += "Make a right turn onto ";
					break;
				case 3:
					turnInstruction += "Make a sharp right turn onto ";
					break;
				case 4:
					turnInstruction += "Make U-Turn and stay on ";
					break;
				case 5:
					turnInstruction += "Make a sharp left turn onto ";
					break;
				case 6:
					turnInstruction += "Make a left turn onto ";
					break;
				case 7:
					turnInstruction += "Make slight left turn onto ";
					break;
				default:
					turnInstruction += "Go on ";
			}
			String result = "";
			if (!isMotorwayLink && !isRoundabout) {
				result += turnInstruction;
				if (!name.equals("")) {
					result += name;
					if (!ref.equals("")) {
						result += " (" + ref + ")";
					}
				} else {
					if (ref.equals("")) {
						result += "current street";
					} else {
						result += ref;
					}
				}
				result += ",\n";
				length = java.lang.Math.round(length / 10) * 10;
				result += "stay on it for ";
				if (length > 1000) {
					length = java.lang.Math.round(length / 100) / 10;
					result += length + " km.";
				} else {
					result += (int) length + " m.";
				}
			} else if (isMotorwayLink) {
				if (!ref.equals(""))
					result = "Use motorway link " + ref + " " + name;
				else
					result += turnInstruction + "motorway link";
			} else if (isRoundabout) {
				result = "go onto the roundabout " + name + "\n";
				result += "take the ";
				exitCount++;
				switch (exitCount) {
					case 1:
						result += "first";
						break;
					case 2:
						result += "second";
						break;
					case 3:
						result += "third";
						break;
					default:
						result += exitCount + "th";
				}
				result += " exit.";
			}
			result += "\n\n";
			return result;
		}
	}

	/**
	 * @param args
	 *            unused
	 */
	public static void main(String[] args) {
		try {
			long time = System.currentTimeMillis();
			FileInputStream iStream = new FileInputStream("C:/uni/berlin_car.hh");
			IRouter router = HHRouterServerside.deserialize(iStream);
			iStream.close();
			time = System.currentTimeMillis() - time;
			System.out.println("Loaded Router in " + time + " ms");
			time = System.currentTimeMillis();
			String filename = "c:/uni/berlin_landmarks.dbs.clustered";
			LandmarksFromPerst landmarkService = new LandmarksFromPerst(filename);
			time = System.currentTimeMillis() - time;
			System.out.println("Loaded LandmarkBuilder in " + time + " ms");
			int source = router
						.getNearestVertex(new GeoCoordinate(52.53156, 13.40274)).getId();
			int target = router.getNearestVertex(
						new GeoCoordinate(52.49246, 13.41722)).getId();
			IEdge[] sp = router.getShortestPath(source, target);

			time = System.currentTimeMillis() - time;
			TurnByTurnDescription tbtd = new TurnByTurnDescription(sp, landmarkService);
			time = System.currentTimeMillis() - time;
			System.out.println("Route directions built in " + time + " ms");
			System.out.println();
			System.out.println(tbtd);
			landmarkService.persistenceManager.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}