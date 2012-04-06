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
package org.mapsforge.core.util;

import java.io.Closeable;

import junit.framework.Assert;

import org.junit.Test;

/**
 * Tests the {@link IOUtils} class.
 */
public class IOUtilsTest {
	static class DummyCloseable implements Closeable {
		int closeCalls;

		@Override
		public void close() {
			++this.closeCalls;
		}
	}

	/**
	 * Tests the {@link IOUtils#closeQuietly(Closeable)} method.
	 */
	@Test
	public void closeQuietlyTest() {
		IOUtils.closeQuietly(null);

		DummyCloseable dummyCloseable = new DummyCloseable();
		IOUtils.closeQuietly(dummyCloseable);
		Assert.assertEquals(1, dummyCloseable.closeCalls);
	}
}
