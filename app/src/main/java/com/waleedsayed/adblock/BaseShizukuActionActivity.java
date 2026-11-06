package com.waleedsayed.adblock;

import android.app.Activity;
import android.os.Bundle;
import android.os.IBinder;
import java.lang.reflect.Method;
import rikka.shizuku.Shizuku;
import rikka.shizuku.ShizukuRemoteProcess;

public abstract class BaseShizukuActionActivity extends Activity {
    private static final int REQUEST_CODE_SHIZUKU = 1001;

    private final Shizuku.OnRequestPermissionResultListener permissionListener = (requestCode, grantResult) -> {
        if (requestCode == REQUEST_CODE_SHIZUKU) {
            if (grantResult == 0) {
                runCommandAsync();
            } else {
                finishSilently();
            }
        }
    };

    protected abstract String getShellCommand();

    protected abstract String getSuccessMessage();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(0, 0);
        Shizuku.addRequestPermissionResultListener(permissionListener);
        checkAndExecute();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Shizuku.removeRequestPermissionResultListener(permissionListener);
    }

    private void checkAndExecute() {
        try {
            IBinder binder = Shizuku.getBinder();
            if (binder == null || !binder.pingBinder()) {
                finishSilently();
                return;
            }
            if (Shizuku.checkSelfPermission() == 0) {
                runCommandAsync();
            } else if (Shizuku.shouldShowRequestPermissionRationale()) {
                finishSilently();
            } else {
                Shizuku.requestPermission(REQUEST_CODE_SHIZUKU);
            }
        } catch (Exception e) {
            finishSilently();
        }
    }

    private void runCommandAsync() {
        String command = getShellCommand();
        new Thread(() -> {
            try {
                Method newProcess = Shizuku.class.getDeclaredMethod(
                        "newProcess",
                        String[].class,
                        String[].class,
                        String.class
                );
                newProcess.setAccessible(true);
                ShizukuRemoteProcess process = (ShizukuRemoteProcess) newProcess.invoke(
                        null,
                        new String[]{"sh", "-c", command},
                        null,
                        null
                );
                process.waitFor();
            } catch (Exception ignored) {
            } finally {
                runOnUiThread(this::finishSilently);
            }
        }).start();
    }

    private void finishSilently() {
        finish();
        overridePendingTransition(0, 0);
    }
}
