package com.indiza.cotisation.users

enum class Permission {
    // Tontine & Configuration
    EDIT_TONTINE_RULES,
    DELETE_TONTINE,
    CLOSE_EXERCISE,      // Clôturer l'année/exercice en cours

    // Sessions (Réunions)
    CREATE_SESSION,      // Ouvrir une nouvelle séance
    CANCEL_SESSION,      // Annuler ou reporter
    VALIDATE_PRESENCE,   // Marquer les présences

    // Finances
    COLLECT_MONEY,      // Enregistrer une réception d'argent (cotisations)
    DISBURSE_MONEY,     // Donner l'argent au bénéficiaire (bouffe/prêt)
    VIEW_ALL_FINANCES,
    MANAGE_FINES,       // Infliger ou annuler des amendes

    // Membres
    ADD_MEMBER,
    REMOVE_MEMBER,      // Exclure un membre
    APPROVE_MEMBER,
    MANAGE_ROLES,       // Changer le rôle d'un membre (ex: Membre -> Trésorier)

    // Audit & Historique
    AUDIT_ACCOUNTS,
    VIEW_OWN_HISTORY,
    GENERATE_REPORTS,    // Générer des PDF/Bilans de fin de séance

    // Confidentialité
    VIEW_MEMBER_NAMES,    // Droit de voir les noms réels (Admin/Secrétaire)
    VIEW_MEMBER_PHONES,   // Droit de voir les numéros (Très restreint)
    MANAGE_PRIVACY        // Modifier le mode anonyme de la tontine
}

enum class Role(val permissions: Set<Permission>, val displayName: String, val isUnique: Boolean = true) {
    PRESIDENT(Permission.values().toSet(), "Président.e"),
    VICE_PRESIDENT(Permission.values().toSet(), "Vice-Président.e"),
    INITIATEUR(Permission.values().toSet(), "Initiateur.rice"),
    SECRETAIRE(setOf(
        Permission.EDIT_TONTINE_RULES,
        Permission.ADD_MEMBER,
        Permission.REMOVE_MEMBER,
        Permission.APPROVE_MEMBER,
        Permission.CREATE_SESSION,
        Permission.VALIDATE_PRESENCE,
        Permission.MANAGE_ROLES,
        Permission.VIEW_ALL_FINANCES,
        Permission.VIEW_OWN_HISTORY,
        Permission.GENERATE_REPORTS,
        Permission.VIEW_MEMBER_NAMES,
    ), "Secrétaire"),
    SECRETAIRE_ADJOINT_A1(setOf(
        Permission.CREATE_SESSION,
        Permission.VALIDATE_PRESENCE,
        Permission.VIEW_OWN_HISTORY,
        Permission.VIEW_MEMBER_NAMES,
    ), "Secrétaire Adjoint.e (A1)"),
    SECRETAIRE_ADJOINT_A2(setOf(
        Permission.CREATE_SESSION,
        Permission.VALIDATE_PRESENCE,
        Permission.VIEW_OWN_HISTORY,
        Permission.VIEW_MEMBER_NAMES,
    ), "Secrétaire Adjoint.e (A2)"),
    TRESORIER(setOf(
        Permission.COLLECT_MONEY,
        Permission.DISBURSE_MONEY,
        Permission.MANAGE_FINES,
        Permission.VIEW_ALL_FINANCES,
        Permission.VIEW_OWN_HISTORY,
        Permission.GENERATE_REPORTS,
        Permission.VIEW_MEMBER_NAMES,
    ), "Trésorier.e (T1)"),
    TRESORIER_ADJOINT_TA2(setOf(
        Permission.COLLECT_MONEY,
        Permission.VIEW_ALL_FINANCES,
        Permission.VIEW_OWN_HISTORY,
        Permission.VIEW_MEMBER_NAMES,
    ), "Trésorier.e Adjoint.e (TA2)"),
    COMMISSAIRE_AUX_COMPTES(setOf(
        Permission.AUDIT_ACCOUNTS,
        Permission.VIEW_ALL_FINANCES,
        Permission.VIEW_OWN_HISTORY,
        Permission.GENERATE_REPORTS
    ), "Commissaire aux Comptes"),
    MEMBRE(setOf(
        Permission.VIEW_OWN_HISTORY
    ), "Membre", isUnique = false)
}