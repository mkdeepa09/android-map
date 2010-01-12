package org.traveler.googleclientlogin;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.traveler.xmpp.XMPPService;
import org.traveler.xmpp.XMPPServiceFactory;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.json.JSONException;
import org.json.JSONObject;

import com.nevilon.bigplanet.BigPlanet;
import com.nevilon.bigplanet.R;
import com.nevilon.bigplanet.core.Place;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CompoundButton.OnCheckedChangeListener;

/**
 * @author tytung
 * @version 1.0
 * @date 2010/01/09
 */
public class GoogleAccountActivity extends Activity {
	
	private static final String TAG = GoogleAccountActivity.class.getSimpleName();
	// Settings
	private static String email = "";
	private static String password = "";
	public static boolean isXMPPConnected = false;
	
	// ClientLogin => http://code.google.com/apis/accounts/AuthForInstalledApps.html
	private static final String ClientLoginURL = "https://www.google.com/accounts/ClientLogin";
	private static final String GoogleAppEngineURL = "https://android-map.appspot.com";
	private static final String GoogleAppEngineLoginURL = GoogleAppEngineURL+"/_ah/login?auth=";
	private static final String JoinedGroupsServlet = "/web/JoinedGroups";
	private static final String GroupLeaderServlet = "/web/GroupLeader?groupname=";
	private static final String XMPPReceiver = "android-map@appspot.com";
	private static final int AuthToken = 0;
	private static final int AuthCookie = 1;
	private static final int AuthWebAccess = 2;
	private static final int AuthUnknownHostException = 3;
	private static final int XMPP_ErrorMessage = 4;
	private static final int XMPP_Notification = 5;
	private static final int XMPP_Packet = 6;
	
	// Reused variables
	private static String GoogleToken;
	private static CookieStore GoogleCookieStore;
	private static String RequestedServlet;
	public static String Groupname;
	public static boolean isLeader;
	private String LeaderEmail;
	private DefaultHttpClient httpclient = null;
	public static XMPPService xmppService;
	private static boolean isWaiting;
	private static boolean isRunning;
	
