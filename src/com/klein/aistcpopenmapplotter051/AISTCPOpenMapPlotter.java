package com.klein.aistcpopenmapplotter051;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import org.mapsforge.android.AndroidUtils;
import org.mapsforge.android.maps.MapActivity;
import org.mapsforge.android.maps.MapController;
import org.mapsforge.android.maps.MapScaleBar;
import org.mapsforge.android.maps.MapView;
import org.mapsforge.android.maps.mapgenerator.MapGenerator;
import org.mapsforge.android.maps.mapgenerator.MapGeneratorInternal;
import org.mapsforge.android.maps.mapgenerator.TileCache;
import org.mapsforge.android.maps.mapgenerator.tiledownloader.MapnikTileDownloader;
import org.mapsforge.android.maps.mapgenerator.tiledownloader.OpenCycleMapTileDownloader;
import org.mapsforge.android.maps.overlay.ItemizedOverlay;
import org.mapsforge.android.maps.overlay.OverlayWay;
import org.mapsforge.core.GeoPoint;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.PathShape;
import android.location.Location;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.klein.activities.AISListActivity;
import com.klein.activities.FilePicker;
import com.klein.activities.MonitoringActivity;
import com.klein.activities.RouteTextActivity;
import com.klein.activities.TargetEditActivity;
import com.klein.activities.TrackListActivity;
import com.klein.commons.AISPlotterGlobals;
import com.klein.commons.Environment2;
import com.klein.commons.PositionTools;
import com.klein.downloaders.EniroAerialAndSeamarksTileDownloader;
import com.klein.downloaders.EniroAerialTileDownloader;
import com.klein.downloaders.EniroMapTileDownloader;
import com.klein.downloaders.EniroNauticalTileDownloader;
import com.klein.downloaders.OpenSeamapTileAndSeamarksDownloader;
import com.klein.downloaders.OsmTileDownloader;
import com.klein.filefilter.FilterByFileExtension;
import com.klein.filefilter.ValidMapFile;
import com.klein.logutils.Logger;
import com.klein.overlays.AISOverlayItem;
import com.klein.overlays.MyItemizedOverlay;
import com.klein.overlays.MyWaysOverlay;
import com.klein.overlays.RouteOverlayItem;
import com.klein.overlays.RoutePointItem;
import com.klein.overlays.SeamarksOverlay;
import com.klein.seamarks.SeamarkOsm;
import com.klein.seamarks.SeamarkSymbol;
import com.klein.service.AISTarget;
import com.klein.service.NMEATCPServiceFactory;
import com.klein.service.TargetList;
import com.klein.service.TrackDbAdapter;





/**
 * @author vk1
 * we use some overlays to display additional information on the map
 * MyItemizedOverlay mItemizedOverlay holds route point sysmbols and AIS-Targets
 * MyWaysOverlay mWaysOverlay holds ways for the AIS-Target tracks and route point connecting ways
 * SeamarksOverlay mSeamarksOverlay holds the Seamarks
 */
public class AISTCPOpenMapPlotter extends MapActivity {
    // debugging
	private static final String TAG = "AISTCPMapPlotter";  
    private static final boolean test =  false; 
	  
	// for file picker
    private static final FileFilter FILE_FILTER_EXTENSION_MAP = new FilterByFileExtension(".map");
    private static final FileFilter FILE_FILTER_EXTENSION_GPX = new FilterByFileExtension(".gpx");
    private static final int SELECT_MAP_FILE = 10;
    private static final int SELECT_RENDER_THEME_FILE = 11;
    private static final int SELECT_GPX_TRACK_FILE = 12;
	private static final int SELECT_GPX_ROUTE_FILE = 13;
	
	// Tile Cache
    private static final int TILE_CACHE_MAX_CAPACITY = 250;
    
	// symbols for the AISTargets, used in OverlayItem / mAISItemizedOverlay
	// private Drawable mShipMarkerBlue;
	 // private Drawable mShipMarkerGreen;
	 // private Drawable mShipMarkerBlueFrame;
	  private Drawable mBaseStationMarker;
	  
	  private Drawable mShipDefaultMarker;
	  private Bitmap mShipBlueSmallBitmap	;
	  private Bitmap mShipBlueSmallFrameBitmap;
	  private Bitmap mShipBlueInactiveSmallFrameBitmap;
	  
	  private Bitmap mShipRedSmallBitmap	;
	  private Bitmap mShipRedSmallFrameBitmap;
	  private Bitmap mShipRedInactiveSmallFrameBitmap;
	  
	  private Drawable mAIS_SART_Drawable;
	  private Drawable mAIS_SART_TEST_Drawable;
	  private Bitmap mOwnShipMarkerBitMap;
	  
	  //private TextView aTextView;
	  public MapView mapView1; // for use in ScreenshotCapturer and overlay
	  
	  private MapView mapView2;
	  private boolean snapToLocation = false;
	  private ToggleButton snapToLocationView;
	  private static final String BUNDLE_SNAP_TO_LOCATION = "snapToLocation";
	  
	  private ScreenshotCapturer screenshotCapturer;
	  /**
	   *  Symbols for the center of the map dealing with routes
	   */
	 
	  //private MyCrossOverlay mCrossOverlay;
	 
	  
	  
	  /**
		 * Holds the ways of the route and symbols
	  */
	  
	  // private ArrayWayOverlay mRouteArrayWayOverlay; 13_03_16
	 
	  // private ArrayRoutePointsItemizedOverlay mArrayRoutePointsOverlay; since 13_03_15 see mItemizedOverlay
	  
	  //private ArrayList<RoutePointItem> mRouteItemList; see nonprivate ItemizedOverlay
	  public ArrayList<RoutePointItem> mRouteItemList;
	  private boolean mRouteEditIsDirty;
	 
      private boolean mShowRoutePoints;
	  // trace a route 
	  private ArrayList<GeoPoint> mTracelist;
	  private int mTracePointIndex;
	  
	  private Button mBreakTraceRouteButton;
	  private boolean mTraceRouteActive = false;
	  private boolean mInRestoreRouteFromDatabase = false;
	  //private int mLastFetchedItem = 0;;
	  
	 // ProgressBar for loading targets from Database
	  // must be static since we use it in a thread
	  // corresponds to Show_Loading_Dialog_ID 
	  private static  ProgressDialog mProgressDialog = null;
	  private static Handler mProgressHandler = new Handler();
	  private static int mProgressStatus;
	  /**
	   * mAISItemizedOverlay is the overlay that displays the AISTargets and myShip
	   */
	  //private AISItemizedOverlay mAISItemizedOverlay; since 13_03_15
	  
	  private MyItemizedOverlay mItemizedOverlay; // holds AIS and Route overlay items
	  
	  public  MyWaysOverlay mWaysOverlay;  // holds the ways from the AIS Targets and the route ways
	  
	  // we must keep the route point symbol and the route ways paint in a variable
	  // as the cant be set as default values in mItemizedOverlay and mWaysOverlay
	  // will bes set in setupRouteDisplayParams
	   public BitmapDrawable mRoutePointSymbol = null;
	   public Paint mRouteWayPaintFill = null;
	   public Paint mRouteWayPaintOutline = null;
	  /** 
	   * mArrayAISTrackWayOverlay displays all the tracks that are actual recorded
	   */
	  //private ArrayWayOverlay mArrayAISTrackWayOverlay; see nonprivate AISItemizedOverlay
	  //public ArrayWayOverlay mArrayAISTrackWayOverlay;  13_03_16
	  
	  
	  // the SeamarksOverlay  
	  private float mDisplayFactor = 1;
	  private SeamarksOverlay mSeamarksOverlay = null;
	  private boolean mFillDirectionalSector = true;
	  private Bitmap mDefaultSeamark = null;
	  private SeamarkOsm mSeamarkOsm = null;
	  
	  private MapController mapController;
	 
	  //private MyBitmapMapOverlay mapViewOverlay; 
	  
	  private GeoPoint  mLastMapCenterPoint;
	  private GeoPoint  lastShipPoint;
	  private GeoPoint  mLastCenteredTargetPoint;
	  
	  // from aisplotter
	  
	  // private TrackDbAdapter mDbAdapter; see nonprivate AISItemizedOverlay
	  public  TrackDbAdapter mDbAdapter;
	  
	  // Local Bluetooth adapter
	 // private BluetoothAdapter mBluetoothAdapter = null;
	  
	  
	  //private TargetList mTargetList = null; see nonprivate AISItemizedOverlay
	  public TargetList mTargetList = null;
	 
	  
	  // private AISTarget myShip = null; see nonprivate AISItemizedOverlay
	  public AISTarget myShip;
	  private boolean firstGPSData = false;
	  
	  private long mCurrentNmeaTime = 0l;
      private long mTargetListLastUpdateTime ;//= mCurrentNmeaTime;
      private float mAISAlarmRadius ;  // in Meilen 
     // private boolean mShowShipData = false;
      private int mUseGPSSource = 0;
      //private boolean mShowSpeedMarkerOfTargets = true;
      //private boolean mUseSatteliteMap = true;
	  // end from aisplotter
    /* not in use 12_04_19
	  private DisplayObjectList mDisplayObjectList;
	  */
	  //private boolean mNewData = false;
	// for the BlueToothConnection
	  // fixed for test
	  
	 //public static final String DEFAULT_BT_ADDRESS = "00:06:66:06:BF:CE"; 
	// for the GPS Service
	 private GpsLocationServiceLocal gpsLocalService = null;
	 private GpsLocationServiceLocal.GpsLocalBinder gpsLocalServiceBinder;
	 boolean mLogPositionData = true;
 // for the server address menu
    
   // public static final float DEFAULT_ALARM_RADIUS = 4f; // 4 miles
    public static final String DEFAULT_ALARM_RADIUS_STRING = "4";
    public static final float DEFAULT_ZOOM_LEVEL = 9;
    public static final int DEFAULT_LAST_GEOPOINT_LAT = 54415700; // Burgtiefe
    public static final int DEFAULT_LAST_GEOPOINT_LON = 11194900; // Burgtiefe
    public static final String DEFAULT_SHIP_LAT = "54415700"; // Burgtiefe
    public static final String DEFAULT_SHIP_LON = "11194900"; // Burgtiefe  
	// Die folgenden definitionen müssen mit den Setzungen in strings.xml <!-- Einstellungen bearbeiten keys -->
    // uebereinstimmen
	  public static final String PREF_ALARM_RADIUS = "alarmradius";
	  
	   
	  
	  private static final int Delete_OldTargets_DIALOG_ID = 0;
	  private static final int Deactivate_OldTargets_DIALOG_ID = 1;
	  private static final int Show_Loading_Dialog_ID = 3;
    
	  SharedPreferences prefs; 
	 
	// end server address menu
	  
	// for the AISListActivity
	private static final int AISACTIVITY_LIST=0; 
	private static final int TRACKACTIVITY_LIST=1; 
  
	int updateDisplayIntervall = 1000; // update display every 1000 ms, see timerTask 
	int sixMinutes = 6 * 60*1000;
	
	private WakeLock wakeLock;
	
	private Button mRoutePointAddButton;
	private Button mRoutePointDeleteButton;
	private Button mRouteFinishButton;
	private Button mRouteSaveButton;
	private ProgressBar mProgressHorizontal;
	Handler mProgressbarHandler = new Handler(); // Handler for runnables for the progressBar
	
	// this deals with the data info screen on the left
	private boolean mDataScreenVisible = false; 
	private boolean mShowDataScreenPossible = false;
	
	private TextView mOSM_info;
	private TextView mOSM_info_LAT;
	private TextView mOSM_info_LON;
	private TextView mOSM_info_Windspeed;
	private TextView mOSM_info_Winddirection;
	private TextView mOSM_info_Depth;  
	private TextView mOSM_info_SOG;  
	private TextView mOSM_info_COG; 
	
	private TextView mOSM_Info_LAT_Label;
	private TextView mOSM_info_LON_Label;
	private TextView mOSM_info_Windspeed_Label;
	private TextView mOSM_info_Winddirection_Label;
	private TextView mOSM_info_Depth_Label;  
	private TextView mOSM_info_SOG_Label;  
	private TextView mOSM_info_COG_Label; 
	
	//private LinearLayout mRouteLayout ;   
	
	/**
	 * set up the TextViews for the datascreen if the data screen is visible
	 * we use then a different layout resource
	 * maybe it is better to reduce the width of the data screen
	 */
	private void setupDataScreen(){
		mOSM_info = (TextView) findViewById(R.id.OSM_Info_Head);
		mOSM_info_LAT = (TextView) findViewById(R.id.OSM_Info_LAT);
		mOSM_info_LON = (TextView) findViewById(R.id.OSM_Info_LON);
		mOSM_info_Windspeed = (TextView) findViewById(R.id.OSM_Info_Windspeed);
		mOSM_info_Winddirection = (TextView) findViewById(R.id.OSM_Info_Winddirection);
		mOSM_info_Depth = (TextView) findViewById(R.id.OSM_Info_Depth);
		mOSM_info_SOG = (TextView) findViewById(R.id.OSM_Info_SOG);
		mOSM_info_COG = (TextView) findViewById(R.id.OSM_Info_COG);
		//mRouteLayout = (LinearLayout) findViewById(R.id.route_layout);
		//mRouteLayout.setVisibility(View.INVISIBLE);
	}
	/**
	 * set up the visual params for the route and the buttons
	 * set up the ArrayWayOverlay and the ArrayRoutePointsOverlay
	 * set up the Cross Overlay for selecting the map center
	 */

	private void setupRouteDisplayParams() {
		// find the buttons for the route edit and install them
		this.mRoutePointAddButton = (Button) findViewById(R.id.route_pointadd);
		this.mRoutePointAddButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				GeoPoint newPoint = mapView1.getMapPosition().getMapCenter();
				addRoutePointOnOverlay(newPoint);
			}
		});
		this.mRoutePointAddButton.setVisibility(View.INVISIBLE);

		this.mRoutePointDeleteButton = (Button) findViewById(R.id.route_pointdelete);
		this.mRoutePointDeleteButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				deleteLastRoutePointFromOverlay();
			}
		});
		this.mRoutePointDeleteButton.setVisibility(View.INVISIBLE);

		this.mRouteFinishButton = (Button) findViewById(R.id.route_finish);
		this.mRouteFinishButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				writeRouteListToDatabase();
				finishRouteOnOverlay();
			}
		});
		this.mRouteFinishButton.setVisibility(View.INVISIBLE);

		this.mRouteSaveButton = (Button) findViewById(R.id.route_save);
		this.mRouteSaveButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				writeRouteListToDatabase();
				saveRoute_Menu();
			}
		});
		this.mRouteSaveButton.setVisibility(View.INVISIBLE);
		
		// this is the initial symbol for a route
		Bitmap aNewBitmap = Bitmap.createBitmap(30,30,Bitmap.Config.ARGB_8888);
		Canvas aCanvas = new Canvas(aNewBitmap);
		//aCanvas.drawColor(Color.WHITE);
		Paint aPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		//Paint aPaint = new Paint();
		aPaint.setStyle(Paint.Style.STROKE);
		aPaint.setStrokeWidth(1);
		aPaint.setColor(0x88FF0000); //red
		RectF aRoundedRect= new RectF(1, 1, 28,28);
		aCanvas.drawRoundRect(aRoundedRect, 5,5, aPaint);
		aPaint.setColor(Color.BLACK);
		aPaint.setTextSize(12);
		String aNumberStr = Integer.toString(-1);
		aCanvas.drawText(aNumberStr, 5, 20,aPaint);
		mRoutePointSymbol = new BitmapDrawable(aNewBitmap);
		/*
		 * try { this is done with the init of mItemizedOverlay
		  this.mArrayRoutePointsOverlay = new ArrayRoutePointsItemizedOverlay(ItemizedOverlay.boundCenter(bmd),this);
		  this.mapView1.getOverlays().add(mArrayRoutePointsOverlay);
		  
		
		} catch (Exception e) {
			Log.d(TAG,"tried to create RoutePointOverlay " + e.toString());
		}*/
		
		// create the default paint objects for overlay route ways not used since 13_03_16 
		// work is done in MyWaysOverlay  
		
		Paint wayDefaultPaintFill = new Paint(Paint.ANTI_ALIAS_FLAG);
		wayDefaultPaintFill.setStyle(Paint.Style.STROKE);
		wayDefaultPaintFill.setColor(Color.BLACK);
		wayDefaultPaintFill.setAlpha(160);
		wayDefaultPaintFill.setStrokeWidth(3);
		wayDefaultPaintFill.setStrokeJoin(Paint.Join.ROUND);
		wayDefaultPaintFill.setPathEffect(new DashPathEffect(new float[] { 20, 20 }, 0));

		Paint wayDefaultPaintOutline = new Paint(Paint.ANTI_ALIAS_FLAG);
		wayDefaultPaintOutline.setStyle(Paint.Style.STROKE);
		wayDefaultPaintOutline.setColor(Color.BLACK);
		wayDefaultPaintOutline.setAlpha(128);
		wayDefaultPaintOutline.setStrokeWidth(3);
		wayDefaultPaintOutline.setStrokeJoin(Paint.Join.ROUND);

		// create an individual paint object for an overlay way, not used  used since 13_03_16
		Paint wayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		wayPaint.setStyle(Paint.Style.FILL);
		wayPaint.setColor(Color.BLACK);
		wayPaint.setAlpha(192);
		wayPaint.setStrokeWidth(3);
		
		mRouteWayPaintFill = wayDefaultPaintFill;
		mRouteWayPaintOutline = wayDefaultPaintOutline;
		// set up the key for the route
		
		this.mWaysOverlay.addOverlayWayKey(AISPlotterGlobals.DEFAULTROUTE);
		// create the WayOverlay and add the ways
		/*try {
		  this.mRouteArrayWayOverlay = new ArrayWayOverlay(wayDefaultPaintFill, wayDefaultPaintOutline);
		  this.mapView1.getOverlays().add(this.mRouteArrayWayOverlay);
		} catch (Exception e) {
			Log.d(TAG,"tried to create RouteWayOverlay " + e.toString());
		}*/
		
