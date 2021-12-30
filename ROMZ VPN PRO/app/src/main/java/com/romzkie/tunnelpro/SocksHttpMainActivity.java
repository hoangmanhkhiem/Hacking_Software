package com.romzkie.tunnelpro;

import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.content.res.*;
import android.graphics.*;
import android.net.*;
import android.os.*;
import android.support.annotation.*;
import android.support.design.widget.*;
import android.support.v4.app.*;
import android.support.v4.content.*;
import android.support.v4.view.*;
import android.support.v4.widget.*;
import android.support.v7.app.*;
import android.support.v7.widget.*;
import android.text.*;
import android.view.*;
import android.widget.*;
import cn.pedant.SweetAlert.widget.*;
import com.google.android.gms.ads.*;
import com.google.android.gms.ads.reward.*;
import com.romzkie.tunnelpro.*;
import com.romzkie.tunnelpro.activities.*;
import com.romzkie.tunnelpro.adapter.*;
import com.romzkie.tunnelpro.fragments.*;
import com.romzkie.tunnelpro.model.*;
import com.romzkie.tunnelpro.util.*;
import com.romzkie.ultrasshservice.*;
import com.romzkie.ultrasshservice.StatisticGraphData.*;
import com.romzkie.ultrasshservice.config.*;
import com.romzkie.ultrasshservice.logger.*;
import com.romzkie.ultrasshservice.tunnel.*;
import com.romzkie.ultrasshservice.util.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;

import android.support.v4.app.DialogFragment;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import com.romzkie.tunnelpro.R;
import com.romzkie.tunnelpro.adapter.SpinnerAdapter;

/**
 * Activity Principal
 * @author SlipkHunter
 */

public class SocksHttpMainActivity extends BaseActivity
	implements View.OnClickListener, RadioGroup.OnCheckedChangeListener,
