package com.herbify.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "captured_plants")
data class CapturedPlantEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val plantName: String,
    val scientificName: String,
    val imageUrl: String?,
    val zoneName: String?,
    val fact: String?,
    val capturedAt: Long,
    val confidence: Double
)