/*      since 12_08_03
		this.mRouteOverlay = new ArrayItemizedOverlay(null);
		this.mapView.getOverlays().add(mRouteOverlay);*/

		/* cross is painted in ArrayRoutePointsOverlay.drawOverlayBitmap
		try {
		mCrossOverlay = new MyCrossOverlay();
		this.mapView.getOverlays().add(mCrossOverlay);
		} catch (Exception e) {
			Log.d(TAG,"tried to create cross overlay " + e.toString());
		}
*/
	}
	
	/**
	 *  set up the params for the  AIS-ways and the holding ArrayWayOverlay
	 */
	
	private void setWayDisplayParams() {
		 // set the paint options for the routes
	     // create the default paint objects for overlay ways
			Paint wayDefaultPaintFill = new Paint(Paint.ANTI_ALIAS_FLAG);
			wayDefaultPaintFill.setStyle(Paint.Style.STROKE);
			wayDefaultPaintFill.setColor(Color.RED);
			wayDefaultPaintFill.setAlpha(160);
			wayDefaultPaintFill.setStrokeWidth(3);
			wayDefaultPaintFill.setStrokeJoin(Paint.Join.ROUND);
			wayDefaultPaintFill.setPathEffect(new DashPathEffect(new float[] { 20, 20 }, 0));

			Paint wayDefaultPaintOutline = new Paint(Paint.ANTI_ALIAS_FLAG);
			wayDefaultPaintOutline.setStyle(Paint.Style.STROKE);
			wayDefaultPaintOutline.setColor(Color.RED);
			wayDefaultPaintOutline.setAlpha(128);
			wayDefaultPaintOutline.setStrokeWidth(3);
			wayDefaultPaintOutline.setStrokeJoin(Paint.Join.ROUND);

			// create an individual paint object for an overlay way
			Paint wayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
			wayPaint.setStyle(Paint.Style.FILL);
			wayPaint.setColor(Color.YELLOW);
			wayPaint.setAlpha(192);
			/*try { til 13_03_16
			// create the AISWayOverlay and add the ways
			mArrayAISTrackWayOverlay = new ArrayWayOverlay(wayDefaultPaintFill,
					wayDefaultPaintOutline);
			GeoPoint geoPoint1 = new GeoPoint(51.42, 6.88); // 
			GeoPoint geoPoint2 = new GeoPoint(51.42, 7.12); // 
			OverlayWay aOverlayWay = new OverlayWay(new GeoPoint[][] { { geoPoint1, geoPoint2 } });
			mArrayAISWayOverlay.addWay(aOverlayWay);
			mapView1.getOverlays().add(mArrayAISTrackWayOverlay);
			} catch (Exception e) {
				Log.d(TAG,"tried to create TrackWayOverlay " + e.toString());
			}*/
			try {
				mWaysOverlay = new MyWaysOverlay(wayDefaultPaintFill,wayDefaultPaintOutline);
				
			}catch (Exception e) {
				Log.d(TAG,"tried to create WaysOverlay " + e.toString());
			}
			
			
	}
	
	
	
	/**
	 * the AIS-Overlay is updated every second, hold in updateDisplayIntervall  
	 */
	
	
	private static Handler mAISOverlayRefreshhandler = new Handler();
	
	/**
	 * the runnable, that is executed from mAISOverlayRefreshhandle 
	 */
	private Runnable overlayRefresh = new Runnable() {
		public void run() {
			// mAISItemizedOverlay.requestRedraw(); // 13_03_15
			mItemizedOverlay.requestRedraw();
			mAISOverlayRefreshhandler.postDelayed(this,updateDisplayIntervall);
		} 
	};
	
	/**
	 * here we check every six minutes if the information for the AISTargets is outdated
	 * the AISTarget-list is iterated with the date befor all old targets should be 
	 * deactivated. if a target was deactivted before, it is deleted from the list and
	 * the database
 	 */
	
	  private static Handler mRefreshTargetListHandler = new Handler();
	
		private Runnable timertaskRefreshTargetList = new Runnable() {
			public void run() {
				// updateTargetList
				
				String weMustUpdateStr = prefs.getString("deactivateoldtargets", "off");
				if (weMustUpdateStr.equals("on")) {
					Logger.d(TAG,"deactivating old targets in list");
					if (test) Log.d(TAG,"updateTargetList");
					Date deactivateBeforeDate = new Date (getCurrentUTCNmeaTime() - sixMinutes);
					//Toast.makeText(AISTCPOpenMapPlotter.this, 
				 	//		   "deactivating old targets in list", Toast.LENGTH_LONG).show();
					if (mTargetList != null) {
						int numberOfTargetsBefore = mTargetList.getSize();
						mTargetList.deactivateOldTargets(deactivateBeforeDate);
						int numberOfTargetsAfter = mTargetList.getSize();
						Logger.d(TAG,"Deleted " + (numberOfTargetsBefore - numberOfTargetsAfter) + " targets");
						mTargetListLastUpdateTime = System.currentTimeMillis();
					}
					mRefreshTargetListHandler.postDelayed(this, sixMinutes);
				}
			}
		};
	  
	  /**
	   * Receive a information from from the nmea-parser
	   
	   * main entry for GUI-Updates
	   * Action handled
	   * ACTION_GPSNMEADATA  	GPS-Data from extermal gps via nmea
	   * ACTION_UPDATEGUI    	aTarget has changed its data 
	   * ACTION_INITGUI      	in the initphase we get events to create the visible targets
	   * ACTION_DELETE_TARGET	a target was deleted from the database cause it was too old
	   */

	  private BroadcastReceiver mNMEAParserDataReceiver = new BroadcastReceiver() {
	  @Override
		public void onReceive(Context context, Intent intent) { 
		  //Toast.makeText(context, "GPS-Data received" , Toast.LENGTH_SHORT).show();
		  if (test)Log.d(TAG,"NMEAParserDataReceiver -- receive ");
		  if (intent != null){
				String action = intent.getAction(); 
				{
					if(test)Log.d(TAG,action);
					//String data  =intent.getStringExtra("data");
					//Log.d(TAG,data);
				}
				// we get different actions from the NMEA-Parser
		 if (action.equals(AISPlotterGlobals.ACTION_GPSNMEADATA)&& mUseGPSSource == 2){
			// we have to update myShip 
			 double aLAT = intent.getDoubleExtra(AISPlotterGlobals.GUIUpdateEvent_LAT_KEY,0);
			 double aLON = intent.getDoubleExtra(AISPlotterGlobals.GUIUpdateEvent_LON_KEY, 0);
			 int aCOG = intent.getIntExtra(AISPlotterGlobals.GUIUpdateEvent_COG_KEY, 0);
			 int aSOG = intent.getIntExtra(AISPlotterGlobals.GUIUpdateEvent_SOG_KEY, 0);
			 //String aCOGStr = intent.getStringExtra(AISPlotterGlobals.GUIUpdateEvent_COGSTR_KEY);
			 //String aSOGStr = intent.getStringExtra(AISPlotterGlobals.GUIUpdateEvent_SOGSTR_KEY);
			 
			 
			 long aLastPosUTCTime = intent.getLongExtra(AISPlotterGlobals.GUIUpdateEvent_LASTPOSUTC_KEY, 0);
			 if (test) {
				 Log.d(TAG,"own ship position LAT " + PositionTools.getLATString(aLAT) +
						 " LON " + PositionTools.getLONString(aLON));
			 }
			 
			 myShip.setLAT(aLAT);
			 myShip.setLON(aLON);
			 myShip.setSOG(aSOG);
			 float aCOGInDegrees = aCOG / 10.0f;
			 myShip.setCOGInDegrees(aCOGInDegrees);
			 myShip.setTimeOfLastPositionReport(aLastPosUTCTime);
			 boolean hasTrack = myShip.getHasTrack();
			 // lastShipPoint =  new GeoPoint(aLAT,aLON); used in mapsforge 0.2.4
			 lastShipPoint =  new GeoPoint(aLAT,aLON);
			 int aDisplayStatus = AISPlotterGlobals.DISPLAYSTATUS_OWN_SHIP;
			 boolean inInitPhase ;
			 if (firstGPSData){
				 inInitPhase = true; // check if there is a track to restore from database
				 firstGPSData = false;
			 } else {
				 inInitPhase = false;
			 }
			 String aSOGStr = PositionTools.customFormat ("##0.0",aSOG/10.0d);
			 String aCOGStr = PositionTools.customFormat ("##0",aCOG/10.0d);
			 int specialManueverStatus = 0;  int shipLenght = 0; int shipWidth = 0;
			 byte navStatus = 0;
	        if (hasTrack){  // added 13_12_08, see the error 
	        	// we must update the tracktable to myShip
	        	String aMMSI = myShip.getMMSIString();
	        	if (!mDbAdapter.isTableToMMSIInShipTrackList(aMMSI)){
	        		mDbAdapter.createShipTrackTable(aMMSI);
	        	}
	        	
        		if (!inInitPhase) {
        			String aLATStr = Double.toString(myShip.getLAT());
        			String aLONStr = Double.toString(myShip.getLON());
        			mDbAdapter.insertTrackPointToTable(aMMSI, aLATStr, aLONStr, aLastPosUTCTime);
        		}
	        }
			 updateTargetOnOverlay(myShip.getMMSIString(),myShip.getShipname(),aDisplayStatus,aLAT, aLON,aCOG,aSOGStr,aCOGStr,aLastPosUTCTime,hasTrack,
					 myShip.getManueverStatus(),navStatus,myShip.getLength(),myShip.getWidth(),inInitPhase);
			 // for test purpose we display the depth if available 2013_11_30
			 String theCurrentTitle = (String) getTitle();
			 double aDepth = intent.getDoubleExtra(AISPlotterGlobals.GUIUpdateEvent_DEPTH_KEY,0d);
			 String aDepthStr = PositionTools.customFormat("##0.0", aDepth);
			 double aWindSpeed = intent.getDoubleExtra(AISPlotterGlobals.GUIUpdateEvent_WIND_SPEED_KEY,0d);
			 String aWindSpeedStr = PositionTools.customFormat("##0.0", aWindSpeed);
			 double aWindAngle = intent.getDoubleExtra(AISPlotterGlobals.GUIUpdateEvent_WIND_ANGLE_KEY,0d);
			 String aWindAngleStr = PositionTools.customFormat("##0.0", aWindAngle);
			 setTitle (theCurrentTitle + "  Depth: " + aDepthStr + " m    Wind Speed: " + aWindSpeedStr + " kn");
			 if(mShowDataScreenPossible && mDataScreenVisible) {   
				 // we have a visible data screen and display the data here
				 
					mOSM_info_LAT.setText(PositionTools.getLATString(aLAT));
					mOSM_info_LON.setText(PositionTools.getLONString(aLON));
					mOSM_info_Windspeed.setText(aWindSpeedStr);
					mOSM_info_Winddirection.setText(aWindAngleStr);
					mOSM_info_Depth.setText(aDepthStr);
					mOSM_info_SOG.setText(aSOGStr);   
					mOSM_info_COG.setText(aCOGStr);	   
				 
			 }
			 //Logger.d(TAG,"initphase finished");	
			 } // action equals ACTION_GPSNMEADATA
		 
		if (action.equals(AISPlotterGlobals.ACTION_UPDATEGUI)){
			// we have a to update the GUI with the data from a updated AISTarget
			String aMMSIStr = intent.getStringExtra(AISPlotterGlobals.GUIUpdateEvent_MMSI_KEY);
			String aName = intent.getStringExtra(AISPlotterGlobals.GUIUpdateEvent_NAME_KEY);
			int aDisplayStatus = intent.getIntExtra(AISPlotterGlobals.GUIUpdateEvent_STATUS_KEY, 0);
			double aLAT = intent.getDoubleExtra(AISPlotterGlobals.GUIUpdateEvent_LAT_KEY,0);
			double aLON = intent.getDoubleExtra(AISPlotterGlobals.GUIUpdateEvent_LON_KEY, 0);
			int aCOG = intent.getIntExtra(AISPlotterGlobals.GUIUpdateEvent_COG_KEY, 0);
			String aCOGStr = intent.getStringExtra(AISPlotterGlobals.GUIUpdateEvent_COGSTR_KEY);
			String aSOGStr = intent.getStringExtra(AISPlotterGlobals.GUIUpdateEvent_SOGSTR_KEY);
			long aLastPosUTCTime = intent.getLongExtra(AISPlotterGlobals.GUIUpdateEvent_LASTPOSUTC_KEY, 0);
			boolean hasTrack = intent.getBooleanExtra(AISPlotterGlobals.GUIUpdateEvent_HASTRACK_KEY, false);
			byte specialManueverStatus = intent.getByteExtra(AISPlotterGlobals.GUIUpdateEvent_SPECIAL_MANUEVER_KEY, (byte) 0);
			int shipLength = intent.getIntExtra(AISPlotterGlobals.GUIUpdateEvent_SHIP_LENGTH_KEY, 0);
			int shipWidth = intent.getIntExtra(AISPlotterGlobals.GUIUpdateEvent_SHIP_WIDTH_KEY,0);
			byte navStatus = intent.getByteExtra(AISPlotterGlobals.GUIUpdateEvent_NAV_STATUS_KEY,(byte)0);
			boolean inInitPhase = false;
			updateTargetOnOverlay(aMMSIStr,aName,aDisplayStatus,aLAT, aLON,aCOG,aSOGStr,aCOGStr,aLastPosUTCTime,
					                hasTrack,specialManueverStatus,navStatus, shipLength, shipWidth, inInitPhase);
		}
		if (action.equals(AISPlotterGlobals.ACTION_INITGUI)){
			// we have a to update the GUI with the data from a updated AISTarget
			String aMMSIStr = intent.getStringExtra(AISPlotterGlobals.GUIUpdateEvent_MMSI_KEY);
			String aName = intent.getStringExtra(AISPlotterGlobals.GUIUpdateEvent_NAME_KEY);
			int aDisplayStatus = intent.getIntExtra(AISPlotterGlobals.GUIUpdateEvent_STATUS_KEY, 0);
			double aLAT = intent.getDoubleExtra(AISPlotterGlobals.GUIUpdateEvent_LAT_KEY,0);
			double aLON = intent.getDoubleExtra(AISPlotterGlobals.GUIUpdateEvent_LON_KEY, 0);
			int aCOG = intent.getIntExtra(AISPlotterGlobals.GUIUpdateEvent_COG_KEY, 0);
			String aCOGStr = intent.getStringExtra(AISPlotterGlobals.GUIUpdateEvent_COGSTR_KEY);
			String aSOGStr = intent.getStringExtra(AISPlotterGlobals.GUIUpdateEvent_SOGSTR_KEY);
			long aLastPosUTCTime = intent.getLongExtra(AISPlotterGlobals.GUIUpdateEvent_LASTPOSUTC_KEY, 0);
			boolean hasTrack = intent.getBooleanExtra(AISPlotterGlobals.GUIUpdateEvent_HASTRACK_KEY, false);
			byte specialManueverStatus = intent.getByteExtra(AISPlotterGlobals.GUIUpdateEvent_SPECIAL_MANUEVER_KEY, (byte) 0);
			int shipLength = intent.getIntExtra(AISPlotterGlobals.GUIUpdateEvent_SHIP_LENGTH_KEY, 0);
			int shipWidth = intent.getIntExtra(AISPlotterGlobals.GUIUpdateEvent_SHIP_WIDTH_KEY,0);
			byte navStatus = intent.getByteExtra(AISPlotterGlobals.GUIUpdateEvent_NAV_STATUS_KEY,(byte)0);
			boolean inInitPhase = true;
			updateTargetOnOverlay(aMMSIStr,aName,aDisplayStatus,aLAT, aLON,aCOG,aSOGStr,aCOGStr,aLastPosUTCTime,
					                 hasTrack, specialManueverStatus, navStatus, shipLength, shipWidth, inInitPhase);
			
		}
		
     	if (action.equals(AISPlotterGlobals.ACTION_DELETE_TARGET)) {
 			String aMMSIStr = intent.getStringExtra(AISPlotterGlobals.GUIUpdateEvent_MMSI_KEY);
 			String info = "Target " + aMMSIStr + " deleted";
 			Logger.d(TAG,info);
 		    if (test)Log.d(TAG,info);
 			// deleteTargetOnOverlay(aMMSIStr,mAISItemizedOverlay); 13_03_12
 			deleteTargetOnOverlay(aMMSIStr,mItemizedOverlay);
     	}	
     	
	   } // intent != null
	  } // onReceive
	 };
	  
	 
	  /**
	   *  delete the target with pMMSIStr from the AISOverlay
	   *  if the target has a track, delete all ways that belong to this target
	   * @param pMMSIStr
	   */
	  //private void deleteTargetOnOverlay (String pMMSIStr, AISItemizedOverlay overlay){ 13_03_12
	 private void deleteTargetOnOverlay (String pMMSIStr, MyItemizedOverlay overlay){
		  AISOverlayItem aItem  = null;
		  // find the Item with pMMSI  13_03_12
		  /*boolean found = false;
		  int aSize = overlay.size();
		  for (int index = 0;index < aSize; index++) {
	    	  aItem  =  (AISOverlayItem)overlay.getItem(index);
	    	  String aMMSIStr = aItem.getTitle();
	    	  if (aMMSIStr.equals(pMMSIStr)){
	    		  found = true;
	    		  break;
	    	  }
		  } */
		  
		  aItem = overlay.getAISItemByMMSI(pMMSIStr);
		  //  if ((aItem != null )&& found){ 13_03_12
		  
		  if (aItem != null ){
			  if (aItem.hasTrack()){
				  // remove all ways from the ArrayWayOverlay concerning to this AISTarget
				  ArrayList<OverlayWay> theWays = aItem.getOverlayways();
				  int count = theWays.size();
				  for (int index = 0;index < count;index++){
					  OverlayWay aOverlayWay = theWays.get(index);
					  //mArrayAISTrackWayOverlay.removeWay(aOverlayWay);
					  mWaysOverlay.removeWay(pMMSIStr, aOverlayWay);
				  }
				  theWays.clear();
			  }
			  overlay.removeItem(aItem) ;
			  if (test) Log.d(TAG,"deletetargetOnOverlay " + pMMSIStr);
		  }
		  //aSize = overlay.size()-1; 13_02_12
		  int aSize = overlay.getAISListSize()-1;
		  String infoKnows = getResources().getString(R.string.guiupdate_infoknows);
		  String info = getResources().getString(R.string.title)
		               + " " + infoKnows + " " + aSize +  " AISTargets";
		  this.setTitle(info);
	  }
	  
	  /**
	   * we read the track data from the database and insert a way to  mArrayAISWayOverlay
	   * @param pMMSIStr
	   * @param hasTrack
	   * @param inInitPhase
	   */
	  private void restoreTrackInInitPhase(String pMMSIStr,AISOverlayItem pNewItem){
		Log.d(TAG,"try to restore the track to " + pMMSIStr);
		Cursor aCursor = null;
		try {
			 if (!mDbAdapter.isTableToMMSIInShipTrackList(pMMSIStr)) {
				 Log.d(TAG," tracktable to MMSI "+ pMMSIStr + " does not exist");
				 return;
			 }
			 
			 mWaysOverlay.clearWay(pMMSIStr);  // clears the way on the Overlay to the Target with MMSI
			 // in initPhase is the track to the mmsi not in the ways dictionary, his caused a null pointer exception 13_09_27
			 // we have to call it here, otherwise the thread in restoreTrackwithThread is not executed. Why ???
			 restoreTrackWithThread(pMMSIStr,pNewItem);
		} catch(SQLException e) {
			Log.d(TAG, "RestoreTrackInInitPhase,SQL Exception"+ e.toString());
		}catch (Exception e){
			Log.d(TAG, "RestoreTrackInInitPhase , other Exception"+ e.toString());
		} finally {
			if (aCursor != null) {
				aCursor.close();
			}
		}
	  }
	  
	  /**
	   * update a AISTarget on the overlay
	   * set the new info in the title bar
	   * 
	   * @param pMMSIStr
	   * @param pName
	   * @param pStatus
	   * @param pLAT
	   * @param pLON
	   * @param pCOG
	   * @param pSOGStr
	   * @param pCOGStr
	   * @param pLastPosUTCTime
	   * @param hasTrack
	   * @param inInitPhase
	   */
	  
	  private void updateTargetOnOverlay(String pMMSIStr, String pName, int pStatus, 
			                             double pLAT, double pLON, int pCOG,
			                             String pSOGStr, String pCOGStr,
			                             long pLastPosUTCTime, boolean hasTrack, byte specialManueverStatus, byte navStatus, 
			                             int shipLength, int shipWidth, boolean inInitPhase) {
		  // the snippet info is actually new set in the overlays ontap
		  Matrix rotationMatrix = new Matrix();
		  AISOverlayItem aItem  = null;
		  boolean is_AIS_SART = false;
		  String aAISSartInfo = "AIS SART --- AIS-SART";
		  Drawable aAIS_SART_Drawable = mAIS_SART_Drawable; 
		  if (pMMSIStr.startsWith("970")) {
			  is_AIS_SART = true; 
			  if (navStatus==15) {	 
				  aAIS_SART_Drawable = mAIS_SART_TEST_Drawable;
				  aAISSartInfo = "AIS SART TEST--- AIS-SART TEST";
			  }
			   
		  }
		  // find the Item with pMMSI in the mAISItemizedOverlay
		  String specialManueverInfo = "xxx";
    	  boolean showsBlueTable = false;
    	  switch (specialManueverStatus) {
    	       case 0 : specialManueverInfo = "no Blue table info";
    	           break;
    	       case 1 : specialManueverInfo = "Blue Table not shown";
    	           break;
    	       case 2 : specialManueverInfo = "shows Blue Table";
    	                showsBlueTable = true;
    	           break;
    	  }
		  boolean found = false;
		 /* int aSize = mAISItemizedOverlay.size(); 13_03_12
		  for (int index = 0;index < aSize; index++) {
	    	  aItem  =  (AISOverlayItem)mAISItemizedOverlay.getItem(index);
	    	  String aMMSIStr = aItem.getTitle();
	    	  if (aMMSIStr.equals(pMMSIStr)){
	    		  found = true;
	    		  break;
	    	  }
		  }*/
		  aItem = mItemizedOverlay.getAISItemByMMSI(pMMSIStr);
		  
		  if (test) {
			  long aMMSI = Long.parseLong(pMMSIStr);
			  AISTarget aTestTarget = mTargetList.findTargetByMMSI(aMMSI);
			  int itemListSize = mItemizedOverlay.getAISListSize();
			  int targetListSize = mTargetList.getSize();
			  Log.d(TAG,"targetListSize " + targetListSize + " itemListSize " + itemListSize);
		  }
		  found = (aItem != null );
		  if (!found) {
			  // new target, create a new Item
			  AISOverlayItem aNewItem = null;
			  if (test)Log.d(TAG,"new Item " + pMMSIStr + " " + pName);
			  
			  long aUTCDiff = pLastPosUTCTime -  System.currentTimeMillis();
	    	  String aTimeStr =PositionTools.getTimeString(aUTCDiff);
	    	  String aReceivedInfo = getResources().getString(R.string.guiupdate_info_received_sec_ago);
	    	  String aNoTrackToShowInfo = getResources().getString(R.string.guiupdate_info_no_track_to_show);
	    	  
	    	  String aInfo = pName + "\n" + "LAT: " + PositionTools.getLATString(pLAT) 
	    	                       + "\nLON: " + PositionTools.getLONString(pLON) 
	    	                       + "\nCOG " + pCOGStr 
	    	                       + "\nSOG " + pSOGStr
	    	                       + "\n" +aReceivedInfo + " " + aTimeStr
	    	                       + "\n" +aNoTrackToShowInfo
	    	                       + "\n" + specialManueverInfo;
	    	 
			  GeoPoint geoPoint = new GeoPoint(pLAT,pLON);
			  // create the right symbol and turn it
			  if (pStatus == AISPlotterGlobals.DISPLAYSTATUS_INACTIVE) {  
	    		  if (is_AIS_SART) {
	    			  aNewItem = new AISOverlayItem(geoPoint, pMMSIStr,aInfo,
		    	    		                        ItemizedOverlay.boundCenterBottom(aAIS_SART_Drawable),
		    	    		                        hasTrack);
		    	      mItemizedOverlay.addItem(aNewItem);
	    		  }else {
	    			  float grad = ((float) pCOG) / 10f;
					  rotationMatrix.setRotate(grad); 
					  Bitmap theBitmap = null;
					  if (showsBlueTable){
						  theBitmap = mShipRedInactiveSmallFrameBitmap; 
					  } else {
						  theBitmap = mShipBlueInactiveSmallFrameBitmap;
					  }
					  Bitmap rotatedbitmap = Bitmap.createBitmap(theBitmap,
			                    0, 0, theBitmap.getWidth(), 
			                    theBitmap.getHeight(),
			                    rotationMatrix, true); 
					  BitmapDrawable bmd = new BitmapDrawable(rotatedbitmap);
		    	      aNewItem = new AISOverlayItem(geoPoint, pMMSIStr,aInfo,
		    	    		  ItemizedOverlay.boundCenterBottom(bmd),hasTrack);
		    	      mItemizedOverlay.addItem(aNewItem);
	    		  }
	    	  }
	    	  if (pStatus == AISPlotterGlobals.DISPLAYSTATUS_HAS_POSITION) {
	    		  if (is_AIS_SART) {
	    			  aNewItem = new AISOverlayItem(geoPoint, pMMSIStr,aInfo,
		    	    		                        ItemizedOverlay.boundCenterBottom(aAIS_SART_Drawable),
		    	    		                        hasTrack);
		    	      mItemizedOverlay.addItem(aNewItem);
	    		  }else {
		    		  float grad = ((float) pCOG) / 10f;
					  rotationMatrix.setRotate(grad); 
					  Bitmap theBitmap = null;
					  if (showsBlueTable){
						  theBitmap = mShipRedSmallFrameBitmap; 
					  } else {
						  theBitmap = mShipBlueSmallFrameBitmap;
					  }
					  Bitmap rotatedbitmap = Bitmap.createBitmap(theBitmap,
			                    0, 0, theBitmap.getWidth(), 
			                    theBitmap.getHeight(),
			                    rotationMatrix, true); 
					  BitmapDrawable bmd = new BitmapDrawable(rotatedbitmap);
		    	      aNewItem = new AISOverlayItem(geoPoint, pMMSIStr,aInfo,
		    	    		  ItemizedOverlay.boundCenterBottom(bmd),hasTrack);
		    	      mItemizedOverlay.addItem(aNewItem);
	    		  }
	    	  }
	    	  if (pStatus == AISPlotterGlobals.DISPLAYSTATUS_HAS_SHIPREPORT) {
	    		  if (is_AIS_SART) {
	    			  aNewItem = new AISOverlayItem(geoPoint, pMMSIStr,aInfo,
		    	    		                        ItemizedOverlay.boundCenterBottom(aAIS_SART_Drawable),
		    	    		                        hasTrack);
		    	      mItemizedOverlay.addItem(aNewItem);
	    		  }else {
		    		  float grad = ((float) pCOG) / 10f;
					  rotationMatrix.setRotate(grad); 
					  Bitmap theBitmap = null;
					  if (showsBlueTable){
						  theBitmap = mShipRedSmallBitmap; 
					  } else {
						  theBitmap = mShipBlueSmallBitmap;
					  }
					  Bitmap rotatedbitmap = Bitmap.createBitmap(theBitmap,
			                    0, 0, theBitmap.getWidth(), 
			                    theBitmap.getHeight(),
			                    rotationMatrix, true); 
					  BitmapDrawable bmd = new BitmapDrawable(rotatedbitmap);
		    	      aNewItem = new AISOverlayItem(geoPoint, pMMSIStr,
		  					aInfo,
							ItemizedOverlay.boundCenterBottom(bmd),hasTrack);
		    	      // mAISItemizedOverlay.addItem(aNewItem); 13_03_12 // default is frame
		    	      mItemizedOverlay.addItem(aNewItem);
	    		  }
	    	  }
	    	  if (pStatus == AISPlotterGlobals.DISPLAYSTATUS_BASE_STATION) {
	    	      aNewItem = new AISOverlayItem(geoPoint, pMMSIStr,
	  					aInfo,
						ItemizedOverlay.boundCenterBottom(mBaseStationMarker), hasTrack);
	    	      // mAISItemizedOverlay.addItem(aNewItem); 13_03_12 // default is frame
	    	      mItemizedOverlay.addItem(aNewItem);
	    	  }
	    	  
	    	  if (pStatus == AISPlotterGlobals.DISPLAYSTATUS_OWN_SHIP) {
	    		  float grad = ((float) pCOG) / 10f;
				  rotationMatrix.setRotate(grad); 
				  Bitmap rotatedbitmap = Bitmap.createBitmap( mOwnShipMarkerBitMap, 0, 0, 
						  						mOwnShipMarkerBitMap.getWidth(), 
						  						mOwnShipMarkerBitMap.getHeight(),
												rotationMatrix, true); 
				  BitmapDrawable bmd = new BitmapDrawable(rotatedbitmap);
	    	      aNewItem = new AISOverlayItem(geoPoint, pMMSIStr,
	  					aInfo,
						ItemizedOverlay.boundCenterBottom(bmd), hasTrack);
	    	      // mAISItemizedOverlay.addItem(aNewItem); 13_03_12 // default is frame
	    	      mItemizedOverlay.addItem(aNewItem);
	    	  }
	    	  if (pStatus == AISPlotterGlobals.DISPLAYSTATUS_SELECTED) {
	    		  if (is_AIS_SART) {
	    			  aNewItem = new AISOverlayItem(geoPoint, pMMSIStr,aInfo,
		    	    		                        ItemizedOverlay.boundCenterBottom(aAIS_SART_Drawable),
		    	    		                        hasTrack);
		    	      mItemizedOverlay.addItem(aNewItem);
	    		  }else {
		    		  float grad = ((float) pCOG) / 10f;
					  rotationMatrix.setRotate(grad); 
					  Bitmap theBitmap = null;
					  if (showsBlueTable){
						  theBitmap = mShipRedSmallBitmap; 
					  } else {
						  theBitmap = mShipBlueSmallBitmap;
					  }
					  Bitmap rotatedbitmap = Bitmap.createBitmap(theBitmap,
			                    0, 0, theBitmap.getWidth(), 
			                    theBitmap.getHeight(),
			                    rotationMatrix, true); 
					  BitmapDrawable bmd = new BitmapDrawable(rotatedbitmap);
		    	      aNewItem = new AISOverlayItem(geoPoint, pMMSIStr,
		  					aInfo,
							ItemizedOverlay.boundCenterBottom(bmd),hasTrack);
		    	      // mAISItemizedOverlay.addItem(aNewItem); 13_03_12 // default is frame
		    	      mItemizedOverlay.addItem(aNewItem);
	    		  }
	    	  }
	    	  // we set up the new AISOverlayItem
	    	  // now we look if it has a track to restore
	    	  if (aNewItem != null) {
	    		  if (hasTrack && inInitPhase){
	    			  // we deliver aNewaItem as a parameter to set the waylist
	    	    	 restoreTrackInInitPhase(pMMSIStr,aNewItem);  
	    	      } 
	    	  }
		  } 
		  else if ((aItem != null )&& found) {
			  // be crazy that we have a valid item reference
			  // target has moved, we must update info and symbol
	    	String aMMSIStr = aItem.getTitle();
	    	try {
		    	  GeoPoint oldTargetPoint = aItem.getPoint();
			      GeoPoint aTargetPoint = new GeoPoint (pLAT,pLON );	
			      if (aItem.hasTrack()) {
			    	// we create a way from the last(old) targetpoint to the new targetpoint
			        OverlayWay aOverlayWay = new OverlayWay(new GeoPoint[][] { { oldTargetPoint, aTargetPoint } });
			        //mArrayAISTrackWayOverlay.addWay(aOverlayWay); 13_03_16
			        mWaysOverlay.addWay(aMMSIStr,aOverlayWay);
			        aItem.addWay(aOverlayWay);
			        if (test) Log.d(TAG, "add new way to " + aMMSIStr +" "+ pName);
			      }
				  aItem.setPoint(aTargetPoint);
				  String aName = "---";
		    	  if (!pName.equals("")) {
		    		  aName = pName;
		    	  }
		    	  long aUTCDiff = pLastPosUTCTime -  System.currentTimeMillis();
		    	  String aTimeStr =PositionTools.getTimeString(aUTCDiff);
		    	  int aDisplayStatus = pStatus;
		    	  String aReceivedInfo = getResources().getString(R.string.guiupdate_info_received_sec_ago);
		    	  String aNoTrackToShowInfo = getResources().getString(R.string.guiupdate_info_no_track_to_show);
		    	  String aTrackToShowInfo = getResources().getString(R.string.guiupdate_info_track_to_show);
		    	  String aInfo = pName + "\n" + "LAT: " + PositionTools.getLATString(pLAT) 
				              + "\nLON: " + PositionTools.getLONString(pLON) 
				              + "\nCOG " + pCOGStr 
				              + "\nSOG " + pSOGStr
				              + "\n" +aReceivedInfo + " " + aTimeStr
				              + "\n" +aNoTrackToShowInfo
				              + "\n" + specialManueverInfo;
		    	 
		    	  if (aItem.hasTrack()){
		    		 aInfo = aInfo + "\n" + aTrackToShowInfo ;
		    	  } 
		    	  else {
		    		 aInfo = aInfo + "\n" + aNoTrackToShowInfo  ;
		    	  }
		    	  aItem.setSnippet(aInfo);
		          String aLogInfo = aName  + "\nSOG " + pSOGStr 
	                                       + "\n" + aReceivedInfo + " " + aTimeStr;
		    	  if (test) Log.d(TAG,"ship position changed " + aLogInfo);
		    	  // choose the right symbol
		    	  if (aDisplayStatus == AISPlotterGlobals.DISPLAYSTATUS_INACTIVE) {
		    		  if (is_AIS_SART) {
			    	       aItem.setMarker (ItemizedOverlay.boundCenter(aAIS_SART_Drawable));		                     
		    		  }else {
			    		  float grad = ((float) pCOG) / 10f;
						  rotationMatrix.setRotate(grad); 
						  Bitmap theBitmap = null;
						  if (showsBlueTable){
							  theBitmap = mShipRedInactiveSmallFrameBitmap; 
						  } else {
							  theBitmap = mShipBlueInactiveSmallFrameBitmap;
						  }
						  Bitmap rotatedbitmap = Bitmap.createBitmap(theBitmap,
				                    0, 0, theBitmap.getWidth(), 
				                    theBitmap.getHeight(),
				                    rotationMatrix, true); 
			    		  aItem.setMarker (ItemizedOverlay.boundCenter(new 
								  BitmapDrawable (rotatedbitmap))); 
			    		 if (test)  Log.d(TAG," Status inactive");
		    		  }
		    	  }
		    	  if (aDisplayStatus == AISPlotterGlobals.DISPLAYSTATUS_HAS_POSITION) {
		    		  if (is_AIS_SART) {
			    	       aItem.setMarker (ItemizedOverlay.boundCenter(aAIS_SART_Drawable));		                     
		    		  }else {
			    		  float grad = ((float) pCOG) / 10f;
						  rotationMatrix.setRotate(grad); 
						  Bitmap theBitmap = null;
						  if (showsBlueTable){ 
							  theBitmap = mShipRedSmallFrameBitmap; 
						  } else {
							  theBitmap = mShipBlueSmallFrameBitmap;
						  }
						  Bitmap rotatedbitmap = Bitmap.createBitmap(theBitmap,
				                    0, 0, theBitmap.getWidth(), 
				                    theBitmap.getHeight(),
				                    rotationMatrix, true); 
			    		  aItem.setMarker (ItemizedOverlay.boundCenter(new 
								  BitmapDrawable (rotatedbitmap))); 
			    		 if (test)  Log.d(TAG," new direction " + grad);
		    		  }
		    	  }
		    	  if (aDisplayStatus == AISPlotterGlobals.DISPLAYSTATUS_HAS_SHIPREPORT) {
		    		  if (is_AIS_SART) {
			    	       aItem.setMarker (ItemizedOverlay.boundCenter(aAIS_SART_Drawable));		                     
		    		  }else {
			    		  float grad = ((float) pCOG) / 10f;
						  rotationMatrix.setRotate(grad); 
						  Bitmap theBitmap = null;
						  if (showsBlueTable){
							  theBitmap = mShipRedSmallBitmap; 
						  } else {
							  theBitmap = mShipBlueSmallBitmap;
						  }
						  Bitmap rotatedbitmap = Bitmap.createBitmap(theBitmap,
				                    0, 0, theBitmap.getWidth(), 
				                    theBitmap.getHeight(),
				                    rotationMatrix, true); 
						  /*
						  BitmapDrawable bmd = new BitmapDrawable(rotatedbitmap);
						  aItem.setMarker(bmd);
						  */
						  aItem.setMarker (ItemizedOverlay.boundCenter(new 
								  BitmapDrawable (rotatedbitmap))); 
						  if (test)Log.d(TAG," new direction " + grad);
		    		  }
		    	  }
		    	  
		    	  
		    	  if (pStatus == AISPlotterGlobals.DISPLAYSTATUS_OWN_SHIP) {
		    		  float grad = ((float) pCOG) / 10f;
					  rotationMatrix.setRotate(grad); 
					  Bitmap rotatedbitmap = Bitmap.createBitmap( mOwnShipMarkerBitMap, 0, 0, 
							  						mOwnShipMarkerBitMap.getWidth(), 
							  						mOwnShipMarkerBitMap.getHeight(),
													rotationMatrix, true); 
					  aItem.setMarker (ItemizedOverlay.boundCenter(new 
							  						BitmapDrawable (rotatedbitmap))); 
					  if (snapToLocation) {
						  centerMapToShip();
					  }
		    	  }
		    	  
		    	  if (aDisplayStatus == AISPlotterGlobals.DISPLAYSTATUS_SELECTED) {
		    		  if (is_AIS_SART) {
			    	       aItem.setMarker (ItemizedOverlay.boundCenter(aAIS_SART_Drawable));		                     
		    		  }else {
			    		  float grad = ((float) pCOG) / 10f; 
						  rotationMatrix.setRotate(grad); 
						  Bitmap theBitmap = null;
						  if (showsBlueTable){
							  theBitmap = mShipRedSmallBitmap; 
						  } else {
							  theBitmap = mShipBlueSmallBitmap;
						  }
						  Bitmap rotatedbitmap = Bitmap.createBitmap(theBitmap,
				                    0, 0, theBitmap.getWidth(), 
				                    theBitmap.getHeight(),
				                    rotationMatrix, true); 
						  /*
						  BitmapDrawable bmd = new BitmapDrawable(rotatedbitmap);
						  aItem.setMarker(bmd);
						  */
						  aItem.setMarker (ItemizedOverlay.boundCenter(new 
								  BitmapDrawable (rotatedbitmap))); 
						  if (test)Log.d(TAG," new direction " + grad);
		    		  }
		    	  }

    		  }
              catch (Exception e){
    			 e.printStackTrace(); 
    		  }
	    		  
	       } // item != null and found
		  
		  // set the info for the top info line and display it
		  // lastt target list update was in aTime seconds before
		  long aTime = System.currentTimeMillis()-mTargetListLastUpdateTime ;
		  
		  // int countAISItems = mAISItemizedOverlay.size()-1; 13_03_12
		  int countAISItems = mItemizedOverlay.getAISListSize()-1;
		  /*double aLatCenter = lastMapCenterPoint.getLatitude();
		  double aLonCenter = lastMapCenterPoint.getLongitude();
		  GeoPoint aGp = mapView.getMapPosition().getMapCenter();*/
		  //String info = "llat " + aLatCenter + " llon " + aLonCenter +  "clat " + aGp.getLatitude() + " clon " + aGp.getLongitude()
		  //String info =  getResources().getString(R.string.title)+ " kennt  "
		  String aNewPositionForInfo = getResources().getString(R.string.guiupdate_info_new_position_for);
		  /* don't need it in production
		   * String info = countAISItems +  " AISTargets "
		                      + PositionTools.getTimeString(aTime);*/
		  String info = "";
		  if (test) {
			  info = countAISItems +  " AISTargets " + PositionTools.getTimeString(aTime);; 
		  } else {
			  info = countAISItems +  " AISTargets ";
		  }
		  if (pName.equals("")) {
			  info = info + " " + aNewPositionForInfo + " " + pMMSIStr;
		  } else {
			  info = info + " " + aNewPositionForInfo + " " + pName;
		  }
		  if (is_AIS_SART){
			  info = info + "  " + aAISSartInfo;
			  showToastOnUiThread(aAISSartInfo);
			  mItemizedOverlay.requestRedraw();
		  }
		  
		  this.setTitle(info);
		  
	  }  
	  
	  /**
	   * set the gpsSource from reading the prefs
	   */
	  private void setGPSSourceFromPrefs(){
		  mUseGPSSource = AISPlotterGlobals.NO_GPS;
		  String prev_GPS_key = getResources().getString(R.string.pref_use_gps_source); 
		  String[] gpsValues = getResources().getStringArray(R.array.use_gps_values);
		  String defaultValue = gpsValues[0];
		  String gpsSourceStr = prefs.getString(prev_GPS_key, defaultValue); 
		  if (gpsSourceStr.equals(gpsValues[1])) {
			  mUseGPSSource = AISPlotterGlobals.INTERNAL_GPS; 
		  }
		  if (gpsSourceStr.equals(gpsValues[2])) {
			  mUseGPSSource = AISPlotterGlobals.NMEA_GPS; 
		  }
		  if (mUseGPSSource == 1)  { 
              Log.d(TAG, "AISMapPlotter->internal GPS-Service is starting ...");
              Logger.d(TAG, "AISMapPlotter->internal GPS-Service is starting ...");
              //verbindeMitGPSService();
      	      Intent intent = new Intent(this, GpsLocationServiceLocal.class);
      	      try {
      	       if (bindService(intent, localGPSServiceConnection, Context.BIND_AUTO_CREATE)){
      	         Log.d(TAG, "AISMapPlotter->GPS-Service gestartet");
      	        }
      	       else {
      	    	 Log.d(TAG, "AISMapPlotter->GPS-Service nicht  gestartet");
      	       }
      	      }
      	      catch (SecurityException e) {
      	        Log.e(TAG, "AISMapPlotter->intent: Fehler: " + e.toString()); 
      	      } // end try
          } // mUseGPSSource == 1 , internal gps
	  }
	  
	  /**
	   * keep the screen light on
	   */
	  private void setScreenLightOn () {
		  Window w = getWindow(); // in Activity's onCreate() for instance 
		  w.setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON ,         
				     WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	  }
	/*  
	  *//**
	   * AISOverlayItem holds the data for a AISItem
	   * mhasTrack
	   * mWayList
	   * @author vkADM
	   * ways could be added to the item addWay()
	   * a reference to all ways of this item is hold in mWaylist, to allow deleteing the ways to this item
	   * with clearWays()
	   *//*
	  
	  private class AISOverlayItem extends OverlayItem {
		  private double mLON;
		  private double mLAT;
		  private boolean mHasTrack;
		  private ArrayList<OverlayWay> mWayList;
		  
		 public  AISOverlayItem(boolean hasTrack) {
			  super();
			  mHasTrack = hasTrack;
			  mWayList = new ArrayList<OverlayWay>();
		  }
		  *//**
		   * @param point hols  the geoPoint of the item
		   * @param title holds the mmsi, is the main reference for serching in the lists
		   * @param snippet  holds initial information about course and speed
		   * @param marker holds the  sysmbol for the target
		   * @param hasTrack has the target a track?
		   *//*
		  public AISOverlayItem(GeoPoint point, String title, String snippet,
				Drawable marker , boolean hasTrack) {
			super(point,title,snippet, marker);
			mHasTrack = hasTrack;
			mWayList = new ArrayList<OverlayWay>();
		}
		public double mLON() {
			  return mLON;
		  }
		  public double getLAT() {
			  return mLAT;
		  }
		  
		  public void setLON (double pLON){
			  mLON = pLON;
		  }
		  public void setLAT (double pLAT){
			  mLAT = pLAT;
		  }
		  
		  public void addWay (OverlayWay pOverlayWay) {
			 mWayList.add(pOverlayWay); 
		  }
		  
		  public ArrayList<OverlayWay> getOverlayways() {
			  return mWayList;
		  }
		  public void clearWays() {
			  mWayList.clear();
		  }
		  
	  }
	 */
	  /**
	   * there is only one instance of AISItemizedOverlay, mAISItemizedOverlay
	   * @author vkADM
	   * a Item may be tapped and shows information over the target
	   * if a target has a track, a dialog asks if the track can be saved or deleted
	   * if a target has no track, a track recording can be initiated
	   * there is no way to disable track recording, and restart it laster
	   */
	 // This class is made a non private class 13_03_14 
	 /* private class AISItemizedOverlay extends ArrayItemizedOverlay {
			private final Context context;
           
			*//**
			 * Constructs a new AISItemizedOverlay.
			 * 
			 * @param defaultMarker
			 *            the default marker (may be null).
			 * @param context
			 *            the reference to the application context.
			 *//*
			AISItemizedOverlay(Drawable defaultMarker, Context context) {
				super(defaultMarker);
				this.context = context;
			}
			
			*//**
			 * Handles a long press event.
			 * <p>
			 
			 * @param index
			 *            the index of the item that has been long pressed.
			 * @return true if the event was handled, false otherwise.
			 *//*
			
			 This does not run without error
			 * the TargetEditActivity is shown
			 * but we crash when we leave AISPlotter
			@Override
			protected boolean onLongPress(int index) {
				OverlayItem item = getItem(index);
				if (item != null) {
					GeoPoint aItemPoint = item.getPoint();
		    		String aMMSIStr = item.getTitle();
		    		try {
			    		  long aMMSI = Long.parseLong(aMMSIStr);
			    		  AISTarget aTarget = mTargetList.findTargetByMMSI(aMMSI);
			    		  showEditTarget(aTarget);
		    		}
	                  catch (Exception e){
		    			 e.printStackTrace(); 
		    		}
		    		
				return true;
				}
			  return false;
			}

			*//**
			 * Handles a tap event on the given item.
			 * we search in the targetList with the mmsi, which is stored in the title
			 *//*
			@Override
			protected boolean onTap(int index) {
				AISOverlayItem item = (AISOverlayItem)getItem(index);
				if (item != null) {
					String aName = "---";
					GeoPoint aItemPoint = item.getPoint();
		    		String aMMSIStr = item.getTitle();
		    		String aInfo = item.getSnippet();
		    		AISTarget aTarget = null;
		    		try {
		    		  long aMMSI = Long.parseLong(aMMSIStr);
		    		  aTarget = mTargetList.findTargetByMMSI(aMMSI);
		    		  if (!aTarget.getShipname().equals("")) {
			    		  aName = aTarget.getShipname();
			    	  }
		    		} catch (Exception e){
		    			 e.printStackTrace(); 
		    			 // we get this exception if mmsi denotes myShip, cause it is not in the list
		    			 // of the AISTargets
		    			 if (aMMSIStr.equals(myShip.getMMSIString())){
		    				aTarget = myShip; 
		    			 }
		    		}
		    		double aTargetLAT = aTarget.getLAT();
		    		double aTargetLON = aTarget.getLON();
	    			  
			    	  
			    	String aCOGStr = aTarget.getCOGString();
			    	String aSOGStr = aTarget.getSOGString();
			    	long aUTCDiff = System.currentTimeMillis() - aTarget.getTimeOfLastPositionReport();
			    	String aTimeStr =PositionTools.getTimeString(aUTCDiff);
			    	int aDisplayStatus = aTarget.getStatusToDisplay();
			    	//String aInfoStr = AISTCPOpenMapPlotter.this.getResources().getString(R.string.aisItem_info1);
			    	String aInfoStr = getResources().getString(R.string.aisItem_info1);
			    	aInfo = aName  + "\n" + "LAT: " + PositionTools.getLATString(aTargetLAT) 
	    	                       + "\nLON: " + PositionTools.getLONString(aTargetLON) 
	    	                       + "\nCOG " + aCOGStr 
	    	                       + "\nSOG " + aSOGStr
	    	                       + "\n" + aInfoStr + " " + aTimeStr;
			    	  
			    	  
		    		
	                if (!mDbAdapter.isTableToMMSIInShipTrackList(aMMSIStr)){
		                	// track table was deleted or no track yet
		                	aTarget.setHasTrack(false);
		                	item.setHasTrack(false);
		                	mDbAdapter.updateTarget(aTarget);
		                		
		            } else {
		            	aTarget.setHasTrack(true);
	                	item.setHasTrack( true);
	                	mDbAdapter.updateTarget(aTarget);
		            }
	                if (item.hasTrack()){
			    		 aInfo = aInfo + "\n" + getResources().getString(R.string.aisItem_track_is_recorded) ;
			    	  } 
			    	  else {
			    		 aInfo = aInfo + "\n" + getResources().getString(R.string.aisItem_track_is_not_recorded);
			    	  } 
	                final String theMMSI = aMMSIStr;
	                final String theName = aName;
	                final AISOverlayItem aItem = item;
	                final AISTarget actualTarget = aTarget;
					Builder builder = new AlertDialog.Builder(this.context);
					builder.setIcon(android.R.drawable.ic_menu_info_details);
					builder.setTitle(item.getTitle());
					builder.setMessage(aInfo);
					builder.setPositiveButton("OK", null);
					if (!item.hasTrack()){ 
						builder.setNeutralButton(R.string.aisItem_record_track, new OnClickListener(){
							public void onClick(DialogInterface dialog , int which) {
								if (actualTarget != null){
									if (!actualTarget.getHasTrack()){
										aItem.setHasTrack( true);
										if (!mDbAdapter.isTableToMMSIInShipTrackList(theMMSI)){
						            		mDbAdapter.createShipTrackTable(theMMSI);
						            		Log.d(TAG,"create track table to "+ theMMSI);
						            	}
										actualTarget.setHasTrack(true);
										if (test)Log.d(TAG,"selected ship to track " + theMMSI + " " + theName);
									}
									
								}
								
							}
						});
						
						
					} else {
						builder.setNeutralButton(R.string.aisItem_delete_track, new OnClickListener(){
							public void onClick(DialogInterface dialog , int which) {
								if (actualTarget != null){
									if (actualTarget.getHasTrack()){
										if (aItem.hasTrack()){
											  // remove all ways from the ArrayWayOverlay concerning to this AISTarget
											  ArrayList<OverlayWay> theWays = aItem.getOverlayways();
											  int count = theWays.size();
											  for (int index = 0;index < count;index++){
												  OverlayWay aOverlayWay = theWays.get(index);
												  Log.d(TAG,"Delete way "+ theMMSI + " " + index);
												  mArrayAISTrackWayOverlay.removeWay(aOverlayWay);
											  }
											  theWays.clear();
										  }
										aItem.setHasTrack( false);
										if (mDbAdapter.isTableToMMSIInShipTrackList(theMMSI)){
						            		mDbAdapter.deleteShipTrackTable(theMMSI);
						            		Log.d(TAG,"delete track table to "+ theMMSI);
						            		
						            	}
										actualTarget.setHasTrack(false);
										if (test) Log.d(TAG,"no further tracking on " + theName);
									}
								}
							}
						});
						
						builder.setNegativeButton(R.string.aisItem_save_track,new OnClickListener(){
							public void onClick(DialogInterface dialog , int which) {
								if (actualTarget != null){
									if (actualTarget.getHasTrack()){
										if (mDbAdapter.isTableToMMSIInShipTrackList(theMMSI)){
						            		mDbAdapter.WriteTrackDataToExternalStorage(theMMSI);
						            		Log.d(TAG,"save track table to "+ theMMSI);
						            	}
									}
								}
							}
						});
						
						
					}
					builder.show();
					
					return true;
				}
				return false;
			}
			
			public AISOverlayItem getItem (int index) {
				return (AISOverlayItem)createItem(index);
			}
			
			public void updateItem (int index) {
				AISOverlayItem item = (AISOverlayItem)createItem(index);
				if (item != null) {
					String aMMSI = item.getTitle();
					
				}
			}
		}
	  */
	  /** 
		 * Sets all file filters and starts the FilePicker to select a map file.
		 */
		private void startMapFilePicker() {   
			FilePicker.setFileDisplayFilter(FILE_FILTER_EXTENSION_MAP);
			FilePicker.setFileSelectFilter(new ValidMapFile());
			startActivityForResult(new Intent(this, FilePicker.class), SELECT_MAP_FILE);
		} 
		       
	/**
	 * only for test purpose	 
	 */
		
	  final View.OnClickListener onButton8Click=new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Button temp = ((Button) v);
				temp.setVisibility(Button.INVISIBLE);
			}
		};
		
		
		
	  @Override 
	  public void onCreate(Bundle savedInstanceState) { 
		    super.onCreate(savedInstanceState); 
		    Bundle extras = getIntent().getExtras();
			
		    requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
			this.screenshotCapturer = new ScreenshotCapturer(this);
			this.screenshotCapturer.start();
		    PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
			//this.wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "AISOMP");
			this.wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "AISOMP");
			
		    prefs = PreferenceManager.getDefaultSharedPreferences(this);
		    DisplayMetrics metrics = new DisplayMetrics();
		    getWindowManager().getDefaultDisplay().getMetrics(metrics);
		    
		    int aDisplayWidth = getWindowManager().getDefaultDisplay().getWidth();
		    int aDisplayHeight = getWindowManager().getDefaultDisplay().getHeight();
		    Logger.d("Screen Dim w= " + aDisplayWidth + "  h= " + aDisplayHeight + " density " + metrics.densityDpi);
		    Log.d (TAG, "display width " +  aDisplayWidth + " display height " + aDisplayHeight + " density " + metrics.densityDpi);
		    mDataScreenVisible = false;
		    if (extras != null ) {
		    	// we analyse the calling intent if he has info from the startpage to show the data screen
				String dataScreenInfo = extras.getString("datascreen"); 
				Log.d(TAG,"extras " + dataScreenInfo);
				if (dataScreenInfo.equals("true")){
					mDataScreenVisible = true;
				}
		    }
		    if (aDisplayWidth >= 800 && aDisplayHeight >= 480) {
		    	//landscape  mode on a 7 inch display
		    	mShowDataScreenPossible = true;
		    	
		    	Log.d(TAG,"7 inch display with landscape mode");
		    }
		    if (aDisplayWidth >= 480 && aDisplayHeight >= 800) {
		    	//portrait  mode on a 7 inch display
		    	   
		    	mShowDataScreenPossible = true;
		    	Log.d(TAG,"7 inch display with portrait mode");
		    }
		   
		    if (aDisplayWidth >= 1280 && aDisplayHeight >= 752) {
		    	//landscape  mode on a 10 inch display
		    	mShowDataScreenPossible = true;
		    	
		    	Log.d(TAG,"10 inch display with landscape mode");
		    }
		    if (aDisplayWidth >= 800 && aDisplayHeight >= 1232) {
		    	//portrait  mode on a 10 inch display
		    	
		    	mShowDataScreenPossible = true;
		    	
		    	Log.d(TAG,"10 inch display with portrait mode");
		    } 
		   // if (aDisplayWidth >= 1200 && aDisplayHeight >= 1824) {
		    if (metrics.densityDpi == 320) {
		    	// we have a nexus 7 nexus 7
		    	mDisplayFactor = 1.5f;
		    	Log.d(TAG,"nexus 7 with 320 dpi"); 
		    } 
		     
		 // should we present a data screen?  
		    if (mShowDataScreenPossible && mDataScreenVisible) {
		    	setContentView(R.layout.mapplotter_with_info_and_with_snap); 
		    	setupDataScreen();
		    } else {
		    	setContentView(R.layout.mapplotter_with_snap); 
		    }
		    
		   // we have one view as default
		    Log.d(TAG," set the map view");
		    mapView1 = (MapView) findViewById(R.id.mapView);
		     
		   /* We do not use the split screen as it conflicts with the data screen 2013_12_09
		    * 
		    * 
		    * Bundle extras = getIntent().getExtras();
			if (extras != null ) {
				String splitInfo = extras.getString("split");        
				// we analyse the calling intent if he has info from the startpage to split the screen
				if (splitInfo.equals("true")){
					mDataScreenVisible = false;
					LinearLayout linearLayout = (LinearLayout) findViewById(R.id.mapViewMain);
						// if the device orientation is portrait, change the orientation to vertical
					if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
						linearLayout.setOrientation(LinearLayout.VERTICAL);
						mapView1 = (MapView) findViewById(R.id.mapViewLeft);
						mapView2 = (MapView) findViewById(R.id.mapViewRight);
					    MapGenerator aMapGeneratorRight =new BingAerialTileDownloader(getApplicationContext());
			       	    mapView2.setMapGenerator(aMapGeneratorRight);
			       	    mapView2.setClickable(true);
						mapView2.setBuiltInZoomControls(true);
					} else {
					    mapView2 = (MapView) findViewById(R.id.mapViewLeft);
					    MapGenerator aMapGeneratorLeft =new BingAerialTileDownloader(getApplicationContext());
			       	    mapView2.setMapGenerator(aMapGeneratorLeft);
			       	    mapView2.setClickable(true);
						mapView2.setBuiltInZoomControls(true);
					    mapView1 = (MapView) findViewById(R.id.mapViewRight);
					}
				}
			    
			} // extras != null
			*/
			
		    this.snapToLocationView = (ToggleButton) findViewById(R.id.snapToLocationView);
		    
		    this.snapToLocationView.setOnClickListener(new View.OnClickListener() {
				
		    	@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					if (isSnapToLocationEnabled()) {
						disableSnapToLocation(true);
					} else {
						enableSnapToLocation(true);
					}
				}
			}); 
		    this.snapToLocationView.setVisibility(View.VISIBLE);
		    
		    this.mBreakTraceRouteButton = (Button)findViewById(R.id.route_trace_break);
		    this.mBreakTraceRouteButton.setOnClickListener(new View.OnClickListener() {
		    	@Override
				public void onClick(View v) {
					if (mTraceRouteActive) {
						mTraceRouteActive = false;
						mBreakTraceRouteButton.setVisibility(View.INVISIBLE);
					} else {
						;
					}
				}
				
			});
		    this.mBreakTraceRouteButton.setVisibility(View.INVISIBLE);
		    

		    setUpMapGeneratorFromPrefs (); // set the mapgenerator for mapView1 according to the prefs
		    mapView1.setClickable(true);
			mapView1.setBuiltInZoomControls(true);
			mapController = mapView1.getController();
		    Logger.d(TAG,"MapPlotter has started");
		    setCurrentUTCNmeaTime(System.currentTimeMillis());
		    
		    
		    mDbAdapter = new TrackDbAdapter(this);
	        mDbAdapter.open();
	        initialzeAISSymbols();
			
			
			// create the seamarksOverlay 
			// load the seamarks file into aSeamarkOsmObject and create the dictionary
			SeamarkSymbol.preloadFromDefsFile(AISPlotterGlobals.DEFAULT_SEAMARKS_SYMBOL_FILENAME);
			//SeamarkSymbol.preloadFromDefsFile("symbols5.defs");
			mSeamarkOsm = new SeamarkOsm(this);
			mDefaultSeamark = BitmapFactory.decodeResource(getResources(),R.drawable.star); 
			mFillDirectionalSector = false;
			Drawable aDrawable = new BitmapDrawable(getResources(),mDefaultSeamark);
			mSeamarksOverlay = new SeamarksOverlay(aDrawable,this,mSeamarkOsm,mFillDirectionalSector,mDisplayFactor);
			//mSeamarksOverlay = new SeamarksOverlay(aDrawable,this,mSeamarkOsm,mFillDirectionalSector);
			mapView1.getOverlays().add(mSeamarksOverlay); 
			setWayDisplayParams(); // creates and sets mWaysOverlay
			mapView1.getOverlays().add(mWaysOverlay);
			// create an individual marker for an overlay item
			//mAISItemizedOverlay = new AISItemizedOverlay(mShipMarkerBlueFrame, this); 13_03_12
			//mapView1.getOverlays().add(mAISItemizedOverlay);
			mItemizedOverlay = new MyItemizedOverlay(mShipDefaultMarker, this); // this initialized the route too
			mapView1.getOverlays().add(mItemizedOverlay);
			// we use the progress bar in a thread called by iniializeMyShip, so we set it up before
			this.mProgressHorizontal = (ProgressBar) findViewById(R.id.progress_horizontal);
	    	setProgress(mProgressHorizontal.getProgress() * 100);
	    	setSecondaryProgress(mProgressHorizontal.getSecondaryProgress() * 100);
	    	mProgressHorizontal.setVisibility(View.INVISIBLE);
		    setGPSSourceFromPrefs(); 
		    
            mTargetList = NMEATCPServiceFactory.getNMEAVerwaltung(this).getAISTargetList();
      		if (test) {
      			if (mTargetList != null) {
      				Log.d(TAG,"Anzahl " + mTargetList.getSize());
      		    }
      		    else  {
      			Log.d(TAG,"Targetlist ist null");
      		    }
	        }
      		if (mTargetList != null) Logger.d(TAG,"Anzahl der AIS-Ziele" + mTargetList.getSize());
      		else Logger.d(TAG,"Targetlist ist null");
      		
      		firstGPSData = true;
      		initializeMyShip();
      		
      		
      		
      		this.setTitle(R.string.title);
      		
      		
      		
           // Get local Bluetooth adapter
             // mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

            /*  // If the adapter is null, then Bluetooth is not supported
              if (mBluetoothAdapter == null) {
                  Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
                  finish();
                  return;
              }*/
              
              
              
             // on the tablet we may have no build-in gps , so we do not start the service
              
           
            if (savedInstanceState != null && savedInstanceState.getBoolean(BUNDLE_SNAP_TO_LOCATION)) {
				enableSnapToLocation(false);
			}
            this.snapToLocationView.setVisibility(View.VISIBLE);
           
    		  
    		mRouteItemList = new ArrayList<RoutePointItem>();
    		mRouteEditIsDirty = false;
    		setupRouteDisplayParams();
            //mShowSpeedMarkerOfTargets = false;
            showDialog(Show_Loading_Dialog_ID); 
         // we have to wait some time until the map is initialized
            mTargetsOnMapHandler.postDelayed(putTargetsOnMapRun,500);    
            Log.d(TAG,"GPS-source " + mUseGPSSource);
	  }
	  
	  private void setUpMapGeneratorFromPrefs () {
		    MapGenerator aMapGenerator;
			aMapGenerator = new MapnikTileDownloader();
			//mapView1.setMapGenerator(aMapGenerator);
			
		    if (prefs.contains("mapGenerator")) {
				String aMapGeneratorName = prefs.getString("mapGenerator", MapGeneratorInternal.MAPNIK.name());
				Log.d(TAG,"actual mapgenerator " + aMapGeneratorName);
				//boolean persistent = prefs.getBoolean("cachePersistence", false);
				if (aMapGeneratorName.equals("DATABASE_RENDERER")) {
					String pref_mapfile = getResources().getString(R.string.pref_mapfilename);
					String mapfilepath = prefs.getString(pref_mapfile, "");
					showToastOnUiThread("Loading... " + mapfilepath);
					Log.d(TAG,"mapfile " + mapfilepath);
					try {
					
					  mapView1.setMapFile(new File(mapfilepath));
					  setStandardRendererTheme();
					//mapView.setRenderTheme(InternalRenderTheme.OSMARENDER);
					} catch (Exception e) {
						Log.d(TAG, " error setting map file");
						e.printStackTrace();
					}
					
				}
				if (aMapGeneratorName.equals("MAPNIK")) {
		        	aMapGenerator =new MapnikTileDownloader();
		        	 mapView1.setMapGenerator(aMapGenerator);
		        	 
		        }
				if (aMapGeneratorName.equals("OPENCYCLEMAP")) {
		        	aMapGenerator =new OpenCycleMapTileDownloader();
		        	mapView1.setMapGenerator(aMapGenerator);
		        	 
		        }
				if (aMapGeneratorName.equals("OPENSTREETMAP")) {
		        	Context myContext = getApplicationContext();
		        	int aTimeoutForDownload = 5000;
		        	aMapGenerator = new OsmTileDownloader(myContext,aTimeoutForDownload);
		        	mapView1.setMapGenerator(aMapGenerator);
		        	 
		        }
		        if (aMapGeneratorName.equals("OPENSEAMAP")) {
		        	Context myContext = getApplicationContext();
		        	int aTimeoutForDownload = 5000;
		        	aMapGenerator = new OpenSeamapTileAndSeamarksDownloader(myContext,aTimeoutForDownload);
		        	mapView1.setMapGenerator(aMapGenerator);
		        	 
		        }
		        if (aMapGeneratorName.equals("ENIROMAP")) {
		        	Context myContext = getApplicationContext();
		        	int aTimeoutForDownload = 5000;
		        	aMapGenerator = new EniroMapTileDownloader(myContext,aTimeoutForDownload);
		        	mapView1.setMapGenerator(aMapGenerator);
		        	 
		        }
		        if (aMapGeneratorName.equals("ENIRONAUTICAL")) {
		        	Context myContext = getApplicationContext();
		        	int aTimeoutForDownload = 5000;
		        	aMapGenerator = new EniroNauticalTileDownloader(myContext,aTimeoutForDownload);
		        	mapView1.setMapGenerator(aMapGenerator);
		        	
		        }
		        
		        if (aMapGeneratorName.equals("ENIROAERIAL")) {
		        	Context myContext = getApplicationContext();
		        	int aTimeoutForDownload = 5000;
		        	aMapGenerator = new EniroAerialTileDownloader(myContext,aTimeoutForDownload);
		        	mapView1.setMapGenerator(aMapGenerator);
		        	
		        }
		        if (aMapGeneratorName.equals("ENIROAERIALWITHSEAMARKS")) {
		        	Context myContext = getApplicationContext();
		        	int aTimeoutForDownload = 5000;
		        	aMapGenerator = new EniroAerialAndSeamarksTileDownloader(myContext,aTimeoutForDownload);
		        	mapView1.setMapGenerator(aMapGenerator);
		        	
		        }
		    }
		    
	  }
	  
	  
	  private void initialzeAISSymbols() {
		    mShipDefaultMarker = getResources().getDrawable(R.drawable.ship_blue_frame_small);
			
			mShipBlueSmallBitmap = BitmapFactory.decodeResource(getResources(),R.drawable.ship_middle_blue); 
			mShipBlueSmallFrameBitmap = BitmapFactory.decodeResource(getResources(),R.drawable.ship_middle_blue_frame);
			mShipBlueInactiveSmallFrameBitmap = BitmapFactory.decodeResource(getResources(),R.drawable.ship_middle_inactive_blue_frame);
			
			mShipRedSmallBitmap = BitmapFactory.decodeResource(getResources(),R.drawable.ship_middle_red); 
			mShipRedSmallFrameBitmap = BitmapFactory.decodeResource(getResources(),R.drawable.ship_middle_red_frame);
			mShipRedInactiveSmallFrameBitmap = BitmapFactory.decodeResource(getResources(),R.drawable.ship_middle_inactive_red_frame);
			
			
			mBaseStationMarker = getResources().getDrawable(R.drawable.basestation);
			mOwnShipMarkerBitMap = BitmapFactory.decodeResource(getResources(),R.drawable.ship_green_long); 
			
			mAIS_SART_Drawable = getResources().getDrawable(R.drawable.ais_sart_symbol96); 
			mAIS_SART_TEST_Drawable = getResources().getDrawable(R.drawable.ais_sart_test96b);
	  }
	  
	  
	  private void setStandardRendererTheme() {
			File cardDirectory = Environment2.getCardDirectory();
			String aPath = cardDirectory.getAbsolutePath();
	        String aRenderThemeFilename =aPath +"/"+ AISPlotterGlobals.DEFAULT_APP_DATA_DIRECTORY + "/" + AISPlotterGlobals.DEFAULT_STANDRAD_RENDERER_FILENAME;
	        //mRenderThemeName = aRenderThemeFilename;
			File aFile = new File(aRenderThemeFilename);
			try {
			this.mapView1.setRenderTheme(aFile);
			} catch (IOException e) {
				String aMsg = "Standard RenderTheme not found" + e.toString();
				//String aMsg = getResources().getString(R.string.osmviewer_start_stdrender_not_found);
				//mTextViewCenterCoordinates.append(aMsg);
			    this.showToastOnUiThread(aMsg);
				Log.d(TAG, e.toString());
			}
		}
	  
	  /**
	   * 12_08_03 not used
	   * @param degrees
	   * @return
	   */
	  public ShapeDrawable makeTriangleSymbol ( float degrees) {
			Path path = new Path();
			// we paint a triangle with a little cross relative to (0,0);
			float size = 1;
			path.moveTo(0,-2);
			path.lineTo(0,+2);
			path.moveTo(-2,0);
			path.lineTo(+2,0);
			path.moveTo(0,-30* size);          // top
			path.lineTo(-10* size,+10*size);   // bottom left
			path.lineTo(10*size,10*size);      // bottom right
			path.lineTo(0,-30*size);           // and back to the top
			path.close();
			Matrix aMatrix = new Matrix();
			aMatrix.setRotate(degrees);
			path.transform(aMatrix);
			float aStandardWidth = 40* size;
			float aStandardHeight = 40* size;
			PathShape pathShape = new PathShape(path,aStandardWidth,aStandardHeight);
			ShapeDrawable aSymbol = new ShapeDrawable(pathShape);
			aSymbol.setIntrinsicHeight((int)(40*size));
			aSymbol.setIntrinsicWidth((int)(40*size));
			//aSymbol.setBounds(-10, -30, 10, 10);
			Paint paint = aSymbol.getPaint();
			paint.setColor(Color.BLUE);
	   	    paint.setStyle(Paint.Style.FILL);
	   	    paint.setStrokeWidth(2);
	   	    aSymbol.setAlpha(255);
			return aSymbol;
		}
	  
	  /**
	   * if we put the ais targets on the Overla y we must do this in a delayed thread
	   */
	  private static Handler mTargetsOnMapHandler = new Handler();
	  
	  /**
	   * putTargetsOnMapRun puts all the targets on the Overlay when the activity is started
	   * it reads the target list and sets the appropriate symbol according to
	   * the displayStatus of the target
	   * handled:
	   * AISPlotterGlobals.DISPLAYSTATUS_HAS_POSITION
	   * AISPlotterGlobals.DISPLAYSTATUS_HAS_SHIPREPORT
	   * AISPlotterGlobals.DISPLAYSTATUS_BASE_STATION
	   * 
	   */
	  private Runnable putTargetsOnMapRun = new Runnable() {
		    public void run() {
		    	
				Matrix rotationMatrix = new Matrix(); 
				long oldTime = System.currentTimeMillis(); 
				
		    	if (mTargetList !=null) { 
		    		  int nrOfTargets = mTargetList.getSize();
		    		  mProgressDialog.setSecondaryProgress(nrOfTargets);
		    		  AISOverlayItem aNewItem = null;
					  for ( int index = 0; index < nrOfTargets;index++){
						  aNewItem = null;
						  AISTarget aTarget = mTargetList.getTargetByNr(index);
						  boolean hasTrack = aTarget.getHasTrack();
						  boolean isAIS_SART = false;
				    	  double aLAT = aTarget.getLAT(); 
				    	  double aLON = aTarget.getLON();
				    	  String aName = "---";
				    	  if (!aTarget.getShipname().equals("")) {
				    		  aName = aTarget.getShipname();
				    	  }
				    	  String aMMSIStr  = aTarget.getMMSIString();
				    	  if (aMMSIStr.startsWith("970")) {
				    		  // we found AIS-SART
				    		  Log.d(TAG,"AIS-SART found "+ aMMSIStr);
				    		  isAIS_SART = true;
				    		  
				    	  }
				    	  String aCOGStr = aTarget.getCOGString();
				    	  String aSOGStr = aTarget.getSOGString();
				    	  long aUTCDiff = System.currentTimeMillis() - aTarget.getTimeOfLastPositionReport();
				    	  String aTimeStr =PositionTools.getTimeString(aUTCDiff);
				    	  int aDisplayStatus = aTarget.getStatusToDisplay();
				    	  String aInfoStr = getResources().getString(R.string.aisItem_info1);
				    	  String aInfo = aName + "\n" + "LAT: " + PositionTools.getLATString(aLAT) 
				    	                       + "\nLON: " + PositionTools.getLONString(aLON) 
				    	                       + "\nCOG " + aCOGStr 
				    	                       + "\nSOG " + aSOGStr
				    	                       + "\n" + aInfoStr + " " +aTimeStr;
				    	  if (test)Log.d(TAG,"Ship " + aInfo);
				    	  GeoPoint geoPoint = new GeoPoint(aLAT,aLON); 
				    	  Drawable aAIS_SART_Drawable = mAIS_SART_Drawable;
				    	  if (isAIS_SART){ 
				    		  byte aNavStatus = aTarget.getNavStatus();
				    		  if (aTarget.getNavStatus() == 15){
				    			  // this is a AIS-SART Test
				    			  aAIS_SART_Drawable = mAIS_SART_TEST_Drawable;
				    		  }
				    	      
				    	  }
				    	  if (aDisplayStatus == AISPlotterGlobals.DISPLAYSTATUS_HAS_POSITION) {
				    		 
							  if (isAIS_SART){
					    	      aNewItem = new AISOverlayItem(geoPoint, aMMSIStr,aInfo,
					    	    		  ItemizedOverlay.boundCenterBottom(aAIS_SART_Drawable),hasTrack); 
					    	      mItemizedOverlay.addItem(aNewItem);
							  }else {
								  float grad = ((float) aTarget.getCOG()) / 10f;
								  rotationMatrix.setRotate(grad); 
								  Bitmap rotatedbitmap = Bitmap.createBitmap(mShipBlueSmallFrameBitmap, 0, 0, 
										  mShipBlueSmallFrameBitmap.getWidth(), mShipBlueSmallFrameBitmap.getHeight(),
											rotationMatrix, true); 
								  BitmapDrawable bmd = new BitmapDrawable(rotatedbitmap);
					    		  
					    	      aNewItem = new AISOverlayItem(geoPoint, aMMSIStr,aInfo,
					    	    		  ItemizedOverlay.boundCenterBottom(bmd),hasTrack);
					    		  /*Drawable aDrawable = makeTriangleSymbol(grad);
					    		  OverlayItem aNewItem = new OverlayItem(geoPoint, aMMSIStr,aInfo,
					    	    		  ItemizedOverlay.boundCenterBottom(aDrawable));*/
					    	   
					    	      // mAISItemizedOverlay.addItem(aNewItem); 13_03_12 // default is frame
					    	      mItemizedOverlay.addItem(aNewItem);
							  }
				    	  }
				    	  if (aDisplayStatus ==AISPlotterGlobals.DISPLAYSTATUS_HAS_SHIPREPORT) {
				    		  float grad = ((float) aTarget.getCOG()) / 10f;
							  rotationMatrix.setRotate(grad); 
							  Bitmap rotatedbitmap = Bitmap.createBitmap(mShipBlueSmallBitmap, 0, 0, 
										mShipBlueSmallBitmap.getWidth(), mShipBlueSmallBitmap.getHeight(),
										rotationMatrix, true); 
							  BitmapDrawable bmd = new BitmapDrawable(rotatedbitmap);
				    	      aNewItem = new AISOverlayItem(geoPoint, aMMSIStr,
				  					aInfo,
									ItemizedOverlay.boundCenterBottom(bmd),hasTrack);
				    	   // mAISItemizedOverlay.addItem(aNewItem); 13_03_12 // default is frame
				    	      mItemizedOverlay.addItem(aNewItem);
				    	    
				    	  }
				    	  if (aDisplayStatus == AISPlotterGlobals.DISPLAYSTATUS_BASE_STATION) {
				    	      aNewItem = new AISOverlayItem(geoPoint, aMMSIStr,
				  					aInfo,
									ItemizedOverlay.boundCenterBottom(mBaseStationMarker),hasTrack);
				    	   // mAISItemizedOverlay.addItem(aNewItem); 13_03_12 // default is frame
				    	      mItemizedOverlay.addItem(aNewItem);
				    	  }
				    	  if (aNewItem != null){
				    		  if (hasTrack) {
					    		  if (test) Log.d(TAG,"put targets on map:  found with track " + aMMSIStr + " " +aName );
								  restoreTrackInInitPhase(aMMSIStr,aNewItem);
							  }  
				    	  }
				    	  if (aDisplayStatus ==AISPlotterGlobals.DISPLAYSTATUS_SELECTED) {
				    		  if (isAIS_SART){
					    	      aNewItem = new AISOverlayItem(geoPoint, aMMSIStr,aInfo,
					    	    		  ItemizedOverlay.boundCenterBottom(aAIS_SART_Drawable),hasTrack); 
					    	      mItemizedOverlay.addItem(aNewItem);
							  }else {
					    		  float grad = ((float) aTarget.getCOG()) / 10f;
								  rotationMatrix.setRotate(grad); 
								  Bitmap rotatedbitmap = Bitmap.createBitmap(mShipRedSmallBitmap, 0, 0, 
											mShipRedSmallBitmap.getWidth(), mShipRedSmallBitmap.getHeight(),
											rotationMatrix, true); 
								  BitmapDrawable bmd = new BitmapDrawable(rotatedbitmap);
					    	      aNewItem = new AISOverlayItem(geoPoint, aMMSIStr,
					  					aInfo,
										ItemizedOverlay.boundCenterBottom(bmd),hasTrack);
					    	   // mAISItemizedOverlay.addItem(aNewItem); 13_03_12 // default is frame
					    	      mItemizedOverlay.addItem(aNewItem);
							  }
				    	  }
				    	  
				    	  mProgressStatus = index;
				    	  // wird nicht aktualisiert 12_02_29 13:30
				    	  /*Log.d(TAG,"progress init " + mProgressStatus);
				    	  if (mProgressDialog != null ) {
                         	 mProgressDialog.setProgress(mProgressStatus);
                         	 mProgressDialog.show();
                         	 Log.d(TAG,"progress execute " + mProgressStatus);
                          }
				    	  try {
	                           Thread.sleep(10);
		                     } 
		                     catch ( Exception e) {
		                    	 e.printStackTrace();
		                     }*/
				    	  /*mProgressHandler.post(new Runnable() {
		                         public void run() {
		                             if (mProgressDialog != null ) {
		                            	 mProgressDialog.setProgress(mProgressStatus);
		                            	 Log.d(TAG,"progress execute" + mProgressStatus);
		                             }
		                         }
		                     });*/
				    	  //mapView.invalidate();
					  } // for
					  
					  mapView1.invalidate();
				  } // if 
		    	long newTime = System.currentTimeMillis();
		    	long diffTime = newTime - oldTime;
		    	String aTimeToCreate = getResources().getString(R.string.guiupdate_info_time_to_create_targets );
		    	String info = aTimeToCreate + " " + mTargetList.getSize() +" targets " + diffTime + " ms";
		    	if (test) Log.d(TAG,"create items in map "+ info);
		    	Logger.d(TAG,info);
		    	dismissDialog(Show_Loading_Dialog_ID);
		    	Toast toast = Toast.makeText(getApplicationContext(), info, Toast.LENGTH_LONG);
				toast.show();
		    	
		    } // run
		  }; // new Runnable
		  
	 
	  
	  
	  /**
	   * this called from the gps service to update the ship position
	   * LAT, LON,COG and SOG are set in myShip
	   */
	  public void setPositionFromInternalGPS(Location aLocation) {
		  // this wil never be called on a tablet without gps
		  // Location.getLatitude .. in Grad
		  // myLat, myLon in Micrograd
		    int myLat = (int)(aLocation.getLatitude()*1E6);
	        int myLon = (int)(aLocation.getLongitude()*1E6);
	     
	       // long aUTCTime = aLocation.getTime(); 
	        // there is a problem with the difference of time from location (gps) and Systtem.currentimeMillis() which is used elsewhere
	        long aUTCTime = System.currentTimeMillis(); // since 13_10_01
	        float aBearing = aLocation.getBearing();
	        float aSpeed = aLocation.getSpeed();
	        if (test) Log.d(TAG,"Handler -> got new utc-time " + aUTCTime);
	        //setCurrentUTCNmeaTime(aUTCTime);
	        GeoPoint geoPoint = new GeoPoint(myLat,myLon);
	        String newPosStr = PositionTools.getTimeString(aUTCTime)+ " : new Pos from int. GPS: LAT " + myLat + " LON " + myLon + " speed " + aSpeed +   " bearing " + aBearing;
	        if (test) Log.d(TAG, "Handler->got new position " + newPosStr);
	        if (mLogPositionData) Logger.d(TAG,newPosStr);
	        /*setMyLATPos(myLat);
	        setMyLONPos(myLon);*/
	        //setMyLATPos(aLocation.getLatitude());
	        myShip.setLAT(aLocation.getLatitude());
	        //setMyLONPos(aLocation.getLongitude());
	        myShip.setLON(aLocation.getLongitude());
	        myShip.setSpeedInMetersPerSecond(aSpeed);
	        myShip.setCOGInDegrees(aBearing);
	        myShip.setTimeOfLastPositionReport(aUTCTime);
	        String newPosition = PositionTools.getTimeString(aUTCTime)+" : myShip new position" 
	                             + " LAT " + myShip.getLATString() 
	                             + " LON " + myShip.getLONString();
	        if (mLogPositionData) Logger.d(TAG,newPosition);
	        if (test) Log.d(TAG, newPosition);
	        String newSOGAndCOG = PositionTools.getTimeString(aUTCTime)+" : myShip SOG " + myShip.getSOGString() 
                                   + " COG " + myShip.getCOGString();
	        if (mLogPositionData)Logger.d(TAG,newSOGAndCOG);
	        if (test) Log.d(TAG,newSOGAndCOG );
	        
	        lastShipPoint = geoPoint;
	        int aDisplayStatus = AISPlotterGlobals.DISPLAYSTATUS_OWN_SHIP;
	        boolean inInitPhase;
	        if (firstGPSData) {
	        	inInitPhase = true;
	        	firstGPSData = false;
	        	Logger.d(TAG,"initphase finished");	
	        } else {
	        	inInitPhase = false;
	        }
	        boolean hasTrack = myShip.getHasTrack();
	        if (hasTrack){
	        	// we must update the tracktable to myShip
	        	String aMMSI = myShip.getMMSIString();
	        	if (!mDbAdapter.isTableToMMSIInShipTrackList(aMMSI)){
	        		mDbAdapter.createShipTrackTable(aMMSI);
	        	}
	        	
        		if (!inInitPhase) {
        			String aLAT = Double.toString(myShip.getLAT());
        			String aLON = Double.toString(myShip.getLON());
        			long aUTC = System.currentTimeMillis();
        			mDbAdapter.insertTrackPointToTable(aMMSI, aLAT, aLON, aUTC);
        		}
	        }
	       
	       
			updateTargetOnOverlay(myShip.getMMSIString(),myShip.getShipname(),aDisplayStatus,
					              myShip.getLAT(), myShip.getLON(),myShip.getCOG(),
					              myShip.getSOGString(),myShip.getCOGString(),aUTCTime,hasTrack,
					              myShip.getManueverStatus(),myShip.getNavStatus(),myShip.getLength(),myShip.getWidth(),inInitPhase);
			displayShipdataOnDataScreen( myShip.getLAT(), myShip.getLON(),
					                     "--","--", 
					                     myShip.getSOGString(),myShip.getCOGString(),
					                     "--");	
             
	  }
	  
	  public void displayShipdataOnDataScreen(double pLAT, double pLON, 
			                                  String pWindSpeedStr , String pWindAngleStr,
			                                  String pSOGStr, String pCOGStr,
			                                  String pDepthStr ) {
		     String theCurrentTitle = (String) getTitle();
			 
			 setTitle (theCurrentTitle + "  Depth: " + pDepthStr + " m    Wind Speed: " + pWindSpeedStr + " kn");
			 if(mShowDataScreenPossible && mDataScreenVisible) {   
				 // we have a visible data screen and display the data here
				 
					mOSM_info_LAT.setText(PositionTools.getLATString(pLAT));
					mOSM_info_LON.setText(PositionTools.getLONString(pLON));
					mOSM_info_Windspeed.setText(pWindSpeedStr);
					mOSM_info_Winddirection.setText(pWindAngleStr);
					mOSM_info_Depth.setText(pDepthStr);
					mOSM_info_SOG.setText(pSOGStr);   
					mOSM_info_COG.setText(pCOGStr);	   
				 
			 }
	  }
	  
	  
	 /* // we call this from the NMEA-GPS-UpdateEvent
	   public void setMyLATPos(double aPos) {
	 	 // aPos measured in Grad
	  	if (test) Log.v(TAG,"Lat changed " + aPos);
	  	myShip.setLAT(aPos);
	  	lastShipPoint =  new GeoPoint(aPos,myShip.getLON());
	  	//Log.v(TAG,"LAT " + myShip.getLAT());
	  }
	  
	  public void setMyLONPos (double aPos){
	 	// aPos measured in Grad
	  	Log.v(TAG,"LON changed "+ aPos);
	  	myShip.setLON(aPos);
	  	lastShipPoint = new GeoPoint(myShip.getLAT(),aPos);
	  	//Log.v(TAG,"LON " + myShip.getLON());
	  	
	  }*/
	  
	 /* public double getMyLatPos(){
	 	//  measured in Grad
	  	return myShip.getLAT();
	  }*/
	  
	 /* public double getMyLONPos(){
	 	//  measured in Grad
	  	return myShip.getLON();
	  }*/
	  
	  // will never be called on a tablet without gps
	  private final Handler uiGPSThreadCallbackHandler = new Handler() {    
		    @Override
		    public void handleMessage(Message msg) {
		      super.handleMessage(msg); 
		      if (test) Log.d(TAG, "Handler->handleMessage(): entered...");
		      Bundle bundle = msg.getData();
		      if (bundle != null) {
		        Location location = (Location)bundle.get("location");
		        setPositionFromInternalGPS(location);              
		      }  
		    } 
		  }; 
		  
		  /**
		   * Baut eine Verbindung zum lokalen Service auf. Der Service laeuft im 
		   * gleichen Prozess wie diese Activity. Daher wird er automatisch beendet,
		   * wenn der Prozess der Activity beendet wird.
		   */
		  private ServiceConnection localGPSServiceConnection = new ServiceConnection() {
		    // Wird aufgerufen, sobald die Verbindung zum lokalen Service steht. 
		    public void onServiceConnected(ComponentName className, IBinder binder) {        
		      Log.d(TAG, "AISTCPMapPlotter->onServiceConnected(): entered..."); 
		      gpsLocalServiceBinder = (GpsLocationServiceLocal.GpsLocalBinder)binder;
		      gpsLocalService = gpsLocalServiceBinder.getService();
		      ((GpsLocationServiceLocal.GpsLocalBinder)binder).setCallbackHandler(uiGPSThreadCallbackHandler);            
		    }

		    // Wird aufgerufen, sobald die Verbindung zum Service unterbrochen wird. 
		    // Dies passiert nur, wenn der Prozess, er den Service gestartet hat, stirbt.
		    // Da dies ein lokaler Service ist, läuft er im selben Prozess wie diese Activity.
		    // Daher kann die Methode niemals aufgrufen werden und muss nicht implementiert
		    // werden.
		    public void onServiceDisconnected(ComponentName className) {        
		      // unerreichbar...
		    }
		  };
		  
		  private void verbindeMitGPSService() {
				Log.d(TAG, "HelloGPS->verbindeMitService(): entered..."); 
				Intent intent = new Intent(getApplicationContext(),GpsLocationServiceLocal.class);
				bindService(intent, localGPSServiceConnection,Context.BIND_AUTO_CREATE);
			}
			
			public Location ermittlePosition() {
				return gpsLocalServiceBinder.getGpsData();
			}
				  
		 /**
		  * aDAndMStr has the format for example: 52' 55,672 N or 05' 24,345 E
		  * This is the format used in the prefs,which will be shown to the user
		  * @param aDAndMStr
		  * @return aString in microdegrees
		  */
			private  String degreeAndMinStrToMicroDegrees(String aDAndMStr){
				// aDAndMStr has the format for example: 52' 55,672 N or 05' 24,345 E
				// This is the format used in the prefs,which will be shown to the user
				String aResult = "00000000";
				String aDegreeStr = "000";
				String aMinuteStr = "00.000";
				String aMark = "'";
				int aPos = -2;
				try {
				  aPos = aDAndMStr.indexOf(aMark, 0);
				} catch (NullPointerException e) {
					
				};
				if ( aPos > 0) {
					aDegreeStr = aDAndMStr.substring(0,aPos  );
					aDAndMStr = aDAndMStr.substring(aPos+1);
					while (aDAndMStr.indexOf(" ", 0) == 0) {
						aDAndMStr = aDAndMStr.substring(1);
					}
					aPos = aDAndMStr.indexOf(" ", 0);
					aMinuteStr =  aDAndMStr.substring(0,aPos  );
					aDAndMStr = aDAndMStr.substring(aPos+1);
					aPos = aMinuteStr.indexOf(",", 0);
					if (aPos > 0 ){
						aMinuteStr = aMinuteStr.replace(",", ".");
					}
					double thePosition = 54.0f;
					try {
						thePosition = Double.valueOf(aDegreeStr);
						thePosition = thePosition + (Double.valueOf(aMinuteStr) / 60);
						thePosition = thePosition * 1E6;
						int thePositionInMicrograd = (int) thePosition;
						if (aDAndMStr.contains("S") || aDAndMStr.contains("W")){
							thePositionInMicrograd = - thePositionInMicrograd;
						}
						aResult = Integer.toString(thePositionInMicrograd);
					}
					catch (Exception e) {
						Logger.d(TAG," no valid pos " + aDAndMStr);
						Toast toast = Toast.makeText(getApplicationContext(), "no valid pos " + aDAndMStr, Toast.LENGTH_LONG);
						toast.show();
				 	}
				}
				
				return aResult;
			}
	    /**
	     * basic initialisation for own ship
	     * has settings if the app is new, ship is fixed to burgtiefe
	     * 
	     */
	    public void initializeMyShip(){
	    	 double myLATPos = 54.0 + 5.672 /60; // 54 Grad 05.672
	    	 double myLONPos = 10.0 + 47.833 /60;  // 10 Grad 47.833
	    	 int aSOG = 0;
	    	 Log.d(TAG,"LAT " + myLATPos + " LON " + myLONPos);
	    	 String prev_LAT_key = getResources().getString(R.string.pref_OwnShipLAT);
		 	 String aLATStr = prefs.getString(prev_LAT_key, DEFAULT_SHIP_LAT);  //  "54415700" Burgtiefe
	    	 String prev_LON_key = getResources().getString(R.string.pref_OwnShipLON);  // "11194900"
		 	 String aLONStr = prefs.getString(prev_LON_key, DEFAULT_SHIP_LON);
		 	 String prev_alarmradius_key = getResources().getString(R.string.pref_alarmradius);
	 		 String alarmRadiusStr = prefs.getString(prev_alarmradius_key, DEFAULT_ALARM_RADIUS_STRING);
		 	 try {
		 	   aLATStr = degreeAndMinStrToMicroDegrees(aLATStr);
		 	   aLONStr = degreeAndMinStrToMicroDegrees(aLONStr);
	    	   myLONPos = Double.valueOf(aLONStr) / 1E6;
	    	   myLATPos = Double.valueOf(aLATStr) / 1E6;
	    	   //aSOG = (int)(Float.valueOf(alarmRadiusStr)*10);
		 	 }
		 	 catch (Exception e) {
		 		Logger.d(TAG, " could not set the inital ship position (LAT/LON) " + aLATStr + " "+  aLONStr);
		 		
		 	 }
		 	 Logger.d(TAG,"LAT " + myLATPos + " LON " + myLONPos);
	    	 long  myMMSI = AISPlotterGlobals.myShipMMSI;
	    	 long id = myMMSI;
	    	 
	    	 
	    	 int aHDG = 0;
	    	 int aCOG = 0;
	    	 String aName = "myShip";
	 		 myShip = new AISTarget(id,myMMSI,myLONPos, myLATPos,aName,aSOG,aCOG,aHDG);
	 		 boolean hasTrack = mDbAdapter.isTableToMMSIInShipTrackList(myShip.getMMSIString());
	 		 myShip.setHasTrack(hasTrack);
	 		 /* is done in updatetargetOnOverlay
	 		  * if (hasTrack){ 
	 			setTrackInInitPhase(myShip.getMMSIString()); 
	 		 }*/
	 		 myShip.setStatusToDisplay(AISPlotterGlobals.DISPLAYSTATUS_HAS_SHIPREPORT);
	 		 lastShipPoint = new GeoPoint((int)(myShip.getLAT()*1E6),(int)(myShip.getLON()*1E6));
	 		 Log.d(TAG,"initialize my ship");
	 		 Log.d(TAG,"LAT " + lastShipPoint.latitudeE6 + " LON " + lastShipPoint.longitudeE6);
	 		 // if we have gps data via nmea we ask the nmea service
	 		if ( mUseGPSSource == 2){
	 		 Intent aGPSNMEAAcquireIntent = new Intent(AISPlotterGlobals.ACTION_SENDGPSNMEADATA);
	 		 //aGPSNMEAAcquireIntent.setAction("SEND_NEW_GPSDATA");
	 		 sendBroadcast(aGPSNMEAAcquireIntent);
	 		 Logger.d(TAG,"Send new gps data requested");
	 		 Log.d(TAG,"Send new gps data requested");
	 		}
	 		
	 		if (mUseGPSSource < 2) {
	 			// simulate a Location
	 			Location aLocation = new Location("gps");
	 			aLocation.setBearing(0.0f);
	 			aLocation.setLatitude(myLATPos);
	 			aLocation.setLongitude(myLONPos);
	 			aLocation.setSpeed(0.0f);
	 			aLocation.setTime(System.currentTimeMillis());
	 			setPositionFromInternalGPS(aLocation);
	 		}
	    }
	    
	    
	     public AISTarget getMyShip() {
	    	 return myShip;
	     }
	     
	     public void setAISWarningDistance(float aRadius){
	    	 mAISAlarmRadius = aRadius;
	     }
	     
	     public float getAISWarningDistance() {
	    	 return mAISAlarmRadius;
	     }
	     
	     
	     protected void onSaveInstanceState(Bundle outState) {
	 		super.onSaveInstanceState(outState);
	 		outState.putBoolean(BUNDLE_SNAP_TO_LOCATION, this.snapToLocation);
	     }
	     
	     @Override
	     public void onPause() {
	    	if (mTraceRouteActive) {
	    		mTraceRouteActive = false;
	    		mCenterMapToTracePointHandler.removeCallbacks(centerMapToTracePointRunnable);
	    	}
	    	
	     	if (this.wakeLock.isHeld()) {
				this.wakeLock.release();
			}
	     	if (test) Log.e(TAG, "onPause entered");
	     	// not in use 12_02_24
	     	// mHandler.removeCallbacks(timerTaskUpdateDisplay);
	     	mAISOverlayRefreshhandler.removeCallbacks(overlayRefresh);
	     	// keep the last mapcenter in prefs
	     	GeoPoint aGp = mapView1.getMapPosition().getMapCenter();
	     	float aZoomFactor = mapView1.getMapPosition().getZoomLevel();
	     	int aGoogleLAT = (int) aGp.latitudeE6; // we keep the prefs in Micrograd
		    int aGoogleLON = (int) aGp.longitudeE6;
		    prefs.edit()
		     	.putInt(AISPlotterGlobals.PREV_LAST_GEOPOINT_LAT, aGoogleLAT)
		     	.putInt(AISPlotterGlobals.PREV_LAST_GEOPOINT_LON, aGoogleLON)
		     	.putFloat(AISPlotterGlobals.PREF_ZOOM_FACTOR,aZoomFactor)
		     	.commit();
		    /* this blocks the pausing, app crashes, we must do it in another way
		     * if (mRouteEditIsDirty) {
		    	// we edited the routelist but did not finish or save it;
		    	writeRouteListToDatabase();
		    	mRouteEditIsDirty= true;
		    }*/
		    super.onPause();
	     }
	     
	     @Override
	     public void onResume() {
	     	super.onResume();
	     	if (test) Log.e(TAG, "onResume entered");
	     	if ( !this.wakeLock.isHeld()) {
				this.wakeLock.acquire();
			}
	     	MapScaleBar mapScaleBar = this.mapView1.getMapScaleBar();
	     	mapScaleBar.setShowMapScaleBar(true);
	     	mapScaleBar.setImperialUnits(false);
	     	prefs = PreferenceManager.getDefaultSharedPreferences(this);
	     	boolean persistent = prefs.getBoolean("cachePersistence", false);
	 		String aBigCacheMsg1 = getResources().getString(R.string.mapcache_handling_msg_Big_cache_1);
			String aBigCacheMsg2 = getResources().getString(R.string.mapcache_handling_msg_Big_cache_2);
			String aBigCacheMsg3 = getResources().getString(R.string.mapcache_handling_msg_Big_cache_3);
			TileCache fileSystemTileCache = this.mapView1.getFileSystemTileCache();
			if (persistent) {
			   fileSystemTileCache.setPersistent(persistent);
			   fileSystemTileCache.setCapacity(TILE_CACHE_MAX_CAPACITY);
			   int aViewID = 1;
			   
			   int aCacheSize = PositionTools.getTileCacheSize(aViewID);
			   if (aCacheSize > 300) {
				   showToastOnUiThread(aBigCacheMsg1+ " "  + aCacheSize + " " + aBigCacheMsg2);
			   }
			} else {
				 fileSystemTileCache.setPersistent(persistent);
				 fileSystemTileCache.destroy();
				 //showToastOnUiThread(aBigCacheMsg3);
			}
			mLogPositionData = prefs.getBoolean(Logger.KEY_IS_AISMSG_LOG_ENABLED, true);
  		    int lastGeopointLAT = prefs.getInt(AISPlotterGlobals.PREV_LAST_GEOPOINT_LAT, DEFAULT_LAST_GEOPOINT_LAT);
  		    int lastGeopointLON = prefs.getInt(AISPlotterGlobals.PREV_LAST_GEOPOINT_LON, DEFAULT_LAST_GEOPOINT_LON);
  		    if (test) Log.d(TAG,"on resume read back LAT " + lastGeopointLAT + " LON " + lastGeopointLON);
  		    GeoPoint aGp = new GeoPoint(lastGeopointLAT,lastGeopointLON);
  		    mLastMapCenterPoint = aGp;
    		mapController.setCenter(mLastMapCenterPoint);
	 		if (test) Log.d(TAG,"set lastMapCenterPoint to LAT:" + mLastMapCenterPoint.getLatitude() + " LON: " + mLastMapCenterPoint.getLongitude());
	 		float aZoomFactor =prefs.getFloat(AISPlotterGlobals.PREF_ZOOM_FACTOR, DEFAULT_ZOOM_LEVEL);
	 		int aZoomLevel = (int)aZoomFactor;
	 		
	 		String prev_alarmradius_key = getResources().getString(R.string.pref_alarmradius);
	 		String alarmRadiusStr = prefs.getString(prev_alarmradius_key, DEFAULT_ALARM_RADIUS_STRING);
	 		setAISWarningDistance(Float.valueOf(alarmRadiusStr));
	 		
	 		mapController.setZoom(aZoomLevel);
	 		// in google: mapController.animateTo(lastMapCenterPoint);
	 		mapController.setCenter(mLastMapCenterPoint);
	 		//Location aLocation = ermittlePosition();
	 		//setPositionAndUTCTime(aLocation);
	 		mAISOverlayRefreshhandler.postDelayed(overlayRefresh,updateDisplayIntervall); 
	 		//setAISWayDisplayParams(); 13_03_16
	 		mShowRoutePoints = true;
	 		mTracelist = new ArrayList<GeoPoint>();
	 		mProgressHorizontal.setVisibility(View.INVISIBLE);
	 		try {
				restoreRouteFromDatabase(AISPlotterGlobals.DEFAULTROUTE);
			} catch (Exception e) {
				Log.d(TAG, "route restore exception found" + e.toString());
			}
	 		if (!this.mapView1.getMapGenerator().requiresInternetConnection() && this.mapView1.getMapFile() == null) {
				startMapFilePicker();
			}
	 		if (!this.mapView1.getMapGenerator().requiresInternetConnection() && this.mapView1.getMapFile() != null ) {
	 			mSeamarksOverlay.updateSeamarksFile();
	 		}
	     }
	     
	     
	     @Override
	     
	     public void onStart() {
	    	 super.onStart();
	    	
	    	 Logger.d(TAG,"NMEA Data Receiver started");
	    	 IntentFilter aNMEAParserIntentFilter = new IntentFilter();
	    	 aNMEAParserIntentFilter.addAction(AISPlotterGlobals.ACTION_GPSNMEADATA);
	    	 aNMEAParserIntentFilter.addAction(AISPlotterGlobals.ACTION_UPDATEGUI);
	    	 aNMEAParserIntentFilter.addAction(AISPlotterGlobals.ACTION_DELETE_TARGET);
	    	 registerReceiver(mNMEAParserDataReceiver, aNMEAParserIntentFilter); 
	    	 mRefreshTargetListHandler.postDelayed(timertaskRefreshTargetList,sixMinutes); 
		     mTargetListLastUpdateTime = System.currentTimeMillis();
	     }
	     
	     
	     @Override
	     public void onStop() {
	     	super.onStop();
	     	if (test) Log.d(TAG,"onStop entered");
	     	
	     	//float aZoomFactor = mapView.getZoomLevel(); mapsforge 0.2.4
	     	float aZoomFactor = mapView1.getMapPosition().getZoomLevel();
	     	// GeoPoint aGp = mapView.getMapCenter();  mapsforge 0.2.4
	     	GeoPoint aGp = mapView1.getMapPosition().getMapCenter();
	     	int aLATE6 = (int) aGp.latitudeE6; // we keep the prefs in Micrograd
	     	int aLONE6 = (int) aGp.longitudeE6;
	     	if (test) Log.d(TAG,"on stop saving  : lastCenter " + aLATE6 + " " + aLONE6);
	     	StringBuffer alarmRadiusStr = new StringBuffer();
	     	alarmRadiusStr.append(getAISWarningDistance());
	     	String prev_ownshipLAT_key = getResources().getString(R.string.pref_OwnShipLAT);
	     	String prev_ownshipLON_key = getResources().getString(R.string.pref_OwnShipLON);
	     	String prev_mapCenterLAT_key = getResources().getString(R.string.pref_mapCenterLAT);
	     	String prev_mapCenterLON_key = getResources().getString(R.string.pref_mapCenterLON);
	     	prefs = PreferenceManager.getDefaultSharedPreferences(this);
	     	prefs.edit()
	     	.putString(prev_ownshipLAT_key, PositionTools.getLATString(myShip.getLAT()))
	     	.putString(prev_ownshipLON_key, PositionTools.getLONString(myShip.getLON()))
	     	.putString(prev_mapCenterLAT_key, PositionTools.getLATString(aGp.getLatitude()))
	     	.putString(prev_mapCenterLON_key, PositionTools.getLONString(aGp.getLongitude()))
	     	.putInt(AISPlotterGlobals.PREV_LAST_GEOPOINT_LAT, aLATE6)
	     	.putInt(AISPlotterGlobals.PREV_LAST_GEOPOINT_LON, aLONE6)
	     	//.putInt(PREV_LAST_GEOPOINT_LAT, lastMapCenterPoint.getLatitudeE6())
	     	//.putInt(PREV_LAST_GEOPOINT_LON, lastMapCenterPoint.getLongitudeE6())
	     	//.putString(PREF_ALARM_RADIUS,alarmRadiusStr.toString())
	     	.putFloat(AISPlotterGlobals.PREF_ZOOM_FACTOR,aZoomFactor)
	 	    .commit();
	     	
	     	
	     	 // we used nmea data
	     	Logger.d(TAG,"NMEAParser Data Receiver stopped");
	     	unregisterReceiver(mNMEAParserDataReceiver);
	     
	     	mRefreshTargetListHandler.removeCallbacks(timertaskRefreshTargetList);
	     	
	     }
	     
	     public void onDestroy(){
	    	 if (test) Log.d(TAG, "onDestroy entered");
	     	if ( mUseGPSSource == 1){	// we used the internal gps
		     	if (!(localGPSServiceConnection == null)) {
		     		gpsLocalService.stopRequestLocationUpdates();
		     		if (test) Log.d(TAG," close gpsservice");
		     		unbindService(localGPSServiceConnection);
		     		//gpsLocalService.stopSelf();
		     	}
	     	}
	     	this.screenshotCapturer.interrupt();
	     	mDbAdapter.close();
	     	Log.d(TAG,"AISTCPOpenMapPlotter-->onDestroy");
	     	super.onDestroy();
	     }   
	     
	     
	     
	     public void displayAsToastMSGX (String aMessage) {
	    	 Toast.makeText(AISTCPOpenMapPlotter.this, 
	    			   aMessage, Toast.LENGTH_LONG).show();
	     }
	     
	    
	     
	     
	     public TargetList getTargetList() {
	     	return mTargetList;
	     }
	     
	     
	     
	     public long getCurrentUTCNmeaTime() {
	    	 mCurrentNmeaTime  = System.currentTimeMillis();
	   	    return mCurrentNmeaTime;
	     }
	      
	      public void setCurrentUTCNmeaTime (long aUTCTime){
	   	   mCurrentNmeaTime = aUTCTime; 
	      }
	      
	      

       /**
        * show a Activity which displays a target
        * @param aShip
        */
	      
	   public void showEditTarget  (AISTarget aShip) {
		    long id = aShip.getId();
			Intent i = new Intent(this,TargetEditActivity.class);
	        i.putExtra(TrackDbAdapter.KEY_ROWID, id);
	        startActivityForResult(i, 1); 
	   }
	 /* 
	  * nur in google maps    
	  @Override 
		protected boolean isRouteDisplayed() { 
		    return false; 
		}*/
	  
	/**
	 * restore a track from the database using a thread
	 * show the progress in a progress bar
	 * see restoreRouteInInitPhase
	 * @parama MMSIStr the MMSI of the target to read the track from teh database
	 * @param pItem the AISOverlayItem that must be updated
	 */
	   public void restoreTrackWithThread(String aMMSIStr,AISOverlayItem pItem) {
			mProgressHorizontal.setVisibility(View.VISIBLE);
			mProgressHorizontal.setProgress(0);
		
			//mArrayAISTrackWayOverlay.clear(); 13_03_16
			//mWaysOverlay.clearWay(aMMSIStr);  // should clear the way on the Overlay to the Target with MMSI
			// but if we do it here, the thread does not execute!!, so we do it i the calling routine
			//if (test)
			Log.d(TAG, "Begin restore track with thread for " + aMMSIStr); 
			final String theMMSI = aMMSIStr;
			final AISOverlayItem theItem = pItem;
			final String aTrackpointMsg = "MMSI: "+ theMMSI + " " +getResources().getString(R.string.guiupdate_info_restore_track) ;
			// Start lengthy operation in a background thread
			new Thread(new Runnable() {
				public void run() {

					String aMMSI;
					String aId;
					String aLATStr;
					String aLONStr;
					long aUTC;
					GeoPoint firstPoint = new GeoPoint(51.0, 6.0);
					final Cursor cursor = mDbAdapter.fetchShipTrackTable(theMMSI);
					int count = 0;
					if (cursor != null) {
						count = cursor.getCount();
						Log.d(TAG,"Restore Track: "+ count + " Trackpoints to process ");
						if (count > 100)
							showToastOnUiThread(aTrackpointMsg+ " " + count);
					}
					int updateProgressbar = count / 100; // The progressbar is 0 to 100,
					// we calculate the counter if we have to update
					int updateCounter = 0;
					//if (test)
					if (test)Log.d(TAG, " Thread runs : Begin restore track in thread for " + theMMSI ); 
					if ((cursor != null) && (cursor.getCount() > 0)) {
						cursor.moveToFirst();
						aId = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_ROWID));
						aMMSI = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_MMSI));
						aLATStr = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_LAT));
						aLONStr = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_LON));
						aUTC = cursor.getLong(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_UTC));
						firstPoint = new GeoPoint(Double.parseDouble(aLATStr), Double.parseDouble(aLONStr));
						if (test)Log.d(TAG, " Process the list :Begin restore track in thread for " + aMMSI); 
						while (cursor.moveToNext()) {
							aId = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_ROWID));
							aMMSI = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_MMSI));
							aLATStr = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_LAT));
							aLONStr = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_LON));
							aUTC = cursor.getLong(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_UTC));
							
							if (test) Log.i(TAG, "next route Point " + aId + " MMSI " + aMMSI + " LAT " + aLATStr + " LON "
										+ aLONStr + " UTC " + aUTC);
							double lat = Double.parseDouble(aLATStr);
							double lon = Double.parseDouble(aLONStr);
							GeoPoint secondPoint = new GeoPoint(lat, lon);
							OverlayWay aOverlayWay = new OverlayWay(new GeoPoint[][] { { firstPoint, secondPoint } });
							//mArrayAISTrackWayOverlay.addWay(aOverlayWay); 13_03_16
							mWaysOverlay.addWay(aMMSI,aOverlayWay);
                            theItem.addWay(aOverlayWay);
							// Update the progress bar
							updateCounter++;
							if (updateCounter == updateProgressbar) {
								updateCounter = 0;
								mProgressHandler.post(new Runnable() { // we must post , as we could not access the UI in a separate
																// thread
									public void run() {
										mProgressHorizontal.incrementProgressBy(1);
										// mArrayAISWayOverlay.requestRedraw();
									}
								});
							}
							firstPoint = secondPoint;
						} // while
						//if (test) 
						Log.d(TAG, "end restore track in thread for " + aMMSI); 
					} // if cursor
					if (cursor != null)
						cursor.close();
					//if (test) 
					Log.d(TAG,"finish the restoreTrack-Function for " + theMMSI);
					
					mProgressHandler.post(new Runnable() { // we must post , as we could not access the UI in a separate thread
						public void run() {
							mProgressHorizontal.setVisibility(View.INVISIBLE);
							Log.d(TAG,"set the progressbar invisible");
							
						}
					});
				} // end of run
			}).start();
			Log.d(TAG, "Thread started to restore track with thread for " + aMMSIStr); 
		}
	   
	   
	   
	// deal with menus
	   /**
	    * enable the route creation buttons
	    * we show the add, delete, finish save button
	    */
	   
	   public void enableRouteCreationButtons() {
		    //this.mRouteLayout.setVisibility(View.VISIBLE);
			this.mRoutePointAddButton.setVisibility(View.VISIBLE);
			this.mRoutePointDeleteButton.setVisibility(View.VISIBLE);
			this.mRouteFinishButton.setVisibility(View.VISIBLE);
			this.mRouteSaveButton.setVisibility(View.VISIBLE);
			//this.mRecordTrackButton.setVisibility(View.INVISIBLE);
			String aInfo = getResources().getString(R.string.guiupdate_info_press_add_tp);
			showToastOnUiThread(aInfo);
			//mArrayRoutePointsOverlay.setMustShowCenter(true); 13_03_12
			//mArrayRoutePointsOverlay.requestRedraw();
			mItemizedOverlay.setMustShowCenter(true);
			mItemizedOverlay.requestRedraw();
		}
  
		/**
		 * add a RoutePoint on the RouteOverlay
		 * a rounded Rectangle with number is shown if the mShowRoutePoints is true
		 * else a little circle
		 * if there is a Routepoint before a way is added
		 * @param pGeoPoint
		 */
		public void addRoutePointOnOverlay(GeoPoint pGeoPoint) {
			//int countOverlaySize = this.mArrayRoutePointsOverlay.size();
			int count = mRouteItemList.size();
			GeoPoint lastPoint = null;
			if (count > 0) {
				lastPoint = mRouteItemList.get(count - 1).getPoint();
			}
			int numberOfPoint = count;
			Bitmap aNewBitmap = null;
			if (mShowRoutePoints) {
				// big symbol with number
				aNewBitmap = Bitmap.createBitmap(30,30,Bitmap.Config.ARGB_8888);
				Canvas aCanvas = new Canvas(aNewBitmap);
				//aCanvas.drawColor(Color.WHITE);
				Paint aPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
				//Paint aPaint = new Paint();
				aPaint.setStyle(Paint.Style.STROKE);
				aPaint.setStrokeWidth(1);
				
				aPaint.setColor(0x880000FF);
				RectF aOval= new RectF(1, 1, 28,28);
				
				aCanvas.drawRoundRect(aOval, 5,5, aPaint);
				
				aPaint.setColor(Color.BLACK);
				aPaint.setTextSize(12);
				
				String aNumberStr = Integer.toString(numberOfPoint);
				aCanvas.drawText(aNumberStr, 5, 20,aPaint);
			} else {
				// little symbol without number
				aNewBitmap = Bitmap.createBitmap(5,5,Bitmap.Config.ARGB_8888);
				Canvas aCanvas = new Canvas(aNewBitmap);
				//aCanvas.drawColor(Color.WHITE);
				Paint aPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
				//Paint aPaint = new Paint();
				aPaint.setStyle(Paint.Style.STROKE);
				aPaint.setStrokeWidth(1);
				
				aPaint.setColor(0x880000FF);
				RectF aOval= new RectF(1, 1, 4,4);
				
				aCanvas.drawRoundRect(aOval, 1,1, aPaint);
				
			}
			BitmapDrawable bmd = new BitmapDrawable(aNewBitmap);
			String aTitleInfo = getResources().getString(R.string.route_tap_title);
			String aTitle = aTitleInfo + " " + Integer.toString(count);
			
			String aInfo = "LAT: " + PositionTools.getLATString(pGeoPoint.getLatitude()) 
                           + "\nLON: " + PositionTools.getLONString(pGeoPoint.getLongitude());
			
			//RouteOverlayItem aRouteOverlayItem = new RouteOverlayItem(pGeoPoint,aTitle,aInfo,ItemizedOverlay.boundCenterBottom(bmd));
			RouteOverlayItem aRouteOverlayItem = new RouteOverlayItem(pGeoPoint,numberOfPoint,aTitle,aInfo,ItemizedOverlay.boundCenter(bmd));
			
			
			GeoPoint newPoint = pGeoPoint;
			RoutePointItem aRouteItem = new RoutePointItem(newPoint);
			aRouteItem.setRouteOverlayItem(aRouteOverlayItem);
			// this.mArrayRoutePointsOverlay.addItem(aRouteOverlayItem); 13_03_12
			this.mItemizedOverlay.addItem(aRouteOverlayItem);  
			this.mItemizedOverlay.requestRedraw();
			
			//OverlayCircle aOverlayCircle = new OverlayCircle();
			//aOverlayCircle.setCircleData(newPoint, 30);
			//aRouteItem.setOverlayCircle(aOverlayCircle);
			//this.mRoutePointsCircleOverlay.addCircle(aOverlayCircle);
			//this.mRoutePointsCircleOverlay.requestRedraw();
			
			
			this.mRouteItemList.add(aRouteItem);
			mRouteEditIsDirty = true;
			if (lastPoint != null) {
				OverlayWay aOverlayWay = new OverlayWay(new GeoPoint[][] { { lastPoint, newPoint } });
				aOverlayWay.setPaint(mRouteWayPaintFill, mRouteWayPaintOutline);
				aRouteItem.setOverlayWay(aOverlayWay);
				//this.mRouteArrayWayOverlay.addWay(aOverlayWay); 13_03_16
				//this.mRouteArrayWayOverlay.requestRedraw();
				this.mWaysOverlay.addWay(AISPlotterGlobals.DEFAULTROUTE,aOverlayWay);
				this.mWaysOverlay.requestRedraw();
			}

		}

		/**
		 * restore a route to from the database
		 * at the moment only the default route "0000" is used 
		 * param pNumberStr the number of the route, defaults to "0000" 
		 * we must assert, that this method is not executed twice if it is executing
		 * see the value of mInRestoreRouteFromDatabase
		 */
		private void restoreRouteFromDatabase(String pNumberStr) {
			/*int countRouteArraySize = this.mArrayRoutePointsOverlay.size(); 13_03_12
			if (countRouteArraySize > 0) {
				this.mRouteArrayWayOverlay.clear();
				this.mArrayRoutePointsOverlay.clear();
			}*/
			
			if (mInRestoreRouteFromDatabase) {  // 13_07_24 prevent the method beeing executed twice
				   return ;
			} else {
				mInRestoreRouteFromDatabase = true; 
				int countRouteArraySize = mItemizedOverlay.getRouteListSize();
				if (countRouteArraySize > 0){
					this.mItemizedOverlay.clearRouteItems();
					//this.mRouteArrayWayOverlay.clear(); 13_03_16
					this.mWaysOverlay.clearWay(pNumberStr);
				}
				if (this.mRouteItemList.size() > 0) {
					this.mRouteItemList.clear();
				}
	
				Cursor cursor = this.mDbAdapter.fetchRouteTable(pNumberStr);
				int count;
				if ((cursor != null) && (cursor.getCount() > 0)) {
					count = cursor.getCount();
					String aId;
					String aLATStr;
					String aLONStr;
					long aUTC;
					cursor.moveToFirst();
					aId = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_ROWID));
					aLATStr = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_LAT));
					aLONStr = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_LON));
					aUTC = cursor.getLong(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_UTC));
					GeoPoint aRoutePoint = new GeoPoint(Double.parseDouble(aLATStr), Double.parseDouble(aLONStr));
					
					addRoutePointOnOverlay(aRoutePoint);
					
					
					GeoPoint lastPoint = aRoutePoint;
	
					while (cursor.moveToNext()) {
						aId = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_ROWID));
	
						aLATStr = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_LAT));
						aLONStr = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_LON));
						aUTC = cursor.getLong(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_UTC));
	
						double lat = Double.parseDouble(aLATStr);
						double lon = Double.parseDouble(aLONStr);
						aRoutePoint = new GeoPoint(lat, lon);
						
						addRoutePointOnOverlay(aRoutePoint);
						
	
						
	
					} // while
					
					//this.mRouteArrayWayOverlay.requestRedraw(); 13_03_16
					
					this.mWaysOverlay.requestRedraw();
					
					/*// why do I do it 12_09_05
					if (mRouteEditIsDirty) {
						writeRouteListToDatabase();
					}*/
					
				} // cursor !=null
				if (cursor != null) cursor.close();
				mInRestoreRouteFromDatabase = false;
			} 
		}
		
		/**
		 * the Route "0000" is kept in mRouteItemList
		 */

		private void writeRouteListToDatabase() {
			String aNumberStr = AISPlotterGlobals.DEFAULTROUTE;
			// write the route data to datbase
			int count = mRouteItemList.size();
			if (count > 0) {
				mDbAdapter.deleteRouteTable(aNumberStr);
				mDbAdapter.createRouteTable(aNumberStr);
				for (int index = 0; index < count; index++) {
					RoutePointItem aRouteItem = mRouteItemList.get(index);
					double aLAT = aRouteItem.getPoint().getLatitude();
					String aLATStr = Double.toString(aLAT);
					double aLON = aRouteItem.getPoint().getLongitude();
					String aLONStr = Double.toString(aLON);
					long aUTC = System.currentTimeMillis();

					mDbAdapter.insertRoutePointToTable(aNumberStr, aLATStr, aLONStr, aUTC);
				}
				mRouteEditIsDirty = false;
			}
		}
		
		/**
		 * finish editing a route
		 * the route is not saved !!
		 */

		private void finishRouteOnOverlay() {  
			//this.mRouteLayout.setVisibility(View.INVISIBLE);
			this.mRoutePointDeleteButton.setVisibility(View.INVISIBLE);
			this.mRoutePointAddButton.setVisibility(View.INVISIBLE);
			this.mRouteFinishButton.setVisibility(View.INVISIBLE);
			this.mRouteSaveButton.setVisibility(View.INVISIBLE);
			//this.mRecordTrackButton.setVisibility(View.VISIBLE);
			//mArrayRoutePointsOverlay.setMustShowCenter(false); 13_03_12
			//mArrayRoutePointsOverlay.requestRedraw();
			mItemizedOverlay.setMustShowCenter(false);
			mItemizedOverlay.requestRedraw();
			  
		}      
		/**
		 * set up the list of tracepoints to trace according to the maps scalevalue
		 * and the distances of the routepoints.We may create additional points 
		 * to gurrantee all tiles that belong to the route are in the cache
		 */ 
		private void traceRouteWithCalculatedStepsAndFillMapcache() {
			if (snapToLocation)  {     
		    	  showToastOnUiThread("can not trace Route while snap to own ship is on"); 
		      }else {
		    	  int count = mRouteItemList.size();
		    	  if (count > 2) {
		    		    mTracelist.clear();
		    		    mTracePointIndex = 0;
		    		    int aMapScaleValue = PositionTools.calculateMapScaleValue(mapView1);
						RoutePointItem aFirstRouteItem = mRouteItemList.get(0);
						GeoPoint aFirstPoint = aFirstRouteItem.getPoint();
						mTracelist.add(aFirstPoint);
						for (int aRoutePointIndex=1;aRoutePointIndex < count;aRoutePointIndex ++) {
							RoutePointItem aSecondRouteItem = mRouteItemList.get(aRoutePointIndex);
							GeoPoint aSecondPoint = aSecondRouteItem.getPoint();
							double aDistanceInMeters = 1.862d * PositionTools.calculateDistance(aFirstPoint, aSecondPoint) * 1852;
							if (aDistanceInMeters < aMapScaleValue) {
								// we simple add the route point to the trace list
								mTracelist.add(aSecondPoint);
							} else {
								// we must generate additional points to trace
								int aRepeater = (int) ((aDistanceInMeters / aMapScaleValue)+ 1);
								double deltaLAT = aSecondPoint.getLatitude()- aFirstPoint.getLatitude();
								double deltaLON = aSecondPoint.getLongitude()- aFirstPoint.getLongitude();
								double incrementLAT = deltaLAT / aRepeater;
								double incrementLON = deltaLON / aRepeater;
								for (int aTracePointIndex = 0; aTracePointIndex < aRepeater-1;aTracePointIndex++){
									GeoPoint aTracePoint =new GeoPoint (aFirstPoint.getLatitude()+ incrementLAT *( aTracePointIndex+1),
											                            aFirstPoint.getLongitude()+ incrementLON* (aTracePointIndex +1));
									mTracelist.add (aTracePoint);
								}
								mTracelist.add(aSecondPoint);
							}
							aFirstPoint = aSecondPoint;
						}
						// we fiiled the trace list
						//mArrayRoutePointsOverlay.setMustShowCenter(true); 13_03_12
						//mArrayRoutePointsOverlay.requestRedraw();
						mItemizedOverlay.setMustShowCenter(true);
						mItemizedOverlay.requestRedraw();
						this.mBreakTraceRouteButton.setVisibility(View.VISIBLE);
						this.mTraceRouteActive = true;
						
						mCenterMapToTracePointHandler.postDelayed(centerMapToTracePointRunnable,100); 
		    	  }
		      }
		}
		/**
		 * trace a route handler
		 * centers the map to the next point in the tracelist
		 * map cache will be filled with the centered maps
		 */
		private static Handler mCenterMapToTracePointHandler = new Handler();
		
		private Runnable centerMapToTracePointRunnable = new Runnable() {
		    public void run() {
		      Logger.d(TAG,"Show Target on map requested executed delayed");
		      int aMapScaleValue = PositionTools.calculateMapScaleValue(mapView1);
		      // in google mapController.animateTo(mLastCenteredTargetPoint);
		      if (!mTraceRouteActive){
		    	 // mArrayRoutePointsOverlay.requestRedraw(); 13_03_12
		    	  mItemizedOverlay.requestRedraw();
				  mTraceRouteActive = false;
				  mTracelist.clear();
				  mBreakTraceRouteButton.setVisibility(View.INVISIBLE);
				  String info1 = getResources().getString(R.string.trace_route_stop_info1);
				  String info3 = getResources().getString(R.string.trace_route_stop_info3);
	    		  showToastOnUiThread(info1 + " " + aMapScaleValue + " " + info3);
	    		  return;
		      }
		      if (snapToLocation) {
		    	  String info = getResources().getString(R.string.trace_route_cannot_trace);
		    	  showToastOnUiThread(info); 
		      }else {
		    	  
		    	  if ((mTracePointIndex  >-1) && (mTracePointIndex < mTracelist.size())){
			    	  
					  GeoPoint aPoint = mTracelist.get(mTracePointIndex);
					  String info1 = getResources().getString(R.string.trace_route_run_info1);
					  String info2 = getResources().getString(R.string.trace_route_run_info2);
					  String info3 = getResources().getString(R.string.trace_route_run_info3);
					  showToastOnUiThread(info1+ " " + mTracePointIndex + " " +info2 +" " 
							              + mTracelist.size() +" " +info3 +" "+ aMapScaleValue);
			    	  mapController.setCenter(aPoint);
			    	  mTracePointIndex++;
			    	  if (mTraceRouteActive){
			    		  mCenterMapToTracePointHandler.postDelayed(this, 5000);  
			    		 
			    	  }
			    	  
		    	  }else {
		    		  //mArrayRoutePointsOverlay.setMustShowCenter(false); 13_03_12
		    		  //mArrayRoutePointsOverlay.requestRedraw();
		    		  mItemizedOverlay.setMustShowCenter(false);
		    		  mItemizedOverlay.requestRedraw();
					  mTraceRouteActive = false;
					  mBreakTraceRouteButton.setVisibility(View.INVISIBLE);
					  String info1 = getResources().getString(R.string.trace_route_stop_info1);
					  String info2 = getResources().getString(R.string.trace_route_stop_info2);
		    		  showToastOnUiThread(info1 + " " + aMapScaleValue + " "  + info2);
		    	  }
		      }
		     // mapView.invalidate();
		      //mCenterMapToSelectedTarget.postDelayed(this,500);
		    }
		  };
		
		/**
		 * try to trace the actual route and try to fill the MapCache
		 * not used 12_08_03 see traceRouteWithCalculatedStepsAndFillMapcache
		 */
		private void traceRouteAndFillMapCache() {
			// we scan the routeItem list and center the map to each route point
			if (snapToLocation) {
		    	  showToastOnUiThread("can not trace Route while snap to own ship is on"); 
		      }else {
				int count = mRouteItemList.size();
				if (count > 2) {
					    routePointIndex = 0;
						RoutePointItem aRouteItem = mRouteItemList.get(0);
						GeoPoint aPoint = aRouteItem.getPoint();
						//mArrayRoutePointsOverlay.setMustShowCenter(true); 13_03_12
						//mArrayRoutePointsOverlay.requestRedraw();
						
						mItemizedOverlay.setMustShowCenter(true);
						mItemizedOverlay.requestRedraw();
						mCenterMapToSelectedRoutePointHandler.postDelayed(centerMapToSelectedRoutePointRunnable,100); 
				}
		      }
		}
		
		/**
		 * map trace of a route is handled by a thread
		 * not used 12_08_03
		 */
		private int routePointIndex = 0; 
		private static Handler mCenterMapToSelectedRoutePointHandler = new Handler();
		/**
		 * not used 12_08_03
		 */
		private Runnable centerMapToSelectedRoutePointRunnable = new Runnable() {
		    public void run() {
		      Logger.d(TAG,"Show Target on map requested executed delayed");
		      // in google mapController.animateTo(mLastCenteredTargetPoint);
		      if (snapToLocation) {
		    	  showToastOnUiThread("can not trace Route while snap to own ship is on"); 
		      }else {
		    	  int aMapScaleValue = PositionTools.calculateMapScaleValue(mapView1);
		    	  if ((routePointIndex >-1) && (routePointIndex < mRouteItemList.size())){
			    	  RoutePointItem aRouteItem = mRouteItemList.get(routePointIndex);
					  GeoPoint aPoint = aRouteItem.getPoint();
					  
					  showToastOnUiThread("center to route point " + routePointIndex + " with scale " + aMapScaleValue);
			    	  mapController.setCenter(aPoint);
			    	  routePointIndex++;
			    	  mCenterMapToSelectedRoutePointHandler.postDelayed(this, 5000);
		    	  }else {
		    		  //mArrayRoutePointsOverlay.setMustShowCenter(false); 13_03_12
		    		  //mArrayRoutePointsOverlay.requestRedraw();
		    		  mItemizedOverlay.setMustShowCenter(false);
		    		  mItemizedOverlay.requestRedraw();
		    		  showToastOnUiThread("trace of route points with scale " + aMapScaleValue + " finished");
		    	  }
		      }
		     // mapView.invalidate();
		      //mCenterMapToSelectedTarget.postDelayed(this,500);
		    }
		  };

		private void createExternalDirectoryIfNecessaryOld(String pDirName) {
			// not used since 12_10_30 see PositionTools.createExternalDirectoryIfNecessary()
			// external SD-card may be mounted not only on sdcard2
			if (test)
				Log.v(TAG, "createAISDirectory");
			String result = Environment.getExternalStorageState();
			if (result.equals(Environment.MEDIA_MOUNTED)) {

				// first we check if there is a directory named sdcard2
				File path = Environment.getExternalStorageDirectory();
				File[] subDirs = path.listFiles(new FileFilter() {
					@Override
					public boolean accept(File d) {
						return d.isDirectory();
					}
				});
				boolean foundSdCard2 = false;
				if (test)
					Log.d(TAG, "Subdirs of " + path.getName());
				if (test)
					Log.d(TAG, "Subdirs of " + path.getAbsolutePath());
				for (int index = 0; index < subDirs.length; index++) {
					String aName = subDirs[index].getName();
					if (test)
						Log.d(TAG, aName);
					if (aName.equals("sdcard2"))
						foundSdCard2 = true;
				}
				// if pDirName contains / we have to analyse the whole path
				String [] dirs = pDirName.split("/");
				int dirCount = dirs.length;
				String newDirName = "/";
			    //  now we know how many dirs we must create
				for (int dirIndex =0;dirIndex < dirCount; dirIndex++){
				
					StringBuffer buf = new StringBuffer();
					if (foundSdCard2)
						buf.append("sdcard2");
					newDirName = newDirName + dirs[dirIndex]  +"/";
					buf.append(newDirName);
					String dirName = buf.toString();
					File file = new File(path, dirName);
					try {
						String filePathStr = file.getAbsolutePath();
						if (file.mkdir()) { // here we need android permission in the manifest
							//if (test)
								Log.v(TAG, "create Directory: " + filePathStr);
						} else {
							// if (test)exists " + filePathStr);
						}
					} catch (SecurityException se) {
						Log.d(TAG,se.toString());
						if (test)
							Log.v("TAG", "Security exception : Directory not created " + se);
					} catch (Exception e ) {
						Log.d(TAG,e.toString());
						
					} // try
				} //for
			}
		}

		/**
		 * @param pattern
		 *            use a pattern like "000.00"
		 * @param value
		 *            the value to convert 45.34523
		 * @return aString with the value formatted 045.34
		 */
		public static String customFormat(String pattern, double value) {
			DecimalFormat myFormatter = new DecimalFormat(pattern);
			String output = myFormatter.format(value);
			return output;
		}

		/**
		 * get the dateTime as needed in the gpx_form 
		 * @param aUTC
		 *      the thime in UTC
		 * @return
		 *      the formatted string e,g 2011-07-16T13:24:55
		 */
		private String getDateTimeGPXForm(long aUTC) {
			// String format = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
			// 2011-07-16T13:24:55
			String format = "yyyy-MM-dd'T'HH:mm:ss";
			SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.US);
			sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
			String result = sdf.format(new Date(aUTC));
			// String result = new String();
			// result.format("%30s %s\n", format, sdf.format(new Date(aUTC)));
			return result;
		}
		
		/**
		 * 
		 * @param pNumberStr
		 *       the numberStr of the route , default is "0000"
		 */

		private void saveRouteToExternalStorage(String pFilename, String pDirPath) {
			if (test)
				Log.v(TAG, "saveRoute");
			// visual response to the user
			mProgressHorizontal.setVisibility(View.VISIBLE);
			mProgressHorizontal.setProgress(0);
			//String prev_route_dir_key =  getResources().getString(R.string.pref_route_directory_key);
	 		//String route_dir_str = prefs.getString(prev_route_dir_key, "AISPlotter/Routedata");
			final String aDirPath = pDirPath;
			final String aFilename = pFilename;
			
			new Thread(new Runnable() { // as we want response to the user we must use a separate thread
						public void run() {
							// Create a path where we will place our data in the user's
							// public directory. Note that you should be careful about
							// what you place here, since the user often manages these files.
							// we write the data in a directory called Trackdata
							PositionTools.createExternalDirectoryIfNecessary(aDirPath);
							
							String result = Environment.getExternalStorageState();
							if (result.equals(Environment.MEDIA_MOUNTED)) {
								File path = PositionTools.getExternalStorageDir();
				
								StringBuffer buf = new StringBuffer();
								buf.append(aDirPath);
								buf.append("/Route_");
								buf.append(aFilename);
								buf.append("_");
								buf.append(PositionTools.getCurrentDateTimeForFilename());
								buf.append(".gpx");
								String fileName = buf.toString();
								File file = new File(path, fileName);
								String filePathStr = file.getAbsolutePath();
								try {
									if (file.createNewFile()) { // here we need android permission in the manifest
										if (test)
											Log.v(TAG, "create file: " + filePathStr);
									} else {
										if (test)
											Log.v(TAG, "file exists, overwrite " + filePathStr);
									}
									// the file exists or was opened for writing
									BufferedWriter fileBuf = new BufferedWriter(new FileWriter(file));
									// Write the gpx header
									// <?xml version="1.0" encoding="UTF-8"?>
									// write header
									String header = "<?xml version=" + '"' + "1.0" + '"' + " encoding=" + '"' + "UTF-8"
											+ '"' + "?>";
									fileBuf.write(header);
									// "creator="GPSTracker" version="1.1"
									String gpxStart = "\n <gpx creator=" + '"' + "AISTCPOpenMapPlotter" + '"' + " version="
											+ '"' + "0.41" + '"';
									// xsi:schemaLocation="http://www.topografix.com/GPX/1/1
									gpxStart = gpxStart + "\n xsi:schemaLocation=" + '"'
											+ "http://www.topografix.com/GPX/1/1 ";
									// http://www.topografix.com/GPX/1/1/gpx.xsd"
									gpxStart = gpxStart + "\n http://www.topografix.com/GPX/1/1/gpx.xsd" + '"';
									// xmlns="http://www.topografix.com/GPX/1/1"
									gpxStart = gpxStart + "\n xmlns=" + '"' + "http://www.topografix.com/GPX/1/1" + '"';
									// xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
									gpxStart = gpxStart + "\n xmlns:xsi=" + '"'
											+ "http://www.w3.org/2001/XMLSchema-instance" + '"' + ">";
									fileBuf.write(gpxStart);
									String metadataStart = "\n <metadata>";
									fileBuf.write(metadataStart);
									String metadataEnd = "\n </metadata>";
									fileBuf.write(metadataEnd);
									String rteStart = "\n <rte>";
									fileBuf.write(rteStart);

									Cursor cursor = mDbAdapter.fetchRouteTable(AISPlotterGlobals.DEFAULTROUTE);
									int count = cursor.getCount();
									int updateProgressbar = count / 100; // The progressbar is 0 to 100,
									// we calculate the counter if we have to update the progressbar
									int updateCounter = 0;
									if ((cursor != null) && (cursor.getCount() > 0)) {

										String aId;
										String aLATStr;
										String aLONStr;
										long aUTC;
										// we use the simpleDataFormat to convert millis to gpx-readable form
										String format = "yyyy-MM-dd'T'HH:mm:ss";
										SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.US);
										sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
										cursor.moveToFirst();
										aId = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_ROWID));
										aLATStr = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_LAT));
										aLONStr = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_LON));
										aUTC = cursor.getLong(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_UTC));
										GeoPoint aRoutePoint = new GeoPoint(Double.parseDouble(aLATStr), Double
												.parseDouble(aLONStr));
										StringBuffer bufPoint = new StringBuffer();
										// <trkpt lon="6.96045754" lat="51.44282806"> <time>2011-07-16T13:24:55</time>
										// </trkpt>
										bufPoint.append("\n <rtept lon=" + '"' + aRoutePoint.getLongitude() + '"' + " lat="
												+ '"' + aRoutePoint.getLatitude() + '"' + ">" + " <time>"
												+ sdf.format(new Date(aUTC)) + "</time> " + "</rtept>");

										String aString = bufPoint.toString();
										fileBuf.write(aString);
										while (cursor.moveToNext()) {
											aId = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_ROWID));

											aLATStr = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_LAT));
											aLONStr = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_LON));
											aUTC = cursor.getLong(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_UTC));
											if (test) {
												Log.i(TAG, "next route Point " + aId + " Number " + AISPlotterGlobals.DEFAULTROUTE + " LAT "
														+ aLATStr + " LON " + aLONStr + " UTC " + aUTC);
											}

											double lat = Double.parseDouble(aLATStr);
											double lon = Double.parseDouble(aLONStr);
											aRoutePoint = new GeoPoint(lat, lon);
											bufPoint = new StringBuffer();
											// <trkpt lon="6.96045754" lat="51.44282806"> </trkpt>
											bufPoint.append("\n <rtept lon=" + '"' + aRoutePoint.getLongitude() + '"'
													+ " lat=" + '"' + aRoutePoint.getLatitude() + '"' + ">" + " <time>"
													+ sdf.format(new Date(aUTC)) + "</time> " + "</rtept>");

											aString = bufPoint.toString();
											fileBuf.write(aString);
											// Update the progress bar
											updateCounter++;
											if (updateCounter == updateProgressbar) {
												updateCounter = 0;
												mProgressbarHandler.post(new Runnable() { // we must post , as we could not access the
																				// UI in a
																				// separate
																				// thread
													public void run() {
														mProgressHorizontal.incrementProgressBy(1);
													}
												});
											}
										}

									}
									cursor.close();
									String rteEnd = "</rte>\n </gpx>";
									fileBuf.write(rteEnd);
									fileBuf.flush();
									fileBuf.close();
									if (test)
										Log.v(TAG, "file write sucessfull " + filePathStr);
									showToastOnUiThread("Route saved to " + fileName);

								} catch (IOException e) {
                                    showToastOnUiThread(" could not write route to " + filePathStr);
									Logger.d(TAG,e.toString());
									// Unable to create file, likely because external storage is
									// not currently mounted.
									if (test)
										Log.w("TAG", "Error writing " + filePathStr);
								} catch (Exception e) {
									showToastOnUiThread(" could not write route to " + filePathStr);
									Logger.d(TAG,e.toString());
									// Unable to create file, likely because external storage is
									// not currently mounted.
									if (test)
										Log.w("TAG", "Other Error writing " + filePathStr);
								}finally {
									mProgressbarHandler.post(new Runnable() { // we must post , as we could not access the UI in a
										// separate thread
										public void run() {
											mProgressHorizontal.setVisibility(View.INVISIBLE);
										}
									});
								} // finally
							} // if media mounted
						} // run
					}).start();
		}
        
		
		/**
		 * 
		 * @param aFile
		 *    aFile which contains a route, selected by FilePicker
		 */
		public void prepareLoadRoute(File aFile) {
			
			String aInfo = getResources().getString(R.string.guiupdate_info_could_not_load_data_for_default_route);
			try {
				loadRouteDataWithThread(aFile);
			} catch (SQLException e) {
				
				showToastOnUiThread(aInfo);
			}
		}
		
		/**
		 * 
		 * @param aFile a file that contains the route, selected via the filepicker
		 *    load the route  from aFile to the datbase
		 *    uses a thread 
		 *    visual feedback to the user with aprogress bar
		 */

		public void loadRouteDataWithThread(File aFile) {

			final String aFilename = aFile.getAbsolutePath(); 
			this.mProgressHorizontal.setVisibility(View.VISIBLE);
			this.mProgressHorizontal.setProgress(0);
			final String aUIMsg = getResources().getString(R.string.guiupdate_info_failed_in_parsing_route_gpx_file);

			new Thread(new Runnable() {

				public void run() {

					try {
						int count = 1000;
						int updateProgressbar = count / 500; // The progressbar is 0 to 100,
						// we calculate the counter if we have to update
						int updateCounter = 0;
						XmlPullParserFactory parserCreator;

						parserCreator = XmlPullParserFactory.newInstance();

						XmlPullParser parser = parserCreator.newPullParser();
						FileReader myReader = new FileReader(aFilename);
						parser.setInput(myReader);
						// parser.setInput(text.openStream(), null);

						int parserEvent = parser.getEventType();
						int pointCounter = -1;

						String aLATStr = null;;
						String aLONStr = null;

						// Parse the XML returned from the file
						while (parserEvent != XmlPullParser.END_DOCUMENT) {
							switch (parserEvent) {
								case XmlPullParser.START_TAG:
									String tag = parser.getName();

									if (tag.compareTo("rtept") == 0) {
										pointCounter++;
										aLATStr = parser.getAttributeValue(null, "lat");
										aLONStr = parser.getAttributeValue(null, "lon");

										// we initialize time with currentTimeMillis
										long aUtc = System.currentTimeMillis();
										mDbAdapter.insertRoutePointToTable(AISPlotterGlobals.DEFAULTROUTE, aLATStr, aLONStr, aUtc);

										if (test)
											Log.i(TAG, "   routepoint=" + pointCounter + " latitude=" + aLATStr
													+ " longitude=" + aLONStr);
										updateCounter++;
										if (updateCounter == updateProgressbar) {
											updateCounter = 0;
											mProgressbarHandler.post(new Runnable() { // we must post , as we could not access the UI
																			// in a separate
																			// thread
												public void run() {
													mProgressHorizontal.incrementProgressBy(1);

												}
											});
										}
									} else if (tag.compareTo("time") == 0) {

										// we do not handle the time

									} else if (tag.compareTo("wpt") == 0) {

										// we do not handle wpt
									}
									break;
							}

							parserEvent = parser.next();
						}

					} catch (FileNotFoundException e) {
						Log.d(TAG, "File not found");
					} catch (Exception e) {
						Log.i(TAG, "Failed in parsing XML", e);
						showToastOnUiThread(aUIMsg);

					}

					mProgressbarHandler.post(new Runnable() { // we must post , as we could not access the UI in a separate
						// thread
						public void run() {
							mProgressHorizontal.setVisibility(View.INVISIBLE);
							// we must not call restoreRouteFromDatabase here
							// there  is a resume() pending, as we selected a file with
							// the file picker activity
							restoreRouteFromDatabase(AISPlotterGlobals.DEFAULTROUTE);
						}
					});

				}
			}).start();

		}

		/**
		 *  delete the last route point from the actual route
		 */
		private void deleteLastRoutePointFromOverlay() {
			// find the last route point in the routeItemlist mRouteItemList
			// delete the corresponding item from the overlay
			// delete the last route point from the list
			// delete a corresponding way, if it exists
			// set the center of the map tothe last available route point
			int count = this.mRouteItemList.size();
			RoutePointItem lastItem = null;
			if (count > 0) {
				lastItem = this.mRouteItemList.get(count - 1);
			}
			if (lastItem != null) {
				RouteOverlayItem  aRouteOverlayItem = lastItem.getRouteOverlayItem();
				//this.mArrayRoutePointsOverlay.removeItem(aRouteOverlayItem); 13_03_12
				this.mItemizedOverlay.removeItem(aRouteOverlayItem);
				
				//OverlayCircle aOverlayCircle = lastItem.getOverlayCircle();
				//this.mRoutePointsCircleOverlay.removeCircle(aOverlayCircle);
				
				OverlayWay aOverlayWay = lastItem.getOverlayWay();
				if (aOverlayWay != null) {
					//this.mRouteArrayWayOverlay.removeWay(aOverlayWay);
					this.mWaysOverlay.removeWay(AISPlotterGlobals.DEFAULTROUTE,aOverlayWay);
				}
				this.mRouteItemList.remove(lastItem);
				mRouteEditIsDirty  = true;
				count = this.mRouteItemList.size();
				if (count > 0) {
					// if we have a item left we set the center to the last routeItem
					lastItem = this.mRouteItemList.get(count - 1);
					if (lastItem != null) {
						this.mapView1.getController().setCenter(lastItem.getPoint());
						this.mapView1.invalidate();
					}

				}

			} else {
				String aInfo = getResources().getString(R.string.guiupdate_info_no_route_point_to_delete);
				showToastOnUiThread(aInfo);
			}

		}
		

		/**
		 * prepare to edit the actual route
		 */
		private void editRoute_Menu() {
			enableRouteCreationButtons();
		}
		
		/**
		 *  make a new route, delete the old one
		 */

		private void newRoute_Menu() {
			deleteRoute_Menu();
			enableRouteCreationButtons();
		}

		/**
		 *  delete the actual route
		 */
		private void deleteRoute_Menu() {
			String aRouteTableNumber = AISPlotterGlobals.DEFAULTROUTE;
			boolean error = false;
			try {
				//this.mDbAdapter.deleteRouteTable(aNumberStr);
				this.mDbAdapter.truncateRouteTable(aRouteTableNumber);
			} catch (SQLException e) {
				Log.d(TAG, "delete Route " + e.toString());
				error = true;
			} catch (Exception e) {
				Log.d(TAG, "delete Route " + e.toString());
				error = true;
			}
			this.mRouteItemList.clear();
			//this.mRouteArrayWayOverlay.clear();  13_03_16
			this.mWaysOverlay.clearWay(AISPlotterGlobals.DEFAULTROUTE);
			//this.mArrayRoutePointsOverlay.clear(); 13_03_12
			this.mItemizedOverlay.clearRouteItems();
			
			//int count = this.mArrayRoutePointsOverlay.size();
			//this.mRoutePointsCircleOverlay.clear();
			//this.mRoutePointsCircleOverlay.requestRedraw();
			showToastOnUiThread("Create a new empty route table");
			//mCreateNewRouteTableHandler.postDelayed(createNewRoutetableRunnable, 1000);
			if (error) {
				String aInfo = getResources().getString(R.string.guiupdate_info_error_creating_new_route_table);
			   this.showToastOnUiThread (aInfo);
			}
		}
		
		
		
		private Handler mCreateNewRouteTableHandler = new Handler();
		
		
		private Runnable createNewRoutetableRunnable = new Runnable() {
			public void run() {
				createNewRouteTable(AISPlotterGlobals.DEFAULTROUTE);
			}
		};
		
		private void createNewRouteTable(String aTableNumberStr) {
			boolean error = false;
			this.mRouteItemList.clear();
			//this.mRouteArrayWayOverlay.clear();  13_03_16
			this.mWaysOverlay.clearWay(AISPlotterGlobals.DEFAULTROUTE);
			// this.mArrayRoutePointsOverlay.clear(); 13_03_12
			this.mItemizedOverlay.clearRouteItems();
			
			//int count = this.mArrayRoutePointsOverlay.size();
			//this.mRoutePointsCircleOverlay.clear();
			//this.mRoutePointsCircleOverlay.requestRedraw();
			try {
				this.mDbAdapter.createRouteTable(aTableNumberStr);
			} catch (SQLException e) {
				Log.d(TAG, "new Route " + e.toString());
				error = true;
			} catch (Exception e) {
				Log.d(TAG, "new Route " + e.toString());
				error = true;
			}
			if (error) {
				String aInfo = getResources().getString(R.string.guiupdate_info_error_creating_new_route_table);
			   this.showToastOnUiThread (aInfo);
			}
		}
		/**
		 * get the current directory of the file Picker
		 * @return
		 */
		private File getCurrentDirectory() {
			File currentDirectory = null;
			SharedPreferences preferences = getSharedPreferences(FilePicker.PREFERENCES_FILE, MODE_PRIVATE);
			currentDirectory = new File(preferences.getString(FilePicker.CURRENT_DIRECTORY, FilePicker.DEFAULT_DIRECTORY));
			if (currentDirectory.exists() || currentDirectory.canRead()) {
				currentDirectory = new File(FilePicker.DEFAULT_DIRECTORY);
			}
			return currentDirectory;
		}
		
		

		/**
		 *  save route to databaese and to the default route file with date_time
		 */
		private void saveRoute_Menu() {
			//writeRouteListToDatabase();
			String aNumberStr = AISPlotterGlobals.DEFAULTROUTE;
			// This example shows how to add a custom layout to an AlertDialog
            LayoutInflater factory = LayoutInflater.from(this);
            final View textEntryView = factory.inflate(R.layout.save_route_alert_dialog_text_entry, null);
            final TextView aFilenameEditField = (TextView)textEntryView.findViewById(R.id.filename_edit);
            aFilenameEditField.setText(aNumberStr);
            final TextView aDirectoryEditField = (TextView)textEntryView.findViewById(R.id.basedirectory_edit);
            String prev_route_dir_key =  getResources().getString(R.string.pref_route_directory_key);
	 		String route_dir_str = prefs.getString(prev_route_dir_key, AISPlotterGlobals.DEFAULT_ROUTE_DATA_DIRECTORY);
            //String curDirPath = getCurrentDirectory().getAbsolutePath();
            aDirectoryEditField.setText(route_dir_str);
            AlertDialog aDialog =  new AlertDialog.Builder(AISTCPOpenMapPlotter.this)
                .setIcon(R.drawable.alert_dialog_icon)
                .setTitle(R.string.menu_alert_dialog_text_entry)
                .setView(textEntryView)
                .setPositiveButton(R.string.menu_alert_dialog_ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    	String aFilename =  aFilenameEditField.getText().toString();
                    	String aDirPath = aDirectoryEditField.getText().toString();
                    	saveRouteToExternalStorage(aFilename,aDirPath);
                        
                    }
                })
                .setNegativeButton(R.string.menu_alert_dialog_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        /* User clicked cancel so do some stuff */
                    }
                })
                .create();
			  aDialog.show();
           
		}
        /**
         *  prepare to load a route file from external storage
         */
		private void loadRoute_Menu() {
			//deleteRoute_Menu();
			startGpxRouteFilePicker();
		}
		
		/**
		 *  prepare to pick a route file from the external storage 
		 *  result comes back in onActivityResult
		 */
		private void startGpxRouteFilePicker() {
			//this.mArrayTrackWayOverlay.clear();
			//this.mArrayTrackWayOverlay.requestRedraw();
			FilePicker.setFileDisplayFilter(FILE_FILTER_EXTENSION_GPX);
			FilePicker.setFileSelectFilter(null);
			startActivityForResult(new Intent(this, FilePicker.class), SELECT_GPX_ROUTE_FILE);
		}
        /**
         * show the route as a text in an separate activity
         */
		private void showRouteAsText() {
			startActivity(new Intent(this, RouteTextActivity.class));
		}
	   
	   @Override
		public boolean onCreateOptionsMenu(Menu menu) {
			getMenuInflater().inflate(R.menu.menuplotter, menu);
			
		    return true;
		}
	   /**
	    * show the route with big or small symbols depending on mShowRoutePoints
	    * is set in the prefs screen
	    */
	   private void showRoutePoints() {  
		   if (mShowRoutePoints) {
			   this.mRouteItemList.clear();
			   //this.mRouteArrayWayOverlay.clear();  13_03_16
			   this.mWaysOverlay.clearWay(AISPlotterGlobals.DEFAULTROUTE);
			   // this.mArrayRoutePointsOverlay.clear(); 13_03_12
			   this.mItemizedOverlay.clearRouteItems();
			   mShowRoutePoints=false;
			   restoreRouteFromDatabase(AISPlotterGlobals.DEFAULTROUTE);
			   
			   mShowRoutePoints= false;
		   } else {
			   showToastOnUiThread("close the activity and restart ");
			   this.mRouteItemList.clear();
			   //this.mRouteArrayWayOverlay.clear();  13_03_16
			   this.mWaysOverlay.clearWay(AISPlotterGlobals.DEFAULTROUTE);
			   // this.mArrayRoutePointsOverlay.clear(); 13_02_12
			   this.mItemizedOverlay.clearRouteItems();
			   mShowRoutePoints=true;
			   restoreRouteFromDatabase(AISPlotterGlobals.DEFAULTROUTE);
			   
		   }
		   
	   }
	   
	   /**
        *  prepare to load a track file from external storage
        */
		private void loadTrack_Menu() {
			//deleteRoute_Menu();
			startGpxTrackFilePicker();
		}
		
		/**
		 *  prepare to pick a track file from the external storage 
		 *  result comes back in onActivityResult
		 */
		private void startGpxTrackFilePicker() {
			//this.mArrayTrackWayOverlay.clear();
			//this.mArrayTrackWayOverlay.requestRedraw();
			FilePicker.setFileDisplayFilter(FILE_FILTER_EXTENSION_GPX);
			FilePicker.setFileSelectFilter(null);
			startActivityForResult(new Intent(this, FilePicker.class), SELECT_GPX_TRACK_FILE);
		}
		
		/**
		 * 
		 * @param aFile
		 *    aFile which contains a track, selected by FilePicker
		 *    we have to treat it as a virtual AIS-Target, we must give him a mmsi to create a track table
		 *    as is should be restored in onResume
		 *    and clear this mmsi's when the app is finished
		 *    for tesing purpose we assume 00000001
		 */
		public void prepareLoadTrack(File aFile) {
			
			String aInfo = getResources().getString(R.string.guiupdate_info_could_not_load_data_for_default_route);
			try {
				String aMMSI ="00000001";
				mDbAdapter.createShipTrackTable(aMMSI);
				loadTrackDataWithThread(aMMSI,aFile);
				
                
			} catch (SQLException e) {
				
				showToastOnUiThread(aInfo);
			}
		}
		
		/**
		 * 
		 * @param aFile a file that contains the track, selected via the filepicker
		 *    load the track from aFile to the datbase
		 *    uses a thread 
		 *    visual feedback to the user with aprogress bar
		 *    this is not complete, it is derived from loadroute
		 *    status not in function 13_09_26
		 */

		public void loadTrackDataWithThread(String aMMSI,File aFile) {

			final String aFilename = aFile.getAbsolutePath();
			final String theMMSI = aMMSI;
			this.mProgressHorizontal.setVisibility(View.VISIBLE);
			this.mProgressHorizontal.setProgress(0);
			final String aUIMsg = getResources().getString(R.string.guiupdate_info_failed_in_parsing_track_gpx_file);

			new Thread(new Runnable() {

				public void run() {

					try {
						int count = 1000;
						int updateProgressbar = count / 500; // The progressbar is 0 to 100,
						// we calculate the counter if we have to update
						int updateCounter = 0;
						XmlPullParserFactory parserCreator;

						parserCreator = XmlPullParserFactory.newInstance();

						XmlPullParser parser = parserCreator.newPullParser();
						FileReader myReader = new FileReader(aFilename);
						parser.setInput(myReader);
						// parser.setInput(text.openStream(), null);

						int parserEvent = parser.getEventType();
						int pointCounter = -1;

						String aLATStr = null;;
						String aLONStr = null;

						// Parse the XML returned from the file
						while (parserEvent != XmlPullParser.END_DOCUMENT) {
							switch (parserEvent) {
								case XmlPullParser.START_TAG:
									String tag = parser.getName();

									if (tag.compareTo("trkpt") == 0) {
										pointCounter++;
										aLATStr = parser.getAttributeValue(null, "lat");
										aLONStr = parser.getAttributeValue(null, "lon");

										// we initialize time with currentTimeMillis
										long aUTC = System.currentTimeMillis();
										// here we have to insert to a track table
										mDbAdapter.insertTrackPointToTable(theMMSI, aLATStr, aLONStr, aUTC);
										//mDbAdapter.insertRoutePointToTable(AISPlotterGlobals.DEFAULTROUTE, aLATStr, aLONStr, aUtc);

										if (test)
											Log.i(TAG, "   trackpoint=" + pointCounter + " latitude=" + aLATStr
													+ " longitude=" + aLONStr);
										updateCounter++;
										if (updateCounter == updateProgressbar) {
											updateCounter = 0;
											mProgressbarHandler.post(new Runnable() { // we must post , as we could not access the UI
																			// in a separate
																			// thread
												public void run() {
													mProgressHorizontal.incrementProgressBy(1);

												}
											});
										}
									} else if (tag.compareTo("time") == 0) {

										// we do not handle the time

									} else if (tag.compareTo("wpt") == 0) {

										// we do not handle wpt
									}
									break;
							}

							parserEvent = parser.next();
						}

					} catch (FileNotFoundException e) {
						Log.d(TAG, "File not found");
					} catch (Exception e) {
						Log.i(TAG, "Failed in parsing XML", e);
						showToastOnUiThread(aUIMsg);

					}

					mProgressbarHandler.post(new Runnable() { // we must post , as we could not access the UI in a separate
						// thread
						public void run() {
							mProgressHorizontal.setVisibility(View.INVISIBLE);
							// we must not call restoreRouteFromDatabase here
							// there  is a resume() pending, as we selected a file with
							// the file picker activity
							//restoreRouteFromDatabase(DEFAULTROUTE);
						}
					});

				}
			}).start();

		}

		
		@Override
		public boolean onPrepareOptionsMenu(Menu menu){
			super.onPrepareOptionsMenu(menu);
			MenuItem aMenuItem = menu.findItem(R.id.menu_route_show_without_points);
			if (mShowRoutePoints) {
				aMenuItem.setTitle(R.string.route_show_route_without_points);
			} else {
				aMenuItem.setTitle(R.string.route_show_route_with_points);
			}
			/* not used 13_12_10
			 * MenuItem aDataScreenMenuItem = menu.findItem(R.id.menu_showDataScreen);
			if (mShowDataScreenPossible){
				aDataScreenMenuItem.setVisible(true);
				aDataScreenMenuItem.setEnabled(true);
			}else {
				aDataScreenMenuItem.setVisible(false);
				aDataScreenMenuItem.setEnabled(false);
			}*/
				
			
			//wird nicht benötigt, schadet auch nicht
			// Titel des menuItems speedmarker dynamisch ändern
			/* disabled 12_04_18
			 * MenuItem aMenuItem = menu.findItem(R.id.show_speed_marker);
			//CharSequence oldItemTitle = aMenuItem.getTitle();
			//String showTitle = getResources().getString(R.string.showspeedmarker);
			//String hideTitle = getResources().getString(R.string.hidespeedmarker);
			if (mShowSpeedMarkerOfTargets)
			{
				//aMenuItem.setTitle((CharSequence)hideTitle);
				aMenuItem.setTitle(R.string.hidespeedmarker);  // direktes Lesen aus der Resource
			} else
			{
				//aMenuItem.setTitle((CharSequence)showTitle);
				aMenuItem.setTitle(R.string.showspeedmarker);
			}*/
			// Titel des menuItems sattelite map dynamisch ändern
			/* disabled 12_04_18
			 * aMenuItem = menu.findItem(R.id.show_sattelitemap);
			if (mUseSatteliteMap) {
				aMenuItem.setTitle(R.string.hidessattelitemap);
			}else {
				aMenuItem.setTitle(R.string.showssattelitemap);
			}*/
			return true;
		}

		@Override
		public boolean onOptionsItemSelected(MenuItem item) {
			// Handle item selection
		    switch (item.getItemId()) {
		        
	             
		        case R.id.deactivate_old_targets:
		    	     showDialog(Deactivate_OldTargets_DIALOG_ID);
		             return true;
		        
			    case R.id.delete_old_targets:
			    	showDialog(Delete_OldTargets_DIALOG_ID);
			        return true;
			        
			    /*case R.id.show_params: 
			    	showParams();
			        return true;*/
			   /* disabled 12_04_18
			    * case R.id.show_speed_marker:
			    	toggleSpeedMarker();
			    	return true;
			    case R.id.show_sattelitemap:
			    	toggleSatteliteMap();
			    	return true;*/
			    case R.id.show_tracklist:
			    	showTrackList();
			    	return true;
			    case R.id.menu_track_load:
			    	loadTrack_Menu();
			         return true;
			    case R.id.show_targetList:
			    	showTargetList();
			    	return true;
			    case R.id.show_centermap:
		    	     centerMapToShip();
		             return true;
			    case R.id.showLogMonitor :
			    	showMonitor();
			    	return true;
			    case R.id.pick_mapfile:
			    	startMapFilePicker();
			    	return true;
			    case R.id.menu_route:
					return true;
				case R.id.menu_route_edit:
					editRoute_Menu();
					return true;
				case R.id.menu_route_new:
					newRoute_Menu();
					return true;

				case R.id.menu_route_save:
					saveRoute_Menu();
					return true;
				case R.id.menu_route_load:
					loadRoute_Menu();
					return true;
				case R.id.menu_route_delete:
					deleteRoute_Menu();
					return true;
				case R.id.menu_route_show:
					showRouteAsText();
					return true;
				case R.id.menu_route_show_without_points:
					showRoutePoints();
					return true;
				case R.id.menu_route_trace:
					//traceRouteAndFillMapCache();
					traceRouteWithCalculatedStepsAndFillMapcache();
					return true;
				case R.id.menu_screenshot:
					return true;

				case R.id.menu_screenshot_jpeg:
					this.screenshotCapturer.captureScreenShot(CompressFormat.JPEG);
					return true;

				case R.id.menu_screenshot_png:
					this.screenshotCapturer.captureScreenShot(CompressFormat.PNG);
					return true;
				/* not used 13_12_10
				 * case R.id.menu_showDataScreen:
					this.toggleDataScreenVisibitity();
					return true;*/
			    default:
			        return super.onOptionsItemSelected(item);
		    }
		}
		
		/*private void toggleSpeedMarker() {
		   if (mShowSpeedMarkerOfTargets == true) mShowSpeedMarkerOfTargets = false;
		   else mShowSpeedMarkerOfTargets = true;
		}
		
		private void toggleSatteliteMap() {
			if (mUseSatteliteMap == true)mUseSatteliteMap = false;
			else mUseSatteliteMap = true;
			// google mapView.setSatellite(mUseSatteliteMap);
			String prev_show_SatteliteMap_key = getResources().getString(R.string.pref_show_sattelite_map);
	 		String show_Sattelite_DataStr = "";
	 		if (mUseSatteliteMap)show_Sattelite_DataStr = "on";
	 		else show_Sattelite_DataStr = "off";
			prefs.edit()
	     	.putString(prev_show_SatteliteMap_key, show_Sattelite_DataStr)
	 	    .commit(); 
		}*/
		
		/** 
		 * show the data monitor
		 */
		private void showMonitor() {
			Intent i = new Intent(this, MonitoringActivity.class);
	        startActivity(i);	
		}
		
		/**
		 *  center the map to the own ship position
		 */
		private void centerMapToShip() {
			if (test) Log.d(TAG,"Center Map to Ship");
			double aLAT = lastShipPoint.getLatitude();
			double aLON = lastShipPoint.getLongitude();
			// in google :mapController.animateTo(lastShipPoint);
			Log.d(TAG,"center map to ship " + "LAT " + PositionTools.getLATString(aLAT) + " LON " + PositionTools.getLONString(aLON));
			mapController.setCenter(lastShipPoint);
			//mapController.setZoom((int)DEFAULT_ZOOM_LEVEL);
           // mapView.invalidate();
		}
		
		/**
		 * show the AISTargetslist in a separate activity
		 */
		private void showTargetList() {
	        Intent i = new Intent(this, AISListActivity.class);
	        startActivityForResult(i, AISACTIVITY_LIST);
	    }
		
		/**
		 * show the list of all tracks in the datbase
		 */
		private void showTrackList() {
	        Intent i = new Intent(this, TrackListActivity.class);
	        startActivityForResult(i, TRACKACTIVITY_LIST);
	    }
		
		/**
		 *  center map to selected target, must be done in a thread, cause it is called from 
		 */
		private static Handler mCenterMapToSelectedTarget = new Handler();
		
		private Runnable centerMapToSelectedTargetRunnable = new Runnable() {
		    public void run() {
		      Logger.d(TAG,"Show Target on map requested executed delayed");
		      // in google mapController.animateTo(mLastCenteredTargetPoint);
		      if (snapToLocation) {
		    	  showToastOnUiThread("can not center to target while snap to own ship is on"); 
		      }else {
		    	  mapController.setCenter(mLastCenteredTargetPoint);  
		      }
		     // mapView.invalidate();
		      //mCenterMapToSelectedTarget.postDelayed(this,500);
		    }
		  };
		  
	    
	   	/**
	   	 *     
	   	 * @param pMMSIStr
	   	 *    center the map to the target with pMMSIStr
	   	 */
		private void centerMapToSeletedTarget(String pMMSIStr){
			if (mTargetList == null){
				 return;
			 }
			 else {
			   int aMMSI = Integer.parseInt(pMMSIStr);
			   
		       AISTarget aTarget =mTargetList.findTargetByMMSI(aMMSI);
		       String aName = aTarget.getShipname();
		       aTarget.setStatusToDisplay(AISPlotterGlobals.DISPLAYSTATUS_SELECTED);
		       mDbAdapter.updateTarget(aTarget);
		       
		       Logger.d(TAG,"Show Target on map requested " + pMMSIStr + " " + aName);
		       int aLAT = (int)(aTarget.getLAT()*1E6); // in Mikrograd
			   int aLON = (int)(aTarget.getLON()*1E6);
		       GeoPoint geoPoint = new GeoPoint(aLAT,aLON);
		       mLastCenteredTargetPoint = geoPoint;
		       // Since the calling onActivityResult is called before onResume
		       // we can not center the map now, we have to post a handler with a delay
		       // that does the job
		       mCenterMapToSelectedTarget.postDelayed(centerMapToSelectedTargetRunnable,300);
		        
		       
			 }
		}
		/**
		 * central entry after another activity has finished
		 * results come from
		 *  AISACTIVITY_LIST
		 *  TRACKACTIVITY_LIST
		 *  SELECT_MAP_FILE
		 *  SELECT_GPX_ROUTE_FILE
		 */
		
		public void onActivityResult(int requestCode, int resultCode, Intent intent) {
			  // should be executed when returning from AISListActivity
		        if(test) Log.d(TAG, "onActivityResult " + resultCode);
		        if (requestCode == AISACTIVITY_LIST) {
		        // we come from AISListActivity
			        switch (resultCode) {
			          case RESULT_OK:
			        	  break;
			          case AISListActivity.SHOW_TARGET_ON_MAP :
	                      // center to selected target
			        	  Logger.d(TAG,"Center to selected target");
			        	  Bundle extras = intent.getExtras();
			        	  String aMMSIStr = "";
				          aMMSIStr = extras != null ? extras.getString("MMSI")
				                                    : null;
				          
				          if (aMMSIStr != null )centerMapToSeletedTarget(aMMSIStr);
			        	  break;
			           }
		        }
		        if (requestCode == TRACKACTIVITY_LIST) {
			        // we come from TrackListActivity
				        switch (resultCode) {
				          case RESULT_OK:
				        	  break;
				          case TrackListActivity.SHOW_TRACK :
		                      // show the selected track
				        	  Logger.d(TAG,"Show track");
				        	  Bundle extras = intent.getExtras();
				        	  String aMMSIStr = "";
					          aMMSIStr = extras != null ? extras.getString("MMSI")
					                                    : null;
					          
					          // here we have to show the track
					          showToastOnUiThread("here we display track to "+ aMMSIStr);
				        	  break;
				           }
			        }
		        if (requestCode == SELECT_GPX_ROUTE_FILE && resultCode == RESULT_OK && intent != null
						&& intent.getStringExtra(FilePicker.SELECTED_FILE) != null) {
					prepareLoadRoute(new File(intent.getStringExtra(FilePicker.SELECTED_FILE)));

				}
		        if (requestCode == SELECT_GPX_TRACK_FILE && resultCode == RESULT_OK && intent != null
						&& intent.getStringExtra(FilePicker.SELECTED_FILE) != null) {
					prepareLoadTrack(new File(intent.getStringExtra(FilePicker.SELECTED_FILE)));

				}
		        if (requestCode == SELECT_MAP_FILE) {
					if (resultCode == RESULT_OK) {
						if (intent != null && intent.getStringExtra(FilePicker.SELECTED_FILE) != null) {
							String mapfilePath = intent.getStringExtra(FilePicker.SELECTED_FILE);
							String pref_mapfile = getResources().getString(R.string.pref_mapfilename);
							prefs.edit()
							.putString(pref_mapfile, mapfilePath)
					 	    .commit();
							if (mapfilePath.contains("nordrhein-westfalen")) {
								   GeoPoint aGp = new GeoPoint(AISPlotterGlobals.MAP_NRW_CENTER_LAT,AISPlotterGlobals.MAP_NRW_CENTER_LON); // Duisburg
								   int aGoogleLAT = (int) aGp.latitudeE6; // we keep the prefs in Micrograd
							       int aGoogleLON = (int) aGp.longitudeE6;
							       prefs.edit()
							     	.putInt(AISPlotterGlobals.PREV_LAST_GEOPOINT_LAT, aGoogleLAT)
							     	.putInt(AISPlotterGlobals.PREV_LAST_GEOPOINT_LON, aGoogleLON)
							     	.putFloat(AISPlotterGlobals.PREF_ZOOM_FACTOR,12)
							     	.commit();
								}
							if (mapfilePath.contains("duesseldorf-regbez")) {
								   GeoPoint aGp = new GeoPoint(AISPlotterGlobals.MAP_NRW_CENTER_LAT,AISPlotterGlobals.MAP_NRW_CENTER_LON); // Duisburg
								   int aGoogleLAT = (int) aGp.latitudeE6; // we keep the prefs in Micrograd
							       int aGoogleLON = (int) aGp.longitudeE6;
							       prefs.edit()
							     	.putInt(AISPlotterGlobals.PREV_LAST_GEOPOINT_LAT, aGoogleLAT)
							     	.putInt(AISPlotterGlobals.PREV_LAST_GEOPOINT_LON, aGoogleLON)
							     	.putFloat(AISPlotterGlobals.PREF_ZOOM_FACTOR,12)
							     	.commit();
								}
							if (mapfilePath.contains("KoelnWesel")) {
								   GeoPoint aGp = new GeoPoint(AISPlotterGlobals.MAP_NRW_CENTER_LAT,AISPlotterGlobals.MAP_NRW_CENTER_LON); // Duisburg
								   int aGoogleLAT = (int) aGp.latitudeE6; // we keep the prefs in Micrograd
							       int aGoogleLON = (int) aGp.longitudeE6;
							       prefs.edit()
							     	.putInt(AISPlotterGlobals.PREV_LAST_GEOPOINT_LAT, aGoogleLAT)
							     	.putInt(AISPlotterGlobals.PREV_LAST_GEOPOINT_LON, aGoogleLON)
							     	.putFloat(AISPlotterGlobals.PREF_ZOOM_FACTOR,12)
							     	.commit();
								}
							if (mapfilePath.contains("netherlands")) { // Stavoren
									GeoPoint aGp = new GeoPoint(AISPlotterGlobals.MAP_NL_CENTER_LAT,AISPlotterGlobals.MAP_NL_CENTER_LON); // stavoren
									int aGoogleLAT = (int) aGp.latitudeE6; // we keep the prefs in Micrograd
								    int aGoogleLON = (int) aGp.longitudeE6;
								    prefs.edit()
								     	.putInt(AISPlotterGlobals.PREV_LAST_GEOPOINT_LAT, aGoogleLAT)
								     	.putInt(AISPlotterGlobals.PREV_LAST_GEOPOINT_LON, aGoogleLON)
								     	.putFloat(AISPlotterGlobals.PREF_ZOOM_FACTOR,12)
								     	.commit();
							}
							this.mapView1.setMapFile(new File(mapfilePath));
							if ( this.mSeamarksOverlay!= null){
								mSeamarksOverlay.updateSeamarksFile();
							}
							showToastOnUiThread(mapfilePath);
							//this.mapView.setRenderTheme(InternalRenderTheme.OSMARENDER);
						}
					}
					 
					else if (resultCode == RESULT_CANCELED && !this.mapView1.getMapGenerator().requiresInternetConnection()
							&& this.mapView1.getMapFile() == null) {
						finish();
					}
				} 
	    }
		
		@Override
	    public boolean onKeyUp(int keyCode, KeyEvent event) {
	   
			int myKeyCode = keyCode;
			KeyEvent myKeyEvent = event;
	    
	        if  (keyCode == KeyEvent.KEYCODE_BACK)
	        {
	        	AlertDialog.Builder builder = new AlertDialog.Builder(this);
	        	String aQuestion = getResources().getString(R.string.mapplotter_exit_question);
	        	String yesMsg = getResources().getString(R.string.mapplotter_exit_yes);
	        	String noMsg = getResources().getString(R.string.mapplotter_exit_no);
	        	builder.setMessage(aQuestion)
	        	       .setCancelable(false)       
	        	       .setPositiveButton(yesMsg, new DialogInterface.OnClickListener() {           
	        	    	   public void onClick(DialogInterface dialog, int id) {               
	        	    		   AISTCPOpenMapPlotter.this.finish();           
	        	    		   }       
	        	    	   })       
	        	    	   .setNegativeButton(noMsg, new DialogInterface.OnClickListener() {          
	        	    		   public void onClick(DialogInterface dialog, int id) {                
	        	    			   dialog.cancel();           
	        	    			   }       
	        	    		   });
	        	AlertDialog alert = builder.create();
	        	alert.show();
	        	/*Builder builder = new AlertDialog.Builder(this);
				//builder.setIcon(android.R.drawable.ic_menu_info_details);
				builder.setTitle("Tittel beenden");
				builder.setMessage("AISPlotter beenden?");
				builder.setNegativeButton("abbrechen", null);
				builder.setPositiveButton("beenden", new OnClickListener(){
					public void onClick(DialogInterface dialog , int which) {
						setResult(RESULT_OK);
						finish();
						
					}
				});
				AlertDialog alert = builder.create();
				alert.show();
	        	*/
	        	
	            return  true;
	        } 
	        
	        return super.onKeyUp(keyCode, event);
	        //return false;
	    }
		
		/**
		 * TimePickerDialog to set the time before all targets should be deleted
		 */
	   
		private TimePickerDialog.OnTimeSetListener mDeleteTimeSetListener =
		    new TimePickerDialog.OnTimeSetListener() {
		        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
		            Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT")); 
		            Date aDate = new Date(getCurrentUTCNmeaTime());
		            cal.setTime(aDate);
		            cal.set(Calendar.HOUR_OF_DAY, hourOfDay);
		            cal.set(Calendar.MINUTE,minute);
		            Date deleteBeforeDate = cal.getTime(); 
		            mTargetList.deleteOldTargets(deleteBeforeDate);
		            //mapView.invalidate();
		        }
		    };
	  /**
	   * Time picker dialogto set the time before all targets should be deactivated
	   */
	    private TimePickerDialog.OnTimeSetListener mDeactivateTimeSetListener =
		    new TimePickerDialog.OnTimeSetListener() {
		        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
		            Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT")); 
		            Date aDate = new Date(getCurrentUTCNmeaTime());
		            cal.setTime(aDate);
		            cal.set(Calendar.HOUR_OF_DAY, hourOfDay);
		            cal.set(Calendar.MINUTE,minute);
		            Date deactivateBeforeDate = cal.getTime(); 
		            mTargetList.deactivateOldTargets(deactivateBeforeDate);
		            //mapView.invalidate();
		        }
		    };
		
		/**
		 * create the dialogs 
		 */
		
	   @Override
		protected Dialog onCreateDialog(int id) {
		Date aDate;
		Calendar cal;
		int aHour;
		int aMinute;
		   switch (id){
				
				case Delete_OldTargets_DIALOG_ID:
					aDate = new Date(getCurrentUTCNmeaTime());
			        cal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
			        cal.setTime(aDate);
			        aHour = cal.get(Calendar.HOUR_OF_DAY);
			        aMinute = cal.get(Calendar.MINUTE);	
			        return new TimePickerDialog(this,
			                mDeleteTimeSetListener, aHour, aMinute, true);
			        
				case Deactivate_OldTargets_DIALOG_ID:
					aDate = new Date(getCurrentUTCNmeaTime());
			        cal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
			        cal.setTime(aDate);
			        aHour = cal.get(Calendar.HOUR_OF_DAY);
			        aMinute = cal.get(Calendar.MINUTE);	
			        return new TimePickerDialog(this,
			                mDeactivateTimeSetListener, aHour, aMinute, true);
				case Show_Loading_Dialog_ID: {
	                ProgressDialog dialog = new ProgressDialog(this);
	                mProgressDialog = dialog;
	                dialog.setMessage("Please wait while building targets from database...");
	                dialog.setIndeterminate(true);
	                // wird nicht upgatated siehe targetsOnMap-->Run
	                //dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
	                dialog.setCancelable(true);
	                return dialog;
	            }
		
				default:
					return super.onCreateDialog(id);
			}
			
		}
        
	   /**
	    * prepare the dialogs
	    */
		@Override
		protected void onPrepareDialog(int id, Dialog dialog) {
			Date aDate;
			Calendar cal;
			int aHour;
			int aMinute;
			
			switch(id) {
			
			case Delete_OldTargets_DIALOG_ID:
				long fifteenMinutes = 1000*60*15;
				aDate = new Date(getCurrentUTCNmeaTime()- fifteenMinutes);
		        cal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
		        cal.setTime(aDate);
		        aHour = cal.get(Calendar.HOUR_OF_DAY);
		        aMinute = cal.get(Calendar.MINUTE);	
		        ((TimePickerDialog)dialog).updateTime(aHour,aMinute);
		        ((TimePickerDialog)dialog).setTitle(R.string.delete_old_targets);
		        break;
			case Deactivate_OldTargets_DIALOG_ID:
				long tenMinutes = 1000*60*10;
				aDate = new Date(getCurrentUTCNmeaTime()- tenMinutes);
		        cal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
		        cal.setTime(aDate);
		        aHour = cal.get(Calendar.HOUR_OF_DAY);
		        aMinute = cal.get(Calendar.MINUTE);	
		        ((TimePickerDialog)dialog).updateTime(aHour,aMinute);
		        ((TimePickerDialog)dialog).setTitle(R.string.deactivate_old_targets);
		        break;
			}
			super.onPrepareDialog(id, dialog);
		}
		
		/**
		 * Disables the "snap to location" mode.
		 * 
		 * @param showToast
		 *            defines whether a toast message is displayed or not.
		 */
		void disableSnapToLocation(boolean showToast) {
			if (this.snapToLocation) {
				this.snapToLocation = false;
				this.snapToLocationView.setChecked(false);
				this.mapView1.setClickable(true);
				if (showToast) {
					showToastOnUiThread(getString(R.string.snap_to_location_disabled));
				}
			}
		}
  
		/**
		 * Enables the "snap to location" mode.
		 * 
		 * @param showToast
		 *            defines whether a toast message is displayed or not.
		 */
		void enableSnapToLocation(boolean showToast) {
			if (!this.snapToLocation) {
				this.snapToLocation = true;
				//this.mapView1.setClickable(false);
				centerMapToShip();
				if (showToast) {
					showToastOnUiThread(getString(R.string.snap_to_location_enabled));
				}
			}
		}
		
		/**
		 * Returns the status of the "snap to location" mode.
		 * 
		 * @return true if the "snap to location" mode is enabled, false otherwise.
		 */
		boolean isSnapToLocationEnabled() {
			return this.snapToLocation;
		}

		/**
		 * Uses the UI thread to display the given text message as toast notification.
		 * 
		 * @param text
		 *            the text message to display
		 */
		public void showToastOnUiThread(final String text) {

			if (AndroidUtils.currentThreadIsUiThread()) {
				Toast toast = Toast.makeText(this, text, Toast.LENGTH_LONG);
				toast.show();
			} else {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						Toast toast = Toast.makeText(AISTCPOpenMapPlotter.this, text, Toast.LENGTH_LONG);
						toast.show();
					}
				});
			}
		}
		
		
		public void toggleDataScreenVisibitity(){
			// not used 13_12_10 we cant remove the data screen, we have to restart the Activity
			Log.d(TAG,"menu show data screen called");
			if (mShowDataScreenPossible){
				Log.d(TAG,"call setDataScreenVisibility");
				setDataScreenVisbility(!mDataScreenVisible);
			}
		}
		
		public void setDataScreenVisbility(boolean pVisibility){
			if (mShowDataScreenPossible){ // only act if we have a xlarge Screen
				if (pVisibility)
				{
					Log.d(TAG,"show data screen");
					mDataScreenVisible = true; 
					mOSM_info_LAT.setVisibility(View.VISIBLE);
					mOSM_info_LON.setVisibility(View.VISIBLE);
					mOSM_info_Windspeed.setVisibility(View.VISIBLE);
					mOSM_info_Winddirection.setVisibility(View.VISIBLE);
					mOSM_info_Depth.setVisibility(View.VISIBLE);
					mOSM_info_SOG.setVisibility(View.VISIBLE);
					mOSM_info_COG.setVisibility(View.VISIBLE);

				} else {
					Log.d(TAG,"hide Data Screen");
					mDataScreenVisible = false;
					mOSM_info_LAT.setVisibility(View.INVISIBLE);
					mOSM_info_LON.setVisibility(View.INVISIBLE);
					mOSM_info_Windspeed.setVisibility(View.INVISIBLE);
					mOSM_info_Winddirection.setVisibility(View.INVISIBLE);
					mOSM_info_Depth.setVisibility(View.INVISIBLE);
					mOSM_info_SOG.setVisibility(View.INVISIBLE);
					mOSM_info_COG.setVisibility(View.INVISIBLE); 
				}
			} else {
				Log.d(TAG,"Data screen not available");
			}
		}	
				
		
}
