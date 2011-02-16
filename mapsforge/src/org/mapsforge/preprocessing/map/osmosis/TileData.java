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

import gnu.trove.list.array.TShortArrayList;
import gnu.trove.set.TShortSet;
import gnu.trove.set.hash.TShortHashSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.mapsforge.core.GeoCoordinate;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;

class TileData {

	static final Logger LOGGER =
			Logger.getLogger(TileData.class.getName());

	private Set<TDNode> pois;
	private Set<TDWay> ways;

	TileData() {
		this.pois = new HashSet<TDNode>();
		this.ways = new HashSet<TDWay>();
	}

	void addPOI(TDNode poi) {
		pois.add(poi);
	}

	void addWay(TDWay way) {
		ways.add(way);
	}

	void removeWay(TDWay way) {
		ways.remove(way);
	}

	Map<Byte, List<TDNode>> poisByZoomlevel(byte minValidZoomlevel,
			byte maxValidZoomlevel) {
		HashMap<Byte, List<TDNode>> poisByZoomlevel = new HashMap<Byte, List<TDNode>>();
		for (TDNode poi : pois) {
			byte zoomlevel = poi.getMinimumZoomLevel();
			if (zoomlevel > maxValidZoomlevel)
				continue;
			if (zoomlevel < minValidZoomlevel)
				zoomlevel = minValidZoomlevel;
			List<TDNode> group = poisByZoomlevel.get(zoomlevel);
			if (group == null)
				group = new ArrayList<TDNode>();
			group.add(poi);
			poisByZoomlevel.put(zoomlevel, group);
		}

		return poisByZoomlevel;
	}

	Map<Byte, List<TDWay>> waysByZoomlevel(byte minValidZoomlevel, byte maxValidZoomlevel) {
		HashMap<Byte, List<TDWay>> waysByZoomlevel = new HashMap<Byte, List<TDWay>>();
		for (TDWay way : ways) {
			byte zoomlevel = way.getMinimumZoomLevel();
			if (zoomlevel > maxValidZoomlevel)
				continue;
			if (zoomlevel < minValidZoomlevel)
				zoomlevel = minValidZoomlevel;
			List<TDWay> group = waysByZoomlevel.get(zoomlevel);
			if (group == null)
				group = new ArrayList<TDWay>();
			group.add(way);
			waysByZoomlevel.put(zoomlevel, group);
		}

		return waysByZoomlevel;
	}

	static class TDNode {

		private static final int MAX_ELEVATION = 9000;
		private static final byte ZOOM_HOUSENUMBER = (byte) 17;
		private static final byte ZOOM_NAME = (byte) 16;

		private final long id;
		private final int latitude;
		private final int longitude;

		private final short elevation;
		private final String houseNumber;
		private final byte layer;
		private final String name;
		private short[] tags;

		static TDNode fromNode(Node node) {
			// special tags
			short elevation = 0;
			byte layer = 5;
			String name = null;
			String housenumber = null;

			OSMTag currentTag = null;
			TShortArrayList currentTags = new TShortArrayList();

			// Process Tags
			for (Tag tag : node.getTags()) {
				// test for special tags
				if (tag.getKey().equalsIgnoreCase("ele")) {
					try {
						elevation = (short) Double.parseDouble(tag.getValue());
						if (elevation > MAX_ELEVATION) {
							LOGGER.finer("invalid elevation " + elevation + " for node "
									+ node.getId());
							elevation = 0;
						}

					} catch (NumberFormatException e) {
						// nothing to do here as elevation is initialized with 0
					}
				} else if (tag.getKey().equalsIgnoreCase("addr:housenumber")) {
					housenumber = tag.getValue();
				} else if (tag.getKey().equalsIgnoreCase("name")) {
					name = tag.getValue();
				} else if (tag.getKey().equalsIgnoreCase("layer")) {
					try {
						layer = Byte.parseByte(tag.getValue());
						if (layer >= -5 && layer <= 5)
							layer += 5;
					} catch (NumberFormatException e) {
						// nothing to do here as layer is initialized with 5
					}
				} else if ((currentTag = MapFileWriterTask.TAG_MAPPING.getPoiTag(tag.getKey(),
						tag.getValue())) != null) {
					currentTags.add(currentTag.getId());
				}
			}
			return new TDNode(node.getId(),
					GeoCoordinate.doubleToInt(node.getLatitude()),
					GeoCoordinate.doubleToInt(node.getLongitude()), elevation,
					layer, housenumber, name, currentTags.toArray());
		}

