package com.klein.activities;




import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import com.klein.aistcpopenmapplotter051.R;
import com.klein.commons.PositionTools;
import com.klein.service.TrackDbAdapter;

public class TargetEditActivity extends Activity {
	
	 private static final String TAG = "TargetEditActivity";
	
	 private TrackDbAdapter mDbAdapter;
	 private Long mRowId;
	 
	 private TextView mMMSI;
	 private TextView mName;
	 private TextView mLAT;
	 private TextView mLON;
	 private TextView mSOG;
	 private TextView mCOG;
	 private TextView mUTC;
	 private TextView mNavStatus;
	 private TextView mShipType;
	 private CheckBox mHasTrack;
	 
	 
	 
	 
	 
	 @Override
	    protected void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
	        mDbAdapter = new TrackDbAdapter(this);
	        mDbAdapter.open();
	        setContentView(R.layout.target_edit);
	        mMMSI = (TextView) findViewById(R.id.mmsi);
	        mName = (TextView) findViewById(R.id.name);
	        mLAT = (TextView) findViewById(R.id.lat);
	        mLON = (TextView) findViewById(R.id.lon);
	        mSOG = (TextView) findViewById(R.id.sog);
	        mCOG = (TextView) findViewById(R.id.cog);
	        mUTC = (TextView) findViewById(R.id.utc);
	        mNavStatus = (TextView) findViewById(R.id.navstatus);
	        mShipType = (TextView) findViewById(R.id.shiptype);
	        mHasTrack =(CheckBox)findViewById(R.id.hastrack);
	        Button confirmButton = (Button) findViewById(R.id.confirm);
	        Button showOnMapButton = (Button) findViewById(R.id.showOnMap);
	        mRowId = (savedInstanceState == null) ? null :
	            (Long) savedInstanceState.getSerializable(TrackDbAdapter.KEY_ROWID);
	        if (mRowId == null) {
	            Bundle extras = getIntent().getExtras();
	            mRowId = extras != null ? extras.getLong(TrackDbAdapter.KEY_ROWID)
	                                    : null;
	        }
	        populateFields();
	        confirmButton.setOnClickListener(new View.OnClickListener() {

	            public void onClick(View view) {
	            	setResult(RESULT_OK);
	                finish();
	            }

	        });
	        
	        showOnMapButton.setOnClickListener(new View.OnClickListener() {

	            public void onClick(View view) {
	            	String aMMSIStr = (String) mMMSI.getText(); // which Target to handle
	            	// we put the selected MMSI in the extras of the intent 
	            	// and send it back to the calling AISListActivity
	            	Intent aIntent= getIntent().putExtra("MMSI", aMMSIStr); // key, value
	            	setResult(AISListActivity.SHOW_TARGET_ON_MAP,aIntent);
	                finish();
	            }

	        });
	       
	 }
	 
	    

	 private void populateFields() {
	        if (mRowId != null) {
	            Cursor target = mDbAdapter.fetchTarget(mRowId);
	            startManagingCursor(target);
	            String theMMSI= target.getString(
	    	            target.getColumnIndexOrThrow(TrackDbAdapter.KEY_MMSI));
	            mMMSI.setText(theMMSI);
	            mName.setText(target.getString(
	                    target.getColumnIndexOrThrow(TrackDbAdapter.KEY_NAME)));
	            mLAT.setText(target.getString(
	                    target.getColumnIndexOrThrow(TrackDbAdapter.KEY_LAT)));
	            mLON.setText(target.getString(
	                    target.getColumnIndexOrThrow(TrackDbAdapter.KEY_LON)));
	            mSOG.setText(target.getString(
	                    target.getColumnIndexOrThrow(TrackDbAdapter.KEY_SOG)));
	            mCOG.setText(target.getString(
	                    target.getColumnIndexOrThrow(TrackDbAdapter.KEY_COG)));
	            long aUTC= target.getLong(
	                    target.getColumnIndexOrThrow(TrackDbAdapter.KEY_UTC));
	            
	            mUTC.setText(PositionTools.getTimeString(aUTC));
	            int aNavStatus = target.getInt(
	                    target.getColumnIndexOrThrow(TrackDbAdapter.KEY_NAVSTATUS));
	            if (theMMSI.startsWith("970")) {
	            	switch (aNavStatus){
	            	case 14: mNavStatus.setText("AIS-SART-ALERT"); break;
	            	case 15: mNavStatus.setText("AIS-SART-TEST"); break;
	            	default: mNavStatus.setText(PositionTools.getNavStatusString(aNavStatus));
	            	}
	            } else {
	                mNavStatus.setText(PositionTools.getNavStatusString(aNavStatus));
	            }
	            int aShipType = target.getInt(
	                    target.getColumnIndexOrThrow(TrackDbAdapter.KEY_SHIPTYPE));
	            mShipType.setText(PositionTools.getShipTypeStringFromIndex(aShipType));
	            String hasTrackStr = target.getString(target.getColumnIndexOrThrow(TrackDbAdapter.KEY_HAS_TRACK));
	            mHasTrack.setChecked(hasTrackStr.equals("true"));
	            int aDisplayStatus = target.getInt(
	            		             target.getColumnIndexOrThrow(TrackDbAdapter.KEY_DISPLAYSTATUS));
	            Log.d(TAG,"DisplayStatus " + mName.getText() + " " + aDisplayStatus);
	           // mHasTrack.setEnabled(false);
	        }
	    }
	 
	@Override
	protected void onDestroy() {
		mDbAdapter.close();
		super.onDestroy();
	}
}
