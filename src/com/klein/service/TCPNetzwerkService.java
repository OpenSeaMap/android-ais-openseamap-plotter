package com.klein.service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Calendar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.klein.aistcpopenmapplotter051.R;
import com.klein.commons.AISPlotterGlobals;
import com.klein.commons.PositionTools;
import com.klein.logutils.Logger;








public class TCPNetzwerkService extends Service {
	private static  boolean test = false;
	;
	// Beachte hierbei unbedingt die Angaben im manifest, Servicename mit Package angeben
	private static final String TAG = "TCPNetzwerkService";
	
	public static final String DEFAULT_SERVER_ADDRESS = "172.016.200.093";
    public static final String DEFAULT_SERVER_PORT = "9999";
    
   // public static final float DEFAULT_ALARM_RADIUS = 4f; // 4 miles
    public static final float DEFAULT_ZOOM_FACTOR = 2;
	
	//public static final String PREF_SERVER_ADDRESS = "host";  //  wird nicht mehr verwendet siehe dazu die definition in der xml Resource
	//public static final String PREF_SERVER_PORT = "port";
	SharedPreferences prefs;
	static String serverAddress;
    String serverPortStr;
	static int port;
	
	private static String mLinuxBoxCmdWatchStr;
	
	//private boolean isBusy = false;
	private static boolean isNMEAListenerRunning = false;
	private static Socket sock;
	private static BufferedReader is;
	private static PrintWriter pw;
	private static TCPNetzwerkService mService;
	
	
	private final IBinder NMEABinder = new NMEALocalBinder();
	//private static Handler mHandler = new Handler();
	
	private ConnectionBroadcastReceiver mBroadcastReceiver;
	private final IntentFilter mIntentFilter = new IntentFilter();
	Handler uiServiceCallbackHandler;
	
	
	private static final int NOTIFICATION_ID = 77;
	private static Thread theThread = null;
	
	
	private static NMEAParser mNMEAParser = null;
	private static int mUseGPSSource = AISPlotterGlobals.NO_GPS;
	
	private NotificationManager mNM;
	
	private static BufferedWriter mLogfileBuf = null;
	private static boolean mMustLogNmeaData = false;
	
