package com.klein.activities;



import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;



import com.klein.aistcpopenmapplotter051.R;

import com.klein.logutils.Logger;
import com.klein.service.TrackDbAdapter;

public class AISListActivity extends ListActivity {
	
    private static final String TAG = "AISList";
    private boolean test = false;
    
    private TrackDbAdapter mDbAdapter;
    private static final int TARGETEDIT_ACTIVITY_SHOW=1;
    
    public static final int SHOW_TARGET_ON_MAP = 2;  // possible result code from Target Edit
    
    private static final int DELETE_ID = Menu.FIRST + 1;
    private static final int SHOW_ID = DELETE_ID + 1;
    
    
    
	public AISListActivity() {
		// TODO Auto-generated constructor stub
	}
	
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
        // Get all of the rows from the database and create the item list
        Cursor mTargetsCursor = mDbAdapter.fetchAllTargetsMMSI_Name_SOG();
        startManagingCursor(mTargetsCursor);

        // Create an array to specify the fields we want to display in the list (only MMSI and Name)
        String[] from = new String[]{TrackDbAdapter.KEY_MMSI,TrackDbAdapter.KEY_NAME,TrackDbAdapter.KEY_SOG};
        
        // and an array of the fields we want to bind those fields to (in this case just text1)
        int[] to = new int[]{R.id.mmsi,R.id.name,R.id.sog};  // this comes from targets_row.xml

        // Now create a simple cursor adapter and set it to display
        SimpleCursorAdapter targets = 
            new SimpleCursorAdapter(this, R.layout.target_row, mTargetsCursor, from, to);
        setListAdapter(targets);
    }
    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        Log.v(TAG,"onListItemClick Position " + position + "  id " + id);
        Intent i = new Intent(this, TargetEditActivity.class);
        i.putExtra(TrackDbAdapter.KEY_ROWID, id);
        startActivityForResult(i, TARGETEDIT_ACTIVITY_SHOW);
    }
    
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
		  // should be executed when returning from TargetEdit
	        if(test) Log.d(TAG, "onActivityResult " + resultCode);
	        if (requestCode != TARGETEDIT_ACTIVITY_SHOW) return;
	        // we come from TargetEdit
	        switch (resultCode) {
	          case RESULT_OK:
	        	  break;
	          case SHOW_TARGET_ON_MAP :
	        	  // we transfer the info in the data intent to the 
	        	  // calling AISMapPlotter Activitiy
	        	  Bundle extras = data.getExtras();
	        	  String aMMSIStr = "";
		          aMMSIStr = extras != null ? extras.getString("MMSI")
		                                    : null;
		          Logger.d(TAG,"Show Target on map requested " + aMMSIStr);
		          setResult(AISListActivity.SHOW_TARGET_ON_MAP,data);
	        	  finish();
	        	  break;
	        }
    }
    
   /* unused
    private void populateFields(Long id) {
        if (id != null) {
            Cursor target = mDbAdapter.fetchTarget(id);
            startManagingCursor(target);
            String aMMSI = (target.getString(
    	            target.getColumnIndexOrThrow(AISDbAdapter.KEY_MMSI)));
            String name = (target.getString(
                    target.getColumnIndexOrThrow(AISDbAdapter.KEY_NAME)));
            Log.v(TAG,"populateFields  " + id + "  MMSI " + aMMSI + " " + name);
        }
    }
    */
    
    private void showTargetOnMap(long rowId) {
		
	   Cursor cursorTarget = mDbAdapter.fetchTarget(rowId);
       startManagingCursor(cursorTarget);
       String aMMSIStr =cursorTarget.getString(cursorTarget.getColumnIndexOrThrow(TrackDbAdapter.KEY_MMSI));
       // we put the selected MMSI in the extras of the intent 
   	   // and send it back to the calling AISListActivity
   	   Intent aIntent= getIntent().putExtra("MMSI", aMMSIStr); // key, value
   	   setResult(AISListActivity.SHOW_TARGET_ON_MAP,aIntent);
	   finish();
		 
    }
    
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
    	
    
        if  (keyCode == KeyEvent.KEYCODE_BACK)
        {
        	setResult(RESULT_OK);
        	this.finish();
            return  true;
        } 
        
        return super.onKeyUp(keyCode, event);
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        //menu.add(0, DELETE_ID, 0, R.string.menu_delete);
        menu.add(0, SHOW_ID, 0, R.string.menu_show);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
    	AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        switch(item.getItemId()) {
            /*case DELETE_ID:
                mDbAdapter.deleteTarget(info.id);
                fillData();
                return true;*/
            case SHOW_ID:
            	showTargetOnMap(info.id);
                return true;
        }
        return super.onContextItemSelected(item);
    }
    
    @Override
    public void onDestroy() {
    	mDbAdapter.close();
    	super.onDestroy();
    }

}
