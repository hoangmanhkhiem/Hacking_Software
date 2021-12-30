package com.romzkie.tunnelpro;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Toast;
import com.romzkie.tunnelpro.R;
import java.net.URL;

public class PayloadGenerator extends RelativeLayout implements OnClickListener, OnItemSelectedListener, RadioGroup.OnCheckedChangeListener, CompoundButton.OnCheckedChangeListener 
{


	public interface OnDismissListener
	{
		public void onDismiss(String payload);
	}
	public static final String URL_HOST = "URL_HOST";
	public static final String INJECT_METHOD = "SELECTED_INJECT_METHOD";
	public static final String REQUEST_METHOD = "SELECTED_REQUEST_METHOD";
	public static final String METHOD = "METHOD";
	public static final String QUERY_MODE = "QUERY_MODE";
	public static final String ONLINE_HOST = "ONLINE_HOST";
	public static final String FORWARD_HOST = "FORWARD_HOST";
	public static final String REVERSE_PROXY = "REVERSE_PROXY";
	public static final String KEEP_ALIVE = "KEEP_ALIVE";
	public static final String DUAL_CONNECT = "DUAL_CONNECT";
	public static final String FULL_HOST = "FULL_HOST";
	public static final String DEFAULT_PROXY = "DEFAULT_PROXY";
	public static final String PROXY = "PROXY";
	public static final String PORT = "PORT";
	public static final String SPLIT_MODE = "SPLIT_MODE";

	private Context context;

	private OnDismissListener listener;
	private SharedPreferences prefs;
	private SharedPreferences.Editor editor;
	private EditText url_host;
	private Spinner request_method, inject_method;
	private RadioGroup query_mode, split_mode;
	private CheckBox online_host,forward_host,reverse_proxy,keep_alive,full_host,dual_connect;
	//private EditText proxy_edit,port_edit;
	private Button generate_btn;
	public PayloadGenerator(Context context)
	{
		super(context);
		this.context = context;
		init();
	}
	public void setDismissListener(OnDismissListener OnDismissListener)
	{
		listener = OnDismissListener;
	}

