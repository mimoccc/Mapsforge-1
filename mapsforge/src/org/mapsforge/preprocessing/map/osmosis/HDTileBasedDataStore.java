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

import gnu.trove.iterator.TLongIterator;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.set.TShortSet;
import gnu.trove.set.hash.TLongHashSet;
import gnu.trove.set.hash.TShortHashSet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

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
import org.openstreetmap.osmosis.core.store.NoSuchIndexElementException;
import org.openstreetmap.osmosis.core.store.SimpleObjectStore;
import org.openstreetmap.osmosis.core.store.SingleClassObjectSerializationFactory;

/**
 * A TileBasedDataStore that uses the hard disk as storage device for temporary data structures.
 * 
 * @author bross
 * 
 */
final class HDTileBasedDataStore extends BaseTileBasedDataStore {

	private static final Logger LOGGER =
			Logger.getLogger(HDTileBasedDataStore.class.getName());

	private final IndexedObjectStore<Node> indexedNodeStore;
	private final IndexedObjectStore<Way> indexedWayStore;
	private final SimpleObjectStore<Way> wayStore;
	private final HDTileData[][][] tileData;
	private final HashMap<TileCoordinate, TLongHashSet> tilesToCoastlines;
	private final TLongObjectHashMap<TLongHashSet> multipolygons;
	private final TLongObjectHashMap<TShortSet> multipolygonTags;
	private final IdTracker innerWayTracker;
	private final IdTracker innerWaysWithAdditionalTags;
	private final IdTracker multipolygonTracker;

	private IndexedObjectStoreReader<Node> nodeIndexReader;
	private IndexedObjectStoreReader<Way> wayIndexReader;

	private HDTileBasedDataStore(
			double minLat, double maxLat,
			double minLon, double maxLon,
			ZoomIntervalConfiguration zoomIntervalConfiguration, int bboxEnlargement) {
		this(new Rect(minLon, maxLon, minLat, maxLat), zoomIntervalConfiguration,
				bboxEnlargement);
	}

	private HDTileBasedDataStore(Rect bbox,
			ZoomIntervalConfiguration zoomIntervalConfiguration, int bboxEnlargement) {
		super(bbox, zoomIntervalConfiguration, bboxEnlargement);
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
		multipolygonTags = new TLongObjectHashMap<TShortSet>();
		innerWayTracker = IdTrackerFactory.createInstance(IdTrackerType.IdList);
		innerWaysWithAdditionalTags = IdTrackerFactory.createInstance(IdTrackerType.IdList);
		multipolygonTracker = IdTrackerFactory.createInstance(IdTrackerType.IdList);
	}

	static HDTileBasedDataStore newInstance(Rect bbox,
			ZoomIntervalConfiguration zoomIntervalConfiguration, int bboxEnlargement) {
		return new HDTileBasedDataStore(bbox, zoomIntervalConfiguration, bboxEnlargement);
	}

	static HDTileBasedDataStore newInstance(double minLat, double maxLat,
			double minLon, double maxLon, ZoomIntervalConfiguration zoomIntervalConfiguration,
			int bboxEnlargement) {
		return new HDTileBasedDataStore(minLat, maxLat, minLon, maxLon,
				zoomIntervalConfiguration, bboxEnlargement);
	}

	static HDTileBasedDataStore getStandardInstance(
			double minLat, double maxLat,
			double minLon, double maxLon, int bboxEnlargement) {

		return new HDTileBasedDataStore(
				minLat, maxLat, minLon, maxLon,
				ZoomIntervalConfiguration.getStandardConfiguration(), bboxEnlargement);
	}

