package com.klein.service;
/**
 * @author Vklein 2011
 * basiert auf aisdecoder delphi_03
 */
import java.text.DecimalFormat;

import android.util.Log;

import com.klein.commons.AISPlotterGlobals;
import com.klein.commons.Entity;
import com.klein.commons.PositionTools;
import com.klein.logutils.Logger;



public class AISTarget extends Entity {
	// beachte LAT, LON in Grad
	// COG in 1/10 Grad
	// SOG in 1/10 Knots
	// HDG in Grad
	private static final String TAG ="AISTarget";
	private final boolean test = false;
	int theLastMSGType = 0;
    int theRepeatIndicator = 0;
    long theMMSI = 0;
    byte theNavigationStatus =0;
    int theRateOfTurn =0;
    int theSOG =0;  // Speed over ground in 1/10 knot steps (0-102.2 knots)
    byte theAccuracy=0;
    double theLON =0;  // in Grad!!! 10.1054 Latitude in 1/10 000 min wird gewandelt  war vertauscht korr 2014_02_03
    double theLAT=0;  //    in Grad!!! 54.1254 Longitude in 1/10 000 min wird gewandelt
    int theCOG=0;  // Course over ground in 1/10 = (0-3599). 
    int theHDG =0;  // Degrees (0-359) 
    byte theSecondOfUTC=0;
    byte theManuever=0;
    byte theAIS_Version=0;
    long theIMO=0;
    String theCallSign="";
    String theShipname="";
    byte theShipType=0;
    int to_bow=0;
    int to_stern=0;
    int to_port=0;
    int to_starboard=0;
    byte thePosFixType=0;
    byte theETAMonth=0;
    byte theETAday=0;
    byte theETAHour=0;
    byte theETAminute=0;
    int theDraught=0;
    String theDestination="";
    byte theDTE=0;
    int statusToDisplay = 0;
    long mTimeOfLastStaticUpdate = 0;
    long mTimeOfLastPositionReport = 0;;
    boolean mHasTrack = false;
    
    public AISTarget (long id ) {
		super(id);
		if (test) Log.v(TAG,"AIStarget-->create" + id);
    }
    
    /**
     * 
     * @param id
     * @param aMMSI
     * @param aLON   in degrees (double)
     * @param aLAT   in degrees (double)
     * @param aName
     * @param aSOG
     * @param aCOG
     * @param aHDG
     */
    
    public AISTarget (long id, long aMMSI, double aLON, double aLAT, String aName,
    		int aSOG, int aCOG, int aHDG) {
    	super(id);
    	theMMSI = aMMSI;
    	theLON = aLON;
    	theLAT = aLAT;
    	theShipname = aName;
    	theSOG = aSOG;
    	theCOG = aCOG;
    	theHDG = aHDG;
    }
    
    /** 
     *  we set the params from the database at the beginning
     * @param rowID
     * @param aMMSI
     * @param aName
     * @param aLAT   as a String 
     * @param aLON   as a String e.g LON in GradMinuteNotation 54'10,345 as stored in the database
     * @param aSOG
     * @param aCOG
     * @param aHDG
     */
    
    public AISTarget (String rowID, String aMMSI, String aName, 
    		          String aLAT, String aLON, String aSOG, String aCOG, String aHDG, long aUTC, int aDisplayStatus,byte aNavStatus,byte aShipType, boolean hasTrack){
    	super(-1l);
    	long aID = Long.parseLong(rowID);
    	this.setId(aID);
    	theMMSI = Long.parseLong(aMMSI);
    	theShipname = aName;
    	try {
    		// LON in GradNotation 54'10,345 wird gewandelt in 
	    	String[] fieldsLON = aLON.split("'");
	    	double gradLON = Double.parseDouble(fieldsLON[0]);
	    	String minStrLON = fieldsLON[1];
	    	minStrLON = minStrLON.replace(",", ".");
	    	double  minLON = Double.parseDouble(minStrLON);
	        theLON = gradLON + minLON/60l;
	    	if (fieldsLON[3].contains("W")) theLON = -theLON;
	    	
	    	String[] fieldsLAT = aLAT.split("'");
	    	double gradLAT = Double.parseDouble(fieldsLAT[0]);
	    	String minStrLAT = fieldsLAT[1];
	    	minStrLAT = minStrLAT.replace(",", ".");
	    	double  minLAT = Double.parseDouble(minStrLAT);
	    	theLAT = gradLAT + minLAT/60l;
	    	if (fieldsLAT[3].contains("S")) theLAT = -theLAT;
	    	
	    	aSOG = aSOG.replace(",", ".");
	    	double sog = Double.parseDouble(aSOG);
	    	sog = sog * 10d;
	    	theSOG = (int) sog;
	    	
	    	aCOG = aCOG.replace(",", ".");
	    	double cog = Double.parseDouble(aCOG);
	    	cog = cog * 10d;
	    	theCOG = (int) cog;
	    	theHDG = Integer.parseInt(aHDG);
	    	
	    	mTimeOfLastStaticUpdate = aUTC;
	    	mTimeOfLastPositionReport= aUTC;
	    	statusToDisplay = aDisplayStatus;
	    	theShipType = aShipType;
	    	theNavigationStatus =aNavStatus;
	    	mHasTrack = hasTrack;
    	}
    	catch (NumberFormatException e) { 
    	  String result = e.toString();
    	  Log.v(TAG,result);
    	
    	}
    	
    	
    	
    }
    
