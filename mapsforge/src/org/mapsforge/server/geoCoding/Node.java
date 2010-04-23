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

import java.util.ArrayList;
import java.util.Map;

import org.mapsforge.server.core.geoinfo.IPoint;

/**
 * This implementation of IPoint is used in the webservice and for GeoCoding.
 * 
 * Important: for each method that starts with get, Axis2 convert the return value to XML.
 * 
 * @author bogumil & kuehn
 * @see org.mapsforge.server.core.geoinfo.IPoint
 */
public class Node implements IPoint {

	int x; // longitude
	int y; // latitude

	/*
	 * Every map (if set) must have a name value, which save the easy name. For 'Berlin' it is
	 * 'Berlin, Germany'.
	 */
	Map<String, String> attribute;

	/**
	 * Constructs an Node with specified parameters.
	 * 
	 * @param x
	 *            longitude
	 * @param y
	 *            latitude
	 */
	public Node(int x, int y) {
		this.x = x;
		this.y = y;
	}

	/**
	 * Constructs an Node with specified parameters. (backward compatible)
	 * 
	 * @param x
	 *            longitude
	 * @param y
	 *            latitude
	 */
	public Node(double x, double y) {
		this.x = (int) (x * 1000000);
		this.y = (int) (y * 1000000);
	}

	public Node(double x, double y, Map<String, String> attribute) {
		this.x = (int) (x * 1000000);
		this.y = (int) (y * 1000000);
		this.attribute = attribute;
	}

	/**
	 * Constructs an Node with specified parameters.
	 * 
	 * @param x
	 *            longitude
	 * @param y
	 *            latitude
	 * @param attribute
	 *            attributes of this node
	 */
	public Node(int x, int y, Map<String, String> attribute) {
		this.x = x;
		this.y = y;
		this.attribute = attribute;
	}

	@Override
	public String toString() {
		return attribute.get("name") + " x:" + x + " y:" + y + " " + attribute + "\n";
	}

	public int getLat() {
		return y;
	}

	public int getLon() {
		return x;
	}

	public Map<String, String> getAttributes() {
		return attribute;
	}

	/**
	 * Return all entries from the attributes Map in a ArrayList. This method is necessary for
	 * the xml conversion with axis2.
	 * 
	 * @return Arraylist with MapEntry.
	 */
	public ArrayList<OSMEntry> getWSAttributes() {
		if (attribute == null) {
			return null;
		}
		ArrayList<OSMEntry> attr = new ArrayList<OSMEntry>();
		for (Map.Entry<String, String> entry : getAttributes().entrySet()) {
			attr.add(new OSMEntry(entry.getKey(), entry.getValue()));
		}
		return attr;
	}

	@Override
	public int distanceTo(IPoint point) {
		throw new UnsupportedOperationException("TODO"); //$NON-NLS-1$ TODO
	}

	@Override
	public double latitudeDegrees() {
		throw new UnsupportedOperationException("TODO"); //$NON-NLS-1$ TODO
	}

	@Override
	public double longitudeDegrees() {
		throw new UnsupportedOperationException("TODO"); //$NON-NLS-1$ TODO
	}

	@Override
	public boolean matches(IPoint p) {
		throw new UnsupportedOperationException("TODO"); //$NON-NLS-1$ TODO
	}

	@Override
	public int distanceTo(int latitude, int longitude) {
		throw new UnsupportedOperationException("TODO"); //$NON-NLS-1$ TODO
	}
}
