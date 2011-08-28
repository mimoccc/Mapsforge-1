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
package org.mapsforge.android.maps.theme;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.xml.sax.Attributes;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

final class LineSymbol extends RenderingInstruction {
	static LineSymbol create(String elementName, Attributes attributes) throws IOException {
		String file = null;
		String jar = null;
		boolean alignCenter = false;
		boolean repeat = false;

		for (int i = 0; i < attributes.getLength(); ++i) {
			String name = attributes.getLocalName(i);
			String value = attributes.getValue(i);

			if ("file".equals(name)) {
				file = value;
			} else if ("jar".equals(name)) {
				jar = value;
			} else if ("align-center".equals(name)) {
				alignCenter = Boolean.parseBoolean(value);
			} else if ("repeat".equals(name)) {
				repeat = Boolean.parseBoolean(value);
			} else {
				RenderThemeHandler.logUnknownAttribute(elementName, name, value, i);
			}
		}

		return new LineSymbol(file, jar, alignCenter, repeat);
	}

	private final boolean alignCenter;
	private final Bitmap bitmap;
	private final boolean repeat;

	private LineSymbol(String file, String jar, boolean alignCenter, boolean repeat) throws IOException {
		super();

		InputStream inputStream;
		if (file != null && file.length() > 0) {
			File inputFile = new File(file);
			if (!inputFile.exists()) {
				throw new IllegalArgumentException("file does not exist: " + file);
			} else if (!inputFile.isFile()) {
				throw new IllegalArgumentException("not a file: " + file);
			} else if (!inputFile.canRead()) {
				throw new IllegalArgumentException("cannot read file: " + file);
			}
			inputStream = new FileInputStream(inputFile);
		} else if (jar != null && jar.length() > 0) {
			inputStream = getClass().getResourceAsStream(jar);
			if (inputStream == null) {
				throw new FileNotFoundException("resource not found: " + jar);
			}
		} else {
			throw new IllegalArgumentException("no image source defined");
		}
		this.bitmap = BitmapFactory.decodeStream(inputStream);
		inputStream.close();

		this.alignCenter = alignCenter;
		this.repeat = repeat;
	}

	@Override
	public void onDestroy() {
		this.bitmap.recycle();
	}

	@Override
	public void renderNode(RenderThemeCallback renderThemeCallback, List<Tag> tags) {
		// do nothing
	}

	@Override
	public void renderWay(RenderThemeCallback renderThemeCallback, List<Tag> tags) {
		renderThemeCallback.addWaySymbol(this.bitmap, this.alignCenter, this.repeat);
	}

	@Override
	public void scaleStrokeWidth(float scaleFactor) {
		// do nothing
	}

	@Override
	public void scaleTextSize(float scaleFactor) {
		// do nothing
	}
}