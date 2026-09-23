package com.pkfuturegkgs.hardsecurityguard.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.pkfuturegkgs.hardsecurityguard.data.remote.CloudflareSyncResult
import com.pkfuturegkgs.hardsecurityguard.viewmodel.SecurityViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(viewModel: SecurityViewModel) {
    val scope = rememberCoroutineScope()
    var tokenInput by remember { mutableStateOf("") }
    var statusText by remember { mutableStateOf<String?>(null) }
    var testing by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text("Cloudflare Sync", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "Optional. The Worker's API token is stored only on this device, encrypted, and is never " +
                "compiled into the app. Leave this empty to keep the app fully offline.",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(Modifier.height(16.dp))

        val masked = viewModel.getMaskedToken()
        if (masked != null) {
            Text("Current token: $masked", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = {
                viewModel.clearToken()
                statusText = "Token removed. Sync disabled."
            }) { Text("Remove Token") }
        } else {
            OutlinedTextField(
                value = tokenInput,
                onValueChange = { tokenInput = it },
                label = { Text("Worker API token") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions.Default,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            Row {
                Button(
                    enabled = tokenInput.isNotBlank() && !testing,
                    onClick = {
                        viewModel.saveToken(tokenInput.trim())
                        testing = true
                        statusText = null
                        scope.launch {
                            val result = viewModel.checkConnection()
                            testing = false
                            statusText = when (result) {
                                is CloudflareSyncResult.Success -> "Connected — token is valid."
                                is CloudflareSyncResult.HttpError -> {
                                    viewModel.clearToken()
                                    "Rejected by server (HTTP ${result.code}). Token was not saved."
                                }
                                is CloudflareSyncResult.Offline -> "Saved, but device is offline right now — will retry on next sync."
                                is CloudflareSyncResult.NotConfigured -> "Unexpected state."
                                is CloudflareSyncResult.UnknownError -> "Saved, but a connection test error occurred."
                            }
                            tokenInput = ""
                        }
                    }
                ) { Text(if (testing) "Testing…" else "Save & Test") }
            }
        }

        statusText?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(Modifier.height(24.dp))
        Text("About", style = MaterialTheme.typography.titleMedium)
        Text(
            "Hard Security Guard checks a defined set of Android-exposed security signals. " +
                "It cannot detect every possible compromise, does not collect passwords, messages, " +
                "photos, or contacts, and never claims complete protection.",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
