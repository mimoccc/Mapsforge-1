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
package org.mapsforge.preprocessing.graph.model.osmxml;

import java.sql.Timestamp;

import org.mapsforge.preprocessing.util.GeoCoordinate;

/**
 * @author Frank Viernau
 */
public class OsmNode extends OsmElement {

	private final double longitude, latitude;

	public OsmNode(long id, double longitude, double latitude) {
		super(id);
		this.longitude = longitude;
		this.latitude = latitude;
	}

	public OsmNode(long id, Timestamp timestamp, String user, boolean visible,
			double longitude, double latitude) {
		super(id, timestamp, user, visible);
		this.longitude = longitude;
		this.latitude = latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public double getLatitude() {
		return latitude;
	}

	public double distance(OsmNode other) {
		GeoCoordinate A = new GeoCoordinate(latitude, longitude);
		GeoCoordinate B = new GeoCoordinate(other.latitude, other.longitude);
		return A.distance(B);
	}
}
