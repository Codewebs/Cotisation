-- Fresh Supabase schema for the Sequelize business models. All explicitly
-- millisecond-valued BIGINT date fields retain their original representation.

CREATE TABLE public.profiles (
    id uuid PRIMARY KEY REFERENCES auth.users (id) ON DELETE CASCADE,
    nom varchar(255),
    email varchar(255),
    phone varchar(255) UNIQUE,
    matricule varchar(255) UNIQUE,
    is_active boolean NOT NULL DEFAULT true,
    is_verified boolean NOT NULL DEFAULT false,
    profile jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE OR REPLACE FUNCTION public.handle_new_auth_user()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, public
AS $$
BEGIN
    INSERT INTO public.profiles (id, email, phone, nom, matricule, is_verified, profile)
    VALUES (
        NEW.id,
        NEW.email,
        COALESCE(NEW.phone, NULLIF(NEW.raw_user_meta_data ->> 'phone', '')),
        COALESCE(NULLIF(NEW.raw_user_meta_data ->> 'nom', ''),
                 NULLIF(NEW.raw_user_meta_data ->> 'name', '')),
        NULLIF(NEW.raw_user_meta_data ->> 'matricule', ''),
        NEW.email_confirmed_at IS NOT NULL,
        COALESCE(NEW.raw_user_meta_data, '{}'::jsonb)
    )
    ON CONFLICT (id) DO UPDATE
    SET email = EXCLUDED.email,
        phone = COALESCE(EXCLUDED.phone, public.profiles.phone),
        nom = COALESCE(EXCLUDED.nom, public.profiles.nom),
        matricule = COALESCE(EXCLUDED.matricule, public.profiles.matricule),
        is_verified = EXCLUDED.is_verified,
        profile = EXCLUDED.profile;
    RETURN NEW;
END;
$$;

CREATE TRIGGER on_auth_user_created_or_updated
AFTER INSERT OR UPDATE OF email, phone, email_confirmed_at, raw_user_meta_data ON auth.users
FOR EACH ROW EXECUTE FUNCTION public.handle_new_auth_user();

CREATE TABLE public.tontines (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    libelle varchar(255) NOT NULL,
    exercice varchar(255),
    date_debut varchar(255),
    date_fin varchar(255),
    creator_id uuid REFERENCES public.profiles (id) ON DELETE SET NULL,
    is_online boolean NOT NULL DEFAULT true,
    updated_at bigint
);

CREATE TABLE public.members (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    tontine_id uuid NOT NULL REFERENCES public.tontines (id) ON DELETE CASCADE,
    user_id uuid REFERENCES public.profiles (id) ON DELETE SET NULL,
    nom_saisi varchar(255) NOT NULL,
    phone varchar(255),
    role varchar(255) DEFAULT 'MEMBRE',
    is_initiator boolean NOT NULL DEFAULT false,
    joined_at bigint NOT NULL DEFAULT
        (floor(extract(epoch FROM clock_timestamp()) * 1000)::bigint)
);

CREATE TABLE public.caisses (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    tontine_id uuid NOT NULL REFERENCES public.tontines (id) ON DELETE CASCADE,
    type varchar(255),
    libelle varchar(255),
    montant_fixe bigint,
    montant_minimum bigint,
    regularite_imposee boolean NOT NULL DEFAULT false,
    redistribution boolean NOT NULL DEFAULT false,
    ordre_redistribution integer,
    taux_interet double precision,
    type_interet varchar(255),
    penalite_retard_fixe bigint,
    penalite_echec_fixe bigint,
    date_limite_jour integer,
    frais_caisse bigint,
    statut varchar(255) NOT NULL DEFAULT 'ACTIVE',
    respecter_frequence_strictement boolean NOT NULL DEFAULT false,
    ordre_fige boolean NOT NULL DEFAULT false,
    possibilite_pret boolean NOT NULL DEFAULT false,
    autoriser_pret_sur_amendes boolean NOT NULL DEFAULT false,
    frequence varchar(255) NOT NULL DEFAULT 'MENSUEL',
    duree_cycles integer,
    plafond_pret bigint,
    is_basique boolean NOT NULL DEFAULT false,
    updated_at bigint
);

CREATE TABLE public.caisse_participants (
    caisse_id uuid NOT NULL REFERENCES public.caisses (id) ON DELETE CASCADE,
    member_id uuid NOT NULL REFERENCES public.members (id) ON DELETE CASCADE,
    ordre_passage integer NOT NULL DEFAULT 0,
    nombre_parts integer NOT NULL DEFAULT 1,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (caisse_id, member_id)
);

CREATE TABLE public.cycles (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    caisse_id uuid NOT NULL REFERENCES public.caisses (id) ON DELETE CASCADE,
    numero_cycle integer NOT NULL,
    date_debut bigint,
    date_fin bigint,
    est_cloture boolean NOT NULL DEFAULT false,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE public.tours (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    cycle_id uuid NOT NULL REFERENCES public.cycles (id) ON DELETE CASCADE,
    beneficiary_id uuid REFERENCES public.members (id) ON DELETE SET NULL,
    numero_tour integer NOT NULL,
    date_tour bigint,
    est_termine boolean NOT NULL DEFAULT false,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE public.transactions (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    caisse_id uuid NOT NULL REFERENCES public.caisses (id) ON DELETE CASCADE,
    member_id uuid NOT NULL REFERENCES public.members (id) ON DELETE CASCADE,
    cycle_id uuid NOT NULL REFERENCES public.cycles (id) ON DELETE CASCADE,
    validated_by_id uuid REFERENCES public.profiles (id) ON DELETE SET NULL,
    montant bigint NOT NULL,
    type varchar(255) NOT NULL,
    motif varchar(255),
    date_transaction bigint NOT NULL DEFAULT
        (floor(extract(epoch FROM clock_timestamp()) * 1000)::bigint),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE public.prets (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    caisse_id uuid NOT NULL REFERENCES public.caisses (id) ON DELETE CASCADE,
    member_id uuid REFERENCES public.members (id) ON DELETE SET NULL,
    transaction_id uuid UNIQUE REFERENCES public.transactions (id) ON DELETE SET NULL,
    montant_principal bigint NOT NULL,
    montant_interets bigint NOT NULL DEFAULT 0,
    montant_restant bigint,
    taux_interet double precision NOT NULL DEFAULT 0,
    type_interet varchar(255),
    date_pret bigint NOT NULL DEFAULT
        (floor(extract(epoch FROM clock_timestamp()) * 1000)::bigint),
    date_echeance bigint,
    statut varchar(255) NOT NULL DEFAULT 'PENDING',
    nom_emprunteur_externe varchar(255),
    cni_emprunteur_externe varchar(255),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE public.amendes (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    caisse_id uuid NOT NULL REFERENCES public.caisses (id) ON DELETE CASCADE,
    member_id uuid NOT NULL REFERENCES public.members (id) ON DELETE CASCADE,
    cycle_id uuid NOT NULL REFERENCES public.cycles (id) ON DELETE CASCADE,
    montant bigint NOT NULL,
    motif varchar(255) NOT NULL,
    type varchar(255) NOT NULL DEFAULT 'AMENDE',
    statut varchar(255) NOT NULL DEFAULT 'A_PAYER',
    date_creation bigint NOT NULL DEFAULT
        (floor(extract(epoch FROM clock_timestamp()) * 1000)::bigint),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE public.sorties_caisse (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    caisse_id uuid NOT NULL REFERENCES public.caisses (id) ON DELETE CASCADE,
    montant bigint NOT NULL,
    motif varchar(255),
    date_sortie bigint NOT NULL DEFAULT
        (floor(extract(epoch FROM clock_timestamp()) * 1000)::bigint),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE public.notifications (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL REFERENCES public.profiles (id) ON DELETE CASCADE,
    title varchar(255) NOT NULL,
    message text,
    type varchar(255),
    reference_id uuid,
    is_read boolean NOT NULL DEFAULT false,
    created_at bigint NOT NULL DEFAULT
        (floor(extract(epoch FROM clock_timestamp()) * 1000)::bigint),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE public.invite_links (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    tontine_id uuid NOT NULL REFERENCES public.tontines (id) ON DELETE CASCADE,
    sender_id uuid NOT NULL REFERENCES public.profiles (id) ON DELETE CASCADE,
    code varchar(255) NOT NULL UNIQUE,
    created_at bigint NOT NULL DEFAULT
        (floor(extract(epoch FROM clock_timestamp()) * 1000)::bigint),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE public.join_requests (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    tontine_id uuid NOT NULL REFERENCES public.tontines (id) ON DELETE CASCADE,
    user_id uuid NOT NULL REFERENCES public.profiles (id) ON DELETE CASCADE,
    invite_code varchar(255),
    status varchar(255) NOT NULL DEFAULT 'PENDING',
    created_at bigint NOT NULL DEFAULT
        (floor(extract(epoch FROM clock_timestamp()) * 1000)::bigint),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE public.comments (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    tontine_id uuid NOT NULL REFERENCES public.tontines (id) ON DELETE CASCADE,
    user_id uuid NOT NULL REFERENCES public.profiles (id) ON DELETE CASCADE,
    content text NOT NULL,
    image_url text,
    created_at bigint NOT NULL DEFAULT
        (floor(extract(epoch FROM clock_timestamp()) * 1000)::bigint),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE public.user_devices (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL REFERENCES public.profiles (id) ON DELETE CASCADE,
    fcm_token text NOT NULL,
    platform varchar(255) NOT NULL DEFAULT 'ANDROID',
    updated_at bigint NOT NULL DEFAULT
        (floor(extract(epoch FROM clock_timestamp()) * 1000)::bigint),
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX members_tontine_id_idx ON public.members (tontine_id);
CREATE INDEX members_user_id_idx ON public.members (user_id);
CREATE INDEX tontines_creator_id_idx ON public.tontines (creator_id);
CREATE INDEX caisses_tontine_id_idx ON public.caisses (tontine_id);
CREATE INDEX caisse_participants_member_id_idx ON public.caisse_participants (member_id);
CREATE INDEX cycles_caisse_id_idx ON public.cycles (caisse_id);
CREATE INDEX tours_cycle_id_idx ON public.tours (cycle_id);
CREATE INDEX tours_beneficiary_id_idx ON public.tours (beneficiary_id);
CREATE INDEX transactions_caisse_id_idx ON public.transactions (caisse_id);
CREATE INDEX transactions_member_id_idx ON public.transactions (member_id);
CREATE INDEX transactions_cycle_id_idx ON public.transactions (cycle_id);
CREATE INDEX transactions_validated_by_id_idx ON public.transactions (validated_by_id);
CREATE INDEX prets_caisse_id_idx ON public.prets (caisse_id);
CREATE INDEX prets_member_id_idx ON public.prets (member_id);
CREATE INDEX amendes_caisse_id_idx ON public.amendes (caisse_id);
CREATE INDEX amendes_member_id_idx ON public.amendes (member_id);
CREATE INDEX amendes_cycle_id_idx ON public.amendes (cycle_id);
CREATE INDEX sorties_caisse_caisse_id_idx ON public.sorties_caisse (caisse_id);
CREATE INDEX notifications_user_created_idx ON public.notifications (user_id, created_at DESC);
CREATE INDEX invite_links_tontine_id_idx ON public.invite_links (tontine_id);
CREATE INDEX invite_links_sender_id_idx ON public.invite_links (sender_id);
CREATE INDEX join_requests_tontine_status_idx ON public.join_requests (tontine_id, status);
CREATE INDEX join_requests_user_id_idx ON public.join_requests (user_id);
CREATE UNIQUE INDEX join_requests_one_pending_per_user
    ON public.join_requests (tontine_id, user_id)
    WHERE status = 'PENDING';
CREATE INDEX comments_tontine_created_idx ON public.comments (tontine_id, created_at DESC);
CREATE INDEX comments_user_id_idx ON public.comments (user_id);
CREATE INDEX user_devices_user_id_idx ON public.user_devices (user_id);

CREATE OR REPLACE FUNCTION public.touch_updated_at()
RETURNS trigger
LANGUAGE plpgsql
SET search_path = pg_catalog
AS $$
BEGIN
    NEW.updated_at := now();
    RETURN NEW;
END;
$$;

CREATE TRIGGER profiles_touch_updated_at BEFORE UPDATE ON public.profiles
FOR EACH ROW EXECUTE FUNCTION public.touch_updated_at();
CREATE TRIGGER caisse_participants_touch_updated_at BEFORE UPDATE ON public.caisse_participants
FOR EACH ROW EXECUTE FUNCTION public.touch_updated_at();
CREATE TRIGGER cycles_touch_updated_at BEFORE UPDATE ON public.cycles
FOR EACH ROW EXECUTE FUNCTION public.touch_updated_at();
CREATE TRIGGER tours_touch_updated_at BEFORE UPDATE ON public.tours
FOR EACH ROW EXECUTE FUNCTION public.touch_updated_at();
CREATE TRIGGER transactions_touch_updated_at BEFORE UPDATE ON public.transactions
FOR EACH ROW EXECUTE FUNCTION public.touch_updated_at();
CREATE TRIGGER prets_touch_updated_at BEFORE UPDATE ON public.prets
FOR EACH ROW EXECUTE FUNCTION public.touch_updated_at();
CREATE TRIGGER amendes_touch_updated_at BEFORE UPDATE ON public.amendes
FOR EACH ROW EXECUTE FUNCTION public.touch_updated_at();
CREATE TRIGGER sorties_caisse_touch_updated_at BEFORE UPDATE ON public.sorties_caisse
FOR EACH ROW EXECUTE FUNCTION public.touch_updated_at();
CREATE TRIGGER notifications_touch_updated_at BEFORE UPDATE ON public.notifications
FOR EACH ROW EXECUTE FUNCTION public.touch_updated_at();
CREATE TRIGGER invite_links_touch_updated_at BEFORE UPDATE ON public.invite_links
FOR EACH ROW EXECUTE FUNCTION public.touch_updated_at();
CREATE TRIGGER join_requests_touch_updated_at BEFORE UPDATE ON public.join_requests
FOR EACH ROW EXECUTE FUNCTION public.touch_updated_at();
CREATE TRIGGER comments_touch_updated_at BEFORE UPDATE ON public.comments
FOR EACH ROW EXECUTE FUNCTION public.touch_updated_at();

CREATE OR REPLACE FUNCTION public.is_tontine_member(p_tontine_id uuid)
RETURNS boolean
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = pg_catalog, public
AS $$
    SELECT EXISTS (
        SELECT 1 FROM public.members m
        WHERE m.tontine_id = p_tontine_id
          AND m.user_id = (SELECT auth.uid())
    );
$$;

CREATE OR REPLACE FUNCTION public.is_tontine_admin(p_tontine_id uuid)
RETURNS boolean
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = pg_catalog, public
AS $$
    SELECT EXISTS (
        SELECT 1 FROM public.tontines t
        WHERE t.id = p_tontine_id AND t.creator_id = (SELECT auth.uid())
    ) OR EXISTS (
        SELECT 1 FROM public.members m
        WHERE m.tontine_id = p_tontine_id
          AND m.user_id = (SELECT auth.uid())
          AND (
              m.is_initiator
              OR upper(COALESCE(m.role, '')) IN
                 ('ADMIN', 'PRESIDENT', 'TRESORIER', 'SECRETAIRE', 'BUREAU')
          )
    );
$$;

CREATE OR REPLACE FUNCTION public.search_tontines(search_term text DEFAULT '')
RETURNS TABLE (
    id uuid,
    libelle varchar,
    exercice varchar,
    member_count bigint
)
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = pg_catalog, public
AS $$
    SELECT t.id, t.libelle, t.exercice, count(m.id)::bigint
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

CREATE OR REPLACE FUNCTION public.tontine_id_for_caisse(p_caisse_id uuid)
RETURNS uuid
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = pg_catalog, public
AS $$
    SELECT c.tontine_id
    FROM public.caisses c
    WHERE c.id = p_caisse_id
      AND (
          public.is_tontine_member(c.tontine_id)
          OR public.is_tontine_admin(c.tontine_id)
      );
$$;

CREATE OR REPLACE FUNCTION public.tontine_id_for_cycle(p_cycle_id uuid)
RETURNS uuid
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = pg_catalog, public
AS $$
    SELECT c.tontine_id
    FROM public.cycles y
    JOIN public.caisses c ON c.id = y.caisse_id
    WHERE y.id = p_cycle_id
      AND (
          public.is_tontine_member(c.tontine_id)
          OR public.is_tontine_admin(c.tontine_id)
      );
$$;

CREATE OR REPLACE FUNCTION public.member_belongs_to_tontine(
    p_member_id uuid, p_tontine_id uuid
)
RETURNS boolean
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = pg_catalog, public
AS $$
    SELECT EXISTS (
        SELECT 1 FROM public.members m
        WHERE m.id = p_member_id AND m.tontine_id = p_tontine_id
    ) AND public.is_tontine_admin(p_tontine_id);
$$;

CREATE OR REPLACE FUNCTION public.notify_join_request_changes()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, public
AS $$
DECLARE
    recipient_id uuid;
    tontine_name text;
BEGIN
    SELECT t.libelle INTO tontine_name
    FROM public.tontines t
    WHERE t.id = NEW.tontine_id;

    IF TG_OP = 'INSERT' THEN
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
                'Nouvelle demande d''adhésion',
                'Une personne souhaite rejoindre ' || COALESCE(tontine_name, 'votre tontine') || '.',
                'JOIN_REQUEST',
                NEW.tontine_id
            );
        END LOOP;
    ELSIF NEW.status IS DISTINCT FROM OLD.status THEN
        INSERT INTO public.notifications (user_id, title, message, type, reference_id)
        VALUES (
            NEW.user_id,
            'Mise à jour de votre demande',
            'Votre demande pour ' || COALESCE(tontine_name, 'la tontine') ||
                ' est maintenant ' || lower(NEW.status) || '.',
            'JOIN_REQUEST',
            NEW.tontine_id
        );
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER join_request_notify_insert
AFTER INSERT ON public.join_requests
FOR EACH ROW EXECUTE FUNCTION public.notify_join_request_changes();
CREATE TRIGGER join_request_notify_status
AFTER UPDATE OF status ON public.join_requests
FOR EACH ROW EXECUTE FUNCTION public.notify_join_request_changes();

CREATE OR REPLACE FUNCTION public.notify_transaction_member()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, public
AS $$
DECLARE
    recipient_id uuid;
    tontine_name text;
BEGIN
    SELECT m.user_id, t.libelle
    INTO recipient_id, tontine_name
    FROM public.members m
    JOIN public.caisses c ON c.id = NEW.caisse_id
    JOIN public.tontines t ON t.id = c.tontine_id
    WHERE m.id = NEW.member_id;

    IF recipient_id IS NOT NULL THEN
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

CREATE TRIGGER transaction_notify_member
AFTER INSERT ON public.transactions
FOR EACH ROW EXECUTE FUNCTION public.notify_transaction_member();

CREATE OR REPLACE FUNCTION public.join_tontine_with_invite(invite_code text)
RETURNS public.join_requests
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, public
AS $$
DECLARE
    actor_id uuid := (SELECT auth.uid());
    invitation public.invite_links%ROWTYPE;
    new_request public.join_requests%ROWTYPE;
BEGIN
    IF actor_id IS NULL THEN
        RAISE EXCEPTION 'Authentication is required';
    END IF;

    SELECT * INTO invitation
    FROM public.invite_links
    WHERE code = btrim(invite_code);

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Invitation code is invalid';
    END IF;

    IF EXISTS (
        SELECT 1 FROM public.members
        WHERE tontine_id = invitation.tontine_id AND user_id = actor_id
    ) THEN
        RAISE EXCEPTION 'User is already a member of this tontine';
    END IF;

    INSERT INTO public.join_requests (tontine_id, user_id, invite_code)
    VALUES (invitation.tontine_id, actor_id, invitation.code)
    RETURNING * INTO new_request;

    RETURN new_request;
END;
$$;

CREATE OR REPLACE FUNCTION public.get_tontine_invite(invite_code text)
RETURNS TABLE (
    tontine_id uuid,
    tontine_name varchar,
    member_count bigint
)
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = pg_catalog, public
AS $$
    SELECT t.id, t.libelle, count(m.id)::bigint
    FROM public.invite_links i
    JOIN public.tontines t ON t.id = i.tontine_id
    LEFT JOIN public.members m ON m.tontine_id = t.id
    WHERE i.code = btrim(get_tontine_invite.invite_code)
    GROUP BY t.id
    LIMIT 1;
$$;

CREATE OR REPLACE FUNCTION public.find_tontine_user_by_phone(
    p_tontine_id uuid, p_phone text
)
RETURNS TABLE (
    id uuid,
    nom varchar,
    phone varchar,
    matricule varchar
)
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = pg_catalog, public
AS $$
    SELECT p.id, p.nom, p.phone, p.matricule
    FROM public.profiles p
    WHERE public.is_tontine_admin(p_tontine_id)
      AND p.phone = btrim(p_phone)
    LIMIT 1;
$$;

CREATE OR REPLACE FUNCTION public.approve_tontine_join_request(
    p_request_id uuid, p_target_member_id uuid DEFAULT NULL
)
RETURNS uuid
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, public
AS $$
DECLARE
    actor_id uuid := (SELECT auth.uid());
    request_row public.join_requests%ROWTYPE;
    profile_row public.profiles%ROWTYPE;
    result_member_id uuid;
BEGIN
    SELECT * INTO request_row
    FROM public.join_requests
    WHERE id = p_request_id
    FOR UPDATE;

    IF NOT FOUND OR request_row.status <> 'PENDING'
       OR NOT public.is_tontine_admin(request_row.tontine_id) THEN
        RAISE EXCEPTION 'Join request not found or not authorized';
    END IF;

    IF p_target_member_id IS NOT NULL THEN
        UPDATE public.members
        SET user_id = request_row.user_id
        WHERE id = p_target_member_id
          AND tontine_id = request_row.tontine_id
          AND user_id IS NULL
        RETURNING id INTO result_member_id;

        IF result_member_id IS NULL THEN
            RAISE EXCEPTION 'Target member is invalid or already linked';
        END IF;
    ELSE
        SELECT * INTO profile_row
        FROM public.profiles
        WHERE id = request_row.user_id;

        INSERT INTO public.members (
            tontine_id, user_id, nom_saisi, phone, role
        )
        VALUES (
            request_row.tontine_id,
            request_row.user_id,
            COALESCE(NULLIF(profile_row.nom, ''), 'Membre'),
            profile_row.phone,
            'MEMBRE'
        )
        RETURNING id INTO result_member_id;
    END IF;

    UPDATE public.join_requests
    SET status = 'ACCEPTED'
    WHERE id = request_row.id;

    RETURN result_member_id;
END;
$$;

CREATE OR REPLACE FUNCTION public.broadcast_tontine_report(p_tontine_id uuid)
RETURNS integer
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, public
AS $$
DECLARE
    actor_id uuid := (SELECT auth.uid());
    tontine_name text;
    inserted_count integer;
BEGIN
    IF NOT public.is_tontine_admin(p_tontine_id) THEN
        RAISE EXCEPTION 'Only tontine administrators can broadcast reports';
    END IF;

    SELECT libelle INTO tontine_name
    FROM public.tontines
    WHERE id = p_tontine_id;

    INSERT INTO public.notifications (user_id, title, message, type, reference_id)
    SELECT DISTINCT m.user_id,
           'Rapport disponible',
           'Le rapport financier de ' || COALESCE(tontine_name, 'votre tontine') || ' est disponible.',
           'REPORT',
           p_tontine_id
    FROM public.members m
    WHERE m.tontine_id = p_tontine_id
      AND m.user_id IS NOT NULL;

    GET DIAGNOSTICS inserted_count = ROW_COUNT;
    RETURN inserted_count;
END;
$$;

CREATE OR REPLACE FUNCTION public.approve_tontine_loan(p_loan_id uuid)
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, public
AS $$
DECLARE
    loan_row public.prets%ROWTYPE;
    cycle_id uuid;
    created_transaction_id uuid;
BEGIN
    SELECT * INTO loan_row
    FROM public.prets
    WHERE id = p_loan_id
    FOR UPDATE;

    IF NOT FOUND OR loan_row.statut <> 'PENDING'
       OR NOT public.is_tontine_admin(public.tontine_id_for_caisse(loan_row.caisse_id)) THEN
        RAISE EXCEPTION 'Loan not found or not authorized';
    END IF;

    SELECT id INTO cycle_id
    FROM public.cycles
    WHERE caisse_id = loan_row.caisse_id AND NOT est_cloture
    ORDER BY numero_cycle DESC
    LIMIT 1;

    IF cycle_id IS NULL THEN
        RAISE EXCEPTION 'No active cycle exists for this fund';
    END IF;

    IF loan_row.member_id IS NOT NULL THEN
        INSERT INTO public.transactions (
            caisse_id, member_id, cycle_id, validated_by_id, montant, type, motif
        )
        VALUES (
            loan_row.caisse_id, loan_row.member_id, cycle_id, (SELECT auth.uid()),
            -abs(loan_row.montant_principal), 'DECAISSEMENT_PRET', 'Décaissement du prêt'
        )
        RETURNING id INTO created_transaction_id;
    END IF;

    UPDATE public.prets AS p
    SET statut = 'ACTIF',
        montant_restant = COALESCE(
            montant_restant, montant_principal + montant_interets
        ),
        transaction_id = COALESCE(p.transaction_id, created_transaction_id)
    WHERE p.id = loan_row.id;
END;
$$;

CREATE OR REPLACE FUNCTION public.repay_tontine_loan(
    p_loan_id uuid, p_amount bigint, p_motif text DEFAULT NULL
)
RETURNS bigint
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, public
AS $$
DECLARE
    loan_row public.prets%ROWTYPE;
    cycle_id uuid;
    remaining_amount bigint;
BEGIN
    SELECT * INTO loan_row
    FROM public.prets
    WHERE id = p_loan_id
    FOR UPDATE;

    IF NOT FOUND OR loan_row.statut <> 'ACTIF'
       OR NOT public.is_tontine_admin(public.tontine_id_for_caisse(loan_row.caisse_id)) THEN
        RAISE EXCEPTION 'Active loan not found or not authorized';
    END IF;

    remaining_amount := COALESCE(
        loan_row.montant_restant, loan_row.montant_principal + loan_row.montant_interets
    );
    IF p_amount IS NULL OR p_amount <= 0 OR p_amount > remaining_amount THEN
        RAISE EXCEPTION 'Repayment amount is invalid or exceeds the remaining balance';
    END IF;

    IF loan_row.member_id IS NOT NULL THEN
        SELECT id INTO cycle_id
        FROM public.cycles
        WHERE caisse_id = loan_row.caisse_id AND NOT est_cloture
        ORDER BY numero_cycle DESC
        LIMIT 1;

        IF cycle_id IS NULL THEN
            RAISE EXCEPTION 'No active cycle exists for this fund';
        END IF;

        INSERT INTO public.transactions (
            caisse_id, member_id, cycle_id, validated_by_id, montant, type, motif
        )
        VALUES (
            loan_row.caisse_id, loan_row.member_id, cycle_id, (SELECT auth.uid()),
            p_amount, 'REMBOURSEMENT', COALESCE(NULLIF(p_motif, ''), 'Remboursement de prêt')
        );
    END IF;

    UPDATE public.prets
    SET montant_restant = remaining_amount - p_amount,
        statut = CASE WHEN remaining_amount = p_amount THEN 'SOLDE' ELSE 'ACTIF' END
    WHERE id = loan_row.id;

    RETURN remaining_amount - p_amount;
END;
$$;

CREATE OR REPLACE FUNCTION public.distribute_tontine_pot(p_tour_id uuid, p_amount bigint)
RETURNS uuid
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, public
AS $$
DECLARE
    tour_row public.tours%ROWTYPE;
    cycle_row public.cycles%ROWTYPE;
    caisse_row public.caisses%ROWTYPE;
    transaction_id uuid;
BEGIN
    IF p_amount IS NULL OR p_amount <= 0 THEN
        RAISE EXCEPTION 'Distribution amount must be positive';
    END IF;

    SELECT * INTO tour_row
    FROM public.tours
    WHERE id = p_tour_id
    FOR UPDATE;

    IF NOT FOUND OR tour_row.est_termine THEN
        RAISE EXCEPTION 'Active payout turn not found';
    END IF;

    SELECT * INTO cycle_row FROM public.cycles WHERE id = tour_row.cycle_id;
    SELECT * INTO caisse_row FROM public.caisses WHERE id = cycle_row.caisse_id;
    IF NOT public.is_tontine_admin(caisse_row.tontine_id) THEN
        RAISE EXCEPTION 'Only tontine administrators can distribute funds';
    END IF;
    IF tour_row.beneficiary_id IS NULL THEN
        RAISE EXCEPTION 'The payout turn has no beneficiary';
    END IF;

    INSERT INTO public.transactions (
        caisse_id, member_id, cycle_id, validated_by_id, montant, type, motif
    )
    VALUES (
        caisse_row.id, tour_row.beneficiary_id, cycle_row.id, (SELECT auth.uid()),
        -abs(p_amount), 'DISTRIBUTION', 'Distribution de la cagnotte'
    )
    RETURNING id INTO transaction_id;

    UPDATE public.tours SET est_termine = true WHERE id = tour_row.id;
    RETURN transaction_id;
END;
$$;

CREATE OR REPLACE FUNCTION public.pay_tontine_fine(p_fine_id uuid)
RETURNS uuid
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, public
AS $$
DECLARE
    fine_row public.amendes%ROWTYPE;
    transaction_id uuid;
BEGIN
    SELECT * INTO fine_row
    FROM public.amendes
    WHERE id = p_fine_id
    FOR UPDATE;

    IF NOT FOUND OR fine_row.statut IN ('PAYE', 'PAYEE')
       OR NOT public.is_tontine_admin(public.tontine_id_for_caisse(fine_row.caisse_id)) THEN
        RAISE EXCEPTION 'Unpaid fine not found or not authorized';
    END IF;

    INSERT INTO public.transactions (
        caisse_id, member_id, cycle_id, validated_by_id, montant, type, motif
    )
    VALUES (
        fine_row.caisse_id, fine_row.member_id, fine_row.cycle_id, (SELECT auth.uid()),
        fine_row.montant, 'AMENDE', fine_row.motif
    )
    RETURNING id INTO transaction_id;

    UPDATE public.amendes SET statut = 'PAYE' WHERE id = fine_row.id;
    RETURN transaction_id;
END;
$$;

REVOKE ALL ON FUNCTION public.handle_new_auth_user() FROM PUBLIC, anon, authenticated;
REVOKE ALL ON FUNCTION public.touch_updated_at() FROM PUBLIC, anon, authenticated;
REVOKE ALL ON FUNCTION public.notify_join_request_changes() FROM PUBLIC, anon, authenticated;
REVOKE ALL ON FUNCTION public.notify_transaction_member() FROM PUBLIC, anon, authenticated;
REVOKE ALL ON FUNCTION public.join_tontine_with_invite(text) FROM PUBLIC, anon;
REVOKE ALL ON FUNCTION public.get_tontine_invite(text) FROM PUBLIC, anon;
REVOKE ALL ON FUNCTION public.find_tontine_user_by_phone(uuid, text) FROM PUBLIC, anon;
REVOKE ALL ON FUNCTION public.approve_tontine_join_request(uuid, uuid) FROM PUBLIC, anon;
REVOKE ALL ON FUNCTION public.broadcast_tontine_report(uuid) FROM PUBLIC, anon;
REVOKE ALL ON FUNCTION public.approve_tontine_loan(uuid) FROM PUBLIC, anon;
REVOKE ALL ON FUNCTION public.repay_tontine_loan(uuid, bigint, text) FROM PUBLIC, anon;
REVOKE ALL ON FUNCTION public.distribute_tontine_pot(uuid, bigint) FROM PUBLIC, anon;
REVOKE ALL ON FUNCTION public.pay_tontine_fine(uuid) FROM PUBLIC, anon;
REVOKE ALL ON FUNCTION public.is_tontine_member(uuid) FROM PUBLIC, anon;
REVOKE ALL ON FUNCTION public.is_tontine_admin(uuid) FROM PUBLIC, anon;
REVOKE ALL ON FUNCTION public.search_tontines(text) FROM PUBLIC, anon;
REVOKE ALL ON FUNCTION public.tontine_id_for_caisse(uuid) FROM PUBLIC, anon;
REVOKE ALL ON FUNCTION public.tontine_id_for_cycle(uuid) FROM PUBLIC, anon;
REVOKE ALL ON FUNCTION public.member_belongs_to_tontine(uuid, uuid) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.is_tontine_member(uuid) TO authenticated;
GRANT EXECUTE ON FUNCTION public.is_tontine_admin(uuid) TO authenticated;
GRANT EXECUTE ON FUNCTION public.search_tontines(text) TO authenticated;
GRANT EXECUTE ON FUNCTION public.tontine_id_for_caisse(uuid) TO authenticated;
GRANT EXECUTE ON FUNCTION public.tontine_id_for_cycle(uuid) TO authenticated;
GRANT EXECUTE ON FUNCTION public.member_belongs_to_tontine(uuid, uuid) TO authenticated;
GRANT EXECUTE ON FUNCTION public.join_tontine_with_invite(text) TO authenticated;
GRANT EXECUTE ON FUNCTION public.get_tontine_invite(text) TO authenticated;
GRANT EXECUTE ON FUNCTION public.find_tontine_user_by_phone(uuid, text) TO authenticated;
GRANT EXECUTE ON FUNCTION public.approve_tontine_join_request(uuid, uuid) TO authenticated;
GRANT EXECUTE ON FUNCTION public.broadcast_tontine_report(uuid) TO authenticated;
GRANT EXECUTE ON FUNCTION public.approve_tontine_loan(uuid) TO authenticated;
GRANT EXECUTE ON FUNCTION public.repay_tontine_loan(uuid, bigint, text) TO authenticated;
GRANT EXECUTE ON FUNCTION public.distribute_tontine_pot(uuid, bigint) TO authenticated;
GRANT EXECUTE ON FUNCTION public.pay_tontine_fine(uuid) TO authenticated;

ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.tontines ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.members ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.caisses ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.caisse_participants ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.cycles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.tours ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.transactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.prets ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.amendes ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.sorties_caisse ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.notifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.invite_links ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.join_requests ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.comments ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_devices ENABLE ROW LEVEL SECURITY;

CREATE POLICY profiles_read_self ON public.profiles FOR SELECT TO authenticated
USING (id = (SELECT auth.uid()));
CREATE POLICY profiles_insert_self ON public.profiles FOR INSERT TO authenticated
WITH CHECK (id = (SELECT auth.uid()));
CREATE POLICY profiles_update_self ON public.profiles FOR UPDATE TO authenticated
USING (id = (SELECT auth.uid())) WITH CHECK (id = (SELECT auth.uid()));

CREATE POLICY tontines_read_members ON public.tontines FOR SELECT TO authenticated
USING (public.is_tontine_member(id) OR public.is_tontine_admin(id));
CREATE POLICY tontines_create_self ON public.tontines FOR INSERT TO authenticated
WITH CHECK (creator_id = (SELECT auth.uid()));
CREATE POLICY tontines_update_admin ON public.tontines FOR UPDATE TO authenticated
USING (public.is_tontine_admin(id)) WITH CHECK (public.is_tontine_admin(id));
CREATE POLICY tontines_delete_admin ON public.tontines FOR DELETE TO authenticated
USING (public.is_tontine_admin(id));

CREATE POLICY members_read_group ON public.members FOR SELECT TO authenticated
USING (public.is_tontine_member(tontine_id));
CREATE POLICY members_insert_admin ON public.members FOR INSERT TO authenticated
WITH CHECK (public.is_tontine_admin(tontine_id));
CREATE POLICY members_update_admin ON public.members FOR UPDATE TO authenticated
USING (public.is_tontine_admin(tontine_id)) WITH CHECK (public.is_tontine_admin(tontine_id));
CREATE POLICY members_delete_admin ON public.members FOR DELETE TO authenticated
USING (public.is_tontine_admin(tontine_id));

CREATE POLICY caisses_read_group ON public.caisses FOR SELECT TO authenticated
USING (public.is_tontine_member(tontine_id));
CREATE POLICY caisses_insert_admin ON public.caisses FOR INSERT TO authenticated
WITH CHECK (public.is_tontine_admin(tontine_id));
CREATE POLICY caisses_update_admin ON public.caisses FOR UPDATE TO authenticated
USING (public.is_tontine_admin(tontine_id)) WITH CHECK (public.is_tontine_admin(tontine_id));
CREATE POLICY caisses_delete_admin ON public.caisses FOR DELETE TO authenticated
USING (public.is_tontine_admin(tontine_id));

CREATE POLICY caisse_participants_read_group ON public.caisse_participants FOR SELECT TO authenticated
USING (public.is_tontine_member(public.tontine_id_for_caisse(caisse_id)));
CREATE POLICY caisse_participants_insert_admin ON public.caisse_participants FOR INSERT TO authenticated
WITH CHECK (public.is_tontine_admin(public.tontine_id_for_caisse(caisse_id))
            AND public.member_belongs_to_tontine(
                member_id, public.tontine_id_for_caisse(caisse_id)));
CREATE POLICY caisse_participants_update_admin ON public.caisse_participants FOR UPDATE TO authenticated
USING (public.is_tontine_admin(public.tontine_id_for_caisse(caisse_id)))
WITH CHECK (public.is_tontine_admin(public.tontine_id_for_caisse(caisse_id))
            AND public.member_belongs_to_tontine(
                member_id, public.tontine_id_for_caisse(caisse_id)));
CREATE POLICY caisse_participants_delete_admin ON public.caisse_participants FOR DELETE TO authenticated
USING (public.is_tontine_admin(public.tontine_id_for_caisse(caisse_id)));

CREATE POLICY cycles_read_group ON public.cycles FOR SELECT TO authenticated
USING (public.is_tontine_member(public.tontine_id_for_caisse(caisse_id)));
CREATE POLICY cycles_insert_admin ON public.cycles FOR INSERT TO authenticated
WITH CHECK (public.is_tontine_admin(public.tontine_id_for_caisse(caisse_id)));
CREATE POLICY cycles_update_admin ON public.cycles FOR UPDATE TO authenticated
USING (public.is_tontine_admin(public.tontine_id_for_caisse(caisse_id)))
WITH CHECK (public.is_tontine_admin(public.tontine_id_for_caisse(caisse_id)));
CREATE POLICY cycles_delete_admin ON public.cycles FOR DELETE TO authenticated
USING (public.is_tontine_admin(public.tontine_id_for_caisse(caisse_id)));

CREATE POLICY tours_read_group ON public.tours FOR SELECT TO authenticated
USING (public.is_tontine_member(public.tontine_id_for_cycle(cycle_id)));
CREATE POLICY tours_insert_admin ON public.tours FOR INSERT TO authenticated
WITH CHECK (
    public.is_tontine_admin(public.tontine_id_for_cycle(cycle_id))
    AND (beneficiary_id IS NULL OR public.member_belongs_to_tontine(
        beneficiary_id, public.tontine_id_for_cycle(cycle_id)))
);
CREATE POLICY tours_update_admin ON public.tours FOR UPDATE TO authenticated
USING (public.is_tontine_admin(public.tontine_id_for_cycle(cycle_id)))
WITH CHECK (
    public.is_tontine_admin(public.tontine_id_for_cycle(cycle_id))
    AND (beneficiary_id IS NULL OR public.member_belongs_to_tontine(
        beneficiary_id, public.tontine_id_for_cycle(cycle_id)))
);
CREATE POLICY tours_delete_admin ON public.tours FOR DELETE TO authenticated
USING (public.is_tontine_admin(public.tontine_id_for_cycle(cycle_id)));

CREATE POLICY transactions_read_group ON public.transactions FOR SELECT TO authenticated
USING (public.is_tontine_member(public.tontine_id_for_caisse(caisse_id)));
CREATE POLICY transactions_insert_admin ON public.transactions FOR INSERT TO authenticated
WITH CHECK (public.is_tontine_admin(public.tontine_id_for_caisse(caisse_id))
            AND public.member_belongs_to_tontine(
                member_id, public.tontine_id_for_caisse(caisse_id))
            AND public.tontine_id_for_cycle(cycle_id)
                = public.tontine_id_for_caisse(caisse_id));
CREATE POLICY transactions_update_admin ON public.transactions FOR UPDATE TO authenticated
USING (public.is_tontine_admin(public.tontine_id_for_caisse(caisse_id)))
WITH CHECK (public.is_tontine_admin(public.tontine_id_for_caisse(caisse_id))
            AND public.member_belongs_to_tontine(
                member_id, public.tontine_id_for_caisse(caisse_id))
            AND public.tontine_id_for_cycle(cycle_id)
                = public.tontine_id_for_caisse(caisse_id));
CREATE POLICY transactions_delete_admin ON public.transactions FOR DELETE TO authenticated
USING (public.is_tontine_admin(public.tontine_id_for_caisse(caisse_id)));

CREATE POLICY prets_read_group ON public.prets FOR SELECT TO authenticated
USING (public.is_tontine_member(public.tontine_id_for_caisse(caisse_id)));
CREATE POLICY prets_insert_admin ON public.prets FOR INSERT TO authenticated
WITH CHECK (public.is_tontine_admin(public.tontine_id_for_caisse(caisse_id))
            AND (member_id IS NULL OR public.member_belongs_to_tontine(
                member_id, public.tontine_id_for_caisse(caisse_id))));
CREATE POLICY prets_update_admin ON public.prets FOR UPDATE TO authenticated
USING (public.is_tontine_admin(public.tontine_id_for_caisse(caisse_id)))
WITH CHECK (public.is_tontine_admin(public.tontine_id_for_caisse(caisse_id))
            AND (member_id IS NULL OR public.member_belongs_to_tontine(
                member_id, public.tontine_id_for_caisse(caisse_id))));
CREATE POLICY prets_delete_admin ON public.prets FOR DELETE TO authenticated
USING (public.is_tontine_admin(public.tontine_id_for_caisse(caisse_id)));

CREATE POLICY amendes_read_group ON public.amendes FOR SELECT TO authenticated
USING (public.is_tontine_member(public.tontine_id_for_caisse(caisse_id)));
CREATE POLICY amendes_insert_admin ON public.amendes FOR INSERT TO authenticated
WITH CHECK (public.is_tontine_admin(public.tontine_id_for_caisse(caisse_id))
            AND public.member_belongs_to_tontine(
                member_id, public.tontine_id_for_caisse(caisse_id))
            AND public.tontine_id_for_cycle(cycle_id)
                = public.tontine_id_for_caisse(caisse_id));
CREATE POLICY amendes_update_admin ON public.amendes FOR UPDATE TO authenticated
USING (public.is_tontine_admin(public.tontine_id_for_caisse(caisse_id)))
WITH CHECK (public.is_tontine_admin(public.tontine_id_for_caisse(caisse_id))
            AND public.member_belongs_to_tontine(
                member_id, public.tontine_id_for_caisse(caisse_id))
            AND public.tontine_id_for_cycle(cycle_id)
                = public.tontine_id_for_caisse(caisse_id));
CREATE POLICY amendes_delete_admin ON public.amendes FOR DELETE TO authenticated
USING (public.is_tontine_admin(public.tontine_id_for_caisse(caisse_id)));

CREATE POLICY sorties_caisse_read_group ON public.sorties_caisse FOR SELECT TO authenticated
USING (public.is_tontine_member(public.tontine_id_for_caisse(caisse_id)));
CREATE POLICY sorties_caisse_insert_admin ON public.sorties_caisse FOR INSERT TO authenticated
WITH CHECK (public.is_tontine_admin(public.tontine_id_for_caisse(caisse_id)));
CREATE POLICY sorties_caisse_update_admin ON public.sorties_caisse FOR UPDATE TO authenticated
USING (public.is_tontine_admin(public.tontine_id_for_caisse(caisse_id)))
WITH CHECK (public.is_tontine_admin(public.tontine_id_for_caisse(caisse_id)));
CREATE POLICY sorties_caisse_delete_admin ON public.sorties_caisse FOR DELETE TO authenticated
USING (public.is_tontine_admin(public.tontine_id_for_caisse(caisse_id)));

CREATE POLICY notifications_read_recipient ON public.notifications FOR SELECT TO authenticated
USING (user_id = (SELECT auth.uid()));
CREATE POLICY notifications_update_recipient ON public.notifications FOR UPDATE TO authenticated
USING (user_id = (SELECT auth.uid())) WITH CHECK (user_id = (SELECT auth.uid()));

CREATE POLICY invite_links_read_admin ON public.invite_links FOR SELECT TO authenticated
USING (public.is_tontine_member(tontine_id) AND public.is_tontine_admin(tontine_id));
CREATE POLICY invite_links_insert_admin ON public.invite_links FOR INSERT TO authenticated
WITH CHECK (public.is_tontine_admin(tontine_id) AND sender_id = (SELECT auth.uid()));
CREATE POLICY invite_links_update_admin ON public.invite_links FOR UPDATE TO authenticated
USING (public.is_tontine_admin(tontine_id)) WITH CHECK (public.is_tontine_admin(tontine_id));
CREATE POLICY invite_links_delete_admin ON public.invite_links FOR DELETE TO authenticated
USING (public.is_tontine_admin(tontine_id));

CREATE POLICY join_requests_read_requester_or_group ON public.join_requests FOR SELECT TO authenticated
USING (user_id = (SELECT auth.uid()) OR public.is_tontine_admin(tontine_id));
CREATE POLICY join_requests_insert_self ON public.join_requests FOR INSERT TO authenticated
WITH CHECK (user_id = (SELECT auth.uid()) AND status = 'PENDING');
CREATE POLICY join_requests_update_admin ON public.join_requests FOR UPDATE TO authenticated
USING (public.is_tontine_admin(tontine_id)) WITH CHECK (public.is_tontine_admin(tontine_id));
CREATE POLICY join_requests_withdraw_self ON public.join_requests FOR DELETE TO authenticated
USING (user_id = (SELECT auth.uid()) AND status = 'PENDING');

CREATE POLICY comments_read_group ON public.comments FOR SELECT TO authenticated
USING (public.is_tontine_member(tontine_id));
CREATE POLICY comments_insert_member ON public.comments FOR INSERT TO authenticated
WITH CHECK (user_id = (SELECT auth.uid()) AND public.is_tontine_member(tontine_id));
CREATE POLICY comments_update_author_or_admin ON public.comments FOR UPDATE TO authenticated
USING (user_id = (SELECT auth.uid()) OR public.is_tontine_admin(tontine_id))
WITH CHECK (user_id = (SELECT auth.uid()) OR public.is_tontine_admin(tontine_id));
CREATE POLICY comments_delete_author_or_admin ON public.comments FOR DELETE TO authenticated
USING (user_id = (SELECT auth.uid()) OR public.is_tontine_admin(tontine_id));

CREATE POLICY user_devices_read_self ON public.user_devices FOR SELECT TO authenticated
USING (user_id = (SELECT auth.uid()));
CREATE POLICY user_devices_insert_self ON public.user_devices FOR INSERT TO authenticated
WITH CHECK (user_id = (SELECT auth.uid()));
CREATE POLICY user_devices_update_self ON public.user_devices FOR UPDATE TO authenticated
USING (user_id = (SELECT auth.uid())) WITH CHECK (user_id = (SELECT auth.uid()));
CREATE POLICY user_devices_delete_self ON public.user_devices FOR DELETE TO authenticated
USING (user_id = (SELECT auth.uid()));

-- Restrictive policies are defense-in-depth gates; the permissive policies
-- above additionally define which operations each role may perform.
CREATE POLICY profiles_identity_gate ON public.profiles AS RESTRICTIVE FOR ALL TO authenticated
USING (id = (SELECT auth.uid())) WITH CHECK (id = (SELECT auth.uid()));
CREATE POLICY tontines_membership_gate ON public.tontines AS RESTRICTIVE FOR ALL TO authenticated
USING (public.is_tontine_member(id) OR public.is_tontine_admin(id))
WITH CHECK (public.is_tontine_admin(id) OR creator_id = (SELECT auth.uid()));
CREATE POLICY members_admin_gate ON public.members AS RESTRICTIVE FOR ALL TO authenticated
USING (public.is_tontine_member(tontine_id) OR public.is_tontine_admin(tontine_id))
WITH CHECK (public.is_tontine_admin(tontine_id));
CREATE POLICY caisses_admin_gate ON public.caisses AS RESTRICTIVE FOR ALL TO authenticated
USING (public.is_tontine_member(tontine_id) OR public.is_tontine_admin(tontine_id))
WITH CHECK (public.is_tontine_admin(tontine_id));
CREATE POLICY caisse_participants_admin_gate ON public.caisse_participants AS RESTRICTIVE FOR ALL TO authenticated
USING (
    public.is_tontine_member(public.tontine_id_for_caisse(caisse_id))
    OR public.is_tontine_admin(public.tontine_id_for_caisse(caisse_id))
)
WITH CHECK (public.is_tontine_admin(public.tontine_id_for_caisse(caisse_id)));
CREATE POLICY cycles_admin_gate ON public.cycles AS RESTRICTIVE FOR ALL TO authenticated
USING (
    public.is_tontine_member(public.tontine_id_for_caisse(caisse_id))
    OR public.is_tontine_admin(public.tontine_id_for_caisse(caisse_id))
)
WITH CHECK (public.is_tontine_admin(public.tontine_id_for_caisse(caisse_id)));
CREATE POLICY tours_admin_gate ON public.tours AS RESTRICTIVE FOR ALL TO authenticated
USING (
    public.is_tontine_member(public.tontine_id_for_cycle(cycle_id))
    OR public.is_tontine_admin(public.tontine_id_for_cycle(cycle_id))
)
WITH CHECK (public.is_tontine_admin(public.tontine_id_for_cycle(cycle_id)));
CREATE POLICY transactions_admin_gate ON public.transactions AS RESTRICTIVE FOR ALL TO authenticated
USING (
    public.is_tontine_member(public.tontine_id_for_caisse(caisse_id))
    OR public.is_tontine_admin(public.tontine_id_for_caisse(caisse_id))
)
WITH CHECK (public.is_tontine_admin(public.tontine_id_for_caisse(caisse_id)));
CREATE POLICY prets_admin_gate ON public.prets AS RESTRICTIVE FOR ALL TO authenticated
USING (
    public.is_tontine_member(public.tontine_id_for_caisse(caisse_id))
    OR public.is_tontine_admin(public.tontine_id_for_caisse(caisse_id))
)
WITH CHECK (public.is_tontine_admin(public.tontine_id_for_caisse(caisse_id)));
CREATE POLICY amendes_admin_gate ON public.amendes AS RESTRICTIVE FOR ALL TO authenticated
USING (
    public.is_tontine_member(public.tontine_id_for_caisse(caisse_id))
    OR public.is_tontine_admin(public.tontine_id_for_caisse(caisse_id))
)
WITH CHECK (public.is_tontine_admin(public.tontine_id_for_caisse(caisse_id)));
CREATE POLICY sorties_caisse_admin_gate ON public.sorties_caisse AS RESTRICTIVE FOR ALL TO authenticated
USING (
    public.is_tontine_member(public.tontine_id_for_caisse(caisse_id))
    OR public.is_tontine_admin(public.tontine_id_for_caisse(caisse_id))
)
WITH CHECK (public.is_tontine_admin(public.tontine_id_for_caisse(caisse_id)));
CREATE POLICY notifications_recipient_gate ON public.notifications AS RESTRICTIVE FOR ALL TO authenticated
USING (user_id = (SELECT auth.uid())) WITH CHECK (user_id = (SELECT auth.uid()));
CREATE POLICY invite_links_admin_gate ON public.invite_links AS RESTRICTIVE FOR ALL TO authenticated
USING (public.is_tontine_member(tontine_id) OR public.is_tontine_admin(tontine_id))
WITH CHECK (public.is_tontine_admin(tontine_id));
CREATE POLICY join_requests_identity_gate ON public.join_requests AS RESTRICTIVE FOR ALL TO authenticated
USING (
    user_id = (SELECT auth.uid())
    OR public.is_tontine_member(tontine_id)
    OR public.is_tontine_admin(tontine_id)
)
WITH CHECK (
    (user_id = (SELECT auth.uid()) AND status = 'PENDING')
    OR public.is_tontine_admin(tontine_id)
);
CREATE POLICY comments_membership_gate ON public.comments AS RESTRICTIVE FOR ALL TO authenticated
USING (public.is_tontine_member(tontine_id))
WITH CHECK (
    public.is_tontine_member(tontine_id)
    AND (user_id = (SELECT auth.uid()) OR public.is_tontine_admin(tontine_id))
);
CREATE POLICY user_devices_identity_gate ON public.user_devices AS RESTRICTIVE FOR ALL TO authenticated
USING (user_id = (SELECT auth.uid())) WITH CHECK (user_id = (SELECT auth.uid()));

GRANT USAGE ON SCHEMA public TO authenticated;
GRANT SELECT ON public.profiles TO authenticated;
GRANT INSERT (id, nom, phone, matricule, profile) ON public.profiles TO authenticated;
GRANT UPDATE (nom, phone, matricule, profile) ON public.profiles TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON
    public.tontines, public.members, public.caisses, public.caisse_participants,
    public.cycles, public.tours, public.transactions, public.prets, public.amendes,
    public.sorties_caisse, public.invite_links, public.join_requests, public.comments
TO authenticated;
GRANT SELECT, UPDATE (is_read) ON public.notifications TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.user_devices TO authenticated;

-- Comments, JoinRequests and Notifications are consumed as live user-facing state.
ALTER TABLE public.join_requests REPLICA IDENTITY FULL;
ALTER TABLE public.notifications REPLICA IDENTITY FULL;
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_publication WHERE pubname = 'supabase_realtime') THEN
        IF NOT EXISTS (
            SELECT 1 FROM pg_publication_tables
            WHERE pubname = 'supabase_realtime'
              AND schemaname = 'public' AND tablename = 'join_requests'
        ) THEN
            ALTER PUBLICATION supabase_realtime ADD TABLE public.join_requests;
        END IF;
        IF NOT EXISTS (
            SELECT 1 FROM pg_publication_tables
            WHERE pubname = 'supabase_realtime'
              AND schemaname = 'public' AND tablename = 'notifications'
        ) THEN
            ALTER PUBLICATION supabase_realtime ADD TABLE public.notifications;
        END IF;
        IF NOT EXISTS (
            SELECT 1 FROM pg_publication_tables
            WHERE pubname = 'supabase_realtime'
            AND schemaname = 'public' AND tablename = 'comments'
        ) THEN
            ALTER PUBLICATION supabase_realtime ADD TABLE public.comments;
        END IF;
    END IF;
END;
$$;

INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES ('comment-images', 'comment-images', false, 5242880, ARRAY['image/*'])
ON CONFLICT (id) DO UPDATE
SET public = false,
    file_size_limit = EXCLUDED.file_size_limit,
    allowed_mime_types = EXCLUDED.allowed_mime_types;

CREATE OR REPLACE FUNCTION public.comment_image_tontine_id(object_name text)
RETURNS uuid
LANGUAGE sql
IMMUTABLE
SET search_path = pg_catalog, storage
AS $$
    SELECT CASE
        WHEN (storage.foldername(object_name))[1] = 'tontines'
         AND (storage.foldername(object_name))[2] ~*
             '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'
        THEN (storage.foldername(object_name))[2]::uuid
        ELSE NULL
    END;
$$;

REVOKE ALL ON FUNCTION public.comment_image_tontine_id(text) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.comment_image_tontine_id(text) TO authenticated;

-- Supabase Storage already enables RLS on storage.objects. The hosted
-- dashboard role is not the table owner, so do not ALTER this managed table.
CREATE POLICY comment_images_membership_gate ON storage.objects AS RESTRICTIVE FOR ALL TO authenticated
USING (
    bucket_id <> 'comment-images'
    OR public.is_tontine_member(public.comment_image_tontine_id(name))
)
WITH CHECK (
    bucket_id <> 'comment-images'
    OR (
        public.is_tontine_member(public.comment_image_tontine_id(name))
        AND COALESCE((storage.foldername(name))[3], '') ~*
            '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'
        AND (
            (owner_id = (SELECT auth.uid())::text
             AND COALESCE((storage.foldername(name))[3], '') = (SELECT auth.uid())::text)
            OR public.is_tontine_admin(public.comment_image_tontine_id(name))
        )
        AND COALESCE(metadata ->> 'mimetype', metadata ->> 'contentType', '') LIKE 'image/%'
    )
);
CREATE POLICY comment_images_read_group ON storage.objects FOR SELECT TO authenticated
USING (
    bucket_id = 'comment-images'
    AND public.comment_image_tontine_id(name) IS NOT NULL
    AND public.is_tontine_member(public.comment_image_tontine_id(name))
    AND COALESCE(metadata ->> 'mimetype', metadata ->> 'contentType', '') LIKE 'image/%'
);
CREATE POLICY comment_images_upload_group_owner ON storage.objects FOR INSERT TO authenticated
WITH CHECK (
    bucket_id = 'comment-images'
    AND owner_id = (SELECT auth.uid())::text
    AND COALESCE((storage.foldername(name))[3], '') = (SELECT auth.uid())::text
    AND public.comment_image_tontine_id(name) IS NOT NULL
    AND public.is_tontine_member(public.comment_image_tontine_id(name))
    AND COALESCE(metadata ->> 'mimetype', metadata ->> 'contentType', '') LIKE 'image/%'
);
CREATE POLICY comment_images_update_owner_or_admin ON storage.objects FOR UPDATE TO authenticated
USING (
    bucket_id = 'comment-images'
    AND (
        (owner_id = (SELECT auth.uid())::text
         AND COALESCE((storage.foldername(name))[3], '') = (SELECT auth.uid())::text)
        OR public.is_tontine_admin(public.comment_image_tontine_id(name))
    )
    AND public.is_tontine_member(public.comment_image_tontine_id(name))
    AND COALESCE((storage.foldername(name))[3], '') ~*
        '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'
)
WITH CHECK (
    bucket_id = 'comment-images'
    AND (
        (owner_id = (SELECT auth.uid())::text
         AND COALESCE((storage.foldername(name))[3], '') = (SELECT auth.uid())::text)
        OR public.is_tontine_admin(public.comment_image_tontine_id(name))
    )
    AND public.is_tontine_member(public.comment_image_tontine_id(name))
    AND COALESCE((storage.foldername(name))[3], '') ~*
        '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'
    AND COALESCE(metadata ->> 'mimetype', metadata ->> 'contentType', '') LIKE 'image/%'
);
CREATE POLICY comment_images_delete_owner_or_admin ON storage.objects FOR DELETE TO authenticated
USING (
    bucket_id = 'comment-images'
    AND (
        owner_id = (SELECT auth.uid())::text
        OR public.is_tontine_admin(public.comment_image_tontine_id(name))
    )
    AND public.is_tontine_member(public.comment_image_tontine_id(name))
    AND COALESCE(metadata ->> 'mimetype', metadata ->> 'contentType', '') LIKE 'image/%'
);

COMMENT ON POLICY comment_images_upload_group_owner ON storage.objects IS
'Object path format: tontines/{tontine_uuid}/{uploader_auth_user_uuid}/{filename}; files must be images and uploader must be a group member.';

NOTIFY pgrst, 'reload schema';
