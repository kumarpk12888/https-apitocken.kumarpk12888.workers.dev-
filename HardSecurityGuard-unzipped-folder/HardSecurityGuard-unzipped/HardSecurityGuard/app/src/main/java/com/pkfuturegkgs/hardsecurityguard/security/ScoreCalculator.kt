package com.pkfuturegkgs.hardsecurityguard.security

/**
 * Converts a list of check results into a single 0–100 score. Pure
 * function, no Android dependencies — kept this way specifically so it's
 * trivial to unit test (see ScoreCalculatorTest).
 */
object ScoreCalculator {
    fun calculate(checks: List<SecurityCheckResult>): Int {
        val deductions = checks.filter { !it.passed }.sumOf { it.severity.weight }
        return (100 - deductions).coerceIn(0, 100)
    }
}
