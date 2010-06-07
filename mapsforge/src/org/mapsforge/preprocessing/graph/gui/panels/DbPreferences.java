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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.mapsforge.preprocessing.graph.gui.util.DatabaseService;
import org.mapsforge.preprocessing.graph.model.gui.DatabaseProperties;

public class DbPreferences extends JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2481113734700551000L;

	private DatabaseService dbs;
	private JTextField tf_hostName, tf_database, tf_username, tf_password;
	private JFormattedTextField ftf_port;

	public DbPreferences(DatabaseService dbs) {

		super("Database Preferences");
		this.dbs = dbs;
		this
				.setLocation(
						(Toolkit.getDefaultToolkit().getScreenSize().width - this.getSize().width) / 4,
						(Toolkit.getDefaultToolkit().getScreenSize().height - this.getSize().height) / 4);

		this.setSize(400, 200);
		init();
	}

	private void init() {

		this.getContentPane().setLayout(new BorderLayout());

		this.getContentPane().add(drawInputPanel(), BorderLayout.NORTH);
		this.getContentPane().add(drawButtonPanel(), BorderLayout.SOUTH);
		loadDbConfig();
		this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		this.pack();
	}

	private JPanel drawButtonPanel() {

		JPanel panel = new JPanel(new FlowLayout());
		// GridBagConstraints constraints = new GridBagConstraints();

		JButton b_saveDbConfig = new JButton("Save Database Configuration");
		JButton b_getDefaultDbConfig = new JButton("Restore Default Configuration");
		panel.add(b_getDefaultDbConfig);
		panel.add(b_saveDbConfig);

		b_getDefaultDbConfig.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				restoreToDefaultDbConfig();
			}
		});
		b_saveDbConfig.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				saveDbConfig();
			}
		});

		return panel;
	}

	private JPanel drawInputPanel() {

		JPanel panel = new JPanel(new GridBagLayout());
		GridBagConstraints panelConstraints = new GridBagConstraints();

		panelConstraints.insets = new Insets(5, 5, 0, 5);
		panelConstraints.anchor = GridBagConstraints.NORTH;
		panelConstraints.fill = GridBagConstraints.HORIZONTAL;

		panelConstraints.gridwidth = GridBagConstraints.REMAINDER;
		JLabel l_information = new JLabel(
				"Here you can change the default database configuration.");
		panel.add(l_information, panelConstraints);

		panelConstraints.weightx = 0.2;
		panelConstraints.gridwidth = GridBagConstraints.RELATIVE;

		panelConstraints.gridy = 1;
		panel.add(new JLabel("host:"), panelConstraints);
		panelConstraints.gridy = 2;
		panel.add(new JLabel("port:"), panelConstraints);
		panelConstraints.gridy = 3;
		panel.add(new JLabel("database:"), panelConstraints);
		panelConstraints.gridy = 4;
		panel.add(new JLabel("username:"), panelConstraints);
		panelConstraints.gridy = 5;
		panel.add(new JLabel("password:"), panelConstraints);

		panelConstraints.gridwidth = GridBagConstraints.REMAINDER;
		panelConstraints.weightx = 1.0;

		tf_hostName = new JTextField();
		ftf_port = new JFormattedTextField();
		tf_database = new JTextField();
		tf_username = new JTextField();
		tf_password = new JTextField();

		tf_hostName.setPreferredSize(new Dimension(100, 20));

		panelConstraints.gridx = 1;
		panelConstraints.gridy = 1;
		panel.add(tf_hostName, panelConstraints);

		panelConstraints.gridy = 2;
		panel.add(ftf_port, panelConstraints);

		panelConstraints.gridy = 3;
		panel.add(tf_database, panelConstraints);

		panelConstraints.gridy = 4;
		panel.add(tf_username, panelConstraints);

		panelConstraints.gridy = 5;
		panel.add(tf_password, panelConstraints);

		return panel;
	}

	private void loadDbConfig() {
		drawDbConfig(dbs.getDbConfig());
	}

	private void restoreToDefaultDbConfig() {
		org.mapsforge.preprocessing.graph.model.gui.DatabaseProperties dbProps = new DatabaseProperties(
				"localhost", 5432, "osm_base", "postgres", "bachelor");
		drawDbConfig(dbProps);
		saveDbConfig();
	}

	private void drawDbConfig(DatabaseProperties dbProps) {
		if (dbProps == null) {
			String message = ("Es wurde kein Datenbankkonfiguration gefunden.");
			JOptionPane.showMessageDialog(this, message, "Fehler", JOptionPane.ERROR_MESSAGE);
			return;
		}
		tf_hostName.setText(dbProps.getHost());
		tf_database.setText(dbProps.getDbName());
		tf_username.setText(dbProps.getUsername());
		tf_password.setText(dbProps.getPassword());
		ftf_port.setValue(dbProps.getPort());
	}

	private void saveDbConfig() {

		String host, dbname, username, password;
		int port = 0;

		host = tf_hostName.getText();
		dbname = tf_database.getText();
		username = tf_username.getText();
		password = tf_password.getText();
		try {
			port = ((Number) ftf_port.getValue()).intValue();
		} catch (Exception e) {

			String message = ("Es wurde kein oder ein falscher Wert für den Port angegeben.");
			JOptionPane.showMessageDialog(this, message, "Fehler", JOptionPane.ERROR_MESSAGE);
		}

		if (host == "" || dbname == "" || username == "" || password == "") {

			String message = ("Mindestens ein Feld enthält keinen gültigen Werts.");
			JOptionPane.showMessageDialog(this, message, "Fehler", JOptionPane.ERROR_MESSAGE);
		} else {
			DatabaseProperties dbProps = new DatabaseProperties(host, port, dbname, username,
					password);
			try {
				dbs.addDatabaseConfig(dbProps);
			} catch (Exception e) {
				JOptionPane.showMessageDialog(this, e.getMessage(), "Fehler",
						JOptionPane.ERROR_MESSAGE);
			}
		}
	}
}