	@Override
	public boolean addNode(Node node) {
		indexedNodeStore.add(node.getId(), node);

		TDNode tdNode = TDNode.fromNode(node);
		if (tdNode.isPOI()) {
			byte minZoomLevel = tdNode.getMinimumZoomLevel();
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
						countPoiTags(tdNode);
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
			TDWay innerWay = null;
			long id = it.next();
			try {
				innerWay = TDWay.fromWay(wayIndexReader.get(id), this);
			} catch (NoSuchIndexElementException e) {
				LOGGER.finer("multipolygon with rel-id " + outerWayID
						+ " references non-existing inner way " + id);
			}
			if (innerWay != null)
				innerways.add(innerWay);
		}

		return innerways;
	}

	@Override
	public boolean addWayMultipolygon(long outerWayID, long[] innerWayIDs,
			List<OSMTag> relationTags) {

		TLongHashSet iw = multipolygons.get(outerWayID);
		if (iw == null) {
			iw = new TLongHashSet();
			multipolygons.put(outerWayID, iw);
		}
		iw.addAll(innerWayIDs);
		if (relationTags != null && relationTags.size() > 0) {
			TShortSet tags = multipolygonTags.get(outerWayID);
			if (tags == null) {
				tags = new TShortHashSet();
			}
			tags.addAll(MapFileWriterTask.TAG_MAPPING.tagIDsFromList(relationTags));
			multipolygonTags.put(outerWayID, tags);
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
		if (tc.getZoomlevel() <= TileInfo.TILE_INFO_ZOOMLEVEL)
			return Collections.emptySet();
		TileCoordinate correspondingOceanTile = tc.translateToZoomLevel(
				TileInfo.TILE_INFO_ZOOMLEVEL).get(0);

		if (wayIndexReader == null)
			throw new IllegalStateException("way store not accessible, call complete() first");

		TLongHashSet coastlines = tilesToCoastlines.get(correspondingOceanTile);
		if (coastlines == null)
			return Collections.emptySet();

		TLongIterator it = coastlines.iterator();
		HashSet<TDWay> coastlinesAsTDWay = new HashSet<TileData.TDWay>(coastlines.size());
		while (it.hasNext()) {
			long id = it.next();
			TDWay tdWay = null;
			try {
				tdWay = TDWay.fromWay(wayIndexReader.get(id), this);
			} catch (NoSuchIndexElementException e) {
				LOGGER.finer("coastline way non-existing" + id);
			}
			if (tdWay != null)
				coastlinesAsTDWay.add(tdWay);
		}
		return coastlinesAsTDWay;
	}

	@Override
	public TDNode getEntity(long id) {
		if (nodeIndexReader == null)
			throw new IllegalStateException("node store not accessible, call complete() first");

		try {
			return TDNode.fromNode(nodeIndexReader.get(id));
		} catch (NoSuchIndexElementException e) {
			LOGGER.finer("node cannot be found in index: " + id);
			return null;
		}
	}

	// TODO add accounting of average number of tiles per way
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

			int bboxEnlargementLocal = bboxEnlargement;
			// COASTLINE DETECTION
			if (way.isCoastline()) {
				// find matching tiles on zoom level 12
				bboxEnlargementLocal = 0;
				Set<TileCoordinate> coastLineTiles = GeoUtils.mapWayToTiles(way,
						TileInfo.TILE_INFO_ZOOMLEVEL, bboxEnlargementLocal);
				for (TileCoordinate tileCoordinate : coastLineTiles) {
					TLongHashSet coastlines = tilesToCoastlines.get(tileCoordinate);
					if (coastlines == null) {
						coastlines = new TLongHashSet();
						tilesToCoastlines.put(tileCoordinate, coastlines);
					}
					coastlines.add(way.getId());
				}
			}

			// ADD WAY TO CORRESPONDING TILES
			byte minZoomLevel = way.getMinimumZoomLevel();
			for (int i = 0; i < zoomIntervalConfiguration.getNumberOfZoomIntervals(); i++) {
				// is way seen in a zoom interval?
				if (minZoomLevel <= zoomIntervalConfiguration.getMaxZoom(i)) {
					Set<TileCoordinate> matchedTiles = GeoUtils.mapWayToTiles(way,
							zoomIntervalConfiguration.getBaseZoom(i),
							bboxEnlargementLocal);
					for (TileCoordinate matchedTile : matchedTiles) {
						HDTileData hdt = getHDTile(i, matchedTile.getX(), matchedTile.getY());
						if (hdt != null) {
							hdt.addWay(way.getId());
							countWayTags(way);
						}
					}
				}
			}

			// REMOVE WAYS FROM LIST OF INNER WAYS IF THEY CONTAIN ADDITIONAL
			// TAGS TO THEIR CORRESPONDING OUTER WAY

			// look at all outer ways and fetch corresponding inner ways
			if (way.isPolygon() && multipolygonTracker.get(way.getId())) {
				// retrieve all tags of the outer way
				TShortSet relationTags = multipolygonTags.get(way.getId());
				if (relationTags == null)
					relationTags = new TShortHashSet();
				if (way.getTags() != null && way.getTags().length > 0) {
					relationTags.addAll(way.getTags());
				}

				// compare with tags of inner way
				List<TDWay> correspondingInnerWays = getInnerWaysOfMultipolygon(way.getId());
				TShortSet innerwayTagsSet = null;
				for (TDWay innerWay : correspondingInnerWays) {
					if (innerWay.getTags() == null || innerWay.getTags().length == 0)
						continue;
					innerwayTagsSet = new TShortHashSet(innerWay.getTags());
					if (!relationTags.containsAll(innerwayTagsSet)) {
						innerWaysWithAdditionalTags.set(innerWay.getId());
					}
				}
			}

		}

