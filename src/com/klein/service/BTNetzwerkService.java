package com.klein.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.UUID;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.klein.aistcpopenmapplotter051.R;
import com.klein.commons.AISPlotterGlobals;
import com.klein.logutils.Logger;




public class BTNetzwerkService extends Service {

	private static final boolean test = false;
	// Beachte hierbei unbedingt die Angaben im manifest, Servicename mit Package angeben
	private static final String TAG = "BTNetzwerkService";
	public static final String DEFAULT_BT_SERVER_ADDRESS = "00:06:66:06:BF:CE"; 
	
 
	// Hint: If you are connecting to a Bluetooth serial board 
    // then try using the well-known SPP UUID 00001101-0000-1000-8000-00805F9B34FB. 
    // However if you are connecting to an Android peer then please generate your own unique UUID
    // We connect to a serial board
    
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

	
	/*public static final String PREF_SERVER_ADDRESS = "host";  //  wird nicht mehr verwendet siehe dazu die definition in der xml Resource
	public static final String PREF_SERVER_PORT = "port";*/
	
	SharedPreferences prefs;
	// Local Bluetooth adapter
	private static BluetoothAdapter mBluetoothAdapter = null;
	private static String btServerAddress;
	
	
	private static boolean isRunning = false;
	

	private static BluetoothSocket sock;
	private static BufferedReader is;
	private static PrintWriter pw;
	private static BTNetzwerkService mService;
	
	
	private final IBinder NMEABinder = new NMEALocalBinder();
	
	private BTConnectionBroadcastReceiver mBroadcastReceiver;
	//private final IntentFilter mIntentFilter = new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE");
	private final IntentFilter mIntentFilter = new IntentFilter("android.bluetooth.device.action.ACL_CONNECTED");
	Handler uiServiceCallbackHandler;
	
	
	private static final int NOTIFICATION_ID = 77;
	private static Thread theThread = null;
	
	
	private static NMEAParser mNMEAParser = null;
	private static int mUseGPSSource = AISPlotterGlobals.NO_GPS;
	
	private NotificationManager mNM;
	
	/*private static class ConnectionBroadcastReceiver extends BroadcastReceiver {   
		// Dieser teil bezieht sich auf die TCP-verbindung und hat wegen des Filters nichts mit
		// Bluetooth zu tun
	    @Override
	    public void onReceive(Context ctxt, Intent intent) { 
	      try {
	        if (test)Log.d(TAG, "Connection Broadcast onReceive(): entered...");
	        final String action = intent.getAction();
	        Logger.d(TAG,"Connection Broadcast action " + action );
	        // enthaelt den APN falls vorhanden, sonst null
	        String info = intent.getStringExtra(ConnectivityManager.EXTRA_EXTRA_INFO);
	 
	        Log.d(TAG,"info: " + info);
	        
	        NetworkInfo nInfo = intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
	        Log.d(TAG,"network info " + nInfo.toString());
	        
	        String nType = nInfo.getTypeName();
	        
	        String nSubType = nInfo.getSubtypeName();  // ist bei WIFI ""
	        String extraInfo = nInfo.getExtraInfo();
	        NetworkInfo.State nState = nInfo.getState();
	        Log.d(TAG,"Type " + nType + " Subtype " + nSubType + " ExtraInfo " + extraInfo);
	        Logger.d(TAG,"Type " + nType + " Subtype " + nSubType + " ExtraInfo " + extraInfo);
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
	          //if (sock != null && sock.isConnected())
	          if (sock != null ) {
	        	//  siehe oben kein BT !!!
	           // if(test)Log.w(TAG, "onReceive(): still connected!");
	           // sock.close();
	           // sock = null;
	          }
	        }
	        else {
	          
	          // Damit nicht zweimal ein Socket eingerichtet wird, pruefe ob sock == null
	          // onReceive wird auch beim Start des Services aufgerufen
	          if (nType.equals("WIFI")&& (nState == NetworkInfo.State.CONNECTED)&& (sock == null)){
	        	  //siehe oben, hat mit BT nichts zu tun!!!
	        	 // if (test)Log.d(TAG, "onReceive(): Verbindung besteht wieder. Starte Connection neu...");
	        	  
		         // if (netzwerkVerbindungHerstellen()) {
		         //   starteNMEAListener();
		            
		         // }  
	          }
	          
	        }
	       
	        
	      } 
	      catch (Exception e) {
	        if (test)Log.e(TAG, "Fehler: " + e.toString());
	      }
	    }
	  };
	  */
	  private static class BTConnectionBroadcastReceiver extends BroadcastReceiver {   
			
