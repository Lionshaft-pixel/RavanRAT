package com.security.ravan;

public class NotificationRecord {
    private final long id;
    private final String packageName;
    private final String appName;
    private final String title;
    private final String text;
    private final long timestamp;

    public NotificationRecord(long id, String packageName, String appName, String title, String text, long timestamp) {
        this.id = id;
        this.packageName = packageName != null ? packageName : "";
        this.appName = appName != null ? appName : "";
        this.title = title != null ? title : "";
        this.text = text != null ? text : "";
        this.timestamp = timestamp;
    }

    public long getId() { return id; }
    public String getPackageName() { return packageName; }
    public String getAppName() { return appName; }
    public String getTitle() { return title; }
    public String getText() { return text; }
    public long getTimestamp() { return timestamp; }

    // Minimal JSON serialization with basic escaping
    public String toJson() {
        return "{\"id\":" + id
                + ",\"packageName\":\"" + jsonEscape(packageName) + "\""
                + ",\"appName\":\"" + jsonEscape(appName) + "\""
                + ",\"title\":\"" + jsonEscape(title) + "\""
                + ",\"text\":\"" + jsonEscape(text) + "\""
                + ",\"timestamp\":" + timestamp + "}";
    }

    private String jsonEscape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
