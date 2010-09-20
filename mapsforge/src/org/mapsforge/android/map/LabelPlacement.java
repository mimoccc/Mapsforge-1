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
package org.mapsforge.android.map;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.PriorityQueue;

import org.mapsforge.android.map.SweepLineRect.Intersection;

import android.graphics.Bitmap;
import android.graphics.Paint;

/**
 * This class place the labels form POIs, area labels and normal labels. The main target is
 * avoiding collisions of these different labels. So that every single label can be read easily.
 * 
 * @author Steffen Reichelt
 */

class LabelPlacement {

	private int placementOption = 0;
	// You can choose between 2 Position and 4 Position
	// placement Model 0 - 2-Position 1 - 4 Position

	// distance adjustments
	private int startDistanceToSymbols = 4;
	private int symbolDistanceToSymbol = 2;
	private int labelDistanceToSymbol = 2;
	private int labelDistanceToLabel = 2;

	// variables for the dependency cache
	private Hashtable<Tile, DependencyCache> dependencyTable;
	private Tile currentTile;
	private DependencyCache currentDependencyCache;

	/* inner classes */

	/**
	 * The class holds the data for a label with dependencies on other tiles.
	 */
	private class DependencyText {
		public int depCounter;
		final Paint paintFront;
		final Paint paintBack;
		final String text;
		LinkedList<Tile> tiles;
		final android.graphics.Rect boundary;

		/**
		 * Creates a text dependency in the dependency cache.
		 * 
		 * @param paintFront
		 *            paint element from the front.
		 * @param paintBack
		 *            paint element form the background of the text.
		 * @param text
		 *            the text of the element.
		 * @param boundary
		 *            the fixed boundary with width and height.
		 * @param tile
		 *            all tile in where the element has an influence.
		 */
		public DependencyText(Paint paintFront, Paint paintBack, String text,
				android.graphics.Rect boundary, Tile tile) {
			this.depCounter = 0;
			this.paintFront = paintFront;
			this.paintBack = paintBack;
			this.text = text;
			this.tiles = new LinkedList<Tile>();
			this.tiles.add(tile);
			this.boundary = boundary;
		}

		void addTile(Tile tile) {
			tiles.add(tile);
		}
	}

	/**
	 * The class holds the data for a symbol with dependencies on other tiles.
	 */
	private class DependencySymbol {
		Bitmap symbol;
		public int depCounter;
		private LinkedList<Tile> tiles;

		/**
		 * Creates a symbol dependency element for the dependency cache
		 * 
		 * @param symbol
		 *            reference on the dependency symbol.
		 * @param tile
		 *            dependency tile.
		 */
		public DependencySymbol(Bitmap symbol, Tile tile) {
			this.depCounter = 0;
			this.symbol = symbol;
			this.tiles = new LinkedList<Tile>();
			tiles.add(tile);
		}

		/**
		 * Adds an additional tile, which has an dependency with this symbol
		 * 
		 * @param tile
		 *            additional tile.
		 */
		void addTile(Tile tile) {
			tiles.add(tile);
		}
	}

	/**
	 * The class holds the data for a symbol with dependencies on other tiles.
	 * 
	 * @param <Type>
	 *            only two types are reasonable. The DependencySymbol or DependencyText class.
	 */
	private class Dependency<Type> {
		final Type value;
		Point point;

		public Dependency(Type value, Point point) {
			this.value = value;
			this.point = point;
		}

	}

	/**
	 * This class holds the reference positions for the two and four point greedy algorithms.
	 */
	private class ReferencePosition {
		final float x;
		final float y;
		final int nodeNumber;
		final float width;
		final float height;

		SymbolContainer symbol;

		public ReferencePosition(float x, float y, int nodeNumber, float width, float height,
				SymbolContainer symbol) {
			this.x = x;
			this.y = y;
			this.nodeNumber = nodeNumber;
			this.width = width;
			this.height = height;
			this.symbol = symbol;
		}

	}

	/**
	 * This class is assigned to one specific tile. It holds all the elements that must be
	 * noticed for the label placement oft the assigned tile.
	 */
	private class DependencyCache {
		boolean drawn; //				
		ArrayList<Dependency<DependencyText>> labels; //
		ArrayList<Dependency<DependencySymbol>> symbols; //

		public DependencyCache() {
			this.labels = null;
			this.symbols = null;
			drawn = false;
		}

		public void addText(Dependency<DependencyText> toAdd) {
			if (labels == null)
				labels = new ArrayList<Dependency<DependencyText>>();
			labels.add(toAdd);
		}

		public void addSymbol(Dependency<DependencySymbol> toAdd) {
			if (symbols == null)
				symbols = new ArrayList<Dependency<DependencySymbol>>();
			symbols.add(toAdd);
		}

	}

	public LabelPlacement() {
		dependencyTable = new Hashtable<Tile, DependencyCache>(60);
	}

	/* main */

	/**
	 * The inputs are all the label and symbol objects of the current tile. The output is
	 * overlap free label and symbol placement with the greedy strategy. The placement model is
	 * either the two fixed point or the four fixed point model.
	 * 
	 * @param labels
	 *            labels from the current tile.
	 * @param symbols
	 *            symbols of the current tile.
	 * @param areaLabels
	 *            area labels from the current tile.
	 * @param cT
	 *            current tile with the x,y- coordinates and the zoom level.
	 * @return the processed list of labels.
	 */
	public ArrayList<PointTextContainer> placeLabels(ArrayList<PointTextContainer> labels,
			ArrayList<SymbolContainer> symbols, ArrayList<PointTextContainer> areaLabels,
			Tile cT) {

		generateTileAndDependencyCache(cT);

		preprocessAreaLabels(areaLabels);

		preprocessLabels(labels);

		preprocessSymbols(symbols);

		removeEmptySymbolReferences(labels, symbols);

		removeOverlappingSymbolsWithAreaLabels(symbols, areaLabels);

		removeOverlappingObjectsWithDependencyCache(labels, areaLabels, symbols);

		if (labels.size() != 0)
			switch (placementOption) {
				case 0:
					labels = processTwoPointGreedy(labels, symbols, areaLabels);
					break;
				case 1:
					labels = processFourPointGreedy(labels, symbols, areaLabels);
					break;
				default:
					break;
			}

		currentDependencyCache.drawn = true;

		if ((labels.size() > 0) || (symbols.size() > 0) || (areaLabels.size() > 0)) {
			fillDependencyCache(labels, symbols, areaLabels);
		}

		if (currentDependencyCache.labels != null)
			addLabelsFromDependencyCache(labels);
		if (currentDependencyCache.symbols != null)
			addSymbolsFromDependencyCache(symbols);

		return labels;
	}

	/**
	 * Removes all objects that overlaps with the objects from the dependency cache.
	 * 
	 * @param labels
	 *            labels from the current tile
	 * @param areaLabels
	 *            area labels from the current tile
	 * @param symbols
	 *            symbols from the current tile
	 */
	private void removeOverlappingObjectsWithDependencyCache(
			ArrayList<PointTextContainer> labels, ArrayList<PointTextContainer> areaLabels,
			ArrayList<SymbolContainer> symbols) {

		if (currentDependencyCache.labels != null)
			if (currentDependencyCache.labels.size() != 0) {
				removeOverlappingLabelsWithDependencyLabels(labels);
				removeOverlappingSymbolsWithDependencyLabels(symbols);
				removeOverlappingAreaLabelsWithDependencyLabels(areaLabels);
			}

		if (currentDependencyCache.symbols != null)
			if (currentDependencyCache.symbols.size() != 0) {
				removeOverlappingSymbolsWithDepencySymbols(symbols);
				removeOverlappingAreaLabelsWithDependencySymbols(areaLabels);
			}

	}

