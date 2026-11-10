CREATE OR REPLACE FUNCTION public.notify_join_request_changes()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, public
AS $$
DECLARE
    recipient_id uuid;
    tontine_name text;
    notification_title text;
    notification_message text;
BEGIN
    SELECT t.libelle INTO tontine_name
    FROM public.tontines t
    WHERE t.id = NEW.tontine_id;

    IF TG_OP = 'INSERT' THEN
        IF NEW.invite_code IS NOT NULL THEN
            notification_title := 'Invitation utilisée';
            notification_message := 'Une personne a répondu à votre invitation pour ' ||
                COALESCE(tontine_name, 'votre tontine') || '.';
        ELSE
            notification_title := 'Nouvelle demande d''adhésion';
            notification_message := 'Une personne souhaite rejoindre ' ||
                COALESCE(tontine_name, 'votre tontine') || '.';
        END IF;

        FOR recipient_id IN
            SELECT DISTINCT candidates.user_id
            FROM (
                SELECT t.creator_id AS user_id
                FROM public.tontines t
                WHERE t.id = NEW.tontine_id
                UNION
                SELECT m.user_id
                FROM public.members m
                WHERE m.tontine_id = NEW.tontine_id
                  AND (
                      m.is_initiator
                      OR upper(COALESCE(m.role, '')) IN
                         ('ADMIN', 'PRESIDENT', 'TRESORIER', 'SECRETAIRE', 'BUREAU')
                  )
            ) candidates
            WHERE candidates.user_id IS NOT NULL
              AND candidates.user_id <> NEW.user_id
        LOOP
            INSERT INTO public.notifications (user_id, title, message, type, reference_id)
            VALUES (
                recipient_id,
                notification_title,
                notification_message,
                'JOIN_REQUEST',
                NEW.tontine_id
            );
        END LOOP;
    ELSIF NEW.status IS DISTINCT FROM OLD.status THEN
        IF upper(NEW.status) = 'ACCEPTED' THEN
            notification_title := 'Demande acceptée';
            notification_message := 'Votre demande pour ' ||
                COALESCE(tontine_name, 'la tontine') || ' a été acceptée.';
        ELSE
            notification_title := 'Mise à jour de votre demande';
            notification_message := 'Votre demande pour ' ||
                COALESCE(tontine_name, 'la tontine') ||
                ' est maintenant ' || lower(NEW.status) || '.';
        END IF;

        INSERT INTO public.notifications (user_id, title, message, type, reference_id)
        VALUES (
            NEW.user_id,
            notification_title,
            notification_message,
            'JOIN_REQUEST',
            NEW.tontine_id
        );
    END IF;

    RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION public.notify_tontine_comment()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, public
AS $$
DECLARE
    tontine_name text;
    author_name text;
BEGIN
    SELECT t.libelle INTO tontine_name
    FROM public.tontines t
    WHERE t.id = NEW.tontine_id;

    SELECT COALESCE(NULLIF(p.nom, ''), 'Un membre')
    INTO author_name
    FROM public.profiles p
    WHERE p.id = NEW.user_id;

    INSERT INTO public.notifications (user_id, title, message, type, reference_id)
    SELECT DISTINCT m.user_id,
           'Nouveau message',
           COALESCE(author_name, 'Un membre') || ' a écrit dans ' ||
               COALESCE(tontine_name, 'votre tontine') || '.',
           'CHAT_MESSAGE',
           NEW.id
    FROM public.members m
    WHERE m.tontine_id = NEW.tontine_id
      AND m.user_id IS NOT NULL
      AND m.user_id <> NEW.user_id;

    RETURN NEW;
END;
$$;

CREATE TRIGGER comment_notify_members
AFTER INSERT ON public.comments
FOR EACH ROW EXECUTE FUNCTION public.notify_tontine_comment();

REVOKE ALL ON FUNCTION public.notify_tontine_comment() FROM PUBLIC, anon, authenticated;

NOTIFY pgrst, 'reload schema';
