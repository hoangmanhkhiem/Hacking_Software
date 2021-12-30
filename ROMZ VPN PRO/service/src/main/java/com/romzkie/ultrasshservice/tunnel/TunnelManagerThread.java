package com.romzkie.ultrasshservice.tunnel;

import android.content.*;
import android.net.*;
import android.os.*;
import android.support.v4.content.*;
import android.util.*;
import android.widget.*;
import com.romzkie.ultrasshservice.*;
import com.romzkie.ultrasshservice.config.*;
import com.romzkie.ultrasshservice.logger.*;
import com.romzkie.ultrasshservice.tunnel.vpn.*;
import com.trilead.ssh2.*;
import com.trilead.ssh2.transport.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class TunnelManagerThread
implements Runnable, ConnectionMonitor, InteractiveCallback,
ServerHostKeyVerifier, DebugLogger {
	private static final String TAG = TunnelManagerThread.class.getSimpleName();

	private OnStopCliente mListener;
	private Context mContext;
	private Handler mHandler;
	private Settings mConfig;
	private boolean mRunning = false, mStopping = false, mStarting = false;
	private Pinger pinger;

	private CountDownLatch mTunnelThreadStopSignal;
	//private ConnectivityManager mCmgr;

	public interface OnStopCliente {
		void onStop();
	}

	public TunnelManagerThread(Handler handler, Context context) {
		mContext = context;
		mHandler = handler;

		mConfig = new Settings(context);
	}

	public void setOnStopClienteListener(OnStopCliente listener) {
		mListener = listener;
	}

	@Override
	public void run() {
		mStarting = true;
		mTunnelThreadStopSignal = new CountDownLatch(1);

		SkStatus.logInfo("<strong>" + mContext.getString(R.string.starting_service_ssh) + "</strong>");

		int tries = 0;
		while (!mStopping) {
			try {
				if (!TunnelUtils.isNetworkOnline(mContext)) {
					SkStatus.updateStateString(SkStatus.SSH_AGUARDANDO_REDE, mContext.getString(R.string.state_nonetwork));

					SkStatus.logInfo(R.string.state_nonetwork);

					try {
						Thread.sleep(5000);
					} catch (InterruptedException e2) {
						stopAll();
						break;
					}
				} else {
					if (tries > 0)
						SkStatus.logInfo("<strong>" + mContext.getString(R.string.state_reconnecting) + "</strong>");

					try {
						Thread.sleep(500);
					} catch (InterruptedException e2) {
						stopAll();
						break;
					}

					startClienteSSH();
					break;
				}
			} catch (Exception e) {

				SkStatus.logError("<strong><font color='red'>" + mContext.getString(R.string.state_disconnected) + "</font></strong>");
				closeSSH();

				try {
					Thread.sleep(500);
				} catch (InterruptedException e2) {
					stopAll();
					break;
				}
			}

			tries++;
		}

		mStarting = false;

		if (!mStopping) {
			try {
				mTunnelThreadStopSignal.await();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}

		if (mListener != null) {
			mListener.onStop();
		}
	}

	public void stopAll() {
		if (mStopping) return;

		SkStatus.updateStateString(SkStatus.SSH_PARANDO, mContext.getString(R.string.stopping_service_ssh));
		SkStatus.logInfo("<strong>" + mContext.getString(R.string.stopping_service_ssh) + "</strong>");

		new Thread(new Runnable() {
				@Override
				public void run() {
					mStopping = true;
					if (mTunnelThreadStopSignal != null)
						mTunnelThreadStopSignal.countDown();

					closeSSH();

					mRunning = false;
					mStarting = false;
					mReconnecting = false;
				}
			}).start();

		Handler handler = new Handler(); 
		handler.postDelayed(new Runnable() {
				@Override 
				public void run() { 
					SkStatus.updateStateString(SkStatus.SSH_DESCONECTADO, mContext.getString(R.string.state_disconnected));
                    
                    if (mConfig.network_meter()){
                        mContext.stopService(new Intent(mContext, NetSpeedNotification.class));

                    }
                    
				} 
			}, 1000);
	}

	/**
	 * Forwarder
	 */

	protected void startForwarder(int portaLocal) throws Exception {
		if (!mConnected) {
			throw new Exception();
		}

		startForwarderSocks(portaLocal);

		startTunnelVpnService();
        
        if (mConfig.network_meter()){
            mContext.startService(new Intent(mContext, NetSpeedNotification.class));

        }
		
		/* If connected, 1s delay before pinger started */
		
		String PING = mConfig.setPinger();	
		
		if (mConfig.setAutoPing()){
		
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {}
			
		   if (!PING.equals(""))
			{
			  pinger = new Pinger(mConnection, PING);
			  pinger.start();
			}
   
		 }
		
		/**new Thread(new Runnable() {
				@Override
				public void run() {
					while (true) {
						if (!mConnected) break;

						try {
							Thread.sleep(2000);
						} catch (InterruptedException e) {
							break;
						}
						
					    if (lastPingLatency > 0) {
							SkStatus.logInfo(String.format("Ping Latency: %d ms", lastPingLatency));
							break;
						}
					}
				}
			}).start();**/
			
	}
	
	private synchronized void interruptPinger() {
		if (pinger != null && pinger.isAlive()) {
			SkStatus.logInfo("stopping pinger");

		    pinger.interrupt();
		}
	}
	
	protected void stopForwarder() {
		stopTunnelVpnService();
		
		stopForwarderSocks();
	}


	/**
	 * Cliente SSH
	 */

	private final static int AUTH_TRIES = 1;
	private final static int RECONNECT_TRIES = 5;

	private Connection mConnection;

	private boolean mConnected = false;

	protected void startClienteSSH() throws Exception {
		mStopping = false;
		mRunning = true;

		String servidor = mConfig.getPrivString(Settings.SERVIDOR_KEY);
		int porta = Integer.parseInt(mConfig.getPrivString(Settings.SERVIDOR_PORTA_KEY));
		String usuario = mConfig.getPrivString(Settings.USUARIO_KEY);

		String _senha = mConfig.getPrivString(Settings.SENHA_KEY);
		String senha = _senha.isEmpty() ? PasswordCache.getAuthPassword(null, false) : _senha;

		String keyPath = mConfig.getSSHKeypath();
		int portaLocal = Integer.parseInt(mConfig.getPrivString(Settings.PORTA_LOCAL_KEY));

		try {

			conectar(servidor, porta);

			for (int i = 0; i < AUTH_TRIES; i++) {
				if (mStopping) {
					return;
				}

				try {
					autenticar(usuario, senha, keyPath);

					break;
				} catch (IOException e) {
					if (i + 1 >= AUTH_TRIES) {
						throw new IOException("Authentication failed");
					} else {
						try {
							Thread.sleep(3000);
						} catch (InterruptedException e2) {
							return;
						}
					}
				}
			}

			SkStatus.updateStateString(SkStatus.SSH_CONECTADO, "SSH Connection Established");
			SkStatus.logInfo("<strong><html><font color='#0062AF'>" + mContext.getString(R.string.state_connected) + "</font></html></strong>");

		 /**if (mConfig.getSSHPinger() > 0) {
			startPinger(mConfig.getSSHPinger());
			}**/
			 

			startForwarder(portaLocal);

		} catch (Exception e) {
			mConnected = false;

			throw e;
		}
	}

	public synchronized void closeSSH() {
		stopForwarder();
		//stopPinger();
		interruptPinger();

		if (mConnection != null) {
			SkStatus.logDebug("Stopping SSH");
			mConnection.close();
		}
	}

	protected void conectar(String servidor, int porta) throws Exception {
		if (!mStarting) {
			throw new Exception();
		}

		SharedPreferences prefs = mConfig.getPrefsPrivate();

		// aqui deve conectar
		try {

			mConnection = new Connection(servidor, porta);

			if (mConfig.getModoDebug() && !prefs.getBoolean(Settings.CONFIG_PROTEGER_KEY, false)) {
				// Desativado, pois estava enchendo o Logger
				//mConnection.enableDebugging(true, this);
				mHandler.post(new Runnable() {
						@Override
						public void run() {
							Toast.makeText(mContext, "Debug mode enabled",
										   Toast.LENGTH_SHORT).show();
						}
					});
			}

			// delay sleep
			if (mConfig.getIsDisabledDelaySSH()) {
				mConnection.setTCPNoDelay(true);
			}
			
			//Data Compression
			if (mConfig.ssh_compression()){
				mConnection.setCompression(true);
				SkStatus.logInfo("<strong>SSH Compression Enabled</strong>");
			}

			// proxy
			addProxy(prefs.getBoolean(Settings.CONFIG_PROTEGER_KEY, false), prefs.getInt(Settings.TUNNELTYPE_KEY, Settings.bTUNNEL_TYPE_SSH_DIRECT),
					 (!prefs.getBoolean(Settings.PROXY_USAR_DEFAULT_PAYLOAD, true) ? mConfig.getPrivString(Settings.CUSTOM_PAYLOAD_KEY) : null),
					 mConnection);

			// monitora a conexão
			mConnection.addConnectionMonitor(this);

			if (Build.VERSION.SDK_INT >= 23) {
				ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
				ProxyInfo proxy = cm.getDefaultProxy();
				if (proxy != null) {
					//SkStatus.logInfo("<strong>Welcome to the world of freenet</strong>");
				}
			}

			SkStatus.updateStateString(SkStatus.SSH_CONECTANDO, mContext.getString(R.string.state_connecting));
			SkStatus.logInfo(R.string.state_connecting);

			mConnection.connect(this, 10 * 1000, 20 * 1000);

			mConnected = true;

		} catch (Exception e) {

			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));

			String cause = e.getCause().toString();
			if (useProxy && cause.contains("Key exchange was not finished")) {
				SkStatus.logError("Proxy: Connection Lost");
			} else {
				SkStatus.logError("SSH: " + cause);
			}

			throw new Exception(e);
		}
	}


	/**
	 * Autenticação
	 */

	private static final String AUTH_PUBLICKEY = "publickey",
	AUTH_PASSWORD = "password", AUTH_KEYBOARDINTERACTIVE = "keyboard-interactive";

	protected void autenticar(String usuario, String senha, String keyPath) throws IOException {
		if (!mConnected) {
			throw new IOException();
		}

		SkStatus.updateStateString(SkStatus.SSH_AUTENTICANDO, mContext.getString(R.string.state_auth));

		try {
			if (mConnection.isAuthMethodAvailable(usuario,
												  AUTH_PASSWORD)) {

				SkStatus.logInfo("Authenticating with password");

				if (mConnection.authenticateWithPassword(usuario,
														 senha)) {
					SkStatus.logInfo("<strong>" + mContext.getString(R.string.state_auth_success) + "</strong>");
				}
			}
		} catch (IllegalStateException e) {
			Log.e(TAG,
				  "Connection went away while we were trying to authenticate",
				  e);
		} catch (Exception e) {
			Log.e(TAG, "Problem during handleAuthentication()", e);
		}

		try {
			if (mConnection.isAuthMethodAvailable(usuario,
												  AUTH_PUBLICKEY) && keyPath != null && !keyPath.isEmpty()) {
				File f = new File(keyPath);
				if (f.exists()) {
					if (senha.equals("")) senha = null;

					SkStatus.logInfo("Authenticating com public key");

					if (mConnection.authenticateWithPublicKey(usuario, f,
															  senha)) {
						SkStatus.logInfo("<strong>" + mContext.getString(R.string.state_auth_success) + "</strong>");
					}
				}
			}
		} catch (Exception e) {
			Log.d(TAG, "Host does not support 'Public key' authentication.");
		}

		/*try {
		 if (mConnection.authenticateWithNone(mSettings.usuario)) {
		 Log.d(TAG, "Authenticate with none");
		 return true;
		 }
		 } catch (Exception e) {
		 Log.d(TAG, "Host does not support 'none' authentication.");
		 }

		 // TODO: Need verification

		 try {
		 if (mConnection.isAuthMethodAvailable(mSettings.usuario,
		 AUTH_KEYBOARDINTERACTIVE)) {
		 if (mConnection.authenticateWithKeyboardInteractive(
		 mSettings.usuario, this))
		 return true;
		 }
		 } catch (Exception e) {
		 Log.d(TAG,
		 "Host does not support 'Keyboard-Interactive' authentication.");
		 }*/

		if (!mConnection.isAuthenticationComplete()) {
			SkStatus.logInfo("Failed to authenticate, User or Password Expired");

			throw new IOException("It was not possible to authenticate with the data provided");
		}
	}

	// XXX: Is it right?
	@Override
	public String[] replyToChallenge(String name, String instruction,
									 int numPrompts, String[] prompt, boolean[] echo) throws Exception {
		String[] responses = new String[numPrompts];
		for (int i = 0; i < numPrompts; i++) {
			// request response from user for each prompt
			if (prompt[i].toLowerCase().contains("password"))
				responses[i] = mConfig.getPrivString(Settings.SENHA_KEY);
		}
		return responses;
	}


	/**
	 * ServerHostKeyVerifier
	 * Fingerprint
	 */

	@Override
	public boolean verifyServerHostKey(String hostname, int port,
									   String serverHostKeyAlgorithm, byte[] serverHostKey)
	throws Exception {

		String fingerPrint = KnownHosts.createHexFingerprint(
			serverHostKeyAlgorithm, serverHostKey);
		//int fingerPrintStatus = SSHConstants.FINGER_PRINT_CHANGED;

		SkStatus.logInfo("Finger Print: " + fingerPrint);

		//Log.d(TAG, "Finger Print Type: " + "");

		return true;
	}


	/**
	 * Proxy
	 */

	private boolean useProxy = false;

	protected void addProxy(boolean isProteger, int mTunnelType, String mCustomPayload, Connection conn) throws Exception {

		if (mTunnelType != 0) {
			useProxy = true;

			switch (mTunnelType) {
				case Settings.bTUNNEL_TYPE_SSH_DIRECT:
					if (mCustomPayload != null) {
						try {
							ProxyData proxyData = new HttpProxyCustom(mConfig.getPrivString(Settings.SERVIDOR_KEY), Integer.parseInt(mConfig.getPrivString(Settings.SERVIDOR_PORTA_KEY)),
																	  null, null, mCustomPayload, true, mContext);

							conn.setProxyData(proxyData);

							/**if (!mCustomPayload.isEmpty() && !isProteger)
								SkStatus.logInfo("Rebuild by: Romzkie#25");**/

						} catch (Exception e) {
							throw new Exception(mContext.getString(R.string.error_proxy_invalid));
						}
					} else {
						useProxy = false;
					}
					break;

				case Settings.bTUNNEL_TYPE_SSH_PROXY:
					String customPayload = mCustomPayload;

					if (customPayload != null && customPayload.isEmpty()) {
						customPayload = null;
					}

					String servidor = mConfig.getPrivString(Settings.PROXY_IP_KEY);
					int porta = Integer.parseInt(mConfig.getPrivString(Settings.PROXY_PORTA_KEY));

					try {
						ProxyData proxyData = new HttpProxyCustom(servidor, porta,
																  null, null, customPayload, false, mContext);

						/**if (!isProteger)
							SkStatus.logInfo(String.format("THANK YOU FOR SUPPORTING!"));**/
						conn.setProxyData(proxyData);

						/**if (customPayload != null && !customPayload.isEmpty() && !isProteger)
							SkStatus.logInfo("Rebuild by: Romzkie#25");**/

					} catch (Exception e) {
						SkStatus.logError(R.string.error_proxy_invalid);

						throw new Exception(mContext.getString(R.string.error_proxy_invalid));
					}
					break;
					
				case Settings.bTUNNEL_TYPE_SSH_SSL:
                    String customsni = mCustomPayload;

                    if (customsni != null && customsni.isEmpty()) {
                        customPayload = null;
                    }

                    String sshServer = mConfig.getPrivString(Settings.PROXY_IP_KEY);
                    int sshPort = Integer.parseInt(mConfig.getPrivString(Settings.PROXY_PORTA_KEY));

                    try{

                        SSLTunnelProxy sslTun = new SSLTunnelProxy(sshServer, sshPort, customsni);
                        conn.setProxyData(sslTun);

                    } catch(Exception e) {
                        SkStatus.logInfo(e.getMessage());
                    }

                    break;
					
					/*case Prefs.TUNNEL_TYPE_SSH_HTTP:
					 SkStatus.logInfo("Usando Tunnel HTTP");

					 String servidorHttp = "165.227.48.122";
					 int portaHttp = 80;

					 ProxyData pData = new HttpTunnelCliente(servidorHttp, portaHttp);

					 SkStatus.logInfo(String.format("Proxy: %s:%d", servidorHttp, portaHttp));
					 conn.setProxyData(pData);
					 break;*/

				default: useProxy = false;
			}
		}
	}


	/**
	 * Socks5 Forwarder
	 */

	private DynamicPortForwarder dpf;

	private synchronized void startForwarderSocks(int portaLocal) throws Exception {
		if (!mConnected) {
			throw new Exception();
		}

		//SkStatus.logInfo("starting socks local");
		//SkStatus.logDebug(String.format("socks local listen: %d", portaLocal));
		
		try {

			int nThreads = mConfig.getMaximoThreadsSocks();
			
			

			if (nThreads > 0) {
				dpf = mConnection.createDynamicPortForwarder(portaLocal, nThreads);

				SkStatus.logDebug("socks local number threads: " + Integer.toString(nThreads));
			}
			else {
				dpf = mConnection.createDynamicPortForwarder(portaLocal);
			}

		} catch (Exception e) {
			SkStatus.logError("Socks Local: " + e.getCause().toString());

			throw new Exception();
		}
	}
	

	private synchronized void stopForwarderSocks() {
		if (dpf != null) {
			try {
				dpf.close(); 
			} catch (IOException e) {}
			dpf = null;
		}
	}
	
	
	/**
	 * Pinger
	 */
