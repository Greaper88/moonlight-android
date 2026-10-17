package com.limelight.ui;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Point;
import android.os.BatteryManager;
import android.os.Build;
import android.os.PowerManager;
import android.view.Display;

import com.limelight.R;

/** Public Android device readings, sampled only while the secondary stats panel is visible. */
public final class DeviceStats {
    private DeviceStats() {}

    public static String collect(Context context, Display streamDisplay, Display controlsDisplay) {
        StringBuilder text = new StringBuilder();
        text.append(Build.MANUFACTURER).append(' ').append(Build.MODEL).append('\n');
        text.append(context.getString(R.string.grimm_stats_android,
                Build.VERSION.RELEASE, Build.VERSION.SDK_INT)).append('\n');

        try {
            Intent battery = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (battery != null && battery.getBooleanExtra(BatteryManager.EXTRA_PRESENT, true)) {
                int level = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                int status = battery.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN);
                int plugged = battery.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
                int state = status == BatteryManager.BATTERY_STATUS_CHARGING ? R.string.grimm_stats_charging :
                        status == BatteryManager.BATTERY_STATUS_FULL ? R.string.grimm_stats_full :
                        plugged > 0 ? R.string.grimm_stats_plugged_in :
                        status == BatteryManager.BATTERY_STATUS_DISCHARGING ? R.string.grimm_stats_on_battery :
                        R.string.grimm_stats_unknown;
                if (level >= 0 && scale > 0) {
                    int percent = Math.max(0, Math.min(100, Math.round(level * 100f / scale)));
                    text.append(context.getString(R.string.grimm_stats_battery, percent,
                            context.getString(state))).append('\n');
                }
                if (battery.hasExtra(BatteryManager.EXTRA_TEMPERATURE)) {
                    text.append(context.getString(R.string.grimm_stats_battery_temperature,
                            battery.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10f)).append('\n');
                }
            }
        } catch (RuntimeException ignored) {
            // Some device firmwares omit these readings. Other sections can still be displayed.
        }

        try {
            ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (manager != null) {
                ActivityManager.MemoryInfo memory = new ActivityManager.MemoryInfo();
                manager.getMemoryInfo(memory);
                if (memory.totalMem > 0) {
                    text.append(context.getString(R.string.grimm_stats_memory,
                            memory.availMem / (1024f * 1024 * 1024),
                            memory.totalMem / (1024f * 1024 * 1024))).append('\n');
                }
            }
        } catch (RuntimeException ignored) {}

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                PowerManager power = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                if (power != null) {
                    String[] states = context.getResources().getStringArray(R.array.grimm_stats_thermal_states);
                    int state = power.getCurrentThermalStatus();
                    text.append(context.getString(R.string.grimm_stats_thermal,
                            state >= 0 && state < states.length ? states[state] :
                                    context.getString(R.string.grimm_stats_unknown))).append('\n');
                }
            } catch (RuntimeException ignored) {}
        }

        appendDisplay(context, text, streamDisplay, R.string.grimm_stats_stream_display);
        appendDisplay(context, text, controlsDisplay, R.string.grimm_stats_controls_display);
        return text.toString().trim();
    }

    @SuppressWarnings("deprecation")
    private static void appendDisplay(Context context, StringBuilder text, Display display, int label) {
        if (display == null || !display.isValid()) return;
        try {
            Point size = new Point();
            display.getRealSize(size);
            text.append(context.getString(R.string.grimm_stats_display, context.getString(label),
                    size.x, size.y, display.getRefreshRate())).append('\n');
        } catch (RuntimeException ignored) {}
    }
}
