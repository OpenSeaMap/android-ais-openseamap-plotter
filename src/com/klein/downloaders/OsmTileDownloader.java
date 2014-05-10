package com.klein.downloaders;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
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
 * @author vkADM
 *  version 2
 */
public class OsmTileDownloader extends TileDownloader {
  // 12_10_30
	// this is the old one ,does not load the seamarks
	// not used since 12:10_30 see OpenSeampaTileAndSeamarksDownloader
	private static final String TAG = "OpenStreetMapTileDownLoader";
	private static final boolean test = true;
	
	private static final String HOST_NAME = "osm1.wtnet.de";
	private static final String TILES_DIRECTORY ="/tiles/base/";
	private static final String PROTOCOL = "http";
	private static final byte ZOOM_MAX = 18;

	private final StringBuilder stringBuilder;
	
	private final int[] pixels;
	private Context mContext;
	private NotificationManager mNM;
	private static final int NOTIFICATION_ID = 78;
	private int mResponse = 0;
    private String mExternalPathName = "";
    private File mExternalStorageDir = null;
    private int mTimeoutForDownload = 3000;
    
    private static final String CACHE_DIR_NAME =  "/OpenStreetMap"; // we keep an own cache dir for the tiles
    private static final String CACHE_TILE_PREFIX = "OpenStreetMapTile_";
    private String mCacheDirName="";

	/**
	 * Constructs a new EniroTileDownloader.
	 */
	public  OsmTileDownloader(Context context, int aTimeout) {
		super();
		mContext = context;
		mTimeoutForDownload = aTimeout;
		this.pixels = new int[Tile.TILE_SIZE * Tile.TILE_SIZE];
		this.stringBuilder = new StringBuilder();
		mExternalStorageDir = Environment2.getCardDirectory();
		mExternalPathName = mExternalStorageDir.getAbsolutePath();
		String appNameDir = "/" + AISPlotterGlobals.DEFAULT_APP_DATA_DIRECTORY;
		//String cacheDirName = appNameDir + "/" + CACHE_DIR_NAME;
		String cacheDirName = "/" +AISPlotterGlobals.DEFAULT_ONLINE_TILE_CACHE_DATA_DIRECTORY + CACHE_DIR_NAME;
		mCacheDirName = cacheDirName;
		PositionTools.createExternalDirectoryIfNecessary(mCacheDirName);
	}
	
	

	@Override
	public String getHostName() {
		return HOST_NAME;
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
		this.stringBuilder.append(tile.tileY);
		this.stringBuilder.append(".png");
		String myPath = this.stringBuilder.toString();
		try {
		   URL myUrl = new URL(getProtocol(), getHostName(),myPath );
		   long aTime = System.currentTimeMillis();
		   String aTimeStr = PositionTools.getTimeString(aTime);
		   String myUrlString ="request " + aTimeStr + " : " + myUrl.toString();
           Log.d(TAG,myUrlString);
		} catch (MalformedURLException e) {
			Log.d(TAG,"Unknown Exception");
			return "";
		}
		
		return this.stringBuilder.toString();
	}

