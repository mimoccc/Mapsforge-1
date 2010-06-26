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
package org.mapsforge.android.map;

/**
 * An implementation of the GeoPoint class from the Google Maps library.
 */
public class GeoPoint implements Comparable<GeoPoint> {
	private static final int MULTIPLICATION_FACTOR = 1000000;
	private final int hashCode;
	private final int latitudeE6;
	private final int longitudeE6;

	/**
	 * Creates a new GeoPoint with the given latitude and longitude, measured in degrees.
	 * 
	 * @param latitude
	 *            the latitude of the point. This will be limited by the minimum and maximum
	 *            possible latitude value.
	 * @param longitude
	 *            the longitude of the point. This will be limited by the minimum and maximum
	 *            possible longitude value.
	 */
	public GeoPoint(double latitude, double longitude) {
		this.latitudeE6 = clipLatitude((int) (latitude * MULTIPLICATION_FACTOR));
		this.longitudeE6 = clipLongitude((int) (longitude * MULTIPLICATION_FACTOR));
		this.hashCode = calculateHashCode();
	}

	/**
	 * Creates a new GeoPoint with the given latitude and longitude, measured in microdegrees.
	 * 
	 * @param latitudeE6
	 *            the latitude of the point in microdegrees. This will be limited by the minimum
	 *            and maximum possible latitude value.
	 * @param longitudeE6
	 *            the longitude of the point in microdegrees. This will be limited by the
	 *            minimum and maximum possible longitude value.
	 */
	public GeoPoint(int latitudeE6, int longitudeE6) {
		this.latitudeE6 = clipLatitude(latitudeE6);
		this.longitudeE6 = clipLongitude(longitudeE6);
		this.hashCode = calculateHashCode();
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
		} else {
			GeoPoint geoPoint = (GeoPoint) obj;
			if (this.latitudeE6 != geoPoint.latitudeE6) {
				return false;
			} else if (this.longitudeE6 != geoPoint.longitudeE6) {
				return false;
			}
			return true;
		}
	}

	/**
	 * Gets the latitude value of this GeoPoint in degrees.
	 * 
	 * @return the latitude value in degrees.
	 */
	public double getLatitude() {
		return this.latitudeE6 / (double) 1000000;
	}

	/**
	 * Gets the latitude value of this GeoPoint in microdegrees.
	 * 
	 * @return the latitude value in microdegrees.
	 */
	public int getLatitudeE6() {
		return this.latitudeE6;
	}

	/**
	 * Gets the longitude value of this GeoPoint in degrees.
	 * 
	 * @return the longitude value in degrees.
	 */
	public double getLongitude() {
		return this.longitudeE6 / (double) 1000000;
	}

	/**
	 * Gets the longitude value of this GeoPoint in microdegrees.
	 * 
	 * @return the longitude value in microdegrees.
	 */
	public int getLongitudeE6() {
		return this.longitudeE6;
	}

	@Override
	public int hashCode() {
		return this.hashCode;
	}

	@Override
	public String toString() {
		return this.latitudeE6 + "," + this.longitudeE6;
	}

	/**
	 * Calculates the hash value of this GeoPoint.
	 * 
	 * @return the hash value of this GeoPoint.
	 */
	private int calculateHashCode() {
		int result = 7;
		result = 31 * result + this.latitudeE6;
		result = 31 * result + this.longitudeE6;
		return result;
	}

	private int clipLatitude(int latitude) {
		if (latitude < MapView.LATITUDE_MIN * MULTIPLICATION_FACTOR) {
			return (int) (MapView.LATITUDE_MIN * MULTIPLICATION_FACTOR);
		} else if (latitude > MapView.LATITUDE_MAX * MULTIPLICATION_FACTOR) {
			return (int) (MapView.LATITUDE_MAX * MULTIPLICATION_FACTOR);
		} else {
			return latitude;
		}
	}

	private int clipLongitude(int longitude) {
		if (longitude < MapView.LONGITUDE_MIN * MULTIPLICATION_FACTOR) {
			return (int) (MapView.LONGITUDE_MIN * MULTIPLICATION_FACTOR);
		} else if (longitude > MapView.LONGITUDE_MAX * MULTIPLICATION_FACTOR) {
			return (int) (MapView.LONGITUDE_MAX * MULTIPLICATION_FACTOR);
		} else {
			return longitude;
		}
	}
}