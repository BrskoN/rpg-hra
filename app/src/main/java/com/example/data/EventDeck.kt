package com.example.data

/**
 * Hybrid content engine: authored, historically-inspired branching event graph that is the single
 * source of truth for choices and their consequences. Gemini is no longer required to invent plot
 * or stat deltas for origins covered by a deck below - it only ever adds sensory flavor on top
 * (future enhancement), which keeps decisions/consequences scalable, offline-capable, and free of
 * AI drift.
 *
 * Design mirrors the "Chronicles of the Realm" Event Deck spec: each event carries triggers/flags,
 * causal flag-chains unlock later cards, and hidden hiddenInfluences (Church_Grace,
 * Underworld_Affinity, Rebel_Trust, Noble_Favor) drive mid-chapter social mobility - not just the
 * Turn 25 climax.
 */
enum class EventPhase {
    PHASE_1, // Turns 1-7: Local oppression & survival
    PHASE_2, // Turns 8-16: Social divergence
    PHASE_3  // Turns 17-25: Climax & causal reckoning
}

data class ChoiceConsequence(
    val goldChange: Int = 0,
    val healthChange: Int = 0,
    val regionalTensionChange: Int = 0,
    val notorietyChange: Int = 0,
    val factionChanges: Map<Faction, Int> = emptyMap(),
    val influenceChanges: Map<String, Int> = emptyMap(),
    val addFlags: Set<String> = emptySet(),
    val removeFlags: Set<String> = emptySet(),
    val addItems: Set<String> = emptySet(),
    val removeItems: Set<String> = emptySet(),
    val resolutionTextEn: String,
    val resolutionTextSk: String,
    val bridgeTextEn: String,
    val bridgeTextSk: String
)

data class EventChoice(
    val id: Int,
    val textEn: String,
    val textSk: String,
    val tagEn: String,
    val tagSk: String,
    val cardArchetype: String,
    /** Default outcome. */
    val consequence: ChoiceConsequence,
    /** Overrides [consequence] when the card's real-world requirement (item/reputation/flag) matters. */
    val conditionalOutcome: ((WorldState) -> ChoiceConsequence)? = null
) {
    fun resolve(world: WorldState): ChoiceConsequence = conditionalOutcome?.invoke(world) ?: consequence
}

data class EventNode(
    val id: String,
    val phase: EventPhase,
    val originClass: OriginClass? = null,
    val minTurn: Int = 1,
    val maxTurn: Int = 25,
    /** When true and eligible, this node is picked over the general phase pool (climax / crisis beats). */
    val forcedPriority: Boolean = false,
    val condition: (WorldState) -> Boolean = { true },
    val titleEn: String,
    val titleSk: String,
    val textEn: String,
    val textSk: String,
    val location: String,
    val npcName: String,
    val npcTitle: String,
    val npcArchetype: String,
    val choices: List<EventChoice>
) {
    fun canTrigger(world: WorldState): Boolean {
        if (originClass != null && originClass != world.activeOrigin) return false
        if (world.turnCount < minTurn || world.turnCount > maxTurn) return false
        return condition(world)
    }
}

object EventDeck {

    fun phaseForTurn(turn: Int): EventPhase = when {
        turn <= 7 -> EventPhase.PHASE_1
        turn <= 16 -> EventPhase.PHASE_2
        else -> EventPhase.PHASE_3
    }

    fun selectNode(world: WorldState): EventNode? {
        val phase = phaseForTurn(world.turnCount)
        val pool = nodesForOrigin(world.activeOrigin).filter { it.phase == phase }
        val eligible = pool.filter { it.id !in world.visitedNodeIds && it.canTrigger(world) }

        val forced = eligible.filter { it.forcedPriority }
        if (forced.isNotEmpty()) return forced.random()
        if (eligible.isNotEmpty()) return eligible.random()

        // Pool exhausted for this phase - allow repeats (except one-off climactic beats).
        val repeatable = pool.filter { !it.forcedPriority && it.canTrigger(world) }
        return repeatable.randomOrNull()
    }

    fun findNode(id: String): EventNode? = ALL_NODES.find { it.id == id }

    fun findChoice(nodeId: String, choiceId: Int): Pair<EventNode, EventChoice>? {
        val node = findNode(nodeId) ?: return null
        val choice = node.choices.find { it.id == choiceId } ?: return null
        return node to choice
    }

    /** Builds the resolution-only response for a chosen deck card (no next-scenario content). */
    fun buildChoiceResolution(nodeId: String, choiceId: Int, world: WorldState, lang: AppLanguage): EventResponse {
        val (node, choice) = findChoice(nodeId, choiceId)
            ?: return EventResponse(resolutionText = "", bridgeText = "")
        val c = choice.resolve(world)
        val isSlovak = lang == AppLanguage.SLOVAK
        return EventResponse(
            resolutionText = if (isSlovak) c.resolutionTextSk else c.resolutionTextEn,
            bridgeText = if (isSlovak) c.bridgeTextSk else c.bridgeTextEn,
            npcName = node.npcName,
            statChanges = StatChanges(
                goldChange = c.goldChange,
                healthChange = c.healthChange,
                regionalTensionChange = c.regionalTensionChange,
                notorietyChange = c.notorietyChange,
                factionChanges = c.factionChanges.mapKeys { it.key.name }
            ),
            newWorldFlags = c.addFlags.toList()
        )
    }

    /** Builds the "what happens next" response for a freshly selected node. */
    fun buildNodeResponse(node: EventNode, lang: AppLanguage): EventResponse {
        val isSlovak = lang == AppLanguage.SLOVAK
        val options = node.choices.map { choice ->
            EventOption(
                id = choice.id,
                text = if (isSlovak) choice.textSk else choice.textEn,
                tag = if (isSlovak) choice.tagSk else choice.tagEn,
                cardArchetype = choice.cardArchetype,
                sourceNodeId = node.id,
                sourceChoiceId = choice.id
            )
        }
        return EventResponse(
            nextEventTitle = if (isSlovak) node.titleSk else node.titleEn,
            nextEventText = if (isSlovak) node.textSk else node.textEn,
            location = node.location,
            npcName = node.npcName,
            npcTitle = node.npcTitle,
            npcArchetype = node.npcArchetype,
            options = options
        )
    }

    private fun nodesForOrigin(origin: OriginClass): List<EventNode> = when (origin) {
        OriginClass.PEASANT -> PEASANT_NODES
        OriginClass.ACOLYTE -> ACOLYTE_NODES
        OriginClass.GUILD_APPRENTICE -> GUILD_NODES
        else -> emptyList()
    }

    val ALL_NODES: List<EventNode> get() = PEASANT_NODES + ACOLYTE_NODES + GUILD_NODES

    // Hidden influence keys used across the deck.
    private const val REBEL_TRUST = "Rebel_Trust"
    private const val NOBLE_FAVOR = "Noble_Favor"
    private const val CHURCH_GRACE = "Church_Grace"
    private const val UNDERWORLD_AFFINITY = "Underworld_Affinity"

    private fun influence(world: WorldState, key: String): Int = world.hiddenInfluences[key] ?: 0