	private void removeOverlappingAreaLabelsWithDependencyLabels(
			ArrayList<PointTextContainer> areaLabels) {
		android.graphics.Rect rect1;
		android.graphics.Rect rect2;

		PointTextContainer pTC;

		for (Dependency<DependencyText> label : currentDependencyCache.labels) {

			rect1 = (new android.graphics.Rect((int) (label.point.x),
					(int) (label.point.y - label.value.boundary.height()),
					(int) (label.point.x + label.value.boundary.width()), (int) (label.point.y)));

			for (int x = 0; x < areaLabels.size(); x++) {
				pTC = areaLabels.get(x);

				rect2 = new android.graphics.Rect((int) pTC.x, (int) pTC.y
						- pTC.boundary.height(), (int) pTC.x + pTC.boundary.width(),
						(int) pTC.y);

				if (android.graphics.Rect.intersects(rect2, rect1)) {
					areaLabels.remove(x);
					x--;
				}

			}
		}

	}

	private void removeOverlappingAreaLabelsWithDependencySymbols(
			ArrayList<PointTextContainer> areaLabels) {
		android.graphics.Rect rect1;
		android.graphics.Rect rect2;

		PointTextContainer label;

		for (Dependency<DependencySymbol> smb : currentDependencyCache.symbols) {

			rect1 = new android.graphics.Rect((int) smb.point.x, (int) smb.point.y,
					(int) smb.point.x + smb.value.symbol.getWidth(), (int) smb.point.y
							+ smb.value.symbol.getHeight());

			for (int x = 0; x < areaLabels.size(); x++) {
				label = areaLabels.get(x);

				rect2 = (new android.graphics.Rect((int) (label.x),
						(int) (label.y - label.boundary.height()),
						(int) (label.x + label.boundary.width()), (int) (label.y)));

				if (android.graphics.Rect.intersects(rect2, rect1)) {
					areaLabels.remove(x);
					x--;
				}
			}
		}
	}

	private void generateTileAndDependencyCache(Tile cT) {
		currentTile = new Tile(cT.x, cT.y, cT.zoomLevel);
		currentDependencyCache = dependencyTable.get(currentTile);

		if (currentDependencyCache == null) {
			this.dependencyTable.put(currentTile, new DependencyCache());
			currentDependencyCache = dependencyTable.get(currentTile);
		}

	}

	private void addLabelsFromDependencyCache(ArrayList<PointTextContainer> labels) {
		for (Dependency<DependencyText> label : currentDependencyCache.labels) {

			labels.add(new PointTextContainer(label.value.text, label.point.x, label.point.y,
					label.value.paintFront, label.value.paintBack));
			label.value.depCounter++;
		}

	}

	private void addSymbolsFromDependencyCache(ArrayList<SymbolContainer> symbols) {
		for (Dependency<DependencySymbol> smb : currentDependencyCache.symbols) {
			symbols.add(new SymbolContainer(smb.value.symbol, smb.point.x, smb.point.y));
			smb.value.depCounter++;
		}

	}

	private void removeOverlappingSymbolsWithDepencySymbols(ArrayList<SymbolContainer> symbols) {
		android.graphics.Rect rect1;
		android.graphics.Rect rect2;

		SymbolContainer sym;
		Dependency<DependencySymbol> sym2;
		int dis = symbolDistanceToSymbol;

		for (int x = 0; x < currentDependencyCache.symbols.size(); x++) {
			sym2 = currentDependencyCache.symbols.get(x);
			rect1 = new android.graphics.Rect((int) sym2.point.x - dis, (int) sym2.point.y
					- dis, (int) sym2.point.x + sym2.value.symbol.getWidth() + dis,
					(int) sym2.point.y + sym2.value.symbol.getHeight() + dis);

			for (int y = 0; y < symbols.size(); y++) {

				sym = symbols.get(y);
				rect2 = (new android.graphics.Rect((int) sym.x, (int) sym.y, (int) sym.x
						+ sym.symbol.getWidth(), (int) sym.y + sym.symbol.getHeight()));

				if (android.graphics.Rect.intersects(rect2, rect1)) {
					symbols.remove(y);
					y--;

				}
			}
		}

	}

	private void removeEmptySymbolReferences(ArrayList<PointTextContainer> nodes,
			ArrayList<SymbolContainer> symbols) {
		for (PointTextContainer label : nodes) {
			if (!symbols.contains(label.symbol))
				label.symbol = null;
		}

	}

	/**
	 * This method uses an adapted greedy strategy for the fixed two position model, above and
	 * under. It uses no priority search tree, because it will not function with symbols only
	 * with points. Instead it uses two minimum heaps. They work similar to a sweep line
	 * algorithm but have not a O(n log n +k) runtime. To find the rectangle that has the
	 * leftest edge, I use also a minimum Heap. The rectangles are sorted by their x
	 * coordinates.
	 * 
	 * @param labels
	 *            label positions and text
	 * @param symbols
	 *            symbol positions
	 * @param areaLabels
	 *            area label positions and text
	 * @return list of labels without overlaps with symbols and other labels by the two fixed
	 *         position greedy strategy
	 */
	private ArrayList<PointTextContainer> processTwoPointGreedy(
			ArrayList<PointTextContainer> labels, ArrayList<SymbolContainer> symbols,
			ArrayList<PointTextContainer> areaLabels) {

		ArrayList<PointTextContainer> resolutionSet = new ArrayList<PointTextContainer>();
		// Array for the generated reference positions around the points of interests
		ReferencePosition[] refPos = new ReferencePosition[(labels.size() * 2)];

		// lists that sorts the reference points after the minimum right edge x position
		PriorityQueue<ReferencePosition> priorRight = new PriorityQueue<ReferencePosition>(
				labels.size() * 2 + labels.size() / 10 * 2,
				new Comparator<ReferencePosition>() {

					@Override
					public int compare(ReferencePosition x, ReferencePosition y) {
						if (x.x + x.width < y.x + y.width)
							return -1;

						if (x.x + x.width > y.x + y.width)
							return 1;

						return 0;
					}
				});
		// lists that sorts the reference points after the minimum left edge x position
		PriorityQueue<ReferencePosition> priorLeft = new PriorityQueue<ReferencePosition>(
				labels.size() * 2 + labels.size() / 10 * 2,
				new Comparator<ReferencePosition>() {

					@Override
					public int compare(ReferencePosition x, ReferencePosition y) {
						if (x.x < y.x)
							return -1;

						if (x.x > y.x)
							return 1;

						return 0;
					}
				});

		// creates the reference positions
		for (int z = 0; z < labels.size(); z++) {
			PointTextContainer label = labels.get(z);

			if (label.symbol != null) {
				refPos[z * 2] = new ReferencePosition(label.x - (label.boundary.width() / 2)
						- 0.1f, label.y - label.boundary.height() - startDistanceToSymbols, z,
						label.boundary.width(), label.boundary.height(), label.symbol);
				refPos[z * 2 + 1] = new ReferencePosition(label.x
						- (label.boundary.width() / 2), label.y
						+ label.symbol.symbol.getHeight() + startDistanceToSymbols, z,
						label.boundary.width(), label.boundary.height(), label.symbol);
			} else {
				refPos[z * 2] = new ReferencePosition(label.x - (label.boundary.width() / 2)
						- 0.1f, label.y, z, label.boundary.width(), label.boundary.height(),
						null);
				refPos[z * 2 + 1] = null;
			}

		}

		// removes reference positions that overlaps with other symbols or dependency objects
		removeNonValidateReferencePosition(refPos, symbols, areaLabels);

		for (ReferencePosition reference : refPos)
			if (reference != null) {
				priorLeft.add(reference);
				priorRight.add(reference);
			}

		// variable for the leftest bounding box of the
		ReferencePosition leftest;
		while (priorRight.size() != 0) {

			leftest = priorRight.remove();

			PointTextContainer label = labels.get(leftest.nodeNumber);

			resolutionSet.add(new PointTextContainer(label.text, leftest.x, leftest.y,
					label.paintFront, label.paintBack, leftest.symbol));

			// Removes the other position that is a possible position for the label of one point
			// of interest

			priorRight.remove(refPos[leftest.nodeNumber * 2 + 1]);
			if (priorRight.size() == 0) {

				return resolutionSet;
			}

			priorLeft.remove(leftest);
			priorLeft.remove((refPos[leftest.nodeNumber * 2 + 1]));

			// find overlapping labels and deletes the reference points and delete them
			LinkedList<ReferencePosition> linkedRef = new LinkedList<ReferencePosition>();

			while (priorLeft.size() != 0) {
				if (priorLeft.peek().x < leftest.x + leftest.width)
					linkedRef.add(priorLeft.remove());
				else {
					break;
				}
			}

			// brute Force collision test (faster then sweep line for a small amount of
			// objects)
			for (int i = 0; i < linkedRef.size(); i++) {
				if ((linkedRef.get(i).x <= leftest.x + leftest.width)
						&& (linkedRef.get(i).y >= leftest.y - linkedRef.get(i).height)
						&& (linkedRef.get(i).y <= leftest.y + linkedRef.get(i).height)) {
					priorRight.remove(linkedRef.get(i));
					linkedRef.remove(i);
					i--;
				}
			}
			priorLeft.addAll(linkedRef);

		}

		return resolutionSet;
	}

