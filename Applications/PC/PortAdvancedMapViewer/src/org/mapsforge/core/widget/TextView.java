package org.mapsforge.core.widget;

import javax.swing.BorderFactory;

import org.mapsforge.core.view.View;

import android.content.Context;

public class TextView extends View {

	private static final long serialVersionUID = 1563252214530185344L;

	public TextView(Context context) {
		this.setEditable(false);
	}

	public void setText(String text) {
		super.setText(text);
	}

	public void setText(double text) {
		super.setText(String.valueOf(text));
	}

	public void setLines(int i) {
		// TODO Auto-generated method stub
	}

	public void setPadding(int left, int top, int right, int bottom) {
		super.setBorder(BorderFactory.createEmptyBorder(top, left, bottom, right));
	}

	public void setCompoundDrawablesWithIntrinsicBounds(int i, int icMenuBack,
			int j, int k) {
		// TODO Auto-generated method stub		
	}

	public void setGravity(int i) {
		super.setAlignmentX(i);
	}

}
