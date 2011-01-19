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

import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.hash.TLongObjectHashMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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

class RAMTileBasedDataStore extends BaseTileBasedDataStore {
	private static final Logger logger =
			Logger.getLogger(TileBasedDataStore.class.getName());

	private TLongObjectHashMap<TDNode> nodes;
	protected TLongObjectHashMap<TDWay> ways;
	protected TLongObjectHashMap<TLongArrayList> multipolygons;
	protected TileData[][][] tileData;
	private final HashMap<TileCoordinate, Set<TDWay>> tilesToCoastlines;

	// private TileData[][][] wayTileData;

	private RAMTileBasedDataStore(
			double minLat, double maxLat,
			double minLon, double maxLon,
			ZoomIntervalConfiguration zoomIntervalConfiguration, int bboxEnlargement) {
		this(new Rect(minLon, maxLon, minLat, maxLat), zoomIntervalConfiguration,
				bboxEnlargement);
	}

	private RAMTileBasedDataStore(Rect bbox,
			ZoomIntervalConfiguration zoomIntervalConfiguration, int bboxEnlargement) {
		super(bbox, zoomIntervalConfiguration, bboxEnlargement);
		this.nodes = new TLongObjectHashMap<TDNode>();
		this.ways = new TLongObjectHashMap<TDWay>();
		this.multipolygons = new TLongObjectHashMap<TLongArrayList>();
		this.tileData = new TileData[zoomIntervalConfiguration.getNumberOfZoomIntervals()][][];
		// compute number of tiles needed on each base zoom level
		for (int i = 0; i < zoomIntervalConfiguration.getNumberOfZoomIntervals(); i++) {
			this.tileData[i] = new TileData[tileGridLayouts[i].getAmountTilesHorizontal()][tileGridLayouts[i]
					.getAmountTilesVertical()];
		}
		this.tilesToCoastlines = new HashMap<TileCoordinate, Set<TDWay>>();
	}

	static RAMTileBasedDataStore newInstance(Rect bbox,
			ZoomIntervalConfiguration zoomIntervalConfiguration, int bboxEnlargement) {
		return new RAMTileBasedDataStore(bbox, zoomIntervalConfiguration, bboxEnlargement);
	}

	static RAMTileBasedDataStore newInstance(double minLat, double maxLat,
			double minLon, double maxLon, ZoomIntervalConfiguration zoomIntervalConfiguration,
			int bboxEnlargement) {
		return new RAMTileBasedDataStore(minLat, maxLat, minLon, maxLon,
				zoomIntervalConfiguration, bboxEnlargement);
	}

	static RAMTileBasedDataStore getStandardInstance(
			double minLat, double maxLat,
			double minLon, double maxLon, int bboxEnlargement) {

		return new RAMTileBasedDataStore(
				minLat, maxLat, minLon, maxLon,
				ZoomIntervalConfiguration.getStandardConfiguration(), bboxEnlargement);
	}

	@Override
	public TDNode getEntity(long id) {
		return nodes.get(id);
	}

	@Override
	public Rect getBoundingBox() {
		return boundingbox;
	}

	@Override
	public ZoomIntervalConfiguration getZoomIntervalConfiguration() {
		return zoomIntervalConfiguration;
	}

	@Override
	public List<TDWay> getInnerWaysOfMultipolygon(long outerWayID) {
		TLongArrayList innerwayIDs = multipolygons.get(outerWayID);
		return getInnerWaysOfMultipolygon(innerwayIDs.toArray());
	}

	private List<TDWay> getInnerWaysOfMultipolygon(long[] innerWayIDs) {
		if (innerWayIDs == null)
			return Collections.emptyList();
		List<TDWay> res = new ArrayList<TileData.TDWay>();
		for (long id : innerWayIDs) {
			TDWay current = ways.get(id);
			if (current == null)
				continue;
			res.add(current);
		}

		return res;
	}

	@Override
	public boolean addNode(Node node) {
		TDNode tdNode = TDNode.fromNode(node);
		nodes.put(tdNode.getId(), tdNode);
		if (tdNode.isPOI())
			addPOI(tdNode);
		return true;
	}

	private void addPOI(TDNode node) {
		byte minZoomLevel = node.getMinimumZoomLevel();
		for (int i = 0; i < zoomIntervalConfiguration.getNumberOfZoomIntervals(); i++) {

			// is poi seen in a zoom interval?
			if (minZoomLevel <= zoomIntervalConfiguration.getMaxZoom(i)) {
				long tileCoordinateX = MercatorProjection.longitudeToTileX(
						GeoCoordinate.intToDouble(node.getLongitude()),
						zoomIntervalConfiguration.getBaseZoom(i));
				long tileCoordinateY = MercatorProjection.latitudeToTileY(
						GeoCoordinate.intToDouble(node.getLatitude()),
						zoomIntervalConfiguration.getBaseZoom(i));
				//
				// System.out.println("adding poi: " + tileCoordinateX + "\t" + tileCoordinateY
				// + "\t" + zoomIntervalConfiguration.getBaseZoom(i));
				// System.out.println(node);
				TileData td = getTile(i, (int) tileCoordinateX, (int) tileCoordinateY);
				if (td != null) {
					td.addPOI(node);
				}
			}
		}
	}

