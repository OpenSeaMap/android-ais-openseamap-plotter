package com.klein.commons;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.Map;

import java.util.TimeZone;

import org.mapsforge.android.maps.MapView;
import org.mapsforge.android.maps.mapgenerator.MapGeneratorJob;
import org.mapsforge.core.GeoPoint;
import org.mapsforge.core.IOUtils;
import org.mapsforge.core.MapPosition;
import org.mapsforge.core.MercatorProjection;



import android.os.Environment;
import android.util.Log;

import com.klein.filefilter.FilterByFileExtension;
import com.klein.logutils.Logger;



public class PositionTools {
	
	private static final String TAG = "PositionTools";
	private static final boolean test = false;
	 public static final FileFilter FILE_FILTER_EXTENSION_BACKUPSER = new FilterByFileExtension(".backupser");
	
	/**
	 * 
	 * @param aPos aPosition in degrees
	 * @return  a String formatted 10' 23,666 E/W
	 */
	public static String getLONString(double aPos) {
		  double grad = Math.floor(aPos);
		  double minutes = (aPos - grad) * 60;
		  StringBuffer sb = new StringBuffer();
		  sb.append(customFormat ("000" , grad));
		  sb.append("\' ");
		  sb.append(customFormat ("00.000",minutes));
		  //sb.append("\''");
		  // String aPosStr = format("%.0f", grad) + "° " +format("%2.3f",minutes);
		  if (aPos > 0) {
			  sb.append(" E");
		  } else {
			  sb.append(" W");
		  }
		  return sb.toString();
	  }
	
	/**
	 * 
	 * @param aPos aPosition in degrees
	 * @return a String formatted 54' 23,666 N/S
	 */
	public static String getLATString(double aPos) {
		  double grad = Math.floor(aPos);
		  double minutes = (aPos - grad) * 60;
		  StringBuffer sb = new StringBuffer();
		  sb.append(customFormat ("00" , grad));
		  sb.append("\' ");
		  sb.append(customFormat ("00.000",minutes));
		 // sb.append("\''");
		  // String aPosStr = format("%.0f", grad) + "° " +format("%2.3f",minutes);
		  if (aPos > 0) {
			  sb.append(" N");
		  } else {
			  sb.append(" S");
		  }
		  return sb.toString();
	  }
	
	/**
	 * 
	 * @param pattern  use a pattern like "000.00"
	 * @param value    the value to convert  45.34523
	 * @return  aString with the value formatted  045.34
	 */
	public static String customFormat(String pattern, double value ) {
	      DecimalFormat myFormatter = new DecimalFormat(pattern);
	      String output = myFormatter.format(value);
	      return output;
	  }
	
	
	public static String getTimeString(long aUTC) {
		Date aDate = new Date(aUTC);
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        cal.setTime(aDate);
        StringBuffer aTime = new StringBuffer();
        aTime.append(customFormat("00",cal.get(Calendar.HOUR_OF_DAY)));
        aTime.append(":");
        aTime.append(customFormat("00",cal.get(Calendar.MINUTE)));
        aTime.append(":");
        aTime.append(customFormat("00",cal.get(Calendar.SECOND)));
        return aTime.toString();
	}
	
