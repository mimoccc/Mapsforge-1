package org.mapsforge.android.map;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.drawable.Drawable;

public class ArrayItemizedOverlay extends ItemizedOverlay {

	private ArrayList<OverlayItem> mOverlays = new ArrayList<OverlayItem>();
	private Context context;

	public ArrayItemizedOverlay(Drawable defaultMarker, Context context) {
		super(defaultMarker);
		this.context = context;
	}

	@Override
	public void addOverLay(OverlayItem overlayItem) {
		this.mOverlays.add(overlayItem);
	}

	@Override
	protected OverlayItem createItem(int i) {
		return mOverlays.get(i);
	}

	@Override
	protected boolean onTap(int index) {
		OverlayItem item = mOverlays.get(index);
		AlertDialog.Builder dialog = new AlertDialog.Builder(context);
		dialog.setTitle(item.getTitle());
		dialog.setMessage(item.getSnippet());
		dialog.show();
		return false;
	}

	@Override
	public int size() {
		return mOverlays.size();
	}
}
