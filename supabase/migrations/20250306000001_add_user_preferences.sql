-- ============================================
-- ADD USER PREFERENCES COLUMNS
-- ============================================
-- This allows settings to sync across all devices
-- when logged into the same account

-- Add selected anti-theft device column to users table
ALTER TABLE public.users 
ADD COLUMN IF NOT EXISTS selected_antitheft_device_id UUID REFERENCES public.devices(id) ON DELETE SET NULL;

-- Create an index for quick lookups
CREATE INDEX IF NOT EXISTS idx_users_antitheft_device ON public.users(selected_antitheft_device_id);
