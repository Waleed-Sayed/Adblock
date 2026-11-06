package com.waleedsayed.adblock;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

public class SwitchMockActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(0, 0);
        Intent incoming = getIntent();
        if (incoming != null && incoming.hasExtra("lat") && incoming.hasExtra("lon")) {
            double lat = incoming.getDoubleExtra("lat", 0.0);
            double lon = incoming.getDoubleExtra("lon", 0.0);
            String label = incoming.getStringExtra("label");
            if (label == null) {
                label = "";
            }
            try {
                Intent serviceIntent = new Intent(this, VehiclePositionService.class);
                serviceIntent.setAction(VehiclePositionService.ACTION_SWITCH);
                serviceIntent.putExtra(VehiclePositionService.EXTRA_LAT, lat);
                serviceIntent.putExtra(VehiclePositionService.EXTRA_LON, lon);
                serviceIntent.putExtra(VehiclePositionService.EXTRA_LABEL, label);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent);
                } else {
                    startService(serviceIntent);
                }
            } catch (Throwable ignored) {
            }
        }
        finish();
        overridePendingTransition(0, 0);
    }
}
