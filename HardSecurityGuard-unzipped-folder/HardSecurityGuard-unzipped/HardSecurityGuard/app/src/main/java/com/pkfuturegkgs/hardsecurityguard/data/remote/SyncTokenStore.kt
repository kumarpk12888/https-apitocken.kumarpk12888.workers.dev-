package com.pkfuturegkgs.hardsecurityguard.data.remote

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * SECURITY NOTE — read this before changing anything here.
 *
 * The Cloudflare Worker's /report and /health endpoints require
 * `Authorization: Bearer <API_TOKEN>`. That token is NEVER compiled into
 * the APK (no BuildConfig field, no hard-coded string, nothing baked in
 * at build time). Instead:
 *
 *  1. The app ships with sync disabled by default and works fully offline.
 *  2. The user (Pk himself, or whoever he shares this app with) can
 *     optionally paste the Worker token into the in-app Settings screen.
 *  3. The token is stored ONLY in EncryptedSharedPreferences, backed by
 *     the Android Keystore, and excluded from cloud/auto backup (see
 *     data_extraction_rules.xml).
 *
 * This is still a bearer-token pattern, and a determined user with root
 * access to their own device could extract a token they entered
 * themselves — but that is fundamentally different from shipping the
 * Worker's admin secret inside a publicly distributed APK, which is what
 * this design avoids.
 *
 * For a wider public release (not just personal/trusted use), the
 * stronger pattern is: the Worker issues short-lived, per-device tokens
 * to authenticated users (e.g. via a login step), rather than every
 * install sharing one long-lived admin token. See README "Production
 * hardening" section.
 */
class SyncTokenStore(context: Context) {

    private val prefs = run {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "hsg_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)?.takeIf { it.isNotBlank() }

    fun setToken(token: String?) {
        prefs.edit().apply {
            if (token.isNullOrBlank()) remove(KEY_TOKEN) else putString(KEY_TOKEN, token)
        }.apply()
    }

    fun isSyncConfigured(): Boolean = !getToken().isNullOrBlank()

    private companion object {
        const val KEY_TOKEN = "worker_api_token"
    }
}
