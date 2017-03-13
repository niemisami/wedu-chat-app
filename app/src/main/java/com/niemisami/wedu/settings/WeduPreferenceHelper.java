package com.niemisami.wedu.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.niemisami.wedu.R;

/**
 * Created by Sami on 13.3.2017.
 */

public class WeduPreferenceHelper {


    public static String getUsername(Context context) {

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getString(context.getString(R.string.pref_username_key), "");
    }

    public static void storeUsername(Context context, String username) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sp.edit();

        editor.putString(context.getString(R.string.pref_username_key), username);
        editor.apply();
    }

    public static void clearUsername(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sp.edit();
        editor.remove(context.getString(R.string.pref_username_key));
        editor.apply();
    }
}
