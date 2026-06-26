package com.security.ravan;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import java.net.Socket;

public class WatchdogService extends Service {
    private static final String TAG = "WatchdogService";
    private static final int CHECK_INTERVAL = 30000;
    private Handler handler;
    private Runnable checkTask;
    private boolean watchdogScheduled = false;

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler();
        checkTask = new Runnable() {
            @Override
            public void run() {
                if (!isServerRunning()) {
                    Log.d(TAG, "Server not running, restarting...");
                    Intent intent = new Intent(WatchdogService.this, HttpServerService.class);
                    intent.setAction("START");
                    startService(intent);
                }
                handler.postDelayed(this, CHECK_INTERVAL);
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!watchdogScheduled) {
            watchdogScheduled = true;
            handler.post(checkTask);
        }
        return START_STICKY;
    }

    private boolean isServerRunning() {
        try {
            Socket socket = new Socket("127.0.0.1", 8080);
            socket.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void onDestroy() {
        watchdogScheduled = false;
        handler.removeCallbacks(checkTask);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
