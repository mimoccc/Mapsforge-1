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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.mapsforge.core.GeoCoordinate;
import org.mapsforge.core.MercatorProjection;
import org.mapsforge.core.Rect;
import org.mapsforge.preprocessing.map.osmosis.TileData.TDNode;
import org.mapsforge.preprocessing.map.osmosis.TileData.TDWay;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;

/**
 * Provides utility functions for the maps preprocessing.
 * 
 * @author bross
 * 
 */
public class GeoUtils {

	private static final int SUBTILE_ZOOMLEVEL_DIFFERENCE = 2;
	private static final double[] EPSILON_ZERO = new double[] { 0, 0 };
	private static final Logger logger =
			Logger.getLogger(GeoUtils.class.getName());
	private static final GeometryFactory geoFac = new GeometryFactory();

	private static final int[] tileBitMaskValues = new int[] { 32768, 16384,
			8192, 4096, 2048, 1024,
			512, 256, 128, 64, 32, 16, 8, 4, 2, 1 };

	// // TODO do we need these epsilons, can we do it better with JTS?
	// // list of values for extending a tile on the base zoom level
	// private static final double[] tileEpsilon = new double[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
	// 0,
	// 0, 0.0006, 0.00025, 0.00013, 0.00006 };
	//
	// // list of values for extending a sub tile
	// private static final double[] subTileEpsilon = new double[] { 0, 0, 0, 0, 0, 0, 0, 0, 0,
	// 0,
	// 0, 0, 0, 0.0015, 0.0001, 0.00085, 0.00065 };

	private static double[] computeTileEnlargement(long tileY, byte zoom,
			int enlargementInMeter) {
		if (enlargementInMeter == 0)
			return EPSILON_ZERO;

		double[] epsilons = new double[2];
		double lat = MercatorProjection.tileYToLatitude(tileY, zoom);
		epsilons[0] = GeoCoordinate.latitudeDistance(enlargementInMeter);
		epsilons[1] = GeoCoordinate.longitudeDistance(enlargementInMeter, lat);

		return epsilons;
	}

	private static double[] computeTileEnlargement(double lat, int enlargementInPixel) {

		if (enlargementInPixel == 0)
			return EPSILON_ZERO;

		double[] epsilons = new double[2];

		epsilons[0] = GeoCoordinate.latitudeDistance(enlargementInPixel);
		epsilons[1] = GeoCoordinate.longitudeDistance(enlargementInPixel, lat);

		return epsilons;
	}

	/**
	 * Computes which tiles on the given base zoom level need to include the given way (which
	 * may be a polygon).
	 * 
	 * @param way
	 *            the way that is mapped to tiles
	 * @param baseZoomLevel
	 *            the base zoom level which is used in the mapping
	 * @param enlargementInMeter
	 *            amount of pixels that is used to enlarge the bounding box of the way and the
	 *            tiles in the mapping process
	 * @return all tiles on the given base zoom level that need to include the given way, an
	 *         empty set if no tiles are matched
	 */
	final static Set<TileCoordinate> mapWayToTiles(final TDWay way, final byte baseZoomLevel,
			final int enlargementInMeter) {
		if (way == null)
			throw new IllegalArgumentException("parameter way is null");

		HashSet<TileCoordinate> matchedTiles = new HashSet<TileCoordinate>();
		Geometry geoWay = GeoUtils.toJTSGeometry(way);
		if (geoWay == null) {
			return matchedTiles;
		}

		TileCoordinate[] bbox = getWayBoundingBox(way, baseZoomLevel, enlargementInMeter);

		// calculate the tile coordinates and the corresponding bounding boxes
		for (int k = bbox[0].getX(); k <= bbox[1].getX(); k++) {
			for (int l = bbox[0].getY(); l <= bbox[1].getY(); l++) {
				Geometry currentTile = geoFac.createPolygon(getBoundingBox(
						k, l, baseZoomLevel, enlargementInMeter), null);
				if (way.getWaytype() == 1) {
					if (geoWay.intersects(currentTile) || geoWay.crosses(currentTile)
							|| geoWay.coveredBy(currentTile)) {
						matchedTiles.add(new TileCoordinate(k, l, baseZoomLevel));
					}
				} else {
					if (geoWay.intersects(currentTile) || geoWay.crosses(currentTile)
							|| geoWay.covers(currentTile) || geoWay.coveredBy(currentTile)) {
						matchedTiles.add(new TileCoordinate(k, l, baseZoomLevel));
					}
				}
			}
		}

		return matchedTiles;
	}

