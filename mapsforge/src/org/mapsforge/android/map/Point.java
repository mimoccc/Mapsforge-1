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

class Point implements Comparable<Point> {
	/**
	 * Calculate the distance of two Points.
	 * 
	 * @param minuend
	 *            the subtrahend.
	 * @param subtrahend
	 *            the subtrahend.
	 * @return the distance.
	 */
	public static Point substract(Point minuend, Point subtrahend) {
		return new Point(minuend.x - subtrahend.x, minuend.y - subtrahend.y);
	}

	private final int hashCode;
	private Point other;
	final float x;
	final float y;

	/**
	 * Constructs a new Point object with the given x and y coordinates.
	 * 
	 * @param x
	 *            the x coordinate of the point.
	 * @param y
	 *            the y coordinate of the point.
	 */
	Point(float x, float y) {
		this.x = x;
		this.y = y;
		this.hashCode = calculateHashCode();
	}

	@Override
	public int compareTo(Point point) {
		if (this.x > point.x) {
			return 1;
		} else if (this.x < point.x) {
			return -1;
		} else if (this.y > point.y) {
			return 1;
		} else if (this.y < point.y) {
			return -1;
		}
		return 0;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		} else if (!(obj instanceof Point)) {
			return false;
		} else {
			this.other = (Point) obj;
			if (this.x != this.other.x) {
				return false;
			}
			if (this.y != this.other.y) {
				return false;
			}
			return true;
		}
	}

	@Override
	public int hashCode() {
		return this.hashCode;
	}

	/**
	 * Calculates the hash value of this Point.
	 * 
	 * @return the hash value of this Point.
	 */
	private int calculateHashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Float.floatToIntBits(x);
		result = prime * result + Float.floatToIntBits(y);
		return result;
	}
}