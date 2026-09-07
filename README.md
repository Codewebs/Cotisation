# Cotisation

**Cotisation** is a high-end Android application designed for the automated and transparent management of tontines (community savings groups). It replaces manual bookkeeping with a secure, real-time financial engine, fostering trust and financial inclusion within associations and circles of friends.

## 🌟 Core Value Proposition
Managing a tontine manually is error-prone and often leads to disputes. **Cotisation** centralizes the entire lifecycle: from membership onboarding and fund configuration to complex interest-bearing loans and automated payout schedules, all wrapped in a premium "Afro-modern" UI.

## 🚀 Key Features
- **Multi-Fund Ecosystem (Caisses)**: Supports various fund types including Simple Savings, Redistribution (Lottery), and Relief Funds.
- **Dynamic Cycle Management**: Automated transition between cycles with precise tracking of "tours de table" (payout turns).
- **Advanced Financial Engine**:
    - **Balance Rollover**: Automatically calculates available liquidity by merging current cycle contributions with unallocated funds from previous cycles.
    - **Loan Management**: Integrated credit system with support for Simple/Compound interest and grace periods.
    - **Penalty System**: Configurable fines for late payments or failed contributions.
- **Fair Redistribution**: Integrated shuffle module for unbiased beneficiary scheduling.
- **Offline-First Architecture**: Robust local persistence with background synchronization ensuring data integrity in low-connectivity areas.

## 🛠 Technical Stack
- **Language**: Kotlin 1.9+
- **UI Framework**: Jetpack Compose (Material 3)
- **Architecture**: MVVM with Clean Architecture principles.
- **DI**: Hilt (Dagger)
- **Persistence**: Room Database (SQLite)
- **Networking**: Retrofit 2 & OkHttp
- **Concurrency**: Kotlin Coroutines & Flow for reactive UI updates.

---

## 📂 Exhaustive Project Documentation

### 🏦 `bank` (Financial Operations & Logic)
This package is the "Accounting Department" of the application, handling all ledger movements and financial business rules.

- **`TransactionEntity.kt`**: Defines the `Transaction` schema. Each record tracks the `caisseId`, `memberId`, and `cycleId`, storing amounts as signed Longs (Positive for deposits/fines, Negative for payouts/loans).
- **`TransactionDao.kt`**: Provides SQL queries for complex financial reports, such as calculating the total debt of a member or the "rollover" balance from all previous cycles.
- **`CycleEntity.kt` / `CycleDao.kt`**: Manages the temporal boundaries of the tontine. It tracks which cycle is active and handles the archival of completed cycles.
- **`Tour.kt` / `TourDao.kt`**: Manages the "Payout Events" within a cycle. It records who received the pot, how many "shares" (parts) they encashed, and links these to the financial ledger.
- **`PretEntity.kt`**: Stores loan data, including the principal, the applied interest rate, and the `moisDiffere` (grace period).
- **`Pretutils.kt`**: The financial logic unit. It implements the math for **Simple vs. Compound Interest** and calculates the exact `montantDu` (amount owed) based on elapsed time and grace periods.
- **`TransactionViewModel.kt`**: Orchestrates high-level business actions like "Validating a payout" or "Enrolling a loan", ensuring that every action results in an atomic database transaction.
- **`BulkValidationScreen.kt`**: A productivity tool for treasurers to mark multiple members as "Paid" in a single interaction.
- **`JournalTransactionsScreen.kt`**: A read-only transparency ledger where members can audit every movement of money within their tontine.

### 📦 `caisses` (Fund Governance)
This package defines the rules and participation models for different savings pools.

- **`CaisseEntity.kt`**: The configuration blueprint for a fund. It stores the `montantFixe` (contribution amount), frequency, and penalty rules.
- **`CaisseType.kt`**: An Enum defining the fund's nature: `COTISATION_SIMPLE` (standard), `REDISTRIBUTION` (randomized payout), or `SECOURS` (emergency/relief fund).
- **`CaisseConfigScreen.kt`**: The administrative UI for creating or editing fund rules, including interest rates for internal loans.
- **`CaisseDetailScreen.kt`**: The command center for a fund. It manages cycles, tours, and participant statuses. It includes features like shuffle beneficiary draw, cycle closure, and real-time status indicators (e.g., checkmarks for members who received their payout).
- **`CaisseDetailViewModel.kt`**: A reactive engine that combines data from `TransactionDao`, `CycleDao`, and `TourDao` to provide a real-time summary of a fund's health.
- **`CaisseHeader.kt`**: A sophisticated Composable that visualizes the fund's performance metrics: "Members Up-to-Date", "Target vs. Actual Balance", and "Next Payout Beneficiary".
- **`ManageParticipantsScreen.kt`**: Handles the mapping of members to specific funds, allowing for "Multi-part" participation (e.g., a member contributing for two shares).

