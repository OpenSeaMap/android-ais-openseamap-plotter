package com.klein.activities;


import java.util.Map;

import com.klein.aistcpopenmapplotter051.R;

import com.klein.logutils.Logger;





import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;



public class EinstellungenBearbeitenActivity extends PreferenceActivity {
	public static final String TAG = EinstellungenBearbeitenActivity.class.getSimpleName();

	
	
	/**
	 *  V.Klein 11_09_20
	 *  important:
	 *  the prefs, which we will work with, must be of string type
	 *  other prefs, which are manipulated throu prefs.edit... can not be set or read
	 *  and will crash the app
	 */
	
	// Menueoptionen
	
	private static final int EINSTELLUNG_BEARBEITEN_ID = Menu.FIRST;
	private static final int ZURUECK_ID = Menu.FIRST + 1;
	private static final int EINSTELLUNGEN_BEENDEN_ID = Menu.FIRST + 2;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		
		// this code is only for testing the type of the associated values to the keys
		// in the prefs
		 SharedPreferences prefs;
		 prefs = PreferenceManager.getDefaultSharedPreferences(this);
		 /*prefs.edit()
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
		 
		String s =  TAG;
		// in xml.einstellungen werden die keys für die preferences definiert
		// dabei werden die keys endgueltig über strings definiert
		// 
        this.addPreferencesFromResource(R.xml.einstellungen);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		/*menu.add(0, EINSTELLUNG_BEARBEITEN_ID, 0,
				R.string.app_einstellungenBearbeiten);
		menu.add(0, ZURUECK_ID, 0, R.string.app_zurueck);*/
		menu.add(0, EINSTELLUNGEN_BEENDEN_ID, 0, R.string.bearbeiten_Beenden);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case EINSTELLUNGEN_BEENDEN_ID:
				finish();
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
	    super.onSaveInstanceState(outState);
	    SharedPreferences settings = getSharedPreferences(TAG, 0);
	    SharedPreferences.Editor editor = settings.edit();
	
	    editor.commit();
	}	
}