	private static class ConnectionBroadcastReceiver extends BroadcastReceiver {             
	    @Override
	    public void onReceive(Context ctxt, Intent intent) { 
	    boolean savedStatus = test;
	    //test = true;
	    if (intent != null){
		    String action = intent.getAction(); 
		    if (test)Log.d(TAG,"intent received " + action);
		    if (action.equals("android.net.conn.CONNECTIVITY_CHANGE")){
			      try {
			        if (test)Log.d(TAG, "Connection Broadcast onReceive(): entered..."); 
			        // enthaelt den APN falls vorhanden, sonst null
			        String info = intent.getStringExtra(ConnectivityManager.EXTRA_EXTRA_INFO);
			 
			        if (test)Log.d(TAG,"info: " + info);
			        
			        NetworkInfo nInfo = intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
			        if (test)Log.d(TAG,"network info " + nInfo.toString());
			        String nType = nInfo.getTypeName();
			        
			        String nSubType = nInfo.getSubtypeName();  // ist bei WIFI ""
			        String extraInfo = nInfo.getExtraInfo();
			        NetworkInfo.State nState = nInfo.getState();
			        if (test) Log.d(TAG,"Type " + nType + " Subtype " + nSubType + " ExtraInfo " + extraInfo);
			        // Type MOBILE Subtype UMTS ExtraInfo internet.eplus.de
			        //  Type WIFI  Subtype "" 
			        if (!(nType.equals("WIFI"))) {
			        	if (test) Log.d(TAG,"Connection Broadcast onReceive()--> no WIFI , break");
			        	return; // wenn kein WIFI Abbruch!!
			        }
			        boolean isNotConnected = (nState != NetworkInfo.State.CONNECTED);
			         // intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
			        // Zeige Warnung, solange keine Netzwerkverbindung besteht:
			        if (isNotConnected) {
			          if (test)Log.d(TAG, "onReceive(): Netzwerkverbindung verloren...");
			          Logger.d(TAG,"TCP-Connection lost " + serverAddress);
			          if ( mService != null) mService.showNotification(false);
			          if (sock != null && sock.isConnected()) {
			            if(test)Log.w(TAG, "onReceive(): still connected!");
			            sock.close();
			            sock = null;
			          }
			        }
			        else {
			          
			          // Damit nicht zweimal ein Socket eingerichtet wird, pruefe ob sock == null
			          // onReceive wird auch beim Start des Services aufgerufen
			          /*if (nType.equals("WIFI")&& (nState == NetworkInfo.State.CONNECTED)&& (sock == null)){
			        	  if (test)Log.d(TAG, "onReceive(): Verbindung besteht wieder. Starte Connection neu...");
				          if (netzwerkVerbindungHerstellen()) {
				            starteNMEAListener();
				            
				          }  
			          }*/
			          
			        }
			      } 
			      catch (Exception e) {
			        if (test)Log.e(TAG, "Fehler: " + e.toString());
			      }
		    } // "android.net.conn.CONNECTIVITY_CHANGE"
		    if (action.equals(AISPlotterGlobals.ACTION_SENDGPSNMEADATA)) {
		    	Toast.makeText(ctxt, "GPS-Data request received" , Toast.LENGTH_SHORT).show();
				if (test) Log.d(TAG,"gpsNMEADataAcquireReceiver -- ");
					if (mNMEAParser != null){
						Logger.d(TAG,"Send new GPS data requested");
						mNMEAParser.sendNMEAPositionViaBroadcast();
					}
		    }
		    
		    if (action.equals(AISPlotterGlobals.ACTION_SHOW_AISMSG_INLOG)) {
		    	// inform the parser to log decodedAISMSG
		    	boolean mustLog = intent.getBooleanExtra(AISPlotterGlobals.SHOW_AISMSG_INLOG_KEY, true);
		    	mNMEAParser.setLogPositionData(mustLog);
		    }
	     } // intent != null
	     test = savedStatus;
	    } // onReceive
	    
	  }; // Class 
	
	
	public class NMEALocalBinder extends Binder {
	    public TCPNetzwerkService getService() {
	    	if (test)Log.d(TAG, "Netzwerkservice->getService(): entered...");
	        return TCPNetzwerkService.this;
	    }
	    public void setCallbackHandler(Handler callbackHandler) {
	    	if (test)Log.d(TAG, "Netzwerkservice->setCallbackHandler(): entered...");
	      uiServiceCallbackHandler = callbackHandler;
	    }
	    
	    
	    
	  }
	
	@Override
	public IBinder onBind(Intent intent) {
		if (test) Log.d(TAG, "onBind(): entered...");    
	    return NMEABinder; 
	}
	
	@Override
	public void onCreate() {
		mService = this;
		if (test){
			Log.d(TAG, "oncreate(): entered..."); 
			Logger.d(TAG," onCreate(): entered");
		}
		// Dieser Teil stammt aus amando services
		mBroadcastReceiver = new ConnectionBroadcastReceiver();
		mIntentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
		mIntentFilter.addAction(AISPlotterGlobals.ACTION_SENDGPSNMEADATA);
		mIntentFilter.addAction(AISPlotterGlobals.ACTION_SHOW_AISMSG_INLOG);
		
	    registerReceiver(mBroadcastReceiver, mIntentFilter);
		
		     
		
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		// prev_Server_address_key  = "host" , das ist der key in den Resourcen definiert ist
		String prev_Server_address_key = getResources().getString(R.string.pref_host);
	    String prev_Server_port_key = getResources().getString(R.string.pref_port);
		serverAddress = prefs.getString(prev_Server_address_key, DEFAULT_SERVER_ADDRESS);
	    serverPortStr = prefs.getString(prev_Server_port_key, DEFAULT_SERVER_PORT);
	    port = Integer.parseInt(serverPortStr);
	    mMustLogNmeaData = prefs.getBoolean("logTCPData", false);
	   // we read from the prefs whether we should save the position data to the database
	    String prev_save_position_data_key = getResources().getString(R.string.pref_save_position_data);
	    String savePosDataStr = prefs.getString(prev_save_position_data_key, "on");
	    boolean savePositionData = savePosDataStr.equalsIgnoreCase(("on"));
	    boolean logPositionData = prefs.getBoolean(Logger.KEY_IS_AISMSG_LOG_ENABLED, true);
	    setGPSSourceFromPrefs();
	    mNMEAParser = new NMEAParser(this,mUseGPSSource,savePositionData,logPositionData); 
	    setLinuxBoxCmdWATCHStr ();
	    makeConnection();
	    
	}
	
