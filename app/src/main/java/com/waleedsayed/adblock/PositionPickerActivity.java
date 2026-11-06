package com.waleedsayed.adblock;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.webkit.GeolocationPermissions;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import org.json.JSONObject;

public class PositionPickerActivity extends AppCompatActivity {
    public static final String EXTRA_INITIAL_LAT = "initial_lat";
    public static final String EXTRA_INITIAL_LON = "initial_lon";
    public static final String EXTRA_PICKED_LAT = "picked_lat";
    public static final String EXTRA_PICKED_LON = "picked_lon";

    private static final int REQ_LOCATION = 401;
    private static final int REQ_NATIVE_FALLBACK = 402;

    private WebView mapWebView;
    private ProgressBar mapProgress;
    private FusedLocationProviderClient fusedClient;
    private double initialLat = 24.7136;
    private double initialLon = 46.6753;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.wrap(newBase));
    }

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_position_picker);

        initialLat = getIntent().getDoubleExtra(EXTRA_INITIAL_LAT, 24.7136);
        initialLon = getIntent().getDoubleExtra(EXTRA_INITIAL_LON, 46.6753);
        fusedClient = LocationServices.getFusedLocationProviderClient(this);

        mapWebView = findViewById(R.id.mapWebView);
        mapProgress = findViewById(R.id.mapProgress);

        WebSettings settings = mapWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setGeolocationEnabled(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        mapWebView.setBackgroundColor(ContextCompat.getColor(this, R.color.bg));
        mapWebView.addJavascriptInterface(new Bridge(), "PositionBridge");
        mapWebView.addJavascriptInterface(new Bridge(), "AndroidPicker");

        mapWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                mapProgress.setVisibility(View.GONE);
            }
        });

        mapWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                callback.invoke(origin, true, false);
            }
        });

        ensureLocationPermission();
        mapWebView.loadUrl("file:///android_asset/obd/picker.html?lat=" + initialLat + "&lon=" + initialLon);
    }

    private void ensureLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQ_LOCATION
            );
        }
    }

    @SuppressLint("MissingPermission")
    private void fetchMyLocationForWeb() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ensureLocationPermission();
            notifyLocationError("permission_denied");
            return;
        }

        CancellationTokenSource cts = new CancellationTokenSource();
        fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.getToken())
                .addOnSuccessListener(this, loc -> {
                    if (loc != null) {
                        sendLocationToWeb(loc.getLatitude(), loc.getLongitude());
                    } else {
                        fallbackLastKnownLocation();
                    }
                })
                .addOnFailureListener(this, err -> fallbackLastKnownLocation());
    }

    @SuppressLint("MissingPermission")
    private void fallbackLastKnownLocation() {
        try {
            LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            Location best = null;
            if (lm != null) {
                for (String provider : new String[]{LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER}) {
                    try {
                        Location l = lm.getLastKnownLocation(provider);
                        if (l != null && (best == null || l.getTime() > best.getTime())) {
                            best = l;
                        }
                    } catch (Throwable ignored) {
                    }
                }
            }
            if (best != null) {
                sendLocationToWeb(best.getLatitude(), best.getLongitude());
            } else {
                notifyLocationError("unavailable");
            }
        } catch (Throwable t) {
            notifyLocationError("unavailable");
        }
    }

    private void sendLocationToWeb(double lat, double lon) {
        runOnUiThread(() -> mapWebView.evaluateJavascript(
                "if (window.onNativeLocation) window.onNativeLocation(" + lat + "," + lon + ", true); " +
                        "if (window.onAndroidMyLocation) window.onAndroidMyLocation(" + lat + "," + lon + ", null);",
                null
        ));
    }

    private void notifyLocationError(String reason) {
        runOnUiThread(() -> mapWebView.evaluateJavascript(
                "if (window.onNativeLocation) window.onNativeLocation(0, 0, false); " +
                        "if (window.onAndroidMyLocation) window.onAndroidMyLocation(null, null, '" + reason + "');",
                null
        ));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_LOCATION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            fetchMyLocationForWeb();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_NATIVE_FALLBACK) {
            if (resultCode == RESULT_OK && data != null) {
                setResult(RESULT_OK, data);
            }
            finish();
        }
    }

    public class Bridge {
        @JavascriptInterface
        public String getInitialState() {
            JSONObject obj = new JSONObject();
            try {
                obj.put("lat", initialLat);
                obj.put("lon", initialLon);
                obj.put("lang", LocaleHelper.getLanguage(PositionPickerActivity.this));
                obj.put("theme", ThemeHelper.getTheme(PositionPickerActivity.this));
            } catch (Throwable ignored) {
            }
            return obj.toString();
        }

        @JavascriptInterface
        public void pick(double lat, double lon) {
            confirmPick(lat, lon);
        }

        @JavascriptInterface
        public void confirmPick(double lat, double lon) {
            runOnUiThread(() -> {
                Intent result = new Intent();
                result.putExtra(EXTRA_PICKED_LAT, lat);
                result.putExtra(EXTRA_PICKED_LON, lon);
                setResult(RESULT_OK, result);
                finish();
            });
        }

        @JavascriptInterface
        public void cancel() {
            runOnUiThread(() -> {
                setResult(RESULT_CANCELED);
                finish();
            });
        }

        @JavascriptInterface
        public void locate() {
            requestMyLocation();
        }

        @JavascriptInterface
        public void requestMyLocation() {
            runOnUiThread(PositionPickerActivity.this::fetchMyLocationForWeb);
        }

        @JavascriptInterface
        public void search(String query) {
            if (query == null || query.trim().isEmpty()) return;
            final String q = query.trim();
            new Thread(() -> {
                String json = NominatimGeocoder.search(q, LocaleHelper.getLanguage(PositionPickerActivity.this));
                runOnUiThread(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        mapWebView.evaluateJavascript("window.onSearchResults && window.onSearchResults(" + json + ");", null);
                    }
                });
            }).start();
        }

        @JavascriptInterface
        public String searchPlace(String query) {
            return NominatimGeocoder.search(query, LocaleHelper.getLanguage(PositionPickerActivity.this));
        }

        @JavascriptInterface
        public void openNativePicker() {
            openNativeFallback(initialLat, initialLon);
        }

        @JavascriptInterface
        public void openNativeFallback(double lat, double lon) {
            runOnUiThread(() -> {
                Intent intent = new Intent(PositionPickerActivity.this, NativePickerActivity.class);
                intent.putExtra(EXTRA_INITIAL_LAT, lat);
                intent.putExtra(EXTRA_INITIAL_LON, lon);
                startActivityForResult(intent, REQ_NATIVE_FALLBACK);
            });
        }

        @JavascriptInterface
        public void selectionChanged(double lat, double lon) {
            initialLat = lat;
            initialLon = lon;
        }
    }
}
