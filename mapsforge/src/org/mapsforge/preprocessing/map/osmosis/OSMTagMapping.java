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

import gnu.trove.map.hash.TShortIntHashMap;
import gnu.trove.procedure.TShortIntProcedure;

import java.io.IOException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.TreeSet;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

final class OSMTagMapping {

	private static final Logger LOGGER =
			Logger.getLogger(OSMTagMapping.class.getName());

	// we use LinkedHashMaps as they guarantee to uphold the
	// insertion order when iterating over the key or value "set"
	private LinkedHashMap<String, OSMTag> stringToPoiTag = new LinkedHashMap<String, OSMTag>();
	private LinkedHashMap<String, OSMTag> stringToWayTag = new LinkedHashMap<String, OSMTag>();

	private LinkedHashMap<Short, OSMTag> idToPoiTag = new LinkedHashMap<Short, OSMTag>();
	private LinkedHashMap<Short, OSMTag> idToWayTag = new LinkedHashMap<Short, OSMTag>();

	private LinkedHashMap<Short, Short> optimizedPoiIds = new LinkedHashMap<Short, Short>();
	private LinkedHashMap<Short, Short> optimizedWayIds = new LinkedHashMap<Short, Short>();

	private short poiID = 0;
	private short wayID = 0;

	private byte defaultZoomAppear;

	private static final String XPATH_EXPRESSION_DEFAULT_ZOOM =
			"/tag-mapping/@default-zoom-appear";

	private static final String XPATH_EXPRESSION_POIS =
			"//pois/osm-tag["
					+ "(../@enabled='true' or not(../@enabled)) and (./@enabled='true' or not(./@enabled)) "
					+ "or (../@enabled='false' and ./@enabled='true')]";

	private static final String XPATH_EXPRESSION_WAYS =
			"//ways/osm-tag["
					+ "(../@enabled='true' or not(../@enabled)) and (./@enabled='true' or not(./@enabled)) "
					+ "or (../@enabled='false' and ./@enabled='true')]";

	OSMTagMapping(URL tagConf) throws IllegalStateException {
		try {
			// ---- Parse XML file ----
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document document = builder.parse(tagConf.openStream());

			XPath xpath = XPathFactory.newInstance().newXPath();

			XPathExpression xe = xpath.compile(XPATH_EXPRESSION_DEFAULT_ZOOM);
			defaultZoomAppear = Byte.parseByte((String) xe.evaluate(document,
					XPathConstants.STRING));

			// ---- Get list of poi nodes ----
			xe = xpath.compile(XPATH_EXPRESSION_POIS);
			NodeList pois = (NodeList) xe.evaluate(document, XPathConstants.NODESET);

			for (int i = 0; i < pois.getLength(); i++) {
				NamedNodeMap attributes = pois.item(i).getAttributes();
				String key = attributes.getNamedItem("key").getTextContent();
				String value = attributes.getNamedItem("value").getTextContent();

				String[] equivalentValues = null;
				if (attributes.getNamedItem("equivalent-values") != null) {
					equivalentValues = attributes.getNamedItem("equivalent-values")
							.getTextContent().split(",");
				}

				Byte zoom = attributes.getNamedItem("zoom-appear") == null ? defaultZoomAppear
						: Byte.parseByte(attributes.getNamedItem("zoom-appear")
								.getTextContent());

				boolean renderable = attributes.getNamedItem("renderable") == null ? true
						: Boolean.parseBoolean(attributes.getNamedItem("renderable")
								.getTextContent());

				OSMTag osmTag = new OSMTag(poiID, key, value, zoom, renderable);
				if (stringToPoiTag.containsKey(osmTag.tagKey())) {
					LOGGER
							.warning("duplicate osm-tag found in tag-mapping configuration (ignoring): "
									+ osmTag);
					continue;
				}
				LOGGER.finest("adding poi: " + osmTag);
				stringToPoiTag.put(osmTag.tagKey(), osmTag);
				if (equivalentValues != null) {
					for (String equivalentValue : equivalentValues) {
						stringToPoiTag.put(OSMTag.tagKey(key, equivalentValue), osmTag);
					}
				}
				idToPoiTag.put(poiID, osmTag);

				// also fill optimization mapping with identity
				optimizedPoiIds.put(poiID, poiID);

				poiID++;
			}

			// ---- Get list of way nodes ----
			xe = xpath.compile(XPATH_EXPRESSION_WAYS);
			NodeList ways = (NodeList) xe.evaluate(document, XPathConstants.NODESET);

			for (int i = 0; i < ways.getLength(); i++) {
				NamedNodeMap attributes = ways.item(i).getAttributes();
				String key = attributes.getNamedItem("key").getTextContent();
				String value = attributes.getNamedItem("value").getTextContent();

				String[] equivalentValues = null;
				if (attributes.getNamedItem("equivalent-values") != null) {
					equivalentValues = attributes.getNamedItem("equivalent-values")
							.getTextContent().split(",");
				}

				Byte zoom = attributes.getNamedItem("zoom-appear") == null ? defaultZoomAppear
						: Byte.parseByte(attributes.getNamedItem("zoom-appear")
								.getTextContent());

				boolean renderable = attributes.getNamedItem("renderable") == null ? true
						: Boolean.parseBoolean(attributes.getNamedItem("renderable")
								.getTextContent());

				OSMTag osmTag = new OSMTag(wayID, key, value, zoom, renderable);
				if (stringToWayTag.containsKey(osmTag.tagKey())) {
					LOGGER
							.warning("duplicate osm-tag found in tag-mapping configuration (ignoring): "
									+ osmTag);
					continue;
				}
				LOGGER.finest("adding way: " + osmTag);
				stringToWayTag.put(osmTag.tagKey(), osmTag);
				if (equivalentValues != null) {
					for (String equivalentValue : equivalentValues) {
						stringToWayTag.put(OSMTag.tagKey(key, equivalentValue), osmTag);
					}
				}
				idToWayTag.put(wayID, osmTag);

				// also fill optimization mapping with identity
				optimizedWayIds.put(wayID, wayID);

				wayID++;
			}

			// ---- Error handling ----
		} catch (SAXParseException spe) {
			System.out.println("\n** Parsing error, line " + spe.getLineNumber()
													+ ", uri " + spe.getSystemId());
			System.out.println("   " + spe.getMessage());
			throw new IllegalStateException(spe);
		} catch (SAXException sxe) {
			throw new IllegalStateException(sxe);
		} catch (ParserConfigurationException pce) {
			throw new IllegalStateException(pce);
		} catch (IOException ioe) {
			throw new IllegalStateException(ioe);
		} catch (XPathExpressionException e) {
			throw new IllegalStateException(e);
		}
	}

