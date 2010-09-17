package org.mapsforge.android.map;

public class Rectangle<E> {
	public E value;
	android.graphics.Rect rect;

	public Rectangle(E value, android.graphics.Rect rect) {
		this.value = value;
		this.rect = rect;

	}

	public Rectangle() {

	}

}