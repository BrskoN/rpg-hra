package com.example.data

enum class OriginClass(
    val title: String,
    val subtitle: String,
    val description: String,
    val baseGold: Int,
    val baseHealth: Int,
    val initialFactions: Map<Faction, Int>,
    val heraldicSymbol: String,
    val reqDescription: String = "Available at start"
) {
    PEASANT(
        title = "Peasant",
        subtitle = "Common Laborer",
        description = "Lowly farmer with humble roots, strong endurance, and deep ties to the countryside folk.",
        baseGold = 15,
        baseHealth = 100,
        initialFactions = mapOf(
            Faction.PEASANTS to 75,
            Faction.CHURCH to 50,
            Faction.NOBILITY to 20,
            Faction.UNDERWORLD to 40,
            Faction.GUILDS to 30
        ),
        heraldicSymbol = "P",
        reqDescription = "Standard Origin"
    ),
    GUILD_APPRENTICE(
        title = "Guild Apprentice",
        subtitle = "Craftsman & Trader",
        description = "Tradesman skilled in craft, ledger books, contracts, and market coinage.",
        baseGold = 45,
        baseHealth = 85,
        initialFactions = mapOf(
            Faction.PEASANTS to 40,
            Faction.CHURCH to 45,
            Faction.NOBILITY to 35,
            Faction.UNDERWORLD to 30,
            Faction.GUILDS to 75
        ),
        heraldicSymbol = "G",
        reqDescription = "Standard Origin"
    ),
    ACOLYTE(
        title = "Acolyte",
        subtitle = "Holy Initiate",
        description = "Devout initiate educated in holy scriptures, cathedral rituals, and sacred vows.",
        baseGold = 25,
        baseHealth = 95,
        initialFactions = mapOf(
            Faction.PEASANTS to 55,
            Faction.CHURCH to 80,
            Faction.NOBILITY to 40,
            Faction.UNDERWORLD to 15,
            Faction.GUILDS to 35
        ),
        heraldicSymbol = "A",
        reqDescription = "Standard Origin"
    ),
    KNIGHT(
        title = "Knight Banneret",
        subtitle = "Chivalric Lord",
        description = "Sworn champion of the realm, wielding military might and high noble standing.",
        baseGold = 90,
        baseHealth = 130,
        initialFactions = mapOf(
            Faction.PEASANTS to 35,
            Faction.CHURCH to 65,
            Faction.NOBILITY to 85,
            Faction.UNDERWORLD to 10,
            Faction.GUILDS to 50
        ),
        heraldicSymbol = "K",
        reqDescription = "Unlocked via Nobility >= 65 or 'CROWN_FAVOR' / 'HERO_OF_OAKVALE'"
    ),
    MASTER_MERCHANT(
        title = "Master Merchant",
        subtitle = "Guildmaster & Patrician",
        description = "Wealthy patron controlling trade routes, charter laws, and merchant guilds.",
        baseGold = 150,
        baseHealth = 90,
        initialFactions = mapOf(
            Faction.PEASANTS to 30,
            Faction.CHURCH to 40,
            Faction.NOBILITY to 60,
            Faction.UNDERWORLD to 45,
            Faction.GUILDS to 90
        ),
        heraldicSymbol = "M",
        reqDescription = "Unlocked via Guilds >= 65 or 'GUILD_MASTER' / 'MERCHANT_PATRON'"
    ),
    BISHOP(
        title = "High Prelate",
        subtitle = "Cathedral Ecclesiast",
        description = "Mighty church leader with ecclesiastical authority over inquisitions and tithes.",
        baseGold = 70,
        baseHealth = 110,
        initialFactions = mapOf(
            Faction.PEASANTS to 60,
            Faction.CHURCH to 90,
            Faction.NOBILITY to 70,
            Faction.UNDERWORLD to 5,
            Faction.GUILDS to 45
        ),
        heraldicSymbol = "B",
        reqDescription = "Unlocked via Church >= 65 or 'HOLY_CRUSADER' / 'SACRED_VOW'"
    ),
    SQUIRE(
        title = "Squire",
        subtitle = "Armed Retainer",
        description = "A commoner raised into the Lord's household guard, trading the plow for a blade and a master's leash.",
        baseGold = 40,
        baseHealth = 105,
        initialFactions = mapOf(
            Faction.PEASANTS to 30,
            Faction.CHURCH to 45,
            Faction.NOBILITY to 70,
            Faction.UNDERWORLD to 15,
            Faction.GUILDS to 35
        ),
        heraldicSymbol = "⚔",
        reqDescription = "Unlocked via 'MAN_AT_ARMS' or 'ENFORCER_OF_TYRANNY'"
    ),
    OUTLAW_KING(
        title = "Shadow Monarch",
        subtitle = "Underworld Chieftain",
        description = "Ruler of covert syndicates, night markets, and assassin leagues.",
        baseGold = 110,
        baseHealth = 115,
        initialFactions = mapOf(
            Faction.PEASANTS to 45,
            Faction.CHURCH to 10,
            Faction.NOBILITY to 15,
            Faction.UNDERWORLD to 90,
            Faction.GUILDS to 55
        ),
        heraldicSymbol = "S",
        reqDescription = "Unlocked via Underworld >= 65 or 'KILLED_BAILIFF' / 'SHADOW_DEAL'"
    ),
    PRISONER(
        title = "Dungeon Prisoner",
        subtitle = "Chained Captive",
        description = "Thrown into iron shackles after a disastrous turn of fate. Stripped of coin and liberty, but hardened by misery.",
        baseGold = 0,
        baseHealth = 50,
        initialFactions = mapOf(
            Faction.PEASANTS to 40,
            Faction.CHURCH to 20,
            Faction.NOBILITY to 5,
            Faction.UNDERWORLD to 60,
            Faction.GUILDS to 10
        ),
        heraldicSymbol = "⛓️",
        reqDescription = "Punishment role: Triggered by high notoriety, crippling debt, or crown crimes."
    ),
    BEGGAR(
        title = "Street Beggar",
        subtitle = "Destitute Commoner",
        description = "Stripped of all fortune and standing, reduced to begging for stale crusts outside the cathedral gates.",
        baseGold = 5,
        baseHealth = 70,
        initialFactions = mapOf(
            Faction.PEASANTS to 60,
            Faction.CHURCH to 50,
            Faction.NOBILITY to 10,
            Faction.UNDERWORLD to 50,
            Faction.GUILDS to 15
        ),
        heraldicSymbol = "🥣",
        reqDescription = "Punishment role: Triggered by extreme poverty, bankruptcy, or regional ruin."
    ),
    OUTCAST(
        title = "Banishment Outcast",
        subtitle = "Exiled Wanderer",
        description = "Cast out from civilized fiefdoms into desolate waste lands. Distrusted by high lords and churchmen alike.",
        baseGold = 10,
        baseHealth = 80,
        initialFactions = mapOf(
            Faction.PEASANTS to 45,
            Faction.CHURCH to 10,
            Faction.NOBILITY to 10,
            Faction.UNDERWORLD to 70,
            Faction.GUILDS to 20
        ),
        heraldicSymbol = "🏕️",
        reqDescription = "Punishment role: Triggered by severe heresy, murder of officials, or outlawry."
    )
}