	private void setGPSSourceFromPrefs(){
		  mUseGPSSource = AISPlotterGlobals.NO_GPS;
		  String prev_GPS_key = getResources().getString(R.string.pref_use_gps_source); 
		  String[] gpsValues = getResources().getStringArray(R.array.use_gps_values);
		  String defaultValue = gpsValues[0];
		  String gpsSourceStr = prefs.getString(prev_GPS_key, defaultValue); 
		  if (gpsSourceStr.equals(gpsValues[1])) {
			  mUseGPSSource = AISPlotterGlobals.INTERNAL_GPS; 
		  }
		  if (gpsSourceStr.equals(gpsValues[2])) {
			  mUseGPSSource = AISPlotterGlobals.NMEA_GPS; 
		  }
	  }
	
	
	public void makeConnection() {
		
		String aLoggerMessage = getResources().getString(R.string.try_to_connect);
	    Logger.d(TAG,aLoggerMessage+ " "+ serverAddress + "  port " + port);
	    Log.d(TAG,"make Connection -->" + aLoggerMessage+ " "+ serverAddress + " port " + port);
	    if (isNMEAListenerRunning)return;
	    if (netzwerkVerbindungHerstellen()) {  
		 
		      startNMEAListener();
		      showNotification(true);
		      if ( mMustLogNmeaData) {
		    	  initLogAllTCPDataToExternalStorage();
		      }
		      aLoggerMessage = getResources().getString(R.string.try_to_connect_ok);
		      Logger.d(TAG,aLoggerMessage + " " + 
		    		  sock.getInetAddress().getHostAddress() +":"+ sock.getLocalPort());
		} else {
			showNotification(false);
			aLoggerMessage = getResources().getString(R.string.try_to_connect_failed);
			Logger.d(TAG,aLoggerMessage + " " + serverAddress + "port " + port);
		}
	}
	
	private void showNoNewAISDataNotification() {
		Log.d(TAG,"Show no new AISDatat Notification"); 
		mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		String aNotificationTitle = getResources().getString(R.string.tcp_network_service); 
		String aNotificationMessage;
		String aPendingMessage;
		
		
		int aVersion  = Build.VERSION.SDK_INT;
		String aVersionStr = Build.VERSION.CODENAME;
		
		  aNotificationMessage = getResources().getString(R.string.no_new_ais_data);
		  Notification aNotification = new Notification (R.drawable.iconnoais, aNotificationMessage,
			        System.currentTimeMillis());
		  /*
		  Problem in Android 4 and later
		  PendingIntent pendingIntent = 
			      PendingIntent.getActivity(null, 0, null, 0);
			      12_11_20  set pending intent to null, we do not start 
			      a activity from the pending intent
		 
		  */
		  PendingIntent pendingIntent = null;
		  if (aVersion < 14) pendingIntent = PendingIntent.getActivity(this, 0, null, 0);
		    
		  aPendingMessage = getResources().getString(R.string.no_new_ais_data); 
		  aNotification.setLatestEventInfo(this, 
			        aNotificationTitle,
			        aPendingMessage, pendingIntent); 
			    
			    // Ab Android 2.0: 
		  startForeground(NOTIFICATION_ID,aNotification);
		 // mNM.notify(NOTIFICATION_ID,aNotification);
	}
	
