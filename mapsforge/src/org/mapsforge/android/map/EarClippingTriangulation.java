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

public class EarClippingTriangulation {

	/* x coordinates of the input polygon */
	private float[] xCoords;
	/* y coordinates of the input polygon */
	private float[] yCoords;
	/* contain the triangle points after triangulation */
	private ArrayList<Point> trianglePoints;
	/* current number of polygon vertices */
	private int num;

	EarClippingTriangulation(float[] polyCoords) {

		boolean clockwise = CoastlineWay.isClockWise(polyCoords);
		num = polyCoords.length / 2;
		int i;

		/* closed polygon? skip duplicate coordinate */
		if ((polyCoords[0] == polyCoords[polyCoords.length - 2])
				&& (polyCoords[1] == polyCoords[polyCoords.length - 1])) {
			num--;
		}

		if (clockwise) {
			System.out.println("is clockwise");
		} else {
			System.out.println("is anti-clockwise");
		}

		xCoords = new float[num];
		yCoords = new float[num];
		trianglePoints = new ArrayList<Point>((num - 2) * 3); // any polygon triangulation has
		// n-2 triangles

		for (i = 0; i < num; i++) {
			if (clockwise) {
				// split into x and y coords
				xCoords[i] = polyCoords[i * 2];
				yCoords[i] = polyCoords[i * 2 + 1];
			} else {
				// anti-clockwise order
				// split into x and y coords in reverse order
				xCoords[num - 1 - i] = polyCoords[i * 2];
				yCoords[num - 1 - i] = polyCoords[i * 2 + 1];
			}
		}
		doTriangulation();
	}

	ArrayList<Point> getTriangles() {
		return trianglePoints;
	}

	float[] getTrianglesAsFloatArray() {
		int s = trianglePoints.size();
		float[] coords = new float[s * 2];

		for (int i = 0; i < s; i++) {
			Point p = trianglePoints.get(i);
			coords[i * 2] = p.x;
			coords[i * 2 + 1] = p.y;
		}
		return coords;
	}

	private void clipEarAtPosition(int p) {

		// System.out.println("clipping ear at position: " + p + " number of polygon vertices: "
		// + num);

		if (p == -1) {
			for (int x = 0; x < num; x++) {
				// Logger.d(xCoords[x] + " " + yCoords[x]);
				// if (((x + 1) % 2) == 0)
				// Logger.d("");
			}
		}

		/*
		 * add the new triangle to the list
		 */
		if ((p > 0) && (p < num - 1)) {
			trianglePoints.add(new Point(xCoords[p - 1], yCoords[p - 1]));
			trianglePoints.add(new Point(xCoords[p], yCoords[p]));
			trianglePoints.add(new Point(xCoords[p + 1], yCoords[p + 1]));
		} else if (0 == p) {
			trianglePoints.add(new Point(xCoords[num - 1], yCoords[num - 1]));
			trianglePoints.add(new Point(xCoords[0], yCoords[0]));
			trianglePoints.add(new Point(xCoords[1], yCoords[1]));
		} else if (num - 1 == p) {
			trianglePoints.add(new Point(xCoords[num - 2], yCoords[num - 2]));
			trianglePoints.add(new Point(xCoords[num - 1], yCoords[num - 1]));
			trianglePoints.add(new Point(xCoords[0], yCoords[0]));
		}

		/* remove point from x and y coordinate arrays */
		for (int i = p; i < num - 1; i++) {
			xCoords[i] = xCoords[i + 1];
			yCoords[i] = yCoords[i + 1];
		}
		/* adjust number of points left in the polygon */

		num--;
	}

	/*
	 * isConvex: return true if triangle (x1,y1) (x1,y2) (x3,y3) is convex
	 */
	private boolean isConvex(float x1, float y1, float x2, float y2, float x3, float y3) {
		/*
		 * triangle area = 0.5 * (x1 * (y2 - y3) + x2 * (y3 - y1) + x3 * (y1 - y2)) is negative
		 * for convex and positive for concave triangle
		 */

		if ((x1 * (y3 - y2) + x2 * (y1 - y3) + x3 * (y2 - y1)) < 0) {
			return true;
		}
		return false;
	}

	/*
	 * isConvexPoint: return true if polygon is convex at point p
	 */
	private boolean isConvexPoint(int p) {
		if (p == 0) {
			return isConvex(xCoords[num - 1], yCoords[num - 1], xCoords[0], yCoords[0],
					xCoords[1], yCoords[1]);
		} else if (p == num - 1) {
			return isConvex(xCoords[num - 2], yCoords[num - 2], xCoords[num - 1],
					yCoords[num - 1], xCoords[0], yCoords[0]);
		}
		return isConvex(xCoords[p - 1], yCoords[p - 1], xCoords[p], yCoords[p], xCoords[p + 1],
				yCoords[p + 1]);
	}

