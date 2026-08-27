package com.indiza.cotisation.users

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.indiza.cotisation.tontine.TontineEntity
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon

import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.indiza.cotisation.tools.AfroGold
import com.indiza.cotisation.tools.AfroOrange
import com.indiza.cotisation.tools.BackButton
import com.indiza.cotisation.tools.DeepViolet
import com.indiza.cotisation.tools.GlassWhite

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*

import androidx.compose.ui.draw.clip


// Thème de couleurs
val DeepViolet = Color(0xFF1E1035)
val DarkPurple = Color(0xFF2D1B4E)
val AfroOrange = Color(0xFFFF5722)
val AfroGold = Color(0xFFFFC107)
val GlassWhite = Color(0x1FFFFFFF)
val CardBackground = Color(0xCC1A102A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchTontineScreen(
    viewModel: SearchTontineViewModel,
    sessionViewModel: SessionViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onCreateTontineWhenAuthenticated: () -> Unit,
    onNavigateToAuth: () -> Unit,
    onJoinTontine: (TontineEntity) -> Unit,
    onNavigateToList: () -> Unit,
    onLogout: () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var searchQuery by remember { mutableStateOf("") }
    var isFabExpanded by remember { mutableStateOf(false) }

    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val hasSession by sessionViewModel.hasActiveSession.collectAsStateWithLifecycle(
        initialValue = false,
        lifecycle = lifecycleOwner.lifecycle
    )

    fun handleCreateTontineClick() {
        if (hasSession) {
            onCreateTontineWhenAuthenticated()
        } else {
            onNavigateToAuth()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            if (hasSession) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Options contextuelles du FAB
                    AnimatedVisibility(
                        visible = isFabExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Action: Mes tontines
                            FloatingActionButton(
                                onClick = {
                                    isFabExpanded = false
                                    onNavigateToList()
                                },
                                containerColor = DarkPurple,
                                contentColor = AfroGold,
                                shape = CircleShape
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Groups, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Mes Tontines", fontWeight = FontWeight.SemiBold)
                                }
                            }

                            // Action: Déconnexion
                            FloatingActionButton(
                                onClick = {
                                    isFabExpanded = false
                                    onLogout()
                                },
                                containerColor = Color(0xFFDC2626),
                                contentColor = Color.White,
                                shape = CircleShape
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Logout, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Déconnexion", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }

                    // Bouton FAB principal
                    ExtendedFloatingActionButton(
                        onClick = { isFabExpanded = !isFabExpanded },
                        containerColor = AfroOrange,
                        contentColor = Color.White,
                        icon = {
                            Icon(
                                imageVector = if (isFabExpanded) Icons.Default.Close else Icons.Default.AccountCircle,
                                contentDescription = "Menu Compte"
                            )
                        },
                        text = { Text(if (isFabExpanded) "Fermer" else "Mon Mon Compte") }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(DeepViolet, Color(0xFF0F081D))
                    )
                )
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
            ) {
                // Bar supérieure d'en-tête
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .background(GlassWhite, CircleShape)
                            .size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Retour",
                            tint = Color.White
                        )
                    }
                }

                // Titre
                Text(
                    text = "REJOINDRE LE CERCLE",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        brush = Brush.horizontalGradient(listOf(AfroGold, AfroOrange))
                    ),
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )

                Text(
                    text = "Recherchez par code (ex: A1B2C3) ou par le nom d'un cercle.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                // Barre de recherche
                SearchBox(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    onSearchClick = {
                        if (searchQuery.isNotBlank()) viewModel.searchTontines(searchQuery)
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Bannière "Créer une tontine"
                CreateTontineCallToAction(
                    isAuthenticated = hasSession,
                    onCreateClick = ::handleCreateTontineClick
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Contenu des résultats
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = AfroOrange)
                    }
                } else if (searchResults.isEmpty() && searchQuery.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyState(query = searchQuery)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(searchResults) { tontine ->
                            TontineFuturisticItem(
                                tontine = tontine,
                                onJoinClick = { onJoinTontine(tontine) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchBox(
    value: String,
    onValueChange: (String) -> Unit,
    onSearchClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = GlassWhite,
        modifier = Modifier.fillMaxWidth()
    ) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    "Rechercher un code ou un nom...",
                    color = Color.White.copy(alpha = 0.5f)
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = AfroGold
                )
            },
            trailingIcon = {
                IconButton(
                    onClick = onSearchClick,
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .background(
                            brush = Brush.horizontalGradient(listOf(AfroOrange, AfroGold)),
                            shape = CircleShape
                        )
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Valider",
                        tint = Color.White
                    )
                }
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun CreateTontineCallToAction(
    isAuthenticated: Boolean,
    onCreateClick: () -> Unit
) {
    val icon = if (isAuthenticated) Icons.Default.AddBusiness else Icons.AutoMirrored.Filled.Login
    val title = if (isAuthenticated) "CRÉER VOTRE CERCLE" else "SE CONNECTER POUR CRÉER"
    val subtitle = if (isAuthenticated) "Lancez votre propre tontine en quelques clics" else "Authentifiez-vous pour gérer votre tontine"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { onCreateClick() },
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(GlassWhite, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = AfroOrange,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = AfroGold
            )
        }
    }
}

@Composable
fun TontineFuturisticItem(tontine: TontineEntity, onJoinClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawRoundRect(
                    brush = Brush.linearGradient(listOf(AfroOrange, AfroGold)),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(20.dp.toPx()),
                    style = Stroke(width = 1.dp.toPx())
                )
            },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        brush = Brush.sweepGradient(listOf(AfroOrange, AfroGold)),
                        shape = CircleShape
                    )
                    .padding(2.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = CircleShape,
                    color = DeepViolet
                ) {
                    Icon(
                        imageVector = Icons.Default.Groups,
                        contentDescription = null,
                        tint = AfroGold,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tontine.libelle.uppercase(),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = "ID: ${tontine.code}",
                    style = MaterialTheme.typography.labelMedium,
                    color = AfroOrange
                )
            }

            IconButton(
                onClick = onJoinClick,
                modifier = Modifier
                    .background(AfroOrange, RoundedCornerShape(12.dp))
                    .size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Rejoindre",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun EmptyState(query: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(24.dp)
    ) {
        Icon(
            imageVector = Icons.Default.SearchOff,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = Color.White.copy(alpha = 0.4f)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Aucun cercle trouvé pour « $query »",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}