    public void setParamsFromNMEA(double aLON, double aLAT){
    	theLON = aLON;
    	theLAT = aLAT;
    }
    
    public void setLAT (double aLAT){
    	theLAT = aLAT;
    }
    
    public void setLON (double aLON){
    	theLON = aLON;
    }
    
    public void setSOG (int aSOG){
    	theSOG = aSOG;
    }
    
    public void setSpeedInKnots(float aSpeed){
      // aSpeed is the  speed of the device over ground in meters/second
    	theSOG = (int)(aSpeed * 10.0f);
    }
    
    public void setSpeedInMetersPerSecond(float aSpeed){
      // aSpeed is the  speed of the device over ground in meters/second
      // SOG: Speed over ground in 1/10 knot steps (0-102.2 knots) 
      // 1m/s = 1,9438kts 1kts = 0, 51444m/s
      float knots = aSpeed * 1.9438f;
      theSOG = (int)(knots * 10.0f);
    }
    
    
    public void setCOGInDegrees(float aBearing) {
    	// aBearing is the direction of travel in degrees East of true North
    	// Course over ground in 1/10 = (0-3599).
    	theCOG = (int) (aBearing * 10);
    }
    
    private long bitStringToLong(String aBitstr) {
    	
    	long value = 0;
    	int aLength = aBitstr.length();
    	for (int i= 0;i<aLength;i++){
          char ch = aBitstr.charAt(i);
          if (ch == '1')
           { value = value *2 +1;}
          else {value = value * 2;}
    	}
    	return value;
    }
    
    char sixBitToAscii(String sixBitString){
    	int index = 0;
    	char ch =' ';
    	index = (int) bitStringToLong(sixBitString);
    	if ((index >= 0)&&(index <= 31)) {
    	  ch = (char)(64 + index);
    	}
    	if ((index >= 32) && (index <= 63)){
    		ch = (char)(index);
    	}
    	return ch;
    }
    
    String sixBitToAsciiString(String sixBitString) {
    	String aSixBitString = sixBitString;
    	String temp = aSixBitString.substring(0,6);
    	String result = "";
    	while ((aSixBitString.length() > 5 )&& (!temp.equals("000000"))){
    		result = result + sixBitToAscii(temp);
    		//sixBitString = sixBitString.substring(6,(sixBitString.length() - 6));
    		aSixBitString = aSixBitString.substring(6);
    		if (aSixBitString.length() >= 6) {
    			temp = aSixBitString.substring(0,6);
    		}
    	}
    	return result;
    }