/**
	private Thread thPing;
	private long lastPingLatency = -1;

	private void startPinger(final int timePing) throws Exception {
		if (!mConnected) {
			throw new Exception();
		}
		
		SkStatus.logInfo("starting pinger");

		thPing = new Thread() {
			@Override
			public void run() {
				while (mConnected) {
					try {
						makePinger();
					} catch(InterruptedException e) {
						break;
					}
				}
				SkStatus.logDebug("pinger stopped");
			}

			private synchronized void makePinger() throws InterruptedException {
				try {
					if (mConnection != null) {
						long ping = mConnection.ping();
						if (lastPingLatency < 0) {
							lastPingLatency = ping;
						}
					}
					else throw new InterruptedException();
				} catch(Exception e) {
					Log.e(TAG, "ping error", e);
				}

				if (timePing == 0)
					return;

				if (timePing > 0)
					sleep(timePing*1000);
				else {
					SkStatus.logError("ping invalid");
					throw new InterruptedException();
				}
			}
		};

		// inicia
		thPing.start();
	}
	

	private synchronized void stopPinger() {
		if (thPing != null && thPing.isAlive()) {
			SkStatus.logInfo("stopping pinger");

			thPing.interrupt();
			thPing = null;
		}
	}**/
	
	/**
	 * Connection Monitor
	 */

	@Override
	public void connectionLost(Throwable reason) {
		if (mStarting || mStopping || mReconnecting) {
			return;
		}

		SkStatus.logError("<strong>" + mContext.getString(R.string.log_conection_lost) + "</strong>");
        
        SkStatus.logError("SSH: " + (reason.getMessage().toString()));

		reconnectSSH();
	}

	public boolean mReconnecting = false;

	public void reconnectSSH() {
		if (mStarting || mStopping || mReconnecting) {
			return;
		}

		mReconnecting = true;

		closeSSH();

		SkStatus.updateStateString(SkStatus.SSH_RECONECTANDO, "Reconnecting..");

		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			mReconnecting = false;
			return;
		}
		
		SkStatus.clearLog();

		for (int i = 0; i < RECONNECT_TRIES; i++) {
			if (mStopping) {
				mReconnecting = false;
				return;
			}

			int sleepTime = 5;
			if (!TunnelUtils.isNetworkOnline(mContext)) {
				SkStatus.updateStateString(SkStatus.SSH_AGUARDANDO_REDE, "Waiting for network ..");

				SkStatus.logInfo(R.string.state_nonetwork);
			} else {
				sleepTime = 3;
				mStarting = true;
				SkStatus.updateStateString(SkStatus.SSH_RECONECTANDO, "Reconnecting..");

				SkStatus.logInfo("<strong>" + mContext.getString(R.string.state_reconnecting) + "</strong>");

				try {
					startClienteSSH();

					mStarting = false;
					mReconnecting = false;
					//mConnected = true;

					return;
				} catch (Exception e) {
					SkStatus.logInfo("<strong><html><font color=\"red\">" + mContext.getString(R.string.state_disconnected) + "</font></html></strong>");
				}

				mStarting = false;
			}

			try {
				Thread.sleep(sleepTime * 1000);
				i--;
			} catch (InterruptedException e2) {
				mReconnecting = false;
				return;
			}
		}

		mReconnecting = false;

		stopAll();
		
	}

	@Override
	public void onReceiveInfo(int id, String msg) {
		if (id == SERVER_BANNER) {
			SkStatus.logInfo("<strong>" + mContext.getString(R.string.log_server_banner) + "</strong> " + "<br><br><b>Server Rules And Regulations</b><br><br>\u274cNO SPAMMING!\u274c<br>\u274cNO TORRENT!\u274c<br>\u274cNO CARDING!\u274c<br>\u274cNO HACKING!\u274c<br>\u274cNO ILLEGAL ACTIVITIES!\u274c<br><br>SERVER OWNER: <b><font color=#0062AF>ROMEL P. BROSAS</font></b><br>");
		}
	}


	/**
	 * Debug Logger
	 */

	@Override
	public void log(int level, String className, String message) {
		SkStatus.logDebug(String.format("%s: %s", className, message));
	}


	/**
	 * Vpn Tunnel
	 */

	String serverAddr;

	protected void startTunnelVpnService() throws IOException {
		if (!mConnected) {
			throw new IOException();
		}

		SkStatus.logInfo("starting tunnel service");
		SharedPreferences prefs = mConfig.getPrefsPrivate();

		// Broadcast
		IntentFilter broadcastFilter =
			new IntentFilter(TunnelVpnService.TUNNEL_VPN_DISCONNECT_BROADCAST);
		broadcastFilter.addAction(TunnelVpnService.TUNNEL_VPN_START_BROADCAST);
		// Inicia Broadcast
		LocalBroadcastManager.getInstance(mContext)
			.registerReceiver(m_vpnTunnelBroadcastReceiver, broadcastFilter);

		String m_socksServerAddress = String.format("127.0.0.1:%s", mConfig.getPrivString(Settings.PORTA_LOCAL_KEY));
		boolean m_dnsForward = mConfig.getVpnDnsForward();
		String m_udpResolver = mConfig.getVpnUdpForward() ? mConfig.getVpnUdpResolver() : null;

		String servidorIP = mConfig.getPrivString(Settings.SERVIDOR_KEY);

		if (prefs.getInt(Settings.TUNNELTYPE_KEY, Settings.bTUNNEL_TYPE_SSH_DIRECT) == Settings.bTUNNEL_TYPE_SSH_PROXY) {
			try {
				servidorIP = mConfig.getPrivString(Settings.PROXY_IP_KEY);
			} catch (Exception e) {
				SkStatus.logError(R.string.error_proxy_invalid);

				throw new IOException(mContext.getString(R.string.error_proxy_invalid));
			}
		}

		try {
			InetAddress servidorAddr = TransportManager.createInetAddress(servidorIP);
			serverAddr = servidorIP = servidorAddr.getHostAddress();
		} catch (UnknownHostException e) {
			throw new IOException(mContext.getString(R.string.error_server_ip_invalid));
		}

		String[] m_excludeIps = {servidorIP};

		String[] m_dnsResolvers = null;
		if (m_dnsForward) {
			m_dnsResolvers = new String[]{mConfig.getVpnDnsResolver()};
		} else {
			List<String> lista = VpnUtils.getNetworkDnsServer(mContext);
			m_dnsResolvers = new String[]{lista.get(0)};
		}

		if (isServiceVpnRunning()) {
			Log.d(TAG, "already running service");

			TunnelVpnManager tunnelManager = TunnelState.getTunnelState()
				.getTunnelManager();

			if (tunnelManager != null) {
				tunnelManager.restartTunnel(m_socksServerAddress);
			}

			return;
		}

		Intent startTunnelVpn = new Intent(mContext, TunnelVpnService.class);
		startTunnelVpn.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

		TunnelVpnSettings settings = new TunnelVpnSettings(m_socksServerAddress, m_dnsForward, m_dnsResolvers,
														   (m_dnsForward && m_udpResolver == null || !m_dnsForward && m_udpResolver != null), m_udpResolver, m_excludeIps,
														   mConfig.getIsFilterApps(), mConfig.getIsFilterBypassMode(), mConfig.getFilterApps(), mConfig.getIsTetheringSubnet());
		startTunnelVpn.putExtra(TunnelVpnManager.VPN_SETTINGS, settings);

		if (mContext.startService(startTunnelVpn) == null) {
			SkStatus.logInfo("failed to start tunnel vpn service");

			throw new IOException("Vpn Service failed to start");
		}

		TunnelState.getTunnelState().setStartingTunnelManager();
	}

	public static boolean isServiceVpnRunning() {
		TunnelState tunnelState = TunnelState.getTunnelState();
		return tunnelState.getStartingTunnelManager() || tunnelState.getTunnelManager() != null;
	}

	protected synchronized void stopTunnelVpnService() {
		if (!isServiceVpnRunning()) {
			return;
		}

		// Use signalStopService to asynchronously stop the service.
		// 1. VpnService doesn't respond to stopService calls
		// 2. The UI will not block while waiting for stopService to return
		// This scheme assumes that the UI will monitor that the service is
		// running while the Activity is not bound to it. This is the state
		// while the tunnel is shutting down.
		SkStatus.logInfo("stopping tunnel service");

		TunnelVpnManager currentTunnelManager = TunnelState.getTunnelState()
			.getTunnelManager();

		if (currentTunnelManager != null) {
			currentTunnelManager.signalStopService();
		}

		/*if (mThreadLocation != null && mThreadLocation.isAlive()) {
		 mThreadLocation.interrupt();
		 }
		 mThreadLocation = null;*/

		// Parando Broadcast
		LocalBroadcastManager.getInstance(mContext)
			.unregisterReceiver(m_vpnTunnelBroadcastReceiver);
	}

	//private Thread mThreadLocation;

	// Local BroadcastReceiver
	private BroadcastReceiver m_vpnTunnelBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public synchronized void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();

			if (TunnelVpnService.TUNNEL_VPN_START_BROADCAST.equals(action)) {
				boolean startSuccess = intent.getBooleanExtra(TunnelVpnService.TUNNEL_VPN_START_SUCCESS_EXTRA, true);

				if (!startSuccess) {
					stopAll();
				}

			} else if (TunnelVpnService.TUNNEL_VPN_DISCONNECT_BROADCAST.equals(action)) {
				stopAll();
			}
		}
	};

}
