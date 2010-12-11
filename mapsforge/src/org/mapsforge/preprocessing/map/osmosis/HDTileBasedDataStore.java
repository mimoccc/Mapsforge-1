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
package org.mapsforge.preprocessing.map.osmosis;

import gnu.trove.iterator.TLongIterator;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.set.hash.TLongHashSet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.mapsforge.core.GeoCoordinate;
import org.mapsforge.core.MercatorProjection;
import org.mapsforge.core.Rect;
import org.mapsforge.preprocessing.map.osmosis.TileData.TDNode;
import org.mapsforge.preprocessing.map.osmosis.TileData.TDWay;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.filter.common.IdTracker;
import org.openstreetmap.osmosis.core.filter.common.IdTrackerFactory;
import org.openstreetmap.osmosis.core.filter.common.IdTrackerType;
import org.openstreetmap.osmosis.core.lifecycle.ReleasableIterator;
import org.openstreetmap.osmosis.core.store.IndexedObjectStore;
import org.openstreetmap.osmosis.core.store.IndexedObjectStoreReader;
import org.openstreetmap.osmosis.core.store.SimpleObjectStore;
import org.openstreetmap.osmosis.core.store.SingleClassObjectSerializationFactory;

class HDTileBasedDataStore extends BaseTileBasedDataStore {

	private final IndexedObjectStore<Node> indexedNodeStore;
	private final IndexedObjectStore<Way> indexedWayStore;
	private final SimpleObjectStore<Way> wayStore;
	private final HDTileData[][][] tileData;
	private final HashMap<TileCoordinate, TLongHashSet> tilesToCoastlines;
	private final TLongObjectHashMap<TLongHashSet> multipolygons;
	private final TLongObjectHashMap<EnumSet<WayEnum>> multipolygonTags;
	private final IdTracker innerWayTracker;
	private final IdTracker multipolygonTracker;

	private IndexedObjectStoreReader<Node> nodeIndexReader;
	private IndexedObjectStoreReader<Way> wayIndexReader;

	private HDTileBasedDataStore(
			double minLat, double maxLat,
			double minLon, double maxLon,
			ZoomIntervalConfiguration zoomIntervalConfiguration) {
		this(new Rect(minLon, maxLon, minLat, maxLat), zoomIntervalConfiguration);
	}

	private HDTileBasedDataStore(Rect bbox, ZoomIntervalConfiguration zoomIntervalConfiguration) {
		super(bbox, zoomIntervalConfiguration);
		indexedNodeStore = new IndexedObjectStore<Node>(
				new SingleClassObjectSerializationFactory(
						Node.class), "idxNodes");
		indexedWayStore = new IndexedObjectStore<Way>(
				new SingleClassObjectSerializationFactory(
						Way.class), "idxWays");
		wayStore = new SimpleObjectStore<Way>(new SingleClassObjectSerializationFactory(
				Way.class), "heapWays", true);

		tileData = new HDTileData[zoomIntervalConfiguration.getNumberOfZoomIntervals()][][];
		for (int i = 0; i < zoomIntervalConfiguration.getNumberOfZoomIntervals(); i++) {
			this.tileData[i] = new HDTileData[tileGridLayouts[i].getAmountTilesHorizontal()][tileGridLayouts[i]
					.getAmountTilesVertical()];
		}

		tilesToCoastlines = new HashMap<TileCoordinate, TLongHashSet>();
		multipolygons = new TLongObjectHashMap<TLongHashSet>();
		multipolygonTags = new TLongObjectHashMap<EnumSet<WayEnum>>();
		innerWayTracker = IdTrackerFactory.createInstance(IdTrackerType.IdList);
		multipolygonTracker = IdTrackerFactory.createInstance(IdTrackerType.IdList);
	}

	static HDTileBasedDataStore newInstance(Rect bbox,
			ZoomIntervalConfiguration zoomIntervalConfiguration) {
		return new HDTileBasedDataStore(bbox, zoomIntervalConfiguration);
	}

	static HDTileBasedDataStore newInstance(double minLat, double maxLat,
			double minLon, double maxLon, ZoomIntervalConfiguration zoomIntervalConfiguration) {
		return new HDTileBasedDataStore(minLat, maxLat, minLon, maxLon,
				zoomIntervalConfiguration);
	}

	static HDTileBasedDataStore getStandardInstance(
			double minLat, double maxLat,
			double minLon, double maxLon) {

		return new HDTileBasedDataStore(
				minLat, maxLat, minLon, maxLon,
				ZoomIntervalConfiguration.getStandardConfiguration());
	}

