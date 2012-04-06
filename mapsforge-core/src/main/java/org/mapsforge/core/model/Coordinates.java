/*
 * Copyright 2010, 2011, 2012 mapsforge.org
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
package org.mapsforge.core.model;

import java.util.StringTokenizer;

/**
 * A utility class to convert, parse and validate geographical coordinates.
 */
public final class Coordinates {
	/**
	 * Maximum possible latitude coordinate.
	 */
	public static final double LATITUDE_MAX = 90;

	/**
	 * Minimum possible latitude coordinate.
	 */
	public static final double LATITUDE_MIN = -LATITUDE_MAX;

	/**
	 * Maximum possible longitude coordinate.
	 */
	public static final double LONGITUDE_MAX = 180;

	/**
	 * Minimum possible longitude coordinate.
	 */
	public static final double LONGITUDE_MIN = -LONGITUDE_MAX;

	/**
	 * Conversion factor from degrees to microdegrees.
	 */
	private static final double CONVERSION_FACTOR = 1000000;

	private static final String DELIMITER = ",";

	/**
	 * Converts a coordinate from degrees to microdegrees. No validation is performed.
	 * 
	 * @param coordinate
	 *            the coordinate in degrees.
	 * @return the coordinate in microdegrees.
	 */
	public static int degreesToMicrodegrees(double coordinate) {
		return (int) (coordinate * CONVERSION_FACTOR);
	}

	/**
	 * Converts a coordinate from microdegrees to degrees. No validation is performed.
	 * 
	 * @param coordinate
	 *            the coordinate in microdegrees.
	 * @return the coordinate in degrees.
	 */
	public static double microdegreesToDegrees(int coordinate) {
		return coordinate / CONVERSION_FACTOR;
	}

	/**
	 * @param latitude
	 *            the latitude coordinate in degrees which should be validated.
	 * @throws IllegalArgumentException
	 *             if the latitude coordinate is invalid.
	 */
	public static void validateLatitude(double latitude) {
		if (latitude < LATITUDE_MIN || latitude > LATITUDE_MAX) {
			throw new IllegalArgumentException("invalid latitude: " + latitude);
		}
	}

	/**
	 * @param longitude
	 *            the longitude coordinate in degrees which should be validated.
	 * @throws IllegalArgumentException
	 *             if the longitude coordinate is invalid.
	 */
	public static void validateLongitude(double longitude) {
		if (longitude < LONGITUDE_MIN || longitude > LONGITUDE_MAX) {
			throw new IllegalArgumentException("invalid longitude: " + longitude);
		}
	}

	static double[] parseCoordinates(String coordinatesString, int numberOfCoordinates) {
		StringTokenizer stringTokenizer = new StringTokenizer(coordinatesString, DELIMITER, true);
		boolean isDelimiter = true;
		double[] coordinates = new double[numberOfCoordinates];
		int i = 0;

		while (stringTokenizer.hasMoreTokens()) {
			String token = stringTokenizer.nextToken();

			isDelimiter = !isDelimiter;
			if (isDelimiter) {
				continue;
			}

			coordinates[i++] = Double.parseDouble(token);
		}

		if (isDelimiter) {
			throw new IllegalArgumentException("invalid delimiter: " + coordinatesString);
		} else if (i != numberOfCoordinates) {
			throw new IllegalArgumentException("invalid coordinates: " + coordinatesString);
		}

		return coordinates;
	}

	private Coordinates() {
		throw new IllegalStateException();
	}
}
