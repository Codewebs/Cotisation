package com.indiza.cotisation.tools

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random


// Palette principale
val AfroOrange = Color(0xFFF89070)        // Orange vif, énergique
val AfroDarkOrange = Color(0xFF9D3F21)        // Orange vif, énergique
val AfroGold = Color(0xFFFFC107)          // Or chaleureux
val DeepViolet = Color(0xFF8F7DDC)        // Violet profond, mystique
val DeepWhite = Color(0xFFFDFDFF)        // Violet profond, mystique
val DeepDarkViolet = Color(0xFF231759)        // Violet profond, mystique
val GlassWhite = Color(0xCCFFFFFF)        // Blanc transparent (effet verre)
val NeonCyan = Color(0xFF00E5FF)          // Cyan néon, high-tech
val TextColor1 = Color(0xFF143569)
val TextColor2 = Color(0xFF020F23)
val TextColor3 = Color(0xFF000000)


val SwitchColor1 = Color(0xFF6D268A)
val SwitchColor2 = Color(0xFF261F56)
val SwitchColor3 = Color(0xFF05011C)




val SolarYellowligth = Color(0xFFFCF7E5)       // Jaune solaire attenué
val SolarDarkligth = Color(0xFF6B5B24)       // Jaune solaire attenué
val DeepSpaceBlack = Color(0xFF121212)    // Noir espace profond

// Dégradés typiques
val AfroSunsetGradient = Brush.horizontalGradient(
    listOf(NeonCyan,AfroOrange, AfroGold, DeepViolet)
)


@Composable
fun AfroFuturistTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    // Typographie
    val AfroTypography = MaterialTheme.typography.copy(
        displayLarge = MaterialTheme.typography.displayLarge.copy(
            fontWeight = FontWeight.Black,
            letterSpacing = (-0.5).sp
        ),
        headlineLarge = MaterialTheme.typography.headlineLarge.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        ),
        titleLarge = MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.SemiBold
        )
    )


    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = AfroOrange,
            secondary = NeonCyan,
            tertiary = AfroGold,
            background = DeepSpaceBlack,
            surface = Color(0xFF1E1E1E),
            onPrimary = Color.White,
            onSecondary = Color.Black,
            onTertiary = Color.Black,
            onBackground = GlassWhite,
            onSurface = GlassWhite.copy(alpha = 0.87f),
            error = Color(0xFFCF6679),
            onError = Color.Black
        )
    } else {
        lightColorScheme(
            primary = AfroOrange,
            secondary = NeonCyan,
            tertiary = AfroGold,
            background = GlassWhite.copy(alpha = 0.95f),
            surface = Color.White,
            onPrimary = Color.White,
            onSecondary = Color.Black,
            onTertiary = Color.Black,
            onBackground = DeepSpaceBlack,
            onSurface = DeepSpaceBlack,
            error = Color(0xFFB00020),
            onError = Color.White
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AfroTypography,
        content = content
    )
}


@Composable
fun CosmicBackground(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        content()
    }
}

