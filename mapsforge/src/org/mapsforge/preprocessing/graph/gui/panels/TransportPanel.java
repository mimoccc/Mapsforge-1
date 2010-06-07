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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.TitledBorder;

import org.mapsforge.preprocessing.graph.gui.util.DatabaseService;
import org.mapsforge.preprocessing.graph.model.gui.Transport;
import org.mapsforge.preprocessing.model.EHighwayLevel;
import org.mapsforge.preprocessing.util.HighwayLevelExtractor;

public class TransportPanel extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7494776847583748626L;
	private DatabaseService dbs;

	private JComboBox cb_ChooseTransport;
	private JTextField tf_TransportName;
	private JFormattedTextField ftf_TransportMaxSpeed;
	private JList jl_TransportUseableWays;
	private JList jl_AllUseableWays;
	private DefaultListModel dlm_listModel;

	public TransportPanel(DatabaseService dbs) {

		this.dbs = dbs;
		this.setLayout(new BorderLayout());

		this.add(getAttributePanel(), BorderLayout.WEST);
		this.add(getManagePanel(), BorderLayout.EAST);
		setComboBoxChooseTransport();
		setJListAllUseabaleWayTags();
	}

	private JPanel getAttributePanel() {

		JPanel panel = new JPanel(new GridBagLayout());
		GridBagConstraints constraints = new GridBagConstraints();

		// add border
		panel.setBorder(BorderFactory.createTitledBorder(null, "transport attributes",
				TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, new Font(
						"Dialog", Font.PLAIN, 11), Color.BLACK));
		// set constraints
		constraints.insets = new Insets(5, 5, 0, 5);
		constraints.anchor = GridBagConstraints.NORTH;
		constraints.fill = GridBagConstraints.HORIZONTAL;

		// add labels to the first row
		panel.add(new JLabel("name: "), constraints);
		constraints.gridx = 2;
		panel.add(new JLabel("maximum speed: "), constraints);

		// create textfields
		tf_TransportName = new JTextField();
		ftf_TransportMaxSpeed = new JFormattedTextField(NumberFormat.getNumberInstance());

		// add textfields to the second row
		constraints.gridy = 1;
		constraints.gridx = 0;
		panel.add(tf_TransportName, constraints);
		constraints.gridx = 2;
		panel.add(ftf_TransportMaxSpeed, constraints);

		// add labels to the third row
		constraints.gridy = 3;
		constraints.gridx = 0;
		panel.add(new JLabel("useabel ways:"), constraints);
		constraints.gridx = 2;
		panel.add(new JLabel("avaiable ways:"), constraints);

		// create buttons and actions listeners
		JButton b_RemoveTagFromTransport = new JButton(">>");
		b_RemoveTagFromTransport.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				deleteTagsFromList(jl_TransportUseableWays.getSelectedValues());

			}
		});

		JButton b_AddTagToTransport = new JButton("<<");
		b_AddTagToTransport.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				addTagsToList(jl_AllUseableWays.getSelectedValues());

			}
		});

		// add buttons the the second column
		constraints.gridx = 1;
		constraints.gridy = 4;
		panel.add(b_AddTagToTransport, constraints);
		constraints.gridy = 5;
		panel.add(b_RemoveTagFromTransport, constraints);

		// create lists
		jl_AllUseableWays = new JList();
		jl_AllUseableWays.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		jl_AllUseableWays.setLayoutOrientation(JList.VERTICAL);
		jl_AllUseableWays.setVisibleRowCount(1);
		jl_AllUseableWays.setBackground(Color.WHITE);
		JScrollPane allUseableWaysScrollPane = new JScrollPane(jl_AllUseableWays);
		allUseableWaysScrollPane.setPreferredSize(new Dimension(150, 60));

		dlm_listModel = new DefaultListModel();
		jl_TransportUseableWays = new JList(dlm_listModel);
		jl_TransportUseableWays
				.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		jl_TransportUseableWays.setLayoutOrientation(JList.VERTICAL);
		jl_TransportUseableWays.setVisibleRowCount(1);
		jl_TransportUseableWays.setBackground(Color.WHITE);
		JScrollPane transportUseableWaysScrollPane = new JScrollPane(jl_TransportUseableWays);
		transportUseableWaysScrollPane.setPreferredSize(new Dimension(150, 60));

		// change constraints
		constraints.insets = new Insets(5, 5, 5, 5);
		constraints.weighty = 1.0;
		constraints.gridy = 4;
		constraints.gridheight = 2;

		// add lists into the last row
		constraints.gridx = 0;
		panel.add(transportUseableWaysScrollPane, constraints);
		constraints.gridx = 2;
		panel.add(allUseableWaysScrollPane, constraints);

		return panel;
	}

	private JPanel getManagePanel() {

		JPanel panel = new JPanel(new GridBagLayout());
		GridBagConstraints constraints = new GridBagConstraints();

		// add border
		panel.setBorder(BorderFactory.createTitledBorder(null, "manage transports",
				TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, new Font(
						"Dialog", Font.PLAIN, 11), Color.BLACK));
		// set constraints
		constraints.insets = new Insets(5, 5, 0, 5);
		constraints.anchor = GridBagConstraints.NORTH;
		constraints.fill = GridBagConstraints.HORIZONTAL;

		// add combobox to the panel
		cb_ChooseTransport = new JComboBox();
		panel.add(cb_ChooseTransport, constraints);

		// add buttons to the panel
		constraints.gridy = 1;
		JButton bSaveTransport = new JButton("save existing configuration");
		panel.add(bSaveTransport, constraints);
		constraints.gridy = 2;
		JButton bCreateTransport = new JButton("create a new configuration");
		panel.add(bCreateTransport, constraints);
		constraints.gridy = 3;
		constraints.insets = new Insets(5, 5, 5, 5);
		constraints.weighty = 1.0;
		JButton bDeleteTransport = new JButton("delete existing configuration");
		panel.add(bDeleteTransport, constraints);

		// add action listener to the combobox
		cb_ChooseTransport.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				showChoosenTransport((Transport) cb_ChooseTransport.getSelectedItem());
			}
		});

		// add action listeners to the buttons
		bSaveTransport.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				updateTransportToDB();
			}
		});

		bCreateTransport.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				createTransportInDB();
			}
		});

		bDeleteTransport.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				deleteTransportFromDB();
			}
		});

		return panel;
	}

	/**
	 * @param selectedValues
	 */
	private void addTagsToList(Object[] selectedValues) {

		for (Object obj : selectedValues) {
			if (dlm_listModel.lastIndexOf(obj) == -1) {
				dlm_listModel.addElement(obj.toString());
			} else {
				JOptionPane.showMessageDialog(this, "This tag is already in the list.",
						"Information", JOptionPane.INFORMATION_MESSAGE);
			}
		}
	}

	private void deleteTagsFromList(Object[] selectedValues) {

		for (Object obj : selectedValues) {
			dlm_listModel.removeElement(obj);
		}
	}

	private void createTransportInDB() {

		Transport t = null;
		try {
			t = getTransportFromInput();
			dbs.addTransport(t);
		} catch (IllegalArgumentException e) {
			JOptionPane.showMessageDialog(this, e.getMessage(), "Error",
					JOptionPane.ERROR_MESSAGE);
			return;
		}
		setComboBoxChooseTransport();
	}

	private void updateTransportToDB() {
		Transport t = null;
		try {
			t = getTransportFromInput();
			dbs.updateTransport(t);
		} catch (IllegalArgumentException e) {
			JOptionPane.showMessageDialog(this, e.getMessage(), "Fehler",
					JOptionPane.ERROR_MESSAGE);
			return;
		} catch (NoSuchElementException e) {
			JOptionPane.showMessageDialog(this, e.getMessage(), "Fehler",
					JOptionPane.ERROR_MESSAGE);
		}
		setComboBoxChooseTransport();
		showChoosenTransport(t);

	}

	private void deleteTransportFromDB() {
		String name = tf_TransportName.getText();
		if (name.equals(null) || name.equals("")) {
			JOptionPane.showMessageDialog(this, "You insert no value for the transport name.",
					"Error", JOptionPane.ERROR_MESSAGE);
		} else {
			int answer = JOptionPane.showConfirmDialog(null,
					"Are you sure to delete this transport configuration?",
					"Delete Transport Configuration?", JOptionPane.YES_NO_OPTION);

			if (answer == 0) {
				try {
					dbs.deleteTransport(name);
				} catch (NoSuchElementException e) {
					JOptionPane.showMessageDialog(this, e.getMessage(), "Error",
							JOptionPane.ERROR_MESSAGE);
				}
				setComboBoxChooseTransport();
			}
		}
	}

	private Transport getTransportFromInput() {
		String name;
		int maxspeed = -1;
		ArrayList<String> ways = new ArrayList<String>();
		name = tf_TransportName.getText();
		if (name.equals(null) || name.equals("")) {
			throw new IllegalArgumentException("You insert no value for the transport name.");
		}
		try {
			maxspeed = ((Number) ftf_TransportMaxSpeed.getValue()).intValue();
		} catch (Exception e) {
			throw new IllegalArgumentException("You insert no value for the maximum speed.");
		}

		if (maxspeed <= 0) {
			throw new IllegalArgumentException(
					"You insert a invalid value for the maximum speed.");
		}
		Enumeration<?> e = dlm_listModel.elements();
		while (e.hasMoreElements()) {
			ways.add(e.nextElement().toString());

		}
		if (ways.size() == 0) {
			throw new IllegalArgumentException(
					"There are no ways added for this transport configuration.");
		}

		return new Transport(name, maxspeed, StringListToHighwaySet(ways));
	}

	private HashSet<EHighwayLevel> StringListToHighwaySet(ArrayList<String> ways) {
		HashSet<EHighwayLevel> result = new HashSet<EHighwayLevel>();
		EHighwayLevel hwyLvl = null;
		for (String way : ways) {
			hwyLvl = HighwayLevelExtractor.getLevel(way);
			if (hwyLvl != null && !result.contains(hwyLvl))
				result.add(hwyLvl);
		}
		return result;
	}

	private void setComboBoxChooseTransport() {

		cb_ChooseTransport.removeAllItems();
		ArrayList<Transport> al_transports = dbs.getAllTransports();

		if (al_transports != null) {
			cb_ChooseTransport.addItem(null);
			for (Transport t : al_transports) {
				cb_ChooseTransport.addItem(t);
			}
		}
	}

	private Vector<String> getListOfAllWays() throws IOException {

		// parse file
		String uri = System.getProperty("user.dir");
		File file = new File(uri + "\\res\\conf\\allWays.conf");
		BufferedReader br;
		Vector<String> hwyLvls = new Vector<String>();
		if (file.exists()) {

			br = new BufferedReader(new FileReader(file));
			EHighwayLevel hwyLvl;
			String input = br.readLine();
			while (input != null) {
				hwyLvl = HighwayLevelExtractor.getLevel(input.split("=")[1]);
				if (hwyLvl != EHighwayLevel.unmapped)
					hwyLvls.add(hwyLvl.toString());
				input = br.readLine();
			}

		} else {
			System.out.println("Can't finde a needed ressource. " + file.getPath());
			System.exit(-1);
		}

		// hole für jeden eintrag den ehighwaylvl

		// zum string machen und dem vector anhängen

		return hwyLvls;
	}

	private void setJListAllUseabaleWayTags() {
		try {
			jl_AllUseableWays.setListData(getListOfAllWays());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void showChoosenTransport(Transport t) {

		if (t == null) {
			tf_TransportName.setText("");
			ftf_TransportMaxSpeed.setValue(null);
			dlm_listModel.clear();

		} else {
			tf_TransportName.setText(t.getName());
			ftf_TransportMaxSpeed.setValue(t.getMaxSpeed());
			dlm_listModel.clear();
			for (EHighwayLevel hwhLvl : t.getUseableWays()) {
				dlm_listModel.addElement(hwhLvl.toString());
			}
		}

	}

}
