package com.klein.downloaders;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;

import org.mapsforge.android.maps.mapgenerator.MapGeneratorJob;
import org.mapsforge.android.maps.mapgenerator.tiledownloader.TileDownloader;
import org.mapsforge.core.Tile;


import com.klein.aistcpopenmapplotter051.R;

import com.klein.commons.AISPlotterGlobals;

import com.klein.commons.Environment2;
import com.klein.commons.PositionTools;
import com.klein.logutils.Logger;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;
import android.os.Build;
import android.util.Log;
/**
 * 
 * @author vkADM
 *  Version 2 EniroNautical  tile downloader since 12_11_15
 */
public class EniroNauticalTileDownloader extends TileDownloader {

	//private static final String HOST_NAME = "map01.eniro.no/geowebcache/service/tms1.0.0/nautical";
	// see the document eniro Kartenanforderung
	// The browser script requests tiles from the four eniro servers
	// we use them in order 1,2,3,4
	// we assume, that a new tile should be loaded from the next host
	private static final String TAG = "EniroNauticalTileDownLoader";
	private static final boolean test = false;
	
	
	private static final String HOST_NAME1 = "map01.eniro.no";
	private static final String HOST_NAME2 = "map02.eniro.no";
	private static final String HOST_NAME3 = "map03.eniro.no";
	private static final String HOST_NAME4 = "map04.eniro.no";
	private static final String TILES_DIRECTORY = "/geowebcache/service/tms1.0.0/nautical/";
	private static final String PROTOCOL = "http";
	private static final byte ZOOM_MAX = 18;

	private final StringBuilder stringBuilder;
	private int mHostNumber; 
	private final int[] pixels;
	private Context mContext;
	private NotificationManager mNM;
	private static final int NOTIFICATION_ID = 78;
	// TileCache
	private String mExternalPathName = "";
	private File mExternalStorageDir = null;
	private static final String CACHE_DIR_NAME =  "/EniroNautical"; // we keep an own cache dir for the tiles
	private static final String CACHE_TILE_PREFIX = "EniroNauticalMapTile_";
	private String mCacheDirName="";
	// Timeout
	private int mTimeoutForDownload = 3000;
	
	/**
	 * Constructs a new EniroTileDownloader.
	 */
	public EniroNauticalTileDownloader(Context myContext, int aTimeout) {
		super();
		mContext = myContext;
		this.stringBuilder = new StringBuilder();
		this.pixels = new int[Tile.TILE_SIZE * Tile.TILE_SIZE];
		mHostNumber = 1;
		mTimeoutForDownload = aTimeout;
		mExternalStorageDir = Environment2.getCardDirectory();
		mExternalPathName = mExternalStorageDir.getAbsolutePath();
		String appNameDir = "/" + AISPlotterGlobals.DEFAULT_APP_DATA_DIRECTORY;  // AISPlotter
		//String cacheDirName = appNameDir + "/" +CACHE_DIR_NAME;                  // AISPlotter/Cachedata/EniroNautical
		String cacheDirName = "/" + AISPlotterGlobals.DEFAULT_ONLINE_TILE_CACHE_DATA_DIRECTORY +CACHE_DIR_NAME;                  // AISPlotter/Cachedata/EniroNautical
		mCacheDirName = cacheDirName;
		PositionTools.createExternalDirectoryIfNecessary(mCacheDirName);
		
	}
	
	
	
	/**
	 * get the Tile with cacheFilepathName from the cache
	 * @param cacheFilePathName
	 * @param bitmap the bitmap is filled with the tiles content
	 * @return true if Tile was in the cache
	 */
	