	private void showNotification(boolean isOn) { 
		mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		String aNotificationTitle = getResources().getString(R.string.tcp_network_service); 
		String aNotificationMessage;
		String aPendingMessage;
		// since 4.0we can send Notifications from a service, so we send a Broadcast to the activities
		String isOnStr;
		if (isOn) {
			isOnStr = "on";
		} else {
			isOnStr = "off";
		}
		
		int aVersion  = Build.VERSION.SDK_INT;
		String aVersionStr = Build.VERSION.CODENAME;
		if (isOn){
		  aNotificationMessage = getResources().getString(R.string.ais_runs);
		  Notification aNotification = new Notification (R.drawable.iconais, aNotificationMessage,
			        System.currentTimeMillis());
		  /*
		  Problem in Android 4 and later
		  PendingIntent pendingIntent = 
			      PendingIntent.getActivity(null, 0, null, 0);
			      12_11_20  set pending intent to null, we do not start 
			      a activity from the pending intent
		 
		  */
		  PendingIntent pendingIntent = null;
		  if (aVersion < 14) pendingIntent = PendingIntent.getActivity(this, 0, null, 0);
		    
		  aPendingMessage = getResources().getString(R.string.delivering_ais_data); 
		  aNotification.setLatestEventInfo(this, 
			        aNotificationTitle,
			        aPendingMessage, pendingIntent); 
			    
			    // Ab Android 2.0: 
		  startForeground(NOTIFICATION_ID,aNotification);
		 // mNM.notify(NOTIFICATION_ID,aNotification);
		}else {
		  aNotificationMessage = getResources().getString(R.string.no_new_ais_data);
		  Notification aNotification = new Notification (R.drawable.iconnoais, aNotificationMessage,
			        System.currentTimeMillis());
		  /*
		  Problem in Android 4 and later
		  PendingIntent pendingIntent = 
			      PendingIntent.getActivity(null, 0, null, 0);
			      12_11_20  set pending intent to null, we do not start 
			      a activity from the pending intent
		 
		  */
		  PendingIntent pendingIntent = null;
		  if (aVersion < 14) pendingIntent = PendingIntent.getActivity(this, 0, null, 0);
		  aPendingMessage = getResources().getString(R.string.no_network_data); 
		  aNotification.setLatestEventInfo(this, 
				    aNotificationTitle,
			        aPendingMessage, pendingIntent); 
			    
			    // Ab Android 2.0: 
		  mNM.notify(NOTIFICATION_ID,aNotification);
		}
	}
		
		
	
	private void cancelNotification(){
		
		mNM.cancel(NOTIFICATION_ID);
	}
	
	
	private static boolean netzwerkVerbindungHerstellen() {
	    if (test) Log.d(TAG, "netzwerkVerbindungHerstellen(): Netzwerkverbindung herstellen...");
	    
	    try {
	      // Neu: Socket mit Timeout, falls der Server nicht verfuegbar ist:
	      sock = new Socket();      
	      sock.connect(new InetSocketAddress(serverAddress, port), 4000);
	      is   = new BufferedReader(new InputStreamReader(sock.getInputStream()));
	      pw   = new PrintWriter(sock.getOutputStream(), true);
	      sock.setSoTimeout(120000); // this causes a InterruptedIOException  in the NMEAListener if no data is send for 120 seconds = 2 minutes
	      sock.setKeepAlive(true);
	      int aTimeout = sock.getSoTimeout();
		  Log.d(TAG,"socktimeout = " + aTimeout);     
	    } 
	    catch (UnknownHostException e) {  
	      if (test)Log.e(TAG, "netzwerkVerbindungHerstellen unknownHost : " + e.toString());
	      sock = null;
	      return false;
	    } 
	    catch (IOException e) {
	      if (test)Log.e(TAG, "netzwerkVerbindungHerstellen IO Exception: " + e.toString());
	      sock = null;
	      Logger.d(TAG,e.toString());
	      return false;
	    } 
	    catch (Exception e) {
	    	if (test) Log.e(TAG, "netzwerkVerbindungHerstellen Exception: " + e.toString());
	    	sock = null;
	    	return false;
	    }
	    if (test) Log.d(TAG, "netzwerkVerbindungHerstellen(): Netzwerkverbindung hergestellt");
	    
	    return true;
	  }
	
