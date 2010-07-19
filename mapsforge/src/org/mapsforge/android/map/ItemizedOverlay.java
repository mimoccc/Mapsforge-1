package org.mapsforge.android.map;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;

/**
 * @author Sebastian Schlaak
 * @author Karsten Groll
 */
public class ItemizedOverlay extends Overlay {
	/**
	 * Custom implementation of the ItemizedOverlay class from the Google Maps library.
	 */
	private Drawable defaultMarker;
	private Canvas bitmapWrapper;
	private Bitmap bitmap;
	private Bitmap shaddowBitmap;
	private Bitmap tempBitmapForSwap;
	private ArrayList<OverlayItem> overlayItems;
	private Drawable itemMarker;
	private Context context;
	private Point itemPixelPositon;
	private Point itemPosOnDisplay;
	private Point displayPositonBeforeDrawing;
	private Point displayPositonAfterDrawing;
	private Matrix matrix;

	/**
	 * construct an Overlay
	 * 
	 * @param defaultMarker
	 *            The default drawable for each item in the overlay.
	 * @param context
	 *            The context.
	 */
	public ItemizedOverlay(Drawable defaultMarker, Context context) {
		this.defaultMarker = defaultMarker;
		this.context = context;
		setup();
	}

	private void setup() {
		this.matrix = new Matrix();
		this.overlayItems = new ArrayList<OverlayItem>();
		this.start();
	}

	/**
	 * 
	 * @return numbers of items in this overlay.
	 */
	public int size() {
		return this.overlayItems.size();
	}

