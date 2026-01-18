# <img width="48" height="48" alt="icon-1024@1x" src="https://github.com/user-attachments/assets/18944d41-c2d1-4c5e-b97a-4598daef2454" /> BattMon

Real-time UPS monitoring with a Kotlin Multiplatform (Compose) mobile app, push/email alerts, and history tracking.

## Screenshots

<p align="center">
  <img width="20%" width="590" height="1278" alt="IMG_8535" src="https://github.com/user-attachments/assets/f010145b-9944-48a8-9e8b-7fa63d196ebf" />
  <img width="20%" width="590" height="1278" alt="IMG_8534" src="https://github.com/user-attachments/assets/3a0123b4-b987-4c83-8227-4aa49d21b997" />
  <img width="20%" width="590" height="1278" alt="IMG_8532" src="https://github.com/user-attachments/assets/f096bbcf-20e6-400d-bdf1-693efa0fe214" />
  <img width="20%" width="590" height="1278" alt="IMG_8536" src="https://github.com/user-attachments/assets/7dea26b4-061d-458b-972b-7d07056646ce" />  
</p>

## What it does

- Monitors UPS status via apcupsd
- Sends push (FCM) and email notifications for power events
- Stores history in PostgreSQL and serves it via a Ktor API
- Mobile app runs on iOS and Android (Kotlin Multiplatform / Compose)

## Quick start

Prerequisites: Docker, Python 3, apcupsd, Firebase project.

```bash
git clone git@github.com:szysz3/battmon.git
cd battmon
./setup.sh
```

Backend runs on `http://localhost:8080`.

## Configuration

Edit `.env` to configure polling and email settings:

```bash
UPS_POLL_INTERVAL=5
EMAIL_ENABLED=true
SMTP_USERNAME=your@gmail.com
SMTP_PASSWORD=app-password
EMAIL_TO=recipient@example.com
```

## Mobile App Configuration

Before building the mobile app, you need to configure a few files:

### 1. Set your server IP address

Update the following files with your BattMon server's IP address:

**AppConfig.kt** (`mobile/common/src/commonMain/kotlin/com/battmon/config/AppConfig.kt`):
```kotlin
const val API_BASE_URL = "http://YOUR_SERVER_IP:8080"
```

**Android** (`mobile/android/src/main/res/xml/network_security_config.xml`):
```xml
<domain includeSubdomains="true">YOUR_SERVER_IP</domain>
```

**iOS** (`mobile/ios/ios-battmon/Info.plist`):
```xml
<key>YOUR_SERVER_IP</key>
```

### 2. Firebase Configuration (for push notifications)

**Android:**
1. Create a Firebase project at [Firebase Console](https://console.firebase.google.com)
2. Add an Android app with package name `com.battmon.android`
3. Download `google-services.json` and place it in `mobile/android/`

**iOS:**
1. Add an iOS app to your Firebase project
2. Download `GoogleService-Info.plist` and add it to the Xcode project

**Backend:**
1. Generate a service account key from Firebase Console
2. Save it as `backend/firebase-credentials.json`

### 3. Build the app

```bash
# Android
./gradlew :mobile:android:assembleDebug

# iOS - open in Xcode
open mobile/ios/ios-battmon.xcodeproj
```

## Project layout

- `backend/` Ktor API + UPS polling
- `mobile/` Kotlin Multiplatform app (iOS + Android, Compose)
- `shared/` shared models

## License

MIT