    // ---------------------------------------------------------------------
    // PEASANT CHAPTER 1 DECK
    // ---------------------------------------------------------------------
    private val PEASANT_NODES: List<EventNode> = listOf(

        // ============ PHASE 1: LOKÁLNY ÚTLAK A PREŽITIE (Ťahy 1-7) ============

        EventNode(
            id = "p1_tax_measure",
            phase = EventPhase.PHASE_1,
            originClass = OriginClass.PEASANT,
            minTurn = 1, maxTurn = 1,
            forcedPriority = true,
            titleEn = "The Winter Tithe and the Rigged Measure",
            titleSk = "Zimný Desiatok a Panská Miera",
            textEn = "The manor official measures your grain with his own, tampered bushel. If you hand over what he demands, your family starves before spring.",
            textSk = "Panský úradník meria tvoje obilie vlastným, zmanipulovaným mercom. Ak mu odovzdáš, čo žiada, tvoja rodina do jari pomrie hladom.",
            location = "Village",
            npcName = "Manor Official Grendel",
            npcTitle = "Tithe Assessor",
            npcArchetype = "NOBLE",
            choices = listOf(
                EventChoice(
                    id = 1, textEn = "Bribe the guard escorting him", textSk = "Podplatiť strážcu, čo ho sprevádza",
                    tagEn = "Bribe", tagSk = "Úplatok", cardArchetype = "Peasant_Action",
                    consequence = ChoiceConsequence(
                        goldChange = -10, influenceChanges = mapOf(CHURCH_GRACE to 5),
                        addFlags = setOf("BRIBED_OFFICIAL"),
                        resolutionTextEn = "The guard's palm closes around your coin and he suddenly finds the tally satisfactory.",
                        resolutionTextSk = "Strážcova dlaň sa zovrie okolo mince a zrazu mu súčet vychádza.",
                        bridgeTextEn = "Your grain stays yours, but your purse is bare down to the last copper.",
                        bridgeTextSk = "Obilie ti ostáva, no mešec máš prázdny do poslednej medenej mince."
                    )
                ),
                EventChoice(
                    id = 2, textEn = "Publicly accuse him of fraud", textSk = "Verejne ho obviniť z podvodu",
                    tagEn = "Accusation", tagSk = "Obvinenie", cardArchetype = "Peasant_Action",
                    consequence = ChoiceConsequence(
                        regionalTensionChange = 15,
                        factionChanges = mapOf(Faction.PEASANTS to 20, Faction.NOBILITY to -15),
                        influenceChanges = mapOf(REBEL_TRUST to 10),
                        addFlags = setOf("AGITATOR"),
                        resolutionTextEn = "Villagers murmur in agreement as Grendel's rigged bushel is exposed for all to see.",
                        resolutionTextSk = "Dedinčania súhlasne mrmlú, keď Grendelov sfalšovaný merec odhalíš pred všetkými.",
                        bridgeTextEn = "Word of your defiance will reach the manor before nightfall.",
                        bridgeTextSk = "Chýr o tvojom vzdore sa do súmraku dostane až na panstvo."
                    )
                ),
                EventChoice(
                    id = 3, textEn = "Stay silent and eat your own seed grain", textSk = "Mlčať a zjesť vlastné osivo",
                    tagEn = "Silence", tagSk = "Mlčanie", cardArchetype = "Peasant_Action",
                    consequence = ChoiceConsequence(
                        healthChange = -20, factionChanges = mapOf(Faction.NOBILITY to 10),
                        addFlags = setOf("STARVING"),
                        resolutionTextEn = "Grendel nods approvingly and moves his cart along without further trouble.",
                        resolutionTextSk = "Grendel spokojne prikývne a s vozom pokračuje ďalej bez ďalších problémov.",
                        bridgeTextEn = "Next season's seed grain now fills your empty belly instead of the soil.",
                        bridgeTextSk = "Osivo na budúcu sezónu ti teraz napĺňa prázdny žalúdok namiesto pôdy."
                    )
                )
            )
        ),

        EventNode(
            id = "p1_black_fever",
            phase = EventPhase.PHASE_1,
            originClass = OriginClass.PEASANT,
            minTurn = 2, maxTurn = 5,
            titleEn = "The Black Fever in the Settlement",
            titleSk = "Čierna Horúčka v Osade",
            textEn = "Your neighbor's daughter has fallen ill. The village priest wants to burn her cottage down with the family inside so the 'curse' cannot spread.",
            textSk = "Susedova dcéra ochorela. Dedinský kňaz chce jej chalupu spáliť aj s rodinou vnútri, aby sa 'kliatba' nešírila.",
            location = "Village",
            npcName = "Father Ambrose",
            npcTitle = "Parish Priest",
            npcArchetype = "ELDER",
            choices = listOf(
                EventChoice(
                    id = 1, textEn = "Secretly hide the girl in the woods", textSk = "Pomôcť dievča tajne ukryť v lese",
                    tagEn = "Rescue", tagSk = "Záchrana", cardArchetype = "Underworld_Action",
                    consequence = ChoiceConsequence(
                        healthChange = -10, factionChanges = mapOf(Faction.PEASANTS to 15),
                        influenceChanges = mapOf(UNDERWORLD_AFFINITY to 5),
                        addFlags = setOf("SAVED_OUTCAST"),
                        resolutionTextEn = "You slip the fevered girl out through the back fence as Ambrose's torchbearers gather at the front door.",
                        resolutionTextSk = "Vyvedieš horúčkou zmorenú dievčinu zadným plotom, kým sa Ambrosovi nosiči fakieľ zhromažďujú pri predných dverách.",
                        bridgeTextEn = "Someone in the shadows saw what you did - and will remember it.",
                        bridgeTextSk = "Niekto v tieni videl, čo si urobil - a zapamätá si to."
                    )
                ),
                EventChoice(
                    id = 2, textEn = "Help the priest bar the door", textSk = "Pomôcť kňazovi zatvoriť dvere",
                    tagEn = "Zealotry", tagSk = "Horlivosť", cardArchetype = "Church_Action",
                    consequence = ChoiceConsequence(
                        factionChanges = mapOf(Faction.CHURCH to 15, Faction.PEASANTS to -20),
                        addFlags = setOf("PIOUS_ZEALOT"),
                        resolutionTextEn = "The bar drops into place as muffled coughing fades behind the shuttered cottage.",
                        resolutionTextSk = "Závora zapadne na miesto, kým za zatvorenou chalupou dohasína tlmený kašeľ.",
                        bridgeTextEn = "Ambrose blesses your hands for their part in cleansing the settlement.",
                        bridgeTextSk = "Ambrose požehná tvoje ruky za ich podiel na očiste osady."
                    )
                ),
                EventChoice(
                    id = 3, textEn = "Use the chaos to loot their stores", textSk = "Využiť chaos a ukradnúť ich zásoby",
                    tagEn = "Looting", tagSk = "Rabovanie", cardArchetype = "Underworld_Action",
                    consequence = ChoiceConsequence(
                        goldChange = 20, notorietyChange = 10, factionChanges = mapOf(Faction.PEASANTS to -20),
                        addFlags = setOf("SCOURGE_SCAVENGER"),
                        resolutionTextEn = "While the crowd gathers to watch the pyre, you slip through their unguarded larder.",
                        resolutionTextSk = "Kým sa dav zhromažďuje sledovať hranicu, prekĺzneš cez ich nestráženú špajzu.",
                        bridgeTextEn = "The stolen sacks weigh heavy - heavier still is the smoke rising behind you.",
                        bridgeTextSk = "Ukradnuté vrecia ťažia, no ešte ťažší je dym stúpajúci za tvojím chrbtom."
                    )
                )
            )
        ),

        EventNode(
            id = "p1_lords_hunt",
            phase = EventPhase.PHASE_1,
            originClass = OriginClass.PEASANT,
            titleEn = "The Lord's Hunt and the Trampled Field",
            titleSk = "Panská Poľovačka a Pošliapané Pole",
            textEn = "The Lord and his retinue thundered on horseback straight through your only wheat field chasing a stag. His squire tosses you a copper coin for the damage.",
            textSk = "Lord so svojou družinou preletel na koňoch cez tvoje jediné pšeničné pole pri naháňaní jeleňa. Jeho zbrojnoš ti hodí medenák za škodu.",
            location = "Village",
            npcName = "Squire Dobbs",
            npcTitle = "Lord's Retinue",
            npcArchetype = "NOBLE",
            choices = listOf(
                EventChoice(
                    id = 1, textEn = "Humbly accept and kiss his boot", textSk = "S pokorou prijať medenák a bozkávať čižmu",
                    tagEn = "Submission", tagSk = "Pokora", cardArchetype = "Peasant_Action",
                    consequence = ChoiceConsequence(
                        goldChange = 2, factionChanges = mapOf(Faction.PEASANTS to -10, Faction.NOBILITY to 5),
                        influenceChanges = mapOf(NOBLE_FAVOR to 5),
                        addFlags = setOf("SUBMISSIVE"),
                        resolutionTextEn = "Dobbs smirks as you bow low, satisfied that the matter is settled.",
                        resolutionTextSk = "Dobbs sa uškrnie, kým sa nízko ukloníš, spokojný, že vec je vybavená.",
                        bridgeTextEn = "The retinue rides on, leaving hoofprints across your ruined field.",
                        bridgeTextSk = "Družina odcvála ďalej a zanechá kopytá naprieč tvojím zničeným poľom."
                    )
                ),
                EventChoice(
                    id = 2, textEn = "Scream curses at the Lord's bloodline", textSk = "Vreštiť na lorda a prekliať jeho rod",
                    tagEn = "Defiance", tagSk = "Vzdor", cardArchetype = "Peasant_Action",
                    consequence = ChoiceConsequence(
                        regionalTensionChange = 25, notorietyChange = 20, healthChange = -30,
                        addFlags = setOf("LOGGED_REBEL"),
                        resolutionTextEn = "Dobbs' fists find your ribs before the words even finish leaving your mouth.",
                        resolutionTextSk = "Dobbsove päste ti nájdu rebrá skôr, než slová vôbec dopovieš.",
                        bridgeTextEn = "You are marked as a rebellious tongue in the manor's ledger.",
                        bridgeTextSk = "Si zapísaný ako buričský jazyk v panskej knihe."
                    )
                ),
                EventChoice(
                    id = 3, textEn = "Quietly kill the wounded hound left behind", textSk = "Potajomky zabiť zraneného panského psa, čo zaostal",
                    tagEn = "Poaching", tagSk = "Pytliactvo", cardArchetype = "Underworld_Action",
                    consequence = ChoiceConsequence(
                        healthChange = 10, influenceChanges = mapOf(UNDERWORLD_AFFINITY to 10),
                        addItems = setOf("Lord_Pedigree_Collar"),
                        addFlags = setOf("SECRET_POACHER"),
                        resolutionTextEn = "The hound's silver-worked collar comes free in your hand as its whimpers fall silent.",
                        resolutionTextSk = "Psí obojok so striebrom sa ti uvoľní v ruke, kým jeho skučanie stíchne.",
                        bridgeTextEn = "You bury the carcass and pocket the collar before anyone returns.",
                        bridgeTextSk = "Zdochlinu zahrabeš a obojok schováš skôr, než sa niekto vráti."
                    )
                )
            )
        ),

        EventNode(
            id = "p1_bloody_tracks",
            phase = EventPhase.PHASE_1,
            originClass = OriginClass.PEASANT,
            condition = { it.worldFlags.contains("STARVING") || it.worldFlags.contains("SECRET_POACHER") },
            titleEn = "Bloody Tracks in the Lord's Forest",
            titleSk = "Krvavé Stopy v Panskom Lese",
            textEn = "You stand in the frost over a set trap. The Lord's own taxman has stumbled into it. Footsteps of the gamekeeper approach through the dark.",
            textSk = "Stojíš v mraze nad chystanou pascou. Chytil sa do nej panský daňovec. V tme počuť kroky panského hájnika.",
            location = "Forest",
            npcName = "Gamekeeper Wulf",
            npcTitle = "Lord's Huntsman",
            npcArchetype = "KNIGHT",
            choices = listOf(
                EventChoice(
                    id = 1, textEn = "Cut the meat and flee into the dark", textSk = "Zrezať mäso a utiecť do tmy",
                    tagEn = "Flee", tagSk = "Útek", cardArchetype = "Underworld_Action",
                    consequence = ChoiceConsequence(
                        healthChange = 30, notorietyChange = 10,
                        addFlags = setOf("HUNTER_IN_DARK"),
                        resolutionTextEn = "You carve what you can and vanish before Wulf's lantern rounds the bend.",
                        resolutionTextSk = "Vyrežeš, čo sa dá, a zmizneš skôr, než Wulfova lampa obíde zákrutu.",
                        bridgeTextEn = "Meat for winter, but a hunter's eye now knows these woods hide a poacher.",
                        bridgeTextSk = "Mäso na zimu, no hájnikovo oko teraz vie, že tento les skrýva pytliaka."
                    )
                ),
                EventChoice(
                    id = 2, textEn = "Blame a neighbor for setting the trap", textSk = "Udať suseda, že pascu nastavil on",
                    tagEn = "Betrayal", tagSk = "Zrada", cardArchetype = "Noble_Action",
                    consequence = ChoiceConsequence(
                        goldChange = 10, factionChanges = mapOf(Faction.PEASANTS to -30, Faction.NOBILITY to 10),
                        addFlags = setOf("TRAITOR_NEIGHBOR"),
                        removeFlags = setOf("STARVING"),
                        resolutionTextEn = "Wulf drags your neighbor away in irons while a small reward coin warms your palm.",
                        resolutionTextSk = "Wulf odvedie tvojho suseda v okovách, kým ti v dlani hreje malá odmena.",
                        bridgeTextEn = "The reward silences your hunger, but the village will not forget your name.",
                        bridgeTextSk = "Odmena utíši tvoj hlad, no dedina tvoje meno nezabudne."
                    )
                ),
                EventChoice(
                    id = 3, textEn = "Ambush the gamekeeper from behind and kill him", textSk = "Prepadnúť hájnika zo zadu a zabiť ho",
                    tagEn = "Murder", tagSk = "Vražda", cardArchetype = "Underworld_Action",
                    consequence = ChoiceConsequence(
                        regionalTensionChange = 40, notorietyChange = 30, factionChanges = mapOf(Faction.PEASANTS to 15),
                        addFlags = setOf("BLOOD_ON_HANDS"),
                        addItems = setOf("Huntsman_Dagger"),
                        resolutionTextEn = "Wulf falls without a sound into the frozen bracken, his dagger yours now.",
                        resolutionTextSk = "Wulf padne bez zvuku do zamrznutého kapradia, jeho dýka je teraz tvoja.",
                        bridgeTextEn = "You drag the body into the undergrowth, hands trembling with what you've done.",
                        bridgeTextSk = "Telo vlečieš do podrastu, ruky sa ti trasú z toho, čo si spravil."
                    )
                )
            )
        ),

        // Gap-fill (not in the original document, added to complete the AGITATOR unlock hook)
        EventNode(
            id = "p1_town_executioner",
            phase = EventPhase.PHASE_1,
            originClass = OriginClass.PEASANT,
            minTurn = 3, maxTurn = 7,
            condition = { it.worldFlags.contains("AGITATOR") },
            titleEn = "The Town Executioner",
            titleSk = "Mestský Kat",
            textEn = "Word of your accusation against Grendel reached the manor. The town executioner has been sent to make an example of you before the assembled village.",
            textSk = "Chýr o tvojom obvinení Grendela sa dostal na panstvo. Mestský kat bol vyslaný, aby z teba urobil výstrahu pred zhromaždenou dedinou.",
            location = "Village",
            npcName = "Executioner Marrow",
            npcTitle = "The Lord's Enforcer",
            npcArchetype = "NOBLE",
            choices = listOf(
                EventChoice(
                    id = 1, textEn = "Publicly recant your accusation", textSk = "Verejne odvolať svoje obvinenie",
                    tagEn = "Recant", tagSk = "Odvolanie", cardArchetype = "Noble_Action",
                    consequence = ChoiceConsequence(
                        notorietyChange = -5, factionChanges = mapOf(Faction.PEASANTS to -10, Faction.NOBILITY to 10),
                        removeFlags = setOf("AGITATOR"),
                        resolutionTextEn = "Marrow lowers his rod as your recanted words satisfy the manor's honor.",
                        resolutionTextSk = "Marrow spustí prút, keď tvoje odvolané slová uspokoja panskú česť.",
                        bridgeTextEn = "The village watches your back turn from them as you walk away unscarred.",
                        bridgeTextSk = "Dedina sleduje, ako sa k nej otáčaš chrbtom, kým odchádzaš bez jazvy."
                    )
                ),
                EventChoice(
                    id = 2, textEn = "Stay defiantly silent under the lash", textSk = "Vzdorovito mlčať pod bičom",
                    tagEn = "Defiance", tagSk = "Vzdor", cardArchetype = "Peasant_Action",
                    consequence = ChoiceConsequence(
                        healthChange = -25, notorietyChange = 10, factionChanges = mapOf(Faction.PEASANTS to 20),
                        influenceChanges = mapOf(REBEL_TRUST to 15),
                        addFlags = setOf("SCARRED_MARTYR"),
                        resolutionTextEn = "Each stroke draws blood but not a word, and the crowd's murmur turns to reverence.",
                        resolutionTextSk = "Každý úder vytiahne krv, no ani slovo, a mrmlanie davu sa mení na úctu.",
                        bridgeTextEn = "Your scars will be remembered long after the welts fade.",
                        bridgeTextSk = "Tvoje jazvy si budú pamätať dlho po tom, čo opuchliny zmiznú."
                    )
                ),
                EventChoice(
                    id = 3, textEn = "Bribe the executioner to soften the blow", textSk = "Podplatiť kata, aby úder zmiernil",
                    tagEn = "Bribe", tagSk = "Úplatok", cardArchetype = "Merchant_Action",
                    consequence = ChoiceConsequence(
                        goldChange = -15, healthChange = -10,
                        addFlags = setOf("QUIET_MERCY"),
                        resolutionTextEn = "Marrow's rod strikes true to the eye but soft to the flesh as silver changes hands beforehand.",
                        resolutionTextSk = "Marrowov prút dopadá pravdivo pre oko, no mäkko pre telo, keď si vopred vymenil striebro.",
                        bridgeTextEn = "The village believes you suffered - only you and Marrow know otherwise.",
                        bridgeTextSk = "Dedina verí, že si trpel - len ty a Marrow viete, že to bolo inak."
                    )
                )
            )
        ),

        // ============ PHASE 2: SOCIÁLNA DIVERGENCIA (Ťahy 8-16) ============

        EventNode(
            id = "p2_deserter",
            phase = EventPhase.PHASE_2,
            originClass = OriginClass.PEASANT,
            titleEn = "The Deserter from the Crown's War",
            titleSk = "Zbeh z Kráľovskej Vojny",
            textEn = "A wounded mercenary hides in your barn. He carries a blood-stained purse with manor seals and travel papers.",
            textSk = "V tvojej stodole sa ukrýva zranený žoldnier. Má u seba krvavý mešec s panskými razidlami a listiny.",
            location = "Village",
            npcName = "Deserter Cain",
            npcTitle = "Wounded Mercenary",
            npcArchetype = "BANDIT",
            choices = listOf(
                EventChoice(
                    id = 1, textEn = "Tend his wounds for his purse and papers", textSk = "Ošetriť ho a vymeniť šaty za jeho mešec",
                    tagEn = "Trade", tagSk = "Výmena", cardArchetype = "Underworld_Action",
                    consequence = ChoiceConsequence(
                        goldChange = 40, influenceChanges = mapOf(UNDERWORLD_AFFINITY to 15),
                        addItems = setOf("Forged_Pass"),
                        addFlags = setOf("HARBORS_DESERTERS"),
                        resolutionTextEn = "Cain presses the sealed papers into your hand, grateful and gone by morning.",
                        resolutionTextSk = "Cain ti vtlačí zapečatené listiny do ruky, vďačný a do rána preč.",
                        bridgeTextEn = "A forged pass now rests hidden beneath your floorboards.",
                        bridgeTextSk = "Falošný priepustok teraz odpočíva ukrytý pod tvojimi doskami."
                    )
                ),
                EventChoice(
                    id = 2, textEn = "Bind him and hand him to the bailiffs", textSk = "Spútať ho a vydať panským drábom",
                    tagEn = "Loyalty", tagSk = "Vernosť", cardArchetype = "Noble_Action",
                    consequence = ChoiceConsequence(
                        goldChange = 20, factionChanges = mapOf(Faction.NOBILITY to 20, Faction.CHURCH to 10),
                        influenceChanges = mapOf(NOBLE_FAVOR to 10),
                        addFlags = setOf("CROWN_INFORMANT"),
                        resolutionTextEn = "The bailiffs drag Cain away as they count coin into your palm.",
                        resolutionTextSk = "Drábi odvlečú Caina preč, kým ti do dlane počítajú mince.",
                        bridgeTextEn = "The manor now considers you a reliable pair of eyes.",
                        bridgeTextSk = "Panstvo ťa teraz považuje za spoľahlivé oči."
                    )
                ),
                EventChoice(
                    id = 3, textEn = "Slit his throat in his sleep and take all", textSk = "Podrezať mu hrdlo v spánku a zobrať všetko",
                    tagEn = "Murder", tagSk = "Vražda", cardArchetype = "Underworld_Action",
                    consequence = ChoiceConsequence(
                        goldChange = 50, notorietyChange = 20, regionalTensionChange = 10,
                        addFlags = setOf("COLD_MURDERER"),
                        resolutionTextEn = "Cain never wakes. His purse, papers, and blade are yours by dawn.",
                        resolutionTextSk = "Cain sa už nezobudí. Jeho mešec, listiny aj čepeľ sú do rána tvoje.",
                        bridgeTextEn = "You bury him beneath the hay before the cock crows.",
                        bridgeTextSk = "Pochováš ho pod senom skôr, než zaspieva kohút."
                    )
                )
            )
        ),

        EventNode(
            id = "p2_indulgences",
            phase = EventPhase.PHASE_2,
            originClass = OriginClass.PEASANT,
            condition = { (it.factions[Faction.CHURCH] ?: 50) < 40 || it.worldFlags.contains("BLOOD_ON_HANDS") },
            titleEn = "The Purchase of Indulgences and the Plague Procession",
            titleSk = "Kúpa Odpustkov a Morový Sprievod",
            textEn = "An inquisitorial wagon has arrived in the village. The priest sells a 'certificate of absolution' that protects against the torture chamber.",
            textSk = "Do dediny dorazil inkvizičný vozeň. Kňaz predáva 'certifikát očistenia duše', ktorý chráni pred mučiarňou.",
            location = "Cathedral",
            npcName = "Father Ambrose",
            npcTitle = "Parish Priest",
            npcArchetype = "BISHOP",
            choices = listOf(
                EventChoice(
                    id = 1, textEn = "Buy the indulgence with your last savings", textSk = "Kúpiť odpustok za posledné úspory",
                    tagEn = "Absolution", tagSk = "Odpustenie", cardArchetype = "Church_Action",
                    consequence = ChoiceConsequence(
                        goldChange = -30, notorietyChange = -15, factionChanges = mapOf(Faction.CHURCH to 20),
                        removeFlags = setOf("LOGGED_REBEL"),
                        resolutionTextEn = "Ambrose stamps the parchment with holy wax, your sins washed clean by coin.",
                        resolutionTextSk = "Ambrose opečiatkuje pergamen svätým voskom, tvoje hriechy zmyté mincou.",
                        bridgeTextEn = "Your name is struck from whatever list the inquisitors carried.",
                        bridgeTextSk = "Tvoje meno je vyškrtnuté z akéhokoľvek zoznamu, ktorý inkvizítori niesli."
                    )
                ),
                EventChoice(
                    id = 2, textEn = "Publicly mock the priest and expose the scheme", textSk = "Verejne spochybniť kňaza a vysmiať ho",
                    tagEn = "Mockery", tagSk = "Výsmech", cardArchetype = "Peasant_Action",
                    consequence = ChoiceConsequence(
                        regionalTensionChange = 20, factionChanges = mapOf(Faction.CHURCH to -25, Faction.PEASANTS to 20),
                        addFlags = setOf("HERETIC_THREAT"),
                        resolutionTextEn = "Laughter ripples through the crowd as Ambrose's certificates suddenly seem worthless.",
                        resolutionTextSk = "Davom sa šíri smiech, keď sa Ambrosove certifikáty zrazu zdajú bezcenné.",
                        bridgeTextEn = "Ambrose's face reddens with a fury that will not be forgotten.",
                        bridgeTextSk = "Ambrosova tvár sčervenie zúrivosťou, na ktorú sa nezabudne."
                    )
                ),
                EventChoice(
                    id = 3, textEn = "Steal a holy relic from the procession", textSk = "Ukradnúť zo sprievodu svätú relikviu",
                    tagEn = "Theft", tagSk = "Krádež", cardArchetype = "Underworld_Action",
                    consequence = ChoiceConsequence(
                        notorietyChange = 25, influenceChanges = mapOf(UNDERWORLD_AFFINITY to 15),
                        addItems = setOf("Stolen_Relic"),
                        resolutionTextEn = "The reliquary's clasp gives way silently beneath the procession's chanting.",
                        resolutionTextSk = "Spona relikviára potichu povolí pod spevom sprievodu.",
                        bridgeTextEn = "A sliver of gilded bone now rests hidden in your coat.",
                        bridgeTextSk = "Úlomok pozlátenej kosti teraz odpočíva ukrytý v tvojom kabáte."
                    )
                )
            )
        ),

        EventNode(
            id = "p2_mill_revolt",
            phase = EventPhase.PHASE_2,
            originClass = OriginClass.PEASANT,
            condition = { it.regionalTension > 50 },
            titleEn = "The Revolt at the Mill",
            titleSk = "Vzbura pri Mlyne",
            textEn = "The miller has raised the grinding fee to half your harvest. A crowd of villagers stands before the mill with torches.",
            textSk = "Mlynár zvýšil poplatok za mletie múky na polovicu úrody. Dav dedinčanov stojí pred mlynom s fakľami.",
            location = "Village",
            npcName = "Miller Osgood",
            npcTitle = "Village Miller",
            npcArchetype = "MERCHANT",
            choices = listOf(
                EventChoice(
                    id = 1, textEn = "Lead the crowd and burn the mill", textSk = "Postaviť sa na čelo davu a zapáliť mlyn",
                    tagEn = "Rebellion", tagSk = "Povstanie", cardArchetype = "Peasant_Action",
                    consequence = ChoiceConsequence(
                        regionalTensionChange = 30, notorietyChange = 25, factionChanges = mapOf(Faction.PEASANTS to 30),
                        influenceChanges = mapOf(REBEL_TRUST to 20),
                        addFlags = setOf("REBELLION_LEADER"),
                        resolutionTextEn = "Torches meet thatch and the mill's wheel groans one last time before the flames take it.",
                        resolutionTextSk = "Fakle sa stretnú so slamou a mlynské koleso naposledy zastoná, skôr než ho pohltia plamene.",
                        bridgeTextEn = "The village will speak your name for years - the manor will too.",
                        bridgeTextSk = "Dedina bude tvoje meno spomínať roky - panstvo tiež."
                    )
                ),
                EventChoice(
                    id = 2, textEn = "Protect the miller for a promised cut", textSk = "Chrániť mlynára za sľub podielu na zisku",
                    tagEn = "Protection", tagSk = "Ochrana", cardArchetype = "Merchant_Action",
                    consequence = ChoiceConsequence(
                        goldChange = 25, factionChanges = mapOf(Faction.PEASANTS to -30, Faction.GUILDS to 15),
                        addFlags = setOf("GUILD_PROTECTOR"),
                        resolutionTextEn = "Osgood clasps your arm in gratitude as the crowd's torches sputter and disperse.",
                        resolutionTextSk = "Osgood ti vďačne stisne rameno, kým fakle davu dohasínajú a rozchádzajú sa.",
                        bridgeTextEn = "Merchant contacts now speak of you as a man of order and profit.",
                        bridgeTextSk = "Obchodní známi teraz o tebe hovoria ako o mužovi poriadku a zisku."
                    )
                ),
                EventChoice(
                    id = 3, textEn = "Flee to the fields and stay out of it", textSk = "Ujsť do polí a do ničoho sa nestarať",
                    tagEn = "Neutrality", tagSk = "Neutralita", cardArchetype = "Peasant_Action",
                    consequence = ChoiceConsequence(
                        regionalTensionChange = 10,
                        resolutionTextEn = "You watch the glow of torchlight from a safe distance across the furrowed fields.",
                        resolutionTextSk = "Sleduješ žiaru fakieľ z bezpečnej diaľky cez zorané polia.",
                        bridgeTextEn = "Whatever happens at the mill tonight, it happens without you.",
                        bridgeTextSk = "Nech sa dnes v noci pri mlyne stane čokoľvek, deje sa to bez teba."
                    )
                )
            )
        ),

        EventNode(
            id = "p2_lords_guard",
            phase = EventPhase.PHASE_2,
            originClass = OriginClass.PEASANT,
            condition = { ((it.factions[Faction.NOBILITY] ?: 50) > 40 || it.worldFlags.contains("CROWN_INFORMANT")) && it.notoriety < 30 },
            titleEn = "Recruitment into the Manor Guard",
            titleSk = "Nábor do Panskej Gardy",
            textEn = "The captain of the guard is seeking strong men to watch the town gate. He promises pay, armor, and freedom from corvee labor.",
            textSk = "Kapitán stráže hľadá silných chlapov na stráženie mestskej brány. Sľubuje plat, zbroj a oslobodenie od robota.",
            location = "Castle",
            npcName = "Captain Aldous",
            npcTitle = "Manor Guard Captain",
            npcArchetype = "KNIGHT",
            choices = listOf(
                EventChoice(
                    id = 1, textEn = "Enter the Lord's service as a man-at-arms", textSk = "Vstúpiť do služieb lorda ako zbrojnoš",
                    tagEn = "Enlist", tagSk = "Nástup", cardArchetype = "Noble_Action",
                    consequence = ChoiceConsequence(
                        goldChange = 15, factionChanges = mapOf(Faction.NOBILITY to 30, Faction.PEASANTS to -20),
                        influenceChanges = mapOf(NOBLE_FAVOR to 15),
                        addFlags = setOf("MAN_AT_ARMS"),
                        resolutionTextEn = "Aldous claps an iron gauntlet into your hands and marks your name on the garrison roll.",
                        resolutionTextSk = "Aldous ti do rúk vloží železnú rukavicu a poznačí tvoje meno na zoznam posádky.",
                        bridgeTextEn = "You are no longer merely a peasant - the manor now calls you its own.",
                        bridgeTextSk = "Už nie si len sedliak - panstvo ťa teraz volá svojím."
                    )
                ),
                EventChoice(
                    id = 2, textEn = "Refuse - your homeland is your field", textSk = "Odmietnuť s tým, že tvoja vlasť je tvoje pole",
                    tagEn = "Refusal", tagSk = "Odmietnutie", cardArchetype = "Peasant_Action",
                    consequence = ChoiceConsequence(
                        factionChanges = mapOf(Faction.PEASANTS to 10, Faction.NOBILITY to -10),
                        resolutionTextEn = "Aldous shrugs and turns to the next man in line without another word.",
                        resolutionTextSk = "Aldous pokrčí plecami a bez ďalšieho slova sa obráti k ďalšiemu mužovi v rade.",
                        bridgeTextEn = "You remain of the soil, for better or worse.",
                        bridgeTextSk = "Ostávaš pôdou, na dobré aj na zlé."
                    )
                ),
                EventChoice(
                    id = 3, textEn = "Bribe the captain to take your rival instead", textSk = "Podplatiť kapitána, aby namiesto teba vzal tvojho rivala",
                    tagEn = "Scheme", tagSk = "Machinácia", cardArchetype = "Underworld_Action",
                    consequence = ChoiceConsequence(
                        goldChange = -15, influenceChanges = mapOf(UNDERWORLD_AFFINITY to 10),
                        addFlags = setOf("SCHEMER"),
                        resolutionTextEn = "Aldous pockets your silver and calls your rival's name instead of yours.",
                        resolutionTextSk = "Aldous si schová tvoje striebro a namiesto teba zavolá meno tvojho rivala.",
                        bridgeTextEn = "Your rival marches off to the garrison, unaware of your hand in it.",
                        bridgeTextSk = "Tvoj rival pochoduje do posádky, netušiac o tvojej ruke v tom."
                    )
                )
            )
        ),

        // Gap-fill (not in the original document, added to complete the SAVED_OUTCAST unlock hook)
        EventNode(
            id = "p2_outlaw_sanctuary",
            phase = EventPhase.PHASE_2,
            originClass = OriginClass.PEASANT,
            minTurn = 9, maxTurn = 15,
            condition = { it.worldFlags.contains("SAVED_OUTCAST") },
            titleEn = "The Outlaw Sanctuary",
            titleSk = "Zbojnícky Azyl",
            textEn = "The fevered girl you once saved leads you, in gratitude, to a hidden outlaw camp deep in the wood - offering deeper ties to a world beyond the manor's law.",
            textSk = "Dievča, ktoré si kedysi zachránil, ťa z vďaky privedie do skrytého zbojníckeho tábora hlboko v lese - ponúka hlbšie väzby na svet mimo panského zákona.",
            location = "Forest",
            npcName = "Outlaw Elder Marek",
            npcTitle = "Camp Elder",
            npcArchetype = "BANDIT",
            choices = listOf(
                EventChoice(
                    id = 1, textEn = "Join the outlaws for good", textSk = "Pridať sa k zbojníkom natrvalo",
                    tagEn = "Allegiance", tagSk = "Vernosť", cardArchetype = "Underworld_Action",
                    consequence = ChoiceConsequence(
                        goldChange = 10, influenceChanges = mapOf(UNDERWORLD_AFFINITY to 15),
                        addFlags = setOf("OUTLAW_ALLY"),
                        resolutionTextEn = "Marek presses a rough hand to your shoulder and welcomes you to the fire circle.",
                        resolutionTextSk = "Marek ti drsnou rukou stlačí rameno a privíta ťa pri ohnisku.",
                        bridgeTextEn = "The forest's shadows now feel less like a threat and more like home.",
                        bridgeTextSk = "Lesné tiene teraz pôsobia menej ako hrozba a viac ako domov."
                    )
                ),
                EventChoice(
                    id = 2, textEn = "Take provisions and leave with no debts owed", textSk = "Vziať jedlo a odísť bez záväzkov",
                    tagEn = "Provisions", tagSk = "Zásoby", cardArchetype = "Peasant_Action",
                    consequence = ChoiceConsequence(
                        healthChange = 15, factionChanges = mapOf(Faction.PEASANTS to 5),
                        resolutionTextEn = "Marek nods respectfully and lets you leave with a sack of dried meat, no strings attached.",
                        resolutionTextSk = "Marek s úctou prikývne a necháva ťa odísť s vrecom sušeného mäsa, bez akýchkoľvek podmienok.",
                        bridgeTextEn = "You walk back toward the village with a fuller stomach and a clear conscience.",
                        bridgeTextSk = "Kráčaš späť k dedine s plnším žalúdkom a čistým svedomím."
                    )
                ),
                EventChoice(
                    id = 3, textEn = "Betray the hideout's location for a reward", textSk = "Udať úkryt panským drábom za odmenu",
                    tagEn = "Betrayal", tagSk = "Zrada", cardArchetype = "Noble_Action",
                    consequence = ChoiceConsequence(
                        goldChange = 30, factionChanges = mapOf(Faction.NOBILITY to 15),
                        influenceChanges = mapOf(UNDERWORLD_AFFINITY to -20),
                        addFlags = setOf("HIDEOUT_BETRAYER"),
                        resolutionTextEn = "Bailiffs storm the camp before dawn, guided by the map you drew for coin.",
                        resolutionTextSk = "Drábi pred úsvitom vtrhnú do tábora, vedení mapou, ktorú si nakreslil za mincu.",
                        bridgeTextEn = "Marek's fire circle burns for the last time - because of you.",
                        bridgeTextSk = "Marekovo ohnisko horí naposledy - kvôli tebe."
                    )
                )
            )
        ),

        // ============ PHASE 3: KLIMAX A KAUZÁLNE ZÚČTOVANIE (Ťahy 17-25) ============

        EventNode(
            id = "p3_inquisition_raid",
            phase = EventPhase.PHASE_3,
            originClass = OriginClass.PEASANT,
            condition = { it.worldFlags.contains("HERETIC_THREAT") || it.inventoryItemIds.contains("Stolen_Relic") },
            titleEn = "The Grand Inquisition Raid on the Settlement",
            titleSk = "Inkvizičný Záťah na Osadu",
            textEn = "A black carriage has stopped at the green. An inquisitor holds a scroll and reads names of heretics. Your name is first.",
            textSk = "Čierny kočiar zastavil na návesí. Inkvizítor drží v ruke listinu a číta mená kacírov. Tvoje meno je prvé.",
            location = "Cathedral",
            npcName = "Inquisitor Voss",
            npcTitle = "Holy Inquisitor",
            npcArchetype = "BISHOP",
            choices = listOf(
                EventChoice(
                    id = 1, textEn = "Point to your neighbor and accuse her of witchcraft", textSk = "Ukázať na susedu a obviniť ju z bosoráctva",
                    tagEn = "Accusation", tagSk = "Obvinenie", cardArchetype = "Church_Action",
                    consequence = ChoiceConsequence(
                        factionChanges = mapOf(Faction.CHURCH to 20, Faction.PEASANTS to -40),
                        removeFlags = setOf("HERETIC_THREAT"),
                        addFlags = setOf("WITCH_HUNTER_TOOL"),
                        resolutionTextEn = "Voss's quill scratches your neighbor's name as guards seize her by the wrists.",
                        resolutionTextSk = "Vossovo brko zaškrabe meno tvojej susedy, kým ju stráže zovrú za zápästia.",
                        bridgeTextEn = "The carriage rolls away with a new prisoner - not you.",
                        bridgeTextSk = "Kočiar odchádza s novým väzňom - nie s tebou."
                    )
                ),
                EventChoice(
                    id = 2, textEn = "Offer the stolen relic to buy your absolution", textSk = "Použiť Stolen_Relic a vyplatiť sa",
                    tagEn = "Ransom", tagSk = "Výkupné", cardArchetype = "Church_Action",
                    consequence = ChoiceConsequence(
                        resolutionTextEn = "You have nothing sacred to offer, and Voss's eyes narrow with suspicion instead.",
                        resolutionTextSk = "Nemáš nič sväté na ponuku a Vossove oči sa namiesto toho prižmúria podozrievavo.",
                        bridgeTextEn = "Your empty hands have made things worse, not better.",
                        bridgeTextSk = "Tvoje prázdne ruky veci skôr zhoršili, než zlepšili.",
                        notorietyChange = 10
                    ),
                    conditionalOutcome = { world ->
                        if (world.inventoryItemIds.contains("Stolen_Relic")) {
                            ChoiceConsequence(
                                factionChanges = mapOf(Faction.CHURCH to 10),
                                removeItems = setOf("Stolen_Relic"),
                                resolutionTextEn = "Voss's eyes widen at the gilded relic - a fitting price for your silence.",
                                resolutionTextSk = "Vossove oči sa rozšíria pri pohľade na pozlátenú relikviu - vhodná cena za tvoje mlčanie.",
                                bridgeTextEn = "The carriage departs, its ledger closed on your name for now.",
                                bridgeTextSk = "Kočiar odchádza, jeho kniha je pre tvoje meno zatiaľ uzavretá."
                            )
                        } else {
                            ChoiceConsequence(
                                notorietyChange = 10,
                                resolutionTextEn = "You have nothing sacred to offer, and Voss's eyes narrow with suspicion instead.",
                                resolutionTextSk = "Nemáš nič sväté na ponuku a Vossove oči sa namiesto toho prižmúria podozrievavo.",
                                bridgeTextEn = "Your empty hands have made things worse, not better.",
                                bridgeTextSk = "Tvoje prázdne ruky veci skôr zhoršili, než zlepšili."
                            )
                        }
                    }
                ),
                EventChoice(
                    id = 3, textEn = "Fight! Draw a blade on the inquisitor", textSk = "Bojovať! Vytiahnuť dýku na inkvizítora",
                    tagEn = "Combat", tagSk = "Boj", cardArchetype = "Underworld_Action",
                    consequence = ChoiceConsequence(
                        notorietyChange = 40, regionalTensionChange = 30,
                        addFlags = setOf("OUTLAW_FOREVER"),
                        resolutionTextEn = "Steel flashes in the grey morning light as the green erupts into screaming chaos.",
                        resolutionTextSk = "Oceľ zablysne v šedom rannom svetle, kým sa náves rozpadne do kričiaceho chaosu.",
                        bridgeTextEn = "There is no path back to an ordinary life after this.",
                        bridgeTextSk = "Po tomto niet cesty späť k obyčajnému životu."
                    )
                )
            )
        ),

        EventNode(
            id = "p3_punitive_expedition",
            phase = EventPhase.PHASE_3,
            originClass = OriginClass.PEASANT,
            condition = { it.regionalTension > 70 || it.worldFlags.contains("REBELLION_LEADER") },
            titleEn = "The Manor's Punitive Expedition",
            titleSk = "Panská Trestná Výprava",
            textEn = "Smoke rises on the horizon. The Lord's mercenaries are burning cottages and hanging men from trees. A knight on a black courser rides straight for you.",
            textSk = "Na horizonte vidieť dym. Žoldnieri lorda pália chalupy a vešajú mužov na stromy. Rytier na čiernom koni mieri k tebe.",
            location = "Village",
            npcName = "Sir Vayle",
            npcTitle = "Punitive Commander",
            npcArchetype = "KNIGHT",
            choices = listOf(
                EventChoice(
                    id = 1, textEn = "Lead villagers into desperate battle with pitchforks", textSk = "Viesť dedinčanov do zúfalého boja s vidlami",
                    tagEn = "Last Stand", tagSk = "Posledná Bitka", cardArchetype = "Peasant_Action",
                    consequence = ChoiceConsequence(
                        healthChange = -50,
                        resolutionTextEn = "The pitchfork line breaks against steel and horse, and you are dragged away in chains.",
                        resolutionTextSk = "Rad s vidlami sa zlomí proti oceli a koňom a ťa odvlečú preč v reťaziach.",
                        bridgeTextEn = "A warrant now bears your name, whatever remains of your freedom.",
                        bridgeTextSk = "Zatykač teraz nesie tvoje meno, nech z tvojej slobody ostalo čokoľvek.",
                        addFlags = setOf("CAPTURED_REBEL")
                    ),
                    conditionalOutcome = { world ->
                        if (world.worldFlags.contains("BLOOD_ON_HANDS") && world.worldFlags.contains("HUNTER_IN_DARK")) {
                            ChoiceConsequence(
                                regionalTensionChange = -10, notorietyChange = 20,
                                factionChanges = mapOf(Faction.PEASANTS to 20, Faction.NOBILITY to -20),
                                addFlags = setOf("PEASANT_WARLORD"),
                                resolutionTextEn = "Blooded and forest-hardened, you drop Sir Vayle from his saddle before the line even breaks.",
                                resolutionTextSk = "Zakrvavený a lesom zocelený, zhodíš Sira Vayla zo sedla skôr, než sa rad vôbec zlomí.",
                                bridgeTextEn = "The villagers roar your name as the mercenaries scatter in disarray.",
                                bridgeTextSk = "Dedinčania revú tvoje meno, kým sa žoldnieri v neporiadku rozutekajú."
                            )
                        } else {
                            ChoiceConsequence(
                                healthChange = -50,
                                addFlags = setOf("CAPTURED_REBEL"),
                                resolutionTextEn = "The pitchfork line breaks against steel and horse, and you are dragged away in chains.",
                                resolutionTextSk = "Rad s vidlami sa zlomí proti oceli a koňom a ťa odvlečú preč v reťaziach.",
                                bridgeTextEn = "A warrant now bears your name, whatever remains of your freedom.",
                                bridgeTextSk = "Zatykač teraz nesie tvoje meno, nech z tvojej slobody ostalo čokoľvek."
                            )
                        }
                    }
                ),
                EventChoice(
                    id = 2, textEn = "Betray your fellow villagers and open the gate", textSk = "Zradiť spolubojovníkov a otvoriť rytierom bránu osady",
                    tagEn = "Betrayal", tagSk = "Zrada", cardArchetype = "Noble_Action",
                    consequence = ChoiceConsequence(
                        goldChange = 50, factionChanges = mapOf(Faction.NOBILITY to 40, Faction.PEASANTS to -100),
                        addFlags = setOf("JUDAS_OF_OAKVALE"),
                        resolutionTextEn = "Sir Vayle nods coldly as the gate swings open and his riders pour through unopposed.",
                        resolutionTextSk = "Sir Vayle chladne prikývne, kým sa brána otvára a jeho jazdci ňou bez odporu prúdia dnu.",
                        bridgeTextEn = "Gold weighs your purse; the screams behind you weigh far more.",
                        bridgeTextSk = "Zlato ťaží tvoj mešec; výkriky za tebou ťažia oveľa viac."
                    )
                ),
                EventChoice(
                    id = 3, textEn = "Flee to the marshes and let it all burn", textSk = "Utiecť do močarísk a nechať všetko zhorieť",
                    tagEn = "Flight", tagSk = "Útek", cardArchetype = "Peasant_Action",
                    consequence = ChoiceConsequence(
                        goldChange = -9999,
                        addFlags = setOf("DESTITUTE_SURVIVOR"),
                        resolutionTextEn = "You watch from the reeds as everything you owned turns to ash and smoke.",
                        resolutionTextSk = "Z trstiny sleduješ, ako sa všetko, čo si vlastnil, mení na popol a dym.",
                        bridgeTextEn = "You survive with nothing but the clothes on your back.",
                        bridgeTextSk = "Prežiješ len s odevom na chrbte a ničím iným."
                    )
                )
            )
        ),

        EventNode(
            id = "p3_grand_trial",
            phase = EventPhase.PHASE_3,
            originClass = OriginClass.PEASANT,
            minTurn = 17, maxTurn = 24,
            forcedPriority = true,
            condition = { it.notoriety > 80 },
            titleEn = "The Grand Trial at the Castle",
            titleSk = "Veľký Súd na Hrade",
            textEn = "Manor bailiffs have shackled you in irons and dragged you before Lord Reginald. A list of your crimes lies on the table.",
            textSk = "Panskí drábi ťa spútali v reťaziach a dotiahli pred lorda Reginalda. Na stole leží zoznam tvojich zločinov.",
            location = "Castle",
            npcName = "Lord Reginald",
            npcTitle = "Manor Justiciar",
            npcArchetype = "NOBLE",
            choices = listOf(
                EventChoice(
                    id = 1, textEn = "Use the forged pass and claim to be a free courier", textSk = "Použiť Forged_Pass a tvrdiť, že si slobodný posol",
                    tagEn = "Deception", tagSk = "Klamstvo", cardArchetype = "Underworld_Action",
                    consequence = ChoiceConsequence(
                        notorietyChange = 10, healthChange = -10,
                        resolutionTextEn = "You have no papers to show, and Reginald's patience for excuses runs thin.",
                        resolutionTextSk = "Nemáš žiadne listiny na ukázanie a Reginaldova trpezlivosť s výhovorkami sa tenčí.",
                        bridgeTextEn = "The lack of proof only deepens his suspicion of you.",
                        bridgeTextSk = "Nedostatok dôkazov len prehĺbi jeho podozrenie voči tebe."
                    ),
                    conditionalOutcome = { world ->
                        if (world.inventoryItemIds.contains("Forged_Pass")) {
                            ChoiceConsequence(
                                goldChange = -20, notorietyChange = -40,
                                removeItems = setOf("Forged_Pass"),
                                addFlags = setOf("ESCAPED_GALLOWS"),
                                resolutionTextEn = "Reginald hesitates over the forged seal, then waves you away with a bribed executioner's blessing.",
                                resolutionTextSk = "Reginald zaváha nad falošnou pečaťou, potom ťa odbaví s požehnaním podplateného kata.",
                                bridgeTextEn = "You walk from the castle gates a free man - for now.",
                                bridgeTextSk = "Odchádzaš z hradných brán ako slobodný muž - zatiaľ."
                            )
                        } else {
                            ChoiceConsequence(
                                notorietyChange = 10, healthChange = -10,
                                resolutionTextEn = "You have no papers to show, and Reginald's patience for excuses runs thin.",
                                resolutionTextSk = "Nemáš žiadne listiny na ukázanie a Reginaldova trpezlivosť s výhovorkami sa tenčí.",
                                bridgeTextEn = "The lack of proof only deepens his suspicion of you.",
                                bridgeTextSk = "Nedostatok dôkazov len prehĺbi jeho podozrenie voči tebe."
                            )
                        }
                    }
                ),
                EventChoice(
                    id = 2, textEn = "Invoke your standing with the Church", textSk = "Prirodzená obhajoba cez vysoké vzťahy s Cirkvou",
                    tagEn = "Church Defense", tagSk = "Cirkevná Obhajoba", cardArchetype = "Church_Action",
                    consequence = ChoiceConsequence(
                        resolutionTextEn = "Reginald scoffs - the Church has no love for you, and no words come to your defense.",
                        resolutionTextSk = "Reginald si odfrkne - Cirkev ťa nemiluje a žiadne slová sa nepostavia na tvoju obhajobu.",
                        bridgeTextEn = "You stand alone before the justiciar's table.",
                        bridgeTextSk = "Stojíš sám pred stolom spravodlivosti."
                    ),
                    conditionalOutcome = { world ->
                        if ((world.factions[Faction.CHURCH] ?: 50) > 60) {
                            ChoiceConsequence(
                                factionChanges = mapOf(Faction.NOBILITY to -5),
                                addFlags = setOf("CHURCH_PROPERTY"),
                                resolutionTextEn = "A parish priest speaks on your behalf, and Reginald grudgingly remands you to Church labor instead of the gallows.",
                                resolutionTextSk = "Farský kňaz sa za teba prihovorí a Reginald ťa neochotne vydá na cirkevné práce namiesto šibenice.",
                                bridgeTextEn = "You trade the noose for a life bound to the cathedral's service.",
                                bridgeTextSk = "Vymeníš slučku za život zviazaný so službou katedrále."
                            )
                        } else {
                            ChoiceConsequence(
                                resolutionTextEn = "Reginald scoffs - the Church has no love for you, and no words come to your defense.",
                                resolutionTextSk = "Reginald si odfrkne - Cirkev ťa nemiluje a žiadne slová sa nepostavia na tvoju obhajobu.",
                                bridgeTextEn = "You stand alone before the justiciar's table.",
                                bridgeTextSk = "Stojíš sám pred stolom spravodlivosti."
                            )
                        }
                    }
                ),
                EventChoice(
                    id = 3, textEn = "Spit in the Lord's face and accept judgment", textSk = "Pľuť lordovi do tváre a prijať rozsudok",
                    tagEn = "Defiance", tagSk = "Vzdor", cardArchetype = "Peasant_Action",
                    consequence = ChoiceConsequence(
                        healthChange = -40, goldChange = -9999,
                        addFlags = setOf("INCARCERATED_CRIMINAL"),
                        resolutionTextEn = "Guards beat you into the castle's darkest cell before Reginald's spittle-flecked roar even fades.",
                        resolutionTextSk = "Stráže ťa dokopú do najtmavšej hradnej kobky skôr, než Reginaldov slinami pokrytý rev vôbec stíchne.",
                        bridgeTextEn = "Iron bars and darkness are your world now.",
                        bridgeTextSk = "Železné mreže a tma sú teraz tvojím svetom."
                    )
                )
            )
        ),

        EventNode(
            id = "p3_smuggler",
            phase = EventPhase.PHASE_3,
            originClass = OriginClass.PEASANT,
            condition = { influence(it, UNDERWORLD_AFFINITY) > 30 || it.worldFlags.contains("SCHEMER") },
            titleEn = "The Smuggler's Cart at the Border",
            titleSk = "Pašerácky Voštinár na Hranici",
            textEn = "A merchant's cart with a hidden false bottom stands by the road. He offers you work: smuggle untaxed silver past the manor tollgate into the city.",
            textSk = "Pri ceste stojí voštinársky voztok so skrytým dvojitým dnom. Obchodník ti ponúka prácu: prepašovať nezdanené striebro cez panskú mýtnicu do mesta.",
            location = "Marketplace",
            npcName = "Trader Yusuf",
            npcTitle = "Black Market Cartwright",
            npcArchetype = "MERCHANT",
            choices = listOf(
                EventChoice(
                    id = 1, textEn = "Smuggle the goods hidden in straw bundles", textSk = "Prepašovať tovar v snopoch slamy",
                    tagEn = "Smuggling", tagSk = "Pašovanie", cardArchetype = "Merchant_Action",
                    consequence = ChoiceConsequence(
                        goldChange = 35, factionChanges = mapOf(Faction.GUILDS to 15),
                        addFlags = setOf("SILVER_SMUGGLER"),
                        resolutionTextEn = "The tollgate guard barely glances at your straw-laden cart as you pass through unquestioned.",
                        resolutionTextSk = "Strážca mýtnice sotva mrkne na tvoj slamou naložený voz, kým prechádzaš bez otázok.",
                        bridgeTextEn = "Yusuf's guild contacts now speak your name with quiet approval.",
                        bridgeTextSk = "Yusufovi cechoví známi teraz spomínajú tvoje meno s tichým súhlasom."
                    )
                ),
                EventChoice(
                    id = 2, textEn = "Report the cart to the tollkeeper for reward", textSk = "Udať voztok mýtnikovi a zobrať odmenu",
                    tagEn = "Betrayal", tagSk = "Zrada", cardArchetype = "Noble_Action",
                    consequence = ChoiceConsequence(
                        goldChange = 15, factionChanges = mapOf(Faction.NOBILITY to 20),
                        influenceChanges = mapOf(UNDERWORLD_AFFINITY to -30),
                        addFlags = setOf("GUILD_BETRAYER"),
                        resolutionTextEn = "The tollkeeper's men seize Yusuf's cart as you collect your informant's fee.",
                        resolutionTextSk = "Mýtnikovi muži zaberú Yusufov voz, kým si vyzdvihneš odmenu za udanie.",
                        bridgeTextEn = "Word of an informant among the smugglers begins to spread.",
                        bridgeTextSk = "Medzi pašerákmi sa začína šíriť chýr o donášačovi."
                    )
                ),
                EventChoice(
                    id = 3, textEn = "Buy the goods and try to sell them yourself", textSk = "Odkúpiť tovar a skúsiť ho predať sám na vlastné triko",
                    tagEn = "Trade", tagSk = "Obchod", cardArchetype = "Merchant_Action",
                    consequence = ChoiceConsequence(
                        resolutionTextEn = "Yusuf counts your coin and shakes his head - it isn't enough to cover his risk.",
                        resolutionTextSk = "Yusuf si prepočíta tvoju mincu a pokrúti hlavou - nestačí na pokrytie jeho rizika.",
                        bridgeTextEn = "You walk away from the cart empty-handed.",
                        bridgeTextSk = "Od voza odchádzaš s prázdnymi rukami.",
                        notorietyChange = 5
                    ),
                    conditionalOutcome = { world ->
                        if (world.gold >= 25) {
                            ChoiceConsequence(
                                goldChange = -25, factionChanges = mapOf(Faction.GUILDS to 10),
                                addItems = setOf("Contraband_Pouch"),
                                resolutionTextEn = "Yusuf hands over the contraband pouch with a satisfied nod once your coin is counted.",
                                resolutionTextSk = "Yusuf ti odovzdá vrecko s pašovaným tovarom so spokojným prikývnutím, len čo je tvoja minca spočítaná.",
                                bridgeTextEn = "The pouch's weight in your coat feels like opportunity.",
                                bridgeTextSk = "Váha vrecka v tvojom kabáte pôsobí ako príležitosť."
                            )
                        } else {
                            ChoiceConsequence(
                                notorietyChange = 5,
                                resolutionTextEn = "Yusuf counts your coin and shakes his head - it isn't enough to cover his risk.",
                                resolutionTextSk = "Yusuf si prepočíta tvoju mincu a pokrúti hlavou - nestačí na pokrytie jeho rizika.",
                                bridgeTextEn = "You walk away from the cart empty-handed.",
                                bridgeTextSk = "Od voza odchádzaš s prázdnymi rukami."
                            )
                        }
                    }
                )
            )
        ),

        EventNode(
            id = "p3_famine_march",
            phase = EventPhase.PHASE_3,
            originClass = OriginClass.PEASANT,
            minTurn = 18, maxTurn = 22,
            forcedPriority = true,
            titleEn = "The Last Harvest and the Hunger March",
            titleSk = "Posledná Úroda a Hladový Pochod",
            textEn = "Frost has burned away the last scraps of grain. Hundreds of desperate people stand before the manor granary gates. The manor guard draws swords.",
            textSk = "Mráz spálil posledné zvyšky obilia. Pred bránami panskej sýpky stoja stovky zúfalých ľudí. Panská stráž tasí meče.",
            location = "Village",
            npcName = "Captain Aldous",
            npcTitle = "Manor Guard Captain",
            npcArchetype = "KNIGHT",
            choices = listOf(
                EventChoice(
                    id = 1, textEn = "Give your own hidden stores to the starving", textSk = "Rozdať vlastné skryté zásoby hladujúcim",
                    tagEn = "Charity", tagSk = "Milosrdenstvo", cardArchetype = "Peasant_Action",
                    consequence = ChoiceConsequence(
                        healthChange = -5, factionChanges = mapOf(Faction.PEASANTS to 10),
                        resolutionTextEn = "You have little enough of your own, and what you offer barely eases the crowd's hunger.",
                        resolutionTextSk = "Sám máš málo a to, čo ponúkneš, sotva utíši hlad davu.",
                        bridgeTextEn = "Your gesture is noted, though it changes little.",
                        bridgeTextSk = "Tvoje gesto je zaznamenané, hoci mení len málo."
                    ),
                    conditionalOutcome = { world ->
                        if (world.worldFlags.contains("SAVED_OUTCAST") || !world.worldFlags.contains("STARVING")) {
                            ChoiceConsequence(
                                healthChange = -10, factionChanges = mapOf(Faction.PEASANTS to 40),
                                addFlags = setOf("SAVIOR_OF_THE_POOR"),
                                resolutionTextEn = "You throw open your own stores, and the crowd's desperate roar turns to grateful weeping.",
                                resolutionTextSk = "Otvoríš svoje vlastné zásoby a zúfalý rev davu sa mení na vďačný plač.",
                                bridgeTextEn = "Your name will be spoken in this village long after you are gone.",
                                bridgeTextSk = "Tvoje meno sa bude v tejto dedine spomínať dlho po tom, čo odídeš."
                            )
                        } else {
                            ChoiceConsequence(
                                factionChanges = mapOf(Faction.PEASANTS to -5),
                                resolutionTextEn = "You have nothing left to give - your own hunger stares back at you from the crowd.",
                                resolutionTextSk = "Nemáš čo dať - tvoj vlastný hlad na teba hľadí z davu.",
                                bridgeTextEn = "Empty hands help no one today.",
                                bridgeTextSk = "Prázdne ruky dnes nikomu nepomôžu."
                            )
                        }
                    }
                ),
                EventChoice(
                    id = 2, textEn = "Infiltrate the granary in the chaos and steal manor flour", textSk = "Infiltrovať sýpku v chaose a ukradnúť panskú múku",
                    tagEn = "Theft", tagSk = "Krádež", cardArchetype = "Underworld_Action",
                    consequence = ChoiceConsequence(
                        healthChange = 30, goldChange = 20, notorietyChange = 15,
                        addFlags = setOf("GRAIN_THIEF"),
                        resolutionTextEn = "In the roaring press of bodies, you slip through a side door and fill your sack with stolen flour.",
                        resolutionTextSk = "V hučiacom tlaku tiel prekĺzneš bočnými dverami a naplníš si vrece ukradnutou múkou.",
                        bridgeTextEn = "You eat well tonight, at the manor's unwitting expense.",
                        bridgeTextSk = "Dnes večer sa dobre naješ, na nevedomý účet panstva."
                    )
                ),
                EventChoice(
                    id = 3, textEn = "Join the manor's soldiers whipping back the crowd", textSk = "Pridať sa k panským žoldnierom bičom rozháňať dav",
                    tagEn = "Enforcement", tagSk = "Presadzovanie", cardArchetype = "Noble_Action",
                    consequence = ChoiceConsequence(
                        regionalTensionChange = 15,
                        resolutionTextEn = "Aldous waves you off - you have no standing among his ranks to lift a whip here.",
                        resolutionTextSk = "Aldous ťa odbaví - medzi jeho radmi nemáš postavenie na to, aby si tu zdvihol bič.",
                        bridgeTextEn = "You watch the crowd scatter from the sidelines instead.",
                        bridgeTextSk = "Namiesto toho sleduješ rozháňanie davu z bočnej línie."
                    ),
                    conditionalOutcome = { world ->
                        if (world.worldFlags.contains("MAN_AT_ARMS") || world.worldFlags.contains("SUBMISSIVE")) {
                            ChoiceConsequence(
                                goldChange = 10, factionChanges = mapOf(Faction.NOBILITY to 15, Faction.PEASANTS to -50),
                                addFlags = setOf("ENFORCER_OF_TYRANNY"),
                                resolutionTextEn = "Your whip falls alongside the garrison's, and the starving crowd finally breaks and scatters.",
                                resolutionTextSk = "Tvoj bič dopadá po boku posádky a hladujúci dav sa konečne zlomí a rozuteká.",
                                bridgeTextEn = "The granary gates hold, but the village will never look at you the same way again.",
                                bridgeTextSk = "Brány sýpky vydržia, no dedina sa na teba už nikdy nepozrie rovnako."
                            )
                        } else {
                            ChoiceConsequence(
                                regionalTensionChange = 15,
                                resolutionTextEn = "Aldous waves you off - you have no standing among his ranks to lift a whip here.",
                                resolutionTextSk = "Aldous ťa odbaví - medzi jeho radmi nemáš postavenie na to, aby si tu zdvihol bič.",
                                bridgeTextEn = "You watch the crowd scatter from the sidelines instead.",
                                bridgeTextSk = "Namiesto toho sleduješ rozháňanie davu z bočnej línie."
                            )
                        }
                    }
                )
            )
        )
    )

