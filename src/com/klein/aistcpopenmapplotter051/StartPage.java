package com.klein.aistcpopenmapplotter051;


import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Map;

import org.mapsforge.android.AndroidUtils;
import org.mapsforge.android.maps.mapgenerator.MapGeneratorInternal;
import org.mapsforge.core.GeoPoint;

import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.DhcpInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.klein.activities.AISListActivity;
import com.klein.activities.EinstellungenBearbeitenActivity;
import com.klein.activities.FilePicker;
import com.klein.activities.MonitoringActivity;
import com.klein.activities.ReadAssetActivity;
import com.klein.commons.AISPlotterGlobals;
import com.klein.commons.Environment2;
import com.klein.commons.PositionTools;
import com.klein.filefilter.FilterByFileExtension;
import com.klein.filefilter.ValidMapFile;
import com.klein.logutils.Logger;
import com.klein.service.NMEATCPServiceFactory;
import com.klein.service.NMEATCPVerwaltung;
import com.klein.service.TargetList;
import com.klein.service.TrackDbAdapter;

public class StartPage extends Activity {
	private static final String TAG = "StartPage";
	private static final boolean test = false;
	  
	
	  public static final String DEFAULT_SERVER_ADDRESS = "192.168.0.1";
	  public static final String DEFAULT_SERVER_PORT = "9999";
	  public static final int VERSION = 57;
	  public static final String VERSIONSTR ="058";
	  public static final int BUILD =1;
	  public static final String BUILDSTR ="01";
	  
	  // file picker
	  private static final int SELECT_MAP_FILE = 10;
	  
	  private static final int SELECT_BACKUPSER_FILE = 20;
	  private static final FileFilter FILE_FILTER_EXTENSION_MAP = new FilterByFileExtension(".map");
	 
	/*  public static final String PREF_BT_ADDRESS ="bt_address";
	  public static final String DEFAULT_BT_ADDRESS = "00:06:66:06:BF:CE";
	  */
	  
	  private NMEATCPVerwaltung   mNMEATCPVerwaltung;
	  private TrackDbAdapter mDbAdapter;
	  private static WifiManager mWifiManager;
	 
	  SharedPreferences prefs;
	  
	  
	  private Button startButton;
	  private Button finishButton;
	  private Button confirmButton;
	  private Button XbuttonShowMonitoring;
	  private ImageView startpage_image;
	  private Button button4;
	  private Button buttonTest;
	  private Button buttonStartService;
	  private Button buttonStopService;
	  
	  //private Button startSplitButton;
	  private Button dataScreenButton;
	  private boolean mDataScreenPossible = false;
	  
	// Intent request codes
	//    private static final int REQUEST_CONNECT_DEVICE = 1;
	   
	   
	    // Local Bluetooth adapter
	 private BluetoothAdapter mBluetoothAdapter = null; 
	 // Name of the connected device
	    
	 private String btAddress; 
	    
	   // private String IPAddress;
	  private String mIPAddressString = "";  
	  
	  
	 private boolean testDataScreenPossible() {
		boolean result = false;
		
		int aDisplayWidth = getWindowManager().getDefaultDisplay().getWidth();
	    int aDisplayHeight = getWindowManager().getDefaultDisplay().getHeight();
	    Logger.d("Screen Dim w= " + aDisplayWidth + "  h= " + aDisplayHeight);
	    Log.d (TAG, "display width " +  aDisplayWidth + " display height " + aDisplayHeight);
	    if (aDisplayWidth >= 800 && aDisplayHeight >= 480) {
	    	//landscape  mode on a 7 inch display
	    	result = true;
	    	
	    	Log.d(TAG,"7 inch display with landscape mode");
	    }
	    if (aDisplayWidth >= 480 && aDisplayHeight >= 800) {
	    	//portrait  mode on a 7 inch display
	    	   
	    	result = true;
	    	Log.d(TAG,"7 inch display with portrait mode");
	    }
	   
	    if (aDisplayWidth >= 1280 && aDisplayHeight >= 752) {
	    	//landscape  mode on a 10 inch display
	    	result = true;
	    	
	    	Log.d(TAG,"10 inch display with landscape mode");
	    }
	    if (aDisplayWidth >= 800 && aDisplayHeight >= 1232) {
	    	//portrait  mode on a 10 inch display
	    	
	    	result = true;
	    	Log.d(TAG,"10 inch display with portrait mode");
	    } 
		return result;
		
	 }
	  
