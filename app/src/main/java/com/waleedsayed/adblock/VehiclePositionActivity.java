package com.waleedsayed.adblock;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class VehiclePositionActivity extends AppCompatActivity {
    private static final String PREFS_NAME = "obd2_vehicle_pos";
    private static final String KEY_LAT = "last_lat";
    private static final String KEY_LON = "last_lon";
    private static final String KEY_LABEL = "last_label";
    private static final String KEY_FULL_MOCK = "full_mock_mode";

    private static final int REQ_PICK_MAP = 201;
    private static final int REQ_EXPORT_FILE = 301;
    private static final int REQ_IMPORT_FILE = 302;

    private SharedPreferences prefs;
    private FavoritesStore favoritesStore;

    private View statusCard;
    private View statusDot;
    private TextView mockStatus;
    private TextView mockDetails;
    private RadioGroup rgNavMode;
    private RadioButton rbRealNavMode;
    private RadioButton rbFullMockMode;
    private EditText inputLat;
    private EditText inputLon;
    private TextView favoriteName;

    private String pendingExportJson = null;
    private Runnable onFavoritesChangedCallback = null;
    private boolean isLaunchingChildActivity = false;

    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private final Runnable statusPoller = new Runnable() {
        @Override
        public void run() {
            refreshStatusUi();
            uiHandler.postDelayed(this, 800L);
        }
    };

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE);
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vehicle_position);

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        favoritesStore = new FavoritesStore(this);

        statusCard = findViewById(R.id.statusCard);
        statusDot = findViewById(R.id.statusDot);
        mockStatus = findViewById(R.id.mockStatus);
        mockDetails = findViewById(R.id.mockDetails);
        rgNavMode = findViewById(R.id.rgNavMode);
        rbRealNavMode = findViewById(R.id.rbRealNavMode);
        rbFullMockMode = findViewById(R.id.rbFullMockMode);
        inputLat = findViewById(R.id.inputLat);
        inputLon = findViewById(R.id.inputLon);
        favoriteName = findViewById(R.id.favoriteName);

        String savedLat = prefs.getString(KEY_LAT, "24.7136");
        String savedLon = prefs.getString(KEY_LON, "46.6753");
        String savedLabel = prefs.getString(KEY_LABEL, "");
        boolean fullMock = prefs.getBoolean(KEY_FULL_MOCK, false);

        inputLat.setText(savedLat);
        inputLon.setText(savedLon);
        favoriteName.setText(savedLabel.isEmpty() ? getString(R.string.favorite_none) : savedLabel);
        if (fullMock) {
            rbFullMockMode.setChecked(true);
        } else {
            rbRealNavMode.setChecked(true);
        }

        rgNavMode.setOnCheckedChangeListener((group, checkedId) -> {
            boolean isFull = checkedId == R.id.rbFullMockMode;
            prefs.edit().putBoolean(KEY_FULL_MOCK, isFull).apply();
            Intent modeIntent = new Intent(this, VehiclePositionService.class);
            modeIntent.setAction(VehiclePositionService.ACTION_SET_MODE);
            modeIntent.putExtra(VehiclePositionService.EXTRA_FULL_MOCK, isFull);
            startService(modeIntent);
        });

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnChangePin).setOnClickListener(v -> {
            Intent intent = new Intent(this, PinActivity.class);
            intent.putExtra(PinActivity.EXTRA_MODE, PinActivity.MODE_CHANGE);
            isLaunchingChildActivity = true;
            startActivity(intent);
        });

        findViewById(R.id.btnPickTarget).setOnClickListener(v -> {
            double[] coords = parseInputsOrDefault();
            Intent intent = new Intent(this, PositionPickerActivity.class);
            intent.putExtra(PositionPickerActivity.EXTRA_INITIAL_LAT, coords[0]);
            intent.putExtra(PositionPickerActivity.EXTRA_INITIAL_LON, coords[1]);
            isLaunchingChildActivity = true;
            startActivityForResult(intent, REQ_PICK_MAP);
        });

        findViewById(R.id.btnSaveFavorite).setOnClickListener(v -> showSaveFavoriteDialog());
        findViewById(R.id.btnFavorites).setOnClickListener(v -> showFavoritesDialog());

        findViewById(R.id.btnStart).setOnClickListener(v -> startOrSwitchMock(false));
        findViewById(R.id.btnSwitch).setOnClickListener(v -> startOrSwitchMock(true));
        findViewById(R.id.btnStop).setOnClickListener(v -> {
            Intent stopIntent = new Intent(this, VehiclePositionService.class);
            stopIntent.setAction(VehiclePositionService.ACTION_STOP);
            startService(stopIntent);
            uiHandler.postDelayed(this::refreshStatusUi, 150L);
        });

        findViewById(R.id.btnCreateShortcut).setOnClickListener(v -> {
            Intent target = new Intent(this, StopMockActivity.class);
            target.setAction(Intent.ACTION_VIEW);
            target.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            showCustomizeShortcutDialog("stop_" + System.currentTimeMillis(), getString(R.string.shortcut_wifi), target);
        });

        findViewById(R.id.btnCreateSwitchShortcut).setOnClickListener(v -> showSelectFavoriteForSwitchShortcut());

        findViewById(R.id.btnCreateResetShortcut).setOnClickListener(v -> {
            Intent target = new Intent(this, ResetMockActionActivity.class);
            target.setAction(Intent.ACTION_VIEW);
            target.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            showCustomizeShortcutDialog("reset_" + System.currentTimeMillis(), getString(R.string.create_reset_shortcut), target);
        });

        findViewById(R.id.btnCreateSetShortcut).setOnClickListener(v -> {
            Intent target = new Intent(this, SetMockActionActivity.class);
            target.setAction(Intent.ACTION_VIEW);
            target.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            showCustomizeShortcutDialog("set_" + System.currentTimeMillis(), getString(R.string.create_set_shortcut), target);
        });

        findViewById(R.id.btnDevOptions).setOnClickListener(v -> MockLocationHelper.openDeveloperOptions(this));
    }

    private double[] parseInputsOrDefault() {
        double lat = 24.7136;
        double lon = 46.6753;
        try {
            lat = Double.parseDouble(inputLat.getText().toString().trim());
            lon = Double.parseDouble(inputLon.getText().toString().trim());
        } catch (Throwable ignored) {
        }
        return new double[]{lat, lon};
    }

    private Double[] parseValidInputs() {
        try {
            double lat = Double.parseDouble(inputLat.getText().toString().trim());
            double lon = Double.parseDouble(inputLon.getText().toString().trim());
            if (Double.isNaN(lat) || Double.isNaN(lon) || lat < -90.0 || lat > 90.0 || lon < -180.0 || lon > 180.0) {
                return null;
            }
            return new Double[]{lat, lon};
        } catch (Throwable t) {
            return null;
        }
    }

    private void startOrSwitchMock(boolean isSwitch) {
        Double[] coords = parseValidInputs();
        if (coords == null) {
            Toast.makeText(this, R.string.pos_bad_number, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!MockLocationHelper.isMockLocationEnabled(this)) {
            showNeedMockAppDialog();
            return;
        }
        double lat = coords[0];
        double lon = coords[1];
        FavoritesStore.FavoriteItem near = favoritesStore.findNear(lat, lon);
        String label = near != null ? near.name : getString(R.string.favorite_custom);

        prefs.edit()
                .putString(KEY_LAT, String.format(Locale.US, "%.6f", lat))
                .putString(KEY_LON, String.format(Locale.US, "%.6f", lon))
                .putString(KEY_LABEL, label)
                .apply();
        favoriteName.setText(label);

        Intent svc = new Intent(this, VehiclePositionService.class);
        svc.setAction(isSwitch ? VehiclePositionService.ACTION_SWITCH : VehiclePositionService.ACTION_START);
        svc.putExtra(VehiclePositionService.EXTRA_LAT, lat);
        svc.putExtra(VehiclePositionService.EXTRA_LON, lon);
        svc.putExtra(VehiclePositionService.EXTRA_LABEL, label);
        svc.putExtra(VehiclePositionService.EXTRA_FULL_MOCK, rbFullMockMode.isChecked());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(svc);
        } else {
            startService(svc);
        }
        uiHandler.postDelayed(this::refreshStatusUi, 150L);
    }

    private void showNeedMockAppDialog() {
        Dialog d = new Dialog(this);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        d.setContentView(R.layout.dialog_sheet);
        if (d.getWindow() != null) {
            d.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            d.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        TextView title = d.findViewById(R.id.sheetTitle);
        TextView msg = d.findViewById(R.id.sheetMessage);
        Button pos = d.findViewById(R.id.sheetPositive);
        Button neg = d.findViewById(R.id.sheetNegative);

        title.setText(R.string.pos_need_app_title);
        msg.setText(R.string.pos_need_app_body);
        msg.setVisibility(View.VISIBLE);
        pos.setText(R.string.pos_open_dev);
        pos.setOnClickListener(v -> {
            d.dismiss();
            MockLocationHelper.openDeveloperOptions(this);
        });
        neg.setOnClickListener(v -> d.dismiss());
        d.show();
    }

    private void showSaveFavoriteDialog() {
        Double[] coords = parseValidInputs();
        if (coords == null) {
            Toast.makeText(this, R.string.pos_bad_number, Toast.LENGTH_SHORT).show();
            return;
        }
        if (favoritesStore.getAll().size() >= FavoritesStore.MAX_FAVORITES) {
            Toast.makeText(this, R.string.favorites_full, Toast.LENGTH_SHORT).show();
            return;
        }

        Dialog d = new Dialog(this);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        d.setContentView(R.layout.dialog_sheet);
        if (d.getWindow() != null) {
            d.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            d.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        TextView title = d.findViewById(R.id.sheetTitle);
        EditText input = d.findViewById(R.id.sheetInput);
        Button pos = d.findViewById(R.id.sheetPositive);
        Button neg = d.findViewById(R.id.sheetNegative);

        title.setText(R.string.save_favorite);
        input.setVisibility(View.VISIBLE);
        input.setHint(R.string.favorite_name_hint);
        String defaultName = getString(R.string.favorite_default_name, favoritesStore.getAll().size() + 1);
        input.setText(defaultName);
        input.selectAll();

        pos.setOnClickListener(v -> {
            String name = input.getText().toString().trim();
            if (name.isEmpty()) {
                name = defaultName;
            }
            FavoritesStore.FavoriteItem saved = favoritesStore.add(name, coords[0], coords[1]);
            if (saved != null) {
                favoriteName.setText(saved.name);
                prefs.edit().putString(KEY_LABEL, saved.name).apply();
                Toast.makeText(this, getString(R.string.favorite_saved, saved.name), Toast.LENGTH_SHORT).show();
            }
            d.dismiss();
        });
        neg.setOnClickListener(v -> d.dismiss());
        d.show();
    }

    private void showFavoritesDialog() {
        Dialog d = new Dialog(this, R.style.Theme_OBD2_FullscreenDialog);
        d.setContentView(R.layout.dialog_favorites);

        TextView countView = d.findViewById(R.id.favoritesCount);
        ListView listView = d.findViewById(R.id.favoritesList);
        d.findViewById(R.id.btnCloseFavorites).setOnClickListener(v -> d.dismiss());

        final List<FavoritesStore.FavoriteItem> items = new ArrayList<>();
        final BaseAdapter adapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return items.size();
            }

            @Override
            public FavoritesStore.FavoriteItem getItem(int position) {
                return items.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View row = convertView != null ? convertView
                        : LayoutInflater.from(VehiclePositionActivity.this).inflate(R.layout.favorite_item, parent, false);
                FavoritesStore.FavoriteItem item = getItem(position);
                TextView tvName = row.findViewById(R.id.favItemName);
                TextView tvCoords = row.findViewById(R.id.favItemCoords);
                View btnDel = row.findViewById(R.id.btnDeleteFav);

                tvName.setText(item.name);
                tvCoords.setText(String.format(Locale.US, "%.5f, %.5f", item.lat, item.lon));

                row.setOnClickListener(v -> {
                    applyFavoriteItem(item);
                    d.dismiss();
                });
                btnDel.setOnClickListener(v -> {
                    favoritesStore.remove(item.id);
                    Toast.makeText(VehiclePositionActivity.this, R.string.favorite_deleted, Toast.LENGTH_SHORT).show();
                    if (onFavoritesChangedCallback != null) {
                        onFavoritesChangedCallback.run();
                    }
                });
                return row;
            }
        };

        listView.setAdapter(adapter);

        onFavoritesChangedCallback = () -> {
            items.clear();
            items.addAll(favoritesStore.getAll());
            countView.setText(getString(R.string.favorites_count, items.size(), FavoritesStore.MAX_FAVORITES));
            adapter.notifyDataSetChanged();
        };
        onFavoritesChangedCallback.run();

        d.findViewById(R.id.btnFavSelect).setOnClickListener(v -> {
            if (!items.isEmpty()) {
                applyFavoriteItem(items.get(0));
                d.dismiss();
            } else {
                Toast.makeText(this, R.string.favorites_empty, Toast.LENGTH_SHORT).show();
            }
        });

        d.findViewById(R.id.btnFavShare).setOnClickListener(v -> shareFavoritesJson());
        d.findViewById(R.id.btnFavExport).setOnClickListener(v -> copyFavoritesToClipboard());
        d.findViewById(R.id.btnFavSaveDevice).setOnClickListener(v -> saveFavoritesToDeviceFile());
        d.findViewById(R.id.btnFavImport).setOnClickListener(v -> showImportFavoritesDialog());

        d.show();
    }

    private void applyFavoriteItem(FavoritesStore.FavoriteItem item) {
        inputLat.setText(String.format(Locale.US, "%.6f", item.lat));
        inputLon.setText(String.format(Locale.US, "%.6f", item.lon));
        favoriteName.setText(item.name);
        prefs.edit()
                .putString(KEY_LAT, String.format(Locale.US, "%.6f", item.lat))
                .putString(KEY_LON, String.format(Locale.US, "%.6f", item.lon))
                .putString(KEY_LABEL, item.name)
                .apply();
        Toast.makeText(this, getString(R.string.favorite_loaded, item.name), Toast.LENGTH_SHORT).show();
    }

    private void shareFavoritesJson() {
        try {
            File file = new File(getCacheDir(), "obd2_favorites.json");
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(favoritesStore.exportJson().getBytes(StandardCharsets.UTF_8));
            }
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType("application/json");
            send.putExtra(Intent.EXTRA_STREAM, uri);
            send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            isLaunchingChildActivity = true;
            startActivity(Intent.createChooser(send, getString(R.string.fav_share)));
        } catch (Throwable t) {
            copyFavoritesToClipboard();
        }
    }

    private void copyFavoritesToClipboard() {
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) {
            cm.setPrimaryClip(ClipData.newPlainText("favorites", favoritesStore.exportJson()));
            Toast.makeText(this, "تم نسخ المواقع إلى الحافظة", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveFavoritesToDeviceFile() {
        pendingExportJson = favoritesStore.exportJson();
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE, "obd2_favorites.json");
        isLaunchingChildActivity = true;
        startActivityForResult(intent, REQ_EXPORT_FILE);
    }

    private void showImportFavoritesDialog() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        isLaunchingChildActivity = true;
        startActivityForResult(intent, REQ_IMPORT_FILE);
    }

    private void showSelectFavoriteForSwitchShortcut() {
        List<FavoritesStore.FavoriteItem> favs = favoritesStore.getAll();
        if (favs.isEmpty()) {
            Toast.makeText(this, R.string.favorites_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        Dialog d = new Dialog(this);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        d.setContentView(R.layout.dialog_sheet);
        if (d.getWindow() != null) {
            d.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            d.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        TextView title = d.findViewById(R.id.sheetTitle);
        ListView list = d.findViewById(R.id.sheetList);
        Button pos = d.findViewById(R.id.sheetPositive);
        Button neg = d.findViewById(R.id.sheetNegative);

        title.setText(R.string.shortcut_select_location);
        pos.setVisibility(View.GONE);
        list.setVisibility(View.VISIBLE);

        List<String> names = new ArrayList<>();
        for (FavoritesStore.FavoriteItem f : favs) {
            names.add(f.name);
        }
        list.setAdapter(new ArrayAdapter<>(this, R.layout.sheet_item, names));
        list.setOnItemClickListener((parent, view, position, id) -> {
            d.dismiss();
            FavoritesStore.FavoriteItem chosen = favs.get(position);
            Intent target = new Intent(this, SwitchMockActivity.class);
            target.setAction(Intent.ACTION_VIEW);
            target.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            target.putExtra("lat", chosen.lat);
            target.putExtra("lon", chosen.lon);
            target.putExtra("label", chosen.name);
            showCustomizeShortcutDialog("switch_" + chosen.id, chosen.name, target);
        });
        neg.setOnClickListener(v -> d.dismiss());
        d.show();
    }

    private void showCustomizeShortcutDialog(String shortcutId, String defaultLabel, Intent targetIntent) {
        Dialog d = new Dialog(this);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        d.setContentView(R.layout.dialog_customize_shortcut);
        if (d.getWindow() != null) {
            d.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            d.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        EditText etName = d.findViewById(R.id.etShortcutName);
        RecyclerView rvIcons = d.findViewById(R.id.rvSettingsIcons);
        Button btnConfirm = d.findViewById(R.id.btnConfirmAddShortcut);

        etName.setText(defaultLabel);

        List<ShortcutIconAdapter.IconOption> options = buildIconOptions();
        ShortcutIconAdapter adapter = new ShortcutIconAdapter(options);
        rvIcons.setLayoutManager(new GridLayoutManager(this, 4));
        rvIcons.setAdapter(adapter);

        btnConfirm.setOnClickListener(v -> {
            String label = etName.getText().toString().trim();
            if (label.isEmpty()) {
                label = "\u200E";
            }
            ShortcutIconAdapter.IconOption selected = adapter.getSelectedOption();
            IconCompat iconCompat;
            if (selected == null || selected.isTransparent || selected.drawable == null) {
                Bitmap transparentBmp = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
                transparentBmp.eraseColor(Color.TRANSPARENT);
                iconCompat = IconCompat.createWithBitmap(transparentBmp);
            } else {
                Bitmap fullBleedBmp = createIconBitmap(selected);
                iconCompat = IconCompat.createWithAdaptiveBitmap(fullBleedBmp);
            }

            if (ShortcutManagerCompat.isRequestPinShortcutSupported(this)) {
                ShortcutInfoCompat info = new ShortcutInfoCompat.Builder(this, shortcutId)
                        .setShortLabel(label)
                        .setLongLabel(label)
                        .setIcon(iconCompat)
                        .setIntent(targetIntent)
                        .build();
                ShortcutManagerCompat.requestPinShortcut(this, info, null);
            }
            d.dismiss();
        });

        d.show();
    }

    private List<ShortcutIconAdapter.IconOption> buildIconOptions() {
        List<ShortcutIconAdapter.IconOption> list = new ArrayList<>();
        list.add(new ShortcutIconAdapter.IconOption("شفاف (مخفي)", null, true));

        int[][] pairs = {
                {R.drawable.ic_opt_dns_shield, 0},
                {R.drawable.ic_opt_stop_ads, 1},
                {R.drawable.ic_opt_privacy_eye, 2},
                {R.drawable.ic_opt_lightning_boost, 3},
                {R.drawable.ic_opt_clean_broom, 4},
                {R.drawable.ic_opt_firewall, 5},
                {R.drawable.ic_opt_vpn_key, 6},
                {R.drawable.ic_opt_malware_bug, 7},
                {R.drawable.ic_opt_tracker_ghost, 8},
                {R.drawable.ic_opt_popup_block, 9},
                {R.drawable.ic_opt_cookie_shield, 10},
                {R.drawable.ic_opt_safe_web, 11},
                {R.drawable.ic_opt_network_signal, 12},
                {R.drawable.ic_opt_battery_saver, 13},
                {R.drawable.ic_opt_speed_rocket, 14},
                {R.drawable.ic_opt_lock_vault, 15},
                {R.drawable.ic_opt_radar, 16},
                {R.drawable.ic_opt_fingerprint_block, 17},
                {R.drawable.ic_opt_dns_server, 18},
                {R.drawable.ic_opt_ad_dustbin, 19},
                {R.drawable.ic_opt_video_ad_block, 20},
                {R.drawable.ic_opt_banner_clean, 21},
                {R.drawable.ic_opt_ip_shield, 22},
                {R.drawable.ic_opt_algorithm_reset, 23}
        };
        String[] labels = {
                "درع DNS", "إيقاف الإعلانات", "حماية الخصوصية", "تسريع فائق",
                "تنظيف شامل", "جدار ناري", "مفتاح VPN", "حماية البرمجيات",
                "منع التتبع", "حظر النوافذ", "حماية الكوكيز", "تصفح آمن",
                "إشارة الشبكة", "توفير البطارية", "صاروخ السرعة", "خزنة القفل",
                "رادار الفحص", "حماية البصمة", "خادم DNS", "سلة المحذوفات",
                "حظر إعلانات الفيديو", "تنظيف البنرات", "حماية IP", "إعادة الضبط"
        };
        for (int i = 0; i < pairs.length; i++) {
            Drawable d = ContextCompat.getDrawable(this, pairs[i][0]);
            list.add(new ShortcutIconAdapter.IconOption(labels[i], d, false));
        }

        // Add official OEM brand icons
        List<OemIconItem> oemIcons = OemIconFactory.createAllIcons(this);
        for (OemIconItem oem : oemIcons) {
            String label = oem.name + " (" + oem.brandName.split(" ")[0] + ")";
            list.add(new ShortcutIconAdapter.IconOption(label, oem.drawable, false));
        }

        return list;
    }

    private Bitmap createIconBitmap(ShortcutIconAdapter.IconOption option) {
        if (option == null || option.isTransparent || option.drawable == null) {
            Bitmap empty = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
            empty.eraseColor(Color.TRANSPARENT);
            return empty;
        }

        // رسم الأيقونة مؤقتاً لاستخراج لون خلفيتها الدائرية الداكن الأصلي
        int sampleSize = 96;
        Bitmap sampleBmp = Bitmap.createBitmap(sampleSize, sampleSize, Bitmap.Config.ARGB_8888);
        Canvas sampleCanvas = new Canvas(sampleBmp);
        option.drawable.setBounds(0, 0, sampleSize, sampleSize);
        option.drawable.draw(sampleCanvas);

        // النقطة (48, 12) تقع دائماً داخل الدائرة الخلفية الداكنة لجميع أيقونات ic_opt_*
        int bgColor = sampleBmp.getPixel(48, 12);
        if (Color.alpha(bgColor) < 200) {
            bgColor = Color.parseColor("#161922");
        }
        sampleBmp.recycle();

        // إنشاء Bitmap بحجم كامل (108dp * 2.66 = 288px) وملؤه بالكامل بلون خلفية الأيقونة نفسها
        int size = 288;
        Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        canvas.drawColor(bgColor);

        // رسم الأيقونة في المنطقة الآمنة بالمنتصف لتندمج خلفيتها مع اللون الكامل بدون أي حواف بيضاء
        int inset = (int) (size * 0.14f);
        option.drawable.setBounds(inset, inset, size - inset, size - inset);
        option.drawable.draw(canvas);
        return bmp;
    }

    private void refreshStatusUi() {
        if (VehiclePositionService.isRunning) {
            statusCard.setBackgroundResource(R.drawable.bg_status_running);
            statusDot.setBackgroundResource(R.drawable.dot_running);
            mockStatus.setText(R.string.pos_running);
            mockStatus.setTextColor(ContextCompat.getColor(this, R.color.primary));
            String lbl = VehiclePositionService.currentLabel;
            if (lbl == null || lbl.isEmpty()) {
                lbl = String.format(Locale.US, "%.4f, %.4f", VehiclePositionService.currentLat, VehiclePositionService.currentLon);
            }
            mockDetails.setText(getString(R.string.pos_details_running, lbl, VehiclePositionService.updateCount));
        } else {
            statusCard.setBackgroundResource(R.drawable.bg_status_stopped);
            statusDot.setBackgroundResource(R.drawable.dot_stopped);
            mockStatus.setText(R.string.pos_status_idle);
            mockStatus.setTextColor(ContextCompat.getColor(this, R.color.danger));
            mockDetails.setText("");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_PICK_MAP && resultCode == RESULT_OK && data != null) {
            double lat = data.getDoubleExtra(PositionPickerActivity.EXTRA_PICKED_LAT, 24.7136);
            double lon = data.getDoubleExtra(PositionPickerActivity.EXTRA_PICKED_LON, 46.6753);
            inputLat.setText(String.format(Locale.US, "%.6f", lat));
            inputLon.setText(String.format(Locale.US, "%.6f", lon));
            String label = getString(R.string.favorite_from_map);
            favoriteName.setText(label);
            prefs.edit()
                    .putString(KEY_LAT, String.format(Locale.US, "%.6f", lat))
                    .putString(KEY_LON, String.format(Locale.US, "%.6f", lon))
                    .putString(KEY_LABEL, label)
                    .apply();
            Toast.makeText(this, R.string.map_picked, Toast.LENGTH_SHORT).show();
        } else if (requestCode == REQ_EXPORT_FILE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            if (pendingExportJson != null) {
                try (OutputStream os = getContentResolver().openOutputStream(data.getData())) {
                    if (os != null) {
                        os.write(pendingExportJson.getBytes(StandardCharsets.UTF_8));
                        Toast.makeText(this, "تم حفظ الملف بنجاح", Toast.LENGTH_SHORT).show();
                    }
                } catch (Throwable t) {
                    Toast.makeText(this, "فشل حفظ الملف", Toast.LENGTH_SHORT).show();
                }
            }
        } else if (requestCode == REQ_IMPORT_FILE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            try (InputStream is = getContentResolver().openInputStream(data.getData());
                 BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                int count = favoritesStore.importJson(sb.toString());
                if (count >= 0) {
                    Toast.makeText(this, "تم استيراد " + count + " موقع", Toast.LENGTH_SHORT).show();
                    if (onFavoritesChangedCallback != null) {
                        onFavoritesChangedCallback.run();
                    }
                } else {
                    Toast.makeText(this, "ملف غير صالح", Toast.LENGTH_SHORT).show();
                }
            } catch (Throwable t) {
                Toast.makeText(this, "تعذر قراءة الملف", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        uiHandler.post(statusPoller);
    }

    @Override
    protected void onPause() {
        super.onPause();
        uiHandler.removeCallbacks(statusPoller);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (!isLaunchingChildActivity && !isChangingConfigurations()) {
            finish();
        }
        isLaunchingChildActivity = false;
    }
}