	/**
	 * A tile on zoom level <i>z<i> has exactly 16 sub tiles on zoom level <i>z+2</i>. For each
	 * of these 16 sub tiles it is analyzed if the given way needs to be included. The result is
	 * represented as a 16 bit short value. Each bit represents one of the 16 sub tiles. A bit
	 * is set to 1 if the sub tile needs to include the way. Representation is row-wise.
	 * 
	 * @param way
	 *            the way which is analyzed
	 * @param tile
	 *            the tile which is split into 16 sub tiles
	 * @param enlargementInMeters
	 *            amount of pixels that is used to enlarge the bounding box of the way and the
	 *            tiles in the mapping process
	 * @return a 16 bit short value that represents the information which of the sub tiles needs
	 *         to include the way
	 */
	final static short computeBitmask(final TDWay way, final TileCoordinate tile,
			final int enlargementInMeters) {

		List<TileCoordinate> subtiles = computeSubtiles(tile, SUBTILE_ZOOMLEVEL_DIFFERENCE);
		Geometry geoWay = toJTSGeometry(way);

		short bitmask = 0;
		int tileCounter = 0;
		for (TileCoordinate subtile : subtiles) {
			Geometry subtilePolygon = geoFac.createPolygon(
					getBoundingBox(subtile.getX(), subtile.getY(), subtile.getZoomlevel(),
							enlargementInMeters),
					null);
			if (way.getWaytype() == 1) {
				if (geoWay.intersects(subtilePolygon) || geoWay.crosses(subtilePolygon)
						|| geoWay.coveredBy(subtilePolygon)) {
					bitmask |= tileBitMaskValues[tileCounter];
				}
			} else {
				if (geoWay.intersects(subtilePolygon) || geoWay.crosses(subtilePolygon)
						|| geoWay.covers(subtilePolygon) || geoWay.coveredBy(subtilePolygon)) {
					bitmask |= tileBitMaskValues[tileCounter];
				}
			}
			tileCounter++;
		}

		return bitmask;
	}

	/**
	 * On coarse zoom levels way nodes maybe mapped to the same or an adjacent pixel, so that
	 * they cannot be distinguished anymore by the human eye. We can therefore eliminate way
	 * nodes that are mapped on the same or adjacent (see parameter delta) pixels.
	 * 
	 * @param waynodes
	 *            the list of way nodes
	 * @param zoom
	 *            the zoom level which is used to do the computation
	 * @param delta
	 *            the minimum distance in pixels that separates two way nodes
	 * @return a new list of way nodes that includes only those way nodes that do not fall onto
	 *         the same or adjacent pixels
	 */
	final static List<GeoCoordinate> filterWaynodesOnSamePixel(
			final List<GeoCoordinate> waynodes,
			byte zoom, float delta) {
		if (waynodes == null)
			throw new IllegalArgumentException("parameter waynodes is null");

		List<GeoCoordinate> result = new ArrayList<GeoCoordinate>();
		double pixelXPrev;
		double pixelYPrev;

		pixelXPrev = MercatorProjection.longitudeToPixelX(waynodes.get(0).getLongitude(), zoom);
		pixelYPrev = MercatorProjection.latitudeToPixelY(waynodes.get(0).getLatitude(), zoom);

		// add the first way node to the result list
		result.add(waynodes.get(0));

		for (GeoCoordinate waynode : waynodes.subList(1, waynodes.size() - 1)) {
			double pixelX = MercatorProjection.longitudeToPixelX(waynode.getLongitude(), zoom);
			double pixelY = MercatorProjection.latitudeToPixelY(waynode.getLatitude(), zoom);

			// if one of the pixel coordinates is more than delta pixels away from the way
			// node which was most recently added to the result list, add the current way node
			// to the result list
			if (Math.abs(pixelX - pixelXPrev) >= delta
					|| Math.abs(pixelY - pixelYPrev) >= delta) {
				pixelXPrev = pixelX;
				pixelYPrev = pixelY;
				result.add(waynode);
			}
		}
		result.add(waynodes.get(waynodes.size() - 1));

		return result;
	}