	private void init()
	{
		prefs = PreferenceManager.getDefaultSharedPreferences(context);
		editor = prefs.edit();
		View v = LayoutInflater.from(context).inflate(R.layout.payload_generator, null);
		url_host = (EditText)findId(v,R.id.url_host);
		split_mode = (RadioGroup)findId(v, R.id.split_group);
		request_method = (Spinner)findId(v, R.id.request_method);
		inject_method = (Spinner)findId(v,R.id.inject_method);
		query_mode = (RadioGroup)findId(v, R.id.query_mode);
		online_host = (CheckBox)findId(v, R.id.online_host);
		forward_host = (CheckBox)findId(v, R.id.forward_host);
		reverse_proxy = (CheckBox)findId(v, R.id.reverse_proxy);
		keep_alive = (CheckBox)findId(v, R.id.keep_alive);
		full_host = (CheckBox)findId(v, R.id.full_host);
		dual_connect = (CheckBox)findId(v, R.id.dual_connect);
		/*default_proxy = (CheckBox)findId(v, R.id.default_proxy);
		 proxy_edit = (EditText)findId(v, R.id.remote_proxy);
		 port_edit = (EditText)findId(v, R.id.remote_proxy_port);*/
		generate_btn = (Button)findId(v, R.id.generate_payload);

		request_method.setAdapter(getRequestAdapter());
		inject_method.setAdapter(getInjectAdapter());
		((RadioButton)query_mode.getChildAt(prefs.getInt(QUERY_MODE, 0))).setChecked(true);
		((RadioButton)split_mode.getChildAt(prefs.getInt(SPLIT_MODE, 0))).setChecked(true);
		url_host.setText(prefs.getString(URL_HOST, ""));
		request_method.setSelection(prefs.getInt(REQUEST_METHOD, 0));
		inject_method.setSelection(prefs.getInt(INJECT_METHOD, 0));
		online_host.setChecked(prefs.getBoolean(ONLINE_HOST, false));
		forward_host.setChecked(prefs.getBoolean(FORWARD_HOST, false));
		reverse_proxy.setChecked(prefs.getBoolean(REVERSE_PROXY, false));
		keep_alive.setChecked(prefs.getBoolean(KEEP_ALIVE, false));
		full_host.setChecked(prefs.getBoolean(FULL_HOST, false));
		dual_connect.setChecked(prefs.getBoolean(DUAL_CONNECT, false));
		/*default_proxy.setChecked(prefs.getBoolean(DEFAULT_PROXY, false));
		 proxy_edit.setText(prefs.getString(PROXY, ""));
		 port_edit.setText(prefs.getString(PORT, ""));*/

		split_mode.setOnCheckedChangeListener(this);
		request_method.setOnItemSelectedListener(this);
		inject_method.setOnItemSelectedListener(this);
		query_mode.setOnCheckedChangeListener(this);
		//default_proxy.setOnCheckedChangeListener(this);
		generate_btn.setOnClickListener(this);

		addView(v);
	}
	private View findId(View v, int id)
	{
		return v.findViewById(id);
	}
	@Override
	public void onClick(View p1)
	{
		String url = url_host.getText().toString();

		if (url.isEmpty()) {
			showToast("Field cannot be empty");
		} else if (url.isEmpty()) {
			showToast("URL/Host is empty");
		} else {
			editor.putString(URL_HOST, url);
			editor.putBoolean(ONLINE_HOST, online_host.isChecked());
			editor.putBoolean(FORWARD_HOST, forward_host.isChecked());
			editor.putBoolean(REVERSE_PROXY, reverse_proxy.isChecked());
			editor.putBoolean(KEEP_ALIVE, keep_alive.isChecked());
			editor.putBoolean(FULL_HOST, full_host.isChecked());
			editor.putBoolean(DUAL_CONNECT, dual_connect.isChecked());
			//editor.putString(PROXY, proxy);
			//editor.putString(PORT, port);
			editor.putInt(INJECT_METHOD, inject_method.getSelectedItemPosition());
			editor.apply();
			listener.onDismiss(getPayload());
		}
		// TODO: Implement this method
	}
	private String getHost()
	{
		String host = String.format("http://%s/", url_host.getText().toString());
		return host;
	}
	private String getHostHeader()
	{
		String host = String.format("http://%s/", url_host.getText().toString());
		try {
			URL url = new URL(host);
			if (full_host.isChecked()) {
				host = "http://" + url.getHost() + url.getPath();
			} else {
				host = url.getHost();
			}
		} catch (Exception e) {

		}
		return host;
	}
	private String getPayload()
	{
		String str = "";
		String url = getHostHeader();
		String server = "[host_port]";
		String crlf = "[crlf]";
		String proto = "[protocol]";
		String split = null;
		int splitid = split_mode.getCheckedRadioButtonId();
		if (splitid == R.id.split_normal) {
			split = "[split]";
		} else if (splitid == R.id.split_delay) {
			split = "[delay_split]";
		} else if (splitid == R.id.split_none) {
			split = "";
		}
		if (server != null) {
			String host = "Host: " + url + crlf;
			String monline_host = online_host.isChecked() == true ? "X-Online-Host: " + url + crlf : "";
			String mforward_host = forward_host.isChecked() == true ? "X-Forward-Host: " + url + crlf : "";
			String mreverse_proxy = reverse_proxy.isChecked() == true ? "X-Forwarded-For: " + url + crlf : "";
			String mkeep_alive = "Connection: Keep-Alive" + crlf;
			String method = (String) request_method.getSelectedItem() + " " + getHost() + " " + "HTTP/1.1" + crlf;
			String mdual_connect = dual_connect.isChecked() == true ? "CONNECT " + server + " " + proto + crlf + crlf : crlf;
			int mQuery = query_mode.indexOfChild(findViewById(query_mode.getCheckedRadioButtonId()));
			switch (inject_method.getSelectedItemPosition()){
				case 0:
					str = "CONNECT " +  setup(server, mQuery, url, crlf) + host + monline_host + mforward_host + mreverse_proxy + mkeep_alive + mdual_connect;
					break;
				case 1:
					str = method + host + monline_host + mforward_host + mreverse_proxy + mkeep_alive + crlf + split +  "CONNECT " + setup(server, mQuery, url, crlf) + mdual_connect;
					break;
				case 2:
					str = "CONNECT " + setup(server, mQuery, url, crlf) + split + method + host + monline_host + mforward_host + mreverse_proxy + mkeep_alive + mdual_connect;
					break;
			}
			return str;
		}
		return null;
	}
	private String setup(String server, int mQuery, String url, String crlf)
	{
		String proto = "[protocol]";
		String str = null;
		switch (mQuery) {
			case 0:
				str = server + " " + proto + crlf;
				break;
			case 1:
				str = url + "@" + server + " " + proto + crlf;
				break;
			case 2:
				str = server + "@" + url + " " + proto + crlf;
				break;
		}
		// TODO: Implement this method
		return str;
	}
	@Override
	public void onItemSelected(AdapterView<?> p1, View p2, int p3, long p4)
	{
		switch (p1.getId()) {
			case R.id.request_method:
				editor.putInt(REQUEST_METHOD, p3).apply();
				editor.putString(METHOD, (String)p1.getSelectedItem()).apply();
				break;
		    case R.id.inject_method:
				break;
		}
		// TODO: Implement this method
	}

	@Override
	public void onNothingSelected(AdapterView<?> p1)
	{
		// TODO: Implement this method
	}

	@Override
	public void onCheckedChanged(RadioGroup p1, int p2)
	{
		switch (p1.getId()) {
			case R.id.query_mode:
				editor.putInt(QUERY_MODE, p1.indexOfChild((RadioButton)p1.findViewById(p2))).apply();
				break;
			case R.id.split_group:
				editor.putInt(SPLIT_MODE, p1.indexOfChild((RadioButton)p1.findViewById(p2))).apply();
				break;
		}
		// TODO: Implement this method
	}

	@Override
	public void onCheckedChanged(CompoundButton p1, boolean p2)
	{
		/*switch (p1.getId()) {
		 case R.id.default_proxy:
		 proxy_edit.setText( p2 ? profile.mConnections[0].mServerName : "");
		 port_edit.setText(p2 ? "8080" : "");
		 editor.putBoolean(DEFAULT_PROXY, p2).apply();
		 break;
		 }*/
		// TODO: Implement this method
	}
	private ArrayAdapter<String> getInjectAdapter()
	{
		String[] injects = {"Normal","Front","Back"};
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(context, R.layout.item, injects);
		return adapter;
	}
	private ArrayAdapter<String> getRequestAdapter()
	{
		String[] requests = {"CONNECT","GET","POST","HEAD","PUT","PATCH","PROPATCH","DELETE"};
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(context, R.layout.item, requests);
		return adapter;
	}
	private void showToast(String message)
	{
		Toast.makeText(context, message, Toast.LENGTH_LONG).show();
	}
}
