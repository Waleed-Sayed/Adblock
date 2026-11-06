package com.waleedsayed.adblock;

import android.content.Context;
import androidx.appcompat.app.AppCompatDelegate;

public final class ThemeHelper {
    private static final String PREFS_NAME = "obd_ui_prefs";
    private static final String KEY_THEME = "theme";

    private ThemeHelper() {}

    public static String getTheme(Context context) {
        String theme = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_THEME, "dark");
        return "light".equalsIgnoreCase(theme) ? "light" : "dark";
    }

    public static void setTheme(Context context, String theme) {
        String normalized = "light".equalsIgnoreCase(theme) ? "light" : "dark";
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_THEME, normalized)
                .apply();
        applyTheme(context);
    }

    public static void applyTheme(Context context) {
        String theme = getTheme(context);
        int targetMode = "light".equals(theme)
                ? AppCompatDelegate.MODE_NIGHT_NO
                : AppCompatDelegate.MODE_NIGHT_YES;
        if (AppCompatDelegate.getDefaultNightMode() != targetMode) {
            AppCompatDelegate.setDefaultNightMode(targetMode);
        }
    }
}
