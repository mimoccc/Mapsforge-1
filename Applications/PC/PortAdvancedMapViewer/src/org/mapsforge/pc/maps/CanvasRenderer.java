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
package org.mapsforge.pc.maps;

import java.util.ArrayList;

//import android.graphics.Bitmap;
import org.mapsforge.core.graphics.Bitmap;
//import android.graphics.Canvas;
import org.mapsforge.core.graphics.Canvas;
//import android.graphics.Color;
import java.awt.Color;
import java.awt.Font;
//import android.graphics.Matrix;
import org.mapsforge.core.graphics.Matrix;
//import android.graphics.Paint;
import org.mapsforge.core.graphics.Paint;
//import android.graphics.Path;
import org.mapsforge.core.graphics.Path;
//import android.graphics.Typeface;
import org.mapsforge.core.graphics.Typeface;

/**
 * A map renderer which uses a Canvas for drawing.
 * 
 * @see <a
 *      href="http://developer.android.com/reference/android/graphics/Canvas.html">Canvas</a>
 */
public class CanvasRenderer extends DatabaseMapGenerator {
	public static final Paint PAINT_TILE_COORDINATES = new Paint(
			Paint.ANTI_ALIAS_FLAG);
	public static final Paint PAINT_TILE_COORDINATES_STROKE = new Paint(
			Paint.ANTI_ALIAS_FLAG);
	public static final Paint PAINT_TILE_FRAME = new Paint(
			Paint.ANTI_ALIAS_FLAG);
	public static final String THREAD_NAME = "CanvasRenderer";
	public int arrayListIndex;
	public Paint bitmapFilterPaint;
	public Canvas canvas;
	public CircleContainer circleContainer;
	public WayContainer complexWayContainer;
	public float[][] coordinates;
	public byte currentLayer;
	public byte currentLevel;
	public Path path;
	public WayTextContainer pathTextContainer;
	public PointTextContainer pointTextContainer;
	public ShapePaintContainer shapePaintContainer;
	public ArrayList<ArrayList<ShapePaintContainer>> shapePaintContainers;
	public StringBuilder stringBuilder;
	public SymbolContainer symbolContainer;
	public Matrix symbolMatrix;
	public float[] textCoordinates;
	public float[] tileFrame;
	public ArrayList<ShapePaintContainer> wayList;

	@Override
	void drawNodes(ArrayList<PointTextContainer> drawNodes) {
		for (this.arrayListIndex = drawNodes.size() - 1; this.arrayListIndex >= 0; --this.arrayListIndex) {
			this.pointTextContainer = drawNodes.get(this.arrayListIndex);
			if (this.pointTextContainer.paintBack != null) {
				this.canvas.drawText(this.pointTextContainer.text,
						this.pointTextContainer.x, this.pointTextContainer.y,
						this.pointTextContainer.paintBack);
			}
			this.canvas.drawText(this.pointTextContainer.text,
					this.pointTextContainer.x, this.pointTextContainer.y,
					this.pointTextContainer.paintFront);
		}
	}

	@Override
	void drawSymbols(ArrayList<SymbolContainer> drawSymbols) {
		for (this.arrayListIndex = drawSymbols.size() - 1; this.arrayListIndex >= 0; --this.arrayListIndex) {
			this.symbolContainer = drawSymbols.get(this.arrayListIndex);
			// use the matrix for rotation and translation of the symbol
			if (this.symbolContainer.alignCenter) {
				this.symbolMatrix.setRotate(this.symbolContainer.rotation,
						this.symbolContainer.symbol.getWidth() >> 1,
						this.symbolContainer.symbol.getHeight() >> 1);
				this.symbolMatrix
						.postTranslate(
								this.symbolContainer.x
										- (this.symbolContainer.symbol
												.getWidth() >> 1),
								this.symbolContainer.y
										- (this.symbolContainer.symbol
												.getHeight() >> 1));
			} else {
				this.symbolMatrix.setRotate(this.symbolContainer.rotation);
				this.symbolMatrix.postTranslate(this.symbolContainer.x,
						this.symbolContainer.y);
			}
			this.canvas.drawBitmap(this.symbolContainer.symbol,
					this.symbolMatrix, this.bitmapFilterPaint);
		}
	}

	@Override
	void drawTileCoordinates(Tile tile) {
		this.stringBuilder.setLength(0);
		this.stringBuilder.append("X: ");
		this.stringBuilder.append(tile.x);
		this.canvas.drawText(this.stringBuilder.toString(), 20, 30,
				PAINT_TILE_COORDINATES_STROKE);
		this.canvas.drawText(this.stringBuilder.toString(), 20, 30,
				PAINT_TILE_COORDINATES);

		this.stringBuilder.setLength(0);
		this.stringBuilder.append("Y: ");
		this.stringBuilder.append(tile.y);
		this.canvas.drawText(this.stringBuilder.toString(), 20, 60,
				PAINT_TILE_COORDINATES_STROKE);
		this.canvas.drawText(this.stringBuilder.toString(), 20, 60,
				PAINT_TILE_COORDINATES);

		this.stringBuilder.setLength(0);
		this.stringBuilder.append("Z: ");
		this.stringBuilder.append(tile.zoomLevel);
		this.canvas.drawText(this.stringBuilder.toString(), 20, 90,
				PAINT_TILE_COORDINATES_STROKE);
		this.canvas.drawText(this.stringBuilder.toString(), 20, 90,
				PAINT_TILE_COORDINATES);
	}

