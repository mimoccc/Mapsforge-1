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

import org.mapsforge.preprocessing.map.osmosis.TileData.TDWay;

import com.vividsolutions.jts.geom.Geometry;

/**
 * A simple wrapper class which holds ways as JTS objects.
 * 
 * @author sahin
 * 
 */
public class JtsWayBlock {
	Geometry jtsWay;
	List<Geometry> jtsInnerWays;

	TDWay way;
	List<TDWay> innerWays;

	JtsWayBlock(Geometry jtsWay, List<Geometry> jtsInnerWays, TDWay way, List<TDWay> innerWays) {
		this.jtsWay = jtsWay;
		this.jtsInnerWays = jtsInnerWays;
		this.way = way;
		this.innerWays = innerWays;
	}

	/**
	 * Returns the outer way of this way block.
	 * 
	 * @return The Geometry object which represents this way.
	 */
	public Geometry getJtsWay() {
		return jtsWay;
	}

	/**
	 * Returns a list of inner ways of this block.
	 * 
	 * @return A list of Geometry objects which represents the inner ways.
	 */
	public List<Geometry> getJtsInnerways() {
		return jtsInnerWays;
	}

	/**
	 * Returns the TDWay which is associated with this JTS object.
	 * 
	 * @return The TDWay which is associated with this JTS object.
	 */
	public TDWay getWay() {
		return way;
	}

	/**
	 * Returns a list of inner ways which are associated with this JTS object.
	 * 
	 * @return A list of inner ways which are associated with this JTS object.
	 */
	public List<TDWay> getInnerWays() {
		return innerWays;
	}

}
