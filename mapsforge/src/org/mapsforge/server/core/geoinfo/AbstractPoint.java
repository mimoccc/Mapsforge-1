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
 * This class provides useful methods and structures for Points and Nodes.
 * 
 */
abstract class AbstractPoint implements IPoint {

	/**
	 * The mean earth radius measured in <b>decimeters</b>.
	 */
	public static final double MEAN_EARTH_RADIUS = 63710007.85;

	/**
	 * 
	 * @param latDeg1
	 * @param lonDeg1
	 * @param latDeg2
	 * @param lonDeg2
	 * @return
	 */
	public static final double distance(double latDeg1, double lonDeg1, double latDeg2,
			double lonDeg2) {
		// return toDegrees(distance(toIntCoordinate(latDeg1),
		// toIntCoordinate(lonDeg1), toIntCoordinate(latDeg2),
		// toIntCoordinate(lonDeg2)));
		double dLat = Math.toRadians(latDeg2 - latDeg1);
		double dLon = Math.toRadians(lonDeg2 - lonDeg1);
		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(latDeg1))
				* Math.cos(Math.toRadians(latDeg2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		return MEAN_EARTH_RADIUS * c;
	}

	public static final int distance(int lat1, int lon1, int lat2, int lon2) {
		return (int) distance(toDegrees(lat1), toDegrees(lon1), toDegrees(lat2),
				toDegrees(lon2));
	}

	protected static final double toDegrees(int coordinate) {
		return coordinate / DEGREE_TO_INT_FACTOR;
	}

	protected static final int toIntCoordinate(double degree) {
		return (int) (degree * DEGREE_TO_INT_FACTOR);
	}

	protected final int lat;

	protected final int lon;

	// lazy hashCode initialization
	private volatile int hashCode;

	protected AbstractPoint(int latitude, int longitude) {
		this.lat = latitude;
		this.lon = longitude;
	}

	@Override
	public final int distanceTo(IPoint point) {
		return distance(this.lat, this.lon, point.getLat(), point.getLon());
	}

	@Override
	public final int distanceTo(int latitude, int longitude) {
		return distance(this.lat, this.lon, latitude, longitude);
	}

	@Override
	public final int getLat() {
		return this.lat;
	}

	@Override
	public final int getLon() {
		return this.lon;
	}

	@Override
	public int hashCode() {
		int result = this.hashCode;
		if (result == 0) {
			result = 17;
			result = 32 * result + this.lat;
			result = 32 * result + this.lon;
			this.hashCode = result;
		}
		return result;
	}

	@Override
	public final double latitudeDegrees() {
		return this.lat / DEGREE_TO_INT_FACTOR;
	}

	@Override
	public final double longitudeDegrees() {
		return this.lon / DEGREE_TO_INT_FACTOR;
	}

	/**
	 * Checks equality ignoring the "angle" a Point has regarding to the poles. This "angle" is
	 * coded in the longitude of that Point.
	 * <p/>
	 * A Point always matches itself.
	 * 
	 * @param p
	 *            the other {@link IPoint}
	 * @return true if the two points match in geographical coordinates (under conditions
	 *         described above), false if not
	 */
	@Override
	public final boolean matches(IPoint p) {
		if (this.equals(p)
				|| (this.lat == p.getLat() && (Math.abs(this.lat) == MAX_LAT || this.lon == p
						.getLon())))
			return true;
		return false;
	}

	@Override
	public String toString() {
		return "(" + latitudeDegrees() + "," + longitudeDegrees() + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}
}
