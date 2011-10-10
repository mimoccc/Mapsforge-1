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
package org.mapsforge.preprocessing.map.osmosis;

import java.util.List;

/**
 * Class to store a WayDataBlock. Each WayDataBlock can store one way and a list of corrospndenting
 * innerways. So it is possible to store simple ways and simple polygons which have zero innerways or
 * multipolyogns with serval innerways.
 * 
 * @author sahin
 * 
 */
public class WayDataBlock {
	List<Integer> outerWay;
	List<List<Integer>> innerWays;

	WayDataBlock(List<Integer> outerWay, List<List<Integer>> innerWays) {
		this.outerWay = outerWay;
		this.innerWays = innerWays;
	}

	public List<Integer> getOuterWay() {
		return outerWay;

	}

	public List<List<Integer>> getInnerWays() {
		return innerWays;
	}

}