	/**
	 * Transforms a list of absolute way node coordinates to a list of offsets between the
	 * coordinates. The first coordinate of the way is represented as an absolute coordinate,
	 * all following coordinates are represented as offsets to their previous coordinate in the
	 * way.
	 * 
	 * @param waynodes
	 *            the list of absolute way node coordinates
	 * @return A list of offsets, where the first coordinate is absolute and all following are
	 *         offsets to their predecessor. For each way node first latitude (offset) and
	 *         second longitude (offset) is added.
	 */
	final static List<Integer> waynodeAbsoluteCoordinatesToOffsets(
			final List<GeoCoordinate> waynodes) {
		ArrayList<Integer> result = new ArrayList<Integer>();

		if (waynodes.isEmpty())
			return result;

		// add the first way node to the result list
		result.add(waynodes.get(0).getLatitudeE6());
		result.add(waynodes.get(0).getLongitudeE6());

		GeoCoordinate prevWaynode = waynodes.get(0);
		for (GeoCoordinate waynode : waynodes.subList(1, waynodes.size())) {
			result.add(waynode.getLatitudeE6() - prevWaynode.getLatitudeE6());
			result.add(waynode.getLongitudeE6() - prevWaynode.getLongitudeE6());
			prevWaynode = waynode;
		}

		return result;
	}

	/**
	 * Computes the maximum absolute difference in the list of way node offsets. It is assumed
	 * that the first two entries in the list are absolute coordinates and not offsets, so only
	 * the remaining entries are considered.
	 * 
	 * @param wayNodeOffsets
	 *            the list containing the way node offsets, where the first two entries are
	 *            absolute coordinates and not offsets
	 * @return the maximum absolute offset
	 */
	final static int maxDiffBetweenCompressedWayNodes(final List<Integer> wayNodeOffsets) {
		int maxDiff = 0;
		for (int diff : wayNodeOffsets.subList(2, wayNodeOffsets.size())) {
			diff = Math.abs(diff);
			if (diff > maxDiff)
				maxDiff = diff;
		}
		return maxDiff;
	}

	/**
	 * Clips a polygon to the bounding box of a tile.
	 * 
	 * @param polygon
	 *            the polygon which is to be clipped
	 * @param tile
	 *            the tile which represents the clipping area
	 * @param enlargementInMeters
	 *            the enlargement of bounding boxes in meters
	 * @return the clipped polygon, null if the polygon is not valid or if the intersection
	 *         between polygon and the tile's bounding box is empty
	 */
	final static List<GeoCoordinate> clipPolygonToTile(final List<GeoCoordinate> polygon,
			final TileCoordinate tile, int enlargementInMeters) {
		if (polygon == null) {
			throw new IllegalArgumentException("polygon is null");
		}

		if (polygon.size() < 4)
			throw new IllegalArgumentException(
					"a valid closed polygon must have at least 4 points");

		Rect bbox = getRectBoundingBox(tile.getX(), tile.getY(), tile.getZoomlevel(),
				enlargementInMeters);

		// left edge
		List<GeoCoordinate> clippedPolygon = clipPolygonToEdge(polygon, new GeoCoordinate(
				bbox.maxLatitudeE6, bbox.minLongitudeE6), new GeoCoordinate(bbox.minLatitudeE6,
				bbox.minLongitudeE6));
		// bottom edge
		clippedPolygon = clipPolygonToEdge(clippedPolygon, new GeoCoordinate(
				bbox.minLatitudeE6,
						bbox.minLongitudeE6), new GeoCoordinate(bbox.minLatitudeE6,
				bbox.maxLongitudeE6));
		// right edge
		clippedPolygon = clipPolygonToEdge(clippedPolygon, new GeoCoordinate(
				bbox.minLatitudeE6, bbox.maxLongitudeE6), new GeoCoordinate(bbox.maxLatitudeE6,
				bbox.maxLongitudeE6));
		// top edge
		clippedPolygon = clipPolygonToEdge(clippedPolygon, new GeoCoordinate(
				bbox.maxLatitudeE6,
				bbox.maxLongitudeE6),
				new GeoCoordinate(bbox.maxLatitudeE6, bbox.minLongitudeE6));

		if (clippedPolygon.size() == 0) {
			logger.finer("clipped polygon is empty: " + polygon);
			return Collections.emptyList();
		}

		// for us a valid closed polygon must have the same start and end point
		if (!clippedPolygon.get(0).equals(clippedPolygon.get(clippedPolygon.size() - 1)))
			clippedPolygon.add(clippedPolygon.get(0));

		return clippedPolygon;
	}

