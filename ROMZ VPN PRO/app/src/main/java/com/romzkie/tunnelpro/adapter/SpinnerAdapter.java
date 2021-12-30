package com.romzkie.tunnelpro.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.romzkie.tunnelpro.R;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.json.JSONObject;
import android.widget.*;

public class SpinnerAdapter extends ArrayAdapter<JSONObject> {

	private int spinner_id;

	public SpinnerAdapter(Context context, int spinner_id, ArrayList<JSONObject> list) {
		super(context, R.layout.spinner_item, list);
		this.spinner_id = spinner_id;
	}

	@Override
	public JSONObject getItem(int position) {
		return super.getItem(position);
	}

	@Override
	public View getDropDownView(int position, View convertView, ViewGroup parent) {
		return view(position, convertView, parent);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		return view(position, convertView, parent);
	}

	private View view(int position, View convertView, ViewGroup parent) {
		View v = LayoutInflater.from(getContext()).inflate(R.layout.spinner_item, parent, false);
		TextView tv = v.findViewById(R.id.itemName);
        TextView extra = v.findViewById(R.id.textExtra);
        TextView info = v.findViewById(R.id.info);
		ImageView im = v.findViewById(R.id.itemImage);
		LinearLayout ok = v.findViewById(R.id.ok12);
		try {
			tv.setText(getItem(position).getString("Name"));
          //  info.setText(getItem(position).getString("Info"));
			if (spinner_id == R.id.serverSpinner) {
				getServerIcon(position, im, info);
				ok.setVisibility(View.GONE);
			} else if (spinner_id == R.id.payloadSpinner) {
				getPayloadIcon(position, im, extra, info);
				ok.setVisibility(View.VISIBLE);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return v;
	}

	private void getServerIcon(int position, ImageView im, TextView info) throws Exception {
		
        String name = getItem(position).getString("FLAG").toUpperCase();
        
        if (name.contains("NX")){
            im.setImageResource(R.drawable.netflix);
        }else if(name.contains("TT")){
            im.setImageResource(R.drawable.torrent);
        }else if(name.contains("GG")){
            im.setImageResource(R.drawable.xgame);
        }else if(name.contains("VP")){
            im.setImageResource(R.drawable.voip);
        }else{
            InputStream inputStream = getContext().getAssets().open("flags/" + getItem(position).getString("FLAG"));
		    im.setImageDrawable(Drawable.createFromStream(inputStream, getItem(position).getString("FLAG"))); 
            if (inputStream != null) {
                inputStream.close();
            }
        }
        
        String sInfo = getItem(position).getString("sInfo");
        
		info.setText(sInfo);
	}

	private void getPayloadIcon(int position, ImageView im, TextView extra, TextView info) throws Exception {
		String name = getItem(position).getString("Name").toLowerCase();
        info.setText("PROMO NEEDED");
        boolean sslType = getItem(position).getBoolean("isSSL");
        if (sslType) {
            extra.setText("SSH/SSL");
        } else{
            extra.setText("SSH/Proxy");
        }
        
		if (name.contains("globe")) {
			im.setImageResource(R.drawable.ic_globe);
		} else if (name.contains("smart")) {
			im.setImageResource(R.drawable.ic_smart);
		} else if (name.contains("gtm")) {
			im.setImageResource(R.drawable.ic_gtm);
		} else if (name.contains("tm")) {
			im.setImageResource(R.drawable.ic_tm);
		} else if (name.contains("tnt")) {
			im.setImageResource(R.drawable.ic_tnt);
		}else if(name.contains("sun")) {
			im.setImageResource(R.drawable.ic_sun);
		}else if(name.contains("dito")) {
			im.setImageResource(R.drawable.ic_dito);
		}
	}

}
