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

class CohenSutherlandClipping {
	private static final int BOTTOM = 4;
	private static int code;
	private static final int LEFT = 1;
	private static final int RIGHT = 2;
	private static final int TOP = 8;

	/**
	 * Sets the correct flags in the bitmap that represents the position of the point.
	 */
	private static int ComputeOutCode(long x, long y, long left2, long bottom2, long right2,
			long top2) {
		code = 0;
		if (y > top2) {
			code = TOP;
		} else if (y < bottom2) {
			code = BOTTOM;
		}
		if (x > right2) {
			return code | RIGHT;
		} else if (x < left2) {
			return code | LEFT;
		} else {
			return code;
		}
	}

	/**
	 * Checks, if a part of a line lies within a rectangular area.
	 * 
	 * @param x1
	 *            first x coordinate of the line
	 * @param y1
	 *            first y coordinate of the line
	 * @param x2
	 *            second x coordinate of the line
	 * @param y2
	 *            second y coordinate of the line
	 * @param left
	 *            left coordinate of the rectangle
	 * @param bottom
	 *            bottom coordinate of the rectangle
	 * @param right
	 *            right coordinate of the rectangle
	 * @param top
	 *            top coordinate of the rectangle
	 * @return true if the line is in the area, false otherwise.
	 */
	static boolean isLineInRectangle(long x1, long y1, long x2, long y2, long left,
			long bottom, long right, long top) {
		long x1new = x1;
		long y1new = y1;
		long x2new = x2;
		long y2new = y2;
		int outcodeOut;
		int outcode0 = ComputeOutCode(x1new, y1new, left, bottom, right, top);
		int outcode1 = ComputeOutCode(x2new, y2new, left, bottom, right, top);

		while (true) {
			if ((outcode0 | outcode1) == 0) {
				return true;
			} else if ((outcode0 & outcode1) > 0) {
				return false;
			} else {
				long x = 0, y = 0;
				outcodeOut = outcode0 != 0 ? outcode0 : outcode1;
				if ((outcodeOut & TOP) > 0) {
					x = x1new + (x2new - x1new) * (top - y1new) / (y2new - y1new);
					y = top;
				} else if ((outcodeOut & BOTTOM) > 0) {
					x = x1new + (x2new - x1new) * (bottom - y1new) / (y2new - y1new);
					y = bottom;
				} else if ((outcodeOut & RIGHT) > 0) {
					y = y1new + (y2new - y1new) * (right - x1new) / (x2new - x1new);
					x = right;
				} else if ((outcodeOut & LEFT) > 0) {
					y = y1new + (y2new - y1new) * (left - x1new) / (x2new - x1new);
					x = left;
				}
				if (outcodeOut == outcode0) {
					x1new = x;
					y1new = y;
					outcode0 = ComputeOutCode(x1new, y1new, left, bottom, right, top);
				} else {
					x2new = x;
					y2new = y;
					outcode1 = ComputeOutCode(x2new, y2new, left, bottom, right, top);
				}
			}
		}
	}
}