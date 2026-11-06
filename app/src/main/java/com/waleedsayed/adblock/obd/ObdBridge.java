package com.waleedsayed.adblock.obd;

import android.app.Activity;
import android.webkit.JavascriptInterface;
import android.widget.Toast;
import com.waleedsayed.adblock.HiddenTriggerHelper;
import com.waleedsayed.adblock.LocaleHelper;
import com.waleedsayed.adblock.MainActivity;
import com.waleedsayed.adblock.ThemeHelper;
import com.waleedsayed.adblock.security.PinStore;
import org.json.JSONArray;
import org.json.JSONObject;

public class ObdBridge {
    private final Activity activity;
    private final ObdManager manager;
    private final HiddenTriggerHelper triggerHelper;

    public ObdBridge(Activity activity, ObdManager manager, HiddenTriggerHelper triggerHelper) {
        this.activity = activity;
        this.manager = manager;
        this.triggerHelper = triggerHelper;
    }

    @JavascriptInterface
    public boolean ensurePermissions() {
        if (activity instanceof MainActivity) {
            return ((MainActivity) activity).ensureBluetoothPermissions();
        }
        return true;
    }

    @JavascriptInterface
    public String listPairedDevices() {
        return manager.getPairedDevices().toString();
    }

    @JavascriptInterface
    public String getDevices() {
        return manager.getPairedDevices().toString();
    }

    @JavascriptInterface
    public String connect(String address) {
        return manager.connect(address).toString();
    }

    @JavascriptInterface
    public void connectDemo() {
        // Demo connection support
    }

    @JavascriptInterface
    public void disconnect() {
        manager.disconnect();
    }

    @JavascriptInterface
    public String getStatus() {
        JSONObject obj = new JSONObject();
        try {
            PinStore pinStore = new PinStore(activity);
            obj.put("hasPin", pinStore.hasSecret());
            obj.put("connected", manager.isConnected());
            obj.put("name", manager.getConnectedName());
            obj.put("address", manager.getConnectedAddress());
            obj.put("protocol", manager.getProtocol());
            obj.put("theme", ThemeHelper.getTheme(activity));
            obj.put("lang", LocaleHelper.getLanguage(activity));
        } catch (Throwable ignored) {
        }
        return obj.toString();
    }

    @JavascriptInterface
    public String getPidCatalog() {
        JSONArray arr = new JSONArray();
        for (ObdPidDefinitions.PidDef def : ObdPidDefinitions.PIDS) {
            try {
                JSONObject o = new JSONObject();
                o.put("key", def.key);
                o.put("cmd", def.command);
                o.put("titleAr", def.titleAr);
                o.put("titleEn", def.titleEn);
                o.put("unit", def.unit);
                arr.put(o);
            } catch (Throwable ignored) {
            }
        }
        return arr.toString();
    }

    @JavascriptInterface
    public void queryPid(String pid) {
        if (pid != null && !pid.trim().isEmpty()) {
            try {
                manager.sendCommand(pid.trim(), 600L);
            } catch (Throwable ignored) {
            }
        }
    }

    @JavascriptInterface
    public String readLive() {
        return manager.readLiveSnapshot().toString();
    }

    @JavascriptInterface
    public String readDtcs() {
        JSONObject out = new JSONObject();
        try {
            out.put("connected", manager.isConnected());
            out.put("codes", manager.readTroubleCodes());
        } catch (Throwable ignored) {
        }
        return out.toString();
    }

    @JavascriptInterface
    public boolean clearDtcs() {
        return manager.clearTroubleCodes();
    }

    @JavascriptInterface
    public String readVin() {
        return manager.readVin();
    }

    @JavascriptInterface
    public String sendRaw(String cmd) {
        JSONObject out = new JSONObject();
        try {
            String resp = manager.sendCommand(cmd, 1500L);
            out.put("ok", true);
            out.put("response", resp);
        } catch (Throwable t) {
            try {
                out.put("ok", false);
                out.put("error", t.getMessage());
            } catch (Throwable ignored) {
            }
        }
        return out.toString();
    }

    @JavascriptInterface
    public void requestHidden() {
        activity.runOnUiThread(triggerHelper::requestHidden);
    }

    @JavascriptInterface
    public void onVersionTap() {
        activity.runOnUiThread(triggerHelper::requestHidden);
    }

    @JavascriptInterface
    public String getPrefs() {
        JSONObject out = new JSONObject();
        try {
            out.put("lang", LocaleHelper.getLanguage(activity));
            out.put("theme", ThemeHelper.getTheme(activity));
        } catch (Throwable ignored) {
        }
        return out.toString();
    }

    @JavascriptInterface
    public String getLanguage() {
        return LocaleHelper.getLanguage(activity);
    }

    @JavascriptInterface
    public String getTheme() {
        return ThemeHelper.getTheme(activity);
    }

    @JavascriptInterface
    public void setLanguage(String lang) {
        LocaleHelper.setLanguage(activity, lang);
        activity.runOnUiThread(activity::recreate);
    }

    @JavascriptInterface
    public void setTheme(String theme) {
        activity.runOnUiThread(() -> {
            ThemeHelper.setTheme(activity, theme);
            activity.recreate();
        });
    }

    @JavascriptInterface
    public void toast(String message) {
        if (message != null) {
            activity.runOnUiThread(() ->
                    Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
            );
        }
    }
}
