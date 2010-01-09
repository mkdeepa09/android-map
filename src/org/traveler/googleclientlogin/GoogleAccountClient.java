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
import org.json.JSONException;
import org.json.JSONObject;

import com.nevilon.bigplanet.R;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CompoundButton.OnCheckedChangeListener;

/**
 * @author tytung
 * @version 1.0
 * @date 2010/01/08
 */
public class GoogleAccountClient extends Activity {
	
	private static final String TAG = GoogleAccountClient.class.getSimpleName();
	private static final String email = ""; // for testing
	private static final String password = "";
	
	// ClientLogin => http://code.google.com/apis/accounts/AuthForInstalledApps.html
	private static final String ClientLoginURL = "https://www.google.com/accounts/ClientLogin";
	private static final String GoogleAppEngineURL = "https://android-map.appspot.com";
	private static final int AuthToken = 0;
	private static final int AuthCookie = 1;
	private static final int AuthWebAccess = 2;
	private static final int AuthUnknownHostException = 3;
	private boolean isHttpClient = true;
	
	private static DefaultHttpClient httpclient = null;
	
	private TextView mTextView_URL;
	private Spinner mSpinner_Groupname;
	private TextView mEditText_Email;
	private TextView mEditText_Pass;
	private CheckBox mCheckBox;
	private TextView mTextView_AuthToken;
	private TextView mTextView_AuthCookie;
	private WebView mWebView;
	private ProgressDialog progressDialog;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.google_account);
		mTextView_URL = (TextView) findViewById(R.id.mTextView_URL);
		mTextView_URL.setText(R.string.share_location_note);
		mSpinner_Groupname = (Spinner) findViewById(R.id.mSpinner_Groupname);
		mSpinner_Groupname.setOnItemSelectedListener(mSpinnerListener);
		mEditText_Email = (EditText) findViewById(R.id.mEditText_Email);
		mEditText_Email.setText(email);
		mEditText_Pass = (EditText) findViewById(R.id.mEditText_Pass);
		mEditText_Pass.setText(password);
		mCheckBox = (CheckBox) findViewById(R.id.mCheckBox);
		mCheckBox.setOnCheckedChangeListener(mCheckBoxOnCheckedChangeListener);
		mTextView_AuthToken = (TextView) findViewById(R.id.mTextView_AuthToken);
		mTextView_AuthCookie = (TextView) findViewById(R.id.mTextView_AuthCookie);
		mWebView = (WebView) findViewById(R.id.mWebView);
		mWebView.setNetworkAvailable(true);
		mWebView.getSettings().setJavaScriptEnabled(true);
		mWebView.getSettings().setBuiltInZoomControls(true);
		mWebView.setWebViewClient(new MyWebClient());
		// Get a singleton instance of DefaultHttpClient
		if (httpclient == null) {
			Log.i(TAG, "new DefaultHttpClient()");
			httpclient = new DefaultHttpClient();
			HttpParams httpParams = httpclient.getParams();
			HttpConnectionParams.setConnectionTimeout(httpParams, 3000);
			HttpConnectionParams.setSoTimeout(httpParams, 3000);				
		}
	}
	
	@Override
	public void onPause() {
		super.onPause();
		if (this.progressDialog != null && this.progressDialog.isShowing()) {
			this.progressDialog.dismiss();
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (httpclient != null) {
			httpclient.getConnectionManager().shutdown();
			httpclient = null;
		}
	}
		
	// use a Handler to update the UI (send the Handler messages from other threads)
	private final Handler authHandler = new Handler() {
		@Override
		public void handleMessage(final Message msg) {
			progressDialog.dismiss();
			String content = "";
			if (msg.what != AuthCookie)
				content = (String) msg.obj;
			
			if (msg.what == AuthToken) {
				int index = content.indexOf("Auth=");
				if (index > -1) {
					content = content.substring(index, content.length()).trim();
					String token = content.replace("Auth=", "");
					Log.i(TAG, "token.length() = " + (token.length()));
					// Uses the token to get a cookie
					getAuthCookie(token);
				}
//				mTextView_AuthToken.setText(content);
				
			} else if (msg.what == AuthCookie) {
				// Uses the cookies to access the authenticated resource from HttpClient or WebView
				if (isHttpClient) {
					// org.apache.http.client.HttpClient
					getGAEResource("/web/JoinedGroups");
				} else {
					// android.webkit.WebView
					CookieStore cookieStore = (CookieStore) msg.obj;
					mTextView_AuthCookie.setText("Get "+cookieStore.getCookies().size()+" cookies successfully.");
					List<Cookie> cookies = cookieStore.getCookies();
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
					mWebView.setVisibility(View.VISIBLE);
					mWebView.loadUrl(GoogleAppEngineURL);
				}
				
			} else if (msg.what == AuthWebAccess) {
				String[] array = null;
				try {
					JSONObject jObject = new JSONObject(content);
					String joinedGroups = jObject.getString("JoinedGroups");
					if (joinedGroups.length() > 0) {
						array = joinedGroups.split(",");
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
				ArrayAdapter<String> adapter = new ArrayAdapter<String>(GoogleAccountClient.this,
						android.R.layout.simple_spinner_item, array);
				mSpinner_Groupname.setAdapter(adapter);
				mWebView.setVisibility(View.VISIBLE);
				mWebView.loadDataWithBaseURL(GoogleAppEngineURL, content, "text/html", "UTF-8", GoogleAppEngineURL);
				
			} else if (msg.what == AuthUnknownHostException) {
				mTextView_AuthToken.setText(content);
			}
		}
	};
	
	private final class MyWebClient extends WebViewClient {
		@Override
		public void onPageFinished(WebView view, String url) {
			setTitle(view.getTitle());
		}
	}

	private OnItemSelectedListener mSpinnerListener = new OnItemSelectedListener() {
		@Override
		public void onItemSelected(AdapterView<?> arg0, View arg1, int index, long arg3) {
			// TODO Auto-generated method stub
			
		}
		@Override
		public void onNothingSelected(AdapterView<?> arg0) {
			// TODO Auto-generated method stub
			
		}
	};
	
	private OnCheckedChangeListener mCheckBoxOnCheckedChangeListener = new CheckBox.OnCheckedChangeListener() {
		@Override
		public void onCheckedChanged(CompoundButton view, boolean isChecked) {
			if (isChecked) {
				mTextView_AuthToken.setText("");
				mTextView_AuthCookie.setText("");
				mWebView.setVisibility(View.GONE);
				String email = mEditText_Email.getText().toString().trim();
				String password = mEditText_Pass.getText().toString();
				if (!email.equals("") && !password.equals("")) {
					getAuthToken(email, password);
				} else {
					mTextView_AuthToken.setText("The email or password is empty.");
				}
			} else {
				
			}
		}
	};
	
	/**
	 * Uses ClientLogin to authenticate the user, returning an auth token.
	 * 
	 * @param email    The user's email address
	 * @param password The user's password
	 */
	private void getAuthToken(final String email, final String password) {
		progressDialog = ProgressDialog.show(GoogleAccountClient.this, 
				"Sign in", "Connecting...");
		progressDialog.setCancelable(true);
		
		// do the HTTP dance in a separate thread
		new Thread() {
			@Override
			public void run() {
				String errorMessage = null;
				try {
					List<NameValuePair> nvps = new ArrayList<NameValuePair>();
					nvps.add(new BasicNameValuePair("Email", email));
					nvps.add(new BasicNameValuePair("Passwd", password));
					nvps.add(new BasicNameValuePair("service", "ah")); // Google App Engine
					nvps.add(new BasicNameValuePair("source", "Android"));
					nvps.add(new BasicNameValuePair("accountType", "HOSTED_OR_GOOGLE"));
					
					HttpPost httppost = new HttpPost(ClientLoginURL);
					httppost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
					Log.i(TAG, httppost.getRequestLine().toString());
					
					HttpResponse response = httpclient.execute(httppost);
					StatusLine statusLine = response.getStatusLine();
					Log.i(TAG, statusLine.toString()); // HTTP/1.1 200 OK
//					Log.i(TAG, "ProtocolVersion: " + statusLine.getProtocolVersion());
//					Log.i(TAG, "StatusCode: " + statusLine.getStatusCode());
//					Log.i(TAG, "ReasonPhrase: " + statusLine.getReasonPhrase());
					
					HttpEntity entity = response.getEntity();
					String content = "";
					if (entity != null) {
						content = EntityUtils.toString(entity);
						Log.i(TAG, content);
					}
					
					httppost.abort();
					Message msg = authHandler.obtainMessage();
					msg.what = AuthToken;
					msg.obj = content;
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
		}.start();
	}
	
	/**
	 * Fetches authentication cookies for an authentication token.</br>
	 * i.e. Uses the token to get a cookie, and then set the cookie on all subsequent requests to Google service.
	 * 
	 * @param authToken The authentication token returned by ClientLogin.
	 */
	private void getAuthCookie(final String authToken) {
		progressDialog = ProgressDialog.show(GoogleAccountClient.this, 
				"Get cookie", "Connecting...");
		progressDialog.setCancelable(true);
		
		// do the HTTP dance in a separate thread
		new Thread() {
			@Override
			public void run() {
				try {
					HttpPost httppost = new HttpPost(GoogleAppEngineURL + "/_ah/login?auth=" + authToken);
					Log.i(TAG, httppost.getRequestLine().toString());
					
					HttpResponse response = httpclient.execute(httppost);
					StatusLine statusLine = response.getStatusLine();
					Log.i(TAG, statusLine.toString()); // HTTP/1.1 204 No Content
					
					HttpEntity entity = response.getEntity();
					if (entity != null) {
						String content = EntityUtils.toString(entity);
						Log.i(TAG, content);
					}
					
					Log.i(TAG, "Cookies:");
					CookieStore cookieStore = httpclient.getCookieStore();
					List<Cookie> cookies = cookieStore.getCookies();
					if (cookies.isEmpty()) {
						Log.i(TAG, "None");
					} else {
						for (int i=0; i < cookies.size(); i++) {
							Log.i(TAG, cookies.get(i).toString());
						}
					}
					
					httppost.abort();					
					Message msg = authHandler.obtainMessage();
					msg.what = AuthCookie;
					msg.obj = cookieStore;
					authHandler.sendMessage(msg);
					
				} catch (UnknownHostException e) {
					e.printStackTrace();
				} catch (SocketTimeoutException e) {
					e.printStackTrace();
				} catch (ClientProtocolException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					//httpclient.getConnectionManager().shutdown();
				}
			}
		}.start();
	}
	
	private void getGAEResource(final String url) {
		progressDialog = ProgressDialog.show(GoogleAccountClient.this, 
				"Get GAE resource", "Connecting...");
		progressDialog.setCancelable(true);
		
		// do the HTTP dance in a separate thread
		new Thread() {
			@Override
			public void run() {
				String errorMessage = null;
				try {
					HttpPost httppost = new HttpPost(GoogleAppEngineURL + url);
					Log.i(TAG, httppost.getRequestLine().toString());
					
					// class variable 'httpclient' has had the CookieStore got from previous step
					HttpResponse response = httpclient.execute(httppost);
					StatusLine statusLine = response.getStatusLine();
					Log.i(TAG, statusLine.toString()); // HTTP/1.1 200 OK
					
					HttpEntity entity = response.getEntity();
					String content = "";
					if (entity != null) {
						content = EntityUtils.toString(entity);
						Log.i(TAG, content);
					}
					
					httppost.abort();
					Message msg = authHandler.obtainMessage();
					msg.what = AuthWebAccess;
					msg.obj = content;
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
		}.start();
	}
	
}