package org.mapsforge.applications.pc.advancedmapviewer;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import javax.swing.filechooser.FileFilter;
import java.util.Comparator;

import javax.swing.JFileChooser;
import javax.swing.JPanel;

import org.mapsforge.pc.maps.MapView;

/**
 * A FilePicker displays the contents of directories. The user can navigate
 * within the file system and select a single file whose path is then returned.
 * The ordering of directory contents can be specified via
 * {@link #setFileComparator(Comparator)}. By default subfolders and files are
 * grouped and each group is ordered alphabetically.
 * <p>
 * A {@link FileFilter} can be activated via
 * {@link #setFileDisplayFilter(FileFilter)} to restrict the displayed files and
 * folders. By default all files and folders are visible.
 * <p>
 * Another <code>FileFilter</code> can be applied via
 * {@link #setFileSelectFilter(FileFilter)} to check if a selected file is valid
 * before its path is returned. By default all files are considered as valid and
 * can be selected by the user.
 */
public class FilePickerPC extends JPanel implements ActionListener {

	private static final long serialVersionUID = -6634123407876979284L;

	private Comparator<File> fileComparator = getDefaultFileComparator();
	private FileFilter fileDisplayFilter;
	private FileFilter fileSelectFilter;
	private JFileChooser fileChooser;
	private String openedFile = new String();

	/**
	 * Sets the file comparator which is used to order the contents of all
	 * directories before displaying them. If set to null, subfolders and files
	 * will not be ordered.
	 * 
	 * @param fileComparator
	 *            the file comparator (may be null).
	 */
	public void setFileComparator(Comparator<File> fileComparator) {
		this.fileComparator = fileComparator;
	}

	/**
	 * Sets the file display filter. This filter is used to determine which
	 * files and subfolders of directories will be displayed. If set to null,
	 * all files and subfolders are shown.
	 * 
	 * @param fileDisplayFilter
	 *            the file display filter (may be null).
	 */
	public void setFileDisplayFilter(FileFilter fileDisplayFilter) {
		this.fileDisplayFilter = fileDisplayFilter;
	}

	/**
	 * Sets the file select filter. This filter is used when the user selects a
	 * file to determine if it is valid. If set to null, all files are
	 * considered as valid.
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

	/** Constructor */
	public FilePickerPC() {
		super(new BorderLayout());
		fileChooser = new JFileChooser(".");
	}

	/**
	 * Configuration of filters.
	 */
	public void configure() {
		// TODO: Configuration (if necessary)
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
			return file.getAbsolutePath();
		}

		return null;
	}

	@Override
	// TODO
	public void actionPerformed(ActionEvent event) {

	}
}