	private boolean getTileFromCache(String cacheFilePathName, Bitmap bitmap){
		boolean result = false;
		// Test if the Tile is in the cache
		File aTestFile = new File(cacheFilePathName);
	    boolean fileExist = aTestFile.exists();
	    if (fileExist) {
	    	Bitmap decodedBitmap;
            if (test) Log.d(TAG,"in Cache: " + cacheFilePathName);
	    	BitmapFactory.Options options = new BitmapFactory.Options();
	        options.inDither = false;   //important here we deal with the transparency  
	        options.inPreferredConfig = Bitmap.Config.ARGB_8888;  // decodeFile must return  a 8888 rgb for transparency
	        decodedBitmap = BitmapFactory.decodeFile(cacheFilePathName , options);

	        if(decodedBitmap == null) {
	           if (test) {
	        	   Log.e(TAG, "unable to decode bitmap");
	           }
	            return false;
	        }
	        decodedBitmap.getPixels(this.pixels, 0, Tile.TILE_SIZE, 0, 0, Tile.TILE_SIZE, Tile.TILE_SIZE);
			decodedBitmap.recycle();

			// copy all pixels from the color array to the tile bitmap
			bitmap.setPixels(this.pixels, 0, Tile.TILE_SIZE, 0, 0, Tile.TILE_SIZE, Tile.TILE_SIZE);
			result = true;
	    } 
	    return result;
	}
	
