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


class Rect implements SpatialShape<Rect> {
	static final int memory_size = 4 * 4; // 4 int values

	int top;
	int left;
	int bottom;
	int right;

	public Rect() {
		// required by perst
	}

	public Rect(Rect r) {
		this.top = r.top;
		this.left = r.left;
		this.bottom = r.bottom;
		this.right = r.right;
	}

	public Rect(int top, int left, int bottom, int right) {
		super();
		this.top = top;
		this.left = left;
		this.bottom = bottom;
		this.right = right;
	}

	@Override
	public long area() {
		return (long) (bottom - top) * (right - left);
	}

	@Override
	public boolean contains(Rect shape) {
		return left <= shape.left && top <= shape.top && right >= shape.right
				&& bottom >= shape.bottom;
	}

	@Override
	public boolean intersects(Rect shape) {
		return left <= shape.right && top <= shape.bottom && right >= shape.left
				&& bottom >= shape.top;
	}

	@Override
	public Rect join(Rect shape) {
		if (left > shape.left) {
			left = shape.left;
		}
		if (right < shape.right) {
			right = shape.right;
		}
		if (top > shape.top) {
			top = shape.top;
		}
		if (bottom < shape.bottom) {
			bottom = shape.bottom;
		}
		return this;
	}

	public Point center() {
		return new Point((left + right) / 2, (top + bottom) / 2);
	}

	@Override
	public long joinArea(Rect shape) {
		int l = (this.left < shape.left) ? this.left : shape.left;
		int r = (this.right > shape.right) ? this.right : shape.right;
		int t = (this.top < shape.top) ? this.top : shape.top;
		int b = (this.bottom > shape.bottom) ? this.bottom : shape.bottom;
		return (long) (b - t) * (r - l);
	}

	/**
	 * Hash code consists of all rectangle coordinates
	 */
	@Override
	public int hashCode() {
		return top ^ (bottom << 1) ^ (left << 2) ^ (right << 3);
	}

	@Override
	public String toString() {
		return "top=" + top + ", left=" + left + ", bottom=" + bottom + ", right=" + right;
	}

	@Override
	public long linearOderValue() {
		Point center = center();
		return Hilbert.computeValue(center.y, center.x);
	}

}
