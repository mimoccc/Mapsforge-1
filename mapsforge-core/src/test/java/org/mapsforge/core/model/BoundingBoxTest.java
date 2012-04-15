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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import junit.framework.Assert;

import org.junit.Test;

/**
 * Tests the {@link BoundingBox} class.
 */
public class BoundingBoxTest {
	private static final String BOUNDING_BOX_TO_STRING = "BoundingBox [minLatitudeE6=2, minLongitudeE6=1, maxLatitudeE6=4, maxLongitudeE6=3]";
	private static final String DELIMITER = ",";
	private static final int MAX_LATITUDE = 4;
	private static final int MAX_LONGITUDE = 3;
	private static final int MIN_LATITUDE = 2;
	private static final int MIN_LONGITUDE = 1;

	private static void verifyInvalid(String string) {
		try {
			BoundingBox.fromString(string);
			Assert.fail(string);
		} catch (IllegalArgumentException e) {
			Assert.assertTrue(true);
		}
	}

	/**
	 * Tests the {@link BoundingBox#contains(GeoPoint)} method.
	 */
	@Test
	public void containsTest() {
		BoundingBox boundingBox = new BoundingBox(MIN_LATITUDE, MIN_LONGITUDE, MAX_LATITUDE, MAX_LONGITUDE);
		GeoPoint geoPoint1 = new GeoPoint(MIN_LATITUDE, MIN_LONGITUDE);
		GeoPoint geoPoint2 = new GeoPoint(MAX_LATITUDE, MAX_LONGITUDE);
		GeoPoint geoPoint3 = new GeoPoint(MIN_LONGITUDE, MIN_LONGITUDE);
		GeoPoint geoPoint4 = new GeoPoint(MAX_LATITUDE, MAX_LATITUDE);

		Assert.assertTrue(boundingBox.contains(geoPoint1));
		Assert.assertTrue(boundingBox.contains(geoPoint2));
		Assert.assertFalse(boundingBox.contains(geoPoint3));
		Assert.assertFalse(boundingBox.contains(geoPoint4));
	}

	/**
	 * Tests the {@link BoundingBox#equals(Object)} and the {@link BoundingBox#hashCode()} method.
	 */
	@Test
	public void equalsTest() {
		BoundingBox boundingBox1 = new BoundingBox(MIN_LATITUDE, MIN_LONGITUDE, MAX_LATITUDE, MAX_LONGITUDE);
		BoundingBox boundingBox2 = new BoundingBox(MIN_LATITUDE, MIN_LONGITUDE, MAX_LATITUDE, MAX_LONGITUDE);
		BoundingBox boundingBox3 = new BoundingBox(0, 0, 0, 0);

		TestUtils.equalsTest(boundingBox1, boundingBox2);

		Assert.assertFalse(boundingBox1.equals(boundingBox3));
		Assert.assertFalse(boundingBox3.equals(boundingBox1));
		Assert.assertFalse(boundingBox1.equals(new Object()));
	}

	/**
	 * Tests the {@link BoundingBox#fromString(String)} method.
	 */
	@Test
	public void fromStringInvalidTest() {
		// invalid strings
		verifyInvalid("1,2,3,4,5");
		verifyInvalid("1,2,3,,4");
		verifyInvalid(",1,2,3,4");
		verifyInvalid("1,2,3,4,");
		verifyInvalid("1,2,3,a");
		verifyInvalid("1,2,3,");
		verifyInvalid("1,2,3");
		verifyInvalid("foo");
		verifyInvalid("");

		// invalid coordinates
		verifyInvalid("1,-181,3,4");
		verifyInvalid("1,2,3,181");
		verifyInvalid("-91,2,3,4");
		verifyInvalid("1,2,91,4");
		verifyInvalid("3,2,1,4");
		verifyInvalid("1,4,3,2");
	}