	/**
	 * This method uses an adapted greedy strategy for the fixed four position model, above,
	 * under left and right form the point of interest. It uses no priority search tree, because
	 * it will not function with symbols only with points. Instead it uses two minimum heaps.
	 * They work similar to a sweep line algorithm but have not a O(n log n +k) runtime. To find
	 * the rectangle that has the top edge, I use also a minimum Heap. The rectangles are sorted
	 * by their y coordinates.
	 * 
	 * @param labels
	 *            label positions and text
	 * @param symbols
	 *            symbol positions
	 * @param areaLabels
	 *            area label positions and text
	 * @return list of labels without overlaps with symbols and other labels by the four fixed
	 *         position greedy strategy
	 */
	private ArrayList<PointTextContainer> processFourPointGreedy(
			ArrayList<PointTextContainer> labels, ArrayList<SymbolContainer> symbols,
			ArrayList<PointTextContainer> areaLabels) {

		ArrayList<PointTextContainer> resolutionSet = new ArrayList<PointTextContainer>();

		// Array for the generated reference positions around the points of interests
		ReferencePosition[] refPos = new ReferencePosition[(labels.size()) * 4];

		// lists that sorts the reference points after the minimum top edge y position
		PriorityQueue<ReferencePosition> priorUp = new PriorityQueue<ReferencePosition>(labels
				.size()
				* 4 * 2 + labels.size() / 10 * 2, new Comparator<ReferencePosition>() {

			@Override
			public int compare(ReferencePosition x, ReferencePosition y) {
				if (x.y < y.y)
					return -1;

				if (x.y > y.y)
					return 1;

				return 0;
			}
		});
		// lists that sorts the reference points after the minimum bottom edge y position
		PriorityQueue<ReferencePosition> priorDown = new PriorityQueue<ReferencePosition>(
				labels.size() * 4 * 2 + labels.size() / 10 * 2,
				new Comparator<ReferencePosition>() {

					@Override
					public int compare(ReferencePosition x, ReferencePosition y) {
						if (x.y - x.height < y.y - y.height)
							return -1;

						if (x.y - x.height > y.y - y.height)
							return 1;

						return 0;
					}
				});

		PointTextContainer tmp;
		int dis = startDistanceToSymbols;

		// creates the reference positions
		for (int z = 0; z < labels.size(); z++) {
			if (labels.get(z) != null) {
				if (labels.get(z).symbol != null) {
					tmp = labels.get(z);

					// up
					refPos[z * 4] = new ReferencePosition(tmp.x - tmp.boundary.width() / 2,
							tmp.y - tmp.symbol.symbol.getHeight() / 2 - dis, z, tmp.boundary
									.width(), tmp.boundary.height(), tmp.symbol);
					// down
					refPos[z * 4 + 1] = new ReferencePosition(tmp.x - tmp.boundary.width() / 2,
							tmp.y + tmp.symbol.symbol.getHeight() / 2 + tmp.boundary.height()
									+ dis, z, tmp.boundary.width(), tmp.boundary.height(),
							tmp.symbol);
					// left
					refPos[z * 4 + 2] = new ReferencePosition(tmp.x
							- tmp.symbol.symbol.getWidth() / 2 - tmp.boundary.width() - dis,
							tmp.y + tmp.boundary.height() / 2, z, tmp.boundary.width(),
							tmp.boundary.height(), tmp.symbol);
					// right
					refPos[z * 4 + 3] = new ReferencePosition(tmp.x
							+ tmp.symbol.symbol.getWidth() / 2 + dis, tmp.y
							+ tmp.boundary.height() / 2 - 0.1f, z, tmp.boundary.width(),
							tmp.boundary.height(), tmp.symbol);
				} else {
					refPos[z * 4] = new ReferencePosition(labels.get(z).x
							- ((labels.get(z).boundary.width()) / 2), labels.get(z).y, z,
							labels.get(z).boundary.width(), labels.get(z).boundary.height(),
							null);
					refPos[z * 4 + 1] = null;
					refPos[z * 4 + 2] = null;
					refPos[z * 4 + 3] = null;
				}
			}
		}

		removeNonValidateReferencePosition(refPos, symbols, areaLabels);

		// do while it gives reference positions
		for (ReferencePosition reference : refPos)
			if (reference != null) {
				priorUp.add(reference);
				priorDown.add(reference);
			}

		ReferencePosition leftest;
		while (priorUp.size() != 0) {

			leftest = priorUp.remove();

			PointTextContainer label = labels.get(leftest.nodeNumber);

			resolutionSet.add(new PointTextContainer(label.text, leftest.x, leftest.y,
					label.paintFront, label.paintBack, label.symbol));

			if (priorUp.size() == 0) {

				return resolutionSet;
			}

			priorUp.remove(refPos[leftest.nodeNumber * 4 + 0]);
			priorUp.remove(refPos[leftest.nodeNumber * 4 + 1]);
			priorUp.remove(refPos[leftest.nodeNumber * 4 + 2]);
			priorUp.remove(refPos[leftest.nodeNumber * 4 + 3]);

			priorDown.remove((refPos[leftest.nodeNumber * 4 + 0]));
			priorDown.remove((refPos[leftest.nodeNumber * 4 + 1]));
			priorDown.remove((refPos[leftest.nodeNumber * 4 + 2]));
			priorDown.remove((refPos[leftest.nodeNumber * 4 + 3]));

			LinkedList<ReferencePosition> linkedRef = new LinkedList<ReferencePosition>();

			while (priorDown.size() != 0) {
				if (priorDown.peek().x < leftest.x + leftest.width)
					linkedRef.add(priorDown.remove());
				else {
					break;
				}
			}
			// brute Force collision test (faster then sweep line for a small amount of
			// objects)
			for (int i = 0; i < linkedRef.size(); i++) {
				if ((linkedRef.get(i).x <= leftest.x + leftest.width)
						&& (linkedRef.get(i).y >= leftest.y - linkedRef.get(i).height)
						&& (linkedRef.get(i).y <= leftest.y + linkedRef.get(i).height)) {
					priorUp.remove(linkedRef.get(i));
					linkedRef.remove(i);
					i--;
				}
			}
			priorDown.addAll(linkedRef);
		}

		return resolutionSet;

	}

