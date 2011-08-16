package org.mapsforge.applications.pc.advancedmapviewer;

import java.awt.Component;
import java.io.File;
import java.util.Properties;
import javax.swing.JOptionPane;

/**
 * A JOptionPane displaying the properties of the current mapFile.
 */
public class MapFilePropertiesPane extends JOptionPane {

	private static String name;
	private static String size;
	private static String version;
	private static String debugInfo;
	private static String date;
	private static String area;
	private static String startPos;
	private static String comment;
	
	/**
	 * @param parent
	 * @param props
	 * @param mapPath
	 */
	public static void showInfo(Component parent, Properties props, String mapPath) {
		File mapFile = new File(mapPath);
		StringBuffer buf = new StringBuffer();
		buf.append(props.getProperty("info_map_file_name") + "\n" + mapFile.getName() + "\n\n");
		buf.append(props.getProperty("info_map_file_size") + "\n" + mapFile.length() + " GB\n\n");
		buf.append(props.getProperty("info_map_file_version") + "\n" + version + ".0\n\n");
		buf.append(props.getProperty("info_map_file_debug") + "\n" + debugInfo + "\n\n");
		buf.append(props.getProperty("info_map_file_date") + "\n" + date + "2012 \n\n");
		buf.append(props.getProperty("info_map_file_area") + "\n" + area + "\n\n");
		buf.append(props.getProperty("info_map_file_start") + "\n" + startPos + "\n\n");
		buf.append(props.getProperty("info_map_file_comment") + "\n" + comment + "\n\n");
		showMessageDialog(parent, buf, props.getProperty("menu_info_map_file"), INFORMATION_MESSAGE);
	}
	
	
	
}
