package com.indiza.cotisation.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Composants uniformes pour l'application Cotisation.
 * Utilise un style épuré (blanc majoritaire) avec des bordures marquées.
 */
object CotisationDesign {

    @Composable
    fun PrimaryButton(
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        text: String,
        enabled: Boolean = true,
        borderColor: Color = Black
    ) {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            shape = Shapes.medium,
            border = BorderStroke(1.5.dp, borderColor),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = White,
                contentColor = Black
            ),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Text(
                text = text.uppercase(),
                style = Typography.labelLarge,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
        }
    }

    @Composable
    fun SecondaryButton(
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        text: String,
        borderColor: Color = BlueBorder
    ) {
        PrimaryButton(onClick = onClick, modifier = modifier, text = text, borderColor = borderColor)
    }

    @Composable
    fun SuccessButton(
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        text: String
    ) {
        PrimaryButton(onClick = onClick, modifier = modifier, text = text, borderColor = GreenBorder)
    }

    @Composable
    fun CardContainer(
        modifier: Modifier = Modifier,
        borderColor: Color = Black,
        borderWidth: Dp = 1.dp,
        content: @Composable () -> Unit
    ) {
        Surface(
            modifier = modifier,
            shape = Shapes.medium,
            color = White,
            border = BorderStroke(borderWidth, borderColor),
            shadowElevation = 0.dp
        ) {
            content()
        }
    }
}
