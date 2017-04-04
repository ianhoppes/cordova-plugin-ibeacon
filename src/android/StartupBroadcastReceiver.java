package com.unarin.cordova.beacon;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

// source: https://github.com/Crashthatch/cordova-plugin-ibeacon/blob/6ceb5f04b6a4ca8fe31d1c591d75227142cdb3a8/src/android/StartupBroadcastReceiver.java
public class StartupBroadcastReceiver extends BroadcastReceiver {

	public static final String TAG = "CordovaBeacon";

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d(TAG, "StartupBroadcastReceiver starting BackgroundBeaconService...");
		//Start BackgroundBeaconService on boot.
		Intent startServiceIntent = new Intent(context.getApplicationContext(), BackgroundBeaconService.class);
		context.getApplicationContext().startService(startServiceIntent);
	}

}