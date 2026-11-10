CREATE OR REPLACE FUNCTION public.create_tontine_with_initiator(
    p_id uuid,
    p_libelle text,
    p_exercice text,
    p_date_debut text,
    p_date_fin text,
    p_is_online boolean,
    p_updated_at bigint
)
RETURNS TABLE (id uuid)
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, public
AS $$
DECLARE
    actor_id uuid := (SELECT auth.uid());
    member_name text;
    member_phone text;
BEGIN
    IF actor_id IS NULL THEN
        RAISE EXCEPTION 'Authentication is required to create a tontine'
            USING ERRCODE = '42501';
    END IF;

    SELECT COALESCE(NULLIF(p.nom, ''), 'Administrateur'),
           COALESCE(p.phone, '')
    INTO member_name, member_phone
    FROM public.profiles p
    WHERE p.id = actor_id;

    IF member_name IS NULL THEN
        RAISE EXCEPTION 'The authenticated user profile does not exist'
            USING ERRCODE = '23503';
    END IF;

    INSERT INTO public.tontines (
        id, libelle, exercice, date_debut, date_fin, creator_id, is_online, updated_at
    )
    VALUES (
        p_id, p_libelle, p_exercice, p_date_debut, p_date_fin, actor_id, p_is_online, p_updated_at
    );

    INSERT INTO public.members (
        tontine_id, user_id, nom_saisi, phone, role, is_initiator
    )
    VALUES (
        p_id, actor_id, member_name, member_phone, 'INITIATEUR', true
    );

    RETURN QUERY SELECT p_id;
END;
$$;

REVOKE ALL ON FUNCTION public.create_tontine_with_initiator(
    uuid, text, text, text, text, boolean, bigint
) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.create_tontine_with_initiator(
    uuid, text, text, text, text, boolean, bigint
) TO authenticated;

DROP FUNCTION public.search_tontines(text);
CREATE FUNCTION public.search_tontines(search_term text DEFAULT '')
RETURNS TABLE (
    id uuid,
    libelle varchar,
    exercice varchar,
    creator_id uuid,
    member_count bigint
)
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = pg_catalog, public
AS $$
    SELECT t.id, t.libelle, t.exercice, t.creator_id, count(m.id)::bigint
    FROM public.tontines t
    LEFT JOIN public.members m ON m.tontine_id = t.id
    WHERE t.is_online
      AND (
          NULLIF(btrim(search_term), '') IS NULL
          OR strpos(lower(t.libelle), lower(btrim(search_term))) > 0
          OR strpos(lower(COALESCE(t.exercice, '')), lower(btrim(search_term))) > 0
      )
    GROUP BY t.id
    ORDER BY t.libelle
    LIMIT 100;
$$;

REVOKE ALL ON FUNCTION public.search_tontines(text) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.search_tontines(text) TO authenticated;

CREATE OR REPLACE FUNCTION public.claim_empty_default_tontine(p_tontine_id uuid)
RETURNS boolean
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, public
AS $$
DECLARE
    actor_id uuid := (SELECT auth.uid());
    member_name text;
    member_phone text;
BEGIN
    IF actor_id IS NULL THEN
        RAISE EXCEPTION 'Authentication is required to claim the default tontine'
            USING ERRCODE = '42501';
    END IF;

    PERFORM 1
    FROM public.tontines t
    WHERE t.id = p_tontine_id
      AND lower(btrim(t.libelle)) = lower('Les amis du dimanche')
    FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Default tontine not found'
            USING ERRCODE = 'P0002';
    END IF;

    IF EXISTS (
        SELECT 1 FROM public.members m
        WHERE m.tontine_id = p_tontine_id AND m.user_id = actor_id
    ) THEN
        RETURN true;
    END IF;

    IF EXISTS (
        SELECT 1 FROM public.members m
        WHERE m.tontine_id = p_tontine_id
    ) THEN
        RETURN false;
    END IF;

    SELECT COALESCE(NULLIF(p.nom, ''), 'Administrateur'),
           COALESCE(p.phone, '')
    INTO member_name, member_phone
    FROM public.profiles p
    WHERE p.id = actor_id;

    IF member_name IS NULL THEN
        RAISE EXCEPTION 'The authenticated user profile does not exist'
            USING ERRCODE = '23503';
    END IF;

    UPDATE public.tontines
    SET creator_id = actor_id
    WHERE id = p_tontine_id;

    INSERT INTO public.members (
        tontine_id, user_id, nom_saisi, phone, role, is_initiator
    )
    VALUES (
        p_tontine_id, actor_id, member_name, member_phone, 'INITIATEUR', true
    );

    INSERT INTO public.caisses (tontine_id, type, libelle, montant_fixe, redistribution)
    SELECT p_tontine_id, 'COTISATION_SIMPLE', 'Caisse Principale', 0, true
    WHERE NOT EXISTS (
        SELECT 1 FROM public.caisses c WHERE c.tontine_id = p_tontine_id
    );

    RETURN true;
END;
$$;

REVOKE ALL ON FUNCTION public.claim_empty_default_tontine(uuid) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.claim_empty_default_tontine(uuid) TO authenticated;

NOTIFY pgrst, 'reload schema';
