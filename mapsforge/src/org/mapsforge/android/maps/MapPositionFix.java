/*
 * Copyright 2010, 2011 mapsforge.org
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
package org.mapsforge.android.maps;

/**
 * An immutable DTO which carries a latitude and a longitude coordinate together with a zoom level.
 */
public class MapPositionFix {
	/**
	 * Conversion factor from degrees to microdegrees.
	 */
	private static final double CONVERSION_FACTOR = 1000000d;

	/**
	 * The latitude coordinate in degrees.
	 */
	private final double latitude;

	/**
	 * The longitude coordinate in degrees.
	 */
	private final double longitude;

	/**
	 * The zoom level.
	 */
	private final byte zoomLevel;

	/**
	 * Constructs an immutable MapPositionFix with the given parameters.
	 * 
	 * @param latitude
	 *            the latitude coordinate in degrees.
	 * @param longitude
	 *            the longitude coordinate in degrees.
	 * @param zoomLevel
	 *            the zoom level.
	 */
	public MapPositionFix(double latitude, double longitude, byte zoomLevel) {
		this.latitude = latitude;
		this.longitude = longitude;
		this.zoomLevel = zoomLevel;
	}

	/**
	 * @return the latitude value of this MapPositionFix in degrees.
	 */
	public double getLatitude() {
		return this.latitude;
	}

	/**
	 * @return the latitude coordinate in microdegrees (degrees * 10^6).
	 */
	public int getLatitudeE6() {
		return (int) (this.latitude * CONVERSION_FACTOR);
	}

	/**
	 * @return the longitude value of this MapPositionFix in degrees.
	 */
	public double getLongitude() {
		return this.longitude;
	}

	/**
	 * @return the longitude coordinate in microdegrees (degrees * 10^6).
	 */
	public int getLongitudeE6() {
		return (int) (this.longitude * CONVERSION_FACTOR);
	}

	/**
	 * @return the zoom level of this MapPositionFix.
	 */
	public byte getZoomLevel() {
		return this.zoomLevel;
	}
}
