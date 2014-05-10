package com.klein.aistcpopenmapplotter051;


import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

public class GpsLocationServiceLocal extends Service {

	  private static final String TAG = "GpsLocationServiceLocal";
	  private static final boolean test = false; 
	  Handler uiServiceCallbackHandler;
	  
	  private final IBinder gpsBinder = new GpsLocalBinder();
	  private LocationManager locationManager;
	  private LocationListener locationListener;
	  private Location mLastLocation = null;
	  private boolean isFirstLocation = true;
	  private int countEqualLocation;
	  
	  private class MyLocationListener implements LocationListener  {
		  
	    public void onLocationChanged(Location location) {
	      if (test) Log.d(TAG, "MyLocationListener->onLocationChanged(): entered...");
	      if (isFirstLocation) {
	    	  mLastLocation = location;
	    	  isFirstLocation = false;
	      }
	      countEqualLocation++; 
	      if (location != null && uiServiceCallbackHandler != null) {                          
	        Message message = new Message();
	        message.obj = location;
	        Bundle bundle = new Bundle();
	        bundle.putParcelable("location", location);
	        message.setData(bundle);
	        if (location.distanceTo(mLastLocation) > 3.0 || countEqualLocation >= 30) {
	        	// do only send a location message 
	        	 // if the distance to the old location is greater than 5 m or
	        	 // if more than 30 equal Locations ( = 30 sec) are counted
	        	mLastLocation = location;
	        	countEqualLocation = 0;
	        	uiServiceCallbackHandler.sendMessage(message);     
	        }        
	      }
	    }    
	    public void onProviderDisabled(String provider) { }

	    public void onProviderEnabled(String provider) { }

	    public void onStatusChanged(String provider, int status,
	        Bundle extras) { }
	  };

	  /**
	   * Klassen, die den Binder fuer den Zugriff von Clients auf diesen 
	   * Service definiert. Da dieser Service immer im gleichen Prozess
	   * wie der Aufrufer laeuft, ist kein IPC notwendig.
	   */
	  public class GpsLocalBinder extends Binder {
	    public GpsLocationServiceLocal getService() {
	    	if (test) Log.d(TAG, "GPSLocalBinder->getService(): entered...");
	        return GpsLocationServiceLocal.this;
	    }
	    public void setCallbackHandler(Handler callbackHandler) {
	    	if (test) Log.d(TAG, "GPSLocalBinder->setCallbackHandler(): entered...");
	      uiServiceCallbackHandler = callbackHandler;
	    }
	    
	    public Location getGpsData() {
	    	Log.d(TAG, "GPSLocalBinder->getGPSData(): entered...");
	      Location result = null;
	      if (locationManager != null) {
	        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
	        if (test)Log.d(TAG, "getGpsData(): Providers found=" + locationManager.getAllProviders());
	        if (test) Log.d(TAG, "getGpsData(): GPS Enabled=" + locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER));
	        if (test) Log.d(TAG, "getGpsData(): Last Known Location=" + locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)); 
	        Log.d(TAG,"gps data requested");
	        result = location;
	      }
	      return result;
	    }
	  }
	  
	  public void stopRequestLocationUpdates() {
		  if (locationManager != null ){
		    locationManager.removeUpdates(locationListener); 
		    locationListener = null;
		  }
	  }

	  @Override
	  public void onCreate() {
	    if (test) Log.d(TAG, "onCreate(): LocalService starten...");    
	    // provide a new location every 10000 ms = 10 sec  should be greater the 60000 = 1 min    
	    locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);  
	    locationListener = new MyLocationListener();    
	    locationManager.requestLocationUpdates(
	        LocationManager.GPS_PROVIDER, 
	       1000, 
	        0, 
	        locationListener);  
	    isFirstLocation = true;
	    countEqualLocation = 0;
	  }

	  @Override
	  public void onDestroy() {  
	    if (test) Log.d(TAG, "onDestroy(): LocalService beenden..."); 
	    if (locationManager != null && locationListener != null){
	         locationManager.removeUpdates(locationListener);
	         locationListener = null;
	    }
	    //this.stopSelf();
	  }
	  
	  @Override
	  public IBinder onBind(Intent intent) {
	    if (test) Log.d(TAG, "onBind(): entered...");    
	    return gpsBinder;        
	  }

	}