/*
 * Copyright 2010, 2011 mapsforge.org
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

import gnu.trove.list.array.TDoubleArrayList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.mapsforge.core.GeoCoordinate;
import org.mapsforge.core.MercatorProjection;
import org.mapsforge.preprocessing.map.osmosis.TileData.TDNode;
import org.mapsforge.preprocessing.map.osmosis.TileData.TDWay;

/**
 * Provides utility functions for the maps preprocessing.
 * 
 * @author bross
 */
final class GeoUtils {

	private static final int CLIPPED_COVERING_POLYGON_SIZE = 10;
	static final int MIN_NODES_POLYGON = 4;
	static final int MIN_COORDINATES_POLYGON = 8;
	private static final byte SUBTILE_ZOOMLEVEL_DIFFERENCE = 2;
	private static final double[] EPSILON_ZERO = new double[] { 0, 0 };
	private static final Logger LOGGER =
			Logger.getLogger(GeoUtils.class.getName());

	private static final int[] TILE_BITMASK_VALUES = new int[] { 32768, 16384,
			8192, 4096, 2048, 1024,
			512, 256, 128, 64, 32, 16, 8, 4, 2, 1 };

	private GeoUtils() {
		// prevent creation of a GeoUtils object
	}

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
	static Set<TileCoordinate> mapWayToTiles(final TDWay way, final byte baseZoomLevel,
			final int enlargementInMeter) {
		if (way == null)
			throw new IllegalArgumentException("parameter way is null");

		HashSet<TileCoordinate> matchedTiles = new HashSet<TileCoordinate>();
		double[] waynodes = way.wayNodesAsArray();
		if (waynodes == null) {
			return matchedTiles;
		}
		// check for valid closed polygon
		if (way.isPolygon() && waynodes.length < MIN_COORDINATES_POLYGON) {
			LOGGER.finer("found closed polygon with fewer than 4 nodes, ignoring this way, way-id: "
					+ way.getId());
			return matchedTiles;
		}

		TileCoordinate[] bbox = getWayBoundingBox(way, baseZoomLevel, enlargementInMeter);
		// calculate the tile coordinates and the corresponding bounding boxes
		for (int k = bbox[0].getX(); k <= bbox[1].getX(); k++) {
			for (int l = bbox[0].getY(); l <= bbox[1].getY(); l++) {
				double[] currentBBox = getBoundingBoxAsArray(k, l, baseZoomLevel,
						enlargementInMeter);
				if (!way.isPolygon()) {
					if (CohenSutherlandClipping.intersectsClippingRegion(waynodes, currentBBox)) {
						matchedTiles.add(new TileCoordinate(k, l, baseZoomLevel));
					}
				} else {
					if (CohenSutherlandClipping.intersectsClippingRegion(waynodes, currentBBox)
							|| SutherlandHodgmanClipping.accept(waynodes, currentBBox)) {
						matchedTiles.add(new TileCoordinate(k, l, baseZoomLevel));
					}
				}
			}
		}

		return matchedTiles;
	}

	/**
	 * A tile on zoom level <i>z</i> has exactly 16 sub tiles on zoom level <i>z+2</i>. For each
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
	static short computeBitmask(final TDWay way, final TileCoordinate tile,
			final int enlargementInMeters) {

		List<TileCoordinate> subtiles = tile.translateToZoomLevel((byte) (tile.getZoomlevel()
				+ SUBTILE_ZOOMLEVEL_DIFFERENCE));
		double[] waynodes = way.wayNodesAsArray();
		// check for valid closed polygon
		if (way.isPolygon() && waynodes.length < MIN_COORDINATES_POLYGON) {
			LOGGER.finer("found closed polygon with fewer than 4 nodes, ignoring this way, way-id: "
					+ way.getId());
			return 0;
		}

		short bitmask = 0;
		int tileCounter = 0;
		for (TileCoordinate subtile : subtiles) {
			double[] currentBBox = getBoundingBoxAsArray(subtile.getX(), subtile.getY(),
					subtile.getZoomlevel(), enlargementInMeters);
			if (!way.isPolygon()) {
				if (CohenSutherlandClipping.intersectsClippingRegion(waynodes, currentBBox)) {
					bitmask |= TILE_BITMASK_VALUES[tileCounter];
				}
			} else {
				if (CohenSutherlandClipping.intersectsClippingRegion(waynodes, currentBBox)
						|| SutherlandHodgmanClipping.accept(waynodes, currentBBox)) {
					bitmask |= TILE_BITMASK_VALUES[tileCounter];
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
	static List<GeoCoordinate> filterWaynodesOnSamePixel(
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
	static List<Integer> waynodeAbsoluteCoordinatesToOffsets(
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
	 * Checks whether the given way is a closed polygon.
	 * 
	 * @param way
	 *            the way which is to be analyzed
	 * @return true if the way is a closed polygon, false otherwise
	 */
	static boolean isClosedPolygon(final TDWay way) {
		if (way == null)
			throw new IllegalArgumentException("parameter way is null");
		if (way.getWayNodes() == null || way.getWayNodes().length < MIN_NODES_POLYGON)
			return false;
		return way.getWayNodes()[0].getId() == way.getWayNodes()[way.getWayNodes().length - 1]
				.getId();
	}

