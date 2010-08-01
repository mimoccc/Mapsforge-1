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
package org.mapsforge.core;

public class Rect {

	public final int minLon, maxLon, minLat, maxLat;

	public Rect(int minLon, int maxLon, int minLat, int maxLat) {
		this.minLon = minLon;
		this.maxLon = maxLon;
		this.minLat = minLat;
		this.maxLat = maxLat;
	}

	public boolean overlaps(Rect r) {
		return overlaps(minLon, maxLon, minLat, maxLat, r.minLon, r.maxLon, r.minLat, r.maxLat);
	}

	public static boolean overlaps(int minLon1, int maxLon1, int minLat1, int maxLat1,
			int minLon2, int maxLon2, int minLat2, int maxLat2) {
		boolean noOverlap = minLon1 > maxLon2 || minLon2 > maxLon1 || minLat1 > maxLat2
				|| minLat2 > maxLat1;
		return !noOverlap;
	}

	@Override
	public String toString() {
		return "[ (" + minLon + "," + minLat + ") (" + maxLon + "," + maxLat + ") ]";
	}

}
