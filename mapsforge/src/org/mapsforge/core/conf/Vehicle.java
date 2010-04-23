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
package org.mapsforge.core.conf;

import java.util.HashSet;

public enum Vehicle implements IVehicle {
	STANDARD_CAR__SIMPLE_HEURISTIC(60, 130, new String[] { "living_street", "residential",
			"primary", "secondary", "tertiary", "motorway_link", "motorway", "primary_link",
			"road", "trunk", "trunk_link", "service" }), STANDARD_CYCLIST__SIMPLE_HEURISTIC(10,
			15, new String[] { "living_street", "residential", "primary", "secondary",
					"tertiary", "primary_link", "road", "service", "cycleway", "path",
					"pedestrian", "footway" }), STANDARD_PEDESTRIAN__SIMPLE_HEURISTIC(4, 6,
			new String[] { "living_street", "residential", "primary", "secondary", "tertiary",
					"primary_link", "road", "service", "cycleway", "path", "pedestrian",
					"footway" });

	private final int avgSpeed;
	private final int maxSpeed;
	private final HashSet<String> validHighwayLevels = new HashSet<String>();

	private Vehicle(int avgSpeed, int maxSpeed, String[] validHighwayLevels) {
		this.avgSpeed = avgSpeed;
		this.maxSpeed = maxSpeed;
		for (String level : validHighwayLevels) {
			this.validHighwayLevels.add(level);
		}
	}

	@Override
	public int getAvgSpeed() {
		return this.avgSpeed;
	}

	@Override
	public int getCost(int distance) {
		return (int) (distance / (this.avgSpeed / 3600f));
	}

	@Override
	public int getMaxSpeed() {
		return this.maxSpeed;
	}

	@Override
	public boolean isValidHighwayLevel(String level) {
		return validHighwayLevels.contains(level);
	}

}
