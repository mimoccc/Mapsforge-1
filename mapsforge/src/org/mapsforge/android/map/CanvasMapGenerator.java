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
package org.mapsforge.android.map;

import java.util.ArrayList;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;

/**
 * A map renderer which uses a Canvas for drawing.
 */
class CanvasMapGenerator extends DatabaseMapGenerator {
	private static final Paint PAINT_TILE_FRAME = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final String THREAD_NAME = "CanvasMapGenerator";
	private int arrayListIndex;
	private Canvas canvas;
	private ArrayList<ArrayList<PathContainer>> innerWayList;
	private PathContainer pathContainer;
	private PathTextContainer pathTextContainer;
	private PointContainer pointContainer;
	private SymbolContainer symbolContainer;
	private ArrayList<PathContainer> wayList;

	@Override
	void drawMapSymbols(ArrayList<SymbolContainer> drawSymbols) {
		for (this.arrayListIndex = drawSymbols.size() - 1; this.arrayListIndex >= 0; --this.arrayListIndex) {
			this.symbolContainer = drawSymbols.get(this.arrayListIndex);
			this.canvas.drawBitmap(this.symbolContainer.symbol, this.symbolContainer.x,
					this.symbolContainer.y, null);
		}
	}

	@Override
	void drawNodes(ArrayList<PointContainer> drawNodes) {
		for (this.arrayListIndex = drawNodes.size() - 1; this.arrayListIndex >= 0; --this.arrayListIndex) {
			this.pointContainer = drawNodes.get(this.arrayListIndex);
			this.canvas.drawText(this.pointContainer.text, this.pointContainer.x,
					this.pointContainer.y, this.pointContainer.paint);
		}
	}

	@Override
	void drawTileFrame() {
		this.canvas.drawLines(new float[] { 0, 0, 0, this.canvas.getHeight(), 0,
				this.canvas.getHeight(), this.canvas.getWidth(), this.canvas.getHeight(),
				this.canvas.getWidth(), this.canvas.getHeight(), this.canvas.getWidth(), 0 },
				PAINT_TILE_FRAME);
	}

	@Override
	void drawWayNames(ArrayList<PathTextContainer> drawWayNames) {
		for (this.arrayListIndex = drawWayNames.size() - 1; this.arrayListIndex >= 0; --this.arrayListIndex) {
			this.pathTextContainer = drawWayNames.get(this.arrayListIndex);
			this.canvas.drawTextOnPath(this.pathTextContainer.text,
					this.pathTextContainer.path, 0, 3, this.pathTextContainer.paint);
		}
	}

	@Override
	void drawWays(ArrayList<ArrayList<ArrayList<PathContainer>>> drawWays, byte layers,
			byte levelsPerLayer) {
		for (byte i = 0; i < layers; ++i) {
			this.innerWayList = drawWays.get(i);
			for (byte j = 0; j < levelsPerLayer; ++j) {
				this.wayList = this.innerWayList.get(j);
				for (this.arrayListIndex = this.wayList.size() - 1; this.arrayListIndex >= 0; --this.arrayListIndex) {
					this.pathContainer = this.wayList.get(this.arrayListIndex);
					this.canvas.drawPath(this.pathContainer.path, this.pathContainer.paint);
				}
			}
		}
	}

	@Override
	void finishMapGeneration() {
		// do nothing
	}

	@Override
	String getThreadName() {
		return THREAD_NAME;
	}

	@Override
	void setupRenderer(Bitmap bitmap) {
		this.canvas = new Canvas(bitmap);
	}

	@Override
	void mapViewHasParent() {
		// TODO Auto-generated method stub

	}
}