package org.mapsforge.applications.pc.advancedmapviewer;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

public class MenuBar extends JMenuBar{

	private AdvancedMapViewerPC parentFrame;
	private JMenu menuInfo, menuPosition, menuScreenshot;
	private JMenuItem itemMapFile, itemPreferences;
	private JMenuItem itemAbout, itemMapinfo;
	private JMenuItem itemFollowGPS, itemEnterCoordiantes, itemMapFileCenter;
	private JMenuItem itemJPEG, itemPNG;
	
	/** Constructor 
	 * @param parentFrame */
	public MenuBar(AdvancedMapViewerPC p) {
		
		//ParenFrame
		parentFrame = p;
		
		//Menus
		menuInfo = new JMenu("Info");
		menuInfo.setMnemonic('i');
		menuPosition = new JMenu("Position");
		menuPosition.setMnemonic('p');
		menuScreenshot = new JMenu("Screenshot");
		menuScreenshot.setMnemonic('s');
		
		itemMapFile = new JMenuItem("Map File",'l');
		itemMapFile.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1,0));
		itemPreferences = new JMenuItem("Preferences",'p');
		itemPreferences.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F2,0));
		
		this.add(itemMapFile);
		this.add(menuInfo);
		this.add(menuPosition);
		this.add(menuScreenshot);
		this.add(itemPreferences);

		//Menuitems
		itemMapinfo = new JMenuItem("Map File Properties",'m');
		itemMapinfo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F3,0));
		itemAbout = new JMenuItem("About this software",'a');
		itemAbout.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F4,0));
		
		itemFollowGPS = new JMenuItem("Follow GPS signal",'f');
		itemFollowGPS.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F4,0));
		itemFollowGPS.setEnabled(false);		
		itemEnterCoordiantes = new JMenuItem("Enter Coordinates",'e');
		itemEnterCoordiantes.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
		itemMapFileCenter = new JMenuItem("Map File center",'m');
		itemMapFileCenter.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0));
		

		itemJPEG = new JMenuItem("JPEG (lossy)",'j');
		itemJPEG.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F7,0));
		itemPNG = new JMenuItem("PNG (lossless)",'p');
		itemPNG.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F8,0));

		menuInfo.add(itemMapinfo);
		menuInfo.add(itemAbout);
		
		menuPosition.add(itemFollowGPS);
		menuPosition.add(itemEnterCoordiantes);
		menuPosition.add(itemMapFileCenter);

		menuScreenshot.add(itemJPEG);
		menuScreenshot.add(itemPNG);
		
		//Listener
		itemMapFile.addActionListener(parentFrame);
		itemMapinfo.addActionListener(parentFrame);
		itemAbout.addActionListener(parentFrame);
		itemFollowGPS.addActionListener(parentFrame);
		itemEnterCoordiantes.addActionListener(parentFrame);
		itemMapFileCenter.addActionListener(parentFrame);
		itemJPEG.addActionListener(parentFrame);
		itemPNG.addActionListener(parentFrame);
		itemPreferences.addActionListener(parentFrame);
		
	}
	
}
