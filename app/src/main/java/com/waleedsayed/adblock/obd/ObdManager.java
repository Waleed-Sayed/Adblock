package com.waleedsayed.adblock.obd;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.SystemClock;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONObject;

public class ObdManager {
    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private BluetoothSocket socket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private volatile boolean connected = false;
    private String connectedAddress;
    private String connectedName;
    private String protocol = "";

    public ObdManager(Context context) {
        context.getApplicationContext();
    }

    public boolean isConnected() {
        return connected && socket != null && socket.isConnected();
    }

    public String getConnectedAddress() {
        return connectedAddress;
    }

    public String getConnectedName() {
        return connectedName;
    }

    public String getProtocol() {
        return protocol;
    }

    @SuppressLint("MissingPermission")
    public JSONArray getPairedDevices() {
        JSONArray arr = new JSONArray();
        try {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if (adapter == null || !adapter.isEnabled()) {
                return arr;
            }
            Set<BluetoothDevice> bonded = adapter.getBondedDevices();
            if (bonded == null) {
                return arr;
            }
            for (BluetoothDevice dev : bonded) {
                JSONObject obj = new JSONObject();
                obj.put("name", dev.getName() != null ? dev.getName() : "OBD-II");
                obj.put("address", dev.getAddress());
                arr.put(obj);
            }
        } catch (Throwable ignored) {
        }
        return arr;
    }

    @SuppressLint("MissingPermission")
    public synchronized JSONObject connect(String address) {
        JSONObject res = new JSONObject();
        disconnect();
        try {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if (adapter == null || !adapter.isEnabled()) {
                res.put("ok", false);
                res.put("error", "bluetooth_disabled");
                return res;
            }
            BluetoothDevice device = adapter.getRemoteDevice(address);
            adapter.cancelDiscovery();
            try {
                socket = device.createRfcommSocketToServiceRecord(SPP_UUID);
                socket.connect();
            } catch (IOException primaryErr) {
                try {
                    socket = (BluetoothSocket) device.getClass()
                            .getMethod("createRfcommSocket", int.class)
                            .invoke(device, 1);
                    socket.connect();
                } catch (Throwable fallbackErr) {
                    throw primaryErr;
                }
            }
            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();
            connected = true;
            connectedAddress = device.getAddress();
            connectedName = device.getName() != null ? device.getName() : address;

            sendCommand("ATZ", 1500L);
            sendCommand("ATE0", 600L);
            sendCommand("ATL0", 600L);
            sendCommand("ATS0", 600L);
            sendCommand("ATH0", 600L);
            sendCommand("ATSP0", 800L);
            protocol = sendCommand("ATDP", 800L).replace("\r", " ").replace("\n", " ").trim();

            res.put("ok", true);
            res.put("name", connectedName);
            res.put("address", connectedAddress);
            res.put("protocol", protocol);
        } catch (Throwable t) {
            disconnect();
            try {
                res.put("ok", false);
                res.put("error", t.getMessage() != null ? t.getMessage() : "connect_failed");
            } catch (Throwable ignored) {
            }
        }
        return res;
    }

    public synchronized void disconnect() {
        connected = false;
        connectedAddress = null;
        connectedName = null;
        protocol = "";
        try {
            if (inputStream != null) inputStream.close();
        } catch (Throwable ignored) {}
        try {
            if (outputStream != null) outputStream.close();
        } catch (Throwable ignored) {}
        try {
            if (socket != null) socket.close();
        } catch (Throwable ignored) {}
        inputStream = null;
        outputStream = null;
        socket = null;
    }

    public synchronized String sendCommand(String cmd, long timeoutMs) throws IOException {
        if (!isConnected() || outputStream == null || inputStream == null) {
            throw new IOException("not_connected");
        }
        while (inputStream.available() > 0) {
            inputStream.read();
        }
        byte[] payload = (cmd.trim() + "\r").getBytes(StandardCharsets.US_ASCII);
        outputStream.write(payload);
        outputStream.flush();

        StringBuilder sb = new StringBuilder();
        long deadline = SystemClock.elapsedRealtime() + timeoutMs;
        byte[] buf = new byte[128];
        while (SystemClock.elapsedRealtime() < deadline) {
            if (inputStream.available() > 0) {
                int r = inputStream.read(buf);
                if (r > 0) {
                    String chunk = new String(buf, 0, r, StandardCharsets.US_ASCII);
                    sb.append(chunk);
                    if (sb.indexOf(">") >= 0) {
                        break;
                    }
                }
            } else {
                SystemClock.sleep(15L);
            }
        }
        return sb.toString().replace(">", "").trim();
    }

