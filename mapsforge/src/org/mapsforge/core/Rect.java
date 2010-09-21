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

public class Rect {

	public final int minLongitudeE6;
	public final int maxLongitudeE6;
	public final int minLatitudeE6;
	public final int maxLatitudeE6;

	/**
	 * Constructs a rectangle, it is not checked if given values are valid coordinates and also
	 * if the minimum values are less equal compared to the respective maximum value.
	 * 
	 * @param minLongitudeE6
	 *            bound of the rectangle.
	 * @param maxLongitudeE6
	 *            bound of the rectangle.
	 * @param minLatitudeE6
	 *            bound of the rectangle.
	 * @param maxLatitudeE6
	 *            bound of the rectangle.
	 */
	public Rect(int minLongitudeE6, int maxLongitudeE6, int minLatitudeE6, int maxLatitudeE6) {
		this.minLongitudeE6 = minLongitudeE6;
		this.maxLongitudeE6 = maxLongitudeE6;
		this.minLatitudeE6 = minLatitudeE6;
		this.maxLatitudeE6 = maxLatitudeE6;
	}

	/**
	 * @return Returns the coordinate lying in the middle of this rectangle.
	 */
	public GeoCoordinate getCenter() {
		return new GeoCoordinate((minLatitudeE6 + maxLatitudeE6) / 2,
				(minLongitudeE6 + maxLongitudeE6) / 2);
	}

	/**
	 * Check if this rectangle. overlaps another rectangle.
	 * 
	 * @param r
	 *            other rectangle to be tested against overlap.
	 * @return true if rectangles overlap.
	 */
	public boolean overlaps(Rect r) {
		return overlaps(minLongitudeE6, maxLongitudeE6, minLatitudeE6, maxLatitudeE6,
				r.minLongitudeE6, r.maxLongitudeE6, r.minLatitudeE6, r.maxLatitudeE6);
	}

	/**
	 * Checks if the coordinate lies within this rectangle.
	 * 
	 * @param c
	 *            coordinate to check.
	 * @return Returns true if the given coordinate lies within this rectangle.
	 */
	public boolean includes(GeoCoordinate c) {
		return includes(c.getLatitudeE6(), c.getLongitudeE6());
	}

	/**
	 * Checks if the coordinate lies within this rectangle.
	 * 
	 * @param latitudeE6
	 *            coordinate to check.
	 * @param longitudeE6
	 *            coordinate to check.
	 * @return Returns true if the given coordinate lies within this rectangle.
	 */
	public boolean includes(int latitudeE6, int longitudeE6) {
		return latitudeE6 >= minLatitudeE6 && latitudeE6 <= maxLatitudeE6
				&& longitudeE6 >= minLongitudeE6
				&& longitudeE6 <= maxLongitudeE6;
	}

	@Override
	public String toString() {
		return "[ (" + minLongitudeE6 + "," + minLatitudeE6 + ") (" + maxLongitudeE6 + ","
				+ maxLatitudeE6 + ") ]";
	}

	/**
	 * Check if two rectangles overlap.
	 * 
	 * @param minLon1
	 *            bound of rectangle 1.
	 * @param maxLon1
	 *            bound of rectangle 1.
	 * @param minLat1
	 *            bound of rectangle 1.
	 * @param maxLat1
	 *            bound of rectangle 1.
	 * @param minLon2
	 *            bound of rectangle 2.
	 * @param maxLon2
	 *            bound of rectangle 2.
	 * @param minLat2
	 *            bound of rectangle 2.
	 * @param maxLat2
	 *            bound of rectangle 2.
	 * @return true if rectangles overlap.
	 */
	public static boolean overlaps(int minLon1, int maxLon1, int minLat1, int maxLat1,
			int minLon2, int maxLon2, int minLat2, int maxLat2) {
		boolean noOverlap = minLon1 > maxLon2 || minLon2 > maxLon1 || minLat1 > maxLat2
				|| minLat2 > maxLat1;
		return !noOverlap;
	}
}
