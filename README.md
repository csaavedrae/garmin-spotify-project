# 🎵 Garmin-Spotify Connect Remote

A full-stack, open-source companion system bridging a **Garmin Smartwatch** to an **Android Device** to unlock low-latency, hardware-level remote control over Spotify media playback via a localized Bluetooth data pipeline.

---

## 📌 Data Flow

```text
+-----------------------+   Bluetooth LE   +----------------------------+   Local Intent   +----------------------------+
|  Garmin Smartwatch    | ---------------> |     Android Companion      | ---------------> |    Official Spotify App    |
|  (Monkey C Watch App) |   Data Packet    |   (Java Background App)    |   Intercept      |    (Local Media Engine)    |
+-----------------------+                  +----------------------------+                  +----------------------------+

```

1. **Garmin Client:** Captures hardware input (`PLAY`, `NEXT`, `VOL_UP`) and broadcasts string tokens over BLE.
2. **Android Bridge:** Listens via the Connect IQ Companion SDK, decodes payloads, and triggers localized intents.
3. **Spotify Target:** Executes media state updates instantly using the Spotify App Remote SDK.

---

## 📂 Project Structure

```text
garmin-spotify-project/                 # Repository Root
│
├── .gitignore                          # Global build cache exclusions
├── README.md                           # Project documentation
│
├── spotify-remote-phone/               # Android Mobile Bridge (Java 17 / Gradle 9.x)
│   └── app/
│       ├── src/main/AndroidManifest.xml # Hardware permissions & OAuth filters
│       └── src/main/java/.../MainActivity.java # App logic & Bluetooth listener
│
└── spotify-remote-watch/               # Garmin Wearable App (Monkey C / CIQ 4.x+)
    ├── source/                         # Application, View, and Input Delegate handlers
    ├── resources/                      # Layout assets, drawables, and strings
    └── manifest.xml                    # App UUID and device permissions profile

```

---

## ⚙️ Quick Start Setup

### 1. Spotify Developer Registration

1. Head to the **Spotify Developer Dashboard** and create an app profile.
2. Set the **Redirect URI** to: `com.example.spotifyremote://callback`
3. Copy your generated `Client ID`, open your `MainActivity.java`, and update the config:
```java
private static final String SPOTIFY_CLIENT_ID = "YOUR_CLIENT_ID_HERE";

```



### 2. Compiling the Mobile Bridge

Run the following commands in your integrated terminal from the `spotify-remote-phone` directory:

```powershell
# Clear local engine cache daemons
.\gradlew.bat --stop

# Fetch Garmin dependencies, compile project, and sideload to your device
.\gradlew.bat app:installDebug

```

### 3. Deploying the Watch App

1. Open the project root in VS Code with the official **Garmin Connect IQ Extension** active.
2. Select your device target (e.g., Fenix, Forerunner, Venu).
3. Open the command palette (`Ctrl+Shift+P` / `Cmd+Shift+P`) ➡️ **Connect IQ: Build Project**, then run the emulator to verify the pipeline.

---

## 📄 License & Terms

Configured strictly for non-commercial, localized hobby development and prototyping. Integration frameworks are property of **Garmin Ltd.** and **Spotify Technology S.A.**
