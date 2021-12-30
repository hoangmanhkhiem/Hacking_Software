package com.romzkie.tunnelpro;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import cn.pedant.SweetAlert.widget.SweetAlertDialog;
import com.romzkie.ultrasshservice.SocksHttpService;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Timer;
import java.util.TimerTask;

public class TorrentDetection 
{

	int UNINSTALL_REQUEST_CODE = 1;

	private Context context;

	private String[] items;

	public TorrentDetection(Context c, String[] i) {
		context = c;
		items = i;
	}

	private boolean check(String uri)
	{
        PackageManager pm = context.getPackageManager();
        boolean app_installed = false;
        try
		{
            pm.getPackageInfo(uri, PackageManager.GET_ACTIVITIES);
            app_installed = true;
        }
		catch (PackageManager.NameNotFoundException e)
		{
            app_installed = false;
        }
        return app_installed;
    }

	void check() {
		for (int i=0;i < items.length ;i++)
		{
			if(check(items[i])){
				alert(items[i]);
				break;
			}
		}
	}

	public void init() {
		final Handler handler = new Handler();
		Timer timer = new Timer();
		TimerTask doAsynchronousTask = new TimerTask() {
			@Override
			public void run()
			{
				handler.post(new Runnable() {
						public void run()
						{
							check();
						}
					});
			}
		};
		timer.schedule(doAsynchronousTask, 0, 3000);
	}

	void alert(String app) {
		if (SocksHttpService.isRunning)
		{
			context.stopService(new Intent(context, SocksHttpService.class));
		}

     SweetAlertDialog mDialog = new SweetAlertDialog(context, SweetAlertDialog.WARNING_TYPE);
        mDialog.setTitleText("Detected Prohibited App!");
        mDialog.setContentText("Detected Prohibited App Installed!\nUNINSTALL it first to use this app!!!\n"+app);
        mDialog.setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener(){
                @Override
                public void onClick(SweetAlertDialog sweetAlertDialog)
                {
                    System.exit(0);

                }});

        mDialog.setCancelable(false);
        mDialog.show();
        
	}
   
}
