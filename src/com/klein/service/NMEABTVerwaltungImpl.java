package com.klein.service;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

public class NMEABTVerwaltungImpl implements NMEABTVerwaltung{
	
	private static final String TAG = "NMEABTVerwaltungImpl";
	private BTNetzwerkService.NMEALocalBinder localServiceBinder;
	private BTNetzwerkService localService;
	
	  private Context context;
	  
	  protected NMEABTVerwaltungImpl() { }
	  
	  public NMEABTVerwaltungImpl(Context context) {        
	    Intent intent = new Intent(context, BTNetzwerkService.class);
	    try {
	    	//context.startService(intent);
		if (context.bindService(intent, localServiceConnection, Context.BIND_AUTO_CREATE))
			Log.d(TAG,"Service gestartet");
		else Log.d(TAG,"Service nicht gestartet");
	    }  catch (SecurityException e) {
	    	Log.e(TAG, "NMEAVerwaltungImpl->intent: Fehler: " + e.toString());
	    }
		this.context = context;
	  }
	  
	  /**
	   * Baut eine Verbindung zum lokalen Service auf. Der Service laeuft im 
	   * gleichen Prozess wie diese Activity. Daher wird er automatisch beendet,
	   * wenn der Prozess der Activity beendet wird.
	   */
	  private ServiceConnection localServiceConnection = new ServiceConnection() {    
	    /**
	     * Wird aufgerufen, sobald die Verbindung zum lokalen Service steht.
	     */
	    public void onServiceConnected(ComponentName className, IBinder binder) {        
	      Log.d(TAG, "onServiceConnected(): entered..."); 
	      localServiceBinder = (BTNetzwerkService.NMEALocalBinder)binder; 
	      localService = localServiceBinder.getService();
	    }

	    /**
	     * Wird aufgerufen, sobald die Verbindung zum Service unterbrochen wird. 
	     * Dies passiert nur, wenn der Prozess, er den Service gestartet hat, stirbt.
	     * Da dies ein lokaler Service ist, läuft er im selben Prozess wie diese Activity.
	     * Daher kann die Methode niemals aufgrufen werden und muss nicht implementiert
	     * werden.
	     */
	    public void onServiceDisconnected(ComponentName className) {     
	      Log.d(TAG, "onServiceDisconnected(): entered..."); 
	      // bei einem lokalen Service unerreichbar...
	    }
	  };
	  
    public TargetList getAISTargetList(){
    	if (localService != null) {
    		return localService.getAISTargetList();
    		
    	}
    	return  null;
    }
    
    public boolean isServiceRunning() {
    	if (localService != null) {
    		return localService.isServiceRunning();
    		
    	}
    	return  false;
    }
	
	public void disconnectNetworkService(){
		localService.stopNMEAListener();
		context.unbindService(localServiceConnection);
		localService.stopSelf();
	}
	
	

	
	
}

