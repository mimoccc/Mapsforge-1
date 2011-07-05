package org.mapsforge.core.app;

import javax.swing.JFrame;

import org.mapsforge.core.os.Bundle;
import org.mapsforge.core.os.Handler;
import org.mapsforge.core.view.Menu;
import org.mapsforge.core.view.MenuInflater;
import org.mapsforge.core.view.MenuItem;
import org.mapsforge.core.view.Window;
import org.mapsforge.core.content.Context;
import org.mapsforge.core.content.Intent;
import org.mapsforge.core.graphics.Canvas;

import org.mapsforge.core.graphics.Paint;

//import android.graphics.Paint;

public class Activity extends Context {
	
	Window mWindow = null;
	boolean mCalled = false;
	boolean mFinished = false;
	MenuInflater inflater = null;
	Thread mUiThread = Thread.currentThread();
    //private final Handler mHandler = new Handler();

	Activity mParent;
	
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
	
	

	/**
     * Check to see whether this activity is in the process of finishing,
     * either because you called {@link #finish} on it or someone else
     * has requested that it finished.  This is often used in
     * {@link #onPause} to determine whether the activity is simply pausing or
     * completely finishing.
     *
     * @return If the activity is finishing, returns true; else returns false.
     *
     * @see #finish
     */
    public boolean isFinishing() {
        return mFinished;
    }
    
    /**
     * Call this when your activity is done and should be closed.  The
     * ActivityResult is propagated back to whoever launched you via
     * onActivityResult().
     */
    public void finish() {        
    	mFinished = true;
    }
	
	public void showDialog(int id) {
		//TODO
	}
	
	/**
     * This hook is called whenever an item in your options menu is selected.
     * The default implementation simply returns false to have the normal
     * processing happen (calling the item's Runnable or sending a message to
     * its Handler as appropriate).  You can use this method for any items
     * for which you would like to do processing without those other
     * facilities.
     *
     * <p>Derived classes should call through to the base class for it to
     * perform the default menu handling.
     *
     * @param item The menu item that was selected.
     *
     * @return boolean Return false to allow normal menu processing to
     *         proceed, true to consume it here.
     *
     * @see #onCreateOptionsMenu
     */
	public boolean onOptionsItemSelected(MenuItem item) {
        if (mParent != null) {
            return mParent.onOptionsItemSelected(item);
        }
        return false;
    }
		
	/**
     * Initialize the contents of the Activity's standard options menu.  You
     * should place your menu items in to <var>menu</var>.
     *
     * <p>This is only called once, the first time the options menu is
     * displayed.  To update the menu every time it is displayed, see
     * {@link #onPrepareOptionsMenu}.
     *
     * <p>The default implementation populates the menu with standard system
     * menu items.  These are placed in the {@link Menu#CATEGORY_SYSTEM} group so that
     * they will be correctly ordered with application-defined menu items.
     * Deriving classes should always call through to the base implementation.
     *
     * <p>You can safely hold on to <var>menu</var> (and any items created
     * from it), making modifications to it as desired, until the next
     * time onCreateOptionsMenu() is called.
     *
     * <p>When you add items to the menu, you can implement the Activity's
     * {@link #onOptionsItemSelected} method to handle them there.
     *
     * @param menu The options menu in which you place your items.
     *
     * @return You must return true for the menu to be displayed;
     *         if you return false it will not be shown.
     *
     * @see #onPrepareOptionsMenu
     * @see #onOptionsItemSelected
     */
    public boolean onCreateOptionsMenu(Menu menu) {
        if (mParent != null) {
            return mParent.onCreateOptionsMenu(menu);
        }
        return true;
    }
    
