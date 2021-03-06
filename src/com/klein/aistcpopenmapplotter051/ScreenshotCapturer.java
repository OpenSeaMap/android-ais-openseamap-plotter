/*
 * Copyright 2010, 2011, 2012 mapsforge.org
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
package com.klein.aistcpopenmapplotter051;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import org.mapsforge.android.maps.PausableThread;

import android.graphics.Bitmap.CompressFormat;
import android.os.Environment;

import com.klein.commons.PositionTools;

class ScreenshotCapturer extends PausableThread {
	private static final String SCREENSHOT_DIRECTORY = "Pictures";
	private static final String SCREENSHOT_FILE_NAME = "Map_screenshot_";
	private static final int SCREENSHOT_QUALITY = 90;
	private static final String THREAD_NAME = "ScreenshotCapturer";

	private final AISTCPOpenMapPlotter mAISTCPOpenMapPlotter;
	private CompressFormat compressFormat;

	ScreenshotCapturer(AISTCPOpenMapPlotter advancedMapViewer) {
		this.mAISTCPOpenMapPlotter = advancedMapViewer;
	}

	private File assembleFilePath(File directory) {
		StringBuilder strinBuilder = new StringBuilder();
		strinBuilder.append(directory.getAbsolutePath());
		strinBuilder.append(File.separatorChar);
		strinBuilder.append(SCREENSHOT_FILE_NAME);
		strinBuilder.append(PositionTools.getCurrentDateTimeForFilename());
		strinBuilder.append('.');
		strinBuilder.append(this.compressFormat.name().toLowerCase(Locale.ENGLISH));
		return new File(strinBuilder.toString());
	}

	@Override
	protected void doWork() {
		try {
			File directory = new File(Environment.getExternalStorageDirectory(), SCREENSHOT_DIRECTORY);
			if (!directory.exists() && !directory.mkdirs()) {
				this.mAISTCPOpenMapPlotter.showToastOnUiThread("Could not create screenshot directory");
				return;
			}

			File outputFile = assembleFilePath(directory);
			if (this.mAISTCPOpenMapPlotter.mapView1.takeScreenshot(this.compressFormat, SCREENSHOT_QUALITY, outputFile)) {
				this.mAISTCPOpenMapPlotter.showToastOnUiThread(outputFile.getAbsolutePath());
			} else {
				this.mAISTCPOpenMapPlotter.showToastOnUiThread("Screenshot could not be saved");
			}
		} catch (IOException e) {
			this.mAISTCPOpenMapPlotter.showToastOnUiThread(e.getMessage());
		}

		this.compressFormat = null;
	}

	@Override
	protected String getThreadName() {
		return THREAD_NAME;
	}

	@Override
	protected boolean hasWork() {
		return this.compressFormat != null;
	}

	void captureScreenShot(CompressFormat screenShotFormat) {
		this.compressFormat = screenShotFormat;
		synchronized (this) {
			notify();
		}
	}
}
