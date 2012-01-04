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
package org.mapsforge.applications.android.advancedmapviewer;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.Date;

import org.mapsforge.android.maps.DebugSettings;
import org.mapsforge.android.maps.MapActivity;
import org.mapsforge.android.maps.MapController;
import org.mapsforge.android.maps.MapScaleBar;
import org.mapsforge.android.maps.MapScaleBar.TextField;
import org.mapsforge.android.maps.MapView;
import org.mapsforge.android.maps.MapViewMode;
import org.mapsforge.android.maps.overlay.ArrayCircleOverlay;
import org.mapsforge.android.maps.overlay.ArrayItemizedOverlay;
import org.mapsforge.android.maps.overlay.ItemizedOverlay;
import org.mapsforge.android.maps.overlay.OverlayCircle;
import org.mapsforge.android.maps.overlay.OverlayItem;
import org.mapsforge.android.maps.rendertheme.InternalRenderTheme;
import org.mapsforge.applications.android.advancedmapviewer.filefilter.FilterByFileExtension;
import org.mapsforge.applications.android.advancedmapviewer.filefilter.ValidMapFile;
import org.mapsforge.applications.android.advancedmapviewer.filefilter.ValidRenderTheme;
import org.mapsforge.applications.android.advancedmapviewer.filepicker.FilePicker;
import org.mapsforge.applications.android.advancedmapviewer.preferences.EditPreferences;
import org.mapsforge.core.BoundingBox;
import org.mapsforge.core.GeoPoint;
import org.mapsforge.map.reader.MapFileInfo;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Color;
import android.graphics.Paint;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

/**
 * A map application which uses the features from the mapsforge map library. The map can be centered to the current
 * location. A simple file browser for selecting the map file is also included. Some preferences can be adjusted via the
 * {@link EditPreferences} activity and screenshots of the map may be taken in different image formats.
 */
public class AdvancedMapViewer extends MapActivity {
	public static final int FILE_SYSTEM_CACHE_SIZE_DEFAULT = 250;
	public static final int FILE_SYSTEM_CACHE_SIZE_MAX = 500;
	public static final int MOVE_SPEED_DEFAULT = 10;
	public static final int MOVE_SPEED_MAX = 30;
	private static final String BUNDLE_CENTER_AT_FIRST_FIX = "centerAtFirstFix";
	private static final String BUNDLE_SHOW_MY_LOCATION = "showMyLocation";
	private static final String BUNDLE_SNAP_TO_LOCATION = "snapToLocation";
	private static final int DIALOG_ENTER_COORDINATES = 0;
	private static final int DIALOG_INFO_MAP_FILE = 1;
	private static final int DIALOG_LOCATION_PROVIDER_DISABLED = 2;
	private static final FileFilter FILE_FILTER_EXTENSION_MAP = new FilterByFileExtension(".map");
	private static final FileFilter FILE_FILTER_EXTENSION_XML = new FilterByFileExtension(".xml");
	private static final String SCREENSHOT_DIRECTORY = "Pictures";
	private static final String SCREENSHOT_FILE_NAME = "Map screenshot";
	private static final int SCREENSHOT_QUALITY = 90;
	private static final int SELECT_MAP_FILE = 0;
	private static final int SELECT_RENDER_THEME_FILE = 1;

	private Paint circleOverlayFill;
	private Paint circleOverlayOutline;
	private LocationManager locationManager;
	private MyLocationListener myLocationListener;
	private boolean showMyLocation;
	private boolean snapToLocation;
	private ToggleButton snapToLocationView;
	private Toast toast;
	private WakeLock wakeLock;
	ArrayCircleOverlay circleOverlay;
	ArrayItemizedOverlay itemizedOverlay;
	MapController mapController;
	MapView mapView;
	OverlayCircle overlayCircle;
	OverlayItem overlayItem;

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.options_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_info:
				return true;

			case R.id.menu_info_map_file:
				showDialog(DIALOG_INFO_MAP_FILE);
				return true;

			case R.id.menu_info_about:
				startActivity(new Intent(this, InfoView.class));
				return true;

			case R.id.menu_position:
				return true;

			case R.id.menu_position_my_location_enable:
				enableShowMyLocation(true);
				return true;

			case R.id.menu_position_my_location_disable:
				disableShowMyLocation();
				return true;

			case R.id.menu_position_last_known:
				gotoLastKnownPosition();
				return true;

			case R.id.menu_position_enter_coordinates:
				showDialog(DIALOG_ENTER_COORDINATES);
				return true;

