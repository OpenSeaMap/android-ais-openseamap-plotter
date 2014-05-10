package com.klein.downloaders;

import java.net.MalformedURLException;
import java.net.URL;

import org.mapsforge.android.maps.mapgenerator.tiledownloader.TileDownloader;
import org.mapsforge.core.Tile;

import com.klein.commons.PositionTools;
import com.klein.logutils.Logger;

import android.util.Log;
/**
 * 
 * @author vkADM
 *  Version 1  without own cache
 *  until 12_11_15
 */
public class EniroMapTileDownloaderOld extends TileDownloader {

	 private static final String TAG = "EniroMapTileDownLoader";
		//private static final String HOST_NAME = "map01.eniro.no/geowebcache/service/tms1.0.0/nautical";
		private static final String HOST_NAME1 = "map01.eniro.no";
		private static final String HOST_NAME2 = "map02.eniro.no";
		private static final String HOST_NAME3 = "map03.eniro.no";
		private static final String HOST_NAME4 = "map04.eniro.no";
		private static final String PROTOCOL = "http";
		private static final byte ZOOM_MAX = 18;

		private final StringBuilder stringBuilder;
		private int mHostNumber;

		/**
		 * Constructs a new EniroTileDownloader.
		 */
		public EniroMapTileDownloaderOld() {
			super();
			this.stringBuilder = new StringBuilder();
			mHostNumber = 1;
		}

		@Override
		public String getHostName() {
			switch (mHostNumber) {
			
			case 1:
				return HOST_NAME1;
			case 2:
				return HOST_NAME2;
			case 3:
				return HOST_NAME3;
			case 4:
				return HOST_NAME4;
			default:
				return HOST_NAME2;
			}
		}

		@Override
		public String getProtocol() {
			return PROTOCOL;
		}

		@Override
		public String getTilePath(Tile tile) {
			this.stringBuilder.setLength(0);
			this.stringBuilder.append("/geowebcache/service/tms1.0.0/map/");
			this.stringBuilder.append(tile.zoomLevel);
			this.stringBuilder.append('/');
			this.stringBuilder.append(tile.tileX);
			this.stringBuilder.append('/');
		    long eniroTileY = (1<< tile.zoomLevel)- 1 - tile.tileY;
			this.stringBuilder.append(eniroTileY);
			this.stringBuilder.append(".png");
			String myPath = this.stringBuilder.toString();
			long aTime = System.currentTimeMillis();
			String aTimeStr = PositionTools.getTimeString(aTime);
			try {
				Log.d(TAG,"get tile " + aTimeStr  +" " + tile.tileX + " " + tile.tileY + " zoom "+ tile.zoomLevel );
				Logger.d(TAG,"get tile " +aTimeStr + " " +tile.tileX + " " + tile.tileY + "zoom "+ tile.zoomLevel );
				URL myUrl = new URL(getProtocol(), getHostName(),myPath );
				String myUrlString ="request " + aTimeStr + " : " + myUrl.toString();
		        Log.d(TAG,"get " + myUrlString);
		        Logger.d(TAG,"get " + myUrlString);
			} catch (MalformedURLException e) {
				Log.d(TAG,"Unknown Exception");
				return "";
			}
		      catch (Exception e ){
			    Logger.d(TAG,"Unkwown Exception " + e.toString());
			     return "";
		    }
			// if we get a new tile path we set a new tile server for the next request
			// see for this algorithm 
			// org.mapsforge.android.maps.mapgenerator.tiledownloader.TileDownloader--> executeJob
			// getTilePath is called at the third position while constructing the url
			if (mHostNumber > 3){
				mHostNumber = 1;
			}else {
				mHostNumber++;
			}
			
			return this.stringBuilder.toString();
		}

		@Override
		public byte getZoomLevelMax() {
			return ZOOM_MAX;
		}

}
