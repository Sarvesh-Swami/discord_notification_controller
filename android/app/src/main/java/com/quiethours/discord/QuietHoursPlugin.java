package com.quiethours.discord;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.provider.Settings;

import androidx.core.app.NotificationManagerCompat;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

/** The bridge app.js calls as QuietHours.getStatus() / setEnabled() / openAccessSettings(). */
@CapacitorPlugin(name = "QuietHours")
public class QuietHoursPlugin extends Plugin {

    @PluginMethod
    public void getStatus(PluginCall call) {
        Context c = getContext();
        JSObject ret = new JSObject();
        ret.put("enabled", DiscordBlockerService.isEnabled(c));
        ret.put("accessGranted",
                NotificationManagerCompat.getEnabledListenerPackages(c).contains(c.getPackageName()));
        ret.put("discordInstalled", isInstalled(c, DiscordBlockerService.DISCORD));
        call.resolve(ret);
    }

    @PluginMethod
    public void setEnabled(PluginCall call) {
        boolean enabled = Boolean.TRUE.equals(call.getBoolean("enabled", true));
        getContext().getSharedPreferences(DiscordBlockerService.PREFS, Context.MODE_PRIVATE)
                .edit().putBoolean(DiscordBlockerService.KEY_ENABLED, enabled).apply();
        call.resolve();
    }

    @PluginMethod
    public void openAccessSettings(PluginCall call) {
        Intent i = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getContext().startActivity(i);
        call.resolve();
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
