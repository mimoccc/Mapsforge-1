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
package org.mapsforge.directions;

import org.mapsforge.core.GeoCoordinate;

/**
 * Landmarks are buildings or other easily identified structures which can be used to enhance
 * directions
 * 
 * @author Eike Send
 */
public class Landmark {

	GeoCoordinate point;
	String name;
	String type;
	String value;

	/**
	 * Constructor method for Landmarks
	 * 
	 * @param point
	 *            Where it's at
	 * @param name
	 *            what it's called
	 * @param type
	 *            what key had the OSM tag ie: amenity
	 * @param value
	 *            what value had the OSM tag ie: post-office
	 */
	public Landmark(GeoCoordinate point, String name, String type, String value) {
		this.point = point;
		this.name = name;
		this.type = type;
		this.value = value;
	}

	/**
	 * @return the coordinates of the Landmark
	 */
	public GeoCoordinate getPoint() {
		return point;
	}

	/**
	 * @return the name of the Landmark, may be null
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the OSM tags type
	 */
	public String getType() {
		return type;
	}

	/**
	 * @return the OSM tags value
	 */
	public String getValue() {
		return value;
	}

	@Override
	public String toString() {
		return value + " " + name;
	}
}
