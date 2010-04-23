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

import java.util.Random;

/**
 * @author Frank Viernau
 * 
 *         This implementation is similar to an implicit 2-dimensional KD-tree. Requieres no
 *         extra data for storage, so given points have to be reordered.
 * 
 *         The Worst case might be much more worse compared to a KD Tree since the partitioning
 *         always appears in the center of the array subsection. If there are many points having
 *         the same value as the median (which is chosen for the split axis) this might result
 *         in bad worst case running times. (linear time)
 * 
 *         Nevertheless Tests with 10.000.000 Points (each coordinate in the range from (0 ..
 *         1.000.000) and 1.000.000 Random Queries yield an average query time of 2.58ns.
 */
public class Implicit2DTree {

	private static final int START_DIM = 0;

	private final int[][] points;
	private final Random rnd;

	public Implicit2DTree(int[] x, int[] y, int[] buff) {
		points = new int[][] { x, y };
		rnd = new Random();
		construct(0, x.length - 1, START_DIM, buff);
	}

	public int nearestNeighbor(int x, int y) {
		return nearestNeighbor(new int[] { x, y }, 0, points[0].length - 1, START_DIM, null);
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
		long dBest = quadraticDistance(point[0], point[1], points[0][best], points[1][best]);
		long dCurrent = quadraticDistance(point[0], point[1], points[0][c], points[1][c]);

		if (dCurrent < dBest) {
			best = c;
		}
		// search the containing branch
		if (point[dim] < points[dim][c]) {
			best = nearestNeighbor(point, p, c - 1, (dim + 1) % 2, best);
		} else if (point[dim] > points[dim][c]) {
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
		dBest = quadraticDistance(point[0], point[1], points[0][best], points[1][best]);
		// distance to split axis
		long dAxis = quadraticDistance(points[dim][best], 0, points[dim][c], 0);

		if (dAxis < dBest) {
			if (point[dim] > points[dim][c]) {
				best = nearestNeighbor(point, p, c - 1, (dim + 1) % 2, best);
			} else if (point[dim] < points[dim][c]) {
				best = nearestNeighbor(point, c + 1, r, (dim + 1) % 2, best);
			}
		}
		return best;
	}

	private void construct(int p, int r, int dim, int[] buff) {
		if (p < r) {
			quicksort(p, r, dim, buff);
			int c = (p + r) / 2;
			construct(p, c - 1, (dim + 1) % 2, buff);
			construct(c + 1, r, (dim + 1) % 2, buff);
		}
	}

	private void quicksort(int p, int r, int dim, int[] buff) {
		if (p < r) {
			int q = partition(p, r, dim, buff);
			quicksort(p, q - 1, dim, buff);
			quicksort(q + 1, r, dim, buff);
		}
	}

	private int partition(int p, int r, int dim, int[] buff) {
		swap(r, rnd.nextInt(r - p + 1) + p, buff);
		int pivot = points[dim][r];
		int i = p - 1;
		for (int j = p; j < r; j++) {
			if (points[dim][j] <= pivot) {
				i++;
				swap(i, j, buff);
			}
		}
		swap(i + 1, r, buff);
		return i + 1;
	}

	private void swap(int i, int j, int[] buff) {
		int tmpX = points[0][i];
		points[0][i] = points[0][j];
		points[0][j] = tmpX;
		int tmpY = points[1][i];
		points[1][i] = points[1][j];
		points[1][j] = tmpY;
		int tmpB = buff[i];
		buff[i] = buff[j];
		buff[j] = tmpB;
	}
}