enum class AppLanguage(val code: String, val displayName: String) {
    SLOVAK("SK", "Slovenčina"),
    ENGLISH("EN", "English")
}

enum class Faction(val displayName: String, val iconSymbol: String) {
    PEASANTS("Peasants", "🌾"),
    CHURCH("Church", "✝️"),
    NOBILITY("Nobility", "👑"),
    UNDERWORLD("Underworld", "🗡️"),
    GUILDS("Guilds", "⚖️")
}

enum class TurnPhase {
    ACTION_SELECTION,
    RESOLUTION,
    NARRATIVE_BRIDGE,
    CHAPTER_ASCENSION
}

enum class EnvironmentCondition(val displayName: String, val description: String) {
    HARSH_WINTER("Harsh Winter", "Freezing blizzards lock roads and starve livestock."),
    PLAGUE("Festering Plague", "Contagious fever ravages countryside hamlets and garrisons."),
    WAR_DRAFT("Royal War Draft", "Crown officers seize grain and draft peasants into frontline levies."),
    DRY_SUMMER("Dry Scorching Drought", "Dried wells and withered crops push peasants to rebellion."),
    FEUDAL_PEACE("Uneasy Feudal Peace", "A fragile truce holds across fiefdoms, masking underlying conspiracies.")
}

data class WorldState(
    val currentChapter: Int = 1,
    val activeOrigin: OriginClass = OriginClass.PEASANT,
    val selectedLanguage: AppLanguage = AppLanguage.SLOVAK,
    val factions: Map<Faction, Int> = mapOf(
        Faction.PEASANTS to 50,
        Faction.CHURCH to 50,
        Faction.NOBILITY to 50,
        Faction.UNDERWORLD to 50,
        Faction.GUILDS to 50
    ),
    val gold: Int = 25,
    val health: Int = 100,
    val worldFlags: Set<String> = emptySet(),
    val regionalTension: Int = 20, // 0 to 100
    val notoriety: Int = 10, // 0 to 100
    val environment: EnvironmentCondition = EnvironmentCondition.HARSH_WINTER,
    val maxHealth: Int = 100,
    val turnCount: Int = 1,
    val isGameOver: Boolean = false,
    val isVictory: Boolean = false,
    val gameOverReason: String? = null,
    val lastResolutionText: String? = null,
    val lastBridgeText: String? = null,
    val lastStatChanges: StatChanges? = null,
    val lastChosenOptionText: String? = null,
    val currentAnchorContext: String? = null,
    val recentCharactersMet: List<String> = emptyList(),
    val lastActionConsequenceSummary: String = "",
    val currentActiveSceneContext: String? = null,
    val currentActiveNpc: String? = null,
    val activeSceneTurns: Int = 0,
    val currentNodeId: String? = null,
    val visitedNodeIds: Set<String> = emptySet(),
    /** Simple item-id ledger for EventDeck gating (e.g. "Forged_Pass", "Stolen_Relic"). */
    val inventoryItemIds: Set<String> = emptySet(),
    /** Hidden personal influence trackers distinct from faction reputation (e.g. "Underworld_Affinity", "Church_Grace"). */
    val hiddenInfluences: Map<String, Int> = emptyMap()
)

