package com.waleedsayed.adblock;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.location.provider.ProviderProperties;
import android.os.Build;
import android.os.Process;
import android.provider.Settings;

public final class MockLocationHelper {
    private MockLocationHelper() {}

    public static boolean isMockLocationEnabled(Context context) {
        try {
            AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
            if (appOps != null) {
                int mode = appOps.unsafeCheckOpNoThrow(
                        AppOpsManager.OPSTR_MOCK_LOCATION,
                        Process.myUid(),
                        context.getPackageName()
                );
                if (mode == AppOpsManager.MODE_ALLOWED) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
        }

        try {
            LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            if (lm == null) {
                return false;
            }
            String testProvider = "gps";
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ProviderProperties props = new ProviderProperties.Builder()
                        .setHasNetworkRequirement(false)
                        .setHasSatelliteRequirement(true)
                        .setHasCellRequirement(false)
                        .setHasMonetaryCost(false)
                        .setHasAltitudeSupport(true)
                        .setHasSpeedSupport(true)
                        .setHasBearingSupport(true)
                        .setPowerUsage(ProviderProperties.POWER_USAGE_LOW)
                        .setAccuracy(ProviderProperties.ACCURACY_FINE)
                        .build();
                lm.addTestProvider(testProvider, props);
            } else {
                lm.addTestProvider(testProvider, false, true, false, false, true, true, true, 1, 1);
            }
            lm.removeTestProvider(testProvider);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    public static void openDeveloperOptions(Context context) {
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Throwable t) {
            try {
                Intent fallback = new Intent(Settings.ACTION_SETTINGS);
                fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(fallback);
            } catch (Throwable ignored) {
            }
        }
    }
}
