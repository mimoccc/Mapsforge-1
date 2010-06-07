/*
 * Copyright 2010 mapsforge.org
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.mapsforge.preprocessing.graph.gui;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.mapsforge.preprocessing.graph.gui.panels.DbPreferences;
import org.mapsforge.preprocessing.graph.gui.panels.ProfilPanel;
import org.mapsforge.preprocessing.graph.gui.panels.TransportPanel;
import org.mapsforge.preprocessing.graph.gui.util.DatabaseService;
import org.mapsforge.preprocessing.graph.gui.util.JDBCConnection;


public class MainFrame extends JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3109971929230077879L;
	private static DatabaseService dbs;

	public MainFrame() {

		super("Preprocessing Configuration");
		dbs = new DatabaseService(new JDBCConnection().getConnection());
		init();
	}

	public DatabaseService getDbService() {
		return dbs;
	}

	protected void init() {

		// In der Mitte des Bildschirms platzieren
		this
				.setLocation(
						(Toolkit.getDefaultToolkit().getScreenSize().width - this.getSize().width) / 4,
						(Toolkit.getDefaultToolkit().getScreenSize().height - this.getSize().height) / 4);

		lookAndFeel();
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		JTabbedPane mainPanel = new JTabbedPane();

		JMenuBar menuBar = new JMenuBar();
		this.setJMenuBar(menuBar);

		JMenu fileMenu = new JMenu("File");
		menuBar.add(fileMenu);

		JMenuItem exitMenuItem = new JMenuItem("Exit");
		exitMenuItem.addActionListener(new exitAction());
		fileMenu.addSeparator();
		fileMenu.add(exitMenuItem);

		JMenu preferencesMenu = new JMenu("Preferences");
		menuBar.add(preferencesMenu);

		JMenuItem dbPrefMenuItem = new JMenuItem("Database Preferences");
		dbPrefMenuItem.addActionListener(new openDbPreferences());
		preferencesMenu.add(dbPrefMenuItem);

		JComponent transportPanel = new TransportPanel(dbs);
		JComponent profilPanel = new ProfilPanel(dbs);

		mainPanel.addTab("transport configuration", transportPanel);
		mainPanel.addTab("profil configuration", profilPanel);

		this.add(mainPanel);
		this.pack();
	}

	private void lookAndFeel() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException e1) {
			e1.printStackTrace();
		} catch (InstantiationException e1) {
			e1.printStackTrace();
		} catch (IllegalAccessException e1) {
			e1.printStackTrace();
		} catch (UnsupportedLookAndFeelException e1) {
			e1.printStackTrace();
		}
	}

	public static void main(String[] args) {
		MainFrame mf = new MainFrame();
		mf.setVisible(true);
	}

	class exitAction implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			System.exit(0);
		}

	}

	class openDbPreferences implements ActionListener {

		public void actionPerformed(ActionEvent e) {

			DbPreferences dbPrefs = new DbPreferences(dbs);
			dbPrefs.setVisible(true);
		}
	}

}
