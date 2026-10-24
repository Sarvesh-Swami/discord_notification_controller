package com.quiethours.discord;

import android.content.ActivityNotFoundException;
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

    @PluginMethod
    public void getStatus(PluginCall call) {
        Context c = getContext();
        boolean access = NotificationManagerCompat.getEnabledListenerPackages(c)
                .contains(c.getPackageName());
        if (access) {
            // Wake the listener up if the phone stopped it.
            NotificationListenerService.requestRebind(new ComponentName(c, DiscordBlockerService.class));
        }
        PowerManager pm = (PowerManager) c.getSystemService(Context.POWER_SERVICE);

        JSObject ret = new JSObject();
        ret.put("enabled", DiscordBlockerService.isEnabled(c));
        ret.put("onAt", DiscordBlockerService.onAt(c));
        ret.put("offAt", DiscordBlockerService.offAt(c));
        ret.put("accessGranted", access);
        ret.put("batteryUnrestricted", pm != null && pm.isIgnoringBatteryOptimizations(c.getPackageName()));
        ret.put("discordInstalled", isInstalled(c, DiscordBlockerService.DISCORD));
        ret.put("sdkInt", Build.VERSION.SDK_INT);
        call.resolve(ret);
    }

    @PluginMethod
    public void setEnabled(PluginCall call) {
        boolean enabled = Boolean.TRUE.equals(call.getBoolean("enabled", true));
        DiscordBlockerService.prefs(getContext()).edit()
                .putBoolean(DiscordBlockerService.KEY_ENABLED, enabled).apply();
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
                .apply();
        call.resolve();
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