    /*B.3.5 Organising the Binary Message Data
    The work sheet has been filled in to decode an "AIS Message 1". Notice that the two grids in
    Figure B.2 have a variety of shaded (coloured) blocks. This was done to make it easier to
    locate the specific bits making up the message 1 parameters in the decoded array of binary
    bits. The fact is, these blocks could not be filled in until the message type (message number)
    of AIS message was identified. Identification of the AIS message is done from the first six bits
    of the binary Message Data. The message number is simply the decimal equivalent of the
    binary number. In this case, 000001 = message 1. After this is known the remaining blocks of
    the message can be shaded using information in table 15, ITU-R M.1371-1.
    The parameters listed in table 15, ITU-R M.1371-1 are transmitted over the radio link as
    Message Data in the same order that they are listed in the table. The "Number of bits" column
    of table 15, ITU-R M.1371-1 used to establish the bits that apply to each of the parameters in
    the table (refer to table 15, ITU-R M.1371-1):
    1) Message ID, bits 1-6
    2) Repeat Indicator, bits 7-8
    3) User ID, bits 9-38
    4) Navigation status, bits 39-42
    5) Rate of turn, bits 43-50
    6) SOG, bits 51-60
    7) Position accuracy, bit 61
    8) Longitude, bits 62-89
    9) Latitude, bits 90-116
    10) COG, bits 117-128
    11) True Heading, bits 129-137
    12) UTC second when report generated, bits 138-143
    13) Regional Application, bits 144-147
    14) Spare, bit 148
    15) RAIM Flag, bit 149
    16) Communications State, bits 150-168
    Once established, this ordering of bits will always be the same for a "message 1". That is,
    until the reference table itself is changed by a revision action of the ITU.*/
    
    
    public void setInfoFromPositionReport123 (String sixBitString){
    	/** THis infois from nl.esi.metis.aisparser.impl
    	 * This is the base class implementation for all AIS position report messages. It implements the fields that they all have in common. 
    	 * <pre>
    	 * Field Nr     Field Name                          NrOf Bits   (from,  to)
    	 * ------------------------------------------------------------------------
    	 *  1           messageID                                  6    (   1,   6)
    	 *  2           repeatIndicator                            2    (   7,   8)
    	 *  3           userID                                    30    (   9,  38)
    	 *  4           navigationalStatus                         4    (  39,  42)
    	 *  5           rateOfTurn                                 8    (  43,  50)
    	 *  6           speedOverGround                           10    (  51,  60)
    	 *  7           positionAccuracy                           1    (  61,  61)
    	 *  8           longitude                                 28    (  62,  89)
    	 *  9           latitude                                  27    (  90, 116)
    	 * 10           courseOverGround                          12    ( 117, 128)
    	 * 11           trueHeading                                9    ( 129, 137)
    	 * 12           timeStamp                                  6    ( 138, 143)
    	 * 13           specialManoeuvre                           2    ( 144, 145)
    	 * 14           spare                                      3    ( 146, 148)
    	 * 15           raimFlag                                   1    ( 149, 149)
    	 * 16           communicationState                        19    ( 150, 168)
    	 *                                                       ---- +
    	 *                       (maximum) number of bits        168
    	 * </pre>
    	 * 
    	 * @author Pierre van de Laar
    	 * @author Pierre America
    	 * @author Brian C. Lane
    	 */
    	
    	if (test) Log.v(TAG, "SetInfoFromPosition"); 
    	if (!(sixBitString.length() == 168)) return;
    	String tempstr = "";
    	tempstr = sixBitString.substring (0,6);
        theLastMSGType = (int) bitStringToLong(tempstr);
        if (theLastMSGType != 1)
        {
        	//Logger.d(TAG, "MSGType " + theLastMSGType); 12_02_06 
        }
        tempstr = sixBitString.substring (6,8);
        theRepeatIndicator = (int) bitStringToLong(tempstr);
        tempstr = sixBitString.substring (8,38);
        theMMSI = bitStringToLong(tempstr);
        tempstr = sixBitString.substring (38,42);
        theNavigationStatus =  (byte) bitStringToLong(tempstr);
        tempstr = sixBitString.substring (42,50);
        theRateOfTurn = (int) bitStringToLong(tempstr);
        tempstr = sixBitString.substring (50,60);
        theSOG = (int) bitStringToLong(tempstr);
        tempstr = sixBitString.substring (60,61);
        theAccuracy = (byte) bitStringToLong(tempstr);
        tempstr = sixBitString.substring (61,89);
        theLON =  ((double)bitStringToLong(tempstr))/10000/60; // in 1/10000 min --> Grad
        tempstr = sixBitString.substring (89,116);
        theLAT =  ((double)bitStringToLong(tempstr))/10000/60; // in 1/10000 min --> Grad
        tempstr = sixBitString.substring (116,128);
        theCOG =  (int)bitStringToLong(tempstr);
        tempstr = sixBitString.substring (128,137);
        theHDG =  (int)bitStringToLong(tempstr);
//        if ((theLON < 5.0)|| theLAT < 50.00){
//        	Logger.d(TAG, "curious data  ");
//        	Logger.d (TAG, "MMSI " + theMMSI);
//        	Logger.d(TAG," lon " + theLON + " lat " + theLAT);
//        }
        // Blue Flag in Europe region may be 0 no info, 1 not blue flag , 2 blue flag set
        tempstr = sixBitString.substring (144,145);
        theManuever = (byte)bitStringToLong(tempstr); 
        if ((theLON == 181.0)|| theLAT == 91.0){
        	Logger.d(TAG, "false data ");
        	Logger.d (TAG, "MMSI " + theMMSI);
        	Logger.d(TAG," lon " + theLON + " lat " + theLAT);
        }
        
       
    }
    
