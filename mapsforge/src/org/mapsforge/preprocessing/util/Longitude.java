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
package org.mapsforge.preprocessing.util;

import org.mapsforge.server.core.geoinfo.IPoint;

/**
 * Die Longitude (geographische Länge) liegt zwischen -180° (westlich des Nullmeridians) und
 * +180° (oestlich des Nullmeridians) Grad.
 * 
 * -180° == +180°
 * 
 * 0° hat dabei der Laengengrad, der durch die Sternwarte in Greenwich (London) verlaeuft.
 * 
 * Die Minuten und Sekunden werden hier als Nachkommastellen angegeben.
 * 
 * Beispiel: 1°30' => 1,5
 * 
 * @author marc
 */
public class Longitude {

	public static final double MAX_VALUE = 180d * IPoint.DEGREE_TO_INT_FACTOR;
	public static final double MIN_VALUE = -180d * IPoint.DEGREE_TO_INT_FACTOR;

	private double degree = 0.0d;

	public Longitude(double degree) throws IllegalArgumentException {
		verifyArgument(degree);
		this.degree = degree;
	}

	public double getDegree() {
		return degree;
	}

	public void setDegree(double degree) throws IllegalArgumentException {
		verifyArgument(degree);
		this.degree = degree;
	}

	public double getRadians() {
		return this.degree / GeoCoordinate.DEG_RAD_FACTOR;
	}

	private void verifyArgument(double val) throws IllegalArgumentException {
		if (val > MAX_VALUE || val < MIN_VALUE)
			throw new IllegalArgumentException("Illegal value: " + val
					+ ". The value must be between -180 and 180");
	}

	@Override
	public String toString() {
		return "" + this.degree;
	}
}
