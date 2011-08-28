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
package org.mapsforge.android.maps.theme;

/**
 * A tag represents a <code>key-value</code> string.
 */
public class Tag {
	private static final char KEY_VALUE_SEPARATOR = '=';

	final String key;
	final String value;

	/**
	 * Constructs a new tag from the given string.
	 * 
	 * @param tag
	 *            the textual representation of the tag.
	 */
	public Tag(String tag) {
		int splitPosition = tag.indexOf(KEY_VALUE_SEPARATOR);
		this.key = tag.substring(0, splitPosition);
		this.value = tag.substring(splitPosition + 1);
	}

	/**
	 * Constructs a new tag with the given key and value.
	 * 
	 * @param key
	 *            the key of the tag.
	 * @param value
	 *            the value of the tag.
	 */
	public Tag(String key, String value) {
		this.key = key;
		this.value = value;
	}
}