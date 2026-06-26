package com.security.ravan;

import android.app.Notification;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import androidx.annotation.RequiresApi;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class NotificationListener extends NotificationListenerService {

    private static final String TAG = "NotificationListener";

    @Override
    public void onCreate() {
        super.onCreate();
        NotificationStore.init(getApplicationContext());
        Log.d(TAG, "onCreate: NotificationListener created");
    }

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        Log.d(TAG, "onListenerConnected: notification listener service connected");
    }

    @Override
    public void onListenerDisconnected() {
        super.onListenerDisconnected();
        Log.d(TAG, "onListenerDisconnected: notification listener service disconnected");
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        Log.d(TAG, "onNotificationPosted entry: " + sbn.getPackageName() + " / id=" + sbn.getId() + " / key=" + sbn.getKey());
        try {
            super.onNotificationPosted(sbn);
            Notification n = sbn.getNotification();
            if (n == null) {
                Log.w(TAG, "onNotificationPosted: notification object is null");
                return;
            }
            CharSequence titleCs = null;
            CharSequence textCs = null;
            if (n.extras != null) {
                titleCs = n.extras.getCharSequence(Notification.EXTRA_TITLE);
                CharSequence titleCsAlt = n.extras.getCharSequence(Notification.EXTRA_TITLE);
                if (titleCs == null && titleCsAlt != null) {
                    titleCs = titleCsAlt;
                }
                textCs = n.extras.getCharSequence(Notification.EXTRA_TEXT);
            }
            String title = titleCs != null ? titleCs.toString() : "";
            String text = textCs != null ? textCs.toString() : "";
            if (title.isEmpty() && text.isEmpty()) {
                Log.w(TAG, "onNotificationPosted: no title/text found for notification from " + sbn.getPackageName());
                return;
            }
            String pkg = sbn.getPackageName();
            String appName = pkg;
            try {
                Context ctx = getApplicationContext();
                PackageManager pm = ctx.getPackageManager();
                android.content.pm.ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
                CharSequence label = pm.getApplicationLabel(ai);
                if (label != null) {
                    appName = label.toString();
                }
            } catch (Exception e) {
                Log.w(TAG, "Unable to resolve app name for package " + pkg, e);
            }
            long ts = System.currentTimeMillis();
            NotificationRecord rec = new NotificationRecord(ts, pkg, appName, title, text, ts);
            NotificationStore.add(rec);
            Log.d(TAG, "Notification stored: " + rec.toJson());
        } catch (Exception e) {
            Log.e(TAG, "Error in onNotificationPosted", e);
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        Log.d(TAG, "onNotificationRemoved entry: " + sbn.getPackageName() + " / id=" + sbn.getId() + " / key=" + sbn.getKey());
        try {
            super.onNotificationRemoved(sbn);
            Log.d(TAG, "onNotificationRemoved handled");
        } catch (Exception e) {
            Log.e(TAG, "Error in onNotificationRemoved", e);
        }
    }
}
