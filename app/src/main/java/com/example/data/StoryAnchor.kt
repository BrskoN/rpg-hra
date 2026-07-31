package com.example.data

enum class AnchorCategory {
    GENERAL,
    DESPERATION,
    HIGH_TENSION,
    FLAG_BASED
}

data class OriginSeed(
    val id: String,
    val originClass: OriginClass,
    val seedTitle: String,
    val seedPromptContext: String
) {
    companion object {
        val PEASANT_SEEDS = listOf(
            OriginSeed(
                id = "peasant_seed_tax",
                originClass = OriginClass.PEASANT,
                seedTitle = "The Winter Road Tax",
                seedPromptContext = "Bailiff Peter demands harsh winter road tax. If unpaid, he threatens dungeon time. The player has only a few coins and a hidden dagger."
            ),
            OriginSeed(
                id = "peasant_seed_disease",
                originClass = OriginClass.PEASANT,
                seedTitle = "The Winter Fever",
                seedPromptContext = "Family member is sick with winter fever. Herbalist demands extortionate price in gold or dangerous labor."
            ),
            OriginSeed(
                id = "peasant_seed_poaching",
                originClass = OriginClass.PEASANT,
                seedTitle = "The Lord's Forest",
                seedPromptContext = "Caught poaching in the Lord's forest by the cruel huntsman, who demands submission or hanging."
            ),
            OriginSeed(
                id = "peasant_seed_draft",
                originClass = OriginClass.PEASANT,
                seedTitle = "The Royal Press-Gang",
                seedPromptContext = "Royal recruiters arrive to press-gang men into a bloody eastern war."
            )
        )

        val GUILD_SEEDS = listOf(
            OriginSeed(
                id = "guild_seed_silk",
                originClass = OriginClass.GUILD_APPRENTICE,
                seedTitle = "The Missing Silk Shipment",
                seedPromptContext = "A shipment of silk went missing, and the Guildmaster blames the player. City guards are outside the door to search the premises."
            ),
            OriginSeed(
                id = "guild_seed_contraband",
                originClass = OriginClass.GUILD_APPRENTICE,
                seedTitle = "The Banned Reagents",
                seedPromptContext = "Unmarked crates of banned alchemical reagents were planted in your workshop cellar right as trade inspectors arrive."
            )
        )

        val ACOLYTE_SEEDS = listOf(
            OriginSeed(
                id = "acolyte_seed_manuscript",
                originClass = OriginClass.ACOLYTE,
                seedTitle = "The Heretical Manuscript",
                seedPromptContext = "They accidentally discovered a forbidden, heretical manuscript in the Cathedral library. The High Bishop approaches, asking what the player is hiding."
            ),
            OriginSeed(
                id = "acolyte_seed_relic",
                originClass = OriginClass.ACOLYTE,
                seedTitle = "The Missing Shrine Relic",
                seedPromptContext = "A venerated saint's relic vanished from the altar shrine during vespers, and the Inquisitor accuses the acolytes."
            )
        )

        val SQUIRE_SEEDS = listOf(
            OriginSeed(
                id = "squire_seed_oath",
                originClass = OriginClass.SQUIRE,
                seedTitle = "The Knight's Shadow",
                seedPromptContext = "Assigned to serve a demanding knight, the player must prove worth before the war council while old peasant ties still pull at their conscience."
            )
        )

        val KNIGHT_SEEDS = listOf(
            OriginSeed(
                id = "knight_seed_raiders",
                originClass = OriginClass.KNIGHT,
                seedTitle = "The Border Raiders",
                seedPromptContext = "Lord Vane demands immediate military service against border raiders, but scouts report the raiders are starving villagers."
            )
        )

        val MERCHANT_SEEDS = listOf(
            OriginSeed(
                id = "merchant_seed_blockade",
                originClass = OriginClass.MASTER_MERCHANT,
                seedTitle = "The Trade Bridge Blockade",
                seedPromptContext = "Rival merchant barons have blockaded the trade bridge during the harsh winter, cutting off supply routes."
            )
        )

        val BISHOP_SEEDS = listOf(
            OriginSeed(
                id = "bishop_seed_heresy",
                originClass = OriginClass.BISHOP,
                seedTitle = "The Parish Heresy",
                seedPromptContext = "Heresy spreads through lower parishes while Church inquisitors demand blood and public executions."
            )
        )

        val OUTLAW_SEEDS = listOf(
            OriginSeed(
                id = "outlaw_seed_ambush",
                originClass = OriginClass.OUTLAW_KING,
                seedTitle = "The Coach Ambush Inquest",
                seedPromptContext = "Crown mercenaries surround your forest hideout after a high-profile coach ambush."
            )
        )

        val PRISONER_SEEDS = listOf(
            OriginSeed(
                id = "prisoner_seed_dungeon",
                originClass = OriginClass.PRISONER,
                seedTitle = "The Iron Shackles",
                seedPromptContext = "The player begins in a damp stone dungeon cell. A sympathetic guard offers a rusty iron file or a quiet escape route in exchange for a future favor."
            )
        )

        val BEGGAR_SEEDS = listOf(
            OriginSeed(
                id = "beggar_seed_crust",
                originClass = OriginClass.BEGGAR,
                seedTitle = "The Frozen Cathedral Gates",
                seedPromptContext = "The player sits wrapped in threadbare rags outside cathedral gates as cold snow falls. A wealthy merchant drops a copper coin or demands you be cleared away."
            )
        )

        val OUTCAST_SEEDS = listOf(
            OriginSeed(
                id = "outcast_seed_wasteland",
                originClass = OriginClass.OUTCAST,
                seedTitle = "The Exile Boundary",
                seedPromptContext = "The player crosses the fiefdom border into desolate waste lands. A band of hungry vagrants approaches with raised cudgels."
            )
        )

        fun getRandomSeedForOrigin(origin: OriginClass): OriginSeed {
            val pool = when (origin) {
                OriginClass.PEASANT -> PEASANT_SEEDS
                OriginClass.GUILD_APPRENTICE -> GUILD_SEEDS
                OriginClass.ACOLYTE -> ACOLYTE_SEEDS
                OriginClass.SQUIRE -> SQUIRE_SEEDS
                OriginClass.KNIGHT -> KNIGHT_SEEDS
                OriginClass.MASTER_MERCHANT -> MERCHANT_SEEDS
                OriginClass.BISHOP -> BISHOP_SEEDS
                OriginClass.OUTLAW_KING -> OUTLAW_SEEDS
                OriginClass.PRISONER -> PRISONER_SEEDS
                OriginClass.BEGGAR -> BEGGAR_SEEDS
                OriginClass.OUTCAST -> OUTCAST_SEEDS
            }
            return pool.random()
        }
    }
}

