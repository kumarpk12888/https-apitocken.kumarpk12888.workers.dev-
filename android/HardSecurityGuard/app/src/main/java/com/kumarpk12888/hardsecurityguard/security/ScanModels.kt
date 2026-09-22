package com.kumarpk12888.hardsecurityguard

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun formatTime(epochMs: Long): String =
    SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(epochMs))

data class SecurityCheck(
    val title: String,
    val status: String,
    val details: String,
    val explanation: String,
    val riskLevel: String = "info"
)

data class InstalledAppAudit(
    val appName: String,
    val packageName: String,
    val sensitivePermissions: List<String>
)

data class NetworkStatus(
    val connectionType: String,
    val internetAvailable: Boolean,
    val vpnEnabled: Boolean,
    val proxyConfigured: Boolean,
    val privateDnsActive: String,
    val networkSummary: String
)

data class SecurityScanResult(
    val score: Int = 100,
    val status: String = "Good",
    val passedChecks: Int = 0,
    val warnings: Int = 0,
    val scanTime: Long = System.currentTimeMillis(),
    val summary: String = "",
    val checks: List<SecurityCheck> = emptyList(),
    val appAudit: List<InstalledAppAudit> = emptyList(),
    val permissionAudit: List<PermissionAudit> = emptyList(),
    val networkStatus: NetworkStatus = NetworkStatus(
        connectionType = "Unknown",
        internetAvailable = false,
        vpnEnabled = false,
        proxyConfigured = false,
        privateDnsActive = "Not available",
        networkSummary = "Connectivity information unavailable"
    ),
    val runTimeSummary: String = "No scan data yet."
) {
    val formattedTime: String
        get() = formatTime(scanTime)
}

data class PermissionAudit(
    val permissionName: String,
    val granted: Boolean,
    val risk: String,
    val explanation: String
)

