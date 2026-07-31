package com.example.ui.components

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
import com.example.ui.theme.ManuscriptBody
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.OriginClass
import com.example.data.WorldState
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
fun LegacyEndgameScreen(
    worldState: WorldState,
    availableOrigins: List<OriginClass>,
    onSelectRole: (OriginClass) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val isSlovak = worldState.selectedLanguage == AppLanguage.SLOVAK

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
            Spacer(modifier = Modifier.height(20.dp))

            // Illuminated Header
            IlluminatedInitial(
                letter = "L",
                modifier = Modifier.size(72.dp),
                fontSize = 36.sp,
                textColor = AntiqueGold,
                borderColor = AgedGold,
                backgroundColor = DeepCharcoal
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (isSlovak) "📜 DEDIČSTVO KAPITOLY ${worldState.currentChapter}" else "📜 LEGACY OF CHAPTER ${worldState.currentChapter}",
                color = AgedGold,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = MedievalTitle,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (isSlovak) "Vaše činy v ríši zmenili tok dejín. Skontrolujte svoju stopu vo svete a vstúpte do novej úlohy." else "Your deeds in the realm have altered the course of history. Review your world mark and ascend into your next role.",
                color = LightInk.copy(alpha = 0.85f),
                fontSize = 12.5.sp,
                fontFamily = MedievalTitle,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // World Flags & Legacy Record Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(12.dp, RoundedCornerShape(16.dp))
                    .border(2.dp, AgedGold, RoundedCornerShape(16.dp))
                    .testTag("legacy_flags_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = AgedBone)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = if (isSlovak) "ZANAMENANÉ ZNAKY SVETA A ČINY" else "WORLD FLAGS & DEEDS RECORDED",
                        color = DarkInk,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = MedievalTitle,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    if (worldState.worldFlags.isEmpty()) {
                        Text(
                            text = if (isSlovak) "• Zatiaľ žiadne významné znaky sveta. Vaša cesta sa stále rozvíja." else "• No monumental world flags earned yet. Your journey is still unfolding.",
                            color = DarkInk.copy(alpha = 0.75f),
                            fontSize = 12.sp,
                            fontFamily = MedievalTitle
                        )
                    } else {
                        worldState.worldFlags.forEach { flag ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = DarkCanvasSurface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, AgedGold.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "⚜️",
                                        fontSize = 14.sp,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                    Text(
                                        text = flag.replace("_", " ").uppercase(),
                                        color = AgedGold,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = MedievalTitle
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = if (isSlovak) "👑 DOSTUPNÉ VZESTUPY PRE KAPITOLU 2" else "👑 AVAILABLE CHAPTER 2 ASCENSIONS",
                color = AgedGold,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = MedievalTitle,
                letterSpacing = 1.5.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            availableOrigins.forEach { origin ->
                AscensionRoleCard(
                    origin = origin,
                    selectedLanguage = worldState.selectedLanguage,
                    onSelect = { onSelectRole(origin) }
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AscensionRoleCard(
    origin: OriginClass,
    selectedLanguage: AppLanguage,
    onSelect: () -> Unit
) {
    val isSlovak = selectedLanguage == AppLanguage.SLOVAK
    val title = origin.getLocalizedTitle(selectedLanguage)
    val subtitle = origin.getLocalizedSubtitle(selectedLanguage)
    val description = origin.getLocalizedDescription(selectedLanguage)

    val isDescension = origin in listOf(OriginClass.PRISONER, OriginClass.BEGGAR, OriginClass.OUTCAST)
    val isAscension = origin in listOf(OriginClass.SQUIRE, OriginClass.KNIGHT, OriginClass.MASTER_MERCHANT, OriginClass.BISHOP, OriginClass.OUTLAW_KING)

    val borderColor = when {
        isAscension -> AgedGold
        isDescension -> MedievalCrimson
        else -> Color(0xFF8C8275)
    }

    val categoryLabel = when {
        isAscension -> if (isSlovak) "👑 VZOSTUPNÁ ÚLOHA" else "👑 ASCENSION ROLE"
        isDescension -> if (isSlovak) "⚔️ PÁD / TREST" else "⚔️ DESCENSION / PUNISHMENT"
        else -> if (isSlovak) "⚖️ PODDANÉ DEDIČSTVO" else "⚖️ COMMONER LEGACY"
    }

    val categoryBg = when {
        isAscension -> AgedGold
        isDescension -> MedievalCrimson
        else -> DarkCanvasSurface
    }

    val categoryTextColor = when {
        isAscension -> DarkInk
        else -> AgedGold
    }

    val buttonBg = when {
        isAscension -> AgedGold
        isDescension -> MedievalCrimson
        else -> DeepCharcoal
    }

    val buttonTextColor = when {
        isAscension -> DarkInk
        isDescension -> Color.White
        else -> AgedGold
    }

    val buttonActionText = when {
        isAscension -> if (isSlovak) "VROSTÚŤ AKO ${title.uppercase()}" else "ASCEND AS ${title.uppercase()}"
        isDescension -> if (isSlovak) "PRIJAŤ OSUD AKO ${title.uppercase()}" else "ACCEPT FATE AS ${title.uppercase()}"
        else -> if (isSlovak) "POKRAČOVAŤ AKO ${title.uppercase()}" else "CONTINUE AS ${title.uppercase()}"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(14.dp))
            .border(2.dp, borderColor, RoundedCornerShape(14.dp))
            .testTag("ascension_card_${origin.name.lowercase()}"),
        shape = RoundedCornerShape(14.dp),
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
                IlluminatedInitial(
                    letter = origin.heraldicSymbol,
                    modifier = Modifier.size(68.dp),
                    fontSize = 34.sp,
                    textColor = if (isAscension) AntiqueGold else if (isDescension) MedievalCrimson else AgedGold,
                    borderColor = borderColor,
                    backgroundColor = DeepCharcoal
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = categoryBg
                    ) {
                        Text(
                            text = categoryLabel,
                            color = categoryTextColor,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = MedievalTitle,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = title,
                        color = DarkInk,
                        fontSize = 16.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = MedievalTitle
                    )

                    Text(
                        text = subtitle.uppercase(),
                        color = DarkInk.copy(alpha = 0.7f),
                        fontSize = 10.sp,
                        fontFamily = MedievalTitle
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                color = DarkInk.copy(alpha = 0.9f),
                fontSize = 13.5.sp,
                fontFamily = ManuscriptBody,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "${if (isSlovak) "PODMIENKA" else "REQUIREMENT"}: ${origin.reqDescription}",
                color = if (isDescension) MedievalCrimson else if (isAscension) DarkInk else DarkInk.copy(alpha = 0.8f),
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = MedievalTitle
            )

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onSelect,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ascend_to_${origin.name.lowercase()}"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonBg,
                    contentColor = buttonTextColor
                ),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
            ) {
                Text(
                    text = buttonActionText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = MedievalTitle,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}
