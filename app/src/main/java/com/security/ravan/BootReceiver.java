package com.security.ravan;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        android.util.Log.d("BootReceiver", "onReceive: action=" + intent.getAction());
        
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) || 
            "android.intent.action.QUICKBOOT_POWERON".equals(intent.getAction())) {
            android.util.Log.d("BootReceiver", "Boot completed detected, starting HttpServerService");
            
            Intent serviceIntent = new Intent(context, HttpServerService.class);
            serviceIntent.setAction("START");

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent);
                } else {
                    context.startService(serviceIntent);
                }
                android.util.Log.d("BootReceiver", "HttpServerService started successfully");
            } catch (Exception e) {
                android.util.Log.e("BootReceiver", "Failed to start HttpServerService: " + e.getMessage());
                e.printStackTrace();
            }

            Intent watchdogIntent = new Intent(context, WatchdogService.class);
            context.startService(watchdogIntent);
        }
    }
}
