package org.mapsforge.core.content;

import org.mapsforge.core.ComponentName;
import org.mapsforge.core.os.Bundle;

/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

public class Intent {

	 private ComponentName mComponent;
	 private Bundle mExtras;
	
	/**
     * Create an intent for a specific component.  All other fields (action, data,
     * type, class) are null, though they can be modified later with explicit
     * calls.  This provides a convenient way to create an intent that is
     * intended to execute a hard-coded class name, rather than relying on the
     * system to find an appropriate class for you; see {@link #setComponent}
     * for more information on the repercussions of this.
     *
     * @param packageContext A Context of the application package implementing
     * this class.
     * @param cls The component class that is to be used for the intent.
     *
     * @see #setClass
     * @see #setComponent
     * @see #Intent(String, android.net.Uri , Context, Class)
     */
	public Intent(Context packageContext, Class<?> cls) {
        mComponent = new ComponentName(packageContext, cls);
    }

	/**
     * Retrieve extended data from the intent.
     *
     * @param name The name of the desired item.
     *
     * @return the value of an item that previously added with putExtra()
     * or null if no String value was found.
     *
     * @see #putExtra(String, String)
     */
    public String getStringExtra(String name) {
        //return mExtras == null ? null : mExtras.getString(name);
    	return null;
    }
    
    /**
     * Retrieve the concrete component associated with the intent.  When receiving
     * an intent, this is the component that was found to best handle it (that is,
     * yourself) and will always be non-null; in all other cases it will be
     * null unless explicitly set.
     *
     * @return The name of the application component to handle the intent.
     *
     * @see #resolveActivity
     * @see #setComponent
     */
    public ComponentName getComponent() {
        return mComponent;
    }
	
    /**
     * Retrieves a map of extended data from the intent.
     *
     * @return the map of all extras previously added with putExtra(),
     * or null if none have been added.
     */
    public Bundle getExtras() {
        return (mExtras != null) ? new Bundle(mExtras) : null;
    }

}
