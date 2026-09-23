# Propositions d'Amélioration - Cotisation App

Ce document présente des axes stratégiques pour renforcer la stabilité, la sécurité et les fonctionnalités de l'écosystème **Cotisation**.

## 1. Stabilité & Résilience (Architecture Sync)

> [!IMPORTANT]
> **Problème actuel** : La synchronisation est partielle (Tontines et Caisses uniquement). Les transactions et prêts sont envoyés en "temps réel" sans file d'attente robuste en cas de coupure réseau.

- **Moteur de Synchro Unifié (Offline-First)** :
    - Rendre toutes les entités (`Transaction`, `Pret`, `Cycle`, `Tour`) synchronisables via une interface `Syncable`.
    - Utiliser la `SyncQueue` pour TOUTES les opérations financières. Si une cotisation est faite en brousse sans réseau, elle doit être envoyée automatiquement dès que la 4G revient.
- **Gestion des Conflits** : Implémenter une stratégie "Last Write Wins" basée sur un horodatage `updatedAt` serveur pour éviter les écrasements de données entre deux trésoriers.

## 2. Automatisation & Intelligence Métier

- **Calcul Automatique des Pénalités** :
    - Au lieu de laisser le trésorier décider, le système doit générer automatiquement une transaction `PENALITE_RETARD` si `dateActuelle > dateLimiteJour` de la caisse.
- **Planificateur de Cycles** :
    - Automatiser l'ouverture du cycle suivant dès la clôture du précédent pour éviter les temps morts.
- **Moteur d'Intérêts Avancé** :
    - Finaliser le calcul des intérêts **Composés** (Actuellement, seul le Simple est implémenté).

## 3. Sécurité & Authentification

- **Biométrie (Empreinte/Visage)** :
    - Remplacer ou compléter le code PIN par `androidx.biometric` pour un accès plus rapide et sécurisé.
- **Signatures Numériques des Reçus** :
    - Chaque transaction importante (Prêt, Remise de cagnotte) devrait générer un hash unique (Preuve d'intégrité) stocké sur la blockchain ou un serveur sécurisé.

## 4. Expérience Utilisateur (UI/UX)

- **Notifications Push (FCM)** :
    - "C'est votre tour !" : Notifier le membre lorsqu'il est le bénéficiaire du cycle actuel.
    - "Alerte Retard" : Rappel automatique 24h avant la date limite de paiement.
- **Visualisation de Données** :
    - Ajouter des graphiques de performance individuelle pour chaque membre (Assiduité, Épargne cumulée).
- **Mode Sombre Adaptatif** : Finaliser le support complet du thème sombre sur tous les écrans.

## 5. Maintenance & Évolutivité

- **Migrations de Base de Données** :
    - Passer à des scripts de migration Room explicites plutôt que des réinitialisations de schéma, pour ne jamais perdre les données des utilisateurs lors d'une mise à jour.
- **Monitoring Serveur** :
    - Intégrer un outil de log (ex: Winston/Morgan) et des alertes sur le serveur Node.js pour détecter les erreurs `HTTP 400/500` en temps réel.

---

### Quelle priorité souhaitez-vous donner à ces propositions ?
Je suggère de commencer par la **Robustesse de la Synchro (1)** pour garantir que l'argent des membres est toujours tracé, même avec une mauvaise connexion.
