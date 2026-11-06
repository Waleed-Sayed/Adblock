package com.waleedsayed.adblock;

public class SetMockActionActivity extends BaseShizukuActionActivity {
    @Override
    protected String getShellCommand() {
        return "appops set " + getPackageName() + " android:mock_location allow";
    }

    @Override
    protected String getSuccessMessage() {
        return "تم تعيين هذا التطبيق للموقع الوهمي بنجاح";
    }
}
