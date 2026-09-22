package com.kumarpk12888.hardsecurityguard.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context

@Entity(tableName = "scan_history")
data class ScanHistoryEntity(
    @androidx.room.PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val score: Int,
    val warnings: Int,
    val summary: String
)

@Dao
interface ScanHistoryDao {
    @Query("SELECT * FROM scan_history ORDER BY timestamp DESC")
    fun observeHistory(): kotlinx.coroutines.flow.Flow<List<ScanHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: ScanHistoryEntity)

    @Query("DELETE FROM scan_history")
    suspend fun deleteAll()

    @Query("DELETE FROM scan_history WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Database(entities = [ScanHistoryEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scanHistoryDao(): ScanHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "hard_security_guard_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