	private void setLinuxBoxCmdWATCHStr () {
		
		StringBuilder buf = new StringBuilder();
		buf.append("?WATCH={");
		buf.append('"');
		buf.append("enable");
		buf.append('"');
		buf.append(":true,");
		buf.append('"');
		buf.append("raw");
		buf.append('"');
		buf.append(":1};");
		mLinuxBoxCmdWatchStr = buf.toString();
	}
	
	private static void enableRawDataFromGPSD(String aMessage) {
		try {
			JSONObject aJSONObject = new JSONObject(aMessage);
			String classVal = aJSONObject.getString("class");
			if (classVal.equals("VERSION")) {
			  String versionVal = aJSONObject.getString("release");
			  String jsonResult = classVal + " " + versionVal;
			  Logger.d(TAG,"JSON found " + jsonResult);
			// we send ?WATCH={"enable":true,"raw",1};
			  String cmdStr = mLinuxBoxCmdWatchStr;
				if (test)Log.d(TAG,cmdStr);
				Logger.d(TAG,"enable GPSD raw NMEA" + cmdStr);
				sendString(cmdStr);
			}
			if (classVal.equals("DEVICES")) {
				  JSONArray devices = aJSONObject.getJSONArray("devices");
				  int count = devices.length();
				  for (int index = 0;index < count;index++ ){
					  JSONObject device = devices.getJSONObject(index);
					  String deviceVal = device.getString("class");
					  String pathVal = device.getString("path");
					  String driverVal = device.getString("driver");
					  int bpsVal = device.getInt("bps");
					  String jsonResult = classVal + " " + pathVal + " " + driverVal + " " + bpsVal;
					  Logger.d(TAG,"JSON found " + jsonResult);
					  if (test)Log.d(TAG,"JSON found " + jsonResult);
				  }
				}
			if (classVal.equals("WATCH")) {
				  boolean enableVal = aJSONObject.getBoolean("enable");
				  String jsonResult = classVal + " " + enableVal;
				  Logger.d(TAG,"JSON found " + jsonResult);
				  if (test)Log.d(TAG,"JSON found " + jsonResult);
				}
		} catch (JSONException e){
			Log.d(TAG, e.toString());
		}
	}
	
	public static void processMessageOld (String aMessage){
		// till 2013_11_30, no canboat nmea 2000 analyse
		if (aMessage.length()<1) return;
		if (aMessage.contains("nodata")) Logger.d(TAG,"nodata recognized " + aMessage);
		if (test) Logger.d(TAG,aMessage);
		if (test) Log.d(TAG,aMessage);
		
		// if we get a response from the gpsd it should show like
		//{"class":"VERSION","release":"2.94",...}
		// then we have to  send ?WATCH={"enable":true,"raw",1};
		
		if ( aMessage.charAt(0)== '{') {
			enableRawDataFromGPSD(aMessage);
		} else {
			if (mNMEAParser != null )mNMEAParser.processMessage(aMessage);
		}
		if (test)  Log.d(TAG,aMessage);
	}
	
