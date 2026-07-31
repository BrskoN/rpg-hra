package com.example.data

import com.example.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class StylizedOutcome(
    val resolutionText: String,
    val bridgeText: String
)

class GeminiApiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private val candidateModels = listOf("gemini-2.5-flash", "gemini-2.0-flash", "gemini-1.5-flash", "gemini-flash-latest", "gemini-3.5-flash")

    private fun hasApiKey(): Boolean {
        val apiKey = BuildConfig.GEMINI_API_KEY
        return apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY"
    }

    /**
     * Sends [promptText] to the first Gemini model (of [candidateModels]) that returns a usable
     * response, and returns the raw JSON text it produced. Returns null on any failure (missing
     * key, no network, every model rejected) so every caller can fall back to static content
     * without ever crashing or blocking the game on a dead API.
     */
    private suspend fun callGeminiJson(promptText: String, maxOutputTokens: Int, temperature: Double = 0.7): String? =
        withContext(Dispatchers.IO) {
            if (!hasApiKey()) return@withContext null

            val requestJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", promptText) })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("responseMimeType", "application/json")
                    put("temperature", temperature)
                    put("maxOutputTokens", maxOutputTokens)
                })
            }
            val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
            val apiKey = BuildConfig.GEMINI_API_KEY

            for (model in candidateModels) {
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
                val request = Request.Builder().url(url).post(requestBody).build()
                try {
                    val response = client.newCall(request).execute()
                    if (!response.isSuccessful) continue

                    val responseBodyString = response.body?.string() ?: continue
                    val rootObj = JSONObject(responseBodyString)
                    val candidates = rootObj.optJSONArray("candidates") ?: continue
                    if (candidates.length() == 0) continue

                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.optJSONObject("content") ?: continue
                    val parts = content.optJSONArray("parts") ?: continue
                    if (parts.length() == 0) continue

                    val jsonText = parts.getJSONObject(0).optString("text", "")
                    if (jsonText.isBlank()) continue

                    val cleanedJson = jsonText
                        .replace("^```json".toRegex(), "")
                        .replace("^```".toRegex(), "")
                        .replace("```$".toRegex(), "")
                        .trim()

                    return@withContext cleanedJson
                } catch (e: Exception) {
                    android.util.Log.e("GeminiApiService", "Failed model $model: ${e.message}")
                }
            }
            null
        }

    suspend fun generateNextEvent(
        worldState: WorldState,
        chosenActionText: String?,
        anchorContext: String? = null
    ): EventResponse? {
        try {
            val activeTitle = worldState.activeOrigin.title
            val activeFlagsArray = JSONArray().apply {
                worldState.worldFlags.forEach { put(it) }
            }

            val factionsObj = JSONObject().apply {
                worldState.factions.forEach { (faction, rep) ->
                    put(faction.displayName.lowercase(), rep)
                }
            }

            val effectiveAnchor = anchorContext ?: when {
                worldState.turnCount == 1 -> OriginSeed.getRandomSeedForOrigin(worldState.activeOrigin).seedPromptContext
                worldState.turnCount % 4 == 0 -> StoryAnchor.selectAnchorForWorldState(worldState).anchorPromptContext
                else -> null
            }

            val ongoingSceneContext = worldState.currentActiveSceneContext
            val ongoingNpc = worldState.currentActiveNpc

            val immediateSceneObj = JSONObject().apply {
                if (!ongoingSceneContext.isNullOrBlank()) {
                    put("current_active_scene_context", ongoingSceneContext)
                    put("current_active_npc", ongoingNpc ?: "Unknown")
                }
                if (effectiveAnchor != null) {
                    if (worldState.turnCount == 1) {
                        put("turn_1_anchor", effectiveAnchor)
                    } else if (ongoingSceneContext.isNullOrBlank()) {
                        put("anchor_event_context", effectiveAnchor)
                    }
                }
                put("previous_action", chosenActionText ?: "Starting encounter as $activeTitle.")
                if (worldState.lastActionConsequenceSummary.isNotBlank()) {
                    put("last_action_consequence_summary", worldState.lastActionConsequenceSummary)
                }
            }

            val backgroundLoreObj = JSONObject().apply {
                put("turn_number", worldState.turnCount)
                put("player_class", activeTitle)
                put("player_stats", JSONObject().apply {
                    put("health", worldState.health)
                    put("gold", worldState.gold)
                    put("regional_tension", worldState.regionalTension)
                    put("notoriety", worldState.notoriety)
                    put("environment", worldState.environment.name)
                })
                put("factions", factionsObj)
                put("active_flags", activeFlagsArray)
                put("recent_characters_met", JSONArray().apply {
                    worldState.recentCharactersMet.forEach { put(it) }
                })
            }

            val payloadContext = JSONObject().apply {
                put("IMMEDIATE_ACTIVE_SCENE_CRITICAL_PRIORITY", immediateSceneObj)
                put("BACKGROUND_LORE_LOW_PRIORITY", backgroundLoreObj)
            }

            val promptText = buildString {
                if (!ongoingSceneContext.isNullOrBlank()) {
                    append("MANDATORY ONGOING SCENE (Turn ${worldState.activeSceneTurns + 1} of max 3): $ongoingSceneContext. ACTIVE NPC: ${ongoingNpc ?: "Unknown"}. ")
                    if (worldState.activeSceneTurns >= 2) {
                        append("CRITICAL: This is turn 3 of this scene. You MUST resolve and finish this encounter completely now! Set 'updatedActiveSceneContext': null and 'updatedActiveNpc': null.\n\n")
                    } else {
                        append("You can continue this scene if needed, or resolve it now by setting 'updatedActiveSceneContext': null.\n\n")
                    }
                }

                if (worldState.selectedLanguage == AppLanguage.SLOVAK) {
                    append("CRITICAL LANGUAGE RULE: Write the ENTIRE response (titles, narratives, choices, resolution text) in flawless, high-register Slovak (Slovenčina). Use rich medieval vocabulary ('panský dráb', 'desiatok', 'verbovač', 'mošna', 'krčmár'). Do not output any English text.\n\n")
                } else {
                    append("CRITICAL LANGUAGE RULE: Write the ENTIRE narrative response (titles, text, choices, resolution) in ENGLISH.\n\n")
                }

                append("You are a ruthless medieval Game Master. Keep responses crisp and fast.\n\n")
                append(originLaneGuidance(worldState.activeOrigin, worldState.selectedLanguage))

                append("CRITICAL DIRECTIVES:\n")
                append("1. CONCISE CARD CHOICES (MANDATORY): Each card option 'text' MUST BE EXTREMELY SHORT, MAXIMUM 4 TO 6 WORDS (e.g. 'Ponúknuť úplatok v zrne', 'Siahnuť po meči a bojovať', 'Požiadať o azyl' / 'Offer grain bribe for passage', 'Draw blade and challenge', 'Request sanctuary'). Each card 'tag' MUST BE 1 TO 2 WORDS MAXIMUM (e.g. 'Úplatok', 'Boj', 'Azyl' / 'Bribe', 'Combat', 'Sanctuary'). NEVER put ellipsis '...' or truncated sentences on cards!\n")
                append("2. REAL STAT CONSEQUENCES & FATAL RISKS (CRITICAL): The player currently has Health=${worldState.health}, Gold=${worldState.gold}, Tension=${worldState.regionalTension}, Notoriety=${worldState.notoriety}. If the player picks a dangerous, violent, or foolish choice, ALWAYS apply severe negative penalties: healthChange (-15 to -40), goldChange (-20 to -50), regionalTensionChange (+10 to +25), notorietyChange (+10 to +25). If the player has low health (Health <= 30) and picks a combat/risky option, deal FATAL damage (healthChange: -35 to -50) so the player CAN DIE and trigger Game Over!\n")
                append("3. SCENE DURATION (1 TO 3 TURNS MAX): A confrontation can span up to 3 turns maximum for multi-part encounters, but MUST finish on or before turn 3 with 'updatedActiveSceneContext': null and 'updatedActiveNpc': null. Advance the narrative to fresh locations and new NPCs after a scene completes!\n")
                append("4. GRAND STORY PROGRESSION: At Turn 4, Turn 8, Turn 12, Turn 16, escalate the overarching kingdom narrative (e.g. War outbreak, Royal decree, Rebellion, Assassination, Plague). Do NOT trap the player in repetitive intro conversations!\n")
                append("5. CHAIN OF THOUGHT REASONING: Use 'internal_reasoning' to think step-by-step first.\n")
                append("6. NO REPETITION: Make the 3 choice cards distinctly different from past options.\n\n")

                if (effectiveAnchor != null && ongoingSceneContext.isNullOrBlank()) {
                    if (worldState.turnCount == 1) {
                        append("MANDATORY TURN 1 STORY HOOK:\n")
                        append("$effectiveAnchor\n\n")
                    } else {
                        append("ANCHOR EVENT CONTEXT:\n")
                        append("$effectiveAnchor\n\n")
                    }
                }

                append("GAME ENGINE CONTEXT PAYLOAD:\n")
                append(payloadContext.toString(2))
                append("\n\n")

                append("EXACT JSON OUTPUT FORMAT RULES:\n")
                append("1. 'internal_reasoning' MUST be the VERY FIRST field.\n")
                append("2. location MUST be one of: 'Forest', 'Village', 'Tavern', 'Castle', 'Cathedral', 'Marketplace'.\n")
                append("3. npcArchetype MUST be one of: 'PEASANT', 'MERCHANT', 'KNIGHT', 'BISHOP', 'ALCHEMIST', 'BANDIT', 'NOBLE', 'MONARCH'.\n")
                append("4. resolutionText (max 2 sentences), bridgeText (max 2 sentences), nextEventText (max 2-3 sentences).\n")
                append("5. options array must contain EXACTLY 3 items with 4-6 word 'text' and 1-2 word 'tag'.\n")
                append("6. Return ONLY valid JSON matching this exact structure without markdown backticks:\n")
                append("{\n")
                append("  \"internal_reasoning\": \"Resolving encounter with Elder Tobias.\",\n")
                append("  \"resolutionText\": \"Brutal immediate reaction of NPC (max 2 sentences).\",\n")
                append("  \"bridgeText\": \"Time-lapse fallout transition leading into next scenario (max 2 sentences).\",\n")
                append("  \"nextEventTitle\": \"Short dramatic title for next turn\",\n")
                append("  \"nextEventText\": \"Concise story setup for next turn (max 2-3 sentences).\",\n")
                append("  \"location\": \"Village\",\n")
                append("  \"npcName\": \"Elder Tobias\",\n")
                append("  \"npcTitle\": \"Village Patriarch\",\n")
                append("  \"npcArchetype\": \"PEASANT\",\n")
                append("  \"updatedActiveSceneContext\": null,\n")
                append("  \"updatedActiveNpc\": null,\n")
                append("  \"statChanges\": {\n")
                append("    \"goldChange\": -15,\n")
                append("    \"healthChange\": -5,\n")
                append("    \"regionalTensionChange\": 10,\n")
                append("    \"notorietyChange\": 5,\n")
                append("    \"statusEffect\": \"Bribe Paid\",\n")
                append("    \"factionChanges\": {\"Church\": 0, \"Peasants\": 5, \"Nobility\": -10, \"Underworld\": 10, \"Guilds\": 0}\n")
                append("  },\n")
                append("  \"newWorldFlags\": [\"GRAIN_BRIBE_ACCEPTED\"],\n")
                append("  \"options\": [\n")
                append("    {\"id\": 1, \"text\": \"Ponúknuť úplatok v zrne\", \"tag\": \"Úplatok\", \"cardArchetype\": \"Peasant_Action\"},\n")
                append("    {\"id\": 2, \"text\": \"Požiadať o pokorný azyl\", \"tag\": \"Azyl\", \"cardArchetype\": \"Church_Action\"},\n")
                append("    {\"id\": 3, \"text\": \"Siahnuť po meči a bojovať\", \"tag\": \"Boj\", \"cardArchetype\": \"Peasant_Action\"}\n")
                append("  ]\n")
                append("}")
            }

            val cleanedJson = callGeminiJson(promptText, maxOutputTokens = 1024) ?: return null
            return parseEventResponseJson(cleanedJson, activeTitle)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * Generates a single self-contained "spice" vignette that slots into the deck-driven sandbox
     * turns of an origin that already has an authored EventDeck. Kept strictly within that
     * origin's plausible social lane (a peasant never attends a royal feast) so it reads as
     * belonging to this specific playthrough rather than a generic fantasy scene. Consequences of
     * the 3 offered choices are NOT decided here - exactly like the pre-existing AI path, they are
     * resolved by a follow-up generateNextEvent call once the player actually picks one, using the
     * same origin-lane guidance for continuity. Returns null on any failure so callers can silently
     * fall back to the authored deck content.
     */
    suspend fun generateSpiceEvent(worldState: WorldState): EventResponse? {
        try {
            val activeTitle = worldState.activeOrigin.title
            val isSlovak = worldState.selectedLanguage == AppLanguage.SLOVAK

            val promptText = buildString {
                if (isSlovak) {
                    append("CRITICAL LANGUAGE RULE: Write the ENTIRE response in flawless, high-register Slovak (Slovenčina). Use rich medieval vocabulary. Do not output any English text.\n\n")
                } else {
                    append("CRITICAL LANGUAGE RULE: Write the ENTIRE response in ENGLISH.\n\n")
                }

                append("You are a ruthless medieval Game Master writing ONE small, self-contained bonus vignette to add per-playthrough variety to an otherwise fixed storyline. ")
                append("This is NOT part of the main plot - it is a short, flavorful side-encounter with no lasting story obligations.\n\n")

                append(originLaneGuidance(worldState.activeOrigin, worldState.selectedLanguage))

                append("CURRENT STATE: Player class: $activeTitle. Turn: ${worldState.turnCount}. Gold: ${worldState.gold}. Health: ${worldState.health}. ")
                append("Tension: ${worldState.regionalTension}. Notoriety: ${worldState.notoriety}.\n\n")

                append("DIRECTIVES:\n")
                append("1. Invent a brand new minor NPC and a small, self-contained situation appropriate to this character's exact social station - nothing that requires the wider story to change.\n")
                append("2. Any new world-flag you introduce MUST be prefixed with 'SPICE_' so it never collides with the main storyline's flags.\n")
                append("3. CONCISE CARD CHOICES: each option 'text' MUST be 4-6 words max, 'tag' MUST be 1-2 words max.\n")
                append("4. location MUST be one of: 'Forest', 'Village', 'Tavern', 'Castle', 'Cathedral', 'Marketplace' - choose only ones plausible for this character.\n")
                append("5. npcArchetype MUST be one of: 'PEASANT', 'MERCHANT', 'KNIGHT', 'BISHOP', 'ALCHEMIST', 'BANDIT', 'NOBLE', 'ELDER'.\n")
                append("6. Return ONLY valid JSON, no markdown backticks:\n")
                append("{\n")
                append("  \"internal_reasoning\": \"Short one-off vignette fitting this character's station.\",\n")
                append("  \"nextEventTitle\": \"Short dramatic title\",\n")
                append("  \"nextEventText\": \"Concise self-contained setup (max 2-3 sentences).\",\n")
                append("  \"location\": \"Village\",\n")
                append("  \"npcName\": \"Name\",\n")
                append("  \"npcTitle\": \"Title\",\n")
                append("  \"npcArchetype\": \"PEASANT\",\n")
                append("  \"options\": [\n")
                append("    {\"id\": 1, \"text\": \"Short option text\", \"tag\": \"Tag\", \"cardArchetype\": \"Peasant_Action\"},\n")
                append("    {\"id\": 2, \"text\": \"Short option text\", \"tag\": \"Tag\", \"cardArchetype\": \"Peasant_Action\"},\n")
                append("    {\"id\": 3, \"text\": \"Short option text\", \"tag\": \"Tag\", \"cardArchetype\": \"Peasant_Action\"}\n")
                append("  ]\n")
                append("}")
            }

            val cleanedJson = callGeminiJson(promptText, maxOutputTokens = 512, temperature = 0.9) ?: return null
            return parseEventResponseJson(cleanedJson, activeTitle)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * Restyles the already-decided mechanical outcome of an authored EventDeck choice into fresh
     * sensory prose. The consequence (stat/flag deltas) is fixed before this is ever called - the
     * model is only asked to re-narrate it, never to change it. Returns null on any failure so the
     * caller keeps its static authored text.
     */
    suspend fun stylizeOutcome(
        worldState: WorldState,
        npcName: String,
        npcTitle: String,
        location: String,
        chosenActionText: String,
        consequenceSummary: String,
        lang: AppLanguage
    ): StylizedOutcome? {
        try {
            val promptText = buildString {
                if (lang == AppLanguage.SLOVAK) {
                    append("CRITICAL LANGUAGE RULE: Write in flawless, high-register Slovak (Slovenčina). Do not output any English text.\n\n")
                } else {
                    append("CRITICAL LANGUAGE RULE: Write in ENGLISH.\n\n")
                }

                append("You are re-narrating an ALREADY-DECIDED outcome in a dark medieval RPG. You must NOT invent a different outcome, change what happened, or add new stat effects - only describe, with fresh sensory detail (sounds, smells, physical sensation), the exact event below.\n\n")

                append("Location: $location\n")
                append("NPC: $npcName ($npcTitle)\n")
                append("Player's chosen action: \"$chosenActionText\"\n")
                append("Decided outcome (do not contradict this): $consequenceSummary\n\n")

                append("Write exactly two short fields, each MAX 2 sentences, in the same tone as a gritty Witcher/Game of Thrones-style narrator:\n")
                append("Return ONLY valid JSON, no markdown backticks:\n")
                append("{\n")
                append("  \"resolutionText\": \"Immediate sensory reaction to the decided outcome (max 2 sentences).\",\n")
                append("  \"bridgeText\": \"Short time-lapse transition following from that outcome (max 2 sentences).\"\n")
                append("}")
            }

            val cleanedJson = callGeminiJson(promptText, maxOutputTokens = 256, temperature = 0.8) ?: return null
            val obj = JSONObject(cleanedJson)
            val resolutionText = sanitizeText(obj.optString("resolutionText", ""), "")
            val bridgeText = sanitizeText(obj.optString("bridgeText", ""), "")
            if (resolutionText.isBlank() || bridgeText.isBlank()) return null
            return StylizedOutcome(resolutionText, bridgeText)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun originLaneGuidance(origin: OriginClass, lang: AppLanguage): String {
        val laneEn = when (origin) {
            OriginClass.PEASANT -> "This character is a lowly peasant. Plausible settings: village, farm, forest, tavern, the fringes of a market. Plausible company: other peasants, local clergy, huntsmen, bailiffs, traveling merchants, bandits. NEVER place them at a royal court, in a king's presence, or handling matters of state - they are far too lowborn for that."
            OriginClass.ACOLYTE -> "This character is a monastery acolyte. Plausible settings: monastery, cathedral, village, crypt. Plausible company: monks, priests, the bishop, pilgrims, common villagers. NEVER place them commanding armies or presiding over royal court politics."
            OriginClass.GUILD_APPRENTICE -> "This character is a city guild apprentice. Plausible settings: workshop, marketplace, tavern, city gate, docks. Plausible company: guildmasters, merchants, city guards, moneylenders, underworld contacts. NEVER place them in rural farm life or in a royal throne room as an equal to nobles."
            OriginClass.LESSER_NOBLE -> "This character is an impoverished lesser noble with a small ancestral keep. Plausible settings: their own keep, neighboring estates, a county town, occasionally the fringes of a royal court (as a minor attendee, never as an equal to the king). Plausible company: other minor nobles, knights, tax collectors, tenants. Keep the scale modest - they do not command national armies."
            else -> "Keep the scene appropriate to this character's actual social station and means."
        }
        val laneSk = when (origin) {
            OriginClass.PEASANT -> "Táto postava je prostý sedliak. Vhodné prostredia: dedina, pole, les, krčma, okraj trhoviska. Vhodná spoločnosť: iní sedliaci, miestny klér, hájnici, drábi, potulní kupci, zbojníci. NIKDY ho nezasaď na kráľovský dvor, do prítomnosti kráľa, ani do štátnych záležitostí - je na to príliš nízkeho pôvodu."
            OriginClass.ACOLYTE -> "Táto postava je kláštorný akolyt. Vhodné prostredia: kláštor, katedrála, dedina, krypta. Vhodná spoločnosť: mnísi, kňazi, biskup, pútnici, obyčajní dedinčania. NIKDY ho nezasaď do velenia vojskám ani do kráľovskej dvorskej politiky."
            OriginClass.GUILD_APPRENTICE -> "Táto postava je mestský cechový tovariš. Vhodné prostredia: dielňa, trhovisko, krčma, mestská brána, prístav. Vhodná spoločnosť: cechmajstri, kupci, mestská stráž, úžerníci, kontakty z podsvetia. NIKDY ho nezasaď do vidieckeho roľníckeho života ani na kráľovský trón ako rovnocenného šľachte."
            OriginClass.LESSER_NOBLE -> "Táto postava je zubožený nízky šľachtic s malou rodovou tvrdzou. Vhodné prostredia: vlastná tvrdz, susedné panstvá, krajské mesto, okrajovo kráľovský dvor (ako drobný účastník, nikdy ako rovný kráľovi). Vhodná spoločnosť: iní drobní šľachtici, rytieri, výbercovia daní, nájomcovia. Drž rozsah skromný - nevelí národným vojskám."
            else -> "Zasaď scénu primerane skutočnému spoločenskému postaveniu a prostriedkom tejto postavy."
        }
        val guidance = if (lang == AppLanguage.SLOVAK) laneSk else laneEn
        return "CHARACTER SOCIAL LANE (MANDATORY): $guidance\n\n"
    }

    // Overload for backward compatibility
    suspend fun generateNextEvent(
        currentState: GameState,
        chosenActionText: String?
    ): EventResponse? {
        val mappedFactions = mutableMapOf<Faction, Int>()
        currentState.factionReputation.forEach { (name, rep) ->
            val matching = Faction.values().find { it.displayName.equals(name, ignoreCase = true) } ?: Faction.PEASANTS
            mappedFactions[matching] = rep
        }
        val convertedWorld = WorldState(
            currentChapter = 1,
            gold = currentState.gold,
            health = currentState.health,
            maxHealth = currentState.maxHealth,
            factions = mappedFactions,
            worldFlags = currentState.flags.toSet(),
            turnCount = currentState.turnCount
        )
        return generateNextEvent(convertedWorld, chosenActionText)
    }

    private fun sanitizeText(input: String, fallback: String): String {
        var trimmed = input.trim()
        if (trimmed.isBlank()) return fallback
        val validEndings = setOf('.', '!', '?', '"', '»', '…')
        if (trimmed.last() !in validEndings) {
            val lastPunct = trimmed.indexOfLast { it in validEndings }
            if (lastPunct > 15) {
                trimmed = trimmed.substring(0, lastPunct + 1)
            } else {
                val lastSpace = trimmed.lastIndexOf(' ')
                if (lastSpace > 10) {
                    trimmed = trimmed.substring(0, lastSpace).trim() + "."
                } else {
                    trimmed = "$trimmed."
                }
            }
        }
        return trimmed
    }

    private fun sanitizeOptionText(input: String, fallback: String): String {
        var trimmed = input.trim()
        if (trimmed.isBlank()) return fallback
        val validEndings = setOf('.', '!', '?', '"', '»', '…')
        if (trimmed.last() in validEndings) {
            trimmed = trimmed.dropLast(1).trim()
        }
        val lastSpace = trimmed.lastIndexOf(' ')
        if (trimmed.length > 80 && lastSpace > 20) {
            trimmed = trimmed.substring(0, lastSpace).trim()
        }
        return trimmed
    }

    private fun parseEventResponseJson(jsonString: String, activeTitle: String): EventResponse? {
        return try {
            val obj = JSONObject(jsonString)
            val internalReasoning = obj.optString("internal_reasoning", obj.optString("internalReasoning", ""))
            val rawResolutionText = obj.optString("resolutionText", "The local figures react swiftly to your chosen path.")
            val rawBridgeText = obj.optString("bridgeText", "Time marches forward as consequences ripple across the local fiefs.")
            val rawNextEventTitle = obj.optString("nextEventTitle", obj.optString("title", "An Unexpected Encounter"))
            val rawNextEventText = obj.optString("nextEventText", obj.optString("text", "You tread carefully through the medieval realm..."))

            val resolutionText = sanitizeText(rawResolutionText, "The local figures react swiftly to your chosen path.")
            val bridgeText = sanitizeText(rawBridgeText, "Time marches forward as consequences ripple across the local fiefs.")
            val nextEventTitle = sanitizeText(rawNextEventTitle, "An Unexpected Encounter").removeSuffix(".")
            val nextEventText = sanitizeText(rawNextEventText, "You tread carefully through the medieval realm...")

            val location = obj.optString("location", "Village")
            val npcName = obj.optString("npcName", "Local Wanderer")
            val npcTitle = obj.optString("npcTitle", "Kingdom Resident")
            val npcArchetype = obj.optString("npcArchetype", "PEASANT")

            val statObj = obj.optJSONObject("statChanges")
            val goldChange = statObj?.optInt("goldChange", 0) ?: 0
            val healthChange = statObj?.optInt("healthChange", 0) ?: 0
            val tensionChange = statObj?.optInt("regionalTensionChange", statObj.optInt("tensionChange", 0)) ?: 0
            val notorietyChange = statObj?.optInt("notorietyChange", 0) ?: 0
            val socialProgressChange = statObj?.optInt("socialProgressChange", 0) ?: 0
            val statusEffect = statObj?.optString("statusEffect", "")?.takeIf { it.isNotBlank() }

            val factionMap = mutableMapOf<String, Int>()
            val factionJson = statObj?.optJSONObject("factionChanges")
            if (factionJson != null) {
                val keys = factionJson.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    factionMap[key] = factionJson.optInt(key, 0)
                }
            }

            val newWorldFlagsList = mutableListOf<String>()
            val flagsArray = obj.optJSONArray("newWorldFlags") ?: obj.optJSONArray("newFlags")
            if (flagsArray != null) {
                for (i in 0 until flagsArray.length()) {
                    val flagStr = flagsArray.optString(i)
                    if (!flagStr.isNullOrBlank()) {
                        newWorldFlagsList.add(flagStr)
                    }
                }
            }

            val defaultArchetype = when {
                activeTitle.contains("Peasant", ignoreCase = true) -> "Peasant_Action"
                activeTitle.contains("Merchant", ignoreCase = true) -> "Merchant_Action"
                activeTitle.contains("Acolyte", ignoreCase = true) || activeTitle.contains("Bishop", ignoreCase = true) -> "Church_Action"
                activeTitle.contains("Shadow", ignoreCase = true) || activeTitle.contains("Outlaw", ignoreCase = true) -> "Underworld_Action"
                else -> "Noble_Action"
            }

            val optionsArray = obj.optJSONArray("options")
            val optionsList = mutableListOf<EventOption>()
            if (optionsArray != null) {
                for (i in 0 until optionsArray.length()) {
                    val optObj = optionsArray.getJSONObject(i)
                    val id = optObj.optInt("id", i + 1)
                    val rawOptText = optObj.optString("text", "Proceed carefully")
                    val optText = sanitizeOptionText(rawOptText, "Proceed carefully")
                    val tag = optObj.optString("tag", "Action")
                    val cardArchetype = optObj.optString("cardArchetype", defaultArchetype)
                    optionsList.add(EventOption(id, optText, tag, cardArchetype))
                }
            }

            while (optionsList.size < 3) {
                val idx = optionsList.size + 1
                optionsList.add(EventOption(idx, "Consider your next move ($idx)", "Action", defaultArchetype))
            }

            val updatedActiveSceneContext = if (obj.has("updatedActiveSceneContext") && !obj.isNull("updatedActiveSceneContext")) {
                obj.optString("updatedActiveSceneContext").trim().takeIf { it.isNotBlank() && it != "null" }
            } else null

            val updatedActiveNpc = if (obj.has("updatedActiveNpc") && !obj.isNull("updatedActiveNpc")) {
                obj.optString("updatedActiveNpc").trim().takeIf { it.isNotBlank() && it != "null" }
            } else null

            EventResponse(
                internal_reasoning = internalReasoning,
                resolutionText = resolutionText,
                bridgeText = bridgeText,
                nextEventTitle = nextEventTitle,
                nextEventText = nextEventText,
                location = location,
                npcName = npcName,
                npcTitle = npcTitle,
                npcArchetype = npcArchetype,
                statChanges = StatChanges(
                    goldChange = goldChange,
                    healthChange = healthChange,
                    socialProgressChange = socialProgressChange,
                    statusEffect = statusEffect,
                    factionChanges = factionMap,
                    regionalTensionChange = tensionChange,
                    notorietyChange = notorietyChange
                ),
                newWorldFlags = newWorldFlagsList,
                options = optionsList.take(3),
                updatedActiveSceneContext = updatedActiveSceneContext,
                updatedActiveNpc = updatedActiveNpc
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
