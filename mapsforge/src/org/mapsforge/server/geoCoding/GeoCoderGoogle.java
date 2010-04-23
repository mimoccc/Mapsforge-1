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
 * This implementation use Google's geocoding service for geocoding!
 * 
 * For documentation of the used service see
 * http://code.google.com/apis/maps/documentation/geocoding/
 * 
 * NOTE: the geocoding service may only be used in conjunction with displaying results on a
 * Google map; geocoding results without displaying them on a map is prohibited. For complete
 * details on allowed usage, consult the Maps API Terms of Service License Restrictions.
 */
public class GeoCoderGoogle implements IGeoCoder {

	@Override
	public List<Node> search(String placeName, int max) {
		String search = placeName;
		List<Node> orte = new ArrayList<Node>();

		final String region = "DE";

		try {
			search = "q=" + URLEncoder.encode(search.trim(), "UTF-8");

			search += "&sensor=false&output=xml&region=" + region;

			URL u = new URL("http://maps.google.com/maps/geo?" + search);

			XMLInputFactory factory = XMLInputFactory.newInstance();
			XMLEventReader parser = factory.createXMLEventReader(u.openStream());

			String temp = new String();
			int x = 0;
			int y = 0;
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
					String startElementName = event.asStartElement().getName().toString()
							.split("}")[1];
					if (startElementName.equalsIgnoreCase("placemark")) {
						attribute = new TreeMap<String, String>();
					}
					break;
				case XMLStreamConstants.CHARACTERS:
					Characters characters = event.asCharacters();
					if (!characters.isWhiteSpace())
						temp = characters.getData();
					break;
				case XMLStreamConstants.END_ELEMENT:
					String endElementName = event.asEndElement().getName().toString()
							.split("}")[1];
					if (endElementName.equalsIgnoreCase("address"))
						attribute.put("name", temp);
					else if (endElementName.equalsIgnoreCase("coordinates")) {
						String[] coord = temp.split(",");
						y = (int) (Double.valueOf(coord[0]) * 1000000);
						x = (int) (Double.valueOf(coord[1]) * 1000000);
					} else if (endElementName.equalsIgnoreCase("placemark")) {
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
