package com.pkfuturegkgs.hardsecurityguard.security

import android.content.Context
import com.pkfuturegkgs.hardsecurityguard.security.checks.AccessibilityServiceCheck
import com.pkfuturegkgs.hardsecurityguard.security.checks.DeveloperOptionsCheck
import com.pkfuturegkgs.hardsecurityguard.security.checks.DeviceAdminCheck
import com.pkfuturegkgs.hardsecurityguard.security.checks.InstalledAppsAuditCheck
import com.pkfuturegkgs.hardsecurityguard.security.checks.PermissionAuditCheck
import com.pkfuturegkgs.hardsecurityguard.security.checks.SecurityCheck
import com.pkfuturegkgs.hardsecurityguard.security.checks.UsbDebuggingCheck
import com.pkfuturegkgs.hardsecurityguard.security.checks.VpnNetworkCheck
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Orchestrates every registered [SecurityCheck] and folds the results into
 * a [SecurityScanResult]. Adding a new check (e.g. a future native API for
 * root detection) means writing one new SecurityCheck implementation and
 * adding it to [checks] — nothing else in the app needs to change.
 *
 * IMPORTANT: This app does not and cannot claim to detect every possible
 * compromise (rootkits, kernel-level spyware, zero-days, etc). Each
 * check's message says exactly what it can and cannot see; the dashboard
 * surfaces that same language rather than a false "you are 100% safe".
 */
class SecurityScanner(
    private val checks: List<SecurityCheck> = listOf(
        UsbDebuggingCheck(),
        DeveloperOptionsCheck(),
        AccessibilityServiceCheck(),
        DeviceAdminCheck(),
        InstalledAppsAuditCheck(),
        PermissionAuditCheck(),
        VpnNetworkCheck()
    )
) {
    suspend fun scan(context: Context): SecurityScanResult = withContext(Dispatchers.Default) {
        val results = checks.map { check ->
            async { check.run(context) }
        }.awaitAll()

        SecurityScanResult(
            timestampIso = isoNow(),
            score = ScoreCalculator.calculate(results),
            checks = results
        )
    }

    private fun isoNow(): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        fmt.timeZone = TimeZone.getTimeZone("UTC")
        return fmt.format(Date())
    }
}
