package com.pkfuturegkgs.hardsecurityguard.data.remote

/** Request body for POST /report — matches the existing Worker contract exactly. */
data class ReportRequest(
    val device: String = "Android",
    val score: Int,
    val warnings: List<String>,
    val timestamp: String
)

/** Outcome of a sync attempt, surfaced to the ViewModel/UI without leaking raw exceptions. */
sealed class CloudflareSyncResult {
    data object Success : CloudflareSyncResult()
    data object NotConfigured : CloudflareSyncResult()
    data object Offline : CloudflareSyncResult()
    data class HttpError(val code: Int) : CloudflareSyncResult()
    data class UnknownError(val messageForLog: String) : CloudflareSyncResult()
}
