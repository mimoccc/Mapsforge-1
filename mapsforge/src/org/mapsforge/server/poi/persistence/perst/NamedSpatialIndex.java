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
import org.garret.perst.SpatialIndexR2;

class NamedSpatialIndex extends Persistent {
	public String name;
	public SpatialIndexR2<PerstPoi> index;

	public NamedSpatialIndex() {
	}

	public NamedSpatialIndex(String name, SpatialIndexR2<PerstPoi> index) {
		super();
		this.name = name;
		this.index = index;
	}
}