    /**
     * Prepare the Screen's standard options menu to be displayed.  This is
     * called right before the menu is shown, every time it is shown.  You can
     * use this method to efficiently enable/disable items or otherwise
     * dynamically modify the contents.
     *
     * <p>The default implementation updates the system menu items based on the
     * activity's state.  Deriving classes should always call through to the
     * base class implementation.
     *
     * @param menu The options menu as last shown or first initialized by
     *             onCreateOptionsMenu().
     *
     * @return You must return true for the menu to be displayed;
     *         if you return false it will not be shown.
     *
     * @see #onCreateOptionsMenu
     */
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (mParent != null) {
            return mParent.onPrepareOptionsMenu(menu);
        }
        return true;
    }

    /**
     * Runs the specified action on the UI thread. If the current thread is the UI
     * thread, then the action is executed immediately. If the current thread is
     * not the UI thread, the action is posted to the event queue of the UI thread.
     *
     * @param action the action to run on the UI thread
     */
    public final void runOnUiThread(Runnable action) {
        if (Thread.currentThread() != 	mUiThread) {
            //mHandler.post(action);
        } else {
            action.run();
        }
    }
    
    /**
     * Launch an activity for which you would like a result when it finished.
     * When this activity exits, your
     * onActivityResult() method will be called with the given requestCode.
     * Using a negative requestCode is the same as calling
     * {@link #startActivity} (the activity is not launched as a sub-activity).
     *
     * <p>Note that this method should only be used with Intent protocols
     * that are defined to return a result.  In other protocols (such as
     * {@link Intent#ACTION_MAIN} or {@link Intent#ACTION_VIEW}), you may
     * not get the result when you expect.  For example, if the activity you
     * are launching uses the singleTask launch mode, it will not run in your
     * task and thus you will immediately receive a cancel result.
     *
     * <p>As a special case, if you call startActivityForResult() with a requestCode
     * >= 0 during the initial onCreate(Bundle savedInstanceState)/onResume() of your
     * activity, then your window will not be displayed until a result is
     * returned back from the started activity.  This is to avoid visible
     * flickering when redirecting to another activity.
     *
     * <p>This method throws {@link android.content.ActivityNotFoundException}
     * if there was no Activity found to run the given Intent.
     *
     * @param intent The intent to start.
     * @param requestCode If >= 0, this code will be returned in
     *                    onActivityResult() when the activity exits.
     *
     * @throws android.content.ActivityNotFoundException
     *
     * @see #startActivity
     */
    public void startActivityForResult(Intent intent, int requestCode) {
        /*if (mParent == null) {
            Instrumentation.ActivityResult ar =
                mInstrumentation.execStartActivity(
                    this, mMainThread.getApplicationThread(), mToken, this,
                    intent, requestCode);
            if (ar != null) {
                mMainThread.sendActivityResult(
                    mToken, mEmbeddedID, requestCode, ar.getResultCode(),
                    ar.getResultData());
            }
            if (requestCode >= 0) {
                // If this start is requesting a result, we can avoid making
                // the activity visible until the result is received.  Setting
                // this code during onCreate(Bundle savedInstanceState) or onResume() will keep the
                // activity hidden during this time, to avoid flickering.
                // This can only be done when a result is requested because
                // that guarantees we will get information back when the
                // activity is finished, no matter what happens to it.
                mStartedActivity = true;
            }
        } else {
            mParent.startActivityFromChild(this, intent, requestCode);
        }*/
    }
    
    /**
     * Called when an activity you launched exits, giving you the requestCode
     * you started it with, the resultCode it returned, and any additional
     * data from it.  The <var>resultCode</var> will be
     * {@link #RESULT_CANCELED} if the activity explicitly returned that,
     * didn't return any result, or crashed during its operation.
     *
     * <p>You will receive this call immediately before onResume() when your
     * activity is re-starting.
     *
     * @param requestCode The integer request code originally supplied to
     *                    startActivityForResult(), allowing you to identify who this
     *                    result came from.
     * @param resultCode The integer result code returned by the child activity
     *                   through its setResult().
     * @param data An Intent, which can return result data to the caller
     *               (various data can be attached to Intent "extras").
     *
     * @see #startActivityForResult
     * @see #createPendingResult
     * @see #setResult(int)
     */
    protected void onActivityResult(int requestCode, int resultCode,
            Intent data) {
    }
    
    /**
     * @deprecated Old no-arguments version of
     * {@link #onPrepareDialog(int, Dialog, Bundle)}.
     */
    @Deprecated
    protected void onPrepareDialog(int id, Dialog dialog) {
        //dialog.setOwnerActivity(this);
    }
    
	public static void main(String[] args) {
		Activity a = new Activity();
		a.onCreate(null);
		Canvas c = new Canvas();
		
		c.drawText("test", 10, 10, new Paint(Paint.ANTI_ALIAS_FLAG));
	}



}
