package org.mapsforge.applications.pc.advancedmapviewer;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import org.mapsforge.core.graphics.Canvas;

public class AdvancedMapViewerPC extends JFrame implements WindowListener, ActionListener {

	private Canvas canvas;
	private JMenuBar menuBar;
	private JMenu menuMap, menuPreferences, menuInfo;
	private JMenuItem itemLoadMap, itemExit, itemPreferences, itemAbout, itemMapinfo, itemHelp;
	
	/** Constructor */
	public AdvancedMapViewerPC() {
		
		this.addWindowListener(this);
		this.setTitle("Advanced Map Viewer");
		
		// Menubar
		menuBar = new JMenuBar();
		this.setJMenuBar(menuBar);

		//Menus
		menuMap = new JMenu("Map");
		menuMap.setMnemonic('m');
		menuPreferences = new JMenu("Preferences");
		menuPreferences.setMnemonic('p');
		menuInfo = new JMenu("Info");
		menuInfo.setMnemonic('i');
		
		menuBar.add(menuMap);
		menuBar.add(menuPreferences);
		menuBar.add(menuInfo);

		//Menuitems
		itemLoadMap = new JMenuItem("Load Map",'l');
		itemLoadMap.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F2,0));
		itemExit = new JMenuItem("Exit",'e');
		itemExit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X,InputEvent.CTRL_MASK+InputEvent.ALT_MASK));
		itemPreferences = new JMenuItem("Preferences..",'p');
		itemPreferences.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F3,0));
		itemAbout = new JMenuItem("About",'a');
		itemAbout.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F4,0));
		itemMapinfo = new JMenuItem("Info",'i');
		itemMapinfo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5,0));
		itemHelp = new JMenuItem("Help",'h');
		itemHelp.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F6,0));
		
		menuMap.add(itemLoadMap);
		menuMap.add(itemExit);		
		
		menuPreferences.add(itemPreferences);
		
		menuInfo.add(itemAbout);
		menuInfo.add(itemMapinfo);
		menuInfo.add(itemHelp);

		//Listener
		itemLoadMap.addActionListener(this);
		itemExit.addActionListener(this);
		itemPreferences.addActionListener(this);
		itemAbout.addActionListener(this);
		itemMapinfo.addActionListener(this);
		itemHelp.addActionListener(this);

//		canvas = new Canvas();
		this.add(new java.awt.Canvas());
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
	
	@Override
	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand(); 
		if (cmd.equals("Exit")) {
			close();
		}
		
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
	private void close() {
		//TODO: Cleaning up
		System.exit(0);
	}


	

}
