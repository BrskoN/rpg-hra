package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.AppLanguage
import com.example.data.EnvironmentCondition
import com.example.data.Faction
import com.example.data.OriginClass
import com.example.data.WorldState
import org.json.JSONArray
import org.json.JSONObject

@Entity(tableName = "game_save")
data class GameSaveEntity(
    @PrimaryKey val id: Int = 1,
    val currentChapter: Int,
    val activeOriginName: String,
    val selectedLanguageName: String = "SLOVAK",
    val gold: Int,
    val health: Int,
    val maxHealth: Int,
    val turnCount: Int,
    val regionalTension: Int,
    val notoriety: Int,
    val environmentName: String,
    val worldFlagsJson: String,
    val factionsJson: String,
    val isGameOver: Boolean,
    val lastResolutionText: String?,
    val lastBridgeText: String?,
    val lastChosenOptionText: String?,
    val currentAnchorContext: String? = null,
    val recentCharactersMetJson: String = "[]",
    val lastActionConsequenceSummary: String = "",
    val currentActiveSceneContext: String? = null,
    val currentActiveNpc: String? = null,
    val currentNodeId: String? = null,
    val visitedNodeIdsJson: String = "[]",
    val inventoryItemIdsJson: String = "[]",
    val hiddenInfluencesJson: String = "{}",
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toWorldState(): WorldState {
        val origin = OriginClass.values().find { it.name == activeOriginName } ?: OriginClass.PEASANT
        val env = EnvironmentCondition.values().find { it.name == environmentName } ?: EnvironmentCondition.HARSH_WINTER
        val lang = AppLanguage.values().find { it.name == selectedLanguageName } ?: AppLanguage.SLOVAK

        val flagsSet = mutableSetOf<String>()
        try {
            val flagsArr = JSONArray(worldFlagsJson)
            for (i in 0 until flagsArr.length()) {
                flagsSet.add(flagsArr.getString(i))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val factionsMap = mutableMapOf<Faction, Int>()
        try {
            val factionsObj = JSONObject(factionsJson)
            val keys = factionsObj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val factionEnum = Faction.values().find { it.name == key }
                if (factionEnum != null) {
                    factionsMap[factionEnum] = factionsObj.optInt(key, 50)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (factionsMap.isEmpty()) {
            factionsMap.putAll(origin.initialFactions)
        }

        val charsList = mutableListOf<String>()
        try {
            val charsArr = JSONArray(recentCharactersMetJson)
            for (i in 0 until charsArr.length()) {
                charsList.add(charsArr.getString(i))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val visitedNodesSet = mutableSetOf<String>()
        try {
            val visitedArr = JSONArray(visitedNodeIdsJson)
            for (i in 0 until visitedArr.length()) {
                visitedNodesSet.add(visitedArr.getString(i))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val inventorySet = mutableSetOf<String>()
        try {
            val inventoryArr = JSONArray(inventoryItemIdsJson)
            for (i in 0 until inventoryArr.length()) {
                inventorySet.add(inventoryArr.getString(i))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val influencesMap = mutableMapOf<String, Int>()
        try {
            val influencesObj = JSONObject(hiddenInfluencesJson)
            val keys = influencesObj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                influencesMap[key] = influencesObj.optInt(key, 0)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return WorldState(
            currentChapter = currentChapter,
            activeOrigin = origin,
            selectedLanguage = lang,
            factions = factionsMap,
            gold = gold,
            health = health,
            maxHealth = maxHealth,
            turnCount = turnCount,
            regionalTension = regionalTension,
            notoriety = notoriety,
            environment = env,
            worldFlags = flagsSet,
            isGameOver = isGameOver,
            lastResolutionText = lastResolutionText,
            lastBridgeText = lastBridgeText,
            lastChosenOptionText = lastChosenOptionText,
            currentAnchorContext = currentAnchorContext,
            recentCharactersMet = charsList,
            lastActionConsequenceSummary = lastActionConsequenceSummary,
            currentActiveSceneContext = currentActiveSceneContext,
            currentActiveNpc = currentActiveNpc,
            currentNodeId = currentNodeId,
            visitedNodeIds = visitedNodesSet,
            inventoryItemIds = inventorySet,
            hiddenInfluences = influencesMap
        )
    }

    companion object {
        fun fromWorldState(state: WorldState): GameSaveEntity {
            val flagsArr = JSONArray()
            state.worldFlags.forEach { flagsArr.put(it) }

            val factionsObj = JSONObject()
            state.factions.forEach { (faction, rep) ->
                factionsObj.put(faction.name, rep)
            }

            val charsArr = JSONArray()
            state.recentCharactersMet.forEach { charsArr.put(it) }

            val visitedArr = JSONArray()
            state.visitedNodeIds.forEach { visitedArr.put(it) }

            val inventoryArr = JSONArray()
            state.inventoryItemIds.forEach { inventoryArr.put(it) }

            val influencesObj = JSONObject()
            state.hiddenInfluences.forEach { (key, value) -> influencesObj.put(key, value) }

            return GameSaveEntity(
                id = 1,
                currentChapter = state.currentChapter,
                activeOriginName = state.activeOrigin.name,
                selectedLanguageName = state.selectedLanguage.name,
                gold = state.gold,
                health = state.health,
                maxHealth = state.maxHealth,
                turnCount = state.turnCount,
                regionalTension = state.regionalTension,
                notoriety = state.notoriety,
                environmentName = state.environment.name,
                worldFlagsJson = flagsArr.toString(),
                factionsJson = factionsObj.toString(),
                isGameOver = state.isGameOver,
                lastResolutionText = state.lastResolutionText,
                lastBridgeText = state.lastBridgeText,
                lastChosenOptionText = state.lastChosenOptionText,
                currentAnchorContext = state.currentAnchorContext,
                recentCharactersMetJson = charsArr.toString(),
                lastActionConsequenceSummary = state.lastActionConsequenceSummary,
                currentActiveSceneContext = state.currentActiveSceneContext,
                currentActiveNpc = state.currentActiveNpc,
                currentNodeId = state.currentNodeId,
                visitedNodeIdsJson = visitedArr.toString(),
                inventoryItemIdsJson = inventoryArr.toString(),
                hiddenInfluencesJson = influencesObj.toString()
            )
        }
    }
}
