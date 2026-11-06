package com.waleedsayed.adblock;

import android.app.Activity;
import android.content.Intent;
import com.waleedsayed.adblock.security.PinStore;

public class HiddenTriggerHelper {
    private final Activity activity;

    public HiddenTriggerHelper(Activity activity) {
        this.activity = activity;
    }

    public void requestHidden() {
        openSecretArea();
    }

    public void registerTap() {
        openSecretArea();
    }

    private void openSecretArea() {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        PinStore store = new PinStore(activity);
        Intent intent = new Intent(activity, PinActivity.class);
        intent.putExtra(
                PinActivity.EXTRA_MODE,
                store.hasSecret() ? PinActivity.MODE_VERIFY : PinActivity.MODE_SET
        );
        intent.putExtra(PinActivity.EXTRA_NEXT_VEHICLE_POSITION, true);
        activity.startActivity(intent);
    }
}