		    @Override
		    public void onReceive(Context ctxt, Intent intent) { 
		      try {
		        if (test)Log.d(TAG, "BTConnection Broadcast onReceive(): entered...");
		        final String action = intent.getAction();
		        if (test) Log.d(TAG,"action: " + action);
		        Logger.d(TAG,"Connection Broadcast action " + action );
		        // we get Bluetooth action messages with filters
		        // android.bluetooth.device.action.ACL_CONNECTED
		        // android.bluetooth.device.action.ACL_DISCONNECTED
		        // android.bluetooth.device.action.ACL_DISCONNECT_REQUESTED
		        
		        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
		       
		        String address = device.getAddress();
		        if (test) Log.d(TAG,"address of remote device" + address);
		        Logger.d(TAG,"address of remote device " +address);
		        
		        boolean isNotConnected = !(action.equals( "android.bluetooth.device.action.ACL_CONNECTED"));
		        // Zeige Warnung, solange keine Netzwerkverbindung besteht:
		        if (isNotConnected) {
		          if (test)Log.d(TAG, "onBroadcastReceive(): BT -Verbindung verloren...");
		          Logger.d(TAG,"BT-Connection lost " + address);
		          if ( mService != null) mService.showNotification(false);
		          //if (sock != null && sock.isConnected())
		          if (sock != null ) {
		        	
		           if(test)Log.w(TAG, "onReceive(): still connected!");
		           sock.close();
		           sock = null;
		          }
		        }
		        else {
		        	if (isRunning){
			        	Logger.d(TAG,"Service is running");
		        	}
		          // Damit nicht zweimal ein Socket eingerichtet wird, pruefe ob sock == null
		          // onReceive wird auch beim Start des Services aufgerufen
		          //if (nType.equals("WIFI")&& (nState == NetworkInfo.State.CONNECTED)&& (sock == null)){
		        	  //siehe oben, hat mit BT nichts zu tun!!!
		        	 // if (test)Log.d(TAG, "onReceive(): Verbindung besteht wieder. Starte Connection neu...");
		        	  
			         // if (netzwerkVerbindungHerstellen()) {
			         //   starteNMEAListener();
			            
			         // }  
		         // }
		          
		        }
		       
		        
		      } 
		      catch (Exception e) {
		        if (test)Log.e(TAG, "Fehler: " + e.toString());
		      }
		    }
		  };
		  
	  
	  
	
	public class NMEALocalBinder extends Binder {
	    public BTNetzwerkService getService() {
	    	if (test)Log.d(TAG, "BTNetzwerkservice->getService(): entered...");
	        return BTNetzwerkService.this;
	    }
	    public void setCallbackHandler(Handler callbackHandler) {
	    	if (test)Log.d(TAG, "BTNetzwerkservice->setCallbackHandler(): entered...");
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
		if (test)Log.d(TAG, "oncreate(): entered..."); 
		// Dieser Teil stammt aus amando services
		mBroadcastReceiver = new BTConnectionBroadcastReceiver();
		mIntentFilter.addAction("android.bluetooth.device.action.ACL_DISCONNECT_REQUESTED");
		mIntentFilter.addAction("android.bluetooth.device.action.ACL_DISCONNECTED");
		mIntentFilter.addAction("android.bluetooth.device.action");
	    registerReceiver(mBroadcastReceiver, mIntentFilter);
		
		    
	   
		
		// Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        
     // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            //finish();
            return;
        }
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        // we read from the prefs whether we should save the position data to the database
        String prev_save_position_data_key = getResources().getString(R.string.pref_save_position_data);
	    String savePosDataStr = prefs.getString(prev_save_position_data_key, "on");
	    boolean savePositionData = savePosDataStr.equalsIgnoreCase(("on"));
        boolean logPositionData = false;
        setGPSSourceFromPrefs();
	    mNMEAParser = new NMEAParser(this,mUseGPSSource ,savePositionData,logPositionData); 
	    
