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
package org.mapsforge.server.poi;

import org.mapsforge.android.map.GeoPoint;

/**
 * This class represents a point of interest. Every poi should be uniquely identifiable by its
 * id so that for two pois a and b a.equals(b) if and only if a.id == b.id.
 * 
 * @author weise
 * 
 */
public class PointOfInterest {

	public final long id;
	public double latitude;
	public double longitude;
	public String name;
	public String url;
	public PoiCategory category;
	public GeoPoint geoPoint;

	public PointOfInterest(Long id, Double latitude, Double longitude, String name, String url,
			PoiCategory category) {
		super();
		this.id = id;
		this.latitude = latitude;
		this.longitude = longitude;
		this.name = name;
		this.url = url;
		this.category = category;
		this.geoPoint = new GeoPoint(latitude, longitude);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(category.toString()).append(": ").append("name=").append(name).append(
				' ').append("url=").append(url).append(' ').append("lat=").append(latitude)
				.append(" lng=").append(longitude);
		return builder.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (id ^ (id >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PointOfInterest other = (PointOfInterest) obj;
		if (id != other.id)
			return false;
		return true;
	}

}