	private static List<GeoCoordinate> clipPolygonToEdge(final List<GeoCoordinate> polygon,
			GeoCoordinate edgeStart, GeoCoordinate edgeEnd) {
		List<GeoCoordinate> clippedPolygon = new ArrayList<GeoCoordinate>();

		if (polygon.size() < 3)
			return polygon;

		GeoCoordinate previousVertex = polygon.get(polygon.size() - 1);
		boolean previousInside = false, currentInside = false;
		for (GeoCoordinate currentVertex : polygon) {
			if (edgeStart.getLatitudeE6() > edgeEnd.getLatitudeE6()) {
				previousInside = previousVertex.getLongitude() >= edgeStart.getLongitude();
				currentInside = currentVertex.getLongitude() >= edgeStart.getLongitude();
			} else if (edgeStart.getLongitudeE6() < edgeEnd.getLongitudeE6()) {
				previousInside = previousVertex.getLatitude() >= edgeStart.getLatitude();
				currentInside = currentVertex.getLatitude() >= edgeStart.getLatitude();
			} else if (edgeStart.getLatitudeE6() < edgeEnd.getLatitudeE6()) {
				previousInside = previousVertex.getLongitude() <= edgeStart.getLongitude();
				currentInside = currentVertex.getLongitude() <= edgeStart.getLongitude();
			} else if (edgeStart.getLongitudeE6() > edgeEnd.getLongitudeE6()) {
				previousInside = previousVertex.getLatitude() <= edgeStart.getLatitude();
				currentInside = currentVertex.getLatitude() <= edgeStart.getLatitude();
			} else
				throw new IllegalArgumentException("illegal edge: " + edgeStart + " : "
						+ edgeEnd);

			if (previousInside) {
				if (currentInside) {
					clippedPolygon.add(currentVertex);
				} else {
					GeoCoordinate intersection = computeIntersection(edgeStart, edgeEnd,
							previousVertex, currentVertex);
					clippedPolygon.add(intersection);
				}
			} else if (currentInside) {
				GeoCoordinate intersection = computeIntersection(edgeStart, edgeEnd,
						previousVertex, currentVertex);
				clippedPolygon.add(intersection);
				clippedPolygon.add(currentVertex);
			}

			previousVertex = currentVertex;
		}

		return clippedPolygon;
	}

	private static GeoCoordinate computeIntersection(GeoCoordinate edgeStart,
			GeoCoordinate edgeEnd, GeoCoordinate p1, GeoCoordinate p2) {
		// horizontal edge
		if (edgeStart.getLatitude() == edgeEnd.getLatitude()) {
			double latitude = edgeStart.getLatitude();
			double longitude = p1.getLongitude() + (edgeStart.getLatitude() - p1.getLatitude())
					* ((p2.getLongitude() - p1.getLongitude())
					/ (p2.getLatitude() - p1.getLatitude()));
			return new GeoCoordinate(latitude, longitude);
		}

		// vertical edge
		double latitude = p1.getLatitude()
					+ (edgeStart.getLongitude() - p1.getLongitude())
					* ((p2.getLatitude() - p1.getLatitude()) / (p2.getLongitude() - p1
							.getLongitude()));
		double longitude = edgeStart.getLongitude();
		return new GeoCoordinate(latitude, longitude);

	}

	// /**
	// * Clips a polygon to the bounding box of a tile.
	// *
	// * @param polygon
	// * the polygon which is to be clipped
	// * @param tile
	// * the tile which represents the clipping area
	// * @return the clipped polygon, null if the polygon is not valid or if the intersection
	// * between polygon and the tile's bounding box is empty
	// */
	// final static List<GeoCoordinate> clipPolygonToTile(final List<GeoCoordinate> polygon,
	// final TileCoordinate tile) {
	// if (polygon == null) {
	// throw new IllegalArgumentException("polygon is null");
	// }
	//
	// Coordinate[] jtsCoordinates = toJTSCoordinates(polygon);
	// if (jtsCoordinates.length < 3
	// || !jtsCoordinates[0].equals(jtsCoordinates[jtsCoordinates.length - 1])) {
	// throw new IllegalArgumentException("not a valid JTS polygon: "
	// + jtsCoordinates.toString());
	// }
	// Geometry jtsPolygon = geoFac.createPolygon(geoFac.createLinearRing(jtsCoordinates),
	// null);
	// if (!jtsPolygon.isValid())
	// return null;
	// Geometry tileBBox = geoFac.createPolygon(
	// getBoundingBox(tile.getX(), tile.getY(), tile.getZoomlevel(), 1), null);
	//
	// Polygon intersection = (Polygon) jtsPolygon.intersection(tileBBox);
	//
	// String jtsPolygonStr = toGPX(jtsPolygon);
	// String tileBBoxStr = toGPX(tileBBox);
	// String intersectionStr = toGPX(intersection);
	//
	// Geometry ch = intersection.convexHull();
	// String chStr = toGPX(ch);
	//
	// if (tile.getX() == 7174 && tile.getY() == 4312)
	// System.out.println("here we are");
	//
	// // if (intersection.isEmpty())
	// // intersection = tileBBox.intersection(jtsPolygon);
	// return toGeoCoordinates(ch);
	//
	// }

