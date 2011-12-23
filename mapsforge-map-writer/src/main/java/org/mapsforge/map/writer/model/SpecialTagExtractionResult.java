/*
 * Copyright 2010, 2011 mapsforge.org
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.mapsforge.map.writer.model;

/**
 * @author bross
 */
public class SpecialTagExtractionResult {

	private final String name;
	private final String ref;
	private final String housenumber;
	private final byte layer;
	private final short elevation;
	private final String type;

	/**
	 * @param name
	 *            the name
	 * @param ref
	 *            the ref
	 * @param housenumber
	 *            the housenumber
	 * @param layer
	 *            the layer
	 * @param elevation
	 *            the elevation
	 * @param type
	 *            the type
	 */
	public SpecialTagExtractionResult(String name, String ref, String housenumber, byte layer, short elevation,
			String type) {
		super();
		this.name = name;
		this.ref = ref;
		this.housenumber = housenumber;
		this.layer = layer;
		this.elevation = elevation;
		this.type = type;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * @return the ref
	 */
	public String getRef() {
		return this.ref;
	}

	/**
	 * @return the housenumber
	 */
	public String getHousenumber() {
		return this.housenumber;
	}

	/**
	 * @return the layer
	 */
	public byte getLayer() {
		return this.layer;
	}

	/**
	 * @return the elevation
	 */
	public short getElevation() {
		return this.elevation;
	}

	/**
	 * @return the type
	 */
	public String getType() {
		return this.type;
	}

}