		TDNode(long id, int latitude, int longitude, short elevation, byte layer,
				String houseNumber, String name) {
			this.id = id;
			this.latitude = latitude;
			this.longitude = longitude;
			this.elevation = elevation;
			this.houseNumber = houseNumber;
			this.layer = layer;
			this.name = name;
		}

		TDNode(long id, int latitude, int longitude, short elevation, byte layer,
				String houseNumber, String name, short[] tags) {
			this.id = id;
			this.latitude = latitude;
			this.longitude = longitude;
			this.elevation = elevation;
			this.houseNumber = houseNumber;
			this.layer = layer;
			this.name = name;
			this.tags = tags;
		}

		boolean isPOI() {
			return houseNumber != null || elevation != 0 || name != null || tags.length > 0;
		}

		byte getMinimumZoomLevel() {
			byte min = Byte.MAX_VALUE;
			if (houseNumber != null)
				min = (byte) Math.min(min, ZOOM_HOUSENUMBER);
			if (name != null)
				min = (byte) Math.min(min, ZOOM_NAME);
			if (tags == null)
				return min;
			OSMTag tag;
			for (short tagID : tags) {
				tag = MapFileWriterTask.TAG_MAPPING.getPoiTag(tagID);
				if (tag.isRenderable())
					min = (byte) Math.min(min, tag.getZoomAppear());
			}

			return min;
		}

		short[] getTags() {
			return tags;
		}

		void setTags(short[] tags) {
			this.tags = tags;
		}

		long getId() {
			return id;
		}

		int getLatitude() {
			return latitude;
		}

		int getLongitude() {
			return longitude;
		}

		short getElevation() {
			return elevation;
		}

		String getHouseNumber() {
			return houseNumber;
		}

		byte getLayer() {
			return layer;
		}

		String getName() {
			return name;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (int) (id ^ (id >>> 32));
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			TDNode other = (TDNode) obj;
			if (id != other.id)
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "TDNode [id=" + id + ", latitude=" + latitude + ", longitude=" + longitude
					+ ", name=" + name + ", tags=" + tags + "]";
		}

	}

	static class TDWay {
		private final long id;
		private final byte layer;
		private final String name;
		private final String ref;
		private short[] tags;
		private short waytype;
		private final TDNode[] wayNodes;

		static TDWay fromWay(Way way, EntityResolver<TDNode> er) {
			if (way == null)
				return null;

			// special tags
			byte layer = 5;
			String name = null;
			String ref = null;

			TShortArrayList currentTags = new TShortArrayList();
			OSMTag currentTag = null;

			// Process Tags
			for (Tag tag : way.getTags()) {
				// test for special tags
				if (tag.getKey().equalsIgnoreCase("name")) {
					name = tag.getValue();
				} else if (tag.getKey().equalsIgnoreCase("layer")) {
					try {
						layer = Byte.parseByte(tag.getValue());
						if (layer >= -5 && layer <= 5)
							layer += 5;
					} catch (NumberFormatException e) {
						// nothing to do here as layer is initialized with 5
					}
				} else if (tag.getKey().equalsIgnoreCase("ref")) {
					ref = tag.getValue();
				} else if ((currentTag = MapFileWriterTask.TAG_MAPPING.getWayTag(tag.getKey(),
						tag.getValue())) != null) {
					currentTags.add(currentTag.getId());
				}
			}

			// only ways with at least 2 way nodes are valid ways
			if (way.getWayNodes().size() >= 2) {

				boolean validWay = true;
				// retrieve way nodes from data store
				TDNode[] waynodes = new TDNode[way.getWayNodes().size()];
				int i = 0;
				for (WayNode waynode : way.getWayNodes()) {
					// TODO adjust interface to support a method getWayNodes()
					waynodes[i] = er.getEntity(waynode.getNodeId());
					if (waynodes[i] == null) {
						validWay = false;
						LOGGER.finer("unknown way node: " + waynode.getNodeId()
								+ " in way " + way.getId());
					}
					i++;
				}

				// for a valid way all way nodes must be existent in the input data
				if (validWay) {

					// mark the way as area if the first and the last way node are the same
					// and if the way has at least 4 way nodes
					short waytype = 1;
					if (waynodes[0].getId() == waynodes[waynodes.length - 1].getId()) {
						if (waynodes.length >= GeoUtils.MIN_NODES_POLYGON)
							waytype = 2;
						else {
							LOGGER.finer("Found closed polygon with fewer than 4 way nodes. Way-id: "
									+ way.getId());
							return null;
						}
					}

					return new TDWay(way.getId(), layer, name,
							ref, currentTags.toArray(), waytype, waynodes);
				}
			}

			return null;
		}

