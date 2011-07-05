package org.mapsforge.applications.pc.advancedmapviewer;

import java.awt.BorderLayout;
import java.awt.List;
import java.awt.Scrollbar;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.border.Border;

/**
<?xml version="1.0" encoding="utf-8"?>
<PreferenceScreen xmlns:android="http://schemas.android.com/apk/res/android"
	android:title="@string/menu_preferences">
	<PreferenceCategory android:title="@string/preferences_map">
		<CheckBoxPreference android:title="@string/preferences_show_scale_bar"
			android:summary="@string/preferences_show_scale_bar_desc" android:key="showScaleBar" />
		<ListPreference android:title="@string/preferences_map_view_mode"
			android:summary="@string/preferences_map_view_mode_desc" android:entryValues="@array/preferences_map_view_mode_keys"
			android:entries="@array/preferences_map_view_mode_values" android:key="mapViewMode"
			android:defaultValue="@string/preferences_map_view_mode_default" />
		<ListPreference android:title="@string/preferences_text_scale" android:summary="@string/preferences_text_scale_desc"
			android:entryValues="@array/preferences_text_scale_keys" android:entries="@array/preferences_text_scale_values"
			android:key="textScale" android:defaultValue="@string/preferences_text_scale_default" />
	</PreferenceCategory>

	<PreferenceCategory android:title="@string/preferences_general">
		<CheckBoxPreference android:title="@string/preferences_fullscreen"
			android:summary="@string/preferences_fullscreen_desc" android:key="fullscreen" />
		<CheckBoxPreference android:title="@string/preferences_wake_lock"
			android:summary="@string/preferences_wake_lock_desc" android:key="wakeLock" />
		<CheckBoxPreference android:title="@string/preferences_cache_persistence"
			android:summary="@string/preferences_cache_persistence_desc" android:key="cachePersistence" />
		<org.mapsforge.applications.android.advancedmapviewer.CacheSizePreference
			android:title="@string/preferences_cache_size" android:summary="@string/preferences_cache_size_desc"
			android:key="cacheSize" />
		<org.mapsforge.applications.android.advancedmapviewer.MoveSpeedPreference
			android:title="@string/preferences_move_speed" android:summary="@string/preferences_move_speed_desc"
			android:key="moveSpeed" />
	</PreferenceCategory>

	<PreferenceCategory android:title="@string/preferences_debug">
		<CheckBoxPreference android:title="@string/preferences_show_fps_counter"
			android:summary="@string/preferences_show_fps_counter_desc" android:key="showFpsCounter" />
		<CheckBoxPreference android:title="@string/preferences_show_tile_frames"
			android:summary="@string/preferences_show_tile_frames_desc" android:key="showTileFrames" />
		<CheckBoxPreference android:title="@string/preferences_show_tile_coordinates"
			android:summary="@string/preferences_show_tile_coordinates_desc" android:key="showTileCoordinates" />
		<CheckBoxPreference android:title="@string/preferences_show_water_tiles"
			android:summary="@string/preferences_show_water_tiles_desc" android:key="showWaterTiles" />
	</PreferenceCategory>
</PreferenceScreen>
 */
public class EditPreferencesPC extends JPanel {

	private static final long serialVersionUID = 9090965786563170056L;
	
	static JFrame frame;
	static JLabel label;
	
	final String mapDesc = "TODO";
    final String generalDesc = "TODO";
    final String debugDesc = "TODO";
	
	public EditPreferencesPC() {
		super(new BorderLayout());
		//TODO
		JPanel mapPanel = createMapSettings();
		JPanel generalPanel = createGeneralSettings();
		JPanel debugPanel = createDebugSettings();
		
		label = new JLabel("Click the \"Show it!\" button"
                + " to bring up the selected dialog.",
                JLabel.CENTER);

		//Lay them out.
		Border padding = BorderFactory.createEmptyBorder(20,20,5,20);
		mapPanel.setBorder(padding);
		generalPanel.setBorder(padding);
		debugPanel.setBorder(padding);
		
		JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.addTab("Map settings", null, mapPanel, mapDesc); 
		tabbedPane.addTab("General settings", null, generalPanel, generalDesc);
		tabbedPane.addTab("Debug settings", null, debugPanel, debugDesc);
		
		add(tabbedPane, BorderLayout.CENTER);
		add(label, BorderLayout.PAGE_END);
		label.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
	}
	
	JCheckBox scale;
	List map;
	List font;
	
	private JPanel createMapSettings() {
		//final String scaleBar = "scale_bar";
		//final String mapMode = "map_mode";
		//final String fontSize = "font_size";
		scale = new JCheckBox("Map scale bar");
		map = new List(4, false);
		font = new List(5, false);		
		
	
		//Map Scale Bar (CHECK BOX)
		//Map Mode (LIST) {Canvas renderer, Mapnik (online), Osmarenderer(online), OpenCycleMap(online)}
		//Font Size (LIST) {tiny, small, normal, large, huge}
		return null;
	}
	
	JCheckBox full;
	JCheckBox stay;
	JCheckBox cache;
	Scrollbar storage;
	Scrollbar move;
	
	private JPanel createGeneralSettings() {
		//final String fullScreen = "full_screen";
		//final String stayAwake = "stay_awake";
		//final String cachePersistence = "cache_persistence";
		//final String externalStorage = "external_storage";
		//final String moveSpeed = "move_speed";
		
		full = new JCheckBox("Full screen mode");
		stay = new JCheckBox("Stay awake");
		cache = new JCheckBox("Cache persistence");
		//TODO parameter
		storage = new Scrollbar();
		move = new Scrollbar();
		//Full screen mode (CHECK BOX)
		//Stay awake (CHECK BOX)
		//Cache persistence (CHECK BOX)
		//External storage (SIDEBAR)
		//Move speed (SIDEBAR)
		return null;
	}
	
	JCheckBox rate;
	JCheckBox boundaries;
	JCheckBox coordinates;
	JCheckBox water;
	
	private JPanel createDebugSettings() {
		//final String frameRate = "frame_rate";
		//final String tileBoundaries = "tile_boundaries";
		//final String tileCoordinates = "tile_coordinates";
		//final String waterTiles = "water_tiles";
		
		rate = new JCheckBox("Frame rate");
		boundaries = new JCheckBox("Tile boundaries");
		coordinates = new JCheckBox("Tile coordinates");
		water = new JCheckBox("Water tiles");
		
		
		//Frame rate (CHECK BOX)
		//Tile boundaries (CHECK BOX)
		//Tile coordinates (CHECK BOX)
		//Water tiles (CHECK BOX)
		return null;
	}
}
