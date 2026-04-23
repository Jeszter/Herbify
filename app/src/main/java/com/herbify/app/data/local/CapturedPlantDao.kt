package com.herbify.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CapturedPlantDao {
    @Query("SELECT * FROM captured_plants ORDER BY capturedAt DESC")
    fun observeCapturedPlants(): Flow<List<CapturedPlantEntity>>

    @Query("SELECT * FROM captured_plants WHERE id = :id LIMIT 1")
    suspend fun getPlantById(id: Long): CapturedPlantEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlant(entity: CapturedPlantEntity)

    @Query("DELETE FROM captured_plants")
    suspend fun clearAll()
}