		TDWay(long id, byte layer, String name, String ref, TDNode[] wayNodes) {
			this.id = id;
			this.layer = layer;
			this.name = name;
			this.ref = ref;
			this.wayNodes = wayNodes;
		}

		TDWay(long id, byte layer, String name, String ref, short[] tags,
				short waytype, TDNode[] wayNodes) {
			this.id = id;
			this.layer = layer;
			this.name = name;
			this.ref = ref;
			this.tags = tags;
			this.waytype = waytype;
			this.wayNodes = wayNodes;
		}

		double[] wayNodesAsArray() {
			if (wayNodes == null)
				return null;
			double[] ret = new double[wayNodes.length * 2];
			int i = 0;
			for (TDNode waynode : wayNodes) {
				ret[i++] = GeoCoordinate.intToDouble(waynode.getLongitude());
				ret[i++] = GeoCoordinate.intToDouble(waynode.getLatitude());
			}

			return ret;
		}

		List<GeoCoordinate> wayNodesAsCoordinateList() {
			List<GeoCoordinate> waynodeCoordinates = new ArrayList<GeoCoordinate>();
			for (TDNode waynode : wayNodes) {
				waynodeCoordinates.add(new GeoCoordinate(waynode.getLatitude(),
						waynode.getLongitude()));
			}

			return waynodeCoordinates;
		}

		byte getMinimumZoomLevel() {
			byte min = Byte.MAX_VALUE;
			if (tags == null)
				return min;

			OSMTag tag = null;
			for (short tagID : tags) {
				tag = MapFileWriterTask.TAG_MAPPING.getWayTag(tagID);
				if (tag.isRenderable())
					min = (byte) Math.min(min, tag.getZoomAppear());

			}

			return min;
		}

		long getId() {
			return id;
		}

		byte getLayer() {
			return layer;
		}

		String getName() {
			return name;
		}

		String getRef() {
			return ref;
		}

		short[] getTags() {
			return tags;
		}

		short getWaytype() {
			return waytype;
		}

		void setWaytype(short waytype) {
			this.waytype = waytype;
		}

		void setTags(short[] tags) {
			this.tags = tags;
		}

		void addTags(short[] addendum) {
			if (tags == null)
				tags = addendum;
			else {
				TShortSet tags2 = new TShortHashSet();
				tags2.addAll(tags);
				tags2.addAll(addendum);
				tags = tags2.toArray();
			}

		}

		void removeTags(short[] substract) {
			TShortSet tags2 = new TShortHashSet();
			tags2.addAll(tags);
			tags2.removeAll(substract);
			tags = tags2.toArray();
		}

		boolean isCoastline() {
			if (tags == null)
				return false;
			OSMTag tag;
			for (short tagID : tags) {
				tag = MapFileWriterTask.TAG_MAPPING.getWayTag(tagID);
				if (tag.isCoastline())
					return true;
			}

			return false;
		}

		TDNode[] getWayNodes() {
			return wayNodes;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (int) (id ^ (id >>> 32));
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			TDWay other = (TDWay) obj;
			if (id != other.id)
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "TDWay [id=" + id + ", name=" + name + ", tags=" + tags + ", waytype="
					+ waytype + "]";
		}

	}
}
