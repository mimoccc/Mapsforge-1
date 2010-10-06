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

import org.mapsforge.core.GeoCoordinate;
import org.mapsforge.preprocessing.graph.osm2rg.osmxml.TagHighway;
import org.mapsforge.server.poi.PointOfInterest;
import org.mapsforge.server.routing.IEdge;

/**
 * Represents one or many routing graph edges which belong to the same street
 * 
 * @author Eike
 */
public class TurnByTurnStreet {
	double length = 0;
	double angleFromStreetLastStreet = -360;
	boolean isRoundabout, isMotorwayLink = false;
	Vector<GeoCoordinate> points = new Vector<GeoCoordinate>();
	String name = "";
	String ref = "";
	String type = "";
	PointOfInterest nearestLandmark;
	int exitCount = 0;
	public static LandmarksFromPerst landmarkService;

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
		if (nearestLandmark != null) {
			double targetLandmarkDistance = points.lastElement().sphericalDistance(
					nearestLandmark.getGeoCoordinate());
			String landmarkName = nearestLandmark.getCategory().getTitle();
			if (nearestLandmark.getName() != null)
				landmarkName += " " + nearestLandmark.getName();
			landmarkName += "\n";
			if (targetLandmarkDistance <= 50) {
				result += "Close to " + landmarkName;
			} else if (targetLandmarkDistance <= 100) {
				result += java.lang.Math.round(targetLandmarkDistance / 10) * 10
						+ " m after " + landmarkName;
			}
		}
		return result;
	}
}
