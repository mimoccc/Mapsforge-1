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
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;

class OpenGlMapRenderer implements android.opengl.GLSurfaceView.Renderer {
	private int arrayListIndex;
	private Bitmap bitmap;
	private byte currentLayer;
	private byte currentLevel;
	private GL10 mGL;
	private ByteBuffer pixelBuffer;
	// private Random random;
	private ShapePaintContainer shapePaintContainer;
	private ArrayList<ArrayList<ShapePaintContainer>> shapePaintContainers;
	private ArrayList<ShapePaintContainer> wayList;
	boolean frameReady;
	private GL11 mGL11;
	private ArrayList<ShapePaintContainer> objectsToDraw;
	private WayContainer complexWayContainer;
	private float[][] coordinates;
	private Paint paint;
	private FloatBuffer vertices;
	private int vboHandle;
	private CircleContainer circleContainer;
	private ByteBuffer circleByteBuffer;
	private FloatBuffer circleVertexBuffer;
	private int circleBufferHandle;
	private int color;
	private ByteBuffer vbuffer;

	OpenGlMapRenderer() {
		Logger.d("OpenGlMapRenderer called");
		// this.random = new Random();
		this.objectsToDraw = new ArrayList<ShapePaintContainer>(1024);
	}

	public void drawWays(ArrayList<ArrayList<ArrayList<ShapePaintContainer>>> drawWays,
			byte layers, byte levelsPerLayer) {

		// extract all ways in all layers and all levels and add them to objectsToDraw()
		// ArrayList
		for (this.currentLayer = 0; this.currentLayer < layers; ++this.currentLayer) {
			this.shapePaintContainers = drawWays.get(this.currentLayer);
			for (this.currentLevel = 0; this.currentLevel < levelsPerLayer; ++this.currentLevel) {
				this.wayList = this.shapePaintContainers.get(this.currentLevel);
				for (this.arrayListIndex = this.wayList.size() - 1; this.arrayListIndex >= 0; --this.arrayListIndex) {
					this.shapePaintContainer = this.wayList.get(this.arrayListIndex);
					objectsToDraw.add(this.shapePaintContainer);
				}
			}
		}
	}

