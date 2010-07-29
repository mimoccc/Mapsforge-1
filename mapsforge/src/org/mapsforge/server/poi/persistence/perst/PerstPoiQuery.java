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
package org.mapsforge.server.poi.persistence.perst;

import java.util.Collection;

import org.mapsforge.android.map.GeoPoint;
import org.mapsforge.server.poi.PointOfInterest;
import org.mapsforge.server.poi.persistence.IPoiQuery;

public class PerstPoiQuery implements IPoiQuery {

	private final PerstPersistenceManager persistenceManager;

	public PerstPoiQuery(String storageFileName) {
		this.persistenceManager = new PerstPersistenceManager(storageFileName);
	}

	@Override
	public Collection<PointOfInterest> findNearPosition(GeoPoint point, int distance,
			String categoryName, int limit) {
		return persistenceManager.findNearPosition(point, distance, categoryName, limit);
	}

	@Override
	public Collection<PointOfInterest> findInRect(GeoPoint p1, GeoPoint p2, String categoryName) {
		return persistenceManager.findInRect(p1, p2, categoryName);
	}

	@Override
	public void close() {
		persistenceManager.close();
	}

}