	@Override
	public boolean addNode(Node node) {
		indexedNodeStore.add(node.getId(), node);

		TDNode tdNode = TDNode.fromNode(node);
		if (tdNode.isPOI()) {
			byte minZoomLevel = tdNode.getMinimumZoomLevel();
			if (minZoomLevel > zoomIntervalConfiguration.getMaxMaxZoom())
				minZoomLevel = zoomIntervalConfiguration.getMaxMaxZoom();
			for (int i = 0; i < zoomIntervalConfiguration.getNumberOfZoomIntervals(); i++) {

				// is poi seen in a zoom interval?
				if (minZoomLevel <= zoomIntervalConfiguration.getMaxZoom(i)) {
					long tileCoordinateX = MercatorProjection.longitudeToTileX(
							GeoCoordinate.intToDouble(tdNode.getLongitude()),
							zoomIntervalConfiguration.getBaseZoom(i));
					long tileCoordinateY = MercatorProjection.latitudeToTileY(
							GeoCoordinate.intToDouble(tdNode.getLatitude()),
							zoomIntervalConfiguration.getBaseZoom(i));
					HDTileData htd = getHDTile(i, (int) tileCoordinateX, (int) tileCoordinateY);
					if (htd != null) {
						htd.addPOI(tdNode.getId());
					}
				}
			}
		}

		return true;
	}

	@Override
	public boolean addWay(Way way) {
		wayStore.add(way);
		indexedWayStore.add(way.getId(), way);
		return true;
	}

	@Override
	public List<TDWay> getInnerWaysOfMultipolygon(long outerWayID) {
		TLongHashSet innerwayIDs = multipolygons.get(outerWayID);
		List<TDWay> innerways = new ArrayList<TileData.TDWay>();
		TLongIterator it = innerwayIDs.iterator();
		while (it.hasNext()) {
			TDWay innerWay = TDWay.fromWay(wayIndexReader.get(it.next()), this);
			if (innerWay != null)
				innerways.add(innerWay);
		}

		return innerways;
	}

	@Override
	public boolean addWayMultipolygon(long outerWayID, long[] innerWayIDs,
			EnumSet<WayEnum> relationTags) {

		TLongHashSet iw = multipolygons.get(outerWayID);
		if (iw == null) {
			iw = new TLongHashSet();
			multipolygons.put(outerWayID, iw);
		}
		iw.addAll(innerWayIDs);
		if (relationTags != null && relationTags.size() > 0) {
			EnumSet<WayEnum> tags = multipolygonTags.get(outerWayID);
			if (tags != null) {
				relationTags.addAll(tags);
			}
			multipolygonTags.put(outerWayID, relationTags);
		}

		multipolygonTracker.set(outerWayID);

		for (long iwID : innerWayIDs) {
			innerWayTracker.set(iwID);
		}

		return true;
	}

	@Override
	public TileData getTile(int baseZoomIndex, int tileCoordinateX, int tileCoordinateY) {
		HDTileData hdt = getHDTile(baseZoomIndex, tileCoordinateX, tileCoordinateY);
		if (hdt == null)
			return new TileData();

		return fromHDTileData(hdt);
	}

	@Override
	public Set<TDWay> getCoastLines(TileCoordinate tc) {
		if (wayIndexReader == null)
			throw new IllegalStateException("way store not accessible, call complete() first");

		TLongHashSet coastlines = tilesToCoastlines.get(tc);
		if (coastlines == null)
			return Collections.emptySet();

		TLongIterator it = coastlines.iterator();
		HashSet<TDWay> coastlinesAsTDWay = new HashSet<TileData.TDWay>(coastlines.size());
		while (it.hasNext()) {
			TDWay tdWay = TDWay.fromWay(wayIndexReader.get(it.next()), this);
			if (tdWay != null)
				coastlinesAsTDWay.add(tdWay);
		}
		return coastlinesAsTDWay;
	}

	@Override
	public TDNode getEntity(long id) {
		if (nodeIndexReader == null)
			throw new IllegalStateException("node store not accessible, call complete() first");

		return TDNode.fromNode(nodeIndexReader.get(id));
	}

