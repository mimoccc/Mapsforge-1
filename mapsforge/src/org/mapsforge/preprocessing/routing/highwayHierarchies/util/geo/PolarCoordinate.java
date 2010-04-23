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
package org.mapsforge.preprocessing.routing.highwayHierarchies.util.geo;

import java.io.Serializable;

/**
 * @author Frank Viernau
 */
public class PolarCoordinate implements Serializable {

	private static final long serialVersionUID = 763186057388414470L;

	private final static double FAC_DOUBLE_TO_INT = 1E7;
	private final static double FAC_INT_TO_DOUBLE = 1 / 1E7;
	private final static double EARTH_RADIUS = 6371000.785d;

	public static int double2Int(double d) {
		return (int) Math.rint(d * FAC_DOUBLE_TO_INT);
	}

	public static double int2Double(int i) {
		return FAC_INT_TO_DOUBLE * i;
	}

	public static double distanceMeters(int lon1, int lat1, int lon2, int lat2) {
		return distanceMeters(int2Double(lon1), int2Double(lat1), int2Double(lon2),
				int2Double(lat2));
	}

	public static double distanceMeters(double lon1, double lat1, double lon2, double lat2) {
		double dLat = Math.toRadians(lat2 - lat1);
		double dLon = Math.toRadians(lon2 - lon1);
		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(lat1))
				* Math.cos(Math.toRadians(lat1)) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

		return c * EARTH_RADIUS;
	}

	private int lon, lat;

	public PolarCoordinate(int lon, int lat) {
		this.lon = lon;
		this.lat = lat;
	}

	public PolarCoordinate(double lon, double lat) {
		this.lon = double2Int(lon);
		this.lat = double2Int(lat);
	}

	public double getLongitudeDouble() {
		return int2Double(lon);
	}

	public double getLatitudeDouble() {
		return int2Double(lat);
	}

	public int getLongitudeInt() {
		return lon;
	}

	public int getLatitudeInt() {
		return lat;
	}

	@Override
	public String toString() {
		return "(" + getLongitudeDouble() + ", " + getLatitudeDouble() + ")";
	}

	public double distanceMeters(PolarCoordinate other) {
		return distanceMeters(lon, lat, other.lon, other.lat);
	}
}
