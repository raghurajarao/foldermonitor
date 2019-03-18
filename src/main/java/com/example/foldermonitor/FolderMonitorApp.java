package com.example.foldermonitor;

import static com.example.foldermonitor.MonitorConfig.MONITOR_FOLDER;
import static com.example.foldermonitor.MonitorConfig.MONITOR_INTERVAL;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class FolderMonitorApp {

	public static void main(String[] args) {
		ScheduledExecutorService scs = Executors.newScheduledThreadPool(1);
		scs.scheduleAtFixedRate(new FolderMonitorTask(MONITOR_FOLDER), 0, MONITOR_INTERVAL, TimeUnit.SECONDS);
	}

}
