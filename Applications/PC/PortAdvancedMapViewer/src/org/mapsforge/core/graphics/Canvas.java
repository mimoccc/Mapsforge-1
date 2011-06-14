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
	
	public Canvas() {
		// TODO Auto-generated constructor stub
	}

	private static void throwIfRecycled(Bitmap bitmap) {
        /*if (bitmap.isRecycled()) {
            throw new RuntimeException(
                        "Canvas: trying to use a recycled bitmap " + bitmap);
        }*/
    }
	
	private static native int initRaster(int nativeBitmapOrZero);

	public void setBitmap(Bitmap overlayBitmap2) {
		// TODO Auto-generated method stub
		
	}

	public int getWidth() {
		// TODO Auto-generated method stub
		return 0;
	}

	public int getHeight() {
		// TODO Auto-generated method stub
		return 0;
	}

	public void drawBitmap(Bitmap overlayBitmap1, Matrix matrix, Object object) {
		// TODO Auto-generated method stub
		
	}

	public void drawPath(Path path, Paint defaultPaintFill) {
		// TODO Auto-generated method stub
		
	}

	public void drawLine(int i, int j, float f, int k, Paint paintScaleBarStroke) {
		// TODO Auto-generated method stub
		
	}

	public void drawLine(float f, int j, float f2, int k,
			Paint paintScaleBarStroke) {
		// TODO Auto-generated method stub
		
	}

	public void drawText(String string, int i, int j,
			Paint paintScaleBarTextWhiteStroke) {
		// TODO Auto-generated method stub
		
	}

	public void drawBitmap(Bitmap mapScaleBitmap, int i, int j, Object object) {
		// TODO Auto-generated method stub
		
	}

	public void drawBitmap(Bitmap bitmap, float f, float g, Object object) {
		// TODO Auto-generated method stub
		
	}

	public void drawText(String text, float x, float y, Paint paintBack) {
		// TODO Auto-generated method stub
		
	}

	public void drawLines(float[] tileFrame, Paint paintTileFrame) {
		// TODO Auto-generated method stub
		
	}

	public void drawTextOnPath(String text, Path path, int i, int j, Paint paint) {
		// TODO Auto-generated method stub
		
	}
}
