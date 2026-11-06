package com.waleedsayed.adblock;

public class ResetMockActionActivity extends BaseShizukuActionActivity {
    @Override
    protected String getShellCommand() {
        return "appops set " + getPackageName() + " android:mock_location deny";
    }

    @Override
    protected String getSuccessMessage() {
        return "تم تعيين تطبيق الموقع الوهمي إلى: لا شيء";
    }
}
