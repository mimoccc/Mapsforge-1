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

final class AnyValueMatcher implements AttributeMatcher {
	private static final AnyValueMatcher INSTANCE = new AnyValueMatcher();

	static AnyValueMatcher getInstance() {
		return INSTANCE;
	}

	/**
	 * Private constructor prevents instantiation from other classes.
	 */
	private AnyValueMatcher() {
		// do nothing
	}

	@Override
	public boolean matches(List<Tag> tags) {
		return true;
	}
}