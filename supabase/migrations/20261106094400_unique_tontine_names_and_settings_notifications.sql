ALTER TABLE public.tontines
    ADD COLUMN IF NOT EXISTS is_anonymous boolean NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS hide_info_for_simple_members boolean NOT NULL DEFAULT false;

CREATE INDEX IF NOT EXISTS tontines_creator_name_lookup_idx
    ON public.tontines (creator_id, lower(btrim(libelle)))
    WHERE creator_id IS NOT NULL;

CREATE OR REPLACE FUNCTION public.enforce_unique_tontine_name_per_creator()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, public
AS $$
BEGIN
    IF NULLIF(btrim(NEW.libelle), '') IS NULL THEN
        RETURN NEW;
    END IF;

    IF lower(btrim(NEW.libelle)) = lower('Les amis du dimanche') THEN
        IF TG_OP = 'UPDATE'
           AND lower(btrim(OLD.libelle)) = lower('Les amis du dimanche') THEN
            RETURN NEW;
        END IF;

        PERFORM pg_advisory_xact_lock(hashtext('shared-default-tontine'));

        IF EXISTS (
            SELECT 1
            FROM public.tontines t
            WHERE lower(btrim(t.libelle)) = lower('Les amis du dimanche')
              AND t.id IS DISTINCT FROM NEW.id
        ) THEN
            RAISE EXCEPTION 'The shared default tontine already exists.'
                USING ERRCODE = '23505';
        END IF;

        RETURN NEW;
    END IF;

    IF TG_OP = 'UPDATE'
       AND NEW.creator_id IS NOT DISTINCT FROM OLD.creator_id
       AND lower(btrim(NEW.libelle)) = lower(btrim(OLD.libelle)) THEN
        RETURN NEW;
    END IF;

    PERFORM pg_advisory_xact_lock(
        hashtext(NEW.creator_id::text),
        hashtext(lower(btrim(NEW.libelle)))
    );

    IF EXISTS (
        SELECT 1
        FROM public.tontines t
        WHERE t.creator_id = NEW.creator_id
          AND lower(btrim(t.libelle)) = lower(btrim(NEW.libelle))
          AND t.id IS DISTINCT FROM NEW.id
    ) THEN
        RAISE EXCEPTION 'You already have a tontine with this name.'
            USING ERRCODE = '23505';
    END IF;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS tontines_unique_name_per_creator ON public.tontines;
CREATE TRIGGER tontines_unique_name_per_creator
BEFORE INSERT OR UPDATE OF creator_id, libelle ON public.tontines
FOR EACH ROW EXECUTE FUNCTION public.enforce_unique_tontine_name_per_creator();

DROP FUNCTION IF EXISTS public.create_tontine_with_initiator(
    uuid, text, text, text, text, boolean, bigint
);

CREATE OR REPLACE FUNCTION public.create_tontine_with_initiator(
    p_id uuid,
    p_libelle text,
    p_exercice text,
    p_date_debut text,
    p_date_fin text,
    p_is_online boolean,
    p_updated_at bigint,
    p_is_anonymous boolean DEFAULT false,
    p_hide_info_for_simple_members boolean DEFAULT false
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
        id, libelle, exercice, date_debut, date_fin, creator_id, is_online,
        updated_at, is_anonymous, hide_info_for_simple_members
    )
    VALUES (
        p_id, p_libelle, p_exercice, p_date_debut, p_date_fin, actor_id, p_is_online,
        p_updated_at, p_is_anonymous, p_hide_info_for_simple_members
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
    uuid, text, text, text, text, boolean, bigint, boolean, boolean
) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.create_tontine_with_initiator(
    uuid, text, text, text, text, boolean, bigint, boolean, boolean
) TO authenticated;

CREATE OR REPLACE FUNCTION public.notify_tontine_settings_changed()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, public
AS $$
DECLARE
    actor_id uuid := (SELECT auth.uid());
    actor_name text;
BEGIN
    IF ROW(
        NEW.libelle, NEW.exercice, NEW.date_debut, NEW.date_fin,
        NEW.is_anonymous, NEW.hide_info_for_simple_members
    ) IS NOT DISTINCT FROM ROW(
        OLD.libelle, OLD.exercice, OLD.date_debut, OLD.date_fin,
        OLD.is_anonymous, OLD.hide_info_for_simple_members
    ) THEN
        RETURN NEW;
    END IF;

    SELECT COALESCE(NULLIF(p.nom, ''), 'Un membre')
    INTO actor_name
    FROM public.profiles p
    WHERE p.id = actor_id;
    actor_name := COALESCE(actor_name, 'Un membre');

    INSERT INTO public.notifications (
        user_id, title, message, type, reference_id,
        group_key, actor_id, actor_name, context_name
    )
    SELECT DISTINCT
        m.user_id,
        'Paramètres de la réunion modifiés',
        actor_name || ' a modifié les paramètres de la réunion « ' ||
            NEW.libelle || ' ».',
        'TONTINE_SETTINGS',
        NEW.id,
        'tontine_settings:' || NEW.id::text,
        actor_id,
        actor_name,
        NEW.libelle
    FROM public.members m
    WHERE m.tontine_id = NEW.id
      AND m.user_id IS NOT NULL
      AND m.user_id IS DISTINCT FROM actor_id;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS tontine_settings_notify_members ON public.tontines;
