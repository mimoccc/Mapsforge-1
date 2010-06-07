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
package org.mapsforge.preprocessing.graph.gui.panels;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.NoSuchElementException;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import org.mapsforge.preprocessing.graph.gui.util.DatabaseService;
import org.mapsforge.preprocessing.graph.gui.util.SimpleRoutingConfigurationWriter;
import org.mapsforge.preprocessing.graph.model.gui.DatabaseProperties;
import org.mapsforge.preprocessing.graph.model.gui.Profil;
import org.mapsforge.preprocessing.graph.model.gui.Transport;

public class ProfilPanel extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7197827414427804065L;
	private DatabaseService dbs;

	private JComboBox cb_ChooseProfil;
	private JComboBox cb_ChooseTransport_controlPanel;
	private JComboBox cb_ChooseTransport_configurationPanel;
	private JComboBox cb_ChooseHeuristic;
	private JTextField tf_ProfilName;
	private JTextField tf_Url;
	JFileChooser fc;

	public ProfilPanel(DatabaseService dbs) {
		this.dbs = dbs;
		this.fc = new JFileChooser() {

			/*
			 * this is a workaround to use the jfilechooser class without losing system
			 * resources
			 */
			private static final long serialVersionUID = -832241031328432102L;

			@Override
			public void updateUI() {
				putClientProperty("FileChooser.useShellFolder", Boolean.FALSE); // <---- VOR
				// super.updateUI()
				// einfügen!
				super.updateUI();
			}
		};
		this.setLayout(new BorderLayout());

		this.add(getConfigurationPanel(), BorderLayout.EAST);
		this.add(getControlPanel(), BorderLayout.WEST);

		setComboBoxChooseTransport();
		setComboBoxChooseProfil(null);
	}

	private Component getControlPanel() {
		JPanel rightPanel = new JPanel(new GridBagLayout());
		GridBagConstraints rightPanelConstraints = new GridBagConstraints();

		// set constraints
		rightPanelConstraints.insets = new Insets(5, 5, 0, 5);
		rightPanelConstraints.anchor = GridBagConstraints.NORTH;
		rightPanelConstraints.fill = GridBagConstraints.HORIZONTAL;

		// add combobox to the panel
		cb_ChooseTransport_controlPanel = new JComboBox();
		rightPanel.add(cb_ChooseTransport_controlPanel, rightPanelConstraints);

		rightPanelConstraints.gridy = 1;
		cb_ChooseProfil = new JComboBox();
		rightPanel.add(cb_ChooseProfil, rightPanelConstraints);

		// add buttons to the panel
		rightPanelConstraints.gridy = 2;
		JButton bSaveProfil = new JButton("Aktuelles Profil speichern");
		rightPanel.add(bSaveProfil, rightPanelConstraints);
		rightPanelConstraints.gridy = 3;
		JButton bCreateProfil = new JButton("Neues Profil anlegen");
		rightPanel.add(bCreateProfil, rightPanelConstraints);
		rightPanelConstraints.gridy = 4;
		rightPanelConstraints.insets = new Insets(5, 5, 5, 5);
		rightPanelConstraints.weighty = 1.0;
		JButton bDeleteProfil = new JButton("Aktuelles Profil löschen");
		rightPanel.add(bDeleteProfil, rightPanelConstraints);

		cb_ChooseTransport_controlPanel.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				setComboBoxChooseProfil((Transport) cb_ChooseTransport_controlPanel
						.getSelectedItem());
			}
		});

		// add action listener to the combobox
		cb_ChooseProfil.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				showChoosenProfil((Profil) cb_ChooseProfil.getSelectedItem());
			}
		});

		// add action listeners to the buttons
		bSaveProfil.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				updateProfilToDB();
			}
		});

		bCreateProfil.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				createProfilInDB();
			}
		});

		bDeleteProfil.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				deleteProfilFromDB();
			}
		});

		// add separator

		rightPanelConstraints.gridy = 5;
		JSeparator north = new JSeparator(SwingConstants.HORIZONTAL);
		rightPanel.add(north, rightPanelConstraints);

		return rightPanel;
	}

	private Component getConfigurationPanel() {
		JPanel leftPanel = new JPanel(new GridBagLayout());
		GridBagConstraints leftPanelConstraints = new GridBagConstraints();

		// set constraints
		leftPanelConstraints.insets = new Insets(5, 5, 0, 5);
		leftPanelConstraints.anchor = GridBagConstraints.NORTH;
		leftPanelConstraints.fill = GridBagConstraints.HORIZONTAL;

		// add labels to first row
		leftPanel.add(new JLabel("Name des Profils: "), leftPanelConstraints);

		// create textfields
		tf_ProfilName = new JTextField();
		tf_Url = new JTextField();

		// add textfield to second row

		leftPanelConstraints.gridy = 1;
		leftPanelConstraints.gridx = 0;
		leftPanel.add(tf_ProfilName, leftPanelConstraints);

		// add labels
		leftPanelConstraints.gridy = 3;
		leftPanelConstraints.gridx = 0;
		leftPanel.add(new JLabel("Transportmittel:"), leftPanelConstraints);
		leftPanelConstraints.gridx = 1;
		// leftPanel.add(new JLabel("Heuristik:"), leftPanelConstraints);

		// create comboBoxen
		cb_ChooseTransport_configurationPanel = new JComboBox();
		cb_ChooseHeuristic = new JComboBox();

		// add comboBoxen

		leftPanelConstraints.gridy = 4;
		leftPanelConstraints.gridx = 0;
		leftPanel.add(cb_ChooseTransport_configurationPanel, leftPanelConstraints);
		leftPanelConstraints.gridx = 1;
		// leftPanel.add(cb_ChooseHeuristic, leftPanelConstraints);

		// add url label

		leftPanelConstraints.gridy = 5;
		leftPanelConstraints.gridx = 0;
		leftPanel.add(new JLabel("Url: "), leftPanelConstraints);

		// change constraints
		leftPanelConstraints.insets = new Insets(5, 5, 5, 5);
		leftPanelConstraints.weighty = 1.0;
		leftPanelConstraints.gridheight = 2;

		// add url components

		leftPanelConstraints.gridy = 6;
		leftPanelConstraints.gridx = 0;
		leftPanel.add(tf_Url = new JTextField(), leftPanelConstraints);

		// create button to browse and add it
		leftPanelConstraints.gridx = 2;
		// JButton b_UrlBrowse = new JButton("Durchsuchen");
		// leftPanel.add(b_UrlBrowse, leftPanelConstraints);
		// add action listeners to buttons
		/*
		 * b_UrlBrowse.addActionListener(new ActionListener() {
		 * 
		 * @Override public void actionPerformed(ActionEvent e) {
		 * 
		 * chooseUrlFile();
		 * 
		 * // TODO Auto-generated method stub /* wir müssen die festplatte nach einem
		 * verzeichnes durchsuchen wo eine datei liegen kann und dieses pfad dann aufnehmen in
		 * das textfeld
		 *//*
			 * } });
			 */

		// add button to create new profil file
		leftPanelConstraints.gridy = 6;
		leftPanelConstraints.gridx = 3;
		JButton b_createConfigurationFile = new JButton("Profildatei erzeugen");

		leftPanel.add(b_createConfigurationFile, leftPanelConstraints);

		b_createConfigurationFile.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				createNewConfigurationFile();
			}
		});

		return leftPanel;
	}

	private void showChoosenProfil(Profil p) {

		if (p == null) {
			// no profile is selected
			tf_ProfilName.setText("");
			tf_Url.setText("");
			cb_ChooseHeuristic.setSelectedItem(null);
			// we choose a transport at the right site so the left one should also be selected
			// this transport
			cb_ChooseTransport_configurationPanel.getModel().setSelectedItem(
					cb_ChooseTransport_controlPanel.getModel().getSelectedItem());

		} else {

			cb_ChooseTransport_controlPanel.getModel().setSelectedItem(p.getTransport());
			cb_ChooseProfil.getModel().setSelectedItem(p);

			// a profile would selected, so the attributes must be shown
			tf_ProfilName.setText(p.getName());
			cb_ChooseTransport_configurationPanel.getModel().setSelectedItem(p.getTransport());
			cb_ChooseHeuristic.getModel().setSelectedItem(p.getHeuristic());
			tf_Url.setText(p.getUrl());

			// sometimes somebody choose a profile without choosing a transport before. anyway
			// the transport should be shown at the right panel, too.
			// doesn't work

		}

	}

	private void updateProfilToDB() {
		Profil p = null;
		try {
			p = getProfilFromInput();
			int answer = JOptionPane
					.showConfirmDialog(
							null,
							"Sie überschreiben hiermit Ihr altes Profil. Die Daten gehen damit verloren. Möchten Sie das?",
							"Profil überscheben", JOptionPane.YES_NO_OPTION);
			if (answer == 0) {
				dbs.updateProfil(p);
			}
		} catch (IllegalArgumentException e) {
			JOptionPane.showMessageDialog(this, e.getMessage(), "Fehler",
					JOptionPane.ERROR_MESSAGE);
			return;
		} catch (NoSuchElementException e) {
			JOptionPane.showMessageDialog(this, e.getMessage(), "Fehler",
					JOptionPane.ERROR_MESSAGE);
			return;
		}
		setComboBoxChooseProfil(p.getTransport());
	}

	private void createProfilInDB() {
		Profil p = null;
		try {
			p = getProfilFromInput();
			dbs.addProfil(p);
		} catch (IllegalArgumentException e) {
			JOptionPane.showMessageDialog(this, e.getMessage(), "Fehler",
					JOptionPane.ERROR_MESSAGE);
			return;
		}
		setComboBoxChooseTransport();
		setComboBoxChooseProfil(null);
	}

	private void deleteProfilFromDB() {
		String name = tf_ProfilName.getText();
		if (name.equals(null) || name.equals("")) {
			JOptionPane.showMessageDialog(this,
					"Es wurde keine gültiger Name für ein Profil angegeben.", "Fehler",
					JOptionPane.ERROR_MESSAGE);
		} else {
			int answer = JOptionPane.showConfirmDialog(null,
					"Möchten Sie das Profil wirklich löschen?", "Profil löschen",
					JOptionPane.YES_NO_OPTION);

			if (answer == 0) {
				try {
					dbs.deleteProfil(name);
				} catch (NoSuchElementException e) {
					JOptionPane.showMessageDialog(this, e.getMessage(), "Fehler",
							JOptionPane.ERROR_MESSAGE);
				}
				setComboBoxChooseTransport();
				setComboBoxChooseProfil(null);
			}
		}

	}

	private Profil getProfilFromInput() {

		String profilName;
		Transport transport = null;
		String heuristic = null;
		String url = null;
		profilName = tf_ProfilName.getText();
		url = tf_Url.getText();
		if (profilName.equals("")) {
			throw new IllegalArgumentException(
					"Es wurde keine gültiger Name für ein Profil angegeben.");
		}

		if (url.equals("")) {
			throw new IllegalArgumentException("Es würde keine gültige Url angegeben.");
		}

		transport = (Transport) cb_ChooseTransport_configurationPanel.getSelectedItem();

		if (transport == null) {
			throw new IllegalArgumentException(
					"Es wurde kein gültiges Transportmittel ausgewählt.");
		}
		heuristic = "no kein String auslesbar";
		return new Profil(profilName, url, transport, heuristic);
	}

	private void setComboBoxChooseProfil(Transport transport) {
		cb_ChooseProfil.removeAllItems();
		ArrayList<Profil> al_profils = null;
		if (transport == null) {
			// no transport specified, so all profiles would be shown
			al_profils = dbs.getAllProfiles();
		} else {
			// show all profiles of transport
			al_profils = dbs.getProfilesOfTransport(transport);
		}
		if (al_profils.size() > 0) {
			// if there are any profiles for this transport, we add them to the combobox
			cb_ChooseProfil.addItem(null);
			for (Profil p : al_profils) {
				cb_ChooseProfil.addItem(p);
			}
		} else {
			// there are no profiles for this transport or no profiles at all. so the user would
			// be informed
			String message;
			if (transport == null) {
				message = " There exists no profils in the Database. Please create one at first.";
			} else {
				message = "There exists no profils for the selectet transport "
						+ transport.getName() + "Please create on at first.";
			}

			JOptionPane.showMessageDialog(this, message, "Hinweis",
					JOptionPane.INFORMATION_MESSAGE);
		}
	}

	private void setComboBoxChooseTransport() {
		cb_ChooseTransport_controlPanel.removeAllItems();
		cb_ChooseTransport_configurationPanel.removeAllItems();
		ArrayList<Transport> al_transports = dbs.getAllTransports();

		if (al_transports != null) {
			cb_ChooseTransport_controlPanel.addItem(null);
			for (Transport t : al_transports) {
				cb_ChooseTransport_controlPanel.addItem(t);
				cb_ChooseTransport_configurationPanel.addItem(t);
			}
		}

	}

	/*
	 * private void chooseUrlFile() { int returnVal = fc.showOpenDialog(ProfilPanel.this);
	 * 
	 * if (returnVal == JFileChooser.APPROVE_OPTION) {
	 * tf_Url.setText(fc.getSelectedFile().toString()); } }
	 */
	private void createNewConfigurationFile() {

		Transport transport = (Transport) cb_ChooseTransport_controlPanel.getSelectedItem();
		DatabaseProperties probs = new DatabaseProperties("localhost", 5432, "osm_base",
				"postgres", "bachelor");
		Profil newProfil = new Profil(tf_ProfilName.getText(), tf_Url.getText(), transport,
				cb_ChooseHeuristic.getSelectedItem().toString(), probs);
		SimpleRoutingConfigurationWriter writer = new SimpleRoutingConfigurationWriter(
				newProfil);
		try {
			writer.createFile();
		} catch (Exception e) {
			JOptionPane.showMessageDialog(this, e.getMessage(), "Error",
					JOptionPane.ERROR_MESSAGE);
		}

	}
}
