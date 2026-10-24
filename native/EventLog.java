package com.quiethours.discord;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * On-device activity log shown in the app (newest first, last 300 lines).
 * Records times, decisions and package names only, never notification text.
 */
final class EventLog {

    private static final String PREFS = "event_log";
    private static final String KEY = "lines";
    private static final int MAX_LINES = 300;

    private EventLog() {}

    static synchronized void add(Context c, String msg) {
        SharedPreferences p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());
        String all = time + "  " + msg + "\n" + p.getString(KEY, "");
        int cut = 0;
        for (int i = 0; i < MAX_LINES && cut >= 0; i++) cut = all.indexOf('\n', cut + 1);
        if (cut > 0) all = all.substring(0, cut + 1);
        p.edit().putString(KEY, all).apply();
    }

    static synchronized String read(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "");
    }

    static synchronized void clear(Context c) {
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY).apply();
    }
}
