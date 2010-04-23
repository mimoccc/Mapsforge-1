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
import android.graphics.Path;

class PathTextContainer {
	final Paint paint;
	final Path path;
	final String text;

	/**
	 * Create a new path text container, that holds a path, a text variable and a paint for
	 * drawing.
	 * 
	 * @param path
	 *            the path
	 * @param paint
	 *            the paint
	 * @param text
	 *            the text
	 */
	PathTextContainer(Path path, Paint paint, String text) {
		this.path = path;
		this.paint = paint;
		this.text = text;
	}
}