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

import java.util.Arrays;
import java.util.Vector;

import javax.naming.directory.InvalidAttributeValueException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mapsforge.core.GeoCoordinate;
import org.mapsforge.core.MercatorProjection;
import org.mapsforge.server.routing.IEdge;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

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
	Vector<TurnByTurnStreet> streets = new Vector<TurnByTurnStreet>();

	/**
	 * Constructs a TurnByTurnDirectionsObject from an array of IEdges as the are provided by
	 * the IRouter
	 * 
	 * @param routeEdges
	 *            the IEdges to convert to a
	 * @throws InvalidAttributeValueException
	 *             if the parameter is null
	 */
	public TurnByTurnDescription(IEdge[] routeEdges) throws InvalidAttributeValueException {
		if (routeEdges == null)
			throw new InvalidAttributeValueException();
		IEdge nextEdge = null;
		IEdge currentEdge = null;
		IEdge lastEdge = null;
		IEdge secondLastEdge = null;
		TurnByTurnStreet previousStreet = null;
		for (int i = 0; i < routeEdges.length; i++) {
			if (i < routeEdges.length - 1) {
				nextEdge = routeEdges[i + 1];
			} else {
				nextEdge = null;
			}
			currentEdge = routeEdges[i];
			if (previousStreet == null) {
				previousStreet = new TurnByTurnStreet(currentEdge);
				streets.add(previousStreet);
			} else {
				double delta = getAngleOfStreets(lastEdge, currentEdge);
				boolean isUturn = determineUTurn(secondLastEdge, lastEdge, currentEdge);
				if (hasSameName(currentEdge, lastEdge)) {
					if (!isUturn) {
						// if the same street just continues on, we only attach the
						// GeoCoordinates
						previousStreet.appendCoordinatesFromEdge(currentEdge);
					} else {
						// if it is a uturn on the same street, we start a new street so
						// the information about the uTurn is not lost
						previousStreet = new TurnByTurnStreet(currentEdge);
						previousStreet.angleToPreviousStreet = 180;
						streets.add(previousStreet);
					}
				} else {
					if (isUturn) {
						// if this is a uturn and the last street has a different name,
						// it's only a short street between two lanes. don't need that name
						previousStreet.appendCoordinatesFromEdge(currentEdge);
						previousStreet.name = currentEdge.getName();
						previousStreet.angleToPreviousStreet = 180;
					} else {
						// The first two cases check for very short streets which can be ignored
						// because they belong to streets which accidently have a different name
						// but are only driven on on a 2 lane junction
						if (previousStreet.length < 30d && (delta < 15 || 345 < delta)) {
							previousStreet.appendCoordinatesFromEdge(currentEdge);
							previousStreet.name = currentEdge.getName();
						} else if (currentEdge.getAllWaypoints()[0]
							.sphericalDistance(currentEdge.getAllWaypoints()[currentEdge
								.getAllWaypoints().length - 1]) < 30d
								&& (delta < 15 || 345 < delta)
								&& (nextEdge == null || !hasSameName(nextEdge, currentEdge))) {
							previousStreet.appendCoordinatesFromEdge(currentEdge);
						} else {
							// Here is the last case in which a new street is started
							previousStreet = new TurnByTurnStreet(currentEdge);
							previousStreet.angleToPreviousStreet = delta;
							streets.add(previousStreet);
						}
					}
				}
			}
			secondLastEdge = lastEdge;
			lastEdge = currentEdge;
		}
	}

	/**
	 * check 3 edges to see if they form a U-turn.
	 * 
	 * @param secondLastEdge
	 *            second last Edge before the current edge
	 * @param lastEdge
	 *            last Edge before the current edge
	 * @param currentEdge
	 *            current Edge
	 * @return true if the edges form a u-turn around the 2nd edge
	 */
	boolean determineUTurn(IEdge secondLastEdge, IEdge lastEdge, IEdge currentEdge) {
		if (secondLastEdge == null || lastEdge == null || currentEdge == null)
			return false;
		double angleSum = (getAngleOfStreets(secondLastEdge, lastEdge) +
				getAngleOfStreets(lastEdge, currentEdge)) % 360;
		if (hasSameName(secondLastEdge, currentEdge)
				&&
				(170 < angleSum && angleSum < 190)
				&&
				currentEdge.getAllWaypoints()[0]
					.sphericalDistance(
					secondLastEdge.getAllWaypoints()[secondLastEdge.getAllWaypoints().length - 1]) < 30d) {
			return true;
		}
		return false;
	}

	boolean hasSameName(IEdge edge1, IEdge edge2) {
		if (edge1 != null && edge2 != null) {
			String name1 = edge1.getName();
			String name2 = edge2.getName();
			if (name1 != null && name2 != null && name1.equalsIgnoreCase(name2)) {
				return true;
			}
			if (name1 == null && name2 == null) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Calculate the angle between two IEdge objects / streets
	 * 
	 * @param lastStreetEdge
	 *            the IEdge of the street before the crossing
	 * @param currentStreetEdge
	 *            the IEdge of the street after the crossing
	 * @return the angle between the given streets
	 */
	static double getAngleOfStreets(IEdge lastStreetEdge, IEdge currentStreetEdge) {
		double delta = -360.0;
		if (lastStreetEdge != null) {
			// Let's see if i can get the angle between the last street and this
			// This is the crossing
			GeoCoordinate crossingCoordinate = currentStreetEdge.getAllWaypoints()[0];
			// The following is the last coordinate before the crossing
			GeoCoordinate lastCoordinate = lastStreetEdge.getAllWaypoints()
					[lastStreetEdge.getAllWaypoints().length - 2];
			// Take a coordinate further away from the crossing if it's too close
			if (lastCoordinate.sphericalDistance(crossingCoordinate) < 10
					&& lastStreetEdge.getAllWaypoints().length > 2) {
				lastCoordinate = lastStreetEdge.getAllWaypoints()
						[lastStreetEdge.getAllWaypoints().length - 3];
			}
			// Here comes the first coordinate after the crossing
			GeoCoordinate firstCoordinate = currentStreetEdge.getAllWaypoints()[1];
			if (firstCoordinate.sphericalDistance(crossingCoordinate) < 10
					&& currentStreetEdge.getAllWaypoints().length > 2) {
				firstCoordinate = currentStreetEdge.getAllWaypoints()[2];
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
	 * Creates a valid KML version of the
	 * 
	 * @return a xml / kml string representation of a set of directions
	 */
	public String toXMLString() {
		XStream xstream = new XStream();
		xstream.registerConverter(new TurnByTurnConverter());
		xstream.alias("Document", TurnByTurnDescription.class);
		return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n"
				+ xstream.toXML(this) + "</kml>";
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
					.put(sc.getLatitude())
					);
			}
			jsonstreet.put("geometry", new JSONObject()
				.put("type", "LineString")
				.put("coordinates", streetCoordinatesAsJson)
				);
			jsonstreet.put("properties", new JSONObject()
				.put("Name", street.name)
				.put("Length", street.length)
				.put("Angle", street.angleToPreviousStreet)
				);
			jsonfeatures.put(jsonstreet);
		}
		return json.toString(2);
	}

	/**
	 * Represents one or many routing graph edges which belong to the same street
	 * 
	 * @author Eike
	 */
	class TurnByTurnStreet {
		double length = 0;
		double angleToPreviousStreet = -360;
		Vector<GeoCoordinate> points = new Vector<GeoCoordinate>();
		String name = "";

		/**
		 * Constructor for using a single IEdge
		 * 
		 * @param edge
		 *            turn this IEdge into a new TurnByTurnStreet
		 */
		TurnByTurnStreet(IEdge edge) {
			name = edge.getName();
			appendCoordinatesFromEdge(edge);
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
			return angleToPreviousStreet + ";" + name + ";"
					+ java.lang.Math.round(length) + "m;" + "\n";
		}
	}

	/**
	 * Converts a TurnByTurnDescription object to a XML Object using xstream
	 */
	class TurnByTurnConverter implements Converter {

		@Override
		public boolean canConvert(Class clazz) {
			return clazz.equals(TurnByTurnDescription.class);
		}

		@Override
		public void marshal(Object value, HierarchicalStreamWriter writer,
						MarshallingContext context) {
			TurnByTurnDescription directions = (TurnByTurnDescription) value;
			writer.startNode("name");
			writer.setValue("MapsForge directions from "
					+ directions.streets.firstElement().name + " to "
					+ directions.streets.lastElement().name);
			writer.endNode(); // name
			writer.startNode("Style");
			writer.addAttribute("id", "MapsForgeStyle");
			writer.startNode("LineStyle");
			writer.startNode("color");
			writer.setValue("ff0000ff");
			writer.endNode(); // color
			writer.startNode("width");
			writer.setValue("3");
			writer.endNode(); // width
			writer.endNode(); // LineStyle
			writer.endNode(); // Style

			for (TurnByTurnStreet street : directions.streets) {
				writer.startNode("Placemark");
				writer.startNode("name");
				writer.setValue(street.name);
				writer.endNode();
				writer.startNode("LineString");
				writer.startNode("coordinates");
				String coordinates = "";
				for (GeoCoordinate c : street.points) {
					coordinates += c.getLongitude() + "," + c.getLatitude() + " ";
				}
				writer.setValue(coordinates);
				writer.endNode(); // coordinates
				writer.endNode(); // LineString
				writer.startNode("ExtendedData");
				if (street.angleToPreviousStreet != -360d) {
					writer.startNode("AngleToPreviousStreet");
					writer.setValue(Double.toString(street.angleToPreviousStreet));
					writer.endNode(); // AngleToPreviousStreet
				}
				writer.startNode("Length");
				writer.setValue(Double.toString(street.length));
				writer.endNode(); // Length
				writer.endNode(); // ExtendedData
				writer.startNode("styleUrl");
				writer.setValue("#MapsForgeStyle");
				writer.endNode(); // styleUrl
				writer.endNode(); // Placemark
			}
		}

		/**
		 * Converting XML to TurnByTurnDescription is not implemented
		 */
		public Object unmarshal(HierarchicalStreamReader reader,
						UnmarshallingContext context) {
			return null;
		}

	}
}