package com.indiza.cotisation.tontine

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
                    items(state.comments) { comment ->
                        ChatBubble(comment)
                    }
                }

                // Input Area
                Surface(
                    color = Color(0xFF1F2937),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    tonalElevation = 8.dp
                ) {
                    Column(Modifier.padding(16.dp)) {
                        
                        // Image Preview
                        if (state.selectedImageBase64 != null) {
                            Box(Modifier.padding(bottom = 12.dp)) {
                                AsyncImage(
                                    model = state.selectedImageBase64,
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
                                    if (textContent.isNotBlank() || state.selectedImageBase64 != null) {
                                        viewModel.sendComment(tontineId, textContent)
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
    // Note: Pour simplifier on ne check pas si c'est "Me" car l'ID local n'est pas passé ici
    // Mais on peut utiliser le style standard
    Column(horizontalAlignment = Alignment.Start) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(comment.author.nom.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.Gray)
            val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(comment.createdAt))
            Text(time, fontSize = 8.sp, color = Color.DarkGray)
        }
        Spacer(Modifier.height(4.dp))
        Surface(
            color = Color(0xFF374151),
            shape = RoundedCornerShape(topEnd = 20.dp, bottomEnd = 20.dp, bottomStart = 20.dp)
        ) {
            Column(Modifier.padding(12.dp)) {
                if (comment.imageUrl != null) {
                    AsyncImage(
                        model = comment.imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp).clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Fit
                    )
                    if (comment.content.isNotBlank()) Spacer(Modifier.height(8.dp))
                }
                if (comment.content.isNotBlank()) {
                    Text(comment.content, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
