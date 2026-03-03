# TrackIt 📍

<div align="center">
  <img src="dashboard/public/logo.jpeg" alt="TrackIt Logo" width="120" height="120" style="border-radius: 20px;">
  
  <h3>Real-time Family Location Tracking with Anti-Theft Protection</h3>
  
  <p>Open source family safety app with powerful theft detection and SMS fallback</p>
  
  [![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)
  [![React](https://img.shields.io/badge/React-18-61DAFB?logo=react)](https://reactjs.org/)
  [![TypeScript](https://img.shields.io/badge/TypeScript-5-3178C6?logo=typescript)](https://www.typescriptlang.org/)
  [![Kotlin](https://img.shields.io/badge/Kotlin-1.9-7F52FF?logo=kotlin)](https://kotlinlang.org/)
  [![Supabase](https://img.shields.io/badge/Supabase-Backend-3ECF8E?logo=supabase)](https://supabase.com/)
</div>

---

## ✨ Features

### 📍 Real-Time Location Tracking
- Live GPS tracking on interactive map
- Location history with timeline view
- Multiple map themes (Standard, Satellite, Dark)
- Device battery and speed monitoring

### 🛡️ Anti-Theft Protection
- **SIM Removal Detection** - Instant alerts when SIM card is removed
- **Thief Photo Capture** - Automatically takes front & back camera photos
- **Remote Lock** - Lock the device remotely
- **Loud Alarm** - Sound alarm even when phone is on silent
- **SMS Fallback** - Control device via SMS when internet is unavailable
- **Motion Detection** - Detects sudden grab movements
- **Theft Mode** - High-frequency tracking during theft events

### 🔔 Smart Alerts
- Geofence entry/exit notifications
- Low battery alerts
- SOS emergency button
- SIM change notifications

### 📱 SMS Commands (No Internet Required)
```
TRACKIT LOCATE   - Get current GPS location via SMS
TRACKIT ALARM    - Sound loud alarm
TRACKIT CAPTURE  - Capture thief photos
TRACKIT THEFT    - Activate theft mode
```

---

## 🏗️ Architecture

```
TrackIt/
├── dashboard/          # React + TypeScript web dashboard
│   ├── src/
│   │   ├── pages/      # Dashboard pages
│   │   ├── components/ # Reusable UI components
│   │   ├── lib/        # API client, utilities
│   │   └── store/      # Zustand state management
│   └── public/         # Static assets, PWA manifest
│
├── android-app/        # Kotlin Android application
│   └── app/src/main/
│       ├── java/com/familytracker/
│       │   ├── services/    # Location, Camera, Alarm services
│       │   ├── receivers/   # Boot, SMS, SIM change receivers
│       │   └── data/        # Preferences, Supabase client
│       └── res/             # Layouts, drawables, values
│
└── supabase/           # Database migrations
    └── migrations/     # SQL schema files
```

---

## 🚀 Getting Started

### Prerequisites

- Node.js 18+
- Android Studio (for Android app)
- Supabase account (free tier works)

### 1. Clone the Repository

```bash
git clone https://github.com/yourusername/trackit.git
cd trackit
```

### 2. Set Up Supabase

1. Create a new project at [supabase.com](https://supabase.com)
2. Go to SQL Editor and run the migrations:

```sql
-- Run each file in supabase/migrations/ in order:
-- 20250101000001_initial_schema.sql
-- 20250101000002_add_avatar_column.sql
-- 20250101000003_add_anti_theft_tables.sql
```

3. Copy your project URL and anon key

### 3. Set Up Dashboard

```bash
cd dashboard
npm install

# Create environment file
cp .env.example .env
```

Edit `.env`:
```env
VITE_SUPABASE_URL=https://your-project.supabase.co
VITE_SUPABASE_ANON_KEY=your-anon-key
```

Start development server:
```bash
npm run dev
```

### 4. Set Up Android App

1. Open `android-app` folder in Android Studio
2. Create `local.properties` if not exists:
```properties
sdk.dir=/path/to/your/android/sdk
```

3. Update Supabase credentials in `SupabaseClient.kt`:
```kotlin
private const val SUPABASE_URL = "https://your-project.supabase.co"
private const val SUPABASE_KEY = "your-anon-key"
```

4. Build and run:
```bash
./gradlew assembleDebug
```

---

## 📱 Android App Setup

### Required Permissions

The app requires these permissions for full functionality:

| Permission | Purpose |
|------------|---------|
| Location (Always) | Background location tracking |
| Camera | Capture thief photos |
| SMS | Send/receive SMS commands |
| Phone State | SIM change detection |
| Contacts | Emergency contact selection |
| Notifications | Alerts and status |

### Enable Device Admin

For remote lock/wipe functionality:
1. Go to Settings → Apps → TrackIt
2. Enable "Device Administrator"

---

## 🔧 Configuration

### Emergency Contacts

Set up to 3 emergency contacts in the Android app. These contacts will receive:
- SMS alerts with location when SIM is removed
- Theft detection notifications
- SOS emergency alerts

### Geofences

Create virtual boundaries on the dashboard:
1. Go to Geofences page
2. Click "Add Geofence"
3. Draw a circle on the map
4. Set name and radius
5. Get notified when devices enter/leave

---

## 🛡️ Anti-Theft Features

### Theft Detection Scoring

TrackIt uses a scoring system to detect theft:

| Event | Points |
|-------|--------|
| SIM Removed | 50 |
| Wrong PIN (each) | 15 |
| Airplane Mode On | 25 |
| Sudden Motion | 10 |

**Theft Mode activates at 50+ points**

### What Happens During Theft

1. **Instant Photo Capture** - Front and back camera
2. **SMS Alert** - Location sent to emergency contacts
3. **High-Frequency Tracking** - Updates every 5 seconds
4. **Offline Caching** - Data stored locally if no internet
5. **Auto-Sync** - Cached data uploads when online

---

## 🌐 Deployment

### Dashboard (Vercel)

```bash
cd dashboard
npm run build
vercel --prod
```

### Dashboard (Self-Hosted)

```bash
npm run build
# Serve the 'dist' folder with any static hosting
```

### API Proxy (Required for Supabase)

The dashboard uses a Vercel serverless function to proxy Supabase requests. For self-hosting, deploy the `/api` folder as serverless functions or create your own proxy.

---

## 📊 Database Schema

### Tables

- **devices** - Registered devices
- **locations** - Location history
- **geofences** - Virtual boundaries
- **alerts** - Notification history
- **theft_photos** - Captured thief images
- **remote_commands** - Dashboard → Device commands
- **theft_events** - Suspicious activity log

---

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 🙏 Acknowledgments

- [Supabase](https://supabase.com) - Backend as a Service
- [Leaflet](https://leafletjs.com) - Interactive maps
- [shadcn/ui](https://ui.shadcn.com) - UI components inspiration
- [Lucide](https://lucide.dev) - Beautiful icons

---

## 📞 Support

- **Issues**: [GitHub Issues](https://github.com/yourusername/trackit/issues)
- **Discussions**: [GitHub Discussions](https://github.com/yourusername/trackit/discussions)

---

<div align="center">
  <p>Made with ❤️ for family safety</p>
  <p>⭐ Star this repo if you find it useful!</p>
</div>
