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
 * Class that holds two coordinates which define a rectangular area.
 */
class Rect {
	final long bottom;
	final long left;
	final long right;
	final long top;

	Rect(long left, long top, long right, long bottom) {
		this.left = left;
		this.top = top;
		this.right = right;
		this.bottom = bottom;
	}

	/**
	 * Returns true, if the given GeoPoint is inside this rectangle, false otherwise.
	 * 
	 * @param geoPoint
	 *            the GeoPoint to be checked.
	 * @return true if the GeoPoint is inside, false otherwise.
	 */
	boolean contains(GeoPoint geoPoint) {
		return (geoPoint.getLatitudeE6() <= this.top && geoPoint.getLatitudeE6() >= this.bottom
				&& geoPoint.getLongitudeE6() >= this.left && geoPoint.getLongitudeE6() <= this.right);
	}

	/**
	 * Returns the center coordinates of this rectangle as a GeoPoint object.
	 * 
	 * @return the center coordinates of this rectangle.
	 */
	GeoPoint getCenter() {
		return new GeoPoint((int) ((this.top + this.bottom) >> 1),
				(int) ((this.left + this.right) >> 1));
	}

	/**
	 * Returns true, if left >= right or top <= bottom.
	 * 
	 * @return true if this rectangle is empty, false otherwise.
	 */
	boolean isEmpty() {
		return this.left >= this.right || this.top <= this.bottom;
	}
}