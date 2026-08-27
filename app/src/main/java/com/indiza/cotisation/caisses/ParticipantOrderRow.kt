package com.indiza.cotisation.caisses

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.indiza.cotisation.tontine.TontineMemberWithUser
import com.indiza.cotisation.tontinemembers.ParticipantWithParts
import com.indiza.cotisation.tools.AfroGold
import com.indiza.cotisation.tools.GlassWhite
import com.indiza.cotisation.tools.NeonCyan

@Composable
fun ParticipantOrderRow(
    participant: ParticipantWithParts,
    ordre: Int,
    isRedistribution: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
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
                color = AfroGold,
                modifier = Modifier.width(40.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(participant.memberWithUser.user?.nom ?: participant.memberWithUser.member.nomSaisi, color = Color.White)
            Text(participant.memberWithUser.member.role.name, style = MaterialTheme.typography.labelSmall, color = GlassWhite)
            Row(verticalAlignment = Alignment.CenterVertically) {

                if (participant.nombreParts > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = NeonCyan.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "${participant.nombreParts} PARTS",
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonCyan,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

            }
        }

        if (isRedistribution) {
            IconButton(onClick = onMoveUp) {
                Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onMoveDown) {
                Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
            }
        }
    }
}