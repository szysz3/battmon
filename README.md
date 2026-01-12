# BattMon

Real-time UPS monitoring with a Kotlin Multiplatform (Compose) mobile app, push/email alerts, and history tracking.

## Screenshots

<p align="center">
  <img src="docs/screenshot-1.png" alt="BattMon iOS App" width="45%" />
  <img src="docs/screenshot-2.png" alt="Notification" width="45%" />
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

## Project layout

- `backend/` Ktor API + UPS polling
- `mobile/` Kotlin Multiplatform app (iOS + Android, Compose)
- `shared/` shared models

## License

MIT
