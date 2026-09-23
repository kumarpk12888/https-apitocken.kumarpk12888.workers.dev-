package com.pkfuturegkgs.hardsecurityguard.security.checks

import android.content.Context
import android.provider.Settings
import com.pkfuturegkgs.hardsecurityguard.security.SecurityCheckResult
import com.pkfuturegkgs.hardsecurityguard.security.Severity

/**
 * Reports whether Developer options are enabled. Not dangerous by itself,
 * but it's the gateway to USB debugging, mock locations, and other
 * settings that increase attack surface — worth flagging at a lower
 * severity than the individual risky settings it unlocks.
 */
class DeveloperOptionsCheck : SecurityCheck {
    override val id = "developer_options"

    override suspend fun run(context: Context): SecurityCheckResult {
        val enabled = Settings.Global.getInt(
            context.contentResolver,
            Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
            0
        ) == 1

        return SecurityCheckResult(
            id = id,
            title = "Developer Options",
            passed = !enabled,
            severity = Severity.MEDIUM,
            message = if (enabled) {
                "Developer options are enabled, exposing settings like USB debugging and mock locations."
            } else {
                "Developer options are off."
            },
            recommendation = if (enabled) {
                "Settings → System → Developer options → toggle off if you don't need it day-to-day."
            } else null
        )
    }
}
