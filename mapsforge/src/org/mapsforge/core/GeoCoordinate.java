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
package org.mapsforge.core;

/**
 * This immutable class represents a geological coordinate with a latitude and longitude value.
 */
public class GeoCoordinate implements Comparable<GeoCoordinate> {
	/**
	 * The multiplication factor to convert from double to int.
	 */
	public static final double FACTOR_DOUBLE_TO_INT = 1000000;

	/**
	 * The largest possible latitude value.
	 */
	public static final double LATITUDE_MAX = 90;

	/**
	 * The smallest possible latitude value.
	 */
	public static final double LATITUDE_MIN = -90;

	/**
	 * The largest possible longitude value.
	 */
	public static final double LONGITUDE_MAX = 180;

	/**
	 * The smallest possible longitude value.
	 */
	public static final double LONGITUDE_MIN = -180;

	/**
	 * Converts a coordinate from degrees to microdegrees.
	 * 
	 * @param coordinate
	 *            the coordinate in degrees.
	 * @return the coordinate in microdegrees.
	 */
	public static int doubleToInt(double coordinate) {
		return (int) (coordinate * FACTOR_DOUBLE_TO_INT);
	}

	/**
	 * Converts a coordinate from microdegrees to degrees.
	 * 
	 * @param coordinate
	 *            the coordinate in microdegrees.
	 * @return the coordinate in degrees.
	 */
	public static double intToDouble(int coordinate) {
		return coordinate / FACTOR_DOUBLE_TO_INT;
	}

	/**
	 * Checks the given latitude value and throws an exception if the value is out of range.
	 * 
	 * @param lat
	 *            the latitude value that should be checked.
	 * @return the latitude value.
	 * @throws IllegalArgumentException
	 *             if the latitude value is < LATITUDE_MIN or > LATITUDE_MAX.
	 */
	public static double validateLatitude(double lat) {
		if (lat < LATITUDE_MIN) {
			throw new IllegalArgumentException("invalid latitude value: " + lat);
		} else if (lat > LATITUDE_MAX) {
			throw new IllegalArgumentException("invalid latitude value: " + lat);
		} else {
			return lat;
		}
	}

	/**
	 * Checks the given longitude value and throws an exception if the value is out of range.
	 * 
	 * @param lon
	 *            the longitude value that should be checked.
	 * @return the longitude value.
	 * @throws IllegalArgumentException
	 *             if the longitude value is < LONGITUDE_MIN or > LONGITUDE_MAX.
	 */
	public static double validateLongitude(double lon) {
		if (lon < LONGITUDE_MIN) {
			throw new IllegalArgumentException("invalid longitude value: " + lon);
		} else if (lon > LONGITUDE_MAX) {
			throw new IllegalArgumentException("invalid longitude value: " + lon);
		} else {
			return lon;
		}
	}

	/**
	 * The internal latitude value.
	 */
	private final double latitude;

	/**
	 * The internal longitude value.
	 */
	private final double longitude;

	/**
	 * Constructs a new GeoCoordinate with the given latitude and longitude values, measured in
	 * degrees.
	 * 
	 * @param latitude
	 *            the latitude value in degrees.
	 * @param longitude
	 *            the longitude value in degrees.
	 * @throws IllegalArgumentException
	 *             if the latitude or longitude value is invalid.
	 */
	public GeoCoordinate(double latitude, double longitude) throws IllegalArgumentException {
		this.latitude = validateLatitude(latitude);
		this.longitude = validateLongitude(longitude);
	}

	/**
	 * Constructs a new GeoCoordinate with the given latitude and longitude values, measured in
	 * microdegrees.
	 * 
	 * @param latitudeE6
	 *            the latitude value in microdegrees.
	 * @param longitudeE6
	 *            the longitude value in microdegrees.
	 * @throws IllegalArgumentException
	 *             if the latitude or longitude value is invalid.
	 */
	public GeoCoordinate(int latitudeE6, int longitudeE6) throws IllegalArgumentException {
		this.latitude = validateLatitude(intToDouble(latitudeE6));
		this.longitude = validateLongitude(intToDouble(longitudeE6));
	}

	@Override
	public int compareTo(GeoCoordinate geoCoordinate) {
		if (this.latitude > geoCoordinate.latitude || this.longitude > geoCoordinate.longitude) {
			return 1;
		} else if (this.latitude < geoCoordinate.latitude
				|| this.longitude < geoCoordinate.longitude) {
			return -1;
		}
		return 0;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		} else if (!(obj instanceof GeoCoordinate)) {
			return false;
		} else {
			GeoCoordinate other = (GeoCoordinate) obj;
			if (this.latitude != other.latitude) {
				return false;
			} else if (this.longitude != other.longitude) {
				return false;
			}
			return true;
		}
	}

	/**
	 * Returns the latitude value of this coordinate.
	 * 
	 * @return the latitude value of this coordinate.
	 */
	public double getLatitude() {
		return this.latitude;
	}

	/**
	 * Returns the longitude value of this coordinate.
	 * 
	 * @return the longitude value of this coordinate.
	 */
	public double getLongitude() {
		return this.longitude;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(this.latitude);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(this.longitude);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	@Override
	public String toString() {
		return "latitude: " + this.latitude + ", longitude: " + this.longitude;
	}
}