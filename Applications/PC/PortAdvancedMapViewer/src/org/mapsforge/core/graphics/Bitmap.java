package org.mapsforge.core.graphics;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;

public class Bitmap {

	public enum CompressFormat {
		JPG, PNG;
	}

	public enum Config {
		ALPHA_8, RGB_565, ARGB_4444, ARGB_8888;
	}

	public static final int DENSITY_NONE = 0;

	private boolean mRecycled;
	private BufferedImage mImage;
	int mImageWidth;
	int mImageHeight;

	public Bitmap(ByteBuffer buffer, int width, int height) {
		System.out.println(buffer);
		byte[] inArray = buffer.array();
		ByteArrayInputStream in = new ByteArrayInputStream(inArray);
		try {
			mImage = ImageIO.read(in);
			System.out.println(mImage);
			mImageWidth = width;
			mImageHeight = height;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Bitmap(BufferedImage image) {
		mImage = image;
		mImageWidth = mImage.getWidth();
		mImageHeight = mImage.getHeight();
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
			mImage.flush();
			mRecycled = true;
		}
	}

	/**
	 * Returns in pixels[] a copy of the data in the bitmap.
	 */
	public void getPixels(int[] pixels, int offset, int stride, int x, int y,
			int width, int height) {
		mImage.getRGB(x, y, width, height, pixels, offset, stride);
	}

	/**
	 * Replace pixels in the bitmap with the colors in the array.
	 */
	public void setPixels(int[] pixels, int offset, int stride, int x, int y,
			int width, int height) {
		mImage.setRGB(x, y, width, height, pixels, offset, stride);
	}

	// TODO NO Quality
	/**
	 * Write a compressed version of the bitmap to the specified outputstream.
	 * If this returns true, the bitmap can be reconstructed by passing a
	 * corresponding inputstream to BitmapFactory.decodeStream(). Note: not all
	 * Formats support all bitmap configs directly, so it is possible that the
	 * returned bitmap from BitmapFactory could be in a different bitdepth,
	 * and/or may have lost per-pixel alpha (e.g. JPEG only supports opaque
	 * pixels).
	 */
	public boolean compress(CompressFormat format, int quality,
			FileOutputStream outputStream) throws IOException {
		if (format.equals(CompressFormat.JPG)) {
			ImageIO.write(mImage, "jpg", outputStream);
		} else {
			ImageIO.write(mImage, "png", outputStream);
		}
		return false;
	}

	/**
	 * Returns a mutable bitmap with the specified width and height. Its initial
	 * density is as per getDensity().
	 */
	public static Bitmap createBitmap(int width, int height, Config config) {
		if (config.equals(Config.ALPHA_8))
			return new Bitmap(new BufferedImage(width, height,
					BufferedImage.TYPE_BYTE_GRAY));
		else if (config.equals(Config.ARGB_4444))
			return new Bitmap(new BufferedImage(width, height,
					BufferedImage.TYPE_4BYTE_ABGR));
		else if (config.equals(Config.ARGB_8888))
			return new Bitmap(new BufferedImage(width, height,
					BufferedImage.TYPE_INT_ARGB));
		else
			return new Bitmap(new BufferedImage(width, height,
					BufferedImage.TYPE_USHORT_565_RGB));
	}

	/**
	 * Returns the bitmap's height
	 */
	public int getHeight() {
		return mImageHeight;
	}

	/**
	 * Returns the bitmap's width
	 */
	public int getWidth() {
		return mImageWidth;
	}

	public BufferedImage getImage() {
		return mImage;
	}

	/**
	 * Fills the bitmap's pixels with the specified Color.
	 */
	public void eraseColor(int c) {
		for (int x = 0; x < getWidth(); x++) {
			for (int y = 0; y < getHeight(); y++) {
				mImage.setRGB(x, y, c);
			}
		}
	}

	/**
	 * Fills the bitmap's pixels with the specified Color.
	 */
	public void eraseColor(Color c) {
		int cRGB = c.getRGB();
		for (int x = 0; x < getWidth(); x++) {
			for (int y = 0; y < getHeight(); y++) {
				mImage.setRGB(x, y, cRGB);
			}
		}
	}
}
