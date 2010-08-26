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
import java.util.HashMap;

import org.mapsforge.server.poi.PoiCategory;

class PerstPoiCategoryManager implements IPoiCategoryManager {

	public static PerstPoiCategoryManager getInstance(Collection<PerstCategory> categories) {
		return new PerstPoiCategoryManager(categories);
	}

	private class PoiCategoryTree {

		PerstCategory root;
		ArrayList<PoiCategoryTree> children;
		int orderNumber = 0;
		int firstChildOrderNumber = 0;

		PoiCategoryTree(PerstCategory category) {
			this.root = category;
			this.children = new ArrayList<PoiCategoryTree>();
		}

		void addChild(PoiCategoryTree child) {
			children.add(child);
		}

		Collection<PoiCategory> selfAndDescendants() {
			ArrayList<PoiCategory> result = new ArrayList<PoiCategory>();
			result.add(root);
			for (PoiCategoryTree child : children) {
				result.addAll(child.selfAndDescendants());
			}
			return result;
		}

		int assignOrderNumbers(int start, int level) {
			firstChildOrderNumber = start;
			orderNumber = start;
			if (!children.isEmpty()) {
				for (PoiCategoryTree child : children) {
					orderNumber = child.assignOrderNumbers(orderNumber, level + 1);
				}
			} else {
				firstChildOrderNumber++;
			}
			return ++orderNumber;
		}

	}

	private final HashMap<String, PoiCategoryTree> categoryMap;

	private PerstPoiCategoryManager(Collection<PerstCategory> categories) {
		categoryMap = new HashMap<String, PoiCategoryTree>(categories.size());

		for (PerstCategory cat : categories) {
			categoryMap.put(cat.title, new PoiCategoryTree(cat));
		}

		for (PerstCategory cat : categories) {
			if (cat.parent != null) {
				categoryMap.get(cat.parent.title).addChild(categoryMap.get(cat.title));
			}
		}

		ArrayList<PoiCategoryTree> roots = findRootTrees();
		int start = 0;
		for (PoiCategoryTree rootTree : roots) {
			start = rootTree.assignOrderNumbers(start, 0);
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
		PerstCategory cat = tree.root;

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
	public PerstCategory get(String categoryName) {
		PoiCategoryTree categoryTree = categoryMap.get(categoryName);
		return categoryTree != null ? categoryTree.root : null;
	}

	private ArrayList<PoiCategoryTree> findRootTrees() {
		ArrayList<PoiCategoryTree> roots = new ArrayList<PoiCategoryTree>();
		for (PoiCategoryTree tree : categoryMap.values()) {
			if (tree.root.parent == null) {
				roots.add(tree);
			}
		}
		return roots;
	}

	public int getOrderNumber(String category) {
		PoiCategoryTree categoryTree = categoryMap.get(category);
		return categoryTree != null ? categoryTree.orderNumber : 0;
	}

	public int getFirstChildOrderNumber(String category) {
		PoiCategoryTree categoryTree = categoryMap.get(category);
		return categoryTree != null ? categoryTree.firstChildOrderNumber : 0;
	}

}
