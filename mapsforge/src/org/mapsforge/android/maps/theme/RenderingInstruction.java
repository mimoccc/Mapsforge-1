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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Shader;
import android.graphics.Shader.TileMode;

abstract class RenderingInstruction {
	Shader createBitmapShader(String file, String jar) throws IOException {
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
			// no image source defined
			return null;
		}

		Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
		inputStream.close();
		Shader shader = new BitmapShader(bitmap, TileMode.REPEAT, TileMode.REPEAT);
		bitmap.recycle();
		return shader;
	}

	abstract void onDestroy();

	abstract void renderNode(RenderThemeCallback renderThemeCallback, List<Tag> tags);

	abstract void renderWay(RenderThemeCallback renderThemeCallback, List<Tag> tags);

	abstract void scaleStrokeWidth(float scaleFactor);

	abstract void scaleTextSize(float scaleFactor);
}