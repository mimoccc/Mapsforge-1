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
package org.mapsforge.preprocessing.map.symbolGeneratorTool;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

/**
 * Tool to generate the MapSymbols java file. It takes a directory that contains SVG image
 * files. Each SVG image file is converted to a PNG image file using Inkscape. Afterwards the
 * PNG file is converted to an 8 bit RGBA image file with ImageMagick. This raw image file is
 * then translated into static java code for Android that can be drawn very fast at runtime.
 */
class SymbolGenerator {
	private static class ImageContainer {
		final String fileName;
		final int height;
		final int width;

		ImageContainer(String fileName, int width, int height) {
			this.fileName = fileName;
			this.width = width;
			this.height = height;
		}
	}

	public static void main(String[] args) throws IOException, InterruptedException {
		// check for correct usage
		if (args.length != 1) {
			System.err.println("usage: directory");
			System.exit(1);
		}

		// check for valid directory parameter
		File directory = new File(args[0]);
		if (!directory.exists()) {
			System.err.println("error: " + directory.getAbsolutePath() + " does not exist");
			System.exit(1);
		} else if (!directory.isDirectory()) {
			System.err.println("error: " + directory.getAbsolutePath() + " is not a directory");
			System.exit(1);
		} else if (!directory.canRead()) {
			System.err.println("error: " + directory.getAbsolutePath() + " cannot be read");
			System.exit(1);
		}

		String filePathWithoutExtension;
		Runtime runtime = Runtime.getRuntime();
		Process process;
		InputStream inputStream;
		byte[] imageDimensionData;
		byte[] rawImageData;
		int width;
		int height;
		String fileName;
		ArrayList<ImageContainer> processedImages = new ArrayList<ImageContainer>();

		File[] svgFiles = directory.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".svg");
			}
		});

		// check if the directory has SVG files to process
		if (svgFiles.length == 0) {
			System.err.println("error: " + directory.getAbsolutePath()
					+ " contains no SVG files");
			System.exit(1);
		}

		Arrays.sort(svgFiles);

		// for each SVG file in the given folder
		for (File file : svgFiles) {
			// get the name of the file without the extension
			filePathWithoutExtension = getFilePathWithoutFileExtension(file);

			// convert the SVG file into a temporary PNG file
			process = runtime.exec("inkscape -z --file=" + file.getAbsolutePath()
					+ " --export-area-drawing --export-png=" + filePathWithoutExtension
					+ ".png");
			if (process.waitFor() != 0) {
				System.err.println("error: inkscape exit code = " + process.waitFor());
				System.exit(1);
			}

			// get the width of the temporary PNG file
			process = runtime.exec("identify -ping -format %w " + filePathWithoutExtension
					+ ".png");
			inputStream = process.getInputStream();
			imageDimensionData = new byte[8];
			if (inputStream.read(imageDimensionData) == -1) {
				System.err.println("error: could not read image dimension input stream");
				System.exit(1);
			}
			inputStream.close();
			if (process.waitFor() != 0) {
				System.err.println("error: identify exit code = " + process.waitFor());
				System.exit(1);
			}
			width = Integer.parseInt(new String(imageDimensionData).trim());

			// get the height of the temporary PNG file
			process = runtime.exec("identify -ping -format %h " + filePathWithoutExtension
					+ ".png");
			inputStream = process.getInputStream();
			imageDimensionData = new byte[8];
			if (inputStream.read(imageDimensionData) == -1) {
				System.err.println("error: could not read image dimension input stream");
				System.exit(1);
			}
			inputStream.close();
			if (process.waitFor() != 0) {
				System.err.println("error: identify exit code = " + process.waitFor());
				System.exit(1);
			}
			height = Integer.parseInt(new String(imageDimensionData).trim());

			// convert the temporary PNG file into an 8 bit RGBA file with 4
			// bytes per pixel and forward the output to STDOUT
			process = runtime.exec("convert " + filePathWithoutExtension + ".png RGBA:fd:1");
			inputStream = process.getInputStream();
			rawImageData = new byte[width * height * 4];
			if (inputStream.read(rawImageData) != rawImageData.length) {
				System.err.println("error: could not read RGBA input stream");
				System.exit(1);
			}
			inputStream.close();
			if (process.waitFor() != 0) {
				System.err.println("error: convert exit code = " + process.waitFor());
				System.exit(1);
			}

			fileName = getFileNameFromFilePath(filePathWithoutExtension);
			generateImageJavaCode(fileName, rawImageData);
			processedImages.add(new ImageContainer(fileName, width, height));

			// delete the temporary PNG file
			if (!(new File(filePathWithoutExtension + ".png")).delete()) {
				System.err.println("error: could not delete png file "
						+ filePathWithoutExtension + ".png");
				System.exit(1);
			}
		}

		// print the java MapSymbols class
		System.out.println("final class MapSymbols {");

		// print the java variables
		System.out.println("\tprivate final ArrayList<Bitmap> bitmapList;");
		for (ImageContainer imageContainer : processedImages) {
			System.out.println("\tfinal Bitmap " + imageContainer.fileName.replace('-', '_')
					+ ";");
		}

		// print the java constructor
		System.out.println();
		System.out.println("\tMapSymbols() {");
		System.out.println("\t\tthis.bitmapList = new ArrayList<Bitmap>();");
		for (ImageContainer imageContainer : processedImages) {
			System.out.println();
			System.out.println("\t\tthis." + imageContainer.fileName.replace('-', '_')
					+ " = Bitmap.createBitmap(" + imageContainer.width + ", "
					+ imageContainer.height + ", Bitmap.Config.ARGB_8888);");
			System.out.println("\t\tthis." + imageContainer.fileName.replace('-', '_')
					+ ".copyPixelsFromBuffer(ByteBuffer.wrap("
					+ capitalizeFirstLetter(imageContainer.fileName.replace('-', '_'))
					+ ".data));");
			System.out.println("\t\tthis.bitmapList.add(this."
					+ imageContainer.fileName.replace('-', '_') + ");");
		}
		System.out.println("\t}");
		System.out.println();

		// print the java method for cleaning up
		System.out.println("\tfinal void recycle() {");
		System.out.println("\t\tfor (Bitmap bitmap : this.bitmapList) {");
		System.out.println("\t\t\tbitmap.recycle();");
		System.out.println("\t\t}");
		System.out.println("\t\tthis.bitmapList.clear();");
		System.out.println("\t}");

		System.out.print("}");
	}

	private static void generateImageJavaCode(String fileName, byte[] imageData) {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("final class " + capitalizeFirstLetter(fileName.replace('-', '_'))
				+ " {\n");
		stringBuilder.append("\tstatic final byte[] data = { ");

		int alpha;
		int red;
		int green;
		int blue;
		double colorFactor;
		for (int i = 0; i < imageData.length; i += 4) {
			// red one RGBA pixel
			red = imageData[i] & 0xFF;
			green = imageData[i + 1] & 0xFF;
			blue = imageData[i + 2] & 0xFF;
			alpha = imageData[i + 3] & 0xFF;

			// calculate the opacity factor
			colorFactor = alpha / 256d;

			// scale the RGB values with the opacity factor
			red = (byte) (red * colorFactor);
			green = (byte) (green * colorFactor);
			blue = (byte) (blue * colorFactor);
			alpha = (byte) alpha;

			// write the modified four pixel bytes
			stringBuilder.append(red + ", " + green + ", " + blue + ", " + alpha + ", ");
		}
		stringBuilder.delete(stringBuilder.length() - 2, stringBuilder.length());

		stringBuilder.append(" };");
		stringBuilder.append("\n}");
		System.out.println(stringBuilder);
	}

	private static String capitalizeFirstLetter(String str) {
		return str.substring(0, 1).toUpperCase(Locale.ENGLISH) + str.substring(1);
	}

	private static String getFileNameFromFilePath(String filePath) {
		return filePath.substring(filePath.lastIndexOf('/') + 1);
	}

	private static String getFilePathWithoutFileExtension(File file) {
		return file.getAbsolutePath().substring(0, file.getAbsolutePath().lastIndexOf('.'));
	}
}