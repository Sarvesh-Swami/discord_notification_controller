package com.quiethours.discord;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import java.util.Calendar;

/**
 * Android calls this service for every notification once the user grants
 * "Notification access". From the OFF time until the ON time it removes anything
 * Discord posts; the rest of the day it lets everything through.
 * Same rule as www/schedule.js.
 */
public class DiscordBlockerService extends NotificationListenerService {

    static final String DISCORD = "com.discord";
    static final String PREFS = "quiet_hours";
    static final String KEY_ENABLED = "enabled";
    static final String KEY_ON_AT = "onAt";
    static final String KEY_OFF_AT = "offAt";
    static final String DEFAULT_ON_AT = "09:00";
    static final String DEFAULT_OFF_AT = "20:00";

    static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, MODE_PRIVATE);
    }

    static boolean isEnabled(Context c) {
        return prefs(c).getBoolean(KEY_ENABLED, true);
    }

    static String onAt(Context c) {
        return prefs(c).getString(KEY_ON_AT, DEFAULT_ON_AT);
    }

    static String offAt(Context c) {
        return prefs(c).getString(KEY_OFF_AT, DEFAULT_OFF_AT);
    }

    static int toMinutes(String hhmm) {
        String[] p = hhmm.split(":");
        return Integer.parseInt(p[0]) * 60 + Integer.parseInt(p[1]);
    }

    static boolean isQuietTime(Context c) {
        int on = toMinutes(onAt(c));
        int off = toMinutes(offAt(c));
        Calendar now = Calendar.getInstance();
        int t = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
        if (on == off) return false;
        if (off < on) return t >= off && t < on;
        return t >= off || t < on;
    }

    private boolean shouldBlock() {
        return isEnabled(this) && isQuietTime(this);
    }

    // Removes every Discord notification currently in the shade.
    private void clearDiscord() {
        StatusBarNotification[] active = getActiveNotifications();
        if (active == null) return;
        for (StatusBarNotification sbn : active) {
            if (DISCORD.equals(sbn.getPackageName())) cancelNotification(sbn.getKey());
        }
    }

    // Runs for every app's notifications, so Discord notifications left over from
    // before the OFF time are also cleared the next time anything arrives.
    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (!shouldBlock()) return;
        if (DISCORD.equals(sbn.getPackageName())) {
            cancelNotification(sbn.getKey());
        } else {
            clearDiscord();
        }
    }

    @Override
    public void onListenerConnected() {
        if (shouldBlock()) clearDiscord();
    }

    // Some phones drop the connection (updates, battery savers); ask to be reconnected.
    @Override
    public void onListenerDisconnected() {
        requestRebind(new ComponentName(this, DiscordBlockerService.class));
    }
}
