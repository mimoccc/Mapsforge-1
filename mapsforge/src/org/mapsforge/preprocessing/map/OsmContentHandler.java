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

import java.util.Date;
import java.util.Hashtable;
import java.util.TreeMap;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;

class OsmContentHandler implements ContentHandler {
	private MapElementNode currentNode;
	private MapElementRelation currentRelation;
	private boolean currentRelationIsMultipolygon;
	private MapElementWay currentWay;
	private int latitudeMax = Integer.MIN_VALUE;
	private int latitudeMin = Integer.MAX_VALUE;
	private int longitudeMax = Integer.MIN_VALUE;
	private int longitudeMin = Integer.MAX_VALUE;
	private Hashtable<Long, MapElementNode> nodes;
	private TreeMap<String, Byte> nodeTagWhitelist;
	private long parseTimeStart;
	private long parseTimeStop;
	private TreeMap<Long, MapElementWay> ways;
	private TreeMap<String, Byte> wayTagWhitelist;

	OsmContentHandler(Hashtable<Long, MapElementNode> nodes, TreeMap<Long, MapElementWay> ways,
			TreeMap<String, Byte> nodeTagWhitelist, TreeMap<String, Byte> wayTagWhitelist) {
		this.nodes = nodes;
		this.ways = ways;
		this.nodeTagWhitelist = nodeTagWhitelist;
		this.wayTagWhitelist = wayTagWhitelist;
	}

	@Override
	public void characters(char[] ch, int start, int length) {
	}

	@Override
	public void endDocument() {
		this.parseTimeStop = new Date().getTime();
		System.out.println("parsing finished (" + (this.parseTimeStop - this.parseTimeStart)
				+ " ms)");
	}

	@Override
	public void endElement(String uri, String localName, String qName) {
		if (localName.equals("node")) {
			endNodeElement();
		} else if (localName.equals("way")) {
			endWayElement();
		} else if (localName.equals("nd")) {
			endNdElement();
		} else if (localName.equals("osm")) {
			// ignore
		} else if (localName.equals("bound")) {
			// ignore
		} else if (localName.equals("member")) {
			// ignore
		} else if (localName.equals("relation")) {
			endRelationElement();
		} else if (localName.equals("tag")) {
			// ignore
		} else {
			// ignore
		}
	}

	@Override
	public void endPrefixMapping(String prefix) {
	}

	@Override
	public void ignorableWhitespace(char[] ch, int start, int length) {
	}

	@Override
	public void processingInstruction(String target, String data) {
	}

	@Override
	public void setDocumentLocator(Locator locator) {
	}

	@Override
	public void skippedEntity(String name) {
	}

	@Override
	public void startDocument() {
		this.parseTimeStart = new Date().getTime();
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) {
		if (localName.equals("node")) {
			startNodeElement(attributes);
		} else if (localName.equals("way")) {
			startWayElement(attributes);
		} else if (localName.equals("nd")) {
			startNdElement(attributes);
		} else if (localName.equals("osm")) {
			// ignore
		} else if (localName.equals("bound")) {
			// ignore
		} else if (localName.equals("member")) {
			startMemberElement(attributes);
		} else if (localName.equals("relation")) {
			startRelationElement();
		} else if (localName.equals("tag")) {
			startTagElement(attributes);
		} else {
			System.err.println("unhandled element: " + localName);
		}
	}

	@Override
	public void startPrefixMapping(String prefix, String uri) {
	}

	private void endNdElement() {
		// ignore
	}

	private void endNodeElement() {
		if (this.currentNode != null) {
			this.nodes.put(Long.valueOf(this.currentNode.id), this.currentNode);
			this.currentNode = null;
		}
	}

	private void endRelationElement() {
		if (this.currentRelation != null) {
			if (this.currentRelationIsMultipolygon) {
				for (Long outerMemberId : this.currentRelation.outerMemberIds) {
					if (this.ways.containsKey(outerMemberId)) {
						this.ways.get(outerMemberId).multipolygonInnerMemberIds = this.currentRelation.innerMemberIds;
					} else {
						System.out.println("could not find outer way");
					}
				}

				for (Long innerMemberId : this.currentRelation.innerMemberIds) {
					if (this.ways.containsKey(innerMemberId)) {
						this.ways.get(innerMemberId).multipolygonOuterMemberIds = this.currentRelation.outerMemberIds;
					} else {
						System.out.println("could not find inner way");
					}
				}
			}
			this.currentRelationIsMultipolygon = false;
			this.currentRelation = null;
		}
	}