	// Map<String, OSMTag> poiMapping() {
	// return stringToPoiTag;
	// }
	//
	// Map<String, OSMTag> wayMapping() {
	// return stringToWayTag;
	// }

	OSMTag getWayTag(String key, String value) {
		return stringToWayTag.get(OSMTag.tagKey(key, value));
	}

	OSMTag getPoiTag(String key, String value) {
		return stringToPoiTag.get(OSMTag.tagKey(key, value));
	}

	OSMTag getWayTag(short id) {
		return idToWayTag.get(id);
	}

	OSMTag getPoiTag(short id) {
		return idToPoiTag.get(id);
	}

	short[] tagIDsFromList(List<OSMTag> tags) {
		short[] tagIDs = new short[tags.size()];
		int i = 0;
		for (OSMTag tag : tags) {
			tagIDs[i++] = tag.getId();
		}

		return tagIDs;
	}

	LinkedHashMap<Short, Short> optimizedPoiIds() {
		return optimizedPoiIds;
	}

	LinkedHashMap<Short, Short> optimizedWayIds() {
		return optimizedWayIds;
	}

	void optimizePoiOrdering(TShortIntHashMap histogram) {
		optimizedPoiIds.clear();
		final TreeSet<HistogramEntry> poiOrdering = new TreeSet<OSMTagMapping.HistogramEntry>();

		histogram.forEachEntry(new TShortIntProcedure() {
			@Override
			public boolean execute(short tag, int amount) {
				poiOrdering.add(new HistogramEntry(tag, amount));
				return true;
			}
		});

		short tmpPoiID = 0;

		OSMTag currentTag = null;
		for (HistogramEntry histogramEntry : poiOrdering.descendingSet()) {
			currentTag = idToPoiTag.get(histogramEntry.id);
			optimizedPoiIds.put(histogramEntry.id, tmpPoiID);
			LOGGER.finer("adding poi tag: " + currentTag.tagKey() + " id:" + tmpPoiID
					+ " amount: "
					+ histogramEntry.amount);
			tmpPoiID++;
		}
	}

	void optimizeWayOrdering(TShortIntHashMap histogram) {
		optimizedWayIds.clear();
		final TreeSet<HistogramEntry> wayOrdering = new TreeSet<OSMTagMapping.HistogramEntry>();

		histogram.forEachEntry(new TShortIntProcedure() {
			@Override
			public boolean execute(short tag, int amount) {
				wayOrdering.add(new HistogramEntry(tag, amount));
				return true;
			}
		});
		short tmpWayID = 0;

		OSMTag currentTag = null;
		for (HistogramEntry histogramEntry : wayOrdering.descendingSet()) {
			currentTag = idToWayTag.get(histogramEntry.id);
			optimizedWayIds.put(histogramEntry.id, tmpWayID);
			LOGGER.finer("adding way tag: " + currentTag.tagKey() + " id:" + tmpWayID
					+ " amount: "
					+ histogramEntry.amount);
			tmpWayID++;
		}
	}

	private class HistogramEntry implements Comparable<HistogramEntry> {

		final short id;
		final int amount;

		public HistogramEntry(short id, int amount) {
			super();
			this.id = id;
			this.amount = amount;
		}

		/**
		 * First order: amount Second order: id (reversed order)
		 */
		@Override
		public int compareTo(HistogramEntry o) {
			if (amount > o.amount) {
				return 1;
			} else if (amount < o.amount) {
				return -1;
			} else {
				if (id < o.id) {
					return 1;
				} else if (id > o.id) {
					return -1;
				} else {
					return 0;
				}
			}
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + amount;
			result = prime * result + id;
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
			HistogramEntry other = (HistogramEntry) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (amount != other.amount)
				return false;
			if (id != other.id)
				return false;
			return true;
		}

		private OSMTagMapping getOuterType() {
			return OSMTagMapping.this;
		}
	}
}
