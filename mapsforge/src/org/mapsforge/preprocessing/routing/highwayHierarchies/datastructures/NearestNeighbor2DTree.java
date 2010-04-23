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
package org.mapsforge.preprocessing.routing.highwayHierarchies.datastructures;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.Random;

import org.mapsforge.preprocessing.routing.highwayHierarchies.util.geo.CarthesianPoint;

/**
 * @author Frank Viernau viernau[at]mi.fu-berlin.de
 * 
 *         Implicit Tree similar to KD-Tree with additional Level of indirection, which allows
 *         for keeping memory locations of all coordinates (no reordering). It consumes 4 byte
 *         overhead per coordinate an needs about twice the time compared to a similar
 *         implementation without indirection level.
 * 
 *         Average nearest neighbor query time depends on distribution of coordinates, If there
 *         are many Coordinates equal to the median used for splitting on the respective
 *         dimension, query times will slow down since both branches are searched recursively.
 *         This is the main difference to a standard KD-Tree, for indexing geoCoordinates this
 *         might be acceptable. 4,7ns on average on 10.000.000 Random test coordinate
 */
public class NearestNeighbor2DTree implements Serializable {

	private static final long serialVersionUID = 1L;
	private static final int START_DIM = 0;

	private final int[][] points;
	private final int[] ind;
	private final Random rnd;
	private final CarthesianPoint coordMin, coordMax;

	public NearestNeighbor2DTree(int[] x, int[] y) {
		points = new int[][] { x, y };
		rnd = new Random();
		ind = new int[x.length];
		for (int i = 0; i < ind.length; i++) {
			ind[i] = i;
		}
		construct(0, x.length - 1, START_DIM);

		// compute bounding rectangle
		int _minX = Integer.MAX_VALUE;
		int _maxX = Integer.MIN_VALUE;
		int _minY = Integer.MAX_VALUE;
		int _maxY = Integer.MIN_VALUE;

		for (int i = 0; i < points[0].length; i++) {
			_minX = Math.min(_minX, points[0][i]);
			_maxX = Math.max(_maxX, points[0][i]);
			_minY = Math.min(_minY, points[1][i]);
			_maxY = Math.max(_maxY, points[1][i]);
		}
		this.coordMin = new CarthesianPoint(_minX, _minY);
		this.coordMax = new CarthesianPoint(_maxX, _maxY);
	}

	public CarthesianPoint maxCoordinate() {
		return coordMax;
	}

	public CarthesianPoint minCoordinate() {
		return coordMin;
	}

	public int nearestNeighborIdx(CarthesianPoint c) {
		return ind[nearestNeighbor(new int[] { c.x, c.y }, 0, points[0].length - 1, START_DIM,
				null)];
	}

	public CarthesianPoint getPoint(int idx) {
		return new CarthesianPoint(points[0][idx], points[1][idx]);
	}

	public LinkedList<Integer> getPointsWithinRectangle(CarthesianPoint cMin,
			CarthesianPoint cMax) {
		LinkedList<Integer> buff = new LinkedList<Integer>();
		getPointsWithinRectangle(new int[] { cMin.x, cMin.y }, new int[] { cMax.x, cMax.y }, 0,
				points.length - 1, START_DIM, buff);
		return buff;
	}

	private void getPointsWithinRectangle(int[] minPoint, int[] maxPoint, int p, int r,
			int dim, LinkedList<Integer> buff) {
		if (p > r) {
			return;
		}
		int c = (p + r) / 2;
		if (points[0][ind[c]] >= minPoint[0] && points[0][ind[c]] <= maxPoint[0]
				&& points[1][ind[c]] >= minPoint[1] && points[1][ind[c]] <= maxPoint[1]) {
			buff.add(ind[c]);
		}
		if (points[dim][ind[c]] >= minPoint[dim]) {
			getPointsWithinRectangle(minPoint, maxPoint, p, c - 1, (dim + 1) % 2, buff);
		}
		if (points[dim][ind[c]] <= maxPoint[dim]) {
			getPointsWithinRectangle(minPoint, maxPoint, c + 1, r, (dim + 1) % 2, buff);
		}
	}

