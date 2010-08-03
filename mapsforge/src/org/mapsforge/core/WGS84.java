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
package org.mapsforge.core;

/**
 * This class provides methods and constants for dealing with distances on earth using the World
 * Geodatic System 1984
 * 
 * @author Eike
 */
public class WGS84 {
	public static final double EQUATORIALRADIUS = 6378137.0;
	public static final double POLARRADIUS = 6356752.3142;
	public static final double INVERSEFLATTENING = 298.257223563;

	/**
	 * Calculate the spherical distance between two GeoCoordinates in meters using the Haversine
	 * formula
	 * 
	 * This calculation is done using the assumption, that the earth is a sphere, it is not
	 * though. If you need a higher precision and can afford a longer execution time you might
	 * want to use vincentyDistance
	 * 
	 * @param gc1
	 *            first GeoCoordinate
	 * @param gc2
	 *            second GeoCoordinate
	 * @return distance in meters as a double
	 * @throws IllegalArgumentException
	 *             if one of the arguments is null
	 */
	public static double sphericalDistance(GeoCoordinate gc1, GeoCoordinate gc2)
			throws IllegalArgumentException {
		if (gc1 == null || gc2 == null)
			throw new IllegalArgumentException(
					"The GeoCoordinates for distance calculations may not be null.");
		double dLat = Math.toRadians(gc1.getLatitude()) - Math.toRadians(gc2.getLatitude());
		double dLon = Math.toRadians(gc1.getLongitude()) - Math.toRadians(gc2.getLongitude());

		// intermediate result 1: (sin((lat1-lat2)/2))^2
		double ir1 = Math.sin(dLat / 2.0d);
		ir1 *= ir1;
		// intermediate result 3: cos(lat1)*cos(lat2)
		double ir2 = Math.cos(Math.toRadians(gc1.getLatitude()))
				* Math.cos(Math.toRadians(gc2.getLatitude()));
		// intermediate result 2: (sin((lon1-lon2)/2))^2
		double ir3 = Math.sin(dLon / 2.0d);
		ir3 *= ir3;

		return 2 * Math.asin(Math.sqrt(ir1 + ir2 * ir3)) * EQUATORIALRADIUS;
	}

	/**
	 * Calculates geodetic distance between two GeoCoordinates using Vincenty inverse formula
	 * for ellipsoids. This is very accurate but consumes more resources and time than the
	 * sphericalDistance method
	 * 
	 * Adaptation of Chriss Veness' JavaScript Code on
	 * http://www.movable-type.co.uk/scripts/latlong-vincenty.html
	 * 
	 * Paper: Vincenty inverse formula - T Vincenty, "Direct and Inverse Solutions of Geodesics
	 * on the Ellipsoid with application of nested equations", Survey Review, vol XXII no 176,
	 * 1975 (http://www.ngs.noaa.gov/PUBS_LIB/inverse.pdf)
	 * 
	 * @param gc1
	 *            first GeoCoordinate
	 * @param gc2
	 *            second GeoCoordinate
	 * 
	 * @return distance in meters between points as a double
	 */
	public static double vincentyDistance(GeoCoordinate gc1, GeoCoordinate gc2) {
		double f = 1 / INVERSEFLATTENING;
		double L = Math.toRadians(gc2.getLongitude() - gc1.getLongitude());
		double U1 = Math.atan((1 - f) * Math.tan(Math.toRadians(gc1.getLatitude())));
		double U2 = Math.atan((1 - f) * Math.tan(Math.toRadians(gc2.getLatitude())));
		double sinU1 = Math.sin(U1), cosU1 = Math.cos(U1);
		double sinU2 = Math.sin(U2), cosU2 = Math.cos(U2);

		double lambda = L, lambdaP, iterLimit = 100;

		double cosSqAlpha = 0, sinSigma = 0, cosSigma = 0, cos2SigmaM = 0, sigma = 0, sinLambda = 0, sinAlpha = 0, cosLambda = 0;
		do {
			sinLambda = Math.sin(lambda);
			cosLambda = Math.cos(lambda);
			sinSigma = Math.sqrt((cosU2 * sinLambda) * (cosU2 * sinLambda)
					+ (cosU1 * sinU2 - sinU1 * cosU2 * cosLambda)
					* (cosU1 * sinU2 - sinU1 * cosU2 * cosLambda));
			if (sinSigma == 0)
				return 0; // co-incident points
			cosSigma = sinU1 * sinU2 + cosU1 * cosU2 * cosLambda;
			sigma = Math.atan2(sinSigma, cosSigma);
			sinAlpha = cosU1 * cosU2 * sinLambda / sinSigma;
			cosSqAlpha = 1 - sinAlpha * sinAlpha;
			if (cosSqAlpha != 0) {
				cos2SigmaM = cosSigma - 2 * sinU1 * sinU2 / cosSqAlpha;
			} else {
				cos2SigmaM = 0;
			}
			double C = f / 16 * cosSqAlpha * (4 + f * (4 - 3 * cosSqAlpha));
			lambdaP = lambda;
			lambda = L
					+ (1 - C)
					* f
					* sinAlpha
					* (sigma + C * sinSigma
							* (cos2SigmaM + C * cosSigma * (-1 + 2 * cos2SigmaM * cos2SigmaM)));
		} while (Math.abs(lambda - lambdaP) > 1e-12 && --iterLimit > 0);

		if (iterLimit == 0)
			return 0; // formula failed to converge

		double uSq = cosSqAlpha * (Math.pow(EQUATORIALRADIUS, 2) - Math.pow(POLARRADIUS, 2))
				/ Math.pow(POLARRADIUS, 2);
		double A = 1 + uSq / 16384 * (4096 + uSq * (-768 + uSq * (320 - 175 * uSq)));
		double B = uSq / 1024 * (256 + uSq * (-128 + uSq * (74 - 47 * uSq)));
		double deltaSigma = B
				* sinSigma
				* (cos2SigmaM + B
						/ 4
						* (cosSigma * (-1 + 2 * cos2SigmaM * cos2SigmaM) - B / 6 * cos2SigmaM
								* (-3 + 4 * sinSigma * sinSigma)
								* (-3 + 4 * cos2SigmaM * cos2SigmaM)));
		double s = POLARRADIUS * A * (sigma - deltaSigma);

		return s;
	}
}
