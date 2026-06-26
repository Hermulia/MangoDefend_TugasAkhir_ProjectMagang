package com.riset.mangodefendd.data.scan

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_results")
data class ScanResultEntity(
    @PrimaryKey val id: String,
    val fileName: String,
    val filePath: String,
    val scanDate: Long,
    val malwareScore: Double,
    val benignScore: Double,
    val status: String,
    val actionTaken: String
)