	private void writeTileToCache (String cacheFilePathName, Bitmap bitmap){
		OutputStream os = null;
		try {
		    os = new FileOutputStream(cacheFilePathName );
		    bitmap.compress(CompressFormat.PNG, 50, os); // PNG: 50 is ignored
		   
		} catch(IOException e) {
		    e.printStackTrace();
		    if (test) {
		    	Log.d(TAG,e.toString());
		    }
		} finally {
			if(os!=null){
				try {
				 os.close();
				} catch (IOException e) {}
			}
		}
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
		this.stringBuilder.append(TILES_DIRECTORY);
		this.stringBuilder.append(tile.zoomLevel);
		this.stringBuilder.append('/');
		this.stringBuilder.append(tile.tileX);
		this.stringBuilder.append('/');
	    long eniroTileY = (1<< tile.zoomLevel)- 1 - tile.tileY;  // enirioY = (2 hoch zoomlevel)-1 -y
		this.stringBuilder.append(eniroTileY);
		this.stringBuilder.append(".png");
		String myPath = this.stringBuilder.toString();
		/*long aTime = System.currentTimeMillis();
		String aTimeStr = PositionTools.getTimeString(aTime);
		try {
		   Log.d(TAG,"get tile " + aTimeStr  +" " + tile.tileX + " " + tile.tileY + " zoom "+ tile.zoomLevel );
		   Logger.d(TAG,"get tile " +aTimeStr + " " +tile.tileX + " " + tile.tileY + "zoom "+ tile.zoomLevel );
		   URL myUrl = new URL(getProtocol(), getHostName(),myPath );
		   String myUrlString ="request " + aTimeStr + " : " + myUrl.toString();
           Log.d(TAG,"get " + myUrlString);
           Logger.d(TAG,"get " + myUrlString);
		} catch (MalformedURLException e) {
			Log.d(TAG,"Malformed Url  Exception");
			return "";
		} catch (Exception e ){
			Logger.d(TAG,"Unkwown Exception " + e.toString());
			return "";
		}*/
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
	
	/*
	 * @Override
	 */
	public boolean executeJob(MapGeneratorJob mapGeneratorJob, Bitmap bitmap) {
		
		Tile tile = mapGeneratorJob.tile;
		String msg = " x= " +tile.tileX + " y= " + tile.tileY + " zoom " + tile.zoomLevel ;
		try {
			//Tile tile = mapGeneratorJob.tile;
			String zoomLevelStr = String.valueOf(tile.zoomLevel);
			String dirPathName =  mCacheDirName +"/"+zoomLevelStr;
			String filename = CACHE_TILE_PREFIX + zoomLevelStr + "_" + tile.tileX+"_"+tile.tileY;
			String aCacheFilePathName = mExternalPathName+dirPathName+"/"+ filename + ".png";
			if (getTileFromCache(aCacheFilePathName, bitmap)){
				return true;
			}
			// the tile is not in the cache
			// String msg = " x= " +tile.tileX + " y= " + tile.tileY + " zoom " + tile.zoomLevel ;
			Log.d(TAG,"try to load tile " + msg );
			Logger.d(TAG,"try to load tile " + msg);
			showNotification(true,msg);
			URL url = new URL(getProtocol(), getHostName(), getTilePath(tile));
			URLConnection con =url.openConnection();
	        //con.setConnectTimeout(1000);
	       // con.setReadTimeout(1000);
	        con.setConnectTimeout(mTimeoutForDownload*2);
	        con.setReadTimeout(mTimeoutForDownload);
	        con.connect(); 
	        InputStream   inputStream = con.getInputStream();
			//InputStream inputStream = url.openStream();
			Bitmap decodedBitmap = BitmapFactory.decodeStream(inputStream);
			inputStream.close();
            cancelNotification();
			// check if the input stream could be decoded into a bitmap
			if (decodedBitmap == null) {
				return false;
			}
			Log.d(TAG,"tile loaded " + msg );
			Logger.d(TAG,"tile loaded " + msg);
			// copy all pixels from the decoded bitmap to the color array
			decodedBitmap.getPixels(this.pixels, 0, Tile.TILE_SIZE, 0, 0, Tile.TILE_SIZE, Tile.TILE_SIZE);
			decodedBitmap.recycle();

			// copy all pixels from the color array to the tile bitmap
			bitmap.setPixels(this.pixels, 0, Tile.TILE_SIZE, 0, 0, Tile.TILE_SIZE, Tile.TILE_SIZE);
			PositionTools.createExternalDirectoryIfNecessary(dirPathName);
			writeTileToCache (aCacheFilePathName,bitmap);
			return true;
		} catch (UnknownHostException e) {
			//LOG.log(Level.SEVERE, null, e);
			String errmsg = "tile not loaded " + msg  + " " +e.toString();
			Log.d(TAG,errmsg);
			Logger.d(TAG,errmsg);
			showNotification(false,errmsg);
			return false;
		} catch (IOException e) {
			//LOG.log(Level.SEVERE, null, e);
			Log.d(TAG,e.toString());
			Logger.d(TAG,e.toString());
			showNotification(false,"");
			return false;
		}
	}
	
	private void showNotification(boolean isOn, String msg) {
		mNM = (NotificationManager)mContext.getSystemService(Context.NOTIFICATION_SERVICE);
		String aNotificationTitle = mContext.getResources().getString(R.string.tcp_network_service); 
		String aNotificationMessage;
		String aPendingMessage;
		int aVersion  = Build.VERSION.SDK_INT;  // we ask for the Version of the OS
		if (isOn){
		  aNotificationMessage = mContext.getResources().getString(R.string.mapdownload_runs) + msg;
		  Notification aNotification = new Notification (R.drawable.downloadactive_green, aNotificationMessage,
			        System.currentTimeMillis());
		  /* PendingIntent pendingIntent = 
	      PendingIntent.getActivity(mContext, 0, null, 0);*/
          PendingIntent pendingIntent = null;
          if (aVersion < 14) pendingIntent = PendingIntent.getActivity(mContext, 0, null, 0);
		  aPendingMessage = mContext.getResources().getString(R.string.mapdownload_false) + msg; 
		  aNotification.setLatestEventInfo(mContext, 
			        						aNotificationTitle,
			        						aPendingMessage, pendingIntent); 
			    
			    // Ab Android 2.0: 
		  mNM.notify(NOTIFICATION_ID,aNotification);
		  //mContext.startForeground(NOTIFICATION_ID,aNotification);
		}else {
		  aNotificationMessage = mContext.getResources().getString(R.string.mapdownload_false);
		  Notification aNotification = new Notification (R.drawable.downloadfalse, aNotificationMessage,
			        System.currentTimeMillis());
		  /* PendingIntent pendingIntent = 
	      PendingIntent.getActivity(mContext, 0, null, 0);*/
          PendingIntent pendingIntent = null;
          if (aVersion < 14) pendingIntent = PendingIntent.getActivity(mContext, 0, null, 0);
		  aPendingMessage = mContext.getResources().getString(R.string.mapdownload_false); 
		  aNotification.setLatestEventInfo(mContext, 
				    						aNotificationTitle,
				    						aPendingMessage, pendingIntent); 
			    
			    // Ab Android 2.0: 
		  mNM.notify(NOTIFICATION_ID,aNotification);
	}
		
	}
	
	private void cancelNotification(){
		mNM.cancel(NOTIFICATION_ID);
	}
	
}
