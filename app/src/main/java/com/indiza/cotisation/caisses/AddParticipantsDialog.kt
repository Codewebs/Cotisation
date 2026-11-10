package com.indiza.cotisation.caisses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.indiza.cotisation.tontine.TontineMemberWithUser
import com.indiza.cotisation.tontinemembers.ParticipantWithParts
import com.indiza.cotisation.tools.AfroGold
import com.indiza.cotisation.tools.NeonCyan

@Composable
fun AddParticipantsDialog(
    allTontineMembers: List<TontineMemberWithUser>,
    currentParticipants: List<ParticipantWithParts>,
    canAddMembers: Boolean,
    membersThatCannotBeRemoved: Set<Long>,
    onSave: (Map<Long, Int>) -> Unit,
    isSaving: Boolean = false
) {
    val selectedParts = remember {
        mutableStateMapOf<Long, Int>().apply {
            currentParticipants.forEach { put(it.member.id, it.nombreParts) }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onSave(selectedParts.toMap()) },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Participants")
                Text(
                    "${selectedParts.size}/${allTontineMembers.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = NeonCyan
                )
            }
        },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 450.dp)) {
                items(allTontineMembers) { memberWithUser ->
                    val memberId = memberWithUser.member.id
                    val isChecked = memberId in selectedParts
                    val partsCount = selectedParts[memberId] ?: 1

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isChecked,
                            enabled = if (isChecked) {
                                memberId !in membersThatCannotBeRemoved
                            } else {
                                canAddMembers
                            },
                            onCheckedChange = {
                                if (it) selectedParts[memberId] = partsCount
                                else selectedParts.remove(memberId)
                            }
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(memberWithUser.member.nomSaisi, color = Color.White)
                            Text(memberWithUser.member.phone, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }

                        // Contrôle des parts (uniquement si coché)
                        if (isChecked) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = {
                                    if (partsCount > 1) {
                                        selectedParts[memberId] = partsCount - 1
                                    }
                                }, enabled = canAddMembers) { Icon(Icons.Default.Remove, null) }

                                Text("$partsCount", color = NeonCyan)

                                IconButton(onClick = {
                                    selectedParts[memberId] = partsCount + 1
                                }, enabled = canAddMembers) { Icon(Icons.Default.Add, null) }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(selectedParts.toMap()) },
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
            ) {
                Text(
                    if (isSaving) "ENREGISTREMENT..." else "TERMINER",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}