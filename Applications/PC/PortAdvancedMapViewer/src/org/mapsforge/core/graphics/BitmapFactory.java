package org.mapsforge.core.graphics;

import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;

/**
 * Creates Bitmap objects from various sources, including files, streams, and
 * byte-arrays.
 */
public class BitmapFactory {

	public static Bitmap decodeStream(InputStream inputStream) {
		Bitmap bitmap;
		try {
			bitmap = new Bitmap(ImageIO.read(inputStream));
			return bitmap;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
}