	@Override
	public byte getZoomLevelMax() {
		return ZOOM_MAX;
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
	
	/*
	 * @Override
	 */
	
	public boolean executeJob(MapGeneratorJob mapGeneratorJob, Bitmap bitmap) {
		
		try {
			Tile tile = mapGeneratorJob.tile;
			String zoomLevelStr = String.valueOf(tile.zoomLevel);
			String dirPathName =  mCacheDirName +"/"+zoomLevelStr;
			String filename = CACHE_TILE_PREFIX + zoomLevelStr + "_" + tile.tileX+"_"+tile.tileY;
			String aCacheFilePathName = mExternalPathName+dirPathName+"/"+ filename + ".png";
			// Test if the Tile is in the cache
			/*File aTestFile = new File(aCacheFilePathName);
		    boolean doExist = aTestFile.exists();
		    if (aTestFile.exists()) {
		    	Bitmap decodedBitmap;
                Log.d(TAG,"in Cache: " + filename);
		    	BitmapFactory.Options options = new BitmapFactory.Options();
		        options.inDither = false;   //important
		        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
		        decodedBitmap = BitmapFactory.decodeFile(aCacheFilePathName , options);

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
				return true;
		    } 
			if (test) {
				Log.d(TAG,"loading tile " + msg );
			}*/
			
			if (getTileFromCache(aCacheFilePathName, bitmap)){
				return true;
			}
			// the tile is not in the cache so we get it from the  Server and set a notification to the user
			String msg = " x= " +tile.tileX + " y= " + tile.tileY + " zoom " + tile.zoomLevel ;
			showNotification(true,msg);
			URL url = new URL(getProtocol(), getHostName(), getTilePath(tile));
			URLConnection con =url.openConnection();
	        con.setConnectTimeout(mTimeoutForDownload*2);
	        con.setReadTimeout(mTimeoutForDownload);
	        con.connect(); 
	        InputStream   inputStream = con.getInputStream();
			//InputStream inputStream = url.openStream();
	        BitmapFactory.Options options = new BitmapFactory.Options();
	        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
	        options.inDither = false ;
			Bitmap decodedBitmap = BitmapFactory.decodeStream(inputStream,null,options);
			//Bitmap decodedBitmap = BitmapFactory.decodeStream(inputStream);
			inputStream.close();
            cancelNotification();
			// check if the input stream could be decoded into a bitmap
			if (decodedBitmap == null) {
				return false;
			}
			if (test) {
				Log.d(TAG,"tile loaded " + msg );
			}
			
			// copy all pixels from the decoded bitmap to the color array
			decodedBitmap.getPixels(this.pixels, 0, Tile.TILE_SIZE, 0, 0, Tile.TILE_SIZE, Tile.TILE_SIZE);
			decodedBitmap.recycle();

			// copy all pixels from the color array to the tile bitmap
			bitmap.setPixels(this.pixels, 0, Tile.TILE_SIZE, 0, 0, Tile.TILE_SIZE, Tile.TILE_SIZE);
			//boolean bitmapHasAlpha = bitmap.hasAlpha();
			//boolean decodedbitmapHasAlpha = decodedBitmap.hasAlpha();
			Bitmap seamarkBitmap = Bitmap.createBitmap(Tile.TILE_SIZE,Tile.TILE_SIZE,Bitmap.Config.ARGB_8888);
		   // boolean seamarkHasAlpha = seamarkBitmap.hasAlpha();
			
			
			/*try {
				OutputStream os = null;
			    os = new FileOutputStream(aCachePathName );
			    bitmap.compress(CompressFormat.PNG, 50, os); // PNG: 50 is ignored 
			} catch(IOException e) {
			    e.printStackTrace();
			    if (test) {
			    	Log.d(TAG,e.toString());
			    }
			}*/
			PositionTools.createExternalDirectoryIfNecessary(dirPathName);
			writeTileToCache(aCacheFilePathName,bitmap);
			// all ok 
			return true;
		} catch (UnknownHostException e) {
			//LOG.log(Level.SEVERE, null, e);
			if (test) {
				Log.d(TAG,e.toString());
			}
			return false;
		} catch (IOException e) {
			//LOG.log(Level.SEVERE, null, e);
			if (test) {
				Log.d(TAG,e.toString());
			}
			
			showNotification(false,"");
			return false;
		}
	}
	
	
	private void showNotification(boolean isOn, String msg) {
		mNM = (NotificationManager)mContext.getSystemService(Context.NOTIFICATION_SERVICE);
		String aNotificationTitle = mContext.getResources().getString(R.string.tcp_network_service); 
		String aNotificationMessage;
		String aPendingMessage;
		int aVersion  = Build.VERSION.SDK_INT;
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
