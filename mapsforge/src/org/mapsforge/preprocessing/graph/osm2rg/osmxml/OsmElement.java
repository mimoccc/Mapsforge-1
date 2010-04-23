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
package org.mapsforge.preprocessing.graph.osm2rg.osmxml;

import java.sql.Timestamp;
import java.util.HashMap;

/**
 * @author Frank Viernau
 * 
 *         Subclass for all available osm xml elements. Tag parsing can be done by additional
 *         subclass methods.
 */
public class OsmElement {

	private final long id;
	private final Timestamp timestamp;
	private final String user;
	private final boolean visible;
	private final HashMap<String, String> tags;

	public OsmElement(long id, Timestamp timestamp, String user, boolean visible) {
		this.id = id;
		this.timestamp = timestamp;
		this.user = user;
		this.visible = visible;
		this.tags = new HashMap<String, String>();
	}

	public long getId() {
		return id;
	}

	public Timestamp getTimestamp() {
		return timestamp;
	}

	public String getUser() {
		return user;
	}

	public boolean isVisible() {
		return visible;
	}

	public void setTag(String key, String value) {
		if (key != null && value != null) {
			this.tags.put(key, value);
		}
	}

	public String getTag(String key) {
		return tags.get(key);
	}

	public HashMap<String, String> getTags() {
		return tags;
	}

	protected static boolean parseBooleanTag(String tag, boolean defaultValue) {
		if (tag == null)
			return defaultValue;
		if (defaultValue) {
			return !(tag.equals("false") || tag.equals("no") || tag.equals("f") || tag
					.equals("0"));
		}
		return (tag.equals("true") || tag.equals("yes") || tag.equals("t") || tag.equals("1"));

	}
}
