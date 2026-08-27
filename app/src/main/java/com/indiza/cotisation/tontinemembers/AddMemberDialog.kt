package com.indiza.cotisation.tontinemembers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.indiza.cotisation.tools.RoleDropdown
import com.indiza.cotisation.users.Role

@Composable
fun AddMemberDialog(
    onDismiss: () -> Unit,
    onConfirm: (nom: String, phone: String, role: Role) -> Unit
) {
    var nom by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var matricule by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(Role.MEMBRE) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajouter un membre") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = nom, onValueChange = { nom = it }, label = { Text("Nom complet") })
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Téléphone") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))


                Text("Rôle", style = MaterialTheme.typography.labelMedium)
                RoleDropdown(selectedRole = role, onRoleSelected = { role = it })
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(nom, phone, role) }, enabled = nom.isNotBlank()) {
                Text("Ajouter")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}