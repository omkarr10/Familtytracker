# TrackIt 🔐

<div align="center">
  <img src="dashboard/public/logo.jpeg" alt="TrackIt Logo" width="150" height="150" style="border-radius: 30px;">
  
  <h1>Family Safety & Anti-Theft System</h1>
  
  <p><strong>The most powerful open-source family tracker with military-grade anti-theft protection</strong></p>
  
  <p>Track your family. Catch thieves. Works offline. No cloud lock-in.</p>

  <br/>
  
  [![License: MIT](https://img.shields.io/badge/License-MIT-green.svg?style=for-the-badge)](https://opensource.org/licenses/MIT)
  [![Android](https://img.shields.io/badge/Android-13+-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com)
  [![React](https://img.shields.io/badge/React-18.2-61DAFB?style=for-the-badge&logo=react&logoColor=black)](https://reactjs.org/)
  [![TypeScript](https://img.shields.io/badge/TypeScript-5.0-3178C6?style=for-the-badge&logo=typescript&logoColor=white)](https://www.typescriptlang.org/)
  [![Kotlin](https://img.shields.io/badge/Kotlin-1.9-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
  [![Supabase](https://img.shields.io/badge/Supabase-Realtime-3ECF8E?style=for-the-badge&logo=supabase&logoColor=white)](https://supabase.com/)
  
  <br/>
  
  [📱 Download APK](https://familytracker.vercel.app/TrackIt.apk) · [🌐 Live Demo](https://familytracker.vercel.app) · [📖 Documentation](#-getting-started)
  
</div>

---

<br/>

<div align="center">
  <table>
    <tr>
      <td align="center"><img src="https://img.icons8.com/fluency/48/map-marker.png" width="32"/><br/><b>Real-Time GPS</b><br/><sub>Track family 24/7</sub></td>
      <td align="center"><img src="https://img.icons8.com/fluency/48/camera.png" width="32"/><br/><b>Thief Selfie</b><br/><sub>Auto photo capture</sub></td>
      <td align="center"><img src="https://img.icons8.com/fluency/48/sim-card.png" width="32"/><br/><b>SIM Detection</b><br/><sub>Instant alerts</sub></td>
      <td align="center"><img src="https://img.icons8.com/fluency/48/sms.png" width="32"/><br/><b>SMS Fallback</b><br/><sub>Works offline</sub></td>
      <td align="center"><img src="https://img.icons8.com/fluency/48/shake-phone.png" width="32"/><br/><b>Shake SOS</b><br/><sub>3 shakes = help</sub></td>
    </tr>
  </table>
</div>

<br/>

---

## 🔥 Why TrackIt?

| Feature | TrackIt | Life360 | Find My | Google Find |
|---------|:-------:|:-------:|:-------:|:-----------:|
| **100% Free** | ✅ | ❌ | ✅ | ✅ |
| **Open Source** | ✅ | ❌ | ❌ | ❌ |
| **Self-Hostable** | ✅ | ❌ | ❌ | ❌ |
| **Works Offline (SMS)** | ✅ | ❌ | ❌ | ❌ |
| **Thief Photo Capture** | ✅ | ❌ | ❌ | ❌ |
| **SIM Removal Alert** | ✅ | ❌ | ❌ | ❌ |
| **Remote Alarm** | ✅ | ❌ | ✅ | ✅ |
| **Shake-to-SOS** | ✅ | ❌ | ❌ | ❌ |
| **Speed Alerts** | ✅ | ✅ | ❌ | ❌ |
| **No Ads** | ✅ | ❌ | ✅ | ✅ |
| **No Data Selling** | ✅ | ❌ | ? | ❌ |

---

## ✨ Features

### 📍 Location Tracking
- **Real-time GPS** - See family members on a live map
- **Location History** - View where they've been with timeline
- **Multiple Map Styles** - Standard, Satellite, Dark mode
- **Battery & Speed Display** - Monitor device status
- **Geofencing** - Get alerts when entering/leaving zones

### 🛡️ Anti-Theft Arsenal

<table>
<tr>
<td width="50%">

**🎭 Intruder Selfie**
- Wrong PIN? Front camera silently captures photo
- Auto-uploads to dashboard
- Works even if phone is locked

**📵 SIM Removal Detection**
- Instant SMS alert to emergency contacts
- Last GPS location included
- Works without internet

**🔊 Remote Alarm**
- 100% volume even on silent mode
- Cannot be stopped without PIN
- Loops until you disable remotely

</td>
<td width="50%">

**🔒 Remote Lock**
- Lock device from anywhere
- Custom lock message
- Stealth mode (screen appears off)

**📱 SMS Commands**
- Control phone without internet
- Location, Alarm, Photos, Lock
- Secret code protected

**🆘 Shake-to-SOS**
- Shake phone 3x = emergency alert
- Sends location to all contacts
- Triggers burst tracking mode

</td>
</tr>
</table>

### ⚡ Battery Optimized

Built for 24/7 operation with minimal battery drain:

```
Normal Mode:    ~3-5% battery per hour
Theft Mode:     ~8-10% battery per hour (burst tracking)
Always-On:      ~15-20% per day in standby
```

**Optimizations include:**
- ⚙️ Adaptive GPS intervals (5 min idle → 15 sec during movement)
- 🔋 Balanced power accuracy (not high accuracy)
- 📡 Sensor throttling for shake detection
- 📦 Batched location updates

---

## 📱 SMS Commands

When internet fails, control via SMS:

```bash
TRACKIT LOCATE    → Returns GPS coordinates + Google Maps link
TRACKIT ALARM     → Triggers loud alarm at max volume
TRACKIT CAPTURE   → Takes front & back camera photos
TRACKIT LOCK      → Remotely locks the device
TRACKIT THEFT     → Activates theft mode (rapid tracking)
```

> 💡 Send from an authorized number. Default secret code: `TRACKIT`

---

## 🏗️ Tech Stack

<div align="center">

| Layer | Technology |
|-------|------------|
| **Android App** | Kotlin, Jetpack, Material 3, Foreground Services |
| **Web Dashboard** | React 18, TypeScript, Vite, Tailwind CSS, Zustand |
| **Maps** | Leaflet, React-Leaflet, OpenStreetMap |
| **Backend** | Supabase (PostgreSQL, Realtime, Auth) |
| **Hosting** | Vercel (dashboard), Self-hosted APK |

</div>

### Project Structure

```
TrackIt/
├── 📱 android-app/                 # Kotlin Android application
│   └── app/src/main/java/com/familytracker/
│       ├── services/
│       │   ├── LocationService.kt      # GPS tracking (battery optimized)
│       │   ├── ShakeDetectorService.kt # Shake-to-SOS
│       │   ├── SpeedAlertService.kt    # Speed monitoring
│       │   ├── CameraCaptureService.kt # Silent photo capture
│       │   ├── AlarmService.kt         # Remote alarm
│       │   └── CommandListenerService.kt # Remote commands
│       ├── receivers/
│       │   ├── SmsReceiver.kt          # SMS command handler
│       │   ├── SimChangeReceiver.kt    # SIM removal detection
│       │   └── BootReceiver.kt         # Auto-start on boot
│       └── ui/
│           ├── HomeFragment.kt
│           ├── StatusFragment.kt
│           ├── EmergencyFragment.kt
│           └── SettingsFragment.kt
│
├── 🌐 dashboard/                   # React web dashboard
│   └── src/
│       ├── pages/
│       │   ├── Landing.tsx         # Public landing page
│       │   ├── Dashboard.tsx       # Main tracking view
│       │   ├── Devices.tsx         # Device management
│       │   └── Settings.tsx        # User preferences
│       ├── components/
│       │   ├── Map.tsx             # Interactive Leaflet map
│       │   ├── AntiTheftPanel.tsx  # Remote control panel
│       │   └── DeviceCard.tsx      # Device status cards
│       └── lib/
│           └── api.ts              # Supabase client
│
└── 🗄️ supabase/                    # Database schema
    └── migrations/                 # SQL migration files
```

---

## 🚀 Quick Start

### Option 1: Use Hosted Version (Recommended)

1. **Download APK**: [familytracker.vercel.app/TrackIt.apk](https://familytracker.vercel.app/TrackIt.apk)
2. **Install on Android**: Allow unknown sources, install APK
3. **Open Dashboard**: [familytracker.vercel.app](https://familytracker.vercel.app)
4. **Create Account** & link your device

### Option 2: Self-Host Everything

<details>
<summary><b>📖 Full Setup Guide (click to expand)</b></summary>

#### Prerequisites
- Node.js 18+
- Android Studio (Hedgehog+)
- Supabase account (free tier works)

#### 1. Clone & Setup

```bash
git clone https://github.com/omkarr10/Familtytracker.git
cd Familtytracker
```

#### 2. Setup Supabase

1. Create project at [supabase.com](https://supabase.com)
2. Run migrations from `supabase/migrations/` in SQL Editor
3. Get your API URL and anon key

#### 3. Configure Dashboard

```bash
cd dashboard
npm install

# Create .env file
echo "VITE_SUPABASE_URL=your_supabase_url" > .env
echo "VITE_SUPABASE_ANON_KEY=your_anon_key" >> .env

npm run dev
```

#### 4. Build Android App

1. Open `android-app/` in Android Studio
2. Update Supabase credentials in `SupabaseClient.kt`
3. Build → Generate Signed APK
4. Install on device

</details>

---

## 📱 Required Permissions

| Permission | Purpose |
|------------|---------|
| **Location (Always)** | Background location tracking |
| **Camera** | Capture thief photos |
| **SMS** | Send/receive SMS commands |
| **Phone State** | SIM change detection |
| **Contacts** | Emergency contact selection |
| **Notifications** | Alerts and status |

---

## 🔒 Privacy & Security

- 🏠 **Your Data, Your Server** - Self-host everything if paranoid
- 🚫 **No Analytics** - Zero tracking, zero telemetry
- 🔍 **Open Source** - Audit every line of code
- 🔐 **Local Auth** - Credentials never leave your Supabase instance
- 📱 **End-to-End** - SMS commands use secret codes

---

## 🤝 Contributing

Pull requests welcome! Here's how:

```bash
# Fork the repo, then:
git checkout -b feature/amazing-feature
git commit -m "Add amazing feature"
git push origin feature/amazing-feature
# Open a Pull Request
```

---

## 📄 License

MIT License - do whatever you want with it.

---

<div align="center">
  <br/>
  
  ### ⭐ Star this repo if it saved your phone or helped your family!
  
  <br/>
  
  <p>
    <a href="https://github.com/omkarr10/Familtytracker/stargazers">⭐ Star</a> ·
    <a href="https://github.com/omkarr10/Familtytracker/issues">🐛 Report Bug</a> ·
    <a href="https://github.com/omkarr10/Familtytracker/issues">💡 Request Feature</a>
  </p>
  
  <br/>
  
  Made with ❤️ for family safety
  
  <br/>
</div>
