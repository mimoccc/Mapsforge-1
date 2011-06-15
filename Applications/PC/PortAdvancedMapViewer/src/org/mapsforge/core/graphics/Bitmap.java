package org.mapsforge.core.graphics;

/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.awt.image.BufferedImage;
import java.io.FileOutputStream;

import java.nio.ByteBuffer;



public final class Bitmap {

    public enum CompressFormat {
        JPEG    (0),
        PNG     (1);

        CompressFormat(int nativeInt) {
            this.nativeInt = nativeInt;
        }
        final int nativeInt;
    }
    public enum Config {
        // these native values must match up with the enum in SkBitmap.h
        ALPHA_8     (2),
        RGB_565     (4),
        ARGB_4444   (5),
        ARGB_8888   (6);

        Config(int ni) {
            this.nativeInt = ni;
        }
        final int nativeInt;

        /* package */ static Config nativeToConfig(int ni) {
            return sConfigs[ni];
        }

        private static Config sConfigs[] = {
            null, null, ALPHA_8, null, RGB_565, ARGB_4444, ARGB_8888
        };
    }

    public static final int DENSITY_NONE = 0;

    // Note:  mNativeBitmap is used by FaceDetector_jni.cpp
    // Don't change/rename without updating FaceDetector_jni.cpp
    private final int mNativeBitmap;

    private final boolean mIsMutable;
    private byte[] mNinePatchChunk;   // may be null
    private int mWidth = -1;
    private int mHeight = -1;
    private boolean mRecycled;
    
    private Bitmap(int nativeBitmap, boolean isMutable, byte[] ninePatchChunk,
            int density) {
        if (nativeBitmap == 0) {
            throw new RuntimeException("internal error: native bitmap is 0");
        }

        // we delete this in our finalizer
        mNativeBitmap = nativeBitmap;
        mIsMutable = isMutable;
        mNinePatchChunk = ninePatchChunk;
        //TODO
        /*if (density >= 0) {
            mDensity = density;
        }*/
    }
    

    /**
     * Free up the memory associated with this bitmap's pixels, and mark the
     * bitmap as "dead", meaning it will throw an exception if getPixels() or
     * setPixels() is called, and will draw nothing. This operation cannot be
     * reversed, so it should only be called if you are sure there are no
     * further uses for the bitmap. This is an advanced call, and normally need
     * not be called, since the normal GC process will free up this memory when
     * there are no more references to this bitmap.
     */
    public void recycle() {
    	if (!mRecycled) {
            //TODO nativeRecycle(mNativeBitmap);
            mNinePatchChunk = null;
            mRecycled = true;
        }
    }

	public void copyPixelsToBuffer(ByteBuffer d) {
		// TODO Auto-generated method stub
		
	}

	public void copyPixelsFromBuffer(ByteBuffer bitmapBuffer) {
		// TODO Auto-generated method stub
		
	}

	public void getPixels(int[] pixelColors, int i, short tileSize, int j,
			int k, short tileSize2, short tileSize3) {
		// TODO Auto-generated method stub
		
	}

	public void setPixels(int[] pixelColors, int i, short tileSize, int j,
			int k, short tileSize2, short tileSize3) {
		// TODO Auto-generated method stub
		
	}

	public void eraseColor(java.awt.Color transparent) {
		// TODO Auto-generated method stub
		
	}

	public boolean compress(CompressFormat format, int quality,
			FileOutputStream outputStream) {
		// TODO Auto-generated method stub
		return false;
	}

	public void eraseColor(int mapViewBackground) {
		// TODO Auto-generated method stub
		
	}

	public static Bitmap createBitmap(int width, int height, Config argb8888) {
		// TODO Auto-generated method stub
		return null;
	}

	public int getHeight() {
		// TODO Auto-generated method stub
		return 0;
	}
	
	final int ni() {
        return mNativeBitmap;
    }

	public int getWidth() {
		// TODO Auto-generated method stub
		return 0;
	}

	public BufferedImage getImage() {
		// TODO Auto-generated method stub
		return null;
	}
}