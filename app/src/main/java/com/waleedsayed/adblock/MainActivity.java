package com.waleedsayed.adblock;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.waleedsayed.adblock.obd.ObdBridge;
import com.waleedsayed.adblock.obd.ObdManager;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int REQ_BT_PERMS = 501;

    private WebView webView;
    private ProgressBar progress;
    private ObdManager obdManager;
    private HiddenTriggerHelper triggerHelper;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.wrap(newBase));
    }

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE);
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webView);
        progress = findViewById(R.id.progress);

        obdManager = new ObdManager(getApplicationContext());
        triggerHelper = new HiddenTriggerHelper(this);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        settings.setMediaPlaybackRequiresUserGesture(false);

        webView.setBackgroundColor(ContextCompat.getColor(this, R.color.bg));
        webView.addJavascriptInterface(new ObdBridge(this, obdManager, triggerHelper), "NativeBridge");
        webView.addJavascriptInterface(new ObdBridge(this, obdManager, triggerHelper), "AndroidOBD");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                progress.setVisibility(View.GONE);
            }
        });
        webView.setWebChromeClient(new WebChromeClient());
        webView.loadUrl("file:///android_asset/obd/index.html");

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                webView.evaluateJavascript(
                        "(window.OBD && typeof window.OBD.handleBack === 'function') ? window.OBD.handleBack() : false",
                        value -> {
                            if (!"true".equals(value)) {
                                finish();
                            }
                        }
                );
            }
        });
    }

    public boolean ensureBluetoothPermissions() {
        List<String> missing = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                missing.add(Manifest.permission.BLUETOOTH_CONNECT);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                    != PackageManager.PERMISSION_GRANTED) {
                missing.add(Manifest.permission.BLUETOOTH_SCAN);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                missing.add(Manifest.permission.ACCESS_FINE_LOCATION);
            }
        }
        if (missing.isEmpty()) {
            return true;
        }
        runOnUiThread(() -> ActivityCompat.requestPermissions(
                this,
                missing.toArray(new String[0]),
                REQ_BT_PERMS
        ));
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_BT_PERMS && webView != null) {
            webView.evaluateJavascript(
                    "window.OBD && window.OBD.onPermissionsUpdated && window.OBD.onPermissionsUpdated()",
                    null
            );
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (obdManager != null) {
            obdManager.disconnect();
        }
        if (webView != null) {
            webView.removeJavascriptInterface("NativeBridge");
            webView.removeJavascriptInterface("AndroidOBD");
            webView.destroy();
        }
    }
}
