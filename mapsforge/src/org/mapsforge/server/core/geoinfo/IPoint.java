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
package org.mapsforge.server.core.geoinfo;

public interface IPoint {

	static final double DEGREE_TO_INT_FACTOR = 1.0E6;
	static final int MAX_LAT = (int) (180 * DEGREE_TO_INT_FACTOR);
	static final int MAX_LON = (int) (90 * DEGREE_TO_INT_FACTOR);

	int distanceTo(int latitude, int longitude);

	int distanceTo(IPoint point);

	int getLat();

	int getLon();

	double latitudeDegrees();

	double longitudeDegrees();

	/**
	 * Determines if two points have identical coordinates, under the condition of all Points at
	 * the poles representing the same point.
	 * <p/>
	 * This method is <b>not identical</b> to the <i>equals()</i> method, since it can work with
	 * <b>different IPoint implementations</b>.
	 * 
	 * @param p
	 *            the other IPoint implementing object
	 * @return true if they match, false if not.
	 */
	boolean matches(IPoint p);

	/**
	 * Returns this IPoint object as a String. The canonical form is defined as: "("+ latitude
	 * +", "+ longitude +")" with latitude and longitude as <b>degree values</b>.
	 * 
	 * @return String representing this IPoint.
	 */
	@Override
	String toString();
}
