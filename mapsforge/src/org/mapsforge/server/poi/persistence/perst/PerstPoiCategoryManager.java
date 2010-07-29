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
import java.util.HashMap;

import org.mapsforge.server.poi.PoiCategory;
import org.mapsforge.server.poi.persistence.IPoiCategoryManager;

class PerstPoiCategoryManager implements IPoiCategoryManager {

	public static PerstPoiCategoryManager getInstance(Collection<PoiCategory> categories) {
		return new PerstPoiCategoryManager(categories);
	}

	private class PoiCategoryTree {

		private PoiCategory root;
		private ArrayList<PoiCategoryTree> children;

		public PoiCategoryTree(PoiCategory poi) {
			this.root = poi;
			this.children = new ArrayList<PoiCategoryTree>();
		}

		public void addChild(PoiCategoryTree child) {
			children.add(child);
		}

		public Collection<PoiCategory> selfAndDescendants() {
			ArrayList<PoiCategory> result = new ArrayList<PoiCategory>();
			result.add(root);
			for (PoiCategoryTree child : children) {
				result.addAll(child.selfAndDescendants());
			}
			return result;
		}
	}

	private final HashMap<String, PoiCategoryTree> categoryMap;

	private PerstPoiCategoryManager(Collection<PoiCategory> categories) {
		categoryMap = new HashMap<String, PoiCategoryTree>(categories.size());

		for (PoiCategory cat : categories) {
			categoryMap.put(cat.title, new PoiCategoryTree(cat));
		}

		for (PoiCategory cat : categories) {
			if (cat.parent != null) {
				categoryMap.get(cat.parent.title).addChild(categoryMap.get(cat.title));
			}
		}
	}

	@Override
	public Collection<PoiCategory> descendants(String category) {
		PoiCategoryTree tree = categoryMap.get(category);
		return (tree == null ? new ArrayList<PoiCategory>(0) : tree.selfAndDescendants());
	}

	public Collection<PoiCategory> ancestors(String category) {
		PoiCategoryTree tree = categoryMap.get(category);
		if (tree == null) {
			return new ArrayList<PoiCategory>(0);
		}

		ArrayList<PoiCategory> categories = new ArrayList<PoiCategory>();
		PoiCategory cat = tree.root;

		categories.add(cat);

		while (cat.parent != null) {
			cat = cat.parent;
			categories.add(cat);
		}

		return categories;
	}

	@Override
	public Collection<PoiCategory> allCategories() {
		Collection<PoiCategoryTree> trees = categoryMap.values();
		Collection<PoiCategory> result = new ArrayList<PoiCategory>(trees.size());

		for (PoiCategoryTree tree : trees) {
			result.add(tree.root);
		}
		return result;
	}

	@Override
	public boolean contains(String category) {
		return categoryMap.get(category) != null;
	}

	@Override
	public PoiCategory get(String categoryName) {
		PoiCategoryTree categoryTree = categoryMap.get(categoryName);
		return categoryTree != null ? categoryTree.root : null;
	}

}
