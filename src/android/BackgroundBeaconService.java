package com.unarin.cordova.beacon;

import android.app.*;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import org.altbeacon.beacon.*;
import org.altbeacon.beacon.powersave.BackgroundPowerSaver;
import org.altbeacon.beacon.startup.BootstrapNotifier;
import org.altbeacon.beacon.startup.RegionBootstrap;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.InvalidKeyException;
import java.util.UUID;

// source: https://github.com/Crashthatch/cordova-plugin-ibeacon/blob/6ceb5f04b6a4ca8fe31d1c591d75227142cdb3a8/src/android/BackgroundBeaconService.java
public class BackgroundBeaconService extends Service implements BootstrapNotifier, BeaconConsumer {

	class BackgroundBinder extends Binder {
		BackgroundBeaconService getService() {
			return BackgroundBeaconService.this;
		}
	}

	public BackgroundBeaconService() {
		super();
	}

	private static final String TAG = "CordovaBeacon";
	private BackgroundPowerSaver backgroundPowerSaver;
	private BeaconManager beaconManager;
	private RegionBootstrap regionBootstrap;

	private Region region = null;

	private final BackgroundBinder backgroundBinder = new BackgroundBinder();

	private boolean errored = false;
	private boolean started = false;
	private boolean startedWithIntent = false;
	private boolean beaconServiceConnected = false;

	private String icon = "res://cordova";
	private String title = "CordovaBeacon";
	private String body = "Found a beacon!";

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(TAG, "[BACKGROUND] Started!");
		startedWithIntent = intent != null;
		started = true;
		if(errored) {
			this.stopSelf();
			return START_NOT_STICKY;
		}
		this.initFromStorage();
		return START_STICKY;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(TAG, "[BACKGROUND] Created BackgroundBeaconService");

