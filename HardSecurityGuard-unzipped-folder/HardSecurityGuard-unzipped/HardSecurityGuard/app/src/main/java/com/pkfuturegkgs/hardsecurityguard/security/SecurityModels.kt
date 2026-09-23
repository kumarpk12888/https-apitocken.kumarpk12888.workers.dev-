package com.pkfuturegkgs.hardsecurityguard.security

enum class Severity(val weight: Int) {
    CRITICAL(25),
    HIGH(15),
    MEDIUM(8),
    LOW(3),
    INFO(0)
}

data class SecurityCheckResult(
    val id: String,
    val title: String,
    val passed: Boolean,
    val severity: Severity,
    val message: String,
    val recommendation: String? = null,
    val limited: Boolean = false
)

data class SecurityScanResult(
    val timestampIso: String,
    val score: Int,
    val checks: List<SecurityCheckResult>
) {
    val warnings: List<SecurityCheckResult> get() = checks.filter { !it.passed }
}
