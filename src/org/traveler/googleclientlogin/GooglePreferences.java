package org.traveler.googleclientlogin;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

/**
 * @author tytung
 * @version 1.0
 * @date 2010/01/09
 */
public class GooglePreferences {

	private static SharedPreferences prefs;
	private static final String EMAIL = "Email";
	private static final String PASSWD = "Password";
	private static final String IsXMPPConnected = "IsXMPPConnected";

	public static void init(Application app) {
		prefs = app.getSharedPreferences("account", Context.MODE_PRIVATE);
	}

	public static void putEmail(String email) {
		put(EMAIL, email);
	}

	public static String getEmail() {
		return prefs.getString(EMAIL, "@gmail.com");
	}

	public static void putPasswd(String password) {
		put(PASSWD, password);
	}

	public static String getPasswd() {
		return prefs.getString(PASSWD, "");
	}

	public static void putIsXMPPConnected(boolean isXMPPConnected) {
		put(IsXMPPConnected, isXMPPConnected);
	}

	public static boolean getIsXMPPConnected() {
		return prefs.getBoolean(IsXMPPConnected, false);
	}

	private static void put(String name, Object value) {
		SharedPreferences.Editor editor = prefs.edit();
		if (value.getClass() == Boolean.class) {
			editor.putBoolean(name, (Boolean) value);
		}
		if (value.getClass() == String.class) {
			editor.putString(name, (String) value);
		}
		if (value.getClass() == Integer.class) {
			editor.putInt(name, ((Integer) value).intValue());
		}
		editor.commit();
	}

}
