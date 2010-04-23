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
package org.mapsforge.server.geoCoding;

import java.util.Map;

/**
 * OSMEntry is build for the xml-conversion. It is used only in Node.
 * 
 * For the export with the axis-archiver everything must be a implementation, not only a
 * interface-type.
 */
public class OSMEntry implements Map.Entry<String, String> {

	String key;
	String value;

	public OSMEntry(String k, String v) {
		this.key = k;
		this.value = v;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getValue() {
		return value;
	}

	public String setValue(String value) {
		this.value = value;
		return "";
	}

}