	@Override
	public void onDrawFrame(GL10 gl) {
		Logger.d("onDrawFrame called");

		// gl.glClearColor(this.random.nextFloat(), this.random.nextFloat(), this.random
		// .nextFloat(), 1);
		gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
		gl.glLoadIdentity(); // Reset The Current Modelview Matrix

		for (this.arrayListIndex = 0; this.arrayListIndex < this.objectsToDraw.size(); ++this.arrayListIndex) {
			this.shapePaintContainer = this.objectsToDraw.get(this.arrayListIndex);
			switch (this.shapePaintContainer.shapeContainer.getShapeType()) {
				case CIRCLE:
					this.circleContainer = (CircleContainer) this.shapePaintContainer.shapeContainer;

					this.paint = this.shapePaintContainer.paint;
					this.color = this.paint.getColor();

					this.circleVertexBuffer.clear();
					this.circleVertexBuffer.put(new float[] {
							(this.circleContainer.x / 128 - 1.0f),
							(this.circleContainer.y / 128 - 1.0f), 0f,
							(float) Color.red(color) / 256, (float) Color.green(color) / 256,
							(float) Color.blue(color) / 256 });

					this.circleVertexBuffer.flip();

					// set point size
					this.mGL11.glPointSize(this.circleContainer.radius * 2);

					// bind the vertex buffer
					this.mGL11.glBindBuffer(GL11.GL_ARRAY_BUFFER, circleBufferHandle);
					// transfer data into video memory
					this.mGL11.glBufferData(GL11.GL_ARRAY_BUFFER, 4 * 6 * 3,
							this.circleVertexBuffer, GL11.GL_DYNAMIC_DRAW);

					this.mGL11.glEnableClientState(GL10.GL_VERTEX_ARRAY);
					this.mGL11.glEnableClientState(GL10.GL_COLOR_ARRAY);
					this.mGL11.glVertexPointer(3, GL10.GL_FLOAT, 0, 0);
					this.mGL11.glColorPointer(3, GL10.GL_FLOAT, 6 * 4, 3 * 4);

					this.mGL11.glDrawArrays(GL10.GL_POINTS, 0, 1);
					// unbind the buffer
					this.mGL11.glBindBuffer(GL11.GL_ARRAY_BUFFER, 0);

					break;

				case WAY:

					this.complexWayContainer = (WayContainer) this.shapePaintContainer.shapeContainer;
					this.coordinates = this.complexWayContainer.coordinates;
					this.paint = this.shapePaintContainer.paint;
					this.color = this.paint.getColor();

					this.vertices.rewind();

					for (int j = 0; j < this.coordinates.length; ++j) {

						this.vertices.clear();
						int i;
						for (i = 0; i < this.coordinates[j].length; i += 2) {

							this.vertices.put(new float[] {
									(this.coordinates[j][i] / 128 - 1.0f),
									(this.coordinates[j][i + 1] / 128 - 1.0f), 0f,
									(float) Color.red(color) / 256,
									(float) Color.green(color) / 256,
									(float) Color.blue(color) / 256 });

						}
						this.vertices.flip();

						// bind the buffer
						mGL11.glBindBuffer(GL11.GL_ARRAY_BUFFER, vboHandle);
						mGL11.glBufferData(GL11.GL_ARRAY_BUFFER, 4 * 6 * i / 2, vertices,
								GL11.GL_DYNAMIC_DRAW);

						mGL11.glEnableClientState(GL10.GL_VERTEX_ARRAY);
						mGL11.glEnableClientState(GL10.GL_COLOR_ARRAY);
						mGL11.glVertexPointer(3, GL10.GL_FLOAT, 6 * 4, 0);
						mGL11.glColorPointer(3, GL10.GL_FLOAT, 6 * 4, 3 * 4);

						mGL11.glLineWidth(this.paint.getStrokeWidth());
						// mGL11.glLineWidth(1.0f);
						// make points clearly visible for debugging
						this.mGL11.glPointSize(4.0f);

						mGL11.glDrawArrays(GL10.GL_POINTS, 0, i / 2);
						mGL11.glDrawArrays(GL10.GL_LINE_LOOP, 0, i / 2);
						// mGL11.glDrawArrays(GL10.GL_TRIANGLE_FAN, 0, i / 2);

						// unbind the buffer
						mGL11.glBindBuffer(GL11.GL_ARRAY_BUFFER, 0);

					}
					break;
			}
		}
		this.objectsToDraw.clear();

		if (this.pixelBuffer != null) {

			this.mGL.glReadPixels(0, 0, this.bitmap.getWidth(), this.bitmap.getHeight(),
					GL10.GL_RGB, GL10.GL_UNSIGNED_SHORT_5_6_5, this.pixelBuffer);

			// // conversion from RGBA to RGB 565
			// int newLength = this.pixelBuffer.array().length / 2;
			// byte[] bufferArray = this.pixelBuffer.array();
			// byte tempByte;
			// for (int i = 0; i < newLength; i += 2) {
			// tempByte = (byte) ((0xF8 & bufferArray[i * 2 + 1]) << 2 | (0xFF & bufferArray[i *
			// 2 + 2]) >> 3);
			// bufferArray[i + 1] = (byte) (0xF8 & bufferArray[i * 2] | (0xFF & bufferArray[i *
			// 2 + 1]) >> 5);
			// bufferArray[i] = tempByte;
			// }

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
		// save gl10 and gl11 objects
		this.mGL = gl;
		this.mGL11 = (GL11) gl;

		gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_FASTEST);
		gl.glClearColor(0.9f, 0.9f, 0.9f, 1);

		// set up buffers for VBOs
		this.vbuffer = ByteBuffer.allocateDirect(4 * 6 * 1000);
		this.vbuffer.order(ByteOrder.nativeOrder());
		this.vertices = vbuffer.asFloatBuffer();

		this.circleByteBuffer = ByteBuffer.allocateDirect(4 * 6 * 100);
		this.circleByteBuffer.order(ByteOrder.nativeOrder());
		this.circleVertexBuffer = this.circleByteBuffer.asFloatBuffer();

		// handles
		int[] handle = new int[2];
		this.mGL11.glGenBuffers(2, handle, 0); // find unused buffers and save in handle[]
		this.vboHandle = handle[0];
		this.circleBufferHandle = handle[1];
	}

	void setBitmap(Bitmap bitmap) {
		this.bitmap = bitmap;
		byte[] bytes = new byte[2 * this.bitmap.getHeight() * this.bitmap.getWidth()];
		this.pixelBuffer = ByteBuffer.wrap(bytes);
	}
}