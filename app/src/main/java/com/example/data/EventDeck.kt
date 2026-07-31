package com.example.data

/**
 * Hybrid content engine: authored branching event graph that is the single source of truth
 * for choices and their consequences. Gemini is no longer required to invent plot or stat deltas
 * for origins covered by a deck below - it is only ever asked to add sensory flavor on top,
 * which keeps decisions/consequences scalable, offline-capable, and free of AI drift.
 */
enum class NodeKind {
    OPENING,
    SANDBOX,
    ANCHOR
}

data class ChoiceConsequence(
    val goldChange: Int = 0,
    val healthChange: Int = 0,
    val regionalTensionChange: Int = 0,
    val notorietyChange: Int = 0,
    val factionChanges: Map<Faction, Int> = emptyMap(),
    val addFlags: Set<String> = emptySet(),
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
    val consequence: ChoiceConsequence,
    /** If set, forces the very next scenario to this node id instead of a fresh pool pick. */
    val nextNodeId: String? = null
)

data class EventNode(
    val id: String,
    val kind: NodeKind,
    val originClass: OriginClass? = null,
    val requiredFlags: Set<String> = emptySet(),
    val excludedFlags: Set<String> = emptySet(),
    val minTension: Int = 0,
    val minNotoriety: Int = 0,
    val maxGold: Int? = null,
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
        if (!requiredFlags.all { world.worldFlags.contains(it) }) return false
        if (excludedFlags.any { world.worldFlags.contains(it) }) return false
        if (world.regionalTension < minTension) return false
        if (world.notoriety < minNotoriety) return false
        if (maxGold != null && world.gold > maxGold) return false
        return true
    }
}

object EventDeck {

    fun isOriginSupported(origin: OriginClass): Boolean = PEASANT_NODES.any { it.originClass == origin }

    fun selectNode(world: WorldState, kind: NodeKind): EventNode? {
        val allNodes = nodesForOrigin(world.activeOrigin)
        val fresh = allNodes.filter { it.kind == kind && it.id !in world.visitedNodeIds && it.canTrigger(world) }
        if (fresh.isNotEmpty()) return fresh.random()

        // Pool exhausted for this kind - allow repeats rather than falling back to a worse system.
        val repeatable = allNodes.filter { it.kind == kind && it.canTrigger(world) }
        return repeatable.randomOrNull()
    }

    fun findNode(id: String): EventNode? = ALL_NODES.find { it.id == id }

    fun findChoice(nodeId: String, choiceId: Int): Pair<EventNode, EventChoice>? {
        val node = findNode(nodeId) ?: return null
        val choice = node.choices.find { it.id == choiceId } ?: return null
        return node to choice
    }

