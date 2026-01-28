# <img width="48" height="48" alt="icon-1024@1x" src="https://github.com/user-attachments/assets/18944d41-c2d1-4c5e-b97a-4598daef2454" /> BattMon

Real-time UPS monitoring with a Kotlin Multiplatform mobile app, push/email alerts, and history tracking.

<p align="center">
  <img width="20%" alt="IMG_8535" src="https://github.com/user-attachments/assets/f010145b-9944-48a8-9e8b-7fa63d196ebf" />
  <img width="20%" alt="IMG_8534" src="https://github.com/user-attachments/assets/3a0123b4-b987-4c83-8227-4aa49d21b997" />
  <img width="20%" alt="IMG_8532" src="https://github.com/user-attachments/assets/f096bbcf-20e6-400d-bdf1-693efa0fe214" />
  <img width="20%" alt="IMG_8536" src="https://github.com/user-attachments/assets/7dea26b4-061d-458b-972b-7d07056646ce" />
</p>

## Prerequisites

**BattMon connects to UPS devices via apcupsd Network Information Server (NIS).** Each UPS host must run [apcupsd](http://www.apcupsd.org/) with NIS enabled (default port 3551).

BattMon expects `apcaccess` to run on the host, not inside the Docker container. Run the host-side proxy (`scripts/apcaccess-proxy.py`) and set `UPS_APCACCESS_PROXY_URL` (default `http://localhost:8083` in docker-compose).

Verify connectivity from the host running the proxy:
```bash
apcaccess -h <host>:3551 status
```

You should see output like `STATUS : ONLINE`, `BCHARGE : 100.0 Percent`, etc. If this fails, configure apcupsd on the UPS host and ensure port 3551 is reachable.

Other requirements: Docker, Python 3, Firebase project (for push notifications).

## Quick start

```bash
git clone git@github.com:szysz3/battmon.git
cd battmon
./setup.sh
```

The script sets up configuration and launches Docker services. After the backend is running, add devices via the mobile app or the `/devices` API.

Backend API runs on `http://localhost:8080`.

## Services and runtime flow

BattMon relies on three running pieces:

1) **apcupsd NIS on each UPS host**  
   Each UPS host must run apcupsd with NIS enabled (default port 3551).

2) **Host-side apcaccess proxy**  
   The proxy runs on the backend host and executes `apcaccess` against each UPS host.

   - Install via the setup script (systemd):
     ```bash
     ./setup.sh
     ```
   - Or run manually:
     ```bash
     python3 scripts/apcaccess-proxy.py
     ```
   - Verify:
     ```bash
     curl http://localhost:8083/health
     ```

   The backend talks to the proxy via `UPS_APCACCESS_PROXY_URL` (default `http://localhost:8083`).

3) **Backend + database**  
   Docker Compose starts Postgres and the backend:
   ```bash
   docker compose up -d
   ```

### Multi-UPS behavior

Each device has its own `host` and `port` (the apcupsd NIS endpoint). The backend uses a **single**
proxy URL and routes requests by passing `host`/`port` to the proxy. View devices via:

```bash
curl http://localhost:8080/devices
curl http://localhost:8080/status/latest
```

### Automatic DB cleanup (multi-UPS safe)

The backend runs a retention job that deletes old status rows across **all devices** by timestamp
using the defaults in `backend/src/main/resources/application.yaml`.

### Platform notes

The backend must be able to reach each UPS host on port 3551. On Docker Desktop (macOS/Windows), ensure containers can reach your LAN or run the backend directly on the host.

## Mobile app

Update your server IP in:
- `mobile/common/src/commonMain/kotlin/com/battmon/config/AppConfig.kt`
- `mobile/android/src/main/res/xml/network_security_config.xml`
- `mobile/ios/ios-battmon/Info.plist`

Add Firebase config files (`google-services.json` for Android, `GoogleService-Info.plist` for iOS).

```bash
# Android
./gradlew :mobile:android:assembleDebug

# iOS
open mobile/ios/ios-battmon.xcodeproj
```

## License

MIT
