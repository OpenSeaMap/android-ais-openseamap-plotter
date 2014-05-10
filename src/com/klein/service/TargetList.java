package com.klein.service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import org.mapsforge.core.GeoPoint;

import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.klein.commons.AISPlotterGlobals;
import com.klein.commons.PositionTools;
import com.klein.logutils.Logger;




public class TargetList {
	
	private ArrayList<AISTarget> mTargetList = null;
	private static String TAG ="Targetlist";
	//private AISPlotter mActivity; 
	private boolean test = false;
	private Context ctx;
	private TrackDbAdapter mDbAdapter;
	
	/*public  TargetList(AISPlotter aActivity) {
		if (test) Log.v(TAG,"Targetlist create");
		mActivity = aActivity;
		mTargetList = new ArrayList<AISTarget>();
	}*/
	
	public  TargetList(Context ctx) {
		if (test) Log.v(TAG,"Targetlist create");
		this.ctx = ctx;
		mTargetList = new ArrayList<AISTarget>();
		mDbAdapter = new TrackDbAdapter(ctx);
		mDbAdapter.open();
	}
	
	public void destroy(){
		mDbAdapter.close();
	}
	
	public int getSize() {
		return mTargetList.size();
	}
	
	public void add(AISTarget aTarget ) {
		mTargetList.add(aTarget);
	}
	
	
	public void removeTargetByNr(int i) {
		mTargetList.remove(i);
	}
	
	
	public AISTarget findTargetByMMSI (long aMMSI){
		AISTarget aTarget = null;
		boolean found = false;
		if (mTargetList.isEmpty()) return null;
		int i = 0; 
		while (( i < mTargetList.size() &&( !found))){
			aTarget = mTargetList.get(i);
			if (aTarget.getMMSI()== aMMSI) {
				found = true;
				return aTarget;
			}
		    i++;
		}
		return null;		
	}
	
	private int abs (int a ) {
		if (a >= 0) return a;
		else return -a;
	}
	
	public AISTarget findTargetByPosition (GeoPoint p, int findRadius){
		AISTarget aTarget = null;
		boolean found = false;
		if (mTargetList.isEmpty()) return null;
		int i = 0; 
		while (( i < mTargetList.size() &&( !found))){
			aTarget = mTargetList.get(i);
			int aLAT = (int)(aTarget.getLAT()*1E6); // in Mikrograd
			int aLON = (int)(aTarget.getLON()*1E6);
			if ((abs(aLAT - p.latitudeE6) < findRadius) && 
					((abs (aLON - p.longitudeE6) < findRadius))) {
				found = true;
				return aTarget;
			}
		    i++;
		}
		return null;		
	}
	
	public AISTarget getTargetByNr(int i) {
		if (mTargetList.isEmpty()) return null;
		if (mTargetList.size() > 0 )return mTargetList.get(i);
		return null;
	}
	
	public void deleteOldTargets(Date pDeleteDate){
		AISTarget aTarget = null;
		if (mTargetList.isEmpty()) return;
		int i = getSize()-1; 
		while ( i >= 0 ){
			aTarget = getTargetByNr(i);
			Date dateOfTarget = new Date(aTarget.getTimeOfLastStaticUpdate());
			if (dateOfTarget.before(pDeleteDate) ) {
				// set displayStatus to DISPLAYSTATUS_0
				// mark all the old one's
				aTarget.setStatusToDisplay(AISPlotterGlobals.DISPLAYSTATUS_0);
			}
			i--;
		}
		// now remove the marked old one's
		// we do this, cause a target may be referenced by the DisplayObjectList
	    i = getSize()-1; 
	    int numberOfDeletedTargets = 0;
		while ( i >= 0 ){
			aTarget = getTargetByNr(i);
			
			if (aTarget.getStatusToDisplay()==AISPlotterGlobals.DISPLAYSTATUS_0)  {
				// delete the target from database and list
				// send a event to the GUI
				Long id = aTarget.getId();
				String aName = aTarget.getShipname();
				String aMMSI = aTarget.getMMSIString();
				Logger.d(TAG,"deleting " + aName + " " + aMMSI);
				sendDeleteTargetEvent(aMMSI);
				removeTargetByNr(i);
				mDbAdapter.deleteTarget(id);
				boolean hasTrack = aTarget.getHasTrack();
				if (hasTrack){
					// delete track from database
					mDbAdapter.deleteShipTrackTable(aMMSI);
				}
				numberOfDeletedTargets++;
			}
			i--;
		}
		Logger.d(TAG,numberOfDeletedTargets + " targets deleted");
		
			String aMessage = mTargetList.size() + " Targets in list " + numberOfDeletedTargets + " targets deleted";
			Logger.d(TAG,aMessage);
			Toast toast = Toast.makeText(ctx, aMessage, Toast.LENGTH_LONG);
			toast.show();
		
	}
	
