package com.herbify.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.herbify.app.data.HerbariumRepository
import com.herbify.app.data.local.CapturedPlantEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class HerbariumUiState(
    val isLoading: Boolean = false,
    val plants: List<CapturedPlantEntity> = emptyList(),
    val selectedPlant: CapturedPlantEntity? = null
)

class HerbariumViewModel(
    private val repository: HerbariumRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HerbariumUiState(isLoading = true))
    val uiState: StateFlow<HerbariumUiState> = _uiState.asStateFlow()

    init {
        repository.observeCapturedPlants()
            .onEach { plants ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    plants = plants
                )
            }
            .launchIn(viewModelScope)
    }

    fun openPlant(plant: CapturedPlantEntity) {
        _uiState.value = _uiState.value.copy(selectedPlant = plant)
    }

    fun closePlant() {
        _uiState.value = _uiState.value.copy(selectedPlant = null)
    }

    fun saveRecognizedPlant(
        plantName: String,
        scientificName: String,
        imageUrl: String?,
        zoneName: String?,
        fact: String?,
        confidence: Double
    ) {
        viewModelScope.launch {
            repository.savePlant(
                plantName = plantName,
                scientificName = scientificName,
                imageUrl = imageUrl,
                zoneName = zoneName,
                fact = fact,
                confidence = confidence
            )
        }
    }
}