package com.example.data

import kotlin.random.Random

class GameRepository(private val geminiApiService: GeminiApiService = GeminiApiService()) {

    suspend fun getNextEvent(
        worldState: WorldState,
        chosenOptionText: String?,
        anchorContext: String? = null
    ): EventResponse {
        val geminiResult = geminiApiService.generateNextEvent(worldState, chosenOptionText, anchorContext)
        if (geminiResult != null) {
            return geminiResult
        }

        return generateOfflineEvent(worldState, chosenOptionText, anchorContext)
    }

    private fun generateOfflineEvent(
        worldState: WorldState,
        chosenOptionText: String?,
        anchorContext: String? = null
    ): EventResponse {
        val isSlovak = worldState.selectedLanguage == AppLanguage.SLOVAK

        if (chosenOptionText == null || worldState.turnCount == 1) {
            val seedText = anchorContext ?: ""
            return when {
                seedText.contains("fever", ignoreCase = true) || seedText.contains("disease", ignoreCase = true) || seedText.contains("horúčka", ignoreCase = true) -> EventResponse(
                    resolutionText = if (isSlovak) "Korenárka sa mračí a pevne zviera vrecúško so sušeným koreňom." else "The herbalist scowls, clutching a pouch of dried fever-root.",
                    bridgeText = if (isSlovak) "Zimný vichor kvíli vonku, zatiaľ čo chorý príbuzný lapá po dychu." else "Winter winds howl outside as your bedridden kin gasps for warmth.",
                    nextEventTitle = if (isSlovak) "Zimná Horúčka" else "The Winter Fever",
                    nextEventText = if (isSlovak) "Člen rodiny je ťažko chorý na zimnú horúčku. Dedinská korenárka Martha žiada nehoráznu sumu v zlate alebo nebezpečnú výpravu do morom zasiahnutých lesov." else "A family member is severely ill with winter fever. The village herbalist demands an extortionate fee in gold or a dangerous errand into the blighted woods.",
                    location = "Village",
                    npcName = if (isSlovak) "Korenárka Martha" else "Herbalist Martha",
                    npcTitle = if (isSlovak) "Dedinská Liečiteľka" else "Village Healer",
                    npcArchetype = "ALCHEMIST",
                    statChanges = StatChanges(goldChange = -15, healthChange = 0, regionalTensionChange = 5, notorietyChange = 0, factionChanges = mapOf("Peasants" to 5)),
                    newWorldFlags = listOf("WINTER_FEVER"),
                    options = if (isSlovak) listOf(
                        EventOption(1, "Odovzdať posledné zlatky za liečivé byliny", "Ťažká Mošna", "Peasant_Action"),
                        EventOption(2, "Sľúbiť prácu pri zbere vzácneho machu v lesoch", "Práca", "Peasant_Action"),
                        EventOption(3, "Prosiť kňaza v katedrále o sväté uzdravenie", "Pokorná Prosba", "Church_Action")
                    ) else listOf(
                        EventOption(1, "Hand over your last gold coins for herbs", "Heavy Purse", "Peasant_Action"),
                        EventOption(2, "Pledge labor gathering rare moss in winter woods", "Labor", "Peasant_Action"),
                        EventOption(3, "Beg the Cathedral priest for holy healing", "Humble Plea", "Church_Action")
                    )
                )

                seedText.contains("poaching", ignoreCase = true) || seedText.contains("deer", ignoreCase = true) || seedText.contains("pytliak", ignoreCase = true) -> EventResponse(
                    resolutionText = if (isSlovak) "Ťažká železná rukavica Sira Guya udrie do snehu vedľa zdochliny." else "Sir Guy's heavy iron gauntlet strikes the snow beside the carcass.",
                    bridgeText = if (isSlovak) "Tieň šibenice sa dlho ťahá cez panské pozemky." else "Gallows shadow stretches long across the manor grounds.",
                    nextEventTitle = if (isSlovak) "Panský Les" else "The Lord's Forest",
                    nextEventText = if (isSlovak) "Chytili vás pri pytliačení hladujúcej srny v panských lesoch. Krutý kráľovský lovec Sir Guy žiada podrobenie alebo okamžitú popravu na šibenici." else "You were caught poaching a starving deer in the Lord's forest by the cruel huntsman Sir Guy, who demands submission or immediate gallows execution.",
                    location = "Forest",
                    npcName = "Sir Guy",
                    npcTitle = if (isSlovak) "Kráľovský Lovec" else "Royal Huntsman",
                    npcArchetype = "KNIGHT",
                    statChanges = StatChanges(goldChange = 0, healthChange = -10, regionalTensionChange = 15, notorietyChange = 10, factionChanges = mapOf("Nobility" to -15, "Peasants" to 10)),
                    newWorldFlags = listOf("CAUGHT_POACHING"),
                    options = if (isSlovak) listOf(
                        EventOption(1, "Vzdať sa jeleniny a pokoriť sa", "Pokorná Prosba", "Peasant_Action"),
                        EventOption(2, "Predstierať nevedomosť a ukázať na zbojníkov", "Klamstvo", "Underworld_Action"),
                        EventOption(3, "Vytiahnuť skrytú dýku proti panskému drábovi", "Boj", "Peasant_Action")
                    ) else listOf(
                        EventOption(1, "Surrender the venison and bow in submission", "Humble Plea", "Peasant_Action"),
                        EventOption(2, "Feign ignorance and point toward shadow bandits", "Bluff", "Underworld_Action"),
                        EventOption(3, "Draw your hidden dagger against the huntsman", "Combat", "Peasant_Action")
                    )
                )

                seedText.contains("draft", ignoreCase = true) || seedText.contains("press-gang", ignoreCase = true) || seedText.contains("verbúnk", ignoreCase = true) -> EventResponse(
                    resolutionText = if (isSlovak) "Kráľovskí drábi tresnú železnými okovami o krčmový stôl." else "Crown sergeants slam iron cuffs onto the tavern tables.",
                    bridgeText = if (isSlovak) "Bubny rytmicky víria pred dedinskou krčmou a kráľovské zástavy sa rozvíjajú." else "Drums beat rhythmically outside the village inn as royal banners unfurl.",
                    nextEventTitle = if (isSlovak) "Kráľovský Verbúnk" else "The Royal Press-Gang",
                    nextEventText = if (isSlovak) "Kráľovskí verbovači prišli s ozbrojenými žoldniermi, aby prinútili všetkých práca schopných mužov do krvavej vojny proti pohraničným nájazdníkom." else "Royal recruiters arrive with armed mercenaries to press-gang all able-bodied men into a bloody eastern war against border raiders.",
                    location = "Tavern",
                    npcName = if (isSlovak) "Seržant Brand" else "Sergeant Brand",
                    npcTitle = if (isSlovak) "Kráľovský Verbovač" else "Crown Recruiter",
                    npcArchetype = "KNIGHT",
                    statChanges = StatChanges(goldChange = 0, healthChange = 0, regionalTensionChange = 20, notorietyChange = 5, factionChanges = mapOf("Nobility" to -10, "Peasants" to 10)),
                    newWorldFlags = listOf("WAR_DRAFT"),
                    options = if (isSlovak) listOf(
                        EventOption(1, "Prihlásiť sa ako táborový robotník za mince", "Zmluva", "Peasant_Action"),
                        EventOption(2, "Podplatiť verbovača striebrom na vymazanie mena", "Ťažká Mošna", "Merchant_Action"),
                        EventOption(3, "Uniknúť cez pivničné dvere do nočnej hmly", "Útek", "Underworld_Action")
                    ) else listOf(
                        EventOption(1, "Volunteer as camp laborer for coin", "Contract", "Peasant_Action"),
                        EventOption(2, "Bribe sergeant with silver to erase your name", "Heavy Purse", "Merchant_Action"),
                        EventOption(3, "Escape through cellar door into night fog", "Escape", "Underworld_Action")
                    )
                )

                worldState.activeOrigin == OriginClass.GUILD_APPRENTICE -> EventResponse(
                    resolutionText = if (isSlovak) "Cechmajster Corvus vás pritlačí ku stene v tkáčskej dielni." else "Guildmaster Corvus corners you in the weaving hall with city watchmen.",
                    bridgeText = if (isSlovak) "Mestskí drábi blokujú východ, zatiaľ čo cechoví úradníci počítajú chýbajúce balíky hodvábu." else "Guards block the workshop exit while trade guild officers tally missing bolts of crimson silk.",
                    nextEventTitle = if (isSlovak) "Stratená Zásielka Hodvábu" else "The Missing Silk Shipment",
                    nextEventText = if (isSlovak) "Zo skladu zmizla drahocenná zásielka hodvábu. Cechmajster Corvus z toho obviňuje vás a mestskí drábi čakajú vonku, aby prehľadali vaše obydlie." else "A shipment of expensive silk went missing from the workshop vaults. Guildmaster Corvus blames you, and city watchmen wait outside to search your quarters.",
                    location = "Marketplace",
                    npcName = "Cechmajster Corvus",
                    npcTitle = if (isSlovak) "Hodvábny Cechmajster" else "Silk Guildmaster",
                    npcArchetype = "MERCHANT",
                    statChanges = StatChanges(
                        goldChange = 0, healthChange = 0, regionalTensionChange = 10, notorietyChange = 5,
                        factionChanges = mapOf("Guilds" to -10, "Peasants" to 5)
                    ),
                    newWorldFlags = listOf("SILK_INVESTIGATION"),
                    options = if (isSlovak) listOf(
                        EventOption(1, "Ponúknuť prehliadku účtovných kníh na nájdenie stôp", "Zmluva", "Merchant_Action"),
                        EventOption(2, "Podplatiť stráže na odloženie prehliadky izby", "Ťažká Mošna", "Merchant_Action"),
                        EventOption(3, "Tvrdiť, že voz prepadla tlupa z podsvetia", "Dohoda v Tieni", "Underworld_Action")
                    ) else listOf(
                        EventOption(1, "Offer to inspect the shipping ledgers for clues", "Contract", "Merchant_Action"),
                        EventOption(2, "Bribe the guards to delay searching your room", "Heavy Purse", "Merchant_Action"),
                        EventOption(3, "Claim a rival underworld gang intercepted the cart", "Shadow Deal", "Underworld_Action")
                    )
                )

                worldState.activeOrigin == OriginClass.ACOLYTE -> EventResponse(
                    resolutionText = if (isSlovak) "Vysoký biskup Alistair vstúpi za vás v prašnom katedrálnom archíve." else "High Bishop Alistair steps behind you in the dusty Cathedral archives.",
                    bridgeText = if (isSlovak) "Sviečky blikajú na pozlátených ikonách, zatiaľ čo klenbami znie priestorový spev." else "Candle light flickers off gilded iconography as solemn chant resounds through stone archways.",
                    nextEventTitle = if (isSlovak) "Kacíarsky Rukopis" else "The Heretical Manuscript",
                    nextEventText = if (isSlovak) "Našli ste zakázaný kacíarsky rukopis ukrytý pod dlážkou katedrálnej knižnice. Vysoký biskup Alistair sa pomaly blíži a pýta sa, aké tajné zvitky držíte." else "You discovered a forbidden, heretical manuscript concealed within the Cathedral library floorboards. High Bishop Alistair approaches slowly, asking what secret scroll you hold.",
                    location = "Cathedral",
                    npcName = "Biskup Alistair",
                    npcTitle = if (isSlovak) "Vysoký Prelát" else "High Prelate",
                    npcArchetype = "BISHOP",
                    statChanges = StatChanges(
                        goldChange = 0, healthChange = 0, regionalTensionChange = 5, notorietyChange = 10,
                        factionChanges = mapOf("Church" to 5, "Nobility" to -5)
                    ),
                    newWorldFlags = listOf("FORBIDDEN_MANUSCRIPT"),
                    options = if (isSlovak) listOf(
                        EventOption(1, "Odovzdať zvitok a priznať sa k svätokrádeži", "Pokorná Prosba", "Church_Action"),
                        EventOption(2, "Ukryť zvitok pod rúcho a hladko klamať", "Klamstvo", "Underworld_Action"),
                        EventOption(3, "Ponúknuť rozlúštenie starobylých rún pre Svätú stolicu", "Sväté Štúdium", "Church_Action")
                    ) else listOf(
                        EventOption(1, "Hand over the scroll and pledge solemn confession", "Humble Plea", "Church_Action"),
                        EventOption(2, "Conceal the scroll under your robes and lie smoothly", "Bluff", "Underworld_Action"),
                        EventOption(3, "Offer to decode the ancient runes for the Holy See", "Holy Study", "Church_Action")
                    )
                )

                else -> EventResponse(
                    resolutionText = if (isSlovak) "Železné čižmy panského drába Petra dupú po zamrznutom blate pred vašou chalupou." else "Bailiff Peter's iron boots slam against the frozen mud outside your cottage.",
                    bridgeText = if (isSlovak) "Dedinčania sa chúlia pri studených ohniskách, zatiaľ čo vyberači počítajú zimné odvedené dávky." else "Villagers huddle near cold hearths as Crown tax collectors tally winter levies.",
                    nextEventTitle = if (isSlovak) "Zimná Cestná Daň" else "The Winter Road Tax",
                    nextEventText = if (isSlovak) "Panský dráb Peter prišiel s ozbrojencami a žiada nehoráznu zimnú cestnú daň. Ak nezaplatíte, hrozí žalár. Máte len pár medených mincí a skrytú dýku." else "Bailiff Peter has arrived with armed men, demanding an exorbitant winter road tax. If unpaid, he threatens dungeon time. You have only a few copper coins and a hidden dagger.",
                    location = "Village",
                    npcName = "Dráb Peter",
                    npcTitle = if (isSlovak) "Kráľovský Vyberač Daní" else "Crown Tax Collector",
                    npcArchetype = "NOBLE",
                    statChanges = StatChanges(
                        goldChange = 0, healthChange = 0, regionalTensionChange = 15, notorietyChange = 5,
                        factionChanges = mapOf("Peasants" to 10, "Nobility" to -10)
                    ),
                    newWorldFlags = listOf("BAILIFF_TAX_DEMAND"),
                    options = if (isSlovak) listOf(
                        EventOption(1, "Ponúknuť zvyšné zásoby obilia ako čiastočnú platbu", "Úplatok Obilím", "Peasant_Action"),
                        EventOption(2, "Prosiť o zmilovanie pod ochranou dedinského rychtára", "Pokorná Prosba", "Peasant_Action"),
                        EventOption(3, "Uchopiť skrytú dýku za kabátcom", "Boj", "Peasant_Action")
                    ) else listOf(
                        EventOption(1, "Offer your remaining grain stores as partial payment", "Grain Bribe", "Peasant_Action"),
                        EventOption(2, "Pleud for mercy under the village elder's protection", "Humble Plea", "Peasant_Action"),
                        EventOption(3, "Grasp the hidden dagger behind your tunic", "Combat", "Peasant_Action")
                    )
                )
            }
        }

        val turn = worldState.turnCount

        // If an active scene is locked, generate a continuous response for that active scene
        if (!worldState.currentActiveSceneContext.isNullOrBlank() && !worldState.currentActiveNpc.isNullOrBlank()) {
            val npcName = worldState.currentActiveNpc
            val action = chosenOptionText ?: "Continuing"
            return EventResponse(
                resolutionText = if (isSlovak) "Reakcia na '$action': $npcName sleduje vaše konanie a napäto reaguje v probiehajúcom stretnutí." else "In response to '$action': $npcName observes your maneuver carefully as the confrontation intensifies.",
                bridgeText = if (isSlovak) "Situácia sa vyhrocuje. Každá sekunda v tejto konfrontácii rozhoduje o živote a smrti." else "The immediate tension reaches a boiling point. Every second in this confrontation matters.",
                nextEventTitle = if (isSlovak) "Vyhrotenie: $npcName" else "Confrontation: $npcName",
                nextEventText = if (isSlovak) "Konfrontácia s $npcName pokračuje. $npcName odmieta ustúpiť a čaká na váš ďalší krok." else "The ongoing encounter with $npcName continues. $npcName stands firm and demands your next decisive action.",
                location = "Village",
                npcName = npcName,
                npcTitle = if (isSlovak) "Aktívny Protivník" else "Active Adversary",
                npcArchetype = "KNIGHT",
                statChanges = StatChanges(goldChange = -5, healthChange = -5, regionalTensionChange = 5, notorietyChange = 5, factionChanges = mapOf("Peasants" to 5)),
                newWorldFlags = listOf("ONGOING_CONFLICT"),
                updatedActiveSceneContext = null, // Resolve scene on follow up
                updatedActiveNpc = null,
                options = if (isSlovak) listOf(
                    EventOption(1, "Pokúsiť sa o ústup", "Útek", "Peasant_Action"),
                    EventOption(2, "Trvať na svojich podmienkach", "Boj", "Noble_Action"),
                    EventOption(3, "Ponúknuť zlato a mier", "Ťažká Mošna", "Merchant_Action")
                ) else listOf(
                    EventOption(1, "Attempt swift retreat", "Escape", "Peasant_Action"),
                    EventOption(2, "Press terms and draw steel", "Combat", "Noble_Action"),
                    EventOption(3, "Offer gold to settle", "Heavy Purse", "Merchant_Action")
                )
            )
        }

        val templates = if (isSlovak) listOf(
            OfflineTemplate(
                title = "Katedrálny Desiatok",
                text = "Biskup Alistair prehovoril k zhromaždeniu pred pozláteným oltárom a žiada príspevky na obnovu panského chrámu.",
                location = "Cathedral", npcName = "Biskup Alistair", npcTitle = "Vysoký Prelát Svätej Stolice", npcArchetype = "BISHOP",
                goldChange = -10, healthChange = 0, statusEffect = "Sväté Požehnanie", factionMap = mapOf("Church" to 15, "Peasants" to 5), flag = "CHURCH_SPONSORSHIP",
                opt1 = "Darovať mince za relikviu", tag1 = "Cirkev", archetype1 = "Church_Action",
                opt2 = "Navrhnúť stavebnú zmluvu", tag2 = "Zmluva", archetype2 = "Merchant_Action",
                opt3 = "Požiadať o azyl pútnikov", tag3 = "Azyl", archetype3 = "Church_Action"
            ),
            OfflineTemplate(
                title = "Prepad Zbojníckeho Tábora",
                text = "Kapitán Vane so svojimi zbojníkmi prepadol vašu družinu v Šeptajúcich lesoch a žiada výkupné alebo súboj.",
                location = "Forest", npcName = "Kapitán Vane", npcTitle = "Zbojnícky Vodca", npcArchetype = "BANDIT",
                goldChange = -15, healthChange = -10, statusEffect = "Bojom Zocelený", factionMap = mapOf("Underworld" to 15, "Nobility" to -10), flag = "BANDIT_PACT",
                opt1 = "Vyzvať vodcu na súboj", tag1 = "Boj", archetype1 = "Noble_Action",
                opt2 = "Hodiť ťažkú mošnu zlata", tag2 = "Výkupné", archetype2 = "Merchant_Action",
                opt3 = "Navrhnúť tajný pakt tieňov", tag3 = "Dohoda", archetype3 = "Underworld_Action"
            ),
            OfflineTemplate(
                title = "Trh Cechových Kupcov",
                text = "Cechmajster Corvus kontroluje dovoz drahého hodvábu a láka bohatých patrónov na investície do karaván.",
                location = "Marketplace", npcName = "Cechmajster Corvus", npcTitle = "Hodvábny Cechmajster", npcArchetype = "MERCHANT",
                goldChange = 35, healthChange = 0, statusEffect = "Patrón Cechu", factionMap = mapOf("Guilds" to 20, "Peasants" to 5), flag = "GUILD_MASTER",
                opt1 = "Sformulovať obchodnú zmluvu", tag1 = "Zmluva", archetype1 = "Merchant_Action",
                opt2 = "Podplatiť úradníka za licenciu", tag2 = "Úplatok", archetype2 = "Merchant_Action",
                opt3 = "Pomôcť s vykládkou debien", tag3 = "Práca", archetype3 = "Peasant_Action"
            ),
            OfflineTemplate(
                title = "Inšpekcia Panského Sídla",
                text = "Lord Reginald obchádza feudálne panstvo a žiada verných rytierov na presadzovanie kráľovského poriadku.",
                location = "Castle", npcName = "Lord Reginald", npcTitle = "Feudálny Správca", npcArchetype = "NOBLE",
                goldChange = 40, healthChange = 0, statusEffect = "Šľachtická Priazeň", factionMap = mapOf("Nobility" to 20, "Church" to 5), flag = "CROWN_FAVOR",
                opt1 = "Zložiť rytiersku prísahu", tag1 = "Prísaha", archetype1 = "Noble_Action",
                opt2 = "Vyzvať panského šampióna", tag2 = "Súboj", archetype2 = "Noble_Action",
                opt3 = "Predložiť zmluvu na rudu", tag3 = "Zmluva", archetype3 = "Merchant_Action"
            ),
            OfflineTemplate(
                title = "Tajný Apenínsky Trh",
                text = "Bylinkárka Isolde ponúka zriedkavé liečivé elixíry z horských bylín a hľadá dôveryhodného pomocníka.",
                location = "Village", npcName = "Isolde Liečiteľka", npcTitle = "Horská Alchymistka", npcArchetype = "ALCHEMIST",
                goldChange = -20, healthChange = 25, statusEffect = "Elixír Života", factionMap = mapOf("Peasants" to 15, "Guilds" to 5), flag = "HERBAL_ALLIANCE",
                opt1 = "Kúpiť mastičku proti moru", tag1 = "Liek", archetype1 = "Peasant_Action",
                opt2 = "Ponúknuť zber vzácnych korienkov", tag2 = "Zber", archetype2 = "Peasant_Action",
                opt3 = "Odkúpiť recept pre lekárnikov", tag3 = "Recept", archetype3 = "Merchant_Action"
            ),
            OfflineTemplate(
                title = "Strážna Veža na Hranici",
                text = "Rytier Sir Roderick hliadkuje pri rozpadnutej veži a podozrieva každého pocestného zo špionáže.",
                location = "Castle", npcName = "Sir Roderick", npcTitle = "Hradný Kapitán", npcArchetype = "KNIGHT",
                goldChange = -5, healthChange = -5, statusEffect = "Vojenská Prísnosť", factionMap = mapOf("Nobility" to 10, "Peasants" to -5), flag = "BORDER_GUARD",
                opt1 = "Ukázať panskú priepustku", tag1 = "Pečať", archetype1 = "Noble_Action",
                opt2 = "Ponúknuť striebro za bránu", tag2 = "Minca", archetype2 = "Merchant_Action",
                opt3 = "Vytiahnuť meč a bojovať", tag3 = "Boj", archetype3 = "Noble_Action"
            )
        ) else listOf(
            OfflineTemplate(
                title = "The Holy Cathedral Tithe",
                text = "Bishop Alistair addresses the congregation from the gilded altar, demanding contributions for church restoration.",
                location = "Cathedral", npcName = "Bishop Alistair", npcTitle = "High Prelate of the Holy See", npcArchetype = "BISHOP",
                goldChange = -10, healthChange = 0, statusEffect = "Pious Blessing", factionMap = mapOf("Church" to 15, "Peasants" to 5), flag = "CHURCH_SPONSORSHIP",
                opt1 = "Donate coins for relic", tag1 = "Church", archetype1 = "Church_Action",
                opt2 = "Propose restoration contract", tag2 = "Contract", archetype2 = "Merchant_Action",
                opt3 = "Request sanctuary for pilgrims", tag3 = "Sanctuary", archetype3 = "Church_Action"
            ),
            OfflineTemplate(
                title = "Bandit Outpost Raid",
                text = "Captain Vane and his outlaws ambush your party in the Whispering Woods, demanding tribute or a duel.",
                location = "Forest", npcName = "Captain Vane", npcTitle = "Outlaw Highwayman", npcArchetype = "BANDIT",
                goldChange = -15, healthChange = -10, statusEffect = "Battle Tested", factionMap = mapOf("Underworld" to 15, "Nobility" to -10), flag = "BANDIT_PACT",
                opt1 = "Challenge outlaw leader", tag1 = "Combat", archetype1 = "Noble_Action",
                opt2 = "Toss heavy purse for passage", tag2 = "Purse", archetype2 = "Merchant_Action",
                opt3 = "Propose pact with guild", tag3 = "Pact", archetype3 = "Underworld_Action"
            ),
            OfflineTemplate(
                title = "Merchant Guild Bazaar",
                text = "Guildmaster Corvus inspects silk imports and invites wealthy patrons to invest in trade caravans.",
                location = "Marketplace", npcName = "Guildmaster Corvus", npcTitle = "Silk Guildhead", npcArchetype = "MERCHANT",
                goldChange = 35, healthChange = 0, statusEffect = "Guild Patron", factionMap = mapOf("Guilds" to 20, "Peasants" to 5), flag = "GUILD_MASTER",
                opt1 = "Draft trade caravan contract", tag1 = "Contract", archetype1 = "Merchant_Action",
                opt2 = "Bribe official for license", tag2 = "Bribe", archetype2 = "Merchant_Action",
                opt3 = "Help unload spice crates", tag3 = "Work", archetype3 = "Peasant_Action"
            ),
            OfflineTemplate(
                title = "Manor Fief Inspection",
                text = "Lord Reginald surveys the feudal manor and demands loyal knights or magistrates to enforce royal order.",
                location = "Castle", npcName = "Lord Reginald", npcTitle = "Feudal Estate Governor", npcArchetype = "NOBLE",
                goldChange = 40, healthChange = 0, statusEffect = "Noble Favor", factionMap = mapOf("Nobility" to 20, "Church" to 5), flag = "CROWN_FAVOR",
                opt1 = "Pledge knight's oath and seal", tag1 = "Oath", archetype1 = "Noble_Action",
                opt2 = "Challenge champion to duel", tag2 = "Duel", archetype2 = "Noble_Action",
                opt3 = "Present iron ore contract", tag3 = "Contract", archetype3 = "Merchant_Action"
            ),
            OfflineTemplate(
                title = "Herbalist's Mountain Market",
                text = "Isolde the Healer offers rare mountain remedies and seeks a trustworthy runner for dangerous herbs.",
                location = "Village", npcName = "Isolde the Healer", npcTitle = "Mountain Alchemist", npcArchetype = "ALCHEMIST",
                goldChange = -20, healthChange = 25, statusEffect = "Vital Salve", factionMap = mapOf("Peasants" to 15, "Guilds" to 5), flag = "HERBAL_ALLIANCE",
                opt1 = "Purchase plague-curing poultice", tag1 = "Herbal Remedy", archetype1 = "Peasant_Action",
                opt2 = "Offer labor gathering rare marsh roots", tag2 = "Labor", archetype2 = "Peasant_Action",
                opt3 = "Buy formula rights for merchant apothecary", tag3 = "Contract", archetype3 = "Merchant_Action"
            ),
            OfflineTemplate(
                title = "Watchtower Garrison",
                text = "Sir Roderick patrols the ruined watchtower, questioning all travelers for signs of rebellion.",
                location = "Castle", npcName = "Sir Roderick", npcTitle = "Garrison Captain", npcArchetype = "KNIGHT",
                goldChange = -5, healthChange = -5, statusEffect = "Military Discipline", factionMap = mapOf("Nobility" to 10, "Peasants" to -5), flag = "BORDER_GUARD",
                opt1 = "Produce safe-passage seal from local magistrate", tag1 = "Royal Seal", archetype1 = "Noble_Action",
                opt2 = "Offer silver coin to slip past quietly", tag2 = "Heavy Purse", archetype2 = "Merchant_Action",
                opt3 = "Draw blade and fight through guardpost", tag3 = "Combat", archetype3 = "Noble_Action"
            )
        )

        val templateIndex = (turn + worldState.worldFlags.size * 3 + worldState.gold) % templates.size
        val selected = templates[templateIndex]

        val actionDesc = chosenOptionText ?: if (isSlovak) "Vaše predchádzajúce rozhodnutie" else "Your previous choice"

        return EventResponse(
            resolutionText = if (isSlovak) "V reakcii na '$actionDesc': Postava ${selected.npcName} prikývne a situácia sa mení." else "In response to '$actionDesc': ${selected.npcName} acknowledges your action as the consequences unfold.",
            bridgeText = if (isSlovak) "Správy o vašich činoch sa šíria. Život v kráľovstve pokračuje ďalej." else "News of your conduct spreads across the region. Life in the realm carries on.",
            nextEventTitle = if (isSlovak) "${selected.title} (Ťah $turn)" else "${selected.title} (Turn $turn)",
            nextEventText = selected.text,
            location = selected.location,
            npcName = selected.npcName,
            npcTitle = selected.npcTitle,
            npcArchetype = selected.npcArchetype,
            statChanges = StatChanges(
                goldChange = selected.goldChange,
                healthChange = selected.healthChange,
                statusEffect = selected.statusEffect,
                factionChanges = selected.factionMap
            ),
            newWorldFlags = listOf(selected.flag),
            options = listOf(
                EventOption(1, selected.opt1, selected.tag1, selected.archetype1),
                EventOption(2, selected.opt2, selected.tag2, selected.archetype2),
                EventOption(3, selected.opt3, selected.tag3, selected.archetype3)
            )
        )
    }

    private data class OfflineTemplate(
        val title: String,
        val text: String,
        val location: String,
        val npcName: String,
        val npcTitle: String,
        val npcArchetype: String,
        val goldChange: Int,
        val healthChange: Int,
        val statusEffect: String,
        val factionMap: Map<String, Int>,
        val flag: String,
        val opt1: String, val tag1: String, val archetype1: String,
        val opt2: String, val tag2: String, val archetype2: String,
        val opt3: String, val tag3: String, val archetype3: String
    )
}
