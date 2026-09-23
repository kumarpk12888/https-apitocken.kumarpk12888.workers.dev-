package com.pkfuturegkgs.hardsecurityguard.security.checks

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.pkfuturegkgs.hardsecurityguard.security.SecurityCheckResult
import com.pkfuturegkgs.hardsecurityguard.security.Severity

/**
 * Audits installed apps for a basic signal: how many come from outside
 * Google Play (sideloaded / unknown installer).
 *
 * IMPORTANT ANDROID API LIMITATION:
 * Since Android 11 (API 30), apps cannot see the full installed-app list
 * unless they hold the QUERY_ALL_PACKAGES permission, which Google Play
 * restricts to a narrow set of use cases (this app does not qualify, and
 * requesting it would go against the "minimum permissions" requirement).
 * Instead this app declares a <queries> filter for the LAUNCHER intent,
 * which only reveals apps that show up on the home screen / app drawer.
 * Background-only apps, system components, and services are NOT visible
 * to this check. This is disclosed to the user in the check's message.
 */
class InstalledAppsAuditCheck : SecurityCheck {
    override val id = "installed_apps_audit"

    override suspend fun run(context: Context): SecurityCheckResult {
        val pm = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)

        val launchableApps = pm.queryIntentActivities(launcherIntent, 0)
            .map { it.activityInfo.packageName }
            .distinct()

        val sideloaded = launchableApps.filter { pkg ->
            val installer = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    pm.getInstallSourceInfo(pkg).installingPackageName
                } else {
                    @Suppress("DEPRECATION")
                    pm.getInstallerPackageName(pkg)
                }
            } catch (e: Exception) {
                null
            }
            installer != "com.android.vending"
        }

        val hasConcern = sideloaded.size > 3 // small tolerance for F-Droid/manual installs

        return SecurityCheckResult(
            id = id,
            title = "Installed App Audit",
            passed = !hasConcern,
            severity = Severity.MEDIUM,
            message = "${launchableApps.size} launchable app(s) visible to this scan; " +
                "${sideloaded.size} were not installed via Google Play. " +
                "Note: Android hides background/system apps from this kind of scan unless a special, " +
                "Play-restricted permission is granted — this app deliberately does not request it.",
            recommendation = if (hasConcern) {
                "Review apps not installed via Play Store and remove anything you don't recognize."
            } else null,
            limited = true
        )
    }
}
