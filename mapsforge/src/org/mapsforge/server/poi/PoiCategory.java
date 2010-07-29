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

/**
 * This class represents a category for {@link PointOfInterest}. Every {@link PoiCategory}
 * should have a unique title so that for two {@link PoiCategory}s a and b a.equals(b) if and
 * only if a.title.equalsIgnoreCase(b.title).
 * 
 * @author weise
 * 
 */
public class PoiCategory {

	public final String title;
	public final PoiCategory parent;

	public PoiCategory(String title, PoiCategory parent) {
		this.title = title;
		this.parent = parent;
	}

	@Override
	public String toString() {
		return title + (parent == null ? "" : " < " + parent.title);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((title == null) ? 0 : title.toLowerCase().hashCode());
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
		PoiCategory other = (PoiCategory) obj;
		if (title == null) {
			if (other.title != null)
				return false;
		} else if (!title.equalsIgnoreCase(other.title))
			return false;
		return true;
	}

}
