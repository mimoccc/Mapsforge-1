package org.mapsforge.applications.pc.advancedmapviewer;

import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

import javax.swing.ImageIcon;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;

import org.mapsforge.core.graphics.Bitmap.CompressFormat;

/**
 * Menu bar for the application preferences.
 */
public class MenuBar extends JMenuBar implements ActionListener {

	private static final long serialVersionUID = 1L;
//	private final String MENUINFOPATH = "file://" + System.getProperty("user.dir") + "/assets/info.xml";
	private final String MENUINFOPATH = System.getProperty("user.dir") + "/assets/info.xml";
	private Properties propertiesStrings, propertiesSettings;
	private AdvancedMapViewerPC parentFrame;
	private JMenu menuInfo, menuPosition, menuScreenshot;
	private JMenu menuRenderTheme, menuMapFile, menuZoom;
	private PreferencesMenu menuPreferences;
	private JMenuItem itemMapFile;
	private JMenuItem itemAbout, itemMapinfo;
	private JMenuItem itemFollowGPS, itemLastKnown, itemEnterCoordiantes,
			itemMapFileCenter;
	private JMenuItem itemJPEG, itemPNG;
	private JMenuItem itemMapnik, itemOsmarenderer, itemSelectXMLFile;
	private JMenuItem itemZoomOut, itemZoomIn;