			case R.id.menu_position_map_center:
				// disable GPS follow mode if it is enabled
				disableSnapToLocation(true);
				this.mapController.setCenter(this.mapView.getMapDatabase().getMapFileInfo().getMapCenter());
				return true;

			case R.id.menu_screenshot:
				return true;

			case R.id.menu_screenshot_jpeg:
				captureScreenshotAsync(CompressFormat.JPEG);
				return true;

			case R.id.menu_screenshot_png:
				captureScreenshotAsync(CompressFormat.PNG);
				return true;

			case R.id.menu_preferences:
				startActivity(new Intent(this, EditPreferences.class));
				return true;

			case R.id.menu_render_theme:
				return true;

			case R.id.menu_render_theme_osmarender:
				this.mapView.setRenderTheme(InternalRenderTheme.OSMARENDER);
				return true;

			case R.id.menu_render_theme_select_file:
				startRenderThemePicker();
				return true;

			case R.id.menu_mapfile:
				startMapFilePicker();
				return true;

			default:
				return false;
		}
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		MapViewMode mapViewMode = this.mapView.getMapViewMode();

		if (mapViewMode.requiresInternetConnection()) {
			menu.findItem(R.id.menu_info_map_file).setEnabled(false);
		} else {
			menu.findItem(R.id.menu_info_map_file).setEnabled(true);
		}

		if (isShowMyLocationEnabled()) {
			menu.findItem(R.id.menu_position_my_location_enable).setVisible(false);
			menu.findItem(R.id.menu_position_my_location_enable).setEnabled(false);
			menu.findItem(R.id.menu_position_my_location_disable).setVisible(true);
			menu.findItem(R.id.menu_position_my_location_disable).setEnabled(true);
		} else {
			menu.findItem(R.id.menu_position_my_location_enable).setVisible(true);
			menu.findItem(R.id.menu_position_my_location_enable).setEnabled(true);
			menu.findItem(R.id.menu_position_my_location_disable).setVisible(false);
			menu.findItem(R.id.menu_position_my_location_disable).setEnabled(false);
		}

		if (mapViewMode.requiresInternetConnection()) {
			menu.findItem(R.id.menu_position_map_center).setEnabled(false);
		} else {
			menu.findItem(R.id.menu_position_map_center).setEnabled(true);
		}

		if (mapViewMode.requiresInternetConnection()) {
			menu.findItem(R.id.menu_render_theme).setEnabled(false);
		} else {
			menu.findItem(R.id.menu_render_theme).setEnabled(true);
		}

		if (mapViewMode.requiresInternetConnection()) {
			menu.findItem(R.id.menu_mapfile).setEnabled(false);
		} else {
			menu.findItem(R.id.menu_mapfile).setEnabled(true);
		}

		return true;
	}

	@Override
	public boolean onTrackballEvent(MotionEvent event) {
		// forward the event to the MapView
		return this.mapView.onTrackballEvent(event);
	}

	private void captureScreenshotAsync(final CompressFormat format) {
		new Thread() {
			@Override
			public void run() {
				try {
					File path = new File(Environment.getExternalStorageDirectory(), SCREENSHOT_DIRECTORY);
					// make sure the Pictures directory exists
					if (!path.exists() && !path.mkdirs()) {
						showToastOnUiThread("Could not create target directory");
						return;
					}

					// assemble the complete name for the screenshot file
					String fileName = path.getAbsolutePath() + File.separatorChar + SCREENSHOT_FILE_NAME + "."
							+ format.name().toLowerCase();

					if (AdvancedMapViewer.this.mapView.takeScreenshot(format, SCREENSHOT_QUALITY, fileName)) {
						// success
						showToastOnUiThread(fileName);
					} else {
						// failure
						showToastOnUiThread("Screenshot could not be saved");
					}
				} catch (IOException e) {
					showToastOnUiThread(e.getLocalizedMessage());
				}
			}

			private void showToastOnUiThread(final String message) {
				AdvancedMapViewer.this.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						showToast(message);
					}
				});
			}
		}.start();
	}

	private void configureMapView() {
		// configure the MapView and activate the zoomLevel buttons
		this.mapView.setClickable(true);
		this.mapView.setBuiltInZoomControls(true);
		this.mapView.setFocusable(true);

		// set the localized text fields
		MapScaleBar mapScaleBar = this.mapView.getMapScaleBar();
		mapScaleBar.setText(TextField.KILOMETER, getString(R.string.unit_symbol_kilometer));
		mapScaleBar.setText(TextField.METER, getString(R.string.unit_symbol_meter));

		// get the map controller for this MapView
		this.mapController = this.mapView.getController();
	}

	/**
	 * Enables the "show my location" mode.
	 * 
	 * @param centerAtFirstFix
	 *            defines whether the map should be centered to the first fix.
	 */
	private void enableShowMyLocation(boolean centerAtFirstFix) {
		if (!this.showMyLocation) {
			Criteria criteria = new Criteria();
			criteria.setAccuracy(Criteria.ACCURACY_FINE);
			String bestProvider = this.locationManager.getBestProvider(criteria, true);
			if (bestProvider == null) {
				showDialog(DIALOG_LOCATION_PROVIDER_DISABLED);
				return;
			}

			this.showMyLocation = true;

			this.circleOverlay = new ArrayCircleOverlay(this.circleOverlayFill, this.circleOverlayOutline);
			this.overlayCircle = new OverlayCircle();
			this.circleOverlay.addCircle(this.overlayCircle);
			this.mapView.getOverlays().add(this.circleOverlay);

			this.itemizedOverlay = new ArrayItemizedOverlay(null);
			this.overlayItem = new OverlayItem();
			this.overlayItem.setMarker(ItemizedOverlay.boundCenter(getResources().getDrawable(R.drawable.my_location)));
			this.itemizedOverlay.addItem(this.overlayItem);
			this.mapView.getOverlays().add(this.itemizedOverlay);

			this.myLocationListener.setCenterAtFirstFix(centerAtFirstFix);
			this.locationManager.requestLocationUpdates(bestProvider, 1000, 0, this.myLocationListener);
			this.snapToLocationView.setVisibility(View.VISIBLE);
		}
	}

	/**
	 * Formats the given file size as a human readable string, using SI prefixes.
	 * 
	 * @param fileSize
	 *            the file size to be formatted.
	 * @return a human readable file size.
	 * @throws IllegalArgumentException
	 *             if the given file size is negative.
	 */
	private String formatFileSize(long fileSize) {
		if (fileSize < 0) {
			throw new IllegalArgumentException("invalid file size: " + fileSize);
		} else if (fileSize < 1000) { // less than 1 kB
			if (fileSize == 1) {
				// singular
				return "1 " + getString(R.string.file_size_byte);
			}
			// plural, including zero
			return fileSize + " " + getString(R.string.file_size_bytes);
		} else {
			DecimalFormat decimalFormat = new DecimalFormat("#.0 ");
			if (fileSize < 1000000) { // less than 1 MB
				return decimalFormat.format(fileSize / 1000d) + getString(R.string.file_size_kb);
			} else if (fileSize < 1000000000) { // less than 1 GB
				return decimalFormat.format(fileSize / 1000000d) + getString(R.string.file_size_mb);
			}
			return decimalFormat.format(fileSize / 1000000000d) + getString(R.string.file_size_gb);
		}
	}

	/**
	 * Centers the map to the last known position as reported by the most accurate location provider. If the last
	 * location is unknown, a toast message is displayed instead.
	 */
	private void gotoLastKnownPosition() {
		Location currentLocation;
		Location bestLocation = null;
		for (String provider : this.locationManager.getProviders(true)) {
			currentLocation = this.locationManager.getLastKnownLocation(provider);
			if (bestLocation == null || currentLocation.getAccuracy() < bestLocation.getAccuracy()) {
				bestLocation = currentLocation;
			}
		}

		// check if a location has been found
		if (bestLocation != null) {
			GeoPoint point = new GeoPoint(bestLocation.getLatitude(), bestLocation.getLongitude());
			this.mapController.setCenter(point);
		} else {
			showToast(getString(R.string.error_last_location_unknown));
		}
	}

	/**
	 * Sets all file filters and starts the FilePicker to select a map file.
	 */
	private void startMapFilePicker() {
		FilePicker.setFileDisplayFilter(FILE_FILTER_EXTENSION_MAP);
		FilePicker.setFileSelectFilter(ValidMapFile.INSTANCE);
		startActivityForResult(new Intent(this, FilePicker.class), SELECT_MAP_FILE);
	}

	/**
	 * Sets all file filters and starts the FilePicker to select an XML file.
	 */
	private void startRenderThemePicker() {
		FilePicker.setFileDisplayFilter(FILE_FILTER_EXTENSION_XML);
		FilePicker.setFileSelectFilter(ValidRenderTheme.INSTANCE);
		startActivityForResult(new Intent(this, FilePicker.class), SELECT_RENDER_THEME_FILE);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == SELECT_MAP_FILE) {
			if (resultCode == RESULT_OK) {
				disableSnapToLocation(true);
				if (data != null && data.getStringExtra(FilePicker.SELECTED_FILE) != null) {
					this.mapView.setMapFile(data.getStringExtra(FilePicker.SELECTED_FILE));
				}
			} else {
				if (resultCode == RESULT_CANCELED && !this.mapView.getMapViewMode().requiresInternetConnection()
						&& this.mapView.getMapFile() == null) {
					finish();
				}
			}
		} else if (requestCode == SELECT_RENDER_THEME_FILE && resultCode == RESULT_OK && data != null
				&& data.getStringExtra(FilePicker.SELECTED_FILE) != null) {
			try {
				this.mapView.setRenderTheme(data.getStringExtra(FilePicker.SELECTED_FILE));
			} catch (FileNotFoundException e) {
				showToast(e.getLocalizedMessage());
			}
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// set up the layout views
		setContentView(R.layout.activity_advanced_map_viewer);
		this.mapView = (MapView) findViewById(R.id.mapView);
		configureMapView();

		this.snapToLocationView = (ToggleButton) findViewById(R.id.snapToLocationView);
		this.snapToLocationView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				if (isSnapToLocationEnabled()) {
					disableSnapToLocation(true);
				} else {
					enableSnapToLocation(true);
				}
			}
		});

		// get the pointers to different system services
		this.locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		this.myLocationListener = new MyLocationListener(this);
		PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
		this.wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "AMV");

		// set up the paint objects for the location overlay
		this.circleOverlayFill = new Paint(Paint.ANTI_ALIAS_FLAG);
		this.circleOverlayFill.setStyle(Paint.Style.FILL);
		this.circleOverlayFill.setColor(Color.BLUE);
		this.circleOverlayFill.setAlpha(48);

		this.circleOverlayOutline = new Paint(Paint.ANTI_ALIAS_FLAG);
		this.circleOverlayOutline.setStyle(Paint.Style.STROKE);
		this.circleOverlayOutline.setColor(Color.BLUE);
		this.circleOverlayOutline.setAlpha(128);
		this.circleOverlayOutline.setStrokeWidth(2);

		if (savedInstanceState != null && savedInstanceState.getBoolean(BUNDLE_SHOW_MY_LOCATION)) {
			enableShowMyLocation(savedInstanceState.getBoolean(BUNDLE_CENTER_AT_FIRST_FIX));
			if (savedInstanceState.getBoolean(BUNDLE_SNAP_TO_LOCATION)) {
				enableSnapToLocation(false);
			}
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		if (id == DIALOG_ENTER_COORDINATES) {
			builder.setIcon(android.R.drawable.ic_menu_mylocation);
			builder.setTitle(R.string.menu_position_enter_coordinates);
			LayoutInflater factory = LayoutInflater.from(this);
			final View view = factory.inflate(R.layout.dialog_enter_coordinates, null);
			builder.setView(view);
			builder.setPositiveButton(R.string.go_to_position, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// disable GPS follow mode if it is enabled
					disableSnapToLocation(true);

					// set the map center and zoom level
					EditText latitudeView = (EditText) view.findViewById(R.id.latitude);
					EditText longitudeView = (EditText) view.findViewById(R.id.longitude);
					double latitude = Double.parseDouble(latitudeView.getText().toString());
					double longitude = Double.parseDouble(longitudeView.getText().toString());
					GeoPoint geoPoint = new GeoPoint(latitude, longitude);
					AdvancedMapViewer.this.mapController.setCenter(geoPoint);
					SeekBar zoomLevelView = (SeekBar) view.findViewById(R.id.zoomLevel);
					AdvancedMapViewer.this.mapController.setZoom(zoomLevelView.getProgress());
				}
			});
			builder.setNegativeButton(R.string.cancel, null);
			return builder.create();
		} else if (id == DIALOG_LOCATION_PROVIDER_DISABLED) {
			builder.setIcon(android.R.drawable.ic_menu_info_details);
			builder.setTitle(R.string.error);
			builder.setMessage(R.string.no_location_provider_available);
			builder.setPositiveButton(R.string.ok, null);
			return builder.create();
		} else if (id == DIALOG_INFO_MAP_FILE) {
			builder.setIcon(android.R.drawable.ic_menu_info_details);
			builder.setTitle(R.string.menu_info_map_file);
			LayoutInflater factory = LayoutInflater.from(this);
			builder.setView(factory.inflate(R.layout.dialog_info_map_file, null));
			builder.setPositiveButton(R.string.ok, null);
			return builder.create();
		} else {
			// do dialog will be created
			return null;
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		disableShowMyLocation();
	}

	@Override
	protected void onPause() {
		super.onPause();
		// release the wake lock if necessary
		if (this.wakeLock.isHeld()) {
			this.wakeLock.release();
		}

		// remove the toast message if visible
		if (this.toast != null) {
			this.toast.cancel();
			this.toast = null;
		}
	}

	@Override
	protected void onPrepareDialog(int id, final Dialog dialog) {
		if (id == DIALOG_ENTER_COORDINATES) {
			// latitude
			EditText editText = (EditText) dialog.findViewById(R.id.latitude);
			GeoPoint mapCenter = this.mapView.getMapPosition().getMapCenter();
			editText.setText(Double.toString(mapCenter.getLatitude()));

			// longitude
			editText = (EditText) dialog.findViewById(R.id.longitude);
			editText.setText(Double.toString(mapCenter.getLongitude()));

			// zoom level
			SeekBar zoomlevel = (SeekBar) dialog.findViewById(R.id.zoomLevel);
			zoomlevel.setMax(this.mapView.getMapGenerator().getZoomLevelMax());
			zoomlevel.setProgress(this.mapView.getMapPosition().getZoomLevel());

			// zoom level value
			final TextView textView = (TextView) dialog.findViewById(R.id.zoomlevelValue);
			textView.setText(String.valueOf(zoomlevel.getProgress()));
			zoomlevel.setOnSeekBarChangeListener(new SeekBarChangeListener(textView));
		} else if (id == DIALOG_INFO_MAP_FILE) {
			MapFileInfo mapFileInfo = this.mapView.getMapDatabase().getMapFileInfo();

			// map file name
			TextView textView = (TextView) dialog.findViewById(R.id.infoMapFileViewName);
			textView.setText(this.mapView.getMapFile());

			// map file size
			textView = (TextView) dialog.findViewById(R.id.infoMapFileViewSize);
			textView.setText(formatFileSize(mapFileInfo.getFileSize()));

			// map file version
			textView = (TextView) dialog.findViewById(R.id.infoMapFileViewVersion);
			textView.setText(String.valueOf(mapFileInfo.getFileVersion()));

			// map file debug
			textView = (TextView) dialog.findViewById(R.id.infoMapFileViewDebug);
			if (mapFileInfo.isDebugFile()) {
				textView.setText(R.string.info_map_file_debug_yes);
			} else {
				textView.setText(R.string.info_map_file_debug_no);
			}

			// map file date
			textView = (TextView) dialog.findViewById(R.id.infoMapFileViewDate);
			Date date = new Date(mapFileInfo.getMapDate());
			textView.setText(DateFormat.getDateTimeInstance().format(date));

			// map file area
			textView = (TextView) dialog.findViewById(R.id.infoMapFileViewArea);
			BoundingBox boundingBox = mapFileInfo.getBoundingBox();
			textView.setText(boundingBox.getMinLatitude() + ", " + boundingBox.getMinLongitude() + " – \n"
					+ boundingBox.getMaxLatitude() + ", " + boundingBox.getMaxLongitude());

			// map file start position
			textView = (TextView) dialog.findViewById(R.id.infoMapFileViewStart);
			GeoPoint startPosition = mapFileInfo.getStartPosition();
			if (startPosition == null) {
				textView.setText(null);
			} else {
				textView.setText(startPosition.getLatitude() + ", " + startPosition.getLongitude());
			}

			// map file comment text
			textView = (TextView) dialog.findViewById(R.id.infoMapFileViewComment);
			textView.setText(mapFileInfo.getCommentText());
		} else {
			super.onPrepareDialog(id, dialog);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();

		// Read the default shared preferences
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

		// set the map settings
		this.mapView.getMapScaleBar().setShowMapScaleBar(preferences.getBoolean("showScaleBar", false));
		if (preferences.contains("mapViewMode")) {
			MapViewMode mapViewMode = Enum.valueOf(MapViewMode.class,
					preferences.getString("mapViewMode", MapView.DEFAULT_MAP_VIEW_MODE.name()));
			this.mapView.setMapViewMode(mapViewMode);
		}
		try {
			this.mapView.setTextScale(Float.parseFloat(preferences.getString("textScale", "1")));
		} catch (NumberFormatException e) {
			this.mapView.setTextScale(1);
		}

		// set the general settings
		if (preferences.getBoolean("fullscreen", false)) {
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
		} else {
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
		}
		if (preferences.getBoolean("wakeLock", false) && !this.wakeLock.isHeld()) {
			this.wakeLock.acquire();
		}

		boolean persistent = preferences.getBoolean("cachePersistence", false);
		int capacity = Math.min(preferences.getInt("cacheSize", FILE_SYSTEM_CACHE_SIZE_DEFAULT),
				FILE_SYSTEM_CACHE_SIZE_MAX);
		this.mapView.getFileSystemTileCache().setPersistent(persistent);
		this.mapView.getFileSystemTileCache().setCapacity(capacity);

		float moveSpeedFactor = Math.min(preferences.getInt("moveSpeed", MOVE_SPEED_DEFAULT), MOVE_SPEED_MAX) / 10f;
		this.mapView.getMapMover().setMoveSpeedFactor(moveSpeedFactor);

		// set the debug settings
		this.mapView.getFpsCounter().setFpsCounter(preferences.getBoolean("showFpsCounter", false));

		boolean drawTileFrames = preferences.getBoolean("drawTileFrames", false);
		boolean drawTileCoordinates = preferences.getBoolean("drawTileCoordinates", false);
		boolean highlightWaterTiles = preferences.getBoolean("highlightWaterTiles", false);
		DebugSettings debugSettings = new DebugSettings(drawTileCoordinates, drawTileFrames, highlightWaterTiles);
		this.mapView.setDebugSettings(debugSettings);

		// check if the file browser needs to be displayed
		if (!this.mapView.getMapViewMode().requiresInternetConnection() && this.mapView.getMapFile() == null) {
			startMapFilePicker();
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(BUNDLE_SHOW_MY_LOCATION, isShowMyLocationEnabled());
		outState.putBoolean(BUNDLE_CENTER_AT_FIRST_FIX, this.myLocationListener.isCenterAtFirstFix());
		outState.putBoolean(BUNDLE_SNAP_TO_LOCATION, this.snapToLocation);
	}

	/**
	 * Disables the "show my location" mode.
	 */
	void disableShowMyLocation() {
		if (this.showMyLocation) {
			this.showMyLocation = false;
			disableSnapToLocation(false);
			this.locationManager.removeUpdates(this.myLocationListener);
			if (this.circleOverlay != null) {
				this.mapView.getOverlays().remove(this.circleOverlay);
				this.mapView.getOverlays().remove(this.itemizedOverlay);
				this.circleOverlay = null;
				this.itemizedOverlay = null;
			}
			this.snapToLocationView.setVisibility(View.GONE);
		}
	}

	/**
	 * Disables the "snap to location" mode.
	 * 
	 * @param showToast
	 *            defines whether a toast message is displayed or not.
	 */
	void disableSnapToLocation(boolean showToast) {
		if (this.snapToLocation) {
			this.snapToLocation = false;
			this.snapToLocationView.setChecked(false);
			this.mapView.setClickable(true);
			if (showToast) {
				showToast(getString(R.string.snap_to_location_disabled));
			}
		}
	}

	/**
	 * Enables the "snap to location" mode.
	 * 
	 * @param showToast
	 *            defines whether a toast message is displayed or not.
	 */
	void enableSnapToLocation(boolean showToast) {
		if (!this.snapToLocation) {
			this.snapToLocation = true;
			this.mapView.setClickable(false);
			if (showToast) {
				showToast(getString(R.string.snap_to_location_enabled));
			}
		}
	}

	/**
	 * Returns the status of the "show my location" mode.
	 * 
	 * @return true if the "show my location" mode is enabled, false otherwise.
	 */
	boolean isShowMyLocationEnabled() {
		return this.showMyLocation;
	}

	/**
	 * Returns the status of the "snap to location" mode.
	 * 
	 * @return true if the "snap to location" mode is enabled, false otherwise.
	 */
	boolean isSnapToLocationEnabled() {
		return this.snapToLocation;
	}

	/**
	 * Displays a text message via the toast notification system. If a previous message is still visible, the previous
	 * message is first removed.
	 * 
	 * @param text
	 *            the text message to display
	 */
	void showToast(String text) {
		if (this.toast == null) {
			this.toast = Toast.makeText(this, text, Toast.LENGTH_LONG);
		} else {
			this.toast.cancel();
			this.toast.setText(text);
		}
		this.toast.show();
	}
}
