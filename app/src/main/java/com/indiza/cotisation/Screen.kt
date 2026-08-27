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

}

enum class CaisseAction { COTISATION, PRET, REMBOURSEMENT, SORTIE_SECOURS, TOUR_ENCAISSER, SORTIE }