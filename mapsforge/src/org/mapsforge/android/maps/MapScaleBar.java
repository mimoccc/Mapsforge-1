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
package org.mapsforge.android.maps;

import java.util.HashMap;
import java.util.Map;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;

/**
 * A MapScaleBar displays the ratio of a distance on the map to the corresponding distance on the ground.
 */
public class MapScaleBar {
	/**
	 * Enumeration of all text fields.
	 */
	public enum TextField {
		/**
		 * Unit symbol for one foot.
		 */
		FOOT,

		/**
		 * Unit symbol for one kilometer.
		 */
		KILOMETER,

		/**
		 * Unit symbol for one meter.
		 */
		METER,

		/**
		 * Unit symbol for one mile.
		 */
		MILE;
	}

	private static final int BITMAP_HEIGHT = 35;
	private static final int BITMAP_WIDTH = 130;
	private static final double LATITUDE_REDRAW_THRESHOLD = 0.2;
	private static final int MARGIN_BOTTOM = 5;
	private static final int MARGIN_LEFT = 5;
	private static final Paint SCALE_BAR = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint SCALE_BAR_STROKE = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final int[] SCALE_BAR_VALUES = { 10000000, 5000000, 2000000, 1000000, 500000, 200000,
			100000, 50000, 20000, 10000, 5000, 2000, 1000, 500, 200, 100, 50, 20, 10, 5, 2, 1 };
	private static final Paint SCALE_TEXT = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint SCALE_TEXT_STROKE = new Paint(Paint.ANTI_ALIAS_FLAG);

	private static void configurePaints() {
		SCALE_BAR.setStrokeWidth(2);
		SCALE_BAR.setStrokeCap(Paint.Cap.SQUARE);
		SCALE_BAR.setColor(Color.BLACK);
		SCALE_BAR_STROKE.setStrokeWidth(5);
		SCALE_BAR_STROKE.setStrokeCap(Paint.Cap.SQUARE);
		SCALE_BAR_STROKE.setColor(Color.WHITE);

		SCALE_TEXT.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
		SCALE_TEXT.setTextSize(14);
		SCALE_TEXT.setColor(Color.BLACK);
		SCALE_TEXT_STROKE.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
		SCALE_TEXT_STROKE.setStyle(Paint.Style.STROKE);
		SCALE_TEXT_STROKE.setColor(Color.WHITE);
		SCALE_TEXT_STROKE.setStrokeWidth(3);
		SCALE_TEXT_STROKE.setTextSize(14);
	}

	private MapPositionFix mapPositionFix;
	private final Bitmap mapScaleBitmap;
	private final Canvas mapScaleCanvas;
	private final MapView mapView;
	private boolean showMapScaleBar;
	private final Map<TextField, String> textFields;

	MapScaleBar(MapView mapView) {
		this.mapView = mapView;
		this.mapScaleBitmap = Bitmap.createBitmap(BITMAP_WIDTH, BITMAP_HEIGHT, Bitmap.Config.ARGB_4444);
		this.mapScaleCanvas = new Canvas(this.mapScaleBitmap);
		this.textFields = new HashMap<TextField, String>();
		setDefaultTexts();
		configurePaints();
	}

	/**
	 * @return true if this map scale bar is visible, false otherwise.
	 */
	public boolean isShowMapScaleBar() {
		return this.showMapScaleBar;
	}

	/**
	 * @param showMapScaleBar
	 *            true if the map scale bar should be drawn, false otherwise.
	 */
	public void setShowMapScaleBar(boolean showMapScaleBar) {
		this.showMapScaleBar = showMapScaleBar;
	}

	/**
	 * Overrides the specified text field with the given string.
	 * 
	 * @param textField
	 *            the text field to override.
	 * @param value
	 *            the new value of the text field.
	 */
	public void setText(TextField textField, String value) {
		this.textFields.put(textField, value);
	}

