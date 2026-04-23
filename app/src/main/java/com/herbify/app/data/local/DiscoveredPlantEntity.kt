package com.herbify.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "discovered_plants")
data class DiscoveredPlantEntity(
    @PrimaryKey val plantId: Int,
    val discoveredAt: Long,
    val isFavorite: Boolean = false
)