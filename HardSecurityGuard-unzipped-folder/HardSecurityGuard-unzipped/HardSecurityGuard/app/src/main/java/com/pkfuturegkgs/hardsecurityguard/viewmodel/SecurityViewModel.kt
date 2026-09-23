package com.pkfuturegkgs.hardsecurityguard.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.pkfuturegkgs.hardsecurityguard.HSGApplication
import com.pkfuturegkgs.hardsecurityguard.data.local.ScanHistoryEntity
import com.pkfuturegkgs.hardsecurityguard.data.remote.CloudflareSyncResult
import com.pkfuturegkgs.hardsecurityguard.security.SecurityScanResult
import com.pkfuturegkgs.hardsecurityguard.security.SecurityScanner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted

sealed class ScanUiState {
    data object Idle : ScanUiState()
    data object Scanning : ScanUiState()
    data class Done(val result: SecurityScanResult) : ScanUiState()
    data class Error(val message: String) : ScanUiState()
}

class SecurityViewModel(application: Application) : AndroidViewModel(application) {

    private val app get() = getApplication<HSGApplication>()
    private val scanner = SecurityScanner()
    private val gson = Gson()

    private val _uiState = MutableStateFlow<ScanUiState>(ScanUiState.Idle)
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    private val _syncState = MutableStateFlow<CloudflareSyncResult?>(null)
    val syncState: StateFlow<CloudflareSyncResult?> = _syncState.asStateFlow()

    val history: StateFlow<List<ScanHistoryEntity>> =
        app.database.scanHistoryDao().observeHistory()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val isSyncConfigured: Boolean get() = app.syncTokenStore.isSyncConfigured()

    fun runScan() {
        viewModelScope.launch {
            _uiState.value = ScanUiState.Scanning
            try {
                val result = scanner.scan(app.applicationContext)
                _uiState.value = ScanUiState.Done(result)

                app.database.scanHistoryDao().insert(
                    ScanHistoryEntity(
                        timestampIso = result.timestampIso,
                        score = result.score,
                        warningsJson = gson.toJson(result.warnings.map { it.title }),
                        synced = false
                    )
                )

                if (isSyncConfigured) {
                    syncNow(result)
                }
            } catch (e: Exception) {
                _uiState.value = ScanUiState.Error(e.message ?: "Scan failed")
            }
        }
    }

    fun syncNow(result: SecurityScanResult) {
        viewModelScope.launch {
            _syncState.value = app.cloudflareSyncService.sync(result)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            app.database.scanHistoryDao().deleteAll()
        }
    }

    fun saveToken(token: String) {
        app.syncTokenStore.setToken(token)
    }

    fun clearToken() {
        app.syncTokenStore.setToken(null)
    }

    fun getMaskedToken(): String? =
        app.syncTokenStore.getToken()?.let { t ->
            if (t.length <= 4) "••••" else "••••${t.takeLast(4)}"
        }

    suspend fun checkConnection(): CloudflareSyncResult = app.cloudflareSyncService.checkHealth()
}
