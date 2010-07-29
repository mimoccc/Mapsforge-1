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

import java.io.IOException;

import org.garret.perst.PerstInputStream;
import org.garret.perst.PerstOutputStream;
import org.garret.perst.SelfSerializable;
import org.mapsforge.server.poi.PointOfInterest;

class Poi implements SelfSerializable {

	public long id;
	public double latitude;
	public double longitude;
	public String name;
	public String url;
	public String category;

	public Poi() {
	}

	public Poi(PointOfInterest poi) {
		this.id = poi.id;
		this.latitude = poi.latitude;
		this.longitude = poi.longitude;
		this.name = poi.name;
		this.url = poi.url;
		this.category = poi.category.title;
	}

	@Override
	public void pack(PerstOutputStream out) throws IOException {
		out.writeLong(this.id);
		out.writeDouble(this.latitude);
		out.writeDouble(this.longitude);
		out.writeString(this.name);
		out.writeString(this.url);
		out.writeString(this.category);
	}

	@Override
	public void unpack(PerstInputStream in) throws IOException {
		this.id = in.readLong();
		this.latitude = in.readDouble();
		this.longitude = in.readDouble();
		this.name = in.readString();
		this.url = in.readString();
		this.category = in.readString();
	}

}