    public void setInfoFromPositionReport_18_19 (String sixBitString){
    	if (test) Log.v(TAG, "SetInfoFromPosition"); 
    	if (!(sixBitString.length() == 168)) return;
    	String tempstr = "";
    	tempstr = sixBitString.substring (0,6);
        theLastMSGType = (int) bitStringToLong(tempstr);
        tempstr = sixBitString.substring (6,8);
        theRepeatIndicator = (int) bitStringToLong(tempstr);
        tempstr = sixBitString.substring (8,38);
        theMMSI = bitStringToLong(tempstr);
        tempstr = sixBitString.substring (38,46);
        theNavigationStatus =  (byte) bitStringToLong(tempstr);
        //tempstr = sixBitString.substring (42,50);
        //theRateOfTurn = (int) bitStringToLong(tempstr);
        tempstr = sixBitString.substring (46,56);
        theSOG = (int) bitStringToLong(tempstr);
        tempstr = sixBitString.substring (56,57);
        theAccuracy = (byte) bitStringToLong(tempstr);
        tempstr = sixBitString.substring (57,85);
        theLON =  ((double)bitStringToLong(tempstr))/10000/60;
        tempstr = sixBitString.substring (85,112);
        theLAT =  ((double)bitStringToLong(tempstr))/10000/60;
        tempstr = sixBitString.substring (112,124);
        theCOG =  (int)bitStringToLong(tempstr);
        tempstr = sixBitString.substring (124,133);
        theHDG =  (int)bitStringToLong(tempstr);
        
    }
    
    public void setInfoFromClassAShipReport(String sixBitString){
 
     if (test) Log.v(TAG, "SetInfoFromShip");
     if (!(sixBitString.length() == 426)) return;
     String tempstr ="";
     tempstr = sixBitString.substring (0,6);
     theLastMSGType =  (int)bitStringToLong(tempstr);
     tempstr = sixBitString.substring (6,8);
     theRepeatIndicator =  (int)bitStringToLong(tempstr);
     tempstr = sixBitString.substring (8,38);
     theMMSI =  bitStringToLong(tempstr);
     if (test) Log.v(TAG, "SetInfoFromShip MMSI " + theMMSI);
     tempstr = sixBitString.substring (38,40);
     theAIS_Version =  (byte)bitStringToLong(tempstr);
     tempstr = sixBitString.substring (40,46);
     theIMO =  (int)bitStringToLong(tempstr);
     tempstr = sixBitString.substring (70,112);
     theCallSign =  sixBitToAsciiString(tempstr);
     tempstr = sixBitString.substring (112,232);
     theShipname =  sixBitToAsciiString(tempstr);
     if (test) Log.v(TAG, "SetInfoFromShip Name "+ theShipname);
     tempstr = sixBitString.substring (232,240);
     theShipType = (byte) bitStringToLong(tempstr);
     tempstr = sixBitString.substring (240,249);
     to_bow = (int) bitStringToLong(tempstr);
     tempstr = sixBitString.substring (249,258);
     to_stern = (int) bitStringToLong(tempstr);
     tempstr = sixBitString.substring (258,264);
     to_port = (int) bitStringToLong(tempstr);
     tempstr = sixBitString.substring (264,270);
     to_starboard = (int) bitStringToLong(tempstr);
     tempstr = sixBitString.substring (294,302);
     theDraught = (int) bitStringToLong(tempstr);
     //tempstr = sixBitString.substring (302,422);
     tempstr = sixBitString.substring(302,sixBitString.length());
     theDestination = sixBitToAsciiString(tempstr);
     
     }
    
