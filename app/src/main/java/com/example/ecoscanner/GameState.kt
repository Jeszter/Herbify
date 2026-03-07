package com.example.ecoscanner

import androidx.compose.runtime.*

// ─── Daily Quest ──────────────────────────────────────────────────────────────

data class DailyQuest(
    val id: String,
    val emoji: String,
    val name: String,
    val target: Int,
    val reward: Int   // ECO
)

val DAILY_QUESTS = listOf(
    DailyQuest("q_scan3",   "📷", "Просканируй 3 объекта",         3,  15),
    DailyQuest("q_rare",    "⭐", "Найди Rare+ объект",            1,  25),
    DailyQuest("q_walk",    "🚶", "Пройди 500м с открытым приложением", 500, 10),
    DailyQuest("q_collect", "🃏", "Добавь 2 карточки в коллекцию", 2,  20)
)

// ─── GameState ────────────────────────────────────────────────────────────────

object GameState {

    // ── Баланс и прогресс ─────────────────────────────────────────────────────
    var ecoBalance    by mutableStateOf(247)
    var totalXp       by mutableStateOf(1250)
    var streak        by mutableStateOf(3)

    val level          get() = (totalXp / 1000) + 1
    val xpForCurrent   get() = totalXp % 1000
    val xpProgress     get() = xpForCurrent / 1000f
    val streakMultiplier get() = 1.0 + (streak * 0.05).coerceAtMost(0.5)
    val levelTitle     get() = when (level) {
        1    -> "Новичок"
        2    -> "Следопыт"
        3    -> "Натуралист"
        4    -> "Эколог"
        5    -> "Ботаник"
        else -> "Мастер природы"
    }

    // ── Коллекция ─────────────────────────────────────────────────────────────
    var collection by mutableStateOf(
        listOf(PLANT_DATABASE[0], PLANT_DATABASE[1], PLANT_DATABASE[2])
    )

    // ── Лимиты рюкзака (апгрейд backpack) ────────────────────────────────────
    val maxCollectionSlots get(): Int {
        val lvl = upgradeLevels["backpack"] ?: 0
        return when (lvl) {
            0 -> 30; 1 -> 50; 2 -> 100; 3 -> 200; 4 -> 400; else -> Int.MAX_VALUE
        }
    }

    // ── Радиус радара ─────────────────────────────────────────────────────────
    val radarRadiusM get(): Int {
        val lvl = upgradeLevels["radar"] ?: 0
        return 200 + when (lvl) {
            0 -> 0; 1 -> 50; 2 -> 100; 3 -> 200; 4 -> 300; else -> 500
        }
    }

    // ── Квесты ────────────────────────────────────────────────────────────────
    val questProgress   = mutableStateMapOf<String, Int>()
    // mutableStateSetOf недоступен в этой версии — используем Map как Set
    val completedQuests = mutableStateMapOf<String, Boolean>()

    fun isQuestDone(id: String)         = completedQuests[id] == true
    fun questProgressFloat(id: String): Float {
        val quest  = DAILY_QUESTS.find { it.id == id } ?: return 0f
        val current = questProgress[id] ?: 0
        return (current.toFloat() / quest.target).coerceIn(0f, 1f)
    }

    // ── КД сканера ────────────────────────────────────────────────────────────
    var lastScanTime by mutableStateOf(0L)
    val scannedPlants = mutableStateMapOf<Int, Long>()

    // Базовый КД — уменьшается апгрейдом scanner_cd
    val SCANNER_CD_BASE_MS = 2 * 60 * 1000L
    val PLANT_CD_MS        = 24 * 60 * 60 * 1000L

    val scannerCdReduction get(): Long {
        val lvl = upgradeLevels["scanner_cd"] ?: 0
        return when (lvl) {
            0 -> 0L; 1 -> 30_000L; 2 -> 60_000L
            3 -> 90_000L; 4 -> 110_000L; else -> SCANNER_CD_BASE_MS
        }
    }
    val effectiveScannerCdMs get() = (SCANNER_CD_BASE_MS - scannerCdReduction).coerceAtLeast(0L)

