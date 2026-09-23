package com.pkfuturegkgs.hardsecurityguard

import com.google.common.truth.Truth.assertThat
import com.pkfuturegkgs.hardsecurityguard.security.ScoreCalculator
import com.pkfuturegkgs.hardsecurityguard.security.SecurityCheckResult
import com.pkfuturegkgs.hardsecurityguard.security.Severity
import org.junit.Test

class ScoreCalculatorTest {

    private fun result(passed: Boolean, severity: Severity) = SecurityCheckResult(
        id = "x", title = "x", passed = passed, severity = severity, message = "x"
    )

    @Test
    fun `all checks passed yields 100`() {
        val checks = listOf(
            result(true, Severity.HIGH),
            result(true, Severity.MEDIUM),
            result(true, Severity.LOW)
        )
        assertThat(ScoreCalculator.calculate(checks)).isEqualTo(100)
    }

    @Test
    fun `each failed check deducts its severity weight`() {
        val checks = listOf(
            result(false, Severity.HIGH),   // -15
            result(true, Severity.MEDIUM),  // 0
            result(false, Severity.LOW)     // -3
        )
        assertThat(ScoreCalculator.calculate(checks)).isEqualTo(100 - 15 - 3)
    }

    @Test
    fun `score never goes below zero`() {
        val checks = List(6) { result(false, Severity.CRITICAL) } // 6 * 25 = 150 deduction
        assertThat(ScoreCalculator.calculate(checks)).isEqualTo(0)
    }

    @Test
    fun `score never exceeds 100`() {
        val checks = listOf(result(true, Severity.INFO))
        assertThat(ScoreCalculator.calculate(checks)).isEqualTo(100)
    }

    @Test
    fun `empty check list yields 100`() {
        assertThat(ScoreCalculator.calculate(emptyList())).isEqualTo(100)
    }
}
