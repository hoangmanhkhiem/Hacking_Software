package com.romzkie.ultrasshservice;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.TrafficStats;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import com.romzkie.ultrasshservice.StatisticGraphData.DataTransferStats;
import java.util.Locale;

public class NetSpeedNotification extends IntentService {

    private Handler mHandler;
    private NotificationManager mNotifyMgr;
    private String mDownloadSpeedOutput;
    private String mUploadSpeedOutput;
    private String mUnits;
    private String mUnits2;
    private boolean mDestroyed = false;

    private Notification.Builder mBuilder;
    
    private DataTransferStats dataTransferStats;

    public NetSpeedNotification() {
        super("NetSpeedNotification");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        initializeNotification();

        while (!mDestroyed) {

            getDownloadSpeed();
            getUploadSpeed();

            Message completeMessage = mHandler.obtainMessage(1);
            completeMessage.sendToTarget();

        }
    }

    private void initializeNotification() {

        mHandler = new Handler(Looper.getMainLooper()) {

            @Override
            public void handleMessage(Message inputMessage) {

                if (mDestroyed) {
                    return;
                }

                dataTransferStats = StatisticGraphData.getStatisticData().getDataTransferStats();
                String bytes_in = (dataTransferStats.byteCountToDisplaySize(dataTransferStats.getTotalBytesReceived(), false));
                String bytes_out = (dataTransferStats.byteCountToDisplaySize(dataTransferStats.getTotalBytesSent(), false));

                String total = "Download: " + bytes_in + "  Upload: " + bytes_out;

                String download = mDownloadSpeedOutput + mUnits;
                String upload = mUploadSpeedOutput + mUnits2;
                String result = download + "↓  " + upload+"↑";

                mBuilder.setSmallIcon(R.drawable.ic_rocket)
                    .setContentTitle(result)
                    .setContentText(total)
                    .setOnlyAlertOnce(true)
                    .setOngoing(true)
                    .setUsesChronometer(true)
                    .setDefaults(Notification.DEFAULT_ALL)
                    .setPriority(Notification.PRIORITY_HIGH);
                mNotifyMgr.notify(26,mBuilder.getNotification());

            }

        };


        mNotifyMgr = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE); 
        mBuilder = new Notification.Builder(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            mBuilder.setChannelId(this.getPackageName() + ".mynotif");
            createNotification(mNotifyMgr, this.getPackageName() + ".mynotif");
        }

        mBuilder.setContentIntent(SocksHttpService.getGraphPendingIntent(this));
        Intent reconnectVPN = new Intent(this, MainReceiver.class);
        reconnectVPN.setAction(MainReceiver.ACTION_SERVICE_RESTART);
        PendingIntent reconnectPendingIntent = PendingIntent.getBroadcast(this, 0, reconnectVPN, PendingIntent.FLAG_CANCEL_CURRENT);

        mBuilder.addAction(R.drawable.ic_autorenew_black_24dp,
                           getString(R.string.reconnect), reconnectPendingIntent);

        Intent disconnectVPN = new Intent(this, MainReceiver.class);
        disconnectVPN.setAction(MainReceiver.ACTION_SERVICE_STOP);
        PendingIntent disconnectPendingIntent = PendingIntent.getBroadcast(this, 0, disconnectVPN, PendingIntent.FLAG_CANCEL_CURRENT);

        mBuilder.addAction(R.drawable.ic_power_settings_new_black_24dp,
						   getString(R.string.stop), disconnectPendingIntent);
        

        startForeground(26, mBuilder.getNotification());
        
    }

    private void createNotification(NotificationManager mNotifyMgr, String id)
    {
        NotificationChannel mNotify = new NotificationChannel(id, "Internet Speed Notification", NotificationManager.IMPORTANCE_HIGH);
        mNotify.setShowBadge(true);
        mNotifyMgr.createNotificationChannel(mNotify);
        // TODO: Implement this method
    }

    private void getDownloadSpeed() {

        long mRxBytesPrevious = TrafficStats.getTotalRxBytes();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {}

        long mRxBytesCurrent = TrafficStats.getTotalRxBytes();

        long mDownloadSpeed = mRxBytesCurrent - mRxBytesPrevious;

        float mDownloadSpeedWithDecimals;

        if (mDownloadSpeed >= 1000000000) {
            mDownloadSpeedWithDecimals = (float) mDownloadSpeed / (float) 1000000000;
            mUnits = " GB/s";
        } else if (mDownloadSpeed >= 1000000) {
            mDownloadSpeedWithDecimals = (float) mDownloadSpeed / (float) 1000000;
            mUnits = " MB/s";

        } else {
            mDownloadSpeedWithDecimals = (float) mDownloadSpeed / (float) 1000;
            mUnits = " kB/s";
        }


        if (!mUnits.equals(" kB/s") && mDownloadSpeedWithDecimals < 100) {
            mDownloadSpeedOutput = String.format(Locale.US, "%.1f", mDownloadSpeedWithDecimals);

        } else {
            mDownloadSpeedOutput = Integer.toString((int) mDownloadSpeedWithDecimals);
        }

    }

    private void getUploadSpeed() {

        long mTxBytesPrevious = TrafficStats.getTotalTxBytes();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {}

        long mTxBytesCurrent = TrafficStats.getTotalTxBytes();

        long mUploadSpeed = mTxBytesCurrent - mTxBytesPrevious;

        float mUploadSpeedWithDecimals;

        if (mUploadSpeed >= 1000000000) {
            mUploadSpeedWithDecimals = (float) mUploadSpeed / (float) 1000000000;
            mUnits2 = " GB/s";
        } else if (mUploadSpeed >= 1000000) {
            mUploadSpeedWithDecimals = (float) mUploadSpeed / (float) 1000000;
            mUnits2 = " MB/s";

        } else {
            mUploadSpeedWithDecimals = (float) mUploadSpeed / (float) 1000;
            mUnits2 = " kB/s";
        }


        if (!mUnits2.equals(" kB/s") && mUploadSpeedWithDecimals < 100) {
            mUploadSpeedOutput = String.format(Locale.US, "%.1f", mUploadSpeedWithDecimals);

        } else {
            mUploadSpeedOutput = Integer.toString((int) mUploadSpeedWithDecimals);
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mDestroyed = true;
        mNotifyMgr.cancelAll();
    }
}
