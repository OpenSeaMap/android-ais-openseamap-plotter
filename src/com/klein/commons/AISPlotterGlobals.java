package com.klein.commons;

public class AISPlotterGlobals {
	// actions and keys for the BroadcastReceiver mNMEAParserDataReceiver
	public static final String myPackagename = "com.klein.aistcpopenmapplotter051";
	public static final String ACTION_GPSNMEADATA = myPackagename + ".GPSNMEADATA";
	public static final String ACTION_UPDATEGUI = myPackagename + ".UPDATEGUI";
	
	public static final String ACTION_INITGUI = myPackagename + ".INITGUI";
	public static final String ACTION_DELETE_TARGET = myPackagename + ".DELETETARGET";
	public static final String ACTION_SENDGPSNMEADATA = myPackagename + ".SENDGPSNMEADATA";
	public static final String ACTION_SHOW_AISMSG_INLOG = myPackagename + ".SHOWAISMSGINLOG";
	public static final String GUIUpdateEvent_MMSI_KEY = "mmsi_key";
	public static final String GUIUpdateEvent_NAME_KEY = "name_key";
	public static final String GUIUpdateEvent_STATUS_KEY = "status_key";
	public static final String GUIUpdateEvent_LAT_KEY = "lat_key" ;
	public static final String GUIUpdateEvent_LON_KEY = "lon_key";
	public static final String GUIUpdateEvent_COG_KEY = "cog_key";
	public static final String GUIUpdateEvent_SOG_KEY = "sog_key";
	public static final String GUIUpdateEvent_DEPTH_KEY = "depth_key";
	public static final String GUIUpdateEvent_WIND_ANGLE_KEY = "wind_angle_key";
	public static final String GUIUpdateEvent_WIND_SPEED_KEY = "wind_speed_key";
	public static final String GUIUpdateEvent_SOGSTR_KEY = "sogstr_key";
	public static final String GUIUpdateEvent_COGSTR_KEY = "cogstr_key";
	public static final String GUIUpdateEvent_LASTPOSUTC_KEY = "lastposutc_key";
	public static final String GUIUpdateEvent_HASTRACK_KEY = "hastrack_key";
	public static final String GUIUpdateEvent_SPECIAL_MANUEVER_KEY = "special_manuever_key";
	public static final String GUIUpdateEvent_SHIP_LENGTH_KEY = "special_shiplength_key";
	public static final String GUIUpdateEvent_SHIP_WIDTH_KEY = "special_shipwidth_key";
	public static final String GUIUpdateEvent_NAV_STATUS_KEY = "nav_status_key";
	
	// mmsi of myShip is to identify myShip as a AISTarget, so it can 
	// be displayed on the AIS-Layer
	public static final long  myShipMMSI = 123456789;
	
	// used with the logger and the TCP-Service to tell the parser
	// if he should log the decoded ais-messages
	public static final String SHOW_AISMSG_INLOG_KEY = "mustlog";
	
	// display status for paint-objects and OverlayItems
     public static final int DISPLAYSTATUS_0 = 0; // nicht angezeigt
	 public static final int DISPLAYSTATUS_INACTIVE = 1; // durchgestrichen, zu alt
	 public static final int DISPLAYSTATUS_HAS_POSITION = 2; // nur PositionReport, Rahmen
	 //public static final int DISPLAYSTATUS_MOORED = 3; // cyan
	 public static final int DISPLAYSTATUS_HAS_SHIPREPORT = 4; // ShipReport, also ausgefüllt
	 public static final int DISPLAYSTATUS_SELECTED = 5; // mit Name und Kreis
	 													// rot, wenn innerhalb des Alarmkreises
	 public static final int DISPLAYSTATUS_BASE_STATION = 10;
	 public static final int DISPLAYSTATUS_OWN_SHIP = 100;
	 
	 public static final int NO_GPS = 0;
	 public static final int INTERNAL_GPS = 1;
	 public static final int NMEA_GPS = 2;
	 
	 public static final String PREF_ZOOM_FACTOR ="zoom_factor";
	 public static final String PREV_LAST_GEOPOINT_LAT ="lastgeopointlat";
	 public static final String PREV_LAST_GEOPOINT_LON ="lastgeopointlon";
	 
	 public static final double MAP_NRW_CENTER_LAT = 51.466; // Duisburg
	 public static final double MAP_NRW_CENTER_LON = 6.728;
	 public static final double MAP_NL_CENTER_LAT =  52.884;  // Stavoren
	 public static final double MAP_NL_CENTER_LON = 5.4823;
	 
	 public static final String DEFAULTROUTE = "0000";
	 public static final String DEFAULT_APP_DATA_DIRECTORY ="AISPlotter";
	 public static final String DEFAULT_ROUTE_DATA_DIRECTORY = DEFAULT_APP_DATA_DIRECTORY + "/Routedata";
	 public static final String DEFAULT_TRACK_DATA_DIRECTORY = DEFAULT_APP_DATA_DIRECTORY + "/Trackdata";
	 public static final String DEFAULT_BACKUP_CACHE_DATA_DIRECTORY = DEFAULT_APP_DATA_DIRECTORY + "/BackupCachedata";
	 public static final String DEFAULT_MAP_DATA_DIRECTORY = DEFAULT_APP_DATA_DIRECTORY + "/Mapdata";
	 public static final String DEFAULT_ONLINE_TILE_CACHE_DATA_DIRECTORY = DEFAULT_APP_DATA_DIRECTORY+ "/CacheData";
	 
	 public static final String DEFAULT_SEAMARKS_SYMBOL_FILENAME = "symbols.xml";
	 public static final String DEFAULT_STANDRAD_RENDERER_FILENAME = "openseamaprenderer001.xml";
	 
	 public static final String OPENSEAMAP_TESTMAP ="Testkarte_Wismarbucht";
}
