Veltrix is an AI-powered Android chat assistant that works online and offline — using OpenRouter (multi-model) via a secure FastAPI backend when connected, and switching to an on-device ML model when not.

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
| Auth | Firebase Auth |
| Online AI | OpenRouter via FastAPI backend |
| Offline AI | On-device ML (MediaPipe) |
| Backend | FastAPI + Firestore |

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
- `FIREBASE_SERVICE_ACCOUNT` (or `serviceAccount.json` for local)

## Environment

| File / var | Purpose |
|---|---|
| `google-services.json` | Firebase config (Android) |
| `OPENROUTER_API_KEY` | OpenRouter key on Render only |
| `FIREBASE_SERVICE_ACCOUNT` | Backend Firebase Admin credentials |

## Author

Saksham Kochhar

## License

MIT
