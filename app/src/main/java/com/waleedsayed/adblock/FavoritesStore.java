package com.waleedsayed.adblock;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONObject;

public class FavoritesStore {
    public static final int MAX_FAVORITES = 20;
    private static final String PREFS_NAME = "obd2_favorites";
    private static final String KEY_ITEMS = "items_v1";

    private final SharedPreferences prefs;

    public static class FavoriteItem {
        public final String id;
        public final String name;
        public final double lat;
        public final double lon;

        public FavoriteItem(String id, String name, double lat, double lon) {
            this.id = id;
            this.name = name;
            this.lat = lat;
            this.lon = lon;
        }
    }

    public FavoritesStore(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public List<FavoriteItem> getAll() {
        List<FavoriteItem> list = new ArrayList<>();
        String raw = prefs.getString(KEY_ITEMS, "[]");
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                list.add(new FavoriteItem(
                        obj.optString("id", UUID.randomUUID().toString()),
                        obj.optString("name", ""),
                        obj.getDouble("lat"),
                        obj.getDouble("lon")
                ));
            }
        } catch (Throwable ignored) {
        }
        return list;
    }

    public void saveAll(List<FavoriteItem> items) {
        JSONArray arr = new JSONArray();
        try {
            for (FavoriteItem item : items) {
                JSONObject obj = new JSONObject();
                obj.put("id", item.id);
                obj.put("name", item.name);
                obj.put("lat", item.lat);
                obj.put("lon", item.lon);
                arr.put(obj);
            }
        } catch (Throwable ignored) {
        }
        prefs.edit().putString(KEY_ITEMS, arr.toString()).apply();
    }

    public FavoriteItem add(String name, double lat, double lon) {
        List<FavoriteItem> items = getAll();
        if (items.size() >= MAX_FAVORITES) {
            return null;
        }
        String trimmed = name == null ? "" : name.trim();
        FavoriteItem created = new FavoriteItem(UUID.randomUUID().toString(), trimmed, lat, lon);
        items.add(0, created);
        saveAll(items);
        return created;
    }

    public void remove(String id) {
        List<FavoriteItem> items = getAll();
        for (int i = items.size() - 1; i >= 0; i--) {
            if (items.get(i).id.equals(id)) {
                items.remove(i);
            }
        }
        saveAll(items);
    }

    public FavoriteItem findNear(double lat, double lon) {
        for (FavoriteItem item : getAll()) {
            if (Math.abs(item.lat - lat) < 1.0e-5 && Math.abs(item.lon - lon) < 1.0e-5) {
                return item;
            }
        }
        return null;
    }

    public String exportJson() {
        JSONObject root = new JSONObject();
        try {
            root.put("app", "obd2_favorites");
            root.put("version", 1);
            JSONArray arr = new JSONArray();
            for (FavoriteItem item : getAll()) {
                JSONObject obj = new JSONObject();
                obj.put("name", item.name);
                obj.put("lat", item.lat);
                obj.put("lon", item.lon);
                arr.put(obj);
            }
            root.put("items", arr);
        } catch (Throwable ignored) {
        }
        return root.toString();
    }

    public int importJson(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return -1;
        }
        try {
            String trimmed = raw.trim();
            JSONArray arr;
            if (trimmed.startsWith("[")) {
                arr = new JSONArray(trimmed);
            } else {
                JSONObject root = new JSONObject(trimmed);
                arr = root.optJSONArray("items");
                if (arr == null) {
                    return -1;
                }
            }
            List<FavoriteItem> existing = getAll();
            int added = 0;
            for (int i = 0; i < arr.length(); i++) {
                if (existing.size() >= MAX_FAVORITES) {
                    break;
                }
                JSONObject obj = arr.optJSONObject(i);
                if (obj == null || !obj.has("lat") || !obj.has("lon")) {
                    continue;
                }
                double lat = obj.getDouble("lat");
                double lon = obj.getDouble("lon");
                if (Double.isNaN(lat) || Double.isNaN(lon) || lat < -90.0 || lat > 90.0 || lon < -180.0 || lon > 180.0) {
                    continue;
                }
                String name = obj.optString("name", "").trim();
                if (name.isEmpty()) {
                    name = String.format("%.5f, %.5f", lat, lon);
                }
                boolean duplicate = false;
                for (FavoriteItem item : existing) {
                    if (Math.abs(item.lat - lat) < 1.0e-5 && Math.abs(item.lon - lon) < 1.0e-5) {
                        duplicate = true;
                        break;
                    }
                }
                if (!duplicate) {
                    existing.add(new FavoriteItem(UUID.randomUUID().toString(), name, lat, lon));
                    added++;
                }
            }
            if (added > 0) {
                saveAll(existing);
            }
            return added;
        } catch (Throwable t) {
            return -1;
        }
    }
}
