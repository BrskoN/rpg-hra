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

class GeminiApiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun generateNextEvent(
        worldState: WorldState,
        chosenActionText: String?,
        anchorContext: String? = null
    ): EventResponse? = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext null
        }

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

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
                worldState.turnCount % 5 == 0 -> StoryAnchor.selectAnchorForWorldState(worldState).anchorPromptContext
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
                    append("MANDATORY ONGOING SCENE: $ongoingSceneContext. MANDATORY ACTIVE NPC: ${ongoingNpc ?: "Unknown"}. Do NOT introduce new plots, do NOT change the NPC.\n\n")
                }

                if (worldState.selectedLanguage == AppLanguage.SLOVAK) {
                    append("CRITICAL: All generated text (titles, narratives, choices, resolution text) must be in flawless, high-register Slovak (Slovenčina) without typos (e.g., use 'relikviu', never 'rekliviu'). Action choices must strictly mirror the immediate physical reality of the active scene. No random church offerings when facing a knight or bandit. Use rich, gritty medieval vocabulary (e.g., 'panský dráb', 'desiatok', 'verbovač', 'mošna', 'krčmár'). Do not output any English text.\n\n")
                } else {
                    append("LANGUAGE RULE: Write the ENTIRE narrative response (nextEventTitle, nextEventText, resolutionText, bridgeText, and card action titles) in ENGLISH. Action choices must strictly mirror the immediate physical reality of the active scene.\n\n")
                }

                append("You are a ruthless, causality-driven Game Master. Do NOT generate random, disconnected fantasy tropes.\n\n")

                append("CRITICAL DIRECTIVES:\n")
                append("1. CHAIN OF THOUGHT REASONING: Before generating ANY narrative text, you MUST use the `internal_reasoning` field to think step-by-step. Example: 'The player challenged the Knight. The mandatory active NPC is the Knight. I must describe the Knight's reaction. I will absolutely not mention Bishop Alistair, the Bailiff, or unrelated past events.' This field forces you to maintain absolute NPC integrity.\n")
                append("2. [IMMEDIATE ACTIVE SCENE - CRITICAL PRIORITY]: This overrides everything in background lore. Do not change the NPC or location under any circumstance while a scene is active.\n")
                append("3. [BACKGROUND LORE - LOW PRIORITY]: Only reference active_flags, factions, and past events if logically demanded by the immediate action. Do not randomly bleed past flags (e.g. KILLED_BAILIFF) into unrelated new scenes.\n")
                append("4. NPC INTEGRITY: The entity responding in 'resolutionText' MUST be the exact same entity leading the 'bridgeText' and 'nextEventText'. Do not magically switch from a Bishop to a Knight.\n")
                append("5. SCENE CONTINUITY: If a MANDATORY ONGOING SCENE is provided, your entire response must focus ONLY on resolving the immediate next seconds of that specific conflict. Do not jump to unrelated events until the current scene is logically marked as resolved.\n")
                append("6. NO REPETITION: Ensure the 3 generated action cards are distinctly different from the previous turn's options.\n")
                append("7. Every stat change (-gold, +tension) MUST have a direct, logical explanation explicitly written in the `resolutionText`.\n\n")

                if (effectiveAnchor != null && ongoingSceneContext.isNullOrBlank()) {
                    if (worldState.turnCount == 1) {
                        append("MANDATORY TURN 1 STORY HOOK:\n")
                        append("$effectiveAnchor\n")
                        append("You MUST establish the opening scene, title, NPC interaction, and options directly around this story hook!\n\n")
                    } else {
                        append("ANCHOR EVENT CONTEXT:\n")
                        append("$effectiveAnchor\n")
                        append("Rule: If an 'Anchor Event Context' is provided in the payload, you MUST make it the central conflict of this turn, blending it seamlessly into the player's ongoing story.\n\n")
                    }
                }

                append("RULES FOR EVENT GENERATION:\n")
                append("A. Causal Link: The core conflict of this turn MUST directly reference the immediate active scene or previous action.\n")
                append("B. Environmental Pressure: The event text MUST incorporate the 'environment' (e.g., descriptions of freezing cold if HARSH_WINTER).\n")
                append("C. Tension/Notoriety Check: If 'tension' is > 70, the event must have a violent or desperate undertone.\n")
                append("D. Options: Generate 3 contextual response cards matching the immediate scene reality.\n")
                append("E. Scene Locking: If the scene/confrontation continues into the next turn, return the exact same or refined updatedActiveSceneContext and updatedActiveNpc. If the NPC dies, leaves, or the player escapes, return null for both, which will unlock the game for a new story anchor.\n\n")

                append("GAME ENGINE CONTEXT PAYLOAD:\n")
                append(payloadContext.toString(2))
                append("\n\n")

                append("EXACT JSON OUTPUT FORMAT RULES:\n")
                append("1. 'internal_reasoning' MUST be the VERY FIRST field in the JSON output, containing your step-by-step reasoning.\n")
                append("2. location MUST be one of: 'Forest', 'Village', 'Tavern', 'Castle', 'Cathedral', 'Marketplace'.\n")
                append("3. npcArchetype MUST be one of: 'PEASANT', 'MERCHANT', 'KNIGHT', 'BISHOP', 'ALCHEMIST', 'BANDIT', 'NOBLE', 'MONARCH'.\n")
                append("4. Provide statChanges with goldChange (-50 to +50), healthChange (-30 to +20), regionalTensionChange (-15 to +15), notorietyChange (-15 to +15), statusEffect (short string), factionChanges (e.g. {\"Church\": 10, \"Peasants\": -5, \"Nobility\": 0, \"Underworld\": 0, \"Guilds\": 0}).\n")
                append("5. newWorldFlags is an array of strings (e.g. [\"KILLED_BAILIFF\", \"SHADOW_DEAL\"]).\n")
                append("6. Always output EXACTLY 3 choice cards in options array.\n")
                append("7. Return ONLY valid JSON matching this exact structure without markdown backticks:\n")
                append("{\n")
                append("  \"internal_reasoning\": \"Step-by-step logic: The player selected grain bribe. The mandatory active NPC is Elder Tobias. I will describe Elder Tobias accepting the grain bribe, maintaining Elder Tobias as the active NPC.\",\n")
                append("  \"resolutionText\": \"Brutal immediate reaction of NPC to chosen action (max 2 sentences).\",\n")
                append("  \"bridgeText\": \"Time-lapse fallout transition leading into the next scenario (max 2 sentences).\",\n")
                append("  \"nextEventTitle\": \"Short dramatic title for next turn\",\n")
                append("  \"nextEventText\": \"Concise story setup for next turn incorporating environment and causal flags (max 2-3 sentences).\",\n")
                append("  \"location\": \"Village\",\n")
                append("  \"npcName\": \"Elder Tobias\",\n")
                append("  \"npcTitle\": \"Village Patriarch\",\n")
                append("  \"npcArchetype\": \"PEASANT\",\n")
                append("  \"updatedActiveSceneContext\": \"Ongoing confrontation with Elder Tobias over uncollected tithes or null if resolved\",\n")
                append("  \"updatedActiveNpc\": \"Elder Tobias or null if resolved\",\n")
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
                append("    {\"id\": 1, \"text\": \"Offer grain bribe for passage\", \"tag\": \"Grain Bribe\", \"cardArchetype\": \"Peasant_Action\"},\n")
                append("    {\"id\": 2, \"text\": \"Pledge service to church sanctuary\", \"tag\": \"Humble Plea\", \"cardArchetype\": \"Church_Action\"},\n")
                append("    {\"id\": 3, \"text\": \"Stand ground with pitchfork drawn\", \"tag\": \"Combat\", \"cardArchetype\": \"Peasant_Action\"}\n")
                append("  ]\n")
                append("}")
            }

            val requestJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", promptText)
                            })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("responseMimeType", "application/json")
                    put("temperature", 0.8)
                    put("maxOutputTokens", 3072)
                })
            }

            val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext null
            }

            val responseBodyString = response.body?.string() ?: return@withContext null
            val rootObj = JSONObject(responseBodyString)
            val candidates = rootObj.optJSONArray("candidates") ?: return@withContext null
            if (candidates.length() == 0) return@withContext null

            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.optJSONObject("content") ?: return@withContext null
            val parts = content.optJSONArray("parts") ?: return@withContext null
            if (parts.length() == 0) return@withContext null

            val jsonText = parts.getJSONObject(0).optString("text", "")
            if (jsonText.isBlank()) return@withContext null

            val cleanedJson = jsonText
                .replace("^```json".toRegex(), "")
                .replace("^```".toRegex(), "")
                .replace("```$".toRegex(), "")
                .trim()

            parseEventResponseJson(cleanedJson, activeTitle)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
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
