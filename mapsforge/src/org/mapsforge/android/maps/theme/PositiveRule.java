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

import java.util.List;

class PositiveRule extends Rule {
	private final ClosedMatcher closedMatcher;
	private final ElementMatcher elementMatcher;
	private final AttributeMatcher keysMatcher;
	private final AttributeMatcher valuesMatcher;
	private final byte zoomMax;
	private final byte zoomMin;

	PositiveRule(ElementMatcher elementMatcher, List<String> keysList, List<String> valuesList,
			ClosedMatcher closedMatcher, byte zoomMin, byte zoomMax) {
		super();

		this.elementMatcher = elementMatcher;

		if ("*".equals(keysList.get(0))) {
			this.keysMatcher = AnyValueMatcher.getInstance();
		} else {
			this.keysMatcher = new KeyListMatcher(keysList);
		}

		if ("*".equals(valuesList.get(0))) {
			this.valuesMatcher = AnyValueMatcher.getInstance();
		} else {
			this.valuesMatcher = new ValueListMatcher(valuesList);
		}

		this.closedMatcher = closedMatcher;
		this.zoomMin = zoomMin;
		this.zoomMax = zoomMax;
	}

	@Override
	boolean matches(Element element, List<Tag> tags, byte zoomLevel, Closed closed) {
		return this.zoomMin <= zoomLevel && this.zoomMax >= zoomLevel
				&& this.elementMatcher.matches(element) && this.closedMatcher.matches(closed)
				&& this.keysMatcher.matches(tags) && this.valuesMatcher.matches(tags);
	}
}