package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import com.example.ui.theme.MedievalTitle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.OriginClass
import com.example.data.WorldState
import com.example.ui.components.IlluminatedInitial
import com.example.ui.theme.AgedBone
import com.example.ui.theme.AgedGold
import com.example.ui.theme.AntiqueGold
import com.example.ui.theme.DarkCanvasSurface
import com.example.ui.theme.DarkInk
import com.example.ui.theme.DeepCharcoal
import com.example.ui.theme.LightInk
import com.example.ui.theme.MedievalCrimson

import com.example.data.AppLanguage
import com.example.data.getLocalizedDescription
import com.example.data.getLocalizedSubtitle
import com.example.data.getLocalizedTitle

@Composable
fun OriginSelectionScreen(
    onSelectOrigin: (OriginClass) -> Unit,
    modifier: Modifier = Modifier,
    hasSavedGame: Boolean = false,
    savedWorldState: WorldState? = null,
    onContinueLegacy: () -> Unit = {},
    selectedLanguage: AppLanguage = AppLanguage.SLOVAK,
    onLanguageSelected: (AppLanguage) -> Unit = {},
    availableOrigins: List<OriginClass> = listOf(OriginClass.PEASANT, OriginClass.GUILD_APPRENTICE, OriginClass.ACOLYTE, OriginClass.LESSER_NOBLE)
) {
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DeepCharcoal)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // Language Selector Toggle Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DarkCanvasSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, AgedGold)
                ) {
                    Row(
                        modifier = Modifier.padding(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { onLanguageSelected(AppLanguage.SLOVAK) },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (selectedLanguage == AppLanguage.SLOVAK) MedievalCrimson else Color.Transparent,
                                contentColor = if (selectedLanguage == AppLanguage.SLOVAK) AgedGold else LightInk
                            ),
                            border = null,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("lang_sk_button")
                        ) {
                            Text("🇸🇰 SK", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.width(2.dp))

                        OutlinedButton(
                            onClick = { onLanguageSelected(AppLanguage.ENGLISH) },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (selectedLanguage == AppLanguage.ENGLISH) MedievalCrimson else Color.Transparent,
                                contentColor = if (selectedLanguage == AppLanguage.ENGLISH) AgedGold else LightInk
                            ),
                            border = null,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("lang_en_button")
                        ) {
                            Text("🇬🇧 EN", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main Title Banner with Tarot / Woodcut Typography
            Text(
                text = if (selectedLanguage == AppLanguage.SLOVAK) "📜 KRONIKY RÍŠE" else "📜 CHRONICLES OF THE REALM",
                color = AgedGold,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = MedievalTitle,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("game_title_banner")
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (selectedLanguage == AppLanguage.SLOVAK) "Temné stredoveké RPG príčinného osudu a frakčných rivalít" else "A Dark Medieval RPG of Causal Fate and Faction Rivalries",
                color = LightInk.copy(alpha = 0.85f),
                fontSize = 12.5.sp,
                fontFamily = MedievalTitle,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Active Saved Game Banner (if exists)
            if (hasSavedGame && savedWorldState != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(12.dp, RoundedCornerShape(16.dp))
                        .border(2.dp, AgedGold, RoundedCornerShape(16.dp))
                        .testTag("continue_legacy_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkCanvasSurface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MedievalCrimson
                        ) {
                            Text(
                                text = if (selectedLanguage == AppLanguage.SLOVAK) "DETEKOVANÉ AKTÍVNE ULOŽENIE" else "ACTIVE SAVED LEGACY DETECTED",
                                color = AgedGold,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = MedievalTitle,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "${savedWorldState.activeOrigin.getLocalizedTitle(selectedLanguage)} • ${if (selectedLanguage == AppLanguage.SLOVAK) "Kapitola ${savedWorldState.currentChapter} (Ťah ${savedWorldState.turnCount})" else "Chapter ${savedWorldState.currentChapter} (Turn ${savedWorldState.turnCount})"}",
                            color = AgedBone,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = MedievalTitle
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = if (selectedLanguage == AppLanguage.SLOVAK) "🪙 Zlato: ${savedWorldState.gold}  |  ❤️ Zdravie: ${savedWorldState.health}/${savedWorldState.maxHealth}  |  ⚡ Napätie: ${savedWorldState.regionalTension}/100" else "🪙 Gold: ${savedWorldState.gold}  |  ❤️ Health: ${savedWorldState.health}/${savedWorldState.maxHealth}  |  ⚡ Tension: ${savedWorldState.regionalTension}/100",
                            color = LightInk,
                            fontSize = 12.sp,
                            fontFamily = MedievalTitle
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = onContinueLegacy,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("continue_legacy_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MedievalCrimson,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AgedGold)
                        ) {
                            Text(
                                text = if (selectedLanguage == AppLanguage.SLOVAK) "⚔️ POKRAČOVAŤ V PRÍBEHU" else "⚔️ CONTINUE LEGACY",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = MedievalTitle,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = if (selectedLanguage == AppLanguage.SLOVAK) "─── ALEBO ZAČAŤ NOVÝ PRÍBEH ───" else "─── OR BEGIN A NEW LEGACY ───",
                    color = AgedGold,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = MedievalTitle,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))
            } else {
                Text(
                    text = if (selectedLanguage == AppLanguage.SLOVAK) "ZAČAŤ NOVÝ PRÍBEH" else "BEGIN A NEW LEGACY",
                    color = AgedGold,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = MedievalTitle,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))
            }

            // Origin Tarot Cards
            availableOrigins.forEach { origin ->
                OriginTarotCard(
                    origin = origin,
                    selectedLanguage = selectedLanguage,
                    onSelect = { onSelectOrigin(origin) }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun OriginTarotCard(
    origin: OriginClass,
    selectedLanguage: AppLanguage,
    onSelect: () -> Unit
) {
    val testTagStr = "origin_card_${origin.name.lowercase()}"
    val title = origin.getLocalizedTitle(selectedLanguage)
    val subtitle = origin.getLocalizedSubtitle(selectedLanguage)
    val description = origin.getLocalizedDescription(selectedLanguage)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(16.dp))
            .border(2.dp, AgedGold, RoundedCornerShape(16.dp))
            .testTag(testTagStr),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AgedBone)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Illuminated Initial Frame
                IlluminatedInitial(
                    letter = title.take(1),
                    modifier = Modifier.size(76.dp),
                    fontSize = 38.sp,
                    textColor = AntiqueGold,
                    borderColor = AgedGold,
                    backgroundColor = DeepCharcoal
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MedievalCrimson
                    ) {
                        Text(
                            text = subtitle.uppercase(),
                            color = AgedGold,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = MedievalTitle,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = title,
                        color = DarkInk,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = MedievalTitle
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = description,
                color = DarkInk.copy(alpha = 0.9f),
                fontSize = 12.5.sp,
                fontFamily = MedievalTitle,
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Stat Badges Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatBadge(
                    label = if (selectedLanguage == AppLanguage.SLOVAK) "ZLATO" else "GOLD",
                    value = "🪙 ${origin.baseGold}"
                )
                StatBadge(
                    label = if (selectedLanguage == AppLanguage.SLOVAK) "ZDRAVIE" else "HEALTH",
                    value = "❤️ ${origin.baseHealth}"
                )
                val primaryFaction = origin.initialFactions.maxByOrNull { it.value }
                StatBadge(
                    label = if (selectedLanguage == AppLanguage.SLOVAK) "AFINITA" else "AFFINITY",
                    value = "${primaryFaction?.key?.iconSymbol ?: ""} ${primaryFaction?.value ?: 50}%"
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Select Origin Button
            Button(
                onClick = onSelect,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("select_${origin.name.lowercase()}"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MedievalCrimson,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, AgedGold)
            ) {
                Text(
                    text = if (selectedLanguage == AppLanguage.SLOVAK) "ZAČAŤ AKO ${title.uppercase()}" else "BEGIN AS ${title.uppercase()}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = MedievalTitle,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
private fun StatBadge(label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = DarkCanvasSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, AgedGold.copy(alpha = 0.5f))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = label,
                color = AgedGold,
                fontSize = 9.sp,
                fontFamily = MedievalTitle,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = value,
                color = LightInk,
                fontSize = 11.5.sp,
                fontFamily = MedievalTitle,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}
