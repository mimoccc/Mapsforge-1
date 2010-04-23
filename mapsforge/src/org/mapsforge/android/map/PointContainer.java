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

import android.graphics.Paint;

class PointContainer {
	final Paint paint;
	final String text;
	final float x;
	final float y;

	/**
	 * Create a new point container, that holds the x-y coordinates of a point, a text variable
	 * and a paint for drawing.
	 * 
	 * @param text
	 *            the text of the point.
	 * @param x
	 *            the x coordinate of the point.
	 * @param y
	 *            the y coordinate of the point.
	 * @param paint
	 *            the paint for the point.
	 */
	PointContainer(String text, float x, float y, Paint paint) {
		this.text = text;
		this.x = x;
		this.y = y;
		this.paint = paint;
	}
}