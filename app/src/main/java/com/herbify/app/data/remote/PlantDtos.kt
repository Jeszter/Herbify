package com.herbify.app.data.remote

data class PlantListResponse(
    val data: List<PlantDto>
)

data class PlantDto(
    val id: Int,
    val common_name: String?,
    val scientific_name: List<String>?,
    val family: String?,
    val genus: String?,
    val default_image: PlantImageDto?
)

data class PlantDetailsDto(
    val id: Int,
    val common_name: String?,
    val scientific_name: List<String>?,
    val family: String?,
    val genus: String?,
    val type: String?,
    val cycle: String?,
    val watering: String?,
    val sunlight: List<String>?,
    val default_image: PlantImageDto?
)

data class PlantImageDto(
    val original_url: String?,
    val regular_url: String?,
    val medium_url: String?,
    val small_url: String?,
    val thumbnail: String?
)