package com.example.data

data class SocialRank(
    val title: String,
    val icon: String,
    val description: String,
    val level: Int = 1
)

object SocialHierarchy {
    val ranks: List<SocialRank> = listOf(
        SocialRank("Peasant", "🌾", "A lowly farmhand working the royal lands.", 1),
        SocialRank("Thief", "🗡️", "A cunning rogue navigating shadow alleys.", 2),
        SocialRank("Merchant", "🪙", "A prosperous trader of fine silk and spices.", 3),
        SocialRank("Priest", "✝️", "A pious monk serving the Church and peasantry.", 4),
        SocialRank("Knight", "⚔️", "A sworn protector of the Realm in shining armor.", 5),
        SocialRank("Mayor", "📜", "A magistrate governing prosperous towns and fiefs.", 6),
        SocialRank("Baron", "🏰", "A lord managing vast provincial territories.", 7),
        SocialRank("Marquis", "🛡️", "A high noble commanding royal border armies.", 8),
        SocialRank("Monarch", "👑", "The supreme ruler of the entire Kingdom!", 9)
    )

    fun initialRank(): SocialRank = ranks.first()

    fun getNextRank(current: SocialRank): SocialRank? {
        val index = ranks.indexOfFirst { it.title.equals(current.title, ignoreCase = true) }
        return if (index != -1 && index + 1 < ranks.size) ranks[index + 1] else null
    }
}

enum class FactionKey(val displayName: String, val icon: String, val description: String) {
    PEASANTS("Peasants", "🌾", "Common folk and countryside laborers."),
    CHURCH("Church", "✝️", "The Holy Clergy and monastic orders."),
    NOBILITY("Nobility", "👑", "Lords, ladies, and royal bloodlines."),
    UNDERWORLD("Underworld", "🗡️", "Outlaws, thieves, and shadow guilds."),
    MERCHANTS("Merchants", "🪙", "Caravan masters, bankers, and traders.")
}

data class InventoryItem(
    val id: String,
    val name: String,
    val icon: String,
    val description: String,
    val statBonus: String = ""
)

data class PropertyAsset(
    val id: String,
    val name: String,
    val icon: String,
    val description: String,
    val passiveGoldIncome: Int = 5
)

data class EventOption(
    val id: Int,
    val text: String,
    val tag: String = "Action",
    val cardArchetype: String = "Peasant_Action", // "Peasant_Action", "Merchant_Action", "Noble_Action", "Church_Action", "Underworld_Action"
    /** Set when this option comes from the authored EventDeck rather than AI/offline generation. */
    val sourceNodeId: String? = null,
    val sourceChoiceId: Int? = null
)

data class StatChanges(
    val goldChange: Int = 0,
    val healthChange: Int = 0,
    val socialProgressChange: Int = 0,
    val statusEffect: String? = null,
    val factionChanges: Map<String, Int> = emptyMap(), // e.g., "Church" to 10, "Underworld" to -5
    val regionalTensionChange: Int = 0,
    val notorietyChange: Int = 0,
    val itemGained: InventoryItem? = null,
    val itemLostName: String? = null,
    val assetGained: PropertyAsset? = null,
    val chapterFlag: String? = null
)

data class EventResponse(
    val internal_reasoning: String = "",
    val resolutionText: String = "",
    val bridgeText: String = "",
    val nextEventTitle: String = "A Shadow Falls",
    val nextEventText: String = "You tread carefully through the medieval realm...",
    val location: String = "Village", // Forest, Village, Tavern, Castle, Cathedral, Marketplace
    val npcName: String = "Local Wanderer",
    val npcTitle: String = "Oakvale Resident",
    val npcArchetype: String = "PEASANT", // PEASANT, MERCHANT, KNIGHT, BISHOP, ALCHEMIST, BANDIT, NOBLE, MONARCH
    val statChanges: StatChanges = StatChanges(),
    val newWorldFlags: List<String> = emptyList(),
    val options: List<EventOption> = emptyList(),
    val updatedActiveSceneContext: String? = null,
    val updatedActiveNpc: String? = null
) {
    val title: String get() = nextEventTitle
    val text: String get() = nextEventText
}

data class ChronicleEntry(
    val turn: Int,
    val eventTitle: String,
    val chosenOptionText: String,
    val outcomeDescription: String,
    val statChangesText: String
)

data class GameState(
    val playerName: String = "Sir Robin",
    val socialClass: SocialRank = SocialHierarchy.initialRank(),
    val gold: Int = 25,
    val health: Int = 100,
    val maxHealth: Int = 100,
    val socialProgress: Int = 10, // 0 to 100% towards next rank
    val factionReputation: Map<String, Int> = mapOf(
        "Peasants" to 60,
        "Church" to 50,
        "Nobility" to 30,
        "Underworld" to 40,
        "Merchants" to 45
    ),
    val inventory: List<InventoryItem> = listOf(
        InventoryItem("wooden_staff", "Wooden Staff", "🦯", "A sturdy oak traveling staff.", "+5 Defense")
    ),
    val properties: List<PropertyAsset> = emptyList(),
    val flags: List<String> = listOf("Humble Beginnings"),
    val chapterFlags: List<String> = emptyList(), // Persistent chapter carry-over choices
    val turnCount: Int = 1,
    val isGameOver: Boolean = false,
    val isVictory: Boolean = false,
    val gameOverReason: String? = null,
    val lastStatChanges: StatChanges? = null,
    val chronicle: List<ChronicleEntry> = emptyList()
) {
    val totalPassiveGoldIncome: Int
        get() = properties.sumOf { it.passiveGoldIncome }
}