	  /** Called when the activity is first created. */
	  @Override
	  public void onCreate(Bundle savedInstanceState) {
	      super.onCreate(savedInstanceState);
	      //setContentView(R.layout.startpagenew);
	      setContentView(R.layout.startpage_datascreen);
	      Logger.d(TAG,"new Start ===============================================================");
	      mDataScreenPossible = testDataScreenPossible();
	      startButton = (Button) findViewById(R.id.startButton);
	      startButton.setOnClickListener(onStartButtonClick);
	      
	      //startSplitButton = (Button) findViewById(R.id.startButtonSplit); 13_12_10
	      //startSplitButton.setOnClickListener(onStartButtonSplitClick);
	      dataScreenButton = (Button) findViewById(R.id.datascreenButton);
	      dataScreenButton.setOnClickListener(onDataScreenButtonClick);
	      
	      finishButton = (Button) findViewById(R.id.finishButton);
	      finishButton.setOnClickListener(onFinishButtonClick);
	      confirmButton = (Button) findViewById(R.id.comfirmButton);
	      confirmButton.setOnClickListener(onConfirmButtonClick);
	      //buttonShowMonitoring = (Button) findViewById(R.id.monitoring);
	      //buttonShowMonitoring.setOnClickListener(onButtonMonitoringClick);
	      startpage_image = (ImageView)findViewById(R.id.startpage_image);
	      startpage_image.setVisibility(View.INVISIBLE);
	      /*button4 = (Button) findViewById(R.id.button4);
	      button4.setOnClickListener(onButton4Click);*/
	      
	      /*buttonTest = (Button) findViewById(R.id.buttontest);
	      buttonTest.setOnClickListener(onButtonTestClick);*/
	      
	      // buttonShowMonitoring.setVisibility(View.INVISIBLE);
	      
	      buttonStartService = (Button) findViewById(R.id.startservice); 
	      buttonStartService.setOnClickListener(onButtonStartServiceClick);
	      
	      buttonStopService = (Button) findViewById(R.id.stopservice);
	      buttonStopService.setOnClickListener(onButtonStopServiceClick);
	      // we hide the buttons, we need them only for test
	      buttonStartService.setClickable(false);
		  buttonStartService.setVisibility(View.INVISIBLE);
		  buttonStopService.setClickable(false);
		  buttonStopService.setVisibility(View.INVISIBLE);
		  /* 13_12_10
		  startSplitButton.setClickable(false);
		  startSplitButton.setVisibility(View.INVISIBLE);
		  */
		  dataScreenButton.setClickable(false);
		  dataScreenButton.setVisibility(View.INVISIBLE);
		  
		  startButton.setClickable(false);
		  startButton.setVisibility(View.INVISIBLE);
		  
	      prefs = PreferenceManager.getDefaultSharedPreferences(this);
	      
	      mDbAdapter = new TrackDbAdapter(this);
	      mDbAdapter.open();
	      if (firstStart() ) doFirstStart();
	      
	      showInfoText();
	      int savedVersion = prefs.getInt("VERSION", 0);
		  int savedBuild = prefs.getInt("BUILD",0);
			if (savedBuild < BUILD) {  // update symbols and renderer
			   String symboldefsName = AISPlotterGlobals.DEFAULT_SEAMARKS_SYMBOL_FILENAME;
			   boolean ok = copyFileFromAssetToStandardDirectory (symboldefsName); // copy a new symboldefs
			   String renderThemeName = AISPlotterGlobals.DEFAULT_STANDRAD_RENDERER_FILENAME;
			   ok = copyFileFromAssetToStandardDirectory (renderThemeName);  // copy the renderer
			}
	      copyStandardSymbolDefsIfNecessary();
	      copyStandardRendererIfNecessary();
	      copyTestMapIfNecessary();
	      showPrivateIPAdddress();
	      //setUpLocalBTAdapter();
	
	  }
	  
	  private void showInfoText() {
		  try {
	            InputStream is = getAssets().open("disclaimer.txt");
	            
	            // We guarantee that the available method returns the total
	            // size of the asset...  of course, this does mean that a single
	            // asset can't be more than 2 gigs.
	            int size = is.available();
	            
	            // Read the entire asset into a local byte buffer.
	            byte[] buffer = new byte[size];
	            is.read(buffer);
	            is.close();
	            
	            // Convert the buffer into a string.
	            String text = new String(buffer);
	            
	            // Finally stick the string into the text view.
	            TextView tv = (TextView)findViewById(R.id.info_text);
	            tv.setText(text);
	        } catch (IOException e) {
	            // Should never happen!
	            throw new RuntimeException(e);
	        }
	  }
	  
	  
	  private void showPrivateIPAdddress() {
		// Try to get the TCP-Address, if empty we started new
			String aTCPAddress = "";
			String prev_TCP_Server_address_key = getResources().getString(R.string.pref_host);
			boolean hasTCPAddressKey =prefs.contains(prev_TCP_Server_address_key);
			if (hasTCPAddressKey) {
				Map<String,?> aMap = prefs.getAll();
				aTCPAddress = (String)aMap.get(prev_TCP_Server_address_key);
			}
			else {
				Toast.makeText(this, "no TCP Server address set, set first", Toast.LENGTH_LONG).show();
			}
			if (aTCPAddress.equals("")){
				Toast.makeText(this, "TCP Server address is empty", Toast.LENGTH_LONG).show();
			}
			// Beachte : <uses-permission android:name="android.permission.ACCESS_WIFI_STATE"></uses-permission>
			// ins manifest einfügen !!!
			// sonst crashed die App beim Aufruf, vermutlich ist mWifimanager dann null
			// am besten abfangen
			
			mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
			if (mWifiManager != null) {
				DhcpInfo aDhcpInfo = mWifiManager.getDhcpInfo();
				/*int myIPAddress = aDhcpInfo.ipAddress;
				
				for (int i = 0;i<4;i++){
					int val = myIPAddress % 256;
					mIPAddressString = mIPAddressString + Integer.valueOf(val)+".";
					myIPAddress = myIPAddress / 256;
				}
				mIPAddressString = mIPAddressString.substring(0,mIPAddressString.length()-1);
				Logger.d(TAG,"IPAdresse " + mIPAddressString);
				Toast.makeText(this, "own IP-Address " + mIPAddressString, Toast.LENGTH_LONG).show();*/
				String aInfoStr = aDhcpInfo.toString();
				String [] fields = aInfoStr.split(" ");
				if (fields[0].equals("ipaddr")) {
					mIPAddressString = fields[1];
					Toast.makeText(this, "own IP-Address " + mIPAddressString, Toast.LENGTH_LONG).show();
				}
				int l = fields.length;
				for (int i = 0;i<l; i++){
					Logger.d(TAG,fields[i]+ " " + fields[i+1]);
					i++;
				}
			    Log.d(TAG,aInfoStr);
				WifiInfo aWifiInfo = mWifiManager.getConnectionInfo();
				Logger.d(TAG,aWifiInfo.toString());
			}
	  }
	  
