package com.quiethours.discord;

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;

import androidx.core.app.NotificationManagerCompat;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import java.util.regex.Pattern;

/** The bridge app.js calls through Capacitor.nativePromise('QuietHours', ...). */
@CapacitorPlugin(name = "QuietHours")
public class QuietHoursPlugin extends Plugin {

    private static final Pattern HHMM = Pattern.compile("([01]\\d|2[0-3]):[0-5]\\d");

    private boolean accessGranted() {
        Context c = getContext();
        return NotificationManagerCompat.getEnabledListenerPackages(c).contains(c.getPackageName());
    }

    private boolean batteryUnrestricted() {
        Context c = getContext();
        PowerManager pm = (PowerManager) c.getSystemService(Context.POWER_SERVICE);
        return pm != null && pm.isIgnoringBatteryOptimizations(c.getPackageName());
    }

    @PluginMethod
    public void getStatus(PluginCall call) {
        Context c = getContext();
        boolean access = accessGranted();
        DiscordBlockerService svc = DiscordBlockerService.connected;
        if (access && svc == null) {
            // Wake the listener up if the phone stopped it.
            NotificationListenerService.requestRebind(new ComponentName(c, DiscordBlockerService.class));
        }

        JSObject ret = new JSObject();
        ret.put("enabled", DiscordBlockerService.isEnabled(c));
        ret.put("onAt", DiscordBlockerService.onAt(c));
        ret.put("offAt", DiscordBlockerService.offAt(c));
        ret.put("accessGranted", access);
        ret.put("listenerConnected", svc != null);
        ret.put("discordInShade", svc != null ? svc.countDiscord() : -1);
        ret.put("batteryUnrestricted", batteryUnrestricted());
        ret.put("discordInstalled", isInstalled(c, DiscordBlockerService.DISCORD));
        ret.put("sdkInt", Build.VERSION.SDK_INT);
        call.resolve(ret);
    }

    // Called once when the screen opens, so the log shows the phone's state.
    @PluginMethod
    public void logAppOpened(PluginCall call) {
        Context c = getContext();
        EventLog.add(c, "App opened | " + Build.MANUFACTURER + " " + Build.MODEL
                + " Android " + Build.VERSION.RELEASE + " (SDK " + Build.VERSION.SDK_INT + ")"
                + " | access=" + accessGranted()
                + " listener=" + (DiscordBlockerService.connected != null ? "connected" : "NOT connected")
                + " battery=" + (batteryUnrestricted() ? "unrestricted" : "optimized")
                + " discordInstalled=" + isInstalled(c, DiscordBlockerService.DISCORD)
                + " | " + DiscordBlockerService.describe(c));
        call.resolve();
    }

    @PluginMethod
    public void setEnabled(PluginCall call) {
        boolean enabled = Boolean.TRUE.equals(call.getBoolean("enabled", true));
        DiscordBlockerService.prefs(getContext()).edit()
                .putBoolean(DiscordBlockerService.KEY_ENABLED, enabled).apply();
        EventLog.add(getContext(), "Schedule switched " + (enabled ? "ON" : "OFF (paused)"));
        call.resolve();
    }

    @PluginMethod
    public void setSchedule(PluginCall call) {
        String onAt = call.getString("onAt", "");
        String offAt = call.getString("offAt", "");
        if (!HHMM.matcher(onAt).matches() || !HHMM.matcher(offAt).matches()) {
            call.reject("Times must be HH:MM (24-hour)");
            return;
        }
        DiscordBlockerService.prefs(getContext()).edit()
                .putString(DiscordBlockerService.KEY_ON_AT, onAt)
                .putString(DiscordBlockerService.KEY_OFF_AT, offAt)
                .commit();
        EventLog.add(getContext(), "Times saved | " + DiscordBlockerService.describe(getContext()));
        call.resolve();
    }

    @PluginMethod
    public void getLog(PluginCall call) {
        JSObject ret = new JSObject();
        ret.put("text", EventLog.read(getContext()));
        call.resolve(ret);
    }

    @PluginMethod
    public void clearLog(PluginCall call) {
        EventLog.clear(getContext());
        call.resolve();
    }

    @PluginMethod
    public void copyLog(PluginCall call) {
        ClipboardManager cm = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("Discord Quiet Hours log", EventLog.read(getContext())));
        call.resolve();
    }

    @PluginMethod
    public void shareLog(PluginCall call) {
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_SUBJECT, "Discord Quiet Hours log");
        send.putExtra(Intent.EXTRA_TEXT, EventLog.read(getContext()));
        open(call, Intent.createChooser(send, "Share log"));
    }

    // Opens this app's own switch on Android 11+, or the full list on older phones.
    @PluginMethod
    public void openAccessSettings(PluginCall call) {
        Context c = getContext();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent detail = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS);
            detail.putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                    new ComponentName(c, DiscordBlockerService.class).flattenToString());
            if (tryStart(detail)) { call.resolve(); return; }
        }
        open(call, new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
    }

    // Discord's own notification settings, to check its switch is ON.
    @PluginMethod
    public void openDiscordSettings(PluginCall call) {
        Intent i = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
        i.putExtra(Settings.EXTRA_APP_PACKAGE, DiscordBlockerService.DISCORD);
        if (tryStart(i)) { call.resolve(); return; }
        open(call, new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + DiscordBlockerService.DISCORD)));
    }

    // App info screen: where Android 13+ hides "Allow restricted settings" (⋮ menu)
    // for apps installed from an APK file.
    @PluginMethod
    public void openAppInfo(PluginCall call) {
        open(call, new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + getContext().getPackageName())));
    }

    @PluginMethod
    public void requestBatteryUnrestricted(PluginCall call) {
        Intent ask = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                Uri.parse("package:" + getContext().getPackageName()));
        if (tryStart(ask)) { call.resolve(); return; }
        open(call, new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
    }

    private void open(PluginCall call, Intent i) {
        if (tryStart(i)) call.resolve();
        else call.reject("This phone has no screen for " + i.getAction());
    }

    private boolean tryStart(Intent i) {
        try {
            getActivity().startActivity(i);
            return true;
        } catch (ActivityNotFoundException | SecurityException e) {
            EventLog.add(getContext(), "Could not open " + i.getAction() + ": " + e.getClass().getSimpleName());
            return false;
        }
    }

    private static boolean isInstalled(Context c, String pkg) {
        try {
            c.getPackageManager().getPackageInfo(pkg, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}