### 🔄 `sync` (Data Integrity & Connectivity)
The synchronization engine ensures the app works flawlessly in various network conditions.

- **`AppDatabase.kt`**: The Room Database definition, managing migrations and providing access to all DAOs.
- **`SyncQueueEntity.kt`**: A persistent "Outbox" that captures all local changes. Each entry records the `SyncAction` (Create, Update, Delete) and the entity involved.
- **`SyncEngine.kt`**: The core reconciler. It reads the `SyncQueue`, converts Room entities into API DTOs, and pushes them to the server, handling retries and conflict resolution.
- **`SyncWorker.kt`**: A `CoroutineWorker` integrated with Android's WorkManager to trigger sync tasks in the background even if the app is closed.
- **`NetworkObserver.kt`**: Uses `ConnectivityManager` to monitor real-time internet status, alerting the `SyncEngine` to resume operations as soon as the device is online.

### 🤝 `tontine` (Organizational Core)
Manages the top-level entity: the Tontine Group itself.

- **`TontineEntity.kt`**: Stores metadata about the group (Name, Creation Date, Admin ID).
- **`TontineRepository.kt`**: The single source of truth for the UI, orchestrating data between the `RemoteApi` and the local `TontineDao`.
- **`TontineDetailScreen.kt`**: The group's landing page, displaying the list of active funds, managing group members, and providing "Quick Actions" for treasurers. It acts as the primary hub for navigating between different fund types.
- **`TontineListViewModel.kt`**: Manages the user's portfolio of tontines, handling the initial loading and search/filtering logic.

### 👥 `tontinemembers` (Membership & Individual Standing)
Manages the relationship between users and groups.

- **`TontineMemberEntity.kt`**: A junction table linking users to tontines, storing their `role` (Admin, Treasurer, Member).
- **`MemberSituationScreen.kt`**: The "Personal Financial Advisor" for the user. It calculates their net position in the tontine: `Total Paid - Payouts Received - Outstanding Debts`.
- **`AddMemberDialog.kt`**: Interface for adding new members via their phone number or matricule.

### 🔑 `users` (Identity & Access Control)
Handles security and session lifecycle.

- **`UserEntity.kt` / `SessionEntity.kt`**: Defines the user profile and the active session (JWT tokens, etc.).
- **`AuthViewModel.kt`**: Manages the state machine for login/registration (Loading, Success, Error).
- **`Permission.kt`**: A logic layer that checks roles before showing administrative UIs, ensuring that only authorized users can modify financial records.
- **`MatriculeGenerator.kt`**: A utility to generate short, unique alphanumeric codes used to invite new members to a tontine securely.

### 🎨 `tools` & `ui.theme`
- **`AfroTheme.kt`**: Custom Material 3 implementation using the project's signature "Dark & Neon" color palette.
- **`DesignSystem.kt`**: Defines reusable UI tokens like `AfroSunsetGradient` and standard `RoundedCornerShape` values.
- **`DateConverter.kt`**: Crucial utility for converting `Long` timestamps to human-readable formats according to the tontine's frequency.

---
*Cotisation - Technology empowering community savings.*

---

# Cotisation (Version Française)

