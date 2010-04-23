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
package org.mapsforge.android.routecontroller;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * QueryParameters save the parameters for the GeoCoding look up.
 * 
 * @author kuehnf & bogumil
 */
public class QueryParameters {

	private String searchString;
	private Point position;

	// private Rect aus Android visibleOnScreen;

	public QueryParameters(String searchString, Point position) {
		try {
			this.searchString = URLEncoder.encode(searchString, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			this.searchString = searchString;
		}
		this.position = position;
	}

	public String getSearchString() {
		return searchString;
	}

	public Point getPosition() {
		return position;
	}

}
