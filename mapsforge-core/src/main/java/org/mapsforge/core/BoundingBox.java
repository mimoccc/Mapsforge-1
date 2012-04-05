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
 * A BoundingBox represents an immutable set of two latitude and two longitude coordinates.
 */
public class BoundingBox implements Serializable {
	/**
	 * Conversion factor from degrees to microdegrees.
	 */
	private static final double CONVERSION_FACTOR = 1000000d;

	private static final long serialVersionUID = 1L;

	private static boolean isBetween(int number, int min, int max) {
		return min <= number && number <= max;
	}

	/**
	 * The maximum latitude value of this BoundingBox in microdegrees (degrees * 10^6).
	 */
	public final int maxLatitudeE6;

	/**
	 * The maximum longitude value of this BoundingBox in microdegrees (degrees * 10^6).
	 */
	public final int maxLongitudeE6;

	/**
	 * The minimum latitude value of this BoundingBox in microdegrees (degrees * 10^6).
	 */
	public final int minLatitudeE6;

	/**
	 * The minimum longitude value of this BoundingBox in microdegrees (degrees * 10^6).
	 */
	public final int minLongitudeE6;

	/**
	 * The hash code of this object.
	 */
	private transient int hashCodeValue;

	/**
	 * @param minLatitudeE6
	 *            the minimum latitude in microdegrees (degrees * 10^6).
	 * @param minLongitudeE6
	 *            the minimum longitude in microdegrees (degrees * 10^6).
	 * @param maxLatitudeE6
	 *            the maximum latitude in microdegrees (degrees * 10^6).
	 * @param maxLongitudeE6
	 *            the maximum longitude in microdegrees (degrees * 10^6).
	 */
	public BoundingBox(int minLatitudeE6, int minLongitudeE6, int maxLatitudeE6, int maxLongitudeE6) {
		this.minLatitudeE6 = minLatitudeE6;
		this.minLongitudeE6 = minLongitudeE6;
		this.maxLatitudeE6 = maxLatitudeE6;
		this.maxLongitudeE6 = maxLongitudeE6;
		this.hashCodeValue = calculateHashCode();
	}

	/**
	 * Creates a new rectangle from a String containing comma-separated coordinates in the order minLat, minLon, maxLat,
	 * maxLon in degrees.
	 * 
	 * @param rectString
	 *            the String that describes the rectangle
	 * @return a new rectangle
	 * @throws IllegalArgumentException
	 *             if input String cannot be parsed, or coordinates describe an invalid rectangle
	 */
	public static BoundingBox fromString(String rectString) {
		String[] splitted = rectString.split(",");
		if (splitted.length != 4) {
			throw new IllegalArgumentException(
					"expects 4 comma-separated values that define a bounding box, only found " + splitted.length);
		}

		double minLat = GeoPoint.validateLatitude(Double.parseDouble(splitted[0]));
		double minLon = GeoPoint.validateLongitude(Double.parseDouble(splitted[1]));
		double maxLat = GeoPoint.validateLatitude(Double.parseDouble(splitted[2]));
		double maxLon = GeoPoint.validateLongitude(Double.parseDouble(splitted[3]));

		if (minLat > maxLat) {
			throw new IllegalArgumentException("minLat > maxLat");
		}
		if (minLon > maxLon) {
			throw new IllegalArgumentException("minLon > maxLon");
		}

		return new BoundingBox(GeoPoint.doubleToInt(minLat), GeoPoint.doubleToInt(minLon),
				GeoPoint.doubleToInt(maxLat), GeoPoint.doubleToInt(maxLon));
	}

	/**
	 * @param geoPoint
	 *            the point whose coordinates should be checked.
	 * @return true if this BoundingBox contains the given GeoPoint, false otherwise.
	 */
	public boolean contains(GeoPoint geoPoint) {
		return isBetween(geoPoint.latitudeE6, this.minLatitudeE6, this.maxLatitudeE6)
				&& isBetween(geoPoint.longitudeE6, this.minLongitudeE6, this.maxLongitudeE6);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		} else if (!(obj instanceof BoundingBox)) {
			return false;
		}
		BoundingBox other = (BoundingBox) obj;
		if (this.maxLatitudeE6 != other.maxLatitudeE6) {
			return false;
		} else if (this.maxLongitudeE6 != other.maxLongitudeE6) {
			return false;
		} else if (this.minLatitudeE6 != other.minLatitudeE6) {
			return false;
		} else if (this.minLongitudeE6 != other.minLongitudeE6) {
			return false;
		}
		return true;
	}

	/**
	 * @return the GeoPoint at the horizontal and vertical center of this BoundingBox.
	 */
	public GeoPoint getCenterPoint() {
		int latitudeOffset = (this.maxLatitudeE6 - this.minLatitudeE6) / 2;
		int longitudeOffset = (this.maxLongitudeE6 - this.minLongitudeE6) / 2;
		return new GeoPoint(this.minLatitudeE6 + latitudeOffset, this.minLongitudeE6 + longitudeOffset);
	}

	/**
	 * @return the maximum latitude value of this BoundingBox in degrees.
	 */
	public double getMaxLatitude() {
		return this.maxLatitudeE6 / CONVERSION_FACTOR;
	}

	/**
	 * @return the maximum longitude value of this BoundingBox in degrees.
	 */
	public double getMaxLongitude() {
		return this.maxLongitudeE6 / CONVERSION_FACTOR;
	}

	/**
	 * @return the minimum latitude value of this BoundingBox in degrees.
	 */
	public double getMinLatitude() {
		return this.minLatitudeE6 / CONVERSION_FACTOR;
	}

	/**
	 * @return the minimum longitude value of this BoundingBox in degrees.
	 */
	public double getMinLongitude() {
		return this.minLongitudeE6 / CONVERSION_FACTOR;
	}

	@Override
	public int hashCode() {
		return this.hashCodeValue;
	}

	@Override
	public String toString() {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("BoundingBox [minLatitudeE6=");
		stringBuilder.append(this.minLatitudeE6);
		stringBuilder.append(", minLongitudeE6=");
		stringBuilder.append(this.minLongitudeE6);
		stringBuilder.append(", maxLatitudeE6=");
		stringBuilder.append(this.maxLatitudeE6);
		stringBuilder.append(", maxLongitudeE6=");
		stringBuilder.append(this.maxLongitudeE6);
		stringBuilder.append("]");
		return stringBuilder.toString();
	}

	/**
	 * @return the hash code of this object.
	 */
	private int calculateHashCode() {
		int result = 7;
		result = 31 * result + this.maxLatitudeE6;
		result = 31 * result + this.maxLongitudeE6;
		result = 31 * result + this.minLatitudeE6;
		result = 31 * result + this.minLongitudeE6;
		return result;
	}

	private void readObject(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
		objectInputStream.defaultReadObject();
		this.hashCodeValue = calculateHashCode();
	}
}
