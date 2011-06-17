package org.mapsforge.core.view;

import javax.swing.JFrame;

public class Window extends JFrame {

	private static final long serialVersionUID = 1168665685059769879L;
	int layoutResourceID;

	public Window(String title) {
		super(title);
	}
	
	public void setContentView(int layoutResourceID) {
		this.layoutResourceID = layoutResourceID;
	}
	
}
