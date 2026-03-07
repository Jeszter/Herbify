package com.example.ecoscanner

import androidx.compose.runtime.*
import java.time.LocalDate

// ─── DailyQuest ───────────────────────────────────────────────────────────────

data class DailyQuest(
    val id: String,
    val emoji: String,
    val name: String,
    val target: Int,
    val reward: Int
)

val DAILY_QUESTS = listOf(
    DailyQuest("q_flowers",   "🌸", "Отсканируй 3 цветка",     3,  25),
    DailyQuest("q_park",      "🦆", "Посети парк или водоём",   1,  40),
    DailyQuest("q_rare",      "⭐", "Найди Rare или выше",      1,  30),
    DailyQuest("q_scan5",     "📷", "Сделай 5 сканирований",   5,  35),
    DailyQuest("q_legendary", "🍄", "Найди Legendary объект",   1, 100)
)

// ─── GameState ────────────────────────────────────────────────────────────────

object GameState {

    // ── Константы ─────────────────────────────────────────────────────────────
    private const val BASE_SCANNER_CD_MS = 120_000L
    private const val BASE_SLOTS         = 30
    private const val BASE_RADAR_M       = 100
    const val PLANT_CD_MS                = 86_400_000L

    // ── Основные состояния ────────────────────────────────────────────────────
    var lastScanTime  by mutableStateOf(0L)
    val scannedPlants = mutableStateMapOf<Int, Long>()
    var collection    by mutableStateOf(
        listOf(PLANT_DATABASE[0], PLANT_DATABASE[1], PLANT_DATABASE[2])
    )
    var ecoBalance    by mutableStateOf(247)

    // ── XP + Уровень ──────────────────────────────────────────────────────────
    var xp              by mutableStateOf(1240)
    val level           get() = (1 + xp / 1000).coerceAtMost(50)
    val xpForCurrent    get() = xp % 1000
    val xpProgress      get() = xpForCurrent.toFloat() / 1000f

    val levelTitle get() = when {
        level >= 40 -> "🌳 Хранитель природы"
        level >= 30 -> "🌿 Эко-эксперт"
        level >= 20 -> "🔬 Натуралист"
        level >= 10 -> "🌱 Исследователь"
        else        -> "🌾 Новичок"
    }

    // ── Стрик ─────────────────────────────────────────────────────────────────
    var streak        by mutableStateOf(7)
    var lastStreakDate by mutableStateOf("")

    val streakMultiplier get() = when {
        streak >= 30 -> 3.0f
        streak >= 14 -> 2.5f
        streak >= 7  -> 2.0f
        streak >= 3  -> 1.5f
        else         -> 1.0f
    }

    // ── Апгрейды (id -> текущий уровень 0..5) ─────────────────────────────────
    val upgradeLevels = mutableStateMapOf(
        "scanner_cd"  to 2,
        "ai_accuracy" to 1,
        "backpack"    to 2,
        "radar"       to 5,
        "eco_bonus"   to 0
    )

    // ── Активные бустеры (id -> время окончания мс) ───────────────────────────
    val activeBoosters = mutableStateMapOf<String, Long>()

    // ── Квесты ────────────────────────────────────────────────────────────────
    val questProgress   = mutableStateMapOf<String, Int>()
    // Вместо mutableStateSetOf — используем mutableStateMapOf с Boolean
    val completedQuests = mutableStateMapOf<String, Boolean>()
    var questDate       by mutableStateOf("")

    // ── Вычисляемые параметры ─────────────────────────────────────────────────

    val scannerCdMs: Long get() {
        val lvl = upgradeLevels["scanner_cd"] ?: 0
        val reduction = SHOP_UPGRADES
            .first { it.id == "scanner_cd" }
            .levels.take(lvl)
            .sumOf { it.effectValue }
        return (BASE_SCANNER_CD_MS - reduction).coerceAtLeast(5_000L)
    }

    val maxCollectionSlots: Int get() {
        val lvl = upgradeLevels["backpack"] ?: 0
        if (lvl >= 5) return Int.MAX_VALUE
        val extra = SHOP_UPGRADES
            .first { it.id == "backpack" }
            .levels.take(lvl)
            .sumOf { it.effectValue }
        return (BASE_SLOTS + extra).toInt()
    }

    val radarRadiusM: Int get() {
        val lvl = upgradeLevels["radar"] ?: 0
        val extra = SHOP_UPGRADES
            .first { it.id == "radar" }
            .levels.take(lvl)
            .sumOf { it.effectValue }
        return (BASE_RADAR_M + extra).toInt()
    }

    val ecoBonusPercent: Int get() {
        val lvl = upgradeLevels["eco_bonus"] ?: 0
        if (lvl == 0) return 0
        return SHOP_UPGRADES
            .first { it.id == "eco_bonus" }
            .levels[lvl - 1].effectValue.toInt()
    }

    // ── КД ────────────────────────────────────────────────────────────────────

