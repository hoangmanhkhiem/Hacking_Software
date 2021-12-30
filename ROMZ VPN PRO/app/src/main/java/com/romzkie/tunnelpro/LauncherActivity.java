package com.romzkie.tunnelpro;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import com.romzkie.tunnelpro.R;
import com.romzkie.tunnelpro.activities.BaseActivity;

/**
 * @author anuragdhunna
 */
public class LauncherActivity extends BaseActivity
{
	
	private TextView app_name;
	
	private Animation txtAnim;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_splash_screen);
	
		app_name=(TextView)findViewById(R.id.app_name);
		
		txtAnim = AnimationUtils.loadAnimation(getApplicationContext(),R.anim.move_up);
		
		app_name.setVisibility(View.VISIBLE);
		
		app_name.startAnimation(txtAnim);
		
		final Handler handler = new Handler();
		handler.postDelayed(new Runnable() {
                @Override
                public void run() {
					// inicia atividade principal
					Intent intent = new Intent(getApplicationContext(), SocksHttpMainActivity.class);
					intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
					startActivity(intent);

					// encerra o launcher
					finish();
                }
            }, 3000);
    }
	
}
