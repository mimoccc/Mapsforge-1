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
package org.mapsforge.android.maps;

/**
 * Creates an object for the sweep line algorithm for rectangles, that stores the bounding box
 * as a rectangle and a reference on the connected object.
 * 
 * @param <E>
 *            references an object, which should be checked with the sweep line algorithm
 */
class Rectangle<E> {
	android.graphics.Rect rect;
	E value;

	Rectangle(E value, android.graphics.Rect rect) {
		this.value = value;
		this.rect = rect;

	}
}