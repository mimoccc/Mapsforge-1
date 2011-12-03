/*
 * Copyright 2010, 2011 mapsforge.org
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.mapsforge.android.maps.mapgenerator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;

import org.mapsforge.android.maps.Logger;
import org.mapsforge.android.maps.Tile;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.os.Build;
import android.os.Environment;

/**
 * A thread-safe cache for image files with a fixed size and LRU policy.
 */
public class FileSystemTileCache extends TileCache {
	private static final class ImageFileNameFilter implements FilenameFilter {
		static final FilenameFilter INSTANCE = new ImageFileNameFilter();

		private ImageFileNameFilter() {
			// do nothing
		}

		@Override
		public boolean accept(File directory, String fileName) {
			return fileName.endsWith(IMAGE_FILE_NAME_EXTENSION);
		}
	}

	/**
	 * Path to the caching folder on the external storage.
	 */
	private static final String CACHE_DIRECTORY = "/Android/data/org.mapsforge.android.maps/cache/";

	/**
	 * Build names to detect the emulator from the Android SDK.
	 */
	private static final String[] EMULATOR_NAMES = { "google_sdk", "sdk" };

	/**
	 * File name extension for cached images.
	 */
	private static final String IMAGE_FILE_NAME_EXTENSION = ".tile";

	/**
	 * Name of the file used for serialization of the cache map.
	 */
	private static final String SERIALIZATION_FILE_NAME = "cache.ser";

	/**
	 * Detects if the application is running on the Android emulator.
	 * 
	 * @return true if the application is running on the Android emulator, false otherwise.
	 */
	private static boolean applicationRunsOnAndroidEmulator() {
		for (int i = 0, n = EMULATOR_NAMES.length; i < n; ++i) {
			if (Build.PRODUCT.equals(EMULATOR_NAMES[i])) {
				return true;
			}
		}
		return false;
	}

	private static File createDirectory(String pathName) {
		File file = new File(pathName);
		if (!file.exists() && !file.mkdirs()) {
			throw new IllegalArgumentException("could not create directory: " + file);
		} else if (!file.isDirectory()) {
			throw new IllegalArgumentException("not a directory: " + file);
		} else if (!file.canRead()) {
			throw new IllegalArgumentException("cannot read directory: " + file);
		} else if (!file.canWrite()) {
			throw new IllegalArgumentException("cannot write directory: " + file);
		}
		return file;
	}

	private static Map<MapGeneratorJob, File> createMap(final int mapCapacity) {
		int initialCapacity = (int) (mapCapacity / LOAD_FACTOR) + 2;

		return new LinkedHashMap<MapGeneratorJob, File>(initialCapacity, LOAD_FACTOR, true) {
			private static final long serialVersionUID = 1L;

			@Override
			protected boolean removeEldestEntry(Map.Entry<MapGeneratorJob, File> eldestEntry) {
				if (size() > mapCapacity) {
					remove(eldestEntry.getKey());
					if (!eldestEntry.getValue().delete()) {
						eldestEntry.getValue().deleteOnExit();
					}
				}
				return false;
			}
		};
	}