	  private void setUpLocalBTAdapter() {
		  	// Get local Bluetooth adapter
	        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

	        // If the adapter is null, then Bluetooth is not supported
	        if (mBluetoothAdapter == null) {
	            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
	            finish();
	            return;
	        }
	        
	     // Try to get the bt address
	        String abtAddress = "";
	        String prev_BT_Server_address_key = getResources().getString(R.string.pref_btaddress);
			boolean haskey =prefs.contains(prev_BT_Server_address_key);
			if (haskey) {
				Map<String,?> aMap = prefs.getAll();
				abtAddress = (String)aMap.get(prev_BT_Server_address_key);
			}
			else {
				Toast.makeText(this, "no Bluetooth address set, pair first", Toast.LENGTH_LONG).show();
			}
			if (abtAddress.equals("")){
				Toast.makeText(this, "Bluetooth address is empty", Toast.LENGTH_LONG).show();
			}
			 
	  }
	  
	  
	  @Override
	  protected void onPause() {
		  prefs.edit()
	     	.putInt("VERSION", VERSION)
	     	.putInt("BUILD", BUILD)
			.commit();
		  super.onPause();
		  
		  
	  }
	  
	  
	  
	  public void showCacheRestoreResult(boolean ok) {
		  if (ok) {
				ArrayList<String> aStringList = PositionTools.getInfoAboutActiveFile();
				
				int count = aStringList.size();
				
				Logger.d(TAG," Restore cache begin");
				int maxFileIndex = 0;
				for (int index = 0; index < count; index++) {
					Logger.d(TAG,aStringList.get(index));
					String aFilename = aStringList.get(index);
					aFilename = aFilename.replace('.','X');
					String [] fields = aFilename.split("X");
					String aFileNumberStr = fields[0];
					int aFileNumber = 0;
					try {
					   aFileNumber = Integer.parseInt(aFileNumberStr);
					} catch (Exception e) {
						Logger.d(TAG,e.toString());
					}
					if (aFileNumber > maxFileIndex) {
						maxFileIndex = aFileNumber;
					}
					 
				}
				String lastTileName = String.valueOf(maxFileIndex) + ".tile";
				showToastOnUiThread(" cache restored " + count + " tiles. Last tile: " + lastTileName);
				Logger.d(TAG," Restore cache finished");
			}else {
				showToastOnUiThread(" cache was not restored");
			}  
	  }
	  
	  public void onActivityResult(int requestCode, int resultCode, Intent data)  {
		  if (requestCode == SELECT_BACKUPSER_FILE) {
				 if (resultCode == Activity.RESULT_OK) {
					 if (data != null && data.getStringExtra(FilePicker.SELECTED_FILE) != null) {
							String backUpFilePath = data.getStringExtra(FilePicker.SELECTED_FILE);
							showToastOnUiThread("I try to restore the cache with " + backUpFilePath);
							boolean ok = PositionTools.restoreCacheSerFile( backUpFilePath);
							
							showCacheRestoreResult(ok);
							/*if (ok) {
								ArrayList<String> aStringList = PositionTools.getRestoreResult();
								int count = aStringList.size();
								String lastTileName = aStringList.get(count -1);
								showToastOnUiThread(" cache restored " + count/2 + " tiles " + lastTileName);
								Logger.d(TAG," Restore cache begin");
								for (int index = 0; index < count; index++) {
									Logger.d(TAG,aStringList.get(index));
								}
								Logger.d(TAG," Restore cache finished");
							}else {
								showToastOnUiThread(" cache was not restored");
							}*/
					 }
				 }
			}
		  if (requestCode == SELECT_MAP_FILE) {
				if (resultCode == RESULT_OK) {
					if (data != null && data.getStringExtra(FilePicker.SELECTED_FILE) != null) {
						String mapfilePath = data.getStringExtra(FilePicker.SELECTED_FILE);
						showToastOnUiThread(mapfilePath);
						String pref_mapfile = getResources().getString(R.string.pref_mapfilename);
						prefs.edit()
						.putString(pref_mapfile, mapfilePath)
				 	    .commit(); 
						// set center of map
						if (mapfilePath.contains("nordrhein-westfalen")) {
							GeoPoint aGp = new GeoPoint(AISPlotterGlobals.MAP_NRW_CENTER_LAT,AISPlotterGlobals.MAP_NRW_CENTER_LON); // Duisburg
							   int aGoogleLAT = (int) aGp.latitudeE6; // we keep the prefs in Micrograd
						       int aGoogleLON = (int) aGp.longitudeE6;
						       prefs.edit()
						     	.putInt(AISPlotterGlobals.PREV_LAST_GEOPOINT_LAT, aGoogleLAT)
						     	.putInt(AISPlotterGlobals.PREV_LAST_GEOPOINT_LON, aGoogleLON)
						     	.putFloat(AISPlotterGlobals.PREF_ZOOM_FACTOR,12)
						     	.commit();
							}
						if (mapfilePath.contains("netherlands")) { // Stavoren
							GeoPoint aGp = new GeoPoint(AISPlotterGlobals.MAP_NL_CENTER_LAT,AISPlotterGlobals.MAP_NL_CENTER_LON); // stavoren
								int aGoogleLAT = (int) aGp.latitudeE6; // we keep the prefs in Micrograd
							    int aGoogleLON = (int) aGp.longitudeE6;
							    prefs.edit()
							     	.putInt(AISPlotterGlobals.PREV_LAST_GEOPOINT_LAT, aGoogleLAT)
							     	.putInt(AISPlotterGlobals.PREV_LAST_GEOPOINT_LON, aGoogleLON)
							     	.putFloat(AISPlotterGlobals.PREF_ZOOM_FACTOR,12)
							     	.commit();
						}
						
					}
				}
		   }
	   }
	  
