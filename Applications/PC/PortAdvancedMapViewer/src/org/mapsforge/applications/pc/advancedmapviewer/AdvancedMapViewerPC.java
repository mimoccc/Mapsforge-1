package org.mapsforge.applications.pc.advancedmapviewer;

import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.filechooser.FileFilter;

import org.mapsforge.pc.maps.MapController;
import org.mapsforge.pc.maps.MapView;
import org.mapsforge.pc.maps.MapViewMode;
import org.mapsforge.applications.android.advancedmapviewer.R;
import org.mapsforge.core.graphics.Canvas;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.location.LocationManager;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.view.WindowManager;
import android.widget.ImageView;

public class AdvancedMapViewerPC extends JFrame implements WindowListener {

	JFrame jFrame;
	protected Properties propertiesStrings, propertiesSettings;
	private Canvas canvas;
	private MenuBar menuBar;
	
	
	/** Constructor */
	public AdvancedMapViewerPC() {

		//Properties
		try{
			this.propertiesSettings = new Properties();
			this.propertiesSettings.load(new FileReader("res/config/config.properties"));
			this.propertiesStrings = new Properties();
			this.propertiesStrings.load(new FileReader("res/values/strings.properties"));
		} catch (IOException e) {
			JOptionPane.showMessageDialog(this, "Could not read properties files!");
		}
		
		this.addWindowListener(this);
		this.setTitle(propertiesStrings.getProperty("application_name"));
		
		// JFrame
		this.jFrame = this;

		// Size
		int height = Integer.parseInt(propertiesSettings.getProperty("application_size_height"));
		int width = Integer.parseInt(propertiesSettings.getProperty("application_size_width"));
		this.setSize(height, width);
		
		//Position
		int xPos = Integer.parseInt(propertiesSettings.getProperty("application_position_x"));
		int yPos = Integer.parseInt(propertiesSettings.getProperty("application_position_y"));
		this.setLocation(xPos, yPos);
		
		// Menubar
		this.menuBar = new MenuBar(this);
		this.setJMenuBar(menuBar);


//		this.add(new Canvas());
	}
	
	
	/**
	 * The Main Method to run the Advanced Map Viewer
	 */
	public static void main(String[] args) {
		AdvancedMapViewerPC mainFrame = new AdvancedMapViewerPC();
		mainFrame.setVisible(true);
	}
	
	/** WindowListener */
	@Override public void windowActivated(WindowEvent e) {}
	@Override public void windowClosed(WindowEvent e) {	close();	}
	@Override public void windowClosing(WindowEvent e) {	close();	}
	@Override public void windowDeactivated(WindowEvent e) {}
	@Override public void windowDeiconified(WindowEvent e) {}
	@Override public void windowIconified(WindowEvent e) {}
	@Override public void windowOpened(WindowEvent e) {}

	/** Cleaning up before exit */
	protected void close() {
		//TODO: Cleaning up
		try {
			propertiesSettings.store(new FileWriter("res/config/config.properties"), null);
		} catch (IOException e) {
			JOptionPane.showMessageDialog(this, "Could not write to config.properties!");
		}
		System.exit(0);
	}

	/** File Browser */
	protected void startFileBrowser() {
		// set the FileDisplayFilter
		FilePickerPC.setFileDisplayFilter(new FileFilter() {
			@Override
			public boolean accept(File file) {
				// accept only readable files
				if (file.canRead()) {
					if (file.isDirectory()) {
						// accept all directories
						return true;
					} else if (file.isFile() && file.getName().endsWith(".map")) {
						// accept all files with a ".map" extension
						return true;
					}
				}
				return false;
			}

			@Override
			public String getDescription() {
				// TODO Auto-generated method stub
				return null;
			}
		});

		// set the FileSelectFilter
		FilePickerPC.setFileSelectFilter(new FileFilter() {
			@Override
			public boolean accept(File file) {
				// accept only valid map files
				return MapView.isValidMapFile(file.getAbsolutePath());
			}

			@Override
			public String getDescription() {
				// TODO Auto-generated method stub
				return null;
			}
		});

		//TODO start the FilePicker
		SwingUtilities.invokeLater(new Runnable() {
        public void run() {
            //Turn off metal's use of bold fonts
            UIManager.put("swing.boldMetal", Boolean.FALSE); 
    		FilePickerPC.createFileChooser(jFrame);
        }
    });
		//startActivityForResult(new Intent(this, FilePicker.class), SELECT_MAP_FILE);
	}
	
	/** Returns the strings properties from the mainFrame. 
	 * @return the propertiesStrings 
	 */
	public Properties getPropertiesStrings() {
		return propertiesStrings;
	}
	
	/** Returns the settings properties from the mainFrame. 
	 * @return the propertiesSettings 
	 */
	public Properties getPropertiesSettings() {
		return propertiesSettings;
	}
	
	
	/** MapView Configuration
	 *  onCreate --> onResume
	 */
	MapView mapView;
	MapController mapController;
	MapViewMode mapViewMode;
	
	public void onCreate() {
		// set up the layout views
		this.mapView = null;

		configureMapView();

		// get the pointers to different system services
		//<- Removed: Android Specific ->

		// set up the paint objects for the location overlay
		//<- Removed: GPS Specific ->

		onResume();
	}
	
	public void onResume() {
		// Read the default shared preferences
		//this.preferences = PreferenceManager.getDefaultSharedPreferences(this);

		// set the map settings
		this.mapView.setScaleBar(false);
		this.mapViewMode = Enum.valueOf(MapViewMode.class, org.mapsforge.pc.maps.MapViewMode.CANVAS_RENDERER.name());
		this.mapView.setMapViewMode(this.mapViewMode);
		this.mapView.setTextScale(Float.parseFloat("1"));


		// set the general settings
		//<- Removed: Android Specific ->

		// set the debug settings
		this.mapView.setFpsCounter(false);
		this.mapView.setTileFrames(false);
		this.mapView.setTileCoordinates(false);
		this.mapView.setWaterTiles(false);

		// check if the file browser needs to be displayed
		if (!this.mapView.getMapViewMode().requiresInternetConnection()
				&& !this.mapView.hasValidMapFile()) {
			startFileBrowser();
		}
	}
	
	private void configureMapView() {
		// configure the MapView and activate the zoomLevel buttons
		this.mapView.setClickable(true);
		this.mapView.setBuiltInZoomControls(true);
		this.mapView.setFocusable(true);

		// set the localized text fields
		//this.mapView.setText(TextField.KILOMETER, getString(R.string.unit_symbol_kilometer));
		//this.mapView.setText(TextField.METER, getString(R.string.unit_symbol_meter));

		// get the map controller for this MapView
		this.mapController = this.mapView.getController();
	}
}
