package com.indiza.cotisation.users

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.indiza.cotisation.tontine.TontineEntity
import com.indiza.cotisation.tools.AfroGold
import com.indiza.cotisation.tools.AfroOrange
import com.indiza.cotisation.tools.NeonCyan

// Thème de couleurs
val DeepViolet = Color(0xFF1E1035)
val DarkPurple = Color(0xFF2D1B4E)
val GlassWhite = Color(0x1FFFFFFF)
val CardBackground = Color(0xCC1A102A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchTontineScreen(
    viewModel: SearchTontineViewModel,
    sessionViewModel: SessionViewModel = hiltViewModel(),
    notifViewModel: NotificationViewModel = hiltViewModel(),
    adminViewModel: AdminStatsViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onCreateTontineWhenAuthenticated: () -> Unit,
    onNavigateToAuth: () -> Unit,
    onJoinTontine: (TontineEntity) -> Unit,
    onNavigateToList: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToReceipt: (String) -> Unit,
    onLogout: () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var searchQuery by remember { mutableStateOf("") }
    var isFabExpanded by remember { mutableStateOf(false) }
    var showNotifMenu by remember { mutableStateOf(false) }

    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val joinStatus by viewModel.joinStatus.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(joinStatus, errorMessage) {
        joinStatus?.let { snackbarHostState.showSnackbar(it) }
        errorMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    val hasSession by sessionViewModel.hasActiveSession.collectAsStateWithLifecycle(
        initialValue = false,
        lifecycle = lifecycleOwner.lifecycle
    )
    val notifState by notifViewModel.state.collectAsStateWithLifecycle()
    val adminState by adminViewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(hasSession) {
        if (hasSession) {
            notifViewModel.fetchNotifications()
            notifViewModel.observeNotifications()
            adminViewModel.fetchStats()
        }
    }

    fun handleCreateTontineClick() {
        if (hasSession) {
            onCreateTontineWhenAuthenticated()
        } else {
            onNavigateToAuth()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (hasSession) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AnimatedVisibility(
                        visible = isFabExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
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
                                    Text("Mes Réunions", fontWeight = FontWeight.SemiBold)
                                }
                            }

                            FloatingActionButton(
                                onClick = {
                                    isFabExpanded = false
                                    onNavigateToProfile()
                                },
                                containerColor = DarkPurple,
                                contentColor = NeonCyan,
                                shape = CircleShape
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.AccountCircle, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Mon Profil", fontWeight = FontWeight.SemiBold)
                                }
                            }

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
                        text = { Text(if (isFabExpanded) "Fermer" else "Mon Compte") }
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
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

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.refreshSearch(searchQuery) },
                            modifier = Modifier
                                .background(GlassWhite, CircleShape)
                                .size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Actualiser",
                                tint = Color.White
                            )
                        }

                        if (hasSession) {
                            Box {
                                IconButton(
                                    onClick = { showNotifMenu = true },
                                    modifier = Modifier
                                        .background(GlassWhite, CircleShape)
                                        .size(40.dp)
                                ) {
                                    BadgedBox(
                                        badge = {
                                            if (notifState.unreadCount > 0) {
                                                Badge(containerColor = Color.Red) {
                                                    Text(notifState.unreadCount.toString(), color = Color.White)
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (notifState.unreadCount > 0) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                                            contentDescription = "Notifications",
                                            tint = if (notifState.unreadCount > 0) AfroGold else Color.White
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = showNotifMenu,
                                    onDismissRequest = { showNotifMenu = false },
                                    modifier = Modifier.background(DarkPurple).width(280.dp)
                                ) {
                                    if (notifState.notifications.isEmpty()) {
                                        DropdownMenuItem(
                                            text = { Text("Aucune notification", color = Color.Gray) },
                                            onClick = {}
                                        )
                                    }
                                    notifState.notifications.take(5).forEach { notif ->
                                        val isRead = notif.isRead
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = notif.title, 
                                                            fontWeight = if (isRead) FontWeight.Medium else FontWeight.Bold, 
                                                            color = if (isRead) Color.White.copy(alpha = 0.5f) else NeonCyan, 
                                                            fontSize = 12.sp
                                                        )
                                                        if (!isRead) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(6.dp)
                                                                    .background(AfroGold, CircleShape)
                                                            )
                                                        }
                                                    }
                                                    Spacer(Modifier.height(2.dp))
                                                    Text(
                                                        text = notif.message, 
                                                        color = if (isRead) Color.Gray else Color.White, 
                                                        fontSize = 10.sp, 
                                                        maxLines = 2
                                                    )
                                                }
                                            },
                                            modifier = Modifier.background(
                                                if (isRead) Color.Transparent else Color.White.copy(alpha = 0.08f)
                                            ),
                                            onClick = {
                                                showNotifMenu = false
                                                notifViewModel.markAsRead(notif.id)
                                                if (notif.type in setOf("RECEIPT", "COTISATION_RECEIPT") &&
                                                    notif.referenceId != null
                                                ) {
                                                    onNavigateToReceipt(notif.referenceId)
                                                } else {
                                                    onNavigateToList()
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Titre
                Text(
                    text = "REJOINDRE LA RÉUNION",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        brush = Brush.horizontalGradient(listOf(AfroGold, AfroOrange))
                    ),
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )

                Text(
                    text = "Recherchez par code (ex: A1B2C3) ou par le nom d'une réunion.",
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

                // Stats Administrateur (Si connecté et signatures > 0)
                if (hasSession && (adminState.stats?.count ?: 0) > 0) {
                    AdminQuickStats(stats = adminState.stats!!)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Bannière "Créer une réunion"
                CreateTontineCallToAction(
                    isAuthenticated = hasSession,
                    onCreateClick = ::handleCreateTontineClick
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Option par défaut "Les amis du dimanche"
                DefaultTontineCallToAction(
                    onClick = {
                        if (hasSession) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                viewModel.openOrJoinDefaultTontine(onOpen = { tontine ->
                                    onJoinTontine(tontine)
                                })
                            }
                        } else {
                            onNavigateToAuth()
                        }
                    }
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
                        items(searchResults) { result ->
                            TontineFuturisticItem(
                                tontine = result.tontine,
                                membershipStatus = result.membershipStatus,
                                onJoinClick = { 
                                    if (result.membershipStatus == "MEMBER") {
                                        onJoinTontine(result.tontine)
                                    } else if (result.membershipStatus == "NONE") {
                                        viewModel.requestJoin(result.tontine.remoteId!!, searchQuery)
                                    }
                                },
                                onCancelJoinClick = {
                                    viewModel.cancelJoinRequest(result.tontine.remoteId!!, searchQuery)
                                }
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
    val title = if (isAuthenticated) "CRÉER VOTRE RÉUNION" else "SE CONNECTER POUR CRÉER"
    val subtitle = if (isAuthenticated) "Lancez votre propre réunion en quelques clics" else "Authentifiez-vous pour gérer votre réunion"

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
fun DefaultTontineCallToAction(
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = BorderStroke(1.dp, AfroGold.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(AfroGold.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Groups,
                    contentDescription = null,
                    tint = AfroGold,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "LES AMIS DU DIMANCHE",
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Rejoindre ou entrer dans la réunion par défaut",
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
fun TontineFuturisticItem(
    tontine: TontineEntity, 
    membershipStatus: String,
    onJoinClick: () -> Unit,
    onCancelJoinClick: () -> Unit = {}
) {
    val buttonColor = when(membershipStatus) {
        "MEMBER" -> NeonCyan
        else -> AfroOrange
    }
    
    val buttonText = when(membershipStatus) {
        "MEMBER" -> "OUVRIR"
        else -> "REJOINDRE"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (membershipStatus == "PENDING") 0.58f else 1f)
            .drawBehind {
                drawRoundRect(
                    brush = if (membershipStatus == "PENDING") {
                        Brush.linearGradient(listOf(Color.LightGray, Color.Gray))
                    } else {
                        Brush.linearGradient(listOf(AfroOrange, AfroGold))
                    },
                    cornerRadius = CornerRadius(20.dp.toPx()),
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
                    text = when (membershipStatus) {
                        "MEMBER" -> "Vous êtes membre"
                        "PENDING" -> "Demande en attente de validation"
                        else -> "ID: ${tontine.code ?: tontine.remoteId?.take(8)}"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = when (membershipStatus) {
                        "MEMBER" -> NeonCyan
                        "PENDING" -> AfroGold
                        else -> AfroOrange
                    }
                )
            }

            if (membershipStatus == "PENDING") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = {},
                        enabled = false,
                        modifier = Modifier.height(36.dp),
                        colors = ButtonDefaults.buttonColors(
                            disabledContainerColor = Color(0xFF5C5962),
                            disabledContentColor = Color.LightGray
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text("EN ATTENTE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black)
                    }
                    IconButton(
                        onClick = onCancelJoinClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Retirer la demande d'adhésion",
                            tint = Color(0xFFEF4444)
                        )
                    }
                }
            } else {
                Button(
                    onClick = onJoinClick,
                    modifier = Modifier.height(36.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = buttonColor,
                        contentColor = if (buttonColor == NeonCyan) Color.Black else Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Text(buttonText, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
fun AdminQuickStats(stats: com.indiza.cotisation.users.AdminSignatureStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = NeonCyan.copy(alpha = 0.05f)),
        border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(NeonCyan.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Check, null, tint = NeonCyan)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "CERTIFICATIONS : ${stats.month.uppercase()}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan,
                    letterSpacing = 1.sp
                )
                Text(
                    "${stats.count} reçus signés ce mois",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
            Text(
                "${stats.totalAmount} F",
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
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
            text = "Aucune réunion trouvée pour « $query »",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}
