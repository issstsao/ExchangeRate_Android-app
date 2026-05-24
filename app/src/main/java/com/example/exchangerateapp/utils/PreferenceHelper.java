package com.example.exchangerateapp.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferenceHelper {
    private static final String PREF_NAME = "ExchangeRatePrefs";
    private SharedPreferences prefs;

    public PreferenceHelper(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // Alert
    public void saveAlertSettings(String from, String to, float targetRate, boolean enabled) {
        prefs.edit()
                .putString("alert_from_currency", from)
                .putString("alert_to_currency", to)
                .putFloat("alert_target_rate", targetRate)
                .putBoolean("alert_enabled", enabled)
                .apply();
    }

    public String getAlertFrom() { return prefs.getString("alert_from_currency", "USD"); }
    public String getAlertTo() { return prefs.getString("alert_to_currency", "TWD"); }
    public float getAlertTargetRate() { return prefs.getFloat("alert_target_rate", 0f); }
    public boolean isAlertEnabled() { return prefs.getBoolean("alert_enabled", false); }
    public void disableAlert() { prefs.edit().putBoolean("alert_enabled", false).apply(); }

    // Last used
    public void saveLastCurrencies(String from, String to) {
        prefs.edit()
                .putString("last_from_currency", from)
                .putString("last_to_currency", to)
                .apply();
    }

    public String getLastFrom() { return prefs.getString("last_from_currency", "USD"); }
    public String getLastTo() { return prefs.getString("last_to_currency", "TWD"); }

    // User ID
    public String getUserId() {
        String id = prefs.getString("firebase_user_id", null);
        if (id == null) {
            id = java.util.UUID.randomUUID().toString();
            prefs.edit().putString("firebase_user_id", id).apply();
        }
        return id;
    }
}