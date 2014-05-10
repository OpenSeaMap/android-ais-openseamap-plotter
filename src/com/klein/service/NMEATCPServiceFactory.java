package com.klein.service;

import android.content.Context;
import android.util.Log;

import com.klein.logutils.Logger;


public class NMEATCPServiceFactory {
	private static boolean test = false;
	private static final String TAG ="NMEATCPServiceFactory";
	private static NMEATCPVerwaltung nmeaTCPVerwaltung = null;

	public static NMEATCPVerwaltung getNMEAVerwaltung(Context context) {
		if (test) {
			String aMsg1 = "NMEATCPServiceFactory--> getNMEAVerwaltung ";
			Logger.d(TAG,aMsg1);
			Log.d(TAG,aMsg1);
		}
		
		if (nmeaTCPVerwaltung == null) {
			if (test) {
				String aMsg2 = "NMEATCPServiceFactory--> getNMEAVerwaltung try to start Impl ";
				Logger.d(TAG,aMsg2);
				Log.d(TAG,aMsg2);
			}
      	    nmeaTCPVerwaltung = new NMEATCPVerwaltungImpl(context);  
      	    
		}else {
			if (test) {
				String aMsg2 = "NMEATCPServiceFactory--> nmeaVerwaltung exists ";
				Logger.d(TAG,aMsg2);
				Log.d(TAG,aMsg2);
				Log.d(TAG,nmeaTCPVerwaltung.toString());
			}
		}
		return nmeaTCPVerwaltung;
	}
	
	public static void stopService(){
		if (test) {
			String aMsg1 = "NMEATCPServiceFactory--> stopService ";
			Logger.d(TAG,aMsg1);
			Log.d(TAG,aMsg1);
		}
		
		if (!(nmeaTCPVerwaltung == null))  {
			if (test) {
				String aMsg2 = "NMEATCPServiceFactory--> stop service: try to stop service ";
				Logger.d(TAG,aMsg2);
				Log.d(TAG,aMsg2);
			}
			
			nmeaTCPVerwaltung.stopService();
			nmeaTCPVerwaltung = null;
			if (test) {
				String aMsg3 = "NMEATCPServiceFactory--> stop service: set nmeaTCPVerwaltung = null ";
				Logger.d(TAG,aMsg3);
				Log.d(TAG,aMsg3);
			}
			
		} else {
			if (test) {
				String aMsg3 = "NMEATCPServiceFactory--> stop service: cannot set nmeaTCPVerwaltung = null ";
				Logger.d(TAG,aMsg3);
				Log.d(TAG,aMsg3);
			}
			
		}
	}
}
