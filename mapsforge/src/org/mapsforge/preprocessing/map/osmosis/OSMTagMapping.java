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

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

	private final HashMap<String, OSMTag> stringToPoiTag = new HashMap<String, OSMTag>();
	private final HashMap<String, OSMTag> stringToWayTag = new HashMap<String, OSMTag>();

	private final HashMap<Short, OSMTag> idToPoiTag = new HashMap<Short, OSMTag>();
	private final HashMap<Short, OSMTag> idToWayTag = new HashMap<Short, OSMTag>();

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

	OSMTagMapping(InputStream is) throws IllegalStateException {
		try {
			// ---- Parse XML file ----
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			// factory.setNamespaceAware(true);
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document document = builder.parse(is);

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

				Byte zoom = attributes.getNamedItem("zoom-appear") == null ? defaultZoomAppear
						: Byte.parseByte(attributes.getNamedItem("zoom-appear")
								.getTextContent());

				boolean renderable = attributes.getNamedItem("renderable") == null ? true
						: Boolean.parseBoolean(attributes.getNamedItem("renderable")
								.getTextContent());

				OSMTag osmTag = new OSMTag(poiID, key, value, zoom, renderable);
				stringToPoiTag.put(osmTag.tagKey(), osmTag);
				idToPoiTag.put(poiID, osmTag);
				poiID++;
			}

			// ---- Get list of way nodes ----
			xe = xpath.compile(XPATH_EXPRESSION_WAYS);
			NodeList ways = (NodeList) xe.evaluate(document, XPathConstants.NODESET);

			for (int i = 0; i < ways.getLength(); i++) {
				NamedNodeMap attributes = ways.item(i).getAttributes();
				String key = attributes.getNamedItem("key").getTextContent();
				String value = attributes.getNamedItem("value").getTextContent();

				Byte zoom = attributes.getNamedItem("zoom-appear") == null ? defaultZoomAppear
						: Byte.parseByte(attributes.getNamedItem("zoom-appear")
								.getTextContent());

				boolean renderable = attributes.getNamedItem("renderable") == null ? true
						: Boolean.parseBoolean(attributes.getNamedItem("renderable")
								.getTextContent());

				OSMTag osmTag = new OSMTag(wayID, key, value, zoom, renderable);
				stringToWayTag.put(osmTag.tagKey(), osmTag);
				idToWayTag.put(wayID, osmTag);
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

	Map<String, OSMTag> poiMapping() {
		return stringToPoiTag;
	}

	Map<String, OSMTag> wayMapping() {
		return stringToWayTag;
	}

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
}
