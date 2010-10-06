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
import org.mapsforge.server.routing.IEdge;

/**
 * This is kind of a factory for Landmarks in certain rectangle
 * 
 * @author Eike
 */
public class LandmarksFromPerst {

	private static final double MAX_DISTANCE_FROM_STREET = 50d;
	IPersistenceManager persistenceManager;

	/**
	 * construct a new landmark generator, so to speak
	 * 
	 * @param databaseFileURI
	 *            the file path of the database
	 */
	public LandmarksFromPerst(String databaseFileURI) {
		this.persistenceManager = PersistenceManagerFactory
				.getPerstMultiRtreePersistenceManager(databaseFileURI);
	}

	@Override
	public void finalize() {
		// free resources
		persistenceManager.close();
	}

	/**
	 * get a landmark near the end of the street / edge
	 * 
	 * @param e
	 *            the street which is to be used
	 * @return a landmark near the end or null
	 */
	public PointOfInterest getPOINearStreet(IEdge e) {

		GeoCoordinate source = e.getSource().getCoordinate();
		GeoCoordinate target = e.getTarget().getCoordinate();

		double length = source.sphericalDistance(target);
		MathVector streetVector = new MathVector(
				MercatorProjection.longitudeToMetersX(target.getLongitude()) -
						MercatorProjection.longitudeToMetersX(source.getLongitude()),
				MercatorProjection.latitudeToMetersY(target.getLatitude()) -
						MercatorProjection.latitudeToMetersY(source.getLatitude()), length);
		MathVector streetNormalVector = streetVector.getNormalVector();

		// Here a bounding box around the source and the target is calculated, which follows the
		// direction of the street
		// These are the 2 front Coordinates
		GeoCoordinate p1 = getOrthogonalGeoCoordinate(target, streetNormalVector,
				-MAX_DISTANCE_FROM_STREET);
		GeoCoordinate p2 = getOrthogonalGeoCoordinate(target, streetNormalVector,
				MAX_DISTANCE_FROM_STREET);
		// These are the 2 rear Coordinates
		GeoCoordinate p3 = getOrthogonalGeoCoordinate(source, streetNormalVector,
				MAX_DISTANCE_FROM_STREET);
		GeoCoordinate p4 = getOrthogonalGeoCoordinate(source, streetNormalVector,
				-MAX_DISTANCE_FROM_STREET);
		// additionally, go a short distance ahead onto the junction
		p1 = getOrthogonalGeoCoordinate(p1, streetVector, MAX_DISTANCE_FROM_STREET);
		p2 = getOrthogonalGeoCoordinate(p2, streetVector, MAX_DISTANCE_FROM_STREET);

		// Calculate the outer bounding box of the tilted box
		GeoCoordinate boundingboxCoordinate1 = getBoundingBoxSouthWest(p1, p2, p3, p4);
		GeoCoordinate boundingboxCoordinate2 = getBoundingBoxNorthEast(p1, p2, p3, p4);

		// Get all landmarks within the outer bounding box of the coordinates
		Collection<PointOfInterest> poisInBoundingBox =
				persistenceManager.findInRect(
						boundingboxCoordinate1,
						boundingboxCoordinate2,
						"Landmark");

		// Keep only the landmarks which are within the inner bounding box (tilted rectangle)
		// and put them in a map sorted by distance from vertex
		TreeMap<Double, PointOfInterest> poisInRectangle = new TreeMap<Double, PointOfInterest>();
		for (PointOfInterest poi : poisInBoundingBox) {
			if (isInsideRectangle(p1, p2, p3, p4, poi.getGeoCoordinate())) {
				poisInRectangle.put(target.sphericalDistance(poi.getGeoCoordinate()), poi);
			}
		}
		if (poisInRectangle.isEmpty()) {
			return null;
		}
		return poisInRectangle.firstEntry().getValue();
	}

	private static GeoCoordinate getOrthogonalGeoCoordinate(GeoCoordinate target,
			MathVector normalVector, double d) {
		return new GeoCoordinate(
				MercatorProjection.metersYToLatitude(
						MercatorProjection.latitudeToMetersY(target.getLatitude())
								+ d * normalVector.y),
				MercatorProjection.metersXToLongitude(
						MercatorProjection.longitudeToMetersX(target.getLongitude())
								+ d * normalVector.x));
	}

	/**
	 * This function checks if {@link GeoCoordinate} t is on one side or the other of a line
	 * formed by the first two parameters.
	 * 
	 * It is used at the core of the function which checks if a {@link GeoCoordinate} is inside
	 * a rectangle.
	 * 
	 * @param p1
	 *            first point of the line
	 * @param p2
	 *            second point of the line
	 * @param t
	 *            the point be checked
	 * @return -1 for one side or 1 for the other
	 */
	private static short whichSide(GeoCoordinate p1, GeoCoordinate p2, GeoCoordinate t) {
		if (p2.getLongitude() == p1.getLongitude()) {
			if (t.getLongitude() > p1.getLongitude())
				return -1;
			return 1;
		}
		if (t.getLatitude() - p1.getLatitude() -
				(p2.getLatitude() - p1.getLatitude())
				/ (p2.getLongitude() - p1.getLongitude())
				* (t.getLongitude() - p1.getLongitude()) < 0) {
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

	// private boolean isNorthOf()
}

class MathVector {
	double x;
	double y;
	double l;

	public MathVector(double x, double y, double l) {
		this.x = x / l;
		this.y = y / l;
		this.l = l;
	}

	public MathVector(double x, double y) {
		this.x = x;
		this.y = y;
		this.l = 1;
	}

	public MathVector getNormalVector() {
		return new MathVector(y, -x);
	}
}
