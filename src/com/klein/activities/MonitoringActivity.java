package com.klein.activities;



import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;


import com.klein.aistcpopenmapplotter051.R;

import com.klein.commons.AISPlotterGlobals;
import com.klein.logutils.LogListener;
import com.klein.logutils.Logger;




public class MonitoringActivity extends Activity implements LogListener, View.OnClickListener {
	
	private static final int DIALOG_FLAGS = 1;
	//private static final int DIALOG_DEVICES = 2;
	
	private static final String KEY_FLAG_PREF = "flag_pref";
	private static final int MAX_ENTRIES = 400;
	
	private Button monitoringBtn;
	private Button showDecodedAISMSGBtn;
	
	private ScrollView logScrollView;
	private TextView logTV;
	private Handler handler;
	
	private int logEntries = 0;
	private boolean monitoring;
	private boolean showDecodedAISMessages;
	private boolean userTouch = false;
	String[] addresses; // connected devices
	String[] flags;
	char selectedFlag;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setTitle(R.string.monitoring_title);
		setContentView(R.layout.monitoring);

		handler = new Handler();

		monitoringBtn = (Button)findViewById(R.id.monitoring_btn);
		showDecodedAISMSGBtn= (Button) findViewById(R.id.showdecodedaismsg_btn);
		//flagBtn = (Button)findViewById(R.id.flag_btn);
		//dataToSendET = (EditText)findViewById(R.id.data_to_send);
		logScrollView = (ScrollView)findViewById(R.id.log_scroll);
		logTV = (TextView)findViewById(R.id.log);
		logTV.setText("========== Logging Window ==========\n");
		
		monitoring = PreferenceManager.getDefaultSharedPreferences(MonitoringActivity.this)
			.getBoolean(Logger.KEY_IS_LOG_ENABLED, true);
		showDecodedAISMessages = PreferenceManager.getDefaultSharedPreferences(MonitoringActivity.this)
		    .getBoolean(Logger.KEY_IS_AISMSG_LOG_ENABLED, true);
		updateMonitoringState();
		
		findViewById(R.id.clear_btn).setOnClickListener(this);
		monitoringBtn.setOnClickListener(this);
		
		showDecodedAISMSGBtn.setOnClickListener(this);
		logScrollView.setOnTouchListener(new View.OnTouchListener() {
			
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (!userTouch && event.getAction() == MotionEvent.ACTION_DOWN){
					userTouch = true;
				}
				else if (userTouch && event.getAction() == MotionEvent.ACTION_UP){
					userTouch = false;
				}
				return false;
			}
		});
	}

	
	@Override
	protected void onStart() {
		super.onStart();
		logScrollView.fullScroll(View.FOCUS_DOWN);
	}

	@Override
	protected void onStop() {
		super.onStop();
		Logger.unregisterLogListener(this);
	}
	
	
	
	private void clearLogClickHandler(View target){
		logTV.setText("========== Logging Window ==========\n");
		Logger.clear();
		updateMonitoringState();
	}
	
	
	private void updateMonitoringState(){
		if (showDecodedAISMessages) {
			showDecodedAISMSGBtn.setText(R.string.disable_showdecodedaismsg);
			Intent aIntent = new Intent(AISPlotterGlobals.ACTION_SHOW_AISMSG_INLOG);
			aIntent.putExtra(AISPlotterGlobals.SHOW_AISMSG_INLOG_KEY,showDecodedAISMessages);
			sendBroadcast(aIntent); // tell the service to log
		}else {
			showDecodedAISMSGBtn.setText(R.string.enable_showdecodedaismsg);
			Intent aIntent = new Intent(AISPlotterGlobals.ACTION_SHOW_AISMSG_INLOG);
			aIntent.putExtra(AISPlotterGlobals.SHOW_AISMSG_INLOG_KEY,showDecodedAISMessages);
			sendBroadcast(aIntent);
		}
		if (monitoring) {
			
            monitoringBtn.setText(R.string.disable_monitoring);
			logTV.append("Monitoring enabled!\n");
			logTV.append(Logger.getLog());
			logScrollView.smoothScrollBy(0, logTV.getHeight());
			Logger.registerLogListener(this);
			Logger.enabled = true;
		}
		else {
			
			monitoringBtn.setText(R.string.enable_monitoring);
			Logger.enabled = false;
			Logger.unregisterLogListener(this);
			logTV.append("Monitoring disabled!\n");
		}
	}


	@Override
	public void logChanged(final String lastAddedMsg) {
		logEntries++;

		handler.post(new Runnable() {
			
			@Override
			public void run() {
				if (logEntries > MAX_ENTRIES){
					int size = logTV.getText().length();
					logTV.setText(logTV.getText().subSequence(size/2, size));
					logEntries /= 2;
				}
				logTV.append(lastAddedMsg + "\n");
				if (!userTouch){
					logScrollView.post(new Runnable() {
						
						@Override
						public void run() {
							logScrollView.smoothScrollBy(0, 60);
						}
					});
				}
			}
		});
	}
	
	@Override
    protected Dialog onCreateDialog(int id) {
		 switch (id) {
	        
		 }
		 return null;
	}
	
	
	@Override
	public void onClick(View v) {
		switch (v.getId()){
		case R.id.monitoring_btn:
			monitoring = !monitoring;
			PreferenceManager.getDefaultSharedPreferences(MonitoringActivity.this)
				.edit()
				.putBoolean(Logger.KEY_IS_LOG_ENABLED, monitoring)
				.commit();
			updateMonitoringState();
			break;
		case R.id.showdecodedaismsg_btn:
			showDecodedAISMessages = ! showDecodedAISMessages;
			PreferenceManager.getDefaultSharedPreferences(MonitoringActivity.this)
			.edit()
			.putBoolean(Logger.KEY_IS_AISMSG_LOG_ENABLED, showDecodedAISMessages)
			.commit();
		     updateMonitoringState();
		break;
			
		case R.id.clear_btn:
			clearLogClickHandler(v);
			break;
		}
	}

}