    fun scannerCdRemaining(): Long {
        val elapsed = System.currentTimeMillis() - lastScanTime
        var base = maxOf(0L, scannerCdMs - elapsed)
        if (isBoosterActive("cd_reset")) return 0L
        if (isBoosterActive("cd_half")) base /= 2
        return base
    }

    fun plantCdRemaining(plantId: Int): Long {
        val t = scannedPlants[plantId] ?: return 0L
        return maxOf(0L, PLANT_CD_MS - (System.currentTimeMillis() - t))
    }

    fun isBoosterActive(id: String): Boolean {
        val expiry = activeBoosters[id] ?: return false
        return System.currentTimeMillis() < expiry
    }

    // ── Покупка апгрейда ──────────────────────────────────────────────────────

    fun buyUpgrade(upgradeId: String): Boolean {
        val upgrade = SHOP_UPGRADES.firstOrNull { it.id == upgradeId } ?: return false
        val current = upgradeLevels[upgradeId] ?: 0
        if (current >= upgrade.maxLevel) return false
        val cost = upgrade.levels[current].cost
        if (ecoBalance < cost) return false
        ecoBalance -= cost
        upgradeLevels[upgradeId] = current + 1
        return true
    }

    // ── Покупка бустера ───────────────────────────────────────────────────────

    fun buyBooster(booster: BoosterItem): Boolean {
        if (ecoBalance < booster.cost) return false
        ecoBalance -= booster.cost
        when (booster.id) {
            "cd_reset"   -> lastScanTime = 0L
            "cd_half"    -> activeBoosters["cd_half"]      = System.currentTimeMillis() + 5_000L
            "double_eco" -> activeBoosters["double_eco"]   = System.currentTimeMillis() + 3_600_000L
            "rare_boost" -> activeBoosters["rare_boost"]   = System.currentTimeMillis() + 1_800_000L
        }
        return true
    }

    // ── Запись скана ──────────────────────────────────────────────────────────

    fun recordScan(card: EcoCard) {
        lastScanTime = System.currentTimeMillis()
        scannedPlants[card.id] = System.currentTimeMillis()

        if (collection.none { it.id == card.id } && collection.size < maxCollectionSlots) {
            collection = collection + card
        }

        val base       = card.rarity.multiplier
        val withStreak = (base * streakMultiplier).toInt()
        val withBooster = if (isBoosterActive("double_eco")) withStreak * 2 else withStreak
        val earned     = withBooster + (withBooster * ecoBonusPercent / 100)
        ecoBalance += earned
        xp += 50 * card.rarity.multiplier

        updateStreak()
        updateQuestProgress(card)
    }

    // ── Стрик ─────────────────────────────────────────────────────────────────

    private fun updateStreak() {
        val today = LocalDate.now().toString()
        if (lastStreakDate == today) return
        val yesterday = LocalDate.now().minusDays(1).toString()
        streak = if (lastStreakDate == yesterday) streak + 1 else 1
        lastStreakDate = today
        refreshDailyQuests()
    }

    private fun refreshDailyQuests() {
        val today = LocalDate.now().toString()
        if (questDate == today) return
        questDate = today
        questProgress.clear()
        completedQuests.clear()
    }

    // ── Квесты ────────────────────────────────────────────────────────────────

    private fun updateQuestProgress(card: EcoCard) {
        if (card.emoji in listOf("🌸", "🌺", "🌹", "🌻")) incrementQuest("q_flowers")
        if (card.rarity != Rarity.COMMON) incrementQuest("q_rare")
        if (card.rarity == Rarity.LEGENDARY) incrementQuest("q_legendary")
        incrementQuest("q_scan5")
    }

    fun incrementQuest(questId: String) {
        if (completedQuests[questId] == true) return
        val quest = DAILY_QUESTS.firstOrNull { it.id == questId } ?: return
        val current = (questProgress[questId] ?: 0) + 1
        questProgress[questId] = current
        if (current >= quest.target) {
            completedQuests[questId] = true
            ecoBalance += quest.reward
        }
    }

    fun questProgressFloat(questId: String): Float {
        val quest = DAILY_QUESTS.firstOrNull { it.id == questId } ?: return 0f
        val current = questProgress[questId] ?: 0
        return (current.toFloat() / quest.target.toFloat()).coerceIn(0f, 1f)
    }

    fun isQuestDone(questId: String): Boolean = completedQuests[questId] == true

    // ── Форматирование КД ─────────────────────────────────────────────────────

    fun formatCd(ms: Long): String {
        if (ms <= 0L) return ""
        val totalSec = ms / 1000
        val hours = totalSec / 3600
        val mins  = (totalSec % 3600) / 60
        val secs  = totalSec % 60
        return when {
            hours > 0 -> "${hours}ч ${mins}м"
            mins  > 0 -> "${mins}м ${secs}с"
            else      -> "${secs}с"
        }
    }
}