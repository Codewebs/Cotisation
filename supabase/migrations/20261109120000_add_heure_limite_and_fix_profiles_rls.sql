-- Add heure_limite to caisses table
ALTER TABLE public.caisses ADD COLUMN IF NOT EXISTS heure_limite integer;

-- Fix RLS policy on public.profiles to allow reading requester profiles
DROP POLICY IF EXISTS profiles_read_self ON public.profiles;
CREATE POLICY profiles_read_authenticated ON public.profiles FOR SELECT TO authenticated USING (true);
