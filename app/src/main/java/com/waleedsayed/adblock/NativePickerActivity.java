package com.waleedsayed.adblock;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.util.MapTileIndex;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;

public class NativePickerActivity extends AppCompatActivity {
    private static final OnlineTileSourceBase ESRI_WORLD_IMAGERY = new XYTileSource(
            "EsriWorldImagery",
            0, 19, 256, ".jpg",
            new String[]{"https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/"}
    ) {
        @Override
        public String getTileURLString(long pMapTileIndex) {
            return getBaseUrl()
                    + MapTileIndex.getZoom(pMapTileIndex) + "/"
                    + MapTileIndex.getY(pMapTileIndex) + "/"
                    + MapTileIndex.getX(pMapTileIndex);
        }
    };

    private MapView osmMap;
    private TextView osmCoords;
    private Button osmConfirm;
    private Button osmSatellite;
    private EditText osmSearchInput;
    private ListView osmResults;

    private Marker targetMarker;
    private Double pickedLat = null;
    private Double pickedLon = null;
    private boolean satelliteEnabled = false;

    private FusedLocationProviderClient fusedClient;
    private final ExecutorService searchExecutor = Executors.newSingleThreadExecutor();
    private final List<JSONObject> currentSearchResults = new ArrayList<>();

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);

        Configuration.getInstance().load(getApplicationContext(), PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
        Configuration.getInstance().setUserAgentValue(getPackageName());

        setContentView(R.layout.activity_native_picker);

        fusedClient = LocationServices.getFusedLocationProviderClient(this);

        osmMap = findViewById(R.id.osmMap);
        osmCoords = findViewById(R.id.osmCoords);
        osmConfirm = findViewById(R.id.osmConfirm);
        osmSatellite = findViewById(R.id.osmSatellite);
        osmSearchInput = findViewById(R.id.osmSearchInput);
        osmResults = findViewById(R.id.osmResults);

        osmMap.setTileSource(TileSourceFactory.MAPNIK);
        osmMap.setMultiTouchControls(true);

        double initLat = getIntent().getDoubleExtra(PositionPickerActivity.EXTRA_INITIAL_LAT, 24.7136);
        double initLon = getIntent().getDoubleExtra(PositionPickerActivity.EXTRA_INITIAL_LON, 46.6753);

        osmMap.getController().setZoom(14.0);
        osmMap.getController().setCenter(new GeoPoint(initLat, initLon));
        setPickedPoint(initLat, initLon, false);

        MapEventsOverlay eventsOverlay = new MapEventsOverlay(new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                osmResults.setVisibility(View.GONE);
                setPickedPoint(p.getLatitude(), p.getLongitude(), false);
                return true;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                setPickedPoint(p.getLatitude(), p.getLongitude(), false);
                return true;
            }
        });
        osmMap.getOverlays().add(0, eventsOverlay);

        findViewById(R.id.osmSearchBtn).setOnClickListener(v -> performSearch());
        osmSearchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch();
                return true;
            }
            return false;
        });

        osmResults.setOnItemClickListener((parent, view, position, id) -> {
            if (position >= 0 && position < currentSearchResults.size()) {
                JSONObject item = currentSearchResults.get(position);
                double lat = item.optDouble("lat", 0.0);
                double lon = item.optDouble("lon", 0.0);
                osmResults.setVisibility(View.GONE);
                setPickedPoint(lat, lon, true);
            }
        });

        findViewById(R.id.osmSwitch).setOnClickListener(v -> finish());

        osmSatellite.setOnClickListener(v -> {
            satelliteEnabled = !satelliteEnabled;
            osmMap.setTileSource(satelliteEnabled ? ESRI_WORLD_IMAGERY : TileSourceFactory.MAPNIK);
            osmSatellite.setText(satelliteEnabled ? R.string.map_default : R.string.map_satellite);
            osmMap.invalidate();
        });

        findViewById(R.id.osmLocate).setOnClickListener(v -> locateMe());
        findViewById(R.id.osmCancel).setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });

        osmConfirm.setOnClickListener(v -> {
            if (pickedLat != null && pickedLon != null) {
                Intent data = new Intent();
                data.putExtra(PositionPickerActivity.EXTRA_PICKED_LAT, pickedLat);
                data.putExtra(PositionPickerActivity.EXTRA_PICKED_LON, pickedLon);
                setResult(RESULT_OK, data);
                finish();
            }
        });
    }

    private void setPickedPoint(double lat, double lon, boolean animateTo) {
        pickedLat = lat;
        pickedLon = lon;
        GeoPoint point = new GeoPoint(lat, lon);
        if (targetMarker == null) {
            targetMarker = new Marker(osmMap);
            targetMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
            Drawable dot = ContextCompat.getDrawable(this, R.drawable.ic_pin_dot);
            if (dot != null) {
                targetMarker.setIcon(dot);
            }
            osmMap.getOverlays().add(targetMarker);
        }
        targetMarker.setPosition(point);
        if (animateTo) {
            osmMap.getController().animateTo(point, 16.0, 600L);
        }
        osmMap.invalidate();
        osmCoords.setText(String.format(Locale.US, "%.6f, %.6f", lat, lon));
        osmConfirm.setEnabled(true);
    }

    private void performSearch() {
        String query = osmSearchInput.getText().toString().trim();
        if (query.isEmpty()) {
            return;
        }
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(osmSearchInput.getWindowToken(), 0);
        }
        Toast.makeText(this, R.string.searching, Toast.LENGTH_SHORT).show();
        String lang = LocaleHelper.getLanguage(this);
        searchExecutor.execute(() -> {
            String raw = NominatimGeocoder.search(query, lang);
            runOnUiThread(() -> {
                currentSearchResults.clear();
                List<String> titles = new ArrayList<>();
                try {
                    JSONArray arr = new JSONArray(raw);
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject o = arr.getJSONObject(i);
                        currentSearchResults.add(o);
                        titles.add(o.optString("name", ""));
                    }
                } catch (Throwable ignored) {
                }
                if (titles.isEmpty()) {
                    osmResults.setVisibility(View.GONE);
                    Toast.makeText(this, R.string.no_results, Toast.LENGTH_SHORT).show();
                } else {
                    osmResults.setAdapter(new ArrayAdapter<>(this, R.layout.osm_result_item, titles));
                    osmResults.setVisibility(View.VISIBLE);
                }
            });
        });
    }

    @SuppressLint("MissingPermission")
    private void locateMe() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 601);
            return;
        }
        CancellationTokenSource cts = new CancellationTokenSource();
        fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.getToken())
                .addOnSuccessListener(this, loc -> {
                    if (loc != null) {
                        setPickedPoint(loc.getLatitude(), loc.getLongitude(), true);
                    } else {
                        fallbackLocationManager();
                    }
                })
                .addOnFailureListener(this, e -> fallbackLocationManager());
    }

    @SuppressLint("MissingPermission")
    private void fallbackLocationManager() {
        try {
            LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            if (lm != null) {
                Location l = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (l == null) {
                    l = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                }
                if (l != null) {
                    setPickedPoint(l.getLatitude(), l.getLongitude(), true);
                    return;
                }
            }
        } catch (Throwable ignored) {
        }
        Toast.makeText(this, R.string.pos_failed, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (osmMap != null) osmMap.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (osmMap != null) osmMap.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        searchExecutor.shutdownNow();
    }
}
