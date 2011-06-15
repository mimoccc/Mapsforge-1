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

public interface DialogInterface {

	public static final int BUTTON_POSITIVE = -1;
	
	public interface OnClickListener {

		/**
         * This method will be invoked when a button in the dialog is clicked.
         *
         * @param dialog The dialog that received the click.
         * @param which The button that was clicked (e.g.
         *            {@link DialogInterface#BUTTON1}) or the position
         *            of the item clicked.
         */
        /* TODO: Change to use BUTTON_POSITIVE after API council */
		public void onClick(DialogInterface dialog, int which);
	}
}
