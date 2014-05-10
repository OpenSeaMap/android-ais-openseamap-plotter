package com.klein.service;

public interface NMEATCPVerwaltung {
	//public  void disconnectNetworkService();
	public void stopService();
	public TargetList getAISTargetList();
	public boolean isServiceRunning();
	public void makeConnection();
}
