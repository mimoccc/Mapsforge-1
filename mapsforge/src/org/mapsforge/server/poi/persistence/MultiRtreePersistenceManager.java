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
import org.garret.perst.GenericIndex;
import org.garret.perst.IterableIterator;
import org.garret.perst.Key;
import org.garret.perst.Storage;
import org.mapsforge.server.poi.PoiCategory;
import org.mapsforge.server.poi.PointOfInterest;

/**
 * {@link IPersistenceManager} for Perst which uses multiple rtrees to index the pois.
 * 
 * @author weise
 * 
 */
public class MultiRtreePersistenceManager extends
		AbstractPerstPersistenceManager<PoiRootElement> {

	/**
	 * @param storageFileName
	 *            filename of the perst storage file that should be used.
	 */
	public MultiRtreePersistenceManager(String storageFileName) {
		super(storageFileName);
	}

	@Override
	public boolean insertCategory(PoiCategory category) {
		if (super.insertCategory(category)) {
			root.addSpatialIndex(db, category.getTitle());
			return true;
		}
		return false;
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

		Collection<PoiCategory> categories = categoryManager.ancestors(perstPoi.category.title);
		for (PoiCategory cat : categories) {
			root.getSpatialIndex(cat.getTitle()).put(
					perstPoi,
					new Rect(perstPoi.latitude, perstPoi.longitude, perstPoi.latitude,
							perstPoi.longitude));
		}
		db.store(perstPoi);
	}

	@Override
	protected Collection<PointOfInterest> find(Rect rect, String category, int limit) {
		if (!categoryManager.contains(category)) {
			return new ArrayList<PointOfInterest>(0);
		}

		ArrayList<PointOfInterest> result = new ArrayList<PointOfInterest>();

		ArrayList<PerstPoi> pois = root.getSpatialIndex(category).getList(rect);

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
	public void removeCategory(PoiCategory category) {
		Collection<PoiCategory> descendants = categoryManager.descendants(category.getTitle());

		IterableIterator<PerstPoi> pois = null;
		Key categoryFK = null;
		Key poiPK = null;
		DeleteQueue deleteQueueEntry = null;
		for (PoiCategory descendant : descendants) {
			categoryFK = new Key(categoryManager.get(descendant.getTitle()));
			pois = root.poiCategoryFkIndex.iterator(categoryFK, categoryFK,
					GenericIndex.ASCENT_ORDER);

			while (pois.hasNext()) {
				deleteQueueEntry = new DeleteQueue(pois.next().id);
				root.deleteQueueIndex.add(deleteQueueEntry);
			}

			Iterator<DeleteQueue> queueIterator = root.deleteQueueIndex.iterator();
			while (queueIterator.hasNext()) {
				poiPK = new Key(queueIterator.next().poiId);
				removePointOfInterest(root.poiIntegerIdPKIndex.get(poiPK));
			}
			root.deleteQueueIndex.clear();

			root.removeSpatialIndex(descendant.getTitle());
			root.categoryTitlePkIndex.remove(descendant);
			db.deallocate(descendant);
		}

		categoryManager = PerstPoiCategoryManager.getInstance(root.categoryTitlePkIndex);
	}

	@Override
	public void removePointOfInterest(PointOfInterest poi) {
		removePointOfInterest(root.poiIntegerIdPKIndex.get(poi.getId()));
	}

	private void removePointOfInterest(PerstPoi perstPoi) {
		if (perstPoi == null)
			return;

		root.poiIntegerIdPKIndex.remove(perstPoi);

		Collection<PoiCategory> categories = categoryManager.ancestors(perstPoi.category.title);
		for (PoiCategory category : categories) {
			root.getSpatialIndex(category.getTitle()).remove(
					perstPoi,
					new Rect(perstPoi.latitude, perstPoi.longitude, perstPoi.latitude,
							perstPoi.longitude));
		}

		root.poiCategoryFkIndex.remove(perstPoi);
		db.deallocate(perstPoi);
	}

	@Override
	public Collection<PoiCategory> allCategories() {
		return categoryManager.allCategories();
	}

	@Override
	public void clusterStorage() {
		MultiRtreePersistenceManager destinationManager = new MultiRtreePersistenceManager(
				fileName + ".dest");

		// create temporary index for cluster value
		FieldIndex<ClusterEntry> clusterIndex = createClusterIndex(root.poiIntegerIdPKIndex
				.iterator());

		Collection<PoiCategory> categories = this.allCategories();

		for (PoiCategory category : categories) {
			destinationManager.insertCategory(category);
		}

		Iterator<ClusterEntry> clusterIterator = clusterIndex.iterator();
		while (clusterIterator.hasNext()) {
			destinationManager.insertPointOfInterest(clusterIterator.next().poi);
		}

		destinationManager.close();

		// TODO delete old db and rename new one to old name
	}

	@Override
	protected PoiRootElement initRootElement(Storage database) {
		return new PoiRootElement(database);
	}

	@Override
	protected ClusterEntry generateClusterEntry(PerstPoi poi) {
		return new ClusterEntry(poi, Hilbert.computeValue(poi.latitude, poi.longitude));
	}

	@Override
	public void packInsert(Iterator<PointOfInterest> poiIterator,
			Collection<PoiCategory> categories) {
		throw new UnsupportedOperationException();
	}

}
