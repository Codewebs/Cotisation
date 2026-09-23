package com.indiza.cotisation.tontine

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.indiza.cotisation.sync.SyncStatus
import com.indiza.cotisation.users.Permission

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TontineItem(
    tontineWithCaisses: TontineWithCaissesAndRole,
    onClick: () -> Unit,
    onEditClick: (Long) -> Unit,
    onDeleteClick: (Long) -> Unit
) {
    val tontine = tontineWithCaisses.tontine
    val caisses = tontineWithCaisses.caisses
    val role = tontineWithCaisses.role

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .shadow(4.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = tontine.exercice?.uppercase() ?: "EXERCICE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.secondary)
                        ) {
                            Text(
                                text = role.name.replace("_", " "),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            color = (if (tontine.isOnline) Color(0xFFE0F7FA) else Color(0xFFF5F5F5)),
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(0.5.dp, if (tontine.isOnline) Color(0xFF00BCD4) else Color.Gray)
                        ) {
                            Text(
                                text = if (tontine.isOnline) "EN LIGNE" else "LOCAL",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (tontine.isOnline) Color(0xFF0097A7) else Color.Gray,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = tontine.libelle.uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row {
                    if (role.permissions.contains(Permission.EDIT_TONTINE_RULES)) {
                        IconButton(onClick = { onEditClick(tontine.id) }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                    }
                    if (role.permissions.contains(Permission.DELETE_TONTINE)) {
                        IconButton(onClick = { onDeleteClick(tontine.id) }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.DeleteSweep, null, tint = Color.Red, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                caisses.take(4).forEach { caisse ->
                    Surface(
                        color = MaterialTheme.colorScheme.outline.copy(0.05f),
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(0.5.dp, Color.Gray.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = caisse.type.name.lowercase(),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = Color.DarkGray
                        )
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${caisses.size} CAISSES ACTIVES",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (tontine.syncStatus == SyncStatus.SYNCED)
                        Icons.Default.CloudDone else Icons.Default.CloudQueue,
                    contentDescription = null,
                    tint = when(tontine.syncStatus) {
                        SyncStatus.SYNCED -> Color(0xFF4CAF50)
                        SyncStatus.PENDING_CREATE, SyncStatus.PENDING_UPDATE -> Color(0xFFFF9800) // Orange pour attente
                        SyncStatus.ERROR -> Color.Red
                        else -> Color.Gray
                    },
                    modifier = Modifier.size(16.dp)
                )
                if (tontine.syncStatus != SyncStatus.SYNCED) {
                    Text(
                        text = tontine.syncStatus.name.replace("PENDING_", ""),
                        fontSize = 7.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.outline)
            }
        }
    }
}
