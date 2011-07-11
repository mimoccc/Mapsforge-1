package org.mapsforge.applications.pc.advancedmapviewer;

import java.awt.BorderLayout;
import java.awt.List;
import java.awt.Scrollbar;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.border.Border;


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
//		mapPanel.setBorder(padding);
//		generalPanel.setBorder(padding);
//		debugPanel.setBorder(padding);
		
		JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.addTab("Map settings", null, mapPanel, mapDesc); 
		tabbedPane.addTab("General settings", null, generalPanel, generalDesc);
		tabbedPane.addTab("Debug settings", null, debugPanel, debugDesc);
		
		this.add(tabbedPane, BorderLayout.CENTER);
		this.add(label, BorderLayout.PAGE_END);
		label.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
	}
	
	JCheckBox scale;
	List map;
	List font;
	
	private JPanel createMapSettings() {
		//final String scaleBar = "scale_bar";
		//final String mapMode = "map_mode";
		//final String fontSize = "font_size";
		scale = new JCheckBox("Map scale bar");
		map = new List(4, false);
		font = new List(5, false);		
		
	
		//Map Scale Bar (CHECK BOX)
		//Map Mode (LIST) {Canvas renderer, Mapnik (online), Osmarenderer(online), OpenCycleMap(online)}
		//Font Size (LIST) {tiny, small, normal, large, huge}
		return null;
	}
	
	JCheckBox full;
	JCheckBox stay;
	JCheckBox cache;
	Scrollbar storage;
	Scrollbar move;
	
	private JPanel createGeneralSettings() {
		//final String fullScreen = "full_screen";
		//final String stayAwake = "stay_awake";
		//final String cachePersistence = "cache_persistence";
		//final String externalStorage = "external_storage";
		//final String moveSpeed = "move_speed";
		
		full = new JCheckBox("Full screen mode");
		stay = new JCheckBox("Stay awake");
		cache = new JCheckBox("Cache persistence");
		//TODO parameter
		storage = new Scrollbar();
		move = new Scrollbar();
		//Full screen mode (CHECK BOX)
		//Stay awake (CHECK BOX)
		//Cache persistence (CHECK BOX)
		//External storage (SIDEBAR)
		//Move speed (SIDEBAR)
		return null;
	}
	
	JCheckBox rate;
	JCheckBox boundaries;
	JCheckBox coordinates;
	JCheckBox water;
	
	private JPanel createDebugSettings() {
		//final String frameRate = "frame_rate";
		//final String tileBoundaries = "tile_boundaries";
		//final String tileCoordinates = "tile_coordinates";
		//final String waterTiles = "water_tiles";
		
		rate = new JCheckBox("Frame rate");
		boundaries = new JCheckBox("Tile boundaries");
		coordinates = new JCheckBox("Tile coordinates");
		water = new JCheckBox("Water tiles");
		
		
		//Frame rate (CHECK BOX)
		//Tile boundaries (CHECK BOX)
		//Tile coordinates (CHECK BOX)
		//Water tiles (CHECK BOX)
		return null;
	}
}
