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

	public final int minLon, maxLon, minLat, maxLat;

	/**
	 * Constructs a rectangle, it is not checked if given values are valid coordinates and also
	 * if the minimum values are less equal compared to the respective maximum value.
	 * 
	 * @param minLon
	 *            bound of the rectangle.
	 * @param maxLon
	 *            bound of the rectangle.
	 * @param minLat
	 *            bound of the rectangle.
	 * @param maxLat
	 *            bound of the rectangle.
	 */
	public Rect(int minLon, int maxLon, int minLat, int maxLat) {
		this.minLon = minLon;
		this.maxLon = maxLon;
		this.minLat = minLat;
		this.maxLat = maxLat;
	}

	/**
	 * Check if this rectangle. overlaps another rectangle.
	 * 
	 * @param r
	 *            other rectangle to be tested against overlap.
	 * @return true if rectangles overlap.
	 */
	public boolean overlaps(Rect r) {
		return overlaps(minLon, maxLon, minLat, maxLat, r.minLon, r.maxLon, r.minLat, r.maxLat);
	}

	@Override
	public String toString() {
		return "[ (" + minLon + "," + minLat + ") (" + maxLon + "," + maxLat + ") ]";
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
