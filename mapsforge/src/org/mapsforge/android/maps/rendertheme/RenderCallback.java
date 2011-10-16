/*
 * Copyright 2010, 2011 mapsforge.org
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.mapsforge.android.maps.rendertheme;

import android.graphics.Bitmap;
import android.graphics.Paint;

/**
 * Callback methods which can be triggered by a {@link RenderInstruction}.
 */
public interface RenderCallback {
	/**
	 * Renders the current area with the given parameters.
	 * 
	 * @param paint
	 *            the paint to be used for rendering the area.
	 * @param level
	 *            the drawing level on which the area should be rendered.
	 */
	void addArea(Paint paint, int level);

	/**
	 * Renders the given caption for the current area.
	 * 
	 * @param caption
	 *            the text to be rendered.
	 * @param dy
	 *            vertical offset.
	 * @param paint
	 *            the paint to be used for rendering the name.
	 * @param stroke
	 *            an optional paint for the name casing (may be null).
	 */
	void addAreaCaption(String caption, float dy, Paint paint, Paint stroke);

	/**
	 * Renders the given symbol at the center position of the current area.
	 * 
	 * @param symbol
	 *            the symbol to be rendered.
	 */
	void addAreaSymbol(Bitmap symbol);

	/**
	 * Renders the given caption for the current node.
	 * 
	 * @param caption
	 *            the text to be rendered.
	 * @param dy
	 *            vertical offset.
	 * @param paint
	 *            the paint to be used for rendering the name.
	 * @param stroke
	 *            an optional paint for the name casing (may be null).
	 */
	void addNodeCaption(String caption, float dy, Paint paint, Paint stroke);

	/**
	 * Renders a circle with the given parameters at the position of the current node.
	 * 
	 * @param radius
	 *            the radius of the circle.
	 * @param fill
	 *            the paint to be used for rendering the circle.
	 * @param level
	 *            the drawing level on which the circle should be rendered.
	 */
	void addNodeCircle(float radius, Paint fill, int level);

	/**
	 * Renders the given symbol at the position of the current node.
	 * 
	 * @param symbol
	 *            the symbol to be rendered.
	 */
	void addNodeSymbol(Bitmap symbol);

	/**
	 * Renders the current way with the given parameters.
	 * 
	 * @param paint
	 *            the paint to be used for rendering the way.
	 * @param level
	 *            the drawing level on which the way should be rendered.
	 */
	void addWay(Paint paint, int level);

	/**
	 * Renders the given symbol along the current way.
	 * 
	 * @param symbol
	 *            the symbol to be rendered.
	 * @param alignCenter
	 *            true if the symbol should be centered, false otherwise.
	 * @param repeat
	 *            true if the symbol should be repeated, false otherwise.
	 */
	void addWaySymbol(Bitmap symbol, boolean alignCenter, boolean repeat);

	/**
	 * Renders the given text along the current way.
	 * 
	 * @param text
	 *            the text to be rendered.
	 * @param paint
	 *            the paint to be used for rendering the name.
	 * @param stroke
	 *            an optional paint for the name casing (may be null).
	 */
	void addWayText(String text, Paint paint, Paint stroke);
}