	private TextView mTextView_Note;
	private TextView mEditText_Email;
	private TextView mEditText_Pass;
	private Button mButton;
	private LinearLayout mGroupnameView;
	private Spinner mSpinner_Groupname;
	private CheckBox mCheckBox_SentReceiveGPS;
	private TextView mTextView_Message;
	private WebView mWebView;
	private ProgressDialog progressDialog;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.google_account);
		
		// Get setting from SharedPreferences
		email = GooglePreferences.getEmail();
		password = GooglePreferences.getPasswd();
		isXMPPConnected = GooglePreferences.getIsXMPPConnected();

		mTextView_Note = (TextView) findViewById(R.id.mTextView_Note);
		mTextView_Note.setText(R.string.share_location_note);
		
		mEditText_Email = (EditText) findViewById(R.id.mEditText_Email);
		mEditText_Email.setText(email);
		mEditText_Pass = (EditText) findViewById(R.id.mEditText_Pass);
		mEditText_Pass.setText(password);
		mButton = (Button) findViewById(R.id.mButton);
		mButton.setOnClickListener(mButtonOnClickListener);

		mGroupnameView = (LinearLayout) findViewById(R.id.mGroupnameView);
		mSpinner_Groupname = (Spinner) findViewById(R.id.mSpinner_Groupname);
		mSpinner_Groupname.setOnItemSelectedListener(mSpinnerListener);

		mCheckBox_SentReceiveGPS = (CheckBox) findViewById(R.id.mCheckBox_SentReceiveGPS);
		mCheckBox_SentReceiveGPS.setOnCheckedChangeListener(mCheckedChangeListener);

		mTextView_Message = (TextView) findViewById(R.id.mTextView_Message);

		mWebView = (WebView) findViewById(R.id.mWebView);

		// Get a singleton instance of DefaultHttpClient
		if (httpclient == null) {
			Log.i(TAG, "new DefaultHttpClient()");
			httpclient = new DefaultHttpClient();
			HttpParams httpParams = httpclient.getParams();
			HttpConnectionParams.setConnectionTimeout(httpParams, 7000);
			HttpConnectionParams.setSoTimeout(httpParams, 7000);
		}
		
		if (!email.equals("") && !password.equals("")) {
			mButton.performClick();
			mCheckBox_SentReceiveGPS.setChecked(isXMPPConnected);
		} else {
			hideLayouts();
		}
	}
	
	@Override
	public void onPause() {
		super.onPause();
		if (this.progressDialog != null && this.progressDialog.isShowing()) {
			this.progressDialog.dismiss();
		}
		isRunning = false;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (httpclient != null) {
			httpclient.getConnectionManager().shutdown();
			httpclient = null;
			Log.i(TAG, "destroy HttpClient");
		}
		if (mWebView != null) {
			mWebView.destroy();
			mWebView = null;
			Log.i(TAG, "destroy WebView");
		}
	}
	
	private void hideLayouts() {
		mGroupnameView.setVisibility(View.GONE);
		mSpinner_Groupname.setAdapter(null);
		mCheckBox_SentReceiveGPS.setChecked(false);
		mCheckBox_SentReceiveGPS.setEnabled(false);
		mWebView.setVisibility(View.GONE);
		isRunning = false;
		progressDialog.dismiss();
	}
		
	private OnClickListener mButtonOnClickListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			mTextView_Message.setText("");
			String newEmail = mEditText_Email.getText().toString().trim();
			String newPass = mEditText_Pass.getText().toString();
			if (!newEmail.equals(email)) {
				// Change the email account
				GoogleToken = null;
			}
			if (!newEmail.equals("") && !newPass.equals("")) {
				email = newEmail;
				password = newPass;
				GooglePreferences.putEmail(email);
				GooglePreferences.putPasswd(password);
				Log.i(TAG, "save GooglePreferences: Email, Passwd");
				if (GoogleToken != null) {
					// Use the token to get cookies
					getAuthCookie(GoogleAppEngineLoginURL + GoogleToken);
				} else {
					// Use the email and password to get the token
					getAuthToken(ClientLoginURL);
				}
			} else {
				mTextView_Message.setText(R.string.msg_account_password_empty);
			}
		}
	};
	
	private OnItemSelectedListener mSpinnerListener = new OnItemSelectedListener() {
		@Override
		public void onItemSelected(AdapterView<?> arg0, View view, int index, long arg3) {
			Groupname = (String) mSpinner_Groupname.getItemAtPosition(index);
			getGAEResource(GoogleAppEngineURL + GroupLeaderServlet + Groupname);
			mCheckBox_SentReceiveGPS.setEnabled(true);
		}
		@Override
		public void onNothingSelected(AdapterView<?> arg0) {
			Groupname = null;
		}
	};
	
	private OnCheckedChangeListener mCheckedChangeListener = new OnCheckedChangeListener() {
		@Override
		public void onCheckedChanged(CompoundButton view, boolean isChecked) {
			if (isChecked) {
				if (Groupname == null) {
					mTextView_Message.setText(R.string.msg_group_empty);
					mCheckBox_SentReceiveGPS.setChecked(false);
				} else {
					email = mEditText_Email.getText().toString().trim();
					password = mEditText_Pass.getText().toString();
					if (!email.equals("") && !password.equals("")) {
						// create an XMPP connection
						connectToXMPPServer();
					} else {
						mTextView_Message.setText(R.string.msg_account_password_empty);
					}
				}
			} else {
				// disconnect an XMPP connection
				if (xmppService != null) {
					xmppService.disconnect();
				}
				mTextView_Message.setText("");
			}
		}
	};
	
	private void getAuthToken(final String url) {
		showProgressDialog();
		// do the HTTP dance in a separate thread
		new Thread() {
			@Override
			public void run() {
				performHttpPost(AuthToken, url);
			}
		}.start();
	}
	
	private void getAuthCookie(final String url) {
		showProgressDialog();
		// do the HTTP dance in a separate thread
		new Thread() {
			@Override
			public void run() {
				performHttpPost(AuthCookie, url);
			}
		}.start();
	}
	
	private void getGAEResource(final String url) {
		showProgressDialog(getString(R.string.msg_get_group_info), 
				getString(R.string.msg_connecting));
		
		String newUrl = url.replace(GoogleAppEngineURL, "");
		int index = newUrl.lastIndexOf("=");
		if (index == -1) {
			RequestedServlet = newUrl;
		} else {
			RequestedServlet = newUrl.substring(0, index+1); // remove the last parameter
		}
		
		// do the HTTP dance in a separate thread
		new Thread() {
			@Override
			public void run() {
				performHttpPost(AuthWebAccess, url);
			}
		}.start();
	}
	
	private void performHttpPost(int what, String url) {
		String errorMessage = null;
		try {
			HttpPost httppost = new HttpPost(url);
			if (what == AuthToken) {
				List<NameValuePair> nvps = new ArrayList<NameValuePair>();
				nvps.add(new BasicNameValuePair("Email", email));
				nvps.add(new BasicNameValuePair("Passwd", password));
				nvps.add(new BasicNameValuePair("service", "ah")); // Google App Engine
				nvps.add(new BasicNameValuePair("source", "Android"));
				nvps.add(new BasicNameValuePair("accountType", "HOSTED_OR_GOOGLE"));
				httppost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
			}
			Log.i(TAG, httppost.getRequestLine().toString());
			
			HttpResponse response = httpclient.execute(httppost);
			StatusLine statusLine = response.getStatusLine();
			Log.i(TAG, statusLine.toString());
			// 0: HTTP/1.1 200 OK
			// 1: HTTP/1.1 204 No Content
			// 2: HTTP/1.1 200 OK
			
			HttpEntity entity = response.getEntity();
			String content = "";
			if (entity != null) {
				content = EntityUtils.toString(entity);
				Log.i(TAG, content);
			}
			
			CookieStore cookieStore = null;
			if (what == AuthCookie) {
				Log.i(TAG, "Cookies:");
				cookieStore = httpclient.getCookieStore();
				List<Cookie> cookies = cookieStore.getCookies();
				if (cookies.isEmpty()) {
					Log.i(TAG, "None");
				} else {
					for (int i=0; i < cookies.size(); i++) {
						Log.i(TAG, cookies.get(i).toString());
					}
				}
			}
			
			httppost.abort();
			Message msg = authHandler.obtainMessage();
			msg.what = what;
			if (what == AuthCookie) {
				msg.obj = cookieStore;
			} else {
				msg.obj = content;
			}
			authHandler.sendMessage(msg);
			
		} catch (UnknownHostException e) {
			errorMessage = e.getMessage();
			e.printStackTrace();
		} catch (SocketTimeoutException e) {
			errorMessage = e.getMessage();
			e.printStackTrace();
		} catch (ClientProtocolException e) {
			errorMessage = e.getMessage();
			e.printStackTrace();
		} catch (IOException e) {
			errorMessage = e.getMessage();
			e.printStackTrace();
		} finally {
			if (errorMessage != null) {
				Message msg = authHandler.obtainMessage();
				msg.what = AuthUnknownHostException;
				msg.obj = errorMessage;
				authHandler.sendMessage(msg);
			}
			//httpclient.getConnectionManager().shutdown();
		}
	}
	
	// use a Handler to update the UI (send the Handler messages from other threads)
	private final Handler authHandler = new Handler() {
		@Override
		public void handleMessage(final Message msg) {
			
			if (msg.what == AuthToken) {
//				progressDialog.dismiss();
				String authToken = (String) msg.obj;
				processAuthToken(authToken);
				
			} else if (msg.what == AuthCookie) {
				progressDialog.dismiss();
				CookieStore cookieStore = (CookieStore) msg.obj;
				processAuthCookie(cookieStore);
				
			} else if (msg.what == AuthWebAccess) {
				String json = (String) msg.obj;
				if (RequestedServlet.equals(JoinedGroupsServlet)) {
//					progressDialog.dismiss();
					processJoinedGroupsServlet(json);
				} else if (RequestedServlet.equals(GroupLeaderServlet)) {
					progressDialog.dismiss();
					processGroupLeaderServlet(json);
				} else {
					progressDialog.dismiss();
				}
				
			} else if (msg.what == AuthUnknownHostException) {
				//progressDialog.dismiss();
				hideLayouts();
				String message = (String) msg.obj;
				mTextView_Message.setText(message);
			}
		}
	};
	
	private void processAuthToken(String authToken) {
		int index = authToken.indexOf("Auth=");
		if (index > -1) {
			// Authenticate success
			authToken = authToken.substring(index, authToken.length()).trim();
			String token = authToken.replace("Auth=", "");
			Log.i(TAG, "token.length() = " + (token.length()));
			// Reuse token next time
			GoogleToken = token;
			// Use the token to get cookies
			getAuthCookie(GoogleAppEngineLoginURL + token);
		} else {
			// Authenticate failure
			//progressDialog.dismiss();
			hideLayouts();
//			int errorIndex = authToken.indexOf("Error=");
			//TODO: process different error messages to i18n texts
			if (authToken.startsWith("Error=BadAuthentication")) {
				mTextView_Message.setText(R.string.msg_account_password_error);
			} else {
				mTextView_Message.setText(authToken);
			}
		}
	}
	
	private void processAuthCookie(CookieStore cookieStore) {
		// Reuse cookies next time
		GoogleCookieStore = cookieStore;
		// Use the cookies to access the authenticated resource via HttpClient
		getGAEResource(GoogleAppEngineURL + JoinedGroupsServlet);
		showWebView("/");
	}
	
	private void showWebView(String url) {
		if (GoogleCookieStore != null) {
			List<Cookie> cookies = GoogleCookieStore.getCookies();
			if (!cookies.isEmpty()) {
				CookieManager cookieManager = CookieManager.getInstance();
				StringBuffer sb = null;
				for (int i=0; i < cookies.size(); i++) {
					Cookie cookie = cookies.get(i);
					sb = new StringBuffer();
					sb.append(cookie.getName()+"="+cookie.getValue()+";\n");
					sb.append("domain="+cookie.getDomain()+";\n");
					sb.append("path="+cookie.getPath()+";\n");
					sb.append("expiry="+cookie.getExpiryDate().toGMTString()+";\n");
					Log.i(TAG, sb.toString());
					cookieManager.setCookie(GoogleAppEngineURL, sb.toString());
				}
			}
			mWebView.setNetworkAvailable(true);
			mWebView.getSettings().setJavaScriptEnabled(true);
			mWebView.getSettings().setBuiltInZoomControls(true);
			mWebView.setWebViewClient(new WebViewClient());
			mWebView.setVisibility(View.VISIBLE);
			mWebView.loadUrl(GoogleAppEngineURL + url);
		}
	}
	
	private void processJoinedGroupsServlet(String json) {
		String[] array = null;
		try {
			JSONObject jObject = new JSONObject(json);
			String joinedGroups = jObject.getString("JoinedGroups");
			if (joinedGroups.length() > 0) {
				array = joinedGroups.split(",");
				ArrayAdapter<String> adapter = new ArrayAdapter<String>(GoogleAccountActivity.this,
						android.R.layout.simple_spinner_item, array);
				mGroupnameView.setVisibility(View.VISIBLE);
				mSpinner_Groupname.setAdapter(adapter);
			} else {
				mSpinner_Groupname.setAdapter(null);
				progressDialog.dismiss();
			}
		} catch (JSONException e) {
			//progressDialog.dismiss();
			hideLayouts();
			e.printStackTrace();
		}
	}
	
	private void processGroupLeaderServlet(String json) {
		try {
			JSONObject jObject = new JSONObject(json);
			LeaderEmail = jObject.getString("Leader");
			if (LeaderEmail.equals(email)) {
				isLeader = true;
			} else {
				isLeader = false;
			}
			Log.i(TAG, "isLeader = " + isLeader);
			String msg = getString(R.string.msg_leader);
			mTextView_Message.setText(msg + " " + LeaderEmail);
		} catch (JSONException e) {
			//progressDialog.dismiss();
			hideLayouts();
			e.printStackTrace();
		}
	}

	private void connectToXMPPServer() {
		isRunning = false;
		showProgressDialog();
		// do the HTTP dance in a separate thread
		new Thread("XMPPServerConnector") {
			@Override
			public void run() {
				isRunning = true;
				while (isRunning) {
					isWaiting = true;
					while (isWaiting) {
						if (LeaderEmail != null) {
							isWaiting = false;
							isRunning = false;
						}
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					// Get a singleton instance of XMPPService
					if (xmppService == null) {
						xmppService = XMPPServiceFactory.getXMPPService();
						Log.i(TAG, "XMPPServiceFactory.getXMPPService()");
					}
					XMPPConnection connection = xmppService.getConnection();
					if (!connection.isAuthenticated()) {
						xmppService.connect();
						if (connection.isConnected()) {
							xmppService.login(email, password);
							xmppService.addPacketListener(myPacketListener, XMPPReceiver);
							if (!connection.isAuthenticated()) {
								sendAndroidHandlerMessage(XMPP_ErrorMessage, getString(R.string.msg_account_password_error));
							} else {
								// XMPP connection has been created
								Log.i(TAG, "XMPP connection has been created.");
								progressDialog.dismiss();
								try {
									// create chat channel with XMPPReceiver(i.e. server agent)
									xmppService.sendMessage("Hi");
								} catch (XMPPException e) {
									e.printStackTrace();
								}
							}
						} else {
							sendAndroidHandlerMessage(XMPP_ErrorMessage, getString(R.string.msg_not_connect_to_server));
						}
					} else {
						Log.i(TAG, "connection.isAuthenticated()");
					}
					isXMPPConnected = connection.isAuthenticated();
					GooglePreferences.putIsXMPPConnected(isXMPPConnected);
					Log.i(TAG, "save GooglePreferences: isXMPPConnected="+isXMPPConnected);
					if (isXMPPConnected) {
						if (isLeader)
							sendAndroidHandlerMessage(XMPP_Notification, getString(R.string.msg_enable_sending));
						else
							sendAndroidHandlerMessage(XMPP_Notification, getString(R.string.msg_enable_receiving));
					}
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}.start();
	}
	
	private PacketListener myPacketListener = new PacketListener() {
		@Override
		public void processPacket(Packet packet) {
			Log.i(TAG, packet.toXML());
			sendAndroidHandlerMessage(XMPP_Packet, packet);
		}
	};
	
	private void sendAndroidHandlerMessage(int what, Object obj) {
		Message msg = xmppHandler.obtainMessage();
		msg.what = what;
		msg.obj = obj;
		xmppHandler.sendMessage(msg);
	}

	// use a handler to update the UI (send the handler messages from other threads)
	private final Handler xmppHandler = new Handler() {
		@Override
		public void handleMessage(final Message msg) {
			progressDialog.dismiss();
			
			if (msg.what == XMPP_ErrorMessage) {
				// error message
				String message = (String) msg.obj;
				mCheckBox_SentReceiveGPS.setChecked(false);
				mTextView_Message.setText(message);
				
			} else if (msg.what == XMPP_Notification)  {
				// notification message
				String message = (String) msg.obj;
				String text = mTextView_Message.getText().toString();
				if (text.length()>0)
					message = text + "\n" + message;
				mTextView_Message.setText(message);
				
			} else if (msg.what == XMPP_Packet)  {
				// packet
				Packet packet = (Packet) msg.obj;
				processPacket(packet);
			}
		}
	};
	
	private void processPacket(Packet packet) {
		String body = null;
		if (packet.getClass() == IQ.class) {
//			IQ iqPacket = (IQ) packet;
		} else if (packet.getClass() == org.jivesoftware.smack.packet.Message.class) {
			org.jivesoftware.smack.packet.Message xmppMessage =
				(org.jivesoftware.smack.packet.Message) packet;
			body = xmppMessage.getBody();
		}
		//TODO: see BigPlanet MyLocationListener.onLocationChanged() for more info
		if (body != null) {
			if (body.startsWith("gps:")) { //gps:121.54,25.018
				// only non-leaders receive this kind of message
				String gps = body.substring("gps:".length());
				String[] array = gps.split(",");
				try {
					Double lon = Double.parseDouble(array[0]);
					Double lat = Double.parseDouble(array[1]);
					Place place = new Place();
					place.setLon(lon);
					place.setLat(lat);
					List<Place> placeList = new ArrayList<Place>();
					placeList.add(place);
					BigPlanet.addMarkersForDrawing(placeList, 3);
				} catch (ArrayIndexOutOfBoundsException e) {
					e.printStackTrace();
				} catch (NumberFormatException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void showProgressDialog() {
		showProgressDialog(getString(R.string.msg_wait), 
				getString(R.string.msg_connecting));
	}
	
	private void showProgressDialog(String title, String message) {
		boolean isShow = false;
		if (progressDialog == null) {
			isShow = true;
		} else {
			if (!progressDialog.isShowing()) {
				isShow = true;
			}
		}
		if (isShow) {
			progressDialog = ProgressDialog.show(GoogleAccountActivity.this, title, message);
			progressDialog.setCancelable(true);
			progressDialog.setOnCancelListener(new OnCancelListener() {
				@Override
				public void onCancel(DialogInterface arg0) {
					mCheckBox_SentReceiveGPS.setChecked(false);
				}
			});
		}
	}
	
}