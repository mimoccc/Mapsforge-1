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
package org.mapsforge.android.maps;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.PriorityQueue;

class SweepLineRect {

	private class Event {
		Rectangle<?> rectangle;
		int time;

		Event(int time, Rectangle<?> rectangle) {
			this.time = time;
			this.rectangle = rectangle;
		}

	}

	class EventComparator implements Comparator<Event> {
		@Override
		public int compare(Event x, Event y) {

			if (x.time < y.time)
				return -1;
			if (x.time > y.time)
				return 1;

			return 0;

		}

	}

	class Intersection {

		Rectangle<?> a;
		Rectangle<?> b;

		Intersection(Rectangle<?> a, Rectangle<?> b) {
			this.a = a;
			this.b = b;
		}
	}

	LinkedList<Intersection> intersects = new LinkedList<Intersection>();

	PriorityQueue<Event> pq;
	IntervalTree set = new IntervalTree();

	LinkedList<Intersection> processSweep() {
		while (this.pq.size() != 0) {
			Event e = this.pq.remove();
			float sweep = e.time;
			Rectangle<?> rectangle = e.rectangle;
			if (sweep == rectangle.rect.right) {
				this.set
						.remove(new Interval<Object>(rectangle.rect.top, rectangle.rect.bottom));

			} else {
				for (Interval<?> inters : this.set.searchAll(new Interval<Object>(
						rectangle.rect.top, rectangle.rect.bottom))) {
					intersects.add(new Intersection(rectangle, (Rectangle<?>) inters.value));
				}
				this.set.put((new Interval<Object>(rectangle.rect.top, rectangle.rect.bottom,
						rectangle)));
			}
		}
		return intersects;
	}

	LinkedList<Intersection> sweep(ArrayList<Rectangle<?>> rectangles) {
		this.pq = new PriorityQueue<Event>(rectangles.size() + rectangles.size() / 100 * 20,
				new EventComparator());
		for (Rectangle<?> rectangle : rectangles) {
			Event e1 = new Event(rectangle.rect.left, rectangle);
			Event e2 = new Event(rectangle.rect.right, rectangle);
			this.pq.add(e1);
			this.pq.add(e2);
		}

		return processSweep();
	}
}
