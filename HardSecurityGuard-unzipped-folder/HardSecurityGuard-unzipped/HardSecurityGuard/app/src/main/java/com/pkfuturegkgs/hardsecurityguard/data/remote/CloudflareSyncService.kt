package com.pkfuturegkgs.hardsecurityguard.data.remote

import com.pkfuturegkgs.hardsecurityguard.BuildConfig
import com.pkfuturegkgs.hardsecurityguard.security.SecurityScanResult
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Thin wrapper around [CloudflareApi] that:
 *  - reads the bearer token at call time from [tokenProvider] (never a
 *    compiled-in constant — see SyncTokenStore for why),
 *  - returns [CloudflareSyncResult] instead of throwing, so the UI never
 *    has to deal with raw exceptions,
 *  - treats "no token configured" as a normal, expected state rather
 *    than an error, since the app is offline-first by design.
 */
class CloudflareSyncService(
    private val api: CloudflareApi,
    private val tokenProvider: () -> String?
) {
    suspend fun checkHealth(): CloudflareSyncResult {
        val token = tokenProvider() ?: return CloudflareSyncResult.NotConfigured
        return runCatching {
            val response = api.health("Bearer $token")
            if (response.isSuccessful) CloudflareSyncResult.Success
            else CloudflareSyncResult.HttpError(response.code())
        }.getOrElse { e ->
            if (e is IOException) CloudflareSyncResult.Offline
            else CloudflareSyncResult.UnknownError(e.message ?: "unknown")
        }
    }

    suspend fun sync(result: SecurityScanResult): CloudflareSyncResult {
        val token = tokenProvider() ?: return CloudflareSyncResult.NotConfigured
        val body = ReportRequest(
            device = "Android",
            score = result.score,
            warnings = result.warnings.map { it.title },
            timestamp = result.timestampIso
        )
        return runCatching {
            val response = api.postReport("Bearer $token", body)
            if (response.isSuccessful) CloudflareSyncResult.Success
            else CloudflareSyncResult.HttpError(response.code())
        }.getOrElse { e ->
            if (e is IOException) CloudflareSyncResult.Offline
            else CloudflareSyncResult.UnknownError(e.message ?: "unknown")
        }
    }

    companion object {
        fun create(tokenProvider: () -> String?): CloudflareSyncService {
            // Logging is verbose only in debug builds, and the Authorization
            // header (which carries the bearer token) is always redacted —
            // it must never end up in logcat, even in debug.
            val logging = HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
                        else HttpLoggingInterceptor.Level.NONE
                redactHeader("Authorization")
            }

            val client = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(BuildConfig.CLOUDFLARE_BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            return CloudflareSyncService(retrofit.create(CloudflareApi::class.java), tokenProvider)
        }
    }
}
