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

import android.content.Context;
import android.graphics.Bitmap;

/**
 * A map renderer which uses a OpenGL for drawing.
 */
class OpenGlMapGenerator extends DatabaseMapGenerator {
	private static final String THREAD_NAME = "OpenGlMapGenerator";

	OpenGlMapGenerator(Context context) {
	}

	@Override
	void drawMapSymbols(ArrayList<SymbolContainer> drawSymbols) {
		// TODO Auto-generated method stub
	}

	@Override
	void drawNodes(ArrayList<PointContainer> drawNodes) {
		// TODO Auto-generated method stub
	}

	@Override
	void drawTileFrame() {
		// TODO Auto-generated method stub
	}

	@Override
	void drawWayNames(ArrayList<PathTextContainer> drawWayNames) {
		// TODO Auto-generated method stub
	}

	@Override
	void drawWays(ArrayList<ArrayList<ArrayList<PathContainer>>> drawWays, byte layers,
			byte levelsPerLayer) {
		// TODO Auto-generated method stub
	}

	@Override
	void finishMapGeneration() {
		// TODO Auto-generated method stub
	}

	@Override
	String getThreadName() {
		return THREAD_NAME;
	}

	@Override
	void setupRenderer(Bitmap bitmap) {
		// TODO Auto-generated method stub
	}
}