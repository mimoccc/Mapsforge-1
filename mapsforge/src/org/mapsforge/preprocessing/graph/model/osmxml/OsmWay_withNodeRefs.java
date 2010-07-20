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
import java.util.LinkedList;

import org.mapsforge.preprocessing.model.EHighwayLevel;

public class OsmWay_withNodeRefs extends OsmElement {

	private static int DEFAULT_TAG_VALUE_ONEWAY = 0;
	private static boolean DEFAULT_TAG_VALUE_URBAN = false;
	private static String DEFAULT_TAG_VALUE_NAME = "";
	private static EHighwayLevel DEFAULT_TAG_VALUE_HIGHWAY_LEVEL = null;

	private LinkedList<Long> nodeRefs;

	public OsmWay_withNodeRefs(long id, Timestamp timestamp, String user, boolean visible) {
		super(id, timestamp, user, visible);
		this.nodeRefs = new LinkedList<Long>();
	}

	public void addNodeRef(Long id) {
		if (id != null && !nodeRefs.contains(id)) {
			nodeRefs.add(id);
		}
	}

	public LinkedList<Long> getNodeRefs() {
		return this.nodeRefs;
	}

	public int isOneway() {
		EHighwayLevel hwyLevel = getHighwayLevel();
		if (hwyLevel == EHighwayLevel.motorway || hwyLevel == EHighwayLevel.motorway_link
				|| hwyLevel == EHighwayLevel.trunk || hwyLevel == EHighwayLevel.trunk_link) {
			return 1;
		}
		String tag = getTag("oneway");
		if (tag == null) {
			return DEFAULT_TAG_VALUE_ONEWAY;
		} else if (tag.equals("true") || tag.equals("yes") || tag.equals("t")
				|| tag.equals("1")) {
			return 1;
		} else if (tag.equals("false") || tag.equals("no") || tag.equals("f")
				|| tag.equals("0")) {
			return 0;
		} else if (tag.equals("-1")) {
			return -1;
		} else {
			return DEFAULT_TAG_VALUE_ONEWAY;
		}
	}

	public EHighwayLevel getHighwayLevel() {
		String v = getTag("highway");
		if (v != null) {
			try {
				return EHighwayLevel.valueOf(v);
			} catch (IllegalArgumentException e) {

			}
		}
		return DEFAULT_TAG_VALUE_HIGHWAY_LEVEL;
	}

	public boolean isUrban() {
		return DEFAULT_TAG_VALUE_URBAN;
	}

	public String getName() {
		String v = getTag("name");
		if (v == null) {
			v = getTag("ref");
		}
		if (v == null) {
			return DEFAULT_TAG_VALUE_NAME;
		}
		return v;
	}
}
