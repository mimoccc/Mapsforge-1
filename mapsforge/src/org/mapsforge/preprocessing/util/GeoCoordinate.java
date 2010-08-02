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

/**
 * 
 * @author marc
 */
public class GeoCoordinate {

	public static final double EARTH_RADIUS = 6378137d;
	public static final double DEG_RAD_FACTOR = 57.29578d;

	private static final double FAC_DOUBLE_TO_INT = 1E7;
	private static final double FAC_INT_TO_DOUBLE = 1 / 1E7;

	private Latitude latitude;
	private Longitude longitude;

	public static final double FACTOR = 1.0d;

	public GeoCoordinate(Latitude lat, Longitude lon) {
		this.latitude = lat;
		this.longitude = lon;
	}

	public GeoCoordinate(double lat, double lon) throws IllegalArgumentException {
		this.latitude = new Latitude(lat);
		this.longitude = new Longitude(lon);
	}

	public GeoCoordinate(int lat, int lon) throws IllegalArgumentException {
		this.latitude = new Latitude(itod(lat));
		this.longitude = new Longitude(itod(lon));
	}

	public GeoCoordinate(GeoCoordinate other) {
		this.latitude = new Latitude(other.getLatitude().getDegree());
		this.longitude = new Longitude(other.getLongitude().getDegree());
	}

	public int getLatitudeInt() {
		return dtoi(latitude.getDegree());
	}

	public int getLongitudeInt() {
		return dtoi(longitude.getDegree());
	}

	public Latitude getLatitude() {
		return this.latitude;
	}

	public Longitude getLongitude() {
		return this.longitude;
	}

	public double getMercatorX() {
		return EARTH_RADIUS * this.longitude.getRadians();
	}

	public double getMercatorY() {
		return EARTH_RADIUS
				* java.lang.Math.log(java.lang.Math.tan(java.lang.Math.PI / 4 + 0.5
						* this.latitude.getRadians()));
	}

	public void setLatitude(Latitude lat) {
		this.latitude = lat;
	}

	public void setLongitude(Longitude lon) {
		this.longitude = lon;
	}

	public double haversineDistance(GeoCoordinate gc) {

		return 0d;
	}

	public boolean equals(GeoCoordinate c) {
		return (c != null) && (getLatitude().getDegree() == c.getLatitude().getDegree())
				&& (getLongitude().getDegree() == c.getLongitude().getDegree());
	}

	/**
	 * Calculates the distance between this and the given coordinate
	 * http://williams.best.vwh.net/avform.htm#Dist d=2*asin( sqrt( (sin((lat1-lat2)/2))^2 +
	 * cos(lat1)*cos(lat2) * (sin((lon1-lon2)/2))^2 ) )
	 * 
	 * @param other
	 * 
	 * @return
	 */
	public double distance(GeoCoordinate other) {
		if (other != null) {
			return GeoCoordinate.sphericalDistance(this, other);
		}
		return 0.0d;
	}

	// d=acos(sin(lat1)*sin(lat2)+cos(lat1)*cos(lat2)*cos(lon1-lon2))
	public static double sDistance(GeoCoordinate gc1, GeoCoordinate gc2) {

		double ir1 = Math.sin(gc1.getLatitude().getRadians())
				* Math.sin(gc2.getLatitude().getRadians());
		double ir2 = Math.cos(gc1.getLatitude().getRadians())
				* Math.cos(gc2.getLatitude().getRadians());
		double ir3 = Math
				.cos(gc1.getLongitude().getRadians() - gc2.getLongitude().getRadians());

		double d = Math.acos(ir1 + ir2 * ir3);
		return (float) (d * EARTH_RADIUS);
	}

	public static double sphericalDistance(GeoCoordinate gc1, GeoCoordinate gc2) {
		double dLat = gc1.getLatitude().getRadians() - gc2.getLatitude().getRadians();
		double dLon = gc1.getLongitude().getRadians() - gc2.getLongitude().getRadians();

		// intermediate result 1: (sin((lat1-lat2)/2))^2
		double ir1 = Math.sin(dLat / 2.0d);
		ir1 *= ir1;
		// intermediate result 3: cos(lat1)*cos(lat2)
		double ir2 = Math.cos(gc1.getLatitude().getRadians())
				* Math.cos(gc2.getLatitude().getRadians());
		// intermediate result 2: (sin((lon1-lon2)/2))^2
		double ir3 = Math.sin(dLon / 2.0d);
		ir3 *= ir3;

		return 2 * Math.asin(Math.sqrt(ir1 + ir2 * ir3)) * EARTH_RADIUS;
	}

	public static int dtoi(double d) {
		return (int) Math.rint(d * FAC_DOUBLE_TO_INT);
	}

	public static double itod(int i) {
		return FAC_INT_TO_DOUBLE * i;
	}

	public static double sphericalDistance(int lon1, int lat1, int lon2, int lat2) {
		return sphericalDistance(itod(lon1), itod(lat1), itod(lon2), itod(lat2));
	}

	public static double sphericalDistance(double lon1, double lat1, double lon2, double lat2) {
		double dLat = Math.toRadians(lat2 - lat1);
		double dLon = Math.toRadians(lon2 - lon1);
		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(lat1))
				* Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

		return c * EARTH_RADIUS;
	}

	@Override
	public String toString() {
		return "lat: " + this.latitude + ", lon: " + this.longitude;
	}
}
