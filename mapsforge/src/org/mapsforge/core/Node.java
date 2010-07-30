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

import static org.mapsforge.preprocessing.util.Constants.TAG_STOP_SIGN;
import static org.mapsforge.preprocessing.util.Constants.TAG_TRAFFIC_SIGNAL;

import org.mapsforge.preprocessing.util.GeoCoordinate;

public class Node {

	public long id;
	public double latitude;
	public double longitude;
	public String highwayTags;

	public Node() {
		super();
	}

	public Node(long id, double latitude, double longitude, String nodeTags) {
		super();
		this.id = id;
		this.latitude = latitude;
		this.longitude = longitude;
		this.highwayTags = nodeTags;
	}

	public boolean hasTrafficLight() {
		return highwayTags != null && highwayTags.contains(TAG_TRAFFIC_SIGNAL);
	}

	public boolean hasStopSign() {
		return highwayTags != null && highwayTags.contains(TAG_STOP_SIGN);
	}

	public double distance(Node other) {
		GeoCoordinate A = new GeoCoordinate(latitude, longitude);
		GeoCoordinate B = new GeoCoordinate(other.latitude, other.longitude);
		return A.distance(B);
	}
}
