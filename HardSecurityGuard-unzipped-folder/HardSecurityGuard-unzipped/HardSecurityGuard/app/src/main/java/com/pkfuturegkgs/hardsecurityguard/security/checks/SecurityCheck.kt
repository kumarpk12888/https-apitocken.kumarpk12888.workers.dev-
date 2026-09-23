package com.pkfuturegkgs.hardsecurityguard.security.checks

import android.content.Context
import com.pkfuturegkgs.hardsecurityguard.security.SecurityCheckResult

/**
 * Contract every modular check implements. Keeping checks behind this
 * interface (rather than one giant scanner class) is what lets native
 * Android APIs be swapped in or added later without touching call sites —
 * see SecurityScanner, which just holds a List<SecurityCheck>.
 */
interface SecurityCheck {
    val id: String
    suspend fun run(context: Context): SecurityCheckResult
}
