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
import android.graphics.Path;

/**
 * A map renderer which uses a Canvas for drawing.
 */
class CanvasMapGenerator extends DatabaseMapGenerator {
	private static final Paint PAINT_TILE_FRAME = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final String THREAD_NAME = "CanvasMapGenerator";
	private int arrayListIndex;
	private Canvas canvas;
	private CircleContainer circleContainer;
	private ComplexWayContainer complexWayContainer;
	private float[] coordinates;
	private float[][] innerCoordinates;
	private byte currentLayer;
	private byte currentLevel;
	private Path path;
	private WayTextContainer pathTextContainer;
	private PointTextContainer pointTextContainer;
	private ShapePaintContainer shapePaintContainer;
	private ArrayList<ArrayList<ShapePaintContainer>> shapePaintContainers;
	private SimpleWayContainer simpleWayContainer;
	private SymbolContainer symbolContainer;
	private ArrayList<ShapePaintContainer> wayList;

	@Override
	void drawMapSymbols(ArrayList<SymbolContainer> drawSymbols) {
		for (this.arrayListIndex = drawSymbols.size() - 1; this.arrayListIndex >= 0; --this.arrayListIndex) {
			this.symbolContainer = drawSymbols.get(this.arrayListIndex);
			this.canvas.drawBitmap(this.symbolContainer.symbol, this.symbolContainer.x,
					this.symbolContainer.y, null);
		}
	}

	@Override
	void drawNodes(ArrayList<PointTextContainer> drawNodes) {
		for (this.arrayListIndex = drawNodes.size() - 1; this.arrayListIndex >= 0; --this.arrayListIndex) {
			this.pointTextContainer = drawNodes.get(this.arrayListIndex);
			this.canvas.drawText(this.pointTextContainer.text, this.pointTextContainer.x,
					this.pointTextContainer.y, this.pointTextContainer.paint);
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
	void drawWayNames(ArrayList<WayTextContainer> drawWayNames) {
		for (this.arrayListIndex = drawWayNames.size() - 1; this.arrayListIndex >= 0; --this.arrayListIndex) {
			this.pathTextContainer = drawWayNames.get(this.arrayListIndex);
			this.path.rewind();
			this.coordinates = this.pathTextContainer.coordinates;
			this.path.moveTo(this.coordinates[0], this.coordinates[1]);
			for (int i = 2; i < this.coordinates.length; i += 2) {
				this.path.lineTo(this.coordinates[i], this.coordinates[i + 1]);
			}
			this.canvas.drawTextOnPath(this.pathTextContainer.text, this.path, 0, 3,
					this.pathTextContainer.paint);
		}
	}

	@Override
	void drawWays(ArrayList<ArrayList<ArrayList<ShapePaintContainer>>> drawWays, byte layers,
			byte levelsPerLayer) {
		for (this.currentLayer = 0; this.currentLayer < layers; ++this.currentLayer) {
			this.shapePaintContainers = drawWays.get(this.currentLayer);
			for (this.currentLevel = 0; this.currentLevel < levelsPerLayer; ++this.currentLevel) {
				this.wayList = this.shapePaintContainers.get(this.currentLevel);
				for (this.arrayListIndex = this.wayList.size() - 1; this.arrayListIndex >= 0; --this.arrayListIndex) {
					this.shapePaintContainer = this.wayList.get(this.arrayListIndex);
					this.path.rewind();
					switch (this.shapePaintContainer.shapeContainer.getShapeType()) {
						case CIRCLE:
							this.circleContainer = (CircleContainer) this.shapePaintContainer.shapeContainer;
							this.path.addCircle(this.circleContainer.x, this.circleContainer.y,
									this.circleContainer.radius, Path.Direction.CCW);
							break;
						case SIMPLE_WAY:
							this.simpleWayContainer = (SimpleWayContainer) this.shapePaintContainer.shapeContainer;
							this.coordinates = this.simpleWayContainer.coordinates;
							this.path.moveTo(this.coordinates[0], this.coordinates[1]);
							for (int i = 2; i < this.coordinates.length; i += 2) {
								this.path.lineTo(this.coordinates[i], this.coordinates[i + 1]);
							}
							break;
						case COMPLEX_WAY:
							this.complexWayContainer = (ComplexWayContainer) this.shapePaintContainer.shapeContainer;
							this.coordinates = this.complexWayContainer.outerCoordinates;
							this.path.moveTo(this.coordinates[0], this.coordinates[1]);
							for (int i = 2; i < this.coordinates.length; i += 2) {
								this.path.lineTo(this.coordinates[i], this.coordinates[i + 1]);
							}
							// add inner ways
							this.innerCoordinates = this.complexWayContainer.innerCoordinates;
							for (int j = 0; j < this.innerCoordinates.length; ++j) {
								this.coordinates = this.innerCoordinates[j];
								this.path.moveTo(this.coordinates[0], this.coordinates[1]);
								for (int i = 2; i < this.coordinates.length; i += 2) {
									this.path.lineTo(this.coordinates[i],
											this.coordinates[i + 1]);
								}
							}
							break;
					}
					this.canvas.drawPath(this.path, this.shapePaintContainer.paint);
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
	void onAttachedToWindow() {
		// do nothing
	}

	@Override
	void onDetachedFromWindow() {
		// do nothing
	}

	@Override
	void setupMapGenerator(Bitmap bitmap) {
		this.canvas = new Canvas(bitmap);
		this.path = new Path();
		this.path.setFillType(Path.FillType.EVEN_ODD);
	}
}