package com.pkfuturegkgs.hardsecurityguard

import android.app.Application
import com.pkfuturegkgs.hardsecurityguard.data.local.AppDatabase
import com.pkfuturegkgs.hardsecurityguard.data.remote.CloudflareSyncService
import com.pkfuturegkgs.hardsecurityguard.data.remote.SyncTokenStore

/**
 * Application-level container for the small set of singletons this app
 * needs (database, sync service, token store). No dependency-injection
 * framework is used — the app is intentionally small, so simple manual
 * wiring keeps the codebase easy to follow and audit.
 */
class HSGApplication : Application() {

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val syncTokenStore: SyncTokenStore by lazy { SyncTokenStore(this) }
    val cloudflareSyncService: CloudflareSyncService by lazy {
        CloudflareSyncService.create(tokenProvider = { syncTokenStore.getToken() })
    }
}