**Cotisation** est une application Android haut de gamme conçue pour la gestion automatisée et transparente des tontines (groupes d'épargne communautaire). Elle remplace la tenue de livres manuelle par un moteur financier sécurisé en temps réel, renforçant la confiance et l'inclusion financière au sein des associations et des cercles d'amis.

## 🌟 Proposition de Valeur Fondamentale
La gestion manuelle d'une tontine est source d'erreurs et conduit souvent à des litiges. **Cotisation** centralise tout le cycle de vie : de l'adhésion des membres et la configuration des fonds aux prêts complexes avec intérêts et aux calendriers de versement automatisés, le tout enveloppé dans une interface utilisateur "Afro-moderne" premium.

## 🚀 Fonctionnalités Clés
- **Écosystème Multi-Fonds (Caisses)** : Supporte divers types de fonds, notamment l'Épargne Simple, la Redistribution (Loterie) et les Fonds de Secours.
- **Gestion Dynamique des Cycles** : Transition automatisée entre les cycles avec un suivi précis des "tours de table" (moments de versement).
- **Moteur Financier Avancé** :
    - **Report de Solde** : Calcule automatiquement la liquidité disponible en fusionnant les cotisations du cycle actuel avec les fonds non alloués des cycles précédents.
    - **Gestion des Prêts** : Système de crédit intégré supportant les intérêts simples/composés et les périodes de différé.
    - **Système de Pénalités** : Amendes configurables pour les retards de paiement ou les cotisations manquées.
- **Redistribution Équitable** : Module de tirage au sort intégré pour une planification impartiale des bénéficiaires.
- **Architecture Offline-First** : Persistance locale robuste avec synchronisation en arrière-plan garantissant l'intégrité des données dans les zones à faible connectivité.

## 🛠 Stack Technique
- **Langage** : Kotlin 1.9+
- **Framework UI** : Jetpack Compose (Material 3)
- **Architecture** : MVVM avec les principes de la Clean Architecture.
- **DI** : Hilt (Dagger)
- **Persistance** : Room Database (SQLite)
- **Réseau** : Retrofit 2 & OkHttp
- **Concurrence** : Kotlin Coroutines & Flow pour les mises à jour réactives de l'UI.

---

## 📂 Documentation Exhaustive du Projet

### 🏦 `bank` (Opérations Financières & Logique)
Ce package est le "Département Comptabilité" de l'application, gérant tous les mouvements du grand livre et les règles de gestion financière.

- **`TransactionEntity.kt`** : Définit le schéma des `Transactions`. Chaque enregistrement suit le `caisseId`, `memberId` et `cycleId`, stockant les montants sous forme de Longs signés (Positif pour les dépôts/amendes, Négatif pour les versements/prêts).
- **`TransactionDao.kt`** : Fournit des requêtes SQL pour des rapports financiers complexes, comme le calcul de la dette totale d'un membre ou le solde reporté de tous les cycles précédents.
- **`CycleEntity.kt` / `CycleDao.kt`** : Gère les limites temporelles de la tontine. Suit quel cycle est actif et gère l'archivage des cycles terminés.
- **`Tour.kt` / `TourDao.kt`** : Gère les "Événements de Versement" au sein d'un cycle. Enregistre qui a reçu la cagnotte, combien de parts ils ont encaissées, et lie ces informations au grand livre financier.
- **`PretEntity.kt`** : Stocke les données de prêt, y compris le principal, le taux d'intérêt appliqué et le `moisDiffere` (période de grâce).
- **`Pretutils.kt`** : L'unité de logique financière. Implémente les calculs pour les **Intérêts Simples vs Composés** et calcule le `montantDu` exact en fonction du temps écoulé et des périodes de grâce.
- **`TransactionViewModel.kt`** : Orchestre les actions métier de haut niveau comme la "Validation d'un versement" ou l' "Octroi d'un prêt", garantissant que chaque action aboutit à une transaction atomique dans la base de données.
- **`BulkValidationScreen.kt`** : Un outil de productivité pour les trésoriers permettant de marquer plusieurs membres comme "Payé" en une seule interaction.
- **`JournalTransactionsScreen.kt`** : Un registre de transparence en lecture seule où les membres peuvent auditer chaque mouvement d'argent au sein de leur tontine.

### 📦 `caisses` (Gouvernance des Fonds)
Ce package définit les règles et les modèles de participation pour les différents pools d'épargne.

- **`CaisseEntity.kt`** : Le plan de configuration d'un fonds. Stocke le `montantFixe` (montant de la cotisation), la fréquence et les règles de pénalité.
- **`CaisseType.kt`** : Une énumération définissant la nature du fonds : `COTISATION_SIMPLE` (standard), `REDISTRIBUTION` (versement aléatoire) ou `SECOURS` (fonds d'urgence/aide).
- **`CaisseConfigScreen.kt`** : L'interface administrative pour créer ou modifier les règles du fonds, y compris les taux d'intérêt pour les prêts internes.
- **`CaisseDetailScreen.kt`** : Le centre de commande d'un fonds. Il gère les cycles, les tours et le statut des participants. Il inclut des fonctionnalités telles que le tirage au sort des bénéficiaires, la clôture des cycles et des indicateurs de statut en temps réel (ex: coches pour les membres ayant reçu leur versement).
- **`CaisseDetailViewModel.kt`** : Un moteur réactif qui combine les données de `TransactionDao`, `CycleDao` et `TourDao` pour fournir un résumé en temps réel de la santé d'un fonds.
- **`CaisseHeader.kt`** : Un composant visuel sophistiqué qui affiche les indicateurs de performance du fonds : "Membres à jour", "Solde Cible vs Réel" et "Prochain Bénéficiaire".
- **`ManageParticipantsScreen.kt`** : Gère l'affectation des membres à des fonds spécifiques, permettant une participation à "Parts Multiples" (ex: un membre cotisant pour deux parts).

### 🔄 `sync` (Intégrité des Données & Connectivité)
Le moteur de synchronisation garantit que l'application fonctionne parfaitement dans diverses conditions de réseau.

- **`AppDatabase.kt`** : La définition de la base de données Room, gérant les migrations et fournissant l'accès à tous les DAOs.
- **`SyncQueueEntity.kt`** : Une "Boîte d'envoi" persistante qui capture tous les changements locaux. Chaque entrée enregistre le `SyncAction` (Création, Mise à jour, Suppression) et l'entité concernée.
- **`SyncEngine.kt`** : Le réconciliateur central. Lit la `SyncQueue`, convertit les entités Room en DTOs API, et les pousse vers le serveur, gérant les tentatives et la résolution de conflits.
- **`SyncWorker.kt`** : Un `CoroutineWorker` intégré au WorkManager d'Android pour déclencher les tâches de synchronisation en arrière-plan, même si l'app est fermée.
- **`NetworkObserver.kt`** : Utilise le `ConnectivityManager` pour surveiller l'état du réseau en temps réel, alertant le `SyncEngine` pour reprendre les opérations dès que l'appareil est en ligne.

### 🤝 `tontine` (Cœur Organisationnel)
Gère l'entité de haut niveau : le Groupe de Tontine lui-même.

- **`TontineEntity.kt`** : Stocke les métadonnées sur le groupe (Nom, Date de création, ID de l'administrateur).
- **`TontineRepository.kt`** : La source unique de vérité pour l'interface utilisateur, orchestrant les données entre l'API distante et le DAO local.
- **`TontineDetailScreen.kt`** : La page d'accueil du groupe, affichant la liste des fonds actifs, gérant les membres du groupe et fournissant des "Actions Rapides" pour les trésoriers. Il sert de hub principal pour naviguer entre les différents types de fonds.
- **`TontineListViewModel.kt`** : Gère le portefeuille de tontines de l'utilisateur, s'occupant du chargement initial et de la logique de recherche/filtrage.

### 👥 `tontinemembers` (Adhésion & Situation Individuelle)
Gère la relation entre les utilisateurs et les groupes.

- **`TontineMemberEntity.kt`** : Une table de jonction liant les utilisateurs aux tontines, stockant leur `role` (Admin, Trésorier, Membre).
- **`MemberSituationScreen.kt`** : Le "Conseiller Financier Personnel" pour l'utilisateur. Calcule sa position nette dans la tontine : `Total Payé - Versements Reçus - Dettes en cours`.
- **`AddMemberDialog.kt`** : Interface pour ajouter de nouveaux membres via leur numéro de téléphone ou leur matricule.

### 🔑 `users` (Identité & Contrôle d'Accès)
Gère la sécurité et le cycle de vie des sessions.

- **`UserEntity.kt` / `SessionEntity.kt`** : Définit le profil utilisateur et la session active (tokens JWT, etc.).
- **`AuthViewModel.kt`** : Gère la machine à états pour la connexion/inscription (Chargement, Succès, Erreur).
- **`Permission.kt`** : Une couche logique qui vérifie les rôles avant d'afficher les interfaces administratives, garantissant que seuls les utilisateurs autorisés peuvent modifier les enregistrements financiers.
- **`MatriculeGenerator.kt`** : Un utilitaire pour générer des codes alphanumériques courts et uniques utilisés pour inviter de nouveaux membres à une tontine en toute sécurité.

### 🎨 `tools` & `ui.theme`
- **`AfroTheme.kt`** : Implémentation personnalisée de Material 3 utilisant la palette de couleurs "Dark & Neon" signature du projet.
- **`DesignSystem.kt`** : Définit des jetons UI réutilisables comme `AfroSunsetGradient` et des valeurs standard de `RoundedCornerShape`.
- **`DateConverter.kt`** : Utilitaire crucial pour convertir les horodatages `Long` en formats lisibles par l'homme selon la fréquence de la tontine.

---
*Cotisation - La technologie au service de l'épargne communautaire.*
