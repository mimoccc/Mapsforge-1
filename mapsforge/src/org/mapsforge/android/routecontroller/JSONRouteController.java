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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * This class is a IRouteController implementation, it use only JSON to communicate with the
 * webservice.
 * 
 * This class will work only in the emulator (and the phone), see
 * http://daverog.wordpress.com/2009
 * /12/14/why-android-isnt-ready-for-tdd-and-how-i-tried-anyway/ . For source of the
 * JSON-Andoid-things see http://blog.marvinware.com/2008/11/secret-source.html.
 * 
 * @author bogumil
 */
public class JSONRouteController implements IRouteController {

	/* The URL of the webservice */
	private final URL address;

	/* The handler for the answers */
	private IRouteHandler routeHandler;

	private BlockingQueue<ParseTask> queue = new ArrayBlockingQueue<ParseTask>(1);
	private boolean taskIsSet;
	private boolean isCanceled;

	/**
	 * Constructs a JSONRoutecontroller with given url.
	 * 
	 * @param url
	 *            The URL for the webservice.
	 */
	public JSONRouteController(URL url) {
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

		queue.add(new ParseTask(url.toString(), Request.GEOCODE));
	}

	@Override
	public void getServerFeatures() {

		taskIsSet = true;

		String url = address.toString() + "getFeatures";

		queue.add(new ParseTask(url.toString(), Request.FEATURES));
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

		queue.add(new ParseTask(url.toString(), Request.POINTS));

	}

	@Override
	public void getRoute(List<Point> wayPoints) {

		taskIsSet = true;

		StringBuilder urlString = new StringBuilder();
		urlString.append(address.toString()).append("getRoute?points=");

		for (Point p : wayPoints) {
			urlString.append(p.getLat()).append(",");
			urlString.append(p.getLon()).append(";");
		}
		queue.add(new ParseTask(urlString.toString(), Request.ROUTE));

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
	 * This class represent a job for parsing. It is used for parsing the answers from the
	 * server.
	 * 
	 * @author bogumil
	 */
	private class ParseTask {
		private String urlString;
		private Request requestType;

		ParseTask(String url, Request type) {
			this.urlString = url;
			this.requestType = type;
		}

		/**
		 * extractOnePoint extract from a JSONObject obj the latitude, longitude and name
		 * information and construct a Point.
		 * 
		 * @param obj
		 *            the JSONObject representing the Point
		 * @return Point
		 * @throws JSONException
		 */
		private Point extractOnePoint(JSONObject obj) throws JSONException {

			String name = "";
			JSONArray wsAttributes = obj.optJSONArray("WSAttributes");
			// look for the name, only if there are attributes
			if (wsAttributes != null) {
				for (int j = wsAttributes.length() - 1; j >= 0; j--) {
					if (wsAttributes.getJSONObject(j).getString("key").equals("name"))
						name = wsAttributes.getJSONObject(j).getString("value");
				}
			}
			return new Point(obj.getInt("lon"), obj.getInt("lat"), name);
		}

		/**
		 * extractPoints extract from response all points.
		 * 
		 * @param answer
		 * @return
		 * @throws JSONException
		 */
		private List<Point> extractPoints(JSONObject answer) throws JSONException {
			ArrayList<Point> geoPoints = new ArrayList<Point>();
			JSONArray geoCodeResponse = answer.optJSONArray("return");
			// more than one point in answer
			if (geoCodeResponse != null) {
				for (int i = 0; i < geoCodeResponse.length(); i++) {
					JSONObject jsonPoint = geoCodeResponse.getJSONObject(i);
					geoPoints.add(extractOnePoint(jsonPoint));
				}
				// only one point in answer
			} else {
				JSONObject geoCodeResponseOnlyOne = answer.optJSONObject("return");
				geoPoints.add(extractOnePoint(geoCodeResponseOnlyOne));
			}
			return geoPoints;

		}

		void parse() {
			URL url;
			InputStream in = null;
			try {
				if (requestType == Request.FEATURES)
					url = new URL(urlString + "?response=application/json");
				else
					url = new URL(urlString + "&response=application/json");

				in = url.openStream();
				String page = new Scanner(in).useDelimiter("\\Z").next();
				JSONObject response = new JSONObject(page);

				switch (requestType) {
					case FEATURES:
						Map<String, String> features = new TreeMap<String, String>();
						JSONObject featureResponse = response.getJSONObject(
								"getFeaturesResponse").getJSONObject("return");

						features.put("algorithm", featureResponse.get("algorithm").toString());
						features.put("description", featureResponse.get("description")
								.toString());
						features.put("transportType", featureResponse.get("transportType")
								.toString());
						if (!isCanceled)
							routeHandler.onServerFeatures(features);
						break;
					case ROUTE:
						JSONObject routeResponse = response.getJSONObject("getRouteResponse");
						if (!isCanceled)
							routeHandler.onRoute(extractPoints(routeResponse));
						break;
					case GEOCODE:
						JSONObject geoCodeResponse = response
								.getJSONObject("getGeoLocationResponse");
						if (!isCanceled)
							routeHandler.onGeoCode(extractPoints(geoCodeResponse));
						break;
					case POINTS:
						JSONObject pointsResponse = response
								.getJSONObject("getNextPointsResponse");
						if (!isCanceled)
							routeHandler.onPoints(extractPoints(pointsResponse));
						break;
				}

			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				routeHandler.onError(ControllerError.NO_SERVER, "Maybe the url is wrong");
			} catch (JSONException e) {
				routeHandler.onError(ControllerError.BAD_RESPONSE, "Answer cannot be parsed");
			} finally {
				if (in != null)
					try {
						in.close();
					} catch (IOException e) {
					}
			}
		}
		// TODO no network
	}
}
