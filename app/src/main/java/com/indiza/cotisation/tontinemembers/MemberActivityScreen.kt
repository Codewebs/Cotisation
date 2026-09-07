package com.indiza.cotisation.tontinemembers

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.indiza.cotisation.tools.AfroGold
import com.indiza.cotisation.tools.CosmicBackground
import com.indiza.cotisation.tools.NeonCyan
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberActivityScreen(
    tontineId: Long,
    memberId: Long,
    viewModel: MemberActivityViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(tontineId, memberId) {
        viewModel.loadMemberActivity(tontineId, memberId)
    }

    CosmicBackground {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Activité du Membre", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = null)
                        }
                    },
                    actions = {
                        IconButton(onClick = { /* PDF */ }) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = "Rapport PDF", tint = NeonCyan)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    )
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                item {
                    state.member?.let { member ->
                        MemberActivityHeader(member.displayName, member.role.displayName, member.phoneNumber)
                    }
                }

                // Résumé Global
                item {
                    MemberGlobalSummary(
                        totalCotise = state.totalCotise,
                        detteTotale = state.totalDettes,
                        totalInterets = state.totalInteretsPayes
                    )
                }

                // Caisses Actives
                item {
                    SectionHeader("Caisses Actives", Icons.Default.AccountBalance)
                }
                items(state.activeCaisses) { caisseInfo ->
                    CaisseActivityCard(caisseInfo)
                }

                // Prêts
                item {
                    SectionHeader("Prêts & Remboursements", Icons.Default.Payments)
                }
                items(state.loans) { loan ->
                    LoanActivityCard(loan)
                }

                // Historique
                item {
                    SectionHeader("Historique des Transactions", Icons.Default.History)
                }
                items(state.recentTransactions) { tx ->
                    TransactionRow(tx)
                }

                item { Spacer(Modifier.height(32.dp)) }
            }
        }
    }
}

@Composable
fun MemberActivityHeader(name: String, role: String, phone: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111827).copy(alpha = 0.85f)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.2f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(56.dp).background(NeonCyan, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(name.take(1), style = MaterialTheme.typography.headlineMedium, color = Color.Black, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(name, style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                Text(role, style = MaterialTheme.typography.bodyMedium, color = AfroGold)
                Text(phone, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
    }
}

@Composable
fun MemberGlobalSummary(totalCotise: Long, detteTotale: Long, totalInterets: Long) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SummaryMiniCard(Modifier.weight(1f), "Cotisé", "$totalCotise F", NeonCyan)
        SummaryMiniCard(Modifier.weight(1f), "Dette", "$detteTotale F", Color(0xFFFF7A59))
        SummaryMiniCard(Modifier.weight(1f), "Intérêts", "$totalInterets F", AfroGold)
    }
}

@Composable
fun SummaryMiniCard(modifier: Modifier, label: String, value: String, color: Color) {
    Surface(
        modifier = modifier,
        color = Color(0xFF1F2937).copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Text(value, style = MaterialTheme.typography.titleMedium, color = color, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
        Icon(icon, null, tint = NeonCyan, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleSmall, color = NeonCyan, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun CaisseActivityCard(info: MemberCaisseActivity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1E2A).copy(alpha = 0.6f))
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(info.caisseName, fontWeight = FontWeight.Bold, color = Color.White)
                Text("${info.nombreParts} Part(s)", color = AfroGold, style = MaterialTheme.typography.labelSmall)
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total versé : ${info.totalVerse} F", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                if (info.estBeneficiaire) {
                    Text("BÉNÉFICIAIRE", style = MaterialTheme.typography.labelSmall, color = NeonCyan)
                }
            }
        }
    }
}

@Composable
fun LoanActivityCard(loan: MemberLoanActivity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1E2A).copy(alpha = 0.6f))
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Prêt du ${SimpleDateFormat("dd/MM/yy").format(Date(loan.date))}", fontWeight = FontWeight.Bold, color = Color.White)
                Text(loan.statut, color = if(loan.statut == "REMBOURSE") NeonCyan else Color(0xFFFF7A59), style = MaterialTheme.typography.labelSmall)
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = loan.montantRembourse.toFloat() / loan.montantTotal.coerceAtLeast(1L),
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                color = NeonCyan,
                trackColor = Color.White.copy(alpha = 0.1f)
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Restant : ${loan.montantTotal - loan.montantRembourse} F", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Text("${loan.montantTotal} F", style = MaterialTheme.typography.bodySmall, color = Color.White)
            }
        }
    }
}

@Composable
fun TransactionRow(tx: MemberTransactionActivity) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(8.dp).background(if(tx.montant > 0) NeonCyan else Color.Red, CircleShape))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(tx.type, style = MaterialTheme.typography.bodyMedium, color = Color.White)
            Text(SimpleDateFormat("dd MMM yyyy", Locale.FRANCE).format(Date(tx.date)), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        }
        Text(
            "${if(tx.montant > 0) "+" else ""}${tx.montant} F",
            color = if(tx.montant > 0) NeonCyan else Color(0xFFFF7A59),
            fontWeight = FontWeight.Bold
        )
    }
}