    public void setInfoFromClassBShipReport(String sixBitString){
   	 
        if (test) Log.v(TAG, "SetInfoFromShip");
        //if (!(sixBitString.length() == 168)) return;
        String tempstr ="";
        tempstr = sixBitString.substring (0,6);
        theLastMSGType =  (int)bitStringToLong(tempstr);
        tempstr = sixBitString.substring (6,8);
        theRepeatIndicator =  (int)bitStringToLong(tempstr);
        tempstr = sixBitString.substring (8,38);
        theMMSI =  bitStringToLong(tempstr);
        if (test) Log.v(TAG, "SetInfoFromShip MMSI " + theMMSI);
        tempstr = sixBitString.substring (38,40);
        int aPartNumber = (int)bitStringToLong(tempstr);
        if (aPartNumber == 0) {
	        	// Part A
	        	tempstr = sixBitString.substring (40,160);
	        	theShipname =  sixBitToAsciiString(tempstr);
	        } else {
	        	//Part B
	        	tempstr = sixBitString.substring (40,48);
	        	theShipType = (byte) bitStringToLong(tempstr);
	        	tempstr = sixBitString.substring (48,90);
	        	String aVendorID = sixBitToAsciiString(tempstr);
	        	tempstr = sixBitString.substring (90,132);
	        	theCallSign =  sixBitToAsciiString(tempstr);
	        	tempstr = sixBitString.substring (132,141);
	        	to_bow = (int) bitStringToLong(tempstr);
	        	tempstr = sixBitString.substring (141,150);
	            to_stern = (int) bitStringToLong(tempstr);
	            tempstr = sixBitString.substring (150,156);
	            to_port = (int) bitStringToLong(tempstr);
	            tempstr = sixBitString.substring (156,162);
	            to_starboard = (int) bitStringToLong(tempstr);
	            
	        }
        }
    
    public void setInfoFromBaseStationReport (String sixBitString){
    	// base station report
    	if (test)Log.v(TAG,"aisplotter --> decodeMsg_4 begin");
    
    	if (!(sixBitString.length() == 168)) return;
    	String tempstr = "";
    	tempstr = sixBitString.substring (0,6);
        theLastMSGType = (int) bitStringToLong(tempstr);
        tempstr = sixBitString.substring (6,8);
        theRepeatIndicator = (int) bitStringToLong(tempstr);
        tempstr = sixBitString.substring (8,38);
        theMMSI = bitStringToLong(tempstr);
        tempstr = sixBitString.substring (38,52);
        int aUTCYear = (int )bitStringToLong(tempstr);
        tempstr = sixBitString.substring (52,56);
        int aUTCMonth = (int )bitStringToLong(tempstr);
        tempstr = sixBitString.substring (56,61);
        int aUTCDay = (int )bitStringToLong(tempstr);
        tempstr = sixBitString.substring (61,66);
        int aUTCHour = (int )bitStringToLong(tempstr);
        tempstr = sixBitString.substring (66,72);
        int aUTCMinute = (int )bitStringToLong(tempstr);
        tempstr = sixBitString.substring (72,78);
        int aUTCSecond = (int )bitStringToLong(tempstr);
        tempstr = sixBitString.substring (78,79);
        //Logger.d(TAG,"baseStation " + theMMSI);
        
        theShipname = "Base Station";
        //Logger.d(TAG,"Date " + aUTCHour + ":" + aUTCMinute + ":" + aUTCSecond );
        theAccuracy = (byte) bitStringToLong(tempstr);
        tempstr = sixBitString.substring (79,107);
        theLON =  ((double)bitStringToLong(tempstr))/10000/60;
        tempstr = sixBitString.substring (107,134);
        theLAT =  ((double)bitStringToLong(tempstr))/10000/60;
        tempstr = sixBitString.substring (134,138);
        int aTypeOfFixing =  (int)bitStringToLong(tempstr);
        
    }
    
    
    public int getLastMsgType() {
    	return theLastMSGType;
    }
    
     public long getMMSI() {
    	return theMMSI; 
     }
     public String getMMSIString () {
    	 return PositionTools.customFormat ("000000000",getMMSI());
     }
     
     public int getSOG() {
    	return theSOG;
     }
     
     public String getSOGString() {
    	 return PositionTools.customFormat ("#0.0",getSOG()/10);
     }
     
     
  
     public double getLON(){
    	 return theLON;
     }

	 public String getLONString() {
		  double aPos = getLON();
		  double grad = Math.floor(aPos);
		  double minutes = (aPos - grad) * 60;
		  StringBuffer sb = new StringBuffer();
		  sb.append(customFormat ("000" , grad));
		  sb.append("\' ");
		  sb.append(customFormat ("00.000",minutes));
		  sb.append("\''");
		  // String aPosStr = format("%.0f", grad) + "° " +format("%2.3f",minutes);
		  if (aPos > 0) {
			  sb.append(" E");
		  } else {
			  sb.append(" W");
		  }
		  return sb.toString();
	  }
  
	  public double getLAT(){
	 	 return theLAT;
	  }
	  