    /** Builds the resolution-only response for a chosen deck card (no next-scenario content). */
    fun buildChoiceResolution(nodeId: String, choiceId: Int, lang: AppLanguage): EventResponse {
        val (node, choice) = findChoice(nodeId, choiceId)
            ?: return EventResponse(resolutionText = "", bridgeText = "")
        val c = choice.consequence
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

    // ---------------------------------------------------------------------
    // PEASANT CHAPTER 1 DECK
    // ---------------------------------------------------------------------
    private val PEASANT_NODES: List<EventNode> = listOf(

        // --- OPENING (Turn 1) ---
        EventNode(
            id = "p_open_tax",
            kind = NodeKind.OPENING,
            originClass = OriginClass.PEASANT,
            titleEn = "The Winter Road Tax",
            titleSk = "Zimná Cestná Daň",
            textEn = "Bailiff Peter has arrived with armed men, demanding an exorbitant winter road tax. If unpaid, he threatens dungeon time. You have only a few copper coins and a hidden dagger.",
            textSk = "Panský dráb Peter prišiel s ozbrojencami a žiada nehoráznu zimnú cestnú daň. Ak nezaplatíte, hrozí žalár. Máte len pár medených mincí a skrytú dýku.",
            location = "Village",
            npcName = "Bailiff Peter",
            npcTitle = "Crown Tax Collector",
            npcArchetype = "NOBLE",
            choices = listOf(
                EventChoice(
                    id = 1, textEn = "Offer grain stores as partial payment", textSk = "Ponúknuť zásoby obilia ako platbu",
                    tagEn = "Grain Bribe", tagSk = "Úplatok Obilím", cardArchetype = "Peasant_Action",
                    nextNodeId = "p_sand_herbalist_debt",
                    consequence = ChoiceConsequence(
                        goldChange = -5, regionalTensionChange = 5,
                        factionChanges = mapOf(Faction.NOBILITY to 5, Faction.PEASANTS to -5),
                        addFlags = setOf("PAID_WINTER_TAX"),
                        resolutionTextEn = "Peter grunts, weighs the sack of grain, and marks your name paid in his ledger.",
                        resolutionTextSk = "Peter zabručí, odváži vrece obilia a poznačí vaše meno ako vyrovnané.",
                        bridgeTextEn = "The tax wagon rolls on to the next cottage as frost settles over the fields.",
                        bridgeTextSk = "Daňový voz sa presúva k ďalšej chalupe, kým mráz zasypáva polia."
                    )
                ),
                EventChoice(
                    id = 2, textEn = "Plead for mercy before the elder", textSk = "Prosiť rychtára o zľutovanie",
                    tagEn = "Humble Plea", tagSk = "Pokorná Prosba", cardArchetype = "Peasant_Action",
                    nextNodeId = "p_sand_village_feast",
                    consequence = ChoiceConsequence(
                        regionalTensionChange = 8, notorietyChange = -2,
                        factionChanges = mapOf(Faction.PEASANTS to 10, Faction.NOBILITY to -8),
                        addFlags = setOf("VILLAGE_SOLIDARITY"),
                        resolutionTextEn = "The village elder steps in, and Peter reluctantly grants a fortnight's grace.",
                        resolutionTextSk = "Dedinský rychtár zasiahne a Peter neochotne udelí dvojtýždňovú odklad.",
                        bridgeTextEn = "Word spreads that the elder stood against the crown's collector.",
                        bridgeTextSk = "Chýr sa šíri, že rychtár sa postavil kráľovskému vyberačovi."
                    )
                ),
                EventChoice(
                    id = 3, textEn = "Draw the hidden dagger", textSk = "Vytiahnuť skrytú dýku",
                    tagEn = "Combat", tagSk = "Boj", cardArchetype = "Peasant_Action",
                    nextNodeId = "p_sand_bailiff_manhunt",
                    consequence = ChoiceConsequence(
                        healthChange = -15, regionalTensionChange = 20, notorietyChange = 20,
                        factionChanges = mapOf(Faction.PEASANTS to 5, Faction.NOBILITY to -20, Faction.UNDERWORLD to 10),
                        addFlags = setOf("KILLED_BAILIFF"),
                        resolutionTextEn = "Steel meets flesh. Peter falls into the frozen mud, and his men scatter in shock.",
                        resolutionTextSk = "Oceľ sa zaryje do tela. Peter padá do zamrznutého blata a jeho muži sa v šoku rozutekajú.",
                        bridgeTextEn = "You flee into the treeline as alarm bells begin to toll across the valley.",
                        bridgeTextSk = "Utekáte do lesa, kým sa údolím rozozvučia poplašné zvony."
                    )
                )
            )
        ),
        EventNode(
            id = "p_open_fever",
            kind = NodeKind.OPENING,
            originClass = OriginClass.PEASANT,
            titleEn = "The Winter Fever",
            titleSk = "Zimná Horúčka",
            textEn = "A family member is severely ill with winter fever. The village herbalist demands an extortionate fee in gold or a dangerous errand into the blighted woods.",
            textSk = "Člen rodiny je ťažko chorý na zimnú horúčku. Dedinská korenárka žiada nehoráznu sumu v zlate alebo nebezpečnú výpravu do morom zasiahnutých lesov.",
            location = "Village",
            npcName = "Herbalist Martha",
            npcTitle = "Village Healer",
            npcArchetype = "ALCHEMIST",
            choices = listOf(
                EventChoice(
                    id = 1, textEn = "Hand over your last gold coins", textSk = "Odovzdať posledné zlatky",
                    tagEn = "Heavy Purse", tagSk = "Ťažká Mošna", cardArchetype = "Peasant_Action",
                    nextNodeId = "p_sand_church_tithe",
                    consequence = ChoiceConsequence(
                        goldChange = -12, healthChange = 5,
                        factionChanges = mapOf(Faction.PEASANTS to 5),
                        addFlags = setOf("SAVED_SICK_CHILD"),
                        resolutionTextEn = "Martha counts the coins twice, then presses a bitter root tonic into your hands.",
                        resolutionTextSk = "Martha dvakrát prepočíta mince a vtlačí vám do rúk horký koreňový odvar.",
                        bridgeTextEn = "The fever breaks by morning, and your kin sleeps soundly for the first time in days.",
                        bridgeTextSk = "Horúčka do rána opadne a váš príbuzný prvýkrát po dňoch pokojne spí."
                    )
                ),
                EventChoice(
                    id = 2, textEn = "Pledge labor gathering rare moss", textSk = "Sľúbiť prácu pri zbere machu",
                    tagEn = "Labor", tagSk = "Práca", cardArchetype = "Peasant_Action",
                    nextNodeId = "p_sand_starving_pack",
                    consequence = ChoiceConsequence(
                        healthChange = -8, regionalTensionChange = 5,
                        factionChanges = mapOf(Faction.PEASANTS to 5),
                        addFlags = setOf("HERBAL_ALLIANCE"),
                        resolutionTextEn = "You trudge into the blighted treeline, nails black with frost and moss.",
                        resolutionTextSk = "Vlečiete sa do zamoreného lesa, nechty čierne od mrazu a machu.",
                        bridgeTextEn = "Martha nods approvingly and owes you a future favor.",
                        bridgeTextSk = "Martha súhlasne prikývne a dlhuje vám budúcu láskavosť."
                    )
                ),
                EventChoice(
                    id = 3, textEn = "Beg the cathedral priest for healing", textSk = "Prosiť kňaza o sväté uzdravenie",
                    tagEn = "Humble Plea", tagSk = "Pokorná Prosba", cardArchetype = "Church_Action",
                    nextNodeId = "p_sand_wandering_cleric",
                    consequence = ChoiceConsequence(
                        regionalTensionChange = 3,
                        factionChanges = mapOf(Faction.CHURCH to 10, Faction.PEASANTS to 3),
                        addFlags = setOf("CHURCH_DEBT"),
                        resolutionTextEn = "The priest anoints your kin with oil, murmuring a rite older than the parish itself.",
                        resolutionTextSk = "Kňaz pomaže vášho príbuzného olejom a šepká obrad starší ako samotná farnosť.",
                        bridgeTextEn = "The Church notes the favor granted - and expects tithe in return one day.",
                        bridgeTextSk = "Cirkev si poznačí udelenú láskavosť - a jedného dňa očakáva desiatok späť."
                    )
                )
            )
        ),

        // --- SANDBOX (chained follow-ups + standalone pool) ---
        EventNode(
            id = "p_sand_herbalist_debt",
            kind = NodeKind.SANDBOX,
            originClass = OriginClass.PEASANT,
            requiredFlags = setOf("PAID_WINTER_TAX"),
            titleEn = "The Herbalist's Debt",
            titleSk = "Korenárkin Dlh",
            textEn = "With coin scarce, Herbalist Martha offers dried fever-root on credit - but her prices climb each week the debt goes unpaid.",
            textSk = "Keďže mincí je málo, korenárka Martha ponúka sušený koreň na úver - jej ceny však stúpajú s každým týždňom nesplateného dlhu.",
            location = "Village",
            npcName = "Herbalist Martha",
            npcTitle = "Village Healer",
            npcArchetype = "ALCHEMIST",
            choices = listOf(
                EventChoice(
                    id = 1, textEn = "Accept the debt for now", textSk = "Prijať dlh na teraz",
                    tagEn = "Debt", tagSk = "Dlh", cardArchetype = "Peasant_Action",
                    consequence = ChoiceConsequence(
                        goldChange = 5, addFlags = setOf("HERBALIST_DEBT"),
                        factionChanges = mapOf(Faction.PEASANTS to 3),
                        resolutionTextEn = "Martha scratches a tally mark into a warped wooden board bearing your name.",
                        resolutionTextSk = "Martha vyryje čiarku do skrivenej dosky s vaším menom.",
                        bridgeTextEn = "You leave with the root, and a debt that will not be forgotten.",
                        bridgeTextSk = "Odchádzate s koreňom a dlhom, na ktorý sa nezabudne."
                    )
                ),
                EventChoice(
                    id = 2, textEn = "Refuse and forage yourself", textSk = "Odmietnuť a hľadať sám",
                    tagEn = "Self-reliance", tagSk = "Sebestačnosť", cardArchetype = "Peasant_Action",
                    consequence = ChoiceConsequence(
                        healthChange = -5, factionChanges = mapOf(Faction.PEASANTS to -3),
                        resolutionTextEn = "Martha shrugs and turns back to her mortar and pestle without another word.",
                        resolutionTextSk = "Martha pokrčí plecami a mlčky sa vráti k mažiaru.",
                        bridgeTextEn = "You spend a cold night combing the frostbitten hedgerows alone.",
                        bridgeTextSk = "Strávite chladnú noc sami prehľadávaním zamrznutých medzí."
                    )
                ),
                EventChoice(
                    id = 3, textEn = "Offer to run an errand instead", textSk = "Ponúknuť pochôdzku namiesto platby",
                    tagEn = "Errand", tagSk = "Pochôdzka", cardArchetype = "Peasant_Action",
                    consequence = ChoiceConsequence(
                        regionalTensionChange = 3, factionChanges = mapOf(Faction.PEASANTS to 5, Faction.GUILDS to 3),
                        addFlags = setOf("HERBAL_ALLIANCE"),
                        resolutionTextEn = "Martha's eyes narrow, then soften - an errand is worth more to her than coin.",
                        resolutionTextSk = "Martha prižmúri oči, potom zmäkne - pochôdzka jej je cennejšia než minca.",
                        bridgeTextEn = "She hands you a sealed letter and a knowing look.",
                        bridgeTextSk = "Podá vám zapečatený list a chápavý pohľad."
                    )
                )
            )
        ),
        EventNode(
            id = "p_sand_bailiff_manhunt",
            kind = NodeKind.SANDBOX,
            originClass = OriginClass.PEASANT,
            requiredFlags = setOf("KILLED_BAILIFF"),
            titleEn = "The Manhunt",
            titleSk = "Naháňačka",
            textEn = "Crown riders sweep the valley searching for Peter's killer. A woodcutter eyes you suspiciously by the crossroads shrine.",
            textSk = "Kráľovskí jazdci prehľadávajú údolie a hľadajú Petrovho vraha. Drevorubač si vás podozrievavo obzerá pri kaplnke na križovatke.",
            location = "Forest",
            npcName = "Woodcutter Osric",
            npcTitle = "Crossroads Witness",
            npcArchetype = "PEASANT",
            choices = listOf(
                EventChoice(
                    id = 1, textEn = "Bribe him into silence", textSk = "Podplatiť ho za mlčanie",
                    tagEn = "Silver", tagSk = "Striebro", cardArchetype = "Merchant_Action",
                    consequence = ChoiceConsequence(
                        goldChange = -10, notorietyChange = -10,
                        factionChanges = mapOf(Faction.UNDERWORLD to 5),
                        resolutionTextEn = "Osric pockets the coin and suddenly recalls seeing nothing at all.",
                        resolutionTextSk = "Osric si schová mincu a zrazu si nič nepamätá.",
                        bridgeTextEn = "The riders pass through empty-handed by dusk.",
                        bridgeTextSk = "Jazdci do súmraku odchádzajú s prázdnymi rukami."
                    )
                ),
                EventChoice(
                    id = 2, textEn = "Threaten him into silence", textSk = "Vyhrážkami ho umlčať",
                    tagEn = "Threat", tagSk = "Vyhrážka", cardArchetype = "Underworld_Action",
                    consequence = ChoiceConsequence(
                        notorietyChange = 10, regionalTensionChange = 10,
                        factionChanges = mapOf(Faction.PEASANTS to -10, Faction.UNDERWORLD to 10),
                        resolutionTextEn = "Osric pales and stumbles back, nodding frantically.",
                        resolutionTextSk = "Osric zbledne, cúvne a zúrivo prikyvuje.",
                        bridgeTextEn = "Fear buys silence - for now. Osric will not soon forget your eyes.",
                        bridgeTextSk = "Strach kúpi mlčanie - zatiaľ. Osric na váš pohľad tak skoro nezabudne."
                    )
                ),
                EventChoice(
                    id = 3, textEn = "Flee deeper into the woods", textSk = "Ujsť hlbšie do lesa",
                    tagEn = "Escape", tagSk = "Útek", cardArchetype = "Peasant_Action",
                    consequence = ChoiceConsequence(
                        healthChange = -5, notorietyChange = 5,
                        factionChanges = mapOf(Faction.UNDERWORLD to 8),
                        resolutionTextEn = "Branches tear at your cloak as you vanish into the undergrowth.",
                        resolutionTextSk = "Konáre trhajú váš plášť, kým miznete v podraste.",
                        bridgeTextEn = "Osric watches you disappear, his suspicion unresolved.",
                        bridgeTextSk = "Osric sleduje, ako miznete, jeho podozrenie zostáva nevyriešené."
                    )
                )
            )
        ),

        // Standalone sandbox pool (no explicit prerequisite - general variety cards)
        EventNode(
            id = "p_sand_village_feast",
            kind = NodeKind.SANDBOX,
            originClass = OriginClass.PEASANT,
            titleEn = "The Village Feast",
            titleSk = "Dedinská Hostina",
            textEn = "A modest harvest feast lifts the village's spirits. A drunk merchant's purse hangs loose from his belt.",
            textSk = "Skromná dožinková hostina zdvíha náladu dediny. Opitému kupcovi voľne visí mešec z opasku.",
            location = "Village",
            npcName = "Merchant Aldric",
            npcTitle = "Traveling Trader",
            npcArchetype = "MERCHANT",
            choices = listOf(
                EventChoice(
                    id = 1, textEn = "Lift the purse quietly", textSk = "Potichu ukradnúť mešec",
                    tagEn = "Theft", tagSk = "Krádež", cardArchetype = "Underworld_Action",
                    consequence = ChoiceConsequence(
                        goldChange = 18, notorietyChange = 8,
                        factionChanges = mapOf(Faction.UNDERWORLD to 8),
                        addFlags = setOf("PICKPOCKET"),
                        resolutionTextEn = "The purse slips free without a sound. Aldric keeps dancing, none the wiser.",
                        resolutionTextSk = "Mešec zmizne bez zvuku. Aldric tancuje ďalej, netušiac nič.",
                        bridgeTextEn = "You melt back into the crowd, coin warm in your palm.",
                        bridgeTextSk = "Vmiešate sa späť do davu, minca vám hreje v dlani."
                    )
                ),
                EventChoice(
                    id = 2, textEn = "Warn him and earn his favor", textSk = "Varovať ho a získať jeho priazeň",
                    tagEn = "Honesty", tagSk = "Čestnosť", cardArchetype = "Peasant_Action",
                    consequence = ChoiceConsequence(
                        goldChange = 5, factionChanges = mapOf(Faction.PEASANTS to 5, Faction.GUILDS to 8),
                        addFlags = setOf("GUILD_CONTACT"),
                        resolutionTextEn = "Aldric startles, then presses a coin into your hand in gratitude.",
                        resolutionTextSk = "Aldric sa zľakne, potom vám vďačne vtlačí mincu do ruky.",
                        bridgeTextEn = "He mentions he trades often through the village and won't forget a kindness.",
                        bridgeTextSk = "Spomenie, že cez dedinu obchoduje často a na láskavosť nezabudne."
                    )
                ),
                EventChoice(
                    id = 3, textEn = "Join the dancing and let it be", textSk = "Pridať sa k tancu a nechať to tak",
                    tagEn = "Revelry", tagSk = "Zábava", cardArchetype = "Peasant_Action",
                    consequence = ChoiceConsequence(
                        healthChange = 5, factionChanges = mapOf(Faction.PEASANTS to 8),
                        resolutionTextEn = "For one night, the weight of the season lifts from your shoulders.",
                        resolutionTextSk = "Na jednu noc spadne ťarcha ročného obdobia z vašich pliec.",
                        bridgeTextEn = "Laughter echoes through the square long after the fires die down.",
                        bridgeTextSk = "Smiech znie námestím ešte dlho po tom, čo ohne dohoria."
                    )
                )
            )
        ),
        EventNode(
            id = "p_sand_starving_pack",
            kind = NodeKind.SANDBOX,
            originClass = OriginClass.PEASANT,
            titleEn = "The Starving Pack",
            titleSk = "Hladujúca Svorka",
            textEn = "Wolves gaunt from the harsh winter circle the livestock pen at dusk, growing bolder by the hour.",
            textSk = "Vlky vychudnuté krutou zimou obkolesujú za súmraku ohradu s dobytkom, s každou hodinou smelšie.",
            location = "Forest",
            npcName = "Old Gerta",
            npcTitle = "Livestock Keeper",
            npcArchetype = "PEASANT",
            choices = listOf(
                EventChoice(
                    id = 1, textEn = "Stand watch with a torch and spear", textSk = "Strážiť s fakľou a kopijou",
                    tagEn = "Vigil", tagSk = "Stráženie", cardArchetype = "Peasant_Action",
                    consequence = ChoiceConsequence(
                        healthChange = -10, factionChanges = mapOf(Faction.PEASANTS to 10),
                        resolutionTextEn = "You drive the pack back into the treeline, exhausted but victorious.",
                        resolutionTextSk = "Zaženiete svorku späť do lesa, vyčerpaní no víťazní.",
                        bridgeTextEn = "Gerta thanks you with a portion of the saved herd's milk.",
                        bridgeTextSk = "Gerta vám poďakuje časťou mlieka zo zachráneného stáda."
                    )
                ),
                EventChoice(
                    id = 2, textEn = "Sacrifice the weakest goat to the pack", textSk = "Obetovať svorke najslabšiu kozu",
                    tagEn = "Sacrifice", tagSk = "Obeta", cardArchetype = "Peasant_Action",
                    consequence = ChoiceConsequence(
                        goldChange = -5, factionChanges = mapOf(Faction.PEASANTS to -3),
                        resolutionTextEn = "The wolves take the offering and vanish, sated, into the dark.",
                        resolutionTextSk = "Vlky si vezmú obeť a sýte zmiznú v tme.",
                        bridgeTextEn = "Gerta says nothing, but her eyes linger on the empty stall.",
                        bridgeTextSk = "Gerta nič nepovie, no jej pohľad utkvie na prázdnom stánku."
                    )
                ),
                EventChoice(
                    id = 3, textEn = "Abandon the pen and flee indoors", textSk = "Opustiť ohradu a utiecť dnu",
                    tagEn = "Retreat", tagSk = "Ústup", cardArchetype = "Peasant_Action",
                    consequence = ChoiceConsequence(
                        goldChange = -8, factionChanges = mapOf(Faction.PEASANTS to -8),
                        resolutionTextEn = "By morning, the pen lies empty and torn apart.",
                        resolutionTextSk = "Do rána je ohrada prázdna a roztrhaná.",
                        bridgeTextEn = "Gerta weeps quietly over the wreckage of a season's work.",
                        bridgeTextSk = "Gerta ticho plače nad troskami celosezónnej práce."
                    )
                )
            )
        ),
        EventNode(
            id = "p_sand_church_tithe",
            kind = NodeKind.SANDBOX,
            originClass = OriginClass.PEASANT,
            titleEn = "The Church Tithe",
            titleSk = "Cirkevný Desiatok",
            textEn = "A bishop's clerk arrives demanding grain quotas for the cathedral's restoration, ledger already half-filled with village names.",
            textSk = "Biskupov pisár prichádza žiadať zásoby obilia na obnovu katedrály, jeho kniha je už napoly popísaná menami z dediny.",
            location = "Cathedral",
            npcName = "Clerk Wendell",
            npcTitle = "Bishop's Tithe Collector",
            npcArchetype = "PEASANT",
            choices = listOf(
                EventChoice(
                    id = 1, textEn = "Pay the tithe in full", textSk = "Zaplatiť desiatok v plnej výške",
                    tagEn = "Tithe", tagSk = "Desiatok", cardArchetype = "Church_Action",
                    consequence = ChoiceConsequence(
                        goldChange = -10, factionChanges = mapOf(Faction.CHURCH to 12),
                        resolutionTextEn = "Wendell marks your household as pious and generous in his ledger.",
                        resolutionTextSk = "Wendell si vás poznačí ako zbožnú a štedrú domácnosť.",
                        bridgeTextEn = "The cathedral bells ring a touch warmer that evening.",
                        bridgeTextSk = "Katedrálne zvony znejú ten večer o čosi teplejšie."
                    )
                ),
                EventChoice(
                    id = 2, textEn = "Argue the quota is unjust", textSk = "Namietať, že kvóta je nespravodlivá",
                    tagEn = "Protest", tagSk = "Protest", cardArchetype = "Peasant_Action",
                    consequence = ChoiceConsequence(
                        regionalTensionChange = 8, factionChanges = mapOf(Faction.CHURCH to -8, Faction.PEASANTS to 8),
                        resolutionTextEn = "Wendell's quill hovers, then he lowers the demand - grudgingly.",
                        resolutionTextSk = "Wendellovo brko sa zastaví, potom neochotne zníži požiadavku.",
                        bridgeTextEn = "Other villagers nod approvingly at your defiance.",
                        bridgeTextSk = "Ostatní dedinčania súhlasne prikyvujú vašej neústupčivosti."
                    )
                ),
                EventChoice(
                    id = 3, textEn = "Hide half the grain stores", textSk = "Ukryť polovicu zásob obilia",
                    tagEn = "Deception", tagSk = "Klamstvo", cardArchetype = "Underworld_Action",
                    consequence = ChoiceConsequence(
                        goldChange = 8, notorietyChange = 5,
                        factionChanges = mapOf(Faction.CHURCH to -5),
                        addFlags = setOf("CHURCH_DEBT"),
                        resolutionTextEn = "Wendell counts the visible sacks and departs none the wiser.",
                        resolutionTextSk = "Wendell spočíta viditeľné vrecia a odíde bez podozrenia.",
                        bridgeTextEn = "The hidden grain will feed your family long past the thaw.",
                        bridgeTextSk = "Skryté obilie uživí vašu rodinu dlho po roztopení snehu."
                    )
                )
            )
        ),
        EventNode(
            id = "p_sand_wandering_cleric",
            kind = NodeKind.SANDBOX,
            originClass = OriginClass.PEASANT,
            titleEn = "The Wandering Cleric",
            titleSk = "Blúdiaci Mních",
            textEn = "A lost monk seeks shelter for the night, muttering fearfully about heresy hunters combing the eastern roads.",
            textSk = "Stratený mních hľadá na noc útočisko a strachom šepká o lovcoch kacírov prehľadávajúcich východné cesty.",
            location = "Village",
            npcName = "Brother Anselm",
            npcTitle = "Wandering Monk",
            npcArchetype = "ELDER",
            choices = listOf(
                EventChoice(
                    id = 1, textEn = "Shelter him for the night", textSk = "Poskytnúť mu na noc útočisko",
                    tagEn = "Shelter", tagSk = "Útočisko", cardArchetype = "Church_Action",
                    consequence = ChoiceConsequence(
                        factionChanges = mapOf(Faction.CHURCH to 10, Faction.NOBILITY to -5),
                        addFlags = setOf("HOLY_CRUSADER"),
                        resolutionTextEn = "Anselm blesses your hearth before dawn, whispering thanks and prophecy alike.",
                        resolutionTextSk = "Anselm pred úsvitom požehná váš krb a šepká vďaku aj proroctvo.",
                        bridgeTextEn = "He vanishes east before sunrise, leaving only a carved wooden cross.",
                        bridgeTextSk = "Zmizne na východ pred svitaním a zanechá len vyrezávaný drevený kríž."
                    )
                ),
                EventChoice(
                    id = 2, textEn = "Turn him away, fearing trouble", textSk = "Odmietnuť ho zo strachu z problémov",
                    tagEn = "Refusal", tagSk = "Odmietnutie", cardArchetype = "Peasant_Action",
                    consequence = ChoiceConsequence(
                        factionChanges = mapOf(Faction.CHURCH to -8),
                        resolutionTextEn = "Anselm nods sadly and trudges back into the freezing dark.",
                        resolutionTextSk = "Anselm smutne prikývne a vlečie sa späť do mrazivej tmy.",
                        bridgeTextEn = "You lie awake wondering what fate found him on the road.",
                        bridgeTextSk = "Ležíte hore a premýšľate, aký osud ho na ceste zastihol."
                    )
                ),
                EventChoice(
                    id = 3, textEn = "Report him to the parish for coin", textSk = "Nahlásiť ho farnosti za odmenu",
                    tagEn = "Betrayal", tagSk = "Zrada", cardArchetype = "Underworld_Action",
                    consequence = ChoiceConsequence(
                        goldChange = 15, notorietyChange = 8,
                        factionChanges = mapOf(Faction.CHURCH to -15, Faction.PEASANTS to -5),
                        resolutionTextEn = "Riders drag Anselm away at first light as you count your silver.",
                        resolutionTextSk = "Jazdci Anselma za úsvitu odvedú, kým si počítate striebro.",
                        bridgeTextEn = "The coin feels heavier than it should in your pocket.",
                        bridgeTextSk = "Minca vo vrecku vám pripadá ťažšia, než by mala."
                    )
                )
            )
        ),
        EventNode(
            id = "p_sand_ancient_chest",
            kind = NodeKind.SANDBOX,
            originClass = OriginClass.PEASANT,
            titleEn = "The Ancient Chest",
            titleSk = "Prastará Truhlica",
            textEn = "Your plow strikes an ironbound chest half-buried in the frozen field, its hinges rusted shut for what looks like decades.",
            textSk = "Váš pluh narazí na okovanú truhlicu zapadnutú v zamrznutom poli, jej pánty sú zhrdzavené celé desaťročia.",
            location = "Village",
            npcName = "Yourself",
            npcTitle = "",
            npcArchetype = "PEASANT",
            choices = listOf(
                EventChoice(
                    id = 1, textEn = "Force it open yourself", textSk = "Sami ju násilím otvoriť",
                    tagEn = "Curiosity", tagSk = "Zvedavosť", cardArchetype = "Peasant_Action",
                    consequence = ChoiceConsequence(
                        goldChange = 25, healthChange = -5,
                        resolutionTextEn = "Rusted iron gives way to reveal tarnished coin and a cracked signet ring.",
                        resolutionTextSk = "Zhrdzavené železo povolí a odhalí sčernené mince a prasknutý pečatný prsteň.",
                        bridgeTextEn = "You bury the empty chest again before curious eyes can find it.",
                        bridgeTextSk = "Prázdnu truhlicu znovu zahrabete, skôr než ju nájdu zvedavé oči."
                    )
                ),
                EventChoice(
                    id = 2, textEn = "Report the find to the Lord", textSk = "Nahlásiť nález pánovi",
                    tagEn = "Duty", tagSk = "Povinnosť", cardArchetype = "Noble_Action",
                    consequence = ChoiceConsequence(
                        goldChange = 5, factionChanges = mapOf(Faction.NOBILITY to 15),
                        addFlags = setOf("CROWN_FAVOR"),
                        resolutionTextEn = "The steward rewards your honesty with a modest handful of silver.",
                        resolutionTextSk = "Správca odmení vašu čestnosť skromnou hrsťou striebra.",
                        bridgeTextEn = "Your name is noted favorably in the manor's rolls.",
                        bridgeTextSk = "Vaše meno je priaznivo zapísané v panských zoznamoch."
                    )
                ),
                EventChoice(
                    id = 3, textEn = "Sell it unopened to a passing trader", textSk = "Predať ju neotvorenú okoloidúcemu kupcovi",
                    tagEn = "Trade", tagSk = "Obchod", cardArchetype = "Merchant_Action",
                    consequence = ChoiceConsequence(
                        goldChange = 12, factionChanges = mapOf(Faction.GUILDS to 8),
                        resolutionTextEn = "The trader's eyes gleam as he hauls the mysterious chest onto his cart.",
                        resolutionTextSk = "Kupcovi zažiaria oči, keď záhadnú truhlicu naloží na svoj voz.",
                        bridgeTextEn = "Whatever secrets it held are no longer your burden to carry.",
                        bridgeTextSk = "Akékoľvek tajomstvá ukrývala, už nie sú vaším bremenom."
                    )
                )
            )
        ),

        // --- ANCHOR NODES (Turns 5 / 10 / 15 / 20 / 25) ---
        EventNode(
            id = "p_anchor_conscription_drive",
            kind = NodeKind.ANCHOR,
            originClass = OriginClass.PEASANT,
            titleEn = "The Conscription Drive",
            titleSk = "Odvodová Kampaň",
            textEn = "Crown recruiters ride into the village square, reading names from a scroll and eyeing every able-bodied villager.",
            textSk = "Kráľovskí verbovači vchádzajú na dedinské námestie, čítajú mená zo zvitku a obzerajú si každého práceschopného dedinčana.",
            location = "Village",
            npcName = "Sergeant Brand",
            npcTitle = "Crown Recruiter",
            npcArchetype = "KNIGHT",
            choices = listOf(
                EventChoice(
                    id = 1, textEn = "Volunteer for a soldier's wage", textSk = "Prihlásiť sa za vojenský žold",
                    tagEn = "Contract", tagSk = "Zmluva", cardArchetype = "Peasant_Action",
                    consequence = ChoiceConsequence(
                        goldChange = 15, healthChange = -5,
                        factionChanges = mapOf(Faction.NOBILITY to 10),
                        addFlags = setOf("CROWN_FAVOR"),
                        resolutionTextEn = "Brand nods and marks you fit for the eastern levy.",
                        resolutionTextSk = "Brand prikývne a poznačí vás ako spôsobilého na východný odvod.",
                        bridgeTextEn = "You drill with the recruits at dawn, coin jingling in your pocket.",
                        bridgeTextSk = "Za úsvitu cvičíte s odvedencami, minca vám cinká vo vrecku."
                    )
                ),
                EventChoice(
                    id = 2, textEn = "Bribe the sergeant to skip your name", textSk = "Podplatiť seržanta, nech vynechá vaše meno",
                    tagEn = "Heavy Purse", tagSk = "Ťažká Mošna", cardArchetype = "Merchant_Action",
                    consequence = ChoiceConsequence(
                        goldChange = -15, factionChanges = mapOf(Faction.NOBILITY to -5),
                        resolutionTextEn = "Brand's quill conveniently skips a line as silver changes hands.",
                        resolutionTextSk = "Brandovo brko pohodlne preskočí riadok, kým si vymieňate striebro.",
                        bridgeTextEn = "You watch your neighbors marched away instead, guilt heavy in your chest.",
                        bridgeTextSk = "Sledujete, ako namiesto vás odvádzajú susedov, vina vám ťaží hruď."
                    )
                ),
                EventChoice(
                    id = 3, textEn = "Slip away before the roll call", textSk = "Zmiznúť pred nástupom",
                    tagEn = "Escape", tagSk = "Útek", cardArchetype = "Underworld_Action",
                    consequence = ChoiceConsequence(
                        notorietyChange = 8, regionalTensionChange = 5,
                        factionChanges = mapOf(Faction.NOBILITY to -8, Faction.UNDERWORLD to 5),
                        resolutionTextEn = "Your name echoes unanswered across the square as you vanish through back alleys.",
                        resolutionTextSk = "Vaše meno sa neodpovedané ozýva námestím, kým miznete zadnými uličkami.",
                        bridgeTextEn = "Sergeant Brand makes a note of the deserter's description.",
                        bridgeTextSk = "Seržant Brand si poznačí popis dezertéra."
                    )
                )
            )
        ),
        EventNode(
            id = "p_anchor_town_fair",
            kind = NodeKind.ANCHOR,
            originClass = OriginClass.PEASANT,
            titleEn = "The Town Fair Permit",
            titleSk = "Povolenie na Jarmok",
            textEn = "A traveling fair sets up at the crossroads, but the Guild demands a steep permit fee from every villager wishing to sell.",
            textSk = "Na križovatke sa usadzuje putovný jarmok, no Cech žiada vysoký poplatok za povolenie od každého dedinčana, ktorý chce predávať.",
            location = "Marketplace",
            npcName = "Guild Steward Hollis",
            npcTitle = "Fair Permit Officer",
            npcArchetype = "MERCHANT",
            choices = listOf(
                EventChoice(
                    id = 1, textEn = "Pay the permit and sell your goods", textSk = "Zaplatiť povolenie a predávať tovar",
                    tagEn = "Contract", tagSk = "Zmluva", cardArchetype = "Merchant_Action",
                    consequence = ChoiceConsequence(
                        goldChange = 20, factionChanges = mapOf(Faction.GUILDS to 12),
                        addFlags = setOf("GUILD_MASTER"),
                        resolutionTextEn = "Hollis stamps your permit and your stall does brisk business by noon.",
                        resolutionTextSk = "Hollis vám opečiatkuje povolenie a váš stánok má do obeda čulý obrat.",
                        bridgeTextEn = "Coin flows steadily into your purse as the fair carries on.",
                        bridgeTextSk = "Kým jarmok pokračuje, minca plynule pribúda do vášho mešca."
                    )
                ),
                EventChoice(
                    id = 2, textEn = "Sell without a permit, risking a fine", textSk = "Predávať bez povolenia, riskovať pokutu",
                    tagEn = "Risk", tagSk = "Riziko", cardArchetype = "Underworld_Action",
                    consequence = ChoiceConsequence(
                        goldChange = 10, notorietyChange = 8,
                        factionChanges = mapOf(Faction.GUILDS to -10),
                        resolutionTextEn = "You sell quietly at the fair's edge, watching for Hollis's enforcers.",
                        resolutionTextSk = "Predávate potichu na okraji jarmoku a sledujete Hollisových výbercov.",
                        bridgeTextEn = "You slip away with coin before the Guild notices the missing stamp.",
                        bridgeTextSk = "Zmiznete s mincami skôr, než si Cech všimne chýbajúcu pečiatku."
                    )
                ),
                EventChoice(
                    id = 3, textEn = "Skip the fair entirely", textSk = "Jarmok úplne obísť",
                    tagEn = "Abstain", tagSk = "Zdržanie sa", cardArchetype = "Peasant_Action",
                    consequence = ChoiceConsequence(
                        resolutionTextEn = "You watch from a distance as others haggle over the permit fees.",
                        resolutionTextSk = "Z diaľky sledujete, ako sa ostatní dohadujú o poplatkoch za povolenie.",
                        bridgeTextEn = "The fair packs up by evening, and life returns to its usual rhythm.",
                        bridgeTextSk = "Jarmok sa do večera zbalí a život sa vráti do zvyčajného rytmu."
                    )
                )
            )
        ),
        EventNode(
            id = "p_anchor_witch_trial",
            kind = NodeKind.ANCHOR,
            originClass = OriginClass.PEASANT,
            minTension = 30,
            titleEn = "The Witch Trial",
            titleSk = "Súd nad Bosorkou",
            textEn = "Villagers accuse Herbalist Martha of cursing the wells. A mob gathers with torches outside her cottage, demanding a verdict.",
            textSk = "Dedinčania obviňujú korenárku Marthu z prekliatia studní. Pred jej chalupou sa zhromažďuje dav s fakľami a žiada rozsudok.",
            location = "Village",
            npcName = "Mob Leader Cobb",
            npcTitle = "Village Firebrand",
            npcArchetype = "PEASANT",
            choices = listOf(
                EventChoice(
                    id = 1, textEn = "Defend Martha before the mob", textSk = "Brániť Marthu pred davom",
                    tagEn = "Defense", tagSk = "Obhajoba", cardArchetype = "Church_Action",
                    consequence = ChoiceConsequence(
                        regionalTensionChange = -10, notorietyChange = 5,
                        factionChanges = mapOf(Faction.PEASANTS to -5, Faction.CHURCH to 8),
                        addFlags = setOf("SAVED_HERBALIST"),
                        resolutionTextEn = "Your words cut through the torchlight, and the mob's fury falters.",
                        resolutionTextSk = "Vaše slová preniknú svetlom fakieľ a zúrivosť davu poľaví.",
                        bridgeTextEn = "Cobb spits at your feet but calls his men back into the dark.",
                        bridgeTextSk = "Cobb vám pľuje pod nohy, no zavolá svojich mužov späť do tmy."
                    )
                ),
                EventChoice(
                    id = 2, textEn = "Join the accusation for standing", textSk = "Pridať sa k obvineniu kvôli postaveniu",
                    tagEn = "Betrayal", tagSk = "Zrada", cardArchetype = "Underworld_Action",
                    consequence = ChoiceConsequence(
                        regionalTensionChange = 15, notorietyChange = 10,
                        factionChanges = mapOf(Faction.PEASANTS to 8, Faction.CHURCH to -10),
                        addFlags = setOf("VILLAGE_HATRED"),
                        resolutionTextEn = "The mob roars approval as torches close in around Martha's door.",
                        resolutionTextSk = "Dav zareve súhlas, kým sa fakle zvierajú okolo Marthiných dverí.",
                        bridgeTextEn = "Smoke rises from the cottage by midnight - a weight you will carry.",
                        bridgeTextSk = "O polnoci sa nad chalupou dvíha dym - bremeno, ktoré si ponesiete."
                    )
                ),
                EventChoice(
                    id = 3, textEn = "Slip away and let fate decide", textSk = "Nenápadne odísť a nechať rozhodnúť osud",
                    tagEn = "Indifference", tagSk = "Ľahostajnosť", cardArchetype = "Peasant_Action",
                    consequence = ChoiceConsequence(
                        regionalTensionChange = 5,
                        resolutionTextEn = "You retreat into the shadows, the mob's shouts fading behind you.",
                        resolutionTextSk = "Stiahnete sa do tieňa, výkriky davu za vami blednú.",
                        bridgeTextEn = "By morning, no one speaks of what happened to the herbalist.",
                        bridgeTextSk = "Do rána nikto nehovorí o tom, čo sa stalo korenárke."
                    )
                )
            )
        ),
        EventNode(
            id = "p_anchor_army_march",
            kind = NodeKind.ANCHOR,
            originClass = OriginClass.PEASANT,
            minTension = 40,
            titleEn = "The Army March",
            titleSk = "Pochod Vojska",
            textEn = "Royal troops march through the valley, foraging farms bare. A young quartermaster eyes your remaining stores.",
            textSk = "Kráľovské vojsko prechádza údolím a vyprázdňuje statky. Mladý proviantný dôstojník si obzerá vaše zvyšné zásoby.",
            location = "Village",
            npcName = "Quartermaster Vance",
            npcTitle = "Royal Foraging Officer",
            npcArchetype = "KNIGHT",
            choices = listOf(
                EventChoice(
                    id = 1, textEn = "Surrender the stores without protest", textSk = "Vzdať sa zásob bez protestu",
                    tagEn = "Compliance", tagSk = "Poslušnosť", cardArchetype = "Noble_Action",
                    consequence = ChoiceConsequence(
                        goldChange = -15, factionChanges = mapOf(Faction.NOBILITY to 10, Faction.PEASANTS to -8),
                        resolutionTextEn = "Vance nods curtly and moves the column onward without incident.",
                        resolutionTextSk = "Vance stroho prikývne a kolóna pokračuje bez incidentu.",
                        bridgeTextEn = "Your larder stands empty as the army vanishes over the ridge.",
                        bridgeTextSk = "Vaša špajza je prázdna, kým vojsko mizne za hrebeňom."
                    )
                ),
                EventChoice(
                    id = 2, textEn = "Hide stores in the root cellar", textSk = "Ukryť zásoby v pivnici",
                    tagEn = "Deception", tagSk = "Klamstvo", cardArchetype = "Underworld_Action",
                    consequence = ChoiceConsequence(
                        notorietyChange = 8, factionChanges = mapOf(Faction.NOBILITY to -8),
                        addFlags = setOf("DESERTER_HELPED"),
                        resolutionTextEn = "Vance's search comes up empty, though his suspicious gaze lingers.",
                        resolutionTextSk = "Vanceovo pátranie je márne, no jeho podozrievavý pohľad na vás dlho utkvie.",
                        bridgeTextEn = "Your family eats well tonight, unlike your neighbors.",
                        bridgeTextSk = "Vaša rodina sa dnes večer dobre naje, na rozdiel od susedov."
                    )
                ),
                EventChoice(
                    id = 3, textEn = "Volunteer for the eastern levy", textSk = "Prihlásiť sa na východný odvod",
                    tagEn = "Enlistment", tagSk = "Odvod", cardArchetype = "Noble_Action",
                    consequence = ChoiceConsequence(
                        goldChange = 20, healthChange = -10,
                        factionChanges = mapOf(Faction.NOBILITY to 15),
                        addFlags = setOf("CROWN_FAVOR"),
                        resolutionTextEn = "Vance claps your shoulder and hands over a soldier's advance pay.",
                        resolutionTextSk = "Vance vám poklepe po pleci a odovzdá vojenský predplatok.",
                        bridgeTextEn = "You march east with the column, uncertain what awaits at the border.",
                        bridgeTextSk = "Pochodujete na východ s kolónou, neistí, čo čaká na hranici."
                    )
                )
            )
        ),
        EventNode(
            id = "p_anchor_special_investigator",
            kind = NodeKind.ANCHOR,
            originClass = OriginClass.PEASANT,
            requiredFlags = setOf("KILLED_BAILIFF"),
            titleEn = "The Special Investigator",
            titleSk = "Osobitný Vyšetrovateľ",
            textEn = "The Lord has sent a grim torturer-investigator to solve Bailiff Peter's death. He questions every household in turn.",
            textSk = "Pán vyslal pochmúrneho vyšetrovateľa-mučiteľa, aby vyriešil smrť drába Petra. Vypočúva každú domácnosť po poradí.",
            location = "Village",
            npcName = "Investigator Kray",
            npcTitle = "Lord's Inquisitor",
            npcArchetype = "NOBLE",
            choices = listOf(
                EventChoice(
                    id = 1, textEn = "Confess and beg for a swift trial", textSk = "Priznať sa a prosiť o rýchly súd",
                    tagEn = "Confession", tagSk = "Priznanie", cardArchetype = "Church_Action",
                    consequence = ChoiceConsequence(
                        notorietyChange = 10, healthChange = -10,
                        factionChanges = mapOf(Faction.NOBILITY to -10, Faction.CHURCH to 5),
                        resolutionTextEn = "Kray's quill scratches your confession into the record without a flicker of surprise.",
                        resolutionTextSk = "Krayovo brko bez prekvapenia zaznamená vaše priznanie do spisu.",
                        bridgeTextEn = "You are marked for judgment - your fate now rests with the crown's mercy.",
                        bridgeTextSk = "Ste označení na súd - váš osud teraz leží v rukách kráľovskej milosti."
                    )
                ),
                EventChoice(
                    id = 2, textEn = "Frame the outlaw gang instead", textSk = "Obviniť namiesto seba zbojnícku bandu",
                    tagEn = "Frame-up", tagSk = "Nastrojenie", cardArchetype = "Underworld_Action",
                    consequence = ChoiceConsequence(
                        notorietyChange = -15, goldChange = -10,
                        factionChanges = mapOf(Faction.UNDERWORLD to -15, Faction.NOBILITY to 5),
                        addFlags = setOf("SHADOW_DEAL"),
                        resolutionTextEn = "Kray studies the planted evidence, then nods slowly, satisfied.",
                        resolutionTextSk = "Kray si prezrie podstrčené dôkazy a pomaly, spokojne prikývne.",
                        bridgeTextEn = "The outlaws will not forgive this betrayal easily.",
                        bridgeTextSk = "Zbojníci vám túto zradu ľahko neodpustia."
                    )
                ),
                EventChoice(
                    id = 3, textEn = "Bribe him to close the case", textSk = "Podplatiť ho, aby prípad uzavrel",
                    tagEn = "Bribe", tagSk = "Úplatok", cardArchetype = "Merchant_Action",
                    consequence = ChoiceConsequence(
                        goldChange = -30, notorietyChange = -10,
                        factionChanges = mapOf(Faction.NOBILITY to -5),
                        resolutionTextEn = "Kray's palm closes around the silver without ever meeting your eyes.",
                        resolutionTextSk = "Krayova dlaň sa zovrie okolo striebra bez toho, aby sa pozrel do vašich očí.",
                        bridgeTextEn = "The investigation quietly stalls, then vanishes from the record.",
                        bridgeTextSk = "Vyšetrovanie potichu uviazne a potom zmizne zo spisov."
                    )
                )
            )
        ),
        EventNode(
            id = "p_anchor_grand_purge",
            kind = NodeKind.ANCHOR,
            originClass = OriginClass.PEASANT,
            minNotoriety = 25,
            titleEn = "The Grand Inquisition Purge",
            titleSk = "Veľká Inkvizičná Čistka",
            textEn = "Inquisitors sweep the parish hunting dark magic and heretical texts. Their gaze lingers too long on your household.",
            textSk = "Inkvizítori prehľadávajú farnosť a hľadajú temnú mágiu a kacírske texty. Ich pohľad sa priveľmi zdržiava na vašej domácnosti.",
            location = "Cathedral",
            npcName = "Inquisitor Roswell",
            npcTitle = "Holy Inquisitor",
            npcArchetype = "BISHOP",
            choices = listOf(
                EventChoice(
                    id = 1, textEn = "Open your home to the search", textSk = "Otvoriť dom prehliadke",
                    tagEn = "Compliance", tagSk = "Poslušnosť", cardArchetype = "Church_Action",
                    consequence = ChoiceConsequence(
                        notorietyChange = -10, factionChanges = mapOf(Faction.CHURCH to 10),
                        resolutionTextEn = "Roswell finds nothing but honest poverty and moves on, disappointed.",
                        resolutionTextSk = "Roswell nenájde nič iné než čestnú chudobu a sklamane pokračuje ďalej.",
                        bridgeTextEn = "Your name is cleared from the inquisitor's ledger.",
                        bridgeTextSk = "Vaše meno je vymazané z inkvizítorovho zoznamu."
                    )
                ),
                EventChoice(
                    id = 2, textEn = "Bar the door and refuse entry", textSk = "Zamknúť dvere a odmietnuť vstup",
                    tagEn = "Defiance", tagSk = "Vzdor", cardArchetype = "Peasant_Action",
                    consequence = ChoiceConsequence(
                        notorietyChange = 15, regionalTensionChange = 10,
                        factionChanges = mapOf(Faction.CHURCH to -15, Faction.PEASANTS to 8),
                        resolutionTextEn = "Roswell's guards splinter the door before dusk falls.",
                        resolutionTextSk = "Roswellova stráž rozštiepi dvere skôr, než padne súmrak.",
                        bridgeTextEn = "Whatever they seek, your defiance has marked you as a suspect.",
                        bridgeTextSk = "Nech hľadajú čokoľvek, váš vzdor vás označil za podozrivého."
                    )
                ),
                EventChoice(
                    id = 3, textEn = "Denounce a neighbor to redirect suspicion", textSk = "Udať suseda a odviesť podozrenie",
                    tagEn = "Betrayal", tagSk = "Zrada", cardArchetype = "Underworld_Action",
                    consequence = ChoiceConsequence(
                        notorietyChange = -20, factionChanges = mapOf(Faction.PEASANTS to -15, Faction.CHURCH to 5),
                        addFlags = setOf("VILLAGE_HATRED"),
                        resolutionTextEn = "Roswell's attention snaps toward your neighbor's cottage instead.",
                        resolutionTextSk = "Roswellova pozornosť sa namiesto toho upriami na susedovu chalupu.",
                        bridgeTextEn = "You are safe tonight, but the village will remember who pointed the finger.",
                        bridgeTextSk = "Dnes večer ste v bezpečí, no dedina si zapamätá, kto ukázal prstom."
                    )
                )
            )
        ),
        EventNode(
            id = "p_anchor_rebellion_leader",
            kind = NodeKind.ANCHOR,
            originClass = OriginClass.PEASANT,
            requiredFlags = setOf("HERO_OF_OAKVALE"),
            titleEn = "The Rebellion Gathers",
            titleSk = "Povstanie sa Zhromažďuje",
            textEn = "The peasantry gathers at your door by torchlight, demanding you lead the assault on the Lord's keep at last.",
            textSk = "Poddaní sa za svetla fakieľ zhromažďujú pri vašich dverách a žiadajú, aby ste konečne viedli útok na pánsku pevnosť.",
            location = "Village",
            npcName = "The Assembled Peasantry",
            npcTitle = "Rebellion",
            npcArchetype = "PEASANT",
            choices = listOf(
                EventChoice(
                    id = 1, textEn = "Lead the assault on the keep", textSk = "Viesť útok na pevnosť",
                    tagEn = "Rebellion", tagSk = "Povstanie", cardArchetype = "Peasant_Action",
                    consequence = ChoiceConsequence(
                        notorietyChange = 20, regionalTensionChange = 25,
                        factionChanges = mapOf(Faction.PEASANTS to 20, Faction.NOBILITY to -25),
                        addFlags = setOf("LED_REBELLION"),
                        resolutionTextEn = "Torches surge forward as one, and the keep's gates groan under the weight of the mob.",
                        resolutionTextSk = "Fakle sa spoločne pohnú vpred a brány pevnosti zastonú pod váhou davu.",
                        bridgeTextEn = "History will remember this night, one way or another.",
                        bridgeTextSk = "Táto noc sa zapíše do dejín, tak či onak."
                    )
                ),
                EventChoice(
                    id = 2, textEn = "Urge caution and delay the uprising", textSk = "Nabádať k opatrnosti a odložiť povstanie",
                    tagEn = "Caution", tagSk = "Opatrnosť", cardArchetype = "Peasant_Action",
                    consequence = ChoiceConsequence(
                        factionChanges = mapOf(Faction.PEASANTS to -10),
                        resolutionTextEn = "The crowd grumbles but disperses, torches sputtering out one by one.",
                        resolutionTextSk = "Dav reptá, no rozíde sa, fakle jedna po druhej dohasínajú.",
                        bridgeTextEn = "Some call you wise. Others call you a coward.",
                        bridgeTextSk = "Niektorí vás nazývajú múdrym. Iní zbabelcom."
                    )
                ),
                EventChoice(
                    id = 3, textEn = "Betray the plan to the Lord for favor", textSk = "Zradiť plán pánovi výmenou za priazeň",
                    tagEn = "Betrayal", tagSk = "Zrada", cardArchetype = "Noble_Action",
                    consequence = ChoiceConsequence(
                        goldChange = 40, factionChanges = mapOf(Faction.NOBILITY to 25, Faction.PEASANTS to -30),
                        addFlags = setOf("CROWN_FAVOR"),
                        resolutionTextEn = "The Lord's men strike before the mob ever reaches the gate.",
                        resolutionTextSk = "Pánovi muži zaútočia skôr, než dav vôbec dosiahne bránu.",
                        bridgeTextEn = "Gold weighs heavy in your purse, heavier still on your conscience.",
                        bridgeTextSk = "Zlato ťaží vaše vrecko, no ešte ťažšie ťaží vaše svedomie."
                    )
                )
            )
        )
    )
}
