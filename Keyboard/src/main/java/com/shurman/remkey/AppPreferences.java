package com.shurman.remkey;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class AppPreferences {
    private static final String KEY_LAST_CONNECTED_MAC = "mac";
    private static final String DEFAULT_LAST_CONNECTED_MAC = "";

    public static void saveLastConnectedMac(Context context, String macAddress) {
        SharedPreferences.Editor editor =
                PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putString(KEY_LAST_CONNECTED_MAC, macAddress);
        editor.apply();
    }

    public static String getKeyLastConnectedMac(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                        .getString(KEY_LAST_CONNECTED_MAC, DEFAULT_LAST_CONNECTED_MAC);
    }
}
