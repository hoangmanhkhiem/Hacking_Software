package com.romzkie.tunnelpro;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ScrollView;
import android.widget.TextView;
import android.support.v4.graphics.ColorUtils;
import android.graphics.Color;

public class ExceptionActivity extends AppCompatActivity {
    TextView error;
    private Toolbar toolbar;
    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        LayoutParams layoutParams = new LinearLayout.LayoutParams(-2, -2);
        layoutParams.gravity = 17;
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setLayoutParams(layoutParams);
        setContentView(linearLayout);
        toolbar = new Toolbar(this);
        setSupportActionBar(this.toolbar);
        toolbar.setTitle(getString(R.string.app_name));
		toolbar.setTitleTextColor(Color.parseColor("#FFFFFF"));
		toolbar.setBackgroundColor(this.getResources().getColor(R.color.colorPrimary));
        ScrollView sv = new ScrollView(this);
        TextView error = new TextView(this);
        sv.addView(error);
        linearLayout.addView(toolbar);
        linearLayout.addView(sv);
        error.setText(getIntent().getStringExtra("error"));
    }
	
}
