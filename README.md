# 🎵 Garmin Spotify Remote

> ⚠️ **Disclaimer — This is an experimental learning project.**
> Garmin already ships a native **Music Controls** widget on supported watches (Fenix, Venu, Forerunner, and others). You do **not** need this app to control Spotify from your wrist. This project was built as a personal engineering challenge to understand the Connect IQ SDK, the Spotify App Remote SDK, and Bluetooth communication between a wearable and an Android companion app. The journey was the point.

---

A full-stack companion system that bridges a **Garmin Smartwatch** (Monkey C / Connect IQ) with an **Android phone** to remotely control Spotify playback over Bluetooth — built for learning, not production.

---

## 📐 Architecture & Data Flow

```
┌──────────────────────┐   BLE (Connect IQ)   ┌──────────────────────┐   App Remote SDK   ┌──────────────────┐
│  Garmin Watch App    │ ──────────────────▶  │  Android Companion   │ ─────────────────▶ │  Spotify App     │
│  (Monkey C / CIQ 4) │  {"action":"PLAY"}   │  (Java / Android)    │  local intent      │  (on device)     │
│                      │ ◀──────────────────  │                      │                    │                  │
└──────────────────────┘  track name + artist └──────────────────────┘                    └──────────────────┘
```

**Watch → Phone:** The watch sends string-keyed command payloads (`PLAY_PAUSE`, `NEXT_TRACK`, `PREV_TRACK`, `VOLUME_UP`, `VOLUME_DOWN`) via the Connect IQ Communications API.

**Phone → Spotify:** The Android companion receives commands through the Garmin ConnectIQ SDK and executes them using the Spotify App Remote SDK (local, no network required).

**Phone → Watch:** The phone subscribes to Spotify's player state and pushes the current track name + artist back to the watch display in real time.

---

## 📂 Project Structure

```
garmin-spotify-project/
│
├── README.md
├── .gitignore
│
├── spotify-remote-phone/               # Android companion app (Java 17 / Gradle)
│   └── app/
│       ├── build.gradle                # Dependencies: Garmin CIQ SDK, Spotify AAR, Gson
│       └── src/main/
│           ├── AndroidManifest.xml
│           └── java/.../MainActivity.java  # ConnectIQ + SpotifyAppRemote bridge
│
└── spotify-remote-watch/               # Garmin Connect IQ watch app (Monkey C)
    ├── manifest.xml                    # App UUID, device targets, permissions
    └── source/
        ├── spotify-remoteApp.mc        # App entry point
        ├── spotify-remoteView.mc       # Main screen: track name, artist, play/pause indicator
        ├── spotify-remoteDelegate.mc   # Button handling + BLE transmit
        ├── VolumeTouchView.mc          # Secondary screen: volume +/- and track skip UI
        ├── VolumeTouchDelegate.mc      # Touch + button handling for volume screen
        └── spotify-remoteMenuDelegate.mc  # Menu scaffold (extensible)
```

---

## ⚙️ Setup

### Prerequisites