CompoundButton.OnCheckedChangeListener, SkStatus.StateListener, RewardedVideoAdListener,PayloadGenerator.OnDismissListener
{
	private static final String TAG = SocksHttpMainActivity.class.getSimpleName();
	private static final String UPDATE_VIEWS = "MainUpdate";
	public static final String OPEN_LOGS = "com.romzkie.tunnelpro:openLogs";

    private LogsAdapter mLogAdapter;

    private RecyclerView logList;

    private ViewPager vp;

    private TabLayout tabs;

	private Switch customSetUP;

	private Spinner sportSetup;

	private Spinner portAuto;

	private Spinner methodSpinner;

	private DrawerPanelMain mDrawerPanel;
	
	private Settings mConfig;
	private Toolbar toolbar_main;
	private Handler mHandler;
	
	private LinearLayout mainLayout;
	private LinearLayout loginLayout;
	private LinearLayout proxyInputLayout;
	private LinearLayout ssl_layout;
	private TextView proxyText;
	private RadioGroup metodoConexaoRadio;
	private LinearLayout payloadLayout;
	private TextInputEditText sslEdit;
	private TextInputEditText payloadEdit;
	private SwitchCompat customPayloadSwitch;
	private Button starterButton;
	
	private ImageButton inputPwShowPass;
	private TextInputEditText inputPwUser;
	private TextInputEditText inputPwPass;
	
	private LinearLayout configMsgLayout;
	private TextView configMsgText;

	private AdView adsBannerView;

	private ConfigUtil config;

	private Spinner serverSpinner;
	private Spinner payloadSpinner;
    private static final String[] tabTitle = {"HOME","LOG"};
	private SpinnerAdapter serverAdapter;
	private SpinnerAdapter payloadAdapter;

	private ArrayList<JSONObject> serverList;
	private ArrayList<JSONObject> payloadList;
	
	String[] countryNames={"Custom Payload","Custom SNI"};
    int flags[] = {R.drawable.tweaks, R.drawable.tweaks};

	private CustomAdapter customAdapter;

	private TextView bytes_in, bytes_out;

	private SweetAlertDialog pDialog;
	
	private Button mButtonSet;

	private CountDownTimer mCountDownTimer;
	private boolean mTimerRunning;
	private long saved_ads_time;
	private long mTimeLeftInMillis;
	private long mEndTime;

	private long mTimeLeftBtn;

	private TextView mTextViewCountDown;

	private RewardedVideoAd rewardedAd;

	private boolean mTimerEnabled;

	private CountDownTimer mBtnCountDown;

	private SweetAlertDialog mDialog;
	
	private AlertDialog dialog;

	private InterstitialAd mInterstitialAd;
    
    private LinearLayout timer_layout;

	@Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

		mHandler = new Handler();
		mConfig = new Settings(this);
		mDrawerPanel = new DrawerPanelMain(this);
		new SMSuPdater(this);
		new TorrentDetection(this, torrentList).init();
		
		Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(this));
        
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); // portrait only
		
		SharedPreferences prefs = getSharedPreferences(SocksHttpApp.PREFS_GERAL, Context.MODE_PRIVATE);

		boolean showFirstTime = prefs.getBoolean("connect_first_time", true);
		int lastVersion = prefs.getInt("last_version", 0);

		// se primeira vez
		if (showFirstTime)
        {
            SharedPreferences.Editor pEdit = prefs.edit();
            pEdit.putBoolean("connect_first_time", false);
            pEdit.apply();

			Settings.setDefaultConfig(this);

			showBoasVindas();
        }

		try {
			int idAtual = ConfigParser.getBuildId(this);

			if (lastVersion < idAtual) {
				SharedPreferences.Editor pEdit = prefs.edit();
				pEdit.putInt("last_version", idAtual);
				pEdit.apply();

				// se estiver atualizando
				if (!showFirstTime) {
					if (lastVersion <= 12) {
						Settings.setDefaultConfig(this);
						Settings.clearSettings(this);

						Toast.makeText(this, "As configurações foram limpas para evitar bugs",
							Toast.LENGTH_LONG).show();
					}
				}

			}
		} catch(IOException e) {}
		
		
		// set layout
		doLayout();

		// verifica se existe algum problema
		SkProtect.CharlieProtect();

		// recebe local dados
		IntentFilter filter = new IntentFilter();
		filter.addAction(UPDATE_VIEWS);
		filter.addAction(OPEN_LOGS);
		
		LocalBroadcastManager.getInstance(this)
			.registerReceiver(mActivityReceiver, filter);
			
		doUpdateLayout();
	
		MobileAds.initialize(this, "ca-app-pub-7598967394813618~9683248207"); // App id
		// Use an activity context to get the rewarded video instance.
		rewardedAd = MobileAds.getRewardedVideoAdInstance(this);
		rewardedAd.setRewardedVideoAdListener(this);

		mTextViewCountDown = (TextView) findViewById(R.id.tvTimeRemaining);
        
        timer_layout = (LinearLayout) findViewById(R.id.timerLayout);
        
		mButtonSet = (Button) findViewById(R.id.btnAddTime);
		mButtonSet.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {

			        loadingAds();
			        loadRewardedVideoAd();
                        
				}
			});
			
	 welcomeNotif();
     setupInterstitial();
	}
    
    /** Interstitial Ads **/

	private void setupInterstitial(){

        mInterstitialAd = new InterstitialAd(this);
        
        mInterstitialAd.setAdUnitId("ca-app-pub-7598967394813618/5360859817"); // inter ads
		
		mInterstitialAd.setAdListener(new AdListener() {

			@Override
			public void onAdClosed() {
				// Code to be executed when the interstitial ad is closed.
				Toast.makeText(SocksHttpMainActivity.this, "Thank you for supporting the app !! 💙", Toast.LENGTH_SHORT)
					.show();
                    
                 loadInterstitial();
					
			}
		});
        
        loadInterstitial();
	}
    
    private void loadInterstitial(){

        mInterstitialAd.loadAd(new AdRequest.Builder().build());
        
    }
    
    private void showInterstitial(){
        if (mInterstitialAd.isLoaded()){
            mInterstitialAd.show(); 
         } else {
            loadInterstitial();    
        }                                                     
    }
    
    // end
	
	private void welcomeNotif(){

		NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE); 
		Notification.Builder notification = new Notification.Builder(this);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			notification.setChannelId(this.getPackageName() + ".romzkie");
			createNotification(notificationManager, this.getPackageName() + ".romzkie");
		}

		notification.setContentTitle("ROMZ VPN PRO")
			.setContentText("Developed by Romzkie Freenet PH")
			.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher))
			.setDefaults(Notification.DEFAULT_ALL)
			.setPriority(Notification.PRIORITY_HIGH)
			.setShowWhen(true)
			.setSmallIcon(R.drawable.ic_launcher);
		notificationManager.notify(4130,notification.getNotification());
	}

	private void createNotification(NotificationManager notificationManager, String id)
	{
		NotificationChannel mNotif = new NotificationChannel(id, "Developer", NotificationManager.IMPORTANCE_HIGH);
		mNotif.setShowBadge(true);
		notificationManager.createNotificationChannel(mNotif);
		// TODO: Implement this method
	}
	
	
	private String[] torrentList = new String[] {
		"com.termux",
		"com.tdo.showbox",
		"com.nitroxenon.terrarium",
		"com.pklbox.translatorspro",
		"com.xunlei.downloadprovider",
		"com.epic.app.iTorrent",
		"hu.bute.daai.amorg.drtorrent",
		"com.mobilityflow.torrent.prof",
		"com.brute.torrentolite",
		"com.nebula.swift",
		"tv.bitx.media",
		"com.DroiDownloader",
		"bitking.torrent.downloader",
		"org.transdroid.lite",
		"com.mobilityflow.tvp",
		"com.gabordemko.torrnado",
		"com.frostwire.android",
		"com.vuze.android.remote",
		"com.akingi.torrent",
		"com.utorrent.web",
		"com.paolod.torrentsearch2",
		"com.delphicoder.flud.paid",
		"com.teeonsoft.ztorrent",
		"megabyte.tdm",
		"com.bittorrent.client.pro",
		"com.mobilityflow.torrent",
		"com.utorrent.client",
		"com.utorrent.client.pro",
		"com.bittorrent.client",
		"torrent",
		"com.AndroidA.DroiDownloader",
		"com.indris.yifytorrents",
		"com.delphicoder.flud",
		"com.oidapps.bittorrent",
		"dwleee.torrentsearch",
		"com.vuze.torrent.downloader",
		"megabyte.dm",
		"com.fgrouptech.kickasstorrents",
		"com.jrummyapps.rootbrowser.classic",
		"com.bittorrent.client",
		"hu.tagsoft.ttorrent.lite",
		"co.we.torrent",
        "com.gmail.heagoo.apkeditor.pro"};
	
	
	private void start(){

		if (saved_ads_time == 0){

			Toast.makeText(SocksHttpMainActivity.this, "Your time is expiring soon, please click ADD TIME to renew access!", Toast.LENGTH_LONG).show();

			long millisInput = 1000 * 500;

			setTime(millisInput);
		}

		if (!mTimerRunning){
			startTimer();
		}

    }


	private void stop(){
		if (mTimerRunning){
			pauseTimer();
		}

	}

	private void addTime(long time){

		setTime(time);

		if (mTimerRunning){
			pauseTimer();
		}

		startTimer();
	}

	private void pauseTimer() {
		mCountDownTimer.cancel();
		mTimerRunning = false;

	}

	private void updateCountDownText(){

		long days = TimeUnit.MILLISECONDS.toDays(mTimeLeftInMillis);
		long daysMillis = TimeUnit.DAYS.toMillis(days);

		long hours = TimeUnit.MILLISECONDS.toHours(mTimeLeftInMillis - daysMillis);
		long hoursMillis = TimeUnit.HOURS.toMillis(hours);

		long minutes = TimeUnit.MILLISECONDS.toMinutes(mTimeLeftInMillis - daysMillis - hoursMillis);
		long minutesMillis = TimeUnit.MINUTES.toMillis(minutes);

		long seconds = TimeUnit.MILLISECONDS.toSeconds(mTimeLeftInMillis - daysMillis - hoursMillis - minutesMillis);

		String resultString = days + "d:" + hours + "h:" + minutes + "m:" + seconds + "s";

		mTextViewCountDown.setText(resultString);
	}

	private void setTime(long milliseconds) {

		saved_ads_time = mTimeLeftInMillis + milliseconds;

		mTimeLeftInMillis = saved_ads_time;
		updateCountDownText();

	}

	private void saveTime(){
		SharedPreferences saved_current_time = getSharedPreferences("time", Context.MODE_PRIVATE);

		SharedPreferences.Editor time_edit = saved_current_time.edit();

		time_edit.putLong("SAVED_TIME", mTimeLeftInMillis);

		time_edit.apply();
	}

	private void resumeTime(){
		SharedPreferences time = getSharedPreferences("time", Context.MODE_PRIVATE);
        long saved_time = time.getLong("SAVED_TIME", 0);

		setTime(saved_time);

		// Use this code to continue time if app close accidentally while connected

		String state = SkStatus.getLastState();

	    if (SkStatus.SSH_CONECTADO.equals(state)) {

			if (!mTimerRunning){
				startTimer();
			}
		}

		mTimerEnabled = true;
	}

	private void startTimer() {
		mEndTime = System.currentTimeMillis() + mTimeLeftInMillis;
		mCountDownTimer = new CountDownTimer(mTimeLeftInMillis, 1000) {


			@Override
			public void onTick(long millisUntilFinished) {
				mTimeLeftInMillis = millisUntilFinished;
				saveTime();
				updateCountDownText();
			}
			@Override
			public void onFinish() {
				mTimerRunning = false;
				pauseTimer();
				saved_ads_time = 0;

				// Code for auto stop vpn (sockshtttp)

				Intent stopVPN = new Intent(SocksHttpService.TUNNEL_SSH_STOP_SERVICE);
				LocalBroadcastManager.getInstance(SocksHttpMainActivity.this)
					.sendBroadcast(stopVPN);

				Toast.makeText(SocksHttpMainActivity.this,"Time expired! Click Add + Time to renew access!", Toast.LENGTH_LONG).show();

			}

		}.start();
		mTimerRunning = true;


	}

	private void btnTimer() {

		mBtnCountDown = new CountDownTimer(20000, 1000) {

			@Override
			public void onTick(long millisUntilFinished) {
				mTimeLeftBtn = millisUntilFinished;
				mButtonSet.setEnabled(false);
				updateBtnText();
			}
			@Override
			public void onFinish() {
				mButtonSet.setEnabled(true);
				mButtonSet.setText("ADD + TIME");
			}

		}.start();

	}

	private void updateBtnText() {
		int seconds = (int) (mTimeLeftBtn / 1000) % 60;
		String timeLeftFormatted;
		if (seconds > 0) {
			timeLeftFormatted = String.format(Locale.getDefault(),
											  "%02d", seconds);

			mButtonSet.setText("Refresh in " + timeLeftFormatted);

		}
	}


	private void loadingAds(){
        mDialog = new SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE);
        mDialog.getProgressHelper().setBarColor(Color.parseColor("#0062AF"));
        mDialog.setTitleText("Loading Rewarded Ad");
        mDialog.setContentText("Please wait while loading... \n\nNote: \nYou need to finish the video to claim your time reward");
        mDialog.setCancelable(true);
        mDialog.show();
	}

	private void showError(){
		new SweetAlertDialog(SocksHttpMainActivity.this, SweetAlertDialog.ERROR_TYPE)
            .setTitleText("Error Loading Ad")
            .setContentText("Failed to load Ad, please check your internet connection !! \n\nNote: If this error still continue please contact the developer for further assistance.")
			.show();
	}

	private void loadRewardedVideoAd() {
		rewardedAd.loadAd("ca-app-pub-7598967394813618/1349748026",
						  new AdRequest.Builder().build());
	}


	@Override
	public void onRewardedVideoAdLoaded() {
		if (rewardedAd.isLoaded()){
			rewardedAd.show();
		}
		mDialog.dismiss();
	}

	@Override
	public void onRewardedVideoAdOpened() {

	}

	@Override
	public void onRewardedVideoStarted() {

	}

	@Override
	public void onRewardedVideoAdClosed() {
		Toast.makeText(SocksHttpMainActivity.this,"Thank you for supporting the app !! 💙", Toast.LENGTH_LONG).show();
		btnTimer();
	}

	@Override
	public void onRewarded(RewardItem rewardItem) {
        long ads_time = 2 * rewardItem.getAmount() * 3600 * 1000;
		addTime(ads_time);
		Toast.makeText(SocksHttpMainActivity.this, "2 hours successfully added to your time!", Toast.LENGTH_LONG).show();
	}

	@Override
	public void onRewardedVideoAdLeftApplication() {
		Toast.makeText(SocksHttpMainActivity.this, "Why naman ganun?", Toast.LENGTH_LONG).show();
	}

	@Override
	public void onRewardedVideoAdFailedToLoad(int i) {

		showError();
		mDialog.dismiss();

	}
	

	/**
	 * Layout
	 */
	 
	private void doLayout() {
		setContentView(R.layout.activity_main_drawer);

		toolbar_main = (Toolbar) findViewById(R.id.toolbar_main);
		
		mDrawerPanel.setDrawer(toolbar_main);
		setSupportActionBar(toolbar_main);
		
		// set ADS
		adsBannerView = (AdView) findViewById(R.id.adBannerMainView);
	
		if (TunnelUtils.isNetworkOnline(SocksHttpMainActivity.this)) {
			adsBannerView.setAdListener(new AdListener() {
				@Override
				public void onAdLoaded() {
					if (adsBannerView != null) {
						adsBannerView.setVisibility(View.VISIBLE);
					}
				}
			});
			adsBannerView.loadAd(new AdRequest.Builder()
				.build());
		}
		
		mainLayout = (LinearLayout) findViewById(R.id.activity_mainLinearLayout);
		loginLayout = (LinearLayout) findViewById(R.id.activity_mainInputPasswordLayout);
		starterButton = (Button) findViewById(R.id.activity_starterButtonMain);

		inputPwUser = (TextInputEditText) findViewById(R.id.activity_mainInputPasswordUserEdit);
		inputPwPass = (TextInputEditText) findViewById(R.id.activity_mainInputPasswordPassEdit);

		inputPwShowPass = (ImageButton) findViewById(R.id.activity_mainInputShowPassImageButton);

		((TextView) findViewById(R.id.activity_mainAutorText))
			.setOnClickListener(this);

		proxyInputLayout = (LinearLayout) findViewById(R.id.activity_mainInputProxyLayout);
		proxyText = (TextView) findViewById(R.id.activity_mainProxyText);
      
        final SharedPreferences prefs = mConfig.getPrefsPrivate();
        SharedPreferences.Editor edit = prefs.edit();
		final SharedPreferences sPrefs = mConfig.getPrefsPrivate();
        sPrefs.edit().putBoolean(Settings.PROXY_USAR_DEFAULT_PAYLOAD, false).apply();
		sPrefs.edit().putInt(Settings.TUNNELTYPE_KEY, Settings.bTUNNEL_TYPE_SSH_PROXY).apply();
        config = new ConfigUtil(this);
		serverSpinner = (Spinner) findViewById(R.id.serverSpinner);
		payloadSpinner = (Spinner) findViewById(R.id.payloadSpinner);
      
		serverList = new ArrayList<>();
		payloadList = new ArrayList<>();

		serverAdapter = new SpinnerAdapter(this, R.id.serverSpinner, serverList);
		payloadAdapter = new SpinnerAdapter(this, R.id.payloadSpinner, payloadList);
		
		serverSpinner.setAdapter(serverAdapter);
		payloadSpinner.setAdapter(payloadAdapter);

		loadServer();
		loadNetworks();
		updateConfig(true);

		/*Spinner spinnerTunnelType = (Spinner) findViewById(R.id.activity_mainTunnelTypeSpinner);
		String[] items = new String[]{"SSH DIRECT", "SSH + PROXY", "SSH + SSL (beta)"};
		//create an adapter to describe how the items are displayed, adapters are used in several places in android.
		//There are multiple variations of this, but this is the basic variant.
		ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, items);
		//set the spinners adapter to the previously created one.
		spinnerTunnelType.setAdapter(adapter);*/
		
		metodoConexaoRadio = (RadioGroup) findViewById(R.id.activity_mainMetodoConexaoRadio);
		customPayloadSwitch = (SwitchCompat) findViewById(R.id.activity_mainCustomPayloadSwitch);

		starterButton.setOnClickListener(this);
		proxyInputLayout.setOnClickListener(this);

		payloadLayout = (LinearLayout) findViewById(R.id.activity_mainInputPayloadLinearLayout);
		payloadEdit = (TextInputEditText) findViewById(R.id.activity_mainInputPayloadEditText);
		
		ssl_layout = (LinearLayout) findViewById(R.id.activity_ssl_layout);
		sslEdit = (TextInputEditText) findViewById(R.id.activity_sni_edit);
		
		bytes_in = (TextView) findViewById (R.id.bytes_in);
		bytes_out = (TextView) findViewById (R.id.bytes_out);

		configMsgLayout = (LinearLayout) findViewById(R.id.activity_mainMensagemConfigLinearLayout);
		configMsgText = (TextView) findViewById(R.id.activity_mainMensagemConfigTextView);
		portAuto = (Spinner) findViewById(R.id.portAuto);
		List<String> Listportauto = new ArrayList<String>();
		Listportauto.add("AUTO");
		ArrayAdapter<String> Adptorportaut = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, Listportauto);


		Adptorportaut.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		portAuto.setAdapter(Adptorportaut);


		portAuto.setSelection(sPrefs.getInt("PortAuto", 0));



		portAuto.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
				@Override
				public void onItemSelected(AdapterView<?> p1, View p2, int position, long p4)
				{

					try
					{
						sPrefs.edit().putInt("PortAuto", position).apply();


						if(position == 0) {

							sPrefs.edit().putString(Settings.PROXY_PORTA_KEY, "").apply();
						}

						doUpdateLayout();
					}
					catch (Exception e)
					{}
				}

				@Override
				public void onNothingSelected(AdapterView<?> p1)
				{

				}
			});
		sportSetup = (Spinner) findViewById(R.id.portSpinner);

		List<String> Listport = new ArrayList<String>();
		Listport.add("80");
		Listport.add("443");
		Listport.add("3128");
		Listport.add("8080");
		Listport.add("8081");
		Listport.add("8789");
		Listport.add("8799");
		Listport.add("8888");
		Listport.add("8000");

		ArrayAdapter<String> Adptorport = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, Listport);

		Adptorport.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		sportSetup.setAdapter(Adptorport);

		sportSetup.setSelection(sPrefs.getInt("Port", 0));

		sportSetup.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
				@Override
				public void onItemSelected(AdapterView<?> p1, View p2, int position, long p4)
				{

					try
					{
						sPrefs.edit().putInt("Port", position).apply();


						if(position == 0) {

							sPrefs.edit().putString(Settings.PROXY_PORTA_KEY, "80").apply();

						}else if(position == 1){



							sPrefs.edit().putString(Settings.PROXY_PORTA_KEY, "443").apply();		


						}else if(position == 2){



							sPrefs.edit().putString(Settings.PROXY_PORTA_KEY, "3128").apply();		
						}else if(position == 3){



							sPrefs.edit().putString(Settings.PROXY_PORTA_KEY, "8080").apply();		

						}else if(position == 4){



							sPrefs.edit().putString(Settings.PROXY_PORTA_KEY, "8081").apply();		

						}else if(position == 5){



							sPrefs.edit().putString(Settings.PROXY_PORTA_KEY, "8789").apply();		


						}else if(position == 6){



							sPrefs.edit().putString(Settings.PROXY_PORTA_KEY, "8799").apply();	


						}else if(position == 7){



							sPrefs.edit().putString(Settings.PROXY_PORTA_KEY, "8888").apply();		



						}else if(position == 8){



							sPrefs.edit().putString(Settings.PROXY_PORTA_KEY, "8000").apply();		


						}

						doUpdateLayout();
					}
					catch (Exception e)
					{}
				}

				@Override
				public void onNothingSelected(AdapterView<?> p1)
				{

				}
			});

		methodSpinner = (Spinner) findViewById(R.id.methodSpinner);

		customAdapter = new CustomAdapter(this,flags,countryNames);

		methodSpinner.setAdapter(customAdapter);

		methodSpinner.setSelection(sPrefs.getInt("method", 0));

		methodSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){

				@Override
				public void onItemSelected(AdapterView<?> p1, View p2, int position, long p4)
				{

					try
					{

						sPrefs.edit().putInt("method", position).apply();


						sPrefs.edit().putBoolean(Settings.PROXY_USAR_DEFAULT_PAYLOAD, false).apply();


						if (position == 0){ // SSH

							SharedPreferences prefs = mConfig.getPrefsPrivate();
							payloadEdit.setText(prefs.getString("CustomPayload", ""));
							
							SharedPreferences.Editor edit = prefs.edit();
							edit.putInt("TunneType", 1).apply();
							
							setupSSH();
							
							payloadLayout.setVisibility(View.VISIBLE);
							ssl_layout.setVisibility(View.GONE);
							
							portAuto.setVisibility(View.GONE);
							portAuto.setClickable(false);
							portAuto.setEnabled(false);
							sportSetup.setVisibility(View.VISIBLE);
							sportSetup.setClickable(true);
							sportSetup.setEnabled(true);
				
						}else if(position == 1){ // SSL
							
							SharedPreferences prefs = mConfig.getPrefsPrivate();
							sslEdit.setText(prefs.getString("CustomSNI", ""));

							SharedPreferences.Editor edit = prefs.edit();
							edit.putInt("TunneType", 2).apply();
							
							setupSSL();
							
                            payloadLayout.setVisibility(View.GONE);
							ssl_layout.setVisibility(View.VISIBLE);
							
							portAuto.setVisibility(View.VISIBLE);
							portAuto.setClickable(true);
							portAuto.setEnabled(true);
							sportSetup.setVisibility(View.GONE);
							sportSetup.setClickable(false);
							sportSetup.setEnabled(false);

						}
						//Atualiza informações
						doUpdateLayout();
					}
					catch (Exception e)
					{}
				}

				@Override
				public void onNothingSelected(AdapterView<?> p1)
				{

				}
			});
			
		customSetUP = (Switch) findViewById(R.id.customSetup);
		customSetUP.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					if(customSetUP.isChecked())
					{
						
						SharedPreferences prefs = mConfig.getPrefsPrivate();
						SharedPreferences.Editor edit = prefs.edit();
						edit.putInt("CustomSetup", 1).apply();
						//mPrefs.getBoolean(Settings.TETHERING_SUBNET, true);

						portAuto.setVisibility(View.GONE);
						portAuto.setClickable(false);
						portAuto.setEnabled(false);
						sportSetup.setVisibility(View.VISIBLE);
						sportSetup.setClickable(true);
						sportSetup.setEnabled(true);

						payloadSpinner.setVisibility(View.GONE);
						payloadSpinner.setClickable(false);
						payloadSpinner.setEnabled(false);
						methodSpinner.setVisibility(View.VISIBLE);
						methodSpinner.setClickable(true);
						methodSpinner.setEnabled(true);
						
						if (prefs.getInt("TunneType", 0) == 1){ // SSH
						
						payloadLayout.setVisibility(View.VISIBLE);
						ssl_layout.setVisibility(View.GONE);
						
					    setupSSH();
						
						}else{ // SSL
						
							payloadLayout.setVisibility(View.GONE);
							ssl_layout.setVisibility(View.VISIBLE);
							methodSpinner.setSelection(1);
							
							setupSSL();
							
							portAuto.setVisibility(View.VISIBLE);
							portAuto.setClickable(true);
							portAuto.setEnabled(true);
							sportSetup.setVisibility(View.GONE);
							sportSetup.setClickable(false);
							sportSetup.setEnabled(false);
							
				
						}

					}


					else {
						

						SharedPreferences prefs = mConfig.getPrefsPrivate();
						SharedPreferences.Editor edit = prefs.edit();
						edit.putInt("CustomSetup", 0).apply();

						payloadSpinner.setVisibility(View.VISIBLE);
						payloadSpinner.setClickable(true);
						payloadSpinner.setEnabled(true);
						methodSpinner.setVisibility(View.GONE);
						methodSpinner.setClickable(false);
						methodSpinner.setEnabled(false);

						portAuto.setVisibility(View.VISIBLE);
						portAuto.setClickable(true);
						portAuto.setEnabled(true);
						sportSetup.setVisibility(View.GONE);
						sportSetup.setClickable(false);
						sportSetup.setEnabled(false);
						
						payloadLayout.setVisibility(View.GONE);
						ssl_layout.setVisibility(View.GONE);


					}
					
				}		
				
			});
		
	
		// fix bugs
		if (mConfig.getPrefsPrivate().getBoolean(Settings.CONFIG_PROTEGER_KEY, false)) {
			if (mConfig.getPrefsPrivate().getBoolean(Settings.CONFIG_INPUT_PASSWORD_KEY, false)) {
				inputPwUser.setText(mConfig.getPrivString(Settings.USUARIO_KEY));
				inputPwPass.setText(mConfig.getPrivString(Settings.SENHA_KEY));
			}
		}
        customPayloadSwitch.setChecked(true);
        
        edit.putBoolean(Settings.PROXY_USAR_DEFAULT_PAYLOAD, !true);
       
		metodoConexaoRadio.setOnCheckedChangeListener(this);
		// customPayloadSwitch.setOnCheckedChangeListener(this);
		inputPwShowPass.setOnClickListener(this);
        
        doTabs();
	}
	
	private void loading(){
		pDialog = new SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE);
		pDialog.getProgressHelper().setBarColor(Color.parseColor("#0062AF"));
		pDialog.setTitleText("Checking Updates");
		pDialog.setContentText("Please wait while loading...");
		pDialog.setCancelable(true);
		pDialog.show();
	}
	
	private void setupSSH(){
		
		try
		{
			SharedPreferences prefs = mConfig.getPrefsPrivate();
			prefs.edit().putInt(Settings.TUNNELTYPE_KEY, Settings.bTUNNEL_TYPE_SSH_PROXY).apply();

			SharedPreferences.Editor edit = prefs.edit();
		
			int pos1 = serverSpinner.getSelectedItemPosition();
			String ssh_port = config.getServersArray().getJSONObject(pos1).getString("ServerPort");
			edit.putString(Settings.SERVIDOR_PORTA_KEY, ssh_port).apply();

			if (prefs.getString(Settings.PROXY_PORTA_KEY, "").isEmpty()){
				edit.putString(Settings.PROXY_PORTA_KEY, "8080").apply();
				sportSetup.setSelection(3);
			}
			
			
		}
		catch (Exception e)
		{}
	}
	
	private void setupSSL(){
		
		setupSSH(); // fix bug
			
		SharedPreferences prefs = mConfig.getPrefsPrivate();
		prefs.edit().putInt(Settings.TUNNELTYPE_KEY, Settings.bTUNNEL_TYPE_SSH_SSL).apply();

		SharedPreferences.Editor edit = prefs.edit();
		edit.putString(Settings.SERVIDOR_PORTA_KEY, "443").apply();
			
	}

	

    @Override
    public void onCheckedChanged(CompoundButton p1, boolean p2) {
    }
	
	public class DrawerPanelMain
	implements NavigationView.OnNavigationItemSelectedListener
	{
		private AppCompatActivity mActivity;

		public DrawerPanelMain(AppCompatActivity activity) {
			mActivity = activity;
		}


		private DrawerLayout drawerLayout;
		private ActionBarDrawerToggle toggle;

		public void setDrawer(Toolbar toolbar) {
			NavigationView drawerNavigationView = (NavigationView) mActivity.findViewById(R.id.drawerNavigationView);
			drawerLayout = (DrawerLayout) mActivity.findViewById(R.id.drawerLayoutMain);

			// set drawer
			toggle = new ActionBarDrawerToggle(mActivity,
											   drawerLayout, toolbar, R.string.open, R.string.cancel);

			drawerLayout.setDrawerListener(toggle);

			toggle.syncState();

			// set app info
			PackageInfo pinfo = Utils.getAppInfo(mActivity);
			if (pinfo != null) {
				String version_nome = pinfo.versionName;
				int version_code = pinfo.versionCode;
				String header_text = String.format("v. %s (%d)", version_nome, version_code);

				View view = drawerNavigationView.getHeaderView(0);

				TextView app_info_text = view.findViewById(R.id.nav_headerAppVersion);
				app_info_text.setText(header_text);
			}

			// set navigation view
			drawerNavigationView.setNavigationItemSelectedListener(this);
		}

		public ActionBarDrawerToggle getToogle() {
			return toggle;
		}

		public DrawerLayout getDrawerLayout() {
			return drawerLayout;
		}

		@Override
		public boolean onNavigationItemSelected(@NonNull MenuItem item) {
			int id = item.getItemId();

			switch(id)
			{
				case R.id.paygen:
					if (SkStatus.isTunnelActive()) {
						Toast.makeText(SocksHttpMainActivity.this, "VPN service is running, stop it first!",Toast.LENGTH_SHORT).show();

					}
					else {
						if (customSetUP.isChecked()) {
							PayloadGenerator();
						}else{
							Toast.makeText(SocksHttpMainActivity.this, "Please enable Custom Setup!",Toast.LENGTH_SHORT).show();

						}
					}
					drawerLayout.closeDrawers();
				break;
				
				case R.id.update:
					
				loading();
				updateConfig(false);
			    drawerLayout.closeDrawers();
				
				break;
				case R.id.miPhoneConfg:
					PackageInfo app_info = Utils.getAppInfo(mActivity);
					if (app_info != null) {
						String aparelho_marca = Build.BRAND.toUpperCase();

						if (aparelho_marca.equals("SAMSUNG") || aparelho_marca.equals("HUAWEY")) {
							Toast.makeText(mActivity, R.string.error_no_supported, Toast.LENGTH_SHORT)
								.show();
						}
						else {
							try {
								Intent in = new Intent(Intent.ACTION_MAIN);
								in.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
								in.setClassName("com.android.settings", "com.android.settings.RadioInfo");
								mActivity.startActivity(in);
							} catch(Exception e) {
								Toast.makeText(mActivity, R.string.error_no_supported, Toast.LENGTH_SHORT)
									.show();
							}
						}
					}
					break;

				case R.id.miSettings:
					Intent intent = new Intent(mActivity, ConfigGeralActivity.class);
					intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					mActivity.startActivity(intent);
					break;

				case R.id.fb:
					String url = "https://www.facebook.com/romzkieph/";
					Intent intent3 = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
					intent3.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					mActivity.startActivity(Intent.createChooser(intent3, mActivity.getText(R.string.open_with)));
					break;
                    
                case R.id.fb_group:
                    String url2 = "https://www.facebook.com/groups/176844084354684/";
                    Intent intent4 = new Intent(Intent.ACTION_VIEW, Uri.parse(url2));
                    intent4.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    mActivity.startActivity(Intent.createChooser(intent4, mActivity.getText(R.string.open_with)));
					break;

				case R.id.miSendFeedback:
					if (false && Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
						try {
							GoogleFeedbackUtils.bindFeedback(mActivity);
						} catch (Exception e) {
							Toast.makeText(mActivity, "Não disponível em seu aparelho", Toast.LENGTH_SHORT)
								.show();
							SkStatus.logDebug("Error: " + e.getMessage());
						}
					}
					else {
						Intent email = new Intent(Intent.ACTION_SEND);  
						email.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

						email.putExtra(Intent.EXTRA_EMAIL, new String[]{"brosasromel01@gmail.com"});  
						email.putExtra(Intent.EXTRA_SUBJECT, "ROMZ VPN PRO - " + mActivity.getString(R.string.feedback));  
						//email.putExtra(Intent.EXTRA_TEXT, "");  

						//need this to prompts email client only  
						email.setType("message/rfc822");  

						mActivity.startActivity(Intent.createChooser(email, "Choose an Email client:"));
					}
					break;

				case R.id.miAbout:
					if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
						drawerLayout.closeDrawers();
					}
					Intent aboutIntent = new Intent(mActivity, AboutActivity.class);
					mActivity.startActivity(aboutIntent);
					break;
			}

			return true;
		}

	}
    
    public void doTabs() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        mLogAdapter = new LogsAdapter(layoutManager,this);
        logList = (RecyclerView) findViewById(R.id.recyclerLog);
        logList.setAdapter(mLogAdapter);
        logList.setLayoutManager(layoutManager);
        mLogAdapter.scrollToLastPosition();
        vp = (ViewPager)findViewById(R.id.viewpager);
        tabs = (TabLayout)findViewById(R.id.tablayout);
        vp.setAdapter(new MyAdapter(Arrays.asList(tabTitle)));
        vp.setOffscreenPageLimit(2);
        tabs.setTabMode(TabLayout.MODE_FIXED);
        tabs.setTabGravity(TabLayout.GRAVITY_FILL);
        tabs.setupWithViewPager(vp);
        }
        
    public class MyAdapter extends PagerAdapter
    {

        @Override
        public int getCount()
        {
            // TODO: Implement this method
            return 2;
        }

        @Override
        public boolean isViewFromObject(View p1, Object p2)
        {
            // TODO: Implement this method
            return p1 == p2;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position)
        {
            int[] ids = new int[]{R.id.tab1, R.id.tab2};
            int id = 0;
            id = ids[position];
            // TODO: Implement this method
            return findViewById(id);
        }

        @Override
        public CharSequence getPageTitle(int position)
        {
            // TODO: Implement this method
            return titles.get(position);
        }

        private List<String> titles;
        public MyAdapter(List<String> str)
        {
            titles = str;
        }
	}
        
	private void doUpdateLayout() {
		SharedPreferences prefs = mConfig.getPrefsPrivate();

		boolean isRunning = SkStatus.isTunnelActive();
		int tunnelType = prefs.getInt(Settings.TUNNELTYPE_KEY, Settings.bTUNNEL_TYPE_SSH_DIRECT);
		
		setStarterButton(starterButton, this);

		String proxyStr = getText(R.string.no_value).toString();

		if (prefs.getBoolean(Settings.CONFIG_PROTEGER_KEY, false)) {
			proxyStr = "*******";
			proxyInputLayout.setEnabled(false);
		}
		else {
			String proxy = mConfig.getPrivString(Settings.PROXY_IP_KEY);

			if (proxy != null && !proxy.isEmpty())
				proxyStr = String.format("%s:%s", proxy, mConfig.getPrivString(Settings.PROXY_PORTA_KEY));
			proxyInputLayout.setEnabled(!isRunning);
		} 

		proxyText.setText(proxyStr);


		switch (tunnelType) {
			case Settings.bTUNNEL_TYPE_SSH_DIRECT:
				((AppCompatRadioButton) findViewById(R.id.activity_mainSSHDirectRadioButton))
					.setChecked(true);
				break;

			case Settings.bTUNNEL_TYPE_SSH_PROXY:
				((AppCompatRadioButton) findViewById(R.id.activity_mainSSHProxyRadioButton))
					.setChecked(true);
                break;
            case Settings.bTUNNEL_TYPE_SSH_SSL:
                ((AppCompatRadioButton) findViewById(R.id.activity_mainSSHSSLRadioButton))
                    .setChecked(true);
                break;
		}

		int msgVisibility = View.GONE;
		int loginVisibility = View.GONE;
		String msgText = "";
		boolean enabled_radio = !isRunning;

		if (prefs.getBoolean(Settings.CONFIG_PROTEGER_KEY, false)) {
			
			if (prefs.getBoolean(Settings.CONFIG_INPUT_PASSWORD_KEY, false)) {
				loginVisibility = View.VISIBLE;
				
				inputPwUser.setText(mConfig.getPrivString(Settings.USUARIO_KEY));
				inputPwPass.setText(mConfig.getPrivString(Settings.SENHA_KEY));
				
				inputPwUser.setEnabled(!isRunning);
				inputPwPass.setEnabled(!isRunning);
				inputPwShowPass.setEnabled(!isRunning);
				
				//inputPwPass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
			}
			
			String msg = mConfig.getPrivString(Settings.CONFIG_MENSAGEM_KEY);
			if (!msg.isEmpty()) {
				msgText = msg.replace("\n", "<br/>");
				msgVisibility = View.VISIBLE;
			}
			
			if (mConfig.getPrivString(Settings.PROXY_IP_KEY).isEmpty() ||
					mConfig.getPrivString(Settings.PROXY_PORTA_KEY).isEmpty()) {
				enabled_radio = false;
			}
		}

		loginLayout.setVisibility(loginVisibility);
		configMsgText.setText(msgText.isEmpty() ? "" : Html.fromHtml(msgText));
		configMsgLayout.setVisibility(msgVisibility);
		
		// desativa/ativa radio group
		for (int i = 0; i < metodoConexaoRadio.getChildCount(); i++) {
			metodoConexaoRadio.getChildAt(i).setEnabled(enabled_radio);
		}
	}
	
	
	private synchronized void doSaveData() {
		try {
			SharedPreferences prefs = mConfig.getPrefsPrivate();
			SharedPreferences.Editor edit = prefs.edit();
			
			edit.apply();
			if (mainLayout != null && !isFinishing())
				mainLayout.requestFocus();

			if (!prefs.getBoolean(Settings.CONFIG_PROTEGER_KEY, false)) {
				if (payloadEdit != null && !prefs.getBoolean(Settings.PROXY_USAR_DEFAULT_PAYLOAD, true)) {
					int pos = payloadSpinner.getSelectedItemPosition();
                    // int modeType = prefs.getInt("TunnelMode",modeGroup.getCheckedRadioButtonId());
					
				
					if (prefs.getInt("CustomSetup", 0) == 1){ // Custom setup on
						
						if (prefs.getInt("TunneType", 0) == 1){ // SSH
						
						String payload = payloadEdit.getText().toString();
						edit.putString(Settings.CUSTOM_PAYLOAD_KEY, payload);
						
						edit.putString("CustomPayload", payload).apply();
						
						}else{ // SSL
							
							String sni = sslEdit.getText().toString();
							edit.putString(Settings.CUSTOM_PAYLOAD_KEY, sni);

							edit.putString("CustomSNI", sni).apply();
							
							}

					}else{
					
                   
                   boolean sslType = config.getNetworksArray().getJSONObject(pos).getBoolean("isSSL");
               
               
                   if (sslType) {
                       prefs.edit().putInt(Settings.TUNNELTYPE_KEY, Settings.bTUNNEL_TYPE_SSH_SSL).apply();
                       String sni = config.getNetworksArray().getJSONObject(pos).getString("SNI");
                       edit.putString(Settings.CUSTOM_PAYLOAD_KEY, sni);
                   } else {
                       prefs.edit().putInt(Settings.TUNNELTYPE_KEY, Settings.bTUNNEL_TYPE_SSH_PROXY).apply();
                       String payload = config.getNetworksArray().getJSONObject(pos).getString("Payload");
                       edit.putString(Settings.CUSTOM_PAYLOAD_KEY, payload);
                   }
				   
				} // end
				   
				   
                   
                   
             /*       switch (modeType) {
                       case R.id.ssh_mode:
                           String payload = config.getNetworksArray().getJSONObject(pos).getString("Payload");
                           edit.putString(Settings.CUSTOM_PAYLOAD_KEY, payload);
                           break;
                       case R.id.ssl_mode:
                           String sni = config.getNetworksArray().getJSONObject(pos).getString("SNI");
                           edit.putString(Settings.CUSTOM_PAYLOAD_KEY, sni);
                           break;
                   } */
					
				}
			}
			else {
				if (prefs.getBoolean(Settings.CONFIG_INPUT_PASSWORD_KEY, false)) {
					edit.putString(Settings.USUARIO_KEY, inputPwUser.getEditableText().toString());
					edit.putString(Settings.SENHA_KEY, inputPwPass.getEditableText().toString());
				}
			}

			edit.apply();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void setSpinner(){
		SharedPreferences prefs = mConfig.getPrefsPrivate();
        int server = prefs.getInt("LastSelectedServer", 0);
        int payload = prefs.getInt("LastSelectedPayload", 0);
        serverSpinner.setSelection(server);
        payloadSpinner.setSelection(payload);
	}
	
	private void saveSpinner(){
		SharedPreferences prefs = mConfig.getPrefsPrivate();
        SharedPreferences.Editor edit = prefs.edit();
        int server = serverSpinner.getSelectedItemPosition();
        int payload = payloadSpinner.getSelectedItemPosition();
        edit.putInt("LastSelectedServer", server);
        edit.putInt("LastSelectedPayload", payload);
        edit.apply();
	}
	

	private void loadServerData() {
		try {
			SharedPreferences prefs = mConfig.getPrefsPrivate();
			SharedPreferences.Editor edit = prefs.edit();
			
			serverSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){

                    @Override
                    public void onItemSelected(AdapterView<?> p1, View p2, int p3, long p4) {
                        SharedPreferences prefs = mConfig.getPrefsPrivate();
                        SharedPreferences.Editor edit = prefs.edit();
                        edit.putInt("LastSelectedServer", p3).apply();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> p1) {
                    }
                });

            payloadSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){

                    @Override
                    public void onItemSelected(AdapterView<?> p1, View p2, int p3, long p4) {
                        SharedPreferences prefs = mConfig.getPrefsPrivate();
                        SharedPreferences.Editor edit = prefs.edit();
                        edit.putInt("LastSelectedPayload", p3).apply();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> p1) {
                    }
                });
			
        //    int modeType = prefs.getInt("TunnelMode",modeGroup.getCheckedRadioButtonId());
            int pos1 = serverSpinner.getSelectedItemPosition();
            int pos2 = payloadSpinner.getSelectedItemPosition();
			
			boolean sslType = config.getNetworksArray().getJSONObject(pos2).getBoolean("isSSL");
			
		    if (prefs.getInt("CustomSetup", 0) == 0){ // Custom setup off

				if (sslType) {
					String ssl_port = config.getServersArray().getJSONObject(pos1).getString("SSLPort");
					edit.putString(Settings.SERVIDOR_PORTA_KEY, ssl_port);
				} else {
					String ssh_port = config.getServersArray().getJSONObject(pos1).getString("ServerPort");
					edit.putString(Settings.SERVIDOR_PORTA_KEY, ssh_port);
				}
			} // end
            
			String ssh_server = config.getServersArray().getJSONObject(pos1).getString("ServerIP");
			String remote_proxy = config.getServersArray().getJSONObject(pos1).getString("ProxyIP");
			String proxy_port = config.getServersArray().getJSONObject(pos1).getString("ProxyPort");
			String ssh_user = config.getServersArray().getJSONObject(pos1).getString("ServerUser");
			String ssh_pass = config.getServersArray().getJSONObject(pos1).getString("ServerPass");

			edit.putString(Settings.USUARIO_KEY, ssh_user);
			edit.putString(Settings.SENHA_KEY, ssh_pass);
			edit.putString(Settings.SERVIDOR_KEY, ssh_server);
			edit.putString(Settings.PROXY_IP_KEY, remote_proxy);
			
			if (prefs.getInt("CustomSetup", 0) == 0){  // Custom setup off
			  edit.putString(Settings.PROXY_PORTA_KEY, proxy_port);
			} // end

			edit.apply();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void loadServer() {
		try {
			if (serverList.size() > 0) {
				serverList.clear();
				serverAdapter.notifyDataSetChanged();
			}
			for (int i = 0; i < config.getServersArray().length(); i++) {
				JSONObject obj = config.getServersArray().getJSONObject(i);
				serverList.add(obj);
				serverAdapter.notifyDataSetChanged();

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void loadNetworks() {
		try {
			if (payloadList.size() > 0) {
				payloadList.clear();
				payloadAdapter.notifyDataSetChanged();
			}
			for (int i = 0; i < config.getNetworksArray().length(); i++) {
				JSONObject obj = config.getNetworksArray().getJSONObject(i);
				payloadList.add(obj);
				payloadAdapter.notifyDataSetChanged();

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void updateConfig(final boolean isOnCreate) {
		new ConfigUpdate(this, new ConfigUpdate.OnUpdateListener() {
			@Override
			public void onUpdateListener(String result) {
				try {
					if (!result.contains("Error on getting data")) {
						String json_data = AESCrypt.decrypt(config.PASSWORD, result);
						if (isNewVersion(json_data)) {
							newUpdateDialog(result);
						} else {
							if (!isOnCreate) {
								noUpdateDialog();
							}
						}
					} else if(result.contains("Error on getting data") && !isOnCreate){
						errorUpdateDialog(result);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start(isOnCreate);
	}

	private boolean isNewVersion(String result) {
		try {
			String current = config.getVersion();
			String update = new JSONObject(result).getString("Version");
			return config.versionCompare(update, current);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return false;
	}

	
	private void newUpdateDialog(final String result){

        new SweetAlertDialog(SocksHttpMainActivity.this, SweetAlertDialog.WARNING_TYPE)
            .setTitleText("New Update Available")
            .setContentText("Please update to get the latest Version.")
            .setConfirmText("Update Now")
            .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {

                @Override
                public void onClick(SweetAlertDialog sweetAlertDialog)
                {
                    // TODO: Implement this method
                    try
                    {
                        File file = new File(getFilesDir(), "Config.json");
                        OutputStream out = new FileOutputStream(file);
                        out.write(result.getBytes());
                        out.flush();
                        out.close();
                        restart_app();
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }

                }})

            .show();
		pDialog.dismiss();

    }

	private void noUpdateDialog() {
        new SweetAlertDialog(SocksHttpMainActivity.this, SweetAlertDialog.SUCCESS_TYPE)
            .setTitleText("ROMZ VPN PRO")
            .setContentText("Your config is on Latest Version")
            .show();
		pDialog.dismiss();
	}

	private void errorUpdateDialog(String error) {
        new SweetAlertDialog(SocksHttpMainActivity.this, SweetAlertDialog.ERROR_TYPE)
            .setTitleText("Error on Update")
            .setContentText("There is an error occurred while checking for update.\n" +
                            "Note: If this error still continue please contact the developer for further assistance.")
            .show();
		pDialog.dismiss();
	}
	

	private void restart_app() {
		Intent intent = new Intent(this, SocksHttpMainActivity.class);
		int i = 123456;
		PendingIntent pendingIntent = PendingIntent.getActivity(this, i, intent, PendingIntent.FLAG_CANCEL_CURRENT);
		AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		alarmManager.set(AlarmManager.RTC, System.currentTimeMillis() + ((long) 1000), pendingIntent);
		finish();
	}
	/**
	 * Tunnel SSH
	 */

	public void startOrStopTunnel(Activity activity) {
		if (SkStatus.isTunnelActive()) {
			TunnelManagerHelper.stopSocksHttp(activity);
		}
		else {
			// oculta teclado se vísivel, tá com bug, tela verde
			//Utils.hideKeyboard(activity);
			
			Settings config = new Settings(activity);
			
			if (config.getPrefsPrivate()
					.getBoolean(Settings.CONFIG_INPUT_PASSWORD_KEY, false)) {
				if (inputPwUser.getText().toString().isEmpty() || 
						inputPwPass.getText().toString().isEmpty()) {
					Toast.makeText(this, R.string.error_userpass_empty, Toast.LENGTH_SHORT)
						.show();
					return;
				}
			}
			
			Intent intent = new Intent(activity, LaunchVpn.class);
			intent.setAction(Intent.ACTION_MAIN);
			
			if (config.getHideLog()) {
				intent.putExtra(LaunchVpn.EXTRA_HIDELOG, true);
			}
			
			activity.startActivity(intent);
		}
	}
    
    private String con, country, tweaks, payloadInfo, selectedServer;
    
    private void showTweaks(){
  
        try {
            int server = serverSpinner.getSelectedItemPosition();
            int payload = payloadSpinner.getSelectedItemPosition();
            selectedServer = config.getServersArray().getJSONObject(server).getString("Name");
            String selectedPayload = config.getNetworksArray().getJSONObject(payload).getString("Name");

            String pInfo = config.getNetworksArray().getJSONObject(payload).getString("Info");
            boolean directModeType = config.getNetworksArray().getJSONObject(payload).getBoolean("isSSL");
            if (directModeType) {
                con = "SSH/SSL";
            } else {
                con = "SSH/Proxy";
            }
            
            country = selectedServer;
            tweaks = selectedPayload;
            payloadInfo = "Promo Needed: " + pInfo;

            
        } catch (JSONException e) {
            e.printStackTrace();
        }
        
        SharedPreferences prefs = mConfig.getPrefsPrivate();

        if (prefs.getInt("CustomSetup", 0) == 1){ // Custom Setup

            if (prefs.getInt("TunneType", 0) == 1){ // SSH
            
                con = "SSH/Proxy";
                tweaks = "Custom Payload";
                payloadInfo = "";

            }else if (prefs.getInt("TunneType", 0) == 2){ // SSL
            
                con = "SSH/SSL";
                tweaks = "Custom SNI";
                payloadInfo = "";

			}
            
         }
        
        new SweetAlertDialog(SocksHttpMainActivity.this, SweetAlertDialog.WARNING_TYPE)
            .setTitleText("Tunnel Type: "+con)
            .setContentText("Selected Server: "+country + "\n" + "Selected Tweaks: " + tweaks + "\n" + payloadInfo)
            .setConfirmText("Connect")
            .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {

                @Override
                public void onClick(SweetAlertDialog sweetAlertDialog)
                {
                    // TODO: Implement this method
                    startOrStopTunnel(SocksHttpMainActivity.this);
                    doSaveData();
                    loadServerData();
                    start();
				    saveSpinner();
                    
                    sweetAlertDialog.dismiss();
                    
                }})
            .setCancelText("Cancel")
            .showCancelButton(true)
            .setCancelClickListener(new SweetAlertDialog.OnSweetClickListener(){

                @Override
                public void onClick(SweetAlertDialog sweetAlertDialog)
                {
                  sweetAlertDialog.dismiss();
                }


            })

            .show();
    }

	public void setStarterButton(Button starterButton, Activity activity) {
		String state = SkStatus.getLastState();
		boolean isRunning = SkStatus.isTunnelActive();

		if (starterButton != null) {
			int resId;
			
			SharedPreferences prefsPrivate = new Settings(activity).getPrefsPrivate();

			if (ConfigParser.isValidadeExpirou(prefsPrivate
					.getLong(Settings.CONFIG_VALIDADE_KEY, 0))) {
				resId = R.string.expired;
				starterButton.setEnabled(false);

				if (isRunning) {
					startOrStopTunnel(activity);
				}
			}
			else if (prefsPrivate.getBoolean(Settings.BLOQUEAR_ROOT_KEY, false) &&
					ConfigParser.isDeviceRooted(activity)) {
			   resId = R.string.blocked;
			   starterButton.setEnabled(false);
			   
			   Toast.makeText(activity, R.string.error_root_detected, Toast.LENGTH_SHORT)
					.show();

			   if (isRunning) {
				   startOrStopTunnel(activity);
			   }
			}
			else if (SkStatus.SSH_INICIANDO.equals(state)) {
				resId = R.string.stop;
				starterButton.setEnabled(false);
				methodSpinner.setEnabled(false);
				customSetUP.setEnabled(false);
				serverSpinner.setEnabled(false);
				payloadSpinner.setEnabled(false);
				sslEdit.setEnabled(false);
				payloadEdit.setEnabled(false);
				sportSetup.setEnabled(false);
				portAuto.setEnabled(false);
			}
			else if (SkStatus.SSH_PARANDO.equals(state)) {
				resId = R.string.state_stopping;
				starterButton.setEnabled(false);
			}else if (SkStatus.SSH_DESCONECTADO.equals(state)){
				resId = R.string.start;
				starterButton.setEnabled(true);
				customSetUP.setEnabled(true);
				methodSpinner.setEnabled(true);
				serverSpinner.setEnabled(true);
				payloadSpinner.setEnabled(true);
				sslEdit.setEnabled(true);
				payloadEdit.setEnabled(true);
				sportSetup.setEnabled(true);
				portAuto.setEnabled(true);
				stop();
                timer_layout.setVisibility(View.GONE);
                
				}else {
				resId = isRunning ? R.string.stop : R.string.start;
				starterButton.setEnabled(true);
			}

			starterButton.setText(resId);
		}
	}
	

	
	@Override
    public void onPostCreate(Bundle savedInstanceState, PersistableBundle persistentState) {
        super.onPostCreate(savedInstanceState, persistentState);
        if (mDrawerPanel.getToogle() != null)
			mDrawerPanel.getToogle().syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mDrawerPanel.getToogle() != null)
			mDrawerPanel.getToogle().onConfigurationChanged(newConfig);
    }
	
	private boolean isMostrarSenha = false;
	
	@Override
	public void onClick(View p1)
	{
		SharedPreferences prefs = mConfig.getPrefsPrivate();

		switch (p1.getId()) {
			case R.id.activity_starterButtonMain:
               
                if (SkStatus.isTunnelActive()) {
                
                    
                    new SweetAlertDialog(SocksHttpMainActivity.this, SweetAlertDialog.WARNING_TYPE)
                        .setTitleText("Disconnect VPN")
                        .setContentText("Are you sure you want to disconnect?")
                        .setConfirmText("Disconnect")
                        .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {

                            @Override
                            public void onClick(SweetAlertDialog sweetAlertDialog)
                            {
                                // TODO: Implement this method
                                TunnelManagerHelper.stopSocksHttp(SocksHttpMainActivity.this);

                                sweetAlertDialog.dismiss();

                            }})
                        .setCancelText("Cancel")
                        .showCancelButton(true)
                        .setCancelClickListener(new SweetAlertDialog.OnSweetClickListener(){

                            @Override
                            public void onClick(SweetAlertDialog sweetAlertDialog)
                            {
                                sweetAlertDialog.dismiss();
                            }


                        })

                        .show();
                    
                    
                }
                else {
                
                showTweaks();
                
                }

				break;

			case R.id.activity_mainInputProxyLayout:
				if (!prefs.getBoolean(Settings.CONFIG_PROTEGER_KEY, false)) {
					doSaveData();

					DialogFragment fragProxy = new ProxyRemoteDialogFragment();
					fragProxy.show(getSupportFragmentManager(), "proxyDialog");
				}
				break;

			case R.id.activity_mainAutorText:
				String url = "http://t.me/SlipkProjects";
				Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(Intent.createChooser(intent, getText(R.string.open_with)));
				break;
				
			case R.id.activity_mainInputShowPassImageButton:
				isMostrarSenha = !isMostrarSenha;
				if (isMostrarSenha) {
					inputPwPass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
					inputPwShowPass.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_visibility_black_24dp));
				}
				else {
					inputPwPass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
					inputPwShowPass.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_visibility_off_black_24dp));
				}
			break;
		}
	}

	@Override
	public void onCheckedChanged(RadioGroup p1, int p2)
	{
		SharedPreferences.Editor edit = mConfig.getPrefsPrivate().edit();

		switch (p1.getCheckedRadioButtonId()) {
			case R.id.activity_mainSSHDirectRadioButton:
				edit.putInt(Settings.TUNNELTYPE_KEY, Settings.bTUNNEL_TYPE_SSH_DIRECT);
				proxyInputLayout.setVisibility(View.GONE);
				break;

			case R.id.activity_mainSSHProxyRadioButton:
				edit.putInt(Settings.TUNNELTYPE_KEY, Settings.bTUNNEL_TYPE_SSH_PROXY);
				proxyInputLayout.setVisibility(View.VISIBLE);
				break;
		}

		edit.apply();

		//doSaveData();
		doUpdateLayout();
	}

	 /* @Override
	public void onCheckedChanged(CompoundButton p1, boolean p2)
	{
		SharedPreferences prefs = mConfig.getPrefsPrivate();
		SharedPreferences.Editor edit = prefs.edit();

		switch (p1.getId()) {
			case R.id.activity_mainCustomPayloadSwitch:
				edit.putBoolean(Settings.PROXY_USAR_DEFAULT_PAYLOAD, !p2);
				setPayloadSwitch(prefs.getInt(Settings.TUNNELTYPE_KEY, Settings.bTUNNEL_TYPE_SSH_DIRECT), p2);
				break;
		}

		edit.apply();

		doSaveData();
	}
	*/ 
	
	protected void showBoasVindas() {
		new SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
			.setTitleText("Attention")
			.setContentText("This application was developed by Romzkie Freenet PH, and for advanced users only. If you don't know how to use this app, contact the admin.")
			.setConfirmText("OK")
			.show();
	}
	
	
	@Override
    public void updateState(final String state, String msg, int localizedResId, final ConnectionStatus level, Intent intent)
    {
        mHandler.post(new Runnable() {
                @Override
                public void run() {
                    doUpdateLayout();
                    if (SkStatus.isTunnelActive()){

                        if (level.equals(ConnectionStatus.LEVEL_CONNECTED)){
                           
							methodSpinner.setEnabled(false);
							customSetUP.setEnabled(false);
							serverSpinner.setEnabled(false);
							payloadSpinner.setEnabled(false);
							sslEdit.setEnabled(false);
							payloadEdit.setEnabled(false);
							sportSetup.setEnabled(false);
							portAuto.setEnabled(false);
							
				            showInterstitial();
                            
                            timer_layout.setVisibility(View.VISIBLE);
                           
								
                            }
                        }
                   }
            });
		
		switch (state) {
			case SkStatus.SSH_CONECTADO:
				// carrega ads banner
				if (adsBannerView != null && TunnelUtils.isNetworkOnline(SocksHttpMainActivity.this)) {
					adsBannerView.setAdListener(new AdListener() {
						@Override
						public void onAdLoaded() {
							if (adsBannerView != null && !isFinishing()) {
								adsBannerView.setVisibility(View.VISIBLE);
							}
						}
					});
				
				}
			
			break;
		}
	}


	/**
	 * Recebe locais Broadcast
	 */

	private BroadcastReceiver mActivityReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null)
                return;

            if (action.equals(UPDATE_VIEWS) && !isFinishing()) {
				doUpdateLayout();
			}else if (action.equals(OPEN_LOGS)) {
				vp.setCurrentItem(1, true);
			}
			
        }
    };


	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (mDrawerPanel.getToogle() != null && mDrawerPanel.getToogle().onOptionsItemSelected(item)) {
            return true;
        }

		// Menu Itens
		switch (item.getItemId()) {
			
			case R.id.miSettings:
				Intent intentSettings = new Intent(this, ConfigGeralActivity.class);
				//intentSettings.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(intentSettings);
				break;

				// logs opções
			
			case R.id.miExit:
				if (Build.VERSION.SDK_INT >= 16) {
					finishAffinity();
				}
				
				System.exit(0);
			break;
		}

		return super.onOptionsItemSelected(item);
	}

	private void PayloadGenerator() {

		PayloadGenerator gen = new PayloadGenerator(this);
		gen.setDismissListener(this);
		dialog = new AlertDialog.Builder(this).create();
		dialog.setView(gen);
		dialog.show();

	}

	@Override
	public void onDismiss(String payload)
	{
		payloadEdit.setText(payload);
		Toast.makeText(this,"Payload successfully generated!",Toast.LENGTH_SHORT).show();
		dialog.dismiss();
		// TODO: Implement this method
	}