    // ---------------------------------------------------------------------
    // ACOLYTE CHAPTER 1 DECK
    // ---------------------------------------------------------------------
    private val ACOLYTE_NODES: List<EventNode> = listOf(

        // ============ PHASE 1: ZAKÁZANÉ TEXTY A KLÁŠTORNÁ ROLA (Ťahy 1-7) ============

        EventNode(
            id = "a1_forbidden_manuscript",
            phase = EventPhase.PHASE_1,
            originClass = OriginClass.ACOLYTE,
            minTurn = 1, maxTurn = 1,
            forcedPriority = true,
            titleEn = "The Forbidden Manuscript Beneath the Floor",
            titleSk = "Zakázaný Rukopis pod Dlážkou",
            textEn = "Sweeping the vaulted library, you find a loose tile. Beneath it lies a dust-caked parchment volume bound in human skin, marked with heretical symbols. Footsteps of Bishop Alistair echo in the corridor.",
            textSk = "Pri zametaní klenutej knižnice nájdeš uvoľnenú dlaždicu. Pod ňou leží prachom zapadnutý pergamencový zväzok viazaný v ľudskej koži s kacírskymi symbolmi. V chodbe počuť kroky Biskupa Alistaira.",
            location = "Cathedral",
            npcName = "Bishop Alistair",
            npcTitle = "High Prelate",
            npcArchetype = "BISHOP",
            choices = listOf(
                EventChoice(
                    id = 1, textEn = "Hand the manuscript to the Bishop at once", textSk = "Okamžite odovzdať rukopis Biskupovi",
                    tagEn = "Loyalty", tagSk = "Vernosť", cardArchetype = "Church_Action",
                    consequence = ChoiceConsequence(
                        factionChanges = mapOf(Faction.CHURCH to 15, Faction.PEASANTS to -10),
                        addFlags = setOf("LOYAL_INFORMER"),
                        resolutionTextEn = "Alistair's eyes widen at the sight, and he presses the archive keys into your hands in gratitude.",
                        resolutionTextSk = "Alistairove oči sa rozšíria pri pohľade naň a on ti vďačne vtlačí do rúk kľúče od archívu.",
                        bridgeTextEn = "You are trusted now in ways few novices ever are.",
                        bridgeTextSk = "Teraz ti dôverujú spôsobom, akým málokedy dôverujú novicom."
                    )
                ),
                EventChoice(
                    id = 2, textEn = "Hide it under your habit and study it by night", textSk = "Ukryť rukopis pod habit a preštudovať ho v noci",
                    tagEn = "Forbidden Study", tagSk = "Zakázané Štúdium", cardArchetype = "Underworld_Action",
                    consequence = ChoiceConsequence(
                        notorietyChange = 10, influenceChanges = mapOf(UNDERWORLD_AFFINITY to 15),
                        addItems = setOf("Forbidden_Manuscript"),
                        addFlags = setOf("HERETIC_KNOWLEDGE"),
                        resolutionTextEn = "By candlelight, the forbidden script's meaning unfolds - terrifying and intoxicating.",
                        resolutionTextSk = "Pri sviečke sa ti odhaľuje význam zakázaného textu - desivý a opojný zároveň.",
                        bridgeTextEn = "You slide the volume beneath a loose floorboard in your own cell.",
                        bridgeTextSk = "Zväzok schováš pod uvoľnenú dosku vo vlastnej cele."
                    )
                ),
                EventChoice(
                    id = 3, textEn = "Plant the book in rival Brother Bernard's cell", textSk = "Podstrčiť knihu do cely rivala, brata Bernarda",
                    tagEn = "Scheme", tagSk = "Intriga", cardArchetype = "Underworld_Action",
                    consequence = ChoiceConsequence(
                        goldChange = 10, regionalTensionChange = 15,
                        addFlags = setOf("MONASTERY_SCHEMER"),
                        resolutionTextEn = "Guards drag a bewildered Bernard away in chains before he can utter a single protest.",
                        resolutionTextSk = "Stráže odvlečú zmäteného Bernarda v reťaziach skôr, než stihne čo i len zaprotestovať.",
                        bridgeTextEn = "Your rival's cell now stands empty, and no one suspects your hand in it.",
                        bridgeTextSk = "Bernardova cela teraz stojí prázdna a nikto netuší o tvojej ruke v tom."
                    )
                )
            )
        ),

        EventNode(
            id = "a1_plague_crypt",
            phase = EventPhase.PHASE_1,
            originClass = OriginClass.ACOLYTE,
            minTurn = 2, maxTurn = 5,
            titleEn = "The Plague Mortuary in the Crypt",
            titleSk = "Morová Márnica v Krypte",
            textEn = "The crypt is full of babbling sick. A wealthy merchant begs for last rites before death, offering a heavy gold ring if you bury him in hallowed ground despite the Bishop's ban.",
            textSk = "Krypta je plná blabotajúcich nemocných. Bohatý kupec pred smrťou žiada absolúciu a núka ti ťažký zlatý prsteň, ak ho pochováš do svätenej pôdy aj napriek Biskupovmu zákazu.",
            location = "Cathedral",
            npcName = "Merchant Aldous",
            npcTitle = "Dying Merchant",
            npcArchetype = "MERCHANT",
            choices = listOf(
                EventChoice(
                    id = 1, textEn = "Take the ring and secretly bury him in the sacred grove", textSk = "Zobrať prsteň a potajomky vykopať hrob v posvätenom háji",
                    tagEn = "Bribe", tagSk = "Úplatok", cardArchetype = "Underworld_Action",
                    consequence = ChoiceConsequence(
                        goldChange = 40, factionChanges = mapOf(Faction.CHURCH to -10),
                        addItems = setOf("Merchant_Signet"),
                        addFlags = setOf("GRAVE_ROBBER"),
                        resolutionTextEn = "Aldous presses the ring into your palm with the last of his strength, at peace.",
                        resolutionTextSk = "Aldous ti s poslednými silami vtlačí prsteň do dlane, konečne pokojný.",
                        bridgeTextEn = "You bury him by lantern light in soil that was never meant for merchants.",
                        bridgeTextSk = "Pochováš ho pri svetle lampáša do pôdy, ktorá nikdy nebola určená pre kupcov."
                    )
                ),
                EventChoice(
                    id = 2, textEn = "Refuse the ring and give true holy service freely", textSk = "Odmietnuť prsteň a vykonať skutočnú svätú službu bezodplatne",
                    tagEn = "Devotion", tagSk = "Oddanosť", cardArchetype = "Church_Action",
                    consequence = ChoiceConsequence(
                        factionChanges = mapOf(Faction.CHURCH to 20, Faction.PEASANTS to 20),
                        addFlags = setOf("SAINTLY_DEVOTION"),
                        resolutionTextEn = "Aldous weeps in gratitude as you anoint him without a single coin changing hands.",
                        resolutionTextSk = "Aldous vďačne plače, kým ho pomažeš bez toho, aby sa vymenila čo i len jedna minca.",
                        bridgeTextEn = "Word of your selfless devotion spreads through the sick-ward.",
                        bridgeTextSk = "Chýr o tvojej nezištnej oddanosti sa šíri chorobincom."
                    )
                ),
                EventChoice(
                    id = 3, textEn = "Rip the ring from his finger and dump the body in the mass grave", textSk = "Prsteň mu strhnúť z prsta a telo hodiť do masového hrobu za hradbami",
                    tagEn = "Cruelty", tagSk = "Krutosť", cardArchetype = "Underworld_Action",
                    consequence = ChoiceConsequence(
                        goldChange = 40, notorietyChange = 15, factionChanges = mapOf(Faction.CHURCH to -20),
                        addFlags = setOf("RUTHLESS_CLERIC"),
                        resolutionTextEn = "The ring comes free with a twist, and Aldous's body joins the nameless dead beyond the walls.",
                        resolutionTextSk = "Prsteň sa uvoľní jedným trhnutím a Aldousovo telo sa pridá k bezmenným mŕtvym za hradbami.",
                        bridgeTextEn = "You wash the grave dirt from your hands and say nothing to anyone.",
                        bridgeTextSk = "Umyješ si z rúk hrobovú hlinu a nikomu nič nepovieš."
                    )
                )
            )
        ),

        EventNode(
            id = "a1_indulgence_sale",
            phase = EventPhase.PHASE_1,
            originClass = OriginClass.ACOLYTE,
            titleEn = "The Sale of Indulgences on the Green",
            titleSk = "Predaj Odpustkov na Návesí",
            textEn = "The Bishop entrusted you with the strongbox and indulgence certificates. A desperate peasant has no coin, but offers his only daughter into monastery service in exchange for his father's salvation.",
            textSk = "Biskup ti zveril pokladničku a certifikáty odpustkov. Zúfalý poddaný nemá ani medenák, ale núka svoju jedinú dcéru do služby v kláštore výmenou za spasenie duše svojho otca.",
            location = "Village",
            npcName = "Peasant Aldric",
            npcTitle = "Desperate Father",
            npcArchetype = "PEASANT",
            choices = listOf(
                EventChoice(
                    id = 1, textEn = "Reconcile, grant the indulgence, take the girl as a cook", textSk = "Uzmieriť sa, dať odpustok a dievča vziať ako kuchárku",
                    tagEn = "Mercy", tagSk = "Milosrdenstvo", cardArchetype = "Church_Action",
                    consequence = ChoiceConsequence(
                        factionChanges = mapOf(Faction.PEASANTS to 15, Faction.CHURCH to -10),
                        addFlags = setOf("MERCIFUL_BROTHER"),
                        resolutionTextEn = "Aldric weeps with relief as his daughter is led gently into the kitchens instead of turned away.",
                        resolutionTextSk = "Aldric plače úľavou, kým jeho dcéru namiesto odmietnutia jemne odvedú do kuchýň.",
                        bridgeTextEn = "The certificate is granted freely, against the letter of church law.",
                        bridgeTextSk = "Certifikát je udelený zadarmo, proti litere cirkevného práva."
                    )
                ),
                EventChoice(
                    id = 2, textEn = "Drive them off with the whip, uncompromising and unpaid", textSk = "Nekompromisne ich vyhnať bičom bez peňazí",
                    tagEn = "Dogma", tagSk = "Dogma", cardArchetype = "Church_Action",
                    consequence = ChoiceConsequence(
                        goldChange = 20, factionChanges = mapOf(Faction.CHURCH to 15, Faction.PEASANTS to -20),
                        addFlags = setOf("DOGMATIC_FANATIC"),
                        resolutionTextEn = "The whip cracks and Aldric stumbles back, his plea unanswered by holy law.",
                        resolutionTextSk = "Bič zapraská a Aldric cúvne, jeho prosba zostáva nevypočutá svätým zákonom.",
                        bridgeTextEn = "Other buyers, watching, pay in full without a word of protest.",
                        bridgeTextSk = "Ostatní kupujúci, ktorí to sledujú, platia v plnej výške bez slova protestu."
                    )
                ),
                EventChoice(
                    id = 3, textEn = "Embezzle part of the collected coin for yourself", textSk = "Spreneveriť časť vybraných peňazí z pokladničky pre seba",
                    tagEn = "Theft", tagSk = "Krádež", cardArchetype = "Underworld_Action",
                    consequence = ChoiceConsequence(
                        goldChange = 30, notorietyChange = 10,
                        addFlags = setOf("CHURCH_THIEF"),
                        resolutionTextEn = "Your fingers slip a fold of coin into your sleeve before the strongbox is sealed again.",
                        resolutionTextSk = "Tvoje prsty schovajú záhyb mincí do rukáva skôr, než sa pokladnička znovu zapečatí.",
                        bridgeTextEn = "The ledger will not balance later, but that is a problem for another day.",
                        bridgeTextSk = "Účtovná kniha sa neskôr nebude zhodovať, no to je problém na iný deň."
                    )
                )
            )
        ),

        EventNode(
            id = "a1_executioner_confession",
            phase = EventPhase.PHASE_1,
            originClass = OriginClass.ACOLYTE,
            condition = { it.worldFlags.contains("SAINTLY_DEVOTION") || it.worldFlags.contains("LOYAL_INFORMER") },
            titleEn = "The Executioner's Midnight Confession",
            titleSk = "Nočná Spoveď Panského Kata",
            textEn = "The blood-soaked executioner comes to the confessional at midnight. In tears, he confesses the manor steward forced him to torture an innocent brother for a hidden treasure.",
            textSk = "Krvavý kat prichádza o polnoci do spovednice. V slzách sa spovedá, že panský správca ho prinútil mučiť nevinného brata pre zlatý poklad.",
            location = "Cathedral",
            npcName = "Executioner Grim",
            npcTitle = "Manor Executioner",
            npcArchetype = "NOBLE",
            choices = listOf(
                EventChoice(
                    id = 1, textEn = "Assure him of forgiveness and learn the treasure's hiding place", textSk = "Ubezpečiť ho o odpustení a získať od neho úkryt pokladu",
                    tagEn = "Exploit", tagSk = "Zneužitie", cardArchetype = "Underworld_Action",
                    consequence = ChoiceConsequence(
                        goldChange = 35, influenceChanges = mapOf(UNDERWORLD_AFFINITY to 10),
                        addItems = setOf("Torturer_Key"),
                        addFlags = setOf("CONFESSION_EXPLOITER"),
                        resolutionTextEn = "Grim's whispered gratitude comes with a rusted key and a hidden vault's location.",
                        resolutionTextSk = "Grimova šeptaná vďaka príde spolu so zhrdzaveným kľúčom a polohou skrytej klenby.",
                        bridgeTextEn = "The seal of confession has never felt so profitable.",
                        bridgeTextSk = "Spovedné tajomstvo sa nikdy nezdalo tak výnosné."
                    )
                ),
                EventChoice(
                    id = 2, textEn = "Order penance and hand him to church court", textSk = "Nariadiť mu pokánie a vydať ho cirkevnému súdu",
                    tagEn = "Justice", tagSk = "Spravodlivosť", cardArchetype = "Church_Action",
                    consequence = ChoiceConsequence(
                        factionChanges = mapOf(Faction.CHURCH to 25, Faction.NOBILITY to 10),
                        addFlags = setOf("HOLY_JUSTICIAR"),
                        resolutionTextEn = "Grim kneels willingly before the church court, accepting whatever penance awaits.",
                        resolutionTextSk = "Grim ochotne kľačí pred cirkevným súdom a prijíma akékoľvek pokánie ho čaká.",
                        bridgeTextEn = "The church's justice is seen to be done, and your name is noted for it.",
                        bridgeTextSk = "Cirkevná spravodlivosť je vykonaná pred zrakmi všetkých a tvoje meno je za to zaznamenané."
                    )
                ),
                EventChoice(
                    id = 3, textEn = "Use his secret to blackmail the manor steward", textSk = "Použiť jeho tajomstvo na vydieranie panského správcu",
                    tagEn = "Blackmail", tagSk = "Vydieranie", cardArchetype = "Underworld_Action",
                    consequence = ChoiceConsequence(
                        goldChange = 50, regionalTensionChange = 20, factionChanges = mapOf(Faction.NOBILITY to -15),
                        addFlags = setOf("BLACKMAILER"),
                        resolutionTextEn = "The steward's face pales as you lay out exactly what you know, and exactly what you want.",
                        resolutionTextSk = "Správcova tvár zbledne, keď mu vyložíš presne to, čo vieš, a presne to, čo chceš.",
                        bridgeTextEn = "Gold arrives quietly at the monastery gate the very next morning.",
                        bridgeTextSk = "Zlato dorazí potichu k bránam kláštora už nasledujúce ráno."
                    )
                )
            )
        ),

        // ============ PHASE 2: INKVIZÍCIA A MOCENSKÉ BOJE (Ťahy 8-16) ============

        EventNode(
            id = "a2_grand_inquisitor",
            phase = EventPhase.PHASE_2,
            originClass = OriginClass.ACOLYTE,
            condition = { it.notoriety > 30 || it.worldFlags.contains("HERETIC_KNOWLEDGE") },
            titleEn = "The Arrival of the Grand Inquisitor",
            titleSk = "Príchod Veľkého Inkvizítora",
            textEn = "A black carriage bearing Inquisitor Malachai arrives in the courtyard. He hunts for traces of black magic and free thought, questioning every brother in turn.",
            textSk = "Čierny kočiar s Inkvizítorom Malachaiom dorazil na nádvorie. Hľadá stopy čiernej mágie a voľnomyšlienkárstva. Vypočúva všetkých bratov v rade.",
            location = "Cathedral",
            npcName = "Inquisitor Malachai",
            npcTitle = "Grand Inquisitor",
            npcArchetype = "BISHOP",
            choices = listOf(
                EventChoice(
                    id = 1, textEn = "Produce the manuscript and claim it belongs to the Bishop", textSk = "Vytiahnuť rukopis a vyhlásiť, že patrí Biskupovi",
                    tagEn = "Betrayal", tagSk = "Zrada", cardArchetype = "Underworld_Action",
                    consequence = ChoiceConsequence(
                        notorietyChange = 5,
                        resolutionTextEn = "You have no manuscript to show, and Malachai's questioning turns sharply toward you instead.",
                        resolutionTextSk = "Nemáš žiadny rukopis na ukázanie a Malachaiovo vypočúvanie sa ostro obráti na teba.",
                        bridgeTextEn = "An empty accusation only draws suspicion onto your own head.",
                        bridgeTextSk = "Prázdne obvinenie priťahuje podozrenie len na tvoju vlastnú hlavu."
                    ),
                    conditionalOutcome = { world ->
                        if (world.inventoryItemIds.contains("Forbidden_Manuscript")) {
                            ChoiceConsequence(
                                factionChanges = mapOf(Faction.CHURCH to 30, Faction.NOBILITY to -50),
                                removeItems = setOf("Forbidden_Manuscript"),
                                addFlags = setOf("BISHOP_BETRAYER"),
                                resolutionTextEn = "Malachai's eyes gleam as he examines the heretical script - and orders the Bishop seized on the spot.",
                                resolutionTextSk = "Malachaiovi zažiaria oči, keď preskúma kacírsky text - a nariadi Biskupa okamžite zatknúť.",
                                bridgeTextEn = "The archive keys you once received now feel like a noose around your own future.",
                                bridgeTextSk = "Kľúče od archívu, ktoré si kedysi dostal, teraz pôsobia ako slučka okolo tvojej vlastnej budúcnosti."
                            )
                        } else {
                            ChoiceConsequence(
                                notorietyChange = 5,
                                resolutionTextEn = "You have no manuscript to show, and Malachai's questioning turns sharply toward you instead.",
                                resolutionTextSk = "Nemáš žiadny rukopis na ukázanie a Malachaiovo vypočúvanie sa ostro obráti na teba.",
                                bridgeTextEn = "An empty accusation only draws suspicion onto your own head.",
                                bridgeTextSk = "Prázdne obvinenie priťahuje podozrenie len na tvoju vlastnú hlavu."
                            )
                        }
                    }
                ),
                EventChoice(
                    id = 2, textEn = "Burn the book in the furnace and swear loyalty to the cross", textSk = "Spáliť knihu v peci a prisahať vernosť na kríž",
                    tagEn = "Renunciation", tagSk = "Zrieknutie", cardArchetype = "Church_Action",
                    consequence = ChoiceConsequence(
                        notorietyChange = -15,
                        removeItems = setOf("Forbidden_Manuscript"),
                        removeFlags = setOf("HERETIC_KNOWLEDGE"),
                        resolutionTextEn = "The pages curl and blacken in the furnace as you kneel and swear your oath anew.",
                        resolutionTextSk = "Stránky sa v peci krútia a čiernejú, kým kľačíš a nanovo skladáš prísahu.",
                        bridgeTextEn = "Malachai nods, satisfied, and moves on to question the next brother.",
                        bridgeTextSk = "Malachai spokojne prikývne a presunie sa vypočúvať ďalšieho brata."
                    )
                ),
                EventChoice(
                    id = 3, textEn = "Secretly flee the monastery and join peasant renegades", textSk = "Tajne utiecť z kláštora a pridať sa k poddaným renegátom",
                    tagEn = "Flight", tagSk = "Útek", cardArchetype = "Peasant_Action",
                    consequence = ChoiceConsequence(
                        factionChanges = mapOf(Faction.CHURCH to -30, Faction.PEASANTS to 30),
                        addFlags = setOf("RENEGADE_PRIEST"),
                        resolutionTextEn = "You slip over the monastery wall in the confusion of the Inquisitor's search.",
                        resolutionTextSk = "Prekĺzneš cez múr kláštora v zmätku Inkvizítorovho pátrania.",
                        bridgeTextEn = "The peasants in the hills take you in without asking too many questions.",
                        bridgeTextSk = "Poddaní v kopcoch ťa prijmú bez toho, aby sa priveľmi vypytovali."
                    )
                )
            )
        ),

        EventNode(
            id = "a2_excommunication",
            phase = EventPhase.PHASE_2,
            originClass = OriginClass.ACOLYTE,
            condition = { (it.factions[Faction.CHURCH] ?: 50) > 50 || it.worldFlags.contains("LOYAL_INFORMER") },
            titleEn = "The Excommunication of the Local Lord",
            titleSk = "Exkomunikácia Miestneho Lorda",
            textEn = "Lord Reginald refuses to pay his tithes. The Bishop wants to declare an interdict over the whole manor. The Lord secretly offers you a purse of gold to forge a papal bull in his favor.",
            textSk = "Lord Reginald refuzuje platiť desiatky. Biskup chce vyhlásiť interdikt nad celým panstvom. Lord ti tajne ponúka mešec zlata, ak falšuješ pápežskú bulu v jeho prospech.",
            location = "Castle",
            npcName = "Lord Reginald",
            npcTitle = "Tithe-Refusing Lord",
            npcArchetype = "NOBLE",
            choices = listOf(
                EventChoice(
                    id = 1, textEn = "Take the Lord's gold and forge a false papal bull", textSk = "Prijímať lordovo zlato a vyhotoviť falošnú bulu",
                    tagEn = "Forgery", tagSk = "Falzifikát", cardArchetype = "Underworld_Action",
                    consequence = ChoiceConsequence(
                        goldChange = 70, factionChanges = mapOf(Faction.NOBILITY to 30, Faction.CHURCH to -40),
                        addItems = setOf("Forged_Papal_Bull"),
                        addFlags = setOf("LORD_PUPPET"),
                        resolutionTextEn = "Your quill forges the papal seal with practiced precision, and Reginald's gold weighs heavy in your sleeve.",
                        resolutionTextSk = "Tvoje brko s precíznou zručnosťou sfalšuje pápežskú pečať a Reginaldovo zlato ti ťaží rukáv.",
                        bridgeTextEn = "The forged bull now sits folded against your chest, a secret weapon or a death sentence.",
                        bridgeTextSk = "Sfalšovaná bula teraz leží zložená na tvojej hrudi - tajná zbraň alebo rozsudok smrti."
                    )
                ),
                EventChoice(
                    id = 2, textEn = "Publicly excommunicate him at Sunday mass", textSk = "Verejne ho exkomunikovať na nedeľnej omši",
                    tagEn = "Excommunication", tagSk = "Exkomunikácia", cardArchetype = "Church_Action",
                    consequence = ChoiceConsequence(
                        regionalTensionChange = 20, factionChanges = mapOf(Faction.CHURCH to 30, Faction.NOBILITY to -40),
                        addFlags = setOf("CHURCH_CHAMPION"),
                        resolutionTextEn = "Your voice rings through the nave as Reginald's name is struck from the rolls of the faithful.",
                        resolutionTextSk = "Tvoj hlas sa nesie loďou katedrály, kým je Reginaldovo meno vyškrtnuté zo zoznamu veriacich.",
                        bridgeTextEn = "The congregation gasps, and the Lord storms out in fury.",
                        bridgeTextSk = "Zhromaždenie zalapá po dychu a Lord zúrivo odchádza."
                    )
                ),
                EventChoice(
                    id = 3, textEn = "Incite the poor to plunder the Lord's granaries", textSk = "Poštvať chudobu, aby vydrancovala lordove obilné sýpky",
                    tagEn = "Incitement", tagSk = "Podnecovanie", cardArchetype = "Peasant_Action",
                    consequence = ChoiceConsequence(
                        regionalTensionChange = 40, factionChanges = mapOf(Faction.PEASANTS to 30),
                        addFlags = setOf("HOLY_REBEL"),
                        resolutionTextEn = "A whispered word to the right hungry ears sends the granary gates crashing open by nightfall.",
                        resolutionTextSk = "Zašepkané slovo do správnych hladných uší privedie k tomu, že brány sýpky sa do súmraku s treskom otvoria.",
                        bridgeTextEn = "Reginald's stores empty into desperate hands, and no one traces it back to you - yet.",
                        bridgeTextSk = "Reginaldove zásoby sa vyprázdnia do zúfalých rúk a zatiaľ to k tebe nikto nevystopuje."
                    )
                )
            )
        ),

        EventNode(
            id = "a2_poisoned_wine",
            phase = EventPhase.PHASE_2,
            originClass = OriginClass.ACOLYTE,
            condition = { it.worldFlags.contains("MONASTERY_SCHEMER") || it.worldFlags.contains("CHURCH_THIEF") },
            titleEn = "Poison in the Monastery Wine",
            titleSk = "Jed v Kláštornom Víne",
            textEn = "You have discovered the Abbot plans to poison the Inquisitor at dinner, to cover up financial fraud and embezzled treasure.",
            textSk = "Zistil si, že opát plánuje otravu Inkvizítora pri večeri, aby zakryl finančné podvody a spreneveru pokladu.",
            location = "Cathedral",
            npcName = "Abbot Corvin",
            npcTitle = "Monastery Abbot",
            npcArchetype = "ELDER",
            choices = listOf(
                EventChoice(
                    id = 1, textEn = "Warn the Inquisitor and foil the assassination", textSk = "Varovať Inkvizítora a zmariť atentát",
                    tagEn = "Warning", tagSk = "Varovanie", cardArchetype = "Church_Action",
                    consequence = ChoiceConsequence(
                        factionChanges = mapOf(Faction.CHURCH to 40, Faction.PEASANTS to -30),
                        addFlags = setOf("INQUISITION_FAVORITE"),
                        resolutionTextEn = "Malachai's cup is quietly swapped moments before Corvin can act, and the Abbot's face drains of color.",
                        resolutionTextSk = "Malachaiov pohár je potichu vymenený chvíľu predtým, než Corvin stihne konať, a Abbatova tvár stráca farbu.",
                        bridgeTextEn = "The Inquisition now counts you among its most trusted informants.",
                        bridgeTextSk = "Inkvizícia ťa teraz počíta medzi svojich najdôveryhodnejších informátorov."
                    )
                ),
                EventChoice(
                    id = 2, textEn = "Stay silent and take the Abbot's bribe", textSk = "Mlčať a vziať úplatok od opáta za mlčanie",
                    tagEn = "Silence", tagSk = "Mlčanie", cardArchetype = "Underworld_Action",
                    consequence = ChoiceConsequence(
                        goldChange = 40, notorietyChange = 20,
                        addFlags = setOf("MURDER_COMPLICE"),
                        resolutionTextEn = "Corvin's purse finds its way into your hands, and you say nothing as dinner is served.",
                        resolutionTextSk = "Corvinov mešec sa dostane do tvojich rúk a mlčky sleduješ, ako sa podáva večera.",
                        bridgeTextEn = "Whatever happens at the table tonight, your hands remain clean - technically.",
                        bridgeTextSk = "Nech sa dnes pri stole stane čokoľvek, tvoje ruky zostávajú čisté - technicky."
                    )
                ),
                EventChoice(
                    id = 3, textEn = "Swap the cups and poison the Abbot himself", textSk = "Vymeniť poháre a otráviť samotného opáta",
                    tagEn = "Murder", tagSk = "Vražda", cardArchetype = "Underworld_Action",
                    consequence = ChoiceConsequence(
                        notorietyChange = 25, influenceChanges = mapOf(UNDERWORLD_AFFINITY to 30),
                        addFlags = setOf("ABBOT_KILLER"),
                        resolutionTextEn = "Corvin drinks from his own poisoned cup and slumps silently over the dinner table.",
                        resolutionTextSk = "Corvin pije z vlastného otráveného pohára a ticho sa zosunie na večerný stôl.",
                        bridgeTextEn = "The monastery's corruption dies with him - and so does any witness to yours.",
                        bridgeTextSk = "Korupcia kláštora zomiera s ním - a s ňou aj akýkoľvek svedok tej tvojej."
                    )
                )
            )
        ),

        EventNode(
            id = "a2_holy_uprising",
            phase = EventPhase.PHASE_2,
            originClass = OriginClass.ACOLYTE,
            condition = { it.regionalTension > 60 || it.worldFlags.contains("HOLY_REBEL") || it.worldFlags.contains("RENEGADE_PRIEST") },
            titleEn = "The Holy Uprising in the Undercity",
            titleSk = "Sväté Povstanie v Podhradí",
            textEn = "Desperate peasants have surrounded the cathedral. They demand the church's treasures be distributed and corrupt priests burned. You stand at the cathedral portal.",
            textSk = "Zúfalí poddaní obkľúčili katedrálu. Žiadajú rozdanie cirkevného cenného majetku a upálenie korupčných kňazov. Ty stojíš na portáli katedrály.",
            location = "Cathedral",
            npcName = "The Assembled Poor",
            npcTitle = "Uprising",
            npcArchetype = "PEASANT",
            choices = listOf(
                EventChoice(
                    id = 1, textEn = "Open the gates, take the monstrance, and give gold to the poor", textSk = "Otvoriť brány, vybrať monštranciu a rozdať zlato chudobe",
                    tagEn = "Redistribution", tagSk = "Prerozdelenie", cardArchetype = "Peasant_Action",
                    consequence = ChoiceConsequence(
                        goldChange = -50, factionChanges = mapOf(Faction.PEASANTS to 50, Faction.CHURCH to -80),
                        addFlags = setOf("HERETIC_PROPHET"),
                        resolutionTextEn = "Golden vessels spill into desperate hands as the crowd's roar turns to disbelieving joy.",
                        resolutionTextSk = "Zlaté nádoby sa vysypú do zúfalých rúk, kým rev davu prechádza do neveriaceho jasotu.",
                        bridgeTextEn = "You have broken every vow of your order tonight - and become something else entirely.",
                        bridgeTextSk = "Dnes v noci si porušil každý sľub svojho rádu - a stal si sa niečím úplne iným."
                    )
                ),
                EventChoice(
                    id = 2, textEn = "Lead the church guards with a cross in hand and scatter the crowd", textSk = "Viesť cirkevné stráže s krížom v ruke a rozprášiť dav",
                    tagEn = "Suppression", tagSk = "Potlačenie", cardArchetype = "Church_Action",
                    consequence = ChoiceConsequence(
                        factionChanges = mapOf(Faction.CHURCH to 30, Faction.NOBILITY to 20, Faction.PEASANTS to -50),
                        addFlags = setOf("BLOODY_INQUISITOR"),
                        resolutionTextEn = "Raised steel and raised crosses drive the crowd back down the cathedral steps in terror.",
                        resolutionTextSk = "Zdvihnutá oceľ a zdvihnuté kríže zaženú dav dolu katedrálnymi schodmi v hrôze.",
                        bridgeTextEn = "Blood stains the cathedral steps, and your name is spoken with fear now.",
                        bridgeTextSk = "Katedrálne schody sfarbuje krv a tvoje meno sa teraz vyslovuje so strachom."
                    )
                ),
                EventChoice(
                    id = 3, textEn = "Use the chaos, dress as a commoner, and flee with the treasury", textSk = "Využiť chaos, obliecť sa do šiat poddaného a utiecť s pokladnicou",
                    tagEn = "Theft", tagSk = "Krádež", cardArchetype = "Underworld_Action",
                    consequence = ChoiceConsequence(
                        goldChange = 80, notorietyChange = 40,
                        addFlags = setOf("SACRILEGIOUS_RUNAWAY"),
                        resolutionTextEn = "In the roaring chaos, you slip out a side door in stolen rags, treasury sack in hand.",
                        resolutionTextSk = "V hučiacom chaose vykĺzneš bočnými dverami v ukradnutých handrách, s vrecom z pokladnice v ruke.",
                        bridgeTextEn = "By the time anyone thinks to count the coin, you are already miles away.",
                        bridgeTextSk = "Kým niekoho napadne spočítať mince, ty si už míle odtiaľto."
                    )
                )
            )
        ),

        // ============ PHASE 3: CIRKEVNÝ KLIMAX A SÚD VIERY (Ťahy 17-25) ============

        EventNode(
            id = "a3_inquisition_trial",
            phase = EventPhase.PHASE_3,
            originClass = OriginClass.ACOLYTE,
            minTurn = 17, maxTurn = 24,
            forcedPriority = true,
            condition = { it.notoriety > 70 || it.worldFlags.contains("HERETIC_KNOWLEDGE") || it.worldFlags.contains("SACRILEGIOUS_RUNAWAY") },
            titleEn = "The Inquisitorial Trial and the Torture Chamber",
            titleSk = "Inkvizičný Súd a Mučiareň",
            textEn = "You stand shackled in the cathedral's underground cell. The Inquisitor prepares heated tongs. He demands the names of your accomplices.",
            textSk = "Stojíš spútaný v podzemnej kobke katedrály. Inkvizítor pripravuje rozpálené kliešte. Žiada mená spolupáchateľov.",
            location = "Cathedral",
            npcName = "Inquisitor Malachai",
            npcTitle = "Grand Inquisitor",
            npcArchetype = "BISHOP",
            choices = listOf(
                EventChoice(
                    id = 1, textEn = "Betray the whole underworld and denounce the manor conspirators", textSk = "Vyzradiť celé podsvetie a udať panských sprisahancov",
                    tagEn = "Betrayal", tagSk = "Zrada", cardArchetype = "Church_Action",
                    consequence = ChoiceConsequence(
                        factionChanges = mapOf(Faction.PEASANTS to -40, Faction.CHURCH to 30),
                        removeFlags = setOf("HERETIC_KNOWLEDGE"),
                        addFlags = setOf("PENITENT_TRAITOR"),
                        resolutionTextEn = "Names spill from your lips faster than Malachai can write them, and the tongs are set aside.",
                        resolutionTextSk = "Mená sa ti valia z pier rýchlejšie, než ich Malachai stíha zapisovať, a kliešte sú odložené.",
                        bridgeTextEn = "You are spared the iron, at the cost of everyone you once named a friend.",
                        bridgeTextSk = "Ušetria ťa železa, za cenu každého, koho si kedysi nazýval priateľom."
                    )
                ),
                EventChoice(
                    id = 2, textEn = "Use the forged papal bull and claim you act by the Pope's will", textSk = "Použiť Forged_Papal_Bull a tvrdiť, že konáš z vôle Pápeža",
                    tagEn = "Deception", tagSk = "Klamstvo", cardArchetype = "Underworld_Action",
                    consequence = ChoiceConsequence(
                        notorietyChange = 10,
                        resolutionTextEn = "You have no papal seal to show, and Malachai's tongs draw closer to the fire.",
                        resolutionTextSk = "Nemáš žiadnu pápežskú pečať na ukázanie a Malachaiove kliešte sa približujú k ohňu.",
                        bridgeTextEn = "An empty claim before the Inquisition only invites worse suspicion.",
                        bridgeTextSk = "Prázdne tvrdenie pred Inkvizíciou vyvoláva len horšie podozrenie."
                    ),
                    conditionalOutcome = { world ->
                        if (world.inventoryItemIds.contains("Forged_Papal_Bull")) {
                            ChoiceConsequence(
                                notorietyChange = -40,
                                removeItems = setOf("Forged_Papal_Bull"),
                                addFlags = setOf("PAPAL_PROTECTION"),
                                resolutionTextEn = "Malachai's hand freezes at the sight of the papal seal, and the tongs are lowered in uncertainty.",
                                resolutionTextSk = "Malachaiova ruka strne pri pohľade na pápežskú pečať a kliešte sú neisto spustené.",
                                bridgeTextEn = "No inquisitor dares act against the Pope's own word - forged or not.",
                                bridgeTextSk = "Žiadny inkvizítor sa neodváži konať proti slovu samotného pápeža - sfalšovanému, či nie."
                            )
                        } else {
                            ChoiceConsequence(
                                notorietyChange = 10,
                                resolutionTextEn = "You have no papal seal to show, and Malachai's tongs draw closer to the fire.",
                                resolutionTextSk = "Nemáš žiadnu pápežskú pečať na ukázanie a Malachaiove kliešte sa približujú k ohňu.",
                                bridgeTextEn = "An empty claim before the Inquisition only invites worse suspicion.",
                                bridgeTextSk = "Prázdne tvrdenie pred Inkvizíciou vyvoláva len horšie podozrenie."
                            )
                        }
                    }
                ),
                EventChoice(
                    id = 3, textEn = "Scream heretical lies and spit on the cross", textSk = "Vreštiť kacírske klamstvá a pľuť na kríž",
                    tagEn = "Defiance", tagSk = "Vzdor", cardArchetype = "Underworld_Action",
                    consequence = ChoiceConsequence(
                        regionalTensionChange = 50, notorietyChange = 40,
                        addFlags = setOf("CONDEMNED_HERETIC"),
                        resolutionTextEn = "Spittle strikes the crucifix as your defiant screams echo through the stone chamber.",
                        resolutionTextSk = "Slina zasiahne krucifix, kým tvoje vzdorné výkriky sa ozývajú kamennou komorou.",
                        bridgeTextEn = "There is no trial left to have - only the sentence.",
                        bridgeTextSk = "Už neostáva žiadny súd - len rozsudok."
                    )
                )
            )
        ),

        EventNode(
            id = "a3_monastery_plague",
            phase = EventPhase.PHASE_3,
            originClass = OriginClass.ACOLYTE,
            minTurn = 18, maxTurn = 22,
            forcedPriority = true,
            titleEn = "The Great Plague in the Monastery",
            titleSk = "Veľký Morový Mor v Kláštore",
            textEn = "Plague has breached the monastery walls. Brothers are dying in droves. The Abbot and the Inquisitor both lie feverish. The keys to the monastery treasury rest on the abandoned Bishop's chair.",
            textSk = "Mor prenikol za kláštorné hradby. Bratstvá vymierajú. Opát aj Inkvizítor ležia v horúčkach. Kľúče od kláštornej pokladnice ležia na stolici opusteného Biskupa.",
            location = "Cathedral",
            npcName = "The Dying Brotherhood",
            npcTitle = "Plague Ward",
            npcArchetype = "ELDER",
            choices = listOf(
                EventChoice(
                    id = 1, textEn = "Stay and tend the sick until you collapse from exhaustion", textSk = "Zostať a liečiť nemocných až do vlastného vysilenia",
                    tagEn = "Sacrifice", tagSk = "Obeta", cardArchetype = "Church_Action",
                    consequence = ChoiceConsequence(
                        healthChange = -30, factionChanges = mapOf(Faction.PEASANTS to 50, Faction.CHURCH to 30),
                        addFlags = setOf("MARTYR_OF_THE_PLAGUE"),
                        resolutionTextEn = "You move from cot to cot until your legs finally give out beneath you, but not one soul dies unattended.",
                        resolutionTextSk = "Prechádzaš od lôžka k lôžku, kým sa ti napokon nepodlomia nohy, no ani jedna duša nezomrie bez pomoci.",
                        bridgeTextEn = "The brotherhood will speak your name in prayer for generations.",
                        bridgeTextSk = "Bratstvo bude tvoje meno spomínať v modlitbách po generácie."
                    )
                ),
                EventChoice(
                    id = 2, textEn = "Break open the treasury, steal the holy relics, and flee", textSk = "Vylomiť pokladnicu, ukradnúť sväté relikvie a utiecť",
                    tagEn = "Theft", tagSk = "Krádež", cardArchetype = "Underworld_Action",
                    consequence = ChoiceConsequence(
                        goldChange = 100, notorietyChange = 30, influenceChanges = mapOf(UNDERWORLD_AFFINITY to 30),
                        addItems = setOf("Holy_Arka_Relic"),
                        addFlags = setOf("DESERTER_MONK"),
                        resolutionTextEn = "The treasury lock splinters under a stolen crowbar, and you vanish into the plague-emptied roads with gold and relics alike.",
                        resolutionTextSk = "Zámok pokladnice sa rozštiepi pod ukradnutou pákou a ty miznieš na morom vyprázdnených cestách so zlatom aj relikviami.",
                        bridgeTextEn = "Behind you, the dying brotherhood never even notices what's missing.",
                        bridgeTextSk = "Za tebou si umierajúce bratstvo ani nevšimne, čo chýba."
                    )
                ),
                EventChoice(
                    id = 3, textEn = "Lock the monastery gates and leave everyone inside to die", textSk = "Zamknúť brány kláštora a nechať všetkých vnútri zhorieť/zomrieť",
                    tagEn = "Abandonment", tagSk = "Opustenie", cardArchetype = "Noble_Action",
                    consequence = ChoiceConsequence(
                        factionChanges = mapOf(Faction.NOBILITY to 20, Faction.PEASANTS to -50),
                        addFlags = setOf("COLD_SANCTIFIER"),
                        resolutionTextEn = "The iron gates groan shut, sealing the plague and the brotherhood together behind you.",
                        resolutionTextSk = "Železné brány so zaskrípaním zapadnú a zapečatia mor aj bratstvo spolu za tebou.",
                        bridgeTextEn = "The manor praises your caution. The dead behind the gates have no voice to disagree.",
                        bridgeTextSk = "Panstvo chváli tvoju opatrnosť. Mŕtvi za bránami nemajú hlas na to, aby nesúhlasili."
                    )
                )
            )
        )
    )

