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
import android.opengl.GLSurfaceView;
import android.view.ViewGroup;

/**
 * A map renderer which uses a OpenGL for drawing.
 */
class OpenGlMapGenerator extends DatabaseMapGenerator {
	private static final String THREAD_NAME = "OpenGlMapGenerator";
	private Context context;
	private GLSurfaceView glSurfaceView;
	private MapView mapView;
	private OpenGlMapRenderer renderer;

	OpenGlMapGenerator(Context context, MapView mapView) {
		this.context = context;
		this.mapView = mapView;
	}

	@Override
	void drawMapSymbols(ArrayList<SymbolContainer> drawSymbols) {
		// TODO Auto-generated method stub
	}

	@Override
	void drawNodes(ArrayList<PointTextContainer> drawNodes) {
		// TODO Auto-generated method stub
	}

	@Override
	void drawTileFrame() {
		// TODO Auto-generated method stub
	}

	@Override
	void drawWayNames(ArrayList<WayTextContainer> drawWayNames) {
		// TODO Auto-generated method stub
	}

	@Override
	void drawWays(ArrayList<ArrayList<ArrayList<ShapePaintContainer>>> drawWays, byte layers,
			byte levelsPerLayer) {
		// TODO Auto-generated method stub
	}

	@Override
	void finishMapGeneration() {
		this.renderer.frameReady = false;
		this.glSurfaceView.requestRender();

		// wait for frame
		while (!this.renderer.frameReady) {
			try {
				sleep(20);
			} catch (InterruptedException e) {
				// restore the interrupted status
				interrupt();
			}
		}
	}

	@Override
	String getThreadName() {
		return THREAD_NAME;
	}

	@Override
	synchronized void onAttachedToWindow() {
		Logger.d("onAttachedToWindow called");
		this.renderer = new OpenGlMapRenderer();
		this.glSurfaceView = new GLSurfaceView(this.context);
		this.glSurfaceView.setRenderer(this.renderer);
		this.glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
		this.glSurfaceView.setDebugFlags(GLSurfaceView.DEBUG_CHECK_GL_ERROR
				| GLSurfaceView.DEBUG_LOG_GL_CALLS);

		((ViewGroup) this.mapView.getParent()).addView(this.glSurfaceView, 256, 256);
	}

	@Override
	synchronized void onDetachedFromWindow() {
		Logger.d("onDetachedFromWindow called");
		// TODO: remove the GLSurfaceView from the view hierarchy
		this.context = null;
		this.mapView = null;
	}

	//
	// @Override
	// void onPause() {
	// if (this.mGLSurfaceView != null) {
	// this.mGLSurfaceView.onPause();
	// }
	// }
	//
	// @Override
	// void onResume() {
	// if (this.mGLSurfaceView != null) {
	// this.mGLSurfaceView.onResume();
	// }
	// }

	@Override
	synchronized void setupMapGenerator(Bitmap bitmap) {
		Logger.d("setupMapGenerator called");
		this.renderer.setBitmap(bitmap);
	}
}