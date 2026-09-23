package com.pkfuturegkgs.hardsecurityguard.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pkfuturegkgs.hardsecurityguard.viewmodel.SecurityViewModel

@Composable
fun HistoryScreen(viewModel: SecurityViewModel) {
    val history by viewModel.history.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Scan History", style = MaterialTheme.typography.headlineMedium)
            TextButton(onClick = viewModel::clearHistory) { Text("Clear") }
        }
        Spacer(Modifier.height(12.dp))

        if (history.isEmpty()) {
            Text(
                "No scans yet. Run a scan from the Dashboard tab.",
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            LazyColumn {
                items(history) { entry ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(Modifier.padding(14.dp)) {
                            Text(entry.timestampIso, style = MaterialTheme.typography.labelSmall)
                            Text("Score: ${entry.score}", style = MaterialTheme.typography.titleMedium)
                            Text(
                                if (entry.synced) "Synced to Cloudflare" else "Not synced",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }
    }
}
