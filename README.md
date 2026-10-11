Cherry AI is an AI-powered Android chat assistant that works online and offline — using OpenRouter (multi-model) via a secure FastAPI backend when connected, and switching to an on-device ML model when not.

## Features

- Online Mode — OpenRouter multi-model chat (GPT, Claude, Gemini, Kimi, MiniMax, and more) with server-side cost metering
- Offline Mode — On-device ML model with zero internet dependency
- Firebase Authentication — Secure user login and session management
- Brainstorming / Learning / Coding modes
- Jetpack Compose + StateFlow UI

## Tech stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose |
| Architecture | MVVM + StateFlow |
| Auth | Firebase Auth (login, signup, password reset, Google) |
| Online AI | OpenRouter via FastAPI backend |
| Account security | FastAPI `/v1/security/*` + Firestore via Admin SDK on Render |
| Notifications | FastAPI `/v1/notifications/*` + FCM (data-only) from Render |
| Offline AI | On-device ML (MediaPipe) |
| Backend | FastAPI + Firestore (Admin on server) |

## Getting started

### Prerequisites

- Android Studio Hedgehog or newer
- Device / emulator API 30+
- Firebase project
- OpenRouter API key stored on the **backend** (Render), not in the Android app

### Android

1. Clone the repo
2. Add `google-services.json` to `/app`
3. Open in Android Studio and Run

### Backend

See [Backend/veltrix-backend-main/veltrix-backend-main/README.md](Backend/veltrix-backend-main/veltrix-backend-main/README.md).

On Render, set at least:

- `OPENROUTER_API_KEY`
- `GEMINI_API_KEY`
- `FIREBASE_SERVICE_ACCOUNT` (or `serviceAccount.json` for local)
- `FCM_ENABLED` (default `true`), `CHAT_PUSH_MIN_SECONDS` (default `8`)
- `CRON_SECRET`, `ADMIN_PROMO_SECRET` (internal plan-expiry and promo jobs)
- `EMAIL_ENABLED` (default `false`), `EMAIL_PROVIDER`, `EMAIL_FROM` — see [docs/EMAIL_SETUP.md](docs/EMAIL_SETUP.md)
- `RESEND_API_KEY` (when `EMAIL_PROVIDER=resend` and email is enabled)

## Environment

| File / var | Purpose |
|---|---|
| `google-services.json` | Firebase config (Android) |
| `OPENROUTER_API_KEY` | OpenRouter key on Render only |
| `FIREBASE_SERVICE_ACCOUNT` | Backend Firebase Admin credentials |
| `EMAIL_ENABLED` / `EMAIL_PROVIDER` / `EMAIL_FROM` | Optional security/notification email (Render) |
| `RESEND_API_KEY` | Resend adapter when email is enabled |
| `serviceAccount.json` | Local Firebase Admin only (gitignored) |

## Author

Saksham Kochhar

## License

MIT
