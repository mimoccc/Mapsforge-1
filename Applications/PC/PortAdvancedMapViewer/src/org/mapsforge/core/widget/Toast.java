package org.mapsforge.core.widget;

import java.awt.print.PrinterException;

import javax.swing.JTextField;

import org.mapsforge.core.content.Context;

public class Toast extends JTextField {
	
	String text;
	
	private static final long serialVersionUID = -120569763199076565L;
	public static final int LENGTH_LONG = 0;

	public static Toast makeText(Context context,
			String text, String lengthLong) {
		// TODO Auto-generated method stub
		return null;
	}

	public void setText(String text) {
		this.text = text;
		super.setText(text);
	}

	public void show() {
		try {
			super.print();
		} catch (PrinterException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
