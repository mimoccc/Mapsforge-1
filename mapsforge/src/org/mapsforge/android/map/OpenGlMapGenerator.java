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

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLSurfaceView;
import android.view.View;
import android.view.ViewGroup;

/**
 * A map renderer which uses a OpenGL for drawing.
 */
class OpenGlMapGenerator extends DatabaseMapGenerator {
	private static final String THREAD_NAME = "OpenGlMapGenerator";
	private MapView mMapView;
	private GLSurfaceView mGLSurfaceView;
	private OpenGlMapRenderer mRenderer;
	private Bitmap mBitmap;
	private Context mContext;

	OpenGlMapGenerator(Context context, MapView mapView) {
		this.mContext = context;
		this.mMapView = mapView;

		this.mGLSurfaceView = new GLSurfaceView(context);
		this.mRenderer = new OpenGlMapRenderer();

		mGLSurfaceView.setRenderer(mRenderer);
		mGLSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
		mGLSurfaceView.setDebugFlags(GLSurfaceView.DEBUG_CHECK_GL_ERROR
				| GLSurfaceView.DEBUG_LOG_GL_CALLS);
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
		while (mGLSurfaceView.getWidth() <= 0) {
			try {
				Logger.d("'waiting for width > 0 -- " + mGLSurfaceView.getWidth());
				sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		Logger.d("width: " + mGLSurfaceView.getWidth() + " height: "
				+ mGLSurfaceView.getHeight());

		this.mGLSurfaceView.requestRender();

		// wait for frame
		while (!mRenderer.frameReady) {
			try {
				sleep(10);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		// Logger.d("copying frame");
		this.mBitmap = mRenderer.mBitmap;
		this.mRenderer.frameReady = false;
	}

	@Override
	String getThreadName() {
		return THREAD_NAME;
	}

	@Override
	void setupRenderer(Bitmap bitmap) {
		this.mBitmap = bitmap;
		this.mRenderer.setBitmap(bitmap);

	}

	@Override
	void mapViewHasParent() {
		final ViewGroup viewGroup = (ViewGroup) mMapView.getParent();
		Logger.d("has parent");
		if (viewGroup == null) {
			Logger.d("  ViewGroup is null");
			return;
		}

		Activity mActivity = (Activity) this.mContext;
		mActivity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				viewGroup.addView(mGLSurfaceView, 256, 256);
				// viewGroup.bringChildToFront(mGLSurfaceView);
				mGLSurfaceView.setVisibility(View.VISIBLE);
				// mGLSurfaceView.requestLayout();
				// mGLSurfaceView.invalidate();
				// mGLSurfaceView.requestFocus();
			}
		});
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
}