	public static String getNavStatusString (int index){
	   	  if (index == 0) return "Under way using engine";
	   	  if (index == 1) return "At anchor";
	   	  if (index == 2) return "Not under command";
	   	  if (index == 3) return "Restricted manoeuverability";
	   	  if (index == 4) return "Constrained by her draught";
	   	  if (index == 5) return "Moored";
	   	  if (index == 6) return "Aground";
	   	  if (index == 7) return "Engaged in Fishing";
	   	  if (index == 8) return "Under way sailing";
	   	  if ((index >= 9)&&(index <= 10)) return "Reserved for future amendment of Navigational Status for HSC";
	   	  if ((index >= 11)&&(index <= 14)) return "Reserved for future use";
	   	  if (index == 15) return "Not defined (default)";
	   	 return "not defined may not occur"; 
	     }
	public static String getShipTypeStringFromIndex( int aIndex){
  	  if (aIndex == 0)return "Not available (default)";
  	  if ((aIndex >= 1)&& (aIndex <= 19)) return "Reserved for future use";
  	  if ((aIndex >= 20)&& (aIndex <= 29)) return "Wing in ground (WIG), all ships of this type";
  	  if (aIndex == 30) return "Fishing";
  	  if (aIndex == 31) return  "Towing" ;
  	  if (aIndex == 32) return "Towing: length exceeds 200m or breadth exceeds 25m";
  	  if (aIndex == 33) return "Dredging or underwater ops";
        if (aIndex == 34) return  "Diving ops";
	      if (aIndex == 35) return  "Military ops";
	      if (aIndex == 36) return "Sailing";
	      if (aIndex == 37) return "Pleasure Craft";
	      if ((aIndex <=38) && (aIndex <= 39))return "Reserved";
	      if ((aIndex >= 40)&& (aIndex <= 49)) return "High speed craft (HSC), all ships of this type";
	      if (aIndex == 50) return "Pilot Vessel";
	      if (aIndex == 51) return "Search and Rescue vessel";
	      if (aIndex == 52) return "Tug";
	      if (aIndex == 53) return "Port Tender";
	      if (aIndex == 54) return "Anti-pollution equipment";
	      if (aIndex == 55) return "Law Enforcement";
	      if ((aIndex >= 56)&&(aIndex <= 57)) return "Spare - Local Vessel";
	      if (aIndex == 58) return "Medical Transport";
	      if (aIndex == 59) return "Ship according to RR Resolution No. 18";
	      if ((aIndex >= 60) && (aIndex <= 69)) return "Passenger, all ships of this type";
	      if ((aIndex >= 70) && (aIndex <= 79)) return "Cargo, all ships of this type";
	      if ((aIndex >= 80) && (aIndex <= 89)) return "Tanker, all ships of this type";
	      if ((aIndex >= 90) && (aIndex <= 99)) return "Other Type, all ships of this type";
	      return  "not defined may not occur";
    }
	
	public static double calculateDistance(GeoPoint fromPoint, GeoPoint toPoint) {
		double rho = 180.0 /Math.PI;
		double lambda1 = fromPoint.getLongitude() /rho;
		double lambda2 = toPoint.getLongitude() / rho;
		double phi1 = fromPoint.getLatitude() /rho;
		double phi2 = toPoint.getLatitude() / rho;
		double mb = (phi1 + phi2) / 2;  // mittlere Breite
		double cosphi = Math.cos(mb);
		double diffLAT = fromPoint.getLatitude() - toPoint.getLatitude();
    	double diffLON = fromPoint.getLongitude() - toPoint.getLongitude();
    	double aDistanceInGrad = Math.sqrt(diffLAT * diffLAT +  diffLON * diffLON);
    	double aDistanceinMinutes = aDistanceInGrad * 60 * cosphi;
		return aDistanceinMinutes;
	}
	
	public static double calculateDistanceX(GeoPoint fromPoint, GeoPoint toPoint){
		return dist2 (fromPoint, toPoint)* 60;
	}
	
	public static double calculateCourse (GeoPoint fromPoint, GeoPoint toPoint){
		// Quelle: http://www.rainerstumpe.de/HTML/kurse1.html
		double rho = 180.0 /Math.PI;
		double lambda1 = fromPoint.getLongitude() /rho;
		double lambda2 = toPoint.getLongitude() / rho;
		double phi1 = fromPoint.getLatitude() /rho;
		double phi2 = toPoint.getLatitude() / rho;
		double mb = (lambda1 + lambda2) / 2;  // mittlere Breite
		double deltalambda = lambda2 - lambda1;
		double deltaphi = phi2 - phi1;
		double a = deltalambda * Math.cos(mb);
		double b = deltaphi;
		double alfa = 0;
		if (a < b) {
		     alfa = Math.atan(a/b);
		} else {
			alfa = Math.atan(b/a);
		}
		alfa = Math.abs(alfa)*rho;
		double kak = 0;
		
		if ((a>0) && (b>0)) {
			if (Math.abs(a)< Math.abs(b)){
				kak = alfa;
			} else {
				kak = 90 - alfa;
			}
		}
		
		if ((a > 0) && (b< 0)) {
			if (Math.abs(a)< Math.abs(b)){
				kak = 90 + alfa;
			} else {
				kak = 90 + alfa;
			}
			
		}
		
		
		if ((a<0)&& (b<0)) {
			if (Math.abs(a)< Math.abs(b)){
				kak = 270 - alfa;
			} else {
				kak = 180 + alfa;
			}
		}
		if ((a<0) && (b>0)){
			if (Math.abs(a)< Math.abs(b)){
				kak = 360-alfa;
			} else {
				kak = 360 - alfa;
			}
		}
		return kak;
	}
	
