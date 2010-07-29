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
package org.mapsforge.server.poi.persistence.postgis;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

import org.mapsforge.android.map.GeoPoint;
import org.mapsforge.server.poi.PoiCategory;
import org.mapsforge.server.poi.PointOfInterest;
import org.mapsforge.server.poi.exchange.IPoiReader;
import org.mapsforge.server.poi.persistence.IPersistenceManager;

public class PostGisPersistenceManager implements IPersistenceManager {

	protected static final String REPLACE_REGEX = "\\Q{{?}}\\E";
	protected static final String INSERT_POI = "INSERT INTO pois (\"location\", category, name, url) VALUES "
			+ "(ST_GeographyFromText('SRID=4326;POINT({{?}} {{?}})'), "
			+ "'{{?}}', {{?}}, {{?}});";
	protected static final String INSERT_CATEGORY = "INSERT INTO categories(title, parent) VALUES "
			+ "('{{?}}', {{?}});";

	private final PostGisConnection pgConnection;
	private PostGisPoiCategoryManager categoryManager;

	public PostGisPersistenceManager(Connection connection) {
		if (connection == null)
			throw new NullPointerException();
		this.pgConnection = new PostGisConnection(connection);
		this.categoryManager = new PostGisPoiCategoryManager(pgConnection);
	}

	@Override
	public Collection<PoiCategory> allCategories() {
		return categoryManager.allCategories();
	}

	@Override
	public void close() {
		pgConnection.close();
	}

	@Override
	public Collection<PoiCategory> descendants(String category) {
		return categoryManager.descendants(category);
	}

	@Override
	public void insertCategory(PoiCategory category) {
		if (!categoryManager.contains(category.title)) {
			String sql = INSERT_CATEGORY.replaceFirst(REPLACE_REGEX, category.title)
					.replaceFirst(
							REPLACE_REGEX,
							category.parent != null ? "'" + category.parent.title + "'"
									: "null");
			if (pgConnection.executeInsertStatement(sql)) {
				categoryManager = new PostGisPoiCategoryManager(pgConnection);
			}
		}
	}

	protected String sanitize(String argument) {
		return (argument == null ? "null" : "'" + argument.replaceAll("\\Q'\\E", "") + "'");
	}

	@Override
	public void insertPointOfInterest(PointOfInterest poi) {
		String sql = insertPoiString(poi);
		pgConnection.executeInsertStatement(sql);
	}

	@Override
	public void insertPointsOfInterest(Collection<PointOfInterest> pois) {
		for (PointOfInterest poi : pois) {
			pgConnection.addToBatch(insertPoiString(poi));
		}

		pgConnection.executeBatch();
	}

	private String insertPoiString(PointOfInterest poi) {
		return INSERT_POI.replaceFirst(REPLACE_REGEX, "" + poi.longitude).replaceFirst(
				REPLACE_REGEX, "" + poi.latitude).replaceFirst(REPLACE_REGEX,
				poi.category.title).replaceFirst(REPLACE_REGEX, sanitize(poi.name))
				.replaceFirst(REPLACE_REGEX, sanitize(poi.url));
	}

	@Override
	public void insertPointsOfInterest(IPoiReader poiReader) {
		Collection<PointOfInterest> pois = poiReader.read();

		for (PointOfInterest poi : pois) {
			insertPointOfInterest(poi);
		}
	}

	@Override
	public void removeCategory(PoiCategory category) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void removePointOfInterest(PointOfInterest poi) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Collection<PointOfInterest> findNearPosition(GeoPoint point, int distance,
			String categoryName, int limit) {
		ArrayList<PointOfInterest> pois = new ArrayList<PointOfInterest>();

		try {
			String queryString = new PoiQueryStringBuilder(point.getLatitude(), point
					.getLongitude(), categoryName, distance, limit).queryString();

			System.out.println(queryString);

			ResultSet result = pgConnection.executeQuery(queryString);

			if (result.first()) {
				while (!result.isLast()) {
					pois.add(new PointOfInterest(result.getLong("id"), result.getDouble("lat"),
							result.getDouble("lng"), result.getString("name"), result
									.getString("url"), categoryManager.get(result
									.getString("category"))));
					result.next();
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return pois;
	}

	@Override
	public Collection<PointOfInterest> findInRect(GeoPoint p1, GeoPoint p2, String categoryName) {
		// TODO implement findInRect
		throw new UnsupportedOperationException();
	}

}