    // ---------------------------------------------------------------------
    // GUILD APPRENTICE CHAPTER 1 DECK
    // ---------------------------------------------------------------------
    private val GUILD_NODES: List<EventNode> = listOf(

        // ============ PHASE 1: MESTSKÉ DNO A CHYBY V ÚČTOCH (Ťahy 1-7) ============

        EventNode(
            id = "g1_missing_silk",
            phase = EventPhase.PHASE_1,
            originClass = OriginClass.GUILD_APPRENTICE,
            minTurn = 1, maxTurn = 1,
            forcedPriority = true,
            titleEn = "The Missing Silk Shipment",
            titleSk = "Zmiznuté Balíky Hodvábu",
            textEn = "Guildmaster Corvus has discovered expensive Oriental goods missing from the warehouse. City guards pound on the workshop door. If no culprit is found, blame falls on you as the youngest apprentice.",
            textSk = "Cechmajster Corvus zistil, že v sklade chýba drahý tovar z Orientu. Mestská stráž búši na dvere dielne. Ak sa páchateľ nenájde, vina padne na teba ako na najmladšieho tovariša.",
            location = "Marketplace",
            npcName = "Guildmaster Corvus",
            npcTitle = "Silk Guildmaster",
            npcArchetype = "MERCHANT",
            choices = listOf(
                EventChoice(
                    id = 1, textEn = "Snitch on a fellow apprentice who drank in secret", textSk = "Udať kolegu tovariša, čo v noci tajne pil v krčme",
                    tagEn = "Betrayal", tagSk = "Zrada", cardArchetype = "Merchant_Action",
                    consequence = ChoiceConsequence(
                        factionChanges = mapOf(Faction.GUILDS to 10, Faction.PEASANTS to -25),
                        addFlags = setOf("SNITCH_FELLOW"),
                        resolutionTextEn = "Corvus's suspicion lands squarely on your unlucky fellow apprentice, and the guards drag him off instead.",
                        resolutionTextSk = "Corvusovo podozrenie padne priamo na tvojho nešťastného kolegu a stráže si namiesto teba odvedú jeho.",
                        bridgeTextEn = "Corvus presses the warehouse keys into your hand as a mark of trust.",
                        bridgeTextSk = "Corvus ti vloží do ruky kľúče od skladu ako znak dôvery."
                    )
                ),
                EventChoice(
                    id = 2, textEn = "Accept the blame and commit to working off the debt", textSk = "Prijať vinu a zaviazať sa k odpracovaniu dlhu",
                    tagEn = "Debt", tagSk = "Dlh", cardArchetype = "Peasant_Action",
                    consequence = ChoiceConsequence(
                        goldChange = -30, factionChanges = mapOf(Faction.GUILDS to 10),
                        addFlags = setOf("INDEBTED_APPRENTICE"),
                        resolutionTextEn = "Corvus nods grimly and marks the debt against your name in the guild ledger.",
                        resolutionTextSk = "Corvus pochmúrne prikývne a poznačí dlh proti tvojmu menu v cechovej knihe.",
                        bridgeTextEn = "The guards leave satisfied, but the debt will follow you for seasons to come.",
                        bridgeTextSk = "Stráže odchádzajú spokojné, no dlh ťa bude sprevádzať ešte celé sezóny."
                    )
                ),
                EventChoice(
                    id = 3, textEn = "Falsify the ledger before the guards arrive", textSk = "Falošne upraviť účtovnú knihu pred príchodom stráže",
                    tagEn = "Forgery", tagSk = "Falzifikát", cardArchetype = "Underworld_Action",
                    consequence = ChoiceConsequence(
                        notorietyChange = 10, influenceChanges = mapOf(UNDERWORLD_AFFINITY to 15),
                        addItems = setOf("Corrupt_Ledger"),
                        addFlags = setOf("FORGER"),
                        resolutionTextEn = "Ink barely dry, the doctored ledger satisfies the guards' cursory inspection.",
                        resolutionTextSk = "Sotva zaschnutý atrament, upravená kniha uspokojí povrchnú prehliadku stráží.",
                        bridgeTextEn = "The real numbers remain locked away in your memory - and your conscience.",
                        bridgeTextSk = "Skutočné čísla zostávajú uzamknuté len v tvojej pamäti - a svedomí."
                    )
                )
            )
        ),

        EventNode(
            id = "g1_night_poisoner",
            phase = EventPhase.PHASE_1,
            originClass = OriginClass.GUILD_APPRENTICE,
            minTurn = 2, maxTurn = 5,
            titleEn = "The Night Poisoner in the Guild Tavern",
            titleSk = "Nočný Travič v Cechovej Krčme",
            textEn = "A messenger from a rival guild offers you a purse of silver in a dark alley, if you slip a laxative into Guildmaster Corvus's beer before the city council's crucial vote.",
            textSk = "Posol z konkurenčného cechu ti v tmavej uličke ponúka mešec striebra, ak do piva cechmajstra Corvusa prisypeš preháňadlo pred dôležitým hlasovaním mestskej rady.",
            location = "Tavern",
            npcName = "Rival Messenger",
            npcTitle = "Guild Rival's Agent",
            npcArchetype = "BANDIT",
            choices = listOf(
                EventChoice(
                    id = 1, textEn = "Poison the cup and take the silver", textSk = "Nasypať jed do pohára a zobrať striebro",
                    tagEn = "Sabotage", tagSk = "Sabotáž", cardArchetype = "Underworld_Action",
                    consequence = ChoiceConsequence(
                        goldChange = 35, influenceChanges = mapOf(UNDERWORLD_AFFINITY to 15),
                        factionChanges = mapOf(Faction.GUILDS to -20),
                        addFlags = setOf("GUILD_SABOTEUR"),
                        resolutionTextEn = "Corvus doubles over mid-toast, and the council vote proceeds without his voice.",
                        resolutionTextSk = "Corvus sa uprostred prípitku zohne od bolesti a hlasovanie rady pokračuje bez jeho hlasu.",
                        bridgeTextEn = "The rival's silver feels heavier in your pocket than it should.",
                        bridgeTextSk = "Striebro rivala vo vrecku pôsobí ťažšie, než by malo."
                    )
                ),
                EventChoice(
                    id = 2, textEn = "Refuse and secretly warn Guildmaster Corvus", textSk = "Odstúpiť a tajne varovať cechmajstra Corvusa",
                    tagEn = "Loyalty", tagSk = "Vernosť", cardArchetype = "Merchant_Action",
                    consequence = ChoiceConsequence(
                        goldChange = -10, factionChanges = mapOf(Faction.GUILDS to 25),
                        addFlags = setOf("CORVUS_FAVORITE"),
                        resolutionTextEn = "Corvus swaps his cup without a word, eyes narrowing toward the rival's table across the room.",
                        resolutionTextSk = "Corvus bez slova vymení svoj pohár, jeho pohľad sa uprie na stôl rivala na druhej strane miestnosti.",
                        bridgeTextEn = "The rival guild's messenger never approaches you again after that night.",
                        bridgeTextSk = "Posol konkurenčného cechu sa k tebe po tej noci už nikdy nepriblíži."
                    )
                ),
                EventChoice(
                    id = 3, textEn = "Swap the cups and poison the messenger instead", textSk = "Vymeniť poháre a otráviť samotného posla",
                    tagEn = "Reversal", tagSk = "Obrat", cardArchetype = "Underworld_Action",
                    consequence = ChoiceConsequence(
                        notorietyChange = 10, influenceChanges = mapOf(UNDERWORLD_AFFINITY to 20),
                        addFlags = setOf("SILENT_ASSASSIN"),
                        resolutionTextEn = "The messenger's own laxative finds his cup instead, and he flees the tavern in visible distress.",
                        resolutionTextSk = "Poslov vlastný preháňadlo skončí v jeho pohári a on v badateľnej núdzi uteká z krčmy.",
                        bridgeTextEn = "No one in the tavern suspects the quiet apprentice in the corner.",
                        bridgeTextSk = "Nikto v krčme nepodozrieva tichého tovariša v kúte."
                    )
                )
            )
        ),

        EventNode(
            id = "g1_smuggler_cart",
            phase = EventPhase.PHASE_1,
            originClass = OriginClass.GUILD_APPRENTICE,
            condition = { it.worldFlags.contains("FORGER") || it.worldFlags.contains("INDEBTED_APPRENTICE") },
            titleEn = "The Smuggler's Cart at the Southern Gate",
            titleSk = "Pašerácky Voz pri Južnej Bráne",
            textEn = "The gate captain demands an unchristian toll for a cart of materials. An underworld smuggler offers you a cut of the profit if you use the guild seal and die to forge a false pass.",
            textSk = "Kapitán stráže pri bráne pýta nekresťanské mýto za voz s materiálom. Pašerák z podsvetia ti ponúka podiel zo zisku, ak použiješ cechovú pečať a razidlo na falšovanie priepustky.",
            location = "Village",
            npcName = "Smuggler Renner",
            npcTitle = "Underworld Cart Runner",
            npcArchetype = "BANDIT",
            choices = listOf(
                EventChoice(
                    id = 1, textEn = "Stamp the guild seal and smuggle the goods into the city workshop", textSk = "Otlačiť cechovú pečať a prepašovať tovar do mestskej dielne",
                    tagEn = "Smuggling", tagSk = "Pašovanie", cardArchetype = "Underworld_Action",
                    consequence = ChoiceConsequence(
                        goldChange = 40, notorietyChange = 15, influenceChanges = mapOf(UNDERWORLD_AFFINITY to 20),
                        addFlags = setOf("BLACK_MARKET_PARTNER"),
                        resolutionTextEn = "The forged seal passes the gate guard's cursory glance without a second look.",
                        resolutionTextSk = "Sfalšovaná pečať prejde povrchným pohľadom brányho strážcu bez druhého ohliadnutia.",
                        bridgeTextEn = "Renner's underworld contacts now consider you a reliable partner.",
                        bridgeTextSk = "Rennerovi kontakty z podsvetia ťa teraz považujú za spoľahlivého partnera."
                    )
                ),
                EventChoice(
                    id = 2, textEn = "Split the bribe with the guard instead", textSk = "Dohodnúť sa so strážnikom na rozdelení úplatku",
                    tagEn = "Bribe", tagSk = "Úplatok", cardArchetype = "Noble_Action",
                    consequence = ChoiceConsequence(
                        goldChange = 20, factionChanges = mapOf(Faction.NOBILITY to 15),
                        addFlags = setOf("GUARD_BRIBER"),
                        resolutionTextEn = "The guard's palm closes around half the coin, and the cart rolls through without incident.",
                        resolutionTextSk = "Strážcova dlaň sa zovrie okolo polovice mince a voz prejde bez incidentu.",
                        bridgeTextEn = "Renner shrugs off the smaller cut and moves on to the next opportunity.",
                        bridgeTextSk = "Renner pokrčí plecami nad menším podielom a presunie sa k ďalšej príležitosti."
                    )
                ),
                EventChoice(
                    id = 3, textEn = "Refuse them both and report the illegal goods to the magistrate", textSk = "Odmietnuť oboch a nelegálny tovar nahlásiť richtárovi",
                    tagEn = "Lawfulness", tagSk = "Zákonnosť", cardArchetype = "Church_Action",
                    consequence = ChoiceConsequence(
                        factionChanges = mapOf(Faction.GUILDS to 10),
                        influenceChanges = mapOf(UNDERWORLD_AFFINITY to -20),
                        addFlags = setOf("LAW_ABIDING_CITIZEN"),
                        resolutionTextEn = "The magistrate's men seize the cart, and Renner disappears into the crowd before they arrive.",
                        resolutionTextSk = "Richtárovi muži zaberú voz a Renner zmizne v dave skôr, než dorazia.",
                        bridgeTextEn = "The guild notes your honesty, though the underworld will remember your refusal.",
                        bridgeTextSk = "Cech si všimne tvoju čestnosť, hoci podsvetie si zapamätá tvoje odmietnutie."
                    )
                )
            )
        ),

        EventNode(
            id = "g1_moneylender_debt",
            phase = EventPhase.PHASE_1,
            originClass = OriginClass.GUILD_APPRENTICE,
            condition = { it.gold < 15 || it.worldFlags.contains("INDEBTED_APPRENTICE") },
            titleEn = "The City Moneylender's Debt Note",
            titleSk = "Dlžobný Úpis Mestského Úžerníka",
            textEn = "Moneylender Malakai's enforcer has you pressed against an alley wall. The workshop's debt has grown with interest. Either you pay immediately, or you hand over the keys to the main guild warehouse.",
            textSk = "Gorila úžerníka Malakaja ťa pritisla k stene v uličke. Dlh z dielne vzrástol o úroky. Buď okamžite zaplatíš, alebo odovzdáš kľúče od hlavného cechového skladu.",
            location = "Village",
            npcName = "Malakai's Enforcer",
            npcTitle = "Moneylender's Thug",
            npcArchetype = "BANDIT",
            choices = listOf(
                EventChoice(
                    id = 1, textEn = "Hand over the warehouse keys and open the doors at night", textSk = "Odovzdať kľúče od skladu a otvoriť im dvere v noci",
                    tagEn = "Betrayal", tagSk = "Zrada", cardArchetype = "Underworld_Action",
                    consequence = ChoiceConsequence(
                        factionChanges = mapOf(Faction.GUILDS to -30),
                        influenceChanges = mapOf(UNDERWORLD_AFFINITY to 25),
                        addFlags = setOf("WAREHOUSE_BETRAYER"),
                        resolutionTextEn = "The warehouse locks click open under your own hand as Malakai's men slip inside in the dark.",
                        resolutionTextSk = "Zámky skladu cvaknú a otvoria sa pod tvojou vlastnou rukou, kým sa Malakajovi muži potme vkradnú dnu.",
                        bridgeTextEn = "By morning the debt is settled - and the guild's trust in you is gone.",
                        bridgeTextSk = "Do rána je dlh vyrovnaný - a dôvera cechu v teba je preč."
                    )
                ),
                EventChoice(
                    id = 2, textEn = "Burn Malakai's house down and destroy the debt notes", textSk = "Podpáliť Malakajov dom a spáliť dlžobné úpisy",
                    tagEn = "Arson", tagSk = "Podpaľačstvo", cardArchetype = "Underworld_Action",
                    consequence = ChoiceConsequence(
                        notorietyChange = 30, regionalTensionChange = 20,
                        addFlags = setOf("ARSONIST"),
                        removeFlags = setOf("INDEBTED_APPRENTICE"),
                        resolutionTextEn = "Flames consume Malakai's ledgers along with every trace of your debt.",
                        resolutionTextSk = "Plamene strávia Malakajove knihy spolu s každou stopou tvojho dlhu.",
                        bridgeTextEn = "The night sky glows orange as you slip away from the smoking ruin.",
                        bridgeTextSk = "Nočná obloha žiari oranžovo, kým sa vzďaľuješ od dymiacich trosiek."
                    )
                ),
                EventChoice(
                    id = 3, textEn = "Ask the manor guard for protection in exchange for informing on the moneylender", textSk = "Požiadať o ochranu panskú gardu výmenou za udanie úžerníka",
                    tagEn = "Informing", tagSk = "Udanie", cardArchetype = "Noble_Action",
                    consequence = ChoiceConsequence(
                        factionChanges = mapOf(Faction.NOBILITY to 20),
                        influenceChanges = mapOf(UNDERWORLD_AFFINITY to -40),
                        addFlags = setOf("CITY_INFORMANT"),
                        resolutionTextEn = "Manor guards drag Malakai's enforcer away as you watch from behind their shields.",
                        resolutionTextSk = "Panská garda odvlečie Malakajovho gorilu preč, kým to sleduješ spoza ich štítov.",
                        bridgeTextEn = "The underworld will not soon forgive an informant in its midst.",
                        bridgeTextSk = "Podsvetie tak skoro neodpustí donášača vo svojich radoch."
                    )
                )
            )
        ),

        // ============ PHASE 2: MESTSKÁ ELITA VS. PODSVETIE (Ťahy 8-16) ============

        EventNode(
            id = "g2_golden_contract",
            phase = EventPhase.PHASE_2,
            originClass = OriginClass.GUILD_APPRENTICE,
            condition = { (it.factions[Faction.GUILDS] ?: 50) > 40 || it.worldFlags.contains("CORVUS_FAVORITE") },
            titleEn = "The Golden Contract for the Manor Court",
            titleSk = "Zlatá Zmluva pre Panský Dvor",
            textEn = "The manor chamberlain seeks an exclusive supplier of luxury goods for the castle. Guildmaster Corvus lies feverish, and has entrusted you with negotiating the terms.",
            textSk = "Panský komorník hľadá exkluzívneho dodávateľa luxusného tovaru pre hrad. Cechmajster leží v horúčkach a vyjednávaním podmienok poveril teba.",
            location = "Castle",
            npcName = "Chamberlain Voss",
            npcTitle = "Manor Chamberlain",
            npcArchetype = "NOBLE",
            choices = listOf(
                EventChoice(
                    id = 1, textEn = "Inflate the contract price and quietly divert half the profit", textSk = "Nadsadiť cenu zmluvy a polovicu zisku tajne odkloniť na vlastný účet",
                    tagEn = "Embezzlement", tagSk = "Sprenevera", cardArchetype = "Underworld_Action",
                    consequence = ChoiceConsequence(
                        goldChange = 60, notorietyChange = 10, factionChanges = mapOf(Faction.NOBILITY to -15),
                        addFlags = setOf("EMBEZZLER"),
                        resolutionTextEn = "Voss signs without reading closely, and half the sum quietly finds its way into your own strongbox.",
                        resolutionTextSk = "Voss podpíše bez dôkladného čítania a polovica sumy si potichu nájde cestu do tvojej vlastnej pokladničky.",
                        bridgeTextEn = "The guild's coffers show a curiously modest gain from such a grand contract.",
                        bridgeTextSk = "Cechová pokladnica vykazuje kuriózne skromný zisk z takej veľkolepej zmluvy."
                    )
                ),
                EventChoice(
                    id = 2, textEn = "Negotiate an honest, favorable contract for the guild", textSk = "Uzatvoriť výhodnú a poctivú zmluvu v prospech cechu",
                    tagEn = "Honest Trade", tagSk = "Čestný Obchod", cardArchetype = "Merchant_Action",
                    consequence = ChoiceConsequence(
                        factionChanges = mapOf(Faction.GUILDS to 30, Faction.NOBILITY to 25),
                        addFlags = setOf("GUILD_BENEFACTOR"),
                        resolutionTextEn = "Voss shakes your hand firmly, impressed by terms that favor both castle and craftsmen alike.",
                        resolutionTextSk = "Voss ti pevne stlačí ruku, ohromený podmienkami, ktoré prospievajú hradu aj remeselníkom.",
                        bridgeTextEn = "Word of your fair dealing reaches the guild elders before nightfall.",
                        bridgeTextSk = "Chýr o tvojom férovom jednaní sa dostane k cechovým starším ešte pred súmrakom."
                    )
                ),
                EventChoice(
                    id = 3, textEn = "Offer the chamberlain forbidden black-market goods", textSk = "Ponúknuť komorníkovi skrytý, zakázaný tovar z čierneho trhu",
                    tagEn = "Corruption", tagSk = "Korupcia", cardArchetype = "Underworld_Action",
                    consequence = ChoiceConsequence(
                        goldChange = 80, influenceChanges = mapOf(UNDERWORLD_AFFINITY to 25),
                        addItems = setOf("Nobleman_Secret"),
                        addFlags = setOf("COURT_CORRUPTOR"),
                        resolutionTextEn = "Voss's eyes gleam at the forbidden wares, and a secret now binds you both.",
                        resolutionTextSk = "Vossovi zažiaria oči nad zakázaným tovarom a teraz vás oboch viaže spoločné tajomstvo.",
                        bridgeTextEn = "The chamberlain's discretion is bought - along with his future obedience.",
                        bridgeTextSk = "Komorníkova mlčanlivosť je kúpená - spolu s jeho budúcou poslušnosťou."
                    )
                )
            )
        ),

        EventNode(
            id = "g2_apprentice_revolt",
            phase = EventPhase.PHASE_2,
            originClass = OriginClass.GUILD_APPRENTICE,
            condition = { it.regionalTension > 50 },
            titleEn = "The Revolt of the Guild Apprentices",
            titleSk = "Vzbura Cechových Tovarišov",
            textEn = "Workshop laborers strike for humane conditions, smashing expensive tools. The manor magistrate sends mercenaries with greatswords to clear the streets.",
            textSk = "Pracovníci v dielňach štrajkujú za ľudskejšie podmienky a ničia drahé nástroje. Panský richtár posiela žoldnierov s obojručnými mečmi na vyčistenie ulíc.",
            location = "Marketplace",
            npcName = "Apprentice Fenn",
            npcTitle = "Strike Organizer",
            npcArchetype = "PEASANT",
            choices = listOf(
                EventChoice(
                    id = 1, textEn = "Lead the apprentices and demand civic rights", textSk = "Postaviť sa na čelo tovarišov a žiadať mestské prístupové práva",
                    tagEn = "Rebellion", tagSk = "Povstanie", cardArchetype = "Peasant_Action",
                    consequence = ChoiceConsequence(
                        regionalTensionChange = 30, factionChanges = mapOf(Faction.PEASANTS to 30, Faction.GUILDS to -40),
                        addFlags = setOf("GUILD_REBEL"),
                        resolutionTextEn = "Fenn's fist rises alongside yours as the assembled apprentices roar their demands at the mercenary line.",
                        resolutionTextSk = "Fennova päsť sa zdvihne popri tvojej, kým zhromaždení tovariši revú svoje požiadavky proti línii žoldnierov.",
                        bridgeTextEn = "The guild elders watch your defiance with cold, calculating eyes.",
                        bridgeTextSk = "Cechoví starší sledujú tvoj vzdor chladnými, vypočítavými očami."
                    )
                ),
                EventChoice(
                    id = 2, textEn = "Secretly supply weapons to the manor mercenaries", textSk = "Dodať tajne zbrane panským žoldnierom a potlačiť vzburu",
                    tagEn = "Suppression", tagSk = "Potlačenie", cardArchetype = "Noble_Action",
                    consequence = ChoiceConsequence(
                        goldChange = 30, factionChanges = mapOf(Faction.NOBILITY to 30, Faction.PEASANTS to -50),
                        addFlags = setOf("PATRICIAN_TOOL"),
                        resolutionTextEn = "Crates of blades change hands quietly behind the workshop, and the mercenaries advance with confidence.",
                        resolutionTextSk = "Debny s čepeľami potichu menia majiteľa za dielňou a žoldnieri postupujú so sebavedomím.",
                        bridgeTextEn = "The patrician families take note of a guildsman willing to arm their cause.",
                        bridgeTextSk = "Patricijské rodiny si všimnú cechára ochotného vyzbrojiť ich vec."
                    )
                ),
                EventChoice(
                    id = 3, textEn = "Lock yourself in the warehouse and guard only the richest chests", textSk = "Zamknúť sa v sklade a chrániť len najdrahšie cechové truhlice",
                    tagEn = "Self-Interest", tagSk = "Sebectvo", cardArchetype = "Merchant_Action",
                    consequence = ChoiceConsequence(
                        goldChange = 25, regionalTensionChange = 10,
                        resolutionTextEn = "You bar the warehouse door and listen to the riot rage past outside, coin close at hand.",
                        resolutionTextSk = "Zatarasíš dvere skladu a počúvaš, ako okolo zúri výtržnosť, mince po ruke.",
                        bridgeTextEn = "The chests survive the night untouched - unlike the streets outside.",
                        bridgeTextSk = "Truhlice noc prežijú nedotknuté - na rozdiel od ulíc vonku."
                    )
                )
            )
        ),

        EventNode(
            id = "g2_counterfeit",
            phase = EventPhase.PHASE_2,
            originClass = OriginClass.GUILD_APPRENTICE,
            condition = { it.worldFlags.contains("FORGER") || it.worldFlags.contains("BLACK_MARKET_PARTNER") },
            titleEn = "Counterfeit Coinage in the Undercity",
            titleSk = "Falošné Mincovníctvo v Podzemí",
            textEn = "An underground coiners' guild has contacted you. They have a die for royal ducats and need your knowledge of metals to alloy lead with silver.",
            textSk = "Podzemný spolok peňazokazov ťa kontaktoval. Majú razidlo na kráľovské dukáty a potrebujú tvoje znalosti kovov na legovanie olova so striebrom.",
            location = "Forest",
            npcName = "Coiner Hollow",
            npcTitle = "Underground Coiner",
            npcArchetype = "BANDIT",
            choices = listOf(
                EventChoice(
                    id = 1, textEn = "Help cast counterfeit coins for a third share", textSk = "Pomôcť odlievať falošné mince za tretinový podiel",
                    tagEn = "Counterfeiting", tagSk = "Falšovanie", cardArchetype = "Underworld_Action",
                    consequence = ChoiceConsequence(
                        goldChange = 70, notorietyChange = 30,
                        addItems = setOf("Counterfeit_Coins"),
                        addFlags = setOf("COINER"),
                        resolutionTextEn = "Molten silver-lead alloy hisses into the mold, taking the shape of the crown's own seal.",
                        resolutionTextSk = "Roztavená striebro-olovená zliatina zasyčí do formy a nadobudne tvar samotnej kráľovskej pečate.",
                        bridgeTextEn = "Your share of the counterfeit hoard weighs heavy and cold in your pocket.",
                        bridgeTextSk = "Tvoj podiel z falošného pokladu ti vo vrecku ťaží chladne a ťažko."
                    )
                ),
                EventChoice(
                    id = 2, textEn = "Report the coiners directly to the city council", textSk = "Udať peňazokazov priamo do rúk mestskej rady",
                    tagEn = "Loyalty", tagSk = "Vernosť", cardArchetype = "Merchant_Action",
                    consequence = ChoiceConsequence(
                        goldChange = 20, factionChanges = mapOf(Faction.GUILDS to 30),
                        influenceChanges = mapOf(UNDERWORLD_AFFINITY to -40),
                        addFlags = setOf("CROWN_LOYALIST"),
                        resolutionTextEn = "City guards raid the counterfeiters' den before the next batch ever cools.",
                        resolutionTextSk = "Mestská stráž vtrhne do doupäťa peňazokazov skôr, než ďalšia dávka vôbec vychladne.",
                        bridgeTextEn = "The council rewards your loyalty with a modest purse and public thanks.",
                        bridgeTextSk = "Rada odmení tvoju vernosť skromným mešcom a verejným poďakovaním."
                    )
                ),
                EventChoice(
                    id = 3, textEn = "Rob the coiners of their die and flee", textSk = "Okrať peňazokazov o razidlo a utiecť",
                    tagEn = "Theft", tagSk = "Krádež", cardArchetype = "Underworld_Action",
                    consequence = ChoiceConsequence(
                        notorietyChange = 20, influenceChanges = mapOf(UNDERWORLD_AFFINITY to 15),
                        addItems = setOf("Royal_Mint_Die"),
                        resolutionTextEn = "The royal die slips into your coat while Hollow's men argue over the last casting.",
                        resolutionTextSk = "Kráľovské razidlo skĺzne do tvojho kabáta, kým sa Hollowovi muži hádajú o poslednom odliatku.",
                        bridgeTextEn = "You vanish into the tunnels before anyone notices the theft.",
                        bridgeTextSk = "Zmizneš v tuneloch skôr, než si niekto všimne krádež."
                    )
                )
            )
        ),

        EventNode(
            id = "g2_barricades",
            phase = EventPhase.PHASE_2,
            originClass = OriginClass.GUILD_APPRENTICE,
            condition = { it.regionalTension > 70 || it.worldFlags.contains("GUILD_REBEL") || it.worldFlags.contains("PATRICIAN_TOOL") },
            titleEn = "Barricades at the City Hall",
            titleSk = "Barikády na Mestskej Radnici",
            textEn = "The city hall is aflame. The poor and the apprentices fight the manor guard for control of the city treasury.",
            textSk = "Radnica je v plameňoch. Chudoba a tovariši bojujú s panskou gardou o kontrolu nad mestskou pokladnicou.",
            location = "Castle",
            npcName = "Magistrate Voclain",
            npcTitle = "City Magistrate",
            npcArchetype = "NOBLE",
            choices = listOf(
                EventChoice(
                    id = 1, textEn = "Break open the treasury and share the gold with the apprentices", textSk = "Vylomiť dvere radničnej pokladnice a podeliť zlato tovarišom",
                    tagEn = "Redistribution", tagSk = "Prerozdelenie", cardArchetype = "Peasant_Action",
                    consequence = ChoiceConsequence(
                        notorietyChange = 30, factionChanges = mapOf(Faction.GUILDS to -30, Faction.PEASANTS to 50),
                        addFlags = setOf("PEOPLE_TRIBUNE"),
                        resolutionTextEn = "Gold cascades into desperate hands as the treasury door finally gives way under the crowd's weight.",
                        resolutionTextSk = "Zlato sa vysype do zúfalých rúk, kým dvere pokladnice napokon povolia pod váhou davu.",
                        bridgeTextEn = "The guild elders will never forgive this night, but the streets will never forget it either.",
                        bridgeTextSk = "Cechoví starší túto noc nikdy neodpustia, no ani ulice na ňu nikdy nezabudnú."
                    )
                ),
                EventChoice(
                    id = 2, textEn = "Defend the city archives with your own body against the mob", textSk = "Brániť radničné archívy vlastným telom pred davom",
                    tagEn = "Defense", tagSk = "Obrana", cardArchetype = "Noble_Action",
                    consequence = ChoiceConsequence(
                        factionChanges = mapOf(Faction.GUILDS to 40, Faction.NOBILITY to 30, Faction.PEASANTS to -40),
                        addFlags = setOf("SAVIOR_OF_THE_CITY"),
                        resolutionTextEn = "You stand between the flames and the archive doors until the magistrate's men finally arrive.",
                        resolutionTextSk = "Stojíš medzi plameňmi a dverami archívu, kým konečne nedorazia richtárovi muži.",
                        bridgeTextEn = "Voclain calls you the savior of the city's records before the assembled council.",
                        bridgeTextSk = "Voclain ťa pred zhromaždenou radou nazve záchrancom mestských záznamov."
                    )
                ),
                EventChoice(
                    id = 3, textEn = "Use the chaos to burn the debt ledger and steal the city seal", textSk = "Využiť zmätok, podpáliť knihu dlhov a ukradnúť mestskú pečať",
                    tagEn = "Theft", tagSk = "Krádež", cardArchetype = "Underworld_Action",
                    consequence = ChoiceConsequence(
                        goldChange = 60, influenceChanges = mapOf(UNDERWORLD_AFFINITY to 25),
                        addItems = setOf("City_Seal"),
                        addFlags = setOf("MASTER_THIEF"),
                        resolutionTextEn = "Flames devour the debt ledger while the city's own seal disappears into your coat unnoticed.",
                        resolutionTextSk = "Plamene strávia knihu dlhov, kým samotná mestská pečať nepozorovane zmizne v tvojom kabáte.",
                        bridgeTextEn = "By dawn, half the city's debts have simply ceased to exist - along with the seal.",
                        bridgeTextSk = "Do rána polovica mestských dlhov jednoducho prestane existovať - spolu s pečaťou."
                    )
                )
            )
        ),

        // ============ PHASE 3: MESTSKÝ KLIMAX A SÚDNE ZÚČTOVANIE (Ťahy 17-25) ============

        EventNode(
            id = "g3_inquisition_court",
            phase = EventPhase.PHASE_3,
            originClass = OriginClass.GUILD_APPRENTICE,
            minTurn = 17, maxTurn = 24,
            forcedPriority = true,
            condition = { it.notoriety > 70 || it.worldFlags.contains("EMBEZZLER") || it.worldFlags.contains("BLACK_MARKET_PARTNER") || it.worldFlags.contains("COINER") },
            titleEn = "The Inquisitorial Court Raid on the City Hall",
            titleSk = "Inkvizičný a Súdny Záťah na Radnici",
            textEn = "The Inquisitor and the city judge have seized the guild ledgers. They found your name tied to smuggling, usury, and coin debasement.",
            textSk = "Inkvizítor a mestský sudca zaistili cechové účtovné knihy. Našli tvoje meno spojené s pašovaním, úžerou a znehodnocovaním meny.",
            location = "Castle",
            npcName = "Judge Harlow",
            npcTitle = "City Judge",
            npcArchetype = "NOBLE",
            choices = listOf(
                EventChoice(
                    id = 1, textEn = "Bribe the judge with proceeds from black-market dealings", textSk = "Podplatiť sudcu ziskom z čiernych obchodov a nelegálnych razieb",
                    tagEn = "Bribe", tagSk = "Úplatok", cardArchetype = "Merchant_Action",
                    consequence = ChoiceConsequence(
                        notorietyChange = 10,
                        resolutionTextEn = "You have no coin left to offer, and Harlow's gavel comes down without mercy.",
                        resolutionTextSk = "Nemáš žiadnu mincu na ponuku a Harlowovo kladivo dopadá bez milosti.",
                        bridgeTextEn = "An empty purse before the court only deepens your sentence.",
                        bridgeTextSk = "Prázdny mešec pred súdom len prehlbuje tvoj rozsudok."
                    ),
                    conditionalOutcome = { world ->
                        if (world.gold >= 60) {
                            ChoiceConsequence(
                                goldChange = -60, notorietyChange = -30,
                                addFlags = setOf("BRIBED_JUDGE"),
                                resolutionTextEn = "Harlow's palm closes around the coin beneath the bench, and the charges quietly evaporate.",
                                resolutionTextSk = "Harlowova dlaň sa zovrie okolo mince pod lavicou a obvinenia potichu vyprchajú.",
                                bridgeTextEn = "The ledger entries concerning your name are quietly amended.",
                                bridgeTextSk = "Zápisy v knihe týkajúce sa tvojho mena sú potichu upravené."
                            )
                        } else {
                            ChoiceConsequence(
                                notorietyChange = 10,
                                resolutionTextEn = "You have no coin left to offer, and Harlow's gavel comes down without mercy.",
                                resolutionTextSk = "Nemáš žiadnu mincu na ponuku a Harlowovo kladivo dopadá bez milosti.",
                                bridgeTextEn = "An empty purse before the court only deepens your sentence.",
                                bridgeTextSk = "Prázdny mešec pred súdom len prehlbuje tvoj rozsudok."
                            )
                        }
                    }
                ),
                EventChoice(
                    id = 2, textEn = "Pin all the blame on the ailing Guildmaster Corvus with the corrupt ledger", textSk = "Hodiť celú vinu na chorého cechmajstra Corvusa a dodať Corrupt_Ledger",
                    tagEn = "Scapegoat", tagSk = "Obetný Baránok", cardArchetype = "Underworld_Action",
                    consequence = ChoiceConsequence(
                        notorietyChange = 10,
                        resolutionTextEn = "You have no doctored ledger to present, and the judge's questions turn back toward you.",
                        resolutionTextSk = "Nemáš žiadnu upravenú knihu na predloženie a sudcove otázky sa obracajú späť na teba.",
                        bridgeTextEn = "Without proof, your accusation rings hollow before the court.",
                        bridgeTextSk = "Bez dôkazu znie tvoje obvinenie pred súdom prázdno."
                    ),
                    conditionalOutcome = { world ->
                        if (world.inventoryItemIds.contains("Corrupt_Ledger")) {
                            ChoiceConsequence(
                                factionChanges = mapOf(Faction.GUILDS to -50, Faction.NOBILITY to 20),
                                removeItems = setOf("Corrupt_Ledger"),
                                addFlags = setOf("CORVUS_SCAPEGOAT"),
                                resolutionTextEn = "The doctored ledger damns Corvus in the judge's eyes, and guards drag the feverish guildmaster to the tower.",
                                resolutionTextSk = "Upravená kniha usvedčí Corvusa v sudcových očiach a stráže odvlečú horúčkou zmoreného cechmajstra do veže.",
                                bridgeTextEn = "The guild hall falls silent as its master is led away in your place.",
                                bridgeTextSk = "Cechová hala stíchne, keď jej majstra odvedú namiesto teba."
                            )
                        } else {
                            ChoiceConsequence(
                                notorietyChange = 10,
                                resolutionTextEn = "You have no doctored ledger to present, and the judge's questions turn back toward you.",
                                resolutionTextSk = "Nemáš žiadnu upravenú knihu na predloženie a sudcove otázky sa obracajú späť na teba.",
                                bridgeTextEn = "Without proof, your accusation rings hollow before the court.",
                                bridgeTextSk = "Bez dôkazu znie tvoje obvinenie pred súdom prázdno."
                            )
                        }
                    }
                ),
                EventChoice(
                    id = 3, textEn = "Jump from the city hall window and flee into the sewers", textSk = "Vyskočiť z okna radnice a utiecť do mestskej kanalizácie",
                    tagEn = "Escape", tagSk = "Útek", cardArchetype = "Underworld_Action",
                    consequence = ChoiceConsequence(
                        goldChange = -9999, influenceChanges = mapOf(UNDERWORLD_AFFINITY to 30),
                        addFlags = setOf("SEWER_RAT"),
                        resolutionTextEn = "You crash through the shutters and drop into the filth below as shouts erupt above.",
                        resolutionTextSk = "Prerazíš okenice a spadneš do špiny dolu, kým hore vypuknú výkriky.",
                        bridgeTextEn = "Everything you owned is left behind in the courtroom above.",
                        bridgeTextSk = "Všetko, čo si vlastnil, zostáva v súdnej sieni hore."
                    )
                )
            )
        ),

        EventNode(
            id = "g3_blockade",
            phase = EventPhase.PHASE_3,
            originClass = OriginClass.GUILD_APPRENTICE,
            minTurn = 18, maxTurn = 22,
            forcedPriority = true,
            titleEn = "The Great Blockade of the Trade Roads",
            titleSk = "Veľká Blokáda Obchodných Ciest",
            textEn = "Surrounding manor families have declared a blockade on the city. Flour and raw material stocks dry up, and guilds are collapsing. Guildmasters sell off assets for a fraction of their worth.",
            textSk = "Okolité panské rodiny vyhlásili mestu blokádu. Zasychanie zásob múky a surovín spôsobuje pád cechov. Cechmajstri rozpredávajú majetky za zlomok ceny.",
            location = "Marketplace",
            npcName = "Guildmaster Corvus",
            npcTitle = "Silk Guildmaster",
            npcArchetype = "MERCHANT",
            choices = listOf(
                EventChoice(
                    id = 1, textEn = "Buy out failing workshops with your accumulated savings", textSk = "Skúpiť krachujúce dielne za svoje nahromadené úspory",
                    tagEn = "Monopoly", tagSk = "Monopol", cardArchetype = "Merchant_Action",
                    consequence = ChoiceConsequence(
                        factionChanges = mapOf(Faction.GUILDS to 10),
                        resolutionTextEn = "You have no savings substantial enough to buy anything of worth in this crisis.",
                        resolutionTextSk = "Nemáš dostatočné úspory na to, aby si v tejto kríze kúpil čokoľvek hodnotné.",
                        bridgeTextEn = "You watch other, wealthier guildsmen seize the failing workshops instead.",
                        bridgeTextSk = "Sleduješ, ako krachujúce dielne namiesto teba získavajú iní, bohatší cechári."
                    ),
                    conditionalOutcome = { world ->
                        if (world.gold >= 50) {
                            ChoiceConsequence(
                                goldChange = -50, factionChanges = mapOf(Faction.GUILDS to 50),
                                addFlags = setOf("MONOPOLIST"),
                                resolutionTextEn = "One by one, desperate guildmasters sign their workshops over to you for a pittance.",
                                resolutionTextSk = "Zúfalí cechmajstri jeden po druhom prepisujú svoje dielne na teba za babku.",
                                bridgeTextEn = "By the blockade's end, half the district's workshops answer to your name.",
                                bridgeTextSk = "Do konca blokády polovica dielní v štvrti odpovedá na tvoje meno."
                            )
                        } else {
                            ChoiceConsequence(
                                factionChanges = mapOf(Faction.GUILDS to 10),
                                resolutionTextEn = "You have no savings substantial enough to buy anything of worth in this crisis.",
                                resolutionTextSk = "Nemáš dostatočné úspory na to, aby si v tejto kríze kúpil čokoľvek hodnotné.",
                                bridgeTextEn = "You watch other, wealthier guildsmen seize the failing workshops instead.",
                                bridgeTextSk = "Sleduješ, ako krachujúce dielne namiesto teba získavajú iní, bohatší cechári."
                            )
                        }
                    }
                ),
                EventChoice(
                    id = 2, textEn = "Smuggle food into the city through the sewers and sell it at a markup", textSk = "Prepašovať potraviny do mesta cez kanalizáciu a predávať ich s prirážkou",
                    tagEn = "War Profiteering", tagSk = "Vojnové Zbohatlíctvo", cardArchetype = "Underworld_Action",
                    consequence = ChoiceConsequence(
                        goldChange = 70, influenceChanges = mapOf(UNDERWORLD_AFFINITY to 20),
                        factionChanges = mapOf(Faction.PEASANTS to -20),
                        addFlags = setOf("WAR_PROFITEEER"),
                        resolutionTextEn = "Sacks of flour emerge from the sewer tunnels, and starving families pay whatever price you name.",
                        resolutionTextSk = "Vrecia múky sa vynárajú z kanalizačných tunelov a hladujúce rodiny platia akúkoľvek cenu, ktorú stanovíš.",
                        bridgeTextEn = "Your profit grows with every desperate customer, and so does their resentment.",
                        bridgeTextSk = "Tvoj zisk rastie s každým zúfalým zákazníkom, rovnako ako ich zášť."
                    )
                ),
                EventChoice(
                    id = 3, textEn = "Join the manor armies outside the walls and betray the gate's weaknesses", textSk = "Pridať sa k panským vojskám za hradbami a vyzradiť im slabiny mestskej brány",
                    tagEn = "Betrayal", tagSk = "Zrada", cardArchetype = "Noble_Action",
                    consequence = ChoiceConsequence(
                        goldChange = 40, factionChanges = mapOf(Faction.NOBILITY to 40, Faction.GUILDS to -80),
                        addFlags = setOf("CITY_TRAITOR"),
                        resolutionTextEn = "You mark the crumbling section of wall on a map and hand it to the besieging captain under cover of darkness.",
                        resolutionTextSk = "Na mape označíš rozpadávajúcu sa časť hradby a pod rúškom tmy ju odovzdáš obliehajúcemu kapitánovi.",
                        bridgeTextEn = "The blockade will end soon - one way or another, and by your hand.",
                        bridgeTextSk = "Blokáda čoskoro skončí - tak či onak, a tvojou rukou."
                    )
                )
            )
        )
    )
}
