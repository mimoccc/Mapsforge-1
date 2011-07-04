package org.mapsforge.core.view;

import javax.swing.JLabel;

//TODO Drawable.Callback 
//TODO AccessibilityEventSource
public class View extends JLabel implements KeyEvent.Callback {

	private static final long serialVersionUID = -4159533482111214072L;

	boolean clickable;
    boolean focusable;
	
    public View(String context) {
    	super(context);
    }
    
	public View() {
		super();
	}

	protected int mID;
	/**
     * Look for a child view with the given id.  If this view has the given
     * id, return this view.
     *
     * @param id The id to search for.
     * @return The view that has the given id in the hierarchy or null
     */
    public final View findViewById(int id) {
        if (id < 0) {
            return null;
        }
        return findViewTraversal(id);
    }
    
    /**
     * {@hide}
     * @param id the id of the view to be found
     * @return the view of the specified id, null if cannot be found
     */
    protected View findViewTraversal(int id) {
        if (id == mID) {
            return this;
        }
        return null;
    }
    
    /**
     * Enables or disables click events for this view. When a view
     * is clickable it will change its state to "pressed" on every click.
     * Subclasses should set the view clickable to visually react to
     * user's clicks.
     *
     * @param clickable true to make the view clickable, false otherwise
     *
     * @see #isClickable()
     * @attr ref android.R.styleable#View_clickable
     */
    public void setClickable(boolean clickable) {
    	this.clickable = clickable;
    }
    
    /**
     * Set whether this view can receive the focus.
     *
     * Setting this to false will also ensure that this view is not focusable
     * in touch mode.
     *
     * @param focusable If true, this view can receive the focus.
     *
     * @see #setFocusableInTouchMode(boolean)
     * @attr ref android.R.styleable#View_focusable
     */
    public void setFocusable(boolean focusable) {
    	this.focusable = focusable;
    }

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
        //Overrided by MapView
		return false;
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		boolean result = false;
        //Overrided by MapView
        return result;
	}

	/**
	 * Moving map with mouse press
	 */
	@Override
	public boolean onMousePressed(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * Stop moving
	 */
	@Override
	public boolean onMouseReleased(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		return false;
	}

}
