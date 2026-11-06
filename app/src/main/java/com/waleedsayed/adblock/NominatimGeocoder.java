package com.waleedsayed.adblock;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

public final class NominatimGeocoder {
    private NominatimGeocoder() {}

    public static String search(String query, String lang) {
        if (query == null || query.trim().isEmpty()) {
            return "[]";
        }
        String safeLang = (lang == null || lang.isEmpty()) ? "ar" : lang;
        HttpURLConnection conn = null;
        try {
            String encoded = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8.name());
            String urlStr = String.format(
                    Locale.US,
                    "https://nominatim.openstreetmap.org/search?format=json&limit=6&accept-language=%s&q=%s",
                    safeLang,
                    encoded
            );
            conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            conn.setRequestProperty("User-Agent", "OBD2DiagnosticApp/1.0 (Android; contact: support@local)");
            conn.setRequestProperty("Accept", "application/json");
            if (conn.getResponseCode() != 200) {
                return "[]";
            }
            StringBuilder sb = new StringBuilder();
            try (InputStream is = conn.getInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }
            JSONArray rawArr = new JSONArray(sb.toString());
            JSONArray outArr = new JSONArray();
            for (int i = 0; i < rawArr.length(); i++) {
                JSONObject item = rawArr.getJSONObject(i);
                JSONObject mapped = new JSONObject();
                mapped.put("name", item.optString("display_name", ""));
                mapped.put("lat", Double.parseDouble(item.optString("lat", "0")));
                mapped.put("lon", Double.parseDouble(item.optString("lon", "0")));
                outArr.put(mapped);
            }
            return outArr.toString();
        } catch (Throwable t) {
            return "[]";
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}
