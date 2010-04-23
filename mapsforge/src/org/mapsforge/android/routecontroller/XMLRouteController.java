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
package org.mapsforge.android.routecontroller;

import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.util.Log;

public class XMLRouteController implements IRouteController {

	/* The URL of the webservice */
	private final URL address;

	/* The handler for the answers */
	private IRouteHandler routeHandler;

	private static final SAXParserFactory factory = SAXParserFactory.newInstance();

	private BlockingQueue<ParseTask> queue = new ArrayBlockingQueue<ParseTask>(1);
	private boolean taskIsSet;
	private boolean isCanceled;

	/**
	 * 
	 * @param url
	 *            The URL for the webservice.
	 */
	public XMLRouteController(URL url) {
		this.address = url;
	}

	@Override
	public void geoCode(final QueryParameters query, final short wanted, final short max) {

		taskIsSet = true;

		StringBuilder url = new StringBuilder();
		url.append(address.toString()).append("getGeoLocation?");
		url.append("searchString=").append(query.getSearchString());
		url.append("&max=").append(max);
		url.append("&wanted=").append(wanted);

		System.out.println(url.toString());

		DefaultHandler handler = new DefaultHandler() {
			final List<Point> points = new ArrayList<Point>(max);

			String charsNew = "";
			String charsOld = "";
			int lon, lat;
			String name = "";

			@Override
			public void characters(char[] ch, int start, int length) {
				char[] text = new char[length];
				int j = 0;
				for (int i = start; i < (start + length); i++) {
					text[i - start] = ch[i];
					j++;
				}
				charsOld = charsNew;
				charsNew = new String(text);
			}

			@Override
			public void endElement(String namespaceURI, String localName, String qName) {
				String element = localName;
				if (element.equals("lon")) {
					lon = Integer.valueOf(charsNew);
				} else if (element.equals("lat")) {
					lat = Integer.valueOf(charsNew);
				} else if (element.equals("value") && charsOld.equals("name")) {
					name = charsNew;
				} else if (element.equals("return")) {
					if (points.size() <= max)
						points.add(new Point(lon, lat, name));
				}
			}

			@Override
			public void endDocument() throws SAXException {
				if (!isCanceled)
					routeHandler.onGeoCode(points);
			}

		};
		queue.add(new ParseTask(url.toString(), handler));
	}

	@Override
	public void getServerFeatures() {

		taskIsSet = true;

		String url = address.toString() + "getFeatures";

		DefaultHandler handler = new DefaultHandler() {
			String lastElement = "";
			Map<String, String> features = new TreeMap<String, String>();

			@Override
			public void startElement(String namespaceURI, String localName, String qName,
					Attributes atts) {
				lastElement = localName;
			}

			@Override
			public void characters(char[] ch, int start, int length) {
				char[] text = new char[length];
				int j = 0;
				for (int i = start; i < (start + length); i++) {
					text[i - start] = ch[i];
					j++;
				}
				features.put(lastElement, new String(text));
			}

			@Override
			public void endDocument() throws SAXException {
				if (!isCanceled)
					routeHandler.onServerFeatures(features);
			}
		};
		queue.add(new ParseTask(url.toString(), handler));
	}

	@Override
	public void setCallback(IRouteHandler rh) {
		routeHandler = rh;
	}

	@Override
	public void run() {
		while (!Thread.currentThread().isInterrupted()) {
			try {
				isCanceled = false;
				queue.take().parse();
				taskIsSet = false;
			} catch (InterruptedException e) {
				break;
			}
		}
	}