fun OriginClass.getLocalizedTitle(lang: AppLanguage): String = when (lang) {
    AppLanguage.SLOVAK -> when (this) {
        OriginClass.PEASANT -> "Roľník"
        OriginClass.GUILD_APPRENTICE -> "Cechový Učeň"
        OriginClass.ACOLYTE -> "Akolyt"
        OriginClass.SQUIRE -> "Zbrojnoš"
        OriginClass.KNIGHT -> "Rytier Banneret"
        OriginClass.MASTER_MERCHANT -> "Cechmajster Kupiec"
        OriginClass.BISHOP -> "Vysoký Prelát"
        OriginClass.OUTLAW_KING -> "Tieňový Monarcha"
        OriginClass.PRISONER -> "Väzeň v Žalári"
        OriginClass.BEGGAR -> "Mestský Žobrák"
        OriginClass.OUTCAST -> "Vyhnanec"
    }
    AppLanguage.ENGLISH -> this.title
}

fun OriginClass.getLocalizedSubtitle(lang: AppLanguage): String = when (lang) {
    AppLanguage.SLOVAK -> when (this) {
        OriginClass.PEASANT -> "Obyčajný Pracovník"
        OriginClass.GUILD_APPRENTICE -> "Remeselník a Obchodník"
        OriginClass.ACOLYTE -> "Svätý Zasvätenec"
        OriginClass.SQUIRE -> "Ozbrojený Sluha Pána"
        OriginClass.KNIGHT -> "Rytiersky Pán"
        OriginClass.MASTER_MERCHANT -> "Patricij a Cechmajster"
        OriginClass.BISHOP -> "Katedrálny Duchovný"
        OriginClass.OUTLAW_KING -> "Šéf Podsvetia"
        OriginClass.PRISONER -> "Pripútaný Zajatec"
        OriginClass.BEGGAR -> "Chudobný Poddaný"
        OriginClass.OUTCAST -> "Vyhnaný Pútnik"
    }
    AppLanguage.ENGLISH -> this.subtitle
}

