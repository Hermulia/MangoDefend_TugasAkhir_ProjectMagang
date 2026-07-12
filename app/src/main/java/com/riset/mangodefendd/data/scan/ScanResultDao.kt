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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(results: List<ScanResultEntity>)

    @Query("SELECT * FROM scan_results ORDER BY scanDate DESC")
    fun getAll(): Flow<List<ScanResultEntity>>

    @Query("SELECT * FROM scan_results ORDER BY scanDate DESC LIMIT :limit")
    fun getPaged(limit: Int): Flow<List<ScanResultEntity>>

    @Query("SELECT * FROM scan_results WHERE :status = 'ALL' OR UPPER(status) = UPPER(:status) ORDER BY scanDate DESC LIMIT :limit")
    fun getPagedFiltered(limit: Int, status: String): Flow<List<ScanResultEntity>>

    @Query("DELETE FROM scan_results")
    suspend fun clearAll()

    @Query("DELETE FROM scan_results WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("SELECT COUNT(*) FROM scan_results")
    fun getTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM scan_results WHERE UPPER(status) = 'DANGEROUS'")
    fun getMalwareCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM scan_results WHERE UPPER(status) = 'SUSPICIOUS'")
    fun getSuspiciousCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM scan_results WHERE UPPER(status) = 'SAFE' OR UPPER(status) = 'DELETED' OR UPPER(status) = 'TERHAPUS'")
    fun getSafeCount(): Flow<Int>

    @Query("SELECT MAX(scanDate) FROM scan_results")
    fun getLastScanTimestamp(): Flow<Long?>
}

