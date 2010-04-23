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
package org.mapsforge.preprocessing.routing.highwayHierarchies.util.geo;

import java.io.Serializable;

/**
 * @author Frank Viernau
 */
public class CarthesianPoint implements Serializable {

	private static final long serialVersionUID = 4755903323170742783L;

	public int x, y;

	public CarthesianPoint(int x, int y) {
		this.x = x;
		this.y = y;
	}

	public double distance(CarthesianPoint other) {
		return Math.sqrt((((double) (x - other.x)) * ((double) (x - other.x)))
				+ (((double) (y - other.y)) * ((double) (y - other.y))));
	}

	@Override
	public String toString() {
		return "(" + x + ", " + y + ")";
	}
}
