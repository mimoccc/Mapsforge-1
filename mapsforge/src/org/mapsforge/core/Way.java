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

import static org.mapsforge.preprocessing.util.Constants.TAG_HIGHWAY;
import static org.mapsforge.preprocessing.util.Constants.TAG_MAXSPEED;
import static org.mapsforge.preprocessing.util.Constants.TAG_ONEWAY;
import static org.mapsforge.preprocessing.util.Constants.TAG_STREETNAME;

import java.util.Vector;

import org.mapsforge.preprocessing.model.EHighwayLevel;
import org.mapsforge.preprocessing.util.HighwayLevelExtractor;

public class Way {
	public short oneway = 0;
	public String maxspeed = null;
	public EHighwayLevel level;

	public String streetname;

	public Vector<WayNode> wayNodes = new Vector<WayNode>();

	public void addWayNode(WayNode w) {
		wayNodes.add(w);
	}

	public void setKnownTag(String tagname, String value) {
		if (tagname.equalsIgnoreCase(TAG_HIGHWAY)) {
			value = value.trim();
			level = HighwayLevelExtractor.getLevel(value);

			// motorways and trunks are oneway!!!
			if (level == EHighwayLevel.motorway || level == EHighwayLevel.motorway_link
					|| level == EHighwayLevel.trunk || level == EHighwayLevel.trunk_link) {

				oneway = 1;
			}
		} else if (tagname.equalsIgnoreCase(TAG_MAXSPEED)) {
			maxspeed = value.trim();
		} else if (tagname.equalsIgnoreCase(TAG_ONEWAY)) {
			if ("true".equalsIgnoreCase(value.trim()) || "yes".equalsIgnoreCase(value.trim())
					|| "1".equals(value.trim()))
				oneway = 1;
			else if ("-1".equalsIgnoreCase(value.trim()))
				oneway = -1;
		} else if (tagname.equalsIgnoreCase(TAG_STREETNAME)) {
			this.streetname = value.trim();
		}
	}
}
