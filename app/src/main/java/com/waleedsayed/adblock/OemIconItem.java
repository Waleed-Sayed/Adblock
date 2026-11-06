package com.waleedsayed.adblock;

import android.graphics.drawable.Drawable;

public class OemIconItem {
    public final String id;
    public final String name;
    public final String brandKey;     // "samsung", "xiaomi", "honor"
    public final String brandName;    // "سامسونج (One UI)", "شاومي وبوكو (HyperOS)", "هونر (MagicOS)"
    public final String categoryKey;  // "apps", "status"
    public final String categoryName; // "تطبيقات النظام", "شريط الإشعارات والحالة"
    public final String iconKey;      // "settings", "camera", "wifi", etc.
    public final Drawable drawable;

    public OemIconItem(String id, String name, String brandKey, String brandName, String categoryKey, String categoryName, String iconKey, Drawable drawable) {
        this.id = id;
        this.name = name;
        this.brandKey = brandKey;
        this.brandName = brandName;
        this.categoryKey = categoryKey;
        this.categoryName = categoryName;
        this.iconKey = iconKey;
        this.drawable = drawable;
    }
}
