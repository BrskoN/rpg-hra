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
        else -> emptyList()
    }

    val ALL_NODES: List<EventNode> get() = PEASANT_NODES

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
}
