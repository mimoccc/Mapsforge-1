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
package org.mapsforge.applications.pc.advancedmapviewer;

import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.mapsforge.core.graphics.Bitmap.CompressFormat;
import org.mapsforge.pc.maps.MapView;


/**
 * A map application which uses the features from the mapsforge map library. The
 * map can be centered to the current location. A simple file browser for
 * selecting the map file is also included. Some preferences can be adjusted and
 * screenshots of the map may be taken in different image formats.
 */
public class AdvancedMapViewerPC extends JFrame implements WindowListener, ComponentListener {

	private static final String SCREENSHOT_DIRECTORY = "Pictures";
	private static final String SCREENSHOT_FILE_NAME = "Map screenshot";
	
	protected Properties propertiesStrings, propertiesSettings;
	private static final long serialVersionUID = -4127875987929158484L;
	private MenuBar menuBar;
	private FilePickerPC filePicker;
	protected MapView mapView;	
	

	/** Constructor */
	public AdvancedMapViewerPC() {

		// Properties
		try {
			this.propertiesSettings = new Properties();
			this.propertiesSettings.load(new FileReader(
					"res/config/config.properties"));
			this.propertiesStrings = new Properties();
			this.propertiesStrings.load(new FileReader(
					"res/values/strings.properties"));
		} catch (IOException e) {
			JOptionPane.showMessageDialog(this,
					"Could not read properties files!");
		}

		this.addWindowListener(this);
		this.addComponentListener(this);
		this.setTitle(propertiesStrings.getProperty("application_name"));

		// Size
		int height = Integer.parseInt(propertiesSettings
				.getProperty("application_size_height"));
		int width = Integer.parseInt(propertiesSettings
				.getProperty("application_size_width"));
		this.setSize(width, height);

		// Position
		int xPos = Integer.parseInt(propertiesSettings
				.getProperty("application_position_x"));
		int yPos = Integer.parseInt(propertiesSettings
				.getProperty("application_position_y"));
		this.setLocation(xPos, yPos);

		// Menubar
		this.menuBar = new MenuBar(this);
		this.setJMenuBar(menuBar);

		// FilePicker
		this.filePicker = new FilePickerPC();
		this.add(filePicker);

		// Map View Configuration
		this.mapView = new MapView(10, propertiesSettings);
		this.add(mapView);

		// Last used map file loading
		String defaultMap = propertiesSettings.getProperty("default_map");
		mapView.setMapFile(defaultMap);

	}

	/**
	 * The Main Method to run the Advanced Map Viewer
	 * 
	 * @param args
	 *            not used
	 */
	public static void main(String[] args) {
		AdvancedMapViewerPC mainFrame = new AdvancedMapViewerPC();
		mainFrame.setVisible(true);
	}

	// WindowListener
	@Override
	public void componentResized(ComponentEvent e) {
		propertiesSettings.setProperty("application_size_height", ""+this.getHeight());
		propertiesSettings.setProperty("application_size_width", ""+this.getWidth());
		propertiesSettings.setProperty("map_size_height", ""+(this.getHeight()-103));
		propertiesSettings.setProperty("map_size_width", ""+this.getWidth());
		if (this.mapView != null)
			this.mapView.onSizeChanged(this.getWidth(), (this.getHeight()-103));
	}

	@Override
	public void componentMoved(ComponentEvent e) {
		propertiesSettings.setProperty("application_position_x", ""+this.getX());
		propertiesSettings.setProperty("application_position_y", ""+this.getY());
	}

	@Override
	public void componentShown(ComponentEvent e) {
	}

	@Override
	public void componentHidden(ComponentEvent e) {
	}

	@Override
	public void windowActivated(WindowEvent e) {
	}

	@Override
	public void windowClosed(WindowEvent e) {
		close();
	}

	@Override
	public void windowClosing(WindowEvent e) {
		close();
	}

	@Override
	public void windowDeactivated(WindowEvent e) {
	}

	@Override
	public void windowDeiconified(WindowEvent e) {
	}

	@Override
	public void windowIconified(WindowEvent e) {
	}

	@Override
	public void windowOpened(WindowEvent e) {
	}

	/** Cleaning up before exit */
	protected void close() {
		// TODO: Cleaning up
		try {
			propertiesSettings.store(new FileWriter(
					"res/config/config.properties"), null);
		} catch (IOException e) {
			JOptionPane.showMessageDialog(this,
					"Could not write to config.properties!");
		}
		System.exit(0);
	}

	/**
	 * File Browser for selecting a map file
	 * 
	 * @throws IOException
	 */
	protected void startMapFileBrowser() throws IOException {
		filePicker.configure();
		String file = filePicker.openMap();

		if (mapView != null) {
			mapView.setMapFile(file);
			propertiesSettings.setProperty("default_map", file);
		} else {
			System.err.println("Internal Error: MapView is not found!");
		}
	}

	/**
	 * File Browser for selecting a render theme
	 * 
	 * @throws IOException
	 */
	protected void startRenderThemeFileBrowser() throws IOException {
		// TODO: Render Theme File Browser
	}
	
	/**
	 * Makes a screenshot of the currently visible map and saves it as compressed image. 
	 * Scale bar, overlays, menus and the title bar are not included in the screenshot.
	 * 
	 * @param format
	 *            the file format of the compressed image.
	 * @param quality
	 *            value from 0 (low) to 100 (high). Has no effect on some formats like PNG.
	 */
	public void captureScreenshotAsync(final JComponent GUI, final CompressFormat format,final int quality) {
		new Thread() {
			@Override
			public void run() {
				try {
					File path = new File(System.getProperty("user.dir"),
							SCREENSHOT_DIRECTORY);
					// make sure the Pictures directory exists
					if (!path.exists() && !path.mkdirs()) {
						JOptionPane.showMessageDialog(GUI, "Could not create screenshot directory");
						return;
					}

					// assemble the complete name for the screenshot file
					String fileName = path.getAbsolutePath() + File.separatorChar
							+ SCREENSHOT_FILE_NAME + "." + format.name().toLowerCase();

					if (AdvancedMapViewerPC.this.mapView.makeScreenshot(format,
							quality, fileName)) {
						// success
						JOptionPane.showMessageDialog(GUI, fileName);
					} else {
						// failure
						JOptionPane.showMessageDialog(GUI, "Screenshot could not be saved:\n" + fileName);
					}
				} catch (IOException e) {
					JOptionPane.showMessageDialog(GUI, e.getLocalizedMessage());
				}
			}
			
		}.start();
	}

	/**
	 * Returns the strings properties from the mainFrame.
	 * 
	 * @return the propertiesStrings
	 */
	public Properties getPropertiesStrings() {
		return propertiesStrings;
	}

	/**
	 * Returns the settings properties from the mainFrame.
	 * 
	 * @return the propertiesSettings
	 */
	public Properties getPropertiesSettings() {
		return propertiesSettings;
	}


}
