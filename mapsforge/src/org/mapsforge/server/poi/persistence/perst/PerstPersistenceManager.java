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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.garret.perst.GenericIndex;
import org.garret.perst.IterableIterator;
import org.garret.perst.Key;
import org.garret.perst.RectangleR2;
import org.garret.perst.Storage;
import org.garret.perst.StorageFactory;
import org.mapsforge.android.map.GeoPoint;
import org.mapsforge.server.poi.PoiCategory;
import org.mapsforge.server.poi.PointOfInterest;
import org.mapsforge.server.poi.exchange.IPoiReader;
import org.mapsforge.server.poi.persistence.IPersistenceManager;

public class PerstPersistenceManager implements IPersistenceManager {

	private static final double SPACE_BETWEEN_LATS_IN_KM = 111.32;
	private static final long PAGE_POOL_SIZE = 2 * 1024 * 1024;
	private static final int MAX_POIS = 20000;

	private final PoiRootElement root;
	private final Storage db;
	private PerstPoiCategoryManager categoryManager;

	public PerstPersistenceManager(String storageFileName) {
		db = StorageFactory.getInstance().createStorage();
		db.open(storageFileName, PAGE_POOL_SIZE);

		PoiRootElement tmpRoot = (PoiRootElement) db.getRoot();

		if (tmpRoot == null) {
			root = new PoiRootElement(db);
			db.setRoot(root);
		} else {
			root = tmpRoot;
		}

		for (NamedSpatialIndex index : root.spatialIndexIndex) {
			System.out.println(index.index.size() + " POIs FOR " + index.name);
		}

		categoryManager = PerstPoiCategoryManager.getInstance(fetchAllCategories());
	}

	@Override
	public void insertCategory(PoiCategory category) {
		if (category == null)
			throw new NullPointerException();

		PerstCategory perstCategory = new PerstCategory(category);
		root.categoryTitlePkIndex.put(perstCategory);
		root.addSpatialIndex(db, perstCategory.title);
		db.store(perstCategory);

		// reload catergoryManager
		categoryManager = PerstPoiCategoryManager.getInstance(fetchAllCategories());
	}

	@Override
	public void insertPointOfInterest(PointOfInterest poi) {
		if (poi == null)
			throw new NullPointerException();

		if (!categoryManager.contains(poi.category.title)) {
			throw new IllegalArgumentException("POI of unknown category, insert category first");
		}

		System.out.println("inserting: " + poi.toString());

		PerstPoi perstPoi = new PerstPoi(poi);
		root.poiIntegerIdPKIndex.put(perstPoi);
		root.poiCategoryFkIndex.put(perstPoi);

		Collection<PoiCategory> categories = categoryManager.ancestors(perstPoi.category);
		for (PoiCategory category : categories) {
			root.getSpatialIndex(category.title).put(
					new RectangleR2(perstPoi.latitude, perstPoi.longitude, perstPoi.latitude,
							perstPoi.longitude), perstPoi);
		}
		db.store(perstPoi);
	}

	@Override
	public Collection<PointOfInterest> findNearPosition(GeoPoint point, int radius,
			String category, int limit) {
		Collection<PointOfInterest> result = find(computeBoundingBox(point.getLatitude(), point
				.getLongitude(), radius), category, limit);

		// TODO: delete pois from result that are in the BB but not within the radius;

		return result;
	}

	@Override
	public Collection<PointOfInterest> findInRect(GeoPoint p1, GeoPoint p2, String categoryName) {
		RectangleR2 rect = new RectangleR2(Math.min(p1.getLatitude(), p2.getLatitude()), Math
				.min(p1.getLongitude(), p2.getLongitude()), Math.max(p1.getLatitude(), p2
				.getLatitude()), Math.max(p1.getLongitude(), p2.getLongitude()));
		return find(rect, categoryName, 0);
	}

	private Collection<PointOfInterest> find(RectangleR2 rect, String category, int limit) {
		if (!categoryManager.contains(category)) {
			return new ArrayList<PointOfInterest>(0);
		}

		ArrayList<PointOfInterest> result = new ArrayList<PointOfInterest>();

		ArrayList<PerstPoi> pois = root.getSpatialIndex(category).getList(rect);

		int max = limit <= 0 ? Integer.MAX_VALUE : limit;
		PerstPoi poi = null;

		for (int i = 0; i < pois.size() && result.size() < max; i++) {
			poi = pois.get(i);
			result.add(new PointOfInterest(poi.id, poi.latitude, poi.longitude, poi.name,
					poi.url, fetchCategory(poi.category)));
		}

		// long startSpatial = System.currentTimeMillis();
		// ArrayList<PerstPoi> pois = root.poiSpatialIndex.getList(rect);
		// System.out.println("SPATIAL took " + (System.currentTimeMillis() - startSpatial) +
		// " msecs");
		//	
		// Collection<PoiCategory> categories = categoryManager.descendants(category);
		// Collection<String> categoryNames = new ArrayList<String>(categories.size());
		//		
		// for (PoiCategory cat : categories) {
		// categoryNames.add(cat.title);
		// }
		//		
		// int max = limit <= 0 ? Integer.MAX_VALUE : limit;
		// long startCategories = System.currentTimeMillis();
		// ArrayList<PointOfInterest> result = new ArrayList<PointOfInterest>();
		// PerstPoi poi = null;
		//		
		// for (int i=0; i<pois.size() && result.size() < max; i++) {
		// poi = pois.get(i);
		// if (categoryNames.contains(poi.category)) {
		// result.add(new PointOfInterest(poi.id,
		// poi.latitude,
		// poi.longitude,
		// poi.name,
		// poi.url,
		// fetchCategory(poi.category)));
		// }
		// }
		//		
		// System.out.println("CATEGORY took " + (System.currentTimeMillis() - startCategories)
		// + " msecs");
		//		
		// // TODO: order pois by distance asc;

		return result;
	}

