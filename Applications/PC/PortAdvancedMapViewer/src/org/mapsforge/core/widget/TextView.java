package org.mapsforge.core.widget;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.mapsforge.core.view.View;

public class TextView extends View {

	private static final long serialVersionUID = 1563252214530185344L;
	
	public TextView() {
		super();
		super.setEnabled(false);
	}
	
	public TextView(String context) {
		super(context);
		super.setEnabled(false);
	}

	public void setText(String text) {
		super.setText(text);
	}

	public void setText(double text) {
		super.setText(String.valueOf(text));
	}

	public void setLines(int i) {
		super.setVerticalAlignment(i);
	}

	public void setPadding(int left, int top, int right, int bottom) {
		super.setBorder(BorderFactory.createEmptyBorder(top, left, bottom, right));
	}

	public void setCompoundDrawablesWithIntrinsicBounds(int left, int top,
			int right, int bottom) {
		Icon icon = getIconByID(top);
		super.setIcon(icon);	
	}

	public void setGravity(int i) {
		super.setAlignmentX(i);
	}
	
	private ImageIcon getIconByID(int id) {
		// TODO Auto-generated method stub
		return null;
	}

}
