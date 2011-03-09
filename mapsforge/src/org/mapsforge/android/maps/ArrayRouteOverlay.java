/*
 * Copyright 2010, 2011 mapsforge.org
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
import java.util.Collection;

import android.graphics.Paint;

/**
 * ArrayRouteOverlay is a thread-safe implementation of the {@link RouteOverlay} class using an
 * {@link ArrayList} as internal data structure. Default paints for all {@link OverlayRoute
 * OverlayRoutes} without individual paints can be defined via the constructor.
 */
public class ArrayRouteOverlay extends RouteOverlay<OverlayRoute> {
	private static final int ARRAY_LIST_INITIAL_CAPACITY = 8;
	private static final String THREAD_NAME = "ArrayRouteOverlay";

	private final ArrayList<OverlayRoute> overlayRoutes;

	/**
	 * Constructs a new ArrayRouteOverlay.
	 * 
	 * @param defaultPaintFill
	 *            the default paint which will be used to fill the routes (may be null).
	 * @param defaultPaintOutline
	 *            the default paint which will be used to draw the route outlines (may be null).
	 */
	public ArrayRouteOverlay(Paint defaultPaintFill, Paint defaultPaintOutline) {
		super(defaultPaintFill, defaultPaintOutline);
		this.overlayRoutes = new ArrayList<OverlayRoute>(ARRAY_LIST_INITIAL_CAPACITY);
	}

	/**
	 * Adds the given route to the overlay.
	 * 
	 * @param overlayRoute
	 *            the route that should be added to the overlay.
	 */
	public void addRoute(OverlayRoute overlayRoute) {
		synchronized (this.overlayRoutes) {
			this.overlayRoutes.add(overlayRoute);
		}
		populate();
	}

	/**
	 * Adds all routes of the given collection to the overlay.
	 * 
	 * @param c
	 *            collection whose routes should be added to the overlay.
	 */
	public void addRoutes(Collection<? extends OverlayRoute> c) {
		synchronized (this.overlayRoutes) {
			this.overlayRoutes.addAll(c);
		}
		populate();
	}

	/**
	 * Removes all routes from the overlay.
	 */
	public void clear() {
		synchronized (this.overlayRoutes) {
			this.overlayRoutes.clear();
		}
		populate();
	}

	@Override
	public String getThreadName() {
		return THREAD_NAME;
	}

	/**
	 * Removes the given route from the overlay.
	 * 
	 * @param overlayRoute
	 *            the route that should be removed from the overlay.
	 */
	public void removeOverlay(OverlayRoute overlayRoute) {
		synchronized (this.overlayRoutes) {
			this.overlayRoutes.remove(overlayRoute);
		}
		populate();
	}

	@Override
	public int size() {
		synchronized (this.overlayRoutes) {
			return this.overlayRoutes.size();
		}
	}

	@Override
	protected OverlayRoute createRoute(int i) {
		synchronized (this.overlayRoutes) {
			return this.overlayRoutes.get(i);
		}
	}
}