fun OriginClass.getLocalizedDescription(lang: AppLanguage): String = when (lang) {
    AppLanguage.SLOVAK -> when (this) {
        OriginClass.PEASANT -> "Prostý roľník so skromnými koreňmi, silnou vytrvalosťou a hlbokými väzbami na vidiecky ľud."
        OriginClass.GUILD_APPRENTICE -> "Remeselník zručný v výrobe, účtovných knihách, zmluvách a trhovom obchode."
        OriginClass.ACOLYTE -> "Zasvätenec vzdelaný v svätých písmach, katedrálnych rituáloch a svätých sľuboch."
        OriginClass.SQUIRE -> "Poddaný povýšený do panskej domácej stráže, ktorý vymenil pluh za čepeľ a slobodu za pánov obojok."
        OriginClass.KNIGHT -> "Prísahou zviazaný šampión ríše, vládnuci vojenskou mocou a vysokým šľachtickým stavom."
        OriginClass.MASTER_MERCHANT -> "Bohatý patrón ovládajúci obchodné cesty, mestské práva a cechové spolky."
        OriginClass.BISHOP -> "Mocný cirkevný vodca s právomocami nad inkvizíciou a cirkevnými desiatkami."
        OriginClass.OUTLAW_KING -> "Vládca tajných spolkov, nočných trhov a cechu vrahov."
        OriginClass.PRISONER -> "Uvrhnutý do železných okov po katastrofálnom obrate osudu. Zbavený mincí a slobody, no zocelený biedou."
        OriginClass.BEGGAR -> "Zbavený všetkého majetku a postavenia, odkázaný na žobranie o staré kôrky chleba pred bránami katedrály."
        OriginClass.OUTCAST -> "Vyhnaný z civilizovaných panstiev do pustatiny. Nedôveryhodný pre pánov aj kňazov."
    }
    AppLanguage.ENGLISH -> this.description
}

fun Faction.getLocalizedName(lang: AppLanguage): String = when (lang) {
    AppLanguage.SLOVAK -> when (this) {
        Faction.PEASANTS -> "Poddaní"
        Faction.CHURCH -> "Cirkev"
        Faction.NOBILITY -> "Šľachta"
        Faction.UNDERWORLD -> "Podsvetie"
        Faction.GUILDS -> "Cechy"
    }
    AppLanguage.ENGLISH -> this.displayName
}

fun EnvironmentCondition.getLocalizedName(lang: AppLanguage): String = when (lang) {
    AppLanguage.SLOVAK -> when (this) {
        EnvironmentCondition.HARSH_WINTER -> "Krutá Zima"
        EnvironmentCondition.PLAGUE -> "Moringová Nákaza"
        EnvironmentCondition.WAR_DRAFT -> "Kráľovský Verbúnk"
        EnvironmentCondition.DRY_SUMMER -> "Priechodné Sucho"
        EnvironmentCondition.FEUDAL_PEACE -> "Krehké Feudálne Prímerie"
    }
    AppLanguage.ENGLISH -> this.displayName
}