	  /**
		 * Sets all file filters and starts the FilePicker to select a map file.
		 */
		private void startMapFilePicker() {
			FilePicker.setFileDisplayFilter(FILE_FILTER_EXTENSION_MAP);
			FilePicker.setFileSelectFilter(new ValidMapFile());
			startActivityForResult(new Intent(this, FilePicker.class), SELECT_MAP_FILE);
		}
	  
	  private void testMapFileIsSet() {
		  String pref_mapfile = getResources().getString(R.string.pref_mapfilename);
		  String mapfilepath = prefs.getString(pref_mapfile, "");
		  if (mapfilepath.equals("")) {
			  startMapFilePicker();
		  }
	  }
	  
	  @Override
	  protected void onResume() {
		super.onResume(); 
		//testMapFileIsSet();
		if (!(mIPAddressString.equals( ""))){
			//  we have a IP 
			  
			  //startAISService();   
		  }
	  }
	  
	  
	  @Override
	  protected void onStop() {
		  super.onStop();
		
	  }
	  @Override
	  protected void onStart() {
		  super.onStart();
		 
		 
	  }
	  @Override
	  protected void onRestart() {
		  super.onRestart();
	  }
	  
	  @Override
	  protected void onDestroy(){
		  stopAISService();
		  // we terminate correctly, so we do not have to signal that we must restore the cache ser file
		  String prev_backup_sercache_key =  getResources().getString(R.string.pref_backup_sercache_key);
		  prefs.edit()
		      .putBoolean(prev_backup_sercache_key, false)
		      .commit();
		  mDbAdapter.close();
		  Log.d(TAG,"Startpage-->onDestroy");
		  super.onDestroy();
	  }
	  
	  @Override
	    public boolean onKeyUp(int keyCode, KeyEvent event) {
	  
	    
	        if  (keyCode == KeyEvent.KEYCODE_BACK)
	        {
	        	setResult(RESULT_OK);
	        	
	            return  true;
	        } 
	        
	        //return super.onKeyUp(keyCode, event);
	        return false;
	    }
	  private boolean firstStart() {
		  boolean isFirstStart = prefs.getBoolean("firstStart", true);
		  return isFirstStart;
	  }
	  
	  private void doFirstStart() {
		  Toast.makeText(this, "first Start", Toast.LENGTH_LONG).show();
		  String pref_ownshipLatKey = getResources().getString(R.string.pref_OwnShipLAT);
		  String pref_ownshipLonKey = getResources().getString(R.string.pref_OwnShipLON);
		  String prev_data_dir_key = getResources().getString(R.string.pref_data_directory_key)	;
		  String prev_route_dir_key =  getResources().getString(R.string.pref_route_directory_key);
		  String prev_track_dir_key =  getResources().getString(R.string.pref_track_directory_key);
		  String prev_map_dir_key =  getResources().getString(R.string.pref_map_directory_key);
		  String prev_backup_dir_key =  getResources().getString(R.string.pref_backup_ser_directory_key);
		  String prev_onlinecache_dir_key = getResources().getString(R.string.pref_online_cache_directory_key);
		  String prev_backup_sercache_key =  getResources().getString(R.string.pref_backup_sercache_key);
		 
		  prefs.edit()
		    .putBoolean("firstStart", false)
		    .putString("mapGenerator",  "OPENSEAMAP")  // since 2014_01_27 prior MAPNIK
	     	.putString(pref_ownshipLatKey, "52' 55,672 N")
	     	.putString(pref_ownshipLonKey, "05' 24,345 E")
	     	.putString(prev_data_dir_key, AISPlotterGlobals.DEFAULT_APP_DATA_DIRECTORY)
	     	.putString(prev_route_dir_key, AISPlotterGlobals.DEFAULT_ROUTE_DATA_DIRECTORY)
	     	.putString(prev_track_dir_key, AISPlotterGlobals.DEFAULT_TRACK_DATA_DIRECTORY)
	     	.putString(prev_backup_dir_key,AISPlotterGlobals.DEFAULT_BACKUP_CACHE_DATA_DIRECTORY)
	     	.putString(prev_map_dir_key,AISPlotterGlobals.DEFAULT_MAP_DATA_DIRECTORY)
	     	.putString(prev_onlinecache_dir_key, AISPlotterGlobals.DEFAULT_ONLINE_TILE_CACHE_DATA_DIRECTORY)
	     	.putBoolean(prev_backup_sercache_key, false)
	     	.putBoolean("logTCPData", false)
	 	    .commit(); 
		  Toast.makeText(this, "no NMEA Server address set, set this first", Toast.LENGTH_LONG).show();
		  String extPathStr = PositionTools.getExternalStorageDirPath();
		  String data_dir_str = prefs.getString(prev_data_dir_key,  AISPlotterGlobals.DEFAULT_APP_DATA_DIRECTORY);
		  PositionTools.createExternalDirectoryIfNecessary(data_dir_str);
		  String backup_dir_str = prefs.getString(prev_backup_dir_key, AISPlotterGlobals.DEFAULT_BACKUP_CACHE_DATA_DIRECTORY);
		  PositionTools.createExternalDirectoryIfNecessary(backup_dir_str);
		  String route_dir_str = prefs.getString(prev_route_dir_key,AISPlotterGlobals.DEFAULT_ROUTE_DATA_DIRECTORY);
		  PositionTools.createExternalDirectoryIfNecessary(route_dir_str);
		  String track_dir_str = prefs.getString(prev_track_dir_key,AISPlotterGlobals.DEFAULT_TRACK_DATA_DIRECTORY);
		  PositionTools.createExternalDirectoryIfNecessary(track_dir_str);
		  String map_dir_str = prefs.getString(prev_map_dir_key,AISPlotterGlobals.DEFAULT_MAP_DATA_DIRECTORY);
		  PositionTools.createExternalDirectoryIfNecessary(map_dir_str);
		  String onlinecache_dir_str = prefs.getString(prev_onlinecache_dir_key,AISPlotterGlobals.DEFAULT_ONLINE_TILE_CACHE_DATA_DIRECTORY);
		  PositionTools.createExternalDirectoryIfNecessary(onlinecache_dir_str);
		  this.mDbAdapter.createRouteTable(AISPlotterGlobals.DEFAULTROUTE);
	  }
	  
