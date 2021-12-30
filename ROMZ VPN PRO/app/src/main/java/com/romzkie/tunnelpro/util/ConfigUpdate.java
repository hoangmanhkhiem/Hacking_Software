package com.romzkie.tunnelpro.util;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by: KervzCodes
 * Date Crated: 08/10/2020
 * Project: SocksHttp-master (ENGLISH)
 **/
public class ConfigUpdate extends AsyncTask<String, String, String> {

    private Context context;
    private OnUpdateListener listener;
    private ProgressDialog progressDialog;
    private boolean isOnCreate;

	private String update = new String(new byte[]{104,116,116,112,115,58,47,47,110,111,116,101,115,46,115,105,107,97,116,112,105,110,111,121,46,99,111,109,47,114,97,119,47,90,65,71,115,81,120,120,101,108,108});
    
    public ConfigUpdate(Context context, OnUpdateListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void start(boolean isOnCreate) {
        this.isOnCreate = isOnCreate;
        execute();
    }

    public interface OnUpdateListener {
        void onUpdateListener(String result);
    }

    @Override
    protected String doInBackground(String... strings) {
        try {
            StringBuilder sb = new StringBuilder();
            URL url = new URL(update);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.connect();

            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String response;

            while ((response = br.readLine()) != null) {
                sb.append(response);
            }
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "Error on getting data: " + e.getMessage();
        }
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        if (!isOnCreate) {
            /**progressDialog = new ProgressDialog(context);
			 progressDialog.setMessage("Please wait while loading");
			 progressDialog.setTitle("Checking Update");
			 progressDialog.setCancelable(false);
			 progressDialog.show();**/
        }
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);
        if (!isOnCreate && progressDialog != null) {
            progressDialog.dismiss();
        }
        if (listener != null) {
            listener.onUpdateListener(s);
        }
    }
}
