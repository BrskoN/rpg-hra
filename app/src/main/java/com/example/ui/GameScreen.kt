package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.TurnPhase
import com.example.ui.components.FloatingStatPopups
import com.example.ui.components.GameOverDialog
import com.example.ui.components.HandOfCardsView
import com.example.ui.components.LegacyEndgameScreen
import com.example.ui.components.NarrativeBannerView
import com.example.ui.components.NarrativeBridgeScreen
import com.example.ui.components.NpcPortraitView
import com.example.ui.components.ResolutionOverlay
import com.example.ui.components.SceneEnvironmentBackground
import com.example.ui.components.TopHeaderBar
import com.example.ui.screens.OriginSelectionScreen
import com.example.ui.theme.DeepCharcoal

import com.example.data.getLocalizedName
import com.example.data.getLocalizedTitle

@Composable
fun GameScreen(
    viewModel: GameViewModel = viewModel()
) {
    val worldState by viewModel.worldState.collectAsState()
    val turnPhase by viewModel.turnPhase.collectAsState()
    val isOriginSelected by viewModel.isOriginSelected.collectAsState()
    val currentEvent by viewModel.currentEvent.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val hasSavedGame by viewModel.hasSavedGame.collectAsState()
    val savedWorldState by viewModel.savedWorldState.collectAsState()

    var isHoveredByCard by remember { mutableStateOf(false) }

    // Screen 1: Main Menu & Origin Selection (Pre-Game)
    if (!isOriginSelected) {
        OriginSelectionScreen(
            hasSavedGame = hasSavedGame,
            savedWorldState = savedWorldState,
            selectedLanguage = worldState.selectedLanguage,
            onLanguageSelected = { lang ->
                viewModel.selectLanguage(lang)
            },
            onContinueLegacy = {
                viewModel.continueLegacy()
            },
            onSelectOrigin = { origin ->
                viewModel.selectOrigin(origin)
            },
            modifier = Modifier.testTag("origin_selection_screen")
        )
        return
    }

    // Legacy / Endgame Chapter Ascension Screen
    if (turnPhase == TurnPhase.CHAPTER_ASCENSION) {
        val availableOrigins = viewModel.evaluateChapterEnd()
        LegacyEndgameScreen(
            worldState = worldState,
            availableOrigins = availableOrigins,
            onSelectRole = { newRole ->
                viewModel.transitionToAscensionRole(newRole)
            },
            modifier = Modifier.testTag("legacy_endgame_screen")
        )
        return
    }

    val eventLocation = currentEvent?.location ?: "Village"
    val npcName = currentEvent?.npcName ?: "Local Townsman"
    val npcTitle = currentEvent?.npcTitle ?: "Kingdom Resident"
    val npcArchetype = currentEvent?.npcArchetype ?: "PEASANT"

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("main_game_screen"),
        topBar = {
            // Compact HUD Header
            TopHeaderBar(
                originTitle = worldState.activeOrigin.getLocalizedTitle(worldState.selectedLanguage),
                gold = worldState.gold,
                health = worldState.health,
                maxHealth = worldState.maxHealth,
                factions = worldState.factions,
                regionalTension = worldState.regionalTension,
                notoriety = worldState.notoriety,
                environmentName = worldState.environment.getLocalizedName(worldState.selectedLanguage),
                selectedLanguage = worldState.selectedLanguage,
                onLanguageSelected = { lang ->
                    viewModel.selectLanguage(lang)
                }
            )
        },
        bottomBar = {
            // Hand of 3 Cards shown ONLY during ACTION_SELECTION phase
            if (turnPhase == TurnPhase.ACTION_SELECTION) {
                val options = currentEvent?.options ?: emptyList()
                HandOfCardsView(
                    options = options,
                    playerRankLevel = 1,
                    isEnabled = !isLoading && !worldState.isGameOver,
                    selectedLanguage = worldState.selectedLanguage,
                    onHoverNpc = { isHovered -> isHoveredByCard = isHovered },
                    onOptionSelected = { option ->
                        viewModel.selectOption(option)
                    }
                )
            }
        },
        containerColor = DeepCharcoal
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Scene Background Artwork
            SceneEnvironmentBackground(
                location = eventLocation,
                modifier = Modifier.fillMaxSize()
            )

            // Main Phase 1 Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(4.dp))

                // NPC Portrait Frame with floating stat popups
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    NpcPortraitView(
                        npcName = npcName,
                        npcTitle = npcTitle,
                        npcArchetype = npcArchetype,
                        isHoveredByCard = isHoveredByCard
                    )

                    FloatingStatPopups(
                        statChanges = worldState.lastStatChanges,
                        turnCount = worldState.turnCount,
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Narrative Story Banner
                NarrativeBannerView(
                    eventTitle = currentEvent?.title,
                    storyText = currentEvent?.text,
                    turnCount = worldState.turnCount,
                    isLoading = isLoading,
                    selectedLanguage = worldState.selectedLanguage,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Phase 2 Overlay: RESOLUTION
            if (turnPhase == TurnPhase.RESOLUTION) {
                ResolutionOverlay(
                    resolutionText = worldState.lastResolutionText ?: "Your action was taken.",
                    statChanges = worldState.lastStatChanges,
                    selectedLanguage = worldState.selectedLanguage,
                    onAcceptFate = {
                        viewModel.acceptFate()
                    }
                )
            }

            // Phase 3 Overlay: NARRATIVE_BRIDGE
            if (turnPhase == TurnPhase.NARRATIVE_BRIDGE) {
                NarrativeBridgeScreen(
                    currentChapter = worldState.currentChapter,
                    bridgeText = worldState.lastBridgeText ?: "Seasons turn and time marches forward...",
                    selectedLanguage = worldState.selectedLanguage,
                    onContinuePath = {
                        viewModel.continueNarrativeBridge()
                    }
                )
            }
        }
    }

    // Game Over Dialog
    if (worldState.isGameOver) {
        GameOverDialog(
            worldState = worldState,
            onRestart = {
                viewModel.restartGame()
            }
        )
    }
}