	@Override
	public void getPoints(Point point, short wanted, short max) {
		taskIsSet = true;

		StringBuilder url = new StringBuilder();
		url.append(address.toString()).append("getNextPoints?");
		url.append("wanted=").append(wanted);
		url.append("&max=").append(max);
		url.append("&points=");
		url.append(point.getLon()).append(",");
		url.append(point.getLat()).append(";");

		DefaultHandler handler = new DefaultHandler() {
			List<Point> points = new ArrayList<Point>();
			String charsNew = "";
			String charsOld = "";
			int lon, lat;
			String name = "";

			@Override
			public void characters(char[] ch, int start, int length) {
				char[] text = new char[length];
				int j = 0;
				for (int i = start; i < (start + length); i++) {
					text[i - start] = ch[i];
					j++;
				}
				charsOld = charsNew;
				charsNew = new String(text);
			}

			@Override
			public void endElement(String namespaceURI, String localName, String qName) {
				String element = localName;
				if (element.equals("lon")) {
					lon = Integer.parseInt(charsNew);
				} else if (element.equals("lat")) {
					lat = Integer.parseInt(charsNew);
				} else if (element.equals("value") && charsOld.equals("name")) {
					name = charsNew;
				} else if (element.equals("return")) {
					points.add(new Point(lon, lat, name));
				}
			}

			@Override
			public void endDocument() throws SAXException {
				if (!isCanceled)
					routeHandler.onPoints(points);
			}
		};
		queue.add(new ParseTask(url.toString(), handler));

	}

	@Override
	public void getRoute(List<Point> wayPoints) {

		taskIsSet = true;

		StringBuilder url = new StringBuilder();
		url.append(address.toString()).append("getRoute?points=");

		for (Point p : wayPoints) {
			url.append(p.getLat()).append(",");
			url.append(p.getLon()).append(";");
		}

		DefaultHandler handler = new DefaultHandler() {
			List<Point> points = new ArrayList<Point>();
			String charsNew = "";
			String charsOld = "";
			int lon, lat;
			String name = "";

			@Override
			public void characters(char[] ch, int start, int length) {
				char[] text = new char[length];
				int j = 0;
				for (int i = start; i < (start + length); i++) {
					text[i - start] = ch[i];
					j++;
				}
				charsOld = charsNew;
				charsNew = new String(text);
				Log.d("osm", charsNew);
			}

			@Override
			public void startElement(String uri, String localName, String qName,
					Attributes attributes) throws SAXException {
				Log.d("osm", "startLocalname:" + localName);
				Log.d("osm", "startQname:" + qName);
			}

			@Override
			public void endElement(String namespaceURI, String localName, String qName) {
				String element = localName;
				Log.d("osm", "localname:" + localName);
				Log.d("osm", "qname:" + qName);
				if (element.equals("lon")) {
					lon = Integer.parseInt(charsNew);
				} else if (element.equals("lat")) {
					lat = Integer.parseInt(charsNew);
				} else if (element.equals("value") && charsOld.equals("name")) {
					name = charsNew;
				} else if (element.equals("return")) {
					points.add(new Point(lon, lat, name));
				}
			}

			@Override
			public void endDocument() throws SAXException {
				if (!isCanceled)
					routeHandler.onRoute(points);
			}
		};
		queue.add(new ParseTask(url.toString(), handler));
	}

	@Override
	public boolean isReady() {
		return !taskIsSet;
	}

	@Override
	public void cancel() {
		isCanceled = true;
	}

	/**
	 *TODO
	 * 
	 * @author bogus
	 * 
	 */
	private class ParseTask {
		private String urlString;
		private DefaultHandler handler;

		ParseTask(String url, DefaultHandler handler) {
			this.urlString = url;
			this.handler = handler;
		}

		void parse() {
			try {

				URL url = new URL(urlString);
				SAXParser saxParser = factory.newSAXParser();

				InputStream in = url.openStream();
				Log.d("osm", "InputStream from " + url.toString() + " open");
				if (!isCanceled)
					saxParser.parse(in, handler);

			} catch (ConnectException e) {
				routeHandler.onError(ControllerError.NO_SERVER, "Server not found");
			} catch (SAXException e) {
				routeHandler.onError(ControllerError.BAD_RESPONSE, "Answer can not be parse");
			} catch (ParserConfigurationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				StackTraceElement[] stackTraces = e.getStackTrace();

				for (int i = 0; i < stackTraces.length; i++) {
					System.out.println(stackTraces[i].toString());
				}

				routeHandler.onError(ControllerError.NO_SERVER, "Maybe the url is wrong");
			} // TODO no network

		}

	}

}