    public synchronized JSONObject readLiveSnapshot() {
        JSONObject out = new JSONObject();
        try {
            if (!isConnected()) {
                out.put("connected", false);
                return out;
            }
            out.put("connected", true);
            JSONObject values = new JSONObject();
            for (ObdPidDefinitions.PidDef def : ObdPidDefinitions.PIDS) {
                try {
                    String raw = sendCommand(def.command, 600L);
                    int[] bytes = ObdPidDefinitions.parseResponseBytes(raw, def.command);
                    if (bytes != null && bytes.length >= def.expectedBytes) {
                        Double decoded = ObdPidDefinitions.decodeValue(def.key, bytes);
                        if (decoded != null) {
                            values.put(def.key, Math.round(decoded * 100.0) / 100.0);
                        }
                    }
                } catch (Throwable ignored) {
                }
            }
            try {
                String voltRaw = sendCommand("ATRV", 500L);
                String num = voltRaw.replaceAll("[^0-9.]", "");
                if (!num.isEmpty() && !values.has("voltage")) {
                    values.put("voltage", Double.parseDouble(num));
                }
            } catch (Throwable ignored) {
            }
            out.put("values", values);
            out.put("timestamp", System.currentTimeMillis());
        } catch (Throwable t) {
            disconnect();
            try {
                out.put("connected", false);
                out.put("error", t.getMessage());
            } catch (Throwable ignored) {
            }
        }
        return out;
    }

    public synchronized JSONArray readTroubleCodes() {
        JSONArray codes = new JSONArray();
        if (!isConnected()) {
            return codes;
        }
        try {
            String raw = sendCommand("03", 2000L);
            for (String code : parseDtcHex(raw, "43")) {
                codes.put(code);
            }
            String pending = sendCommand("07", 2000L);
            for (String code : parseDtcHex(pending, "47")) {
                codes.put(code + " (P)");
            }
        } catch (Throwable ignored) {
        }
        return codes;
    }

    public synchronized boolean clearTroubleCodes() {
        if (!isConnected()) {
            return false;
        }
        try {
            String resp = sendCommand("04", 2500L);
            return resp.contains("44") || resp.toUpperCase(Locale.US).contains("OK");
        } catch (Throwable t) {
            return false;
        }
    }

    public synchronized String readVin() {
        if (!isConnected()) {
            return "";
        }
        try {
            String raw = sendCommand("0902", 2000L);
            String hex = raw.replaceAll("[^0-9A-Fa-f]", "").toUpperCase(Locale.US);
            int idx = hex.indexOf("4902");
            if (idx >= 0) {
                hex = hex.substring(idx + 6);
            }
            StringBuilder vin = new StringBuilder();
            for (int i = 0; i + 1 < hex.length(); i += 2) {
                int b = Integer.parseInt(hex.substring(i, i + 2), 16);
                if (b >= 0x30 && b <= 0x5A) {
                    vin.append((char) b);
                }
            }
            return vin.toString();
        } catch (Throwable t) {
            return "";
        }
    }

    private static List<String> parseDtcHex(String raw, String modePrefix) {
        List<String> list = new ArrayList<>();
        if (raw == null) {
            return list;
        }
        String cleaned = raw.replaceAll("[^0-9A-Fa-f]", "").toUpperCase(Locale.US);
        int idx = cleaned.indexOf(modePrefix);
        if (idx < 0) {
            return list;
        }
        String body = cleaned.substring(idx + 2);
        char[] prefixChars = {'P', 'C', 'B', 'U'};
        for (int i = 0; i + 3 < body.length(); i += 4) {
            String chunk = body.substring(i, i + 4);
            if ("0000".equals(chunk)) {
                continue;
            }
            try {
                int firstNib = Integer.parseInt(chunk.substring(0, 1), 16);
                char type = prefixChars[(firstNib >> 2) & 0x3];
                int secondDigit = firstNib & 0x3;
                String code = type + String.valueOf(secondDigit) + chunk.substring(1);
                if (!list.contains(code)) {
                    list.add(code);
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return list;
    }
}
