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
import java.util.Map.Entry;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;

class Utils {
	private static GeometryFactory geoFac = new GeometryFactory();
	private static final int[] tileBitMaskValues = new int[] { 32768, 16384, 2048, 1024, 8192,
			4096, 512, 256, 128, 64, 8, 4, 32, 16, 2, 1 };

	// private static final int[] tileBitMaskValues = new int[] { 8, 4, 2, 1 };

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

	static double[] getBoundingBoxArray(long tileX, long tileY, byte zoom) {
		double[] result = new double[4];

		double minLat = MercatorProjection.tileYToLatitude(tileY, zoom);
		double maxLat = MercatorProjection.tileYToLatitude(tileY + 1, zoom);
		double minLon = MercatorProjection.tileXToLongitude(tileX, zoom);
		double maxLon = MercatorProjection.tileXToLongitude(tileX + 1, zoom);

		result[0] = minLat;
		result[1] = maxLat;
		result[2] = minLon;
		result[3] = maxLon;

		return result;
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
	 * Calculate all tiles on a certain zoom level which contain a given way.
	 * 
	 * @param geoWay
	 *            the way
	 * @param wayType
	 *            the type of the way, 1 = simple way, 2 = closed way/area, 3 = part of a
	 *            multipolygon
	 * @param zoom
	 *            the zoom level at which the tiles should be calculated
	 * @return a set of tiles which contain the whole way or a part of the way
	 * 
	 */
	static Set<Tile> wayToTilesWay(Geometry geoWay, int wayType, byte zoom) {
		Set<Tile> wayTiles = new HashSet<Tile>();

		Tile parentTile = null;

		int minTileX;
		int minTileY;
		int maxTileX;
		int maxTileY;
		Coordinate min;
		Coordinate max;

		if (geoWay == null) {
			return wayTiles;
		}

		Geometry boundingBox = geoWay.getEnvelope();
		Coordinate[] bBoxCoords = boundingBox.getCoordinates();

		if (bBoxCoords.length == 1) {
			return wayTiles;
		}

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
			// System.out.println("tilecoordinates: " + c.x + " " + c.y);
			Geometry currentTile = e.getValue();
			if (wayType == 1) {
				if (geoWay.crosses(currentTile) || geoWay.within(currentTile)
						|| geoWay.intersects(currentTile) || currentTile.contains(geoWay)
						|| currentTile.contains(geoWay) || currentTile.intersects(geoWay)) {
					// System.out.println("crosses -1");
					parentTile = new Tile((long) c.x, (long) c.y, zoom);
					// System.out.println("parentTile: " + parentTile.toString());
					wayTiles.add(parentTile);
					// wayTiles.put(new Tile((long) c.x, (long) c.y, zoom), (short) 0);
				}
			}
			if (wayType != 1) {
				if (currentTile.within(geoWay) || currentTile.intersects(geoWay)
						|| currentTile.contains(geoWay) || geoWay.contains(currentTile)
						|| geoWay.crosses(currentTile)) {
					// System.out.println("crosses -2");
					parentTile = new Tile((long) c.x, (long) c.y, zoom);
					// System.out.println("parentTile: " + parentTile.toString());
					wayTiles.add(parentTile);
				}
			}
		}
		return wayTiles;
	}

	/**
	 * Creates a geometry which represents a way.
	 * 
	 * @param way
	 *            element that holds all information of a way
	 * @param wayNodes
	 *            the coordinates of the way nodes
	 * @return a geometry which represents a certain way
	 */
	static Geometry createWay(MapElementWay way, Coordinate[] wayNodes) {
		Geometry geoWay;
		if (wayNodes.length < 2) {
			return null;
		}

		if (way.wayType == 1) {
			geoWay = geoFac.createLineString(wayNodes);
		} else {
			if (wayNodes.length < 4) {
				return null;
			}
			geoWay = geoFac.createPolygon(geoFac.createLinearRing(wayNodes), null);
			way.convexness = (int) ((geoWay.getArea() / geoWay.convexHull().getArea()) * 100);
		}
		return geoWay;
	}

	/**
	 * Calculates for each way that is related to a given tile a tile bitmask. The bitmask
	 * determines for which tiles on zoom level initialTile.zoom+2 the way is needed for
	 * rendering.
	 * 
	 * @param geoWay
	 *            a certain way
	 * @param wayTiles
	 *            all tiles to which the way is related
	 * @param wayType
	 *            the type of the way, 1 = simple way, 2 = closed way/area, 3 = part of a
	 *            multipolygon
	 * @return a map containing tiles and the calculated tile bitmask
	 */
	static Map<Tile, Short> getTileBitMask(Geometry geoWay, Set<Tile> wayTiles, int wayType) {
		Map<Tile, Short> result = new HashMap<Tile, Short>();
		short tileCounter;
		short bitmap;
		short tmp = 0;

		Geometry subTile;

		if (geoWay == null) {
			return result;
		}

		for (Tile p : wayTiles) {
			List<Tile> currentSubTiles = getSubtiles(new Tile(p.x, p.y, p.zoomLevel), (byte) 2);
			tileCounter = 0;
			bitmap = 0;
			result.put(p, (short) 0);
			for (Tile csb : currentSubTiles) {
				subTile = geoFac.createPolygon(Utils.getBoundingBox(csb.x, csb.y,
						(byte) (p.zoomLevel + 2)), null);
				if (wayType == 1) {
					if (geoWay.crosses(subTile) || geoWay.within(subTile)
							|| geoWay.intersects(subTile)) {
						tmp = result.get(p);
						tmp |= tileBitMaskValues[tileCounter];
						result.put(p, tmp);
					}
				}
				if (wayType != 1) {
					if (subTile.within(geoWay) || subTile.intersects(geoWay)
							|| subTile.contains(geoWay)) {
						bitmap = bitmap |= tileBitMaskValues[tileCounter];
						result.put(p, bitmap);
					}
				}
				tileCounter++;
			}
		}
		return result;
	}