	private void removeNonValidateReferencePosition(ReferencePosition[] refPos,
			ArrayList<SymbolContainer> symbols, ArrayList<PointTextContainer> areaLabels) {

		android.graphics.Rect rect1;
		android.graphics.Rect rect2;

		int dis = labelDistanceToSymbol;

		for (SymbolContainer smb : symbols) {

			rect1 = new android.graphics.Rect((int) smb.x - dis, (int) smb.y - dis, (int) smb.x
					+ smb.symbol.getWidth() + dis, (int) smb.y + smb.symbol.getHeight() + dis);

			for (int y = 0; y < refPos.length; y++) {
				if (refPos[y] != null) {

					rect2 = new android.graphics.Rect((int) refPos[y].x,
							(int) (refPos[y].y - refPos[y].height),
							(int) (refPos[y].x + refPos[y].width), (int) (refPos[y].y));

					if (android.graphics.Rect.intersects(rect2, rect1)) {
						refPos[y] = null;
					}
				}

			}
		}

		dis = labelDistanceToLabel;

		for (PointTextContainer areaLabel : areaLabels) {

			rect1 = new android.graphics.Rect((int) areaLabel.x - dis, (int) areaLabel.y
					- areaLabel.boundary.height() - dis, (int) areaLabel.x
					+ areaLabel.boundary.width() + dis, (int) areaLabel.y + dis);

			for (int y = 0; y < refPos.length; y++) {
				if (refPos[y] != null) {

					rect2 = new android.graphics.Rect((int) refPos[y].x,
							(int) (refPos[y].y - refPos[y].height),
							(int) (refPos[y].x + refPos[y].width), (int) (refPos[y].y));

					if (android.graphics.Rect.intersects(rect2, rect1)) {
						refPos[y] = null;
					}
				}

			}
		}
		// removes all Reverence Points that are in already drawn areas Runtime n
		Tile lefttmp = new Tile(currentTile.x - 1, currentTile.y, currentTile.zoomLevel);
		Tile righttmp = new Tile(currentTile.x + 1, currentTile.y, currentTile.zoomLevel);
		Tile uptmp = new Tile(currentTile.x, currentTile.y - 1, currentTile.zoomLevel);
		Tile downtmp = new Tile(currentTile.x, currentTile.y + 1, currentTile.zoomLevel);

		boolean up;
		boolean left;
		boolean right;
		boolean down;
		DependencyCache tmp;

		tmp = dependencyTable.get(lefttmp);
		left = tmp == null ? false : tmp.drawn;

		tmp = dependencyTable.get(righttmp);
		right = tmp == null ? false : tmp.drawn;

		tmp = dependencyTable.get(uptmp);
		up = tmp == null ? false : tmp.drawn;

		tmp = dependencyTable.get(downtmp);
		down = tmp == null ? false : tmp.drawn;

		ReferencePosition ref;

		for (int i = 0; i < refPos.length; i++) {
			ref = refPos[i];

			if (ref == null)
				continue;
			if (up)
				if (ref.y - ref.height < 0) {
					refPos[i] = null;
					continue;
				}

			if (down)
				if (ref.y > 256.0f) {
					refPos[i] = null;
					continue;
				}
			if (left)
				if (ref.x < 0) {
					refPos[i] = null;
					continue;
				}
			if (right)
				if (ref.x + ref.width > 256.0f) {
					refPos[i] = null;
					continue;
				}
		}

		// removes all Reverence Points that intersects with Labels from the Depency Cache

		dis = labelDistanceToLabel;
		if (currentDependencyCache != null)
			if (currentDependencyCache.labels != null) {
				for (Dependency<DependencyText> label : currentDependencyCache.labels) {

					rect1 = new android.graphics.Rect((int) label.point.x - dis,
							(int) (label.point.y - label.value.boundary.height()) - dis,
							(int) (label.point.x + label.value.boundary.width() + dis),
							(int) (label.point.y + dis));

					for (int y = 0; y < refPos.length; y++) {
						if (refPos[y] != null) {
							rect2 = new android.graphics.Rect((int) refPos[y].x,
									(int) (refPos[y].y - refPos[y].height),
									(int) (refPos[y].x + refPos[y].width), (int) (refPos[y].y));

							if (android.graphics.Rect.intersects(rect2, rect1)) {
								refPos[y] = null;
							}
						}
					}

				}
			}
	}

	/* Process Symbols */

	private void preprocessSymbols(ArrayList<SymbolContainer> symbols) {

		removeOutOfTileSymbols(symbols);
		removeOverlappingSymbolsOld(symbols);
		removeSymbolsFromDrawnAreas(symbols);
	}

	private void removeSymbolsFromDrawnAreas(ArrayList<SymbolContainer> symbols) {
		Tile lefttmp = new Tile(currentTile.x - 1, currentTile.y, currentTile.zoomLevel);
		Tile righttmp = new Tile(currentTile.x + 1, currentTile.y, currentTile.zoomLevel);
		Tile uptmp = new Tile(currentTile.x, currentTile.y - 1, currentTile.zoomLevel);
		Tile downtmp = new Tile(currentTile.x, currentTile.y + 1, currentTile.zoomLevel);

		boolean up;
		boolean left;
		boolean right;
		boolean down;
		DependencyCache tmp;

		tmp = dependencyTable.get(lefttmp);
		left = tmp == null ? false : tmp.drawn;

		tmp = dependencyTable.get(righttmp);
		right = tmp == null ? false : tmp.drawn;

		tmp = dependencyTable.get(uptmp);
		up = tmp == null ? false : tmp.drawn;

		tmp = dependencyTable.get(downtmp);
		down = tmp == null ? false : tmp.drawn;

		SymbolContainer ref;

		for (int i = 0; i < symbols.size(); i++) {
			ref = symbols.get(i);

			if (up)
				if (ref.y < 0) {
					symbols.remove(i);
					i--;
					continue;
				}

			if (down)
				if (ref.y + ref.symbol.getHeight() > 256.0f) {
					symbols.remove(i);
					i--;
					continue;
				}
			if (left)
				if (ref.x < 0) {
					symbols.remove(i);
					i--;
					continue;
				}
			if (right)
				if (ref.x + ref.symbol.getWidth() > 256.0f) {
					symbols.remove(i);
					i--;
					continue;
				}
		}

	}

