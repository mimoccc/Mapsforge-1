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
package org.mapsforge.preprocessing.util;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MaxSpeedMapper {

	private HashMap<String, Integer> mapping;
	private static Pattern mphPattern = Pattern.compile("([0-9]{1,3})\\s*(mph)\\s*");
	private static Pattern kmhPattern = Pattern.compile("([0-9]{1,3})\\s*(kmh)\\s*");

	public MaxSpeedMapper() {
		super();
		this.mapping = new HashMap<String, Integer>();

		mapping.put("DE:urban", 50);
		mapping.put("city_limits", 50);
		mapping.put("DE:living_street", 6);
		mapping.put("walk", 6);
		mapping.put("DE:speed:30", 30);
		mapping.put("DE:rural", 100);
		mapping.put("DE:motorway", 250);

	}

	/**
	 * Translates the value of an OSM maxspeed-tag to an integer value.
	 * 
	 * @param maxspeed
	 * @return an integer > 0 if value could be translated, 0 otherwise
	 */
	public int translate(String maxspeed) {
		if (maxspeed == null)
			return 0;

		int speed = 0;
		try {
			speed = Integer.parseInt(maxspeed);
		} catch (NumberFormatException e) {
			// check for mph pattern
			Matcher m = mphPattern.matcher(maxspeed);
			if (m.matches())
				return Integer.parseInt(m.group(1));

			// check for kmh pattern
			m = kmhPattern.matcher(maxspeed);
			if (m.matches())
				return Integer.parseInt(m.group(1));

			// check for other known tag value
			if (mapping.containsKey(maxspeed))
				return mapping.get(maxspeed);
		}

		return speed;
	}

}
