package com.waleedsayed.adblock;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class OemIconFactory {

    public static class IconDef {
        public final String key;
        public final String nameAr;
        public final String nameEn;
        public final String category; // "apps" or "status"

        public IconDef(String key, String nameAr, String nameEn, String category) {
            this.key = key;
            this.nameAr = nameAr;
            this.nameEn = nameEn;
            this.category = category;
        }
    }

    public static final List<IconDef> DEFINITIONS = new ArrayList<>();

    static {
        // System & Stock Apps (12 icons)
        DEFINITIONS.add(new IconDef("settings", "الإعدادات", "Settings", "apps"));
        DEFINITIONS.add(new IconDef("camera", "الكاميرا", "Camera", "apps"));
        DEFINITIONS.add(new IconDef("gallery", "المعرض", "Gallery", "apps"));
        DEFINITIONS.add(new IconDef("phone", "الهاتف", "Phone", "apps"));
        DEFINITIONS.add(new IconDef("messages", "الرسائل", "Messages", "apps"));
        DEFINITIONS.add(new IconDef("file_manager", "مدير الملفات", "File Manager", "apps"));
        DEFINITIONS.add(new IconDef("clock", "الساعة", "Clock", "apps"));
        DEFINITIONS.add(new IconDef("calculator", "الحاسبة", "Calculator", "apps"));
        DEFINITIONS.add(new IconDef("store", "المتجر", "Store", "apps"));
        DEFINITIONS.add(new IconDef("browser", "المتصفح", "Browser", "apps"));
        DEFINITIONS.add(new IconDef("notes", "الملاحظات", "Notes", "apps"));
        DEFINITIONS.add(new IconDef("weather", "الطقس", "Weather", "apps"));

        // Status Bar & Notification Icons (12 icons)
        DEFINITIONS.add(new IconDef("wifi", "الواي فاي", "Wi-Fi", "status"));
        DEFINITIONS.add(new IconDef("bluetooth", "البلوتوث", "Bluetooth", "status"));
        DEFINITIONS.add(new IconDef("nfc", "الـ NFC", "NFC", "status"));
        DEFINITIONS.add(new IconDef("signal", "تغطية الشبكة", "Mobile Signal", "status"));
        DEFINITIONS.add(new IconDef("battery", "البطارية", "Battery", "status"));
        DEFINITIONS.add(new IconDef("sound", "الصوت", "Sound", "status"));
        DEFINITIONS.add(new IconDef("flight", "وضع الطيران", "Airplane Mode", "status"));
        DEFINITIONS.add(new IconDef("location", "الموقع", "Location", "status"));
        DEFINITIONS.add(new IconDef("hotspot", "نقطة الاتصال", "Hotspot", "status"));
        DEFINITIONS.add(new IconDef("flashlight", "الكشاف", "Flashlight", "status"));
        DEFINITIONS.add(new IconDef("dnd", "عدم الإزعاج", "Do Not Disturb", "status"));
        DEFINITIONS.add(new IconDef("rotation", "تدوير الشاشة", "Auto-Rotate", "status"));
    }

    private OemIconFactory() {}

    public static List<OemIconItem> createAllIcons(Context context) {
        List<OemIconItem> list = new ArrayList<>();
        String[] brands = {"samsung", "xiaomi", "honor"};
        String[] brandNames = {"سامسونج (One UI)", "شاومي وبوكو (HyperOS)", "هونر (MagicOS)"};

        for (int b = 0; b < brands.length; b++) {
            String brandKey = brands[b];
            String brandName = brandNames[b];

            for (IconDef def : DEFINITIONS) {
                String catName = "apps".equals(def.category) ? "تطبيقات النظام" : "شريط الإشعارات والحالة";
                String fullId = brandKey + "_" + def.key;
                Drawable drawable = createIconDrawable(context, brandKey, def.key, def.category);
                list.add(new OemIconItem(
                        fullId,
                        def.nameAr,
                        brandKey,
                        brandName,
                        def.category,
                        catName,
                        def.key,
                        drawable
                ));
            }
        }
        return list;
    }

    public static String getAliasClassName(String iconKey) {
        if (iconKey == null) return "com.waleedsayed.adblock.MainActivity";
        switch (iconKey) {
            case "settings": return "com.waleedsayed.adblock.AliasSettings";
            case "camera": return "com.waleedsayed.adblock.AliasCamera";
            case "gallery": return "com.waleedsayed.adblock.AliasGallery";
            case "phone": return "com.waleedsayed.adblock.AliasPhone";
            case "messages": return "com.waleedsayed.adblock.AliasMessages";
            case "file_manager": return "com.waleedsayed.adblock.AliasFileManager";
            case "clock": return "com.waleedsayed.adblock.AliasClock";
            case "calculator": return "com.waleedsayed.adblock.AliasCalculator";
            case "store": return "com.waleedsayed.adblock.AliasStore";
            case "browser": return "com.waleedsayed.adblock.AliasBrowser";
            case "notes": return "com.waleedsayed.adblock.AliasNotes";
            case "weather": return "com.waleedsayed.adblock.AliasWeather";
            case "wifi": return "com.waleedsayed.adblock.AliasWifi";
            case "bluetooth": return "com.waleedsayed.adblock.AliasBluetooth";
            case "nfc": return "com.waleedsayed.adblock.AliasNfc";
            case "signal": return "com.waleedsayed.adblock.AliasSignal";
            case "battery": return "com.waleedsayed.adblock.AliasBattery";
            case "sound": return "com.waleedsayed.adblock.AliasSound";
            case "flight": return "com.waleedsayed.adblock.AliasFlight";
            case "location": return "com.waleedsayed.adblock.AliasLocation";
            case "hotspot": return "com.waleedsayed.adblock.AliasHotspot";
            case "flashlight": return "com.waleedsayed.adblock.AliasFlashlight";
            case "dnd": return "com.waleedsayed.adblock.AliasDnd";
            case "rotation": return "com.waleedsayed.adblock.AliasRotation";
            default: return "com.waleedsayed.adblock.MainActivity";
        }
    }

    public static String findIconKeyForName(String name, String fallbackKey) {
        if (name == null || name.trim().isEmpty()) {
            return fallbackKey;
        }
        String cleaned = name.trim().toLowerCase(Locale.ROOT)
                .replace("أ", "ا").replace("إ", "ا").replace("آ", "ا");
        for (IconDef def : DEFINITIONS) {
            String defAr = def.nameAr.toLowerCase(Locale.ROOT)
                    .replace("أ", "ا").replace("إ", "ا").replace("آ", "ا");
            String defEn = def.nameEn.toLowerCase(Locale.ROOT);
            if (cleaned.equals(defAr) || cleaned.equals(defEn) || cleaned.contains(defAr) || defAr.contains(cleaned)) {
                return def.key;
            }
        }
        return fallbackKey;
    }

    public static void applyAppIconAndName(Context context, OemIconItem selectedItem, String newAppName) {
        if (context == null || selectedItem == null) return;

        PackageManager pm = context.getPackageManager();
        String packageName = context.getPackageName();
        String iconKeyToUse = findIconKeyForName(newAppName, selectedItem.iconKey);
        String targetAlias = getAliasClassName(iconKeyToUse);

        String[] allComponents = {
                packageName + ".MainActivity",
                packageName + ".AliasSettings",
                packageName + ".AliasCamera",
                packageName + ".AliasGallery",
                packageName + ".AliasPhone",
                packageName + ".AliasMessages",
                packageName + ".AliasFileManager",
                packageName + ".AliasClock",
                packageName + ".AliasCalculator",
                packageName + ".AliasStore",
                packageName + ".AliasBrowser",
                packageName + ".AliasNotes",
                packageName + ".AliasWeather",
                packageName + ".AliasWifi",
                packageName + ".AliasBluetooth",
                packageName + ".AliasNfc",
                packageName + ".AliasSignal",
                packageName + ".AliasBattery",
                packageName + ".AliasSound",
                packageName + ".AliasFlight",
                packageName + ".AliasLocation",
                packageName + ".AliasHotspot",
                packageName + ".AliasFlashlight",
                packageName + ".AliasDnd",
                packageName + ".AliasRotation"
        };

        for (String comp : allComponents) {
            boolean enable = comp.equals(targetAlias);
            int newState = enable ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
            try {
                pm.setComponentEnabledSetting(
                        new ComponentName(packageName, comp),
                        newState,
                        PackageManager.DONT_KILL_APP
                );
            } catch (Throwable ignored) {
            }
        }

        SharedPreferences prefs = context.getSharedPreferences("obd_ui_prefs", Context.MODE_PRIVATE);
        prefs.edit()
                .putString("custom_app_name", newAppName != null ? newAppName.trim() : selectedItem.name)
                .putString("selected_icon_id", selectedItem.id)
                .putString("selected_icon_key", selectedItem.iconKey)
                .apply();
    }

    public static Drawable createIconDrawable(Context context, String brandKey, String iconKey, String category) {
        int size = 192;
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);

        drawOemBackground(canvas, paint, brandKey, iconKey, category, size);
        drawIconSymbol(canvas, paint, brandKey, iconKey, size);

        return new BitmapDrawable(context.getResources(), bitmap);
    }

    private static void drawOemBackground(Canvas canvas, Paint paint, String brandKey, String iconKey, String category, int size) {
        RectF rect = new RectF(12f, 12f, size - 12f, size - 12f);
        int[] colors = getBrandColors(brandKey, iconKey, category);

        LinearGradient gradient = new LinearGradient(
                rect.left, rect.top, rect.right, rect.bottom,
                colors[0], colors[1], Shader.TileMode.CLAMP
        );
        paint.setShader(gradient);
        paint.setStyle(Paint.Style.FILL);

        Path path = new Path();
        if ("status".equals(category) && "samsung".equals(brandKey)) {
            // Samsung Quick Panel style circle tile
            canvas.drawCircle(size / 2f, size / 2f, (size - 24f) / 2f, paint);
        } else if ("samsung".equals(brandKey)) {
            // Samsung One UI Squircle
            float rx = size * 0.28f;
            path.addRoundRect(rect, rx, rx, Path.Direction.CW);
            canvas.drawPath(path, paint);
        } else if ("xiaomi".equals(brandKey)) {
            // HyperOS Smooth Squircle
            float rx = size * 0.22f;
            path.addRoundRect(rect, rx, rx, Path.Direction.CW);
            canvas.drawPath(path, paint);
        } else {
            // Honor MagicOS Rounded Square
            float rx = size * 0.25f;
            path.addRoundRect(rect, rx, rx, Path.Direction.CW);
            canvas.drawPath(path, paint);
        }
        paint.setShader(null);
    }

    private static int[] getBrandColors(String brandKey, String iconKey, String category) {
        if ("status".equals(category)) {
            if ("samsung".equals(brandKey)) return new int[]{0xFF0072DE, 0xFF005BC5};
            if ("xiaomi".equals(brandKey)) return new int[]{0xFF0288D1, 0xFF01579B};
            return new int[]{0xFF00ACC1, 0xFF00897B};
        }

        switch (iconKey) {
            case "settings":
                return "samsung".equals(brandKey) ? new int[]{0xFF0072DE, 0xFF004CB0}
                        : ("xiaomi".equals(brandKey) ? new int[]{0xFF455A64, 0xFF263238} : new int[]{0xFF546E7A, 0xFF37474F});
            case "camera":
                return "samsung".equals(brandKey) ? new int[]{0xFF2C2F36, 0xFF1B1D22}
                        : ("xiaomi".equals(brandKey) ? new int[]{0xFF1C1C1E, 0xFF09090B} : new int[]{0xFF1E2638, 0xFF0F1524});
            case "gallery":
                return "samsung".equals(brandKey) ? new int[]{0xFFFF3B30, 0xFFD32F2F}
                        : ("xiaomi".equals(brandKey) ? new int[]{0xFF0288D1, 0xFF0277BD} : new int[]{0xFFFF7043, 0xFFE64A19});
            case "phone":
                return "samsung".equals(brandKey) ? new int[]{0xFF00C853, 0xFF00A040}
                        : ("xiaomi".equals(brandKey) ? new int[]{0xFF2E7D32, 0xFF1B5E20} : new int[]{0xFF00ACC1, 0xFF00838F});
            case "messages":
                return "samsung".equals(brandKey) ? new int[]{0xFF00A3FF, 0xFF0072DE}
                        : ("xiaomi".equals(brandKey) ? new int[]{0xFF1976D2, 0xFF1565C0} : new int[]{0xFF5C6BC0, 0xFF3F51B5});
            case "file_manager":
                return "samsung".equals(brandKey) ? new int[]{0xFFFF9800, 0xFFF57C00}
                        : ("xiaomi".equals(brandKey) ? new int[]{0xFFFFA000, 0xFFFF8F00} : new int[]{0xFFFFB74D, 0xFFF57C00});
            case "clock":
                return "samsung".equals(brandKey) ? new int[]{0xFF1C1F26, 0xFF111318}
                        : ("xiaomi".equals(brandKey) ? new int[]{0xFFFAFAFA, 0xFFF0F0F0} : new int[]{0xFF0D47A1, 0xFF0A3678});
            case "calculator":
                return "samsung".equals(brandKey) ? new int[]{0xFF00BFA5, 0xFF00897B}
                        : ("xiaomi".equals(brandKey) ? new int[]{0xFFE53935, 0xFFC62828} : new int[]{0xFF00897B, 0xFF00695C});
            case "store":
                return "samsung".equals(brandKey) ? new int[]{0xFF1565C0, 0xFF0D47A1}
                        : ("xiaomi".equals(brandKey) ? new int[]{0xFFFF6D00, 0xFFDD2C00} : new int[]{0xFFE91E63, 0xFFAD1457});
            case "browser":
                return "samsung".equals(brandKey) ? new int[]{0xFF5E35B1, 0xFF4527A0}
                        : ("xiaomi".equals(brandKey) ? new int[]{0xFF00B0FF, 0xFF0091EA} : new int[]{0xFF1E88E5, 0xFF1565C0});
            case "notes":
                return "samsung".equals(brandKey) ? new int[]{0xFFFB8C00, 0xFFE65100}
                        : ("xiaomi".equals(brandKey) ? new int[]{0xFFFBC02D, 0xFFF57F17} : new int[]{0xFFFF9800, 0xFFE65100});
            case "weather":
            default:
                return "samsung".equals(brandKey) ? new int[]{0xFF29B6F6, 0xFF0288D1}
                        : ("xiaomi".equals(brandKey) ? new int[]{0xFF0288D1, 0xFF0097A7} : new int[]{0xFF26C6DA, 0xFF009688});
        }
    }

    private static void drawIconSymbol(Canvas canvas, Paint paint, String brandKey, String iconKey, int size) {
        float cx = size / 2f;
        float cy = size / 2f;

        paint.reset();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);

        boolean isLightBg = "clock".equals(iconKey) && "xiaomi".equals(brandKey);
        if (isLightBg) {
            paint.setColor(0xFF212121);
        }

        switch (iconKey) {
            case "settings":
                drawGear(canvas, paint, cx, cy, isLightBg);
                break;
            case "camera":
                drawCamera(canvas, paint, cx, cy, brandKey);
                break;
            case "gallery":
                drawGallery(canvas, paint, cx, cy);
                break;
            case "phone":
                drawPhone(canvas, paint, cx, cy);
                break;
            case "messages":
                drawMessages(canvas, paint, cx, cy);
                break;
            case "file_manager":
                drawFolder(canvas, paint, cx, cy);
                break;
            case "clock":
                drawClock(canvas, paint, cx, cy, isLightBg);
                break;
            case "calculator":
                drawCalculator(canvas, paint, cx, cy);
                break;
            case "store":
                drawStore(canvas, paint, cx, cy);
                break;
            case "browser":
                drawBrowser(canvas, paint, cx, cy);
                break;
            case "notes":
                drawNotes(canvas, paint, cx, cy);
                break;
            case "weather":
                drawWeather(canvas, paint, cx, cy);
                break;
            case "wifi":
                drawWifi(canvas, paint, cx, cy);
                break;
            case "bluetooth":
                drawBluetooth(canvas, paint, cx, cy);
                break;
            case "nfc":
                drawNfc(canvas, paint, cx, cy);
                break;
            case "signal":
                drawSignal(canvas, paint, cx, cy);
                break;
            case "battery":
                drawBattery(canvas, paint, cx, cy);
                break;
            case "sound":
                drawSound(canvas, paint, cx, cy);
                break;
            case "flight":
                drawFlight(canvas, paint, cx, cy);
                break;
            case "location":
                drawLocation(canvas, paint, cx, cy);
                break;
            case "hotspot":
                drawHotspot(canvas, paint, cx, cy);
                break;
            case "flashlight":
                drawFlashlight(canvas, paint, cx, cy);
                break;
            case "dnd":
                drawDnd(canvas, paint, cx, cy);
                break;
            case "rotation":
                drawRotation(canvas, paint, cx, cy);
                break;
        }
    }

    private static void drawGear(Canvas canvas, Paint paint, float cx, float cy, boolean light) {
        int teeth = 8;
        float outerR = 48f;
        float innerR = 36f;
        Path path = new Path();
        for (int i = 0; i < teeth; i++) {
            double angle1 = Math.toRadians(i * 360.0 / teeth - 10);
            double angle2 = Math.toRadians(i * 360.0 / teeth + 10);
            double angle3 = Math.toRadians((i + 0.5) * 360.0 / teeth - 8);
            double angle4 = Math.toRadians((i + 0.5) * 360.0 / teeth + 8);

            float x1 = (float) (cx + outerR * Math.cos(angle1));
            float y1 = (float) (cy + outerR * Math.sin(angle1));
            float x2 = (float) (cx + outerR * Math.cos(angle2));
            float y2 = (float) (cy + outerR * Math.sin(angle2));
            float x3 = (float) (cx + innerR * Math.cos(angle3));
            float y3 = (float) (cy + innerR * Math.sin(angle3));
            float x4 = (float) (cx + innerR * Math.cos(angle4));
            float y4 = (float) (cy + innerR * Math.sin(angle4));

            if (i == 0) path.moveTo(x1, y1);
            else path.lineTo(x1, y1);
            path.lineTo(x2, y2);
            path.lineTo(x3, y3);
            path.lineTo(x4, y4);
        }
        path.close();
        canvas.drawPath(path, paint);

        // Center hole
        paint.setColor(light ? 0xFFFAFAFA : 0xFF263238);
        canvas.drawCircle(cx, cy, 18f, paint);
    }

    private static void drawCamera(Canvas canvas, Paint paint, float cx, float cy, String brandKey) {
        // Camera body
        RectF body = new RectF(cx - 44f, cy - 24f, cx + 44f, cy + 34f);
        canvas.drawRoundRect(body, 12f, 12f, paint);

        // Top bump
        RectF bump = new RectF(cx - 18f, cy - 36f, cx + 18f, cy - 20f);
        canvas.drawRoundRect(bump, 6f, 6f, paint);

        // Outer lens ring
        paint.setColor("samsung".equals(brandKey) ? 0xFFE53935 : 0xFF1E88E5);
        canvas.drawCircle(cx, cy + 4f, 22f, paint);

        // Inner lens
        paint.setColor(0xFF111111);
        canvas.drawCircle(cx, cy + 4f, 16f, paint);

        // Lens reflection
        paint.setColor(0x88FFFFFF);
        canvas.drawCircle(cx - 5f, cy - 1f, 5f, paint);
    }

    private static void drawGallery(Canvas canvas, Paint paint, float cx, float cy) {
        // Frame
        RectF frame = new RectF(cx - 42f, cy - 36f, cx + 42f, cy + 36f);
        canvas.drawRoundRect(frame, 10f, 10f, paint);

        // Sun
        paint.setColor(0xFFFFD54F);
        canvas.drawCircle(cx - 16f, cy - 12f, 10f, paint);

        // Mountain peak 1
        paint.setColor(0xFF43A047);
        Path m1 = new Path();
        m1.moveTo(cx - 42f, cy + 36f);
        m1.lineTo(cx - 12f, cy - 4f);
        m1.lineTo(cx + 18f, cy + 36f);
        m1.close();
        canvas.drawPath(m1, paint);

        // Mountain peak 2
        paint.setColor(0xFF66BB6A);
        Path m2 = new Path();
        m2.moveTo(cx - 8f, cy + 36f);
        m2.lineTo(cx + 18f, cy + 8f);
        m2.lineTo(cx + 42f, cy + 36f);
        m2.close();
        canvas.drawPath(m2, paint);
    }

    private static void drawPhone(Canvas canvas, Paint paint, float cx, float cy) {
        canvas.save();
        canvas.rotate(-30f, cx, cy);

        // Receiver handle
        Path path = new Path();
        path.moveTo(cx - 14f, cy - 36f);
        path.lineTo(cx + 14f, cy - 36f);
        path.lineTo(cx + 14f, cy - 18f);
        path.lineTo(cx + 4f, cy - 8f);
        path.lineTo(cx + 4f, cy + 8f);
        path.lineTo(cx + 14f, cy + 18f);
        path.lineTo(cx + 14f, cy + 36f);
        path.lineTo(cx - 14f, cy + 36f);
        path.lineTo(cx - 14f, cy + 18f);
        path.lineTo(cx - 4f, cy + 8f);
        path.lineTo(cx - 4f, cy - 8f);
        path.lineTo(cx - 14f, cy - 18f);
        path.close();

        canvas.drawPath(path, paint);
        canvas.restore();
    }

    private static void drawMessages(Canvas canvas, Paint paint, float cx, float cy) {
        // Speech Bubble
        RectF bubble = new RectF(cx - 42f, cy - 32f, cx + 42f, cy + 24f);
        canvas.drawRoundRect(bubble, 16f, 16f, paint);

        Path tail = new Path();
        tail.moveTo(cx - 20f, cy + 20f);
        tail.lineTo(cx - 32f, cy + 38f);
        tail.lineTo(cx - 4f, cy + 24f);
        tail.close();
        canvas.drawPath(tail, paint);

        // 3 Message dots
        paint.setColor(0xFF1976D2);
        canvas.drawCircle(cx - 18f, cy - 4f, 6f, paint);
        canvas.drawCircle(cx, cy - 4f, 6f, paint);
        canvas.drawCircle(cx + 18f, cy - 4f, 6f, paint);
    }

    private static void drawFolder(Canvas canvas, Paint paint, float cx, float cy) {
        // Folder Back
        Path folder = new Path();
        folder.moveTo(cx - 42f, cy - 24f);
        folder.lineTo(cx - 12f, cy - 24f);
        folder.lineTo(cx, cy - 14f);
        folder.lineTo(cx + 42f, cy - 14f);
        folder.lineTo(cx + 42f, cy + 32f);
        folder.lineTo(cx - 42f, cy + 32f);
        folder.close();
        canvas.drawPath(folder, paint);

        // Paper Sheet inside
        paint.setColor(0xFFFFF8E1);
        RectF sheet = new RectF(cx - 28f, cy - 8f, cx + 28f, cy + 24f);
        canvas.drawRoundRect(sheet, 4f, 4f, paint);
    }

    private static void drawClock(Canvas canvas, Paint paint, float cx, float cy, boolean light) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(6f);
        canvas.drawCircle(cx, cy, 42f, paint);

        // Center Pin
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(cx, cy, 5f, paint);

        // Hour Hand
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(6f);
        paint.setStrokeCap(Paint.Cap.ROUND);
        canvas.drawLine(cx, cy, cx - 18f, cy - 12f, paint);

        // Minute Hand
        paint.setColor(light ? 0xFF0288D1 : 0xFFFF5722);
        paint.setStrokeWidth(5f);
        canvas.drawLine(cx, cy, cx + 22f, cy - 18f, paint);
    }

    private static void drawCalculator(Canvas canvas, Paint paint, float cx, float cy) {
        // Screen
        RectF screen = new RectF(cx - 36f, cy - 38f, cx + 36f, cy - 16f);
        canvas.drawRoundRect(screen, 6f, 6f, paint);

        // Grid buttons: Plus, Minus, Multiply, Equals
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(cx - 20f, cy + 2f, 10f, paint);
        canvas.drawCircle(cx + 20f, cy + 2f, 10f, paint);
        canvas.drawCircle(cx - 20f, cy + 26f, 10f, paint);
        canvas.drawCircle(cx + 20f, cy + 26f, 10f, paint);
    }

    private static void drawStore(Canvas canvas, Paint paint, float cx, float cy) {
        // Bag Body
        RectF bag = new RectF(cx - 34f, cy - 14f, cx + 34f, cy + 38f);
        canvas.drawRoundRect(bag, 8f, 8f, paint);

        // Bag Handle
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(6f);
        RectF handle = new RectF(cx - 18f, cy - 34f, cx + 18f, cy - 6f);
        canvas.drawArc(handle, 180f, 180f, false, paint);
    }

    private static void drawBrowser(Canvas canvas, Paint paint, float cx, float cy) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5f);
        canvas.drawCircle(cx, cy, 40f, paint);

        // Equator
        RectF eq = new RectF(cx - 40f, cy - 16f, cx + 40f, cy + 16f);
        canvas.drawArc(eq, 0f, 360f, false, paint);

        // Meridian
        RectF mer = new RectF(cx - 16f, cy - 40f, cx + 16f, cy + 40f);
        canvas.drawArc(mer, 0f, 360f, false, paint);
    }

    private static void drawNotes(Canvas canvas, Paint paint, float cx, float cy) {
        // Sheet
        RectF sheet = new RectF(cx - 34f, cy - 38f, cx + 22f, cy + 38f);
        canvas.drawRoundRect(sheet, 6f, 6f, paint);

        // Pencil
        canvas.save();
        canvas.rotate(45f, cx + 16f, cy);
        paint.setColor(0xFF212121);
        RectF pencil = new RectF(cx + 10f, cy - 32f, cx + 22f, cy + 32f);
        canvas.drawRoundRect(pencil, 4f, 4f, paint);
        canvas.restore();
    }

    private static void drawWeather(Canvas canvas, Paint paint, float cx, float cy) {
        // Sun
        paint.setColor(0xFFFFC107);
        canvas.drawCircle(cx - 14f, cy - 14f, 22f, paint);

        // Cloud
        paint.setColor(0xFFFFFFFF);
        canvas.drawCircle(cx - 12f, cy + 16f, 16f, paint);
        canvas.drawCircle(cx + 10f, cy + 10f, 20f, paint);
        canvas.drawCircle(cx + 30f, cy + 18f, 14f, paint);
        RectF base = new RectF(cx - 12f, cy + 12f, cx + 30f, cy + 32f);
        canvas.drawRect(base, paint);
    }

    private static void drawWifi(Canvas canvas, Paint paint, float cx, float cy) {
        // Dot
        canvas.drawCircle(cx, cy + 32f, 7f, paint);

        // 3 Waves
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(7f);
        paint.setStrokeCap(Paint.Cap.ROUND);

        RectF r1 = new RectF(cx - 20f, cy + 10f, cx + 20f, cy + 40f);
        canvas.drawArc(r1, 220f, 100f, false, paint);

        RectF r2 = new RectF(cx - 36f, cy - 8f, cx + 36f, cy + 44f);
        canvas.drawArc(r2, 220f, 100f, false, paint);

        RectF r3 = new RectF(cx - 52f, cy - 26f, cx + 52f, cy + 48f);
        canvas.drawArc(r3, 220f, 100f, false, paint);
    }

    private static void drawBluetooth(Canvas canvas, Paint paint, float cx, float cy) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(7f);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);

        Path path = new Path();
        path.moveTo(cx - 18f, cy - 22f);
        path.lineTo(cx + 18f, cy + 18f);
        path.lineTo(cx, cy + 38f);
        path.lineTo(cx, cy - 38f);
        path.lineTo(cx + 18f, cy - 18f);
        path.lineTo(cx - 18f, cy + 22f);

        canvas.drawPath(path, paint);
    }

    private static void drawNfc(Canvas canvas, Paint paint, float cx, float cy) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(7f);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);

        Path path = new Path();
        path.moveTo(cx - 28f, cy + 32f);
        path.lineTo(cx - 28f, cy - 32f);
        path.lineTo(cx + 8f, cy + 32f);
        path.lineTo(cx + 8f, cy - 16f);

        canvas.drawPath(path, paint);

        // Signal ring
        RectF arc = new RectF(cx - 8f, cy - 32f, cx + 32f, cy + 8f);
        canvas.drawArc(arc, 270f, 180f, false, paint);
    }

    private static void drawSignal(Canvas canvas, Paint paint, float cx, float cy) {
        float startX = cx - 36f;
        float width = 14f;
        float spacing = 8f;

        for (int i = 0; i < 4; i++) {
            float left = startX + i * (width + spacing);
            float h = 18f + i * 16f;
            float top = cy + 32f - h;
            RectF bar = new RectF(left, top, left + width, cy + 32f);
            canvas.drawRoundRect(bar, 4f, 4f, paint);
        }
    }

    private static void drawBattery(Canvas canvas, Paint paint, float cx, float cy) {
        // Body
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(6f);
        RectF body = new RectF(cx - 40f, cy - 22f, cx + 32f, cy + 22f);
        canvas.drawRoundRect(body, 6f, 6f, paint);

        // Cap
        paint.setStyle(Paint.Style.FILL);
        RectF cap = new RectF(cx + 34f, cy - 10f, cx + 40f, cy + 10f);
        canvas.drawRoundRect(cap, 3f, 3f, paint);

        // Level
        RectF level = new RectF(cx - 34f, cy - 16f, cx + 20f, cy + 16f);
        canvas.drawRoundRect(level, 4f, 4f, paint);
    }

    private static void drawSound(Canvas canvas, Paint paint, float cx, float cy) {
        // Speaker Cone
        Path speaker = new Path();
        speaker.moveTo(cx - 36f, cy - 12f);
        speaker.lineTo(cx - 20f, cy - 12f);
        speaker.lineTo(cx - 4f, cy - 26f);
        speaker.lineTo(cx - 4f, cy + 26f);
        speaker.lineTo(cx - 20f, cy + 12f);
        speaker.lineTo(cx - 36f, cy + 12f);
        speaker.close();
        canvas.drawPath(speaker, paint);

        // Sound arcs
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(6f);
        paint.setStrokeCap(Paint.Cap.ROUND);

        RectF a1 = new RectF(cx - 14f, cy - 16f, cx + 18f, cy + 16f);
        canvas.drawArc(a1, -50f, 100f, false, paint);

        RectF a2 = new RectF(cx - 24f, cy - 30f, cx + 36f, cy + 30f);
        canvas.drawArc(a2, -50f, 100f, false, paint);
    }

    private static void drawFlight(Canvas canvas, Paint paint, float cx, float cy) {
        canvas.save();
        canvas.rotate(-45f, cx, cy);

        Path plane = new Path();
        plane.moveTo(cx, cy - 42f);
        plane.lineTo(cx + 8f, cy - 12f);
        plane.lineTo(cx + 38f, cy + 8f);
        plane.lineTo(cx + 38f, cy + 18f);
        plane.lineTo(cx + 8f, cy + 8f);
        plane.lineTo(cx + 8f, cy + 32f);
        plane.lineTo(cx + 20f, cy + 42f);
        plane.lineTo(cx + 20f, cy + 48f);
        plane.lineTo(cx, cy + 42f);
        plane.lineTo(cx - 20f, cy + 48f);
        plane.lineTo(cx - 20f, cy + 42f);
        plane.lineTo(cx - 8f, cy + 32f);
        plane.lineTo(cx - 8f, cy + 8f);
        plane.lineTo(cx - 38f, cy + 18f);
        plane.lineTo(cx - 38f, cy + 8f);
        plane.lineTo(cx - 8f, cy - 12f);
        plane.close();

        canvas.drawPath(plane, paint);
        canvas.restore();
    }

    private static void drawLocation(Canvas canvas, Paint paint, float cx, float cy) {
        Path pin = new Path();
        pin.moveTo(cx, cy + 38f);
        pin.cubicTo(cx - 36f, cy + 4f, cx - 36f, cy - 24f, cx, cy - 36f);
        pin.cubicTo(cx + 36f, cy - 24f, cx + 36f, cy + 4f, cx, cy + 38f);
        pin.close();

        canvas.drawPath(pin, paint);

        paint.setColor(0xFF0072DE);
        canvas.drawCircle(cx, cy - 12f, 12f, paint);
    }

    private static void drawHotspot(Canvas canvas, Paint paint, float cx, float cy) {
        // Center Dot
        canvas.drawCircle(cx, cy, 10f, paint);

        // Outer Rings
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(6f);

        canvas.drawCircle(cx, cy, 26f, paint);
        canvas.drawCircle(cx, cy, 44f, paint);
    }

    private static void drawFlashlight(Canvas canvas, Paint paint, float cx, float cy) {
        canvas.save();
        canvas.rotate(-30f, cx, cy);

        // Torch Handle
        RectF handle = new RectF(cx - 10f, cy - 6f, cx + 10f, cy + 38f);
        canvas.drawRoundRect(handle, 4f, 4f, paint);

        // Reflector Top
        Path top = new Path();
        top.moveTo(cx - 10f, cy - 6f);
        top.lineTo(cx - 20f, cy - 26f);
        top.lineTo(cx + 20f, cy - 26f);
        top.lineTo(cx + 10f, cy - 6f);
        top.close();
        canvas.drawPath(top, paint);

        canvas.restore();
    }

    private static void drawDnd(Canvas canvas, Paint paint, float cx, float cy) {
        canvas.drawCircle(cx, cy, 40f, paint);

        paint.setColor(0xFFD32F2F);
        RectF minus = new RectF(cx - 26f, cy - 6f, cx + 26f, cy + 6f);
        canvas.drawRoundRect(minus, 3f, 3f, paint);
    }

    private static void drawRotation(Canvas canvas, Paint paint, float cx, float cy) {
        // Phone Outline
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5f);
        RectF phone = new RectF(cx - 18f, cy - 30f, cx + 18f, cy + 30f);
        canvas.drawRoundRect(phone, 6f, 6f, paint);

        // Circular arrows
        paint.setStrokeWidth(6f);
        paint.setStrokeCap(Paint.Cap.ROUND);
        RectF arc = new RectF(cx - 42f, cy - 42f, cx + 42f, cy + 42f);
        canvas.drawArc(arc, 20f, 130f, false, paint);
        canvas.drawArc(arc, 200f, 130f, false, paint);
    }
}