	private void removeOverlappingSymbolsWithDependencyLabels(ArrayList<SymbolContainer> symbols) {

		android.graphics.Rect rect1;
		android.graphics.Rect rect2;

		SymbolContainer smb;

		for (Dependency<DependencyText> label : currentDependencyCache.labels) {

			rect1 = (new android.graphics.Rect((int) (label.point.x),
					(int) (label.point.y - label.value.boundary.height()),
					(int) (label.point.x + label.value.boundary.width()), (int) (label.point.y)));

			for (int x = 0; x < symbols.size(); x++) {
				smb = symbols.get(x);

				rect2 = new android.graphics.Rect((int) smb.x, (int) smb.y, (int) smb.x
						+ smb.symbol.getWidth(), (int) smb.y + smb.symbol.getHeight());

				if (android.graphics.Rect.intersects(rect2, rect1)) {
					symbols.remove(x);
					x--;
				}

			}
		}

	}

	private void removeOverlappingSymbolsWithAreaLabels(ArrayList<SymbolContainer> symbols,
			ArrayList<PointTextContainer> pTC) {
		android.graphics.Rect rect1;
		android.graphics.Rect rect2;

		SymbolContainer sym;
		PointTextContainer label;

		int dis = labelDistanceToSymbol;

		for (int x = 0; x < pTC.size(); x++) {
			label = pTC.get(x);

			rect1 = new android.graphics.Rect((int) label.x - dis,
					(int) (label.y - label.boundary.height()) - dis, (int) (label.x
							+ label.boundary.width() + dis), (int) (label.y + dis));

			for (int y = 0; y < symbols.size(); y++) {
				sym = symbols.get(y);

				rect2 = (new android.graphics.Rect((int) sym.x, (int) sym.y,
						(int) (sym.x + sym.symbol.getWidth()), (int) (sym.y + sym.symbol
								.getHeight())));

				if (android.graphics.Rect.intersects(rect1, rect2)) {
					symbols.remove(y);
					y--;
				}
			}
		}
	}

	private void removeOutOfTileSymbols(ArrayList<SymbolContainer> symbols) {
		SymbolContainer smb;

		for (int i = 0; i < symbols.size();) {
			smb = symbols.get(i);

			if (smb.x > 256.0f) {
				symbols.remove(i);

			} else if (smb.y > 256.0f) {
				symbols.remove(i);

			} else if (smb.x + smb.symbol.getWidth() < 0.0f) {
				symbols.remove(i);

			} else if (smb.y + smb.symbol.getHeight() < 0.0f) {
				symbols.remove(i);

			} else {
				i++;
			}

		}

	}

	public void removeOverlappingSymbolsOld(ArrayList<SymbolContainer> symbols) {

		android.graphics.Rect rect1;
		android.graphics.Rect rect2;

		SymbolContainer sym;
		int dis = symbolDistanceToSymbol;

		for (int x = 0; x < symbols.size(); x++) {
			sym = symbols.get(x);
			rect1 = new android.graphics.Rect((int) sym.x - dis, (int) sym.y - dis, (int) sym.x
					+ sym.symbol.getWidth() + dis, (int) sym.y + sym.symbol.getHeight() + dis);

			for (int y = x + 1; y < symbols.size(); y++) {
				if (y != x) {
					sym = symbols.get(y);
					rect2 = (new android.graphics.Rect((int) sym.x, (int) sym.y, (int) sym.x
							+ sym.symbol.getWidth(), (int) sym.y + sym.symbol.getHeight()));

					if (android.graphics.Rect.intersects(rect2, rect1)) {
						symbols.remove(y);
						y--;
					}
				}
			}
		}
	}

	public void deleteOverlappingSymbolsNew(ArrayList<SymbolContainer> symbols) {
		ArrayList<Rectangle<?>> boundarysSymbol = new ArrayList<Rectangle<?>>();

		for (SymbolContainer smb : symbols) {
			boundarysSymbol.add(new Rectangle<SymbolContainer>(smb, new android.graphics.Rect(
					(int) smb.x,
					(int) smb.y, (int) smb.x + smb.symbol.getWidth(), (int) smb.y
							+ smb.symbol.getHeight())));
		}

		if (boundarysSymbol.size() != 0) {
			SweepLineRect swp = new SweepLineRect();
			LinkedList<Intersection> intersections = swp.sweep(boundarysSymbol);

			for (Intersection inter : intersections) {
				if ((inter.a.value != null) && (inter.b.value != null)) {
					SymbolContainer a = (SymbolContainer) inter.a.value;
					SymbolContainer b = (SymbolContainer) inter.b.value;
					if (a.x < b.x) {
						symbols.remove(a);
					} else {
						symbols.remove(b);
					}
				}
			}
		}

	}

	private void preprocessAreaLabels(ArrayList<PointTextContainer> areaLabels) {
		centerLabels(areaLabels);

		removeOutOfTileAreaLabels(areaLabels);

		removeOverlappingAreaLabels(areaLabels);

		if (areaLabels.size() != 0)
			removeAreaLabelsInalreadyDrawnareas(areaLabels);
	}

	private void removeAreaLabelsInalreadyDrawnareas(ArrayList<PointTextContainer> areaLabels) {
		Tile lefttmp = new Tile(currentTile.x - 1, currentTile.y, currentTile.zoomLevel);
		Tile righttmp = new Tile(currentTile.x + 1, currentTile.y, currentTile.zoomLevel);
		Tile uptmp = new Tile(currentTile.x, currentTile.y - 1, currentTile.zoomLevel);
		Tile downtmp = new Tile(currentTile.x, currentTile.y + 1, currentTile.zoomLevel);

		boolean up;
		boolean left;
		boolean right;
		boolean down;
		DependencyCache tmp;

		tmp = dependencyTable.get(lefttmp);
		left = tmp == null ? false : tmp.drawn;

		tmp = dependencyTable.get(righttmp);
		right = tmp == null ? false : tmp.drawn;

		tmp = dependencyTable.get(uptmp);
		up = tmp == null ? false : tmp.drawn;

		tmp = dependencyTable.get(downtmp);
		down = tmp == null ? false : tmp.drawn;

		PointTextContainer label;

		for (int i = 0; i < areaLabels.size(); i++) {
			label = areaLabels.get(i);

			if (up)
				if (label.y - label.boundary.height() < 0.0f) {
					areaLabels.remove(i);
					i--;
					continue;
				}

			if (down)
				if (label.y > 256.0f) {
					areaLabels.remove(i);
					i--;
					continue;
				}
			if (left)
				if (label.x < 0.0f) {
					areaLabels.remove(i);
					i--;
					continue;
				}
			if (right)
				if (label.x + label.boundary.width() > 256.0f) {
					areaLabels.remove(i);
					i--;
					continue;
				}
		}

	}

	private void removeOutOfTileAreaLabels(ArrayList<PointTextContainer> areaLabels) {
		PointTextContainer label;

		for (int i = 0; i < areaLabels.size(); i++) {
			label = areaLabels.get(i);

			if (label.x > 256.0f) {
				areaLabels.remove(i);

				i--;
			} else if (label.y - label.boundary.height() > 256.0f) {
				areaLabels.remove(i);

				i--;
			} else if (label.x + label.boundary.width() < 0.0f) {
				areaLabels.remove(i);

				i--;
			} else if (label.y + label.boundary.height() < 0.0f) {
				areaLabels.remove(i);

				i--;
			}
		}
	}

