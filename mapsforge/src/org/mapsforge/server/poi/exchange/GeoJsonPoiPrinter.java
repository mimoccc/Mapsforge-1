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
package org.mapsforge.server.poi.exchange;

import java.util.Collection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mapsforge.server.poi.PointOfInterest;

public class GeoJsonPoiPrinter implements IPoiPrinter {

	private final Collection<PointOfInterest> pois;

	public GeoJsonPoiPrinter(Collection<PointOfInterest> pois) {
		this.pois = pois;
	}

	@Override
	public String print() {
		try {
			return buildJsonObject(pois).toString(4);
		} catch (JSONException e) {
			throw new RuntimeException(e.getMessage());
		}
	}

	private JSONObject buildJsonObject(Collection<PointOfInterest> pointsOfInterest) {
		JSONObject geoJsonObject = new JSONObject();
		JSONArray geometryCollection = new JSONArray();
		try {
			geoJsonObject.put("type", "FeatureCollection");
			for (PointOfInterest point : pointsOfInterest) {
				geometryCollection.put(pointJsonObject(point));
			}
			geoJsonObject.put("features", geometryCollection);
		} catch (JSONException e) {
			throw new RuntimeException(e.getMessage());
		}
		return geoJsonObject;
	}

	private JSONObject pointJsonObject(PointOfInterest poi) throws JSONException {
		JSONObject jsonPoint = new JSONObject();
		jsonPoint.put("type", "Point");
		JSONArray coordinates = new JSONArray();

		// order of longitude and latitude is important here!
		coordinates.put(poi.longitude);
		coordinates.put(poi.latitude);
		jsonPoint.put("coordinates", coordinates);

		JSONObject jsonProperties = new JSONObject();
		jsonProperties.put("name", poi.name);
		jsonProperties.put("url", poi.url);
		jsonProperties.put("id", poi.id);

		JSONObject jsonFeature = new JSONObject();
		jsonFeature.put("type", "Feature");
		jsonFeature.put("geometry", jsonPoint);
		jsonFeature.put("properties", jsonProperties);

		return jsonFeature;
	}

}