	private boolean isRedrawNecessary() {
		if (this.mapPositionFix == null) {
			return true;
		}

		MapPositionFix currentMapPositionFix = this.mapView.getMapPosition().getMapPositionFix();

		if (currentMapPositionFix.zoomLevel != this.mapPositionFix.zoomLevel) {
			return true;
		}

		double latitudeDiff = Math.abs(currentMapPositionFix.latitude - this.mapPositionFix.latitude);
		if (latitudeDiff > LATITUDE_REDRAW_THRESHOLD) {
			return true;
		}

		return false;
	}

	/**
	 * Redraws the map scale bitmap with the given parameters.
	 * 
	 * @param scaleBarLength
	 *            the length of the map scale bar in pixels.
	 * @param mapScaleValue
	 *            the map scale value in meters.
	 */
	private void redrawMapScaleBitmap(float scaleBarLength, int mapScaleValue) {
		this.mapScaleBitmap.eraseColor(Color.TRANSPARENT);

		// draw the scale bar
		this.mapScaleCanvas.drawLine(7, 20, scaleBarLength + 3, 20, SCALE_BAR_STROKE);
		this.mapScaleCanvas.drawLine(5, 10, 5, 30, SCALE_BAR_STROKE);
		this.mapScaleCanvas.drawLine(scaleBarLength + 5, 10, scaleBarLength + 5, 30, SCALE_BAR_STROKE);
		this.mapScaleCanvas.drawLine(7, 20, scaleBarLength + 3, 20, SCALE_BAR);
		this.mapScaleCanvas.drawLine(5, 10, 5, 30, SCALE_BAR);
		this.mapScaleCanvas.drawLine(scaleBarLength + 5, 10, scaleBarLength + 5, 30, SCALE_BAR);

		// draw the scale text
		if (mapScaleValue < 1000) {
			String unitSymbol = this.textFields.get(TextField.METER);
			this.mapScaleCanvas.drawText(mapScaleValue + unitSymbol, 10, 15, SCALE_TEXT_STROKE);
			this.mapScaleCanvas.drawText(mapScaleValue + unitSymbol, 10, 15, SCALE_TEXT);
		} else {
			int kmValue = mapScaleValue / 1000;
			String unitSymbol = this.textFields.get(TextField.KILOMETER);
			this.mapScaleCanvas.drawText(kmValue + unitSymbol, 10, 15, SCALE_TEXT_STROKE);
			this.mapScaleCanvas.drawText(kmValue + unitSymbol, 10, 15, SCALE_TEXT);
		}
	}

	private void setDefaultTexts() {
		this.textFields.put(TextField.FOOT, " ft");
		this.textFields.put(TextField.MILE, " mi");

		this.textFields.put(TextField.METER, " m");
		this.textFields.put(TextField.KILOMETER, " km");
	}

	void destroy() {
		this.mapScaleBitmap.recycle();
	}

	void draw(Canvas canvas) {
		int top = this.mapView.getHeight() - BITMAP_HEIGHT - MARGIN_BOTTOM;
		canvas.drawBitmap(this.mapScaleBitmap, MARGIN_LEFT, top, null);
	}

	void redrawScaleBar() {
		if (!isRedrawNecessary()) {
			return;
		}

		this.mapPositionFix = this.mapView.getMapPosition().getMapPositionFix();
		double meterPerPixel = MercatorProjection.calculateGroundResolution(this.mapPositionFix.latitude,
				this.mapPositionFix.zoomLevel);

		float scaleBarLength = 0;
		int mapScaleValue = 0;
		for (int i = 0; i < SCALE_BAR_VALUES.length; ++i) {
			mapScaleValue = SCALE_BAR_VALUES[i];
			scaleBarLength = mapScaleValue / (float) meterPerPixel;
			if (scaleBarLength < (BITMAP_WIDTH - 10)) {
				break;
			}
		}

		redrawMapScaleBitmap(scaleBarLength, mapScaleValue);
	}
}
