package com.kumarpk12888.hardsecurityguard.security

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.admin.DevicePolicyManager
import android.app.usage.UsageStatsManager
import android.content.ContentResolver
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import androidx.core.content.getSystemService
import java.security.KeyStore
import javax.crypto.KeyGenerator
import kotlin.math.max

private const val TAG = "LocalSecurityScanner"

class LocalSecurityScanner(private val context: Context) {
    fun scan(): SecurityScanResult {
        val cr: ContentResolver = context.contentResolver
        val pm: PackageManager = context.packageManager
        val connectivityManager = context.getSystemService<ConnectivityManager>()
        val devicePolicyManager = context.getSystemService<DevicePolicyManager>()
        val checks = mutableListOf<SecurityCheck>()
        var score = 100
        var warningCount = 0
        var passedCount = 0

        fun addCheck(
            title: String,
            status: String,
            details: String,
            explanation: String,
            riskLevel: String = "info",
            deduction: Int = 0
        ) {
            if (status == "passed") passedCount++
            else if (status == "warning" || status == "review") warningCount++
            checks.add(SecurityCheck(title, status, details, explanation, riskLevel))
            if (deduction > 0) score = max(0, score - deduction)
        }

        val adbEnabled = try {
            Settings.Global.getInt(cr, Settings.Global.ADB_ENABLED, 0) == 1
        } catch (e: Exception) {
            false
        }

        if (adbEnabled) {
            addCheck(
                title = "USB debugging / ADB",
                status = "warning",
                details = "USB debugging is enabled.",
                explanation = "USB debugging exposes developer-level access to a connected device. It can increase risk if the device is compromised or connected to an untrusted computer.",
                riskLevel = "warning",
                deduction = 15
            )
        } else {
            addCheck(
                title = "USB debugging / ADB",
                status = "passed",
                details = "USB debugging is not enabled.",
                explanation = "This setting is not currently active, which reduces the chance of unintended developer access.",
                riskLevel = "safe"
            )
        }

        val developerModeEnabled = try {
            Settings.Global.getInt(cr, "development_settings_enabled", 0) == 1
        } catch (e: Exception) {
            false
        }

        if (developerModeEnabled) {
            addCheck(
                title = "Developer options",
                status = "review",
                details = "Developer options are enabled.",
                explanation = "Developer options can expose debugging and testing capabilities that are not intended for everyday use.",
                riskLevel = "review",
                deduction = 10
            )
        } else {
            addCheck(
                title = "Developer options",
                status = "passed",
                details = "Developer options are disabled.",
                explanation = "This reduces the ability to expose debugging or special-device settings.",
                riskLevel = "safe"
            )
        }

        val accessibilityManager = context.getSystemService<android.view.accessibility.AccessibilityManager>()
        val enabledServices = accessibilityManager?.getEnabledAccessibilityServiceList(
            AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        ) ?: emptyList()

        if (enabledServices.isNotEmpty()) {
            val serviceNames = enabledServices.mapNotNull { it.id ?: it.packageName }.distinct().joinToString(", ")
            addCheck(
                title = "Accessibility services",
                status = "warning",
                details = "Enabled accessibility services detected: $serviceNames",
                explanation = "Accessibility services can observe user interaction, read screen content, and trigger actions. This sensitivity makes them a high-value access point if misused.",
                riskLevel = "warning",
                deduction = 12
            )
        } else {
            addCheck(
                title = "Accessibility services",
                status = "passed",
                details = "No enabled accessibility services were detected by the Android accessibility manager.",
                explanation = "This reduces the chance of screen-level observation beyond what the app itself is permitted to do.",
                riskLevel = "safe"
            )
        }

        val activeAdmins = devicePolicyManager?.activeAdmins ?: emptyList()
        if (activeAdmins.isNotEmpty()) {
            val adminNames = activeAdmins.joinToString(", ") { it.flattenToShortString() }
            addCheck(
                title = "Device administrator",
                status = "review",
                details = "Active device administrators detected: $adminNames",
                explanation = "Device administrator apps can enforce security policies and can be powerful. They should be reviewed for legitimate purpose and trust.",
                riskLevel = "review",
                deduction = 20
            )
        } else {
            addCheck(
                title = "Device administrator",
                status = "passed",
                details = "No active device administrators are currently enabled.",
                explanation = "This indicates that no app is currently managing device security policies from the system level.",
                riskLevel = "safe"
            )
        }

        val keyguardManager = context.getSystemService<android.app.KeyguardManager>()
        val deviceSecured = keyguardManager?.isDeviceSecure == true
        if (deviceSecured) {
            addCheck(
                title = "Screen lock / security",
                status = "passed",
                details = "The device reports a secure screen lock or equivalent protection.",
                explanation = "This helps reduce unauthorized physical access to the phone and the app data it contains.",
                riskLevel = "safe"
            )
        } else {
            addCheck(
                title = "Screen lock / security",
                status = "warning",
                details = "No secure screen lock was detected.",
                explanation = "This leaves the device more exposed to physical access. Enabling a PIN, pattern, or biometric lock reduces this risk.",
                riskLevel = "warning",
                deduction = 10
            )
        }

        val encryptionStatus = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            devicePolicyManager?.storageEncryptionStatus ?: -1
        } else {
            -1
        }
        if (encryptionStatus == DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE ||
            (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
        ) {
            addCheck(
                title = "Encryption",
                status = "passed",
                details = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    "Storage encryption is active."
                } else {
                    "This Android version does not expose a detailed storage encryption status to the app."
                },
                explanation = "Encryption helps protect data if the device is lost or the storage is accessed without the user’s login state.",
                riskLevel = "safe"
            )
        } else {
            addCheck(
                title = "Encryption",
                status = "warning",
                details = "No active device encryption state was reported.",
                explanation = "A device without active encryption exposes more data if the storage is accessed outside the normal lock-screen flow.",
                riskLevel = "warning",
                deduction = 10
            )
        }

