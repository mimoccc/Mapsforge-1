/*
 * Copyright 2010, 2011, 2012 mapsforge.org
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.mapsforge.core;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

/**
 * A GeoPoint represents an immutable pair of latitude and longitude coordinates.
 */
public class GeoPoint implements Comparable<GeoPoint>, Serializable {
	/**
	 * Conversion factor from degrees to microdegrees.
	 */
	private static final double CONVERSION_FACTOR = 1000000d;

	/**
	 * Equatorial radius of earth is required for distance computation.
	 */
	private static final double EQUATORIALRADIUS = 6378137.0;

	private static final long serialVersionUID = 1L;

	/**
	 * The latitude value of this GeoPoint in microdegrees (degrees * 10^6).
	 */
	public final int latitudeE6;

	/**
	 * The longitude value of this GeoPoint in microdegrees (degrees * 10^6).
	 */
	public final int longitudeE6;

	/**
	 * The hash code of this object.
	 */
	private transient int hashCodeValue;

	/**
	 * @param latitude
	 *            the latitude in degrees, will be limited to the possible latitude range.
	 * @param longitude
	 *            the longitude in degrees, will be limited to the possible longitude range.
	 */
	public GeoPoint(double latitude, double longitude) {
		double limitLatitude = MercatorProjection.limitLatitude(latitude);
		this.latitudeE6 = (int) (limitLatitude * CONVERSION_FACTOR);

		double limitLongitude = MercatorProjection.limitLongitude(longitude);
		this.longitudeE6 = (int) (limitLongitude * CONVERSION_FACTOR);

		this.hashCodeValue = calculateHashCode();
	}

	/**
	 * @param latitudeE6
	 *            the latitude in microdegrees (degrees * 10^6), will be limited to the possible latitude range.
	 * @param longitudeE6
	 *            the longitude in microdegrees (degrees * 10^6), will be limited to the possible longitude range.
	 */
	public GeoPoint(int latitudeE6, int longitudeE6) {
		this(latitudeE6 / CONVERSION_FACTOR, longitudeE6 / CONVERSION_FACTOR);
	}

	@Override
	public int compareTo(GeoPoint geoPoint) {
		if (this.longitudeE6 > geoPoint.longitudeE6) {
			return 1;
		} else if (this.longitudeE6 < geoPoint.longitudeE6) {
			return -1;
		} else if (this.latitudeE6 > geoPoint.latitudeE6) {
			return 1;
		} else if (this.latitudeE6 < geoPoint.latitudeE6) {
			return -1;
		}
		return 0;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		} else if (!(obj instanceof GeoPoint)) {
			return false;
		}
		GeoPoint other = (GeoPoint) obj;
		if (this.latitudeE6 != other.latitudeE6) {
			return false;
		} else if (this.longitudeE6 != other.longitudeE6) {
			return false;
		}
		return true;
	}

	/**
	 * @return the latitude value of this GeoPoint in degrees.
	 */
	public double getLatitude() {
		return this.latitudeE6 / CONVERSION_FACTOR;
	}

	/**
	 * @return the longitude value of this GeoPoint in degrees.
	 */
	public double getLongitude() {
		return this.longitudeE6 / CONVERSION_FACTOR;
	}

	/**
	 * Converts a coordinate from degrees to microdegrees.
	 * 
	 * @param coordinate
	 *            the coordinate in degrees.
	 * @return the coordinate in microdegrees.
	 */
	public static int doubleToInt(double coordinate) {
		return (int) (coordinate * CONVERSION_FACTOR);
	}

	/**
	 * Converts a coordinate from microdegrees to degrees.
	 * 
	 * @param coordinate
	 *            the coordinate in microdegrees.
	 * @return the coordinate in degrees.
	 */
	public static double intToDouble(int coordinate) {
		return coordinate / CONVERSION_FACTOR;
	}

	/**
	 * Calculate the amount of degrees of latitude for a given distance in meters.
	 * 
	 * @param meters
	 *            distance in meters
	 * @return latitude degrees
	 */
	public static double latitudeDistance(int meters) {
		return (meters * 360) / (2 * Math.PI * EQUATORIALRADIUS);
	}

	/**
	 * Calculate the amount of degrees of longitude for a given distance in meters.
	 * 
	 * @param meters
	 *            distance in meters
	 * @param latitude
	 *            the latitude at which the calculation should be performed
	 * @return longitude degrees
	 */
	public static double longitudeDistance(int meters, double latitude) {
		return (meters * 360) / (2 * Math.PI * EQUATORIALRADIUS * Math.cos(Math.toRadians(latitude)));
	}

	/**
	 * Constructs a new GeoCoordinate from a comma-separated String containing latitude and longitude values (also ';',
	 * ':' and whitespace work as separator). Latitude and longitude are interpreted as measured in degrees.
	 * 
	 * @param latLonString
	 *            the String containing the latitude and longitude values in degrees
	 * @return the GeoCoordinate
	 * @throws IllegalArgumentException
	 *             if the latLonString could not be interpreted as a coordinate
	 */
	public static GeoPoint fromString(String latLonString) {
		String[] splitted = latLonString.split("[,;:\\s]");
		if (splitted.length != 2) {
			throw new IllegalArgumentException("cannot read coordinate, not a valid format");
		}
		double latitude = validateLatitude(Double.parseDouble(splitted[0]));
		double longitude = validateLongitude(Double.parseDouble(splitted[1]));
		return new GeoPoint(latitude, longitude);
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
		if (lat < MercatorProjection.LATITUDE_MIN) {
			throw new IllegalArgumentException("invalid latitude value: " + lat);
		} else if (lat > MercatorProjection.LATITUDE_MAX) {
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
		if (lon < MercatorProjection.LONGITUDE_MIN) {
			throw new IllegalArgumentException("invalid longitude value: " + lon);
		} else if (lon > MercatorProjection.LONGITUDE_MAX) {
			throw new IllegalArgumentException("invalid longitude value: " + lon);
		} else {
			return lon;
		}
	}

	@Override
	public int hashCode() {
		return this.hashCodeValue;
	}

	@Override
	public String toString() {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("GeoPoint [latitudeE6=");
		stringBuilder.append(this.latitudeE6);
		stringBuilder.append(", longitudeE6=");
		stringBuilder.append(this.longitudeE6);
		stringBuilder.append("]");
		return stringBuilder.toString();
	}

	/**
	 * @return the hash code of this object.
	 */
	private int calculateHashCode() {
		int result = 7;
		result = 31 * result + this.latitudeE6;
		result = 31 * result + this.longitudeE6;
		return result;
	}

	private void readObject(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
		objectInputStream.defaultReadObject();
		this.hashCodeValue = calculateHashCode();
	}
}
