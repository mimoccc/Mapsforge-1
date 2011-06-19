package org.mapsforge.core.content;

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

/*import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;*/
//import android.graphics.Bitmap;
import org.mapsforge.core.graphics.Bitmap;
//import android.graphics.drawable.Drawable;
import org.mapsforge.core.graphics.drawable.Drawable;

import android.net.Uri;
//import android.os.Bundle;
import org.mapsforge.core.os.Bundle;
//import android.os.Handler;
import org.mapsforge.core.os.Handler;
//import android.os.Looper;
import org.mapsforge.core.ComponentName;
import org.mapsforge.core.Looper;
//import android.util.AttributeSet;
import org.mapsforge.core.util.AttributeSet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Interface to global information about an application environment.  This is
 * an abstract class whose implementation is provided by
 * the Android system.  It
 * allows access to application-specific resources and classes, as well as
 * up-calls for application-level operations such as launching activities,
 * broadcasting and receiving intents, etc.
 */
public abstract class Context {
    /**
     * File creation mode: the default mode, where the created file can only
     * be accessed by the calling application (or all applications sharing the
     * same user ID).
     * @see #MODE_WORLD_READABLE
     * @see #MODE_WORLD_WRITEABLE
     */
    public static final int MODE_PRIVATE = 0x0000;
    /**
     * File creation mode: allow all other applications to have read access
     * to the created file.
     * @see #MODE_PRIVATE
     * @see #MODE_WORLD_WRITEABLE
     */
    public static final int MODE_WORLD_READABLE = 0x0001;
    /**
     * File creation mode: allow all other applications to have write access
     * to the created file.
     * @see #MODE_PRIVATE
     * @see #MODE_WORLD_READABLE
     */
    public static final int MODE_WORLD_WRITEABLE = 0x0002;
    /**
     * File creation mode: for use with {@link #openFileOutput}, if the file
     * already exists then write data to the end of the existing file
     * instead of erasing it.
     * @see #openFileOutput
     */
    public static final int MODE_APPEND = 0x8000;

    /**
     * Flag for {@link #bindService}: automatically create the service as long
     * as the binding exists.  Note that while this will create the service,
     * its {@link android.app.Service#onStartCommand}
     * method will still only be called due to an
     * explicit call to {@link #startService}.  Even without that, though,
     * this still provides you with access to the service object while the
     * service is created.
     *
     * <p>Specifying this flag also tells the system to treat the service
     * as being as important as your own process -- that is, when deciding
     * which process should be killed to free memory, the service will only
     * be considered a candidate as long as the processes of any such bindings
     * is also a candidate to be killed.  This is to avoid situations where
     * the service is being continually created and killed due to low memory.
     */
    public static final int BIND_AUTO_CREATE = 0x0001;

    /**
     * Flag for {@link #bindService}: include debugging help for mismatched
     * calls to unbind.  When this flag is set, the callstack of the following
     * {@link #unbindService} call is retained, to be printed if a later
     * incorrect unbind call is made.  Note that doing this requires retaining
     * information about the binding that was made for the lifetime of the app,
     * resulting in a leak -- this should only be used for debugging.
     */
    public static final int BIND_DEBUG_UNBIND = 0x0002;

    /**
     * Flag for {@link #bindService}: don't allow this binding to raise
     * the target service's process to the foreground scheduling priority.
     * It will still be raised to the at least the same memory priority
     * as the client (so that its process will not be killable in any
     * situation where the client is not killable), but for CPU scheduling
     * purposes it may be left in the background.  This only has an impact
     * in the situation where the binding client is a foreground process
     * and the target service is in a background process.
     */
    public static final int BIND_NOT_FOREGROUND = 0x0004;

}