package com.tekdi.hrdemo;

import android.content.Context;
import android.content.SharedPreferences;

public class Prefs {
    private static String PREF_FILE = "com.tekdi.hrdemo.prefs";
    private static String DEVICE_REG_ID_PREF = "device_reg_id";

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREF_FILE, 0);
    }

    public static String getDeviceRegIdPref(Context context) {
        return getPrefs(context).getString(DEVICE_REG_ID_PREF, "");
    }

    public static void setDeviceIdRegPref(Context context, String value) {
        // perform validation etc..
        getPrefs(context).edit().putString(DEVICE_REG_ID_PREF, value).commit();
    }

}