	/**
	 * Constructor
	 * 
	 * @param p
	 *            parentFrame
	 */
	public MenuBar(AdvancedMapViewerPC p) {

		/** ParenFrame */
		this.parentFrame = p;

		/** Properties */
		this.propertiesStrings = parentFrame.getPropertiesStrings();
		this.propertiesSettings = parentFrame.getPropertiesSettings();

		// Menus
		this.menuInfo = new JMenu(propertiesStrings.getProperty("menu_info"));
		this.menuInfo.setIcon(new ImageIcon(
				"res/drawable/ic_menu_info_details.png"));
		this.menuInfo.setHorizontalTextPosition(SwingConstants.CENTER);
		this.menuInfo.setVerticalTextPosition(SwingConstants.BOTTOM);
		this.menuInfo.setMnemonic('i');
		this.menuPosition = new JMenu(
				propertiesStrings.getProperty("menu_position"));
		this.menuPosition.setIcon(new ImageIcon(
				"res/drawable/ic_menu_mylocation.png"));
		this.menuPosition.setHorizontalTextPosition(SwingConstants.CENTER);
		this.menuPosition.setVerticalTextPosition(SwingConstants.BOTTOM);
		this.menuPosition.setMnemonic('p');
		this.menuScreenshot = new JMenu(
				propertiesStrings.getProperty("menu_screenshot"));
		this.menuScreenshot.setIcon(new ImageIcon(
				"res/drawable/ic_menu_camera.png"));
		this.menuScreenshot.setHorizontalTextPosition(SwingConstants.CENTER);
		this.menuScreenshot.setVerticalTextPosition(SwingConstants.BOTTOM);
		this.menuScreenshot.setMnemonic('s');

		this.menuPreferences = new PreferencesMenu(parentFrame,
				propertiesStrings.getProperty("menu_preferences"));
		this.menuPreferences.setIcon(new ImageIcon(
				"res/drawable/ic_menu_preferences.png"));
		this.menuPreferences.setHorizontalTextPosition(SwingConstants.CENTER);
		this.menuPreferences.setVerticalTextPosition(SwingConstants.BOTTOM);
		this.menuPreferences.setMnemonic('p');

		this.menuRenderTheme = new JMenu(
				propertiesStrings.getProperty("menu_render_theme"));
		this.menuRenderTheme.setIcon(new ImageIcon("res/drawable/folder.png"));
		this.menuRenderTheme.setHorizontalTextPosition(SwingConstants.CENTER);
		this.menuRenderTheme.setVerticalTextPosition(SwingConstants.BOTTOM);
		this.menuRenderTheme.setMnemonic('r');
		this.menuMapFile = new JMenu(
				propertiesStrings.getProperty("menu_mapfile"));
		this.menuMapFile.setIcon(new ImageIcon("res/drawable/folder.png"));
		this.menuMapFile.setHorizontalTextPosition(SwingConstants.CENTER);
		this.menuMapFile.setVerticalTextPosition(SwingConstants.BOTTOM);
		this.menuMapFile.setMnemonic('m');
		this.menuZoom = new JMenu(
				propertiesStrings.getProperty("preferences_zoom"));
		this.menuZoom.setIcon(new ImageIcon("res/drawable/zoom_controls.png"));
		this.menuZoom.setHorizontalTextPosition(SwingConstants.CENTER);
		this.menuZoom.setVerticalTextPosition(SwingConstants.BOTTOM);
		this.menuZoom.setMnemonic('z');

		this.add(menuInfo);
		this.add(menuPosition);
		this.add(menuScreenshot);
		this.add(menuPreferences);
		this.add(menuRenderTheme);
		this.add(menuMapFile);
		this.add(menuZoom);

		// Menuitems
		this.itemMapinfo = new JMenuItem(
				propertiesStrings.getProperty("menu_info_map_file"), 'm');
		this.itemMapinfo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1,
				0));
		this.itemAbout = new JMenuItem(
				propertiesStrings.getProperty("menu_info_about"), 'a');
		this.itemAbout
				.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0));

		this.itemFollowGPS = new JMenuItem(
				propertiesStrings
						.getProperty("menu_position_follow_gps_signal"),
				'f');
		this.itemFollowGPS.setAccelerator(KeyStroke.getKeyStroke(
				KeyEvent.VK_F3, 0));
		this.itemFollowGPS.setEnabled(false);
		this.itemLastKnown = new JMenuItem(
				propertiesStrings.getProperty("menu_position_last_known"), 'l');
		this.itemLastKnown.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F,
				InputEvent.ALT_DOWN_MASK));
		this.itemEnterCoordiantes = new JMenuItem(
				propertiesStrings
						.getProperty("menu_position_enter_coordinates"),
				'e');
		this.itemEnterCoordiantes.setAccelerator(KeyStroke.getKeyStroke(
				KeyEvent.VK_F4, 0));
		this.itemMapFileCenter = new JMenuItem(
				propertiesStrings.getProperty("menu_position_map_file_center"),
				'c');
		this.itemMapFileCenter.setAccelerator(KeyStroke.getKeyStroke(
				KeyEvent.VK_F5, 0));

		this.itemJPEG = new JMenuItem(
				propertiesStrings.getProperty("menu_screenshot_jpeg"), 'j');
		this.itemJPEG.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0));
		this.itemPNG = new JMenuItem(
				propertiesStrings.getProperty("menu_screenshot_png"), 'p');
		this.itemPNG.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0));

		this.itemMapnik = new JMenuItem(
				propertiesStrings.getProperty("menu_render_theme_mapnik"), 'm');
		this.itemMapnik.setAccelerator(KeyStroke
				.getKeyStroke(KeyEvent.VK_F9, 0));
		this.itemOsmarenderer = new JMenuItem(
				propertiesStrings.getProperty("menu_render_theme_osmarender"),
				'o');
		this.itemOsmarenderer.setAccelerator(KeyStroke.getKeyStroke(
				KeyEvent.VK_F10, 0));
		this.itemSelectXMLFile = new JMenuItem(
				propertiesStrings.getProperty("menu_render_theme_select_file"),
				'x');
		this.itemSelectXMLFile.setAccelerator(KeyStroke.getKeyStroke(
				KeyEvent.VK_F11, 0));

		this.itemMapFile = new JMenuItem(
				propertiesStrings.getProperty("menu_mapfile_select_file"), 'f');
		this.itemMapFile.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F12,
				0));

		this.itemZoomOut = new JMenuItem(
				propertiesStrings.getProperty("preferences_zoom_out"));
//		this.itemZoomOut.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, 0));
		this.itemZoomOut.setToolTipText(propertiesStrings
				.getProperty("preferences_zoom_out_desc"));
		this.itemZoomIn = new JMenuItem(
				propertiesStrings.getProperty("preferences_zoom_in"));
