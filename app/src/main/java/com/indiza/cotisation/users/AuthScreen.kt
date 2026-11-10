package com.indiza.cotisation.users

import android.util.Patterns
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.indiza.cotisation.tools.*
import androidx.compose.ui.text.style.TextAlign
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
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
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
                    value = email, onValueChange = { email = it },
                    label = { Text("Adresse e-mail") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = password, onValueChange = { password = it },
                    label = { Text("Mot de passe") },
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
                        if (email.isBlank() || password.isBlank()) {
                            errorMessage = "Veuillez remplir tous les champs"
                            return@Button
                        }
                        isLoading = true
                        viewModel.login(email, password) { result ->
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
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isErrorMessage by remember { mutableStateOf(true) }
    var confirmationPending by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("REJOINDRE L'AVENTURE", color = Color(0xFF2D3436), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Créez votre profil", color = Color.Gray, modifier = Modifier.padding(bottom = 16.dp))
        
        CleanCard {
            OutlinedTextField(value = nom, onValueChange = { nom = it }, label = { Text("Nom complet") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Adresse e-mail") }, modifier = Modifier.fillMaxWidth())
            Text("Format valide requis (ex: nom@domaine.com)", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(start = 8.dp, top = 2.dp))
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Téléphone") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Mot de passe") }, modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation())
            Text("Minimum 6 caractères requis", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(start = 8.dp, top = 2.dp))
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = confirmPassword, onValueChange = { confirmPassword = it }, label = { Text("Confirmer le mot de passe") }, modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation())
            
            if (errorMessage != null) {
                var showDetail by remember(errorMessage) { mutableStateOf(false) }
                Text(
                    text = if (showDetail) errorMessage!! else if (isErrorMessage) {
                        "Erreur d'inscription. Appuyez pour voir le détail."
                    } else {
                        errorMessage!!
                    },
                    color = if (isErrorMessage) MaterialTheme.colorScheme.error else Color(0xFF16803C),
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
                    if (nom.isBlank()) {
                        isErrorMessage = true
                        errorMessage = "Veuillez saisir votre nom complet."
                        return@Button
                    }
                    if (!Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
                        isErrorMessage = true
                        errorMessage = "Veuillez saisir une adresse e-mail valide."
                        return@Button
                    }
                    if (password.length < 6) {
                        isErrorMessage = true
                        errorMessage = "Le mot de passe doit contenir au moins 6 caractères."
                        return@Button
                    }
                    if (password != confirmPassword) {
                        isErrorMessage = true
                        errorMessage = "Les mots de passe ne correspondent pas"
                        return@Button
                    }
                    isLoading = true
                    viewModel.register(nom, MatriculeGenerator.generate(), phone, email, password) { result ->
                        isLoading = false
                        result.onSuccess { registration ->
                            when (registration) {
                                is RegistrationResult.SignedIn -> onRegisterSuccess()
                                is RegistrationResult.ConfirmationRequired -> {
                                    confirmationPending = true
                                    isErrorMessage = false
                                    errorMessage = "Compte créé pour ${registration.email}. Confirmez votre adresse e-mail, puis revenez ici pour vous connecter."
                                }
                            }
                        }
                              .onFailure { error ->
                                  isErrorMessage = true
                                  errorMessage = error.message ?: "Erreur inscription"
                              }
                    }
                },
                enabled = !isLoading && !confirmationPending,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AfroOrange)
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
            else Text(
                if (confirmationPending) "E-MAIL À CONFIRMER" else "CRÉER MON COMPTE",
                fontWeight = FontWeight.Bold
            )
            }
        }
        TextButton(onClick = onBackToLogin) {
            Text("Déjà un compte ? ", color = Color.Gray)
            Text("Se connecter", color = AfroOrange)
        }
    }
}
