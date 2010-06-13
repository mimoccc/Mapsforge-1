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

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Random;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.graphics.Bitmap;

class OpenGlMapRenderer implements android.opengl.GLSurfaceView.Renderer {
	private Bitmap bitmap;
	private GL10 mGL;
	private ByteBuffer pixelBuffer;
	private Random random;
	boolean frameReady;
	private int arrayListIndex;
	private ArrayList<ShapePaintContainer> wayList;
	private byte currentLayer;
	private byte currentLevel;
	private ShapePaintContainer shapePaintContainer;
	private ArrayList<ArrayList<ShapePaintContainer>> shapePaintContainers;

	OpenGlMapRenderer() {
		Logger.d("OpenGlMapRenderer called");
		this.random = new Random();
	}

	@Override
	public void onDrawFrame(GL10 gl) {
		Logger.d("onDrawFrame called");

		gl.glClearColor(this.random.nextFloat(), this.random.nextFloat(), this.random
				.nextFloat(), 1);
		gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
		gl.glLoadIdentity(); // Reset The Current Modelview Matrix

		// GL11 gl11 = (GL11) gl;
		//
		// gl11.glBindBuffer(GL11.GL_ARRAY_BUFFER, vboHandle);
		// gl11.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		// gl11.glEnableClientState(GL10.GL_COLOR_ARRAY);
		//
		// gl11.glVertexPointer(3, GL10.GL_FLOAT, 6 * 4, 0);
		// gl11.glColorPointer(3, GL10.GL_FLOAT, 6 * 4, 3 * 4);
		//
		// gl.glPointSize(8f);
		//
		// gl.glLineWidth(8f);
		//
		// gl11.glDrawArrays(GL10.GL_TRIANGLE_FAN, 0, 4);

		// int[] b = new int[numPixelBytes];
		// int[] bt = new int[numPixelBytes];
		// IntBuffer pixelBuffer = IntBuffer.wrap(b);

		// pixelBuffer.order(ByteOrder.nativeOrder());

		// if (bitmap == null) {
		// Logger.d("Bitmap is null");
		// }
		//
		// if (pixelBuffer == null) {
		// Logger.d("pixelBuffer is null");
		// }
		//
		// if (mGL == null) {
		// Logger.d("mGL is null");
		// }

		if (this.pixelBuffer != null) {
			this.mGL.glReadPixels(0, 0, this.bitmap.getWidth(), this.bitmap.getHeight(),
					GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, this.pixelBuffer);
			this.bitmap.copyPixelsFromBuffer(this.pixelBuffer);
			this.frameReady = true;
		}
	}

	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		Logger.d("onSurfaceChanged called, width: " + width + ", height: " + height);

		gl.glViewport(0, 0, width, height);

		gl.glMatrixMode(GL10.GL_PROJECTION);
		gl.glLoadIdentity();
		gl.glOrthof(0.0f, width, 0.0f, height, 0.0f, 1.0f);

		gl.glDisable(GL10.GL_DEPTH_TEST);
		gl.glEnable(GL10.GL_TEXTURE_2D); // needed for textured lines?
		gl.glEnable(GL10.GL_BLEND); // needed for textured lines?
		gl.glBlendFunc(GL10.GL_ONE, GL10.GL_SRC_COLOR); // needed for textured lines?
		gl.glDisable(GL10.GL_DITHER);
		gl.glDisable(GL10.GL_LIGHTING);
		gl.glEnable(GL10.GL_LINE_SMOOTH);
		gl.glEnable(GL10.GL_POINT_SMOOTH);
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		Logger.d("onSurfaceCreated called");

		// gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_FASTEST);
		// gl.glClearColor(0.5f, 0.5f, 0.5f, 1);
		//
		// ByteBuffer buffer = ByteBuffer.allocateDirect(4 * 6 * 4);
		// buffer.order(ByteOrder.nativeOrder());
		// FloatBuffer vertices = buffer.asFloatBuffer();
		// vertices.put(new float[] { -0.2f, -0.2f, 0, 1.0f, 0, 0, 0.2f, -0.2f, 0, 0, 1.0f, 0,
		// 0.0f, 0.2f, 0, 0, 0, 1.0f, -0.3f, 0.3f, 0, 0, 1.0f, 0, });
		// vertices.flip();
		//
		// // we need GL ES 1.1 for VBOs
		// GL11 gl11 = (GL11) gl;
		//
		// // generate VBOs
		// int[] handle = new int[1];
		// gl11.glGenBuffers(1, handle, 0); // find one unused buffer and save in handle[0]
		// vboHandle = handle[0];
		// gl11.glBindBuffer(GL11.GL_ARRAY_BUFFER, vboHandle); // bind the buffer
		// gl11.glBufferData(GL11.GL_ARRAY_BUFFER, 4 * 6 * 4, vertices, GL11.GL_STATIC_DRAW); //
		// transfer
		// // data
		// gl11.glBindBuffer(GL11.GL_ARRAY_BUFFER, 0); // unbind the buffer

		// from test

		gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_FASTEST);

		gl.glClearColor(0.5f, 0.0f, 0.5f, 1);

		// ByteBuffer buffer = ByteBuffer.allocateDirect(4 * 6 * 4);
		// buffer.order(ByteOrder.nativeOrder());
		// FloatBuffer vertices = buffer.asFloatBuffer();
		// vertices.put(new float[] { -0.2f, -0.2f, 0, 1.0f, 0, 0, 0.2f, -0.2f, 0, 0, 1.0f, 0,
		// 0.0f, 0.2f, 0, 0, 0, 1.0f, -0.3f, 0.3f, 0, 0, 1.0f, 0, });
		// vertices.flip();
		//
		// GL11 gl11 = (GL11) gl;

		// generate VBOs
		// int[] handle = new int[1];
		// gl11.glGenBuffers(1, handle, 0); // find one unused buffer and save in handle[0]
		// vboHandle = handle[0];
		// gl11.glBindBuffer(GL11.GL_ARRAY_BUFFER, vboHandle); // bind the buffer
		// gl11.glBufferData(GL11.GL_ARRAY_BUFFER, 4 * 6 * 4, vertices, GL11.GL_STATIC_DRAW); //
		// transfer
		// // data
		// gl11.glBindBuffer(GL11.GL_ARRAY_BUFFER, 0); // unbind the buffer

		this.mGL = gl;
	}

	void setBitmap(Bitmap bitmap) {
		this.bitmap = bitmap;
		this.pixelBuffer = ByteBuffer.allocateDirect(4 * this.bitmap.getHeight()
				* this.bitmap.getWidth());
	}

	public void drawWays(ArrayList<ArrayList<ArrayList<ShapePaintContainer>>> drawWays,
			byte layers, byte levelsPerLayer) {
		for (this.currentLayer = 0; this.currentLayer < layers; ++this.currentLayer) {
			this.shapePaintContainers = drawWays.get(this.currentLayer);
			for (this.currentLevel = 0; this.currentLevel < levelsPerLayer; ++this.currentLevel) {
				this.wayList = this.shapePaintContainers.get(this.currentLevel);
				for (this.arrayListIndex = this.wayList.size() - 1; this.arrayListIndex >= 0; --this.arrayListIndex) {
					this.shapePaintContainer = this.wayList.get(this.arrayListIndex);
					switch (this.shapePaintContainer.shapeContainer.getShapeType()) {
						case CIRCLE:
							// this.circleContainer = (CircleContainer)
							// this.shapePaintContainer.shapeContainer;
							// // this.shapePaintContainer.paint.
							//
							// this.mGL11.glVertexPointer(2, GL10.GL_FLOAT, 0, 0);
							// // mGL11.glColorPointer( 2, GL10.GL_FLOAT, 6 * 2, 2 * 4 );
							//
							// this.mGL11.glPointSize(this.circleContainer.radius * 2f);
							// this.circleVertexBuffer.clear();
							// this.circleVertexBuffer.put(new float[] { this.circleContainer.x,
							// this.circleContainer.y });
							// // mGL11.glLineWidth(8f);
							// this.mGL11.glDrawArrays(GL10.GL_POINTS, 0, 1);
							//
							// break;

						case WAY:

							break;
					}

				}
			}
		}

	}
}