	  /*private void createExternalDirectoryIfNecessaryOld(String pDirName) {
			if (test)
				Log.v(TAG, "createAISDirectory");
			String result = Environment.getExternalStorageState();
			if (result.equals(Environment.MEDIA_MOUNTED)) {

				// first we check if there is a directory named sdcard2
				File path = Environment.getExternalStorageDirectory();
				File[] subDirs = path.listFiles(new FileFilter() {
					@Override
					public boolean accept(File d) {
						return d.isDirectory();
					}
				});
				boolean foundSdCard2 = false;
				if (test)
					Log.d(TAG, "Subdirs of " + path.getName());
				if (test)
					Log.d(TAG, "Subdirs of " + path.getAbsolutePath());
				for (int index = 0; index < subDirs.length; index++) {
					String aName = subDirs[index].getName();
					if (test)
						Log.d(TAG, aName);
					if (aName.equals("sdcard2"))
						foundSdCard2 = true;
				}
				// if pDirName contains / we have to analyse the whole path
				String [] dirs = pDirName.split("/");
				int dirCount = dirs.length;
				String newDirName = "/";
			    //  now we know how many dirs we must create
				for (int dirIndex =0;dirIndex < dirCount; dirIndex++){
				
					StringBuffer buf = new StringBuffer();
					if (foundSdCard2)
						buf.append("sdcard2");
					newDirName = newDirName + dirs[dirIndex]  +"/";
					buf.append(newDirName);
					String dirName = buf.toString();
					File file = new File(path, dirName);
					try {
						String filePathStr = file.getAbsolutePath();
						if (file.mkdir()) { // here we need android permission in the manifest
							//if (test)
								Log.v(TAG, "erzeuge Directory: " + filePathStr);
						} else {
							// if (test)
								Log.v(TAG, "directory schon vorhanden " + filePathStr);
						}
					} catch (SecurityException se) {
						Log.d(TAG,se.toString());
						if (test)
							Log.v("TAG", "Security exception : Directory not created " + se);
					} catch (Exception e ) {
						Log.d(TAG,e.toString());
						
					} // try
				} //for
			}
		}*/
	  
