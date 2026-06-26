package com.security.ravan;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class ScreenCaptureService extends Service {

    private static final String TAG = "ScreenCaptureService";
    private static final String CHANNEL_ID = "ScreenCaptureChannel";
    private static final int NOTIFICATION_ID = 2;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForeground(NOTIFICATION_ID, createNotification());
            }
            Log.d(TAG, "Screen capture service started");
        } catch (Exception e) {
            Log.e(TAG, "Error starting foreground service", e);
        }
        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        "Screen Capture",
                        NotificationManager.IMPORTANCE_LOW);
                channel.setDescription("Screen capture in progress");
                channel.enableLights(false);
                channel.enableVibration(false);

                NotificationManager manager = getSystemService(NotificationManager.class);
                if (manager != null) {
                    manager.createNotificationChannel(channel);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error creating notification channel", e);
            }
        }
    }

    private Notification createNotification() {
        try {
            Intent notificationIntent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    this, 0, notificationIntent,
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

            return new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("📱 Screen Capture Active")
                    .setContentText("Recording screen...")
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentIntent(pendingIntent)
                    .setOngoing(true)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .build();
        } catch (Exception e) {
            Log.e(TAG, "Error creating notification", e);
            // Return a minimal notification if there's an error
            return new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("Screen Capture")
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .build();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        try {
            stopForeground(true);
            ScreenCaptureManager.stopProjection();
            Log.d(TAG, "Screen capture service destroyed");
        } catch (Exception e) {
            Log.e(TAG, "Error destroying service", e);
        }
        super.onDestroy();
    }
}