	@Override
	public void close() {
		db.close();
	}

	@Override
	public void removeCategory(PoiCategory category) {
		Key categoryFK = new Key(category.title);

		// TODO revamp removeCategory in PerstPersistenceManager

		// if poi count to large for main memory -> create DeleteQueue entries
		if (root.poiCategoryFkIndex.iterator(categoryFK, categoryFK, GenericIndex.ASCENT_ORDER)
				.size() > MAX_POIS) {
			IterableIterator<PerstPoi> pois = root.poiCategoryFkIndex.iterator(categoryFK,
					categoryFK, GenericIndex.ASCENT_ORDER);

			PerstPoi poi = null;
			while (pois.hasNext()) {
				poi = pois.next();
				DeleteQueue deleteQueue = new DeleteQueue(poi.id);
				root.deleteQueueIndex.put(deleteQueue);
				db.store(deleteQueue);
			}

			// Iterate over DeleteQueue objects and delete pois
			Iterator<DeleteQueue> deletes = root.deleteQueueIndex.iterator();
			DeleteQueue delete = null;
			while (deletes.hasNext()) {
				delete = deletes.next();
				PerstPoi perstPoi = root.poiIntegerIdPKIndex.get(new Key(delete.poiId));
				removePointOfInterest(perstPoi);
				db.deallocate(delete);
			}

			// clear deleteQueueIndex
			root.deleteQueueIndex.clear();

			// else, if poi count fits in memory just get list of all pois in this category
			// and delete them.
		} else {
			Collection<PerstPoi> pois = root.poiCategoryFkIndex.getList(categoryFK, categoryFK);
			for (PerstPoi poi : pois) {
				removePointOfInterest(poi);
			}
		}

		PerstCategory perstCategory = new PerstCategory(category);
		root.categoryTitlePkIndex.remove(perstCategory);
		root.removeSpatialIndex(category.title);
		db.deallocate(perstCategory);

		categoryManager = PerstPoiCategoryManager.getInstance(fetchAllCategories());
	}

	@Override
	public void removePointOfInterest(PointOfInterest poi) {
		removePointOfInterest(root.poiIntegerIdPKIndex.get(poi.id));
	}

	private void removePointOfInterest(PerstPoi perstPoi) {
		if (perstPoi == null)
			return;

		root.poiIntegerIdPKIndex.remove(perstPoi);

		Collection<PoiCategory> categories = categoryManager.ancestors(perstPoi.category);
		for (PoiCategory category : categories) {
			root.getSpatialIndex(category.title).remove(
					new RectangleR2(perstPoi.latitude, perstPoi.longitude, perstPoi.latitude,
							perstPoi.longitude), perstPoi);
		}

		root.poiCategoryFkIndex.remove(perstPoi);
		db.deallocate(perstPoi);
	}

	private Collection<PoiCategory> toPoiCategories(Collection<PerstCategory> categories) {
		Collection<PoiCategory> result = new ArrayList<PoiCategory>(categories.size());
		for (PerstCategory perstCategory : categories) {
			result.add(new PoiCategory(perstCategory.title,
					fetchCategory(perstCategory.parentTitle)));
		}
		return result;
	}

	private RectangleR2 computeBoundingBox(double latitude, double longitude, int radius) {
		double deltaLat = deltaLatitudeForRadius(radius);
		double deltaLng = deltaLongitudeForRadius(radius, latitude);

		double lat1 = latitude - deltaLat;
		double lng1 = longitude - deltaLng;
		double lat2 = latitude + deltaLat;
		double lng2 = longitude + deltaLng;

		return new RectangleR2(Math.min(lat1, lat2), Math.min(lng1, lng2),
				Math.max(lat1, lat2), Math.max(lng1, lng2));
	}

	private double deltaLongitudeForRadius(int radius, double latitude) {
		return Math.abs((new Double(radius) / 1000)
				/ (SPACE_BETWEEN_LATS_IN_KM * Math.cos(Math.toRadians(latitude))));
	}

	private double deltaLatitudeForRadius(int radius) {
		return Math.abs((new Double(radius) / 1000) / SPACE_BETWEEN_LATS_IN_KM);
	}

	private PoiCategory fetchCategory(String title) {
		if (title == null || title == "")
			return null;

		if (categoryManager != null)
			return categoryManager.get(title);

		PerstCategory perstCategory = root.categoryTitlePkIndex.get(new Key(title));
		if (perstCategory == null)
			return null;

		return new PoiCategory(perstCategory.title, fetchCategory(perstCategory.parentTitle));
	}

	private Collection<PoiCategory> fetchAllCategories() {
		return toPoiCategories(root.categoryTitlePkIndex);
	}

	@Override
	public void insertPointsOfInterest(IPoiReader poiReader) {
		Collection<PointOfInterest> pois = poiReader.read();
		for (PointOfInterest poi : pois) {
			insertPointOfInterest(poi);
		}
	}

	@Override
	public Collection<PoiCategory> allCategories() {
		return categoryManager.allCategories();
	}

	@Override
	public Collection<PoiCategory> descendants(String category) {
		return categoryManager.descendants(category);
	}

	@Override
	public void insertPointsOfInterest(Collection<PointOfInterest> pois) {
		for (PointOfInterest poi : pois) {
			insertPointOfInterest(poi);
		}
	}

}