	public static void processMessage (String aMessage){
		// can process canboat nmea2000 2013_11_30
		if (aMessage.length()<1) return;
		logTCPMessageToExternalStorage(aMessage);
		if (aMessage.contains("nodata")) Logger.d(TAG,"nodata recognized " + aMessage);
		// if we get a message from the gpsd it should show like
		//{"class":"VERSION","release":"2.94",...}
		// then we have to  send ?WATCH={"enable":true,"raw",1};
		// a typical canboat message looks like
		// {"timestamp":"2013-10-17-19:39:51.544","prio":"3","src":"160","dst":"255","pgn":"129029",
		//  "description":"GNSS Position Data","fields":{field data, the main info}}
		if (test) 
			Logger.d(TAG,aMessage);
		if (test)
			Log.d(TAG,aMessage);
		
		if ( aMessage.charAt(0)== '{') {
			JSONObject aJSONObject = null;
			try {
				aJSONObject = new JSONObject(aMessage);
			} catch (JSONException e){
				Log.d(TAG, e.toString());
			}
			String classVal = null;
			try {
				classVal = aJSONObject.getString("class");
			} catch (JSONException e){
				// no Property "class" , maybe another JSON record
				if (test) Log.d(TAG, e.toString());
			}
			if (classVal != null){
				try {
				if (classVal.equals("VERSION")) {
				  String versionVal = aJSONObject.getString("release");
				  String jsonResult = classVal + " " + versionVal;
				  Logger.d(TAG,"JSON found " + jsonResult);
				// we send ?WATCH={"enable":true,"raw",1};
				  String cmdStr = mLinuxBoxCmdWatchStr;
					Log.d(TAG,cmdStr);
					Logger.d(TAG,cmdStr);
					sendString(cmdStr);
				}
				if (classVal.equals("DEVICES")) {
					  JSONArray devices = aJSONObject.getJSONArray("devices");
					  int count = devices.length();
					  for (int index = 0;index < count;index++ ){
						  JSONObject device = devices.getJSONObject(index);
						  String deviceVal = device.getString("class");
						  String pathVal = device.getString("path");
						  String driverVal = device.getString("driver");
						  int bpsVal = device.getInt("bps");
						  String jsonResult = classVal + " " + pathVal + " " + driverVal + " " + bpsVal;
						  Logger.d(TAG,"JSON found " + jsonResult);
						  Log.d(TAG,"JSON found " + jsonResult);
					  }
					}
				if (classVal.equals("WATCH")) {
					  boolean enableVal = aJSONObject.getBoolean("enable");
					  String jsonResult = classVal + " " + enableVal;
					  Logger.d(TAG,"JSON found " + jsonResult);
					  Log.d(TAG,"JSON found " + jsonResult);
					}
				} catch (JSONException e){
					Log.d(TAG, e.toString());
				}
		    } else { // JSON Record but no Class record
		       Log.d(TAG,aMessage);
		       if (mNMEAParser != null )mNMEAParser.processMessage(aMessage);
		    } 
			
		}  else {// no  leading { marker
			// we have a nmea0183 from the gpsd or another json record from the canboat software
		     //sendNMEAStringViaBroadcast(aMessage);
		    	if (mNMEAParser != null )mNMEAParser.processMessage(aMessage);
		}
		if (test) 
			Log.d(TAG,aMessage);
	}
	
	public static void sendString (String pMsg){
		if (sock != null && !sock.isClosed()) {
			pw.println(pMsg);
			pw.flush();
			if (test) Log.d(TAG, "send: "+ pMsg);
		}
	}
	
	private static void startNMEAListener() {
	    Log.d(TAG,"starteNMEAListener()->entered...");
	    theThread = new Thread() {
	      public void run() {  
	    	  String line="xxx";;
	        try {
	           if (!isNMEAListenerRunning ){
	                theThread = null;
	                if (test) Log.d(TAG,"isRunning = false");
	           }else {
		          //String line;
		          //boolean routenInfoGeloescht = false;          
		          // blockiert, bis eine neue Zeile ankommt:
		          while (isNMEAListenerRunning && sock != null && !sock.isClosed() && 
		              (line = is.readLine()) != null) {
		            if (test) Log.d(TAG, "run()->Vom Server uebermittelt: " + line);
		            if (line.length() < 1) {
		            	if (test)Log.e(TAG,"line is empty " + line);
		            } else {
		            	processMessage(line); 
		    
		            }
		                  
		          } 
		          if (test) Log.d(TAG,"Leseschleife beendet");
	           }
	        } 
	        catch (InterruptedIOException e) { // a socket timeout (sock.setSoTimeout(120000); ) has happened
	        	mService.showNoNewAISDataNotification();
	        	//mService.showNotification(false);
	        	if (test);
	        		Log.d(TAG,"InterruptedIOException in starteNMEAListener->run()->Fehler: " + e.toString());
	        }
	        catch (IOException ex) {
	          if (test) Log.d(TAG,"IO exception in starteNMEAListener->run()->Fehler: " + ex.toString());
	          return;
	        }
	        catch (IllegalStateException ex) {
	        	if (test)Log.d(TAG,"Illegal State exception in starteNMEAListener->run()->Fehler: " + ex.toString());
	        	ex.printStackTrace();
	        }
	        catch (Exception ex){
	        	if (test) Log.d(TAG,"Exception in starteNMEAListener->run()->Fehler: " + line + " " +ex.toString());
	        	ex.printStackTrace();
	        }
	        finally {
	        	if (test)Log.d(TAG,"execute finally");
	          if (sock == null) {
	        	  if (test)Log.d(TAG, "starte NMEAListerner->run() finally sock = null");
	          }
	          else if (sock.isClosed()){
	        	  if (test)Log.d(TAG, "starte NMEAListerner->run() finally sock was closed");
	          }
	          isNMEAListenerRunning = false;
	        /*  if (theThread != null) {
	  			theThread.interrupt();
	          }*/
	          theThread = null;
	          Log.d(TAG,"NMEAListerner->run() finally done, Thread terminated");
	         
	          }  
	        }
	    };  // new Thread
	    
	    
	    if (sock != null) {
	    	isNMEAListenerRunning = true;
	    	theThread.start();
	    	if (test)Log.d(TAG,"starteNMEAListener()-> listener gestartet...");
	    	}
	    if (test)Log.d(TAG,"starteNMEAListener()->end...");
	  }
	
