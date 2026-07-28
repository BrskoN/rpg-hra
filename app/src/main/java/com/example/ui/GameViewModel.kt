package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.EventOption
import com.example.data.EventResponse
import com.example.data.Faction
import com.example.data.GameRepository
import com.example.data.GameState
import com.example.data.OriginClass
import com.example.data.TurnPhase
import com.example.data.WorldState
import com.example.data.local.AppDatabase
import com.example.data.local.GameSaveEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import com.example.data.OriginSeed
import com.example.data.StoryAnchor

import com.example.data.AppLanguage

class GameViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: GameRepository = GameRepository()
) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val saveDao = db.gameSaveDao()

    private val _worldState = MutableStateFlow(WorldState())
    val worldState: StateFlow<WorldState> = _worldState.asStateFlow()

    private val _turnPhase = MutableStateFlow(TurnPhase.ACTION_SELECTION)
    val turnPhase: StateFlow<TurnPhase> = _turnPhase.asStateFlow()

    private val _isOriginSelected = MutableStateFlow(false)
    val isOriginSelected: StateFlow<Boolean> = _isOriginSelected.asStateFlow()

    private val _currentEvent = MutableStateFlow<EventResponse?>(null)
    val currentEvent: StateFlow<EventResponse?> = _currentEvent.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _hasSavedGame = MutableStateFlow(false)
    val hasSavedGame: StateFlow<Boolean> = _hasSavedGame.asStateFlow()

    private val _savedWorldState = MutableStateFlow<WorldState?>(null)
    val savedWorldState: StateFlow<WorldState?> = _savedWorldState.asStateFlow()

    init {
        // Check for active save file in Room DB on app launch
        viewModelScope.launch(Dispatchers.IO) {
            val existingSave = saveDao.getSaveSync()
            if (existingSave != null && !existingSave.isGameOver) {
                val restoredWorld = existingSave.toWorldState()
                _savedWorldState.value = restoredWorld
                _hasSavedGame.value = true
            }
        }
    }

    fun selectLanguage(language: AppLanguage) {
        val current = _worldState.value
        if (current.selectedLanguage == language) return
        val updated = current.copy(selectedLanguage = language)
        _worldState.value = updated
        persistStateToRoom(updated)
        // If in middle of a turn or starting turn, re-request narrative for new language
        if (_isOriginSelected.value) {
            loadNextScenario(updated, current.lastChosenOptionText, updated.currentAnchorContext)
        }
    }

    fun continueLegacy() {
        val saved = _savedWorldState.value ?: return
        _worldState.value = saved
        _isOriginSelected.value = true
        _turnPhase.value = TurnPhase.ACTION_SELECTION
        loadNextScenario(saved, saved.lastChosenOptionText, saved.currentAnchorContext)
    }

    fun selectOrigin(origin: OriginClass) {
        val updatedFactions = origin.initialFactions.toMutableMap()
        Faction.values().forEach { faction ->
            if (!updatedFactions.containsKey(faction)) {
                updatedFactions[faction] = 50
            }
        }
        val originSeed = OriginSeed.getRandomSeedForOrigin(origin)
        val currentLang = _worldState.value.selectedLanguage
        val initialWorld = WorldState(
            currentChapter = 1,
            turnCount = 1,
            activeOrigin = origin,
            selectedLanguage = currentLang,
            factions = updatedFactions,
            gold = origin.baseGold,
            health = origin.baseHealth,
            maxHealth = origin.baseHealth,
            regionalTension = 20,
            notoriety = 0,
            worldFlags = setOf("OriginSelected_${origin.name}"),
            currentAnchorContext = originSeed.seedPromptContext,
            recentCharactersMet = emptyList(),
            lastActionConsequenceSummary = "",
            currentActiveSceneContext = null,
            currentActiveNpc = null,
            lastResolutionText = null,
            lastBridgeText = null,
            lastChosenOptionText = null,
            isGameOver = false
        )
        _worldState.value = initialWorld
        _isOriginSelected.value = true
        _turnPhase.value = TurnPhase.ACTION_SELECTION

        viewModelScope.launch(Dispatchers.IO) {
            saveDao.deleteSave()
            saveDao.insertSave(GameSaveEntity.fromWorldState(initialWorld))
            _savedWorldState.value = initialWorld
            _hasSavedGame.value = true
        }

        loadNextScenario(initialWorld, chosenOptionText = null, anchorContext = originSeed.seedPromptContext)
    }

    fun selectOption(option: EventOption) {
        val currentWorld = _worldState.value
        if (currentWorld.isGameOver || _isLoading.value) return

        viewModelScope.launch {
            _isLoading.value = true

            val nextResponse = repository.getNextEvent(currentWorld, option.text)

            val changes = nextResponse.statChanges
            val netGoldChange = changes.goldChange
            val newGold = (currentWorld.gold + netGoldChange).coerceAtLeast(0)
            val newHealth = (currentWorld.health + changes.healthChange).coerceIn(0, currentWorld.maxHealth)
            val newTension = (currentWorld.regionalTension + changes.regionalTensionChange).coerceIn(0, 100)
            val newNotoriety = (currentWorld.notoriety + changes.notorietyChange).coerceIn(0, 100)

            val updatedFactions = currentWorld.factions.toMutableMap()
            changes.factionChanges.forEach { (factionNameStr, delta) ->
                val matchingFaction = Faction.values().find {
                    it.name.equals(factionNameStr, ignoreCase = true) ||
                    it.displayName.equals(factionNameStr, ignoreCase = true)
                } ?: Faction.PEASANTS
                val currentRep = updatedFactions[matchingFaction] ?: 50
                updatedFactions[matchingFaction] = (currentRep + delta).coerceIn(0, 100)
            }

            val updatedFlags = currentWorld.worldFlags.toMutableSet()
            updatedFlags.addAll(nextResponse.newWorldFlags)

            if (!changes.statusEffect.isNullOrBlank()) {
                updatedFlags.add(changes.statusEffect)
            }
            if (!changes.chapterFlag.isNullOrBlank()) {
                updatedFlags.add(changes.chapterFlag)
            }

            val textLower = option.text.lowercase()
            val tagLower = option.tag.lowercase()
            when {
                tagLower.contains("bailiff") || textLower.contains("bailiff") || tagLower.contains("combat") || textLower.contains("kill") ->
                    updatedFlags.add("KILLED_BAILIFF")
                tagLower.contains("royal") || textLower.contains("crown") || tagLower.contains("seal") ->
                    updatedFlags.add("CROWN_FAVOR")
                tagLower.contains("trade") || textLower.contains("guild") || tagLower.contains("market") ->
                    updatedFlags.add("GUILD_MASTER")
                tagLower.contains("holy") || textLower.contains("vow") || tagLower.contains("plea") ->
                    updatedFlags.add("HOLY_CRUSADER")
                tagLower.contains("hero") || textLower.contains("oakvale") ->
                    updatedFlags.add("HERO_OF_OAKVALE")
            }

            var gameOver = false
            var gameOverReason: String? = null

            if (newHealth <= 0) {
                gameOver = true
                gameOverReason = "Your journey ended prematurely after succumbing to fatal injuries."
            }

            val resolutionReaction = nextResponse.resolutionText.ifBlank {
                "Regarding '${option.text}': ${nextResponse.npcName} eyes you with grim intent as your action takes effect."
            }
            val bridgeNarrative = nextResponse.bridgeText.ifBlank {
                "Days pass in the realm as the consequences of your decision take root across the provinces."
            }

            val updatedCharactersMet = (currentWorld.recentCharactersMet + nextResponse.npcName).takeLast(2)
            val consequenceSummary = resolutionReaction

            val newActiveSceneContext = nextResponse.updatedActiveSceneContext?.takeIf { it.isNotBlank() && it != "null" }
            val newActiveNpc = nextResponse.updatedActiveNpc?.takeIf { it.isNotBlank() && it != "null" } ?: if (newActiveSceneContext != null) nextResponse.npcName else null

            val updatedWorld = currentWorld.copy(
                gold = newGold,
                health = newHealth,
                factions = updatedFactions,
                worldFlags = updatedFlags,
                regionalTension = newTension,
                notoriety = newNotoriety,
                isGameOver = gameOver,
                gameOverReason = gameOverReason,
                lastResolutionText = resolutionReaction,
                lastBridgeText = bridgeNarrative,
                lastStatChanges = changes.copy(goldChange = netGoldChange),
                lastChosenOptionText = option.text,
                recentCharactersMet = updatedCharactersMet,
                lastActionConsequenceSummary = consequenceSummary,
                currentActiveSceneContext = newActiveSceneContext,
                currentActiveNpc = newActiveNpc
            )

            _worldState.value = updatedWorld
            _currentEvent.value = nextResponse
            _isLoading.value = false

            // Auto-save at the end of resolution / state calculation
            persistStateToRoom(updatedWorld)

            // Transition to Phase 2: RESOLUTION
            _turnPhase.value = TurnPhase.RESOLUTION
        }
    }

    fun acceptFate() {
        if (_worldState.value.isGameOver) return
        // Automatically save at the end of RESOLUTION phase
        persistStateToRoom(_worldState.value)
        // Transition to Phase 3: NARRATIVE_BRIDGE
        _turnPhase.value = TurnPhase.NARRATIVE_BRIDGE
    }

    fun continueNarrativeBridge() {
        val currentWorld = _worldState.value
        if (currentWorld.isGameOver) return

        val nextTurn = currentWorld.turnCount + 1

        val anchorContext = if (nextTurn % 5 == 0) {
            StoryAnchor.selectAnchorForWorldState(currentWorld).anchorPromptContext
        } else null

        val updatedWorld = currentWorld.copy(
            turnCount = nextTurn,
            currentAnchorContext = anchorContext
        )
        _worldState.value = updatedWorld
        persistStateToRoom(updatedWorld)

        if (nextTurn == 25 || (nextTurn > 25 && (nextTurn - 1) % 25 == 0)) {
            _turnPhase.value = TurnPhase.CHAPTER_ASCENSION
        } else {
            _turnPhase.value = TurnPhase.ACTION_SELECTION
            loadNextScenario(updatedWorld, currentWorld.lastChosenOptionText, anchorContext)
        }
    }

    fun evaluateChapterEnd(): List<OriginClass> {
        val currentWorld = _worldState.value
        val gold = currentWorld.gold
        val notoriety = currentWorld.notoriety
        val tension = currentWorld.regionalTension
        val nobilityRep = currentWorld.factions[Faction.NOBILITY] ?: 50
        val guildsRep = currentWorld.factions[Faction.GUILDS] ?: 50
        val churchRep = currentWorld.factions[Faction.CHURCH] ?: 50
        val underworldRep = currentWorld.factions[Faction.UNDERWORLD] ?: 50
        val flags = currentWorld.worldFlags

        val isSevereDescension = (notoriety >= 80 && gold <= 0) ||
                (flags.contains("KILLED_BAILIFF") && notoriety >= 70) ||
                (gold <= -10)

        if (isSevereDescension) {
            // Force ONLY punishment options to compel accountability
            return listOf(OriginClass.PRISONER, OriginClass.BEGGAR, OriginClass.OUTCAST)
        }

        val available = mutableListOf<OriginClass>()

        val isAscension = gold >= 50 || nobilityRep >= 60 || flags.contains("HERO_OF_OAKVALE") || flags.contains("CROWN_FAVOR")
        val isDescension = notoriety >= 80 || gold <= 0 || flags.contains("KILLED_BAILIFF")
        val isNeutral = gold > 0 && tension < 80 && notoriety < 80

        if (isAscension) {
            available.add(OriginClass.KNIGHT)
            if (gold >= 60 || guildsRep >= 60 || flags.contains("MERCHANT_PATRON")) {
                available.add(OriginClass.MASTER_MERCHANT)
            }
            if (churchRep >= 60 || flags.contains("HOLY_CRUSADER") || flags.contains("SACRED_VOW")) {
                available.add(OriginClass.BISHOP)
            }
            if (underworldRep >= 60 || flags.contains("SHADOW_DEAL")) {
                available.add(OriginClass.OUTLAW_KING)
            }
        }

        if (isNeutral || available.isEmpty()) {
            available.add(currentWorld.activeOrigin)
            if (currentWorld.activeOrigin != OriginClass.PEASANT) {
                available.add(OriginClass.PEASANT)
            }
            if (currentWorld.activeOrigin != OriginClass.GUILD_APPRENTICE) {
                available.add(OriginClass.GUILD_APPRENTICE)
            }
        }

        if (isDescension) {
            available.add(OriginClass.PRISONER)
            available.add(OriginClass.BEGGAR)
            available.add(OriginClass.OUTCAST)
        }

        return available.distinct()
    }

    fun transitionToAscensionRole(newOrigin: OriginClass) {
        val currentWorld = _worldState.value
        val newGold = when (newOrigin) {
            OriginClass.PRISONER -> 0
            OriginClass.BEGGAR -> 5
            OriginClass.OUTCAST -> 10
            else -> newOrigin.baseGold
        }
        val newHealth = when (newOrigin) {
            OriginClass.PRISONER -> 50
            OriginClass.BEGGAR -> 70
            OriginClass.OUTCAST -> 80
            else -> newOrigin.baseHealth
        }

        val seed = OriginSeed.getRandomSeedForOrigin(newOrigin)

        val updatedWorld = currentWorld.copy(
            currentChapter = currentWorld.currentChapter + 1,
            turnCount = 1,
            activeOrigin = newOrigin,
            gold = newGold,
            health = newHealth,
            maxHealth = newOrigin.baseHealth,
            worldFlags = currentWorld.worldFlags + "AscendedTo_${newOrigin.name}",
            currentAnchorContext = seed.seedPromptContext,
            lastResolutionText = null,
            lastBridgeText = null,
            lastChosenOptionText = null
        )

        _worldState.value = updatedWorld
        _turnPhase.value = TurnPhase.ACTION_SELECTION
        persistStateToRoom(updatedWorld)
        loadNextScenario(updatedWorld, chosenOptionText = null, anchorContext = seed.seedPromptContext)
    }

    fun openLegacyEndgameScreen() {
        _turnPhase.value = TurnPhase.CHAPTER_ASCENSION
    }

    fun restartGame() {
        val freshWorld = WorldState()
        _worldState.value = freshWorld
        _isOriginSelected.value = false
        _turnPhase.value = TurnPhase.ACTION_SELECTION
        _currentEvent.value = null
        viewModelScope.launch(Dispatchers.IO) {
            saveDao.deleteSave()
            _hasSavedGame.value = false
            _savedWorldState.value = null
        }
    }

    private fun persistStateToRoom(world: WorldState) {
        viewModelScope.launch(Dispatchers.IO) {
            if (world.isGameOver) {
                saveDao.deleteSave()
                _hasSavedGame.value = false
                _savedWorldState.value = null
            } else {
                saveDao.insertSave(GameSaveEntity.fromWorldState(world))
                _savedWorldState.value = world
                _hasSavedGame.value = true
            }
        }
    }

    private fun loadNextScenario(world: WorldState, chosenOptionText: String?, anchorContext: String? = world.currentAnchorContext) {
        viewModelScope.launch {
            _isLoading.value = true
            val response = repository.getNextEvent(world, chosenOptionText, anchorContext)
            _currentEvent.value = response

            val newActiveSceneContext = response.updatedActiveSceneContext?.takeIf { it.isNotBlank() && it != "null" }
            val newActiveNpc = response.updatedActiveNpc?.takeIf { it.isNotBlank() && it != "null" } ?: if (newActiveSceneContext != null) response.npcName else null

            val updatedChars = if (response.npcName.isNotBlank()) (world.recentCharactersMet + response.npcName).takeLast(2) else world.recentCharactersMet
            val updatedWorld = world.copy(
                recentCharactersMet = updatedChars,
                currentActiveSceneContext = newActiveSceneContext,
                currentActiveNpc = newActiveNpc
            )
            _worldState.value = updatedWorld
            persistStateToRoom(updatedWorld)

            _isLoading.value = false
        }
    }
}
