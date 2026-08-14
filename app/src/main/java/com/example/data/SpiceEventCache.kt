package com.example.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Loads a pre-generated, human-curated pool of AI-written spice vignettes bundled with the app at
 * assets/spice_events.json, so runtime play never needs a live Gemini call for flavor content.
 * The pool is built once, offline, by tools/generate_spice_cache.py and shipped as a static asset -
 * this is the same "authored backbone, AI only where it's safe" philosophy as EventDeck, just
 * applied to the flavor tier: the AI call happens once at content-creation time, not per player.
 *
 * Missing or empty entries for a given origin/phase/language are expected and harmless - callers
 * fall back to a live Gemini call in that case, so the cache can be filled in gradually per origin.
 */
class SpiceEventCache(context: Context) {

    private val entries: List<CachedEntry> = loadEntries(context)

    fun pick(origin: OriginClass, phase: EventPhase, lang: AppLanguage): EventResponse? {
        val matches = entries.filter { it.origin == origin && it.phase == phase && it.lang == lang }
        return matches.randomOrNull()?.response
    }

    private data class CachedEntry(
        val origin: OriginClass,
        val phase: EventPhase,
        val lang: AppLanguage,
        val response: EventResponse
    )

    private fun loadEntries(context: Context): List<CachedEntry> {
        return try {
            val json = context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }
            val array = JSONArray(json)
            (0 until array.length()).mapNotNull { i -> parseEntry(array.optJSONObject(i)) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseEntry(obj: JSONObject?): CachedEntry? {
        if (obj == null) return null
        return try {
            val origin = OriginClass.valueOf(obj.getString("origin"))
            val phase = EventPhase.valueOf(obj.getString("phase"))
            val lang = AppLanguage.valueOf(obj.getString("lang"))

            val optionsArr = obj.getJSONArray("options")
            val options = (0 until optionsArr.length()).map { i ->
                val o = optionsArr.getJSONObject(i)
                EventOption(
                    id = o.getInt("id"),
                    text = o.getString("text"),
                    tag = o.optString("tag", "Action"),
                    cardArchetype = o.optString("cardArchetype", "Peasant_Action")
                )
            }

            val flag = obj.optString("flag", "").takeIf { it.isNotBlank() }

            val response = EventResponse(
                nextEventTitle = obj.getString("title"),
                nextEventText = obj.getString("text"),
                location = obj.optString("location", "Village"),
                npcName = obj.optString("npcName", "Local Wanderer"),
                npcTitle = obj.optString("npcTitle", "Kingdom Resident"),
                npcArchetype = obj.optString("npcArchetype", "PEASANT"),
                newWorldFlags = if (flag != null) listOf(flag) else emptyList(),
                options = options
            )
            CachedEntry(origin, phase, lang, response)
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        private const val ASSET_NAME = "spice_events.json"
    }
}
