/*
 * Copyright 2010, 2011 mapsforge.org
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.mapsforge.applications.pc.advancedmapviewer;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.Comparator;

/* APP */
import android.app.Activity;
//import org.mapsforge.core.app.Activity;
import android.app.AlertDialog;
//import org.mapsforge.core.app.AlertDialog;
import android.app.Dialog;
//import org.mapsforge.core.app.Dialog;

/* CONTENT */
import android.content.Intent;
//import org.mapsforge.core.content.Intent;
import android.content.SharedPreferences;
//import org.mapsforge.core.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
//import org.mapsforge.core.content.SharedPreferences.Editor;

/* OS */
import android.os.Bundle;
//import org.mapsforge.core.os.Bundle;

/* PREFERENCE */
import android.preference.PreferenceManager;
//import org.mapsforge.core.;

/* VIEW */
import android.view.View;
//import org.mapsforge.core.view.View;
import android.view.WindowManager;
//import org.mapsforge.core.view.WindowMAnager;

/* WIDGET */
import android.widget.AdapterView;
//import org.mapsforge.core.widget.AdapterView;
import android.widget.GridView;
//import org.mapsforge.core.widget.GridView;

/**
 * A FilePicker displays the contents of directories. The user can navigate within the file
 * system and select a single file whose path is then returned to the calling activity. The
 * ordering of directory contents can be specified via {@link #setFileComparator(Comparator)}.
 * By default subfolders and files are grouped and each group is ordered alphabetically.
 * <p>
 * A {@link FileFilter} can be activated via {@link #setFileDisplayFilter(FileFilter)} to
 * restrict the displayed files and folders. By default all files and folders are visible.
 * <p>
 * Another <code>FileFilter</code> can be applied via {@link #setFileSelectFilter(FileFilter)}
 * to check if a selected file is valid before its path is returned. By default all files are
 * considered as valid and can be selected by the user.
 */
public class FilePicker extends Activity implements AdapterView.OnItemClickListener {
	private static final String DEFAULT_DIRECTORY = "/";
	private static final int DIALOG_FILE_INVALID = 0;
	private static final int DIALOG_FILE_SELECT = 1;
	private static Comparator<File> fileComparator = getDefaultFileComparator();
	private static FileFilter fileDisplayFilter;
	private static FileFilter fileSelectFilter;
	private static final String PREFERENCES_FILE = "FilePicker";

	/**
	 * Sets the file comparator which is used to order the contents of all directories before
	 * displaying them. If set to null, subfolders and files will not be ordered.
	 * 
	 * @param fileComparator
	 *            the file comparator (may be null).
	 */
	public static void setFileComparator(Comparator<File> fileComparator) {
		FilePicker.fileComparator = fileComparator;
	}

	/**
	 * Sets the file display filter. This filter is used to determine which files and subfolders
	 * of directories will be displayed. If set to null, all files and subfolders are shown.
	 * 
	 * @param fileDisplayFilter
	 *            the file display filter (may be null).
	 */
	public static void setFileDisplayFilter(FileFilter fileDisplayFilter) {
		FilePicker.fileDisplayFilter = fileDisplayFilter;
	}

	/**
	 * Sets the file select filter. This filter is used when the user selects a file to
	 * determine if it is valid. If set to null, all files are considered as valid.
	 * 
	 * @param fileSelectFilter
	 *            the file selection filter (may be null).
	 */
	public static void setFileSelectFilter(FileFilter fileSelectFilter) {
		FilePicker.fileSelectFilter = fileSelectFilter;
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

	private File currentDirectory;
	private FilePickerIconAdapter filePickerIconAdapter;
	private File[] files;
	private File[] filesWithParentFolder;
	private GridView gridView;
	private File selectedFile;

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		this.selectedFile = this.files[(int) id];
		if (this.selectedFile.isDirectory()) {
			this.currentDirectory = this.selectedFile;
			browseToCurrentDirectory();
		} else if (fileSelectFilter == null || fileSelectFilter.accept(this.selectedFile)) {
			setResult(RESULT_OK, new Intent().putExtra("selectedFile", this.selectedFile
					.getAbsolutePath()));
			finish();
		} else {
			showDialog(DIALOG_FILE_INVALID);
		}
	}

	/**
	 * Browses to the current directory.
	 */
	private void browseToCurrentDirectory() {
		setTitle(this.currentDirectory.getAbsolutePath());

		// read the subfolders and files from the current directory
		if (fileDisplayFilter == null) {
			this.files = this.currentDirectory.listFiles();
		} else {
			this.files = this.currentDirectory.listFiles(fileDisplayFilter);
		}

		if (this.files == null) {
			this.files = new File[0];
		} else {
			// order the subfolders and files
			Arrays.sort(this.files, fileComparator);
		}

		// if a parent directory exists, add it at the first position
		if (this.currentDirectory.getParentFile() != null) {
			this.filesWithParentFolder = new File[this.files.length + 1];
			this.filesWithParentFolder[0] = this.currentDirectory.getParentFile();
			System.arraycopy(this.files, 0, this.filesWithParentFolder, 1, this.files.length);
			this.files = this.filesWithParentFolder;
			this.filePickerIconAdapter.setFiles(this.files, true);
		} else {
			this.filePickerIconAdapter.setFiles(this.files, false);
		}
		this.filePickerIconAdapter.notifyDataSetChanged();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_file_picker);

		this.filePickerIconAdapter = new FilePickerIconAdapter(this);
		this.gridView = (GridView) findViewById(R.id.filePickerView);
		this.gridView.setOnItemClickListener(this);
		this.gridView.setAdapter(this.filePickerIconAdapter);

		if (savedInstanceState == null) {
			// first start of this instance
			showDialog(DIALOG_FILE_SELECT);
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		switch (id) {
			case DIALOG_FILE_INVALID:
				builder.setIcon(android.R.drawable.ic_menu_info_details);
				builder.setTitle(R.string.error);
				builder.setMessage(R.string.file_invalid);
				builder.setPositiveButton(R.string.ok, null);
				return builder.create();
			case DIALOG_FILE_SELECT:
				builder.setMessage(R.string.file_select);
				builder.setPositiveButton(R.string.ok, null);
				return builder.create();
			default:
				// do dialog will be created
				return null;
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		// save the current directory
		Editor editor = getSharedPreferences(PREFERENCES_FILE, MODE_PRIVATE).edit();
		editor.clear();
		if (this.currentDirectory != null) {
			editor.putString("currentDirectory", this.currentDirectory.getAbsolutePath());
		}
		editor.commit();
	}

	@Override
	protected void onResume() {
		super.onResume();
		// check if the full screen mode should be activated
		if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("fullscreen", false)) {
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
		} else {
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
		}

		// restore the current directory
		SharedPreferences preferences = getSharedPreferences(PREFERENCES_FILE, MODE_PRIVATE);
		this.currentDirectory = new File(preferences.getString("currentDirectory",
				DEFAULT_DIRECTORY));
		if (!this.currentDirectory.exists() || !this.currentDirectory.canRead()) {
			this.currentDirectory = new File(DEFAULT_DIRECTORY);
		}
		browseToCurrentDirectory();
	}
}