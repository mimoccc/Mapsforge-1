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

import java.io.IOException;
import java.util.List;

import org.xml.sax.Attributes;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Style;

final class Area extends RenderingInstruction {
	static Area create(String elementName, Attributes attributes, int level) throws IOException {
		String file = null;
		String jar = null;
		int fill = Color.BLACK;
		int stroke = Color.TRANSPARENT;
		float strokeWidth = 0;

		for (int i = 0; i < attributes.getLength(); ++i) {
			String name = attributes.getLocalName(i);
			String value = attributes.getValue(i);

			if ("file".equals(name)) {
				file = value;
			} else if ("jar".equals(name)) {
				jar = value;
			} else if ("fill".equals(name)) {
				fill = Color.parseColor(value);
			} else if ("stroke".equals(name)) {
				stroke = Color.parseColor(value);
			} else if ("stroke-width".equals(name)) {
				strokeWidth = Float.parseFloat(value);
			} else {
				RenderThemeHandler.logUnknownAttribute(elementName, name, value, i);
			}
		}

		return new Area(file, jar, fill, stroke, strokeWidth, level);
	}

	private final Paint fill;
	private final int level;
	private final Paint outline;
	private final float strokeWidth;

	private Area(String file, String jar, int fill, int stroke, float strokeWidth, int level)
			throws IOException {
		super();

		Shader shader = createBitmapShader(file, jar);

		if (fill == Color.TRANSPARENT) {
			this.fill = null;
		} else {
			this.fill = new Paint(Paint.ANTI_ALIAS_FLAG);
			this.fill.setShader(shader);
			this.fill.setStyle(Style.FILL);
			this.fill.setColor(fill);
			this.fill.setStrokeCap(Cap.ROUND);
		}

		if (stroke == Color.TRANSPARENT) {
			this.outline = null;
		} else {
			this.outline = new Paint(Paint.ANTI_ALIAS_FLAG);
			this.outline.setStyle(Style.STROKE);
			this.outline.setColor(stroke);
			this.outline.setStrokeCap(Cap.ROUND);
		}

		this.strokeWidth = strokeWidth;
		this.level = level;
	}

	@Override
	public void onDestroy() {
		// do nothing
	}

	@Override
	public void renderNode(RenderThemeCallback renderThemeCallback, List<Tag> tags) {
		// do nothing
	}

	@Override
	public void renderWay(RenderThemeCallback renderThemeCallback, List<Tag> tags) {
		if (this.outline != null) {
			renderThemeCallback.addArea(this.outline, this.level);
		}
		if (this.fill != null) {
			renderThemeCallback.addArea(this.fill, this.level);
		}
	}

	@Override
	public void scaleStrokeWidth(float scaleFactor) {
		if (this.outline != null) {
			this.outline.setStrokeWidth(this.strokeWidth * scaleFactor);
		}
	}

	@Override
	public void scaleTextSize(float scaleFactor) {
		// do nothing
	}
}