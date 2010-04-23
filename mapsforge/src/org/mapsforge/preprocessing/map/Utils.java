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
package org.mapsforge.preprocessing.map;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;

class Utils {
	private static GeometryFactory geoFac = new GeometryFactory();
	private static final int[] tileBitMaskValues = new int[] { 32768, 16384, 2048, 1024, 8192,
			4096, 512, 256, 128, 64, 8, 4, 32, 16, 2, 1 };

	/**
	 * Get the bounding box of a tile.
	 * 
	 * @param tileX
	 *            the X number of the tile.
	 * @param tileY
	 *            the Y number of the tile.
	 * @param zoom
	 *            the zoom level at which the bounding box should be calculated.
	 * @return the bounding box of this tile.
	 */
	static LinearRing getBoundingBox(long tileX, long tileY, byte zoom) {
		double minLat = MercatorProjection.tileYToLatitude(tileY, zoom);
		double maxLat = MercatorProjection.tileYToLatitude(tileY + 1, zoom);
		double minLon = MercatorProjection.tileXToLongitude(tileX, zoom);
		double maxLon = MercatorProjection.tileXToLongitude(tileX + 1, zoom);

		return geoFac.createLinearRing(new Coordinate[] { new Coordinate(maxLat, minLon),
				new Coordinate(minLat, minLon), new Coordinate(minLat, maxLon),
				new Coordinate(maxLat, maxLon), new Coordinate(maxLat, minLon) });
	}

	/**
	 * Returns all subtiles of a given tile on a configurable higher zoom level. The resulting
	 * list is computed by recursively calling getSubtiles() on all four subtiles of the given
	 * tile. On each recursive call the second parameter is decremented by one until zero is
	 * reached.
	 * 
	 * @param currentTile
	 *            the starting tile for the computation of the subtiles.
	 * @param zoomLevelDistance
	 *            how many zoom levels higher the subtiles should be.
	 * @return a list containing all subtiles on the higher zoom level.
	 * @throws InvalidParameterException
	 *             if {@code zoomLevelDistance} is less than zero.
	 */
	static List<Tile> getSubtiles(Tile currentTile, byte zoomLevelDistance) {
		// check for valid zoomLevelDistance parameter
		if (zoomLevelDistance < 0) {
			throw new InvalidParameterException();
		}

		// create the list of tiles that will be returned
		List<Tile> childrenTiles = new ArrayList<Tile>();

		if (zoomLevelDistance == 0) {
			// add only the current tile to the list
			childrenTiles.add(currentTile);
		} else {
			// add all subtiles of the upper left subtile recursively
			childrenTiles.addAll(getSubtiles(new Tile(currentTile.x * 2, currentTile.y * 2,
					(byte) (currentTile.zoomLevel + 1)), (byte) (zoomLevelDistance - 1)));

			// add all subtiles of the upper right subtile recursively
			childrenTiles.addAll(getSubtiles(new Tile(currentTile.x * 2 + 1, currentTile.y * 2,
					(byte) (currentTile.zoomLevel + 1)), (byte) (zoomLevelDistance - 1)));

			// add all subtiles of the lower left subtile recursively
			childrenTiles.addAll(getSubtiles(new Tile(currentTile.x * 2, currentTile.y * 2 + 1,
					(byte) (currentTile.zoomLevel + 1)), (byte) (zoomLevelDistance - 1)));

			// add all subtiles of the lower right subtile recursively
			childrenTiles.addAll(getSubtiles(new Tile(currentTile.x * 2 + 1,
					currentTile.y * 2 + 1, (byte) (currentTile.zoomLevel + 1)),
					(byte) (zoomLevelDistance - 1)));
		}

		// return the tile list
		return childrenTiles;
	}