	  final View.OnClickListener onStartButtonClick=new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				buttonStartPressed((Button) v);
			}
		};
		
		 final View.OnClickListener onStartButtonSplitClick=new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					buttonStartSplitPressed((Button) v);
				}
			};
			
			 final View.OnClickListener onDataScreenButtonClick=new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						buttonDataScreenPressed((Button) v);
					}
				};
		
		final View.OnClickListener onFinishButtonClick=new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				buttonFinishPressed((Button) v);
			}
		};
		
		final View.OnClickListener onConfirmButtonClick=new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				buttonConfirmPressed((Button) v);
			}
		};
		// Monitoring
		final View.OnClickListener onButtonMonitoringClick=new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				buttonMonitoringPressed((Button) v);
			}
		};
		
		
		// Test
		final View.OnClickListener onButtonTestClick=new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				buttonTestPressed((Button) v);
			}
		};
		// ListActivity
		final View.OnClickListener onButton4Click=new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				button4Pressed((Button) v);
			}
		};
		// startAISService
		final View.OnClickListener onButtonStartServiceClick=new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				buttonStartServicePressed((Button) v);
			}
		};
		// stopAISService
		final View.OnClickListener onButtonStopServiceClick=new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				buttonStopServicePressed((Button) v);
			}
		};
		
		public void buttonConfirmPressed(Button pConfirmButton) {
			Logger.d(TAG,"button Confirm pressed");
			startButton.setClickable(true);
			startButton.setVisibility(View.VISIBLE);
			//startSplitButton.setClickable(true);
			// startSplitButton.setVisibility(View.VISIBLE); the splitButton is disabled since 2013_12_09 , see AISPlotter onCreate
			if (mDataScreenPossible) {
				dataScreenButton.setClickable(true);
				dataScreenButton.setVisibility(View.VISIBLE);
			}
			 
			pConfirmButton.setClickable(false);
			pConfirmButton.setVisibility(View.INVISIBLE);
			TextView tv = (TextView)findViewById(R.id.info_text);
			tv.setVisibility(View.INVISIBLE);
			startpage_image.setVisibility(View.VISIBLE);
			// backUpTileCacheSerFile(); disabled since 12_11_19
		    startAISService();
		}
		
		public void buttonTestPressed(Button button) {
		      Log.v(TAG,"buttonTestPressed");
		      Logger.d(TAG,"buttonTest pressed");
		      //WifiManager theWifiManager = (WifiManager) Context.getSystemService(Context.WIFI_SERVICE);
		      /*WifiInfo aWifiInfo = theWifiManager.getConnectionInfo();
		      Log.d(TAG,aWifiInfo.toString());
		      DhcpInfo aDhcpInfo = theWifiManager.getDhcpInfo();
		      Log.d(TAG,aDhcpInfo.toString());*/
		 }
		
		
		 public void buttonStartPressed(Button button) {
		      Log.v(TAG,"buttonStartPressed");
		      
		      Intent i = new Intent(getApplicationContext(), AISTCPOpenMapPlotter.class);
		      i.putExtra("split", "false");
		      i.putExtra("datascreen", "false");
		      startActivity(i);
		 }
		 
		 public void buttonStartSplitPressed(Button button) {
		      Log.v(TAG,"buttonStartPressed");
		      
		      Intent i = new Intent(getApplicationContext(), AISTCPOpenMapPlotter.class);
		      i.putExtra("split", "true");
		      startActivity(i);
		 }
		 
		 public void buttonDataScreenPressed(Button button) {
		      Log.v(TAG,"buttonDataScreenPressed");
		      
		      Intent i = new Intent(getApplicationContext(), AISTCPOpenMapPlotter.class);
		      i.putExtra("datascreen", "true");
		      startActivity(i);
		 }
		 
		 public void buttonFinishPressed(Button button){
			 stopAISService();
			this.finish();
		 }
		 
		 public void buttonMonitoringPressed (Button button) {
			 //showPrefsInMonitor();
			 Intent i = new Intent(getApplicationContext(), MonitoringActivity.class);
		     startActivity(i); 
		 }
		 
		 public void button4Pressed (Button button) {
			 Intent i = new Intent(getApplicationContext(), AISListActivity.class);
		        startActivity(i); 
		 }
		 
		 public void buttonStartServicePressed (Button button) {
			 startAISService(); 
		 }
		 
		 public void buttonStopServicePressed (Button button) {
			stopAISService(); 
		 }
		 
		 public void startAISService() {
			 if (mNMEATCPVerwaltung == null) {
				 String aMsg = "Try to start ServiceFactory";
				 Log.d(TAG,aMsg);
				 Logger.d(TAG,aMsg);
				 mNMEATCPVerwaltung = NMEATCPServiceFactory.getNMEAVerwaltung(getApplicationContext()); 
			 }
			 else {
				 if (!mNMEATCPVerwaltung.isServiceRunning()) {
					 mNMEATCPVerwaltung.makeConnection(); 
				 }
			 }
				 
		 }
		 
		 public void disconnectNetworkServiceX() {
			 if (mNMEATCPVerwaltung != null) {
				// mNMEATCPVerwaltung.disconnectNetworkService();
				
			  }  
		 }
		 public void stopAISService() {
			 if (mNMEATCPVerwaltung != null) {
				 String aMsg = "Try to stop NMEATCPVerwaltung";
				 Log.d(TAG,aMsg);
				 Logger.d(TAG,aMsg);
				 NMEATCPServiceFactory.stopService();
				// mNMEATCPVerwaltung.stopService();
				 mNMEATCPVerwaltung = null;
			  } else {
				  String aMsg = "mNMEATCPVerwaltung = null, nothing to stop";
				 Log.d(TAG,aMsg);
				 Logger.d(TAG,aMsg);  
			  }
		 }
		 
		 
		 
		 public void showPrefsInMonitor() {
			/* prefs.edit()
				.remove("bthost")
			    .commit();*/
		 Map<String,?> aMap = prefs.getAll();
		 Logger.d(TAG, "All preference keys");
		 for (String aKey : aMap.keySet()) {
			 
			   //Logger.d(TAG,aKey);
			   Object aValue = aMap.get(aKey);
			   String aClassName = aValue.getClass().getSimpleName();
			   Logger.d(TAG,aClassName + " " + aKey + " " + aValue);
		  }
		 
		 if (mWifiManager != null){
		 DhcpInfo aDhcpInfo = mWifiManager.getDhcpInfo();
			int myIPAddress = aDhcpInfo.ipAddress;
			String mIPAddressString = "";
			for (int i = 0;i<4;i++){
				int val = myIPAddress % 256;
				mIPAddressString = mIPAddressString + Integer.valueOf(val)+".";
				myIPAddress = myIPAddress / 256;
			}
			mIPAddressString = mIPAddressString.substring(0,mIPAddressString.length()-1);
			Logger.d(TAG,"own IPAddress " + mIPAddressString);
		 }
		  
		  Logger.d(TAG, "special preference keys : values");
		  String aShipname = (String)aMap.get("shipname");
		  Logger.d(TAG, "shipname " + aShipname);
		  String prev_TCP_Server_address_key = getResources().getString(R.string.pref_host);
		  String aServerIPAddress = (String)aMap.get(prev_TCP_Server_address_key);
		  String prev_TCP_Port_address_key = getResources().getString(R.string.pref_port);
		  String aServerPort = (String)aMap.get(prev_TCP_Port_address_key);
		  Logger.d(TAG,"ServerIP " + aServerIPAddress + " Port " + aServerPort);
		  
		  String aRadius = (String)aMap.get("alarmradius");
		  Logger.d(TAG, "alarmradius " + aRadius);
		 }
		 
		private void resetDatabase() {
			mDbAdapter.resetDatabase();
		}
		
		/**
		 * backup  the tile cache.ser file 
		 */
		
		private void backUpTileCacheSerFile() {
			boolean persistent = prefs.getBoolean("cachePersistence", false);
			String prev_backup_sercache_key =  getResources().getString(R.string.pref_backup_sercache_key);
			// if the app crashed, we must restore save the last serfile to restore it
			// if the app was terminated correctly, mustRestoreCache will be false
			// this value is set in onDestroy()
			boolean mustRestoreCache = prefs.getBoolean(prev_backup_sercache_key, true);
			if (persistent && mustRestoreCache) {
		 		//String result = Environment.getExternalStorageState();
				String result = Environment2.getCardState();
				if (result.equals(Environment.MEDIA_MOUNTED)) {
					//File path = Environment.getExternalStorageDirectory();
					File path = Environment2.getCardDirectory();
					// we check if there is a directory sdcard2
					/* we use  the new gaetCardDirectory 12_11_19
					 * File[] subDirs = path.listFiles(new FileFilter() {
						@Override
						public boolean accept(File d) {
							return d.isDirectory();
						}
					});
					boolean foundSdCard2 = false;
					Log.d(TAG, "Subdirs of " + path.getName());
					Log.d(TAG, "Subdirs of " + path.getAbsolutePath());
					for (int index = 0; index < subDirs.length; index++) {
						String aName = subDirs[index].getName();
						Log.d(TAG, aName);
						if (aName.equals("sdcard2"))
							foundSdCard2 = true;
					}
					StringBuffer buf = new StringBuffer();
					buf.append(path.getAbsolutePath());
					buf.append("/");
					// if we found a sdcard2 directory we put it in front of the path
					if (foundSdCard2)
						buf.append("sdcard2/");*/
					StringBuffer buf = new StringBuffer();
					buf.append(path.getAbsolutePath());
					buf.append("/");
					String prev_backup_dir_key =  getResources().getString(R.string.pref_backup_ser_directory_key);
			 		String backup_dir_str = prefs.getString(prev_backup_dir_key, AISPlotterGlobals.DEFAULT_BACKUP_CACHE_DATA_DIRECTORY);
					buf.append(backup_dir_str);
					backup_dir_str = buf.toString();
					PositionTools.backUpTileCacheSerFile(backup_dir_str);
				} // Media mounted
	 		} // persistent
			prefs.edit()
			   .putBoolean(prev_backup_sercache_key, true)
			   .commit();
		}
		
		private void restoreTileCacheSerFile() {
			FilePicker.setFileDisplayFilter(PositionTools.FILE_FILTER_EXTENSION_BACKUPSER);
			FilePicker.setFileSelectFilter(null);
			startActivityForResult(new Intent(this, FilePicker.class), SELECT_BACKUPSER_FILE);	
		}
		
		private void saveAISDataToSDCard() {
			if (mNMEATCPVerwaltung != null) {
				TargetList aTargetList = mNMEATCPVerwaltung.getAISTargetList();
	      		if (test) {
	      			if (aTargetList != null) {
	      				Log.d(TAG,"Anzahl " + aTargetList.getSize());
	      		    }
	      		    else  {
	      			Log.d(TAG,"Targetlist ist leer");
	      		    }
		        }
	      		if (aTargetList != null) {
	      			Logger.d(TAG,"Anzahl der AIS-Ziele" + aTargetList.getSize());
	      			Logger.d(TAG,"AIS-Ziele werden exportiert");
	      			aTargetList.WriteAISDataToExternalStorage();
	      		}
	      		else {
	      			Logger.d(TAG,"Targetlist ist leer");
	      		}
			}
		}
		
		/** 
		 * show the data monitor
		 */
		private void showMonitor() {
			Intent i = new Intent(this, MonitoringActivity.class);
	        startActivity(i);	
		}
		
		@Override
		public boolean onCreateOptionsMenu(Menu menu) {
			getMenuInflater().inflate(R.menu.menustartpage, menu);
		    return true;
		}
		
		

		@Override
		public boolean onOptionsItemSelected(MenuItem item) {
			// Handle item selection
			int myId = item.getItemId();
		    switch (item.getItemId()) {
			  /*  case R.id.scan:
		        	if (test) Log.d(TAG,"Scan requested");
		            // Launch the DeviceListActivity to see devices and do scan
		            Intent serverIntent = new Intent(this, DeviceListActivity.class);
		            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
		            return true;*/
			    case R.id.opt_einstellungenAnzeigen: {    	
				      Intent i = new Intent(this, EinstellungenBearbeitenActivity.class);
				      startActivity(i);
				      return true;
				    }
			   /* we don't use it since 14_01_07 v054
			    * case R.id.opt_exportaisdata: {
			    	saveAISDataToSDCard();
			    	return true;
			        }*/
			    case R.id.opt_showinfo: {
			    	Intent i = new Intent(this,ReadAssetActivity.class);
			    	startActivity(i);
			    	return true; 
			    }
			    case R.id.opt_startpage_pickmap:{
			    	startMapFilePicker();
			    	return true;
			    }
			    case R.id.opt_startpage_show_log:
			    	showMonitor();
			    	return true;
			   /* 12_11_15 
			    * case R.id.opt_startpage_restore_Cache:
			    {
			       restoreTileCacheSerFile();
			    }*/
			    /*
			    case R.id.resetDatabase: {
			    	
			    }*/
			    default:
			        return super.onOptionsItemSelected(item);
		    }
		}
	   
		
		
	   @Override
		protected Dialog onCreateDialog(int id) {
			
		   switch (id){
				default:
					return super.onCreateDialog(id);
			}
			
		}
	   
	   
	   
	   /**
		 * Uses the UI thread to display the given text message as toast notification.
		 * 
		 * @param text
		 *            the text message to display
		 */
		void showToastOnUiThread(final String text) {

			if (AndroidUtils.currentThreadIsUiThread()) {
				Toast toast = Toast.makeText(this, text, Toast.LENGTH_LONG);
				toast.show();
			} else {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						Toast toast = Toast.makeText(StartPage.this, text, Toast.LENGTH_LONG);
						toast.show();
					}
				});
			}
		}
		
		
	   
	  /* public void onActivityResult(int requestCode, int resultCode, Intent data) {
		   // will be executed when a BT Device is selected
		   // currently not used 8.10.11
	        if(test) Log.d(TAG, "onActivityResult " + resultCode);
	        switch (requestCode) {
	        case REQUEST_CONNECT_DEVICE:
	            // When DeviceListActivity returns with a deviceaddress to connect
	            if (resultCode == Activity.RESULT_OK) {
	                // Get the device MAC address
	                String address = data.getExtras()
	                                     .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
	                if (test) Log.d(TAG,"here we set the deviceaddress " + address);
	                // we save it to the prefs
	                String prev_BT_Server_address_key = getResources().getString(R.string.pref_btaddress);
	                prefs.edit()
					.putString(prev_BT_Server_address_key, address)
				    .commit();
	                btAddress = address;
				    Logger.d(TAG,"BT Address set to: " + btAddress);
	            }
	           
	        }
	    }*/


		@Override
		protected void onPrepareDialog(int id, Dialog dialog) {
			
			switch(id) {
			}
			super.onPrepareDialog(id, dialog);
		}
		
		
		 private boolean copyFileFromAssetToStandardDirectory (String fileName) {
				boolean result = false;
				InputStream is = null;
				FileOutputStream fos = null;
				try {
					String prev_data_dir_key = getResources().getString(R.string.pref_data_directory_key)	;
					String data_dir_str = prefs.getString(prev_data_dir_key,  AISPlotterGlobals.DEFAULT_APP_DATA_DIRECTORY);  // AISPlotter
					String dest = PositionTools.getExternalStorageDirPath()+ "/" + data_dir_str + "/" + fileName;
					is = getAssets().open(fileName);
					fos = new FileOutputStream(dest);
					PositionTools.copy(is,fos);
					//String aStr = getResources().getString(R.string.main_activity_copy);
					//mMainTextView.append("\n" + "copy : " + fileName); 
					//mMainTextView.append("\n" + aStr + fileName);  
					result = true;
				} catch (IOException e){
					if (test) Log.d(TAG,"cant create file "+ e.toString());
					//String aStr = getResources().getString(R.string.main_activity_create_std_renderer);
					//mMainTextView.append("\n" + "can't create OpenSeaRenderer "+ e.toString());
					//mMainTextView.append("\n" + aStr + e.toString());
				} finally {
					if (is != null)
						try { is.close(); }catch (IOException e){}
					if (fos != null)
						try { fos.close(); } catch (IOException e) {}
				}
				return result;
			}
		 
		 private boolean copyFileFromAssetToDest (String fileName,String dest) {
				boolean result = false; 
				InputStream is = null;
				FileOutputStream fos = null;
				try {
					is = getAssets().open(fileName);
					fos = new FileOutputStream(dest);
					PositionTools.copy(is,fos);
					
					result = true;
				} catch (IOException e){
					if (test) Log.d(TAG,"cant create file "+ e.toString());
					
				} finally {
					if (is != null)
						try { is.close(); }catch (IOException e){}
					if (fos != null)
						try { fos.close(); } catch (IOException e) {}
				}
				return result;
			} 
		 
		 
		 private void copyStandardSymbolDefsIfNecessary() {
			   String symboldefsName = AISPlotterGlobals.DEFAULT_SEAMARKS_SYMBOL_FILENAME;
			   String prev_data_dir_key = getResources().getString(R.string.pref_data_directory_key)	;
			   String data_dir_str = prefs.getString(prev_data_dir_key,  AISPlotterGlobals.DEFAULT_APP_DATA_DIRECTORY);  // AISPlotter
			   String dest = PositionTools.getExternalStorageDirPath()+ "/" + data_dir_str + "/" + symboldefsName;
			   File aTestFile = new File(dest);
			   if (!aTestFile.exists()){
				   boolean ok = copyFileFromAssetToStandardDirectory (symboldefsName);
				   if (!ok) {
					   showToastOnUiThread("Could not create seamarks symbol file") ; 
				   }
			   }
			  
		   }
		 
		 private void copyStandardRendererIfNecessary() {
			   String renderThemeName = AISPlotterGlobals.DEFAULT_STANDRAD_RENDERER_FILENAME;
			   String prev_data_dir_key = getResources().getString(R.string.pref_data_directory_key)	;
			   String data_dir_str = prefs.getString(prev_data_dir_key,  AISPlotterGlobals.DEFAULT_APP_DATA_DIRECTORY);  // AISPlotter
			   String dest = PositionTools.getExternalStorageDirPath()+ "/" + data_dir_str + "/" + renderThemeName;
			   File aTestFile = new File(dest);
			   if (!aTestFile.exists()){
				   boolean ok = copyFileFromAssetToStandardDirectory (renderThemeName);
				   if (!ok) {
					   showToastOnUiThread("Could not create standard renderer file") ; 
				   }
			   }
		   }
		 
		 private void copyTestMapIfNecessary() {
			   String testMapName = AISPlotterGlobals.OPENSEAMAP_TESTMAP; 
			   String map_dir_str = AISPlotterGlobals.DEFAULT_MAP_DATA_DIRECTORY;
			   String dest = PositionTools.getExternalStorageDirPath()+ "/" + map_dir_str + "/" + testMapName+".map";
			   File aTestFile = new File(dest);
			   if (!aTestFile.exists()){
				   boolean ok = copyFileFromAssetToDest (testMapName+".map", dest);
			   }
			   
			    dest = PositionTools.getExternalStorageDirPath()+ "/" + map_dir_str + "/" + testMapName+"_seamarks.xml";
			    aTestFile = new File(dest);
			    if (!aTestFile.exists()){
				   boolean ok = copyFileFromAssetToDest (testMapName+"_seamarks.xml",dest);
			   }
			   
			  
			    
		   }

}