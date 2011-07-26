package org.mapsforge.applications.pc.advancedmapviewer;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Properties;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JRadioButtonMenuItem;

/**
 * Preferences menu for the application.
 */
public class PreferencesMenu extends JMenu implements ActionListener {

	private static final long serialVersionUID = 1L;
	private Properties propertiesStrings, propertiesSettings;
	private AdvancedMapViewerPC parentFrame;
	
	private JMenu menuMapMode, menuFontSize;
	private JCheckBoxMenuItem itemMapScale, itemFrameRate, itemTileBoundaries, itemWaterTiles;
	private JRadioButtonMenuItem itemModeCanvas, itemModeMapnik, itemModeOsma, itemModeCycle;
	private JRadioButtonMenuItem itemFontTiny, itemFontSmall, itemFontNormal, itemFontLarge, itemFontHuge;
	
	
	/** Constructor 
	 * @param p parentFrame 
	 * @param n name*/
	public PreferencesMenu(AdvancedMapViewerPC p, String n) {
		
		super(n);
		
		/** ParenFrame */
		parentFrame = p;
		
		/** Properties */
		propertiesStrings = parentFrame.getPropertiesStrings();
		propertiesSettings = parentFrame.getPropertiesSettings();
		
		itemMapScale = new JCheckBoxMenuItem(propertiesStrings.getProperty("preferences_show_scale_bar"));
		itemMapScale.setToolTipText(propertiesStrings.getProperty("preferences_show_scale_bar_desc"));
		itemMapScale.setSelected(Boolean.parseBoolean(propertiesSettings.getProperty("preferences_show_scale_bar")));
//		itemMapScale.setActionCommand(propertiesStrings.getProperty("preferences_show_scale_bar"));
		itemFrameRate = new JCheckBoxMenuItem(propertiesStrings.getProperty("preferences_show_fps_counter"));
		itemFrameRate.setToolTipText(propertiesStrings.getProperty("preferences_show_fps_counter_desc"));
		itemFrameRate.setSelected(Boolean.parseBoolean(propertiesSettings.getProperty("preferences_show_fps_counter")));
		
		itemTileBoundaries = new JCheckBoxMenuItem(propertiesStrings.getProperty("preferences_show_tile_frames"));
		itemTileBoundaries.setToolTipText(propertiesStrings.getProperty("preferences_show_tile_frames_desc"));
		itemTileBoundaries.setSelected(Boolean.parseBoolean(propertiesSettings.getProperty("preferences_show_tile_frames")));
		itemWaterTiles = new JCheckBoxMenuItem(propertiesStrings.getProperty("preferences_show_water_tiles"));
		itemWaterTiles.setToolTipText(propertiesStrings.getProperty("preferences_show_water_tiles_desc"));
		itemWaterTiles.setSelected(Boolean.parseBoolean(propertiesSettings.getProperty("preferences_show_water_tiles")));
		menuMapMode = new JMenu(propertiesStrings.getProperty("preferences_map_view_mode"));
		itemModeCanvas = new JRadioButtonMenuItem(propertiesStrings.getProperty("preferences_map_view_mode_values_canvas"));
		itemModeMapnik = new JRadioButtonMenuItem(propertiesStrings.getProperty("preferences_map_view_mode_values_mapnik"));
		itemModeOsma = new JRadioButtonMenuItem(propertiesStrings.getProperty("preferences_map_view_mode_values_osmarenderer"));
		itemModeCycle = new JRadioButtonMenuItem(propertiesStrings.getProperty("preferences_map_view_mode_values_opencyclemap"));
		menuMapMode.add(itemModeCanvas);
		menuMapMode.add(itemModeMapnik);
		menuMapMode.add(itemModeOsma);
		menuMapMode.add(itemModeCycle);
		String mode = propertiesSettings.getProperty("preferences_map_view_mode_values_default");
		if (mode.equals(propertiesSettings.getProperty("preferences_map_view_mode_values_canvas"))) itemModeCanvas.setSelected(true);
		else if (mode.equals(propertiesSettings.getProperty("preferences_map_view_mode_values_mapnik"))) itemModeMapnik.setSelected(true);
		else if (mode.equals(propertiesSettings.getProperty("preferences_map_view_mode_values_osmarenderer"))) itemModeOsma.setSelected(true);
		else if (mode.equals(propertiesSettings.getProperty("preferences_map_view_mode_values_opencyclemap"))) itemModeCycle.setSelected(true);
				
		ButtonGroup bg = new ButtonGroup();
		bg.add(itemModeCanvas);
		bg.add(itemModeMapnik);
		bg.add(itemModeOsma);
		bg.add(itemModeCycle);
		
		menuFontSize = new JMenu(propertiesStrings.getProperty("preferences_text_scale"));
		menuFontSize.setToolTipText(propertiesStrings.getProperty("preferences_text_scale_desc"));
		itemFontTiny = new JRadioButtonMenuItem(propertiesStrings.getProperty("preferences_text_scale_values_tiny"));
		itemFontSmall = new JRadioButtonMenuItem(propertiesStrings.getProperty("preferences_text_scale_values_small"));
		itemFontNormal = new JRadioButtonMenuItem(propertiesStrings.getProperty("preferences_text_scale_values_normal"));
		itemFontLarge = new JRadioButtonMenuItem(propertiesStrings.getProperty("preferences_text_scale_values_large"));
		itemFontHuge = new JRadioButtonMenuItem(propertiesStrings.getProperty("preferences_text_scale_values_huge"));
		
		menuFontSize.add(itemFontTiny);
		menuFontSize.add(itemFontSmall);
		menuFontSize.add(itemFontNormal);
		menuFontSize.add(itemFontLarge);
		menuFontSize.add(itemFontHuge);
		String font = propertiesSettings.getProperty("preferences_text_scale_values_default");
		if (font.equals(propertiesSettings.getProperty("preferences_text_scale_values_tiny"))) itemFontTiny.setSelected(true);
		else if (font.equals(propertiesSettings.getProperty("preferences_text_scale_values_small"))) itemFontSmall.setSelected(true);
		else if (font.equals(propertiesSettings.getProperty("preferences_text_scale_values_normal"))) itemFontNormal.setSelected(true);
		else if (font.equals(propertiesSettings.getProperty("preferences_text_scale_values_large"))) itemFontLarge.setSelected(true);
		else if (font.equals(propertiesSettings.getProperty("preferences_text_scale_values_huge"))) itemFontHuge.setSelected(true);
		
		bg = new ButtonGroup();
		bg.add(itemFontTiny);
		bg.add(itemFontSmall);
		bg.add(itemFontNormal);
		bg.add(itemFontLarge);
		bg.add(itemFontHuge);
		
		this.add(itemMapScale);
		this.add(menuMapMode);
		this.add(menuFontSize);
		this.addSeparator();
		
		this.add(itemFrameRate);
		this.add(itemTileBoundaries);
		this.add(itemWaterTiles);	
		
		//Listener
		itemMapScale.addActionListener(this);
		itemFrameRate.addActionListener(this);
		itemTileBoundaries.addActionListener(this);
		itemWaterTiles.addActionListener(this);
		itemModeCanvas.addActionListener(this);
		itemModeMapnik.addActionListener(this);
		itemModeOsma.addActionListener(this);
		itemModeCycle.addActionListener(this);
		itemFontTiny.addActionListener(this);
		itemFontSmall.addActionListener(this);
		itemFontNormal.addActionListener(this);
		itemFontLarge.addActionListener(this);
		itemFontHuge.addActionListener(this);
		
	}
	
	
	@Override
	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		
		// Checkboxes
		if (cmd.equals(propertiesStrings.getProperty("preferences_show_scale_bar"))) {
			propertiesSettings.setProperty("preferences_show_scale_bar", Boolean.toString(itemMapScale.isSelected()));
		}
		else if(cmd.equals(propertiesStrings.getProperty("preferences_show_fps_counter"))) {
			propertiesSettings.setProperty("preferences_show_fps_counter", Boolean.toString(itemFrameRate.isSelected()));
		}
		else if(cmd.equals(propertiesStrings.getProperty("preferences_show_tile_frames"))) {
			propertiesSettings.setProperty("preferences_show_tile_frames", Boolean.toString(itemTileBoundaries.isSelected()));
		}
		else if(cmd.equals(propertiesStrings.getProperty("preferences_show_water_tiles"))) {
			propertiesSettings.setProperty("preferences_show_water_tiles", Boolean.toString(itemWaterTiles.isSelected()));
		}
		
