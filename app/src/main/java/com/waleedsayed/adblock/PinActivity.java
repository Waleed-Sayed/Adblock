package com.waleedsayed.adblock;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.waleedsayed.adblock.security.PatternView;
import com.waleedsayed.adblock.security.PinStore;
import java.util.List;

public class PinActivity extends AppCompatActivity {
    public static final String EXTRA_MODE = "mode";
    public static final String EXTRA_NEXT_VEHICLE_POSITION = "next_vehicle_position";
    public static final String MODE_VERIFY = "verify";
    public static final String MODE_SET = "set";
    public static final String MODE_CHANGE = "change";

    private PinStore pinStore;
    private String mode = MODE_VERIFY;
    private String authType = PinStore.MODE_PIN;
    private boolean openNext = false;

    private final StringBuilder currentPin = new StringBuilder();
    private String firstSecret = null;

    private TextView pinTitle;
    private TextView pinSubtitle;
    private View modeToggleRow;
    private Button btnModePin;
    private Button btnModePattern;
    private View pinContainer;
    private View patternContainer;
    private PatternView patternView;
    private final View[] dots = new View[4];

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE);
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin);

        pinStore = new PinStore(this);
        String argMode = getIntent().getStringExtra(EXTRA_MODE);
        if (argMode != null) {
            mode = argMode;
        }
        openNext = getIntent().getBooleanExtra(EXTRA_NEXT_VEHICLE_POSITION, false);

        if (MODE_VERIFY.equals(mode) && !pinStore.hasSecret()) {
            mode = MODE_SET;
        }
        authType = MODE_VERIFY.equals(mode) ? pinStore.getMode() : PinStore.MODE_PIN;

        pinTitle = findViewById(R.id.pinTitle);
        pinSubtitle = findViewById(R.id.pinSubtitle);
        modeToggleRow = findViewById(R.id.modeToggleRow);
        btnModePin = findViewById(R.id.btnModePin);
        btnModePattern = findViewById(R.id.btnModePattern);
        pinContainer = findViewById(R.id.pinContainer);
        patternContainer = findViewById(R.id.patternContainer);
        patternView = findViewById(R.id.patternView);

        dots[0] = findViewById(R.id.dot0);
        dots[1] = findViewById(R.id.dot1);
        dots[2] = findViewById(R.id.dot2);
        dots[3] = findViewById(R.id.dot3);

        int[] keyIds = {
                R.id.key0, R.id.key1, R.id.key2, R.id.key3, R.id.key4,
                R.id.key5, R.id.key6, R.id.key7, R.id.key8, R.id.key9
        };
        for (int i = 0; i < keyIds.length; i++) {
            final int digit = i;
            Button b = findViewById(keyIds[i]);
            b.setOnClickListener(v -> onDigitPressed(digit));
        }

        findViewById(R.id.keyDelete).setOnClickListener(v -> {
            if (currentPin.length() > 0) {
                currentPin.deleteCharAt(currentPin.length() - 1);
                updateDots();
            }
        });

        findViewById(R.id.keyClear).setOnClickListener(v -> {
            if (isSetupMode() && currentPin.length() >= 4) {
                handlePinSubmitted(currentPin.toString());
            } else {
                currentPin.setLength(0);
                updateDots();
            }
        });

        findViewById(R.id.pinCancel).setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });

        btnModePin.setOnClickListener(v -> switchAuthType(PinStore.MODE_PIN));
        btnModePattern.setOnClickListener(v -> switchAuthType(PinStore.MODE_PATTERN));
        patternView.setOnPatternListener(this::onPatternCompleted);

        updateUiState();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                setResult(RESULT_CANCELED);
                finish();
            }
        });
    }

    private boolean isSetupMode() {
        return MODE_SET.equals(mode) || MODE_CHANGE.equals(mode);
    }

    private void switchAuthType(String newType) {
        authType = newType;
        firstSecret = null;
        currentPin.setLength(0);
        patternView.clearPattern();
        updateUiState();
    }

    private void updateUiState() {
        if (isSetupMode()) {
            modeToggleRow.setVisibility(View.VISIBLE);
            pinTitle.setText(firstSecret == null ? R.string.pin_create : R.string.pin_confirm);
            pinSubtitle.setText(R.string.pin_hint_set);
        } else {
            modeToggleRow.setVisibility(View.GONE);
            pinTitle.setText(R.string.pin_enter);
            pinSubtitle.setText(R.string.pin_hint_verify);
        }

        boolean isPattern = PinStore.MODE_PATTERN.equals(authType);
        pinContainer.setVisibility(isPattern ? View.GONE : View.VISIBLE);
        patternContainer.setVisibility(isPattern ? View.VISIBLE : View.GONE);

        if (isPattern) {
            btnModePattern.setBackgroundResource(R.drawable.bg_primary_button);
            btnModePattern.setTextColor(ContextCompat.getColor(this, R.color.bg));
            btnModePin.setBackgroundResource(android.R.color.transparent);
            btnModePin.setTextColor(ContextCompat.getColor(this, R.color.text_muted));
        } else {
            btnModePin.setBackgroundResource(R.drawable.bg_primary_button);
            btnModePin.setTextColor(ContextCompat.getColor(this, R.color.bg));
            btnModePattern.setBackgroundResource(android.R.color.transparent);
            btnModePattern.setTextColor(ContextCompat.getColor(this, R.color.text_muted));
        }

        Button keyClear = findViewById(R.id.keyClear);
        if (keyClear != null) {
            keyClear.setText(isSetupMode() ? R.string.save : R.string.pin_clear);
        }
        updateDots();
    }

    private void redirectToDeviceSettings() {
        try {
            Intent settingsIntent = new Intent(android.provider.Settings.ACTION_SETTINGS);
            settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(settingsIntent);
        } catch (Throwable ignored) {
        }
        setResult(RESULT_CANCELED);
        finish();
        overridePendingTransition(0, 0);
    }

    private void onDigitPressed(int digit) {
        if (currentPin.length() >= 8) {
            return;
        }
        currentPin.append(digit);
        updateDots();

        if (!isSetupMode()) {
            int targetLen = pinStore.getPinLength();
            if (currentPin.length() >= targetLen) {
                String candidate = currentPin.toString();
                if (pinStore.verifyPin(candidate)) {
                    onAuthSuccess();
                } else {
                    redirectToDeviceSettings();
                }
            }
        } else if (firstSecret != null && currentPin.length() == firstSecret.length()) {
            handlePinSubmitted(currentPin.toString());
        }
    }

    private void handlePinSubmitted(String entered) {
        if (entered.length() < 4) {
            return;
        }
        if (firstSecret == null) {
            firstSecret = entered;
            currentPin.setLength(0);
            updateUiState();
        } else if (firstSecret.equals(entered)) {
            if (pinStore.savePin(entered)) {
                Toast.makeText(this, R.string.pin_saved, Toast.LENGTH_SHORT).show();
                onAuthSuccess();
            } else {
                Toast.makeText(this, R.string.pin_save_error, Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, R.string.pin_mismatch, Toast.LENGTH_SHORT).show();
            firstSecret = null;
            currentPin.setLength(0);
            updateUiState();
        }
    }

    private void onPatternCompleted(List<Integer> nodes) {
        if (!isSetupMode()) {
            if (nodes == null || nodes.size() < 4) {
                redirectToDeviceSettings();
                return;
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < nodes.size(); i++) {
                if (i > 0) sb.append('-');
                sb.append(nodes.get(i));
            }
            if (pinStore.verifyPattern(sb.toString())) {
                onAuthSuccess();
            } else {
                redirectToDeviceSettings();
            }
            return;
        }

        if (nodes == null || nodes.size() < 4) {
            patternView.showError();
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < nodes.size(); i++) {
            if (i > 0) sb.append('-');
            sb.append(nodes.get(i));
        }
        String patternStr = sb.toString();

        if (firstSecret == null) {
            firstSecret = patternStr;
            patternView.clearPattern();
            updateUiState();
        } else if (firstSecret.equals(patternStr)) {
            if (pinStore.savePattern(patternStr)) {
                Toast.makeText(this, R.string.pin_saved, Toast.LENGTH_SHORT).show();
                onAuthSuccess();
            } else {
                Toast.makeText(this, R.string.pin_save_error, Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, R.string.pin_mismatch, Toast.LENGTH_SHORT).show();
            firstSecret = null;
            patternView.showError();
            updateUiState();
        }
    }

    private void onAuthSuccess() {
        if (openNext) {
            Intent intent = new Intent(this, VehiclePositionActivity.class);
            startActivity(intent);
        }
        setResult(RESULT_OK);
        finish();
    }

    private void updateDots() {
        for (int i = 0; i < dots.length; i++) {
            if (dots[i] != null) {
                dots[i].setBackgroundResource(R.drawable.pin_dot);
            }
        }
    }
}
