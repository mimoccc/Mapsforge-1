package org.mapsforge.core.content;


public interface SharedPreferences {

	public interface Editor {
		/**
         * Commit your preferences changes back from this Editor to the
         * {@link SharedPreferences} object it is editing.  This atomically
         * performs the requested modifications, replacing whatever is currently
         * in the SharedPreferences.
         *
         * <p>Note that when two editors are modifying preferences at the same
         * time, the last one to call commit wins.
         *
         * <p>If you don't care about the return value and you're
         * using this from your application's main thread, consider
         * using {@link #apply} instead.
         *
         * @return Returns true if the new values were successfully written
         * to persistent storage.
         */
		boolean commit();
		
		/**
         * Set an int value in the preferences editor, to be written back once
         * {@link #commit} or {@link #apply} are called.
         *
         * @param key The name of the preference to modify.
         * @param value The new value for the preference.
         *
         * @return Returns a reference to the same Editor object, so you can
         * chain put calls together.
         */
		Editor putInt(String key, int value);
		
		/**
         * Mark in the editor to remove <em>all</em> values from the
         * preferences.  Once commit is called, the only remaining preferences
         * will be any that you have defined in this editor.
         *
         * <p>Note that when committing back to the preferences, the clear
         * is done first, regardless of whether you called clear before
         * or after put methods on this editor.
         *
         * @return Returns a reference to the same Editor object, so you can
         * chain put calls together.
         */
		Editor clear();

		/**
         * Set a String value in the preferences editor, to be written back once
         * {@link #commit} or {@link #apply} are called.
         *
         * @param key The name of the preference to modify.
         * @param value The new value for the preference.
         *
         * @return Returns a reference to the same Editor object, so you can
         * chain put calls together.
         */
		Editor putString(String string, String value);

	}

	/**
     * Retrieve a boolean value from the preferences.
     *
     * @param key The name of the preference to retrieve.
     * @param defValue Value to return if this preference does not exist.
     *
     * @return Returns the preference value if it exists, or defValue.  Throws
     * ClassCastException if there is a preference with this name that is not
     * a boolean.
     *
     * @throws ClassCastException
     */
	boolean getBoolean(String key, boolean defValue);

	/**
     * Checks whether the preferences contains a preference.
     *
     * @param key The name of the preference to check.
     * @return Returns true if the preference exists in the preferences,
     *         otherwise false.
     */
	boolean contains(String key);

	/**
     * Retrieve a String value from the preferences.
     *
     * @param key The name of the preference to retrieve.
     * @param defValue Value to return if this preference does not exist.
     *
     * @return Returns the preference value if it exists, or defValue.  Throws
     * ClassCastException if there is a preference with this name that is not
     * a String.
     *
     * @throws ClassCastException
     */
	String getString(String key, String defValue);

	/**
     * Retrieve an int value from the preferences.
     *
     * @param key The name of the preference to retrieve.
     * @param defValue Value to return if this preference does not exist.
     *
     * @return Returns the preference value if it exists, or defValue.  Throws
     * ClassCastException if there is a preference with this name that is not
     * an int.
     *
     * @throws ClassCastException
     */
	int getInt(String key, int defValue);

	/**
     * Create a new Editor for these preferences, through which you can make
     * modifications to the data in the preferences and atomically commit those
     * changes back to the SharedPreferences object.
     *
     * <p>Note that you <em>must</em> call {@link Editor#commit} to have any
     * changes you perform in the Editor actually show up in the
     * SharedPreferences.
     *
     * @return Returns a new instance of the {@link Editor} interface, allowing
     * you to modify the values in this SharedPreferences object.
     */
	Editor edit();

}
