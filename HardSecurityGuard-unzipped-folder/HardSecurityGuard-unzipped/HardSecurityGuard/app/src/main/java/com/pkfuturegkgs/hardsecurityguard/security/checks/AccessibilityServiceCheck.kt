package com.pkfuturegkgs.hardsecurityguard.security.checks

import android.content.Context
import android.provider.Settings
import com.pkfuturegkgs.hardsecurityguard.security.SecurityCheckResult
import com.pkfuturegkgs.hardsecurityguard.security.Severity

/**
 * Lists which Accessibility Services are currently enabled. Accessibility
 * services are a common vector for banking-trojan-style malware because
 * the API lets an enabled service read screen content and simulate taps
 * across other apps. We can only see *which services are turned on*, not
 * judge whether each one is malicious — that judgement is left to the
 * user, which we say explicitly in the message.
 */
class AccessibilityServiceCheck : SecurityCheck {
    override val id = "accessibility_services"

    override suspend fun run(context: Context): SecurityCheckResult {
        val raw = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        val services = raw?.split(':')?.filter { it.isNotBlank() } ?: emptyList()

        return SecurityCheckResult(
            id = id,
            title = "Accessibility Services",
            passed = services.isEmpty(),
            severity = Severity.HIGH,
            message = if (services.isEmpty()) {
                "No accessibility services are currently enabled."
            } else {
                "${services.size} accessibility service(s) enabled: ${services.joinToString()}. " +
                    "Any of these can read screen content and perform actions on your behalf."
            },
            recommendation = if (services.isNotEmpty()) {
                "Settings → Accessibility → review each enabled service and turn off anything you don't recognize or actively use."
            } else null,
            limited = services.isNotEmpty() // we list them but can't assess intent/safety automatically
        )
    }
}