//		this.itemZoomIn.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, 0));
		this.itemZoomIn.setToolTipText(propertiesStrings
				.getProperty("preferences_zoom_in_desc"));

		this.menuInfo.add(itemMapinfo);
		this.menuInfo.add(itemAbout);

		this.menuPosition.add(itemFollowGPS);
		this.menuPosition.add(itemLastKnown);
		this.menuPosition.add(itemEnterCoordiantes);
		this.menuPosition.add(itemMapFileCenter);

		this.menuScreenshot.add(itemJPEG);
		this.menuScreenshot.add(itemPNG);

		this.menuRenderTheme.add(itemMapnik);
		this.menuRenderTheme.add(itemOsmarenderer);
		this.menuRenderTheme.add(itemSelectXMLFile);

		this.menuMapFile.add(itemMapFile);

		this.menuZoom.add(itemZoomOut);
		this.menuZoom.add(itemZoomIn);

		// Listener
		this.itemMapinfo.addActionListener(this);
		this.itemAbout.addActionListener(this);
		this.itemFollowGPS.addActionListener(this);
		this.itemEnterCoordiantes.addActionListener(this);
		this.itemMapFileCenter.addActionListener(this);
		this.itemJPEG.addActionListener(this);
		this.itemPNG.addActionListener(this);
		this.itemMapnik.addActionListener(this);
		this.itemOsmarenderer.addActionListener(this);
		this.itemSelectXMLFile.addActionListener(this);
		this.itemMapFile.addActionListener(this);

	}

	@Override
	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		// TODO: Menu functions
		if (cmd.equals(propertiesStrings.getProperty("menu_info_map_file"))) {
			String mapFilePath = propertiesSettings.getProperty("default_map");
			MapFilePropertiesPane.showInfo(this, propertiesStrings, mapFilePath);
			
		} else if (cmd.equals(propertiesStrings.getProperty("menu_info_about"))) {
			try {
				//Open the MENUINFOPATH-URI in the default browser 
				Desktop desktop = Desktop.getDesktop();
				URI uri = new URI(MENUINFOPATH);
				desktop.browse( uri );
			} catch (URISyntaxException e1) {
				JOptionPane.showMessageDialog(this, e1.getMessage());
			} catch (IOException e1) {
				JOptionPane.showMessageDialog(this, e1.getMessage());
			}
			
		} else if (cmd.equals(propertiesStrings
				.getProperty("menu_position_follow_gps_signal"))) {

			
		} else if (cmd.equals(propertiesStrings
				.getProperty("menu_position_last_known"))) {

			
		} else if (cmd.equals(propertiesStrings
				.getProperty("menu_position_enter_coordinates"))) {

			
		} else if (cmd.equals(propertiesStrings
				.getProperty("menu_position_map_file_center"))) {

			
		} else if (cmd.equals(propertiesStrings
				.getProperty("menu_screenshot_jpeg"))) {
			parentFrame.captureScreenshotAsync(this, CompressFormat.JPG, 
				Integer.parseInt(propertiesSettings
				.getProperty("preferences_screenshot_quality")));
			
		} else if (cmd.equals(propertiesStrings
				.getProperty("menu_screenshot_png"))) {
			parentFrame.captureScreenshotAsync(this, CompressFormat.PNG, 
					Integer.parseInt(propertiesSettings
					.getProperty("preferences_screenshot_quality")));
			
		} else if (cmd.equals(propertiesStrings
				.getProperty("menu_render_theme_mapnik"))) {

			
		} else if (cmd.equals(propertiesStrings
				.getProperty("menu_render_theme_osmarender"))) {

			
		} else if (cmd.equals(propertiesStrings
				.getProperty("menu_render_theme_select_file"))) {
			try {
				parentFrame.startRenderThemeFileBrowser();
			} catch (IOException e1) {
				JOptionPane.showMessageDialog(this, e1.getMessage());
			}
			
		} else if (cmd.equals(propertiesStrings
				.getProperty("menu_mapfile_select_file"))) {
			try {
				parentFrame.startMapFileBrowser();
			} catch (IOException e1) {
				JOptionPane.showMessageDialog(this, e1.getMessage());
			}
			
		} else if (cmd.equals("Exit")) {
			parentFrame.close();

		} else if (cmd.equals("About this software")) {

		}

	}

}
