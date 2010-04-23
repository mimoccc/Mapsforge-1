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
import java.util.EnumMap;
import java.util.Map;

public class Node extends AbstractPoint {

	public static enum Attribute {
		COST_TO_DESTINATION, //
		DISTANCE_TO_DESTINATION, //
	}

	public static Node newNode(int latitude, int longitude, Map<Attribute, String> map) {
		EnumMap<Attribute, String> eMap = new EnumMap<Attribute, String>(Attribute.class);
		eMap.putAll(map);
		return new Node(latitude, longitude, eMap);
	}

	protected final Map<Attribute, String> attributes;

	/** lazy hashCode initialization */
	protected volatile int hashCode;

	protected Node(int lat, int lon, EnumMap<Attribute, String> eMap) {
		super(lat, lon);

		/**
		 * make attributes unmodifiable at this point ensures faster access at "getAttributes()"
		 */
		this.attributes = Collections.unmodifiableMap(eMap);
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

	public Map<Attribute, String> getAttributes() {
		/** attributes already unmodifiable, so just returning suffices */
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

	public static final Map<Attribute, String> EMPTY_ATTRIBUTE_MAP = Collections.emptyMap();

	public static Node getNode(IPoint point) {
		if (!(point instanceof Node))
			return newNode(point.getLat(), point.getLon());
		return (Node) point;
	}
}
