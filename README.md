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
- **`TontineDetailScreen.kt`**: The group's landing page, displaying the list of active funds and providing "Quick Actions" for treasurers.
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
