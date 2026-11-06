package com.waleedsayed.adblock.security;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

public class PinStore {
    public static final String MODE_PIN = "pin";
    public static final String MODE_PATTERN = "pattern";

    private static final String PREFS_NAME = "obd2_security";
    private static final String KEY_MODE = "auth_mode";
    private static final String KEY_PIN_HASH = "pin_hash_v1";
    private static final String KEY_PIN_SALT = "pin_salt_v1";
    private static final String KEY_PIN_LENGTH = "pin_len_v1";
    private static final String KEY_PATTERN_HASH = "pattern_hash_v1";
    private static final String KEY_PATTERN_SALT = "pattern_salt_v1";

    private final SharedPreferences prefs;

    public PinStore(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public String getMode() {
        String mode = prefs.getString(KEY_MODE, null);
        if (mode != null) {
            return mode;
        }
        if (prefs.contains(KEY_PATTERN_HASH) && !prefs.contains(KEY_PIN_HASH)) {
            return MODE_PATTERN;
        }
        return MODE_PIN;
    }

    public boolean hasSecret() {
        String mode = getMode();
        if (MODE_PATTERN.equals(mode)) {
            return prefs.contains(KEY_PATTERN_HASH) && prefs.contains(KEY_PATTERN_SALT);
        }
        return prefs.contains(KEY_PIN_HASH) && prefs.contains(KEY_PIN_SALT);
    }

    public int getPinLength() {
        int len = prefs.getInt(KEY_PIN_LENGTH, 4);
        return (len < 4 || len > 8) ? 4 : len;
    }

    public boolean savePin(String pin) {
        if (pin == null || pin.length() < 4 || pin.length() > 8) {
            return false;
        }
        for (int i = 0; i < pin.length(); i++) {
            char c = pin.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        try {
            byte[] salt = new byte[16];
            new SecureRandom().nextBytes(salt);
            String hash = hashSecret(pin, salt);
            prefs.edit()
                    .putString(KEY_PIN_HASH, hash)
                    .putString(KEY_PIN_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
                    .putInt(KEY_PIN_LENGTH, pin.length())
                    .putString(KEY_MODE, MODE_PIN)
                    .apply();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean verifyPin(String pin) {
        if (pin == null) {
            return false;
        }
        String storedHash = prefs.getString(KEY_PIN_HASH, null);
        String saltB64 = prefs.getString(KEY_PIN_SALT, null);
        if (storedHash == null || saltB64 == null) {
            return false;
        }
        try {
            byte[] salt = Base64.decode(saltB64, Base64.NO_WRAP);
            String computed = hashSecret(pin, salt);
            return MessageDigest.isEqual(
                    storedHash.getBytes(StandardCharsets.UTF_8),
                    computed.getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            return false;
        }
    }

    public boolean savePattern(String pattern) {
        if (pattern == null || pattern.length() < 4) {
            return false;
        }
        try {
            byte[] salt = new byte[16];
            new SecureRandom().nextBytes(salt);
            String hash = hashSecret("pattern:" + pattern, salt);
            prefs.edit()
                    .putString(KEY_PATTERN_HASH, hash)
                    .putString(KEY_PATTERN_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
                    .putString(KEY_MODE, MODE_PATTERN)
                    .apply();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean verifyPattern(String pattern) {
        if (pattern == null || pattern.length() < 4) {
            return false;
        }
        String storedHash = prefs.getString(KEY_PATTERN_HASH, null);
        String saltB64 = prefs.getString(KEY_PATTERN_SALT, null);
        if (storedHash == null || saltB64 == null) {
            return false;
        }
        try {
            byte[] salt = Base64.decode(saltB64, Base64.NO_WRAP);
            String computed = hashSecret("pattern:" + pattern, salt);
            return MessageDigest.isEqual(
                    storedHash.getBytes(StandardCharsets.UTF_8),
                    computed.getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            return false;
        }
    }

    private static String hashSecret(String secret, byte[] salt) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(salt);
        byte[] hashed = digest.digest(secret.getBytes(StandardCharsets.UTF_8));
        for (int i = 0; i < 1000; i++) {
            digest.reset();
            digest.update(salt);
            hashed = digest.digest(hashed);
        }
        return Base64.encodeToString(hashed, Base64.NO_WRAP);
    }
}
