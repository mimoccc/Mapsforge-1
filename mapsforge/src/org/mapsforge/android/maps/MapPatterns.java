/*
 * Copyright 2010, 2011 mapsforge.org
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
package org.mapsforge.android.maps;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Shader;
import android.graphics.Shader.TileMode;

/**
 * This class holds all patterns that can be rendered on the map. All bitmaps are created when
 * the MapPatterns constructor is called and are recycled when the recycle() method is called.
 */
class MapPatterns {
	private final Bitmap cemetery;
	private final Bitmap woodDeciduous;
	private final Bitmap woodMixed;
	final Shader cemeteryShader;
	final Shader woodDeciduousShader;
	final Shader woodMixedShader;

	MapPatterns() {
		this.cemetery = BitmapFactory.decodeStream(getClass().getResourceAsStream(
				"patterns/cemetery.png"));
		this.cemeteryShader = new BitmapShader(this.cemetery, TileMode.REPEAT, TileMode.REPEAT);
		this.woodDeciduous = BitmapFactory.decodeStream(getClass().getResourceAsStream(
				"patterns/wood-deciduous.png"));
		this.woodDeciduousShader = new BitmapShader(this.woodDeciduous, TileMode.REPEAT,
				TileMode.REPEAT);
		this.woodMixed = BitmapFactory.decodeStream(getClass().getResourceAsStream(
				"patterns/wood-mixed.png"));
		this.woodMixedShader = new BitmapShader(this.woodMixed, TileMode.REPEAT,
				TileMode.REPEAT);
	}

	void recycle() {
		if (this.cemetery != null) {
			this.cemetery.recycle();
		}
		if (this.woodDeciduous != null) {
			this.woodDeciduous.recycle();
		}
		if (this.woodMixed != null) {
			this.woodMixed.recycle();
		}
	}
}