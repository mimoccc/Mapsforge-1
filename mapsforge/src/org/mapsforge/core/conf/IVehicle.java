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

public interface IVehicle {

	int getAvgSpeed();

	/**
	 * Maximal speed in m/s of the transportation type. This is used by the heuristic estimation
	 * function of the search algorithm to calculate an admissible estimation of the cost of the
	 * remaining path.
	 * 
	 * @return maximum speed of transportation medium
	 */
	int getMaxSpeed();

	/**
	 * The name of the vehicle / transportation type (enums already support this method by
	 * inheritance)
	 * 
	 * @return the name of the transportation type
	 */
	String name();

	/**
	 * Determines the cost of a fixed length distance to be in the unit of seconds.
	 * 
	 * @param distance
	 *            the distance in decimeters
	 * @return the cost in seconds
	 */
	int getCost(int distance);

	boolean isValidHighwayLevel(String level);

}
