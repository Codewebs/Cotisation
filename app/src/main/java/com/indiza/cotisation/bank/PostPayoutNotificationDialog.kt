package com.indiza.cotisation.bank

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

@Composable
fun PostPayoutNotificationDialog(
    members: List<TontineMemberWithUser>,
    isOnline: Boolean,
    onDismiss: () -> Unit,
    onSend: (selectedMembers: List<TontineMemberWithUser>, sendSms: Boolean, sendNotification: Boolean) -> Unit
) {
    var selectedIds by remember { mutableStateOf(members.map { it.member.id }.toSet()) }
    var sendSms by remember { mutableStateOf(true) }
    var sendNotification by remember { mutableStateOf(isOnline) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Notifier les membres", fontWeight = FontWeight.Bold, color = Color.White) },
        containerColor = Color(0xFF1A1A1A),
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                
                Text("Membres à notifier :", style = MaterialTheme.typography.labelMedium, color = NeonCyan)
                
                LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                    items(members) { member ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
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
                            Text(member.user?.nom ?: member.member.nomSaisi, color = Color.White)
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
