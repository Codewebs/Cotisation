package com.indiza.cotisation.tontine

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.indiza.cotisation.tools.CosmicBackground
import com.indiza.cotisation.tools.NeonCyan
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeetingChatScreen(
    tontineId: String,
    onBack: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var textContent by remember { mutableStateOf("") }
    val context = LocalContext.current
    val listState = rememberLazyListState()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.onImageSelected(context, it) }
    }

    LaunchedEffect(tontineId) {
        viewModel.loadComments(tontineId)
        viewModel.observeComments(tontineId)
    }

    // Scroll to bottom when new messages arrive
    LaunchedEffect(state.comments.size) {
        if (state.comments.isNotEmpty()) {
            listState.animateScrollToItem(state.comments.size - 1)
        }
    }

    CosmicBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("Chat de Réunion", fontSize = 18.sp, fontWeight = FontWeight.Black)
                            Text("Discussions en direct", fontSize = 10.sp, color = Color.Gray)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF111827).copy(alpha = 0.8f), titleContentColor = Color.White)
                )
            }
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                
                // Message List
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    if (state.isLoading && state.comments.isEmpty()) {
                        item {
                            Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = NeonCyan)
                            }
                        }
                    } else if (state.comments.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier.fillParentMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    state.errorMessage ?: "Aucun message pour le moment.",
                                    color = if (state.errorMessage == null) Color.Gray else Color(0xFFFF8A80),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                if (state.errorMessage != null) {
                                    TextButton(onClick = { viewModel.loadComments(tontineId) }) {
                                        Text("Réessayer", color = NeonCyan)
                                    }
                                }
                            }
                        }
                    } else {
                        items(state.comments, key = { it.id }) { comment ->
                            ChatBubble(comment)
                        }
                    }
                }
                state.errorMessage?.takeIf { state.comments.isNotEmpty() }?.let { errorMessage ->
                    Text(
                        errorMessage,
                        color = Color(0xFFFF8A80),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        fontSize = 12.sp
                    )
                }

                // Input Area
                Surface(
                    color = Color(0xFF1F2937),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    tonalElevation = 8.dp
                ) {
                    Column(Modifier.padding(16.dp)) {
                        
                        // Image Preview
                        if (state.selectedImageUri != null) {
                            Box(Modifier.padding(bottom = 12.dp)) {
                                AsyncImage(
                                    model = state.selectedImageUri,
                                    contentDescription = null,
                                    modifier = Modifier.size(100.dp).clip(RoundedCornerShape(12.dp)).border(2.dp, NeonCyan, RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                IconButton(
                                    onClick = { viewModel.clearImage() },
                                    modifier = Modifier.size(24.dp).align(Alignment.TopEnd).offset(8.dp, (-8).dp).background(Color.Red, CircleShape)
                                ) {
                                    Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            IconButton(
                                onClick = { launcher.launch("image/*") },
                                colors = IconButtonDefaults.iconButtonColors(containerColor = Color.White.copy(alpha = 0.05f))
                            ) {
                                Icon(Icons.Default.PhotoCamera, null, tint = NeonCyan)
                            }

                            TextField(
                                value = textContent,
                                onValueChange = { textContent = it },
                                placeholder = { Text("Écrivez un message...", fontSize = 14.sp) },
                                modifier = Modifier.weight(1f),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                maxLines = 3
                            )

                            FloatingActionButton(
                                onClick = {
                                    if (textContent.isNotBlank() || state.selectedImageUri != null) {
                                        viewModel.sendComment(context, tontineId, textContent)
                                        textContent = ""
                                    }
                                },
                                containerColor = NeonCyan,
                                contentColor = Color.Black,
                                shape = CircleShape,
                                modifier = Modifier.size(48.dp)
                            ) {
                                if (state.isSending) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.Black, strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.AutoMirrored.Filled.Send, null)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(comment: CommentResponse) {
    val authorColors = listOf(
        Color(0xFF1E293B), // Dark Slate
        Color(0xFF1E3A8A), // Dark Blue
        Color(0xFF065F46), // Dark Emerald
        Color(0xFF581C87), // Dark Violet
        Color(0xFF831843), // Dark Rose
        Color(0xFF7C2D12)  // Dark Amber
    )
    val authorAccentColors = listOf(
        NeonCyan,
        Color(0xFF60A5FA),
        Color(0xFF34D399),
        Color(0xFFC084FC),
        Color(0xFFF472B6),
        Color(0xFFFBBF24)
    )
    val authorKey = comment.author.nom.ifBlank { "Membre" }.lowercase()
    val colorIndex = Math.abs(authorKey.hashCode()) % authorColors.size
    val bubbleBg = authorColors[colorIndex]
    val accentColor = authorAccentColors[colorIndex]
    val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(comment.createdAt))

    Surface(
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .padding(vertical = 4.dp),
        color = bubbleBg,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // 1. Image (if present)
            if (comment.imageUrl != null) {
                AsyncImage(
                    model = comment.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Fit
                )
                if (comment.content.isNotBlank()) Spacer(Modifier.height(8.dp))
            }

            // 2. Message content
            if (comment.content.isNotBlank()) {
                Text(
                    text = comment.content,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(8.dp))

            // 3. Fine divider line
            HorizontalDivider(
                color = Color.White.copy(alpha = 0.15f),
                thickness = 0.5.dp
            )

            Spacer(Modifier.height(6.dp))

            // 4. Author name & date below the divider inside the single block
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = comment.author.nom.ifBlank { "MEMBRE" }.uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
                Text(
                    text = time,
                    fontSize = 9.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }
    }
}
