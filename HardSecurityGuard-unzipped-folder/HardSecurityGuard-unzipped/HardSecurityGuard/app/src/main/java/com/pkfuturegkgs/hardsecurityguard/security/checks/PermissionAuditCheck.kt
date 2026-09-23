package com.pkfuturegkgs.hardsecurityguard.security.checks

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.pkfuturegkgs.hardsecurityguard.security.SecurityCheckResult
import com.pkfuturegkgs.hardsecurityguard.security.Severity

/**
 * Counts how many *other, launchable* apps hold sensitive/dangerous
 * permissions (camera, microphone, location, contacts, SMS). Same
 * visibility limitation as [InstalledAppsAuditCheck] applies: only apps
 * visible via the LAUNCHER <queries> filter are inspected.
 */
class PermissionAuditCheck : SecurityCheck {
    override val id = "permission_audit"

    private val sensitivePermissions = setOf(
        android.Manifest.permission.CAMERA,
        android.Manifest.permission.RECORD_AUDIO,
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.READ_CONTACTS,
        android.Manifest.permission.READ_SMS,
        android.Manifest.permission.READ_CALL_LOG
    )

    override suspend fun run(context: Context): SecurityCheckResult {
        val pm = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val packages = pm.queryIntentActivities(launcherIntent, 0)
            .map { it.activityInfo.packageName }
            .distinct()
            .filter { it != context.packageName }

        var heavyPermissionApps = 0
        for (pkg in packages) {
            try {
                val info = pm.getPackageInfo(pkg, PackageManager.GET_PERMISSIONS)
                val granted = info.requestedPermissions
                    ?.filterIndexed { i, _ ->
                        (info.requestedPermissionsFlags?.getOrNull(i) ?: 0) and
                            PackageManager.PERMISSION_GRANTED != 0
                    }
                    ?: emptyList()
                val sensitiveCount = granted.count { it in sensitivePermissions }
                if (sensitiveCount >= 3) heavyPermissionApps++
            } catch (e: Exception) {
                // Package uninstalled mid-scan or info unavailable — skip it.
            }
        }

        return SecurityCheckResult(
            id = id,
            title = "Sensitive Permission Audit",
            passed = heavyPermissionApps == 0,
            severity = Severity.MEDIUM,
            message = if (heavyPermissionApps == 0) {
                "No launchable app holds an unusually broad set of sensitive permissions."
            } else {
                "$heavyPermissionApps app(s) hold 3+ sensitive permissions (camera/mic/location/contacts/SMS/call log)."
            },
            recommendation = if (heavyPermissionApps > 0) {
                "Settings → Privacy → Permission manager → review and revoke permissions apps don't need."
            } else null,
            limited = true
        )
    }
}
