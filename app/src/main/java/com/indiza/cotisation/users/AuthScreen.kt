package com.indiza.cotisation.users

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.indiza.cotisation.tools.*
import androidx.compose.ui.text.style.TextAlign
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable

@Composable
fun AuthScreen(onAuthSuccess: () -> Unit) {
    var showRegister by remember { mutableStateOf(false) }

    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(Color(0xFFF8F9FA), Color(0xFFE9ECEF))
    )

    Box(modifier = Modifier.fillMaxSize().background(backgroundBrush)) {
        if (showRegister) {
            RegisterScreen(
                onRegisterSuccess = onAuthSuccess,
                onBackToLogin = { showRegister = false }
            )
        } else {
            LoginScreen(
                onLoginSuccess = onAuthSuccess,
                onNavigateToRegister = { showRegister = true }
            )
        }
    }
}

@Composable
fun CleanCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp), content = content)
    }
}

@Composable
fun LoginScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    var phone by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showUrlSettings by remember { mutableStateOf(false) }
    val context = LocalContext.current

    if (showUrlSettings) {
        UrlSettingsDialog(
            onDismiss = { showUrlSettings = false },
            onSaved = {
                Toast.makeText(context, "URL mise à jour. Veuillez redémarrer l'application.", Toast.LENGTH_LONG).show()
                showUrlSettings = false
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        IconButton(
            onClick = { showUrlSettings = true },
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
        ) {
            Icon(Icons.Default.Settings, contentDescription = "Paramètres API", tint = Color.Gray)
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.Groups, contentDescription = null, tint = AfroOrange, modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text("BON RETOUR", color = Color(0xFF2D3436), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Connectez-vous pour accéder à vos tontines", color = Color.Gray)
            
            Spacer(modifier = Modifier.height(32.dp))

            CleanCard {
                OutlinedTextField(
                    value = phone, onValueChange = { phone = it },
                    label = { Text("Numéro de téléphone") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = pin, onValueChange = { pin = it },
                    label = { Text("Code PIN") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    shape = RoundedCornerShape(16.dp)
                )
                if (errorMessage != null) {
                    var showDetail by remember(errorMessage) { mutableStateOf(false) }
                    Text(
                        text = if (showDetail) errorMessage!! else "Problème de connexion au serveur. Appuyez pour voir le détail.",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .clickable { showDetail = !showDetail },
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        if (phone.isBlank() || pin.length != 4) {
                            errorMessage = "Veuillez remplir tous les champs"
                            return@Button
                        }
                        isLoading = true
                        viewModel.login(phone, pin) { result ->
                            isLoading = false
                            result.onSuccess { onLoginSuccess() }
                                  .onFailure { error ->
                                      errorMessage = error.message ?: "Erreur de connexion"
                                  }
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AfroOrange)
                ) {
                    if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                    else Text("SE CONNECTER", fontWeight = FontWeight.Bold)
                }
            }
            
            TextButton(onClick = onNavigateToRegister) {
                Text("Pas encore membre ? ", color = Color.Gray)
                Text("Créer un compte", color = AfroOrange)
            }
        }
    }
}

@Composable
fun RegisterScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onRegisterSuccess: () -> Unit,
    onBackToLogin: () -> Unit
) {
    var nom by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("REJOINDRE L'AVENTURE", color = Color(0xFF2D3436), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Créez votre profil", color = Color.Gray, modifier = Modifier.padding(bottom = 16.dp))
        
        CleanCard {
            OutlinedTextField(value = nom, onValueChange = { nom = it }, label = { Text("Nom complet") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Téléphone") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = pin, onValueChange = { if(it.length <= 4) pin = it }, label = { Text("PIN") }, modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = confirmPin, onValueChange = { if(it.length <= 4) confirmPin = it }, label = { Text("Confirmer PIN") }, modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation())
            
            if (errorMessage != null) {
                var showDetail by remember(errorMessage) { mutableStateOf(false) }
                Text(
                    text = if (showDetail) errorMessage!! else "Erreur d'inscription. Appuyez pour voir le détail.",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .clickable { showDetail = !showDetail },
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    if (nom.isBlank() || phone.isBlank() || pin.length != 4) {
                        errorMessage = "Champs invalides"
                        return@Button
                    }
                    if (pin != confirmPin) {
                        errorMessage = "Les PIN ne correspondent pas"
                        return@Button
                    }
                    isLoading = true
                    viewModel.register(nom, MatriculeGenerator.generate(), phone, pin) { result ->
                        isLoading = false
                        result.onSuccess { onRegisterSuccess() }
                              .onFailure { error ->
                                  errorMessage = error.message ?: "Erreur inscription"
                              }
                    }
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AfroOrange)
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                else Text("CRÉER MON COMPTE", fontWeight = FontWeight.Bold)
            }
        }
        TextButton(onClick = onBackToLogin) {
            Text("Déjà un compte ? ", color = Color.Gray)
            Text("Se connecter", color = AfroOrange)
        }
    }
}

@Composable
fun UrlSettingsDialog(onDismiss: () -> Unit, onSaved: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("cotisation_prefs", Context.MODE_PRIVATE) }
    var url by remember { mutableStateOf(prefs.getString("api_base_url", "http://10.0.2.2:3000/") ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("URL du Serveur API") },
        text = {
            Column {
                Text("Modifiez l'URL pour pointer vers votre serveur local ou distant.", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL de base") },
                    placeholder = { Text("http://192.168.x.x:3000/") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                prefs.edit().putString("api_base_url", url).apply()
                onSaved()
            }) { Text("ENREGISTRER") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("ANNULER") }
        }
    )
}
