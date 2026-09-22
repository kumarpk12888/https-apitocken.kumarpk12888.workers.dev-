package com.kumarpk12888.hardsecurityguard.sync

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.kumarpk12888.hardsecurityguard.security.SecurityScanResult
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

private const val PREFS_NAME = "secure_hard_security_guard"
private const val KEY_TOKEN = "cloudflare_api_token"

class CloudflareSyncService(private val context: Context) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private val sharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            PREFS_NAME,
            MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveToken(token: String) {
        sharedPreferences.edit().putString(KEY_TOKEN, token.trim()).apply()
    }

    fun getToken(): String = sharedPreferences.getString(KEY_TOKEN, "") ?: ""

    fun clearToken() {
        sharedPreferences.edit().remove(KEY_TOKEN).apply()
    }

    fun sync(scan: SecurityScanResult): CloudSyncResult {
        val token = getToken().trim()
        if (token.isEmpty()) {
            return CloudSyncResult.Offline("Cloud Sync Offline")
        }

        val payload = JSONObject().apply {
            put("score", scan.score)
            put("warnings", scan.warnings)
            put("status", scan.status)
            put("summary", scan.summary)
            put("timestamp", System.currentTimeMillis())
        }

        val request = Request.Builder()
            .url("https://apitocken.kumarpk12888.workers.dev/report")
            .addHeader("Authorization", "Bearer $token")
            .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        return try {
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""
            if (response.isSuccessful) {
                CloudSyncResult.Success("Cloud Sync Successful")
            } else {
                CloudSyncResult.Offline("Cloud Sync Offline")
            }
        } catch (e: Exception) {
            CloudSyncResult.Offline("Cloud Sync Offline")
        }
    }
}

sealed class CloudSyncResult {
    data class Success(val message: String) : CloudSyncResult()
    data class Offline(val message: String) : CloudSyncResult()
    data class Error(val message: String) : CloudSyncResult()
}