data class StoryAnchor(
    val id: String,
    val title: String,
    val category: AnchorCategory,
    val requiredFlag: String? = null,
    val minTension: Int = 0,
    val minNotoriety: Int = 0,
    val maxGold: Int? = null,
    val anchorPromptContext: String
) {
    fun canTrigger(worldState: WorldState): Boolean {
        if (requiredFlag != null && !worldState.worldFlags.contains(requiredFlag)) return false
        if (worldState.regionalTension < minTension) return false
        if (worldState.notoriety < minNotoriety) return false
        if (maxGold != null && worldState.gold > maxGold) return false
        return true
    }

    companion object {
        val ANCHOR_POOL = listOf(
            // GENERAL EVENTS (1-15)
            StoryAnchor(
                id = "anchor_wandering_quack",
                title = "Wandering Quack",
                category = AnchorCategory.GENERAL,
                anchorPromptContext = "A traveling healer offers a suspicious herbal remedy for your remaining coins."
            ),
            StoryAnchor(
                id = "anchor_deserter_barn",
                title = "Deserter in the Barn",
                category = AnchorCategory.GENERAL,
                anchorPromptContext = "A runaway mercenary hides in your hayloft while guards search nearby."
            ),
            StoryAnchor(
                id = "anchor_church_tithe",
                title = "Church Tithe",
                category = AnchorCategory.GENERAL,
                anchorPromptContext = "A bishop's clerk demands grain quotas to construct the regional cathedral."
            ),
            StoryAnchor(
                id = "anchor_village_feast",
                title = "Village Feast",
                category = AnchorCategory.GENERAL,
                anchorPromptContext = "An opportunity to boost village morale or steal from a drunk merchant's purse."
            ),
            StoryAnchor(
                id = "anchor_dead_horse",
                title = "Dead Horse on the Road",
                category = AnchorCategory.GENERAL,
                anchorPromptContext = "A butchered pack horse lies on the highway. Meat for winter, but who owned it?"
            ),
            StoryAnchor(
                id = "anchor_winter_blizzard",
                title = "Winter Blizzard",
                category = AnchorCategory.GENERAL,
                anchorPromptContext = "Your hovel roof collapses. You must decide whose shelter to beg for."
            ),
            StoryAnchor(
                id = "anchor_conscription_drive",
                title = "Conscription Drive",
                category = AnchorCategory.GENERAL,
                anchorPromptContext = "Royal recruiters forcibly press-gang young men into an eastern war."
            ),
            StoryAnchor(
                id = "anchor_lords_lost_hound",
                title = "Lord's Lost Hound",
                category = AnchorCategory.GENERAL,
                anchorPromptContext = "You discover a lord's pedigree hunting dog with an expensive silver collar."
            ),
            StoryAnchor(
                id = "anchor_wandering_cleric",
                title = "Wandering Cleric",
                category = AnchorCategory.GENERAL,
                anchorPromptContext = "A lost monk seeks shelter and guidance to the regional abbey."
            ),
            StoryAnchor(
                id = "anchor_ancient_chest",
                title = "Ancient Chest",
                category = AnchorCategory.GENERAL,
                anchorPromptContext = "Uncovered an ironbound box while plowing the soil. Risk curse or surrender to lord?"
            ),
            StoryAnchor(
                id = "anchor_relic_peddler",
                title = "Relic Peddler",
                category = AnchorCategory.GENERAL,
                anchorPromptContext = "A merchant sells a claimed 'holy bone' guaranteeing crop protection."
            ),
            StoryAnchor(
                id = "anchor_boundary_dispute",
                title = "Boundary Dispute",
                category = AnchorCategory.GENERAL,
                anchorPromptContext = "A neighbor moved their fence overnight, stealing your arable soil."
            ),
            StoryAnchor(
                id = "anchor_town_fair_permit",
                title = "Town Fair Permit",
                category = AnchorCategory.GENERAL,
                anchorPromptContext = "Opportunity to sell excess grain at the town market, but guild fees apply."
            ),
            StoryAnchor(
                id = "anchor_starving_pack",
                title = "Starving Pack",
                category = AnchorCategory.GENERAL,
                anchorPromptContext = "Wolves surround the livestock pen at dusk."
            ),
            StoryAnchor(
                id = "anchor_beggars_gate",
                title = "Beggars at the Gate",
                category = AnchorCategory.GENERAL,
                maxGold = 20,
                anchorPromptContext = "Destitute families beg for scraps at your door during a freeze."
            ),

            // HIGH TENSION & DESPERATION EVENTS (16-21)
            StoryAnchor(
                id = "anchor_night_thief",
                title = "Night Thief",
                category = AnchorCategory.DESPERATION,
                maxGold = 15,
                anchorPromptContext = "Catching a desperate orphan stealing your winter grain reserves."
            ),
            StoryAnchor(
                id = "anchor_army_march",
                title = "Army March",
                category = AnchorCategory.HIGH_TENSION,
                minTension = 40,
                anchorPromptContext = "Royal troops march through, foraging and looting local farms."
            ),
            StoryAnchor(
                id = "anchor_witch_trial",
                title = "Witch Trial",
                category = AnchorCategory.HIGH_TENSION,
                minTension = 50,
                anchorPromptContext = "Villagers accuse an old herbalist of cursing the wells. A mob forms."
            ),
            StoryAnchor(
                id = "anchor_bailiffs_murder",
                title = "Bailiff's Murder",
                category = AnchorCategory.HIGH_TENSION,
                minTension = 60,
                anchorPromptContext = "The regional mayor is found assassinated. Guards lock down the village."
            ),
            StoryAnchor(
                id = "anchor_crossroads_mutiny",
                title = "Crossroads Mutiny",
                category = AnchorCategory.HIGH_TENSION,
                minTension = 70,
                anchorPromptContext = "Armed peasants gather with pitchforks preparing to attack the tax wagon."
            ),
            StoryAnchor(
                id = "anchor_grand_inquisition_purge",
                title = "Grand Inquisition Purge",
                category = AnchorCategory.HIGH_TENSION,
                minNotoriety = 40,
                anchorPromptContext = "Inquisitors arrive looking for dark magic and heretical texts."
            ),

            // FLAG-BASED CONSEQUENCE EVENTS (22-30)
            StoryAnchor(
                id = "anchor_herbalist_debt",
                title = "Herbalist's Debt",
                category = AnchorCategory.FLAG_BASED,
                requiredFlag = "SAVED_SICK_CHILD",
                anchorPromptContext = "The herbalist who healed your family flees inquisitors and asks for asylum."
            ),
            StoryAnchor(
                id = "anchor_huntsmans_blackmail",
                title = "Huntsman's Blackmail",
                category = AnchorCategory.FLAG_BASED,
                requiredFlag = "POACHER",
                anchorPromptContext = "The huntsman you bribed demands more gold, threatening to report you to the Lord."
            ),
            StoryAnchor(
                id = "anchor_draft_inquest",
                title = "Draft Inquest",
                category = AnchorCategory.FLAG_BASED,
                requiredFlag = "DESERTER_HELPED",
                anchorPromptContext = "Royal guards return searching for runaway recruits, interrogating you personally."
            ),
            StoryAnchor(
                id = "anchor_special_investigator",
                title = "Special Investigator",
                category = AnchorCategory.FLAG_BASED,
                requiredFlag = "KILLED_BAILIFF",
                anchorPromptContext = "The Lord sends a torturer-investigator specifically to solve Peter's death."
            ),
            StoryAnchor(
                id = "anchor_bloody_cut",
                title = "Bloody Cut",
                category = AnchorCategory.FLAG_BASED,
                requiredFlag = "OUTLAW_ALLY",
                anchorPromptContext = "Bandits leave a sack of bloodstained silver on your doorstep as your share."
            ),
            StoryAnchor(
                id = "anchor_excommunication",
                title = "Excommunication",
                category = AnchorCategory.FLAG_BASED,
                requiredFlag = "CHURCH_DEBT",
                anchorPromptContext = "The village priest publicly curses you from the pulpit for unpaid tithes."
            ),
            StoryAnchor(
                id = "anchor_smugglers_cart",
                title = "Smuggler's Cart",
                category = AnchorCategory.FLAG_BASED,
                requiredFlag = "GUILD_CONTACT",
                anchorPromptContext = "A city trader asks you to hide contraband under your hay wagon past guards."
            ),
            StoryAnchor(
                id = "anchor_torch_night",
                title = "Torch Night",
                category = AnchorCategory.FLAG_BASED,
                requiredFlag = "VILLAGE_HATRED",
                anchorPromptContext = "Angered neighbors throw firebrands at your thatch roof in the dark."
            ),
            StoryAnchor(
                id = "anchor_rebellion_leader",
                title = "Rebellion Leader",
                category = AnchorCategory.FLAG_BASED,
                requiredFlag = "HERO_OF_OAKVALE",
                anchorPromptContext = "The peasantry gathers at your home, demanding you lead the assault on the Keep (Chapter 1 Climax)."
            )
        )

        fun selectAnchorForWorldState(worldState: WorldState): StoryAnchor {
            val eligible = ANCHOR_POOL.filter { it.canTrigger(worldState) }
            return if (eligible.isNotEmpty()) eligible.random() else ANCHOR_POOL.random()
        }
    }
}