	/**
	 * Calculates for each way that is related to a given tile a tile bitmask. The bitmask
	 * determines for which tiles on zoom level initialTile.zoom+2 the way is needed for
	 * rendering.
	 * 
	 * @param initialTile
	 *            a tile for which all subtiles on zoom level initialTile.zoom+2 will be
	 *            calculated
	 * @param ways
	 *            all ways which are related to the given tile
	 * @return a map containing ways and the calculated tile bitmask
	 */
	static Map<MapElementWay, Short> getTileBitmasksForWays(Tile initialTile,
			Map<MapElementWay, Geometry> ways) {

		// get the needed subtiles
		List<Tile> childrenTiles = getSubtiles(initialTile, (byte) 2);

		Set<Entry<MapElementWay, Geometry>> entries = ways.entrySet();

		// create a result map for the ways and their tile bitmasks
		Map<MapElementWay, Short> result = new TreeMap<MapElementWay, Short>();

		MapElementWay currentElement;
		Geometry way;
		Tile tmpTile;

		// for each subtile
		for (int i = 0; i < childrenTiles.size(); i++) {
			tmpTile = childrenTiles.get(i);
			// get the bounding box of that subtile
			Geometry tileBBox = geoFac.createPolygon(getBoundingBox(tmpTile.x, tmpTile.y,
					tmpTile.zoomLevel), null);

			// for each way
			for (Entry<MapElementWay, Geometry> entry : entries) {
				currentElement = entry.getKey();
				way = entry.getValue();

				// check if the way is related to the bounding box
				if (way.crosses(tileBBox) || tileBBox.contains(way) || way.intersects(tileBBox)) {
					// add a specified value to the tile bitmask of the way and put it in the
					// result map
					result.put(currentElement,
							(currentElement.tileBitmask |= tileBitMaskValues[i]));
				}
			}
		}

		// return the result map
		return result;
	}

	/**
	 * Calculate all tiles on a certain zoom level which contain a given way.
	 * 
	 * @param way
	 *            the way
	 * @param wayNodes
	 *            a list of coordinates of the way nodes
	 * @param wayType
	 *            the type of the way, 1 = simple way, 2 = closed way/area, 3 = part of a
	 *            multipolygon
	 * @param zoom
	 *            the zoom level at which the tiles should be calculated
	 * @return a set of tiles which contain the whole way or a part of the way
	 * 
	 */
	static Set<Tile> wayToTiles(MapElementWay way, Coordinate[] wayNodes, int wayType, byte zoom) {
		Geometry geoWay;

		int minTileX;
		int minTileY;
		int maxTileX;
		int maxTileY;
		Coordinate min;
		Coordinate max;

		Set<Tile> wayTiles = new HashSet<Tile>();

		if (wayNodes.length < 2) {
			return wayTiles;
		}

		if (wayType == 1) {
			geoWay = geoFac.createLineString(wayNodes);
		} else {
			if (wayNodes.length < 4) {
				return wayTiles;
			}
			geoWay = geoFac.createPolygon(geoFac.createLinearRing(wayNodes), null);
			way.convexness = (int) ((geoWay.getArea() / geoWay.convexHull().getArea()) * 100);
		}

		// get the bounding box of the way
		Geometry boundingBox = geoWay.getEnvelope();
		Coordinate[] bBoxCoords = boundingBox.getCoordinates();

		if (bBoxCoords.length == 2) {
			int compare = bBoxCoords[0].compareTo(bBoxCoords[1]);

			if (compare == 1) {
				max = bBoxCoords[0];
				min = bBoxCoords[1];
			} else if (compare == 0) {
				max = bBoxCoords[0];
				min = bBoxCoords[0];
			} else {
				max = bBoxCoords[1];
				min = bBoxCoords[0];
			}

		} else {
			min = bBoxCoords[3];
			max = bBoxCoords[1];
		}

		// get the minimal and maximal tile coordinates
		minTileX = (int) MercatorProjection.longitudeToTileX(min.y, zoom);
		minTileY = (int) MercatorProjection.latitudeToTileY(min.x, zoom);

		maxTileX = (int) MercatorProjection.longitudeToTileX(max.y, zoom);
		maxTileY = (int) MercatorProjection.latitudeToTileY(max.x, zoom);

		// calculate the tile coordinates and the corresponding bounding boxes
		Map<Coordinate, Geometry> tiles = new HashMap<Coordinate, Geometry>();
		for (long i = minTileX; i <= maxTileX; i++) {
			for (long j = minTileY; j <= maxTileY; j++) {
				tiles.put(new Coordinate(i, j), geoFac.createPolygon(Utils.getBoundingBox(i, j,
						zoom), null));
			}
		}

		// check for every tile in the set if the tile contains the given way

		Set<Entry<Coordinate, Geometry>> set = tiles.entrySet();
		for (Entry<Coordinate, Geometry> e : set) {
			Coordinate c = e.getKey();
			Geometry currentTile = e.getValue();

			if (wayType == 1) {
				if (geoWay.crosses(currentTile) || geoWay.within(currentTile)
						|| geoWay.intersects(currentTile)) {
					wayTiles.add(new Tile((long) c.x, (long) c.y, zoom));
				}
			}
			if (wayType != 1) {
				if (currentTile.within(geoWay) || currentTile.intersects(geoWay)
						|| currentTile.contains(geoWay)) {
					wayTiles.add(new Tile((long) c.x, (long) c.y, zoom));
				}
			}
		}
		return wayTiles;
	}
}
