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
package org.mapsforge.directions;

import org.mapsforge.core.GeoCoordinate;
import org.mapsforge.server.routing.IEdge;
import org.mapsforge.server.routing.IVertex;

/**
 * This Class is for testing purposes only.
 * 
 * @author Eike
 */
public class DummyEdge implements IEdge {
	GeoCoordinate[] wayPoints;
	int id;
	String name, ref = "";
	boolean isMotorWayLink, isRoundabout = false;

	/**
	 * @param wayPoints
	 * @param name
	 */
	public DummyEdge(GeoCoordinate[] wayPoints, String name) {
		this.wayPoints = wayPoints;
		this.name = name;
	}

	public DummyEdge(GeoCoordinate[] wayPoints, String name, String ref,
			boolean isMotorwayLink, boolean isRoundabout) {
		this.wayPoints = wayPoints;
		this.name = name;
		this.ref = ref;
		this.isMotorWayLink = isMotorwayLink;
		this.isRoundabout = isRoundabout;
	}

	@Override
	public GeoCoordinate[] getAllWaypoints() {
		return wayPoints;
	}

	@Override
	public int getId() {
		return id;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public IVertex getSource() {
		return null;
	}

	@Override
	public IVertex getTarget() {
		return null;
	}

	@Override
	public GeoCoordinate[] getWaypoints() {
		return wayPoints;
	}

	@Override
	public int getWeight() {
		return 0;
	}

	@Override
	public String getRef() {
		return ref;
	}

	@Override
	public boolean isMotorWayLink() {
		return isMotorWayLink;
	}

	@Override
	public boolean isRoundabout() {
		return isRoundabout;
	}

}
