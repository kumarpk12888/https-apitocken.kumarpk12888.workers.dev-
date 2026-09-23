package com.pkfuturegkgs.hardsecurityguard.security.checks

import android.content.Context
import android.provider.Settings
import com.pkfuturegkgs.hardsecurityguard.security.SecurityCheckResult
import com.pkfuturegkgs.hardsecurityguard.security.Severity

/**
 * Reports whether USB (ADB) debugging is enabled. This is a device-wide
 * developer setting readable via Settings.Global without any special
 * permission.
 */
class UsbDebuggingCheck : SecurityCheck {
    override val id = "usb_debugging"

    override suspend fun run(context: Context): SecurityCheckResult {
        val enabled = Settings.Global.getInt(
            context.contentResolver,
            Settings.Global.ADB_ENABLED,
            0
        ) == 1

        return SecurityCheckResult(
            id = id,
            title = "USB Debugging",
            passed = !enabled,
            severity = Severity.HIGH,
            message = if (enabled) {
                "USB debugging is ON. A connected computer can install apps, pull data, and run shell commands on this device."
            } else {
                "USB debugging is off."
            },
            recommendation = if (enabled) {
                "Settings → System → Developer options → turn off USB debugging when not actively developing."
            } else null
        )
    }
}
