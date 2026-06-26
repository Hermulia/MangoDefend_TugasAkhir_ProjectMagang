package com.riset.mangodefendd.data.scan

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanResultDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(result: ScanResultEntity)

    @Query("SELECT * FROM scan_results ORDER BY scanDate DESC")
    fun getAll(): Flow<List<ScanResultEntity>>

    @Query("DELETE FROM scan_results")
    suspend fun clearAll()
}

