package com.klein.service;

import android.content.Context;

public class NMEABTServiceFactory {
	private static NMEABTVerwaltung nmeaBTVerwaltung;

	public static NMEABTVerwaltung getNMEAVerwaltung(Context context) {
		if (nmeaBTVerwaltung == null) {
      	  nmeaBTVerwaltung = new NMEABTVerwaltungImpl(context);      	  
		}
		return nmeaBTVerwaltung;
	}
	
	public static void stopService(){
		if (!(nmeaBTVerwaltung == null))  {
			nmeaBTVerwaltung.disconnectNetworkService();
			nmeaBTVerwaltung = null;
		}
	}
}
