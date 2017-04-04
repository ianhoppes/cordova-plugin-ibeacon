package com.unarin.cordova.beacon;

import android.app.Application;
import android.app.IntentService;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import org.altbeacon.beacon.*;
import org.altbeacon.beacon.powersave.BackgroundPowerSaver;
import org.altbeacon.beacon.service.BeaconService;
import org.altbeacon.beacon.startup.BootstrapNotifier;
import org.altbeacon.beacon.startup.RegionBootstrap;

// source: https://github.com/Crashthatch/cordova-plugin-ibeacon/blob/6ceb5f04b6a4ca8fe31d1c591d75227142cdb3a8/src/android/BackgroundBeaconService.java
public class BackgroundBeaconService extends Service implements BootstrapNotifier {

	public BackgroundBeaconService() {
		super();
	}

	public static final String TAG = "CordovaBeacon";
	private BackgroundPowerSaver backgroundPowerSaver;
	private BeaconManager iBeaconManager;
	private RegionBootstrap regionBootstrap;

	public void onCreate() {
		Log.d(TAG, "BACKGROUND: Creating BackgroundBeaconService.");
		super.onCreate();
		iBeaconManager = BeaconManager.getInstanceForApplication(this);
		iBeaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));
		//iBeaconManager.setBackgroundBetweenScanPeriod(17000l);
		//iBeaconManager.setBackgroundScanPeriod(5000l);
		// Simply constructing this class and holding a reference to it
		// enables auto battery saving of about 60%
		backgroundPowerSaver = new BackgroundPowerSaver(this);
		iBeaconManager.setDebug(true);

		Region region = new Region("backgroundRegion", Identifier.parse("B9407F30-F5F8-466E-AFF9-25556B57FE6D"), null, null);
		regionBootstrap = new RegionBootstrap(this, region);
		Log.d(TAG, "BACKGROUND: Created RegionBootstrap in BackgroundBeaconService.");
	}

	public void onDestroy(){
		Log.d(TAG, "Destroying BackgroundBeaconService");
	}

	@Override
	public void didEnterRegion(Region region) {
		Log.d(TAG, "BackgroundBeaconService.didEnterRegion called!");
	}

	@Override
	public void didExitRegion(Region region) {
		Log.d(TAG, "BackgroundBeaconService.didExitRegion called!");
	}

	@Override
	public void didDetermineStateForRegion(int i, Region region) {
		Log.d(TAG, "BackgroundBeaconService.didDetermineStateForRegion called!");
	}

	@Override
	public Context getApplicationContext() {
		return this.getApplication().getApplicationContext();
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
}