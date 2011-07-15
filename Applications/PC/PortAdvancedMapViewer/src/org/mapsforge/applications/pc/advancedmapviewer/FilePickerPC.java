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

import org.mapsforge.pc.maps.MapView;

public class FilePickerPC extends JPanel implements ActionListener {

	private static final long serialVersionUID = -6634123407876979284L;

	Comparator<File> fileComparator = getDefaultFileComparator();
	FileFilter fileDisplayFilter;
	FileFilter fileSelectFilter;
	
	String openedFile = new String();

	/**
	 * Sets the file comparator which is used to order the contents of all directories before
	 * displaying them. If set to null, subfolders and files will not be ordered.
	 * 
	 * @param fileComparator
	 *            the file comparator (may be null).
	 */
	public void setFileComparator(Comparator<File> fileComparator) {
		this.fileComparator = fileComparator;
	}

	/**
	 * Sets the file display filter. This filter is used to determine which files and subfolders
	 * of directories will be displayed. If set to null, all files and subfolders are shown.
	 * 
	 * @param fileDisplayFilter
	 *            the file display filter (may be null).
	 */
	public void setFileDisplayFilter(FileFilter fileDisplayFilter) {
		this.fileDisplayFilter = fileDisplayFilter;
	}

	/**
	 * Sets the file select filter. This filter is used when the user selects a file to
	 * determine if it is valid. If set to null, all files are considered as valid.
	 * 
	 * @param fileSelectFilter
	 *            the file selection filter (may be null).
	 */
	public void setFileSelectFilter(FileFilter fileSelectFilter) {
		this.fileSelectFilter = fileSelectFilter;
	}

	/**
	 * Creates the default file comparator.
	 * 
	 * @return the default file comparator.
	 */
	private Comparator<File> getDefaultFileComparator() {
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
	
	JFileChooser fileChooser;

	public FilePickerPC() {
		super(new BorderLayout());
		fileChooser = new JFileChooser(".");
		
		

		//TODO start the FilePicker

		/*SwingUtilities.invokeLater(new Runnable() {
			/**
			 * Create FileChooser
			 * @param frame
			 
			public void createFileChooser(JFrame frame) {
				frame.add(this);

		        //Display the window.
		        //frame.pack();
		        frame.setVisible(true);
			}
			
			public void run() {
	            //Turn off metal's use of bold fonts
	            UIManager.put("swing.boldMetal", Boolean.FALSE); 
	            this.createFileChooser(frame);
	        }
		});
		
		fileChooser.addChoosableFileFilter(this.fileDisplayFilter);
		int returnVal = fileChooser.showOpenDialog(this);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            //This is where a real application would open the file.
            System.out.println("Opening: " + file.getName() + ".");
            if(file.getName().endsWith(".map")) {
            	openedFile = file.getName();
            }
        } else {
        	System.out.println("Open command cancelled by user.");
        }*/
	}
	
	public void configure() {
		// set the FileDisplayFilter
		this.setFileDisplayFilter(new FileFilter() {
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
		this.setFileSelectFilter(new FileFilter() {
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
	}
	
	public String openMap() {
		int returnVal = fileChooser.showOpenDialog(FilePickerPC.this);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            //This is where a real application would open the file.
            System.out.println("Opening: " + file.getName() + ".");
            //TODO PATH
            return file.getName();
        } else {
        	System.out.println("Open command cancelled by user.");
        	return "bremen-0.2.2.map";
        }
	}
	
	@Override
	//TODO
	public void actionPerformed(ActionEvent event) {
		
	}
}