	public static double dist2  (GeoPoint fromPoint, GeoPoint toPoint){
		double rho = 180.0 /Math.PI;
		double lambda1 = fromPoint.getLongitude() /rho;
		double lambda2 = toPoint.getLongitude() / rho;
		double phi1 = fromPoint.getLatitude() /rho;
		double phi2 = toPoint.getLatitude() / rho;
		double mb = (lambda1 + lambda2) / 2;  // mittlere Breite
		double deltalambda = lambda2 - lambda1;
		double deltaphi = phi2 - phi1;
		double a = deltalambda * Math.cos(mb);
		double b = deltaphi;
		double alfa = 0;
		if (a < b) {
		     alfa = Math.atan(a/b);
		} else {
			alfa = Math.atan(b/a);
		}
		
		double c = a/ Math.sin(alfa);
		return c;
	}
	
	// the following is needed to save the data to the sd-card
	public static String getCurrentDateTimeForFilename() {
		final Calendar c = Calendar.getInstance();
		int mYear = c.get(Calendar.YEAR);
		if (mYear > 2000)
			mYear = mYear - 2000;
		int mMonth = c.get(Calendar.MONTH);
		int mDay = c.get(Calendar.DAY_OF_MONTH);
		int mHour = c.get(Calendar.HOUR_OF_DAY);
		int mMinute = c.get(Calendar.MINUTE);
		int mSecond = c.get(Calendar.SECOND);
		StringBuffer buf = new StringBuffer();
		buf.append(customFormat("00", mYear));
		buf.append("_");
		buf.append(customFormat("00", mMonth + 1));
		buf.append("_");
		buf.append(customFormat("00", mDay));
		buf.append("_");
		buf.append(customFormat("00", mHour));
		buf.append("_");
		buf.append(customFormat("00", mMinute));
		buf.append("_");
		buf.append(customFormat("00", mSecond));
		String aDateStr = buf.toString();
		return aDateStr;
	}
	// all from MapScalebar --> redrawScaleBar()
	private static final double METER_FOOT_RATIO = 0.3048;
	private static final int[] SCALE_BAR_VALUES_IMPERIAL = { 26400000, 10560000, 5280000, 2640000, 1056000, 528000,
		264000, 105600, 52800, 26400, 10560, 5280, 2000, 1000, 500, 200, 100, 50, 20, 10, 5, 2, 1 };
    private static final int[] SCALE_BAR_VALUES_METRIC = { 10000000, 5000000, 2000000, 1000000, 500000, 200000, 100000,
		50000, 20000, 10000, 5000, 2000, 1000, 500, 200, 100, 50, 20, 10, 5, 2, 1 };
    private static final int BITMAP_WIDTH = 150;
    
    
	public static int calculateMapScaleValue(MapView mapView) {
		MapPosition mapPosition = mapView.getMapPosition().getMapPosition();
		double groundResolution = MercatorProjection.calculateGroundResolution(mapPosition.geoPoint.getLatitude(),
				mapPosition.zoomLevel);
        
		int[] scaleBarValues;
		boolean imperialUnits = mapView.getMapScaleBar().isImperialUnits();
		if (imperialUnits) {
			groundResolution = groundResolution / METER_FOOT_RATIO;
			scaleBarValues = SCALE_BAR_VALUES_IMPERIAL;
		} else {
			scaleBarValues = SCALE_BAR_VALUES_METRIC;
		}

		float scaleBarLength = 0;
		int mapScaleValue = 0;

		for (int i = 0; i < scaleBarValues.length; ++i) {
			mapScaleValue = scaleBarValues[i];
			scaleBarLength = mapScaleValue / (float) groundResolution;
			if (scaleBarLength < (BITMAP_WIDTH - 10)) {
				break;
			}
		}
		return mapScaleValue;
	}
	