	  public String customFormat(String pattern, double value ) {
	      DecimalFormat myFormatter = new DecimalFormat(pattern);
	      String output = myFormatter.format(value);
	      return output;
	  }
  
	  public String getLATString() {
		  double aPos = getLAT();
		  double grad = Math.floor(aPos);
		  double minutes = (aPos - grad) * 60;
		  StringBuffer sb = new StringBuffer();
		  sb.append(customFormat ("000" , grad));
		  sb.append("\' ");
		  sb.append(customFormat ("00.000",minutes));
		  sb.append("\''");
		  // String aPosStr = format("%.0f", grad) + "° " +format("%2.3f",minutes);
		  if (aPos > 0) {
			  sb.append(" N");
		  } else {
			  sb.append(" S");
		  }
		  return sb.toString();
	  }
	 
	  public int getROT(){
		 	 return theRateOfTurn;
		  }
  
	  public int getCOG(){
		 	 return theCOG;
		  }
	  
	  public String getCOGString() {
		  return PositionTools.customFormat ("000.0",getCOG()/10);
	  }
  
	  public int getHDG(){
		 	 return theHDG;
		  }
	  
	  public String getHDGString() {
		  return PositionTools.customFormat ("000",getHDG());
	  }

	  public String getShipname(){
		 	 return theShipname;
		  }
	  
	  public byte getShiptype(){
		 	 return theShipType;
		  }
      
	  public int getLength() {
		  return to_bow + to_stern;
	  }
	  
      public int getWidth() {
    	  return to_port + to_starboard;
      }
      
      public int getDraught() {
    	  return theDraught;
      }

      public String getDestination() {
    	  return theDestination;
      }
   
      public String getShipTypeString(){
    	  int index = getShiptype();
    	  return getShipTypeStringFromIndex(index);  
      }
      
      public  String getShipTypeStringFromIndex( int aIndex){
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
      
      public byte getNavStatus(){
       return theNavigationStatus;
      }
      /**
       * last source update 2014_01_22
       * return a SART-Alarm-info with status = 14
       * return the Nav status as a string
       * @return
       */
      
      public String getNavStatusString (){
    	  int index = getNavStatus();
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
    	  if ((index >= 11)&&(index <= 13)) return "Reserved for future use";
    	  if (index == 14) return "AIS-SART";
    	  if (index == 15) return "Not defined (default)";
    	 return "not defined may not occur"; 
      }
      
      public byte getManueverStatus() {
    	  return theManuever;
      }
      
      
      public int getStatusToDisplay (){
    	  return statusToDisplay;
      }
      
      public void setStatusToDisplay ( int pStatus){
    	  statusToDisplay = pStatus;
      }
      
      public void toggleDisplayStatus() {
    	  if (statusToDisplay == AISPlotterGlobals.DISPLAYSTATUS_HAS_SHIPREPORT)  statusToDisplay = AISPlotterGlobals.DISPLAYSTATUS_SELECTED;
		  else if (statusToDisplay == AISPlotterGlobals.DISPLAYSTATUS_SELECTED)statusToDisplay = AISPlotterGlobals.DISPLAYSTATUS_HAS_SHIPREPORT ;
      }
      
  
      public void setTimeOfLastStaticUpdate(long pTime){
    	  mTimeOfLastStaticUpdate = pTime;
      }
      public long getTimeOfLastStaticUpdate()
      {
    	  return mTimeOfLastStaticUpdate;
      }
      public void setTimeOfLastPositionReport(long pTime) {
    	  mTimeOfLastPositionReport = pTime;
      }
      
      public long getTimeOfLastPositionReport() {
    	  return mTimeOfLastPositionReport;
      }
      
      public boolean getHasTrack() {
    	  return mHasTrack;
      }
      
      public void setHasTrack(boolean hasTrack){
    	  mHasTrack = hasTrack;
      }
      
      @Override
      public String toString(){
    	StringBuffer buf = new StringBuffer();
      	
      	buf.append("MMSI " + getMMSI()     + "# ");
      	buf.append("LAT  " + getLATString()+ "# ");
      	buf.append("LON  " + getLONString()+ "# ");
      	buf.append("COG  " + customFormat ("000.0",getCOG()/10)+ "# "); 
      	buf.append("SOG  " + customFormat ("000.0",getSOG()/10)+ "# ");
      	buf.append("HDG  " + customFormat ("000",getHDG())  + "# ");
      	buf.append("Name " + getShipname() + "# ");
        return buf.toString();
      }
      

   
}
