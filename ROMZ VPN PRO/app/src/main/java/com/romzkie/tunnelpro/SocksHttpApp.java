package com.romzkie.tunnelpro;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.support.v7.app.AppCompatDelegate;
import com.romzkie.ultrasshservice.SocksHttpCore;
import com.romzkie.ultrasshservice.config.Settings;
import com.romzkie.ultrasshservice.util.SkProtect;

/**
* App
*/
public class SocksHttpApp extends Application
{
	private static final String TAG = SocksHttpApp.class.getSimpleName();
	public static final String PREFS_GERAL = "SocksHttpGERAL";
	
	private static SocksHttpApp mApp;
	
	@Override
	public void onCreate()
	{
		super.onCreate();
		
		mApp = this;
		
		// captura dados para análise
		/*new FlurryAgent.Builder()
			.withCaptureUncaughtExceptions(true)
            .withIncludeBackgroundSessionsInMetrics(true)
            .withLogLevel(Log.VERBOSE)
            .withPerformanceMetrics(FlurryPerformance.ALL)
			.build(this, APP_FLURRY_KEY);*/
			
		// inicia
		SocksHttpCore.init(this);
		
		// protege o app
		SkProtect.init(this);
		
		// modo noturno
		setModoNoturno(this);
	}
	
	@Override
	protected void attachBaseContext(Context base) {
		super.attachBaseContext(base);
		//LocaleHelper.setLocale(this);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		//LocaleHelper.setLocale(this);
	}
	
	private void setModoNoturno(Context context) {
		boolean is = new Settings(context)
			.getModoNoturno().equals("on");

		int night_mode = is ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
		AppCompatDelegate.setDefaultNightMode(night_mode);

	}
	
	public static SocksHttpApp getApp() {
		return mApp;
	}
}