		// Map Mode
		else if(cmd.equals(propertiesStrings.getProperty("preferences_map_view_mode_values_canvas"))) {
			propertiesSettings.setProperty("preferences_map_view_mode_values_default", propertiesSettings.getProperty("preferences_map_view_mode_values_canvas"));
		}
		else if(cmd.equals(propertiesStrings.getProperty("preferences_map_view_mode_values_mapnik"))) {
			propertiesSettings.setProperty("preferences_map_view_mode_values_default", propertiesSettings.getProperty("preferences_map_view_mode_values_mapnik"));
		}
		else if(cmd.equals(propertiesStrings.getProperty("preferences_map_view_mode_values_osmarenderer"))) {
			propertiesSettings.setProperty("preferences_map_view_mode_values_default", propertiesSettings.getProperty("preferences_map_view_mode_values_osmarenderer"));
		}
		else if(cmd.equals(propertiesStrings.getProperty("preferences_map_view_mode_values_opencyclemap"))) {
			propertiesSettings.setProperty("preferences_map_view_mode_values_default", propertiesSettings.getProperty("preferences_map_view_mode_values_opencyclemap"));
		}
		
		// Font Size
		else if(cmd.equals(propertiesStrings.getProperty("preferences_text_scale_values_tiny"))) {
			propertiesSettings.setProperty("preferences_text_scale_values_default", propertiesSettings.getProperty("preferences_text_scale_values_tiny"));
		}
		else if(cmd.equals(propertiesStrings.getProperty("preferences_text_scale_values_small"))) {
			propertiesSettings.setProperty("preferences_text_scale_values_default", propertiesSettings.getProperty("preferences_text_scale_values_small"));
		}
		else if(cmd.equals(propertiesStrings.getProperty("preferences_text_scale_values_normal"))) {
			propertiesSettings.setProperty("preferences_text_scale_values_default", propertiesSettings.getProperty("preferences_text_scale_values_normal"));
		}
		else if(cmd.equals(propertiesStrings.getProperty("preferences_text_scale_values_large"))) {
			propertiesSettings.setProperty("preferences_text_scale_values_default", propertiesSettings.getProperty("preferences_text_scale_values_large"));
		}
		else if(cmd.equals(propertiesStrings.getProperty("preferences_text_scale_values_huge"))) {
			propertiesSettings.setProperty("preferences_text_scale_values_default", propertiesSettings.getProperty("preferences_text_scale_values_huge"));
		}
		
		
	}

}