	public void removeOverlappingAreaLabels(ArrayList<PointTextContainer> areaLabels) {
		android.graphics.Rect rect1;
		android.graphics.Rect rect2;

		PointTextContainer label;

		int dis = labelDistanceToLabel;

		for (int x = 0; x < areaLabels.size(); x++) {
			label = areaLabels.get(x);
			rect1 = new android.graphics.Rect((int) label.x - dis, (int) label.y - dis,
					(int) (label.x + label.boundary.width()) + dis, (int) (label.y
							+ label.boundary.height() + dis));

			for (int y = x + 1; y < areaLabels.size(); y++) {
				if (y != x) {
					label = areaLabels.get(y);
					rect2 = new android.graphics.Rect((int) label.x, (int) label.y,
							(int) (label.x + label.boundary.width()),
							(int) (label.y + label.boundary.height()));

					if (android.graphics.Rect.intersects(rect1, rect2)) {
						areaLabels.remove(y);

						y--;
					}
				}
			}
		}
	}

	/*
	 * Process Labels
	 */

	private void preprocessLabels(ArrayList<PointTextContainer> labels) {

		removeOutOfTileLabels(labels);
	}

	private void removeOutOfTileLabels(ArrayList<PointTextContainer> labels) {
		PointTextContainer label;

		for (int i = 0; i < labels.size();) {
			label = labels.get(i);

			if (label.x - label.boundary.width() / 2 > 256.0f) {
				labels.remove(i);
				label = null;

			} else if (label.y - label.boundary.height() > 256.0f) {
				labels.remove(i);
				label = null;

			} else if ((label.x - label.boundary.width() / 2 + label.boundary.width()) < 0.0f) {
				labels.remove(i);
				label = null;

			} else if (label.y < 0.0f) {
				labels.remove(i);
				label = null;

			} else {
				i++;

			}
		}
	}

	/*
	 * Depency Cache
	 */

	private void fillDependencyCache(ArrayList<PointTextContainer> labels,
			ArrayList<SymbolContainer> symbols, ArrayList<PointTextContainer> areaLabels) {

		Tile left = new Tile(currentTile.x - 1, currentTile.y, currentTile.zoomLevel);
		Tile right = new Tile(currentTile.x + 1, currentTile.y, currentTile.zoomLevel);
		Tile up = new Tile(currentTile.x, currentTile.y - 1, currentTile.zoomLevel);
		Tile down = new Tile(currentTile.x, currentTile.y + 1, currentTile.zoomLevel);

		Tile leftup = new Tile(currentTile.x - 1, currentTile.y - 1, currentTile.zoomLevel);
		Tile leftdown = new Tile(currentTile.x - 1, currentTile.y + 1, currentTile.zoomLevel);
		Tile rightup = new Tile(currentTile.x + 1, currentTile.y - 1, currentTile.zoomLevel);
		Tile rightdown = new Tile(currentTile.x + 1, currentTile.y + 1, currentTile.zoomLevel);

		if (dependencyTable.get(up) == null) {
			dependencyTable.put(up, new DependencyCache());
		}
		if (dependencyTable.get(down) == null) {
			dependencyTable.put(down, new DependencyCache());
		}
		if (dependencyTable.get(left) == null) {
			dependencyTable.put(left, new DependencyCache());
		}
		if (dependencyTable.get(right) == null) {
			dependencyTable.put(right, new DependencyCache());
		}

		if (dependencyTable.get(leftdown) == null) {
			dependencyTable.put(leftdown, new DependencyCache());
		}
		if (dependencyTable.get(rightup) == null) {
			dependencyTable.put(rightup, new DependencyCache());
		}
		if (dependencyTable.get(leftup) == null) {
			dependencyTable.put(leftup, new DependencyCache());
		}
		if (dependencyTable.get(rightdown) == null) {
			dependencyTable.put(rightdown, new DependencyCache());
		}

		fillDependencyLabels(labels);
		fillDependencyLabels(areaLabels);

		DependencyCache linkedDep;
		DependencySymbol addSmb;

		for (SymbolContainer symbol : symbols) {
			addSmb = null;

			// up
			if ((symbol.y < 0.0f) && (!dependencyTable.get(up).drawn)) {
				linkedDep = dependencyTable.get(up);

				addSmb = new DependencySymbol(symbol.symbol, currentTile);
				currentDependencyCache.addSymbol((new Dependency<DependencySymbol>(addSmb,
						new Point(symbol.x, symbol.y))));
				addSmb.depCounter++;

				linkedDep.addSymbol((new Dependency<DependencySymbol>(addSmb, new Point(
						symbol.x, symbol.y + 256))));
				addSmb.addTile(up);

				if ((symbol.x < 0.0f) && (!dependencyTable.get(leftup).drawn)) {
					linkedDep = dependencyTable.get(leftup);

					linkedDep.addSymbol((new Dependency<DependencySymbol>(addSmb, new Point(
							symbol.x + 256, symbol.y + 256))));
					addSmb.addTile(leftup);
				}

				if ((symbol.x + symbol.symbol.getWidth() > 256.0f)
						&& (!dependencyTable.get(rightup).drawn)) {
					linkedDep = dependencyTable.get(rightup);

					linkedDep.addSymbol((new Dependency<DependencySymbol>(addSmb, new Point(
							symbol.x - 256, symbol.y + 256))));
					addSmb.addTile(rightup);
				}

			}

			// down
			if ((symbol.y + symbol.symbol.getHeight() > 256.0f)
					&& (!dependencyTable.get(down).drawn)) {

				linkedDep = dependencyTable.get(down);

				if (addSmb == null) {
					addSmb = new DependencySymbol(symbol.symbol, currentTile);
					currentDependencyCache.addSymbol((new Dependency<DependencySymbol>(addSmb,
							new Point(symbol.x, symbol.y))));
					addSmb.depCounter++;
				}

				linkedDep.addSymbol((new Dependency<DependencySymbol>(addSmb, new Point(
						symbol.x, symbol.y - 256))));
				addSmb.addTile(down);

				if ((symbol.x < 0.0f) && (!dependencyTable.get(leftdown).drawn)) {
					linkedDep = dependencyTable.get(leftdown);

					linkedDep.addSymbol((new Dependency<DependencySymbol>(addSmb, new Point(
							symbol.x + 256, symbol.y - 256))));
					addSmb.addTile(leftdown);

				}

				if ((symbol.x + symbol.symbol.getWidth() > 256.0f)
						&& (!dependencyTable.get(rightdown).drawn)) {

					linkedDep = dependencyTable.get(rightdown);

					linkedDep.addSymbol((new Dependency<DependencySymbol>(addSmb, new Point(
							symbol.x - 256, symbol.y - 256))));
					addSmb.addTile(rightdown);

				}
			}
			// left

			if ((symbol.x < 0.0f) && (!dependencyTable.get(left).drawn)) {
				linkedDep = dependencyTable.get(left);

				if (addSmb == null) {
					addSmb = new DependencySymbol(symbol.symbol, currentTile);
					currentDependencyCache.addSymbol((new Dependency<DependencySymbol>(addSmb,
							new Point(symbol.x, symbol.y))));
					addSmb.depCounter++;
				}

				linkedDep.addSymbol((new Dependency<DependencySymbol>(addSmb, new Point(
						symbol.x + 256.0f, symbol.y))));
				addSmb.addTile(left);

			}
			// right
			if ((symbol.x + symbol.symbol.getWidth() > 256.0f)
					&& (!dependencyTable.get(right).drawn)) {
				linkedDep = dependencyTable.get(right);
				if (addSmb == null) {
					addSmb = new DependencySymbol(symbol.symbol, currentTile);
					currentDependencyCache.addSymbol((new Dependency<DependencySymbol>(addSmb,
							new Point(symbol.x, symbol.y))));
					addSmb.depCounter++;
				}

				linkedDep.addSymbol((new Dependency<DependencySymbol>(addSmb, new Point(
						symbol.x - 256.0f, symbol.y))));
				addSmb.addTile(right);
			}
		}

	}

