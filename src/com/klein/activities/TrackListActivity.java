package com.klein.activities;

import java.util.ArrayList;

import android.app.ListActivity;
import android.content.Intent;

import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import android.widget.AdapterView.AdapterContextMenuInfo;



import com.klein.aistcpopenmapplotter051.R;

import com.klein.service.TrackDbAdapter;

public class TrackListActivity extends ListActivity {
	 public static final int SHOW_TRACK = 2;
	 private static final String TAG = "TrackList";
	 private static final int TRACK_ACTIVITY_SHOW=1;
	 private boolean test = true;
	    
	 private TrackDbAdapter mDbAdapter;
	 private ArrayAdapter<String> mAdapter;
	 private static final int DELETE_ID = Menu.FIRST + 1;
	 private static final int SAVE_ID = DELETE_ID + 1;
	 
	 /** Called when the activity is first created. */
	    @Override
	    public void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
	        setContentView(R.layout.target_list);
	        mDbAdapter = new TrackDbAdapter(this);
	        mDbAdapter.open();
	        fillData();
	        registerForContextMenu(getListView());
	    }
	    
	    private void fillData() {
	        // Get all of the tracknames from the database and create the item list
	    	ArrayList<String> aTrackList = mDbAdapter.listAllShipTrackTablesInDatabase();
	    	mAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, aTrackList);
	        
	        setListAdapter(mAdapter);
	        
	    }
	    
	    @Override
	    protected void onListItemClick(ListView l, View v, int position, long id) {
	        super.onListItemClick(l, v, position, id);
	        Log.v(TAG,"onListItemClick Position " + position + "  id " + id);
	        Intent i = new Intent(this, TrackTextActivity.class);
	        String aTrackName = mAdapter.getItem(position);
	        if (aTrackName.contains("shiptrack")){
	        	int count = aTrackName.indexOf("k");
	        	aTrackName = aTrackName.substring(count+1, aTrackName.length());
	        	i.putExtra(TrackDbAdapter.KEY_MMSI, aTrackName);
		        startActivityForResult(i, TRACK_ACTIVITY_SHOW);
	        }  
	    }
	    
	    public void onActivityResult(int requestCode, int resultCode, Intent data) {
			  // should be executed when returning from TargetEdit
		        if(test) Log.d(TAG, "onActivityResult " + resultCode);
		        if (requestCode != TRACK_ACTIVITY_SHOW) return;
		        // we come from TrackTextShow
		        switch (resultCode) {
		          case RESULT_OK:
		        	  break;
		        }
	    }
	    
	    @Override
	    public void onCreateContextMenu(ContextMenu menu, View v,
	            ContextMenuInfo menuInfo) {
	        super.onCreateContextMenu(menu, v, menuInfo);
	        menu.add(0, DELETE_ID, 0, R.string.track_menu_delete);
	       menu.add(0, SAVE_ID, 0, R.string.track_menu_save);
	    }

	    @Override
	    public boolean onContextItemSelected(MenuItem item) {
	    	AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
	    	String aTrackName;
	        switch(item.getItemId()) {
	            case DELETE_ID:
	            	aTrackName = mAdapter.getItem(info.position);
	            	if (aTrackName.contains("shiptrack")){
	    	        	int count = aTrackName.indexOf("k");
	    	        	aTrackName = aTrackName.substring(count+1, aTrackName.length());
	    	        	mDbAdapter.deleteShipTrackTable(aTrackName);
	            	}
	               // mDbAdapter.deleteTarget(info.id);
	               
	                return true;
	            case SAVE_ID:
	            	aTrackName = mAdapter.getItem(info.position);
	            	if (aTrackName.contains("shiptrack")){
	    	        	int count = aTrackName.indexOf("k");
	    	        	aTrackName = aTrackName.substring(count+1, aTrackName.length());
	    	        	mDbAdapter.WriteTrackDataToExternalStorage(aTrackName);
	            	}
	            	
	           /* case SHOW_ID:
	            	showTargetOnMap(info.id);
	                return true;*/
	        }
	        return super.onContextItemSelected(item);
	    }
	    
	    public void onDestroy() {
	    	mDbAdapter.close();
	     	super.onDestroy();	
	    }
}
