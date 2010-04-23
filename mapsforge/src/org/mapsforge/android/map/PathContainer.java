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

class PathContainer {
	final Paint paint;
	final Path path;

	/**
	 * Create a new path container, that holds a path and a paint for drawing.
	 * 
	 * @param path
	 *            the path
	 * @param paint
	 *            the paint
	 */
	PathContainer(Path path, Paint paint) {
		this.path = path;
		this.paint = paint;
	}
}