		beaconManager = BeaconManager.getInstanceForApplication(this.getApplicationContext());
		// beaconManager.setBackgroundBetweenScanPeriod(15000L);
		// beaconManager.setBackgroundScanPeriod(5000L);
		beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));
		beaconManager.bind(this);
		// Simply constructing this class and holding a reference to it
		// enables auto battery saving of about 60%
		backgroundPowerSaver = new BackgroundPowerSaver(this.getApplicationContext());
	}

	private int getResIdForDrawable(String resPath) {
		return this.getApplicationContext().getResources().getIdentifier(resPath, "drawable", getApplicationContext().getPackageName());
	}

	private void createNotification() {

		Context context = this.getApplicationContext();
		String pkgName = context.getPackageName();
		Intent intent = context.getPackageManager().getLaunchIntentForPackage(pkgName);
		intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);

		PendingIntent contentIntent = PendingIntent.getActivity(this.getApplicationContext(), 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

		Notification.Builder b = new Notification.Builder(this.getApplicationContext());

		int iconResId = this.getResIdForDrawable(icon);

		b.setAutoCancel(true)
				.setDefaults(Notification.DEFAULT_ALL)
				.setWhen(System.currentTimeMillis())
				.setTicker("Hearty365")
				.setSmallIcon(iconResId > 0 ? iconResId : android.R.drawable.ic_dialog_info)
				.setContentTitle(title)
				.setContentText(body)
				.setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_SOUND)
				.setContentIntent(contentIntent)
				.setContentInfo("Info");

		NotificationManager notificationManager = (NotificationManager) this.getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.notify(0, b.build());
	}

	public void onDestroy(){
		Log.d(TAG, "[BACKGROUND] Destroyed!");
		this.beaconManager.unbind(this);
	}

	private void initFromStorage() {
		if(!started || !beaconServiceConnected || startedWithIntent) return;
		SharedPreferences preferences = this.getApplicationContext().getSharedPreferences(TAG, 0);
		String bgRegion = preferences.getString("bgregion", "");
		String notificationInfo = preferences.getString("notification", "");
		if(!bgRegion.equals("")) {
			try {
				this.setBackgroundMonitoringRegion(new JSONObject(bgRegion));
				Log.i(TAG, "[BACKGROUND] Loaded background monitoring region from storage!");
			} catch (Exception e) {
				Log.e(TAG, "[BACKGROUND] Couldn't get/parse/apply stored background monitoring region!", e);
				errored = true;
				if(started && startedWithIntent)
					this.stopSelf();
			}
		} else Log.i(TAG, "[BACKGROUND] No bgregion info stored =(");

		if(!notificationInfo.equals("")) {
			try {
				JSONObject json = new JSONObject(notificationInfo);
				icon  = json.has("icon" ) && !json.isNull("icon" ) ? json.optString("icon" , icon ) : icon;
				title = json.has("title") && !json.isNull("title") ? json.optString("title", title) : title;
				body  = json.has("body" ) && !json.isNull("body" ) ? json.optString("body" , body ) : body;
				Log.i(TAG, "[BACKGROUND] Loaded notification settings from storage!");
			} catch (Exception e) {
				Log.e(TAG, "[BACKGROUND] Couldn't get/parse/apply stored notification settings!", e);
			}

		} else Log.i(TAG, "[BACKGROUND] No notification info stored =(");
	}

	@Override
	public void onBeaconServiceConnect() {
		beaconServiceConnected = true;
		this.initFromStorage();
	}

	@Override
	public void didEnterRegion(Region region) {
		Log.d(TAG, "[BACKGROUND] Found a beacon!");
		this.createNotification();
	}

	@Override
	public void didExitRegion(Region region) {
		Log.d(TAG, "[BACKGROUND] Lost a beacon!");
		NotificationManager notificationManager = (NotificationManager) this.getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.cancelAll();
	}

	@Override
	public void didDetermineStateForRegion(int state, Region region) {
		Log.d(TAG, "[BACKGROUND] Determined state for region!");
		/*if(state == INSIDE) {
			this.didEnterRegion(region);
		} else { // state == OUTSIDE
			this.didExitRegion(region);
		}*/
	}

	@Override
	public Context getApplicationContext() {
		return this.getApplication().getApplicationContext();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return this.backgroundBinder;
	}

	private Region parseRegion(JSONObject json) throws JSONException, InvalidKeyException, UnsupportedOperationException {

		if (!json.has("typeName"))
			throw new InvalidKeyException("'typeName' is missing, cannot parse Region.");

		if (!json.has("identifier"))
			throw new InvalidKeyException("'identifier' is missing, cannot parse Region.");

		String typeName = json.getString("typeName");
		if (typeName.equals("BeaconRegion")) {

			String identifier = json.getString("identifier");

			//For Android, uuid can be null when scanning for all beacons (I think)
			String uuid = json.has("uuid") && !json.isNull("uuid") ? json.getString("uuid") : null;
			String major = json.has("major") && !json.isNull("major") ? json.getString("major") : null;
			String minor = json.has("minor") && !json.isNull("minor") ? json.getString("minor") : null;

			if (major == null && minor != null)
				throw new UnsupportedOperationException("Unsupported combination of 'major' and 'minor' parameters.");

			Identifier id1 = uuid != null ? Identifier.parse(uuid) : null;
			Identifier id2 = major != null ? Identifier.parse(major) : null;
			Identifier id3 = minor != null ? Identifier.parse(minor) : null;
			return new Region(identifier, id1, id2, id3);

		} else {
			throw new UnsupportedOperationException("Unsupported region type");
		}

	}

	void setBackgroundMonitoringRegion(JSONObject rawRegion) throws RemoteException, InvalidKeyException, JSONException {
		this.getApplicationContext().getSharedPreferences(TAG, 0).edit().putString("bgregion", rawRegion.toString()).commit();
		Log.d(TAG, "[BACKGROUND] Set background region...");
		if(this.region != null) {
			if(regionBootstrap != null) {
				regionBootstrap.disable();
				regionBootstrap = null;
			}
			beaconManager.stopMonitoringBeaconsInRegion(this.region);
			this.region = null;
		}
		if(!this.startedWithIntent) {
			this.region = parseRegion(rawRegion);
			beaconManager.startMonitoringBeaconsInRegion(this.region);
			regionBootstrap = new RegionBootstrap(this, this.region);
		}
	}

	void clearBackgroundMonitoringRegion() throws RemoteException {
		this.getApplicationContext().getSharedPreferences(TAG, 0).edit().remove("bgregion");
		Log.d(TAG, "[BACKGROUND] Cleared background region...");
		if(this.region == null) return;
		if(regionBootstrap != null) {
			regionBootstrap.disable();
			regionBootstrap = null;
		}
		beaconManager.stopMonitoringBeaconsInRegion(this.region);
		this.region = null;
	}

	void setBackgroundMonitorNotification(final String icon, final String title, final String body) {
		this.icon = icon;
		this.title = title;
		this.body = body;
		try {
			this.getApplicationContext().getSharedPreferences(TAG, 0).edit()
					.putString("notification", new JSONObject()
							.put("icon", icon)
							.put("title", title)
							.put("body", body).toString())
					.commit();
			Log.d(TAG, "[BACKGROUND] Set new notification settings!");
		} catch (Exception e) {
			Log.e(TAG, "[BACKGROUND] Error setting new notification settings!", e);
		}
	}
}