        val installUnknownSources = try {
            Settings.Secure.getInt(cr, Settings.Secure.INSTALL_NON_MARKET_APPS, 0) == 1
        } catch (e: Exception) {
            false
        }

        if (installUnknownSources) {
            addCheck(
                title = "Unknown source installs",
                status = "warning",
                details = "Unknown app installations are allowed.",
                explanation = "This setting can allow apps from untrusted sources to be installed, which increases the chance of malware or phishing apps.",
                riskLevel = "warning",
                deduction = 10
            )
        } else {
            addCheck(
                title = "Unknown source installs",
                status = "passed",
                details = "Unknown app installations are not allowed.",
                explanation = "This reduces the chance of rogue apps being installed from untrusted sources.",
                riskLevel = "safe"
            )
        }

        val networkCapabilities = connectivityManager?.getNetworkCapabilities(connectivityManager.activeNetwork)
        val internetAvailable = networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
            networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

        val connectionType = when {
            networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "Wi‑Fi"
            networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "Mobile"
            networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true -> "VPN"
            else -> "Unknown"
        }

        val vpnEnabled = networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
        val proxyConfigured = false
        val privateDnsActive = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Settings.Global.getString(cr, "private_dns_mode") ?: "Not available"
        } else {
            "Not available on this Android version"
        }

        addCheck(
            title = "Network connectivity",
            status = if (internetAvailable) "passed" else "warning",
            details = "Connection type: $connectionType. Internet validated: ${if (internetAvailable) "yes" else "no"}.",
            explanation = if (internetAvailable) {
                "The device appears to have a working Internet connection for normal network traffic."
            } else {
                "This app could not confirm that the device currently has a validated Internet connection."
            },
            riskLevel = if (internetAvailable) "safe" else "warning",
            deduction = if (internetAvailable) 0 else 8
        )

        addCheck(
            title = "VPN status",
            status = if (vpnEnabled) "review" else "passed",
            details = if (vpnEnabled) "VPN transport detected." else "No VPN transport was detected.",
            explanation = if (vpnEnabled) {
                "A VPN can provide privacy or security for traffic, but it can also hide activity from local network checks. It should be used intentionally and configured correctly."
            } else {
                "No active VPN transport was visible to the app."
            },
            riskLevel = if (vpnEnabled) "review" else "safe",
            deduction = if (vpnEnabled) 5 else 0
        )

        val installedApps = mutableListOf<InstalledAppAudit>()
        val appInfoList = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        for (appInfo in appInfoList.sortedBy { it.loadLabel(pm).toString() }) {
            val label = appInfo.loadLabel(pm)?.toString() ?: appInfo.packageName
            val packageName = appInfo.packageName
            val requestedPermissions = try {
                pm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS).requestedPermissions
                    ?.filter { it in SENSITIVE_PERMISSIONS }
                    ?: emptyList()
            } catch (_: Exception) {
                emptyList()
            }

            if (requestedPermissions.isNotEmpty()) {
                installedApps.add(
                    InstalledAppAudit(
                        appName = label,
                        packageName = packageName,
                        sensitivePermissions = requestedPermissions
                    )
                )
            }
        }

        val permissionAudit = mutableListOf<PermissionAudit>()
        for (permission in SENSITIVE_PERMISSIONS) {
            val granted = context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
            val risk = when (permission) {
                Manifest.permission.CAMERA -> "High"
                Manifest.permission.RECORD_AUDIO -> "High"
                Manifest.permission.ACCESS_FINE_LOCATION -> "High"
                Manifest.permission.READ_CONTACTS -> "Medium"
                Manifest.permission.READ_PHONE_STATE -> "High"
                Manifest.permission.SEND_SMS -> "High"
                Manifest.permission.POST_NOTIFICATIONS -> "Medium"
                Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.READ_MEDIA_IMAGES -> "Medium"
                else -> "Medium"
            }
            val explanation = if (granted) {
                "This permission is granted to the app. It may be required for a feature and should be used only for the feature it is needed for."
            } else {
                "This permission is not currently granted. The app should not request it unless a clearly justified feature needs it."
            }
            permissionAudit.add(PermissionAudit(permission, granted, risk, explanation))
        }

        val overallStatus = when {
            score >= 85 -> "Good"
            score >= 70 -> "Review Needed"
            else -> "High Risk"
        }

        val summary = buildString {
            append("$passedCount passed checks, $warningCount warnings. ")
            append("Status: $overallStatus. ")
            append("The score is an indicator based on Android security settings visible to this app.")
        }

        return SecurityScanResult(
            score = score,
            status = overallStatus,
            passedChecks = passedCount,
            warnings = warningCount,
            scanTime = System.currentTimeMillis(),
            summary = summary,
            checks = checks,
            appAudit = installedApps,
            permissionAudit = permissionAudit,
            networkStatus = NetworkStatus(
                connectionType = connectionType,
                internetAvailable = internetAvailable,
                vpnEnabled = vpnEnabled,
                proxyConfigured = proxyConfigured,
                privateDnsActive = privateDnsActive,
                networkSummary = "Connection: $connectionType; internet:$internetAvailable; vpn:$vpnEnabled"
            ),
            runTimeSummary = "This security score is an indicator based on Android security settings available to this application. It does not prove that the device has been hacked or is completely secure."
        )
    }

    companion object {
        private val SENSITIVE_PERMISSIONS = listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.SEND_SMS,
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO
        )
    }
}
