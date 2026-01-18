# <img width="48" height="48" alt="icon-1024@1x" src="https://github.com/user-attachments/assets/18944d41-c2d1-4c5e-b97a-4598daef2454" /> BattMon

Real-time UPS monitoring with a Kotlin Multiplatform mobile app, push/email alerts, and history tracking.

<p align="center">
  <img width="20%" alt="IMG_8535" src="https://github.com/user-attachments/assets/f010145b-9944-48a8-9e8b-7fa63d196ebf" />
  <img width="20%" alt="IMG_8534" src="https://github.com/user-attachments/assets/3a0123b4-b987-4c83-8227-4aa49d21b997" />
  <img width="20%" alt="IMG_8532" src="https://github.com/user-attachments/assets/f096bbcf-20e6-400d-bdf1-693efa0fe214" />
  <img width="20%" alt="IMG_8536" src="https://github.com/user-attachments/assets/7dea26b4-061d-458b-972b-7d07056646ce" />
</p>

## Prerequisites

**The entire system depends on `apcaccess status` being available and working on the host machine.** This command is provided by [apcupsd](http://www.apcupsd.org/) and reads UPS data over USB/serial. Without it, BattMon has nothing to monitor.

Verify your setup:
```bash
apcaccess status
```

You should see output like `STATUS : ONLINE`, `BCHARGE : 100.0 Percent`, etc. If this fails, install and configure apcupsd first.

Other requirements: Docker, Python 3, Firebase project (for push notifications).

## Quick start

```bash
git clone git@github.com:szysz3/battmon.git
cd battmon
./setup.sh
```

The script verifies `apcaccess status` works, sets up configuration, starts an HTTP proxy for the backend to read UPS data, and launches Docker services.

Backend API runs on `http://localhost:8080`.

### Platform notes

The backend uses `network_mode: host` to reach the apcaccess proxy on the host. This requires Linux. On macOS/Windows, switch to bridge networking and set `UPS_COMMAND` to `http://host.docker.internal:8081/apcaccess`.

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
