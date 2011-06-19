package org.mapsforge.core.widget;

import javax.swing.BorderFactory;
import javax.swing.JSlider;

import android.content.Context;

public class SeekBar extends JSlider {
	
	private static final long serialVersionUID = 407039293849184286L;

	/**
     * A callback that notifies clients when the progress level has been
     * changed. This includes changes that were initiated by the user through a
     * touch gesture or arrow key/trackball as well as changes that were initiated
     * programmatically.
     */
    public interface OnSeekBarChangeListener {

        /**
         * Notification that the progress level has changed. Clients can use the fromUser parameter
         * to distinguish user-initiated changes from those that occurred programmatically.
         *
         * @param seekBar The SeekBar whose progress has changed
         * @param progress The current progress level. This will be in the range 0..max where max
         *        was set by {@link ProgressBar#setMax(int)}. (The default value for max is 100.)
         * @param fromUser True if the progress change was initiated by the user.
         */
        void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser);

        /**
         * Notification that the user has started a touch gesture. Clients may want to use this
         * to disable advancing the seekbar.
         * @param seekBar The SeekBar in which the touch gesture began
         */
        void onStartTrackingTouch(SeekBar seekBar);

        /**
         * Notification that the user has finished a touch gesture. Clients may want to use this
         * to re-enable advancing the seekbar.
         * @param seekBar The SeekBar in which the touch gesture began
         */
        void onStopTrackingTouch(SeekBar seekBar);
    }
    
    OnSeekBarChangeListener mOnSeekBarChangeListener;

	public SeekBar(Context context) {
		//TODO context
		super();
	}

	public int getProgress() {
		return super.getValue();
	}

	/**
     * Sets a listener to receive notifications of changes to the SeekBar's progress level. Also
     * provides notifications of when the user starts and stops a touch gesture within the SeekBar.
     *
     * @param l The seek bar notification listener
     *
     * @see SeekBar.OnSeekBarChangeListener
     */
    public void setOnSeekBarChangeListener(OnSeekBarChangeListener l) {
        mOnSeekBarChangeListener = l;
    }

	public void setMax(int max) {
		super.setMaximum(max);
	}

	public void setProgress(int value) {
		super.setValue(value);
	}

	public void setKeyProgressIncrement(int increment) {
		super.setMinorTickSpacing(increment);
		super.setMajorTickSpacing(increment);
	}

	public void setPadding(int left, int top, int right, int bottom) {
		super.setBorder(BorderFactory.createEmptyBorder(top, left, bottom, right));
	}

}