	private static void starteNMEAListenerOld() { // until 12_05_10
	    Log.d(TAG,"starteNMEAListener()->entered...");
	    theThread = new Thread() {
	      public void run() {        
	        try {
	           if (!isNMEAListenerRunning ){
	                theThread = null;
	                if (test) Log.d(TAG,"isRunning = false");
	           }else {
		          String line;
		          //boolean routenInfoGeloescht = false;          
		          // blockiert, bis eine neue Zeile ankommt:
		          while (isNMEAListenerRunning && sock != null && !sock.isClosed() && 
		              (line = is.readLine()) != null) {
		            if (test)Log.d(TAG, "run()->Vom Server uebermittelt: " + line);
		            processMessage(line);       
		            } 
		          Log.d(TAG,"Leseschleife beendet");
	           }
	        } 
	        catch (IOException ex) {
	          Log.d(TAG,"IO exception in starteNMEAListener->run()->Fehler: " + ex.toString());
	          return;
	        }
	        catch (IllegalStateException ex) {
	        	Log.d(TAG,"Illegal State exception in starteNMEAListener->run()->Fehler: " + ex.toString());
	        	ex.printStackTrace();
	        }
	        catch (Exception ex){
	        	Log.d(TAG,"Exception in starteNMEAListener->run()->Fehler: " + ex.toString());
	        	ex.printStackTrace();
	        }
	        finally {
	        	Log.d(TAG,"execute finally");
	          if (sock == null) {
	        	  Log.d(TAG, "starte NMEAListerner->run() finally sock = null");
	          }
	          else if (sock.isClosed()){
	        	  Log.d(TAG, "starte NMEAListerner->run() finally sock was closed");
	          }
	          isNMEAListenerRunning = false;
	        /*  if (theThread != null) {
	  			theThread.interrupt();
	          }*/
	          theThread = null;
	          Log.d(TAG,"NMEAListerner->run() finally done, Thread terminated");
	          }  
	        }
	    };  // new Thread
	    
	    
	    if (sock != null) {
	    	isNMEAListenerRunning = true;
	    	theThread.start();
	    	if (test)Log.d(TAG,"starteNMEAListener()-> listener gestartet...");
	    	}
	    if (test)Log.d(TAG,"starteNMEAListener()->end...");
	  }
	
	private void stopNMEAListener(){
		if (test)Log.d(TAG, "stopNMEAListerner service begin ");
		isNMEAListenerRunning = false;
		if (theThread != null) {
			theThread.interrupt();
		}
		cancelNotification();
		theThread = null;
		if (test) Log.d(TAG, "stopNMEAListerner service finished");
    
    }
	
	
	@Override
	  public void onDestroy() {
		stopNMEAListener();
		try {
		 if (mLogfileBuf != null && theThread == null){
			 mLogfileBuf.close();
		 }
		}catch (IOException ex) {
	          if (test) Log.d(TAG,"nmea0183 logger not closed " + ex.toString());
	           return;
	    }
	
		unregisterReceiver(mBroadcastReceiver);
	    cancelNotification();
	    if (mNMEAParser != null) mNMEAParser.destroy();
		if (test)Log.d(TAG, "onDestroy(): LocalService beenden...");
		try {
		 if (sock != null) {
			  sock.close();
			  if (test) Log.d(TAG,"Socket closed");
		 }
		} catch (IOException ex) {
	          if (test) Log.d(TAG,"destroy->Fehler: " + ex.toString());
	           return;
	    }
	    if (test) Log.d(TAG, "onDestroy(): Local TCP-Service erfolgreich beendet..."); 
	    String aLoggerMessage = getResources().getString(R.string.tcp_network_service_terminated);
	    Log.d(TAG,aLoggerMessage);
	    Logger.d(TAG,aLoggerMessage);
	    super.onDestroy();
	  }
	
