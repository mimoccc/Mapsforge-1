package org.mapsforge.core.app;

import javax.swing.JFrame;

import org.mapsforge.core.os.Bundle;
import org.mapsforge.core.view.MenuInflater;
import org.mapsforge.core.view.Window;

public class Activity {
	
	Window mWindow;
	boolean mCalled;
	MenuInflater inflater;
	
	 /**
     * Called when the activity is starting.  This is where most initialization
     * should go: calling {@link #setContentView(int)} to inflate the
     * activity's UI, using {@link #findViewById} to programmatically interact
     * with widgets in the UI, calling
     * {@link #managedQuery(android.net.Uri , String[], String, String[], String)} to retrieve
     * cursors for data being displayed, etc.
     *
     * <p>You can call {@link #finish} from within this function, in
     * which case onDestroy() will be immediately called without any of the rest
     * of the activity lifecycle ({@link #onStart}, {@link #onResume},
     * {@link #onPause}, etc) executing.
     *
     * <p><em>Derived classes must call through to the super class's
     * implementation of this method.  If they do not, an exception will be
     * thrown.</em></p>
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
     *
     * @see #onStart
     * @see #onSaveInstanceState
     * @see #onRestoreInstanceState
     * @see #onPostCreate
     */
	protected void onCreate(Bundle savedInstanceState) {
		// Initialize Window
		mWindow = new Window("Mapsforge - AdvancedMapViewer");
		mWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		mWindow.setSize(800, 600);
		mWindow.setVisible(true);
		
		// onCreate() has been called
		mCalled = true;
		
		inflater = getMenuInflater();
		mWindow.setJMenuBar(inflater);

	}
	
	protected void onStart() {
		mCalled = true;
	}
	
	protected void onRestart() {
		mCalled = true;
	}
	
	protected void onResume() {
		mCalled = true;
	}
	
	protected void onPause() {
		mCalled = true;
	}
	
	protected void onStop() {
		mCalled = true;
	}
	
	protected void onDestroy() {
		mCalled = true;
		//TODO
	}
	
	public Window getWindow() {
        return mWindow;
    }
	
	public void setContentView(int layoutResID) {
        getWindow().setContentView(layoutResID);
    }
	
	public MenuInflater getMenuInflater() {
		return new MenuInflater(this);
	}
	
	public static void main(String[] args) {
		Activity a = new Activity();
		a.onCreate(null);
	}
}
