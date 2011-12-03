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

/**
 * 
 */
public class DebugSettings {
	private boolean drawTileCoordinates;
	private boolean drawTileFrames;
	private boolean highlightWaterTiles;
	private final MapView mapView;

	DebugSettings(MapView mapView) {
		this.mapView = mapView;
	}

	/**
	 * @return true if drawing of tile coordinates is enabled, false otherwise.
	 */
	public boolean isDrawTileCoordinates() {
		return this.drawTileCoordinates;
	}

	/**
	 * @return true if drawing of tile frames is enabled, false otherwise.
	 */
	public boolean isDrawTileFrames() {
		return this.drawTileFrames;
	}

	/**
	 * @return true if highlighting of water tiles is enabled, false otherwise.
	 */
	public boolean isHighlightWaterTiles() {
		return this.highlightWaterTiles;
	}

	/**
	 * @param drawTileCoordinates
	 *            true if tile coordinates should be drawn, false otherwise. Has no effect in downloading mode.
	 */
	public void setDrawTileCoordinates(boolean drawTileCoordinates) {
		this.drawTileCoordinates = drawTileCoordinates;
		this.mapView.clearAndRedrawMapView();
	}

	/**
	 * @param drawTileFrames
	 *            true if tile frames should be drawn, false otherwise. Has no effect in downloading mode.
	 */
	public void setDrawTileFrames(boolean drawTileFrames) {
		this.drawTileFrames = drawTileFrames;
		this.mapView.clearAndRedrawMapView();
	}

	/**
	 * @param highlightWaterTiles
	 *            true if water tiles should be highlighted, false otherwise. Has no effect in downloading mode.
	 */
	public void setHighlightWaterTiles(boolean highlightWaterTiles) {
		this.highlightWaterTiles = highlightWaterTiles;
		this.mapView.clearAndRedrawMapView();
	}
}
