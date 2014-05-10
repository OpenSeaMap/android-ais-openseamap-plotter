package com.klein.downloaders;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;

import org.mapsforge.android.maps.mapgenerator.MapGeneratorJob;
import org.mapsforge.android.maps.mapgenerator.tiledownloader.TileDownloader;
import org.mapsforge.core.Tile;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.os.Build;
import android.util.Log;


import com.klein.aistcpopenmapplotter051.R;

import com.klein.commons.AISPlotterGlobals;

import com.klein.commons.Environment2;
import com.klein.commons.PositionTools;
/**
 * 
 * @author vkADM
 * Version 2 from 12_11_15, Tileserver new from 13_10_13
 */
public class OpenSeamapTileAndSeamarksDownloader extends TileDownloader {

	private static final String TAG = "OpenSeapmapTileAndSeamarksDownLoader";
	private static final boolean test = false;
	//private static final String HOST_NAME = "t2.openseamap.org";
	//private static final String HOST_NAME_SEAMARKS = "tiles.openseamap.org";
	private static final String HOST_NAME_SEAMARKS = "t1.openseamap.org";
	//private static final String HOST_NAME = "osm1.wtnet.de";
	private static final String HOST_NAME = "osm2.wtnet.de";
	private static final String TILES_DIRECTORY ="/tiles/base/";
	//private static final String HOST_NAME_SEAMARKS = "tiles.openseamap.org";
	private static final String SEAMARKS_DIRECTORY = "/seamark/";
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
    
    private static final String CACHE_DIR_NAME =  "/OpenSeaMap"; // we keep an own cache dir for the tiles
    private static final String CACHE_TILE_PREFIX = "OpenSeaMapTile_";
    private String mCacheDirName="";
	/**
	 * Constructs a new EniroTileDownloader.
	 */
	public  OpenSeamapTileAndSeamarksDownloader(Context context, int aTimeout) {
		super();
		mContext = context;
		mTimeoutForDownload = aTimeout;
		this.pixels = new int[Tile.TILE_SIZE * Tile.TILE_SIZE];
		this.stringBuilder = new StringBuilder();
		mExternalStorageDir = Environment2.getCardDirectory();
		mExternalPathName = mExternalStorageDir.getAbsolutePath();
		String appNameDir = "/" + AISPlotterGlobals.DEFAULT_APP_DATA_DIRECTORY;
		//String cacheDirName = appNameDir + "/" + CACHE_DIR_NAME;
		String cacheDirName = "/" + AISPlotterGlobals.DEFAULT_ONLINE_TILE_CACHE_DATA_DIRECTORY + CACHE_DIR_NAME;
		mCacheDirName = cacheDirName;
		PositionTools.createExternalDirectoryIfNecessary(mCacheDirName);
		
	}
	
	

	@Override
	public String getHostName() {
		return HOST_NAME;
	}
    private String getSeamarksHostName() {
    	return HOST_NAME_SEAMARKS;
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
		   String myUrlString = myUrl.toString();
           if (test)Log.d(TAG,myUrlString);
		} catch (MalformedURLException e) {
			if (test) Log.d(TAG,"Unknown Exception");
			return "";
		}
		