	/*
	 * return true if the triangle (x1,y1) (x1,y2) (x3,y3) is an ear, false otherwise
	 */
	private boolean isEar(float x1, float y1, float x2, float y2, float x3, float y3) {

		// make sure the triangle is convex
		if (!isConvex(x1, y1, x2, y2, x3, y3)) {
			// System.out.println("not convex at " + x1 + "," + y1 + " " + x2 + "," + y2 + " "
			// + x3 + "," + y3);
			return false;
		}

		// if it contains no point, it's an ear
		return !pointInsideTriangle(x1, y1, x2, y2, x3, y3);
	}

	/*
	 * return true if there is an ear at point p
	 */
	private boolean earAtPoint(int p) {
		// System.out.println("check for ear at point " + p);
		if (p == 0) {
			return isEar(xCoords[num - 1], yCoords[num - 1], xCoords[0], yCoords[0],
					xCoords[1], yCoords[1]);
		} else if (p == num - 1) {
			return isEar(xCoords[num - 2], yCoords[num - 2], xCoords[num - 1],
					yCoords[num - 1], xCoords[0], yCoords[0]);
		}

		return isEar(xCoords[p - 1], yCoords[p - 1], xCoords[p], yCoords[p], xCoords[p + 1],
				yCoords[p + 1]);
	}

	/*
	 * test if any point of the polygon lies inside the triangle (x1,y1) (x1,y2) (x3,y3)
	 */
	private boolean pointInsideTriangle(float x1, float y1, float x2, float y2, float x3,
			float y3) {

		for (int i = 0; i < num; i++) {
			if ((!isConvexPoint(i)) /* point is concave */
					&& (((xCoords[i] != x1) && (yCoords[i] != y1))
							|| ((xCoords[i] != x2) && (yCoords[i] != y2)) || ((xCoords[i] != x3) && (yCoords[i] != y3)))) {

				boolean convex1 = isConvex(x1, y1, x2, y2, xCoords[i], yCoords[i]);
				boolean convex2 = isConvex(x2, y2, x3, y3, xCoords[i], yCoords[i]);
				boolean convex3 = isConvex(x3, y3, x1, y1, xCoords[i], yCoords[i]);

				if ((!convex1 && !convex2 && !convex3) || (convex1 && convex2 && convex3)) {
					return true;
				}
			}
		}
		return false;
	}

	private void doTriangulation() {

		int pos;

		while (num > 3) {
			// as long as there are more than 2 points (at least 1 triangle)

			pos = 0;
			// find position to clip
			// TODO: for negative coordinates this does not always find an ear (convex test
			// wrong)
			for (int i = 0; i < num; i++) {
				// find position to clip an ear
				if (earAtPoint(i)) {
					pos = i;
					break;
				}
			}
			clipEarAtPosition(pos);
		}
		// if 3 points are left, clip this last triangle anywhere
		if (num == 3) {
			clipEarAtPosition(0);
		}
	}

	public static void main(String[] args) {
		// just a test

		// square clockwise
		// float[] poly = new float[] { 1.0f, 1.0f, 5.0f, 1.0f, 5.0f, 5.0f, }; //
		// square anticlockwise
		// float[] poly = new float[] { 5.0f, 1.0f, 1.0f, 1.0f, 1.0f, 5.0f, 5.0f, 5.0f };
		// float[] poly = new float[] { 1.9f, 1f, 4f, 1.6f, 3.63f, 2.52f, 5f, 3.5f, 2.66f,
		// 4.71f,
		// 0.72f, 2.28f };
		float[] poly = new float[] { 0.72f, 2.28f, 2.66f, 4.71f, 5f, 3.5f, 3.63f, 2.52f, 4f,
				1.6f, 1.9f, 1f };

		EarClippingTriangulation ec = new EarClippingTriangulation(poly);

		System.out.println("triangulation: \n");
		float[] f = ec.getTrianglesAsFloatArray();
		for (int i = 0; i < ec.getTrianglesAsFloatArray().length; i++) {
			System.out.print(f[i] + " ");
			if (((i + 1) % 2) == 0)
				System.out.println();
			if (((i + 1) % 6) == 0)
				System.out.println();
		}
	}
}