	private void endWayElement() {
		if (this.currentWay != null) {
			this.ways.put(Long.valueOf(this.currentWay.id), this.currentWay);
		}
		this.currentWay = null;
	}

	private void startMemberElement(Attributes attributes) {
		try {
			String type = attributes.getValue("type");
			if (type.equals("way")) {
				long ref = Long.parseLong(attributes.getValue("ref"));
				String role = attributes.getValue("role");
				if (role.equals("outer")) {
					this.currentRelation.outerMemberIds.add(Long.valueOf(ref));
				} else if (role.equals("inner")) {
					this.currentRelation.innerMemberIds.add(Long.valueOf(ref));
				} else {
					// skip current member element
				}
			} else {
				// skip current member element
			}
		} catch (NumberFormatException e) {
			System.err.println(e.getMessage());
		}
	}

	private void startNdElement(Attributes attributes) {
		try {
			long ref = Long.parseLong(attributes.getValue("ref"));
			if (this.nodes.get(Long.valueOf(ref)) != null) {
				// ignore
			}
			if (this.currentWay != null) {
				this.currentWay.nodesSequence.add(Long.valueOf(ref));
			} else {
				System.err.println("unexpected nd element");
			}
		} catch (NumberFormatException e) {
			System.err.println(e.getMessage());
		}
	}

	private void startNodeElement(Attributes attributes) {
		try {
			long id = Long.parseLong(attributes.getValue("id"));
			int latitude = (int) (Float.parseFloat(attributes.getValue("lat")) * 1000000);
			int longitude = (int) (Float.parseFloat(attributes.getValue("lon")) * 1000000);
			this.currentNode = new MapElementNode(id, latitude, longitude);
			if (latitude < this.latitudeMin) {
				this.latitudeMin = latitude;
			} else if (latitude > this.latitudeMax) {
				this.latitudeMax = latitude;
			}
			if (longitude < this.longitudeMin) {
				this.longitudeMin = longitude;
			} else if (longitude > this.longitudeMax) {
				this.longitudeMax = longitude;
			}
		} catch (NumberFormatException e) {
			System.err.println(e.getMessage());
		}
	}

	private void startRelationElement() {
		try {
			this.currentRelation = new MapElementRelation();
			this.currentRelationIsMultipolygon = false;
		} catch (NumberFormatException e) {
			System.err.println(e.getMessage());
		}
	}

	private void startTagElement(Attributes attributes) {
		String key = attributes.getValue("k");
		String value = attributes.getValue("v");
		if (key == null || value == null) {
			System.err.println("invalid tag element: " + key + "=" + value);
			return;
		} else if (key.equals("name")) {
			if (this.currentNode != null) {
				this.currentNode.name = value;
			} else if (this.currentWay != null) {
				this.currentWay.name = value;
			}
		} else if (key.equals("layer")) {
			if (this.currentWay != null) {
				try {
					this.currentWay.layer = Byte.parseByte(value);
				} catch (NumberFormatException e) {
					this.currentWay.layer = 0;
				}
			}
		} else if (this.currentNode != null) {
			if (!this.nodeTagWhitelist.containsKey(key + "=" + value)) {
				// ignore
			} else {
				this.currentNode.tags.add(key + "=" + value);
			}
		} else if (this.currentWay != null) {
			if (!this.wayTagWhitelist.containsKey(key + "=" + value)) {
				// ignore
			} else {
				this.currentWay.tags.add(key + "=" + value);
			}
		} else if (this.currentRelation != null) {
			if (key.equals("type") && value.equals("multipolygon")) {
				this.currentRelationIsMultipolygon = true;
			} else {
				// ignore
			}
		} else {
			System.err.println("unexpected tag element");
		}
	}

	private void startWayElement(Attributes attributes) {
		try {
			long id = Long.parseLong(attributes.getValue("id"));
			this.currentWay = new MapElementWay(id);
		} catch (NumberFormatException e) {
			System.err.println(e.getMessage());
		}
	}

	GeoRectangle getBoundingBox() {
		return new GeoRectangle(this.latitudeMax, this.longitudeMin, this.latitudeMin,
				this.longitudeMax);
	}
}