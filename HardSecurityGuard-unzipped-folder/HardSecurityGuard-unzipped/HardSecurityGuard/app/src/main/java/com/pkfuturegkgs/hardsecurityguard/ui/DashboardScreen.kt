package com.pkfuturegkgs.hardsecurityguard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pkfuturegkgs.hardsecurityguard.security.SecurityCheckResult
import com.pkfuturegkgs.hardsecurityguard.security.Severity
import com.pkfuturegkgs.hardsecurityguard.ui.theme.HsgAccentGreen
import com.pkfuturegkgs.hardsecurityguard.ui.theme.HsgAccentRed
import com.pkfuturegkgs.hardsecurityguard.ui.theme.HsgAccentAmber
import com.pkfuturegkgs.hardsecurityguard.viewmodel.ScanUiState
import com.pkfuturegkgs.hardsecurityguard.viewmodel.SecurityViewModel

@Composable
fun DashboardScreen(viewModel: SecurityViewModel, onOpenEmergency: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Text("Hard Security Guard", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Local, offline-first Android security checks.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )

        Spacer(Modifier.height(24.dp))

        when (val state = uiState) {
            is ScanUiState.Idle -> IdleCard(onScan = viewModel::runScan)
            is ScanUiState.Scanning -> ScanningCard()
            is ScanUiState.Done -> ScanResultView(
                score = state.result.score,
                checks = state.result.checks,
                onRescan = viewModel::runScan
            )
            is ScanUiState.Error -> ErrorCard(state.message, onRetry = viewModel::runScan)
        }

        Spacer(Modifier.height(16.dp))

        OutlinedButton(
            onClick = onOpenEmergency,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = HsgAccentRed)
        ) {
            Icon(Icons.Filled.Warning, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Emergency Security Screen")
        }

        Spacer(Modifier.height(12.dp))
        Text(
            "This app cannot detect every possible compromise (e.g. kernel-level spyware or zero-day exploits). " +
                "It checks a defined set of Android-exposed signals and explains what it can't see.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun IdleCard(onScan: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("No scan yet", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            Button(onClick = onScan) { Text("Run Security Scan") }
        }
    }
}

@Composable
private fun ScanningCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
            Spacer(Modifier.height(12.dp))
            Text("Scanning device…")
        }
    }
}

@Composable
private fun ErrorCard(message: String, onRetry: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Text("Scan failed", color = HsgAccentRed, style = MaterialTheme.typography.titleMedium)
            Text(message, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            Button(onClick = onRetry) { Text("Retry") }
        }
    }
}

@Composable
private fun ScanResultView(score: Int, checks: List<SecurityCheckResult>, onRescan: () -> Unit) {
    Column {
        ScoreGauge(score)
        Spacer(Modifier.height(20.dp))
        Text("Findings", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
            items(checks) { check -> CheckRow(check) }
        }
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = onRescan, modifier = Modifier.fillMaxWidth()) {
            Text("Scan Again")
        }
    }
}

@Composable
private fun ScoreGauge(score: Int) {
    val color = when {
        score >= 80 -> HsgAccentGreen
        score >= 50 -> HsgAccentAmber
        else -> HsgAccentRed
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text("$score", style = MaterialTheme.typography.headlineMedium, color = color, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text("Security Score", style = MaterialTheme.typography.titleMedium)
            Text(
                when {
                    score >= 80 -> "Good — minor or no issues found."
                    score >= 50 -> "Fair — some settings need attention."
                    else -> "Weak — review the findings below."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun CheckRow(check: SecurityCheckResult) {
    val color = if (check.passed) HsgAccentGreen else when (check.severity) {
        Severity.CRITICAL, Severity.HIGH -> HsgAccentRed
        Severity.MEDIUM -> HsgAccentAmber
        else -> Color.Gray
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = if (check.passed) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(check.title, style = MaterialTheme.typography.titleMedium)
                Text(check.message, style = MaterialTheme.typography.bodyMedium)
                if (check.limited) {
                    Text(
                        "ℹ Limited by Android API visibility rules.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                    )
                }
                check.recommendation?.let {
                    Spacer(Modifier.height(4.dp))
                    Text("Fix: $it", style = MaterialTheme.typography.labelSmall, color = color)
                }
            }
        }
    }
}
