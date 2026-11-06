package com.waleedsayed.adblock.obd;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class ObdPidDefinitions {

    public static class PidDef {
        public final String key;
        public final String command;
        public final String titleAr;
        public final String titleEn;
        public final String unit;
        public final int expectedBytes;

        public PidDef(String key, String command, String titleAr, String titleEn, String unit, int expectedBytes) {
            this.key = key;
            this.command = command;
            this.titleAr = titleAr;
            this.titleEn = titleEn;
            this.unit = unit;
            this.expectedBytes = expectedBytes;
        }
    }

    public static final List<PidDef> PIDS = Arrays.asList(
            new PidDef("rpm", "010C", "سرعة المحرك", "Engine RPM", "RPM", 2),
            new PidDef("speed", "010D", "سرعة المركبة", "Vehicle Speed", "km/h", 1),
            new PidDef("coolant", "0105", "حرارة سائل التبريد", "Coolant Temp", "°C", 1),
            new PidDef("intake", "010F", "حرارة هواء السحب", "Intake Air Temp", "°C", 1),
            new PidDef("load", "0104", "حمل المحرك", "Engine Load", "%", 1),
            new PidDef("throttle", "0111", "موضع دواسة الوقود", "Throttle Position", "%", 1),
            new PidDef("fuel", "012F", "مستوى الوقود", "Fuel Level", "%", 1),
            new PidDef("voltage", "0142", "جهد وحدة التحكم", "Control Module Voltage", "V", 2),
            new PidDef("maf", "0110", "معدل تدفق الهواء", "MAF Air Flow", "g/s", 2),
            new PidDef("map", "010B", "ضغط مجمع السحب", "Intake Manifold Pressure", "kPa", 1),
            new PidDef("timing", "010E", "تقدم الشرارة", "Timing Advance", "°", 1),
            new PidDef("ambient", "0146", "حرارة الجو الخارجي", "Ambient Air Temp", "°C", 1)
    );

    public static Double decodeValue(String key, int[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        int a = bytes[0] & 0xFF;
        int b = bytes.length > 1 ? (bytes[1] & 0xFF) : 0;
        switch (key) {
            case "rpm":
                return ((a * 256) + b) / 4.0;
            case "speed":
            case "map":
                return (double) a;
            case "coolant":
            case "intake":
            case "ambient":
                return (double) (a - 40);
            case "load":
            case "throttle":
            case "fuel":
                return (a * 100.0) / 255.0;
            case "voltage":
                return ((a * 256) + b) / 1000.0;
            case "maf":
                return ((a * 256) + b) / 100.0;
            case "timing":
                return (a / 2.0) - 64.0;
            default:
                return null;
        }
    }

    public static int[] parseResponseBytes(String rawResponse, String command) {
        if (rawResponse == null || command == null || command.length() < 4) {
            return null;
        }
        String cleaned = rawResponse.replaceAll("[^0-9A-Fa-f]", "").toUpperCase(Locale.US);
        String expectedPrefix = "4" + command.substring(1, 4).toUpperCase(Locale.US);
        int idx = cleaned.indexOf(expectedPrefix);
        if (idx < 0) {
            return null;
        }
        String payload = cleaned.substring(idx + 4);
        int count = payload.length() / 2;
        if (count == 0) {
            return null;
        }
        int[] out = new int[count];
        for (int i = 0; i < count; i++) {
            try {
                out[i] = Integer.parseInt(payload.substring(i * 2, i * 2 + 2), 16);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return out;
    }
}
