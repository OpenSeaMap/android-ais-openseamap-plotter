package com.klein.logutils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;


import android.util.Log;


public class Logger {
	public static boolean enabled = true;
	public static final String KEY_IS_LOG_ENABLED = "is_log_enabled";
	public static final String KEY_IS_AISMSG_LOG_ENABLED = "is_aismsg_log_enabled";
	private static final boolean DEBUG = false;
	private static final String TAG = "Logger";
	
	private static final int MAX_LOG_ENTRIES = 200;

	private static ArrayList<LogListener> listeners = new ArrayList<LogListener>();
	private static List<String> log = Collections.synchronizedList(new LinkedList<String>());
	private static int logSize = 0;

	
	public static void d(String tag, String msg){
		String text = tag + ": " + msg;
		if (DEBUG) 
			Log.d(TAG, text);
		if (enabled)
			add(text);
		
	}
	
	public static void d(String msg){
		if (DEBUG) 
			Log.d(TAG, msg);
		if (enabled) 
			add(msg);
	}
	
	private static void add(String msg){
		synchronized (log){
			if (logSize < MAX_LOG_ENTRIES){
				logSize++;
			}
			else {
				// we don't check if elements are present, since we trust logSize
				log.remove(0);
				//log.removeFirst(); 
			}
			log.add(msg);
		}
		notifyListeners(msg.toString());
	}
	
	public static synchronized void clear(){
		logSize = 0;
		log.clear();
	}
	
	public static String getLog(){
		StringBuilder sb = new StringBuilder();
		synchronized(log){
			ListIterator<String> iter = log.listIterator();
			while (iter.hasNext()){
				sb.append(iter.next());
				sb.append("\n");
			}
		}
		
		return sb.toString();
	}
	
	
	public static synchronized void registerLogListener(LogListener listener) {
		if (!listeners.contains(listener))
			listeners.add(listener);
	}
	
	public static synchronized void unregisterLogListener(LogListener listener) {
		listeners.remove(listener);
	}
	
	private static void notifyListeners(final String lastAddedMsg){
		if (listeners != null){
			for (LogListener ll : listeners)
				ll.logChanged(lastAddedMsg);
		}
	}
}