	@Override
	void drawTileFrame() {
		this.canvas.drawLines(this.tileFrame, PAINT_TILE_FRAME);
	}

	@Override
	void drawWayNames(ArrayList<WayTextContainer> drawWayNames) {
		for (this.arrayListIndex = drawWayNames.size() - 1; this.arrayListIndex >= 0; --this.arrayListIndex) {
			this.pathTextContainer = drawWayNames.get(this.arrayListIndex);
			this.path.rewind();
			this.textCoordinates = this.pathTextContainer.coordinates;
			this.path.moveTo(this.textCoordinates[0], this.textCoordinates[1]);
			for (int i = 2; i < this.textCoordinates.length; i += 2) {
				this.path.lineTo(this.textCoordinates[i],
						this.textCoordinates[i + 1]);
			}
			this.canvas.drawTextOnPath(this.pathTextContainer.text, this.path,
					0, 3, this.pathTextContainer.paint);
		}
	}

	@Override
	void drawWays(
			ArrayList<ArrayList<ArrayList<ShapePaintContainer>>> drawWays,
			byte layers, byte levelsPerLayer) {
		for (this.currentLayer = 0; this.currentLayer < layers; ++this.currentLayer) {
			this.shapePaintContainers = drawWays.get(this.currentLayer);
			for (this.currentLevel = 0; this.currentLevel < levelsPerLayer; ++this.currentLevel) {
				this.wayList = this.shapePaintContainers.get(this.currentLevel);
				for (this.arrayListIndex = this.wayList.size() - 1; this.arrayListIndex >= 0; --this.arrayListIndex) {
					this.shapePaintContainer = this.wayList
							.get(this.arrayListIndex);
					this.path.rewind();
					switch (this.shapePaintContainer.shapeContainer
							.getShapeType()) {
					case CIRCLE:
						this.circleContainer = (CircleContainer) this.shapePaintContainer.shapeContainer;
						this.path
								.addCircle(this.circleContainer.x,
										this.circleContainer.y,
										this.circleContainer.radius,
										Path.Direction.CCW);
						break;
					case WAY:
						this.complexWayContainer = (WayContainer) this.shapePaintContainer.shapeContainer;
						this.coordinates = this.complexWayContainer.coordinates;
						for (int j = 0; j < this.coordinates.length; ++j) {
							// make sure that the coordinates sequence is not
							// empty
							if (this.coordinates[j].length > 2) {
								this.path.moveTo(this.coordinates[j][0],
										this.coordinates[j][1]);
								for (int i = 2; i < this.coordinates[j].length; i += 2) {
									this.path.lineTo(this.coordinates[j][i],
											this.coordinates[j][i + 1]);
								}
							}
						}
						break;
					default:
						// all cases are covered, the default case should never
						// occur
						continue;
					}
					this.canvas.drawPath(this.path,
							this.shapePaintContainer.paint);
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
	public void setupRenderer(Bitmap bitmap) {
		this.canvas = new Canvas(bitmap);
		this.symbolMatrix = new Matrix();
		this.bitmapFilterPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
		//this.tileFrame = new float[] { 0, 0, 0, Tile.TILE_SIZE, 0,
		//		Tile.TILE_SIZE, Tile.TILE_SIZE, Tile.TILE_SIZE, Tile.TILE_SIZE,
		//		Tile.TILE_SIZE, Tile.TILE_SIZE, 0 };
		this.tileFrame = new float[] { 0, 0, 0, Tile.TILE_SIZE, 0,
				Tile.TILE_SIZE, Tile.TILE_SIZE, Tile.TILE_SIZE, Tile.TILE_SIZE,
				Tile.TILE_SIZE, Tile.TILE_SIZE, 0, Tile.TILE_SIZE, 0, 0, 0 };
		this.path = new Path();
		this.path.setFillType(Path.FillType.EVEN_ODD);
		this.stringBuilder = new StringBuilder(16);
		// PAINT_TILE_COORDINATES.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
		// PAINT_TILE_COORDINATES.setTextSize(20);
		PAINT_TILE_COORDINATES.setTypeface(new Typeface(null, Font.BOLD, 20));
		// PAINT_TILE_COORDINATES_STROKE.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
		// PAINT_TILE_COORDINATES_STROKE.setTextSize(20);
		PAINT_TILE_COORDINATES_STROKE.setTypeface(new Typeface(null, Font.BOLD,
				20));
		PAINT_TILE_COORDINATES_STROKE.setStyle(Paint.Style.STROKE);
		PAINT_TILE_COORDINATES_STROKE.setStrokeWidth(5);

		PAINT_TILE_COORDINATES_STROKE.setColor(Color.WHITE);
	}
}