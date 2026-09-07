package com.indiza.cotisation.tontinemembers

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.indiza.cotisation.tools.NeonCyan
import com.indiza.cotisation.tools.RoleDropdown
import com.indiza.cotisation.users.Role

@Composable
fun EditMemberDialog(
    member: MemberUiModel,
    takenUniqueRoles: Set<Role>,
    onDismiss: () -> Unit,
    onConfirm: (nom: String, phone: String, role: Role) -> Unit,
    onMapRequest: () -> Unit
) {
    var nom by remember { mutableStateOf(member.displayName) }
    var phone by remember { mutableStateOf(member.phoneNumber) } 
    var role by remember { mutableStateOf(member.role) }

    val availableRoles = remember(takenUniqueRoles, member.role) {
        Role.values().filter { 
            !it.isUnique || !takenUniqueRoles.contains(it) || it == Role.MEMBRE || it == member.role 
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modifier le membre") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = nom, onValueChange = { nom = it }, label = { Text("Nom complet") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Téléphone") }, modifier = Modifier.fillMaxWidth())

                Text("Rôle", style = MaterialTheme.typography.labelMedium)
                RoleDropdown(
                    selectedRole = role,
                    availableRoles = availableRoles,
                    onRoleSelected = { role = it }
                )

                if (!member.isLinked) {
                    OutlinedButton(
                        onClick = onMapRequest,
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, NeonCyan)
                    ) {
                        Icon(Icons.Default.Link, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Mapper avec une demande")
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(nom, phone, role) }) {
                Text("Enregistrer")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}