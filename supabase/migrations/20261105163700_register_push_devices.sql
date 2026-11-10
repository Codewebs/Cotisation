CREATE UNIQUE INDEX user_devices_fcm_token_key
    ON public.user_devices (fcm_token);

CREATE OR REPLACE FUNCTION public.register_push_device(p_fcm_token text)
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, public
AS $$
DECLARE
    actor_id uuid := (SELECT auth.uid());
BEGIN
    IF actor_id IS NULL THEN
        RAISE EXCEPTION 'Authentication is required to register a push device'
            USING ERRCODE = '42501';
    END IF;

    IF p_fcm_token IS NULL OR btrim(p_fcm_token) = '' THEN
        RAISE EXCEPTION 'FCM token must not be empty'
            USING ERRCODE = '22023';
    END IF;

    INSERT INTO public.user_devices (user_id, fcm_token, platform, updated_at)
    VALUES (
        actor_id,
        p_fcm_token,
        'ANDROID',
        floor(extract(epoch FROM clock_timestamp()) * 1000)::bigint
    )
    ON CONFLICT (fcm_token) DO UPDATE
    SET user_id = EXCLUDED.user_id,
        platform = EXCLUDED.platform,
        updated_at = EXCLUDED.updated_at;
END;
$$;

REVOKE ALL ON FUNCTION public.register_push_device(text) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.register_push_device(text) TO authenticated;

NOTIFY pgrst, 'reload schema';
