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

public class PerstQueryTracer {

	private static PerstQueryTracer instance;

	public static synchronized PerstQueryTracer getInstance() {
		if (instance == null) {
			instance = new PerstQueryTracer();
		}
		return instance;
	}

	private int pages = 0;
	private int nodes = 0;
	private long start = 0;
	private long stop = 0;

	public void incrementPages() {
		pages++;
	}

	public void incrementNodes() {
		nodes++;
	}

	public void start() {
		pages = 0;
		nodes = 0;
		stop = 0;
		start = System.currentTimeMillis();
	}

	public void stop() {
		stop = System.currentTimeMillis();
	}

	public int nodesTouched() {
		return nodes;
	}

	public int pagesLoaded() {
		return pages;
	}

	public int noneNodePagesLoaded() {
		return pages - nodes;
	}

	public long queryTime() {
		return stop - start;
	}
}