	/**
	 * Tests the {@link BoundingBox#fromString(String)} method.
	 */
	@Test
	public void fromStringValidTest() {
		String boundingBoxString = MIN_LATITUDE + DELIMITER + MIN_LONGITUDE + DELIMITER + MAX_LATITUDE + DELIMITER
				+ MAX_LONGITUDE;
		BoundingBox boundingBox = BoundingBox.fromString(boundingBoxString);
		Assert.assertEquals(Coordinates.degreesToMicrodegrees(MIN_LATITUDE), boundingBox.minLatitudeE6, 0);
		Assert.assertEquals(Coordinates.degreesToMicrodegrees(MIN_LONGITUDE), boundingBox.minLongitudeE6, 0);
		Assert.assertEquals(Coordinates.degreesToMicrodegrees(MAX_LATITUDE), boundingBox.maxLatitudeE6, 0);
		Assert.assertEquals(Coordinates.degreesToMicrodegrees(MAX_LONGITUDE), boundingBox.maxLongitudeE6, 0);
	}

	/**
	 * Tests the {@link BoundingBox#getCenterPoint()} method.
	 */
	@Test
	public void getCenterPointTest() {
		BoundingBox boundingBox = new BoundingBox(MIN_LATITUDE, MIN_LONGITUDE, MAX_LATITUDE, MAX_LONGITUDE);
		GeoPoint centerPoint = boundingBox.getCenterPoint();
		Assert.assertEquals((MIN_LATITUDE + MAX_LATITUDE) / 2, centerPoint.latitudeE6);
		Assert.assertEquals((MIN_LONGITUDE + MAX_LONGITUDE) / 2, centerPoint.longitudeE6);
	}

	/**
	 * Tests the public fields and the getter-methods.
	 */
	@Test
	public void getterTest() {
		BoundingBox boundingBox = new BoundingBox(MIN_LATITUDE, MIN_LONGITUDE, MAX_LATITUDE, MAX_LONGITUDE);

		Assert.assertEquals(MIN_LATITUDE, boundingBox.minLatitudeE6);
		Assert.assertEquals(MIN_LONGITUDE, boundingBox.minLongitudeE6);
		Assert.assertEquals(MAX_LATITUDE, boundingBox.maxLatitudeE6);
		Assert.assertEquals(MAX_LONGITUDE, boundingBox.maxLongitudeE6);

		Assert.assertEquals(Coordinates.microdegreesToDegrees(MIN_LATITUDE), boundingBox.getMinLatitude(), 0);
		Assert.assertEquals(Coordinates.microdegreesToDegrees(MIN_LONGITUDE), boundingBox.getMinLongitude(), 0);
		Assert.assertEquals(Coordinates.microdegreesToDegrees(MAX_LATITUDE), boundingBox.getMaxLatitude(), 0);
		Assert.assertEquals(Coordinates.microdegreesToDegrees(MAX_LONGITUDE), boundingBox.getMaxLongitude(), 0);
	}

	/**
	 * Tests the serialization and deserialization methods.
	 * 
	 * @throws IOException
	 *             see {@link ObjectOutputStream#writeObject(Object)}
	 * @throws ClassNotFoundException
	 *             see {@link ObjectInputStream#readObject()}
	 */
	@Test
	public void serializeTest() throws IOException, ClassNotFoundException {
		BoundingBox boundingBox = new BoundingBox(MIN_LATITUDE, MIN_LONGITUDE, MAX_LATITUDE, MAX_LONGITUDE);
		TestUtils.serializeTest(boundingBox);
	}

	/**
	 * Tests the {@link BoundingBox#toString()} method.
	 */
	@Test
	public void toStringTest() {
		BoundingBox boundingBox = new BoundingBox(MIN_LATITUDE, MIN_LONGITUDE, MAX_LATITUDE, MAX_LONGITUDE);
		Assert.assertEquals(BOUNDING_BOX_TO_STRING, boundingBox.toString());
	}
}