		return this.stringBuilder.toString();
	}
    
	private String getSeamarksTilePath(Tile tile) {
		this.stringBuilder.setLength(0);
		
		this.stringBuilder.append(SEAMARKS_DIRECTORY);
		this.stringBuilder.append(tile.zoomLevel);
		this.stringBuilder.append('/');
		this.stringBuilder.append(tile.tileX);
		this.stringBuilder.append('/');
		this.stringBuilder.append(tile.tileY);
		this.stringBuilder.append(".png");
		String myPath = this.stringBuilder.toString();
		try {
		   URL myUrl = new URL(getProtocol(), getSeamarksHostName(),myPath );
		   String myUrlString = myUrl.toString();
           if (test)Log.d(TAG,myUrlString);
		} catch (MalformedURLException e) {
			if (test)Log.d(TAG,"Unknown Exception");
			return "";
		}
		
		return this.stringBuilder.toString();
	}
	@Override
	public byte getZoomLevelMax() {
		return ZOOM_MAX;
	}
	/**
	 * Reads an InputStream and converts it to a String.
	 * @param stream
	 * @param len
	 * @return
	 * @throws IOException
	 * @throws UnsupportedEncodingException
	 */
	 
	private String readIt(InputStream stream, int len) throws IOException, UnsupportedEncodingException {
	    Reader reader = null;
	    reader = new InputStreamReader(stream, "UTF-8");        
	    char[] buffer = new char[len];
	    reader.read(buffer);
	    return new String(buffer);
	}
	
	/**
	 * Read the first 100 bytes from the URL as String
	 * @param testurl
	 * @return
	 * @throws IOException
	 */
	private String downloadUrlAsString(URL testurl) throws IOException{
		InputStream   testinputStream = null;
		try {
			
			HttpURLConnection testcon = (HttpURLConnection) testurl.openConnection();
	        testcon.setConnectTimeout(mTimeoutForDownload);
	        testcon.setReadTimeout(mTimeoutForDownload);
	        testcon.setRequestMethod("GET");
	        testcon.connect(); 
	        mResponse = testcon.getResponseCode();
	        if (test)Log.d(TAG,"the request code " + mResponse);
	        testinputStream = testcon.getInputStream();
	        String contentAsString = readIt(testinputStream, 100);
			if (test)Log.d(TAG,contentAsString);
			return contentAsString;
		   } finally {
			   if (testinputStream != null) {
				   testinputStream.close();
			   }
		   }
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
		try {
			OutputStream os = null;
		    os = new FileOutputStream(cacheFilePathName );
		    bitmap.compress(CompressFormat.PNG, 50, os); // PNG: 50 is ignored 
		} catch(IOException e) {
		    e.printStackTrace();
		    if (test) {
		    	Log.d(TAG,e.toString());
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
			
			// we read the seamarksTile from the seamarks tile server if there is one
			if (readSeamarksBitmap( mapGeneratorJob,seamarkBitmap)){
				if (test) {
					Log.d(TAG,"try to combine the two bitmaps");
				}
				//seamarkHasAlpha = seamarkBitmap.hasAlpha();
				Canvas comboImage = new Canvas(bitmap);
				comboImage.drawBitmap(seamarkBitmap, 0f, 0f, null);
				if (test) {
					Log.d(TAG,"combine Bitmaps success");
				}
			}
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
	
	
	/**
	 * read the Seamarks Tile from the server
	 * There are many condition to fill : The url can be loaded, no timeout, the response is 200 , the type is .png
	 * then we can get the tile and put it on the bitmap
	 * @param mapGeneratorJob
	 * @param bitmap  we return the changed bitmap if no error
	 * @return false on error
	 */
	
	private boolean readSeamarksBitmap(MapGeneratorJob mapGeneratorJob, Bitmap bitmap) {
		String aPNGStr = "";
		Tile tile = mapGeneratorJob.tile;
		String aMsg = "";
		String msg = " x= " +tile.tileX + " y= " + tile.tileY + " zoom " + tile.zoomLevel ;
		if (test) {
			Log.d(TAG,"loading tile " + msg );
		}
		try {
			URL testurl = new URL(getProtocol(), getSeamarksHostName(), getSeamarksTilePath(tile));
			aMsg = testurl.toString();
			String response = downloadUrlAsString(testurl);
			if (response.length() > 5) {
				aPNGStr = response.substring(1, 4);
			}
			
		} catch (IOException e) {
			//LOG.log(Level.SEVERE, null, e);
			if (test) Log.d(TAG,e.toString());
			return false;
		}
		if (mResponse != 200){
			return false;
		}
		
		if (!aPNGStr.equalsIgnoreCase("PNG")) {
			if (test) Log.d(TAG,"no PNG " + aMsg);
			return false;
		}
		if (test) {
			Log.d(TAG,"Tile is PNG");
		}
		try {
			URL url = new URL(getProtocol(), getSeamarksHostName(), getSeamarksTilePath(tile));
			URLConnection con =url.openConnection();
	        con.setConnectTimeout(mTimeoutForDownload);
	        con.setReadTimeout(mTimeoutForDownload);
	        con.connect(); 
	        InputStream   inputStream = con.getInputStream();
			//InputStream inputStream = url.openStream();
	        BitmapFactory.Options options = new BitmapFactory.Options();
	        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
	        options.inDither = false ;
			Bitmap decodedBitmap = null;
			decodedBitmap = BitmapFactory.decodeStream(inputStream,null,options);
			//boolean seamarkHasAlpha = decodedBitmap.hasAlpha();
			inputStream.close();
           
			// check if the input stream could be decoded into a bitmap
			if (decodedBitmap == null) {
				return false;
			}
			if (test) {
				Log.d(TAG," Seamarks tile loaded " + msg );
			}
			
			// copy all pixels from the decoded bitmap to the color array
			decodedBitmap.getPixels(this.pixels, 0, Tile.TILE_SIZE, 0, 0, Tile.TILE_SIZE, Tile.TILE_SIZE);
			decodedBitmap.recycle();

			// copy all pixels from the color array to the tile bitmap
			bitmap.setPixels(this.pixels, 0, Tile.TILE_SIZE, 0, 0, Tile.TILE_SIZE, Tile.TILE_SIZE);
			
			return true;
		} catch (UnknownHostException e) {
			//LOG.log(Level.SEVERE, null, e);
			if (test)Log.d(TAG,e.toString());
			return false;
		} catch (IOException e) {
			//LOG.log(Level.SEVERE, null, e);
			if (test) Log.d(TAG,e.toString());
			
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
