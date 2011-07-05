package org.mapsforge.applications.pc.advancedmapviewer;

import java.awt.BorderLayout;

import javax.swing.BorderFactory;
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
	
	private JPanel createMapSettings() {
		
		return null;
	}
	
	private JPanel createGeneralSettings() {
		
		return null;
	}
	
	private JPanel createDebugSettings() {
		
		return null;
	}
}
