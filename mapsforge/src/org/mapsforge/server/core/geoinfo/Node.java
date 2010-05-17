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
package org.mapsforge.server.core.geoinfo;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Node extends AbstractPoint {

	public static Node newNode(int latitude, int longitude, Map<String, String> map) {
		HashMap<String, String> hMap = new HashMap<String, String>();
		hMap.putAll(map);
		return new Node(latitude, longitude, hMap);
	}

	protected final HashMap<String, String> attributes;
	
	/* have your id ready, you just might need it (This is the internal_id which can be mapped to the OSM id)*/
	protected int id;
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	/** lazy hashCode initialization */
	protected volatile int hashCode;

	protected Node(int lat, int lon, HashMap<String, String> hMap) {
		super(lat, lon);
		this.attributes = hMap;
	}

	@Override
	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof Node))
			return false;
		Node n = (Node) o;
		return this.lat == n.lat && this.lon == n.lon && this.attributes.equals(n.attributes);
	}

	public HashMap<String, String> getAttributes() {
		//this.attributes.put("Node.java Key", "Node.java Value");
		return this.attributes;
	}

	@Override
	public int hashCode() {
		int result = this.hashCode;
		if (result == 0) {
			result = 17;
			result = 32 * result + super.hashCode();
			result = 32 * result + this.attributes.hashCode();
			this.hashCode = result;
		}
		return result;
	}

	public static Node newNode(int lat, int lon) {
		return newNode(lat, lon, EMPTY_ATTRIBUTE_MAP);
	}

	public static final Map<String, String> EMPTY_ATTRIBUTE_MAP = Collections.emptyMap();

	public static Node getNode(IPoint point) {
		if (!(point instanceof Node))
			return newNode(point.getLat(), point.getLon());
		return (Node) point;
	}
}