	/**
	 * Pause the Thread.
	 * 
	 * @param pauseInSeconds
	 *            Time in seconds to sleep.
	 */
	public void pause(int pauseInSeconds) {
		try {
			Thread.sleep(pauseInSeconds * 1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Add an overlayItem to this overlay.
	 * 
	 * @param overlayItem
	 *            The overlay item.
	 */
	public void addOverLay(OverlayItem overlayItem) {
		this.overlayItems.add(overlayItem);
	}

	@Override
	public boolean onTouchEvent(android.view.MotionEvent event, MapView mapView) {
		OverlayItem item;
		for (int i = 0; i < size(); i++) {
			item = createItem(i);
			if (hitTest(item, item.getMarker(), (int) event.getX(), (int) event.getY())) {
				onTap(i);
				return true;
			}
		}
		return true;
	}

	/**
	 * Access and create the actual Items.
	 * 
	 * @param i
	 *            The index of the item.
	 * @return The overlay item.
	 */
	protected OverlayItem createItem(int i) {
		return this.overlayItems.get(i);
	}

	/**
	 * Adjusts a drawable of an item so that (0,0) is the center.
	 * 
	 * @param balloon
	 *            The drawable to center.
	 * @param itemPosRelative
	 *            The position of the item.
	 * @return The adjusted drawable.
	 */
	protected Drawable boundCenter(Drawable balloon, Point itemPosRelative) {
		balloon.setBounds((int) itemPosRelative.x - balloon.getIntrinsicWidth() / 2,
				(int) itemPosRelative.y - balloon.getIntrinsicHeight() / 2,
				(int) itemPosRelative.x + balloon.getIntrinsicWidth() / 2,
				(int) itemPosRelative.y + balloon.getIntrinsicHeight() / 2);
		return balloon;
	}

	/**
	 * Adjusts the drawable of an item so that (0,0) is the center of the bottom row.
	 * 
	 * @param balloon
	 *            The drawable to center.
	 * @param itemPosRelative
	 *            The position of the item.
	 * @return The adjusted drawable
	 */
	protected Drawable boundCenterBottom(Drawable balloon, Point itemPosRelative) {
		balloon.setBounds((int) itemPosRelative.x - balloon.getIntrinsicWidth() / 2,
				(int) itemPosRelative.y - balloon.getIntrinsicHeight(), (int) itemPosRelative.x
						+ balloon.getIntrinsicWidth() / 2, (int) itemPosRelative.y);
		return balloon;
	}

	/**
	 * Handle a tap event.
	 * 
	 * @param index
	 *            The position of the item.
	 * 
	 * @return true
	 */
	protected boolean onTap(int index) {
		AlertDialog.Builder dialog = new AlertDialog.Builder(this.context);
		dialog.setTitle(this.overlayItems.get(index).getTitle());
		dialog.setMessage(this.overlayItems.get(index).getSnippet());
		dialog.show();
		return true;
	}

	@Override
	public void draw(Canvas canvas, MapView mapView, boolean shadow) {
		canvas.drawBitmap(this.bitmap, this.matrix, null);
	}

	@Override
	final protected void createOverlayBitmapsAndCanvas(int width, int height) {
		this.bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		this.shaddowBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		this.bitmapWrapper = new Canvas();
	}

	/**
	 * Calulate if a given point is within the bounds of an item.
	 * 
	 * @param item
	 *            The item to test.
	 * @param marker
	 *            The marker of the item.
	 * @param hitX
	 *            The x-coordinate of the point.
	 * @param hitY
	 *            The y-coordinate of the point.
	 * @return true, if the point is within the bounds of the item.
	 */
	protected boolean hitTest(OverlayItem item, Drawable marker, int hitX, int hitY) {
		Point eventPos = new Point(hitX, hitY);
		Point itemHitPosOnDisplay = calculateItemPostionRelativeToDisplay(item.getPoint());
		Point distance = Point.substract(eventPos, itemHitPosOnDisplay);
		if (marker == null) {
			marker = this.defaultMarker;
		}
		if (Math.abs(distance.x) < marker.getIntrinsicWidth() / 2
				&& Math.abs(distance.y) < marker.getIntrinsicHeight() / 2) {
			return true;
		}
		return false;
	}

	private Point calculateItemPostionRelativeToDisplay(GeoPoint itemPostion) {
		Point itemPixelPosition = calculateItemPoint(itemPostion);
		Point displayPixelPosition = calculateDisplayPoint(new GeoPoint(this.mapView.latitude,
				this.mapView.longitude));
		Point distance = Point.substract(itemPixelPosition, displayPixelPosition);
		return distance;
	}

	@Override
	protected Matrix getMatrix() {
		return this.matrix;
	}

	@Override
	final protected void prepareOverlayBitmap(MapView mapView) {
		if (!isPrepareOverlayIsNecessary())
			return;
		saveDisplayPositionBeforeDrawing();
		drawItemsOnShaddowBitmap();
		saveDisplayPositionAfterDrawing();
		swapBitmapAndCorrectMatrix(this.displayPositonBeforeDrawing,
				this.displayPositonAfterDrawing);
		notifyMapViewToRedraw();
	}

	private boolean isPrepareOverlayIsNecessary() {
		// return (this.displayPositonAfterDrawing == calculateDisplayPoint(new GeoPoint(
		// this.mapView.latitude, this.mapView.longitude)));
		return true;
	}

	private void saveDisplayPositionBeforeDrawing() {
		this.displayPositonBeforeDrawing = calculateDisplayPoint(new GeoPoint(
				this.mapView.latitude, this.mapView.longitude));
	}

	private void saveDisplayPositionAfterDrawing() {
		this.displayPositonAfterDrawing = calculateDisplayPoint(new GeoPoint(
				this.mapView.latitude, this.mapView.longitude));
	}

	private void drawItemsOnShaddowBitmap() {
		this.shaddowBitmap.eraseColor(Color.TRANSPARENT);
		this.bitmapWrapper.setBitmap(this.shaddowBitmap);
		for (int i = 0; i < size(); i++) {
			drawItem(createItem(i));
		}
	}

	private void drawItem(OverlayItem item) {
		if (hasValidDisplayPosition(item)) {
			this.itemPixelPositon = item.posOnDisplay;
		} else {
			item.posOnDisplay = calculateItemPoint(item.getPoint());
			this.itemPixelPositon = item.posOnDisplay;
			item.zoomLevel = this.mapView.zoomLevel;
		}
		this.itemPosOnDisplay = Point.substract(this.itemPixelPositon,
				this.displayPositonBeforeDrawing);
		setCostumOrDeaultItemMarker(item);
		if (isItemOnDisplay(this.itemPosOnDisplay)) {
			boundCenter(this.itemMarker, this.itemPosOnDisplay).draw(this.bitmapWrapper);
		}
	}

	private boolean hasValidDisplayPosition(OverlayItem item) {
		boolean displayPositionValid = true;
		displayPositionValid &= (this.mapView.zoomLevel == item.zoomLevel);
		return displayPositionValid;
	}

	private void setCostumOrDeaultItemMarker(OverlayItem item) {
		if (item.getMarker() == null) {
			this.itemMarker = this.defaultMarker;
			item.setMarker(this.defaultMarker, 0);
		} else {
			this.itemMarker = item.getMarker();
		}
	}

	private void swapBitmapAndCorrectMatrix(Point displayPosBefore, Point displayPosAfter) {
		synchronized (this.matrix) {
			this.matrix.reset();
			Point diff = Point.substract(displayPosBefore, displayPosAfter);
			this.matrix.postTranslate(diff.x, diff.y);
			// swap the two MapViewBitmaps
			this.tempBitmapForSwap = this.bitmap;
			this.bitmap = this.shaddowBitmap;
			this.shaddowBitmap = this.tempBitmapForSwap;
		}
	}

	private void notifyMapViewToRedraw() {
		this.mapView.postInvalidate();
	}

	private boolean isItemOnDisplay(Point itemPos) {
		boolean isOnDisplay = true;
		isOnDisplay &= itemPos.x > 0;
		isOnDisplay &= itemPos.x < this.bitmap.getWidth();
		isOnDisplay &= itemPos.y > 0;
		isOnDisplay &= itemPos.y < this.bitmap.getHeight();
		return isOnDisplay;
	}

	private Point calculateItemPoint(GeoPoint geoPoint) {
		return new Point((float) MercatorProjection.longitudeToPixelX(geoPoint.getLongitude(),
				this.mapView.zoomLevel), (float) MercatorProjection.latitudeToPixelY(geoPoint
				.getLatitude(), this.mapView.zoomLevel));
	}

	private Point calculateDisplayPoint(GeoPoint geoPoint) {
		return new Point((float) MercatorProjection.longitudeToPixelX(geoPoint.getLongitude(),
				this.mapView.zoomLevel)
				- this.mapView.getWidth() / 2, (float) MercatorProjection.latitudeToPixelY(
				geoPoint.getLatitude(), this.mapView.zoomLevel)
				- this.mapView.getHeight() / 2);
	}
}