		MapFileWriterTask.TAG_MAPPING.optimizePoiOrdering(histogramPoiTags);
		MapFileWriterTask.TAG_MAPPING.optimizeWayOrdering(histogramWayTags);
	}

	@Override
	public void release() {
		this.indexedNodeStore.release();
		this.indexedWayStore.release();
		this.wayStore.release();
	}

	private HDTileData getHDTile(int baseZoomIndex, int tileCoordinateX, int tileCoordinateY) {
		int tileCoordinateXIndex = tileCoordinateX - tileGridLayouts[baseZoomIndex]
				.getUpperLeft().getX();
		int tileCoordinateYIndex = tileCoordinateY - tileGridLayouts[baseZoomIndex]
				.getUpperLeft().getY();
		// check for valid range
		if (tileCoordinateXIndex < 0 || tileCoordinateYIndex < 0
				|| tileData[baseZoomIndex].length <= tileCoordinateXIndex
				|| tileData[baseZoomIndex][0].length <= tileCoordinateYIndex)
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
			TDWay way = null;
			long id = it.next();
			try {
				way = TDWay.fromWay(wayIndexReader.get(id), HDTileBasedDataStore.this);
			} catch (NoSuchIndexElementException e) {
				LOGGER.finer("referenced way non-existing" + id);
			}
			if (way == null)
				continue;
			if (!innerWayTracker.get(way.getId())) {
				if (multipolygonTracker.get(way.getId())) {
					// the way must be a valid polygon, i.e.
					// it must have been marked as such before
					// see TDWay.fromWay()
					if (!way.isPolygon()) {
						// encountered an invalid polygon
						LOGGER.finer("outer way is not a polygon, id: " + way.getId());
						continue;
					}

					way.setShape(TDWay.MULTI_POLYGON);
					TShortSet relationTags = multipolygonTags.get(way.getId());
					if (relationTags != null) {
						way.addTags(relationTags.toArray());
					}
				}

				// only add ways that are not inner ways
				// inner ways are retrieved through the getInnerWaysOfMultipolygonMethod
				td.addWay(way);
			} else {
				// we encountered an inner way
				// only add inner ways that have additional tags compared
				// to their outer way, we have checked for this during completion
				// and have marked such ways
				if (innerWaysWithAdditionalTags.get(way.getId())) {
					td.addWay(way);
				}
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
