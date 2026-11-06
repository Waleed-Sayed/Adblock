package com.waleedsayed.adblock;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.location.provider.ProviderProperties;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import androidx.core.app.NotificationCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import java.util.Random;

public class VehiclePositionService extends Service {
    public static final String ACTION_START = "com.waleedsayed.adblock.action.START";
    public static final String ACTION_STOP = "com.waleedsayed.adblock.action.STOP";
    public static final String ACTION_SWITCH = "com.waleedsayed.adblock.action.SWITCH";
    public static final String ACTION_SET_MODE = "com.waleedsayed.adblock.action.SET_MODE";

    public static final String EXTRA_LAT = "lat";
    public static final String EXTRA_LON = "lon";
    public static final String EXTRA_LABEL = "label";
    public static final String EXTRA_FULL_MOCK = "full_mock";

    private static final String CHANNEL_ID = "obd2_position_channel";
    private static final int NOTIFICATION_ID = 4102;

    public static volatile boolean isRunning = false;
    public static volatile double currentLat = 24.7136;
    public static volatile double currentLon = 46.6753;
    public static volatile String currentLabel = "";
    public static volatile boolean fullMockMode = false;
    public static volatile int updateCount = 0;

    private LocationManager locationManager;
    private FusedLocationProviderClient fusedLocationClient;
    private Handler handler;
    private final Random random = new Random();

    private final Runnable mockRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isRunning) return;
            pushMockLocation();
            updateCount++;
            if (handler != null) {
                handler.postDelayed(this, 1000L);
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        handler = new Handler(Looper.getMainLooper());
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_START.equals(action) || ACTION_SWITCH.equals(action)) {
                currentLat = intent.getDoubleExtra(EXTRA_LAT, 24.7136);
                currentLon = intent.getDoubleExtra(EXTRA_LON, 46.6753);
                currentLabel = intent.getStringExtra(EXTRA_LABEL);
                if (currentLabel == null) currentLabel = "";
                fullMockMode = intent.getBooleanExtra(EXTRA_FULL_MOCK, false);

                startForeground(NOTIFICATION_ID, buildNotification());
                setupMockProviders();
                isRunning = true;
                handler.removeCallbacks(mockRunnable);
                handler.post(mockRunnable);
            } else if (ACTION_SET_MODE.equals(action)) {
                fullMockMode = intent.getBooleanExtra(EXTRA_FULL_MOCK, false);
            } else if (ACTION_STOP.equals(action)) {
                stopMocking();
                stopForeground(true);
                stopSelf();
            }
        }
        return START_STICKY;
    }

    private void stopMocking() {
        isRunning = false;
        if (handler != null) {
            handler.removeCallbacks(mockRunnable);
        }
        removeMockProviders();
    }

    private void setupMockProviders() {
        removeMockProviders();
        String[] providers = fullMockMode
                ? new String[]{LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER}
                : new String[]{LocationManager.GPS_PROVIDER};

        for (String provider : providers) {
            try {
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
                    locationManager.addTestProvider(provider, props);
                } else {
                    locationManager.addTestProvider(
                            provider,
                            false, true, false, false,
                            true, true, true,
                            Criteria.POWER_LOW, Criteria.ACCURACY_FINE
                    );
                }
                locationManager.setTestProviderEnabled(provider, true);
            } catch (Throwable ignored) {
            }
        }
    }

    private void removeMockProviders() {
        String[] providers = {LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER};
        for (String provider : providers) {
            try {
                locationManager.setTestProviderEnabled(provider, false);
                locationManager.removeTestProvider(provider);
            } catch (Throwable ignored) {
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void pushMockLocation() {
        double jitterLat = currentLat + (random.nextDouble() - 0.5) * 0.000004;
        double jitterLon = currentLon + (random.nextDouble() - 0.5) * 0.000004;

        Location gpsLoc = createLocation(LocationManager.GPS_PROVIDER, jitterLat, jitterLon, 3.0f, 15.0);
        try {
            locationManager.setTestProviderLocation(LocationManager.GPS_PROVIDER, gpsLoc);
        } catch (Throwable ignored) {
        }

        try {
            fusedLocationClient.setMockMode(true);
            fusedLocationClient.setMockLocation(gpsLoc);
        } catch (Throwable ignored) {
        }

        if (fullMockMode) {
            Location netLoc = createLocation(LocationManager.NETWORK_PROVIDER, jitterLat, jitterLon, 15.0f, 0.0);
            Location passLoc = createLocation(LocationManager.PASSIVE_PROVIDER, jitterLat, jitterLon, 5.0f, 0.0);
            try {
                locationManager.setTestProviderLocation(LocationManager.NETWORK_PROVIDER, netLoc);
            } catch (Throwable ignored) {
            }
            try {
                locationManager.setTestProviderLocation(LocationManager.PASSIVE_PROVIDER, passLoc);
            } catch (Throwable ignored) {
            }
        }
    }

    private Location createLocation(String provider, double lat, double lon, float accuracy, double altitude) {
        Location loc = new Location(provider);
        loc.setLatitude(lat);
        loc.setLongitude(lon);
        loc.setAccuracy(accuracy);
        loc.setAltitude(altitude);
        loc.setSpeed(0.0f);
        loc.setBearing(0.0f);
        loc.setTime(System.currentTimeMillis());
        loc.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            loc.setVerticalAccuracyMeters(2.0f);
            loc.setSpeedAccuracyMetersPerSecond(0.1f);
            loc.setBearingAccuracyDegrees(1.0f);
        }
        Bundle extras = new Bundle();
        extras.putInt("satellites", 11);
        loc.setExtras(extras);
        return loc;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Adblock Telemetry",
                    NotificationManager.IMPORTANCE_MIN
            );
            channel.setSound(null, null);
            channel.enableVibration(false);
            channel.setShowBadge(false);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) {
                nm.createNotificationChannel(channel);
            }
        }
    }

    private Notification buildNotification() {
        Intent openIntent = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(
                this,
                0,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("Adblock Active")
                .setOngoing(true)
                .setSilent(true)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setVisibility(NotificationCompat.VISIBILITY_SECRET)
                .setContentIntent(pi)
                .build();
    }

    @Override
    public void onDestroy() {
        stopMocking();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
