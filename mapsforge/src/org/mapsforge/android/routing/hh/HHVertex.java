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
package org.mapsforge.android.routing.hh;

public class HHVertex {

	int neighborhood;
	int id, idPrevLvl, idNextLvl, idZeroLvl;
	byte lvl;
	int longitude, latitude;
	int internalEdgeStartIdx, externalEdgeStartIdx;
	short numInternalEdges, numExternalEdges;

	HHVertex() {
		// empty constructor suffices since
		// this class will be object pooled
	}

	public int getId() {
		return id;
	}

	public int getLatitudeE6() {
		return latitude;
	}

	public int getLongitudeE6() {
		return longitude;
	}

	public short getOutboundDegree() {
		return (short) (numInternalEdges + numExternalEdges);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(HHVertex.class.getName() + " (\n");
		sb.append("  neighborhood = " + neighborhood + "\n");
		sb.append("  id = " + id + "\n");
		sb.append("  idPrevLvl = " + idPrevLvl + "\n");
		sb.append("  idNextLvl = " + idNextLvl + "\n");
		sb.append("  idZeroLvl = " + idZeroLvl + "\n");
		sb.append("  lvl = " + lvl + "\n");
		sb.append("  longitude = " + longitude + "\n");
		sb.append("  latitude = " + latitude + "\n");
		sb.append(")");
		return sb.toString();
	}
}
