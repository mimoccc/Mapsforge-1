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

import org.garret.perst.FieldIndex;
import org.garret.perst.Persistent;
import org.garret.perst.SpatialIndexR2;
import org.garret.perst.Storage;

class PoiRootElement extends Persistent {

	public FieldIndex<PerstPoi> poiIntegerIdPKIndex;
	public FieldIndex<PerstPoi> poiCategoryFkIndex;
	public FieldIndex<PerstCategory> categoryTitlePkIndex;
	public FieldIndex<DeleteQueue> deleteQueueIndex;
	public FieldIndex<NamedSpatialIndex> spatialIndexIndex;

	public PoiRootElement() {
	}

	public PoiRootElement(Storage db) {
		super(db);
		poiIntegerIdPKIndex = db.<PerstPoi> createFieldIndex(PerstPoi.class, "id", true);
		poiCategoryFkIndex = db.<PerstPoi> createFieldIndex(PerstPoi.class, "category", false);
		categoryTitlePkIndex = db.<PerstCategory> createFieldIndex(PerstCategory.class,
				"title", true);
		deleteQueueIndex = db.<DeleteQueue> createFieldIndex(DeleteQueue.class, "poiId", true);
		spatialIndexIndex = db.<NamedSpatialIndex> createFieldIndex(NamedSpatialIndex.class,
				"name", true);
	}

	public void addSpatialIndex(Storage db, String categoryName) {
		SpatialIndexR2<PerstPoi> index = db.<PerstPoi> createSpatialIndexR2();
		NamedSpatialIndex namedIndex = new NamedSpatialIndex(categoryName, index);

		spatialIndexIndex.add(namedIndex);
		db.store(namedIndex);
	}

	public void removeSpatialIndex(String categoryName) {
		NamedSpatialIndex namedIndex = spatialIndexIndex.get(categoryName);
		spatialIndexIndex.remove(namedIndex);
		namedIndex.index.clear();
		namedIndex.index.deallocate();
		namedIndex.deallocate();
	}

	public SpatialIndexR2<PerstPoi> getSpatialIndex(String categoryName) {
		NamedSpatialIndex namedIndex = spatialIndexIndex.get(categoryName);
		return namedIndex == null ? null : namedIndex.index;
	}

}