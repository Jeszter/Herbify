package com.herbify.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.herbify.app.data.local.CapturedPlantEntity
import com.herbify.app.ui.theme.DarkBg
import com.herbify.app.viewmodel.HerbariumViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HerbariumScreen(
    viewModel: HerbariumViewModel
) {
    val state by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        if (state.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        } else if (state.plants.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Herbarium is empty",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Take photos of plants and they will appear here",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Herbarium",
                    style = MaterialTheme.typography.headlineMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Captured plants: ${state.plants.size}",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(16.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.plants) { plant ->
                        CapturedPlantCard(
                            plant = plant,
                            onClick = { viewModel.openPlant(plant) }
                        )
                    }
                }
            }
        }

        state.selectedPlant?.let { plant ->
            AlertDialog(
                onDismissRequest = { viewModel.closePlant() },
                confirmButton = {
                    TextButton(onClick = { viewModel.closePlant() }) {
                        Text("Close")
                    }
                },
                title = {
                    Text(plant.plantName)
                },
                text = {
                    Column {
                        AsyncImage(
                            model = plant.imageUrl,
                            contentDescription = plant.plantName,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(190.dp)
                                .clip(RoundedCornerShape(18.dp)),
                            contentScale = ContentScale.Crop
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        if (plant.scientificName.isNotBlank()) {
                            Text("Scientific: ${plant.scientificName}")
                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        if (!plant.zoneName.isNullOrBlank()) {
                            Text("Found in: ${plant.zoneName}")
                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        Text("Captured: ${formatDate(plant.capturedAt)}")
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Confidence: ${(plant.confidence * 100).toInt()}%")

                        if (!plant.fact.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Interesting fact:")
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(plant.fact)
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun CapturedPlantCard(
    plant: CapturedPlantEntity,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(10.dp)
        ) {
            AsyncImage(
                model = plant.imageUrl,
                contentDescription = plant.plantName,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(14.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = plant.plantName,
                style = MaterialTheme.typography.titleMedium
            )

            if (plant.scientificName.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = plant.scientificName,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = formatDate(plant.capturedAt),
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

private fun formatDate(time: Long): String {
    return SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(time))
}