	/**
	 * Restores the serialized cache map if possible.
	 * 
	 * @param directory
	 *            the directory of the serialized map file.
	 * @return the deserialized map or null, in case of an error.
	 */
	private static Map<MapGeneratorJob, File> deserializeMap(File directory) {
		try {
			File serializedMapFile = new File(directory, SERIALIZATION_FILE_NAME);
			if (!serializedMapFile.exists()) {
				return null;
			} else if (!serializedMapFile.isFile()) {
				return null;
			} else if (!serializedMapFile.canRead()) {
				return null;
			}

			FileInputStream fileInputStream = new FileInputStream(serializedMapFile);
			ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);

			// the compiler warning in the following line cannot be avoided unfortunately
			Map<MapGeneratorJob, File> map = (Map<MapGeneratorJob, File>) objectInputStream.readObject();

			objectInputStream.close();
			fileInputStream.close();

			if (!serializedMapFile.delete()) {
				serializedMapFile.deleteOnExit();
			}

			return map;
		} catch (IOException e) {
			Logger.exception(e);
			return null;
		} catch (ClassNotFoundException e) {
			Logger.exception(e);
			return null;
		}
	}

	/**
	 * Serializes the cache map.
	 * 
	 * @param directory
	 *            the directory of the serialized map file.
	 * @param map
	 *            the map to be serialized.
	 * @return true if the map was serialized successfully, false otherwise.
	 */
	private static boolean serializeMap(File directory, Map<MapGeneratorJob, File> map) {
		try {
			File serializedMapFile = new File(directory, SERIALIZATION_FILE_NAME);
			if (serializedMapFile.exists() && !serializedMapFile.delete()) {
				return false;
			}

			FileOutputStream fileOutputStream = new FileOutputStream(serializedMapFile);
			ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
			objectOutputStream.writeObject(map);
			objectOutputStream.close();
			fileOutputStream.close();

			return true;
		} catch (IOException e) {
			Logger.exception(e);
			return false;
		}
	}

	private final ByteBuffer byteBufferGet;
	private final ByteBuffer byteBufferPut;
	private final File cacheDirectory;
	private long cacheId;
	private final Map<MapGeneratorJob, File> map;
	private boolean persistent;
	private final Bitmap bitmapGet;

	/**
	 * @param mapViewId
	 *            the ID of the MapView to separate caches for different MapViews.
	 * @param cacheCapacity
	 *            the maximum number of entries in the cache.
	 */
	public FileSystemTileCache(int mapViewId, int cacheCapacity) {
		super(applicationRunsOnAndroidEmulator() ? 0 : cacheCapacity);

		String externalStorageDirectory = Environment.getExternalStorageDirectory().getAbsolutePath();
		String cacheDirectoryPath = externalStorageDirectory + CACHE_DIRECTORY + mapViewId;
		this.cacheDirectory = createDirectory(cacheDirectoryPath);

		Map<MapGeneratorJob, File> deserializedMap = deserializeMap(this.cacheDirectory);
		if (deserializedMap == null) {
			this.map = createMap(this.cacheCapacity);
		} else {
			this.map = deserializedMap;
		}
		this.byteBufferGet = ByteBuffer.allocate(Tile.TILE_SIZE_IN_BYTES);
		this.byteBufferPut = ByteBuffer.allocate(Tile.TILE_SIZE_IN_BYTES);
		this.bitmapGet = Bitmap.createBitmap(Tile.TILE_SIZE, Tile.TILE_SIZE, Config.RGB_565);
	}

	@Override
	public boolean containsKey(MapGeneratorJob mapGeneratorJob) {
		synchronized (this.map) {
			return this.map.containsKey(mapGeneratorJob);
		}
	}

	@Override
	public void destroy() {
		synchronized (this.map) {
			if (!this.persistent || !serializeMap(this.cacheDirectory, this.map)) {
				for (File file : this.map.values()) {
					if (!file.delete()) {
						file.deleteOnExit();
					}
				}
				this.map.clear();

				synchronized (this.cacheDirectory) {
					for (File file : this.cacheDirectory.listFiles(ImageFileNameFilter.INSTANCE)) {
						if (!file.delete()) {
							file.deleteOnExit();
						}
					}

					if (!this.cacheDirectory.delete()) {
						this.cacheDirectory.deleteOnExit();
					}
				}
			}
		}
	}

	@Override
	public Bitmap get(MapGeneratorJob mapGeneratorJob) {
		if (this.cacheCapacity == 0) {
			return null;
		}

		try {
			File inputFile;
			synchronized (this.map) {
				inputFile = this.map.get(mapGeneratorJob);
			}

			FileInputStream fileInputStream = new FileInputStream(inputFile);
			synchronized (this.byteBufferGet) {
				byte[] array = this.byteBufferGet.array();
				int bytesRead = fileInputStream.read(array);
				fileInputStream.close();

				if (bytesRead == array.length) {
					this.bitmapGet.copyPixelsFromBuffer(this.byteBufferGet);
					return this.bitmapGet;
				}
			}

			return null;
		} catch (FileNotFoundException e) {
			synchronized (this.map) {
				this.map.remove(mapGeneratorJob);
			}
			return null;
		} catch (IOException e) {
			Logger.exception(e);
			return null;
		}
	}

	@Override
	public boolean isPersistent() {
		return this.persistent;
	}

	@Override
	public void put(MapGeneratorJob mapGeneratorJob, Bitmap bitmap) {
		if (this.cacheCapacity == 0) {
			return;
		}

		try {
			File outputFile;
			synchronized (this.cacheDirectory) {
				do {
					++this.cacheId;
					outputFile = new File(this.cacheDirectory, this.cacheId + IMAGE_FILE_NAME_EXTENSION);
				} while (outputFile.exists());
			}

			FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
			synchronized (this.byteBufferPut) {
				this.byteBufferPut.rewind();
				bitmap.copyPixelsToBuffer(this.byteBufferPut);
				byte[] array = this.byteBufferPut.array();
				fileOutputStream.write(array, 0, array.length);
			}
			fileOutputStream.close();

			synchronized (this.map) {
				this.map.put(mapGeneratorJob, outputFile);
			}
		} catch (IOException e) {
			Logger.exception(e);
		}
	}

	@Override
	public void setPersistent(boolean persistent) {
		this.persistent = persistent;
	}
}