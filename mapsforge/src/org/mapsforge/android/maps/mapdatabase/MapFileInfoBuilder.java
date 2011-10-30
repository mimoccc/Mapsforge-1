/*
 * Copyright 2010, 2011 mapsforge.org
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.mapsforge.android.maps.mapdatabase;

import org.mapsforge.android.maps.GeoPoint;

import android.graphics.Rect;

class MapFileInfoBuilder {
	private String commentText;
	private boolean debugFile;
	private long fileSize;
	private int fileVersion;
	private String languagePreference;
	private Rect mapBoundary;
	private long mapDate;
	private String projectionName;
	private GeoPoint startPosition;

	MapFileInfo build() {
		return new MapFileInfo(this);
	}

	String getCommentText() {
		return this.commentText;
	}

	long getFileSize() {
		return this.fileSize;
	}

	int getFileVersion() {
		return this.fileVersion;
	}

	String getLanguagePreference() {
		return this.languagePreference;
	}

	Rect getMapBoundary() {
		return this.mapBoundary;
	}

	long getMapDate() {
		return this.mapDate;
	}

	String getProjectionName() {
		return this.projectionName;
	}

	GeoPoint getStartPosition() {
		return this.startPosition;
	}

	boolean isDebugFile() {
		return this.debugFile;
	}

	MapFileInfoBuilder setCommentText(String commentText) {
		this.commentText = commentText;
		return this;
	}

	MapFileInfoBuilder setDebugFile(boolean debugFile) {
		this.debugFile = debugFile;
		return this;
	}

	MapFileInfoBuilder setFileSize(long fileSize) {
		this.fileSize = fileSize;
		return this;
	}

	MapFileInfoBuilder setFileVersion(int fileVersion) {
		this.fileVersion = fileVersion;
		return this;
	}

	MapFileInfoBuilder setLanguagePreference(String languagePreference) {
		this.languagePreference = languagePreference;
		return this;
	}

	MapFileInfoBuilder setMapBoundary(Rect mapBoundary) {
		this.mapBoundary = mapBoundary;
		return this;
	}

	MapFileInfoBuilder setMapDate(long mapDate) {
		this.mapDate = mapDate;
		return this;
	}

	MapFileInfoBuilder setProjectionName(String projectionName) {
		this.projectionName = projectionName;
		return this;
	}

	void setStartPosition(GeoPoint startPosition) {
		this.startPosition = startPosition;
	}
}