		// prev_BT_Server_address_key  = "btaddress" , das ist der key in den Resourcen definiert ist
		//String prev_BT_Server_address_key = getResources().getString(R.string.pref_btaddress);
        String prev_BT_Server_address_key = getResources().getString(R.string.pref_btaddress);
		boolean haskey =prefs.contains(prev_BT_Server_address_key);
		Map<String,?> aMap = prefs.getAll();
		String abtAddress = (String)aMap.get(prev_BT_Server_address_key);
		btServerAddress = prefs.getString(prev_BT_Server_address_key, DEFAULT_BT_SERVER_ADDRESS);
		Logger.d(TAG,"try to connect to BT Server " + btServerAddress);
		
		if (netzwerkVerbindungHerstellen()) { 
			  
		      starteNMEAListener();
		      showNotification(true);
		      Logger.d(TAG,"BT Verbindung hergestellt zu " + btServerAddress);
		}
		else {
			showNotification(false);
			Logger.d(TAG,"keine BT Verbindung hergestellt ");
		}
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
	
	private void showNotification(boolean isOn) {
		mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		if (isOn){
		  Notification aNotification = new Notification (R.drawable.iconais, "AIS runs!",
			        System.currentTimeMillis());
		  PendingIntent pendingIntent = 
			      PendingIntent.getActivity(this, 0, null, 0);
		  aNotification.setLatestEventInfo(this, 
			        "AIS NMEA Service",
			        "Im Hintergrund gestartet!", pendingIntent); 
			    
			    // Ab Android 2.0: 
		  startForeground(NOTIFICATION_ID,aNotification);
		}else {
		  Notification aNotification = new Notification (R.drawable.iconnoais, "no AIS data!",
			        System.currentTimeMillis());
		  PendingIntent pendingIntent = 
			      PendingIntent.getActivity(this, 0, null, 0);
		  aNotification.setLatestEventInfo(this, 
			        "AIS Service unterbrochen ",
			        "Bitte Service manuell stoppen", pendingIntent); 
			    
			    // Ab Android 2.0: 
		  mNM.notify(NOTIFICATION_ID,aNotification);
	}
		
	}
	
	private void cancelNotification(){
		mNM.cancel(NOTIFICATION_ID);
	}
	
	private static boolean netzwerkVerbindungHerstellen() {
	    if (test) Log.d(TAG, "netzwerkVerbindungHerstellen(): Netzwerkverbindung herstellen...");
	    
	    BluetoothSocket tmp = null;
	    BluetoothDevice aDevice = mBluetoothAdapter.getRemoteDevice(btServerAddress);
	    if (test) Log.d(TAG, " zu Adresse " + btServerAddress);
        // Get a BluetoothSocket for a connection with the
        // given BluetoothDevice
        try {
            tmp = aDevice.createRfcommSocketToServiceRecord(MY_UUID);
        } catch (IOException e) {
            Log.e(TAG, "create() failed", e);
        }
	    try {
	      // 
	      sock = tmp;      
	      sock.connect();
	      is   = new BufferedReader(new InputStreamReader(sock.getInputStream()));
	      pw   = new PrintWriter(sock.getOutputStream(), true);      
	    } 
	    catch (UnknownHostException e) {
	      if (test)Log.e(TAG, "netzwerkVerbindungHerstellen unknownHost : " + e.toString());
	      sock = null;
	      return false;
	    } 
	    catch (IOException e) {
	      if (test)Log.e(TAG, "netzwerkVerbindungHerstellen IO Exception: " + e.toString());
	      sock = null;
	      return false;
	    } 
	    catch (Exception e) {
	    	if (test) Log.e(TAG, "netzwerkVerbindungHerstellen Exception: " + e.toString());
	    	sock = null;
	    	return false;
	    }
	    if (test) Log.d(TAG, "netzwerkVerbindungHerstellen erfolgreich ");
	    return true;
	  }
	
