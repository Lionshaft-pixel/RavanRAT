package com.security.ravan;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NotificationStore {
    private static final int MAX_SIZE = 500;
    private static final String FILE_NAME = "notifications.json";
    private static final String TAG = "NotificationStore";
    private static final List<NotificationRecord> store = Collections.synchronizedList(new ArrayList<NotificationRecord>());
    private static File storeFile;
    private static boolean initialized = false;

    public static synchronized void init(Context context) {
        Log.d(TAG, "init() called");
        if (initialized) {
            Log.d(TAG, "init() skipped because already initialized");
            return;
        }
        try {
            storeFile = new File(context.getFilesDir(), FILE_NAME);
            loadFromFile();
            initialized = true;
            Log.d(TAG, "init() completed");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize notification store", e);
        }
    }

    private static void loadFromFile() {
        synchronized (store) {
            store.clear();
            if (storeFile == null) {
                Log.d(TAG, "loadFromFile() skipped because storeFile is null");
                return;
            }
            Log.d(TAG, "loadFromFile() file exists: " + storeFile.exists() + ", size: " + storeFile.length());
            if (!storeFile.exists()) {
                return;
            }
            try (FileInputStream fis = new FileInputStream(storeFile)) {
                byte[] data = new byte[(int) storeFile.length()];
                int read = fis.read(data);
                if (read <= 0) {
                    Log.d(TAG, "loadFromFile() read no data or empty file");
                    return;
                }
                String content = new String(data, StandardCharsets.UTF_8);
                Log.d(TAG, "JSON: " + content);
                JSONArray array;
                try {
                    array = new JSONArray(content);
                } catch (Exception ex) {
                    Log.d(TAG, "JSON is not an array; attempting to parse single object");
                    JSONObject obj = new JSONObject(content);
                    array = new JSONArray();
                    array.put(obj);
                }
                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);
                    Log.d(TAG, "Parsing notification " + i + ": " + obj.toString());
                    NotificationRecord record = new NotificationRecord(
                            obj.optLong("id"),
                            obj.optString("packageName", ""),
                            obj.optString("appName", ""),
                            obj.optString("title", ""),
                            obj.optString("text", ""),
                            obj.optLong("timestamp"));
                    store.add(record);
                }
                trimIfNecessary();
                Log.d(TAG, "Loaded " + store.size() + " notifications");
            } catch (Exception e) {
                Log.e(TAG, "Failed to load notifications from file", e);
            }
        }
    }

    private static void persistToFile() {
        if (storeFile == null) {
            return;
        }
        synchronized (store) {
            try (FileOutputStream fos = new FileOutputStream(storeFile, false)) {
                JSONArray array = new JSONArray();
                for (NotificationRecord record : store) {
                    JSONObject obj = new JSONObject();
                    obj.put("id", record.getId());
                    obj.put("packageName", record.getPackageName());
                    obj.put("appName", record.getAppName());
                    obj.put("title", record.getTitle());
                    obj.put("text", record.getText());
                    obj.put("timestamp", record.getTimestamp());
                    array.put(obj);
                }
                fos.write(array.toString().getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                Log.e(TAG, "Failed to persist notifications to file", e);
            }
        }
    }

    private static void trimIfNecessary() {
        while (store.size() > MAX_SIZE) {
            store.remove(store.size() - 1);
        }
    }

    public static void add(NotificationRecord record) {
        synchronized (store) {
            store.add(0, record);
            trimIfNecessary();
            persistToFile();
        }
    }

    public static List<NotificationRecord> getAll() {
        synchronized (store) {
            return new ArrayList<>(store);
        }
    }

    public static void clear() {
        synchronized (store) {
            store.clear();
            persistToFile();
        }
    }

    public static boolean removeById(long id) {
        synchronized (store) {
            for (int i = 0; i < store.size(); i++) {
                if (store.get(i).getId() == id) {
                    store.remove(i);
                    persistToFile();
                    return true;
                }
            }
            return false;
        }
    }
}