	public int size() {
		return ind.length;
	}

	private long quadraticDistance(int x1, int y1, int x2, int y2) {
		return ((((long) x2) - ((long) x1)) * (((long) x2) - ((long) x1)))
				+ ((((long) y2) - ((long) y1)) * (((long) y2) - ((long) y1)));
	}

	private int nearestNeighbor(int[] point, int p, int r, int dim, Integer best) {
		if (p > r) {
			return best;
		}
		int c = (p + r) / 2;
		if (best == null) {
			best = c;
		}
		// current node
		// distance to best
		long dBest = quadraticDistance(point[0], point[1], points[0][ind[best]],
				points[1][ind[best]]);
		long dCurrent = quadraticDistance(point[0], point[1], points[0][ind[c]],
				points[1][ind[c]]);

		if (dCurrent < dBest) {
			best = c;
		}
		// search the containing branch
		if (point[dim] < points[dim][ind[c]]) {
			best = nearestNeighbor(point, p, c - 1, (dim + 1) % 2, best);
		} else if (point[dim] > points[dim][ind[c]]) {
			best = nearestNeighbor(point, c + 1, r, (dim + 1) % 2, best);
		} else {
			int best1 = nearestNeighbor(point, p, c - 1, (dim + 1) % 2, best);
			int best2 = nearestNeighbor(point, c + 1, r, (dim + 1) % 2, best);
			if (best1 < best2) {
				best = best1;
			} else {
				best = best2;
			}
		}
		// search the other branch
		// best distance
		dBest = quadraticDistance(point[0], point[1], points[0][ind[best]],
				points[1][ind[best]]);
		// distance to split axis
		long dAxis = quadraticDistance(points[dim][ind[best]], 0, points[dim][ind[c]], 0);

		if (dAxis < dBest) {
			if (point[dim] > points[dim][ind[c]]) {
				best = nearestNeighbor(point, p, c - 1, (dim + 1) % 2, best);
			} else if (point[dim] < points[dim][ind[c]]) {
				best = nearestNeighbor(point, c + 1, r, (dim + 1) % 2, best);
			}
		}
		return best;
	}

	private void construct(int p, int r, int dim) {
		if (p < r) {
			quicksort(p, r, dim);
			int c = (p + r) / 2;
			construct(p, c - 1, (dim + 1) % 2);
			construct(c + 1, r, (dim + 1) % 2);
		}
	}

	private void quicksort(int p, int r, int dim) {
		if (p < r) {
			int q = partition(p, r, dim);
			quicksort(p, q - 1, dim);
			quicksort(q + 1, r, dim);
		}
	}

	private int partition(int p, int r, int dim) {
		swap(r, rnd.nextInt(r - p + 1) + p);
		int pivot = points[dim][ind[r]];
		int i = p - 1;
		for (int j = p; j < r; j++) {
			if (points[dim][ind[j]] <= pivot) {
				i++;
				swap(i, j);
			}
		}
		swap(i + 1, r);
		return i + 1;
	}

	private void swap(int i, int j) {
		int tmp = ind[i];
		ind[i] = ind[j];
		ind[j] = tmp;
	}

	public static void main(String[] args) {
		int len = 100000;
		int[] x = new int[len];
		int[] y = new int[len];
		Random rnd = new Random(271123);
		System.out.println("build tree");
		for (int i = 0; i < x.length; i++) {
			x[i] = rnd.nextInt(len / 10);
			y[i] = rnd.nextInt(len / 10);
		}
		NearestNeighbor2DTree tree = new NearestNeighbor2DTree(x, y);

		System.out.println("test nn");
		long startTime = System.currentTimeMillis();
		for (int i = 0; i < 1000; i++) {
			// System.out.println("[" + i + "] " + x[i] + " " + y[i] + "        " +
			// (tree.find(x[i], y[i]) == null));
			int idx = rnd.nextInt(x.length);
			int nn = tree.nearestNeighborIdx(tree.getPoint(idx));
			System.out.println(tree.getPoint(idx) + " " + tree.getPoint(nn));
		}
		long time = System.currentTimeMillis() - startTime;
		System.out.println(time + "ms");

	}

}
