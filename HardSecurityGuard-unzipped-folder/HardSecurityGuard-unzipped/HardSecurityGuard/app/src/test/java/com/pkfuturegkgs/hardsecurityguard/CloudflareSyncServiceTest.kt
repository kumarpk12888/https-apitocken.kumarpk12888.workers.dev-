package com.pkfuturegkgs.hardsecurityguard

import com.google.common.truth.Truth.assertThat
import com.pkfuturegkgs.hardsecurityguard.data.remote.CloudflareApi
import com.pkfuturegkgs.hardsecurityguard.data.remote.CloudflareSyncResult
import com.pkfuturegkgs.hardsecurityguard.data.remote.CloudflareSyncService
import com.pkfuturegkgs.hardsecurityguard.data.remote.ReportRequest
import com.pkfuturegkgs.hardsecurityguard.security.SecurityCheckResult
import com.pkfuturegkgs.hardsecurityguard.security.SecurityScanResult
import com.pkfuturegkgs.hardsecurityguard.security.Severity
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test
import retrofit2.Response

/**
 * Fake CloudflareApi so these tests never touch the network — they only
 * verify CloudflareSyncService's decision logic (no token => NotConfigured,
 * HTTP failure => HttpError, etc).
 */
private class FakeCloudflareApi(
    private val healthResponse: Response<Void>? = null,
    private val reportResponse: Response<Void>? = null
) : CloudflareApi {
    override suspend fun health(authHeader: String): Response<Void> =
        healthResponse ?: Response.success(null)

    override suspend fun postReport(authHeader: String, body: ReportRequest): Response<Void> =
        reportResponse ?: Response.success(null)
}

class CloudflareSyncServiceTest {

    private fun sampleResult() = SecurityScanResult(
        timestampIso = "2026-01-01T00:00:00Z",
        score = 90,
        checks = listOf(
            SecurityCheckResult("a", "A", passed = true, severity = Severity.LOW, message = "ok")
        )
    )

    @Test
    fun `sync returns NotConfigured when no token is set`() = runBlocking {
        val service = CloudflareSyncService(FakeCloudflareApi(), tokenProvider = { null })
        assertThat(service.sync(sampleResult())).isEqualTo(CloudflareSyncResult.NotConfigured)
    }

    @Test
    fun `sync returns Success on 2xx response`() = runBlocking {
        val service = CloudflareSyncService(FakeCloudflareApi(), tokenProvider = { "test-token" })
        assertThat(service.sync(sampleResult())).isEqualTo(CloudflareSyncResult.Success)
    }

    @Test
    fun `sync returns HttpError on non-2xx response`() = runBlocking {
        val errorResponse = Response.error<Void>(401, "unauthorized".toResponseBody(null))
        val service = CloudflareSyncService(
            FakeCloudflareApi(reportResponse = errorResponse),
            tokenProvider = { "bad-token" }
        )
        val result = service.sync(sampleResult())
        assertThat(result).isEqualTo(CloudflareSyncResult.HttpError(401))
    }

    @Test
    fun `checkHealth returns NotConfigured when no token is set`() = runBlocking {
        val service = CloudflareSyncService(FakeCloudflareApi(), tokenProvider = { null })
        assertThat(service.checkHealth()).isEqualTo(CloudflareSyncResult.NotConfigured)
    }
}
