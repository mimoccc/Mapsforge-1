package org.mapsforge.applications.pc.advancedmapviewer;

import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.mapsforge.pc.maps.MapView;
import org.mapsforge.pc.maps.TileMemoryCardCache;
import org.mapsforge.pc.maps.TileRAMCache;



public class AdvancedMapViewerPC extends JFrame implements WindowListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4127875987929158484L;
	JFrame jFrame;
	protected Properties propertiesStrings, propertiesSettings;
	private MenuBar menuBar;
	private FilePickerPC filePicker;
	MapView mapView;
	
	
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
		this.setSize(width, height);
		
		//Position
		int xPos = Integer.parseInt(propertiesSettings.getProperty("application_position_x"));
		int yPos = Integer.parseInt(propertiesSettings.getProperty("application_position_y"));
		this.setLocation(xPos, yPos);
		
		// Menubar
		this.menuBar = new MenuBar(this);
		this.setJMenuBar(menuBar);
		
		//FilePicker
		this.add(filePicker = new FilePickerPC());
		
		//Map View Configure
		this.mapView = new MapView(10);
		this.add(mapView);
		String defaultMap = propertiesSettings.getProperty("default_map");
		try {
			startActivityForResult(defaultMap);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		/*try {
			this.startFileBrowser();
		} catch (IOException e) {
			e.printStackTrace();
		}*/
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
}
