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

import java.util.ArrayList;
import java.util.List;

import org.xml.sax.Attributes;

import android.graphics.Color;

/**
 * A RenderTheme defines how ways and nodes are drawn.
 */
public class RenderTheme {
	static RenderTheme create(String elementName, Attributes attributes) {
		int mapBackground = Color.WHITE;
		float scaleStrokeWidth = 1;
		float scaleTextWidth = 1;

		for (int i = 0; i < attributes.getLength(); ++i) {
			String name = attributes.getLocalName(i);
			String value = attributes.getValue(i);

			if ("schemaLocation".equals(name)) {
				continue;
			} else if ("map-background".equals(name)) {
				mapBackground = Color.parseColor(value);
			} else if ("scale-stroke-width".equals(name)) {
				scaleStrokeWidth = Float.parseFloat(value);
			} else if ("scale-text-width".equals(name)) {
				scaleTextWidth = Float.parseFloat(value);
			} else {
				RenderThemeHandler.logUnknownAttribute(elementName, name, value, i);
			}
		}

		return new RenderTheme(mapBackground, scaleStrokeWidth, scaleTextWidth);
	}

	private int levels;
	private final int mapBackground;
	private final ArrayList<Rule> rulesList;
	private final float scaleStrokeWidth;
	private final float scaleTextWidth;

	RenderTheme(int mapBackground, float scaleStrokeWidth, float scaleTextWidth) {
		this.mapBackground = mapBackground;
		this.scaleStrokeWidth = scaleStrokeWidth;
		this.scaleTextWidth = scaleTextWidth;
		this.rulesList = new ArrayList<Rule>();
	}

	/**
	 * Must be called when this RenderTheme gets destroyed to clean up and free resources.
	 */
	public void destroy() {
		for (int i = 0, n = this.rulesList.size(); i < n; ++i) {
			this.rulesList.get(i).onDestroy();
		}
	}

	/**
	 * Returns the number of distinct drawing levels required by this RenderTheme.
	 * 
	 * @return the number of distinct drawing levels required by this RenderTheme.
	 */
	public int getLevels() {
		return this.levels;
	}

	/**
	 * Returns the map background color of this RenderTheme.
	 * 
	 * @return the map background color of this RenderTheme.
	 * @see Color
	 */
	public int getMapBackground() {
		return this.mapBackground;
	}

	/**
	 * Matches a closed way with the given parameters against this RenderTheme.
	 * 
	 * @param renderThemeCallback
	 *            the callback implementation which will be executed on each match.
	 * @param tags
	 *            the tags of the way.
	 * @param zoomLevel
	 *            the zoom level at which the way should be matched.
	 */
	public void matchClosedWay(RenderThemeCallback renderThemeCallback, List<Tag> tags, byte zoomLevel) {
		for (int i = 0, n = this.rulesList.size(); i < n; ++i) {
			this.rulesList.get(i).matchWay(renderThemeCallback, tags, zoomLevel, Closed.YES);
		}
	}

	/**
	 * Matches a linear way with the given parameters against this RenderTheme.
	 * 
	 * @param renderThemeCallback
	 *            the callback implementation which will be executed on each match.
	 * @param tags
	 *            the tags of the way.
	 * @param zoomLevel
	 *            the zoom level at which the way should be matched.
	 */
	public void matchLinearWay(RenderThemeCallback renderThemeCallback, List<Tag> tags, byte zoomLevel) {
		for (int i = 0, n = this.rulesList.size(); i < n; ++i) {
			this.rulesList.get(i).matchWay(renderThemeCallback, tags, zoomLevel, Closed.NO);
		}
	}

	/**
	 * Matches a node with the given parameters against this RenderTheme.
	 * 
	 * @param renderThemeCallback
	 *            the callback implementation which will be executed on each match.
	 * @param tags
	 *            the tags of the node.
	 * @param zoomLevel
	 *            the zoom level at which the node should be matched.
	 */
	public void matchNode(RenderThemeCallback renderThemeCallback, List<Tag> tags, byte zoomLevel) {
		for (int i = 0, n = this.rulesList.size(); i < n; ++i) {
			this.rulesList.get(i).matchNode(renderThemeCallback, tags, zoomLevel);
		}
	}

	/**
	 * Scales the stroke width of this RenderTheme by the given factor.
	 * 
	 * @param scaleFactor
	 *            the factor by which the stroke width should be scaled.
	 */
	public void scaleStrokeWidth(float scaleFactor) {
		for (int i = 0, n = this.rulesList.size(); i < n; ++i) {
			this.rulesList.get(i).scaleStrokeWidth(scaleFactor * this.scaleStrokeWidth);
		}
	}

	/**
	 * Scales the text size of this RenderTheme by the given factor.
	 * 
	 * @param scaleFactor
	 *            the factor by which the text size should be scaled.
	 */
	public void scaleTextSize(float scaleFactor) {
		for (int i = 0, n = this.rulesList.size(); i < n; ++i) {
			this.rulesList.get(i).scaleTextSize(scaleFactor * this.scaleTextWidth);
		}
	}

	void addRule(Rule rule) {
		this.rulesList.add(rule);
	}

	void complete() {
		this.rulesList.trimToSize();
		for (int i = 0, n = this.rulesList.size(); i < n; ++i) {
			this.rulesList.get(i).onComplete();
		}
	}

	void setLevels(int levels) {
		this.levels = levels;
	}
}