	/**
	 * Computes the area enclosed by a non self-intersecting closed polygon.
	 * 
	 * see <a href="http://mathworld.wolfram.com/PolygonArea.html"/>
	 * 
	 * @param coordinates
	 *            the coordinates as a collection of lat/lon pairs
	 * @return the area enclosed by the polygon
	 */
	static double computePolygonArea(final Collection<GeoCoordinate> coordinates) {
		if (coordinates.size() < MIN_NODES_POLYGON) {
			LOGGER.finer("closed polygon must consist of at least 4 coordinates");
			return -1;
		}
		Iterator<GeoCoordinate> it = coordinates.iterator();

		GeoCoordinate c1 = null;
		GeoCoordinate c2 = it.next();
		double area = 0;
		while (it.hasNext()) {
			c1 = c2;
			c2 = it.next();

			area += c1.getLongitude() * c2.getLatitude();
			area -= c2.getLongitude() * c1.getLatitude();
		}

		return area / 2;
	}

	static GeoCoordinate computePolygonCentroid(final Collection<GeoCoordinate> coordinates,
			TDWay way) {
		if (coordinates.size() < MIN_NODES_POLYGON) {
			LOGGER.finer("closed polygon must consist of at least 4 coordinates");
			return null;
		}

		double area = computePolygonArea(coordinates);
		area *= 6;

		double cLon = 0, cLat = 0, factor;
		Iterator<GeoCoordinate> it = coordinates.iterator();
		GeoCoordinate c1;
		GeoCoordinate c2 = it.next();

		while (it.hasNext()) {
			c1 = c2;
			c2 = it.next();

			factor = c1.getLongitude() * c2.getLatitude() - c2.getLongitude()
					* c1.getLatitude();
			cLon += (c1.getLongitude() + c2.getLongitude()) * factor;
			cLat += (c1.getLatitude() + c2.getLatitude()) * factor;
		}

		GeoCoordinate gc = null;
		try {
			gc = new GeoCoordinate(cLat / area, cLon / area);
		} catch (IllegalArgumentException e) {
			LOGGER.fine("unable to compute valid polygon centroid for way: " + way.getId());
		}

		return gc;

	}

	static boolean pointInTile(GeoCoordinate point, TileCoordinate tile) {
		if (point == null || tile == null)
			return false;

		int lon1 = GeoCoordinate.doubleToInt(MercatorProjection.tileXToLongitude(tile.getX(),
				tile.getZoomlevel()));
		int lon2 = GeoCoordinate.doubleToInt(MercatorProjection.tileXToLongitude(
				tile.getX() + 1, tile.getZoomlevel()));
		int lat1 = GeoCoordinate.doubleToInt(MercatorProjection.tileYToLatitude(tile.getY(),
				tile.getZoomlevel()));
		int lat2 = GeoCoordinate.doubleToInt(MercatorProjection.tileYToLatitude(
				tile.getY() + 1, tile.getZoomlevel()));
		return point.getLatitudeE6() <= lat1 && point.getLatitudeE6() >= lat2
				&& point.getLongitudeE6() >= lon1 && point.getLongitudeE6() <= lon2;
	}

