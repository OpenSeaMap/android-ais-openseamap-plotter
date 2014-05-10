package com.klein.service;

public interface NMEABTVerwaltung {
	public void disconnectNetworkService();
	public TargetList getAISTargetList();
	public boolean isServiceRunning();
	
}
