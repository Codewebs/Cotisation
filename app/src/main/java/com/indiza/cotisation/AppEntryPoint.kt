package com.indiza.cotisation

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.indiza.cotisation.bank.BulkValidationScreen
import com.indiza.cotisation.bank.CagnotteValidationScreen
import com.indiza.cotisation.bank.CreateTourScreen
import com.indiza.cotisation.bank.JournalTransactionsScreen
import com.indiza.cotisation.bank.LoanManagementScreen
import com.indiza.cotisation.bank.MemberSelectionScreen
import com.indiza.cotisation.bank.PretSaisieScreen
import com.indiza.cotisation.bank.RemboursementScreen
import com.indiza.cotisation.bank.SortieSecoursScreen
import com.indiza.cotisation.bank.TransactionScreen
import com.indiza.cotisation.bank.TransactionType
import com.indiza.cotisation.bank.TransactionViewModel
import com.indiza.cotisation.caisses.CaisseDetailScreen
import com.indiza.cotisation.caisses.CaisseDetailViewModel
import com.indiza.cotisation.caisses.ManageParticipantsScreen
import com.indiza.cotisation.invitations.JoinInvitationScreen
import com.indiza.cotisation.invitations.JoinRequestsScreen
import com.indiza.cotisation.reports.AttendanceReportScreen
import com.indiza.cotisation.reports.CycleClosureReportScreen
import com.indiza.cotisation.reports.FinanceReportScreen
import com.indiza.cotisation.reports.LoanReportScreen
import com.indiza.cotisation.reports.MemberReportScreen
import com.indiza.cotisation.reports.PredictiveReportScreen
import com.indiza.cotisation.reports.RemboursementReportScreen
import com.indiza.cotisation.reports.ReportsMenuScreen
import com.indiza.cotisation.sync.ConnectivityObserver
import com.indiza.cotisation.tontine.CreateTontineScreen
import com.indiza.cotisation.tontine.EditTontineScreen
import com.indiza.cotisation.tontine.TontineDetailScreen
import com.indiza.cotisation.tontine.TontineEntity
import com.indiza.cotisation.tontine.TontineListScreen
import com.indiza.cotisation.tontinemembers.MemberActivityScreen
import com.indiza.cotisation.tontinemembers.MemberActivityViewModel
import com.indiza.cotisation.tools.CosmicBackground
import com.indiza.cotisation.users.AuthScreen
import com.indiza.cotisation.users.ProfileScreen
import com.indiza.cotisation.users.SearchTontineScreen
import com.indiza.cotisation.users.SessionViewModel
import kotlinx.coroutines.launch


@Composable
fun AppEntryPoint(connectivityObserver: ConnectivityObserver) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("cotisation_prefs", Context.MODE_PRIVATE) }
    val sessionViewModel: SessionViewModel = hiltViewModel()
    val hasSession by sessionViewModel.hasActiveSession
        .collectAsStateWithLifecycle(initialValue = false)

