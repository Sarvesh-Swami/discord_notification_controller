package com.quiethours.discord;

import android.content.Context;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import java.util.Calendar;

/**
 * Android calls this service for every notification once the user grants
 * "Notification access". Between 8:00 PM and 9:00 AM it removes anything Discord
 * posts; between 9:00 AM and 8:00 PM it lets everything through.
 * Hours must match www/schedule.js.
 */
public class DiscordBlockerService extends NotificationListenerService {

    static final String DISCORD = "com.discord";
    static final int ALLOW_FROM_HOUR = 9;
    static final int BLOCK_FROM_HOUR = 20;
    static final String PREFS = "quiet_hours";
    static final String KEY_ENABLED = "enabled";

    static boolean isEnabled(Context c) {
        return c.getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(KEY_ENABLED, true);
    }

    static boolean isQuietTime() {
        int h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        return h >= BLOCK_FROM_HOUR || h < ALLOW_FROM_HOUR;
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (DISCORD.equals(sbn.getPackageName()) && isEnabled(this) && isQuietTime()) {
            cancelNotification(sbn.getKey());
        }
    }

    // When the service (re)connects during quiet hours, clear Discord notifications
    // that are already in the shade.
    @Override
    public void onListenerConnected() {
        if (!isEnabled(this) || !isQuietTime()) return;
        StatusBarNotification[] active = getActiveNotifications();
        if (active == null) return;
        for (StatusBarNotification sbn : active) {
            if (DISCORD.equals(sbn.getPackageName())) cancelNotification(sbn.getKey());
        }
    }
}
