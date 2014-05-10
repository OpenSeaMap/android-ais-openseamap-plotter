package com.klein.service;







import com.klein.logutils.Logger;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;


public class NMEATCPVerwaltungImpl implements NMEATCPVerwaltung{
	
	//private static final String TAG = NMEATCPVerwaltungImpl.class.getSimpleName();
	private static final String TAG = "NMEATCPVerwaltungImpl";
	private TCPNetzwerkService.NMEALocalBinder localServiceBinder;
	private TCPNetzwerkService localService;
	
	  private Context context;
	  
	  protected NMEATCPVerwaltungImpl() { }
	  
	  public NMEATCPVerwaltungImpl(Context context) { 
		 String aMsg = " Starting NMEAVerwImpl";
		 Log.d(TAG,aMsg);
		 Logger.d(TAG,aMsg);
	    Intent intent = new Intent(context, TCPNetzwerkService.class);
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
	      localServiceBinder = (TCPNetzwerkService.NMEALocalBinder)binder; 
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
    public void stopService(){
    	try {
    	   context.unbindService(localServiceConnection);
    	} catch (IllegalArgumentException e){
    		Logger.d("Cound stop local service" + e.toString());
    	}
    }
	    
	private void disconnectNetworkService(){
		if (localService != null){
			//localService.stopNMEAListener();
		}
		
	}
	
	public void makeConnection() {
		if (localService != null) {
			localService.makeConnection();
		}
	}
	
	
}
