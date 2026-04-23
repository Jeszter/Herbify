package com.herbify.app.game

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.herbify.app.model.MapObjects
import com.herbify.app.model.PlantObject
import com.herbify.app.model.Rarity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class ShopItem(
    val id: String,
    val name: String,
    val description: String,
    val cost: Int,
    val emoji: String,
    val type: ShopItemType
)

enum class ShopItemType { UPGRADE, BOOSTER }

data class EventItem(
    val id: String,
    val title: String,
    val description: String,
    val reward: String,
    val isActive: Boolean,
    val emoji: String
)

class GameState : ViewModel() {

    // Player stats
    private val _eco = MutableStateFlow(0)
    val eco: StateFlow<Int> = _eco

    private val _xp = MutableStateFlow(0)
    val xp: StateFlow<Int> = _xp

    private val _level = MutableStateFlow(1)
    val level: StateFlow<Int> = _level

    private val _streak = MutableStateFlow(0)
    val streak: StateFlow<Int> = _streak

    // Cooldowns
    private val _globalCooldownUntil = MutableStateFlow(0L)
    val globalCooldownUntil: StateFlow<Long> = _globalCooldownUntil

    private val _plantCooldowns = MutableStateFlow<Map<String, Long>>(emptyMap())
    val plantCooldowns: StateFlow<Map<String, Long>> = _plantCooldowns

    // Collection
    private val _collection = MutableStateFlow<List<PlantObject>>(emptyList())
    val collection: StateFlow<List<PlantObject>> = _collection

    // Selected plant for scanning
    val selectedPlant = mutableStateOf<PlantObject?>(null)

    // Scan result
    val lastScanResult = mutableStateOf<ScanResult?>(null)

    // Owned shop items
    private val _ownedItems = MutableStateFlow<Set<String>>(emptySet())
    val ownedItems: StateFlow<Set<String>> = _ownedItems

    // Scan range multiplier (upgradeable)
    private val _scanRange = MutableStateFlow(150.0) // meters
    val scanRange: StateFlow<Double> = _scanRange

    val shopItems = listOf(
        ShopItem("upgrade_range", "Extended Radar", "Doubles your scan range to 300m", 80, "📡", ShopItemType.UPGRADE),
        ShopItem("upgrade_xp", "XP Booster", "Earn 50% more XP for 10 scans", 60, "⚡", ShopItemType.BOOSTER),
        ShopItem("upgrade_eco", "ECO Amplifier", "Earn 2x ECO on next 5 scans", 100, "💰", ShopItemType.BOOSTER),
        ShopItem("upgrade_cooldown", "Cooldown Reducer", "Global cooldown reduced to 30s", 150, "⏱️", ShopItemType.UPGRADE),
        ShopItem("upgrade_scanner", "Pro Scanner", "Instantly identifies plant rarity", 200, "🔬", ShopItemType.UPGRADE),
    )

    val events = listOf(
        EventItem("evt_001", "Spring Harvest Festival", "Scan 5 different plants this week to earn bonus ECO!", "500 ECO", true, "🌸"),
        EventItem("evt_002", "Legendary Hunt", "Find and scan a Legendary plant anywhere on the map.", "1000 ECO + Badge", true, "🐉"),
        EventItem("evt_003", "Streak Master", "Maintain a 7-day scanning streak.", "250 ECO + Title", false, "🔥"),
        EventItem("evt_004", "Community Census", "Be part of the first 1000 scanners this season.", "Exclusive Badge", false, "🌍"),
    )

    fun canScan(plant: PlantObject): ScanStatus {
        val now = System.currentTimeMillis()
        if (now < _globalCooldownUntil.value) {
            val remaining = (_globalCooldownUntil.value - now) / 1000
            return ScanStatus.GlobalCooldown(remaining)
        }
        val plantCooldown = _plantCooldowns.value[plant.id] ?: 0L
        if (now < plantCooldown) {
            val remaining = (plantCooldown - now) / 1000
            return ScanStatus.PlantCooldown(remaining)
        }
        return ScanStatus.Ready
    }

    fun performScan(plant: PlantObject): ScanResult {
        val now = System.currentTimeMillis()
        val globalCooldownMs = if (_ownedItems.value.contains("upgrade_cooldown")) 30_000L else 120_000L
        _globalCooldownUntil.value = now + globalCooldownMs
        _plantCooldowns.value = _plantCooldowns.value + (plant.id to (now + 24 * 60 * 60 * 1000L))

        val xpMultiplier = if (_ownedItems.value.contains("upgrade_xp")) 1.5f else 1f
        val ecoMultiplier = if (_ownedItems.value.contains("upgrade_eco")) 2f else 1f

        val earnedEco = (plant.ecoReward * ecoMultiplier).toInt()
        val earnedXp = (plant.xpReward * xpMultiplier).toInt()

        _eco.value += earnedEco
        _xp.value += _xp.value + earnedXp
        checkLevelUp()
        _streak.value += 1

        val isNew = _collection.value.none { it.id == plant.id }
        if (isNew) {
            _collection.value = _collection.value + plant
        }

        val result = ScanResult(plant, earnedEco, earnedXp, isNew)
        lastScanResult.value = result
        return result
    }

    fun buyItem(item: ShopItem): Boolean {
        if (_eco.value < item.cost) return false
        if (_ownedItems.value.contains(item.id)) return false
        _eco.value -= item.cost
        _ownedItems.value = _ownedItems.value + item.id
        if (item.id == "upgrade_range") {
            _scanRange.value = 300.0
        }
        return true
    }

    private fun checkLevelUp() {
        val xpForNextLevel = _level.value * 100
        if (_xp.value >= xpForNextLevel) {
            _level.value += 1
        }
    }

    fun getCollectionCount() = _collection.value.size
    fun getTotalPlants() = MapObjects.plants.size

    fun getRarityCount(rarity: Rarity): Int =
        _collection.value.count { it.rarity == rarity }

    fun getXpProgress(): Float {
        val xpForNextLevel = _level.value * 100
        return (_xp.value % xpForNextLevel).toFloat() / xpForNextLevel
    }
}

sealed class ScanStatus {
    object Ready : ScanStatus()
    data class GlobalCooldown(val secondsRemaining: Long) : ScanStatus()
    data class PlantCooldown(val secondsRemaining: Long) : ScanStatus()
}

data class ScanResult(
    val plant: PlantObject,
    val ecoEarned: Int,
    val xpEarned: Int,
    val isNewDiscovery: Boolean
)
