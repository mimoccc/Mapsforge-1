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
import java.util.Random;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.graphics.Bitmap;

public class OpenGlMapRenderer implements android.opengl.GLSurfaceView.Renderer {

	int vboHandle;

	GL10 mGL;

	private Random mRandom;

	Bitmap mBitmap;

	boolean frameReady;

	public OpenGlMapRenderer() {
		Logger.d("OpenGlMapRenderer constructor");
		this.mRandom = new Random();
		this.frameReady = false;
	}

	@Override
	public void onDrawFrame(GL10 gl) {

		gl.glClearColor(this.mRandom.nextFloat(), this.mRandom.nextFloat(), this.mRandom
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

		Logger.d(" drawing frame.");

		final int numPixelBytes = 4 * mBitmap.getHeight() * mBitmap.getWidth();

		// int[] b = new int[numPixelBytes];
		// int[] bt = new int[numPixelBytes];
		// IntBuffer pixelBuffer = IntBuffer.wrap(b);

		ByteBuffer pixelBuffer = ByteBuffer.allocateDirect(numPixelBytes);
		// pixelBuffer.order(ByteOrder.nativeOrder());

		// if (mBitmap == null) {
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

		// pixelBuffer.position(0);
		mGL.glReadPixels(0, 0, mBitmap.getWidth(), mBitmap.getHeight(), GL10.GL_RGBA,
				GL10.GL_UNSIGNED_BYTE, pixelBuffer);

		// pixelBuffer.rewind();
		// for (int i = 0; i < mBitmap.getHeight(); i++) {// remember, that OpenGL bitmap is
		// // incompatible with Android bitmap
		// // and so, some correction need.
		// for (int j = 0; j < mBitmap.getWidth(); j++) {
		// int pix = b[i * mBitmap.getWidth() + j];
		// int pb = (pix >> 16) & 0xff;
		// int pr = (pix << 16) & 0x00ff0000;
		// int pix1 = (pix & 0xff00ff00) | pr | pb;
		// bt[(mBitmap.getHeight() - i - 1) * mBitmap.getWidth() + j] = pix1;
		// }
		// }
		// this.mBitmap = Bitmap.createBitmap(bt, mBitmap.getWidth(), mBitmap.getHeight(),
		// Bitmap.Config.ALPHA_8);
		//		

		this.mBitmap.copyPixelsFromBuffer(pixelBuffer);

		frameReady = true;
		Logger.d("frame ready");

	}

	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		// TODO Auto-generated method stub

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

		// mGL11 = (GL11) gl;

		Logger.d("onSurfaceChanged() width: " + width + " height:" + height);
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {

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

		mGL = gl;
		Logger.d("onSurfaceCreated()");

	}

	public void setBitmap(Bitmap bitmap) {
		this.mBitmap = bitmap;
	}

}
