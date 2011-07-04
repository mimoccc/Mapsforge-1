package org.mapsforge.applications.pc.advancedmapviewer;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import javax.swing.filechooser.FileFilter;
import java.util.Comparator;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class FilePickerPC extends JPanel implements ActionListener {

	private static final long serialVersionUID = -6634123407876979284L;

	static Comparator<File> fileComparator = getDefaultFileComparator();
	static FileFilter fileDisplayFilter;
	static FileFilter fileSelectFilter;

	/**
	 * Sets the file comparator which is used to order the contents of all directories before
	 * displaying them. If set to null, subfolders and files will not be ordered.
	 * 
	 * @param fileComparator
	 *            the file comparator (may be null).
	 */
	public static void setFileComparator(Comparator<File> fileComparator) {
		FilePickerPC.fileComparator = fileComparator;
	}

	/**
	 * Sets the file display filter. This filter is used to determine which files and subfolders
	 * of directories will be displayed. If set to null, all files and subfolders are shown.
	 * 
	 * @param fileDisplayFilter
	 *            the file display filter (may be null).
	 */
	public static void setFileDisplayFilter(FileFilter fileDisplayFilter) {
		FilePickerPC.fileDisplayFilter = fileDisplayFilter;
	}

	/**
	 * Sets the file select filter. This filter is used when the user selects a file to
	 * determine if it is valid. If set to null, all files are considered as valid.
	 * 
	 * @param fileSelectFilter
	 *            the file selection filter (may be null).
	 */
	public static void setFileSelectFilter(FileFilter fileSelectFilter) {
		FilePickerPC.fileSelectFilter = fileSelectFilter;
	}

	/**
	 * Creates the default file comparator.
	 * 
	 * @return the default file comparator.
	 */
	private static Comparator<File> getDefaultFileComparator() {
		// order all files by type and alphabetically by name
		return new Comparator<File>() {
			@Override
			public int compare(File file1, File file2) {
				if (file1.isDirectory() && !file2.isDirectory()) {
					return -1;
				} else if (!file1.isDirectory() && file2.isDirectory()) {
					return 1;
				} else {
					return file1.getName().compareToIgnoreCase(file2.getName());
				}
			}
		};
	}
	
	static JFileChooser filePicker;
	static JFrame frame;

	public FilePickerPC() {
		super(new BorderLayout());
		filePicker = new JFileChooser(".");
		filePicker.addChoosableFileFilter(FilePickerPC.fileDisplayFilter);
		int returnVal = filePicker.showOpenDialog(this);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = filePicker.getSelectedFile();
            //This is where a real application would open the file.
            System.out.println("Opening: " + file.getName() + ".");
            //TODO
        } else {
        	System.out.println("Open command cancelled by user.");
        }
	}

	/**
	 * Create FileChooser
	 * @param frame
	 */
	public static void createFileChooser(JFrame frame) {
		FilePickerPC.frame = frame;
		frame.add(new FilePickerPC());

        //Display the window.
        //frame.pack();
        frame.setVisible(true);
	}
	
	@Override
	//TODO
	public void actionPerformed(ActionEvent event) {
		int returnVal = filePicker.showOpenDialog(FilePickerPC.this);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = filePicker.getSelectedFile();
            //This is where a real application would open the file.
            System.out.println("Opening: " + file.getName() + ".");
            //TODO
        } else {
        	System.out.println("Open command cancelled by user.");
        }
	}
}
