# Family Tracker

A real-time family location tracking system with Android app and web dashboard.

## 🆓 100% FREE Hosting Stack

| Component | Service | Cost |
|-----------|---------|------|
| Database | Supabase (free tier) | $0 |
| Auth | Supabase Auth | $0 |
| Real-time | Supabase Realtime | $0 |
| Dashboard | Vercel | $0 |
| Maps | OpenStreetMap + Leaflet | $0 |

## 📁 Project Structure

```
family-tracker/
├── dashboard/          # React web dashboard (Vercel)
│   ├── src/
│   │   ├── components/
│   │   ├── pages/
│   │   ├── store/
│   │   ├── lib/
│   │   └── types/
│   └── package.json
│
├── android-app/        # Kotlin Android app
│   └── (build with Android Studio)
│
└── supabase/
    └── migrations/     # Database schema
```

## 🚀 Setup Instructions

### 1. Supabase Setup

1. Create account at [supabase.com](https://supabase.com)
2. Create new project
3. Go to SQL Editor
4. Run the SQL from `supabase/migrations/001_initial_schema.sql`
5. Copy your project URL and anon key from Settings → API

### 2. Dashboard Setup

```bash
cd dashboard
npm install
```

Create `.env` file:
```
VITE_SUPABASE_URL=https://xxxxx.supabase.co
VITE_SUPABASE_ANON_KEY=your-anon-key
```

Run locally:
```bash
npm run dev
```

### 3. Deploy to Vercel

1. Push code to GitHub
2. Go to [vercel.com](https://vercel.com)
3. Import your repository
4. Add environment variables:
   - `VITE_SUPABASE_URL`
   - `VITE_SUPABASE_ANON_KEY`
5. Deploy!

### 4. Android App Setup

Build the Android app with Android Studio using the code in `android-app/`.

## 📱 Features

### Dashboard
- ✅ Live location tracking on map
- ✅ Device management
- ✅ Location history with playback
- ✅ Geofence zones with alerts
- ✅ Alert center
- ✅ Real-time updates via WebSocket

### Android App (to build)
- ✅ Background location service
- ✅ Boot persistence
- ✅ Battery optimization handling
- ✅ Offline queue
- ✅ SOS button
- ✅ SIM change detection

## 🔒 Security

- All data transmitted over HTTPS
- Row Level Security (RLS) on all tables
- JWT authentication via Supabase Auth
- Devices authenticated by unique ID

## 📄 License

MIT - Use for personal/family tracking only.
