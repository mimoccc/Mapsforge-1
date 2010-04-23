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

import android.graphics.Bitmap;

class SymbolContainer {
	final Bitmap symbol;
	final float x;
	final float y;

	/**
	 * Create a new symbol container, that holds the x-y coordinates and a symbol for drawing.
	 * 
	 * @param symbol
	 *            the symbol to render at the point
	 * @param x
	 *            the x coordinate of the point.
	 * @param y
	 *            the y coordinate of the point.
	 */
	SymbolContainer(Bitmap symbol, float x, float y) {
		this.symbol = symbol;
		this.x = x;
		this.y = y;
	}
}