    fun scannerCdRemaining(): Long {
        if (effectiveScannerCdMs == 0L) return 0L
        return maxOf(0L, effectiveScannerCdMs - (System.currentTimeMillis() - lastScanTime))
    }

    fun plantCdRemaining(plantId: Int): Long {
        val t = scannedPlants[plantId] ?: return 0L
        return maxOf(0L, PLANT_CD_MS - (System.currentTimeMillis() - t))
    }

    fun canScan()              = scannerCdRemaining() == 0L
    fun canScanPlant(id: Int)  = plantCdRemaining(id) == 0L

    fun formatCd(ms: Long): String {
        if (ms <= 0L) return ""
        val s = ms / 1000
        return when {
            s >= 3600  -> "${s / 3600}ч ${(s % 3600) / 60}м"
            s >= 60    -> "${s / 60}м ${s % 60}с"
            else       -> "${s}с"
        }
    }

    // ── ECO бонус (апгрейд eco_bonus) ─────────────────────────────────────────
    val ecoMultiplier get(): Float {
        val lvl = upgradeLevels["eco_bonus"] ?: 0
        return when (lvl) {
            0 -> 1.0f; 1 -> 1.10f; 2 -> 1.25f; 3 -> 1.50f; 4 -> 1.75f; else -> 2.0f
        }
    }

    // ── Активные бустеры (id -> expiry timestamp) ─────────────────────────────
    val activeBoosters = mutableStateMapOf<String, Long>()

    fun isBoosterActive(id: String): Boolean =
        activeBoosters[id]?.let { it > System.currentTimeMillis() } ?: false

    fun buyBooster(booster: BoosterItem): Boolean {
        if (ecoBalance < booster.cost) return false
        ecoBalance -= booster.cost
        when (booster.id) {
            "cd_reset"   -> lastScanTime = 0L
            "cd_half"    -> {
                val remaining = scannerCdRemaining()
                if (remaining > 0) lastScanTime -= remaining / 2
                activeBoosters[booster.id] = System.currentTimeMillis() + 30 * 60_000L
            }
            "double_eco"  -> activeBoosters[booster.id] = System.currentTimeMillis() + 60 * 60_000L
            "rare_boost"  -> activeBoosters[booster.id] = System.currentTimeMillis() + 30 * 60_000L
        }
        return true
    }

    // ── Апгрейды ──────────────────────────────────────────────────────────────
    val upgradeLevels = mutableStateMapOf<String, Int>()

    fun buyUpgrade(id: String): Boolean {
        val upgrade = SHOP_UPGRADES.find { it.id == id } ?: return false
        val current = upgradeLevels[id] ?: 0
        if (current >= upgrade.maxLevel) return false
        val nextLevel = upgrade.levels[current]
        if (ecoBalance < nextLevel.cost) return false
        ecoBalance -= nextLevel.cost
        upgradeLevels[id] = current + 1
        return true
    }

    // ── Запись скана ──────────────────────────────────────────────────────────
    fun recordScan(card: EcoCard) {
        lastScanTime = System.currentTimeMillis()
        scannedPlants[card.id] = System.currentTimeMillis()

        // Награда с учётом множителей
        val baseReward = card.rarity.multiplier
        val doubleActive = isBoosterActive("double_eco")
        val reward = (baseReward * ecoMultiplier * if (doubleActive) 2f else 1f).toInt().coerceAtLeast(1)
        ecoBalance += reward

        // XP
        val xpGain = baseReward * 10
        totalXp += xpGain

        // Стрик
        streak++

        // Коллекция
        if (collection.none { it.id == card.id }) {
            collection = collection + card
        }

        // Квесты
        incrementQuest("q_scan3")
        incrementQuest("q_collect")
        if (card.rarity == Rarity.RARE || card.rarity == Rarity.EPIC || card.rarity == Rarity.LEGENDARY) {
            incrementQuest("q_rare")
        }
    }

    private fun incrementQuest(id: String) {
        val quest = DAILY_QUESTS.find { it.id == id } ?: return
        if (isQuestDone(id)) return
        val newVal = (questProgress[id] ?: 0) + 1
        questProgress[id] = newVal
        if (newVal >= quest.target) {
            completedQuests[id] = true
            ecoBalance += quest.reward
        }
    }
}