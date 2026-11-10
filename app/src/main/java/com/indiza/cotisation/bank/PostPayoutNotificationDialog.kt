package com.indiza.cotisation.bank

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.indiza.cotisation.tontine.TontineMemberWithUser
import com.indiza.cotisation.tools.NeonCyan

enum class NotificationAudience {
    BENEFICIARIES_ONLY,
    ALL_MEMBERS
}

@Composable
fun PostPayoutNotificationDialog(
    members: List<TontineMemberWithUser>,
    beneficiaryIds: Set<Long> = emptySet(),
    isOnline: Boolean,
    onDismiss: () -> Unit,
    onSend: (selectedMembers: List<TontineMemberWithUser>, sendSms: Boolean, sendNotification: Boolean) -> Unit
) {
    val initialBeneficiaries = remember(members, beneficiaryIds) {
        if (beneficiaryIds.isNotEmpty()) beneficiaryIds
        else members.take(1).map { it.member.id }.toSet()
    }

    var audienceMode by remember { mutableStateOf(NotificationAudience.BENEFICIARIES_ONLY) }
    var selectedIds by remember { mutableStateOf(initialBeneficiaries) }
    var sendSms by remember { mutableStateOf(false) }
    var sendNotification by remember { mutableStateOf(isOnline) }

    LaunchedEffect(audienceMode) {
        selectedIds = when (audienceMode) {
            NotificationAudience.BENEFICIARIES_ONLY -> initialBeneficiaries
            NotificationAudience.ALL_MEMBERS -> members.map { it.member.id }.toSet()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Notifier les membres", fontWeight = FontWeight.Bold, color = Color.White) },
        containerColor = Color(0xFF1A1A1A),
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Souhaitez-vous informer les membres de cette remise de cagnotte ?", color = Color.White)
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = sendSms, 
                        onCheckedChange = { sendSms = it },
                        colors = CheckboxDefaults.colors(checkedColor = NeonCyan)
                    )
                    Text("Envoyer par SMS", color = Color.White)
                }
                
                if (isOnline) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = sendNotification, 
                            onCheckedChange = { sendNotification = it },
                            colors = CheckboxDefaults.colors(checkedColor = NeonCyan)
                        )
                        Text("Notification Push & Reçu numérique", color = Color.White)
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                Text("Audience à notifier :", style = MaterialTheme.typography.labelMedium, color = NeonCyan)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { audienceMode = NotificationAudience.BENEFICIARIES_ONLY },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = audienceMode == NotificationAudience.BENEFICIARIES_ONLY,
                        onClick = { audienceMode = NotificationAudience.BENEFICIARIES_ONLY },
                        colors = RadioButtonDefaults.colors(selectedColor = NeonCyan)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("Bénéficiaire(s) uniquement (Par défaut)", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("Seul le récipiendaire de la cagnotte est notifié", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { audienceMode = NotificationAudience.ALL_MEMBERS },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = audienceMode == NotificationAudience.ALL_MEMBERS,
                        onClick = { audienceMode = NotificationAudience.ALL_MEMBERS },
                        colors = RadioButtonDefaults.colors(selectedColor = NeonCyan)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("Tous les membres de la tontine", color = Color.White, fontWeight = FontWeight.Normal)
                        Text("Informer l'ensemble du groupe", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                
                Text("Membres sélectionnés :", style = MaterialTheme.typography.labelMedium, color = NeonCyan)
                
                LazyColumn(modifier = Modifier.heightIn(max = 140.dp)) {
                    items(members) { member ->
                        val isBeneficiary = initialBeneficiaries.contains(member.member.id)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = selectedIds.contains(member.member.id),
                                onCheckedChange = { checked ->
                                    val current = selectedIds.toMutableSet()
                                    if (checked) current.add(member.member.id) else current.remove(member.member.id)
                                    selectedIds = current
                                },
                                colors = CheckboxDefaults.colors(checkedColor = NeonCyan)
                            )
                            Text(
                                text = (member.user?.nom ?: member.member.nomSaisi) + if (isBeneficiary) " (Bénéficiaire)" else "",
                                color = if (isBeneficiary) NeonCyan else Color.White,
                                fontWeight = if (isBeneficiary) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val selected = members.filter { selectedIds.contains(it.member.id) }
                    onSend(selected, sendSms, sendNotification)
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
            ) {
                Text("Envoyer", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Passer", color = Color.White)
            }
        }
    )
}