// Variable pour mémoriser une action après connexion
    var pendingJoinTontine by remember { mutableStateOf<TontineEntity?>(null) }

    // Déterminer l'écran de départ
    val startDestination = when {
        !prefs.getBoolean("guide_seen", false) -> Screen.Onboarding.route
        else -> Screen.SearchTontine.route
    }

    CosmicBackground {
        NavHost(
            navController = navController,
            startDestination = startDestination
        ) {
            composable(Screen.Auth.route) {
                AuthScreen(
                    onAuthSuccess = {
                        navController.popBackStack() // Retourne à l'écran précédent après connexion
                        // Si une action (pendingJoinTontine) la déclencher ici.,
                    },
                )
            }

            // --- GUIDE / ONBOARDING ---
            composable(Screen.Onboarding.route) {
                OnboardingScreen(onFinish = {
                    prefs.edit().putBoolean("guide_seen", true).apply()
                    navController.navigate(Screen.SearchTontine.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                })
            }

// Remplacez l'ancien composable remboursement par celui-ci
            composable(
                route = "remboursement/{caisseId}/{memberId}/{name}/{cycleId}", // Ajout de {cycleId}
                arguments = listOf(
                    navArgument("caisseId") { type = NavType.LongType },
                    navArgument("memberId") { type = NavType.LongType },
                    navArgument("name") { type = NavType.StringType },
                    navArgument("cycleId") { type = NavType.LongType }
                )
            ) { backStack ->
                val cid = backStack.arguments?.getLong("caisseId") ?: 0L
                val mid = backStack.arguments?.getLong("memberId") ?: 0L
                val name = backStack.arguments?.getString("name") ?: ""
                val cyid = backStack.arguments?.getLong("cycleId") ?: 0L

                RemboursementScreen(
                    caisseId = cid,
                    memberId = mid,
                    memberName = name,
                    cycleId = cyid, // N'oubliez pas de passer le cycleId à votre écran
                    onSuccess = { navController.popBackStack() }
                )
            }


            // --- RECHERCHE ---
            composable(Screen.SearchTontine.route) {
                SearchTontineScreen(
                    viewModel = hiltViewModel(),
                    sessionViewModel = sessionViewModel,
                    notifViewModel = hiltViewModel(),
                    adminViewModel = hiltViewModel(),
                    onNavigateToList = { navController.navigate(Screen.ListTontine.route) },
                    onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                    onCreateTontineWhenAuthenticated = { navController.navigate(Screen.CreateTontine.route) },
                    onBack = { TODO() },
                    onNavigateToAuth = {
                        navController.navigate(Screen.Auth.route)
                    },
                    onJoinTontine = { tontine ->
                        if (hasSession) {
                            joinTontine(tontine)
                        } else {
                            pendingJoinTontine = tontine
                            navController.navigate(Screen.Auth.route)
                        }
                    },
                    onNavigateToReceipt = { txId ->
                        navController.navigate("receipt/$txId")
                    },
                    onLogout = {
                        sessionViewModel.logout()
                    }
                )
            }

            // --- LISTE DES TONTINES ---
            composable(Screen.ListTontine.route) {
                TontineListScreen(
                    onReunionClick = { id ->
                        navController.navigate(Screen.TontineDetail.createRoute(id))
                    },
                    onAddClick = { navController.navigate(Screen.CreateTontine.route) },
                    viewModel = hiltViewModel(),
                    onEditClick = { id ->
                        navController.navigate(Screen.EditTontine.createRoute(id))
                    },
                    onProfileClick = {
                        navController.navigate(Screen.Profile.route)
                    }
                )
            }

            composable(
                route = Screen.TontineDetail.route,
                arguments = listOf(navArgument("tontineId") { type = NavType.LongType })
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getLong("tontineId") ?: 0L
                TontineDetailScreen(
                    tontineId = id,
                    navController = navController,
                    onBack = { navController.popBackStack() },
                    onCaisseClick = { caisseId, cycleId ->
                        navController.navigate(Screen.CaisseDetail.createRoute(caisseId, cycleId))
                    },
                    onLoanManagementClick = { tid ->
                        navController.navigate(Screen.LoanManagement.createRoute(tid))
                    },
                    onMemberClick = { tid, mid ->
                        navController.navigate("member_activity/$tid/$mid")
                    },
                    onEditTontine = { tid ->
                        navController.navigate(Screen.EditTontine.createRoute(tid))
                    }
                )
            }

            composable(
                route = Screen.LoanManagement.route,
                arguments = listOf(navArgument("tontineId") { type = NavType.LongType })
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getLong("tontineId") ?: 0L
                LoanManagementScreen(
                    tontineId = id,
                    onBack = { navController.popBackStack() },
                    navController = navController
                )
            }

            // --- DÉTAILS CAISSE ---
            composable(
                route = Screen.CaisseDetail.route,
                arguments = listOf(
                    navArgument("caisseId") { type = NavType.LongType },
                    navArgument("cycleId") { type = NavType.LongType }
                    ),

            ) { backStackEntry ->
                val cycleId = backStackEntry.arguments?.getLong("cycleId") ?: 0L
                val id = backStackEntry.arguments?.getLong("caisseId") ?: 0L
                CaisseDetailScreen(
                    caisseId = id,
                    cycleId = cycleId,
                    navController = navController,
                    onBack = { navController.popBackStack() },

                    )
            }

            composable(
                route = "cycle_report/{caisseId}/{cycleId}/{cycleNumber}",
                arguments = listOf(
                    navArgument("caisseId") { type = NavType.LongType },
                    navArgument("cycleId") { type = NavType.LongType },
                    navArgument("cycleNumber") { type = NavType.IntType }
                )
            ) { backStackEntry ->
                val caisseId = backStackEntry.arguments?.getLong("caisseId") ?: 0L
                val cycleId = backStackEntry.arguments?.getLong("cycleId") ?: 0L
                val cycleNumber = backStackEntry.arguments?.getInt("cycleNumber") ?: 1
                com.indiza.cotisation.caisses.CycleReportScreen(
                    caisseId = caisseId,
                    cycleId = cycleId,
                    cycleNumber = cycleNumber,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = "recapitulatif_masse/{caisseId}/{action}/{cycleId}",
                arguments = listOf(
                    navArgument("caisseId") { type = NavType.LongType },
                    navArgument("action") { type = NavType.StringType },
                    navArgument("cycleId") { type = NavType.LongType }
                )
            ) { backStack ->
                val cid = backStack.arguments?.getLong("caisseId") ?: 0L
                val actionStr = backStack.arguments?.getString("action") ?: ""
                val cycleId = backStack.arguments?.getLong("cycleId") ?: 0L
                val action = CaisseAction.valueOf(actionStr)

                // On récupère le ViewModel du parent (CaisseDetail) pour avoir accès à la liste des participants
                val parentEntry = remember(backStack) { navController.getBackStackEntry(Screen.CaisseDetail.createRoute(cid, cycleId)) }
                val detailViewModel: CaisseDetailViewModel = hiltViewModel(parentEntry)
                val transactionViewModel: TransactionViewModel = hiltViewModel()

                val state by detailViewModel.state.collectAsStateWithLifecycle()
                val participants = if (action == CaisseAction.TOUR_ENCAISSER) state.participantsPlanifies else state.participants

                // On filtre ceux qui n'ont pas encore cotisé pour proposer la liste de base
                val participantsNonTraites = participants.filter { p ->
                    if (action == CaisseAction.COTISATION) !state.dejaCotise.contains(p.memberWithUser.member.id)
                    else true
                }

                BulkValidationScreen(
                    caisse = state.caisse!!,
                    actionType = action,
                    participants = participantsNonTraites,
                    cycleId = cycleId,
                    viewModel = transactionViewModel,
                    onBack = { navController.popBackStack() },
                    onConfirm = {
                        navController.popBackStack()
                    }
                )
            }

            composable(
                route = "selection_membre/{caisseId}/{action}/{cycleId}",
                arguments = listOf(
                    navArgument("caisseId") { type = NavType.LongType },
                    navArgument("action") { type = NavType.StringType },
                    navArgument("cycleId") { type = NavType.LongType }
                )
            ) { backStack ->
                val cid = backStack.arguments?.getLong("caisseId") ?: 0L
                val actionStr = backStack.arguments?.getString("action") ?: ""
                val cycleId = backStack.arguments?.getLong("cycleId") ?: 0L
                val action = CaisseAction.valueOf(actionStr)

                MemberSelectionScreen(
                    caisseId = cid,
                    actionType = action,
                    onMemberSelected = { participant, cycId ->
                        val mid = participant.memberWithUser.member.id
                        when (action) {
                            CaisseAction.COTISATION -> navController.navigate("cotisation/$cid/$mid/$cycId")
                            CaisseAction.TOUR_ENCAISSER -> navController.navigate("attribuer_cagnotte/$cid/$mid/$cycId")
                            CaisseAction.PRET -> navController.navigate("pret/$cid/$mid/$cycId")
                            CaisseAction.REMBOURSEMENT -> navController.navigate("remboursement/$cid/$mid/${participant.memberWithUser.member.nomSaisi}/$cycId")
                            CaisseAction.SORTIE_SECOURS -> navController.navigate("sortie_secours/$cid/$mid/$cycId/SORTIE_SECOURS")
                            CaisseAction.SORTIE -> navController.navigate("sortie_secours/$cid/$mid/$cycId/SORTIE")
                            else -> {}
                        }
                    },
                    onBack = { navController.popBackStack() },
                    viewModel = hiltViewModel(),
                    navController = navController
                )
            }

            composable(
                route = "cotisation/{caisseId}/{memberId}/{cycleId}",
                arguments = listOf(
                    navArgument("caisseId") { type = NavType.LongType },
                    navArgument("memberId") { type = NavType.LongType },
                    navArgument("cycleId") { type = NavType.LongType }
                )
            ) { backStack ->
                val cycleId = backStack.arguments?.getLong("cycleId") ?: 0L
                val vm: TransactionViewModel = hiltViewModel()
                val uiState by vm.uiState.collectAsStateWithLifecycle()

                uiState?.let { (caisse, participant) ->
                    TransactionScreen(
                        caisse = caisse,
                        participant = participant,
                        onDismiss = { navController.popBackStack() },
                        cycleId = cycleId
                    )
                }
            }

// Dans votre NavHost
            composable(
                route = "attribuer_cagnotte/{caisseId}/{memberId}/{cycleId}",
                arguments = listOf(
                    navArgument("caisseId") { type = NavType.LongType },
                    navArgument("memberId") { type = NavType.LongType },
                    navArgument("cycleId") { type = NavType.LongType }
                )
            ) { backStack ->
                val cycleId = backStack.arguments?.getLong("cycleId") ?: 0L

                val vm: TransactionViewModel = hiltViewModel()
                val uiState by vm.uiState.collectAsStateWithLifecycle()

                uiState?.let { (caisse, participant) ->
                    CagnotteValidationScreen(
                        caisse = caisse,
                        participant = participant,
                        onDismiss = { navController.popBackStack() },
                        viewModel = vm,
                        cycleId = cycleId
                    )
                }
            }


            composable(
                route = Screen.ManageParticipants.route,
                arguments = listOf(navArgument("caisseId") { type = NavType.LongType })
            ) { backStack ->
                val id = backStack.arguments?.getLong("caisseId") ?: 0L
                ManageParticipantsScreen(
                    caisseId = id,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.CreateTour.route,
                arguments = listOf(
                    navArgument("caisseId") { type = NavType.LongType },
                    navArgument("cycleId") { type = NavType.LongType }
                )
            ) { backStackEntry ->
                val caisseId = backStackEntry.arguments?.getLong("caisseId") ?: 0L
                val cycleId = backStackEntry.arguments?.getLong("cycleId") ?: 0L

                // Ici, le plus simple est de laisser le ViewModel du CreateTourScreen
                // charger la caisse et les participants à partir de l'ID.
                // Si votre CreateTourScreen actuel demande les objets directement,
                // modifiez-le pour qu'il prenne les IDs et utilise son propre ViewModel.

                // Exemple si vous adaptez CreateTourScreen :
                CreateTourScreen(
                    caisseId = caisseId,
                    cycleId = cycleId,
                    onTourValidated = { navController.popBackStack() }
                )
            }

// --- NAVIGATION VERS PRÊT ---
            composable(
                route = "pret/{caisseId}/{memberId}/{cycleId}",
                arguments = listOf(
                    navArgument("caisseId") { type = NavType.LongType },
                    navArgument("memberId") { type = NavType.LongType },
                    navArgument("cycleId") { type = NavType.LongType }
                )
            ) { backStack ->
                val cycleId = backStack.arguments?.getLong("cycleId") ?: 0L
                val vm: TransactionViewModel = hiltViewModel()
                val uiState by vm.uiState.collectAsStateWithLifecycle()

                uiState?.let { (caisse, participant) ->
                    PretSaisieScreen(
                        caisse = caisse,
                        participant = participant,
                        onDismiss = { navController.popBackStack() },
                        viewModel = vm,
                        cycleId = cycleId
                    )
                }
            }

// --- NAVIGATION VERS SORTIE SECOURS ---
            composable(
                route = "sortie_secours/{caisseId}/{memberId}/{cycleId}/{type}",
                arguments = listOf(
                    navArgument("caisseId") { type = NavType.LongType },
                    navArgument("memberId") { type = NavType.LongType },
                    navArgument("cycleId") { type = NavType.LongType },
                    navArgument("type") { type = NavType.StringType }
                )
            ) { backStack ->
                val vm: TransactionViewModel = hiltViewModel()
                val uiState by vm.uiState.collectAsStateWithLifecycle()
                val cycleId = backStack.arguments?.getLong("cycleId") ?: 0L
                val typeStr = backStack.arguments?.getString("type") ?: "SORTIE_SECOURS"
                val type = try { TransactionType.valueOf(typeStr) } catch (e: Exception) { TransactionType.SORTIE_SECOURS }

                uiState?.let { (caisse, participant) ->
                    SortieSecoursScreen(
                        caisseId = caisse.id,
                        onDismiss = { navController.popBackStack() },
                        participant = participant,
                        viewModel = vm,
                        cycleId = cycleId,
                        transactionType = type
                    )
                }
            }
            // --- CRÉATION ---
            composable(Screen.CreateTontine.route) {
                CreateTontineScreen(
                    onCancel = { navController.popBackStack() },
                    onBack = { navController.popBackStack() },
                    onSuccess = {
                        navController.navigate(Screen.ListTontine.route) {
                            popUpTo(Screen.SearchTontine.route)
                        }
                    }
                )
            }

            composable(
                route = Screen.EditTontine.route,
                arguments = listOf(navArgument("tontineId") { type = NavType.LongType })
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getLong("tontineId") ?: 0L
                EditTontineScreen(
                    viewModel = hiltViewModel(),
                    tontineId = id,
                    onBack = { navController.popBackStack() },

                )
            }

            composable(
                route = Screen.TransactionJournal.route,
                arguments = listOf(
                    navArgument("caisseId") { type = NavType.LongType },
                    navArgument("cycleId") { type = NavType.LongType } )
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getLong("caisseId") ?: 0L
                val cycleId = backStackEntry.arguments?.getLong("cycleId") ?: 0L
                JournalTransactionsScreen(
                    caisseId = id,
                    viewModel = hiltViewModel(),
                    onBack = { navController.popBackStack() },
                )
            }

            composable(
                route = Screen.ReportsMenu.route,
                arguments = listOf(
                    navArgument("caisseId") { type = NavType.LongType },
                    navArgument("cycleId") { type = NavType.LongType }
                )
            ) { backStackEntry ->
                val caisseId = backStackEntry.arguments?.getLong("caisseId") ?: 0L
                val cycleId = backStackEntry.arguments?.getLong("cycleId") ?: 0L
                ReportsMenuScreen(
                    caisseId = caisseId,
                    cycleId = cycleId,
                    navController = navController,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.FinanceReport.route,
                arguments = listOf(
                    navArgument("caisseId") { type = NavType.LongType },
                    navArgument("cycleId") { type = NavType.LongType }
                )
            ) { backStackEntry ->
                val caisseId = backStackEntry.arguments?.getLong("caisseId") ?: 0L
                val cycleId = backStackEntry.arguments?.getLong("cycleId") ?: 0L
                FinanceReportScreen(
                    caisseId = caisseId,
                    cycleId = cycleId,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.AttendanceReport.route,
                arguments = listOf(
                    navArgument("caisseId") { type = NavType.LongType },
                    navArgument("cycleId") { type = NavType.LongType }
                )
            ) { backStackEntry ->
                val caisseId = backStackEntry.arguments?.getLong("caisseId") ?: 0L
                val cycleId = backStackEntry.arguments?.getLong("cycleId") ?: 0L
                AttendanceReportScreen(
                    caisseId = caisseId,
                    cycleId = cycleId,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.LoanReport.route,
                arguments = listOf(
                    navArgument("caisseId") { type = NavType.LongType },
                    navArgument("cycleId") { type = NavType.LongType }
                )
            ) { backStackEntry ->
                val caisseId = backStackEntry.arguments?.getLong("caisseId") ?: 0L
                val cycleId = backStackEntry.arguments?.getLong("cycleId") ?: 0L
                LoanReportScreen(
                    caisseId = caisseId,
                    cycleId = cycleId,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.MemberReport.route,
                arguments = listOf(
                    navArgument("caisseId") { type = NavType.LongType },
                    navArgument("cycleId") { type = NavType.LongType }
                )
            ) { backStackEntry ->
                val caisseId = backStackEntry.arguments?.getLong("caisseId") ?: 0L
                val cycleId = backStackEntry.arguments?.getLong("cycleId") ?: 0L
                MemberReportScreen(
                    caisseId = caisseId,
                    cycleId = cycleId,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.CycleClosureReport.route,
                arguments = listOf(
                    navArgument("caisseId") { type = NavType.LongType },
                    navArgument("cycleId") { type = NavType.LongType }
                )
            ) { backStackEntry ->
                val caisseId = backStackEntry.arguments?.getLong("caisseId") ?: 0L
                val cycleId = backStackEntry.arguments?.getLong("cycleId") ?: 0L
                CycleClosureReportScreen(
                    caisseId = caisseId,
                    cycleId = cycleId,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.PredictiveReport.route,
                arguments = listOf(
                    navArgument("caisseId") { type = NavType.LongType },
                    navArgument("cycleId") { type = NavType.LongType }
                )
            ) { backStackEntry ->
                val caisseId = backStackEntry.arguments?.getLong("caisseId") ?: 0L
                val cycleId = backStackEntry.arguments?.getLong("cycleId") ?: 0L
                PredictiveReportScreen(
                    caisseId = caisseId,
                    cycleId = cycleId,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.RemboursementReport.route,
                arguments = listOf(
                    navArgument("caisseId") { type = NavType.LongType },
                    navArgument("cycleId") { type = NavType.LongType }
                )
            ) { backStackEntry ->
                val caisseId = backStackEntry.arguments?.getLong("caisseId") ?: 0L
                val cycleId = backStackEntry.arguments?.getLong("cycleId") ?: 0L
                RemboursementReportScreen(
                    caisseId = caisseId,
                    cycleId = cycleId,
                    onBack = { navController.popBackStack() }
                )
            }

            // --- INVITATIONS & DEEP LINKING ---
            composable(
                route = Screen.JoinInvitation.route,
                arguments = listOf(navArgument("code") { type = NavType.StringType }),
                deepLinks = listOf(
                    androidx.navigation.navDeepLink { uriPattern = "cotisation://join?code={code}" },
                    androidx.navigation.navDeepLink { uriPattern = "http://10.0.2.2:3000/join?code={code}" },
                    androidx.navigation.navDeepLink { uriPattern = "http://192.168.1.50:3000/join?code={code}" }
                )
            ) { backStackEntry ->
                val code = backStackEntry.arguments?.getString("code") ?: ""
                JoinInvitationScreen(
                    inviteCode = code,
                    onBack = { navController.popBackStack() },
                    onNavigateToAuth = { navController.navigate(Screen.Auth.route) },
                    onSuccess = { navController.navigate(Screen.SearchTontine.route) }
                )
            }

            composable(
                route = "join_requests/{tontineId}",
                arguments = listOf(navArgument("tontineId") { type = NavType.LongType })
            ) { backStack ->
                val id = backStack.arguments?.getLong("tontineId") ?: 0L
                JoinRequestsScreen(
                    tontineId = id,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = "member_activity/{tontineId}/{memberId}",
                arguments = listOf(
                    navArgument("tontineId") { type = NavType.LongType },
                    navArgument("memberId") { type = NavType.LongType }
                )
            ) { backStack ->
                val tid = backStack.arguments?.getLong("tontineId") ?: 0L
                val mid = backStack.arguments?.getLong("memberId") ?: 0L
                MemberActivityScreen(
                    tontineId = tid,
                    memberId = mid,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = "receipt/{txId}",
                arguments = listOf(navArgument("txId") { type = NavType.StringType })
            ) { backStack ->
                val txId = backStack.arguments?.getString("txId") ?: ""
                com.indiza.cotisation.users.ReceiptScreen(
                    transactionId = txId,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = "meeting_chat/{tontineId}",
                arguments = listOf(navArgument("tontineId") { type = NavType.StringType })
            ) { backStack ->
                val tontineId = backStack.arguments?.getString("tontineId") ?: ""
                com.indiza.cotisation.tontine.MeetingChatScreen(
                    tontineId = tontineId,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Profile.route) {
                ProfileScreen(
                    onBack = { navController.popBackStack() },
                    onLogout = {
                        sessionViewModel.logout()
                        navController.navigate(Screen.Auth.route) {
                            popUpTo(0)
                        }
                    },
                    onTontineClick = { id ->
                        navController.navigate(Screen.TontineDetail.createRoute(id))
                    }
                )
            }
        }
    }
}
private fun joinTontine(tontine: TontineEntity) {
    // Implémentez une logique de demande d'adhésion ici
    // Par exemple : envoyer une requête API, mettre à jour la base locale, etc.
    println("Demande d'adhésion à: ${tontine.libelle}")
}

@OptIn(ExperimentalFoundationApi::class) // Utilisation du Pager natif
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    // Correction 1: Utiliser la lambda { 3 } pour le nombre de pages
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            when (page) {
                0 -> PageContent(title = "Bienvenue", description = "...")
                1 -> PageContent(title = "Statistiques", description = "...")
                2 -> PageContent(title = "Paramètres", description = "...")
            }
        }

        // Correction 2: Indicateur manuel
        Row(
            Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(3) { iteration ->
                val color = if (pagerState.currentPage == iteration) Color.DarkGray else Color.LightGray
                Box(
                    modifier = Modifier
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(color)
                        .size(8.dp)
                )
            }
        }

        Button(
            onClick = {
                if (pagerState.currentPage < 2) {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                } else {
                    onFinish()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(if (pagerState.currentPage < 2) "Suivant" else "Commencer")
        }
    }
}

@Composable
fun PageContent(title: String, description: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text(description, style = MaterialTheme.typography.bodyLarge)
    }
}

