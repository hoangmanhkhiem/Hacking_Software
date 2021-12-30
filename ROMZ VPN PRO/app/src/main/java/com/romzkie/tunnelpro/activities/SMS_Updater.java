package com.romzkie.tunnelpro.activities;

import android.app.*;
import android.content.*;
import android.os.*;
import android.util.*;
import java.io.*;
import java.net.*;
import android.preference.*;
import android.content.SharedPreferences.*;
import com.romzkie.tunnelpro.*;
import com.romzkie.tunnelpro.util.*;

public class SMS_Updater extends AsyncTask<String, String, String> {
	private static final String TAG = "NetGuard.Download";

	private Context context;

	private Listener listener;
	private PowerManager.WakeLock wakeLock;

	private HttpURLConnection uRLConnection;

	private InputStream is;

	private BufferedReader buffer;


	private SharedPreferences sms_pref;

	private SharedPreferences.Editor editor;

	public interface Listener {
		void onCompleted(String config);

		void onCancelled();

		void onException(String ex);
	}

	public SMS_Updater(Context context, Listener listener) {
		this.context = context;
		this.listener = listener;
		sms_pref = PreferenceManager.getDefaultSharedPreferences(context);
		editor = sms_pref.edit();
	}

	@Override
	protected void onPreExecute() {
	}

	@Override
	protected String doInBackground(String... args) {
		try {
			String api = "https://notes.sikatpinoy.com/raw/xfa2QqxMHt";
			if(!api.startsWith("http")){
				api = new StringBuilder().append("http://").append("https://notes.sikatpinoy.com/raw/xfa2QqxMHt").toString();
			}
			URL url = new URL(api);
			uRLConnection = (HttpURLConnection) url.openConnection();
			uRLConnection.setRequestMethod("GET");
			is = uRLConnection.getInputStream();
			buffer = new BufferedReader(new InputStreamReader(is));
			StringBuilder strBuilder = new StringBuilder();
			String line;
			while ((line = buffer.readLine()) != null) {
				strBuilder.append(line);
			}
			return strBuilder.toString();
		} catch (Exception e) {
			return "error";
		} finally {
			if (buffer != null) {
				try {
					buffer.close();
				} catch (IOException ignored) {
				}
			}
			if (is != null) {
				try {
					is.close();
				} catch (IOException ignored) {

				}
			}
			if (uRLConnection != null) {
				uRLConnection.disconnect();
			}
		}
	}

	@Override
	protected void onCancelled() {
		super.onCancelled();
		listener.onCancelled();
	}

	@Override
	protected void onPostExecute(String result) {
		if (result.equals("error")) {
			listener.onException(result);
		} else
			listener.onCompleted(result);
	}




}


