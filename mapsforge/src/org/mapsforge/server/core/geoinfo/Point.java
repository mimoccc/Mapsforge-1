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
package org.mapsforge.server.core.geoinfo;

/**
 * Represents a Point at the earth's surface, implements the {@link IPoint} interface and is
 * immutable.
 * <p/>
 * This class provides some useful methods for comparison of geographic coordinates.
 * 
 */
public final class Point extends AbstractPoint {

	/**
	 * Is the Point representing the maximum geographic coordinate instance possible.
	 */
	public static final Point MAX_VALUE = Point.newInstance(90.0, 180.0);

	/**
	 * Is the Point representing the minimum geographic coordinate instance possible.
	 */
	public static final Point MIN_VALUE = Point.newInstance(-90.0, -180.0);

	/**
	 * Returns the median geographic coordinate representing Point between the two Points given
	 * as parameters.
	 * 
	 * @param point1
	 *            one point
	 * @param point2
	 *            another point
	 * @return the Point exactly in the middle between the two points, as far as possible due to
	 *         rounding errors.
	 */
	public static Point center(IPoint point1, IPoint point2) {
		int lat = (point1.getLat() + point2.getLat()) / 2;
		int lon = (point1.getLon() + point2.getLon()) / 2;
		return new Point(lat, lon);
	}

	/**
	 * Guarantees getting an instance of Point for each IPoint.
	 * 
	 * @param point
	 *            the INode representation
	 * @return the Node instance
	 */
	public static Point getInstance(IPoint point) {
		if (!(point instanceof Point))
			return new Point(point.getLat(), point.getLon());
		return (Point) point;
	}

	/**
	 * Constructs a new Point object from degree coordinates.
	 * 
	 * @param latitudeDegrees
	 *            the latitude in degrees
	 * @param longitudeDegrees
	 *            the longitude in degrees
	 * @return a new Point object representing those coordinates
	 */
	public static Point newInstance(double latitudeDegrees, double longitudeDegrees) {
		return newInstance(toIntCoordinate(latitudeDegrees), toIntCoordinate(longitudeDegrees));
	}

	/**
	 * Constructs a new Point object from integer coordinates.
	 * <p/>
	 * For more details on integer coordinates see {@link IPoint#DEGREE_TO_INT_FACTOR}.
	 * 
	 * @param latitude
	 *            the latitude as integer coordinate
	 * @param longitude
	 *            the longitude as integer coordinate
	 * @return a new Point object representing those coordinates
	 */
	public static Point newInstance(int latitude, int longitude) {
		int lat = latitude % MAX_LAT;
		int lon = longitude % MAX_LON;
		return new Point(lat, lon);
	}

	/**
	 * Constructs a new Point object from integer coordinate differences to a given IPoint.
	 * <p/>
	 * For more details on integer coordinates see {@link IPoint#DEGREE_TO_INT_FACTOR}.
	 * 
	 * @param point
	 *            the coordinate starting from
	 * @param latDiff
	 *            the latitude difference as integer coordinate
	 * @param lonDiff
	 *            the longitude difference as integer coordinate
	 * @return a new Point object representing those coordinates
	 */
	public static Point newInstance(IPoint point, int latDiff, int lonDiff) {
		return newInstance(
				point.getLat()
						+ (int) (Math.toDegrees(latDiff / MEAN_EARTH_RADIUS) * DEGREE_TO_INT_FACTOR),
				point.getLon()
						+ (int) (Math.toDegrees(lonDiff / MEAN_EARTH_RADIUS) * DEGREE_TO_INT_FACTOR));
	}

	private Point(int latitude, int longitude) {
		super(latitude, longitude);
	}

	/**
	 * Fulfills the equals contract, which needs comparison of exact types and values of this
	 * and the other object.
	 */
	@Override
	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof Point))
			return false;
		Point n = (Point) o;
		return this.lat == n.lat && this.lon == n.lon;
	}
}
