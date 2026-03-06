-- Anti-Theft System Tables
-- Run this in Supabase SQL Editor

-- Table for storing theft photos (captured when SIM removed, wrong PIN, etc.)
CREATE TABLE IF NOT EXISTS public.theft_photos (
  id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
  device_id UUID REFERENCES public.devices(id) ON DELETE CASCADE,
  photo_base64 TEXT NOT NULL,
  camera_type TEXT NOT NULL CHECK (camera_type IN ('front', 'back')),
  trigger_event TEXT NOT NULL, -- 'sim_removed', 'wrong_pin', 'alarm_triggered', 'manual_capture'
  latitude DOUBLE PRECISION,
  longitude DOUBLE PRECISION,
  captured_at TIMESTAMPTZ DEFAULT NOW(),
  created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Table for remote commands sent from dashboard to device
CREATE TABLE IF NOT EXISTS public.remote_commands (
  id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
  device_id UUID REFERENCES public.devices(id) ON DELETE CASCADE,
  command TEXT NOT NULL CHECK (command IN ('lock', 'alarm', 'capture', 'locate', 'wipe', 'activate_theft_mode', 'deactivate_theft_mode', 'stop_alarm')),
  parameters JSONB DEFAULT '{}',
  status TEXT DEFAULT 'pending' CHECK (status IN ('pending', 'executed', 'failed')),
  created_at TIMESTAMPTZ DEFAULT NOW(),
  executed_at TIMESTAMPTZ
);

-- Table for theft events (timeline of suspicious activities)
CREATE TABLE IF NOT EXISTS public.theft_events (
  id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
  device_id UUID REFERENCES public.devices(id) ON DELETE CASCADE,
  event_type TEXT NOT NULL, -- 'sim_removed', 'sim_changed', 'wrong_pin', 'airplane_mode', 'power_off', 'motion_detected', 'theft_mode_activated'
  severity TEXT DEFAULT 'medium' CHECK (severity IN ('low', 'medium', 'high', 'critical')),
  description TEXT,
  latitude DOUBLE PRECISION,
  longitude DOUBLE PRECISION,
  metadata JSONB DEFAULT '{}',
  created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_theft_photos_device_id ON public.theft_photos(device_id);
CREATE INDEX IF NOT EXISTS idx_theft_photos_captured_at ON public.theft_photos(captured_at DESC);
CREATE INDEX IF NOT EXISTS idx_remote_commands_device_id ON public.remote_commands(device_id);
CREATE INDEX IF NOT EXISTS idx_remote_commands_status ON public.remote_commands(status);
CREATE INDEX IF NOT EXISTS idx_remote_commands_pending ON public.remote_commands(device_id, status) WHERE status = 'pending';
CREATE INDEX IF NOT EXISTS idx_theft_events_device_id ON public.theft_events(device_id);
CREATE INDEX IF NOT EXISTS idx_theft_events_created_at ON public.theft_events(created_at DESC);

-- Enable Row Level Security
ALTER TABLE public.theft_photos ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.remote_commands ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.theft_events ENABLE ROW LEVEL SECURITY;

-- RLS Policies for theft_photos
CREATE POLICY "Users can view their devices theft photos" ON public.theft_photos
  FOR SELECT USING (
    device_id IN (SELECT id FROM public.devices WHERE user_id = auth.uid())
  );

CREATE POLICY "Devices can insert theft photos" ON public.theft_photos
  FOR INSERT WITH CHECK (true);

-- RLS Policies for remote_commands
CREATE POLICY "Users can view and create commands for their devices" ON public.remote_commands
  FOR ALL USING (
    device_id IN (SELECT id FROM public.devices WHERE user_id = auth.uid())
  );

CREATE POLICY "Devices can view and update their commands" ON public.remote_commands
  FOR SELECT USING (true);

CREATE POLICY "Devices can update command status" ON public.remote_commands
  FOR UPDATE USING (true);

-- RLS Policies for theft_events
CREATE POLICY "Users can view their devices theft events" ON public.theft_events
  FOR SELECT USING (
    device_id IN (SELECT id FROM public.devices WHERE user_id = auth.uid())
  );

CREATE POLICY "Devices can insert theft events" ON public.theft_events
  FOR INSERT WITH CHECK (true);

-- Function to clean up old theft photos (keep last 100 per device)
CREATE OR REPLACE FUNCTION cleanup_old_theft_photos()
RETURNS TRIGGER AS $$
BEGIN
  DELETE FROM public.theft_photos
  WHERE id IN (
    SELECT id FROM public.theft_photos
    WHERE device_id = NEW.device_id
    ORDER BY captured_at DESC
    OFFSET 100
  );
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger to auto-cleanup old photos
DROP TRIGGER IF EXISTS cleanup_theft_photos_trigger ON public.theft_photos;
CREATE TRIGGER cleanup_theft_photos_trigger
  AFTER INSERT ON public.theft_photos
  FOR EACH ROW
  EXECUTE FUNCTION cleanup_old_theft_photos();

-- Function to auto-expire old pending commands after 24 hours
CREATE OR REPLACE FUNCTION expire_old_commands()
RETURNS void AS $$
BEGIN
  UPDATE public.remote_commands
  SET status = 'failed'
  WHERE status = 'pending'
  AND created_at < NOW() - INTERVAL '24 hours';
END;
$$ LANGUAGE plpgsql;
