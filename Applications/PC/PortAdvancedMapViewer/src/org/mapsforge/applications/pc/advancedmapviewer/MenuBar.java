package org.mapsforge.applications.pc.advancedmapviewer;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Properties;

import javax.swing.ImageIcon;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;

public class MenuBar extends JMenuBar implements ActionListener {

	private Properties propertiesStrings, propertiesSettings;
	private AdvancedMapViewerPC parentFrame;
	private JMenu menuInfo, menuPosition, menuScreenshot;
	private JMenu menuRenderTheme, menuMapFile;
	private PreferencesMenu menuPreferences;
	private JMenuItem itemMapFile;
	private JMenuItem itemAbout, itemMapinfo;
	private JMenuItem itemFollowGPS, itemLastKnown, itemEnterCoordiantes, itemMapFileCenter;
	private JMenuItem itemJPEG, itemPNG;
	private JMenuItem itemMapnik, itemOsmarenderer, itemSelectXMLFile; 
	
	
	/** Constructor 
	 * @param parentFrame */
	public MenuBar(AdvancedMapViewerPC p) {
		
		/** ParenFrame */
		parentFrame = p;
		
		/** Properties */
		propertiesStrings = parentFrame.getPropertiesStrings();
		propertiesSettings = parentFrame.getPropertiesSettings();
		
		//Menus
		menuInfo = new JMenu(propertiesStrings.getProperty("menu_info"));
		menuInfo.setIcon(new ImageIcon("res/drawable/ic_menu_info_details.png"));
		menuInfo.setHorizontalTextPosition(SwingConstants.CENTER);
		menuInfo.setVerticalTextPosition(SwingConstants.BOTTOM);
		menuInfo.setMnemonic('i');
		menuPosition = new JMenu(propertiesStrings.getProperty("menu_position"));
		menuPosition.setIcon(new ImageIcon("res/drawable/ic_menu_mylocation.png"));
		menuPosition.setHorizontalTextPosition(SwingConstants.CENTER);
		menuPosition.setVerticalTextPosition(SwingConstants.BOTTOM);
		menuPosition.setMnemonic('p');
		menuScreenshot = new JMenu(propertiesStrings.getProperty("menu_screenshot"));
		menuScreenshot.setIcon(new ImageIcon("res/drawable/ic_menu_camera.png"));
		menuScreenshot.setHorizontalTextPosition(SwingConstants.CENTER);
		menuScreenshot.setVerticalTextPosition(SwingConstants.BOTTOM);
		menuScreenshot.setMnemonic('s');
		
		menuPreferences = new PreferencesMenu(parentFrame, propertiesStrings.getProperty("menu_preferences"));
		menuPreferences.setIcon(new ImageIcon("res/drawable/ic_menu_preferences.png"));
		menuPreferences.setHorizontalTextPosition(SwingConstants.CENTER);
		menuPreferences.setVerticalTextPosition(SwingConstants.BOTTOM);
		menuPreferences.setMnemonic('p');
		
		menuRenderTheme = new JMenu(propertiesStrings.getProperty("menu_render_theme"));
		menuRenderTheme.setIcon(new ImageIcon("res/drawable/folder.png"));
		menuRenderTheme.setHorizontalTextPosition(SwingConstants.CENTER);
		menuRenderTheme.setVerticalTextPosition(SwingConstants.BOTTOM);
		menuRenderTheme.setMnemonic('r');
		menuMapFile = new JMenu(propertiesStrings.getProperty("menu_mapfile"));
		menuMapFile.setIcon(new ImageIcon("res/drawable/folder.png"));
		menuMapFile.setHorizontalTextPosition(SwingConstants.CENTER);
		menuMapFile.setVerticalTextPosition(SwingConstants.BOTTOM);
		menuMapFile.setMnemonic('m');
		
		this.add(menuInfo);
		this.add(menuPosition);
		this.add(menuScreenshot);
		this.add(menuPreferences);
		this.add(menuRenderTheme);
		this.add(menuMapFile);


		//Menuitems
		itemMapinfo = new JMenuItem(propertiesStrings.getProperty("menu_info_map_file"),'m');
		itemMapinfo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1,0));
		itemAbout = new JMenuItem(propertiesStrings.getProperty("menu_info_about"),'a');
		itemAbout.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F2,0));
		
		itemFollowGPS = new JMenuItem(propertiesStrings.getProperty("menu_position_follow_gps_signal"),'f');
		itemFollowGPS.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F3,0));
		itemFollowGPS.setEnabled(false);
		itemLastKnown = new JMenuItem(propertiesStrings.getProperty("menu_position_last_known"),'l');
		itemLastKnown.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.ALT_DOWN_MASK));
		itemEnterCoordiantes = new JMenuItem(propertiesStrings.getProperty("menu_position_enter_coordinates"),'e');
		itemEnterCoordiantes.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F4, 0));
		itemMapFileCenter = new JMenuItem(propertiesStrings.getProperty("menu_position_map_file_center"),'c');
		itemMapFileCenter.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
		
		itemJPEG = new JMenuItem(propertiesStrings.getProperty("menu_screenshot_jpeg"),'j');
		itemJPEG.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F6,0));
		itemPNG = new JMenuItem(propertiesStrings.getProperty("menu_screenshot_png"),'p');
		itemPNG.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F7,0));
		
		itemMapnik = new JMenuItem(propertiesStrings.getProperty("menu_render_theme_mapnik"),'m');
		itemMapnik.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F9,0));
		itemOsmarenderer = new JMenuItem(propertiesStrings.getProperty("menu_render_theme_osmarender"),'o');
		itemOsmarenderer.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F10,0));
		itemSelectXMLFile = new JMenuItem(propertiesStrings.getProperty("menu_render_theme_select_file"),'x');
		itemSelectXMLFile.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F11,0));
		
		itemMapFile = new JMenuItem(propertiesStrings.getProperty("menu_mapfile_select_file"),'f');
		itemMapFile.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F12,0));		

		menuInfo.add(itemMapinfo);
		menuInfo.add(itemAbout);
		
		menuPosition.add(itemFollowGPS);
		menuPosition.add(itemLastKnown);
		menuPosition.add(itemEnterCoordiantes);
		menuPosition.add(itemMapFileCenter);

		menuScreenshot.add(itemJPEG);
		menuScreenshot.add(itemPNG);
		
		menuRenderTheme.add(itemMapnik);
		menuRenderTheme.add(itemOsmarenderer);
		menuRenderTheme.add(itemSelectXMLFile);
		
		menuMapFile.add(itemMapFile);
		
		//Listener
		itemMapinfo.addActionListener(this);
		itemAbout.addActionListener(this);
		itemFollowGPS.addActionListener(this);
		itemEnterCoordiantes.addActionListener(this);
		itemMapFileCenter.addActionListener(this);
		itemJPEG.addActionListener(this);
		itemPNG.addActionListener(this);
		itemMapnik.addActionListener(this);
		itemOsmarenderer.addActionListener(this);
		itemSelectXMLFile.addActionListener(this);
		itemMapFile.addActionListener(this);
		
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		
		if (cmd.equals(propertiesStrings.getProperty("menu_info_map_file"))) {
			//TODO: 
		}
		else if (cmd.equals(propertiesStrings.getProperty("menu_info_about"))) {
			//TODO: 
		}
		else if (cmd.equals(propertiesStrings.getProperty("menu_position_follow_gps_signal"))) {
			//TODO: 
		}
		else if (cmd.equals(propertiesStrings.getProperty("menu_position_last_known"))) {
			//TODO: 
		}
		else if (cmd.equals(propertiesStrings.getProperty("menu_position_enter_coordinates"))) {
			//TODO: 
		}
		else if (cmd.equals(propertiesStrings.getProperty("menu_position_map_file_center"))) {
			//TODO: 
		}
		else if (cmd.equals(propertiesStrings.getProperty("menu_screenshot_jpeg"))) {
			//TODO: 
		}
		else if (cmd.equals(propertiesStrings.getProperty("menu_screenshot_png"))) {
			//TODO: 
		}
		else if (cmd.equals(propertiesStrings.getProperty("menu_render_theme_mapnik"))) {
			//TODO: 
		}
		else if (cmd.equals(propertiesStrings.getProperty("menu_render_theme_osmarender"))) {
			//TODO: 
		}
		else if (cmd.equals(propertiesStrings.getProperty("menu_render_theme_select_file"))) {
			//TODO: 
			//parentFrame.startFileBrowser();
		}
		else if (cmd.equals(propertiesStrings.getProperty("menu_mapfile_select_file"))) {
			//TODO:
			parentFrame.startFileBrowser();
		}
		else if (cmd.equals("Exit")) {
			parentFrame.close();

		}
		else if (cmd.equals("About this software")) {

		}
		
	}
	
}