	/**
	 * Clips a given polygon to a clipping edge (here: edge of a tile).
	 * 
	 * (edges go from A to B)
	 * 
	 * @param tileA
	 *            first point of the clipping edge
	 * @param tileB
	 *            second point of the clipping edge
	 * @param polygonNodes
	 *            list of polygon nodes
	 * @return list of nodes of the clipped polygon
	 */
	static ArrayList<Coordinate> clipPolygonToTile(Coordinate tileA, Coordinate tileB,
			ArrayList<Coordinate> polygonNodes) {

		ArrayList<Coordinate> output = new ArrayList<Coordinate>();

		Coordinate lastPoint;
		Coordinate currentPoint;

		Coordinate tmpPoint;

		// get the last polygon point
		lastPoint = polygonNodes.get(polygonNodes.size() - 1);

		// for every point of the polygon
		for (int j = 0; j < polygonNodes.size(); j++) {
			// get the current point
			currentPoint = polygonNodes.get(j);

			if (pointInsideTile(currentPoint, tileA, tileB)) {
				// the current point lies inside the clipping region
				if (pointInsideTile(lastPoint, tileA, tileB)) {
					// the last point lies inside the clipping region
					// add the current point to the output list
					output.add(currentPoint);

				} else {
					// the last point lies outside the clipping region

					// calculate the intersection point of the edges (lastPoint,currentPoint)
					// and (tileA,tileB)
					tmpPoint = intersection(lastPoint, currentPoint, tileA, tileB);

					// add the intersection point to the output list
					output.add(tmpPoint);
					// add also the current point to the output list
					output.add(currentPoint);
				}
			} else {
				// the current point lies outside the clipping region
				if (pointInsideTile(lastPoint, tileA, tileB)) {
					// the lastPoint lies inside the clipping region

					// calculate the intersection point of the edges (lastPoint,currentPoint)
					// and (tileA,tileB)
					tmpPoint = intersection(lastPoint, currentPoint, tileA, tileB);
					// add the intersection point to the output list
					output.add(tmpPoint);
				}
				// if lastPoint and currentPoint are both outside the clipping region, no point
				// is added to the output list
			}
			// set lastPoint to currentPoint
			lastPoint = currentPoint;
		}

		return output;
	}

	/**
	 * calculates whether a given point lies inside the clipping region (i.e. on the left side
	 * of the clipping edge)
	 * 
	 * @param pointA
	 *            point for which the position should be tested
	 * @param tileA
	 *            first point of the clipping edge
	 * @param tileB
	 *            second point of the clipping edge
	 * @return true if the given point lies in the clipping region, else false
	 */
	static boolean pointInsideTile(Coordinate pointA, Coordinate tileA, Coordinate tileB) {
		// note: the vertices of the tile edges are ordered counterclockwise

		if (tileB.x > tileA.x) {
			// left line
			if (pointA.y <= tileA.y) {
				return true;
			}
		}
		if (tileB.x < tileA.x) {
			// right line
			if (pointA.y >= tileA.y) {
				return true;
			}
		}
		if (tileB.y > tileA.y) {
			// bottom line
			if (pointA.x >= tileB.x) {
				return true;
			}
		}
		if (tileB.y < tileA.y) {
			// top line
			if (pointA.x <= tileB.x) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Calculates the intersection point of two edges (here: an edge of the polygon and the
	 * current clipping edge
	 * 
	 * (the edges go from A to B)
	 * 
	 * @param polygonA
	 *            first point of the polygon edge
	 * @param polygonB
	 *            second point of the polygon edge
	 * @param tileA
	 *            first point of the clipping edge
	 * @param tileB
	 *            second point of the clipping edge
	 * @return the coordinates of the intersection point
	 * 
	 */
	static Coordinate intersection(Coordinate polygonA, Coordinate polygonB, Coordinate tileA,
			Coordinate tileB) {
		Coordinate intersectionPoint = new Coordinate();

		if (tileA.x == tileB.x) {
			// tile edge is horizontal
			// this means that the y coordinate of the intersection point is equal to the values
			// of the y coordinate of the tile edge vertices

			intersectionPoint.x = tileA.x;
			intersectionPoint.y = polygonA.y + (tileA.x - polygonA.x)
					* (polygonB.y - polygonA.y) / (polygonB.x - polygonA.x);
		} else {
			// tile edge is vertical
			// this means that the x coordinate of the intersection point is equal to the values
			// of the y coordinates of the tile edge vertices

			intersectionPoint.x = polygonA.x + (tileA.y - polygonA.y)
					* (polygonB.x - polygonA.x) / (polygonB.y - polygonA.y);
			intersectionPoint.y = tileA.y;
		}
		return intersectionPoint;
	}

}
