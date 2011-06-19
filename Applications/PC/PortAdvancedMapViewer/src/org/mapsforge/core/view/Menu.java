package org.mapsforge.core.view;

import javax.swing.JMenu;

public class Menu extends JMenu {
	
	private static final long serialVersionUID = -6626819896374671254L;
	
	protected int itemID;
	
	public Menu(String title) {
		super(title);
	}
	
	public void setItemID(int itemID) {
		this.itemID = itemID;
	}
	
	public int getItemID() {
		return this.itemID;
	}
	
}
