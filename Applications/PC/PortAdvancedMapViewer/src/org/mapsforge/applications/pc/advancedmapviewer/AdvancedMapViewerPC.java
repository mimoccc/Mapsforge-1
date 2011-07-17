package org.mapsforge.applications.pc.advancedmapviewer;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

import org.mapsforge.pc.maps.MapActivity;
import org.mapsforge.pc.maps.MapController;
import org.mapsforge.pc.maps.MapView;
import org.mapsforge.pc.maps.MapView.TextField;
import org.mapsforge.pc.maps.MapViewMode;
import org.mapsforge.pc.maps.TileMemoryCardCache;
import org.mapsforge.pc.maps.TileRAMCache;
import org.mapsforge.core.graphics.Canvas;



public class AdvancedMapViewerPC extends JFrame implements WindowListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4127875987929158484L;
	JFrame jFrame;
	protected Properties propertiesStrings, propertiesSettings;
	private Canvas canvas;
	private MenuBar menuBar;
	private FilePickerPC filePicker;
	private ArrayList<MapView> mapViews = new ArrayList<MapView>(2);
	
	
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
		
		//FilePicker
		this.add(filePicker = new FilePickerPC());

		//Configure
		this.onCreate(10);
		try {
			this.startFileBrowser();
		} catch (IOException e) {
			e.printStackTrace();
		}
		//this.onCreate(height, width);
		//this.add();
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

	/** File Browser 
	 * @throws IOException */
	protected void startFileBrowser() throws IOException {
		filePicker.configure();
		String file = filePicker.openMap();
		
		startActivityForResult(file);
	}
	
	BufferedImage image = null;
	TileRAMCache tileRAMCache = new TileRAMCache(16);
	TileMemoryCardCache tileMemoryCardCache = new TileMemoryCardCache("res" + File.separatorChar + "cache",
			500);
	
	public void startActivityForResult(String file) throws IOException {
		/*
		Bitmap bitmap = new Bitmap(new BufferedImage(400, 400, BufferedImage.TYPE_INT_RGB));
		
		MapDatabase db = new MapDatabase();
		db.openFile(file);

		CanvasRenderer gen = new CanvasRenderer();
		
		
		gen.setDatabase(db);
		gen.setupRenderer(bitmap);

		gen.start();
		image = gen.canvas.mBufferedImage;
		 */
		//image = ImageIO.read(new File("kanon0.jpg"));
		
		 if(mapView != null) {
			mapView.setMapFile(file);
		}
		else {
			System.out.println("Error");
		}
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
	
	public void onCreate(int id) {
		// set up the layout views
		this.mapView = new MapView(id);

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
		//	this.mapView.setScaleBar(false);
		this.mapViewMode = Enum.valueOf(MapViewMode.class, org.mapsforge.pc.maps.MapViewMode.CANVAS_RENDERER.name());
		this.mapView.setTextScale(1);


		// set the general settings
		//<- Removed: Android Specific ->

		// set the debug settings
		//this.mapView.setFpsCounter(false);
		//this.mapView.setTileFrames(false);
		//this.mapView.setTileCoordinates(false);
		//this.mapView.setWaterTiles(false);

		// check if the file browser needs to be displayed
		//if (!this.mapView.getMapViewMode().requiresInternetConnection()
		//		&& !this.mapView.hasValidMapFile()) {
			//startFileBrowser();
		//}
	}
	
	private void configureMapView() {
		//TODO configure the MapView and activate the zoomLevel buttons
		//this.mapView.setClickable(true);
		//this.mapView.setBuiltInZoomControls(true);
		//this.mapView.setFocusable(true);

		// set the localized text fields
		this.mapView.setText(TextField.KILOMETER, "KiloMeter");
		this.mapView.setText(TextField.METER, "Meter");

		// get the map controller for this MapView
		this.mapController = this.mapView.getController();
	}
	
}