	public static File createDirectory(String pathName) {
		File file = new File(pathName);
		if (!file.exists() && !file.mkdirs()) {
			throw new IllegalArgumentException("could not create directory: " + file);
		} else if (!file.isDirectory()) {
			throw new IllegalArgumentException("not a directory: " + file);
		} else if (!file.canRead()) {
			throw new IllegalArgumentException("cannot read directory: " + file);
		} else if (!file.canWrite()) {
			throw new IllegalArgumentException("cannot write directory: " + file);
		}
		return file;
	}

	public static int getTileCacheSize (int mapViewId ) {
		String CACHE_DIRECTORY = "/Android/data/org.mapsforge.android.maps/cache/";
		String externalStorageDirectory = Environment.getExternalStorageDirectory().getAbsolutePath();
		String cacheDirectoryPath = externalStorageDirectory + CACHE_DIRECTORY + mapViewId;
		File cacheDirectory = createDirectory(cacheDirectoryPath);
		File[] allFiles = cacheDirectory.listFiles();
		int aSize = allFiles.length;
		return aSize;
	}
	
	public static void backUpTileCacheSerFile (String backupDirStr) {
		String CACHE_DIRECTORY = "/Android/data/org.mapsforge.android.maps/cache/";
		String SERIALIZATION_FILE_NAME = "cache.ser";
		
		String BACKUP_FILE_NAME = backupDirStr + "/cache_ser_backup_" + getCurrentDateTimeForFilename()+".backupser";
		String externalStorageDirectory = Environment.getExternalStorageDirectory().getAbsolutePath();
		String cacheDirectoryPath = externalStorageDirectory + CACHE_DIRECTORY + "1";
		File cacheDirectory = createDirectory(cacheDirectoryPath);
		File serializedMapFile = new File(cacheDirectory, SERIALIZATION_FILE_NAME);
		if (serializedMapFile.exists() && serializedMapFile.isFile() && serializedMapFile.canRead()) {
		  copyFile(serializedMapFile.getAbsolutePath(),BACKUP_FILE_NAME);
		}
		
	}
	
	public static boolean restoreCacheSerFile(String backUpFilePath){
		String CACHE_DIRECTORY = "/Android/data/org.mapsforge.android.maps/cache/";
		String SERIALIZATION_FILE_NAME = "cache.ser";
		String externalStorageDirectory = Environment.getExternalStorageDirectory().getAbsolutePath();
		String cacheDirectoryPath = externalStorageDirectory + CACHE_DIRECTORY + "1";
		String cacheSerFilePath = cacheDirectoryPath+"/" + SERIALIZATION_FILE_NAME;
		boolean ok =  copyFile(backUpFilePath,cacheSerFilePath);
		
		return ok;
	}
	
