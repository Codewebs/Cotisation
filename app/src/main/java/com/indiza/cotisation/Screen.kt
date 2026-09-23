package com.indiza.cotisation

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Auth : Screen("auth")
    object SearchTontine : Screen("search_tontine")
    object EditTontine : Screen("edit_tontine/{tontineId}") {
        fun createRoute(id: Long) = "edit_tontine/$id"
    }
    object ListTontine : Screen("list_tontine")
    object CreateTontine : Screen("create_tontine")
    object TontineDetail : Screen("tontine_detail/{tontineId}") {
        fun createRoute(id: Long) = "tontine_detail/$id"
    }
    object CaisseDetail : Screen("caisse_detail/{caisseId}/{cycleId}") {
        fun createRoute(caisseId: Long, cycleId: Long) = "caisse_detail/$caisseId/$cycleId"
    }
    object TransactionJournal : Screen("journal_transactions/{caisseId}/{cycleId}") {
        fun createRoute(caisseId: Long, cycleId: Long) = "journal_transactions/$caisseId/$cycleId"
    }
    object CreateTour : Screen("creation_tour/{caisseId}/{cycleId}") {
        fun createRoute(caisseId: Long, cycleId: Long) = "creation_tour/$caisseId/$cycleId"
    }
    object ManageParticipants : Screen("manage_participants/{caisseId}") {
        fun createRoute(caisseId: Long) = "manage_participants/$caisseId"
    }
    object ReportsMenu : Screen("reports_menu/{caisseId}/{cycleId}") {
        fun createRoute(caisseId: Long, cycleId: Long) = "reports_menu/$caisseId/$cycleId"
    }
    object FinanceReport : Screen("finance_report/{caisseId}/{cycleId}") {
        fun createRoute(caisseId: Long, cycleId: Long) = "finance_report/$caisseId/$cycleId"
    }
    object AttendanceReport : Screen("attendance_report/{caisseId}/{cycleId}") {
        fun createRoute(caisseId: Long, cycleId: Long) = "attendance_report/$caisseId/$cycleId"
    }
    object LoanReport : Screen("loan_report/{caisseId}/{cycleId}") {
        fun createRoute(caisseId: Long, cycleId: Long) = "loan_report/$caisseId/$cycleId"
    }
    object MemberReport : Screen("member_report/{caisseId}/{cycleId}") {
        fun createRoute(caisseId: Long, cycleId: Long) = "member_report/$caisseId/$cycleId"
    }
    object CycleClosureReport : Screen("cycle_closure_report/{caisseId}/{cycleId}") {
        fun createRoute(caisseId: Long, cycleId: Long) = "cycle_closure_report/$caisseId/$cycleId"
    }
    object PredictiveReport : Screen("predictive_report/{caisseId}/{cycleId}") {
        fun createRoute(caisseId: Long, cycleId: Long) = "predictive_report/$caisseId/$cycleId"
    }
    object LoanManagement : Screen("loan_management/{tontineId}") {
        fun createRoute(tontineId: Long) = "loan_management/$tontineId"
    }
    object RemboursementReport : Screen("remboursement_report/{caisseId}/{cycleId}") {
        fun createRoute(caisseId: Long, cycleId: Long) = "remboursement_report/$caisseId/$cycleId"
    }
    object JoinInvitation : Screen("join_invitation?code={code}") {
        fun createRoute(code: String) = "join_invitation?code=$code"
    }
    object Profile : Screen("profile")
}

enum class CaisseAction { COTISATION, PRET, REMBOURSEMENT, SORTIE_SECOURS, TOUR_ENCAISSER, SORTIE, TRANSFERT_ENTREE, VOIR_TOURS }