	public boolean isServiceRunning () {
		return isNMEAListenerRunning;
	}
	
	public TargetList getAISTargetList(){
	  if (mNMEAParser != null) {
		  return mNMEAParser.getAISTargetList();
	  }
	  else {
		  return null;
		  }
	}
	
	private static String getCurrentDateTime() {
		final Calendar c = Calendar.getInstance();
        int mYear = c.get(Calendar.YEAR);
        int mMonth = c.get(Calendar.MONTH);
        int mDay = c.get(Calendar.DAY_OF_MONTH);
        int mHour = c.get(Calendar.HOUR_OF_DAY);
        int mMinute = c.get(Calendar.MINUTE);
        int mSecond = c.get(Calendar.SECOND);
        StringBuffer buf = new StringBuffer();
        buf.append(mYear);
        buf.append("_");
        buf.append(mMonth +1);
        buf.append("_");
        buf.append(mDay);
        buf.append("_");
        buf.append(mHour);
        buf.append("_");
        buf.append(mMinute);
        buf.append("_");
        buf.append(mSecond);
        String aDateStr = buf.toString();
        if (test) Log.v(TAG,"Datum: " + aDateStr);
        return aDateStr;
	}
	
	private  static void initLogAllTCPDataToExternalStorage() {
		 if (test) Log.v(TAG,"logAllNMEA0183DataToStorageAISData");
	    // Create a path where we will place our data in the user's
	    // public directory.  Note that you should be careful about
	    // what you place here, since the user often manages these files. 
		 // we write the data in a directory called AISPlotter/AISData
		 
		String aDataDirName = "/"+ AISPlotterGlobals.DEFAULT_APP_DATA_DIRECTORY
		                      +"/" + "NMEA0183Data";
		PositionTools.createExternalDirectoryIfNecessary(aDataDirName);
		String result = Environment.getExternalStorageState();
		if (result.equals(Environment.MEDIA_MOUNTED)){
		    File path = PositionTools.getExternalStorageDir();
	        StringBuffer buf = new StringBuffer();
	        buf.append(aDataDirName);
	        buf.append("/NMEA0183data");
	        buf.append(getCurrentDateTime());
	        buf.append(".txt");
	        String fileName = buf.toString();
		    File file = new File(path, fileName);
		    String filePathStr = file.getAbsolutePath();
		    try {
			    if (file.createNewFile()) {  // here we need android permission in the manifest
			        if (test) Log.v(TAG,"create file: " + filePathStr);
			    }else {
			    	if (test) Log.v(TAG,"file exists, overwrite " + filePathStr);
			    }
			    // the file exists or was opened for writing
			    mLogfileBuf = new BufferedWriter(new FileWriter(file));
			   
		    } catch (IOException e) {

		    	e.printStackTrace();
		        // Unable to create file, likely because external storage is
		        // not currently mounted.
		        if (test)Log.w("TAG", "Error writing " + filePathStr);
		    }  // try
		
	   }
	}
	
	private static void logTCPMessageToExternalStorage(String aMsg){
		try {
		if (mLogfileBuf != null &&  mMustLogNmeaData && aMsg != null) {
			 mLogfileBuf.write(aMsg);
			 mLogfileBuf.write("\n");
			 mLogfileBuf.flush();
		}
		} catch (IOException e){
			Logger.d("error  while writing nmealog " + e.toString());
		}
	}
	
	
	
}