	public static void processMessage (String aMessage){
		if (mNMEAParser != null )mNMEAParser.processMessage(aMessage);
		//Logger.d(TAG,"Message " + aMessage);
		//if (test) Log.d(TAG,aMessage);
	}
	
	
	
	
	private static void starteNMEAListener() {
	    if (test) Log.d(TAG,"starteNMEAListener()->entered...");
	    theThread = new Thread() {
	      public void run() {        
	        try {
	           if (!isRunning ){
	                theThread = null;
	                if (test) Log.d(TAG,"isRunning = false");
	           }else {
		          String line;
		                
		          // blockiert, bis eine neue Zeile ankommt:
		         
		          while (isRunning && sock != null && //!sock.isClosed() && 
		              (line = is.readLine()) != null) {
		            if (test)Log.d(TAG, "run()->Vom Server uebermittelt: " + line);
		            processMessage(line);       
		            } 
		          if (test)Log.d(TAG,"Leseschleife beendet");
	           }
	        } 
	        catch (IOException ex) {
	          if (test) Log.d(TAG,"starteNMEAListener->run()->Fehler: " + ex.toString());
	          
	          return;
	        }
	        finally {
	          if (sock == null) {
	        	  if (test)Log.d(TAG, "starte NMEAListerner->run() finally sock = null");
	          }
	          /*else if (sock.isClosed()){
	        	  if (test) Log.d(TAG, "starte NMEAListerner->run() finally sock was closed");
	          }*/
	          isRunning = false;
	          theThread = null;
	          if (test) Log.d(TAG,"NMEAListerner->run() finally done, Thread terminated");
	          }  
	        }
	    };  // new Thread
	    
	    
	    if (sock != null) {
	    	isRunning = true;
	    	theThread.start();
	    	}
	    if (test)Log.d(TAG,"starteNMEAListener()->gestartet...");
	  }
	
	public void stopNMEAListener(){
		if (test)Log.d(TAG, "stopNMEAListerner service begin ");
		isRunning = false;
		if (theThread != null) theThread.interrupt();
		theThread = null;
		if (test) Log.d(TAG, "stopNMEAListerner service finished");
    
    }
	@Override
	  public void onDestroy() {
        
		 if (test)Log.d(TAG, "onDestroy(): LocalService beenden...");
		try {
		 if (sock != null) {
			  sock.close();
			  if (test) Log.d(TAG,"Socket closed");
		 }
		} catch (IOException ex) {
	          if (test) Log.d(TAG,"BT Netzwerk ->IO-Fehler: " + ex.toString());
	           return;
	    }
	    if (test) Log.d(TAG, "onDestroy(): LocalService erfolgreich beendet..."); 
	    unregisterReceiver(mBroadcastReceiver);
	    cancelNotification();
	    if (mNMEAParser != null) mNMEAParser.destroy();
	    Logger.d(TAG,"BT-Service terminated");
	  }
	
	public boolean isServiceRunning () {
		return isRunning;
	}
	
	public TargetList getAISTargetList(){
	  if (mNMEAParser != null) {
		  return mNMEAParser.getAISTargetList();
	  }
	  else {
		  return null;
		  }
	}
	

}