/**
	@Override
	public void onBackPressed() {
		
		if (mDrawerPanel.getDrawerLayout().isDrawerOpen(GravityCompat.START)) {
            mDrawerPanel.getDrawerLayout().closeDrawers();
        }
		else {
			// mostra opção para sair
			showExitDialog();
		}
	}**/
	


	@Override
	public void onBackPressed()
	{
		new SweetAlertDialog(SocksHttpMainActivity.this, SweetAlertDialog.WARNING_TYPE)
            .setTitleText(getString(R.string.attention))
            .setContentText(getString(R.string.alert_exit))
            .setConfirmText(getString(R.string.exit))
            .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {

                @Override
                public void onClick(SweetAlertDialog sweetAlertDialog)
                {
                    // TODO: Implement this method

                    Utils.exitAll(SocksHttpMainActivity.this);

                }})
            .setCancelText(getString(R.string.minimize))
			.showCancelButton(true)
            .setCancelClickListener(new SweetAlertDialog.OnSweetClickListener(){

                @Override
                public void onClick(SweetAlertDialog sweetAlertDialog)
                {
                    // TODO: Implement this method
                    Intent startMain = new Intent(Intent.ACTION_MAIN);
                    startMain.addCategory(Intent.CATEGORY_HOME);
                    startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(startMain);
                }


            })

            .show();
	}
	
	private void checkNetwork() {
        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        NetworkInfo mMobile = connManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

        if (mWifi.isConnected())
        {
	        toolbar_main.setSubtitle("WIFI: "+TunnelUtils.getLocalIpAddress());
			toolbar_main.setSubtitleTextAppearance(this, R.style.Toolbar_SubTitleText);

        } else if (mMobile.isConnected()) {
            
			toolbar_main.setSubtitle("MOBILE: "+TunnelUtils.getLocalIpAddress());
			toolbar_main.setSubtitleTextAppearance(this, R.style.Toolbar_SubTitleText);
			
        } else {
			toolbar_main.setSubtitle("NO INTERNET CONNECTION");
			toolbar_main.setSubtitleTextAppearance(this, R.style.Toolbar_SubTitleText);
        }
	}
	
	private void updateHeaderCallback() {
		DataTransferStats dataTransferStats = StatisticGraphData.getStatisticData().getDataTransferStats();
		bytes_in.setText(dataTransferStats.byteCountToDisplaySize(dataTransferStats.getTotalBytesReceived(), false));
		bytes_out.setText(dataTransferStats.byteCountToDisplaySize(dataTransferStats.getTotalBytesSent(), false));
	}

	@Override
    public void onResume() {
        super.onResume();
		
		setSpinner();
		
	    showInterstitial();
        
		if (!mTimerEnabled){
			resumeTime(); // resume time
		}
		
		SharedPreferences prefs = mConfig.getPrefsPrivate();
		
		if (prefs.getInt("CustomSetup", 0) == 1){ // Custom Setup
		
		   customSetUP.setChecked(true);

			if (prefs.getInt("TunneType", 0) == 1){ // SSH

				setupSSH();

			}else if (prefs.getInt("TunneType", 0) == 2){ // SSL

				setupSSL();

			}

		}else{
			payloadLayout.setVisibility(View.GONE);
			ssl_layout.setVisibility(View.GONE);
		}
	    
		
		new Timer().schedule(new TimerTask()
			{
				@Override
				public void run()
				{
					runOnUiThread(new Runnable()
						{
							@Override
							public void run()
							{
								updateHeaderCallback();
								checkNetwork();
								// TODO: Implement this method
							}
						});
					// TODO: Implement this method
				}
			}, 0,1000);
		
		
		SkStatus.addStateListener(this);
		
		if (adsBannerView != null) {
			adsBannerView.resume();
		}
    }

	@Override
	protected void onPause()
	{
		super.onPause();
		
		doSaveData();
		
		SkStatus.removeStateListener(this);
		
		if (adsBannerView != null) {
			adsBannerView.pause();
		}
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();

		LocalBroadcastManager.getInstance(this)
			.unregisterReceiver(mActivityReceiver);
			
		if (adsBannerView != null) {
			adsBannerView.destroy();
		}
	}


	/**
	 * DrawerLayout Listener
	 */

	/**
	 * Utils
	 */

	public static void updateMainViews(Context context) {
		Intent updateView = new Intent(UPDATE_VIEWS);
		LocalBroadcastManager.getInstance(context)
			.sendBroadcast(updateView);
	}
	
	public void showExitDialog() {
		AlertDialog dialog = new AlertDialog.Builder(this).
			create();
		dialog.setTitle(getString(R.string.attention));
		dialog.setMessage(getString(R.string.alert_exit));

		dialog.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.
				string.exit),
			new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					Utils.exitAll(SocksHttpMainActivity.this);
				}
			}
		);

		dialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.
				string.minimize),
			new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// minimiza app
					Intent startMain = new Intent(Intent.ACTION_MAIN);
					startMain.addCategory(Intent.CATEGORY_HOME);
					startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					startActivity(startMain);
				}
			}
		);

		dialog.show();
	}
}

