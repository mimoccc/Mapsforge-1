package org.mapsforge.applications.pc.advancedmapviewer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.filechooser.FileFilter;

import org.mapsforge.pc.maps.MapView;
import org.mapsforge.core.graphics.Canvas;

public class AdvancedMapViewerPC extends JFrame implements WindowListener {

	JFrame jFrame;
	protected Properties properties;
	private Canvas canvas;
	private MenuBar menuBar;
	
	
	/** Constructor */
	public AdvancedMapViewerPC() {
		
		this.addWindowListener(this);
		this.setTitle("Advanced Map Viewer");
		
		// JFrame
		this.jFrame = this;
		
		//Properties
		try{
			this.properties = new Properties();
			this.properties.load(new FileReader("res/values/strings.properties"));
		} catch (IOException e) {
			JOptionPane.showMessageDialog(this, "Could not read strings.properties!");
		}
		
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
		mainFrame.setSize(800, 600);
		mainFrame.setLocation(200,100);
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
	
	/** Preferences */
	protected void startPreferences() {
		JInternalFrame preferencesPane = new JInternalFrame(properties.getProperty("menu_preferences"), true, true, true, false);
		EditPreferencesPC editPanel = new EditPreferencesPC();
		preferencesPane.add(editPanel);
		preferencesPane.setVisible(true);
		this.add(preferencesPane);
	}
	
	/** Returns the properties from the mainFrame. 
	 * @return the properties 
	 */
	public Properties getProperties() {
		return properties;
	}
}