CREATE TRIGGER tontine_settings_notify_members
AFTER UPDATE OF
    libelle, exercice, date_debut, date_fin, is_anonymous, hide_info_for_simple_members
ON public.tontines
FOR EACH ROW EXECUTE FUNCTION public.notify_tontine_settings_changed();

CREATE OR REPLACE FUNCTION public.notify_caisse_settings_changed()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, public
AS $$
DECLARE
    actor_id uuid := (SELECT auth.uid());
    actor_name text;
    tontine_name text;
BEGIN
    IF ROW(
        NEW.type, NEW.libelle, NEW.montant_fixe, NEW.montant_minimum,
        NEW.regularite_imposee, NEW.redistribution, NEW.ordre_redistribution,
        NEW.taux_interet, NEW.type_interet, NEW.penalite_retard_fixe,
        NEW.penalite_echec_fixe, NEW.date_limite_jour, NEW.frais_caisse,
        NEW.statut, NEW.respecter_frequence_strictement, NEW.ordre_fige,
        NEW.possibilite_pret, NEW.autoriser_pret_sur_amendes, NEW.frequence,
        NEW.duree_cycles, NEW.plafond_pret, NEW.is_basique
    ) IS NOT DISTINCT FROM ROW(
        OLD.type, OLD.libelle, OLD.montant_fixe, OLD.montant_minimum,
        OLD.regularite_imposee, OLD.redistribution, OLD.ordre_redistribution,
        OLD.taux_interet, OLD.type_interet, OLD.penalite_retard_fixe,
        OLD.penalite_echec_fixe, OLD.date_limite_jour, OLD.frais_caisse,
        OLD.statut, OLD.respecter_frequence_strictement, OLD.ordre_fige,
        OLD.possibilite_pret, OLD.autoriser_pret_sur_amendes, OLD.frequence,
        OLD.duree_cycles, OLD.plafond_pret, OLD.is_basique
    ) THEN
        RETURN NEW;
    END IF;

    SELECT t.libelle INTO tontine_name
    FROM public.tontines t
    WHERE t.id = NEW.tontine_id;

    SELECT COALESCE(NULLIF(p.nom, ''), 'Un membre')
    INTO actor_name
    FROM public.profiles p
    WHERE p.id = actor_id;
    actor_name := COALESCE(actor_name, 'Un membre');

    INSERT INTO public.notifications (
        user_id, title, message, type, reference_id,
        group_key, actor_id, actor_name, context_name
    )
    SELECT DISTINCT
        m.user_id,
        'Paramètres d''une caisse modifiés',
        actor_name || ' a modifié la caisse « ' || COALESCE(NEW.libelle, 'Sans nom') ||
            ' » de la réunion « ' || COALESCE(tontine_name, 'votre réunion') || ' ».',
        'CAISSE_SETTINGS',
        NEW.tontine_id,
        'caisse_settings:' || NEW.id::text,
        actor_id,
        actor_name,
        COALESCE(tontine_name, 'votre réunion')
    FROM public.members m
    WHERE m.tontine_id = NEW.tontine_id
      AND m.user_id IS NOT NULL
      AND m.user_id IS DISTINCT FROM actor_id;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS caisse_settings_notify_members ON public.caisses;
CREATE TRIGGER caisse_settings_notify_members
AFTER UPDATE OF
    type, libelle, montant_fixe, montant_minimum, regularite_imposee,
    redistribution, ordre_redistribution, taux_interet, type_interet,
    penalite_retard_fixe, penalite_echec_fixe, date_limite_jour, frais_caisse,
    statut, respecter_frequence_strictement, ordre_fige, possibilite_pret,
    autoriser_pret_sur_amendes, frequence, duree_cycles, plafond_pret, is_basique
ON public.caisses
FOR EACH ROW EXECUTE FUNCTION public.notify_caisse_settings_changed();

REVOKE ALL ON FUNCTION public.enforce_unique_tontine_name_per_creator()
    FROM PUBLIC, anon, authenticated;
REVOKE ALL ON FUNCTION public.notify_tontine_settings_changed()
    FROM PUBLIC, anon, authenticated;
REVOKE ALL ON FUNCTION public.notify_caisse_settings_changed()
    FROM PUBLIC, anon, authenticated;

NOTIFY pgrst, 'reload schema';
