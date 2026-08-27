package com.indiza.cotisation.tontine

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.indiza.cotisation.caisses.FrequenceCotisation
import com.indiza.cotisation.tools.NeonCyan

@Composable
fun FrequenceSelector(
    selected: FrequenceCotisation,
    onSelected: (FrequenceCotisation) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text("Fréquence des versements", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FrequenceCotisation.values().forEach { freq ->
                val isSelected = freq == selected
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelected(freq) },
                    label = { Text(freq.name) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NeonCyan,
                        selectedLabelColor = Color.Black
                    )
                )
            }
        }
    }
}