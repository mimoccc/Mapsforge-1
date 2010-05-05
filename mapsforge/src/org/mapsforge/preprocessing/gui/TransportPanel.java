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
package org.mapsforge.preprocessing.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Vector;

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

public class TransportPanel extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7494776847583748626L;
	protected DatabaseService dbs;
	
	private JComboBox cb_ChooseTransport;
	private JTextField tf_TransportName;
	private JFormattedTextField ftf_TransportMaxSpeed;
	private JList jl_TransportUseableWays;
	private JList jl_AllUseableWays;
	private ArrayList<Transport> al_transports;
	private DefaultListModel dlm_listModel;

	public TransportPanel(DatabaseService dbs) {

		this.dbs = dbs;
		this.setLayout(new BorderLayout());

		this.add(getLeftPanel(), BorderLayout.WEST);
		this.add(getRightPanel(), BorderLayout.EAST);
		setComboBoxChooseTransport();
		setJListAllUseabaleWayTags();
	}

	private JPanel getLeftPanel() {

		JPanel leftPanel = new JPanel(new GridBagLayout());
		GridBagConstraints leftPanelConstraints = new GridBagConstraints();

		// set constraints
		leftPanelConstraints.insets = new Insets(5, 5, 0, 5);
		leftPanelConstraints.anchor = GridBagConstraints.NORTH;
		leftPanelConstraints.fill = GridBagConstraints.HORIZONTAL;

		// add labels to the first row
		leftPanel.add(new JLabel("Name: "), leftPanelConstraints);
		leftPanelConstraints.gridx = 2;
		leftPanel.add(new JLabel("maximale Geschwindigkeit: "), leftPanelConstraints);

		// create textfields
		tf_TransportName = new JTextField();
		ftf_TransportMaxSpeed = new JFormattedTextField(NumberFormat.getNumberInstance());

		// add textfields to the second row
		leftPanelConstraints.gridy = 1;
		leftPanelConstraints.gridx = 0;
		leftPanel.add(tf_TransportName, leftPanelConstraints);
		leftPanelConstraints.gridx = 2;
		leftPanel.add(ftf_TransportMaxSpeed, leftPanelConstraints);

		// add labels to the third row
		leftPanelConstraints.gridy = 3;
		leftPanelConstraints.gridx = 0;
		leftPanel.add(new JLabel("nutzbare Wege:"), leftPanelConstraints);
		leftPanelConstraints.gridx = 2;
		leftPanel.add(new JLabel("alle verfügbaren Wege:"), leftPanelConstraints);

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
		leftPanelConstraints.gridx = 1;
		leftPanelConstraints.gridy = 4;
		leftPanel.add(b_AddTagToTransport, leftPanelConstraints);
		leftPanelConstraints.gridy = 5;
		leftPanel.add(b_RemoveTagFromTransport, leftPanelConstraints);

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
		leftPanelConstraints.insets = new Insets(5, 5, 5, 5);
		leftPanelConstraints.weighty = 1.0;
		leftPanelConstraints.gridy = 4;
		leftPanelConstraints.gridheight = 2;

		// add lists into the last row
		leftPanelConstraints.gridx = 0;
		leftPanel.add(transportUseableWaysScrollPane, leftPanelConstraints);
		leftPanelConstraints.gridx = 2;
		leftPanel.add(allUseableWaysScrollPane, leftPanelConstraints);

		return leftPanel;
	}

	private JPanel getRightPanel() {

		JPanel rightPanel = new JPanel(new GridBagLayout());
		GridBagConstraints rightPanelConstraints = new GridBagConstraints();

		rightPanelConstraints.insets = new Insets(5, 5, 0, 5);
		rightPanelConstraints.anchor = GridBagConstraints.NORTH;
		rightPanelConstraints.fill = GridBagConstraints.HORIZONTAL;

		// add combobox to the panel
		cb_ChooseTransport = new JComboBox();
		rightPanel.add(cb_ChooseTransport, rightPanelConstraints);

		// add buttons to the panel
		rightPanelConstraints.gridy = 1;
		JButton bSaveTransport = new JButton("Transportmittel speichern");
		rightPanel.add(bSaveTransport, rightPanelConstraints);
		rightPanelConstraints.gridy = 2;
		JButton bCreateTransport = new JButton("Transportmittel anlegen");
		rightPanel.add(bCreateTransport, rightPanelConstraints);
		rightPanelConstraints.gridy = 3;
		rightPanelConstraints.insets = new Insets(5, 5, 5, 5);
		rightPanelConstraints.weighty = 1.0;
		JButton bDeleteTransport = new JButton("Transportmittel löschen");
		rightPanel.add(bDeleteTransport, rightPanelConstraints);
		
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

		return rightPanel;
	}

	/**
	 * @param selectedValues
	 */
	private void addTagsToList(Object[] selectedValues) {
		
		for(Object obj : selectedValues)
		{
			if(dlm_listModel.lastIndexOf(obj) == -1)
			{
				dlm_listModel.addElement(obj.toString());
			}
			else
			{
				JOptionPane.showMessageDialog(this,"Dieser Tag ist schon in der Liste.");
			}
		}
	}

	private void deleteTagsFromList(Object[] selectedValues) {
		
		for(Object obj : selectedValues)
		{
			dlm_listModel.removeElement(obj);
		}
	}

	private void createTransportInDB() {
		
		Transport t = null;
		try {
			t = getTransportFromInput();
			dbs.addTransport(t);
		}
		catch (IllegalArgumentException e) {
			JOptionPane.showMessageDialog(this,e.getMessage(),"Fehler",JOptionPane.ERROR_MESSAGE);
			return;
		}
		setComboBoxChooseTransport();
	}

	private void updateTransportToDB() {
		Transport t = null;
		try {
			t = getTransportFromInput();
			dbs.updateTransport(t);
		}
		catch (IllegalArgumentException e) {
			JOptionPane.showMessageDialog(this,e.getMessage(),"Fehler",JOptionPane.ERROR_MESSAGE);
			return;
		} 
		catch (NoSuchElementException e) {
			JOptionPane.showMessageDialog(this,e.getMessage(),"Fehler",JOptionPane.ERROR_MESSAGE);
		}
	}

	private void deleteTransportFromDB() {
		String name = tf_TransportName.getText();
		if (name.equals(null) || name.equals(""))
		{
			JOptionPane.showMessageDialog(this,"Es wurde keine gültiger Name für ein Transportmittel angegeben.","Fehler",JOptionPane.ERROR_MESSAGE);
		}
		else
		{
			 int answer = JOptionPane.showConfirmDialog(null, 
				      "Möchten Sie das Transportmittel wirklich löschen?", 
				      "Transportmittel löschen", 
				      JOptionPane.YES_NO_OPTION); 
				 
			 if (answer == 0) 
			 {
				 try {
					 dbs.deleteTransport(name);
				 } 
				 catch(NoSuchElementException e) {	
					JOptionPane.showMessageDialog(this,e.getMessage(),"Fehler",JOptionPane.ERROR_MESSAGE);
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
		if(name.equals(null) || name.equals("")) {
			throw new IllegalArgumentException("Es wurde keine gültiger Name für ein Transportmittel angegeben.");
			//JOptionPane.showMessageDialog(this,"Es wurde keine gültiger Name für ein Transportmittel angegeben.","Fehler",JOptionPane.ERROR_MESSAGE);
			//return null;
		}
		try {
			maxspeed = ((Number)ftf_TransportMaxSpeed.getValue()).intValue();
		}
		catch (Exception e) {
			throw new IllegalArgumentException("Es wurde kein Wert für die maximale Geschwindkeit angegeben.");
			//JOptionPane.showMessageDialog(this,"Es wurde kein Wert für die maximale Geschwindkeit angegeben.","Fehler",JOptionPane.ERROR_MESSAGE);
			//return null;
		}
		
		if(maxspeed <= 0) {
			throw new IllegalArgumentException("Es wurde ein ungültiger Wert für die maximale Geschwindkeit angegeben.");
//			JOptionPane.showMessageDialog(this,"Es wurde ein ungültiger Wert für die maximale Geschwindkeit angegeben.","Fehler",JOptionPane.ERROR_MESSAGE);
//			return null;
		}
		Enumeration<?> e = dlm_listModel.elements();
		while(e.hasMoreElements())
		{
			ways.add(e.nextElement().toString());
			
		}
		if(ways.size() == 0) {
			throw new IllegalArgumentException("Dem Transportmittel wurden keine Wege zugeordnet.");
//			JOptionPane.showMessageDialog(this,"Dem Transportmittel wurden keine Wege zugeordnet.","Fehler",JOptionPane.ERROR_MESSAGE);
//			return null;
		}
		
		return new Transport(name, maxspeed,StringListToTagList(ways));
	}
	
	private List<Tag> StringListToTagList(ArrayList<String> ways) {
		ArrayList<Tag> result = new ArrayList<Tag>();
		for (String way : ways) {
			//System.out.println(way.split("=")[0]+":"+way.split("=")[1]);
			result.add(new Tag(way.split("=")[0],way.split("=")[1]));
		}
		return result;
	}

	private void setComboBoxChooseTransport() {

		cb_ChooseTransport.removeAllItems();
		al_transports = dbs.getAllTransports();
		
		if (al_transports != null) 
		{
			cb_ChooseTransport.addItem(null);
			for (Transport t : al_transports) {
				cb_ChooseTransport.addItem(t);
			}
		}
	}

	private void setJListAllUseabaleWayTags() {
		// TODO Auto-generated method stub
		// just test data
		Vector<String> list = new Vector<String>();

		String teststring1 = "key=value";
		String teststring2 = "key=value2";
		list.add(teststring1);
		list.add(teststring2);
		jl_AllUseableWays.setListData(list);

		// this method must read a file where all useable tags are listed and write them into
		// the allUseableWaysList
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
			for(Tag tag : t.getUseableWays())
			{
				dlm_listModel.addElement(tag.toString());
			}
		}

	}

}
