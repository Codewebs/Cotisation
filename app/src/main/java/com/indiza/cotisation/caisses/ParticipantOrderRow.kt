package com.indiza.cotisation.caisses

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.indiza.cotisation.tontinemembers.ParticipantWithParts
import com.indiza.cotisation.tools.AfroGold
import com.indiza.cotisation.tools.GlassWhite
import com.indiza.cotisation.tools.NeonCyan

@Composable
fun ParticipantOrderRow(
    participant: ParticipantWithParts,
    ordre: Int,
    isRedistribution: Boolean,
    aCotiseCeCycle: Boolean = false,
    partsRecuesTotal: Int = 0,
    canReorder: Boolean = true,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    val aToutRecu = partsRecuesTotal >= participant.nombreParts

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(GlassWhite.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isRedistribution) {
            Text(
                text = "#$ordre",
                style = MaterialTheme.typography.titleMedium,
                color = if (aToutRecu) Color.Gray else AfroGold,
                modifier = Modifier.width(40.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = participant.member.nomSaisi,
                    color = if (aCotiseCeCycle) Color(0xFF4CAF50) else if (aToutRecu) Color.Gray else Color.White,
                    fontWeight = if (aCotiseCeCycle) FontWeight.Bold else FontWeight.Normal
                )
                
                if (partsRecuesTotal > 0) {
                    Spacer(Modifier.width(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        repeat(partsRecuesTotal) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Part reçue",
                                tint = NeonCyan,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
            
            Text(
                text = participant.member.role.name, 
                style = MaterialTheme.typography.labelSmall, 
                color = GlassWhite.copy(alpha = 0.6f)
            )
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (participant.nombreParts > 0) {
                    Surface(
                        color = (if (aToutRecu) Color.Gray else NeonCyan).copy(alpha = 0.1f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "${participant.nombreParts} PARTS",
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (aToutRecu) Color.Gray else NeonCyan,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                if (aCotiseCeCycle) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "PAYÉ", 
                        style = MaterialTheme.typography.labelSmall, 
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        if (isRedistribution && canReorder) {
            Row {
                IconButton(onClick = onMoveUp, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onMoveDown, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
