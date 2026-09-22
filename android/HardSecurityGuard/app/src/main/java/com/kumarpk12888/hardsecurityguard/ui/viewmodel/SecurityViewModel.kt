package com.kumarpk12888.hardsecurityguard.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kumarpk12888.hardsecurityguard.data.local.AppDatabase
import com.kumarpk12888.hardsecurityguard.data.local.ScanHistoryEntity
import com.kumarpk12888.hardsecurityguard.security.LocalSecurityScanner
import com.kumarpk12888.hardsecurityguard.security.SecurityScanResult
import com.kumarpk12888.hardsecurityguard.sync.CloudflareSyncService
import com.kumarpk12888.hardsecurityguard.sync.CloudSyncResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class SecurityViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    private val cloudflareSyncService = CloudflareSyncService(application)

    private val _scanResult = MutableStateFlow(SecurityScanResult())
    val scanResult: StateFlow<SecurityScanResult> = _scanResult.asStateFlow()

    val history = database.scanHistoryDao().observeHistory()
        .map { it }

    init {
        runScan()
    }

    fun runScan() {
        viewModelScope.launch {
            val result = LocalSecurityScanner(getApplication()).scan()
            _scanResult.value = result
            database.scanHistoryDao().insert(
                ScanHistoryEntity(
                    timestamp = result.scanTime,
                    score = result.score,
                    warnings = result.warnings,
                    summary = result.summary
                )
            )
        }
    }

    fun clearHistory() {
        viewModelScope.launch { database.scanHistoryDao().deleteAll() }
    }

    fun syncNow(): CloudSyncResult {
        val current = _scanResult.value
        return cloudflareSyncService.sync(current)
    }

    fun saveToken(token: String) {
        cloudflareSyncService.saveToken(token)
    }

    fun getToken(): String = cloudflareSyncService.getToken()
}
