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
 * This class represents a bounding box.
 * 
 * @author bross
 * 
 */
public class Rect {

	/**
	 * The minimum longitude of this rectangle.
	 */
	public int minLongitudeE6;
	/**
	 * The maximum longitude of this rectangle.
	 */
	public int maxLongitudeE6;
	/**
	 * The minimum latitude of this rectangle.
	 */
	public int minLatitudeE6;
	/**
	 * The maximum latitude of this rectangle.
	 */
	public int maxLatitudeE6;

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
	 * Constructs a rectangle defined by a single coordinate, i.e. minLongitudeE6 =
	 * maxLongitudeE6 and minLatitudeE6 = maxLatitudeE6.
	 * 
	 * @param gc
	 *            the coordinate to define the rectangle
	 */
	public Rect(GeoCoordinate gc) {
		this.minLatitudeE6 = this.maxLatitudeE6 = gc.getLatitudeE6();
		this.minLongitudeE6 = this.maxLongitudeE6 = gc.getLongitudeE6();
	}

	/**
	 * Creates a new rectangle from an existing one.
	 * 
	 * @param rect
	 *            the existing rectangle
	 */
	public Rect(Rect rect) {
		this.minLatitudeE6 = rect.minLatitudeE6;
		this.maxLatitudeE6 = rect.maxLatitudeE6;
		this.minLongitudeE6 = rect.minLongitudeE6;
		this.maxLongitudeE6 = rect.maxLongitudeE6;
	}

	/**
	 * Enlarges the boundary of the Rect so that it contains the coordinate. The special case
	 * occurring at longitude +180° and -180° is not considered.
	 * 
	 * @param gc
	 *            the coordinate which should be included in the rectangle
	 */
	public void expandToInclude(GeoCoordinate gc) {
		expandToInclude(gc.getLatitudeE6(), gc.getLongitudeE6());
	}

	/**
	 * Enlarges the boundary of the Rect so that it contains the rectangle. The special case
	 * occurring at longitude +180° and -180° is not considered.
	 * 
	 * @param otherRect
	 *            the other rectangle which should be included in the rectangle
	 */
	public void expandToInclude(Rect otherRect) {
		expandToInclude(otherRect.minLatitudeE6, otherRect.minLongitudeE6);
		expandToInclude(otherRect.maxLatitudeE6, otherRect.maxLongitudeE6);
	}

	/**
	 * Enlarges the boundary of the Rect so that it contains the coordinate. The special case
	 * occurring at longitude +180° and -180° is not considered.
	 * 
	 * @param latE6
	 *            the latitude of the coordinate
	 * @param lonE6
	 *            the longitude of the coordinate
	 */
	public void expandToInclude(int latE6, int lonE6) {
		this.minLatitudeE6 = Math.min(this.minLatitudeE6, latE6);
		this.maxLatitudeE6 = Math.max(this.maxLatitudeE6, latE6);
		this.minLongitudeE6 = Math.min(this.minLongitudeE6, lonE6);
		this.maxLongitudeE6 = Math.max(this.maxLongitudeE6, lonE6);
	}

	/**
	 * Computes the center of this rectangle.
	 * 
	 * @return the center of this rectangle as new GeoCoordinate
	 */
	public GeoCoordinate center() {
		return new GeoCoordinate(minLatitudeE6
				+ (int) Math.round((maxLatitudeE6 - minLatitudeE6) / 2.0d), minLongitudeE6
				+ (int) Math.round((maxLongitudeE6 - minLongitudeE6) / 2.0d));
	}

	/**
	 * Computes the distance between this and another rectangle. The distance between
	 * overlapping rectangles is 0. Otherwise, the distance is the spherical distance between
	 * the closest points.
	 * 
	 * @param otherRect
	 *            the rectangle to compute the distance
	 * @return the distance between this rectangle and the given one
	 */
	public double distance(Rect otherRect) {
		if (overlaps(otherRect))
			return 0;

		int dLat1, dLat2;
		int dLon1, dLon2;

		if (maxLatitudeE6 < otherRect.minLatitudeE6) {
			dLat1 = maxLatitudeE6;
			dLat2 = otherRect.minLatitudeE6;
		} else if (minLatitudeE6 > otherRect.maxLatitudeE6) {
			dLat1 = minLatitudeE6;
			dLat2 = otherRect.maxLatitudeE6;
		} else {
			dLat1 = dLat2 = Math.min(maxLatitudeE6, otherRect.maxLatitudeE6);
		}

		if (maxLongitudeE6 < otherRect.minLongitudeE6) {
			dLon1 = maxLongitudeE6;
			dLon2 = otherRect.minLongitudeE6;
		} else if (minLongitudeE6 > otherRect.maxLongitudeE6) {
			dLon1 = minLongitudeE6;
			dLon2 = otherRect.maxLongitudeE6;
		} else {
			dLon1 = dLon2 = Math.min(maxLongitudeE6, otherRect.maxLongitudeE6);
		}

		return GeoCoordinate.sphericalDistance(dLon1, dLat1, dLon2, dLat2);
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + maxLatitudeE6;
		result = prime * result + maxLongitudeE6;
		result = prime * result + minLatitudeE6;
		result = prime * result + minLongitudeE6;
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Rect other = (Rect) obj;
		if (maxLatitudeE6 != other.maxLatitudeE6)
			return false;
		if (maxLongitudeE6 != other.maxLongitudeE6)
			return false;
		if (minLatitudeE6 != other.minLatitudeE6)
			return false;
		if (minLongitudeE6 != other.minLongitudeE6)
			return false;
		return true;
	}

}
