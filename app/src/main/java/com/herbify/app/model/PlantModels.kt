package com.herbify.app.model

data class PlantCardModel(
    val id: Int,
    val name: String,
    val scientificName: String,
    val imageUrl: String?,
    val discovered: Boolean
)

data class PlantDetailsModel(
    val id: Int,
    val name: String,
    val scientificName: String,
    val family: String,
    val genus: String,
    val type: String,
    val cycle: String,
    val watering: String,
    val sunlight: List<String>,
    val imageUrl: String?,
    val discovered: Boolean
)