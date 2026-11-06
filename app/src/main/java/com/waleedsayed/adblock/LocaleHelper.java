package com.waleedsayed.adblock;

import android.content.Context;
import android.content.res.Configuration;
import java.util.Locale;

public final class LocaleHelper {
    private static final String PREFS_NAME = "obd_ui_prefs";
    private static final String KEY_LANG = "lang";

    private LocaleHelper() {}

    public static String getLanguage(Context context) {
        String lang = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_LANG, "ar");
        return "en".equalsIgnoreCase(lang) ? "en" : "ar";
    }

    public static void setLanguage(Context context, String lang) {
        String normalized = "en".equalsIgnoreCase(lang) ? "en" : "ar";
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_LANG, normalized)
                .apply();
    }

    public static Context wrap(Context base) {
        String lang = getLanguage(base);
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        Configuration config = new Configuration(base.getResources().getConfiguration());
        config.setLocale(locale);
        config.setLayoutDirection(locale);
        return base.createConfigurationContext(config);
    }
}