	/**
	 * Checks whether the given way is a closed polygon.
	 * 
	 * @param way
	 *            the way which is to be analyzed
	 * @return true if the way is a closed polygon, false otherwise
	 */
	final static boolean isClosedPolygon(final TDWay way) {
		if (way == null)
			throw new IllegalArgumentException("parameter way is null");
		if (way.getWayNodes() == null || way.getWayNodes().length < 3)
			return false;
		return way.getWayNodes()[0].getId() == way.getWayNodes()[way.getWayNodes().length - 1]
				.getId();
	}

	private static Geometry toJTSGeometry(final TDWay way) {
		assert way != null;
		int amount = way.getWayNodes().length;
		if (amount < 2) {
			logger.finer("way has fewer than 2 way nodes, id: " + way.getId());
			return null;
		}

		// LINE
		if (way.getWaytype() == 1) {
			return geoFac.createLineString(toJTSCoordinates(way.getWayNodes()));
		}

		// CLOSED POLYGON
		// data cleansing: sometimes way nodes are missing if an area has been
		// cut out of a larger data file
		// --> a polygon must have at least 4 points including the duplicated start/end
		if (amount < 4) {
			logger.finer("Found way of type polygon with fewer than 4 way nodes. Way-ID: "
						+ way.getId());
			return null;
		}
		return geoFac.createPolygon(
				geoFac.createLinearRing(toJTSCoordinates(way.getWayNodes())), null);
	}

	// TODO now implemented in TileCoordinate.translateToZoomLevel()
	private static List<TileCoordinate> computeSubtiles(final TileCoordinate tile,
			final int zoomLevelDistance) {
		if (zoomLevelDistance < 0)
			throw new IllegalArgumentException("zoomLevelDistance must be greater than 0, was "
					+ zoomLevelDistance);
		int factor = (int) Math.pow(2, zoomLevelDistance);
		List<TileCoordinate> subtiles = new ArrayList<TileCoordinate>((int) Math.pow(4,
				zoomLevelDistance));
		int tileUpperLeftX = tile.getX() * factor;
		int tileUpperLeftY = tile.getY() * factor;
		for (int i = 0; i < factor; i++) {
			for (int j = 0; j < factor; j++) {
				subtiles.add(new TileCoordinate(tileUpperLeftX + j, tileUpperLeftY + i,
						(byte) (tile.getZoomlevel() + zoomLevelDistance)));
			}
		}
		return subtiles;
	}

	private static LinearRing getBoundingBox(long tileX, long tileY, byte zoom,
			int enlargementInPixel) {
		double minLat = MercatorProjection.tileYToLatitude(tileY + 1, zoom);
		double maxLat = MercatorProjection.tileYToLatitude(tileY, zoom);
		double minLon = MercatorProjection.tileXToLongitude(tileX, zoom);
		double maxLon = MercatorProjection.tileXToLongitude(tileX + 1, zoom);

		double[] epsilons = computeTileEnlargement(tileY, zoom, enlargementInPixel);

		return geoFac.createLinearRing(new Coordinate[] {
				new Coordinate(maxLat + epsilons[0], minLon - epsilons[1]),
				new Coordinate(minLat - epsilons[0], minLon - epsilons[1]),
				new Coordinate(minLat - epsilons[0], maxLon + epsilons[1]),
				new Coordinate(maxLat + epsilons[0], maxLon + epsilons[1]),
				new Coordinate(maxLat + epsilons[0], minLon - epsilons[1]) });
	}