- A **Garmin watch** with Connect IQ 4.0+ support (this repo targets the Venu Sq 2)
- An **Android phone** with Spotify installed and a **Spotify Premium** account
- [VS Code](https://code.visualstudio.com/) + [Garmin Monkey C extension](https://marketplace.visualstudio.com/items?itemName=Garmin.monkey-c)
- [Android Studio](https://developer.android.com/studio) or Gradle CLI
- A [Spotify Developer account](https://developer.spotify.com/dashboard)

---

### 1. Register a Spotify App

1. Go to the [Spotify Developer Dashboard](https://developer.spotify.com/dashboard) and create a new app.
2. Under **Redirect URIs**, add: `com.example.spotifyremote://callback`
3. Copy your **Client ID**.

---

### 2. Configure the Android App

The project reads your Spotify Client ID from `local.properties` (which is excluded from version control). Create this file in `spotify-remote-phone/`:

```
# spotify-remote-phone/local.properties
SPOTIFY_CLIENT_ID=your_client_id_here
```

> **Never hardcode your Client ID directly in `MainActivity.java`.** The `build.gradle` is already set up to inject it at build time via `BuildConfig.SPOTIFY_CLIENT_ID`. Update the constant in `MainActivity.java` to use `BuildConfig.SPOTIFY_CLIENT_ID` instead of the hardcoded string.

You will also need to place the Spotify App Remote SDK (`.aar` file) in `spotify-remote-phone/app/libs/`. Download it from the [Spotify Android SDK releases](https://github.com/spotify/android-sdk).

---

### 3. Build & Install the Android App

From the `spotify-remote-phone/` directory:

```bash
# Stop any running Gradle daemons
./gradlew --stop

# Build and sideload to a connected device
./gradlew app:installDebug
```

Enable **USB debugging** on your Android phone before running.

---

### 4. Build & Deploy the Watch App

1. Open the `spotify-remote-watch/` folder in VS Code with the Monkey C extension active.
2. Open the command palette (`Ctrl+Shift+P` / `Cmd+Shift+P`) and run **Monkey C: Build Project**.
3. To test in the simulator: **Monkey C: Run in Simulator**.
4. To sideload to a physical watch: connect via USB and copy the compiled `.prg` to the `GARMIN/APPS/` directory on the device.

> The `manifest.xml` currently targets **Venu Sq 2** only. To support other devices, open the command palette and use **Monkey C: Edit Products**.

---

## 🕹️ How to Use

### Main Screen (Watch)
| Input | Action |
|---|---|
| **Select button** | Play / Pause |
| **Down button** | Navigate to Volume & Skip screen |

The display shows the current track name and artist (pushed from the phone in real time), and a green play/pause indicator.

### Volume & Skip Screen
| Input | Action |
|---|---|
| **Tap top half of screen** | Volume Up |
| **Tap bottom half of screen** | Volume Down |
| **Select button** | Next Track |
| **Down button** | Previous Track |
| **Back button** | Return to main screen |

> **Note:** Volume control commands are sent from the watch but the current Android companion does not yet implement a handler for `VOLUME_UP` / `VOLUME_DOWN` in `MainActivity.java`. This is a known gap — contributions welcome.

---

## 🔒 Security Notes

### What's safe to publish
- **Garmin App UUID** (`bd3245ea-175c-451c-b85c-58e898904fb9`) — this is a Connect IQ channel identifier, not a secret. It needs to match between `manifest.xml` and `MainActivity.java` to work, and publishing it is standard practice.
- **Spotify Client ID** — despite looking like a secret, the Client ID is a *public* app identifier (like a username). Spotify does not treat it as sensitive and it appears openly in their dashboard. The actual secret is the **Client Secret**, which is nowhere in this repo.
- No OAuth tokens, refresh tokens, or user credentials are stored anywhere in the codebase.
- No `.keystore` or Android signing credentials are committed.

### What to keep out of version control
- **`local.properties`** — Android Studio adds this to `.gitignore` by default. This is where your `SPOTIFY_CLIENT_ID` should live when following the `BuildConfig` pattern, even though the Client ID itself isn't sensitive. It's a good habit for any credentials-shaped value.
- **`spotify-app-remote-release-x.x.x.aar`** — the Spotify SDK binary in `libs/`. It's referenced locally in `build.gradle` but should not be committed (Spotify's terms require users to download it themselves from the developer dashboard).

### Known code quality note
The `SPOTIFY_CLIENT_ID` is currently hardcoded as a string directly in `MainActivity.java`. The `build.gradle` is already set up to inject it via `BuildConfig` — the plumbing is there, the last step is missing. To complete it, replace the hardcoded line with `BuildConfig.SPOTIFY_CLIENT_ID` and store the value in `local.properties`. This is a cleanliness improvement, not a security emergency.

### Package name
The app uses `com.example.spotifyremote` as its package namespace. This is intentional for a prototype — if you ever publish to the Play Store, you would need a unique reverse-domain package name.

---

## 🧠 What I Learned

This project was built to understand:

- The **Garmin Connect IQ SDK** and Monkey C language
- Two-way **BLE communication** between a wearable and Android via the CIQ Companion SDK
- The **Spotify App Remote SDK** for local (non-network) media control
- Multi-view navigation in Connect IQ (view stack with `pushView` / `popView`)
- Separating credentials from code using `BuildConfig` and `local.properties`

And the humbling discovery at the end: Garmin already ships a native Music Controls widget. Read the [lessons learned post on LinkedIn](#) for the full story.

---

## 📄 License
This project is for educational and non-commercial use only. The Connect IQ SDK is property of Garmin Ltd. The Spotify App Remote SDK is property of Spotify Technology S.A. Refer to their respective developer terms before building on top of this.

This project is for educational and non-commercial use only. The Connect IQ SDK is property of **Garmin Ltd.** The Spotify App Remote SDK is property of **Spotify Technology S.A.** Refer to their respective developer terms before building on top of this.
