/*
 * Copyright 2010, 2011 mapsforge.org
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.mapsforge.android.maps.overlay;

import java.util.ArrayList;
import java.util.Collection;

import org.mapsforge.android.maps.MapView;

/**
 * An OverlayArrayList manages the overlay list of a MapView.
 */
public class OverlayArrayList extends ArrayList<Overlay> {
	private static final int INITIAL_CAPACITY = 2;
	private static final long serialVersionUID = 1L;

	private final MapView mapView;

	/**
	 * Constructs a new OverlayArrayList for the given MapView.
	 * 
	 * @param mapView
	 *            the MapView whose overlays are handled by this list instance.
	 */
	public OverlayArrayList(MapView mapView) {
		super(INITIAL_CAPACITY);
		this.mapView = mapView;
	}

	@Override
	public void add(int index, Overlay overlay) {
		if (!overlay.isAlive()) {
			overlay.start();
		}
		overlay.setupOverlay(this.mapView);
		super.add(index, overlay);
	}

	@Override
	public boolean add(Overlay overlay) {
		if (!overlay.isAlive()) {
			overlay.start();
		}
		overlay.setupOverlay(this.mapView);
		return super.add(overlay);
	}

	@Override
	public boolean addAll(Collection<? extends Overlay> collection) {
		for (Overlay overlay : collection) {
			if (!overlay.isAlive()) {
				overlay.start();
			}
			overlay.setupOverlay(this.mapView);
		}
		return super.addAll(collection);
	}

	@Override
	public boolean addAll(int index, Collection<? extends Overlay> collection) {
		for (Overlay overlay : collection) {
			if (!overlay.isAlive()) {
				overlay.start();
			}
			overlay.setupOverlay(this.mapView);
		}
		return super.addAll(index, collection);
	}

	@Override
	public void clear() {
		for (int i = size() - 1; i >= 0; --i) {
			get(i).interrupt();
		}
		super.clear();
		this.mapView.invalidateOnUiThread();
	}

	@Override
	public Overlay remove(int index) {
		Overlay removedElement = super.remove(index);
		removedElement.interrupt();
		this.mapView.invalidateOnUiThread();
		return removedElement;
	}

	@Override
	public boolean remove(Object object) {
		boolean listChanged = super.remove(object);
		if (object instanceof Overlay) {
			((Overlay) object).interrupt();
		}
		this.mapView.invalidateOnUiThread();
		return listChanged;
	}

	@Override
	public boolean removeAll(Collection<?> collection) {
		boolean listChanged = super.removeAll(collection);
		for (Object object : collection) {
			if (object instanceof Overlay) {
				((Overlay) object).interrupt();
			}
		}
		this.mapView.invalidateOnUiThread();
		return listChanged;
	}

	@Override
	public Overlay set(int index, Overlay overlay) {
		if (!overlay.isAlive()) {
			overlay.start();
		}
		overlay.setupOverlay(this.mapView);
		Overlay previousElement = super.set(index, overlay);
		previousElement.interrupt();
		this.mapView.invalidateOnUiThread();
		return previousElement;
	}
}