	/**
	 * fills the dependency entry from the tile and the neighbor tiles with the dependency
	 * information, that are necessary for drawing. To do that every label and symbol that will
	 * be drawn, will be checked if it produces dependencies with other tiles.
	 * 
	 * @param pTC
	 *            list of the labels
	 */
	private void fillDependencyLabels(ArrayList<PointTextContainer> pTC) {
		Tile left = new Tile(currentTile.x - 1, currentTile.y, currentTile.zoomLevel);
		Tile right = new Tile(currentTile.x + 1, currentTile.y, currentTile.zoomLevel);
		Tile up = new Tile(currentTile.x, currentTile.y - 1, currentTile.zoomLevel);
		Tile down = new Tile(currentTile.x, currentTile.y + 1, currentTile.zoomLevel);

		Tile leftup = new Tile(currentTile.x - 1, currentTile.y - 1, currentTile.zoomLevel);
		Tile leftdown = new Tile(currentTile.x - 1, currentTile.y + 1, currentTile.zoomLevel);
		Tile rightup = new Tile(currentTile.x + 1, currentTile.y - 1, currentTile.zoomLevel);
		Tile rightdown = new Tile(currentTile.x + 1, currentTile.y + 1, currentTile.zoomLevel);

		PointTextContainer label;
		DependencyCache linkedDep;
		DependencyText toAdd;

		for (int i = 0; i < pTC.size(); i++) {

			label = pTC.get(i);

			toAdd = null;

			// up
			if ((label.y - label.boundary.height() < 0.0f) && (!dependencyTable.get(up).drawn)) {
				linkedDep = dependencyTable.get(up);

				toAdd = new DependencyText(label.paintFront, label.paintBack, label.text,
						label.boundary, currentTile);

				currentDependencyCache.addText(new Dependency<DependencyText>(toAdd, new Point(
						label.x, label.y)));

				toAdd.depCounter++;

				linkedDep.addText(new Dependency<DependencyText>(toAdd, new Point(label.x,
						label.y + 256)));

				toAdd.addTile(up);

				if ((label.x < 0.0f) && (!dependencyTable.get(leftup).drawn)) {
					linkedDep = dependencyTable.get(leftup);

					linkedDep.addText(new Dependency<DependencyText>(toAdd, new Point(
							label.x + 256, label.y + 256)));

					toAdd.addTile(leftup);

				}

				if ((label.x + label.boundary.width() > 256.0f)
						&& (!dependencyTable.get(rightup).drawn)) {
					linkedDep = dependencyTable.get(rightup);

					linkedDep.addText(new Dependency<DependencyText>(toAdd, new Point(
							label.x - 256, label.y + 256)));

					toAdd.addTile(rightup);

				}

			}

			// down
			if ((label.y > 256.0f) && (!dependencyTable.get(down).drawn)) {

				linkedDep = dependencyTable.get(down);

				if (toAdd == null) {
					toAdd = new DependencyText(label.paintFront, label.paintBack, label.text,
							label.boundary, currentTile);

					currentDependencyCache.addText(new Dependency<DependencyText>(toAdd,
							new Point(label.x, label.y)));

					toAdd.depCounter++;

				}

				linkedDep.addText(new Dependency<DependencyText>(toAdd, new Point(label.x,
						label.y - 256)));

				toAdd.addTile(down);

				if ((label.x < 0.0f) && (!dependencyTable.get(leftdown).drawn)) {
					linkedDep = dependencyTable.get(leftdown);

					linkedDep.addText(new Dependency<DependencyText>(toAdd, new Point(
							label.x + 256, label.y - 256)));

					toAdd.addTile(leftdown);

				}

				if ((label.x + label.boundary.width() > 256.0f)
						&& (!dependencyTable.get(rightdown).drawn)) {

					linkedDep = dependencyTable.get(rightdown);

					linkedDep.addText(new Dependency<DependencyText>(toAdd, new Point(
							label.x - 256, label.y - 256)));

					toAdd.addTile(rightdown);

				}

			}
			// left

			if ((label.x < 0.0f) && (!dependencyTable.get(left).drawn)) {
				linkedDep = dependencyTable.get(left);

				if (toAdd == null) {
					toAdd = new DependencyText(label.paintFront, label.paintBack, label.text,
							label.boundary, currentTile);

					currentDependencyCache.addText(new Dependency<DependencyText>(toAdd,
							new Point(label.x, label.y)));

					toAdd.depCounter++;

				}

				linkedDep.addText(new Dependency<DependencyText>(toAdd, new Point(
						label.x + 256, label.y)));

				toAdd.addTile(left);

			}
			// right
			if ((label.x + label.boundary.width() > 256.0f)
					&& (!dependencyTable.get(right).drawn)) {
				linkedDep = dependencyTable.get(right);

				if (toAdd == null) {
					toAdd = new DependencyText(label.paintFront, label.paintBack, label.text,
							label.boundary, currentTile);

					currentDependencyCache.addText(new Dependency<DependencyText>(toAdd,
							new Point(label.x, label.y)));

					toAdd.depCounter++;

				}

				linkedDep.addText(new Dependency<DependencyText>(toAdd, new Point(
						label.x - 256, label.y)));

				toAdd.addTile(right);

			}

			// check symbols

			if ((label.symbol != null) && (toAdd == null)) {

				if ((label.symbol.y <= 0.0f) && (!dependencyTable.get(up).drawn)) {
					linkedDep = dependencyTable.get(up);

					toAdd = new DependencyText(label.paintFront, label.paintBack, label.text,
							label.boundary, currentTile);

					currentDependencyCache.addText(new Dependency<DependencyText>(toAdd,
							new Point(label.x, label.y)));

					toAdd.depCounter++;

					linkedDep.addText(new Dependency<DependencyText>(toAdd, new Point(label.x,
							label.y + 256)));

					toAdd.addTile(up);

					if ((label.symbol.x < 0.0f) && (!dependencyTable.get(leftup).drawn)) {
						linkedDep = dependencyTable.get(leftup);

						linkedDep.addText(new Dependency<DependencyText>(toAdd, new Point(
								label.x + 256, label.y + 256)));

						toAdd.addTile(leftup);

					}

					if ((label.symbol.x + label.symbol.symbol.getWidth() > 256.0f)
							&& (!dependencyTable.get(rightup).drawn)) {
						linkedDep = dependencyTable.get(rightup);

						linkedDep.addText(new Dependency<DependencyText>(toAdd, new Point(
								label.x - 256, label.y + 256)));

						toAdd.addTile(rightup);

					}

				}

				if ((label.symbol.y + label.symbol.symbol.getHeight() >= 256.0f)
						&& (!dependencyTable.get(down).drawn)) {

					linkedDep = dependencyTable.get(down);

					if (toAdd == null) {
						toAdd = new DependencyText(label.paintFront, label.paintBack,
								label.text, label.boundary, currentTile);

						currentDependencyCache.addText(new Dependency<DependencyText>(toAdd,
								new Point(label.x, label.y)));

						toAdd.depCounter++;

					}

					linkedDep.addText(new Dependency<DependencyText>(toAdd, new Point(label.x,
							label.y + 256)));

					toAdd.addTile(up);

					if ((label.symbol.x < 0.0f) && (!dependencyTable.get(leftdown).drawn)) {
						linkedDep = dependencyTable.get(leftdown);

						linkedDep.addText(new Dependency<DependencyText>(toAdd, new Point(
								label.x + 256, label.y - 256)));

						toAdd.addTile(leftdown);

					}

					if ((label.symbol.x + label.symbol.symbol.getWidth() > 256.0f)
							&& (!dependencyTable.get(rightdown).drawn)) {

						linkedDep = dependencyTable.get(rightdown);

						linkedDep.addText(new Dependency<DependencyText>(toAdd, new Point(
								label.x - 256, label.y - 256)));

						toAdd.addTile(rightdown);

					}
				}

				if ((label.symbol.x <= 0.0f) && (!dependencyTable.get(left).drawn)) {
					linkedDep = dependencyTable.get(left);

					if (toAdd == null) {
						toAdd = new DependencyText(label.paintFront, label.paintBack,
								label.text, label.boundary, currentTile);

						currentDependencyCache.addText(new Dependency<DependencyText>(toAdd,
								new Point(label.x, label.y)));

						toAdd.depCounter++;

					}

					linkedDep.addText(new Dependency<DependencyText>(toAdd, new Point(
							label.x - 256, label.y)));

					toAdd.addTile(left);

				}

				if ((label.symbol.x + label.symbol.symbol.getWidth() >= 256.0f)
						&& (!dependencyTable.get(right).drawn)) {
					linkedDep = dependencyTable.get(right);

					if (toAdd == null) {
						toAdd = new DependencyText(label.paintFront, label.paintBack,
								label.text, label.boundary, currentTile);

						currentDependencyCache.addText(new Dependency<DependencyText>(toAdd,
								new Point(label.x, label.y)));

						toAdd.depCounter++;

					}

					linkedDep.addText(new Dependency<DependencyText>(toAdd, new Point(
							label.x + 256, label.y)));

					toAdd.addTile(right);

				}

			}
		}

	}