	public void sendDeleteTargetEvent(String aMMSIStr){
	    	// theTarget is too old, so we delete the symbol
	    	// so we have to inform the GUI
		    if (test) Log.d(TAG, "sendDeleteTargetEvent " + aMMSIStr);
	    	Intent aDeleteTargetIntent = new Intent (AISPlotterGlobals.ACTION_DELETE_TARGET);
	    	aDeleteTargetIntent.putExtra(AISPlotterGlobals.GUIUpdateEvent_MMSI_KEY,aMMSIStr);
	    	ctx.sendBroadcast(aDeleteTargetIntent);
	}
	
	
	public void sendGUIUpdateEvent(AISTarget pTarget){
    	// we have a new position or the status changed
    	// so we have to inform the GUI
    	Intent aGUIUpdateIntent = new Intent (AISPlotterGlobals.ACTION_UPDATEGUI);
    	aGUIUpdateIntent.putExtra(AISPlotterGlobals.GUIUpdateEvent_MMSI_KEY,pTarget.getMMSIString());
    	aGUIUpdateIntent.putExtra(AISPlotterGlobals.GUIUpdateEvent_STATUS_KEY, pTarget.getStatusToDisplay());
    	aGUIUpdateIntent.putExtra(AISPlotterGlobals.GUIUpdateEvent_LON_KEY,pTarget.getLON());
    	aGUIUpdateIntent.putExtra(AISPlotterGlobals.GUIUpdateEvent_LAT_KEY, pTarget.getLAT());
    	aGUIUpdateIntent.putExtra(AISPlotterGlobals.GUIUpdateEvent_NAME_KEY,pTarget.getShipname());
    	aGUIUpdateIntent.putExtra(AISPlotterGlobals.GUIUpdateEvent_COG_KEY,pTarget.getCOG());
    	aGUIUpdateIntent.putExtra(AISPlotterGlobals.GUIUpdateEvent_COGSTR_KEY,pTarget.getCOGString());
    	aGUIUpdateIntent.putExtra(AISPlotterGlobals.GUIUpdateEvent_SOG_KEY,pTarget.getSOG());
    	aGUIUpdateIntent.putExtra(AISPlotterGlobals.GUIUpdateEvent_SOGSTR_KEY,pTarget.getSOGString());
    	aGUIUpdateIntent.putExtra(AISPlotterGlobals.GUIUpdateEvent_LASTPOSUTC_KEY,pTarget.mTimeOfLastPositionReport);
    	ctx.sendBroadcast(aGUIUpdateIntent);
    }   
	
	public void deactivateOldTargets(Date pDeactivateDate){
		AISTarget aTarget = null;
		if (mTargetList.isEmpty()) return;
		if (test) Log.d(TAG,"start Deactivation  ");
		int i = getSize()-1; 
		int numberOfDeletedTargets = 0;
		while ( i >= 0 ){
			aTarget = getTargetByNr(i);
			Date dateOfTarget = new Date(aTarget.getTimeOfLastStaticUpdate());
			if (dateOfTarget.before(pDeactivateDate) ) {
				if (aTarget.getStatusToDisplay() == AISPlotterGlobals.DISPLAYSTATUS_INACTIVE) {
				  // if the status is inactive then delete it
					if (test) Log.d(TAG,"Delete  " + aTarget.getMMSIString());
					String aName = aTarget.getShipname();
					String aMMSI = aTarget.getMMSIString();
					Logger.d(TAG,"deleting " + aName + " " + aMMSI);
					sendDeleteTargetEvent(aMMSI);
					Long id = aTarget.getId();
					removeTargetByNr(i);
					mDbAdapter.deleteTarget(id);
					boolean hasTrack = aTarget.getHasTrack();
					if (hasTrack){
						// delete track from database
						mDbAdapter.deleteShipTrackTable(aMMSI);
					}
					numberOfDeletedTargets++;
				} else {
				    // set status of the target to DISPLAYSTATUS_INACTIVE 
				    aTarget.setStatusToDisplay(AISPlotterGlobals.DISPLAYSTATUS_INACTIVE);
				    if (test) Log.d(TAG,"Deactivate  " + aTarget.getMMSIString());
				    String aName = aTarget.getShipname();
					String aMMSI = aTarget.getMMSIString();
					sendGUIUpdateEvent(aTarget);
					Logger.d(TAG,"deactivating " + aName + " " + aMMSI);
				    mDbAdapter.updateTarget(aTarget);
				}
			}
			i--;
		}
		
		String aMessage = mTargetList.size()+ " Targets in list " + numberOfDeletedTargets + " targets deleted";
		Logger.d(TAG,aMessage);
		Toast toast = Toast.makeText(ctx, aMessage, Toast.LENGTH_LONG);
		toast.show();
		
	}
	
	
	// the following is needed  to save the data to the sd-card
	private String getCurrentDateTime() {
		final Calendar c = Calendar.getInstance();
        int mYear = c.get(Calendar.YEAR);
        int mMonth = c.get(Calendar.MONTH);
        int mDay = c.get(Calendar.DAY_OF_MONTH);
        int mHour = c.get(Calendar.HOUR_OF_DAY);
        int mMinute = c.get(Calendar.MINUTE);
        int mSecond = c.get(Calendar.SECOND);
        StringBuffer buf = new StringBuffer();
        buf.append(mYear);
        buf.append("_");
        buf.append(mMonth +1);
        buf.append("_");
        buf.append(mDay);
        buf.append("_");
        buf.append(mHour);
        buf.append("_");
        buf.append(mMinute);
        buf.append("_");
        buf.append(mSecond);
        String aDateStr = buf.toString();
        if (test) Log.v(TAG,"Datum: " + aDateStr);
        return aDateStr;
	}
	
