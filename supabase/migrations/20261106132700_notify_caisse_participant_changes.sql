CREATE OR REPLACE FUNCTION public.notify_caisse_participant_membership()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, public
AS $$
DECLARE
    target_member_id uuid;
    target_caisse_id uuid;
    recipient_id uuid;
    caisse_name text;
    tontine_name text;
    actor_id uuid := (SELECT auth.uid());
    actor_name text;
    notification_title text;
    notification_message text;
BEGIN
    IF TG_OP = 'INSERT' THEN
        target_member_id := NEW.member_id;
        target_caisse_id := NEW.caisse_id;
        notification_title := 'Ajouté à une caisse';
    ELSE
        target_member_id := OLD.member_id;
        target_caisse_id := OLD.caisse_id;
        notification_title := 'Retiré d''une caisse';
    END IF;

    SELECT c.libelle, t.libelle
    INTO caisse_name, tontine_name
    FROM public.caisses c
    JOIN public.tontines t ON t.id = c.tontine_id
    WHERE c.id = target_caisse_id;
    IF NOT FOUND THEN
        IF TG_OP = 'DELETE' THEN
            RETURN OLD;
        END IF;
        RETURN NEW;
    END IF;

    SELECT m.user_id
    INTO recipient_id
    FROM public.members m
    WHERE m.id = target_member_id;
    IF NOT FOUND OR recipient_id IS NULL THEN
        IF TG_OP = 'DELETE' THEN
            RETURN OLD;
        END IF;
        RETURN NEW;
    END IF;

    SELECT COALESCE(NULLIF(p.nom, ''), 'Un responsable')
    INTO actor_name
    FROM public.profiles p
    WHERE p.id = actor_id;
    actor_name := COALESCE(actor_name, 'Un responsable');

    IF TG_OP = 'INSERT' THEN
        notification_message := 'Vous avez été ajouté dans la caisse « ' ||
            COALESCE(caisse_name, 'Sans nom') || ' » de la réunion « ' ||
            COALESCE(tontine_name, 'votre réunion') || ' ».';
    ELSE
        notification_message := 'Vous avez été retiré de la caisse « ' ||
            COALESCE(caisse_name, 'Sans nom') || ' » de la réunion « ' ||
            COALESCE(tontine_name, 'votre réunion') || ' ».';
    END IF;

    INSERT INTO public.notifications (
        user_id, title, message, type, reference_id,
        group_key, actor_id, actor_name, context_name
    )
    VALUES (
        recipient_id,
        notification_title,
        notification_message,
        'CAISSE_MEMBERSHIP',
        target_caisse_id,
        'caisse_membership:' || target_caisse_id::text || ':' || target_member_id::text,
        actor_id,
        actor_name,
        COALESCE(caisse_name, 'Sans nom')
    );

    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    END IF;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS caisse_participant_membership_notify ON public.caisse_participants;
CREATE TRIGGER caisse_participant_membership_notify
AFTER INSERT OR DELETE ON public.caisse_participants
FOR EACH ROW EXECUTE FUNCTION public.notify_caisse_participant_membership();

REVOKE ALL ON FUNCTION public.notify_caisse_participant_membership()
    FROM PUBLIC, anon, authenticated;

NOTIFY pgrst, 'reload schema';