	public static ArrayList<String> getInfoAboutActiveFile (){
		String CACHE_DIRECTORY = "/Android/data/org.mapsforge.android.maps/cache/";
		String externalStorageDirectory = Environment.getExternalStorageDirectory().getAbsolutePath();
		String cacheDirectoryPath = externalStorageDirectory + CACHE_DIRECTORY + "1";
		ArrayList<String> aStringList = new ArrayList<String>();
		File cacheFileDir = new File(cacheDirectoryPath);
		Map<MapGeneratorJob, File> map = deserializeMap(cacheFileDir);
		/*// if we try to read the file value from the map, we get an exception
		// so we use a second map from which we can read the value --> file to get the filename
		Map<MapGeneratorJob, File> map2 = deserializeMap(cacheFileDir);
		try {
			
			if (map != null) {
				Set<MapGeneratorJob> aSet = map.keySet();
				for (Iterator<MapGeneratorJob> jobIterator = aSet.iterator(); jobIterator.hasNext();) {
					MapGeneratorJob aJob = jobIterator.next();
					Tile aTile = aJob.tile;
					
					File aFile = map2.get(aJob); // see above
					String aFileName = "null";
					if (aFile != null ) aFileName = aFile.getName();
					String aTileDesc = "tile.x " + aTile.tileX + " tile.y " + aTile.tileY + " zoom " + aTile.zoomLevel + " Filename: " + aFileName;
					aStringList.add(aTileDesc);
				}
			}
		} catch (Exception e ){
			// this may be a ConcurrentModificationException
			Log.d(TAG,e.toString());
		}*/
		if (map != null) {
			Collection<File> aCollection = map.values();
			int count = aCollection.size();
			
			for (Iterator<File> iterator = aCollection.iterator();iterator.hasNext();){
				File aFile=iterator.next();
				String aName = aFile.getName();
				aStringList.add(aName);
			}
		}
		return aStringList;
	}
	
	public  static void copy( InputStream in , OutputStream out)throws IOException {
		byte[] buffer = new byte[0xFFFF];
		for (int len; (len = in.read(buffer)) !=-1;) {
			out.write(buffer,0,len);
		}
	}
	
	private static Map<MapGeneratorJob, File> deserializeMap(File directory) {
		String SERIALIZATION_FILE_NAME = "cache.ser";
		File serializedMapFile = new File(directory, SERIALIZATION_FILE_NAME);
		if (!serializedMapFile.exists() || !serializedMapFile.isFile() || !serializedMapFile.canRead()) {
			return null;
		}

		FileInputStream fileInputStream = null;
		ObjectInputStream objectInputStream = null;
		try {
			fileInputStream = new FileInputStream(serializedMapFile);
			objectInputStream = new ObjectInputStream(fileInputStream);

			// the compiler warning in the following line cannot be avoided unfortunately
			Map<MapGeneratorJob, File> map = (Map<MapGeneratorJob, File>) objectInputStream.readObject();

			

			return map;
		} catch (IOException e) {
			Log.d(TAG, e.toString());
			return null;
		} catch (ClassNotFoundException e) {
			Log.d(TAG, e.toString());
			return null;
		} finally {
			IOUtils.closeQuietly(objectInputStream);
			IOUtils.closeQuietly(fileInputStream);
		}
	}
	
	private static boolean copyFile (String src, String dest) {
		boolean result = false;
		FileInputStream fis = null;
		FileOutputStream fos = null;
		try {
			fis = new FileInputStream(src);
			fos = new FileOutputStream(dest);
			copy(fis,fos);
			result = true;
		} catch (IOException e){
			Logger.d("cant create cache_ser_backup "+ e.toString());
		} finally {
			if (fis != null)
				try { fis.close(); }catch (IOException e){}
			if (fos != null)
				try { fos.close(); } catch (IOException e) {}
		}
		return result;
	}
	
	public static boolean hasSecondaryStorage() {
		boolean myHasSecondaryStorage = Environment2.isSecondaryExternalStorageAvailable();	
		return myHasSecondaryStorage;
	}
	
