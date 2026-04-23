package com.herbify.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PlantDao {
    @Query("SELECT * FROM discovered_plants")
    fun observeDiscoveredPlants(): Flow<List<DiscoveredPlantEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM discovered_plants WHERE plantId = :plantId)")
    suspend fun isDiscovered(plantId: Int): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlant(entity: DiscoveredPlantEntity)
}