	@Override
	public void complete() {
		indexedNodeStore.complete();
		nodeIndexReader = indexedNodeStore.createReader();

		indexedWayStore.complete();
		wayIndexReader = indexedWayStore.createReader();

		ReleasableIterator<Way> wayReader = wayStore.iterate();
		while (wayReader.hasNext()) {
			TDWay way = TDWay.fromWay(wayReader.next(), this);
			if (way == null)
				continue;

			if (way.getTags() != null && way.getTags().contains(WayEnum.NATURAL$COASTLINE)) {
				Set<TileCoordinate> coastLineTiles = GeoUtils.mapWayToTiles(way,
						TileInfo.TILE_INFO_ZOOMLEVEL);// find matching tiles on zoom level 12
				for (TileCoordinate tileCoordinate : coastLineTiles) {
					TLongHashSet coastlines = tilesToCoastlines.get(tileCoordinate);
					if (coastlines == null) {
						coastlines = new TLongHashSet();
						tilesToCoastlines.put(tileCoordinate, coastlines);
					}
					coastlines.add(way.getId());
				}
			}

			byte minZoomLevel = way.getMinimumZoomLevel();
			if (minZoomLevel > zoomIntervalConfiguration.getMaxMaxZoom())
				minZoomLevel = zoomIntervalConfiguration.getMaxMaxZoom();
			for (int i = 0; i < zoomIntervalConfiguration.getNumberOfZoomIntervals(); i++) {
				// is way seen in a zoom interval?
				if (minZoomLevel <= zoomIntervalConfiguration.getMaxZoom(i)) {
					Set<TileCoordinate> matchedTiles = GeoUtils.mapWayToTiles(way,
							zoomIntervalConfiguration.getBaseZoom(i));
					for (TileCoordinate matchedTile : matchedTiles) {
						HDTileData hdt = getHDTile(i, matchedTile.getX(), matchedTile.getY());
						if (hdt != null)
							hdt.addWay(way.getId());
					}
				}
			}

		}

	}

	private HDTileData getHDTile(int baseZoomIndex, int tileCoordinateX, int tileCoordinateY) {
		int tileCoordinateXIndex = (tileCoordinateX - tileGridLayouts[baseZoomIndex]
				.getUpperLeft()
				.getX());
		int tileCoordinateYIndex = (tileCoordinateY - tileGridLayouts[baseZoomIndex]
				.getUpperLeft()
				.getY());
		// check for valid range
		if (tileCoordinateXIndex < 0 || tileCoordinateYIndex < 0 ||
				tileData[baseZoomIndex].length <= tileCoordinateXIndex ||
				tileData[baseZoomIndex][0].length <= tileCoordinateYIndex)
			return null;

		HDTileData td = tileData[baseZoomIndex][tileCoordinateXIndex][tileCoordinateYIndex];
		if (td == null) {
			td = tileData[baseZoomIndex][tileCoordinateXIndex][tileCoordinateYIndex] = new HDTileData();
		}

		return td;
	}

	private TileData fromHDTileData(HDTileData hdt) {
		final TileData td = new TileData();
		TLongIterator it = hdt.getPois().iterator();
		while (it.hasNext()) {
			td.addPOI(TDNode.fromNode(nodeIndexReader.get(it.next())));
		}

		it = hdt.getWays().iterator();
		while (it.hasNext()) {
			TDWay way = TDWay.fromWay(wayIndexReader.get(it.next()), HDTileBasedDataStore.this);
			if (way == null)
				continue;
			if (!innerWayTracker.get(way.getId())) {
				if (multipolygonTracker.get(way.getId())) {
					if (way.getWayNodes() != null
							&& way.getWayNodes().length >= 4
							&& way.getWayNodes()[0].getId() == way.getWayNodes()[way
									.getWayNodes().length - 1].getId())
						way.setWaytype((short) 3);

					EnumSet<WayEnum> relationTags = multipolygonTags.get(way.getId());
					if (relationTags != null) {
						way.getTags().addAll(relationTags);
					}
				}
				td.addWay(way);
			} else {
				// TODO do not mark remove inner ways they contain
				// other tags than the outer way
			}
		}

		return td;
	}

	private class HDTileData {
		private final TLongArrayList pois;
		private final TLongArrayList ways;

		HDTileData() {
			pois = new TLongArrayList();
			ways = new TLongArrayList();
		}

		void addPOI(long id) {
			pois.add(id);
		}

		void addWay(long id) {
			ways.add(id);
		}

		TLongArrayList getPois() {
			return pois;
		}

		TLongArrayList getWays() {
			return ways;
		}
	}

}
