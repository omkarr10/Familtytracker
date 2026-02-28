-- ============================================
-- FAMILY TRACKER - SUPABASE DATABASE SCHEMA
-- ============================================
-- Run this in your Supabase SQL Editor
-- ============================================

-- Enable necessary extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================
-- USERS TABLE (extends Supabase auth.users)
-- ============================================
CREATE TABLE IF NOT EXISTS public.users (
  id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
  email TEXT UNIQUE NOT NULL,
  name TEXT,
  created_at TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================
-- DEVICES TABLE
-- ============================================
CREATE TABLE IF NOT EXISTS public.devices (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
  device_name TEXT NOT NULL,
  device_token TEXT,  -- FCM token for push notifications
  last_seen TIMESTAMPTZ,
  battery_level INTEGER CHECK (battery_level >= 0 AND battery_level <= 100),
  is_online BOOLEAN DEFAULT false,
  created_at TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================
-- LOCATIONS TABLE
-- ============================================
CREATE TABLE IF NOT EXISTS public.locations (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  device_id UUID NOT NULL REFERENCES public.devices(id) ON DELETE CASCADE,
  latitude DOUBLE PRECISION NOT NULL,
  longitude DOUBLE PRECISION NOT NULL,
  accuracy REAL,
  speed REAL,
  altitude REAL,
  bearing REAL,
  event_type TEXT DEFAULT 'normal' CHECK (event_type IN ('normal', 'sos', 'sim_change', 'low_battery', 'burst', 'geofence')),
  created_at TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================
-- GEOFENCES TABLE
-- ============================================
CREATE TABLE IF NOT EXISTS public.geofences (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
  name TEXT NOT NULL,
  latitude DOUBLE PRECISION NOT NULL,
  longitude DOUBLE PRECISION NOT NULL,
  radius_meters INTEGER NOT NULL CHECK (radius_meters > 0),
  alert_on_enter BOOLEAN DEFAULT true,
  alert_on_exit BOOLEAN DEFAULT true,
  is_active BOOLEAN DEFAULT true,
  created_at TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================
-- ALERTS TABLE
-- ============================================
CREATE TABLE IF NOT EXISTS public.alerts (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  device_id UUID NOT NULL REFERENCES public.devices(id) ON DELETE CASCADE,
  alert_type TEXT NOT NULL CHECK (alert_type IN ('sos', 'geofence_enter', 'geofence_exit', 'low_battery', 'sim_change', 'offline', 'speed')),
  message TEXT,
  latitude DOUBLE PRECISION,
  longitude DOUBLE PRECISION,
  is_read BOOLEAN DEFAULT false,
  geofence_id UUID REFERENCES public.geofences(id) ON DELETE SET NULL,
  created_at TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================
-- INDEXES FOR PERFORMANCE
-- ============================================
CREATE INDEX IF NOT EXISTS idx_devices_user_id ON public.devices(user_id);
CREATE INDEX IF NOT EXISTS idx_locations_device_id ON public.locations(device_id);
CREATE INDEX IF NOT EXISTS idx_locations_device_time ON public.locations(device_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_locations_created_at ON public.locations(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_alerts_device_id ON public.alerts(device_id);
CREATE INDEX IF NOT EXISTS idx_alerts_created_at ON public.alerts(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_geofences_user_id ON public.geofences(user_id);

-- ============================================
-- ENABLE ROW LEVEL SECURITY (RLS)
-- ============================================
ALTER TABLE public.users ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.devices ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.locations ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.geofences ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.alerts ENABLE ROW LEVEL SECURITY;

-- ============================================
-- RLS POLICIES - USERS
-- ============================================
CREATE POLICY "Users can view own profile"
  ON public.users FOR SELECT
  USING (auth.uid() = id);

CREATE POLICY "Users can update own profile"
  ON public.users FOR UPDATE
  USING (auth.uid() = id);

-- ============================================
-- RLS POLICIES - DEVICES
-- ============================================
CREATE POLICY "Users can view own devices"
  ON public.devices FOR SELECT
  USING (user_id = auth.uid());

CREATE POLICY "Users can insert own devices"
  ON public.devices FOR INSERT
  WITH CHECK (user_id = auth.uid());

CREATE POLICY "Users can update own devices"
  ON public.devices FOR UPDATE
  USING (user_id = auth.uid());

CREATE POLICY "Users can delete own devices"
  ON public.devices FOR DELETE
  USING (user_id = auth.uid());

-- Allow devices to update themselves (for location updates)
CREATE POLICY "Devices can update themselves"
  ON public.devices FOR UPDATE
  USING (true);  -- We'll validate via device_id in the app

-- ============================================
-- RLS POLICIES - LOCATIONS
-- ============================================
CREATE POLICY "Users can view locations of own devices"
  ON public.locations FOR SELECT
  USING (
    device_id IN (
      SELECT id FROM public.devices WHERE user_id = auth.uid()
    )
  );

-- Allow inserting locations (validated by device_id)
CREATE POLICY "Anyone can insert locations"
  ON public.locations FOR INSERT
  WITH CHECK (true);  -- Device validates itself via device_id

-- ============================================
-- RLS POLICIES - GEOFENCES
-- ============================================
CREATE POLICY "Users can view own geofences"
  ON public.geofences FOR SELECT
  USING (user_id = auth.uid());

CREATE POLICY "Users can insert own geofences"
  ON public.geofences FOR INSERT
  WITH CHECK (user_id = auth.uid());

CREATE POLICY "Users can update own geofences"
  ON public.geofences FOR UPDATE
  USING (user_id = auth.uid());

CREATE POLICY "Users can delete own geofences"
  ON public.geofences FOR DELETE
  USING (user_id = auth.uid());

-- ============================================
-- RLS POLICIES - ALERTS
-- ============================================
CREATE POLICY "Users can view alerts for own devices"
  ON public.alerts FOR SELECT
  USING (
    device_id IN (
      SELECT id FROM public.devices WHERE user_id = auth.uid()
    )
  );

CREATE POLICY "Anyone can insert alerts"
  ON public.alerts FOR INSERT
  WITH CHECK (true);

CREATE POLICY "Users can update own alerts"
  ON public.alerts FOR UPDATE
  USING (
    device_id IN (
      SELECT id FROM public.devices WHERE user_id = auth.uid()
    )
  );

-- ============================================
-- ENABLE REALTIME
-- ============================================
ALTER PUBLICATION supabase_realtime ADD TABLE public.locations;
ALTER PUBLICATION supabase_realtime ADD TABLE public.alerts;
ALTER PUBLICATION supabase_realtime ADD TABLE public.devices;

-- ============================================
-- FUNCTION: Auto-create user profile on signup
-- ============================================
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
BEGIN
  INSERT INTO public.users (id, email)
  VALUES (NEW.id, NEW.email);
  RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Trigger to auto-create user profile
DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
  AFTER INSERT ON auth.users
  FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();

-- ============================================
-- FUNCTION: Update device last_seen on location insert
-- ============================================
CREATE OR REPLACE FUNCTION public.update_device_last_seen()
RETURNS TRIGGER AS $$
BEGIN
  UPDATE public.devices
  SET 
    last_seen = NEW.created_at,
    is_online = true
  WHERE id = NEW.device_id;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS on_location_insert ON public.locations;
CREATE TRIGGER on_location_insert
  AFTER INSERT ON public.locations
  FOR EACH ROW EXECUTE FUNCTION public.update_device_last_seen();

-- ============================================
-- DONE!
-- ============================================