	@Override
	public boolean addWay(Way way) {
		TDWay tdWay = TDWay.fromWay(way, this);
		if (tdWay == null)
			return false;
		this.ways.put(tdWay.getId(), tdWay);
		byte minZoomLevel = tdWay.getMinimumZoomLevel();
		int bboxEnlargementLocal = bboxEnlargement;

		if (tdWay.getTags() != null && tdWay.getTags().contains(WayEnum.NATURAL$COASTLINE)) {
			bboxEnlargementLocal = 0;
			// find matching tiles on zoom level 12
			Set<TileCoordinate> coastLineTiles = GeoUtils.mapWayToTiles(tdWay,
					TileInfo.TILE_INFO_ZOOMLEVEL, bboxEnlargementLocal);
			for (TileCoordinate tileCoordinate : coastLineTiles) {
				Set<TDWay> coastlines = tilesToCoastlines.get(tileCoordinate);
				if (coastlines == null) {
					coastlines = new HashSet<TDWay>();
					tilesToCoastlines.put(tileCoordinate, coastlines);
				}
				coastlines.add(tdWay);
			}
		}

		for (int i = 0; i < zoomIntervalConfiguration.getNumberOfZoomIntervals(); i++) {
			// is way seen in a zoom interval?
			if (minZoomLevel <= zoomIntervalConfiguration.getMaxZoom(i)) {
				Set<TileCoordinate> matchedTiles = GeoUtils.mapWayToTiles(tdWay,
						zoomIntervalConfiguration.getBaseZoom(i),
						bboxEnlargementLocal);
				boolean added = false;
				for (TileCoordinate matchedTile : matchedTiles) {
					TileData td = getTile(i, matchedTile.getX(), matchedTile.getY());
					if (td != null) {
						countWayTileFactor[i]++;
						added = true;
						td.addWay(tdWay);
					}
				}
				if (added)
					countWays[i]++;
			}
		}

		return true;
	}

	@Override
	public boolean addWayMultipolygon(long outerWayID, long[] innerWayIDs,
			EnumSet<WayEnum> relationTags) {
		TDWay outerWay = ways.get(outerWayID);
		// check if outer way exists
		if (outerWay == null) {
			logger.finer("outer way with id " + outerWayID + " not existent in relation");
			return false;
		}
		// check if outer way is polygon
		if (!GeoUtils.isClosedPolygon(outerWay)) {
			logger.finer("outer way is not a polygon, id: " + outerWayID);
			return false;
		}

		// check if all inner ways exist
		List<TDWay> innerWays = getInnerWaysOfMultipolygon(innerWayIDs);
		if (innerWays.size() < innerWayIDs.length) {
			logger.finer("some inner ways are missing for outer way with id " + outerWayID);
			return false;
		}

		// add relation tags to outer way
		if (outerWay.getTags() == null) {
			if (relationTags.size() > 0)
				outerWay.setTags(relationTags);
		} else
			outerWay.getTags().addAll(relationTags);

		for (Iterator<TDWay> innerWaysIterator = innerWays.iterator(); innerWaysIterator
				.hasNext();) {
			TDWay innerWay = innerWaysIterator.next();
			// remove all tags from the inner way that are already present in the outer way
			if (outerWay.getTags() != null && innerWay.getTags() != null) {
				innerWay.getTags().removeAll(outerWay.getTags());
			}
			// only remove from normal ways, if the inner way has no tags other than the
			// outer way
			if (innerWay.getTags() == null || innerWay.getTags().size() == 0) {
				for (int i = 0; i < zoomIntervalConfiguration.getNumberOfZoomIntervals(); i++) {
					Set<TileCoordinate> associatedTiles = GeoUtils.mapWayToTiles(innerWay,
							zoomIntervalConfiguration.getBaseZoom(i), bboxEnlargement);
					if (associatedTiles == null)
						continue;
					for (TileCoordinate associatedTile : associatedTiles) {
						TileData td = getTile(i, associatedTile.getX(), associatedTile.getY());
						if (td != null)
							td.removeWay(innerWay);
					}
				}
			}
			// the inner way has tags other than the outer way --> must be rendered as normal
			// way, remove it from list of inner ways
			else {
				innerWaysIterator.remove();
			}
		}

		// only change way type to multipolygon if inner ways are existent
		if (innerWays.size() > 0) {
			TLongArrayList innerWayIDList = multipolygons.get(outerWayID);
			if (innerWayIDList == null) {
				innerWayIDList = new TLongArrayList();
			}
			innerWayIDList.add(innerWayIDs);
			multipolygons.put(outerWayID, innerWayIDList);

			// TODO document this side effect

			outerWay.setWaytype((short) 3);
		}

		return true;
	}

	@Override
	public TileData getTile(int baseZoomIndex, int tileCoordinateX, int tileCoordinateY) {
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

		TileData td = tileData[baseZoomIndex][tileCoordinateXIndex][tileCoordinateYIndex];
		if (td == null) {
			td = tileData[baseZoomIndex][tileCoordinateXIndex][tileCoordinateYIndex] = new TileData();
		}

		return td;
	}

	@Override
	public Set<TDWay> getCoastLines(TileCoordinate tc) {
		if (tc.getZoomlevel() <= TileInfo.TILE_INFO_ZOOMLEVEL)
			return Collections.emptySet();
		TileCoordinate correspondingOceanTile = tc.translateToZoomLevel((byte) 12).get(0);
		Set<TDWay> coastlines = tilesToCoastlines.get(correspondingOceanTile);
		if (coastlines == null)
			coastlines = Collections.emptySet();
		return coastlines;
	}

	@Override
	public void complete() {
		int count = 0;
		for (Set<TDWay> coastlines : tilesToCoastlines.values()) {
			count += coastlines.size();
		}

		logger.info("number of coastlines: " + count);
		for (int i = 0; i < countWays.length; i++) {
			logger.info("zoom-interval " + i + ", added ways: " + countWays[i]);
			logger.info("zoom-interval " + i + ", average tiles per way: "
					+ (countWayTileFactor[i] / countWays[i]));
		}

	}

}
