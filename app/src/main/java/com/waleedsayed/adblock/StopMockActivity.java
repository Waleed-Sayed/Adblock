package com.waleedsayed.adblock;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class StopMockActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(0, 0);
        try {
            Intent stopIntent = new Intent(this, VehiclePositionService.class);
            stopIntent.setAction(VehiclePositionService.ACTION_STOP);
            startService(stopIntent);
        } catch (Throwable t) {
            try {
                stopService(new Intent(this, VehiclePositionService.class));
            } catch (Throwable ignored) {
            }
        }
        finish();
        overridePendingTransition(0, 0);
    }
}
