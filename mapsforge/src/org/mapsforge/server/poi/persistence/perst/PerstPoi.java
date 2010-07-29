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
package org.mapsforge.server.poi.persistence.perst;

import org.garret.perst.Persistent;
import org.mapsforge.server.poi.PointOfInterest;

public class PerstPoi extends Persistent {

	public long id;
	public double latitude;
	public double longitude;
	public String name;
	public String url;
	public String category;

	public PerstPoi() {
	}

	public PerstPoi(PointOfInterest poi) {
		this.id = poi.id;
		this.latitude = poi.latitude;
		this.longitude = poi.longitude;
		this.name = poi.name;
		this.url = poi.url;
		this.category = poi.category.title;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + (int) (id ^ (id >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		PerstPoi other = (PerstPoi) obj;
		if (id != other.id)
			return false;
		return true;
	}

}
