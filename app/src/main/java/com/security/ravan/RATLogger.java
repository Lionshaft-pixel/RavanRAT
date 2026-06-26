package com.security.ravan;

import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.Deque;
import java.util.List;
import java.util.Locale;

public class RATLogger {

    private static final int MAX_ENTRIES = 200;
    private static final Deque<String> entries = new ArrayDeque<>(MAX_ENTRIES);
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss", Locale.US);

    public static synchronized void log(String message) {
        if (message == null) {
            message = "null";
        }
        String timestamp = TIME_FORMAT.format(new Date());
        if (entries.size() >= MAX_ENTRIES) {
            entries.removeFirst();
        }
        entries.addLast("[" + timestamp + "] " + message);
    }

    public static synchronized List<String> getLogs() {
        return new ArrayList<>(entries);
    }

    public static synchronized void clearLogs() {
        entries.clear();
    }
}
