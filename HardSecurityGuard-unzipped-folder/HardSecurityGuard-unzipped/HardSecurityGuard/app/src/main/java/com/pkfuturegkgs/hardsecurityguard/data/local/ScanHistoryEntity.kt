package com.pkfuturegkgs.hardsecurityguard.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local record of one completed scan. This is the source of truth for
 * "Local scan history" — the app works fully offline off this table;
 * Cloudflare sync (see CloudflareSyncService) is a best-effort mirror on
 * top, never a requirement for the app to function.
 */
@Entity(tableName = "scan_history")
data class ScanHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampIso: String,
    val score: Int,
    val warningsJson: String, // serialized list of warning titles, see Converters
    val synced: Boolean = false
)
