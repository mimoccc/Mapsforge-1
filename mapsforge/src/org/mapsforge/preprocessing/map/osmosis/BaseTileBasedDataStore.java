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
package org.mapsforge.preprocessing.map.osmosis;

import gnu.trove.map.hash.TShortIntHashMap;

import java.util.logging.Logger;

import org.mapsforge.core.GeoCoordinate;
import org.mapsforge.core.MercatorProjection;
import org.mapsforge.core.Rect;
import org.mapsforge.preprocessing.map.osmosis.TileData.TDNode;
import org.mapsforge.preprocessing.map.osmosis.TileData.TDWay;

abstract class BaseTileBasedDataStore implements TileBasedDataStore {

	private static final Logger LOGGER =
			Logger.getLogger(BaseTileBasedDataStore.class.getName());

	protected final Rect boundingbox;
	protected TileGridLayout[] tileGridLayouts;
	protected final ZoomIntervalConfiguration zoomIntervalConfiguration;
	protected final int bboxEnlargement;

	// accounting
	protected float[] countWays;
	protected float[] countWayTileFactor;

	protected final TShortIntHashMap histogramPoiTags;
	protected final TShortIntHashMap histogramWayTags;

	public BaseTileBasedDataStore(
			double minLat, double maxLat,
			double minLon, double maxLon,
			ZoomIntervalConfiguration zoomIntervalConfiguration, int bboxEnlargement) {
		this(new Rect(minLon, maxLon, minLat, maxLat), zoomIntervalConfiguration,
				bboxEnlargement);

	}

	public BaseTileBasedDataStore(Rect bbox,
			ZoomIntervalConfiguration zoomIntervalConfiguration, int bboxEnlargement) {
		super();
		this.boundingbox = bbox;
		this.zoomIntervalConfiguration = zoomIntervalConfiguration;
		this.tileGridLayouts = new TileGridLayout[zoomIntervalConfiguration
				.getNumberOfZoomIntervals()];
		this.bboxEnlargement = bboxEnlargement;
		this.countWays = new float[zoomIntervalConfiguration.getNumberOfZoomIntervals()];
		this.countWayTileFactor = new float[zoomIntervalConfiguration
				.getNumberOfZoomIntervals()];

		this.histogramPoiTags = new TShortIntHashMap();
		this.histogramWayTags = new TShortIntHashMap();

		// compute horizontal and vertical tile coordinate offsets for all
		// base zoom levels
		for (int i = 0; i < zoomIntervalConfiguration.getNumberOfZoomIntervals(); i++) {
			TileCoordinate upperLeft =
					new TileCoordinate((int) MercatorProjection.longitudeToTileX(
							GeoCoordinate.intToDouble(boundingbox.minLongitudeE6),
							zoomIntervalConfiguration.getBaseZoom(i)),
							(int) MercatorProjection.latitudeToTileY(
									GeoCoordinate.intToDouble(boundingbox.maxLatitudeE6),
									zoomIntervalConfiguration.getBaseZoom(i)),
								zoomIntervalConfiguration.getBaseZoom(i));
			this.tileGridLayouts[i] = new TileGridLayout(upperLeft,
					computeNumberOfHorizontalTiles(i), computeNumberOfVerticalTiles(i));
		}
	}

	@Override
	public Rect getBoundingBox() {
		return boundingbox;
	}

	@Override
	public TileGridLayout getTileGridLayout(int zoomIntervalIndex) {
		return tileGridLayouts[zoomIntervalIndex];
	}

	@Override
	public ZoomIntervalConfiguration getZoomIntervalConfiguration() {
		return zoomIntervalConfiguration;
	}

	@Override
	public long cumulatedNumberOfTiles() {
		long cumulated = 0;
		for (int i = 0; i < zoomIntervalConfiguration.getNumberOfZoomIntervals(); i++) {
			cumulated += tileGridLayouts[i].getAmountTilesHorizontal() * tileGridLayouts[i]
					.getAmountTilesVertical();
		}
		return cumulated;
	}

	protected void countPoiTags(TDNode poi) {
		if (poi == null || poi.getTags() == null)
			return;
		for (short tag : poi.getTags()) {
			histogramPoiTags.adjustOrPutValue(tag, 1, 1);
		}
	}

	protected void countWayTags(TDWay way) {
		if (way == null || way.getTags() == null)
			return;
		for (short tag : way.getTags()) {
			histogramWayTags.adjustOrPutValue(tag, 1, 1);
		}
	}

	protected void countWayTags(short[] tags) {
		if (tags == null)
			return;
		for (short tag : tags) {
			histogramWayTags.adjustOrPutValue(tag, 1, 1);
		}
	}

	private int computeNumberOfHorizontalTiles(int zoomIntervalIndex) {
		long tileCoordinateLeft = MercatorProjection.longitudeToTileX(
				GeoCoordinate.intToDouble(boundingbox.getMinLongitudeE6()),
				zoomIntervalConfiguration.getBaseZoom(zoomIntervalIndex));

		long tileCoordinateRight = MercatorProjection.longitudeToTileX(
				GeoCoordinate.intToDouble(boundingbox.getMaxLongitudeE6()),
				zoomIntervalConfiguration.getBaseZoom(zoomIntervalIndex));

		assert tileCoordinateLeft <= tileCoordinateRight;
		assert tileCoordinateLeft - tileCoordinateRight + 1 < Integer.MAX_VALUE;

		LOGGER.finer("basezoom: " + zoomIntervalConfiguration.getBaseZoom(zoomIntervalIndex)
				+ "\t+n_horizontal: " + (tileCoordinateRight - tileCoordinateLeft + 1));

		return (int) (tileCoordinateRight - tileCoordinateLeft + 1);

	}

	private int computeNumberOfVerticalTiles(int zoomIntervalIndex) {
		long tileCoordinateBottom = MercatorProjection.latitudeToTileY(
				GeoCoordinate.intToDouble(boundingbox.getMinLatitudeE6()),
				zoomIntervalConfiguration.getBaseZoom(zoomIntervalIndex));

		long tileCoordinateTop = MercatorProjection.latitudeToTileY(
				GeoCoordinate.intToDouble(boundingbox.getMaxLatitudeE6()),
				zoomIntervalConfiguration.getBaseZoom(zoomIntervalIndex));

		assert tileCoordinateBottom >= tileCoordinateTop;
		assert tileCoordinateBottom - tileCoordinateTop + 1 <= Integer.MAX_VALUE;

		LOGGER.finer("basezoom: " + zoomIntervalConfiguration.getBaseZoom(zoomIntervalIndex)
				+ "\t+n_vertical: " + (tileCoordinateBottom - tileCoordinateTop + 1));

		return (int) (tileCoordinateBottom - tileCoordinateTop + 1);
	}
}
