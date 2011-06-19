package org.mapsforge.core.view;

import javax.swing.JMenuItem;

public class MenuItem extends JMenuItem {

	private static final long serialVersionUID = 8157517835595155791L;
	protected int itemID;
	
	public MenuItem(String title) {
		super(title);
	}
	
	public void setItemID(int itemID) {
		this.itemID = itemID;
	}
	
	public int getItemID() {
		return this.itemID;
	}

}
