package org.mapsforge.core.view;

public class KeyEvent {
	
	/** Key code constant: Directional Pad Up key.
     * May also be synthesized from trackball motions. */
	/* KEYBOARD UP */
    public static final int KEYCODE_DPAD_UP         = java.awt.event.KeyEvent.VK_UP;
    /** Key code constant: Directional Pad Down key.
     * May also be synthesized from trackball motions. */
    /* KEYBOARD DOWN */
    public static final int KEYCODE_DPAD_DOWN       = java.awt.event.KeyEvent.VK_DOWN;
    /** Key code constant: Directional Pad Left key.
     * May also be synthesized from trackball motions. */
    /* KEYBOARD LEFT */
    public static final int KEYCODE_DPAD_LEFT       = java.awt.event.KeyEvent.VK_LEFT;
    /** Key code constant: Directional Pad Right key.
     * May also be synthesized from trackball motions. */  
    /* KEYBOARD RIGHT */
    public static final int KEYCODE_DPAD_RIGHT      = java.awt.event.KeyEvent.VK_RIGHT;
	
	public interface Callback {
        /**
         * Called when a key down event has occurred.  If you return true,
         * you can first call {@link KeyEvent#startTracking()
         * KeyEvent.startTracking()} to have the framework track the event
         * through its {@link #onKeyUp(int, KeyEvent)} and also call your
         * {@link #onKeyLongPress(int, KeyEvent)} if it occurs.
         *
         * @param keyCode The value in event.getKeyCode().
         * @param event Description of the key event.
         *
         * @return If you handled the event, return true.  If you want to allow
         *         the event to be handled by the next receiver, return false.
         */
        boolean onKeyDown(int keyCode, KeyEvent event);

        /**
         * Called when a long press has occurred.  If you return true,
         * the final key up will have {@link KeyEvent#FLAG_CANCELED} and
         * {@link KeyEvent#FLAG_CANCELED_LONG_PRESS} set.  Note that in
         * order to receive this callback, someone in the event change
         * <em>must</em> return true from {@link #onKeyDown} <em>and</em>
         * call {@link KeyEvent#startTracking()} on the event.
         *
         * @param keyCode The value in event.getKeyCode().
         * @param event Description of the key event.
         *
         * @return If you handled the event, return true.  If you want to allow
         *         the event to be handled by the next receiver, return false.
         */
        //boolean onKeyLongPress(int keyCode, KeyEvent event);
        /**
         * Called when a key up event has occurred.
         *
         * @param keyCode The value in event.getKeyCode().
         * @param event Description of the key event.
         *
         * @return If you handled the event, return true.  If you want to allow
         *         the event to be handled by the next receiver, return false.
         */
        boolean onKeyUp(int keyCode, KeyEvent event);
        /**
         * Called when multiple down/up pairs of the same key have occurred
         * in a row.
         *
         * @param keyCode The value in event.getKeyCode().
         * @param count Number of pairs as returned by event.getRepeatCount().
         * @param event Description of the key event.
         *
         * @return If you handled the event, return true.  If you want to allow
         *         the event to be handled by the next receiver, return false.
         */
        /* Removed temporary */
        //boolean onKeyMultiple(int keyCode, int count, KeyEvent event);
		
		/* Mouse Click Handler */
		boolean onMousePressed(int keyCode, KeyEvent event);
		boolean onMouseReleased(int keyCode, KeyEvent event);
    }

	int mAction;
	
	/**
     * Retrieve the action of this key event.  May be either
     * {@link #ACTION_DOWN}, {@link #ACTION_UP}, or {@link #ACTION_MULTIPLE}.
     *
     * @return The event action: ACTION_DOWN, ACTION_UP, or ACTION_MULTIPLE.
     */
    public final int getAction() {
        return mAction;
    }
}
