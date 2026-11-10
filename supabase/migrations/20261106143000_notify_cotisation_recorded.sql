CREATE OR REPLACE FUNCTION public.notify_transaction_member()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, public
AS $$
DECLARE
    recipient_id uuid;
    caisse_name text;
    cycle_number integer;
    tontine_name text;
    actor_name text;
BEGIN
    SELECT m.user_id, c.libelle, cy.numero_cycle, t.libelle
    INTO recipient_id, caisse_name, cycle_number, tontine_name
    FROM public.members m
    JOIN public.caisses c ON c.id = NEW.caisse_id
    JOIN public.tontines t ON t.id = c.tontine_id
    LEFT JOIN public.cycles cy
        ON cy.id = NEW.cycle_id
       AND cy.caisse_id = c.id
    WHERE m.id = NEW.member_id;

    IF recipient_id IS NULL THEN
        RETURN NEW;
    END IF;

    IF upper(COALESCE(NEW.type, '')) = 'COTISATION' THEN
        SELECT COALESCE(NULLIF(p.nom, ''), 'Le trésorier')
        INTO actor_name
        FROM public.profiles p
        WHERE p.id = NEW.validated_by_id;
        actor_name := COALESCE(actor_name, 'Le trésorier');

        INSERT INTO public.notifications (
            user_id, title, message, type, reference_id,
            group_key, actor_id, actor_name, context_name
        )
        VALUES (
            recipient_id,
            'Cotisation enregistrée',
            actor_name || ' a enregistré votre cotisation de ' ||
                NEW.montant::text || ' FCFA pour le cycle n°' ||
                COALESCE(cycle_number::text, '?') || ' dans la caisse « ' ||
                COALESCE(caisse_name, 'Sans nom') || ' ». Touchez pour afficher votre reçu.',
            'COTISATION_RECEIPT',
            NEW.id,
            'cotisation_receipt:' || NEW.id::text,
            NEW.validated_by_id,
            actor_name,
            COALESCE(caisse_name, 'Sans nom')
        );
    ELSE
        INSERT INTO public.notifications (user_id, title, message, type, reference_id)
        VALUES (
            recipient_id,
            'Mouvement enregistré',
            'Un mouvement de ' || NEW.montant::text || ' a été enregistré pour ' ||
                COALESCE(tontine_name, 'votre tontine') || '.',
            'RECEIPT',
            NEW.id
        );
    END IF;

    RETURN NEW;
END;
$$;

REVOKE ALL ON FUNCTION public.notify_transaction_member()
    FROM PUBLIC, anon, authenticated;

NOTIFY pgrst, 'reload schema';
