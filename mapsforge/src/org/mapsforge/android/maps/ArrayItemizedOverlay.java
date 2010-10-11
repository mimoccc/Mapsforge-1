package org.mapsforge.android.maps;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.drawable.Drawable;

/**
 * Implementation of the ItemizedOverlay class using an ArrayList as data structure.
 * 
 * @author Sebastian Schlaak
 * @author Karsten Groll
 */
public class ArrayItemizedOverlay extends ItemizedOverlay {
	private Context context;
	private ArrayList<OverlayItem> overlayItems = new ArrayList<OverlayItem>();

	/**
	 * Constructs an overlay.
	 * 
	 * @param defaultMarker
	 *            the default marker.
	 * @param context
	 *            the context for the alert-dialog.
	 */
	public ArrayItemizedOverlay(Drawable defaultMarker, Context context) {
		super(defaultMarker);
		this.context = context;
	}

	@Override
	public void addOverLay(OverlayItem overlayItem) {
		this.overlayItems.add(overlayItem);
	}

	@Override
	public int size() {
		return this.overlayItems.size();
	}

	@Override
	protected OverlayItem createItem(int i) {
		return this.overlayItems.get(i);
	}

	@Override
	protected boolean onTap(int index) {
		OverlayItem item = this.overlayItems.get(index);
		AlertDialog.Builder dialog = new AlertDialog.Builder(this.context);
		dialog.setTitle(item.getTitle());
		dialog.setMessage(item.getSnippet());
		dialog.show();
		return false;
	}
}