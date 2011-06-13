package org.mapsforge.core.graphics;

public class Canvas {
	
	public Canvas(Bitmap bitmap)
	{
		int mDensity = 0;//Bitmap.DENSITY_NONE;
		
		/*if (!bitmap.isMutable()) {
            throw new IllegalStateException(
                            "Immutable bitmap passed to Canvas constructor");
        }
        throwIfRecycled(bitmap);
        mNativeCanvas = initRaster(bitmap.ni());
        mBitmap = bitmap;
        mDensity = bitmap.mDensity;*/
	}
	
	private static void throwIfRecycled(Bitmap bitmap) {
        /*if (bitmap.isRecycled()) {
            throw new RuntimeException(
                        "Canvas: trying to use a recycled bitmap " + bitmap);
        }*/
    }
	
	private static native int initRaster(int nativeBitmapOrZero);
}