	private static Rect getRectBoundingBox(long tileX, long tileY, byte zoom,
			int enlargementInPixel) {
		double minLat = MercatorProjection.tileYToLatitude(tileY + 1, zoom);
		double maxLat = MercatorProjection.tileYToLatitude(tileY, zoom);
		double minLon = MercatorProjection.tileXToLongitude(tileX, zoom);
		double maxLon = MercatorProjection.tileXToLongitude(tileX + 1, zoom);

		double[] epsilons = computeTileEnlargement(tileY, zoom, enlargementInPixel);
		return new Rect(minLon - epsilons[1], maxLon + epsilons[1],
				minLat - epsilons[0], maxLat + epsilons[0]);
	}

	private static TileCoordinate[] getWayBoundingBox(final TDWay way, byte zoomlevel,
			int enlargementInPixel) {
		double maxx = Double.NEGATIVE_INFINITY, maxy = Double.NEGATIVE_INFINITY, minx = Double.POSITIVE_INFINITY, miny = Double.POSITIVE_INFINITY;
		for (TDNode coordinate : way.getWayNodes()) {
			maxy = Math.max(maxy, GeoCoordinate.intToDouble(coordinate.getLatitude()));
			miny = Math.min(miny, GeoCoordinate.intToDouble(coordinate.getLatitude()));
			maxx = Math.max(maxx, GeoCoordinate.intToDouble(coordinate.getLongitude()));
			minx = Math.min(minx, GeoCoordinate.intToDouble(coordinate.getLongitude()));
		}

		double epsilonsTopLeft[] = computeTileEnlargement(maxy, enlargementInPixel);
		double epsilonsBottomRight[] = computeTileEnlargement(miny, enlargementInPixel);

		TileCoordinate[] bbox = new TileCoordinate[2];
		bbox[0] = new TileCoordinate(
				(int) MercatorProjection.longitudeToTileX(minx - epsilonsTopLeft[1], zoomlevel),
				(int) MercatorProjection.latitudeToTileY(maxy + epsilonsTopLeft[0], zoomlevel),
				zoomlevel);
		bbox[1] = new TileCoordinate(
				(int) MercatorProjection.longitudeToTileX(maxx + epsilonsBottomRight[1],
						zoomlevel),
				(int) MercatorProjection.latitudeToTileY(miny - epsilonsBottomRight[0],
						zoomlevel), zoomlevel);

		return bbox;
	}

	private static Coordinate toJTSCoordinate(final TDNode coordinate) {
		return new Coordinate(GeoCoordinate.intToDouble(coordinate.getLatitude()),
				GeoCoordinate.intToDouble(coordinate.getLongitude()));
	}

	private static Coordinate[] toJTSCoordinates(final TDNode[] coordinates) {
		if (coordinates == null)
			return null;
		Coordinate[] jtsCoordinates = new Coordinate[coordinates.length];
		for (int i = 0, length = coordinates.length; i < length; i++) {
			jtsCoordinates[i] = toJTSCoordinate(coordinates[i]);
		}

		return jtsCoordinates;
	}

	// private static String toGPX(List<GeoCoordinate> g) {
	// StringBuilder sb = new StringBuilder();
	// sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?>").append("\n");
	// sb.append("<gpx xmlns=\"http://www.topografix.com/GPX/1/1\" creator=\"byHand\" " +
	// "version=\"1.1\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
	// "xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 " +
	// "http://www.topografix.com/GPX/1/1/gpx.xsd\">").append("\n");
	// for (GeoCoordinate c : g) {
	// sb.append("\t<wpt ").append("lat=\"").append(c.getLatitude()).append("\" ");
	// sb.append("lon=\"").append(c.getLongitude()).append("\"/>");
	// sb.append("\n");
	// }
	// sb.append("</gpx>");
	//
	// return sb.toString();
	// }
	//
	// private static String toGPX(Geometry g) {
	// StringBuilder sb = new StringBuilder();
	// sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?>").append("\n");
	// sb.append("<gpx xmlns=\"http://www.topografix.com/GPX/1/1\" creator=\"byHand\" " +
	// "version=\"1.1\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
	// "xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 " +
	// "http://www.topografix.com/GPX/1/1/gpx.xsd\">").append("\n");
	// for (Coordinate c : g.getCoordinates()) {
	// sb.append("\t<wpt ").append("lat=\"").append(c.x).append("\" ");
	// sb.append("lon=\"").append(c.y).append("\"/>");
	// sb.append("\n");
	// }
	// sb.append("</gpx>");
	//
	// return sb.toString();
	// }
}
