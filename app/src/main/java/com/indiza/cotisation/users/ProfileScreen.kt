package com.indiza.cotisation.users

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.indiza.cotisation.tools.AfroGold
import com.indiza.cotisation.tools.AfroOrange
import com.indiza.cotisation.tools.NeonCyan

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onTontineClick: (Long) -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mon Profil", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Déconnexion", tint = Color.Red)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF111827))
            )
        },
        containerColor = Color(0xFF0F172A)
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NeonCyan)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                // Header Profil
                item {
                    ProfileHeader(state.user)
                }

                // Statistiques
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ProfileStatCard(
                            modifier = Modifier.weight(1f),
                            label = "Créées",
                            count = state.myCreatedTontines.size,
                            color = NeonCyan
                        )
                        ProfileStatCard(
                            modifier = Modifier.weight(1f),
                            label = "Membre de",
                            count = state.myMemberTontines.size,
                            color = AfroGold
                        )
                    }
                }

                // Section "Mes Tontines Créées"
                if (state.myCreatedTontines.isNotEmpty()) {
                    item {
                        SectionTitle("Réunions que j'ai créées")
                    }
                    items(state.myCreatedTontines) { item ->
                        MiniTontineRow(item.tontine.libelle, "Initiateur", NeonCyan) {
                            onTontineClick(item.tontine.id)
                        }
                    }
                }

                // Section "Mes Participations"
                if (state.myMemberTontines.isNotEmpty()) {
                    item {
                        SectionTitle("Mes Participations")
                    }
                    items(state.myMemberTontines) { item ->
                        MiniTontineRow(item.tontine.libelle, item.role.displayName, AfroGold) {
                            onTontineClick(item.tontine.id)
                        }
                    }
                }

                item { Spacer(Modifier.height(40.dp)) }
            }
        }
    }
}

@Composable
fun ProfileHeader(user: UserEntity?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(AfroOrange.copy(alpha = 0.2f))
                    .border(2.dp, AfroOrange, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, null, modifier = Modifier.size(60.dp), tint = AfroOrange)
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = user?.nom?.uppercase() ?: "UTILISATEUR",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = user?.phone ?: "Aucun numéro",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )

            if (!user?.matricule.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    color = Color.White.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "ID: ${user?.matricule}",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonCyan
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileStatCard(modifier: Modifier, label: String, count: Int, color: Color) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = count.toString(), style = MaterialTheme.typography.headlineMedium, color = color, fontWeight = FontWeight.Black)
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = Color.Gray,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
fun MiniTontineRow(name: String, role: String, color: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = Color(0xFF1E293B),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(name, color = Color.White, fontWeight = FontWeight.Bold)
                Text(role, color = color, style = MaterialTheme.typography.labelSmall)
            }
            Icon(Icons.Default.ChevronRight, null, tint = Color.Gray)
        }
    }
}
