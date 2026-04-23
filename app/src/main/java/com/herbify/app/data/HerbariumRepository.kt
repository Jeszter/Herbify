package com.herbify.app.data

import com.herbify.app.data.local.CapturedPlantDao
import com.herbify.app.data.local.CapturedPlantEntity
import kotlinx.coroutines.flow.Flow

class HerbariumRepository(
    private val dao: CapturedPlantDao
) {
    fun observeCapturedPlants(): Flow<List<CapturedPlantEntity>> {
        return dao.observeCapturedPlants()
    }

    suspend fun savePlant(
        plantName: String,
        scientificName: String,
        imageUrl: String?,
        zoneName: String?,
        fact: String?,
        confidence: Double
    ) {
        dao.insertPlant(
            CapturedPlantEntity(
                plantName = plantName,
                scientificName = scientificName,
                imageUrl = imageUrl,
                zoneName = zoneName,
                fact = fact,
                capturedAt = System.currentTimeMillis(),
                confidence = confidence
            )
        )
    }
}