	// TODO clarify the semantic of a line segment
	static List<GeoCoordinate> clipLineToTile(final List<GeoCoordinate> line,
			final TileCoordinate tile, int enlargementInMeters) {
		if (line == null) {
			throw new IllegalArgumentException("line is null");
		}

		if (line.size() < 2)
			throw new IllegalArgumentException(
					"a valid line must have at least 2 points");

		double[] bbox = getBoundingBoxAsArray(tile.getX(), tile.getY(), tile.getZoomlevel(),
				enlargementInMeters);
		double[] lineArray = geocoordinatesAsArray(line);
		double[] clippedLine = CohenSutherlandClipping.clipLine(lineArray, bbox);
		if (clippedLine == null || clippedLine.length == 0) {
			LOGGER.finer("clipped polygon is empty: " + line);
			return Collections.emptyList();
		}

		return arrayAsGeoCoordinates(clippedLine);
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
	static List<GeoCoordinate> clipPolygonToTile(final List<GeoCoordinate> polygon,
			final TileCoordinate tile, int enlargementInMeters) {
		if (polygon == null) {
			throw new IllegalArgumentException("polygon is null");
		}

		if (polygon.size() < MIN_NODES_POLYGON)
			throw new IllegalArgumentException(
					"a valid closed polygon must have at least 4 points");

		double[] bbox = getBoundingBoxAsArray(tile.getX(), tile.getY(), tile.getZoomlevel(),
				enlargementInMeters);
		double[] polygonArray = geocoordinatesAsArray(polygon);

		double[] clippedPolygon = SutherlandHodgmanClipping.clipPolygon(polygonArray, bbox);

		if (clippedPolygon == null || clippedPolygon.length == 0) {
			LOGGER.finer("clipped polygon is empty: " + polygon);
			return Collections.emptyList();
		}

		List<GeoCoordinate> ret = arrayAsGeoCoordinates(clippedPolygon);

		// for us a valid closed polygon must have the same start and end point
		if (!ret.get(0).equals(ret.get(ret.size() - 1)))
			ret.add(ret.get(0));

		return ret;
	}

	static boolean covers(float[] polygon,
			final TileCoordinate tile, int enlargementInMeters) {
		if (polygon == null) {
			throw new IllegalArgumentException("polygon is null");
		}

		if (polygon.length < MIN_COORDINATES_POLYGON)
			throw new IllegalArgumentException(
					"a valid closed polygon must have at least 4 points");

		double[] bbox = getBoundingBoxAsArray(tile.getX(), tile.getY(), tile.getZoomlevel(),
				enlargementInMeters);
		double[] polygonAsDoubleArray = new double[polygon.length];
		for (int i = 0; i < polygon.length; i++) {
			polygonAsDoubleArray[i] = polygon[i];
		}

		double[] clippedPolygon = SutherlandHodgmanClipping.clipPolygon(polygonAsDoubleArray,
				bbox);

		if (clippedPolygon == null || clippedPolygon.length == 0
				|| clippedPolygon.length != CLIPPED_COVERING_POLYGON_SIZE) {
			return false;
		}

		// TODO implement
		return true;

	}

	private static double[] getBoundingBoxAsArray(long tileX, long tileY, byte zoom,
			int enlargementInMeter) {
		double minLat = MercatorProjection.tileYToLatitude(tileY + 1, zoom);
		double maxLat = MercatorProjection.tileYToLatitude(tileY, zoom);
		double minLon = MercatorProjection.tileXToLongitude(tileX, zoom);
		double maxLon = MercatorProjection.tileXToLongitude(tileX + 1, zoom);

		double[] epsilons = computeTileEnlargement(tileY, zoom, enlargementInMeter);

		return new double[] {
				minLon - epsilons[1],
				minLat - epsilons[0],
				maxLon + epsilons[1],
				maxLat + epsilons[0] };
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

		double[] epsilonsTopLeft = computeTileEnlargement(maxy, enlargementInPixel);
		double[] epsilonsBottomRight = computeTileEnlargement(miny, enlargementInPixel);

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

	private static double[] geocoordinatesAsArray(List<GeoCoordinate> coordinates) {
		double[] ret = new double[coordinates.size() * 2];
		int i = 0;
		for (GeoCoordinate c : coordinates) {
			ret[i++] = c.getLongitude();
			ret[i++] = c.getLatitude();
		}

		return ret;
	}

	private static List<GeoCoordinate> arrayAsGeoCoordinates(double[] coordinates) {
		List<GeoCoordinate> ret = new ArrayList<GeoCoordinate>();
		for (int i = 0; i < coordinates.length - 1; i += 2) {
			ret.add(new GeoCoordinate(coordinates[i + 1], coordinates[i]));
		}

		return ret;
	}

	static String toGPX(final List<GeoCoordinate> g) {
		StringBuilder sb = new StringBuilder();
		sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?>").append("\n");
		sb.append("<gpx xmlns=\"http://www.topografix.com/GPX/1/1\" creator=\"byHand\" "
				+ "version=\"1.1\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
				+ "xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 "
				+ "http://www.topografix.com/GPX/1/1/gpx.xsd\">").append("\n");
		for (GeoCoordinate c : g) {
			sb.append("\t<wpt ").append("lat=\"").append(c.getLatitude()).append("\" ");
			sb.append("lon=\"").append(c.getLongitude()).append("\"/>");
			sb.append("\n");
		}
		sb.append("</gpx>");

		return sb.toString();
	}

	static String arraysSVG(List<float[]> closedPolygons) {
		StringBuilder sb = new StringBuilder();
		sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>").append("\n");
		sb.append("<svg xmlns=\"http://www.w3.org/2000/svg\" "
				+ "xmlns:xlink=\"http://www.w3.org/1999/xlink\" "
				+ "xmlns:ev=\"http://www.w3.org/2001/xml-events\" "
				+ "version=\"1.1\" baseProfile=\"full\" width=\"800mm\" height=\"600mm\">");

		for (float[] fs : closedPolygons) {
			sb.append("<polygon points=\"");
			for (float f : fs) {
				sb.append(f).append(" ");
			}
			sb.append("\" />");
		}

		sb.append("</svg>");

		return sb.toString();
	}

	/**
	 * Implements the Sutherland-Hodgman algorithm for clipping a polygon.
	 * 
	 * @author bross
	 * 
	 */
	static class SutherlandHodgmanClipping {

		/**
		 * Check whether the polygon "touches" the clipping region (that includes "covers"), or
		 * whether the polygon is completely outside of the clipping region.
		 * 
		 * @param polygon
		 *            A closed polygon as a double array in the form
		 *            [x1,y1,x2,y2,x3,y3,...,x1,y1]
		 * @param rectangle
		 *            A rectangle defined by the bottom/left and top/right point, i.e. [xmin,
		 *            ymin, xmax, ymax]
		 * @return true if the polygon "touches" (includes "covers") the clipping region, false
		 *         otherwise
		 */
		static boolean accept(final double[] polygon, final double[] rectangle) {
			double[] clipped = clipPolygon(polygon, rectangle);
			return clipped != null && clipped.length > 0;
		}

		/**
		 * Clips a closed polygon to a rectangular clipping region.
		 * 
		 * @param polygon
		 *            A closed polygon as a double array in the form
		 *            [x1,y1,x2,y2,x3,y3,...,x1,y1]
		 * @param rectangle
		 *            A rectangle defined by the bottom/left and top/right point, i.e. [xmin,
		 *            ymin, xmax, ymax]
		 * @return the clipped polygon if the polygon "touches" the clipping region, an empty
		 *         array otherwise
		 */
		static double[] clipPolygon(final double[] polygon, final double[] rectangle) {
			if (polygon == null) {
				throw new IllegalArgumentException("polygon is null");
			}

			if (polygon.length < MIN_COORDINATES_POLYGON)
				throw new IllegalArgumentException(
						"a valid closed polygon must have at least 4 points");

			// bottom edge
			double[] clippedPolygon = clipPolygonToEdge(polygon, new double[] { rectangle[0],
					rectangle[1], rectangle[2], rectangle[1] });
			// right edge
			clippedPolygon = clipPolygonToEdge(clippedPolygon, new double[] { rectangle[2],
					rectangle[1], rectangle[2], rectangle[3] });
			// top edge
			clippedPolygon = clipPolygonToEdge(clippedPolygon, new double[] { rectangle[2],
					rectangle[3], rectangle[0], rectangle[3] });
			// left edge
			clippedPolygon = clipPolygonToEdge(clippedPolygon, new double[] { rectangle[0],
					rectangle[3], rectangle[0], rectangle[1] });

			return clippedPolygon;
		}

		private static boolean inside(double x, double y, double[] edge) {

			if (edge[0] < edge[2]) {
				// bottom edge
				return y >= edge[1];
			} else if (edge[0] > edge[2]) {
				// top edge
				return y <= edge[1];
			} else if (edge[1] < edge[3]) {
				// right edge
				return x <= edge[0];
			} else if (edge[1] > edge[3]) {
				// left edge
				return x >= edge[0];
			} else
				throw new IllegalArgumentException();
		}

		private static double[] clipPolygonToEdge(final double[] polygon, double[] edge) {
			TDoubleArrayList clippedPolygon = new TDoubleArrayList();

			if (polygon.length < MIN_COORDINATES_POLYGON)
				return polygon;

			// polygon not closed
			if (polygon[0] != polygon[polygon.length - 2]
					|| polygon[1] != polygon[polygon.length - 1]) {
				throw new IllegalArgumentException("polygon must be closed");
			}

			double x1, y1, x2, y2;
			boolean startPointInside = false, endPointInside = false;
			for (int i = 0; i < polygon.length - 2; i += 2) {
				// line starts with previous point
				x1 = polygon[i];
				y1 = polygon[i + 1];
				x2 = polygon[i + 2];
				y2 = polygon[i + 3];
				startPointInside = inside(x1, y1, edge);
				endPointInside = inside(x2, y2, edge);
				if (startPointInside) {
					if (endPointInside) {
						clippedPolygon.add(x2);
						clippedPolygon.add(y2);
					} else {
						double[] intersection = computeIntersection(edge, x1, y1, x2, y2);
						clippedPolygon.add(intersection);
					}
				} else if (endPointInside) {
					double[] intersection = computeIntersection(edge, x1, y1, x2, y2);
					clippedPolygon.add(intersection);
					clippedPolygon.add(x2);
					clippedPolygon.add(y2);
				}
			}

			// if clipped polygon is not closed, add the start point to the end
			if (clippedPolygon.size() > 0
					&& (clippedPolygon.get(0) != clippedPolygon.get(clippedPolygon.size() - 2)
					|| clippedPolygon.get(1) != clippedPolygon.get(clippedPolygon.size() - 1))) {
				clippedPolygon.add(clippedPolygon.get(0));
				clippedPolygon.add(clippedPolygon.get(1));
			}

			return clippedPolygon.toArray();
		}

		private static double[] computeIntersection(double[] edge, double x1, double y1,
				double x2, double y2) {
			double[] ret = new double[2];

			if (edge[1] == edge[3]) {
				// horizontal edge
				ret[1] = edge[1];
				ret[0] = x1 + (edge[1] - y1) * ((x2 - x1) / (y2 - y1));

			} else {
				// vertical edge
				ret[1] = y1 + (edge[0] - x1) * ((y2 - y1) / (x2 - x1));
				ret[0] = edge[0];
			}
			return ret;

		}
	}

	/**
	 * Clips a line to a clipping region using the Cohen-Sutherland algorithm.
	 * 
	 * @author bross
	 * 
	 */
	static class CohenSutherlandClipping {

		private static final int MIN_COORDINATES_LINE = 4;
		private static final byte INSIDE = 0;
		private static final byte LEFT = 1;
		private static final byte RIGHT = 2;
		private static final byte BOTTOM = 4;
		private static final byte TOP = 8;

		// TODO clarify the semantic of a line segment
		/**
		 * 
		 * @param line
		 *            A line as a double array in the form [x1,y1,x2,y2,x3,y3,...,xn,yn]
		 * @param rectangle
		 *            A rectangle defined by the bottom/left and top/right point, i.e. [xmin,
		 *            ymin, xmax, ymax]
		 * @return All line segments that can be clipped to the clipping region.
		 */
		static double[] clipLine(final double[] line, final double[] rectangle) {
			if (line.length < MIN_COORDINATES_LINE)
				throw new IllegalArgumentException("line must have at least 2 points");
			if (rectangle.length != 4)
				throw new IllegalArgumentException(
						"clipping rectangle must be defined by exactly 2 points");

			TDoubleArrayList clippedSegments = new TDoubleArrayList();
			double[] clippedSegment;
			for (int i = 0; i < line.length - 2; i += 2) {
				clippedSegment = clipLine(line[i], line[i + 1], line[i + 2], line[i + 3],
						rectangle);
				if (clippedSegment != null)
					clippedSegments.add(clippedSegment);
			}

			return clippedSegments.toArray();
		}

		/**
		 * Checks whether a given line intersects the given clipping region.
		 * 
		 * @param line
		 *            A line as a double array in the form [x1,y1,x2,y2,x3,y3,...,xn,yn]
		 * @param rectangle
		 *            A rectangle defined by the bottom/left and top/right point, i.e. [xmin,
		 *            ymin, xmax, ymax]
		 * @return true if any line segments intersects the clipping region, false otherwise
		 */
		static boolean intersectsClippingRegion(final double[] line, final double[] rectangle) {
			if (line.length < MIN_COORDINATES_LINE)
				throw new IllegalArgumentException("line must have at least 2 points");
			if (rectangle.length != 4)
				throw new IllegalArgumentException(
						"clipping rectangle must be defined by exactly 2 points");

			double[] clippedSegment;
			for (int i = 0; i < line.length - 2; i += 2) {
				clippedSegment = clipLine(line[i], line[i + 1], line[i + 2], line[i + 3],
						rectangle);
				if (clippedSegment != null)
					return true;
			}

			return false;
		}

		private static byte outCode(double x, double y, double xMin, double xMax, double yMin,
				double yMax) {
			byte outcode = INSIDE;
			if (x < xMin)
				outcode |= LEFT;
			else if (x > xMax)
				outcode |= RIGHT;
			if (y < yMin)
				outcode |= BOTTOM;
			else if (y > yMax)
				outcode |= TOP;

			return outcode;
		}

		private static double[] clipLine(double x1, double y1, double x2, double y2,
				double[] rectangle) {
			double x1Copy = x1, y1Copy = y1, x2Copy = x2, y2Copy = y2;

			byte outcode1 = outCode(x1Copy, y1Copy, rectangle[0], rectangle[2], rectangle[1],
					rectangle[3]);
			byte outcode2 = outCode(x2Copy, y2Copy, rectangle[0], rectangle[2], rectangle[1],
					rectangle[3]);

			while (true) {

				if ((outcode1 | outcode2) == 0) {
					// both are inside
					return new double[] { x1Copy, y1Copy, x2Copy, y2Copy };
				} else if ((outcode1 & outcode2) != 0) {
					// both are outside and in the same region
					return null;
				} else {
					// at least one is outside
					byte outcodeOut = outcode1 > 0 ? outcode1 : outcode2;
					double x = 0, y = 0;
					// Now find the intersection point;
					// use formulas y = y0 + slope * (x - x0), x = x0 + (1 / slope) * (y - y0)
					if ((outcodeOut & TOP) != 0) { // point is above the clip rectangle
						x = x1Copy + (x2Copy - x1Copy) * (rectangle[3] - y1Copy)
								/ (y2Copy - y1Copy);
						y = rectangle[3];
					} else if ((outcodeOut & BOTTOM) != 0) { // point is below the clip
																// rectangle
						x = x1Copy + (x2Copy - x1Copy) * (rectangle[1] - y1Copy)
								/ (y2Copy - y1Copy);
						y = rectangle[1];
					} else if ((outcodeOut & RIGHT) != 0) { // point is to the right of clip
															// rectangle
						y = y1Copy + (y2Copy - y1Copy) * (rectangle[2] - x1Copy)
								/ (x2Copy - x1Copy);
						x = rectangle[2];
					} else if ((outcodeOut & LEFT) != 0) { // point is to the left of clip
															// rectangle
						y = y1Copy + (y2Copy - y1Copy) * (rectangle[0] - x1Copy)
								/ (x2Copy - x1Copy);
						x = rectangle[0];
					}

					// Now we move outside point to intersection point to clip
					// and get ready for next pass.
					if (outcodeOut == outcode1) {
						x1Copy = x;
						y1Copy = y;
						outcode1 = outCode(x1Copy, y1Copy, rectangle[0], rectangle[2],
								rectangle[1],
								rectangle[3]);
					} else {
						x2Copy = x;
						y2Copy = y;
						outcode2 = outCode(x2Copy, y2Copy, rectangle[0], rectangle[2],
								rectangle[1],
								rectangle[3]);
					}
				}
			}
		}
	}
}
