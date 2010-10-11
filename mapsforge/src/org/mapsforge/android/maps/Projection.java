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
 * An implementation of the Projection interface from the Google Maps library.
 */
public interface Projection {
	/**
	 * Translates a coordinate on the screen to a GeoPoint.
	 * 
	 * @param x
	 *            the pixel x coordinate on the screen.
	 * @param y
	 *            the pixel y coordinate on the screen.
	 * @return a new GeoPoint which is relative to the top-left of the MapView.
	 */
	GeoPoint fromPixels(int x, int y);
}