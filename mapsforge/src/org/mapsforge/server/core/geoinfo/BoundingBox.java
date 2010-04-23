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
package org.mapsforge.server.core.geoinfo;

/**
 * Immutable representation of an area bounded by latitudes and longitudes. This class
 * encapsulates the calculation of equator and / or 180-meridian crossing bounding boxes in a
 * transparent way through useful methods.
 * 
 * @author Till Stolzenhain
 * 
 */
public final class BoundingBox {

	public static final BoundingBox WHOLE_WORLD = BoundingBox.getInstance(Point.MIN_VALUE,
			Point.MAX_VALUE);

	public static BoundingBox getInstance(IPoint center, int radiusInDecimeter) {
		return new BoundingBox(Point
				.newInstance(center, -radiusInDecimeter, -radiusInDecimeter), Point
				.newInstance(center, radiusInDecimeter, radiusInDecimeter));
	}

	/**
	 * Creates a new instance representing a BoundingBox corresponding to the parameters
	 * provided.
	 * 
	 * @param left
	 *            upper left or lower left corner
	 * @param right
	 *            lower right or upper right corner
	 * @return a BoundingBox with
	 */
	public static BoundingBox getInstance(IPoint left, IPoint right) {

		/** get Point instances */
		Point lowerLeft = null;
		Point upperRight = null;

		if (left.getLat() > right.getLat()) {
			/**
			 * exchange latitudes to make the left point the one more to the south
			 */
			lowerLeft = Point.newInstance(right.getLat(), left.getLon());
			upperRight = Point.newInstance(left.getLat(), right.getLon());
		} else {
			/** do no latitude exchange */
			lowerLeft = Point.getInstance(left);
			upperRight = Point.getInstance(right);
		}
		return new BoundingBox(lowerLeft, upperRight);
	}

	public static BoundingBox valueOf(String text) {
		// TODO : parse value of toString()
		// '{' + this.lowerLeft.toString() + '|'
		// + this.upperRight.toString() + '}'
		return WHOLE_WORLD;
	}

	private final Point lowerLeft;
	private final Point upperRight;

	private BoundingBox(Point lowerLeft, Point upperRight) {
		this.lowerLeft = lowerLeft;
		this.upperRight = upperRight;
	}

	/**
	 * returns a INode representing the center of the BoundingBox.
	 * 
	 * @return the central point.
	 */
	public IPoint getCenter() {
		return Point.center(this.getMinGeo(), this.getMaxGeo());
	}

	/**
	 * Provides access to the maximal geographical coordinates of this window.
	 * 
	 * @return node representing the lower right corner of the BoundingBox
	 */
	public IPoint getMaxGeo() {
		return this.upperRight;
	}

	/**
	 * Provides access to the minimal geographical coordinates of this window.
	 * 
	 * @return node representing the upper left corner of the BoundingBox
	 */
	public IPoint getMinGeo() {
		return this.lowerLeft;
	}

	/**
	 * Determines if the node given succeeds the latitude / longitude test.
	 * 
	 * @param node
	 *            the questionable node
	 * @return true if the node lies within the boundaries given and false if not.
	 */
	public boolean surrounds(IPoint node) {
		return this.getMinGeo().getLat() <= node.getLat()
				&& this.getMinGeo().getLon() <= node.getLon()
				&& this.getMaxGeo().getLat() >= node.getLat()
				&& this.getMaxGeo().getLon() >= node.getLon();
	}

	public static BoundingBox getInstance(int lat1, int lon1, int lat2, int lon2) {
		return getInstance(Point.newInstance(lat1, lon1), Point.newInstance(lat2, lon2));
	}

	@Override
	public String toString() {
		return '{' + this.lowerLeft.toString() + '|' + this.upperRight.toString() + '}';
	}

	public boolean overlaps(BoundingBox bb) {
		return (this.lowerLeft.lat <= bb.lowerLeft.lat
				&& bb.lowerLeft.lat <= this.upperRight.lat || this.lowerLeft.lat <= bb.upperRight.lat
				&& bb.upperRight.lat <= this.upperRight.lat)
				&& (this.lowerLeft.lon <= bb.lowerLeft.lon
						&& bb.lowerLeft.lon <= this.upperRight.lon || this.lowerLeft.lon <= bb.upperRight.lon
						&& bb.upperRight.lon <= this.upperRight.lon)
				|| (bb.lowerLeft.lat <= this.lowerLeft.lat
						&& this.lowerLeft.lat <= bb.upperRight.lat || bb.lowerLeft.lat <= this.upperRight.lat
						&& this.upperRight.lat <= bb.upperRight.lat)
				&& (bb.lowerLeft.lon <= this.lowerLeft.lon
						&& this.lowerLeft.lon <= bb.upperRight.lon || bb.lowerLeft.lon <= this.upperRight.lon
						&& this.upperRight.lon <= bb.upperRight.lon);
	}

	public BoundingBox intersect(BoundingBox bb) {
		if (!overlaps(bb))
			return null;
		return getInstance(Math.max(this.lowerLeft.lat, bb.lowerLeft.lat), Math.max(
				this.lowerLeft.lon, bb.lowerLeft.lon), Math.min(this.upperRight.lat,
				bb.upperRight.lat), Math.min(this.upperRight.lon, bb.upperRight.lon));
	}
}
