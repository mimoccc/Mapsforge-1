package org.mapsforge.android.map;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.PriorityQueue;

import org.mapsforge.android.map.intervaltree.Interval;
import org.mapsforge.android.map.intervaltree.IntervalTree;

@SuppressWarnings("unchecked")
public class SweepLineRect {

	LinkedList<Intersection> intersects = new LinkedList<Intersection>();

	public class Intersection {

		Rectangle a;
		Rectangle b;

		public Intersection() {
		}

		public Intersection(Rectangle a, Rectangle b) {
			this.a = a;
			this.b = b;
		}
	}

	private class Event {
		private int time;
		private Rectangle rectangle;

		public Event(int time, Rectangle rectangle) {
			this.time = time;
			this.rectangle = rectangle;
		}

	}

	private class EventComparator implements Comparator<Event> {
		@Override
		public int compare(Event x, Event y) {

			if (x.time < y.time)
				return -1;
			if (x.time > y.time)
				return 1;

			return 0;

		}

	}

	IntervalTree set = new IntervalTree();
	PriorityQueue<Event> pq;

	public SweepLineRect() {

	}

	public LinkedList<Intersection> sweep(ArrayList<Rectangle> rectangles) {
		pq = new PriorityQueue<Event>(rectangles.size() + rectangles.size() / 100 * 20,
				new EventComparator());
		for (Rectangle rectangle : rectangles) {
			Event e1 = new Event(rectangle.rect.left, rectangle);
			Event e2 = new Event(rectangle.rect.right, rectangle);
			pq.add(e1);
			pq.add(e2);
		}

		return processSweep();
	}

	public LinkedList<Intersection> processSweep() {
		while (pq.size() != 0) {
			Event e = pq.remove();
			float sweep = e.time;
			Rectangle rectangle = e.rectangle;
			if (sweep == rectangle.rect.right) {
				set.remove(new Interval(rectangle.rect.top, rectangle.rect.bottom));

			} else {
				for (Interval inters : set.searchAll(new Interval(rectangle.rect.top,
						rectangle.rect.bottom))) {
					intersects.add(new Intersection(rectangle, (Rectangle) inters.value));
				}
				set.put((new Interval(rectangle.rect.top, rectangle.rect.bottom, rectangle)));
			}
		}
		return intersects;
	}
}
