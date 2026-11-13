package com.radioauto.es.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.radioauto.es.BuildConfig

/**
 * Diálogo composable para configurar la conexión con el servidor Mock local y opciones de red
 *
 * @param currentUrl URL actual del endpoint API
 * @param isForceOffline Estado del switch de modo offline
 * @param onDismiss Callback al cerrar el diálogo sin guardar
 * @param onSave Callback con los nuevos valores guardados
 */
@Composable
fun SettingsDialog(
    currentUrl: String,
    isForceOffline: Boolean,
    onDismiss: () -> Unit,
    onSave: (newUrl: String, forceOffline: Boolean) -> Unit
) {
    var urlText by remember { mutableStateOf(currentUrl) }
    var offlineChecked by remember { mutableStateOf(isForceOffline) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Configuración del Servidor",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Dirección del catálogo JSON (Servidor Mock Docker en puerto 4112):",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = urlText,
                    onValueChange = { urlText = it },
                    label = { Text("URL del API") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(
                            onClick = { urlText = BuildConfig.DEFAULT_API_URL }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Restablecer URL por defecto"
                            )
                        }
                    }
                )

                Text(
                    text = "• Emulador Android: http://10.0.2.2:4112/api/stations\n• Coche / WiFi: http://[IP_DE_TU_PC]:4112/api/stations",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Usar sólo catálogo local",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Usa assets/stations.json sin consultar la red",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Switch(
                        checked = offlineChecked,
                        onCheckedChange = { offlineChecked = it }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(urlText.trim(), offlineChecked)
                }
            ) {
                Text("Guardar y Aplicar")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
