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
package org.mapsforge.core.tests;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.mapsforge.core.MercatorProjection;

/**
 * Test the MercatorProjection Class
 * 
 * @author Eike
 */
public class MercatorProjectionTest {

	/**
	 * The hardcoded values have been calculated using the proj4js lib at http://proj4js.org/
	 */
	@Test
	public void testLongitudeToMetersX() {
		assertTrue(java.lang.Math.abs(MercatorProjection.longitudeToMetersX(0)) < 0.01);
		assertTrue(java.lang.Math
				.abs(MercatorProjection.longitudeToMetersX(45) - 5009377.085697311) < 0.01);
		assertTrue(java.lang.Math.abs(MercatorProjection.longitudeToMetersX(180) - 20037508.34) < 0.01);
		assertTrue(java.lang.Math
				.abs(MercatorProjection.longitudeToMetersX(8.983152841195215) - 1000000.0) < 0.01);
	}

	/**
	 * The hardcoded values have been calculated using the proj4js lib at http://proj4js.org/
	 */
	@Test
	public void testLatitudeToMetersY() {
		assertTrue(java.lang.Math.abs(MercatorProjection.latitudeToMetersY(0)) < 0.01);
		assertTrue(java.lang.Math
				.abs(MercatorProjection.latitudeToMetersY(45) - 5621521.486192066) < 0.01);
		// 85.0511°N is the northern border of most online mapping services
		// also OpenStreetMap's SlippyMap
		assertTrue(java.lang.Math
				.abs(MercatorProjection.latitudeToMetersY(85.0511) - 20037471.20513706) < 0.01);
		assertTrue(java.lang.Math
				.abs(MercatorProjection.latitudeToMetersY(8.946573850543412) - 1000000.0) < 0.01);

	}
}
