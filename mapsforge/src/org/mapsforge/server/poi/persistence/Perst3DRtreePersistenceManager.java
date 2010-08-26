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
package org.mapsforge.server.poi.persistence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.garret.perst.FieldIndex;
import org.garret.perst.Storage;
import org.mapsforge.server.poi.PoiCategory;
import org.mapsforge.server.poi.PointOfInterest;

class Perst3DRtreePersistenceManager extends AbstractPerstPersistenceManager<Poi3DRootElement> {

	private static final int CATEGORY_SPREAD_FACTOR = 5;

	private class PerstPoiIterator implements Iterator<PerstPoi> {
		private final Iterator<PointOfInterest> poiIterator;

		public PerstPoiIterator(Iterator<PointOfInterest> poiIterator) {
			this.poiIterator = poiIterator;
		}

		@Override
		public boolean hasNext() {
			return this.poiIterator.hasNext();
		}

		@Override
		public PerstPoi next() {
			PointOfInterest poi = poiIterator.next();
			return new PerstPoi(poi, categoryManager.get(poi.getCategory().getTitle()));
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	private class PackEntryIterator implements Iterator<PackEntry<RtreeBox, PerstPoi>> {

		private final Iterator<ClusterEntry> iterator;

		public PackEntryIterator(Iterator<ClusterEntry> iterator) {
			super();
			this.iterator = iterator;
		}

		public boolean hasNext() {
			return iterator.hasNext();
		}

		public PackEntry<RtreeBox, PerstPoi> next() {
			if (!iterator.hasNext()) {
				return null;
			}
			ClusterEntry entry = iterator.next();
			PerstPoi poi = entry.poi;

			int x = poi.latitude;
			int y = poi.longitude;
			int z = categoryManager.getOrderNumber(poi.category.title) * CATEGORY_SPREAD_FACTOR;
			RtreeBox box = new RtreeBox(x, y, z);

			return new PackEntry<RtreeBox, PerstPoi>(box, poi);
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}

	}

	public Perst3DRtreePersistenceManager(String storageFileName) {
		super(storageFileName);
	}

	@Override
	protected Collection<PointOfInterest> find(Rect rect, String category, int limit) {
		if (!categoryManager.contains(category)) {
			return new ArrayList<PointOfInterest>(0);
		}

		ArrayList<PointOfInterest> result = new ArrayList<PointOfInterest>();
		ArrayList<PerstPoi> pois = root.spatialIndex.getList(createBoxFromRectAndCategory(rect,
				category));

		// TODO: order pois by distance asc;

		int max = limit <= 0 ? Integer.MAX_VALUE : limit;

		if (max >= pois.size()) {
			result.addAll(pois);
		} else {
			result.addAll(pois.subList(0, max));
		}

		return result;
	}

	@Override
	protected ClusterEntry generateClusterEntry(PerstPoi poi) {
		int z = categoryManager.getOrderNumber(poi.category.title) * CATEGORY_SPREAD_FACTOR;
		return new ClusterEntry(poi, Hilbert.computeValue3D(poi.longitude, poi.latitude, z));
	}

	@Override
	protected Poi3DRootElement initRootElement(Storage storage) {
		return new Poi3DRootElement(storage);
	}

	@Override
	public void clusterStorage() {
		// TODO Auto-generated method stub

	}

	@Override
	public void insertPointOfInterest(PointOfInterest poi) {
		if (poi == null)
			throw new NullPointerException();

		PerstCategory category = categoryManager.get(poi.getCategory().getTitle());

		if (category == null) {
			throw new IllegalArgumentException("POI of unknown category, insert category first");
		}

		PerstPoi perstPoi = new PerstPoi(poi, category);
		root.poiIntegerIdPKIndex.put(perstPoi);
		root.poiCategoryFkIndex.put(perstPoi);

		root.spatialIndex.put(perstPoi, createBoxFromRectAndCategory(new Rect(
				perstPoi.latitude, perstPoi.longitude, perstPoi.latitude, perstPoi.longitude),
				category.title));

		db.store(perstPoi);
	}

	@Override
	public void packInsert(Iterator<PointOfInterest> poiIterator,
			Collection<PoiCategory> categories) {
		for (PoiCategory category : categories) {
			this.insertCategory(category);
			System.out.println("added " + category.toString());
		}

		FieldIndex<ClusterEntry> clusterIndex = createClusterIndex(new PerstPoiIterator(
				poiIterator));

		root.spatialIndex.packInsert(new PackEntryIterator(clusterIndex.iterator()), db);

		Iterator<ClusterEntry> clusterIterator = clusterIndex.iterator();

		int i = 0;
		PerstPoi poi = null;
		while (clusterIterator.hasNext()) {
			i++;
			poi = clusterIterator.next().poi;
			root.poiCategoryFkIndex.add(poi);
			root.poiIntegerIdPKIndex.add(poi);
			db.store(poi);
		}
		System.out.println("added " + i + " pois to storage");
	}

	@Override
	public void removeCategory(PoiCategory category) {
		// TODO Auto-generated method stub

	}

	@Override
	public void removePointOfInterest(PointOfInterest poi) {
		removePointOfInterest(root.poiIntegerIdPKIndex.get(poi.getId()));
	}

	private void removePointOfInterest(PerstPoi perstPoi) {
		if (perstPoi == null)
			return;

		root.poiIntegerIdPKIndex.remove(perstPoi);

		root.spatialIndex.remove(perstPoi, createBoxFromRectAndCategory(new Rect(
				perstPoi.latitude, perstPoi.longitude, perstPoi.latitude, perstPoi.longitude),
				perstPoi.category.title));

		root.poiCategoryFkIndex.remove(perstPoi);
		db.deallocate(perstPoi);
	}

	private RtreeBox createBoxFromRectAndCategory(Rect rect, String category) {
		return new RtreeBox(rect.top, rect.bottom, rect.left, rect.right, categoryManager
				.getOrderNumber(category)
				* CATEGORY_SPREAD_FACTOR, categoryManager.getFirstChildOrderNumber(category)
				* CATEGORY_SPREAD_FACTOR);
	}

}
