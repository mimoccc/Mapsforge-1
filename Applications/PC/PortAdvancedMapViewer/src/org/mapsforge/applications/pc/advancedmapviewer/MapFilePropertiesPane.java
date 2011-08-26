package org.mapsforge.applications.pc.advancedmapviewer;

import java.awt.Component;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import javax.swing.JOptionPane;

import org.mapsforge.pc.maps.MapDatabase;

/**
 * A JOptionPane displaying the properties of the current mapFile.
 */
public class MapFilePropertiesPane extends JOptionPane {

	private static final long serialVersionUID = 1L;
	private static String name;
	private static String size;
	private static String version;
	private static String debugInfo;
	private static String date;
	private static String area;
	private static String startPos;
	private static String comments;
	
	/**
	 * This extended JOptionPane is intended to display the properties of the
	 * loaded map file. 
	 * @param parent
	 * 			the component to the pane is displayed on
	 * @param props
	 * 			the properties containing the localized string
	 * @param mapDB
	 * 			the DB of the current map 
	 */
	public static void showInfo(Component parent, Properties props, MapDatabase mapDB) {
		StringBuffer buf = new StringBuffer();
		
		// Name
		name = mapDB.file.getName();  
		buf.append(props.getProperty("info_map_file_name") + "\n" + name + "\n\n");
		
		// Size
		size = "" + mapDB.fileSize/1000000;
		buf.append(props.getProperty("info_map_file_size") + "\n" + size + " MB\n\n");
		
		// Version
		version = "" + mapDB.fileVersionNumber;
		buf.append(props.getProperty("info_map_file_version") + "\n" + version + ".0\n\n");
		
		// DebugInfo
		debugInfo = "" + mapDB.debugFile;
		buf.append(props.getProperty("info_map_file_debug") + "\n" + debugInfo + "\n\n");
		
		// Date
		SimpleDateFormat sdf = new SimpleDateFormat();
		date = sdf.format(new Date(mapDB.mapDate));
		buf.append(props.getProperty("info_map_file_date") + "\n" + date + "\n\n");
		
		// Area
		area = "(" + mapDB.mapBoundary.left + ", " + mapDB.mapBoundary.top + ") - (" +
					mapDB.mapBoundary.right + ", " + mapDB.mapBoundary.bottom + ")";
		buf.append(props.getProperty("info_map_file_area") + "\n" + area + "\n\n");
		
		// Start position
		startPos = "(" + mapDB.startPositionLongitude + ", " + mapDB.startPositionLatitude + ")";
		buf.append(props.getProperty("info_map_file_start") + "\n" + startPos + "\n\n");
		
		// Comments
		comments = "" + mapDB.getCommentText();
		buf.append(props.getProperty("info_map_file_comment") + "\n" + comments + "\n\n");
		
		showMessageDialog(parent, buf, props.getProperty("menu_info_map_file"), INFORMATION_MESSAGE);
	}	
	
}
