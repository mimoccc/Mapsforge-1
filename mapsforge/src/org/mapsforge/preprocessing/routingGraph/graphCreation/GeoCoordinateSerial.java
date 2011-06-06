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
package org.mapsforge.preprocessing.routingGraph.graphCreation;

import java.io.Serializable;

import org.mapsforge.core.GeoCoordinate;

/**
 * Serializable Geocoordinate class
 * 
 * @author rob
 * 
 */
public class GeoCoordinateSerial extends GeoCoordinate implements Serializable {

	private static final long serialVersionUID = 1L;

	GeoCoordinateSerial(double latitude, double longitude) throws IllegalArgumentException {
		super(latitude, longitude);
		// TODO Auto-generated constructor stub
	}

	GeoCoordinateSerial(int latitudeE6, int longitudeE6) throws IllegalArgumentException {
		super(latitudeE6, longitudeE6);
		// TODO Auto-generated constructor stub
	}

	GeoCoordinateSerial(String wellKnownText) {
		super(wellKnownText);
		// TODO Auto-generated constructor stub
	}

}
