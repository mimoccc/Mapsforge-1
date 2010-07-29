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

import java.util.Collection;

import org.mapsforge.server.poi.PoiCategory;
import org.mapsforge.server.poi.PointOfInterest;
import org.mapsforge.server.poi.exchange.IPoiReader;

/**
 * Abstracts from an underlying Storage/DB by providing methods for inserting/deleting/search
 * {@link PointOfInterest} and {@link PoiCategory} objects in named Storage/DB.<br/>
 * <br/>
 * Remember to call the {@link #close()} method as soon as your done manipulating the Storage/DB
 * via this {@link IPersistenceManager}.
 * 
 * @author weise
 * 
 */
public interface IPersistenceManager extends IPoiQuery {

	/**
	 * Insert a {@link PoiCategory} into storage.
	 * 
	 * @param category
	 *            {@link PoiCategory} to insert into storage.
	 */
	public void insertCategory(PoiCategory category);

	/**
	 * Insert a single {@link PointOfInterest} into storage.
	 * 
	 * @param poi
	 *            {@link PointOfInterest} to insert into storage.
	 */
	public void insertPointOfInterest(PointOfInterest poi);

	/**
	 * Insert {@link PointOfInterest} into this {@link IPersistenceManager}.
	 * 
	 * @param pois
	 *            collection of {@link PointOfInterest} to insert into storage.
	 */
	public void insertPointsOfInterest(Collection<PointOfInterest> pois);

	/**
	 * Insert {@link PointOfInterest} from a {@link IPoiReader} instance.
	 * 
	 * @param poiReader
	 *            the {@link IPoiReader} fetching the {@link PointOfInterest}s.
	 */
	public void insertPointsOfInterest(IPoiReader poiReader);

	/**
	 * Removes a {@link PoiCategory} from this {@link IPersistenceManager}.
	 * 
	 * @param category
	 *            the {@link PoiCategory} to be removed given by its unique title.
	 */
	public void removeCategory(PoiCategory category);

	/**
	 * Removes a point of interest from this {@link IPersistenceManager}.
	 * 
	 * @param poi
	 *            the {@link PointOfInterest} to be removed.
	 */
	public void removePointOfInterest(PointOfInterest poi);

	/**
	 * Use this to get a {@link Collection} of all {@link PoiCategory} managed by this
	 * {@link IPersistenceManager}.
	 * 
	 * @return a Collection of {@link PoiCategory} objects.
	 */
	public Collection<PoiCategory> allCategories();

	/**
	 * @param category
	 *            {@link PoiCategory} given by its unique title.
	 * @return A collection of {@link PoiCategory} objects containing the given category itself
	 *         and all of its descendants.
	 */
	public Collection<PoiCategory> descendants(String category);

	/**
	 * Use this to free claimed resources. After that you might no longer be able to
	 * insert/remove/search points of interest and categories. This should be called as soon as
	 * you are done working with this {@link IPersistenceManager}.
	 */
	public void close();

}
