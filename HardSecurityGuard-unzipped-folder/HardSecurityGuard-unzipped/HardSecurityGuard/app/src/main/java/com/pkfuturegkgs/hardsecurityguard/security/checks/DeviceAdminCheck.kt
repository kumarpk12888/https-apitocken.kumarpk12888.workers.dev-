package com.pkfuturegkgs.hardsecurityguard.security.checks

import android.app.admin.DevicePolicyManager
import android.content.Context
import com.pkfuturegkgs.hardsecurityguard.security.SecurityCheckResult
import com.pkfuturegkgs.hardsecurityguard.security.Severity

/**
 * Lists apps granted Device Administrator rights on this device. Device
 * admin is legitimately used by MDM/work-profile and some anti-theft
 * apps, but it's also a classic persistence mechanism for malware (it
 * makes the app harder to uninstall). We only enumerate active admins on
 * this device — there is no API to inspect other devices or other users.
 */
class DeviceAdminCheck : SecurityCheck {
    override val id = "device_admin"

    override suspend fun run(context: Context): SecurityCheckResult {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
        val admins = dpm?.activeAdmins?.map { it.packageName } ?: emptyList()

        return SecurityCheckResult(
            id = id,
            title = "Device Administrator Apps",
            passed = admins.isEmpty(),
            severity = Severity.HIGH,
            message = if (admins.isEmpty()) {
                "No apps currently hold Device Administrator rights."
            } else {
                "${admins.size} app(s) hold Device Administrator rights: ${admins.joinToString()}."
            },
            recommendation = if (admins.isNotEmpty()) {
                "Settings → Security → Device admin apps → remove rights from anything you don't recognize as a work/MDM or anti-theft app."
            } else null
        )
    }
}
