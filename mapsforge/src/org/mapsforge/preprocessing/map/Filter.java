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
package org.mapsforge.preprocessing.map;

import java.util.TreeMap;

public class Filter {
	static TreeMap<String, Byte> getNodeFilter() {
		TreeMap<String, Byte> filter = new TreeMap<String, Byte>();
		filter.put("highway=bus_stop", Byte.valueOf((byte) 16));
		filter.put("highway=traffic_signals", Byte.valueOf((byte) 17));
		return filter;
	}

	static TreeMap<String, Byte> getWayFilter() {
		TreeMap<String, Byte> filter = new TreeMap<String, Byte>();
		filter.put("highway=residential", Byte.valueOf((byte) 14));
		filter.put("highway=footway", Byte.valueOf((byte) 15));
		return filter;
	}
}