	private void removeOverlappingLabelsWithDependencyLabels(
			ArrayList<PointTextContainer> labels) {

		for (int i = 0; i < currentDependencyCache.labels.size(); i++) {
			for (int x = 0; x < labels.size(); x++) {
				if ((labels.get(x).text.equals(currentDependencyCache.labels.get(i).value.text))
						&& (labels.get(x).paintFront.equals(currentDependencyCache.labels
								.get(i).value.paintFront))
						&& (labels.get(x).paintBack
								.equals(currentDependencyCache.labels.get(i).value.paintBack))) {
					labels.remove(x);
					i--;
					break;
				}
			}
		}

	}

	private void centerLabels(ArrayList<PointTextContainer> labels) {

		for (PointTextContainer label : labels) {
			label.x = label.x - label.boundary.width() / 2;

		}

	}

	public void onDeleteTile(Tile tile) {
		Tile Tile = new Tile(tile.x, tile.y, tile.zoomLevel);
		DependencyCache cache = dependencyTable.get(Tile);

		if (cache == null)
			return;
		if (isDependenyEmpty(cache)) {
			dependencyTable.remove(Tile);
			return;
		}

		for (Dependency<DependencyText> label : cache.labels) {
			label.value.depCounter--;
			if (label.value.depCounter == 0)
				for (Tile tmpTile : label.value.tiles) {
					dependencyTable.get(tmpTile).labels.remove(label);
				}
		}

		if (isDependenyEmpty(cache)) {
			dependencyTable.remove(Tile);
			return;
		}

		// test if all six connected tiles can be deleted

		// up
		Tile = new Tile(tile.x, tile.y - 1, tile.zoomLevel);
		cache = dependencyTable.get(Tile);
		if (cache != null)
			if (isDependenyEmpty(cache)) {
				dependencyTable.remove(Tile);
				return;
			}

		// down
		Tile = new Tile(tile.x, tile.y + 1, tile.zoomLevel);
		cache = dependencyTable.get(Tile);
		if (cache != null)
			if (isDependenyEmpty(cache)) {
				dependencyTable.remove(Tile);
				return;
			}
		// left
		Tile = new Tile(tile.x - 1, tile.y, tile.zoomLevel);
		cache = dependencyTable.get(Tile);
		if (cache != null)
			if (isDependenyEmpty(cache)) {
				dependencyTable.remove(Tile);
				return;
			}
		// right
		Tile = new Tile(tile.x + 1, tile.y, tile.zoomLevel);
		cache = dependencyTable.get(Tile);
		if (cache != null)
			if (isDependenyEmpty(cache)) {
				dependencyTable.remove(Tile);
				return;
			}
		// leftup
		Tile = new Tile(tile.x - 1, tile.y - 1, tile.zoomLevel);
		cache = dependencyTable.get(Tile);
		if (cache != null)
			if (isDependenyEmpty(cache)) {
				dependencyTable.remove(Tile);
				return;
			}
		// leftdown
		Tile = new Tile(tile.x - 1, tile.y + 1, tile.zoomLevel);
		cache = dependencyTable.get(Tile);
		if (cache != null)
			if (isDependenyEmpty(cache)) {
				dependencyTable.remove(Tile);
				return;
			}
		// righttup
		Tile = new Tile(tile.x + 1, tile.y - 1, tile.zoomLevel);
		cache = dependencyTable.get(Tile);
		if (cache != null)
			if (isDependenyEmpty(cache)) {
				dependencyTable.remove(Tile);
				return;
			}
		// rightdown
		Tile = new Tile(tile.x + 1, tile.y + 1, tile.zoomLevel);
		cache = dependencyTable.get(Tile);
		if (cache != null)
			if (isDependenyEmpty(cache)) {
				dependencyTable.remove(Tile);
				return;
			}
	}

	private boolean isDependenyEmpty(DependencyCache cache) {

		if (cache.labels == null)
			if (cache.symbols == null) {
				return true;
			} else if (cache.symbols.size() == 0) {
				return true;
			}

		if (cache.labels.size() == 0)
			if (cache.symbols == null) {
				return true;
			} else if (cache.symbols.size() == 0) {
				return true;
			}

		return false;
	}

	/* getter and setter methods */

	public int getPlacementOption() {
		return placementOption;
	}

	public int getStartDistanceToSymbols() {
		return startDistanceToSymbols;
	}

	public void setStartDistanceToSymbols(int startDistanceToSymbols) {
		this.startDistanceToSymbols = startDistanceToSymbols;
	}

	public int getSymbolDistanceToSymbol() {
		return symbolDistanceToSymbol;
	}

	public void setSymbolDistanceToSymbol(int symbolDistanceToSymbol) {
		this.symbolDistanceToSymbol = symbolDistanceToSymbol;
	}

	public int getLabelDistanceToSymbol() {
		return labelDistanceToSymbol;
	}

	public void setLabelDistanceToSymbol(int labelDistanceToSymbol) {
		this.labelDistanceToSymbol = labelDistanceToSymbol;
	}

	public int getLabelDistanceToLabel() {
		return labelDistanceToLabel;
	}

	public void setLabelDistanceToLabel(int labelDistanceToLabel) {
		this.labelDistanceToLabel = labelDistanceToLabel;
	}

}
