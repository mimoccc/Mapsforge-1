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
import android.graphics.Shader.TileMode;

abstract class RenderInstruction {
	static BitmapShader createBitmapShader(Bitmap bitmap) {
		if (bitmap == null) {
			return null;
		}

		BitmapShader shader = new BitmapShader(bitmap, TileMode.REPEAT, TileMode.REPEAT);
		bitmap.recycle();
		return shader;
	}

	Bitmap createBitmap(String src) throws IOException {
		if (src == null || src.length() == 0) {
			// no image source defined
			return null;
		}

		InputStream inputStream;
		if (src.startsWith("jar:")) {
			inputStream = getClass().getResourceAsStream(src.substring(4));
			if (inputStream == null) {
				throw new FileNotFoundException("resource not found: " + src);
			}
		} else if (src.startsWith("file:")) {
			File inputFile = new File(src.substring(5));
			if (!inputFile.exists()) {
				throw new IllegalArgumentException("file does not exist: " + src);
			} else if (!inputFile.isFile()) {
				throw new IllegalArgumentException("not a file: " + src);
			} else if (!inputFile.canRead()) {
				throw new IllegalArgumentException("cannot read file: " + src);
			}
			inputStream = new FileInputStream(inputFile);
		} else {
			throw new IllegalArgumentException("invalid bitmap source: " + src);
		}

		Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
		inputStream.close();
		return bitmap;
	}

	abstract void onDestroy();

	abstract void renderNode(RenderCallback renderCallback, List<Tag> tags);

	abstract void renderWay(RenderCallback renderCallback, List<Tag> tags);

	abstract void scaleStrokeWidth(float scaleFactor);

	abstract void scaleTextSize(float scaleFactor);
}