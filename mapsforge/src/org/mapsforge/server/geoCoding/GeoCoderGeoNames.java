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
package org.mapsforge.server.geoCoding;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.XMLEvent;

/**
 * This implementation use the geonames service for geocoding!
 * 
 * For documentation of the used service see http://www.geonames.org/export/geonames-search.html
 */
public class GeoCoderGeoNames implements IGeoCoder {

	@Override
	public List<Node> search(String request, int max) {
		String search = request;
		List<Node> orte = new ArrayList<Node>();

		try {
			search = "q=" + URLEncoder.encode(search.trim(), "UTF-8");

			search += "&maxRows=" + max;

			URL u = new URL("http://ws.geonames.org/search?" + search);

			XMLInputFactory factory = XMLInputFactory.newInstance();
			XMLEventReader parser = factory.createXMLEventReader(u.openStream());

			String temp = new String();
			double x = 0;
			double y = 0;
			TreeMap<String, String> attribute = new TreeMap<String, String>();

			while (parser.hasNext() && max > 0) {
				XMLEvent event = parser.nextEvent();

				switch (event.getEventType()) {
				case XMLStreamConstants.START_DOCUMENT:

					break;
				case XMLStreamConstants.END_DOCUMENT:

					parser.close();
					break;
				case XMLStreamConstants.START_ELEMENT:
					String startElementName = event.asStartElement().getName().toString();
					if (startElementName.equalsIgnoreCase("geoname")) {
						attribute = new TreeMap<String, String>();
					}
					break;
				case XMLStreamConstants.CHARACTERS:
					Characters characters = event.asCharacters();
					if (!characters.isWhiteSpace())
						temp = characters.getData();
					break;
				case XMLStreamConstants.END_ELEMENT:
					String endElementName = event.asEndElement().getName().toString();
					if (endElementName.equalsIgnoreCase("lat"))
						y = Double.valueOf(temp);
					else if (endElementName.equalsIgnoreCase("lng")) {
						x = Double.valueOf(temp);
					} else if (endElementName.equalsIgnoreCase("geoname")) {
						orte.add(new Node(x, y, attribute));
						max--;
					} else
						attribute.put(endElementName, temp);
					break;
				case XMLStreamConstants.ATTRIBUTE:
					break;

				default:
					break;
				}

			}

		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (XMLStreamException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return orte;

	}
}