	private void createAISDirectoryIfNecessaryOld () {
		// not used since 12_10_30
		// external sd-card may be no mounted on sdcard2
		// use PositionTools.createAISDirectoryIfNecessary
		if (test) Log.v(TAG,"createAISDirectory");
		String result = Environment.getExternalStorageState();
		if (result.equals(Environment.MEDIA_MOUNTED)){
			
			// first we check if there is a directory named sdcard2
		    File path = Environment.getExternalStorageDirectory();
		    File [] subDirs = path.listFiles(new FileFilter() {
		    	@Override
		    	public boolean accept (File d){
		    		return d.isDirectory();
		    	}
		    });
		    boolean foundSdCard2 = false;
		    if (test)Log.d(TAG,"Subdirs of " + path.getName());
		    if (test)Log.d(TAG,"Subdirs of " + path.getAbsolutePath());
		    for (int index = 0; index < subDirs.length;index++) {
		    	String aName = subDirs[index].getName();
		    	if (test)Log.d(TAG,aName);
		    	if (aName.equals("sdcard2")) foundSdCard2 = true;
		    }
		    StringBuffer buf = new StringBuffer();
	        if (foundSdCard2)buf.append("sdcard2/");
	        buf.append("AISData");
	        String dirName = buf.toString();
		    File file = new File(path,dirName );
		    try {
		    	String filePathStr =  file.getAbsolutePath();
			    if (file.mkdir()) {  // here we need android permission in the manifest
			        if (test) Log.v(TAG,"erzeuge Directory: " + filePathStr);
			    } else {
			    	if (test) Log.v(TAG,"directory schon vorhanden " + filePathStr);
			    }
		    } catch (SecurityException se) {
		    	se.printStackTrace();
		    	if (test) Log.v("TAG", "Security exception : Directory not created " + se);
		    }// try
		}
	}
	
	public void WriteAISDataToExternalStorage() {
		 if (test) Log.v(TAG,"createExternalStorageAISData");
	    // Create a path where we will place our data in the user's
	    // public directory.  Note that you should be careful about
	    // what you place here, since the user often manages these files. 
		 // we write the data in a directory called AISPlotter/AISData
		 
		String aDataDirName = "/"+ AISPlotterGlobals.DEFAULT_APP_DATA_DIRECTORY
		                      +"/" + "AISData";
		PositionTools.createExternalDirectoryIfNecessary(aDataDirName);
		String result = Environment.getExternalStorageState();
		if (result.equals(Environment.MEDIA_MOUNTED)){
		    File path = PositionTools.getExternalStorageDir();
	        StringBuffer buf = new StringBuffer();
	        buf.append(aDataDirName);
	        buf.append("/AISdata");
	        buf.append(getCurrentDateTime());
	        buf.append(".txt");
	        String fileName = buf.toString();
		    File file = new File(path, fileName);
		    String filePathStr = file.getAbsolutePath();
		    try {
			    if (file.createNewFile()) {  // here we need android permission in the manifest
			        if (test) Log.v(TAG,"create file: " + filePathStr);
			    }else {
			    	if (test) Log.v(TAG,"file exists, overwrite " + filePathStr);
			    }
			    // the file exists or was opened for writing
		        BufferedWriter fileBuf = new BufferedWriter(new FileWriter(file));
		        if (!(mTargetList == null) && (!(mTargetList.isEmpty()))){
		        	 
		           for (int index = 0;index < mTargetList.size();index++){
		        	 AISTarget aTarget = mTargetList.get(index);
		        	 String theTargetDescription = aTarget.toString();
		        	 String aString = customFormat("0000",index)+" # "+ theTargetDescription +"\r\n";
		        	 fileBuf.write(aString);			
		           }  
		        }else {
		           fileBuf.write("no AIS-Targets in List \r\n");
		        }
		        fileBuf.flush();
		        fileBuf.close();
		        if (test) Log.v(TAG,"file write sucessfull " + filePathStr);
			   
		    } catch (IOException e) {

		    	e.printStackTrace();
		        // Unable to create file, likely because external storage is
		        // not currently mounted.
		        if (test)Log.w("TAG", "Error writing " + filePathStr);
		    }  // try
		
	   }
	}
	
	
	
	
	 private String customFormat(String pattern, double value ) {
	      DecimalFormat myFormatter = new DecimalFormat(pattern);
	      String output = myFormatter.format(value);
	      return output;
	  }
	 
	 
	
	
}
