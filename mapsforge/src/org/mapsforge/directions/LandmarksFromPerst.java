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

import java.util.Collection;
import java.util.TreeMap;

import org.mapsforge.core.GeoCoordinate;
import org.mapsforge.core.MercatorProjection;
import org.mapsforge.server.poi.PointOfInterest;
import org.mapsforge.server.poi.persistence.IPersistenceManager;
import org.mapsforge.server.poi.persistence.PersistenceManagerFactory;

/**
 * This is kind of a factory for Landmarks in certain rectangle
 * 
 * @author Eike
 */
public class LandmarksFromPerst {

	/**
	 * @param args
	 *            unused
	 */
	public static void main(String[] args) {
		// TODO: Use IEdge to find rectangle(s) around
		// DummyEdge e = new DummyEdge(null, KottbusserStr);

		// tilted rectangle around Schönleinstr - Kottbusser Tor
		GeoCoordinate p1 = new GeoCoordinate(52.49958, 13.41682);
		GeoCoordinate p2 = new GeoCoordinate(52.49961, 13.41898);
		GeoCoordinate p3 = new GeoCoordinate(52.49274, 13.42295);
		GeoCoordinate p4 = new GeoCoordinate(52.49234, 13.42169);
		// junction Kottbusser Tor / kottbusser str
		GeoCoordinate vertex = new GeoCoordinate(52.49886, 13.41841);

		GeoCoordinate boundingboxCoordinate1 = getBoundingBoxSouthWest(p1, p2, p3, p4);
		GeoCoordinate boundingboxCoordinate2 = getBoundingBoxNorthEast(p1, p2, p3, p4);

		// initialize persistenceManager
		String filename = "c:/uni/berlin_landmarks.dbs.clustered";
		IPersistenceManager persistenceManager =
				PersistenceManagerFactory.getPerstMultiRtreePersistenceManager(filename);

		// Get all landmarks within the bounding box of the coordinates
		Collection<PointOfInterest> poisInBoundingBox =
				persistenceManager.findInRect(boundingboxCoordinate1, boundingboxCoordinate2,
						"Landmark");

		// Resourcen freigeben
		persistenceManager.close();

		// Keep only the landmarks which are within the rectangle
		// and put them in a map sorted by distance from vertex
		TreeMap<Double, PointOfInterest> poisInRectangle = new TreeMap<Double, PointOfInterest>();
		for (PointOfInterest poi : poisInBoundingBox) {
			if (isInsideRectangle(p1, p2, p3, p4, poi.getGeoCoordinate())) {
				poisInRectangle.put(vertex.sphericalDistance(poi.getGeoCoordinate()), poi);
			}
		}

		for (PointOfInterest poi : poisInRectangle.values()) {
			System.out.println(java.lang.Math.round(vertex.sphericalDistance(poi
					.getGeoCoordinate()))
					+ " m " + poi.getCategory().getTitle() + " " + poi.getName());
		}

	}

	private static short whichSide(GeoCoordinate p1, GeoCoordinate p2, GeoCoordinate t) {
		if (p2.getLongitude() == p1.getLongitude()) {
			if (t.getLongitude() > p1.getLongitude())
				return -1;
			return 1;
		}
		if (MercatorProjection.latitudeToMetersY(t.getLatitude())
				- MercatorProjection.latitudeToMetersY(p1.getLatitude())
				- (MercatorProjection.latitudeToMetersY(p2.getLatitude()) -
						MercatorProjection.latitudeToMetersY(p1.getLatitude()))
				/ (MercatorProjection.longitudeToMetersX(p2.getLongitude()) -
						MercatorProjection.longitudeToMetersX(p1.getLongitude()))
				* (MercatorProjection.longitudeToMetersX(t.getLongitude()) -
						MercatorProjection.longitudeToMetersX(p1.getLongitude())) < 0) {
			return -1;
		}
		return 1;
	}

	private static boolean isOnTheSameSide(GeoCoordinate p1, GeoCoordinate p2,
			GeoCoordinate t1, GeoCoordinate t2) {
		return whichSide(p1, p2, t1) == whichSide(p1, p2, t2);
	}

	/**
	 * Determine if a {@link GeoCoordinate} is inside a rectangle (actually any convex foursided
	 * geometry works)
	 * 
	 * @param p1
	 *            first corner of rectangle
	 * @param p2
	 *            second corner of rectangle
	 * @param p3
	 *            third corner of rectangle
	 * @param p4
	 *            forth corner of rectangle
	 * @param t
	 *            the coordinate to be tested
	 * @return true if the last parameter is inside the rectangle, false if not
	 */
	public static boolean isInsideRectangle(GeoCoordinate p1, GeoCoordinate p2,
			GeoCoordinate p3, GeoCoordinate p4, GeoCoordinate t) {
		return isOnTheSameSide(p1, p2, p3, t) &&
				isOnTheSameSide(p2, p3, p4, t) &&
				isOnTheSameSide(p3, p4, p1, t) &&
				isOnTheSameSide(p4, p1, p2, t);
	}

	private static GeoCoordinate getBoundingBoxSouthWest(GeoCoordinate p1, GeoCoordinate p2,
			GeoCoordinate p3, GeoCoordinate p4) {
		double south = p1.getLatitude();
		if (p2.getLatitude() < south)
			south = p2.getLatitude();
		if (p3.getLatitude() < south)
			south = p3.getLatitude();
		if (p4.getLatitude() < south)
			south = p4.getLatitude();
		double west = p1.getLongitude();
		if (p2.getLongitude() < west)
			west = p2.getLongitude();
		if (p3.getLongitude() < west)
			west = p3.getLongitude();
		if (p4.getLongitude() < west)
			west = p4.getLongitude();
		return new GeoCoordinate(south, west);
	}

	private static GeoCoordinate getBoundingBoxNorthEast(GeoCoordinate p1, GeoCoordinate p2,
			GeoCoordinate p3, GeoCoordinate p4) {
		double north = p1.getLatitude();
		if (p2.getLatitude() > north)
			north = p2.getLatitude();
		if (p3.getLatitude() > north)
			north = p3.getLatitude();
		if (p4.getLatitude() > north)
			north = p4.getLatitude();
		double east = p1.getLongitude();
		if (p2.getLongitude() > east)
			east = p2.getLongitude();
		if (p3.getLongitude() > east)
			east = p3.getLongitude();
		if (p4.getLongitude() > east)
			east = p4.getLongitude();
		return new GeoCoordinate(north, east);
	}
}