	public static File getExternalStorageDir() {
		// we use the Environment2.getCardDirectory since 12_11_19
		/*Device firstExternalStorageDevice = Environment2.getPrimaryExternalStorage();
        String firstExternalStorageDevicePath = firstExternalStorageDevice.getMountPoint();
        File aDir = firstExternalStorageDevice.getFile();
		String externalPathName = firstExternalStorageDevicePath;
		File externalStorageDir = aDir;
		boolean hasSecondaryStorage = Environment2.isSecondaryExternalStorageAvailable();
        if (hasSecondaryStorage){
        try {
        	aDir = Environment2.getSecondaryExternalStorageDirectory();
            String aPathname = aDir.getAbsolutePath();
            externalPathName = aPathname;
            externalStorageDir = aDir;
        } catch (Exception e){
        	Log.d(TAG,e.toString());
        }
        } else {
        	Log.d(TAG, "no Secondary storage on SD-Card: ");
        }*/
		File externalStorageDir = Environment2.getCardDirectory();
        return externalStorageDir;
	}
	
public static String getExternalStorageDirPath() {
		
		/*Device firstExternalStorageDevice = Environment2.getPrimaryExternalStorage();
        String firstExternalStorageDevicePath = firstExternalStorageDevice.getMountPoint();
        File aDir = firstExternalStorageDevice.getFile();
		String externalPathName = firstExternalStorageDevicePath;
		File externalStorageDir = aDir;
		boolean hasSecondaryStorage = Environment2.isSecondaryExternalStorageAvailable();
        if (hasSecondaryStorage){
        try {
        	aDir = Environment2.getSecondaryExternalStorageDirectory();
            String aPathname = aDir.getAbsolutePath();
            externalPathName = aPathname;
            externalStorageDir = aDir;
        } catch (Exception e){
        	Log.d(TAG,e.toString());
        }
        } else {
        	Log.d(TAG, "no Secondary storage on SD-Card: ");
        }*/
	     File aDir =  getExternalStorageDir();
	     String externalPathName = aDir.getAbsolutePath();
         return externalPathName;
	}
/**
 * Create the necessary directories to follow the path to the external directory given in pDirPath
 * knows the path to the cardDirectory, the /mnt/sdcard or /mnt/sdcard/sdcard2 
 * @param pDirPath  the path describing the path without ref to the card dir
 */

public static void createExternalDirectoryIfNecessary(String pDirPath) {
	if (test)
		Log.v(TAG, "createAISDirectory");
	//String result = Environment.getExternalStorageState(); since 12_11_19 use Environment2
	String result = Environment2.getCardState();
	if (result.equals(Environment.MEDIA_MOUNTED)) {

	//File path = getExternalStorageDir(); since 12_11_19 
	File path = Environment2.getCardDirectory();
	
		File file = new File(path, pDirPath);
		try {
			String filePathStr = file.getAbsolutePath();
			if (file.mkdirs()) { // here we need android permission in the manifest, mkdirs with generating parents if necessary
				if (test)
					Log.v(TAG, "erzeuge Directory: " + filePathStr);
			} else {
				if (test)
					Log.v(TAG, "directory schon vorhanden " + filePathStr);
			}
		} catch (SecurityException se) {
			Log.d(TAG,se.toString());
			if (test)
				Log.v("TAG", "Security exception : Directory nicht erzeugt " + se);
		} catch (Exception e ) {
			Log.d(TAG,e.toString());
			
		} // try
	
	}
}
public static void old_createExternalDirectoryIfNecessary(String pDirPath) {
	if (test)
		Log.v(TAG, "createAISDirectory");
	//String result = Environment.getExternalStorageState(); since 12_11_19 use Environment2
	String result = Environment2.getCardState();
	if (result.equals(Environment.MEDIA_MOUNTED)) {

		//File path = getExternalStorageDir(); since 12_11_19 
		File path = Environment2.getCardDirectory();
		// if pDirName contains / we have to analyse the whole path
		String [] dirs = pDirPath.split("/");
		int dirCount = dirs.length;
		String newDirName = "/"; // we must begin with a /
	    //  now we know how many dirs we must create
		for (int dirIndex =0;dirIndex < dirCount; dirIndex++){
			StringBuffer buf = new StringBuffer();
			newDirName = newDirName + dirs[dirIndex]  +"/";
			buf.append(newDirName);
			String dirName = buf.toString();
			File file = new File(path, dirName);
			try {
				String filePathStr = file.getAbsolutePath();
				if (file.mkdir()) { // here we need android permission in the manifest
					if (test)
						Log.v(TAG, "erzeuge Directory: " + filePathStr);
				} else {
					if (test)
						Log.v(TAG, "directory schon vorhanden " + filePathStr);
				}
			} catch (SecurityException se) {
				Log.d(TAG,se.toString());
				if (test)
					Log.v("TAG", "Security exception : Directory nicht erzeugt " + se);
			} catch (Exception e ) {
				Log.d(TAG,e.toString());
				
			} // try
		} //for
	}
}
		
}
